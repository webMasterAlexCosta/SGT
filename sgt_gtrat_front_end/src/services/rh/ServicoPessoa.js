import { extrairLista, extrairPagina, requisitar } from "../../core/http.js";
import { limparParametrosPessoaVinculo } from "./ServicoDominioPessoaVinculo.js";

const endpoint = "/pessoas";

export const ServicoPessoa = Object.freeze({
  listar: async (parametros = {}, options = {}) => extrairPagina(await requisitar({
    method: "GET",
    url: endpoint,
    params: limparParametrosPessoaVinculo(parametros),
    retries: 1,
    ...options,
  })),

  buscarPorId: async (id, options = {}) => requisitar({ method: "GET", url: `${endpoint}/${id}`, ...options }),

  buscarDadosRestritos: async (id, finalidade, options = {}) => requisitar({
    method: "GET",
    url: `${endpoint}/${id}/dados-restritos`,
    params: { finalidade: String(finalidade || "").trim() },
    retries: 0,
    ...options,
  }),

  atualizarDadosOpcionais: async (id, dados, options = {}) => requisitar({
    method: "PUT",
    url: `${endpoint}/${id}/dados-opcionais`,
    data: dados,
    retries: 0,
    successMessage: "Dados opcionais da pessoa atualizados.",
    confirmacaoSenha: {
      acao: "Atualizar dados opcionais",
      entidade: `Pessoa ${id}`,
      consequencia: "Os dados atuais serão alterados sem reescrever a fotografia dos eventos históricos.",
    },
    ...options,
  }),

  corrigirIdentidade: async (id, dados, options = {}) => requisitar({
    method: "POST",
    url: `${endpoint}/${id}/correcoes-identidade`,
    data: dados,
    retries: 0,
    successMessage: "Correção excepcional de identidade registrada.",
    confirmacaoSenha: {
      acao: "Registrar correção de identidade",
      entidade: `Pessoa ${id}`,
      consequencia: "A alteração ficará vinculada ao valor anterior, ao responsável e às evidências informadas.",
    },
    ...options,
  }),

  listarCorrecoesIdentidade: async (id, options = {}) => extrairLista(await requisitar({
    method: "GET",
    url: `${endpoint}/${id}/correcoes-identidade`,
    ...options,
  })),
});
