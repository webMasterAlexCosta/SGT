import type { PainelVisoesDTO, VisaoGerencial } from '../../../../contracts/painel-visoes';
import { baixarArquivoXlsx } from '../../../../core/export/xlsx';
import { obterGeracaoSessao, sessaoAtual } from '../../../../core/security/geracaoSessao';
import { linhasRelatorio } from '../PainelDesempenho/exportacao';

const nomes = { resumo: 'Resumo', contratos: 'Contratos', operacao: 'Operação', tudo: 'Painel gerencial' };
export function linhasVisao(d: PainelVisoesDTO, visao: VisaoGerencial): (string | number)[][] {
  const g = d.gestao;
  return [
    [`SGT — ${nomes[visao]}`, d.analitico.intervalo.granularidade],
    ...linhasRelatorio(d.analitico).slice(1),
    ['Indicadores complementares', 'Valor', 'Fonte / referência'],
    ['Colaboradores cadastrados', g.quadro.empregadosCadastrados, 'Cadastro consultado, limitado pela admissão e filtros'],
    ['Admitidos no período', g.analiseCadastro.admitidosNoPeriodo, 'Data de admissão cadastrada'],
    ['Sem data de admissão', g.analiseCadastro.semDataAdmissao],
    ['Contratos vencidos marcados ativos', g.contratos.vencidosMarcadosAtivos],
    ['Contratos sem data final', g.contratos.semDataFim],
    ['Contratos sem efetivo previsto', g.contratos.semEfetivoPrevisto],
    ['RT no período', g.pressaoOperacional.repousosTrabalhados],
    ['Empregados com ausência recorrente', g.periodo.empregadosComAusenciaRecorrente],
    ['Inconsistências cadastrais', g.integridade.totalInconsistencias],
    ['Distribuição de status', 'Quantidade', 'Percentual'],
    ...g.distribuicaoStatus.map(v => [v.status ?? "Sem status", v.quantidade, v.percentual]),
    ['Padrão semanal', 'Registros', 'Ausências', 'Absenteísmo (%)'],
    ...g.padraoSemanal.map(v => [v.dia ?? "Sem dia", v.registrosOperacionais, v.ausencias, v.taxaAbsenteismo]),
    ...g.notasMetodologicas.map(n => ['Metodologia complementar', n]),
  ];
}
export async function exportarVisao(d: PainelVisoesDTO, visao: VisaoGerencial, formato: 'xlsx' | 'pdf') {
  const rows = linhasVisao(d, visao), nome = `sgt_${visao}_${d.analitico.intervalo.granularidade}_${d.analitico.referencia}`;
  if (formato === 'xlsx') {
    baixarArquivoXlsx({ sheets: [{ name: nomes[visao], rows, columns: [{ width: 62 }, { width: 90 }, { width: 24 }, { width: 24 }], freezeRows: 1 }] }, `${nome}.xlsx`);
    return;
  }
  const geracao = obterGeracaoSessao();
  const [{ jsPDF }, { default: autoTable }] = await Promise.all([import('jspdf'), import('jspdf-autotable')]);
  if (!sessaoAtual(geracao)) return;
  const doc = new jsPDF({ orientation: 'landscape' });
  autoTable(doc, { columns: [0, 1, 2, 3].map(dataKey => ({ dataKey })), body: rows.map(row => row.length === 2 ? [row[0], { content: row[1], colSpan: 3 }] : row), styles: { fontSize: 9, overflow: 'linebreak' }, margin: 12 });
  if (sessaoAtual(geracao)) doc.save(`${nome}.pdf`);
}
