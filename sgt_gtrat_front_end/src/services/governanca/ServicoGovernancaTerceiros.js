import { extrairLista, requisitar } from "../../core/http";

const base = "/governanca-terceiros";

export const ServicoGovernancaTerceiros = {
  resumo: (options = {}) => requisitar({ method: "GET", url: `${base}/resumo`, ...options }),
  fornecedores: async (options = {}) => extrairLista(await requisitar({ method: "GET", url: `${base}/fornecedores`, ...options })),
  criarFornecedor: (data, options = {}) => requisitar({ method: "POST", url: `${base}/fornecedores`, data, ...options }),
  atualizarFornecedor: (id, data, options = {}) => requisitar({ method: "PUT", url: `${base}/fornecedores/${id}`, data, ...options }),
  vincularFornecedor: (contratoId, fornecedorId, options = {}) => requisitar({ method: "PUT", url: `${base}/contratos/${contratoId}/fornecedor/${fornecedorId}`, ...options }),
  tabelasTarifarias: async (contratoId, options = {}) => extrairLista(await requisitar({ method: "GET", url: `${base}/tabelas-tarifarias`, params: { contratoId }, ...options })),
  criarTabelaTarifaria: (data, options = {}) => requisitar({ method: "POST", url: `${base}/tabelas-tarifarias`, data, ...options }),
  processos: async (options = {}) => extrairLista(await requisitar({ method: "GET", url: `${base}/processos`, ...options })),
  criarProcesso: (data, options = {}) => requisitar({ method: "POST", url: `${base}/processos`, data, ...options }),
  atualizarProcesso: (id, status, options = {}) => requisitar({ method: "PATCH", url: `${base}/processos/${id}/status`, data: { status }, ...options }),
  folhasHoras: async (options = {}) => extrairLista(await requisitar({ method: "GET", url: `${base}/folhas-horas`, ...options })),
  gerarFolhaHoras: (data, options = {}) => requisitar({ method: "POST", url: `${base}/folhas-horas/gerar`, data, ...options }),
  submeterFolhaHoras: (id, options = {}) => requisitar({ method: "POST", url: `${base}/folhas-horas/${id}/submeter`, ...options }),
  aprovarFolhaHoras: (id, options = {}) => requisitar({ method: "POST", url: `${base}/folhas-horas/${id}/aprovar`, ...options }),
  rejeitarFolhaHoras: (id, motivo, options = {}) => requisitar({ method: "POST", url: `${base}/folhas-horas/${id}/rejeitar`, data: { motivo }, ...options }),
  faturas: async (options = {}) => extrairLista(await requisitar({ method: "GET", url: `${base}/faturas`, ...options })),
  gerarFatura: (folhaHorasId, glosas = 0, options = {}) => requisitar({ method: "POST", url: `${base}/faturas/gerar/${folhaHorasId}`, data: { glosas }, ...options }),
  atualizarFatura: (id, status, numeroDocumento = "", options = {}) => requisitar({ method: "PATCH", url: `${base}/faturas/${id}/status`, data: { status, numeroDocumento }, ...options }),
  riscos: async (options = {}) => extrairLista(await requisitar({ method: "GET", url: `${base}/riscos`, ...options })),
  avaliarRisco: (data, options = {}) => requisitar({ method: "POST", url: `${base}/riscos`, data, ...options }),
};
