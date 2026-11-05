import { requisitar, extrairLista } from '../../core/http';

export const ServicoJornada = {
  listar: async (options = {}) => extrairLista(await requisitar({ method: 'GET', url: '/jornadas', ...options })),
  listarTodos: async (options = {}) =>
    extrairLista(await requisitar({ method: 'GET', url: '/jornadas', ...options })),
  salvar: async (payload, options = {}) => requisitar({
    method: payload.id ? 'PUT' : 'POST', url: payload.id ? `/jornadas/${payload.id}` : '/jornadas', data: payload, successMessage: payload.id ? 'Turno atualizado.' : 'Turno criado.', ...options
  }),
  excluir: async (id, options = {}) => requisitar({ method: 'DELETE', url: `/jornadas/${id}`, successMessage: 'Turno removido.', ...options }),

  buscarRegras: async (jornadaId, options = {}) => requisitar({ method: 'GET', url: `/jornadas/${jornadaId}/regras-adicionais`, ...options }),
  adicionarRegra: async (jornadaId, payload, options = {}) => requisitar({ method: 'POST', url: `/jornadas/${jornadaId}/regras-adicionais`, data: payload, successMessage: 'Regra adicionada.', ...options }),
  atualizarRegra: async (regraId, payload, options = {}) => requisitar({ method: 'PUT', url: `/regras-adicionais/${regraId}`, data: payload, successMessage: 'Regra atualizada.', ...options }),
  excluirRegra: async (regraId, options = {}) => requisitar({ method: 'DELETE', url: `/regras-adicionais/${regraId}`, successMessage: 'Regra removida.', ...options })
};
