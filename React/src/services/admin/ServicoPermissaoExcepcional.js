import { extrairLista, requisitar } from '../../core/http';

export const TIPOS_PERMISSAO_EXCEPCIONAL = Object.freeze([
  'CORRECAO_IDENTIFICADOR_CONTRATO',
  'REDUCAO_CONTRATUAL_EXCEPCIONAL',
  'TRANSFERENCIA_GESTAO_LOCAL',
  'INCORPORACAO_PROVA_HISTORICA',
  'ACESSO_DADOS_PESSOAIS_RESTRITOS',
  'INTERVENCAO_NEGOCIAL_TECNICA',
]);

export const ServicoPermissaoExcepcional = Object.freeze({
  listar: async (options = {}) => extrairLista(await requisitar({
    method: 'GET', url: '/permissoes-excepcionais', ...options,
  })),

  conceder: async (payload, options = {}) => requisitar({
    method: 'POST',
    url: '/permissoes-excepcionais',
    data: payload,
    retries: 0,
    ...options,
  }),

  revogar: async (id, motivo, options = {}) => requisitar({
    method: 'POST',
    url: `/permissoes-excepcionais/${id}/revogar`,
    data: { motivo },
    retries: 0,
    ...options,
  }),

  listarUsos: async (id, options = {}) => extrairLista(await requisitar({
    method: 'GET', url: `/permissoes-excepcionais/${id}/usos`, ...options,
  })),
});
