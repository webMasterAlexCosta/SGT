import {
  useState,
  useEffect,
  useMemo,
  useCallback,
  useRef,
} from "react";
import { ServicoEmpregado } from "../../../../services/rh/ServicoEmpregado.js";
import { ServicoAutenticacao } from "../../../../services/auth/ServicoAutenticacao.js";
import { useConfirmacaoSenha } from "../../../../core/security/useConfirmacaoSenha.jsx";
import { useAutenticacao } from "../../../auth/context.jsx";
import { notificar } from "../../../../core/events.js";
import { CONFIG } from "../../../../core/config.js";
import { possuiAcessoGlobal } from "../../../../core/security/roles";
import {
  normalizarModoVisualizacao,
  obterModoVisualizacaoInicial,
  salvarModoVisualizacao,
} from "./visualizacao.js";
import {
  lerContratosFavoritos,
  salvarContratosFavoritos,
} from "./favoritos.js";
import {
  criarConsultaEmpregados,
  consultaPossuiFiltro,
  normalizarTextoBusca,
  SEM_CONTRATO,
} from "../../../../services/rh/ServicoDominioEmpregado.js";

const obterStatus = (colaborador) =>
  colaborador?.status?.descricao || colaborador?.statusDescricao || "";

const obterContratos = (colaborador) =>
  Array.isArray(colaborador?.contratos) ? colaborador.contratos : [];

export const useEmpregadosLista = () => {
  const { user } = useAutenticacao();
  const eAdministrador = useMemo(() => possuiAcessoGlobal(user), [user]);
  const { confirmarComSenha, ModalConfirmacao } = useConfirmacaoSenha();

  const controladorConsultaRef = useRef(null);
  const sequenciaConsultaRef = useRef(0);
  const versaoDebounceRef = useRef(0);
  const consultaAtualRef = useRef(null);

  const [colaboradores, definirColaboradores] = useState([]);
  const [lotacoesBase, definirLotacoesBase] = useState([]);
  const [contratosBase, definirContratosBase] = useState([]);
  const [lotacaoFiltro, definirLotacaoFiltro] = useState("");
  const [turnoFiltro, definirTurnoFiltro] = useState("");
  const [filtrosGrupoAtivos, definirFiltrosGrupoAtivos] = useState([]);

  const [carregando, definirCarregando] = useState(false);
  const [mensagemCarregamento, definirMensagemCarregamento] = useState("");
  const [duracaoUltimaBuscaMs, definirDuracaoUltimaBuscaMs] = useState(null);
  const [termoBusca, definirTermoBusca] = useState("");
  const [buscaDebounced, definirBuscaDebounced] = useState("");
  const [revisaoConsulta, definirRevisaoConsulta] = useState(0);

  const [modoVisualizacao, definirModoVisualizacao] = useState(
    obterModoVisualizacaoInicial,
  );
  const [filtrosAvancadosAbertos, definirFiltrosAvancadosAbertos] = useState(false);
  const [contratosFavoritos, definirContratosFavoritos] = useState(() =>
    typeof window !== "undefined"
      ? lerContratosFavoritos(window.localStorage)
      : [],
  );

  const [paginaAtual, definirPaginaAtual] = useState(0);
  const [totalPaginas, definirTotalPaginas] = useState(0);
  const [totalElementos, definirTotalElementos] = useState(0);
  const [tamanhoPagina, definirTamanhoPagina] = useState(
    CONFIG.EMPREGADOS_INITIAL_SIZE,
  );
  const [cpfsRevelados, definirCpfsRevelados] = useState({});

  const [modalEdicao, definirModalEdicao] = useState({ aberto: false, id: null });
  const [modalAuditoria, definirModalAuditoria] = useState({
    aberto: false,
    colaborador: null,
  });
  const [modalHistorico, definirModalHistorico] = useState({
    aberto: false,
    colaborador: null,
  });

  useEffect(() => {
    const versao = ++versaoDebounceRef.current;
    const temporizador = window.setTimeout(() => {
      if (versao !== versaoDebounceRef.current) return;
      const busca = normalizarTextoBusca(termoBusca);
      if (busca && tamanhoPagina === 0) {
        definirTamanhoPagina(CONFIG.PAGINATION_DEFAULT_SIZE);
      }
      definirBuscaDebounced(busca);
      definirPaginaAtual(0);
    }, CONFIG.SEARCH_DEBOUNCE_MS);

    return () => window.clearTimeout(temporizador);
  }, [termoBusca, tamanhoPagina]);

  useEffect(() => {
    if (typeof window !== "undefined") {
      salvarModoVisualizacao(window.localStorage, modoVisualizacao);
    }
  }, [modoVisualizacao]);

  useEffect(() => {
    if (typeof window !== "undefined") {
      salvarContratosFavoritos(window.localStorage, contratosFavoritos);
    }
  }, [contratosFavoritos]);

  useEffect(() => {
    const controlador = new AbortController();

    Promise.all([
      ServicoEmpregado.buscarDadosAuxiliaresLista({ signal: controlador.signal }),
    ])
      .then(([auxiliares]) => {
        definirLotacoesBase(auxiliares.lotacoes || []);
        definirContratosBase(auxiliares.contratos || []);
      })
      .catch((erro) => {
        if (!erro?.canceled && erro?.name !== "CanceledError") {
          console.error("Erro ao carregar opções da lista de colaboradores:", erro);
          notificar("error", "Não foi possível carregar lotações e contratos.");
        }
      });

    return () => controlador.abort();
  }, []);

  const consultaAtual = useMemo(
    () =>
      criarConsultaEmpregados({
        pagina: paginaAtual,
        tamanho: tamanhoPagina,
        busca: buscaDebounced,
        lotacaoId: lotacaoFiltro,
        turnoId: turnoFiltro,
        contratos: filtrosGrupoAtivos,
      }),
    [
      paginaAtual,
      tamanhoPagina,
      buscaDebounced,
      lotacaoFiltro,
      turnoFiltro,
      filtrosGrupoAtivos,
    ],
  );

  consultaAtualRef.current = consultaAtual;

  const executarConsulta = useCallback(async (consultaRecebida) => {
    const consulta = criarConsultaEmpregados(consultaRecebida);
    const possuiFiltro = consultaPossuiFiltro(consulta);

    controladorConsultaRef.current?.abort();
    const controlador = new AbortController();
    controladorConsultaRef.current = controlador;
    const sequencia = ++sequenciaConsultaRef.current;

    if (consulta.tamanho === 0 && !possuiFiltro) {
      definirColaboradores([]);
      definirTotalPaginas(0);
      definirTotalElementos(0);
      definirPaginaAtual(0);
      definirCpfsRevelados({});
      definirDuracaoUltimaBuscaMs(null);
      definirCarregando(false);
      definirMensagemCarregamento("");
      return;
    }

    const tamanhoEfetivo = Math.min(
      Math.max(
        consulta.tamanho || CONFIG.PAGINATION_DEFAULT_SIZE,
        1,
      ),
      CONFIG.PAGINATION_MAX_SIZE,
    );

    definirCarregando(true);
    definirMensagemCarregamento(
      tamanhoEfetivo >= 1000
        ? `Consultando até ${tamanhoEfetivo.toLocaleString("pt-BR")} colaboradores...`
        : "Consultando colaboradores...",
    );
    const inicio = performance.now();

    try {
      const resposta = await ServicoEmpregado.buscarResumoLista(
        consulta.pagina,
        {
          size: tamanhoEfetivo,
          search: consulta.busca || undefined,
          lotacaoId: consulta.lotacaoId || undefined,
          turnoId: consulta.turnoId || undefined,
          contratoIds: consulta.contratoIds,
          incluirSemContrato: consulta.incluirSemContrato || undefined,
          sort: "id,asc",
        },
        {
          signal: controlador.signal,
          timeout: Math.max(CONFIG.HTTP_TIMEOUT, 30000),
        },
      );

      if (sequencia !== sequenciaConsultaRef.current) return;

      const conteudo = Array.isArray(resposta) ? resposta : resposta?.content || [];
      const paginas = Array.isArray(resposta) ? 1 : resposta?.totalPages || 0;
      const paginaRecebida = Array.isArray(resposta)
        ? 0
        : resposta?.number ?? consulta.pagina;
      const total = Array.isArray(resposta)
        ? conteudo.length
        : resposta?.totalElements ?? conteudo.length;

      definirColaboradores(conteudo);
      definirTotalPaginas(paginas);
      definirTotalElementos(total);
      definirPaginaAtual(paginaRecebida);
      definirCpfsRevelados({});
      definirDuracaoUltimaBuscaMs(Math.round(performance.now() - inicio));
    } catch (erro) {
      if (
        sequencia === sequenciaConsultaRef.current &&
        erro?.name !== "CanceledError" &&
        erro?.message !== "canceled" &&
        !erro?.canceled
      ) {
        console.error("Falha ao carregar colaboradores:", erro);
        notificar(
          "error",
          erro?.message || "Não foi possível carregar os colaboradores.",
        );
      }
    } finally {
      if (sequencia === sequenciaConsultaRef.current) {
        definirCarregando(false);
        definirMensagemCarregamento("");
      }
    }
  }, []);

  useEffect(() => {
    executarConsulta(consultaAtual);
  }, [consultaAtual, revisaoConsulta, executarConsulta]);

  useEffect(
    () => () => {
      controladorConsultaRef.current?.abort();
      sequenciaConsultaRef.current += 1;
      versaoDebounceRef.current += 1;
    },
    [],
  );

  const carregarColaboradores = useCallback(
    (paginaAlvo = 0, tamanhoCustomizado, pesquisaCustomizada) => {
      const atual = consultaAtualRef.current || criarConsultaEmpregados({
        pagina: 0,
        tamanho: CONFIG.PAGINATION_DEFAULT_SIZE,
      });
      const pagina = Math.max(0, Number(paginaAlvo) || 0);
      const tamanho = tamanhoCustomizado === undefined
        ? atual.tamanho
        : Math.max(0, Number(tamanhoCustomizado) || 0);
      const busca = pesquisaCustomizada === undefined
        ? atual.busca
        : normalizarTextoBusca(pesquisaCustomizada);

      const alterouConsulta =
        pagina !== atual.pagina ||
        tamanho !== atual.tamanho ||
        busca !== atual.busca;

      if (pesquisaCustomizada !== undefined) {
        versaoDebounceRef.current += 1;
        definirTermoBusca(busca);
        definirBuscaDebounced(busca);
      }
      definirPaginaAtual(pagina);
      definirTamanhoPagina(tamanho);

      // Atualizar a página atual após uma edição não muda os filtros. Nesse
      // caso a revisão força uma única nova consulta, sem duplicar a chamada.
      if (!alterouConsulta) {
        definirRevisaoConsulta((valor) => valor + 1);
      }
    },
    [],
  );

  const turnosDisponiveis = useMemo(() => {
    if (!lotacaoFiltro) return [];
    const lotacao = lotacoesBase.find(
      (item) => String(item.id) === String(lotacaoFiltro),
    );
    return Array.isArray(lotacao?.turnos) ? lotacao.turnos : [];
  }, [lotacaoFiltro, lotacoesBase]);

  const lotacaoSelecionadaNome = useMemo(() => {
    const lotacao = lotacoesBase.find(
      (item) => String(item.id) === String(lotacaoFiltro),
    );
    return lotacao?.nome || lotacao?.descricao || "";
  }, [lotacoesBase, lotacaoFiltro]);

  const turnoSelecionadoNome = useMemo(() => {
    const turno = turnosDisponiveis.find(
      (item) => String(item.id) === String(turnoFiltro),
    );
    return turno?.descricao || turno?.nome || "";
  }, [turnosDisponiveis, turnoFiltro]);

  const processamentoTabela = useMemo(() => {
    const mapaGrupos = new Map();
    let ativos = 0;
    let semContrato = 0;

    for (const colaborador of colaboradores) {
      if (String(obterStatus(colaborador)).toUpperCase() === "ATIVO") ativos += 1;
      const contratos = obterContratos(colaborador);

      if (contratos.length === 0) {
        semContrato += 1;
        const grupo = mapaGrupos.get(SEM_CONTRATO) || {
          id: SEM_CONTRATO,
          titulo: "Sem Vínculo Contratual",
          descricao: "Colaboradores sem contrato definido",
          isWarning: true,
          lista: [],
          stats: { total: 0, ativos: 0 },
        };
        grupo.lista.push(colaborador);
        grupo.stats.total += 1;
        if (String(obterStatus(colaborador)).toUpperCase() === "ATIVO") {
          grupo.stats.ativos += 1;
        }
        mapaGrupos.set(SEM_CONTRATO, grupo);
        continue;
      }

      // A regra atual permite somente um contrato vigente por empregado.
      // O Set também impede duplicidade visual caso a resposta contenha dados repetidos.
      const idsProcessados = new Set();
      for (const contrato of contratos) {
        const id = String(contrato?.id || "");
        if (!id || idsProcessados.has(id)) continue;
        idsProcessados.add(id);
        const grupo = mapaGrupos.get(id) || {
          id,
          titulo: `Contrato ${contrato.numeroContrato || id}`,
          descricao: contrato.descricao || "",
          isWarning: false,
          lista: [],
          stats: { total: 0, ativos: 0 },
        };
        grupo.lista.push(colaborador);
        grupo.stats.total += 1;
        if (String(obterStatus(colaborador)).toUpperCase() === "ATIVO") {
          grupo.stats.ativos += 1;
        }
        mapaGrupos.set(id, grupo);
      }
    }

    const gruposBase = [...mapaGrupos.values()].sort((a, b) => {
      if (a.id === SEM_CONTRATO) return 1;
      if (b.id === SEM_CONTRATO) return -1;
      return a.titulo.localeCompare(b.titulo, "pt-BR");
    });

    // O filtro contratual é aplicado no servidor. Não filtramos novamente no
    // navegador, pois isso escondia resultados quando o DTO resumido ainda não
    // havia anexado o contrato ou quando a resposta chegava durante uma troca.
    const gruposParaRenderizar = gruposBase;

    return {
      gruposBase,
      gruposParaRenderizar,
      estatisticasRenderizadas: {
        total: colaboradores.length,
        ativos,
        semContrato,
        comContrato: colaboradores.length - semContrato,
      },
      quantidadePagina: colaboradores.length,
    };
  }, [colaboradores, filtrosGrupoAtivos]);

  const favoritosSet = useMemo(
    () => new Set(contratosFavoritos.map(String)),
    [contratosFavoritos],
  );

  const filtrosDisponiveis = useMemo(() => {
    const contratos = contratosBase
      .map((contrato) => {
        const titulo = `Contrato ${contrato.numeroContrato}`;
        const descricao = contrato.descricao || "";
        return {
          id: String(contrato.id),
          titulo,
          descricao,
          indiceBusca: normalizarTextoBusca(`${titulo} ${descricao}`).toLocaleLowerCase("pt-BR"),
          inativo: contrato.ativo === false,
          isWarning: false,
          favorito: favoritosSet.has(String(contrato.id)),
          quantidadeColaboradores: Number(contrato.quantidadeColaboradores || 0),
        };
      })
      .sort((a, b) => {
        if (a.favorito !== b.favorito) return a.favorito ? -1 : 1;
        return a.titulo.localeCompare(b.titulo, "pt-BR");
      });

    return [
      ...contratos,
      {
        id: SEM_CONTRATO,
        titulo: "Sem Vínculo Contratual",
        descricao: "Colaboradores sem contrato definido",
        indiceBusca: "sem vinculo contratual colaboradores sem contrato definido",
        isWarning: true,
        favorito: false,
        quantidadeColaboradores: null,
      },
    ];
  }, [contratosBase, favoritosSet]);

  const alternarVisualizacaoCpf = useCallback(
    (idColaborador) => {
      if (cpfsRevelados[idColaborador]) {
        definirCpfsRevelados((anterior) => {
          const limpo = { ...anterior };
          delete limpo[idColaborador];
          return limpo;
        });
        return;
      }

      confirmarComSenha({
        titulo: "Acesso a Dados Sensíveis",
        mensagem: "Confirme a sua senha de acesso para visualizar o CPF completo.",
        sempreExigirSenha: true,
        acao: async (senhaInformada) => {
          try {
            await ServicoAutenticacao.validarSenha(senhaInformada);
            definirCpfsRevelados((anterior) => ({
              ...anterior,
              [idColaborador]: true,
            }));
            notificar("success", "Acesso liberado.");
          } catch (erro) {
            notificar("error", erro?.message || "Senha incorreta. Tente novamente.");
            throw erro;
          }
        },
      });
    },
    [cpfsRevelados, confirmarComSenha],
  );

  const aoConfirmarBusca = useCallback(
    (valor) => {
      versaoDebounceRef.current += 1;
      const busca = normalizarTextoBusca(valor);
      definirTermoBusca(busca);
      definirBuscaDebounced(busca);
      definirPaginaAtual(0);
      if (busca && tamanhoPagina === 0) {
        definirTamanhoPagina(CONFIG.PAGINATION_DEFAULT_SIZE);
      }
    },
    [tamanhoPagina],
  );

  const aoMudarTamanhoPagina = useCallback((tamanho) => {
    definirPaginaAtual(0);
    definirTamanhoPagina(Math.max(0, Number(tamanho) || 0));
  }, []);

  const aoAplicarFiltrosAvancados = useCallback(
    ({ lotacaoId, turnoId, tamanho }) => {
      const tamanhoNormalizado = Math.max(0, Number(tamanho) || 0);
      const possuiFiltro = Boolean(lotacaoId || turnoId || filtrosGrupoAtivos.length || buscaDebounced);
      definirPaginaAtual(0);
      definirLotacaoFiltro(lotacaoId ? String(lotacaoId) : "");
      definirTurnoFiltro(turnoId ? String(turnoId) : "");
      definirTamanhoPagina(
        tamanhoNormalizado === 0 && possuiFiltro
          ? CONFIG.PAGINATION_DEFAULT_SIZE
          : tamanhoNormalizado,
      );
    },
    [filtrosGrupoAtivos.length, buscaDebounced],
  );

  const aoAlternarFiltro = useCallback(
    (id) => {
      definirPaginaAtual(0);
      if (id === "limpar") {
        definirFiltrosGrupoAtivos([]);
        return;
      }

      const normalizado = String(id);
      // O navegador apresenta um contrato por vez. Isso evita acumular filtros
      // invisíveis e deixa a navegação coerente com a regra de contrato vigente único.
      definirFiltrosGrupoAtivos((anteriores) =>
        anteriores.length === 1 && anteriores[0] === normalizado
          ? []
          : [normalizado],
      );
      if (tamanhoPagina === 0) {
        definirTamanhoPagina(CONFIG.PAGINATION_DEFAULT_SIZE);
      }
    },
    [tamanhoPagina],
  );

  const aoLimparBuscaEFiltros = useCallback(() => {
    controladorConsultaRef.current?.abort();
    sequenciaConsultaRef.current += 1;
    versaoDebounceRef.current += 1;

    definirTermoBusca("");
    definirBuscaDebounced("");
    definirFiltrosGrupoAtivos([]);
    definirLotacaoFiltro("");
    definirTurnoFiltro("");
    definirPaginaAtual(0);
    definirTamanhoPagina(CONFIG.PAGINATION_DEFAULT_SIZE);
    definirColaboradores([]);
    definirTotalPaginas(0);
    definirTotalElementos(0);
    definirCpfsRevelados({});
    definirDuracaoUltimaBuscaMs(null);
    definirCarregando(false);
    definirMensagemCarregamento("");
    definirRevisaoConsulta((valor) => valor + 1);
  }, []);

  const aoMudarModoVisualizacao = useCallback((modo) => {
    definirModoVisualizacao(normalizarModoVisualizacao(modo));
  }, []);

  const aoAlternarFiltrosAvancados = useCallback(() => {
    definirFiltrosAvancadosAbertos((anterior) => !anterior);
  }, []);

  const aoFecharFiltrosAvancados = useCallback(() => {
    definirFiltrosAvancadosAbertos(false);
  }, []);

  const aoAlternarFavoritoContrato = useCallback((idContrato) => {
    const id = String(idContrato);
    if (!id || id === SEM_CONTRATO) return;
    definirContratosFavoritos((anteriores) =>
      anteriores.includes(id)
        ? anteriores.filter((item) => item !== id)
        : [...anteriores, id],
    );
  }, []);

  const acoesMemoizadas = useMemo(
    () => ({
      definirTermoBusca,
      definirBuscaDebounced: aoConfirmarBusca,
      carregarColaboradores,
      alternarVisualizacaoCpf,
      aoMudarTamanhoPagina,
      aoAplicarFiltrosAvancados,
      aoAlternarFiltro,
      aoLimparBuscaEFiltros,
      aoMudarModoVisualizacao,
      aoAlternarFiltrosAvancados,
      aoFecharFiltrosAvancados,
      aoAlternarFavoritoContrato,
    }),
    [
      aoConfirmarBusca,
      carregarColaboradores,
      alternarVisualizacaoCpf,
      aoMudarTamanhoPagina,
      aoAplicarFiltrosAvancados,
      aoAlternarFiltro,
      aoLimparBuscaEFiltros,
      aoMudarModoVisualizacao,
      aoAlternarFiltrosAvancados,
      aoFecharFiltrosAvancados,
      aoAlternarFavoritoContrato,
    ],
  );

  const modaisMemoizados = useMemo(
    () => ({
      edicao: modalEdicao,
      definirEdicao: definirModalEdicao,
      auditoria: modalAuditoria,
      definirAuditoria: definirModalAuditoria,
      historico: modalHistorico,
      definirHistorico: definirModalHistorico,
    }),
    [modalEdicao, modalAuditoria, modalHistorico],
  );

  return {
    estado: {
      colaboradores,
      carregando,
      mensagemCarregamento,
      duracaoUltimaBuscaMs,
      termoBusca,
      tamanhoPagina,
      paginaAtual,
      totalPaginas,
      totalElementos,
      cpfsRevelados,
      filtrosGrupoAtivos,
      lotacaoFiltro,
      turnoFiltro,
      lotacaoSelecionadaNome,
      turnoSelecionadoNome,
      modoVisualizacao,
      filtrosAvancadosAbertos,
      contratosFavoritos,
    },
    processamento: {
      gruposBase: processamentoTabela.gruposBase,
      gruposParaRenderizar: processamentoTabela.gruposParaRenderizar,
      estatisticasRenderizadas: processamentoTabela.estatisticasRenderizadas,
      quantidadePagina: processamentoTabela.quantidadePagina,
      filtrosDisponiveis,
      eAdministrador,
      lotacoesBase,
      turnosDisponiveis,
      quantidadeFavoritos: contratosFavoritos.length,
      quantidadeContratos: contratosBase.length,
    },
    acoes: acoesMemoizadas,
    modais: modaisMemoizados,
    ModalConfirmacao,
  };
};
