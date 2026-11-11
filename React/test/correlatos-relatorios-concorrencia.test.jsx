import React from 'react';
import { act, cleanup, fireEvent, render, screen, waitFor } from '@testing-library/react';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import '@testing-library/jest-dom/vitest';

const { requisitar, notificar, iniciarExportacao } = vi.hoisted(() => ({ requisitar: vi.fn(), notificar: vi.fn(), iniciarExportacao: vi.fn() }));
vi.mock('../src/core/http.ts', async (original) => ({ ...(await original()), requisitar }));
vi.mock('../src/core/events.ts', () => ({ notificar }));
vi.mock('../src/core/exportacao/context.tsx', () => ({ useExportacaoRelatorio: () => ({ iniciarExportacao }) }));
vi.mock('recharts', () => Object.fromEntries(['PieChart', 'Pie', 'Cell', 'BarChart', 'Bar', 'XAxis', 'YAxis', 'CartesianGrid', 'Tooltip', 'Legend', 'ResponsiveContainer', 'LabelList'].map(name => [name, () => null])));

import { PaginaRelatorioFrequencia } from '../src/modules/rh/pages/PaginaRelatorioFrequencia/index.tsx';
import { PaginaRelatorioAbsenteismo } from '../src/modules/rh/pages/PaginaRelatorioAbsenteismo/index.tsx';
import { validarFiltrosAbsenteismo } from '../src/services/rh/ServicoAbsenteismo.ts';
import { invalidarGeracaoSessao } from '../src/core/security/geracaoSessao.ts';

const adiar = () => { let resolve, reject; const promise = new Promise((ok, falha) => { resolve = ok; reject = falha; }); return { promise, resolve, reject }; };
const filtros = {
  contratos: [{ id: 1, numeroContrato: 'C-1', descricao: 'Contrato Alfa', ativo: true }, { id: 2, numeroContrato: 'C-2', descricao: 'Contrato Histórico', ativo: false }],
  lotacoes: [{ id: 7, nome: 'Lotação Norte', turnos: [{ id: 8, descricao: 'Manhã' }] }, { id: 9, nome: 'Lotação Sul', turnos: [{ id: 10, descricao: 'Noite' }] }],
  turnos: [{ id: 8, descricao: 'Manhã' }, { id: 10, descricao: 'Noite' }],
};
const pagina = (prefixo = 'Pessoa atual', revisao = null) => ({
  completo: true, pagina: 0, tamanho: 25, totalPaginas: 1, totalColaboradores: 7, primeira: true, ultima: true, revisao,
  registros: Array.from({ length: 7 }, (_, i) => ({ empregadoId: i + 1, nome: `${prefixo} ${i + 1}`, chaveAlocacao: `${i + 1}-contexto`, cargo: 'Cargo', lotacao: 'Lotação', turno: 'Turno', diario: {}, totais: {}, diasTrabalhaveis: 20, faltas: 0, atestados: 0, dsrPerdido: 0, taxaIndividual: 0, disrupcao: 0 })),
});
const resumo = (revisao = null) => ({ totalColaboradores: 7, taxaGeral: 8.75, diasTrabalhaveis: 140, faltas: 2, atestados: 1, dsrPerdido: 0, alertasDisrupcao: 0, maioresTaxas: [], revisao });
const aviso = (mensagem) => ({ sujeitoARevisao: true, quantidadePendencias: 1, mensagem });
const paginaUrl = (tipo) => tipo === 'frequencia' ? '/relatorios/frequencia-consolidada/paginada' : '/relatorios/absenteismo/paginado';
const elemento = (tipo) => tipo === 'frequencia' ? <PaginaRelatorioFrequencia /> : <PaginaRelatorioAbsenteismo />;
const botaoGerar = (tipo) => screen.getByRole('button', { name: tipo === 'frequencia' ? 'Gerar Relatório' : 'CALCULAR' });
const botaoExcel = () => screen.getByRole('button', { name: /Planilha.*XLSX|Excel/i });
const mudarLotacao = () => fireEvent.change(screen.getByLabelText('Lotação', { exact: true }), { target: { value: '7' } });
let carregarPagina, carregarResumo, carregarFiltros;

describe('Competência administrativa nos limites de mês e ano', () => {
  it.each([
    ['2024-03-01T01:30:00Z', 2024, 2, '2024-02-01', '2024-02-29'],
    ['2026-01-01T01:30:00Z', 2025, 12, '2025-12-01', '2025-12-31'],
  ])('abre os filtros civis em São Paulo para %s', async (instante, ano, mes, inicio, fim) => {
    vi.setSystemTime(new Date(instante));
    const frequencia = await abrir('frequencia');
    expect(screen.getByLabelText('Início', { exact: true })).toHaveValue(inicio);
    expect(screen.getByLabelText('Fim', { exact: true })).toHaveValue(fim);
    frequencia.unmount();
    await abrir('absenteismo');
    expect(screen.getByLabelText('Ano da competência')).toHaveValue(ano);
    expect(screen.getByLabelText('Mês da competência')).toHaveValue(String(mes));
  });

  it.each([
    ['2024-03-01T01:30:00Z', 2024, 3, false],
    ['2024-03-01T03:30:00Z', 2024, 3, true],
    ['2026-01-01T01:30:00Z', 2026, 1, false],
    ['2026-01-01T03:30:00Z', 2026, 1, true],
  ])('valida a competência %s com a mesma referência administrativa', (instante, ano, mes, valido) => {
    const resultado = validarFiltrosAbsenteismo({ ano, mes, referencia: new Date(instante) });
    expect(resultado.valido).toBe(valido);
    expect(resultado.codigo).toBe(valido ? null : 'COMPETENCIA_FUTURA');
  });
});

beforeEach(() => {
  // O cenário precisa mudar o filtro; a data real da execução não pode coincidir com o valor escolhido.
  vi.useFakeTimers({ toFake: ['Date'] });
  vi.setSystemTime(new Date('2026-11-01T12:00:00Z'));
  invalidarGeracaoSessao(); requisitar.mockReset(); notificar.mockReset(); iniciarExportacao.mockReset();
  carregarPagina = async () => pagina(); carregarResumo = async () => resumo(); carregarFiltros = async () => filtros;
  requisitar.mockImplementation(async (pedido) => {
    if (pedido.url === '/relatorios/filtros') return carregarFiltros(pedido);
    if (pedido.url === '/relatorios/absenteismo/resumo') return carregarResumo(pedido);
    if (pedido.url.endsWith('/paginada') || pedido.url.endsWith('/paginado')) return carregarPagina(pedido);
    throw new Error('Consulta imprevista: ' + pedido.url);
  });
  vi.spyOn(console, 'error').mockImplementation(() => {});
});
afterEach(() => { cleanup(); vi.restoreAllMocks(); vi.useRealTimers(); invalidarGeracaoSessao(); });

async function abrir(tipo) {
  const montado = render(elemento(tipo));
  await screen.findByRole('option', { name: 'Lotação Norte' });
  return montado;
}
async function gerar(tipo) {
  fireEvent.click(botaoGerar(tipo));
  await waitFor(() => expect(requisitar.mock.calls.some(([r]) => r.url === paginaUrl(tipo))).toBe(true));
}

for (const tipo of ['frequencia', 'absenteismo']) {
  describe(`Consulta vigente do relatório de ${tipo}`, () => {
    it('descarta página antiga após mudança de filtro, inclusive revisão e exportação', async () => {
      const antiga = adiar(); carregarPagina = () => antiga.promise;
      await abrir(tipo); await gerar(tipo); mudarLotacao();
      await act(async () => antiga.resolve(pagina('Pessoa antiga', aviso('Revisão de filtro antigo'))));
      expect(screen.queryByText('Pessoa antiga 1')).not.toBeInTheDocument();
      expect(screen.queryByText('Revisão de filtro antigo')).not.toBeInTheDocument();
      expect(botaoExcel()).toBeDisabled();
      expect(notificar).not.toHaveBeenCalled();
    });

    it('não publica erro tardio depois de mudar o filtro', async () => {
      const antiga = adiar(); carregarPagina = () => antiga.promise;
      await abrir(tipo); await gerar(tipo); mudarLotacao();
      await act(async () => antiga.reject(new Error('Falha da consulta anterior')));
      expect(notificar).not.toHaveBeenCalled();
      expect(botaoGerar(tipo)).toBeEnabled();
      expect(botaoExcel()).toBeDisabled();
    });

    it('libera geração do filtro atual e conserva o sucesso novo após erro antigo', async () => {
      const antiga = adiar();
      carregarPagina = ({ params }) => params.lotacaoId ? pagina('Pessoa nova') : antiga.promise;
      await abrir(tipo); await gerar(tipo); mudarLotacao();
      expect(botaoGerar(tipo)).toBeEnabled();
      await gerar(tipo); await screen.findByText('Pessoa nova 1');
      notificar.mockClear();
      await act(async () => antiga.reject(new Error('Falha antiga')));
      expect(screen.getByText('Pessoa nova 1')).toBeInTheDocument();
      expect(botaoExcel()).toBeEnabled();
      expect(notificar).not.toHaveBeenCalled();
    });

    it('não notifica erro de página depois de desmontar', async () => {
      const antiga = adiar(); carregarPagina = () => antiga.promise;
      const montado = await abrir(tipo); await gerar(tipo); montado.unmount();
      await act(async () => antiga.reject(new Error('Página encerrada')));
      expect(notificar).not.toHaveBeenCalled();
    });

    it('não publica página de geração de sessão anterior', async () => {
      const antiga = adiar(); carregarPagina = () => antiga.promise;
      const montado = await abrir(tipo); await gerar(tipo);
      act(() => { invalidarGeracaoSessao(); }); montado.rerender(elemento(tipo));
      await act(async () => antiga.resolve(pagina('Pessoa da sessão anterior', aviso('Revisão de outra sessão'))));
      expect(screen.queryByText('Pessoa da sessão anterior 1')).not.toBeInTheDocument();
      expect(screen.queryByText('Revisão de outra sessão')).not.toBeInTheDocument();
      expect(botaoExcel()).toBeDisabled();
      expect(notificar).not.toHaveBeenCalled();
    });

    it('não publica falha da carga inicial depois de desmontar', async () => {
      const antiga = adiar(); carregarFiltros = () => antiga.promise;
      const montado = render(elemento(tipo));
      await waitFor(() => expect(requisitar).toHaveBeenCalled()); montado.unmount();
      await act(async () => antiga.reject(new Error('Catálogos antigos')));
      expect(notificar).not.toHaveBeenCalled();
    });

    it('não instala opções da carga inicial de outra sessão', async () => {
      const antiga = adiar(); let inicial = true;
      carregarFiltros = () => { if (inicial) { inicial = false; return antiga.promise; } return filtros; };
      const montado = render(elemento(tipo));
      await waitFor(() => expect(requisitar).toHaveBeenCalled());
      act(() => { invalidarGeracaoSessao(); }); montado.rerender(elemento(tipo));
      await act(async () => antiga.resolve({ contratos: [], lotacoes: [{ id: 99, nome: 'Lotação da sessão antiga' }], turnos: [] }));
      expect(screen.queryByRole('option', { name: 'Lotação da sessão antiga' })).not.toBeInTheDocument();
      expect(await screen.findByRole('option', { name: 'Lotação Norte' })).toBeInTheDocument();
    });

    const campos = tipo === 'frequencia' ? ['inicio', 'fim', 'contrato', 'lotacao', 'turno'] : ['mes', 'ano', 'contrato', 'lotacao', 'turno'];
    it.each(campos)('limpa dados, revisão e autorização de exportação ao mudar %s', async (campo) => {
      carregarPagina = async () => pagina('Pessoa atual', aviso('Revisão do recorte carregado'));
      carregarResumo = async () => resumo(aviso('Revisão do recorte carregado'));
      await abrir(tipo); await gerar(tipo); await screen.findByText('Pessoa atual 1');
      expect(botaoExcel()).toBeEnabled();
      if (campo === 'contrato' && tipo === 'frequencia') {
        fireEvent.click(screen.getByRole('button', { name: /Todos os contratos autorizados/ }));
        fireEvent.click(screen.getByRole('checkbox', { name: 'Contrato Alfa' }));
      } else {
        const [label, valor] = {
          inicio: ['Início', '2026-01-02'], fim: ['Fim', '2026-11-02'], mes: ['Mês da competência', '1'], ano: ['Ano da competência', '2025'],
          contrato: ['Contrato', '1'], lotacao: ['Lotação', '7'], turno: ['Turno', '8'],
        }[campo];
        const entrada = screen.getByLabelText(label, { exact: true });
        expect(entrada.value).not.toBe(valor);
        fireEvent.change(entrada, { target: { value: valor } });
      }
      expect(screen.queryByText('Pessoa atual 1')).not.toBeInTheDocument();
      expect(screen.queryByText('Revisão do recorte carregado')).not.toBeInTheDocument();
      expect(botaoExcel()).toBeDisabled();
    });
  });
}

describe('Resumo de absenteísmo associado ao recorte, independente da página visual', () => {
  it.each(['sucesso', 'erro'])('descarta %s do resumo após mudança de filtro', async (desfecho) => {
    const antigo = adiar(); carregarResumo = () => antigo.promise;
    await abrir('absenteismo'); await gerar('absenteismo'); await screen.findByText('Pessoa atual 1');
    await waitFor(() => expect(requisitar.mock.calls.some(([r]) => r.url.endsWith('/resumo'))).toBe(true));
    mudarLotacao(); notificar.mockClear();
    await act(async () => desfecho === 'sucesso' ? antigo.resolve(resumo(aviso('Revisão do resumo antigo'))) : antigo.reject(new Error('Resumo antigo')));
    expect(screen.queryByText('Revisão do resumo antigo')).not.toBeInTheDocument();
    expect(screen.queryByText('8.75%')).not.toBeInTheDocument();
    expect(notificar).not.toHaveBeenCalled();
    expect(botaoExcel()).toBeDisabled();
  });

  it.each(['desmontagem', 'sessao'])('descarta erro de resumo após %s', async (fronteira) => {
    const antigo = adiar(); carregarResumo = () => antigo.promise;
    const montado = await abrir('absenteismo'); await gerar('absenteismo'); await screen.findByText('Pessoa atual 1');
    await waitFor(() => expect(requisitar.mock.calls.some(([r]) => r.url.endsWith('/resumo'))).toBe(true));
    if (fronteira === 'desmontagem') montado.unmount();
    else { act(() => { invalidarGeracaoSessao(); }); montado.rerender(elemento('absenteismo')); }
    notificar.mockClear();
    await act(async () => antigo.reject(new Error('Resumo da sessão anterior')));
    expect(notificar).not.toHaveBeenCalled();
  });

  it('aceita resumo pendente do mesmo recorte após avançar a página, sem recalculá-lo', async () => {
    const pendente = adiar(); carregarResumo = () => pendente.promise;
    await abrir('absenteismo'); await gerar('absenteismo'); await screen.findByText('Pessoa atual 1');
    expect(screen.getByLabelText(/por página/)).toHaveValue('5');
    fireEvent.click(screen.getByRole('button', { name: 'Próxima', exact: true }));
    await screen.findByText('Pessoa atual 6');
    await act(async () => pendente.resolve(resumo()));
    expect(screen.getByText('8.75%')).toBeInTheDocument();
    expect(screen.getByText('Pessoa atual 6')).toBeInTheDocument();
    expect(requisitar.mock.calls.filter(([r]) => r.url.endsWith('/resumo'))).toHaveLength(1);
    expect([...screen.getAllByRole('table')[0].tBodies[0].children]).toHaveLength(2);
  });
});
