import { readFile, readdir } from "node:fs/promises";
import path from "node:path";

const raiz = process.cwd();
const extensoes = new Set([".js", ".jsx", ".html"]);
const ignorar = new Set(["node_modules", "dist", ".git", "coverage", "reports"]);

async function listar(diretorio) {
  const resultado = [];
  for (const entrada of await readdir(diretorio, { withFileTypes: true })) {
    if (ignorar.has(entrada.name)) continue;
    const completo = path.join(diretorio, entrada.name);
    if (entrada.isDirectory()) resultado.push(...(await listar(completo)));
    else if (extensoes.has(path.extname(entrada.name))) resultado.push(completo);
  }
  return resultado;
}

const falhas = [];
const alvos = [path.join(raiz, "src"), path.join(raiz, "index.html"), path.join(raiz, "vite.config.js")];
const arquivos = [];
for (const alvo of alvos) {
  if (path.extname(alvo)) arquivos.push(alvo);
  else arquivos.push(...(await listar(alvo)));
}

for (const arquivo of arquivos) {
  const texto = await readFile(arquivo, "utf8");
  const relativo = path.relative(raiz, arquivo).replaceAll("\\", "/");
  if (/new\s+Function\s*\(/.test(texto)) falhas.push(`${relativo}: executa código dinâmico`);
  if (/["'`]\/config\.js\b/.test(texto)) falhas.push(`${relativo}: ainda referencia /config.js`);
  if (/window\.APP_CONFIG/.test(texto)) falhas.push(`${relativo}: usa configuração global executável`);
  if (
    relativo.startsWith("src/") &&
    relativo !== "src/core/config.js" &&
    /import\.meta\.env\./.test(texto)
  ) {
    falhas.push(`${relativo}: acessa import.meta.env fora de core/config.js`);
  }
}

const runtime = await readFile(path.join(raiz, "src/core/runtimeConfig.js"), "utf8");
const servicoSistema = await readFile(path.join(raiz, "src/services/system/ServicoSistema.js"), "utf8");
const aplicacao = await readFile(path.join(raiz, "src/Aplicacao.jsx"), "utf8");
const paginaManutencao = await readFile(path.join(raiz, "src/core/ui/PaginaManutencao.jsx"), "utf8");
const httpCore = await readFile(path.join(raiz, "src/core/http.js"), "utf8");
const configCore = await readFile(path.join(raiz, "src/core/config.js"), "utf8");
const envExemplo = await readFile(path.join(raiz, ".env.example"), "utf8");
const pacote = JSON.parse(await readFile(path.join(raiz, "package.json"), "utf8"));
const versaoNodeEsperada = "v22.16.0";
const npmEsperado = "npm@10.9.2";

if (process.version !== versaoNodeEsperada) {
  falhas.push(`Node esperado ${versaoNodeEsperada}, encontrado ${process.version}`);
}
if (pacote.packageManager !== npmEsperado) {
  falhas.push(`packageManager deve permanecer ${npmEsperado}`);
}
if (!servicoSistema.includes("/config.json")) falhas.push("ServicoSistema.js não consome /config.json");
if (!servicoSistema.includes("validarRuntimeConfig")) falhas.push("ServicoSistema.js não valida o JSON recebido");
if (!runtime.includes("validarRuntimeConfig")) falhas.push("runtimeConfig.js não expõe a validação do JSON");
if (!runtime.includes("combinarConfiguracaoEfetiva")) falhas.push("runtimeConfig.js não documenta/aplica a precedência de runtime");
if (!aplicacao.includes("ServicoSistema.carregarRuntimeConfig")) falhas.push("Aplicacao.jsx não inicializa /config.json no bootstrap");
if (!aplicacao.includes("combinarConfiguracaoEfetiva(CONFIG, configuracaoServidor)")) falhas.push("Aplicacao.jsx não aplica precedência do runtime sobre o fallback VITE");
if (!paginaManutencao.includes("previsaoRetorno = CONFIG.PREVISAO_RETORNO")) falhas.push("PaginaManutencao.jsx não aceita previsão dinâmica do backend");

// O .env.example é o contrato documentado de build do frontend: nenhuma VITE_* usada
// pode faltar e nenhuma VITE_* documentada deve ficar órfã.
const extrairChaves = (texto, regex) => new Set([...texto.matchAll(regex)].map((match) => match[1]));
const chavesViteDocumentadas = extrairChaves(envExemplo, /^(VITE_[A-Z0-9_]+)=/gm);
const chavesViteConsumidas = extrairChaves(configCore, /import\.meta\.env\.(VITE_[A-Z0-9_]+)/g);
for (const chave of chavesViteConsumidas) {
  if (!chavesViteDocumentadas.has(chave)) falhas.push(`.env.example não documenta ${chave}`);
}
for (const chave of chavesViteDocumentadas) {
  if (!chavesViteConsumidas.has(chave)) falhas.push(`.env.example contém variável VITE órfã: ${chave}`);
}

const nomesViteDePermissao = [...chavesViteDocumentadas].filter((chave) =>
  /(?:ROLE|PERMISSAO|AUTORIDADE|GRANT|ACESSO)/i.test(chave),
);
if (nomesViteDePermissao.length) {
  falhas.push(`Feature/config VITE_* não pode conceder permissão: ${nomesViteDePermissao.join(", ")}`);
}

if (!httpCore.includes("X-Timezone") || !httpCore.includes("resolvedOptions().timeZone")) {
  falhas.push("core/http.js não envia timezone IANA do navegador como informação de cliente");
}

if (falhas.length) {
  console.error("Validação de runtime falhou:\n- " + falhas.join("\n- "));
  process.exit(1);
}
console.log(`Runtime validado: ${process.version}, ${npmEsperado}, /config.json tipado e import.meta.env centralizado.`);
