import type * as DTO from './java';
import type { ReactNode } from 'react';
/** Missing properties represent the loading/empty presentation, never new API fields. */
export type LoadingView<T> = T extends (infer E)[] ? LoadingView<E>[] : T extends object ? { [K in keyof T]?: LoadingView<T[K]> } : T;
/** Older cached panel maps used registrosLancados; Java emits registrosEsperados. */
export type CoverageView = Partial<DTO.PainelGestaoDTOCoberturaFrequencia> & { registrosLancados?: number };
export type ManagementPanelView = Omit<LoadingView<DTO.PainelGestaoDTO>, 'frequencia'> & { frequencia?: CoverageView | null };
export interface IndicatorHelp { finalidade: string; calculo: string; fonte: string; interpretacao: string; }
export interface SummaryItem { rotulo: string | null; valor: ReactNode; classe?: string; }
export interface ChartTooltipProps<T = Record<string, unknown>> {
  active?: boolean; label?: ReactNode;
  payload?: readonly { dataKey?: string | number; name?: string | number; value?: import("recharts").TooltipValueType; color?: string; fill?: string; payload?: T }[];
  formatadorValor?: (valor: import("recharts").TooltipValueType | undefined) => ReactNode;
}
export interface StaffingReportContext {
  referencia?: string | null;
  contratoNome?: string | null;
  escopoSelecionado?: string | null;
  periodoRotulo?: string | null;
  comparar?: boolean;
}
export interface StaffingReport {
  dados: DTO.PainelEfetivoDTO;
  contexto: StaffingReportContext;
}
export type StaffingSeriesPoint = DTO.PainelEfetivoDTOPontoDiario & { rotulo: string; id?: string | number | null; nome?: string | null; presentesAnoAnterior?: number | null };
export type StaffingScatterPoint = Partial<DTO.PainelEfetivoDTOPontoDiario> & { rotulo: string; dimensao?: string; id?: string | number | null; nome?: string | null; presentesAnoAnterior?: number | null };

export type ActivitySummary = Pick<DTO.AtividadePainelDTO, 'id' | 'dataEvento' | 'descricao' | 'empregadoNome' | 'usuarioUsername'>;
