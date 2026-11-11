import type * as DTO from '../../contracts/java';
import type { Page } from '../../contracts/shared';
import type { RequestOptions, QueryParams } from '../../core/http/types';
import { requisitar } from '../../core/http';

export const ServicoPerfil = {
  buscarStatusSegundoFator: async (options: RequestOptions = {}) => requisitar<DTO.StatusSegundoFatorDTO>({
    method: 'GET',
    url: '/perfil/2fa/status',
    silentError: true,
    ...options,
  }),
  configurarSegundoFator: async (currentPassword: (string)) => requisitar<DTO.ConfiguracaoSegundoFatorDTO>({
    method: 'POST', url: '/perfil/2fa/configurar', data: { currentPassword },
  }),
  ativarSegundoFator: async (token: (string)) => requisitar<DTO.ResultadoSegundoFatorDTO>({
    method: 'POST', url: '/perfil/2fa/ativar', data: { token },
  }),
  desativarSegundoFator: async (currentPassword: (string), token: (string)) => requisitar<DTO.ResultadoSegundoFatorDTO>({
    method: 'DELETE', url: '/perfil/2fa', data: { currentPassword, token },
  }),
  validarSenha: async (password: string, options: RequestOptions = {}) => requisitar<DTO.ResultadoValidacaoSenhaDTO>({
    method: 'POST', url: '/perfil/validar-senha', data: { password }, ...options,
  }),
  alterarSenha: async (currentPassword: (string), newPassword: (string), token: (string | undefined)) => requisitar<DTO.ResultadoAlteracaoSenhaDTO>({
    method: 'POST',
    url: '/perfil/alterar-senha',
    data: {
      currentPassword,
      newPassword,
      ...(token ? { token } : {}),
    },
    successMessage: null,
  }),
  enviarAvatar: async (file: (File)) => {
    const formData = new FormData();
    formData.append('file', file);
    return requisitar<DTO.ResultadoEnvioAvatarDTO>({
      method: 'POST',
      url: '/perfil/avatar',
      data: formData,
    });
  },
};
