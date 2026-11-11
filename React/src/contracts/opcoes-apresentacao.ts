import type * as DTO from './java';
/** Optional aliases read by existing presentation fallbacks. They are not Java wire fields and are never used to build a command. */
export type ContractOptionView = DTO.ContratoOpcaoDTO & { numero?: string | number; nome?: string; label?: string; ano?: number; anoContrato?: number };
export type LocationOptionView = Omit<DTO.LotacaoOpcaoDTO, 'turnos'> & { turnos: ShiftSummaryView[] | null; descricao?: string };
export type ShiftSummaryView = DTO.JornadaResumoDTO & { nome?: string };
export type LocalManagementView = DTO.GestaoLocalDTO & { sigla?: string; superintendencia?: DTO.SuperintendenciaDTO | null };
export type LocationView = DTO.LotacaoDTO & { gestaoLocal?: LocalManagementView | null };
export type ContractView = DTO.ContratoDTO & { numero?: string | number; nome?: string; label?: string; ano?: number; gestaoLocalUf?: string };
export type UserSummaryView = DTO.ResumoUsuarioAplicacaoDTO & { nome?: string; uf?: string; retroactiveExpireAt?: string | null };
export type CalendarEventView = DTO.CalendarioEventoDTO & { superintendenciaId?: number; gestaoLocalId?: number; superintendenciaNome?: string };
export type AgencyView = DTO.AgenciaReferenciaDTO & { bancoId?: number | null };
