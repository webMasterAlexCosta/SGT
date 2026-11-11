interface ClockPeriod { inicio: number; fim: number }
interface ClockPeriodsInput { entrada1?: string | null; saida1?: string | null; entrada2?: string | null; saida2?: string | null }
interface DailyShiftConfig extends ClockPeriodsInput { diaSemana?: string | null; diaTrabalho?: boolean | null }
interface NightRule { inicioMinutos: number; fimMinutos: number; minutosPorHora: number; toleranciaPresencaMinutos: number }
import type { FrequenciaDTO, RelatorioFrequenciaConsolidadoDTOLinha, RelatorioFrequenciaConsolidadoDTODia } from '../../contracts/java';
const MINUTOS_DIA = 24 * 60;

export const REGRA_NOTURNA_CLT = Object.freeze({
  inicioMinutos: 22 * 60,
  fimMinutos: 5 * 60,
  minutosPorHora: 52.5,
  toleranciaPresencaMinutos: 10,
});

const horaParaMinutos = (valor: unknown) => {
  if (!/^([01]\d|2[0-3]):[0-5]\d$/.test(String(valor || ""))) return null;
  const [horas, minutos] = String(valor).split(":").map(Number);
  return horas * 60 + minutos;
};

const intersecao = (inicio1: number, fim1: number, inicio2: (number), fim2: (number)) =>
  Math.max(0, Math.min(fim1, fim2) - Math.max(inicio1, inicio2));

const normalizarPeriodos = (origem: ClockPeriodsInput = {}) => {
  const pares = [
    [origem.entrada1, origem.saida1],
    [origem.entrada2, origem.saida2],
  ];
  const periodos: ClockPeriod[] = [];

  pares.forEach(([entrada, saida]) => {
    let inicio = horaParaMinutos(entrada);
    let fim = horaParaMinutos(saida);
    if (inicio === null || fim === null || inicio === fim) return;
    if (fim < inicio) fim += MINUTOS_DIA;

    if (periodos.length > 0) {
      const fimAnterior = periodos[periodos.length - 1].fim;
      while (inicio < fimAnterior) {
        inicio += MINUTOS_DIA;
        fim += MINUTOS_DIA;
      }
    }
    periodos.push({ inicio, fim });
  });

  return periodos;
};

const minutosNoturnosLegais = (inicio: number, fim: number, regra: NightRule) => {
  let total = 0;
  const primeiroDia = Math.floor(inicio / MINUTOS_DIA) - 1;
  const ultimoDia = Math.floor(fim / MINUTOS_DIA) + 1;

  for (let dia = primeiroDia; dia <= ultimoDia; dia += 1) {
    const inicioJanela = dia * MINUTOS_DIA + regra.inicioMinutos;
    let fimJanela = dia * MINUTOS_DIA + regra.fimMinutos;
    if (fimJanela <= inicioJanela) fimJanela += MINUTOS_DIA;
    total += intersecao(inicio, fim, inicioJanela, fimJanela);
  }
  return total;
};

const fimPeriodoNoturnoIntegral = (periodos: ClockPeriod[], regra: NightRule) => {
  if (!periodos.length) return null;
  const primeiro = periodos[0];
  const ultimo = periodos[periodos.length - 1];
  const diaBase = Math.floor(primeiro.inicio / MINUTOS_DIA) * MINUTOS_DIA;

  for (let deslocamento = -1; deslocamento <= 1; deslocamento += 1) {
    const inicioJanela = diaBase + deslocamento * MINUTOS_DIA + regra.inicioMinutos;
    let fimJanela = diaBase + deslocamento * MINUTOS_DIA + regra.fimMinutos;
    if (fimJanela <= inicioJanela) fimJanela += MINUTOS_DIA;
    if (primeiro.inicio <= inicioJanela && ultimo.fim > fimJanela) return fimJanela;
  }
  return null;
};

export const calcularJornadaClt = (origem: ClockPeriodsInput = {}, regra: NightRule = REGRA_NOTURNA_CLT) => {
  const periodos = normalizarPeriodos(origem);
  const minutosFisicos = periodos.reduce(
    (total, periodo) => total + (periodo.fim - periodo.inicio),
    0,
  );
  let minutosNoturnosReais = periodos.reduce(
    (total, periodo) => total + minutosNoturnosLegais(periodo.inicio, periodo.fim, regra),
    0,
  );

  const fimIntegral = fimPeriodoNoturnoIntegral(periodos, regra);
  if (fimIntegral !== null) {
    const fimJornada = periodos[periodos.length - 1].fim;
    minutosNoturnosReais += periodos.reduce(
      (total, periodo) => total + intersecao(periodo.inicio, periodo.fim, fimIntegral, fimJornada),
      0,
    );
  }

  const horasNoturnasReduzidas = minutosNoturnosReais > 0
    ? minutosNoturnosReais / regra.minutosPorHora
    : 0;
  const minutosNoturnosReduzidos = Math.round(horasNoturnasReduzidas * 60);
  const bonusNoturnoMinutos = horasNoturnasReduzidas * 60 - minutosNoturnosReais;

  return {
    periodos,
    minutosFisicos,
    minutosNoturnosReais,
    horasNoturnasReduzidas,
    minutosNoturnosReduzidos,
    minutosFaturaveis: minutosFisicos + Math.round(bonusNoturnoMinutos),
    prorrogacaoNoturna: fimIntegral !== null,
  };
};

export const duracaoParaMinutos = (valor: (string | null | undefined)) => {
  const correspondencia = String(valor || "").match(/^(\d{1,3}):([0-5]\d)$/);
  if (!correspondencia) return 0;
  return Number(correspondencia[1]) * 60 + Number(correspondencia[2]);
};

export const minutosParaDuracao = (totalMinutos: (number)) => {
  const seguro = Math.max(0, Math.round(Number(totalMinutos) || 0));
  return `${String(Math.floor(seguro / 60)).padStart(2, "0")}:${String(seguro % 60).padStart(2, "0")}`;
};

/**
 * Consolida a fonte correta para o relatório sem aplicar a redução noturna
 * duas vezes. Batidas completas têm prioridade. Para uma presença automática
 * sem batidas/saída manual, a própria escala é a fonte de verdade: os campos
 * persistidos são derivados da escala oficial configurada.
 */
export const apurarMinutosRelatorio = ({
  frequencia = {},
  calculoPrevisto = calcularJornadaClt({}),
}: { frequencia?: Partial<FrequenciaDTO>; calculoPrevisto?: ReturnType<typeof calcularJornadaClt> } = {}) => {
  const status = String(frequencia?.status || "").toUpperCase();
  const calculoBatidas = calcularJornadaClt(frequencia || {});
  const possuiBatidasCompletas = calculoBatidas.minutosFisicos > 0;
  const possuiEscalaDefinida = Array.isArray(calculoPrevisto?.periodos)
    && calculoPrevisto.periodos.length > 0;
  const possuiApuracaoManual = Boolean(String(frequencia?.horarioSaida || "").trim());
  const presencaAutomatica = status === "PRESENCA"
    && !possuiBatidasCompletas
    && !possuiApuracaoManual
    && possuiEscalaDefinida;

  if (possuiBatidasCompletas) {
    return {
      minutosFisicos: calculoBatidas.minutosFisicos,
      minutosFaturaveis: calculoBatidas.minutosFaturaveis,
      minutosNoturnosReduzidos: Math.round(
        calculoBatidas.horasNoturnasReduzidas * 60,
      ),
      origem: "BATIDAS",
    };
  }

  if (presencaAutomatica) {
    return {
      minutosFisicos: calculoPrevisto.minutosFisicos,
      minutosFaturaveis: calculoPrevisto.minutosFaturaveis,
      minutosNoturnosReduzidos: Math.round(
        calculoPrevisto.horasNoturnasReduzidas * 60,
      ),
      origem: "ESCALA",
    };
  }

  const minutosSalvos = duracaoParaMinutos(frequencia?.horasTrabalhadas);
  const minutosNoturnosSalvos = Number(frequencia?.horasNoturnas) > 0
    ? Number(frequencia.horasNoturnas) * 60
    : 0;
  const minutosNoturnosPrevistos = possuiEscalaDefinida
    ? calculoPrevisto.horasNoturnasReduzidas * 60
    : 0;
  const minutosPadrao = calculoPrevisto?.minutosFaturaveis > 0
    ? calculoPrevisto.minutosFaturaveis
    : 8 * 60;

  return {
    minutosFisicos: minutosSalvos,
    minutosFaturaveis: minutosSalvos > 0
      ? (status === "PRESENCA"
        ? Math.max(minutosSalvos, minutosPadrao)
        : minutosSalvos)
      : minutosPadrao,
    minutosNoturnosReduzidos: Math.round(
      status === "PRESENCA"
        ? Math.max(minutosNoturnosSalvos, minutosNoturnosPrevistos)
        : minutosNoturnosSalvos,
    ),
    origem: possuiApuracaoManual ? "MANUAL" : "SALVO",
  };
};

export const calcularDiaDaJornada = (jornada: { dias?: DailyShiftConfig[] | null } | null | undefined, dataIso: string | null | undefined) => {
  if (!jornada || !dataIso) return calcularJornadaClt({});
  const data = new Date(`${dataIso}T12:00:00Z`);
  const dias = ["SUNDAY", "MONDAY", "TUESDAY", "WEDNESDAY", "THURSDAY", "FRIDAY", "SATURDAY"];
  const configuracao = (jornada.dias || []).find((dia) => dia.diaSemana === dias[data.getUTCDay()]);
  if (!configuracao || configuracao.diaTrabalho === false) return calcularJornadaClt({});
  return calcularJornadaClt(configuracao);
};

export const obterSiglaStatus = (status: (string), ehFeriado: (boolean)) => {
  if (!status) return "";
  switch (String(status).toUpperCase()) {
    case "PRESENCA":
    case "TR": return "P";
    case "FALTA": return "F";
    case "PRESENCA_PARCIAL": return "PP";
    case "ATESTADO": return "AT";
    case "FERIADO": return "FER";
    case "FOLGA":
    case "DESCANSO": return ehFeriado ? "FER" : "D";
    case "DSR_PERDIDO": return "DP";
    case "RT": return "RT";
    case "SUSPENSAO": return "SUSP";
    case "ADVERTENCIA": return "ADV";
    case "FOLGA_REMUNERADA": return "FR";
    default: return "?";
  }
};

export const contarSigla = (diario: Record<string, { sigla?: string }>, sigla: string) => Object.values(diario || {})
  .filter((item) => item?.sigla === sigla).length;

export const criarDias = (inicio: (string), fim: (string)) => {
  const primeiro = new Date(`${inicio}T12:00:00Z`);
  const ultimo = new Date(`${fim}T12:00:00Z`);
  const dias: string[] = [];
  for (const cursor = new Date(primeiro); cursor <= ultimo; cursor.setUTCDate(cursor.getUTCDate() + 1)) {
    dias.push(cursor.toISOString().slice(0, 10));
  }
  return dias;
};

const contarStatus = (diario: Record<string, Partial<RelatorioFrequenciaConsolidadoDTODia>>, status: string) => Object.values(diario || {})
  .filter((item) => String(item?.status || "").toUpperCase() === status).length;

export const formatarLinhaRelatorio = (linha: RelatorioFrequenciaConsolidadoDTOLinha) => {
  const diario = Object.fromEntries(Object.entries(linha.diario || {}).map(([data, dia]) => {
    const status = String(dia?.status || "SEM_LANCAMENTO").toUpperCase();
    return [data, { ...dia, status, sigla: status === "SEM_LANCAMENTO" ? "-" : obterSiglaStatus(status, false) }];
  }));
  const totais: Partial<NonNullable<RelatorioFrequenciaConsolidadoDTOLinha["totais"]>> = linha.totais || {};
  const minutosPagaveis = Number(totais.minutosPagaveis || 0);
  const minutosFisicos = Number(totais.minutosFisicos || 0);
  const minutosNoturnosReduzidos = Number(totais.minutosNoturnosReduzidos || 0);
  const minutosRt = Object.values(linha.diario || {}).reduce((soma, dia) =>
    soma + (String(dia?.status || "").toUpperCase() === "RT" ? Number(dia?.minutosPagaveis || 0) : 0), 0);

  return {
    id: linha.chaveAlocacao,
    empregadoId: linha.empregadoId,
    nome: linha.nome,
    cargo: linha.cargo || "-",
    contrato: linha.contrato || "-",
    lotacao: linha.lotacao || "-",
    turno: linha.turno || "-",
    diario,
    totais: {
      presencas: Number(totais.presencas || 0),
      parciais: Number(totais.presencasParciais || 0),
      faltas: Number(totais.faltas || 0),
      atestados: Number(totais.atestados || 0),
      descansos: Math.max(Number(totais.descansos || 0), contarSigla(diario, "D")),
      dsrPerdido: contarStatus(diario, "DSR_PERDIDO"),
      rt: contarStatus(diario, "RT"),
      semLancamento: Number(totais.semLancamento || 0),
      horasFisicas: minutosParaDuracao(minutosFisicos),
      totalFisicoDecimal: minutosFisicos / 60,
      horasPagaveis: minutosParaDuracao(minutosPagaveis),
      totalPagavelDecimal: minutosPagaveis / 60,
      horasNoturnas: minutosParaDuracao(minutosNoturnosReduzidos),
      totalNoturnoDecimal: minutosNoturnosReduzidos / 60,
      horasRt: minutosParaDuracao(minutosRt),
      totalRtDecimal: minutosRt / 60,
      minutosFisicos,
      minutosPagaveis,
      minutosAdicionais: Number(totais.minutosAdicionais || 0),
    },
  };
};
