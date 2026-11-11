import React from 'react';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { act, cleanup, render, screen } from '@testing-library/react';
import { useConfirmacaoSenha } from '../src/core/security/useConfirmacaoSenha.tsx';
import { invalidarGeracaoSessao } from '../src/core/security/geracaoSessao.ts';
vi.mock('../src/modules/auth/context.tsx', () => ({ useAutenticacao: () => ({ user: { twoFactorEnabled: false } }) }));
let dialogo, confirmacao;
vi.mock('../src/core/security/ModalConfirmacaoSenha', () => ({ ModalConfirmacaoSenha: props => { dialogo = props; return props.aberto ? <div role="dialog">{props.titulo}</div> : null; } }));
const Observador = () => { confirmacao = useConfirmacaoSenha(); const Modal = confirmacao.ModalConfirmacao; return <Modal />; };
const diferida = () => { let resolve, reject; const promise = new Promise((a,b) => { resolve=a; reject=b; }); return { promise, resolve, reject }; };
const pedir = (acao, titulo = 'Ação sintética') => { let promessa; act(() => { promessa = confirmacao.confirmarComSenha({ titulo, acao }); }); return promessa; };
beforeEach(() => invalidarGeracaoSessao());
afterEach(() => cleanup());
describe('Intenção de confirmação pertence à sessão e ao componente', () => {
  it('desmontagem resolve a intenção como cancelada e não executa callback guardado', async () => {
    const view = render(<Observador />); let executadas = 0, resultado = 'pendente';
    pedir(() => { executadas += 1; }).then(r => { resultado = r; }); const callbackAntigo = dialogo.aoConfirmar;
    view.unmount(); await act(async () => { await callbackAntigo('senha-sintetica'); });
    expect(resultado).toBe(false); expect(executadas).toBe(0);
  });
  it('mudança de sessão cancela imediatamente diálogo e promessa abertos', async () => {
    render(<Observador />); let resultado = 'pendente'; pedir(() => true).then(r => { resultado = r; });
    await act(async () => { invalidarGeracaoSessao(); });
    expect(resultado).toBe(false); expect(screen.queryByRole('dialog')).toBeNull();
  });
  it('resultado antigo não resolve uma intenção mais recente nem dispara a sua ação', async () => {
    render(<Observador />); const antiga = diferida(); let resultadoA = 'pendente', resultadoB = 'pendente', executadasB = 0;
    pedir(() => antiga.promise, 'Intenção A').then(r => { resultadoA = r; });
    let execucaoA; act(() => { execucaoA = dialogo.aoConfirmar('senha-a-sintetica'); });
    pedir(() => { executadasB += 1; return true; }, 'Intenção B').then(r => { resultadoB = r; });
    await act(async () => { antiga.resolve(true); await execucaoA; });
    expect(resultadoA).toBe(false); expect(resultadoB).toBe('pendente'); expect(executadasB).toBe(0);
    expect(screen.getByRole('dialog').textContent).toBe('Intenção B');
    await act(async () => { await dialogo.aoConfirmar('senha-b-sintetica'); });
    expect(resultadoB).toBe(true); expect(executadasB).toBe(1);
  });
  it('erro de ação antiga é silencioso e não altera a intenção atual', async () => {
    render(<Observador />); const antiga = diferida(); pedir(() => antiga.promise, 'Intenção A');
    let execucao; act(() => { execucao = dialogo.aoConfirmar('senha-sintetica').catch(erro => erro); });
    await act(async () => { invalidarGeracaoSessao(); }); pedir(() => true, 'Intenção B');
    let resultado; await act(async () => { antiga.reject({ status: 401, code: 'SESSION_REPLACED' }); resultado = await execucao; });
    expect(resultado).toBe(false); expect(screen.getByRole('dialog').textContent).toBe('Intenção B');
  });
});
