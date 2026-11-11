import { exigirIdentificadorRota, type ResourceIdentifierInput } from '../../core/http/identificadores';
import type * as DTO from '../../contracts/java';
import type { Page } from '../../contracts/shared';
import type { RequestOptions, QueryParams } from '../../core/http/types';
import { requisitar } from '../../core/http';

export const ServicoAuditoria = {
  buscar: async (params: QueryParams, options: RequestOptions = {}) => requisitar<Page<DTO.RegistroAuditoriaDTO>>({ method: 'GET', url: '/registros-auditoria', params, ...options }),
  buscarPorId: async (id: ResourceIdentifierInput, options: RequestOptions = {}) => { id = exigirIdentificadorRota(id); return requisitar<DTO.RegistroAuditoriaDTO>({ method: 'GET', url: `/registros-auditoria/${id}`, ...options }); }
};