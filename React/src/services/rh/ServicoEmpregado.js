import { requisitar, extrairLista } from '../../core/http';
import { CONFIG } from '../../core/config';
import { requisitarTodasAsPaginas } from './paginacao';

export const ServicoEmpregado = {
  listar: async (page = 0, filters = {}, options = {}) => {
    const cleanFilters = {};
    Object.keys(filters).forEach(key => { if (filters[key]) cleanFilters[key] = filters[key]; });
    
    return await requisitar({ 
      method: 'GET', 
      url: '/empregados', 
      params: { page, size: CONFIG.PAGINATION_DEFAULT_SIZE, ...cleanFilters }, 
      ...options 
    });
  },

  listarTodasPaginas: async (filters = {}, options = {}) => {
    const cleanFilters = {};
    Object.keys(filters).forEach((key) => {
      const value = filters[key];
      if (value !== undefined && value !== null && value !== '') cleanFilters[key] = value;
    });
    return requisitarTodasAsPaginas({ url: '/empregados', params: cleanFilters, options });
  },
  
  // Endpoint enxuto da grade: não transfere conta, endereço, telefones e demais
  // campos do cadastro completo para cada linha.
  buscarResumoLista: async (page = 0, filters = {}, options = {}) => {
    const cleanFilters = {};
    Object.keys(filters).forEach((key) => {
      const value = filters[key];
      if (Array.isArray(value)) {
        if (value.length > 0) {
          cleanFilters[key] = key === 'contratoIds'
            ? value.map(String).filter(Boolean).join(',')
            : value;
        }
      } else if (value !== undefined && value !== null && value !== '') {
        cleanFilters[key] = value;
      }
    });

    return requisitar({
      method: 'GET',
      url: '/empregados/resumo',
      params: { page, size: CONFIG.PAGINATION_DEFAULT_SIZE, ...cleanFilters },
      retries: 1,
      ...options,
    });
  },

  // Dados mínimos utilizados somente pela listagem de colaboradores.
  buscarDadosAuxiliaresLista: async (options = {}) => {
    const [resLotacoes, resContratos] = await Promise.all([
      requisitar({ method: 'GET', url: '/lotacoes/opcoes', ...options }),
      requisitar({ method: 'GET', url: '/contratos/opcoes', ...options }),
    ]);

    return {
      lotacoes: extrairLista(resLotacoes),
      contratos: extrairLista(resContratos),
    };
  },

  buscarPaginadosParaFrequencia: async (params, options = {}) => requisitar({ method: 'GET', url: '/empregados/filtrados-frequencia', params, ...options }),
  buscarTodosParaFrequencia: async (params, options = {}) => requisitarTodasAsPaginas({
    url: '/empregados/filtrados-frequencia', params, options, otimizarContagem: true,
  }),

  buscarPorId: async (id, options = {}) => requisitar({ method: 'GET', url: `/empregados/${id}`, ...options }),
  
  // Empregado é uma projeção somente leitura. Escritas pertencem a Pessoa e Vínculo Contratual.
  // Não existem shims POST/PUT/DELETE de /empregados no cliente canônico.

  buscarDadosAuxiliares: async (options = {}) => {
    // O Java congelado não expõe catálogos bancários. Falhas dos catálogos
    // canônicos devem chegar à tela; não significam listas comprovadamente vazias.
    const [statuses, lotacoes, cargos, contratos] = await Promise.all([
      requisitar({ method: 'GET', url: '/status-empregados', ...options }),
      requisitar({ method: 'GET', url: '/lotacoes', ...options }),
      requisitarTodasAsPaginas({ url: '/cargos', options }),
      requisitar({ method: 'GET', url: '/contratos', ...options }),
    ]);
    return {
      statuses: extrairLista(statuses),
      lotacoes: extrairLista(lotacoes),
      cargos,
      contratos: extrairLista(contratos),
    };
  },

  // A frequência usa apenas dados do escopo operacional. Evita chamadas a
  // cadastros administrativos que o usuário operacional não deve consultar.
  buscarDadosAuxiliaresFrequencia: async (options = {}) => {
    const [resLotacoes, resContratos] = await Promise.all([
      requisitar({ method: 'GET', url: '/lotacoes', params: { size: CONFIG.PAGINATION_MAX_SIZE }, ...options }),
      requisitar({ method: 'GET', url: '/contratos', ...options }),
    ]);
    return {
      lotacoes: extrairLista(resLotacoes),
      contratos: extrairLista(resContratos),
    };
  },

  buscarHistorico: async (id, options = {}) => requisitar({ method: 'GET', url: `/historicos-empregados/empregado/${id}`, ...options }),
  
  buscarHistoricoContrato: async (id, options = {}) => requisitar({ method: 'GET', url: `/empregados/${id}/historico-contratos`, ...options }),

  buscarRelatorioAbsenteismo: async (params, options = {}) => requisitar({ method: 'GET', url: '/relatorios/absenteismo', params, ...options })
};
