import type * as Input from '../../contracts/java-requests';
import { exigirIdentificadorRota, type ResourceIdentifierInput } from '../../core/http/identificadores';
import type * as DTO from '../../contracts/java';
import type { Page } from '../../contracts/shared';
import type { RequestOptions, QueryParams } from '../../core/http/types';
import { extrairLista, extrairPagina, requisitar } from "../../core/http.ts";
import { limparParametrosPessoaVinculo } from "./ServicoDominioPessoaVinculo.ts";

const endpoint = "/pessoas";

export const ServicoPessoa = Object.freeze({
  listar: async (parametros: QueryParams = {}, options: RequestOptions = {}) => extrairPagina(await requisitar<Page<DTO.PessoaResumoDTO>>({
    method: "GET",
    url: endpoint,
    params: limparParametrosPessoaVinculo(parametros),
    retries: 1,
    ...options,
  })),

  buscarPorId: async (id: ResourceIdentifierInput, options: RequestOptions = {}) => { id = exigirIdentificadorRota(id); return requisitar<DTO.PessoaDTO>({ method: "GET", url: `${endpoint}/${id}`, ...options }); },

  buscarDadosRestritos: async (id: ResourceIdentifierInput, finalidade: (string), options: RequestOptions = {}) => { id = exigirIdentificadorRota(id); return requisitar<DTO.PessoaDTO>({
    method: "GET",
    url: `${endpoint}/${id}/dados-restritos`,
    params: { finalidade: String(finalidade || "").trim() },
    retries: 0,
    ...options,
  }); },

  atualizarDadosOpcionais: async (id: ResourceIdentifierInput, dados: Input.RequestAtualizacaoPessoaOpcionalRequisicao, options: RequestOptions = {}) => { id = exigirIdentificadorRota(id); return requisitar<DTO.PessoaDTO>({
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
  }); },

  corrigirIdentidade: async (id: ResourceIdentifierInput, dados: Input.RequestCorrecaoIdentidadePessoaRequisicao, options: RequestOptions = {}) => { id = exigirIdentificadorRota(id); return requisitar<DTO.PessoaDTO>({
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
  }); },

  listarCorrecoesIdentidade: async (id: ResourceIdentifierInput, options: RequestOptions = {}) => { id = exigirIdentificadorRota(id); return extrairLista(await requisitar<Array<DTO.CorrecaoPessoaDTO>>({
    method: "GET",
    url: `${endpoint}/${id}/correcoes-identidade`,
    ...options,
  })); },
});
