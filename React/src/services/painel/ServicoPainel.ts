import { exigirIdentificadorRota, type ResourceIdentifierInput } from '../../core/http/identificadores';
import type * as DTO from '../../contracts/java';
import type { Page } from '../../contracts/shared';
import type { RequestOptions, QueryParams } from '../../core/http/types';
import { requisitar } from "../../core/http";
import { CONFIG } from "../../core/config";
import type { PainelAnaliticoDTO, ParametrosAnalitico, PaginaRegistrosAnalitico, ParametrosRegistrosAnalitico } from '../../contracts/painel-analitico';

export const ServicoPainel = {
  buscarVisoes: async (params: ParametrosAnalitico, options: RequestOptions = {}) =>
    requisitar<import('../../contracts/painel-visoes').PainelVisoesDTO>({ method: 'GET', url: '/painel/visoes', params: { ...params }, timeout: 120_000, ...options }),
  buscarRegistrosAnalitico: async (params: ParametrosRegistrosAnalitico, options: RequestOptions = {}) =>
    requisitar<PaginaRegistrosAnalitico>({ method: 'GET', url: '/painel/analitico/registros', params: { ...params }, timeout: 120_000, ...options }),
  buscarAnalitico: async (params: ParametrosAnalitico, options: RequestOptions = {}) =>
    requisitar<PainelAnaliticoDTO>({ method: 'GET', url: '/painel/analitico', params: { ...params }, timeout: 120_000, ...options }),
  solicitarCalculo: async (referencia: (string), periodoDias = 1, forcar = false, options: RequestOptions = {}) =>
    requisitar<DTO.PainelCalculoDTO>({
      method: "POST",
      url: "/painel/calculos",
      params: { ...(referencia ? { referencia } : {}), periodoDias, forcar },
      timeout: 30_000,
      ...options,
    }),

  consultarCalculo: async (id: ResourceIdentifierInput, options: RequestOptions = {}) =>
    { id = exigirIdentificadorRota(id); return requisitar<DTO.PainelCalculoDTO>({ method: "GET", url: `/painel/calculos/${id}`, timeout: 30_000, ...options }); },

  buscarGestao: async (params: QueryParams = {}, options: RequestOptions = {}) =>
    requisitar<DTO.PainelGestaoDTO>({ method: "GET", url: "/painel/gestao", params, timeout: 120_000, ...options }),

  buscarAtividadesRecentes: async (size = CONFIG.DASHBOARD_RECENT_ACTIVITIES, options: RequestOptions = {}) =>
    requisitar<Page<DTO.AtividadePainelDTO>>({ method: "GET", url: "/painel/atividades", params: { size }, ...options }),
  buscarEstatisticasAdministrativas: async (options: RequestOptions = {}) =>
    requisitar<DTO.EstatisticasPainelDTO>({ method: "GET", url: "/painel/estatisticas", ...options }),
  buscarRankingLotacoes: async (params: QueryParams, options: RequestOptions = {}) =>
    requisitar<Array<DTO.PainelRankingDTO>>({ method: "GET", url: "/painel/ranking-lotacoes", params, timeout: 120_000, ...options }),

  buscarFiltrosEfetivo: async (params: QueryParams = {}, options: RequestOptions = {}) =>
    requisitar<DTO.PainelEfetivoDTOFiltros>({ method: "GET", url: "/painel/efetivo/filtros", params, timeout: 30_000, ...options }),

  buscarIndicadorEfetivo: async (params: QueryParams = {}, options: RequestOptions = {}) =>
    requisitar<DTO.PainelEfetivoDTO>({ method: "GET", url: "/painel/efetivo", params, timeout: 120_000, ...options }),
};
