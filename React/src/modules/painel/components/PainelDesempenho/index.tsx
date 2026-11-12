import React, { useCallback, useState } from 'react';
import { AlertTriangle, CalendarDays, Download, RefreshCw } from 'lucide-react';
import type { FaixaAnalitico, GranularidadePainel, TotaisAnalitico } from '../../../../contracts/painel-analitico';
import { ModalPainel } from '../ModalPainel';
import { Evolucao, Heatmap, type AbrirDetalhe, type DetalheDesempenho } from './GraficosDesempenho';
import { hojeCivil, useDesempenho } from './useDesempenho';
import { dataCivil, intervalo, nomesMetricas, numero, percentual } from './formatos';
import { exportarPdf, exportarXlsx } from './exportacao';
import estilos from './index.module.css';
import { IndicadoresPeriodo, MetasProgresso, FunilCobertura, AlertasOperacionais, DiagnosticoRecorte } from './BlocosDesempenho';
import { RegistrosOrigem } from './RegistrosOrigem';

const periodos: { id: GranularidadePainel; nome: string; panorama: string }[] = [
  { id: 'DIARIO', nome: 'Diário', panorama: 'do dia' }, { id: 'SEMANAL', nome: 'Semanal', panorama: 'da semana' },
  { id: 'MENSAL', nome: 'Mensal', panorama: 'do mês' }, { id: 'TRIMESTRAL', nome: 'Trimestral', panorama: 'do trimestre' }, { id: 'ANUAL', nome: 'Anual', panorama: 'do ano' },
];


export default function PainelDesempenho({ referenciaInicial, aoNavegar }: { referenciaInicial?: string; aoNavegar?: (rota: string) => void }) {
  const { filtros, dados, opcoes, erro, carregando, alterar, recarregar, limpar } = useDesempenho(referenciaInicial);
  const [detalhe, setDetalhe] = useState<DetalheDesempenho | null>(null);
  const [erroExportacao, setErroExportacao] = useState<string | null>(null);
  const fechar = useCallback(() => setDetalhe(null), []);
  const abrir: AbrirDetalhe = novo => setDetalhe(novo);
  const faixa: FaixaAnalitico | null = dados ? { inicio: dados.intervalo.inicio, fim: dados.intervalo.fim, parcial: dados.intervalo.parcial, rotulo: 'Período selecionado' } : null;
  const exportar = async (tipo: 'pdf' | 'xlsx') => {
    if (!dados) return; setErroExportacao(null);
    try { if (tipo === 'pdf') await exportarPdf(dados); else exportarXlsx(dados); }
    catch { setErroExportacao('Não foi possível gerar o arquivo. Tente novamente.'); }
  };

  return <div className={estilos.desempenho}>
    <header className={estilos.titulo}><div><h1>Desempenho operacional</h1><p>Frequências, efetivo e prioridades para a decisão gerencial.</p></div><div className={estilos.acoesTitulo}><span><CalendarDays size={15} aria-hidden="true" />{dados ? `Referência ${dataCivil(dados.referencia)}` : 'Consulta ao servidor'}</span><div className={estilos.exportacoes}><Download size={16} aria-hidden="true" /><button type="button" disabled={!dados} onClick={() => void exportar('xlsx')}>Exportar XLSX</button><button type="button" disabled={!dados} onClick={() => void exportar('pdf')}>PDF</button></div></div></header>
    <section className={estilos.painel} aria-label="Filtros de desempenho">
      <div className={estilos.cabecalhoFiltro}>
        <div className={estilos.segmentado} aria-label="Período de análise">{periodos.map(p => <button key={p.id} type="button" aria-pressed={filtros.granularidade === p.id} onClick={() => { fechar(); alterar({ granularidade: p.id }); }}>{p.nome}</button>)}</div>
        <label className={estilos.dataReferencia}>Referência<input aria-label="Data de referência" type="date" min={String(Number(hojeCivil().slice(0,4))-2)+hojeCivil().slice(4)} max={hojeCivil()} value={filtros.referencia} onChange={e => { if (e.target.value) { fechar(); alterar({ referencia: e.target.value }); } }} /></label>
        <label className={estilos.checkbox}><input type="checkbox" checked={filtros.comparar} onChange={e => { fechar(); alterar({ comparar: e.target.checked }); }} />Comparar período anterior</label>
        <button type="button" className={estilos.atualizar} aria-label="Atualizar desempenho" onClick={recarregar}><RefreshCw size={16} aria-hidden="true" /></button>
      </div>
      <div className={estilos.filtrosRecorte}>
        <label>Contrato<select aria-label="Contrato" value={filtros.contratoId ?? ''} onChange={e => { fechar(); alterar({ contratoId: e.target.value ? Number(e.target.value) : undefined, unidadeId: undefined }); }}><option value="">Todos os contratos</option>{opcoes?.contratos.map(c => <option key={c.id} value={c.id}>{c.nome}</option>)}</select></label>
        <label>Prestador / empresa<select aria-label="Prestador / empresa" value={filtros.prestadorId ?? ''} onChange={e => { fechar(); alterar({ prestadorId: e.target.value ? Number(e.target.value) : undefined, contratoId: undefined }); }}><option value="">Todos os prestadores</option>{opcoes?.prestadores.map(p => <option key={p.id} value={p.id}>{p.nome}</option>)}</select></label>
        <label>Unidade<select aria-label="Unidade" value={filtros.unidadeId ?? ''} onChange={e => { fechar(); alterar({ unidadeId: e.target.value ? Number(e.target.value) : undefined, tipoUnidade: opcoes?.tipoUnidade }); }}><option value="">Todas as unidades</option>{opcoes?.unidades.map(u => <option key={u.id} value={u.id}>{u.nome}</option>)}</select></label>
        <button type="button" onClick={() => { fechar(); limpar(); }}>Limpar contrato e unidade</button>
      </div>
    </section>
    {erro && <div role="alert" className={estilos.erro}><AlertTriangle size={19} aria-hidden="true" /><span>{erro}</span><button type="button" onClick={recarregar}>Tentar novamente</button></div>}
    {erroExportacao && <p role="alert" className={estilos.erro}>{erroExportacao}</p>}
    <div role="status" aria-live="polite">{carregando && <p className={estilos.carregando}>Consultando indicadores reais no servidor…</p>}</div>
    {dados && faixa && <>
      <div className={estilos.intervaloAplicado}><div><strong>{periodos.find(p => p.id === dados.intervalo.granularidade)?.nome} · {intervalo(dados.intervalo.inicio, dados.intervalo.fim)}</strong><span>{dados.intervalo.parcial ? 'Período parcial' : 'Período completo'}</span></div><small>{dados.comparativo && <>Anterior: {intervalo(dados.intervalo.anteriorInicio,dados.intervalo.anteriorFim)} · </>}{dados.escopo.nome}</small></div>
      <IndicadoresPeriodo dados={dados} abrir={abrir} />
      <div className={estilos.faixaAnalise}><MetasProgresso dados={dados} abrir={abrir} /><Evolucao dados={dados} abrir={abrir} /></div>
      <div className={estilos.faixaCobertura}><Heatmap dados={dados} abrir={abrir} /><FunilCobertura dados={dados} abrir={abrir} /></div>
      <nav className={estilos.paginacao} aria-label="Páginas das unidades"><button type="button" disabled={!dados.paginaUnidades} onClick={() => alterar({ paginaUnidades: dados.paginaUnidades - 1 })}>Anterior</button><span>{dados.totalUnidades} unidade(s) · página {dados.paginaUnidades + 1}</span><button type="button" disabled={(dados.paginaUnidades + 1) * dados.tamanhoPagina >= dados.totalUnidades} onClick={() => alterar({ paginaUnidades: dados.paginaUnidades + 1 })}>Próxima</button></nav>
      <div className={estilos.faixaDiagnostico}><AlertasOperacionais dados={dados} abrir={abrir} /><DiagnosticoRecorte dados={dados} abrir={abrir} /></div>
      <details className={estilos.metodologia}><summary>Fontes e regras dos indicadores</summary><p>Versão {dados.versaoMetodologia} · gerado em {new Date(dados.geradoEm).toLocaleString('pt-BR')}</p><ul>{[...dados.metodologia, ...dados.indisponibilidades].map(m => <li key={m}>{m}</li>)}</ul></details>
    </>}
    <ModalPainel aberto={Boolean(detalhe && dados)} titulo={detalhe?.titulo || ''} detalhe={detalhe ? intervalo(detalhe.faixa.inicio, detalhe.faixa.fim) : ''} aoFechar={fechar} selo="Origem do indicador" permitirTelaCheia={false} lateral>
      {detalhe && dados && <div className={estilos.detalhe}><p><strong>Escopo:</strong> {dados.escopo.nome}</p><p><strong>Contrato:</strong> {dados.filtros.contratos.find(c => c.id === dados.selecao.contratoId)?.nome || 'Todos no escopo'}</p><p><strong>Filtro de unidade:</strong> {dados.filtros.unidades.find(u => u.id === dados.selecao.unidadeId)?.nome || 'Todas no escopo'}</p>{detalhe.nota ? <p>{detalhe.nota}</p> : <><dl>{(Object.keys(nomesMetricas) as (keyof TotaisAnalitico)[]).map(m => <div key={m}><dt>{nomesMetricas[m]}</dt><dd>{numero(detalhe.totais[m])}</dd></div>)}</dl><p>Fonte: frequências operacionais válidas, por data de referência e fotografia organizacional. {dados.metodologia[0]}</p></>}{!detalhe.nota && dados.escopo.permiteDetalhamentoOperacional && <RegistrosOrigem key={`${detalhe.titulo}|${detalhe.faixa.inicio}|${detalhe.faixa.fim}`} dados={dados} detalhe={detalhe} aoAtualizar={() => {fechar();recarregar();}} />}{detalhe.rota && aoNavegar && <button type="button" onClick={() => { aoNavegar(detalhe.rota || '/painel'); fechar(); }}>Abrir módulo de origem</button>}<p className={estilos.nota}>Filtros e intervalo permanecem nesta análise. O módulo de origem aplica suas próprias permissões e filtros.</p></div>}
    </ModalPainel>
  </div>;
}
