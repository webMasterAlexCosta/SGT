import type { ExportTask, ExportPanelProps } from '../../contracts/exportacao';
import React from "react";
import {
  CheckCircle2,
  ChevronDown,
  ChevronUp,
  Clock3,
  FileDown,
  FileSpreadsheet,
  FileText,
  Layers3,
  LoaderCircle,
  X,
} from "lucide-react";
import estilos from "./PainelExportacoes.module.css";

const formatarNumero = (valor: (number)) => Number(valor || 0).toLocaleString("pt-BR");

const formatarTempo = (segundos: number | null | undefined) => {
  if (segundos === null || segundos === undefined || !Number.isFinite(Number(segundos))) return "calculando...";
  const total = Math.max(0, Math.round(Number(segundos)));
  if (total < 60) return `${total}s`;
  const minutos = Math.floor(total / 60);
  const segundosRestantes = total % 60;
  if (minutos < 60) return segundosRestantes ? `${minutos}min ${segundosRestantes}s` : `${minutos}min`;
  const horas = Math.floor(minutos / 60);
  const minutosRestantes = minutos % 60;
  return minutosRestantes ? `${horas}h ${minutosRestantes}min` : `${horas}h`;
};

const formatarTamanho = (bytes: number | null | undefined) => {
  const valor = Number(bytes || 0);
  if (valor <= 0) return null;
  if (valor < 1024 * 1024) return `${(valor / 1024).toFixed(1)} KB`;
  return `${(valor / (1024 * 1024)).toFixed(1)} MB`;
};

const IconeFormato = ({ formato, size = 20 }: { formato?: string | null; size?: number }) => formato === "PDF"
  ? <FileText size={size} aria-hidden="true" />
  : <FileSpreadsheet size={size} aria-hidden="true" />;

const CartaoExportacao = ({ tarefa, aoFechar, aoBaixarNovamente, aoCancelar, cancelando, errosCancelamento }: { tarefa: ExportTask } & Pick<ExportPanelProps, "aoFechar" | "aoBaixarNovamente" | "aoCancelar" | "cancelando" | "errosCancelamento">) => {
  const falhou = Boolean(tarefa?.falhou || tarefa?.status === "FALHA");
  const concluido = Boolean(tarefa?.status === "BAIXADO" || tarefa?.status === "CONCLUIDO");
  const baixando = tarefa?.status === "BAIXANDO";
  const cancelada = tarefa.status === 'CANCELADO';
  const cancelamentoPendente = Boolean(tarefa.id && cancelando.has(tarefa.id));
  const podeCancelar = ['NA_FILA', 'PROCESSANDO'].includes(tarefa.status ?? '');
  const percentual = Math.max(0, Math.min(100, Number(tarefa?.percentual || 0)));
  const processados = Number(tarefa?.processados || 0);
  const total = Number(tarefa?.totalEstimado || 0);
  const loteAtual = processados > 0 ? Math.ceil(processados / 5000) : 0;
  const totalLotes = total > 0 ? Math.ceil(total / 5000) : 0;
  const tamanho = formatarTamanho(tarefa?.tamanhoBytes);
  const arquivoPronto = tarefa?.status === "CONCLUIDO" && Boolean(tarefa?.podeBaixar);
  const podeFechar = cancelada || falhou || tarefa?.status === "BAIXADO";

  return (
    <article
      className={`${estilos.cartao} ${falhou ? estilos.cartaoFalha : ""} ${concluido ? estilos.cartaoConcluido : ""}`}
      aria-live="polite"
    >
      <header className={estilos.cabecalhoCartao}>
        <div className={estilos.iconeArquivo}>
          {falhou ? <FileDown size={21} /> : concluido ? <CheckCircle2 size={21} /> : <IconeFormato formato={tarefa?.formato} size={21} />}
        </div>
        <div className={estilos.titulos}>
          <strong>{tarefa?.relatorio || "Relatório"}</strong>
          <span>{tarefa?.formato || "Arquivo"} · {cancelada ? 'exportação cancelada' : 'exportação completa'}</span>
        </div>
        {podeFechar && (
          <button type="button" className={estilos.botaoIcone} onClick={() => aoFechar(tarefa.id)} aria-label="Fechar aviso de exportação">
            <X size={17} />
          </button>
        )}
      </header>

      <div className={estilos.etapaLinha}>
        <span className={`${estilos.pontoStatus} ${falhou ? estilos.pontoFalha : concluido ? estilos.pontoConcluido : ""}`} />
        <span>{cancelada ? 'Cancelada' : tarefa?.etapa || "Preparando exportação"}</span>
        {!cancelada && !falhou && !concluido && !baixando && <LoaderCircle className={estilos.girando} size={16} />}
      </div>

      {!falhou && !cancelada && (
        <>
          <div className={estilos.progressoCabecalho}>
            <span>{total > 0 ? `${formatarNumero(processados)} de ${formatarNumero(total)}` : `${formatarNumero(processados)} processados`}</span>
            <strong>{percentual}%</strong>
          </div>
          <div className={estilos.trilhoProgresso} aria-label={`Progresso ${percentual}%`}>
            <div className={estilos.barraProgresso} style={{ width: `${percentual}%` }} />
          </div>
        </>
      )}

      <p className={estilos.mensagem}>{tarefa?.mensagem}</p>

      {!cancelada && !falhou && !concluido && (
        <div className={estilos.metricas}>
          <div>
            <Clock3 size={15} />
            <span><small>Tempo restante</small><strong>≈ {formatarTempo(tarefa?.segundosRestantesEstimados)}</strong></span>
          </div>
          <div>
            <Layers3 size={15} />
            <span><small>Lotes processados</small><strong>{totalLotes > 0 ? `${loteAtual}/${totalLotes}` : loteAtual || "—"}</strong></span>
          </div>
        </div>
      )}

      {(concluido || baixando) && (
        <div className={estilos.resultado}>
          <CheckCircle2 size={17} />
          <span>
            {baixando
              ? "Arquivo pronto. Transferência solicitada ao navegador."
              : tarefa?.status === "BAIXADO"
                ? "Download concluído com sucesso."
                : "Exportação concluída e pronta para download."}
          </span>
          {tamanho && <strong>{tamanho}</strong>}
        </div>
      )}

      {arquivoPronto && (
        <button
          type="button"
          className={estilos.botaoDownload}
          onClick={() => aoBaixarNovamente?.(tarefa.id)}
        >
          <FileDown size={16} />
          Baixar arquivo
        </button>
      )}

      {tarefa.id && errosCancelamento[tarefa.id] && <p role="alert" className={estilos.erroCancelamento}>{errosCancelamento[tarefa.id]}</p>}
      {podeCancelar && <button type="button" className={estilos.botaoCancelar} disabled={cancelamentoPendente} onClick={() => { void aoCancelar(tarefa.id); }}>{cancelamentoPendente ? 'Cancelando exportação…' : 'Cancelar exportação'}</button>}
      {!cancelada && !falhou && !concluido && !baixando && (
        <div className={estilos.rodapeInformativo}>
          Você pode continuar usando o sistema. O arquivo será baixado automaticamente quando estiver pronto.
        </div>
      )}
    </article>
  );
};

export const PainelExportacoes = ({ tarefas = [], minimizado, aoAlternarMinimizado, aoFechar, aoBaixarNovamente, aoCancelar, cancelando, errosCancelamento }: ExportPanelProps) => {
  const visiveis = tarefas;
  if (!visiveis.length) return null;

  const emAndamento = visiveis.filter((item) => !item?.falhou && !["CONCLUIDO", "BAIXADO", "CANCELADO", "FALHA"].includes(item?.status ?? '')).length;

  return (
    <aside className={`${estilos.painel} ${minimizado ? estilos.painelMinimizado : ""}`} aria-label="Exportação de documento">
      <button type="button" className={estilos.cabecalhoPainel} onClick={aoAlternarMinimizado} aria-expanded={!minimizado}>
        <span className={estilos.iconePainel}><FileDown size={18} /></span>
        <span className={estilos.textoPainel}>
          <strong>Exportação de documento</strong>
          <small>{emAndamento > 0 ? `${emAndamento} arquivo(s) sendo preparado(s)` : "Exportações recentes"}</small>
        </span>
        {minimizado ? <ChevronUp size={18} /> : <ChevronDown size={18} />}
      </button>

      {!minimizado && (
        <div className={estilos.lista}>
          {visiveis.map((tarefa) => (
            <CartaoExportacao key={tarefa.id} tarefa={tarefa} aoFechar={aoFechar} aoBaixarNovamente={aoBaixarNovamente} aoCancelar={aoCancelar} cancelando={cancelando} errosCancelamento={errosCancelamento} />
          ))}
        </div>
      )}
    </aside>
  );
};
