import React from 'react';
import { Ban, CheckCircle2, RefreshCw, RotateCcw, ShieldCheck } from 'lucide-react';
import { calcularPercentualFechamento } from '../../../../../services/rh/ServicoDominioFrequencia.js';
import estilos from './PainelFechamentoFrequencia.module.css';

const TERMINAIS_REPROCESSAVEIS = new Set(['ERRO', 'CANCELADO']);

const formatarDataHora = (valor) => {
  if (!valor) return '-';
  const data = new Date(valor);
  return Number.isFinite(data.getTime()) ? data.toLocaleString('pt-BR') : String(valor);
};

export const PainelFechamentoFrequencia = ({
  fechamento,
  ativo,
  carregando,
  erro,
  podeGerenciar,
  filtrosProntos,
  possuiRascunhos,
  aoCriar,
  aoCancelar,
  aoReprocessar,
  aoAtualizar,
}) => {
  const status = String(fechamento?.status || '').toUpperCase();
  const percentual = calcularPercentualFechamento(fechamento);
  const podeReprocessar = TERMINAIS_REPROCESSAVEIS.has(status);

  return (
    <section className={estilos.painel} aria-labelledby="titulo-fechamento-frequencia">
      <div className={estilos.cabecalho}>
        <div>
          <span className={estilos.sobreTitulo}><ShieldCheck size={16} aria-hidden="true" /> Fechamento autoritativo</span>
          <h2 id="titulo-fechamento-frequencia">Fechamento do escopo</h2>
          <p>O processamento, estados e validações pertencem ao Java. O React apenas acompanha o comando assíncrono.</p>
        </div>
        <button type="button" className={estilos.botaoSecundario} onClick={aoAtualizar} disabled={carregando || !filtrosProntos}>
          <RefreshCw size={16} aria-hidden="true" /> Atualizar
        </button>
      </div>

      {!filtrosProntos ? (
        <div className={estilos.aviso}>Selecione lotação e turno para consultar ou iniciar um fechamento.</div>
      ) : erro ? (
        <div className={estilos.erro} role="alert">{erro}</div>
      ) : fechamento ? (
        <>
          <div className={estilos.gradeResumo}>
            <div><span>Status</span><strong>{status || '-'}</strong></div>
            <div><span>Processado</span><strong>{Number(fechamento.totalProcessado || 0).toLocaleString('pt-BR')} / {Number(fechamento.totalPrevisto || 0).toLocaleString('pt-BR')}</strong></div>
            <div><span>Tentativa</span><strong>{fechamento.tentativa ?? '-'}</strong></div>
            <div><span>Atualizado</span><strong>{formatarDataHora(fechamento.atualizadoEm)}</strong></div>
          </div>
          <div className={estilos.progresso} aria-label={`Progresso do fechamento: ${percentual}%`}>
            <span style={{ width: `${percentual}%` }} />
          </div>
          <div className={estilos.linhaStatus}>
            <strong>{percentual}%</strong>
            <span>{fechamento.mensagem || fechamento.detalheErro || (ativo ? 'Processamento em andamento.' : 'Processamento concluído.')}</span>
          </div>
          {fechamento.cancelamentoSolicitado && <div className={estilos.aviso}>Cancelamento já solicitado ao servidor.</div>}
          {podeGerenciar && (
            <div className={estilos.acoes}>
              {ativo && (
                <button type="button" className={estilos.botaoPerigo} onClick={aoCancelar}>
                  <Ban size={16} aria-hidden="true" /> Solicitar cancelamento
                </button>
              )}
              {podeReprocessar && (
                <button type="button" className={estilos.botaoPrimario} onClick={aoReprocessar}>
                  <RotateCcw size={16} aria-hidden="true" /> Reprocessar
                </button>
              )}
              {status === 'CONCLUIDO' && <span className={estilos.concluido}><CheckCircle2 size={16} /> Fechamento concluído</span>}
            </div>
          )}
        </>
      ) : (
        <div className={estilos.estadoVazio}>
          <span>Nenhum fechamento atual encontrado para o recorte selecionado.</span>
          {podeGerenciar && (
            <button type="button" className={estilos.botaoPrimario} onClick={aoCriar} disabled={possuiRascunhos || carregando}>
              <ShieldCheck size={16} aria-hidden="true" /> Iniciar fechamento
            </button>
          )}
          {possuiRascunhos && <small>Salve as alterações em rascunho antes de iniciar o fechamento.</small>}
        </div>
      )}
    </section>
  );
};
