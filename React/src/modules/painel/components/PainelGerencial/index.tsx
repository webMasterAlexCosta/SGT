import { useState } from 'react';
import { AlertTriangle, Download, FileText } from 'lucide-react';
import type { VisaoGerencial } from '../../../../contracts/painel-visoes';
import type { ParametrosAnalitico } from '../../../../contracts/painel-analitico';
import { useVisoes } from './useVisoes';
import { FiltrosGerenciais, PERIODOS } from './FiltrosGerenciais';
import { IndicadoresGerenciais } from './IndicadoresGerenciais';
import { ComplementosGerenciais } from './ComplementosGerenciais';
import { ContratosGerenciais } from './ContratosGerenciais';
import { exportarVisao } from './exportacao';
import { Evolucao, Heatmap, type DetalheDesempenho } from '../PainelDesempenho/GraficosDesempenho';
import { MetasProgresso, FunilCobertura, AlertasOperacionais, DiagnosticoRecorte } from '../PainelDesempenho/BlocosDesempenho';
import { RegistrosOrigem } from '../PainelDesempenho/RegistrosOrigem';
import { dataCivil, intervalo, nomesMetricas, numero } from '../PainelDesempenho/formatos';
import { ModalPainel } from '../ModalPainel';
import { IndicadorEfetivo } from '../IndicadorEfetivo';
import { AtalhosGerenciais } from '../FerramentasGestao';
import base from '../PainelDesempenho/index.module.css';
import estilos from './index.module.css';

const TITULOS = {
  resumo: ['Resumo gerencial', 'Efetivo, contratos e prioridades para a decisão gerencial.'],
  contratos: ['Contratos e dimensionamento', 'Vigência, ocupação e capacidade contratada no recorte selecionado.'],
  operacao: ['Operação e frequência', 'Presenças, ausências, pendências e evolução da operação.'],
  tudo: ['Painel gerencial', 'Visão integrada do efetivo, dos contratos e da operação.'],
};
interface Props { visao: VisaoGerencial; referenciaInicial: string; perfil: Record<string, boolean>; aoNavegar: (rota: string) => void; }
export default function PainelGerencial({ visao, referenciaInicial, perfil, aoNavegar }: Props) {
  const consulta = useVisoes(referenciaInicial);
  const { filtros, dados: resposta, opcoes, erro, carregando, recarregar } = consulta;
  const [aberto, setAberto] = useState<{ visao: VisaoGerencial; detalhe: DetalheDesempenho } | null>(null);
  const [erroExportacao, setErroExportacao] = useState<string | null>(null);
  const [efetivoAberto, setEfetivoAberto] = useState(false);
  const dados = resposta?.analitico;
  const detalhe = aberto?.visao === visao ? aberto.detalhe : null;
  const abrir = (d: DetalheDesempenho) => setAberto({ visao, detalhe: d });
  const fechar = () => setAberto(null);
  const alterar = (f: Partial<ParametrosAnalitico>) => { fechar(); consulta.alterar(f); };
  const exportar = async (formato: 'xlsx' | 'pdf') => {
    if (!resposta) return;
    setErroExportacao(null);
    try { await exportarVisao(resposta, visao, formato); }
    catch { setErroExportacao('Não foi possível gerar o arquivo. Tente novamente.'); }
  };
  const contratos = visao === 'contratos' || visao === 'tudo';
  const resumo = visao === 'resumo' || visao === 'tudo';
  return <div className={base.desempenho}>
    <header className={base.titulo}><div><h1>{TITULOS[visao][0]}</h1><p>{TITULOS[visao][1]}</p></div>
      <div className={base.exportacoes}><button type="button" disabled={!resposta} onClick={() => void exportar('xlsx')}><Download size={16} aria-hidden="true" />Exportar XLSX</button><button type="button" disabled={!resposta} onClick={() => void exportar('pdf')}><FileText size={16} aria-hidden="true" />PDF</button></div>
    </header>
    <FiltrosGerenciais filtros={filtros} opcoes={opcoes} alterar={alterar} recarregar={() => { fechar(); recarregar(); }} limpar={() => { fechar(); consulta.limpar(); }} />
    {erro && <div role="alert" className={base.erro}><AlertTriangle size={19} aria-hidden="true" /><span>{erro}</span><button type="button" onClick={recarregar}>Tentar novamente</button></div>}
    {erroExportacao && <p role="alert" className={base.erro}>{erroExportacao}</p>}
    <div role="status" aria-live="polite" className={base.statusConsulta}>{carregando && <p>Consultando indicadores reais no servidor…</p>}</div>
    {dados && resposta && <>
      <div className={base.intervaloAplicado}><div><strong>{PERIODOS.find(p => p.id === dados.intervalo.granularidade)?.nome} · {intervalo(dados.intervalo.inicio, dados.intervalo.fim)}</strong><span>{dados.intervalo.parcial ? 'Período parcial' : 'Período completo'}</span><small>{dados.comparativo ? `Anterior equivalente: ${intervalo(dados.intervalo.anteriorInicio, dados.intervalo.anteriorFim)}` : 'Comparação desligada'}</small></div><div className={base.contextoConsulta}><small>{dados.escopo.nome}</small><small>Atualizado em <time dateTime={dados.geradoEm}>{new Date(dados.geradoEm).toLocaleString('pt-BR')}</time></small></div></div>
      <IndicadoresGerenciais dados={dados} visao={visao} abrir={abrir} />
      <nav className={estilos.navegacaoSecoes} aria-label="Seções da análise"><a href="#evolucao-gerencial">Evolução e comparação</a>{contratos && <a href="#contratos-recorte">Contratos e capacidade</a>}<a href="#diagnostico-gerencial">Detalhamento</a>{!resumo && <a href="#complementos-gerenciais">Análises específicas</a>}</nav>
      {contratos && <ContratosGerenciais resposta={resposta} abrir={abrir} />}
      <section id="evolucao-gerencial" className={base.faixaAnalise}><Evolucao dados={dados} abrir={abrir} /><MetasProgresso dados={dados} abrir={abrir} /></section>
      <section className={base.faixaCobertura}><Heatmap dados={dados} abrir={abrir} /><FunilCobertura dados={dados} abrir={abrir} /></section>
      <section id="diagnostico-gerencial" className={base.faixaDiagnostico}><DiagnosticoRecorte dados={dados} abrir={abrir} /><AlertasOperacionais dados={dados} abrir={abrir} /></section>
      <nav className={base.paginacao} aria-label="Páginas das unidades"><button type="button" disabled={!dados.paginaUnidades} onClick={() => alterar({ paginaUnidades: dados.paginaUnidades - 1 })}>Anterior</button><span>{dados.totalUnidades} unidade(s) · página {dados.paginaUnidades + 1}</span><button type="button" disabled={(dados.paginaUnidades + 1) * dados.tamanhoPagina >= dados.totalUnidades} onClick={() => alterar({ paginaUnidades: dados.paginaUnidades + 1 })}>Próxima</button></nav>
      <section id="complementos-gerenciais"><ComplementosGerenciais resposta={resposta} visao={visao} perfil={perfil} navegar={aoNavegar} /></section>
      {(resumo || contratos) && <details className={estilos.explorador} onToggle={e => setEfetivoAberto(e.currentTarget.open)}><summary>Explorar efetivo por lotação, turno e cargo</summary><p className={estilos.nota}>Análise específica de efetivo com filtros próprios, incluindo turno e cargo. Inicia na referência {dataCivil(dados.referencia)}; seus controles e exportações se aplicam somente a esta análise.</p>{efetivoAberto && <IndicadorEfetivo referencia={dados.referencia} />}</details>}
      {resumo && <AtalhosGerenciais perfil={perfil} aoNavegar={aoNavegar} />}
      <details className={base.metodologia}><summary>Fontes e regras dos indicadores</summary><p>Fotografias contratuais e de escala usam a referência; registros usam o intervalo selecionado. Indicadores cadastrais usam o cadastro consultado. As tabelas de unidades acompanham a página atual.</p><ul>{[...dados.metodologia, ...dados.indisponibilidades, ...resposta.gestao.notasMetodologicas].map((m, i) => <li key={`${i}-${m}`}>{m}</li>)}</ul></details>
    </>}
    <ModalPainel aberto={Boolean(detalhe && dados)} titulo={detalhe?.titulo || ''} detalhe={detalhe ? intervalo(detalhe.faixa.inicio, detalhe.faixa.fim) : ''} aoFechar={fechar} selo="Origem do indicador" permitirTelaCheia={false} lateral>
      {detalhe && dados && <div className={base.detalhe}><p><strong>Escopo:</strong> {dados.escopo.nome}</p><p><strong>Contrato:</strong> {dados.filtros.contratos.find(c => c.id === dados.selecao.contratoId)?.nome || 'Todos no escopo'}</p><p><strong>Prestador:</strong> {dados.filtros.prestadores.find(c => c.id === dados.selecao.prestadorId)?.nome || 'Todos no escopo'}</p><p><strong>Unidade:</strong> {dados.filtros.unidades.find(u => u.id === dados.selecao.unidadeId)?.nome || 'Todas no escopo'}</p>{detalhe.nota ? <p>{detalhe.nota}</p> : <dl>{(Object.keys(nomesMetricas) as (keyof typeof nomesMetricas)[]).map(m => <div key={m}><dt>{nomesMetricas[m]}</dt><dd>{numero(detalhe.totais[m])}</dd></div>)}</dl>}{!detalhe.nota && dados.escopo.permiteDetalhamentoOperacional && <RegistrosOrigem key={`${detalhe.titulo}|${detalhe.faixa.inicio}|${detalhe.faixa.fim}`} dados={dados} detalhe={detalhe} aoAtualizar={() => { fechar(); recarregar(); }} />}{detalhe.rota && <button type="button" onClick={() => { aoNavegar(detalhe.rota || '/painel'); fechar(); }}>Abrir módulo de origem</button>}<p className={base.nota}>Os filtros e o intervalo são mantidos nesta análise. O módulo de origem aplica suas próprias permissões e filtros.</p></div>}
    </ModalPainel>
  </div>;
}
