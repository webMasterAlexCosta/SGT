import type * as Input from '../../contracts/java-requests';
import type * as DTO from '../../contracts/java';
import type { Page } from '../../contracts/shared';
import type { RequestOptions, QueryParams } from '../../core/http/types';
// Caminho: src/services/admin/ServicoConfiguracaoSistema.ts
import { requisitar } from '../../core/http';

export const ServicoConfiguracaoSistema = {
  buscarConfiguracoes: async (options: RequestOptions = {}) => 
    requisitar<Array<DTO.ConfiguracaoSistemaDTO>>({ method: 'GET', url: '/sistema/configuracoes', ...options }),
  
  salvarConfiguracoes: async (payload: Array<Input.RequestConfiguracaoSistemaDTO>, options: RequestOptions = {}) => 
    requisitar<DTO.MensagemApiDTO>({ method: 'POST', url: '/sistema/configuracoes', data: payload, ...options })
};