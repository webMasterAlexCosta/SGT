import React, { useEffect, useMemo, useRef, useState } from "react";
import { AlertTriangle, CalendarClock, FileSignature, Send, X } from "lucide-react";
import { useDialogoAcessivel } from "../../../../../../core/ui/acessibilidadeDialogo.js";
import { ServicoContrato } from "../../../../../../services/admin/ServicoContrato.js";
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
  CORRIGIR_ANULACAO: {
    titulo: "Corrigir anulação",
    descricao: "O ato anulatório permanece auditável. A correção mantém o contrato inativo até reativação explícita.",
    exigeDataAto: true,
    exigeReferencia: true,
    rotuloData: "Data efetiva de alcance",
  },
});

const OPERACOES = Object.freeze({
  INATIVAR: "inativar", REATIVAR: "reativar", ENCERRAR_ANTECIPADAMENTE: "encerrar-antecipadamente",
  CANCELAR_FUTURO: "cancelar-futuro", ANULAR: "anular", CORRIGIR_ANULACAO: "corrigir-anulacao",
});
const RELACOES = ["SUCEDE", "RETIFICA", "CANCELA", "SUBSTITUI", "MANTEM_COEXISTE"];
const hoje = () => {
  const agora = new Date();
  return `${agora.getFullYear()}-${String(agora.getMonth() + 1).padStart(2, "0")}-${String(agora.getDate()).padStart(2, "0")}`;
};
const lista = (valor) => Array.isArray(valor) ? valor : [];
const numeroOuNulo = (valor) => valor === "" || valor == null ? null : Number(valor);
const dadosNovos = (origem = {}) => ({
  dataEfetiva: origem.dataEfetiva || hoje(), dataAto: origem.dataAto || hoje(),
  referenciaSei: origem.referenciaSei || "", motivo: origem.motivo || "",
  eventoPrecedenteId: origem.eventoPrecedenteId || "", relacaoPrecedencia: origem.relacaoPrecedencia || "",
  ordemDocumental: origem.ordemDocumental ?? "",
});

const DetalhesPaginados = ({ titulo, itens, truncada, total, renderizar }) => {
  const [pagina, setPagina] = useState(0);
  const paginas = Math.max(1, Math.ceil(itens.length / 5));
  useEffect(() => { setPagina(0); }, [itens]);
  if (!itens.length && !total) return null;
  return <section className={estilos.detalhesImpacto}>
    <strong>{titulo} ({total ?? itens.length})</strong>
    {truncada && <p>O servidor retornou {itens.length} detalhes de {total ?? "um total maior"}. A contagem e a confirmação incluem todo o alcance.</p>}
    <ul>{itens.slice(pagina * 5, pagina * 5 + 5).map((item, indice) => <li key={item?.vinculoId ? `${item.tipo || "vinculo"}-${item.id || item.vinculoId}` : `${pagina}-${indice}`}>{renderizar(item)}</li>)}</ul>
    {paginas > 1 && <nav className={estilos.paginacao} aria-label={`Páginas de ${titulo}`}>
      <button type="button" disabled={pagina === 0} onClick={() => setPagina((p) => p - 1)}>Anterior</button>
      <span>{pagina + 1} / {paginas}</span>
      <button type="button" disabled={pagina + 1 >= paginas} onClick={() => setPagina((p) => p + 1)}>Próxima</button>
    </nav>}
  </section>;
};

export const ResumoPreviaContrato = ({ previa }) => {
  if (!previa) return null;
  return <section className={estilos.previa} aria-label="Impactos calculados pelo servidor">
    <h3>Prévia de impactos</h3>
    <dl className={estilos.metricas}>
      <div><dt>Data efetiva</dt><dd>{previa.dataEfetiva || "Não informada"}</dd></div>
      <div><dt>Vínculos</dt><dd>{previa.quantidadeVinculosAfetados ?? "Não informado"}</dd></div>
      <div><dt>Frequências</dt><dd>{previa.quantidadeFrequenciasAfetadas ?? "Não informado"}</dd></div>
      <div><dt>Programações futuras</dt><dd>{previa.quantidadeProgramacoesFuturasAfetadas ?? "Não informado"}</dd></div>
    </dl>
    {previa.autorizacaoNecessaria && <p>Autorização: {previa.autorizacaoNecessaria}</p>}
    <ul>{lista(previa.impactosConhecidos).map((impacto, indice) => <li key={`${indice}-${impacto}`}>{impacto}</li>)}</ul>
    <DetalhesPaginados titulo="Vínculos afetados" itens={lista(previa.vinculosAfetados)} total={previa.quantidadeVinculosAfetados} truncada={previa.vinculosTruncados} renderizar={(v) => <>Vínculo #{v.vinculoId} · {v.pessoaNome || "Nome não informado"} · matrícula {v.matricula || "não informada"} · {v.dataInicio || "—"} a {v.dataFim || "sem término informado"}{v.programado ? " · programado" : ""}</>} />
    <DetalhesPaginados titulo="Programações afetadas" itens={lista(previa.programacoesFuturasAfetadas)} total={previa.quantidadeProgramacoesFuturasAfetadas} truncada={previa.programacoesTruncadas} renderizar={(p) => <>{p.tipo} #{p.id} · {p.dataEfetiva} · {p.descricao || "Sem descrição"}</>} />
    <DetalhesPaginados titulo="Datas com frequência" itens={lista(previa.datasComFrequenciasAfetadas)} total={previa.quantidadeDatasComFrequenciasAfetadas} truncada={previa.datasComFrequenciasTruncadas} renderizar={(data) => data} />
  </section>;
};

export const ModalAcaoContrato = ({ aberto, tipoAcao, contrato, processando, aoFechar, aoConfirmar, dadosIniciais }) => {
  const config = CONFIGURACOES[tipoAcao] || null;
  const [dados, setDados] = useState(() => dadosNovos(dadosIniciais));
  const [previa, setPrevia] = useState(null);
  const [confirmado, setConfirmado] = useState(false);
  const [consultando, setConsultando] = useState(false);
  const [submetendo, setSubmetendo] = useState(false);
  const [erro, setErro] = useState("");
  const [resultadoIncerto, setResultadoIncerto] = useState(false);
  const pedidoRef = useRef(0);
  const controleRef = useRef(null);
  const envioRef = useRef(false);
  const contextoRef = useRef("");
  const referenciaModal = useDialogoAcessivel({ aberto, aoFechar, bloquearFechamento: Boolean(processando || submetendo) });

  const invalidar = () => {
    pedidoRef.current += 1;
    controleRef.current?.abort();
    setPrevia(null);
    setConfirmado(false);
    setConsultando(false);
  };

  useEffect(() => {
    contextoRef.current = `${aberto}:${tipoAcao}:${contrato?.id}:${dadosIniciais?.eventoPrecedenteId || ""}`;
    invalidar();
    envioRef.current = false;
    setSubmetendo(false);
    setErro("");
    setResultadoIncerto(false);
    if (aberto) setDados(dadosNovos(dadosIniciais));
    return () => { pedidoRef.current += 1; controleRef.current?.abort(); };
  }, [aberto, tipoAcao, contrato?.id, dadosIniciais?.eventoPrecedenteId]);

  useEffect(() => { invalidar(); }, [contrato?.version]);

  const publicar = tipoAcao === "PUBLICAR";
  const payload = useMemo(() => publicar ? {
    dataPublicacao: dados.dataEfetiva,
    referenciaSei: dados.referenciaSei.trim(),
    justificativa: dados.motivo.trim() || "Publicação do contrato",
  } : {
    dataEfetiva: dados.dataEfetiva,
    dataAto: dados.dataAto || null,
    motivo: dados.motivo.trim(),
    referenciaSei: dados.referenciaSei.trim() || null,
    eventoPrecedenteId: numeroOuNulo(dados.eventoPrecedenteId),
    relacaoPrecedencia: dados.relacaoPrecedencia || null,
    ordemDocumental: numeroOuNulo(dados.ordemDocumental),
  }, [dados, publicar]);
  const assinatura = JSON.stringify(payload);
  const valido = useMemo(() => {
    if (!config) return false;
    if (!dados.dataEfetiva) return false;
    if (config.exigeDataAto && !dados.dataAto) return false;
    if (config.exigeReferencia && !dados.referenciaSei.trim()) return false;
    if (!publicar && !dados.motivo.trim()) return false;
    if (!publicar && Boolean(dados.eventoPrecedenteId) !== Boolean(dados.relacaoPrecedencia)) return false;
    if (dados.eventoPrecedenteId && (!Number.isSafeInteger(Number(dados.eventoPrecedenteId)) || Number(dados.eventoPrecedenteId) < 1)) return false;
    if (tipoAcao === "CORRIGIR_ANULACAO" && (!dados.eventoPrecedenteId || dados.relacaoPrecedencia !== "CANCELA")) return false;
    return true;
  }, [config, dados, publicar, tipoAcao]);

  const previaValida = Boolean(previa?.resposta?.tokenPrevia && previa.assinatura === assinatura);
  const ocupado = Boolean(processando || submetendo);

  if (!aberto || !config) return null;

  const alterar = (campo, valor) => {
    if (envioRef.current || processando) return;
    invalidar();
    setErro("");
    setDados((atual) => ({ ...atual, [campo]: valor }));
  };

  const gerarPrevia = async () => {
    if (!valido || ocupado || consultando || resultadoIncerto) return;
    invalidar();
    const numeroPedido = pedidoRef.current;
    const controlador = new AbortController();
    controleRef.current = controlador;
    setErro("");
    setConsultando(true);
    try {
      const resposta = publicar
        ? await ServicoContrato.previaPublicacao(contrato.id, payload, { signal: controlador.signal })
        : await ServicoContrato.previaCicloVida(contrato.id, OPERACOES[tipoAcao], payload, { signal: controlador.signal });
      if (numeroPedido !== pedidoRef.current || controlador.signal.aborted) return;
      if (!resposta?.tokenPrevia) throw new Error("O servidor não retornou uma prévia válida. Consulte novamente antes de confirmar.");
      setPrevia({ resposta, assinatura });
    } catch (falha) {
      if (numeroPedido === pedidoRef.current && !falha?.canceled && !controlador.signal.aborted) setErro(falha?.message || "Não foi possível obter a prévia.");
    } finally {
      if (numeroPedido === pedidoRef.current) setConsultando(false);
    }
  };

  const informarFalha = (falha) => {
    if (falha?.senhaConfirmacaoInvalida || falha?.code === "INVALID_CONFIRMATION_PASSWORD") return false;
    invalidar();
    setErro(falha?.message || "A operação não foi confirmada. Gere uma nova prévia antes de tentar novamente.");
    if (falha?.resultadoIncerto) setResultadoIncerto(true);
    return true;
  };

  const submeter = async (evento) => {
    evento.preventDefault();
    if (!valido || ocupado || envioRef.current || !previaValida || !confirmado || resultadoIncerto) return;
    envioRef.current = true;
    setSubmetendo(true);
    const contexto = contextoRef.current;
    try {
      const resultado = await aoConfirmar({ ...payload, tokenPrevia: previa.resposta.tokenPrevia, confirmarImpacto: true }, informarFalha);
      if (resultado === false && contexto === contextoRef.current) invalidar();
    } catch (falha) {
      if (contexto === contextoRef.current) informarFalha(falha);
    } finally {
      if (contexto === contextoRef.current) {
        envioRef.current = false;
        setSubmetendo(false);
      }
    }
  };

  return (
    <div className={estilos.sobreposicao} onMouseDown={(e) => e.target === e.currentTarget && !ocupado && aoFechar()} role="presentation">
      <section ref={referenciaModal} tabIndex={-1} role="dialog" aria-modal="true" aria-labelledby="titulo-acao-contrato" className={estilos.modal}>
        <header className={estilos.cabecalho}>
          <div><span>Ação de ciclo de vida</span><h2 id="titulo-acao-contrato">{config.titulo}</h2><small>{contrato?.numeroContrato || "Contrato"}{contrato?.anoContrato ? `/${contrato.anoContrato}` : ""}</small></div>
          <button type="button" onClick={aoFechar} disabled={ocupado} aria-label="Fechar"><X size={20} /></button>
        </header>
        <form onSubmit={submeter} className={estilos.formulario}>
          <div className={estilos.alerta}><AlertTriangle size={18} /><p>{config.descricao}</p></div>
          {publicar && <div className={estilos.observacao}><FileSignature size={17} /><span>O servidor validará vigência completa, empresa, Tipo de Contratação, fonte comprobatória e composição original consistente antes de publicar.</span></div>}
          <fieldset className={estilos.campos} disabled={ocupado}><div className={estilos.grade}>
            <label><span>{config.rotuloData} *</span><div className={estilos.comIcone}><CalendarClock size={16} /><input type="date" value={dados.dataEfetiva} onChange={(e) => alterar("dataEfetiva", e.target.value)} required /></div></label>
            {!publicar && <label><span>Data do ato {config.exigeDataAto ? "*" : "(opcional)"}</span><input type="date" value={dados.dataAto} onChange={(e) => alterar("dataAto", e.target.value)} required={config.exigeDataAto} /></label>}
            <label className={estilos.larguraTotal}><span>Referência SEI {config.exigeReferencia ? "*" : "(opcional)"}</span><input value={dados.referenciaSei} onChange={(e) => alterar("referenciaSei", e.target.value)} required={config.exigeReferencia} maxLength={120} placeholder="Documento/processo que comprova o ato" /></label>
            <label className={estilos.larguraTotal}><span>{publicar ? "Justificativa" : "Motivo/justificativa *"}</span><textarea rows={4} value={dados.motivo} onChange={(e) => alterar("motivo", e.target.value)} required={!publicar} maxLength={1000} /></label>
            {!publicar && <>
              <label><span>Evento precedente (ID)</span><input type="number" min="1" step="1" value={dados.eventoPrecedenteId} onChange={(e) => alterar("eventoPrecedenteId", e.target.value)} readOnly={tipoAcao === "CORRIGIR_ANULACAO"} /></label>
              <label><span>Relação de precedência</span><select value={dados.relacaoPrecedencia} disabled={tipoAcao === "CORRIGIR_ANULACAO"} onChange={(e) => alterar("relacaoPrecedencia", e.target.value)}><option value="">Sem precedência</option>{RELACOES.map((relacao) => <option key={relacao} value={relacao}>{relacao.replaceAll("_", " ")}</option>)}</select></label>
              <label><span>Ordem documental (opcional)</span><input type="number" min="0" step="1" value={dados.ordemDocumental} onChange={(e) => alterar("ordemDocumental", e.target.value)} /></label>
            </>}
          </div></fieldset>
          {erro && <p role="alert" className={estilos.erro}>{erro}</p>}
          {resultadoIncerto && <div className={estilos.alerta}><label><input type="checkbox" onChange={(e) => e.target.checked && setResultadoIncerto(false)} /> Consulte o histórico do contrato e marque após conferir o resultado antes de uma nova tentativa.</label></div>}
          {previaValida && <><ResumoPreviaContrato previa={previa.resposta} /><label className={estilos.confirmacaoImpacto}><input type="checkbox" checked={confirmado} disabled={ocupado} onChange={(e) => setConfirmado(e.target.checked)} /> Confirmo os impactos apresentados para esta intenção.</label></>}
          <footer className={estilos.rodape}>
            <button type="button" className={estilos.cancelar} onClick={aoFechar} disabled={ocupado}>Cancelar</button>
            <button type="button" className={estilos.cancelar} onClick={gerarPrevia} disabled={!valido || ocupado || consultando || resultadoIncerto}>{consultando ? "Consultando..." : "Gerar prévia"}</button>
            <button type="submit" className={estilos.confirmar} disabled={!valido || ocupado || !previaValida || !confirmado || resultadoIncerto}><Send size={17} />{ocupado ? "Aguardando confirmação..." : "Continuar e confirmar senha"}</button>
          </footer>
        </form>
      </section>
    </div>
  );
};
