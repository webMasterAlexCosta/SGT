import { exigirIdentificadorRota, type ResourceIdentifierInput } from '../../core/http/identificadores';
import type * as DTO from '../../contracts/java';
import type { Page } from '../../contracts/shared';
import type { RequestOptions, QueryParams } from '../../core/http/types';
import { extrairLista, requisitar } from "../../core/http";

const endpoint = "/fornecedores";

export const ServicoEmpresaContratada = {
  listar: async (options: RequestOptions = {}) =>
    extrairLista(await requisitar<Array<DTO.FornecedorDTO>>({ method: "GET", url: endpoint, ...options })),
  criar: (data: ({ razaoSocial: string | undefined; cnpj: string | undefined; email: string | undefined; }), options: RequestOptions = {}) =>
    requisitar<DTO.FornecedorDTO>({ method: "POST", url: endpoint, data, retries: 0, ...options }),
  atualizar: async (id: ResourceIdentifierInput, data: ({ razaoSocial: string; cnpj: string; email: string | null; ativo: boolean; }), options: RequestOptions = {}) =>
    { id = exigirIdentificadorRota(id); return requisitar<DTO.FornecedorDTO>({ method: "PUT", url: `${endpoint}/${id}`, data, retries: 0, ...options }); },
  vincularAoContrato: async (contratoId: ResourceIdentifierInput, empresaId: ResourceIdentifierInput, options: RequestOptions = {}) =>
    { contratoId = exigirIdentificadorRota(contratoId); empresaId = exigirIdentificadorRota(empresaId); return requisitar<DTO.ContratoFornecedorDTO>({ method: "PUT", url: `/contratos/${contratoId}/fornecedor/${empresaId}`, retries: 0, ...options }); },
};
