import type * as DTO from '../../../../contracts/java';
import type { CoverageView, ManagementPanelView, IndicatorHelp, SummaryItem, ChartTooltipProps } from '../../../../contracts/painel';
import React, { useMemo, useState } from "react";
import {
  Activity,
  AlertOctagon,
  ArrowDownRight,
  ArrowUpRight,
  BarChart3,
  Bell,
  Building2,
  CalendarCheck,
  CalendarRange,
  CheckCircle2,
  ChevronRight,
  Clock3,
  FileBarChart,
  Gauge,
  ListChecks,
  Minus,
  ScrollText,
  ShieldCheck,
  UserCog,
  Users,
  UsersRound,
  XCircle,
  Zap,
} from "lucide-react";
import {
  Bar,
  BarChart,
  CartesianGrid,
  Cell,
  Pie,
  PieChart,
  ResponsiveContainer,
  Tooltip,
  XAxis,
  YAxis,
} from "recharts";
import estilos from "./index.module.css";
import { BotaoAjudaPainel, BotaoExpandirPainel, ModalAjudaIndicador, ModalPainel } from "../ModalPainel/index.tsx";

const numero = (valor: (number | undefined) | (number) | (import("recharts").TooltipValueType | undefined)) => new Intl.NumberFormat("pt-BR").format(Number(valor || 0));
const percentual = (valor: (number | undefined) | (number) | (import("recharts").TooltipValueType | undefined)) => `${Number(valor || 0).toLocaleString("pt-BR", { maximumFractionDigits: 1 })}%`;
const dataHora = (valor: (`${number}-${number}-${number}T${string}` | null | undefined)) => valor ? new Date(valor).toLocaleString("pt-BR") : "Sem atualização no período";
const mesAno = (valor: (`${number}-${number}-${number}` | null)) => valor
  ? new Intl.DateTimeFormat("pt-BR", { month: "short", year: "2-digit" })
      .format(new Date(`${valor}T12:00:00`)).replace(".", "")
  : "—";
const dataBr = (valor: (string) | (`${number}-${number}-${number}` | null | undefined)) => valor
  ? new Intl.DateTimeFormat("pt-BR").format(new Date(`${valor}T12:00:00`))
  : "—";
const intervaloPeriodo = (periodo: Partial<DTO.PainelGestaoDTOAnalisePeriodo> = {}) => (
  periodo?.inicio && periodo?.fim
    ? `${dataBr(periodo.inicio)} a ${dataBr(periodo.fim)}`
    : `${numero(periodo?.dias)} dias`
);

const AJUDA = {
  quadro: {
    finalidade: "Mostra a composição cadastral real do quadro até a data de referência.",
    calculo: "Conta colaboradores cuja data de admissão é nula ou não é posterior à referência e separa o status atual entre ativos e demais situações.",
    fonte: "empregado, status_empregado, lotacao, cargo e jornada.",
    interpretacao: "Este bloco independe da frequência. Se há colaboradores cadastrados, eles devem aparecer aqui mesmo que ainda não existam lançamentos de presença ou falta.",
  },
  cadastro: {
    finalidade: "Evidencia movimentação de admissões da base para que meses como fevereiro e março apareçam mesmo sem frequência lançada.",
    calculo: "Conta data_admissao no período selecionado, no mês da referência e nos últimos 12 meses; o gráfico agrupa admissões por mês.",
    fonte: "Campo empregado.data_admissao, sempre filtrado pelo escopo organizacional do usuário.",
    interpretacao: "Zero em um mês significa que não há admissão cadastrada naquele mês. Colaboradores sem data de admissão ficam separados e não são distribuídos artificialmente no gráfico.",
  },
  absenteismo: {
    finalidade: "Mostra ausências operacionais efetivamente registradas no período selecionado.",
    calculo: "Falta + atestado + suspensão. A taxa divide essas ausências pelos registros operacionais de frequência do período.",
    fonte: "Tabela frequencia e seus snapshots organizacionais históricos.",
    interpretacao: "Sem lançamentos operacionais, o painel mostra 'Sem lançamentos' em vez de tratar a ausência de dados como taxa real de 0%.",
  },
  funil: {
    finalidade: "Compara quem deveria trabalhar na referência com quem possui frequência lançada e sua classificação operacional.",
    calculo: "A escala esperada considera jornada, exceções aprovadas e calendário; lançados são frequências existentes para esse conjunto.",
    fonte: "empregado, jornada_dia_semana, frequencia, frequencia_excecao e calendario_evento.",
    interpretacao: "Se ninguém é esperado na data, a cobertura é não aplicável; se há esperados e nenhum lançamento, a pendência é real.",
  },
};

const ROTULOS_STATUS: Readonly<Record<string, string | undefined>> = {
  PRESENCA: "Presença",
  PRESENCA_PARCIAL: "Presença parcial",
  ATRASO: "Atraso",
  FALTA: "Falta",
  ATESTADO: "Atestado",
  SUSPENSAO: "Suspensão",
  DESCANSO: "Descanso",
  DSR_PERDIDO: "DSR perdido",
  FOLGA: "Folga",
  RT: "RT",
  TR: "TR",
  SEM_STATUS: "Sem status",
};

const ROTULOS_SITUACAO: Readonly<Record<string, string | undefined>> = {
  ATIVO: "Ativo",
  INATIVO: "Inativo",
  FERIAS: "Férias",
  AFASTADO_INSS: "Afastado INSS",
  SEM_STATUS: "Sem status",
};

const CORES = {
  azul: "#0f8bd6",
  azulEscuro: "#0a5b85",
  verde: "#138a63",
  verdeEscuro: "#0b6b4b",
  amarelo: "#d59a00",
  laranja: "#d87710",
  vermelho: "#c43c30",
  roxo: "#7c59b8",
  cinza: "#7a8a93",
  turquesa: "#0f9aa8",
};

const corCobertura = (valor: (number)) => {
  const n = Number(valor || 0);
  if (n >= 95) return CORES.verde;
  if (n >= 85) return CORES.azul;
  if (n >= 70) return CORES.amarelo;
  return CORES.vermelho;
};

const corOcupacao = (valor: (number | null)) => {
  if (valor == null) return CORES.cinza;
  const n = Number(valor);
  if (n > 110) return CORES.vermelho;
  if (n < 80) return CORES.amarelo;
  return CORES.verde;
};

const corAbsenteismo = (valor: (number)) => {
  const n = Number(valor || 0);
  if (n <= 2) return CORES.verde;
  if (n <= 5) return CORES.azul;
  if (n <= 8) return CORES.amarelo;
  return CORES.vermelho;
};

const corVolume = (valor: (number), maximo: (number)) => {
  const n = Number(valor || 0);
  if (n <= 0) return "#d7e4eb";
  const razao = maximo > 0 ? n / maximo : 0;
  if (razao < 0.5) return "#53a9d5";
  if (razao < 0.8) return CORES.azul;
  return CORES.azulEscuro;
};

const COR_STATUS: Readonly<Record<string, string | undefined>> = {
  PRESENCA: CORES.verde,
  PRESENCA_PARCIAL: CORES.amarelo,
  ATRASO: CORES.laranja,
  FALTA: CORES.vermelho,
  ATESTADO: CORES.azul,
  SUSPENSAO: CORES.roxo,
  DESCANSO: CORES.turquesa,
  DSR_PERDIDO: "#934838",
  FOLGA: CORES.azulEscuro,
  RT: CORES.turquesa,
  TR: CORES.amarelo,
  SEM_STATUS: CORES.cinza,
};

const COR_SITUACAO: Readonly<Record<string, string | undefined>> = {
  ATIVO: CORES.verde,
  INATIVO: CORES.cinza,
  FERIAS: CORES.azul,
  AFASTADO_INSS: CORES.roxo,
  SEM_STATUS: CORES.azulEscuro,
};

const TooltipPadrao = ({ active, label, payload, formatadorValor = numero }: ChartTooltipProps) => {
  if (!active || !payload?.length) return null;
  return (
    <div className={estilos.tooltipGrafico}>
      {label ? <strong>{label}</strong> : null}
      <ul>
        {payload.map((item) => (
          <li key={`${item.dataKey}-${item.name}`}>
            <span style={{ background: item.color || item.fill || CORES.azulEscuro }} />
            <b>{item.name}:</b> {formatadorValor(item.value)}
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

const Cartao = ({ titulo, icone: Icone, detalhe, ajuda, children, largo = false, expansivel = true }: React.PropsWithChildren<{ titulo: string; icone: import("lucide-react").LucideIcon; detalhe?: React.ReactNode; ajuda?: IndicatorHelp; largo?: boolean; expansivel?: boolean }>) => {
  const [modalAberto, setModalAberto] = useState(false);
  const [ajudaAberta, setAjudaAberta] = useState(false);
  return (
    <>
      <article className={`${estilos.cartao} ${largo ? estilos.largo : ""}`}>
        <header className={estilos.cabecalhoCartao}>
          <div><Icone size={19} aria-hidden="true" /><h2>{titulo}</h2></div>
          <div className={estilos.acoesCabecalhoCartao}>
            {detalhe && <span>{detalhe}</span>}
            {ajuda ? <BotaoAjudaPainel titulo={titulo} aoAbrir={() => setAjudaAberta(true)} /> : null}
            {expansivel && <BotaoExpandirPainel titulo={titulo} aoAbrir={() => setModalAberto(true)} />}
          </div>
        </header>
        <div className={`${estilos.previewCartao} ${largo ? estilos.previewCartaoLargo : ""} ${!expansivel ? estilos.previewSemCorte : ""}`}>
          {children}
        </div>
        {expansivel && (
          <button type="button" className={estilos.abrirAnalise} onClick={() => setModalAberto(true)}>
            Ver análise completa <ChevronRight size={15} aria-hidden="true" />
          </button>
        )}
      </article>
      <ModalPainel aberto={modalAberto} titulo={titulo} detalhe={detalhe} aoFechar={() => setModalAberto(false)}>
        <div className={estilos.conteudoModal}>{children}</div>
      </ModalPainel>
      <ModalAjudaIndicador aberto={ajudaAberta} titulo={titulo} ajuda={ajuda} aoFechar={() => setAjudaAberta(false)} />
    </>
  );
};

export const AtalhosGerenciais = ({ perfil, aoNavegar }: { perfil: Record<string, boolean>; aoNavegar: (rota: string) => void }) => {
  const atalhos = [
    perfil.podeOperarFrequencia && { rota: "/frequencia", rotulo: "Frequência", icone: CalendarCheck },
    perfil.podeOperarOcorrencias && { rota: "/ocorrencias", rotulo: "Ocorrências", icone: Bell },
    perfil.podeVisualizarEmpregados && { rota: "/pessoas", rotulo: "Pessoas e vínculos", icone: Users },
    perfil.podeVisualizarContratos && { rota: "/contratos", rotulo: "Contratos", icone: ScrollText },
    perfil.podeAcessarVisaoConsolidadaRelatorios && { rota: "/relatorios", rotulo: "Visão consolidada", icone: FileBarChart },
    perfil.podeAcessarRelatorios && { rota: "/relatorio-mensal", rotulo: "Frequência mensal", icone: FileBarChart },
    perfil.podeAcessarRelatorios && { rota: "/relatorio-absenteismo", rotulo: "Absenteísmo", icone: BarChart3 },
    perfil.podeAcessarRelatoriosAdministrativos && { rota: "/relatorios-administrativos", rotulo: "Emissões administrativas", icone: FileBarChart },
    perfil.podeGerenciarCalendario && { rota: "/calendario-folgas", rotulo: "Calendário", icone: CalendarRange },
    perfil.podeGerenciarUsuarios && { rota: "/gestao-usuarios", rotulo: perfil.presidente ? "Usuários de gestores estaduais" : "Usuários", icone: UserCog },
    perfil.podeGerenciarEstrutura && { rota: "/estrutura-organizacional", rotulo: "Gestores estaduais", icone: Building2 },
    perfil.podeAcessarAuditoria && { rota: "/logs", rotulo: "Auditoria", icone: ShieldCheck },
  ].filter((item): item is Exclude<typeof item, false | null | undefined> => Boolean(item));

  return (
    <Cartao titulo="Central de ações gerenciais" icone={Zap} detalhe={`${atalhos.length} ferramenta(s)`} largo expansivel={false}>
      <div className={estilos.atalhos}>
        {atalhos.map(({ rota, rotulo, icone: Icone }) => (
          <button key={rota} type="button" onClick={() => aoNavegar(rota)}>
            <Icone size={20} aria-hidden="true" />
            <span>{rotulo}</span>
          </button>
        ))}
      </div>
    </Cartao>
  );
};

export const QuadroPessoal = ({ quadro = {} }: { quadro?: Partial<DTO.PainelGestaoDTOQuadroPessoal> }) => {
  const dados = [
    { name: "Ativos", value: Number(quadro.ativos || 0), cor: CORES.verde },
    { name: "Inativos/afastados", value: Number(quadro.inativosOuAfastados || 0), cor: CORES.amarelo },
  ];
  return (
    <Cartao titulo="Composição do quadro" icone={UsersRound} ajuda={AJUDA.quadro} detalhe={`${percentual(quadro.percentualAtivos)} ativos`}>
      <FaixaResumo
        itens={[
          { rotulo: "Cadastrados", valor: numero(quadro.empregadosCadastrados), classe: estilos.resumoInfo },
          { rotulo: "Ativos", valor: numero(quadro.ativos), classe: estilos.resumoSucesso },
          { rotulo: "Afastados", valor: numero(quadro.inativosOuAfastados), classe: estilos.resumoAtencao },
        ]}
      />
      <div className={estilos.quadroConteudo}>
        <div className={estilos.graficoRosca}>
          <ResponsiveContainer width="100%" height="100%">
            <PieChart>
              <Pie data={dados} dataKey="value" nameKey="name" innerRadius={48} outerRadius={72} paddingAngle={2}>
                {dados.map((item) => <Cell key={item.name} fill={item.cor} />)}
              </Pie>
              <Tooltip content={<TooltipPadrao formatadorValor={numero} />} />
            </PieChart>
          </ResponsiveContainer>
          <div><strong>{numero(quadro.empregadosCadastrados)}</strong><span>cadastrados</span></div>
        </div>
        <dl className={estilos.listaResumo}>
          <div><dt>Ativos</dt><dd>{numero(quadro.ativos)}</dd></div>
          <div><dt>Inativos/afastados</dt><dd>{numero(quadro.inativosOuAfastados)}</dd></div>
          <div><dt>Lotações</dt><dd>{numero(quadro.lotacoes)}</dd></div>
          <div><dt>Cargos</dt><dd>{numero(quadro.cargos)}</dd></div>
          <div><dt>Jornadas</dt><dd>{numero(quadro.jornadas)}</dd></div>
        </dl>
      </div>
    </Cartao>
  );
};

export const AnaliseCadastral = ({ analise = {}, admissoesMensais = [], periodoDias = 30 }: { analise?: Partial<DTO.PainelGestaoDTOAnaliseCadastro>; admissoesMensais?: DTO.PainelGestaoDTOAdmissaoMensal[]; periodoDias?: number }) => {
  const serieBase = Array.isArray(admissoesMensais) ? admissoesMensais.map((item) => ({
    ...item,
    rotulo: mesAno(item?.mes),
    quantidade: Number(item?.quantidade || 0),
  })) : [];
  const maxAdmissoes = Math.max(0, ...serieBase.map((item) => item.quantidade));
  const serie = serieBase.map((item) => ({ ...item, cor: corVolume(item.quantidade, maxAdmissoes) }));
  return (
    <Cartao titulo="Movimentação cadastral" icone={Activity} ajuda={AJUDA.cadastro} detalhe="Admissões reais da base" largo>
      <FaixaResumo
        itens={[
          { rotulo: `Admitidos em ${periodoDias} dias`, valor: numero(analise.admitidosNoPeriodo), classe: estilos.resumoSucesso },
          { rotulo: "No mês da referência", valor: numero(analise.admitidosNoMesReferencia), classe: estilos.resumoInfo },
          { rotulo: "Últimos 12 meses", valor: numero(analise.admitidosUltimos12Meses), classe: estilos.resumoInfo },
          { rotulo: "Sem data de admissão", valor: numero(analise.semDataAdmissao), classe: Number(analise.semDataAdmissao || 0) ? estilos.resumoAtencao : estilos.resumoSucesso },
        ]}
      />
      <div className={estilos.graficoMedio}>
        <ResponsiveContainer width="100%" height="100%">
          <BarChart data={serie} margin={{ top: 8, right: 8, left: -14, bottom: 2 }}>
            <CartesianGrid strokeDasharray="3 3" vertical={false} stroke="#d9e5eb" />
            <XAxis dataKey="rotulo" />
            <YAxis allowDecimals={false} />
            <Tooltip content={<TooltipPadrao formatadorValor={numero} />} />
            <Bar dataKey="quantidade" name="Admissões" radius={[6, 6, 0, 0]}>
              {serie.map((item) => <Cell key={item.mes || item.rotulo} fill={item.cor} />)}
            </Bar>
          </BarChart>
        </ResponsiveContainer>
      </div>
      <p className={estilos.nota}>Qualidade da data de admissão: {percentual(analise.percentualComDataAdmissao)} dos cadastros possuem data informada. Meses sem admissão permanecem zerados porque a consulta não encontrou registros, não por estimativa.</p>
    </Cartao>
  );
};

export const LeituraGerencialPeriodo = ({
  periodo = {},
  comparativo = {},
}: { periodo?: Partial<DTO.PainelGestaoDTOAnalisePeriodo>; comparativo?: Partial<DTO.PainelGestaoDTOComparativoPeriodo> }) => {
  const registros = Number(periodo.registrosOperacionais || 0);
  const presencasGerenciais = Number(periodo.presencas || 0)
    + Number(periodo.presencasParciais || 0)
    + Number(periodo.atrasos || 0);
  const ausencias = Number(periodo.faltasNaoJustificadas || 0)
    + Number(periodo.atestados || 0)
    + Number(periodo.suspensoes || 0);
  const temBaseAtual = registros > 0;
  const temBaseAnterior = Number(comparativo.registrosOperacionaisAnterior || 0) > 0;

  return (
    <Cartao
      titulo="Desempenho operacional do período"
      icone={FileBarChart}
      detalhe={`${intervaloPeriodo(periodo)} · ${numero(periodo.dias)} dias`}
      largo
      expansivel={false}
    >
      <FaixaResumo
        itens={[
          { rotulo: "Lançamentos operacionais", valor: numero(registros), classe: estilos.resumoInfo },
          { rotulo: "Presenças", valor: numero(presencasGerenciais), classe: estilos.resumoSucesso },
          { rotulo: "Ausências", valor: numero(ausencias), classe: ausencias > 0 ? estilos.resumoCritico : estilos.resumoInfo },
          { rotulo: "Absenteísmo", valor: temBaseAtual ? percentual(periodo.taxaAbsenteismo) : "Sem lançamentos", classe: estilos.resumoAtencao },
        ]}
      />

      <div className={estilos.kpisQuatro}>
        <div><span>Taxa de presença</span><strong>{temBaseAtual ? percentual(periodo.taxaPresenca) : "—"}</strong></div>
        <div><span>Empregados com ausência</span><strong>{numero(periodo.empregadosComAusencia)}</strong></div>
        <div><span>Recorrentes (3+ ausências)</span><strong>{numero(periodo.empregadosComAusenciaRecorrente)}</strong></div>
        <div><span>Período analisado</span><strong>{numero(periodo.dias)} dias</strong></div>
      </div>

      <div className={estilos.composicaoAusencias}>
        <span><i className={estilos.marcadorFalta} />Faltas: <strong>{numero(periodo.faltasNaoJustificadas)}</strong></span>
        <span><i className={estilos.marcadorAtestado} />Atestados: <strong>{numero(periodo.atestados)}</strong></span>
        <span><i className={estilos.marcadorSuspensao} />Suspensões: <strong>{numero(periodo.suspensoes)}</strong></span>
      </div>

      <p className={estilos.nota}>
        Os números acima usam todos os lançamentos operacionais entre {intervaloPeriodo(periodo)}.
        A data de referência é o último dia da janela, não o único dia analisado.
      </p>

      {!temBaseAtual ? (
        <p className={estilos.nota}>
          Não há lançamentos operacionais nesta janela. Essa ausência de registros não é apresentada como presença de 100% nem como absenteísmo real de 0%.
        </p>
      ) : !temBaseAnterior ? (
        <p className={estilos.nota}>
          Não existem lançamentos operacionais na janela anterior de igual duração; a comparação histórica ficará indisponível até existir base anterior.
        </p>
      ) : null}
    </Cartao>
  );
};

export const ImpactoAbsenteismo = ({ periodo = {} }: { periodo?: Partial<DTO.PainelGestaoDTOAnalisePeriodo> }) => {
  const ausencias = Number(periodo.faltasNaoJustificadas || 0)
    + Number(periodo.atestados || 0)
    + Number(periodo.suspensoes || 0);

  return (
    <Cartao
      titulo="Composição das ausências"
      icone={AlertOctagon}
      ajuda={AJUDA.absenteismo}
      detalhe={`${intervaloPeriodo(periodo)} · ${numero(ausencias)} ocorrência(s)`}
    >
      <FaixaResumo
        itens={[
          { rotulo: "Faltas", valor: numero(periodo.faltasNaoJustificadas), classe: estilos.resumoCritico },
          { rotulo: "Atestados", valor: numero(periodo.atestados), classe: estilos.resumoAtencao },
          { rotulo: "Suspensões", valor: numero(periodo.suspensoes), classe: estilos.resumoInfo },
        ]}
      />

      <div className={estilos.kpisQuatro}>
        <div><span>Total de ausências</span><strong>{numero(ausencias)}</strong></div>
        <div><span>Empregados afetados</span><strong>{numero(periodo.empregadosComAusencia)}</strong></div>
        <div><span>Recorrentes (3+)</span><strong className={Number(periodo.empregadosComAusenciaRecorrente || 0) ? estilos.alerta : ""}>{numero(periodo.empregadosComAusenciaRecorrente)}</strong></div>
        <div><span>Absenteísmo do período</span><strong>{Number(periodo.registrosOperacionais || 0) > 0 ? percentual(periodo.taxaAbsenteismo) : "—"}</strong></div>
      </div>

      <p className={estilos.nota}>
        Um mesmo empregado pode aparecer mais de uma vez nas ausências. “Empregados afetados” conta pessoas distintas; “recorrentes” exige 3 ou mais ausências na janela selecionada.
      </p>
      <small className={estilos.rodapeDado}>Última atualização da frequência: {dataHora(periodo.ultimaAtualizacao)}</small>
    </Cartao>
  );
};

export const PressaoOperacional = ({ pressao = {} }: { pressao?: Partial<DTO.PainelGestaoDTOPressaoOperacional> }) => (
  <Cartao titulo="Repouso trabalhado (RT)" icone={Gauge} detalhe="Indicador observacional do período">
    <FaixaResumo
      itens={[
        { rotulo: "Registros RT", valor: numero(pressao.repousosTrabalhados), classe: estilos.resumoInfo },
        { rotulo: "Empregados com RT", valor: numero(pressao.empregadosComRt), classe: estilos.resumoSucesso },
        { rotulo: "RT por 100 ativos", valor: percentual(pressao.rtPorCemAtivos), classe: estilos.resumoInfo },
      ]}
    />
    <div className={estilos.medidorRt}>
      <div className={estilos.medidorFaixa}>
        <span style={{ width: `${Math.min(100, Number(pressao.rtPorCemAtivos || 0))}%` }} />
      </div>
      <strong>{percentual(pressao.rtPorCemAtivos)}</strong>
      <span>registros RT para cada 100 empregados ativos no período</span>
    </div>
    <div className={estilos.kpisTres}>
      <div><span>Registros RT</span><strong>{numero(pressao.repousosTrabalhados)}</strong></div>
      <div><span>Pessoas com RT</span><strong>{numero(pressao.empregadosComRt)}</strong></div>
      <div><span>Base</span><strong>Período selecionado</strong></div>
    </div>
    <p className={estilos.nota}>
      RT significa trabalho em repouso. O painel não usa a relação entre RT e ausências como prova de substituição de empregado ausente.
    </p>
  </Cartao>
);

export const HorizonteContratual = ({ horizonte = {} }: { horizonte?: Partial<DTO.PainelGestaoDTOHorizonteContratual> }) => {
  const dados = [
    { faixa: "Vencidos", total: Number(horizonte.vencidos || 0), cor: CORES.vermelho },
    { faixa: "Até 30d", total: Number(horizonte.ate30Dias || 0), cor: CORES.laranja },
    { faixa: "31–90d", total: Number(horizonte.de31A90Dias || 0), cor: CORES.amarelo },
    { faixa: "91–180d", total: Number(horizonte.de91A180Dias || 0), cor: CORES.azul },
    { faixa: "> 180d", total: Number(horizonte.acimaDe180Dias || 0), cor: CORES.verde },
    { faixa: "Sem fim", total: Number(horizonte.semDataFim || 0), cor: CORES.roxo },
  ];
  return (
    <Cartao titulo="Horizonte de vencimento contratual" icone={Clock3} detalhe="Contratos ativos">
      <FaixaResumo
        itens={[
          { rotulo: "Vencidos", valor: numero(horizonte.vencidos), classe: estilos.resumoCritico },
          { rotulo: "Até 30 dias", valor: numero(horizonte.ate30Dias), classe: estilos.resumoAtencao },
          { rotulo: "Sem data fim", valor: numero(horizonte.semDataFim), classe: estilos.resumoInfo },
        ]}
      />
      <div className={estilos.graficoMedio}>
        <ResponsiveContainer width="100%" height="100%">
          <BarChart data={dados} margin={{ top: 8, right: 8, left: -18, bottom: 2 }}>
            <CartesianGrid strokeDasharray="3 3" vertical={false} stroke="#d9e5eb" />
            <XAxis dataKey="faixa" tick={{ fontSize: 11 }} />
            <YAxis allowDecimals={false} />
            <Tooltip content={<TooltipPadrao formatadorValor={numero} />} />
            <Bar dataKey="total" name="Contratos" radius={[6, 6, 0, 0]}>
              {dados.map((item) => <Cell key={item.faixa} fill={item.cor} />)}
            </Bar>
          </BarChart>
        </ResponsiveContainer>
      </div>
    </Cartao>
  );
};

export const DistribuicaoStatus = ({ dados = [], periodo = {} }: { dados?: DTO.PainelGestaoDTODistribuicaoStatus[]; periodo?: Partial<DTO.PainelGestaoDTOAnalisePeriodo> }) => {
  const listaStatus = Array.isArray(dados) ? dados : [];
  const principais = listaStatus.slice(0, 10).map((item, indice) => {
    const status = String(item?.status || "").trim();
    return {
      ...item,
      status,
      rotulo: ROTULOS_STATUS[String(status)] || status || `Status ${indice + 1}`,
      cor: COR_STATUS[status] || CORES.azulEscuro,
    };
  });

  const statusOperacionais = new Set([
    "PRESENCA",
    "PRESENCA_PARCIAL",
    "ATRASO",
    "TR",
    "FALTA",
    "ATESTADO",
    "SUSPENSAO",
  ]);

  const totalGeral = listaStatus.reduce((soma, item) => soma + Number(item?.quantidade || 0), 0);
  const totalOperacionalDistribuicao = listaStatus
    .filter((item) => statusOperacionais.has(item.status || ""))
    .reduce((soma, item) => soma + Number(item.quantidade || 0), 0);
  const baseOperacional = Number(periodo.registrosOperacionais ?? totalOperacionalDistribuicao);
  const outrosRegistros = Math.max(0, totalGeral - totalOperacionalDistribuicao);

  return (
    <Cartao titulo="Todos os lançamentos do período" icone={ListChecks} detalhe={`${numero(totalGeral)} registros no total`}>
      <FaixaResumo
        itens={[
          { rotulo: "Base das taxas", valor: numero(baseOperacional), classe: estilos.resumoSucesso },
          { rotulo: "Outros status", valor: numero(outrosRegistros), classe: estilos.resumoInfo },
          { rotulo: "Total armazenado", valor: numero(totalGeral), classe: estilos.resumoInfo },
        ]}
      />

      <p className={estilos.nota}>
        “Base das taxas” considera somente presença, presença parcial, atraso, TR, falta, atestado e suspensão.
        O total armazenado também pode incluir RT, folga, descanso, DSR perdido e outros status; por isso ele pode ser maior sem representar divergência.
      </p>

      <div className={estilos.graficoAlto}>
        {principais.length ? (
          <ResponsiveContainer width="100%" height="100%">
            <BarChart data={principais} layout="vertical" margin={{ top: 4, right: 28, left: 18, bottom: 2 }}>
              <CartesianGrid strokeDasharray="3 3" horizontal={false} stroke="#d9e5eb" />
              <XAxis type="number" allowDecimals={false} />
              <YAxis type="category" dataKey="rotulo" width={112} tick={{ fontSize: 11 }} />
              <Tooltip formatter={(valor, nome, item) => [`${numero(valor)} (${percentual(item.payload.percentual)})`, "Registros"]} />
              <Bar dataKey="quantidade" name="Registros" radius={[0, 6, 6, 0]}>
                {principais.map((item, indice) => <Cell key={`${item.status || item.rotulo || "status"}-${indice}`} fill={item.cor} />)}
              </Bar>
            </BarChart>
          </ResponsiveContainer>
        ) : <div className={estilos.vazio}>Sem lançamentos no período.</div>}
      </div>
    </Cartao>
  );
};

export const PadraoSemanal = ({ dados = [] }: { dados?: DTO.PainelGestaoDTOPadraoSemanal[] }) => {
  const semana = Array.from({ length: 7 }, (_, ordem) => {
    const encontrado = dados.find((item) => Number(item.ordem) === ordem);
    return encontrado || {
      ordem,
      dia: ["Segunda", "Terça", "Quarta", "Quinta", "Sexta", "Sábado", "Domingo"][ordem],
      taxaAbsenteismo: 0,
      ausencias: 0,
      registrosOperacionais: 0,
    };
  }).map((item) => ({ ...item, cor: corAbsenteismo(item.taxaAbsenteismo) }));

  const diasComBase = semana.filter((item) => Number(item.registrosOperacionais || 0) > 0);
  const pico = diasComBase.reduce<(typeof diasComBase)[number] | null>(
    (atual, item) => !atual || Number(item.taxaAbsenteismo || 0) > Number(atual.taxaAbsenteismo || 0) ? item : atual,
    null,
  );
  const totalAusencias = semana.reduce((soma, item) => soma + Number(item.ausencias || 0), 0);

  return (
    <Cartao titulo="Padrão das ausências por dia da semana" icone={Activity} detalhe="Janela histórica selecionada">
      <FaixaResumo
        itens={[
          {
            rotulo: "Maior taxa",
            valor: pico ? `${pico.dia?.slice(0, 3)} · ${percentual(pico.taxaAbsenteismo)}` : "Sem base",
            classe: estilos.resumoAtencao,
          },
          { rotulo: "Ausências no período", valor: numero(totalAusencias), classe: estilos.resumoCritico },
        ]}
      />
      <div className={estilos.graficoMedio}>
        <ResponsiveContainer width="100%" height="100%">
          <BarChart data={semana} margin={{ top: 8, right: 8, left: -18, bottom: 2 }}>
            <CartesianGrid strokeDasharray="3 3" vertical={false} stroke="#d9e5eb" />
            <XAxis dataKey="dia" tickFormatter={(valor: string) => valor.slice(0, 3)} />
            <YAxis unit="%" />
            <Tooltip formatter={(valor, nome, item) => [
              `${percentual(valor)} · ${numero(item.payload.ausencias)} ausência(s) · ${numero(item.payload.registrosOperacionais)} lançamento(s) operacionais`,
              "Absenteísmo",
            ]} />
            <Bar dataKey="taxaAbsenteismo" name="Absenteísmo" radius={[6, 6, 0, 0]}>
              {semana.map((item) => <Cell key={item.dia} fill={item.cor} />)}
            </Bar>
          </BarChart>
        </ResponsiveContainer>
      </div>
      <p className={estilos.nota}>
        Cada barra reúne todos os dias equivalentes dentro da janela. Ex.: “Qua” representa todas as quartas-feiras entre o início e o fim do período, não apenas a quarta da referência.
      </p>
    </Cartao>
  );
};

export const FunilFrequencia = ({ frequencia = {} }: { frequencia?: CoverageView }) => {
  const esperados = Number(frequencia.empregadosEsperados || 0);
  const etapas = [
    { rotulo: "Empregados esperados", valor: esperados, detalhe: "Jornada e calendário aplicáveis" },
    { rotulo: "Registros lançados", valor: Number((frequencia.registrosLancados ?? frequencia.registrosEsperados) || 0), detalhe: esperados > 0 ? `${percentual(frequencia.percentualCobertura)} de cobertura` : "Cobertura não aplicável" },
    { rotulo: "Presenças operacionais", valor: Number(frequencia.presencas || 0), detalhe: "Presença, parcial, atraso e TR" },
    { rotulo: "Ausências operacionais", valor: Number(frequencia.ausencias || 0), detalhe: "Falta, atestado e suspensão" },
    { rotulo: "Pendências de lançamento", valor: Number(frequencia.pendentes || 0), detalhe: "Esperados ainda sem frequência" },
  ];
  const maximo = Math.max(1, esperados, ...etapas.map((item) => item.valor));
  return (
    <Cartao titulo="Funil da frequência diária" icone={CalendarCheck} ajuda={AJUDA.funil} detalhe={frequencia.data || "Data de referência"}>
      <FaixaResumo
        itens={[
          { rotulo: "Esperados", valor: numero(esperados), classe: estilos.resumoInfo },
          { rotulo: "Lançados", valor: numero((frequencia.registrosLancados ?? frequencia.registrosEsperados)), classe: estilos.resumoSucesso },
          { rotulo: "Pendentes", valor: numero(frequencia.pendentes), classe: Number(frequencia.pendentes || 0) > 0 ? estilos.resumoAtencao : estilos.resumoInfo },
        ]}
      />
      <div className={estilos.funilFrequencia}>
        {etapas.map((item, indice) => (
          <div key={item.rotulo}>
            <div><span>{item.rotulo}</span><strong>{numero(item.valor)}</strong></div>
            <div className={estilos.faixaFunil}><span style={{ width: `${Math.max(2, item.valor * 100 / maximo)}%`, background: [CORES.azulEscuro, CORES.azul, CORES.verde, CORES.vermelho, CORES.amarelo][indice] }} /></div>
            <small>{item.detalhe}</small>
          </div>
        ))}
      </div>
      <p className={estilos.nota}>Presenças e ausências são subconjuntos operacionais; registros administrativos de descanso ou folga não são forçados nessas duas categorias.</p>
    </Cartao>
  );
};

export const ComparativoUnidades = ({ unidades = [], nivel }: { unidades?: DTO.PainelGestaoDTOUnidadeGestao[]; nivel?: string }) => {
  const dados = useMemo(() => unidades
    .filter((item) => Number(item.empregadosEsperados || 0) > 0 || item.percentualOcupacao != null)
    .sort((a, b) => Number(a.percentualCoberturaEscala || 0) - Number(b.percentualCoberturaEscala || 0)
      || Number(b.alertasContratuais || 0) - Number(a.alertasContratuais || 0))
    .slice(0, 12)
    .map((item) => ({
      nome: (item.nome?.length || 0) > 24 ? `${item.nome?.slice(0, 22)}…` : item.nome,
      cobertura: Number(item.percentualCoberturaEscala || 0),
      ocupacao: item.percentualOcupacao == null ? null : Number(item.percentualOcupacao),
      pendentes: Number(item.pendentesFrequencia || 0),
      corCobertura: corCobertura(item.percentualCoberturaEscala),
      corOcupacao: corOcupacao(item.percentualOcupacao),
    })), [unidades]);
  const rotulo = nivel === "NACIONAL" ? "gestor estadual" : nivel === "SUPERINTENDENCIA" ? "Gestão Local" : "lotação";
  return (
    <Cartao titulo="Comparativo de capacidade e cobertura na referência" icone={BarChart3} detalhe={`Até 12 ${rotulo}(s) prioritária(s)`} largo>
      <FaixaResumo
        itens={[
          { rotulo: "Unidades comparadas", valor: numero(dados.length), classe: estilos.resumoInfo },
          { rotulo: "Cobertura média", valor: dados.length ? percentual(dados.reduce((soma, item) => soma + Number(item.cobertura || 0), 0) / dados.length) : "0%", classe: estilos.resumoSucesso },
          { rotulo: "Pendências", valor: numero(dados.reduce((soma, item) => soma + Number(item.pendentes || 0), 0)), classe: estilos.resumoAtencao },
        ]}
      />
      {dados.length ? (
        <>
        <div className={estilos.legendaSemantica} aria-label="Legenda de situação">
          <span><i style={{ background: CORES.verde }} />Adequado</span>
          <span><i style={{ background: CORES.azul }} />Acompanhar</span>
          <span><i style={{ background: CORES.amarelo }} />Atenção</span>
          <span><i style={{ background: CORES.vermelho }} />Crítico</span>
        </div>
        <div className={estilos.graficoUnidades} style={{ height: `${Math.max(300, dados.length * 42)}px` }}>
          <ResponsiveContainer width="100%" height="100%">
            <BarChart data={dados} layout="vertical" margin={{ top: 8, right: 18, left: 22, bottom: 4 }}>
              <CartesianGrid strokeDasharray="3 3" horizontal={false} stroke="#d9e5eb" />
              <XAxis type="number" unit="%" domain={[0, (maximo) => Math.max(120, Math.ceil(maximo / 10) * 10)]} />
              <YAxis type="category" dataKey="nome" width={150} tick={{ fontSize: 11 }} />
              <Tooltip formatter={(valor, nome, item) => [`${percentual(valor)}${nome === "Cobertura da escala" ? ` · ${numero(item.payload.pendentes)} pendência(s)` : ""}`, nome]} />
              <Bar dataKey="cobertura" name="Cobertura da escala" radius={[0, 5, 5, 0]}>
                {dados.map((item) => <Cell key={`cob-${item.nome}`} fill={item.corCobertura} />)}
              </Bar>
              <Bar dataKey="ocupacao" name="Ocupação contratual" radius={[0, 5, 5, 0]}>
                {dados.map((item) => <Cell key={`ocu-${item.nome}`} fill={item.corOcupacao} />)}
              </Bar>
            </BarChart>
          </ResponsiveContainer>
        </div>
        </>
      ) : <div className={estilos.vazio}>Sem unidades com escala esperada ou dimensionamento contratual para comparar.</div>}
      <p className={estilos.nota}>Cobertura da escala, pendências e ausências deste comparativo são uma fotografia da data de referência; não são médias do período selecionado. A lista prioriza menor cobertura e maior quantidade de alertas contratuais. Ocupação acima de 100% indica efetivo vinculado superior ao previsto.</p>
    </Cartao>
  );
};

const motivosUnidade = (unidade: (DTO.PainelGestaoDTOUnidadeGestao)) => {
  const motivos: string[] = [];
  if (Number(unidade.alertasContratuais || 0) > 0) motivos.push(`${numero(unidade.alertasContratuais)} alerta(s) contratual(is)`);
  if (Number(unidade.empregadosEsperados || 0) > 0 && Number(unidade.percentualCoberturaEscala || 0) < 85) {
    motivos.push(`${numero(unidade.pendentesFrequencia)} pendência(s) de frequência · cobertura ${percentual(unidade.percentualCoberturaEscala)}`);
  }
  if (Number(unidade.percentualOcupacao || 0) > 110) motivos.push("ocupação acima de 110%");
  if (unidade.efetivoPrevisto != null && Number(unidade.percentualOcupacao || 0) < 80) motivos.push("ocupação abaixo de 80%");
  if (Number(unidade.ausenciasNaReferencia || 0) > 0) motivos.push(`${numero(unidade.ausenciasNaReferencia)} ausência(s) na referência`);
  return motivos;
};

export const PrioridadesPorUnidade = ({ unidades = [], nivel }: { unidades?: DTO.PainelGestaoDTOUnidadeGestao[]; nivel?: string }) => {
  const ranking = useMemo(() => unidades
    .map((unidade) => ({ unidade, motivos: motivosUnidade(unidade) }))
    .filter((item) => item.motivos.length)
    .sort((a, b) => b.motivos.length - a.motivos.length
      || Number(a.unidade.percentualCoberturaEscala || 0) - Number(b.unidade.percentualCoberturaEscala || 0))
    .slice(0, 8), [unidades]);
  const rotulo = nivel === "NACIONAL" ? "gestores estaduais" : nivel === "SUPERINTENDENCIA" ? "Gestões Locais" : "lotações";
  return (
    <Cartao titulo="Unidades que exigem atenção na referência" icone={AlertOctagon} detalhe={`${ranking.length} ${rotulo}`}>
      <FaixaResumo
        itens={[
          { rotulo: "Em atenção", valor: numero(ranking.length), classe: ranking.length ? estilos.resumoCritico : estilos.resumoInfo },
        ]}
      />
      <div className={estilos.listaPrioridades}>
        {ranking.map(({ unidade, motivos }, indice) => (
          <div key={`${unidade.tipo}-${unidade.id}`}>
            <span className={estilos.posicao}>{indice + 1}</span>
            <div><strong>{unidade.nome}</strong><p>{motivos.join(" · ")}</p></div>
          </div>
        ))}
        {!ranking.length && <div className={estilos.vazio}>Nenhuma unidade ultrapassou os critérios explícitos de atenção.</div>}
      </div>
    </Cartao>
  );
};

export const RankingLotacaoTurno = ({ dados = [], periodoDias = 30, carregando = false, aoCarregar }: { dados?: DTO.PainelRankingDTO[]; periodoDias?: number; carregando?: boolean; aoCarregar?: () => void }) => {
  const comRegistros = useMemo(() => dados
    .map((item) => ({ ...item, totalOperacional: Number(item.presencas || 0) + Number(item.faltas || 0) }))
    .filter((item) => item.totalOperacional > 0)
    .sort((a, b) => Number(a.taxa || 0) - Number(b.taxa || 0)
      || Number(b.faltas || 0) - Number(a.faltas || 0))
    .slice(0, 12), [dados]);
  const semRegistros = dados.filter((item) => Number(item.presencas || 0) + Number(item.faltas || 0) === 0).length;
  return (
    <Cartao titulo="Atenção por lotação e turno" icone={Gauge} detalhe={`${periodoDias} dias`} largo>
      {!dados.length && (
        <div className={estilos.auditoriaCabecalho}>
          <span>Consulta detalhada sob demanda para não bloquear a abertura do painel com grandes efetivos.</span>
          <button type="button" onClick={aoCarregar} disabled={carregando}>
            {carregando ? "Calculando..." : "Carregar ranking"}
          </button>
        </div>
      )}
      {dados.length > 0 && (
        <FaixaResumo
          itens={[
            { rotulo: "Lotações / turnos", valor: numero(comRegistros.length), classe: estilos.resumoInfo },
            { rotulo: "Sem registro", valor: numero(semRegistros), classe: semRegistros > 0 ? estilos.resumoAtencao : estilos.resumoSucesso },
          ]}
        />
      )}
      <div className={estilos.tabelaRanking}>
        <table>
          <thead><tr><th>Lotação</th><th>Turno</th><th>Ativos</th><th>Presenças</th><th>Ausências</th><th>Taxa de presença</th></tr></thead>
          <tbody>
            {comRegistros.map((item) => (
              <tr key={`${item.lotacaoId}-${item.turnoId}`}>
                <td><strong>{item.nome}</strong></td>
                <td>{item.turno || "—"}</td>
                <td>{numero(item.escalados)}</td>
                <td>{numero(item.presencas)}</td>
                <td className={Number(item.faltas || 0) > 0 ? estilos.textoAlerta : ""}>{numero(item.faltas)}</td>
                <td><span className={Number(item.taxa || 0) < 85 ? estilos.taxaCritica : estilos.taxaRegular}>{percentual(item.taxa)}</span></td>
              </tr>
            ))}
            {!comRegistros.length && <tr><td colSpan={6} className={estilos.vazio}>Sem registros operacionais para classificar lotações e turnos.</td></tr>}
          </tbody>
        </table>
      </div>
      <p className={estilos.nota}>Ordenado pela menor taxa de presença entre os registros operacionais do período. Não representa cobertura da escala. {semRegistros > 0 ? `${numero(semRegistros)} combinação(ões) ativa(s) não possui(em) registros operacionais no período.` : ""}</p>
    </Cartao>
  );
};

export const AuditoriaRecente = ({ atividades = [], podeAbrirLogs, aoNavegar }: { atividades?: import("../../../../contracts/painel").ActivitySummary[]; podeAbrirLogs?: boolean; aoNavegar: (rota: string) => void }) => (
  <Cartao titulo="Atividade gerencial recente" icone={ShieldCheck} detalhe={podeAbrirLogs ? "Auditoria completa disponível" : "Escopo autenticado"} largo>
    <div className={estilos.auditoriaCabecalho}>
      <span>Alterações recentes registradas no escopo permitido.</span>
      {podeAbrirLogs && <button type="button" onClick={() => aoNavegar("/logs")}>Abrir logs</button>}
    </div>
    <FaixaResumo itens={[{ rotulo: "Eventos listados", valor: numero(atividades.length), classe: estilos.resumoInfo }]} />
    <div className={estilos.listaAuditoria}>
      {atividades.map((item) => (
        <div key={item.id || `${item.dataEvento}-${item.descricao}`}>
          <span className={estilos.avatar}>{String(item.usuarioUsername || "S").charAt(0).toUpperCase()}</span>
          <div>
            <strong>{item.usuarioUsername || "Sistema"}</strong>
            <p>{item.descricao}{item.empregadoNome ? ` · ${item.empregadoNome}` : ""}</p>
          </div>
          <time>{item.dataEvento ? new Date(item.dataEvento).toLocaleString("pt-BR") : "—"}</time>
        </div>
      ))}
      {!atividades.length && <div className={estilos.vazio}>Nenhuma atividade recente disponível para este escopo.</div>}
    </div>
  </Cartao>
);

const IndicadorVariacao = ({ valor, melhorQuandoCai = false, sufixo = "" }: { valor?: number; melhorQuandoCai?: boolean; sufixo?: string }) => {
  if (valor == null) {
    return <span className={estilos.deltaNeutro}><Minus size={15} aria-hidden="true" />Sem base comparável</span>;
  }
  const numeroValor = Number(valor || 0);
  const neutro = Math.abs(numeroValor) < 0.05;
  const favoravel = melhorQuandoCai ? numeroValor < 0 : numeroValor > 0;
  const Icone = neutro ? Minus : numeroValor > 0 ? ArrowUpRight : ArrowDownRight;
  const classe = neutro ? estilos.deltaNeutro : favoravel ? estilos.deltaFavoravel : estilos.deltaDesfavoravel;
  return (
    <span className={classe}>
      <Icone size={15} aria-hidden="true" />
      {numeroValor > 0 ? "+" : ""}{numeroValor.toLocaleString("pt-BR", { maximumFractionDigits: 1 })}{sufixo}
    </span>
  );
};

export const ComparativoPeriodo = ({ periodo = {}, comparativo = {} }: { periodo?: Partial<DTO.PainelGestaoDTOAnalisePeriodo>; comparativo?: Partial<DTO.PainelGestaoDTOComparativoPeriodo> }) => {
  const ausenciasAtuais = Number(periodo.faltasNaoJustificadas || 0)
    + Number(periodo.atestados || 0)
    + Number(periodo.suspensoes || 0);
  const temBaseAtual = Number(periodo.registrosOperacionais || 0) > 0;
  const temBaseAnterior = Number(comparativo.registrosOperacionaisAnterior || 0) > 0;
  const temBaseComparavel = temBaseAtual && temBaseAnterior;

  if (!temBaseComparavel) {
    return (
      <Cartao titulo="Comparação com a janela anterior" icone={BarChart3} detalhe="Janelas de igual duração" largo>
        <FaixaResumo
          itens={[
            {
              rotulo: "Período atual",
              valor: intervaloPeriodo(periodo),
              classe: estilos.resumoInfo,
            },
            {
              rotulo: "Período anterior",
              valor: `${dataBr(comparativo.inicioAnterior)} a ${dataBr(comparativo.fimAnterior)}`,
              classe: estilos.resumoInfo,
            },
          ]}
        />
        <div className={estilos.vazio}>
          Não há lançamentos operacionais suficientes nas duas janelas para calcular uma comparação confiável.
        </div>
        <p className={estilos.nota}>
          O painel não apresenta 0% para a janela sem dados, porque isso poderia ser interpretado incorretamente como ausência real de absenteísmo.
        </p>
      </Cartao>
    );
  }

  const itens = [
    {
      rotulo: "Taxa de presença",
      atual: percentual(periodo.taxaPresenca),
      anterior: percentual(comparativo.taxaPresencaAnterior),
      delta: comparativo.variacaoTaxaPresencaPp,
      sufixo: " p.p.",
      melhorQuandoCai: false,
      classe: estilos.resumoSucesso,
    },
    {
      rotulo: "Taxa de absenteísmo",
      atual: percentual(periodo.taxaAbsenteismo),
      anterior: percentual(comparativo.taxaAbsenteismoAnterior),
      delta: comparativo.variacaoTaxaAbsenteismoPp,
      sufixo: " p.p.",
      melhorQuandoCai: true,
      classe: estilos.resumoAtencao,
    },
    {
      rotulo: "Ausências",
      atual: numero(ausenciasAtuais),
      anterior: numero(comparativo.ausenciasAnterior),
      delta: comparativo.variacaoAusencias,
      sufixo: "",
      melhorQuandoCai: true,
      classe: estilos.resumoCritico,
    },
    {
      rotulo: "Registros RT",
      atual: numero(periodo.repousosTrabalhados),
      anterior: numero(comparativo.repousosTrabalhadosAnterior),
      delta: comparativo.variacaoRepousosTrabalhados,
      sufixo: "",
      melhorQuandoCai: true,
      classe: estilos.resumoInfo,
    },
  ];

  return (
    <Cartao titulo="Comparação com a janela anterior" icone={BarChart3} detalhe="Janelas de igual duração" largo>
      <div className={estilos.comparativoGrade}>
        {itens.map((item) => (
          <div key={item.rotulo} className={item.classe}>
            <span>{item.rotulo}</span>
            <strong>{item.atual}</strong>
            <small>Anterior: {item.anterior}</small>
            <IndicadorVariacao valor={item.delta} sufixo={item.sufixo} melhorQuandoCai={item.melhorQuandoCai} />
          </div>
        ))}
      </div>
      <p className={estilos.nota}>
        Atual: {dataBr(periodo.inicio)} a {dataBr(periodo.fim)}. Anterior: {dataBr(comparativo.inicioAnterior)} a {dataBr(comparativo.fimAnterior)}.
        As variações das taxas são mostradas em pontos percentuais.
      </p>
    </Cartao>
  );
};

export const SituacaoDetalhadaQuadro = ({ dados = [] }: { dados?: DTO.PainelGestaoDTOSituacaoQuadro[] }) => {
  const itens = dados.map((item) => ({
    ...item,
    rotulo: ROTULOS_SITUACAO[String(item.status)] || item.status,
    cor: COR_SITUACAO[String(item.status)] || CORES.azulEscuro,
  }));
  return (
    <Cartao titulo="Situação detalhada do quadro" icone={UsersRound} detalhe={`${numero(itens.reduce((soma, item) => soma + Number(item.quantidade || 0), 0))} empregados`}>
      <FaixaResumo
        itens={itens.slice(0, 4).map((item) => ({
          rotulo: item.rotulo,
          valor: numero(item.quantidade),
          classe: estilos.resumoInfo,
        }))}
      />
      <div className={estilos.graficoAlto}>
        {itens.length ? (
          <ResponsiveContainer width="100%" height="100%">
            <BarChart data={itens} layout="vertical" margin={{ top: 4, right: 34, left: 24, bottom: 2 }}>
              <CartesianGrid strokeDasharray="3 3" horizontal={false} stroke="#d9e5eb" />
              <XAxis type="number" allowDecimals={false} />
              <YAxis type="category" dataKey="rotulo" width={122} tick={{ fontSize: 11 }} />
              <Tooltip formatter={(valor, nome, item) => [`${numero(valor)} (${percentual(item.payload.percentual)})`, "Empregados"]} />
              <Bar dataKey="quantidade" name="Empregados" radius={[0, 6, 6, 0]}>
                {itens.map((item, indice) => (
                  <Cell key={`${item.status || item.rotulo}-${indice}`} fill={item.cor} />
                ))}
              </Bar>
            </BarChart>
          </ResponsiveContainer>
        ) : <div className={estilos.vazio}>Sem empregados no escopo autenticado.</div>}
      </div>
    </Cartao>
  );
};

export const ChecklistGovernanca = ({ dados = {} }: { dados?: ManagementPanelView }) => {
  const verificacoes = [
    {
      rotulo: "Vínculos organizacionais e contratuais íntegros",
      conforme: Number(dados.integridade?.totalInconsistencias || 0) === 0,
      detalhe: `${numero(dados.integridade?.totalInconsistencias)} inconsistência(s)`,
    },
    {
      rotulo: "Cobertura da frequência na data de referência em pelo menos 95%",
      conforme: Number(dados.frequencia?.empregadosEsperados || 0) === 0
        || Number(dados.frequencia?.percentualCobertura || 0) >= 95,
      detalhe: percentual(dados.frequencia?.percentualCobertura),
    },
    {
      rotulo: "Nenhum contrato vencido mantido como ativo",
      conforme: Number(dados.contratos?.vencidosMarcadosAtivos || 0) === 0,
      detalhe: `${numero(dados.contratos?.vencidosMarcadosAtivos)} contrato(s)`,
    },
    {
      rotulo: "Nenhum empregado ativo sem contrato vigente",
      conforme: Number(dados.efetivo?.ativosSemContrato || 0) === 0,
      detalhe: `${numero(dados.efetivo?.ativosSemContrato)} empregado(s)`,
    },
    {
      rotulo: "Nenhum trabalho fora da escala sem RT",
      conforme: Number(dados.frequencia?.trabalhosForaEscalaSemRt || 0) === 0,
      detalhe: `${numero(dados.frequencia?.trabalhosForaEscalaSemRt)} registro(s)`,
    },
  ];
  const conformes = verificacoes.filter((item) => item.conforme).length;
  return (
    <Cartao titulo="Checklist de governança" icone={ShieldCheck} detalhe={`${conformes}/${verificacoes.length} controles conformes`}>
      <FaixaResumo
        itens={[
          { rotulo: "Conformes", valor: numero(conformes), classe: estilos.resumoSucesso },
          { rotulo: "Pendentes", valor: numero(verificacoes.length - conformes), classe: conformes === verificacoes.length ? estilos.resumoInfo : estilos.resumoAtencao },
        ]}
      />
      <ul className={estilos.checklist}>
        {verificacoes.map((item) => (
          <li key={item.rotulo} className={item.conforme ? estilos.itemConforme : estilos.itemPendente}>
            {item.conforme ? <CheckCircle2 size={18} aria-hidden="true" /> : <XCircle size={18} aria-hidden="true" />}
            <div><strong>{item.rotulo}</strong><span>{item.detalhe}</span></div>
          </li>
        ))}
      </ul>
      <p className={estilos.nota}>O checklist não cria uma nota opaca: cada controle e sua condição são exibidos separadamente.</p>
    </Cartao>
  );
};
