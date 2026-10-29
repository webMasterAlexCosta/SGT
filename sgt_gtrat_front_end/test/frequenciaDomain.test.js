import test from "node:test";
import assert from "node:assert/strict";
import {
  agruparFaltasNoTopo,
  apenasDigitosDeBusca,
  calcularEsperaPolling,
  calcularPercentualFechamento,
  criarIndiceBusca,
  fechamentoEstaAtivo,
  fechamentoTerminou,
  filtrarFrequenciasPorTermo,
  montarAlteracaoFechamento,
  montarPayloadFechamento,
  normalizarStatusFrequencia,
  normalizarTextoDeBusca,
} from "../src/services/rh/ServicoDominioFrequencia.js";

test("normaliza acentos, caixa, pontuação e espaços na busca", () => {
  assert.equal(normalizarTextoDeBusca("  João  D'Ávila - 123  "), "joao d avila 123");
});

test("normalização aceita nulo sem lançar erro", () => {
  assert.equal(normalizarTextoDeBusca(null), "");
  assert.equal(normalizarTextoDeBusca(undefined), "");
});

test("extração numérica remove máscara de CPF e matrícula", () => {
  assert.equal(apenasDigitosDeBusca("123.456.789-00"), "12345678900");
  assert.equal(apenasDigitosDeBusca("8.961.474-5"), "89614745");
});

test("índice reúne nome, CPF e matrícula em formatos pesquisáveis", () => {
  const indice = criarIndiceBusca({ nome: "Érica Lima", cpf: "123.456.789-00", matricula: "8.961.474-5" });
  assert.match(indice, /erica lima/);
  assert.match(indice, /12345678900/);
  assert.match(indice, /89614745/);
});

test("busca vazia devolve a mesma referência da lista", () => {
  const lista = [{ nome: "A" }];
  assert.equal(filtrarFrequenciasPorTermo(lista, "  "), lista);
});

test("busca ignora acentuação", () => {
  const lista = [{ nome: "José Álvares", cpf: "", matricula: "" }];
  assert.equal(filtrarFrequenciasPorTermo(lista, "jose alvares").length, 1);
});

test("busca por CPF funciona com ou sem máscara", () => {
  const lista = [{ nome: "Ana", cpf: "111.222.333-44", matricula: "" }];
  assert.equal(filtrarFrequenciasPorTermo(lista, "11122233344").length, 1);
  assert.equal(filtrarFrequenciasPorTermo(lista, "111.222").length, 1);
});

test("busca exige que todas as palavras estejam presentes", () => {
  const lista = [
    { nome: "Maria Souza", cpf: "111", matricula: "999" },
    { nome: "Maria Lima", cpf: "222", matricula: "888" },
  ];
  const resultado = filtrarFrequenciasPorTermo(lista, "maria 999");
  assert.deepEqual(resultado.map((item) => item.nome), ["Maria Souza"]);
});

test("busca usa índice pré-calculado quando presente", () => {
  const lista = [{ nome: "Não deveria", indiceBusca: "atalho 555" }];
  assert.equal(filtrarFrequenciasPorTermo(lista, "atalho 555").length, 1);
});

test("faltas e atestados sobem ao topo preservando a ordem relativa", () => {
  const a = { id: 1, status: "PRESENCA" };
  const b = { id: 2, status: "FALTA" };
  const c = { id: 3, status: "FALTA" };
  const d = { id: 4, status: "ATESTADO" };
  const resultado = agruparFaltasNoTopo([a, b, c, d]);
  assert.deepEqual(resultado.listaOrdenada.map((item) => item.id), [2, 3, 4, 1]);
  assert.equal(resultado.totalFaltas, 2);
  assert.equal(resultado.totalAtestados, 1);
});

test("agrupamento reconhece falta em caixa baixa e com espaços", () => {
  const resultado = agruparFaltasNoTopo([{ id: 1, status: " falta " }]);
  assert.equal(resultado.totalFaltas, 1);
});

test("agrupamento lida com lista vazia ou inválida", () => {
  assert.deepEqual(agruparFaltasNoTopo([]), { listaOrdenada: [], totalFaltas: 0, totalAtestados: 0 });
  assert.deepEqual(agruparFaltasNoTopo(null), { listaOrdenada: [], totalFaltas: 0, totalAtestados: 0 });
});

test("normaliza status antes do envio", () => {
  assert.equal(normalizarStatusFrequencia(" falta "), "FALTA");
});

test("monta alteração completa e converte vírgula decimal", () => {
  const alteracao = montarAlteracaoFechamento({
    empregadoId: 7,
    status: " presenca_parcial ",
    obs: "  saída antecipada  ",
    entrada1: "08:00",
    saida1: "12:00",
    horasTrabalhadas: "4,5",
    horasNoturnas: 1.25,
  }, "2026-08-06");
  assert.deepEqual(alteracao.empregado, { id: 7 });
  assert.equal(alteracao.status, "PRESENCA_PARCIAL");
  assert.equal(alteracao.observacao, "saída antecipada");
  assert.equal(alteracao.horasTrabalhadas, "4.5");
  assert.equal(alteracao.horasNoturnas, 1.25);
  assert.equal(alteracao.saida2, null);
});

test("alteração sem empregado é rejeitada", () => {
  assert.throws(() => montarAlteracaoFechamento({ status: "FALTA" }, "2026-08-06"), /empregadoId/);
});

test("payload deduplica e ordena contratos", () => {
  const payload = montarPayloadFechamento({
    dataReferencia: "2026-08-06",
    lotacaoId: "10",
    jornadaId: "20",
    contratoIds: [3, "1", 3, "2"],
    registrosPendentes: [],
  });
  assert.deepEqual(payload.contratoIds, [1, 2, 3]);
  assert.equal(payload.lotacaoId, 10);
  assert.equal(payload.jornadaId, 20);
});

test("payload envia apenas registros explicitamente alterados", () => {
  const payload = montarPayloadFechamento({
    dataReferencia: "2026-08-06",
    lotacaoId: 1,
    jornadaId: 2,
    registrosPendentes: [{ empregadoId: 9, status: "FALTA" }],
  });
  assert.equal(payload.alteracoes.length, 1);
  assert.equal(payload.alteracoes[0].empregado.id, 9);
});

test("payload rejeita escopo incompleto", () => {
  assert.throws(() => montarPayloadFechamento({ dataReferencia: "2026-08-06", lotacaoId: 1 }), /obrigatórios/);
});

test("identifica estados ativos do fechamento", () => {
  assert.equal(fechamentoEstaAtivo({ status: "PENDENTE" }), true);
  assert.equal(fechamentoEstaAtivo({ status: "processando" }), true);
  assert.equal(fechamentoEstaAtivo({ status: "CONCLUIDO" }), false);
});

test("identifica estados terminais do fechamento", () => {
  for (const status of ["CONCLUIDO", "ERRO", "CANCELADO"]) {
    assert.equal(fechamentoTerminou({ status }), true);
  }
  assert.equal(fechamentoTerminou({ status: "PROCESSANDO" }), false);
});

test("percentual informado é arredondado e limitado", () => {
  assert.equal(calcularPercentualFechamento({ percentual: 34.6 }), 35);
  assert.equal(calcularPercentualFechamento({ percentual: 180 }), 100);
  assert.equal(calcularPercentualFechamento({ percentual: -2 }), 0);
});

test("percentual é calculado pelos totais quando não vem do backend", () => {
  assert.equal(calcularPercentualFechamento({ totalPrevisto: 100, totalProcessado: 33 }), 33);
  assert.equal(calcularPercentualFechamento({ totalPrevisto: 0, totalProcessado: 33 }), 0);
});

test("espera de polling cresce gradualmente sem falha", () => {
  assert.equal(calcularEsperaPolling({ tentativas: 0 }), 1000);
  assert.equal(calcularEsperaPolling({ tentativas: 4 }), 2000);
  assert.equal(calcularEsperaPolling({ tentativas: 100 }), 3000);
});

test("espera de polling aplica backoff em falhas e respeita teto", () => {
  assert.equal(calcularEsperaPolling({ falhasConsecutivas: 1 }), 2000);
  assert.equal(calcularEsperaPolling({ falhasConsecutivas: 2 }), 4000);
  assert.equal(calcularEsperaPolling({ falhasConsecutivas: 10 }), 15000);
});

test("agrupamento de 100 mil linhas permanece linear e íntegro", () => {
  const lista = Array.from({ length: 100_000 }, (_, indice) => ({
    id: indice + 1,
    status: indice % 1000 === 0 ? "FALTA" : "PRESENCA",
  }));
  const inicio = performance.now();
  const resultado = agruparFaltasNoTopo(lista);
  const duracao = performance.now() - inicio;
  assert.equal(resultado.listaOrdenada.length, 100_000);
  assert.equal(resultado.totalFaltas, 100);
  assert.equal(resultado.listaOrdenada[0].status, "FALTA");
  assert.ok(duracao < 2000, `Agrupamento levou ${duracao.toFixed(1)} ms`);
});
