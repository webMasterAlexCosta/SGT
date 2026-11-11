export interface LocationForm { id: number | null; nome: string; municipio: string; gestaoLocalId: string; ativo: boolean; turnos: { id: number | null }[] }
export type AnalyzedShift = import('./java').JornadaDTO & { analise: ReturnType<typeof import('../services/admin/ServicoDominioJornada').analisarEscalaCompleta> };

export interface StateManagementForm { id: number | null; nome: string; uf: string; ativo: boolean; ativoAnterior: boolean }
export interface LocalManagementForm { id: number | null; nome: string; codigo: string; superintendenciaId: string; ativo: boolean }
