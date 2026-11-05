import { CONFIG } from '../config';
import { ehMetodoIdempotenteHttp, ehMutacaoHttp, normalizarMetodoHttp } from './metodosHttp.js';

const sleepCancelavel = (ms, signal) => new Promise((resolve) => {
  if (signal?.aborted) {
    resolve(false);
    return;
  }

  let timer = null;
  const concluir = (resultado) => {
    if (timer != null) clearTimeout(timer);
    signal?.removeEventListener?.('abort', aoAbortar);
    resolve(resultado);
  };
  const aoAbortar = () => concluir(false);

  timer = setTimeout(() => concluir(true), Math.max(0, ms));
  signal?.addEventListener?.('abort', aoAbortar, { once: true });
});
const defaultRetryStatuses = new Set([0, 429, 502, 503, 504]);

const possuiSenhaConfirmacao = (headers) => {
  if (!headers) return false;
  if (typeof headers.has === 'function') {
    return headers.has('X-User-Password') || headers.has('x-user-password');
  }
  return Object.keys(headers).some((chave) => chave.toLowerCase() === 'x-user-password');
};

export const obterMaximoTentativas = (config) => {
  if (typeof config?.retries === 'number') return Math.max(0, Math.floor(config.retries));
  return normalizarMetodoHttp(config?.method) === 'GET' ? 2 : 0;
};

export const mutacaoPodeSerRepetida = (config) => {
  const metodo = normalizarMetodoHttp(config?.method);
  if (!ehMutacaoHttp(metodo)) return ehMetodoIdempotenteHttp(metodo);
  if (possuiSenhaConfirmacao(config?.headers)) return false;
  return Boolean(config?.mutacaoIdempotente || config?.retryNonIdempotent);
};

export const podeTentarNovamente = ({ status, config, error }) => {
  const metodo = normalizarMetodoHttp(config?.method);
  const metodoSeguro = ehMetodoIdempotenteHttp(metodo) || mutacaoPodeSerRepetida(config);
  if (!metodoSeguro) return false;
  if (error?.code === 'ERR_CANCELED' || error?.name === 'CanceledError') return false;

  const statuses = Array.isArray(config?.retryOnStatuses)
    ? new Set(config.retryOnStatuses.map((s) => Number(s)))
    : defaultRetryStatuses;

  return statuses.has(Number(status));
};

export const calcularEsperaNovaTentativaMs = ({ attempt, config, retryAfterHeader }) => {
  const ra = Number(retryAfterHeader);
  if (Number.isFinite(ra) && ra > 0) return Math.min(ra * 1000, 10_000);

  const base = typeof config?.retryDelayMs === 'number' ? config.retryDelayMs : CONFIG.HTTP_RETRY_BASE_MS;
  const cap = CONFIG.HTTP_RETRY_MAX_MS;
  const exp = Math.min(cap, base * Math.pow(2, Math.max(0, attempt - 1)));
  const jitter = Math.random() * 120;
  return Math.floor(exp + jitter);
};

export const aguardarNovaTentativa = async ({ attempt, config, headers }) => {
  const retryAfter = headers?.['retry-after'] || headers?.['Retry-After'] || headers?.get?.('retry-after');
  const delay = calcularEsperaNovaTentativaMs({ attempt, config, retryAfterHeader: retryAfter });
  return sleepCancelavel(delay, config?.signal);
};
