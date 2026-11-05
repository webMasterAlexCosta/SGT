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
  notificar,
} from "../../core/events";
import { ModalReautenticacao } from "./components/ModalReautenticacao";
import { CONFIG } from "../../core/config";
import {
  eAdministradorTecnico,
  obterAutoridade,
  obterPerfilPrincipal,
} from "../../core/security/roles";
import {
  erroEncerraSessao,
  ESTADOS_AUTENTICACAO,
  estadoAutenticadoPorUsuario,
  mensagemSessaoEncerrada,
} from "../../core/security/estadoAutenticacao.js";

const ContextoAutenticacao = createContext();
import {
  obterGeracaoSessao, sessaoAtual, invalidarGeracaoSessao, obterSinalSessao,
  CHAVE_TROCA_SESSAO, EVENT_TROCA_SESSAO_EXTERNA, houveTrocaSessaoExterna, anunciarTrocaSessao,
} from "../../core/security/geracaoSessao.js";
import {
  obterIntencaoSaida, registrarIntencaoSaida, confirmarLoginExplicito,
  obterRevogacaoEmAndamento, revogacaoFoiConfirmada, iniciarRevogacaoCoordenada,
  exigirLoginExplicitoLocal, restauracaoLocalBloqueada,
} from "../../core/security/saidaSessao.js";

const possui = (objeto, campo) => Object.prototype.hasOwnProperty.call(objeto || {}, campo);
const valorAtual = (base, perfil, campo) => possui(perfil, campo) ? perfil[campo] : base[campo];


const normalizarVinculos = (valor) => (Array.isArray(valor) ? valor : [])
  .map((item) => ({
    ...item,
    lotacaoId: item?.lotacaoId || item?.lotacao?.id || null,
    jornadaId: item?.jornadaId || item?.jornada?.id || null,
    lotacaoNome: item?.lotacaoNome || item?.lotacao?.label || item?.lotacao?.nome || null,
    jornadaNome: item?.jornadaNome || item?.jornada?.label || item?.jornada?.descricao || null,
  }));

export const normalizarUsuarioAutenticado = (base = {}, perfil = {}) => {
  // Ausência de propriedade permite complementar o DTO; null/false/[] são
  // respostas autoritativas e nunca recuperam uma concessão antiga.
  const perfilCanonico = possui(perfil, "perfil") || possui(perfil, "autoridade")
    ? obterPerfilPrincipal(perfil) : obterPerfilPrincipal(base);
  const autoridadeCanonica = possui(perfil, "perfil") || possui(perfil, "autoridade")
    ? obterAutoridade(perfil) : obterAutoridade(base);
  if (!perfilCanonico || !autoridadeCanonica) return null;
  const dado = (campo) => valorAtual(base, perfil, campo);
  const vinculosGestao = normalizarVinculos(dado("vinculosGestao"));
  const principal = vinculosGestao[0] || null;
  const referencia = (campo, atributo, alternativo = null) => {
    const direto = `${campo}${atributo === "id" ? "Id" : "Nome"}`;
    if (possui(perfil, direto)) return perfil[direto] ?? null;
    if (possui(perfil, campo)) return perfil[campo]?.[atributo] ?? null;
    if (possui(base, direto)) return base[direto] ?? null;
    return base[campo]?.[atributo] ?? alternativo;
  };
  const lotacaoNome = referencia("lotacao", "label", principal?.lotacaoNome ?? null);
  return {
    ...base,
    ...perfil,
    perfil: perfilCanonico,
    autoridade: autoridadeCanonica,
    username: dado("username") ?? dado("user") ?? null,
    email: dado("email") ?? null,
    matricula: dado("matricula") ?? null,
    vinculosGestao,
    jornada: dado("jornada") ?? null,
    jornadaId: referencia("jornada", "id", principal?.jornadaId ?? null),
    turno: possui(perfil, "turno") ? perfil.turno : referencia("jornada", "label", dado("turno") ?? principal?.jornadaNome ?? null),
    lotacaoObj: dado("lotacao") ?? null,
    lotacaoId: referencia("lotacao", "id", principal?.lotacaoId ?? null),
    lotacaoNome,
    lotacao: lotacaoNome,
    superintendencia: dado("superintendencia") ?? null,
    superintendenciaId: referencia("superintendencia", "id"),
    superintendenciaNome: referencia("superintendencia", "label"),
    uf: possui(perfil, "uf") ? perfil.uf : possui(perfil, "superintendencia") ? perfil.superintendencia?.uf ?? null : base.superintendencia?.uf ?? base.uf ?? null,
    gestaoLocal: dado("gestaoLocal") ?? null,
    gestaoLocalId: referencia("gestaoLocal", "id"),
    gestaoLocalNome: referencia("gestaoLocal", "label"),
    isRetroactiveUnlocked: Boolean(dado("isRetroactiveUnlocked") ?? dado("retroactiveUnlocked")),
    retroactiveExpireAt: dado("retroactiveExpireAt") ?? null,
    forceChangePassword: Boolean(dado("forceChangePassword")),
    twoFactorEnabled: Boolean(dado("twoFactorEnabled")),
    twoFactorVerified: Boolean(dado("twoFactorVerified")),
    fotoUrl: dado("fotoUrl") ?? null,
    createdAt: dado("createdAt") ?? null,
  };
};

const chaveContextoUsuario = (user) => user ? JSON.stringify([
  user.id ?? null, user.username, user.perfil, user.autoridade,
  user.superintendenciaId, user.gestaoLocalId, user.lotacaoId, user.jornadaId,
  user.vinculosGestao.map(v => [v.lotacaoId, v.jornadaId]).sort(),
  user.forceChangePassword, user.twoFactorEnabled, user.twoFactorVerified,
  user.isRetroactiveUnlocked, user.retroactiveExpireAt,
]) : null;

const identidadeCompativel = (base, proximo) => !base?.id || !proximo?.id || String(base.id) === String(proximo.id);
const resultadoCancelado = () => ({ error: true, canceled: true, silencioso: true });

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
  const [geracaoSessao, setGeracaoSessao] = useState(obterGeracaoSessao);

  const referenciaUsuario = useRef(null);
  const montadoRef = useRef(true);
  const geracaoRef = useRef(obterGeracaoSessao());
  const ordemConsultaRef = useRef(0);
  const controladorConsultaRef = useRef(null);
  const encerramentoRef = useRef(null);
  const sessaoExpiradaRef = useRef(false);
  const avisoExpiracao5MinMostrado = useRef(false);
  const avisoExpiracao1MinMostrado = useRef(false);
  const requisicoesSessao = useRef({ leitura: null, toque: null });
  const chaveArmazenamentoFoto = `${CONFIG.APP_NAME.toLowerCase()}_foto_url`;
  const chaveLegadaUsuario = `${CONFIG.APP_NAME.toLowerCase()}_user_app`;

  const avancarGeracao = useCallback((geracaoJaInvalidada) => {
    const geracao = geracaoJaInvalidada ?? invalidarGeracaoSessao();
    geracaoRef.current = geracao;
    ordemConsultaRef.current += 1;
    controladorConsultaRef.current?.abort();
    try { sessionStorage.removeItem("sgt_exportacoes_relatorios"); } catch {}
    requisicoesSessao.current = { leitura: null, toque: null };
    setGeracaoSessao(geracao);
    return geracao;
  }, []);

  const publicarUsuario = useCallback((proximo, invalidarContexto = true) => {
    if (invalidarContexto && referenciaUsuario.current
      && chaveContextoUsuario(referenciaUsuario.current) !== chaveContextoUsuario(proximo)) avancarGeracao();
    referenciaUsuario.current = proximo;
    setUsuario(proximo);
    try {
      if (proximo?.fotoUrl) localStorage.setItem(chaveArmazenamentoFoto, proximo.fotoUrl);
      else localStorage.removeItem(chaveArmazenamentoFoto);
    } catch {}
  }, [avancarGeracao, chaveArmazenamentoFoto]);

  const limparDadosSessao = useCallback(() => {
    publicarUsuario(null, false);
    try { localStorage.removeItem(chaveLegadaUsuario); } catch {}
    setDetalhesSessao({ expiraEmEpochMs: null, offsetServidorMs: 0 });
    setMostrarModalExpiracao(false);
    avisoExpiracao5MinMostrado.current = false;
    avisoExpiracao1MinMostrado.current = false;
  }, [chaveLegadaUsuario, publicarUsuario]);

  const limparSessaoExterna = useCallback(() => {
    // O cookie HttpOnly pode já pertencer a B. A observação externa invalida
    // apenas esta aba: não registra saída global nem envia revogação HTTP.
    exigirLoginExplicitoLocal();
    avancarGeracao();
    encerramentoRef.current = null;
    limparDadosSessao();
    setCarregandoSistema(false);
    setEncerrandoSessao(false);
    setEstadoAutenticacao(ESTADOS_AUTENTICACAO.NAO_AUTENTICADO);
    sessaoExpiradaRef.current = false;
    notificar('warning', 'O contexto da sessão mudou. Entre novamente para continuar.');
  }, [avancarGeracao, limparDadosSessao]);

  const iniciarConsulta = useCallback(() => {
    controladorConsultaRef.current?.abort();
    const controlador = new AbortController();
    controladorConsultaRef.current = controlador;
    return { geracao: obterGeracaoSessao(), ordem: ++ordemConsultaRef.current, signal: controlador.signal };
  }, []);
  const consultaAtual = useCallback((consulta) => montadoRef.current && sessaoAtual(consulta.geracao) && consulta.ordem === ordemConsultaRef.current, []);
  const opcoesDaConsulta = (consulta) => ({ geracaoSessao: consulta.geracao, signal: consulta.signal });
  const eAdministrador = useMemo(() => eAdministradorTecnico(usuario), [usuario]);

  const consultarSessao = useCallback((options = {}) => {
    const geracao = options.geracaoSessao ?? obterGeracaoSessao();
    const touch = Boolean(options.touch);
    const chave = touch ? "toque" : "leitura";
    const existente = requisicoesSessao.current[chave];
    if (existente?.geracao === geracao && !existente.signal?.aborted) return existente.promessa;
    const promessa = Promise.resolve(ServicoAutenticacao.informacoesSessao({
      touch, skipLoading: true, ...options, geracaoSessao: geracao,
    })).finally(() => {
      if (requisicoesSessao.current[chave]?.promessa === promessa) requisicoesSessao.current[chave] = null;
    });
    requisicoesSessao.current[chave] = { geracao, promessa, signal: options.signal };
    return promessa;
  }, []);

  const sincronizarDetalhesSessao = useCallback((info) => {
    const expiraEmEpochMs = calcularExpiracao(info);
    if (expiraEmEpochMs) sessaoExpiradaRef.current = false;
    setDetalhesSessao({ expiraEmEpochMs, offsetServidorMs: 0 });
  }, []);

  const tratarErroSessao = useCallback((erro, geracao) => {
    if (erroEncerraSessao(erro)) {
      const codigo = erro?.code || erro?.data?.code;
      dispararSaida(mensagemSessaoEncerrada(codigo, erro?.message || erro?.data?.message), { code: codigo, geracaoSessao: geracao });
    }
  }, []);

  const atualizarInformacoesSessao = useCallback(async (options = {}) => {
    const consulta = iniciarConsulta();
    try {
      const info = await consultarSessao({ ...options, ...opcoesDaConsulta(consulta) });
      if (!consultaAtual(consulta)) return null;
      const atual = referenciaUsuario.current;
      if (info?.authenticated === false || !identidadeCompativel(atual, info?.user)) {
        throw Object.assign(new Error("A identidade da sessão mudou. Entre novamente."), { status: 401, code: "SESSION_INVALID" });
      }
      const mesclado = atual && info?.user ? normalizarUsuarioAutenticado(atual, info.user) : atual;
      sincronizarDetalhesSessao(info);
      if (atual && !mesclado) throw Object.assign(new Error("Perfil da sessão inválido."), { status: 401, code: "SESSION_INVALID" });
      if (mesclado) {
        publicarUsuario(mesclado);
        setEstadoAutenticacao(estadoAutenticadoPorUsuario(mesclado));
      }
      return info;
    } catch (erro) {
      if (!consultaAtual(consulta) || erro?.canceled) return null;
      tratarErroSessao(erro, consulta.geracao);
      throw erro;
    }
  }, [iniciarConsulta, consultarSessao, consultaAtual, publicarUsuario, sincronizarDetalhesSessao, tratarErroSessao]);

  const atualizarPerfil = useCallback(async (usuarioBase = null) => {
    const base = usuarioBase || referenciaUsuario.current;
    // Revalidação não cria uma sessão; somente bootstrap/login podem fazê-lo.
    if (!base) return null;
    const consulta = iniciarConsulta();
    try {
      const perfilRemoto = await ServicoAutenticacao.buscarMeuPerfil(opcoesDaConsulta(consulta));
      if (!consultaAtual(consulta)) return null;
      if (!identidadeCompativel(base, perfilRemoto)) throw Object.assign(new Error("A identidade da sessão mudou."), { status: 401, code: "SESSION_INVALID" });
      const mesclado = normalizarUsuarioAutenticado(base, perfilRemoto);
      if (!mesclado) throw Object.assign(new Error("Perfil/autoridade canônicos ausentes na resposta do servidor."), { status: 401, code: "SESSION_INVALID" });
      publicarUsuario(mesclado);
      setEstadoAutenticacao(estadoAutenticadoPorUsuario(mesclado));
      return mesclado;
    } catch (erro) {
      if (!consultaAtual(consulta) || erro?.canceled) return null;
      tratarErroSessao(erro, consulta.geracao);
      return null;
    }
  }, [consultaAtual, iniciarConsulta, publicarUsuario, tratarErroSessao]);

  const realizarLogout = useCallback((geracaoJaInvalidada) => {
    if (houveTrocaSessaoExterna()) { limparSessaoExterna(); return; }
    const haviaSessao = Boolean(referenciaUsuario.current);
    const pendente = obterRevogacaoEmAndamento();
    const reutilizar = pendente && sessaoAtual(pendente.geracao);
    if (!reutilizar && !anunciarTrocaSessao()) {
      registrarIntencaoSaida();
      limparSessaoExterna();
      return;
    }
    const intencao = reutilizar ? pendente.intencao : registrarIntencaoSaida();
    // Uma segunda saída cancela intenções de login/consulta, mas não aborta o
    // próprio POST de revogação que o próximo login precisa aguardar.
    const geracao = avancarGeracao(reutilizar ? pendente.geracao
      : typeof geracaoJaInvalidada === "number" ? geracaoJaInvalidada : undefined);
    encerramentoRef.current = geracao;
    limparDadosSessao();
    setCarregandoSistema(false);
    setEncerrandoSessao(haviaSessao);
    setEstadoAutenticacao(haviaSessao ? ESTADOS_AUTENTICACAO.SESSAO_ENCERRANDO : ESTADOS_AUTENTICACAO.NAO_AUTENTICADO);
    sessaoExpiradaRef.current = true;
    iniciarRevogacaoCoordenada({
      intencao, geracao,
      executar: () => ServicoAutenticacao.encerrarSessao({ geracaoSessao: geracao, signal: obterSinalSessao(geracao) }),
    });
  }, [avancarGeracao, limparDadosSessao, limparSessaoExterna]);

  const concluirSaida = useCallback(async () => {
    if (encerramentoRef.current == null || !sessaoAtual(encerramentoRef.current)) return;
    encerramentoRef.current = null;
    limparDadosSessao();
    redefinirIndicadorRedirecionamento?.();
    window.history.pushState(null, "", "/login");
    setEncerrandoSessao(false);
    setEstadoAutenticacao(ESTADOS_AUTENTICACAO.NAO_AUTENTICADO);
    sessaoExpiradaRef.current = false;
  }, [limparDadosSessao]);

  useEffect(() => {
    montadoRef.current = true;
    geracaoRef.current = obterGeracaoSessao();
    setGeracaoSessao(geracaoRef.current);
    try { localStorage.removeItem(chaveLegadaUsuario); } catch {}
    const aoLogout = (evento) => realizarLogout(evento.detail?.geracaoSessao);
    window.addEventListener(EVENT_LOGOUT, aoLogout);
    const aoTrocarSessaoExterna = () => {
      if (houveTrocaSessaoExterna()) limparSessaoExterna();
    };
    const aoArmazenar = evento => {
      if (evento.key === CHAVE_TROCA_SESSAO || evento.key === null) aoTrocarSessaoExterna();
    };
    window.addEventListener('storage', aoArmazenar);
    window.addEventListener('focus', aoTrocarSessaoExterna);
    window.addEventListener(EVENT_TROCA_SESSAO_EXTERNA, aoTrocarSessaoExterna);
    const trocaAntesDaMontagem = houveTrocaSessaoExterna();
    if (trocaAntesDaMontagem) limparSessaoExterna();
    const consulta = iniciarConsulta();
    const bootstrap = async () => {
      if (trocaAntesDaMontagem) return;
      setEstadoAutenticacao(ESTADOS_AUTENTICACAO.RESTAURANDO_SESSAO);
      try {
        if (obterIntencaoSaida() || restauracaoLocalBloqueada()) {
          limparDadosSessao();
          setEstadoAutenticacao(ESTADOS_AUTENTICACAO.NAO_AUTENTICADO);
          return;
        }
        const info = await consultarSessao({ touch: false, skipAuthLogout: true, ...opcoesDaConsulta(consulta) });
        if (!consultaAtual(consulta)) return;
        if (!info?.authenticated) throw Object.assign(new Error("Sessão não autenticada."), { status: 401 });
        const perfil = await ServicoAutenticacao.buscarMeuPerfil(opcoesDaConsulta(consulta));
        if (!consultaAtual(consulta)) return;
        if (obterIntencaoSaida()) {
          limparDadosSessao();
          setEstadoAutenticacao(ESTADOS_AUTENTICACAO.NAO_AUTENTICADO);
          return;
        }
        if (!identidadeCompativel(info.user, perfil)) throw new Error("Identidade divergente na restauração.");
        const mesclado = normalizarUsuarioAutenticado(info.user || {}, perfil);
        if (!mesclado) throw new Error("Sessão sem perfil/autoridade canônicos.");
        publicarUsuario(mesclado, false);
        sincronizarDetalhesSessao(info);
        setEstadoAutenticacao(estadoAutenticadoPorUsuario(mesclado));
      } catch (erro) {
        if (!consultaAtual(consulta)) return;
        tratarErroSessao(erro, consulta.geracao);
        if (consultaAtual(consulta)) {
          limparDadosSessao();
          setEstadoAutenticacao(ESTADOS_AUTENTICACAO.NAO_AUTENTICADO);
        }
      } finally {
        if (consultaAtual(consulta)) setCarregandoSistema(false);
      }
    };
    void bootstrap();
    return () => {
      montadoRef.current = false;
      controladorConsultaRef.current?.abort();
      window.removeEventListener(EVENT_LOGOUT, aoLogout);
      window.removeEventListener('storage', aoArmazenar);
      window.removeEventListener('focus', aoTrocarSessaoExterna);
      window.removeEventListener(EVENT_TROCA_SESSAO_EXTERNA, aoTrocarSessaoExterna);
      const revogacao = obterRevogacaoEmAndamento();
      if (sessaoAtual(geracaoRef.current) && revogacao?.geracao !== geracaoRef.current) {
        geracaoRef.current = invalidarGeracaoSessao();
      }
      requisicoesSessao.current = { leitura: null, toque: null };
    };
  }, [chaveLegadaUsuario, consultarSessao, consultaAtual, iniciarConsulta, limparDadosSessao, limparSessaoExterna, publicarUsuario, realizarLogout, sincronizarDetalhesSessao, tratarErroSessao]);

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
    if (houveTrocaSessaoExterna()) limparSessaoExterna();
    // Preservar o POST de saída já em andamento; caso contrário cancelar as
    // intenções locais antes de aguardar qualquer etapa do novo login.
    if (!obterRevogacaoEmAndamento()) avancarGeracao();
    // Publicar antes de enviar credenciais: o servidor pode instalar o novo
    // cookie antes de a resposta de autenticação/perfil chegar a esta aba.
    if (!anunciarTrocaSessao()) {
      limparSessaoExterna();
      return { error: true, code: 'SESSION_COORDINATION_UNAVAILABLE', message: 'Não foi possível coordenar a sessão entre as abas. Habilite o armazenamento ou os cookies deste site e tente novamente.' };
    }
    const espera = iniciarConsulta();
    const intencaoSaida = obterIntencaoSaida();
    encerramentoRef.current = null;
    redefinirIndicadorRedirecionamento?.();
    limparDadosSessao();
    setEncerrandoSessao(false);
    setCarregandoSistema(false);
    setEstadoAutenticacao(ESTADOS_AUTENTICACAO.CREDENCIAIS_ACEITAS);
    let revogacao = obterRevogacaoEmAndamento();
    if (!revogacao && intencaoSaida && !revogacaoFoiConfirmada(intencaoSaida)) {
      revogacao = iniciarRevogacaoCoordenada({
        intencao: intencaoSaida, geracao: espera.geracao,
        executar: () => ServicoAutenticacao.encerrarSessao({ geracaoSessao: espera.geracao, signal: obterSinalSessao(espera.geracao) }),
      });
    }
    if (revogacao) {
      const confirmada = await revogacao.promessa;
      if (!consultaAtual(espera)) return resultadoCancelado();
      if (!confirmada) {
        setEstadoAutenticacao(ESTADOS_AUTENTICACAO.NAO_AUTENTICADO);
        return { error: true, code: "SESSION_LOGOUT_UNCONFIRMED", message: "A saída local foi mantida, mas não foi possível confirmar o encerramento no servidor. Tente entrar novamente." };
      }
    }
    if (!consultaAtual(espera)) return resultadoCancelado();
    avancarGeracao();
    const consulta = iniciarConsulta();
    try {
      const retorno = await ServicoAutenticacao.autenticar(matricula, password, totp, opcoesDaConsulta(consulta));
      if (!consultaAtual(consulta) || retorno?.canceled) return resultadoCancelado();
      if (retorno?.requiresTwoFactor) {
        setEstadoAutenticacao(ESTADOS_AUTENTICACAO.SEGUNDO_FATOR_REQUERIDO);
        return retorno;
      }
      if (retorno?.error) {
        setEstadoAutenticacao(totp ? ESTADOS_AUTENTICACAO.SEGUNDO_FATOR_REQUERIDO : ESTADOS_AUTENTICACAO.NAO_AUTENTICADO);
        return retorno;
      }
      const perfil = await ServicoAutenticacao.buscarMeuPerfil(opcoesDaConsulta(consulta));
      if (!consultaAtual(consulta)) return resultadoCancelado();
      if (!identidadeCompativel(retorno, perfil)) throw new Error("Identidade divergente no login.");
      const base = normalizarUsuarioAutenticado(retorno, perfil);
      if (!base) throw new Error("Perfil/autoridade canônicos ausentes.");
      const info = await consultarSessao({ touch: false, silentError: true, ...opcoesDaConsulta(consulta) });
      if (!consultaAtual(consulta)) return resultadoCancelado();
      if (!info?.authenticated || !identidadeCompativel(base, info.user)) throw new Error("Não foi possível confirmar a identidade da sessão.");
      const mesclado = normalizarUsuarioAutenticado(base, info.user || {});
      if (!mesclado) throw new Error("Perfil/autoridade canônicos ausentes.");
      if (!confirmarLoginExplicito(intencaoSaida)) {
        limparDadosSessao();
        setEstadoAutenticacao(ESTADOS_AUTENTICACAO.NAO_AUTENTICADO);
        return resultadoCancelado();
      }
      publicarUsuario(mesclado, false);
      sincronizarDetalhesSessao(info);
      setEstadoAutenticacao(estadoAutenticadoPorUsuario(mesclado));
      return mesclado;
    } catch (erro) {
      if (!consultaAtual(consulta) || erro?.canceled) return resultadoCancelado();
      tratarErroSessao(erro, consulta.geracao);
      if (consultaAtual(consulta)) {
        limparDadosSessao();
        setEstadoAutenticacao(ESTADOS_AUTENTICACAO.NAO_AUTENTICADO);
      }
      return { error: true, message: "Não foi possível validar o perfil da sessão." };
    }
  }, [avancarGeracao, consultaAtual, consultarSessao, iniciarConsulta, limparDadosSessao, limparSessaoExterna, publicarUsuario, sincronizarDetalhesSessao, tratarErroSessao]);

  const registrarUsuario = useCallback((u, e, m, se, gl) => ServicoAutenticacao.cadastrar(u, e, m, se, gl), []);

  const valor = useMemo(() => ({
    user: usuario,
    geracaoSessao,
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
    usuario, geracaoSessao, eAdministrador, efetuarLogin, registrarUsuario, realizarLogout,
    carregandoSistema, atualizarPerfil, encerrandoSessao, concluirSaida,
    atualizarInformacoesSessao, detalhesSessao.expiraEmEpochMs, estadoAutenticacao,
  ]);

  return (
    <ContextoAutenticacao.Provider value={valor}>
      {children}
      <ModalReautenticacao
        key={geracaoSessao}
        isOpen={mostrarModalExpiracao}
        onClose={() => setMostrarModalExpiracao(false)}
        onConfirm={async () => {
          if (!sessaoAtual(geracaoSessao)) return;
          await atualizarInformacoesSessao({ touch: false, silentError: true }).catch(() => {});
          if (!sessaoAtual(geracaoSessao)) return;
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
