import type { MeuPerfilDTO, PerfilAcesso, ResumoUsuarioAplicacaoDTOReferencia, ResumoUsuarioAplicacaoDTOOrganizacaoReferencia, ResumoUsuarioAplicacaoDTOVinculoGestaoReferencia } from './java';
import type { Instant, LocalDateTime } from './shared';

/** The auth service currently emits maps; these fields mirror AutenticacaoServico. */
export interface AuthFields extends Partial<Omit<MeuPerfilDTO, 'lotacao' | 'vinculosGestao' | 'superintendencia' | 'gestaoLocal'>> {
  superintendencia?: (ResumoUsuarioAplicacaoDTOOrganizacaoReferencia & { nome?: string }) | null;
  gestaoLocal?: (ResumoUsuarioAplicacaoDTOOrganizacaoReferencia & { nome?: string }) | null;
  /** Display aliases retained by the existing UI; they are not Java DTO fields. */
  nomeCompleto?: string | null; nome?: string | null; displayName?: string | null;
  user?: string; lotacao?: (ResumoUsuarioAplicacaoDTOReferencia & { nome?: string }) | string | null;
  lotacaoObj?: ResumoUsuarioAplicacaoDTOReferencia | null;
  superintendenciaId?: number | null; superintendenciaNome?: string | null;
  gestaoLocalId?: number | null; gestaoLocalNome?: string | null; uf?: string | null;
  retroactiveUnlocked?: boolean; twoFactorVerified?: boolean;
  vinculosGestao?: AuthManagementLink[] | null;
}
export interface AuthManagementLink extends Partial<ResumoUsuarioAplicacaoDTOVinculoGestaoReferencia> {
  lotacao?: (ResumoUsuarioAplicacaoDTOReferencia & { nome?: string }) | null;
  jornada?: (ResumoUsuarioAplicacaoDTOReferencia & { descricao?: string }) | null;
  lotacaoId?: number | null; jornadaId?: number | null; lotacaoNome?: string | null; jornadaNome?: string | null;
}
export interface AuthenticatedUser extends Omit<AuthFields, 'lotacao' | 'vinculosGestao'> {
  perfil: PerfilAcesso; autoridade: string; username: string | null;
  vinculosGestao: AuthManagementLink[]; lotacao: string | null;
  forceChangePassword: boolean; isRetroactiveUnlocked: boolean;
  twoFactorEnabled: boolean; twoFactorVerified: boolean;
  error?: false; requiresTwoFactor?: false; canceled?: false;
}
export interface LoginSuccess extends AuthFields {
  message: string; error?: false; requiresTwoFactor?: false; canceled?: false;
}
export interface LoginChallenge {
  requiresTwoFactor: true; canSkipTwoFactor: boolean; message: string;
  error?: false; canceled?: false;
}
export interface LoginFailure {
  error: true; canceled?: boolean; silencioso?: boolean; status?: number; code?: string;
  message?: string; lockout?: number | null; requiresTwoFactor?: false;
}
export type LoginResponse = LoginSuccess | LoginChallenge;
export type LoginResult = LoginResponse | LoginFailure;
export interface SessionResponse {
  authenticated: boolean; version: number;
  user: AuthFields;
  session: { expiresAtUtc: Instant; remainingSeconds: number; boundToDevice: boolean;
    lastSeenAt: LocalDateTime | null; loginIp: string | null; userAgent: string | null; };
}
export type { ResumoUsuarioAplicacaoDTOOrganizacaoReferencia as OrganizationReference };
