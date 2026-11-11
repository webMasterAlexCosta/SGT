import type * as DTO from '../../contracts/java';
import type { Page } from '../../contracts/shared';
import type { RequestOptions, QueryParams } from '../../core/http/types';
import { CONFIG } from '../../core/config';
import { requisitar } from '../../core/http';
import { prepararPaginaVisual, recortarPaginaVisual } from './paginacaoRelatorios.ts';

/**
 * A interface nunca materializa a base inteira. A leitura visual é paginada em
 * blocos de até 5.000 registros; exportações completas são geradas em fluxo no
 * backend e baixadas diretamente pelo navegador.
 */
export const RELATORIO_TERCEIRIZADOS_TAMANHO_MAXIMO = 5000;

const consolidado = async (params: QueryParams = {}, options: RequestOptions = {}): Promise<import("../../contracts/relatorios").ContractorReportView> => {
  const consulta = prepararPaginaVisual(params, 10, RELATORIO_TERCEIRIZADOS_TAMANHO_MAXIMO);
  const resposta = await requisitar<DTO.RelatorioTerceirizadosDTO>({
    method: 'GET',
    url: '/relatorios/terceirizados/consolidado',
    params: consulta.params,
    retries: 1,
    silentError: true,
    ...options,
  });
  const bloco: (DTO.RelatorioTerceirizadosDTOPaginaEmpregados & { pageable?: { pageNumber?: number; pageSize?: number; offset?: number } }) | null = resposta?.empregados;
  if (!Array.isArray(bloco?.content)) return resposta;
  const visual = recortarPaginaVisual(bloco.content, {
    pagina: bloco.number, tamanho: bloco.size, total: bloco.totalElements,
  }, consulta);
  return {
    ...resposta,
    empregados: {
      ...bloco,
      content: visual.itens,
      number: visual.pagina,
      size: visual.tamanho,
      totalPages: visual.totalPaginas,
      totalElements: visual.total,
      numberOfElements: visual.itens.length,
      first: visual.primeira,
      last: visual.ultima,
      empty: visual.itens.length === 0,
      ...(bloco.pageable && { pageable: { ...bloco.pageable, pageNumber: visual.pagina, pageSize: visual.tamanho, offset: visual.pagina * visual.tamanho } }),
    },
  };
};

const obterUrlExportacao = (params: QueryParams = {}, formato = 'XLSX') => {
  const query = new URLSearchParams();
  Object.entries({ ...params, formato: String(formato || 'XLSX').toUpperCase() } as QueryParams)
    .forEach(([chave, valor]) => {
      if (valor !== undefined && valor !== null && valor !== '' && valor !== false) {
        query.append(chave, String(valor));
      }
    });
  const base = String(CONFIG.API_URL || '').replace(/\/$/, '');
  return `${base}/relatorios/terceirizados/exportacao?${query.toString()}`;
};

export const ServicoRelatorioTerceirizados = {
  consolidado,
  obterUrlExportacao,
};
