import { exigirIdentificadorRota, type ResourceIdentifierInput } from '../../core/http/identificadores';
import type * as DTO from '../../contracts/java';
import type { Page } from '../../contracts/shared';
import type { RequestOptions, QueryParams } from '../../core/http/types';
import { requisitar, extrairLista, extrairPagina } from '../../core/http';

const paginaTodas = async (params: QueryParams = {}, options: RequestOptions = {}) => {
  const tamanho = Math.max(1, Number(params?.size || 500));
  const baseParams = { ...params, size: tamanho };
  const acumulado: DTO.LotacaoDTO[] = [];
  let pagina = Math.max(0, Number(params?.page || 0));
  let totalPaginas = 1;

  do {
    const resposta = await requisitar<Page<DTO.LotacaoDTO>>({
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
  listar: async (params: QueryParams, options: RequestOptions = {}) => extrairLista(await requisitar<Page<DTO.LotacaoDTO>>({ method: 'GET', url: '/lotacoes', params, ...options })),
  listarTodos: paginaTodas,
  salvar: async (payload: import('../../contracts/java-requests').RequestLotacaoDTO, options: RequestOptions = {}) => requisitar({
    method: payload.id ? 'PUT' : 'POST', url: payload.id ? `/lotacoes/${payload.id}` : '/lotacoes', data: payload, successMessage: payload.id ? 'Lotação atualizada.' : 'Lotação criada.', ...options
  }),
  excluir: async (id: ResourceIdentifierInput, options: RequestOptions = {}) => { id = exigirIdentificadorRota(id); return requisitar<null>({ method: 'DELETE', url: `/lotacoes/${id}`, successMessage: 'Lotação excluída.', ...options }); }
};
