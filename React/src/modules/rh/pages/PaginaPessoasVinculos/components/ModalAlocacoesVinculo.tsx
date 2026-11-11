import { observarFalha } from '../../../../../core/http/falhaObservada';
import type { HttpError } from '../../../../../core/http/types';
import type { AllocationSubmission } from '../../../../../contracts/pessoas-vinculos';
import type * as DTO from '../../../../../contracts/java';
import type { MutableForm } from '../../../../../contracts/formularios';
import React, { useEffect, useMemo, useRef, useState } from "react";
import {
  CalendarRange, GitCompareArrows, History, MapPin, RefreshCw, RotateCcw,
  Search, ShieldCheck, TimerReset, TriangleAlert,
} from "lucide-react";
import { notificar } from "../../../../../core/events.ts";
import { obterDataNoFusoNegocio } from "../../../../../core/formatters/dataHoraNegocio.ts";
import { ServicoVinculoContratual } from "../../../../../services/rh/ServicoVinculoContratual.ts";
import {
  criarChaveContextoOpcoesAlocacao,
  ehProgramacaoFuturaCancelavel,
  prepararAlteracaoAlocacao,
  prepararCancelamentoAlocacao,
  rotularEstadoAlocacao,
  rotularRelacaoEventoAlocacao,
  rotularSituacaoTemporalAlocacao,
  rotularTipoEventoAlocacao,
  validarAlteracaoAlocacao,
  validarCancelamentoAlocacao,
} from "../../../../../services/rh/ServicoDominioAlocacaoVinculo.ts";
import {
  formatarDataPessoaVinculo,
  formatarInstantePessoaVinculo,
} from "../../../../../services/rh/ServicoDominioPessoaVinculo.ts";
import { ModalEstrutural } from "./ModalEstrutural.tsx";
import estilos from "../index.module.css";

const hojeNegocial = () => obterDataNoFusoNegocio() || "";

const mesAtual = () => hojeNegocial().slice(0, 7);

const DADOS_ALOCACAO = Object.freeze({
  dataEfetiva: "", lotacaoId: "", jornadaId: "", opcaoChave: "",
  justificativa: "", evidencia: "", autorizacao: "",
  confirmarImpacto: false, tokenPrevia: "", confirmarPrecedenciaUltimoDia: false,
  confirmarInclusaoExcedente: false,
});

const DADOS_CANCELAMENTO = Object.freeze({
  justificativa: "", evidencia: "", autorizacao: "", confirmarImpacto: false, tokenPrevia: null,
});

const chaveOpcao = (opcao: (DTO.OpcaoAlocacaoTemporalDTO)) => [
  opcao?.composicaoBaseId ?? "sem-base", opcao?.lotacaoId ?? "", opcao?.jornadaId ?? "", opcao?.cargoId ?? "",
].join(":");

const deduplicar = (opcoes: (DTO.OpcaoAlocacaoTemporalDTO[]), campoId: 'lotacaoId' | 'jornadaId' | 'cargoId', campoNome: 'lotacaoNome' | 'jornadaDescricao' | 'cargoNome') => {
  const mapa = new Map<string, { id: number; nome: string }>();
  (opcoes || []).forEach((opcao) => {
    const id = opcao?.[campoId];
    if (id != null && !mapa.has(String(id))) mapa.set(String(id), { id, nome: opcao?.[campoNome] || `ID ${id}` });
  });
  return [...mapa.values()];
};

const formatarCombinacao = (item: (DTO.AlocacaoVinculoDTO) | (DTO.PosicaoAlocacaoTemporalDTO)) => [item?.lotacaoNome, item?.jornadaDescricao, item?.cargoNome].filter(Boolean).join(" · ") || "—";

const formatarCombinacaoEvento = (evento: (DTO.EventoAlocacaoVinculoDTO), momento: (string)) => {
  const anterior = momento === "ANTERIOR";
  return [
    anterior ? evento?.lotacaoAnteriorNome : evento?.lotacaoNovaNome,
    anterior ? evento?.jornadaAnteriorDescricao : evento?.jornadaNovaDescricao,
    anterior ? evento?.cargoAnteriorNome : evento?.cargoNovoNome,
  ].filter(Boolean).join(" · ") || "—";
};

const mensagemErro = (erro: Partial<HttpError>, fallback: (string)) => {
  if (erro?.status === 409) return erro.message || "A linha temporal mudou. Recarregue e gere uma nova prévia.";
  if (erro?.resultadoIncerto) return `Resultado incerto. Não repita a mutação sem conferir a linha do tempo.${erro.requestId ? ` Referência: ${erro.requestId}.` : ""}`;
  return erro?.message || fallback;
};

const estadoNivel = (): { controlador: AbortController | null; chave: string } => ({ controlador: null, chave: "" });

export const ModalAlocacoesVinculo = ({
  aberto, vinculo, podeGerenciar, processando, aoFechar, aoSalvarAlocacao, aoCancelarAlocacao,
}: { aberto: boolean; vinculo: DTO.VinculoContratualDTO | null; podeGerenciar?: boolean; processando?: boolean; aoFechar: () => void; aoSalvarAlocacao: (operation: AllocationSubmission) => Promise<boolean | void>; aoCancelarAlocacao: (alocacao: DTO.AlocacaoVinculoDTO, payload: ReturnType<typeof prepararCancelamentoAlocacao>) => Promise<boolean | void> }) => {
  const [mes, setMes] = useState(mesAtual);
  const [alocacoes, setAlocacoes] = useState<(DTO.AlocacaoVinculoDTO[])>([]);
  const [eventos, setEventos] = useState<(DTO.EventoAlocacaoVinculoDTO[])>([]);
  const [carregandoLinha, setCarregandoLinha] = useState(false);
  const [posicao, setPosicao] = useState<(DTO.PosicaoAlocacaoTemporalDTO) | null>(null);
  const [dataPosicao, setDataPosicao] = useState(hojeNegocial);
  const [conhecidoAte, setConhecidoAte] = useState("");
  const [consultandoPosicao, setConsultandoPosicao] = useState(false);

  const [modo, setModo] = useState<("ALTERAR") | ("RETIFICAR") | ("CANCELAR") | null>(null);
  const [alocacaoAlvo, setAlocacaoAlvo] = useState<(DTO.AlocacaoVinculoDTO) | null>(null);
  const [dados, setDados] = useState<Omit<MutableForm<typeof DADOS_ALOCACAO>, "dataEfetiva"> & { dataEfetiva: string | null }>({ ...DADOS_ALOCACAO });
  const [dadosCancelamento, setDadosCancelamento] = useState<Omit<MutableForm<typeof DADOS_CANCELAMENTO>, "tokenPrevia"> & { tokenPrevia: string | null }>({ ...DADOS_CANCELAMENTO });
  const [previa, setPrevia] = useState<(DTO.PreviaImpactoAlocacaoDTO) | null>(null);
  const [carregandoPrevia, setCarregandoPrevia] = useState(false);
  const [submetendo, setSubmetendo] = useState(false);
  const [erroFormulario, setErroFormulario] = useState("");
  const pedidoPrevia = useRef(0);
  const controladorPrevia = useRef<(AbortController) | null>(null);
  const envioPendente = useRef<object | null>(null); // Identity marker; no payload properties.
  const contextoOpcoes = useRef(0);
  const ocupado = Boolean(processando || submetendo);

  const [opcoesLotacoes, setOpcoesLotacoes] = useState<(DTO.OpcaoAlocacaoTemporalDTO[])>([]);
  const [opcoesJornadas, setOpcoesJornadas] = useState<(DTO.OpcaoAlocacaoTemporalDTO[])>([]);
  const [opcoesCargos, setOpcoesCargos] = useState<(DTO.OpcaoAlocacaoTemporalDTO[])>([]);
  const [carregandoNivel, setCarregandoNivel] = useState({ LOTACOES: false, JORNADAS: false, CARGOS: false });
  const requisicoesOpcoes = useRef({ LOTACOES: estadoNivel(), JORNADAS: estadoNivel(), CARGOS: estadoNivel() });
  const requisicaoLinha = useRef<(AbortController) | null>(null);

  const hoje = hojeNegocial();
  const lotacoes = useMemo(() => deduplicar(opcoesLotacoes, "lotacaoId", "lotacaoNome"), [opcoesLotacoes]);
  const jornadas = useMemo(() => deduplicar(opcoesJornadas, "jornadaId", "jornadaDescricao"), [opcoesJornadas]);
  const opcaoSelecionada = useMemo(() => opcoesCargos.find((opcao) => chaveOpcao(opcao) === dados.opcaoChave) || null, [dados.opcaoChave, opcoesCargos]);
  const exigePrecedenciaUltimoDia = Boolean(vinculo?.dataFim && dados.dataEfetiva && vinculo.dataFim === dados.dataEfetiva);

  const abortarOpcoes = () => {
    contextoOpcoes.current += 1;
    Object.values(requisicoesOpcoes.current).forEach((nivel) => nivel.controlador?.abort());
    setCarregandoNivel({ LOTACOES: false, JORNADAS: false, CARGOS: false });
  };

  const carregarLinhaTemporal = async (mesReferencia = mes) => {
    if (!vinculo?.id) return;
    requisicaoLinha.current?.abort();
    const controlador = new AbortController();
    requisicaoLinha.current = controlador;
    setCarregandoLinha(true);
    try {
      const [faixas, trilha] = await Promise.all([
        ServicoVinculoContratual.listarAlocacoes(vinculo.id, { mes: mesReferencia }, { signal: controlador.signal }),
        ServicoVinculoContratual.listarEventosAlocacao(vinculo.id, { signal: controlador.signal }),
      ]);
      if (!controlador.signal.aborted) {
        setAlocacoes(faixas);
        setEventos(trilha);
      }
    } catch (erroOriginal) {
      const erro = observarFalha(erroOriginal);

      if (!erro?.canceled) notificar("error", mensagemErro(erro, "Não foi possível carregar a linha temporal de alocação."));
    } finally {
      if (!controlador.signal.aborted) setCarregandoLinha(false);
    }
  };

  const carregarNivelOpcoes = async (nivel: 'LOTACOES' | 'JORNADAS' | 'CARGOS', contexto: { data: string | null; lotacaoId?: number | null; jornadaId?: number | null }) => {
    if (!vinculo?.contratoId || !contexto?.data) return [];
    const anterior = requisicoesOpcoes.current[nivel];
    anterior.controlador?.abort();
    const controlador = new AbortController();
    const chave = `${nivel}|${criarChaveContextoOpcoesAlocacao({ contratoId: vinculo.contratoId, ...contexto })}`;
    requisicoesOpcoes.current[nivel] = { controlador, chave };
    setCarregandoNivel((atual) => ({ ...atual, [nivel]: true }));
    try {
      const opcoes = await ServicoVinculoContratual.obterOpcoesAlocacao(vinculo.contratoId, contexto, { signal: controlador.signal });
      const estadoAtual = requisicoesOpcoes.current[nivel];
      if (controlador.signal.aborted || estadoAtual.chave !== chave) return [];
      if (nivel === "LOTACOES") setOpcoesLotacoes(opcoes);
      if (nivel === "JORNADAS") setOpcoesJornadas(opcoes);
      if (nivel === "CARGOS") setOpcoesCargos(opcoes);
      return opcoes;
    } catch (erroOriginal) {
      const erro = observarFalha(erroOriginal);

      if (!controlador.signal.aborted && !erro?.canceled) notificar("error", mensagemErro(erro, "Não foi possível obter opções de alocação para este contexto."));
      return [];
    } finally {
      if (requisicoesOpcoes.current[nivel].chave === chave && !controlador.signal.aborted) {
        setCarregandoNivel((atual) => ({ ...atual, [nivel]: false }));
      }
    }
  };

  useEffect(() => {
    invalidarPrevia();
    envioPendente.current = null;
    setSubmetendo(false);
    setErroFormulario("");
    if (!aberto || !vinculo?.id) return undefined;
    setMes(mesAtual());
    setDataPosicao(hojeNegocial());
    setConhecidoAte("");
    setPosicao(null);
    setModo(null);
    setAlocacaoAlvo(null);
    carregarLinhaTemporal(mesAtual());
    return () => {
      requisicaoLinha.current?.abort();
      abortarOpcoes();
      pedidoPrevia.current += 1;
      controladorPrevia.current?.abort();
      envioPendente.current = null;
    };
  }, [aberto, vinculo?.id]); // vínculo muda o contexto inteiro; leituras são reabertas do zero

  const invalidarPrevia = () => {
    pedidoPrevia.current += 1;
    controladorPrevia.current?.abort();
    setPrevia(null);
    setCarregandoPrevia(false);
    setDados((atual) => ({ ...atual, tokenPrevia: "", confirmarImpacto: false }));
  };

  useEffect(() => { invalidarPrevia(); }, [vinculo?.version]);

  const alterarDado = (campo: (string), valor: (string) | (boolean)) => {
    if (envioPendente.current || processando) return;
    invalidarPrevia();
    setErroFormulario("");
    setDados((atual) => ({ ...atual, [campo]: valor, tokenPrevia: "", confirmarImpacto: false }));
  };

  const iniciarAlteracao = async () => {
    if (envioPendente.current || processando) return;
    abortarOpcoes();
    invalidarPrevia();
    setErroFormulario("");
    const referencia = vinculo?.dataFim && hoje > vinculo.dataFim ? vinculo.dataFim : (vinculo?.dataInicio && hoje < vinculo.dataInicio ? vinculo.dataInicio : hoje);
    setModo("ALTERAR");
    setAlocacaoAlvo(null);
    setPrevia(null);
    setDados({ ...DADOS_ALOCACAO, dataEfetiva: referencia });
    setOpcoesLotacoes([]); setOpcoesJornadas([]); setOpcoesCargos([]);
    await carregarNivelOpcoes("LOTACOES", { data: referencia });
  };

  const iniciarRetificacao = async (alocacao: (DTO.AlocacaoVinculoDTO)) => {
    if (envioPendente.current || processando) return;
    abortarOpcoes();
    invalidarPrevia();
    setErroFormulario("");
    const contexto = contextoOpcoes.current;
    setModo("RETIFICAR");
    setAlocacaoAlvo(alocacao);
    setPrevia(null);
    setOpcoesLotacoes([]); setOpcoesJornadas([]); setOpcoesCargos([]);
    const base = {
      ...DADOS_ALOCACAO,
      dataEfetiva: alocacao.dataInicio,
      lotacaoId: String(alocacao.lotacaoId ?? ""),
      jornadaId: String(alocacao.jornadaId ?? ""),
    };
    setDados(base);
    await carregarNivelOpcoes("LOTACOES", { data: alocacao.dataInicio });
    if (contexto !== contextoOpcoes.current) return;
    await carregarNivelOpcoes("JORNADAS", { data: alocacao.dataInicio, lotacaoId: alocacao.lotacaoId });
    if (contexto !== contextoOpcoes.current) return;
    const cargos = await carregarNivelOpcoes("CARGOS", { data: alocacao.dataInicio, lotacaoId: alocacao.lotacaoId, jornadaId: alocacao.jornadaId });
    if (contexto !== contextoOpcoes.current) return;
    const atual = cargos.find((opcao) => Number(opcao.cargoId) === Number(alocacao.cargoId));
    if (atual) setDados((estado) => ({ ...estado, opcaoChave: chaveOpcao(atual) }));
  };

  const iniciarCancelamento = (alocacao: (DTO.AlocacaoVinculoDTO)) => {
    if (envioPendente.current || processando) return;
    abortarOpcoes();
    invalidarPrevia();
    setErroFormulario("");
    setModo("CANCELAR");
    setAlocacaoAlvo(alocacao);
    setDadosCancelamento({ ...DADOS_CANCELAMENTO });
    setPrevia(null);
  };

  const mudarData = async (valor: (string)) => {
    if (envioPendente.current || processando) return;
    invalidarPrevia();
    setErroFormulario("");
    setDados((atual) => ({ ...atual, dataEfetiva: valor, lotacaoId: "", jornadaId: "", opcaoChave: "", tokenPrevia: "", confirmarImpacto: false, confirmarPrecedenciaUltimoDia: false, confirmarInclusaoExcedente: false }));
    setOpcoesLotacoes([]); setOpcoesJornadas([]); setOpcoesCargos([]);
    abortarOpcoes();
    if (valor) await carregarNivelOpcoes("LOTACOES", { data: valor });
  };

  const mudarLotacao = async (valor: (string)) => {
    if (envioPendente.current || processando) return;
    contextoOpcoes.current += 1;
    invalidarPrevia();
    setErroFormulario("");
    setDados((atual) => ({ ...atual, lotacaoId: valor, jornadaId: "", opcaoChave: "", tokenPrevia: "", confirmarImpacto: false, confirmarInclusaoExcedente: false }));
    setOpcoesJornadas([]); setOpcoesCargos([]);
    setCarregandoNivel((atual) => ({ ...atual, JORNADAS: false, CARGOS: false }));
    requisicoesOpcoes.current.JORNADAS.controlador?.abort();
    requisicoesOpcoes.current.CARGOS.controlador?.abort();
    if (valor) await carregarNivelOpcoes("JORNADAS", { data: dados.dataEfetiva, lotacaoId: Number(valor) });
  };

  const mudarJornada = async (valor: (string)) => {
    if (envioPendente.current || processando) return;
    contextoOpcoes.current += 1;
    invalidarPrevia();
    setErroFormulario("");
    setDados((atual) => ({ ...atual, jornadaId: valor, opcaoChave: "", tokenPrevia: "", confirmarImpacto: false, confirmarInclusaoExcedente: false }));
    setOpcoesCargos([]);
    setCarregandoNivel((atual) => ({ ...atual, CARGOS: false }));
    requisicoesOpcoes.current.CARGOS.controlador?.abort();
    if (valor) await carregarNivelOpcoes("CARGOS", { data: dados.dataEfetiva, lotacaoId: Number(dados.lotacaoId), jornadaId: Number(valor) });
  };

  const montarPayload = (comToken = true) => prepararAlteracaoAlocacao({
    ...dados,
    composicaoId: opcaoSelecionada?.composicaoBaseId ?? null,
    lotacaoId: opcaoSelecionada?.lotacaoId ?? dados.lotacaoId,
    jornadaId: opcaoSelecionada?.jornadaId ?? dados.jornadaId,
    cargoId: opcaoSelecionada?.cargoId ?? null,
    tokenPrevia: comToken ? dados.tokenPrevia : null,
    confirmarImpacto: comToken ? dados.confirmarImpacto : false,
    exigePrecedenciaUltimoDia,
  });

  const gerarPrevia = async () => {
    if (!aberto || envioPendente.current || processando || carregandoPrevia) return;
    if (!dados.dataEfetiva || !opcaoSelecionada || !dados.justificativa.trim() || !dados.evidencia.trim() || !dados.autorizacao.trim()) {
      notificar("warning", "Informe data, Lotação, Turno, Cargo e os três campos auditáveis antes da prévia.");
      return;
    }
    invalidarPrevia();
    const pedido = pedidoPrevia.current;
    const controlador = new AbortController();
    controladorPrevia.current = controlador;
    setErroFormulario("");
    setCarregandoPrevia(true);
    try {
      const resposta = await ServicoVinculoContratual.gerarPreviaAlocacao(vinculo?.id, montarPayload(false), { signal: controlador.signal });
      if (pedido !== pedidoPrevia.current || controlador.signal.aborted) return;
      const token = resposta?.token;
      if (!token) throw new Error("O servidor não retornou um token de prévia válido. Gere uma nova prévia.");
      setPrevia(resposta);
      setDados((atual) => ({ ...atual, tokenPrevia: token, confirmarImpacto: false }));
    } catch (erroOriginal) {
      const erro = observarFalha(erroOriginal);

      if (pedido === pedidoPrevia.current && !controlador.signal.aborted && !erro?.canceled) setErroFormulario(mensagemErro(erro, "Não foi possível gerar a prévia de alocação."));
    } finally {
      if (pedido === pedidoPrevia.current) setCarregandoPrevia(false);
    }
  };

  const cancelarEditor = () => {
    if (envioPendente.current || processando) return;
    abortarOpcoes();
    invalidarPrevia();
    setModo(null); setAlocacaoAlvo(null); setErroFormulario("");
  };

  const fechar = () => {
    if (envioPendente.current || processando) return;
    cancelarEditor();
    aoFechar();
  };

  const executarComando = async (acao: (() => Promise<boolean | void>)) => {
    if (!aberto || envioPendente.current || processando) return;
    const envio = {};
    envioPendente.current = envio;
    setSubmetendo(true);
    setErroFormulario("");
    try {
      const concluido = await acao();
      if (envioPendente.current !== envio) return;
      if (concluido) {
        abortarOpcoes();
        setModo(null); setAlocacaoAlvo(null);
        await carregarLinhaTemporal(mes);
      }
    } catch (erroOriginal) {
      const erro = observarFalha(erroOriginal);

      if (envioPendente.current === envio && !erro?.canceled) setErroFormulario(mensagemErro(erro, "Não foi possível concluir a operação de alocação."));
    } finally {
      if (envioPendente.current === envio) {
        envioPendente.current = null;
        setSubmetendo(false);
        invalidarPrevia();
      }
    }
  };

  const confirmarMudanca = async (evento: React.FormEvent<HTMLFormElement>) => {
    evento.preventDefault();
    if (envioPendente.current || processando || carregandoPrevia || !modo) return;
    const erroValidacao = validarAlteracaoAlocacao({ ...dados, cargoId: opcaoSelecionada?.cargoId, exigePrecedenciaUltimoDia });
    if (erroValidacao) { notificar("warning", erroValidacao); return; }
    if ((previa?.incompatibilidades || []).length) { notificar("warning", "Resolva as incompatibilidades apontadas pelo servidor antes de concluir."); return; }
    await executarComando(() => aoSalvarAlocacao({ modo, alocacaoId: alocacaoAlvo?.id, payload: montarPayload(true) }));
  };

  const confirmarCancelamento = async (evento: React.FormEvent<HTMLFormElement>) => {
    evento.preventDefault();
    if (envioPendente.current || processando || !alocacaoAlvo) return;
    const erroValidacao = validarCancelamentoAlocacao(dadosCancelamento);
    if (erroValidacao) { notificar("warning", erroValidacao); return; }
    await executarComando(() => aoCancelarAlocacao(alocacaoAlvo, prepararCancelamentoAlocacao(dadosCancelamento)));
  };

  const consultarPosicao = async () => {
    if (!dataPosicao) return;
    setConsultandoPosicao(true);
    try {
      const instante = conhecidoAte ? new Date(conhecidoAte).toISOString() : undefined;
      setPosicao(await ServicoVinculoContratual.consultarPosicaoAlocacao(vinculo?.id, { data: dataPosicao, conhecidoAte: instante }));
    } catch (erroOriginal) {
      const erro = observarFalha(erroOriginal);
 notificar("error", mensagemErro(erro, "Não foi possível reconstruir a posição temporal da alocação.")); }
    finally { setConsultandoPosicao(false); }
  };

  const alterarMes = async (valor: (string)) => {
    setMes(valor);
    await carregarLinhaTemporal(valor);
  };

  return (
    <ModalEstrutural aberto={aberto} titulo={`Alocações temporais · vínculo ${vinculo?.id || ""}`} subtitulo="Lotação + Turno + Cargo como faixas temporais auditáveis" aoFechar={fechar} largura="extra">
      <div className={estilos.alocacoesCabecalho}>
        <div><strong>{vinculo?.pessoaNome || "Pessoa"}</strong><span>Contrato {vinculo?.contratoNumero || vinculo?.contratoId}{vinculo?.contratoAno ? `/${vinculo.contratoAno}` : ""} · vínculo {formatarDataPessoaVinculo(vinculo?.dataInicio)} a {formatarDataPessoaVinculo(vinculo?.dataFim, "em aberto")}</span></div>
        {podeGerenciar && vinculo?.estadoNegocial === "REGISTRADO" && <button type="button" className={estilos.botaoPrimario} onClick={iniciarAlteracao} disabled={ocupado}><CalendarRange size={16} /> Programar / alterar alocação</button>}
      </div>

      <div className={estilos.avisoTemporalAlocacao}><ShieldCheck size={18} /><p>O navegador não calcula cobertura, capacidade, excedente, lacuna ou sobreposição. A prévia e o comando usam a linha temporal reconstruída pelo Java.</p></div>

      <section className={estilos.secaoDetalhe}>
        <div className={estilos.tituloSecao}><CalendarRange size={18} /><h3>Faixas do mês</h3><span>{alocacoes.length}</span></div>
        <div className={estilos.filtroMesAlocacao}><label><span>Mês</span><input type="month" value={mes} onChange={(e) => alterarMes(e.target.value)} /></label><button type="button" className={estilos.botaoSecundario} onClick={() => carregarLinhaTemporal(mes)} disabled={carregandoLinha}><RefreshCw size={15} className={carregandoLinha ? estilos.girando : ""} /> Atualizar</button></div>
        {carregandoLinha ? <p className={estilos.estadoVazio}>Reconstruindo faixas e eventos…</p> : alocacoes.length === 0 ? <p className={estilos.estadoVazio}>Nenhuma faixa sobrepõe o mês selecionado.</p> : (
          <div className={estilos.listaAlocacoes}>{alocacoes.map((alocacao) => (
            <article key={alocacao.id} className={estilos.cartaoAlocacao}>
              <header><div><strong>{formatarCombinacao(alocacao)}</strong><span>{formatarDataPessoaVinculo(alocacao.dataInicio)} → {formatarDataPessoaVinculo(alocacao.dataFim, "em aberto")}</span></div><em>{rotularEstadoAlocacao(alocacao.estadoNegocial)}</em></header>
              <small>Faixa #{alocacao.id} · criada {formatarInstantePessoaVinculo(alocacao.criadoEm)} por {alocacao.criadoPorUsername || "sistema"}</small>
              {podeGerenciar && alocacao.estadoNegocial === "REGISTRADA" && <footer><button type="button" className={estilos.botaoSecundario} onClick={() => iniciarRetificacao(alocacao)} disabled={ocupado}><GitCompareArrows size={14} /> Retificar esta faixa</button>{ehProgramacaoFuturaCancelavel(alocacao, hoje) && <button type="button" className={estilos.botaoPerigoContorno} onClick={() => iniciarCancelamento(alocacao)} disabled={ocupado}><RotateCcw size={14} /> Cancelar programação</button>}</footer>}
            </article>
          ))}</div>
        )}
      </section>

      <section className={estilos.secaoDetalhe}>
        <div className={estilos.tituloSecao}><Search size={18} /><h3>Posição bitemporal da alocação</h3></div>
        <div className={estilos.consultaTemporal}>
          <label><span>Data de referência</span><input type="date" value={dataPosicao} onChange={(e) => { setDataPosicao(e.target.value); setPosicao(null); }} /></label>
          <label><span>Conhecido até (opcional)</span><input type="datetime-local" value={conhecidoAte} onChange={(e) => { setConhecidoAte(e.target.value); setPosicao(null); }} /></label>
          <button type="button" className={estilos.botaoSecundario} onClick={consultarPosicao} disabled={!dataPosicao || consultandoPosicao}>{consultandoPosicao ? "Consultando…" : "Reconstruir no servidor"}</button>
        </div>
        {posicao && <div className={estilos.resultadoTemporal}><strong>{rotularSituacaoTemporalAlocacao(posicao.situacaoTemporal)} · {formatarCombinacao(posicao)}</strong><span>Referência {formatarDataPessoaVinculo(posicao.dataReferencia)} · conhecido até {formatarInstantePessoaVinculo(posicao.conhecidoAte)}</span><p>Faixa #{posicao.alocacaoId || "—"} · evento conhecido mais recente #{posicao.eventoConhecidoMaisRecenteId || "—"}</p></div>}
      </section>

      <section className={estilos.secaoDetalhe}>
        <div className={estilos.tituloSecao}><History size={18} /><h3>Eventos imutáveis de alocação</h3><span>{eventos.length}</span></div>
        {eventos.length === 0 ? <p className={estilos.estadoVazio}>Nenhum evento de alocação visível.</p> : <ol className={`${estilos.linhaTempo} ${estilos.linhaTempoAlocacao}`}>{eventos.map((evento) => (
          <li key={evento.id}><span className={estilos.marcadorEvento} /><article><header><strong>{rotularTipoEventoAlocacao(evento.tipoEvento)}</strong><em>efetivo em {formatarDataPessoaVinculo(evento.dataEfetiva)}</em></header><small>{rotularRelacaoEventoAlocacao(evento.relacaoPrecedencia)} · registrado {formatarInstantePessoaVinculo(evento.registradoEm)} por {evento.responsavelUsername || "sistema"}</small>{evento.justificativa && <p>{evento.justificativa}</p>}<div className={estilos.comparacaoAlocacao}><span>Antes: {formatarCombinacaoEvento(evento, "ANTERIOR")}</span><span>Depois: {formatarCombinacaoEvento(evento, "NOVO")}</span></div></article></li>
        ))}</ol>}
      </section>

      {(modo === "ALTERAR" || modo === "RETIFICAR") && (
        <section className={`${estilos.secaoDetalhe} ${estilos.editorAlocacao}`}>
          <div className={estilos.tituloSecao}><GitCompareArrows size={18} /><h3>{modo === "RETIFICAR" ? `Retificar faixa #${alocacaoAlvo?.id}` : "Nova faixa temporal"}</h3></div>
          <form onSubmit={confirmarMudanca} className={estilos.formularioAlocacao}>
            <label><span>Válido a partir de *</span><input data-foco-inicial type="date" min={vinculo?.dataInicio || undefined} max={vinculo?.dataFim || undefined} value={(dados.dataEfetiva) ?? undefined} disabled={modo === "RETIFICAR" || ocupado} onChange={(e) => mudarData(e.target.value)} /></label>
            <label><span>Lotação *</span><select value={dados.lotacaoId} onChange={(e) => mudarLotacao(e.target.value)} disabled={ocupado || !dados.dataEfetiva || carregandoNivel.LOTACOES}><option value="">{carregandoNivel.LOTACOES ? "Consultando…" : "Selecione"}</option>{lotacoes.map((item) => <option key={item.id} value={item.id}>{item.nome}</option>)}</select></label>
            <label><span>Turno / Jornada *</span><select value={dados.jornadaId} onChange={(e) => mudarJornada(e.target.value)} disabled={ocupado || !dados.lotacaoId || carregandoNivel.JORNADAS}><option value="">{carregandoNivel.JORNADAS ? "Consultando…" : "Selecione"}</option>{jornadas.map((item) => <option key={item.id} value={item.id}>{item.nome}</option>)}</select></label>
            <label><span>Cargo *</span><select value={dados.opcaoChave} onChange={(e) => alterarDado("opcaoChave", e.target.value)} disabled={ocupado || !dados.jornadaId || carregandoNivel.CARGOS}><option value="">{carregandoNivel.CARGOS ? "Consultando…" : "Selecione"}</option>{opcoesCargos.map((opcao) => <option key={chaveOpcao(opcao)} value={chaveOpcao(opcao)}>{opcao.cargoNome}</option>)}</select></label>

            {dados.jornadaId && !carregandoNivel.CARGOS && opcoesCargos.length === 0 && <div className={estilos.avisoIncompatibilidade}><TriangleAlert size={17} /><span>O servidor não encontrou composição com efetivo disponível para este recorte/data. Não há fallback local.</span></div>}
            {opcaoSelecionada && <div className={estilos.capacidadeServidor}><strong>Capacidade informada pelo Java</strong><span>Previsto: {opcaoSelecionada.efetivoPrevisto}</span><span>Ocupação atual: {opcaoSelecionada.ocupacaoAtual}</span><span>Após inclusão: {opcaoSelecionada.ocupacaoAposInclusao}</span><span>Excedente após inclusão: {opcaoSelecionada.excedenteAposInclusao}</span>{opcaoSelecionada.exigeJustificativaExcedente && <em>O servidor sinalizou inclusão excedente; a justificativa auditável abaixo será revalidada no comando.</em>}</div>}

            <label className={estilos.campoLargo}><span>Justificativa *</span><textarea disabled={ocupado} value={dados.justificativa} onChange={(e) => alterarDado("justificativa", e.target.value)} /></label>
            <label className={estilos.campoLargo}><span>Evidência *</span><textarea disabled={ocupado} value={dados.evidencia} onChange={(e) => alterarDado("evidencia", e.target.value)} /></label>
            <label className={estilos.campoLargo}><span>Autorização *</span><textarea disabled={ocupado} value={dados.autorizacao} onChange={(e) => alterarDado("autorizacao", e.target.value)} /></label>

            {exigePrecedenciaUltimoDia && <label className={`${estilos.confirmacao} ${estilos.precedenciaUltimoDia}`}><input type="checkbox" disabled={ocupado} checked={dados.confirmarPrecedenciaUltimoDia} onChange={(e) => alterarDado("confirmarPrecedenciaUltimoDia", e.target.checked)} /><span><strong>Precedência no último dia</strong> Confirmo que a alocação vale neste último dia e o desligamento produz efeito após o dia, conforme regra do servidor.</span></label>}
            <label className={`${estilos.confirmacao} ${estilos.campoLargo}`}><input type="checkbox" disabled={ocupado} checked={dados.confirmarInclusaoExcedente} onChange={(e) => alterarDado("confirmarInclusaoExcedente", e.target.checked)} /> Confirmo a inclusão excedente indicada pelo servidor. Alterar este aceite exige nova prévia.</label>
            {erroFormulario && <p className={estilos.campoLargo} role="alert">{erroFormulario}</p>}

            <div className={`${estilos.areaPrevia} ${estilos.campoLargo}`}>
              <button type="button" className={estilos.botaoSecundario} onClick={gerarPrevia} disabled={carregandoPrevia || ocupado}>{carregandoPrevia ? "Gerando…" : "Gerar prévia de impacto"}</button>
              {previa && <div className={estilos.resultadoPrevia}><strong>Prévia vinculada a esta seleção</strong><p>Data efetiva: {formatarDataPessoaVinculo(previa.dataEfetiva)} · faixa afetada #{previa.alocacaoAfetadaId || "—"}</p>{(previa.impactos || []).map((impacto) => <span key={impacto}>• {impacto}</span>)}{(previa.incompatibilidades || []).map((item) => <span key={item} className={estilos.textoIncompatibilidade}>• {item}</span>)}{(previa.incompatibilidades || []).length === 0 && <label className={estilos.confirmacao}><input type="checkbox" disabled={ocupado} checked={dados.confirmarImpacto} onChange={(e) => setDados((atual) => ({ ...atual, confirmarImpacto: e.target.checked }))} /> Confirmo os impactos desta prévia e o uso do token atual.</label>}</div>}
            </div>

            <footer className={`${estilos.rodapeModal} ${estilos.campoLargo}`}><p><TimerReset size={16} /> Qualquer alteração de data, composição ou justificativa invalida o token e exige nova prévia.</p><div><button type="button" className={estilos.botaoSecundario} onClick={cancelarEditor} disabled={ocupado}>Cancelar</button><button type="submit" className={estilos.botaoPrimario} disabled={ocupado || carregandoPrevia || Boolean(previa?.incompatibilidades?.length)}>{ocupado ? "Processando…" : modo === "RETIFICAR" ? "Confirmar retificação" : "Confirmar alteração"}</button></div></footer>
          </form>
        </section>
      )}

      {modo === "CANCELAR" && (
        <section className={`${estilos.secaoDetalhe} ${estilos.editorAlocacao}`}>
          <div className={estilos.tituloSecao}><RotateCcw size={18} /><h3>Cancelar programação futura #{alocacaoAlvo?.id}</h3></div>
          <p className={estilos.resumoCancelamento}>A API cancelará somente uma faixa futura ainda registrada e restaurará a predecessora quando aplicável. A cadeia original continuará auditável.</p>
          <form onSubmit={confirmarCancelamento} className={estilos.formularioAcao}>
            <label><span>Justificativa *</span><textarea disabled={ocupado} value={dadosCancelamento.justificativa} onChange={(e) => setDadosCancelamento((a) => ({ ...a, justificativa: e.target.value }))} /></label>
            <label><span>Evidência *</span><textarea disabled={ocupado} value={dadosCancelamento.evidencia} onChange={(e) => setDadosCancelamento((a) => ({ ...a, evidencia: e.target.value }))} /></label>
            <label><span>Autorização *</span><textarea disabled={ocupado} value={dadosCancelamento.autorizacao} onChange={(e) => setDadosCancelamento((a) => ({ ...a, autorizacao: e.target.value }))} /></label>
            {erroFormulario && <p role="alert">{erroFormulario}</p>}
            <footer className={estilos.rodapeModal}><p><ShieldCheck size={16} /> Elegibilidade e restauração da faixa anterior são decididas pelo Java.</p><div><button type="button" className={estilos.botaoSecundario} onClick={cancelarEditor} disabled={ocupado}>Voltar</button><button type="submit" className={estilos.botaoPerigo} disabled={ocupado}>{ocupado ? "Processando…" : "Cancelar programação"}</button></div></footer>
          </form>
        </section>
      )}
    </ModalEstrutural>
  );
};
