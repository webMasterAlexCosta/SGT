import React, { useState, useEffect, useMemo, useRef } from "react";
import {
  Search,
  Filter,
  Users,
  Calendar,
  Info,
  Briefcase,
  Target,
  UserX,
  Stethoscope,
  AlertOctagon,
  TrendingDown,
  TrendingUp,
  ChevronDown,
  PieChart as PieChartIcon,
  BarChart as BarChartIcon,
  Activity,
  ShieldAlert,
  HelpCircle,
  Building2,
  Lock,
  Unlock,
  Clock,
} from "lucide-react";

import {
  ServicoAbsenteismo,
  INDICADORES_ABSENTEISMO_VAZIOS,
} from "../../../../services/rh/ServicoAbsenteismo.js";
import { notificar } from "../../../../core/events";
import { FaFileExcel } from "react-icons/fa";
import { BiLogoAdobe } from "react-icons/bi";

import {
  PieChart,
  Pie,
  Cell,
  BarChart,
  Bar,
  XAxis,
  YAxis,
  CartesianGrid,
  Tooltip as RechartsTooltip,
  Legend,
  ResponsiveContainer,
  LabelList,
} from "recharts";
import { CONFIG } from "../../../../core/config";
import { useExportacaoRelatorio } from "../../../../core/exportacao/context";
import { normalizarAvisoRevisaoRelatorio } from "../../../../services/rh/revisaoRelatorio.js";

import estilos from "./index.module.css";

export const PaginaRelatorioAbsenteismo = () => {
  const { iniciarExportacao } = useExportacaoRelatorio();
  const dataAtual = new Date();
  const anoAtual = dataAtual.getFullYear();
  const mesAtual = dataAtual.getMonth() + 1;

  const [mes, setMes] = useState(mesAtual);
  const [ano, setAno] = useState(anoAtual);

  const [contratos, setContratos] = useState([]);
  const [lotacoes, setLotacoes] = useState([]);
  const [turnos, setTurnos] = useState([]);

  const [contratoSelecionado, setContratoSelecionado] = useState("");
  const [lotacaoSelecionada, setLotacaoSelecionada] = useState("");
  const [turnoSelecionado, setTurnoSelecionado] = useState("");

  const [meta, setMeta] = useState(CONFIG.ABSENTEISMO_DEFAULT_META);
  const [metaBloqueada, setMetaBloqueada] = useState(true);

  const [dados, setDados] = useState([]);
  const [indicadores, setIndicadores] = useState(() => ({ ...INDICADORES_ABSENTEISMO_VAZIOS }));
  const [carregando, setCarregando] = useState(false);
  const [carregandoResumo, setCarregandoResumo] = useState(false);
  const [resumoDisponivel, setResumoDisponivel] = useState(false);
  const [revisao, setRevisao] = useState(() => normalizarAvisoRevisaoRelatorio(null));
  const [paginacao, setPaginacao] = useState({
    pagina: 0, tamanho: 100, totalPaginas: 0, totalColaboradores: 0, primeira: true, ultima: true,
  });

  const resumoRequisicaoRef = useRef(0);

  const lotacoesDisponiveis = useMemo(
    () => [...lotacoes].sort((a, b) => String(a?.nome || "").localeCompare(String(b?.nome || ""), "pt-BR")),
    [lotacoes]
  );

  const turnosDisponiveis = useMemo(() => {
    const lista = !lotacaoSelecionada
      ? turnos
      : lotacoesDisponiveis.find((item) => String(item.id) === String(lotacaoSelecionada))?.turnos || [];
    return [...lista].sort((a, b) => String(a?.descricao || "").localeCompare(String(b?.descricao || ""), "pt-BR"));
  }, [lotacaoSelecionada, lotacoesDisponiveis, turnos]);

  useEffect(() => {
    setLotacaoSelecionada("");
    setTurnoSelecionado("");
  }, [contratoSelecionado]);

  useEffect(() => {
    setTurnoSelecionado("");
  }, [lotacaoSelecionada]);

  useEffect(() => {
    const carregarFiltros = async () => {
      try {
        const filtros = await ServicoAbsenteismo.carregarFiltros();
        const contratosRecebidos = Array.isArray(filtros?.contratos) ? filtros.contratos : [];
        const lotacoesRecebidas = Array.isArray(filtros?.lotacoes) ? filtros.lotacoes : [];
        const turnosRecebidos = Array.isArray(filtros?.turnos) ? filtros.turnos : [];
        setContratos(
          [...contratosRecebidos]
            .filter((item) => item?.ativo !== false)
            .sort((a, b) => String(a?.numeroContrato || a?.descricao || "").localeCompare(
              String(b?.numeroContrato || b?.descricao || ""), "pt-BR"
            ))
        );
        setContratoSelecionado("");
        setLotacoes(lotacoesRecebidas);
        setTurnos(turnosRecebidos);
      } catch (erro) {
        console.error("Erro ao buscar filtros", erro);
        notificar("error", "Não foi possível carregar as listas de filtro.");
      }
    };

    carregarFiltros();
  }, []);

  const parametrosRelatorio = (pagina = 0, tamanho = paginacao.tamanho) => ({
    mes,
    ano,
    ...(contratoSelecionado ? { contratoId: contratoSelecionado } : {}),
    ...(lotacaoSelecionada ? { lotacaoId: lotacaoSelecionada } : {}),
    ...(turnoSelecionado ? { jornadaId: turnoSelecionado } : {}),
    page: pagina,
    size: tamanho,
  });

  const carregarPagina = async (pagina = 0, carregarResumo = false, tamanhoSolicitado = paginacao.tamanho) => {
    setCarregando(true);
    try {
      const resultado = await ServicoAbsenteismo.gerarRelatorio(parametrosRelatorio(pagina, tamanhoSolicitado));
      const dadosRecebidos = Array.isArray(resultado?.dados) ? resultado.dados : [];
      const paginacaoRecebida = resultado?.paginacao && typeof resultado.paginacao === "object"
        ? resultado.paginacao
        : { pagina: 0, tamanho: paginacao.tamanho, totalPaginas: 0, totalColaboradores: 0, primeira: true, ultima: true };
      setDados(dadosRecebidos);
      setPaginacao(paginacaoRecebida);
      setRevisao(normalizarAvisoRevisaoRelatorio(resultado?.revisao));
      if (dadosRecebidos.length === 0 && pagina === 0) {
        notificar("warning", "Nenhum dado encontrado para os filtros selecionados.");
      }
      if (carregarResumo && Number(paginacaoRecebida.totalColaboradores || 0) > 0) {
        const requisicaoResumo = ++resumoRequisicaoRef.current;
        setCarregandoResumo(true);
        setResumoDisponivel(false);
        ServicoAbsenteismo.gerarResumo({
          mes,
          ano,
          ...(contratoSelecionado ? { contratoId: contratoSelecionado } : {}),
          ...(lotacaoSelecionada ? { lotacaoId: lotacaoSelecionada } : {}),
          ...(turnoSelecionado ? { jornadaId: turnoSelecionado } : {}),
          limiteDisrupcao: CONFIG.ABSENTEISMO_DISRUPCAO_THRESHOLD,
        }).then((resumo) => {
          if (resumoRequisicaoRef.current !== requisicaoResumo) return;
          setIndicadores(resumo);
          setRevisao(normalizarAvisoRevisaoRelatorio(resumo?.revisao));
          setResumoDisponivel(true);
        }).catch((erro) => {
          if (resumoRequisicaoRef.current !== requisicaoResumo) return;
          console.error("Falha ao calcular resumo nacional de absenteísmo", erro);
          setResumoDisponivel(false);
          notificar("warning", "A tabela foi carregada, mas os indicadores gerais não puderam ser calculados.");
        }).finally(() => {
          if (resumoRequisicaoRef.current === requisicaoResumo) setCarregandoResumo(false);
        });
      } else if (carregarResumo) {
        ++resumoRequisicaoRef.current;
        setCarregandoResumo(false);
        setResumoDisponivel(false);
      }
    } catch (erro) {
      console.error(erro);
      notificar("error", "Erro ao processar relatório no servidor.");
    } finally {
      setCarregando(false);
    }
  };

  const gerarRelatorio = async () => {
    const validacao = ServicoAbsenteismo.validarFiltros({
      mes,
      ano,
      contratoId: contratoSelecionado,
      referencia: dataAtual,
    });
    if (!validacao.valido) {
      notificar("warning", validacao.mensagem);
      return;
    }
    ++resumoRequisicaoRef.current;
    setDados([]);
    setIndicadores({ ...INDICADORES_ABSENTEISMO_VAZIOS });
    setRevisao(normalizarAvisoRevisaoRelatorio(null));
    setResumoDisponivel(false);
    await carregarPagina(0, true);
  };

  const parametrosExportacao = () => ({
    mes,
    ano,
    ...(contratoSelecionado ? { contratoId: contratoSelecionado } : {}),
    ...(lotacaoSelecionada ? { lotacaoId: lotacaoSelecionada } : {}),
    ...(turnoSelecionado ? { jornadaId: turnoSelecionado } : {}),
    meta,
    limiteDisrupcao: CONFIG.ABSENTEISMO_DISRUPCAO_THRESHOLD,
  });

  const exportarPlanilhaExcel = async () => {
    if (paginacao.totalColaboradores <= 0) return;
    try {
      await iniciarExportacao({
        tipo: "absenteismo",
        parametros: parametrosExportacao(),
        formato: "XLSX",
        totalEstimado: paginacao.totalColaboradores,
      });
    } catch (erro) {
      console.error("Falha ao iniciar XLSX de absenteísmo", erro);
    }
  };

  const exportarDocumentoPDF = async () => {
    if (paginacao.totalColaboradores <= 0) return;
    try {
      await iniciarExportacao({
        tipo: "absenteismo",
        parametros: parametrosExportacao(),
        formato: "PDF",
        totalEstimado: paginacao.totalColaboradores,
      });
    } catch (erro) {
      console.error("Falha ao iniciar PDF de absenteísmo", erro);
    }
  };

  const metaAlcancada = resumoDisponivel ? indicadores.taxaGeral <= meta : null;

  const obterEmblemaRisco = (taxaAtual) => {
    if (taxaAtual === 0)
      return (
        <span className={estilos.emblemaRiscoExcelente}>
          <div className={estilos.pontoVerde}></div> Excelente
        </span>
      );
    if (taxaAtual <= meta)
      return (
        <span className={estilos.emblemaRiscoNaMeta}>
          <div className={estilos.pontoAmarelo}></div> Na Meta
        </span>
      );
    if (taxaAtual <= meta * 2)
      return (
        <span className={estilos.emblemaRiscoAcima}>
          <div className={estilos.pontoBranco}></div> Acima
        </span>
      );
    return (
      <span className={estilos.emblemaRiscoCritico}>
        <div className={estilos.pontoBrancoPisca}></div> Crítico
      </span>
    );
  };

  const obterEmblemaDisrupcao = (pontuacao) => {
    if (pontuacao === 0)
      return <span className={estilos.disrupcaoVazia}>-</span>;
    if (pontuacao < CONFIG.ABSENTEISMO_DISRUPCAO_THRESHOLD)
      return <span className={estilos.disrupcaoNormal}>{pontuacao}</span>;
    if (pontuacao < CONFIG.ABSENTEISMO_DISRUPCAO_THRESHOLD * 2)
      return (
        <span className={estilos.disrupcaoAtencao}>
          <ShieldAlert size={14} /> {pontuacao}
        </span>
      );
    return (
      <span className={estilos.disrupcaoCritica}>
        <ShieldAlert size={14} /> {pontuacao}
      </span>
    );
  };

  const dadosGraficoPizza = [
    { name: "Faltas", value: indicadores.faltas, color: "#e11d48" },
    { name: "Atestados", value: indicadores.atestados, color: "#00416B" },
  ].filter((d) => d.value > 0);

  const dadosGraficoBarras = (indicadores.maioresTaxas || [])
    .filter((d) => d.taxaIndividual > 0)
    .slice(0, 10)
    .map((d) => ({
      nome: String(d.nome || "-").split(" ").slice(0, 2).join(" "),
      Taxa: d.taxaIndividual,
    }));

  return (
    <div className={estilos.containerGeral}>
      <header className={estilos.cabecalho}>
        <div className={estilos.identificacaoCabecalho}>
          <div className={estilos.iconeTituloCabecalho}><Activity size={25} aria-hidden="true" /></div>
          <div>
            <span className={estilos.seloCabecalho}><BarChartIcon size={13} aria-hidden="true" /> Inteligência de pessoas</span>
            <h1 className={estilos.tituloPrincipal}>Indicadores de Absenteísmo</h1>
            <p className={estilos.subtitulo}>
              Acompanhe ausências, taxa operacional, concentração de risco e índice de disrupção com leitura rápida do período.
            </p>
          </div>
        </div>

        <div className={estilos.exportacoesCabecalho} aria-label="Exportar relatório de absenteísmo">
          <span>Exportar relatório</span>
          <div className={estilos.botoesExportacaoDireta}>
            <button
              type="button"
              onClick={exportarPlanilhaExcel}
              disabled={paginacao.totalColaboradores <= 0}
              className={`${estilos.botaoExportacaoDireta} ${estilos.botaoExcelDireto}`}
              title="Exportar relatório de absenteísmo em Excel (.xlsx)"
            >
              <FaFileExcel size={20} aria-hidden="true" />
              <span>Excel</span>
              <small>.xlsx</small>
            </button>
            <button
              type="button"
              onClick={exportarDocumentoPDF}
              disabled={paginacao.totalColaboradores <= 0}
              className={`${estilos.botaoExportacaoDireta} ${estilos.botaoPdfDireto}`}
              title="Exportar relatório de absenteísmo em PDF"
            >
              <BiLogoAdobe size={22} aria-hidden="true" />
              <span>PDF</span>
              <small>.pdf</small>
            </button>
          </div>
          {paginacao.totalColaboradores <= 0 && <small className={estilos.ajudaExportacaoDireta}>Calcule o relatório para habilitar a exportação.</small>}
        </div>
      </header>

      <div className={estilos.cartaoFiltros}>
        <div className={estilos.bordaTopoDecorativa}></div>
        <h2 className={estilos.tituloFiltro}>
          <Filter color="#facc15" size={20} strokeWidth={2.5} /> Parâmetros de
          Análise
        </h2>
        <div className={estilos.gradeFiltros}>
          <div className={estilos.grupoCampoFiltro}>
            <label htmlFor="absenteismo-mes" className={estilos.rotuloFiltro}>Competência</label>
            <div className={estilos.envoltorioEntrada}>
              <select
                id="absenteismo-mes"
                aria-label="Mês da competência"
                value={mes}
                onChange={(e) => setMes(Number(e.target.value))}
                className={`${estilos.entradaFiltro} ${estilos.entradaMes}`}
              >
                {[...Array(12)].map((_, i) => {
                  const valorMesLoop = i + 1;
                  const eFuturo = ano === anoAtual && valorMesLoop > mesAtual;
                  return (
                    <option key={valorMesLoop} value={valorMesLoop} disabled={eFuturo}>
                      {new Date(0, i)
                        .toLocaleString("pt-BR", { month: "short" })
                        .toUpperCase()}
                    </option>
                  );
                })}
              </select>
              <input
                id="absenteismo-ano"
                aria-label="Ano da competência"
                type="number"
                max={anoAtual}
                value={ano}
                onChange={(e) => {
                  let val = Number(e.target.value);
                  if (val > anoAtual) val = anoAtual;
                  setAno(val);
                  if (val === anoAtual && mes > mesAtual) {
                    setMes(mesAtual);
                  }
                }}
                className={`${estilos.entradaFiltro} ${estilos.entradaAno}`}
              />
            </div>
          </div>
          <div className={estilos.grupoCampoFiltro}>
            <label htmlFor="absenteismo-contrato" className={estilos.rotuloFiltro}>Contrato</label>
            <div className={estilos.envoltorioRelativo}>
              <Briefcase className={estilos.iconeInputEsquerda} size={16} />
              <select
                id="absenteismo-contrato"
                value={contratoSelecionado}
                onChange={(e) => setContratoSelecionado(e.target.value)}
                className={`${estilos.entradaFiltro} ${estilos.entradaComIconeEsquerda} ${estilos.entradaComIconeDireita} ${estilos.entradaOcultarSeta} ${estilos.entradaCortarTexto}`}
              >
                <option value="">TODOS OS CONTRATOS</option>
                {contratos.map((c) => (
                  <option key={c.id} value={c.id}>
                    {c.numeroContrato ? `${c.numeroContrato} - ` : c.numero ? `${c.numero} - ` : ""}
                    {c.nome || c.descricao || "Contrato " + c.id}
                  </option>
                ))}
              </select>
              <ChevronDown className={estilos.iconeInputDireita} size={16} />
            </div>
          </div>
          <div className={estilos.grupoCampoFiltro}>
            <label htmlFor="absenteismo-lotacao" className={estilos.rotuloFiltro}>Lotação</label>
            <div className={estilos.envoltorioRelativo}>
              <Building2 className={estilos.iconeInputEsquerda} size={16} />
              <select
                id="absenteismo-lotacao"
                value={lotacaoSelecionada}
                onChange={(e) => setLotacaoSelecionada(e.target.value)}
                className={`${estilos.entradaFiltro} ${estilos.entradaComIconeEsquerda} ${estilos.entradaComIconeDireita} ${estilos.entradaOcultarSeta} ${estilos.entradaCortarTexto}`}
              >
                <option value="">TODAS AS LOTAÇÕES</option>
                {lotacoesDisponiveis.map((l) => (
                  <option key={l.id} value={l.id}>
                    {l.nome || l.descricao}
                  </option>
                ))}
              </select>
              <ChevronDown className={estilos.iconeInputDireita} size={16} />
            </div>
          </div>
          <div className={estilos.grupoCampoFiltro}>
            <label htmlFor="absenteismo-turno" className={estilos.rotuloFiltro}>Turno</label>
            <div className={estilos.envoltorioRelativo}>
              <Clock className={estilos.iconeInputEsquerda} size={16} />
              <select
                id="absenteismo-turno"
                value={turnoSelecionado}
                onChange={(e) => setTurnoSelecionado(e.target.value)}
                className={`${estilos.entradaFiltro} ${estilos.entradaComIconeEsquerda} ${estilos.entradaComIconeDireita} ${estilos.entradaOcultarSeta} ${estilos.entradaCortarTexto}`}
              >
                <option value="">TODOS OS TURNOS</option>
                {turnosDisponiveis.map((t) => (
                  <option key={t.id} value={t.id}>
                    {t.nome || t.descricao}
                  </option>
                ))}
              </select>
              <ChevronDown className={estilos.iconeInputDireita} size={16} />
            </div>
          </div>
          <div
            className={`${estilos.grupoCampoFiltro} ${estilos.grupoCampoLargo}`}
          >
            <label htmlFor="absenteismo-meta" className={estilos.rotuloFiltro}>
              <Target size={14} aria-hidden="true" /> Meta (%)
            </label>
            <div className={estilos.envoltorioEntrada}>
              <div className={estilos.envoltorioRelativo} style={{ flex: 1 }}>
                <input
                  id="absenteismo-meta"
                  type="number"
                  step="0.1"
                  min="0"
                  max="100"
                  value={meta}
                  onChange={(e) => {
                    const valor = Number.parseFloat(e.target.value);
                    setMeta(Number.isFinite(valor) ? Math.min(100, Math.max(0, valor)) : 0);
                  }}
                  disabled={metaBloqueada}
                  className={`${estilos.entradaFiltro} ${metaBloqueada ? estilos.entradaFiltroDesabilitada : ""}`}
                  style={{ paddingRight: "1.5rem" }}
                />
                <span
                  className={estilos.simboloPorcentagem}
                  style={{ color: metaBloqueada ? "#9ca3af" : "#6b7280" }}
                >
                  %
                </span>
              </div>
              <button
                type="button"
                aria-label={metaBloqueada ? "Desbloquear edição da meta" : "Bloquear edição da meta"}
                onClick={() => setMetaBloqueada((bloqueada) => !bloqueada)}
                className={`${estilos.botaoTravaMeta} ${metaBloqueada ? estilos.botaoTravaAtiva : estilos.botaoTravaInativa}`}
                title={
                  metaBloqueada
                    ? "Desbloquear edição da meta"
                    : "Bloquear edição da meta"
                }
              >
                {metaBloqueada ? <Lock size={18} /> : <Unlock size={18} />}
              </button>
            </div>
          </div>
          <div className={estilos.grupoCampoFiltro}>
            <button
              type="button"
              onClick={gerarRelatorio}
              disabled={carregando}
              className={`${estilos.botaoCalcular} ${carregando ? estilos.botaoCalcularInativo : estilos.botaoCalcularAtivo}`}
            >
              {carregando ? (
                <>
                  <div className={estilos.giradorAzul}></div> AGUARDE...
                </>
              ) : (
                <>
                  <Search size={16} strokeWidth={3} /> CALCULAR
                </>
              )}
            </button>
          </div>
        </div>
      </div>

      {revisao.sujeitoARevisao && (
        <div className={estilos.avisoRevisaoRelatorio} role="alert">
          <ShieldAlert size={18} aria-hidden="true" />
          <div>
            <strong>SUJEITO A REVISÃO</strong>
            <span>{revisao.mensagem || `${revisao.quantidadePendencias} pendência(s) de retificação no recorte.`}</span>
          </div>
        </div>
      )}

      {dados.length > 0 ? (
        <div
          className={estilos.animacaoSurgirBaixo}
          style={{ display: "flex", flexDirection: "column", gap: "1.5rem" }}
        >
          <div className={estilos.gradeEstatisticas}>
            <div
              className={`${estilos.cartaoTaxaGeral} ${metaAlcancada === null ? estilos.cartaoTaxaGeralCalculando : metaAlcancada ? estilos.cartaoTaxaGeralMetaAlcancada : estilos.cartaoTaxaGeralMetaNaoAlcancada}`}
            >
              <div className={estilos.decoracaoCantoCartao}></div>
              <div className={estilos.cabecalhoTaxaGeral}>
                <p className={estilos.tituloTaxaGeral}>
                  Taxa de Absenteísmo{" "}
                  {metaAlcancada === null ? (
                    <Activity size={18} />
                  ) : metaAlcancada ? (
                    <TrendingDown size={18} />
                  ) : (
                    <TrendingUp size={18} />
                  )}
                </p>
                <div className={estilos.emblemaMetaTaxa}>Meta: {meta}%</div>
              </div>
              <div className={estilos.valorTaxaGeral}>
                <h3 className={estilos.textoValorTaxa}>
                  {resumoDisponivel ? `${indicadores.taxaGeral}%` : "—"}
                </h3>
              </div>
              <p className={estilos.mensagemTaxaGeral}>
                {!resumoDisponivel
                  ? "Calculando os indicadores da base completa…"
                  : metaAlcancada
                    ? "Operação dentro da meta definida."
                    : "Atenção: o índice ultrapassou a meta definida."}
              </p>
            </div>

            <div className={`${estilos.cartaoKpiPadrao} ${estilos.bordaCinza}`}>
              <div className={estilos.cabecalhoKpi}>
                <p className={`${estilos.tituloKpi} ${estilos.tituloKpiCinza}`}>
                  Base Útil{" "}
                  <HelpCircle size={14} className={estilos.iconeAjuda} />
                </p>
                <div
                  className={`${estilos.caixaIconeKpi} ${estilos.fundoIconeCinza}`}
                >
                  <Calendar size={16} />
                </div>
              </div>
              <h3 className={`${estilos.valorKpi} ${estilos.valorKpiCinza}`}>
                {resumoDisponivel ? indicadores.diasTrabalhaveis : "—"}
              </h3>
              <p className={estilos.subtextoKpi}>Capacidade Produtiva</p>

              <div
                className={`${estilos.tooltipCustomizado} ${estilos.tooltipBaseUtil}`}
              >
                Soma os dias realmente previstos nas escalas históricas das{" "}
                <strong>{Number(indicadores.totalPessoas || paginacao.totalColaboradores).toLocaleString("pt-BR")}</strong> alocações, descontando
                os dias sem expediente; não utiliza divisor fixo.
                <div className={estilos.tooltipSetaBaseUtil}></div>
              </div>
            </div>

            <div className={`${estilos.cartaoKpiPadrao} ${estilos.bordaCinza}`}>
              <div className={estilos.cabecalhoKpi}>
                <p
                  className={`${estilos.tituloKpi} ${estilos.tituloKpiVermelho}`}
                >
                  Faltas
                </p>
                <div
                  className={`${estilos.caixaIconeKpi} ${estilos.fundoIconeVermelho}`}
                >
                  <UserX size={16} />
                </div>
              </div>
              <h3 className={`${estilos.valorKpi} ${estilos.valorKpiVermelho}`}>
                {resumoDisponivel ? indicadores.faltas : "—"}
              </h3>
              <p className={estilos.subtextoKpi}>Dias sem justificativa</p>
            </div>

            <div className={`${estilos.cartaoKpiPadrao} ${estilos.bordaCinza}`}>
              <div className={estilos.cabecalhoKpi}>
                <p className={`${estilos.tituloKpi} ${estilos.tituloKpiAzul}`}>
                  Atestados
                </p>
                <div
                  className={`${estilos.caixaIconeKpi} ${estilos.fundoIconeAzul}`}
                >
                  <Stethoscope size={16} />
                </div>
              </div>
              <h3 className={`${estilos.valorKpi} ${estilos.valorKpiAzul}`}>
                {resumoDisponivel ? indicadores.atestados : "—"}
              </h3>
              <p className={estilos.subtextoKpi}>Dias de saúde</p>
            </div>

            <div
              className={`${estilos.cartaoKpiPadrao} ${estilos.bordaLaranja}`}
            >
              <div className={estilos.cabecalhoKpi}>
                <p
                  className={`${estilos.tituloKpi} ${estilos.tituloKpiLaranja}`}
                >
                  Alertas{" "}
                  <HelpCircle size={14} className={estilos.iconeAjudaLaranja} />
                </p>
                <div
                  className={`${estilos.caixaIconeKpi} ${estilos.fundoIconeLaranja}`}
                >
                  <Activity size={16} />
                </div>
              </div>
              <h3 className={`${estilos.valorKpi} ${estilos.valorKpiLaranja}`}>
                {resumoDisponivel ? indicadores.alertasDisrupcao : "—"}
              </h3>
              <p
                className={`${estilos.subtextoKpi} ${estilos.subtextoKpiCinzaLongo}`}
              >
                Índice Disrupção &gt; {CONFIG.ABSENTEISMO_DISRUPCAO_THRESHOLD}
              </p>

              <div
                className={`${estilos.tooltipCustomizado} ${estilos.tooltipAlertas}`}
              >
                Quantidade de colaboradores que atingiram a zona de risco devido
                a faltas frequentes e muito espaçadas.
                <div className={estilos.tooltipSetaAlertas}></div>
              </div>
            </div>
          </div>

          {resumoDisponivel && indicadores.faltas + indicadores.atestados > 0 && (
            <div className={estilos.gradeGraficos}>
              <div className={estilos.cartaoGrafico}>
                <h3 className={estilos.tituloGrafico}>
                  <PieChartIcon className={estilos.iconeAmarelo} size={20} />{" "}
                  Composição da Taxa
                </h3>
                <div className={estilos.envoltorioGrafico}>
                  <ResponsiveContainer width="100%" height="100%">
                    <PieChart>
                      <Pie
                        data={dadosGraficoPizza}
                        cx="50%"
                        cy="50%"
                        innerRadius={70}
                        outerRadius={100}
                        paddingAngle={5}
                        dataKey="value"
                        stroke="none"
                      >
                        {dadosGraficoPizza.map((entry, index) => (
                          <Cell key={`cell-${index}`} fill={entry.color} />
                        ))}
                      </Pie>
                      <RechartsTooltip
                        formatter={(value) => [`${value} Dias`, "Quantidade"]}
                        contentStyle={{
                          borderRadius: "8px",
                          border: "1px solid #00416B",
                          fontWeight: "bold",
                        }}
                      />
                      <Legend
                        verticalAlign="bottom"
                        height={36}
                        iconType="circle"
                        wrapperStyle={{
                          fontWeight: "bold",
                          fontSize: "12px",
                          color: "#00416B",
                        }}
                      />
                    </PieChart>
                  </ResponsiveContainer>
                </div>
              </div>

              <div className={estilos.cartaoGrafico}>
                <h3 className={estilos.tituloGrafico}>
                  <BarChartIcon className={estilos.iconeAmarelo} size={20} />{" "}
                  Top 10 maiores taxas de absenteísmo
                </h3>
                <div className={estilos.envoltorioGrafico}>
                  <ResponsiveContainer width="100%" height="100%">
                    <BarChart
                      data={dadosGraficoBarras}
                      layout="vertical"
                      margin={{ top: 0, right: 40, left: 0, bottom: 0 }}
                    >
                      <CartesianGrid
                        strokeDasharray="3 3"
                        horizontal={false}
                        stroke="#e2e8f0"
                      />
                      <XAxis
                        type="number"
                        hide
                        domain={[
                          0,
                          (dataMax) =>
                            Math.max(meta * 2, Math.ceil(dataMax * 1.2)),
                        ]}
                      />
                      <YAxis
                        dataKey="nome"
                        type="category"
                        width={130}
                        tick={{
                          fontSize: 11,
                          fill: "#00416B",
                          fontWeight: "bold",
                        }}
                        axisLine={false}
                        tickLine={false}
                      />
                      <RechartsTooltip
                        formatter={(value) => [
                          `${value}%`,
                          "Taxa de Absenteísmo",
                        ]}
                        cursor={{ fill: "#f1f5f9" }}
                        contentStyle={{
                          borderRadius: "8px",
                          border: "1px solid #00416B",
                          fontWeight: "bold",
                        }}
                      />
                      <Bar dataKey="Taxa" radius={[0, 4, 4, 0]} barSize={18}>
                        {dadosGraficoBarras.map((entry, index) => (
                          <Cell
                            key={`cell-${index}`}
                            fill={entry.Taxa > meta ? "#e11d48" : "#00416B"}
                          />
                        ))}
                        <LabelList
                          dataKey="Taxa"
                          position="right"
                          formatter={(value) => `${value}%`}
                          fill="#00416B"
                          fontSize={11}
                          fontWeight="bold"
                        />
                      </Bar>
                    </BarChart>
                  </ResponsiveContainer>
                </div>
              </div>
            </div>
          )}

          <div className={estilos.cartaoTabela}>
            <div className={estilos.cabecalhoCartaoTabela}>
              <div className={estilos.grupoTituloTabela}>
                <AlertOctagon
                  size={20}
                  className={estilos.iconeAmarelo}
                  strokeWidth={2.5}
                />
                <h3 className={estilos.tituloCartaoTabela}>
                  DETALHAMENTO DE ABSENTEÍSMO POR COLABORADOR
                </h3>
              </div>
            </div>

            <div className={estilos.envoltorioTabela}>
              <table className={estilos.tabelaAbsenteismo}>
                <thead className={estilos.cabecalhoTabela}>
                  <tr>
                    <th className={estilos.celulaCabecalhoFixa}>Colaborador</th>
                    <th
                      className={`${estilos.celulaCabecalho} ${estilos.celulaCabecalhoVermelha}`}
                    >
                      Faltas
                    </th>
                    <th
                      className={`${estilos.celulaCabecalho} ${estilos.celulaCabecalhoAzul}`}
                    >
                      Atestados
                    </th>
                    <th
                      className={`${estilos.celulaCabecalho} ${estilos.celulaCabecalhoLaranja}`}
                    >
                      DSRs Perd.
                    </th>
                    <th
                      className={`${estilos.celulaCabecalho} ${estilos.fundoEscuroCabecalho}`}
                      title="Identifica padrões de faltas muito espaçadas"
                    >
                      Índice Disrupção{" "}
                      <Info size={12} className={estilos.iconeInfoTabela} />
                    </th>
                    <th
                      className={`${estilos.celulaCabecalho} ${estilos.fundoEscuroBordaAmarela}`}
                    >
                      Taxa Abs. (%)
                    </th>
                    <th
                      className={`${estilos.celulaCabecalho} ${estilos.semBordaDireita}`}
                    >
                      Classificação
                    </th>
                  </tr>
                </thead>
                <tbody className={estilos.corpoTabela}>
                  {dados.map((empregado, indice) => {
                    const eLinhaPar = indice % 2 === 0;
                    const classeLinha = eLinhaPar
                      ? estilos.linhaPar
                      : estilos.linhaImpar;
                    const taxaAcimaDaMeta = empregado.taxaIndividual > meta;

                    return (
                      <tr
                        key={empregado.id}
                        className={`${estilos.linhaTabela} ${classeLinha}`}
                      >
                        <td className={`${estilos.celulaFixa} ${classeLinha}`}>
                          <div className={estilos.perfilTabela}>
                            <div
                              className={`${estilos.rankEmblema} ${estilos.rankNormal}`}
                            >
                              #{paginacao.pagina * paginacao.tamanho + indice + 1}
                            </div>
                            <div className={estilos.textoPerfilTabela}>
                              <p
                                className={estilos.nomeTabela}
                                title={empregado.nome}
                              >
                                {empregado.nome}
                              </p>
                              <p
                                className={estilos.cargoTabela}
                                title={empregado.cargo}
                              >
                                {empregado.cargo}
                              </p>
                            </div>
                          </div>
                        </td>
                        <td className={estilos.celulaCentro}>
                          {empregado.faltas > 0 ? (
                            <span className={estilos.valorFalta}>
                              {empregado.faltas}
                            </span>
                          ) : (
                            <span className={estilos.valorVazio}>-</span>
                          )}
                        </td>
                        <td className={estilos.celulaCentro}>
                          {empregado.atestados > 0 ? (
                            <span className={estilos.valorAtestado}>
                              {empregado.atestados}
                            </span>
                          ) : (
                            <span className={estilos.valorVazio}>-</span>
                          )}
                        </td>
                        <td className={estilos.celulaCentro}>
                          {empregado.dsr > 0 ? (
                            <span className={estilos.valorDsr}>
                              {empregado.dsr}
                            </span>
                          ) : (
                            <span className={estilos.valorVazio}>-</span>
                          )}
                        </td>
                        <td
                          className={`${estilos.celulaCentro} ${estilos.fundoCinzaClaro}`}
                        >
                          {obterEmblemaDisrupcao(empregado.disrupcao)}
                        </td>
                        <td className={estilos.celulaTaxa}>
                          <span
                            className={`${estilos.emblemaTaxa} ${empregado.taxaIndividual === 0 ? estilos.taxaVazia : taxaAcimaDaMeta ? estilos.taxaAcimaMeta : estilos.taxaNormal}`}
                          >
                            {empregado.taxaIndividual}%
                          </span>
                        </td>
                        <td className={estilos.celulaCentro}>
                          <div className={estilos.classificacaoCentralizada}>
                            {obterEmblemaRisco(empregado.taxaIndividual)}
                          </div>
                        </td>
                      </tr>
                    );
                  })}
                </tbody>
              </table>
            </div>
          </div>

          {paginacao.totalColaboradores > 0 && (
            <div className={estilos.paginacaoRelatorio}>
              <button type="button" disabled={paginacao.primeira || carregando} onClick={() => carregarPagina(Math.max(0, paginacao.pagina - 1), false)}>Anterior</button>
              <span>Página <strong>{paginacao.pagina + 1}</strong> de <strong>{Math.max(1, paginacao.totalPaginas)}</strong> · {Number(paginacao.totalColaboradores).toLocaleString("pt-BR")} colaborador(es)</span>
              <label className={estilos.seletorTamanhoPagina}>Itens por página <select value={paginacao.tamanho} disabled={carregando} onChange={(evento) => carregarPagina(0, false, Number(evento.target.value))}><option value="25">25</option><option value="100">100</option><option value="200">200</option><option value="500">500</option></select></label>
              <button type="button" disabled={paginacao.ultima || carregando} onClick={() => carregarPagina(paginacao.pagina + 1, false)}>Próxima</button>
            </div>
          )}
          {carregandoResumo && <div className={estilos.avisoResumo}>Calculando indicadores e ranking global da base completa…</div>}

          <div className={estilos.gradeInformacoes}>
            <div className={estilos.cartaoInfoAzul}>
              <Info size={24} className={estilos.iconeInfoAmarelo} />
              <div>
                <h4 className={estilos.tituloInfoAzul}>
                  Cálculo de Absenteísmo
                </h4>
                <p className={estilos.textoInfoClaro}>
                  O percentual é gerado dividindo os{" "}
                  <strong>Dias Ausentes (Faltas + Atestados)</strong> pelos{" "}
                  <strong>Dias Previstos de Trabalho</strong> da equipe
                  (descontando folgas e feriados oficiais).
                </p>
              </div>
            </div>

            <div className={estilos.cartaoInfoEscuro}>
              <div className={estilos.cabecalhoInfoEscuro}>
                <div className={estilos.iconeCaixaLaranja}>
                  <Activity size={20} className={estilos.iconeLaranja} />
                </div>
                <h4 className={estilos.tituloInfoLaranja}>
                  Entendendo o Índice de Disrupção
                </h4>
              </div>
              <p className={estilos.textoInfoEscuro}>
                Este índice identifica colaboradores que têm o hábito de faltar
                frequentemente por poucos dias (ex: toda sexta-feira). Esse
                comportamento quebra a previsibilidade da escala de trabalho
                muito mais do que um atestado médico contínuo de vários dias.
              </p>

              <div className={estilos.gradeExemplosDisrupcao}>
                <div className={`${estilos.caixaExemplo} ${estilos.bordaAzul}`}>
                  <span className={estilos.rotuloExemplo}>
                    0 a {CONFIG.ABSENTEISMO_DISRUPCAO_THRESHOLD - 1}
                  </span>
                  <span className={estilos.tituloExemplo}>Padrão Normal</span>
                  <span className={estilos.descExemplo}>
                    Faltas raras ou atestados longos isolados.
                  </span>
                </div>
                <div
                  className={`${estilos.caixaExemplo} ${estilos.bordaLaranja}`}
                >
                  <span className={estilos.rotuloExemplo}>
                    {CONFIG.ABSENTEISMO_DISRUPCAO_THRESHOLD} a{" "}
                    {CONFIG.ABSENTEISMO_DISRUPCAO_THRESHOLD * 2 - 1}
                  </span>
                  <span className={estilos.tituloExemplo}>Atenção</span>
                  <span className={estilos.descExemplo}>
                    O colaborador tem começado a faltar de forma espaçada.
                  </span>
                </div>
                <div
                  className={`${estilos.caixaExemplo} ${estilos.bordaVermelha}`}
                >
                  <span className={estilos.rotuloExemplo}>
                    {CONFIG.ABSENTEISMO_DISRUPCAO_THRESHOLD * 2} ou mais
                  </span>
                  <span className={estilos.tituloExemplo}>Risco Crítico</span>
                  <span className={estilos.descExemplo}>
                    Padrão repetitivo de ausência. Requer feedback imediato.
                  </span>
                </div>
              </div>
            </div>
          </div>
        </div>
      ) : (
        !carregando && (
          <div className={estilos.estadoVazioInicial}>
            <div className={estilos.circuloIconeVazio}>
              <Search
                size={48}
                className={estilos.iconeVazio}
                strokeWidth={1.5}
              />
            </div>
            <h3 className={estilos.tituloEstadoVazio}>Nenhum dado calculado</h3>
            <p className={estilos.textoEstadoVazio}>
              Preencha os parâmetros acima e defina a meta para visualizar as
              taxas.
            </p>
          </div>
        )
      )}
    </div>
  );
};
