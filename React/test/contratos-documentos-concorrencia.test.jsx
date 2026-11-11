import React from 'react';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { act, cleanup, fireEvent, render, screen, within } from '@testing-library/react';
import '@testing-library/jest-dom/vitest';

const { requisitar } = vi.hoisted(() => ({ requisitar: vi.fn() }));
vi.mock('../src/core/http.ts', () => ({ requisitar, requisitarDownload: vi.fn(), extrairLista: r => Array.isArray(r) ? r : [] }));
vi.mock('../src/modules/auth/context.tsx', () => ({ useAutenticacao: () => ({ user: { perfil: 'ADMINISTRADOR' } }) }));
vi.mock('../src/core/events.ts', () => ({ notificar: vi.fn() }));
import { PainelExcecoesContrato } from '../src/modules/admin/pages/PaginaContratos/components/PainelExcecoesContrato/index.tsx';

const documentos = [
  { id: 1, titulo: 'Documento A', classificacaoAcesso: 'PESSOAL_RESTRITO', estado: 'PUBLICADO' },
  { id: 2, titulo: 'Documento B', classificacaoAcesso: 'PESSOAL_RESTRITO', estado: 'PUBLICADO' },
];
const adiar = () => {
  let resolver;
  const promessa = new Promise(resolve => { resolver = resolve; });
  return { promessa, resolver };
};
const respostaPadrao = ({ url }) => {
  if (/\/contratos\/\d+\/documentos$/.test(url)) return documentos;
  if (url.endsWith('/documentos/1')) return documentos[0];
  if (url.endsWith('/documentos/2')) return documentos[1];
  if (url.endsWith('/documentos/1/permissoes')) return [{ id: 11, usuarioBeneficiarioUsername: 'Leitor A', finalidade: 'Finalidade A', ativo: true }];
  if (url.endsWith('/documentos/2/permissoes')) return [{ id: 22, usuarioBeneficiarioUsername: 'Leitor B', finalidade: 'Finalidade B', ativo: true }];
  return [];
};
const montar = (id = 9) => render(<PainelExcecoesContrato contrato={{ id, estadoNegocial: 'PUBLICADO' }} />);
const selecionar = async (nome) => {
  const titulo = await screen.findByText(`Documento ${nome}`, { exact: true });
  fireEvent.click(within(titulo.closest('article')).getByRole('button', { name: 'Detalhes' }));
};
const abrir = async (nome) => {
  await selecionar(nome);
  await screen.findByRole('heading', { name: `Documento ${nome} · cadeia documental` });
};
beforeEach(() => {
  requisitar.mockReset();
  requisitar.mockImplementation(pedido => Promise.resolve(respostaPadrao(pedido)));
});
afterEach(cleanup);

describe('Detalhes documentais vinculados à seleção e ao contexto atuais', () => {
  it('limpa dados e o formulário de concessão de A enquanto os metadados de B ainda carregam', async () => {
    const pendenteB = adiar();
    requisitar.mockImplementation(pedido => pedido.url.endsWith('/documentos/2') ? pendenteB.promessa : Promise.resolve(respostaPadrao(pedido)));
    montar();
    await abrir('A');
    await screen.findByText(/Leitor A/);
    fireEvent.change(screen.getByLabelText('Finalidade *'), { target: { value: 'Concessão somente A' } });
    await selecionar('B');
    expect(screen.queryByText(/Leitor A/)).not.toBeInTheDocument();
    expect(screen.queryByRole('heading', { name: 'Documento A · cadeia documental' })).not.toBeInTheDocument();
    await act(async () => pendenteB.resolver(documentos[1]));
    await screen.findByText(/Leitor B/);
    expect(screen.getByLabelText('Finalidade *')).toHaveValue('');
  });

  it('ignora metadados de A que chegam após a seleção e a resposta de B', async () => {
    const pendenteA = adiar();
    requisitar.mockImplementation(pedido => pedido.url.endsWith('/documentos/1') ? pendenteA.promessa : Promise.resolve(respostaPadrao(pedido)));
    montar();
    await selecionar('A');
    await abrir('B');
    await screen.findByText(/Leitor B/);
    await act(async () => pendenteA.resolver(documentos[0]));
    expect(screen.getByRole('heading', { name: 'Documento B · cadeia documental' })).toBeInTheDocument();
    expect(screen.queryByRole('heading', { name: 'Documento A · cadeia documental' })).not.toBeInTheDocument();
    expect(screen.queryByText(/Leitor A/)).not.toBeInTheDocument();
  });

  it('limpa a seleção ao mudar finalidade e descarta permissões atrasadas do contexto anterior', async () => {
    const pendenteA = adiar();
    requisitar.mockImplementation(pedido => pedido.url.endsWith('/documentos/1/permissoes') ? pendenteA.promessa : Promise.resolve(respostaPadrao(pedido)));
    montar();
    await abrir('A');
    const pedidoPermissoes = requisitar.mock.calls.map(([pedido]) => pedido).find(pedido => pedido.url.endsWith('/documentos/1/permissoes'));
    fireEvent.change(screen.getByLabelText('Finalidade para PESSOAL_RESTRITO'), { target: { value: 'Nova finalidade documentada' } });
    expect(screen.queryByRole('heading', { name: 'Documento A · cadeia documental' })).not.toBeInTheDocument();
    expect(pedidoPermissoes.signal?.aborted).toBe(true);
    await act(async () => pendenteA.resolver([{ id: 11, usuarioBeneficiarioUsername: 'Leitor tardio anterior', finalidade: 'Anterior', ativo: true }]));
    expect(screen.queryByText(/Leitor tardio anterior/)).not.toBeInTheDocument();
  });

  it('limpa os detalhes e cancela leituras quando o contrato da tela muda', async () => {
    const pendenteA = adiar();
    requisitar.mockImplementation(pedido => pedido.url === '/contratos/9/documentos/1/permissoes' ? pendenteA.promessa : Promise.resolve(respostaPadrao(pedido)));
    const tela = montar();
    await abrir('A');
    const pedidoPermissoes = requisitar.mock.calls.map(([pedido]) => pedido).find(pedido => pedido.url.endsWith('/documentos/1/permissoes'));
    tela.rerender(<PainelExcecoesContrato contrato={{ id: 10, estadoNegocial: 'PUBLICADO' }} />);
    expect(screen.queryByRole('heading', { name: 'Documento A · cadeia documental' })).not.toBeInTheDocument();
    expect(pedidoPermissoes.signal?.aborted).toBe(true);
    await act(async () => pendenteA.resolver([{ id: 11, usuarioBeneficiarioUsername: 'Leitor de outro contrato', ativo: true }]));
    expect(screen.queryByText(/Leitor de outro contrato/)).not.toBeInTheDocument();
  });

  it('mostra falha 500 de acessos sem ocultar permissões carregadas com sucesso', async () => {
    requisitar.mockImplementation(pedido => pedido.url.endsWith('/documentos/1/acessos') ? Promise.reject({ status: 500, message: 'Falha sintética na leitura de acessos.' }) : Promise.resolve(respostaPadrao(pedido)));
    montar();
    await abrir('A');
    expect(await screen.findByRole('alert')).toHaveTextContent('Falha sintética na leitura de acessos.');
    expect(screen.getByText(/Leitor A/)).toBeInTheDocument();
    expect(screen.queryByText(/Nenhum acesso/)).not.toBeInTheDocument();
  });

  it('mostra a recusa 403 de permissões e conserva os acessos que o Java retornou', async () => {
    requisitar.mockImplementation(pedido => {
      if (pedido.url.endsWith('/documentos/1/permissoes')) return Promise.reject({ status: 403, message: 'Permissões recusadas no contexto atual.' });
      if (pedido.url.endsWith('/documentos/1/acessos')) return Promise.resolve([{ id: 4, usuarioUsername: 'Acesso autorizado A', tipoAcesso: 'CONSULTA', finalidade: 'Auditoria sintética' }]);
      return Promise.resolve(respostaPadrao(pedido));
    });
    montar();
    await abrir('A');
    expect(await screen.findByRole('alert')).toHaveTextContent('Permissões recusadas no contexto atual.');
    expect(screen.getByText(/Acesso autorizado A/)).toBeInTheDocument();
    expect(screen.queryByText(/Nenhuma permissão/)).not.toBeInTheDocument();
  });

  it('expõe falha de metadados e remove os detalhes da seleção anterior', async () => {
    requisitar.mockImplementation(pedido => pedido.url.endsWith('/documentos/2') ? Promise.reject({ status: 403, message: 'Documento B recusado.' }) : Promise.resolve(respostaPadrao(pedido)));
    montar();
    await abrir('A');
    await screen.findByText(/Leitor A/);
    await selecionar('B');
    expect(await screen.findByRole('alert')).toHaveTextContent('Documento B recusado.');
    expect(screen.queryByText(/Leitor A/)).not.toBeInTheDocument();
    expect(screen.queryByRole('heading', { name: 'Documento A · cadeia documental' })).not.toBeInTheDocument();
  });
});
