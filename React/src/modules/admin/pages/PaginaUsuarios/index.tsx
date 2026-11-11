import { observarFalha } from '../../../../core/http/falhaObservada';
import type { UserSummaryView } from '../../../../contracts/opcoes-apresentacao';
import type { UserForm, PasswordResetForm, ManagementLinkInput, ManagementLinkForm } from '../../../../contracts/usuarios';
import type { PerfilAcesso } from '../../../../contracts/java';
import type { MutableForm } from '../../../../contracts/formularios';
import React, { useEffect, useMemo, useState } from "react";
import { notificar } from "../../../../core/events";
import { useConfirmacaoSenha } from "../../../../core/security/useConfirmacaoSenha.tsx";
import { SENHA_MINIMA_UI, SENHA_MAXIMA_UI } from "../../../../core/security/politicaSenha.ts";
import {
  PERFIS,
  eAdministradorTecnico,
  eGestaoLocal,
  eGestorEstadual,
  eGestorNacional,
  obterPerfilPrincipal,
} from "../../../../core/security/roles";
import { useAutenticacao } from "../../../auth/context.tsx";
import { ServicoLotacao } from "../../../../services/admin/ServicoLotacao";
import { ServicoOrganizacional } from "../../../../services/admin/ServicoOrganizacional";
import { ServicoUsuario } from "../../../../services/admin/ServicoUsuario";
import { CONFIG } from "../../../../core/config";
import { formatarMatricula, matriculaEstaCompleta } from "../../../../core/formatters/matricula";
import { formatarGestorEstadual, formatarGestaoLocal } from "../../../../core/formatters/nomenclaturaGestao.ts";
import estilos from "./index.module.css";
import { CabecalhoUsuarios } from "./components/CabecalhoUsuarios";
import { TabelaUsuarios } from "./components/TabelaUsuarios";
import { ModalUsuario } from "./components/ModalUsuario";
import { ModalRedefinirSenha } from "./components/ModalRedefinirSenha";
import { ModalSenhaTemporaria } from "../../components/ModalSenhaTemporaria";

const ITENS_POR_PAGINA = 5;

const redefinicaoInicial = Object.freeze({
  usuario: null,
  novaSenha: "",
  confirmarSenha: "",
});

const formularioInicial = Object.freeze({
  id: null,
  username: "",
  email: "",
  password: "",
  matricula: "",
  perfil: PERFIS.OPERACIONAL,
  active: false,
  superintendenciaId: "",
  gestaoLocalId: "",
  vinculosGestao: [],
  novoLotacaoId: "",
  novoJornadaId: "",
});

const normalizarVinculo = (item: ManagementLinkInput | null) => {
  const lotacaoId = String(item?.lotacao?.id || item?.lotacaoId || "");
  const jornadaId = String(item?.jornada?.id || item?.jornadaId || "");
  if (!lotacaoId || !jornadaId) return null;
  return {
    lotacaoId,
    jornadaId,
    lotacaoNome: item?.lotacao?.label || item?.lotacaoNome || "Lotação",
    jornadaNome: item?.jornada?.label || item?.jornadaNome || "Turno",
  };
};

const obterVinculos = (usuario: import("../../../../contracts/opcoes-apresentacao").UserSummaryView | null) => {
  const vinculos = (usuario?.vinculosGestao || []).map(normalizarVinculo).filter((item): item is ManagementLinkForm => item !== null);
  if (vinculos.length) return vinculos;
  const principal = normalizarVinculo({ lotacao: usuario?.lotacao, jornada: usuario?.jornada });
  return principal ? [principal] : [];
};

const obterPerfil = (usuario: (import("../../../../contracts/opcoes-apresentacao").UserSummaryView)) => obterPerfilPrincipal(usuario);

export const PaginaUsuarios = () => {
  const { user: gestorAtual, atualizarPerfil, atualizarInformacoesSessao } = useAutenticacao();
  const { confirmarComSenha, ModalConfirmacao } = useConfirmacaoSenha();
  const [usuarios, setUsuarios] = useState<(import("../../../../contracts/opcoes-apresentacao").UserSummaryView[])>([]);
  const [lotacoes, setLotacoes] = useState<(import("../../../../contracts/opcoes-apresentacao").LocationView[])>([]);
  const [superintendencias, setSuperintendencias] = useState<(import("../../../../contracts/java").SuperintendenciaDTO[])>([]);
  const [gestoesLocais, setGestoesLocais] = useState<(import("../../../../contracts/opcoes-apresentacao").LocalManagementView[])>([]);
  const [carregando, setCarregando] = useState(false);
  const [modalAberto, setModalAberto] = useState(false);
  const [modalSenhaAberto, setModalSenhaAberto] = useState(false);
  const [credencialTemporaria, setCredencialTemporaria] = useState<(import("../../../../contracts/java").AtivacaoUsuarioAplicacaoDTO) | null>(null);
  const [termoBusca, setTermoBusca] = useState("");
  const [filtroPerfil, setFiltroPerfil] = useState("");
  const [filtroStatus, setFiltroStatus] = useState("");
  const [paginaAtual, setPaginaAtual] = useState(1);
  const [dadosFormulario, setDadosFormulario] = useState<UserForm>({ ...formularioInicial });
  const [dadosRedefinicao, setDadosRedefinicao] = useState<PasswordResetForm>({ ...redefinicaoInicial });

  const editando = dadosFormulario.id != null;
  const propriaContaEmEdicao = editando && String(dadosFormulario.id) === String(gestorAtual?.id);

  const perfisPermitidos = useMemo(() => {
    if (eGestaoLocal(gestorAtual)) return [PERFIS.OPERACIONAL];
    if (eGestorEstadual(gestorAtual)) return [PERFIS.GESTOR_LOCAL];
    if (eGestorNacional(gestorAtual)) return [PERFIS.GESTOR_ESTADUAL];
    if (eAdministradorTecnico(gestorAtual)) return [
      PERFIS.OPERACIONAL,
      PERFIS.GESTOR_LOCAL,
      PERFIS.GESTOR_ESTADUAL,
      PERFIS.GESTOR_NACIONAL,
      PERFIS.PRESIDENTE,
      PERFIS.ADMINISTRADOR,
    ];
    return [];
  }, [gestorAtual]);

  const carregarDados = async (opcoes = {}) => {
    setCarregando(true);
    try {
      const carregaGestoesLocais = !eGestorNacional(gestorAtual);
      const carregaLotacoes = eAdministradorTecnico(gestorAtual) || eGestaoLocal(gestorAtual);
      const [resUsuarios, resLotacoes, resSuperintendencias, resGestoesLocais] = await Promise.all([
        ServicoUsuario.listarTodos(opcoes),
        carregaLotacoes ? ServicoLotacao.listar({ size: CONFIG.PAGINATION_MAX_SIZE }, opcoes) : Promise.resolve([]),
        ServicoOrganizacional.listarSuperintendencias(opcoes),
        carregaGestoesLocais ? ServicoOrganizacional.listarGestoesLocais({}, opcoes) : Promise.resolve([]),
      ]);
      setUsuarios(Array.isArray(resUsuarios) ? resUsuarios : []);
      setLotacoes(resLotacoes);
      setSuperintendencias(Array.isArray(resSuperintendencias) ? resSuperintendencias : []);
      setGestoesLocais(Array.isArray(resGestoesLocais) ? resGestoesLocais : []);
    } catch (erroOriginal) {
      const erro = observarFalha(erroOriginal);

      if (!erro?.canceled) notificar("error", erro?.message || "Erro ao carregar os usuários.");
    } finally {
      setCarregando(false);
    }
  };

  useEffect(() => {
    const controlador = new AbortController();
    carregarDados({ signal: controlador.signal });
    return () => controlador.abort();
  }, []);

  const prepararNovoUsuario = () => {
    const gestaoLocalInicial = eGestaoLocal(gestorAtual) ? gestoesLocais[0] : null;
    const seInicial = gestaoLocalInicial
      ? gestaoLocalInicial.superintendenciaId
      : eGestorEstadual(gestorAtual) ? superintendencias[0]?.id : "";
    setDadosFormulario({
      ...formularioInicial,
      perfil: perfisPermitidos[0] || PERFIS.OPERACIONAL,
      superintendenciaId: String(seInicial || ""),
      gestaoLocalId: String(gestaoLocalInicial?.id || ""),
    });
    setModalAberto(true);
  };

  const prepararEdicao = (usuario: UserSummaryView) => {
    setDadosFormulario({
      ...formularioInicial,
      id: usuario.id,
      username: usuario.username || "",
      email: usuario.email || "",
      matricula: formatarMatricula(usuario.matricula || ""),
      perfil: obterPerfil(usuario) || PERFIS.OPERACIONAL,
      active: !!usuario.active,
      superintendenciaId: String(usuario?.superintendencia?.id || ""),
      gestaoLocalId: String(usuario?.gestaoLocal?.id || ""),
      vinculosGestao: obterVinculos(usuario),
    });
    setModalAberto(true);
  };

  const gestoesLocaisDisponiveis = useMemo(() => {
    if (!dadosFormulario.superintendenciaId) return gestoesLocais;
    return gestoesLocais.filter((item) => String(item.superintendenciaId) === String(dadosFormulario.superintendenciaId));
  }, [gestoesLocais, dadosFormulario.superintendenciaId]);

  const lotacoesDisponiveis = useMemo(() => {
    if (!dadosFormulario.gestaoLocalId) return [];
    return lotacoes.filter((item) => String(item.gestaoLocalId) === String(dadosFormulario.gestaoLocalId));
  }, [lotacoes, dadosFormulario.gestaoLocalId]);

  const lotacaoSelecionada = useMemo(
    () => lotacoesDisponiveis.find((item) => String(item.id) === String(dadosFormulario.novoLotacaoId)),
    [lotacoesDisponiveis, dadosFormulario.novoLotacaoId],
  );

  const jornadasDisponiveis = Array.isArray(lotacaoSelecionada?.turnos) ? lotacaoSelecionada.turnos : [];

  const alterarPerfil = (perfil: PerfilAcesso) => {
    setDadosFormulario((atual) => {
      const exigeSe = ([PERFIS.GESTOR_ESTADUAL, PERFIS.GESTOR_LOCAL, PERFIS.OPERACIONAL] as readonly string[]).includes(perfil);
      const exigeGestaoLocal = ([PERFIS.GESTOR_LOCAL, PERFIS.OPERACIONAL] as readonly string[]).includes(perfil);
      const seId = exigeSe ? atual.superintendenciaId : "";
      const gestaoLocalAtualPertenceASe = gestoesLocais.some((item) =>
        String(item.id) === String(atual.gestaoLocalId)
        && String(item.superintendenciaId) === String(seId),
      );
      return {
        ...atual,
        perfil,
        superintendenciaId: seId,
        gestaoLocalId: exigeGestaoLocal && gestaoLocalAtualPertenceASe
          ? String(atual.gestaoLocalId)
          : "",
        vinculosGestao: perfil === PERFIS.OPERACIONAL ? atual.vinculosGestao : [],
      };
    });
  };

  const alterarSuperintendencia = (superintendenciaId: string) => {
    setDadosFormulario((atual) => ({
      ...atual,
      superintendenciaId,
      gestaoLocalId: "",
      vinculosGestao: [],
      novoLotacaoId: "",
      novoJornadaId: "",
    }));
  };

  const alterarGestaoLocal = (gestaoLocalId: string) => {
    const gestaoLocal = gestoesLocais.find((item) => String(item.id) === String(gestaoLocalId));
    setDadosFormulario((atual) => ({
      ...atual,
      gestaoLocalId,
      superintendenciaId: String(gestaoLocal?.superintendenciaId || atual.superintendenciaId || ""),
      vinculosGestao: [],
      novoLotacaoId: "",
      novoJornadaId: "",
    }));
  };

  const adicionarVinculo = () => {
    const lotacao = lotacoesDisponiveis.find((item) => String(item.id) === String(dadosFormulario.novoLotacaoId));
    const jornada = (lotacao?.turnos || []).find((item) => String(item.id) === String(dadosFormulario.novoJornadaId));
    if (!lotacao || !jornada) return notificar("error", "Selecione uma lotação e um turno válidos.");
    const chave = `${lotacao.id}:${jornada.id}`;
    if (dadosFormulario.vinculosGestao.some((item) => `${item.lotacaoId}:${item.jornadaId}` === chave)) {
      return notificar("info", "Esse vínculo operacional já foi adicionado.");
    }
    setDadosFormulario((atual) => ({
      ...atual,
      vinculosGestao: [...atual.vinculosGestao, {
        lotacaoId: String(lotacao.id),
        jornadaId: String(jornada.id),
        lotacaoNome: lotacao.nome || "Lotação",
        jornadaNome: jornada.descricao || "Turno",
      }],
      novoLotacaoId: "",
      novoJornadaId: "",
    }));
  };

  const removerVinculo = (alvo: ManagementLinkForm) => setDadosFormulario((atual) => ({
    ...atual,
    vinculosGestao: atual.vinculosGestao.filter((item) =>
      `${item.lotacaoId}:${item.jornadaId}` !== `${alvo.lotacaoId}:${alvo.jornadaId}`),
  }));

  const validar = () => {
    const dados = dadosFormulario;
    if (!dados.username.trim()) return notificar("error", "Informe o nome de usuário."), false;
    if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(dados.email.trim())) return notificar("error", "Informe um e-mail válido."), false;
    if (!/^[^\s@]+@correios\.com\.br$/i.test(dados.email.trim())) return notificar("error", "Informe um e-mail institucional @correios.com.br."), false;
    if (!matriculaEstaCompleta(dados.matricula)) return notificar("error", "Informe a matrícula completa no formato 8.961.474-5."), false;
    if (!editando && dados.password.length < SENHA_MINIMA_UI) return notificar("error", `A senha inicial deve ter pelo menos ${SENHA_MINIMA_UI} caracteres.`), false;
    if (!editando && dados.password.length > SENHA_MAXIMA_UI) return notificar("error", "A senha inicial deve ter no máximo 72 caracteres."), false;
    if (([PERFIS.GESTOR_ESTADUAL, PERFIS.GESTOR_LOCAL, PERFIS.OPERACIONAL] as readonly string[]).includes(dados.perfil) && !dados.superintendenciaId) {
      return notificar("error", "Selecione o gestor estadual."), false;
    }
    if (([PERFIS.GESTOR_LOCAL, PERFIS.OPERACIONAL] as readonly string[]).includes(dados.perfil) && !dados.gestaoLocalId) {
      return notificar("error", "Selecione a Gestão Local responsável."), false;
    }
    if (dados.perfil === PERFIS.OPERACIONAL && !dados.vinculosGestao.length) {
      return notificar("error", "Informe ao menos uma lotação e turno para o usuário operacional."), false;
    }
    return true;
  };

  const salvar = async () => {
    if (!validar()) return;
    const principal = dadosFormulario.vinculosGestao[0];
    const payload = {
      email: dadosFormulario.email.trim().toLowerCase(),
      matricula: formatarMatricula(dadosFormulario.matricula),
      perfil: dadosFormulario.perfil,
      active: dadosFormulario.active,
      superintendenciaId: dadosFormulario.superintendenciaId ? Number(dadosFormulario.superintendenciaId) : null,
      gestaoLocalId: dadosFormulario.gestaoLocalId ? Number(dadosFormulario.gestaoLocalId) : null,
      vinculosGestao: dadosFormulario.perfil === PERFIS.OPERACIONAL
        ? dadosFormulario.vinculosGestao.map((item) => ({ lotacaoId: Number(item.lotacaoId), jornadaId: Number(item.jornadaId) }))
        : [],
      lotacaoId: principal ? Number(principal.lotacaoId) : null,
      jornadaId: principal ? Number(principal.jornadaId) : null,
    };

    await confirmarComSenha({
      titulo: editando ? "Salvar alterações do usuário" : "Criar usuário",
      mensagem: "Confirme sua senha para persistir o perfil e o escopo organizacional.",
      acao: async (senhaConfirmacao) => {
        if (editando) await ServicoUsuario.atualizar(dadosFormulario.id, payload, { senhaConfirmacao });
        else await ServicoUsuario.criar({ ...payload, username: dadosFormulario.username.trim().toLowerCase(), password: dadosFormulario.password }, { senhaConfirmacao });
        setModalAberto(false);
        await carregarDados();
        if (propriaContaEmEdicao) {
          await atualizarPerfil();
          await atualizarInformacoesSessao({ touch: false, silentError: true }).catch(() => {});
        }
      },
    });
  };

  const alterarAtivacao = (usuario: UserSummaryView) => {
    const ativar = !usuario.active;
    const perfil = obterPerfil(usuario);
    const mensagemAtivacao = perfil === PERFIS.GESTOR_ESTADUAL
      ? `Ativar "${usuario.username}" e gerar uma nova senha temporária? Os Gestões Locais e usuários operacionais continuarão inativos.`
      : perfil === PERFIS.GESTOR_LOCAL
        ? `Ativar "${usuario.username}" e gerar uma nova senha temporária? Os usuários operacionais continuarão inativos.`
        : `Ativar "${usuario.username}" e gerar uma nova senha temporária? Ela será exibida uma única vez.`;
    const mensagemInativacao = perfil === PERFIS.GESTOR_ESTADUAL
      ? `Desativar "${usuario.username}"? Todas as Gestões Locais e usuários operacionais vinculados serão inativados, e as senhas anteriores deixarão de funcionar.`
      : perfil === PERFIS.GESTOR_LOCAL
        ? `Desativar "${usuario.username}"? Todos os usuários operacionais vinculados serão inativados, e as senhas anteriores deixarão de funcionar.`
        : `Desativar "${usuario.username}", invalidar a senha anterior e encerrar imediatamente as sessões ativas?`;
    return confirmarComSenha({
      titulo: ativar ? "Ativar usuário" : "Desativar usuário",
      mensagem: ativar ? mensagemAtivacao : mensagemInativacao,
      acao: async (senhaConfirmacao) => {
        if (ativar) {
          const resposta = await ServicoUsuario.ativar(usuario.id, { senhaConfirmacao });
          setCredencialTemporaria(resposta);
        } else {
          await ServicoUsuario.excluir(usuario.id, { senhaConfirmacao });
        }
        await carregarDados();
      },
    });
  };

  const abrirRedefinicaoSenha = (usuario: UserSummaryView) => {
    setDadosRedefinicao({
      ...redefinicaoInicial,
      usuario,
    });
    setModalSenhaAberto(true);
  };

  const redefinirSenha = async () => {
    const novaSenha = dadosRedefinicao.novaSenha;
    if (novaSenha.length < SENHA_MINIMA_UI) return notificar("error", `A nova senha deve ter pelo menos ${SENHA_MINIMA_UI} caracteres.`);
    if (novaSenha.length > SENHA_MAXIMA_UI) return notificar("error", "A nova senha deve ter no máximo 72 caracteres.");
    if (novaSenha !== dadosRedefinicao.confirmarSenha) return notificar("error", "A confirmação da senha não coincide.");
    const usuario = dadosRedefinicao.usuario;
    if (!usuario?.id) return notificar("error", "Usuário inválido para redefinição de senha.");

    await confirmarComSenha({
      titulo: "Confirmar redefinição de senha",
      mensagem: `Confirme a sua própria senha para redefinir o acesso de "${usuario.username}".`,
      acao: async (senhaConfirmacao) => {
        await ServicoUsuario.redefinirSenha(usuario.id, { newPassword: novaSenha }, { senhaConfirmacao });
        setModalSenhaAberto(false);
        setDadosRedefinicao({ ...redefinicaoInicial });
        await carregarDados();
      },
    });
  };

  const usuariosPermitidosPeloPerfil = useMemo(() => usuarios.filter((usuario) => {
    const perfil = obterPerfil(usuario);
    if (eAdministradorTecnico(gestorAtual)) return true;
    if (eGestorNacional(gestorAtual)) return perfil === PERFIS.GESTOR_ESTADUAL;
    if (eGestorEstadual(gestorAtual)) return perfil === PERFIS.GESTOR_LOCAL;
    if (eGestaoLocal(gestorAtual)) return perfil === PERFIS.OPERACIONAL;
    return false;
  }), [usuarios, gestorAtual]);

  const usuariosFiltrados = useMemo(() => {
    const termo = termoBusca.trim().toLowerCase();
    return usuariosPermitidosPeloPerfil.filter((usuario) => {
      const perfil = obterPerfil(usuario);
      const correspondeTexto = !termo || [
        usuario.username, usuario.email, usuario.matricula,
        usuario.superintendencia?.label, usuario.superintendencia?.uf,
        usuario.gestaoLocal?.label, usuario.lotacao?.label,
      ].some((valor) => String(valor || "").toLowerCase().includes(termo));
      const correspondePerfil = !filtroPerfil || perfil === filtroPerfil;
      const correspondeStatus = !filtroStatus
        || (filtroStatus === "ATIVO" && !!usuario.active)
        || (filtroStatus === "INATIVO" && !usuario.active)
        || (filtroStatus === "BLOQUEADO" && !!usuario.locked)
        || (filtroStatus === "TROCA_PENDENTE" && !!usuario.forceChangePassword);
      return correspondeTexto && correspondePerfil && correspondeStatus;
    });
  }, [usuariosPermitidosPeloPerfil, termoBusca, filtroPerfil, filtroStatus]);

  useEffect(() => {
    setPaginaAtual(1);
  }, [termoBusca, filtroPerfil, filtroStatus]);

  const totalPaginas = Math.max(1, Math.ceil(usuariosFiltrados.length / ITENS_POR_PAGINA));
  useEffect(() => {
    setPaginaAtual((atual) => Math.min(atual, totalPaginas));
  }, [totalPaginas]);

  const usuariosPaginados = useMemo(() => {
    const inicio = (paginaAtual - 1) * ITENS_POR_PAGINA;
    return usuariosFiltrados.slice(inicio, inicio + ITENS_POR_PAGINA);
  }, [usuariosFiltrados, paginaAtual]);

  const podeGerenciarUsuario = (usuario: UserSummaryView) => {
    const perfilAlvo = obterPerfil(usuario);
    if (eAdministradorTecnico(gestorAtual)) return true;
    if (eGestorNacional(gestorAtual)) return perfilAlvo === PERFIS.GESTOR_ESTADUAL;
    if (eGestorEstadual(gestorAtual)) return perfilAlvo === PERFIS.GESTOR_LOCAL;
    if (eGestaoLocal(gestorAtual)) return perfilAlvo === PERFIS.OPERACIONAL;
    return false;
  };

  const descreverAbrangencia = (usuario: UserSummaryView) => {
    const perfil = obterPerfil(usuario);
    if (perfil === PERFIS.PRESIDENTE) return "Nacional — visão consolidada por gestor estadual";
    if (perfil === PERFIS.ADMINISTRADOR) return "Nacional — administração técnica";
    if (perfil === PERFIS.GESTOR_ESTADUAL) return formatarGestorEstadual(usuario.superintendencia, usuario?.uf);
    if (perfil === PERFIS.GESTOR_LOCAL) return formatarGestaoLocal(usuario.gestaoLocal);
    const vinculos = obterVinculos(usuario);
    return vinculos.length
      ? `${formatarGestaoLocal(usuario.gestaoLocal)} · ${vinculos.map((item) => `${item.lotacaoNome} / ${item.jornadaNome}`).join(" | ")}`
      : "Sem vínculo operacional";
  };

  const fecharRedefinicao = () => {
    setModalSenhaAberto(false);
    setDadosRedefinicao({ ...redefinicaoInicial });
  };

  return (
    <div className={estilos.containerPagina}>
      <ModalConfirmacao />
      <CabecalhoUsuarios gestorAtual={gestorAtual} termoBusca={termoBusca} aoBuscar={setTermoBusca} aoNovoUsuario={prepararNovoUsuario} totalUsuarios={usuariosFiltrados.length} filtroPerfil={filtroPerfil} aoFiltrarPerfil={setFiltroPerfil} filtroStatus={filtroStatus} aoFiltrarStatus={setFiltroStatus} perfisPermitidos={perfisPermitidos} />
      <TabelaUsuarios
        usuarios={usuariosPaginados}
        carregando={carregando}
        gestorAtual={gestorAtual}
        obterPerfil={obterPerfil}
        podeGerenciarUsuario={podeGerenciarUsuario}
        descreverAbrangencia={descreverAbrangencia}
        aoRedefinirSenha={abrirRedefinicaoSenha}
        aoEditar={prepararEdicao}
        aoAlterarAtivacao={alterarAtivacao}
        paginaAtual={paginaAtual}
        totalPaginas={totalPaginas}
        totalRegistros={usuariosFiltrados.length}
        aoMudarPagina={setPaginaAtual}
      />
      <ModalUsuario
        aberto={modalAberto}
        aoFechar={() => setModalAberto(false)}
        editando={editando}
        dados={dadosFormulario}
        setDados={setDadosFormulario}
        perfisPermitidos={perfisPermitidos}
        superintendencias={superintendencias}
        gestoesLocaisDisponiveis={gestoesLocaisDisponiveis}
        lotacoesDisponiveis={lotacoesDisponiveis}
        jornadasDisponiveis={jornadasDisponiveis}
        aoAlterarPerfil={alterarPerfil}
        aoAlterarSuperintendencia={alterarSuperintendencia}
        aoAlterarGestaoLocal={alterarGestaoLocal}
        aoAdicionarVinculo={adicionarVinculo}
        aoRemoverVinculo={removerVinculo}
        aoSalvar={salvar}
        propriaConta={propriaContaEmEdicao}
        senhaMinima={SENHA_MINIMA_UI}
      />
      <ModalRedefinirSenha aberto={modalSenhaAberto} dados={dadosRedefinicao} setDados={setDadosRedefinicao} aoFechar={fecharRedefinicao} aoConfirmar={redefinirSenha} senhaMinima={SENHA_MINIMA_UI} />
      <ModalSenhaTemporaria credencial={credencialTemporaria} aoFechar={() => setCredencialTemporaria(null)} />
    </div>
  );
};
