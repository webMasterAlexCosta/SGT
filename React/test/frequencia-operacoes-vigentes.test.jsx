import { act, cleanup, renderHook, waitFor } from '@testing-library/react';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';

const observador = vi.hoisted(() => ({ notificar: vi.fn(), atualizarPerfil: vi.fn(), confirmar: vi.fn() }));
vi.mock('../src/core/events.js', async original => ({ ...(await original()), notificar: observador.notificar }));
vi.mock('../src/modules/auth/context.jsx', () => ({ useAutenticacao: () => ({
  user: { id: 81, perfil: 'GESTOR_LOCAL', autoridade: 'PERFIL_GESTOR_LOCAL', vinculosGestao: [] }, atualizarPerfil: observador.atualizarPerfil,
}) }));
vi.mock('../src/core/security/useConfirmacaoSenha.jsx', () => ({ useConfirmacaoSenha: () => ({ confirmarComSenha: observador.confirmar, ModalConfirmacao: () => null }) }));
// Executa o algoritmo real em vários lotes com uma página real de cinco linhas.
// Só o limite do lote é reduzido para não exigir 1.001 rascunhos nesta prova.
vi.mock('../src/services/rh/ServicoDominioFrequencia.js', async original => ({ ...(await original()), TAMANHO_MAXIMO_LOTE_FREQUENCIA: 2 }));
import { useLogicaFrequencia } from '../src/modules/rh/pages/PaginaFrequencia/useLogicaFrequencia.js';
import { api } from '../src/core/http.js';
import { invalidarGeracaoSessao, obterGeracaoSessao } from '../src/core/security/geracaoSessao.js';
import { resetarCoordenadorCsrfParaTestes } from '../src/core/http/coordenadorCsrf.js';
import { redefinirIndicadorRedirecionamento } from '../src/core/events.js';

const adiar = () => { let resolve, reject; const promise = new Promise((r, j) => { resolve = r; reject = j; }); return { promise, resolve, reject }; };
const ok = (config, data) => ({ config, data, status: 200, statusText: 'OK', headers: {} });
const erro = config => ({ config, response: { status: 409, data: { code: 'CONFLICT', message: 'Conflito sintético do envio anterior' }, headers: {} } });
const empregados = Array.from({ length: 5 }, (_, i) => ({ id: 17 + i, nome: `Pessoa sintética ${i + 1}`, lotacaoId: 7, turnoId: 8 }));
const frequencias = empregados.map((emp, i) => ({ id: 71 + i, empregado: { id: emp.id }, vinculoContratualId: 501 + i, status: 'PRESENCA', validadeOperacional: 'VALIDA', estadoNegocial: 'ABERTA' }));
let adapterOriginal, pedidos, responder, fechamentoInicial, confirmacoes;
beforeEach(() => {
  adapterOriginal = api.defaults.adapter;
  resetarCoordenadorCsrfParaTestes(); redefinirIndicadorRedirecionamento();
  observador.notificar.mockReset(); observador.atualizarPerfil.mockReset(); observador.confirmar.mockReset();
  pedidos = []; responder = undefined; fechamentoInicial = null; confirmacoes = [];
  observador.confirmar.mockImplementation(({ acao }) => { confirmacoes.push(acao); return acao('senha-sintetica'); });
  api.defaults.adapter = async config => {
    pedidos.push({ config, geracao: obterGeracaoSessao() });
    const personalizado = responder?.(config);
    if (personalizado !== undefined) return personalizado;
    if (config.url === '/autenticacao/csrf') return ok(config, { headerName: 'X-XSRF-TOKEN', token: 'csrf-sintetico' });
    if (config.url === '/lotacoes') return ok(config, [{ id: 7, nome: 'Lotação sintética', turnos: [{ id: 8, descricao: 'Turno sintético' }] }]);
    if (config.url === '/contratos') return ok(config, [{ id: 9, ativo: true }]);
    if (config.url === '/jornadas') return ok(config, [{ id: 8, descricao: 'Turno sintético', dias: [] }]);
    if (config.url === '/empregados/filtrados-frequencia') return ok(config, { content: empregados, totalPages: 1, totalElements: 5, number: 0 });
    if (config.url === '/frequencias' && config.method === 'get') return ok(config, frequencias);
    if (config.url === '/frequencias' && config.method === 'post') return ok(config, JSON.parse(config.data));
    if (config.url === '/frequencias/lote') return ok(config, JSON.parse(config.data));
    if (config.url === '/frequencias/fechamentos/atual' || config.url === '/frequencias/fechamentos/601') return ok(config, fechamentoInicial);
    return ok(config, []);
  };
});
afterEach(() => { cleanup(); api.defaults.adapter = adapterOriginal; invalidarGeracaoSessao(); vi.useRealTimers(); vi.restoreAllMocks(); });

async function carregar() {
  const hook = renderHook(() => useLogicaFrequencia());
  await waitFor(() => expect(hook.result.current.estado.listaDaFrequencia).toHaveLength(5));
  return hook;
}
function editar(hook, quantidade = 1) {
  act(() => {
    hook.result.current.acoes.setJustificativaRetificacao('Conferência sintética da geração atual');
    for (const emp of empregados.slice(0, quantidade)) hook.result.current.acoes.invocarAlteracaoDeAnotacaoDeObservacao(emp.id, 'Rascunho sintético');
  });
}
const envios = () => pedidos.filter(p => p.config.method === 'post' && ['/frequencias', '/frequencias/lote'].includes(p.config.url));
async function iniciarEnvio(hook) {
  let envio;
  await act(async () => { envio = hook.result.current.acoes.invocarGatilhoDeDespachoAoServidorGlobal(); });
  return { envio };
}

describe('Gravação de frequência vinculada à montagem, sessão e intenção', () => {
  it('não recarrega nem notifica na sessão B após falha de um lote parcial da sessão A', async () => {
    const atrasado = adiar(); let segundo;
    responder = config => {
      if (config.url === '/frequencias/lote' && envios().length === 2) { segundo = config; return atrasado.promise; }
    };
    const hook = await carregar(); editar(hook, 5);
    const { envio } = await iniciarEnvio(hook);
    await waitFor(() => expect(segundo).toBeDefined());
    hook.unmount(); invalidarGeracaoSessao(); invalidarGeracaoSessao(); redefinirIndicadorRedirecionamento();
    const antes = pedidos.length; observador.notificar.mockClear();
    await act(async () => { atrasado.reject(erro(segundo)); await envio; });
    expect(pedidos.slice(antes)).toEqual([]);
    expect(observador.notificar.mock.calls).toEqual([]);
  });

  it('não inicia o lote seguinte nem recarrega o recorte antigo após mudar a data', async () => {
    const atrasado = adiar(); let primeiro;
    responder = config => {
      if (config.url === '/frequencias/lote' && !primeiro) { primeiro = config; return atrasado.promise; }
    };
    const hook = await carregar(); editar(hook, 5);
    const { envio } = await iniciarEnvio(hook);
    await waitFor(() => expect(primeiro).toBeDefined());
    act(() => hook.result.current.acoes.aoAcionarMudarDataPeloCabecalho(-1));
    await waitFor(() => expect(hook.result.current.estado.indicadorCarregamento).toBe(false));
    const antes = pedidos.length; observador.notificar.mockClear();
    await act(async () => { atrasado.resolve(ok(primeiro, JSON.parse(primeiro.data))); await envio; });
    expect(envios()).toHaveLength(1);
    expect(pedidos.slice(antes)).toEqual([]);
    expect(observador.notificar.mock.calls).toEqual([]);
  });

  it('preserva o aviso de lote parcial e recarrega quando a operação ainda está vigente', async () => {
    responder = config => config.url === '/frequencias/lote' && envios().length === 2 ? Promise.reject(erro(config)) : undefined;
    const hook = await carregar(); editar(hook, 5);
    const cargasAntes = pedidos.filter(p => p.config.url === '/empregados/filtrados-frequencia').length;
    await act(async () => hook.result.current.acoes.invocarGatilhoDeDespachoAoServidorGlobal());
    expect(envios()).toHaveLength(2);
    expect(observador.notificar).toHaveBeenCalledWith('error', expect.stringMatching(/2 registro\(s\).*lote 2\/3 falhou/));
    expect(pedidos.filter(p => p.config.url === '/empregados/filtrados-frequencia')).toHaveLength(cargasAntes + 1);
    expect(hook.result.current.estado.listaDaFrequencia.filter(item => item.alterado)).toHaveLength(3);
  });

  it('não publica sucesso nem inicia recarga quando um POST individual termina depois da desmontagem', async () => {
    const atrasado = adiar(); let enviado;
    responder = config => { if (config.url === '/frequencias' && config.method === 'post') { enviado = config; return atrasado.promise; } };
    const hook = await carregar(); editar(hook);
    const { envio } = await iniciarEnvio(hook);
    await waitFor(() => expect(enviado).toBeDefined());
    hook.unmount(); const antes = pedidos.length; observador.notificar.mockClear();
    await act(async () => { atrasado.resolve(ok(enviado, JSON.parse(enviado.data))); await envio; });
    expect(pedidos.slice(antes)).toEqual([]);
    expect(observador.notificar.mock.calls).toEqual([]);
  });

  it('não reutiliza a confirmação do recorte A para enviar rascunhos novos do recorte B', async () => {
    observador.confirmar.mockImplementation(({ acao }) => { confirmacoes.push(acao); return Promise.resolve(false); });
    const hook = await carregar(); editar(hook);
    await act(async () => hook.result.current.acoes.invocarGatilhoDeDespachoAoServidorGlobal());
    const acaoAntiga = confirmacoes[0];
    act(() => hook.result.current.acoes.aoAcionarMudarDataPeloCabecalho(-1));
    await waitFor(() => expect(hook.result.current.estado.indicadorCarregamento).toBe(false));
    editar(hook); observador.notificar.mockClear();
    await act(async () => acaoAntiga('senha-sintetica'));
    expect(envios()).toEqual([]);
    expect(observador.notificar.mock.calls).toEqual([]);
    expect(hook.result.current.estado.listaDaFrequencia.filter(item => item.alterado)).toHaveLength(1);
  });

  it('mantém a gravação de todos os lotes e a recarga na intenção atual', async () => {
    const hook = await carregar(); editar(hook, 5);
    await act(async () => hook.result.current.acoes.invocarGatilhoDeDespachoAoServidorGlobal());
    expect(envios().map(p => JSON.parse(p.config.data).length)).toEqual([2, 2, 1]);
    expect(observador.notificar).toHaveBeenCalledWith('success', expect.stringMatching(/^5 frequência/));
    expect(hook.result.current.estado.indicadorSalvamento).toBe(false);
  });
});

describe('Fechamentos e callbacks mantêm a intenção original', () => {
  it('preserva polling vigente quando a confirmação de criação é cancelada sem enviar', async () => {
    fechamentoInicial = { id: 601, status: 'PROCESSANDO' };
    const atrasado = adiar(); let consultado;
    responder = config => { if (config.url === '/frequencias/fechamentos/601' && !consultado) { consultado = config; return atrasado.promise; } };
    observador.confirmar.mockImplementation(({ acao }) => { confirmacoes.push(acao); return Promise.resolve(false); });
    const hook = await carregar();
    await waitFor(() => expect(consultado).toBeDefined());
    vi.useFakeTimers({ toFake: ['setTimeout', 'clearTimeout'] });
    await act(async () => { atrasado.resolve(ok(consultado, fechamentoInicial)); });
    await act(async () => hook.result.current.acoes.solicitarCriacaoDoFechamento());
    const antes = pedidos.filter(p => p.config.url === '/frequencias/fechamentos/601').length;
    await act(async () => vi.advanceTimersByTimeAsync(1500));
    expect(pedidos.filter(p => p.config.url === '/frequencias/fechamentos/601')).toHaveLength(antes + 1);
    expect(pedidos.filter(p => p.config.method === 'post')).toEqual([]);
  });

  it.each(['sessão', 'data'])('não inicia fechamento confirmado tardiamente após trocar %s', async tipo => {
    observador.confirmar.mockImplementation(({ acao }) => { confirmacoes.push(acao); return Promise.resolve(false); });
    const hook = await carregar();
    await act(async () => hook.result.current.acoes.solicitarCriacaoDoFechamento());
    const acaoAntiga = confirmacoes[0];
    if (tipo === 'sessão') { invalidarGeracaoSessao(); redefinirIndicadorRedirecionamento(); }
    else {
      act(() => hook.result.current.acoes.aoAcionarMudarDataPeloCabecalho(-1));
      await waitFor(() => expect(hook.result.current.estado.indicadorCarregamento).toBe(false));
    }
    const antes = pedidos.length; observador.notificar.mockClear();
    await act(async () => acaoAntiga('senha-sintetica'));
    expect(pedidos.slice(antes)).toEqual([]);
    expect(observador.notificar.mock.calls).toEqual([]);
  });

  it('não consulta nem publica fechamento criado para a data anterior', async () => {
    const atrasado = adiar(); let enviado;
    responder = config => { if (config.url === '/frequencias/fechamentos' && config.method === 'post') { enviado = config; return atrasado.promise; } };
    const hook = await carregar(); let operacao;
    await act(async () => { operacao = hook.result.current.acoes.solicitarCriacaoDoFechamento(); });
    await waitFor(() => expect(enviado).toBeDefined());
    act(() => hook.result.current.acoes.aoAcionarMudarDataPeloCabecalho(-1));
    await waitFor(() => expect(hook.result.current.estado.carregandoFechamento).toBe(false));
    const antes = pedidos.length; observador.notificar.mockClear();
    await act(async () => { atrasado.resolve(ok(enviado, { id: 601, status: 'PROCESSANDO' })); await operacao; });
    expect(pedidos.slice(antes)).toEqual([]);
    expect(observador.notificar.mock.calls).toEqual([]);
    expect(hook.result.current.estado.fechamentoFrequencia).toBeNull();
  });

  it('não publica erro do cancelamento de fechamento de outra sessão', async () => {
    fechamentoInicial = { id: 601, status: 'PROCESSANDO' };
    const atrasado = adiar(); let enviado;
    responder = config => { if (config.url.endsWith('/cancelamento')) { enviado = config; return atrasado.promise; } };
    const hook = await carregar(); let operacao;
    await waitFor(() => expect(hook.result.current.estado.fechamentoFrequencia?.id).toBe(601));
    await act(async () => { operacao = hook.result.current.acoes.solicitarCancelamentoDoFechamento(); });
    await waitFor(() => expect(enviado).toBeDefined());
    hook.unmount(); invalidarGeracaoSessao(); redefinirIndicadorRedirecionamento();
    const antes = pedidos.length; observador.notificar.mockClear();
    await act(async () => { atrasado.reject(erro(enviado)); await operacao; });
    expect(pedidos.slice(antes)).toEqual([]);
    expect(observador.notificar.mock.calls).toEqual([]);
  });

  it('não consulta nem notifica reprocessamento concluído depois da desmontagem', async () => {
    fechamentoInicial = { id: 601, status: 'ERRO' };
    const atrasado = adiar(); let enviado;
    responder = config => { if (config.url.endsWith('/reprocessamento')) { enviado = config; return atrasado.promise; } };
    const hook = await carregar(); let operacao;
    await waitFor(() => expect(hook.result.current.estado.fechamentoFrequencia?.id).toBe(601));
    await act(async () => { operacao = hook.result.current.acoes.solicitarReprocessamentoDoFechamento(); });
    await waitFor(() => expect(enviado).toBeDefined());
    hook.unmount(); const antes = pedidos.length; observador.notificar.mockClear();
    await act(async () => { atrasado.resolve(ok(enviado, { id: 601, status: 'PROCESSANDO' })); await operacao; });
    expect(pedidos.slice(antes)).toEqual([]);
    expect(observador.notificar.mock.calls).toEqual([]);
  });

  it('não retoma polling cuja resposta pertence ao recorte anterior', async () => {
    fechamentoInicial = { id: 601, status: 'PROCESSANDO' };
    const atrasado = adiar(); let consultado;
    responder = config => { if (config.url === '/frequencias/fechamentos/601' && !consultado) { consultado = config; return atrasado.promise; } };
    const hook = await carregar();
    await waitFor(() => expect(consultado).toBeDefined());
    fechamentoInicial = null;
    act(() => hook.result.current.acoes.aoAcionarMudarDataPeloCabecalho(-1));
    await waitFor(() => expect(hook.result.current.estado.carregandoFechamento).toBe(false));
    vi.useFakeTimers({ toFake: ['setTimeout', 'clearTimeout'] });
    const antes = pedidos.length;
    await act(async () => { atrasado.resolve(ok(consultado, { id: 601, status: 'PROCESSANDO' })); });
    expect(hook.result.current.estado.fechamentoFrequencia).toBeNull();
    await act(async () => vi.advanceTimersByTimeAsync(10000));
    expect(pedidos.slice(antes)).toEqual([]);
  });
});
