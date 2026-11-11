import type { HttpContext } from './types';
const limparNome = (nome: unknown) => {
  const semCaminho = String(nome || '').replace(/\\/g, '/').split('/').pop() || 'download';
  const limpo = semCaminho.replace(/[\u0000-\u001f\u007f<>:"|?*]/g, '_').trim();
  return (limpo || 'download').slice(0, 180);
};

export const extrairNomeArquivoContentDisposition = (contentDisposition: unknown, fallback = 'download') => {
  const valor = String(contentDisposition || '');
  const utf8 = valor.match(/filename\*=UTF-8''([^;]+)/i);
  if (utf8?.[1]) {
    try { return limparNome(decodeURIComponent(utf8[1].replace(/^"|"$/g, ''))); } catch {}
  }
  const simples = valor.match(/filename\s*=\s*(?:"([^"]+)"|([^;]+))/i);
  return limparNome(simples?.[1] || simples?.[2] || fallback);
};

export const obterDadosDownload = (contexto: HttpContext<Blob> | null | undefined, fallback = 'download') => {
  const headers = contexto?.headers || {};
  const disposition = headers['content-disposition'] || headers['Content-Disposition'];
  return {
    blob: contexto?.data ?? null,
    nomeArquivo: extrairNomeArquivoContentDisposition(disposition, fallback),
    contentType: headers['content-type'] || headers['Content-Type'] || '',
    requestId: contexto?.requestId || null,
  };
};
