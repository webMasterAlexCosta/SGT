import { readFileSync, readdirSync, existsSync } from "node:fs";
import { spawnSync } from "node:child_process";
import path from "node:path";

const raiz = path.resolve(import.meta.dirname, "..");
const raizFonte = path.join(raiz, "src");
const falhas = [];

const listar = (diretorio) => {
  const resultado = [];
  for (const entrada of readdirSync(diretorio, { withFileTypes: true })) {
    const caminho = path.join(diretorio, entrada.name);
    if (entrada.isDirectory()) resultado.push(...listar(caminho));
    else if (/\.(?:js|jsx)$/.test(entrada.name)) resultado.push(caminho);
  }
  return resultado;
};

const resolverImportacao = (origem, importacao) => {
  const base = path.resolve(path.dirname(origem), importacao);
  const candidatos = [
    base,
    `${base}.js`,
    `${base}.jsx`,
    path.join(base, "index.js"),
    path.join(base, "index.jsx"),
  ];
  return candidatos.some(existsSync);
};

const arquivos = listar(raizFonte);
const nomesAntigosAutenticacao =
  /\.(?:login|register|logout|refreshProfile|completeLogout|refreshSessionInfo)\b|\b(?:refreshProfile|completeLogout|refreshSessionInfo)\b|\b(?:login|register|logout)\s*(?::|=|\()/;
const chamadasNativasCorrompidas = /(?:api\.requisitar|interceptors\.requisitar|\.estaVazio\s*\()/;
const funcoesInglesRestantes = /\b(?:checkEnv|normalizeMethod|isPlainObject|getFriendlyMessage|isLoginRequest|tryParseJsonString|extractMessage|handleScrollOrResize|handleClickOutside|updateCountdown)\b/;

for (const arquivo of arquivos) {
  const relativo = path.relative(raiz, arquivo);
  const fonte = readFileSync(arquivo, "utf8");
  if (/\b(?:legacy|legado|legada|legados|legadas)\b/i.test(fonte)) {
    falhas.push(`${relativo}: código ou comentário legado`);
  }
  if (nomesAntigosAutenticacao.test(fonte)) falhas.push(`${relativo}: método antigo de autenticação`);
  if (chamadasNativasCorrompidas.test(fonte)) falhas.push(`${relativo}: API externa ou nativa traduzida`);
  if (funcoesInglesRestantes.test(fonte)) falhas.push(`${relativo}: função própria ainda em inglês`);

  const importacoes = [
    ...fonte.matchAll(/\bfrom\s*["'](\.{1,2}\/[^"']+)["']/g),
    ...fonte.matchAll(/\bimport\s*\(\s*["'](\.{1,2}\/[^"']+)["']\s*\)/g),
  ];
  for (const importacao of importacoes) {
    if (!resolverImportacao(arquivo, importacao[1])) {
      falhas.push(`${relativo}: import relativo não resolvido: ${importacao[1]}`);
    }
  }

  if (arquivo.endsWith(".js")) {
    const sintaxe = spawnSync(process.execPath, ["--check", arquivo], { encoding: "utf8" });
    if (sintaxe.status !== 0) falhas.push(`${relativo}: sintaxe inválida: ${sintaxe.stderr.trim()}`);
  }
}

const arquivosRemovidos = [
  "src/services/rh/pagination.js",
  "src/services/rh/paginationCore.js",
  "src/services/painel/painel.js",
  "src/core/http/axiosRequestConfig.js",
  "src/core/http/paramsSerializer.js",
  "src/modules/rh/pages/PaginaFrequencia/domain/frequenciaDomain.js",
];
for (const relativo of arquivosRemovidos) {
  if (existsSync(path.join(raiz, relativo))) falhas.push(`Arquivo antigo encontrado: ${relativo}`);
}

console.log(`Arquivos React/JavaScript verificados: ${arquivos.length}`);
if (falhas.length) {
  console.error(`Falhas de nomenclatura: ${falhas.length}`);
  for (const falha of falhas) console.error(`- ${falha}`);
  process.exit(1);
}
console.log("Validação de nomenclatura React aprovada.");
