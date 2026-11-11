import { exigirIdentificadorRota, type ResourceIdentifierInput } from '../http/identificadores';
import type { ExportacaoRelatorioStatusDTO } from '../../contracts/java';
import type { QueryParams } from '../http/types';
import type { RequestOptions } from '../http/types';
import { CONFIG } from "../config";
import { requisitar } from "../http";

const baseApi = String(CONFIG.API_URL || "/api").replace(/\/$/, "");

const solicitar = (url: string, params: QueryParams) => requisitar<ExportacaoRelatorioStatusDTO>({
  method: "POST",
  url,
  params,
  timeout: 30000,
  retries: 0,
  silentError: true,
});

const normalizarFormato = (formato: (string)) => String(formato || "XLSX").toUpperCase() === "PDF" ? "PDF" : "XLSX";

export const ServicoExportacaoRelatorio = {
  solicitarFrequencia: (params: QueryParams = {}, formato = "XLSX", totalEstimado = 0) => solicitar(
    "/relatorios/frequencia-consolidada/exportacao/solicitacao",
    { ...params, formato: normalizarFormato(formato), totalEstimado },
  ),

  solicitarAbsenteismo: (params: QueryParams = {}, formato = "XLSX", totalEstimado = 0) => solicitar(
    "/relatorios/absenteismo/exportacao/solicitacao",
    { ...params, formato: normalizarFormato(formato), totalEstimado },
  ),

  solicitarTerceirizados: (params: QueryParams = {}, formato = "XLSX", totalEstimado = 0) => solicitar(
    "/relatorios/terceirizados/exportacao/solicitacao",
    { ...params, formato: normalizarFormato(formato), totalEstimado },
  ),

  consultar: async (id: ResourceIdentifierInput, options: RequestOptions = {}) => {
    id = exigirIdentificadorRota(id);
    return requisitar<ExportacaoRelatorioStatusDTO>({
    method: "GET",
    url: `/relatorios/exportacoes/${encodeURIComponent(id)}`,
    timeout: 15000,
    retries: 1,
    silentError: true,
    ...options,
  }); },

  cancelar: (id: ResourceIdentifierInput, options: RequestOptions = {}) => requisitar<ExportacaoRelatorioStatusDTO>({
    method: 'POST',
    url: `/relatorios/exportacoes/${encodeURIComponent(exigirIdentificadorRota(id))}/cancelamento`,
    timeout: 15000,
    retries: 0,
    silentError: true,
    ...options,
  }),

  obterUrlDownload: (id: ResourceIdentifierInput) => `${baseApi}/relatorios/exportacoes/${encodeURIComponent(exigirIdentificadorRota(id))}/arquivo`,
};
