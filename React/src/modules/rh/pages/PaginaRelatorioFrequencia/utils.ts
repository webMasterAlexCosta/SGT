export const itensLegenda = [
  { sigla: "P", desc: "Presença Integral", bg: "FFD1FAE5", text: "FF065F46", rgbBg: [209, 250, 229], textPdf: [6, 95, 70], border: [110, 231, 183] },
  { sigla: "PP", desc: "Presença Parcial", bg: "FFFFEDD5", text: "FF9A3412", rgbBg: [255, 237, 213], textPdf: [154, 52, 18], border: [253, 186, 116] },
  { sigla: "F", desc: "Falta Injustificada", bg: "FFFEE2E2", text: "FF991B1B", rgbBg: [254, 226, 226], textPdf: [153, 27, 27], border: [252, 165, 165] },
  { sigla: "AT", desc: "Atestado Médico", bg: "FFDBEAFE", text: "FF1E40AF", rgbBg: [219, 234, 254], textPdf: [30, 64, 175], border: [147, 197, 253] },
  { sigla: "FER", desc: "Feriado", bg: "FFF3E8FF", text: "FF6B21A8", rgbBg: [243, 232, 255], textPdf: [107, 33, 168], border: [216, 180, 254] },
  { sigla: "DP", desc: "DSR Perdido", bg: "FFFFEDD5", text: "FFC2410C", rgbBg: [255, 237, 213], textPdf: [194, 65, 12], border: [251, 146, 60] },
  { sigla: "RT", desc: "Repouso Trabalhado", bg: "FFCCFBF1", text: "FF115E59", rgbBg: [204, 251, 241], textPdf: [17, 94, 89], border: [94, 234, 212] },
  { sigla: "D", desc: "Descanso (Folga/DSR)", bg: "FFF1F5F9", text: "FF475569", rgbBg: [241, 245, 249], textPdf: [71, 85, 105], border: [203, 213, 225] },
  { sigla: "SUSP", desc: "Suspensão", bg: "FFFFE4E6", text: "FF9F1239", rgbBg: [255, 228, 230], textPdf: [159, 18, 57], border: [253, 164, 175] },
  { sigla: "ADV", desc: "Advertência", bg: "FFFEF3C7", text: "FF92400E", rgbBg: [254, 243, 199], textPdf: [146, 64, 14], border: [252, 211, 77] },
  { sigla: "FR", desc: "Folga Remunerada", bg: "FFE0F2FE", text: "FF0369A1", rgbBg: [224, 242, 254], textPdf: [3, 105, 161], border: [125, 211, 252] }
];

export const adicionarDias = (dataStr: string, dias: number) => {
  const data = new Date(dataStr + "T12:00:00Z");
  data.setDate(data.getDate() + dias);
  return data.toISOString().split("T")[0];
};

export { obterSiglaStatus, contarSigla } from "../../../../services/rh/ServicoDominioRelatorioFrequencia.ts";

export const formatarDecimal = (valor: string | number | null | undefined) => Number(valor || 0).toFixed(2).replace(".", ",");