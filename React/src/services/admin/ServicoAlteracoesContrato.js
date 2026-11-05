import { extrairLista, requisitar } from '../../core/http';

const base = (contratoId) => `/contratos/${contratoId}`;

const mutacao = (method, url, data, options = {}, successMessage) => requisitar({
  method,
  url,
  ...(data !== undefined ? { data } : {}),
  ...(successMessage ? { successMessage } : {}),
  ...options,
});

export const ServicoAlteracoesContrato = Object.freeze({
  listarComposicoes: async (contratoId, data, options = {}) =>
    extrairLista(await requisitar({
      method: 'GET',
      url: `${base(contratoId)}/composicoes`,
      params: data ? { data } : undefined,
      ...options,
    })),

  listarExcedentes: async (contratoId, data, options = {}) =>
    extrairLista(await requisitar({
      method: 'GET',
      url: `${base(contratoId)}/composicoes/excedentes`,
      params: data ? { data } : undefined,
      ...options,
    })),

  criarComposicao: async (contratoId, dados, options = {}) =>
    mutacao('POST', `${base(contratoId)}/composicoes`, dados, options, 'Composição adicionada ao rascunho.'),

  atualizarComposicao: async (contratoId, composicaoId, dados, options = {}) =>
    mutacao('PUT', `${base(contratoId)}/composicoes/${composicaoId}`, dados, options, 'Composição do rascunho atualizada.'),

  excluirComposicao: async (contratoId, composicaoId, options = {}) =>
    mutacao('DELETE', `${base(contratoId)}/composicoes/${composicaoId}`, undefined, options, 'Composição removida do rascunho.'),

  listarAditamentos: async (contratoId, options = {}) =>
    extrairLista(await requisitar({ method: 'GET', url: `${base(contratoId)}/aditamentos`, ...options })),

  previaAditamento: async (contratoId, dados, options = {}) =>
    mutacao('POST', `${base(contratoId)}/aditamentos/previa`, dados, options),

  registrarAditamento: async (contratoId, dados, options = {}) =>
    mutacao('POST', `${base(contratoId)}/aditamentos`, dados, options, 'Aditamento registrado.'),

  retificarAditamento: async (contratoId, aditamentoId, dados, options = {}) =>
    mutacao('POST', `${base(contratoId)}/aditamentos/${aditamentoId}/retificar`, dados, options, 'Retificação do aditamento registrada.'),

  cancelarAditamento: async (contratoId, aditamentoId, dados, options = {}) =>
    mutacao('POST', `${base(contratoId)}/aditamentos/${aditamentoId}/cancelar`, dados, options, 'Programação do aditamento cancelada.'),

  listarRemanejamentos: async (contratoId, options = {}) =>
    extrairLista(await requisitar({ method: 'GET', url: `${base(contratoId)}/remanejamentos`, ...options })),

  previaRemanejamento: async (contratoId, dados, options = {}) =>
    mutacao('POST', `${base(contratoId)}/remanejamentos/previa`, dados, options),

  registrarRemanejamento: async (contratoId, dados, options = {}) =>
    mutacao('POST', `${base(contratoId)}/remanejamentos`, dados, options, 'Remanejamento registrado.'),

  retificarRemanejamento: async (contratoId, remanejamentoId, dados, options = {}) =>
    mutacao('POST', `${base(contratoId)}/remanejamentos/${remanejamentoId}/retificar`, dados, options, 'Retificação do remanejamento registrada.'),

  cancelarRemanejamento: async (contratoId, remanejamentoId, dados, options = {}) =>
    mutacao('POST', `${base(contratoId)}/remanejamentos/${remanejamentoId}/cancelar`, dados, options, 'Programação do remanejamento cancelada.'),
});
