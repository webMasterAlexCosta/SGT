import test from "node:test";
import assert from "node:assert/strict";
import { readFile } from "node:fs/promises";
import {
  agruparFaltasNoTopo,
  dividirEmLotesFrequencia,
  montarPayloadFrequenciaCanonica,
} from "../src/services/rh/ServicoDominioFrequencia.js";

const raiz = new URL("../", import.meta.url);
const ler = (caminho) => readFile(new URL(caminho, raiz), "utf8");
const trechoEnvioCanonico = (logica) => {
  const inicio = logica.indexOf("const enviarPendentes = useCallback");
  const fim = logica.indexOf("const invocarGatilhoDeDespachoAoServidorGlobal", inicio);
  assert.ok(inicio >= 0 && fim > inicio, "bloco canônico enviarPendentes não encontrado");
  return logica.slice(inicio, fim);
};


test("Etapa 14 preserva POST individual/lote canônico e não usa fechamento para gravar a grade", async () => {
  const [logica, servico] = await Promise.all([
    ler("src/modules/rh/pages/PaginaFrequencia/useLogicaFrequencia.js"),
    ler("src/services/rh/ServicoFrequencia.js"),
  ]);
  assert.match(logica, /ServicoFrequencia\.registrar\(/);
  assert.match(logica, /ServicoFrequencia\.registrarLote\(/);
  assert.doesNotMatch(trechoEnvioCanonico(logica), /criarFechamento|acompanharFechamento|buscarFechamentoAtual/);
  assert.match(servico, /registrar:[\s\S]{0,180}?retries:\s*0/);
  assert.match(servico, /registrarLote:[\s\S]{0,180}?retries:\s*0/);
});

test("payload público nunca usa empregado.id como identidade de escrita", () => {
  const payload = montarPayloadFrequenciaCanonica({
    linha: { empregadoId: 8, vinculoContratualId: 77, status: "FALTA" },
    dataReferencia: "2026-09-11",
  });
  assert.equal(payload.vinculoContratualId, 77);
  assert.equal("empregado" in payload, false);
  assert.throws(() => montarPayloadFrequenciaCanonica({
    linha: { empregadoId: 8, status: "FALTA" },
    dataReferencia: "2026-09-11",
  }), /vinculoContratualId/);
});

test("projeção de empregados é paginada no servidor em 250 e recebe searchTerm", async () => {
  const logica = await ler("src/modules/rh/pages/PaginaFrequencia/useLogicaFrequencia.js");
  assert.match(logica, /TAMANHO_PAGINA_GRADE\s*=\s*250/);
  assert.match(logica, /buscarPaginadosParaFrequencia/);
  assert.match(logica, /searchTerm:\s*termoDeBuscaDiferido/);
  assert.match(logica, /page:\s*paginaGrade/);
  assert.doesNotMatch(logica, /buscarTodosParaFrequencia/);
});

test("prazoEdicao é apenas informativo e não autoriza mutação no navegador", async () => {
  const [logica, tabela] = await Promise.all([
    ler("src/modules/rh/pages/PaginaFrequencia/useLogicaFrequencia.js"),
    ler("src/modules/rh/pages/PaginaFrequencia/components/TabelaFrequencia.jsx"),
  ]);
  assert.doesNotMatch(logica, /validarSeEstaNaJanelaDeSeguranca/);
  assert.match(logica, /frequenciaBloqueadaParaOperacional/);
  assert.match(tabela, /edicaoBloqueadaPeloBackend/);
  assert.doesNotMatch(tabela, /!detalhesJanela\.isValid/);
});

test("linha sem lançamento permanece pendente e não recebe PRESENCA implícita", async () => {
  const [logica, tabela] = await Promise.all([
    ler("src/modules/rh/pages/PaginaFrequencia/useLogicaFrequencia.js"),
    ler("src/modules/rh/pages/PaginaFrequencia/components/TabelaFrequencia.jsx"),
  ]);
  assert.match(logica, /status:\s*logFrequencia\?\.status\s*\|\|\s*null/);
  assert.match(logica, /estadoLancamento:\s*logFrequencia\s*\?\s*"LANCADO"\s*:\s*"PENDENTE"/);
  assert.match(tabela, /Vínculo será resolvido ao salvar/);
});

test("turno noturno preserva dataReferencia sem deslocamento de dia", async () => {
  const logica = await ler("src/modules/rh/pages/PaginaFrequencia/useLogicaFrequencia.js");
  assert.match(logica, /dataReferencia:\s*dataReferenciaAtiva/);
  assert.match(logica, /data permanece exatamente a data de início da jornada/);
});

test("lotes grandes são limitados a 1000 e podem representar mais de 40 mil alterações", () => {
  const itens = Array.from({ length: 40_001 }, (_, i) => i);
  const lotes = dividirEmLotesFrequencia(itens);
  assert.equal(lotes.length, 41);
  assert.equal(lotes[0].length, 1000);
  assert.equal(lotes.at(-1).length, 1);
});

test("40 mil linhas permanecem processáveis linearmente sem expansão do DOM", async () => {
  const tabela = await ler("src/modules/rh/pages/PaginaFrequencia/components/TabelaFrequencia.jsx");
  const lista = Array.from({ length: 40_000 }, (_, indice) => ({ status: indice % 997 === 0 ? "FALTA" : "PRESENCA" }));
  const resultado = agruparFaltasNoTopo(lista);
  assert.equal(resultado.listaOrdenada.length, 40_000);
  assert.match(tabela, /virtualizacaoObrigatoria\s*=\s*listaDeFrequencia\.length\s*>=\s*150/);
  assert.match(tabela, /ListaVirtualNativa/);
});

test("falhas 403 e parciais preservam mensagem do backend e não têm retry cego", async () => {
  const logica = await ler("src/modules/rh/pages/PaginaFrequencia/useLogicaFrequencia.js");
  assert.match(logica, /erro\?\.response\?\.data\?\.message/);
  assert.match(logica, /Não houve retry automático/);
  assert.match(logica, /retries:\s*0/);
  assert.match(logica, /progressoFrequencia/);
});
