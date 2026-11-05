// @vitest-environment jsdom
import React from 'react';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { act, cleanup, fireEvent, render, screen, waitFor, within } from '@testing-library/react';
import '@testing-library/jest-dom/vitest';

const { requisitar, confirmarComSenha } = vi.hoisted(() => ({ requisitar: vi.fn(), confirmarComSenha: vi.fn() }));
vi.mock('../src/core/http.js', () => ({
  requisitar, requisitarDownload: vi.fn(),
  extrairLista: (resposta) => Array.isArray(resposta) ? resposta : resposta?.content || [],
  extrairPagina: (resposta) => Array.isArray(resposta) ? { content: resposta, totalPages: 1 } : resposta,
}));
vi.mock('../src/modules/auth/context.jsx', () => ({ useAutenticacao: () => ({ user: { perfil: 'GESTOR_LOCAL', gestaoLocalId: 1 } }) }));
vi.mock('../src/core/security/useConfirmacaoSenha.jsx', () => ({ useConfirmacaoSenha: () => ({ confirmarComSenha, ModalConfirmacao: () => null, fechar: vi.fn() }) }));
vi.mock('../src/core/events.js', () => ({ notificar: vi.fn() }));
import { PainelExcecoesContrato } from '../src/modules/admin/pages/PaginaContratos/components/PainelExcecoesContrato/index.jsx';

const mapa = { id: 70, composicaoOrigemId: 31, lotacaoOrigemId: 32, lotacaoOrigemNome: 'Origem fotografada', jornadaOrigemId: 33, jornadaOrigemDescricao: 'Turno fotografado', cargoOrigemId: 34, cargoOrigemNome: 'Cargo fotografado', lotacaoDestinoId: 42, lotacaoDestinoNome: 'Destino fotografado', jornadaDestinoId: 43, jornadaDestinoDescricao: 'Turno destino', cargoDestinoId: 44, cargoDestinoNome: 'Cargo destino' };
const transferencia = { id: 17, contratoId: 9, eventoContratoId: 101, gestaoOrigemId: 1, gestaoOrigemNome: 'Gestão origem histórica', gestaoDestinoId: 23, gestaoDestinoNome: 'Gestão destino histórica', dataEfetiva: '2099-11-05', motivo: 'Transferência anterior', referenciaSei: 'TESTE-ANTERIOR', autorizacaoAdministrativa: 'AUTORIZACAO-ANTERIOR', registradoEm: '2026-11-01T12:00:00Z', responsavelId: 7, responsavelUsername: 'gestor-sintetico', mapeamentos: [mapa], tipoContratacaoDestinoId: 71, tipoContratacaoDestinoDescricao: 'Tipo fotografado' };
const evento = { id: 101, tipoEvento: 'TRANSFERENCIA_GESTAO_LOCAL', situacaoFato: 'VIGENTE', dataEfetiva: '2099-11-05', registradoEm: '2026-11-01T12:00:00Z' };
const respostaPrevia = { contratoId: 9, operacao: 'transferir-gestao', dataEfetiva: '2026-11-05', dataAto: '2026-11-01', motivo: 'Intenção documentada', usuarioResponsavelId: 7, usuarioResponsavelUsername: 'gestor-sintetico', autorizacaoNecessaria: 'TRANSFERENCIA_GESTAO_LOCAL', quantidadeVinculosAfetados: 0, vinculosAfetados: [], programacoesFuturasAfetadas: [], quantidadeFrequenciasAfetadas: 0, datasComFrequenciasAfetadas: [], impactosConhecidos: ['Os mapas informados serão revalidados.'], exigeConfirmacao: true, tokenPrevia: 'token-transferencia', limiteDetalhes: 200, vinculosTruncados: false, quantidadeProgramacoesFuturasAfetadas: 0, programacoesTruncadas: false, quantidadeDatasComFrequenciasAfetadas: 0, datasComFrequenciasTruncadas: false };

const responder = async (pedido) => {
  if (pedido.url.endsWith('/transferencias-gestao/previa')) return respostaPrevia;
  if (pedido.url.endsWith('/transferencias-gestao')) return [transferencia];
  if (pedido.url.endsWith('/eventos')) return [evento];
  if (pedido.url.endsWith('/composicoes')) return [{ id: 31, lotacaoId: 32, lotacaoNome: 'Origem na data', jornadaId: 33, jornadaDescricao: 'Turno na data', cargoId: 34, cargoNome: 'Cargo na data', efetivoPrevisto: 1, permiteNovaAlocacao: true }];
  if (pedido.url === '/gestoes-locais') return [{ id: 1, nome: 'Somente origem acessível', ativo: true }];
  if (pedido.url === '/lotacoes' || pedido.url === '/cargos') return { content: [], totalPages: 1 };
  if (pedido.method === 'POST') return { id: 9, estadoNegocial: 'PUBLICADO' };
  return [];
};

beforeEach(() => {
  requisitar.mockReset(); requisitar.mockImplementation(responder);
  confirmarComSenha.mockReset(); confirmarComSenha.mockImplementation(async ({ acao }) => { await acao('senha-sintetica'); return true; });
});
afterEach(cleanup);

const montar = (props = {}) => render(<PainelExcecoesContrato contrato={{ id: 9, gestaoLocalId: 1, estadoNegocial: 'PUBLICADO' }} eventos={[evento]} podeGerenciar {...props} />);
const abrirTransferencias = async () => {
  montar();
  fireEvent.click(screen.getByRole('tab', { name: 'Transferências' }));
  await screen.findByText('Gestão origem histórica → Gestão destino histórica');
};
const mudar = (rotulo, valor) => fireEvent.change(screen.getByLabelText(rotulo), { target: { value: valor } });

const preencher = async () => {
  await abrirTransferencias();
  const destino = screen.queryByLabelText(/Gestão destino.*ID/);
  expect(destino).toBeInTheDocument();
  fireEvent.change(destino, { target: { value: '23' } });
  mudar(/Tipo de contratação destino.*ID/, '71');
  mudar(/Data efetiva da transferência/, '2026-11-05');
  mudar(/^SEI da transferência/, 'TESTE-TRANSFERENCIA');
  mudar(/^Autorização administrativa/, 'AUTORIZACAO-SINTETICA');
  mudar(/^Motivo da transferência/, 'Intenção documentada');
  fireEvent.click(screen.getByRole('button', { name: 'Adicionar mapa' }));
  for (const [rotulo, valor] of [['Composição origem do mapa 1', '31'], ['Lotação origem do mapa 1', '32'], ['Turno origem do mapa 1', '33'], ['Cargo origem do mapa 1', '34'], ['Lotação destino do mapa 1', '42'], ['Turno destino do mapa 1', '43'], ['Cargo destino do mapa 1', '44']]) mudar(rotulo, valor);
};

describe('Transferência de gestão conforme os contratos congelados', () => {
  it('falha de consulta aparece como indisponível e não como histórico vazio', async () => {
    requisitar.mockImplementation((pedido) => pedido.url.endsWith('/transferencias-gestao')
      ? Promise.reject({ status: 403, message: 'Histórico de transferências indisponível para este contexto.' })
      : responder(pedido));
    montar();
    fireEvent.click(screen.getByRole('tab', { name: 'Transferências' }));
    expect(await screen.findByRole('alert')).toHaveTextContent('Histórico de transferências indisponível para este contexto.');
    expect(screen.queryByText(/Nenhuma transferência retornada/)).not.toBeInTheDocument();
  });

  it('um conflito invalida a prévia e conserva os mapas e a intenção', async () => {
    requisitar.mockImplementation((pedido) => pedido.url.endsWith('/transferir-gestao')
      ? Promise.reject({ status: 409, message: 'Os mapas mudaram no servidor.' }) : responder(pedido));
    await preencher();
    fireEvent.click(screen.getByRole('button', { name: /Gerar prévia da transferência/ }));
    await screen.findByText('Os mapas informados serão revalidados.');
    fireEvent.click(screen.getByRole('checkbox', { name: /Confirmo.*impactos.*transferência/i }));
    fireEvent.submit(screen.getByRole('form', { name: 'Transferência de gestão' }));
    expect(await screen.findByRole('alert')).toHaveTextContent('Os mapas mudaram no servidor.');
    expect(screen.getByLabelText('Lotação destino do mapa 1')).toHaveValue(42);
    expect(screen.getByLabelText(/^Motivo da transferência/)).toHaveValue('Intenção documentada');
    expect(screen.queryByRole('checkbox', { name: /Confirmo.*impactos.*transferência/i })).not.toBeInTheDocument();
    expect(screen.getByRole('button', { name: 'Confirmar transferência' })).toBeDisabled();
    expect(requisitar.mock.calls.filter(([r]) => r.url.endsWith('/transferir-gestao'))).toHaveLength(1);
  });

  it('bloqueia duplo envio e revoga a prévia quando a confirmação de senha é cancelada', async () => {
    let resolver;
    confirmarComSenha.mockImplementation(() => new Promise((resolve) => { resolver = resolve; }));
    await preencher();
    fireEvent.click(screen.getByRole('button', { name: /Gerar prévia da transferência/ }));
    await screen.findByText('Os mapas informados serão revalidados.');
    fireEvent.click(screen.getByRole('checkbox', { name: /Confirmo.*impactos.*transferência/i }));
    const form = screen.getByRole('form', { name: 'Transferência de gestão' });
    fireEvent.submit(form); fireEvent.submit(form);
    expect(confirmarComSenha).toHaveBeenCalledTimes(1);
    await act(async () => resolver(false));
    expect(screen.queryByRole('checkbox', { name: /Confirmo.*impactos.*transferência/i })).not.toBeInTheDocument();
    expect(screen.getByLabelText('Cargo destino do mapa 1')).toHaveValue(44);
    expect(requisitar.mock.calls.some(([r]) => r.url.endsWith('/transferir-gestao'))).toBe(false);
  });

  it('apresenta cinco transferências por página e não oferece mutações ao leitor', async () => {
    requisitar.mockImplementation((pedido) => pedido.url.endsWith('/transferencias-gestao')
      ? Promise.resolve(Array.from({ length: 6 }, (_, i) => ({ ...transferencia, id: i + 1, gestaoOrigemNome: `Origem histórica ${i + 1}` })))
      : responder(pedido));
    montar({ podeGerenciar: false });
    fireEvent.click(screen.getByRole('tab', { name: 'Transferências' }));
    await screen.findByText('Origem histórica 1 → Gestão destino histórica');
    expect(screen.queryByText('Origem histórica 6 → Gestão destino histórica')).not.toBeInTheDocument();
    expect(screen.getAllByText(/Origem histórica.*→/)).toHaveLength(5);
    const paginas = screen.getByRole('navigation', { name: 'Páginas do histórico de transferências' });
    fireEvent.click(within(paginas).getByRole('button', { name: 'Próxima' }));
    expect(screen.getByText('Origem histórica 6 → Gestão destino histórica')).toBeInTheDocument();
    expect(screen.getAllByText(/Origem histórica.*→/)).toHaveLength(1);
    expect(screen.queryByRole('button', { name: /Retificar transferência|Cancelar transferência futura|Gerar prévia/ })).not.toBeInTheDocument();
  });

  it('aceita IDs documentados de destino fora da listagem local e só confirma após prévia', async () => {
    await preencher();
    fireEvent.click(screen.getByRole('button', { name: /Gerar prévia da transferência/ }));
    await screen.findByText('Os mapas informados serão revalidados.');
    expect(requisitar).toHaveBeenCalledWith(expect.objectContaining({ url: '/contratos/9/transferencias-gestao/previa', data: expect.objectContaining({ gestaoLocalDestinoId: 23, tipoContratacaoDestinoId: 71, dataEfetiva: '2026-11-05', tokenPrevia: null, confirmarImpacto: false }) }));
    expect(requisitar.mock.calls.some(([r]) => r.url === '/contratos/9/transferir-gestao')).toBe(false);
    fireEvent.click(screen.getByRole('checkbox', { name: /Confirmo.*impactos.*transferência/i }));
    fireEvent.click(screen.getByRole('button', { name: 'Confirmar transferência' }));
    await waitFor(() => expect(requisitar).toHaveBeenCalledWith(expect.objectContaining({ url: '/contratos/9/transferir-gestao', data: expect.objectContaining({ tipoContratacaoDestinoId: 71, tokenPrevia: 'token-transferencia', confirmarImpacto: true }) })));
  });

  it('carrega composição pela data da intenção, sem reutilizar a seleção de hoje', async () => {
    await abrirTransferencias();
    const data = screen.queryByLabelText(/Data efetiva da transferência/);
    expect(data).toBeInTheDocument();
    fireEvent.change(data, { target: { value: '2025-03-17' } });
    await waitFor(() => expect(requisitar).toHaveBeenCalledWith(expect.objectContaining({ url: '/contratos/9/composicoes', params: { data: '2025-03-17' } })));
  });

  it('mudar um mapa invalida a prévia e impede enviar o token antigo', async () => {
    await preencher();
    fireEvent.click(screen.getByRole('button', { name: /Gerar prévia da transferência/ }));
    await screen.findByText('Os mapas informados serão revalidados.');
    fireEvent.click(screen.getByRole('checkbox', { name: /Confirmo.*impactos.*transferência/i }));
    mudar('Cargo destino do mapa 1', '45');
    expect(screen.queryByRole('checkbox', { name: /Confirmo.*impactos.*transferência/i })).not.toBeInTheDocument();
    expect(screen.getByRole('button', { name: 'Confirmar transferência' })).toBeDisabled();
  });

  it('cancela somente pelo endpoint existente com a data programada original', async () => {
    await abrirTransferencias();
    const botao = screen.queryByRole('button', { name: /Cancelar transferência futura/ });
    expect(botao).toBeInTheDocument();
    fireEvent.click(botao);
    const form = screen.getByRole('form', { name: 'Cancelamento de transferência' });
    fireEvent.change(within(form).getByLabelText(/Motivo do cancelamento/), { target: { value: 'Revogação documentada' } });
    fireEvent.change(within(form).getByLabelText(/SEI do cancelamento/), { target: { value: 'TESTE-CANCELAR' } });
    fireEvent.change(within(form).getByLabelText(/Autorização do cancelamento/), { target: { value: 'AUTORIZACAO-SINTETICA' } });
    fireEvent.submit(form);
    await waitFor(() => expect(requisitar).toHaveBeenCalledWith(expect.objectContaining({ method: 'POST', url: '/contratos/9/transferencias-gestao/17/cancelar', data: expect.objectContaining({ dataEfetiva: '2099-11-05', motivo: 'Revogação documentada' }) })));
    expect(requisitar.mock.calls.some(([r]) => r.method === 'DELETE')).toBe(false);
  });

  it('retifica usando o evento e o mapa original da transferência selecionada', async () => {
    await abrirTransferencias();
    const botao = screen.queryByRole('button', { name: 'Retificar transferência' });
    expect(botao).toBeInTheDocument();
    fireEvent.click(botao);
    expect(screen.getByLabelText('Evento precedente da transferência (ID)')).toHaveValue(101);
    expect(screen.getByLabelText('Relação de precedência da transferência')).toHaveValue('RETIFICA');
    expect(screen.getByLabelText('Lotação origem do mapa 1')).toHaveValue(32);
    expect(screen.getByLabelText('Lotação destino do mapa 1')).toHaveValue(42);
  });

  it('abre correção de anulação em formulário com prévia, sem prompts nativos', async () => {
    montar({ contrato: { id: 9, gestaoLocalId: 1, estadoNegocial: 'ANULADO' }, eventos: [{ id: 102, tipoEvento: 'ANULACAO', situacaoFato: 'VIGENTE', dataEfetiva: '2026-10-01' }] });
    fireEvent.click(screen.getByRole('tab', { name: 'Correções' }));
    const prompt = vi.spyOn(window, 'prompt').mockReturnValue(null);
    fireEvent.click(screen.getByRole('button', { name: 'Registrar CANCELA' }));
    expect(prompt).not.toHaveBeenCalled();
    const dialogo = screen.queryByRole('dialog', { name: 'Corrigir anulação' });
    expect(dialogo).toBeInTheDocument();
    expect(within(dialogo).getByRole('button', { name: 'Gerar prévia' })).toBeInTheDocument();
    prompt.mockRestore();
  });
});
