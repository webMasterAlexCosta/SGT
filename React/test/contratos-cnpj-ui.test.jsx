// @vitest-environment jsdom
import React from 'react';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { cleanup, fireEvent, render, screen, waitFor, within } from '@testing-library/react';
import '@testing-library/jest-dom/vitest';

const { requisitar } = vi.hoisted(() => ({ requisitar: vi.fn() }));
vi.mock('../src/core/http.js', () => ({ requisitar, requisitarDownload: vi.fn(), extrairLista: (r) => Array.isArray(r) ? r : r?.content || [] }));
vi.mock('../src/modules/auth/context.jsx', () => ({ useAutenticacao: () => ({ user: { perfil: 'GESTOR_LOCAL', gestaoLocalId: 1 } }) }));
vi.mock('../src/core/events.js', () => ({ notificar: vi.fn() }));
import { PainelExcecoesContrato } from '../src/modules/admin/pages/PaginaContratos/components/PainelExcecoesContrato/index.jsx';

const historico = Array.from({ length: 6 }, (_, i) => ({ id: i + 1, contratoId: 9, eventoId: 100 + i,
  cnpjAnterior: `1111111100000${i + 1}`, cnpjCorrigido: `2222222200000${i + 1}`,
  dataEfetiva: '2026-10-01', dataAto: '2026-10-02', motivo: `Correção sintética ${i + 1}`, referenciaSei: `SEI-CNPJ-${i + 1}`,
  chaveIdempotencia: `chave-historica-${i + 1}`, responsavelId: 7, responsavelUsername: 'gestor-sintetico', registradoEm: '2026-10-03T12:00:00Z',
}));
const responder = async (pedido) => pedido.url === '/contratos/9/correcoes-cnpj'
  ? pedido.method === 'GET' ? historico : { ...historico[0], ...pedido.data, id: 7 }
  : [];
beforeEach(() => { requisitar.mockReset(); requisitar.mockImplementation(responder); });
afterEach(cleanup);

const montar = (props = {}) => render(<PainelExcecoesContrato contrato={{ id: 9, estadoNegocial: 'PUBLICADO' }} eventos={[]} podeGerenciar {...props} />);
const abrir = async (props = {}) => {
  montar(props);
  const aba = screen.queryByRole('tab', { name: 'Correções CNPJ' });
  expect(aba).toBeInTheDocument();
  fireEvent.click(aba);
  await screen.findByRole('heading', { name: 'Correção material de CNPJ' });
};
const preencher = async (props = {}) => {
  await abrir(props);
  const form = screen.getByRole('form', { name: 'Correção material de CNPJ' });
  for (const [rotulo, valor] of [[/CNPJ corrigido/, '11.222.333/0001-44'], [/Data efetiva da correção/, '2026-10-01'], [/Data do ato da correção/, '2026-10-02'], [/Motivo da correção/, 'Correção documental sintética'], [/Referência SEI da correção/, 'SEI-CNPJ-SINTETICO']]) {
    fireEvent.change(within(form).getByLabelText(rotulo), { target: { value: valor } });
  }
  return form;
};
const confirmarSenha = async (form) => {
  fireEvent.submit(form);
  const dialogo = await screen.findByRole('dialog', { name: 'Confirmar correção material de CNPJ' });
  fireEvent.change(within(dialogo).getByLabelText(/Senha de Acesso/), { target: { value: 'senha-sintetica' } });
  fireEvent.click(within(dialogo).getByRole('button', { name: 'Confirmar' }));
};
const comandos = () => requisitar.mock.calls.map(([r]) => r).filter((r) => r.method === 'POST' && r.url.endsWith('/correcoes-cnpj'));

describe('Correção material de CNPJ pelo painel e pela confirmação de senha reais', () => {
  it('consulta os fatos retornados, diferencia aditamento e pagina cinco correções', async () => {
    await abrir({ podeGerenciar: false });
    expect(screen.getByText(/troca real de empresa.*ADITAMENTO/i)).toBeInTheDocument();
    await screen.findByRole('heading', { name: 'Correção #1' });
    expect(screen.queryByRole('heading', { name: 'Correção #6' })).not.toBeInTheDocument();
    expect(screen.getAllByRole('heading', { name: /^Correção #/ })).toHaveLength(5);
    fireEvent.click(within(screen.getByRole('navigation', { name: 'Páginas das correções de CNPJ' })).getByRole('button', { name: 'Próxima' }));
    expect(screen.getByRole('heading', { name: 'Correção #6' })).toBeInTheDocument();
    expect(screen.queryByRole('form', { name: 'Correção material de CNPJ' })).not.toBeInTheDocument();
  });

  it('envia somente o DTO suportado depois da senha, sem chamar rota de prévia', async () => {
    const atualizarContrato = vi.fn().mockResolvedValue({ id: 9, cnpjEmpresa: '33333333000033' });
    const form = await preencher({ aoAtualizarContrato: atualizarContrato });
    for (const [rotulo, valor] of [[/Evento precedente da correção/, '101'], [/Relação de precedência da correção/, 'RETIFICA'], [/Ordem documental da correção/, '2']]) {
      fireEvent.change(within(form).getByLabelText(rotulo), { target: { value: valor } });
    }
    await confirmarSenha(form);
    await waitFor(() => expect(comandos()).toHaveLength(1));
    expect(comandos()[0]).toEqual(expect.objectContaining({ senhaConfirmacao: 'senha-sintetica', data: {
      cnpjCorrigido: '11222333000144', dataEfetiva: '2026-10-01', dataAto: '2026-10-02', motivo: 'Correção documental sintética',
      referenciaSei: 'SEI-CNPJ-SINTETICO', chaveIdempotencia: expect.any(String), eventoPrecedenteId: 101, relacaoPrecedencia: 'RETIFICA', ordemDocumental: 2,
    } }));
    await waitFor(() => expect(within(form).getByLabelText(/CNPJ corrigido/)).toHaveValue(''));
    expect(requisitar.mock.calls.some(([r]) => r.url.includes('previa'))).toBe(false);
    expect(requisitar.mock.calls.filter(([r]) => r.method === 'GET' && r.url.endsWith('/correcoes-cnpj')).length).toBeGreaterThanOrEqual(2);
    await waitFor(() => expect(atualizarContrato).toHaveBeenCalledTimes(1));
  });

  it('distingue correção registrada de falha ao atualizar a visão atual do contrato', async () => {
    const atualizarContrato = vi.fn().mockRejectedValue({ status: 503, message: 'Consulta temporariamente indisponível.' });
    const form = await preencher({ aoAtualizarContrato: atualizarContrato });
    await confirmarSenha(form);
    expect(await screen.findByRole('alert')).toHaveTextContent(/correção.*registrada.*atualizar.*contrato/i);
    expect(within(form).getByLabelText(/CNPJ corrigido/)).toHaveValue('');
    expect(comandos()).toHaveLength(1);
  });

  it('recusa CNPJ incompleto sem pedir senha nem emitir POST', async () => {
    const form = await preencher();
    fireEvent.change(within(form).getByLabelText(/CNPJ corrigido/), { target: { value: '123' } });
    fireEvent.submit(form);
    expect(screen.getByRole('alert')).toHaveTextContent('14 dígitos');
    expect(screen.queryByRole('dialog')).not.toBeInTheDocument();
    expect(comandos()).toHaveLength(0);
  });

  it.each([409, 403, 0])('preserva formulário e chave após status %s; outra intenção recebe nova chave', async (status) => {
    requisitar.mockImplementation((pedido) => pedido.method === 'POST' && pedido.url.endsWith('/correcoes-cnpj')
      ? Promise.reject({ status, resultadoIncerto: status === 0, message: `Correção não confirmada (${status}).` }) : responder(pedido));
    const atualizarContrato = vi.fn();
    const form = await preencher({ aoAtualizarContrato: atualizarContrato });
    await confirmarSenha(form);
    await screen.findByRole('alert');
    await waitFor(() => expect(screen.queryByRole('dialog')).not.toBeInTheDocument());
    expect(within(form).getByLabelText(/CNPJ corrigido/)).toHaveValue('11.222.333/0001-44');
    expect(within(form).getByLabelText(/Motivo da correção/)).toHaveValue('Correção documental sintética');
    await confirmarSenha(form);
    await waitFor(() => expect(comandos()).toHaveLength(2));
    await waitFor(() => expect(screen.queryByRole('dialog')).not.toBeInTheDocument());
    expect(comandos()[1].data.chaveIdempotencia).toBe(comandos()[0].data.chaveIdempotencia);
    fireEvent.change(within(form).getByLabelText(/Motivo da correção/), { target: { value: 'Outra intenção documentada' } });
    await confirmarSenha(form);
    await waitFor(() => expect(comandos()).toHaveLength(3));
    expect(comandos()[2].data.chaveIdempotencia).not.toBe(comandos()[0].data.chaveIdempotencia);
    expect(atualizarContrato).not.toHaveBeenCalled();
  });

  it('bloqueia dupla submissão enquanto a senha está pendente e conserva o formulário ao cancelar', async () => {
    const form = await preencher();
    fireEvent.submit(form); fireEvent.submit(form);
    const dialogo = await screen.findByRole('dialog', { name: 'Confirmar correção material de CNPJ' });
    expect(screen.getAllByRole('dialog')).toHaveLength(1);
    expect(within(form).getByLabelText(/CNPJ corrigido/)).toBeDisabled();
    fireEvent.click(within(dialogo).getByRole('button', { name: 'Cancelar' }));
    await waitFor(() => expect(screen.queryByRole('dialog')).not.toBeInTheDocument());
    expect(within(form).getByLabelText(/CNPJ corrigido/)).toHaveValue('11.222.333/0001-44');
    expect(comandos()).toHaveLength(0);
  });

  it('permite corrigir somente a senha inválida mantendo a mesma intenção', async () => {
    let tentativa = 0;
    requisitar.mockImplementation((pedido) => pedido.method === 'POST' && pedido.url.endsWith('/correcoes-cnpj') && tentativa++ === 0
      ? Promise.reject({ status: 403, code: 'INVALID_CONFIRMATION_PASSWORD', senhaConfirmacaoInvalida: true, message: 'Senha de confirmação inválida.' }) : responder(pedido));
    const form = await preencher();
    await confirmarSenha(form);
    expect(await screen.findByRole('alert')).toHaveTextContent('Senha de confirmação inválida.');
    const dialogo = screen.getByRole('dialog', { name: 'Confirmar correção material de CNPJ' });
    fireEvent.change(within(dialogo).getByLabelText(/Senha de Acesso/), { target: { value: 'outra-senha-sintetica' } });
    fireEvent.click(within(dialogo).getByRole('button', { name: 'Confirmar' }));
    await waitFor(() => expect(comandos()).toHaveLength(2));
    expect(comandos()[1].data.chaveIdempotencia).toBe(comandos()[0].data.chaveIdempotencia);
  });

  it('uma leitura recusada não se apresenta como histórico vazio', async () => {
    requisitar.mockImplementation((pedido) => pedido.url.endsWith('/correcoes-cnpj')
      ? Promise.reject({ status: 403, message: 'Histórico de CNPJ indisponível neste contexto.' }) : responder(pedido));
    await abrir();
    expect(await screen.findByRole('alert')).toHaveTextContent('Histórico de CNPJ indisponível neste contexto.');
    expect(screen.queryByText(/Nenhuma correção de CNPJ/)).not.toBeInTheDocument();
  });

  it('mantém histórico consultável sem formulário em contrato não publicado', async () => {
    await abrir({ contrato: { id: 9, estadoNegocial: 'RASCUNHO' } });
    await screen.findByRole('heading', { name: 'Correção #1' });
    expect(screen.queryByRole('form', { name: 'Correção material de CNPJ' })).not.toBeInTheDocument();
    expect(screen.getByText(/correção material exige contrato publicado/i)).toBeInTheDocument();
  });
});
