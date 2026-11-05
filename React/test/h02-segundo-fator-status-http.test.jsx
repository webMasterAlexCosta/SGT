// @vitest-environment jsdom
import React from 'react';
import { AxiosError } from 'axios';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { cleanup, fireEvent, render, screen, waitFor } from '@testing-library/react';
import '@testing-library/jest-dom/vitest';
import { GerenciadorSegundoFator } from '../src/modules/perfil/components/GerenciadorSegundoFator/index.jsx';
import { api } from '../src/core/http.js';
import { configurarBuscadorTokenCsrf, resetarCoordenadorCsrfParaTestes } from '../src/core/http/coordenadorCsrf.js';
import { invalidarGeracaoSessao } from '../src/core/security/geracaoSessao.js';
import * as eventos from '../src/core/events.js';

// Serviço, interceptores, normalização e notificações HTTP reais. Apenas o adapter
// devolve respostas sintéticas: o teste precisa observar o efeito de silentError.
const ok = (config, data) => ({ config, data, status: 200, statusText: 'OK', headers: {} });
let originalAdapter;
beforeEach(() => {
  originalAdapter = api.defaults.adapter;
  invalidarGeracaoSessao();
  resetarCoordenadorCsrfParaTestes();
  eventos.redefinirIndicadorRedirecionamento();
  configurarBuscadorTokenCsrf(async ({ geracaoSessao }) => api.request({
    method: 'GET', url: '/autenticacao/csrf', retries: 0, silentError: true,
    skipAuthLogout: true, __csrfBootstrap: true, __geracaoSessao: geracaoSessao,
  }));
});
afterEach(() => {
  cleanup(); api.defaults.adapter = originalAdapter;
  resetarCoordenadorCsrfParaTestes(); vi.restoreAllMocks();
});

describe('H02: falha de leitura após mutação 2FA confirmada', () => {
  it.each(['configurar', 'ativar', 'desativar'])('%s preserva o estado confirmado pelo Java e comunica a falha posterior de status', async operacao => {
    const notificar = vi.spyOn(eventos, 'notificar');
    let enabled = operacao === 'desativar', setupPending = false, mutations = 0, failedReads = 0;
    const mensagem = {
      configurar: 'Consulta indisponível após configuração.',
      ativar: 'Consulta indisponível após ativação.',
      desativar: 'Consulta indisponível após desativação.',
    }[operacao];
    api.defaults.adapter = async config => {
      const method = config.method.toUpperCase();
      if (config.url === '/autenticacao/csrf') return ok(config, { headerName: 'X-XSRF-TOKEN', parameterName: '_csrf', token: 'csrf-sintetico-h02' });
      if (method === 'GET' && config.url === '/perfil/2fa/status') {
        if (mutations > 0) {
          failedReads += 1;
          throw new AxiosError(mensagem, AxiosError.ERR_BAD_RESPONSE, config, null, {
            config, data: { message: mensagem }, status: 500, statusText: 'Internal Server Error', headers: {},
          });
        }
        return ok(config, { enabled, setupPending });
      }
      if (method === 'POST' && config.url === '/perfil/2fa/configurar') {
        setupPending = true;
        if (operacao === 'configurar') mutations += 1;
        return ok(config, { secret: 'SEGREDO_SINTETICO_DO_TESTE', otpauthUri: 'otpauth://totp/sintetico', qrCodeDataUrl: 'data:image/svg+xml;base64,PHN2Zy8+' });
      }
      if ((method === 'POST' && config.url === '/perfil/2fa/ativar') || (method === 'DELETE' && config.url === '/perfil/2fa')) {
        mutations += 1; enabled = operacao === 'ativar'; setupPending = false;
        return ok(config, { enabled, message: 'Operação sintética confirmada.' });
      }
      throw new Error(`Transporte não previsto: ${method} ${config.url}`);
    };
    render(<GerenciadorSegundoFator />);
    await screen.findByLabelText('Senha atual');
    fireEvent.change(screen.getByLabelText('Senha atual'), { target: { value: 'senha-sintetica' } });
    if (operacao !== 'desativar') {
      fireEvent.click(screen.getByRole('button', { name: 'Gerar QR Code', exact: true }));
      await screen.findByRole('button', { name: 'Confirmar e ativar', exact: true });
    }
    if (operacao !== 'configurar') {
      fireEvent.change(screen.getByLabelText('Código do aplicativo autenticador'), { target: { value: '123456' } });
      fireEvent.click(screen.getByRole('button', { name: operacao === 'ativar' ? 'Confirmar e ativar' : 'Desativar segundo fator', exact: true }));
    }
    await waitFor(() => expect(failedReads).toBe(1));
    expect(mutations).toBe(1);
    await waitFor(() => expect(notificar).toHaveBeenCalledWith('error', mensagem));
    if (operacao === 'ativar') {
      expect(screen.getByText('Segundo fator ativo', { exact: true })).toBeInTheDocument();
      expect(screen.queryByAltText('QR Code para configurar o autenticador')).not.toBeInTheDocument();
    } else if (operacao === 'desativar') {
      expect(screen.getByRole('button', { name: 'Gerar QR Code', exact: true })).toBeEnabled();
      expect(screen.queryByText('Segundo fator ativo', { exact: true })).not.toBeInTheDocument();
    } else {
      expect(screen.getByRole('button', { name: 'Confirmar e ativar', exact: true })).toBeEnabled();
      expect(screen.getByAltText('QR Code para configurar o autenticador')).toBeInTheDocument();
    }
  });
});
