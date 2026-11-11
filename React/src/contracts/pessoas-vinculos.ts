import type * as DTO from './java';
import type * as Input from './java-requests';
/** Detail begins with the list projection while its additional fields load. */
export type PersonDetailsView = DTO.PessoaResumoDTO & Partial<DTO.PessoaDTO>;
/** Optional display aliases supported by the existing legacy option renderer. */
export interface ContractOptionDisplay extends DTO.ContratoOpcaoDTO { numero?: string | number; anoContrato?: number; ano?: number; label?: string }
export type LinkCommand = Input.RequestRetificacaoInicioVinculoRequisicao | Input.RequestAlteracaoMatriculaVinculoRequisicao | Input.RequestAcaoVinculoContratualRequisicao | Input.RequestDesligamentoVinculoRequisicao;
export interface AllocationSubmission { modo: string; alocacaoId?: number | null; payload: Input.RequestAlteracaoAlocacaoVinculoRequisicao }

/** GET /vinculos-contratuais/elegiveis-frequencia; Java supplies eligibility and the D/K cut. */
export interface ConsultaVinculosElegiveisFrequencia {
  dataRef: import('./shared').CivilDate;
  lotacaoId: number;
  jornadaId: number;
  contratoId?: number;
  contratoIds?: readonly number[];
  busca?: string;
  page?: number;
  size?: number;
}
