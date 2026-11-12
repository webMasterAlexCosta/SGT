import { useEffect, useState } from 'react';
import type { PainelVisoesDTO, VisaoGerencial } from '../../../../contracts/painel-visoes';
import type { ActivitySummary } from '../../../../contracts/painel';
import { ServicoPainel } from '../../../../services/painel/ServicoPainel';
import { obterGeracaoSessao, obterSinalSessao, sessaoAtual } from '../../../../core/security/geracaoSessao';
import { IntegridadeDados, MatrizUnidades, SaudeContratual } from '../GestaoExecutiva';
import { AnaliseCadastral, QuadroPessoal, HorizonteContratual, SituacaoDetalhadaQuadro, ComparativoUnidades, PrioridadesPorUnidade, ImpactoAbsenteismo, PressaoOperacional, DistribuicaoStatus, PadraoSemanal, RankingLotacaoTurno, AuditoriaRecente, ComparativoPeriodo } from '../FerramentasGestao';
import estilos from './index.module.css';

function Atividades({ podeAbrir, navegar }: { podeAbrir: boolean; navegar: (rota: string) => void }) {
  const [atividades, setAtividades] = useState<ActivitySummary[]>([]);
  const [erro, setErro] = useState(false);
  useEffect(() => {
    const controller = new AbortController(), geracao = obterGeracaoSessao();
    const sinal = obterSinalSessao(geracao);
    const abortar = () => { controller.abort(); setAtividades([]); };
    sinal.addEventListener('abort', abortar, { once: true });
    ServicoPainel.buscarAtividadesRecentes(undefined, { signal: controller.signal, geracaoSessao: geracao }).then(d => {
      if (!controller.signal.aborted && sessaoAtual(geracao)) setAtividades(d.content);
    }).catch(() => { if (!controller.signal.aborted) setErro(true); });
    return () => { controller.abort(); sinal.removeEventListener('abort', abortar); };
  }, []);
  return <><p className={estilos.nota}>Atividade recente do escopo autenticado; não representa uma série do intervalo analítico.</p>{erro && <p role="alert">Não foi possível carregar a atividade recente.</p>}<AuditoriaRecente atividades={atividades} podeAbrirLogs={podeAbrir} aoNavegar={navegar} /></>;
}

export function ComplementosGerenciais({ resposta, visao, perfil, navegar }: { resposta: PainelVisoesDTO; visao: VisaoGerencial; perfil: Record<string, boolean>; navegar: (rota: string) => void }) {
  const d = resposta.gestao;
  const contratos = visao === 'contratos' || visao === 'tudo', operacao = visao === 'operacao' || visao === 'tudo';
  return <div className={estilos.complementos}>
    {contratos && <>
      <section className={estilos.grade}><SaudeContratual contratos={d.contratos} efetivo={d.efetivo} podeAbrir={perfil.podeVisualizarContratos} aoNavegar={navegar} /><HorizonteContratual horizonte={d.horizonteContratual} /></section>
      <section className={estilos.grade}><QuadroPessoal quadro={d.quadro} /><SituacaoDetalhadaQuadro dados={d.situacaoQuadro} /></section>
      <AnaliseCadastral analise={d.analiseCadastro} admissoesMensais={d.admissoesMensais} periodoDias={d.periodo.dias} />
      <section className={estilos.grade}><ComparativoUnidades unidades={d.unidades} nivel={d.escopo.nivel ?? undefined} /><PrioridadesPorUnidade unidades={d.unidades} nivel={d.escopo.nivel ?? undefined} /></section>
      <MatrizUnidades unidades={d.unidades} nivel={d.escopo.nivel ?? undefined} />
    </>}
    {operacao && <>
      <section className={estilos.grade}><ImpactoAbsenteismo periodo={d.periodo} /><PressaoOperacional pressao={d.pressaoOperacional} /></section>
      <section className={estilos.grade}><DistribuicaoStatus dados={d.distribuicaoStatus} periodo={d.periodo} /><PadraoSemanal dados={d.padraoSemanal} /></section>
      {resposta.analitico.selecao.comparar && d.comparativoPeriodo && <ComparativoPeriodo periodo={d.periodo} comparativo={d.comparativoPeriodo} />}
      {!perfil.presidente && <RankingLotacaoTurno dados={resposta.ranking} periodoDias={d.periodo.dias} />}
      <IntegridadeDados integridade={d.integridade} />
      {!perfil.presidente && <details className={estilos.explorador}><summary>Atividade recente no escopo autenticado</summary><Atividades podeAbrir={perfil.podeAcessarAuditoria} navegar={navegar} /></details>}
    </>}
  </div>;
}
