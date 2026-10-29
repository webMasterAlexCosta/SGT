import { extrairLista, requisitar } from "../../core/http.js";

const endpoint = "/motivos-desligamento";

export const ServicoMotivoDesligamento = Object.freeze({
  listar: async (options = {}) => extrairLista(await requisitar({ method: "GET", url: endpoint, ...options })),
  buscarPorId: async (id, options = {}) => requisitar({ method: "GET", url: `${endpoint}/${id}`, ...options }),
  criar: async (dados, options = {}) => requisitar({
    method: "POST",
    url: endpoint,
    data: dados,
    retries: 0,
    successMessage: "Motivo de desligamento criado.",
    confirmacaoSenha: {
      acao: "Criar motivo de desligamento",
      entidade: "Catálogo da Gestão Local",
      consequencia: "O novo motivo ficará disponível aos vínculos do escopo autorizado.",
    },
    ...options,
  }),
  atualizar: async (id, dados, options = {}) => requisitar({
    method: "PUT",
    url: `${endpoint}/${id}`,
    data: dados,
    retries: 0,
    successMessage: "Motivo de desligamento atualizado.",
    confirmacaoSenha: {
      acao: "Atualizar motivo de desligamento",
      entidade: `Motivo ${id}`,
      consequencia: "A disponibilidade futura do catálogo será alterada sem reescrever vínculos históricos.",
    },
    ...options,
  }),
});
