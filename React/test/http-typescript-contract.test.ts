import { afterEach, expect, test, vi } from 'vitest';
import { api, requestBackEnd, requisitarEnvelope } from '../src/core/http';
import { invalidarGeracaoSessao } from '../src/core/security/geracaoSessao';
import type { ContratoDTO } from '../src/contracts/java';

const adapter = api.defaults.adapter;
afterEach(() => { api.defaults.adapter = adapter; invalidarGeracaoSessao(); vi.restoreAllMocks(); });

test('requestBackEnd entrega o DTO normalizado e o contexto conserva o envelope Java', async () => {
  const dado = { id: 23, numeroContrato: 'SINTETICO-23' };
  api.defaults.adapter = async config => ({ config, status: 200, statusText: 'OK', headers: { 'x-request-id': 'req-sintetico' }, data: {
    timestamp: '2026-11-06T00:00:00Z', status: 200, error: null, message: 'OK', path: '/api/contratos/23',
    requestId: 'req-sintetico', meta: null, data: dado,
  } });
  const dto = await requestBackEnd<ContratoDTO>({ method: 'GET', url: '/contratos/23' });
  expect(dto).toEqual(dado);
  expect(dto).not.toHaveProperty('config');
  const contexto = await requisitarEnvelope<ContratoDTO>({ method: 'GET', url: '/contratos/23' });
  expect(contexto).toMatchObject({ data: dado, statusHttp: 200, requestId: 'req-sintetico', envelope: { data: dado } });
  expect(api.defaults.withCredentials).toBe(true);
});
