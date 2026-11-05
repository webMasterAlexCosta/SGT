// RI03: componentes, hooks, serviços e interceptores reais; somente o adapter
// Axios e os sinais da outra aba são controlados. Não é HTTP/E2E/MySQL real.
import React from 'react';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { act, cleanup, fireEvent, render, renderHook, screen, waitFor, within } from '@testing-library/react';
import '@testing-library/jest-dom/vitest';
import { ProvedorAutenticacao, useAutenticacao } from '../src/modules/auth/context.jsx';
import { PaginaEmpresasContratadas } from '../src/modules/admin/pages/PaginaEmpresasContratadas/index.jsx';
import { useConsultaVigente } from '../src/core/hooks/useConsultaVigente.js';
import { obterGeracaoSessao, invalidarGeracaoSessao, sessaoAtual } from '../src/core/security/geracaoSessao.js';
import { confirmarLoginExplicito, obterIntencaoSaida } from '../src/core/security/saidaSessao.js';
import { api } from '../src/core/http.js';
import { configurarBuscadorTokenCsrf, resetarCoordenadorCsrfParaTestes } from '../src/core/http/coordenadorCsrf.js';
import { resetarSolicitadorConfirmacaoSenhaParaTestes } from '../src/core/http/coordenadorConfirmacaoSenha.js';
import { EVENT_SHOW_TOAST, redefinirIndicadorRedirecionamento } from '../src/core/events.js';
import { diferida } from './fixtures/promessaDiferida.js';

const CHAVE = 'sgt_revisao_compartilhada_sessao';
const A = { id: 9801, username: 'conta-a-sintetica', perfil: 'GESTOR_LOCAL', autoridade: 'PERFIL_GESTOR_LOCAL', gestaoLocalId: 1 };
const B = { id: 9802, username: 'conta-b-sintetica', perfil: 'OPERACIONAL', autoridade: 'PERFIL_OPERACIONAL', gestaoLocalId: 2 };
const resposta = (config, data) => ({ config, status: 200, statusText: 'OK', headers: {}, data });
const mudarOutraAba = (revisao, { storage = true, cookie = true, evento = false } = {}) => {
  if (storage) localStorage.setItem(CHAVE, revisao);
  if (cookie) document.cookie = `${CHAVE}=${encodeURIComponent(revisao)}; Path=/; SameSite=Strict`;
  if (evento) window.dispatchEvent(new StorageEvent('storage', { key: CHAVE, newValue: revisao }));
};
let contexto, adapterOriginal, usuarioServidor, envios, especial, empresas, avisos, coletar;
const Observador = ({ pagina = false }) => {
  contexto = useAutenticacao();
  return <><output aria-label="Identidade atual">{contexto.user?.username || 'sem-sessao'}</output>
    {pagina && contexto.user && <PaginaEmpresasContratadas />}</>;
};
const montar = (pagina = false) => render(<ProvedorAutenticacao><Observador pagina={pagina}/></ProvedorAutenticacao>);
const montarA = async (pagina = false) => {
  const view = montar(pagina);
  await waitFor(() => expect(contexto.user?.id).toBe(A.id));
  if (pagina) await screen.findByRole('option', { name: 'Rascunho RI03 sintético' });
  return view;
};
const mutacoes = () => envios.filter(e => ['POST', 'PUT', 'DELETE'].includes(e.method));
const preencherEmpresa = () => {
  fireEvent.change(screen.getByLabelText('Razão social da empresa'), { target: { value: 'Empresa RI03 sintética' } });
  fireEvent.change(screen.getByLabelText('CNPJ da empresa'), { target: { value: '11222333000144' } });
  fireEvent.change(screen.getByLabelText('E-mail da empresa'), { target: { value: 'ri03@example.invalid' } });
  fireEvent.change(screen.getByLabelText('Contrato a vincular à empresa'), { target: { value: '9811' } });
};
const confirmarCadastro = async () => {
  fireEvent.click(screen.getByRole('button', { name: 'Cadastrar e vincular' }));
  const modal = await screen.findByRole('dialog', { name: 'Cadastrar empresa contratada' });
  fireEvent.change(within(modal).getByLabelText('Senha de Acesso'), { target: { value: 'sintetico-unitario' } });
  fireEvent.click(within(modal).getByRole('button', { name: 'Confirmar', exact: true }));
};

beforeEach(() => {
  localStorage.clear(); sessionStorage.clear();
  confirmarLoginExplicito(obterIntencaoSaida()); // Ambiente novo, jamais entre ações de um caso.
  mudarOutraAba('revisao-inicial-sintetica-a');
  invalidarGeracaoSessao(); redefinirIndicadorRedirecionamento();
  resetarCoordenadorCsrfParaTestes(); resetarSolicitadorConfirmacaoSenhaParaTestes();
  usuarioServidor = A; envios = []; empresas = []; especial = null; avisos = [];
  coletar = e => avisos.push(e.detail); window.addEventListener(EVENT_SHOW_TOAST, coletar);
  adapterOriginal = api.defaults.adapter;
  api.defaults.adapter = async config => {
    const method = config.method.toUpperCase();
    // Credenciais e headers não são armazenados no log controlado.
    envios.push({ method, url: config.url });
    const custom = especial?.(config);
    if (custom !== undefined) return custom;
    if (config.url === '/autenticacao/csrf') return resposta(config, { headerName: 'X-XSRF-TOKEN', parameterName: '_csrf', token: 'csrf-unitario' });
    if (config.url === '/autenticacao/sessao') return resposta(config, { authenticated: true, user: usuarioServidor, session: { remainingSeconds: 3600 } });
    if (config.url === '/usuarios/meu-perfil' || config.url === '/autenticacao/autenticar') return resposta(config, usuarioServidor);
    if (config.url === '/autenticacao/encerrar-sessao') return resposta(config, null);
    if (method === 'GET' && config.url === '/contratos') return resposta(config, [{ id: 9811, numeroContrato: 'Rascunho RI03 sintético', estadoNegocial: 'RASCUNHO', ativo: true }]);
    if (method === 'GET' && config.url === '/fornecedores') return resposta(config, empresas);
    if (method === 'PUT' && config.url === '/contratos/9811/fornecedor/9821') return resposta(config, { contratoId: 9811, fornecedorId: 9821 });
    throw new Error(`Transporte inesperado: ${method} ${config.url}`);
  };
  configurarBuscadorTokenCsrf(async ({ geracaoSessao }) => api.request({ method: 'GET', url: '/autenticacao/csrf', retries: 0,
    silentError: true, skipAuthLogout: true, __csrfBootstrap: true, __geracaoSessao: geracaoSessao }));
});
afterEach(() => {
  cleanup(); vi.restoreAllMocks(); api.defaults.adapter = adapterOriginal;
  window.removeEventListener(EVENT_SHOW_TOAST, coletar);
  resetarCoordenadorCsrfParaTestes(); resetarSolicitadorConfirmacaoSenhaParaTestes();
});

describe('RI03: fronteira síncrona compartilhada', () => {
  it('o hook recusa a continuação antes de storage/focus chegarem à aba', () => {
    const { result } = renderHook(() => useConsultaVigente());
    const intencao = result.current.iniciar();
    expect(intencao.atual()).toBe(true);
    mudarOutraAba('revisao-b-sem-evento');
    expect(intencao.atual()).toBe(false);
  });
  it('clear do marcador invalida a geração previamente autenticada', () => {
    const geracao = obterGeracaoSessao();
    localStorage.removeItem(CHAVE);
    document.cookie = `${CHAVE}=; Max-Age=0; Path=/`;
    expect(sessaoAtual(geracao)).toBe(false);
  });
  it('storage negado ainda detecta alteração pelo canal síncrono de cookie', () => {
    vi.spyOn(Storage.prototype, 'getItem').mockImplementation(() => { throw new DOMException('Bloqueio sintético', 'SecurityError'); });
    vi.spyOn(Storage.prototype, 'setItem').mockImplementation(() => { throw new DOMException('Bloqueio sintético', 'SecurityError'); });
    invalidarGeracaoSessao();
    const geracao = obterGeracaoSessao();
    expect(sessaoAtual(geracao)).toBe(true);
    mudarOutraAba('revisao-cookie-b', { storage: false });
    expect(sessaoAtual(geracao)).toBe(false);
  });
  it('sem os dois canais nenhuma intenção antiga pode continuar', () => {
    const geracao = obterGeracaoSessao();
    vi.spyOn(Storage.prototype, 'getItem').mockImplementation(() => { throw new DOMException('Bloqueado', 'SecurityError'); });
    vi.spyOn(document, 'cookie', 'get').mockImplementation(() => { throw new DOMException('Bloqueado', 'SecurityError'); });
    expect(sessaoAtual(geracao)).toBe(false);
  });
  it('cookie indisponível bloqueia a intenção mesmo com Storage disponível', () => {
    vi.spyOn(document, 'cookie', 'get').mockImplementation(() => { throw new DOMException('Bloqueado', 'SecurityError'); });
    invalidarGeracaoSessao(); const geracao = obterGeracaoSessao();
    expect(sessaoAtual(geracao)).toBe(false);
    mudarOutraAba('revisao-storage-b', { cookie: false });
    expect(sessaoAtual(geracao)).toBe(false);
  });
  it('canal que volta disponível não revalida a geração capturada antes da troca', () => {
    const leitura = vi.spyOn(Storage.prototype, 'getItem').mockImplementation(() => { throw new DOMException('Bloqueado', 'SecurityError'); });
    invalidarGeracaoSessao(); const geracao = obterGeracaoSessao();
    leitura.mockRestore(); localStorage.setItem(CHAVE, 'revisao-b-recuperada');
    expect(sessaoAtual(geracao)).toBe(false);
  });
});

describe('RI03: provider, POST→PUT e resposta antiga', () => {
  it.each(['storage', 'focus'])('%s invalida A, remove dados privados e não revoga cookie B', async evento => {
    await montarA(); usuarioServidor = B;
    act(() => { mudarOutraAba('revisao-b'); window.dispatchEvent(evento === 'storage'
      ? new StorageEvent('storage', { key: CHAVE, newValue: 'revisao-b' }) : new Event('focus')); });
    await waitFor(() => expect(contexto.user).toBeNull());
    expect(screen.getByLabelText('Identidade atual')).toHaveTextContent('sem-sessao');
    expect(mutacoes()).toEqual([]);
  });
  it('trocas A→B→C e novo login da mesma conta invalidam pela sessão, não pelo userId', async () => {
    await montarA(); const geracao = contexto.geracaoSessao;
    act(() => { mudarOutraAba('revisao-b'); mudarOutraAba('revisao-c-mesma-conta-a', { evento: true }); });
    await waitFor(() => expect(contexto.user).toBeNull());
    expect(sessaoAtual(geracao)).toBe(false); expect(mutacoes()).toEqual([]);
  });
  it('logout local atrasado não envia encerrar-sessao sob o cookie de B', async () => {
    await montarA(); usuarioServidor = B;
    act(() => { mudarOutraAba('revisao-b-sem-evento'); contexto.encerrarSessao(); });
    await act(async () => { await Promise.resolve(); });
    expect(contexto.user).toBeNull();
    expect(mutacoes()).toEqual([]);
  });
  it('instalação tardia do listener não restaura automaticamente outra sessão', async () => {
    usuarioServidor = B; mudarOutraAba('revisao-b-antes-do-provider'); montar();
    await waitFor(() => expect(contexto.loading).toBe(false));
    expect(contexto.user).toBeNull();
    expect(envios).toEqual([]);
  });
  it('remount após observar troca externa exige login explícito, sem restaurar B', async () => {
    const view = await montarA(); usuarioServidor = B;
    act(() => mudarOutraAba('revisao-b-observada', { evento: true }));
    await waitFor(() => expect(contexto.user).toBeNull());
    view.unmount(); envios = []; montar();
    await waitFor(() => expect(contexto.loading).toBe(false));
    expect(contexto.user).toBeNull(); expect(envios).toEqual([]);
    await act(async () => contexto.autenticar('matricula-sintetica-b', 'senha-unitaria'));
    expect(contexto.user?.id).toBe(B.id);
    expect(envios.filter(e => e.url === '/autenticacao/encerrar-sessao')).toEqual([]);
  });
  it('login recusado não libera restauração automática após a troca externa', async () => {
    const view = await montarA(); usuarioServidor = B;
    act(() => mudarOutraAba('revisao-b-observada', { evento: true }));
    await waitFor(() => expect(contexto.user).toBeNull());
    especial = config => config.url === '/autenticacao/autenticar' ? Promise.reject({ config,
      response: { status: 403, data: { code: 'INVALID_CREDENTIALS', message: 'Credenciais sintéticas recusadas' }, headers: {} },
    }) : undefined;
    let retorno; await act(async () => { retorno = await contexto.autenticar('matricula-sintetica', 'senha-unitaria'); });
    expect(retorno.error).toBe(true); expect(contexto.user).toBeNull();
    view.unmount(); envios = []; montar(); await waitFor(() => expect(contexto.loading).toBe(false));
    expect(contexto.user).toBeNull(); expect(envios).toEqual([]);
  });
  it('login tardio depois de C não limpa o bloqueio local de restauração', async () => {
    const view = await montarA(); usuarioServidor = B;
    act(() => mudarOutraAba('revisao-b-observada', { evento: true }));
    await waitFor(() => expect(contexto.user).toBeNull());
    const loginAntigo = diferida(); let configLogin;
    especial = config => { if (config.url === '/autenticacao/autenticar') { configLogin = config; return loginAntigo.promise; } };
    let login; act(() => { login = contexto.autenticar('matricula-sintetica', 'senha-unitaria'); });
    await waitFor(() => expect(configLogin).toBeDefined());
    act(() => mudarOutraAba('revisao-c-posterior', { evento: true }));
    let retorno; await act(async () => { loginAntigo.resolve(resposta(configLogin, B)); retorno = await login; });
    expect(retorno.canceled).toBe(true); expect(contexto.user).toBeNull();
    view.unmount(); envios = []; montar(); await waitFor(() => expect(contexto.loading).toBe(false));
    expect(contexto.user).toBeNull(); expect(envios).toEqual([]);
  });
  it('bloqueio local sobrevive em memória quando sessionStorage não consegue gravá-lo', async () => {
    const view = await montarA(); usuarioServidor = B;
    const setItem = Storage.prototype.setItem;
    vi.spyOn(Storage.prototype, 'setItem').mockImplementation(function (key, value) {
      if (this === sessionStorage) throw new DOMException('Aba sem escrita persistente', 'SecurityError');
      return setItem.call(this, key, value);
    });
    act(() => mudarOutraAba('revisao-b-observada', { evento: true }));
    await waitFor(() => expect(contexto.user).toBeNull());
    view.unmount(); envios = []; montar(); await waitFor(() => expect(contexto.loading).toBe(false));
    expect(contexto.user).toBeNull(); expect(envios).toEqual([]);
    expect(localStorage.getItem('sgt_intencao_saida_sessao')).toBeNull();
  });
  it('renovação na própria aba conclui pelo modal real e conserva o usuário validado', async () => {
    let renovada = false;
    especial = config => {
      if (config.url === '/autenticacao/sessao') return resposta(config, { authenticated: true, user: A,
        session: { remainingSeconds: renovada ? 3600 : 120 } });
      if (config.url === '/autenticacao/renovar-sessao') { renovada = true; return resposta(config, { renovada: true }); }
    };
    await montarA(); const anterior = localStorage.getItem(CHAVE);
    fireEvent.change(await screen.findByPlaceholderText('Sua senha atual'), { target: { value: 'senha-unitaria' } });
    fireEvent.click(screen.getByRole('button', { name: 'Renovar Sessão', exact: true }));
    await waitFor(() => expect(screen.queryByPlaceholderText('Sua senha atual')).toBeNull());
    expect(contexto.user?.id).toBe(A.id); expect(localStorage.getItem(CHAVE)).not.toBe(anterior);
    expect(envios.filter(e => e.url === '/autenticacao/renovar-sessao')).toHaveLength(1);
    expect(envios.filter(e => e.url === '/autenticacao/encerrar-sessao')).toEqual([]);
  });
  it('POST antigo entregue após B não inicia PUT nem anuncia associação/cadastro atual', async () => {
    await montarA(true); const post = diferida(); let configPost;
    especial = config => {
      if (config.method === 'post' && config.url === '/fornecedores') {
        configPost = config; empresas = [{ id: 9821, razaoSocial: 'Empresa RI03 sintética', cnpj: '11222333000144', ativo: true }];
        return post.promise;
      }
    };
    preencherEmpresa(); await confirmarCadastro();
    await waitFor(() => expect(configPost).toBeDefined());
    usuarioServidor = B; mudarOutraAba('revisao-b-pos-post');
    await act(async () => { post.resolve(resposta(configPost, empresas[0])); await post.promise; });
    await act(async () => { await Promise.resolve(); });
    expect(mutacoes().filter(e => e.method === 'POST' && e.url === '/fornecedores')).toHaveLength(1);
    expect(mutacoes().filter(e => e.method === 'PUT')).toEqual([]);
    expect(avisos.filter(a => a.type === 'success')).toEqual([]);
    expect(empresas).toHaveLength(1); // Controle do transporte, não prova de banco.
  });
  it('resposta antiga de perfil não repõe A depois da invalidação externa', async () => {
    await montarA(); const perfil = diferida(); let configPerfil;
    especial = config => { if (config.url === '/usuarios/meu-perfil') { configPerfil = config; return perfil.promise; } };
    let leitura; act(() => { leitura = contexto.atualizarPerfil(); });
    await waitFor(() => expect(configPerfil).toBeDefined());
    act(() => mudarOutraAba('revisao-b', { evento: true }));
    await act(async () => { perfil.resolve(resposta(configPerfil, A)); await leitura; });
    expect(contexto.user).toBeNull(); expect(mutacoes()).toEqual([]);
  });
  it('publica revisão opaca antes de enviar login, inclusive para a mesma conta', async () => {
    await montarA(); const anterior = localStorage.getItem(CHAVE); let revisaoNoEnvio;
    especial = config => {
      if (config.url === '/autenticacao/autenticar') {
        revisaoNoEnvio = localStorage.getItem(CHAVE); return resposta(config, A);
      }
    };
    await act(async () => contexto.autenticar('matricula-sintetica', 'senha-unitaria'));
    expect(contexto.user?.id).toBe(A.id);
    expect(revisaoNoEnvio).toBeTruthy(); expect(revisaoNoEnvio).not.toBe(anterior);
    expect(revisaoNoEnvio).not.toMatch(/conta-a|9801|matricula|senha|csrf|token/i);
  });
  it('login explícito continua disponível com Storage bloqueado e cookie funcional', async () => {
    vi.spyOn(Storage.prototype, 'getItem').mockImplementation(() => { throw new DOMException('Bloqueio sintético', 'SecurityError'); });
    vi.spyOn(Storage.prototype, 'setItem').mockImplementation(() => { throw new DOMException('Bloqueio sintético', 'SecurityError'); });
    vi.spyOn(Storage.prototype, 'removeItem').mockImplementation(() => { throw new DOMException('Bloqueio sintético', 'SecurityError'); });
    invalidarGeracaoSessao(); montar();
    await waitFor(() => expect(contexto.loading).toBe(false));
    expect(contexto.user).toBeNull(); usuarioServidor = B;
    await act(async () => contexto.autenticar('matricula-sintetica-b', 'senha-unitaria'));
    expect(contexto.user?.id).toBe(B.id);
    const geracao = contexto.geracaoSessao;
    mudarOutraAba('revisao-cookie-c', { storage: false });
    expect(sessaoAtual(geracao)).toBe(false);
  });
  it('recusa login sem enviar credenciais quando nenhum canal consegue publicar a revisão', async () => {
    await montarA();
    vi.spyOn(Storage.prototype, 'setItem').mockImplementation(() => { throw new DOMException('Bloqueado', 'SecurityError'); });
    vi.spyOn(document, 'cookie', 'set').mockImplementation(() => {});
    let resultado;
    await act(async () => { resultado = await contexto.autenticar('matricula-sintetica', 'senha-unitaria'); });
    expect(resultado?.error).toBe(true); expect(contexto.user).toBeNull();
    expect(envios.filter(e => e.url === '/autenticacao/autenticar')).toEqual([]);
  });
});
