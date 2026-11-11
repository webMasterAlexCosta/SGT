import React from 'react';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { act, cleanup, fireEvent, render, screen, waitFor, within } from '@testing-library/react';
import '@testing-library/jest-dom/vitest';

const { requisitar, notificar } = vi.hoisted(() => ({ requisitar: vi.fn(), notificar: vi.fn() }));
vi.mock('../src/core/http.ts', async (original) => ({ ...(await original()), requisitar }));
vi.mock('../src/core/events.ts', async (original) => ({ ...(await original()), notificar }));

import { ModalAcaoVinculo, ModalVinculo } from '../src/modules/rh/pages/PaginaPessoasVinculos/components/ModalVinculo.tsx';
import { ModalAlocacoesVinculo } from '../src/modules/rh/pages/PaginaPessoasVinculos/components/ModalAlocacoesVinculo.tsx';
import { ModalCadastroVinculo } from '../src/modules/rh/pages/PaginaPessoasVinculos/components/ModalCadastroVinculo.tsx';

const pendente = () => {
  let resolve, reject;
  const promise = new Promise((res, rej) => { resolve = res; reject = rej; });
  return { promise, resolve, reject };
};
const vinculo = { id: 7, contratoId: 9, pessoaNome: 'Pessoa sintética', dataInicio: '2025-01-01', dataFim: '2040-12-31', estadoNegocial: 'REGISTRADO', version: 1 };
const opcao = { composicaoBaseId: 11, lotacaoId: 21, lotacaoNome: 'Lotação sintética', jornadaId: 31, jornadaDescricao: 'Turno sintético', cargoId: 41, cargoNome: 'Cargo sintético', efetivoPrevisto: 2, ocupacaoAtual: 1, ocupacaoAposInclusao: 2, excedenteAposInclusao: 0 };
const previa = { token: 'token-sintetico', valorAtual: '2026-01-01', valorProposto: '2026-01-02', impactos: ['Impacto sintético'], incompatibilidades: [] };
const auditar = () => {
  fireEvent.change(screen.getByLabelText(/^Justificativa/), { target: { value: 'Justificativa preservada' } });
  fireEvent.change(screen.getByLabelText(/^Evidência/), { target: { value: 'Fonte sintética' } });
  fireEvent.change(screen.getByLabelText(/^Autorização/), { target: { value: 'Ato sintético' } });
};
const formulario = () => screen.getByRole('dialog').querySelector('form');
const gerarPrevia = () => fireEvent.click(screen.getByRole('button', { name: 'Gerar prévia de impacto' }));

beforeEach(() => {
  requisitar.mockReset(); notificar.mockReset();
  requisitar.mockImplementation(async ({ url }) => {
    if (url.endsWith('/opcoes-alocacao')) return [opcao];
    if (url.endsWith('/previas/alocacoes')) return previa;
    return [];
  });
});
afterEach(cleanup);

const montarAcao = (extras = {}) => {
  const props = { aberto: true, tipo: 'RETIFICAR_INICIO', vinculo, processando: false, aoFechar: vi.fn(), aoPrevia: vi.fn().mockResolvedValue(previa), aoExecutar: vi.fn().mockResolvedValue(false), ...extras };
  const montagem = render(<ModalAcaoVinculo {...props} />);
  if (props.tipo === 'RETIFICAR_INICIO') fireEvent.change(screen.getByLabelText(/^Novo início efetivo/), { target: { value: '2026-01-02' } });
  auditar();
  return { ...montagem, props };
};

describe('Prévia e confirmação das ações do vínculo', () => {
  it('descarta uma prévia que termina depois de mudar a data', async () => {
    const pedido = pendente();
    const { props } = montarAcao({ aoPrevia: vi.fn(() => pedido.promise) });
    gerarPrevia();
    fireEvent.change(screen.getByLabelText(/^Novo início efetivo/), { target: { value: '2026-02-02' } });
    await act(async () => pedido.resolve(previa));
    expect(screen.queryByText('Prévia vinculada ao formulário atual')).not.toBeInTheDocument();
    fireEvent.submit(formulario());
    expect(props.aoExecutar).not.toHaveBeenCalled();
    expect(screen.getByLabelText(/^Novo início efetivo/)).toHaveValue('2026-02-02');
  });

  it('mostra a recusa de prévia, preserva o formulário e permite nova consulta', async () => {
    const pedido = pendente();
    const aoPrevia = vi.fn().mockImplementationOnce(() => pedido.promise).mockResolvedValue(previa);
    montarAcao({ aoPrevia });
    gerarPrevia();
    await act(async () => pedido.reject(Object.assign(new Error('Recorte temporal recusado'), { status: 409 })));
    expect(await screen.findByRole('alert')).toHaveTextContent('Recorte temporal recusado');
    expect(screen.getByLabelText(/^Justificativa/)).toHaveValue('Justificativa preservada');
    expect(screen.getByLabelText(/^Novo início efetivo/)).toHaveValue('2026-01-02');
    gerarPrevia();
    await screen.findByText('Prévia vinculada ao formulário atual');
    expect(aoPrevia).toHaveBeenCalledTimes(2);
  });

  it('não restaura prévia da abertura anterior depois de fechar e reabrir', async () => {
    const pedido = pendente();
    const { props, rerender } = montarAcao({ aoPrevia: vi.fn(() => pedido.promise) });
    gerarPrevia();
    rerender(<ModalAcaoVinculo {...props} aberto={false} />);
    rerender(<ModalAcaoVinculo {...props} aberto />);
    await act(async () => pedido.resolve(previa));
    expect(screen.queryByText('Prévia vinculada ao formulário atual')).not.toBeInTheDocument();
    expect(screen.getByLabelText(/^Novo início efetivo/)).toHaveValue('');
  });

  it('bloqueia duplo envio enquanto a confirmação externa está pendente e conserva dados no cancelamento', async () => {
    const confirmacao = pendente();
    const aoExecutar = vi.fn(() => confirmacao.promise);
    montarAcao({ tipo: 'CANCELAR_PROGRAMACAO', aoExecutar });
    act(() => { fireEvent.submit(formulario()); fireEvent.submit(formulario()); });
    expect(aoExecutar).toHaveBeenCalledTimes(1);
    await act(async () => confirmacao.resolve(false));
    expect(screen.getByLabelText(/^Justificativa/)).toHaveValue('Justificativa preservada');
    expect(screen.getByRole('button', { name: 'Confirmar ação' })).toBeEnabled();
  });

  it('exige nova prévia após alterar o aceite explícito de excedente e transmite o DTO', async () => {
    const { props } = montarAcao();
    gerarPrevia();
    await screen.findByText('Prévia vinculada ao formulário atual');
    fireEvent.click(screen.getByRole('checkbox', { name: /Confirmo os impactos/ }));
    const aceite = screen.queryByRole('checkbox', { name: /inclusão excedente/i });
    expect(aceite).toBeInTheDocument();
    fireEvent.click(aceite);
    fireEvent.submit(formulario());
    expect(props.aoExecutar).not.toHaveBeenCalled();
    expect(screen.queryByRole('checkbox', { name: /Confirmo os impactos/ })).not.toBeInTheDocument();
    gerarPrevia();
    await screen.findByText('Prévia vinculada ao formulário atual');
    expect(props.aoPrevia.mock.lastCall[1]).toMatchObject({ confirmarInclusaoExcedente: true, tokenPrevia: null, confirmarImpacto: false });
    fireEvent.click(screen.getByRole('checkbox', { name: /Confirmo os impactos/ }));
    fireEvent.submit(formulario());
    await waitFor(() => expect(props.aoExecutar).toHaveBeenCalledTimes(1));
    expect(props.aoExecutar.mock.lastCall[1]).toMatchObject({ confirmarInclusaoExcedente: true, tokenPrevia: previa.token, confirmarImpacto: true });
  });

  it('desligamento envia somente o aceite de precedência informado pelo usuário', async () => {
    const { props } = montarAcao({ tipo: 'DESLIGAR', motivos: [{ id: 5, descricao: 'Motivo sintético' }] });
    fireEvent.change(screen.getByLabelText(/^Último dia do vínculo/), { target: { value: '2026-03-10' } });
    fireEvent.change(screen.getByLabelText(/^Motivo de desligamento/), { target: { value: '5' } });
    const aceite = screen.queryByRole('checkbox', { name: /precedência/i });
    expect(aceite).toBeInTheDocument();
    fireEvent.click(aceite);
    fireEvent.submit(formulario());
    await waitFor(() => expect(props.aoExecutar).toHaveBeenCalledTimes(1));
    expect(props.aoExecutar.mock.lastCall[1]).toMatchObject({ confirmarPrecedenciaUltimoDia: true, confirmarInclusaoExcedente: false });
  });
});

const montarAlocacao = async (extras = {}) => {
  const props = { aberto: true, vinculo, podeGerenciar: true, processando: false, aoFechar: vi.fn(), aoSalvarAlocacao: vi.fn().mockResolvedValue(false), aoCancelarAlocacao: vi.fn().mockResolvedValue(false), ...extras };
  render(<ModalAlocacoesVinculo {...props} />);
  fireEvent.click(screen.getByRole('button', { name: 'Programar / alterar alocação' }));
  await screen.findByRole('option', { name: opcao.lotacaoNome });
  fireEvent.change(screen.getByLabelText(/^Lotação/), { target: { value: String(opcao.lotacaoId) } });
  await screen.findByRole('option', { name: opcao.jornadaDescricao });
  fireEvent.change(screen.getByLabelText(/^Turno \/ Jornada/), { target: { value: String(opcao.jornadaId) } });
  await screen.findByRole('option', { name: opcao.cargoNome });
  fireEvent.change(screen.getByLabelText(/^Cargo/), { target: { value: '11:21:31:41' } });
  auditar();
  return props;
};

describe('Data civil administrativa nos modais de vínculo e alocação', () => {
  // Relógio e offset controlados somente neste teste isolado; não é meia-noite física.
  afterEach(() => { vi.useRealTimers(); vi.restoreAllMocks(); });
  const relogio = instante => { vi.useFakeTimers({ toFake: ['Date'] }); vi.setSystemTime(new Date(instante)); };
  const detalhe = extras => ({ aberto: true, vinculo: { ...vinculo, dataFim: '2026-11-10' }, podeGerenciar: true,
    aoFechar: vi.fn(), aoConsultarTemporal: vi.fn(), aoAbrirAcao: vi.fn(), aoAbrirAlocacoes: vi.fn(), ...extras });
  const alocacao = extras => ({ aberto: true, vinculo, podeGerenciar: true, processando: false,
    aoFechar: vi.fn(), aoSalvarAlocacao: vi.fn(), aoCancelarAlocacao: vi.fn(), ...extras });

  it.each([
    ['2026-11-11T01:30:00Z', '2026-11-10', true],
    ['2026-11-11T03:05:00Z', '2026-11-11', false],
  ])('vínculo usa São Paulo no instante %s para consulta e cancelamento', (instante, data, cancelavel) => {
    relogio(instante); render(<ModalVinculo {...detalhe()} />);
    expect(screen.getByLabelText('Data de referência', { exact: true })).toHaveValue(data);
    const cancelar = screen.queryByRole('button', { name: 'Cancelar desligamento programado', exact: true });
    if (cancelavel) expect(cancelar).toBeInTheDocument(); else expect(cancelar).not.toBeInTheDocument();
  });

  it('reabrir o mesmo vínculo em outro dia atualiza a data administrativa', () => {
    relogio('2026-11-10T18:00:00Z'); const props = detalhe({ aberto: false });
    const view = render(<ModalVinculo {...props} />);
    vi.setSystemTime(new Date('2026-11-11T12:00:00Z')); view.rerender(<ModalVinculo {...props} aberto />);
    expect(screen.getByLabelText('Data de referência', { exact: true })).toHaveValue('2026-11-11');
    expect(screen.queryByRole('button', { name: 'Cancelar desligamento programado', exact: true })).not.toBeInTheDocument();
  });

  it('mudança do dia atualiza ações sem apagar D/K escolhido num modal aberto', () => {
    relogio('2026-11-10T18:00:00Z'); const props = detalhe(); const view = render(<ModalVinculo {...props} />);
    fireEvent.change(screen.getByLabelText('Data de referência', { exact: true }), { target: { value: '2026-10-05' } });
    fireEvent.change(screen.getByLabelText('Conhecido até (opcional)', { exact: true }), { target: { value: '2026-11-01T10:20' } });
    vi.setSystemTime(new Date('2026-11-11T12:00:00Z')); view.rerender(<ModalVinculo {...props} />);
    expect(screen.getByLabelText('Data de referência', { exact: true })).toHaveValue('2026-10-05');
    expect(screen.getByLabelText('Conhecido até (opcional)', { exact: true })).toHaveValue('2026-11-01T10:20');
    expect(screen.queryByRole('button', { name: 'Cancelar desligamento programado', exact: true })).not.toBeInTheDocument();
  });

  it('alocação não usa o dia do cliente UTC+14 na consulta e no novo movimento', async () => {
    relogio('2026-11-10T12:00:00Z'); vi.spyOn(Date.prototype, 'getTimezoneOffset').mockReturnValue(-840);
    await act(async () => render(<ModalAlocacoesVinculo {...alocacao()} />));
    expect(screen.getByLabelText('Data de referência', { exact: true })).toHaveValue('2026-11-10');
    fireEvent.click(screen.getByRole('button', { name: 'Programar / alterar alocação' }));
    expect(screen.getByLabelText(/^Válido a partir de/)).toHaveValue('2026-11-10');
  });

  it('reabrir alocações em outro dia também renova a data do novo movimento', async () => {
    relogio('2026-11-10T12:00:00Z'); const props = alocacao({ aberto: false });
    const view = render(<ModalAlocacoesVinculo {...props} />);
    vi.setSystemTime(new Date('2026-11-11T12:00:00Z'));
    await act(async () => view.rerender(<ModalAlocacoesVinculo {...props} aberto />));
    expect(screen.getByLabelText('Data de referência', { exact: true })).toHaveValue('2026-11-11');
    fireEvent.click(screen.getByRole('button', { name: 'Programar / alterar alocação' }));
    expect(screen.getByLabelText(/^Válido a partir de/)).toHaveValue('2026-11-11');
  });
});

describe('Prévia de alocação vinculada ao editor atual', () => {
  it('ignora a prévia lenta depois de editar a justificativa', async () => {
    const pedido = pendente();
    requisitar.mockImplementation(async ({ url }) => url.endsWith('/opcoes-alocacao') ? [opcao] : url.endsWith('/previas/alocacoes') ? pedido.promise : []);
    const props = await montarAlocacao();
    gerarPrevia();
    fireEvent.change(screen.getByLabelText(/^Justificativa/), { target: { value: 'Intenção posterior' } });
    await act(async () => pedido.resolve(previa));
    expect(screen.queryByText('Prévia vinculada a esta seleção')).not.toBeInTheDocument();
    fireEvent.submit(formulario());
    expect(props.aoSalvarAlocacao).not.toHaveBeenCalled();
  });

  it('cancelar o editor impede uma resposta antiga de reaparecer na nova alteração', async () => {
    const pedido = pendente();
    requisitar.mockImplementation(async ({ url }) => url.endsWith('/opcoes-alocacao') ? [opcao] : url.endsWith('/previas/alocacoes') ? pedido.promise : []);
    await montarAlocacao();
    gerarPrevia();
    fireEvent.click(screen.getByRole('button', { name: 'Cancelar', exact: true }));
    fireEvent.click(screen.getByRole('button', { name: 'Programar / alterar alocação' }));
    await act(async () => pedido.resolve(previa));
    expect(screen.queryByText('Prévia vinculada a esta seleção')).not.toBeInTheDocument();
    expect(screen.getByLabelText(/^Justificativa/)).toHaveValue('');
  });

  it('transmite consentimento de excedente, bloqueia duplo envio e invalida o token ao cancelar a confirmação', async () => {
    const confirmacao = pendente();
    const aoSalvarAlocacao = vi.fn(() => confirmacao.promise);
    await montarAlocacao({ aoSalvarAlocacao });
    const aceite = screen.queryByRole('checkbox', { name: /inclusão excedente/i });
    expect(aceite).toBeInTheDocument();
    fireEvent.click(aceite);
    gerarPrevia();
    await screen.findByText('Prévia vinculada a esta seleção');
    fireEvent.click(screen.getByRole('checkbox', { name: /Confirmo os impactos/ }));
    act(() => { fireEvent.submit(formulario()); fireEvent.submit(formulario()); });
    expect(aoSalvarAlocacao).toHaveBeenCalledTimes(1);
    expect(aoSalvarAlocacao.mock.lastCall[0].payload).toMatchObject({ confirmarInclusaoExcedente: true, tokenPrevia: previa.token, confirmarImpacto: true });
    await act(async () => confirmacao.resolve(false));
    expect(screen.getByLabelText(/^Justificativa/)).toHaveValue('Justificativa preservada');
    expect(screen.queryByRole('checkbox', { name: /Confirmo os impactos/ })).not.toBeInTheDocument();
  });
});

const montarCadastro = (extras = {}) => {
  const props = { aberto: true, contratos: [{ id: 9, numero: 'Contrato sintético' }], pessoas: [{ id: 1, nome: 'Pessoa sintética' }], salvando: false, aoFechar: vi.fn(), aoSalvar: vi.fn().mockResolvedValue(false), ...extras };
  render(<ModalCadastroVinculo {...props} />);
  fireEvent.change(screen.getByPlaceholderText('ID da pessoa'), { target: { value: '1' } });
  fireEvent.change(screen.getByLabelText(/^Contrato/), { target: { value: '9' } });
  fireEvent.change(screen.getByLabelText(/^Início efetivo/), { target: { value: '2026-01-01' } });
  return props;
};

describe('Opções e confirmação do cadastro de vínculo', () => {
  it('mantém as opções da data atual quando uma consulta abortada termina depois', async () => {
    const antiga = pendente(); const nova = pendente();
    requisitar.mockImplementation(({ params }) => params.data === '2026-01-01' ? antiga.promise : nova.promise);
    montarCadastro();
    fireEvent.change(screen.getByLabelText(/^Início efetivo/), { target: { value: '2026-02-01' } });
    await act(async () => nova.resolve([{ ...opcao, composicaoBaseId: 12, cargoNome: 'Cargo da data atual' }]));
    const select = screen.getByLabelText(/^Composição vigente/);
    expect(within(select).getByRole('option', { name: /Cargo da data atual/ })).toBeInTheDocument();
    await act(async () => antiga.resolve([{ ...opcao, cargoNome: 'Cargo da data antiga' }]));
    expect(within(select).queryByRole('option', { name: /Cargo da data atual/ })).toBeInTheDocument();
    expect(within(select).queryByRole('option', { name: /Cargo da data antiga/ })).not.toBeInTheDocument();
  });

  it('limpar a data encerra o estado de consulta sem ressuscitar opções canceladas', async () => {
    const pedido = pendente(); requisitar.mockImplementation(() => pedido.promise);
    montarCadastro();
    fireEvent.change(screen.getByLabelText(/^Início efetivo/), { target: { value: '' } });
    await act(async () => pedido.resolve([opcao]));
    expect(screen.queryByText('Consultando a posição temporal no servidor…')).not.toBeInTheDocument();
    expect(screen.getByLabelText(/^Composição vigente/)).toBeDisabled();
  });

  it('não abre duas confirmações de cadastro e mantém os campos ao cancelar', async () => {
    const confirmacao = pendente(); const aoSalvar = vi.fn(() => confirmacao.promise);
    montarCadastro({ aoSalvar });
    await screen.findByRole('option', { name: /Cargo sintético/ });
    fireEvent.change(screen.getByLabelText(/^Composição vigente/), { target: { value: '11' } });
    act(() => { fireEvent.submit(formulario()); fireEvent.submit(formulario()); });
    expect(aoSalvar).toHaveBeenCalledTimes(1);
    await act(async () => confirmacao.resolve(false));
    expect(screen.getByPlaceholderText('ID da pessoa')).toHaveValue('1');
    expect(screen.getByLabelText(/^Composição vigente/)).toHaveValue('11');
    expect(screen.getByRole('button', { name: 'Cadastrar vínculo' })).toBeEnabled();
  });
});
