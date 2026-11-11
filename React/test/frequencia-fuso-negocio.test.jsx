import { act, cleanup, renderHook } from '@testing-library/react';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';

const { atualizarPerfil } = vi.hoisted(() => ({ atualizarPerfil: vi.fn() }));
vi.mock('../src/modules/auth/context.tsx', () => ({ useAutenticacao: () => ({
  user: { id: 81, perfil: 'OPERACIONAL', autoridade: 'PERFIL_OPERACIONAL', vinculosGestao: [] }, atualizarPerfil,
}) }));
vi.mock('../src/core/security/useConfirmacaoSenha.tsx', () => ({ useConfirmacaoSenha: () => ({
  confirmarComSenha: () => Promise.resolve(false), ModalConfirmacao: () => null,
}) }));

import { useLogicaFrequencia } from '../src/modules/rh/pages/PaginaFrequencia/useLogicaFrequencia.ts';
import { api } from '../src/core/http.ts';
import { invalidarGeracaoSessao } from '../src/core/security/geracaoSessao.ts';

let adapterOriginal;

beforeEach(() => {
  vi.useFakeTimers({ toFake: ['Date', 'setInterval', 'clearInterval'] });
  vi.setSystemTime(new Date('2026-11-02T02:30:00Z'));
  adapterOriginal = api.defaults.adapter;
  api.defaults.adapter = async (config) => ({ config, data: [], status: 200, statusText: 'OK', headers: {} });
});

afterEach(() => {
  cleanup();
  api.defaults.adapter = adapterOriginal;
  invalidarGeracaoSessao();
  vi.useRealTimers();
});

// O prazo do cenário é fornecido pelo Java no início do dia em America/Sao_Paulo; N é configurável no servidor.
// O React somente representa esse instante; não decide autorização retroativa.
describe('Frequência no fuso de negócio confirmado pelo Java', () => {
  it('inicia a referência em 01/11 quando o UTC já está em 02/11', async () => {
    let hook;
    await act(async () => { hook = renderHook(() => useLogicaFrequencia()); });
    expect(hook.result.current.estado.dataReferenciaAtiva).toBe('2026-11-01');
  });

  it('recusa como futura uma data que só chegou no fuso do navegador', async () => {
    let hook;
    await act(async () => { hook = renderHook(() => useLogicaFrequencia()); });
    await act(async () => hook.result.current.acoes.setDataReferenciaAtiva('2026-11-02'));
    expect(hook.result.current.estado.statusDaDataValidada).toEqual({ valid: false, reason: 'future' });
  });

  it.each([
    ['antes', '2026-11-02T02:59:59.999Z', { isValid: true, remaining: '0m', isEndingSoon: true }],
    ['no limite', '2026-11-02T03:00:00.000Z', { isValid: false, remaining: 'encerrado', isEndingSoon: false }],
    ['depois', '2026-11-02T03:00:00.001Z', { isValid: false, remaining: 'encerrado', isEndingSoon: false }],
  ])('informa corretamente o prazo %s do instante fornecido pelo servidor', async (_caso, agora, esperado) => {
    vi.setSystemTime(new Date(agora));
    let hook;
    await act(async () => { hook = renderHook(() => useLogicaFrequencia()); });
    expect(hook.result.current.utilitarios.obterRelatorioDaJanelaDeEdicao('2026-11-02T00:00:00')).toEqual(esperado);
  });

  it.each(['2026-11-02T03:00:00Z', '2026-11-02T00:00:00-03:00'])('preserva o instante quando o prazo já traz offset: %s', async (prazo) => {
    let hook;
    await act(async () => { hook = renderHook(() => useLogicaFrequencia()); });
    expect(hook.result.current.utilitarios.obterRelatorioDaJanelaDeEdicao(prazo)).toMatchObject({ isValid: true, remaining: '30m' });
  });

  it.each(['2026-11-15T00:00:00', '2026-12-01T00:00:00'])('exibe prazo variável recebido do Java sem derivar N: %s', async (prazo) => {
    let hook;
    await act(async () => { hook = renderHook(() => useLogicaFrequencia()); });
    expect(hook.result.current.utilitarios.obterRelatorioDaJanelaDeEdicao(prazo).isValid).toBe(true);
  });

  it.each(['2026-10-01', '2026-09-15'])('não inventa limite retroativo local para consultar %s', async (referencia) => {
    let hook;
    await act(async () => { hook = renderHook(() => useLogicaFrequencia()); });
    await act(async () => hook.result.current.acoes.setDataReferenciaAtiva(referencia));
    expect(hook.result.current.estado.statusDaDataValidada).toEqual({ valid: true, reason: '' });
  });

  it('respeita o início de dia histórico ajustado pelo horário de verão de São Paulo', async () => {
    vi.setSystemTime(new Date('2018-11-04T02:59:00Z'));
    let hook;
    await act(async () => { hook = renderHook(() => useLogicaFrequencia()); });
    // ZonedDateTime.atStartOfDay retornava 01:00 neste dia de avanço do relógio.
    expect(hook.result.current.utilitarios.obterRelatorioDaJanelaDeEdicao('2018-11-04T01:00:00')).toMatchObject({ isValid: true, remaining: '1m' });
  });

  it('reavalia a data selecionada quando a virada do dia chega ao fuso de negócio', async () => {
    vi.setSystemTime(new Date('2026-11-02T01:59:59Z'));
    let hook;
    await act(async () => { hook = renderHook(() => useLogicaFrequencia()); });
    await act(async () => hook.result.current.acoes.setDataReferenciaAtiva('2026-11-03'));
    expect(hook.result.current.estado.statusDaDataValidada.valid).toBe(false);
    vi.setSystemTime(new Date('2026-11-03T03:00:00Z'));
    await act(async () => vi.advanceTimersByTime(60000));
    expect(hook.result.current.estado.statusDaDataValidada).toEqual({ valid: true, reason: '' });
  });
});
