// Componentes reais, transporte controlado. Não é E2E nem prova de autorização Java.
import React from 'react';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { act, cleanup, render, screen, waitFor } from '@testing-library/react';
import { ProvedorAutenticacao, useAutenticacao, normalizarUsuarioAutenticado } from '../src/modules/auth/context.jsx';
import { obterGeracaoSessao } from '../src/core/security/geracaoSessao.js';
import { redefinirIndicadorRedirecionamento } from '../src/core/events.js';
import { iniciarContextoSessaoIsolado } from './fixtures/contextoSessaoIsolado.js';

const { servico } = vi.hoisted(() => ({ servico: {
  informacoesSessao: vi.fn(), buscarMeuPerfil: vi.fn(), encerrarSessao: vi.fn(), autenticar: vi.fn(), cadastrar: vi.fn(),
} }));
vi.mock('../src/services/auth/ServicoAutenticacao', () => ({ ServicoAutenticacao: servico }));
vi.mock('../src/modules/auth/components/ModalReautenticacao', () => ({ ModalReautenticacao: () => null }));
const A = { id: 901, username: 'conta-a-sintetica', email: 'a@example.invalid', fotoUrl: '/foto-a-sintetica', perfil: 'GESTOR_LOCAL', autoridade: 'PERFIL_GESTOR_LOCAL', gestaoLocalId: 1, lotacaoId: 10, jornadaId: 20 };
const B = { id: 902, username: 'conta-b-sintetica', email: 'b@example.invalid', perfil: 'OPERACIONAL', autoridade: 'PERFIL_OPERACIONAL', gestaoLocalId: 2 };
const sessao = (user, remainingSeconds = 3600) => ({ authenticated: true, user, session: { remainingSeconds } });
const diferida = () => { let resolve, reject; const promise = new Promise((a, b) => { resolve = a; reject = b; }); return { promise, resolve, reject }; };
let contexto;
const Observador = () => {
  contexto = useAutenticacao();
  return <output data-testid="usuario">{contexto.user?.username || 'sem-sessao'}</output>;
};
const montar = () => render(<ProvedorAutenticacao><Observador /></ProvedorAutenticacao>);
const montarA = async () => { montar(); await waitFor(() => expect(contexto.loading).toBe(false)); expect(contexto.user.username).toBe(A.username); };
const sair = async () => { act(() => contexto.encerrarSessao()); await act(async () => contexto.concluirSaida()); };
const entrarB = async () => {
  servico.autenticar.mockResolvedValue(B); servico.buscarMeuPerfil.mockResolvedValue(B); servico.informacoesSessao.mockResolvedValue(sessao(B));
  await act(async () => contexto.autenticar('matricula-b-sintetica', 'segredo-apenas-mock'));
  expect(contexto.user.username).toBe(B.username);
};
beforeEach(() => {
  localStorage.clear(); sessionStorage.clear(); redefinirIndicadorRedirecionamento();
  iniciarContextoSessaoIsolado();
  servico.informacoesSessao.mockReset().mockResolvedValue(sessao(A));
  servico.buscarMeuPerfil.mockReset().mockResolvedValue(A);
  servico.encerrarSessao.mockReset().mockResolvedValue(true);
  servico.autenticar.mockReset().mockResolvedValue(A);
});
afterEach(() => cleanup());

describe('Geração e ordem de publicação no provedor', () => {
  it('StrictMode mantém a geração publicada alinhada ao transporte após remontar efeitos', async () => {
    render(<React.StrictMode><ProvedorAutenticacao><Observador /></ProvedorAutenticacao></React.StrictMode>);
    await waitFor(() => expect(contexto.loading).toBe(false));
    expect(contexto.user.username).toBe(A.username);
    expect(contexto.geracaoSessao).toBe(obterGeracaoSessao());
  });
  it('apaga usuário, foto e prazo no início do logout, mantendo a animação de saída', async () => {
    await montarA(); act(() => contexto.encerrarSessao());
    expect(contexto.user).toBeNull(); expect(contexto.sessionExpiresAtEpochMs).toBeNull();
    expect(screen.getByTestId('usuario').textContent).toBe('sem-sessao');
    expect(contexto.loggingOut).toBe(true);
    expect(Object.values(localStorage)).not.toContain(A.fotoUrl);
  });
  it('bootstrap atrasado não autentica após logout', async () => {
    const antiga = diferida(); servico.informacoesSessao.mockReturnValueOnce(antiga.promise); montar();
    await sair(); await act(async () => { antiga.resolve(sessao(A)); await antiga.promise; });
    await waitFor(() => expect(contexto.loading).toBe(false));
    expect(contexto.user).toBeNull(); expect(contexto.sessionExpiresAtEpochMs).toBeNull();
  });
  it('bootstrap de A não compartilha sua promessa nem sobrescreve login B', async () => {
    const antiga = diferida(); servico.informacoesSessao.mockReturnValueOnce(antiga.promise); montar();
    await sair();
    servico.autenticar.mockResolvedValue(B); servico.buscarMeuPerfil.mockResolvedValue(B); servico.informacoesSessao.mockResolvedValue(sessao(B));
    let login; act(() => { login = contexto.autenticar('b-sintetica', 'segredo-apenas-mock'); });
    await act(async () => { antiga.resolve(sessao(A)); await login; });
    expect(contexto.user.username).toBe(B.username); expect(contexto.user.perfil).toBe(B.perfil);
  });
  it.each(['autenticar', 'perfil'])('logout invalida login pendente na etapa %s', async (etapa) => {
    await montarA(); await sair(); const antiga = diferida();
    if (etapa === 'autenticar') servico.autenticar.mockReturnValueOnce(antiga.promise);
    else servico.buscarMeuPerfil.mockReturnValueOnce(antiga.promise);
    let login; act(() => { login = contexto.autenticar('a-sintetica', 'segredo-apenas-mock'); });
    await act(async () => { await Promise.resolve(); });
    act(() => contexto.encerrarSessao());
    let retorno; await act(async () => { antiga.resolve(A); retorno = await login; });
    expect(contexto.user).toBeNull(); expect(contexto.estadoAutenticacao).toBe('NAO_AUTENTICADO');
    expect(retorno?.canceled).toBe(true);
  });
  it('login B concluído primeiro prevalece sobre resposta de autenticação A atrasada', async () => {
    await montarA(); await sair(); const antiga = diferida(); servico.autenticar.mockReturnValueOnce(antiga.promise);
    let loginA; act(() => { loginA = contexto.autenticar('a-sintetica', 'segredo-apenas-mock'); });
    await entrarB();
    await act(async () => { antiga.resolve(A); await loginA; });
    expect(contexto.user.username).toBe(B.username); expect(contexto.user.perfil).toBe(B.perfil);
    expect(contexto.estadoAutenticacao).toBe('AUTENTICADO');
  });
  it.each([401, 403, 500])('erro %s de perfil A não limpa a sessão B', async status => {
    await montarA(); const antiga = diferida(); servico.buscarMeuPerfil.mockReturnValueOnce(antiga.promise);
    let leitura; act(() => { leitura = contexto.atualizarPerfil(); }); await sair(); await entrarB();
    await act(async () => { antiga.reject({ status, code: 'SESSION_IDLE_TIMEOUT', data: { action: 'LOGOUT' } }); await leitura.catch(() => null); });
    expect(contexto.user.username).toBe(B.username); expect(contexto.loggingOut).toBe(false);
  });
  it.each([401, 403, 500])('erro %s de sessão A não inicia logout nem notificação na sessão B', async status => {
    await montarA(); const antiga = diferida(); servico.informacoesSessao.mockReturnValueOnce(antiga.promise);
    let leitura; act(() => { leitura = contexto.atualizarInformacoesSessao({ touch: true }); }); await sair(); await entrarB();
    await act(async () => { antiga.reject({ status, code: 'SESSION_REPLACED', data: { action: 'LOGOUT' } }); await leitura.catch(() => null); });
    expect(contexto.user.username).toBe(B.username); expect(contexto.loggingOut).toBe(false);
    expect(sessionStorage.getItem('logout_reason')).toBeNull();
  });
  it('perfil mais recente prevalece e invalida dados carregados sob permissões revogadas', async () => {
    await montarA(); const antiga = diferida(); const nova = diferida();
    const antes = contexto.geracaoSessao;
    servico.buscarMeuPerfil.mockReturnValueOnce(antiga.promise).mockReturnValueOnce(nova.promise);
    let p1, p2; act(() => { p1 = contexto.atualizarPerfil(); p2 = contexto.atualizarPerfil(); });
    const restrito = { ...A, perfil: 'OPERACIONAL', autoridade: 'PERFIL_OPERACIONAL', gestaoLocalId: null, lotacaoId: null, jornadaId: null, vinculosGestao: [] };
    await act(async () => { nova.resolve(restrito); await p2; });
    expect(contexto.user.perfil).toBe('OPERACIONAL'); expect(contexto.geracaoSessao).not.toBe(antes);
    expect(contexto.user.gestaoLocalId).toBeNull(); expect(contexto.user.lotacaoId).toBeNull();
    await act(async () => { antiga.resolve(A); await p1; });
    expect(contexto.user.perfil).toBe('OPERACIONAL'); expect(contexto.user.gestaoLocalId).toBeNull();
  });
  it('leitura de sessão anterior não desfaz revalidação mais recente com touch', async () => {
    await montarA(); const antiga = diferida(); const nova = diferida();
    servico.informacoesSessao.mockReturnValueOnce(antiga.promise).mockReturnValueOnce(nova.promise);
    let p1, p2; act(() => { p1 = contexto.atualizarInformacoesSessao(); p2 = contexto.atualizarInformacoesSessao({ touch: true }); });
    const restrito = { ...A, perfil: 'OPERACIONAL', autoridade: 'PERFIL_OPERACIONAL' };
    await act(async () => { nova.resolve(sessao(restrito, 1800)); await p2; }); const prazo = contexto.sessionExpiresAtEpochMs;
    await act(async () => { antiga.resolve(sessao(A, 7200)); await p1; });
    expect(contexto.user.perfil).toBe('OPERACIONAL'); expect(contexto.sessionExpiresAtEpochMs).toBe(prazo);
  });
  it('erro antigo de perfil não desfaz uma revalidação bem-sucedida mais recente', async () => {
    await montarA(); const antiga = diferida(); servico.buscarMeuPerfil.mockReturnValueOnce(antiga.promise);
    let p1; act(() => { p1 = contexto.atualizarPerfil(); });
    servico.buscarMeuPerfil.mockResolvedValue({ ...A, username: 'nome-atual-sintetico' });
    await act(async () => contexto.atualizarPerfil());
    await act(async () => { antiga.reject({ status: 500 }); await p1; });
    expect(contexto.user.username).toBe('nome-atual-sintetico');
  });
  it('403 genérico da consulta não encerra uma sessão válida', async () => {
    await montarA(); servico.informacoesSessao.mockRejectedValueOnce({ status: 403, code: 'FORBIDDEN' });
    await act(async () => contexto.atualizarInformacoesSessao().catch(() => null));
    expect(contexto.user.username).toBe(A.username); expect(contexto.loggingOut).toBe(false);
  });
  it('null, false e lista vazia explícitos removem escopo e privilégios antigos', () => {
    const antiga = { ...A, superintendencia: { id: 8 }, gestaoLocal: { id: 1 }, jornada: { id: 20 }, lotacao: { id: 10 }, vinculosGestao: [{ lotacaoId: 10, jornadaId: 20 }], twoFactorVerified: true, isRetroactiveUnlocked: true, retroactiveExpireAt: '2030-01-01' };
    const proxima = normalizarUsuarioAutenticado(antiga, { ...A, superintendencia: null, gestaoLocal: null, jornada: null, lotacao: null, gestaoLocalId: null, lotacaoId: null, jornadaId: null, vinculosGestao: [], twoFactorVerified: false, isRetroactiveUnlocked: false, retroactiveExpireAt: null });
    expect(proxima.gestaoLocalId).toBeNull(); expect(proxima.lotacaoId).toBeNull(); expect(proxima.jornadaId).toBeNull(); expect(proxima.superintendenciaId).toBeNull();
    expect(proxima.twoFactorVerified).toBe(false); expect(proxima.isRetroactiveUnlocked).toBe(false); expect(proxima.retroactiveExpireAt).toBeNull();
  });
});
