import React from 'react';
import { act, cleanup, fireEvent, render, screen, waitFor } from '@testing-library/react';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import '@testing-library/jest-dom/vitest';

const observador = vi.hoisted(() => ({ notificar: vi.fn(), confirmar: vi.fn() }));
vi.mock('../src/core/events.js', async original => ({ ...(await original()), notificar: observador.notificar }));
vi.mock('../src/modules/auth/context.jsx', () => ({ useAutenticacao: () => ({ user: { id: 81, perfil: 'GESTOR_LOCAL' } }) }));
vi.mock('../src/core/security/useConfirmacaoSenha.jsx', () => ({ useConfirmacaoSenha: () => ({ confirmarComSenha: observador.confirmar, ModalConfirmacao: () => null }) }));
vi.mock('../src/core/config.js', async original => { const modulo = await original(); return { ...modulo, CONFIG: { ...modulo.CONFIG, SEARCH_DEBOUNCE_MS: 0 } }; });
import { PaginaOcorrencias } from '../src/modules/rh/pages/PaginaOcorrencias/index.jsx';
import { api } from '../src/core/http.js';
import { invalidarGeracaoSessao, obterGeracaoSessao } from '../src/core/security/geracaoSessao.js';
import { resetarCoordenadorCsrfParaTestes } from '../src/core/http/coordenadorCsrf.js';
import { redefinirIndicadorRedirecionamento } from '../src/core/events.js';

const adiar = () => { let resolve, reject; const promise = new Promise((r, j) => { resolve = r; reject = j; }); return { promise, resolve, reject }; };
const ok = (config, data) => ({ config, data, status: 200, statusText: 'OK', headers: {} });
const erro = config => ({ config, response: { status: 409, data: { code: 'CONFLICT', message: 'Conflito sintético anterior' }, headers: {} } });
const registro = (id, observacao) => ({ id, motivo: 'ELOGIO', dataOcorrencia: '2026-10-31', observacao, anexos: [] });
let adapterOriginal, pedidos, responder, confirmacoes;
beforeEach(() => {
  adapterOriginal = api.defaults.adapter; resetarCoordenadorCsrfParaTestes(); redefinirIndicadorRedirecionamento();
  observador.notificar.mockReset(); observador.confirmar.mockReset(); pedidos = []; responder = undefined; confirmacoes = [];
  observador.confirmar.mockImplementation(({ acao }) => { confirmacoes.push(acao); return Promise.resolve(false); });
  vi.spyOn(console, 'error').mockImplementation(() => {});
  api.defaults.adapter = async config => {
    pedidos.push({ config, geracao: obterGeracaoSessao() });
    const personalizado = responder?.(config); if (personalizado !== undefined) return personalizado;
    if (config.url === '/autenticacao/csrf') return ok(config, { headerName: 'X-XSRF-TOKEN', token: 'csrf-sintetico' });
    if (config.url === '/empregados') return ok(config, [{ id: 17, nome: 'Pessoa A' }, { id: 18, nome: 'Pessoa B' }]);
    if (config.url === '/ocorrencias' && config.method === 'get') return ok(config, [registro(Number(config.params.empregadoId) === 18 ? 72 : 71, Number(config.params.empregadoId) === 18 ? 'Histórico da Pessoa B' : 'Histórico da Pessoa A')]);
    return ok(config, { id: 73 });
  };
});
afterEach(() => { cleanup(); api.defaults.adapter = adapterOriginal; invalidarGeracaoSessao(); vi.restoreAllMocks(); });

async function selecionar(nome = 'Pessoa A') {
  fireEvent.change(screen.getByPlaceholderText('Nome ou CPF...'), { target: { value: nome } });
  fireEvent.click(await screen.findByRole('button', { name: new RegExp(nome) }));
  await screen.findByText(`Histórico da ${nome}`);
}
async function preparar(tipo = 'criar') {
  const view = render(<PaginaOcorrencias />); await selecionar();
  if (tipo === 'criar') {
    fireEvent.click(screen.getByRole('button', { name: /Registrar Ocorrência/ }));
    fireEvent.click(screen.getByRole('button', { name: 'Salvar Registro' }));
  } else fireEvent.click(view.container.querySelector('button[class*="botaoLixeira"]'));
  await waitFor(() => expect(confirmacoes).toHaveLength(1));
  return { ...view, acao: confirmacoes[0] };
}
async function executar(acao) {
  let operacao;
  await act(async () => { operacao = acao('senha-sintetica').catch(error => error); });
  return { operacao };
}

describe('Ocorrências: escrita, confirmação e recarga pertencem à intenção vigente', () => {
  it.each(['criar', 'excluir'])('não publica sucesso de %s depois de desmontar a página', async tipo => {
    const atrasado = adiar(); let enviado;
    responder = config => {
      if (config.method === (tipo === 'criar' ? 'post' : 'delete')) { enviado = config; return atrasado.promise; }
    };
    const view = await preparar(tipo); const { operacao } = await executar(view.acao);
    await waitFor(() => expect(enviado).toBeDefined());
    view.unmount(); const antes = pedidos.length; observador.notificar.mockClear();
    await act(async () => { atrasado.resolve(ok(enviado, { id: 73 })); await operacao; });
    expect(pedidos.slice(antes)).toEqual([]);
    expect(observador.notificar.mock.calls).toEqual([]);
  });

  it.each(['criar', 'excluir'])('não notifica nem relança erro de %s da sessão anterior', async tipo => {
    const atrasado = adiar(); let enviado;
    responder = config => {
      if (config.method === (tipo === 'criar' ? 'post' : 'delete')) { enviado = config; return atrasado.promise; }
    };
    const view = await preparar(tipo); const { operacao } = await executar(view.acao);
    await waitFor(() => expect(enviado).toBeDefined());
    invalidarGeracaoSessao(); invalidarGeracaoSessao(); redefinirIndicadorRedirecionamento();
    const antes = pedidos.length; observador.notificar.mockClear(); let resultado;
    await act(async () => { atrasado.reject(erro(enviado)); resultado = await operacao; });
    expect(observador.notificar.mock.calls).toEqual([]);
    expect(pedidos.slice(antes)).toEqual([]);
    expect(resultado).not.toBeInstanceOf(Error);
    expect(resultado?.canceled).not.toBe(true);
  });

  it('não usa confirmação da Pessoa A depois que a seleção muda para a Pessoa B', async () => {
    const view = await preparar(); await selecionar('Pessoa B');
    const antes = pedidos.length; observador.notificar.mockClear();
    await act(async () => view.acao('senha-sintetica'));
    expect(pedidos.slice(antes)).toEqual([]);
    expect(observador.notificar.mock.calls).toEqual([]);
    expect(screen.getByText('Histórico da Pessoa B')).toBeInTheDocument();
  });

  it('não inicia exclusão com confirmação capturada na sessão anterior', async () => {
    const view = await preparar('excluir'); invalidarGeracaoSessao(); redefinirIndicadorRedirecionamento();
    const antes = pedidos.length; observador.notificar.mockClear();
    await act(async () => view.acao('senha-sintetica'));
    expect(pedidos.slice(antes)).toEqual([]);
    expect(observador.notificar.mock.calls).toEqual([]);
  });

  it('não substitui o histórico da seleção nova por recarga de uma criação antiga', async () => {
    const atrasado = adiar(); let recarga;
    const view = await preparar();
    responder = config => {
      if (config.url === '/ocorrencias' && config.method === 'get' && Number(config.params.empregadoId) === 17) { recarga = config; return atrasado.promise; }
    };
    const { operacao } = await executar(view.acao);
    await waitFor(() => expect(recarga).toBeDefined());
    await selecionar('Pessoa B'); observador.notificar.mockClear();
    await act(async () => { atrasado.resolve(ok(recarga, [registro(99, 'Recarga antiga da Pessoa A')])); await operacao; });
    expect(screen.getByText('Histórico da Pessoa B')).toBeInTheDocument();
    expect(screen.queryByText('Recarga antiga da Pessoa A')).not.toBeInTheDocument();
    expect(observador.notificar.mock.calls).toEqual([]);
  });

  it('preserva criação, notificação e recarga quando a intenção continua atual', async () => {
    const view = await preparar(); const antes = pedidos.length;
    await act(async () => view.acao('senha-sintetica'));
    const depois = pedidos.slice(antes).map(p => `${p.config.method} ${p.config.url}`);
    expect(depois).toContain('post /ocorrencias'); expect(depois).toContain('get /ocorrencias');
    expect(observador.notificar).toHaveBeenCalledWith('success', expect.stringMatching(/^Ocorrência registrada/));
    expect(screen.getByText('Histórico da Pessoa A')).toBeInTheDocument();
  });
});
