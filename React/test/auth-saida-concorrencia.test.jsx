// Regressões complementares da saída persistida. Serviço simulado; não é E2E.
import React from 'react';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { act, cleanup, render, waitFor } from '@testing-library/react';
import { ProvedorAutenticacao, useAutenticacao } from '../src/modules/auth/context.tsx';
import { redefinirIndicadorRedirecionamento } from '../src/core/events.ts';
import { iniciarContextoSessaoIsolado } from './fixtures/contextoSessaoIsolado.js';
const { servico } = vi.hoisted(() => ({ servico: { informacoesSessao: vi.fn(), buscarMeuPerfil: vi.fn(), encerrarSessao: vi.fn(), autenticar: vi.fn(), cadastrar: vi.fn() } }));
vi.mock('../src/services/auth/ServicoAutenticacao', () => ({ ServicoAutenticacao: servico }));
vi.mock('../src/modules/auth/components/ModalReautenticacao', () => ({ ModalReautenticacao: () => null }));
const A = { id: 821, username: 'saida-a-sintetico', perfil: 'GESTOR_LOCAL', autoridade: 'PERFIL_GESTOR_LOCAL' };
const B = { id: 822, username: 'saida-b-sintetico', perfil: 'OPERACIONAL', autoridade: 'PERFIL_OPERACIONAL' };
const sessao = user => ({ authenticated: true, user, session: { remainingSeconds: 3600 } });
const diferida = () => { let resolve; const promise = new Promise(r => { resolve = r; }); return { promise, resolve }; };
let contexto;
const Observador = () => { contexto = useAutenticacao(); return <output>{contexto.user?.id || 'sem-sessao'}</output>; };
const montar = () => render(<ProvedorAutenticacao><Observador /></ProvedorAutenticacao>);
const montarA = async () => { const view = montar(); await waitFor(() => expect(contexto.user?.id).toBe(A.id)); return view; };
const prepararB = () => { servico.autenticar.mockResolvedValue(B); servico.buscarMeuPerfil.mockResolvedValue(B); servico.informacoesSessao.mockResolvedValue(sessao(B)); };
beforeEach(() => {
  localStorage.clear(); sessionStorage.clear(); redefinirIndicadorRedirecionamento();
  iniciarContextoSessaoIsolado();
  servico.informacoesSessao.mockReset().mockResolvedValue(sessao(A)); servico.buscarMeuPerfil.mockReset().mockResolvedValue(A);
  servico.encerrarSessao.mockReset().mockResolvedValue(true); servico.autenticar.mockReset().mockResolvedValue(A);
});
afterEach(() => { cleanup(); vi.restoreAllMocks(); });
describe('Coordenação da saída entre montagens e tentativas', () => {
  it('StrictMode não consulta cookie marcado como encerrado nem duplica revogação no bootstrap', async () => {
    const tela = await montarA(); act(() => contexto.encerrarSessao()); await act(async () => contexto.concluirSaida()); tela.unmount();
    servico.informacoesSessao.mockClear(); servico.buscarMeuPerfil.mockClear(); servico.encerrarSessao.mockClear();
    render(<React.StrictMode><ProvedorAutenticacao><Observador /></ProvedorAutenticacao></React.StrictMode>);
    await waitFor(() => expect(contexto.loading).toBe(false));
    expect(contexto.user).toBeNull(); expect(servico.informacoesSessao).not.toHaveBeenCalled(); expect(servico.buscarMeuPerfil).not.toHaveBeenCalled(); expect(servico.encerrarSessao).not.toHaveBeenCalled();
  });
  it('desmontagem não aborta a revogação e a nova instância aguarda sua resposta antes de B', async () => {
    const tela = await montarA(); const fim = diferida(); let sinal;
    servico.encerrarSessao.mockImplementationOnce(options => { sinal = options.signal; return fim.promise; });
    act(() => contexto.encerrarSessao()); tela.unmount(); montar(); prepararB();
    let login; act(() => { login = contexto.autenticar('b', 'senha-apenas-mock'); });
    await act(async () => { await Promise.resolve(); });
    const antes = servico.autenticar.mock.calls.length; const abortou = sinal?.aborted;
    await act(async () => { fim.resolve(true); await login; });
    expect(antes).toBe(0); expect(abortou).toBe(false); expect(contexto.user?.id).toBe(B.id);
  });
  it('repetir saída enquanto o HTTP está pendente não substitui nem cancela a revogação em curso', async () => {
    await montarA(); const fim = diferida(); let sinal;
    servico.encerrarSessao.mockImplementationOnce(options => { sinal = options.signal; return fim.promise; });
    act(() => { contexto.encerrarSessao(); contexto.encerrarSessao(); });
    const abortou = sinal?.aborted; const quantidade = servico.encerrarSessao.mock.calls.length;
    await act(async () => { fim.resolve(true); });
    expect(abortou).toBe(false); expect(quantidade).toBe(1); expect(contexto.user).toBeNull();
  });
  it('falha de revogação mantém saída local e bloqueia credenciais até uma nova revogação confirmada', async () => {
    await montarA(); const fim = diferida(); servico.encerrarSessao.mockReturnValueOnce(fim.promise);
    act(() => contexto.encerrarSessao()); prepararB();
    let login; act(() => { login = contexto.autenticar('b', 'senha-apenas-mock'); });
    let resultado; await act(async () => { fim.resolve(false); resultado = await login; });
    expect(resultado?.error).toBe(true); expect(contexto.user).toBeNull(); expect(servico.autenticar).not.toHaveBeenCalled();
    await act(async () => contexto.autenticar('b', 'senha-apenas-mock'));
    expect(contexto.user?.id).toBe(B.id); expect(servico.encerrarSessao).toHaveBeenCalledTimes(2);
  });
  it('logout durante a espera de login cancela aquela intenção e nunca envia credenciais sob saída nova', async () => {
    await montarA(); const fim = diferida(); servico.encerrarSessao.mockReturnValueOnce(fim.promise);
    act(() => contexto.encerrarSessao()); prepararB();
    let login; act(() => { login = contexto.autenticar('b', 'senha-apenas-mock'); });
    act(() => contexto.encerrarSessao());
    let resultado; await act(async () => { fim.resolve(true); resultado = await login; });
    expect(resultado?.canceled).toBe(true); expect(contexto.user).toBeNull(); expect(servico.autenticar).not.toHaveBeenCalled();
  });
  it('storage que rejeita gravação não quebra logout e mantém bloqueio em memória até login explícito', async () => {
    const tela = await montarA(); const original = Storage.prototype.setItem;
    vi.spyOn(Storage.prototype, 'setItem').mockImplementation(() => { throw new DOMException('Bloqueado no cenário sintético', 'SecurityError'); });
    act(() => contexto.encerrarSessao()); await act(async () => contexto.concluirSaida());
    tela.unmount(); montar(); await waitFor(() => expect(contexto.loading).toBe(false)); expect(contexto.user).toBeNull();
    prepararB(); await act(async () => contexto.autenticar('b', 'senha-apenas-mock')); expect(contexto.user?.id).toBe(B.id);
    Storage.prototype.setItem = original;
  });
  it('storage totalmente indisponível impede restauração silenciosa e permite login explícito validado', async () => {
    vi.spyOn(Storage.prototype, 'getItem').mockImplementation(() => { throw new DOMException('Bloqueado no cenário sintético', 'SecurityError'); });
    vi.spyOn(Storage.prototype, 'setItem').mockImplementation(() => { throw new DOMException('Bloqueado no cenário sintético', 'SecurityError'); });
    vi.spyOn(Storage.prototype, 'removeItem').mockImplementation(() => { throw new DOMException('Bloqueado no cenário sintético', 'SecurityError'); });
    montar(); await waitFor(() => expect(contexto.loading).toBe(false)); expect(contexto.user).toBeNull();
    prepararB(); await act(async () => contexto.autenticar('b', 'senha-apenas-mock')); expect(contexto.user?.id).toBe(B.id);
  });
});
