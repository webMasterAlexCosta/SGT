import React from 'react';
import { act, cleanup, fireEvent, render, screen, waitFor, within } from '@testing-library/react';
import { afterEach, beforeEach, expect, it, vi } from 'vitest';
import '@testing-library/jest-dom/vitest';
const usuario = vi.hoisted(() => ({ id: 17, perfil: 'GESTOR_LOCAL', autoridade: 'PERFIL_GESTOR_LOCAL', twoFactorEnabled: true, twoFactorVerified: true }));
vi.mock('../src/modules/auth/context.tsx', () => ({ useAutenticacao: () => ({ user: usuario }) }));
import { LancamentoPorVinculo } from '../src/modules/rh/pages/PaginaFrequencia/components/LancamentoPorVinculo.tsx';
import { api } from '../src/core/http.ts';
import { invalidarGeracaoSessao } from '../src/core/security/geracaoSessao.ts';
import { resetarCoordenadorCsrfParaTestes } from '../src/core/http/coordenadorCsrf.ts';
import { redefinirIndicadorRedirecionamento } from '../src/core/events.ts';

const data = '2026-10-30';
const elegivel = {
  id: 301, pessoaId: 201, pessoaNome: 'Pessoa sintética atual', pessoaCpf: null,
  contratoId: 91, contratoNumero: 'LAB-91', contratoAno: 2026, matricula: 'LAB-301',
  dataReferencia: data, conhecidoAte: '2026-10-30T12:00:00Z', gestaoLocalId: 55,
  alocacaoId: 501, lotacaoId: 7, jornadaId: 8, cargoId: 6,
};
const ok = (config, body) => ({ config, data: body, status: 200, statusText: 'OK', headers: {} });
const erro = (config, status) => ({ config, response: { status, data: { message: 'Resultado sintético não confirmado' }, headers: {} } });
const adiar = () => { let resolve, reject; const promise = new Promise((r, j) => { resolve = r; reject = j; }); return { promise, resolve, reject }; };
let adapterOriginal, envios, salvar, aoSalvar;
beforeEach(() => {
  adapterOriginal = api.defaults.adapter; resetarCoordenadorCsrfParaTestes(); redefinirIndicadorRedirecionamento();
  envios = []; aoSalvar = vi.fn(); salvar = async () => ({ id: 701 });
  api.defaults.adapter = async config => {
    if (config.url === '/autenticacao/csrf') return ok(config, { headerName: 'X-XSRF-TOKEN', token: 'csrf-sintetico' });
    if (config.url === '/vinculos-contratuais/elegiveis-frequencia') return ok(config, { content: [elegivel], totalPages: 1, totalElements: 1, number: 0, size: 5, first: true, last: true, sort: null });
    if (config.url.includes('/pessoas/')) return ok(config, { pessoaId: 201, dataReferencia: data, vinculos: [{ vinculoId: 301, pessoaId: 201, contratoId: 91, dataReferencia: data, estadoNegocial: 'REGISTRADO', situacaoTemporal: 'VIGENTE' }] });
    if (config.url.endsWith('/alocacoes/posicao')) return ok(config, { vinculoId: 301, dataReferencia: data, situacaoTemporal: 'VIGENTE', alocacaoId: 501, lotacaoId: 7, jornadaId: 8, cargoId: 6 });
    if (config.url === '/frequencias' && config.method === 'post') { envios.push(JSON.parse(config.data)); return ok(config, await salvar(config)); }
    throw new Error('Requisição inesperada: ' + config.url);
  };
});
afterEach(() => { cleanup(); api.defaults.adapter = adapterOriginal; invalidarGeracaoSessao(); redefinirIndicadorRedirecionamento(); });
async function abrir() {
  render(<LancamentoPorVinculo dataReferencia={data} lotacaoId={7} jornadaId={8} contratoIds={[91]} usuario={usuario} gestorLocal podeLancar aoSalvar={aoSalvar} />);
  fireEvent.click(screen.getByRole('button', { name: 'Lançar por vínculo', exact: true }));
  fireEvent.click(await screen.findByRole('button', { name: 'Selecionar vínculo 301', exact: true }));
  await screen.findByText(/Posição validada/);
  fireEvent.change(screen.getByLabelText('Status da frequência'), { target: { value: 'PRESENCA' } });
  fireEvent.change(screen.getByLabelText('Observação do lançamento'), { target: { value: 'Intenção original explícita' } });
  fireEvent.change(screen.getByLabelText('Justificativa da retificação'), { target: { value: 'Conferência documental sintética' } });
}
async function confirmar() {
  fireEvent.click(screen.getByRole('button', { name: 'Confirmar lançamento', exact: true }));
  const dialogo = await screen.findByRole('dialog', { name: 'Confirmar lançamento por vínculo', exact: true });
  fireEvent.click(within(dialogo).getByRole('button', { name: 'Continuar', exact: true }));
  return dialogo;
}
async function fecharErro(dialogo) {
  await within(dialogo).findByRole('alert');
  fireEvent.click(within(dialogo).getByRole('button', { name: 'Cancelar', exact: true }));
  await waitFor(() => expect(screen.queryByRole('dialog', { name: 'Confirmar lançamento por vínculo', exact: true })).toBeNull());
}

it.each([403, 409, 503])('após%s fixa os campos e repete exclusivamente payload/chave originais', async status => {
  salvar = async config => { if (envios.length === 1) throw erro(config, status); return { id: 701 }; };
  await abrir(); await fecharErro(await confirmar());
  for (const label of ['Status da frequência', 'Observação do lançamento', 'Justificativa da retificação']) expect(screen.getByLabelText(label)).toBeDisabled();
  fireEvent.change(screen.getByLabelText('Observação do lançamento'), { target: { value: 'Alteração sem nova intenção' } });
  expect(screen.getByLabelText('Observação do lançamento')).toHaveValue('Intenção original explícita');
  expect(envios).toHaveLength(1);
  await confirmar(); await waitFor(() => expect(aoSalvar).toHaveBeenCalledTimes(1));
  expect(envios).toHaveLength(2); expect(envios[1]).toEqual(envios[0]);
});

it.each([403, 409, 503])('após%s exige ação explícita antes de editar e só então atribui outra chave', async status => {
  salvar = async config => { if (envios.length === 1) throw erro(config, status); return { id: 701 }; };
  await abrir(); await fecharErro(await confirmar());
  expect(screen.getByText(/Confira o resultado da tentativa anterior/)).toBeInTheDocument();
  const anterior = { ...envios[0] };
  fireEvent.click(screen.getByRole('button', { name: 'Editar como nova operação', exact: true }));
  expect(screen.getByLabelText('Observação do lançamento')).toBeEnabled();
  expect(envios).toHaveLength(1);
  fireEvent.change(screen.getByLabelText('Observação do lançamento'), { target: { value: 'Nova intenção declarada após conferência' } });
  await confirmar(); await waitFor(() => expect(aoSalvar).toHaveBeenCalledTimes(1));
  expect(envios).toHaveLength(2);
  expect(envios[1].chaveIdempotencia).not.toBe(anterior.chaveIdempotencia);
  expect(envios[1].observacao).toBe('Nova intenção declarada após conferência');
  expect(envios[1].vinculoContratualId).toBe(anterior.vinculoContratualId);
  expect(envios[1].dataReferencia).toBe(anterior.dataReferencia);
});

it('não permite criar outra intenção durante um POST pendente', async () => {
  const atraso = adiar(); let configAntiga;
  salvar = config => { configAntiga = config; return atraso.promise; };
  await abrir(); const dialogo = await confirmar();
  await waitFor(() => expect(envios).toHaveLength(1));
  const novaOperacao = screen.getByRole('button', { name: 'Editar como nova operação', exact: true });
  expect(novaOperacao).toBeDisabled(); fireEvent.click(novaOperacao);
  fireEvent.change(screen.getByLabelText('Observação do lançamento'), { target: { value: 'Conteúdo concorrente indevido' } });
  expect(screen.getByLabelText('Observação do lançamento')).toHaveValue('Intenção original explícita');
  await act(async () => { atraso.reject(erro(configAntiga, 503)); });
  await fecharErro(dialogo); expect(envios).toHaveLength(1);
  expect(screen.getByRole('button', { name: 'Editar como nova operação', exact: true })).toBeEnabled();
});

it('a confirmação real pode repetir a tentativa exata sem fechar seu diálogo', async () => {
  salvar = async config => { if (envios.length === 1) throw erro(config, 503); return { id: 701 }; };
  await abrir(); const dialogo = await confirmar(); await within(dialogo).findByRole('alert');
  fireEvent.click(within(dialogo).getByRole('button', { name: 'Continuar', exact: true }));
  await waitFor(() => expect(aoSalvar).toHaveBeenCalledTimes(1));
  expect(envios).toHaveLength(2); expect(envios[1]).toEqual(envios[0]);
});

it('fechar e reabrir descarta explicitamente a intenção e exige seleção nova', async () => {
  salvar = async config => { if (envios.length === 1) throw erro(config, 503); return { id: 701 }; };
  await abrir(); await fecharErro(await confirmar());
  fireEvent.click(screen.getByRole('button', { name: 'Fechar Lançar por vínculo', exact: true }));
  fireEvent.click(screen.getByRole('button', { name: 'Lançar por vínculo', exact: true }));
  expect(screen.queryByLabelText('Status da frequência')).not.toBeInTheDocument();
  fireEvent.click(await screen.findByRole('button', { name: 'Selecionar vínculo 301', exact: true }));
  await screen.findByText(/Posição validada/);
  fireEvent.change(screen.getByLabelText('Status da frequência'), { target: { value: 'FALTA' } });
  fireEvent.change(screen.getByLabelText('Justificativa da retificação'), { target: { value: 'Outra intenção sintética conferida' } });
  await confirmar(); await waitFor(() => expect(aoSalvar).toHaveBeenCalledTimes(1));
  expect(envios).toHaveLength(2); expect(envios[1].chaveIdempotencia).not.toBe(envios[0].chaveIdempotencia);
});
