import { exigirIdentificadorRota, type ResourceIdentifierInput } from '../../core/http/identificadores';
import type * as DTO from '../../contracts/java';
import type { Page } from '../../contracts/shared';
import type { RequestOptions, QueryParams } from '../../core/http/types';
import { requisitar } from '../../core/http';

const limparParametros = (params: QueryParams = {}) => Object.fromEntries(
  Object.entries(params).filter(([, valor]) => valor !== undefined && valor !== null && valor !== ''),
);

export const ServicoPendenciaAdministrativa = Object.freeze({
  listar: async (params: QueryParams = {}, options: RequestOptions = {}) => requisitar<DTO.PaginaPendenciasDTO>({
    method: 'GET',
    url: '/pendencias',
    params: limparParametros(params),
    ...options,
  }),

  obterResumo: async (params: QueryParams = {}, options: RequestOptions = {}) => requisitar<DTO.ResumoPendenciasDTO>({
    method: 'GET',
    url: '/pendencias/resumo',
    params: limparParametros(params),
    ...options,
  }),

  iniciarTratamento: async (id: ResourceIdentifierInput, observacao = '', options: RequestOptions = {}) => { id = exigirIdentificadorRota(id); return requisitar<DTO.PendenciaDTO>({
    method: 'POST',
    url: `/pendencias/${id}/iniciar-tratamento`,
    data: observacao?.trim() ? { observacao: observacao.trim() } : {},
    retries: 0,
    ...options,
  }); },

  confirmarResolucao: async (id: ResourceIdentifierInput, observacao = '', options: RequestOptions = {}) => { id = exigirIdentificadorRota(id); return requisitar<DTO.PendenciaDTO>({
    method: 'POST',
    url: `/pendencias/${id}/confirmar-resolucao`,
    data: observacao?.trim() ? { observacao: observacao.trim() } : {},
    retries: 0,
    ...options,
  }); },
});
