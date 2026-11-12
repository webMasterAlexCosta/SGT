import { ArrowUpRight, BriefcaseBusiness, ClipboardList, Users, UserCheck, UserMinus, CalendarClock, ShieldCheck } from 'lucide-react';
import type { PainelAnaliticoDTO, MetricaRegistroAnalitico } from '../../../../contracts/painel-analitico';
import type { VisaoGerencial } from '../../../../contracts/painel-visoes';
import type { AbrirDetalhe } from '../PainelDesempenho/GraficosDesempenho';
import { numero, percentual, dataCivil } from '../PainelDesempenho/formatos';
import estilos from '../PainelDesempenho/index.module.css';

export function IndicadoresGerenciais({ dados: d, visao, abrir }: { dados: PainelAnaliticoDTO; visao: VisaoGerencial; abrir: AbrirDetalhe }) {
  const b = d.indicadores.cobertura, r = d.referenciaDiaria;
  const contratos = d.indicadores.contratos;
  const notaEfetivo = `${numero(r.efetivoAlocado)} alocados / ${numero(r.efetivoPrevisto)} previstos. Composição e vínculos vigentes em ${dataCivil(d.referencia)}. Déficit e excesso não se compensam entre contratos.`;
  const deficit = contratos.reduce((n, c) => n + Math.max(0, c.previsto - c.vinculado), 0);
  const excesso = contratos.reduce((n, c) => n + Math.max(0, c.vinculado - c.previsto), 0);
  const vencendo = contratos.filter(c => c.vigenciaFim && c.vigenciaFim >= d.referencia && (Date.parse(c.vigenciaFim) - Date.parse(d.referencia)) / 86400000 <= 30).length;
  const faixa = { ...d.intervalo, rotulo: 'Período selecionado' };
  const diaria = { inicio: d.referencia, fim: d.referencia, parcial: false, rotulo: 'Referência' };
  const variacao = (m: 'registros' | 'presencas' | 'ausencias') => d.variacoes ? `${numero(d.variacoes[m].diferenca)} em relação ao anterior` : 'Comparação desligada';
  const operacional = [
    { titulo: 'Registros operacionais', valor: numero(d.resumoPeriodo.registros), texto: 'Lançamentos válidos no período', ajuda: variacao('registros'), Icone: ClipboardList, metrica: 'registros' as MetricaRegistroAnalitico },
    { titulo: 'Presenças', valor: numero(d.resumoPeriodo.presencas), texto: 'Presença, parcial, atraso e TR', ajuda: variacao('presencas'), Icone: UserCheck, metrica: 'presencas' as MetricaRegistroAnalitico },
    { titulo: 'Ausências', valor: numero(d.resumoPeriodo.ausencias), texto: 'Falta, atestado e suspensão', ajuda: variacao('ausencias'), Icone: UserMinus, metrica: 'ausencias' as MetricaRegistroAnalitico },
    { titulo: 'Em revisão', valor: numero(d.indicadores.tempestividade.emRevisao), texto: 'Aguardando o Gestor Local', ajuda: 'Pode coexistir com dentro ou fora do prazo', Icone: ShieldCheck, metrica: 'revisao' as MetricaRegistroAnalitico },
  ];
  const contratual = [
    { titulo: 'Contratos vigentes', valor: numero(r.contratosVigentes), texto: `Fotografia de ${dataCivil(d.referencia)}`, ajuda: 'Vigência canônica e filtros aplicados', Icone: BriefcaseBusiness, nota: `${r.contratosVigentes} contrato(s) vigente(s) na referência. Consulte a tabela para vigência, prestador e dimensionamento.` },
    { titulo: 'Ocupação do efetivo', valor: percentual(r.ocupacao), texto: `${numero(r.efetivoAlocado)} alocados / ${numero(r.efetivoPrevisto)} previstos`, ajuda: 'Base contratual na referência', Icone: Users, nota: notaEfetivo },
    { titulo: 'Déficit de efetivo', valor: numero(deficit), texto: 'Vagas abaixo do previsto por contrato', ajuda: `${numero(excesso)} alocação(ões) excedente(s), em separado`, Icone: UserMinus, nota: notaEfetivo },
    { titulo: 'Vencem em até 30 dias', valor: numero(vencendo), texto: 'A partir da data de referência', ajuda: 'Término contratual reconstruído', Icone: CalendarClock, nota: 'Janela inclusiva de 30 dias após a referência, somente contratos vigentes do recorte. Sem data final comprovada não é estimada.' },
  ];
  const resumo = [contratual[0], contratual[1], operacional[0], { titulo: 'Cobertura de frequência', valor: percentual(b.semProva || !b.esperados ? null : b.lancados * 100 / b.esperados), texto: `${numero(b.lancados)} lançados / ${numero(b.esperados)} esperados`, ajuda: `${numero(b.pendentes)} pendentes na referência`, Icone: ShieldCheck, nota: `${b.semProva} sem prova de escala. Esperados seguem a grade, folgas e calendário aplicável. Pendência de lançamento não significa falta.` }];
  const itens = visao === 'contratos' ? contratual : visao === 'operacao' ? operacional : resumo;
  return <section className={estilos.kpis} aria-label={`Indicadores de ${visao}`}>{itens.map((k, i) => <button type="button" key={k.titulo} className={estilos.kpi} data-metrica={i === 0 ? 'registros' : undefined} onClick={() => abrir({ titulo: k.titulo, faixa: 'nota' in k ? diaria : faixa, totais: d.resumoPeriodo, ...('nota' in k ? { nota: k.nota } : { metrica: k.metrica }) })}>
    <span className={estilos.rotuloKpi}>{k.titulo}<k.Icone size={19} aria-hidden="true" /></span><strong>{k.valor}</strong><small>{k.texto}</small><p>{k.ajuda}</p><span className={estilos.acaoKpi}>Ver detalhes<ArrowUpRight size={15} aria-hidden="true" /></span>
  </button>)}</section>;
}
