import React, { useEffect, useMemo, useState } from "react";
import { AlertTriangle, CalendarClock, FileSignature, Send, X } from "lucide-react";
import { useDialogoAcessivel } from "../../../../../../core/ui/acessibilidadeDialogo.js";
import estilos from "./index.module.css";

const CONFIGURACOES = Object.freeze({
  PUBLICAR: {
    titulo: "Publicar contrato",
    descricao: "A publicação torna o rascunho um instrumento negocial e bloqueia edição livre.",
    exigeDataAto: false,
    exigeReferencia: true,
    rotuloData: "Data da publicação",
  },
  INATIVAR: {
    titulo: "Inativar administrativamente",
    descricao: "A inativação não encerra vigência nem vínculos; registra bloqueio operacional em data efetiva.",
    exigeDataAto: false,
    exigeReferencia: false,
    rotuloData: "Data efetiva da inativação",
  },
  REATIVAR: {
    titulo: "Reativar administrativamente",
    descricao: "A reativação é um novo evento auditável e não apaga a inativação anterior.",
    exigeDataAto: false,
    exigeReferencia: false,
    rotuloData: "Data efetiva da reativação",
  },
  ENCERRAR_ANTECIPADAMENTE: {
    titulo: "Encerrar antecipadamente",
    descricao: "Reduz a data final vigente e pode encerrar vínculos posteriores. Exige fonte comprobatória.",
    exigeDataAto: false,
    exigeReferencia: true,
    rotuloData: "Nova data final efetiva",
  },
  CANCELAR_FUTURO: {
    titulo: "Cancelar contrato futuro",
    descricao: "Disponível somente para contrato publicado que ainda não iniciou. O identificador permanece reservado.",
    exigeDataAto: false,
    exigeReferencia: false,
    rotuloData: "Data efetiva do cancelamento",
  },
  ANULAR: {
    titulo: "Anular contrato",
    descricao: "A anulação preserva o contrato e seus fatos para auditoria, mas retira validade operacional.",
    exigeDataAto: true,
    exigeReferencia: true,
    rotuloData: "Data efetiva de alcance",
  },
});

const hoje = () => new Date().toISOString().slice(0, 10);

export const ModalAcaoContrato = ({ aberto, tipoAcao, contrato, processando, aoFechar, aoConfirmar }) => {
  const config = CONFIGURACOES[tipoAcao] || null;
  const [dados, setDados] = useState({ dataEfetiva: hoje(), dataAto: hoje(), referenciaSei: "", motivo: "" });
  const referenciaModal = useDialogoAcessivel({ aberto, aoFechar, bloquearFechamento: Boolean(processando) });

  useEffect(() => {
    if (!aberto) return;
    setDados({ dataEfetiva: hoje(), dataAto: hoje(), referenciaSei: "", motivo: "" });
  }, [aberto, tipoAcao, contrato?.id]);

  const publicar = tipoAcao === "PUBLICAR";
  const valido = useMemo(() => {
    if (!config) return false;
    if (!dados.dataEfetiva) return false;
    if (config.exigeDataAto && !dados.dataAto) return false;
    if (config.exigeReferencia && !dados.referenciaSei.trim()) return false;
    if (!publicar && !dados.motivo.trim()) return false;
    return true;
  }, [config, dados, publicar]);

  if (!aberto || !config) return null;

  const alterar = (campo, valor) => setDados((atual) => ({ ...atual, [campo]: valor }));
  const submeter = (evento) => {
    evento.preventDefault();
    if (!valido || processando) return;
    if (publicar) {
      aoConfirmar({ dataPublicacao: dados.dataEfetiva, referenciaSei: dados.referenciaSei.trim(), justificativa: dados.motivo.trim() || "Publicação do contrato" });
      return;
    }
    aoConfirmar({
      dataEfetiva: dados.dataEfetiva,
      dataAto: config.exigeDataAto ? dados.dataAto : (dados.dataAto || null),
      motivo: dados.motivo.trim(),
      referenciaSei: dados.referenciaSei.trim() || null,
      eventoPrecedenteId: null,
      relacaoPrecedencia: null,
      ordemDocumental: null,
    });
  };

  return (
    <div className={estilos.sobreposicao} onMouseDown={(e) => e.target === e.currentTarget && !processando && aoFechar()} role="presentation">
      <section ref={referenciaModal} tabIndex={-1} role="dialog" aria-modal="true" aria-labelledby="titulo-acao-contrato" className={estilos.modal}>
        <header className={estilos.cabecalho}>
          <div><span>Ação de ciclo de vida</span><h2 id="titulo-acao-contrato">{config.titulo}</h2><small>{contrato?.numeroContrato || "Contrato"}{contrato?.anoContrato ? `/${contrato.anoContrato}` : ""}</small></div>
          <button type="button" onClick={aoFechar} disabled={processando} aria-label="Fechar"><X size={20} /></button>
        </header>
        <form onSubmit={submeter} className={estilos.formulario}>
          <div className={estilos.alerta}><AlertTriangle size={18} /><p>{config.descricao}</p></div>
          {publicar && <div className={estilos.observacao}><FileSignature size={17} /><span>O servidor validará vigência completa, empresa, Tipo de Contratação, fonte comprobatória e composição original consistente antes de publicar.</span></div>}
          <div className={estilos.grade}>
            <label><span>{config.rotuloData} *</span><div className={estilos.comIcone}><CalendarClock size={16} /><input type="date" value={dados.dataEfetiva} onChange={(e) => alterar("dataEfetiva", e.target.value)} required /></div></label>
            {config.exigeDataAto && <label><span>Data do ato anulatório *</span><input type="date" value={dados.dataAto} onChange={(e) => alterar("dataAto", e.target.value)} required /></label>}
            <label className={estilos.larguraTotal}><span>Referência SEI {config.exigeReferencia ? "*" : "(opcional)"}</span><input value={dados.referenciaSei} onChange={(e) => alterar("referenciaSei", e.target.value)} required={config.exigeReferencia} maxLength={120} placeholder="Documento/processo que comprova o ato" /></label>
            <label className={estilos.larguraTotal}><span>{publicar ? "Justificativa" : "Motivo/justificativa *"}</span><textarea rows={4} value={dados.motivo} onChange={(e) => alterar("motivo", e.target.value)} required={!publicar} maxLength={1000} /></label>
          </div>
          <footer className={estilos.rodape}><button type="button" className={estilos.cancelar} onClick={aoFechar} disabled={processando}>Cancelar</button><button type="submit" className={estilos.confirmar} disabled={!valido || processando}><Send size={17} />{processando ? "Registrando..." : "Continuar e confirmar senha"}</button></footer>
        </form>
      </section>
    </div>
  );
};
