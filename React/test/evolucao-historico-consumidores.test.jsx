// RPI-03: consumidores reais; substitui somente transporte/auth para DTOs sintéticos explícitos.
import React from 'react';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { cleanup, fireEvent, render, screen, waitFor, within } from '@testing-library/react';
import '@testing-library/jest-dom/vitest';
const { requisitar } = vi.hoisted(() => ({ requisitar: vi.fn() }));
vi.mock('../src/core/http.js', () => ({ requisitar, requisitarDownload: vi.fn(),
  extrairLista: value => Array.isArray(value) ? value : value?.content || [],
  extrairPagina: value => Array.isArray(value) ? { content: value, totalPages: 1 } : value,
}));
vi.mock('../src/modules/auth/context.jsx', () => ({ useAutenticacao: () => ({ user: { id: 7, perfil: 'GESTOR_LOCAL', gestaoLocalId: 1 } }) }));
vi.mock('../src/core/security/useConfirmacaoSenha.jsx', () => ({ useConfirmacaoSenha: () => ({ confirmarComSenha: vi.fn(), ModalConfirmacao: () => null, fechar: vi.fn() }) }));
vi.mock('../src/core/events.js', () => ({ notificar: vi.fn() }));
import { ModalDetalhesContrato } from '../src/modules/admin/pages/PaginaContratos/components/ModalDetalhesContrato/index.jsx';
import { PainelAlteracoesContrato } from '../src/modules/admin/pages/PaginaContratos/components/PainelAlteracoesContrato/index.jsx';
import { PainelExcecoesContrato } from '../src/modules/admin/pages/PaginaContratos/components/PainelExcecoesContrato/index.jsx';

const origem = { tipo: 'PROVA_HISTORICA', provaId: 81, composicaoId: 31,
  dataEfetiva: '2026-01-01', registradoEm: '2026-02-01T12:00:00.123456Z' };
const prova = estado => ({ estado, visibilidade: 'DISPONIVEL', origens: estado === 'NAO_COMPROVADO' ? [] : [origem] });
const restrita = { estado: null, visibilidade: 'RESTRITA', origens: [] };
const contrato = { id: 9, numeroContrato: 'CADASTRO-ATUAL-SEPARADO', anoContrato: 2026,
  estadoNegocial: 'PUBLICADO', situacaoTemporal: 'VIGENTE', habilitacaoAdministrativa: 'ATIVO' };
const composicao = () => ({ id: 31, contratoId: 9, lotacaoId: 32, jornadaId: 33, cargoId: 34,
  lotacaoNome: 'LOTACAO-RESTRITA-RPI03', jornadaDescricao: 'TURNO-RESIDUAL-RPI03', cargoNome: 'Cargo documentado',
  efetivoPrevisto: 0, dataInicioVigencia: '2026-01-01', dataFimVigencia: null,
  permiteNovaAlocacao: false, version: 0, provaCampos: {
    lotacaoId: prova('VALOR_COMPROVADO'), jornadaId: prova('VALOR_COMPROVADO'), cargoId: prova('VALOR_COMPROVADO'),
    lotacaoNome: restrita, jornadaDescricao: prova('NAO_COMPROVADO'), cargoNome: prova('VALOR_COMPROVADO'),
    efetivoPrevisto: prova('NAO_COMPROVADO'), dataInicioVigencia: prova('VALOR_COMPROVADO'),
    dataFimVigencia: prova('AUSENCIA_COMPROVADA'), permiteNovaAlocacao: prova('VALOR_COMPROVADO'),
  } });
let composicoes;
beforeEach(() => {
  composicoes = [composicao()];
  requisitar.mockReset(); requisitar.mockImplementation(async ({ url }) => url.endsWith('/composicoes') ? composicoes : []);
});
afterEach(cleanup);
const montarModal = extras => render(<ModalDetalhesContrato aberto contrato={contrato} eventos={[]} precedencias={[]}
  aoFechar={vi.fn()} aoConsultarTemporal={vi.fn()} {...extras} />);

describe('RPI-03: todos os consumidores das leituras com prova por campo', () => {
  it('o detalhe contratual interpreta visibilidade e estados antes dos rótulos dos eixos', async () => {
    montarModal({ posicaoTemporal: { contratoId: 9, dataReferencia: '2026-01-01', conhecidoAte: origem.registradoEm,
      estadoNegocial: 'PUBLICADO', situacaoTemporal: 'VIGENTE', habilitacaoAdministrativa: 'INATIVO',
      provaCampos: { estadoNegocial: restrita, situacaoTemporal: prova('NAO_COMPROVADO'), habilitacaoAdministrativa: prova('AUSENCIA_COMPROVADA') },
    } });
    const secao = screen.getByRole('heading', { name: 'Posição temporal / bitemporal' }).closest('section');
    expect(secao).toHaveTextContent(/Acesso restrito/i);
    expect(secao).toHaveTextContent(/Desconhecido|não comprovado/i);
    expect(secao).toHaveTextContent(/Ausência comprovada/i);
    expect(secao).not.toHaveTextContent(/Negocial:\s*Publicado|Temporal:\s*Vigente|Habilitação:\s*Inativo/);
    await waitFor(() => expect(requisitar).toHaveBeenCalled());
  });

  it('a lista de versões não expõe número restrito nem trata ano desconhecido como fato', async () => {
    montarModal({ versoes: [{ id: 71, numeroVersao: 1, numeroContrato: 'NUMERO-RESTRITO-RPI03', anoContrato: 2098,
      dataEfetiva: null, provaCampos: { numeroContrato: restrita, anoContrato: prova('NAO_COMPROVADO'), dataEfetiva: prova('AUSENCIA_COMPROVADA') } }] });
    const secao = screen.getByRole('heading', { name: /Versões negociais/ }).closest('section');
    expect(secao).not.toHaveTextContent('NUMERO-RESTRITO-RPI03');
    expect(secao).not.toHaveTextContent('2098');
    expect(secao).toHaveTextContent(/Acesso restrito/i);
    expect(secao).toHaveTextContent(/Desconhecido|não comprovado/i);
    expect(secao).toHaveTextContent(/Ausência comprovada/i);
    expect(secao).toHaveTextContent('Versão 1');
    await waitFor(() => expect(requisitar).toHaveBeenCalled());
  });

  it('o cartão de composição não promove zero residual nem exibe rótulos restritos/desconhecidos', async () => {
    render(<PainelAlteracoesContrato contrato={contrato} />);
    const cartao = (await screen.findByText(/Cargo documentado/)).closest('article');
    expect(cartao).not.toHaveTextContent('LOTACAO-RESTRITA-RPI03');
    expect(cartao).not.toHaveTextContent('TURNO-RESIDUAL-RPI03');
    expect(cartao).not.toHaveTextContent(/Efetivo previsto:\s*0\b/);
    expect(cartao).toHaveTextContent(/Acesso restrito/i);
    expect(cartao).toHaveTextContent(/Desconhecido|não comprovado/i);
    expect(cartao).toHaveTextContent(/Ausência comprovada/i);
  });

  it('o cartão identifica zero comprovado e sua origem autorizada sem convertê-lo em ausência', async () => {
    composicoes[0].provaCampos.efetivoPrevisto = prova('VALOR_COMPROVADO');
    render(<PainelAlteracoesContrato contrato={contrato} />);
    const cartao = (await screen.findByText(/Cargo documentado/)).closest('article');
    expect(cartao).toHaveTextContent(/Efetivo previsto:.*Valor comprovado.*\b0\b/s);
    expect(cartao).toHaveTextContent(/Prova #81/);
  });

  it.each([['Aditamentos', 'Adicionar linha'], ['Remanejamentos', 'Adicionar item']])('%s descreve a comprovação nas opções de composição', async (aba, adicionar) => {
    render(<PainelAlteracoesContrato contrato={contrato} podeGerenciar />);
    await screen.findByText(/Cargo documentado/);
    fireEvent.click(screen.getByRole('tab', { name: aba }));
    fireEvent.click(screen.getByRole('button', { name: adicionar }));
    const opcao = screen.getByRole('option', { name: /Cargo documentado/ });
    expect(opcao).not.toHaveTextContent('LOTACAO-RESTRITA-RPI03');
    expect(opcao).not.toHaveTextContent('TURNO-RESIDUAL-RPI03');
    expect(opcao).toHaveTextContent(/Acesso restrito/i);
    expect(opcao).toHaveTextContent(/Desconhecido|não comprovado/i);
    expect(opcao).toHaveValue('31');
  });

  it('a origem de transferência conserva ID físico e não revela rótulo ou ID de catálogo restritos', async () => {
    composicoes[0].provaCampos.lotacaoId = restrita;
    render(<PainelExcecoesContrato contrato={contrato} eventos={[]} podeGerenciar />);
    fireEvent.click(screen.getByRole('tab', { name: 'Transferências' }));
    fireEvent.change(screen.getByLabelText(/Data efetiva da transferência/), { target: { value: '2026-11-05' } });
    fireEvent.click(screen.getByRole('button', { name: 'Adicionar mapa' }));
    const seletor = screen.getByRole('combobox', { name: 'Composição disponível na data do mapa 1' });
    const opcao = await within(seletor).findByRole('option', { name: /Cargo documentado/ });
    expect(opcao).toHaveTextContent('#31');
    expect(opcao).not.toHaveTextContent('LOTACAO-RESTRITA-RPI03');
    expect(opcao).not.toHaveTextContent('TURNO-RESIDUAL-RPI03');
    expect(opcao).toHaveTextContent(/Acesso restrito/i);
    expect(opcao).toHaveTextContent(/Desconhecido|não comprovado/i);
    fireEvent.change(seletor, { target: { value: '0' } });
    expect(screen.getByLabelText('Composição origem do mapa 1')).toHaveValue(31);
    expect(screen.getByLabelText('Lotação origem do mapa 1')).toHaveValue(null);
    expect(screen.getByLabelText('Turno origem do mapa 1')).toHaveValue(33);
  });

  it('preserva o fallback da composição legada sem fabricar um estado documental', async () => {
    const legada = composicao(); delete legada.provaCampos;
    legada.lotacaoNome = 'Lotação legada'; legada.jornadaDescricao = 'Jornada legada'; composicoes = [legada];
    render(<PainelAlteracoesContrato contrato={contrato} />);
    const cartao = (await screen.findByText(/Cargo documentado/)).closest('article');
    expect(cartao).toHaveTextContent('Lotação legada');
    const linhaEfetivo = within(cartao).getByText('Efetivo previsto:', { exact: true });
    expect(linhaEfetivo.querySelector('b')).toHaveTextContent(/^0$/);
    expect(cartao).not.toHaveTextContent(/Valor comprovado|Ausência comprovada/);
  });

  it('mantém os valores de preparação no editor do rascunho, sem exigir prova publicada para editar', async () => {
    composicoes[0].efetivoPrevisto = 6;
    composicoes[0].provaCampos = Object.fromEntries(Object.keys(composicoes[0].provaCampos).map(key => [key, prova('NAO_COMPROVADO')]));
    render(<PainelAlteracoesContrato contrato={{ ...contrato, estadoNegocial: 'RASCUNHO' }} podeGerenciar />);
    fireEvent.click(await screen.findByRole('button', { name: 'Editar', exact: true }));
    expect(screen.getByRole('heading', { name: 'Editar composição do rascunho' })).toBeInTheDocument();
    expect(screen.getByLabelText(/Efetivo previsto/)).toHaveValue(6);
  });
});
