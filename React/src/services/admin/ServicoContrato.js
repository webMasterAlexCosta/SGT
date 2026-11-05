import { extrairLista, requisitar } from '../../core/http';

const endpoint = '/contratos';

const mutacao = (method, url, data, options = {}, successMessage) => requisitar({
  method,
  url,
  retries: 0,
  ...(data !== undefined ? { data } : {}),
  ...(successMessage ? { successMessage } : {}),
  ...options,
});

export const ServicoContrato = Object.freeze({
  listarTodos: async (options = {}) =>
    extrairLista(await requisitar({ method: 'GET', url: endpoint, ...options })),

  obterOpcoes: async (options = {}) =>
    extrairLista(await requisitar({ method: 'GET', url: `${endpoint}/opcoes`, ...options })),

  buscarPorId: async (id, options = {}) =>
    requisitar({ method: 'GET', url: `${endpoint}/${id}`, ...options }),

  listarEventos: async (id, options = {}) =>
    extrairLista(await requisitar({ method: 'GET', url: `${endpoint}/${id}/eventos`, ...options })),

  listarPrecedencias: async (id, options = {}) =>
    extrairLista(await requisitar({ method: 'GET', url: `${endpoint}/${id}/eventos/precedencias`, ...options })),

  listarVersoes: async (id, options = {}) =>
    extrairLista(await requisitar({ method: 'GET', url: `${endpoint}/${id}/versoes`, ...options })),

  consultarPosicaoTemporal: async (id, { data, conhecidoAte } = {}, options = {}) =>
    requisitar({
      method: 'GET',
      url: `${endpoint}/${id}/posicao-temporal`,
      params: { data, ...(conhecidoAte ? { conhecidoAte } : {}) },
      ...options,
    }),

  criar: async (dados, options = {}) => mutacao('POST', endpoint, dados, options, 'Rascunho de contrato criado.'),

  atualizar: async (id, dados, options = {}) =>
    mutacao('PUT', `${endpoint}/${id}`, dados, options, 'Rascunho de contrato atualizado.'),

  salvar: async (dados, options = {}) => dados?.id
    ? ServicoContrato.atualizar(dados.id, dados, options)
    : ServicoContrato.criar(dados, options),

  publicar: async (id, dados, options = {}) =>
    mutacao('POST', `${endpoint}/${id}/publicar`, dados, options, 'Contrato publicado.'),

  previaPublicacao: async (id, dados, options = {}) =>
    mutacao('POST', `${endpoint}/${id}/previas/publicar`, dados, options),

  previaCicloVida: async (id, operacao, dados, options = {}) => {
    const operacoes = ['inativar', 'reativar', 'encerrar-antecipadamente', 'cancelar-futuro', 'anular', 'corrigir-anulacao'];
    if (!operacoes.includes(operacao)) throw new TypeError('Operação de ciclo de vida inválida.');
    return mutacao('POST', `${endpoint}/${id}/previas/${operacao}`, dados, options);
  },

  inativar: async (id, dados, options = {}) =>
    mutacao('POST', `${endpoint}/${id}/inativar`, dados, options, 'Inativação administrativa registrada.'),

  reativar: async (id, dados, options = {}) =>
    mutacao('POST', `${endpoint}/${id}/reativar`, dados, options, 'Reativação administrativa registrada.'),

  encerrarAntecipadamente: async (id, dados, options = {}) =>
    mutacao('POST', `${endpoint}/${id}/encerrar-antecipadamente`, dados, options, 'Encerramento antecipado registrado.'),

  cancelarFuturo: async (id, dados, options = {}) =>
    mutacao('POST', `${endpoint}/${id}/cancelar-futuro`, dados, options, 'Contrato futuro cancelado.'),

  anular: async (id, dados, options = {}) =>
    mutacao('POST', `${endpoint}/${id}/anular`, dados, options, 'Anulação do contrato registrada.'),

  excluir: async (id, options = {}) =>
    mutacao('DELETE', `${endpoint}/${id}`, undefined, options, 'Rascunho descartado.'),
});
