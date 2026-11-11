// @vitest-environment jsdom
import React, { useState } from 'react';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { act, cleanup, fireEvent, render, screen } from '@testing-library/react';
import '@testing-library/jest-dom/vitest';

const { requisitar } = vi.hoisted(() => ({ requisitar: vi.fn() }));
vi.mock('../src/core/http.ts', () => ({ requisitar }));
import { GerenciadorSegundoFator } from '../src/modules/perfil/components/GerenciadorSegundoFator/index.tsx';

// UI e serviço reais; só a fronteira de transporte usa DTOs sintéticos.
// O pai reproduz o callback inline da página que sincroniza o perfil e renderiza
// novamente. O limite de uma sincronização evita uma tempestade no processo de teste.
function PaiQueSincronizaPerfil() {
  const [sincronizado, setSincronizado] = useState(false);
  return <>
    <output aria-label="Perfil sincronizado">{sincronizado ? 'Sim' : 'Não'}</output>
    <GerenciadorSegundoFator onStatusChange={() => { if (!sincronizado) setSincronizado(true); }} />
  </>;
}

beforeEach(() => {
  requisitar.mockReset();
  requisitar.mockResolvedValue({ enabled: false, setupPending: false });
});
afterEach(cleanup);

describe('H02: consulta 2FA não depende da identidade do callback de sincronização', () => {
  it('consulta uma vez ao abrir, mesmo quando o status recebido faz o pai renderizar novamente', async () => {
    render(<PaiQueSincronizaPerfil />);
    await screen.findByRole('button', { name: 'Gerar QR Code', exact: true });
    await act(async () => {});
    expect(screen.getByLabelText('Perfil sincronizado')).toHaveTextContent('Sim');
    expect(requisitar.mock.calls.filter(([r]) => r.url === '/perfil/2fa/status')).toHaveLength(1);
  });

  it('trocar o callback preserva o recurso carregado e entrega a próxima atualização ao pai atual', async () => {
    const anterior = vi.fn(), atual = vi.fn();
    let configurado = false;
    requisitar.mockImplementation(async ({ url, method }) => {
      if (method === 'POST' && url === '/perfil/2fa/configurar') {
        configurado = true;
        return { secret: 'SEGREDO_SINTETICO_DO_TESTE', otpauthUri: 'otpauth://totp/sintetico', qrCodeDataUrl: 'data:image/svg+xml;base64,PHN2Zy8+' };
      }
      if (url === '/perfil/2fa/status') return { enabled: false, setupPending: configurado };
      throw new Error(`Requisição não prevista: ${method} ${url}`);
    });
    const { rerender } = render(<GerenciadorSegundoFator onStatusChange={anterior} />);
    await screen.findByRole('button', { name: 'Gerar QR Code', exact: true });
    expect(anterior).toHaveBeenCalledExactlyOnceWith(false);

    rerender(<GerenciadorSegundoFator onStatusChange={atual} />);
    await act(async () => {});
    expect(requisitar.mock.calls.filter(([r]) => r.url === '/perfil/2fa/status')).toHaveLength(1);
    expect(atual).not.toHaveBeenCalled();
    fireEvent.change(screen.getByLabelText('Senha atual'), { target: { value: 'senha-sintetica' } });
    fireEvent.click(screen.getByRole('button', { name: 'Gerar QR Code', exact: true }));
    await screen.findByRole('button', { name: 'Confirmar e ativar', exact: true });
    expect(atual).toHaveBeenCalledExactlyOnceWith(false);
    expect(anterior).toHaveBeenCalledTimes(1);
    expect(requisitar.mock.calls.filter(([r]) => r.url === '/perfil/2fa/status')).toHaveLength(2);
  });
});
