import test from "node:test";
import assert from "node:assert/strict";
import { readFile } from "node:fs/promises";
import {
  apurarMinutosRelatorio,
  calcularDiaDaJornada,
  calcularJornadaClt,
  minutosParaDuracao,
} from "../src/services/rh/ServicoDominioRelatorioFrequencia.js";
import { formatarLinhaRelatorio } from "../src/services/rh/ServicoDominioRelatorioFrequencia.js";
import { obterSiglaStatus } from "../src/modules/rh/pages/PaginaRelatorioFrequencia/utils.js";

test("turno integral 22h-05h gera 7h reais e 8h noturnas reduzidas", () => {
  const calculo = calcularJornadaClt({ entrada1: "22:00", saida1: "05:00" });

  assert.equal(calculo.minutosFisicos, 420);
  assert.equal(calculo.minutosNoturnosReais, 420);
  assert.equal(calculo.minutosNoturnosReduzidos, 480);
  assert.equal(minutosParaDuracao(calculo.minutosFaturaveis), "08:00");
});

test("turno 22h-06h com intervalo gera 7h físicas e 8h faturáveis", () => {
  const calculo = calcularJornadaClt({
    entrada1: "22:00",
    saida1: "01:00",
    entrada2: "02:00",
    saida2: "06:00",
  });

  assert.equal(calculo.minutosFisicos, 420);
  assert.equal(calculo.minutosNoturnosReais, 420);
  assert.equal(calculo.horasNoturnasReduzidas, 8);
  assert.equal(minutosParaDuracao(calculo.minutosFaturaveis), "08:00");
  assert.equal(calculo.prorrogacaoNoturna, true);
});

test("jornada iniciada à 01h não recebe prorrogação noturna depois das 05h", () => {
  const calculo = calcularJornadaClt({ entrada1: "01:00", saida1: "06:00" });

  assert.equal(calculo.minutosNoturnosReais, 240);
  assert.equal(calculo.prorrogacaoNoturna, false);
  assert.equal(minutosParaDuracao(calculo.minutosFaturaveis), "05:34");
});

test("localiza a configuração noturna pelo dia da semana", () => {
  const calculo = calcularDiaDaJornada({
    dias: [{
      diaSemana: "MONDAY",
      diaTrabalho: true,
      entrada1: "22:00",
      saida1: "01:00",
      entrada2: "02:00",
      saida2: "06:00",
    }],
  }, "2026-08-10");

  assert.equal(calculo.minutosFisicos, 420);
  assert.equal(calculo.minutosFaturaveis, 480);
});

test("relatório usa a escala oficial em presença automática e ignora total inconsistente", () => {
  const calculoPrevisto = calcularJornadaClt({
    entrada1: "22:00",
    saida1: "02:00",
    entrada2: "03:00",
    saida2: "06:00",
  });
  const apuracao = apurarMinutosRelatorio({
    frequencia: {
      status: "PRESENCA",
      horasTrabalhadas: "08:14",
      horasNoturnas: 8.23,
    },
    calculoPrevisto,
  });

  assert.equal(apuracao.origem, "ESCALA");
  assert.equal(apuracao.minutosFisicos, 420);
  assert.equal(apuracao.minutosFaturaveis, 480);
  assert.equal(apuracao.minutosNoturnosReduzidos, 480);
});

test("cinco presenças automáticas do turno 22h-06h totalizam 40h noturnas", () => {
  const calculoPrevisto = calcularJornadaClt({
    entrada1: "22:00",
    saida1: "02:00",
    entrada2: "03:00",
    saida2: "06:00",
  });
  const total = Array.from({ length: 5 }).reduce((minutos) => minutos + apurarMinutosRelatorio({
    frequencia: { status: "PRESENCA", horasTrabalhadas: "08:14", horasNoturnas: 8.23 },
    calculoPrevisto,
  }).minutosNoturnosReduzidos, 0);

  assert.equal(minutosParaDuracao(total), "40:00");
});

test("relatório aceita consolidação sem filtros e exige apuração oficial completa", async () => {
  const [pagina, modelo, servico] = await Promise.all([
    readFile(new URL("../src/modules/rh/pages/PaginaRelatorioFrequencia/index.jsx", import.meta.url), "utf8"),
    readFile(new URL("../src/services/rh/ServicoDominioRelatorioFrequencia.js", import.meta.url), "utf8"),
    readFile(new URL("../src/services/rh/ServicoFrequencia.js", import.meta.url), "utf8"),
  ]);

  assert.doesNotMatch(pagina, /Selecione ao menos contrato, lotação ou turno/);
  assert.match(pagina, /c\?\.ativo !== false/);
  assert.match(pagina, /ServicoFrequencia\.carregarFiltrosRelatorio\(\)/);
  assert.match(pagina, /ServicoFrequencia\.gerarRelatorioConsolidado/);
  assert.match(servico, /\/relatorios\/frequencia-consolidada/);
  assert.match(pagina, /resposta\?\.completo !== true/);
  assert.doesNotMatch(pagina, /ServicoFrequencia\.buscarIntervalo/);
  assert.match(modelo, /horasNoturnas:/);
  assert.match(modelo, /totalNoturnoDecimal:/);
});

test("linha consolidada mantém turno, descanso e separa horas reais das pagáveis", () => {
  const linha = formatarLinhaRelatorio({
    chaveAlocacao: "1:2:3",
    nome: "Empregado Noturno",
    turno: "Noturno 22h às 05h",
    diario: {
      "2026-08-10": { status: "PRESENCA", minutosPagaveis: 480 },
      "2026-08-11": { status: "DESCANSO", minutosPagaveis: 0 },
    },
    totais: {
      presencas: 1,
      descansos: 1,
      minutosFisicos: 420,
      minutosNoturnosReduzidos: 480,
      minutosPagaveis: 480,
    },
  });

  assert.equal(linha.turno, "Noturno 22h às 05h");
  assert.equal(linha.diario["2026-08-11"].sigla, "D");
  assert.equal(linha.totais.descansos, 1);
  assert.equal(linha.totais.horasFisicas, "07:00");
  assert.equal(linha.totais.horasNoturnas, "08:00");
  assert.equal(linha.totais.horasPagaveis, "08:00");
});

test("feriado consolidado mantém sigla FER na tela e nas exportações", () => {
  assert.equal(obterSiglaStatus("FERIADO", false), "FER");

  const linha = formatarLinhaRelatorio({
    chaveAlocacao: "1:2:3",
    nome: "Empregado em feriado",
    diario: {
      "2026-08-15": { status: "FERIADO", minutosPagaveis: 0 },
    },
    totais: { descansos: 1 },
  });

  assert.equal(linha.diario["2026-08-15"].sigla, "FER");
  assert.equal(linha.totais.descansos, 1);
});
