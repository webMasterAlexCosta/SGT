import { requisitar, extrairLista } from "../../core/http";

export const ServicoOrganizacional = {
  listarSuperintendencias: async (options = {}) =>
    extrairLista(await requisitar({ method: "GET", url: "/superintendencias", ...options })),

  salvarSuperintendencia: async (item, options = {}) =>
    requisitar({
      method: item.id ? "PUT" : "POST",
      url: item.id ? `/superintendencias/${item.id}` : "/superintendencias",
      data: item,
      successMessage: item.id ? "Gestor estadual atualizado." : "Gestor estadual criado.",
      ...options,
    }),

  ativarSuperintendencia: async (id, options = {}) =>
    requisitar({
      method: "POST",
      url: `/superintendencias/${id}/ativar`,
      successMessage: "Gestor estadual reativado. Guarde a senha temporária.",
      ...options,
    }),

  excluirSuperintendencia: async (id, options = {}) =>
    requisitar({
      method: "DELETE",
      url: `/superintendencias/${id}`,
      successMessage: "Gestor estadual removido.",
      ...options,
    }),

  listarGestoesLocais: async (params = {}, options = {}) =>
    extrairLista(await requisitar({ method: "GET", url: "/gestoes-locais", params, ...options })),

  salvarGestaoLocal: async (item, options = {}) =>
    requisitar({
      method: item.id ? "PUT" : "POST",
      url: item.id ? `/gestoes-locais/${item.id}` : "/gestoes-locais",
      data: item,
      successMessage: item.id ? "Gestão Local atualizada." : "Gestão Local criada.",
      ...options,
    }),

  excluirGestaoLocal: async (id, options = {}) =>
    requisitar({
      method: "DELETE",
      url: `/gestoes-locais/${id}`,
      successMessage: "Gestão Local removida.",
      ...options,
    }),
};
