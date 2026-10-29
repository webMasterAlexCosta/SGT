import { requisitar } from "../../core/http.js";

const confirmacoesRecuperacaoEmAndamento = new Map();
let renovacaoSessaoEmAndamento = null;

const confirmarRecuperacaoUmaVez = (token) => {
  const chave = String(token || "").trim();
  if (confirmacoesRecuperacaoEmAndamento.has(chave)) {
    return confirmacoesRecuperacaoEmAndamento.get(chave);
  }

  const requisicao = requisitar({
    method: "POST",
    url: "/autenticacao/recuperar-senha/confirmar",
    data: { token: chave },
    silentError: true,
  });

  confirmacoesRecuperacaoEmAndamento.set(chave, requisicao);
  const liberarCache = () => {
    globalThis.setTimeout(() => confirmacoesRecuperacaoEmAndamento.delete(chave), 30000);
  };
  requisicao.then(liberarCache, liberarCache);
  return requisicao;
};

export const ServicoAutenticacao = {
  autenticar: async (matricula, password, totp) => {
    try {
      const res = await requisitar({
        method: "POST",
        url: "/autenticacao/autenticar",
        data: { matricula, password, totp: totp || undefined },
        silentError: true,
      });
      if (res?.requiresTwoFactor) {
        return { ...res, requiresTwoFactor: true };
      }
      return {
        ...res,
        matricula: res?.matricula || matricula,
      };
    } catch (e) {
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
  buscarMeuPerfil: async () => requisitar({ method: "GET", url: "/usuarios/meu-perfil" }),
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
  encerrarSessao: async () => {
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
      });
    } catch (e) {}
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
    if (renovacaoSessaoEmAndamento) return renovacaoSessaoEmAndamento;
    const requisicao = Promise.resolve(requisitar({
      method: "POST",
      url: "/autenticacao/renovar-sessao",
      data: { password },
      ...options,
    }));
    renovacaoSessaoEmAndamento = requisicao.finally(() => {
      if (renovacaoSessaoEmAndamento) renovacaoSessaoEmAndamento = null;
    });
    return renovacaoSessaoEmAndamento;
  },
  validarSenha: async (password, options = {}) =>
    requisitar({
      method: "POST",
      url: "/perfil/validar-senha",
      data: { password },
      ...options,
    }),
};
