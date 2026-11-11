import type { AxiosRequestConfig, AxiosResponse, AxiosResponseHeaders, RawAxiosResponseHeaders, AxiosHeaders, RawAxiosRequestHeaders } from 'axios';
import type { ApiResponse, PageMeta, ValidationField } from '../../contracts/shared';

export type HeaderCollection = AxiosHeaders | RawAxiosRequestHeaders | undefined;
export type QueryParams = Record<string, unknown>;
export type IdempotencyOption = boolean | string | { chave?: string; campo?: string; prefixo?: string };
export interface PreparedIdempotency { chave: string; campo: string; }
export interface PasswordConfirmationDescription {
  titulo?: string; mensagem?: string; acao?: string; entidade?: string; consequencia?: string;
}
/** Options shared by domain services. Authentication remains cookie based. */
export interface RequestOptions<D = unknown> extends Omit<AxiosRequestConfig<D>, 'params' | 'signal' | 'headers'> {
  headers?: HeaderCollection; params?: QueryParams; signal?: AbortSignal; geracaoSessao?: number;
  senhaConfirmacao?: string; retries?: number; retryNonIdempotent?: boolean;
  mutacaoIdempotente?: boolean; retryOnStatuses?: number[]; retryDelayMs?: number;
  successMessage?: string | null; skipLoading?: boolean; skipAuthLogout?: boolean; silentError?: boolean;
  retornarEnvelopeHttp?: boolean; confirmacaoSenha?: PasswordConfirmationDescription;
  csrfRetrySeguro?: boolean; idempotencia?: IdempotencyOption;
}
export interface InternalRequestOptions<D = unknown> extends RequestOptions<D> {
  __geracaoSessao?: number; __sinalSessao?: AbortSignal; __limparSinaisSessao?: () => void;
  __csrfBootstrap?: boolean; __csrfHeaderName?: string; __csrfRenovado?: boolean;
  __confirmacaoSenhaReplay?: boolean; __retryCount?: number; __idempotencia?: PreparedIdempotency;
}
export interface HttpContext<T> {
  status: number; statusHttp: number; message: string | null; path: string | null;
  requestId: string | null; meta: PageMeta | null; headers: Record<string, unknown>;
  data: T | null; envelope: ApiResponse<T> | null; pendente: boolean; semConteudo: boolean;
}
export const HTTP_ERROR_KINDS = ['NETWORK', 'VALIDATION', 'AUTHENTICATION', 'AUTHORIZATION', 'NOT_FOUND',
  'CONFLICT', 'PAYLOAD_TOO_LARGE', 'UNSUPPORTED_MEDIA', 'PASSWORD_CONFIRMATION_REQUIRED',
  'PASSWORD_CONFIRMATION_INVALID', 'RATE_LIMIT', 'SERVER', 'HTTP', 'CANCELED'] as const;
export type HttpErrorKind = typeof HTTP_ERROR_KINDS[number];
export interface HttpError extends Error {
  status: number; code: string | null; tipo: HttpErrorKind; requestId?: string | null;
  fields?: ValidationField[]; data?: unknown; retryAfterMs?: number | null;
  resultadoIncerto?: boolean; podeReenviarAutomaticamente?: boolean;
  confirmacaoSenhaNecessaria?: boolean; senhaConfirmacaoInvalida?: boolean;
  isNetworkError?: boolean; canceled?: boolean; silencioso?: boolean;
}
export interface TransportError {
  code?: string; name?: string; canceled?: boolean; config?: InternalRequestOptions;
  response?: Pick<AxiosResponse<unknown>, 'status' | 'data'> & { headers?: RawAxiosResponseHeaders | AxiosResponseHeaders };
}
export interface PasswordConfirmationContext<T = unknown> extends PasswordConfirmationDescription {
  requestId?: string | null; signal?: AbortSignal; executar: (senha: string) => Promise<T>;
}
