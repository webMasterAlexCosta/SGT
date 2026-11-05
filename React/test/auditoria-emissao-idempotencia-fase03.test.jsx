// Auditoria independente: duas emissões concluídas são intenções diferentes;
// repetição após resultado incerto conserva a intenção anterior.
import React from 'react';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { cleanup, fireEvent, render, screen, waitFor } from '@testing-library/react';
import '@testing-library/jest-dom/vitest';
const { requisitar } = vi.hoisted(() => ({ requisitar: vi.fn() }));
vi.mock('../src/core/http.js', () => ({ requisitar, extrairLista: r => Array.isArray(r) ? r : [], extrairPagina: r => r }));
vi.mock('../src/modules/auth/context.jsx', () => ({ useAutenticacao: () => ({ user: { perfil: 'GESTOR_LOCAL', autoridade: 'PERFIL_GESTOR_LOCAL' } }) }));
vi.mock('../src/core/events.js', () => ({ notificar: vi.fn() }));
import { PaginaRelatoriosAdministrativos } from '../src/modules/rh/pages/PaginaRelatoriosAdministrativos/index.jsx';
const emissoes = () => requisitar.mock.calls.map(([pedido]) => pedido).filter(pedido => pedido.method === 'POST' && pedido.url === '/relatorios/administrativos/emissoes');
const paginaVazia = { content: [], number: 0, totalPages: 0, totalElements: 0, first: true, last: true };
let falharPrimeira;
beforeEach(() => {
  falharPrimeira = false;
  requisitar.mockReset();
  const confirmadas = new Map();
  requisitar.mockImplementation(async pedido => {
    if (pedido.url === '/relatorios/filtros') return { contratos: [], lotacoes: [], turnos: [] };
    if (pedido.method === 'POST') {
      if (falharPrimeira) { falharPrimeira = false; throw { status: 0, resultadoIncerto: true, message: 'Resposta não recebida.' }; }
      if (!confirmadas.has(pedido.data.chaveIdempotencia)) confirmadas.set(pedido.data.chaveIdempotencia, confirmadas.size + 1);
      const versao = confirmadas.get(pedido.data.chaveIdempotencia);
      return { metadados: { id: versao, versao, tipo: 'FREQUENCIA_CONSOLIDADA', formato: 'VISUALIZACAO', estado: 'VIGENTE' }, conteudo: { registros: [] } };
    }
    return paginaVazia;
  });
});
afterEach(cleanup);
const emitir = async () => {
  const botao = screen.getByRole('button', { name: 'Emitir versão formal' });
  fireEvent.click(botao);
  await waitFor(() => expect(botao).not.toBeDisabled());
};
describe('Emissões administrativas e semântica de idempotência Java', () => {
  it('cria uma nova chave para uma nova emissão deliberada depois da primeira concluída', async () => {
    render(<PaginaRelatoriosAdministrativos podeEmitir />);
    await emitir();
    await screen.findByRole('heading', { name: 'Emissão #1' });
    await emitir();
    expect(emissoes()).toHaveLength(2);
    expect(emissoes()[1].data.chaveIdempotencia).not.toBe(emissoes()[0].data.chaveIdempotencia);
    expect(screen.getByRole('heading', { name: 'Emissão #2' })).toBeInTheDocument();
  });
  it('preserva a chave no reenvio da mesma intenção depois de resposta incerta', async () => {
    falharPrimeira = true;
    vi.spyOn(console, 'error').mockImplementation(() => {});
    render(<PaginaRelatoriosAdministrativos podeEmitir />);
    await emitir();
    await emitir();
    expect(emissoes()).toHaveLength(2);
    expect(emissoes()[1].data.chaveIdempotencia).toBe(emissoes()[0].data.chaveIdempotencia);
    vi.restoreAllMocks();
  });
});
