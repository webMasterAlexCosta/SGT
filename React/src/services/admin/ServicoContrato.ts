import type * as Input from '../../contracts/java-requests';
import { exigirIdentificadorRota, type ResourceIdentifierInput } from '../../core/http/identificadores';
import type * as DTO from '../../contracts/java';
import type { Page } from '../../contracts/shared';
import type { RequestOptions, QueryParams } from '../../core/http/types';
import { extrairLista, requisitar } from '../../core/http';

const endpoint = '/contratos';

const mutacao = <T,>(method: string, url: string, data: unknown, options: RequestOptions = {}, successMessage?: string): Promise<T> => requisitar<T>({
  method,
  url,
  retries: 0,
  ...(data !== undefined ? { data } : {}),
  ...(successMessage ? { successMessage } : {}),
  ...options,
});

export const ServicoContrato = Object.freeze({
  listarTodos: async (options: RequestOptions = {}) =>
    extrairLista(await requisitar<Array<DTO.ContratoDTO>>({ method: 'GET', url: endpoint, ...options })),

  obterOpcoes: async (options: RequestOptions = {}) =>
    extrairLista(await requisitar<Array<DTO.ContratoOpcaoDTO>>({ method: 'GET', url: `${endpoint}/opcoes`, ...options })),

  buscarPorId: async (id: ResourceIdentifierInput, options: RequestOptions = {}) =>
    { id = exigirIdentificadorRota(id); return requisitar<DTO.ContratoDTO>({ method: 'GET', url: `${endpoint}/${id}`, ...options }); },

  listarEventos: async (id: ResourceIdentifierInput, options: RequestOptions = {}) =>
    { id = exigirIdentificadorRota(id); return extrairLista(await requisitar<Array<DTO.EventoCicloVidaContratoDTO>>({ method: 'GET', url: `${endpoint}/${id}/eventos`, ...options })); },

  listarPrecedencias: async (id: ResourceIdentifierInput, options: RequestOptions = {}) =>
    { id = exigirIdentificadorRota(id); return extrairLista(await requisitar<Array<DTO.RelacaoPrecedenciaEventoContratoDTO>>({ method: 'GET', url: `${endpoint}/${id}/eventos/precedencias`, ...options })); },

  listarVersoes: async (id: ResourceIdentifierInput, options: RequestOptions = {}) =>
    { id = exigirIdentificadorRota(id); return extrairLista(await requisitar<Array<DTO.VersaoContratoLeituraDTO>>({ method: 'GET', url: `${endpoint}/${id}/versoes`, ...options })); },

  consultarPosicaoTemporal: async (id: ResourceIdentifierInput, { data, conhecidoAte }: { data?: string; conhecidoAte?: string } = {}, options: RequestOptions = {}) =>
    { id = exigirIdentificadorRota(id); return requisitar<DTO.PosicaoTemporalContratoLeituraDTO>({
      method: 'GET',
      url: `${endpoint}/${id}/posicao-temporal`,
      params: { data, ...(conhecidoAte ? { conhecidoAte } : {}) },
      ...options,
    }); },

  criar: async (dados: Input.RequestContratoDTO, options: RequestOptions = {}): Promise<DTO.ContratoDTO> => mutacao<DTO.ContratoDTO>('POST', endpoint, dados, options, 'Rascunho de contrato criado.'),

  atualizar: async (id: ResourceIdentifierInput, dados: Input.RequestContratoDTO, options: RequestOptions = {}): Promise<DTO.ContratoDTO> =>
    { id = exigirIdentificadorRota(id); return mutacao<DTO.ContratoDTO>('PUT', `${endpoint}/${id}`, dados, options, 'Rascunho de contrato atualizado.'); },

  salvar: async (dados: Input.RequestContratoDTO, options: RequestOptions = {}) => dados?.id
    ? ServicoContrato.atualizar(dados.id, dados, options)
    : ServicoContrato.criar(dados, options),

  publicar: async (id: ResourceIdentifierInput, dados: Input.RequestPublicacaoContratoDTO, options: RequestOptions = {}): Promise<DTO.ContratoDTO> =>
    { id = exigirIdentificadorRota(id); return mutacao<DTO.ContratoDTO>('POST', `${endpoint}/${id}/publicar`, dados, options, 'Contrato publicado.'); },

  previaPublicacao: async (id: ResourceIdentifierInput, dados: Input.RequestPublicacaoContratoDTO, options: RequestOptions = {}): Promise<DTO.PreviaCicloVidaContratoDTO> =>
    { id = exigirIdentificadorRota(id); return mutacao<DTO.PreviaCicloVidaContratoDTO>('POST', `${endpoint}/${id}/previas/publicar`, dados, options); },

  previaCicloVida: async (id: ResourceIdentifierInput, operacao: string, dados: Input.RequestAcaoCicloVidaContratoDTO, options: RequestOptions = {}): Promise<DTO.PreviaCicloVidaContratoDTO> => {
  id = exigirIdentificadorRota(id);

    const operacoes = ['inativar', 'reativar', 'encerrar-antecipadamente', 'cancelar-futuro', 'anular', 'corrigir-anulacao'];
    if (!operacoes.includes(operacao)) throw new TypeError('Operação de ciclo de vida inválida.');
    return mutacao<DTO.PreviaCicloVidaContratoDTO>('POST', `${endpoint}/${id}/previas/${operacao}`, dados, options);
  },

  inativar: async (id: ResourceIdentifierInput, dados: Input.RequestAcaoCicloVidaContratoDTO, options: RequestOptions = {}): Promise<DTO.ContratoDTO> =>
    { id = exigirIdentificadorRota(id); return mutacao<DTO.ContratoDTO>('POST', `${endpoint}/${id}/inativar`, dados, options, 'Inativação administrativa registrada.'); },

  reativar: async (id: ResourceIdentifierInput, dados: Input.RequestAcaoCicloVidaContratoDTO, options: RequestOptions = {}): Promise<DTO.ContratoDTO> =>
    { id = exigirIdentificadorRota(id); return mutacao<DTO.ContratoDTO>('POST', `${endpoint}/${id}/reativar`, dados, options, 'Reativação administrativa registrada.'); },

  encerrarAntecipadamente: async (id: ResourceIdentifierInput, dados: Input.RequestAcaoCicloVidaContratoDTO, options: RequestOptions = {}): Promise<DTO.ContratoDTO> =>
    { id = exigirIdentificadorRota(id); return mutacao<DTO.ContratoDTO>('POST', `${endpoint}/${id}/encerrar-antecipadamente`, dados, options, 'Encerramento antecipado registrado.'); },

  cancelarFuturo: async (id: ResourceIdentifierInput, dados: Input.RequestAcaoCicloVidaContratoDTO, options: RequestOptions = {}): Promise<DTO.ContratoDTO> =>
    { id = exigirIdentificadorRota(id); return mutacao<DTO.ContratoDTO>('POST', `${endpoint}/${id}/cancelar-futuro`, dados, options, 'Contrato futuro cancelado.'); },

  anular: async (id: ResourceIdentifierInput, dados: Input.RequestAcaoCicloVidaContratoDTO, options: RequestOptions = {}): Promise<DTO.ContratoDTO> =>
    { id = exigirIdentificadorRota(id); return mutacao<DTO.ContratoDTO>('POST', `${endpoint}/${id}/anular`, dados, options, 'Anulação do contrato registrada.'); },

  excluir: async (id: ResourceIdentifierInput, options: RequestOptions = {}): Promise<null> =>
    { id = exigirIdentificadorRota(id); return mutacao<null>('DELETE', `${endpoint}/${id}`, undefined, options, 'Rascunho descartado.'); },
});
