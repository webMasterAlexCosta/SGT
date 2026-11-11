import type { ExportacaoRelatorioStatusDTO } from './java';
import type { QueryParams } from '../core/http/types';
/** Local failures have no server artifact or server timestamps. */
export type ExportTask = Omit<ExportacaoRelatorioStatusDTO, 'nomeArquivo' | 'tamanhoBytes' | 'iniciadoEm' | 'atualizadoEm'> & Partial<Pick<ExportacaoRelatorioStatusDTO, 'nomeArquivo' | 'tamanhoBytes' | 'iniciadoEm' | 'atualizadoEm'>>;
export interface ExportRequest { tipo: 'frequencia' | 'absenteismo' | 'terceirizados'; parametros?: QueryParams; formato?: string; totalEstimado?: number }
export interface ExportContext { iniciarExportacao: (pedido: ExportRequest) => Promise<ExportacaoRelatorioStatusDTO>; cancelarExportacao: (id: string | null) => Promise<void>; tarefas: ExportTask[] }
export interface ExportPanelProps {
  tarefas?: ExportTask[]; minimizado: boolean; aoAlternarMinimizado: () => void;
  aoFechar: (id: string | null) => void; aoBaixarNovamente: (id: string | null) => void;
  aoCancelar: (id: string | null) => Promise<void>;
  cancelando: ReadonlySet<string>;
  errosCancelamento: Readonly<Record<string, string>>;
}
