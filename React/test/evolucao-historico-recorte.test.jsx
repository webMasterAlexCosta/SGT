// A rota Java foi evoluída para K opcional; o método legado D continua preservado.
import React from 'react';
import { act, cleanup, fireEvent, render, screen, waitFor, within } from '@testing-library/react';
import '@testing-library/jest-dom/vitest';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
const { requisitar, auth } = vi.hoisted(() => ({ requisitar: vi.fn(), auth: { user: null } }));
vi.mock('../src/core/http.js', () => ({ requisitar, extrairLista: valor => Array.isArray(valor) ? valor : valor?.content || [] }));
vi.mock('../src/modules/auth/context.jsx', () => ({ useAutenticacao: () => auth }));
import { ServicoHistorico } from '../src/services/historico/ServicoHistorico.js';
import { PaginaHistorico } from '../src/modules/historico/PaginaHistorico.jsx';
import { invalidarGeracaoSessao } from '../src/core/security/geracaoSessao.js';
import { diferida } from './fixtures/promessaDiferida.js';

const K = '2026-02-01T12:00:00.123456Z';
const origem = { tipo: 'PROVA_HISTORICA', provaId: 81, composicaoId: 71, dataEfetiva: '2026-01-01', registradoEm: K };
const fragmento = { id: 71, lotacaoId: 1, lotacaoNome: 'Lotação documentada em K', jornadaId: 2,
  jornadaDescricao: 'Jornada documentada', cargoId: 3, cargoNome: 'Cargo documentado', efetivoPrevisto: 0,
  lotacaoMunicipio: null, jornadaSemanal: null, dataReferencia: '2026-01-01', conhecidoAte: K,
  provaCampos: { lotacaoMunicipio: { estado: 'AUSENCIA_COMPROVADA', visibilidade: 'DISPONIVEL', origens: [origem] },
    efetivoPrevisto: { estado: 'VALOR_COMPROVADO', visibilidade: 'DISPONIVEL', origens: [origem] },
    lotacaoNome: { estado: 'VALOR_COMPROVADO', visibilidade: 'DISPONIVEL', origens: [origem] } } };
beforeEach(() => { invalidarGeracaoSessao(); requisitar.mockReset(); requisitar.mockResolvedValue([]); auth.user = null; });
afterEach(() => { cleanup(); invalidarGeracaoSessao(); });
const preencher = () => {
  for (const [rotulo, valor] of [['ID do contrato', '9'], ['Data efetiva consultada', '2026-01-01'], ['Conhecido até (ISO com fuso)', K]]) {
    fireEvent.change(screen.getByLabelText(rotulo), { target: { value: valor } });
  }
  fireEvent.click(screen.getByRole('button', { name: 'Consultar histórico' }));
};

describe('Composições históricas no recorte efetivamente consultado', () => {
  it('oferece leitura aditiva com D/K sem alterar o contrato do método legado', async () => {
    expect(typeof ServicoHistorico.listarComposicoesNoRecorte).toBe('function');
    const alvo = { id: 9, data: '2026-01-01', conhecidoAte: K };
    const controlador = new AbortController();
    await ServicoHistorico.listarComposicoesNoRecorte(alvo, { signal: controlador.signal });
    await ServicoHistorico.listarComposicoes(alvo);
    expect(requisitar.mock.calls[0][0]).toMatchObject({ method: 'GET', url: '/contratos/9/composicoes',
      params: { data: '2026-01-01', conhecidoAte: K }, signal: controlador.signal, silentError: true });
    expect(requisitar.mock.calls[1][0]).toMatchObject({ params: { data: '2026-01-01' } });
  });

  it('a página usa o novo D/K e mostra o fragmento devolvido nesse recorte', async () => {
    requisitar.mockImplementation(({ url, params }) => Promise.resolve(url.endsWith('/composicoes') && params?.conhecidoAte
      ? [fragmento] : url.endsWith('/posicao-temporal') ? { contratoId: 9, dataReferencia: '2026-01-01', conhecidoAte: K,
        versaoContratual: null, eventosAplicados: [], relacoesConsideradas: [] } : []));
    render(<PaginaHistorico />); preencher();
    const tabela = await screen.findByRole('table', { name: 'Composições no recorte D/K' });
    expect(tabela).toHaveTextContent('Lotação documentada em K');
    fireEvent.click(within(tabela).getByText('Ver registro 71'));
    expect(await within(tabela).findByText(/ausência comprovada/i)).toBeInTheDocument();
    expect(requisitar.mock.calls.some(([cfg]) => cfg.url === '/contratos/9/composicoes' && cfg.params?.conhecidoAte === K)).toBe(true);
    expect(screen.getByLabelText('Conhecido até (ISO com fuso)')).toHaveValue(K);
  });

  it('não apresenta fragmento do K anterior depois de mudar o corte', async () => {
    const pendente = diferida();
    requisitar.mockImplementation(({ url, params }) => url.endsWith('/composicoes') && params?.conhecidoAte ? pendente.promise : Promise.resolve([]));
    render(<PaginaHistorico />); preencher();
    await waitFor(() => expect(requisitar.mock.calls.some(([cfg]) => cfg.url.endsWith('/composicoes') && cfg.params?.conhecidoAte)).toBe(true));
    fireEvent.change(screen.getByLabelText('Conhecido até (ISO com fuso)'), { target: { value: '2026-02-01T12:00:00.123455Z' } });
    await act(async () => { pendente.resolve([fragmento]); await pendente.promise; });
    expect(screen.queryByText('Lotação documentada em K')).not.toBeInTheDocument();
  });
});
