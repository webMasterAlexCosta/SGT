import type * as Input from '../../contracts/java-requests';
import { exigirIdentificadorRota, type ResourceIdentifierInput } from '../../core/http/identificadores';
import type * as DTO from '../../contracts/java';
import type { Page } from '../../contracts/shared';
import type { RequestOptions, QueryParams } from '../../core/http/types';
import { extrairLista, requisitar } from "../../core/http.ts";

const endpoint = "/motivos-desligamento";

export const ServicoMotivoDesligamento = Object.freeze({
  listar: async (options: RequestOptions = {}) => extrairLista(await requisitar<Array<DTO.MotivoDesligamentoDTO>>({ method: "GET", url: endpoint, ...options })),
  buscarPorId: async (id: ResourceIdentifierInput, options: RequestOptions = {}) => { id = exigirIdentificadorRota(id); return requisitar<DTO.MotivoDesligamentoDTO>({ method: "GET", url: `${endpoint}/${id}`, ...options }); },
  criar: async (dados: Input.RequestMotivoDesligamentoDTO, options: RequestOptions = {}) => requisitar<DTO.MotivoDesligamentoDTO>({
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
  atualizar: async (id: ResourceIdentifierInput, dados: Input.RequestMotivoDesligamentoDTO, options: RequestOptions = {}) => { id = exigirIdentificadorRota(id); return requisitar<DTO.MotivoDesligamentoDTO>({
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
  }); },
});
