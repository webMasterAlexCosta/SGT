import type * as DTO from './java';
import type * as Input from './java-requests';
/** UI command assembled from the publication or lifecycle form. */
export type ContractActionPayload = Input.RequestPublicacaoContratoDTO & Input.RequestAcaoCicloVidaContratoDTO;
export interface ContractActionState { tipo: string; contrato: DTO.ContratoDTO }
export interface ContractForm {
 id: string | number | null; numeroContrato: string; anoContrato: string | number; descricao: string;
 tipoContratacaoId: string; dataInicio: string; dataFim: string; gestaoLocalId: string;
 efetivoPrevisto: number; cnpjEmpresa: string; razaoSocialEmpresa: string; nomeFantasiaEmpresa: string;
 matriculaVinculoObrigatoria: boolean;
}
