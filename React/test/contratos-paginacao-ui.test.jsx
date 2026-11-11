// @vitest-environment jsdom
import React from 'react';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { cleanup, fireEvent, render, screen, within } from '@testing-library/react';
import '@testing-library/jest-dom/vitest';

const { requisitar } = vi.hoisted(() => ({ requisitar: vi.fn() }));
vi.mock('../src/core/http.ts', () => ({ requisitar, requisitarDownload: vi.fn(), extrairLista: (r) => Array.isArray(r) ? r : r?.content || [] }));
vi.mock('../src/modules/auth/context.tsx', () => ({ useAutenticacao: () => ({ user: { perfil: 'GESTOR_LOCAL', gestaoLocalId: 1 } }) }));
vi.mock('../src/core/security/useConfirmacaoSenha.tsx', () => ({ useConfirmacaoSenha: () => ({ confirmarComSenha: vi.fn(), ModalConfirmacao: () => null, fechar: vi.fn() }) }));
vi.mock('../src/core/events.ts', () => ({ notificar: vi.fn() }));
import { PainelExcecoesContrato } from '../src/modules/admin/pages/PaginaContratos/components/PainelExcecoesContrato/index.tsx';

const seis = (criar) => Array.from({ length: 6 }, (_, i) => criar(i + 1));
const documentos = seis((id) => ({ id, titulo: `Documento sintético ${id}`, estado: 'PUBLICADO', classificacaoAcesso: 'PESSOAL_RESTRITO', referenciaSei: `SEI-${id}`, versaoDocumento: 1 }));
const correcoes = seis((id) => ({ id, numeroAnterior: `ANTIGO-${id}`, anoAnterior: 2025, numeroNovo: `NOVO-${id}`, anoNovo: 2026, dataEfetiva: '2026-01-01' }));
const eventos = seis((id) => ({ id, tipoEvento: 'ANULACAO', situacaoFato: 'CANCELADO', dataEfetiva: `2026-01-${String(id).padStart(2, '0')}`, registradoEm: '2026-01-01T12:00:00Z' }));
const eventosDocumento = seis((id) => ({ id, tipoEvento: 'RETIFICADO', motivo: `Motivo documental ${id}`, registradoEm: '2026-01-01T12:00:00Z' }));
const permissoes = seis((id) => ({ id, usuarioBeneficiarioUsername: `Leitor sintético ${id}`, finalidade: `FINALIDADE-${id}`, ativo: true }));
const acessos = seis((id) => ({ id, usuarioUsername: `Acesso sintético ${id}`, tipoAcesso: 'CONSULTA', finalidade: `FINALIDADE-${id}`, acessadoEm: '2026-01-01T12:00:00Z' }));

beforeEach(() => {
  requisitar.mockReset();
  requisitar.mockImplementation(async ({ url }) => {
    if (url === '/contratos/9/documentos') return documentos;
    if (url === '/contratos/9/documentos/1') return documentos[0];
    if (url.endsWith('/documentos/1/eventos')) return eventosDocumento;
    if (url.endsWith('/documentos/1/permissoes')) return permissoes;
    if (url.endsWith('/documentos/1/acessos')) return acessos;
    if (url === '/contratos/9/correcoes-identificador' || url === '/contratos/historico-identificador') return correcoes;
    return [];
  });
});
afterEach(cleanup);

const montar = () => render(<PainelExcecoesContrato contrato={{ id: 9, estadoNegocial: 'PUBLICADO' }} eventos={eventos} podeGerenciar />);
const avancar = (nome) => fireEvent.click(within(screen.getByRole('navigation', { name: nome })).getByRole('button', { name: 'Próxima' }));

describe('Cinco registros por página nas listas do painel contratual', () => {
  it('pagina documentos e permite voltar ao primeiro registro', async () => {
    montar();
    await screen.findByText('Documento sintético 1');
    expect(screen.queryByText('Documento sintético 6')).not.toBeInTheDocument();
    expect(screen.getAllByText(/^Documento sintético \d$/)).toHaveLength(5);
    avancar('Páginas de documentos');
    expect(screen.getByText('Documento sintético 6')).toBeInTheDocument();
    expect(screen.queryByText('Documento sintético 1')).not.toBeInTheDocument();
    fireEvent.click(within(screen.getByRole('navigation', { name: 'Páginas de documentos' })).getByRole('button', { name: 'Anterior' }));
    expect(screen.getByText('Documento sintético 1')).toBeInTheDocument();
  });

  it.each([
    ['eventos documentais', 'Páginas dos eventos documentais', /Motivo documental 1/, /Motivo documental 6/],
    ['permissões', 'Páginas das permissões documentais', /Leitor sintético 1/, /Leitor sintético 6/],
    ['acessos', 'Páginas dos acessos documentais', /Acesso sintético 1/, /Acesso sintético 6/],
  ])('pagina %s sem esconder os registros depois do quinto', async (_nome, navegacao, primeiro, sexto) => {
    montar();
    const titulo = await screen.findByText('Documento sintético 1');
    fireEvent.click(within(titulo.closest('article')).getByRole('button', { name: 'Detalhes' }));
    await screen.findByText(primeiro);
    expect(screen.queryByText(sexto)).not.toBeInTheDocument();
    avancar(navegacao);
    expect(screen.getByText(sexto)).toBeInTheDocument();
    expect(screen.queryByText(primeiro)).not.toBeInTheDocument();
  });

  it('pagina correções do identificador', async () => {
    montar();
    fireEvent.click(screen.getByRole('tab', { name: 'Correções' }));
    await screen.findByText('NOVO-1/2026');
    expect(screen.queryByText('NOVO-6/2026')).not.toBeInTheDocument();
    avancar('Páginas das correções de identificador');
    expect(screen.getByText('NOVO-6/2026')).toBeInTheDocument();
    expect(screen.queryByText('NOVO-1/2026')).not.toBeInTheDocument();
  });

  it('pagina a pesquisa por identificador antigo', async () => {
    montar();
    fireEvent.click(screen.getByRole('tab', { name: 'Correções' }));
    fireEvent.change(screen.getByPlaceholderText('Número'), { target: { value: 'ANTIGO' } });
    fireEvent.change(screen.getByPlaceholderText('Ano'), { target: { value: '2025' } });
    fireEvent.click(screen.getByRole('button', { name: 'Pesquisar' }));
    await screen.findByText(/ANTIGO-1\/2025 → NOVO-1\/2026/);
    expect(screen.queryByText(/ANTIGO-6\/2025 → NOVO-6\/2026/)).not.toBeInTheDocument();
    avancar('Páginas da pesquisa de identificador');
    expect(screen.getByText(/ANTIGO-6\/2025 → NOVO-6\/2026/)).toBeInTheDocument();
    expect(screen.queryByText(/ANTIGO-1\/2025 → NOVO-1\/2026/)).not.toBeInTheDocument();
  });

  it('pagina atos de anulação sem mudar sua situação histórica', () => {
    montar();
    fireEvent.click(screen.getByRole('tab', { name: 'Correções' }));
    expect(screen.getAllByText(/^Anulação em /)).toHaveLength(5);
    expect(screen.queryByText('Anulação em 06/01/2026')).not.toBeInTheDocument();
    avancar('Páginas das anulações');
    expect(screen.getByText('Anulação em 06/01/2026')).toBeInTheDocument();
    expect(screen.getAllByText(/^Anulação em /)).toHaveLength(1);
    expect(screen.queryByRole('button', { name: 'Registrar CANCELA' })).not.toBeInTheDocument();
  });
});
