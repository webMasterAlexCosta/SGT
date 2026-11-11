import { mensagemFalha } from "../../../../core/http/mensagemFalha";
import { observarFalha } from '../../../../core/http/falhaObservada';
import type * as DTO from '../../../../contracts/java';
import type { FrequencyRow, FrequencyContext, FrequencyEmployee, PartialAttendanceState, SubmissionProgress, UserFrequencyScope } from '../../../../contracts/frequencia';
type QueryOperation = ReturnType<ReturnType<typeof useConsultaVigente>['iniciar']>;
import { useState, useEffect, useLayoutEffect, useCallback, useMemo, useRef, useDeferredValue } from "react";
import { useAutenticacao } from "../../../auth/context.tsx";
import { notificar } from "../../../../core/events.ts";
import { useConfirmacaoSenha } from "../../../../core/security/useConfirmacaoSenha.tsx";
import { useConsultaVigente } from "../../../../core/hooks/useConsultaVigente.ts";
import { obterGeracaoSessao, criarErroSessaoObsoleta } from "../../../../core/security/geracaoSessao.ts";
import { CONFIG } from "../../../../core/config.ts";
import { obterDataNoFusoNegocio, interpretarDataHoraNoFusoNegocio } from "../../../../core/formatters/dataHoraNegocio.ts";
import {
  eAdministradorTecnico,
  eGestaoLocal,
  eSuperintendencia,
  eUsuarioOperacional,
  podeAcessarFrequenciaDiaria,
} from "../../../../core/security/roles.ts";
import { ServicoJornada } from "../../../../services/admin/ServicoJornada.ts";
import { ServicoEmpregado } from "../../../../services/rh/ServicoEmpregado.ts";
import { ServicoOcorrencia } from "../../../../services/rh/ServicoOcorrencia.ts";
import { ServicoVinculoContratual } from "../../../../services/rh/ServicoVinculoContratual.ts";
import {
  ServicoFrequencia,
  ServicoCalendarioEvento,
  ServicoExcecaoFrequencia,
  ServicoAdicionalFrequencia,
} from "../../../../services/rh/ServicoFrequencia.ts";
import {
  criarIndiceBusca,
  filtrarFrequenciasPorTermo,
  frequenciaBloqueadaParaOperacional,
  gerarChaveIdempotenciaFrequencia,
  montarPayloadFrequenciaCanonica,
  possuiIdentidadeCanonicaFrequencia,
  executarLotesFrequenciaSequencial,
  montarPayloadFechamento,
  fechamentoEstaAtivo,
  calcularEsperaPolling,
  TAMANHO_MAXIMO_LOTE_FREQUENCIA,
} from "../../../../services/rh/ServicoDominioFrequencia.ts";

const TAMANHO_PAGINA_GRADE = 5;

const extrairMensagemErro = mensagemFalha;

const numeroPositivoOuNulo = (valor: (string)) => {
  const numero = Number(valor);
  return Number.isFinite(numero) && numero > 0 ? numero : null;
};

export const useLogicaFrequencia = () => {
  const { user, atualizarPerfil } = useAutenticacao();
  const eGestorGestaoLocal = useMemo(() => eGestaoLocal(user), [user]);
  const eUsuarioOperacionalAtual = useMemo(() => eUsuarioOperacional(user), [user]);
  const isAdmin = useMemo(() => eAdministradorTecnico(user), [user]);
  // Nome mantido apenas para compatibilidade de props visuais da tela.
  const eGestorOperacional = eGestorGestaoLocal;
  const possuiEscopoGerencial = eGestorGestaoLocal || eSuperintendencia(user);
  const podeRealizarFrequencias = useMemo(() => podeAcessarFrequenciaDiaria(user), [user]);
  const { confirmarComSenha, ModalConfirmacao } = useConfirmacaoSenha();

  const rascunhosNaoSalvosRef = useRef(new Map<number | null, FrequencyRow>());
  const carregamentoRef = useRef<AbortController | null>(null);
  const salvamentoEmCursoRef = useRef<QueryOperation | null>(null);

  const [dataReferenciaAtiva, setDataReferenciaAtiva] = useState(obterDataNoFusoNegocio);
  const [listaDaFrequencia, setListaDaFrequencia] = useState<FrequencyRow[]>([]);
  const [indicadorCarregamento, setIndicadorCarregamento] = useState(false);
  const [erroCargaFrequencia, setErroCargaFrequencia] = useState("");
  const [indicadorSalvamento, setIndicadorSalvamento] = useState(false);
  const [progressoEnvio, setProgressoEnvio] = useState<SubmissionProgress | null>(null);
  const [fechamentoFrequencia, setFechamentoFrequencia] = useState<DTO.FechamentoFrequenciaDTO | null>(null);
  const [carregandoFechamento, setCarregandoFechamento] = useState(false);
  const [erroFechamento, setErroFechamento] = useState("");
  const pollingFechamentoRef = useRef<{ timer: ReturnType<typeof setTimeout> | null; tentativas: number; falhasConsecutivas: number; id: number | null }>({ timer: null, tentativas: 0, falhasConsecutivas: 0, id: null });
  const [statusDaDataValidada, setStatusDaDataValidada] = useState({ valid: true, reason: "" });
  const [horaAtualNoNavegador, setHoraAtualNoNavegador] = useState(Date.now());
  const [justificativaRetificacao, setJustificativaRetificacao] = useState("");
  const intencaoCriacaoFechamentoRef = useRef<{ contexto: string; chaveIdempotencia: string } | null>(null);

  const [contratosBaixados, setContratosBaixados] = useState<import("../../../../contracts/opcoes-apresentacao").ContractView[]>([]);
  const [lotacoesGeraisBaixadas, setLotacoesGeraisBaixadas] = useState<import("../../../../contracts/opcoes-apresentacao").LocationView[]>([]);
  const [jornadasGlobaisBaixadas, setJornadasGlobaisBaixadas] = useState<DTO.JornadaDTO[]>([]);
  const jornadasPorId = useMemo(
    () => new Map(jornadasGlobaisBaixadas.map((jornada) => [String(jornada.id), jornada])),
    [jornadasGlobaisBaixadas],
  );

  const [contratosSelecionadosPeloUsuario, setContratosSelecionadosPeloUsuario] = useState<string[]>([]);
  const [lotacaoSelecionadaPeloUsuario, setLotacaoSelecionadaPeloUsuario] = useState("");
  const [turnoSelecionadoPeloUsuario, setTurnoSelecionadoPeloUsuario] = useState("");
  const [termoDeBuscaDigitado, setTermoDeBuscaDigitado] = useState("");
  const termoDeBuscaDiferido = useDeferredValue(termoDeBuscaDigitado);
  const [paginaGrade, setPaginaGrade] = useState(0);
  const [paginacaoGrade, setPaginacaoGrade] = useState({
    pagina: 0,
    tamanho: TAMANHO_PAGINA_GRADE,
    totalElementos: 0,
    totalPaginas: 0,
  });
  const geracaoSessao = obterGeracaoSessao();
  const contextoOperacao = JSON.stringify([
    dataReferenciaAtiva, lotacaoSelecionadaPeloUsuario, turnoSelecionadoPeloUsuario,
    contratosSelecionadosPeloUsuario, user?.id, user?.perfil, user?.autoridade, geracaoSessao,
  ]);
  const operacoesEnvio = useConsultaVigente(contextoOperacao);
  const consultasGrade = useConsultaVigente(JSON.stringify([contextoOperacao, paginaGrade, termoDeBuscaDiferido]));
  const consultasAuxiliares = useConsultaVigente(geracaoSessao);
  const consultasFechamento = useConsultaVigente(contextoOperacao);
  const operacoesFechamento = useConsultaVigente(contextoOperacao);
  const contextoCriacaoFechamento = JSON.stringify([contextoOperacao, justificativaRetificacao.trim()]);
  const operacoesCriacaoFechamento = useConsultaVigente(contextoCriacaoFechamento);

  const [estadoDoModalParcial, setEstadoDoModalParcial] = useState<PartialAttendanceState>({
    open: false,
    empregadoId: null,
    horas: "",
    saida: "",
    entradaOriginal: "",
    obs: "",
    noturnas: 0,
    saidaManual: false,
  });
  const [tourAberto, setTourAberto] = useState(false);

  useEffect(() => {
    const intervalo = setInterval(() => setHoraAtualNoNavegador(Date.now()), 60000);
    return () => clearInterval(intervalo);
  }, []);

  useEffect(() => {
    atualizarPerfil();
  }, [atualizarPerfil]);

  useEffect(() => () => {
    carregamentoRef.current?.abort();
  }, []);

  useEffect(() => () => {
    if (pollingFechamentoRef.current.timer) clearTimeout(pollingFechamentoRef.current.timer);
  }, []);

  useLayoutEffect(() => {
    rascunhosNaoSalvosRef.current.clear();
    salvamentoEmCursoRef.current = null;
    setIndicadorSalvamento(false);
    setProgressoEnvio(null);
    setJustificativaRetificacao("");
    intencaoCriacaoFechamentoRef.current = null;
    setPaginaGrade(0);
  }, [contextoOperacao]);

  useEffect(() => {
    setPaginaGrade(0);
  }, [termoDeBuscaDiferido]);

  const obterRelatorioDaJanelaDeEdicao = useCallback(
    (prazoServidor: (`${number}-${number}-${number}T${string}`)) => {
      // Nunca inventar prazo de edição no navegador: prazoEdicao é somente informativo no React.
      // A autorização é decidida pelos
      // estados oficiais e validada novamente pelo backend no POST.
      if (!prazoServidor) return { isValid: false, remaining: null, isEndingSoon: false };
      const prazo = interpretarDataHoraNoFusoNegocio(prazoServidor);
      if (!Number.isFinite(prazo)) return { isValid: false, remaining: null, isEndingSoon: false };
      const diferenca = prazo - horaAtualNoNavegador;
      if (diferenca <= 0) return { isValid: false, remaining: "encerrado", isEndingSoon: false };
      const minutosTotais = Math.floor(diferenca / 60000);
      const horas = Math.floor(minutosTotais / 60);
      const minutos = minutosTotais % 60;
      return {
        isValid: true,
        remaining: `${horas > 0 ? `${horas}h ` : ""}${minutos}m`.trim(),
        isEndingSoon: minutosTotais <= CONFIG.FREQUENCIA_EDIT_WARNING_MINUTES,
      };
    },
    [horaAtualNoNavegador],
  );

  const formatarDeslocamentoDeDias = useCallback((dataBase: (string | null), deslocamento: (number)) => {
    const base = new Date(`${dataBase}T12:00:00Z`);
    base.setUTCDate(base.getUTCDate() + deslocamento);
    return base.toISOString().split("T")[0];
  }, []);

  const aoAcionarMudarDataPeloCabecalho = useCallback(
    (deslocamento: (-1) | (number)) => setDataReferenciaAtiva((atual) => formatarDeslocamentoDeDias(atual, deslocamento)),
    [formatarDeslocamentoDeDias],
  );

  useEffect(() => {
    const consulta = consultasAuxiliares.iniciar();
    const carregarAuxiliares = async () => {
      try {
        const [dados, jornadas] = await Promise.all([
          ServicoEmpregado.buscarDadosAuxiliaresFrequencia({ signal: consulta.signal }),
          ServicoJornada.listarTodos({ signal: consulta.signal }).catch(() => []),
        ]);
        if (!consulta.atual()) return;
        setContratosBaixados((Array.isArray(dados?.contratos) ? dados.contratos : []).filter((c) => c.ativo === true));
        setLotacoesGeraisBaixadas(Array.isArray(dados?.lotacoes) ? dados.lotacoes : []);
        setJornadasGlobaisBaixadas(jornadas);
      } catch (erroOriginal) {
      const erro = observarFalha(erroOriginal);

        if (!consulta.atual()) return;
        if (!erro?.canceled && erro?.name !== "CanceledError") console.error("Erro ao carregar dados auxiliares", erroOriginal);
      }
    };
    void carregarAuxiliares();
    return consultasAuxiliares.invalidar;
  }, [consultasAuxiliares, geracaoSessao]);

  const escoposLiberadosDoUsuario = useMemo(() => {
    const brutos = Array.isArray(user?.vinculosGestao) ? user.vinculosGestao : [];
    const mapa = new Map<string, UserFrequencyScope>();
    brutos.forEach((item) => {
      const lotacaoId = String(item?.lotacaoId || item?.lotacao?.id || "");
      const jornadaId = String(item?.jornadaId || item?.jornada?.id || "");
      if (!lotacaoId || !jornadaId) return;
      mapa.set(`${lotacaoId}:${jornadaId}`, {
        lotacaoId,
        jornadaId,
        lotacaoNome: item?.lotacaoNome || item?.lotacao?.label || item?.lotacao?.nome || "",
        jornadaNome: item?.jornadaNome || item?.jornada?.label || item?.jornada?.descricao || "",
      });
    });
    return Array.from(mapa.values());
  }, [user]);

  const listaLotacoesVisiveisAoUsuario = useMemo(() => {
    if (possuiEscopoGerencial) return lotacoesGeraisBaixadas;
    const mapa = new Map<string, { id: string | number | null; nome: string | null; descricao?: string; turnos: import("../../../../contracts/opcoes-apresentacao").ShiftSummaryView[] | null }>();
    escoposLiberadosDoUsuario.forEach((escopo) => {
      const completa = lotacoesGeraisBaixadas.find((l) => String(l.id) === escopo.lotacaoId);
      mapa.set(escopo.lotacaoId, completa || { id: escopo.lotacaoId, nome: escopo.lotacaoNome, turnos: [] });
    });
    return Array.from(mapa.values());
  }, [possuiEscopoGerencial, lotacoesGeraisBaixadas, escoposLiberadosDoUsuario]);

  const listaTurnosVisiveisAoUsuario = useMemo(() => {
    if (!lotacaoSelecionadaPeloUsuario) return [];
    if (possuiEscopoGerencial) {
      return lotacoesGeraisBaixadas.find((l) => String(l.id) === String(lotacaoSelecionadaPeloUsuario))?.turnos || [];
    }
    const mapa = new Map<string, { id: string | number | null; descricao: string | null; nome?: string }>();
    escoposLiberadosDoUsuario
      .filter((e) => e.lotacaoId === String(lotacaoSelecionadaPeloUsuario))
      .forEach((escopo) => {
        const lotacao = lotacoesGeraisBaixadas.find((l) => String(l.id) === escopo.lotacaoId);
        const turno = (lotacao?.turnos || []).find((t) => String(t.id) === escopo.jornadaId);
        mapa.set(escopo.jornadaId, turno || { id: escopo.jornadaId, descricao: escopo.jornadaNome });
      });
    return Array.from(mapa.values());
  }, [possuiEscopoGerencial, lotacaoSelecionadaPeloUsuario, lotacoesGeraisBaixadas, escoposLiberadosDoUsuario]);

  useEffect(() => {
    if (turnoSelecionadoPeloUsuario && !listaTurnosVisiveisAoUsuario.some((t) => String(t.id) === String(turnoSelecionadoPeloUsuario))) {
      setTurnoSelecionadoPeloUsuario("");
    }
    if (listaLotacoesVisiveisAoUsuario.length === 1 && !lotacaoSelecionadaPeloUsuario) {
      setLotacaoSelecionadaPeloUsuario(String(listaLotacoesVisiveisAoUsuario[0].id));
    }
    if (lotacaoSelecionadaPeloUsuario && listaTurnosVisiveisAoUsuario.length === 1 && !turnoSelecionadoPeloUsuario) {
      setTurnoSelecionadoPeloUsuario(String(listaTurnosVisiveisAoUsuario[0].id));
    }
  }, [listaLotacoesVisiveisAoUsuario, lotacaoSelecionadaPeloUsuario, listaTurnosVisiveisAoUsuario, turnoSelecionadoPeloUsuario]);

  useEffect(() => {
    if (!dataReferenciaAtiva) return;
    const hoje = obterDataNoFusoNegocio(horaAtualNoNavegador);
    // Única regra temporal local: não oferecer data futura. Janela retroativa é do backend.
    setStatusDaDataValidada(
      hoje != null && dataReferenciaAtiva > hoje
        ? { valid: false, reason: "future" }
        : { valid: true, reason: "" },
    );
  }, [dataReferenciaAtiva, horaAtualNoNavegador]);

  const construirLinha = useCallback((emp: FrequencyEmployee, contexto: FrequencyContext): FrequencyRow => {
    const {
      mapaFrequencias,
      mapaOcorrencias,
      mapaExcecoes,
      mapaAdicionais,
      tipoCalendario,
      objetoCalendario,
      diaDaSemanaStr,
    } = contexto;
    const logFrequencia = mapaFrequencias.get(emp.id);
    const ocorrencia = mapaOcorrencias.get(emp.id);
    const turnoObj = jornadasPorId.get(String(emp.turnoId));
    let entradaTurno: string | null = null;
    let ehDescansoPelaJornada = false;
    let horarioTrabalhoDoDia = "";

    if (turnoObj?.dias?.length) {
      const diaConfig = turnoObj.dias.find((d) => d.diaSemana === diaDaSemanaStr);
      if (diaConfig) {
        ehDescansoPelaJornada = !diaConfig.diaTrabalho;
        if (diaConfig.diaTrabalho) {
          entradaTurno = diaConfig.entrada1;
          const p1 = diaConfig.entrada1 ? `${diaConfig.entrada1} às ${diaConfig.saida1}` : "";
          const p2 = diaConfig.entrada2 ? `${diaConfig.entrada2} às ${diaConfig.saida2}` : "";
          horarioTrabalhoDoDia = [p1, p2].filter(Boolean).join(" e ");
        } else horarioTrabalhoDoDia = "Folga";
      }
    }

    const temFolga = (mapaExcecoes.get(emp.id) || []).some(
      (ex) => String(ex?.tipo || "").toUpperCase() === "FOLGA_REMUNERADA" && String(ex?.status || "").toUpperCase() !== "CANCELADO",
    );
    const adicionalRT = (mapaAdicionais.get(emp.id) || []).find((ad) => String(ad?.tipo || "").toUpperCase() === "RT");
    const ehFeriadoGlobal = tipoCalendario === "FERIADO";
    const ehDescansoGlobal = tipoCalendario === "DIA_DESCANSO";
    const ehDiaDescansoFinal = ehDescansoPelaJornada || ehFeriadoGlobal || ehDescansoGlobal || temFolga;

    const bloqueadaPeloBackend = !eGestorGestaoLocal && frequenciaBloqueadaParaOperacional({
      ...logFrequencia,
      jaSalvo: Boolean(logFrequencia),
    });
    const vinculoContratualId = possuiIdentidadeCanonicaFrequencia(logFrequencia)
      ? Number(logFrequencia?.vinculoContratualId) : null;

    const linhaBase = {
      empregadoId: emp.id,
      frequenciaId: logFrequencia?.id || null,
      vinculoContratualId,
      identidadeCanonicaDisponivel: Boolean(vinculoContratualId),
      resolucaoCanonicaPermitida: eGestorGestaoLocal && !vinculoContratualId,
      nome: emp.nome,
      cpf: emp.cpf,
      matricula: emp.matricula,
      indiceBusca: criarIndiceBusca(emp),
      lotacao: emp.lotacaoNome || "Sem Lotação",
      lotacaoId: emp.lotacaoId,
      turno: emp.turnoDescricao || "Sem Turno",
      turnoId: emp.turnoId,
      cargo: emp.cargoNome || "-",
      horarioTrabalhoDoDia,
      entradaTurno,
      status: logFrequencia?.status || null,
      obs: logFrequencia?.observacao || "",
      entrada1: logFrequencia?.entrada1 || null,
      saida1: logFrequencia?.saida1 || null,
      entrada2: logFrequencia?.entrada2 || null,
      saida2: logFrequencia?.saida2 || null,
      horasTrabalhadas: logFrequencia?.horasTrabalhadas || "",
      horarioSaida: logFrequencia?.horarioSaida || "",
      horasNoturnas: logFrequencia?.horasNoturnas || 0,
      jaSalvo: Boolean(logFrequencia),
      estadoLancamento: logFrequencia ? "LANCADO" : "PENDENTE",
      prazoEdicao: logFrequencia?.prazoEdicao || null,
      estadoNegocial: logFrequencia?.estadoNegocial || null,
      validadeOperacional: logFrequencia?.validadeOperacional || null,
      estadoAuditoria: logFrequencia?.estadoAuditoria || null,
      bloqueadaEdicaoOperacional: logFrequencia?.bloqueadaEdicaoOperacional === true,
      versaoNegocial: logFrequencia?.versaoNegocial ?? null,
      edicaoBloqueadaPeloBackend: bloqueadaPeloBackend,
      ocorrencia: ocorrencia ? { motivo: ocorrencia.motivo, tipo: ocorrencia.tipoRo } : null,
      diaDescansoCalendario: ehDescansoGlobal || ehFeriadoGlobal || ehDescansoPelaJornada,
      calendarioTipo: tipoCalendario,
      calendarioDescricao: objetoCalendario?.descricao || "",
      diaFeriado: ehFeriadoGlobal,
      folgaRemunerada: temFolga,
      rtQuantidade: adicionalRT?.quantidade ?? null,
      rtJustificativa: adicionalRT?.justificativa || "",
      isDiaDescanso: ehDiaDescansoFinal,
      showRtBadge: ehDiaDescansoFinal && ["RT", "PRESENCA", "PRESENCA_PARCIAL", "ATRASO"].includes(String(logFrequencia?.status || "").toUpperCase()),
      isImplicit: false,
    };

    const rascunho = rascunhosNaoSalvosRef.current.get(emp.id);
    // A edição local conserva sua intenção; identidade e estados oficiais vêm da nova leitura.
    return rascunho ? {
      ...linhaBase,
      status: rascunho.status,
      obs: rascunho.obs,
      horasTrabalhadas: rascunho.horasTrabalhadas,
      horarioSaida: rascunho.horarioSaida,
      horasNoturnas: rascunho.horasNoturnas,
      alterado: true,
      isImplicit: false,
      chaveIdempotencia: rascunho.chaveIdempotencia,
      showRtBadge: linhaBase.isDiaDescanso && ["RT", "PRESENCA", "PRESENCA_PARCIAL", "ATRASO"].includes(String(rascunho.status || "").toUpperCase()),
    } : linhaBase;
  }, [eGestorGestaoLocal, jornadasPorId]);

  const iniciarProcessoDeCargaDaFrequencia = useCallback(async (sinalExterno?: AbortSignal, origem?: QueryOperation) => {
    if (sinalExterno?.aborted || (origem && !origem.atual())) return;
    const consulta = consultasGrade.iniciar();
    if (!consulta.atual()) return;
    carregamentoRef.current?.abort();
    const controlador = new AbortController();
    carregamentoRef.current = controlador;
    const cancelar = () => controlador.abort();
    consulta.signal.addEventListener("abort", cancelar, { once: true });
    if (sinalExterno) sinalExterno.addEventListener("abort", cancelar, { once: true });
    const sinal = controlador.signal;
    const atual = () => consulta.atual() && !sinal.aborted && (!origem || origem.atual());

    if (!lotacaoSelecionadaPeloUsuario || !turnoSelecionadoPeloUsuario) {
      setListaDaFrequencia([]);
      setErroCargaFrequencia("");
      setIndicadorCarregamento(false);
      setPaginacaoGrade({ pagina: 0, tamanho: TAMANHO_PAGINA_GRADE, totalElementos: 0, totalPaginas: 0 });
      consulta.signal.removeEventListener("abort", cancelar);
      sinalExterno?.removeEventListener("abort", cancelar);
      if (carregamentoRef.current === controlador) carregamentoRef.current = null;
      return;
    }

    const filtrosOrganizacionais = {
      lotacaoId: lotacaoSelecionadaPeloUsuario,
      jornadaId: turnoSelecionadoPeloUsuario,
      ...(contratosSelecionadosPeloUsuario.length === 1
        ? { contratoId: Number(contratosSelecionadosPeloUsuario[0]) }
        : {}),
    };

    setIndicadorCarregamento(true);
    setErroCargaFrequencia("");
    try {
      const [paginaEmpregados, frequencias, ocorrencias, excecoes, adicionais, calendarios] = await Promise.all([
        ServicoEmpregado.buscarPaginadosParaFrequencia({
          dataRef: dataReferenciaAtiva,
          lotacaoId: lotacaoSelecionadaPeloUsuario,
          turnoId: turnoSelecionadoPeloUsuario,
          contratoIds: contratosSelecionadosPeloUsuario.length ? contratosSelecionadosPeloUsuario.map(Number) : undefined,
          searchTerm: termoDeBuscaDiferido || undefined,
          page: paginaGrade,
          size: TAMANHO_PAGINA_GRADE,
          skipCount: false,
        }, { signal: sinal, silentError: true, retries: 1 }),
        ServicoFrequencia.buscarDoDia(dataReferenciaAtiva, filtrosOrganizacionais, { signal: sinal, silentError: true, retries: 1 }),
        ServicoOcorrencia.buscarPorData(dataReferenciaAtiva, filtrosOrganizacionais, { signal: sinal, silentError: true, retries: 1 }),
        ServicoExcecaoFrequencia.buscarPorData(dataReferenciaAtiva, filtrosOrganizacionais, { signal: sinal, silentError: true, retries: 1 }),
        ServicoAdicionalFrequencia.buscarPorData(dataReferenciaAtiva, filtrosOrganizacionais, { signal: sinal, silentError: true, retries: 1 }),
        ServicoCalendarioEvento.buscarPorData(dataReferenciaAtiva, {
          signal: sinal,
          silentError: true,
          retries: 1,
          params: { lotacaoId: lotacaoSelecionadaPeloUsuario },
        }),
      ]);
      if (!atual()) return;

      const empregados: DTO.EmpregadoFrequenciaDTO[] = Array.isArray(paginaEmpregados?.content)
        ? paginaEmpregados.content
        : Array.isArray(paginaEmpregados) ? paginaEmpregados : [];
      const totalElementos = Number(paginaEmpregados?.totalElements ?? empregados.length);
      const totalPaginas = Number(paginaEmpregados?.totalPages ?? (totalElementos ? Math.ceil(totalElementos / TAMANHO_PAGINA_GRADE) : 0));
      setPaginacaoGrade({ pagina: paginaGrade, tamanho: TAMANHO_PAGINA_GRADE, totalElementos, totalPaginas });

      const mapaFrequencias = new Map<number, DTO.FrequenciaDTO>();
      (Array.isArray(frequencias) ? frequencias : []).forEach((f) => {
        if (f?.empregado?.id) mapaFrequencias.set(f.empregado.id, f);
      });
      const mapaOcorrencias = new Map<number, DTO.OcorrenciaResumoDTO>();
      (Array.isArray(ocorrencias) ? ocorrencias : []).forEach((o) => {
        if (o?.empregado?.id) mapaOcorrencias.set(o.empregado.id, o);
      });
      const mapaExcecoes = new Map<number, DTO.FrequenciaExcecaoDTO[]>();
      (Array.isArray(excecoes) ? excecoes : []).forEach((e) => {
        const id = e?.empregadoId;
        if (id) mapaExcecoes.set(id, [...(mapaExcecoes.get(id) || []), e]);
      });
      const mapaAdicionais = new Map<number, DTO.FrequenciaAdicionalDTO[]>();
      (Array.isArray(adicionais) ? adicionais : []).forEach((a) => {
        const id = a?.empregadoId;
        if (id) mapaAdicionais.set(id, [...(mapaAdicionais.get(id) || []), a]);
      });
      const objetoCalendario = (Array.isArray(calendarios) ? calendarios : []).find((e) => {
        const tipo = String(e?.tipo || "").toUpperCase();
        return ["FERIADO", "DIA_DESCANSO"].includes(tipo) && e?.ativo !== false;
      });
      const tipoCalendario = objetoCalendario ? String(objetoCalendario.tipo).toUpperCase() : null;
      const diasDaSemanaMap = ["SUNDAY", "MONDAY", "TUESDAY", "WEDNESDAY", "THURSDAY", "FRIDAY", "SATURDAY"];
      const diaDaSemanaStr = diasDaSemanaMap[new Date(`${dataReferenciaAtiva}T12:00:00Z`).getUTCDay()];

      const contexto = { mapaFrequencias, mapaOcorrencias, mapaExcecoes, mapaAdicionais, tipoCalendario, objetoCalendario, diaDaSemanaStr };
      setListaDaFrequencia(empregados.map((emp) => construirLinha(emp, contexto)));
    } catch (erroOriginal) {
      const erro = observarFalha(erroOriginal);

      if (atual() && !erro?.canceled && erro?.name !== "CanceledError") {
        const mensagem = extrairMensagemErro(erro, "Erro ao carregar dados da escala de frequência.");
        setListaDaFrequencia([]);
        setErroCargaFrequencia(mensagem);
        notificar("error", mensagem);
      }
    } finally {
      if (atual()) setIndicadorCarregamento(false);
      if (carregamentoRef.current === controlador) carregamentoRef.current = null;
      consulta.signal.removeEventListener("abort", cancelar);
      sinalExterno?.removeEventListener("abort", cancelar);
    }
  }, [
    construirLinha,
    contratosSelecionadosPeloUsuario,
    dataReferenciaAtiva,
    lotacaoSelecionadaPeloUsuario,
    paginaGrade,
    termoDeBuscaDiferido,
    turnoSelecionadoPeloUsuario,
    consultasGrade,
    geracaoSessao,
  ]);

  // Cada tentativa encerrada pode ter confirmado todo o escopo ou só um prefixo.
  const chaveReconciliacaoFechamento = fechamentoFrequencia?.id && !fechamentoEstaAtivo(fechamentoFrequencia)
    ? JSON.stringify([fechamentoFrequencia.id, fechamentoFrequencia.tentativa, fechamentoFrequencia.status])
    : "";

  useEffect(() => {
    const controlador = new AbortController();
    void iniciarProcessoDeCargaDaFrequencia(controlador.signal);
    return () => controlador.abort();
  }, [iniciarProcessoDeCargaDaFrequencia, chaveReconciliacaoFechamento]);

  const aoRequisitarLimpezaDosFiltros = useCallback(() => {
    setContratosSelecionadosPeloUsuario([]);
    setLotacaoSelecionadaPeloUsuario("");
    setTurnoSelecionadoPeloUsuario("");
    setTermoDeBuscaDigitado("");
    setPaginaGrade(0);
  }, []);

  const linhaPodeSerEditada = useCallback((linha: (FrequencyRow)) => {
    if (!statusDaDataValidada.valid || indicadorSalvamento
      || (!possuiIdentidadeCanonicaFrequencia(linha) && !eGestorGestaoLocal)) return false;
    if (eGestorGestaoLocal) return true;
    return !linha?.edicaoBloqueadaPeloBackend;
  }, [eGestorGestaoLocal, indicadorSalvamento, statusDaDataValidada.valid]);

  const atualizarRascunho = useCallback((idEmpregado: (number | null), transformador: (linha: FrequencyRow) => FrequencyRow) => {
    setListaDaFrequencia((anterior) => {
      const indice = anterior.findIndex((linha) => linha.empregadoId === idEmpregado);
      if (indice < 0) return anterior;
      const atual = anterior[indice];
      if (!linhaPodeSerEditada(atual)) {
        notificar("warning", !statusDaDataValidada.valid
          ? "Data futura não permitida. Selecione hoje ou uma data anterior."
          : !possuiIdentidadeCanonicaFrequencia(atual)
          ? "O servidor não informou a associação desta linha ao vínculo contratual. A edição está indisponível até que o contrato de consulta forneça essa identidade."
          : "Este registro está bloqueado pelos estados oficiais retornados pelo backend.");
        return anterior;
      }
      const modificado = transformador(atual);
      const chaveIdempotencia = eGestorGestaoLocal
        ? (rascunhosNaoSalvosRef.current.get(idEmpregado)?.chaveIdempotencia || gerarChaveIdempotenciaFrequencia(`freq-${idEmpregado}`))
        : null;
      const rascunho = { ...modificado, isImplicit: false, alterado: true, chaveIdempotencia };
      const clone = [...anterior];
      clone[indice] = rascunho;
      rascunhosNaoSalvosRef.current.set(idEmpregado, rascunho);
      return clone;
    });
  }, [eGestorGestaoLocal, linhaPodeSerEditada, statusDaDataValidada.valid]);

  const invocarMudancaDeStatusDoUsuarioNaTabela = useCallback((idEmpregado: (number | null), novoStatus: string) => {
    const statusLimpo = String(novoStatus || "").toUpperCase();
    const linha = listaDaFrequencia.find((item) => item.empregadoId === idEmpregado);
    if (!linha || !linhaPodeSerEditada(linha)) {
      atualizarRascunho(idEmpregado, (item) => item);
      return;
    }
    if (linha.isDiaDescanso && ["FALTA", "PRESENCA"].includes(statusLimpo)) {
      notificar("warning", 'Em dias de descanso e folga na jornada, use "DSR PERDIDO", "FOLGA" ou "RT".');
      return;
    }
    if (statusLimpo === "PRESENCA_PARCIAL") {
      setEstadoDoModalParcial({
        open: true,
        empregadoId: idEmpregado,
        horas: linha.horasTrabalhadas || "",
        saida: linha.horarioSaida || "",
        entradaOriginal: linha.entradaTurno,
        obs: linha.obs || "",
        noturnas: linha.horasNoturnas || 0,
        saidaManual: false,
      });
      return;
    }
    atualizarRascunho(idEmpregado, (item) => ({
      ...item,
      status: statusLimpo,
      showRtBadge: item.isDiaDescanso && ["RT", "PRESENCA", "PRESENCA_PARCIAL", "ATRASO"].includes(statusLimpo),
      horasTrabalhadas: null,
      horarioSaida: null,
      horasNoturnas: 0,
    }));
  }, [linhaPodeSerEditada, listaDaFrequencia, atualizarRascunho]);

  const invocarAlteracaoDeAnotacaoDeObservacao = useCallback((idEmpregado: (number | null), valor: (string)) => {
    atualizarRascunho(idEmpregado, (item) => ({ ...item, obs: valor }));
  }, [atualizarRascunho]);

  const validarFechamentoEConfirmacaoDoModalParcial = useCallback(() => {
    const id = estadoDoModalParcial.empregadoId;
    atualizarRascunho(id, (item) => ({
      ...item,
      status: "PRESENCA_PARCIAL",
      showRtBadge: item.isDiaDescanso,
      horasTrabalhadas: String(estadoDoModalParcial.horas || "").replace(",", "."),
      horarioSaida: estadoDoModalParcial.saida,
      horasNoturnas: estadoDoModalParcial.noturnas,
      obs: estadoDoModalParcial.obs,
    }));
    setEstadoDoModalParcial({ open: false, empregadoId: null, horas: "", saida: "", entradaOriginal: "", obs: "", noturnas: 0, saidaManual: false });
  }, [estadoDoModalParcial, atualizarRascunho]);

  const obterRegistrosPendentes = useCallback(
    () => Array.from(rascunhosNaoSalvosRef.current.values()),
    [],
  );

  const validarPendentes = useCallback((pendentes: (FrequencyRow[])) => {
    if (!pendentes.length) return "Não há alterações explícitas para salvar.";
    if (!eGestorGestaoLocal && pendentes.some((l) => !possuiIdentidadeCanonicaFrequencia(l))) return "Há linhas sem associação canônica informada pelo servidor. A gravação foi bloqueada para preservar a identidade dos colaboradores.";
    if (pendentes.some((l) => !String(l.status || "").trim())) return "Há linhas alteradas sem status.";
    if (pendentes.some((l) => l.status === "PRESENCA_PARCIAL" && (!l.horasTrabalhadas || (!l.horarioSaida && l.entradaTurno) || !l.obs?.trim()))) {
      return "Há marcações de presença parcial incompletas (horas, saída ou justificativa).";
    }
    if (pendentes.some((l) => String(l.status).toUpperCase() === "ATESTADO" && !l.obs?.trim())) {
      return "Há atestados sem justificativa no campo de observação.";
    }
    if (eGestorGestaoLocal && String(justificativaRetificacao || "").trim().length < 5) {
      return "Gestor Local deve informar uma justificativa de retificação com ao menos 5 caracteres.";
    }
    return null;
  }, [eGestorGestaoLocal, justificativaRetificacao]);

  const enviarPendentes = useCallback(async (senhaDigital: string | undefined, operacao: ({ signal: AbortSignal; atual: () => boolean; })) => {
    if (!operacao.atual() || salvamentoEmCursoRef.current) return false;
    const pendentes = obterRegistrosPendentes();
    const erroValidacao = validarPendentes(pendentes);
    if (erroValidacao) {
      notificar(pendentes.length ? "error" : "info", erroValidacao);
      return;
    }

    salvamentoEmCursoRef.current = operacao;
    setIndicadorSalvamento(true);
    try {
      const resolvidos: (FrequencyRow & { vinculoContratualId: number })[] = [];
      for (const linha of pendentes) {
        if (!operacao.atual()) return false;
        const vinculoContratualId = await ServicoVinculoContratual.resolverVinculoAtivoParaFrequencia(linha, {
          data: dataReferenciaAtiva ?? undefined,
          contratoIds: contratosSelecionadosPeloUsuario,
          consultarAssociacaoPorEmpregado: eGestorGestaoLocal,
        }, { signal: operacao.signal, skipLoading: true, silentError: true, retries: 0 });
        if (!operacao.atual()) return false;
        resolvidos.push({ ...linha, vinculoContratualId });
      }
      const payloads = resolvidos.map((linha) => montarPayloadFrequenciaCanonica({
        linha,
        // A data é a de início da jornada, inclusive quando o turno cruza a noite.
        dataReferencia: dataReferenciaAtiva,
        gestorLocal: eGestorGestaoLocal,
        justificativaRetificacao,
        chaveIdempotencia: linha.chaveIdempotencia,
      }));
      // Invalidar a intenção interrompe consultas, próximos lotes e efeitos de
      // interface. Isso não desfaz um POST que o Java já possa ter recebido.
      const enviarSeAtual = <P, R,>(enviar: (payload: P, options?: import("../../../../core/http/types").RequestOptions) => Promise<R>, payload: P): Promise<R> => {
        if (!operacao.atual()) throw criarErroSessaoObsoleta();
        return enviar(payload, { senhaConfirmacao: senhaDigital, skipLoading: true, silentError: true, retries: 0 });
      };
      setProgressoEnvio({ processados: 0, total: payloads.length, loteAtual: 0, totalLotes: Math.ceil(payloads.length / TAMANHO_MAXIMO_LOTE_FREQUENCIA) });
      const resultado = await executarLotesFrequenciaSequencial({
        payloads,
        tamanhoLote: TAMANHO_MAXIMO_LOTE_FREQUENCIA,
        enviarIndividual: (payload) => enviarSeAtual(ServicoFrequencia.registrar, payload),
        enviarLote: (lote) => enviarSeAtual(ServicoFrequencia.registrarLote, lote),
        aoProgresso: (progresso) => { if (operacao.atual()) setProgressoEnvio(progresso); },
      });
      if (!operacao.atual()) return false;
      rascunhosNaoSalvosRef.current.clear();
      setJustificativaRetificacao("");
      notificar("success", `${resultado.totalSalvo.toLocaleString("pt-BR")} frequência(s) salva(s) pelo contrato canônico.`);
      await iniciarProcessoDeCargaDaFrequencia(undefined, operacao);
    } catch (erroOriginal) {
      const erro = observarFalha(erroOriginal);

      if (!operacao.atual()) return false;
      const progresso = erro?.progressoFrequencia;
      if (progresso && progresso.totalSalvo > 0) {
        const idsSalvos = pendentes.slice(0, progresso.totalSalvo).map((item) => item.empregadoId);
        idsSalvos.forEach((id) => rascunhosNaoSalvosRef.current.delete(id));
        notificar(
          "error",
          `${progresso.totalSalvo.toLocaleString("pt-BR")} registro(s) foram salvos em lote(s) anteriores; o lote ${progresso.loteFalhou}/${progresso.totalLotes} falhou. Os registros restantes foram mantidos como rascunho. Não houve retry automático. ${extrairMensagemErro(erro, "")}`.trim(),
        );
        await iniciarProcessoDeCargaDaFrequencia(undefined, operacao);
      } else {
        notificar("error", extrairMensagemErro(erro, "Não foi possível salvar a frequência. Nenhum retry automático foi executado."));
      }
    } finally {
      if (salvamentoEmCursoRef.current === operacao) salvamentoEmCursoRef.current = null;
      if (operacao.atual()) {
        setIndicadorSalvamento(false);
        setProgressoEnvio(null);
      }
    }
  }, [
    dataReferenciaAtiva,
    contratosSelecionadosPeloUsuario,
    eGestorGestaoLocal,
    iniciarProcessoDeCargaDaFrequencia,
    justificativaRetificacao,
    obterRegistrosPendentes,
    validarPendentes,
  ]);

  const invocarGatilhoDeDespachoAoServidorGlobal = useCallback(async () => {
    if (!podeRealizarFrequencias) {
      notificar("error", "Seu perfil não possui permissão para realizar frequências.");
      return;
    }
    if (salvamentoEmCursoRef.current || indicadorSalvamento) {
      notificar("info", "Já existe um envio de frequência em andamento.");
      return;
    }
    if (!statusDaDataValidada.valid) {
      notificar("error", "Data futura não permitida.");
      return;
    }
    const pendentes = obterRegistrosPendentes();
    const erroValidacao = validarPendentes(pendentes);
    if (erroValidacao) {
      notificar(pendentes.length ? "error" : "info", erroValidacao);
      return;
    }
    const operacao = operacoesEnvio.iniciar();
    await confirmarComSenha({
      titulo: "Assinatura Digital Requerida",
      mensagem: `Confirme sua senha para enviar ${pendentes.length.toLocaleString("pt-BR")} alteração(ões) de frequência.`,
      acao: (senhaDigital) => enviarPendentes(senhaDigital, operacao),
    });
  }, [confirmarComSenha, enviarPendentes, indicadorSalvamento, obterRegistrosPendentes, operacoesEnvio, podeRealizarFrequencias, statusDaDataValidada.valid, validarPendentes]);

  const parametrosFechamento = useMemo(() => {
    const lotacaoId = numeroPositivoOuNulo(lotacaoSelecionadaPeloUsuario);
    const jornadaId = numeroPositivoOuNulo(turnoSelecionadoPeloUsuario);
    if (!dataReferenciaAtiva || !lotacaoId || !jornadaId) return null;
    return {
      data: dataReferenciaAtiva ?? undefined,
      lotacaoId,
      jornadaId,
      contratoIds: contratosSelecionadosPeloUsuario.map(Number).filter((id) => Number.isFinite(id) && id > 0),
    };
  }, [contratosSelecionadosPeloUsuario, dataReferenciaAtiva, lotacaoSelecionadaPeloUsuario, turnoSelecionadoPeloUsuario]);

  const pararPollingFechamento = useCallback(() => {
    const atual = pollingFechamentoRef.current;
    if (atual.timer) clearTimeout(atual.timer);
    pollingFechamentoRef.current = { timer: null, tentativas: 0, falhasConsecutivas: 0, id: null };
  }, []);

  const consultarFechamentoPorId = useCallback(async (id: (number | null) | (number), consulta: ({ signal: AbortSignal; atual: () => boolean; })) => {
    if (!id || !consulta?.atual()) return null;
    try {
      const fechamento = await ServicoFrequencia.buscarFechamento(id, { signal: consulta.signal, skipLoading: true, silentError: true });
      if (!consulta.atual()) return null;
      setFechamentoFrequencia(fechamento || null);
      setErroFechamento("");
      pollingFechamentoRef.current.falhasConsecutivas = 0;
      if (!fechamentoEstaAtivo(fechamento)) {
        pararPollingFechamento();
        return fechamento;
      }
      const atual = pollingFechamentoRef.current;
      atual.tentativas += 1;
      atual.id = id;
      const espera = calcularEsperaPolling(atual);
      atual.timer = setTimeout(() => { if (consulta.atual()) void consultarFechamentoPorId(id, consulta); }, espera);
      return fechamento;
    } catch (erroOriginal) {
      const erro = observarFalha(erroOriginal);

      if (!consulta.atual() || erro?.canceled) return null;
      const atual = pollingFechamentoRef.current;
      atual.falhasConsecutivas += 1;
      atual.tentativas += 1;
      atual.id = id;
      setErroFechamento(extrairMensagemErro(erro, "Falha ao atualizar o fechamento."));
      const espera = calcularEsperaPolling(atual);
      atual.timer = setTimeout(() => { if (consulta.atual()) void consultarFechamentoPorId(id, consulta); }, espera);
      return null;
    }
  }, [pararPollingFechamento]);

  const recuperarFechamentoAtual = useCallback(async () => {
    operacoesFechamento.invalidar();
    const consulta = consultasFechamento.iniciar();
    if (!consulta.atual()) return;
    pararPollingFechamento();
    if (!parametrosFechamento) {
      setFechamentoFrequencia(null);
      setErroFechamento("");
      setCarregandoFechamento(false);
      return;
    }
    setCarregandoFechamento(true);
    try {
      const fechamento = await ServicoFrequencia.buscarFechamentoAtual(parametrosFechamento, {
        signal: consulta.signal,
        skipLoading: true,
        silentError: true,
      });
      if (!consulta.atual()) return;
      setFechamentoFrequencia(fechamento || null);
      setErroFechamento("");
      if (fechamentoEstaAtivo(fechamento)) {
        pollingFechamentoRef.current.id = fechamento.id;
        void consultarFechamentoPorId(fechamento.id, consulta);
      }
    } catch (erroOriginal) {
      const erro = observarFalha(erroOriginal);

      if (consulta.atual() && !erro?.canceled) setErroFechamento(extrairMensagemErro(erro, "Não foi possível consultar o fechamento atual."));
    } finally {
      if (consulta.atual()) setCarregandoFechamento(false);
    }
  }, [consultarFechamentoPorId, consultasFechamento, geracaoSessao, operacoesFechamento, parametrosFechamento, pararPollingFechamento]);

  useEffect(() => {
    void recuperarFechamentoAtual();
    return () => { consultasFechamento.invalidar(); pararPollingFechamento(); };
  }, [consultasFechamento, recuperarFechamentoAtual, pararPollingFechamento]);

  const solicitarCriacaoDoFechamento = useCallback(async () => {
    if (!podeRealizarFrequencias) {
      notificar("error", "Seu perfil não possui permissão para iniciar fechamento de frequência.");
      return;
    }
    if (!parametrosFechamento) {
      notificar("warning", "Selecione lotação e turno antes de iniciar o fechamento.");
      return;
    }
    if (rascunhosNaoSalvosRef.current.size > 0) {
      notificar("warning", "Salve ou descarte as alterações em rascunho antes de fechar o escopo.");
      return;
    }
    const justificativa = justificativaRetificacao.trim();
    if (eGestorGestaoLocal && justificativa.length < 5) {
      notificar("warning", "Gestor Local deve informar uma justificativa de retificação com ao menos 5 caracteres.");
      return;
    }
    if (eGestorGestaoLocal && intencaoCriacaoFechamentoRef.current?.contexto !== contextoCriacaoFechamento) {
      intencaoCriacaoFechamentoRef.current = {
        contexto: contextoCriacaoFechamento,
        chaveIdempotencia: gerarChaveIdempotenciaFrequencia("fechamento"),
      };
    }
    const payload = montarPayloadFechamento({
      dataReferencia: dataReferenciaAtiva,
      lotacaoId: parametrosFechamento.lotacaoId,
      jornadaId: parametrosFechamento.jornadaId,
      contratoIds: parametrosFechamento.contratoIds,
      registrosPendentes: [],
    });
    const comando = eGestorGestaoLocal ? {
      ...payload,
      justificativaRetificacao: justificativa,
      chaveIdempotencia: intencaoCriacaoFechamentoRef.current?.chaveIdempotencia,
    } : payload;
    const operacao = operacoesCriacaoFechamento.iniciar();
    await confirmarComSenha({
      titulo: "Iniciar fechamento da frequência",
      mensagem: "O Java validará escopo, janela temporal, capacidade e estado antes de aceitar o processamento assíncrono.",
      acao: async (senhaConfirmacao) => {
        if (!operacao.atual()) return false;
        consultasFechamento.invalidar();
        pararPollingFechamento();
        try {
          const fechamento = await ServicoFrequencia.criarFechamento(comando, { senhaConfirmacao, skipLoading: true, silentError: true });
          if (!operacao.atual()) return false;
          setFechamentoFrequencia(fechamento || null);
          setErroFechamento("");
          if (fechamento?.id && fechamentoEstaAtivo(fechamento)) void consultarFechamentoPorId(fechamento.id, consultasFechamento.iniciar());
          notificar("success", "Fechamento encaminhado ao Java para processamento.");
        } catch (erro) {
          if (!operacao.atual()) return false;
          throw erro;
        }
      },
    });
  }, [confirmarComSenha, consultasFechamento, consultarFechamentoPorId, contextoCriacaoFechamento, dataReferenciaAtiva, eGestorGestaoLocal, justificativaRetificacao, operacoesCriacaoFechamento, parametrosFechamento, pararPollingFechamento, podeRealizarFrequencias]);

  const solicitarCancelamentoDoFechamento = useCallback(async () => {
    const id = fechamentoFrequencia?.id;
    if (!id || !fechamentoEstaAtivo(fechamentoFrequencia)) return;
    const operacao = operacoesFechamento.iniciar();
    if (!operacao.atual()) return;
    consultasFechamento.invalidar();
    pararPollingFechamento();
    try {
      const fechamento = await ServicoFrequencia.cancelarFechamento(id, { skipLoading: true, silentError: true });
      if (!operacao.atual()) return;
      setFechamentoFrequencia(fechamento || fechamentoFrequencia);
      if (fechamento?.id && fechamentoEstaAtivo(fechamento)) void consultarFechamentoPorId(fechamento.id, consultasFechamento.iniciar());
      notificar("success", "Solicitação de cancelamento enviada.");
    } catch (erroOriginal) {
      const erro = observarFalha(erroOriginal);

      if (!operacao.atual()) return;
      notificar("error", extrairMensagemErro(erro, "Não foi possível solicitar o cancelamento."));
      if (fechamentoEstaAtivo(fechamentoFrequencia)) void consultarFechamentoPorId(id, consultasFechamento.iniciar());
    }
  }, [consultasFechamento, consultarFechamentoPorId, fechamentoFrequencia, operacoesFechamento, pararPollingFechamento]);

  const solicitarReprocessamentoDoFechamento = useCallback(async () => {
    const id = fechamentoFrequencia?.id;
    if (!id || !["ERRO", "CANCELADO"].includes(String(fechamentoFrequencia.status || "").toUpperCase())) return;
    const operacao = operacoesFechamento.iniciar();
    await confirmarComSenha({
      titulo: "Reprocessar fechamento",
      mensagem: "O Java revalidará o fechamento antes de uma nova tentativa.",
      acao: async (senhaConfirmacao) => {
        if (!operacao.atual()) return false;
        consultasFechamento.invalidar();
        pararPollingFechamento();
        try {
          const fechamento = await ServicoFrequencia.reprocessarFechamento(id, { senhaConfirmacao, skipLoading: true, silentError: true });
          if (!operacao.atual()) return false;
          setFechamentoFrequencia(fechamento || null);
          if (fechamento?.id) void consultarFechamentoPorId(fechamento.id, consultasFechamento.iniciar());
          notificar("success", "Reprocessamento solicitado.");
        } catch (erro) {
          if (!operacao.atual()) return false;
          throw erro;
        }
      },
    });
  }, [confirmarComSenha, consultasFechamento, consultarFechamentoPorId, fechamentoFrequencia, operacoesFechamento, pararPollingFechamento]);

  const listaDaFrequenciaFiltradaNaMemoria = useMemo(
    () => filtrarFrequenciasPorTermo(listaDaFrequencia, termoDeBuscaDiferido),
    [listaDaFrequencia, termoDeBuscaDiferido],
  );

  const pendenciasSemIdentidadeCanonica = useMemo(
    () => listaDaFrequencia.filter((linha) => !linha.jaSalvo && !linha.identidadeCanonicaDisponivel).length,
    [listaDaFrequencia],
  );

  return {
    estado: {
      tourAberto,
      dataReferenciaAtiva,
      listaDaFrequencia,
      indicadorCarregamento,
      indicadorSalvamento,
      progressoEnvio,
      fechamentoFrequencia,
      fechamentoAtivo: fechamentoEstaAtivo(fechamentoFrequencia),
      carregandoFechamento,
      erroFechamento,
      erroCargaFrequencia,
      statusDaDataValidada,
      estadoDoModalParcial,
      listaDaFrequenciaFiltradaNaMemoria,
      alertaCalendarioPendencias: null,
      paginacaoGrade,
      paginaGrade,
      pendenciasSemIdentidadeCanonica,
    },
    filtros: {
      contratosBaixados,
      listaLotacoesVisiveisAoUsuario,
      listaTurnosVisiveisAoUsuario,
      contratosSelecionadosPeloUsuario,
      lotacaoSelecionadaPeloUsuario,
      turnoSelecionadoPeloUsuario,
      termoDeBuscaDigitado,
      escoposLiberadosDoUsuario,
      justificativaRetificacao,
    },
    acoes: {
      setTourAberto,
      setDataReferenciaAtiva,
      aoAcionarMudarDataPeloCabecalho,
      setContratosSelecionadosPeloUsuario,
      setLotacaoSelecionadaPeloUsuario,
      setTurnoSelecionadoPeloUsuario,
      setTermoDeBuscaDigitado,
      setJustificativaRetificacao,
      setPaginaGrade,
      aoRequisitarLimpezaDosFiltros,
      invocarMudancaDeStatusDoUsuarioNaTabela,
      invocarAlteracaoDeAnotacaoDeObservacao,
      validarFechamentoEConfirmacaoDoModalParcial,
      invocarGatilhoDeDespachoAoServidorGlobal,
      recarregarFrequencia: (origem?: QueryOperation) => iniciarProcessoDeCargaDaFrequencia(undefined, origem),
      solicitarCriacaoDoFechamento,
      solicitarCancelamentoDoFechamento,
      solicitarReprocessamentoDoFechamento,
      recuperarFechamentoAtual,
      setEstadoDoModalParcial,
    },
    auth: {
      user,
      isAdmin,
      eGestorGestaoLocal,
      eGestorOperacional,
      eUsuarioOperacional: eUsuarioOperacionalAtual,
      podeRealizarFrequencias,
    },
    utilitarios: { obterRelatorioDaJanelaDeEdicao },
    ModalConfirmacao,
  };
};
