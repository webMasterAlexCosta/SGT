import React from 'react';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { act, cleanup, fireEvent, render, screen } from '@testing-library/react';
import '@testing-library/jest-dom/vitest';
import { ProvedorExportacaoRelatorio, useExportacaoRelatorio } from '../src/core/exportacao/context.tsx';
import { invalidarGeracaoSessao } from '../src/core/security/geracaoSessao.ts';
const { servico } = vi.hoisted(() => ({ servico: {
  solicitarFrequencia: vi.fn(), solicitarAbsenteismo: vi.fn(), solicitarTerceirizados: vi.fn(),
  consultar: vi.fn(), cancelar: vi.fn(), obterUrlDownload: id => `/arquivo-sintetico/${id}`,
} }));
vi.mock('../src/core/exportacao/ServicoExportacaoRelatorio', () => ({ ServicoExportacaoRelatorio: servico }));
let contexto;
function Observador() { contexto = useExportacaoRelatorio(); return null; }
const arvore = key => <ProvedorExportacaoRelatorio key={key}><Observador /></ProvedorExportacaoRelatorio>;
const diferida = () => { let resolve, reject; const promise = new Promise((a, b) => { resolve = a; reject = b; }); return { promise, resolve, reject }; };
const tarefa = (status = 'PROCESSANDO') => ({ id: 'exportacao-sintetica', relatorio: 'Relatório sintético', status, podeBaixar: status === 'CONCLUIDO', etapa: status, falhou: false });
beforeEach(() => {
  vi.useFakeTimers(); invalidarGeracaoSessao(); sessionStorage.clear();
  servico.solicitarFrequencia.mockReset().mockResolvedValue(tarefa());
  servico.consultar.mockReset().mockResolvedValue(tarefa()); servico.cancelar.mockReset();
});
afterEach(() => { cleanup(); document.querySelectorAll('iframe').forEach(el => el.remove()); vi.useRealTimers(); });
async function iniciar() { await act(async () => { await contexto.iniciarExportacao({ tipo: 'frequencia' }); }); }
describe('Cancelamento de exportação confirmado pelo servidor', () => {
  it.each([4,6])('acompanha e mantém canceláveis todas as %i exportações distintas ainda ativas', async quantidade => {
    const porId = id => ({ ...tarefa(), id, relatorio: `Lote ${id}` });
    servico.solicitarFrequencia.mockImplementation(({ indice }) => Promise.resolve(porId(`ativa-${indice}`)));
    servico.consultar.mockImplementation(id => Promise.resolve(porId(id)));
    servico.cancelar.mockImplementation(id => Promise.resolve({ ...porId(id), status:'CANCELADO', podeBaixar:false }));
    render(arvore('A'));
    for(let indice=0;indice<quantidade;indice++) await act(async()=>{await contexto.iniciarExportacao({tipo:'frequencia',parametros:{indice}});});
    expect(contexto.tarefas).toHaveLength(quantidade);
    expect(screen.getAllByRole('button',{name:'Cancelar exportação',exact:true})).toHaveLength(quantidade);
    expect(JSON.parse(sessionStorage.getItem('sgt_exportacoes_relatorios'))).toHaveLength(quantidade);
    await act(async()=>{await vi.advanceTimersByTimeAsync(1500);});
    expect(new Set(servico.consultar.mock.calls.map(([id])=>id))).toEqual(new Set(Array.from({length:quantidade},(_,i)=>`ativa-${i}`)));
    await act(async()=>{await contexto.cancelarExportacao('ativa-0');});
    expect(servico.cancelar).toHaveBeenCalledWith('ativa-0',expect.objectContaining({signal:expect.any(AbortSignal)}));
    expect(contexto.tarefas.find(t=>t.id==='ativa-0').status).toBe('CANCELADO');
  });
  it('limita somente o histórico terminal, preservando tarefa ativa mais antiga', async () => {
    servico.solicitarFrequencia.mockImplementation(({ indice }) => Promise.resolve({ ...tarefa(indice===0?'PROCESSANDO':'CANCELADO'),id:`lote-${indice}` }));
    render(arvore('A'));
    for(let indice=0;indice<8;indice++)await act(async()=>{await contexto.iniciarExportacao({tipo:'frequencia',parametros:{indice}});});
    expect(contexto.tarefas.find(t=>t.id==='lote-0')?.status).toBe('PROCESSANDO');
    expect(contexto.tarefas.filter(t=>t.status==='CANCELADO')).toHaveLength(5);
    expect(contexto.tarefas).toHaveLength(6);
  });
  it('restaura todas as seis tarefas ainda ativas após recarregar a interface', async () => {
    const ids=Array.from({length:6},(_,i)=>`restaurada-${i}`);
    sessionStorage.setItem('sgt_exportacoes_relatorios',JSON.stringify(ids));
    servico.consultar.mockImplementation(id=>Promise.resolve({...tarefa(),id}));
    await act(async()=>{render(arvore('A'));});
    expect(new Set(contexto.tarefas.map(t=>t.id))).toEqual(new Set(ids));
    expect(screen.getAllByRole('button',{name:'Cancelar exportação',exact:true})).toHaveLength(6);
  });
  it('não cancela otimisticamente, deduplica POST e ignora GET antigo que permitiria download', async () => {
    const consulta = diferida(), cancelamento = diferida();
    servico.consultar.mockReturnValueOnce(consulta.promise); servico.cancelar.mockReturnValueOnce(cancelamento.promise);
    render(arvore('A')); await iniciar();
    await act(async () => { await vi.advanceTimersByTimeAsync(1500); });
    let p1, p2;
    act(() => { p1 = contexto.cancelarExportacao('exportacao-sintetica'); p2 = contexto.cancelarExportacao('exportacao-sintetica'); });
    expect(contexto.tarefas[0].status).toBe('PROCESSANDO'); expect(servico.cancelar).toHaveBeenCalledTimes(1);
    expect(screen.getByRole('button', { name: 'Cancelando exportação…' })).toBeDisabled();
    await act(async () => { cancelamento.resolve(tarefa('CANCELADO')); await Promise.all([p1, p2]); });
    await act(async () => { consulta.resolve(tarefa('CONCLUIDO')); await Promise.resolve(); await vi.advanceTimersByTimeAsync(5000); });
    expect(contexto.tarefas[0].status).toBe('CANCELADO'); expect(document.querySelectorAll('iframe')).toHaveLength(0);
    expect(servico.consultar).toHaveBeenCalledTimes(1); expect(screen.getByText('Cancelada')).toBeInTheDocument();
    expect(screen.queryByLabelText(/Progresso/)).not.toBeInTheDocument();
    fireEvent.click(screen.getByRole('button', { name: 'Fechar aviso de exportação' })); expect(contexto.tarefas).toEqual([]);
  });
  it.each([403, 409])('recusa %i preserva tarefa, apresenta recuperação e não repete a mutação', async status => {
    servico.cancelar.mockRejectedValue({ status, message: 'Cancelamento recusado no estado atual.' });
    render(arvore('A')); await iniciar();
    await act(async () => { await contexto.cancelarExportacao('exportacao-sintetica'); });
    expect(contexto.tarefas[0].status).toBe('PROCESSANDO'); expect(servico.cancelar).toHaveBeenCalledTimes(1);
    expect(screen.getByRole('alert')).toHaveTextContent('Cancelamento recusado no estado atual.');
    expect(screen.getByRole('button', { name: 'Cancelar exportação', exact: true })).toBeEnabled();
    expect(JSON.parse(sessionStorage.getItem('sgt_exportacoes_relatorios'))).toEqual(['exportacao-sintetica']);
  });
  it('resposta de cancelamento de A não publica tarefas na sessão B', async () => {
    const cancelamento = diferida(); servico.cancelar.mockReturnValueOnce(cancelamento.promise);
    const view = render(arvore('A')); await iniciar(); let pedido;
    act(() => { pedido = contexto.cancelarExportacao('exportacao-sintetica'); invalidarGeracaoSessao(); });
    view.rerender(arvore('B'));
    await act(async () => { cancelamento.resolve(tarefa('CANCELADO')); await pedido; });
    expect(contexto.tarefas).toEqual([]); expect(document.querySelectorAll('iframe')).toHaveLength(0);
    expect(screen.queryByRole('alert')).not.toBeInTheDocument();
  });
});
