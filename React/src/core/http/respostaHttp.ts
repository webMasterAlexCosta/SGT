import type { AxiosResponse } from 'axios';
import type { ApiResponse, PageMeta } from '../../contracts/shared';
import type { HttpContext } from './types';

const contextosPorObjeto = new WeakMap<object, HttpContext<unknown>>();
const objeto = (valor: unknown): valor is Record<string, unknown> => valor !== null && typeof valor === 'object';
const interpretarJson = (data: unknown): unknown => {
  if (typeof data !== 'string') return data;
  const texto = data.trim();
  if (!(texto.startsWith('{') || texto.startsWith('['))) return data;
  try { return JSON.parse(texto) as unknown; } catch { return data; }
};
const headersPlanos = (headers: unknown): Record<string, unknown> => {
  if (!objeto(headers)) return {};
  const bruto: unknown = typeof headers.toJSON === 'function' ? headers.toJSON() : headers;
  const saida: Record<string, unknown> = {};
  Object.entries(objeto(bruto) ? bruto : {}).forEach(([chave, valor]) => {
    saida[chave] = valor;
    saida[chave.toLowerCase()] = valor;
  });
  return saida;
};
export const registrarContextoResposta = <T,>(dados: T, contexto: HttpContext<T>): T => {
  if (dados && (typeof dados === 'object' || typeof dados === 'function')) contextosPorObjeto.set(dados, contexto);
  return dados;
};
export const obterContextoResposta = <T,>(dados: T): HttpContext<T> | null => {
  if (objeto(dados) && objeto(dados.__contextoHttp)) return dados.__contextoHttp as unknown as HttpContext<T>;
  return dados && (typeof dados === 'object' || typeof dados === 'function')
    ? (contextosPorObjeto.get(dados) as HttpContext<T> | undefined) || null : null;
};
/** The only deserialization boundary: T is the endpoint DTO chosen by the service. */
export const normalizarRespostaAxios = <T,>(response: Pick<AxiosResponse<unknown>, 'headers' | 'status' | 'data'>): HttpContext<T> => {
  const headers = headersPlanos(response?.headers);
  const statusHttp = Number(response?.status || 0);
  const raw = interpretarJson(response?.data);
  const envelope = objeto(raw) && !Array.isArray(raw) && Object.hasOwn(raw, 'status') && Object.hasOwn(raw, 'data')
    ? raw as unknown as ApiResponse<T> : null;
  const requestId = envelope?.requestId || headers['x-request-id'];
  const contexto: HttpContext<T> = {
    status: envelope ? Number(envelope.status || statusHttp) : statusHttp,
    statusHttp, message: envelope?.message ?? null, path: envelope?.path ?? null,
    requestId: requestId == null ? null : String(requestId),
    meta: envelope?.meta as PageMeta | null ?? null, headers,
    data: statusHttp === 204 ? null : envelope ? envelope.data ?? null : raw as T,
    envelope, pendente: statusHttp === 202, semConteudo: statusHttp === 204,
  };
  registrarContextoResposta(contexto.data, contexto);
  return contexto;
};
export const extrairDadosResposta = <T,>(response: Pick<AxiosResponse<unknown>, 'headers' | 'status' | 'data'>): T | null => normalizarRespostaAxios<T>(response).data;
