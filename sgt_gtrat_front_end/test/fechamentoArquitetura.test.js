import test from "node:test";
import assert from "node:assert/strict";
import { readFile } from "node:fs/promises";

const raiz = new URL("../", import.meta.url);
const ler = (caminho) => readFile(new URL(caminho, raiz), "utf8");
const trechoEnvioCanonico = (logica) => {
  const inicio = logica.indexOf("const enviarPendentes = useCallback");
  const fim = logica.indexOf("const invocarGatilhoDeDespachoAoServidorGlobal", inicio);
  assert.ok(inicio >= 0 && fim > inicio, "bloco canônico enviarPendentes não encontrado");
  return logica.slice(inicio, fim);
};


test("gravação da grade continua canônica mesmo com fechamento assíncrono integrado na Etapa 14", async () => {
  const logica = await ler("src/modules/rh/pages/PaginaFrequencia/useLogicaFrequencia.js");
  assert.match(logica, /montarPayloadFrequenciaCanonica/);
  assert.match(logica, /obterRegistrosPendentes\(\)/);
  assert.match(logica, /ServicoFrequencia\.registrar\(/);
  assert.match(logica, /ServicoFrequencia\.registrarLote\(/);
  assert.match(logica, /TAMANHO_MAXIMO_LOTE_FREQUENCIA/);
  assert.doesNotMatch(trechoEnvioCanonico(logica), /criarFechamento|acompanharFechamento|buscarFechamentoAtual/);
});

test("domínio testável é usado pelo hook e pela tabela", async () => {
  const [logica, tabela] = await Promise.all([
    ler("src/modules/rh/pages/PaginaFrequencia/useLogicaFrequencia.js"),
    ler("src/modules/rh/pages/PaginaFrequencia/components/TabelaFrequencia.jsx"),
  ]);
  assert.match(logica, /filtrarFrequenciasPorTermo/);
  assert.match(logica, /criarIndiceBusca/);
  assert.match(tabela, /agruparFaltasNoTopo/);
});

test("serviço mantém fechamento assíncrono separado da gravação explícita da grade diária", async () => {
  const servico = await ler("src/services/rh/ServicoFrequencia.js");
  for (const nome of ["criarFechamento", "buscarFechamento", "buscarFechamentoAtual", "cancelarFechamento", "reprocessarFechamento"]) {
    assert.match(servico, new RegExp(nome));
  }
});

test("paginação runtime foi separada para teste e mantém limite configurado", async () => {
  const [adaptador, core] = await Promise.all([
    ler("src/services/rh/paginacao.js"),
    ler("src/services/rh/nucleoPaginacao.js"),
  ]);
  assert.match(adaptador, /carregarTodasAsPaginas/);
  assert.match(core, /Promise\.all/);
  assert.match(core, /totalElements > maximoItens/);
});

test("suíte separa testes Vitest dos relatórios de integridade estática", async () => {
  const [pkg, runner] = await Promise.all([
    ler("package.json"),
    ler("scripts/run-tests.mjs"),
  ]);
  assert.match(pkg, /"test"\s*:\s*"vitest run --config vitest\.config\.js --coverage"/);
  assert.match(pkg, /"test:integridade"\s*:\s*"node scripts\/run-tests\.mjs"/);
  assert.match(runner, /relatorio-testes-react\.pdf/);
  assert.match(runner, /resultados-react\.json/);
});
