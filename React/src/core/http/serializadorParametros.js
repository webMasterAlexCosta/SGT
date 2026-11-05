const ehObjetoSimples = (v) => {
  if (!v) return false;
  if (typeof v !== 'object') return false;
  return Object.prototype.toString.call(v) === '[object Object]';
};

const append = (sp, key, value) => {
  if (value == null) return;
  if (value instanceof Date) {
    sp.append(key, value.toISOString());
    return;
  }
  if (Array.isArray(value)) {
    value.forEach((item) => append(sp, key, item));
    return;
  }
  if (ehObjetoSimples(value)) {
    Object.entries(value).forEach(([k, v]) => append(sp, `${key}.${k}`, v));
    return;
  }
  sp.append(key, String(value));
};

export const serializarParametros = (params) => {
  if (!params) return '';
  const sp = new URLSearchParams();
  Object.entries(params).forEach(([key, value]) => append(sp, key, value));
  return sp.toString();
};
