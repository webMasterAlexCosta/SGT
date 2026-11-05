// @vitest-environment jsdom
import React from 'react';
import { afterEach, describe, expect, it, vi } from 'vitest';
import { act, cleanup, fireEvent, render, screen, waitFor } from '@testing-library/react';
import '@testing-library/jest-dom/vitest';

const { requisitar } = vi.hoisted(() => ({ requisitar: vi.fn() }));
vi.mock('../src/core/http.js', () => ({ requisitar }));
import { GerenciadorSegundoFator } from '../src/modules/perfil/components/GerenciadorSegundoFator/index.jsx';
import * as eventos from '../src/core/events.js';

afterEach(() => { cleanup(); vi.restoreAllMocks(); });

// Os serviços e eventos da UI são reais, com rejeições controladas na fronteira
// HTTP. Além das asserções abaixo, o Vitest deve terminar sem Unhandled Rejection.
// Não instalamos um tratador que absorva essas exceções: no H01 elas tornam o
// processo vermelho, como o pageerror observado no Chrome real.
describe('H02: recusas e cancelamentos de 2FA permitem corrigir e tentar novamente', () => {
  it.each([
    ['ativar', 'recusa HTTP', { status: 400, message: 'Código TOTP inválido ou expirado.' }],
    ['ativar', 'cancelamento', { canceled: true, message: 'Contexto de sessão mudou.' }],
    ['ativar', 'erro inesperado', { message: 'Falha local inesperada ao ativar.' }],
    ['desativar', 'recusa HTTP', { status: 400, message: 'Senha ou código TOTP inválido.' }],
    ['desativar', 'cancelamento', { canceled: true, message: 'Contexto de sessão mudou.' }],
    ['desativar', 'erro inesperado', { message: 'Falha local inesperada ao desativar.' }],
  ])('%s: trata %s, conserva o formulário e aceita uma nova tentativa', async (operacao, tipoErro, erro) => {
    const notificar = vi.spyOn(eventos, 'notificar');
    let enabled = operacao === 'desativar', setupPending = false, tentativas = 0;
    requisitar.mockReset();
    requisitar.mockImplementation(async ({ method, url }) => {
      if (method === 'GET' && url === '/perfil/2fa/status') return { enabled, setupPending };
      if (method === 'POST' && url === '/perfil/2fa/configurar') {
        setupPending = true;
        return { secret: 'SEGREDO_SINTETICO_DO_TESTE', otpauthUri: 'otpauth://totp/sintetico', qrCodeDataUrl: 'data:image/svg+xml;base64,PHN2Zy8+' };
      }
      if ((operacao === 'ativar' && method === 'POST' && url === '/perfil/2fa/ativar')
        || (operacao === 'desativar' && method === 'DELETE' && url === '/perfil/2fa')) {
        tentativas += 1;
        if (tentativas === 1) throw Object.assign(new Error(erro.message), erro);
        enabled = operacao === 'ativar'; setupPending = false;
        return { enabled, setupPending };
      }
      throw new Error(`Transporte não previsto: ${method} ${url}`);
    });
    render(<GerenciadorSegundoFator />);
    await screen.findByLabelText('Senha atual');
    fireEvent.change(screen.getByLabelText('Senha atual'), { target: { value: 'senha-sintetica' } });
    if (operacao === 'ativar') {
      fireEvent.click(screen.getByRole('button', { name: 'Gerar QR Code', exact: true }));
      await screen.findByRole('button', { name: 'Confirmar e ativar', exact: true });
    }
    const nome = operacao === 'ativar' ? 'Confirmar e ativar' : 'Desativar segundo fator';
    fireEvent.change(screen.getByLabelText('Código do aplicativo autenticador'), { target: { value: '111111' } });
    fireEvent.click(screen.getByRole('button', { name: nome, exact: true }));
    await waitFor(() => expect(tentativas).toBe(1));
    await act(async () => { await new Promise(resolve => setTimeout(resolve, 0)); });
    expect(screen.getByRole('button', { name: nome, exact: true })).toBeEnabled();
    expect(screen.getByLabelText('Código do aplicativo autenticador')).toHaveValue('111111');
    if (tipoErro === 'erro inesperado') {
      expect(notificar).toHaveBeenCalledWith('error', erro.message);
    } else {
      expect(notificar.mock.calls.filter(([tipo]) => tipo === 'error')).toHaveLength(0);
    }
    fireEvent.change(screen.getByLabelText('Código do aplicativo autenticador'), { target: { value: '222222' } });
    fireEvent.click(screen.getByRole('button', { name: nome, exact: true }));
    if (operacao === 'ativar') await screen.findByText('Segundo fator ativo', { exact: true });
    else await screen.findByRole('button', { name: 'Gerar QR Code', exact: true });
    expect(tentativas).toBe(2);
  });
});
