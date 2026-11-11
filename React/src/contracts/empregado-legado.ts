/** Projeção de compatibilidade da ficha antiga, disponível apenas para leitura.
 * Os campos opcionais legados não fazem parte do DTO nem de novas requisições Java.
 */
import type { EmpregadoListaDTO, EmpregadoDTO, ContratoDTO, JornadaDTO, JornadaResumoDTO, LotacaoDTO, CargoDTO, StatusEmpregadoDTO } from './java';

export interface LegacyEmployeeView extends EmpregadoDTO {
  lotacao?: { id?: number };
  cargo?: { id?: number };
  jornada?: { id?: number };
  turno?: { id?: number };
  status?: { id?: number };
  telefones?: Array<{ numero: string }>;
  conta?: { bancoId?: number; agenciaId?: number; tipoContaId?: number; numeroConta?: string };
  optanteFgts?: boolean;
  optanteVa?: boolean;
  dataOpcaoFgts?: string;
}
export type LegacyShiftView = Partial<JornadaDTO> & Partial<JornadaResumoDTO> & { nome?: string; duracaoPadraoMin?: number | null };
export type LegacyContractView = ContratoDTO & { valorHora?: string | number; numero?: string; nome?: string };
export interface LegacyCatalogOption { id: number; codigo?: string; numero?: string; nome?: string; descricao?: string; bancoId?: number; banco?: { id?: number } }
export interface LegacyEmployeeCatalogs {
  lotacoes: LotacaoDTO[];
  cargos: CargoDTO[];
  jornadas: JornadaDTO[];
  bancos: LegacyCatalogOption[];
  agencias: LegacyCatalogOption[];
  tiposConta: LegacyCatalogOption[];
  contratos: LegacyContractView[];
  statuses: StatusEmpregadoDTO[];
}

/** Read-model used by the compact list. Optional nested aliases are presentation compatibility only. */
export interface EmployeeListView extends EmpregadoListaDTO {
  lotacao?: { nome?: string; descricao?: string };
  jornada?: { descricao?: string; nome?: string };
  turno?: { descricao?: string; nome?: string };
  cargo?: { nome?: string; descricao?: string };
  status?: { descricao?: string };
}
export interface EmployeeGroup { id: string; titulo: string; descricao: string; isWarning: boolean; lista: EmployeeListView[]; stats: { total: number; ativos: number } }
export interface AdvancedEmployeeFilters { lotacaoId?: string | number | null; turnoId?: string | number | null; tamanho: string | number }
