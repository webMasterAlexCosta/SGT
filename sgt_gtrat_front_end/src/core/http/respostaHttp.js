const contextosPorObjeto = new WeakMap();

const interpretarJson = (data) => {
  if (typeof data !== 'string') return data;
  const texto = data.trim();
  if (!(texto.startsWith('{') || texto.startsWith('['))) return data;
  try { return JSON.parse(texto); } catch { return data; }
};

const headersPlanos = (headers) => {
  if (!headers) return {};
  const bruto = typeof headers.toJSON === 'function' ? headers.toJSON() : headers;
  const saida = {};
  Object.entries(bruto || {}).forEach(([chave, valor]) => {
    saida[chave] = valor;
    saida[chave.toLowerCase()] = valor;
  });
  return saida;
};

export const registrarContextoResposta = (dados, contexto) => {
  if (dados && (typeof dados === 'object' || typeof dados === 'function')) {
    contextosPorObjeto.set(dados, contexto);
  }
  return dados;
};

export const obterContextoResposta = (dados) => {
  if (dados?.__contextoHttp && typeof dados.__contextoHttp === 'object') return dados.__contextoHttp;
  return dados && (typeof dados === 'object' || typeof dados === 'function')
    ? contextosPorObjeto.get(dados) || null
    : null;
};

export const normalizarRespostaAxios = (response) => {
  const headers = headersPlanos(response?.headers);
  const statusHttp = Number(response?.status || 0);
  const raw = interpretarJson(response?.data);
  const ehEnvelope = raw && typeof raw === 'object' && !Array.isArray(raw)
    && Object.prototype.hasOwnProperty.call(raw, 'status')
    && Object.prototype.hasOwnProperty.call(raw, 'data');

  const contexto = {
    status: ehEnvelope ? Number(raw.status || statusHttp) : statusHttp,
    statusHttp,
    message: ehEnvelope ? (raw.message ?? null) : null,
    path: ehEnvelope ? (raw.path ?? null) : null,
    requestId: (ehEnvelope ? raw.requestId : null) || headers['x-request-id'] || null,
    meta: ehEnvelope ? (raw.meta ?? null) : null,
    headers,
    data: statusHttp === 204 ? null : (ehEnvelope ? (raw.data ?? null) : raw),
    envelope: ehEnvelope ? raw : null,
    pendente: statusHttp === 202,
    semConteudo: statusHttp === 204,
  };

  registrarContextoResposta(contexto.data, contexto);
  return contexto;
};

export const extrairDadosResposta = (response) => normalizarRespostaAxios(response).data;
