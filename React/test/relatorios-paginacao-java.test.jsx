// Reprodução dos limites do Java congelado: mínimo 25/máximo 500 e paginação
// por colaborador, com várias linhas temporais possíveis para a mesma pessoa.
import React from 'react';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { cleanup, fireEvent, render, screen, waitFor } from '@testing-library/react';
import '@testing-library/jest-dom/vitest';

const { requisitar, iniciarExportacao } = vi.hoisted(() => ({ requisitar: vi.fn(), iniciarExportacao: vi.fn() }));
vi.mock('../src/core/http.ts', () => ({ requisitar, extrairLista: value => Array.isArray(value) ? value : value?.content || [] }));
vi.mock('../src/core/events.ts', () => ({ notificar: vi.fn() }));
vi.mock('../src/core/exportacao/context.tsx', () => ({ useExportacaoRelatorio: () => ({ iniciarExportacao }) }));
vi.mock('recharts', () => Object.fromEntries(['PieChart', 'Pie', 'Cell', 'BarChart', 'Bar', 'XAxis', 'YAxis', 'CartesianGrid', 'Tooltip', 'Legend', 'ResponsiveContainer', 'LabelList'].map(name => [name, () => null])));

import { PaginaRelatorioFrequencia } from '../src/modules/rh/pages/PaginaRelatorioFrequencia/index.tsx';
import { PaginaRelatorioAbsenteismo } from '../src/modules/rh/pages/PaginaRelatorioAbsenteismo/index.tsx';

let totalColaboradores;
let contextosDoPrimeiro;
const paginarComoJava = params => {
  const tamanho = Math.min(Math.max(Number(params.size ?? 100), 25), 500);
  const totalPaginas = Math.ceil(totalColaboradores / tamanho);
  const pagina = totalPaginas ? Math.min(Math.max(0, Number(params.page || 0)), totalPaginas - 1) : 0;
  const ids = Array.from({ length: totalColaboradores }, (_, index) => index + 1).slice(pagina * tamanho, (pagina + 1) * tamanho);
  return { ids, pagina, tamanho, totalColaboradores, totalPaginas, primeira: pagina === 0, ultima: !totalPaginas || pagina + 1 >= totalPaginas };
};
const linhasPrincipais = () => [...screen.getAllByRole('table')[0].tBodies[0].children];

beforeEach(() => {
  totalColaboradores = 31;
  contextosDoPrimeiro = 1;
  iniciarExportacao.mockReset().mockResolvedValue({ id: 'job-sintetico' });
  requisitar.mockReset().mockImplementation(async ({ url, params = {} }) => {
    if (url === '/relatorios/filtros') return {
      contratos: [
        { id: 1, numeroContrato: 'ATIVO-01', descricao: 'Contrato ativo', ativo: true },
        { id: 2, numeroContrato: 'HISTORICO-02', descricao: 'Contrato histórico autorizado', ativo: false },
      ], lotacoes: [], turnos: [],
    };
    if (url === '/relatorios/frequencia-consolidada/paginada') {
      const { ids, ...pagina } = paginarComoJava(params);
      return {
        ...pagina, completo: true, inicio: params.inicio, fim: params.fim,
        registros: ids.flatMap(id => Array.from({ length: id === 1 ? contextosDoPrimeiro : 1 }, (_, context) => ({
          empregadoId: id, chaveAlocacao: `${id}-contexto-${context}`, nome: `Pessoa ${id}`,
          cargo: `Cargo ${id}`, lotacao: id === 1 && contextosDoPrimeiro > 1 ? `Lotação histórica ${context + 1}` : `Lotação ${id}`,
          contrato: `Contrato do contexto ${context + 1}`, turno: 'Diurno', diario: {}, totais: {},
        }))),
      };
    }
    if (url === '/relatorios/absenteismo/paginado') {
      const { ids, ...pagina } = paginarComoJava(params);
      return { ...pagina, registros: ids.map(id => ({ empregadoId: id, nome: `Pessoa ${id}`, cargo: `Cargo ${id}`, lotacao: 'Lotação', turno: 'Diurno', diasTrabalhaveis: 20, faltas: 0, atestados: 0, dsrPerdido: 0, taxaIndividual: 0, disrupcao: 0 })) };
    }
    if (url === '/relatorios/absenteismo/resumo') return { totalColaboradores, diasTrabalhaveis: totalColaboradores * 20, maioresTaxas: [] };
    throw new Error(`Consulta não prevista pela reprodução: ${url}`);
  });
});
afterEach(cleanup);

describe('Filtros históricos autorizados pelo Java', () => {
  it('oferece o contrato inativo no relatório mensal e o envia como filtro', async () => {
    render(<PaginaRelatorioFrequencia />);
    await waitFor(() => expect(requisitar).toHaveBeenCalledWith(expect.objectContaining({ url: '/relatorios/filtros' })));
    fireEvent.click(screen.getByRole('button', { name: /Todos os contratos/i }));
    fireEvent.click(await screen.findByRole('checkbox', { name: /Contrato histórico autorizado/i }));
    fireEvent.click(screen.getByRole('button', { name: 'Gerar Relatório' }));
    await screen.findByText('Pessoa 1');
    expect(requisitar).toHaveBeenCalledWith(expect.objectContaining({ url: '/relatorios/frequencia-consolidada/paginada', params: expect.objectContaining({ contratoIds: ['2'] }) }));
  });

  it('oferece o contrato inativo no absenteísmo e o envia como filtro', async () => {
    render(<PaginaRelatorioAbsenteismo />);
    await screen.findByRole('option', { name: /HISTORICO-02/ });
    fireEvent.change(screen.getByLabelText('Contrato', { exact: true }), { target: { value: '2' } });
    fireEvent.click(screen.getByRole('button', { name: 'CALCULAR' }));
    await screen.findByText('Pessoa 1');
    expect(requisitar).toHaveBeenCalledWith(expect.objectContaining({ url: '/relatorios/absenteismo/paginado', params: expect.objectContaining({ contratoId: '2' }) }));
  });
});

describe('Páginas visuais de cinco sobre os blocos Java de vinte e cinco', () => {
  for (const kind of ['frequencia', 'absenteismo']) {
    it(`${kind}: navega pelos 31 colaboradores sem perder a cauda ou repetir IDs`, async () => {
      render(kind === 'frequencia' ? <PaginaRelatorioFrequencia /> : <PaginaRelatorioAbsenteismo />);
      fireEvent.click(screen.getByRole('button', { name: kind === 'frequencia' ? 'Gerar Relatório' : 'CALCULAR' }));
      await screen.findByText('Pessoa 1');
      expect(screen.getByLabelText(/por página/)).toHaveValue('5');
      const vistos = [];
      for (let pagina = 0; pagina < 7; pagina += 1) {
        const primeiraPessoa = pagina * 5 + 1;
        await screen.findByText(`Pessoa ${primeiraPessoa}`);
        const rows = linhasPrincipais();
        expect(rows).toHaveLength(pagina === 6 ? 1 : 5);
        const ids = rows.map(row => Number(row.textContent.match(/Pessoa (\d+)/)[1]));
        expect(ids).toEqual(Array.from({ length: pagina === 6 ? 1 : 5 }, (_, index) => primeiraPessoa + index));
        vistos.push(...ids);
        if (pagina < 6) fireEvent.click(screen.getByRole('button', { name: /Próxima/ }));
      }
      expect(vistos).toEqual(Array.from({ length: 31 }, (_, index) => index + 1));
      expect(screen.getByRole('button', { name: /Próxima/ })).toBeDisabled();
      fireEvent.click(screen.getByRole('button', { name: /Anterior/ }));
      await screen.findByText('Pessoa 26');
      expect(linhasPrincipais()).toHaveLength(5);
      const requests = requisitar.mock.calls.map(([request]) => request).filter(request => request.url.endsWith(kind === 'frequencia' ? '/frequencia-consolidada/paginada' : '/absenteismo/paginado'));
      expect(requests.some(request => request.params.page === 1 && request.params.size === 25)).toBe(true);
      expect(requests.every(request => request.params.size === 25)).toBe(true);
    });
  }

  it('mudar o tamanho visual volta à primeira página e conserva o total oficial', async () => {
    render(<PaginaRelatorioFrequencia />);
    fireEvent.click(screen.getByRole('button', { name: 'Gerar Relatório' }));
    await screen.findByText('Pessoa 1');
    expect(linhasPrincipais()).toHaveLength(5);
    fireEvent.click(screen.getByRole('button', { name: /Próxima/ }));
    await screen.findByText('Pessoa 6');
    fireEvent.change(screen.getByLabelText(/por página/), { target: { value: '25' } });
    await screen.findByText('Pessoa 1');
    expect(linhasPrincipais()).toHaveLength(25);
    expect(screen.getByRole('button', { name: /Anterior/ })).toBeDisabled();
    fireEvent.click(screen.getByRole('button', { name: /Próxima/ }));
    await screen.findByText('Pessoa 31');
    expect(linhasPrincipais()).toHaveLength(6);
    expect(screen.getByRole('button', { name: /Próxima/ })).toBeDisabled();
  });

  it('mantém cinco linhas principais e todos os contextos temporais do colaborador nos detalhes', async () => {
    totalColaboradores = 6;
    contextosDoPrimeiro = 6;
    render(<PaginaRelatorioFrequencia />);
    fireEvent.click(screen.getByRole('button', { name: 'Gerar Relatório' }));
    await screen.findByText('Pessoa 5');
    expect(linhasPrincipais()).toHaveLength(5);
    const details = linhasPrincipais()[0].querySelector('details');
    expect(details).toBeInTheDocument();
    expect(details.open).toBe(false);
    expect(details.querySelector('summary')).toHaveTextContent('6 contextos históricos');
    fireEvent.click(details.querySelector('summary'));
    await waitFor(() => expect(details.open).toBe(true));
    for (let context = 1; context <= 6; context += 1) expect(screen.getByText(`Lotação histórica ${context}`)).toBeVisible();
    expect(screen.getByText(/colaborador\(es\) encontrados/)).toHaveTextContent('6');
    fireEvent.click(screen.getByRole('button', { name: /Próxima/ }));
    await screen.findByText('Pessoa 6');
    expect(linhasPrincipais()).toHaveLength(1);
    expect(screen.queryByText('Lotação histórica 6')).not.toBeInTheDocument();
  });

  it('a exportação mensal conserva os filtros completos, sem page/size do recorte visual', async () => {
    render(<PaginaRelatorioFrequencia />);
    fireEvent.click(screen.getByRole('button', { name: 'Gerar Relatório' }));
    await screen.findByText('Pessoa 1');
    fireEvent.click(screen.getByTitle('Exportar dados em formato de planilha Excel (.xlsx)'));
    await waitFor(() => expect(iniciarExportacao).toHaveBeenCalled());
    const [request] = iniciarExportacao.mock.calls[0];
    expect(request).toEqual(expect.objectContaining({ tipo: 'frequencia', formato: 'XLSX', totalEstimado: 31 }));
    expect(request.parametros).not.toHaveProperty('page');
    expect(request.parametros).not.toHaveProperty('size');
  });
});
