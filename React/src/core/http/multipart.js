const ehFormData = (valor) => typeof FormData !== 'undefined' && valor instanceof FormData;

export const removerContentTypeMultipart = (headers, data) => {
  if (!ehFormData(data) || !headers) return headers;
  if (typeof headers.delete === 'function') {
    headers.delete('Content-Type');
    headers.delete('content-type');
    return headers;
  }
  for (const chave of Object.keys(headers)) {
    if (chave.toLowerCase() === 'content-type') delete headers[chave];
  }
  return headers;
};

export const criarFormData = (partes = {}) => {
  if (typeof FormData === 'undefined') throw new Error('FormData indisponível neste ambiente.');
  const form = new FormData();
  Object.entries(partes).forEach(([nome, valor]) => {
    if (valor == null) return;
    if (Array.isArray(valor)) valor.forEach((item) => form.append(nome, item));
    else form.append(nome, valor);
  });
  return form;
};
