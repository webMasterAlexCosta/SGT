import type * as Input from '../../contracts/java-requests';
import { exigirIdentificadorRota, type ResourceIdentifierInput } from '../../core/http/identificadores';
import type * as DTO from '../../contracts/java';
import type { Page } from '../../contracts/shared';
import type { RequestOptions, QueryParams } from '../../core/http/types';
import { requisitar, extrairLista } from '../../core/http';

export const ServicoJornada = {
  listar: async (options: RequestOptions = {}) => extrairLista(await requisitar<Array<DTO.JornadaDTO>>({ method: 'GET', url: '/jornadas', ...options })),
  listarTodos: async (options: RequestOptions = {}) =>
    extrairLista(await requisitar<Array<DTO.JornadaDTO>>({ method: 'GET', url: '/jornadas', ...options })),
  salvar: async (payload: Input.RequestJornadaDTO, options: RequestOptions = {}) => requisitar({
    method: payload.id ? 'PUT' : 'POST', url: payload.id ? `/jornadas/${payload.id}` : '/jornadas', data: payload, successMessage: payload.id ? 'Turno atualizado.' : 'Turno criado.', ...options
  }),
  excluir: async (id: ResourceIdentifierInput, options: RequestOptions = {}) => { id = exigirIdentificadorRota(id); return requisitar<null>({ method: 'DELETE', url: `/jornadas/${id}`, successMessage: 'Turno removido.', ...options }); },

  buscarRegras: async (jornadaId: ResourceIdentifierInput, options: RequestOptions = {}) => { jornadaId = exigirIdentificadorRota(jornadaId); return requisitar<Array<DTO.RegraAdicionalDTO>>({ method: 'GET', url: `/jornadas/${jornadaId}/regras-adicionais`, ...options }); },
  adicionarRegra: async (jornadaId: ResourceIdentifierInput, payload: Input.RequestRegraAdicionalDTO, options: RequestOptions = {}) => { jornadaId = exigirIdentificadorRota(jornadaId); return requisitar<DTO.RegraAdicionalDTO>({ method: 'POST', url: `/jornadas/${jornadaId}/regras-adicionais`, data: payload, successMessage: 'Regra adicionada.', ...options }); },
  atualizarRegra: async (regraId: ResourceIdentifierInput, payload: Input.RequestRegraAdicionalDTO, options: RequestOptions = {}) => { regraId = exigirIdentificadorRota(regraId); return requisitar<DTO.RegraAdicionalDTO>({ method: 'PUT', url: `/regras-adicionais/${regraId}`, data: payload, successMessage: 'Regra atualizada.', ...options }); },
  excluirRegra: async (regraId: ResourceIdentifierInput, options: RequestOptions = {}) => { regraId = exigirIdentificadorRota(regraId); return requisitar<null>({ method: 'DELETE', url: `/regras-adicionais/${regraId}`, successMessage: 'Regra removida.', ...options }); }
};
