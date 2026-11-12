import { CalendarDays, RefreshCw } from 'lucide-react';
import type { GranularidadePainel, PainelAnaliticoDTO, ParametrosAnalitico } from '../../../../contracts/painel-analitico';
import { hojeCivil } from './useVisoes';
import estilos from '../PainelDesempenho/index.module.css';

export const PERIODOS: { id: GranularidadePainel; nome: string }[] = [
  { id: 'DIARIO', nome: 'Diário' }, { id: 'SEMANAL', nome: 'Semanal' }, { id: 'MENSAL', nome: 'Mensal' },
  { id: 'TRIMESTRAL', nome: 'Trimestral' }, { id: 'ANUAL', nome: 'Anual' },
];
interface Props {
  filtros: ParametrosAnalitico;
  opcoes: PainelAnaliticoDTO['filtros'] | null;
  alterar: (filtros: Partial<ParametrosAnalitico>) => void;
  recarregar: () => void;
  limpar: () => void;
}
export function FiltrosGerenciais({ filtros, opcoes, alterar, recarregar, limpar }: Props) {
  return <section className={`${estilos.painel} ${estilos.painelFiltros}`} aria-label="Filtros gerenciais">
    <div className={estilos.cabecalhoFiltro}>
      <div className={estilos.segmentado} role="group" aria-label="Período de análise">
        {PERIODOS.map(p => <button key={p.id} type="button" aria-pressed={filtros.granularidade === p.id} onClick={() => alterar({ granularidade: p.id })}>{p.nome}</button>)}
      </div>
      <label className={estilos.dataReferencia}><CalendarDays size={16} aria-hidden="true" />Referência
        <input aria-label="Data de referência" type="date" min={String(Number(hojeCivil().slice(0, 4)) - 2) + hojeCivil().slice(4)} max={hojeCivil()} value={filtros.referencia} onChange={e => { if (e.target.value) alterar({ referencia: e.target.value }); }} />
      </label>
      <label className={estilos.checkbox}><input type="checkbox" checked={filtros.comparar} onChange={e => alterar({ comparar: e.target.checked })} />Comparar período anterior</label>
      <button type="button" className={estilos.atualizar} aria-label="Atualizar indicadores" onClick={recarregar}><RefreshCw size={16} aria-hidden="true" /></button>
    </div>
    <div className={estilos.filtrosRecorte}>
      <label>Contrato<select value={filtros.contratoId ?? ''} onChange={e => alterar({ contratoId: e.target.value ? Number(e.target.value) : undefined })}><option value="">Todos os contratos</option>{opcoes?.contratos.map(c => <option key={c.id} value={c.id}>{c.nome}</option>)}</select></label>
      <label>Prestador / empresa<select value={filtros.prestadorId ?? ''} onChange={e => alterar({ prestadorId: e.target.value ? Number(e.target.value) : undefined, contratoId: undefined })}><option value="">Todos os prestadores</option>{opcoes?.prestadores.map(p => <option key={p.id} value={p.id}>{p.nome}</option>)}</select></label>
      <label>Unidade<select value={filtros.unidadeId ?? ''} onChange={e => alterar({ unidadeId: e.target.value ? Number(e.target.value) : undefined, tipoUnidade: opcoes?.tipoUnidade, contratoId: undefined })}><option value="">Todas as unidades</option>{opcoes?.unidades.map(u => <option key={u.id} value={u.id}>{u.nome}</option>)}</select></label>
      <button type="button" onClick={limpar}>Limpar filtros</button>
    </div>
  </section>;
}
