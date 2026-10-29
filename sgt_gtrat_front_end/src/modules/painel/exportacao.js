import { notificar } from "../../core/events";
import { baixarArquivoXlsx, celulaXlsx } from "../../core/export/xlsx";

const valor = (item) => item ?? 0;
const percentual = (item) => Number(item || 0) / 100;
const numero = (item) => Number(item || 0);

const CORES = Object.freeze({
  azul: "FF00416B",
  azulEscuro: "FF00263D",
  azulMedio: "FF006FA8",
  azulClaro: "FFE2F1F8",
  azulMuitoClaro: "FFF2F8FB",
  amarelo: "FFFFD400",
  amareloEscuro: "FFD9B800",
  amareloClaro: "FFFFF8D6",
  branco: "FFFFFFFF",
  texto: "FF203541",
  cinza: "FF66747E",
  cinzaClaro: "FFF3F6F8",
  verde: "FF16704A",
  verdeClaro: "FFEAF7F0",
  laranja: "FFA45C00",
  laranjaClaro: "FFFFF4E5",
  vermelho: "FFB3261E",
  vermelhoClaro: "FFFFEEEE",
});

const estiloTitulo = {
  font: { bold: true, size: 18, color: CORES.branco },
  fillColor: CORES.azul,
  alignment: { horizontal: "left", vertical: "center" },
  border: { enabled: true, color: CORES.azulEscuro },
};
const estiloSubtitulo = {
  font: { bold: true, size: 11, color: CORES.azulEscuro },
  fillColor: CORES.amarelo,
  alignment: { vertical: "center", wrapText: true },
  border: { enabled: true, color: CORES.amareloEscuro },
};
const estiloSecao = {
  font: { bold: true, color: CORES.branco, size: 12 },
  fillColor: CORES.azul,
  alignment: { vertical: "center", wrapText: true },
  border: { enabled: true, color: CORES.azulEscuro },
};
const estiloSecaoAmarela = {
  font: { bold: true, color: CORES.azulEscuro, size: 12 },
  fillColor: CORES.amarelo,
  alignment: { vertical: "center", wrapText: true },
  border: { enabled: true, color: CORES.amareloEscuro },
};
const estiloCabecalho = {
  font: { bold: true, color: CORES.azulEscuro },
  fillColor: CORES.azulClaro,
  alignment: { vertical: "center", wrapText: true },
  border: true,
};
const estiloCabecalhoAmarelo = {
  font: { bold: true, color: CORES.azulEscuro },
  fillColor: CORES.amareloClaro,
  alignment: { vertical: "center", wrapText: true },
  border: true,
};
const estiloTexto = { alignment: { vertical: "center", wrapText: true }, border: true };
const estiloInteiro = { numberFormat: "#,##0", border: true };
const estiloDecimal = { numberFormat: "0.0", border: true };
const estiloPercentual = { numberFormat: "0.0%", border: true };
const estiloData = { border: true };
const estiloKpiAzul = {
  font: { bold: true, size: 14, color: CORES.azulEscuro },
  fillColor: CORES.azulClaro,
  alignment: { horizontal: "center", vertical: "center", wrapText: true },
  border: { enabled: true, color: CORES.azulMedio },
};
const estiloKpiAmarelo = {
  font: { bold: true, size: 14, color: CORES.azulEscuro },
  fillColor: CORES.amareloClaro,
  alignment: { horizontal: "center", vertical: "center", wrapText: true },
  border: { enabled: true, color: CORES.amareloEscuro },
};
const estiloOk = {
  font: { bold: true, color: CORES.verde },
  fillColor: CORES.verdeClaro,
  alignment: { vertical: "center", wrapText: true },
  border: true,
};
const estiloAtencao = {
  font: { bold: true, color: CORES.laranja },
  fillColor: CORES.laranjaClaro,
  alignment: { vertical: "center", wrapText: true },
  border: true,
};
const estiloCritico = {
  font: { bold: true, color: CORES.vermelho },
  fillColor: CORES.vermelhoClaro,
  alignment: { vertical: "center", wrapText: true },
  border: true,
};

const celula = (conteudo, estilo = estiloTexto) => celulaXlsx(conteudo, estilo);
const celulaInteiro = (conteudo) => celulaXlsx(numero(conteudo), estiloInteiro);
const celulaPercentual = (conteudo) => celulaXlsx(percentual(conteudo), estiloPercentual);
const celulaPp = (conteudo) => celulaXlsx(numero(conteudo), estiloDecimal);

const celulaCobertura = (frequencia = {}) => Number(frequencia.empregadosEsperados || 0) > 0
  ? celulaPercentual(frequencia.percentualCobertura)
  : celulaXlsx("Não aplicável", estiloTexto);

const celulaTaxaOperacional = (periodo = {}, campo) => Number(periodo.registrosOperacionais || 0) > 0
  ? celulaPercentual(periodo[campo])
  : celulaXlsx("Sem lançamentos", estiloTexto);

const textoPercentual = (conteudo) => `${Number(conteudo || 0).toLocaleString("pt-BR", { maximumFractionDigits: 1 })}%`;
const textoCobertura = (frequencia = {}) => Number(frequencia.empregadosEsperados || 0) > 0
  ? textoPercentual(frequencia.percentualCobertura)
  : "Não aplicável";
const textoTaxaOperacional = (periodo = {}, campo) => Number(periodo.registrosOperacionais || 0) > 0
  ? textoPercentual(periodo[campo])
  : "Sem lançamentos";
const dataPtBr = (conteudo) => {
  if (!conteudo) return "—";
  const data = new Date(`${String(conteudo).slice(0, 10)}T12:00:00`);
  return Number.isNaN(data.getTime()) ? String(conteudo) : new Intl.DateTimeFormat("pt-BR").format(data);
};

const nomeArquivoSeguro = (texto) => String(texto || "escopo")
  .normalize("NFD")
  .replace(/[\u0300-\u036f]/g, "")
  .replace(/[^a-zA-Z0-9_-]+/g, "_")
  .replace(/^_+|_+$/g, "")
  .toLowerCase();

const statusGestao = (condicaoCritica, condicaoAtencao = false) => {
  if (condicaoCritica) return { texto: "CRÍTICO", estilo: estiloCritico };
  if (condicaoAtencao) return { texto: "ATENÇÃO", estilo: estiloAtencao };
  return { texto: "CONTROLADO", estilo: estiloOk };
};

const classificacaoUnidade = (item) => {
  const cobertura = Number(item?.percentualCoberturaEscala || 0);
  const ocupacao = item?.percentualOcupacao == null ? null : Number(item.percentualOcupacao);
  const alertas = Number(item?.alertasContratuais || 0);
  const pendentes = Number(item?.pendentesFrequencia || 0);
  const ausencias = Number(item?.ausenciasNaReferencia || 0);
  const motivos = [];

  if (alertas > 0) motivos.push(`${alertas} alerta(s) contratual(is)`);
  if (Number(item?.empregadosEsperados || 0) > 0 && cobertura < 85) motivos.push(`cobertura ${cobertura.toFixed(1)}%`);
  if (pendentes > 0) motivos.push(`${pendentes} pendência(s) de frequência`);
  if (ocupacao != null && ocupacao > 110) motivos.push(`ocupação ${ocupacao.toFixed(1)}% acima do previsto`);
  if (ocupacao != null && item?.efetivoPrevisto != null && ocupacao < 80) motivos.push(`ocupação ${ocupacao.toFixed(1)}% abaixo do previsto`);
  if (ausencias > 0) motivos.push(`${ausencias} ausência(s) na referência`);

  const critico = alertas > 0 || (Number(item?.empregadosEsperados || 0) > 0 && cobertura < 85) || (ocupacao != null && (ocupacao > 110 || ocupacao < 80));
  const atencao = !critico && (pendentes > 0 || ausencias > 0 || (Number(item?.empregadosEsperados || 0) > 0 && cobertura < 95));
  const status = statusGestao(critico, atencao);
  return { ...status, motivos: motivos.join(" · ") || "Sem desvio relevante pelos critérios exibidos" };
};

const linhasMetadados = (dados, periodoDias, colunas = 6) => [
  { cells: [celulaXlsx("PAINEL GERENCIAL SGT-GTRAT — VISÃO EXECUTIVA", estiloTitulo)], height: 28 },
  { cells: [celulaXlsx(`Escopo: ${dados.escopo?.nome || "—"} · nível: ${dados.escopo?.nivel || "—"}`, estiloSubtitulo)], height: 22 },
  { cells: [celulaXlsx(`Referência: ${dados.referencia || "—"} · janela analítica: ${periodoDias} dias · servidor: ${dados.geradoEm || "—"}`, { ...estiloTexto, fillColor: CORES.azulMuitoClaro })] },
  { cells: [celulaXlsx("As classificações abaixo são regras transparentes de apoio gerencial; não substituem análise do gestor.", { ...estiloTexto, font: { italic: true, color: CORES.cinza } })] },
  { cells: Array.from({ length: colunas }, () => "") },
];

const planilhaDashboardExecutivo = (dados, periodoDias) => {
  const efetivo = dados.efetivo || {};
  const contratos = dados.contratos || {};
  const frequencia = dados.frequencia || {};
  const periodo = dados.periodo || {};
  const integridade = dados.integridade || {};
  const pressao = dados.pressaoOperacional || {};
  const comparativo = dados.comparativoPeriodo || {};
  const alertas = dados.alertas || [];
  const unidades = dados.unidades || [];

  const statusCobertura = statusGestao(Number(frequencia.empregadosEsperados || 0) > 0 && Number(frequencia.percentualCobertura || 0) < 85,
    Number(frequencia.empregadosEsperados || 0) > 0 && Number(frequencia.percentualCobertura || 0) < 95);
  const statusIntegridade = statusGestao(Number(integridade.totalInconsistencias || 0) > 0);
  const statusContrato = statusGestao(Number(contratos.vencidosMarcadosAtivos || 0) > 0 || Number(efetivo.ativosSemContrato || 0) > 0,
    Number(contratos.vencendoEm30Dias || 0) > 0);
  const statusAbsenteismo = statusGestao(Number(periodo.taxaAbsenteismo || 0) >= 10, Number(periodo.taxaAbsenteismo || 0) >= 5);
  const unidadesAtencao = unidades.filter((item) => classificacaoUnidade(item).texto !== "CONTROLADO").length;

  const linhas = [
    ...linhasMetadados(dados, periodoDias, 6),
    { cells: [celulaXlsx("INDICADORES-CHAVE DE GESTÃO", estiloSecao)], height: 22 },
    { cells: [
      celulaXlsx("Efetivo vinculado / previsto", estiloCabecalho),
      celulaXlsx("Ocupação contratual", estiloCabecalho),
      celulaXlsx("Cobertura da frequência", estiloCabecalho),
      celulaXlsx("Absenteísmo no período", estiloCabecalho),
      celulaXlsx("Inconsistências", estiloCabecalho),
      celulaXlsx("Alertas gerenciais", estiloCabecalho),
    ] },
    { cells: [
      celulaXlsx(`${valor(efetivo.efetivoVinculado)} / ${valor(efetivo.efetivoPrevisto)}`, estiloKpiAzul),
      celulaXlsx(percentual(efetivo.percentualOcupacao), { ...estiloKpiAmarelo, numberFormat: "0.0%" }),
      Number(frequencia.empregadosEsperados || 0) > 0 ? celulaXlsx(percentual(frequencia.percentualCobertura), { ...estiloKpiAzul, numberFormat: "0.0%" }) : celulaXlsx("Não aplicável", estiloKpiAzul),
      Number(periodo.registrosOperacionais || 0) > 0 ? celulaXlsx(percentual(periodo.taxaAbsenteismo), { ...estiloKpiAmarelo, numberFormat: "0.0%" }) : celulaXlsx("Sem lançamentos", estiloKpiAmarelo),
      celulaXlsx(numero(integridade.totalInconsistencias), estiloKpiAzul),
      celulaXlsx(numero(alertas.length), estiloKpiAmarelo),
    ], height: 34 },
    { cells: [] },
    { cells: [celulaXlsx("SEMÁFORO EXECUTIVO", estiloSecaoAmarela)], height: 22 },
    { cells: ["Tema", "Situação", "Indicador", "Leitura gerencial", "Ação sugerida", "Responsável sugerido"].map((item) => celulaXlsx(item, estiloCabecalho)) },
    { cells: [
      "Cobertura da frequência",
      celulaXlsx(statusCobertura.texto, statusCobertura.estilo),
      celulaCobertura(frequencia),
      `${valor(frequencia.pendentes)} pendência(s) entre ${valor(frequencia.empregadosEsperados)} esperado(s).`,
      Number(frequencia.pendentes || 0) > 0 ? "Priorizar conclusão dos lançamentos pendentes e validar escala." : "Manter acompanhamento diário.",
      "Gestão operacional",
    ] },
    { cells: [
      "Contratos e efetivo",
      celulaXlsx(statusContrato.texto, statusContrato.estilo),
      `${valor(contratos.vigentes)} vigente(s) · saldo ${valor(efetivo.saldoContratual)}`,
      `${valor(contratos.vencendoEm30Dias)} contrato(s) vencendo em 30 dias · ${valor(efetivo.ativosSemContrato)} ativo(s) sem contrato vigente.`,
      statusContrato.texto === "CONTROLADO" ? "Manter agenda de renovação e dimensionamento." : "Revisar vencimentos, vínculos e dimensionamento prioritariamente.",
      "Gestão contratual",
    ] },
    { cells: [
      "Absenteísmo",
      celulaXlsx(statusAbsenteismo.texto, statusAbsenteismo.estilo),
      celulaTaxaOperacional(periodo, "taxaAbsenteismo"),
      `${valor(periodo.empregadosComAusencia)} empregado(s) afetado(s); ${valor(periodo.empregadosComAusenciaRecorrente)} recorrente(s).`,
      Number(periodo.empregadosComAusenciaRecorrente || 0) > 0 ? "Analisar recorrência por unidade e padrão semanal." : "Acompanhar tendência e composição das ausências.",
      "Gestão operacional / RH",
    ] },
    { cells: [
      "Integridade de dados",
      celulaXlsx(statusIntegridade.texto, statusIntegridade.estilo),
      celulaInteiro(integridade.totalInconsistencias),
      `${valor(integridade.empregadosAtivosSemLotacao)} sem lotação · ${valor(integridade.empregadosAtivosSemJornada)} sem jornada · ${valor(integridade.empregadosAtivosSemContrato)} sem contrato.`,
      Number(integridade.totalInconsistencias || 0) > 0 ? "Executar saneamento dos vínculos que sustentam os indicadores gerenciais." : "Manter rotina de qualidade cadastral.",
      "Administração / dados mestres",
    ] },
    { cells: [
      "Unidades prioritárias",
      celulaXlsx(unidadesAtencao > 0 ? "ATENÇÃO" : "CONTROLADO", unidadesAtencao > 0 ? estiloAtencao : estiloOk),
      celulaInteiro(unidadesAtencao),
      `${unidadesAtencao} unidade(s) possuem desvio pelos critérios de cobertura, alertas, ocupação ou pendências.`,
      unidadesAtencao > 0 ? "Abrir a aba 'Unidades e riscos' e atuar pelas maiores criticidades." : "Manter comparação periódica entre unidades.",
      "Gestores estaduais / locais",
    ] },
    { cells: [] },
    { cells: [celulaXlsx("TENDÊNCIA E PRESSÃO OPERACIONAL", estiloSecao)] },
    { cells: ["Indicador", "Período atual", "Período anterior", "Variação", "Interpretação"].map((item) => celulaXlsx(item, estiloCabecalhoAmarelo)) },
    { cells: ["Taxa de presença", celulaTaxaOperacional(periodo, "taxaPresenca"), Number(comparativo.registrosOperacionaisAnterior || 0) > 0 ? celulaPercentual(comparativo.taxaPresencaAnterior) : "Sem lançamentos", celulaPp(comparativo.variacaoTaxaPresencaPp), "Variação em pontos percentuais quando existem lançamentos nas janelas comparadas."] },
    { cells: ["Taxa de absenteísmo", celulaTaxaOperacional(periodo, "taxaAbsenteismo"), Number(comparativo.registrosOperacionaisAnterior || 0) > 0 ? celulaPercentual(comparativo.taxaAbsenteismoAnterior) : "Sem lançamentos", celulaPp(comparativo.variacaoTaxaAbsenteismoPp), "Queda é favorável; alta demanda investigação quando existem lançamentos comparáveis."] },
    { cells: ["Ausências", celulaInteiro(Number(periodo.faltasNaoJustificadas || 0) + Number(periodo.atestados || 0) + Number(periodo.suspensoes || 0)), celulaInteiro(comparativo.ausenciasAnterior), celulaInteiro(comparativo.variacaoAusencias), "Comparação entre janelas de igual duração."] },
    { cells: ["Registros RT", celulaInteiro(periodo.repousosTrabalhados), celulaInteiro(comparativo.repousosTrabalhadosAnterior), celulaInteiro(comparativo.variacaoRepousosTrabalhados), `${numero(pressao.empregadosComRt)} empregado(s) com RT · ${Number(pressao.rtPorCemAtivos || 0).toFixed(1)} por 100 ativos.`] },
  ];

  return {
    name: "Dashboard executivo",
    columns: [{ width: 27 }, { width: 18 }, { width: 22 }, { width: 46 }, { width: 52 }, { width: 27 }],
    rows: linhas,
    merges: ["A1:F1", "A2:F2", "A3:F3", "A4:F4", "A6:F6", "A10:F10", "A18:F18"],
    freezeRows: 5,
  };
};

const planilhaEfetivoContratos = (dados) => {
  const efetivo = dados.efetivo || {};
  const quadro = dados.quadro || {};
  const contratos = dados.contratos || {};
  const horizonte = dados.horizonteContratual || {};
  const analiseCadastro = dados.analiseCadastro || {};
  const admissoesMensais = dados.admissoesMensais || [];
  const linhas = [
    { cells: [celulaXlsx("EFETIVO, DIMENSIONAMENTO E CONTRATOS", estiloTitulo)], height: 28 },
    { cells: [celulaXlsx(`Escopo ${dados.escopo?.nome || "—"} · referência ${dados.referencia || "—"}`, estiloSubtitulo)] },
    { cells: [] },
    { cells: [celulaXlsx("CAPACIDADE CONTRATUAL", estiloSecao)] },
    { cells: ["Indicador", "Valor", "Leitura de gestão"].map((item) => celulaXlsx(item, estiloCabecalho)) },
    { cells: ["Efetivo previsto", celulaInteiro(efetivo.efetivoPrevisto), "Quantidade prevista nos contratos vigentes do escopo."] },
    { cells: ["Efetivo vinculado", celulaInteiro(efetivo.efetivoVinculado), "Empregados vinculados a contratos vigentes no escopo."] },
    { cells: ["Saldo contratual (previsto - vinculado)", celulaInteiro(efetivo.saldoContratual), Number(efetivo.saldoContratual || 0) < 0 ? "Valor negativo indica efetivo acima do previsto." : "Valor positivo indica capacidade ainda não preenchida."] },
    { cells: ["Ocupação contratual", celulaPercentual(efetivo.percentualOcupacao), "Relação entre efetivo vinculado e efetivo previsto."] },
    { cells: ["Ativos sem contrato vigente", celulaInteiro(efetivo.ativosSemContrato), "Requer validação de vínculo quando maior que zero."] },
    { cells: [] },
    { cells: [celulaXlsx("SAÚDE DOS CONTRATOS", estiloSecaoAmarela)] },
    { cells: ["Indicador", "Quantidade", "Prioridade", "Ação gerencial sugerida"].map((item) => celulaXlsx(item, estiloCabecalho)) },
  ];

  const itensContrato = [
    ["Contratos vigentes", contratos.vigentes, false, false, "Acompanhar calendário contratual."],
    ["Vencidos ainda marcados como ativos", contratos.vencidosMarcadosAtivos, Number(contratos.vencidosMarcadosAtivos || 0) > 0, false, "Regularizar situação contratual e status no cadastro."],
    ["Vencendo em 30 dias", contratos.vencendoEm30Dias, false, Number(contratos.vencendoEm30Dias || 0) > 0, "Antecipar renovação, substituição ou encerramento."],
    ["Sem data final", contratos.semDataFim, false, Number(contratos.semDataFim || 0) > 0, "Validar se ausência de data final é esperada."],
    ["Sem efetivo previsto", contratos.semEfetivoPrevisto, Number(contratos.semEfetivoPrevisto || 0) > 0, false, "Completar dimensionamento para permitir análise de capacidade."],
    ["Acima do efetivo previsto", contratos.acimaDoEfetivoPrevisto, false, Number(contratos.acimaDoEfetivoPrevisto || 0) > 0, "Revisar alocação e justificativa de excesso."],
    ["Abaixo do efetivo previsto", contratos.abaixoDoEfetivoPrevisto, false, Number(contratos.abaixoDoEfetivoPrevisto || 0) > 0, "Avaliar necessidade de recomposição ou redimensionamento."],
  ];
  for (const [rotulo, quantidade, critico, atencao, acao] of itensContrato) {
    const status = statusGestao(critico, atencao);
    linhas.push({ cells: [rotulo, celulaInteiro(quantidade), celulaXlsx(status.texto, status.estilo), acao] });
  }

  linhas.push(
    { cells: [] },
    { cells: [celulaXlsx("HORIZONTE DE VENCIMENTO", estiloSecao)] },
    { cells: ["Faixa", "Contratos", "Leitura"].map((item) => celulaXlsx(item, estiloCabecalhoAmarelo)) },
    { cells: ["Vencidos", celulaInteiro(horizonte.vencidos), "Maior urgência de regularização."] },
    { cells: ["Até 30 dias", celulaInteiro(horizonte.ate30Dias), "Janela imediata de renovação/encerramento."] },
    { cells: ["31 a 90 dias", celulaInteiro(horizonte.de31A90Dias), "Planejamento de curto prazo."] },
    { cells: ["91 a 180 dias", celulaInteiro(horizonte.de91A180Dias), "Planejamento de médio prazo."] },
    { cells: ["Acima de 180 dias", celulaInteiro(horizonte.acimaDe180Dias), "Menor urgência temporal."] },
    { cells: ["Sem data fim", celulaInteiro(horizonte.semDataFim), "Validar regra contratual aplicável."] },
    { cells: [] },
    { cells: [celulaXlsx("COMPOSIÇÃO DO QUADRO", estiloSecaoAmarela)] },
    { cells: ["Indicador", "Quantidade", "Percentual/observação"].map((item) => celulaXlsx(item, estiloCabecalho)) },
    { cells: ["Empregados cadastrados", celulaInteiro(quadro.empregadosCadastrados), "Base cadastral no escopo."] },
    { cells: ["Ativos", celulaInteiro(quadro.ativos), celulaPercentual(quadro.percentualAtivos)] },
    { cells: ["Inativos/afastados", celulaInteiro(quadro.inativosOuAfastados), "Situações não ativas consolidadas."] },
    { cells: ["Lotações", celulaInteiro(quadro.lotacoes), "Estruturas com empregados no escopo."] },
    { cells: ["Cargos", celulaInteiro(quadro.cargos), "Cargos distintos no escopo."] },
    { cells: ["Jornadas", celulaInteiro(quadro.jornadas), "Jornadas distintas no escopo."] },
  );

  const linhaSecaoCadastro = linhas.length + 2;
  linhas.push(
    { cells: [] },
    { cells: [celulaXlsx("ANÁLISE CADASTRAL REAL", estiloSecao)] },
    { cells: ["Indicador", "Valor", "Leitura", "Fonte"].map((item) => celulaXlsx(item, estiloCabecalho)) },
    { cells: ["Admitidos no período selecionado", celulaInteiro(analiseCadastro.admitidosNoPeriodo), "Admissões cuja data cadastrada pertence à janela analítica.", "empregado.data_admissao"] },
    { cells: ["Admitidos no mês da referência", celulaInteiro(analiseCadastro.admitidosNoMesReferencia), "Entradas ocorridas no mesmo mês da data de referência.", "empregado.data_admissao"] },
    { cells: ["Admitidos nos últimos 12 meses", celulaInteiro(analiseCadastro.admitidosUltimos12Meses), "Movimento cadastral acumulado em 12 meses.", "empregado.data_admissao"] },
    { cells: ["Com data de admissão", celulaInteiro(analiseCadastro.comDataAdmissao), celulaPercentual(analiseCadastro.percentualComDataAdmissao), "empregado.data_admissao"] },
    { cells: ["Sem data de admissão", celulaInteiro(analiseCadastro.semDataAdmissao), "Registros que precisam de saneamento para análises temporais completas.", "empregado.data_admissao"] },
    { cells: [] },
    { cells: [celulaXlsx("ADMISSÕES MENSAIS — ÚLTIMOS 12 MESES", estiloSecaoAmarela)] },
    { cells: ["Mês", "Admissões", "Interpretação", "Fonte"].map((item) => celulaXlsx(item, estiloCabecalhoAmarelo)) },
    ...admissoesMensais.map((item) => ({ cells: [dataPtBr(item.mes), celulaInteiro(item.quantidade), "Quantidade real de admissões cadastradas no mês.", "empregado.data_admissao"] })),
  );
  const linhaSecaoAdmissoes = linhaSecaoCadastro + 8;

  return {
    name: "Efetivo e contratos",
    columns: [{ width: 38 }, { width: 18 }, { width: 22 }, { width: 64 }],
    rows: linhas,
    merges: ["A1:D1", "A2:D2", "A4:D4", "A12:D12", "A22:D22", "A31:D31", `A${linhaSecaoCadastro}:D${linhaSecaoCadastro}`, `A${linhaSecaoAdmissoes}:D${linhaSecaoAdmissoes}`],
    freezeRows: 5,
  };
};

const planilhaFrequenciaAbsenteismo = (dados, periodoDias) => {
  const frequencia = dados.frequencia || {};
  const periodo = dados.periodo || {};
  const comparativo = dados.comparativoPeriodo || {};
  const pressao = dados.pressaoOperacional || {};
  const distribuicao = dados.distribuicaoStatus || [];
  const semana = dados.padraoSemanal || [];

  const linhas = [
    { cells: [celulaXlsx("FREQUÊNCIA, ABSENTEÍSMO E PRESSÃO OPERACIONAL", estiloTitulo)], height: 28 },
    { cells: [celulaXlsx(`Referência ${dados.referencia || "—"} · análise de ${periodoDias} dias`, estiloSubtitulo)] },
    { cells: [] },
    { cells: [celulaXlsx("COBERTURA DA REFERÊNCIA", estiloSecao)] },
    { cells: ["Indicador", "Valor", "Interpretação"].map((item) => celulaXlsx(item, estiloCabecalho)) },
    { cells: ["Empregados ativos", celulaInteiro(frequencia.empregadosAtivos), "Ativos cadastrados no escopo."] },
    { cells: ["Esperados na escala", celulaInteiro(frequencia.empregadosEsperados), "Empregados para os quais a escala determina trabalho na data."] },
    { cells: ["Registros lançados entre os esperados", celulaInteiro(frequencia.registrosLancados ?? frequencia.registrosEsperados), "Base efetivamente coberta pela frequência."] },
    { cells: ["Pendentes", celulaInteiro(frequencia.pendentes), "Esperados ainda sem lançamento."] },
    { cells: ["Cobertura", celulaCobertura(frequencia), Number(frequencia.empregadosEsperados || 0) > 0 ? "Lançados / esperados." : "Não há empregados esperados para a data; o painel não fabrica 0% ou 100%." ] },
    { cells: ["Presenças", celulaInteiro(frequencia.presencas), "Presenças classificadas na referência."] },
    { cells: ["Ausências", celulaInteiro(frequencia.ausencias), "Ausências operacionais na referência."] },
    { cells: ["Atestados", celulaInteiro(frequencia.ausenciasJustificadas), "Ausências justificadas por atestado."] },
    { cells: ["Faltas", celulaInteiro(frequencia.faltasNaoJustificadas), "Faltas não justificadas."] },
    { cells: ["RT", celulaInteiro(frequencia.repousosTrabalhados), "Repousos/folgas trabalhados."] },
    { cells: ["Trabalhos fora da escala sem RT", celulaInteiro(frequencia.trabalhosForaEscalaSemRt), "Deve ser analisado quando maior que zero."] },
    { cells: [] },
    { cells: [celulaXlsx(`ANÁLISE DO PERÍODO — ${periodoDias} DIAS`, estiloSecaoAmarela)] },
    { cells: ["Indicador", "Atual", "Anterior", "Variação", "Leitura"].map((item) => celulaXlsx(item, estiloCabecalho)) },
    { cells: ["Registros operacionais", celulaInteiro(periodo.registrosOperacionais), celulaInteiro(comparativo.registrosOperacionaisAnterior), celulaInteiro(comparativo.variacaoRegistrosOperacionais), "Volume operacional registrado nas janelas comparadas."] },
    { cells: ["Taxa de presença", celulaTaxaOperacional(periodo, "taxaPresenca"), Number(comparativo.registrosOperacionaisAnterior || 0) > 0 ? celulaPercentual(comparativo.taxaPresencaAnterior) : "Sem lançamentos", celulaPp(comparativo.variacaoTaxaPresencaPp), "Variação em pontos percentuais quando existem lançamentos nas janelas comparadas."] },
    { cells: ["Taxa de absenteísmo", celulaTaxaOperacional(periodo, "taxaAbsenteismo"), Number(comparativo.registrosOperacionaisAnterior || 0) > 0 ? celulaPercentual(comparativo.taxaAbsenteismoAnterior) : "Sem lançamentos", celulaPp(comparativo.variacaoTaxaAbsenteismoPp), "Variação em pontos percentuais quando há base operacional; queda é favorável."] },
    { cells: ["Faltas não justificadas", celulaInteiro(periodo.faltasNaoJustificadas), "—", "—", "Componente do absenteísmo."] },
    { cells: ["Atestados", celulaInteiro(periodo.atestados), "—", "—", "Componente do absenteísmo."] },
    { cells: ["Suspensões", celulaInteiro(periodo.suspensoes), "—", "—", "Componente do absenteísmo."] },
    { cells: ["DSR perdidos", celulaInteiro(periodo.dsrPerdidos), "—", "—", "Indicador disciplinar/operacional."] },
    { cells: ["Empregados com ausência", celulaInteiro(periodo.empregadosComAusencia), "—", "—", "Quantidade de pessoas afetadas no período."] },
    { cells: ["Recorrentes (3+)", celulaInteiro(periodo.empregadosComAusenciaRecorrente), "—", "—", "Priorizar análise de recorrência, sem inferir causa automaticamente."] },
    { cells: ["Registros RT", celulaInteiro(periodo.repousosTrabalhados), celulaInteiro(comparativo.repousosTrabalhadosAnterior), celulaInteiro(comparativo.variacaoRepousosTrabalhados), `${valor(pressao.empregadosComRt)} empregado(s) com RT; relação RT/ausências ${Number(pressao.relacaoRtAusencias || 0).toFixed(1)}%.`] },
    { cells: [] },
    { cells: [celulaXlsx("DISTRIBUIÇÃO DOS LANÇAMENTOS", estiloSecao)] },
    { cells: ["Status", "Quantidade", "Percentual"].map((item) => celulaXlsx(item, estiloCabecalhoAmarelo)) },
    ...distribuicao.map((item) => ({ cells: [item.status || "SEM_STATUS", celulaInteiro(item.quantidade), celulaPercentual(item.percentual)] })),
    { cells: [] },
    { cells: [celulaXlsx("PADRÃO SEMANAL", estiloSecaoAmarela)] },
    { cells: ["Dia", "Registros operacionais", "Ausências", "Taxa de absenteísmo"].map((item) => celulaXlsx(item, estiloCabecalho)) },
    ...semana.map((item) => ({ cells: [item.dia, celulaInteiro(item.registrosOperacionais), celulaInteiro(item.ausencias), celulaPercentual(item.taxaAbsenteismo)] })),
  ];

  return {
    name: "Frequência e absenteísmo",
    columns: [{ width: 38 }, { width: 20 }, { width: 20 }, { width: 18 }, { width: 68 }],
    rows: linhas,
    merges: ["A1:E1", "A2:E2", "A4:E4", "A18:E18", "A31:E31", `A${34 + distribuicao.length}:E${34 + distribuicao.length}`],
    freezeRows: 5,
  };
};

const planilhaEvolucao = (dados) => {
  const evolucao = dados.evolucao || [];
  return {
    name: "Evolução operacional",
    columns: [{ width: 16 }, { width: 18 }, { width: 18 }, { width: 18 }, { width: 18 }, { width: 24 }],
    rows: [
      { cells: [celulaXlsx("EVOLUÇÃO OPERACIONAL DIÁRIA", estiloTitulo)], height: 28 },
      { cells: [celulaXlsx(`Escopo ${dados.escopo?.nome || "—"} · referência ${dados.referencia || "—"}`, estiloSubtitulo)] },
      { cells: [] },
      { cells: ["Data", "Registros", "Presenças", "Ausências", "RT", "Presença / registros"].map((item) => celulaXlsx(item, estiloCabecalho)) },
      ...evolucao.map((item) => ({
        cells: [
          celulaXlsx(item.data || "—", estiloData),
          celulaInteiro(item.registros),
          celulaInteiro(item.presencas),
          celulaInteiro(item.ausencias),
          celulaInteiro(item.repousosTrabalhados),
          Number(item.registros || 0) > 0 ? celulaXlsx(Number(item.presencas || 0) / Number(item.registros), estiloPercentual) : "—",
        ],
      })),
    ],
    merges: ["A1:F1", "A2:F2"],
    autoFilter: evolucao.length ? `A4:F${4 + evolucao.length}` : undefined,
    freezeRows: 4,
  };
};

const planilhaUnidades = (dados) => {
  const unidades = dados.unidades || [];
  return {
    name: "Unidades e riscos",
    columns: [34, 10, 14, 14, 14, 14, 14, 16, 14, 14, 18, 16, 62].map((width) => ({ width })),
    rows: [
      { cells: [celulaXlsx("MATRIZ DE UNIDADES, CAPACIDADE E RISCO", estiloTitulo)], height: 28 },
      { cells: [celulaXlsx(`Nível ${dados.escopo?.nivel || "—"} · referência ${dados.referencia || "—"}`, estiloSubtitulo)] },
      { cells: [celulaXlsx("Critérios de atenção: alertas contratuais; cobertura < 95%; ocupação < 80% ou > 110%; pendências/ausências. Crítico usa cobertura < 85%, alerta contratual ou ocupação fora de 80–110%.", { ...estiloTexto, font: { italic: true, color: CORES.cinza } })] },
      { cells: [] },
      {
        cells: ["Unidade", "UF", "Efetivo atual", "Previsto", "Saldo", "Ocupação", "Contratos", "Alertas contratuais", "Esperados", "Pendentes", "Cobertura", "Classificação", "Motivos de atenção"]
          .map((item) => celulaXlsx(item, estiloCabecalho)),
      },
      ...unidades.map((item) => {
        const classificacao = classificacaoUnidade(item);
        return {
          cells: [
            item.nome || "—",
            item.uf || item.tipo || "—",
            celulaInteiro(item.efetivoAtual),
            item.efetivoPrevisto == null ? "—" : celulaInteiro(item.efetivoPrevisto),
            item.saldo == null ? "—" : celulaInteiro(item.saldo),
            item.percentualOcupacao == null ? "—" : celulaPercentual(item.percentualOcupacao),
            celulaInteiro(item.contratosVigentes),
            celulaInteiro(item.alertasContratuais),
            celulaInteiro(item.empregadosEsperados),
            celulaInteiro(item.pendentesFrequencia),
            Number(item.empregadosEsperados || 0) > 0 ? celulaPercentual(item.percentualCoberturaEscala) : "Sem escala esperada",
            celulaXlsx(classificacao.texto, classificacao.estilo),
            classificacao.motivos,
          ],
        };
      }),
    ],
    merges: ["A1:M1", "A2:M2", "A3:M3"],
    autoFilter: unidades.length ? `A5:M${5 + unidades.length}` : undefined,
    freezeRows: 5,
  };
};

const planilhaRiscosAlertas = (dados) => {
  const alertas = dados.alertas || [];
  const unidadesPrioritarias = (dados.unidades || [])
    .map((item) => ({ item, classificacao: classificacaoUnidade(item) }))
    .filter(({ classificacao }) => classificacao.texto !== "CONTROLADO")
    .sort((a, b) => {
      const peso = { CRÍTICO: 2, ATENÇÃO: 1, CONTROLADO: 0 };
      return peso[b.classificacao.texto] - peso[a.classificacao.texto]
        || Number(a.item.percentualCoberturaEscala || 0) - Number(b.item.percentualCoberturaEscala || 0);
    })
    .slice(0, 25);

  const linhas = [
    { cells: [celulaXlsx("RISCOS, ALERTAS E PRIORIDADES GERENCIAIS", estiloTitulo)], height: 28 },
    { cells: [celulaXlsx(`Escopo ${dados.escopo?.nome || "—"} · referência ${dados.referencia || "—"}`, estiloSubtitulo)] },
    { cells: [] },
    { cells: [celulaXlsx("ALERTAS PRODUZIDOS PELO PAINEL", estiloSecao)] },
    { cells: ["Severidade", "Código", "Título", "Descrição", "Rota de atuação"].map((item) => celulaXlsx(item, estiloCabecalho)) },
    ...alertas.map((item) => {
      const severidade = String(item.severidade || "INFO").toUpperCase();
      const estilo = severidade === "CRITICO" || severidade === "CRÍTICO" ? estiloCritico : severidade === "AVISO" ? estiloAtencao : estiloOk;
      return { cells: [celulaXlsx(severidade, estilo), item.codigo || "—", item.titulo || "—", item.mensagem || "—", item.rota || "—"] };
    }),
    { cells: [] },
    { cells: [celulaXlsx("UNIDADES PRIORITÁRIAS PELOS CRITÉRIOS EXPLÍCITOS", estiloSecaoAmarela)] },
    { cells: ["Classificação", "Unidade", "UF/tipo", "Cobertura", "Pendentes", "Ocupação", "Alertas contratuais", "Motivos"].map((item) => celulaXlsx(item, estiloCabecalhoAmarelo)) },
    ...unidadesPrioritarias.map(({ item, classificacao }) => ({
      cells: [
        celulaXlsx(classificacao.texto, classificacao.estilo),
        item.nome || "—",
        item.uf || item.tipo || "—",
        Number(item.empregadosEsperados || 0) > 0 ? celulaPercentual(item.percentualCoberturaEscala) : "—",
        celulaInteiro(item.pendentesFrequencia),
        item.percentualOcupacao == null ? "—" : celulaPercentual(item.percentualOcupacao),
        celulaInteiro(item.alertasContratuais),
        classificacao.motivos,
      ],
    })),
  ];

  return {
    name: "Riscos e alertas",
    columns: [{ width: 18 }, { width: 24 }, { width: 34 }, { width: 72 }, { width: 28 }, { width: 18 }, { width: 20 }, { width: 72 }],
    rows: linhas,
    merges: ["A1:H1", "A2:H2", "A4:H4", `A${7 + alertas.length}:H${7 + alertas.length}`],
    freezeRows: 5,
  };
};

const planilhaIntegridade = (dados) => {
  const integridade = dados.integridade || {};
  const itens = [
    ["Ativos sem contrato vigente", integridade.empregadosAtivosSemContrato, "Vínculo contratual"],
    ["Empregados em múltiplos contratos vigentes", integridade.empregadosComMultiplosContratosVigentes, "Vínculo contratual"],
    ["Contrato em Gestão Local divergente", integridade.empregadoContratoGestaoLocalDivergente, "Estrutura organizacional"],
    ["Lotação em Gestão Local divergente", integridade.empregadoLotacaoGestaoLocalDivergente, "Estrutura organizacional"],
    ["Turno em Gestão Local divergente", integridade.empregadoJornadaGestaoLocalDivergente, "Estrutura organizacional"],
    ["Jornadas sem configuração completa dos 7 dias", integridade.jornadasComCalendarioIncompleto, "Escala/jornada"],
    ["Empregados ativos sem lotação", integridade.empregadosAtivosSemLotacao, "Cadastro"],
    ["Empregados ativos sem jornada", integridade.empregadosAtivosSemJornada, "Cadastro"],
    ["Adicionais RT sem frequência de origem", integridade.adicionaisRtSemFrequencia, "Frequência/RT"],
    ["Empregados sem Gestão Local", integridade.empregadosSemGestaoLocal, "Estrutura organizacional"],
    ["Contratos sem Gestão Local", integridade.contratosSemGestaoLocal, "Estrutura organizacional"],
  ];

  const controles = [
    ["Vínculos organizacionais e contratuais íntegros", Number(integridade.totalInconsistencias || 0) === 0, `${valor(integridade.totalInconsistencias)} inconsistência(s)`],
    ["Cobertura da frequência em pelo menos 95%", Number(dados.frequencia?.empregadosEsperados || 0) === 0 || Number(dados.frequencia?.percentualCobertura || 0) >= 95, `${Number(dados.frequencia?.percentualCobertura || 0).toFixed(1)}%`],
    ["Nenhum contrato vencido mantido como ativo", Number(dados.contratos?.vencidosMarcadosAtivos || 0) === 0, `${valor(dados.contratos?.vencidosMarcadosAtivos)} contrato(s)`],
    ["Nenhum empregado ativo sem contrato vigente", Number(dados.efetivo?.ativosSemContrato || 0) === 0, `${valor(dados.efetivo?.ativosSemContrato)} empregado(s)`],
    ["Nenhum trabalho fora da escala sem RT", Number(dados.frequencia?.trabalhosForaEscalaSemRt || 0) === 0, `${valor(dados.frequencia?.trabalhosForaEscalaSemRt)} registro(s)`],
  ];

  return {
    name: "Integridade e controles",
    columns: [{ width: 52 }, { width: 18 }, { width: 28 }, { width: 22 }, { width: 54 }],
    rows: [
      { cells: [celulaXlsx("INTEGRIDADE, GOVERNANÇA E CONTROLES", estiloTitulo)], height: 28 },
      { cells: [celulaXlsx(`Total de inconsistências: ${valor(integridade.totalInconsistencias)} · referência ${dados.referencia || "—"}`, estiloSubtitulo)] },
      { cells: [] },
      { cells: [celulaXlsx("PONTOS DE INTEGRIDADE", estiloSecao)] },
      { cells: ["Verificação", "Quantidade", "Domínio", "Situação", "Orientação"].map((item) => celulaXlsx(item, estiloCabecalho)) },
      ...itens.map(([rotulo, quantidade, dominio]) => {
        const temProblema = Number(quantidade || 0) > 0;
        return { cells: [rotulo, celulaInteiro(quantidade), dominio, celulaXlsx(temProblema ? "PENDENTE" : "OK", temProblema ? estiloCritico : estiloOk), temProblema ? "Saneamento recomendado antes de usar o indicador para decisão detalhada." : "Sem inconsistência detectada neste controle."] };
      }),
      { cells: [] },
      { cells: [celulaXlsx("CHECKLIST DE GOVERNANÇA", estiloSecaoAmarela)] },
      { cells: ["Controle", "Resultado", "Evidência"].map((item) => celulaXlsx(item, estiloCabecalhoAmarelo)) },
      ...controles.map(([rotulo, conforme, evidencia]) => ({ cells: [rotulo, celulaXlsx(conforme ? "CONFORME" : "PENDENTE", conforme ? estiloOk : estiloAtencao), evidencia] })),
    ],
    merges: ["A1:E1", "A2:E2", "A4:E4", `A${7 + itens.length}:E${7 + itens.length}`],
    freezeRows: 5,
  };
};

const planilhaIndicadorEfetivoOperacional = (indicadorEfetivo) => {
  const dadosIndicador = indicadorEfetivo?.dados;
  if (!dadosIndicador) return null;

  const contexto = indicadorEfetivo?.contexto || {};
  const resumo = dadosIndicador.resumoAtual || {};
  const serie = Array.isArray(dadosIndicador.serieAtual) ? dadosIndicador.serieAtual : [];
  const quadro = Array.isArray(dadosIndicador.quadroDimensionado) ? dadosIndicador.quadroDimensionado : [];
  const rows = [];
  const merges = [];
  const mergeLinha = (titulo, estilo, altura) => {
    const numeroLinha = rows.length + 1;
    rows.push({ cells: [celulaXlsx(titulo, estilo)], ...(altura ? { height: altura } : {}) });
    merges.push(`A${numeroLinha}:J${numeroLinha}`);
  };

  mergeLinha("INDICADOR CONTRATUAL DE EFETIVO", estiloTitulo, 28);
  mergeLinha(`Escopo: ${contexto.escopoSelecionado || "—"} · referência ${dataPtBr(contexto.referencia || dadosIndicador.fim)}`, estiloSubtitulo, 22);
  rows.push({ cells: [] });
  mergeLinha("FOTOGRAFIA DA DATA DE REFERÊNCIA", estiloSecao);
  rows.push({ cells: ["Indicador", "Valor", "Previsto", "Alocados", "Lançados", "Pendentes", "Presentes", "Ausentes", "Cobertura", "Leitura"].map((item) => celulaXlsx(item, estiloCabecalho)) });
  rows.push({ cells: [
    "Cobertura dos alocados",
    celulaPercentual(resumo.percentualCobertura),
    celulaInteiro(resumo.efetivoPrevisto),
    celulaInteiro(resumo.efetivoAlocado),
    celulaInteiro(resumo.registrosLancados),
    celulaInteiro(resumo.pendentesLancamento),
    celulaInteiro(resumo.presentes),
    celulaInteiro(resumo.ausentes),
    celulaPercentual(resumo.percentualCobertura),
    "Percentual autoritativo devolvido pelo Java",
  ] });
  rows.push({ cells: [
    "Presença contratual",
    celulaPercentual(resumo.percentualAtendimento),
    celulaInteiro(resumo.efetivoPrevisto),
    celulaInteiro(resumo.efetivoAlocado),
    celulaInteiro(resumo.registrosLancados),
    celulaInteiro(resumo.pendentesLancamento),
    celulaInteiro(resumo.presentes),
    celulaInteiro(resumo.ausentes),
    celulaPercentual(resumo.percentualCobertura),
    "Presentes ÷ efetivo previsto no contrato",
  ] });
  rows.push({ cells: [
    "Déficit contratual",
    celulaInteiro(resumo.deficitContratual),
    celulaInteiro(resumo.efetivoPrevisto),
    celulaInteiro(resumo.efetivoAlocado),
    celulaInteiro(resumo.registrosLancados),
    celulaInteiro(resumo.pendentesLancamento),
    celulaInteiro(resumo.presentes),
    celulaInteiro(resumo.ausentes),
    celulaPercentual(resumo.percentualCobertura),
    "Previsto − alocados; pendência de lançamento é apurada separadamente",
  ] });

  rows.push({ cells: [] });
  mergeLinha("SÉRIE HISTÓRICA", estiloSecaoAmarela);
  rows.push({ cells: ["Data", "Previsto", "Alocados", "Lançados", "Pendentes", "Presentes", "Ausentes", "Déficit", "% Completude alocados", "% Atendimento"].map((item) => celulaXlsx(item, estiloCabecalhoAmarelo)) });
  serie.forEach((item) => rows.push({ cells: [
    dataPtBr(item.data),
    celulaInteiro(item.efetivoPrevisto),
    celulaInteiro(item.efetivoAlocado),
    celulaInteiro(item.registrosLancados),
    celulaInteiro(item.pendentesLancamento),
    celulaInteiro(item.presentes),
    celulaInteiro(item.ausentes),
    celulaInteiro(item.deficitContratual),
    celulaPercentual(item.percentualCobertura),
    celulaPercentual(item.percentualAtendimento),
  ] }));

  if (quadro.length) {
    rows.push({ cells: [] });
    mergeLinha("COMPOSIÇÃO OPERACIONAL DA REFERÊNCIA", estiloSecao);
    rows.push({ cells: ["Contrato", "Lotação", "Turno", "Cargo", "Alocados", "Lançados", "Pendentes", "Presentes", "Ausentes", "% Cobertura"].map((item) => celulaXlsx(item, estiloCabecalho)) });
    quadro.forEach((item) => rows.push({ cells: [
      item.contrato || "Sem contrato",
      item.lotacao || "Sem lotação",
      item.turno || "Sem turno",
      item.cargo || "Sem cargo",
      celulaInteiro(item.efetivoAlocado),
      celulaInteiro(item.registrosLancados),
      celulaInteiro(item.pendentesLancamento),
      celulaInteiro(item.presentes),
      celulaInteiro(item.ausentes),
      celulaPercentual(item.percentualCobertura),
    ] }));
  }

  return {
    name: "Indicador de efetivo",
    columns: [
      { width: 28 }, { width: 22 }, { width: 18 }, { width: 18 }, { width: 18 },
      { width: 18 }, { width: 18 }, { width: 18 }, { width: 20 }, { width: 44 },
    ],
    rows,
    merges,
    freezeRows: 5,
  };
};

const planilhaMetodologia = (dados, periodoDias) => ({
  name: "Metodologia",
  columns: [{ width: 24 }, { width: 96 }, { width: 42 }],
  rows: [
    { cells: [celulaXlsx("METODOLOGIA E DICIONÁRIO GERENCIAL", estiloTitulo)], height: 28 },
    { cells: [celulaXlsx(`Painel de ${periodoDias} dias · referência ${dados.referencia || "—"} · gerado ${dados.geradoEm || "—"}`, estiloSubtitulo)] },
    { cells: [] },
    { cells: [celulaXlsx("DEFINIÇÕES DOS PRINCIPAIS INDICADORES", estiloSecao)] },
    { cells: ["Indicador", "Definição", "Uso recomendado"].map((item) => celulaXlsx(item, estiloCabecalho)) },
    { cells: ["Ocupação contratual", "Efetivo vinculado dividido pelo efetivo previsto dos contratos vigentes.", "Dimensionamento; acima de 100% representa mais vínculos que o previsto."] },
    { cells: ["Saldo contratual", "Efetivo previsto menos efetivo vinculado.", "Positivo indica capacidade não preenchida; negativo indica excesso sobre o previsto."] },
    { cells: ["Cobertura da frequência", "Registros lançados entre empregados efetivamente esperados na escala da referência.", "Controle de completude do lançamento diário."] },
    { cells: ["Taxa de presença", "Taxa calculada pelo backend sobre registros operacionais do período.", "Acompanhar tendência e comparar períodos de igual duração."] },
    { cells: ["Taxa de absenteísmo", "Taxa calculada pelo backend sobre ausências operacionais no período.", "Investigar concentração, recorrência e padrão semanal sem inferir causa automaticamente."] },
    { cells: ["RT", "Trabalho em repouso/folga registrado como RT.", "Indicador de pressão operacional; não prova substituição individual de ausência."] },
    { cells: ["Integridade", "Contagem de inconsistências cadastrais, organizacionais, contratuais e de frequência verificadas pelo painel.", "Usar como pré-condição para confiar em análises detalhadas."] },
    { cells: [] },
    { cells: [celulaXlsx("CRITÉRIOS DE CLASSIFICAÇÃO DAS UNIDADES NO XLSX", estiloSecaoAmarela)] },
    { cells: ["Classificação", "Critério transparente", "Observação"].map((item) => celulaXlsx(item, estiloCabecalhoAmarelo)) },
    { cells: [celulaXlsx("CRÍTICO", estiloCritico), "Alerta contratual > 0; ou cobertura < 85% quando há escala esperada; ou ocupação < 80% / > 110% quando aplicável.", "Prioridade de revisão gerencial."] },
    { cells: [celulaXlsx("ATENÇÃO", estiloAtencao), "Sem condição crítica, mas com pendências/ausências ou cobertura entre 85% e 95%.", "Acompanhar e tratar desvios antes de se tornarem críticos."] },
    { cells: [celulaXlsx("CONTROLADO", estiloOk), "Nenhum dos critérios explícitos de crítico/atenção foi acionado.", "Não significa ausência total de risco; significa apenas que esses critérios não dispararam."] },
    { cells: [] },
    { cells: [celulaXlsx("NOTAS METODOLÓGICAS DO SERVIDOR", estiloSecao)] },
    ...(dados.notasMetodologicas || []).map((item, indice) => ({ cells: [indice + 1, item] })),
    { cells: [] },
    { cells: [celulaXlsx("IDENTIDADE VISUAL", estiloSecaoAmarela)] },
    { cells: ["Paleta", "Azul Correios e Amarelo Correios conforme identidade visual institucional usada no sistema.", "Cores semânticas de alerta (vermelho/laranja/verde) são usadas somente para risco e conformidade."] },
  ],
  merges: ["A1:C1", "A2:C2", "A4:C4", "A14:C14", "A20:C20", `A${22 + (dados.notasMetodologicas || []).length}:C${22 + (dados.notasMetodologicas || []).length}`],
  freezeRows: 5,
});

export const criarConfiguracaoPainelXlsx = (dados, periodoDias, indicadorEfetivo = null) => {
  const planilhaNovoIndicador = planilhaIndicadorEfetivoOperacional(indicadorEfetivo);
  return {
    creator: "SGT-GTRAT",
    sheets: [
      planilhaDashboardExecutivo(dados, periodoDias),
      planilhaEfetivoContratos(dados),
      planilhaFrequenciaAbsenteismo(dados, periodoDias),
      planilhaEvolucao(dados),
      planilhaUnidades(dados),
      planilhaRiscosAlertas(dados),
      planilhaIntegridade(dados),
      ...(planilhaNovoIndicador ? [planilhaNovoIndicador] : []),
      planilhaMetodologia(dados, periodoDias),
    ],
  };
};

export const exportarPainelGerencial = async (dados, periodoDias, indicadorEfetivo = null) => {
  if (!dados) return;
  try {
    const escopo = nomeArquivoSeguro(dados.escopo?.nome);
    baixarArquivoXlsx(
      criarConfiguracaoPainelXlsx(dados, periodoDias, indicadorEfetivo),
      `painel_gerencial_executivo_${escopo}_${dados.referencia || "referencia"}.xlsx`,
    );
    notificar("success", "Painel exportado em Excel com dados cadastrais, operacionais, novo indicador de efetivo, riscos e metodologia.");
  } catch (erro) {
    console.error("Falha ao exportar painel gerencial em XLSX:", erro);
    notificar("error", "Não foi possível exportar o painel em Excel.");
  }
};

const linhasResumoPdf = (dados, indicadorEfetivo = null) => {
  const quadro = dados.quadro || {};
  const cadastro = dados.analiseCadastro || {};
  const efetivo = dados.efetivo || {};
  const contratos = dados.contratos || {};
  const frequencia = dados.frequencia || {};
  const periodo = dados.periodo || {};
  const integridade = dados.integridade || {};
  return [
    ["Colaboradores cadastrados", valor(quadro.empregadosCadastrados), `${valor(quadro.ativos)} ativos`],
    ["Admitidos no período", valor(cadastro.admitidosNoPeriodo), "Baseado em empregado.data_admissao"],
    ["Efetivo contratual", `${valor(efetivo.efetivoVinculado)} / ${valor(efetivo.efetivoPrevisto)}`, `Saldo ${valor(efetivo.saldoContratual)}`],
    ["Contratos vigentes", valor(contratos.vigentes), `${valor(contratos.vencendoEm30Dias)} vencendo em 30 dias`],
    ["Cobertura da frequência", textoCobertura(frequencia), `${valor(frequencia.pendentes)} pendente(s)`],
    ["Absenteísmo", textoTaxaOperacional(periodo, "taxaAbsenteismo"), `${valor(periodo.empregadosComAusencia)} colaborador(es) com ausência`],
    ["Integridade", valor(integridade.totalInconsistencias), integridade.totalInconsistencias ? "Requer saneamento" : "Sem inconsistências nos controles verificados"],
    ...(indicadorEfetivo?.dados?.resumoAtual ? [
      ["Cobertura dos alocados", textoPercentual(indicadorEfetivo.dados.resumoAtual.percentualCobertura), `${valor(indicadorEfetivo.dados.resumoAtual.registrosLancados)} registros de ${valor(indicadorEfetivo.dados.resumoAtual.efetivoAlocado)} alocados`],
      ["Presença contratual", textoPercentual(indicadorEfetivo.dados.resumoAtual.percentualAtendimento), `${valor(indicadorEfetivo.dados.resumoAtual.presentes)} presentes · ${valor(indicadorEfetivo.dados.resumoAtual.pendentesLancamento)} pendência(s) do operador`],
    ] : []),
  ];
};

const adicionarCabecalhoPdf = (doc, dados, periodoDias, titulo = "Painel Gerencial SGT-GTRAT") => {
  const largura = doc.internal.pageSize.getWidth();
  doc.setFillColor(0, 65, 107);
  doc.rect(0, 0, largura, 58, "F");
  doc.setFillColor(255, 212, 0);
  doc.rect(0, 58, largura, 4, "F");
  doc.setTextColor(255, 255, 255);
  doc.setFont("helvetica", "bold");
  doc.setFontSize(17);
  doc.text(titulo, 34, 28);
  doc.setFont("helvetica", "normal");
  doc.setFontSize(8.5);
  doc.text(`Escopo: ${dados.escopo?.nome || "—"} | Referência: ${dataPtBr(dados.referencia)} | Janela: ${periodoDias} dias`, 34, 44);
  doc.setTextColor(45, 61, 70);
};

const adicionarRodapePdf = (doc, dados) => {
  const paginas = doc.getNumberOfPages();
  for (let pagina = 1; pagina <= paginas; pagina += 1) {
    doc.setPage(pagina);
    const largura = doc.internal.pageSize.getWidth();
    const altura = doc.internal.pageSize.getHeight();
    doc.setDrawColor(222, 229, 234);
    doc.line(34, altura - 27, largura - 34, altura - 27);
    doc.setTextColor(105, 116, 124);
    doc.setFontSize(7.5);
    doc.text(`SGT-GTRAT | Dados consolidados da base | Gerado em ${dataPtBr(String(dados.geradoEm || "").slice(0, 10))}`, 34, altura - 14);
    doc.text(`Página ${pagina} de ${paginas}`, largura - 34, altura - 14, { align: "right" });
  }
};

export const exportarPainelPdf = async (dados, periodoDias, indicadorEfetivo = null) => {
  if (!dados) return;
  try {
    const [{ default: jsPDF }, { default: autoTable }] = await Promise.all([
      import("jspdf"),
      import("jspdf-autotable"),
    ]);
    const doc = new jsPDF("p", "pt", "a4");
    const margem = 34;
    const larguraUtil = doc.internal.pageSize.getWidth() - (margem * 2);
    const quadro = dados.quadro || {};
    const cadastro = dados.analiseCadastro || {};
    const contratos = dados.contratos || {};
    const frequencia = dados.frequencia || {};
    const periodo = dados.periodo || {};
    const integridade = dados.integridade || {};
    const admissoes = dados.admissoesMensais || [];
    const alertas = dados.alertas || [];
    const unidades = dados.unidades || [];

    adicionarCabecalhoPdf(doc, dados, periodoDias, "Painel Gerencial - Resumo Executivo");
    doc.setFont("helvetica", "bold");
    doc.setFontSize(11);
    doc.text("Indicadores principais", margem, 86);
    autoTable(doc, {
      startY: 96,
      margin: { left: margem, right: margem, bottom: 42 },
      head: [["Indicador", "Valor", "Leitura"]],
      body: linhasResumoPdf(dados, indicadorEfetivo),
      theme: "grid",
      styles: { fontSize: 8.2, cellPadding: 5, overflow: "linebreak", textColor: [45, 61, 70] },
      headStyles: { fillColor: [0, 65, 107], textColor: [255, 255, 255], fontStyle: "bold" },
      alternateRowStyles: { fillColor: [247, 250, 252] },
      columnStyles: { 0: { cellWidth: 150 }, 1: { cellWidth: 92, fontStyle: "bold" }, 2: { cellWidth: larguraUtil - 242 } },
    });

    let y = (doc.lastAutoTable?.finalY || 250) + 20;
    doc.setFont("helvetica", "bold");
    doc.setFontSize(11);
    doc.text("Análise cadastral real", margem, y);
    autoTable(doc, {
      startY: y + 10,
      margin: { left: margem, right: margem, bottom: 42 },
      head: [["Métrica", "Valor", "Fonte / interpretação"]],
      body: [
        ["Cadastrados", valor(quadro.empregadosCadastrados), "Cadastro de empregados no escopo e na referência"],
        ["Admitidos no período", valor(cadastro.admitidosNoPeriodo), "empregado.data_admissao"],
        ["Admitidos no mês da referência", valor(cadastro.admitidosNoMesReferencia), "empregado.data_admissao"],
        ["Admitidos nos últimos 12 meses", valor(cadastro.admitidosUltimos12Meses), "empregado.data_admissao"],
        ["Com data de admissão", valor(cadastro.comDataAdmissao), textoPercentual(cadastro.percentualComDataAdmissao)],
        ["Sem data de admissão", valor(cadastro.semDataAdmissao), "Saneamento recomendado para análises temporais"],
      ],
      theme: "grid",
      styles: { fontSize: 8, cellPadding: 4.5, overflow: "linebreak" },
      headStyles: { fillColor: [0, 65, 107], textColor: [255, 255, 255] },
      alternateRowStyles: { fillColor: [248, 250, 252] },
    });

    y = (doc.lastAutoTable?.finalY || y) + 16;
    if (admissoes.length) {
      doc.setFont("helvetica", "bold");
      doc.setFontSize(10.5);
      doc.text("Admissões mensais - últimos 12 meses", margem, y);
      autoTable(doc, {
        startY: y + 8,
        margin: { left: margem, right: margem, bottom: 42 },
        head: [["Mês", "Admissões"]],
        body: admissoes.map((item) => [dataPtBr(item.mes), valor(item.quantidade)]),
        theme: "striped",
        styles: { fontSize: 8, cellPadding: 4 },
        headStyles: { fillColor: [0, 65, 107], textColor: [255, 255, 255] },
      });
    }

    doc.addPage();
    adicionarCabecalhoPdf(doc, dados, periodoDias, "Painel Gerencial - Operação e Contratos");
    autoTable(doc, {
      startY: 82,
      margin: { left: margem, right: margem, bottom: 42 },
      head: [["Operação", "Valor", "Interpretação"]],
      body: [
        ["Empregados esperados", valor(frequencia.empregadosEsperados), "Definidos pela escala da referência"],
        ["Registros lançados", valor(frequencia.registrosLancados ?? frequencia.registrosEsperados), "Lançamentos entre os esperados"],
        ["Pendentes", valor(frequencia.pendentes), "Esperados sem lançamento"],
        ["Cobertura", textoCobertura(frequencia), Number(frequencia.empregadosEsperados || 0) > 0 ? "Lançados / esperados" : "Não aplicável sem empregados esperados"],
        ["Registros operacionais no período", valor(periodo.registrosOperacionais), `Janela de ${periodoDias} dias`],
        ["Taxa de presença", textoTaxaOperacional(periodo, "taxaPresenca"), Number(periodo.registrosOperacionais || 0) > 0 ? "Calculada sobre lançamentos reais" : "Sem base operacional"],
        ["Taxa de absenteísmo", textoTaxaOperacional(periodo, "taxaAbsenteismo"), Number(periodo.registrosOperacionais || 0) > 0 ? "Calculada sobre lançamentos reais" : "Sem base operacional"],
        ["Faltas não justificadas", valor(periodo.faltasNaoJustificadas), "Componente das ausências"],
        ["Atestados", valor(periodo.atestados), "Componente das ausências"],
        ["Repousos trabalhados (RT)", valor(periodo.repousosTrabalhados), "Indicador de pressão operacional"],
      ],
      theme: "grid",
      styles: { fontSize: 8.2, cellPadding: 5, overflow: "linebreak" },
      headStyles: { fillColor: [0, 65, 107], textColor: [255, 255, 255] },
      alternateRowStyles: { fillColor: [247, 250, 252] },
    });

    y = (doc.lastAutoTable?.finalY || 310) + 18;
    doc.setFont("helvetica", "bold");
    doc.setFontSize(10.5);
    doc.text("Contratos e integridade", margem, y);
    autoTable(doc, {
      startY: y + 8,
      margin: { left: margem, right: margem, bottom: 42 },
      head: [["Controle", "Quantidade", "Leitura"]],
      body: [
        ["Contratos vigentes", valor(contratos.vigentes), "Vigentes na referência"],
        ["Vencendo em 30 dias", valor(contratos.vencendoEm30Dias), "Requer planejamento de renovação/encerramento"],
        ["Vencidos ainda ativos", valor(contratos.vencidosMarcadosAtivos), "Requer regularização quando maior que zero"],
        ["Inconsistências detectadas", valor(integridade.totalInconsistencias), "Controles objetivos de integridade do painel"],
        ["Ativos sem contrato", valor(integridade.empregadosAtivosSemContrato), "Vínculo contratual ausente"],
        ["Ativos sem lotação", valor(integridade.empregadosAtivosSemLotacao), "Estrutura organizacional ausente"],
        ["Ativos sem jornada", valor(integridade.empregadosAtivosSemJornada), "Configuração operacional ausente"],
      ],
      theme: "grid",
      styles: { fontSize: 8, cellPadding: 4.5, overflow: "linebreak" },
      headStyles: { fillColor: [0, 65, 107], textColor: [255, 255, 255] },
      alternateRowStyles: { fillColor: [248, 250, 252] },
    });

    if (indicadorEfetivo?.dados?.resumoAtual) {
      const dadosIndicador = indicadorEfetivo.dados;
      const contextoIndicador = indicadorEfetivo.contexto || {};
      const resumoIndicador = dadosIndicador.resumoAtual || {};
      const serieIndicador = Array.isArray(dadosIndicador.serieAtual) ? dadosIndicador.serieAtual : [];
      const quadroIndicador = Array.isArray(dadosIndicador.quadroDimensionado) ? dadosIndicador.quadroDimensionado : [];

      doc.addPage("a4", "landscape");
      adicionarCabecalhoPdf(doc, dados, periodoDias, "Painel Gerencial - Indicador de Efetivo");
      doc.setFont("helvetica", "normal");
      doc.setFontSize(8);
      doc.text(`Escopo do indicador: ${contextoIndicador.escopoSelecionado || "—"} | Período: ${contextoIndicador.periodoRotulo || dadosIndicador.periodo || "—"} | Referência: ${dataPtBr(contextoIndicador.referencia || dadosIndicador.fim)}`, 28, 76);
      autoTable(doc, {
        startY: 86,
        margin: { left: 28, right: 28, bottom: 42 },
        head: [["Indicador", "Valor", "Previsto", "Alocados", "Lançados", "Pendentes", "Leitura"]],
        body: [
          ["Cobertura dos alocados", textoPercentual(resumoIndicador.percentualCobertura), valor(resumoIndicador.efetivoPrevisto), valor(resumoIndicador.efetivoAlocado), valor(resumoIndicador.registrosLancados), valor(resumoIndicador.pendentesLancamento), "Percentual devolvido pelo Java"],
          ["Presença contratual", textoPercentual(resumoIndicador.percentualAtendimento), valor(resumoIndicador.efetivoPrevisto), valor(resumoIndicador.efetivoAlocado), valor(resumoIndicador.registrosLancados), valor(resumoIndicador.pendentesLancamento), "Presentes ÷ efetivo previsto"],
          ["Déficit contratual", valor(resumoIndicador.deficitContratual), valor(resumoIndicador.efetivoPrevisto), valor(resumoIndicador.efetivoAlocado), valor(resumoIndicador.registrosLancados), valor(resumoIndicador.pendentesLancamento), "Previsto − alocados"],
        ],
        theme: "grid",
        styles: { fontSize: 7.6, cellPadding: 4, overflow: "linebreak" },
        headStyles: { fillColor: [0, 65, 107], textColor: [255, 255, 255] },
        alternateRowStyles: { fillColor: [248, 250, 252] },
      });

      let indicadorY = (doc.lastAutoTable?.finalY || 140) + 16;
      if (serieIndicador.length) {
        doc.setFont("helvetica", "bold");
        doc.setFontSize(10);
        doc.text("Série histórica do indicador", 28, indicadorY);
        autoTable(doc, {
          startY: indicadorY + 7,
          margin: { left: 28, right: 28, bottom: 42 },
          head: [["Data", "Previsto", "Alocados", "Lançados", "Pendentes", "Presentes", "Ausentes", "% Completude alocados", "% Atendimento"]],
          body: serieIndicador.map((item) => [dataPtBr(item.data), valor(item.efetivoPrevisto), valor(item.efetivoAlocado), valor(item.registrosLancados), valor(item.pendentesLancamento), valor(item.presentes), valor(item.ausentes), textoPercentual(item.percentualCobertura), textoPercentual(item.percentualAtendimento)]),
          theme: "striped",
          styles: { fontSize: 6.8, cellPadding: 3, overflow: "linebreak" },
          headStyles: { fillColor: [0, 65, 107], textColor: [255, 255, 255] },
        });
      }

      if (quadroIndicador.length) {
        doc.addPage("a4", "landscape");
        adicionarCabecalhoPdf(doc, dados, periodoDias, "Painel Gerencial - Quadro Dimensionado do Efetivo");
        autoTable(doc, {
          startY: 82,
          margin: { left: 28, right: 28, bottom: 42 },
          head: [["Contrato", "Lotação", "Turno", "Cargo", "Alocados", "Lançados", "Pendentes", "Presentes", "Ausentes", "% Cobertura"]],
          body: quadroIndicador.map((item) => [
            item.contrato || "Sem contrato",
            item.lotacao || "Sem lotação",
            item.turno || "Sem turno",
            item.cargo || "Sem cargo",
            valor(item.efetivoAlocado),
            valor(item.registrosLancados),
            valor(item.pendentesLancamento),
            valor(item.presentes),
            valor(item.ausentes),
            textoPercentual(item.percentualCobertura),
          ]),
          theme: "grid",
          styles: { fontSize: 6.6, cellPadding: 3, overflow: "linebreak" },
          headStyles: { fillColor: [0, 65, 107], textColor: [255, 255, 255] },
          alternateRowStyles: { fillColor: [248, 250, 252] },
          columnStyles: { 0: { cellWidth: 100 }, 1: { cellWidth: 135 }, 2: { cellWidth: 105 }, 3: { cellWidth: 135 } },
        });
      }
    }

    doc.addPage("a4", "landscape");
    adicionarCabecalhoPdf(doc, dados, periodoDias, "Painel Gerencial - Unidades e Riscos");
    autoTable(doc, {
      startY: 80,
      margin: { left: 28, right: 28, bottom: 42 },
      head: [["Unidade", "UF", "Efetivo", "Previsto", "Ocupação", "Esperados", "Lançados", "Cobertura", "Pendentes", "Alertas"]],
      body: unidades.map((item) => [
        item.nome || "—",
        item.uf || "—",
        valor(item.efetivoAtual),
        item.efetivoPrevisto == null ? "—" : valor(item.efetivoPrevisto),
        item.percentualOcupacao == null ? "—" : textoPercentual(item.percentualOcupacao),
        valor(item.empregadosEsperados),
        valor(item.registrosLancados ?? item.registrosEsperados),
        Number(item.empregadosEsperados || 0) > 0 ? textoPercentual(item.percentualCoberturaEscala) : "N/A",
        valor(item.pendentesFrequencia),
        valor(item.alertasContratuais),
      ]),
      theme: "grid",
      styles: { fontSize: 6.8, cellPadding: 3.5, overflow: "linebreak" },
      headStyles: { fillColor: [0, 65, 107], textColor: [255, 255, 255], fontStyle: "bold" },
      alternateRowStyles: { fillColor: [248, 250, 252] },
      columnStyles: { 0: { cellWidth: 155 } },
    });

    y = (doc.lastAutoTable?.finalY || 320) + 18;
    if (y > doc.internal.pageSize.getHeight() - 140) {
      doc.addPage("a4", "landscape");
      adicionarCabecalhoPdf(doc, dados, periodoDias, "Painel Gerencial - Alertas e Metodologia");
      y = 82;
    }
    doc.setFont("helvetica", "bold");
    doc.setFontSize(10.5);
    doc.text("Alertas gerenciais", 28, y);
    autoTable(doc, {
      startY: y + 8,
      margin: { left: 28, right: 28, bottom: 42 },
      head: [["Severidade", "Título", "Mensagem"]],
      body: alertas.length ? alertas.map((item) => [item.severidade || "INFO", item.titulo || "—", item.mensagem || "—"]) : [["INFO", "Sem alertas", "Nenhum alerta gerencial foi produzido para o escopo e a referência."]],
      theme: "grid",
      styles: { fontSize: 7.2, cellPadding: 4, overflow: "linebreak" },
      headStyles: { fillColor: [0, 65, 107], textColor: [255, 255, 255] },
      alternateRowStyles: { fillColor: [248, 250, 252] },
      columnStyles: { 0: { cellWidth: 72 }, 1: { cellWidth: 180 } },
    });

    const notas = dados.notasMetodologicas || [];
    y = (doc.lastAutoTable?.finalY || y) + 18;
    if (y > doc.internal.pageSize.getHeight() - 120) {
      doc.addPage("a4", "landscape");
      adicionarCabecalhoPdf(doc, dados, periodoDias, "Painel Gerencial - Metodologia");
      y = 82;
    }
    doc.setFont("helvetica", "bold");
    doc.setFontSize(10.5);
    doc.text("Notas metodológicas", 28, y);
    doc.setFont("helvetica", "normal");
    doc.setFontSize(8);
    const textos = notas.length ? notas : ["Os indicadores refletem os dados consolidados pelo servidor dentro do escopo autenticado.", "Ausência de lançamentos operacionais é apresentada como sem lançamentos/não aplicável, sem números fictícios."];
    let linhaY = y + 16;
    textos.forEach((nota, indice) => {
      const linhas = doc.splitTextToSize(`${indice + 1}. ${nota}`, doc.internal.pageSize.getWidth() - 56);
      doc.text(linhas, 28, linhaY);
      linhaY += (linhas.length * 10) + 4;
    });

    adicionarRodapePdf(doc, dados);
    const escopo = nomeArquivoSeguro(dados.escopo?.nome);
    doc.save(`painel_gerencial_executivo_${escopo}_${dados.referencia || "referencia"}.pdf`);
    notificar("success", "Painel exportado em PDF com resumo, novo indicador de efetivo, operação, unidades, riscos e metodologia.");
  } catch (erro) {
    console.error("Falha ao exportar painel gerencial em PDF:", erro);
    notificar("error", "Não foi possível exportar o painel em PDF.");
  }
};
