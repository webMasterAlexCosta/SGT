import type * as DTO from '../../../../contracts/java';
import type { CoverageView, ManagementPanelView, IndicatorHelp, SummaryItem, ChartTooltipProps } from '../../../../contracts/painel';
import React, { useState } from "react";
import {
  FileSpreadsheet,
  FileText,
  AlertTriangle,
  BadgeCheck,
  CalendarClock,
  CheckCircle2,
  ChevronRight,
  ClipboardCheck,
  FileWarning,
  RefreshCw,
  ShieldCheck,
  Users,
} from "lucide-react";
import {
  CartesianGrid,
  Legend,
  Line,
  LineChart,
  ResponsiveContainer,
  Tooltip,
  XAxis,
  YAxis,
} from "recharts";
import estilos from "./index.module.css";
import { BotaoAjudaPainel, BotaoExpandirPainel, ModalAjudaIndicador, ModalPainel } from "../ModalPainel/index.tsx";

const numero = (valor: (unknown) | (number) | (number | undefined) | (string | number | undefined)) => new Intl.NumberFormat("pt-BR").format(Number(valor || 0));
const percentual = (valor: (number | undefined) | (number)) => `${Number(valor || 0).toLocaleString("pt-BR", { maximumFractionDigits: 1 })}%`;
const dataBr = (valor: (`${number}-${number}-${number}` | null | undefined) | (string)) => valor ? new Intl.DateTimeFormat("pt-BR").format(new Date(`${valor}T12:00:00`)) : "—";

const classeSaldo = (valor: (number | undefined) | (number | null)) => Number(valor || 0) < 0 ? estilos.valorCritico : estilos.valorNormal;
const classeSeveridade = (valor: (string | null)) => estilos[String(valor || "INFO").toLowerCase()] || estilos.info;

const CORES = {
  principal: "#0f5f8f",
  positivo: "#138a63",
  alerta: "#d48b00",
  critico: "#c03c30",
  informativo: "#0f8bd6",
};

const AJUDA = {
  cadastro: {
    finalidade: "Mostra quantos colaboradores existem de fato no cadastro dentro do escopo do usuário, independentemente de haver frequência lançada.",
    calculo: "Conta empregados com data de admissão nula ou não posterior à data de referência; o detalhe mostra o status atual e admissões do período selecionado.",
    fonte: "Tabela empregado e status_empregado, filtradas pelo escopo organizacional autenticado.",
    interpretacao: "Se este número for maior que zero e os indicadores operacionais estiverem sem dados, a base cadastral existe e o que falta são lançamentos/regras operacionais, não colaboradores fictícios.",
  },
  efetivo: {
    finalidade: "Mostra quantas pessoas ativas estão vinculadas a contratos vigentes em comparação ao efetivo previsto nesses contratos.",
    calculo: "Efetivo vinculado ÷ efetivo previsto. O saldo é previsto menos vinculado; não há estimativa ou preenchimento fictício.",
    fonte: "empregado, empregado_contrato, contrato e status_empregado, filtrados pelo escopo do usuário e pela data de referência.",
    interpretacao: "Saldo negativo indica mais pessoas vinculadas do que o previsto. Efetivo previsto igual a zero não produz uma ocupação artificial.",
  },
  contratos: {
    finalidade: "Indica a quantidade de contratos vigentes na data selecionada e destaca os que se aproximam do término.",
    calculo: "Conta contratos ativos cuja vigência contém a data de referência; vencimentos em 30 dias usam a data final cadastrada.",
    fonte: "Tabela contrato da base de dados, respeitando o gestor/localidade do usuário.",
    interpretacao: "Serve para antecipar renovação, substituição ou redimensionamento contratual.",
  },
  frequencia: {
    finalidade: "Mede se os colaboradores esperados para trabalhar na data possuem lançamento de frequência.",
    calculo: "Registros lançados para empregados esperados ÷ empregados esperados. Jornada, folgas aprovadas e calendário são considerados.",
    fonte: "empregado, jornada_dia_semana, frequencia, frequencia_excecao e calendario_evento.",
    interpretacao: "Se não houver empregados esperados na data, o indicador é não aplicável; zero lançamentos com pessoas esperadas representa cobertura real de 0%.",
  },
  integridade: {
    finalidade: "Sinaliza inconsistências cadastrais que podem distorcer decisões de contrato, frequência ou estrutura.",
    calculo: "Soma verificações objetivas, como ativo sem contrato, vínculos divergentes, jornada incompleta e estrutura ausente.",
    fonte: "Relacionamentos cadastrais e operacionais persistidos no banco; não usa amostragem.",
    interpretacao: "Zero significa que as regras verificadas não encontraram inconsistências, não que toda a base esteja perfeita em qualquer critério possível.",
  },
};

const TooltipPadrao = ({ active, label, payload }: ChartTooltipProps) => {
  if (!active || !payload?.length) return null;
  return (
    <div className={estilos.tooltipGrafico}>
      <strong>{label}</strong>
      <ul>
        {payload.map((item) => (
          <li key={`${item.name}-${item.dataKey}`}>
            <span style={{ background: item.color || item.fill || CORES.principal }} />
            <b>{item.name}:</b> {numero(item.value)}
          </li>
        ))}
      </ul>
    </div>
  );
};

const FaixaResumo = ({ itens = [] }: { itens?: SummaryItem[] }) => {
  const visiveis = itens.filter((item) => item && item.rotulo);
  if (!visiveis.length) return null;
  return (
    <div className={estilos.faixaResumo}>
      {visiveis.map((item) => (
        <div key={item.rotulo} className={`${estilos.chipResumo} ${item.classe || ""}`}>
          <span>{item.rotulo}</span>
          <strong>{item.valor}</strong>
        </div>
      ))}
    </div>
  );
};

const PainelExecutivo = ({ titulo, icone: Icone, detalhe, acao, ajuda, largo = false, children }: React.PropsWithChildren<{ titulo: string; icone: import("lucide-react").LucideIcon; detalhe?: React.ReactNode; acao?: React.ReactNode; ajuda?: IndicatorHelp; largo?: boolean }>) => {
  const [modalAberto, setModalAberto] = useState(false);
  const [ajudaAberta, setAjudaAberta] = useState(false);
  return (
    <>
      <article className={`${estilos.painel} ${largo ? estilos.painelLargo : ""}`}>
        <div className={estilos.tituloPainel}>
          <div><Icone size={20} aria-hidden="true" /><h2>{titulo}</h2></div>
          <div className={estilos.acoesTituloPainel}>
            {detalhe ? <span>{detalhe}</span> : null}
            {acao || null}
            {ajuda ? <BotaoAjudaPainel titulo={titulo} aoAbrir={() => setAjudaAberta(true)} /> : null}
            <BotaoExpandirPainel titulo={titulo} aoAbrir={() => setModalAberto(true)} />
          </div>
        </div>
        <div className={`${estilos.previewPainel} ${largo ? estilos.previewPainelLargo : ""}`}>
          {children}
        </div>
        <button type="button" className={estilos.abrirAnalise} onClick={() => setModalAberto(true)}>
          Ver análise completa <ChevronRight size={15} aria-hidden="true" />
        </button>
      </article>
      <ModalPainel aberto={modalAberto} titulo={titulo} detalhe={detalhe} aoFechar={() => setModalAberto(false)}>
        <div className={estilos.conteudoModal}>{children}</div>
      </ModalPainel>
      <ModalAjudaIndicador aberto={ajudaAberta} titulo={titulo} ajuda={ajuda} aoFechar={() => setAjudaAberta(false)} />
    </>
  );
};

export const CabecalhoGovernanca = ({
  dados,
  referencia,
  hoje,
  periodoDias,
  carregando,
  aoMudarReferencia,
  aoMudarPeriodo,
  aoRecarregar,
  aoExportarXlsx,
  aoExportarPdf,
}: { dados?: ManagementPanelView; referencia: string; hoje: string; periodoDias: number; carregando?: boolean; aoMudarReferencia: (data: string) => void; aoMudarPeriodo: (dias: string) => void; aoRecarregar: () => void; aoExportarXlsx: () => void; aoExportarPdf: () => void }) => (
  <header className={estilos.cabecalho}>
    <div className={estilos.identidadeCabecalho}>
      <span className={estilos.selo}>Painel de governança</span>
      <h1>Gestão operacional e contratual</h1>
      <p>
        {dados?.escopo?.nome || "Escopo autenticado"} · janela analítica de {dataBr(dados?.periodo?.inicio)} a {dataBr(dados?.periodo?.fim || dados?.referencia || referencia)}
        {" "}({numero(dados?.periodo?.dias || periodoDias)} dias) · referência final em {dataBr(dados?.referencia || referencia)}
      </p>
    </div>
    <div className={estilos.controlesCabecalho}>
      <div className={estilos.filtros}>
        <label>
          Período analítico
          <select value={periodoDias} onChange={(evento) => aoMudarPeriodo(evento.target.value)}>
            <option value="7">7 dias</option>
            <option value="15">15 dias</option>
            <option value="30">30 dias</option>
            <option value="60">60 dias</option>
            <option value="90">90 dias</option>
          </select>
        </label>
        <label>
          Data de referência
          <input type="date" value={referencia} max={hoje} onChange={(evento) => aoMudarReferencia(evento.target.value)} />
        </label>
        <button type="button" className={estilos.botaoAtualizar} onClick={aoRecarregar} disabled={carregando}>
          <RefreshCw size={17} aria-hidden="true" />Atualizar
        </button>
      </div>
      <div className={estilos.exportacoes} aria-label="Exportar painel">
        <span>Exportar relatório</span>
        <div>
          <button
            type="button"
            className={`${estilos.botaoExportacao} ${estilos.botaoExcel}`}
            onClick={aoExportarXlsx}
            disabled={!dados || carregando}
            title="Exportar dados do painel em Excel (.xlsx)"
          >
            <FileSpreadsheet size={19} aria-hidden="true" />
            <span>Excel</span>
            <small>.xlsx</small>
          </button>
          <button
            type="button"
            className={`${estilos.botaoExportacao} ${estilos.botaoPdf}`}
            onClick={aoExportarPdf}
            disabled={!dados || carregando}
            title="Exportar análise executiva do painel em PDF"
          >
            <FileText size={21} aria-hidden="true" />
            <span>PDF</span>
            <small>.pdf</small>
          </button>
        </div>
      </div>
    </div>
  </header>
);

const CartaoIndicador = ({ icone: Icone, titulo, valor, detalhe, ajuda, classeValor = "", classeCartao = "" }: { icone: import("lucide-react").LucideIcon; titulo: string; valor: React.ReactNode; detalhe?: React.ReactNode; ajuda?: IndicatorHelp; classeValor?: string; classeCartao?: string }) => {
  const [ajudaAberta, setAjudaAberta] = useState(false);
  return (
    <>
      <article className={`${estilos.cartaoIndicador} ${classeCartao}`}>
        <span className={estilos.icone}><Icone size={21} aria-hidden="true" /></span>
        <div className={estilos.conteudoIndicador}>
          <div className={estilos.tituloIndicador}><p>{titulo}</p>{ajuda ? <BotaoAjudaPainel titulo={titulo} aoAbrir={() => setAjudaAberta(true)} /> : null}</div>
          <strong className={classeValor}>{valor}</strong><small>{detalhe}</small>
        </div>
      </article>
      <ModalAjudaIndicador aberto={ajudaAberta} titulo={titulo} ajuda={ajuda} aoFechar={() => setAjudaAberta(false)} />
    </>
  );
};

export const ResumoExecutivo = ({ dados, indicadorEfetivo }: { dados?: ManagementPanelView; indicadorEfetivo?: Partial<import("../../../../contracts/painel").StaffingReport> }) => {
  const efetivo = dados?.efetivo || {};
  const quadro = dados?.quadro || {};
  const analiseCadastro = dados?.analiseCadastro || {};
  const contratos = dados?.contratos || {};
  const frequencia = dados?.frequencia || {};
  const periodo = dados?.periodo || {};
  const integridade = dados?.integridade || {};
  const resumoEfetivoOperacional = indicadorEfetivo?.dados?.resumoAtual || null;
  const contextoEfetivoOperacional = indicadorEfetivo?.contexto || {};
  const coberturaAlocados = Number(resumoEfetivoOperacional?.percentualCobertura || 0);
  const temOperacionais = Number(periodo.registrosOperacionais || 0) > 0;
  const ausenciasPeriodo = Number(periodo.faltasNaoJustificadas || 0)
    + Number(periodo.atestados || 0)
    + Number(periodo.suspensoes || 0);

  return (
    <section className={estilos.gradeIndicadores} aria-label="Resumo executivo">
      <CartaoIndicador ajuda={AJUDA.cadastro} icone={Users} titulo="Colaboradores cadastrados" valor={numero(quadro.empregadosCadastrados)} detalhe={`${numero(quadro.ativos)} ativos · ${numero(analiseCadastro.admitidosNoPeriodo)} admitido(s) na janela`} classeCartao={estilos.indicadorNeutro} />
      <CartaoIndicador ajuda={AJUDA.efetivo} icone={Users} titulo="Efetivo contratual na referência" valor={`${numero(efetivo.efetivoVinculado)} / ${numero(efetivo.efetivoPrevisto)}`} detalhe={`Saldo ${numero(efetivo.saldoContratual)} · ocupação ${percentual(efetivo.percentualOcupacao)}`} classeValor={classeSaldo(efetivo.saldoContratual)} classeCartao={estilos.indicadorAzul} />
      <CartaoIndicador ajuda={AJUDA.contratos} icone={ClipboardCheck} titulo="Contratos vigentes na referência" valor={numero(contratos.vigentes)} detalhe={`${numero(contratos.vencendoEm30Dias)} vencendo nos próximos 30 dias`} classeCartao={estilos.indicadorRoxo} />
      <CartaoIndicador ajuda={AJUDA.frequencia} icone={CheckCircle2} titulo="Cobertura na data de referência" valor={Number(frequencia.empregadosEsperados || 0) > 0 ? percentual(frequencia.percentualCobertura) : "Não aplicável"} detalhe={Number(frequencia.empregadosEsperados || 0) > 0 ? `${numero((frequencia.registrosLancados ?? frequencia.registrosEsperados))} lançados · ${numero(frequencia.pendentes)} pendentes em ${dataBr(frequencia.data || dados?.referencia)}` : `Nenhum colaborador esperado em ${dataBr(frequencia.data || dados?.referencia)}`} classeCartao={estilos.indicadorVerde} />
      <CartaoIndicador
        icone={CheckCircle2}
        titulo="Atendimento contratual"
        valor={resumoEfetivoOperacional ? percentual(resumoEfetivoOperacional.percentualAtendimento) : "—"}
        detalhe={resumoEfetivoOperacional
          ? `${numero(resumoEfetivoOperacional.presentes)} presentes de ${numero(resumoEfetivoOperacional.efetivoPrevisto)} previstos · ${contextoEfetivoOperacional.periodoRotulo || "período selecionado"}`
          : "Carregando indicador contratual de efetivo"}
        classeCartao={estilos.indicadorAzul}
      />
      <CartaoIndicador
        icone={ClipboardCheck}
        titulo="Cobertura dos alocados"
        valor={resumoEfetivoOperacional ? percentual(coberturaAlocados) : "—"}
        detalhe={resumoEfetivoOperacional
          ? `${numero(resumoEfetivoOperacional.registrosLancados)} registros de ${numero(resumoEfetivoOperacional.efetivoAlocado)} alocados · ${numero(resumoEfetivoOperacional.pendentesLancamento)} pendência(s) do operador`
          : "Aguardando consolidação do indicador"}
        classeCartao={Number(resumoEfetivoOperacional?.pendentesLancamento || 0) > 0 ? estilos.indicadorAlerta : estilos.indicadorVerde}
      />
      <CartaoIndicador icone={CalendarClock} titulo={`Absenteísmo — ${numero(periodo.dias)} dias`} valor={temOperacionais ? percentual(periodo.taxaAbsenteismo) : "Sem lançamentos"} detalhe={`${numero(ausenciasPeriodo)} ausência(s) · ${dataBr(periodo.inicio)} a ${dataBr(periodo.fim)}`} classeCartao={estilos.indicadorAlerta} />
      <CartaoIndicador ajuda={AJUDA.integridade} icone={ShieldCheck} titulo="Integridade dos dados" valor={numero(integridade.totalInconsistencias)} detalhe={integridade.totalInconsistencias ? "inconsistências para saneamento" : "nenhuma inconsistência detectada"} classeValor={integridade.totalInconsistencias ? estilos.valorCritico : estilos.valorNormal} classeCartao={integridade.totalInconsistencias ? estilos.indicadorAlerta : estilos.indicadorNeutro} />
    </section>
  );
};

export const SaudeContratual = ({ contratos, efetivo, podeAbrir, aoNavegar }: { contratos?: Partial<DTO.PainelGestaoDTOSaudeContratual>; efetivo?: Partial<DTO.PainelGestaoDTOResumoEfetivo>; podeAbrir?: boolean; aoNavegar: (rota: string) => void }) => (
  <PainelExecutivo
    titulo="Saúde contratual"
    icone={FileWarning}
    detalhe="Riscos e dimensionamento"
    ajuda={AJUDA.contratos}
    acao={podeAbrir ? <button type="button" onClick={() => aoNavegar("/contratos")}>Abrir contratos</button> : null}
  >
    <FaixaResumo itens={[
      { rotulo: "Vigentes", valor: numero(contratos?.vigentes), classe: estilos.resumoInfo },
      { rotulo: "Vencendo em 30 dias", valor: numero(contratos?.vencendoEm30Dias), classe: estilos.resumoAtencao },
      { rotulo: "Sem contrato", valor: numero(efetivo?.ativosSemContrato), classe: Number(efetivo?.ativosSemContrato || 0) > 0 ? estilos.resumoCritico : estilos.resumoSucesso },
    ]} />
    <div className={estilos.matrizMetricas}>
      <div><span>Vencidos ainda ativos</span><strong className={contratos?.vencidosMarcadosAtivos ? estilos.valorCritico : ""}>{numero(contratos?.vencidosMarcadosAtivos)}</strong></div>
      <div><span>Sem data final</span><strong>{numero(contratos?.semDataFim)}</strong></div>
      <div><span>Sem efetivo previsto</span><strong className={contratos?.semEfetivoPrevisto ? estilos.valorAlerta : ""}>{numero(contratos?.semEfetivoPrevisto)}</strong></div>
      <div><span>Acima do previsto</span><strong className={contratos?.acimaDoEfetivoPrevisto ? estilos.valorAlerta : ""}>{numero(contratos?.acimaDoEfetivoPrevisto)}</strong></div>
      <div><span>Abaixo do previsto</span><strong>{numero(contratos?.abaixoDoEfetivoPrevisto)}</strong></div>
      <div><span>Ativos sem contrato</span><strong className={efetivo?.ativosSemContrato ? estilos.valorCritico : ""}>{numero(efetivo?.ativosSemContrato)}</strong></div>
    </div>
  </PainelExecutivo>
);

export const CoberturaFrequencia = ({ frequencia = {}, podeOperar, aoNavegar }: { frequencia?: CoverageView; podeOperar?: boolean; aoNavegar: (rota: string) => void }) => {
  const temEsperados = Number(frequencia.empregadosEsperados || 0) > 0;
  const coberturaExibida = temEsperados ? percentual(frequencia.percentualCobertura) : "Não aplicável";
  return (
    <PainelExecutivo
      titulo="Cobertura na data de referência"
      icone={CalendarClock}
      ajuda={AJUDA.frequencia}
      detalhe={`${dataBr(frequencia.data)} · ${coberturaExibida}`}
      acao={podeOperar ? <button type="button" onClick={() => aoNavegar("/frequencia")}>Abrir frequência</button> : null}
    >
      <div className={estilos.barra} aria-label={temEsperados ? `Cobertura de ${coberturaExibida}` : "Cobertura não aplicável"}>
        <span style={{ width: `${temEsperados ? Math.min(100, Math.max(0, Number(frequencia.percentualCobertura || 0))) : 0}%` }} />
      </div>
      <FaixaResumo itens={[
        { rotulo: "Cobertura no dia", valor: coberturaExibida, classe: estilos.resumoSucesso },
        { rotulo: "Pendentes no dia", valor: numero(frequencia.pendentes), classe: Number(frequencia.pendentes || 0) > 0 ? estilos.resumoAtencao : estilos.resumoInfo },
        { rotulo: "Faltas no dia", valor: numero(frequencia.faltasNaoJustificadas), classe: Number(frequencia.faltasNaoJustificadas || 0) > 0 ? estilos.resumoCritico : estilos.resumoInfo },
        { rotulo: "Atestados no dia", valor: numero(frequencia.ausenciasJustificadas), classe: estilos.resumoAtencao },
        { rotulo: "RT no dia", valor: numero(frequencia.repousosTrabalhados), classe: estilos.resumoInfo },
      ]} />
      <div className={estilos.matrizMetricas}>
        <div><span>Ativos cadastrados</span><strong>{numero(frequencia.empregadosAtivos)}</strong></div>
        <div><span>Esperados na escala</span><strong>{numero(frequencia.empregadosEsperados)}</strong></div>
        <div><span>Registros lançados</span><strong>{numero((frequencia.registrosLancados ?? frequencia.registrosEsperados))}</strong></div>
        <div><span>Pendentes</span><strong className={frequencia.pendentes ? estilos.valorAlerta : ""}>{numero(frequencia.pendentes)}</strong></div>
        <div><span>Presenças</span><strong>{numero(frequencia.presencas)}</strong></div>
        <div><span>Ausências</span><strong>{numero(frequencia.ausencias)}</strong></div>
        <div><span>Atestados</span><strong>{numero(frequencia.ausenciasJustificadas)}</strong></div>
        <div><span>Faltas</span><strong className={frequencia.faltasNaoJustificadas ? estilos.valorAlerta : ""}>{numero(frequencia.faltasNaoJustificadas)}</strong></div>
        <div><span>RT</span><strong>{numero(frequencia.repousosTrabalhados)}</strong></div>
      </div>
      {!temEsperados && <p className={estilos.observacao}>Nenhum colaborador está marcado como esperado para a data escolhida. O painel não converte esse cenário em 100% de cobertura.</p>}
      {Number(frequencia.trabalhosForaEscalaSemRt || 0) > 0 && <p className={estilos.observacaoCritica}>{numero(frequencia.trabalhosForaEscalaSemRt)} trabalho(s) fora da escala sem classificação RT.</p>}
      <p className={estilos.observacao}>Este cartão é uma fotografia de {dataBr(frequencia.data)} e não representa a janela histórica selecionada. A escala considera jornada, folga remunerada aprovada e feriado/descanso aplicável. Jornada parcialmente configurada não presume dia de trabalho ausente.</p>
    </PainelExecutivo>
  );
};

export const IntegridadeDados = ({ integridade }: { integridade?: Partial<DTO.PainelGestaoDTOIntegridadeDados> }) => {
  const itens = [
    ["Ativos sem contrato vigente", integridade?.empregadosAtivosSemContrato],
    ["Empregados em múltiplos contratos vigentes", integridade?.empregadosComMultiplosContratosVigentes],
    ["Contrato em Gestão Local divergente", integridade?.empregadoContratoGestaoLocalDivergente],
    ["Lotação em Gestão Local divergente", integridade?.empregadoLotacaoGestaoLocalDivergente],
    ["Turno em Gestão Local divergente", integridade?.empregadoJornadaGestaoLocalDivergente],
    ["Jornadas sem configuração completa dos 7 dias", integridade?.jornadasComCalendarioIncompleto],
    ["Empregados ativos sem lotação", integridade?.empregadosAtivosSemLotacao],
    ["Empregados ativos sem jornada", integridade?.empregadosAtivosSemJornada],
    ["Adicionais RT sem frequência de origem", integridade?.adicionaisRtSemFrequencia],
    ["Empregados sem Gestão Local", integridade?.empregadosSemGestaoLocal],
    ["Contratos sem Gestão Local", integridade?.contratosSemGestaoLocal],
  ];
  const totalCriticos = itens.filter(([, valor]) => Number(valor || 0) > 0).length;
  return (
    <PainelExecutivo titulo="Confiabilidade e integridade" icone={ShieldCheck} ajuda={AJUDA.integridade} detalhe={integridade?.totalInconsistencias ? "Requer saneamento" : "Consistente"}>
      <FaixaResumo itens={[
        { rotulo: "Inconsistências", valor: numero(integridade?.totalInconsistencias), classe: Number(integridade?.totalInconsistencias || 0) > 0 ? estilos.resumoCritico : estilos.resumoSucesso },
        { rotulo: "Pontos críticos", valor: numero(totalCriticos), classe: totalCriticos > 0 ? estilos.resumoAtencao : estilos.resumoInfo },
      ]} />
      <ul className={estilos.listaIntegridade}>
        {itens.map(([rotulo, valor]) => <li key={rotulo}><span>{Number(valor || 0) > 0 ? <AlertTriangle size={16} /> : <BadgeCheck size={16} />}{rotulo}</span><strong>{numero(valor)}</strong></li>)}
      </ul>
    </PainelExecutivo>
  );
};

export const EvolucaoOperacional = ({ evolucao = [], periodoDias = 30 }: { evolucao?: DTO.PainelGestaoDTOSerieTemporal[]; periodoDias?: number }) => {
  const dados = evolucao.map((item) => ({ ...item, rotulo: new Intl.DateTimeFormat("pt-BR", { day: "2-digit", month: "2-digit" }).format(new Date(`${item.data}T12:00:00`)) }));
  const totais = dados.reduce((acc, item) => ({ presencas: acc.presencas + Number(item.presencas || 0), ausencias: acc.ausencias + Number(item.ausencias || 0), repousosTrabalhados: acc.repousosTrabalhados + Number(item.repousosTrabalhados || 0) }), { presencas: 0, ausencias: 0, repousosTrabalhados: 0 });
  return (
    <PainelExecutivo titulo={`Evolução operacional — ${periodoDias} dias`} icone={ClipboardCheck} detalhe="Série histórica" largo>
      <FaixaResumo itens={[
        { rotulo: "Presenças", valor: numero(totais.presencas), classe: estilos.resumoSucesso },
        { rotulo: "Ausências", valor: numero(totais.ausencias), classe: estilos.resumoCritico },
        { rotulo: "RT", valor: numero(totais.repousosTrabalhados), classe: estilos.resumoInfo },
      ]} />
      <div className={estilos.grafico}>
        <ResponsiveContainer width="100%" height="100%">
          <LineChart data={dados} margin={{ top: 10, right: 16, left: -14, bottom: 0 }}>
            <CartesianGrid strokeDasharray="3 3" vertical={false} stroke="#d7e4eb" />
            <XAxis dataKey="rotulo" minTickGap={24} tick={{ fill: "#566a77", fontSize: 11 }} />
            <YAxis allowDecimals={false} tick={{ fill: "#566a77", fontSize: 11 }} />
            <Tooltip content={<TooltipPadrao />} />
            <Legend wrapperStyle={{ paddingTop: 12 }} />
            <Line type="monotone" dataKey="presencas" name="Presenças" stroke={CORES.positivo} strokeWidth={3} dot={false} activeDot={{ r: 5 }} />
            <Line type="monotone" dataKey="ausencias" name="Ausências" stroke={CORES.critico} strokeWidth={3} dot={false} activeDot={{ r: 5 }} />
            <Line type="monotone" dataKey="repousosTrabalhados" name="RT" stroke={CORES.informativo} strokeWidth={3} dot={false} activeDot={{ r: 5 }} />
          </LineChart>
        </ResponsiveContainer>
      </div>
    </PainelExecutivo>
  );
};

export const AlertasDecisao = ({ alertas = [], aoNavegar }: { alertas?: DTO.PainelGestaoDTOAlertaGestao[]; aoNavegar: (rota: string) => void }) => (
  <PainelExecutivo titulo="Prioridades de gestão — período e referência" icone={AlertTriangle} detalhe={`${numero(alertas.length)} alerta(s)`}>
    <div className={estilos.alertas}>
      {alertas.map((alerta) => <button key={`${alerta.codigo}-${alerta.titulo}`} type="button" className={classeSeveridade(alerta.severidade)} onClick={() => { if (alerta.rota) aoNavegar(alerta.rota); }}><span>{alerta.severidade}</span><strong>{alerta.titulo}</strong><p>{alerta.mensagem}</p></button>)}
      {!alertas.length && <p className={estilos.vazioAlertas}>Nenhuma prioridade gerencial identificada.</p>}
    </div>
  </PainelExecutivo>
);

export const MatrizUnidades = ({ unidades = [], nivel }: { unidades?: DTO.PainelGestaoDTOUnidadeGestao[]; nivel?: string }) => {
  const titulo = nivel === "NACIONAL" ? "Gestores estaduais na referência" : nivel === "SUPERINTENDENCIA" ? "Gestões Locais na referência" : "Lotações da Gestão Local na referência";
  return (
    <PainelExecutivo titulo={titulo} icone={Users} detalhe={`${numero(unidades.length)} unidade(s)`} largo>
      <FaixaResumo itens={[
        { rotulo: "Unidades", valor: numero(unidades.length), classe: estilos.resumoInfo },
        { rotulo: "Com pendências", valor: numero(unidades.filter((item) => Number(item.pendentesFrequencia || 0) > 0).length), classe: estilos.resumoAtencao },
        { rotulo: "Com alertas", valor: numero(unidades.filter((item) => Number(item.alertasContratuais || 0) > 0).length), classe: estilos.resumoCritico },
      ]} />
      <div className={estilos.tabelaRolagem}>
        <table>
          <thead><tr><th>Unidade</th><th>Efetivo</th><th>Previsto</th><th>Saldo</th><th>Ocupação</th><th>Contratos</th><th>Alertas</th><th>Esperados</th><th>Pendentes</th><th>Cobertura da escala</th></tr></thead>
          <tbody>
            {unidades.map((unidade) => (
              <tr key={`${unidade.tipo}-${unidade.id}`}>
                <td><strong>{unidade.nome}</strong><small>{unidade.uf || unidade.tipo}</small></td>
                <td>{numero(unidade.efetivoAtual)}</td><td>{unidade.efetivoPrevisto == null ? "—" : numero(unidade.efetivoPrevisto)}</td>
                <td className={classeSaldo(unidade.saldo)}>{unidade.saldo == null ? "—" : numero(unidade.saldo)}</td>
                <td>{unidade.percentualOcupacao == null ? "—" : percentual(unidade.percentualOcupacao)}</td><td>{numero(unidade.contratosVigentes)}</td>
                <td className={unidade.alertasContratuais ? estilos.valorAlerta : ""}>{numero(unidade.alertasContratuais)}</td><td>{numero(unidade.empregadosEsperados)}</td>
                <td className={unidade.pendentesFrequencia ? estilos.valorAlerta : ""}>{numero(unidade.pendentesFrequencia)}</td>
                <td>{Number(unidade.empregadosEsperados || 0) > 0 ? percentual(unidade.percentualCoberturaEscala) : "Sem escala esperada"}</td>
              </tr>
            ))}
            {!unidades.length && <tr><td colSpan={10} className={estilos.vazio}>Nenhuma unidade disponível no escopo autenticado.</td></tr>}
          </tbody>
        </table>
      </div>
    </PainelExecutivo>
  );
};

export const NotasMetodologicas = ({ notas = [], geradoEm }: { notas?: string[]; geradoEm?: string }) => (
  <footer className={estilos.notas}>
    <div><ShieldCheck size={18} /><strong>Critérios de confiabilidade</strong></div>
    <ul>{notas.map((nota) => <li key={nota}>{nota}</li>)}</ul>
    <small>Dados gerados pelo servidor em {geradoEm ? new Date(geradoEm).toLocaleString("pt-BR") : "—"}.</small>
  </footer>
);