import React from 'react';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { cleanup, fireEvent, render, screen, waitFor, within } from '@testing-library/react';
import '@testing-library/jest-dom/vitest';

const { requisitar } = vi.hoisted(() => ({ requisitar: vi.fn() }));
vi.mock('../src/core/http.ts', () => ({ requisitar, extrairLista: r => Array.isArray(r) ? r : [] }));
vi.mock('../src/modules/auth/context.tsx', () => ({ useAutenticacao: () => ({ user: { perfil: 'ADMINISTRADOR' } }) }));
vi.mock('../src/core/events.ts', () => ({ notificar: vi.fn() }));
import { TIPOS_PERMISSAO_EXCEPCIONAL } from '../src/services/admin/ServicoPermissaoExcepcional.ts';
import { PaginaPermissoesExcepcionais } from '../src/modules/admin/pages/PaginaPermissoesExcepcionais/index.tsx';

// Oráculo literal do Java congelado: dominio/acesso/TipoPermissaoExcepcional.java.
const TIPOS_JAVA = [
  'CORRECAO_IDENTIFICADOR_CONTRATO',
  'REDUCAO_CONTRATUAL_EXCEPCIONAL',
  'TRANSFERENCIA_GESTAO_LOCAL',
  'INCORPORACAO_PROVA_HISTORICA',
  'ACESSO_DADOS_PESSOAIS_RESTRITOS',
  'INTERVENCAO_NEGOCIAL_TECNICA',
];

beforeEach(() => {
  requisitar.mockReset();
  requisitar.mockImplementation(({ url }) => Promise.resolve(
    url === '/usuarios' ? [{ id: 5, username: 'Beneficiário sintético', perfil: 'GESTOR_LOCAL' }]
      : url === '/contratos' ? [{ id: 1, numeroContrato: 'CONTRATO-SINTETICO' }]
        : [],
  ));
});
afterEach(cleanup);

const abrir = async () => {
  render(<PaginaPermissoesExcepcionais />);
  await waitFor(() => expect(requisitar).toHaveBeenCalledWith(expect.objectContaining({ url: '/contratos' })));
  fireEvent.click(screen.getByRole('button', { name: 'Nova permissão', exact: true }));
  return within(await screen.findByRole('dialog', { name: 'Conceder permissão excepcional' }));
};

describe('Permissões excepcionais conforme os seis tipos do Java', () => {
  it('preserva todos os valores anteriores e incorpora prova histórica na ordem do enum Java', () => {
    expect(TIPOS_PERMISSAO_EXCEPCIONAL).toEqual(TIPOS_JAVA);
  });

  it('expõe todos os tipos reais nas opções da tela administrativa', async () => {
    const formulario = await abrir();
    expect(within(formulario.getByRole('combobox', { name: 'Tipo', exact: true })).getAllByRole('option').map(opcao => opcao.value)).toEqual(TIPOS_JAVA);
  });

  it('associa os rótulos visíveis aos quatro seletores e mantém nomes acessíveis exatos', async () => {
    const formulario = await abrir();
    for (const nome of ['Tipo', 'Beneficiário', 'Contrato (quando aplicável)', 'Gestão Local (quando aplicável)']) {
      const seletor = formulario.getByRole('combobox', { name: nome, exact: true });
      expect(seletor).toHaveAccessibleName(nome);
      expect(seletor.labels).toHaveLength(1);
      expect(seletor.labels[0].control).toBe(seletor);
    }
  });

  it('envia a concessão de prova histórica selecionada com a confirmação de senha real', async () => {
    const formulario = await abrir();
    const tipo = formulario.getByRole('combobox', { name: 'Tipo', exact: true });
    expect(within(tipo).queryByRole('option', { name: 'Incorporacao Prova Historica' })).toBeInTheDocument();
    fireEvent.change(tipo, { target: { value: 'INCORPORACAO_PROVA_HISTORICA' } });
    fireEvent.change(formulario.getByRole('combobox', { name: 'Beneficiário', exact: true }), { target: { value: '5' } });
    fireEvent.change(formulario.getByRole('combobox', { name: 'Contrato (quando aplicável)', exact: true }), { target: { value: '1' } });
    fireEvent.change(formulario.getByLabelText('Validade', { exact: true }), { target: { value: '2027-01-01T12:00' } });
    fireEvent.change(formulario.getByLabelText('Finalidade', { exact: true }), { target: { value: 'Finalidade sintética de prova' } });
    fireEvent.change(formulario.getByLabelText('Justificativa', { exact: true }), { target: { value: 'Justificativa sintética' } });
    fireEvent.change(formulario.getByLabelText('Autorização administrativa', { exact: true }), { target: { value: 'AUTORIZACAO-SINTETICA' } });
    fireEvent.click(formulario.getByRole('button', { name: 'Conceder', exact: true }));
    fireEvent.change(await screen.findByLabelText('Senha de Acesso', { exact: true }), { target: { value: 'senha-sintetica' } });
    fireEvent.click(screen.getByRole('button', { name: 'Confirmar', exact: true }));
    await waitFor(() => expect(requisitar).toHaveBeenCalledWith(expect.objectContaining({
      method: 'POST', url: '/permissoes-excepcionais', retries: 0, senhaConfirmacao: 'senha-sintetica',
      data: {
        tipo: 'INCORPORACAO_PROVA_HISTORICA', usuarioBeneficiarioId: 5, contratoId: 1, gestaoLocalId: null,
        finalidade: 'Finalidade sintética de prova', justificativa: 'Justificativa sintética',
        autorizacaoAdministrativa: 'AUTORIZACAO-SINTETICA', validoAte: new Date('2027-01-01T12:00').toISOString(),
      },
    })));
  });
});
