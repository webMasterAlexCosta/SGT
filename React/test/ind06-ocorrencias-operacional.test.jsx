import React from 'react';
import { act, cleanup, fireEvent, render, screen, waitFor } from '@testing-library/react';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import '@testing-library/jest-dom/vitest';

const { requisitar, user } = vi.hoisted(() => ({ requisitar: vi.fn(), user: { perfil: 'OPERACIONAL', vinculosGestao: [] } }));
vi.mock('../src/core/http.ts', async (original) => ({ ...(await original()), requisitar }));
vi.mock('../src/core/config.ts', async (original) => { const m = await original(); return { ...m, CONFIG: { ...m.CONFIG, SEARCH_DEBOUNCE_MS: 0 } }; });
vi.mock('../src/modules/auth/context.tsx', () => ({ useAutenticacao: () => ({ user }) }));
vi.mock('../src/core/security/useConfirmacaoSenha.tsx', () => ({ useConfirmacaoSenha: () => ({ confirmarComSenha: vi.fn(), ModalConfirmacao: () => null }) }));
import { PaginaOcorrencias } from '../src/modules/rh/pages/PaginaOcorrencias/index.tsx';

const escopos = [
  { id: 1, lotacao: { id: 7, label: 'Lotação Norte' }, jornada: { id: 8, label: 'Manhã' } },
  { id: 2, lotacao: { id: 9, label: 'Lotação Sul' }, jornada: { id: 10, label: 'Noite' } },
];
const pessoa = { id: 17, nome: 'Pessoa sintética', matricula: 'MAT-123', cpf: '***.111.***-**', lotacaoId: 7, turnoId: 8, cargoNome: 'Cargo sintético' };
const pagina = (content) => ({ content, totalPages: 1, totalElements: content.length, number: 0, size: 1000 });
const adiar = () => { let resolve; const promise = new Promise((r) => { resolve = r; }); return { promise, resolve }; };
beforeEach(() => {
  user.perfil = 'OPERACIONAL'; user.vinculosGestao = [escopos[0]]; requisitar.mockReset();
  requisitar.mockImplementation(async ({ url, params }) => {
    if (url === '/empregados') throw Object.assign(new Error('Busca geral proibida ao Operacional'), { status: 403 });
    if (url === '/empregados/filtrados-frequencia') return pagina([pessoa]);
    if (url === '/ocorrencias' && Number(params.empregadoId) === 17) return [{ id: 71, dataOcorrencia: '2026-11-01', motivo: 'ELOGIO', observacao: 'Ocorrência da pessoa 17', anexos: [] }];
    return [];
  });
  vi.spyOn(console, 'error').mockImplementation(() => {});
});
afterEach(() => { cleanup(); vi.restoreAllMocks(); });

const buscar = () => fireEvent.change(screen.getByPlaceholderText('Nome ou CPF...'), { target: { value: 'Pessoa' } });

describe('IND-06: Operacional consulta Ocorrências no próprio contexto', () => {
  it('seleciona a projeção autorizada e consulta o histórico pelo ID original sem oferecer escrita', async () => {
    render(<PaginaOcorrencias />); buscar();
    fireEvent.click(await screen.findByRole('button', { name: /Pessoa sintética/ }));
    expect(await screen.findByText('Ocorrência da pessoa 17')).toBeInTheDocument();
    expect(requisitar.mock.calls.some(([r]) => r.url === '/empregados')).toBe(false);
    expect(requisitar.mock.calls.find(([r]) => r.url === '/empregados/filtrados-frequencia')?.[0].params).toMatchObject({ lotacaoId: 7, turnoId: 8, searchTerm: 'Pessoa' });
    expect(screen.queryByRole('button', { name: /Registrar Ocorrência/ })).not.toBeInTheDocument();
  });

  it('apresenta recusa real de busca como erro, sem anunciar ausência de colaboradores', async () => {
    requisitar.mockRejectedValue(Object.assign(new Error('Escopo operacional revogado'), { status: 403 }));
    render(<PaginaOcorrencias />); buscar();
    expect(await screen.findByRole('alert')).toHaveTextContent('Escopo operacional revogado');
    expect(screen.queryByText(/Nenhum colaborador encontrado/)).not.toBeInTheDocument();
  });

  it('só anuncia ausência após uma resposta bem sucedida no escopo consultado', async () => {
    requisitar.mockResolvedValue(pagina([]));
    render(<PaginaOcorrencias />); buscar();
    expect(await screen.findByText(/Nenhum colaborador encontrado/)).toBeInTheDocument();
    expect(screen.queryByRole('alert')).not.toBeInTheDocument();
  });

  it('não inicia busca sem lotação/turno autorizado no perfil', async () => {
    user.vinculosGestao = [];
    render(<PaginaOcorrencias />);
    expect(screen.getByPlaceholderText('Nome ou CPF...')).toBeDisabled();
    expect(screen.getByText(/lotação e turno.*perfil/i)).toBeInTheDocument();
    expect(requisitar).not.toHaveBeenCalled();
  });

  it('descarta resposta antiga quando muda o escopo durante a busca', async () => {
    user.vinculosGestao = escopos;
    const antiga = adiar();
    requisitar.mockImplementation(async ({ url, params }) => {
      if (url === '/empregados/filtrados-frequencia') return Number(params.lotacaoId) === 7 ? antiga.promise : pagina([{ ...pessoa, id: 18, nome: 'Pessoa do Sul' }]);
      throw new Error('Endpoint indevido: ' + url);
    });
    render(<PaginaOcorrencias />);
    fireEvent.change(screen.getByLabelText('Lotação e turno'), { target: { value: '7:8' } }); buscar();
    await waitFor(() => expect(requisitar.mock.calls.some(([r]) => r.params?.lotacaoId === 7)).toBe(true));
    fireEvent.change(screen.getByLabelText('Lotação e turno'), { target: { value: '9:10' } });
    expect(await screen.findByRole('button', { name: /Pessoa do Sul/ })).toBeInTheDocument();
    await act(async () => antiga.resolve(pagina([{ ...pessoa, nome: 'Pessoa antiga do Norte' }])));
    expect(screen.queryByRole('button', { name: /Pessoa antiga do Norte/ })).not.toBeInTheDocument();
    expect(screen.getByRole('button', { name: /Pessoa do Sul/ })).toBeInTheDocument();
  });

  it('apresenta 403 do histórico sem convertê-lo em zero registros', async () => {
    requisitar.mockImplementation(async ({ url }) => {
      if (url === '/empregados/filtrados-frequencia') return pagina([pessoa]);
      throw Object.assign(new Error('Leitura do histórico recusada'), { status: 403 });
    });
    render(<PaginaOcorrencias />); buscar();
    fireEvent.click(await screen.findByRole('button', { name: /Pessoa sintética/ }));
    expect(await screen.findByRole('alert')).toHaveTextContent('Leitura do histórico recusada');
    expect(screen.queryByText('Nenhum registro encontrado.')).not.toBeInTheDocument();
  });
});
