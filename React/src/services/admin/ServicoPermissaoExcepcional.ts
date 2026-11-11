import type * as Input from '../../contracts/java-requests';
import { exigirIdentificadorRota, type ResourceIdentifierInput } from '../../core/http/identificadores';
import type * as DTO from '../../contracts/java';
import type { Page } from '../../contracts/shared';
import type { RequestOptions, QueryParams } from '../../core/http/types';
import { extrairLista, requisitar } from '../../core/http';

export const TIPOS_PERMISSAO_EXCEPCIONAL: readonly DTO.TipoPermissaoExcepcional[] = Object.freeze([
  'CORRECAO_IDENTIFICADOR_CONTRATO',
  'REDUCAO_CONTRATUAL_EXCEPCIONAL',
  'TRANSFERENCIA_GESTAO_LOCAL',
  'INCORPORACAO_PROVA_HISTORICA',
  'ACESSO_DADOS_PESSOAIS_RESTRITOS',
  'INTERVENCAO_NEGOCIAL_TECNICA',
]);

export const ServicoPermissaoExcepcional = Object.freeze({
  listar: async (options: RequestOptions = {}) => extrairLista(await requisitar<Array<DTO.PermissaoExcepcionalDTO>>({
    method: 'GET', url: '/permissoes-excepcionais', ...options,
  })),

  conceder: async (payload: Input.RequestConcessaoPermissaoExcepcionalRequisicao, options: RequestOptions = {}) => requisitar<DTO.PermissaoExcepcionalDTO>({
    method: 'POST',
    url: '/permissoes-excepcionais',
    data: payload,
    retries: 0,
    ...options,
  }),

  revogar: async (id: ResourceIdentifierInput, motivo: (string), options: RequestOptions = {}) => { id = exigirIdentificadorRota(id); return requisitar<DTO.PermissaoExcepcionalDTO>({
    method: 'POST',
    url: `/permissoes-excepcionais/${id}/revogar`,
    data: { motivo },
    retries: 0,
    ...options,
  }); },

  listarUsos: async (id: ResourceIdentifierInput, options: RequestOptions = {}) => { id = exigirIdentificadorRota(id); return extrairLista(await requisitar<Array<DTO.UsoPermissaoExcepcionalDTO>>({
    method: 'GET', url: `/permissoes-excepcionais/${id}/usos`, ...options,
  })); },
});
