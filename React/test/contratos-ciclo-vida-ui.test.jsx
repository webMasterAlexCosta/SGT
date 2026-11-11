// @vitest-environment jsdom
import React from 'react';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { act, cleanup, fireEvent, render, screen, waitFor } from '@testing-library/react';
import '@testing-library/jest-dom/vitest';

const { requisitar } = vi.hoisted(() => ({ requisitar: vi.fn() }));
vi.mock('../src/core/http.ts', () => ({ requisitar, extrairLista: (v) => v }));
import { ModalAcaoContrato } from '../src/modules/admin/pages/PaginaContratos/components/ModalAcaoContrato/index.tsx';

const previa = {
  contratoId: 9, operacao: 'inativar', dataEfetiva: '2026-11-01', dataAto: '2026-11-01', motivo: 'Inativação documentada',
  usuarioResponsavelId: 7, usuarioResponsavelUsername: 'gestor-sintetico', autorizacaoNecessaria: 'GESTOR_LOCAL_RESPONSAVEL',
  quantidadeVinculosAfetados: 1, vinculosAfetados: [{ vinculoId: 31, pessoaId: 32, pessoaNome: 'Pessoa sintética', matricula: 'TESTE-1', dataInicio: '2026-01-01', dataFim: null, programado: false }],
  programacoesFuturasAfetadas: [], quantidadeFrequenciasAfetadas: 1, datasComFrequenciasAfetadas: ['2026-11-01'],
  impactosConhecidos: ['A inativação bloqueia a operação e mantém a vigência e os vínculos.'],
  exigeConfirmacao: true, tokenPrevia: 'token-ui-1', limiteDetalhes: 200, vinculosTruncados: false,
  quantidadeProgramacoesFuturasAfetadas: 0, programacoesTruncadas: false,
  quantidadeDatasComFrequenciasAfetadas: 1, datasComFrequenciasTruncadas: false,
};

const montar = (props = {}) => {
  const aoConfirmar = vi.fn().mockResolvedValue(undefined);
  render(<ModalAcaoContrato aberto tipoAcao="INATIVAR" contrato={{ id: 9, numeroContrato: 'TESTE', estadoNegocial: 'PUBLICADO' }} processando={false} aoFechar={vi.fn()} aoConfirmar={aoConfirmar} {...props} />);
  fireEvent.change(screen.getByLabelText(/Motivo\/justificativa/), { target: { value: 'Inativação documentada' } });
  return props.aoConfirmar || aoConfirmar;
};

beforeEach(() => { requisitar.mockReset(); requisitar.mockResolvedValue(previa); });
afterEach(() => cleanup());

describe('Confirmação de ciclo de vida vinculada à prévia Java', () => {
  it('não deixa submeter a mutação sem gerar e confirmar uma prévia', async () => {
    const confirmar = montar();
    fireEvent.submit(screen.getByRole('dialog').querySelector('form'));
    expect(confirmar).not.toHaveBeenCalled();
  });

  it('mostra impacto e envia o token somente depois da confirmação explícita', async () => {
    const confirmar = montar();
    const botaoPrevia = screen.queryByRole('button', { name: /Gerar prévia/i });
    expect(botaoPrevia).toBeInTheDocument();
    fireEvent.click(botaoPrevia);
    expect(await screen.findByText(previa.impactosConhecidos[0])).toBeInTheDocument();
    fireEvent.click(screen.getByRole('checkbox', { name: /Confirmo.*impactos/i }));
    fireEvent.submit(screen.getByRole('dialog').querySelector('form'));
    await waitFor(() => expect(confirmar).toHaveBeenCalledTimes(1));
    expect(confirmar.mock.calls[0][0]).toEqual(expect.objectContaining({ tokenPrevia: 'token-ui-1', confirmarImpacto: true, motivo: 'Inativação documentada' }));
  });

  it('editar o motivo invalida a prévia e impede usar o token anterior', async () => {
    const confirmar = montar();
    const botaoPrevia = screen.queryByRole('button', { name: /Gerar prévia/i });
    expect(botaoPrevia).toBeInTheDocument();
    fireEvent.click(botaoPrevia);
    await screen.findByText(previa.impactosConhecidos[0]);
    fireEvent.click(screen.getByRole('checkbox', { name: /Confirmo.*impactos/i }));
    fireEvent.change(screen.getByLabelText(/Motivo\/justificativa/), { target: { value: 'Outro motivo documentado' } });
    fireEvent.submit(screen.getByRole('dialog').querySelector('form'));
    expect(confirmar).not.toHaveBeenCalled();
    expect(screen.queryByRole('checkbox', { name: /Confirmo.*impactos/i })).not.toBeInTheDocument();
  });

  it('descarta resposta de prévia que chega depois de editar a intenção', async () => {
    let resolver;
    requisitar.mockImplementation(() => new Promise((resolve) => { resolver = resolve; }));
    const confirmar = montar();
    const botaoPrevia = screen.queryByRole('button', { name: /Gerar prévia/i });
    expect(botaoPrevia).toBeInTheDocument();
    fireEvent.click(botaoPrevia);
    fireEvent.change(screen.getByLabelText(/Motivo\/justificativa/), { target: { value: 'Intenção posterior' } });
    await act(async () => { resolver(previa); });
    fireEvent.submit(screen.getByRole('dialog').querySelector('form'));
    expect(confirmar).not.toHaveBeenCalled();
    expect(screen.queryByRole('checkbox', { name: /Confirmo.*impactos/i })).not.toBeInTheDocument();
  });

  it('bloqueia duplo envio enquanto a confirmação de senha está pendente', async () => {
    let resolverConfirmacao;
    const confirmar = vi.fn(() => new Promise((resolve) => { resolverConfirmacao = resolve; }));
    montar({ aoConfirmar: confirmar });
    const botaoPrevia = screen.queryByRole('button', { name: /Gerar prévia/i });
    expect(botaoPrevia).toBeInTheDocument();
    fireEvent.click(botaoPrevia);
    await screen.findByText(previa.impactosConhecidos[0]);
    fireEvent.click(screen.getByRole('checkbox', { name: /Confirmo.*impactos/i }));
    const form = screen.getByRole('dialog').querySelector('form');
    fireEvent.submit(form);
    fireEvent.submit(form);
    expect(confirmar).toHaveBeenCalledTimes(1);
    await act(async () => { resolverConfirmacao(false); });
    expect(screen.getByLabelText(/Motivo\/justificativa/)).toHaveValue('Inativação documentada');
    expect(screen.queryByRole('checkbox', { name: /Confirmo.*impactos/i })).not.toBeInTheDocument();
  });

  it('um conflito comunicado pelo fluxo de senha invalida o token e conserva a intenção', async () => {
    const confirmar = vi.fn(async (_dados, aoErro) => { aoErro({ status: 409, message: 'Os impactos mudaram.' }); return false; });
    montar({ aoConfirmar: confirmar });
    const botaoPrevia = screen.queryByRole('button', { name: /Gerar prévia/i });
    expect(botaoPrevia).toBeInTheDocument();
    fireEvent.click(botaoPrevia);
    await screen.findByText(previa.impactosConhecidos[0]);
    fireEvent.click(screen.getByRole('checkbox', { name: /Confirmo.*impactos/i }));
    fireEvent.submit(screen.getByRole('dialog').querySelector('form'));
    await waitFor(() => expect(confirmar).toHaveBeenCalledTimes(1));
    expect(screen.getByLabelText(/Motivo\/justificativa/)).toHaveValue('Inativação documentada');
    expect(screen.queryByRole('checkbox', { name: /Confirmo.*impactos/i })).not.toBeInTheDocument();
    expect(screen.getByRole('alert')).toHaveTextContent('Os impactos mudaram.');
  });
});
