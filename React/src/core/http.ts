import type { AxiosResponse, InternalAxiosRequestConfig, AxiosHeaderValue } from 'axios';
import type { HttpContext, InternalRequestOptions, RequestOptions, HeaderCollection, QueryParams } from './http/types';
import type { Page, PageMeta } from '../contracts/shared';
// src/core/http.ts
import axios, { AxiosHeaders } from 'axios';
import { notificar, dispararSaida, dispararConexaoPerdida, dispararConexaoRestaurada } from './events';
import { CONFIG } from './config';
import { criarConfiguracaoRequisicaoAxios } from './http/configuracaoRequisicaoAxios';
import { serializarParametros } from './http/serializadorParametros';
import { gerarIdRequisicao } from './http/requestId';
import { obterMaximoTentativas, podeTentarNovamente, aguardarNovaTentativa, mutacaoPodeSerRepetida } from './http/retryPolicy';
import { configurarBuscadorTokenCsrf, invalidarTokenCsrf, obterTokenCsrf } from './http/coordenadorCsrf';
import { ehMutacaoHttp, normalizarMetodoHttp } from './http/metodosHttp';
import { removerContentTypeMultipart } from './http/multipart';
import { normalizarRespostaAxios, obterContextoResposta } from './http/respostaHttp';
import { normalizarErroHttp, ehErroCsrfIdentificado, decodificarCorpoErroHttp } from './http/erroHttp';
import { solicitarConfirmacaoSenha } from './http/coordenadorConfirmacaoSenha';
import { obterDadosDownload } from './http/download';

import { criarErroSessaoObsoleta, obterGeracaoSessao, obterSinalSessao, sessaoAtual } from './security/geracaoSessao.ts';
import { erroEncerraSessao as ehErroQueEncerraSessao } from './security/estadoAutenticacao.ts';

const MENSAGENS_SESSAO: Record<string, string> = {
  SESSION_REPLACED: 'A sua conta foi acessada em outro dispositivo.',
  SESSION_EXPIRED: 'A sua sessão expirou por inatividade ou tempo limite.',
  SESSION_IDLE_TIMEOUT: 'A sua sessão expirou por inatividade. Entre novamente.',
  SESSION_INVALID: 'Sessão inválida.',
  SESSION_CLAIMS_ERROR: 'Não foi possível validar os dados da sessão.',
  SESSION_DEVICE_MISSING: 'Sessão não reconhecida neste dispositivo.',
  SESSION_DEVICE_MISMATCH: 'Dispositivo bloqueado para esta sessão.',
  SESSION_MISSING: 'Sessão não encontrada no servidor.',
  ACCOUNT_INACTIVE: 'A sua conta encontra-se inativa ou pendente de aprovação.',
  ACCOUNT_LOCKED: 'A sua conta está temporariamente bloqueada.',
  USER_NOT_FOUND: 'Usuário não encontrado no sistema.',
};

const URLS_QUE_MUDAM_CONTEXTO_SESSAO = [
  '/autenticacao/autenticar',
  '/autenticacao/encerrar-sessao',
  '/autenticacao/alterar-senha-inicial',
];

const setHeader = (headers: NonNullable<HeaderCollection>, nome: string, valor: AxiosHeaderValue) => {
  if (headers instanceof AxiosHeaders) headers.set(nome, valor);
  else headers[nome] = valor;
};

const getHeader = (headers: HeaderCollection, nome: string) => {
  if (!headers) return null;
  if (headers instanceof AxiosHeaders) return headers.get(nome) || headers.get(nome.toLowerCase()) || null;
  const chave = Object.keys(headers).find((item) => item.toLowerCase() === nome.toLowerCase());
  return chave ? headers[chave] : null;
};

const deleteHeader = (headers: HeaderCollection, nome: string) => {
  if (!headers) return;
  if (headers instanceof AxiosHeaders) {
    headers.delete(nome);
    headers.delete(nome.toLowerCase());
    return;
  }
  Object.keys(headers).forEach((chave) => {
    if (chave.toLowerCase() === nome.toLowerCase()) delete headers[chave];
  });
};

const removerSenhaDaConfiguracao = (config: InternalRequestOptions) => {
  deleteHeader(config?.headers, 'X-User-Password');
  if (config && Object.prototype.hasOwnProperty.call(config, 'senhaConfirmacao')) delete config.senhaConfirmacao;
};

// Abort reduz trabalho; a comparação da geração continua sendo o oráculo
// após cada await, inclusive quando o transporte ignora o cancelamento.
const verificarSessaoRequisicao = (config: InternalRequestOptions) => {
  if ((config?.__geracaoSessao != null && !sessaoAtual(config.__geracaoSessao)) || config?.signal?.aborted) {
    removerSenhaDaConfiguracao(config);
    throw criarErroSessaoObsoleta();
  }
};

const vincularSinalSessao = (config: InternalRequestOptions) => {
  const sinalSessao = obterSinalSessao(config.__geracaoSessao);
  if (!config.signal || config.signal === sinalSessao) {
    config.signal = sinalSessao;
    return;
  }
  if (config.__sinalSessao === sinalSessao) return;
  const origem = config.signal;
  const controlador = new AbortController();
  const cancelar = () => controlador.abort();
  origem.addEventListener('abort', cancelar, { once: true });
  sinalSessao.addEventListener('abort', cancelar, { once: true });
  config.__limparSinaisSessao = () => {
    origem.removeEventListener('abort', cancelar);
    sinalSessao.removeEventListener('abort', cancelar);
  };
  config.__sinalSessao = sinalSessao;
  config.signal = controlador.signal;
  if (origem.aborted || sinalSessao.aborted) cancelar();
};

const ehEndpointAutenticacao = (config: InternalRequestOptions) => String(config?.url || '').includes('/autenticacao/');
const urlMudaContextoSessao = (config: InternalRequestOptions) => URLS_QUE_MUDAM_CONTEXTO_SESSAO.some((trecho) => String(config?.url || '').includes(trecho));

const descreverConfirmacao = (config: InternalRequestOptions) => {
  const fornecida = config?.confirmacaoSenha || {};
  const metodo = normalizarMetodoHttp(config?.method);
  const acaoPadrao = metodo === 'DELETE' ? 'Excluir registro' : metodo === 'POST' ? 'Criar ou executar operação' : 'Alterar registro';
  const url = String(config?.url || 'operação protegida');
  return {
    titulo: fornecida.titulo || 'Confirmação de Segurança',
    mensagem: fornecida.mensagem || 'O servidor exige confirmação de senha para concluir esta operação.',
    acao: fornecida.acao || acaoPadrao,
    entidade: fornecida.entidade || url,
    consequencia: fornecida.consequencia || 'A alteração será efetivada no sistema após a validação da sua senha.',
  };
};

export const api = axios.create({
  baseURL: CONFIG.API_URL,
  withCredentials: true,
  timeout: CONFIG.HTTP_TIMEOUT,
  paramsSerializer: {
    serialize: serializarParametros,
  },
});

configurarBuscadorTokenCsrf(async ({ geracaoSessao }) => requisitar({
  geracaoSessao,
  method: 'GET',
  url: '/autenticacao/csrf',
  retries: 0,
  silentError: true,
  skipAuthLogout: true,
}));

export const requisitar = async <T = unknown, D = unknown>(options: RequestOptions<D>): Promise<T> => {
  const config = criarConfiguracaoRequisicaoAxios(options);
  config.__geracaoSessao = options?.geracaoSessao ?? obterGeracaoSessao();
  return await api.request<unknown, unknown>(config) as T;
};

export const requisitarEnvelope = async <T = unknown, D = unknown>(options: RequestOptions<D>): Promise<HttpContext<T>> => {
  const config = criarConfiguracaoRequisicaoAxios({ ...options, retornarEnvelopeHttp: true });
  config.__geracaoSessao = options?.geracaoSessao ?? obterGeracaoSessao();
  return api.request<unknown, HttpContext<T>>(config);
};

export const requisitarDownload = async (options: RequestOptions, fallbackNome = 'download') => {
  const contexto = await requisitarEnvelope<Blob>({ ...options, responseType: options?.responseType || 'blob' });
  return obterDadosDownload(contexto, fallbackNome);
};

export const extrairLista = <T,>(res: T[] | Page<T> | HttpContext<T[] | Page<T>> | null | undefined): T[] => {
  const contexto = res && 'statusHttp' in res ? res : obterContextoResposta(res) as HttpContext<T[] | Page<T>> | null;
  const dados = contexto ? contexto.data : res;
  if (Array.isArray(dados)) return dados;
  if (dados && 'content' in dados && Array.isArray(dados.content)) return dados.content;
  return [];
};

export const extrairPagina = <T,>(res: T[] | Page<T> | HttpContext<T[] | Page<T>> | null | undefined): Page<T> => {
  const contexto = res && 'statusHttp' in res ? res : obterContextoResposta(res) as HttpContext<T[] | Page<T>> | null;
  const dados = contexto ? contexto.data : res;
  if (dados && 'content' in dados && Array.isArray(dados.content)) return dados;

  const lista = Array.isArray(dados) ? dados : extrairLista(dados);
  const meta: Partial<PageMeta> = contexto?.meta || {};
  return {
    content: lista,
    totalElements: Number(meta.totalElements ?? lista.length),
    totalPages: Number(meta.totalPages ?? (lista.length ? 1 : 0)),
    number: Number(meta.page ?? 0),
    size: Number(meta.size ?? lista.length),
    first: meta.first ?? true,
    last: meta.last ?? true,
    sort: meta.sort ?? null,
  };
};

export { criarConfiguracaoRequisicaoAxios, obterContextoResposta };

let estaDesconectado = false;

api.interceptors.request.use(
  async (originalConfig: InternalAxiosRequestConfig<unknown, QueryParams>) => {
    const config = originalConfig as InternalAxiosRequestConfig<unknown, QueryParams> & InternalRequestOptions;
    config.__geracaoSessao ??= obterGeracaoSessao();
    verificarSessaoRequisicao(config);
    vincularSinalSessao(config);
    try {
      const headers = (config.headers = AxiosHeaders.from(config.headers));
      if (!getHeader(headers, 'X-Request-Id')) setHeader(headers, 'X-Request-Id', gerarIdRequisicao());
      setHeader(headers, 'X-Client', CONFIG.APP_CLIENT_ID);
      setHeader(headers, 'X-Client-Version', CONFIG.APP_VERSION);
      setHeader(headers, 'X-Timezone', Intl.DateTimeFormat().resolvedOptions().timeZone || 'UTC');

      removerContentTypeMultipart(headers, config.data);

      if (ehMutacaoHttp(config.method) && !config.__csrfBootstrap) {
        const token = await obterTokenCsrf({ geracaoSessao: config.__geracaoSessao });
        verificarSessaoRequisicao(config);
        setHeader(headers, token.headerName, token.token);
        config.__csrfHeaderName = token.headerName;
      }

      return config;
    } catch (erro) {
      if (erro !== null && typeof erro === 'object' && !('config' in erro && erro.config)) {
        Object.assign(erro, { config });
      }
      throw erro;
    }
  },
  (error: unknown) => Promise.reject(error),
);

const responseInterceptors = api.interceptors.response as unknown as {
  use(fulfilled: (response: AxiosResponse<unknown, unknown, Record<string, unknown>, QueryParams> & { config: InternalAxiosRequestConfig<unknown, QueryParams> & InternalRequestOptions }) => unknown,
      rejected: (error: unknown) => Promise<unknown>): number;
};
responseInterceptors.use(
  (response) => {
    response?.config?.__limparSinaisSessao?.();
    verificarSessaoRequisicao(response?.config);
    removerSenhaDaConfiguracao(response?.config);

    if (estaDesconectado) {
      estaDesconectado = false;
      dispararConexaoRestaurada();
    }

    if (urlMudaContextoSessao(response?.config)) invalidarTokenCsrf();

    const successMessage = response.config?.successMessage;
    if (successMessage) notificar('success', successMessage);

    const contexto = normalizarRespostaAxios<unknown>(response);
    return response.config?.retornarEnvelopeHttp ? contexto : contexto.data;
  },
  async (original) => {
    const errorOriginal = original as import('./http/types').TransportError;
    const cfg = errorOriginal?.config || {};
    try {
      verificarSessaoRequisicao(cfg);
      await decodificarCorpoErroHttp(errorOriginal);
      verificarSessaoRequisicao(cfg);
      const status = Number(errorOriginal?.response?.status || 0);
      const tinhaSenhaConfirmacao = Boolean(getHeader(cfg.headers, 'X-User-Password'));
      removerSenhaDaConfiguracao(cfg);

      const erro = normalizarErroHttp(errorOriginal);
      if (erro.canceled) return Promise.reject(erro);

      if (erro.confirmacaoSenhaNecessaria && ehMutacaoHttp(cfg.method) && !cfg.__confirmacaoSenhaReplay) {
        try {
          const descricao = descreverConfirmacao(cfg);
          const resultado = await solicitarConfirmacaoSenha({
            ...descricao,
            requestId: erro.requestId,
            signal: cfg.signal,
            executar: async (senha) => {
              verificarSessaoRequisicao(cfg);
              const replay = {
                ...cfg,
                headers: typeof cfg.headers?.toJSON === 'function' ? { ...cfg.headers.toJSON() } : { ...(cfg.headers || {}) },
                __confirmacaoSenhaReplay: true,
                silentError: true,
              };
              setHeader(replay.headers, 'X-User-Password', String(senha || ''));
              try {
                return await api.request<unknown, unknown, unknown, QueryParams>(replay);
              } finally {
                removerSenhaDaConfiguracao(replay);
              }
            },
          });
          verificarSessaoRequisicao(cfg);
          return resultado;
        } catch (erroConfirmacao) {
          return Promise.reject(erroConfirmacao);
        }
      }

      const podeRenovarCsrf = ehErroCsrfIdentificado(erro)
        && ehMutacaoHttp(cfg.method)
        && !cfg.__csrfRenovado
        && (cfg.csrfRetrySeguro || cfg.mutacaoIdempotente || mutacaoPodeSerRepetida(cfg));

      if (podeRenovarCsrf) {
        cfg.__csrfRenovado = true;
        const novoToken = await obterTokenCsrf({ forcarRenovacao: true, geracaoSessao: cfg.__geracaoSessao });
        verificarSessaoRequisicao(cfg);
        cfg.headers = cfg.headers instanceof AxiosHeaders ? cfg.headers : AxiosHeaders.from(Object.fromEntries(Object.entries(cfg.headers || {}).filter((entry): entry is [string, AxiosHeaderValue] => entry[1] !== undefined)));
        setHeader(cfg.headers, novoToken.headerName, novoToken.token);
        return await api.request<unknown, unknown, unknown, QueryParams>(cfg);
      }

      const maxRetries = obterMaximoTentativas(cfg);
      cfg.__retryCount = cfg.__retryCount || 0;
      if (!tinhaSenhaConfirmacao && cfg.__retryCount < maxRetries && podeTentarNovamente({ status, config: cfg, error: errorOriginal })) {
        cfg.__retryCount += 1;
        const continuar = await aguardarNovaTentativa({ attempt: cfg.__retryCount, config: cfg, headers: errorOriginal?.response?.headers });
        verificarSessaoRequisicao(cfg);
        if (!continuar) {
          return Promise.reject(normalizarErroHttp({ code: 'ERR_CANCELED', name: 'CanceledError', config: cfg }));
        }
        return await api.request<unknown, unknown, unknown, QueryParams>(cfg);
      }

      if (!errorOriginal?.response) {
        if (!estaDesconectado) {
          estaDesconectado = true;
          dispararConexaoPerdida();
          if (!cfg?.silentError) notificar('error', 'Sem conexão com o servidor. Verifique a sua rede.');
        }
        return Promise.reject(erro);
      }

      const backendCode = erro.code;
      const erroEncerraSessao = !cfg?.skipAuthLogout && ehErroQueEncerraSessao(erro);

      if (erroEncerraSessao) {
        invalidarTokenCsrf();
        dispararSaida((backendCode ? MENSAGENS_SESSAO[backendCode] : null) || erro.message, { code: backendCode, requestId: erro.requestId, geracaoSessao: cfg.__geracaoSessao });
      } else if (status >= 400 && !cfg?.silentError && status !== 428) {
        if (!(status === 429 && ehEndpointAutenticacao(cfg)) && !erro.senhaConfirmacaoInvalida) {
          notificar('error', erro.message);
        }
      }

      return Promise.reject(erro);
    } finally {
      cfg.__limparSinaisSessao?.();
    }
  },
);

/** Shared entry point. Domain services use normalized data, never AxiosResponse. */
export const requestBackEnd = requisitar;
