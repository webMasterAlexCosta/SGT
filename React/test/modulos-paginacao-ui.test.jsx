import React from 'react';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { act, cleanup, fireEvent, render, renderHook, screen, waitFor, within } from '@testing-library/react';
import '@testing-library/jest-dom/vitest';
import { MemoryRouter } from 'react-router-dom';

const { requisitar, usuario, atualizarPerfil, iniciarExportacao } = vi.hoisted(() => ({
  requisitar: vi.fn(), usuario: { id: 99, perfil: 'GESTOR_LOCAL', gestaoLocalId: 1 }, atualizarPerfil: vi.fn(), iniciarExportacao: vi.fn(),
}));
vi.mock('../src/core/http.ts', async (original) => ({ ...(await original()), requisitar }));
vi.mock('../src/modules/auth/context.tsx', () => ({ useAutenticacao: () => ({ user: usuario, atualizarPerfil, atualizarInformacoesSessao: atualizarPerfil }) }));
vi.mock('../src/core/security/useConfirmacaoSenha.tsx', () => ({ useConfirmacaoSenha: () => ({ confirmarComSenha: vi.fn(), ModalConfirmacao: () => null }) }));
vi.mock('../src/core/exportacao/context.tsx', () => ({ useExportacaoRelatorio: () => ({ iniciarExportacao }) }));
vi.mock('../src/modules/rh/pages/PaginaRelatoriosTerceirizados/components/GraficosRelatorios/index.tsx', () => ({ GraficosRelatorios: () => null }));

import { CadastroGenerico } from '../src/modules/admin/components/CadastroGenerico.tsx';
import { PaginaPessoasVinculos } from '../src/modules/rh/pages/PaginaPessoasVinculos/index.tsx';
import { PaginaUsuarios } from '../src/modules/admin/pages/PaginaUsuarios/index.tsx';
import { PaginaRelatoriosTerceirizados } from '../src/modules/rh/pages/PaginaRelatoriosTerceirizados/index.tsx';
import { PaginaRelatoriosAdministrativos } from '../src/modules/rh/pages/PaginaRelatoriosAdministrativos/index.tsx';
import { useLogicaFrequencia } from '../src/modules/rh/pages/PaginaFrequencia/useLogicaFrequencia.ts';
import { TAMANHO_PAGINA as LOTE_CONSULTA_COMPLETA } from '../src/services/rh/paginacao.ts';
import { RELATORIO_TERCEIRIZADOS_TAMANHO_MAXIMO } from '../src/services/rh/ServicoRelatorioTerceirizados.ts';

const itens = Array.from({ length: 7 }, (_, i) => ({ id: i + 1, nome: `Pessoa ${i + 1}`, username: `Usuario ${i + 1}`,
  pessoaNome: `Pessoa ${i + 1}`, cpf: '***', pessoaCpf: '***', perfil: 'OPERACIONAL', active: true,
  dataInicio: '2025-01-01', estadoNegocial: 'REGISTRADO', situacaoTemporal: 'VIGENTE', gestaoLocalId: 1,
  contratoId: 1, contratoNumero: 'SINTETICO', matricula: '00000001' }));
const pagina = (lista, params = {}) => {
  const size = Number(params.size ?? params.tamanho ?? 20);
  const number = Number(params.page ?? params.pagina ?? 0);
  const totalPages = Math.ceil(lista.length / size);
  return { content: lista.slice(number * size, (number + 1) * size), size, number,
    totalElements: lista.length, totalPages, first: number === 0, last: number + 1 >= totalPages };
};

beforeEach(() => {
  usuario.perfil = 'GESTOR_LOCAL'; requisitar.mockReset(); atualizarPerfil.mockReset(); iniciarExportacao.mockReset();
  requisitar.mockImplementation(async ({ url, params }) => {
    if (['/pessoas', '/vinculos-contratuais', '/cargos', '/empregados/filtrados-frequencia'].includes(url)) return pagina(itens, params);
    if (url === '/usuarios' || url === '/tipos-contratacao') return itens;
    if (url === '/lotacoes') return [{ id: 1, nome: 'Lotação', turnos: [{ id: 1, descricao: 'Turno' }] }];
    if (url === '/relatorios/terceirizados/consolidado') return { empregados: pagina(itens, params), indicadores: {}, contratos: [], lotacoes: [], statusDisponiveis: [] };
    if (url === '/relatorios/administrativos/emissoes') return pagina(itens.map((i) => ({ ...i, tipo: 'FREQUENCIA_CONSOLIDADA', versao: 1, estado: 'VIGENTE' })), params);
    if (url === '/relatorios/administrativos/previa') return { conteudo: { registros: itens }, metadados: {} };
    if (url === '/relatorios/filtros') return { contratos: [], lotacoes: [], turnos: [] };
    if (url === '/frequencias/fechamentos/atual') return null;
    return [];
  });
});
afterEach(cleanup);
const linhas = (scope = document) => scope.querySelectorAll('tbody tr');

describe('Cinco itens visíveis por padrão, com acesso às páginas seguintes', () => {
  it('Pessoas e Vínculos usam cinco sem reduzir a consulta detalhada auxiliar', async () => {
    render(<PaginaPessoasVinculos />);
    await screen.findByText('Pessoa 1');
    expect(linhas()).toHaveLength(5);
    fireEvent.click(screen.getByRole('button', { name: /Próxima/ }));
    await screen.findByText('Pessoa 6');
    expect(linhas()).toHaveLength(2);
    fireEvent.click(screen.getByRole('tab', { name: /Vínculos/ }));
    await screen.findByText('Pessoa 1');
    expect(linhas()).toHaveLength(5);
  });

  it('CadastroGenerico com Page começa em cinco e navega até o último registro', async () => {
    render(<CadastroGenerico title="Cargos" serviceEndpoint="/cargos" columns={[{ key: 'nome', label: 'Nome' }]} fields={[]} />);
    await screen.findByText('Pessoa 1');
    expect(linhas()).toHaveLength(5);
    expect(screen.getByLabelText('Itens por página')).toHaveValue('5');
    fireEvent.click(screen.getByRole('button', { name: 'Próxima' }));
    await screen.findByText('Pessoa 7');
    expect(linhas()).toHaveLength(2);
  });

  it('CadastroGenerico com lista completa pagina no cliente e mantém a busca no conjunto', async () => {
    render(<CadastroGenerico title="Tipos" serviceEndpoint="/tipos-contratacao" columns={[{ key: 'nome', label: 'Nome' }]} fields={[]} paginado={false} />);
    await screen.findByText('Pessoa 7');
    expect(linhas()).toHaveLength(5);
    fireEvent.change(screen.getByRole('searchbox', { name: 'Buscar em Tipos' }), { target: { value: 'Pessoa 1' } });
    expect(await screen.findByText('Pessoa 1')).toBeInTheDocument();
    expect(linhas()).toHaveLength(1);
  });

  it('Usuários mostra cinco e preserva os demais para a segunda página', async () => {
    render(<PaginaUsuarios />);
    await screen.findByText('Usuario 1');
    expect(linhas()).toHaveLength(5);
    fireEvent.click(screen.getByRole('button', { name: 'Próxima página' }));
    await screen.findByText('Usuario 7');
    expect(linhas()).toHaveLength(2);
  });

  it('Relatório de terceirizados começa em cinco sem mudar o bloco completo de exportação', async () => {
    render(<MemoryRouter><PaginaRelatoriosTerceirizados /></MemoryRouter>);
    await screen.findByText('Pessoa 1');
    expect(linhas()).toHaveLength(5);
    expect(RELATORIO_TERCEIRIZADOS_TAMANHO_MAXIMO).toBe(5000);
    fireEvent.click(screen.getByRole('button', { name: /Próxima/i }));
    await screen.findByText('Pessoa 7');
    expect(linhas()).toHaveLength(2);
  });

  it('Histórico das emissões administrativas começa em cinco', async () => {
    render(<PaginaRelatoriosAdministrativos />);
    await screen.findByText('#1');
    const historico = screen.getByRole('region', { name: 'Histórico de emissões' });
    expect(linhas(historico)).toHaveLength(5);
    expect(within(historico).getByLabelText('Itens por página')).toHaveValue('5');
    fireEvent.click(within(historico).getByRole('button', { name: 'Próxima' }));
    await screen.findByText('#7');
    expect(linhas(historico)).toHaveLength(2);
  });

  it('Prévia administrativa pagina o snapshot completo sem eliminar registros posteriores', async () => {
    render(<PaginaRelatoriosAdministrativos />);
    fireEvent.click(screen.getByRole('button', { name: 'Gerar prévia' }));
    await screen.findByText('Pessoa 1');
    const tabela = screen.getByRole('columnheader', { name: 'Colaborador' }).closest('table');
    expect(linhas(tabela)).toHaveLength(5);
    fireEvent.click(screen.getByRole('button', { name: 'Próxima página do conteúdo' }));
    await screen.findByText('Pessoa 7');
    expect(linhas(tabela)).toHaveLength(2);
  });

  it('Grade de frequência usa cinco e mantém o lote completo de consultas auxiliares', async () => {
    const { result } = renderHook(() => useLogicaFrequencia());
    await waitFor(() => expect(result.current.estado.listaDaFrequencia.length).toBeGreaterThan(0));
    expect(result.current.estado.listaDaFrequencia).toHaveLength(5);
    expect(result.current.estado.paginacaoGrade.tamanho).toBe(5);
    expect(LOTE_CONSULTA_COMPLETA).toBe(1000);
    await act(async () => result.current.acoes.setPaginaGrade(1));
    await waitFor(() => expect(result.current.estado.listaDaFrequencia).toHaveLength(2));
  });
});
