const ehObjetoSimples = (v: unknown): v is Record<string, unknown> => {
  if (!v) return false;
  if (typeof v !== 'object') return false;
  return Object.prototype.toString.call(v) === '[object Object]';
};

const append = (sp: URLSearchParams, key: string, value: unknown): void => {
  if (value == null) return;
  if (value instanceof Date) {
    sp.append(key, value.toISOString());
    return;
  }
  if (Array.isArray(value)) {
    value.forEach((item: unknown) => append(sp, key, item));
    return;
  }
  if (ehObjetoSimples(value)) {
    Object.entries(value).forEach(([k, v]) => append(sp, `${key}.${k}`, v));
    return;
  }
  sp.append(key, String(value));
};

export const serializarParametros = (params?: Record<string, unknown>): string => {
  if (!params) return '';
  const sp = new URLSearchParams();
  Object.entries(params).forEach(([key, value]) => append(sp, key, value));
  return sp.toString();
};
