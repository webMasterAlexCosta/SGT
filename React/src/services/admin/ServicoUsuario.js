import { requisitar, extrairLista } from '../../core/http';

export const ServicoUsuario = {
  listarTodos: async (options = {}) => extrairLista(await requisitar({ method: 'GET', url: '/usuarios', ...options })),
  criar: async (payload, options = {}) => requisitar({
    method: 'POST',
    url: '/usuarios',
    data: payload,
    successMessage: 'Usuário criado com sucesso.',
    ...options,
  }),
  atualizar: async (id, userData, options = {}) => requisitar({
    method: 'PUT',
    url: `/usuarios/${id}`,
    data: userData,
    successMessage: 'Usuário atualizado com sucesso.',
    ...options,
  }),
  redefinirSenha: async (id, payload, options = {}) => requisitar({
    method: 'POST',
    url: `/usuarios/${id}/redefinir-senha`,
    data: payload,
    successMessage: 'Senha redefinida e sessões anteriores encerradas.',
    ...options,
  }),
  ativar: async (id, options = {}) => requisitar({
    method: 'POST',
    url: `/usuarios/${id}/ativar`,
    successMessage: 'Usuário ativado.',
    ...options,
  }),
  excluir: async (id, options = {}) => requisitar({
    method: 'DELETE',
    url: `/usuarios/${id}`,
    successMessage: 'Usuário desativado.',
    ...options,
  }),
  concederAcessoRetroativo: async (id, options = {}) => requisitar({
    method: 'POST',
    url: `/usuarios/${id}/conceder-acesso-retroativo`,
    successMessage: 'Acesso retroativo liberado.',
    ...options,
  }),
  revogarAcessoRetroativo: async (id, options = {}) => requisitar({
    method: 'POST',
    url: `/usuarios/${id}/revogar-acesso-retroativo`,
    successMessage: 'Acesso retroativo bloqueado.',
    ...options,
  }),
};
