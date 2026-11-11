import type * as Input from '../../contracts/java-requests';
import { exigirIdentificadorRota, type ResourceIdentifierInput } from '../../core/http/identificadores';
import type * as DTO from '../../contracts/java';
import type { Page } from '../../contracts/shared';
import type { RequestOptions, QueryParams } from '../../core/http/types';
import { requisitar, extrairLista } from '../../core/http';

export const ServicoUsuario = {
  listarTodos: async (options: RequestOptions = {}) => extrairLista(await requisitar<Array<DTO.ResumoUsuarioAplicacaoDTO>>({ method: 'GET', url: '/usuarios', ...options })),
  criar: async (payload: Input.RequestCriacaoUsuarioAplicacaoDTO, options: RequestOptions = {}) => requisitar<DTO.ResumoUsuarioAplicacaoDTO>({
    method: 'POST',
    url: '/usuarios',
    data: payload,
    successMessage: 'Usuário criado com sucesso.',
    ...options,
  }),
  atualizar: async (id: ResourceIdentifierInput, userData: Input.RequestAtualizacaoUsuarioAplicacaoDTO, options: RequestOptions = {}) => { id = exigirIdentificadorRota(id); return requisitar<DTO.MensagemApiDTO>({
    method: 'PUT',
    url: `/usuarios/${id}`,
    data: userData,
    successMessage: 'Usuário atualizado com sucesso.',
    ...options,
  }); },
  redefinirSenha: async (id: ResourceIdentifierInput, payload: Input.RequestRedefinicaoSenhaUsuarioAplicacaoDTO, options: RequestOptions = {}) => { id = exigirIdentificadorRota(id); return requisitar<DTO.ResultadoRedefinicaoSenhaDTO>({
    method: 'POST',
    url: `/usuarios/${id}/redefinir-senha`,
    data: payload,
    successMessage: 'Senha redefinida e sessões anteriores encerradas.',
    ...options,
  }); },
  ativar: async (id: ResourceIdentifierInput, options: RequestOptions = {}) => { id = exigirIdentificadorRota(id); return requisitar<DTO.AtivacaoUsuarioAplicacaoDTO>({
    method: 'POST',
    url: `/usuarios/${id}/ativar`,
    successMessage: 'Usuário ativado.',
    ...options,
  }); },
  excluir: async (id: ResourceIdentifierInput, options: RequestOptions = {}) => { id = exigirIdentificadorRota(id); return requisitar<DTO.MensagemApiDTO>({
    method: 'DELETE',
    url: `/usuarios/${id}`,
    successMessage: 'Usuário desativado.',
    ...options,
  }); },
  concederAcessoRetroativo: async (id: ResourceIdentifierInput, options: RequestOptions = {}) => { id = exigirIdentificadorRota(id); return requisitar<DTO.MensagemApiDTO>({
    method: 'POST',
    url: `/usuarios/${id}/conceder-acesso-retroativo`,
    successMessage: 'Acesso retroativo liberado.',
    ...options,
  }); },
  revogarAcessoRetroativo: async (id: ResourceIdentifierInput, options: RequestOptions = {}) => { id = exigirIdentificadorRota(id); return requisitar<DTO.MensagemApiDTO>({
    method: 'POST',
    url: `/usuarios/${id}/revogar-acesso-retroativo`,
    successMessage: 'Acesso retroativo bloqueado.',
    ...options,
  }); },
};
