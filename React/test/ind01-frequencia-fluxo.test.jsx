import { act, cleanup, renderHook, waitFor } from '@testing-library/react';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';

const { requisitar, notificar, atualizarPerfil, confirmarComSenha, user } = vi.hoisted(() => ({
  requisitar: vi.fn(), notificar: vi.fn(), atualizarPerfil: vi.fn(),
  confirmarComSenha: vi.fn(async ({ acao }) => acao('senha-sintetica')),
  user: { perfil: 'OPERACIONAL', vinculosGestao: [{ lotacao: { id: 7, label: 'Lotação 7' }, jornada: { id: 8, label: 'Turno 8' } }] },
}));
vi.mock('../src/core/http.js', async (original) => ({ ...(await original()), requisitar }));
vi.mock('../src/core/events.js', () => ({ notificar }));
vi.mock('../src/modules/auth/context.jsx', () => ({ useAutenticacao: () => ({ user, atualizarPerfil }) }));
vi.mock('../src/core/security/useConfirmacaoSenha.jsx', () => ({ useConfirmacaoSenha: () => ({ confirmarComSenha, ModalConfirmacao: () => null }) }));
import { useLogicaFrequencia } from '../src/modules/rh/pages/PaginaFrequencia/useLogicaFrequencia.js';

let frequencias;
let historicos;
const empregado = { id: 17, nome: 'Ana Silva', matricula: 'MAT-123', cpf: '111.111.111-11', lotacaoId: 7, lotacaoNome: 'Lotação 7', turnoId: 8, turnoDescricao: 'Turno 8', cargoNome: 'Cargo' };
beforeEach(() => {
  requisitar.mockReset(); notificar.mockReset(); atualizarPerfil.mockReset(); confirmarComSenha.mockClear(); frequencias = [];
  user.perfil = 'OPERACIONAL'; historicos = [];
  requisitar.mockImplementation(async ({ url, method, data }) => {
    if (url === '/lotacoes') return [{ id: 7, nome: 'Lotação 7', turnos: [{ id: 8, descricao: 'Turno 8' }] }];
    if (url === '/contratos') return [{ id: 9, ativo: true }];
    if (url === '/jornadas') return [{ id: 8, descricao: 'Turno 8', dias: [] }];
    if (url === '/empregados/filtrados-frequencia') return { content: [empregado], totalElements: 1, totalPages: 1, number: 0 };
    if (url === '/frequencias' && method === 'GET') return frequencias;
    if (url === '/frequencias' && method === 'POST') return { ...data, id: 71, empregado: { id: 17 } };
    if (url === '/vinculos-contratuais') return { content: [{ id: 902, pessoaId: 202, contratoId: 9, matricula: 'MAT-1234', pessoaNome: 'Ana Silva' }], totalPages: 1 };
    if (url === '/empregados/17/historico-contratos') return historicos;
    if (url === '/vinculos-contratuais/501/alocacoes/posicao') return { vinculoId: 501, dataReferencia: new Date().toLocaleDateString('en-CA'), situacaoTemporal: 'VIGENTE', alocacaoId: 61, lotacaoId: 7, jornadaId: 8 };
    if (url === '/frequencias/fechamentos/atual') return null;
    return [];
  });
});
afterEach(() => { cleanup(); });

async function carregar() {
  const hook = renderHook(() => useLogicaFrequencia());
  await waitFor(() => expect(hook.result.current.estado.listaDaFrequencia).toHaveLength(1));
  return hook;
}

describe('IND-01: comportamento de gravação da grade real', () => {
  it('não transforma linha sem associação em rascunho gravável e não envia outro vínculo', async () => {
    const { result } = await carregar();
    await act(async () => result.current.acoes.invocarMudancaDeStatusDoUsuarioNaTabela(17, 'PRESENCA'));
    await act(async () => result.current.acoes.invocarGatilhoDeDespachoAoServidorGlobal());
    expect(result.current.estado.listaDaFrequencia[0]).toMatchObject({ empregadoId: 17, identidadeCanonicaDisponivel: false });
    expect(result.current.estado.listaDaFrequencia[0].alterado).not.toBe(true);
    expect(requisitar.mock.calls.filter(([r]) => r.method === 'POST')).toEqual([]);
    expect(requisitar.mock.calls.filter(([r]) => r.url === '/vinculos-contratuais')).toEqual([]);
  });

  it('mantém envio com ID explícito e origem da linha para conferência Java', async () => {
    frequencias = [{ id: 71, empregado: { id: 17 }, vinculoContratualId: 501, pessoaId: 101, status: 'FALTA', validadeOperacional: 'VALIDA', estadoNegocial: 'ABERTA', estadoAuditoria: 'ORIGINAL' }];
    const { result } = await carregar();
    await act(async () => result.current.acoes.invocarMudancaDeStatusDoUsuarioNaTabela(17, 'PRESENCA'));
    await act(async () => result.current.acoes.invocarGatilhoDeDespachoAoServidorGlobal());
    const posts = requisitar.mock.calls.filter(([r]) => r.method === 'POST');
    expect(posts).toHaveLength(1);
    expect(posts[0][0]).toMatchObject({ url: '/frequencias', data: { empregado: { id: 17 }, vinculoContratualId: 501, status: 'PRESENCA' } });
    expect(requisitar.mock.calls.filter(([r]) => r.url === '/vinculos-contratuais')).toEqual([]);
  });

  it('Gestor Local usa associação canônica explícita do histórico da linha para criar frequência', async () => {
    user.perfil = 'GESTOR_LOCAL';
    const { result } = await carregar();
    historicos = [{ empregadoId: 17, contratoId: 9, vinculoCanonicoId: 501, dataReferenciaIdentidade: result.current.estado.dataReferenciaAtiva }];
    await act(async () => result.current.acoes.setJustificativaRetificacao('Conferência documentada'));
    await act(async () => result.current.acoes.invocarMudancaDeStatusDoUsuarioNaTabela(17, 'PRESENCA'));
    await act(async () => result.current.acoes.invocarGatilhoDeDespachoAoServidorGlobal());
    const posts = requisitar.mock.calls.filter(([r]) => r.method === 'POST');
    expect(posts).toHaveLength(1);
    expect(posts[0][0]).toMatchObject({ url: '/frequencias', data: { empregado: { id: 17 }, vinculoContratualId: 501, status: 'PRESENCA' } });
    expect(requisitar.mock.calls.some(([r]) => r.url === '/vinculos-contratuais')).toBe(false);
  });

  it('descarta associação pendente após trocar a data e não envia a linha antiga', async () => {
    user.perfil = 'GESTOR_LOCAL';
    let liberar;
    historicos = new Promise((resolve) => { liberar = resolve; });
    const { result } = await carregar();
    const dataAnterior = result.current.estado.dataReferenciaAtiva;
    await act(async () => result.current.acoes.setJustificativaRetificacao('Conferência documentada'));
    await act(async () => result.current.acoes.invocarMudancaDeStatusDoUsuarioNaTabela(17, 'PRESENCA'));
    let envio;
    await act(async () => { envio = result.current.acoes.invocarGatilhoDeDespachoAoServidorGlobal(); });
    await waitFor(() => expect(requisitar.mock.calls.some(([r]) => r.url === '/empregados/17/historico-contratos')).toBe(true));
    await act(async () => result.current.acoes.setDataReferenciaAtiva('2026-10-31'));
    await act(async () => {
      liberar([{ empregadoId: 17, contratoId: 9, vinculoCanonicoId: 501, dataReferenciaIdentidade: dataAnterior }]);
      await envio;
    });
    expect(requisitar.mock.calls.filter(([r]) => r.method === 'POST')).toEqual([]);
    expect(result.current.estado.listaDaFrequencia[0].identidadeCanonicaDisponivel).toBe(false);
  });
});
