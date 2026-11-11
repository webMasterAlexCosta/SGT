const METODOS_PERMITIDOS = new Set(['GET', 'HEAD', 'OPTIONS', 'POST', 'PUT', 'PATCH', 'DELETE']);
const METODOS_MUTACAO = new Set(['POST', 'PUT', 'PATCH', 'DELETE']);
const METODOS_IDEMPOTENTES_HTTP = new Set(['GET', 'HEAD', 'OPTIONS']);

export const normalizarMetodoHttp = (metodo: unknown = 'GET') => {
  const normalizado = String(metodo || 'GET').trim().toUpperCase();
  return METODOS_PERMITIDOS.has(normalizado) ? normalizado : 'GET';
};

export const ehMutacaoHttp = (metodo: unknown) => METODOS_MUTACAO.has(normalizarMetodoHttp(metodo));
export const ehMetodoIdempotenteHttp = (metodo: unknown) => METODOS_IDEMPOTENTES_HTTP.has(normalizarMetodoHttp(metodo));
