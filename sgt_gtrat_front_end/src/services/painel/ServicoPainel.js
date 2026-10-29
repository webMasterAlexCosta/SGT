import { requisitar } from "../../core/http";
import { CONFIG } from "../../core/config";

export const ServicoPainel = {
  solicitarCalculo: async (referencia, periodoDias = 30, forcar = false, options = {}) =>
    requisitar({
      method: "POST",
      url: "/painel/calculos",
      params: { ...(referencia ? { referencia } : {}), periodoDias, forcar },
      timeout: 30_000,
      ...options,
    }),

  consultarCalculo: async (id, options = {}) =>
    requisitar({ method: "GET", url: `/painel/calculos/${id}`, timeout: 30_000, ...options }),

  buscarGestao: async (params = {}, options = {}) =>
    requisitar({ method: "GET", url: "/painel/gestao", params, timeout: 120_000, ...options }),

  buscarAtividadesRecentes: async (size = CONFIG.DASHBOARD_RECENT_ACTIVITIES, options = {}) =>
    requisitar({ method: "GET", url: "/painel/atividades", params: { size }, ...options }),
  buscarEstatisticasAdministrativas: async (options = {}) =>
    requisitar({ method: "GET", url: "/painel/estatisticas", ...options }),
  buscarRankingLotacoes: async (params, options = {}) =>
    requisitar({ method: "GET", url: "/painel/ranking-lotacoes", params, timeout: 120_000, ...options }),

  buscarFiltrosEfetivo: async (params = {}, options = {}) =>
    requisitar({ method: "GET", url: "/painel/efetivo/filtros", params, timeout: 30_000, ...options }),

  buscarIndicadorEfetivo: async (params = {}, options = {}) =>
    requisitar({ method: "GET", url: "/painel/efetivo", params, timeout: 120_000, ...options }),
};
