import React, { useState } from 'react';
import { AlertTriangle, ArrowUpRight, CheckCheck, ClipboardList, Clock3, SearchCheck } from 'lucide-react';
import type { FaixaAnalitico, PainelAnaliticoDTO } from '../../../../contracts/painel-analitico';
import type { AbrirDetalhe } from './GraficosDesempenho';
import { dataCivil, intervalo, numero, percentual } from './formatos';
import { RegistrosOrigem } from './RegistrosOrigem';
import estilos from './index.module.css';

type Props = { dados: PainelAnaliticoDTO; abrir: AbrirDetalhe };
export function IndicadoresPeriodo({ dados, abrir }: Props) {
  const t = dados.indicadores.tempestividade;
  const classificados = t.dentro + t.fora;
  const faixa: FaixaAnalitico = { ...dados.intervalo, rotulo: 'Período selecionado' };
  const kpis = [
    { nome: 'Total de registros', valor: numero(dados.resumoPeriodo.registros), texto: 'Lançamentos operacionais no período', ajuda: dados.comparativo ? `${numero(dados.variacoes?.registros.diferenca ?? 0)} em relação ao anterior` : 'Comparação desligada', Icone: ClipboardList, metrica: 'registros' as const },
    { nome: 'Dentro do prazo', valor: percentual(classificados ? t.dentro / classificados * 100 : null), texto: `${numero(t.dentro)} de ${numero(classificados)} classificados`, ajuda: `${numero(t.indeterminado)} sem prazo histórico capturado`, Icone: CheckCheck, metrica: 'dentro' as const },
    { nome: 'Fora do prazo', valor: numero(t.fora), texto: 'Primeiro lançamento após o prazo', ajuda: 'A edição preserva essa classificação', Icone: Clock3, metrica: 'fora' as const },
    { nome: 'Em revisão', valor: numero(t.emRevisao), texto: 'Solicitações aguardando o Gestor Local', ajuda: 'Pode coexistir com dentro ou fora do prazo', Icone: SearchCheck, metrica: 'revisao' as const },
  ];
  return <section className={estilos.kpis} aria-label="Indicadores do período">{kpis.map(k => <button key={k.nome} type="button" className={estilos.kpi} aria-label={`${k.nome}: ${k.valor}`} onClick={() => abrir({ titulo: k.nome, faixa, totais: dados.resumoPeriodo, metrica: k.metrica })}>
    <span>{k.nome}<k.Icone size={16} aria-hidden="true" /></span><strong>{k.valor}</strong><small>{k.texto}</small><p>{k.ajuda}</p>
  </button>)}</section>;
}

export function MetasProgresso({ dados, abrir }: Props) {
  return <section className={estilos.painel} aria-label="Indicadores de desempenho e metas"><header className={estilos.cabecalhoBloco}><div><h2>Indicadores de desempenho e metas</h2><p>Fotografia de {dataCivil(dados.referencia)} · metas propostas</p></div></header>
    {dados.metas.map(m => <button type="button" className={estilos.meta} key={m.codigo} onClick={() => abrir({ titulo: m.nome, faixa: { inicio: dados.referencia, fim: dados.referencia, parcial: false, rotulo: 'Referência' }, totais: dados.resumoPeriodo, nota: m.fonte })}>
      <span><strong>{m.nome}</strong><b>{percentual(m.valor)}</b></span>
      {m.valor !== null ? <progress max={100} value={Math.max(0, Math.min(100, m.valor))} aria-label={m.nome} /> : <span className={estilos.semBase}>Sem base comprovada para publicar a taxa</span>}
      <small>{m.fonte}</small><span className={estilos.alvo}>Meta proposta: {percentual(m.meta)}</span>
    </button>)}
    <p className={estilos.nota}>Propostas para avaliação da chefia. Clique no indicador para consultar sua base.</p>
  </section>;
}

export function FunilCobertura({ dados, abrir }: Props) {
  const b = dados.indicadores.cobertura;
  return <section className={estilos.painel} aria-label="Funil de cobertura"><header className={estilos.cabecalhoBloco}><div><h2>Funil de cobertura</h2><p>Fotografia de {dataCivil(dados.referencia)} · mesma base da cobertura</p></div></header>
    {([['Efetivo vinculado', b.vinculados], ['Empregados esperados', b.esperados], ['Lançamentos entre esperados', b.lancados]] as const).map(([nome, valor]) => <button key={nome} type="button" className={estilos.etapaFunil} onClick={() => abrir({ titulo: nome, faixa: { inicio: dados.referencia, fim: dados.referencia, parcial: false, rotulo: 'Referência' }, totais: dados.resumoPeriodo, nota: `${numero(b.vinculados)} vínculos com alocação vigente; ${numero(b.esperados)} esperados pela escala comprovada; ${numero(b.lancados)} com lançamento válido entre esses esperados; ${numero(b.semProva)} sem prova completa de escala. A grade fotografada, as folgas e o calendário aplicável definem os esperados, na verdade conhecida na consulta.` })}>
      <span>{nome}<b>{numero(valor)}</b></span><progress max={Math.max(1,b.vinculados)} value={valor} aria-label={nome} />
    </button>)}
    <div className={estilos.pendentes}><span>Registros pendentes na referência</span><strong>{numero(b.pendentes)}</strong></div>
    <p className={estilos.nota}>Pendência de lançamento não significa falta do trabalhador.{b.semProva > 0 && ` ${numero(b.semProva)} sem prova de escala: base incompleta.`}</p>
  </section>;
}

export function AlertasOperacionais({ dados, abrir }: Props) {
  return <section className={estilos.painel}><h2>Precisa da sua atenção</h2>{dados.alertas.length ? dados.alertas.map(a => <button key={a.codigo} type="button" className={estilos.alerta} onClick={() => abrir({ titulo: a.titulo, faixa: { inicio: a.codigo === 'REVISAO_SOLICITADA' || a.codigo === 'AUSENCIAS_REGISTRADAS' ? dados.intervalo.inicio : dados.referencia, fim: dados.referencia, parcial: false, rotulo: 'Alerta' }, totais: dados.resumoPeriodo, nota: a.mensagem, rota: a.rota })}>
    <AlertTriangle size={17} aria-hidden="true" /><span><strong>{a.titulo}</strong><small>{a.mensagem}</small></span><ArrowUpRight size={16} aria-hidden="true" />
  </button>) : <p className={estilos.vazio}>Nenhum alerta derivado das bases disponíveis neste recorte.</p>}</section>;
}

export function DiagnosticoRecorte({ dados, abrir }: Props) {
  const [aba, setAba] = useState<'unidades' | 'contratos' | 'registros'>('unidades');
  const [busca, setBusca] = useState('');
  const faixa: FaixaAnalitico = { ...dados.intervalo, rotulo: 'Período selecionado' };
  const procurar = (texto: string) => texto.toLocaleLowerCase('pt-BR').includes(busca.toLocaleLowerCase('pt-BR'));
  return <section className={estilos.painel} aria-label="Diagnóstico do recorte"><header className={estilos.cabecalhoBloco}><h2>Diagnóstico do recorte</h2><div className={estilos.segmentado} aria-label="Visões de diagnóstico">
    {(['unidades','contratos','registros'] as const).filter(a => a !== 'registros' || dados.escopo.permiteDetalhamentoOperacional).map(a => <button key={a} type="button" aria-pressed={aba===a} onClick={() => { setAba(a); setBusca(''); }}>{({ unidades: 'Unidades', contratos: 'Contratos e prestadores', registros: 'Registros' })[a]}</button>)}
  </div></header>
  {aba !== 'registros' && <label className={estilos.busca}>Buscar na página<input type="search" value={busca} onChange={e => setBusca(e.target.value)} placeholder={aba==='unidades'?'Nome da unidade':'Contrato ou prestador'} /></label>}
  <div className={estilos.rolagemTabela}>{aba==='unidades' && <table><caption>Período {intervalo(dados.intervalo.inicio,dados.intervalo.fim)} · página atual</caption><thead><tr><th>Unidade</th><th>Registros</th><th>Presenças</th><th>Ausências</th></tr></thead><tbody>{dados.unidades.filter(u => procurar(u.nome)).map(u => <tr key={u.id}><th scope="row"><button type="button" className={estilos.linkTabela} onClick={() => abrir({ titulo: u.nome, faixa, totais: u.totais, unidadeId: u.id })}>{u.nome}</button></th><td>{numero(u.totais.registros)}</td><td>{numero(u.totais.presencas)}</td><td>{numero(u.totais.ausencias)}</td></tr>)}</tbody></table>}
  {aba==='contratos' && <table><caption>Efetivo em {dataCivil(dados.referencia)} · vigência canônica e prestador cadastrado no contrato</caption><thead><tr><th>Contrato / prestador</th><th>Vigência até</th><th>Previsto</th><th>Vinculado</th><th>Pendentes</th></tr></thead><tbody>{dados.indicadores.contratos.filter(c => procurar(`${c.nome} ${c.prestador ?? ''}`)).map(c => <tr key={c.id}><th scope="row">{c.nome}<small className={estilos.subTexto}>{c.prestador || 'Prestador não informado'}</small></th><td>{c.vigenciaFim ? dataCivil(c.vigenciaFim) : 'Sem data comprovada'}</td><td>{numero(c.previsto)}</td><td>{numero(c.vinculado)}</td><td>{numero(c.pendentes)}</td></tr>)}</tbody></table>}</div>
  {aba==='registros' && dados.escopo.permiteDetalhamentoOperacional && <RegistrosOrigem dados={dados} detalhe={{ titulo: 'Registros do período', faixa, totais: dados.resumoPeriodo, metrica: 'registros' }} />}
  {aba==='unidades' && <p className={estilos.nota}>{dados.totalUnidades} unidade(s). As páginas do diagnóstico e do mapa de calor são compartilhadas.</p>}
  {aba==='contratos' && !dados.indicadores.contratos.length && <p className={estilos.vazio}>Nenhum contrato vigente na referência e no filtro.</p>}
  </section>;
}
