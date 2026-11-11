import type * as DTO from './java';
export interface ManagementLinkForm { lotacaoId: string; jornadaId: string; lotacaoNome: string; jornadaNome: string }
export interface UserForm {
 id: number | null; username: string; email: string; password: string; matricula: string;
 perfil: DTO.PerfilAcesso; active: boolean; superintendenciaId: string; gestaoLocalId: string;
 vinculosGestao: ManagementLinkForm[]; novoLotacaoId: string; novoJornadaId: string;
}
export interface PasswordResetForm { usuario: DTO.ResumoUsuarioAplicacaoDTO | null; novaSenha: string; confirmarSenha: string }
export type ManagementLinkInput = Partial<DTO.ResumoUsuarioAplicacaoDTOVinculoGestaoReferencia> & Partial<ManagementLinkForm>;
