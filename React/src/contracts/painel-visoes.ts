import type { PainelAnaliticoDTO } from './painel-analitico';
import type { PainelGestaoDTO, PainelRankingDTO, PainelGestaoDTOResumoEfetivo } from './java';

export type VisaoGerencial = 'resumo' | 'contratos' | 'operacao' | 'tudo';
/** A consulta composta sempre materializa estes agregados, sem payload de cache parcial. */
export type GestaoVisoes = { [K in keyof PainelGestaoDTO]: K extends 'comparativoPeriodo' ? PainelGestaoDTO[K] : NonNullable<PainelGestaoDTO[K]> };
export interface PainelVisoesDTO {
  analitico: PainelAnaliticoDTO;
  gestao: GestaoVisoes;
  ranking: PainelRankingDTO[];
  efetivoAnterior: PainelGestaoDTOResumoEfetivo | null;
}
