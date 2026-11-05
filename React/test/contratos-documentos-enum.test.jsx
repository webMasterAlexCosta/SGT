// @vitest-environment jsdom
import React from 'react';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { cleanup, fireEvent, render, screen, waitFor, within } from '@testing-library/react';
import '@testing-library/jest-dom/vitest';

const { requisitar } = vi.hoisted(() => ({ requisitar: vi.fn() }));
vi.mock('../src/core/http.js', () => ({ requisitar, requisitarDownload: vi.fn(), extrairLista: (r) => Array.isArray(r) ? r : r?.content || [] }));
vi.mock('../src/modules/auth/context.jsx', () => ({ useAutenticacao: () => ({ user: { perfil: 'GESTOR_LOCAL', gestaoLocalId: 1 } }) }));
vi.mock('../src/core/events.js', () => ({ notificar: vi.fn() }));
import { CLASSIFICACOES_DOCUMENTO, TIPOS_FATO_DOCUMENTAL } from '../src/services/admin/ServicoDominioExcecoesContrato.js';
import { PainelExcecoesContrato } from '../src/modules/admin/pages/PaginaContratos/components/PainelExcecoesContrato/index.jsx';

// Oráculo literal: backend congelado, dominio/documento/TipoFatoDocumental.java.
const TIPOS_JAVA = [
  'CONTRATO_ORIGINAL', 'ADITAMENTO', 'REMANEJAMENTO', 'TRANSFERENCIA_GESTAO',
  'CORRECAO_IDENTIFICADOR', 'CORRECAO_CNPJ', 'ENCERRAMENTO_ANTECIPADO', 'ANULACAO',
  'VINCULO', 'ALOCACAO', 'OUTRO',
];
beforeEach(() => { requisitar.mockReset(); requisitar.mockResolvedValue([]); });
afterEach(cleanup);
const montar = () => render(<PainelExcecoesContrato contrato={{ id: 9, estadoNegocial: 'PUBLICADO' }} eventos={[]} podeGerenciar />);
const lerMetadados = (formData) => new Promise((resolve, reject) => {
  const leitor = new FileReader();
  leitor.onload = () => resolve(JSON.parse(leitor.result));
  leitor.onerror = reject;
  leitor.readAsText(formData.get('metadados'));
});

describe('Fatos documentais conforme os enums Java completos', () => {
  it('mantém todos os onze tipos e as duas classificações de acesso do contrato congelado', () => {
    expect(TIPOS_FATO_DOCUMENTAL).toEqual(TIPOS_JAVA);
    expect(CLASSIFICACOES_DOCUMENTO).toEqual(['NEGOCIAL', 'PESSOAL_RESTRITO']);
  });

  it('oferece as onze opções na tela, preservando nomes e ordem das opções anteriores', () => {
    montar();
    const seletor = screen.getByLabelText(/Tipo de fato/);
    expect(within(seletor).getAllByRole('option').map((opcao) => opcao.value)).toEqual(TIPOS_JAVA);
  });

  it('associa o documento à correção CNPJ selecionada no multipart, pelo fluxo real de senha', async () => {
    montar();
    const formulario = screen.getByRole('button', { name: 'Registrar com confirmação' }).closest('form');
    const seletor = within(formulario).getByLabelText(/Tipo de fato/);
    expect(within(seletor).queryByRole('option', { name: 'CORRECAO_CNPJ' })).toBeInTheDocument();
    fireEvent.change(seletor, { target: { value: 'CORRECAO_CNPJ' } });
    fireEvent.change(within(formulario).getByLabelText(/ID do fato/), { target: { value: '77' } });
    fireEvent.change(within(formulario).getByLabelText(/Título/), { target: { value: 'Comprovação sintética da correção CNPJ' } });
    fireEvent.change(within(formulario).getByLabelText(/Referência SEI/), { target: { value: 'SEI-CNPJ-DOCUMENTO' } });
    fireEvent.submit(formulario);
    const dialogo = await screen.findByRole('dialog', { name: 'Registrar documento comprobatório' });
    fireEvent.change(within(dialogo).getByLabelText(/Senha de Acesso/), { target: { value: 'senha-sintetica' } });
    fireEvent.click(within(dialogo).getByRole('button', { name: 'Confirmar' }));
    await waitFor(() => expect(requisitar).toHaveBeenCalledWith(expect.objectContaining({ method: 'POST', url: '/contratos/9/documentos', senhaConfirmacao: 'senha-sintetica' })));
    const pedido = requisitar.mock.calls.map(([r]) => r).find((r) => r.method === 'POST' && r.url === '/contratos/9/documentos');
    expect(await lerMetadados(pedido.data)).toEqual(expect.objectContaining({
      tipoFato: 'CORRECAO_CNPJ', fatoId: 77, titulo: 'Comprovação sintética da correção CNPJ',
      referenciaSei: 'SEI-CNPJ-DOCUMENTO', classificacaoAcesso: 'NEGOCIAL', chaveIdempotencia: expect.any(String),
    }));
  });
});
