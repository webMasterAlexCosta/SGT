import test from "node:test";
import assert from "node:assert/strict";
import { access, readdir, readFile } from "node:fs/promises";
import path from "node:path";
import { fileURLToPath } from "node:url";
import { CHAVES_CONFIGURACAO_FRONTEND } from "../src/core/configuracaoFrontend.js";

const raiz = path.resolve(path.dirname(fileURLToPath(import.meta.url)), "..");
const src = path.join(raiz, "src");

const listarFontes = async (diretorio) => {
  const itens = await readdir(diretorio, { withFileTypes: true });
  const saida = [];
  for (const item of itens) {
    const completo = path.join(diretorio, item.name);
    if (item.isDirectory()) saida.push(...await listarFontes(completo));
    else if (/\.(js|jsx)$/.test(item.name)) saida.push(completo);
  }
  return saida;
};

const lerEnvKeys = async (arquivo) => {
  const texto = await readFile(arquivo, "utf8");
  return new Set(texto.split(/\r?\n/)
    .map((linha) => linha.trim())
    .filter((linha) => linha && !linha.startsWith("#") && linha.includes("="))
    .map((linha) => linha.split("=", 1)[0].trim()));
};

test("UI e hooks não acessam HTTP diretamente; chamadas ficam em services", async () => {
  const arquivos = await listarFontes(src);
  const violacoes = [];
  for (const arquivo of arquivos) {
    const relativo = path.relative(src, arquivo).replaceAll("\\", "/");
    if (relativo.startsWith("services/") || relativo === "core/http.js" || relativo.startsWith("core/http/")) continue;
    const texto = await readFile(arquivo, "utf8");
    if (/\brequest\s*\(|\bfetch\s*\(|\baxios(?:\.|\s*\()/.test(texto)) violacoes.push(relativo);
  }
  assert.deepEqual(violacoes, []);
});

test("módulos de UI não importam a camada de transporte HTTP", async () => {
  const modulesDir = path.join(src, "modules");
  const violacoes = [];
  for (const arquivo of await listarFontes(modulesDir)) {
    const texto = await readFile(arquivo, "utf8");
    if (/from\s+["'][^"']*core\/http(?:\.js)?["']/.test(texto)) {
      violacoes.push(path.relative(src, arquivo).replaceAll("\\", "/"));
    }
  }
  assert.deepEqual(violacoes, []);
});

test("regras extraídas da UI estão centralizadas em services", async () => {
  const esperados = [
    "services/rh/ServicoDominioFrequencia.js",
    "services/rh/ServicoDominioEmpregado.js",
    "services/rh/ServicoAbsenteismo.js",
    "services/rh/ServicoDominioRelatorioFrequencia.js",
    "services/admin/ServicoDominioJornada.js",
    "services/admin/ServicoDominioContrato.js",
  ];
  await Promise.all(esperados.map((relativo) => access(path.join(src, relativo))));

  const frequencia = await readFile(path.join(src, "modules/rh/pages/PaginaFrequencia/useLogicaFrequencia.js"), "utf8");
  const contratos = await readFile(path.join(src, "modules/admin/pages/PaginaContratos/index.jsx"), "utf8");
  assert.match(frequencia, /services\/rh\/ServicoDominioFrequencia/);
  assert.match(contratos, /services\/admin\/ServicoDominioContrato/);
});

test("toda referência CONFIG usada pelo React existe no contrato central de configuração", async () => {
  const configPath = path.join(src, "core", "config.js");
  const definidas = new Set(CHAVES_CONFIGURACAO_FRONTEND);
  const usadas = new Set();
  for (const arquivo of await listarFontes(src)) {
    if (arquivo === configPath) continue;
    const texto = await readFile(arquivo, "utf8");
    for (const match of texto.matchAll(/\bCONFIG\.([A-Z][A-Z0-9_]+)/g)) usadas.add(match[1]);
  }
  const ausentes = [...usadas].filter((chave) => !definidas.has(chave)).sort();
  assert.deepEqual(ausentes, []);
});

test(".env.example cobre todas as VITE_* exigidas por core/config", async () => {
  const configTexto = await readFile(path.join(src, "core", "config.js"), "utf8");
  const exigidas = new Set([...configTexto.matchAll(/import\.meta\.env\.(VITE_[A-Z0-9_]+)/g)].map((m) => m[1]));
  const exemplo = await lerEnvKeys(path.join(raiz, ".env.example"));
  const ausentes = [...exigidas].filter((chave) => !exemplo.has(chave)).sort();
  assert.deepEqual(ausentes, []);
});
