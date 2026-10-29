import { ehMutacaoHttp } from './metodosHttp.js';
import { mensagemEhTecnica, redigirTextoSensivel, sanitizarEstrutura } from './sanitizacao.js';

const STATUS_MESSAGES = {
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

const interpretarJson = (data) => {
  if (typeof data !== 'string') return data;
  const t = data.trim();
  if (!(t.startsWith('{') || t.startsWith('['))) return data;
  try { return JSON.parse(t); } catch { return data; }
};

const obterHeader = (headers, nome) => {
  if (!headers) return null;
  if (typeof headers.get === 'function') return headers.get(nome) || headers.get(nome.toLowerCase()) || null;
  return headers[nome] ?? headers[nome.toLowerCase()] ?? null;
};

export const calcularRetryAfterMs = (valor, agora = Date.now()) => {
  if (valor == null || valor === '') return null;
  const segundos = Number(valor);
  if (Number.isFinite(segundos) && segundos >= 0) return Math.max(0, Math.round(segundos * 1000));
  const instante = Date.parse(String(valor));
  if (!Number.isFinite(instante)) return null;
  return Math.max(0, instante - Number(agora));
};

const camposValidacao = (data) => {
  const fields = Array.isArray(data?.fields) ? data.fields : [];
  const errors = Array.isArray(data?.errors) ? data.errors : [];
  return [...fields, ...errors].map((item) => {
    if (typeof item === 'string') return { field: null, message: redigirTextoSensivel(item) };
    return {
      field: item?.field || item?.campo || null,
      message: redigirTextoSensivel(item?.message || item?.mensagem || 'Valor inválido.'),
    };
  });
};

const classificar = (status, code) => {
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

export const normalizarErroHttp = (error) => {
  if (error?.code === 'ERR_CANCELED' || error?.name === 'CanceledError' || error?.canceled) {
    const cancelado = new Error('Requisição cancelada.');
    Object.assign(cancelado, { status: 0, code: 'REQUEST_CANCELED', tipo: 'CANCELED', canceled: true, silencioso: true });
    return cancelado;
  }

  const response = error?.response;
  const status = Number(response?.status || 0);
  const dataBruta = interpretarJson(response?.data) || {};
  const code = dataBruta?.code || null;
  const mensagemBackend = dataBruta?.message || dataBruta?.mensagem || dataBruta?.detail || dataBruta?.title || '';
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

  const normalizado = new Error(mensagem);
  Object.assign(normalizado, {
    status,
    code,
    tipo: classificar(status, code),
    requestId,
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

export const ehErroCsrfIdentificado = (erro) => {
  const codigo = String(erro?.code || '').toUpperCase();
  return ['CSRF_INVALID', 'CSRF_MISSING', 'INVALID_CSRF_TOKEN', 'MISSING_CSRF_TOKEN'].includes(codigo);
};
