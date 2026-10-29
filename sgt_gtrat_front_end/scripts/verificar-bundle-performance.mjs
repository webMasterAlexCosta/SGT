import fs from "node:fs";
import path from "node:path";
import zlib from "node:zlib";

const raiz = process.cwd();
const dist = path.join(raiz, "dist");
const reports = path.join(raiz, "reports");
fs.mkdirSync(reports, { recursive: true });

if (!fs.existsSync(dist)) {
  console.error("Diretório dist não encontrado. Execute vite build antes da análise de bundle.");
  process.exit(1);
}

const listar = (dir) => fs.readdirSync(dir, { withFileTypes: true }).flatMap((entrada) => {
  const completo = path.join(dir, entrada.name);
  if (entrada.isDirectory()) return listar(completo);
  return [completo];
});

const arquivos = listar(dist).filter((arquivo) => /\.(?:js|css)$/.test(arquivo));
const metricas = arquivos.map((arquivo) => {
  const buffer = fs.readFileSync(arquivo);
  return {
    arquivo: path.relative(dist, arquivo).replaceAll("\\", "/"),
    tipo: path.extname(arquivo).slice(1),
    bytes: buffer.length,
    gzipBytes: zlib.gzipSync(buffer, { level: 9 }).length,
  };
}).sort((a, b) => b.bytes - a.bytes);

const js = metricas.filter((item) => item.tipo === "js");
const css = metricas.filter((item) => item.tipo === "css");
const soma = (lista, campo) => lista.reduce((total, item) => total + item[campo], 0);

const limites = {
  maiorJsBytes: Number(process.env.SGT_BUNDLE_MAX_CHUNK_JS_BYTES || 1_500_000),
  maiorJsGzipBytes: Number(process.env.SGT_BUNDLE_MAX_CHUNK_JS_GZIP_BYTES || 450_000),
  totalJsBytes: Number(process.env.SGT_BUNDLE_MAX_TOTAL_JS_BYTES || 5_000_000),
  totalJsGzipBytes: Number(process.env.SGT_BUNDLE_MAX_TOTAL_JS_GZIP_BYTES || 1_500_000),
  totalCssBytes: Number(process.env.SGT_BUNDLE_MAX_TOTAL_CSS_BYTES || 750_000),
};

const avisos = [];
for (const item of js) {
  if (item.bytes > 1_000_000 || item.gzipBytes > 350_000) {
    avisos.push(`Chunk pesado para revisão: ${item.arquivo} (${item.bytes} bytes / ${item.gzipBytes} gzip).`);
  }
}
if (soma(js, "gzipBytes") > 1_500_000) avisos.push("JS total gzip acima de 1,5 MB; revisar lazy loading e dependências pesadas.");

const resumo = {
  geradoEm: new Date().toISOString(),
  arquivosJs: js.length,
  arquivosCss: css.length,
  totalJsBytes: soma(js, "bytes"),
  totalJsGzipBytes: soma(js, "gzipBytes"),
  totalCssBytes: soma(css, "bytes"),
  maiorJs: js[0] || null,
  limites,
  avisos,
  arquivos: metricas,
};

const violacoes = [];
if (resumo.maiorJs?.bytes > limites.maiorJsBytes) violacoes.push(`Maior chunk JS excede ${limites.maiorJsBytes} bytes.`);
if (resumo.maiorJs?.gzipBytes > limites.maiorJsGzipBytes) violacoes.push(`Maior chunk JS gzip excede ${limites.maiorJsGzipBytes} bytes.`);
if (resumo.totalJsBytes > limites.totalJsBytes) violacoes.push(`JS total excede ${limites.totalJsBytes} bytes.`);
if (resumo.totalJsGzipBytes > limites.totalJsGzipBytes) violacoes.push(`JS total gzip excede ${limites.totalJsGzipBytes} bytes.`);
if (resumo.totalCssBytes > limites.totalCssBytes) violacoes.push(`CSS total excede ${limites.totalCssBytes} bytes.`);
resumo.violacoes = violacoes;

fs.writeFileSync(path.join(reports, "bundle-performance.json"), JSON.stringify(resumo, null, 2));

const kb = (bytes) => `${(bytes / 1024).toFixed(1)} KB`;
const linhas = [
  "# Orçamento de bundle React",
  "",
  `- JS total: ${kb(resumo.totalJsBytes)} (${kb(resumo.totalJsGzipBytes)} gzip)`,
  `- CSS total: ${kb(resumo.totalCssBytes)}`,
  `- Maior JS: ${resumo.maiorJs ? `${resumo.maiorJs.arquivo} — ${kb(resumo.maiorJs.bytes)} (${kb(resumo.maiorJs.gzipBytes)} gzip)` : "nenhum"}`,
  `- Violações: ${violacoes.length}`,
  `- Avisos de otimização: ${avisos.length}`,
  "",
  ...(avisos.length ? ["## Avisos de otimização", "", ...avisos.map((v) => `- ${v}`), ""] : []),
  "## Maiores arquivos",
  "",
  ...metricas.slice(0, 20).map((item) => `- \`${item.arquivo}\`: ${kb(item.bytes)} / ${kb(item.gzipBytes)} gzip`),
  "",
  ...(violacoes.length ? ["## Violações", "", ...violacoes.map((v) => `- ${v}`), ""] : []),
].join("\n");
fs.writeFileSync(path.join(reports, "bundle-performance.md"), linhas);

console.log(`Bundle: JS ${kb(resumo.totalJsBytes)} (${kb(resumo.totalJsGzipBytes)} gzip), CSS ${kb(resumo.totalCssBytes)}, maior chunk ${resumo.maiorJs ? kb(resumo.maiorJs.bytes) : "0 KB"}.`);
if (violacoes.length) {
  for (const violacao of violacoes) console.error(`BUDGET: ${violacao}`);
  process.exitCode = 1;
}
