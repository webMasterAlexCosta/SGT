import { requisitar, extrairLista, extrairPagina } from '../../core/http';

const paginaTodas = async (params = {}, options = {}) => {
  const tamanho = Math.max(1, Number(params?.size || 500));
  const baseParams = { ...params, size: tamanho };
  const acumulado = [];
  let pagina = Math.max(0, Number(params?.page || 0));
  let totalPaginas = 1;

  do {
    const resposta = await requisitar({
      method: 'GET',
      url: '/lotacoes',
      params: { ...baseParams, page: pagina },
      ...options,
    });
    const dadosPagina = extrairPagina(resposta);
    acumulado.push(...dadosPagina.content);
    totalPaginas = Math.max(1, Number(dadosPagina.totalPages || 1));
    pagina += 1;
  } while (pagina < totalPaginas);

  return acumulado;
};

export const ServicoLotacao = {
  listar: async (params, options = {}) => extrairLista(await requisitar({ method: 'GET', url: '/lotacoes', params, ...options })),
  listarTodos: paginaTodas,
  salvar: async (payload, options = {}) => requisitar({
    method: payload.id ? 'PUT' : 'POST', url: payload.id ? `/lotacoes/${payload.id}` : '/lotacoes', data: payload, successMessage: payload.id ? 'Lotação atualizada.' : 'Lotação criada.', ...options
  }),
  excluir: async (id, options = {}) => requisitar({ method: 'DELETE', url: `/lotacoes/${id}`, successMessage: 'Lotação excluída.', ...options })
};
