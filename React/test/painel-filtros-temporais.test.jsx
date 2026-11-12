import React from 'react';
import { act, cleanup, fireEvent, render, screen, waitFor, within } from '@testing-library/react';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import '@testing-library/jest-dom/vitest';
import { IndicadorEfetivo } from '../src/modules/painel/components/IndicadorEfetivo/index.tsx';
import { api } from '../src/core/http.ts';
import { invalidarGeracaoSessao } from '../src/core/security/geracaoSessao.ts';
import { resetarCoordenadorCsrfParaTestes } from '../src/core/http/coordenadorCsrf.ts';
import { redefinirIndicadorRedirecionamento } from '../src/core/events.ts';
import * as xlsx from '../src/core/export/xlsx.ts';

// Componente, serviço, serialização e interceptores reais; somente o transporte
// Axios é controlado. As opções são fixtures explícitas dos contratos temporais
// já cobertos em PainelEfetivoTemporalIT, não uma reimplementação do Java.
const mensalJunho = {
  contratos: [{ id: 71, nome: 'Contrato encerrado em 10 de junho' }],
  lotacoes: [{ id: 72, nome: 'Lotação histórica de junho' }],
  turnos: [{ id: 73, nome: 'Turno histórico de junho' }],
  cargos: [{ id: 74, nome: 'Cargo histórico de junho' }],
  permiteDetalhamentoOperacional: true,
};
const mensalJulho = {
  contratos: [{ id: 81, nome: 'Contrato do período de julho' }],
  lotacoes: [{ id: 82, nome: 'Lotação de julho' }],
  turnos: [{ id: 83, nome: 'Turno de julho' }],
  cargos: [{ id: 84, nome: 'Cargo de julho' }],
  permiteDetalhamentoOperacional: true,
};
const diarioJunho = {
  contratos: [{ id: 91, nome: 'Contrato vigente em 30 de junho' }],
  lotacoes: [{ id: 92, nome: 'Lotação vigente em 30 de junho' }],
  turnos: [{ id: 93, nome: 'Turno vigente em 30 de junho' }],
  cargos: [{ id: 94, nome: 'Cargo vigente em 30 de junho' }],
  permiteDetalhamentoOperacional: true,
};
const anoAnterior = {
  contratos: [...mensalJunho.contratos, { id: 61, nome: 'Contrato exclusivo do ano anterior' }],
  lotacoes: [...mensalJunho.lotacoes, { id: 62, nome: 'Lotação exclusiva do ano anterior' }],
  turnos: [...mensalJunho.turnos, { id: 63, nome: 'Turno exclusivo do ano anterior' }],
  cargos: [...mensalJunho.cargos, { id: 64, nome: 'Cargo exclusivo do ano anterior' }],
  permiteDetalhamentoOperacional: true,
};
const foraDoRecorte = {
  contratos: [{ id: 99, nome: 'Contrato atual fora do recorte histórico' }],
  lotacoes: [], turnos: [], cargos: [], permiteDetalhamentoOperacional: true,
};
const resumo = {
  efetivoPrevisto: 0, efetivoAlocado: 0, registrosLancados: 0, presentes: 0,
  ausentes: 0, outrosRegistros: 0, pendentesLancamento: 0,
  deficitContratual: 0, excedenteContratual: 0,
  percentualCobertura: 0, percentualAtendimento: 0,
};
const ok = (config, data) => ({ config, data, status: 200, statusText: 'OK', headers: {} });
const catalogo = (params = {}) => {
  if (params.referencia === '2026-07-31' && params.periodo === 'MENSAL') return mensalJulho;
  if (params.referencia !== '2026-06-30') return foraDoRecorte;
  if (params.periodo === 'DIARIO') return diarioJunho;
  if (params.periodo === 'MENSAL') return params.compararAnoAnterior ? anoAnterior : mensalJunho;
  return foraDoRecorte;
};
const indicador = (params = {}) => ({
  inicio: params.periodo === 'DIARIO' ? '2026-06-30'
    : params.referencia === '2026-07-31' ? '2026-07-01' : '2026-06-01',
  fim: params.referencia,
  periodo: params.periodo,
  contratoId: params.contratoId ? Number(params.contratoId) : null,
  lotacaoId: params.lotacaoId ? Number(params.lotacaoId) : null,
  jornadaId: params.jornadaId ? Number(params.jornadaId) : null,
  cargoId: params.cargoId ? Number(params.cargoId) : null,
  resumoAtual: resumo,
  resumoAnoAnterior: params.compararAnoAnterior ? resumo : null,
  serieAtual: [], serieAnoAnterior: [], detalhamentoContratos: [],
  detalhamentoLotacoes: [], detalhamentoTurnos: [], detalhamentoCargos: [], quadroDimensionado: [],
});

let adapterOriginal, pedidos, respostaPersonalizada;
beforeEach(() => {
  adapterOriginal = api.defaults.adapter;
  pedidos = [];
  respostaPersonalizada = undefined;
  resetarCoordenadorCsrfParaTestes();
  redefinirIndicadorRedirecionamento();
  api.defaults.adapter = async (config) => {
    pedidos.push({
      method: config.method, url: config.url, params: { ...config.params },
      uri: api.getUri(config), timeout: config.timeout, signal: config.signal,
    });
    const personalizada = respostaPersonalizada?.(config);
    if (personalizada !== undefined) return personalizada;
    if (config.url === '/painel/efetivo/filtros') return ok(config, catalogo(config.params));
    if (config.url === '/painel/efetivo') return ok(config, indicador(config.params));
    throw new Error(`Requisição inesperada: ${config.method} ${config.url}`);
  };
});
afterEach(() => {
  cleanup();
  api.defaults.adapter = adapterOriginal;
  invalidarGeracaoSessao();
  resetarCoordenadorCsrfParaTestes();
  redefinirIndicadorRedirecionamento();
  vi.useRealTimers();
  vi.restoreAllMocks();
});

const cartaoPrevisto = () => screen.getByText('dimensionamento contratual vigente').closest('article');
const pdf = () => screen.getByRole('button', { name: 'PDF', exact: true });
const planilha = () => screen.getByRole('button', { name: 'XLSX', exact: true });
const indicadorComValor = (params, valor) => ({
  ...indicador(params), resumoAtual: { ...resumo, efetivoPrevisto: valor },
});

it.each([
  ['2024-03-01T01:30:00Z', '2024-02-28', '2024-02-29', '2024-03-01T03:30:00Z', '2024-03-01'],
  ['2026-01-01T01:30:00Z', '2025-12-30', '2025-12-31', '2026-01-01T03:30:00Z', '2026-01-01'],
])('usa ontem administrativo em %s e atualiza o limite sem substituir o recorte escolhido', async (instante, ontem, hoje, depois, novoHoje) => {
  vi.useFakeTimers({ toFake: ['Date'] });
  vi.setSystemTime(new Date(instante));
  const view = render(<IndicadorEfetivo />);
  const data = screen.getByLabelText('Data de referência do indicador');
  expect(data).toHaveValue(ontem);
  expect(data).toHaveAttribute('max', hoje);
  await waitFor(() => expect(pedidos.some(p => p.url === '/painel/efetivo' && p.params.referencia === ontem)).toBe(true));
  vi.setSystemTime(new Date(depois));
  view.rerender(<IndicadorEfetivo />);
  expect(data).toHaveValue(ontem);
  expect(data).toHaveAttribute('max', novoHoje);
});

async function abrirComRelatorio(responderDados) {
  const relatorios = [];
  const exportacoes = [];
  // O construtor de planilha do exportador é real. Só o download pelo navegador
  // é substituído; suas células e seu nome são os valores produzidos pelo fluxo.
  vi.spyOn(xlsx, 'baixarArquivoXlsx').mockImplementation((configuracao, nome) => {
    exportacoes.push({ configuracao, nome });
  });
  respostaPersonalizada = (config) => {
    if (config.url === '/painel/efetivo/filtros') return ok(config, mensalJunho);
    if (config.url === '/painel/efetivo') {
      return responderDados ? responderDados(config) : ok(config, indicadorComValor(config.params, 11));
    }
    return undefined;
  };
  render(<IndicadorEfetivo referencia="2026-06-30" aoAtualizarRelatorio={(valor) => relatorios.push(valor)} />);
  fireEvent.click(screen.getByRole('button', { name: 'Mensal', exact: true }));
  await waitFor(() => expect(pdf()).toBeEnabled());
  expect(cartaoPrevisto()).toHaveTextContent('11');
  expect(relatorios.at(-1)?.dados.resumoAtual.efetivoPrevisto).toBe(11);
  return { relatorios, exportacoes };
}

describe('Dados e exportação pertencem à consulta vigente do indicador', () => {
  it.each([
    ['referência', () => fireEvent.change(screen.getByLabelText('Data de referência do indicador'), { target: { value: '2026-07-31' } })],
    ['período', () => fireEvent.click(screen.getByRole('button', { name: 'Diário', exact: true }))],
    ['comparação', () => fireEvent.click(screen.getByRole('button', { name: 'Comparar ano anterior', exact: true }))],
    ['contrato', () => fireEvent.change(selecionar('Contrato'), { target: { value: '71' } })],
    ['lotação', () => fireEvent.change(selecionar('Lotação'), { target: { value: '72' } })],
    ['turno', () => fireEvent.change(selecionar('Turno'), { target: { value: '73' } })],
    ['cargo', () => fireEvent.change(selecionar('Cargo'), { target: { value: '74' } })],
    ['atualização explícita', () => fireEvent.click(screen.getByRole('button', { name: 'Atualizar indicador' }))],
  ])('retira dados e exportação anteriores imediatamente ao mudar %s, antes dos 220 ms', async (_nome, alterar) => {
    const { relatorios, exportacoes } = await abrirComRelatorio();
    const quantidade = dados().length;
    vi.useFakeTimers({ toFake: ['setTimeout', 'clearTimeout'] });
    alterar();
    expect(dados()).toHaveLength(quantidade);
    expect(pdf()).toBeDisabled();
    expect(planilha()).toBeDisabled();
    expect(cartaoPrevisto()).not.toHaveTextContent('11');
    expect(relatorios.at(-1)).toBeNull();
    fireEvent.click(planilha());
    expect(exportacoes).toEqual([]);
  });

  it('mantém o erro da consulta B sem oferecer dados A e volta a exportar somente após sucesso válido de B', async () => {
    let recusarB = true;
    const { relatorios, exportacoes } = await abrirComRelatorio((config) => {
      if (config.params.referencia === '2026-07-31' && recusarB) {
        return Promise.reject({ config, response: {
          status: 403, data: { code: 'ACCESS_DENIED', message: 'Consulta B recusada pelo servidor' }, headers: {},
        } });
      }
      return ok(config, indicadorComValor(config.params, config.params.referencia === '2026-07-31' ? 22 : 11));
    });
    fireEvent.change(screen.getByLabelText('Data de referência do indicador'), { target: { value: '2026-07-31' } });
    await screen.findByText('Consulta B recusada pelo servidor');
    expect(pdf()).toBeDisabled();
    expect(planilha()).toBeDisabled();
    expect(cartaoPrevisto()).not.toHaveTextContent('11');
    expect(relatorios.at(-1)).toBeNull();
    fireEvent.click(planilha());
    expect(exportacoes).toEqual([]);

    recusarB = false;
    fireEvent.click(screen.getByRole('button', { name: 'Atualizar indicador' }));
    await waitFor(() => expect(cartaoPrevisto()).toHaveTextContent('22'));
    await waitFor(() => expect(planilha()).toBeEnabled());
    expect(screen.queryByText('Consulta B recusada pelo servidor')).not.toBeInTheDocument();
    expect(relatorios.at(-1)).toMatchObject({
      dados: { fim: '2026-07-31', periodo: 'MENSAL', resumoAtual: { efetivoPrevisto: 22 } },
      contexto: { referencia: '2026-07-31', periodoRotulo: 'Mensal', comparar: false },
    });
    fireEvent.click(planilha());
    expect(exportacoes).toHaveLength(1);
    expect(exportacoes[0].nome).toBe('indicador_efetivo_todos_os_contratos_2026-07-31.xlsx');
    const linhas = exportacoes[0].configuracao.sheets.find((item) => item.name === 'Resumo').rows;
    expect(linhas.find((linha) => linha.cells[0] === 'Referência').cells).toEqual(['Referência', '31/07/2026', 'Período', 'Mensal']);
    expect(linhas.find((linha) => linha.cells[0] === 'Efetivo previsto').cells[1].value).toBe(22);
  });

  it('resposta B atrasada não substitui a consulta A renovada nem seu contexto de exportação', async () => {
    let configB, liberarB, consultasA = 0;
    const respostaB = new Promise((resolve) => { liberarB = resolve; });
    const { relatorios } = await abrirComRelatorio((config) => {
      if (config.params.referencia === '2026-07-31') {
        configB = config;
        return respostaB;
      }
      consultasA += 1;
      return ok(config, indicadorComValor(config.params, consultasA === 1 ? 11 : 33));
    });
    fireEvent.change(screen.getByLabelText('Data de referência do indicador'), { target: { value: '2026-07-31' } });
    await waitFor(() => expect(configB).toBeDefined());
    fireEvent.change(screen.getByLabelText('Data de referência do indicador'), { target: { value: '2026-06-30' } });
    await waitFor(() => expect(cartaoPrevisto()).toHaveTextContent('33'));
    expect(configB.signal.aborted).toBe(true);
    await act(async () => { liberarB(ok(configB, indicadorComValor(configB.params, 22))); });
    expect(cartaoPrevisto()).toHaveTextContent('33');
    expect(cartaoPrevisto()).not.toHaveTextContent('22');
    expect(relatorios.at(-1)).toMatchObject({
      dados: { fim: '2026-06-30', resumoAtual: { efetivoPrevisto: 33 } },
      contexto: { referencia: '2026-06-30', periodoRotulo: 'Mensal', comparar: false },
    });
    expect(planilha()).toBeEnabled();
  });
});

const filtros = () => pedidos.filter((item) => item.url === '/painel/efetivo/filtros');
const dados = () => pedidos.filter((item) => item.url === '/painel/efetivo');
const selecionar = (rotulo) => screen.getByRole('combobox', { name: rotulo, exact: true });
async function abrir() {
  const tela = render(<IndicadorEfetivo referencia="2026-06-30" />);
  fireEvent.click(screen.getByRole('button', { name: 'Mensal', exact: true }));
  await waitFor(() => expect(dados().length).toBeGreaterThan(0));
  return tela;
}
async function esperarContexto(contexto) {
  await waitFor(() => {
    expect(filtros().at(-1)?.params).toMatchObject(contexto);
    expect(dados().at(-1)?.params).toMatchObject(contexto);
  });
  for (const pedido of [filtros().at(-1), dados().at(-1)]) {
    const query = new URL(pedido.uri, 'https://sgt.example.test').searchParams;
    expect(query.get('referencia')).toBe(contexto.referencia);
    expect(query.get('periodo')).toBe(contexto.periodo);
    expect(query.get('compararAnoAnterior')).toBe(String(contexto.compararAnoAnterior));
  }
}

describe('Filtros temporais do consumidor real do indicador', () => {
  it('oferece contrato encerrado no mês histórico com o mesmo recorte da consulta do indicador', async () => {
    await abrir();
    await esperarContexto({ referencia: '2026-06-30', periodo: 'MENSAL', compararAnoAnterior: false });
    expect(within(selecionar('Contrato')).getByRole('option', { name: 'Contrato encerrado em 10 de junho' })).toBeInTheDocument();
    expect(screen.queryByRole('option', { name: 'Contrato atual fora do recorte histórico' })).not.toBeInTheDocument();
  });

  it('refaz filtros e dados ao mudar a referência na tela, removendo as opções do período anterior', async () => {
    await abrir();
    const anteriores = filtros().length;
    fireEvent.change(screen.getByLabelText('Data de referência do indicador'), { target: { value: '2026-07-31' } });
    await waitFor(() => expect(filtros().length).toBeGreaterThan(anteriores));
    await esperarContexto({ referencia: '2026-07-31', periodo: 'MENSAL', compararAnoAnterior: false });
    expect(within(selecionar('Contrato')).getByRole('option', { name: 'Contrato do período de julho' })).toBeInTheDocument();
    expect(screen.queryByRole('option', { name: 'Contrato encerrado em 10 de junho' })).not.toBeInTheDocument();
  });

  it('acompanha a nova referência recebida da página do painel nos dois endpoints', async () => {
    const tela = await abrir();
    const anteriores = filtros().length;
    tela.rerender(<IndicadorEfetivo referencia="2026-07-31" />);
    await waitFor(() => expect(filtros().length).toBeGreaterThan(anteriores));
    await esperarContexto({ referencia: '2026-07-31', periodo: 'MENSAL', compararAnoAnterior: false });
    expect(screen.getByLabelText('Data de referência do indicador')).toHaveValue('2026-07-31');
    expect(within(selecionar('Lotação')).getByRole('option', { name: 'Lotação de julho' })).toBeInTheDocument();
  });

  it('refaz as opções ao trocar mensal por diário sem manter contrato encerrado antes da data', async () => {
    await abrir();
    const anteriores = filtros().length;
    fireEvent.click(screen.getByRole('button', { name: 'Diário', exact: true }));
    await waitFor(() => expect(filtros().length).toBeGreaterThan(anteriores));
    await esperarContexto({ referencia: '2026-06-30', periodo: 'DIARIO', compararAnoAnterior: false });
    expect(within(selecionar('Contrato')).getByRole('option', { name: 'Contrato vigente em 30 de junho' })).toBeInTheDocument();
    expect(screen.queryByRole('option', { name: 'Contrato encerrado em 10 de junho' })).not.toBeInTheDocument();
  });

  it('inclui e retira as opções exclusivas do ano anterior ao ligar e desligar a comparação', async () => {
    await abrir();
    const antesDeComparar = filtros().length;
    fireEvent.click(screen.getByRole('button', { name: 'Comparar ano anterior', exact: true }));
    await waitFor(() => expect(filtros().length).toBeGreaterThan(antesDeComparar));
    await esperarContexto({ referencia: '2026-06-30', periodo: 'MENSAL', compararAnoAnterior: true });
    expect(within(selecionar('Contrato')).getByRole('option', { name: 'Contrato exclusivo do ano anterior' })).toBeInTheDocument();
    expect(within(selecionar('Lotação')).getByRole('option', { name: 'Lotação exclusiva do ano anterior' })).toBeInTheDocument();
    const antesDeDesligar = filtros().length;
    fireEvent.click(screen.getByRole('button', { name: 'Comparando ano anterior', exact: true }));
    await waitFor(() => expect(filtros().length).toBeGreaterThan(antesDeDesligar));
    await esperarContexto({ referencia: '2026-06-30', periodo: 'MENSAL', compararAnoAnterior: false });
    expect(screen.queryByRole('option', { name: 'Contrato exclusivo do ano anterior' })).not.toBeInTheDocument();
    expect(screen.queryByRole('option', { name: 'Lotação exclusiva do ano anterior' })).not.toBeInTheDocument();
  });

  it('preserva recorte temporal e os quatro identificadores ao refinar as dimensões autorizadas', async () => {
    await abrir();
    await esperarContexto({ referencia: '2026-06-30', periodo: 'MENSAL', compararAnoAnterior: false });
    for (const [rotulo, valor] of [['Contrato', '71'], ['Lotação', '72'], ['Turno', '73'], ['Cargo', '74']]) {
      await waitFor(() => expect(selecionar(rotulo)).toBeEnabled());
      fireEvent.change(selecionar(rotulo), { target: { value: valor } });
      await waitFor(() => expect(selecionar(rotulo)).toHaveValue(valor));
      await waitFor(() => expect(selecionar(rotulo)).toBeEnabled());
    }
    await esperarContexto({
      referencia: '2026-06-30', periodo: 'MENSAL', compararAnoAnterior: false,
      contratoId: '71', lotacaoId: '72', jornadaId: '73', cargoId: '74',
    });
  });

  it('descarta filtros atrasados do período substituído antes de publicar opções na tela', async () => {
    await abrir();
    let configAntiga, liberarAntiga;
    const atrasada = new Promise((resolve) => { liberarAntiga = resolve; });
    respostaPersonalizada = (config) => {
      if (config.url === '/painel/efetivo/filtros' && config.params.referencia === '2026-07-31') {
        configAntiga = config;
        return atrasada;
      }
      return undefined;
    };
    fireEvent.change(screen.getByLabelText('Data de referência do indicador'), { target: { value: '2026-07-31' } });
    await waitFor(() => expect(configAntiga).toBeDefined());
    fireEvent.change(screen.getByLabelText('Data de referência do indicador'), { target: { value: '2026-06-30' } });
    await esperarContexto({ referencia: '2026-06-30', periodo: 'MENSAL', compararAnoAnterior: false });
    expect(configAntiga.signal.aborted).toBe(true);
    await act(async () => { liberarAntiga(ok(configAntiga, mensalJulho)); });
    expect(within(selecionar('Contrato')).getByRole('option', { name: 'Contrato encerrado em 10 de junho' })).toBeInTheDocument();
    expect(screen.queryByRole('option', { name: 'Contrato do período de julho' })).not.toBeInTheDocument();
  });

  it('mantém a restrição operacional devolvida pelo Java ao consultar o recorte histórico', async () => {
    respostaPersonalizada = (config) => config.url === '/painel/efetivo/filtros'
      ? ok(config, { contratos: mensalJunho.contratos, lotacoes: [], turnos: [], cargos: [], permiteDetalhamentoOperacional: false })
      : undefined;
    await abrir();
    await esperarContexto({ referencia: '2026-06-30', periodo: 'MENSAL', compararAnoAnterior: false });
    expect(selecionar('Contrato')).toBeEnabled();
    for (const rotulo of ['Lotação', 'Turno', 'Cargo']) expect(selecionar(rotulo)).toBeDisabled();
    for (const pedido of [...filtros(), ...dados()]) {
      expect(pedido.params).not.toHaveProperty('lotacaoId');
      expect(pedido.params).not.toHaveProperty('jornadaId');
      expect(pedido.params).not.toHaveProperty('cargoId');
    }
  });
});


it('inicia em diário nos dois endpoints sem ampliar o recorte por padrão', async () => {
  render(<IndicadorEfetivo referencia="2026-06-30" />);
  await esperarContexto({ referencia: '2026-06-30', periodo: 'DIARIO', compararAnoAnterior: false });
  expect(screen.getByRole('button', { name: 'Diário', exact: true })).toHaveAttribute('aria-pressed', 'true');
  expect(screen.queryByRole('option', { name: 'Contrato encerrado em 10 de junho' })).not.toBeInTheDocument();
});
