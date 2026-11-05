import { requisitar } from "../../core/http.js";
import {
  obterGeracaoSessao, obterSinalSessao, sessaoAtual, criarErroSessaoObsoleta, anunciarTrocaSessao,
} from "../../core/security/geracaoSessao.js";

const confirmacoesRecuperacaoEmAndamento = new Map();
let renovacaoSessaoEmAndamento = null;

const confirmarRecuperacaoUmaVez = (token) => {
  const valorToken = String(token || "").trim();
  const geracao = obterGeracaoSessao();
  const chave = `${geracao}:${valorToken}`;
  if (confirmacoesRecuperacaoEmAndamento.has(chave)) {
    return confirmacoesRecuperacaoEmAndamento.get(chave);
  }

  const requisicao = requisitar({
    method: "POST",
    url: "/autenticacao/recuperar-senha/confirmar",
    data: { token: valorToken },
    geracaoSessao: geracao,
    silentError: true,
  });

  confirmacoesRecuperacaoEmAndamento.set(chave, requisicao);
  obterSinalSessao(geracao).addEventListener("abort", () => confirmacoesRecuperacaoEmAndamento.delete(chave), { once: true });
  const liberarCache = () => {
    globalThis.setTimeout(() => confirmacoesRecuperacaoEmAndamento.delete(chave), 30000);
  };
  requisicao.then(liberarCache, liberarCache);
  return requisicao;
};

export const ServicoAutenticacao = {
  autenticar: async (matricula, password, totp, options = {}) => {
    try {
      const res = await requisitar({
        method: "POST",
        url: "/autenticacao/autenticar",
        data: { matricula, password, totp: totp || undefined },
        silentError: true,
        ...options,
      });
      if (res?.requiresTwoFactor) {
        return { ...res, requiresTwoFactor: true };
      }
      return {
        ...res,
        matricula: res?.matricula || matricula,
      };
    } catch (e) {
      if (e?.canceled) return { error: true, canceled: true, silencioso: true };
      if (e?.status === 429) {
        return {
          error: true,
          status: e.status,
          message: e?.data?.message || e?.message || "Muitas tentativas.",
          lockout: e?.data?.lockout || null,
        };
      }
      return {
        error: true,
        status: e?.status,
        code: e?.code || e?.data?.code,
        message: e?.message || "Falha na conexão",
      };
    }
  },
  buscarMeuPerfil: async (options = {}) => requisitar({ method: "GET", url: "/usuarios/meu-perfil", ...options }),
  alterarSenhaInicial: async (newPassword) =>
    requisitar({
      method: "POST",
      url: "/autenticacao/alterar-senha-inicial",
      data: { newPassword },
      silentError: true,
    }),
  listarSuperintendenciasCadastro: async () =>
    requisitar({
      method: "GET",
      url: "/autenticacao/superintendencias",
      silentError: true,
    }),
  listarGestoesLocaisCadastro: async (superintendenciaId, options = {}) =>
    requisitar({
      method: "GET",
      url: "/autenticacao/gestoes-locais",
      params: { superintendenciaId },
      silentError: true,
      ...options,
    }),
  solicitarRecuperacaoSenha: async (email) =>
    requisitar({
      method: "POST",
      url: "/autenticacao/recuperar-senha/solicitar",
      data: { email },
      silentError: true,
    }),
  validarTokenRecuperacaoSenha: async (token) =>
    requisitar({
      method: "GET",
      url: "/autenticacao/recuperar-senha/validar",
      params: { token },
      silentError: true,
    }),
  confirmarRecuperacaoSenha: async (token) => confirmarRecuperacaoUmaVez(token),
  cadastrar: async (username, email, matricula, superintendenciaId, gestaoLocalId) =>
    requisitar({
      method: "POST",
      url: "/autenticacao/cadastrar",
      data: { username, email, matricula, superintendenciaId, gestaoLocalId },
      successMessage: "Solicitação enviada!",
      silentError: true,
    }),
  encerrarSessao: async (options = {}) => {
    try {
      // Logout é idempotente: repetir a chamada é seguro e aumenta a chance
      // de revogação em falhas transitórias sem bloquear a interface.
      await requisitar({
        method: "POST",
        url: "/autenticacao/encerrar-sessao",
        retries: 1,
        retryNonIdempotent: true,
        retryOnStatuses: [0, 502, 503, 504],
        silentError: true,
        skipAuthLogout: true,
        ...options,
      });
    } catch {
      // Saída local não comprova revogação remota em falha ou cancelamento.
      return false;
    }
    return true;
  },
  informacoesSessao: async (options = {}) =>
    requisitar({
      method: "GET",
      url: "/autenticacao/sessao",
      params: { touch: options.touch || false },
      ...options,
    }),
  renovarSessao: (password, options = {}) => {
    const geracao = options.geracaoSessao ?? obterGeracaoSessao();
    if (!sessaoAtual(geracao)) return Promise.reject(criarErroSessaoObsoleta());
    if (renovacaoSessaoEmAndamento?.geracao === geracao) return renovacaoSessaoEmAndamento.promessa;
    // A renovação também substitui o cookie do servidor. A intenção é publicada
    // antes do POST; a própria aba conserva sua geração e a promessa deduplicada.
    // Uma falha HTTP não autoriza outras abas a retomar intenções já invalidadas.
    if (!anunciarTrocaSessao()) return Promise.reject(Object.assign(
      new Error('Não foi possível coordenar a sessão entre as abas. Habilite o armazenamento ou os cookies deste site e tente novamente.'),
      { status: 0, code: 'SESSION_COORDINATION_UNAVAILABLE' },
    ));
    const requisicao = Promise.resolve(requisitar({
      method: "POST",
      url: "/autenticacao/renovar-sessao",
      data: { password },
      ...options,
      geracaoSessao: geracao,
    }));
    const promessa = requisicao.finally(() => {
      if (renovacaoSessaoEmAndamento?.promessa === promessa) renovacaoSessaoEmAndamento = null;
    });
    renovacaoSessaoEmAndamento = { geracao, promessa };
    return promessa;
  },
  validarSenha: async (password, options = {}) =>
    requisitar({
      method: "POST",
      url: "/perfil/validar-senha",
      data: { password },
      ...options,
    }),
};
