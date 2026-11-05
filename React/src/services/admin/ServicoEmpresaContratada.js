import { extrairLista, requisitar } from "../../core/http";

const endpoint = "/fornecedores";

export const ServicoEmpresaContratada = {
  listar: async (options = {}) =>
    extrairLista(await requisitar({ method: "GET", url: endpoint, ...options })),
  criar: (data, options = {}) =>
    requisitar({ method: "POST", url: endpoint, data, retries: 0, ...options }),
  atualizar: (id, data, options = {}) =>
    requisitar({ method: "PUT", url: `${endpoint}/${id}`, data, retries: 0, ...options }),
  vincularAoContrato: (contratoId, empresaId, options = {}) =>
    requisitar({ method: "PUT", url: `/contratos/${contratoId}/fornecedor/${empresaId}`, retries: 0, ...options }),
};
