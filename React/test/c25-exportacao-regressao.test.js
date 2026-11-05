import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';

// Exportadores, contexto de sessão e notificações reais. O desenho PDF e o
// download são fronteiras externas instrumentadas; os dados são sintéticos.
let gate, release, started, rejectImport, saves, notifications, onToast;
const summary = { efetivoPrevisto: 10, efetivoAlocado: 10, registrosLancados: 10, presentes: 10, ausentes: 0, outrosRegistros: 0, pendentesLancamento: 0, deficitContratual: 0, excedenteContratual: 0, percentualCobertura: 100, percentualAtendimento: 100 };
const dataBase = { inicio: '2024-02-16', fim: '2024-03-01', periodo: 'QUINZENAL', resumoAtual: summary, resumoAnoAnterior: summary, serieAtual: [], serieAnoAnterior: [], detalhamentoContratos: [], detalhamentoLotacoes: [], detalhamentoTurnos: [], detalhamentoCargos: [], quadroDimensionado: [] };
const exportContext = { referencia: '2024-03-01', periodoRotulo: 'Quinzenal', comparar: true, contratoNome: 'Contrato C25', escopoSelecionado: 'Escopo C25' };

beforeEach(() => {
  vi.resetModules();
  gate = new Promise(resolve => { release = resolve; });
  started = false;
  rejectImport = false;
  saves = [];
  notifications = [];
  onToast = event => notifications.push(event.detail);
  window.addEventListener('app-show-toast', onToast);
  vi.spyOn(console, 'error').mockImplementation(() => {});
  vi.doMock('jspdf', async () => {
    started = true;
    await gate;
    if (rejectImport) throw new Error('Falha sintética no carregamento da biblioteca PDF');
    return { default: class {
      constructor() {
        this.internal = { pageSize: { getWidth: () => 842, getHeight: () => 595 } };
        return new Proxy(this, { get(target, key) { return key in target ? target[key] : () => {}; } });
      }
      getNumberOfPages() { return 1; }
      splitTextToSize(text) { return [text]; }
      save(name) { saves.push(name); }
    } };
  });
  vi.doMock('jspdf-autotable', () => ({ default: doc => { doc.lastAutoTable = { finalY: 100 }; } }));
});
afterEach(() => {
  release();
  window.removeEventListener('app-show-toast', onToast);
  vi.doUnmock('jspdf');
  vi.doUnmock('jspdf-autotable');
  vi.restoreAllMocks();
});

async function beginPdf(kind) {
  const session = await import('@sgt/core/security/geracaoSessao.js');
  const module = kind === 'indicador'
    ? await import('@sgt/modules/painel/components/IndicadorEfetivo/exportacao.js')
    : await import('@sgt/modules/painel/exportacao.js');
  const pending = kind === 'indicador'
    ? module.exportarIndicadorEfetivoPdf(dataBase, exportContext)
    : module.exportarPainelPdf({ referencia: '2024-03-01', escopo: { nome: 'Escopo C25' } }, 15);
  await vi.waitFor(() => expect(started).toBe(true));
  return { session, pending };
}

describe('FE-01 PDF pertence à geração que iniciou a exportação', () => {
  it.each(['indicador', 'painel'])('%s: troca em outra aba durante o import impede download sem depender do evento storage', async kind => {
    const { pending } = await beginPdf(kind);
    const revisao = `fe01-sessao-sintetica-${crypto.randomUUID()}`;
    localStorage.setItem('sgt_revisao_compartilhada_sessao', revisao);
    document.cookie = `sgt_revisao_compartilhada_sessao=${revisao}; Path=/; SameSite=Strict`;
    release();
    await pending;
    expect(saves).toEqual([]);
    expect(notifications).toEqual([]);
  });
  it.each(['indicador', 'painel'])('%s: não salva nem notifica sucesso após substituir a sessão durante o import', async kind => {
    const { session, pending } = await beginPdf(kind);
    session.invalidarGeracaoSessao();
    release();
    await pending;
    expect(saves).toEqual([]);
    expect(notifications).toEqual([]);
  });
  it.each(['indicador', 'painel'])('%s: preserva o download e a notificação na geração vigente', async kind => {
    const { pending } = await beginPdf(kind);
    release();
    await pending;
    expect(saves).toHaveLength(1);
    expect(saves[0]).toMatch(/\.pdf$/);
    expect(notifications.filter(event => event.type === 'success')).toHaveLength(1);
  });
  it.each(['indicador', 'painel'])('%s: não publica erro de importação da geração encerrada', async kind => {
    const { session, pending } = await beginPdf(kind);
    session.invalidarGeracaoSessao();
    rejectImport = true;
    release();
    await pending;
    expect(saves).toEqual([]);
    expect(notifications).toEqual([]);
  });
  it.each(['indicador', 'painel'])('%s: erro da geração vigente continua explícito', async kind => {
    const { pending } = await beginPdf(kind);
    rejectImport = true;
    release();
    await pending;
    expect(saves).toEqual([]);
    expect(notifications.filter(event => event.type === 'error')).toHaveLength(1);
  });
});

function dates(start, end) {
  const result = [];
  for (let date = new Date(start + 'T12:00:00Z'); date <= new Date(end + 'T12:00:00Z'); date.setUTCDate(date.getUTCDate() + 1)) result.push(date.toISOString().slice(0, 10));
  return result;
}
async function exportRows(data) {
  const writer = await import('@sgt/core/export/xlsx.js');
  const download = vi.spyOn(writer, 'baixarArquivoXlsx').mockImplementation(() => {});
  const { exportarIndicadorEfetivoXlsx } = await import('@sgt/modules/painel/components/IndicadorEfetivo/exportacao.js');
  await exportarIndicadorEfetivoXlsx(data, { ...exportContext, referencia: data.fim });
  expect(download).toHaveBeenCalledOnce();
  return download.mock.calls[0][0].sheets.find(sheet => sheet.name === 'Serie historica').rows.slice(2);
}
const previousValue = (rows, date) => rows.find(row => row.cells[0].value === date)?.cells[11].value;

describe('FE-02 ano anterior do XLSX acompanha a posição de calendário', () => {
  it('ano atual bissexto: 29/02 não toma o valor de março, e março conserva seu homólogo', async () => {
    const data = { ...dataBase,
      serieAtual: dates('2024-02-16', '2024-03-01').map(data => ({ data, percentualAtendimento: 50 })),
      serieAnoAnterior: dates('2023-02-16', '2023-03-01').map((data, index) => ({ data, percentualAtendimento: 101 + index })),
    };
    const rows = await exportRows(data);
    expect(previousValue(rows, '28/02/2024')).toBe(1.13);
    expect(previousValue(rows, '29/02/2024')).toBe('—');
    expect(previousValue(rows, '01/03/2024')).toBe(1.14);
  });
  it('ano anterior bissexto: março não recebe o valor de 29/02 do ano anterior', async () => {
    const data = { ...dataBase, inicio: '2025-02-15', fim: '2025-03-01',
      serieAtual: dates('2025-02-15', '2025-03-01').map(data => ({ data, percentualAtendimento: 50 })),
      serieAnoAnterior: dates('2024-02-15', '2024-03-01').map((data, index) => ({ data, percentualAtendimento: 101 + index })),
    };
    const rows = await exportRows(data);
    expect(previousValue(rows, '28/02/2025')).toBe(1.14);
    expect(previousValue(rows, '01/03/2025')).toBe(1.16);
  });
  it('agregado anual acompanha o mês e conserva percentual zero', async () => {
    const data = { ...dataBase, inicio: '2026-01-01', fim: '2026-03-01', periodo: 'ANUAL',
      serieAtual: [{ data: '2026-01-01', percentualAtendimento: 10 }, { data: '2026-02-01', percentualAtendimento: 20 }, { data: '2026-03-01', percentualAtendimento: 30 }],
      serieAnoAnterior: [{ data: '2025-01-01', percentualAtendimento: 0 }, { data: '2025-02-01', percentualAtendimento: 50 }, { data: '2025-03-01', percentualAtendimento: 90 }],
    };
    const rows = await exportRows(data);
    expect(previousValue(rows, '01/01/2026')).toBe(0);
    expect(previousValue(rows, '01/02/2026')).toBe(0.5);
    expect(previousValue(rows, '01/03/2026')).toBe(0.9);
  });
});
