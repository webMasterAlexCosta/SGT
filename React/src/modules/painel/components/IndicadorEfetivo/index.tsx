import { mensagemFalha } from "../../../../core/http/mensagemFalha";
import { observarFalha } from '../../../../core/http/falhaObservada';
import type * as DTO from '../../../../contracts/java';
import type { StaffingReport } from '../../../../contracts/painel';
type NonNullableFilterOptions = { [K in keyof DTO.PainelEfetivoDTOFiltros]: NonNullable<DTO.PainelEfetivoDTOFiltros[K]> };
import type { PainelEfetivoDTOPontoDiario } from '../../../../contracts/java';
import React, { useEffect, useLayoutEffect, useMemo, useRef, useState } from "react";
import {
  FileSpreadsheet,
  FileText,
  AlertTriangle,
  BarChart3,
  BriefcaseBusiness,
  Building2,
  CalendarDays,
  CheckCircle2,
  ChevronLeft,
  ChevronRight,
  ClipboardCheck,
  Clock3,
  RefreshCw,
  UserMinus,
  Users,
} from "lucide-react";
import { ServicoPainel } from "../../../../services/painel/ServicoPainel.ts";
import { useConsultaVigente } from "../../../../core/hooks/useConsultaVigente.ts";
import { obterDataNoFusoNegocio } from "../../../../core/formatters/dataHoraNegocio.ts";
import { ModalPainel } from "../ModalPainel/index.tsx";
import { VisualizadorEfetivo } from "../VisualizadorEfetivo/index.tsx";
import { exportarIndicadorEfetivoPdf, exportarIndicadorEfetivoXlsx } from "./exportacao.ts";
import estilos from "./index.module.css";

const PERIODOS = [
  ["DIARIO", "Diário"],
  ["SEMANAL", "Semanal"],
  ["QUINZENAL", "Quinzenal"],
  ["MENSAL", "Mensal"],
  ["SEMESTRAL", "Semestral"],
  ["ANUAL", "Anual"],
];

const numero = (valor: number | null | undefined) => new Intl.NumberFormat("pt-BR").format(Math.round(Number(valor || 0)));
const percentual = (valor: number | null | undefined) => `${Number(valor || 0).toLocaleString("pt-BR", {
  minimumFractionDigits: 1,
  maximumFractionDigits: 2,
})}%`;

const dataCurta = (valor: string | null | undefined) => valor
  ? new Intl.DateTimeFormat("pt-BR", { day: "2-digit", month: "2-digit" }).format(new Date(`${valor}T12:00:00`))
  : "—";

const dataCompleta = (valor: string | null | undefined) => valor
  ? new Intl.DateTimeFormat("pt-BR").format(new Date(`${valor}T12:00:00`))
  : "—";

const mesCurto = (valor: (`${number}-${number}-${number}` | null)) => valor
  ? new Intl.DateTimeFormat("pt-BR", { month: "short", year: "2-digit" })
    .format(new Date(`${valor}T12:00:00`))
    .replace(" de ", "/")
  : "—";

const formatarDataIso = (data: (Date)) => {
  const ano = data.getFullYear();
  const mes = String(data.getMonth() + 1).padStart(2, "0");
  const dia = String(data.getDate()).padStart(2, "0");
  return `${ano}-${mes}-${dia}`;
};

const ontemIso = () => {
  const hoje = obterDataNoFusoNegocio();
  return hoje ? deslocarDataIso(hoje, -1) : "";
};

const deslocarDataIso = (valor: string, dias: (number)) => {
  const data = new Date(`${valor}T12:00:00`);
  if (Number.isNaN(data.getTime())) return valor;
  data.setDate(data.getDate() + dias);
  return formatarDataIso(data);
};

const rotuloPonto = (data: (`${number}-${number}-${number}` | null), periodo: (string)) => ["SEMESTRAL", "ANUAL"].includes(periodo)
  ? mesCurto(data)
  : dataCurta(data);

const chaveComparacao = (data: (`${number}-${number}-${number}` | null), periodo: (string)) => {
  if (!data) return "";
  return ["SEMESTRAL", "ANUAL"].includes(periodo) ? String(data).slice(5, 7) : String(data).slice(5, 10);
};

const encontrarNome = (lista: DTO.PainelEfetivoDTOOpcao[], id: (string), padrao: (string)) => {
  if (!id) return padrao;
  return lista.find((item) => String(item.id) === String(id))?.nome || padrao;
};

const CartaoMetrica = ({ icone: Icone, titulo, valor, detalhe, destaque = false, tipo = "neutro" }: { icone: import("lucide-react").LucideIcon; titulo: string; valor: string; detalhe: string; destaque?: boolean; tipo?: string }) => (
  <article className={`${estilos.cartaoMetrica} ${destaque ? estilos.cartaoPrincipal : ""} ${estilos[`metrica_${tipo}`] || ""}`}>
    <span className={estilos.iconeMetrica}><Icone size={18} aria-hidden="true" /></span>
    <div className={estilos.conteudoMetrica}>
      <span>{titulo}</span>
      <strong>{valor}</strong>
      <small>{detalhe}</small>
    </div>
  </article>
);

const BarraTaxa = ({ valor, alerta = false }: { valor?: number | null; alerta?: boolean }) => {
  const taxa = Number(valor || 0);
  const largura = Math.max(0, Math.min(100, taxa));
  return (
    <div className={`${estilos.taxaCelula} ${alerta ? estilos.taxaAlerta : ""}`}>
      <strong>{percentual(taxa)}</strong>
      <span className={estilos.trilhoTaxa}><i style={{ width: `${largura}%` }} /></span>
    </div>
  );
};

const usePaginacao = <T,>(itens: T[] = [], tamanhoPagina = 8) => {
  const [pagina, setPagina] = useState(1);
  const totalPaginas = Math.max(1, Math.ceil(itens.length / tamanhoPagina));

  useEffect(() => {
    setPagina((atual) => Math.min(atual, totalPaginas));
  }, [totalPaginas]);

  useEffect(() => {
    setPagina(1);
  }, [itens]);

  const itensPagina = useMemo(() => {
    const inicio = (pagina - 1) * tamanhoPagina;
    return itens.slice(inicio, inicio + tamanhoPagina);
  }, [itens, pagina, tamanhoPagina]);

  return { pagina, setPagina, totalPaginas, itensPagina };
};

const PaginacaoQuadro = ({ pagina, totalPaginas, totalItens, aoMudar }: { pagina: number; totalPaginas: number; totalItens: number; aoMudar: React.Dispatch<React.SetStateAction<number>> }) => {
  if (totalPaginas <= 1) return null;
  return (
    <footer className={estilos.paginacaoQuadro}>
      <span>{numero(totalItens)} registro(s)</span>
      <div>
        <button type="button" onClick={() => aoMudar(pagina - 1)} disabled={pagina <= 1} aria-label="Página anterior"><ChevronLeft size={14} /></button>
        <strong>{pagina} / {totalPaginas}</strong>
        <button type="button" onClick={() => aoMudar(pagina + 1)} disabled={pagina >= totalPaginas} aria-label="Próxima página"><ChevronRight size={14} /></button>
      </div>
    </footer>
  );
};

const QuadroContratos = ({ itens = [], selecionado }: { itens?: DTO.PainelEfetivoDTODetalheContrato[]; selecionado?: boolean }) => {
  const { pagina, setPagina, totalPaginas, itensPagina } = usePaginacao(itens, 8);
  return (
    <article className={estilos.quadroDimensao}>
      <header>
        <span><BriefcaseBusiness size={16} aria-hidden="true" /></span>
        <div>
          <h4>Contratos</h4>
          <p>{selecionado ? "Contrato selecionado" : `${itens.length} contrato(s) vigente(s)`}</p>
        </div>
      </header>
      <div className={estilos.tabelaDimensaoWrap}>
        {itens.length ? (
          <table className={estilos.tabelaContrato}>
            <thead>
              <tr>
                <th>Contrato</th><th>Prev.</th><th>Aloc.</th><th>Lanç.</th><th>Pend.</th><th>Pres.</th><th>Atend.</th>
              </tr>
            </thead>
            <tbody>
              {itensPagina.map((item, indice) => (
                <tr key={`${item.id ?? "sem-id"}-${item.nome}-${indice}`} className={item.pendentesLancamento > 0 ? estilos.linhaPendente : ""}>
                  <td title={item.nome ?? undefined}>{item.nome || "Não informado"}</td>
                  <td>{numero(item.efetivoPrevisto)}</td>
                  <td>{numero(item.efetivoAlocado)}</td>
                  <td>{numero(item.registrosLancados)}</td>
                  <td>{numero(item.pendentesLancamento)}</td>
                  <td>{numero(item.presentes)}</td>
                  <td><BarraTaxa valor={item.percentualAtendimento} alerta={item.percentualAtendimento < 100} /></td>
                </tr>
              ))}
            </tbody>
          </table>
        ) : <div className={estilos.vazioDimensao}>Nenhum contrato vigente para os filtros selecionados.</div>}
      </div>
      <PaginacaoQuadro pagina={pagina} totalPaginas={totalPaginas} totalItens={itens.length} aoMudar={setPagina} />
    </article>
  );
};

const QuadroDimensao = ({ titulo, icone: Icone, itens = [], selecionado }: { titulo: string; icone: import("lucide-react").LucideIcon; itens?: DTO.PainelEfetivoDTODetalheDimensao[]; selecionado?: boolean }) => {
  const { pagina, setPagina, totalPaginas, itensPagina } = usePaginacao(itens, 8);
  return (
    <article className={estilos.quadroDimensao}>
      <header>
        <span><Icone size={16} aria-hidden="true" /></span>
        <div>
          <h4>{titulo}</h4>
          <p>{selecionado ? "Filtro específico" : `${itens.length} item(ns) alocado(s)`}</p>
        </div>
      </header>
      <div className={estilos.tabelaDimensaoWrap}>
        {itens.length ? (
          <table>
            <thead>
              <tr><th>Descrição</th><th>Aloc.</th><th>Lanç.</th><th>Pend.</th><th>Pres.</th><th>Cob.</th></tr>
            </thead>
            <tbody>
              {itensPagina.map((item, indice) => (
                <tr key={`${item.id ?? "sem-id"}-${item.nome}-${indice}`} className={item.pendentesLancamento > 0 ? estilos.linhaPendente : ""}>
                  <td title={item.nome ?? undefined}>{item.nome || "Não informado"}</td>
                  <td>{numero(item.efetivoAlocado)}</td>
                  <td>{numero(item.registrosLancados)}</td>
                  <td>{numero(item.pendentesLancamento)}</td>
                  <td>{numero(item.presentes)}</td>
                  <td><BarraTaxa valor={item.percentualCobertura} alerta={item.pendentesLancamento > 0} /></td>
                </tr>
              ))}
            </tbody>
          </table>
        ) : <div className={estilos.vazioDimensao}>Nenhuma alocação vigente encontrada para esta dimensão.</div>}
      </div>
      <PaginacaoQuadro pagina={pagina} totalPaginas={totalPaginas} totalItens={itens.length} aoMudar={setPagina} />
    </article>
  );
};

const PainelItemTodos = ({ item, tipo }: { item: DTO.PainelEfetivoDTODetalheDimensao; tipo: string }) => {
  const contrato = tipo === "Contratos";
  return (
    <article className={estilos.painelItemTodos}>
      <header>
        <span>{tipo}</span>
        <strong title={item.nome ?? undefined}>{item.nome || "Não informado"}</strong>
      </header>
      <div className={estilos.metricasPainelItemTodos}>
        {contrato && <div><span>Previsto</span><strong>{numero(item.efetivoPrevisto)}</strong></div>}
        <div><span>Alocados</span><strong>{numero(item.efetivoAlocado)}</strong></div>
        <div><span>Lançados</span><strong>{numero(item.registrosLancados)}</strong></div>
        <div><span>Presentes</span><strong>{numero(item.presentes)}</strong></div>
        <div><span>Ausentes</span><strong>{numero(item.ausentes)}</strong></div>
        <div className={Number(item.pendentesLancamento || 0) > 0 ? estilos.metricaPainelItemAlerta : ""}>
          <span>Pendentes</span><strong>{numero(item.pendentesLancamento)}</strong>
        </div>
      </div>
      <footer>
        <span>Cobertura <strong>{percentual(item.percentualCobertura)}</strong></span>
        <span>Atendimento <strong>{percentual(item.percentualAtendimento)}</strong></span>
      </footer>
    </article>
  );
};

const GrupoPaineisTodos = ({ titulo, itens = [] }: { titulo: string; itens?: DTO.PainelEfetivoDTODetalheDimensao[] }) => {
  const { pagina, setPagina, totalPaginas, itensPagina } = usePaginacao(itens, 6);
  if (!itens.length) return null;
  return (
    <section className={estilos.grupoPaineisTodos} aria-label={`Painéis separados por ${titulo.toLowerCase()}`}>
      <header className={estilos.cabecalhoGrupoPaineisTodos}>
        <div><span>Seleção Todos</span><h4>{titulo}</h4></div>
        <small>{numero(itens.length)} item(ns)</small>
      </header>
      <div className={estilos.gradePaineisTodos}>
        {itensPagina.map((item, indice) => (
          <PainelItemTodos key={`${titulo}-${item.id ?? item.nome}-${indice}`} item={item} tipo={titulo} />
        ))}
      </div>
      <PaginacaoQuadro pagina={pagina} totalPaginas={totalPaginas} totalItens={itens.length} aoMudar={setPagina} />
    </section>
  );
};

const QuadroCruzado = ({ linhas = [], referencia }: { linhas?: DTO.PainelEfetivoDTOLinhaDimensionada[]; referencia?: string | null }) => {
  const { pagina, setPagina, totalPaginas, itensPagina } = usePaginacao(linhas, 15);
  return (
    <article className={estilos.quadroCruzado}>
      <div className={estilos.tituloQuadroCruzado}>
        <div>
          <h3>Composição operacional do dia</h3>
          <p>Baseada na alocação vigente, inclusive quando a frequência ainda não foi lançada.</p>
        </div>
        <span>{dataCompleta(referencia)} · {linhas.length} combinação(ões)</span>
      </div>
      <div className={estilos.tabelaCruzadaWrap}>
        {linhas.length ? (
          <table>
            <thead>
              <tr>
                <th>Contrato</th><th>Lotação</th><th>Turno</th><th>Cargo</th>
                <th>Alocados</th><th>Lançados</th><th>Pendentes</th><th>Presentes</th><th>Ausentes</th><th>Cobertura</th>
              </tr>
            </thead>
            <tbody>
              {itensPagina.map((item, indice) => (
                <tr
                  key={`${item.contratoId ?? "c"}-${item.lotacaoId ?? "l"}-${item.jornadaId ?? "j"}-${item.cargoId ?? "g"}-${indice}`}
                  className={item.pendentesLancamento > 0 ? estilos.linhaPendente : ""}
                >
                  <td>{item.contrato || "Sem contrato"}</td>
                  <td>{item.lotacao || "Sem lotação"}</td>
                  <td>{item.turno || "Sem turno"}</td>
                  <td>{item.cargo || "Sem cargo"}</td>
                  <td>{numero(item.efetivoAlocado)}</td>
                  <td>{numero(item.registrosLancados)}</td>
                  <td>{numero(item.pendentesLancamento)}</td>
                  <td>{numero(item.presentes)}</td>
                  <td>{numero(item.ausentes)}</td>
                  <td><BarraTaxa valor={item.percentualCobertura} alerta={item.pendentesLancamento > 0} /></td>
                </tr>
              ))}
            </tbody>
          </table>
        ) : <div className={estilos.vazioDimensao}>Nenhuma alocação operacional encontrada para esta data e estes filtros.</div>}
      </div>
      <PaginacaoQuadro pagina={pagina} totalPaginas={totalPaginas} totalItens={linhas.length} aoMudar={setPagina} />
    </article>
  );
};

export const IndicadorEfetivo = ({ referencia, aoAtualizarRelatorio }: { referencia?: string | null; aoAtualizarRelatorio?: (relatorio: StaffingReport | null) => void }) => {
  const [filtros, setFiltros] = useState<NonNullableFilterOptions>({
    contratos: [],
    lotacoes: [],
    turnos: [],
    cargos: [],
    permiteDetalhamentoOperacional: true,
  });
  const [filtrosCarregados, setFiltrosCarregados] = useState(false);
  const [filtrosAtualizando, setFiltrosAtualizando] = useState(false);
  const [periodo, setPeriodo] = useState("MENSAL");
  const [comparar, setComparar] = useState(false);
  const [contratoId, setContratoId] = useState("");
  const [lotacaoId, setLotacaoId] = useState("");
  const [turnoId, setTurnoId] = useState("");
  const [cargoId, setCargoId] = useState("");
  const [referenciaSelecionada, setReferenciaSelecionada] = useState(referencia || ontemIso);
  const [resultado, setResultado] = useState<({ chave: string; consulta: { signal: AbortSignal; atual: () => boolean; }; dados: import("../../../../contracts/java").PainelEfetivoDTO; }) | null>(null);
  const [carregando, setCarregando] = useState(false);
  const [erro, setErro] = useState("");
  const [erroFiltros, setErroFiltros] = useState("");
  const [versaoConsulta, setVersaoConsulta] = useState(0);
  const [leituraOperacionalAberta, setLeituraOperacionalAberta] = useState(false);
  const [abaLeituraOperacional, setAbaLeituraOperacional] = useState("DIMENSOES");
  const controladorFiltrosRef = useRef<AbortController | null>(null);

  const detalhamentoPermitido = filtros.permiteDetalhamentoOperacional !== false;
  const consolidado = !contratoId;
  const hoje = obterDataNoFusoNegocio() || "";

  useEffect(() => {
    if (referencia) setReferenciaSelecionada(referencia);
  }, [referencia]);

  const parametrosFiltros = useMemo(() => ({
    referencia: referenciaSelecionada,
    periodo,
    compararAnoAnterior: comparar,
    ...(contratoId ? { contratoId } : {}),
    ...(detalhamentoPermitido && lotacaoId ? { lotacaoId } : {}),
    ...(detalhamentoPermitido && turnoId ? { jornadaId: turnoId } : {}),
    ...(detalhamentoPermitido && cargoId ? { cargoId } : {}),
  }), [referenciaSelecionada, periodo, comparar, contratoId, detalhamentoPermitido, lotacaoId, turnoId, cargoId]);

  useEffect(() => {
    controladorFiltrosRef.current?.abort();
    const controlador = new AbortController();
    controladorFiltrosRef.current = controlador;
    setFiltrosAtualizando(true);
    setErroFiltros("");

    ServicoPainel.buscarFiltrosEfetivo(parametrosFiltros, { signal: controlador.signal })
      .then((resposta) => {
        if (controlador.signal.aborted) return;
        const proximosFiltros = {
          contratos: Array.isArray(resposta?.contratos) ? resposta.contratos : [],
          lotacoes: Array.isArray(resposta?.lotacoes) ? resposta.lotacoes : [],
          turnos: Array.isArray(resposta?.turnos) ? resposta.turnos : [],
          cargos: Array.isArray(resposta?.cargos) ? resposta.cargos : [],
          permiteDetalhamentoOperacional: resposta?.permiteDetalhamentoOperacional !== false,
        };
        setFiltros(proximosFiltros);

        const contem = (lista: (import("../../../../contracts/java").PainelEfetivoDTOOpcao[]), id: (string)) => !id || lista.some((item) => String(item.id) === String(id));
        setContratoId((atual) => (contem(proximosFiltros.contratos, atual) ? atual : ""));
        setLotacaoId((atual) => (contem(proximosFiltros.lotacoes, atual) ? atual : ""));
        setTurnoId((atual) => (contem(proximosFiltros.turnos, atual) ? atual : ""));
        setCargoId((atual) => (contem(proximosFiltros.cargos, atual) ? atual : ""));
      })
      .catch((falhaOriginal: unknown) => {
      const falha = observarFalha(falhaOriginal);

        if (!controlador.signal.aborted) {
          setErroFiltros(mensagemFalha(falha, "Não foi possível carregar os filtros do indicador."));
        }
      })
      .finally(() => {
        if (!controlador.signal.aborted) {
          setFiltrosCarregados(true);
          setFiltrosAtualizando(false);
        }
      });

    return () => controlador.abort();
  }, [parametrosFiltros]);

  useEffect(() => {
    if (!detalhamentoPermitido) {
      setLotacaoId("");
      setTurnoId("");
      setCargoId("");
      setAbaLeituraOperacional("DIMENSOES");
    }
  }, [detalhamentoPermitido]);

  const parametros = useMemo(() => ({
    referencia: referenciaSelecionada,
    periodo,
    compararAnoAnterior: comparar,
    ...(contratoId ? { contratoId } : {}),
    ...(detalhamentoPermitido && lotacaoId ? { lotacaoId } : {}),
    ...(detalhamentoPermitido && turnoId ? { jornadaId: turnoId } : {}),
    ...(detalhamentoPermitido && cargoId ? { cargoId } : {}),
  }), [referenciaSelecionada, periodo, comparar, contratoId, detalhamentoPermitido, lotacaoId, turnoId, cargoId]);

  const chaveConsulta = useMemo(() => JSON.stringify([parametros, versaoConsulta]), [parametros, versaoConsulta]);
  const consultasIndicador = useConsultaVigente(chaveConsulta);
  // A guarda na renderização cobre também o intervalo anterior ao debounce.
  // Um resultado de outra consulta nunca recebe os rótulos nem a exportação atuais.
  const dados = resultado?.chave === chaveConsulta && resultado.consulta.atual() ? resultado.dados : null;

  useEffect(() => {
    if (!filtrosCarregados) return undefined;

    const consulta = consultasIndicador.iniciar();
    setCarregando(true);
    setErro("");

    const timer = setTimeout(async () => {
      try {
        const resposta = await ServicoPainel.buscarIndicadorEfetivo(parametros, { signal: consulta.signal });
        if (consulta.atual()) setResultado({ chave: chaveConsulta, consulta, dados: resposta });
      } catch (falhaOriginal) {
      const falha = observarFalha(falhaOriginal);

        if (consulta.atual() && !falha?.canceled) {
          setErro(mensagemFalha(falha, "Não foi possível calcular o indicador de efetivo."));
        }
      } finally {
        if (consulta.atual()) setCarregando(false);
      }
    }, 220);

    return () => {
      clearTimeout(timer);
      consultasIndicador.invalidar();
    };
  }, [parametros, filtrosCarregados, chaveConsulta, consultasIndicador]);

  const serieGrafico = useMemo(() => {
    const atual = Array.isArray(dados?.serieAtual) ? dados.serieAtual : [];
    const anterior = Array.isArray(dados?.serieAnoAnterior) ? dados.serieAnoAnterior : [];
    const mapaAnterior = new Map<string, PainelEfetivoDTOPontoDiario>(anterior.map((ponto) => [chaveComparacao(ponto.data, periodo), ponto]));
    return atual.map((ponto) => ({
      ...ponto,
      rotulo: rotuloPonto(ponto.data, periodo),
      presentesAnoAnterior: mapaAnterior.get(chaveComparacao(ponto.data, periodo))?.presentes ?? null,
    }));
  }, [dados, periodo]);

  const resumo: Partial<DTO.PainelEfetivoDTOResumo> = dados?.resumoAtual || {};
  const resumoAnterior = dados?.resumoAnoAnterior || null;
  const contratoNome = encontrarNome(filtros.contratos, contratoId, "Todos os contratos");
  const escopoSelecionado = useMemo(() => [
    contratoNome,
    encontrarNome(filtros.lotacoes, lotacaoId, "Todas as lotações"),
    encontrarNome(filtros.turnos, turnoId, "Todos os turnos"),
    encontrarNome(filtros.cargos, cargoId, "Todos os cargos"),
  ].join(" · "), [filtros, contratoNome, lotacaoId, turnoId, cargoId]);

  const intervalo = dados?.inicio && dados?.fim
    ? `${dataCurta(dados.inicio)} a ${dataCurta(dados.fim)}`
    : "—";
  const periodoRotulo = PERIODOS.find(([id]) => id === periodo)?.[1] || periodo;

  const contextoExportacao = useMemo(() => ({
    referencia: referenciaSelecionada,
    escopoSelecionado,
    periodoRotulo,
    comparar,
    contratoNome,
  }), [referenciaSelecionada, escopoSelecionado, periodoRotulo, comparar, contratoNome]);

  useLayoutEffect(() => {
    aoAtualizarRelatorio?.(dados ? { dados, contexto: contextoExportacao } : null);
  }, [dados, contextoExportacao, aoAtualizarRelatorio]);

  const mudarDia = (deslocamento: (-1) | (number)) => {
    setReferenciaSelecionada((atual) => {
      const proxima = deslocarDataIso(atual || ontemIso(), deslocamento);
      return proxima > hoje ? hoje : proxima;
    });
  };

  const selecionarContrato = (valor: (string)) => {
    setContratoId(valor);
    setLotacaoId("");
    setTurnoId("");
    setCargoId("");
  };

  const selecionarLotacao = (valor: (string)) => {
    setLotacaoId(valor);
    setTurnoId("");
    setCargoId("");
  };

  const selecionarTurno = (valor: (string)) => {
    setTurnoId(valor);
    setCargoId("");
  };

  const dispersaoGerencial = useMemo(() => {
    if (serieGrafico.length >= 3) {
      return { pontos: serieGrafico, contexto: "períodos da série atual" };
    }

    const normalizarDimensao = (itens: DTO.PainelEfetivoDTODetalheDimensao[] | null | undefined = [], dimensao: string) => (itens ?? [])
      .filter((item) => Number(item.efetivoAlocado || 0) > 0)
      .map((item, indice) => ({
        ...item,
        rotulo: item.nome || `${dimensao} ${indice + 1}`,
        dimensao,
      }));

    if (!contratoId) {
      return {
        pontos: normalizarDimensao(dados?.detalhamentoContratos, "Contrato"),
        contexto: "contratos do escopo atual",
      };
    }
    if (detalhamentoPermitido && !lotacaoId) {
      return {
        pontos: normalizarDimensao(dados?.detalhamentoLotacoes, "Lotação"),
        contexto: "lotações do contrato",
      };
    }
    if (detalhamentoPermitido && !turnoId) {
      return {
        pontos: normalizarDimensao(dados?.detalhamentoTurnos, "Turno"),
        contexto: "turnos da lotação",
      };
    }
    if (detalhamentoPermitido && !cargoId) {
      return {
        pontos: normalizarDimensao(dados?.detalhamentoCargos, "Cargo"),
        contexto: "cargos do escopo filtrado",
      };
    }
    return { pontos: serieGrafico, contexto: "períodos da série atual" };
  }, [serieGrafico, dados, contratoId, detalhamentoPermitido, lotacaoId, turnoId, cargoId]);

  const dimensoesOperacionais = detalhamentoPermitido ? [
    { id: "lotacoes", titulo: "Lotações", icone: Building2, itens: dados?.detalhamentoLotacoes || [], selecionado: Boolean(lotacaoId) },
    { id: "turnos", titulo: "Turnos", icone: Clock3, itens: dados?.detalhamentoTurnos || [], selecionado: Boolean(turnoId) },
    { id: "cargos", titulo: "Cargos", icone: Users, itens: dados?.detalhamentoCargos || [], selecionado: Boolean(cargoId) },
  ] : [];

  const temPendencia = Number(resumo.pendentesLancamento || 0) > 0;
  const temDeficit = Number(resumo.deficitContratual || 0) > 0;
  const temExcedente = Number(resumo.excedenteContratual || 0) > 0;
  const exibirPendentesNoGrafico = serieGrafico.some((ponto) => Number(ponto.pendentesLancamento || 0) > 0);
  const gruposTodos = [
    ...(!contratoId ? [{ titulo: "Contratos", itens: dados?.detalhamentoContratos || [] }] : []),
    ...(detalhamentoPermitido && !lotacaoId ? [{ titulo: "Lotações", itens: dados?.detalhamentoLotacoes || [] }] : []),
    ...(detalhamentoPermitido && !turnoId ? [{ titulo: "Turnos", itens: dados?.detalhamentoTurnos || [] }] : []),
    ...(detalhamentoPermitido && !cargoId ? [{ titulo: "Cargos", itens: dados?.detalhamentoCargos || [] }] : []),
  ].filter((grupo) => grupo.itens.length > 0);

  return (
    <section className={estilos.indicador} aria-label="Indicador contratual de efetivo">
      <header className={estilos.cabecalho}>
        <div className={estilos.tituloPrincipal}>
          <span className={estilos.selo}><BarChart3 size={14} /> Controle de efetivo</span>
          <h2>Efetivo operacional e execução contratual</h2>
          <p>Compare o previsto em contrato com o efetivo operacional, presentes e ausentes. Pendências de lançamento aparecem somente quando exigem atenção.</p>
        </div>
        <div className={estilos.escopoAtual}>
          <span>{consolidado ? "Visão consolidada" : "Contrato selecionado"}</span>
          <strong>{contratoNome}</strong>
          <small>{dataCompleta(referenciaSelecionada)}</small>
        </div>
      </header>

      <div className={estilos.painelFiltros}>
        <div className={estilos.filtrosPrincipais}>
          <label>
            <span>Contrato</span>
            <select value={contratoId} onChange={(e) => selecionarContrato(e.target.value)} disabled={!filtrosCarregados || filtrosAtualizando}>
              <option value="">Todos os contratos</option>
              {filtros.contratos.map((item) => <option key={item.id} value={(item.id) ?? undefined}>{item.nome}</option>)}
            </select>
          </label>
          <label>
            <span>Lotação</span>
            <select value={lotacaoId} onChange={(e) => selecionarLotacao(e.target.value)} disabled={!filtrosCarregados || filtrosAtualizando || !detalhamentoPermitido}>
              <option value="">Todas as lotações</option>
              {filtros.lotacoes.map((item) => <option key={item.id} value={(item.id) ?? undefined}>{item.nome}</option>)}
            </select>
          </label>
          <label>
            <span>Turno</span>
            <select value={turnoId} onChange={(e) => selecionarTurno(e.target.value)} disabled={!filtrosCarregados || filtrosAtualizando || !detalhamentoPermitido}>
              <option value="">Todos os turnos</option>
              {filtros.turnos.map((item) => <option key={item.id} value={(item.id) ?? undefined}>{item.nome}</option>)}
            </select>
          </label>
          <label>
            <span>Cargo</span>
            <select value={cargoId} onChange={(e) => setCargoId(e.target.value)} disabled={!filtrosCarregados || filtrosAtualizando || !detalhamentoPermitido}>
              <option value="">Todos os cargos</option>
              {filtros.cargos.map((item) => <option key={item.id} value={(item.id) ?? undefined}>{item.nome}</option>)}
            </select>
          </label>
        </div>

        <div className={estilos.ferramentas}>
          <div className={estilos.periodos} aria-label="Período do gráfico">
            <span><CalendarDays size={14} /> Período</span>
            <div>{PERIODOS.map(([id, rotulo]) => (
              <button key={id} type="button" className={periodo === id ? estilos.ativo : ""} onClick={() => setPeriodo(id)}>{rotulo}</button>
            ))}</div>
          </div>

          <div className={estilos.navegacaoData}>
            <span>Referência</span>
            <button type="button" onClick={() => mudarDia(-1)} title="Dia anterior" aria-label="Dia anterior"><ChevronLeft size={16} /></button>
            <input
              type="date"
              value={referenciaSelecionada || ""}
              max={hoje}
              onChange={(e) => e.target.value && setReferenciaSelecionada(e.target.value)}
              aria-label="Data de referência do indicador"
            />
            <button type="button" onClick={() => mudarDia(1)} disabled={referenciaSelecionada >= hoje} title="Próximo dia" aria-label="Próximo dia"><ChevronRight size={16} /></button>
          </div>

          <button type="button" className={`${estilos.botaoComparar} ${comparar ? estilos.ativo : ""}`} onClick={() => setComparar((atual) => !atual)}>
            {comparar ? "Comparando ano anterior" : "Comparar ano anterior"}
          </button>

          <button
            type="button"
            className={estilos.botaoAtualizar}
            onClick={() => setVersaoConsulta((valor) => valor + 1)}
            disabled={carregando || !filtrosCarregados}
            title="Atualizar indicador"
            aria-label="Atualizar indicador"
          >
            <RefreshCw size={16} className={carregando ? estilos.girando : ""} />
          </button>

          <div className={estilos.exportacoesIndicador}>
            <button type="button" className={estilos.botaoPdf} onClick={() => exportarIndicadorEfetivoPdf(dados, contextoExportacao)} disabled={!dados || carregando} title="Exportar indicador em PDF">
              <FileText size={19} aria-hidden="true" /> PDF
            </button>
            <button type="button" className={estilos.botaoExcel} onClick={() => exportarIndicadorEfetivoXlsx(dados, contextoExportacao)} disabled={!dados || carregando} title="Exportar indicador em Excel">
              <FileSpreadsheet size={16} aria-hidden="true" /> XLSX
            </button>
          </div>
        </div>
      </div>

      {!detalhamentoPermitido && (
        <div className={estilos.avisoEscopo}>Seu perfil visualiza o consolidado autorizado e os contratos, sem exposição de lotação, turno ou cargo.</div>
      )}
      {erroFiltros && <div className={estilos.erro} role="alert">{erroFiltros}</div>}
      {erro && <div className={estilos.erro}>{erro}</div>}

      <div className={estilos.metricas} aria-busy={carregando}>
        <CartaoMetrica
          icone={BriefcaseBusiness}
          titulo="Previsto em contrato"
          valor={carregando && !dados ? "…" : numero(resumo.efetivoPrevisto)}
          detalhe="dimensionamento contratual vigente"
          destaque
          tipo="contrato"
        />
        <CartaoMetrica
          icone={Users}
          titulo="Efetivo operacional"
          valor={carregando && !dados ? "…" : numero(resumo.efetivoAlocado)}
          detalhe={temDeficit ? `${numero(resumo.deficitContratual)} abaixo do previsto` : temExcedente ? `${numero(resumo.excedenteContratual)} acima do previsto` : "aderente ao previsto"}
          tipo="principal"
        />
        <CartaoMetrica
          icone={CheckCircle2}
          titulo="Presentes"
          valor={carregando && !dados ? "…" : numero(resumo.presentes)}
          detalhe={`${percentual(resumo.percentualAtendimento)} de atendimento sobre o previsto`}
          tipo="positivo"
        />
        <CartaoMetrica
          icone={UserMinus}
          titulo="Ausentes"
          valor={carregando && !dados ? "…" : numero(resumo.ausentes)}
          detalhe="classificação consolidada pelo servidor"
          tipo="critico"
        />
      </div>

      {temPendencia && (
        <div className={`${estilos.statusOperacional} ${estilos.statusAtencao}`}>
          <span className={estilos.iconeStatus}><AlertTriangle size={17} aria-hidden="true" /></span>
          <div>
            <strong>{numero(resumo.pendentesLancamento)} lançamento(s) pendente(s)</strong>
            <span>Há empregado(s) do efetivo operacional sem presença ou ausência registrada em {dataCompleta(referenciaSelecionada)}.</span>
          </div>
          <button type="button" onClick={() => setLeituraOperacionalAberta(true)}>Ver pendências</button>
        </div>
      )}

      <div className={`${estilos.corpo} ${comparar ? estilos.corpoComComparacao : ""}`}>
        <VisualizadorEfetivo
          serie={serieGrafico}
          pontosDispersao={dispersaoGerencial.pontos}
          contextoDispersao={dispersaoGerencial.contexto}
          resumo={resumo}
          comparar={comparar}
          exibirPendentes={exibirPendentesNoGrafico}
          carregando={carregando}
          escopoSelecionado={escopoSelecionado}
          periodoRotulo={periodoRotulo}
          intervalo={intervalo}
        />

        {comparar && (
          <aside className={estilos.comparativoCard}>
            <div className={estilos.tituloComparativo}>
              <div>
                <h3>Referência × ano anterior</h3>
                <p>Mesma posição de calendário</p>
              </div>
              <ClipboardCheck size={19} aria-hidden="true" />
            </div>
            <table>
              <thead><tr><th>Indicador</th><th>Atual</th><th>Anterior</th></tr></thead>
              <tbody>
                <tr><td>Previsto em contrato</td><td>{numero(resumo.efetivoPrevisto)}</td><td>{resumoAnterior ? numero(resumoAnterior.efetivoPrevisto) : "—"}</td></tr>
                <tr><td>Efetivo operacional</td><td>{numero(resumo.efetivoAlocado)}</td><td>{resumoAnterior ? numero(resumoAnterior.efetivoAlocado) : "—"}</td></tr>
                <tr><td>Presentes</td><td>{numero(resumo.presentes)}</td><td>{resumoAnterior ? numero(resumoAnterior.presentes) : "—"}</td></tr>
                <tr><td>Ausentes</td><td>{numero(resumo.ausentes)}</td><td>{resumoAnterior ? numero(resumoAnterior.ausentes) : "—"}</td></tr>
                <tr><td>Presença sobre o previsto</td><td>{percentual(resumo.percentualAtendimento)}</td><td>{resumoAnterior ? percentual(resumoAnterior.percentualAtendimento) : "—"}</td></tr>
                {(temPendencia || Number(resumoAnterior?.pendentesLancamento || 0) > 0) && (
                  <tr><td>Pendentes de lançamento</td><td>{numero(resumo.pendentesLancamento)}</td><td>{resumoAnterior ? numero(resumoAnterior.pendentesLancamento) : "—"}</td></tr>
                )}
              </tbody>
            </table>
          </aside>
        )}
      </div>

      {gruposTodos.length > 0 && (
        <section className={estilos.paineisTodos} aria-label="Painéis separados para filtros com seleção Todos">
          <div className={estilos.tituloPaineisTodos}>
            <span>Desdobramento da seleção Todos</span>
            <h3>O mesmo recorte da API, separado por item</h3>
            <p>Os valores e percentuais abaixo são exibidos exatamente como retornados pelo Java; o navegador não recalcula indicadores gerenciais.</p>
          </div>
          {gruposTodos.map((grupo) => <GrupoPaineisTodos key={grupo.titulo} {...grupo} />)}
        </section>
      )}

      <section className={estilos.chamadaLeitura} aria-label="Acesso à leitura operacional detalhada">
        <div className={estilos.chamadaLeituraTitulo}>
          <span>Leitura operacional</span>
          <h3>Investigue as divergências sem perder o foco do painel</h3>
          <p>O detalhamento por contrato, lotação, turno e cargo foi concentrado em uma janela própria.</p>
        </div>
        <div className={estilos.resumoLeitura}>
          <div><span>Previsto</span><strong>{numero(resumo.efetivoPrevisto)}</strong></div>
          <div><span>Operacional</span><strong>{numero(resumo.efetivoAlocado)}</strong></div>
          <div><span>Presentes</span><strong>{numero(resumo.presentes)}</strong></div>
          <div><span>Ausentes</span><strong>{numero(resumo.ausentes)}</strong></div>
        </div>
        <button type="button" className={estilos.botaoAbrirLeitura} onClick={() => setLeituraOperacionalAberta(true)}>
          <ClipboardCheck size={17} aria-hidden="true" /> Abrir leitura operacional <ChevronRight size={16} aria-hidden="true" />
        </button>
      </section>

      <ModalPainel
        aberto={leituraOperacionalAberta}
        titulo="Leitura operacional do efetivo"
        detalhe={`${escopoSelecionado} · ${dataCompleta(referenciaSelecionada)}`}
        selo="Detalhamento operacional"
        aoFechar={() => setLeituraOperacionalAberta(false)}
      >
        <div className={estilos.modalLeituraOperacional}>
          <section className={estilos.resumoModalLeitura} aria-label="Resumo da leitura operacional">
            <div><span>Previsto em contrato</span><strong>{numero(resumo.efetivoPrevisto)}</strong><small>dimensionamento vigente</small></div>
            <div><span>Alocados</span><strong>{numero(resumo.efetivoAlocado)}</strong><small>{temDeficit ? `${numero(resumo.deficitContratual)} abaixo do previsto` : "base operacional"}</small></div>
            <div><span>Registros realizados</span><strong>{numero(resumo.registrosLancados)}</strong><small>{percentual(resumo.percentualCobertura)} dos alocados com registro</small></div>
            <div className={temPendencia ? estilos.cartaoModalAlerta : ""}><span>Pendentes do operador</span><strong>{numero(resumo.pendentesLancamento)}</strong><small>alocados sem presença/ausência</small></div>
            <div><span>Presentes</span><strong>{numero(resumo.presentes)}</strong><small>{percentual(resumo.percentualAtendimento)} do previsto</small></div>
            <div><span>Ausentes</span><strong>{numero(resumo.ausentes)}</strong><small>{numero(resumo.outrosRegistros)} em outros status</small></div>
          </section>

          {(temPendencia || temDeficit || temExcedente) && (
            <section className={estilos.alertasModalLeitura} aria-label="Alertas da leitura operacional">
              {temPendencia && <div><AlertTriangle size={16} /><span><strong>{numero(resumo.pendentesLancamento)} lançamento(s) pendente(s)</strong>Empregado(s) alocado(s) ainda sem presença ou ausência registrada.</span></div>}
              {temDeficit && <div><BriefcaseBusiness size={16} /><span><strong>{numero(resumo.deficitContratual)} vaga(s) sem alocação</strong>Diferença entre o previsto nos contratos e a alocação vigente.</span></div>}
              {temExcedente && <div><Users size={16} /><span><strong>{numero(resumo.excedenteContratual)} alocado(s) acima do previsto</strong>Revise o dimensionamento contratual ou os vínculos vigentes.</span></div>}
            </section>
          )}

          <nav className={estilos.abasLeituraOperacional} aria-label="Seções da leitura operacional">
            <button type="button" className={abaLeituraOperacional === "DIMENSOES" ? estilos.abaAtiva : ""} onClick={() => setAbaLeituraOperacional("DIMENSOES")}>
              <Building2 size={16} aria-hidden="true" /> Por dimensão
            </button>
            {detalhamentoPermitido && (
              <button type="button" className={abaLeituraOperacional === "COMPOSICAO" ? estilos.abaAtiva : ""} onClick={() => setAbaLeituraOperacional("COMPOSICAO")}>
                <ClipboardCheck size={16} aria-hidden="true" /> Composição do dia
              </button>
            )}
          </nav>

          {abaLeituraOperacional === "DIMENSOES" ? (
            <section className={estilos.dimensionamentoModal} aria-label="Quadros dimensionados do efetivo">
              <div className={estilos.tituloDimensionamento}>
                <div>
                  <span>Onde está a diferença</span>
                  <h3>Previsto, alocação e lançamentos por dimensão</h3>
                  <p>Contratos mostram o previsto; lotação, turno e cargo mostram a alocação vigente e os lançamentos faltantes.</p>
                </div>
                <strong>{dataCompleta(referenciaSelecionada)}</strong>
              </div>
              <div className={estilos.gradeDimensoes}>
                <QuadroContratos itens={dados?.detalhamentoContratos || []} selecionado={Boolean(contratoId)} />
                {dimensoesOperacionais.map((dimensao) => <QuadroDimensao key={dimensao.id} {...dimensao} />)}
              </div>
            </section>
          ) : (
            detalhamentoPermitido && <QuadroCruzado linhas={dados?.quadroDimensionado || []} referencia={referenciaSelecionada} />
          )}
        </div>
      </ModalPainel>
    </section>
  );
};
