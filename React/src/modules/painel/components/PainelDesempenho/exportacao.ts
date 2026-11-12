import type { PainelAnaliticoDTO } from '../../../../contracts/painel-analitico';
import { baixarArquivoXlsx } from '../../../../core/export/xlsx';
import { obterGeracaoSessao, sessaoAtual } from '../../../../core/security/geracaoSessao';
import { intervalo } from './formatos';

export const linhasRelatorio = (d: PainelAnaliticoDTO): (string | number)[][] => [
  ['SGT — Painel de desempenho', d.intervalo.granularidade],
  ['Gerado em', d.geradoEm], ['Escopo autorizado', d.escopo.nome],
  ['Contrato', d.selecao.contratoId === null ? 'Todos no escopo' : d.filtros.contratos.find(c => c.id === d.selecao.contratoId)?.nome || String(d.selecao.contratoId)],
  ['Prestador',d.selecao.prestadorId === null?'Todos no escopo':d.filtros.prestadores.find(p=>p.id===d.selecao.prestadorId)?.nome||String(d.selecao.prestadorId)],
  ['Unidade', d.selecao.unidadeId === null ? 'Todas no escopo' : d.filtros.unidades.find(c => c.id === d.selecao.unidadeId)?.nome || String(d.selecao.unidadeId)],
  ['Período', intervalo(d.intervalo.inicio, d.intervalo.fim)],
  ['Comparação', d.comparativo ? intervalo(d.intervalo.anteriorInicio, d.intervalo.anteriorFim) : 'Desligada'],
  ['Indicador', 'Atual', 'Anterior'],
  ['Registros', d.resumoPeriodo.registros, d.comparativo?.registros ?? 'Não solicitado'],
  ['Presenças', d.resumoPeriodo.presencas, d.comparativo?.presencas ?? 'Não solicitado'],
  ['Ausências', d.resumoPeriodo.ausencias, d.comparativo?.ausencias ?? 'Não solicitado'],
  ...d.indisponibilidades.map(m => ['Indisponibilidade', m]),
  ['Tempestividade do primeiro lançamento', 'Quantidade'],
  ['Dentro do prazo',d.indicadores.tempestividade.dentro],['Fora do prazo',d.indicadores.tempestividade.fora],
  ['Sem prazo histórico capturado',d.indicadores.tempestividade.indeterminado],['Em revisão (pode sobrepor prazo)',d.indicadores.tempestividade.emRevisao],
  ['Indicador na referência','Valor (%)','Meta proposta (%)','Base e fonte'],
  ...d.metas.map(m=>[m.nome,m.valor??'Sem base',m.meta??'Sem meta',m.fonte]),
  ['Retrato do efetivo em', d.referencia],
  ['Previsto', d.referenciaDiaria.efetivoPrevisto], ['Alocado', d.referenciaDiaria.efetivoAlocado],
  ['Esperados pela escala',d.indicadores.cobertura.esperados],['Lançamentos entre esperados',d.indicadores.cobertura.lancados],
  ['Pendências entre esperados',d.indicadores.cobertura.pendentes],['Sem prova de escala',d.indicadores.cobertura.semProva],
  ['Contratos no filtro e na referência','Prestador cadastrado','Vigência canônica até','Previsto / vinculado / pendentes'],
  ...d.indicadores.contratos.map(c=>[c.nome,c.prestador??'Não informado',c.vigenciaFim??'Sem data comprovada',`${c.previsto} / ${c.vinculado} / ${c.pendentes}`]),
  ...d.alertas.map(a=>['Alerta',a.titulo,a.mensagem]),
  ['Heatmap de pendências — página atual','Intervalo','Pendentes / esperados','Taxa (%)'],
  ...d.indicadores.heatmapPendencias.flatMap(l=>l.celulas.map((c,i)=>[l.nome,intervalo(d.heatmap.colunas[i].inicio,d.heatmap.colunas[i].fim),`${c.pendentes} / ${c.esperados}`,c.semProva||!c.esperados?'Sem base':Math.round(c.pendentes*10000/c.esperados)/100])),
  ['Evolução histórica', 'Registros', 'Presenças', 'Ausências'],
  ...d.evolucao.map(p => [`${intervalo(p.periodo.inicio, p.periodo.fim)}${p.periodo.parcial ? ' (parcial)' : ''}`, p.atual.registros, p.atual.presencas, p.atual.ausencias]),
  ['Evolução — comparação por ponto','Intervalo anterior','Métrica','Atual / anterior'],
  ...d.evolucao.flatMap(p=>(['registros','presencas','ausencias'] as const).map(m=>[intervalo(p.periodo.inicio,p.periodo.fim),p.periodoAnterior?intervalo(p.periodoAnterior.inicio,p.periodoAnterior.fim):'Não solicitado',m,`${p.atual[m]} / ${p.anterior?.[m]??'Não solicitado'}`])),
  ['Evolução de pendências','Base atual (pendentes / esperados)','Base anterior (pendentes / esperados)','Disponibilidade atual / anterior'],
  ...d.indicadores.evolucaoPendencias.map((p,i)=>[intervalo(p.periodo.inicio,p.periodo.fim),`${p.atual.pendentes} / ${p.atual.esperados}`,
    p.anterior?`${p.anterior.pendentes} / ${p.anterior.esperados}`:'Não solicitado',
    `${p.atual.semProva?'Incompleta: '+p.atual.semProva+' sem prova':p.atual.esperados?'Com base':'Sem esperados'} / ${p.anterior?(p.anterior.semProva?'Incompleta: '+p.anterior.semProva+' sem prova':p.anterior.esperados?'Com base':'Sem esperados'):'Não solicitado'}; anterior: ${d.evolucao[i]?.periodoAnterior?intervalo(d.evolucao[i].periodoAnterior?.inicio??null,d.evolucao[i].periodoAnterior?.fim??null):'não solicitado'}`]),
  [`Unidades — página ${d.paginaUnidades + 1} de ${Math.max(1, Math.ceil(d.totalUnidades / d.tamanhoPagina))}`, 'Registros', 'Presenças', 'Ausências'],
  ...d.unidades.map(u => [u.nome, u.totais.registros, u.totais.presencas, u.totais.ausencias]),
  ...d.metodologia.map(m => ['Metodologia', m]),
];
export const exportarXlsx = (dados: PainelAnaliticoDTO) => baixarArquivoXlsx({ sheets: [{ name: 'Desempenho', rows: linhasRelatorio(dados), columns: [{ width: 62 }, { width: 90 }, { width: 24 }, { width: 24 }], freezeRows: 1 }] }, `sgt_desempenho_${dados.intervalo.granularidade}_${dados.referencia}.xlsx`);
export async function criarPdf(dados: PainelAnaliticoDTO) {
  const [{ jsPDF }, { default: autoTable }] = await Promise.all([import('jspdf'), import('jspdf-autotable')]);
  const doc = new jsPDF({ orientation: 'landscape' });
  autoTable(doc, {
    columns: [0, 1, 2, 3].map(dataKey => ({ dataKey })),
    body: linhasRelatorio(dados).map(linha => linha.length === 2
      ? [linha[0], { content: linha[1], colSpan: 3 }]
      : linha),
    styles: { fontSize: 9, overflow: 'linebreak' },
    columnStyles: { 0: { cellWidth: 75 }, 1: { cellWidth: 90 }, 2: { cellWidth: 54 }, 3: { cellWidth: 54 } },
    margin: 12,
  });
  return doc;
}
export async function exportarPdf(dados: PainelAnaliticoDTO) {
  const geracao = obterGeracaoSessao();
  const doc = await criarPdf(dados);
  if (sessaoAtual(geracao)) doc.save(`sgt_desempenho_${dados.intervalo.granularidade}_${dados.referencia}.pdf`);
}
