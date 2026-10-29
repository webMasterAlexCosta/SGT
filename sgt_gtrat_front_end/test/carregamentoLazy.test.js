import test from "node:test";
import assert from "node:assert/strict";
import { existsSync, readFileSync, readdirSync, statSync } from "node:fs";
import { dirname, extname, join, resolve } from "node:path";

const raizFontes = resolve(import.meta.dirname, "../src");

const listarFontes = (diretorio) => readdirSync(diretorio, { withFileTypes: true }).flatMap((entrada) => {
  const caminho = join(diretorio, entrada.name);
  if (entrada.isDirectory()) return listarFontes(caminho);
  return /\.[cm]?[jt]sx?$/.test(entrada.name) ? [caminho] : [];
});

const resolverModulo = (arquivoOrigem, especificador) => {
  const base = resolve(dirname(arquivoOrigem), especificador);
  const candidatos = extname(base)
    ? [base]
    : [base, `${base}.js`, `${base}.jsx`, join(base, "index.js"), join(base, "index.jsx")];
  return candidatos.find((caminho) => existsSync(caminho) && statSync(caminho).isFile());
};

const escaparExpressaoRegular = (texto) => texto.replace(/[.*+?^${}()|[\]\\]/g, "\\$&");

const obterExportacoes = (fonte) => {
  const exportacoes = new Set();
  for (const resultado of fonte.matchAll(/export\s+(?:async\s+)?(?:const|let|var|function|class)\s+([A-Za-z_$][\w$]*)/g)) {
    exportacoes.add(resultado[1]);
  }
  if (/export\s+default\b/.test(fonte)) exportacoes.add("default");
  for (const resultado of fonte.matchAll(/export\s*\{([^}]*)\}/gs)) {
    for (const item of resultado[1].split(",")) {
      const partes = item.trim().split(/\s+as\s+/);
      if (partes[0]) exportacoes.add((partes[1] || partes[0]).trim());
    }
  }
  return exportacoes;
};

test("todo carregamento lazy aponta para um export existente", () => {
  const falhas = [];
  const padraoLazy = /import\(\s*["']([^"']+)["']\s*\)\.then\(\s*\((\w+)\)\s*=>\s*\(\{\s*default:\s*\2\.([A-Za-z_$][\w$]*)/gs;

  for (const arquivo of listarFontes(raizFontes)) {
    const fonte = readFileSync(arquivo, "utf8");
    for (const correspondencia of fonte.matchAll(padraoLazy)) {
      const [, especificador, , nomeExportado] = correspondencia;
      const modulo = resolverModulo(arquivo, especificador);
      if (!modulo) {
        falhas.push(`${arquivo}: módulo lazy não encontrado: ${especificador}`);
        continue;
      }

      const fonteModulo = readFileSync(modulo, "utf8");
      const nome = escaparExpressaoRegular(nomeExportado);
      const exportacaoDireta = new RegExp(`export\\s+(?:const|function|class)\\s+${nome}\\b`);
      const exportacaoAgrupada = new RegExp(`export\\s*\\{[^}]*\\b${nome}\\b[^}]*\\}`, "s");
      if (!exportacaoDireta.test(fonteModulo) && !exportacaoAgrupada.test(fonteModulo)) {
        falhas.push(`${arquivo}: ${especificador} não exporta ${nomeExportado}`);
      }
    }
  }

  assert.deepEqual(falhas, []);
});

test("toda importação local aponta para um arquivo e um export existentes", () => {
  const falhas = [];
  const padraoImportacao = /import\s+([\s\S]*?)\s+from\s+["']([^"']+)["']\s*;?/g;

  for (const arquivo of listarFontes(raizFontes)) {
    const fonte = readFileSync(arquivo, "utf8");
    for (const correspondencia of fonte.matchAll(padraoImportacao)) {
      const [, declaracao, especificador] = correspondencia;
      if (!especificador.startsWith(".")) continue;

      const modulo = resolverModulo(arquivo, especificador);
      if (!modulo) {
        falhas.push(`${arquivo}: módulo local não encontrado: ${especificador}`);
        continue;
      }
      if (!/\.[cm]?[jt]sx?$/.test(modulo)) continue;

      const exportacoes = obterExportacoes(readFileSync(modulo, "utf8"));
      const declaracaoLimpa = declaracao.replace(/\/\*[\s\S]*?\*\//g, "").trim();
      const trechoNomeado = declaracaoLimpa.match(/\{([\s\S]*?)\}/)?.[1];
      const trechoPadrao = declaracaoLimpa.replace(/\{[\s\S]*?\}/, "").replace(/,\s*$/, "").trim();

      if (trechoPadrao && !trechoPadrao.startsWith("* as ") && !exportacoes.has("default")) {
        falhas.push(`${arquivo}: ${especificador} não possui export default`);
      }
      if (trechoNomeado) {
        for (const item of trechoNomeado.split(",")) {
          const nomeImportado = item.trim().split(/\s+as\s+/)[0];
          if (nomeImportado && !exportacoes.has(nomeImportado)) {
            falhas.push(`${arquivo}: ${especificador} não exporta ${nomeImportado}`);
          }
        }
      }
    }
  }

  assert.deepEqual(falhas, []);
});
