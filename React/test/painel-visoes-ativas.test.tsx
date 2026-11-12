import React from 'react';
import '@testing-library/jest-dom/vitest';
import { afterEach, describe, expect, it, vi } from 'vitest';
import { cleanup, fireEvent, render, screen, within } from '@testing-library/react';
import { Painel } from '../src/modules/painel/pages/index.tsx';

vi.mock('../src/modules/painel/pages/index.ts', () => ({ usePainel: () => ({
  dados: { referencia: '2026-11-11', geradoEm: '2026-11-12T08:00:00Z', periodo: { inicio: '2026-11-11', fim: '2026-11-11', dias: 1 }, escopo: { nome: 'Laboratório' } },
  atividades: [], rankingLotacoes: [], rankingCarregando: false,
  referencia: '2026-11-11', periodoDias: 1, hoje: '2026-11-12', carregando: false,
  erro: '', processamento: {}, perfil: {},
  acoes: { setReferencia: vi.fn(), setPeriodoDias: vi.fn(), recarregar: vi.fn(), navegar: vi.fn(), carregarRanking: vi.fn() },
}) }));
vi.mock('../src/modules/painel/components/PainelDesempenho', () => ({ default: () => <h1>Desempenho operacional</h1> }));
vi.mock('../src/modules/painel/components/IndicadorEfetivo/index.tsx', () => ({ IndicadorEfetivo: () => <div>Efetivo operacional</div> }));
afterEach(cleanup);

describe('visões vigentes do painel', () => {
  it('oferece somente as visões ativas e mantém o acesso ao desempenho', async () => {
    render(<Painel onNavigate={vi.fn()} />);
    const nav = screen.getByRole('navigation', { name: 'Modo de visualização do painel' });
    expect(within(nav).getAllByRole('button').map(b => b.textContent)).toEqual(['Desempenho', 'Resumo', 'Contratos', 'Operação', 'Tudo']);
    expect(await screen.findByRole('heading', { name: 'Desempenho operacional' })).toBeTruthy();
    fireEvent.click(within(nav).getByRole('button', { name: 'Resumo', exact: true }));
    expect(screen.getByRole('heading', { name: 'Resumo gerencial', exact: true })).toBeTruthy();
    expect(screen.getByRole('combobox', { name: 'Período analítico' })).toHaveValue('1');
    expect(screen.queryByText(/governança/i)).toBeNull();
  });

  it('mantém integridade, comparações e auditoria em suas visões operacionais', () => {
    render(<Painel onNavigate={vi.fn()} />);
    const nav = screen.getByRole('navigation', { name: 'Modo de visualização do painel' });
    fireEvent.click(within(nav).getByRole('button', { name: 'Contratos', exact: true }));
    expect(screen.getByRole('heading', { name: 'Contratos e dimensionamento', exact: true })).toBeTruthy();
    expect(screen.getByRole('heading', { name: 'Saúde contratual', exact: true })).toBeTruthy();
    fireEvent.click(within(nav).getByRole('button', { name: 'Operação', exact: true }));
    expect(screen.getByRole('heading', { name: 'Operação e frequência', exact: true })).toBeTruthy();
    expect(screen.getByRole('heading', { name: 'Confiabilidade e integridade', exact: true })).toBeTruthy();
    expect(screen.getByRole('heading', { name: 'Atividade gerencial recente', exact: true })).toBeTruthy();
    expect(screen.getByText('Período analisado').parentElement).toHaveTextContent('1 dia');
    expect(screen.getByText(/Nesta janela diária, o período e a data de referência correspondem ao mesmo dia/)).toBeVisible();
    expect(screen.queryByText(/checklist de governança/i)).toBeNull();
  });
});
