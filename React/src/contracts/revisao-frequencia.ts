export interface RevisaoFrequencia {
  frequenciaId: number; versao: number; status: 'SEM_SOLICITACAO' | 'PENDENTE' | 'CONFERIDA' | 'REJEITADA';
  solicitadaEm: string | null; motivo: string | null; decididaEm: string | null; decisao: string | null;
  podeSolicitar: boolean; podeDecidir: boolean;
}
export interface ComandoRevisaoFrequencia { versao: number; motivo: string; chaveIdempotencia: string; decisao?: 'CONFERIDA' | 'REJEITADA'; }
