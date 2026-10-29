import { spawn } from "node:child_process";
import { access, readdir, mkdir, writeFile } from "node:fs/promises";
import path from "node:path";
import process from "node:process";
import { fileURLToPath } from "node:url";
import { gerarPdfTexto } from "./lib/simple-pdf.mjs";

const raiz = path.resolve(path.dirname(fileURLToPath(import.meta.url)), "..");
const pastaTestes = path.join(raiz, "test");
const pastaRelatorios = path.join(raiz, "reports");
const pastaTemporaria = path.join(pastaRelatorios, ".temporario");
const arquivos = (await readdir(pastaTestes))
  .filter((arquivo) => arquivo.endsWith(".test.js"))
  .sort()
  .map((arquivo) => path.join(pastaTestes, arquivo));

const existe = async (caminho) => {
  try {
    await access(caminho);
    return true;
  } catch {
    return false;
  }
};

const localizarBackendIrmao = async () => {
  if (process.env.SGT_BACKEND_DIR) return path.resolve(process.env.SGT_BACKEND_DIR);

  const pastaPai = path.dirname(raiz);
  const entradas = await readdir(pastaPai, { withFileTypes: true });
  const candidatos = entradas
    .filter((entrada) => entrada.isDirectory() && path.join(pastaPai, entrada.name) !== raiz)
    .map((entrada) => path.join(pastaPai, entrada.name));

  for (const candidato of candidatos) {
    const [temPom, temApplicationProperties] = await Promise.all([
      existe(path.join(candidato, "pom.xml")),
      existe(path.join(candidato, "src", "main", "resources", "application.properties")),
    ]);
    if (temPom && temApplicationProperties) return candidato;
  }
  return null;
};

const ambienteTestes = { ...process.env };
const backendDetectado = await localizarBackendIrmao();
if (backendDetectado) {
  ambienteTestes.SGT_BACKEND_DIR = backendDetectado;
  console.log(`Back-end detectado para os testes integrados: ${backendDetectado}`);
} else if (!process.env.SGT_BACKEND_DIR) {
  console.warn("Aviso: back-end irmão não encontrado. Defina SGT_BACKEND_DIR para executar testes integrados de contrato.");
}

if (arquivos.length === 0) {
  console.error("Nenhum arquivo *.test.js foi encontrado.");
  process.exit(1);
}

await mkdir(pastaTemporaria, { recursive: true });
ambienteTestes.TMPDIR = pastaTemporaria;
ambienteTestes.TMP = pastaTemporaria;
ambienteTestes.TEMP = pastaTemporaria;
const argumentos = ["--experimental-test-coverage", "--test", "--test-reporter=tap", ...arquivos];
const filho = spawn(process.execPath, argumentos, { cwd: raiz, env: ambienteTestes });
let saida = "";
let erro = "";
filho.stdout.on("data", (chunk) => { const texto = chunk.toString(); saida += texto; process.stdout.write(texto); });
filho.stderr.on("data", (chunk) => { const texto = chunk.toString(); erro += texto; process.stderr.write(texto); });
const codigo = await new Promise((resolve) => filho.on("close", resolve));

const obterNumero = (nome) => {
  const match = saida.match(new RegExp(`^# ${nome} (\\d+)`, "m"));
  return match ? Number(match[1]) : 0;
};
const casos = [];
for (const linha of saida.split(/\r?\n/)) {
  const match = linha.match(/^(ok|not ok)\s+\d+\s+-\s+(.+)$/);
  if (match) casos.push({ status: match[1] === "ok" ? "PASSOU" : "FALHOU", nome: match[2].trim() });
}
const duracaoMatch = saida.match(/^# duration_ms ([\d.]+)/m);
const coberturaInicio = saida.indexOf("# start of coverage report");
const coberturaFim = saida.indexOf("# end of coverage report");
const cobertura = coberturaInicio >= 0
  ? saida.slice(coberturaInicio, coberturaFim > coberturaInicio ? coberturaFim : undefined)
      .split(/\r?\n/).filter((linha) => linha.startsWith("#")).slice(1, -1)
  : [];

const resumo = {
  geradoEm: new Date().toISOString(),
  comando: "npm run test:integridade",
  node: process.version,
  arquivosDeTeste: arquivos.length,
  testes: obterNumero("tests"),
  aprovados: obterNumero("pass"),
  falhos: obterNumero("fail"),
  ignorados: obterNumero("skipped"),
  cancelados: obterNumero("cancelled"),
  duracaoMs: duracaoMatch ? Number(duracaoMatch[1]) : null,
  codigoSaida: codigo,
  backendDir: ambienteTestes.SGT_BACKEND_DIR || null,
  casos,
  cobertura,
  stderr: erro.trim() || null,
};

await writeFile(path.join(pastaRelatorios, "resultados-react.json"), `${JSON.stringify(resumo, null, 2)}\n`, "utf8");
const secoes = [
  { texto: "RELATORIO DE INTEGRIDADE ESTATICA - REACT / CONTRATO JAVA", tamanho: 18, negrito: true, espacoDepois: 10 },
  { texto: `Gerado em: ${new Date().toLocaleString("pt-BR")}` },
  { texto: `Comando: ${resumo.comando}` },
  { texto: `Runtime: Node ${resumo.node}` },
  { texto: `Arquivos de teste: ${resumo.arquivosDeTeste}` },
  { texto: `Resultado: ${codigo === 0 ? "APROVADO" : "REPROVADO"}`, tamanho: 14, negrito: true, espacoAntes: 8, espacoDepois: 8 },
  { texto: `Testes: ${resumo.testes} | Passaram: ${resumo.aprovados} | Falharam: ${resumo.falhos} | Ignorados: ${resumo.ignorados}` },
  { texto: `Duracao: ${resumo.duracaoMs ?? "nao informada"} ms` },
  { texto: "CENARIOS EXECUTADOS", tamanho: 13, negrito: true, espacoAntes: 12, espacoDepois: 4 },
  ...casos.map((caso, indice) => ({ texto: `${indice + 1}. [${caso.status}] ${caso.nome}`, recuo: 8 })),
];
if (cobertura.length) {
  secoes.push({ texto: "COBERTURA NODE", tamanho: 13, negrito: true, espacoAntes: 12, espacoDepois: 4 });
  secoes.push(...cobertura.map((linha) => ({ texto: linha.replace(/^#\s?/, ""), recuo: 8, limite: 100, tamanho: 9, alturaLinha: 11 })));
}
if (erro.trim()) {
  secoes.push({ texto: "SAIDA DE ERRO", tamanho: 13, negrito: true, espacoAntes: 12 });
  secoes.push({ texto: erro, recuo: 8, limite: 100 });
}
await gerarPdfTexto({
  destino: path.join(pastaRelatorios, "relatorio-testes-react.pdf"),
  titulo: "Relatorio de Integridade Estatica React SGT",
  secoes,
});
console.log(`\nRelatorios gerados em ${path.relative(raiz, pastaRelatorios)}.`);
process.exitCode = codigo;
