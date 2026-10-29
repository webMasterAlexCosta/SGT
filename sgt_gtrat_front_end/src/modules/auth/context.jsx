import React, {
  createContext,
  useContext,
  useState,
  useEffect,
  useCallback,
  useRef,
  useMemo,
} from "react";
import { ServicoAutenticacao } from "../../services/auth/ServicoAutenticacao";
import {
  EVENT_LOGOUT,
  redefinirIndicadorRedirecionamento,
  dispararSaida,
} from "../../core/events";
import { ModalReautenticacao } from "./components/ModalReautenticacao";
import { CONFIG } from "../../core/config";
import {
  eAdministradorTecnico,
  obterAutoridade,
  obterPerfilPrincipal,
} from "../../core/security/roles";
import {
  CODIGOS_SESSAO_ENCERRADA,
  ESTADOS_AUTENTICACAO,
  estadoAutenticadoPorUsuario,
  mensagemSessaoEncerrada,
} from "../../core/security/estadoAutenticacao.js";

const ContextoAutenticacao = createContext();
const CODIGOS_SESSAO = new Set(CODIGOS_SESSAO_ENCERRADA);

const normalizarVinculos = (valor) => (Array.isArray(valor) ? valor : [])
  .map((item) => ({
    ...item,
    lotacaoId: item?.lotacaoId || item?.lotacao?.id || null,
    jornadaId: item?.jornadaId || item?.jornada?.id || null,
    lotacaoNome: item?.lotacaoNome || item?.lotacao?.label || item?.lotacao?.nome || null,
    jornadaNome: item?.jornadaNome || item?.jornada?.label || item?.jornada?.descricao || null,
  }));

export const normalizarUsuarioAutenticado = (base = {}, perfil = {}) => {
  const perfilCanonico = obterPerfilPrincipal(perfil) || obterPerfilPrincipal(base);
  const autoridadeCanonica = obterAutoridade(perfil) || obterAutoridade(base);
  if (!perfilCanonico || !autoridadeCanonica) return null;

  const vinculosGestao = normalizarVinculos(perfil?.vinculosGestao || base?.vinculosGestao);
  const escopoPrincipal = vinculosGestao[0] || null;
  return {
    ...base,
    ...perfil,
    perfil: perfilCanonico,
    autoridade: autoridadeCanonica,
    username: perfil?.username || base?.username || base?.user || null,
    email: perfil?.email || base?.email || null,
    matricula: perfil?.matricula || base?.matricula || null,
    vinculosGestao,
    jornada: perfil?.jornada || base?.jornada || escopoPrincipal?.jornada || null,
    jornadaId: perfil?.jornada?.id || perfil?.jornadaId || base?.jornada?.id || base?.jornadaId || escopoPrincipal?.jornadaId || null,
    turno: perfil?.jornada?.label || perfil?.turno || base?.jornada?.label || base?.turno || escopoPrincipal?.jornadaNome || null,
    lotacaoObj: perfil?.lotacao || base?.lotacao || escopoPrincipal?.lotacao || null,
    lotacaoId: perfil?.lotacao?.id || perfil?.lotacaoId || base?.lotacao?.id || base?.lotacaoId || escopoPrincipal?.lotacaoId || null,
    lotacaoNome: perfil?.lotacao?.label || perfil?.lotacaoNome || base?.lotacao?.label || base?.lotacaoNome || escopoPrincipal?.lotacaoNome || null,
    lotacao: perfil?.lotacao?.label || perfil?.lotacaoNome || perfil?.lotacao || base?.lotacao?.label || base?.lotacaoNome || base?.lotacao || escopoPrincipal?.lotacaoNome || null,
    superintendencia: perfil?.superintendencia || base?.superintendencia || null,
    superintendenciaId: perfil?.superintendencia?.id || perfil?.superintendenciaId || base?.superintendencia?.id || base?.superintendenciaId || null,
    superintendenciaNome: perfil?.superintendencia?.label || perfil?.superintendenciaNome || base?.superintendencia?.label || base?.superintendenciaNome || null,
    uf: perfil?.superintendencia?.uf || perfil?.uf || base?.superintendencia?.uf || base?.uf || null,
    gestaoLocal: perfil?.gestaoLocal || base?.gestaoLocal || null,
    gestaoLocalId: perfil?.gestaoLocal?.id || perfil?.gestaoLocalId || base?.gestaoLocal?.id || base?.gestaoLocalId || null,
    gestaoLocalNome: perfil?.gestaoLocal?.label || perfil?.gestaoLocalNome || base?.gestaoLocal?.label || base?.gestaoLocalNome || null,
    isRetroactiveUnlocked: perfil?.isRetroactiveUnlocked ?? perfil?.retroactiveUnlocked ?? base?.isRetroactiveUnlocked ?? base?.retroactiveUnlocked ?? false,
    retroactiveExpireAt: perfil?.retroactiveExpireAt ?? base?.retroactiveExpireAt ?? null,
    forceChangePassword: Boolean(perfil?.forceChangePassword ?? base?.forceChangePassword),
    twoFactorEnabled: Boolean(perfil?.twoFactorEnabled ?? base?.twoFactorEnabled),
    twoFactorVerified: Boolean(base?.twoFactorVerified ?? perfil?.twoFactorVerified),
    fotoUrl: perfil?.fotoUrl || base?.fotoUrl || null,
    createdAt: perfil?.createdAt || base?.createdAt || null,
  };
};

const calcularExpiracao = (info) => {
  const segundos = Number(info?.session?.remainingSeconds);
  if (!Number.isFinite(segundos) || segundos <= 0) return null;
  const estimada = Date.now() + Math.floor(segundos * 1000);
  const declarada = Date.parse(info?.session?.expiresAtUtc || "");
  return Number.isFinite(declarada) ? Math.max(declarada, estimada) : estimada;
};

export const ProvedorAutenticacao = ({ children }) => {
  const [usuario, setUsuario] = useState(null);
  const [carregandoSistema, setCarregandoSistema] = useState(true);
  const [encerrandoSessao, setEncerrandoSessao] = useState(false);
  const [estadoAutenticacao, setEstadoAutenticacao] = useState(ESTADOS_AUTENTICACAO.RESTAURANDO_SESSAO);
  const [mostrarModalExpiracao, setMostrarModalExpiracao] = useState(false);
  const [detalhesSessao, setDetalhesSessao] = useState({ expiraEmEpochMs: null, offsetServidorMs: 0 });

  const referenciaUsuario = useRef(null);
  const sessaoExpiradaRef = useRef(false);
  const avisoExpiracao5MinMostrado = useRef(false);
  const avisoExpiracao1MinMostrado = useRef(false);
  const requisicoesSessao = useRef({ leitura: null, toque: null });
  const chaveArmazenamentoFoto = `${CONFIG.APP_NAME.toLowerCase()}_foto_url`;
  const chaveLegadaUsuario = `${CONFIG.APP_NAME.toLowerCase()}_user_app`;

  const publicarUsuario = useCallback((proximo) => {
    referenciaUsuario.current = proximo;
    setUsuario(proximo);
    if (proximo?.fotoUrl) localStorage.setItem(chaveArmazenamentoFoto, proximo.fotoUrl);
    else localStorage.removeItem(chaveArmazenamentoFoto);
  }, [chaveArmazenamentoFoto]);

  useEffect(() => { referenciaUsuario.current = usuario; }, [usuario]);
  const eAdministrador = useMemo(() => eAdministradorTecnico(usuario), [usuario]);

  const consultarSessao = useCallback((options = {}) => {
    const touch = Boolean(options.touch);
    const chave = touch ? "toque" : "leitura";
    if (requisicoesSessao.current[chave]) return requisicoesSessao.current[chave];
    const promessa = ServicoAutenticacao.informacoesSessao({
      touch,
      skipLoading: true,
      ...options,
    }).finally(() => {
      if (requisicoesSessao.current[chave] === promessa) requisicoesSessao.current[chave] = null;
    });
    requisicoesSessao.current[chave] = promessa;
    return promessa;
  }, []);

  const sincronizarDetalhesSessao = useCallback((info) => {
    const expiraEmEpochMs = calcularExpiracao(info);
    if (!expiraEmEpochMs) {
      setDetalhesSessao({ expiraEmEpochMs: null, offsetServidorMs: 0 });
      return null;
    }
    sessaoExpiradaRef.current = false;
    setDetalhesSessao({ expiraEmEpochMs, offsetServidorMs: 0 });
    return { expiraEmEpochMs, serverOffsetMs: 0, raw: info };
  }, []);

  const atualizarInformacoesSessao = useCallback(async (options = {}) => {
    try {
      const info = await consultarSessao(options);
      sincronizarDetalhesSessao(info);
      const atual = referenciaUsuario.current;
      if (atual && info?.user) {
        const mesclado = normalizarUsuarioAutenticado(atual, info.user);
        if (mesclado) publicarUsuario(mesclado);
      }
      return info;
    } catch (erro) {
      const codigo = erro?.code || erro?.data?.code;
      if (CODIGOS_SESSAO.has(codigo)) {
        dispararSaida(mensagemSessaoEncerrada(codigo, erro?.message || erro?.data?.message), { code: codigo });
      }
      throw erro;
    }
  }, [consultarSessao, publicarUsuario, sincronizarDetalhesSessao]);

  const atualizarPerfil = useCallback(async (usuarioBase = null) => {
    try {
      const perfilRemoto = await ServicoAutenticacao.buscarMeuPerfil();
      const base = usuarioBase || referenciaUsuario.current || {};
      const mesclado = normalizarUsuarioAutenticado(base, perfilRemoto);
      if (!mesclado) throw new Error("Perfil/autoridade canônicos ausentes na resposta do servidor.");
      publicarUsuario(mesclado);
      setEstadoAutenticacao(estadoAutenticadoPorUsuario(mesclado));
      return mesclado;
    } catch (erro) {
      if (!usuarioBase) {
        publicarUsuario(null);
        setEstadoAutenticacao(ESTADOS_AUTENTICACAO.NAO_AUTENTICADO);
      }
      return null;
    }
  }, [publicarUsuario]);

  const realizarLogout = useCallback(() => {
    const haviaSessao = Boolean(referenciaUsuario.current);
    setEncerrandoSessao(haviaSessao);
    setEstadoAutenticacao(ESTADOS_AUTENTICACAO.SESSAO_ENCERRANDO);
    setMostrarModalExpiracao(false);
    localStorage.removeItem(chaveLegadaUsuario);
    localStorage.removeItem(chaveArmazenamentoFoto);
    setDetalhesSessao({ expiraEmEpochMs: null, offsetServidorMs: 0 });
    sessaoExpiradaRef.current = true;
    if (haviaSessao) void ServicoAutenticacao.encerrarSessao();
    else {
      publicarUsuario(null);
      setEncerrandoSessao(false);
      setEstadoAutenticacao(ESTADOS_AUTENTICACAO.NAO_AUTENTICADO);
    }
  }, [chaveArmazenamentoFoto, chaveLegadaUsuario, publicarUsuario]);

  const concluirSaida = useCallback(async () => {
    publicarUsuario(null);
    localStorage.removeItem(chaveLegadaUsuario);
    localStorage.removeItem(chaveArmazenamentoFoto);
    redefinirIndicadorRedirecionamento?.();
    window.history.pushState(null, "", "/login");
    setEncerrandoSessao(false);
    setEstadoAutenticacao(ESTADOS_AUTENTICACAO.NAO_AUTENTICADO);
    sessaoExpiradaRef.current = false;
    setMostrarModalExpiracao(false);
  }, [chaveArmazenamentoFoto, chaveLegadaUsuario, publicarUsuario]);

  useEffect(() => {
    let montado = true;
    localStorage.removeItem(chaveLegadaUsuario);
    const aoLogout = () => realizarLogout();
    window.addEventListener(EVENT_LOGOUT, aoLogout);

    const bootstrap = async () => {
      setEstadoAutenticacao(ESTADOS_AUTENTICACAO.RESTAURANDO_SESSAO);
      try {
        const info = await consultarSessao({ touch: false, skipAuthLogout: true });
        if (!info?.authenticated) throw Object.assign(new Error("Sessão não autenticada."), { status: 401 });
        const perfil = await ServicoAutenticacao.buscarMeuPerfil();
        const mesclado = normalizarUsuarioAutenticado(info.user || {}, perfil);
        if (!mesclado) throw new Error("Sessão sem perfil/autoridade canônicos.");
        if (!montado) return;
        publicarUsuario(mesclado);
        sincronizarDetalhesSessao(info);
        setEstadoAutenticacao(estadoAutenticadoPorUsuario(mesclado));
      } catch (erro) {
        const codigo = erro?.code || erro?.data?.code;
        if (CODIGOS_SESSAO.has(codigo)) {
          dispararSaida(mensagemSessaoEncerrada(codigo, erro?.message || erro?.data?.message), { code: codigo });
        }
        if (montado) {
          publicarUsuario(null);
          setEstadoAutenticacao(ESTADOS_AUTENTICACAO.NAO_AUTENTICADO);
        }
      } finally {
        if (montado) setCarregandoSistema(false);
      }
    };
    void bootstrap();
    return () => {
      montado = false;
      window.removeEventListener(EVENT_LOGOUT, aoLogout);
    };
  }, [chaveLegadaUsuario, consultarSessao, publicarUsuario, realizarLogout, sincronizarDetalhesSessao]);

  useEffect(() => {
    if (!usuario || !detalhesSessao.expiraEmEpochMs || encerrandoSessao) return undefined;
    const verificar = () => {
      const faltam = detalhesSessao.expiraEmEpochMs - Date.now();
      if (faltam <= CONFIG.SESSION_WARN_5MIN_MS && faltam > CONFIG.SESSION_WARN_1MIN_MS) {
        if (!avisoExpiracao5MinMostrado.current) {
          avisoExpiracao5MinMostrado.current = true;
          setMostrarModalExpiracao(true);
        }
      } else if (faltam <= CONFIG.SESSION_WARN_1MIN_MS && faltam > 0) {
        if (!avisoExpiracao1MinMostrado.current) {
          avisoExpiracao1MinMostrado.current = true;
          setMostrarModalExpiracao(true);
        }
      } else if (faltam > CONFIG.SESSION_WARN_5MIN_MS) {
        avisoExpiracao5MinMostrado.current = false;
        avisoExpiracao1MinMostrado.current = false;
        setMostrarModalExpiracao(false);
      }
      if (faltam <= 0 && !sessaoExpiradaRef.current) {
        sessaoExpiradaRef.current = true;
        realizarLogout();
      }
    };
    verificar();
    const id = window.setInterval(verificar, 1000);
    return () => window.clearInterval(id);
  }, [detalhesSessao.expiraEmEpochMs, encerrandoSessao, realizarLogout, usuario]);

  useEffect(() => {
    if (!usuario || encerrandoSessao) return undefined;
    const id = window.setInterval(() => {
      void atualizarInformacoesSessao({ touch: false, silentError: true }).catch(() => {});
    }, CONFIG.SESSION_REFRESH_INTERVAL_MS);
    return () => window.clearInterval(id);
  }, [atualizarInformacoesSessao, encerrandoSessao, usuario]);

  const efetuarLogin = useCallback(async (matricula, password, totp) => {
    setEstadoAutenticacao(ESTADOS_AUTENTICACAO.CREDENCIAIS_ACEITAS);
    const retorno = await ServicoAutenticacao.autenticar(matricula, password, totp);
    if (retorno?.requiresTwoFactor) {
      setEstadoAutenticacao(ESTADOS_AUTENTICACAO.SEGUNDO_FATOR_REQUERIDO);
      return retorno;
    }
    if (retorno?.error) {
      setEstadoAutenticacao(totp ? ESTADOS_AUTENTICACAO.SEGUNDO_FATOR_REQUERIDO : ESTADOS_AUTENTICACAO.NAO_AUTENTICADO);
      return retorno;
    }
    try {
      const perfil = await ServicoAutenticacao.buscarMeuPerfil();
      const mesclado = normalizarUsuarioAutenticado(retorno, perfil);
      if (!mesclado) throw new Error("Perfil/autoridade canônicos ausentes.");
      publicarUsuario(mesclado);
      setEstadoAutenticacao(estadoAutenticadoPorUsuario(mesclado));
      await atualizarInformacoesSessao({ touch: false, silentError: true });
      return mesclado;
    } catch {
      publicarUsuario(null);
      setEstadoAutenticacao(ESTADOS_AUTENTICACAO.NAO_AUTENTICADO);
      return { error: true, message: "Não foi possível validar o perfil da sessão." };
    }
  }, [atualizarInformacoesSessao, publicarUsuario]);

  const registrarUsuario = useCallback((u, e, m, se, gl) => ServicoAutenticacao.cadastrar(u, e, m, se, gl), []);

  const valor = useMemo(() => ({
    user: usuario,
    isAdmin: eAdministrador,
    autenticar: efetuarLogin,
    cadastrar: registrarUsuario,
    encerrarSessao: realizarLogout,
    loading: carregandoSistema,
    atualizarPerfil,
    loggingOut: encerrandoSessao,
    concluirSaida,
    atualizarInformacoesSessao,
    sessionExpiresAtEpochMs: detalhesSessao.expiraEmEpochMs,
    estadoAutenticacao,
  }), [
    usuario, eAdministrador, efetuarLogin, registrarUsuario, realizarLogout,
    carregandoSistema, atualizarPerfil, encerrandoSessao, concluirSaida,
    atualizarInformacoesSessao, detalhesSessao.expiraEmEpochMs, estadoAutenticacao,
  ]);

  return (
    <ContextoAutenticacao.Provider value={valor}>
      {children}
      <ModalReautenticacao
        isOpen={mostrarModalExpiracao}
        onClose={() => setMostrarModalExpiracao(false)}
        onConfirm={async () => {
          await atualizarInformacoesSessao({ touch: false, silentError: true }).catch(() => {});
          setMostrarModalExpiracao(false);
          avisoExpiracao5MinMostrado.current = false;
          avisoExpiracao1MinMostrado.current = false;
        }}
        user={usuario}
        expiraEmEpochMs={detalhesSessao.expiraEmEpochMs}
      />
    </ContextoAutenticacao.Provider>
  );
};

export const useAutenticacao = () => useContext(ContextoAutenticacao);
