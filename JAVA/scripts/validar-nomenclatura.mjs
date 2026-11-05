import { readFileSync, readdirSync, existsSync } from "node:fs";
import path from "node:path";

const raiz = path.resolve(import.meta.dirname, "..");
const raizJava = path.join(raiz, "src", "main", "java");
const raizPacote = path.join(raizJava, "com", "example", "siggtrat");
const falhas = [];

const listar = (diretorio, extensao) => {
  const resultado = [];
  for (const entrada of readdirSync(diretorio, { withFileTypes: true })) {
    const caminho = path.join(diretorio, entrada.name);
    if (entrada.isDirectory()) resultado.push(...listar(caminho, extensao));
    else if (entrada.name.endsWith(extensao)) resultado.push(caminho);
  }
  return resultado;
};

const arquivos = listar(raizJava, ".java");
const pacotesAntigos = /com\.example\.siggtrat\.(?:config|controller|model|repository|security|service|util)(?:\.|;)/;
const aliasesNativosInvalidos = [
  /\bFiles\.(?:criarDiretorios|excluirSeExistir)\s*\(/,
  /\b(?:Integer|Double)\.interpretar[A-Za-z]*\s*\(/,
  /\b(?:Number|Byte|Short|Integer|Long|Float|Double)\b[^;\n]*\.valorLong\s*\(/,
  /\bTransactionSynchronizationManager\.registrarSincronizacao\s*\(/,
  /\bConcurrentHashMap\.novoConjuntoChaves\s*\(/,
  /\.estaVazio\s*\(/,
];
const tabelasAntigas = /\b(?:app_user|audit_log|rate_card|timesheet)\b/i;
const diretoriosAntigos = new Set(["config", "controller", "model", "repository", "security", "service", "util"]);

for (const entrada of readdirSync(raizPacote, { withFileTypes: true })) {
  if (entrada.isDirectory() && diretoriosAntigos.has(entrada.name)) {
    falhas.push(`Pacote antigo encontrado: ${entrada.name}`);
  }
}

for (const arquivo of arquivos) {
  const relativo = path.relative(raiz, arquivo);
  const fonte = readFileSync(arquivo, "utf8");
  const tipoPublico = fonte.match(
    /public\s+(?:(?:abstract|final|sealed|non-sealed)\s+)*(?:class|interface|record|enum|@interface)\s+(\w+)/,
  );
  if (tipoPublico && tipoPublico[1] !== path.basename(arquivo, ".java")) {
    falhas.push(`${relativo}: tipo público ${tipoPublico[1]} difere do nome do arquivo`);
  }
  if (pacotesAntigos.test(fonte)) falhas.push(`${relativo}: referência a pacote antigo`);
  if (tabelasAntigas.test(fonte)) falhas.push(`${relativo}: nome antigo de tabela`);
  if (/\b(?:legacy|legado|legada|legados|legadas)\b/i.test(fonte)) {
    falhas.push(`${relativo}: código ou comentário legado`);
  }
  for (const padrao of aliasesNativosInvalidos) {
    if (padrao.test(fonte)) falhas.push(`${relativo}: API nativa traduzida indevidamente (${padrao})`);
  }

  for (const resultado of fonte.matchAll(/^import\s+(com\.example\.siggtrat\.[\w.]+);/gm)) {
    const partes = resultado[1].split(".");
    const inicio = partes.indexOf("siggtrat") + 1;
    let resolvido = false;
    for (let limite = partes.length; limite > inicio; limite -= 1) {
      const candidato = path.join(raizPacote, ...partes.slice(inicio, limite - 1), `${partes[limite - 1]}.java`);
      if (existsSync(candidato)) {
        resolvido = true;
        break;
      }
    }
    if (!resolvido) falhas.push(`${relativo}: import local não resolvido: ${resultado[1]}`);
  }
}

const entidades = arquivos.filter((arquivo) =>
  arquivo.includes(`${path.sep}entidade${path.sep}`) && /@Entity\b/.test(readFileSync(arquivo, "utf8")),
);
for (const arquivo of entidades) {
  const fonte = readFileSync(arquivo, "utf8");
  const tabela = fonte.match(/@Table\s*\([\s\S]*?\bname\s*=\s*"([^"]+)"/);
  if (!tabela) {
    falhas.push(`${path.relative(raiz, arquivo)}: entidade sem nome explícito de tabela`);
  } else if (!/^[a-z0-9_]+$/.test(tabela[1])) {
    falhas.push(`${path.relative(raiz, arquivo)}: tabela fora do padrão pt-BR snake_case: ${tabela[1]}`);
  }
}

console.log(`Arquivos Java verificados: ${arquivos.length}`);
console.log(`Entidades com tabela explícita: ${entidades.length}`);
if (falhas.length) {
  console.error(`Falhas de nomenclatura: ${falhas.length}`);
  for (const falha of falhas) console.error(`- ${falha}`);
  process.exit(1);
}
console.log("Validação de nomenclatura Java aprovada.");
