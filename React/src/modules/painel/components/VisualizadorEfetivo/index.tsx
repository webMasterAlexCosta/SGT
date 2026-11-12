import type { ChartTooltipProps } from '../../../../contracts/painel';
import React, { useMemo, useState } from "react";
import {
  AlertTriangle,
  ArrowRight,
  BarChart3,
  BriefcaseBusiness,
  CheckCircle2,
  CircleDot,
  CircleEllipsis,
  ClipboardCheck,
  GitBranch,
  LineChart as LineChartIcon,
  UserMinus,
  Users,
} from "lucide-react";
import {
  Bar,
  BarChart,
  CartesianGrid,
  Cell,
  Legend,
  Line,
  LineChart,
  ReferenceLine,
  ResponsiveContainer,
  Scatter,
  ScatterChart,
  Tooltip,
  XAxis,
  YAxis,
  ZAxis,
} from "recharts";
import estilos from "./index.module.css";

const CORES = {
  previsto: "#005baa",
  operacional: "#003b71",
  presentes: "#e8bd00",
  ausentes: "#c8423a",
  pendentes: "#d97706",
  outros: "#71808a",
  anterior: "#71808a",
  grade: "#dbe4ea",
  eixo: "#536977",
};

const TIPOS_VISUALIZACAO = [
  { id: "LINHAS", rotulo: "Linhas", icone: LineChartIcon },
  { id: "BARRAS", rotulo: "Barras", icone: BarChart3 },
  { id: "DISPERSAO", rotulo: "Dispersão", icone: CircleDot },
  { id: "INFOGRAFICO", rotulo: "Infográfico", icone: GitBranch },
];

const numero = (valor: (unknown) | (number | undefined) | (number)) => new Intl.NumberFormat("pt-BR").format(Math.round(Number(valor || 0)));
const percentual = (valor: number | null | undefined) => `${Number(valor || 0).toLocaleString("pt-BR", {
  minimumFractionDigits: 1,
  maximumFractionDigits: 2,
})}%`;

// Transformação estritamente visual: dimensiona faixas/barras. Não produz indicador gerencial.
const larguraVisual = (valor: number | null | undefined, base: (number)) => {
  const denominador = Number(base || 0);
  if (denominador <= 0) return 0;
  return Math.max(0, Math.min(100, (Number(valor || 0) * 100) / denominador));
};

const TooltipSerie = ({ active, label, payload }: ChartTooltipProps) => {
  if (!active || !payload?.length) return null;
  return (
    <div className={estilos.tooltip}>
      <strong>{label}</strong>
      {payload.map((item) => (
        <div key={`${item.dataKey}-${item.name}`}>
          <span style={{ background: item.color || item.fill }} />
          <b>{item.name}</b>
          <em>{numero(item.value)}</em>
        </div>
      ))}
    </div>
  );
};

const TooltipDispersao = ({ active, payload }: ChartTooltipProps<{ rotulo?: string; efetivoPrevisto?: number; efetivoAlocado?: number; presentes?: number; ausentes?: number; pendentesLancamento?: number }>) => {
  const ponto = payload?.[0]?.payload;
  if (!active || !ponto) return null;
  return (
    <div className={`${estilos.tooltip} ${estilos.tooltipDispersao}`}>
      <strong>{ponto.rotulo}</strong>
      {Object.prototype.hasOwnProperty.call(ponto, "efetivoPrevisto") && (
        <div><span style={{ background: CORES.previsto }} /><b>Previsto em contrato</b><em>{numero(ponto.efetivoPrevisto)}</em></div>
      )}
      <div><span style={{ background: CORES.operacional }} /><b>Efetivo operacional</b><em>{numero(ponto.efetivoAlocado)}</em></div>
      <div><span style={{ background: CORES.presentes }} /><b>Presentes</b><em>{numero(ponto.presentes)}</em></div>
      <div><span style={{ background: CORES.ausentes }} /><b>Ausentes</b><em>{numero(ponto.ausentes)}</em></div>
      {Number(ponto.pendentesLancamento || 0) > 0 && (
        <div><span style={{ background: CORES.pendentes }} /><b>Pendentes</b><em>{numero(ponto.pendentesLancamento)}</em></div>
      )}
    </div>
  );
};

const EixosTemporais = () => (
  <>
    <CartesianGrid strokeDasharray="3 3" vertical={false} stroke={CORES.grade} />
    <XAxis dataKey="rotulo" minTickGap={20} tick={{ fontSize: 10, fill: CORES.eixo }} />
    <YAxis allowDecimals={false} tick={{ fontSize: 10, fill: CORES.eixo }} width={42} />
    <Tooltip content={<TooltipSerie />} />
    <Legend wrapperStyle={{ fontSize: 10, paddingTop: 7 }} />
  </>
);

const GraficoLinhas = ({ serie, comparar, exibirPendentes }: { serie: import("../../../../contracts/painel").StaffingSeriesPoint[]; comparar: (boolean); exibirPendentes: (boolean) }) => (
  <ResponsiveContainer width="100%" height="100%" initialDimension={{ width: 400, height: 300 }}>
    <LineChart data={serie} margin={{ top: 14, right: 18, left: -4, bottom: 2 }}>
      <EixosTemporais />
      <Line isAnimationActive={false} type="monotone" dataKey="efetivoPrevisto" name="Previsto em contrato" stroke={CORES.previsto} strokeDasharray="5 4" strokeWidth={2.2} dot={false} />
      <Line isAnimationActive={false} type="monotone" dataKey="efetivoAlocado" name="Efetivo operacional" stroke={CORES.operacional} strokeWidth={2.8} dot={serie.length <= 31} />
      <Line isAnimationActive={false} type="monotone" dataKey="presentes" name="Presentes" stroke={CORES.presentes} strokeWidth={2.3} dot={serie.length <= 16} />
      <Line isAnimationActive={false} type="monotone" dataKey="ausentes" name="Ausentes" stroke={CORES.ausentes} strokeWidth={2} dot={serie.length <= 16} />
      {exibirPendentes && (
        <Line isAnimationActive={false} type="monotone" dataKey="pendentesLancamento" name="Pendentes de lançamento" stroke={CORES.pendentes} strokeWidth={1.8} dot={serie.length <= 16} />
      )}
      {comparar && (
        <Line isAnimationActive={false} type="monotone" dataKey="presentesAnoAnterior" name="Presentes no ano anterior" stroke={CORES.anterior} strokeDasharray="3 4" strokeWidth={1.7} dot={false} connectNulls />
      )}
    </LineChart>
  </ResponsiveContainer>
);

const BarraFotografia = ({ rotulo, valor, base, detalhe, cor, alerta = false }: { rotulo: string; valor: number | null | undefined; base: number; detalhe: React.ReactNode; cor: string; alerta?: boolean }) => {
  const largura = larguraVisual(valor, base);
  return (
    <div className={`${estilos.linhaFotografia} ${alerta ? estilos.linhaFotografiaAlerta : ""}`}>
      <div className={estilos.rotuloFotografia}>
        <span>{rotulo}<small>{detalhe}</small></span>
        <strong>{numero(valor)}</strong>
      </div>
      <span className={estilos.trilhoFotografia}>
        <i style={{ width: `${largura}%`, background: cor }} />
      </span>
    </div>
  );
};

const BarrasFotografia = ({ resumo, exibirPendentes }: { resumo: Partial<import("../../../../contracts/java").PainelEfetivoDTOResumo>; exibirPendentes: boolean }) => {
  const previsto = Number(resumo.efetivoPrevisto || 0);
  const alocado = Number(resumo.efetivoAlocado || 0);
  const base = Math.max(1, previsto, alocado);
  const itens = [
    { rotulo: "Previsto em contrato", valor: previsto, base, detalhe: "Dimensionamento vigente devolvido pelo Java", cor: CORES.previsto },
    { rotulo: "Efetivo operacional", valor: alocado, base, detalhe: `${numero(alocado)} alocado(s) para ${numero(previsto)} previsto(s)`, cor: CORES.operacional },
    { rotulo: "Presentes", valor: resumo.presentes, base, detalhe: `${percentual(resumo.percentualAtendimento)} de atendimento contratual`, cor: CORES.presentes },
    { rotulo: "Ausentes", valor: resumo.ausentes, base, detalhe: "Quantidade classificada pelo backend", cor: CORES.ausentes },
    ...(exibirPendentes ? [{
      rotulo: "Pendentes de lançamento",
      valor: resumo.pendentesLancamento,
      base,
      detalhe: "Alocados sem lançamento, conforme o backend",
      cor: CORES.pendentes,
      alerta: true,
    }] : []),
  ];

  return (
    <div className={estilos.fotografiaBarras} aria-label="Comparação das quantidades na data de referência">
      {itens.map((item) => <BarraFotografia key={item.rotulo} {...item} />)}
    </div>
  );
};

const GraficoBarras = ({ serie, resumo, comparar, exibirPendentes }: { serie: import("../../../../contracts/painel").StaffingSeriesPoint[]; resumo: Partial<import("../../../../contracts/java").PainelEfetivoDTOResumo>; comparar: (boolean); exibirPendentes: (boolean) }) => {
  if (serie.length <= 1) return <BarrasFotografia resumo={resumo} exibirPendentes={exibirPendentes} />;
  return (
    <ResponsiveContainer width="100%" height="100%" initialDimension={{ width: 400, height: 300 }}>
      <BarChart data={serie} margin={{ top: 14, right: 18, left: -4, bottom: 2 }} barGap={1}>
        <EixosTemporais />
        <Bar isAnimationActive={false} dataKey="efetivoPrevisto" name="Previsto em contrato" fill={CORES.previsto} radius={[3, 3, 0, 0]} maxBarSize={15} />
        <Bar isAnimationActive={false} dataKey="efetivoAlocado" name="Efetivo operacional" fill={CORES.operacional} radius={[3, 3, 0, 0]} maxBarSize={15} />
        <Bar isAnimationActive={false} dataKey="presentes" name="Presentes" fill={CORES.presentes} radius={[3, 3, 0, 0]} maxBarSize={15} />
        <Bar isAnimationActive={false} dataKey="ausentes" name="Ausentes" fill={CORES.ausentes} radius={[3, 3, 0, 0]} maxBarSize={13} />
        {exibirPendentes && (
          <Bar isAnimationActive={false} dataKey="pendentesLancamento" name="Pendentes de lançamento" fill={CORES.pendentes} radius={[3, 3, 0, 0]} maxBarSize={12} />
        )}
        {comparar && (
          <Bar isAnimationActive={false} dataKey="presentesAnoAnterior" name="Presentes no ano anterior" fill={CORES.anterior} radius={[3, 3, 0, 0]} maxBarSize={12} />
        )}
      </BarChart>
    </ResponsiveContainer>
  );
};

const GraficoDispersao = ({ pontos }: { pontos: import("../../../../contracts/painel").StaffingScatterPoint[] }) => {
  const dados = useMemo(() => pontos.map((ponto) => ({
    ...ponto,
    impactoOperacional: Math.max(1, Number(ponto.ausentes || 0) + Number(ponto.pendentesLancamento || 0)),
  })), [pontos]);
  const limite = Math.max(1, Math.ceil(Math.max(...dados.flatMap((ponto) => [
    Number(ponto.efetivoAlocado || 0),
    Number(ponto.presentes || 0),
  ])) * 1.08));

  return (
    <ResponsiveContainer width="100%" height="100%" initialDimension={{ width: 400, height: 300 }}>
      <ScatterChart margin={{ top: 12, right: 22, left: 10, bottom: 18 }}>
        <CartesianGrid strokeDasharray="3 3" stroke={CORES.grade} />
        <XAxis
          type="number"
          dataKey="efetivoAlocado"
          name="Efetivo operacional"
          domain={[0, limite]}
          allowDecimals={false}
          tick={{ fontSize: 10, fill: CORES.eixo }}
          label={{ value: "Efetivo operacional (pessoas)", position: "insideBottom", offset: -10, fontSize: 10, fill: CORES.eixo }}
        />
        <YAxis
          type="number"
          dataKey="presentes"
          name="Presentes"
          domain={[0, limite]}
          allowDecimals={false}
          width={54}
          tick={{ fontSize: 10, fill: CORES.eixo }}
          label={{ value: "Presentes", angle: -90, position: "insideLeft", fontSize: 10, fill: CORES.eixo }}
        />
        <ZAxis type="number" dataKey="impactoOperacional" range={[70, 340]} />
        <ReferenceLine
          segment={[{ x: 0, y: 0 }, { x: limite, y: limite }]}
          stroke={CORES.operacional}
          strokeDasharray="5 5"
          ifOverflow="extendDomain"
        />
        <Tooltip content={<TooltipDispersao />} cursor={{ strokeDasharray: "3 3" }} />
        <Scatter isAnimationActive={false} name="Pontos analisados" data={dados} fill={CORES.previsto}>
          {dados.map((ponto, indice) => (
            <Cell
              key={`${ponto.data || ponto.rotulo}-${indice}`}
              fill={Number(ponto.pendentesLancamento || 0) > 0 ? CORES.pendentes : CORES.previsto}
              fillOpacity={0.76}
              stroke={Number(ponto.pendentesLancamento || 0) > 0 ? CORES.pendentes : CORES.operacional}
            />
          ))}
        </Scatter>
      </ScatterChart>
    </ResponsiveContainer>
  );
};

const EtapaInfografico = ({ icone: Icone, titulo, valor, detalhe, tipo = "neutro" }: { icone: import("lucide-react").LucideIcon; titulo: string; valor: number; detalhe: string; tipo?: string | "atencao" | "operacional" }) => (
  <article className={`${estilos.etapaInfografico} ${estilos[`etapa_${tipo}`] || ""}`}>
    <span className={estilos.iconeEtapa}><Icone size={19} aria-hidden="true" /></span>
    <div><small>{titulo}</small><strong>{numero(valor)}</strong><p>{detalhe}</p></div>
  </article>
);

const InfograficoEfetivo = ({ resumo }: { resumo: Partial<import("../../../../contracts/java").PainelEfetivoDTOResumo> }) => {
  const previsto = Number(resumo.efetivoPrevisto || 0);
  const alocado = Number(resumo.efetivoAlocado || 0);
  const lancados = Number(resumo.registrosLancados || 0);
  const presentes = Number(resumo.presentes || 0);
  const ausentes = Number(resumo.ausentes || 0);
  const outros = Number(resumo.outrosRegistros || 0);
  const pendentes = Number(resumo.pendentesLancamento || 0);
  const cobertura = Number(resumo.percentualCobertura || 0);
  const atendimento = Number(resumo.percentualAtendimento || 0);
  const saldo = Number(resumo.deficitContratual || 0) > 0
    ? `${numero(resumo.deficitContratual)} vaga(s) sem alocação`
    : Number(resumo.excedenteContratual || 0) > 0
      ? `${numero(resumo.excedenteContratual)} acima do previsto`
      : "Aderente ao previsto";

  return (
    <div className={estilos.infografico} aria-label="Fluxo gerencial do efetivo na data de referência">
      <div className={estilos.fluxoInfografico}>
        <EtapaInfografico icone={BriefcaseBusiness} titulo="1. Capacidade contratada" valor={previsto} detalhe="Previsto vigente em contrato" tipo="contrato" />
        <ArrowRight className={estilos.setaFluxo} size={20} aria-hidden="true" />
        <EtapaInfografico icone={Users} titulo="2. Efetivo operacional" valor={alocado} detalhe={saldo} tipo="operacional" />
        <ArrowRight className={estilos.setaFluxo} size={20} aria-hidden="true" />
        <EtapaInfografico icone={ClipboardCheck} titulo="3. Registros realizados" valor={lancados} detalhe={`${percentual(cobertura)} dos alocados com registro`} tipo={pendentes > 0 ? "atencao" : "operacional"} />
      </div>

      <div className={estilos.resultadoInfografico}>
        <div className={estilos.tituloResultado}>
          <div><small>4. Resultado operacional</small><strong>{percentual(atendimento)} de atendimento sobre o previsto</strong></div>
          {pendentes > 0 && <span><AlertTriangle size={15} aria-hidden="true" /> {numero(pendentes)} pendente(s)</span>}
        </div>
        <div className={estilos.barraComposicao} aria-label={`Presentes ${numero(presentes)}, ausentes ${numero(ausentes)}, outros ${numero(outros)} e pendentes ${numero(pendentes)}`}>
          <span className={estilos.faixaPresentes} style={{ width: `${larguraVisual(presentes, alocado)}%` }} />
          <span className={estilos.faixaAusentes} style={{ width: `${larguraVisual(ausentes, alocado)}%` }} />
          <span className={estilos.faixaOutros} style={{ width: `${larguraVisual(outros, alocado)}%` }} />
          <span className={estilos.faixaPendentes} style={{ width: `${larguraVisual(pendentes, alocado)}%` }} />
        </div>
        <div className={estilos.legendaComposicao}>
          <div><CheckCircle2 size={15} /><span>Presentes<strong>{numero(presentes)}</strong></span></div>
          <div><UserMinus size={15} /><span>Ausentes<strong>{numero(ausentes)}</strong></span></div>
          <div><CircleEllipsis size={15} /><span>Outros registros<strong>{numero(outros)}</strong></span></div>
          <div className={pendentes > 0 ? estilos.legendaPendente : ""}><AlertTriangle size={15} /><span>Pendentes<strong>{numero(pendentes)}</strong></span></div>
        </div>
      </div>
    </div>
  );
};

const TabelaAlternativa = ({ tipo, serie, pontosDispersao, comparar, exibirPendentes }: { tipo: (string); serie: import("../../../../contracts/painel").StaffingSeriesPoint[]; pontosDispersao: import("../../../../contracts/painel").StaffingScatterPoint[]; comparar: (boolean); exibirPendentes: (boolean) }) => {
  const itens = tipo === "DISPERSAO" ? pontosDispersao : serie;
  if (!itens.length) return null;
  return (
    <details className={estilos.tabelaAlternativa}>
      <summary>Ver os dados desta visualização em tabela acessível</summary>
      <div>
        <table>
          <thead>
            <tr>
              <th>{tipo === "DISPERSAO" ? "Ponto" : "Período"}</th>
              <th>Previsto</th><th>Alocados</th><th>Presentes</th><th>Ausentes</th>
              {exibirPendentes && <th>Pendentes</th>}
              <th>Cobertura</th><th>Atendimento</th>
              {comparar && tipo !== "DISPERSAO" && <th>Presentes ano anterior</th>}
            </tr>
          </thead>
          <tbody>
            {itens.map((item, indice) => (
              <tr key={`${item.data || item.id || item.rotulo || "ponto"}-${indice}`}>
                <td>{item.rotulo || item.nome || item.data || `Ponto ${indice + 1}`}</td>
                <td>{Object.prototype.hasOwnProperty.call(item, "efetivoPrevisto") ? numero(item.efetivoPrevisto) : "—"}</td>
                <td>{numero(item.efetivoAlocado)}</td>
                <td>{numero(item.presentes)}</td>
                <td>{numero(item.ausentes)}</td>
                {exibirPendentes && <td>{numero(item.pendentesLancamento)}</td>}
                <td>{Object.prototype.hasOwnProperty.call(item, "percentualCobertura") ? percentual(item.percentualCobertura) : "—"}</td>
                <td>{Object.prototype.hasOwnProperty.call(item, "percentualAtendimento") ? percentual(item.percentualAtendimento) : "—"}</td>
                {comparar && tipo !== "DISPERSAO" && <td>{item.presentesAnoAnterior == null ? "—" : numero(item.presentesAnoAnterior)}</td>}
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    </details>
  );
};

const NOTAS: Readonly<Record<string, string | undefined>> = {
  LINHAS: "Evidencia tendência e mudança de direção. Previsto e efetivo operacional representam capacidade; presentes e ausentes representam execução.",
  BARRAS: "Compara as quantidades na referência ou entre períodos. Pendentes aparecem apenas quando existe empregado alocado sem lançamento.",
  INFOGRAFICO: "Resume a fotografia da referência e mostra onde a capacidade contratada se transforma — ou deixa de se transformar — em presença registrada.",
};

export const VisualizadorEfetivo = ({
  serie = [],
  pontosDispersao = [],
  contextoDispersao = "períodos analisados",
  resumo = {},
  comparar = false,
  exibirPendentes = false,
  carregando = false,
  escopoSelecionado,
  periodoRotulo,
  intervalo,
}: { serie?: import("../../../../contracts/painel").StaffingSeriesPoint[]; pontosDispersao?: import("../../../../contracts/painel").StaffingScatterPoint[]; contextoDispersao?: (string); resumo?: Partial<import("../../../../contracts/java").PainelEfetivoDTOResumo>; comparar?: (boolean); exibirPendentes?: (boolean); carregando?: (boolean); escopoSelecionado: (string); periodoRotulo: (string); intervalo: (string) }) => {
  const [tipoSelecionado, setTipoSelecionado] = useState("LINHAS");
  const podeExibirLinhas = serie.length >= 2;
  const podeExibirDispersao = pontosDispersao.length >= 3;
  const tipo = tipoSelecionado === "LINHAS" && !podeExibirLinhas
    ? "BARRAS"
    : tipoSelecionado === "DISPERSAO" && !podeExibirDispersao
      ? "BARRAS"
      : tipoSelecionado;
  const possuiResumo = Object.prototype.hasOwnProperty.call(resumo, "efetivoPrevisto");

  const grafico = tipo === "LINHAS"
    ? <GraficoLinhas serie={serie} comparar={comparar} exibirPendentes={exibirPendentes} />
    : tipo === "BARRAS"
      ? <GraficoBarras serie={serie} resumo={resumo} comparar={comparar} exibirPendentes={exibirPendentes} />
      : tipo === "DISPERSAO"
        ? <GraficoDispersao pontos={pontosDispersao} />
        : <InfograficoEfetivo resumo={resumo} />;

  const nota = tipo === "DISPERSAO"
    ? `Cada ponto representa ${contextoDispersao}: eixo X = efetivo operacional, eixo Y = presentes. A bolha cresce com ausências e pendências; a diagonal representa presença integral.`
    : NOTAS[String(tipo)];

  return (
    <article className={estilos.visualizador}>
      <header className={estilos.cabecalhoVisualizador}>
        <div>
          <h3>{tipo === "INFOGRAFICO" ? "Leitura gerencial do efetivo" : "Análise do efetivo no período"}</h3>
          <p>{escopoSelecionado}</p>
        </div>
        <div className={estilos.contextoVisualizador}>
          {tipo === "DISPERSAO" && (
            <span className={estilos.contextoDispersao}>{numero(pontosDispersao.length)} pontos · {contextoDispersao}</span>
          )}
          <strong>{periodoRotulo} · {intervalo}</strong>
        </div>
      </header>

      <nav className={estilos.seletorVisualizacao} aria-label="Tipo de visualização do efetivo">
        <span>Visualização</span>
        <div>
          {TIPOS_VISUALIZACAO.map(({ id, rotulo, icone: Icone }) => {
            const indisponivel = (id === "LINHAS" && !podeExibirLinhas) || (id === "DISPERSAO" && !podeExibirDispersao);
            const motivo = id === "LINHAS"
              ? "Disponível a partir de 2 observações no período"
              : "Disponível com pelo menos 3 períodos ou dimensões comparáveis";
            return (
              <button
                key={id}
                type="button"
                className={tipo === id ? estilos.tipoAtivo : ""}
                aria-pressed={tipo === id}
                disabled={indisponivel}
                title={indisponivel ? motivo : undefined}
                onClick={() => setTipoSelecionado(id)}
              >
                <Icone size={15} aria-hidden="true" /> {rotulo}
              </button>
            );
          })}
        </div>
      </nav>

      <div className={`${estilos.areaVisualizacao} ${tipo === "INFOGRAFICO" ? estilos.areaInfografico : ""} ${tipo === "BARRAS" && serie.length <= 1 ? estilos.areaFotografia : ""}`}>
        {serie.length || (possuiResumo && ["BARRAS", "INFOGRAFICO"].includes(tipo)) || (tipo === "DISPERSAO" && podeExibirDispersao)
          ? grafico
          : <div className={estilos.vazio}>{carregando ? "Calculando indicador…" : "Nenhum contrato ou alocação encontrado para os filtros selecionados."}</div>}
      </div>
      <p className={estilos.notaVisualizacao}>{nota}</p>
      <TabelaAlternativa
        tipo={tipo}
        serie={serie}
        pontosDispersao={pontosDispersao}
        comparar={comparar}
        exibirPendentes={exibirPendentes}
      />
    </article>
  );
};
