import { requisitar } from '../../core/http';

export const ServicoPerfil = {
  buscarStatusSegundoFator: async (options = {}) => requisitar({
    method: 'GET',
    url: '/perfil/2fa/status',
    silentError: true,
    ...options,
  }),
  configurarSegundoFator: async (currentPassword) => requisitar({
    method: 'POST', url: '/perfil/2fa/configurar', data: { currentPassword },
  }),
  ativarSegundoFator: async (token) => requisitar({
    method: 'POST', url: '/perfil/2fa/ativar', data: { token },
  }),
  desativarSegundoFator: async (currentPassword, token) => requisitar({
    method: 'DELETE', url: '/perfil/2fa', data: { currentPassword, token },
  }),
  validarSenha: async (password, options = {}) => requisitar({
    method: 'POST', url: '/perfil/validar-senha', data: { password }, ...options,
  }),
  alterarSenha: async (currentPassword, newPassword, token) => requisitar({
    method: 'POST',
    url: '/perfil/alterar-senha',
    data: {
      currentPassword,
      newPassword,
      ...(token ? { token } : {}),
    },
    successMessage: null,
  }),
  enviarAvatar: async (file) => {
    const formData = new FormData();
    formData.append('file', file);
    return requisitar({
      method: 'POST',
      url: '/perfil/avatar',
      data: formData,
    });
  },
};
