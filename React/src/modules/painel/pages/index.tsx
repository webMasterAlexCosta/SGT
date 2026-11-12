import { lazy, Suspense, useState } from 'react';
import { LayoutDashboard } from 'lucide-react';
import type { VisaoGerencial } from '../../../contracts/painel-visoes';
import { usePainel } from './index';
import estilos from './index.module.css';

const PainelDesempenho = lazy(() => import('../components/PainelDesempenho'));
const PainelGerencial = lazy(() => import('../components/PainelGerencial'));
type Visao = 'desempenho' | VisaoGerencial;
const VISOES: { id: Visao; nome: string }[] = [
  { id: 'desempenho', nome: 'Desempenho' }, { id: 'resumo', nome: 'Resumo' },
  { id: 'contratos', nome: 'Contratos' }, { id: 'operacao', nome: 'Operação' }, { id: 'tudo', nome: 'Tudo' },
];
export const Painel = ({ onNavigate }: { onNavigate: (caminho: string) => void }) => {
  const [visao, setVisao] = useState<Visao>('desempenho');
  // O hook legado fornece somente perfil/referência; não inicia cálculo nem polling.
  const { referencia, perfil, acoes } = usePainel(onNavigate, false);
  return <div className={estilos.pagina}>
    <nav className={estilos.navegacaoPainel} aria-label="Modo de visualização do painel">
      <span><LayoutDashboard size={16} aria-hidden="true" />Modo foco</span>
      {VISOES.map(v => <button key={v.id} type="button" aria-pressed={visao === v.id} className={visao === v.id ? estilos.navegacaoAtiva : ''} onClick={() => setVisao(v.id)}>{v.nome}</button>)}
    </nav>
    <Suspense fallback={<p role="status">Abrindo painel gerencial…</p>}>
      {visao === 'desempenho' ? <PainelDesempenho referenciaInicial={referencia} aoNavegar={onNavigate} /> : <PainelGerencial visao={visao} referenciaInicial={referencia} perfil={perfil} aoNavegar={acoes.navegar} />}
    </Suspense>
  </div>;
};
