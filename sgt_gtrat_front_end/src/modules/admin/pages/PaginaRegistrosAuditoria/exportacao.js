import { CONFIG } from "../../../../core/config";
import { baixarArquivoXlsx, celulaXlsx, colunaXlsx } from "../../../../core/export/xlsx";
import { formatarDataHora } from "./utils";

const preparar = (dados) => dados.map((linha) => ({
  "Data/Hora": formatarDataHora(linha.createdAt),
  Ação: linha.action,
  Usuário: linha.username,
  Entidade: linha.entityType,
  "ID Entidade": linha.entityId || "-",
  "Request ID": linha.requestId || "-",
  "Rótulo da entidade": linha.entityLabel || "-",
}));

const escaparCsv = (valor) => {
  const texto = valor == null ? "" : String(valor);
  return /[\n\r",]/.test(texto) ? `"${texto.replace(/"/g, '""')}"` : texto;
};

export const exportarParaCSV = async (dados) => {
  if (!dados?.length) return;
  const linhas = preparar(dados);
  const cabecalhos = Object.keys(linhas[0]);
  const csv = "\uFEFF" + [cabecalhos.map(escaparCsv).join(","), ...linhas.map((linha) => cabecalhos.map((chave) => escaparCsv(linha[chave])).join(","))].join("\n");
  const url = URL.createObjectURL(new Blob([csv], { type: "text/csv;charset=utf-8;" }));
  const link = document.createElement("a");
  link.href = url;
  link.download = `logs_auditoria_${Date.now()}.csv`;
  link.click();
  URL.revokeObjectURL(url);
};

export const exportarParaExcel = async (dados) => {
  if (!dados?.length) return;
  const linhas = preparar(dados);
  const cabecalhos = Object.keys(linhas[0]);
  const colunas = cabecalhos.map((chave) => ({ width: Math.min(60, Math.max(12, chave.length + 2, ...linhas.map((linha) => String(linha[chave] ?? "").length + 2))) }));
  const cabecalho = { font: { bold: true, color: "FFFFFFFF" }, fillColor: "FF00416B", alignment: { vertical: "center" }, border: true };
  baixarArquivoXlsx({
    creator: CONFIG.APP_NAME,
    sheets: [{
      name: "Logs de Auditoria",
      columns: colunas,
      rows: [{ cells: cabecalhos.map((item) => celulaXlsx(item, cabecalho)), height: 22 }, ...linhas.map((linha) => ({ cells: cabecalhos.map((chave) => linha[chave] ?? "") }))],
      autoFilter: `A1:${colunaXlsx(cabecalhos.length)}1`,
      freezeRows: 1,
      defaultRowHeight: 18,
    }],
  }, `logs_auditoria_${Date.now()}.xlsx`);
};

export const exportarParaPDF = async (dados) => {
  if (!dados?.length) return;
  const [{ default: jsPDF }, { default: autoTable }] = await Promise.all([import("jspdf"), import("jspdf-autotable")]);
  const doc = new jsPDF();
  doc.setFontSize(16);
  doc.text("Relatório de Logs de Auditoria", 14, 15);
  doc.setFontSize(10);
  doc.text(`Gerado em: ${new Date().toLocaleString("pt-BR")}`, 14, 22);
  autoTable(doc, {
    head: [["Data/Hora", "Ação", "Usuário", "Entidade", "ID Ref."]],
    body: dados.map((linha) => [formatarDataHora(linha.createdAt), linha.action, linha.username, linha.entityType, linha.entityId || "-"]),
    startY: 28,
    styles: { fontSize: 8 },
    headStyles: { fillColor: [0, 65, 107] },
  });
  doc.save(`logs_auditoria_${Date.now()}.pdf`);
};
