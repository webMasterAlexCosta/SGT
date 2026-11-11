import type { FrequenciaDTO, EmpregadoFrequenciaDTO, OcorrenciaResumoDTO, FrequenciaExcecaoDTO, FrequenciaAdicionalDTO, CalendarioEventoDTO } from './java';

/** Linha de edição construída da frequência, empregado e complementos Java. */
export interface FrequencyRow extends Pick<FrequenciaDTO, 'vinculoContratualId' | 'entrada1' | 'saida1' | 'entrada2' | 'saida2' | 'horasTrabalhadas' | 'horarioSaida' | 'horasNoturnas' | 'prazoEdicao' | 'estadoNegocial' | 'validadeOperacional' | 'estadoAuditoria' | 'versaoNegocial' | 'bloqueadaEdicaoOperacional' | 'status'> {
  empregadoId: number | null;
  frequenciaId: number | null;
  identidadeCanonicaDisponivel: boolean;
  resolucaoCanonicaPermitida: boolean;
  nome: string | null; cpf: string | null; matricula: string | null;
  indiceBusca: string;
  lotacao: string; lotacaoId: number | null;
  turno: string; turnoId: number | null; cargo: string;
  horarioTrabalhoDoDia: string; entradaTurno: string | null;
  obs: string;
  jaSalvo: boolean;
  estadoLancamento: string;
  edicaoBloqueadaPeloBackend: boolean;
  ocorrencia: { motivo: string | null; tipo: string | null } | null;
  diaDescansoCalendario: boolean;
  calendarioTipo: string | null; calendarioDescricao: string;
  diaFeriado: boolean; folgaRemunerada: boolean;
  rtQuantidade: number | null; rtJustificativa: string;
  isDiaDescanso: boolean; showRtBadge: boolean; isImplicit: boolean;
  chaveIdempotencia?: string | null; alterado?: boolean;
}
export interface FrequencyContext {
  mapaFrequencias: Map<number | null, FrequenciaDTO>;
  mapaOcorrencias: Map<number | null, OcorrenciaResumoDTO>;
  mapaExcecoes: Map<number | null, FrequenciaExcecaoDTO[]>;
  mapaAdicionais: Map<number | null, FrequenciaAdicionalDTO[]>;
  tipoCalendario: string | null;
  objetoCalendario?: CalendarioEventoDTO;
  diaDaSemanaStr: string;
}
export interface PartialAttendanceState {
  open: boolean; empregadoId: number | null; horas: string; saida: string;
  entradaOriginal: string | null; obs: string; noturnas: number; saidaManual: boolean;
}
export interface SubmissionProgress { processados: number; total: number; loteAtual: number; totalLotes: number }
export interface UserFrequencyScope { lotacaoId: string; jornadaId: string; lotacaoNome: string; jornadaNome: string }
export type FrequencyEmployee = EmpregadoFrequenciaDTO;
