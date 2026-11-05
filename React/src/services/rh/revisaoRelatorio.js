export const normalizarAvisoRevisaoRelatorio = (revisao) => ({
  sujeitoARevisao: Boolean(revisao?.sujeitoARevisao),
  quantidadePendencias: Number(revisao?.quantidadePendencias || 0),
  mensagem: String(revisao?.mensagem || "").trim(),
});
