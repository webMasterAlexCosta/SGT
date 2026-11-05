// Contraprova do E2E real: cookie ainda existente durante uma revogação pendente.
// Transporte controlado; nenhuma asserção pretende provar revogação no servidor.
import React from 'react';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { act, cleanup, render, waitFor } from '@testing-library/react';
import { ProvedorAutenticacao, useAutenticacao } from '../src/modules/auth/context.jsx';
import { redefinirIndicadorRedirecionamento } from '../src/core/events.js';
import { iniciarContextoSessaoIsolado } from './fixtures/contextoSessaoIsolado.js';
const { servico } = vi.hoisted(() => ({ servico: { informacoesSessao: vi.fn(), buscarMeuPerfil: vi.fn(), encerrarSessao: vi.fn(), autenticar: vi.fn(), cadastrar: vi.fn() } }));
vi.mock('../src/services/auth/ServicoAutenticacao', () => ({ ServicoAutenticacao: servico }));
vi.mock('../src/modules/auth/components/ModalReautenticacao', () => ({ ModalReautenticacao: () => null }));
const A = { id: 741, username: 'logout-a-sintetico', perfil: 'GESTOR_LOCAL', autoridade: 'PERFIL_GESTOR_LOCAL', gestaoLocalId: 1 };
const B = { id: 742, username: 'logout-b-sintetico', perfil: 'OPERACIONAL', autoridade: 'PERFIL_OPERACIONAL', gestaoLocalId: 2 };
const sessao = user => ({ authenticated: true, user, session: { remainingSeconds: 3600 } });
const diferida = () => { let resolve; const promise = new Promise(r => { resolve = r; }); return { promise, resolve }; };
let contexto;
const Observador = () => { contexto = useAutenticacao(); return <output>{contexto.user?.username || 'sem-sessao'}</output>; };
const montar = () => render(<ProvedorAutenticacao><Observador /></ProvedorAutenticacao>);
const montarA = async () => { const tela = montar(); await waitFor(() => expect(contexto.user?.id).toBe(A.id)); return tela; };
const prepararB = () => { servico.autenticar.mockResolvedValue(B); servico.buscarMeuPerfil.mockResolvedValue(B); servico.informacoesSessao.mockResolvedValue(sessao(B)); };
beforeEach(() => {
  localStorage.clear(); sessionStorage.clear(); redefinirIndicadorRedirecionamento();
  iniciarContextoSessaoIsolado();
  servico.informacoesSessao.mockReset().mockResolvedValue(sessao(A));
  servico.buscarMeuPerfil.mockReset().mockResolvedValue(A);
  servico.encerrarSessao.mockReset().mockResolvedValue(true);
  servico.autenticar.mockReset().mockResolvedValue(A);
});
afterEach(() => cleanup());
describe('Logout atravessa remontagem sem restaurar cookie antigo', () => {
  it('uma nova instância não restaura A depois da saída, mesmo que o servidor ainda retorne A', async () => {
    const tela = await montarA();
    act(() => contexto.encerrarSessao());
    await act(async () => contexto.concluirSaida());
    tela.unmount();
    montar();
    await waitFor(() => expect(contexto.loading).toBe(false));
    expect(contexto.user).toBeNull();
    expect(contexto.estadoAutenticacao).toBe('NAO_AUTENTICADO');
  });
  it('login B aguarda o encerramento HTTP anterior antes de enviar credenciais', async () => {
    await montarA(); const fim = diferida(); servico.encerrarSessao.mockReturnValueOnce(fim.promise);
    act(() => contexto.encerrarSessao()); prepararB();
    let login; act(() => { login = contexto.autenticar('b', 'segredo-apenas-mock'); });
    await act(async () => { await Promise.resolve(); });
    const enviouAntesDoEncerramento = servico.autenticar.mock.calls.length;
    await act(async () => { fim.resolve(true); await login; });
    expect(enviouAntesDoEncerramento).toBe(0);
    expect(contexto.user?.id).toBe(B.id);
  });
  it('logout durante bootstrap impede restauração na instância seguinte', async () => {
    const antiga = diferida(); servico.informacoesSessao.mockReturnValueOnce(antiga.promise);
    const tela = montar(); act(() => contexto.encerrarSessao());
    tela.unmount(); montar();
    await act(async () => { antiga.resolve(sessao(A)); });
    await waitFor(() => expect(contexto.loading).toBe(false));
    expect(contexto.user).toBeNull();
  });
  it('falha de novo login conserva a saída quando a aplicação remonta', async () => {
    const tela = await montarA(); act(() => contexto.encerrarSessao());
    await act(async () => contexto.concluirSaida());
    servico.autenticar.mockResolvedValue({ error: true, status: 401, message: 'Credencial sintética incorreta' });
    await act(async () => contexto.autenticar('a', 'senha-incorreta-mock'));
    tela.unmount(); montar(); await waitFor(() => expect(contexto.loading).toBe(false));
    expect(contexto.user).toBeNull();
  });
  it('login explícito validado libera a restauração da nova identidade', async () => {
    const tela = await montarA(); act(() => contexto.encerrarSessao());
    await act(async () => contexto.concluirSaida()); prepararB();
    await act(async () => contexto.autenticar('b', 'segredo-apenas-mock'));
    expect(contexto.user?.id).toBe(B.id);
    tela.unmount(); montar(); await waitFor(() => expect(contexto.loading).toBe(false));
    expect(contexto.user?.id).toBe(B.id);
  });
});
