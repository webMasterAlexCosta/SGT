import React from 'react';
import { act, cleanup, fireEvent, render, screen, waitFor, within } from '@testing-library/react';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import '@testing-library/jest-dom/vitest';

const observador = vi.hoisted(() => ({ modelo: null, confirmar: vi.fn(), notificar: vi.fn(), recarregar: vi.fn() }));
vi.mock('../src/modules/rh/pages/PaginaFrequencia/useLogicaFrequencia.ts', () => ({ useLogicaFrequencia: () => observador.modelo }));
vi.mock('../src/modules/rh/pages/PaginaFrequencia/components/CabecalhoFrequencia.tsx', () => ({ CabecalhoFrequencia: () => null }));
vi.mock('../src/modules/rh/pages/PaginaFrequencia/components/FiltrosFrequencia.tsx', () => ({ FiltrosFrequencia: () => null }));
vi.mock('../src/modules/rh/pages/PaginaFrequencia/components/TabelaFrequencia.tsx', () => ({ TabelaFrequencia: () => null }));
vi.mock('../src/modules/rh/pages/PaginaFrequencia/components/PainelFechamentoFrequencia.tsx', () => ({ PainelFechamentoFrequencia: () => null }));
vi.mock('../src/modules/rh/pages/PaginaFrequencia/components/TourGuiado.tsx', () => ({ TourGuiado: () => null }));
vi.mock('../src/modules/rh/pages/PaginaFrequencia/components/ModalParcial.tsx', () => ({ ModalParcial: () => null }));
vi.mock('../src/core/security/useConfirmacaoSenha.tsx', () => ({ useConfirmacaoSenha: () => ({ confirmarComSenha: observador.confirmar, ModalConfirmacao: () => null }) }));
vi.mock('../src/core/events.ts', async original => ({ ...(await original()), notificar: observador.notificar }));
import { PaginaFrequencia } from '../src/modules/rh/pages/PaginaFrequencia/index.tsx';
import { api } from '../src/core/http.ts';
import { invalidarGeracaoSessao } from '../src/core/security/geracaoSessao.ts';
import { resetarCoordenadorCsrfParaTestes } from '../src/core/http/coordenadorCsrf.ts';
import { redefinirIndicadorRedirecionamento } from '../src/core/events.ts';

const data = '2026-11-01';
const vinculos = Array.from({ length: 6 }, (_, i) => ({ id: 501 + i, pessoaId: i < 2 ? 101 : 102 + i, contratoId: 9,
  pessoaNome: 'Nome cadastral homônimo', pessoaCpf: '111.111.111-11', matricula: i === 0 ? 'MAT-123' : i === 1 ? 'MAT-1234' : `MAT-${i}`,
  dataReferencia: data, conhecidoAte: '2026-11-01T01:00:00Z', gestaoLocalId: 55, alocacaoId: 61, lotacaoId: 7, jornadaId: 8, cargoId: 6,
}));
const posicao = id => ({ vinculoId: id, dataReferencia: data, situacaoTemporal: 'VIGENTE', alocacaoId: 61, lotacaoId: 7, jornadaId: 8, cargoId: 6 });
const posicaoPessoa = id => ({ pessoaId: id, dataReferencia: data, conhecidoAte: '2026-11-01T01:00:00Z', vinculos: vinculos.filter(item => item.pessoaId === id).map(item => ({ vinculoId: item.id, pessoaId: item.pessoaId, contratoId: item.contratoId, dataReferencia: data, estadoNegocial: 'REGISTRADO', situacaoTemporal: 'VIGENTE' })) });
const adiar = () => { let resolve, reject; const promise = new Promise((r, j) => { resolve = r; reject = j; }); return { promise, resolve, reject }; };
const ok = (config, body) => ({ config, data: body, status: 200, statusText: 'OK', headers: {} });
const falha = (config, status, message) => ({ config, response: { status, data: { message }, headers: {} } });
let originalAdapter, pedidos, responder, lista, confirmacoes, acoes;
beforeEach(() => {
  originalAdapter = api.defaults.adapter; resetarCoordenadorCsrfParaTestes(); redefinirIndicadorRedirecionamento();
  observador.notificar.mockReset(); observador.confirmar.mockReset(); observador.recarregar.mockReset();
  pedidos = []; responder = undefined; lista = vinculos; confirmacoes = []; acoes = [];
  observador.confirmar.mockImplementation(({ acao }) => { confirmacoes.push(acao); const promessa = acao('senha-sintetica').catch(() => false); acoes.push(promessa); return promessa; });
  observador.modelo = {
    estado: { dataReferenciaAtiva: data, indicadorSalvamento: false, indicadorCarregamento: false, statusDaDataValidada: { valid: true }, listaDaFrequencia: [], listaDaFrequenciaFiltradaNaMemoria: [], estadoDoModalParcial: {}, paginacaoGrade: {} },
    filtros: { lotacaoSelecionadaPeloUsuario: '7', turnoSelecionadoPeloUsuario: '8', contratosSelecionadosPeloUsuario: ['9'], contratosBaixados: [{ id: 9, numeroContrato: 'Contrato sintético 9' }] },
    acoes: { recarregarFrequencia: observador.recarregar }, utilitarios: {},
    auth: { user: { id: 81, perfil: 'OPERACIONAL' }, eGestorGestaoLocal: false, podeRealizarFrequencias: true }, ModalConfirmacao: () => null,
  };
  api.defaults.adapter = async config => {
    pedidos.push(config);
    const personalizado = responder?.(config); if (personalizado !== undefined) return personalizado;
    if (config.url === '/autenticacao/csrf') return ok(config, { headerName: 'X-XSRF-TOKEN', token: 'token-sintetico' });
    if (config.url === '/vinculos-contratuais/elegiveis-frequencia') {
      const inicio = Number(config.params.page || 0) * Number(config.params.size || 5);
      return ok(config, { content: lista.slice(inicio, inicio + Number(config.params.size || 5)), totalElements: lista.length, totalPages: Math.ceil(lista.length / 5), number: Number(config.params.page || 0), size: 5 });
    }
    const id = /\/vinculos-contratuais\/(\d+)\/alocacoes\/posicao/.exec(config.url)?.[1];
    if (id) return ok(config, posicao(Number(id)));
    const pessoaId = /\/vinculos-contratuais\/pessoas\/(\d+)\/posicao/.exec(config.url)?.[1];
    if (pessoaId) return ok(config, posicaoPessoa(Number(pessoaId)));
    if (config.url === '/frequencias' && config.method === 'post') return ok(config, { id: 71, ...JSON.parse(config.data) });
    return ok(config, []);
  };
});
afterEach(() => { cleanup(); api.defaults.adapter = originalAdapter; invalidarGeracaoSessao(); vi.restoreAllMocks(); });
const posts = () => pedidos.filter(p => p.method === 'post' && p.url === '/frequencias');
const consultasPosicao = () => pedidos.filter(p => p.url.includes('/alocacoes/posicao'));
async function abrir() {
  const view = render(<PaginaFrequencia />);
  const botao = screen.getByRole('button', { name: 'Lançar por vínculo' }); botao.focus(); fireEvent.click(botao);
  await screen.findByRole('dialog', { name: 'Lançar por vínculo' });
  return view;
}
async function selecionar(id = 501) {
  fireEvent.click(await screen.findByRole('button', { name: `Selecionar vínculo ${id}` }));
  await screen.findByText(/Posição validada para/);
}
function preencher() {
  fireEvent.change(screen.getByLabelText('Status da frequência'), { target: { value: 'FOLGA' } });
  fireEvent.change(screen.getByLabelText('Observação do lançamento'), { target: { value: 'Registro sintético explícito' } });
  const justificativa = screen.queryByLabelText('Justificativa da retificação');
  if (justificativa) fireEvent.change(justificativa, { target: { value: 'Conferência sintética documental' } });
}
async function salvar() { fireEvent.click(screen.getByRole('button', { name: 'Confirmar lançamento' })); await act(async () => { await Promise.all(acoes); }); }

describe('IND-01: primeiro lançamento por seleção canônica explícita', () => {
  it('oferece seleção manual e pagina seis vínculos em cinco mais um sem autoseleção', async () => {
    await abrir();
    await waitFor(() => expect(screen.getAllByRole('button', { name: /Selecionar vínculo/ })).toHaveLength(5));
    expect(consultasPosicao()).toEqual([]); expect(posts()).toEqual([]);
    expect(screen.queryByLabelText('Status da frequência')).not.toBeInTheDocument();
    expect(pedidos.find(p => p.url === '/vinculos-contratuais/elegiveis-frequencia').params).toMatchObject({ dataRef: data, lotacaoId: 7, jornadaId: 8, contratoId: 9, page: 0, size: 5 });
    fireEvent.click(screen.getByRole('button', { name: 'Próximos vínculos' }));
    expect(await screen.findByRole('button', { name: 'Selecionar vínculo 506' })).toBeInTheDocument();
    expect(screen.getAllByRole('button', { name: /Selecionar vínculo/ })).toHaveLength(1);
    expect(consultasPosicao()).toEqual([]);
    fireEvent.click(screen.getByRole('button', { name: 'Vínculos anteriores' }));
    expect(await screen.findByRole('button', { name: 'Selecionar vínculo 501' })).toBeInTheDocument();
  });

  it.each([501, 502])('Operacional grava somente o vínculo %s escolhido entre homônimos e múltiplos vínculos da mesma pessoa', async id => {
    await abrir(); await selecionar(id); preencher(); await salvar();
    expect(posts()).toHaveLength(1);
    const payload = JSON.parse(posts()[0].data);
    expect(payload).toMatchObject({ vinculoContratualId: id, dataReferencia: data, status: 'FOLGA', observacao: 'Registro sintético explícito' });
    expect(payload).not.toHaveProperty('empregado'); expect(payload).not.toHaveProperty('empregadoId');
    expect(payload).not.toHaveProperty('pessoaId'); expect(payload).not.toHaveProperty('cpf');
    expect(consultasPosicao()).toHaveLength(1);
    expect(consultasPosicao()[0]).toMatchObject({ url: `/vinculos-contratuais/${id}/alocacoes/posicao`, params: { data } });
    expect(observador.recarregar).toHaveBeenCalledTimes(1);
  });

  it('usa o DTO elegível de D e revalida a posição em vez de consumir cadastro histórico', async () => {
    lista = [vinculos[0]];
    await abrir();
    expect(await screen.findByText(/Nome cadastral atual:/)).toHaveTextContent('Nome cadastral homônimo');
    expect(screen.getByText(/Pessoa ID: 101/)).toBeInTheDocument();
    expect(screen.getByText(/Contrato ID: 9/)).toBeInTheDocument();
    expect(consultasPosicao()).toEqual([]);
    expect(pedidos.some(p => p.url === '/vinculos-contratuais')).toBe(false);
    await selecionar(); preencher(); await salvar(); expect(posts()).toHaveLength(1);
  });

  it.each([
    ['vínculo diferente', { vinculoId: 502 }], ['data diferente', { dataReferencia: '2026-10-31' }],
    ['sem vigência', { situacaoTemporal: 'ENCERRADA' }], ['sem alocação', { alocacaoId: null }],
    ['lotação diferente', { lotacaoId: 9 }], ['jornada diferente', { jornadaId: 10 }],
    ['alocação mudou após seleção', { alocacaoId: 62 }], ['cargo mudou após seleção', { cargoId: 99 }],
  ])('bloqueia escrita quando a posição informa %s', async (_nome, alteracao) => {
    responder = config => config.url.includes('/alocacoes/posicao') ? ok(config, { ...posicao(501), ...alteracao }) : undefined;
    await abrir(); fireEvent.click(await screen.findByRole('button', { name: 'Selecionar vínculo 501' }));
    expect(await screen.findByRole('alert')).toHaveTextContent(/posição.*vínculo.*data.*escopo/i);
    expect(screen.queryByLabelText('Status da frequência')).not.toBeInTheDocument(); expect(posts()).toEqual([]);
  });

  it('apresenta 403 de listagem ao Operacional sem anunciar lista vazia', async () => {
    responder = config => config.url === '/vinculos-contratuais/elegiveis-frequencia' ? Promise.reject(falha(config, 403, 'Escopo revogado pelo Java')) : undefined;
    await abrir(); expect(await screen.findByRole('alert')).toHaveTextContent('Escopo revogado pelo Java');
    expect(screen.queryByText(/Nenhum vínculo encontrado/)).not.toBeInTheDocument();
  });

  it('apresenta recusa de posição sem substituir por outra identidade', async () => {
    responder = config => config.url.includes('/alocacoes/posicao') ? Promise.reject(falha(config, 409, 'Posição temporal ambígua no Java')) : undefined;
    await abrir(); fireEvent.click(await screen.findByRole('button', { name: 'Selecionar vínculo 501' }));
    expect(await screen.findByRole('alert')).toHaveTextContent('Posição temporal ambígua');
    expect(consultasPosicao()).toHaveLength(1); expect(posts()).toEqual([]);
  });

  it.each([
    ['Pessoa divergente', resposta => ({ ...resposta, pessoaId: 999 })],
    ['data divergente', resposta => ({ ...resposta, dataReferencia: '2026-10-31' })],
    ['vínculo ausente', resposta => ({ ...resposta, vinculos: [] })],
    ['vínculo duplicado', resposta => ({ ...resposta, vinculos: [resposta.vinculos[0], resposta.vinculos[0]] })],
    ['Pessoa do vínculo divergente', resposta => ({ ...resposta, vinculos: [{ ...resposta.vinculos[0], pessoaId: 999 }] })],
    ['Contrato divergente', resposta => ({ ...resposta, vinculos: [{ ...resposta.vinculos[0], contratoId: 10 }] })],
    ['vínculo não vigente', resposta => ({ ...resposta, vinculos: [{ ...resposta.vinculos[0], situacaoTemporal: 'DESLIGADO' }] })],
    ['vínculo não registrado', resposta => ({ ...resposta, vinculos: [{ ...resposta.vinculos[0], estadoNegocial: 'CANCELADO' }] })],
    ['data do vínculo divergente', resposta => ({ ...resposta, vinculos: [{ ...resposta.vinculos[0], dataReferencia: '2026-10-31' }] })],
  ])('exige fotografia canônica da Pessoa em D: %s', async (_nome, alterar) => {
    responder = config => config.url === '/vinculos-contratuais/pessoas/101/posicao' ? ok(config, alterar(posicaoPessoa(101))) : undefined;
    await abrir(); fireEvent.click(await screen.findByRole('button', { name: 'Selecionar vínculo 501' }));
    expect(await screen.findByRole('alert')).toHaveTextContent(/posição.*Pessoa.*vínculo.*contrato.*data/i);
    expect(screen.queryByLabelText('Status da frequência')).not.toBeInTheDocument(); expect(posts()).toEqual([]);
  });

  it('exige contrato explícito para listar quando o filtro contém vários contratos', async () => {
    observador.modelo.filtros.contratosSelecionadosPeloUsuario = ['9', '10'];
    observador.modelo.filtros.contratosBaixados.push({ id: 10, numeroContrato: 'Contrato sintético 10' });
    await abrir(); expect(pedidos.filter(p => p.url === '/vinculos-contratuais/elegiveis-frequencia')).toEqual([]);
    fireEvent.change(screen.getByLabelText('Contrato da listagem'), { target: { value: '9' } });
    await screen.findByRole('button', { name: 'Selecionar vínculo 501' });
    expect(pedidos.find(p => p.url === '/vinculos-contratuais/elegiveis-frequencia').params.contratoId).toBe(9);
  });

  it.each(['data', 'lotação', 'jornada', 'contratos', 'gestão', 'sessão'])('invalida seleção e confirmação pendente ao mudar %s', async tipo => {
    observador.confirmar.mockImplementation(({ acao }) => { confirmacoes.push(acao); return Promise.resolve(false); });
    const view = await abrir(); await selecionar(); preencher(); await salvar();
    expect(confirmacoes).toHaveLength(1);
    if (tipo === 'data') observador.modelo.estado.dataReferenciaAtiva = '2026-10-31';
    if (tipo === 'lotação') observador.modelo.filtros.lotacaoSelecionadaPeloUsuario = '9';
    if (tipo === 'jornada') observador.modelo.filtros.turnoSelecionadoPeloUsuario = '10';
    if (tipo === 'contratos') observador.modelo.filtros.contratosSelecionadosPeloUsuario = ['10'];
    if (tipo === 'gestão') observador.modelo.auth.user.gestaoLocalId = 56;
    if (tipo === 'sessão') invalidarGeracaoSessao();
    view.rerender(<PaginaFrequencia />); observador.notificar.mockClear();
    await act(async () => confirmacoes[0]('senha-sintetica'));
    expect(posts()).toEqual([]); expect(observador.recarregar).not.toHaveBeenCalled();
    expect(screen.queryByRole('dialog', { name: 'Lançar por vínculo' })).not.toBeInTheDocument();
  });

  it('descarta posição antiga se o usuário fecha o modal e abre nova consulta', async () => {
    const atraso = adiar(); let antigo;
    responder = config => { if (config.url.includes('/alocacoes/posicao') && !antigo) { antigo = config; return atraso.promise; } };
    await abrir(); fireEvent.click(await screen.findByRole('button', { name: 'Selecionar vínculo 501' }));
    await waitFor(() => expect(antigo).toBeDefined()); fireEvent.keyDown(document, { key: 'Escape' });
    fireEvent.click(screen.getByRole('button', { name: 'Lançar por vínculo' }));
    await screen.findByRole('button', { name: 'Selecionar vínculo 501' });
    await act(async () => { atraso.resolve(ok(antigo, posicao(501))); });
    expect(screen.queryByLabelText('Status da frequência')).not.toBeInTheDocument(); expect(posts()).toEqual([]);
  });

  it.each([409, 0])('Gestor Local mantém formulário e chave após erro %s, sem retry automático', async status => {
    observador.modelo.auth.eGestorGestaoLocal = true; observador.modelo.auth.user.perfil = 'GESTOR_LOCAL';
    responder = config => config.url === '/frequencias' && config.method === 'post' && posts().length === 1
      ? Promise.reject(status ? falha(config, status, 'Conflito sintético revisável') : Object.assign(new Error('Network Error'), { config, code: 'ERR_NETWORK', isAxiosError: true })) : undefined;
    await abrir(); await selecionar(); preencher(); await salvar();
    expect(posts()).toHaveLength(1);
    expect(await screen.findByRole('alert')).toHaveTextContent(/mantidos.*nova tentativa automática/i);
    expect(screen.getByLabelText('Observação do lançamento')).toHaveValue('Registro sintético explícito');
    expect(screen.getByLabelText('Justificativa da retificação')).toHaveValue('Conferência sintética documental');
    const primeiro = JSON.parse(posts()[0].data);
    expect(primeiro.justificativaRetificacao).toBe('Conferência sintética documental'); expect(primeiro.chaveIdempotencia.length).toBeGreaterThanOrEqual(8);
    await salvar(); expect(posts()).toHaveLength(2); expect(JSON.parse(posts()[1].data)).toEqual(primeiro);
  });

  it('impede POST duplicado durante confirmação e envio pendente', async () => {
    const atraso = adiar(); let enviado;
    responder = config => { if (config.url === '/frequencias' && config.method === 'post') { enviado = config; return atraso.promise; } };
    await abrir(); await selecionar(); preencher();
    const botao = screen.getByRole('button', { name: 'Confirmar lançamento' }); fireEvent.click(botao); fireEvent.click(botao);
    await waitFor(() => expect(enviado).toBeDefined());
    expect(posts()).toHaveLength(1); expect(confirmacoes).toHaveLength(1);
    await act(async () => { atraso.resolve(ok(enviado, { id: 71 })); await Promise.all(acoes); });
  });

  it.each(['desmontagem', 'sessão'])('não publica sucesso nem recarrega após %s durante POST', async tipo => {
    const atraso = adiar(); let enviado;
    responder = config => { if (config.url === '/frequencias' && config.method === 'post') { enviado = config; return atraso.promise; } };
    const view = await abrir(); await selecionar(); preencher(); fireEvent.click(screen.getByRole('button', { name: 'Confirmar lançamento' }));
    await waitFor(() => expect(enviado).toBeDefined());
    if (tipo === 'desmontagem') view.unmount(); else invalidarGeracaoSessao();
    observador.notificar.mockClear(); const antes = pedidos.length;
    await act(async () => { atraso.resolve(ok(enviado, { id: 71 })); await Promise.all(acoes); });
    expect(observador.notificar.mock.calls).toEqual([]); expect(observador.recarregar).not.toHaveBeenCalled();
    expect(pedidos.slice(antes)).toEqual([]);
  });

  it.each([
    ['data', { dataReferencia: '2026-10-31' }], ['lotação', { lotacaoId: 99 }],
    ['jornada', { jornadaId: 99 }], ['contrato', { contratoId: 99 }],
    ['sem alocação', { alocacaoId: null }], ['sem gestão', { gestaoLocalId: null }],
  ])('recusa lista que não corresponde ao contexto: %s', async (_caso, divergencia) => {
    lista = [{ ...vinculos[0], ...divergencia }];
    await abrir(); expect(await screen.findByRole('alert')).toHaveTextContent(/seleção.*data.*gestão.*contrato.*lotação.*jornada/i);
    expect(screen.queryByRole('button', { name: 'Selecionar vínculo 501' })).not.toBeInTheDocument();
    expect(consultasPosicao()).toEqual([]); expect(posts()).toEqual([]);
  });

  it('recusa gestão divergente do ator local sem oferecer uma identidade', async () => {
    observador.modelo.auth.user = { ...observador.modelo.auth.user, perfil: 'GESTOR_LOCAL', gestaoLocalId: 56 };
    await abrir(); expect(await screen.findByRole('alert')).toHaveTextContent(/seleção.*gestão/i);
    expect(screen.queryByRole('button', { name: 'Selecionar vínculo 501' })).not.toBeInTheDocument(); expect(posts()).toEqual([]);
  });

  it('cancela leitura elegível pendente quando o escopo muda e descarta a resposta antiga', async () => {
    const atraso = adiar(); let antigo;
    responder = config => { if (config.url === '/vinculos-contratuais/elegiveis-frequencia' && !antigo) { antigo = config; return atraso.promise; } };
    const view = await abrir(); await waitFor(() => expect(antigo).toBeDefined());
    observador.modelo.filtros.lotacaoSelecionadaPeloUsuario = '99'; view.rerender(<PaginaFrequencia />);
    await waitFor(() => expect(antigo.signal.aborted).toBe(true));
    await act(async () => { atraso.resolve(ok(antigo, { content: [vinculos[0]], totalElements: 1, totalPages: 1, number: 0, size: 5 })); });
    expect(screen.queryByRole('button', { name: 'Selecionar vínculo 501' })).not.toBeInTheDocument(); expect(posts()).toEqual([]);
  });

  it('reutiliza Escape e devolução de foco do diálogo existente', async () => {
    await abrir(); await screen.findByRole('button', { name: 'Selecionar vínculo 501' });
    expect(within(screen.getByRole('dialog', { name: 'Lançar por vínculo' })).getByRole('button', { name: 'Fechar Lançar por vínculo' })).toBeInTheDocument();
    fireEvent.keyDown(document, { key: 'Escape' });
    expect(screen.queryByRole('dialog', { name: 'Lançar por vínculo' })).not.toBeInTheDocument();
    await waitFor(() => expect(screen.getByRole('button', { name: 'Lançar por vínculo' })).toHaveFocus());
  });
});
