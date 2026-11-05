import React, { useCallback, useEffect, useMemo, useState } from "react";
import { Eye } from "lucide-react";
import { ServicoContrato } from "../../../../services/admin/ServicoContrato";
import { ServicoOrganizacional } from "../../../../services/admin/ServicoOrganizacional";
import { ServicoRecursoGenerico } from "../../../../services/admin/ServicoRecursoGenerico.js";
import { useConfirmacaoSenha } from "../../../../core/security/useConfirmacaoSenha.jsx";
import { useAutenticacao } from "../../../auth/context";
import { eGestaoLocal, ePresidente, podeGerenciarContratos, possuiAcessoGlobal } from "../../../../core/security/roles";
import { notificar } from "../../../../core/events";
import { useConsultaVigente } from "../../../../core/hooks/useConsultaVigente.js";
import { formatarGestaoLocal } from "../../../../core/formatters/nomenclaturaGestao.js";
import { CabecalhoContratos } from "./components/CabecalhoContratos";
import { IndicadoresContratos } from "./components/IndicadoresContratos";
import { FiltrosContratos } from "./components/FiltrosContratos";
import { ListaContratos } from "./components/ListaContratos";
import { ModalContrato } from "./components/ModalContrato";
import { ModalAcaoContrato } from "./components/ModalAcaoContrato";
import { ModalDetalhesContrato } from "./components/ModalDetalhesContrato";
import {
  calcularIndicadoresDosContratos,
  ehRascunho,
  filtrarContratos,
  obterUfsDisponiveis,
  prepararContratoRascunho,
} from "../../../../services/admin/ServicoDominioContrato.js";
import estilos from "./index.module.css";

const ServicoTipoContratacao = ServicoRecursoGenerico("/tipos-contratacao");

const FORMULARIO_VAZIO = Object.freeze({
  id: "",
  numeroContrato: "",
  anoContrato: "",
  descricao: "",
  tipoContratacaoId: "",
  dataInicio: "",
  dataFim: "",
  gestaoLocalId: "",
  efetivoPrevisto: 0,
  cnpjEmpresa: "",
  razaoSocialEmpresa: "",
  nomeFantasiaEmpresa: "",
  matriculaVinculoObrigatoria: false,
});

export const PaginaContratos = () => {
  const { user } = useAutenticacao();
  const { confirmarComSenha, ModalConfirmacao, fechar: fecharConfirmacaoSenha } = useConfirmacaoSenha();

  const [contratos, setContratos] = useState([]);
  const [gestoesLocais, setGestoesLocais] = useState([]);
  const [tiposContratacao, setTiposContratacao] = useState([]);
  const [carregando, setCarregando] = useState(false);
  const [salvando, setSalvando] = useState(false);
  const [modalAberto, setModalAberto] = useState(false);
  const [dadosFormulario, setDadosFormulario] = useState({ ...FORMULARIO_VAZIO });

  const [detalheAberto, setDetalheAberto] = useState(false);
  const [contratoDetalhe, setContratoDetalhe] = useState(null);
  const [eventos, setEventos] = useState([]);
  const [precedencias, setPrecedencias] = useState([]);
  const [versoes, setVersoes] = useState([]);
  const [carregandoDetalhe, setCarregandoDetalhe] = useState(false);
  const [erroDetalhe, setErroDetalhe] = useState(null);
  const [posicaoTemporal, setPosicaoTemporal] = useState(null);
  const [consultandoTemporal, setConsultandoTemporal] = useState(false);
  const [acaoAtual, setAcaoAtual] = useState(null);
  const [processandoAcao, setProcessandoAcao] = useState(false);
  const consultaDetalhe = useConsultaVigente();
  const consultaTemporal = useConsultaVigente();
  const invalidarPosicaoTemporal = useCallback(() => {
    consultaTemporal.invalidar();
    setPosicaoTemporal(null);
    setConsultandoTemporal(false);
  }, [consultaTemporal]);

  const [termoBusca, setTermoBusca] = useState("");
  const [filtroStatus, setFiltroStatus] = useState("TODOS");
  const [filtroUf, setFiltroUf] = useState("TODAS");
  const [filtroGestaoLocal, setFiltroGestaoLocal] = useState("TODAS");
  const [filtroVigencia, setFiltroVigencia] = useState("TODAS");

  const podeGerenciar = useMemo(() => podeGerenciarContratos(user), [user]);
  const acessoGlobal = useMemo(() => possuiAcessoGlobal(user), [user]);
  const presidente = useMemo(() => ePresidente(user), [user]);
  const acessoNacionalContratos = acessoGlobal || presidente;
  const perfilGestaoLocal = useMemo(() => eGestaoLocal(user), [user]);

  const gestaoLocalDoUsuario = useMemo(() => ({
    id: user?.gestaoLocal?.id || user?.gestaoLocalId || null,
    nome: user?.gestaoLocal?.label || user?.gestaoLocal?.nome || user?.gestaoLocalNome || null,
    uf: user?.gestaoLocal?.uf || user?.uf || null,
    superintendenciaNome: user?.superintendencia?.nome || user?.superintendenciaNome || null,
  }), [user]);

  const carregarDados = useCallback(async () => {
    setCarregando(true);
    try {
      const promessas = [ServicoContrato.listarTodos(), ServicoOrganizacional.listarGestoesLocais()];
      if (podeGerenciar) promessas.push(ServicoTipoContratacao.listarTodos());
      const [listaContratos = [], listaGestoesLocais = [], listaTipos = []] = await Promise.all(promessas);
      setContratos(listaContratos);
      setGestoesLocais(listaGestoesLocais);
      setTiposContratacao(listaTipos);
    } catch (erro) {
      if (!erro?.canceled) notificar("error", "Não foi possível carregar os contratos do seu escopo.");
    } finally {
      setCarregando(false);
    }
  }, [podeGerenciar]);

  useEffect(() => { carregarDados(); }, [carregarDados]);

  const carregarDetalhes = useCallback(async (id, { silencioso = false } = {}) => {
    if (!id) return;
    const consulta = consultaDetalhe.iniciar();
    setCarregandoDetalhe(true);
    setErroDetalhe(null);
    setEventos([]);
    setPrecedencias([]);
    setVersoes([]);
    if (!silencioso) invalidarPosicaoTemporal();
    try {
      const [detalhe, listaEventos, listaPrecedencias, listaVersoes] = await Promise.all([
        ServicoContrato.buscarPorId(id, { signal: consulta.signal }),
        ServicoContrato.listarEventos(id, { signal: consulta.signal }),
        ServicoContrato.listarPrecedencias(id, { signal: consulta.signal }),
        ServicoContrato.listarVersoes(id, { signal: consulta.signal }),
      ]);
      if (!consulta.atual()) return;
      setContratoDetalhe(detalhe);
      setEventos(listaEventos);
      setPrecedencias(listaPrecedencias);
      setVersoes(listaVersoes);
    } catch (erro) {
      if (consulta.atual()) {
        setErroDetalhe("Não foi possível carregar o ciclo de vida do contrato. Feche e reabra o detalhe para tentar novamente.");
        if (!erro?.canceled) notificar("error", "Não foi possível carregar o ciclo de vida do contrato.");
      }
    } finally {
      if (consulta.atual()) setCarregandoDetalhe(false);
    }
  }, [consultaDetalhe, invalidarPosicaoTemporal]);

  const criarFormularioVazio = useCallback(() => ({
    ...FORMULARIO_VAZIO,
    gestaoLocalId: gestaoLocalDoUsuario.id ? String(gestaoLocalDoUsuario.id) : (gestoesLocais.length === 1 ? String(gestoesLocais[0].id) : ""),
  }), [gestaoLocalDoUsuario.id, gestoesLocais]);

  const abrirNovoContrato = () => { setDadosFormulario(criarFormularioVazio()); setModalAberto(true); };

  const abrirEdicaoDoContrato = (contrato) => {
    if (!podeGerenciar || !ehRascunho(contrato)) {
      notificar("warning", "Somente rascunhos podem ser editados livremente.");
      return;
    }
    consultaDetalhe.invalidar();
    invalidarPosicaoTemporal();
    setCarregandoDetalhe(false);
    setDetalheAberto(false);
    setDadosFormulario({
      ...FORMULARIO_VAZIO,
      ...contrato,
      anoContrato: contrato.anoContrato ?? "",
      tipoContratacaoId: contrato.tipoContratacaoId ? String(contrato.tipoContratacaoId) : "",
      gestaoLocalId: contrato.gestaoLocalId ? String(contrato.gestaoLocalId) : "",
      dataInicio: contrato.dataInicio ? String(contrato.dataInicio).split("T")[0] : "",
      dataFim: contrato.dataFim ? String(contrato.dataFim).split("T")[0] : "",
    });
    setModalAberto(true);
  };

  const fecharModal = useCallback(() => {
    if (salvando) return;
    setModalAberto(false);
    setDadosFormulario(criarFormularioVazio());
  }, [criarFormularioVazio, salvando]);

  const alterarCampoDoFormulario = useCallback((campo, valor) => setDadosFormulario((estado) => ({ ...estado, [campo]: valor })), []);

  const validarRascunho = () => {
    if (dadosFormulario.anoContrato && (Number(dadosFormulario.anoContrato) < 1900 || Number(dadosFormulario.anoContrato) > 9999)) return "Informe um ano entre 1900 e 9999.";
    if (dadosFormulario.dataInicio && dadosFormulario.dataFim && dadosFormulario.dataFim < dadosFormulario.dataInicio) return "A data de término não pode ser anterior à data de início.";
    const cnpj = String(dadosFormulario.cnpjEmpresa || "").replace(/\D/g, "");
    if (cnpj && cnpj.length !== 14) return "CNPJ deve conter 14 dígitos quando informado.";
    return null;
  };

  const salvarContrato = async (evento) => {
    evento.preventDefault();
    if (!podeGerenciar || salvando) return;
    const mensagem = validarRascunho();
    if (mensagem) { notificar("warning", mensagem); return; }
    const gestaoLocalId = gestaoLocalDoUsuario.id || dadosFormulario.gestaoLocalId;
    if (!gestaoLocalId) { notificar("warning", "A Gestão Local responsável é obrigatória."); return; }
    const payload = prepararContratoRascunho(dadosFormulario, gestaoLocalId);
    const estaEditando = Boolean(dadosFormulario.id);

    await confirmarComSenha({
      titulo: estaEditando ? "Confirmar alteração do rascunho" : "Confirmar criação do rascunho",
      mensagem: "Rascunhos podem estar incompletos. A publicação fará a validação integral do contrato.",
      acao: async (senhaConfirmacao) => {
        setSalvando(true);
        try {
          if (estaEditando) await ServicoContrato.atualizar(dadosFormulario.id, payload, { senhaConfirmacao });
          else await ServicoContrato.criar(payload, { senhaConfirmacao });
          setModalAberto(false);
          setDadosFormulario(criarFormularioVazio());
          await carregarDados();
        } finally { setSalvando(false); }
      },
    });
  };

  const abrirDetalhes = (contrato) => {
    setContratoDetalhe(contrato);
    setDetalheAberto(true);
    setPosicaoTemporal(null);
    carregarDetalhes(contrato.id);
  };

  const fecharDetalhes = () => {
    if (processandoAcao) return;
    consultaDetalhe.invalidar();
    invalidarPosicaoTemporal();
    setCarregandoDetalhe(false);
    setDetalheAberto(false);
  };

  const consultarTemporal = async ({ data, conhecidoAte }) => {
    if (!contratoDetalhe?.id || !data) return;
    const consulta = consultaTemporal.iniciar();
    setPosicaoTemporal(null);
    setConsultandoTemporal(true);
    try {
      const posicao = await ServicoContrato.consultarPosicaoTemporal(contratoDetalhe.id, { data, conhecidoAte }, { signal: consulta.signal, silentError: true });
      if (consulta.atual()) setPosicaoTemporal(posicao);
    } catch (erro) {
      if (consulta.atual() && !erro?.canceled) notificar("error", "Não foi possível consultar a posição temporal do contrato.");
    } finally {
      if (consulta.atual()) setConsultandoTemporal(false);
    }
  };

  const descartarRascunho = async (contrato) => {
    if (!podeGerenciar || !ehRascunho(contrato)) return;
    await confirmarComSenha({
      titulo: `Descartar rascunho ${contrato.numeroContrato || "sem número"}?`,
      mensagem: "O servidor permitirá exclusão física somente se o rascunho nunca foi publicado e não possui fatos/vínculos impeditivos.",
      acao: async (senhaConfirmacao) => {
        setProcessandoAcao(true);
        try {
          await ServicoContrato.excluir(contrato.id, { senhaConfirmacao });
          consultaDetalhe.invalidar();
          invalidarPosicaoTemporal();
          setCarregandoDetalhe(false);
          setDetalheAberto(false);
          await carregarDados();
        } finally { setProcessandoAcao(false); }
      },
    });
  };

  const abrirAcaoCicloVida = (tipo, contrato) => {
    if (tipo === "DESCARTAR") { descartarRascunho(contrato); return; }
    setAcaoAtual({ tipo, contrato });
  };

  const executarAcaoCicloVida = async (dados, aoErro) => {
    const atual = acaoAtual;
    if (!atual || processandoAcao) return;
    const { tipo, contrato } = atual;
    const executar = async (senhaConfirmacao) => {
      const options = { senhaConfirmacao };
      if (tipo === "PUBLICAR") return ServicoContrato.publicar(contrato.id, dados, options);
      if (tipo === "INATIVAR") return ServicoContrato.inativar(contrato.id, dados, options);
      if (tipo === "REATIVAR") return ServicoContrato.reativar(contrato.id, dados, options);
      if (tipo === "ENCERRAR_ANTECIPADAMENTE") return ServicoContrato.encerrarAntecipadamente(contrato.id, dados, options);
      if (tipo === "CANCELAR_FUTURO") return ServicoContrato.cancelarFuturo(contrato.id, dados, options);
      if (tipo === "ANULAR") return ServicoContrato.anular(contrato.id, dados, options);
      throw new Error(`Ação contratual não suportada nesta etapa: ${tipo}`);
    };

    return confirmarComSenha({
      titulo: "Confirmar evento contratual",
      mensagem: "O evento será persistido com datas efetiva/do ato/registro distintas conforme o contrato do Java.",
      acao: async (senhaConfirmacao) => {
        setProcessandoAcao(true);
        try {
          await executar(senhaConfirmacao);
          setAcaoAtual(null);
          await Promise.all([carregarDados(), carregarDetalhes(contrato.id, { silencioso: true })]);
        } catch (erro) {
          if (aoErro?.(erro)) fecharConfirmacaoSenha();
          throw erro;
        } finally { setProcessandoAcao(false); }
      },
    });
  };

  const indicadores = useMemo(() => calcularIndicadoresDosContratos(contratos), [contratos]);
  const ufsDisponiveis = useMemo(() => obterUfsDisponiveis(contratos, acessoNacionalContratos), [contratos, acessoNacionalContratos]);
  const gestoesLocaisDisponiveis = useMemo(() => {
    const unicas = new Map();
    contratos.forEach((contrato) => {
      const id = contrato?.gestaoLocalId;
      const uf = String(contrato?.uf || contrato?.gestaoLocalUf || "").toUpperCase();
      if (!id || !contrato?.gestaoLocalNome || (filtroUf !== "TODAS" && uf !== filtroUf)) return;
      unicas.set(String(id), { id: String(id), nome: contrato.gestaoLocalNome, uf });
    });
    return [...unicas.values()].sort((a, b) => a.nome.localeCompare(b.nome, "pt-BR"));
  }, [contratos, filtroUf]);

  useEffect(() => {
    if (filtroGestaoLocal !== "TODAS" && !gestoesLocaisDisponiveis.some((item) => item.id === String(filtroGestaoLocal))) setFiltroGestaoLocal("TODAS");
  }, [filtroGestaoLocal, gestoesLocaisDisponiveis]);

  const contratosFiltrados = useMemo(() => filtrarContratos({ contratos, termoBusca, filtroStatus, filtroUf, filtroGestaoLocal, filtroVigencia }), [contratos, termoBusca, filtroStatus, filtroUf, filtroGestaoLocal, filtroVigencia]);

  const contextoAcesso = acessoNacionalContratos
    ? "Visão nacional de contratos, preservando a Gestão Local responsável por cada instrumento."
    : perfilGestaoLocal
      ? `Ciclo de vida dos contratos da ${gestaoLocalDoUsuario.nome ? formatarGestaoLocal(gestaoLocalDoUsuario) : "Gestão Local vinculada ao seu perfil"}.`
      : "Consulta dos contratos pertencentes ao seu escopo organizacional.";

  const limparFiltros = () => { setTermoBusca(""); setFiltroStatus("TODOS"); setFiltroUf("TODAS"); setFiltroGestaoLocal("TODAS"); setFiltroVigencia("TODAS"); };

  return (
    <div className={estilos.containerPagina}>
      <ModalConfirmacao />
      <CabecalhoContratos contextoAcesso={contextoAcesso} podeGerenciar={podeGerenciar} carregando={carregando} aoCriarContrato={abrirNovoContrato} aoAtualizar={carregarDados} />

      {!podeGerenciar && <div className={estilos.avisoSomenteLeitura}><Eye size={17}/><div><strong>Perfil de consulta</strong><span>{presidente ? "Você pode consultar todos os contratos do Brasil, inclusive histórico e posição temporal, sem permissão de escrita." : "Você pode consultar contratos, histórico e posição temporal do seu escopo, sem permissão de escrita."}</span></div></div>}

      <IndicadoresContratos indicadores={indicadores}/>
      <FiltrosContratos termoBusca={termoBusca} filtroStatus={filtroStatus} filtroUf={filtroUf} filtroGestaoLocal={filtroGestaoLocal} filtroVigencia={filtroVigencia} ufsDisponiveis={ufsDisponiveis} gestoesLocaisDisponiveis={gestoesLocaisDisponiveis} mostrarFiltroGestaoLocal={!perfilGestaoLocal} totalEncontrado={contratosFiltrados.length} aoAlterarBusca={setTermoBusca} aoAlterarStatus={setFiltroStatus} aoAlterarUf={setFiltroUf} aoAlterarGestaoLocal={setFiltroGestaoLocal} aoAlterarVigencia={setFiltroVigencia} aoLimparFiltros={limparFiltros}/>
      <ListaContratos contratos={contratosFiltrados} carregando={carregando} podeGerenciar={podeGerenciar} aoDetalhar={abrirDetalhes} aoEditar={abrirEdicaoDoContrato} aoExcluir={descartarRascunho}/>

      <ModalContrato aberto={modalAberto} dadosFormulario={dadosFormulario} gestoesLocais={gestoesLocais} tiposContratacao={tiposContratacao} acessoGlobal={!gestaoLocalDoUsuario.id} gestaoLocalDoUsuario={gestaoLocalDoUsuario} salvando={salvando} aoAlterarCampo={alterarCampoDoFormulario} aoFechar={fecharModal} aoSalvar={salvarContrato}/>
      <ModalDetalhesContrato aberto={detalheAberto} contrato={contratoDetalhe} eventos={eventos} precedencias={precedencias} versoes={versoes} carregando={carregandoDetalhe} erroCarregamento={erroDetalhe} podeGerenciar={podeGerenciar} posicaoTemporal={posicaoTemporal} consultandoTemporal={consultandoTemporal} aoConsultarTemporal={consultarTemporal} aoEditar={abrirEdicaoDoContrato} aoAcao={abrirAcaoCicloVida} aoFechar={fecharDetalhes} aoAtualizarContrato={() => contratoDetalhe?.id && carregarDetalhes(contratoDetalhe.id)}/>
      <ModalAcaoContrato aberto={Boolean(acaoAtual)} tipoAcao={acaoAtual?.tipo} contrato={acaoAtual?.contrato} processando={processandoAcao} aoFechar={() => !processandoAcao && setAcaoAtual(null)} aoConfirmar={executarAcaoCicloVida}/>
    </div>
  );
};
