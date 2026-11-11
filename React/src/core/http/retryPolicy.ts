import { AxiosHeaders } from 'axios';
import type { RequestOptions, HeaderCollection, TransportError } from './types';
import { CONFIG } from '../config';
import { ehMetodoIdempotenteHttp, ehMutacaoHttp, normalizarMetodoHttp } from './metodosHttp.ts';

const sleepCancelavel = (ms: number, signal?: AbortSignal) => new Promise<boolean>((resolve) => {
  if (signal?.aborted) {
    resolve(false);
    return;
  }

  let timer: ReturnType<typeof setTimeout> | null = null;
  const concluir = (resultado: boolean) => {
    if (timer != null) clearTimeout(timer);
    signal?.removeEventListener?.('abort', aoAbortar);
    resolve(resultado);
  };
  const aoAbortar = () => concluir(false);

  timer = setTimeout(() => concluir(true), Math.max(0, ms));
  signal?.addEventListener?.('abort', aoAbortar, { once: true });
});
const defaultRetryStatuses = new Set([0, 429, 502, 503, 504]);

const lerCabecalho = (headers: HeaderCollection, nome: string) => {
  if (!headers) return undefined;
  if (headers instanceof AxiosHeaders) return headers.get(nome);
  const chave = Object.keys(headers).find((item) => item.toLowerCase() === nome.toLowerCase());
  return chave ? headers[chave] : undefined;
};
const possuiSenhaConfirmacao = (headers?: HeaderCollection): boolean => Boolean(lerCabecalho(headers, 'X-User-Password')); 

export const obterMaximoTentativas = (config?: RequestOptions) => {
  if (typeof config?.retries === 'number') return Math.max(0, Math.floor(config.retries));
  return normalizarMetodoHttp(config?.method) === 'GET' ? 2 : 0;
};

export const mutacaoPodeSerRepetida = (config?: RequestOptions) => {
  const metodo = normalizarMetodoHttp(config?.method);
  if (!ehMutacaoHttp(metodo)) return ehMetodoIdempotenteHttp(metodo);
  if (possuiSenhaConfirmacao(config?.headers)) return false;
  return Boolean(config?.mutacaoIdempotente || config?.retryNonIdempotent);
};

export const podeTentarNovamente = ({ status, config, error }: { status: unknown; config?: RequestOptions; error?: TransportError }) => {
  const metodo = normalizarMetodoHttp(config?.method);
  const metodoSeguro = ehMetodoIdempotenteHttp(metodo) || mutacaoPodeSerRepetida(config);
  if (!metodoSeguro) return false;
  if (error?.code === 'ERR_CANCELED' || error?.name === 'CanceledError') return false;

  const statuses = Array.isArray(config?.retryOnStatuses)
    ? new Set(config.retryOnStatuses.map((s) => Number(s)))
    : defaultRetryStatuses;

  return statuses.has(Number(status));
};

export const calcularEsperaNovaTentativaMs = ({ attempt, config, retryAfterHeader }: { attempt: number; config?: RequestOptions; retryAfterHeader?: unknown }) => {
  const ra = Number(retryAfterHeader);
  if (Number.isFinite(ra) && ra > 0) return Math.min(ra * 1000, 10_000);

  const base = typeof config?.retryDelayMs === 'number' ? config.retryDelayMs : CONFIG.HTTP_RETRY_BASE_MS;
  const cap = CONFIG.HTTP_RETRY_MAX_MS;
  const exp = Math.min(cap, base * Math.pow(2, Math.max(0, attempt - 1)));
  const jitter = Math.random() * 120;
  return Math.floor(exp + jitter);
};

export const aguardarNovaTentativa = async ({ attempt, config, headers }: { attempt: number; config?: RequestOptions; headers?: HeaderCollection }) => {
  const retryAfter = lerCabecalho(headers, 'retry-after');
  const delay = calcularEsperaNovaTentativaMs({ attempt, config, retryAfterHeader: retryAfter });
  return sleepCancelavel(delay, config?.signal);
};
