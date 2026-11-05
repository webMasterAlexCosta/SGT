// Roteador, menu, página cadastral, serviço, Axios, CSRF e confirmação reais.
// O adapter controla somente o servidor; não comprova Java/MySQL/browser real.
import React from 'react';
import { AxiosError } from 'axios';
import { act, cleanup, fireEvent, render, screen, waitFor, within } from '@testing-library/react';
import '@testing-library/jest-dom/vitest';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { MemoryRouter, useLocation } from 'react-router-dom';

const { auth } = vi.hoisted(() => ({ auth: {
  user: null, geracaoSessao: 1, loading: false, loggingOut: false,
  encerrarSessao: () => {}, atualizarInformacoesSessao: () => Promise.resolve(), concluirSaida: () => {},
} }));
vi.mock('../src/modules/auth/context', () => ({ ProvedorAutenticacao: ({ children }) => children, useAutenticacao: () => auth }));
// As consultas de latência e o pulso periódico não fazem parte do cadastro.
vi.mock('../src/services/system/ServicoSistema.js', () => ({ ServicoSistema: {
  carregarRuntimeConfig: () => Promise.resolve({}), medirLatencia: () => Promise.resolve(0),
} }));
vi.mock('../src/hooks/usePulsoAtividade', () => ({ usePulsoAtividade: () => {} }));
vi.mock('../src/Leiaute/Cabecalho/index.jsx', () => ({ default: () => null }));
vi.mock('../src/core/ui/HomologacaoModal.jsx', () => ({ default: () => null }));
vi.mock('../src/core/ui/LembreteSegundoFator', () => ({ LembreteSegundoFator: () => null }));
vi.mock('../src/modules/AtualizacaoModulo/index.jsx', () => ({ default: () => null }));
vi.mock('../src/core/ModalConexao', () => ({ ModalConexao: () => null }));
vi.mock('../src/core/ui/Aviso', () => ({ ConteinerAvisos: () => null }));
// Destinos preservados têm suites próprias; mantemos aqui sua fronteira de rota.
vi.mock('../src/modules/painel/pages/index.jsx', () => ({ Painel: () => <section aria-label="Destino painel" /> }));
vi.mock('../src/modules/rh/pages/PaginaFrequencia/index.jsx', () => ({ PaginaFrequencia: () => <section aria-label="Destino frequência" /> }));
vi.mock('../src/modules/auth/pages/Login/index.jsx', () => ({ Login: () => <h1>Acesso à aplicação</h1> }));

import Aplicacao from '../src/Aplicacao.jsx';
import { api } from '../src/core/http.js';
import { configurarBuscadorTokenCsrf, resetarCoordenadorCsrfParaTestes } from '../src/core/http/coordenadorCsrf.js';
import { resetarSolicitadorConfirmacaoSenhaParaTestes } from '../src/core/http/coordenadorConfirmacaoSenha.js';
import { invalidarGeracaoSessao } from '../src/core/security/geracaoSessao.js';
import { EVENT_SHOW_TOAST, redefinirIndicadorRedirecionamento } from '../src/core/events.js';
import { diferida } from './fixtures/promessaDiferida.js';

let adapterOriginal;
let envios;
let empresas;
let responderEspecial;
let notificacoes;
let coletarNotificacao;

const resposta = (config, data) => ({
  config, data: { timestamp: '2026-11-02T10:00:00Z', status: 200, error: null,
    message: null, requestId: 'requisicao-sintetica', meta: null, data, path: `/api${config.url}` },
  status: 200, headers: {}, statusText: 'OK',
});
const recusar = (config, status, message) => {
  throw new AxiosError(message, 'ERR_BAD_REQUEST', config, null, {
    config, status, statusText: 'Recusa sintética', headers: {},
    data: { timestamp: '2026-11-02T10:00:00Z', status, error: 'CONFLICT', message,
      requestId: 'recusa-sintetica', meta: null, data: null, path: `/api${config.url}` },
  });
};
const removida = ({ url }) => /\/governanca-terceiros(?:\/|$)|\/(tabelas-tarifarias|folhas-horas|faturas)(?:\/|$)/.test(url);
const mutacoes = () => envios.filter(({ metodo }) => ['POST', 'PUT', 'PATCH', 'DELETE'].includes(metodo));
const usuario = (perfil = 'GESTOR_LOCAL') => ({
  id: 7, username: 'gestor-sintetico', perfil, autoridade: `PERFIL_${perfil}`, gestaoLocalId: 1,
});
const RotaAtual = () => <output aria-label="Rota atual">{useLocation().pathname}</output>;
const arvore = (rota = '/empresas-contratadas') => (
  <MemoryRouter initialEntries={[rota]}><RotaAtual /><Aplicacao /></MemoryRouter>
);

beforeEach(() => {
  envios = [];
  empresas = Array.from({ length: 6 }, (_, indice) => ({ id: 41 + indice,
    razaoSocial: `Empresa sintética ${indice + 1}`, cnpj: `1144477700016${indice}`,
    email: `empresa${indice + 1}@example.invalid`, ativo: true,
  }));
  responderEspecial = null;
  notificacoes = [];
  coletarNotificacao = evento => notificacoes.push(evento.detail);
  window.addEventListener(EVENT_SHOW_TOAST, coletarNotificacao);
  sessionStorage.clear();
  HTMLElement.prototype.scrollTo = () => {};
  auth.user = usuario(); auth.geracaoSessao += 1; auth.loading = false; auth.loggingOut = false;
  invalidarGeracaoSessao(); resetarCoordenadorCsrfParaTestes();
  resetarSolicitadorConfirmacaoSenhaParaTestes(); redefinirIndicadorRedirecionamento();
  adapterOriginal = api.defaults.adapter;
  api.defaults.adapter = async config => {
    const corpo = typeof config.data === 'string' ? JSON.parse(config.data) : config.data;
    const envio = { url: config.url, metodo: config.method.toUpperCase(), corpo,
      senhaEnviada: Boolean(config.headers?.get?.('X-User-Password')),
      csrfEnviado: Boolean(config.headers?.get?.('X-XSRF-TOKEN')),
      signal: config.signal,
    };
    envios.push(envio);
    if (responderEspecial) {
      const especial = responderEspecial(config, envio);
      if (especial !== undefined) return especial;
    }
    if (config.url === '/autenticacao/csrf') return resposta(config, {
      headerName: 'X-XSRF-TOKEN', parameterName: '_csrf', token: 'csrf-sintetico-cadastro',
    });
    if (envio.metodo === 'GET' && config.url === '/contratos') return resposta(config, [
      { id: 11, numeroContrato: 'Contrato sintético em rascunho', estadoNegocial: 'RASCUNHO', ativo: true },
      { id: 12, numeroContrato: 'Contrato sintético publicado', estadoNegocial: 'PUBLICADO', ativo: true },
    ]);
    if (envio.metodo === 'GET' && config.url === '/fornecedores') return resposta(config, empresas.map(item => ({ ...item })));
    if (envio.metodo === 'POST' && config.url === '/fornecedores') {
      const nova = { id: 701, razaoSocial: corpo.razaoSocial, cnpj: corpo.cnpj, email: corpo.email, ativo: true };
      empresas.push(nova); return resposta(config, nova);
    }
    if (envio.metodo === 'PUT' && /^\/fornecedores\/\d+$/.test(config.url)) {
      const id = Number(config.url.split('/').at(-1));
      empresas = empresas.map(item => item.id === id ? { ...item, ...corpo } : item);
      return resposta(config, empresas.find(item => item.id === id));
    }
    if (envio.metodo === 'PUT' && config.url === '/contratos/11/fornecedor/701') {
      return resposta(config, { contratoId: 11, fornecedor: empresas.find(item => item.id === 701) });
    }
    // Respostas antigas permitem observar o RED de comportamento sem falhar o adapter.
    if (envio.metodo === 'GET' && config.url === '/governanca-terceiros/resumo') {
      return resposta(config, { fornecedores: 6, processosAbertos: 0, riscosVencidos: 0 });
    }
    if (envio.metodo === 'GET' && config.url === '/governanca-terceiros/fornecedores') return resposta(config, empresas);
    if (envio.metodo === 'GET' && ['/status-empregados', '/lotacoes', '/cargos',
      '/governanca-terceiros/processos', '/governanca-terceiros/riscos'].includes(config.url)) return resposta(config, []);
    throw new Error(`Requisição inesperada: ${envio.metodo} ${envio.url}`);
  };
  configurarBuscadorTokenCsrf(async ({ geracaoSessao }) => api.request({
    method: 'GET', url: '/autenticacao/csrf', retries: 0, silentError: true,
    skipAuthLogout: true, __csrfBootstrap: true, __geracaoSessao: geracaoSessao,
  }));
});

afterEach(() => {
  cleanup(); api.defaults.adapter = adapterOriginal;
  window.removeEventListener(EVENT_SHOW_TOAST, coletarNotificacao);
  resetarCoordenadorCsrfParaTestes(); resetarSolicitadorConfirmacaoSenhaParaTestes();
  vi.restoreAllMocks();
});

async function abrirCadastro() {
  const view = render(arvore());
  await screen.findByRole('heading', { name: 'Empresas contratadas', exact: true });
  await screen.findByText('Empresa sintética 1');
  return view;
}
function preencherEmpresa() {
  fireEvent.change(screen.getByLabelText('Razão social da empresa'), { target: { value: 'Empresa contratada sintética nova' } });
  fireEvent.change(screen.getByLabelText('CNPJ da empresa'), { target: { value: '11222333000144' } });
  fireEvent.change(screen.getByLabelText('E-mail da empresa'), { target: { value: 'nova@example.invalid' } });
}
async function confirmar() {
  const dialogo = await screen.findByRole('dialog');
  fireEvent.change(within(dialogo).getByLabelText(/Senha de Acesso/), { target: { value: 'senha-sintetica' } });
  fireEvent.click(within(dialogo).getByRole('button', { name: 'Confirmar', exact: true }));
  return dialogo;
}

describe('Remoção da navegação e das APIs de governança de terceiros', () => {
  it.each(['/governanca-terceiros', '/governanca-terceiros?aba=riscos', '/governanca-terceiros/riscos'])(
    'a URL antiga %s não monta a página nem consulta o módulo removido', async rota => {
      render(arvore(rota));
      await waitFor(() => expect(screen.getByLabelText('Rota atual')).toHaveTextContent('/painel'));
      expect(screen.queryByRole('heading', { name: /Governança do ciclo de terceiros|Risco contínuo/ })).not.toBeInTheDocument();
      expect(screen.queryByRole('button', { name: 'Criar processo', exact: true })).not.toBeInTheDocument();
      expect(envios.filter(removida)).toEqual([]);
    },
  );

  it('mantém acesso ao cadastro de empresa e remove o item de governança do menu real', async () => {
    render(arvore('/painel'));
    const menu = await screen.findByRole('navigation', { name: 'Navegação principal' });
    expect(within(menu).queryByRole('button', { name: 'Governança do ciclo', exact: true })).not.toBeInTheDocument();
    const cadastros = within(menu).getByRole('button', { name: 'Cadastros e configurações', exact: true });
    expect(cadastros).toHaveAttribute('aria-expanded', 'false');
    fireEvent.click(cadastros);
    expect(cadastros).toHaveAttribute('aria-expanded', 'true');
    expect(within(menu).queryByRole('button', { name: 'Governança do ciclo', exact: true })).not.toBeInTheDocument();
    expect(within(menu).queryByText('Governança do ciclo', { exact: true })).not.toBeInTheDocument();
    fireEvent.click(within(menu).getByRole('button', { name: 'Empresas contratadas', exact: true }));
    await screen.findByRole('heading', { name: 'Empresas contratadas', exact: true });
    expect(screen.getByLabelText('Rota atual')).toHaveTextContent('/empresas-contratadas');
    expect(envios.filter(removida)).toEqual([]);
  });

  it.each(['ADMINISTRADOR', 'GESTOR_NACIONAL', 'GESTOR_ESTADUAL', 'PRESIDENTE', 'OPERACIONAL'])(
    'não amplia o acesso cadastral para %s', async perfil => {
      auth.user = usuario(perfil);
      render(arvore());
      await screen.findByRole('heading', { name: 'Acesso não autorizado' });
      expect(screen.queryByRole('button', { name: 'Empresas contratadas', exact: true })).not.toBeInTheDocument();
      expect(envios.filter(({ url }) => url === '/fornecedores' || removida({ url }))).toEqual([]);
    },
  );
});

describe('Empresa contratada preservada fora do módulo removido', () => {
  it('carrega apenas empresa e contratos, sem abas, colunas ou controles de risco e processo', async () => {
    await abrirCadastro();
    expect(envios.filter(({ metodo }) => metodo === 'GET').map(({ url }) => url).sort()).toEqual(['/contratos', '/fornecedores']);
    expect(screen.queryByRole('button', { name: /Risco contínuo|Registrar avaliação|Criar processo/ })).not.toBeInTheDocument();
    expect(screen.queryByRole('columnheader', { name: /Risco|Monitoramento|Próxima revisão/ })).not.toBeInTheDocument();
    expect(screen.queryByRole('spinbutton', { name: /Score/ })).not.toBeInTheDocument();
    expect(envios.filter(removida)).toEqual([]);
  });

  it('preserva paginação de cinco empresas e pesquisa sobre a sexta', async () => {
    await abrirCadastro();
    const tabela = screen.getByRole('table');
    expect(tabela.querySelectorAll('tbody tr')).toHaveLength(5);
    const primeiraPagina = tabela.textContent;
    fireEvent.click(screen.getByRole('button', { name: 'Próxima página de empresas' }));
    expect(tabela.querySelectorAll('tbody tr')).toHaveLength(1);
    expect(tabela.textContent).not.toBe(primeiraPagina);
    expect(within(tabela).getByText('Empresa sintética 6')).toBeInTheDocument();
    fireEvent.click(screen.getByRole('button', { name: 'Página anterior de empresas' }));
    expect(tabela.querySelectorAll('tbody tr')).toHaveLength(5);
    expect(tabela.textContent).toBe(primeiraPagina);
    fireEvent.change(screen.getByRole('searchbox', { name: 'Pesquisar empresas' }), { target: { value: 'SINTÉTICA 6' } });
    expect(within(tabela).getByText('Empresa sintética 6')).toBeInTheDocument();
    expect(tabela.querySelectorAll('tbody tr')).toHaveLength(1);
    expect(screen.getByRole('button', { name: 'Próxima página de empresas' })).toBeDisabled();
    fireEvent.change(screen.getByRole('searchbox', { name: 'Pesquisar empresas' }), { target: { value: 'sem-correspondencia' } });
    expect(screen.getByText('Nenhum registro encontrado para esta pesquisa.')).toBeInTheDocument();
  });

  it('cadastra e associa ao rascunho somente depois da senha, usando as rotas canônicas e CSRF', async () => {
    await abrirCadastro(); preencherEmpresa();
    fireEvent.change(screen.getByLabelText('Contrato a vincular à empresa'), { target: { value: '11' } });
    fireEvent.click(screen.getByRole('button', { name: 'Cadastrar e vincular', exact: true }));
    await screen.findByRole('dialog');
    expect(mutacoes()).toHaveLength(0);
    await confirmar();
    await waitFor(() => expect(mutacoes()).toHaveLength(2));
    expect(mutacoes().map(({ url, metodo }) => [metodo, url])).toEqual([
      ['POST', '/fornecedores'], ['PUT', '/contratos/11/fornecedor/701'],
    ]);
    expect(mutacoes()[0]).toMatchObject({ senhaEnviada: true, csrfEnviado: true,
      corpo: { razaoSocial: 'Empresa contratada sintética nova', cnpj: '11222333000144', email: 'nova@example.invalid' } });
    expect(mutacoes()[1]).toMatchObject({ senhaEnviada: true, csrfEnviado: true });
    expect(mutacoes()[1].corpo).toBeUndefined();
    expect(envios.filter(removida)).toEqual([]);
    await waitFor(() => expect(screen.getByLabelText('Razão social da empresa')).toHaveValue(''));
  });

  it('permite cadastrar sem vínculo imediato e não envia associação fictícia', async () => {
    await abrirCadastro(); preencherEmpresa();
    fireEvent.click(screen.getByRole('button', { name: 'Cadastrar e vincular', exact: true }));
    await confirmar();
    await waitFor(() => expect(screen.getByLabelText('Razão social da empresa')).toHaveValue(''));
    expect(mutacoes().map(({ metodo, url }) => [metodo, url])).toEqual([['POST', '/fornecedores']]);
  });

  it('oferece somente contratos em rascunho para a associação cadastral', async () => {
    await abrirCadastro();
    const selecao = screen.getByLabelText('Contrato a vincular à empresa');
    expect(within(selecao).getByRole('option', { name: 'Contrato sintético em rascunho' })).toBeInTheDocument();
    expect(within(selecao).queryByRole('option', { name: 'Contrato sintético publicado' })).not.toBeInTheDocument();
  });

  it('informa criação confirmada e vínculo recusado, fecha a confirmação e atualiza o catálogo', async () => {
    await abrirCadastro(); preencherEmpresa();
    responderEspecial = (config, envio) => envio.metodo === 'PUT' && envio.url === '/contratos/11/fornecedor/701'
      ? recusar(config, 409, 'Contrato publicado não aceita associação cadastral.') : undefined;
    fireEvent.change(screen.getByLabelText('Contrato a vincular à empresa'), { target: { value: '11' } });
    fireEvent.click(screen.getByRole('button', { name: 'Cadastrar e vincular', exact: true }));
    await confirmar();
    const parcial = await screen.findByRole('region', { name: 'Vínculo pendente da empresa cadastrada' });
    expect(within(parcial).getByRole('alert')).toHaveTextContent(/Empresa contratada sintética nova.*cadastrada/);
    expect(within(parcial).getByRole('alert')).toHaveTextContent('Contrato publicado não aceita associação cadastral.');
    await waitFor(() => expect(screen.queryByRole('dialog')).not.toBeInTheDocument());
    expect(screen.getByLabelText('Razão social da empresa')).toHaveValue('');
    expect(screen.getByRole('button', { name: 'Cadastrar e vincular', exact: true })).toBeDisabled();
    await waitFor(() => expect(envios.filter(({ metodo, url }) => metodo === 'GET' && url === '/fornecedores')).toHaveLength(2));
    fireEvent.change(screen.getByRole('searchbox', { name: 'Pesquisar empresas' }), { target: { value: 'contratada sintética nova' } });
    expect(within(screen.getByRole('table')).getByText('Empresa contratada sintética nova')).toBeInTheDocument();
    expect(empresas.filter(({ cnpj }) => cnpj === '11222333000144')).toHaveLength(1);
    expect(mutacoes().map(({ metodo, url }) => [metodo, url])).toEqual([
      ['POST', '/fornecedores'], ['PUT', '/contratos/11/fornecedor/701'],
    ]);
    expect(notificacoes.filter(({ type }) => type === 'success')).toHaveLength(0);
    expect(notificacoes.some(({ type, message }) => type === 'warning' && /cadastrada/.test(message))).toBe(true);
  });

  it('retoma somente a associação da empresa já criada sem repetir POST após o 409', async () => {
    await abrirCadastro(); preencherEmpresa();
    let publicadoAposCarregar = false;
    responderEspecial = (config, envio) => {
      if (envio.metodo === 'PUT' && envio.url === '/contratos/11/fornecedor/701') {
        publicadoAposCarregar = true;
        return recusar(config, 409, 'Contrato publicado não aceita associação cadastral.');
      }
      if (envio.metodo === 'GET' && envio.url === '/contratos' && publicadoAposCarregar) return resposta(config, [
        { id: 11, numeroContrato: 'Contrato sintético em rascunho', estadoNegocial: 'PUBLICADO', ativo: true },
        { id: 13, numeroContrato: 'Outro contrato sintético em rascunho', estadoNegocial: 'RASCUNHO', ativo: true },
      ]);
      if (envio.metodo === 'PUT' && envio.url === '/contratos/13/fornecedor/701') return resposta(config, {
        contratoId: 13, fornecedor: empresas.find(({ id }) => id === 701),
      });
      return undefined;
    };
    fireEvent.change(screen.getByLabelText('Contrato a vincular à empresa'), { target: { value: '11' } });
    fireEvent.click(screen.getByRole('button', { name: 'Cadastrar e vincular', exact: true }));
    await confirmar();
    await screen.findByRole('region', { name: 'Vínculo pendente da empresa cadastrada' });
    await waitFor(() => expect(screen.queryByRole('dialog')).not.toBeInTheDocument());
    const selecao = screen.getByLabelText('Contrato para vincular a empresa cadastrada');
    await waitFor(() => expect(within(selecao).getByRole('option', { name: 'Outro contrato sintético em rascunho' })).toBeInTheDocument());
    expect(selecao).toHaveValue('');
    expect(screen.getByRole('button', { name: 'Tentar vínculo novamente', exact: true })).toBeDisabled();
    fireEvent.change(selecao, { target: { value: '13' } });
    fireEvent.click(screen.getByRole('button', { name: 'Tentar vínculo novamente', exact: true }));
    expect(mutacoes()).toHaveLength(2);
    await confirmar();
    await waitFor(() => expect(screen.queryByRole('region', { name: 'Vínculo pendente da empresa cadastrada' })).not.toBeInTheDocument());
    expect(mutacoes().map(({ metodo, url }) => [metodo, url])).toEqual([
      ['POST', '/fornecedores'], ['PUT', '/contratos/11/fornecedor/701'], ['PUT', '/contratos/13/fornecedor/701'],
    ]);
    expect(mutacoes()[2]).toMatchObject({ senhaEnviada: true, csrfEnviado: true });
    expect(empresas.filter(({ cnpj }) => cnpj === '11222333000144')).toHaveLength(1);
    expect(screen.getByRole('button', { name: 'Cadastrar e vincular', exact: true })).toBeEnabled();
    expect(notificacoes.filter(({ type }) => type === 'success')).toEqual([
      { type: 'success', message: 'Empresa contratada vinculada ao contrato.' },
    ]);
  });

  it('continuar sem vínculo preserva a empresa criada e libera um formulário vazio', async () => {
    await abrirCadastro(); preencherEmpresa();
    responderEspecial = (config, envio) => envio.metodo === 'PUT' && envio.url === '/contratos/11/fornecedor/701'
      ? recusar(config, 409, 'Contrato publicado não aceita associação cadastral.') : undefined;
    fireEvent.change(screen.getByLabelText('Contrato a vincular à empresa'), { target: { value: '11' } });
    fireEvent.click(screen.getByRole('button', { name: 'Cadastrar e vincular', exact: true }));
    await confirmar();
    await screen.findByRole('region', { name: 'Vínculo pendente da empresa cadastrada' });
    await waitFor(() => expect(screen.queryByRole('dialog')).not.toBeInTheDocument());
    fireEvent.click(screen.getByRole('button', { name: 'Continuar sem vínculo', exact: true }));
    expect(screen.queryByRole('region', { name: 'Vínculo pendente da empresa cadastrada' })).not.toBeInTheDocument();
    expect(screen.getByLabelText('Razão social da empresa')).toHaveValue('');
    expect(screen.getByLabelText('CNPJ da empresa')).toHaveValue('');
    expect(screen.getByRole('button', { name: 'Cadastrar e vincular', exact: true })).toBeEnabled();
    expect(empresas.filter(({ cnpj }) => cnpj === '11222333000144')).toHaveLength(1);
    expect(mutacoes()).toHaveLength(2);
    expect(mutacoes().some(({ metodo }) => metodo === 'DELETE')).toBe(false);
  });

  it('preserva edição dos dados cadastrais e envia somente os campos cadastrais', async () => {
    await abrirCadastro();
    vi.spyOn(window, 'prompt').mockReturnValueOnce('Empresa sintética atualizada').mockReturnValueOnce('11222333000144').mockReturnValueOnce('atualizada@example.invalid');
    const linha = screen.getByText('Empresa sintética 1').closest('tr');
    fireEvent.click(within(linha).getByRole('button', { name: 'Editar', exact: true }));
    expect(mutacoes()).toHaveLength(0);
    await confirmar();
    await screen.findByText('Empresa sintética atualizada');
    expect(mutacoes()).toHaveLength(1);
    expect(mutacoes()[0]).toMatchObject({ metodo: 'PUT', url: '/fornecedores/41', senhaEnviada: true,
      corpo: { razaoSocial: 'Empresa sintética atualizada', cnpj: '11222333000144', email: 'atualizada@example.invalid', ativo: true } });
    expect(Object.keys(mutacoes()[0].corpo).sort()).toEqual(['ativo', 'cnpj', 'email', 'razaoSocial']);
  });

  it('inativa e reativa sem apagar empresa nem introduzir chamada de risco', async () => {
    await abrirCadastro();
    fireEvent.click(within(screen.getByText('Empresa sintética 1').closest('tr')).getByRole('button', { name: 'Inativar', exact: true }));
    await confirmar();
    await waitFor(() => expect(within(screen.getByText('Empresa sintética 1').closest('tr')).getByText('INATIVO')).toBeInTheDocument());
    await waitFor(() => expect(screen.queryByRole('dialog')).not.toBeInTheDocument());
    fireEvent.click(within(screen.getByText('Empresa sintética 1').closest('tr')).getByRole('button', { name: 'Reativar', exact: true }));
    await confirmar();
    await waitFor(() => expect(within(screen.getByText('Empresa sintética 1').closest('tr')).getByText('ATIVO')).toBeInTheDocument());
    expect(mutacoes().map(({ corpo }) => corpo.ativo)).toEqual([false, true]);
    expect(mutacoes().every(({ metodo, url }) => metodo === 'PUT' && url === '/fornecedores/41')).toBe(true);
    expect(envios.filter(removida)).toEqual([]);
  });

  it('cancelar a confirmação mantém os dados preenchidos e não envia a criação', async () => {
    await abrirCadastro(); preencherEmpresa();
    fireEvent.click(screen.getByRole('button', { name: 'Cadastrar e vincular', exact: true }));
    const dialogo = await screen.findByRole('dialog');
    fireEvent.click(within(dialogo).getByRole('button', { name: 'Cancelar', exact: true }));
    await waitFor(() => expect(screen.queryByRole('dialog')).not.toBeInTheDocument());
    expect(mutacoes()).toHaveLength(0);
    expect(screen.getByLabelText('Razão social da empresa')).toHaveValue('Empresa contratada sintética nova');
  });

  it('recusa cadastral 409 não simula sucesso nem apaga o formulário', async () => {
    await abrirCadastro(); preencherEmpresa();
    responderEspecial = (config, envio) => envio.metodo === 'POST' && envio.url === '/fornecedores'
      ? recusar(config, 409, 'Empresa sintética já cadastrada.') : undefined;
    fireEvent.click(screen.getByRole('button', { name: 'Cadastrar e vincular', exact: true }));
    await confirmar();
    await screen.findByText('Empresa sintética já cadastrada.');
    expect(screen.getByLabelText('Razão social da empresa')).toHaveValue('Empresa contratada sintética nova');
    expect(mutacoes()).toHaveLength(1);
    expect(notificacoes.filter(({ type }) => type === 'success')).toHaveLength(0);
  });

  it('uma recusa de consulta 403 permanece visível como erro', async () => {
    responderEspecial = (config, envio) => envio.metodo === 'GET' && envio.url === '/fornecedores'
      ? recusar(config, 403, 'Consulta de empresas fora do escopo sintético.') : undefined;
    render(arvore());
    await screen.findByRole('heading', { name: 'Empresas contratadas', exact: true });
    await waitFor(() => expect(notificacoes).toContainEqual({ type: 'error', message: 'Consulta de empresas fora do escopo sintético.' }));
    expect(mutacoes()).toHaveLength(0);
  });

  it('logout invalida a carga em andamento e uma resposta antiga não repopula o cadastro', async () => {
    const pendente = diferida();
    let configPendente;
    responderEspecial = (config, envio) => {
      if (envio.metodo !== 'GET' || envio.url !== '/fornecedores') return undefined;
      configPendente = config; return pendente.promise;
    };
    const view = render(arvore());
    await screen.findByRole('heading', { name: 'Empresas contratadas', exact: true });
    await waitFor(() => expect(configPendente).toBeDefined());
    await act(async () => {
      invalidarGeracaoSessao(); auth.user = null; auth.loggingOut = true; auth.geracaoSessao += 1;
      view.rerender(arvore());
    });
    await screen.findByRole('heading', { name: 'Acesso à aplicação' });
    expect(configPendente.signal.aborted).toBe(true);
    await act(async () => pendente.resolve(resposta(configPendente, [{ id: 999, razaoSocial: 'DADO ANTIGO NÃO EXIBÍVEL', cnpj: '11222333000144', email: null, ativo: true }])));
    expect(screen.queryByText('DADO ANTIGO NÃO EXIBÍVEL')).not.toBeInTheDocument();
    expect(screen.queryByRole('heading', { name: 'Empresas contratadas', exact: true })).not.toBeInTheDocument();
    expect(mutacoes()).toHaveLength(0);
  });
});
