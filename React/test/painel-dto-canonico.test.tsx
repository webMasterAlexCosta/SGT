import React from 'react';
import { afterEach, expect, test } from 'vitest';
import { cleanup, render, screen } from '@testing-library/react';
import { CoberturaFrequencia } from '../src/modules/painel/components/GestaoExecutiva';
import { FunilFrequencia } from '../src/modules/painel/components/FerramentasGestao';
import type { PainelGestaoDTOCoberturaFrequencia } from '../src/contracts/java';

afterEach(cleanup);
const frequencia: PainelGestaoDTOCoberturaFrequencia = {
  data: '2026-11-06', empregadosAtivos: 8, empregadosEsperados: 8, registrosEsperados: 5,
  pendentes: 3, percentualCobertura: 62.5, presencas: 4, ausencias: 1, ausenciasJustificadas: 0,
  faltasNaoJustificadas: 1, repousosTrabalhados: 0, trabalhosForaEscalaSemRt: 0,
};
test('cartão de cobertura lê os lançamentos do campo canônico do DTO Java', () => {
  render(<CoberturaFrequencia frequencia={frequencia} podeOperar={false} aoNavegar={() => {}} />);
  expect(screen.getByText('Registros lançados').parentElement?.querySelector('strong')?.textContent).toBe('5');
});
test('funil lê o mesmo total canônico e conserva o zero quando não houve lançamento', () => {
  const { rerender } = render(<FunilFrequencia frequencia={frequencia} />);
  expect(screen.getByText('Registros lançados').parentElement?.querySelector('strong')?.textContent).toBe('5');
  rerender(<FunilFrequencia frequencia={{ ...frequencia, registrosEsperados: 0 }} />);
  expect(screen.getByText('Registros lançados').parentElement?.querySelector('strong')?.textContent).toBe('0');
});
