import type { BancoReferenciaDTO, AgenciaReferenciaDTO, CargoDTO, TipoContratacaoDTO } from './java';
export interface CatalogIdentity { id?: number | null; version?: number | null; ativo?: boolean | null }
export interface CatalogRecord extends CatalogIdentity { [field: string]: string | number | boolean | null | undefined }
export interface CatalogEndpoints { '/status-empregados': import('./java').StatusEmpregadoDTO; '/bancos': BancoReferenciaDTO; '/agencias': AgenciaReferenciaDTO; '/cargos': CargoDTO; '/tipos-contratacao': TipoContratacaoDTO }
export type CatalogDto<E extends string> = (E extends keyof CatalogEndpoints ? CatalogEndpoints[E] : CatalogRecord) & CatalogIdentity;

/** Payload mantido pela tela legada; referência de agência retornada é distinta. */
export type CatalogCommand<E extends string> = E extends '/agencias' ? { id?: number; numero?: string; bancoId?: number } : Partial<CatalogDto<E>>;
export type ScalarCatalogEndpoint = '/cargos' | '/tipos-contratacao' | '/status-empregados';
export type ScalarCatalogKey = keyof CargoDTO | keyof TipoContratacaoDTO | keyof import('./java').StatusEmpregadoDTO;
export interface CatalogColumn { key: ScalarCatalogKey; label: string; type?: 'boolean' | 'date' | 'text'; trueLabel?: string; falseLabel?: string; render?: (item: CatalogRecord) => import('react').ReactNode }
export interface CatalogField { key: ScalarCatalogKey; label: string; type?: string; defaultValue?: CatalogRecord[string]; required?: boolean; readOnlyOnEdit?: boolean }
export interface ScalarCatalogProps {
 title: string; descricao?: string; serviceEndpoint: ScalarCatalogEndpoint;
 columns: CatalogColumn[]; fields: CatalogField[]; podeEscrever?: boolean; paginado?: boolean;
 suportaAtivo?: boolean; permiteExcluir?: boolean; exclusaoEhInativacao?: boolean; avisoSomenteLeitura?: string | null;
}
