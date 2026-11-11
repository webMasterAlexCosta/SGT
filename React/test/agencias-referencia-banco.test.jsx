import React from 'react';
import { cleanup, fireEvent, render, screen } from '@testing-library/react';
import { afterEach, expect, it, vi } from 'vitest';
import '@testing-library/jest-dom/vitest';
import { TabelaAgencias } from '../src/modules/admin/pages/PaginaAgencias/components/TabelaAgencias/index.tsx';
afterEach(cleanup);
const agencia = { id: 3, numero: '1234', banco: { id: 7, codigo: '001', nome: 'Banco sintético' } };
it('resolve o banco pela referência aninhada emitida pelo Java', () => {
  const obterNomeBanco = vi.fn(id => id === 7 ? '001 - Banco sintético' : 'Sem banco');
  render(<TabelaAgencias agencias={[agencia]} obterNomeBanco={obterNomeBanco} aoEditar={vi.fn()} aoExcluir={vi.fn()} />);
  expect(screen.getByText('001 - Banco sintético')).toBeInTheDocument();
  expect(obterNomeBanco).toHaveBeenCalledWith(7);
});
it('preserva a agência e seu identificador nas ações da linha', () => {
  const aoEditar = vi.fn(), aoExcluir = vi.fn();
  render(<TabelaAgencias agencias={[agencia]} obterNomeBanco={() => 'Banco'} aoEditar={aoEditar} aoExcluir={aoExcluir} podeEscrever />);
  fireEvent.click(screen.getByRole('button', { name: 'Editar' }));
  fireEvent.click(screen.getByRole('button', { name: 'Excluir' }));
  expect(aoEditar).toHaveBeenCalledWith(agencia);
  expect(aoExcluir).toHaveBeenCalledWith(3);
});
