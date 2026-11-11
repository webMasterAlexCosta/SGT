// Transporte Axios controlado: não utiliza backend, credenciais nem cookies reais.
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { waitFor } from '@testing-library/react';
import { api, requisitar } from '../src/core/http.ts';
import { obterGeracaoSessao, invalidarGeracaoSessao } from '../src/core/security/geracaoSessao.ts';
import { configurarBuscadorTokenCsrf, invalidarTokenCsrf, obterTokenCsrf, obterTokenCsrfEmMemoria, resetarCoordenadorCsrfParaTestes } from '../src/core/http/coordenadorCsrf.ts';
import { registrarSolicitadorConfirmacaoSenha, resetarSolicitadorConfirmacaoSenhaParaTestes } from '../src/core/http/coordenadorConfirmacaoSenha.ts';
import { EVENT_LOGOUT, EVENT_SHOW_TOAST, redefinirIndicadorRedirecionamento } from '../src/core/events.ts';
import { ServicoAutenticacao } from '../src/services/auth/ServicoAutenticacao.ts';
const diferida = () => { let resolve, reject; const promise = new Promise((a, b) => { resolve = a; reject = b; }); return { promise, resolve, reject }; };
const csrf = token => ({ headerName: 'X-XSRF-TOKEN', parameterName: '_csrf', token });
const ok = (config, data) => ({ config, data, status: 200, headers: {}, statusText: 'OK' });
const falha = (config, status, code, action) => ({ config, response: { status, headers: {}, data: { code, action, message: 'Mensagem sintética.' } } });
const observar = promise => promise.then(value => ({ value }), error => ({ error }));
let adapter, saidas, avisos;
const aoSair = e => saidas.push(e.detail);
const aoAviso = e => avisos.push(e.detail);
beforeEach(() => {
  adapter = api.defaults.adapter; invalidarGeracaoSessao(); resetarCoordenadorCsrfParaTestes(); resetarSolicitadorConfirmacaoSenhaParaTestes(); redefinirIndicadorRedirecionamento();
  sessionStorage.clear(); saidas = []; avisos = []; window.addEventListener(EVENT_LOGOUT, aoSair); window.addEventListener(EVENT_SHOW_TOAST, aoAviso);
  configurarBuscadorTokenCsrf(async options => api.request({ method: 'GET', url: '/autenticacao/csrf', retries: 0, silentError: true, skipAuthLogout: true, __csrfBootstrap: true, __geracaoSessao: options?.geracaoSessao }));
});
afterEach(() => {
  api.defaults.adapter = adapter; resetarSolicitadorConfirmacaoSenhaParaTestes(); invalidarTokenCsrf();
  window.removeEventListener(EVENT_LOGOUT, aoSair); window.removeEventListener(EVENT_SHOW_TOAST, aoAviso); vi.useRealTimers();
});

describe('Fronteira global do transporte HTTP', () => {
  it('captura a geração antes do primeiro microtask e não envia uma intenção criada na sessão anterior', async () => {
    const enviados = []; api.defaults.adapter = async config => { enviados.push(config.url); return ok(config, {}); };
    const resposta = observar(requisitar({ method: 'GET', url: '/leitura-sintetica', retries: 0 })); invalidarGeracaoSessao();
    expect((await resposta).error).toMatchObject({ canceled: true, silencioso: true }); expect(enviados).toEqual([]);
  });
  it('descarta sucesso antigo sem publicar dados nem toast na sessão nova', async () => {
    const antiga = diferida(); let configA;
    api.defaults.adapter = config => { configA = config; return antiga.promise; };
    const resposta = observar(requisitar({ url: '/leitura-sintetica', successMessage: 'Sucesso antigo sintético.', retries: 0 }));
    await waitFor(() => expect(configA).toBeDefined()); invalidarGeracaoSessao(); antiga.resolve(ok(configA, { pessoa: 'dado-da-sessao-a' }));
    expect((await resposta).error).toMatchObject({ canceled: true, silencioso: true }); expect(avisos).toEqual([]);
  });
  it.each([401, 403, 500])('descarta erro antigo %s sem encerrar B nem notificar', async status => {
    const antiga = diferida(); let configA; api.defaults.adapter = config => { configA = config; return antiga.promise; };
    const resposta = observar(requisitar({ url: '/leitura-sintetica', retries: 0 }));
    await waitFor(() => expect(configA).toBeDefined()); invalidarGeracaoSessao(); const geracaoB = obterGeracaoSessao();
    antiga.reject(falha(configA, status, 'SESSION_IDLE_TIMEOUT', 'LOGOUT'));
    expect((await resposta).error).toMatchObject({ canceled: true }); expect(saidas).toEqual([]); expect(avisos).toEqual([]); expect(obterGeracaoSessao()).toBe(geracaoB);
  });
  it('SESSION_IDLE_TIMEOUT encerra no primeiro erro de /autenticacao/sessao', async () => {
    api.defaults.adapter = async config => { throw falha(config, 401, 'SESSION_IDLE_TIMEOUT', 'LOGOUT'); };
    const geracao = obterGeracaoSessao();
    await expect(requisitar({ url: '/autenticacao/sessao', retries: 0 })).rejects.toMatchObject({ code: 'SESSION_IDLE_TIMEOUT' });
    expect(saidas).toHaveLength(1); expect(obterGeracaoSessao()).not.toBe(geracao);
  });
  it('403 genérico preserva a sessão e a classificação de autorização', async () => {
    api.defaults.adapter = async config => { throw falha(config, 403, 'FORBIDDEN'); };
    const geracao = obterGeracaoSessao();
    await expect(requisitar({ url: '/recurso-protegido', silentError: true, retries: 0 })).rejects.toMatchObject({ status: 403, tipo: 'AUTHORIZATION' });
    expect(saidas).toEqual([]); expect(obterGeracaoSessao()).toBe(geracao);
  });
  it('retry aguardando backoff não é enviado com a sessão nova', async () => {
    vi.useFakeTimers(); const enviados = [];
    api.defaults.adapter = async config => { enviados.push(config.url); throw falha(config, 503, 'UNAVAILABLE'); };
    const resposta = observar(requisitar({ url: '/consulta-sintetica', retries: 1, retryDelayMs: 100, silentError: true }));
    await vi.advanceTimersByTimeAsync(0); invalidarGeracaoSessao(); await vi.runAllTimersAsync();
    expect((await resposta).error).toMatchObject({ canceled: true }); expect(enviados).toHaveLength(1);
  });
  it('confirmação recebida por A não permite replay de mutação em B', async () => {
    let pedido; const confirmacao = diferida(); const envios = [];
    registrarSolicitadorConfirmacaoSenha(contexto => { pedido = contexto; return confirmacao.promise; });
    api.defaults.adapter = async config => {
      if (config.url === '/autenticacao/csrf') return ok(config, csrf('token-sintetico'));
      envios.push(config.url);
      if (!config.headers.get('X-User-Password')) throw falha(config, 428, 'PASSWORD_CONFIRMATION_REQUIRED');
      return ok(config, { gravado: true });
    };
    const resposta = observar(requisitar({ method: 'POST', url: '/mutacao-sintetica', data: {}, retries: 0 }));
    await waitFor(() => expect(pedido).toBeDefined()); invalidarGeracaoSessao();
    const replay = await observar(pedido.executar('senha-sintetica'));
    confirmacao.resolve({ finalizado: true });
    expect(replay.error).toMatchObject({ canceled: true }); expect((await resposta).error).toMatchObject({ canceled: true }); expect(envios).toHaveLength(1);
  });
  it('resposta CSRF antiga não renova token nem repete mutação em B', async () => {
    const antiga = diferida(); let mutacao; const envios = [];
    api.defaults.adapter = async config => {
      envios.push(config.url);
      if (config.url === '/autenticacao/csrf') return ok(config, csrf('token-sintetico'));
      mutacao = config; return antiga.promise;
    };
    const resposta = observar(requisitar({ method: 'POST', url: '/mutacao-sintetica', data: {}, csrfRetrySeguro: true, retries: 0 }));
    await waitFor(() => expect(mutacao).toBeDefined()); invalidarGeracaoSessao(); antiga.reject(falha(mutacao, 403, 'CSRF_INVALID'));
    expect((await resposta).error).toMatchObject({ canceled: true }); expect(envios).toEqual(['/autenticacao/csrf', '/mutacao-sintetica']);
  });
  it('renovação de sessão de B não reutiliza promessa de senha da sessão A', async () => {
    const antiga = diferida(); let configA; let quantidade = 0;
    api.defaults.adapter = async config => {
      if (config.url === '/autenticacao/csrf') return ok(config, csrf('token-sintetico'));
      if (++quantidade === 1) { configA = config; return antiga.promise; }
      return ok(config, { renovada: 'B' });
    };
    const a = observar(ServicoAutenticacao.renovarSessao('senha-a-sintetica', { retries: 0 }));
    await waitFor(() => expect(configA).toBeDefined()); invalidarGeracaoSessao();
    const b = observar(ServicoAutenticacao.renovarSessao('senha-b-sintetica', { retries: 0 }));
    antiga.resolve(ok(configA, { renovada: 'A' }));
    expect((await a).error).toMatchObject({ canceled: true }); expect((await b).value).toEqual({ renovada: 'B' }); expect(quantidade).toBe(2);
  });
});

describe('Cache e bootstrap CSRF', () => {
  it('invalidação descarta bootstrap antigo e conserva o token novo, mesmo com resolução invertida', async () => {
    const antiga = diferida(), nova = diferida(); let buscas = 0;
    configurarBuscadorTokenCsrf(() => ++buscas === 1 ? antiga.promise : nova.promise);
    const a = observar(obterTokenCsrf()); await Promise.resolve(); invalidarTokenCsrf();
    const b = observar(obterTokenCsrf()); await Promise.resolve(); nova.resolve(csrf('token-b-sintetico')); antiga.resolve(csrf('token-a-sintetico'));
    expect((await a).error).toMatchObject({ canceled: true }); expect((await b).value.token).toBe('token-b-sintetico'); expect(obterTokenCsrfEmMemoria()?.token).toBe('token-b-sintetico');
  });
  it('troca de sessão impede uso do token em cache e do bootstrap anterior', async () => {
    let token = 'a-sintetico'; configurarBuscadorTokenCsrf(async () => csrf(token)); await obterTokenCsrf();
    invalidarGeracaoSessao(); token = 'b-sintetico';
    expect(obterTokenCsrfEmMemoria()).toBeNull(); expect((await obterTokenCsrf()).token).toBe('b-sintetico');
  });
  it('finally antigo não solta a deduplicação do bootstrap da sessão nova', async () => {
    const antiga = diferida(), nova = diferida(); let buscas = 0;
    configurarBuscadorTokenCsrf(() => ++buscas === 1 ? antiga.promise : nova.promise);
    const a = observar(obterTokenCsrf()); await Promise.resolve(); invalidarTokenCsrf();
    const b = observar(obterTokenCsrf()); await Promise.resolve(); antiga.resolve(csrf('a-sintetico')); await a;
    const c = observar(obterTokenCsrf()); await Promise.resolve(); nova.resolve(csrf('b-sintetico'));
    expect((await b).value?.token).toBe('b-sintetico'); expect((await c).value?.token).toBe('b-sintetico'); expect(buscas).toBe(2);
  });
});
