// Contraprovas de componente com transporte controlado; não são evidência E2E.
import React from 'react';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { act, cleanup, fireEvent, render, screen, waitFor } from '@testing-library/react';
import '@testing-library/jest-dom/vitest';

const { requisitar } = vi.hoisted(() => ({ requisitar: vi.fn() }));
vi.mock('../src/core/http.ts', () => ({
  requisitar, requisitarDownload: vi.fn(),
  extrairLista: resposta => Array.isArray(resposta) ? resposta : [],
}));
vi.mock('../src/modules/auth/context.tsx', () => ({ useAutenticacao: () => ({ user: { id: 7, perfil: 'GESTOR_LOCAL', gestaoLocalId: 1 } }) }));
vi.mock('../src/core/events.ts', () => ({ notificar: vi.fn() }));
import { PainelExcecoesContrato } from '../src/modules/admin/pages/PaginaContratos/components/PainelExcecoesContrato/index.tsx';

const adiar = () => {
  let resolver;
  const promessa = new Promise(resolve => { resolver = resolve; });
  return { promessa, resolver };
};
const casos = [
  {
    lista: 'documentos', aba: 'Documentos', sufixo: '/documentos',
    registro: marca => ({ id: marca === 'A' ? 101 : 102, titulo: `Documento ${marca} da lista`, estado: 'PUBLICADO', classificacaoAcesso: 'NEGOCIAL', versaoDocumento: 1 }),
    texto: marca => `Documento ${marca} da lista`,
  },
  {
    lista: 'transferências', aba: 'Transferências', sufixo: '/transferencias-gestao',
    registro: marca => ({ id: marca === 'A' ? 201 : 202, gestaoOrigemNome: `Origem ${marca}`, gestaoDestinoNome: `Destino ${marca}`, mapeamentos: [] }),
    texto: marca => `Origem ${marca} → Destino ${marca}`,
  },
  {
    lista: 'correções', aba: 'Correções', sufixo: '/correcoes-identificador',
    registro: marca => ({ id: marca === 'A' ? 301 : 302, numeroAnterior: `ANTERIOR-${marca}`, anoAnterior: 2025, numeroNovo: `NOVO-${marca}`, anoNovo: 2026 }),
    texto: marca => `NOVO-${marca}/2026`,
  },
];
const painel = id => <PainelExcecoesContrato contrato={{ id, estadoNegocial: 'PUBLICADO' }} />;
const abrirAba = nome => fireEvent.click(screen.getByRole('tab', { name: nome, exact: true }));

beforeEach(() => { requisitar.mockReset(); });
afterEach(cleanup);

describe('Listas de exceções só publicam dados da leitura vigente', () => {
  it.each(casos)('descarta $lista do contrato A que chegam depois do contrato B', async caso => {
    const antiga = adiar();
    requisitar.mockImplementation(pedido => {
      if (pedido.url === `/contratos/9${caso.sufixo}`) return antiga.promessa;
      if (pedido.url === `/contratos/10${caso.sufixo}`) return Promise.resolve([caso.registro('B')]);
      return Promise.resolve([]);
    });
    const tela = render(painel(9));
    await waitFor(() => expect(requisitar).toHaveBeenCalledWith(expect.objectContaining({ url: `/contratos/9${caso.sufixo}` })));
    tela.rerender(painel(10));
    abrirAba(caso.aba);
    await screen.findByText(caso.texto('B'), { exact: true });
    await act(async () => antiga.resolver([caso.registro('A')]));
    expect(screen.queryByText(caso.texto('A'), { exact: true })).not.toBeInTheDocument();
    expect(screen.getByText(caso.texto('B'), { exact: true })).toBeInTheDocument();
  });

  it.each(casos)('descarta $lista da finalidade A que chegam depois da finalidade B', async caso => {
    const antiga = adiar();
    let chamadas = 0;
    requisitar.mockImplementation(pedido => {
      if (pedido.url === `/contratos/9${caso.sufixo}`) {
        chamadas += 1;
        return chamadas === 1 ? antiga.promessa : Promise.resolve([caso.registro('B')]);
      }
      return Promise.resolve([]);
    });
    render(painel(9));
    await waitFor(() => expect(chamadas).toBe(1));
    fireEvent.change(screen.getByLabelText('Finalidade para PESSOAL_RESTRITO'), { target: { value: 'Finalidade B sintética' } });
    abrirAba(caso.aba);
    await screen.findByText(caso.texto('B'), { exact: true });
    await act(async () => antiga.resolver([caso.registro('A')]));
    expect(screen.queryByText(caso.texto('A'), { exact: true })).not.toBeInTheDocument();
    expect(screen.getByText(caso.texto('B'), { exact: true })).toBeInTheDocument();
    if (caso.lista === 'documentos') {
      expect(requisitar).toHaveBeenCalledWith(expect.objectContaining({ url: '/contratos/9/documentos', params: { finalidade: 'Finalidade B sintética' } }));
    }
  });
});
