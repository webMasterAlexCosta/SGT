import React from 'react';
import { cleanup, fireEvent, render, screen } from '@testing-library/react';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import '@testing-library/jest-dom/vitest';

const observador = vi.hoisted(() => ({ modelo: null, criar: vi.fn(), cancelar: vi.fn(), reprocessar: vi.fn(), atualizar: vi.fn() }));
vi.mock('../src/modules/rh/pages/PaginaFrequencia/useLogicaFrequencia.ts', () => ({ useLogicaFrequencia: () => observador.modelo }));
vi.mock('../src/modules/rh/pages/PaginaFrequencia/components/CabecalhoFrequencia.tsx', () => ({ CabecalhoFrequencia: () => null }));
vi.mock('../src/modules/rh/pages/PaginaFrequencia/components/FiltrosFrequencia.tsx', () => ({ FiltrosFrequencia: () => null }));
vi.mock('../src/modules/rh/pages/PaginaFrequencia/components/TabelaFrequencia.tsx', () => ({ TabelaFrequencia: () => null }));
vi.mock('../src/modules/rh/pages/PaginaFrequencia/components/LancamentoPorVinculo.tsx', () => ({ LancamentoPorVinculo: () => null }));
vi.mock('../src/modules/rh/pages/PaginaFrequencia/components/TourGuiado.tsx', () => ({ TourGuiado: () => null }));
vi.mock('../src/modules/rh/pages/PaginaFrequencia/components/ModalParcial.tsx', () => ({ ModalParcial: () => null }));
import { PaginaFrequencia } from '../src/modules/rh/pages/PaginaFrequencia/index.tsx';
import { PainelFechamentoFrequencia } from '../src/modules/rh/pages/PaginaFrequencia/components/PainelFechamentoFrequencia.tsx';

const props = () => ({ ativo: false, carregando: false, erro: '', podeGerenciar: true, dataValida: true,
  filtrosProntos: true, possuiRascunhos: false, aoCriar: observador.criar, aoCancelar: observador.cancelar,
  aoReprocessar: observador.reprocessar, aoAtualizar: observador.atualizar });
beforeEach(() => {
  observador.criar.mockReset(); observador.cancelar.mockReset(); observador.reprocessar.mockReset(); observador.atualizar.mockReset();
  observador.modelo = {
    estado: { dataReferenciaAtiva: '2026-11-06', statusDaDataValidada: { valid: true, reason: '' },
      listaDaFrequencia: [], listaDaFrequenciaFiltradaNaMemoria: [], estadoDoModalParcial: {} },
    filtros: { lotacaoSelecionadaPeloUsuario: '46', turnoSelecionadoPeloUsuario: '730' },
    acoes: { solicitarCriacaoDoFechamento: observador.criar, solicitarCancelamentoDoFechamento: observador.cancelar,
      solicitarReprocessamentoDoFechamento: observador.reprocessar, recuperarFechamentoAtual: observador.atualizar },
    auth: { podeRealizarFrequencias: true }, utilitarios: {}, ModalConfirmacao: () => null,
  };
});
afterEach(cleanup);

describe('fechamento usa a validade de data já resolvida pela página', () => {
  it('desabilita Iniciar e não dispara criação se a data estiver inválida', () => {
    render(<PainelFechamentoFrequencia {...props()} dataValida={false} />);
    const iniciar = screen.getByRole('button', { name: 'Iniciar fechamento' });
    expect(iniciar).toBeDisabled(); fireEvent.click(iniciar); expect(observador.criar).not.toHaveBeenCalled();
  });

  it('propaga a recusa de data futura da validação existente da página', () => {
    observador.modelo.estado.dataReferenciaAtiva = '2026-11-07';
    observador.modelo.estado.statusDaDataValidada = { valid: false, reason: 'future' };
    render(<PaginaFrequencia />); const iniciar = screen.getByRole('button', { name: 'Iniciar fechamento' });
    expect(iniciar).toBeDisabled(); fireEvent.click(iniciar); expect(observador.criar).not.toHaveBeenCalled();
  });

  it.each(['', null])('não oferece criação sem data de referência: %s', data => {
    observador.modelo.estado.dataReferenciaAtiva = data;
    render(<PaginaFrequencia />); expect(screen.getByRole('button', { name: 'Iniciar fechamento' })).toBeDisabled();
    expect(observador.criar).not.toHaveBeenCalled();
  });

  it.each(['2026-11-06', '2026-09-01'])('data validada permite a intenção sem calcular uma janela local: %s', data => {
    observador.modelo.estado.dataReferenciaAtiva = data;
    render(<PaginaFrequencia />); const iniciar = screen.getByRole('button', { name: 'Iniciar fechamento' });
    expect(iniciar).toBeEnabled(); fireEvent.click(iniciar); expect(observador.criar).toHaveBeenCalledTimes(1);
  });

  it('ao invalidar a data preserva lotação/jornada selecionadas e bloqueia nova criação', () => {
    const view = render(<PaginaFrequencia />);
    observador.modelo.estado.statusDaDataValidada = { valid: false, reason: 'future' };
    view.rerender(<PaginaFrequencia />);
    expect(screen.getByRole('button', { name: 'Iniciar fechamento' })).toBeDisabled();
    expect(observador.modelo.filtros).toMatchObject({ lotacaoSelecionadaPeloUsuario: '46', turnoSelecionadoPeloUsuario: '730' });
    expect(observador.criar).not.toHaveBeenCalled();
  });

  it.each([{ carregando: true }, { possuiRascunhos: true }])('preserva impedimentos anteriores de criação: %j', impedimento => {
    render(<PainelFechamentoFrequencia {...props()} {...impedimento} />);
    expect(screen.getByRole('button', { name: 'Iniciar fechamento' })).toBeDisabled();
  });

  it('preserva permissão: ator sem gestão não recebe botão de criação', () => {
    render(<PainelFechamentoFrequencia {...props()} podeGerenciar={false} />);
    expect(screen.queryByRole('button', { name: 'Iniciar fechamento' })).not.toBeInTheDocument();
  });

  it('preserva exigência de filtros sem apagar a seleção', () => {
    render(<PainelFechamentoFrequencia {...props()} filtrosProntos={false} />);
    expect(screen.queryByRole('button', { name: 'Iniciar fechamento' })).not.toBeInTheDocument();
  });

  it('data inválida não bloqueia consulta ou cancelamento de processamento já ativo', () => {
    render(<PainelFechamentoFrequencia {...props()} dataValida={false} ativo fechamento={{ id: 12, status: 'PROCESSANDO' }} />);
    expect(screen.queryByRole('button', { name: 'Iniciar fechamento' })).not.toBeInTheDocument();
    fireEvent.click(screen.getByRole('button', { name: 'Atualizar' })); fireEvent.click(screen.getByRole('button', { name: 'Solicitar cancelamento' }));
    expect(observador.atualizar).toHaveBeenCalledTimes(1); expect(observador.cancelar).toHaveBeenCalledTimes(1);
  });

  it('preserva a ação sobre job terminal sem reclassificar a validade do job pelos filtros atuais', () => {
    render(<PainelFechamentoFrequencia {...props()} dataValida={false} fechamento={{ id: 12, status: 'ERRO' }} />);
    fireEvent.click(screen.getByRole('button', { name: 'Reprocessar' })); expect(observador.reprocessar).toHaveBeenCalledTimes(1);
    expect(screen.queryByRole('button', { name: 'Iniciar fechamento' })).not.toBeInTheDocument();
  });
});
