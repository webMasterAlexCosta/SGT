import React from 'react';
import { afterEach, beforeEach, expect, it, vi } from 'vitest';
import { cleanup, fireEvent, render, screen, waitFor } from '@testing-library/react';
import '@testing-library/jest-dom/vitest';
const { notificar } = vi.hoisted(() => ({ notificar: vi.fn() }));
vi.mock('../src/core/events.ts', async original => ({ ...(await original()), notificar }));
import { ModalPessoa } from '../src/modules/rh/pages/PaginaPessoasVinculos/components/ModalPessoa.tsx';
afterEach(cleanup); beforeEach(() => notificar.mockReset());
const montar = () => {
  const aoAtualizar = vi.fn().mockResolvedValue(true);
  render(<ModalPessoa aberto pessoa={{ id: 1, nome: 'Pessoa sintética', cpf: '***.000.000-**' }} podeGerenciar aoFechar={vi.fn()} aoAbrirVinculo={vi.fn()} aoNovoVinculo={vi.fn()} aoSolicitarRestritos={vi.fn()} aoAtualizar={aoAtualizar} aoCorrigir={vi.fn()} />);
  fireEvent.click(screen.getByRole('button', { name: 'Dados opcionais' }));
  return aoAtualizar;
};
it('mantém o endereço totalmente ausente como null na requisição', async () => {
  const salvar = montar();
  fireEvent.click(screen.getByRole('button', { name: 'Salvar opcionais' }));
  await waitFor(() => expect(salvar).toHaveBeenCalledTimes(1));
  expect(salvar.mock.lastCall[0]).toMatchObject({ endereco: null, limparEndereco: false });
});
it('preserva os campos normalizados de um endereço com logradouro', async () => {
  const salvar = montar();
  fireEvent.change(screen.getByLabelText('logradouro'), { target: { value: ' Rua sintética ' } });
  fireEvent.change(screen.getByLabelText('numero'), { target: { value: ' 17 ' } });
  fireEvent.click(screen.getByRole('button', { name: 'Salvar opcionais' }));
  await waitFor(() => expect(salvar).toHaveBeenCalledTimes(1));
  expect(salvar.mock.lastCall[0].endereco).toEqual({ logradouro: 'Rua sintética', numero: '17', bairro: null, cidade: null, uf: null, cep: null });
});
it('não apresenta um endereço parcial sem logradouro como um comando válido', async () => {
  const salvar = montar();
  fireEvent.change(screen.getByLabelText('numero'), { target: { value: '17' } });
  fireEvent.click(screen.getByRole('button', { name: 'Salvar opcionais' }));
  expect(salvar).not.toHaveBeenCalled();
  expect(notificar).toHaveBeenCalledWith('warning', 'Ao informar endereço, o logradouro é obrigatório.');
});
