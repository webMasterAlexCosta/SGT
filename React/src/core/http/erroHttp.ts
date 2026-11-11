import type { HttpError, TransportError, HttpErrorKind } from './types';
import { ehMutacaoHttp } from './metodosHttp.ts';
import { mensagemEhTecnica, redigirTextoSensivel, sanitizarEstrutura } from './sanitizacao.ts';

const STATUS_MESSAGES: Record<number, string> = {
  400: 'Dados inválidos. Verifique as informações preenchidas.',
  401: 'Sessão inválida ou expirada. Faça login novamente.',
  403: 'Acesso negado para esta ação.',
  404: 'O recurso solicitado não foi encontrado.',
  409: 'A operação conflita com o estado atual dos dados.',
  413: 'O arquivo ou conteúdo enviado excede o limite permitido.',
  415: 'O formato enviado não é suportado.',
  422: 'Não foi possível validar os dados enviados.',
  428: 'Confirmação de senha necessária para concluir a operação.',
  429: 'Muitas requisições. Aguarde antes de tentar novamente.',
  500: 'O servidor não conseguiu concluir a operação.',
  502: 'Falha temporária de comunicação com o servidor.',
  503: 'Serviço temporariamente indisponível.',
  504: 'O servidor excedeu o tempo de resposta.',
};

const interpretarJson = (data: unknown): unknown => {
  if (typeof data !== 'string') return data;
  const t = data.trim();
  if (!(t.startsWith('{') || t.startsWith('['))) return data;
  try { return JSON.parse(t); } catch { return data; }
};

// responseType=blob também se aplica às respostas de erro do download.
// Decodificar antes de avaliar sessão, CSRF e confirmação mantém o contrato Java.
export const decodificarCorpoErroHttp = async (error: TransportError) => {
  const response = error?.response;
  const data = response?.data;
  if (!response || data == null) return;
  try {
    if (typeof Blob !== 'undefined' && data instanceof Blob && data.size <= 1_048_576) {
      const texto = typeof data.text === 'function' ? await data.text() : await new Promise((resolve, reject) => {
        const leitor = new FileReader();
        leitor.onload = () => resolve(String(leitor.result || ''));
        leitor.onerror = () => reject(leitor.error);
        leitor.readAsText(data);
      });
      const json = interpretarJson(texto);
      if (json && typeof json === 'object') response.data = json;
    } else if (data instanceof ArrayBuffer && data.byteLength <= 1_048_576) {
      const json = interpretarJson(new TextDecoder().decode(data));
      if (json && typeof json === 'object') response.data = json;
    }
  } catch {
    // Corpo binário/ilegível conserva o status HTTP e a mensagem segura padrão.
  }
};

const obterHeader = (headers: unknown, nome: string): unknown => {
  if (typeof headers !== 'object' || headers === null) return null;
  const valores = headers as Record<string, unknown>;
  if (!headers) return null;
  if (typeof valores.get === 'function') return valores.get(nome) || valores.get(nome.toLowerCase()) || null;
  return valores[nome] ?? valores[nome.toLowerCase()] ?? null;
};

export const calcularRetryAfterMs = (valor: unknown, agora = Date.now()) => {
  if (valor == null || valor === '') return null;
  const segundos = Number(valor);
  if (Number.isFinite(segundos) && segundos >= 0) return Math.max(0, Math.round(segundos * 1000));
  const instante = Date.parse(String(valor));
  if (!Number.isFinite(instante)) return null;
  return Math.max(0, instante - Number(agora));
};

const camposValidacao = (data: Record<string, unknown>) => {
  const fields: unknown[] = Array.isArray(data?.fields) ? data.fields : [];
  const errors: unknown[] = Array.isArray(data?.errors) ? data.errors : [];
  return [...fields, ...errors].map((item) => {
    if (typeof item === 'string') return { field: null, message: redigirTextoSensivel(item) };
    const campo = item !== null && typeof item === 'object' ? item as Record<string, unknown> : {};
    const nome = campo.field || campo.campo;
    return {
      field: typeof nome === 'string' ? nome : null,
      message: redigirTextoSensivel(campo.message || campo.mensagem || 'Valor inválido.'),
    };
  });
};

const classificar = (status: number, code: string | null): HttpErrorKind => {
  if (status === 0) return 'NETWORK';
  if (status === 400 || status === 422) return 'VALIDATION';
  if (status === 401) return 'AUTHENTICATION';
  if (status === 403 && code === 'INVALID_CONFIRMATION_PASSWORD') return 'PASSWORD_CONFIRMATION_INVALID';
  if (status === 403) return 'AUTHORIZATION';
  if (status === 404) return 'NOT_FOUND';
  if (status === 409) return 'CONFLICT';
  if (status === 413) return 'PAYLOAD_TOO_LARGE';
  if (status === 415) return 'UNSUPPORTED_MEDIA';
  if (status === 428 && code === 'PASSWORD_CONFIRMATION_REQUIRED') return 'PASSWORD_CONFIRMATION_REQUIRED';
  if (status === 429) return 'RATE_LIMIT';
  if (status >= 500) return 'SERVER';
  return 'HTTP';
};

export const normalizarErroHttp = (error: TransportError): HttpError => {
  if (error?.code === 'ERR_CANCELED' || error?.name === 'CanceledError' || error?.canceled) {
    const cancelado = Object.assign(new Error('Requisição cancelada.'), { status: 0, code: 'REQUEST_CANCELED', tipo: 'CANCELED' as const, canceled: true, silencioso: true });
    return cancelado;
  }

  const response = error?.response;
  const status = Number(response?.status || 0);
  const interpretado = interpretarJson(response?.data);
  const dataBruta = interpretado && typeof interpretado === 'object' ? interpretado as Record<string, unknown> : {};
  const code = typeof dataBruta.code === 'string' ? dataBruta.code : null;
  const mensagemBackend = String(dataBruta.message || dataBruta.mensagem || dataBruta.detail || dataBruta.title || '');
  const mensagem = mensagemBackend && !mensagemEhTecnica(mensagemBackend)
    ? redigirTextoSensivel(mensagemBackend)
    : (STATUS_MESSAGES[status] || (status ? `Falha na comunicação (HTTP ${status}).` : 'Falha de conexão com o servidor.'));
  const metodo = String(error?.config?.method || 'GET').toUpperCase();
  const requestId = dataBruta?.requestId
    || obterHeader(response?.headers, 'X-Request-Id')
    || obterHeader(error?.config?.headers, 'X-Request-Id')
    || null;
  const mutacao = ehMutacaoHttp(metodo);
  const resultadoIncerto = mutacao && (status === 0 || status >= 500);

  const normalizado = Object.assign(new Error(mensagem), {
    status,
    code,
    tipo: classificar(status, code),
    requestId: requestId == null ? null : String(requestId),
    fields: camposValidacao(dataBruta),
    data: sanitizarEstrutura(dataBruta),
    retryAfterMs: calcularRetryAfterMs(obterHeader(response?.headers, 'Retry-After')),
    resultadoIncerto,
    podeReenviarAutomaticamente: !mutacao,
    confirmacaoSenhaNecessaria: status === 428 && code === 'PASSWORD_CONFIRMATION_REQUIRED',
    senhaConfirmacaoInvalida: status === 403 && code === 'INVALID_CONFIRMATION_PASSWORD',
    isNetworkError: status === 0,
  });
  return normalizado;
};

export const ehErroCsrfIdentificado = (erro: Pick<HttpError, 'code'>) => {
  const codigo = String(erro?.code || '').toUpperCase();
  return ['CSRF_INVALID', 'CSRF_MISSING', 'INVALID_CSRF_TOKEN', 'MISSING_CSRF_TOKEN'].includes(codigo);
};
