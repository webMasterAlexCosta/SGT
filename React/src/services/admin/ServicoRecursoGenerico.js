import { requisitar, extrairLista, extrairPagina } from '../../core/http';
import { prepararRecursoParaEnvio } from './ServicoDominioRecursoGenerico.js';
import { requisitarTodasAsPaginas } from '../rh/paginacao.js';

export const ServicoRecursoGenerico = (endpoint) => ({
  listarPagina: async (params = {}, options = {}) =>
    extrairPagina(await requisitar({ method: 'GET', url: endpoint, params, ...options })),

  listarTodos: async (options = {}) => endpoint === '/cargos'
    ? requisitarTodasAsPaginas({ url: endpoint, options })
    : extrairLista(await requisitar({ method: 'GET', url: endpoint, ...options })),

  buscarPorId: async (id, options = {}) =>
    requisitar({ method: 'GET', url: `${endpoint}/${id}`, ...options }),

  salvar: async (item, options = {}) => requisitar({
    method: item.id ? 'PUT' : 'POST',
    url: item.id ? `${endpoint}/${item.id}` : endpoint,
    data: prepararRecursoParaEnvio(item),
    successMessage: item.id ? 'Registro atualizado.' : 'Registro criado.',
    ...options
  }),

  excluir: async (id, options = {}) => requisitar({
    method: 'DELETE',
    url: `${endpoint}/${id}`,
    successMessage: 'Registro removido.',
    ...options
  })
});
