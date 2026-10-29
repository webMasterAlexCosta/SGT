// Caminho: src/services/admin/ServicoConfiguracaoSistema.js
import { requisitar } from '../../core/http';

export const ServicoConfiguracaoSistema = {
  buscarConfiguracoes: async (options = {}) => 
    requisitar({ method: 'GET', url: '/sistema/configuracoes', ...options }),
  
  salvarConfiguracoes: async (payload, options = {}) => 
    requisitar({ method: 'POST', url: '/sistema/configuracoes', data: payload, ...options })
};