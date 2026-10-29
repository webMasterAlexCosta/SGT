import { requisitar } from '../../core/http';

const limparParametros = (params = {}) => Object.fromEntries(
  Object.entries(params).filter(([, valor]) => valor !== undefined && valor !== null && valor !== ''),
);

export const ServicoPendenciaAdministrativa = Object.freeze({
  listar: async (params = {}, options = {}) => requisitar({
    method: 'GET',
    url: '/pendencias',
    params: limparParametros(params),
    ...options,
  }),

  obterResumo: async (params = {}, options = {}) => requisitar({
    method: 'GET',
    url: '/pendencias/resumo',
    params: limparParametros(params),
    ...options,
  }),

  iniciarTratamento: async (id, observacao = '', options = {}) => requisitar({
    method: 'POST',
    url: `/pendencias/${id}/iniciar-tratamento`,
    data: observacao?.trim() ? { observacao: observacao.trim() } : {},
    retries: 0,
    ...options,
  }),

  confirmarResolucao: async (id, observacao = '', options = {}) => requisitar({
    method: 'POST',
    url: `/pendencias/${id}/confirmar-resolucao`,
    data: observacao?.trim() ? { observacao: observacao.trim() } : {},
    retries: 0,
    ...options,
  }),
});
