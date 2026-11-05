// Fontes: RelatorioFrequenciaConsolidadoServico.contratosParaFiltros inclui
// acervo inativo autorizado; RelatorioTerceirizadosServico impõe size >= 10.
import React from 'react';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { cleanup, fireEvent, render, screen, waitFor, within } from '@testing-library/react';
import '@testing-library/jest-dom/vitest';
import { MemoryRouter } from 'react-router-dom';
const { requisitar } = vi.hoisted(() => ({ requisitar: vi.fn() }));
vi.mock('../src/core/http.js', () => ({ requisitar, extrairLista: r => Array.isArray(r) ? r : [], extrairPagina: r => r }));
vi.mock('../src/modules/auth/context.jsx', () => ({ useAutenticacao: () => ({ user: { perfil: 'GESTOR_LOCAL', autoridade: 'PERFIL_GESTOR_LOCAL' } }) }));
vi.mock('../src/core/exportacao/context.jsx', () => ({ useExportacaoRelatorio: () => ({ iniciarExportacao: vi.fn() }) }));
vi.mock('../src/core/events.js', () => ({ notificar: vi.fn() }));
vi.mock('../src/modules/rh/pages/PaginaRelatoriosTerceirizados/components/GraficosRelatorios/index.jsx', () => ({ GraficosRelatorios: () => null }));
import { PaginaRelatoriosAdministrativos } from '../src/modules/rh/pages/PaginaRelatoriosAdministrativos/index.jsx';
import { PaginaRelatoriosTerceirizados } from '../src/modules/rh/pages/PaginaRelatoriosTerceirizados/index.jsx';
const paginaVazia = { content: [], number: 0, totalPages: 0, totalElements: 0, first: true, last: true };
const colaboradores = Array.from({ length: 13 }, (_, i) => ({ id: i + 1, nome: `Pessoa sintética ${i + 1}`, cpf: '***', active: true, status: 'ATIVO' }));
beforeEach(() => {
  requisitar.mockReset();
  requisitar.mockImplementation(async ({ url, params = {} }) => {
    if (url === '/relatorios/filtros') return { contratos: [
      { id: 1, numeroContrato: 'CONTRATO-ATIVO', ativo: true },
      { id: 2, numeroContrato: 'CONTRATO-HISTORICO-INATIVO', ativo: false },
    ], lotacoes: [], turnos: [] };
    if (url === '/relatorios/terceirizados/consolidado') {
      // Mesma regra Java da candidata congelada; não aceita size=5 por hipótese.
      const size = Math.min(Math.max(Number(params.size ?? 100), 10), 5000);
      const number = Number(params.page ?? 0);
      return { empregados: { content: colaboradores.slice(number * size, (number + 1) * size), size, number, totalElements: colaboradores.length, totalPages: Math.ceil(colaboradores.length / size), first: number === 0, last: (number + 1) * size >= colaboradores.length }, indicadores: {}, contratos: [], lotacoes: [], statusDisponiveis: [] };
    }
    return paginaVazia;
  });
});
afterEach(cleanup);
describe('Contratos históricos e paginação contra as regras reais do Java', () => {
  it('preserva no seletor administrativo o contrato histórico inativo autorizado pelo Java', async () => {
    render(<PaginaRelatoriosAdministrativos />);
    await screen.findByRole('option', { name: 'CONTRATO-ATIVO' });
    expect(screen.queryByRole('option', { name: 'CONTRATO-HISTORICO-INATIVO' })).toBeInTheDocument();
  });
  it('mostra cinco terceirizados quando o Java devolve seu bloco mínimo de dez, sem perder a segunda metade', async () => {
    render(<MemoryRouter><PaginaRelatoriosTerceirizados /></MemoryRouter>);
    await screen.findByText('Pessoa sintética 1');
    const linhas = () => [...document.querySelectorAll('tbody tr')];
    expect(linhas()).toHaveLength(5);
    expect(screen.queryByText('Pessoa sintética 6')).not.toBeInTheDocument();
    fireEvent.click(screen.getByRole('button', { name: /Próxima/i }));
    await screen.findByText('Pessoa sintética 6');
    expect(linhas()).toHaveLength(5);
    expect(screen.queryByText('Pessoa sintética 11')).not.toBeInTheDocument();
    fireEvent.click(screen.getByRole('button', { name: /Próxima/i }));
    await screen.findByText('Pessoa sintética 13');
    expect(linhas()).toHaveLength(3);
  });
});
