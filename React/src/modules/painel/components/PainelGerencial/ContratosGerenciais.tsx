import { useState } from 'react';
import type { PainelVisoesDTO } from '../../../../contracts/painel-visoes';
import type { AbrirDetalhe } from '../PainelDesempenho/GraficosDesempenho';
import { dataCivil, numero, percentual } from '../PainelDesempenho/formatos';
import base from '../PainelDesempenho/index.module.css';
import estilos from './index.module.css';

export function ContratosGerenciais({ resposta, abrir }: { resposta: PainelVisoesDTO; abrir: AbrirDetalhe }) {
  const [busca, setBusca] = useState('');
  const d = resposta.analitico, anterior = resposta.efetivoAnterior;
  const contratos = d.indicadores.contratos.filter(c => `${c.nome} ${c.prestador || ''}`.toLocaleLowerCase('pt-BR').includes(busca.toLocaleLowerCase('pt-BR')));
  return <section className={base.painel} id="contratos-recorte">
    <header className={base.cabecalhoBloco}><div><h2>Contratos, vigência e dimensionamento</h2><p>Fotografia de {dataCivil(d.referencia)} · contratos do recorte autorizado</p></div></header>
    <label className={base.busca}>Buscar contrato ou prestador<input type="search" value={busca} onChange={e => setBusca(e.target.value)} placeholder="Nome do contrato ou da empresa" /></label>
    <div className={estilos.tabela}><table><caption className={estilos.nota}>Selecione o contrato para consultar sua base. Déficit e excesso são apresentados separadamente.</caption><thead><tr><th>Contrato / prestador</th><th>Vigência até</th><th>Previsto</th><th>Vinculado</th><th>Déficit</th><th>Excesso</th><th>Pendentes</th></tr></thead><tbody>{contratos.map(c => <tr key={c.id}>
      <th scope="row"><button type="button" onClick={() => abrir({ titulo: c.nome, faixa: { inicio: d.referencia, fim: d.referencia, parcial: false, rotulo: 'Referência' }, totais: d.resumoPeriodo, nota: `${c.prestador || 'Prestador não informado'}. ${numero(c.previsto)} previstos; ${numero(c.vinculado)} vinculados; ${numero(c.esperados)} esperados; ${numero(c.lancados)} lançados; ${numero(c.pendentes)} pendentes. Vigência final: ${c.vigenciaFim ? dataCivil(c.vigenciaFim) : 'sem data comprovada'}. Fonte: composição e alocação vigentes, escala fotografada e lançamentos válidos no mesmo recorte.` })}>{c.nome}</button><small>{c.prestador || 'Prestador não informado'}</small></th>
      <td>{c.vigenciaFim ? dataCivil(c.vigenciaFim) : 'Sem data comprovada'}</td><td>{numero(c.previsto)}</td><td>{numero(c.vinculado)}</td><td data-alerta={c.previsto > c.vinculado}>{numero(Math.max(0, c.previsto - c.vinculado))}</td><td data-alerta={c.vinculado > c.previsto}>{numero(Math.max(0, c.vinculado - c.previsto))}</td><td data-alerta={c.pendentes > 0}>{numero(c.pendentes)}</td>
    </tr>)}</tbody></table></div>
    {!contratos.length && <p className={base.vazio}>Nenhum contrato encontrado para esta busca e os filtros selecionados.</p>}
    {anterior && <><p className={estilos.nota}>Comparação de fotografias: {dataCivil(d.intervalo.anteriorFim)} e {dataCivil(d.referencia)}.</p><div className={estilos.comparacao}>
      <div><small>Vínculos no fim do período anterior</small><strong>{numero(anterior.efetivoVinculado)}</strong></div>
      <div><small>Vínculos na referência atual</small><strong>{numero(d.referenciaDiaria.efetivoAlocado)}</strong></div>
      <div><small>Variação da ocupação</small><strong>{anterior.efetivoPrevisto && d.referenciaDiaria.ocupacao !== null ? `${percentual(d.referenciaDiaria.ocupacao - anterior.efetivoVinculado * 100 / anterior.efetivoPrevisto).replace('%', '')} p.p.` : 'Sem base'}</strong></div>
    </div></>}
  </section>;
}
