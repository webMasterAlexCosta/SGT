import { requisitar } from '../../core/http';

export const ServicoAuditoria = {
  buscar: async (params, options = {}) => requisitar({ method: 'GET', url: '/registros-auditoria', params, ...options }),
  buscarPorId: async (id, options = {}) => requisitar({ method: 'GET', url: `/registros-auditoria/${id}`, ...options })
};