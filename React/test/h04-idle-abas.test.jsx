// Hook de atividade, evento, provider e transporte reais com adapter controlado.
// O relógio e o callback do timeout são controlados; não é idle/E2E real.
import React from 'react';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { act, cleanup, render, renderHook, waitFor } from '@testing-library/react';
import { ProvedorAutenticacao, useAutenticacao } from '../src/modules/auth/context.tsx';
import { usePulsoAtividade } from '../src/hooks/usePulsoAtividade.ts';
import { CONFIG } from '../src/core/config.ts';
import { api } from '../src/core/http.ts';
import { invalidarGeracaoSessao } from '../src/core/security/geracaoSessao.ts';
import { confirmarLoginExplicito, obterIntencaoSaida } from '../src/core/security/saidaSessao.ts';
import { dispararSaida, redefinirIndicadorRedirecionamento } from '../src/core/events.ts';
import { configurarBuscadorTokenCsrf, resetarCoordenadorCsrfParaTestes } from '../src/core/http/coordenadorCsrf.ts';

const CHAVE = 'sgt_revisao_compartilhada_sessao';
const A = { id: 9891, username: 'idle-a-sintetico', perfil: 'GESTOR_LOCAL', autoridade: 'PERFIL_GESTOR_LOCAL' };
const resposta = (config, data) => ({ config, data, status: 200, statusText: 'OK', headers: {} });
const revisao = valor => {
  localStorage.setItem(CHAVE, valor); document.cookie = `${CHAVE}=${valor}; Path=/; SameSite=Strict`;
};
let contexto, originalAdapter, envios;
const Observador = () => { contexto = useAutenticacao(); return null; };
beforeEach(() => {
  localStorage.clear(); sessionStorage.clear(); revisao('idle-sessao-a'); invalidarGeracaoSessao();
  confirmarLoginExplicito(obterIntencaoSaida()); // Reset somente antes do cenário independente.
  redefinirIndicadorRedirecionamento(); resetarCoordenadorCsrfParaTestes();
  originalAdapter = api.defaults.adapter; envios = [];
  api.defaults.adapter = async config => {
    envios.push({ method: config.method, url: config.url });
    if (config.url === '/autenticacao/csrf') return resposta(config, { headerName: 'X-XSRF-TOKEN', token: 'csrf-sintetico' });
    if (config.url === '/autenticacao/sessao') return resposta(config, { authenticated: true, user: A, session: { remainingSeconds: 3600 } });
    if (config.url === '/usuarios/meu-perfil') return resposta(config, A);
    if (config.url === '/autenticacao/encerrar-sessao') return resposta(config, null);
    throw new Error(`Transporte inesperado ${config.method} ${config.url}`);
  };
  configurarBuscadorTokenCsrf(async ({ geracaoSessao }) => api.request({ method: 'GET', url: '/autenticacao/csrf',
    retries: 0, silentError: true, skipAuthLogout: true, __csrfBootstrap: true, __geracaoSessao: geracaoSessao }));
});
afterEach(() => { cleanup(); vi.restoreAllMocks(); api.defaults.adapter = originalAdapter; resetarCoordenadorCsrfParaTestes(); });
const montar = async () => {
  render(<ProvedorAutenticacao><Observador/></ProvedorAutenticacao>);
  await waitFor(() => expect(contexto.user?.id).toBe(A.id));
};
const armarIdleReal = () => {
  const setTimeoutOriginal = window.setTimeout.bind(window); let callback;
  vi.spyOn(window, 'setTimeout').mockImplementation((fn, atraso, ...args) => {
    if (atraso > CONFIG.INACTIVITY_TIMEOUT_MS - 2000 && atraso <= CONFIG.INACTIVITY_TIMEOUT_MS) callback = fn;
    return setTimeoutOriginal(fn, atraso, ...args);
  });
  const agora = Date.now();
  renderHook(() => usePulsoAtividade(A, contexto.atualizarInformacoesSessao));
  expect(callback).toBeTypeOf('function');
  return () => {
    vi.spyOn(Date, 'now').mockReturnValue(agora + CONFIG.INACTIVITY_TIMEOUT_MS + 2000);
    callback();
  };
};
const logouts = () => envios.filter(e => e.method === 'post' && e.url === '/autenticacao/encerrar-sessao');

describe('RI03: timeout real do hook e saída sem metadado de geração', () => {
  it('idle de A após troca para B, antes de storage/focus, não revoga B', async () => {
    await montar(); const vencerIdle = armarIdleReal(); revisao('idle-sessao-b');
    await act(async () => { vencerIdle(); await Promise.resolve(); });
    expect(logouts()).toEqual([]); expect(contexto.user).toBeNull();
  });
  it('disparador sem geração recusa a transição externa antes de adotar o marcador', async () => {
    await montar(); revisao('idle-sessao-b');
    await act(async () => { dispararSaida('Saída sintética', { code: 'SESSION_IDLE_TIMEOUT' }); await Promise.resolve(); });
    expect(logouts()).toEqual([]); expect(contexto.user).toBeNull();
  });
  it('idle da própria sessão atual ainda envia uma única revogação', async () => {
    await montar(); const vencerIdle = armarIdleReal();
    await act(async () => { vencerIdle(); await Promise.resolve(); });
    expect(logouts()).toHaveLength(1); expect(contexto.user).toBeNull();
  });
});
