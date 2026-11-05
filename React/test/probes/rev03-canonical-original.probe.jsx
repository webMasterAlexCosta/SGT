// Independent real component + real confirmation dialog + real services/Axios.
// User context and transport are controlled. No Java, database or external login.
import React from 'react';
import { act, cleanup, fireEvent, render, screen, waitFor, within } from '@testing-library/react';
import { afterEach, beforeEach, expect, it, vi } from 'vitest';
const user = vi.hoisted(() => ({ id: 17, perfil: 'GESTOR_LOCAL', autoridade: 'PERFIL_GESTOR_LOCAL', twoFactorEnabled: true, twoFactorVerified: true }));
vi.mock('@review/modules/auth/context.jsx', () => ({ useAutenticacao: () => ({ user }) }));
import { LancamentoPorVinculo } from '@review/modules/rh/pages/PaginaFrequencia/components/LancamentoPorVinculo.jsx';
import { api } from '@review/core/http.js';
import { EVENT_SHOW_TOAST, redefinirIndicadorRedirecionamento } from '@review/core/events.js';
import { invalidarGeracaoSessao } from '@review/core/security/geracaoSessao.js';
import { resetarCoordenadorCsrfParaTestes } from '@review/core/http/coordenadorCsrf.js';

const D = '2026-10-30';
const scope = { lotacaoId: 7, jornadaId: 8 };
const link = (id = 301) => ({ id, pessoaId: id - 100, contratoId: 91, pessoaNome: 'Nome cadastral de consulta', matricula: id === 301 ? 'MAT-12' : 'MAT-123',
  estadoNegocial: 'REGISTRADO', situacaoTemporal: 'ENCERRADO', lotacaoInicialId: null, jornadaInicialId: 99 });
const person = (id, date = D) => ({ pessoaId: id, dataReferencia: date, conhecidoAte: '2026-11-01T12:00:00.000Z',
  vinculos: [{ vinculoId: id + 100, pessoaId: id, contratoId: 91, dataReferencia: date, estadoNegocial: 'REGISTRADO', situacaoTemporal: 'VIGENTE' }] });
const allocation = (id, date = D) => ({ vinculoId: id, dataReferencia: date, conhecidoAte: '2026-11-01T12:00:00.025Z', situacaoTemporal: 'VIGENTE', alocacaoId: id + 200, ...scope });
const ok = (config, data) => ({ config, data, status: 200, statusText: 'OK', headers: {} });
const failure = (config, status, message) => ({ config, response: { status, data: { code: status === 403 ? 'ACCESS_DENIED' : 'CONFLICT', message }, headers: {} } });
const delayed = () => { let resolve, reject; const promise = new Promise((a, b) => { resolve = a; reject = b; }); return { promise, resolve, reject }; };
let originalAdapter, requests, submissions, notices, observe, handlers, afterSave;
const props = (extra = {}) => ({ dataReferencia: D, ...scope, contratoIds: [91], contratos: [{ id: 91, numeroContrato: 'Contrato sintético' }], usuario: user,
  gestorLocal: true, podeLancar: true, aoSalvar: afterSave, ...extra });

beforeEach(() => {
  originalAdapter = api.defaults.adapter; resetarCoordenadorCsrfParaTestes(); redefinirIndicadorRedirecionamento();
  requests = []; submissions = []; notices = []; afterSave = vi.fn();
  observe = event => notices.push(event.detail); window.addEventListener(EVENT_SHOW_TOAST, observe);
  handlers = {
    list: async () => ({ content: [link(301), link(302)], totalPages: 1, totalElements: 2, number: 0, size: 5 }),
    person: async config => person(Number(config.url.split('/')[3]), config.params.data),
    allocation: async config => allocation(Number(config.url.split('/')[2]), config.params.data),
    save: async config => ({ id: 701, ...JSON.parse(config.data) }),
  };
  api.defaults.adapter = async config => {
    requests.push(config);
    if (config.url === '/autenticacao/csrf') return ok(config, { headerName: 'X-XSRF-TOKEN', token: 'token-exclusivamente-sintetico' });
    if (config.url === '/vinculos-contratuais') return ok(config, await handlers.list(config));
    if (config.url.includes('/pessoas/')) return ok(config, await handlers.person(config));
    if (config.url.endsWith('/alocacoes/posicao')) return ok(config, await handlers.allocation(config));
    if (config.url === '/frequencias' && config.method === 'post') {
      submissions.push(JSON.parse(config.data));
      return ok(config, await handlers.save(config));
    }
    throw new Error('Unexpected controlled request: ' + config.url);
  };
});
afterEach(() => {
  cleanup(); api.defaults.adapter = originalAdapter; window.removeEventListener(EVENT_SHOW_TOAST, observe);
  invalidarGeracaoSessao(); redefinirIndicadorRedirecionamento();
});
async function open(extra = {}) {
  const view = render(<LancamentoPorVinculo {...props(extra)} />);
  fireEvent.click(screen.getByRole('button', { name: 'Lançar por vínculo', exact: true }));
  await screen.findByRole('button', { name: 'Selecionar vínculo 301', exact: true });
  return view;
}
async function choose(id = 301) {
  fireEvent.click(screen.getByRole('button', { name: `Selecionar vínculo ${id}`, exact: true }));
  await screen.findByText(new RegExp(`Posição validada.*vínculo ${id}`));
}
function fill() {
  fireEvent.change(screen.getByLabelText('Status da frequência'), { target: { value: 'PRESENCA' } });
  fireEvent.change(screen.getByLabelText('Observação do lançamento'), { target: { value: 'Intenção sintética original' } });
  const justification = screen.queryByLabelText('Justificativa da retificação');
  if (justification) fireEvent.change(justification, { target: { value: 'Conferência independente do lançamento' } });
}
async function confirm() {
  fireEvent.click(screen.getByRole('button', { name: 'Confirmar lançamento', exact: true }));
  const dialog = await screen.findByRole('dialog', { name: 'Confirmar lançamento por vínculo', exact: true });
  fireEvent.click(within(dialog).getByRole('button', { name: 'Continuar', exact: true }));
  return dialog;
}
async function cancelFailedConfirmation(dialog) {
  await within(dialog).findByRole('alert');
  fireEvent.click(within(dialog).getByRole('button', { name: 'Cancelar', exact: true }));
  await waitFor(() => expect(screen.queryByRole('dialog', { name: 'Confirmar lançamento por vínculo', exact: true })).toBeNull());
}

it.each([true, false])('explicit selection uses the requested historical positions and sends no Employee projection; localManager=%s', async localManager => {
  await open({ gestorLocal: localManager });
  expect(requests.filter(r => r.url.includes('/posicao'))).toEqual([]);
  expect(screen.queryByLabelText('Status da frequência')).toBeNull();
  expect(requests[0].params).toMatchObject({ ativoEm: D, contratoId: 91, page: 0, size: 5 });
  await choose(302); fill(); await confirm();
  await waitFor(() => expect(afterSave).toHaveBeenCalledTimes(1));
  expect(submissions).toHaveLength(1);
  expect(submissions[0]).toMatchObject({ vinculoContratualId: 302, dataReferencia: D, status: 'PRESENCA' });
  expect(submissions[0]).not.toHaveProperty('empregado'); expect(submissions[0]).not.toHaveProperty('empregadoId');
  expect(submissions[0]).not.toHaveProperty('pessoaId'); expect(submissions[0]).not.toHaveProperty('cpf');
  if (localManager) expect(submissions[0].chaveIdempotencia.length).toBeGreaterThanOrEqual(8);
  expect(requests.some(r => r.url.startsWith('/empregados'))).toBe(false);
});

it.each(['outer-person', 'outer-date', 'missing-link', 'duplicate-link', 'wrong-contract', 'wrong-person', 'wrong-date'])('a conflicting canonical person chain blocks submission: %s', async mismatch => {
  handlers.person = async () => {
    const p = person(201);
    if (mismatch === 'outer-person') p.pessoaId = 202;
    if (mismatch === 'outer-date') p.dataReferencia = '2026-10-29';
    if (mismatch === 'missing-link') p.vinculos = [person(202).vinculos[0]];
    if (mismatch === 'duplicate-link') p.vinculos.push({ ...p.vinculos[0] });
    if (mismatch === 'wrong-contract') p.vinculos[0].contratoId = 92;
    if (mismatch === 'wrong-person') p.vinculos[0].pessoaId = 202;
    if (mismatch === 'wrong-date') p.vinculos[0].dataReferencia = '2026-10-29';
    return p;
  };
  await open(); fireEvent.click(screen.getByRole('button', { name: 'Selecionar vínculo 301', exact: true }));
  await screen.findByRole('alert');
  expect(screen.queryByLabelText('Status da frequência')).toBeNull(); expect(submissions).toEqual([]);
});

it.each([{ lotacaoId: 8 }, { jornadaId: 7 }, { dataReferencia: '2026-10-29' }, { vinculoId: 302 }, { alocacaoId: null }])('a position from another date/link/pair cannot authorize the form: %j', async mismatch => {
  handlers.allocation = async () => ({ ...allocation(301), ...mismatch });
  await open(); fireEvent.click(screen.getByRole('button', { name: 'Selecionar vínculo 301', exact: true }));
  await screen.findByRole('alert'); expect(screen.queryByLabelText('Status da frequência')).toBeNull(); expect(submissions).toEqual([]);
});

it.each(['success', 'error'])('old selected-link position %s cannot replace B or notify after explicit selection B', async outcome => {
  const old = delayed(); let oldConfig;
  handlers.allocation = config => {
    if (config.url.includes('/301/')) { oldConfig = config; return old.promise; }
    return allocation(302);
  };
  await open(); fireEvent.click(screen.getByRole('button', { name: 'Selecionar vínculo 301', exact: true }));
  await waitFor(() => expect(oldConfig).toBeDefined());
  await choose(302); notices.length = 0;
  await act(async () => { if (outcome === 'success') old.resolve(allocation(301)); else old.reject(failure(oldConfig, 403, 'Posição antiga recusada')); });
  expect(screen.getByText(/Posição validada.*vínculo 302/)).toBeTruthy();
  expect(screen.queryByRole('alert')).toBeNull(); expect(notices).toEqual([]);
  fill(); await confirm(); await waitFor(() => expect(afterSave).toHaveBeenCalledTimes(1));
  expect(submissions[0].vinculoContratualId).toBe(302);
});

it.each([{ dataReferencia: '2026-10-29' }, { jornadaId: 9 }])('context change invalidates a running position without late errors: %j', async change => {
  const old = delayed(); let oldConfig;
  handlers.allocation = config => { oldConfig = config; return old.promise; };
  const view = await open(); fireEvent.click(screen.getByRole('button', { name: 'Selecionar vínculo 301', exact: true }));
  await waitFor(() => expect(oldConfig).toBeDefined());
  view.rerender(<LancamentoPorVinculo {...props(change)} />);
  await act(async () => { old.reject(failure(oldConfig, 409, 'Posição de recorte encerrado')); });
  expect(screen.queryByRole('dialog')).toBeNull(); expect(notices).toEqual([]); expect(submissions).toEqual([]);
});

it.each([403, 409, 503])('current refusal %s retains fields and exact retry key, without automatic replay', async status => {
  handlers.save = async config => { if (submissions.length === 1) throw failure(config, status, 'Recusa sintética revisável'); return { id: 701 }; };
  await open(); await choose(); fill();
  await cancelFailedConfirmation(await confirm());
  expect(submissions).toHaveLength(1); expect(afterSave).not.toHaveBeenCalled();
  expect(screen.getByLabelText('Observação do lançamento').value).toBe('Intenção sintética original');
  expect(screen.getByLabelText('Justificativa da retificação').value).toBe('Conferência independente do lançamento');
  await confirm(); await waitFor(() => expect(afterSave).toHaveBeenCalledTimes(1));
  expect(submissions).toHaveLength(2); expect(submissions[1]).toEqual(submissions[0]);
});

it('session invalidation during canonical POST suppresses old effects and preserves current-generation operation', async () => {
  const old = delayed(); let oldConfig;
  handlers.save = config => { oldConfig = config; return old.promise; };
  const first = await open(); await choose(); fill(); await confirm();
  await waitFor(() => expect(oldConfig).toBeDefined());
  act(() => invalidarGeracaoSessao()); first.unmount(); notices.length = 0;
  await act(async () => { old.resolve({ id: 700 }); });
  expect(afterSave).not.toHaveBeenCalled(); expect(notices).toEqual([]);
  handlers.save = async () => ({ id: 701 });
  await open(); await choose(302); fill(); await confirm();
  await waitFor(() => expect(afterSave).toHaveBeenCalledTimes(1));
  expect(submissions).toHaveLength(2); expect(submissions[1].vinculoContratualId).toBe(302);
});

it('a changed payload must not be sent with the key of an unresolved earlier intention', async () => {
  handlers.save = async config => { throw failure(config, submissions.length === 1 ? 503 : 409, 'Resultado sintético não confirmado'); };
  await open(); await choose(); fill();
  await cancelFailedConfirmation(await confirm());
  fireEvent.change(screen.getByLabelText('Observação do lançamento'), { target: { value: 'Conteúdo alterado depois da resposta incerta' } });
  await cancelFailedConfirmation(await confirm());
  expect(submissions).toHaveLength(2);
  expect(submissions[1].observacao).not.toBe(submissions[0].observacao);
  // Two acceptable designs: prevent changed-content submission until resolution,
  // or make a new intention explicit. Reusing the original key for changed bytes
  // creates a collision if the first command was already committed by the Java.
  expect(submissions[1].chaveIdempotencia).not.toBe(submissions[0].chaveIdempotencia);
});
