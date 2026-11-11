// PROR01/ROP-064: JSX, domínio, confirmação, serviços e Axios reais.
// Adapter controlado e ator sintético; não é validação do Java nem E2E.
import React from 'react';
import { AxiosError } from 'axios';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { act, cleanup, fireEvent, render, screen, waitFor, within } from '@testing-library/react';
import '@testing-library/jest-dom/vitest';
import { PainelAlteracoesContrato } from '../src/modules/admin/pages/PaginaContratos/components/PainelAlteracoesContrato/index.tsx';
import { api } from '../src/core/http.ts';
import { configurarBuscadorTokenCsrf, resetarCoordenadorCsrfParaTestes } from '../src/core/http/coordenadorCsrf.ts';
import { resetarSolicitadorConfirmacaoSenhaParaTestes } from '../src/core/http/coordenadorConfirmacaoSenha.ts';
import { invalidarGeracaoSessao } from '../src/core/security/geracaoSessao.ts';
import { EVENT_SHOW_TOAST, redefinirIndicadorRedirecionamento } from '../src/core/events.ts';
import { diferida } from './fixtures/promessaDiferida.js';
import { validarAditamentoLocal } from '../src/services/admin/ServicoDominioAlteracoesContrato.ts';

vi.mock('../src/modules/auth/context.tsx', () => ({ useAutenticacao: () => ({ user: {
  id: 9701, perfil: 'GESTOR_LOCAL', autoridade: 'PERFIL_GESTOR_LOCAL', gestaoLocalId: 1,
} }) }));
const contrato = { id: 9741, numeroContrato: 'PROR-SINTETICO', anoContrato: 2026, estadoNegocial: 'PUBLICADO',
  situacaoTemporal: 'VIGENTE', habilitacaoAdministrativa: 'ATIVO', dataInicio: '2026-01-01', dataFim: '2026-11-30' };
const comp = (id, nome) => ({ id, contratoId: 9741, lotacaoId: id, jornadaId: 9732, cargoId: 9733,
  lotacaoNome: nome, jornadaDescricao: 'Diurno sintético', cargoNome: 'Cargo sintético', efetivoPrevisto: 50,
  dataInicioVigencia: '2026-01-01', dataFimVigencia: null, permiteNovaAlocacao: true, version: 0 });
const resposta = (config, data) => ({ config, data, status: 200, statusText: 'OK', headers: {} });
const recusar = (config, status, message) => Promise.reject(new AxiosError(message, 'ERR_BAD_REQUEST', config, null, {
  config, status, statusText: 'Recusa sintética', headers: {}, data: { status, message, code: status === 409 ? 'CONFLICT' : 'VALIDATION_ERROR' },
}));
let originalAdapter, pedidos, especial, avisos, coletar, numeroPrevia;
beforeEach(() => {
  localStorage.clear(); sessionStorage.clear();
  document.cookie = 'sgt_revisao_compartilhada_sessao=; Max-Age=0; Path=/';
  invalidarGeracaoSessao(); redefinirIndicadorRedirecionamento();
  resetarCoordenadorCsrfParaTestes(); resetarSolicitadorConfirmacaoSenhaParaTestes();
  originalAdapter = api.defaults.adapter; pedidos = []; especial = null; avisos = []; numeroPrevia = 0;
  coletar = e => avisos.push(e.detail); window.addEventListener(EVENT_SHOW_TOAST, coletar);
  api.defaults.adapter = async config => {
    const method = config.method.toUpperCase(); const data = typeof config.data === 'string' ? JSON.parse(config.data) : config.data;
    pedidos.push({ method, url: config.url, data, params: config.params });
    const custom = especial?.(config, data); if (custom !== undefined) return custom;
    if (config.url === '/autenticacao/csrf') return resposta(config, { headerName: 'X-XSRF-TOKEN', token: 'csrf-unitario' });
    if (method === 'GET') {
      if (config.url === '/contratos/9741/composicoes') return resposta(config, [comp(9731, 'Lotação inicial sintética'), comp(9734, 'Lotação destino sintética')]);
      if (config.url === '/lotacoes') return resposta(config, [{ id: 9731, nome: 'Lotação inicial sintética' }]);
      if (config.url === '/jornadas') return resposta(config, [{ id: 9732, descricao: 'Diurno sintético' }]);
      if (config.url === '/cargos') return resposta(config, [{ id: 9733, nome: 'Cargo sintético' }]);
      if (/^\/contratos\/9741\/(composicoes\/excedentes|aditamentos|remanejamentos)$/.test(config.url)) return resposta(config, []);
    }
    if (config.url.endsWith('/previa')) return resposta(config, {
      contratoId: 9741, dataEfetiva: data.dataEfetiva, efetivoAnterior: 100, efetivoResultante: data.novoEfetivoTotal ?? 100,
      efetivoOriginalPublicado: 100, pisoNormalReducao: 75, reducao: Boolean(data.novoEfetivoTotal < 100),
      excedeLimiteNormal25: false, exigeAutorizacaoExcepcional: false, possuiImpactoOcupacao: false,
      impactosComposicao: [], alertas: [], totalPreservado: true, somaVariacoes: 0,
      efetivoTotalAntes: 100, efetivoTotalDepois: 100, tokenPrevia: `previa-unitaria-${++numeroPrevia}`,
    });
    if (method === 'POST' && /^\/contratos\/9741\/(aditamentos|remanejamentos)$/.test(config.url)) return resposta(config, { id: 9751, ...data, versaoInstrumento: 1, version: 0 });
    throw new Error(`Transporte inesperado: ${method} ${config.url}`);
  };
  configurarBuscadorTokenCsrf(async ({ geracaoSessao }) => api.request({ method: 'GET', url: '/autenticacao/csrf', retries: 0,
    silentError: true, skipAuthLogout: true, __csrfBootstrap: true, __geracaoSessao: geracaoSessao }));
});
afterEach(() => {
  cleanup(); api.defaults.adapter = originalAdapter; vi.restoreAllMocks();
  window.removeEventListener(EVENT_SHOW_TOAST, coletar);
  resetarCoordenadorCsrfParaTestes(); resetarSolicitadorConfirmacaoSenhaParaTestes();
});
const enviarCampo = (rotulo, valor) => fireEvent.change(screen.getByLabelText(rotulo), { target: { value: valor } });
const abrir = async (props = {}) => {
  const view = render(<PainelAlteracoesContrato contrato={contrato} podeGerenciar {...props}/>);
  await waitFor(() => expect(screen.queryByText('Atualizando posição contratual...')).not.toBeInTheDocument());
  fireEvent.click(screen.getByRole('tab', { name: 'Aditamentos', exact: true })); return view;
};
const preencherBase = () => {
  enviarCampo(/Identificação/, 'ADI-PROR-SINTETICO'); enviarCampo(/Data efetiva/, '2026-11-15');
  enviarCampo('Data do documento', '2026-11-03'); enviarCampo('Data da assinatura', '2026-11-04');
  enviarCampo('Referência SEI', 'SEI-SINTETICO-PROR'); enviarCampo(/Justificativa/, 'Prorrogação sintética para regressão.');
};
const calcular = async () => {
  fireEvent.click(screen.getByRole('button', { name: 'Calcular prévia', exact: true }));
  await waitFor(() => expect(screen.getByRole('button', { name: 'Confirmar com esta prévia' })).toBeEnabled());
};
const abrirConfirmacao = async () => {
  fireEvent.click(screen.getByRole('button', { name: 'Confirmar com esta prévia' }));
  return screen.findByRole('dialog', { name: /Confirmar (?:retificação do )?(aditamento|remanejamento)/ });
};
const confirmar = modal => {
  fireEvent.change(within(modal).getByLabelText('Senha de Acesso'), { target: { value: 'senha-sintetica-unitaria' } });
  fireEvent.click(within(modal).getByRole('button', { name: 'Confirmar', exact: true }));
};
const posts = fim => pedidos.filter(p => p.method === 'POST' && p.url === `/contratos/9741/${fim}`);

describe('PROR01: ato acessível e contratos de prévia', () => {
  it('validação local recusa vigência final que não contém a data efetiva', () => {
    expect(validarAditamentoLocal({ identificacao: 'ADI-SINTETICO', dataEfetiva: '2026-11-15',
      justificativa: 'Regressão sintética.', novaDataFim: '2026-11-14' })).toEqual(expect.stringMatching(/data final|vigência/i));
  });
  it('exibe campo de nova data final somente no aditamento de contrato publicado gerenciável', async () => {
    const view = await abrir();
    const campo = screen.getByLabelText('Nova data final da vigência');
    expect(campo).toHaveAttribute('type', 'date'); expect(campo).toBeEnabled();
    view.rerender(<PainelAlteracoesContrato contrato={contrato} podeGerenciar={false}/>);
    expect(screen.queryByLabelText('Nova data final da vigência')).toBeNull();
    view.rerender(<PainelAlteracoesContrato contrato={{ ...contrato, estadoNegocial: 'RASCUNHO' }} podeGerenciar/>);
    expect(screen.queryByLabelText('Nova data final da vigência')).toBeNull();
  });
  it('preserva data efetiva, documento, assinatura e nova data final distintas na prévia e confirmação', async () => {
    await abrir(); preencherBase(); enviarCampo('Nova data final da vigência', '2027-11-30');
    await calcular(); const modal = await abrirConfirmacao(); confirmar(modal);
    await waitFor(() => expect(posts('aditamentos')).toHaveLength(1));
    expect(posts('aditamentos/previa')[0].data).toMatchObject({ dataEfetiva: '2026-11-15', dataDocumento: '2026-11-03',
      dataAssinatura: '2026-11-04', novaDataFim: '2027-11-30', tokenPrevia: null });
    expect(posts('aditamentos')[0].data).toMatchObject({ dataEfetiva: '2026-11-15', novaDataFim: '2027-11-30', tokenPrevia: 'previa-unitaria-1' });
  });
  it('mudar a data final invalida a prévia até recalcular para os novos dados', async () => {
    await abrir(); preencherBase(); enviarCampo('Nova data final da vigência', '2027-11-30'); await calcular();
    enviarCampo('Nova data final da vigência', '2028-11-30');
    expect(screen.getByRole('button', { name: 'Confirmar com esta prévia' })).toBeDisabled();
    await calcular(); const modal = await abrirConfirmacao(); confirmar(modal);
    await waitFor(() => expect(posts('aditamentos')).toHaveLength(1));
    expect(posts('aditamentos')[0].data).toMatchObject({ novaDataFim: '2028-11-30', tokenPrevia: 'previa-unitaria-2' });
  });
  it('não envia token capturado se o formulário mudar com a confirmação já aberta', async () => {
    await abrir(); preencherBase(); enviarCampo('Novo efetivo total', '75'); await calcular();
    const modal = await abrirConfirmacao(); enviarCampo('Novo efetivo total', '80'); confirmar(modal);
    await act(async () => { await Promise.resolve(); });
    expect(posts('aditamentos')).toEqual([]);
    expect(screen.getByLabelText('Novo efetivo total')).toHaveValue(80);
    expect(avisos.filter(a => a.type === 'success')).toEqual([]);
    expect(avisos.some(a => /prévia/i.test(a.message))).toBe(true);
  });
  it.each([403, 409])('recusa %s mantém todos os campos e nunca anuncia registro confirmado', async status => {
    await abrir(); preencherBase(); enviarCampo('Nova data final da vigência', '2027-11-30'); await calcular();
    especial = config => config.method === 'post' && config.url === '/contratos/9741/aditamentos'
      ? recusar(config, status, `Recusa sintética ${status} do aditamento`) : undefined;
    const modal = await abrirConfirmacao(); confirmar(modal);
    await screen.findByText(`Recusa sintética ${status} do aditamento`);
    expect(screen.getByLabelText('Nova data final da vigência')).toHaveValue('2027-11-30');
    expect(screen.getByLabelText(/Data efetiva/)).toHaveValue('2026-11-15');
    expect(screen.getByLabelText(/Justificativa/)).toHaveValue('Prorrogação sintética para regressão.');
    expect(avisos.filter(a => a.type === 'success')).toEqual([]);
    if (status === 409) expect(screen.getByRole('button', { name: 'Confirmar com esta prévia' })).toBeDisabled();
  });
  it('ESC cancela confirmação, conserva intenção preenchida e não envia o ato', async () => {
    await abrir(); preencherBase(); enviarCampo('Nova data final da vigência', '2027-11-30'); await calcular();
    await abrirConfirmacao(); fireEvent.keyDown(document, { key: 'Escape' });
    await waitFor(() => expect(screen.queryByRole('dialog')).toBeNull());
    expect(posts('aditamentos')).toEqual([]); expect(screen.getByLabelText('Nova data final da vigência')).toHaveValue('2027-11-30');
    expect(avisos.filter(a => a.type === 'success')).toEqual([]);
  });
  it('redução quantitativa continua sem exigir ou fabricar nova data final', async () => {
    await abrir(); preencherBase(); enviarCampo('Novo efetivo total', '75'); await calcular();
    const modal = await abrirConfirmacao(); confirmar(modal);
    await waitFor(() => expect(posts('aditamentos')).toHaveLength(1));
    expect(posts('aditamentos')[0].data).toMatchObject({ novoEfetivoTotal: 75, novaDataFim: null, dataEfetiva: '2026-11-15' });
  });
  it('remanejamento conserva soma zero e não ganha campo de prorrogação', async () => {
    await abrir(); fireEvent.click(screen.getByRole('tab', { name: 'Remanejamentos', exact: true }));
    expect(screen.queryByLabelText('Nova data final da vigência')).toBeNull();
    enviarCampo(/Identificação/, 'REM-SINTETICO'); enviarCampo(/Data efetiva/, '2026-11-15');
    enviarCampo(/Justificativa/, 'Remanejamento sintético sem alterar vigência.');
    fireEvent.click(screen.getByRole('button', { name: 'Adicionar item' }));
    fireEvent.click(screen.getByRole('button', { name: 'Adicionar item' }));
    const selects = within(screen.getByRole('tabpanel')).getAllByRole('combobox');
    fireEvent.change(selects[0], { target: { value: '9731' } });
    fireEvent.change(selects[1], { target: { value: '9734' } });
    const variacoes = screen.getAllByLabelText('Variação');
    fireEvent.change(variacoes[0], { target: { value: '-5' } }); fireEvent.change(variacoes[1], { target: { value: '5' } });
    await calcular(); const modal = await abrirConfirmacao(); confirmar(modal);
    await waitFor(() => expect(posts('remanejamentos')).toHaveLength(1));
    expect(posts('remanejamentos')[0].data).toMatchObject({ dataEfetiva: '2026-11-15',
      itens: [{ composicaoId: 9731, variacao: -5 }, { composicaoId: 9734, variacao: 5 }] });
    expect(posts('remanejamentos')[0].data).not.toHaveProperty('novaDataFim');
  });
  it('data final anterior à data efetiva recebe recusa local sem POST de prévia', async () => {
    await abrir(); preencherBase(); enviarCampo('Nova data final da vigência', '2026-11-14');
    fireEvent.click(screen.getByRole('button', { name: 'Calcular prévia', exact: true }));
    await act(async () => { await Promise.resolve(); });
    expect(posts('aditamentos/previa')).toEqual([]);
    expect(avisos.some(a => a.type === 'warning' && /data final|vigência/i.test(a.message))).toBe(true);
    expect(screen.getByLabelText('Nova data final da vigência')).toHaveValue('2026-11-14');
  });
});

describe('ROP-064: leituras parciais e fora de ordem do painel', () => {
  it('prévia de A entregue depois da troca de contrato não habilita confirmação em B', async () => {
    const view = await abrir(); preencherBase(); enviarCampo('Novo efetivo total', '75');
    const previaA = diferida(); let configA;
    especial = config => {
      if (config.url === '/contratos/9741/aditamentos/previa') { configA = config; return previaA.promise; }
      if (config.method === 'get' && config.url.startsWith('/contratos/9742/')) return resposta(config, []);
    };
    fireEvent.click(screen.getByRole('button', { name: 'Calcular prévia', exact: true }));
    await waitFor(() => expect(configA).toBeDefined());
    view.rerender(<PainelAlteracoesContrato contrato={{ ...contrato, id: 9742, numeroContrato: 'CONTRATO-B-SINTETICO' }} podeGerenciar/>);
    await waitFor(() => expect(screen.queryByText('Atualizando posição contratual...')).not.toBeInTheDocument());
    await act(async () => { previaA.resolve(resposta(configA, { contratoId: 9741, dataEfetiva: '2026-11-15',
      efetivoAnterior: 100, efetivoResultante: 75, tokenPrevia: 'token-antigo-a', impactosComposicao: [], alertas: [] })); await previaA.promise; });
    expect(screen.getByRole('button', { name: 'Confirmar com esta prévia' })).toBeDisabled();
    expect(pedidos.filter(p => p.method === 'POST' && p.url === '/contratos/9742/aditamentos')).toEqual([]);
    expect(avisos.filter(a => a.type === 'success')).toEqual([]);
  });
  it('editar e voltar aos dados originais não recupera uma prévia em andamento anterior', async () => {
    await abrir(); preencherBase(); enviarCampo('Novo efetivo total', '75');
    const antiga = diferida(); let configA;
    especial = config => {
      if (config.url === '/contratos/9741/aditamentos/previa') { configA = config; return antiga.promise; }
    };
    fireEvent.click(screen.getByRole('button', { name: 'Calcular prévia', exact: true }));
    await waitFor(() => expect(configA).toBeDefined());
    enviarCampo('Novo efetivo total', '80'); enviarCampo('Novo efetivo total', '75');
    await act(async () => { antiga.resolve(resposta(configA, { contratoId: 9741, dataEfetiva: '2026-11-15',
      efetivoAnterior: 100, efetivoResultante: 75, tokenPrevia: 'token-anterior-aba', impactosComposicao: [], alertas: [] })); await antiga.promise; });
    expect(screen.getByRole('button', { name: 'Confirmar com esta prévia' })).toBeDisabled();
    expect(screen.getByRole('button', { name: 'Calcular prévia', exact: true })).toBeEnabled();
  });
  it('falha de catálogo tem feedback acessível e conserva formulário', async () => {
    especial = config => config.url === '/cargos' ? recusar(config, 403, 'Catálogo sintético recusado') : undefined;
    await abrir(); preencherBase();
    expect(screen.getByRole('alert')).toHaveTextContent(/catálogo|opções/i);
    expect(screen.getByLabelText(/Identificação/)).toHaveValue('ADI-PROR-SINTETICO');
    expect(avisos.filter(a => a.type === 'success')).toEqual([]);
  });
  it('falha ao mudar data não deixa a composição anterior sob a nova referência', async () => {
    render(<PainelAlteracoesContrato contrato={contrato} podeGerenciar/>);
    await screen.findByText(/Lotação inicial sintética/, { selector: 'strong' });
    especial = config => config.url === '/contratos/9741/composicoes' && config.params?.data === '2026-12-01'
      ? recusar(config, 403, 'Posição sintética recusada') : undefined;
    enviarCampo('Data de referência', '2026-12-01');
    await waitFor(() => expect(screen.queryByText('Atualizando posição contratual...')).not.toBeInTheDocument());
    expect(screen.queryByText(/Lotação inicial sintética/, { selector: 'strong' })).toBeNull();
    expect(screen.getByRole('alert')).toHaveTextContent(/carregar|posição|consulta/i);
    expect(screen.queryByText('Nenhuma composição válida na data selecionada.')).toBeNull();
  });
  it('resposta de B liberada após C não substitui a composição atual', async () => {
    render(<PainelAlteracoesContrato contrato={contrato}/>);
    await screen.findByText(/Lotação inicial sintética/, { selector: 'strong' });
    const b = diferida(); let configB;
    especial = config => {
      if (config.url !== '/contratos/9741/composicoes') return undefined;
      if (config.params?.data === '2026-12-01') { configB = config; return b.promise; }
      if (config.params?.data === '2026-12-02') return resposta(config, [comp(9772, 'Lotação C sintética')]);
    };
    enviarCampo('Data de referência', '2026-12-01'); await waitFor(() => expect(configB).toBeDefined());
    enviarCampo('Data de referência', '2026-12-02'); await screen.findByText(/Lotação C sintética/, { selector: 'strong' });
    await act(async () => { b.resolve(resposta(configB, [comp(9771, 'Lotação B sintética') ])); await b.promise; });
    expect(screen.getByText(/Lotação C sintética/, { selector: 'strong' })).toBeInTheDocument();
    expect(screen.queryByText(/Lotação B sintética/, { selector: 'strong' })).toBeNull();
  });
});

describe('Aditamento: empresa documental e redução excepcional', () => {
  const fornecedor = { id: 9790, razaoSocial: 'Empresa destino sintética', cnpj: '73995543000160', ativo: true };
  it('não reutiliza a prévia anterior se a nova consulta falhar para o mesmo ato', async () => {
    await abrir(); preencherBase(); await calcular();
    especial = config => config.url === '/contratos/9741/aditamentos/previa'
      ? recusar(config, 409, 'O contexto mudou durante a nova prévia') : undefined;
    fireEvent.click(screen.getByRole('button', { name: 'Calcular prévia', exact: true }));
    await waitFor(() => expect(avisos.some(a => /contexto mudou/.test(JSON.stringify(a)))).toBe(true));
    expect(screen.getByLabelText(/Identificação/)).toHaveValue('ADI-PROR-SINTETICO');
    expect(screen.getByRole('button', { name: 'Confirmar com esta prévia' })).toBeDisabled();
    expect(posts('aditamentos')).toHaveLength(0);
  });
  it('mantém os dados documentais ao experimentar outra empresa e desistir na retificação', async () => {
    especial = (config, dados) => {
      if (config.method === 'get' && config.url === '/contratos/9741/aditamentos') return resposta(config, [{
        id: 9789, eventoId: 9778, versaoInstrumento: 1, identificacao: 'ATO-SEM-TROCA', dataDocumento: '2026-11-03',
        dataEfetiva: '2026-11-15', justificativa: 'Documento histórico sem troca de fornecedor.',
        novaRazaoSocialEmpresa: 'Razão documental histórica', novoCnpjEmpresa: '12345678000190', novaDataFim: '2027-11-30',
        novaMatriculaVinculoObrigatoria: false, novoDiaSemanaReduzido: 'FRIDAY', novasHorasDiaReduzido: 6,
      }]);
      if (config.method === 'get' && config.url === '/fornecedores') return resposta(config, [fornecedor]);
      if (config.method === 'post' && config.url === '/contratos/9741/aditamentos/9789/retificar') return resposta(config, { id: 9788, ...dados });
    };
    await abrir();
    fireEvent.click(screen.getByRole('button', { name: 'Retificar' }));
    const trocar = screen.getByRole('checkbox', { name: /empresa.*aditamento|empresa.*retificação/i });
    if (!trocar.checked) fireEvent.click(trocar);
    const seletor = await screen.findByRole('combobox', { name: 'Nova empresa contratada' });
    await within(seletor).findByRole('option', { name: /Empresa destino sintética/ });
    fireEvent.change(seletor, { target: { value: String(fornecedor.id) } });
    fireEvent.click(trocar);
    await calcular();
    const expected = {
      novoFornecedorId: null, novoCnpjEmpresa: '12345678000190', novaRazaoSocialEmpresa: 'Razão documental histórica',
      eventoPrecedenteId: 9778, novaMatriculaVinculoObrigatoria: false, novoDiaSemanaReduzido: 'FRIDAY', novasHorasDiaReduzido: 6,
    };
    expect(posts('aditamentos/previa').at(-1).data).toMatchObject(expected);
    confirmar(await abrirConfirmacao());
    await waitFor(() => expect(posts('aditamentos/9789/retificar')).toHaveLength(1));
    expect(posts('aditamentos/9789/retificar')[0].data).toMatchObject(expected);
  });
  it('permite avaliar a empresa atual na data efetiva de um novo ato', async () => {
    especial = config => config.method === 'get' && config.url === '/fornecedores' ? resposta(config, [fornecedor]) : undefined;
    await abrir({ contrato: { ...contrato, fornecedorId: fornecedor.id } }); preencherBase();
    fireEvent.click(screen.getByRole('checkbox', { name: 'Alterar empresa neste aditamento' }));
    const seletor = await screen.findByRole('combobox', { name: 'Nova empresa contratada' });
    await within(seletor).findByRole('option', { name: /Empresa destino sintética/ });
    fireEvent.change(seletor, { target: { value: String(fornecedor.id) } });
    enviarCampo('Nome fantasia da nova empresa', 'Documento de retorno futuro');
    await calcular();
    expect(posts('aditamentos/previa').at(-1).data).toMatchObject({ novoFornecedorId: fornecedor.id, dataEfetiva: '2026-11-15' });
  });
  it('não confirma nem recalcula a troca quando o fornecedor some do catálogo ativo', async () => {
    let ativo = true;
    especial = config => config.method === 'get' && config.url === '/fornecedores'
      ? resposta(config, [{ ...fornecedor, ativo }]) : undefined;
    await abrir(); preencherBase();
    fireEvent.click(screen.getByRole('checkbox', { name: 'Alterar empresa neste aditamento' }));
    const seletor = await screen.findByRole('combobox', { name: 'Nova empresa contratada' });
    await within(seletor).findByRole('option', { name: /Empresa destino sintética/ });
    fireEvent.change(seletor, { target: { value: String(fornecedor.id) } });
    enviarCampo('Nome fantasia da nova empresa', 'Documento sintético');
    await calcular();
    ativo = false;
    fireEvent.click(screen.getByRole('tab', { name: 'Remanejamentos' }));
    fireEvent.click(screen.getByRole('tab', { name: 'Aditamentos' }));
    await waitFor(() => {
      expect(pedidos.filter(p => p.url === '/fornecedores')).toHaveLength(2);
      expect(screen.queryByText('Consultando empresas contratadas…')).not.toBeInTheDocument();
    });
    expect(screen.getByRole('button', { name: 'Confirmar com esta prévia' })).toBeDisabled();
    expect(screen.queryByRole('option', { name: /Empresa registrada no ato/ })).not.toBeInTheDocument();
    fireEvent.click(screen.getByRole('button', { name: 'Calcular prévia', exact: true }));
    expect(posts('aditamentos/previa')).toHaveLength(1);
    expect(posts('aditamentos')).toHaveLength(0);
  });
  it('seleciona empresa ativa, preserva dados documentais e exige nova prévia se os dados mudarem', async () => {
    especial = config => config.method === 'get' && config.url === '/fornecedores'
      ? resposta(config, [fornecedor, { id: 9791, razaoSocial: 'Empresa inativa', cnpj: '73995543000240', ativo: false }]) : undefined;
    await abrir(); preencherBase();
    expect(pedidos.filter(x => x.url === '/fornecedores')).toHaveLength(0);
    fireEvent.click(screen.getByRole('checkbox', { name: 'Alterar empresa neste aditamento' }));
    const empresa = await screen.findByRole('combobox', { name: 'Nova empresa contratada' });
    await within(empresa).findByRole('option', { name: /Empresa destino sintética/ });
    expect(within(empresa).queryByRole('option', { name: /Empresa inativa/ })).not.toBeInTheDocument();
    fireEvent.change(empresa, { target: { value: String(fornecedor.id) } });
    expect(screen.getByLabelText('CNPJ da nova empresa')).toHaveValue(fornecedor.cnpj);
    expect(screen.getByLabelText('Razão social da nova empresa')).toHaveValue(fornecedor.razaoSocial);
    enviarCampo('Nome fantasia da nova empresa', 'Destino documental sintético');
    await calcular();
    expect(posts('aditamentos/previa').at(-1).data).toMatchObject({ novoFornecedorId: 9790, novoCnpjEmpresa: fornecedor.cnpj,
      novaRazaoSocialEmpresa: fornecedor.razaoSocial, novoNomeFantasiaEmpresa: 'Destino documental sintético' });
    enviarCampo('Nome fantasia da nova empresa', 'Destino retificado no formulário');
    expect(screen.getByRole('button', { name: 'Confirmar com esta prévia' })).toBeDisabled();
    await calcular(); confirmar(await abrirConfirmacao());
    await waitFor(() => expect(posts('aditamentos')).toHaveLength(1));
    expect(posts('aditamentos')[0].data).toMatchObject({ novoFornecedorId: 9790, novoCnpjEmpresa: fornecedor.cnpj,
      novaRazaoSocialEmpresa: fornecedor.razaoSocial, novoNomeFantasiaEmpresa: 'Destino retificado no formulário', tokenPrevia: 'previa-unitaria-2' });
  });
  it('confirma explicitamente a exceção válida sem invalidar o token de seus impactos', async () => {
    especial = (config, data) => config.url === '/contratos/9741/aditamentos/previa' ? resposta(config, {
      contratoId: 9741, dataEfetiva: data.dataEfetiva, efetivoAnterior: 100, efetivoResultante: 74,
      efetivoOriginalPublicado: 100, pisoNormalReducao: 75, exigeAutorizacaoExcepcional: true,
      possuiImpactoOcupacao: false, impactosComposicao: [], alertas: [], tokenPrevia: 'previa-excecao',
    }) : undefined;
    await abrir(); preencherBase(); enviarCampo('Novo efetivo total', '74');
    fireEvent.click(screen.getByRole('checkbox', { name: 'Usar fluxo excepcional para redução acima do limite normal' }));
    enviarCampo(/Fundamento da exceção/, 'Ato sintético com fonte comprobatória e escopo definido.');
    fireEvent.click(screen.getByRole('button', { name: 'Calcular prévia', exact: true }));
    const aceite = await screen.findByRole('checkbox', { name: /Confirmo os impactos desta redução excepcional/ });
    expect(posts('aditamentos/previa')).toHaveLength(1);
    expect(posts('aditamentos/previa')[0].data).toMatchObject({ reducaoExcepcional: true, confirmarImpacto: false });
    expect(screen.getByRole('button', { name: 'Confirmar com esta prévia' })).toBeDisabled();
    fireEvent.click(aceite);
    expect(screen.getByRole('heading', { name: 'Prévia calculada no servidor' })).toBeInTheDocument();
    confirmar(await abrirConfirmacao());
    await waitFor(() => expect(posts('aditamentos')).toHaveLength(1));
    expect(posts('aditamentos')[0].data).toMatchObject({ reducaoExcepcional: true, confirmarImpacto: true,
      fundamentoExcecao: 'Ato sintético com fonte comprobatória e escopo definido.', tokenPrevia: 'previa-excecao' });
  });
});
