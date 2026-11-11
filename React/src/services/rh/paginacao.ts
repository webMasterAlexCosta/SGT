import type { Page } from '../../contracts/shared';
import type { QueryParams, RequestOptions } from '../../core/http/types';
import { requisitar, extrairPagina } from '../../core/http';
import { CONFIG } from '../../core/config';
import { carregarTodasAsPaginas } from './nucleoPaginacao';

export const TAMANHO_PAGINA = 1000;
export const MAXIMO_ITENS_GRADE = CONFIG.FREQUENCIA_MAX_GRID_ITEMS;
export const MAXIMO_REQUISICOES_PAGINAS_SIMULTANEAS = 4;

const buscarPagina = async <T,>({ url, params, options, page, size, otimizarContagem }: PaginationOptions & { page: number; size: number }): Promise<Page<T>> => {
  const parametros: QueryParams = { ...params, page, size };
  if (otimizarContagem) parametros.skipCount = page > 0;
  const response = await requisitar<Page<T>>({
    method: 'GET',
    url,
    params: parametros,
    ...options,
  });
  return extrairPagina(response);
};

/**
 * Lê a primeira página para conhecer o total e busca as demais em pequenos
 * grupos paralelos. Mantém a ordem das páginas e evita dezenas de viagens
 * sequenciais para escalas com milhares de colaboradores.
 */
interface PaginationOptions { url: string; params?: QueryParams; options?: RequestOptions; maximoItens?: number; otimizarContagem?: boolean; }

export async function requisitarTodasAsPaginas<T>({
  url,
  params = {},
  options = {},
  maximoItens = MAXIMO_ITENS_GRADE,
  otimizarContagem = false,
}: PaginationOptions): Promise<T[]> {
  return carregarTodasAsPaginas<T>({
    tamanhoPagina: TAMANHO_PAGINA,
    maximoItens,
    concorrencia: MAXIMO_REQUISICOES_PAGINAS_SIMULTANEAS,
    buscarPagina: (page, size) => buscarPagina<T>({ url, params, options, page, size, otimizarContagem }),
  });
}
