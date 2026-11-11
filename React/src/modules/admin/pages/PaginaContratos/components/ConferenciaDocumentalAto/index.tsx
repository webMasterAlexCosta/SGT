import { useId } from 'react';
import type { ConferenciaDocumentalAtoDTO } from '../../../../../../contracts/java';
import { DECLARACAO_CONFERENCIA_TRANSFERENCIA } from '../../../../../../services/admin/conferenciaDocumentalTransferencia';

export const ConferenciaDocumentalAto = ({ confirmada, desabilitada, aoMudar, className }: {
  confirmada: boolean; desabilitada?: boolean; aoMudar: (confirmada: boolean) => void; className?: string;
}) => {
  const descricaoId = useId();
  return <label className={className}>
  <input type="checkbox" aria-label="Conferência documental deste ato" aria-describedby={descricaoId} required checked={confirmada} disabled={desabilitada}
    onChange={evento => aoMudar(evento.target.checked)} />
  <span id={descricaoId}>{DECLARACAO_CONFERENCIA_TRANSFERENCIA}<small style={{ display: 'block' }}>Declaração de conferência — versão 1. Válida somente para a intenção acima.</small></span>
</label>;
};

export const ConferenciaDocumentalRegistrada = ({ conferencia }: { conferencia?: ConferenciaDocumentalAtoDTO | null }) => {
  if (!conferencia) return null;
  return <details>
    <summary>Conferência documental registrada neste ato</summary>
    <p>{conferencia.declaracao}</p>
    <dl>
      <div><dt>Referência da autorização do ato</dt><dd>{conferencia.autorizacaoAdministrativaAto}</dd></div>
      <div><dt>Versão da declaração</dt><dd>{conferencia.versaoDeclaracao}</dd></div>
      <div><dt>Uso registrado da concessão</dt><dd>Uso #{conferencia.usoPermissaoExcepcionalId} · Concessão #{conferencia.permissaoExcepcionalId}</dd></div>
    </dl>
    <details><summary>Intenção preservada deste ato</summary><pre style={{ whiteSpace: 'pre-wrap', overflowWrap: 'anywhere', maxHeight: '16rem', overflow: 'auto' }}>{conferencia.intencaoCanonica}</pre></details>
  </details>;
};
