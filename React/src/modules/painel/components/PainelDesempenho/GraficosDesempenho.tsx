import React, { useState } from 'react';
import { Area, AreaChart, CartesianGrid, Legend, ResponsiveContainer, Tooltip, XAxis, YAxis } from 'recharts';
import type { FaixaAnalitico, PainelAnaliticoDTO, TotaisAnalitico, MetricaRegistroAnalitico } from '../../../../contracts/painel-analitico';
import { intervalo, nomesMetricas, numero, percentual, dataCivil } from './formatos';
import estilos from './index.module.css';

export interface DetalheDesempenho {
  titulo: string; faixa: FaixaAnalitico; totais: TotaisAnalitico; nota?: string; rota?: string | null;
  unidadeId?: number; metrica?: MetricaRegistroAnalitico;
}
export type AbrirDetalhe = (detalhe: DetalheDesempenho) => void;

export function Evolucao({ dados, abrir }: { dados: PainelAnaliticoDTO; abrir: AbrirDetalhe }) {
  const [metrica, setMetrica] = useState<keyof TotaisAnalitico | 'pendentes'>('registros');
  const nome = metrica==='pendentes' ? 'Pendências' : nomesMetricas[metrica];
  const pontos = dados.evolucao.map((p,i) => ({ rotulo:p.periodo.rotulo+(p.periodo.parcial?' *':''),
    atual:metrica==='pendentes' ? (dados.indicadores.evolucaoPendencias[i]?.atual.semProva ? null : dados.indicadores.evolucaoPendencias[i]?.atual.pendentes ?? null) : p.atual[metrica],
    anterior:metrica==='pendentes' ? (dados.indicadores.evolucaoPendencias[i]?.anterior?.semProva ? null : dados.indicadores.evolucaoPendencias[i]?.anterior?.pendentes ?? null) : p.anterior?.[metrica] ?? null }));
  const basePendencias=dados.indicadores.evolucaoPendencias.at(-1);
  const valorAtual=metrica==='pendentes' ? (basePendencias?.atual.semProva ? null : basePendencias?.atual.pendentes ?? null) : dados.resumoPeriodo[metrica];
  const valorAnterior=metrica==='pendentes' ? (basePendencias?.anterior?.semProva ? null : basePendencias?.anterior?.pendentes ?? null) : dados.comparativo?.[metrica] ?? null;
  const variacao=metrica==='pendentes'
    ? valorAtual!==null && valorAnterior!==null ? {diferenca:valorAtual-valorAnterior,variacaoPercentual:valorAnterior ? (valorAtual-valorAnterior)/valorAnterior*100 : null} : null
    : dados.variacoes?.[metrica];
  const detalhar = (i:number) => { const p=dados.evolucao[i]; if(!p) return;
    const base=dados.indicadores.evolucaoPendencias[i]?.atual;
    abrir({ titulo:`Evolução · ${nome} · ${p.periodo.rotulo}`,faixa:p.periodo,totais:p.atual,
      ...(metrica==='pendentes' ? { nota:base ? `${numero(base.pendentes)} pendências em ${numero(base.esperados)} jornadas esperadas. ${numero(base.semProva)} sem prova de escala. Não representa faltas.` : 'Sem base de elegibilidade.' } : {metrica}) });
  };
  return <section className={estilos.painel} aria-label="Evolução histórica">
    <header className={`${estilos.cabecalhoBloco} ${estilos.cabecalhoGrafico}`}><div><h2>Evolução — gráfico de área</h2><p>Contexto histórico · cortes equivalentes</p></div>
      <label className={estilos.seletorMetrica}><span className={estilos.somenteLeitor}>Métrica da evolução</span><select aria-label="Métrica da evolução" value={metrica} onChange={e => setMetrica(e.target.value as keyof TotaisAnalitico | 'pendentes')}><option value="registros">Registros operacionais</option><option value="presencas">Presenças</option><option value="ausencias">Ausências</option><option value="pendentes">Pendências de lançamento</option></select></label>
    </header>
    <div className={estilos.grafico}>
      {pontos.length ? <ResponsiveContainer width="100%" height={185}>
        <AreaChart data={pontos} margin={{ left: 10, right: 22, top: 14, bottom: 10 }} onClick={e => {
          const i = Number(e.activeTooltipIndex);
          if (e.activeTooltipIndex !== null && Number.isInteger(i) && dados.evolucao[i]) {
            detalhar(i);
          }
        }}>
          <CartesianGrid vertical={false} stroke="var(--cor-borda-sutil)" />
          <XAxis dataKey="rotulo" tick={{ fill: 'var(--cor-texto-secundario)', fontSize: 12 }} />
          <YAxis allowDecimals={false} tick={{ fill: 'var(--cor-texto-secundario)', fontSize: 12 }} width={62} />
          <Tooltip formatter={v => numero(Number(v ?? 0))} labelFormatter={label => {
            const i = pontos.findIndex(p => p.rotulo === label); const p = dados.evolucao[i];
            return p ? <span>{intervalo(p.periodo.inicio, p.periodo.fim)}{p.periodo.parcial ? ' · PARCIAL' : ''}{p.periodoAnterior && <span style={{ display: 'block' }}>Anterior: {intervalo(p.periodoAnterior.inicio, p.periodoAnterior.fim)}</span>}</span> : String(label);
          }} />
          <Legend />
          <Area dataKey="atual" name="Atual" type="linear" stroke="var(--cor-correios-azul-600)" fill="var(--cor-correios-azul-100)" strokeWidth={3} dot={{ r: 4 }} isAnimationActive={false} />
          {dados.selecao.comparar && <Area dataKey="anterior" name="Anterior equivalente" type="linear" stroke="var(--cor-texto-secundario)" fill="transparent" strokeDasharray="6 5" connectNulls={false} isAnimationActive={false} />}
        </AreaChart>
      </ResponsiveContainer> : <p className={estilos.vazio}>Nenhum ponto histórico disponível.</p>}
    </div>
    <p className={estilos.nota}>* Corte parcial. Datas e bases na tabela; volume não indica melhora ou piora.</p>
    <div className={estilos.comparacaoIntegrada} aria-label={`Comparação de ${nome.toLowerCase()}`}><div><small title={intervalo(dados.intervalo.anteriorInicio,dados.intervalo.anteriorFim)}>Anterior equivalente</small><strong>{dados.selecao.comparar ? valorAnterior===null?'Sem base':`${numero(valorAnterior)} ${nome.toLowerCase()}`:'Desligada'}</strong></div><div><small title={intervalo(dados.intervalo.inicio,dados.intervalo.fim)}>Período atual</small><strong>{valorAtual===null?'Sem base':`${numero(valorAtual)} ${nome.toLowerCase()}`}</strong></div><div><small>Variação de volume</small><strong>{dados.selecao.comparar && variacao ? `${numero(variacao.diferenca)} · ${percentual(variacao.variacaoPercentual)}` : 'Sem base'}</strong></div></div>
    <details className={estilos.tabelaAcessivel}><summary>Ver valores e detalhar pontos</summary><table><thead><tr><th>Intervalo</th><th>Atual</th>{dados.selecao.comparar && <th>Anterior</th>}<th>Detalhamento</th></tr></thead><tbody>{dados.evolucao.map(p => <tr key={p.periodo.inicio}><td>{intervalo(p.periodo.inicio, p.periodo.fim)}{p.periodo.parcial ? ' · parcial' : ''}</td><td>{pontos[dados.evolucao.indexOf(p)]?.atual == null ? 'Sem base' : numero(pontos[dados.evolucao.indexOf(p)].atual ?? 0)}</td>{dados.selecao.comparar && <td>{pontos[dados.evolucao.indexOf(p)]?.anterior === null ? 'Sem base' : numero(pontos[dados.evolucao.indexOf(p)]?.anterior ?? 0)}</td>}<td><button type="button" onClick={() => detalhar(dados.evolucao.indexOf(p))}>Detalhar {p.periodo.rotulo}</button></td></tr>)}</tbody></table></details>
  </section>;
}

export function Heatmap({ dados, abrir }: { dados: PainelAnaliticoDTO; abrir: AbrirDetalhe }) {
  const linhas = dados.indicadores.heatmapPendencias;
  return <section className={estilos.painel} aria-label="Mapa de calor">
    <header className={estilos.cabecalhoBloco}><div><h2>Heatmap — pendências de frequência</h2><p>{dados.intervalo.granularidade==='DIARIO' ? 'Contexto dos últimos sete dias' : 'Período selecionado'} · taxa por unidade · página atual</p></div></header>
    <div className={estilos.rolagemTabela}><table className={estilos.heatmap}><thead><tr><th>Unidade</th>{dados.heatmap.colunas.map(c => <th key={c.inicio} title={intervalo(c.inicio,c.fim)}>{c.inicio===c.fim?dataCivil(c.inicio).slice(0,5):c.rotulo}{c.parcial ? ' *' : ''}</th>)}</tr></thead><tbody>{linhas.map(l => <tr key={l.unidadeId}><th scope="row">{l.nome}</th>{l.celulas.map((c,i) => {
      const taxa = c.semProva || !c.esperados ? null : c.pendentes / c.esperados * 100;
      const faixa = dados.heatmap.colunas[i];
      return <td key={faixa.inicio}><button type="button" data-intensidade={taxa===null?'sem-base':taxa<=5?'baixa':taxa<=10?'media':'alta'} aria-label={`${l.nome}, ${intervalo(faixa.inicio,faixa.fim)}: ${taxa===null?'sem base comprovada':`${numero(c.pendentes)} pendentes de ${numero(c.esperados)} esperados`}`} onClick={() => abrir({ titulo: `${l.nome} · pendências`, faixa, totais: { registros:c.lancados,presencas:0,ausencias:0 }, unidadeId:l.unidadeId, nota: `${numero(c.pendentes)} pendências / ${numero(c.esperados)} esperados; ${numero(c.lancados)} lançamentos entre esperados; ${numero(c.semProva)} sem prova completa de escala. A taxa soma as bases do intervalo, sem média simples dos percentuais. Pendências não são faltas. Faixas de cor para leitura: até 5%, acima de 5 até 10%, acima de 10%; não são metas oficiais.` })}>{taxa===null?'Sem base':`${new Intl.NumberFormat('pt-BR',{maximumFractionDigits:1}).format(taxa)}%`}<small>{numero(c.pendentes)}/{numero(c.esperados)}</small></button></td>;
    })}</tr>)}</tbody></table></div>
    {!linhas.length && <p className={estilos.vazio}>Sem vínculos ou registros no recorte.</p>}
    <p className={estilos.legendaCalor}><span data-faixa="baixa">Até 5%</span><span data-faixa="media">Acima de 5 a 10%</span><span data-faixa="alta">Acima de 10%</span><span>Sem base</span></p>
    <p className={estilos.nota}>Pendentes / esperados. Faixas de leitura; metas propostas na seção de desempenho.</p>
  </section>;
}
