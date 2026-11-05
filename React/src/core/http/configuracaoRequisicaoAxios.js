import { aplicarIdempotenciaAoCorpo, prepararIdempotencia } from './idempotencia.js';
import { normalizarMetodoHttp } from './metodosHttp.js';

export const criarConfiguracaoRequisicaoAxios = ({
  method = 'GET',
  url,
  params,
  data,
  headers,
  senhaConfirmacao,
  responseType,
  timeout,
  signal,
  retries,
  retryNonIdempotent,
  mutacaoIdempotente,
  retryOnStatuses,
  retryDelayMs,
  successMessage,
  skipAuthLogout,
  silentError,
  retornarEnvelopeHttp,
  confirmacaoSenha,
  csrfRetrySeguro,
  idempotencia,
  onUploadProgress,
  onDownloadProgress,
} = {}) => {
  if (!url || typeof url !== 'string') {
    throw new Error('criarConfiguracaoRequisicaoAxios: "url" é obrigatório e deve ser uma string.');
  }

  const metodo = normalizarMetodoHttp(method);
  const idempotenciaPreparada = prepararIdempotencia(idempotencia);
  const corpo = aplicarIdempotenciaAoCorpo(data, idempotenciaPreparada);

  const cfg = {
    method: metodo,
    url,
  };

  if (params != null) cfg.params = params;
  if (corpo != null) cfg.data = corpo;

  const cabecalhos = { ...(headers || {}) };
  if (senhaConfirmacao != null && String(senhaConfirmacao).length > 0) {
    cabecalhos['X-User-Password'] = String(senhaConfirmacao);
  }
  if (Object.keys(cabecalhos).length > 0) cfg.headers = cabecalhos;

  if (responseType != null) cfg.responseType = responseType;
  if (timeout != null) cfg.timeout = timeout;
  if (signal != null) cfg.signal = signal;
  if (typeof onUploadProgress === 'function') cfg.onUploadProgress = onUploadProgress;
  if (typeof onDownloadProgress === 'function') cfg.onDownloadProgress = onDownloadProgress;
  if (successMessage) cfg.successMessage = successMessage;
  if (skipAuthLogout != null) cfg.skipAuthLogout = !!skipAuthLogout;
  if (senhaConfirmacao != null && silentError == null) cfg.silentError = true;
  else if (silentError != null) cfg.silentError = !!silentError;

  if (retries != null) cfg.retries = retries;
  if (retryNonIdempotent != null) cfg.retryNonIdempotent = !!retryNonIdempotent;
  if (mutacaoIdempotente != null) cfg.mutacaoIdempotente = !!mutacaoIdempotente;
  if (idempotenciaPreparada) {
    cfg.mutacaoIdempotente = true;
    cfg.__idempotencia = idempotenciaPreparada;
  }
  if (retryOnStatuses != null) cfg.retryOnStatuses = retryOnStatuses;
  if (retryDelayMs != null) cfg.retryDelayMs = retryDelayMs;

  if (retornarEnvelopeHttp != null) cfg.retornarEnvelopeHttp = !!retornarEnvelopeHttp;
  if (confirmacaoSenha != null) cfg.confirmacaoSenha = { ...confirmacaoSenha };
  if (csrfRetrySeguro != null) cfg.csrfRetrySeguro = !!csrfRetrySeguro;

  return cfg;
};
