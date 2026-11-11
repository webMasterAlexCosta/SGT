import { exigirIdentificadorRota, type ResourceIdentifierInput } from '../../core/http/identificadores';
import type { CatalogDto, CatalogCommand } from '../../contracts/catalogos';
import type * as DTO from '../../contracts/java';
import type { Page } from '../../contracts/shared';
import type { RequestOptions, QueryParams } from '../../core/http/types';
import { requisitar, extrairLista, extrairPagina } from '../../core/http';
import { prepararRecursoParaEnvio } from './ServicoDominioRecursoGenerico.ts';
import { requisitarTodasAsPaginas } from '../rh/paginacao.ts';

export const ServicoRecursoGenerico = <E extends string>(endpoint: E) => ({
  listarPagina: async (params: QueryParams = {}, options: RequestOptions = {}) =>
    extrairPagina(await requisitar<Page<CatalogDto<E>>>({ method: 'GET', url: endpoint, params, ...options })),

  listarTodos: async (options: RequestOptions = {}) => endpoint === '/cargos'
    ? requisitarTodasAsPaginas<CatalogDto<E>>({ url: endpoint, options })
    : extrairLista(await requisitar<CatalogDto<E>[]>({ method: 'GET', url: endpoint, ...options })),

  buscarPorId: async (id: ResourceIdentifierInput, options: RequestOptions = {}) =>
    { id = exigirIdentificadorRota(id); return requisitar<CatalogDto<E>>({ method: 'GET', url: `${endpoint}/${id}`, ...options }); },

  salvar: async (item: CatalogCommand<E>, options: RequestOptions = {}) => requisitar<CatalogDto<E>>({
    method: item.id ? 'PUT' : 'POST',
    url: item.id ? `${endpoint}/${item.id}` : endpoint,
    data: prepararRecursoParaEnvio(item),
    successMessage: item.id ? 'Registro atualizado.' : 'Registro criado.',
    ...options
  }),

  excluir: async (id: ResourceIdentifierInput, options: RequestOptions = {}) => { id = exigirIdentificadorRota(id); return requisitar<null>({
    method: 'DELETE',
    url: `${endpoint}/${id}`,
    successMessage: 'Registro removido.',
    ...options
  }); }
});
