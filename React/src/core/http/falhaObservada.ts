import { HTTP_ERROR_KINDS, type HttpError } from './types';
import type { ValidationField } from '../../contracts/shared';

export interface BatchFailureProgress {
  totalSalvo: number; total: number; lotesConcluidos: number; loteFalhou: number; totalLotes: number;
}
export type ObservedFailure = Partial<Pick<HttpError,
  'name' | 'message' | 'stack' | 'status' | 'code' | 'tipo' | 'requestId' | 'fields' | 'retryAfterMs'
  | 'resultadoIncerto' | 'podeReenviarAutomaticamente' | 'confirmacaoSenhaNecessaria'
  | 'senhaConfirmacaoInvalida' | 'isNetworkError' | 'canceled' | 'silencioso'>> & {
  data?: unknown;
  response?: { status?: number | null; data?: unknown };
  progressoFrequencia?: BatchFailureProgress;
};
const objeto = (valor: unknown): Record<string, unknown> | null =>
  valor !== null && typeof valor === 'object' ? valor as Record<string, unknown> : null;
const tipos = new Set<string>(HTTP_ERROR_KINDS);
const textos = ['name', 'message', 'stack'] as const;
const textosNulos = ['code', 'requestId'] as const;
const booleanos = ['resultadoIncerto', 'podeReenviarAutomaticamente', 'confirmacaoSenhaNecessaria',
  'senhaConfirmacaoInvalida', 'isNetworkError', 'canceled', 'silencioso'] as const;
const progressoValido = (valor: unknown): valor is BatchFailureProgress => {
  const dados = objeto(valor);
  return dados !== null && ['totalSalvo', 'total', 'lotesConcluidos', 'loteFalhou', 'totalLotes']
    .every((chave) => typeof dados[chave] === 'number');
};
const camposValidos = (valor: unknown): valor is ValidationField[] => Array.isArray(valor) && valor.every((campo: unknown) => {
  const dados = objeto(campo);
  return dados !== null && (dados.field === null || typeof dados.field === 'string') && typeof dados.message === 'string';
});
const respostaValida = (valor: unknown): valor is NonNullable<ObservedFailure['response']> => {
  const resposta = objeto(valor);
  return resposta !== null && (resposta.status == null || typeof resposta.status === 'number');
};
function ehFalhaObservada(valor: unknown): valor is ObservedFailure {
  const dados = objeto(valor);
  if (!dados) return false;
  return textos.every((chave) => dados[chave] === undefined || typeof dados[chave] === 'string')
    && textosNulos.every((chave) => dados[chave] == null || typeof dados[chave] === 'string')
    && booleanos.every((chave) => dados[chave] === undefined || typeof dados[chave] === 'boolean')
    && (dados.status === undefined || typeof dados.status === 'number')
    && (dados.retryAfterMs == null || typeof dados.retryAfterMs === 'number')
    && (dados.tipo === undefined || (typeof dados.tipo === 'string' && tipos.has(dados.tipo)))
    && (dados.fields === undefined || camposValidos(dados.fields))
    && (dados.response === undefined || respostaValida(dados.response))
    && (dados.progressoFrequencia === undefined || progressoValido(dados.progressoFrequencia));
}
/** A valid Error/HTTP failure keeps its identity. Unknown bodies remain unknown;
 * malformed optional fields never acquire a stronger type by assertion. */
export function observarFalha(causa: unknown): ObservedFailure {
  if (ehFalhaObservada(causa)) return causa;
  const dados = objeto(causa);
  if (!dados) return {};
  const falha: ObservedFailure = {};
  for (const chave of textos) if (typeof dados[chave] === 'string') falha[chave] = dados[chave];
  for (const chave of textosNulos) if (dados[chave] === null || typeof dados[chave] === 'string') falha[chave] = dados[chave];
  for (const chave of booleanos) if (typeof dados[chave] === 'boolean') falha[chave] = dados[chave];
  if (typeof dados.status === 'number') falha.status = dados.status;
  if (dados.retryAfterMs === null || typeof dados.retryAfterMs === 'number') falha.retryAfterMs = dados.retryAfterMs;
  if ('data' in dados) falha.data = dados.data;
  if (camposValidos(dados.fields)) falha.fields = dados.fields;
  if (respostaValida(dados.response)) falha.response = dados.response;
  if (progressoValido(dados.progressoFrequencia)) falha.progressoFrequencia = dados.progressoFrequencia;
  return falha;
}
