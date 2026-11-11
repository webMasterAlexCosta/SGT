// Transportes simulados; não é prova de servidor nem usa sessão/credenciais reais.
import React from 'react';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { cleanup, fireEvent, render, screen, waitFor } from '@testing-library/react';
import '@testing-library/jest-dom/vitest';
vi.mock('../src/modules/auth/context.tsx', () => ({ useAutenticacao: () => ({ user: { perfil: 'GESTOR_LOCAL', autoridade: 'PERFIL_GESTOR_LOCAL' } }) }));
vi.mock('../src/core/events.ts', () => ({ notificar: vi.fn(), dispararSaida: vi.fn(), dispararConexaoPerdida: vi.fn(), dispararConexaoRestaurada: vi.fn() }));
import { api } from '../src/core/http.ts';
import { resetarCoordenadorCsrfParaTestes } from '../src/core/http/coordenadorCsrf.ts';
import { resetarSolicitadorConfirmacaoSenhaParaTestes } from '../src/core/http/coordenadorConfirmacaoSenha.ts';
import { ProvedorConfirmacaoSenhaHttp } from '../src/core/security/ProvedorConfirmacaoSenhaHttp.tsx';
import { FormularioProvaHistorica } from '../src/modules/historico/FormularioProvaHistorica.tsx';
let gravacoes;
let adapterOriginal;
beforeEach(() => {
  gravacoes = [];
  adapterOriginal = api.defaults.adapter;
  resetarCoordenadorCsrfParaTestes();
  resetarSolicitadorConfirmacaoSenhaParaTestes();
  api.defaults.adapter = async config => {
    const responder = data => ({ data, status: 200, statusText: 'OK', headers: {}, config });
    if (config.url === '/autenticacao/csrf') return responder({ headerName: 'X-XSRF-TOKEN', parameterName: '_csrf', token: 'csrf-sintetico' });
    if (config.url.endsWith('/previa')) return responder({ tokenPrevia: 'previa-sintetica', impactos: ['Impacto sintético.'] });
    if (!config.headers.get('X-User-Password')) throw { config, response: { status: 428, headers: {}, data: { code: 'PASSWORD_CONFIRMATION_REQUIRED', message: 'Confirme a senha.' } } };
    gravacoes.push({ url: config.url, data: config.data });
    return responder({ eventoId: 99, reenvio: false });
  };
});
afterEach(() => { cleanup(); api.defaults.adapter = adapterOriginal; resetarSolicitadorConfirmacaoSenhaParaTestes(); });
const conteudo = ativo => <ProvedorConfirmacaoSenhaHttp>{ativo ? <FormularioProvaHistorica tipo="catalogo" alvo={{ id: 8, tipoCatalogo: 'CARGO' }} /> : <h1>Outra rota</h1>}</ProvedorConfirmacaoSenhaHttp>;
describe('Cancelamento da intenção histórica antes de enviar a senha', () => {
  it('encerra a confirmação se o formulário é desmontado por mudança de rota', async () => {
    const view = render(conteudo(true));
    for (const [campo, valor] of [
      ['Data efetiva da transição', '2026-01-01'], ['Disponibilidade documentada', 'false'],
      ['Versão esperada do catálogo', '0'], ['Motivo / justificativa', 'Ato sintético'],
      ['Evidência da transição', 'SEI-SINTETICO'], ['Autorização', 'AUT-SINTETICA'],
    ]) fireEvent.change(screen.getByLabelText(campo), { target: { value: valor } });
    fireEvent.click(screen.getByRole('button', { name: 'Gerar prévia' }));
    await screen.findByText('Impacto sintético.');
    fireEvent.click(screen.getByRole('checkbox', { name: /Li os impactos/i }));
    fireEvent.submit(screen.getByRole('form', { name: 'Fonte histórica' }));
    await screen.findByRole('dialog');
    view.rerender(conteudo(false));
    expect(screen.queryByRole('form', { name: 'Fonte histórica' })).not.toBeInTheDocument();
    // Na candidata03 o diálogo global sobrevive e o clique ainda dispara o replay.
    if (screen.queryByRole('dialog')) {
      fireEvent.change(screen.getByLabelText(/Senha de Acesso/), { target: { value: 'senha-sintetica' } });
      fireEvent.click(screen.getByRole('button', { name: 'Confirmar' }));
      await waitFor(() => expect(gravacoes).toHaveLength(1));
    }
    expect(gravacoes).toHaveLength(0);
  });
});
