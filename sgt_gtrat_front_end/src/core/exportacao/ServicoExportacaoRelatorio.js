import { CONFIG } from "../config";
import { requisitar } from "../http";

const baseApi = String(CONFIG.API_URL || "/api").replace(/\/$/, "");

const solicitar = (url, params) => requisitar({
  method: "POST",
  url,
  params,
  timeout: 30000,
  retries: 0,
  silentError: true,
});

const normalizarFormato = (formato) => String(formato || "XLSX").toUpperCase() === "PDF" ? "PDF" : "XLSX";

export const ServicoExportacaoRelatorio = {
  solicitarFrequencia: (params = {}, formato = "XLSX", totalEstimado = 0) => solicitar(
    "/relatorios/frequencia-consolidada/exportacao/solicitacao",
    { ...params, formato: normalizarFormato(formato), totalEstimado },
  ),

  solicitarAbsenteismo: (params = {}, formato = "XLSX", totalEstimado = 0) => solicitar(
    "/relatorios/absenteismo/exportacao/solicitacao",
    { ...params, formato: normalizarFormato(formato), totalEstimado },
  ),

  solicitarTerceirizados: (params = {}, formato = "XLSX", totalEstimado = 0) => solicitar(
    "/relatorios/terceirizados/exportacao/solicitacao",
    { ...params, formato: normalizarFormato(formato), totalEstimado },
  ),

  consultar: (id) => requisitar({
    method: "GET",
    url: `/relatorios/exportacoes/${encodeURIComponent(id)}`,
    timeout: 15000,
    retries: 1,
    silentError: true,
  }),

  obterUrlDownload: (id) => `${baseApi}/relatorios/exportacoes/${encodeURIComponent(id)}/arquivo`,
};
