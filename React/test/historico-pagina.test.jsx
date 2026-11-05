// @vitest-environment jsdom
import React from 'react';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { act, cleanup, fireEvent, render, screen } from '@testing-library/react';
import '@testing-library/jest-dom/vitest';
const { requisitar } = vi.hoisted(() => ({ requisitar: vi.fn() }));
vi.mock('../src/core/http.js', () => ({ requisitar, extrairLista: (v) => Array.isArray(v) ? v : v?.content || [] }));
vi.mock('../src/modules/auth/context.jsx', () => ({ useAutenticacao: () => ({ user: { perfil: 'GESTOR_LOCAL', autoridade: 'PERFIL_GESTOR_LOCAL' } }) }));
import { PaginaHistorico } from '../src/modules/historico/PaginaHistorico.jsx';

beforeEach(() => { requisitar.mockReset(); requisitar.mockResolvedValue([]); });
afterEach(() => cleanup());
const consultaNome = () => {
  fireEvent.click(screen.getByRole('tab', { name: 'Nome' }));
  fireEvent.change(screen.getByLabelText('ID da pessoa'), { target: { value: '4' } });
  fireEvent.change(screen.getByLabelText('ID do vínculo'), { target: { value: '5' } });
  fireEvent.change(screen.getByLabelText('Data efetiva consultada'), { target: { value: '2026-01-01' } });
  fireEvent.change(screen.getByLabelText('Conhecido até (ISO com fuso)'), { target: { value: '2026-01-02T00:00:00Z' } });
  fireEvent.click(screen.getByRole('button', { name: 'Consultar histórico' }));
};

describe('Página contextual de histórico', () => {
  it('barra ID zero sem consultar o servidor', () => {
    render(<PaginaHistorico />);
    fireEvent.change(screen.getByLabelText('ID do contrato'), { target: { value: '0' } });
    fireEvent.click(screen.getByRole('button', { name: 'Consultar histórico' }));
    expect(screen.getByRole('alert')).toHaveTextContent(/ID.*intervalo/i);
    expect(requisitar).not.toHaveBeenCalled();
  });

  it('consulta a mesma Pessoa/Vínculo em dois cortes e mantém nome desconhecido', async () => {
    requisitar.mockImplementation(({ url }) => Promise.resolve(url.endsWith('/nome-historico') ? {
      nome: null, comprovado: false, tipoFonte: 'AUSENTE', fonteId: null, fonteRegistradaEm: null, inicioComprovado: null, fimComprovado: null,
    } : []));
    render(<PaginaHistorico />); consultaNome();
    expect(await screen.findByText('Nome desconhecido no período consultado')).toBeInTheDocument();
    expect(requisitar.mock.calls.find(([cfg]) => cfg.url.endsWith('/nome-historico'))[0].params).toEqual({ vinculoId: 5, data: '2026-01-01', conhecidoAte: '2026-01-02T00:00:00Z' });
    expect(screen.getByText(/Cadeia documental.*conhecimento atual/i)).toBeInTheDocument();
  });

  it('descarta consulta em andamento ao trocar o recorte e aborta as requisições antigas', async () => {
    let resolver;
    requisitar.mockImplementation(({ url }) => url.endsWith('/nome-historico') ? new Promise((resolve) => { resolver = resolve; }) : Promise.resolve([]));
    render(<PaginaHistorico />); consultaNome();
    const sinal = requisitar.mock.calls[0][0].signal;
    fireEvent.change(screen.getByLabelText('Data efetiva consultada'), { target: { value: '2026-02-01' } });
    await act(async () => resolver({ nome: 'Nome da consulta anterior', comprovado: true, tipoFonte: 'PROVA_NOME_HISTORICO' }));
    expect(sinal.aborted).toBe(true);
    expect(screen.queryByText('Nome da consulta anterior')).not.toBeInTheDocument();
  });

  it('não apresenta leitura negada como cadeia documental vazia', async () => {
    requisitar.mockImplementation(({ url }) => url.endsWith('/provas-nome')
      ? Promise.reject({ status: 403, message: 'Leitura da cadeia não autorizada.' })
      : Promise.resolve({ nome: null, comprovado: false, tipoFonte: 'AUSENTE' }));
    render(<PaginaHistorico />); consultaNome();
    expect(await screen.findByRole('alert')).toHaveTextContent('Leitura da cadeia não autorizada.');
    expect(screen.queryByText('Nenhum registro retornado para esta consulta.')).not.toBeInTheDocument();
  });
});
