import React from 'react';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { act, cleanup, render } from '@testing-library/react';
import { ProvedorExportacaoRelatorio, useExportacaoRelatorio } from '../src/core/exportacao/context.jsx';
import { invalidarGeracaoSessao } from '../src/core/security/geracaoSessao.js';
const { servico } = vi.hoisted(() => ({ servico: { solicitarFrequencia: vi.fn(), solicitarAbsenteismo: vi.fn(), solicitarTerceirizados: vi.fn(), consultar: vi.fn(), obterUrlDownload: id => `/arquivo-sintetico/${id}` } }));
vi.mock('../src/core/exportacao/ServicoExportacaoRelatorio', () => ({ ServicoExportacaoRelatorio: servico }));
vi.mock('../src/core/exportacao/PainelExportacoes', () => ({ PainelExportacoes: () => null }));
let contexto;
const Observador = () => { contexto = useExportacaoRelatorio(); return <output>{contexto.tarefas.map(t => t.id).join(',')}</output>; };
const arvore = key => <ProvedorExportacaoRelatorio key={key}><Observador /></ProvedorExportacaoRelatorio>;
const diferida = () => { let resolve, reject; const promise = new Promise((a,b) => { resolve=a; reject=b; }); return { promise, resolve, reject }; };
const job = id => ({ id, status: 'CONCLUIDO', podeBaixar: true });
const iniciar = () => { let p; act(() => { p = contexto.iniciarExportacao({ tipo: 'frequencia' }).then(value => ({ value }), error => ({ error })); }); return p; };
beforeEach(() => { vi.useFakeTimers(); invalidarGeracaoSessao(); sessionStorage.clear(); servico.solicitarFrequencia.mockReset(); servico.consultar.mockReset().mockResolvedValue(null); });
afterEach(() => { cleanup(); document.querySelectorAll('iframe[title^="download-relatorio-"]').forEach(el => el.remove()); vi.useRealTimers(); });
describe('Exportações encerram junto com o contexto de sessão', () => {
  it.each(['sucesso', 'erro'])('resposta antiga de %s não restaura tarefa A, nem grava ID, nem cria download sob B', async tipo => {
    const antiga = diferida(); servico.solicitarFrequencia.mockReturnValueOnce(antiga.promise);
    const view = render(arvore('A')); const p = iniciar();
    act(() => invalidarGeracaoSessao()); view.rerender(arvore('B'));
    await act(async () => { if (tipo === 'sucesso') antiga.resolve(job('job-a-sintetico')); else antiga.reject({ status: 500 }); await p; });
    await act(async () => { await vi.advanceTimersByTimeAsync(550); });
    expect(contexto.tarefas).toEqual([]); expect(JSON.parse(sessionStorage.getItem('sgt_exportacoes_relatorios') || '[]')).toEqual([]);
    expect(document.querySelectorAll('iframe')).toHaveLength(0);
  });
  it('timer pendente não inicia download depois da mudança de sessão', async () => {
    servico.solicitarFrequencia.mockResolvedValue(job('job-a-sintetico')); render(arvore('A')); await act(async () => { await iniciar(); });
    act(() => invalidarGeracaoSessao()); await act(async () => { await vi.advanceTimersByTimeAsync(550); });
    expect(document.querySelectorAll('iframe')).toHaveLength(0); expect(contexto.tarefas).toEqual([]);
  });
  it('iframe de download existente é removido no logout', async () => {
    servico.solicitarFrequencia.mockResolvedValue(job('job-a-sintetico')); render(arvore('A')); await act(async () => { await iniciar(); });
    await act(async () => { await vi.advanceTimersByTimeAsync(550); }); expect(document.querySelectorAll('iframe')).toHaveLength(1);
    act(() => invalidarGeracaoSessao()); expect(document.querySelectorAll('iframe')).toHaveLength(0);
    expect(sessionStorage.getItem('sgt_exportacoes_relatorios')).toBeNull();
  });
  it('desmontagem cancela timer ainda pendente', async () => {
    servico.solicitarFrequencia.mockResolvedValue(job('job-a-sintetico')); const view = render(arvore('A')); await act(async () => { await iniciar(); });
    view.unmount(); await act(async () => { await vi.advanceTimersByTimeAsync(550); });
    expect(document.querySelectorAll('iframe')).toHaveLength(0);
  });
  it('sessão B não restaura IDs do painel encerrado e aceita sua própria exportação', async () => {
    servico.solicitarFrequencia.mockResolvedValueOnce(job('job-a-sintetico')).mockResolvedValueOnce(job('job-b-sintetico'));
    const view = render(arvore('A')); await act(async () => { await iniciar(); });
    act(() => invalidarGeracaoSessao()); view.rerender(arvore('B'));
    expect(contexto.tarefas).toEqual([]); expect(servico.consultar).not.toHaveBeenCalled();
    await act(async () => { await iniciar(); }); await act(async () => { await vi.advanceTimersByTimeAsync(550); });
    expect(contexto.tarefas.map(t => t.id)).toEqual(['job-b-sintetico']); expect(document.querySelectorAll('iframe')).toHaveLength(1);
    expect(document.querySelector('iframe').getAttribute('src')).toBe('/arquivo-sintetico/job-b-sintetico');
  });
});
