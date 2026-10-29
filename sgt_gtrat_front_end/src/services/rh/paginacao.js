import { requisitar, extrairPagina } from '../../core/http';
import { CONFIG } from '../../core/config';
import { carregarTodasAsPaginas } from './nucleoPaginacao';

export const TAMANHO_PAGINA = 1000;
export const MAXIMO_ITENS_GRADE = CONFIG.FREQUENCIA_MAX_GRID_ITEMS;
export const MAXIMO_REQUISICOES_PAGINAS_SIMULTANEAS = 4;

const buscarPagina = async ({ url, params, options, page, size, otimizarContagem }) => {
  const parametros = { ...params, page, size };
  if (otimizarContagem) parametros.skipCount = page > 0;
  const response = await requisitar({
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
export async function requisitarTodasAsPaginas({
  url,
  params = {},
  options = {},
  maximoItens = MAXIMO_ITENS_GRADE,
  otimizarContagem = false,
}) {
  return carregarTodasAsPaginas({
    tamanhoPagina: TAMANHO_PAGINA,
    maximoItens,
    concorrencia: MAXIMO_REQUISICOES_PAGINAS_SIMULTANEAS,
    buscarPagina: (page, size) => buscarPagina({ url, params, options, page, size, otimizarContagem }),
  });
}
