/** Oráculo independente. Copiar sem alterar para test/ da cópia de execução. */
import React from 'react';
import { act, cleanup, fireEvent, render, screen, waitFor, within } from '@testing-library/react';
import '@testing-library/jest-dom/vitest';
import { afterEach, beforeEach, expect, it, vi } from 'vitest';
const { listar, filtros } = vi.hoisted(() => ({ listar: vi.fn(), filtros: vi.fn() }));
vi.mock('../src/services/rh/ServicoFrequencia.js', () => ({ ServicoFrequencia: { carregarFiltrosRelatorio: filtros } }));
vi.mock('../src/services/rh/ServicoRelatorioAdministrativo.js', () => ({
  ServicoRelatorioAdministrativo: { listar },
  criarChaveIdempotenciaRelatorio: () => 'intencao-sintetica-apenas-consulta',
}));
vi.mock('../src/modules/auth/context.jsx', () => ({ useAutenticacao: () => ({ user: { perfil: 'PERFIL_GESTOR_LOCAL' } }) }));
vi.mock('../src/core/events.js', () => ({ notificar: vi.fn() }));
import { PaginaRelatoriosAdministrativos } from '../src/modules/rh/pages/PaginaRelatoriosAdministrativos/index.jsx';
const pendente = () => {
  let resolve;
  const promise = new Promise(ok => { resolve = ok; });
  return { promise, resolve };
};
const pagina = (id, tipo) => ({
  content: [{ id, tipo, versao: 1, estado: 'VIGENTE', natureza: 'ORIGINAL', sujeitoARevisao: false }],
  number: 0, totalPages: 1, totalElements: 1, first: true, last: true,
});
beforeEach(() => { vi.resetAllMocks(); filtros.mockResolvedValue({ contratos: [], lotacoes: [], turnos: [] }); });
afterEach(cleanup);
it('mantém o histórico de absenteísmo selecionado quando a consulta anterior de frequência termina por último', async () => {
  const frequencia = pendente();
  const absenteismo = pendente();
  listar.mockImplementation(params => params.tipo === 'ABSENTEISMO' ? absenteismo.promise : frequencia.promise);
  render(<PaginaRelatoriosAdministrativos podeEmitir={false} />);
  await waitFor(() => expect(listar).toHaveBeenCalledWith(expect.objectContaining({ tipo: 'FREQUENCIA_CONSOLIDADA' })));
  fireEvent.change(screen.getByRole('combobox', { name: 'Relatório', exact: true }), { target: { value: 'ABSENTEISMO' } });
  await waitFor(() => expect(listar).toHaveBeenCalledWith(expect.objectContaining({ tipo: 'ABSENTEISMO' })));
  await act(async () => { absenteismo.resolve(pagina(202, 'ABSENTEISMO')); await absenteismo.promise; });
  const tabela = within(screen.getByRole('region', { name: 'Histórico de emissões' })).getByRole('table');
  expect(within(tabela).getByText('#202')).toBeInTheDocument();
  await act(async () => { frequencia.resolve(pagina(101, 'FREQUENCIA_CONSOLIDADA')); await frequencia.promise; });
  expect(screen.getByRole('combobox', { name: 'Relatório', exact: true })).toHaveValue('ABSENTEISMO');
  expect(within(tabela).getByText('#202')).toBeInTheDocument();
  expect(within(tabela).queryByText('#101')).not.toBeInTheDocument();
});
