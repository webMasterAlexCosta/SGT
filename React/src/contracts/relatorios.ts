import type * as DTO from './java';
/** RelatorioAdministrativoServico.gerarConteudo selects exactly these two snapshots. */
export type AdministrativeContent = DTO.RelatorioFrequenciaConsolidadoDTO | DTO.RelatorioAbsenteismoDTO[];
export type AdministrativeReport = Omit<DTO.RelatorioAdministrativoDTO, 'conteudo'> & { conteudo: AdministrativeContent | null };
export type AdministrativeRequest = Omit<import('./java-requests').RequestEmissaoRelatorioRequisicao, 'lotacaoId' | 'jornadaId'> & { lotacaoId?: string | number | null; jornadaId?: string | number | null };
export type ContractorReportView = Omit<DTO.RelatorioTerceirizadosDTO, 'empregados'> & { empregados: (DTO.RelatorioTerceirizadosDTOPaginaEmpregados & { numberOfElements?: number; empty?: boolean; pageable?: { pageNumber?: number; pageSize?: number; offset?: number } }) | null };
/** Retained presentation shape for the existing legacy loading indicator. */
export interface ContractorLoadProgress { registrosCarregados: number; totalRegistros: number }
export type ContractorEmployeeView = DTO.RelatorioTerceirizadosDTOEmpregadoResumo & { contrato?: DTO.RelatorioTerceirizadosDTOContratoResumo | null };
