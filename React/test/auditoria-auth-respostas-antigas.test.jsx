/**
 * Regressões adversariais independentes. Transporte controlado; NÃO são E2E
 * Java/MySQL/Redis nem demonstração de bypass de autorização no servidor.
 * Cada expectativa representa o comportamento seguro desejado e deve ficar
 * RED se uma resposta da sessão encerrada alterar o estado de autenticação.
 */
import React from 'react';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { act, cleanup, render, screen, waitFor } from '@testing-library/react';
import { ProvedorAutenticacao, useAutenticacao } from '@sgt/modules/auth/context.jsx';
import { redefinirIndicadorRedirecionamento } from '@sgt/core/events.js';
import { iniciarContextoSessaoIsolado } from './fixtures/contextoSessaoIsolado.js';

const { servico } = vi.hoisted(() => ({ servico: {
  informacoesSessao: vi.fn(), buscarMeuPerfil: vi.fn(), encerrarSessao: vi.fn(),
  autenticar: vi.fn(), cadastrar: vi.fn(),
} }));
vi.mock('@sgt/services/auth/ServicoAutenticacao', () => ({ ServicoAutenticacao: servico }));
vi.mock('@sgt/modules/auth/components/ModalReautenticacao', () => ({ ModalReautenticacao: () => null }));

const A = { id: 901, username: 'auditoria-a-sintetico', perfil: 'GESTOR_LOCAL', autoridade: 'PERFIL_GESTOR_LOCAL', gestaoLocalId: 1 };
const B = { id: 902, username: 'auditoria-b-sintetico', perfil: 'OPERACIONAL', autoridade: 'PERFIL_OPERACIONAL', gestaoLocalId: 2 };
const sessao = usuario => ({ authenticated: true, user: usuario, session: { remainingSeconds: 3600 } });
const diferida = () => {
  let resolver;
  const promessa = new Promise(resolve => { resolver = resolve; });
  return { promessa, resolver };
};
let contexto;
function Observador() {
  contexto = useAutenticacao();
  return <><output data-testid="usuario">{contexto.user?.username || 'sem-sessao'}</output>
    <output data-testid="estado">{contexto.estadoAutenticacao}</output>
    <output data-testid="perfil">{contexto.user?.perfil || 'sem-perfil'}</output></>;
}
const montarA = async () => {
  render(<ProvedorAutenticacao><Observador /></ProvedorAutenticacao>);
  await waitFor(() => expect(contexto.loading).toBe(false));
  expect(screen.getByTestId('usuario').textContent).toBe(A.username);
};
const sair = async () => {
  act(() => contexto.encerrarSessao());
  await act(async () => { await contexto.concluirSaida(); });
  expect(screen.getByTestId('usuario').textContent).toBe('sem-sessao');
};

beforeEach(() => {
  vi.clearAllMocks();
  localStorage.clear(); sessionStorage.clear(); contexto = null;
  iniciarContextoSessaoIsolado();
  redefinirIndicadorRedirecionamento();
  servico.informacoesSessao.mockReset().mockResolvedValue(sessao(A));
  servico.buscarMeuPerfil.mockReset().mockResolvedValue(A);
  servico.encerrarSessao.mockReset().mockResolvedValue(true);
  servico.autenticar.mockReset().mockResolvedValue(B);
});
afterEach(() => { cleanup(); });

describe('Fronteira de sessão: respostas antigas não podem publicar um usuário', () => {
  it('controle: logout concluído remove o usuário quando não há respostas pendentes', async () => {
    await montarA(); await sair();
    expect(contexto.estadoAutenticacao).toBe('NAO_AUTENTICADO');
  });

  it('RED: resposta de meu-perfil iniciada antes do logout não restaura a sessão encerrada', async () => {
    await montarA();
    const perfilAntigo = diferida();
    servico.buscarMeuPerfil.mockImplementationOnce(() => perfilAntigo.promessa);
    let atualizacao;
    act(() => { atualizacao = contexto.atualizarPerfil(); });
    await sair();
    await act(async () => { perfilAntigo.resolver(A); await atualizacao; });
    expect(screen.getByTestId('usuario').textContent).toBe('sem-sessao');
    expect(contexto.estadoAutenticacao).toBe('NAO_AUTENTICADO');
  });

  it('RED: resposta da sessão A não substitui usuário e perfil de B após novo login', async () => {
    await montarA();
    const sessaoAntiga = diferida();
    servico.informacoesSessao.mockImplementationOnce(() => sessaoAntiga.promessa);
    let leituraA;
    act(() => { leituraA = contexto.atualizarInformacoesSessao({ touch: true }); });
    await sair();
    servico.buscarMeuPerfil.mockResolvedValue(B);
    servico.informacoesSessao.mockResolvedValue(sessao(B));
    await act(async () => { await contexto.autenticar('matricula-sintetica-b', 'segredo-apenas-mock'); });
    expect(screen.getByTestId('usuario').textContent).toBe(B.username);
    expect(screen.getByTestId('perfil').textContent).toBe(B.perfil);
    await act(async () => { sessaoAntiga.resolver(sessao(A)); await leituraA; });
    expect(screen.getByTestId('usuario').textContent).toBe(B.username);
    expect(screen.getByTestId('perfil').textContent).toBe(B.perfil);
  });

  it('controle: SESSION_REPLACED inicia a saída ao consultar a sessão', async () => {
    await montarA();
    servico.informacoesSessao.mockRejectedValueOnce({ status: 401, code: 'SESSION_REPLACED', data: { action: 'LOGOUT' } });
    await act(async () => { await contexto.atualizarInformacoesSessao({ touch: false }).catch(() => {}); });
    expect(contexto.loggingOut).toBe(true);
  });

  it('RED: SESSION_IDLE_TIMEOUT emitido pelo Java também deve iniciar a saída', async () => {
    await montarA();
    servico.informacoesSessao.mockRejectedValueOnce({ status: 401, code: 'SESSION_IDLE_TIMEOUT', data: { action: 'LOGOUT' } });
    await act(async () => { await contexto.atualizarInformacoesSessao({ touch: false }).catch(() => {}); });
    expect(contexto.loggingOut).toBe(true);
  });
});
