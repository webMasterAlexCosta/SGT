import React from 'react';
import '@testing-library/jest-dom/vitest';
import { act, cleanup, fireEvent, render, screen, waitFor, within } from '@testing-library/react';
import { beforeEach, afterEach, describe, expect, it, vi } from 'vitest';
import type { AxiosAdapter, InternalAxiosRequestConfig } from 'axios';
import { api } from '../src/core/http';
import { invalidarGeracaoSessao } from '../src/core/security/geracaoSessao';
import PainelGerencial from '../src/modules/painel/components/PainelGerencial';
import { fixtureVisoes } from './fixtures/painel-visoes';
import { linhasVisao } from '../src/modules/painel/components/PainelGerencial/exportacao';

let anterior: typeof api.defaults.adapter;
let pedidos: InternalAxiosRequestConfig[];
beforeEach(() => {
  anterior = api.defaults.adapter; pedidos = [];
  api.defaults.adapter = (async c => {
    pedidos.push(c);
    const data = c.url === '/painel/atividades' ? { content: [] } : fixtureVisoes(c.params.granularidade);
    return { config: c, data, status: 200, statusText: 'OK', headers: {} };
  }) satisfies AxiosAdapter;
});
afterEach(() => { cleanup(); api.defaults.adapter = anterior; invalidarGeracaoSessao(); });

describe('paridade do painel gerencial', () => {
  it.each(['resumo', 'contratos', 'operacao'] as const)('%s oferece os cinco calendários, filtros e comparação no mesmo pedido', async visao => {
    render(<PainelGerencial visao={visao} referenciaInicial="2026-11-10" perfil={{ presidente: true }} aoNavegar={vi.fn()} />);
    await screen.findByRole('option', { name: 'Contrato sintético' });
    expect(pedidos[0].url).toBe('/painel/visoes');
    expect(pedidos[0].params.granularidade).toBe('DIARIO');
    const periodos = screen.getByRole('group', { name: 'Período de análise' });
    expect(within(periodos).getAllByRole('button').map(b => b.textContent)).toEqual(['Diário', 'Semanal', 'Mensal', 'Trimestral', 'Anual']);
    fireEvent.click(within(periodos).getByRole('button', { name: 'Trimestral' }));
    await waitFor(() => expect(pedidos.at(-1)?.params.granularidade).toBe('TRIMESTRAL'));
    fireEvent.change(screen.getByRole('combobox', { name: 'Prestador / empresa' }), { target: { value: '11' } });
    fireEvent.change(screen.getByRole('combobox', { name: 'Contrato', exact: true }), { target: { value: '8' } });
    fireEvent.click(screen.getByRole('checkbox', { name: 'Comparar período anterior' }));
    await waitFor(() => expect(pedidos.at(-1)?.params).toMatchObject({ granularidade: 'TRIMESTRAL', contratoId: 8, prestadorId: 11, comparar: false }));
    fireEvent.change(screen.getByRole('combobox', { name: 'Unidade' }), { target: { value: '9' } });
    await waitFor(() => expect(pedidos.at(-1)?.params).toMatchObject({ unidadeId: 9, tipoUnidade: 'LOTACAO' }));
    expect(pedidos.at(-1)?.params.contratoId).toBeUndefined();
    fireEvent.click(screen.getByRole('button', { name: 'Limpar filtros' }));
    await waitFor(() => expect(pedidos.at(-1)?.params.unidadeId).toBeUndefined());
  });

  it('oculta resultado e desabilita exportação enquanto um novo recorte está pendente', async () => {
    render(<PainelGerencial visao="resumo" referenciaInicial="2026-11-10" perfil={{}} aoNavegar={vi.fn()} />);
    await screen.findByRole('button', { name: /Contratos vigentes/ });
    let pendente = false;
    api.defaults.adapter = () => new Promise(() => { pendente = true; });
    fireEvent.click(screen.getByRole('button', { name: 'Anual', exact: true }));
    expect(screen.queryByRole('button', { name: /Contratos vigentes/ })).toBeNull();
    expect(screen.getByRole('button', { name: 'Exportar XLSX' })).toBeDisabled();
    await waitFor(() => expect(pendente).toBe(true));
    await act(async () => { invalidarGeracaoSessao(); });
    expect(screen.queryByRole('button', { name: /Contratos vigentes/ })).toBeNull();
    expect(pendente).toBe(true);
  });

  it('exporta filtros, período e indicadores complementares da visão', () => {
    const d = fixtureVisoes('ANUAL');
    const linhas = linhasVisao(d, 'contratos');
    expect(linhas[0]).toEqual(['SGT — Contratos', 'ANUAL']);
    expect(linhas.flat()).toContain('Contratos sem data final');
    expect(linhas.flat()).toContain('01/01/2026 a 10/11/2026');
  });
});
