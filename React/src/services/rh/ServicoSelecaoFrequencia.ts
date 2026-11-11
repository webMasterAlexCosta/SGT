import type { VinculoElegivelFrequenciaDTO } from '../../contracts/java';
import type { CivilDate } from '../../contracts/shared';

/** Wire-shape refinement; civil validity and business eligibility remain Java responsibilities. */
export const ehReferenciaCivilIso = (valor: string | null): valor is CivilDate =>
  typeof valor === 'string' && /^\d{4}-\d{2}-\d{2}$/.test(valor);

export interface ContextoSelecaoFrequencia {
  dataReferencia: string | null;
  lotacaoId: string;
  jornadaId: string;
  contratoId: number | null;
  gestaoLocalId: number | null;
}

const idValido = (valor: number | null) => Number.isSafeInteger(valor) && Number(valor) > 0;

/** Checks response correspondence, not whether a link or contract is temporally eligible. */
export const correspondeAoContextoDaFrequencia = (
  vinculo: VinculoElegivelFrequenciaDTO,
  contexto: ContextoSelecaoFrequencia,
): boolean => [vinculo.id, vinculo.pessoaId, vinculo.contratoId, vinculo.gestaoLocalId,
  vinculo.alocacaoId, vinculo.lotacaoId, vinculo.jornadaId, vinculo.cargoId].every(idValido)
  && vinculo.dataReferencia === contexto.dataReferencia
  && Number(vinculo.lotacaoId) === Number(contexto.lotacaoId)
  && Number(vinculo.jornadaId) === Number(contexto.jornadaId)
  && (contexto.contratoId == null || vinculo.contratoId === contexto.contratoId)
  && (contexto.gestaoLocalId == null || vinculo.gestaoLocalId === contexto.gestaoLocalId);
