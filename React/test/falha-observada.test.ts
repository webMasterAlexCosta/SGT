import { expect, it } from 'vitest';
import { observarFalha } from '../src/core/http/falhaObservada';
it('refina Error nativo preservando identidade e pilha', () => {
  const original = new TypeError('Campo obrigatório');
  const falha = observarFalha(original);
  expect(falha).toBe(original);
  expect(falha.message).toBe('Campo obrigatório');
  expect(falha.stack).toBe(original.stack);
});
it('preserva os campos de cancelamento e correlação do erro HTTP', () => {
  const original = Object.assign(new Error('Cancelado'), { canceled: true, silencioso: true, status: 0, code: 'REQUEST_CANCELED', tipo: 'CANCELED', requestId: 'req-sintetico' });
  expect(observarFalha(original)).toBe(original);
  expect(observarFalha(original)).toMatchObject({ canceled: true, silencioso: true, status: 0, requestId: 'req-sintetico' });
});
it('preserva o corpo opaco e a resposta HTTP sem promover seus campos', () => {
  const data = { message: 'Aguarde', lockout: { remainingSeconds: 12 } };
  const original = { status: 429, code: 'RATE_LIMIT', message: 'Aguarde', data, response: { status: 429, data } };
  const falha = observarFalha(original);
  expect(falha).toBe(original);
  expect(falha.data).toBe(data);
  expect(falha.response?.data).toBe(data);
});
it('preserva zero, nulos permitidos e progresso de envio parcial', () => {
  const original = Object.assign(new Error('Lote recusado'), { status: 409, code: null, retryAfterMs: 0, requestId: null, progressoFrequencia: { totalSalvo: 0, total: 7, lotesConcluidos: 0, loteFalhou: 1, totalLotes: 2 } });
  expect(observarFalha(original)).toBe(original);
  expect(observarFalha(original).progressoFrequencia?.totalSalvo).toBe(0);
});
it('não apresenta valores de tipos incorretos como campos de erro válidos', () => {
  const falha = observarFalha({ message: ['mensagem'], status: '500', canceled: 'false', code: 2, progressoFrequencia: { totalSalvo: '3' } });
  expect(falha.message).toBeUndefined(); expect(falha.status).toBeUndefined(); expect(falha.canceled).toBeUndefined(); expect(falha.code).toBeUndefined(); expect(falha.progressoFrequencia).toBeUndefined();
});
it('aceita rejeições primitivas e ausência sem lançar outra exceção', () => {
  for (const original of [null, undefined, 'erro', 42, false]) expect(observarFalha(original)).toEqual({});
});
