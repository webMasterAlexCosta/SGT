import { observarFalha } from "../../core/http/falhaObservada";
import type { LoginResponse, LoginResult, SessionResponse } from '../../contracts/auth';
import type * as DTO from '../../contracts/java';
import type { Page } from '../../contracts/shared';
import type { RequestOptions, QueryParams } from '../../core/http/types';
import { requisitar } from "../../core/http.ts";
import {
  obterGeracaoSessao, obterSinalSessao, sessaoAtual, criarErroSessaoObsoleta, anunciarTrocaSessao,
} from "../../core/security/geracaoSessao.ts";

const confirmacoesRecuperacaoEmAndamento = new Map<string, Promise<DTO.MensagemApiDTO>>();
let renovacaoSessaoEmAndamento: { geracao: number; promessa: Promise<DTO.MensagemApiDTO> } | null = null;

const confirmarRecuperacaoUmaVez = (token: (string)) => {
  const valorToken = String(token || "").trim();
  const geracao = obterGeracaoSessao();
  const chave = `${geracao}:${valorToken}`;
  if (confirmacoesRecuperacaoEmAndamento.has(chave)) {
    return confirmacoesRecuperacaoEmAndamento.get(chave);
  }

  const requisicao = requisitar<DTO.MensagemApiDTO>({
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
  autenticar: async (matricula: string, password: string, totp?: string, options: RequestOptions = {}): Promise<LoginResult> => {
    try {
      const res = await requisitar<LoginResponse>({
        method: "POST",
        url: "/autenticacao/autenticar",
        data: { matricula, password, totp: totp || undefined },
        silentError: true,
        ...options,
      });
      if (res?.requiresTwoFactor === true) {
        return { ...res, requiresTwoFactor: true as const };
      }
      return {
        ...(res as import("../../contracts/auth").LoginSuccess),
        matricula: (res as import("../../contracts/auth").LoginSuccess)?.matricula || matricula,
      };
    } catch (causa) {
      const e = observarFalha(causa);
      const dados = e.data !== null && typeof e.data === "object" ? e.data as Record<string, unknown> : {};
      if (e?.canceled) return { error: true, canceled: true, silencioso: true };
      if (e?.status === 429) {
        return {
          error: true,
          status: e.status,
          message: (typeof dados.message === "string" ? dados.message : undefined) || e.message || "Muitas tentativas.",
          lockout: typeof dados.lockout === "number" ? dados.lockout : null,
        };
      }
      return {
        error: true,
        status: e?.status,
        code: e.code || (typeof dados.code === "string" ? dados.code : undefined),
        message: e?.message || "Falha na conexão",
      };
    }
  },
  buscarMeuPerfil: async (options: RequestOptions = {}) => requisitar<DTO.MeuPerfilDTO>({ method: "GET", url: "/usuarios/meu-perfil", ...options }),
  alterarSenhaInicial: async (newPassword: (string)) =>
    requisitar({
      method: "POST",
      url: "/autenticacao/alterar-senha-inicial",
      data: { newPassword },
      silentError: true,
    }),
  listarSuperintendenciasCadastro: async () =>
    requisitar<Array<DTO.AutenticacaoSuperintendenciaDTO>>({
      method: "GET",
      url: "/autenticacao/superintendencias",
      silentError: true,
    }),
  listarGestoesLocaisCadastro: async (superintendenciaId: number | string, options: RequestOptions = {}) =>
    requisitar<Array<DTO.AutenticacaoGestaoLocalDTO>>({
      method: "GET",
      url: "/autenticacao/gestoes-locais",
      params: { superintendenciaId },
      silentError: true,
      ...options,
    }),
  solicitarRecuperacaoSenha: async (email: (string)) =>
    requisitar<DTO.MensagemApiDTO>({
      method: "POST",
      url: "/autenticacao/recuperar-senha/solicitar",
      data: { email },
      silentError: true,
    }),
  validarTokenRecuperacaoSenha: async (token: (string)) =>
    requisitar<DTO.ValidacaoTokenRecuperacaoSenhaDTO>({
      method: "GET",
      url: "/autenticacao/recuperar-senha/validar",
      params: { token },
      silentError: true,
    }),
  confirmarRecuperacaoSenha: async (token: (string)) => confirmarRecuperacaoUmaVez(token),
  cadastrar: async (username: (string), email: (string), matricula: (string), superintendenciaId: number | string, gestaoLocalId: number | string) =>
    requisitar({
      method: "POST",
      url: "/autenticacao/cadastrar",
      data: { username, email, matricula, superintendenciaId, gestaoLocalId },
      successMessage: "Solicitação enviada!",
      silentError: true,
    }),
  encerrarSessao: async (options: RequestOptions = {}) => {
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
  informacoesSessao: async (options: RequestOptions & { touch?: boolean; skipLoading?: boolean } = {}) =>
    requisitar<SessionResponse>({
      method: "GET",
      url: "/autenticacao/sessao",
      params: { touch: (options as RequestOptions & { touch?: boolean }).touch || false },
      ...options,
    }),
  renovarSessao: (password: (string), options: RequestOptions = {}) => {
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
    const requisicao = Promise.resolve(requisitar<DTO.MensagemApiDTO>({
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
  validarSenha: async (password: (string), options: RequestOptions = {}) =>
    requisitar({
      method: "POST",
      url: "/perfil/validar-senha",
      data: { password },
      ...options,
    }),
};
