import type * as Input from '../../contracts/java-requests';
import { exigirIdentificadorRota, type ResourceIdentifierInput } from '../../core/http/identificadores';
import type * as DTO from '../../contracts/java';
import type { Page } from '../../contracts/shared';
import type { RequestOptions, QueryParams } from '../../core/http/types';
import { requisitar, extrairLista } from "../../core/http";

export const ServicoOrganizacional = {
  listarSuperintendencias: async (options: RequestOptions = {}) =>
    extrairLista(await requisitar<Array<DTO.SuperintendenciaDTO>>({ method: "GET", url: "/superintendencias", ...options })),

  salvarSuperintendencia: async (item: Input.RequestSuperintendenciaDTO, options: RequestOptions = {}) =>
    requisitar<DTO.SuperintendenciaDTO>({
      method: item.id ? "PUT" : "POST",
      url: item.id ? `/superintendencias/${item.id}` : "/superintendencias",
      data: item,
      successMessage: item.id ? "Gestor estadual atualizado." : "Gestor estadual criado.",
      ...options,
    }),

  ativarSuperintendencia: async (id: ResourceIdentifierInput, options: RequestOptions = {}) =>
    { id = exigirIdentificadorRota(id); return requisitar<DTO.AtivacaoUsuarioAplicacaoDTO>({
      method: "POST",
      url: `/superintendencias/${id}/ativar`,
      successMessage: "Gestor estadual reativado. Guarde a senha temporária.",
      ...options,
    }); },

  excluirSuperintendencia: async (id: ResourceIdentifierInput, options: RequestOptions = {}) =>
    { id = exigirIdentificadorRota(id); return requisitar<null>({
      method: "DELETE",
      url: `/superintendencias/${id}`,
      successMessage: "Gestor estadual removido.",
      ...options,
    }); },

  listarGestoesLocais: async (params: QueryParams = {}, options: RequestOptions = {}) =>
    extrairLista(await requisitar<Array<DTO.GestaoLocalDTO>>({ method: "GET", url: "/gestoes-locais", params, ...options })),

  salvarGestaoLocal: async (item: Input.RequestGestaoLocalDTO, options: RequestOptions = {}) =>
    requisitar<DTO.GestaoLocalDTO>({
      method: item.id ? "PUT" : "POST",
      url: item.id ? `/gestoes-locais/${item.id}` : "/gestoes-locais",
      data: item,
      successMessage: item.id ? "Gestão Local atualizada." : "Gestão Local criada.",
      ...options,
    }),

  excluirGestaoLocal: async (id: ResourceIdentifierInput, options: RequestOptions = {}) =>
    { id = exigirIdentificadorRota(id); return requisitar<null>({
      method: "DELETE",
      url: `/gestoes-locais/${id}`,
      successMessage: "Gestão Local removida.",
      ...options,
    }); },
};
