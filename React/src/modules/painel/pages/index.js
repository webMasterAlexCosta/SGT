import { useCallback, useEffect, useMemo, useRef, useState } from "react";
import { useAutenticacao } from "../../auth/context.jsx";
import { ServicoPainel } from "../../../services/painel/ServicoPainel.js";
import {
  eAdministradorTecnico,
  ePresidente,
  podeAcessarAuditoria,
  podeAcessarFrequenciaDiaria,
  podeAcessarOcorrencias,
  podeAcessarRelatorios,
  podeAcessarRelatoriosAdministrativos,
  podeAcessarVisaoConsolidadaRelatorios,
  podeGerenciarCalendario,
  podeGerenciarEstruturaOrganizacional,
  podeGerenciarUsuarios,
  podeVisualizarContratos,
  podeVisualizarEmpregados,
} from "../../../core/security/roles.js";

const formatarDataLocal = (data) => {
  const ano = data.getFullYear();
  const mes = String(data.getMonth() + 1).padStart(2, "0");
  const dia = String(data.getDate()).padStart(2, "0");
  return `${ano}-${mes}-${dia}`;
};

const subtrairDias = (dataIso, quantidade) => {
  const data = new Date(`${dataIso}T12:00:00`);
  data.setDate(data.getDate() - quantidade);
  return formatarDataLocal(data);
};

const dataOntem = () => {
  const data = new Date();
  data.setDate(data.getDate() - 1);
  return formatarDataLocal(data);
};

const normalizarAtividades = (resposta) => {
  const lista = Array.isArray(resposta) ? resposta : resposta?.content || [];
  return lista.map((item) => ({
    id: item.id,
    dataEvento: item.dataEvento,
    descricao: item.descricao || "Atividade registrada",
    empregadoNome: item.empregadoNome,
    usuarioUsername: item.usuarioUsername || "Sistema",
  }));
};

const aguardar = (ms, signal) => new Promise((resolve, reject) => {
  if (signal?.aborted) {
    reject(Object.assign(new Error("Operação cancelada."), { canceled: true }));
    return;
  }
  const timer = setTimeout(resolve, ms);
  signal?.addEventListener("abort", () => {
    clearTimeout(timer);
    reject(Object.assign(new Error("Operação cancelada."), { canceled: true }));
  }, { once: true });
});

const erroPainel = (falha) => falha?.data?.message || falha?.message || "Não foi possível carregar o painel gerencial.";
const calculoTerminou = (resposta) => Boolean(
  resposta?.concluido || resposta?.status === "CONCLUIDO" || resposta?.status === "ERRO",
);

export const usePainel = (onNavigate) => {
  const { user } = useAutenticacao();
  const presidente = useMemo(() => ePresidente(user), [user]);
  const [referencia, setReferencia] = useState(dataOntem);
  const [periodoDias, setPeriodoDias] = useState(30);
  const [dados, setDados] = useState(null);
  const [atividades, setAtividades] = useState([]);
  const [rankingLotacoes, setRankingLotacoes] = useState([]);
  const [rankingCarregando, setRankingCarregando] = useState(false);
  const [carregando, setCarregando] = useState(true);
  const [erro, setErro] = useState("");
  const [processamento, setProcessamento] = useState({
    id: null, status: "PENDENTE", progresso: 0, mensagem: "", desatualizado: false,
  });
  const controladorRef = useRef(null);
  const controladorRankingRef = useRef(null);
  const rankingCarregandoRef = useRef(false);

  const aplicarCalculo = useCallback((resposta) => {
    if (resposta?.dados) setDados(resposta.dados);
    setProcessamento({
      id: resposta?.id ?? null,
      status: resposta?.status || "PENDENTE",
      progresso: Number(resposta?.progresso || 0),
      mensagem: resposta?.mensagem || "Consolidando indicadores no servidor.",
      desatualizado: Boolean(resposta?.desatualizado),
    });
  }, []);

  const carregarAtividades = useCallback(async (signal) => {
    if (presidente) {
      setAtividades([]);
      return;
    }
    try {
      const resposta = await ServicoPainel.buscarAtividadesRecentes(undefined, { signal });
      if (!signal?.aborted) setAtividades(normalizarAtividades(resposta));
    } catch {
      if (!signal?.aborted) setAtividades([]);
    }
  }, [presidente]);

  const carregar = useCallback(async (signal, forcar = false) => {
    setCarregando(true);
    setErro("");
    setRankingLotacoes([]);
    try {
      let respostaFinal = null;

      // Uma segunda tentativa forçada protege a tela contra payload persistido
      // incompatível após implantação. Nunca repete indefinidamente.
      for (let tentativa = 0; tentativa < 2; tentativa += 1) {
        const forcarTentativa = forcar || tentativa > 0;
        let resposta = await ServicoPainel.solicitarCalculo(referencia, periodoDias, forcarTentativa, { signal });
        aplicarCalculo(resposta);

        let espera = 1_200;
        while (!calculoTerminou(resposta)) {
          if (!resposta?.id) throw new Error("O servidor não retornou o identificador do cálculo do painel.");
          await aguardar(espera, signal);
          resposta = await ServicoPainel.consultarCalculo(resposta.id, { signal });
          aplicarCalculo(resposta);
          espera = Math.min(5_000, Math.round(espera * 1.35));
        }

        if (resposta?.status === "ERRO") {
          throw new Error(resposta?.mensagem || "O cálculo do painel falhou no servidor.");
        }
        if (resposta?.dados) {
          respostaFinal = resposta;
          break;
        }

        if (tentativa === 0) {
          setProcessamento((atual) => ({
            ...atual,
            status: "PENDENTE",
            progresso: 0,
            mensagem: "O cache retornou sem indicadores. Reconstruindo o painel uma vez.",
            desatualizado: false,
          }));
        }
      }

      if (!respostaFinal?.dados) {
        throw new Error("O servidor concluiu o cálculo sem indicadores mesmo após a reconstrução automática.");
      }
      if (!signal?.aborted) {
        setDados(respostaFinal.dados);
        carregarAtividades(signal);
      }
    } catch (falha) {
      if (!signal?.aborted && !falha?.canceled) setErro(erroPainel(falha));
    } finally {
      if (!signal?.aborted) setCarregando(false);
    }
  }, [referencia, periodoDias, aplicarCalculo, carregarAtividades]);

  const cancelarRanking = useCallback(() => {
    controladorRankingRef.current?.abort();
    controladorRankingRef.current = null;
    rankingCarregandoRef.current = false;
    setRankingCarregando(false);
  }, []);

  const iniciar = useCallback((forcar = false) => {
    cancelarRanking();
    setRankingLotacoes([]);
    controladorRef.current?.abort();
    const controlador = new AbortController();
    controladorRef.current = controlador;
    carregar(controlador.signal, forcar);
  }, [cancelarRanking, carregar]);

  useEffect(() => {
    setDados(null);
    iniciar(false);
    return () => {
      controladorRef.current?.abort();
      controladorRankingRef.current?.abort();
    };
  }, [referencia, periodoDias]); // A mudança dos filtros cria uma nova consulta; não depende da identidade de iniciar.

  const carregarRanking = useCallback(async () => {
    if (presidente || rankingCarregandoRef.current) return;

    controladorRankingRef.current?.abort();
    const controlador = new AbortController();
    controladorRankingRef.current = controlador;
    rankingCarregandoRef.current = true;
    setRankingCarregando(true);

    try {
      const inicio = subtrairDias(referencia, periodoDias - 1);
      const resposta = await ServicoPainel.buscarRankingLotacoes(
        { inicio, fim: referencia },
        { signal: controlador.signal },
      );
      if (!controlador.signal.aborted) {
        setRankingLotacoes(Array.isArray(resposta) ? resposta : []);
      }
    } catch (falha) {
      if (!controlador.signal.aborted && !falha?.canceled) {
        setErro(erroPainel(falha));
      }
    } finally {
      if (controladorRankingRef.current === controlador) {
        controladorRankingRef.current = null;
        rankingCarregandoRef.current = false;
        setRankingCarregando(false);
      }
    }
  }, [presidente, referencia, periodoDias]);

  const navegar = useCallback((rota) => {
    if (!rota || rota === "/painel") return;
    onNavigate?.(rota.startsWith("/") ? rota : `/${rota}`);
  }, [onNavigate]);

  return {
    user,
    dados,
    atividades,
    rankingLotacoes,
    rankingCarregando,
    referencia,
    periodoDias,
    carregando,
    erro,
    processamento,
    perfil: {
      presidente,
      administrador: eAdministradorTecnico(user),
      podeOperarFrequencia: podeAcessarFrequenciaDiaria(user),
      podeOperarOcorrencias: podeAcessarOcorrencias(user),
      podeVisualizarContratos: podeVisualizarContratos(user),
      podeVisualizarEmpregados: podeVisualizarEmpregados(user),
      podeAcessarRelatorios: podeAcessarRelatorios(user),
      podeAcessarRelatoriosAdministrativos: podeAcessarRelatoriosAdministrativos(user),
      podeAcessarVisaoConsolidadaRelatorios: podeAcessarVisaoConsolidadaRelatorios(user),
      podeGerenciarUsuarios: podeGerenciarUsuarios(user),
      podeGerenciarCalendario: podeGerenciarCalendario(user),
      podeGerenciarEstrutura: podeGerenciarEstruturaOrganizacional(user),
      podeAcessarAuditoria: podeAcessarAuditoria(user),
    },
    acoes: {
      setReferencia,
      setPeriodoDias: (valor) => setPeriodoDias(Number(valor)),
      recarregar: () => iniciar(true),
      carregarRanking,
      navegar,
    },
    hoje: useMemo(() => formatarDataLocal(new Date()), []),
  };
};
