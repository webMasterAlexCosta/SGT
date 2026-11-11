export const normalizarAvisoRevisaoRelatorio = (revisao: (import("../../contracts/java").AvisoRevisaoRelatorioDTO | null) | ({ sujeitoARevisao: boolean; quantidadePendencias: number; mensagem: string; })) => ({
  sujeitoARevisao: Boolean(revisao?.sujeitoARevisao),
  quantidadePendencias: Number(revisao?.quantidadePendencias || 0),
  mensagem: String(revisao?.mensagem || "").trim(),
});
