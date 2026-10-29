import { useState, useEffect, useCallback, useMemo, useRef, useDeferredValue } from "react";
import { useAutenticacao } from "../../../auth/context.jsx";
import { notificar } from "../../../../core/events.js";
import { useConfirmacaoSenha } from "../../../../core/security/useConfirmacaoSenha.jsx";
import { CONFIG } from "../../../../core/config.js";
import {
  eAdministradorTecnico,
  eGestaoLocal,
  eSuperintendencia,
  eUsuarioOperacional,
  podeAcessarFrequenciaDiaria,
} from "../../../../core/security/roles.js";
import { ServicoJornada } from "../../../../services/admin/ServicoJornada.js";
import { ServicoEmpregado } from "../../../../services/rh/ServicoEmpregado.js";
import { ServicoOcorrencia } from "../../../../services/rh/ServicoOcorrencia.js";
import { ServicoVinculoContratual } from "../../../../services/rh/ServicoVinculoContratual.js";
import {
  ServicoFrequencia,
  ServicoCalendarioEvento,
  ServicoExcecaoFrequencia,
  ServicoAdicionalFrequencia,
} from "../../../../services/rh/ServicoFrequencia.js";
import {
  criarIndiceBusca,
  filtrarFrequenciasPorTermo,
  frequenciaBloqueadaParaOperacional,
  gerarChaveIdempotenciaFrequencia,
  montarPayloadFrequenciaCanonica,
  executarLotesFrequenciaSequencial,
  montarPayloadFechamento,
  fechamentoEstaAtivo,
  calcularEsperaPolling,
  TAMANHO_MAXIMO_LOTE_FREQUENCIA,
} from "../../../../services/rh/ServicoDominioFrequencia.js";

const TAMANHO_PAGINA_GRADE = 250;

const obterDataHojeStringLocal = () => {
  const dataAtual = new Date();
  return `${dataAtual.getFullYear()}-${String(dataAtual.getMonth() + 1).padStart(2, "0")}-${String(dataAtual.getDate()).padStart(2, "0")}`;
};

const extrairMensagemErro = (erro, fallback) =>
  erro?.response?.data?.message
  || erro?.response?.data?.mensagem
  || erro?.message
  || fallback;

const numeroPositivoOuNulo = (valor) => {
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

  const rascunhosNaoSalvosRef = useRef(new Map());
  const carregamentoRef = useRef(null);
  const salvamentoEmCursoRef = useRef(false);
  const cacheVinculoFrequenciaRef = useRef(new Map());

  const [dataReferenciaAtiva, setDataReferenciaAtiva] = useState(obterDataHojeStringLocal());
  const [listaDaFrequencia, setListaDaFrequencia] = useState([]);
  const [indicadorCarregamento, setIndicadorCarregamento] = useState(false);
  const [erroCargaFrequencia, setErroCargaFrequencia] = useState("");
  const [indicadorSalvamento, setIndicadorSalvamento] = useState(false);
  const [progressoEnvio, setProgressoEnvio] = useState(null);
  const [fechamentoFrequencia, setFechamentoFrequencia] = useState(null);
  const [carregandoFechamento, setCarregandoFechamento] = useState(false);
  const [erroFechamento, setErroFechamento] = useState("");
  const pollingFechamentoRef = useRef({ timer: null, tentativas: 0, falhasConsecutivas: 0, id: null });
  const [statusDaDataValidada, setStatusDaDataValidada] = useState({ valid: true, reason: "" });
  const [horaAtualNoNavegador, setHoraAtualNoNavegador] = useState(Date.now());
  const [justificativaRetificacao, setJustificativaRetificacao] = useState("");

  const [contratosBaixados, setContratosBaixados] = useState([]);
  const [lotacoesGeraisBaixadas, setLotacoesGeraisBaixadas] = useState([]);
  const [jornadasGlobaisBaixadas, setJornadasGlobaisBaixadas] = useState([]);
  const jornadasPorId = useMemo(
    () => new Map(jornadasGlobaisBaixadas.map((jornada) => [String(jornada.id), jornada])),
    [jornadasGlobaisBaixadas],
  );

  const [contratosSelecionadosPeloUsuario, setContratosSelecionadosPeloUsuario] = useState([]);
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

  const [estadoDoModalParcial, setEstadoDoModalParcial] = useState({
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

  useEffect(() => () => carregamentoRef.current?.abort(), []);

  useEffect(() => () => {
    if (pollingFechamentoRef.current.timer) clearTimeout(pollingFechamentoRef.current.timer);
  }, []);

  useEffect(() => {
    rascunhosNaoSalvosRef.current.clear();
    cacheVinculoFrequenciaRef.current.clear();
    setJustificativaRetificacao("");
    setPaginaGrade(0);
  }, [
    dataReferenciaAtiva,
    lotacaoSelecionadaPeloUsuario,
    turnoSelecionadoPeloUsuario,
    contratosSelecionadosPeloUsuario,
  ]);

  useEffect(() => {
    setPaginaGrade(0);
  }, [termoDeBuscaDiferido]);

  const obterRelatorioDaJanelaDeEdicao = useCallback(
    (prazoServidor) => {
      // Nunca inventar prazo de edição no navegador: prazoEdicao é somente informativo no React.
      // A autorização é decidida pelos
      // estados oficiais e validada novamente pelo backend no POST.
      if (!prazoServidor) return { isValid: false, remaining: null, isEndingSoon: false };
      const diferenca = new Date(prazoServidor).getTime() - horaAtualNoNavegador;
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

  const formatarDeslocamentoDeDias = useCallback((dataBase, deslocamento) => {
    const base = new Date(`${dataBase}T12:00:00Z`);
    base.setUTCDate(base.getUTCDate() + deslocamento);
    return base.toISOString().split("T")[0];
  }, []);

  const aoAcionarMudarDataPeloCabecalho = useCallback(
    (deslocamento) => setDataReferenciaAtiva((atual) => formatarDeslocamentoDeDias(atual, deslocamento)),
    [formatarDeslocamentoDeDias],
  );

  useEffect(() => {
    const controlador = new AbortController();
    const carregarAuxiliares = async () => {
      try {
        const [dados, jornadas] = await Promise.all([
          ServicoEmpregado.buscarDadosAuxiliaresFrequencia({ signal: controlador.signal }),
          ServicoJornada.listarTodos({ signal: controlador.signal }).catch(() => []),
        ]);
        setContratosBaixados((Array.isArray(dados?.contratos) ? dados.contratos : []).filter((c) => c.ativo === true));
        setLotacoesGeraisBaixadas(Array.isArray(dados?.lotacoes) ? dados.lotacoes : []);
        setJornadasGlobaisBaixadas(Array.isArray(jornadas) ? jornadas : jornadas?.content || []);
      } catch (erro) {
        if (!erro?.canceled && erro?.name !== "CanceledError") console.error("Erro ao carregar dados auxiliares", erro);
      }
    };
    void carregarAuxiliares();
    return () => controlador.abort();
  }, []);

  const escoposLiberadosDoUsuario = useMemo(() => {
    const brutos = Array.isArray(user?.vinculosGestao) ? user.vinculosGestao : [];
    const mapa = new Map();
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
    const mapa = new Map();
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
    const mapa = new Map();
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
    const [ano, mes, dia] = dataReferenciaAtiva.split("-").map(Number);
    const selecionada = new Date(ano, mes - 1, dia);
    const hoje = new Date();
    hoje.setHours(0, 0, 0, 0);
    // Única regra temporal local: não oferecer data futura. Janela retroativa é do backend.
    setStatusDaDataValidada(
      selecionada > hoje
        ? { valid: false, reason: "future" }
        : { valid: true, reason: "" },
    );
  }, [dataReferenciaAtiva]);

  const construirLinha = useCallback((emp, contexto) => {
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
    let entradaTurno = null;
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
    const vinculoContratualId = numeroPositivoOuNulo(logFrequencia?.vinculoContratualId);

    const linhaBase = {
      empregadoId: emp.id,
      frequenciaId: logFrequencia?.id || null,
      vinculoContratualId,
      identidadeCanonicaDisponivel: Boolean(vinculoContratualId),
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
    return rascunho ? { ...linhaBase, ...rascunho } : linhaBase;
  }, [eGestorGestaoLocal, jornadasPorId]);

  const iniciarProcessoDeCargaDaFrequencia = useCallback(async (sinalExterno) => {
    carregamentoRef.current?.abort();
    const controlador = new AbortController();
    carregamentoRef.current = controlador;
    if (sinalExterno) sinalExterno.addEventListener("abort", () => controlador.abort(), { once: true });
    const sinal = controlador.signal;

    if (!lotacaoSelecionadaPeloUsuario || !turnoSelecionadoPeloUsuario) {
      setListaDaFrequencia([]);
      setErroCargaFrequencia("");
      setPaginacaoGrade({ pagina: 0, tamanho: TAMANHO_PAGINA_GRADE, totalElementos: 0, totalPaginas: 0 });
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
      if (sinal.aborted) return;

      const empregados = Array.isArray(paginaEmpregados?.content)
        ? paginaEmpregados.content
        : Array.isArray(paginaEmpregados) ? paginaEmpregados : [];
      const totalElementos = Number(paginaEmpregados?.totalElements ?? empregados.length);
      const totalPaginas = Number(paginaEmpregados?.totalPages ?? (totalElementos ? Math.ceil(totalElementos / TAMANHO_PAGINA_GRADE) : 0));
      setPaginacaoGrade({ pagina: paginaGrade, tamanho: TAMANHO_PAGINA_GRADE, totalElementos, totalPaginas });

      const mapaFrequencias = new Map();
      (Array.isArray(frequencias) ? frequencias : []).forEach((f) => {
        if (f?.empregado?.id) mapaFrequencias.set(f.empregado.id, f);
      });
      const mapaOcorrencias = new Map();
      (Array.isArray(ocorrencias) ? ocorrencias : []).forEach((o) => {
        if (o?.empregado?.id) mapaOcorrencias.set(o.empregado.id, o);
      });
      const mapaExcecoes = new Map();
      (Array.isArray(excecoes) ? excecoes : []).forEach((e) => {
        const id = e?.empregadoId ?? e?.empregado?.id;
        if (id) mapaExcecoes.set(id, [...(mapaExcecoes.get(id) || []), e]);
      });
      const mapaAdicionais = new Map();
      (Array.isArray(adicionais) ? adicionais : []).forEach((a) => {
        const id = a?.empregadoId ?? a?.empregado?.id;
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
    } catch (erro) {
      if (!sinal.aborted && !erro?.canceled && erro?.name !== "CanceledError") {
        const mensagem = extrairMensagemErro(erro, "Erro ao carregar dados da escala de frequência.");
        setListaDaFrequencia([]);
        setErroCargaFrequencia(mensagem);
        notificar("error", mensagem);
      }
    } finally {
      if (!sinal.aborted) setIndicadorCarregamento(false);
      if (carregamentoRef.current === controlador) carregamentoRef.current = null;
    }
  }, [
    construirLinha,
    contratosSelecionadosPeloUsuario,
    dataReferenciaAtiva,
    lotacaoSelecionadaPeloUsuario,
    paginaGrade,
    termoDeBuscaDiferido,
    turnoSelecionadoPeloUsuario,
  ]);

  useEffect(() => {
    const controlador = new AbortController();
    void iniciarProcessoDeCargaDaFrequencia(controlador.signal);
    return () => controlador.abort();
  }, [iniciarProcessoDeCargaDaFrequencia]);

  const aoRequisitarLimpezaDosFiltros = useCallback(() => {
    setContratosSelecionadosPeloUsuario([]);
    setLotacaoSelecionadaPeloUsuario("");
    setTurnoSelecionadoPeloUsuario("");
    setTermoDeBuscaDigitado("");
    setPaginaGrade(0);
  }, []);

  const linhaPodeSerEditada = useCallback((linha) => {
    if (!statusDaDataValidada.valid || indicadorSalvamento) return false;
    if (eGestorGestaoLocal) return true;
    return !linha?.edicaoBloqueadaPeloBackend;
  }, [eGestorGestaoLocal, indicadorSalvamento, statusDaDataValidada.valid]);

  const atualizarRascunho = useCallback((idEmpregado, transformador) => {
    setListaDaFrequencia((anterior) => {
      const indice = anterior.findIndex((linha) => linha.empregadoId === idEmpregado);
      if (indice < 0) return anterior;
      const atual = anterior[indice];
      if (!linhaPodeSerEditada(atual)) {
        notificar("warning", "Este registro está bloqueado pelos estados oficiais retornados pelo backend.");
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
  }, [eGestorGestaoLocal, linhaPodeSerEditada]);

  const invocarMudancaDeStatusDoUsuarioNaTabela = useCallback((idEmpregado, novoStatus) => {
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

  const invocarAlteracaoDeAnotacaoDeObservacao = useCallback((idEmpregado, valor) => {
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

  const validarPendentes = useCallback((pendentes) => {
    if (!pendentes.length) return "Não há alterações explícitas para salvar.";
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

  const resolverIdentidadesCanonicasPendentes = useCallback(async (pendentes) => {
    const resolvidos = [];
    for (const linha of pendentes) {
      if (linha.identidadeCanonicaDisponivel && Number(linha.vinculoContratualId) > 0) {
        resolvidos.push(linha);
        continue;
      }
      const chaveCache = `${dataReferenciaAtiva}|${linha.empregadoId}|${contratosSelecionadosPeloUsuario.join(",")}`;
      let vinculoContratualId = cacheVinculoFrequenciaRef.current.get(chaveCache);
      if (!vinculoContratualId) {
        vinculoContratualId = await ServicoVinculoContratual.resolverVinculoAtivoParaFrequencia(
          linha,
          { data: dataReferenciaAtiva, contratoIds: contratosSelecionadosPeloUsuario },
          { skipLoading: true, silentError: true, retries: 0 },
        );
        cacheVinculoFrequenciaRef.current.set(chaveCache, vinculoContratualId);
      }
      const atualizado = { ...linha, vinculoContratualId, identidadeCanonicaDisponivel: true };
      rascunhosNaoSalvosRef.current.set(linha.empregadoId, atualizado);
      resolvidos.push(atualizado);
    }
    return resolvidos;
  }, [contratosSelecionadosPeloUsuario, dataReferenciaAtiva]);

  const enviarPendentes = useCallback(async (senhaDigital) => {
    if (salvamentoEmCursoRef.current) return;
    const pendentesIniciais = obterRegistrosPendentes();
    const erroValidacaoInicial = validarPendentes(pendentesIniciais);
    if (erroValidacaoInicial) {
      notificar(pendentesIniciais.length ? "error" : "info", erroValidacaoInicial);
      return;
    }
    let pendentes;
    try {
      pendentes = await resolverIdentidadesCanonicasPendentes(pendentesIniciais);
    } catch (erro) {
      notificar("error", extrairMensagemErro(erro, "Não foi possível resolver o vínculo contratual exigido pelo Java para uma das linhas alteradas."));
      return;
    }
    const erroValidacao = validarPendentes(pendentes);
    if (erroValidacao) {
      notificar(pendentes.length ? "error" : "info", erroValidacao);
      return;
    }

    const payloads = pendentes.map((linha) => montarPayloadFrequenciaCanonica({
      linha,
      // A data permanece exatamente a data de início da jornada, inclusive turno noturno.
      dataReferencia: dataReferenciaAtiva,
      gestorLocal: eGestorGestaoLocal,
      justificativaRetificacao,
      chaveIdempotencia: linha.chaveIdempotencia,
    }));

    salvamentoEmCursoRef.current = true;
    setIndicadorSalvamento(true);
    setProgressoEnvio({ processados: 0, total: payloads.length, loteAtual: 0, totalLotes: Math.ceil(payloads.length / TAMANHO_MAXIMO_LOTE_FREQUENCIA) });
    try {
      const resultado = await executarLotesFrequenciaSequencial({
        payloads,
        tamanhoLote: TAMANHO_MAXIMO_LOTE_FREQUENCIA,
        enviarIndividual: (payload) => ServicoFrequencia.registrar(payload, {
          senhaConfirmacao: senhaDigital,
          skipLoading: true,
          silentError: true,
          retries: 0,
        }),
        enviarLote: (lote) => ServicoFrequencia.registrarLote(lote, {
          senhaConfirmacao: senhaDigital,
          skipLoading: true,
          silentError: true,
          retries: 0,
        }),
        aoProgresso: setProgressoEnvio,
      });
      rascunhosNaoSalvosRef.current.clear();
      setJustificativaRetificacao("");
      notificar("success", `${resultado.totalSalvo.toLocaleString("pt-BR")} frequência(s) salva(s) pelo contrato canônico.`);
      await iniciarProcessoDeCargaDaFrequencia();
    } catch (erro) {
      const progresso = erro?.progressoFrequencia;
      if (progresso?.totalSalvo > 0) {
        const idsSalvos = pendentes.slice(0, progresso.totalSalvo).map((item) => item.empregadoId);
        idsSalvos.forEach((id) => rascunhosNaoSalvosRef.current.delete(id));
        notificar(
          "error",
          `${progresso.totalSalvo.toLocaleString("pt-BR")} registro(s) foram salvos em lote(s) anteriores; o lote ${progresso.loteFalhou}/${progresso.totalLotes} falhou. Os registros restantes foram mantidos como rascunho. Não houve retry automático. ${extrairMensagemErro(erro, "")}`.trim(),
        );
        await iniciarProcessoDeCargaDaFrequencia();
      } else {
        notificar("error", extrairMensagemErro(erro, "Não foi possível salvar a frequência. Nenhum retry automático foi executado."));
      }
    } finally {
      salvamentoEmCursoRef.current = false;
      setIndicadorSalvamento(false);
      setProgressoEnvio(null);
    }
  }, [
    dataReferenciaAtiva,
    eGestorGestaoLocal,
    iniciarProcessoDeCargaDaFrequencia,
    justificativaRetificacao,
    obterRegistrosPendentes,
    resolverIdentidadesCanonicasPendentes,
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
    await confirmarComSenha({
      titulo: "Assinatura Digital Requerida",
      mensagem: `Confirme sua senha para enviar ${pendentes.length.toLocaleString("pt-BR")} alteração(ões) de frequência.`,
      acao: enviarPendentes,
    });
  }, [confirmarComSenha, enviarPendentes, indicadorSalvamento, obterRegistrosPendentes, podeRealizarFrequencias, statusDaDataValidada.valid, validarPendentes]);

  const parametrosFechamento = useMemo(() => {
    const lotacaoId = numeroPositivoOuNulo(lotacaoSelecionadaPeloUsuario);
    const jornadaId = numeroPositivoOuNulo(turnoSelecionadoPeloUsuario);
    if (!dataReferenciaAtiva || !lotacaoId || !jornadaId) return null;
    return {
      data: dataReferenciaAtiva,
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

  const consultarFechamentoPorId = useCallback(async (id) => {
    if (!id) return null;
    try {
      const fechamento = await ServicoFrequencia.buscarFechamento(id, { skipLoading: true, silentError: true });
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
      atual.timer = setTimeout(() => { void consultarFechamentoPorId(id); }, espera);
      return fechamento;
    } catch (erro) {
      if (erro?.canceled) return null;
      const atual = pollingFechamentoRef.current;
      atual.falhasConsecutivas += 1;
      atual.tentativas += 1;
      atual.id = id;
      setErroFechamento(extrairMensagemErro(erro, "Falha ao atualizar o fechamento."));
      const espera = calcularEsperaPolling(atual);
      atual.timer = setTimeout(() => { void consultarFechamentoPorId(id); }, espera);
      return null;
    }
  }, [pararPollingFechamento]);

  const recuperarFechamentoAtual = useCallback(async () => {
    pararPollingFechamento();
    if (!parametrosFechamento) {
      setFechamentoFrequencia(null);
      setErroFechamento("");
      return;
    }
    setCarregandoFechamento(true);
    try {
      const fechamento = await ServicoFrequencia.buscarFechamentoAtual(parametrosFechamento, {
        skipLoading: true,
        silentError: true,
      });
      setFechamentoFrequencia(fechamento || null);
      setErroFechamento("");
      if (fechamentoEstaAtivo(fechamento)) {
        pollingFechamentoRef.current.id = fechamento.id;
        void consultarFechamentoPorId(fechamento.id);
      }
    } catch (erro) {
      if (!erro?.canceled) setErroFechamento(extrairMensagemErro(erro, "Não foi possível consultar o fechamento atual."));
    } finally {
      setCarregandoFechamento(false);
    }
  }, [consultarFechamentoPorId, parametrosFechamento, pararPollingFechamento]);

  useEffect(() => {
    void recuperarFechamentoAtual();
    return pararPollingFechamento;
  }, [recuperarFechamentoAtual, pararPollingFechamento]);

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
    const payload = montarPayloadFechamento({
      dataReferencia: dataReferenciaAtiva,
      lotacaoId: parametrosFechamento.lotacaoId,
      jornadaId: parametrosFechamento.jornadaId,
      contratoIds: parametrosFechamento.contratoIds,
      registrosPendentes: [],
    });
    await confirmarComSenha({
      titulo: "Iniciar fechamento da frequência",
      mensagem: "O Java validará escopo, janela temporal, capacidade e estado antes de aceitar o processamento assíncrono.",
      acao: async (senhaConfirmacao) => {
        const fechamento = await ServicoFrequencia.criarFechamento(payload, { senhaConfirmacao, skipLoading: true });
        setFechamentoFrequencia(fechamento || null);
        setErroFechamento("");
        if (fechamento?.id && fechamentoEstaAtivo(fechamento)) void consultarFechamentoPorId(fechamento.id);
        notificar("success", "Fechamento encaminhado ao Java para processamento.");
      },
    });
  }, [confirmarComSenha, consultarFechamentoPorId, dataReferenciaAtiva, parametrosFechamento, podeRealizarFrequencias]);

  const solicitarCancelamentoDoFechamento = useCallback(async () => {
    const id = fechamentoFrequencia?.id;
    if (!id || !fechamentoEstaAtivo(fechamentoFrequencia)) return;
    try {
      const fechamento = await ServicoFrequencia.cancelarFechamento(id, { skipLoading: true });
      setFechamentoFrequencia(fechamento || fechamentoFrequencia);
      if (fechamento?.id && fechamentoEstaAtivo(fechamento)) void consultarFechamentoPorId(fechamento.id);
      notificar("success", "Solicitação de cancelamento enviada.");
    } catch (erro) {
      notificar("error", extrairMensagemErro(erro, "Não foi possível solicitar o cancelamento."));
    }
  }, [consultarFechamentoPorId, fechamentoFrequencia]);

  const solicitarReprocessamentoDoFechamento = useCallback(async () => {
    const id = fechamentoFrequencia?.id;
    if (!id || !["ERRO", "CANCELADO"].includes(String(fechamentoFrequencia.status || "").toUpperCase())) return;
    await confirmarComSenha({
      titulo: "Reprocessar fechamento",
      mensagem: "O Java revalidará o fechamento antes de uma nova tentativa.",
      acao: async (senhaConfirmacao) => {
        const fechamento = await ServicoFrequencia.reprocessarFechamento(id, { senhaConfirmacao, skipLoading: true });
        setFechamentoFrequencia(fechamento || null);
        if (fechamento?.id) void consultarFechamentoPorId(fechamento.id);
        notificar("success", "Reprocessamento solicitado.");
      },
    });
  }, [confirmarComSenha, consultarFechamentoPorId, fechamentoFrequencia]);

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
