// Duas instâncias de módulos simulam os contextos JS das abas. Serviço e Axios
// reais, adapter controlado. Não há cookies autenticadores, backend ou E2E real.
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { AxiosError } from 'axios';

const CHAVE = 'sgt_revisao_compartilhada_sessao';
const resposta = (config, data) => ({ config, data, status: 200, statusText: 'OK', headers: {} });
const mudarRevisao = valor => {
  localStorage.setItem(CHAVE, valor);
  document.cookie = `${CHAVE}=${valor}; Path=/; SameSite=Strict`;
};
let sessaoA, sessaoB, servicoB, apiB, requisitarB, geracaoA, geracaoB, enviados, resolver, rejeitar, configuracao;
beforeEach(async () => {
  vi.resetModules(); localStorage.clear(); sessionStorage.clear(); mudarRevisao('sessao-inicial-sintetica');
  sessaoA = await import('../src/core/security/geracaoSessao.ts'); geracaoA = sessaoA.obterGeracaoSessao();
  vi.resetModules();
  sessaoB = await import('../src/core/security/geracaoSessao.ts'); geracaoB = sessaoB.obterGeracaoSessao();
  ({ api: apiB, requisitar: requisitarB } = await import('../src/core/http.ts'));
  ({ ServicoAutenticacao: servicoB } = await import('../src/services/auth/ServicoAutenticacao.ts'));
  enviados = []; configuracao = null;
  const gate = new Promise((resolve, reject) => { resolver = resolve; rejeitar = reject; });
  apiB.defaults.adapter = async config => {
    if (config.url === '/autenticacao/csrf') return resposta(config, { headerName: 'X-XSRF-TOKEN', token: 'csrf-sintetico-unitario' });
    enviados.push({ method: config.method, url: config.url });
    configuracao = config; return gate;
  };
});
afterEach(() => { if (configuracao) resolver(resposta(configuracao, { renovada: true })); vi.restoreAllMocks(); });

describe('RI03: renovação da mesma conta na outra aba', () => {
  it('publica invalidação antes do POST que pode substituir o cookie do servidor', async () => {
    const renovacao = servicoB.renovarSessao('senha-somente-unitaria', { retries: 0 });
    await vi.waitFor(() => expect(configuracao).not.toBeNull());
    const antigaAindaAtual = sessaoA.sessaoAtual(geracaoA);
    const renovacaoAindaAtual = sessaoB.sessaoAtual(geracaoB);
    resolver(resposta(configuracao, { renovada: true }));
    expect(await renovacao).toEqual({ renovada: true });
    expect(antigaAindaAtual).toBe(false);
    expect(renovacaoAindaAtual).toBe(true);
    expect(enviados).toEqual([{ method: 'post', url: '/autenticacao/renovar-sessao' }]);
  });
  it('a deduplicação preserva a própria renovação e não anuncia uma segunda intenção', async () => {
    const primeira = servicoB.renovarSessao('senha-somente-unitaria', { retries: 0 });
    await vi.waitFor(() => expect(configuracao).not.toBeNull());
    const revisao = localStorage.getItem(CHAVE);
    const segunda = servicoB.renovarSessao('senha-somente-unitaria', { retries: 0 });
    expect(segunda).toBe(primeira); expect(localStorage.getItem(CHAVE)).toBe(revisao);
    resolver(resposta(configuracao, { renovada: true })); await primeira;
    expect(enviados).toHaveLength(1);
  });
  it('renovação iniciada por uma aba já obsoleta não publica nem envia sob a sessão nova', async () => {
    mudarRevisao('sessao-c-sintetica');
    let resultado;
    const renovacao = servicoB.renovarSessao('senha-somente-unitaria', { retries: 0 })
      .then(value => (resultado = { value }), error => (resultado = { error }));
    // A baseline envia indevidamente. Libere esse transporte para que o RED
    // seja a asserção de zero envios/cancelamento, nunca um timeout da fixture.
    await vi.waitFor(() => expect(Boolean(resultado || configuracao)).toBe(true));
    if (configuracao) resolver(resposta(configuracao, { renovada: true }));
    await renovacao;
    expect(enviados).toEqual([]);
    expect(resultado.error).toMatchObject({ canceled: true });
    expect(localStorage.getItem(CHAVE)).toBe('sessao-c-sintetica');
  });
  it('falha HTTP mantém a invalidação conservadora de A sem revogar a sessão compartilhada', async () => {
    const renovacao = servicoB.renovarSessao('senha-somente-unitaria', { retries: 0, silentError: true })
      .then(value => ({ value }), error => ({ error }));
    await vi.waitFor(() => expect(configuracao).not.toBeNull());
    rejeitar(new AxiosError('Renovação sintética recusada', 'ERR_BAD_RESPONSE', configuracao, null,
      { config: configuracao, status: 503, headers: {}, data: { message: 'Renovação sintética recusada' } }));
    expect((await renovacao).error).toMatchObject({ status: 503 });
    expect(sessaoA.sessaoAtual(geracaoA)).toBe(false); expect(sessaoB.sessaoAtual(geracaoB)).toBe(true);
    expect(enviados).toEqual([{ method: 'post', url: '/autenticacao/renovar-sessao' }]);
  });
  it('resposta de renovação tardia depois de C é descartada e não republica a revisão B', async () => {
    const renovacao = servicoB.renovarSessao('senha-somente-unitaria', { retries: 0 })
      .then(value => ({ value }), error => ({ error }));
    await vi.waitFor(() => expect(configuracao).not.toBeNull());
    mudarRevisao('sessao-c-durante-renovacao');
    resolver(resposta(configuracao, { renovada: true }));
    expect((await renovacao).error).toMatchObject({ canceled: true });
    expect(localStorage.getItem(CHAVE)).toBe('sessao-c-durante-renovacao');
    expect(enviados).toHaveLength(1);
  });
  it('publicação indisponível rejeita renovação antes de enviar a senha', async () => {
    vi.spyOn(Storage.prototype, 'setItem').mockImplementation(() => { throw new DOMException('Bloqueado', 'SecurityError'); });
    vi.spyOn(document, 'cookie', 'set').mockImplementation(() => {});
    const renovacao = servicoB.renovarSessao('senha-somente-unitaria', { retries: 0 })
      .then(value => ({ value }), error => ({ error }));
    // Se a implementação errada enviar a senha, liberamos apenas o adapter
    // para observar o resultado: a asserção abaixo ainda exige ZERO envios.
    await Promise.resolve(); await Promise.resolve();
    if (configuracao) resolver(resposta(configuracao, { renovada: true }));
    const resultado = await Promise.race([renovacao, new Promise(resolve => setTimeout(() => resolve({ timeout: true }), 200))]);
    expect(enviados).toEqual([]);
    expect(resultado.error).toMatchObject({ code: 'SESSION_COORDINATION_UNAVAILABLE' });
  });
  it('canais assimétricos A apenas cookie / B apenas Storage recusam a renovação antes do HTTP', async () => {
    const storageA = vi.spyOn(Storage.prototype, 'getItem').mockImplementation(() => { throw new DOMException('A sem Storage', 'SecurityError'); });
    sessaoA.invalidarGeracaoSessao(); storageA.mockRestore();
    vi.spyOn(document, 'cookie', 'set').mockImplementation(() => {});
    let resultado;
    const renovacao = servicoB.renovarSessao('senha-somente-unitaria', { retries: 0 })
      .then(value => (resultado = { value }), error => (resultado = { error }));
    await vi.waitFor(() => expect(Boolean(resultado || configuracao)).toBe(true));
    if (configuracao) resolver(resposta(configuracao, { renovada: true }));
    await renovacao;
    expect(enviados).toEqual([]);
    expect(resultado.error).toMatchObject({ code: 'SESSION_COORDINATION_UNAVAILABLE' });
  });
  it('canais assimétricos A apenas Storage / B apenas cookie bloqueiam a intenção antiga de A', async () => {
    const cookieA = vi.spyOn(document, 'cookie', 'get').mockImplementation(() => { throw new DOMException('A sem cookie', 'SecurityError'); });
    const capturaA = sessaoA.invalidarGeracaoSessao(); cookieA.mockRestore();
    const getB = vi.spyOn(Storage.prototype, 'getItem').mockImplementation(() => { throw new DOMException('B sem Storage', 'SecurityError'); });
    const setB = vi.spyOn(Storage.prototype, 'setItem').mockImplementation(() => { throw new DOMException('B sem Storage', 'SecurityError'); });
    sessaoB.invalidarGeracaoSessao();
    const renovacao = servicoB.renovarSessao('senha-somente-unitaria', { retries: 0 });
    await vi.waitFor(() => expect(configuracao).not.toBeNull());
    resolver(resposta(configuracao, { renovada: true }));
    expect(await renovacao).toEqual({ renovada: true });
    getB.mockRestore(); setB.mockRestore();
    vi.spyOn(document, 'cookie', 'get').mockImplementation(() => { throw new DOMException('A sem cookie', 'SecurityError'); });
    expect(sessaoA.sessaoAtual(capturaA)).toBe(false);
    expect(enviados).toEqual([{ method: 'post', url: '/autenticacao/renovar-sessao' }]);
  });
  it('compatibilidade: renovação da própria aba preserva a operação local já iniciada na mesma identidade', async () => {
    // A renovação não troca a identidade/escopo desta aba. Este controle delimita
    // o comportamento preservado; não comprova autorização no backend.
    const adapter = apiB.defaults.adapter; let configNegocio, resolverNegocio;
    const gateNegocio = new Promise(resolve => { resolverNegocio = resolve; });
    apiB.defaults.adapter = config => {
      if (config.url === '/negocio-sintetico') { configNegocio = config; return gateNegocio; }
      return adapter(config);
    };
    const negocio = requisitarB({ method: 'POST', url: '/negocio-sintetico', data: {}, retries: 0 });
    await vi.waitFor(() => expect(configNegocio).toBeDefined());
    const renovacao = servicoB.renovarSessao('senha-somente-unitaria', { retries: 0 });
    await vi.waitFor(() => expect(configuracao).not.toBeNull());
    resolver(resposta(configuracao, { renovada: true })); await renovacao;
    resolverNegocio(resposta(configNegocio, { confirmadoNaMesmaIdentidade: true }));
    expect(await negocio).toEqual({ confirmadoNaMesmaIdentidade: true });
    expect(sessaoB.sessaoAtual(geracaoB)).toBe(true);
  });
});
