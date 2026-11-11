import type * as Input from '../../contracts/java-requests';
import type { CompositionPayload, AmendmentPayload, ReallocationPayload } from './ServicoDominioAlteracoesContrato';
import { exigirIdentificadorRota, type ResourceIdentifierInput } from '../../core/http/identificadores';
import type * as DTO from '../../contracts/java';
import type { Page } from '../../contracts/shared';
import type { RequestOptions, QueryParams } from '../../core/http/types';
import { extrairLista, requisitar } from '../../core/http';

const base = (contratoId: ResourceIdentifierInput) => { contratoId = exigirIdentificadorRota(contratoId); return `/contratos/${contratoId}`; };

const mutacao = <T,>(method: string, url: string, data: unknown, options: RequestOptions = {}, successMessage?: string): Promise<T> => requisitar<T>({
  method,
  url,
  ...(data !== undefined ? { data } : {}),
  ...(successMessage ? { successMessage } : {}),
  ...options,
});

export const ServicoAlteracoesContrato = Object.freeze({
  listarComposicoes: async (contratoId: ResourceIdentifierInput, data: (string), options: RequestOptions = {}) =>
    { contratoId = exigirIdentificadorRota(contratoId); return extrairLista(await requisitar<Array<DTO.ComposicaoContratualLeituraDTO>>({
      method: 'GET',
      url: `${base(contratoId)}/composicoes`,
      params: data ? { data } : undefined,
      ...options,
    })); },

  listarExcedentes: async (contratoId: ResourceIdentifierInput, data: (string), options: RequestOptions = {}) =>
    { contratoId = exigirIdentificadorRota(contratoId); return extrairLista(await requisitar<Array<DTO.ExcedenteComposicaoDTO>>({
      method: 'GET',
      url: `${base(contratoId)}/composicoes/excedentes`,
      params: data ? { data } : undefined,
      ...options,
    })); },

  criarComposicao: async (contratoId: ResourceIdentifierInput, dados: Input.RequestComposicaoContratualDTO, options: RequestOptions = {}) =>
    { contratoId = exigirIdentificadorRota(contratoId); return mutacao<DTO.ComposicaoContratualDTO>('POST', `${base(contratoId)}/composicoes`, dados, options, 'Composição adicionada ao rascunho.'); },

  atualizarComposicao: async (contratoId: ResourceIdentifierInput, composicaoId: ResourceIdentifierInput, dados: Input.RequestComposicaoContratualDTO, options: RequestOptions = {}) =>
    { contratoId = exigirIdentificadorRota(contratoId); composicaoId = exigirIdentificadorRota(composicaoId); return mutacao<DTO.ComposicaoContratualDTO>('PUT', `${base(contratoId)}/composicoes/${composicaoId}`, dados, options, 'Composição do rascunho atualizada.'); },

  excluirComposicao: async (contratoId: ResourceIdentifierInput, composicaoId: ResourceIdentifierInput, options: RequestOptions = {}) =>
    { contratoId = exigirIdentificadorRota(contratoId); composicaoId = exigirIdentificadorRota(composicaoId); return mutacao<null>('DELETE', `${base(contratoId)}/composicoes/${composicaoId}`, undefined, options, 'Composição removida do rascunho.'); },

  listarAditamentos: async (contratoId: ResourceIdentifierInput, options: RequestOptions = {}) =>
    { contratoId = exigirIdentificadorRota(contratoId); return extrairLista(await requisitar<Array<DTO.AditamentoContratualDTO>>({ method: 'GET', url: `${base(contratoId)}/aditamentos`, ...options })); },

  previaAditamento: async (contratoId: ResourceIdentifierInput, dados: Input.RequestAditamentoContratualRequisicao, options: RequestOptions = {}) =>
    { contratoId = exigirIdentificadorRota(contratoId); return mutacao<DTO.PreviaAditamentoContratualDTO>('POST', `${base(contratoId)}/aditamentos/previa`, dados, options); },

  registrarAditamento: async (contratoId: ResourceIdentifierInput, dados: Input.RequestAditamentoContratualRequisicao, options: RequestOptions = {}) =>
    { contratoId = exigirIdentificadorRota(contratoId); return mutacao<DTO.AditamentoContratualDTO>('POST', `${base(contratoId)}/aditamentos`, dados, options, 'Aditamento registrado.'); },

  retificarAditamento: async (contratoId: ResourceIdentifierInput, aditamentoId: ResourceIdentifierInput, dados: Input.RequestAditamentoContratualRequisicao, options: RequestOptions = {}) =>
    { contratoId = exigirIdentificadorRota(contratoId); aditamentoId = exigirIdentificadorRota(aditamentoId); return mutacao<DTO.AditamentoContratualDTO>('POST', `${base(contratoId)}/aditamentos/${aditamentoId}/retificar`, dados, options, 'Retificação do aditamento registrada.'); },

  cancelarAditamento: async (contratoId: ResourceIdentifierInput, aditamentoId: ResourceIdentifierInput, dados: Input.RequestAcaoCicloVidaContratoDTO, options: RequestOptions = {}) =>
    { contratoId = exigirIdentificadorRota(contratoId); aditamentoId = exigirIdentificadorRota(aditamentoId); return mutacao<DTO.AditamentoContratualDTO>('POST', `${base(contratoId)}/aditamentos/${aditamentoId}/cancelar`, dados, options, 'Programação do aditamento cancelada.'); },

  listarRemanejamentos: async (contratoId: ResourceIdentifierInput, options: RequestOptions = {}) =>
    { contratoId = exigirIdentificadorRota(contratoId); return extrairLista(await requisitar<Array<DTO.RemanejamentoContratualDTO>>({ method: 'GET', url: `${base(contratoId)}/remanejamentos`, ...options })); },

  previaRemanejamento: async (contratoId: ResourceIdentifierInput, dados: Input.RequestRemanejamentoContratualRequisicao, options: RequestOptions = {}) =>
    { contratoId = exigirIdentificadorRota(contratoId); return mutacao<DTO.PreviaRemanejamentoContratualDTO>('POST', `${base(contratoId)}/remanejamentos/previa`, dados, options); },

  registrarRemanejamento: async (contratoId: ResourceIdentifierInput, dados: Input.RequestRemanejamentoContratualRequisicao, options: RequestOptions = {}) =>
    { contratoId = exigirIdentificadorRota(contratoId); return mutacao<DTO.RemanejamentoContratualDTO>('POST', `${base(contratoId)}/remanejamentos`, dados, options, 'Remanejamento registrado.'); },

  retificarRemanejamento: async (contratoId: ResourceIdentifierInput, remanejamentoId: ResourceIdentifierInput, dados: Input.RequestRemanejamentoContratualRequisicao, options: RequestOptions = {}) =>
    { contratoId = exigirIdentificadorRota(contratoId); remanejamentoId = exigirIdentificadorRota(remanejamentoId); return mutacao<DTO.RemanejamentoContratualDTO>('POST', `${base(contratoId)}/remanejamentos/${remanejamentoId}/retificar`, dados, options, 'Retificação do remanejamento registrada.'); },

  cancelarRemanejamento: async (contratoId: ResourceIdentifierInput, remanejamentoId: ResourceIdentifierInput, dados: Input.RequestAcaoCicloVidaContratoDTO, options: RequestOptions = {}) =>
    { contratoId = exigirIdentificadorRota(contratoId); remanejamentoId = exigirIdentificadorRota(remanejamentoId); return mutacao<DTO.RemanejamentoContratualDTO>('POST', `${base(contratoId)}/remanejamentos/${remanejamentoId}/cancelar`, dados, options, 'Programação do remanejamento cancelada.'); },
});
