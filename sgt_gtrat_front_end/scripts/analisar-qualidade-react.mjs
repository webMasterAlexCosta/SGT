import fs from "node:fs";
import path from "node:path";

const raiz = process.cwd();
const src = path.join(raiz, "src");
const reports = path.join(raiz, "reports");
fs.mkdirSync(reports, { recursive: true });

const listar = (dir) => fs.readdirSync(dir, { withFileTypes: true }).flatMap((entrada) => {
  const completo = path.join(dir, entrada.name);
  if (entrada.isDirectory()) return listar(completo);
  return /\.(?:js|jsx)$/.test(entrada.name) ? [completo] : [];
});

const arquivos = listar(src);
const achados = [];
const adicionar = (nivel, regra, arquivo, detalhe, linha = null) => achados.push({
  nivel,
  regra,
  arquivo: path.relative(raiz, arquivo).replaceAll("\\", "/"),
  linha,
  detalhe,
});

for (const arquivo of arquivos) {
  const texto = fs.readFileSync(arquivo, "utf8");
  const linhas = texto.split(/\r?\n/);
  const relativo = path.relative(src, arquivo).replaceAll("\\", "/");
  const ehUi = /^(?:modules|Leiaute)\//.test(relativo);

  linhas.forEach((linhaTexto, indice) => {
    const numero = indice + 1;
    if (ehUi && /\bfetch\s*\(|\baxios\.|\bapi\.(?:get|post|put|patch|delete|request)\s*\(/.test(linhaTexto)) {
      adicionar("ERRO", "HTTP_DIRETO_NA_UI", arquivo, "UI/hook não deve acessar transporte HTTP diretamente; centralize em services.", numero);
    }
    if (/useEffect\s*\(\s*async\b/.test(linhaTexto)) {
      adicionar("ERRO", "USE_EFFECT_ASYNC", arquivo, "useEffect não deve receber função async diretamente; crie função interna e trate cleanup/cancelamento.", numero);
    }
    if (/dangerouslySetInnerHTML/.test(linhaTexto)) {
      adicionar("AVISO", "HTML_DINAMICO", arquivo, "Revisar sanitização de conteúdo usado em dangerouslySetInnerHTML.", numero);
    }
    if (/key=\{\s*(?:index|indice|i)\s*\}/.test(linhaTexto)) {
      adicionar("AVISO", "KEY_POR_INDICE", arquivo, "Key por índice pode causar reconciliação incorreta em listas mutáveis; prefira identificador estável.", numero);
    }
    if (/\b(?:lista|dados|itens|registros|usuarios|contratos|lotacoes|turnos)\.sort\s*\(/.test(linhaTexto) && !/\[\.\.\.|\.slice\(\)\.sort/.test(linhaTexto)) {
      adicionar("AVISO", "SORT_POTENCIALMENTE_MUTAVEL", arquivo, "Array.sort() modifica a referência original; verificar se a coleção veio de props/state/cache.", numero);
    }
  });

  const adicionaListeners = (texto.match(/\b(?:window|document)\.addEventListener\s*\(/g) || []).length;
  const removeListeners = (texto.match(/\b(?:window|document)\.removeEventListener\s*\(/g) || []).length;
  if (adicionaListeners > 0 && removeListeners === 0) {
    adicionar("ERRO", "LISTENER_SEM_CLEANUP", arquivo, `${adicionaListeners} addEventListener sem removeEventListener no mesmo módulo.`);
  }

  const intervalos = (texto.match(/\b(?:window\.)?setInterval\s*\(/g) || []).length;
  const limpaIntervalos = (texto.match(/\b(?:window\.)?clearInterval\s*\(/g) || []).length;
  if (intervalos > 0 && limpaIntervalos === 0) {
    adicionar("ERRO", "INTERVALO_SEM_CLEANUP", arquivo, `${intervalos} setInterval sem clearInterval no mesmo módulo.`);
  }

  if (linhas.length > 1200) {
    adicionar("AVISO", "MODULO_MUITO_GRANDE", arquivo, `${linhas.length} linhas. Avaliar divisão para reduzir custo cognitivo e rerenderizações acopladas.`);
  }
}

const erros = achados.filter((item) => item.nivel === "ERRO");
const avisos = achados.filter((item) => item.nivel === "AVISO");
const resumo = {
  arquivosAnalisados: arquivos.length,
  erros: erros.length,
  avisos: avisos.length,
  achados,
};
fs.writeFileSync(path.join(reports, "qualidade-react.json"), JSON.stringify(resumo, null, 2));

const markdown = [
  "# Auditoria estática de qualidade React",
  "",
  `- Arquivos analisados: ${arquivos.length}`,
  `- Erros de gate: ${erros.length}`,
  `- Avisos de otimização: ${avisos.length}`,
  "",
  ...achados.map((item) => `- **${item.nivel}** \`${item.regra}\` — \`${item.arquivo}${item.linha ? `:${item.linha}` : ""}\`: ${item.detalhe}`),
  "",
].join("\n");
fs.writeFileSync(path.join(reports, "qualidade-react.md"), markdown);

console.log(`Auditoria React: ${arquivos.length} arquivos, ${erros.length} erros de gate, ${avisos.length} avisos.`);
if (erros.length) process.exitCode = 1;
