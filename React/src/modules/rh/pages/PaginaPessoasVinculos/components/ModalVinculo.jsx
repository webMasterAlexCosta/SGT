import React, { useEffect, useMemo, useRef, useState } from "react";
import { CalendarClock, GitCompareArrows, History, IdCard, Link2Off, MapPin, Search, ShieldCheck } from "lucide-react";
import {
  formatarCpfProtegido,
  formatarDataPessoaVinculo,
  formatarInstantePessoaVinculo,
  prepararAcaoAuditavel,
  prepararAlteracaoMatricula,
  prepararDesligamento,
  prepararRetificacaoInicio,
  rotularEstadoVinculo,
  rotularSituacaoVinculo,
  rotularTipoEventoVinculo,
} from "../../../../../services/rh/ServicoDominioPessoaVinculo.js";
import { notificar } from "../../../../../core/events.js";
import { ModalEstrutural } from "./ModalEstrutural.jsx";
import estilos from "../index.module.css";

const CONFIG_ACOES = Object.freeze({
  RETIFICAR_INICIO: { titulo: "Retificar início", previa: true, campoData: "novoInicio", rotuloData: "Novo início efetivo" },
  ALTERAR_MATRICULA: { titulo: "Alterar matrícula", campoData: "dataEfetiva", rotuloData: "Data efetiva", matricula: true },
  CANCELAR_PROGRAMACAO: { titulo: "Cancelar programação futura" },
  DESLIGAR: { titulo: "Registrar desligamento", campoData: "dataFim", rotuloData: "Último dia do vínculo", motivo: true },
  RETIFICAR_DESLIGAMENTO: { titulo: "Retificar desligamento", previa: true, campoData: "dataFim", rotuloData: "Novo último dia", motivo: true },
  CANCELAR_DESLIGAMENTO: { titulo: "Cancelar desligamento programado" },
});

const DADOS_ACAO_INICIAIS = Object.freeze({
  novoInicio: "", dataEfetiva: "", dataFim: "", novaMatricula: "", motivoDesligamentoId: "",
  justificativa: "", evidencia: "", autorizacao: "", confirmarImpacto: false, tokenPrevia: "",
  confirmarInclusaoExcedente: false, confirmarPrecedenciaUltimoDia: false,
});

const validarAuditaveis = (dados) => [dados.justificativa, dados.evidencia, dados.autorizacao].every((valor) => String(valor || "").trim());

export const ModalAcaoVinculo = ({ aberto, tipo, vinculo, motivos = [], processando, aoFechar, aoPrevia, aoExecutar }) => {
  const configuracao = CONFIG_ACOES[tipo] || CONFIG_ACOES.CANCELAR_PROGRAMACAO;
  const [dados, setDados] = useState({ ...DADOS_ACAO_INICIAIS });
  const [previa, setPrevia] = useState(null);
  const [carregandoPrevia, setCarregandoPrevia] = useState(false);
  const [submetendo, setSubmetendo] = useState(false);
  const [erro, setErro] = useState("");
  const pedidoPrevia = useRef(0);
  const envioPendente = useRef(null);
  const ocupado = Boolean(processando || submetendo);

  const invalidarPrevia = () => {
    pedidoPrevia.current += 1;
    setPrevia(null);
    setCarregandoPrevia(false);
    setDados((atual) => ({ ...atual, tokenPrevia: "", confirmarImpacto: false }));
  };

  useEffect(() => {
    invalidarPrevia();
    envioPendente.current = null;
    setSubmetendo(false);
    setErro("");
    if (aberto) setDados({ ...DADOS_ACAO_INICIAIS });
    return () => { pedidoPrevia.current += 1; envioPendente.current = null; };
  }, [aberto, tipo, vinculo?.id]);

  useEffect(() => { invalidarPrevia(); }, [vinculo?.version]);

  const alterar = (campo, valor) => {
    if (envioPendente.current || processando) return;
    invalidarPrevia();
    setErro("");
    setDados((atual) => ({ ...atual, [campo]: valor, tokenPrevia: "", confirmarImpacto: false }));
  };

  const fechar = () => {
    if (envioPendente.current || processando) return;
    invalidarPrevia();
    aoFechar();
  };

  const montarPayload = (incluiToken = true) => {
    const base = configuracao.matricula
      ? prepararAlteracaoMatricula(dados)
      : configuracao.motivo
        ? prepararDesligamento(dados)
        : configuracao.campoData === "novoInicio"
          ? prepararRetificacaoInicio(dados)
          : prepararAcaoAuditavel(dados);
    return incluiToken ? base : { ...base, confirmarImpacto: false, tokenPrevia: null };
  };

  const gerarPrevia = async () => {
    if (!aberto || envioPendente.current || processando || carregandoPrevia) return;
    if (!dados[configuracao.campoData] || !validarAuditaveis(dados) || (configuracao.motivo && !dados.motivoDesligamentoId)) {
      notificar("warning", "Preencha a data e todos os campos auditáveis antes de gerar a prévia.");
      return;
    }
    invalidarPrevia();
    const pedido = pedidoPrevia.current;
    setErro("");
    setCarregandoPrevia(true);
    try {
      const resposta = await aoPrevia(tipo, montarPayload(false));
      if (pedido !== pedidoPrevia.current) return;
      if (!resposta?.token) throw new Error("O servidor não retornou um token de prévia válido. Gere uma nova prévia.");
      setPrevia(resposta);
      setDados((atual) => ({ ...atual, tokenPrevia: resposta.token, confirmarImpacto: false }));
    } catch (falha) {
      if (pedido === pedidoPrevia.current && !falha?.canceled) setErro(falha?.message || "Não foi possível gerar a prévia do vínculo.");
    } finally {
      if (pedido === pedidoPrevia.current) setCarregandoPrevia(false);
    }
  };

  const enviar = async (evento) => {
    evento.preventDefault();
    if (!aberto || envioPendente.current || processando || carregandoPrevia) return;
    if (!validarAuditaveis(dados)) { notificar("warning", "Justificativa, evidência e autorização são obrigatórias."); return; }
    if (configuracao.campoData && !dados[configuracao.campoData]) { notificar("warning", "Informe a data efetiva da ação."); return; }
    if (configuracao.matricula && !dados.novaMatricula.trim()) { notificar("warning", "Informe a nova matrícula."); return; }
    if (configuracao.motivo && !dados.motivoDesligamentoId) { notificar("warning", "Selecione o motivo do desligamento."); return; }
    if (configuracao.previa && (!dados.tokenPrevia || !dados.confirmarImpacto)) { notificar("warning", "Gere a prévia e confirme os impactos atuais antes de concluir."); return; }
    const envio = {};
    envioPendente.current = envio;
    setSubmetendo(true);
    setErro("");
    try {
      await aoExecutar(tipo, montarPayload());
    } catch (falha) {
      if (envioPendente.current === envio && !falha?.canceled) setErro(falha?.message || "Não foi possível concluir a ação. Confira os dados antes de tentar novamente.");
    } finally {
      if (envioPendente.current === envio) {
        envioPendente.current = null;
        setSubmetendo(false);
        invalidarPrevia();
      }
    }
  };

  return (
    <ModalEstrutural aberto={aberto} titulo={configuracao.titulo} subtitulo={`Vínculo ${vinculo?.id || ""} · comando auditável`} aoFechar={fechar}>
      <form onSubmit={enviar} className={estilos.formularioAcao}>
        {configuracao.campoData && <label><span>{configuracao.rotuloData} *</span><input data-foco-inicial type="date" disabled={ocupado} value={dados[configuracao.campoData]} onChange={(e) => alterar(configuracao.campoData, e.target.value)} /></label>}
        {configuracao.matricula && <label><span>Nova matrícula *</span><input disabled={ocupado} value={dados.novaMatricula} onChange={(e) => alterar("novaMatricula", e.target.value)} maxLength={80} /></label>}
        {configuracao.motivo && <label><span>Motivo de desligamento *</span><select disabled={ocupado} value={dados.motivoDesligamentoId} onChange={(e) => alterar("motivoDesligamentoId", e.target.value)}><option value="">Selecione</option>{motivos.filter((motivo) => motivo.ativo !== false).map((motivo) => <option key={motivo.id} value={motivo.id}>{motivo.descricao}</option>)}</select></label>}
        <label><span>Justificativa *</span><textarea disabled={ocupado} value={dados.justificativa} onChange={(e) => alterar("justificativa", e.target.value)} /></label>
        <label><span>Evidência *</span><textarea disabled={ocupado} value={dados.evidencia} onChange={(e) => alterar("evidencia", e.target.value)} /></label>
        <label><span>Autorização *</span><textarea disabled={ocupado} value={dados.autorizacao} onChange={(e) => alterar("autorizacao", e.target.value)} /></label>
        {configuracao.previa && <label className={estilos.confirmacao}><input type="checkbox" disabled={ocupado} checked={dados.confirmarInclusaoExcedente} onChange={(e) => alterar("confirmarInclusaoExcedente", e.target.checked)} /> Confirmo a inclusão excedente indicada pelo servidor. Alterar este aceite exige nova prévia.</label>}
        {configuracao.motivo && <label className={estilos.confirmacao}><input type="checkbox" disabled={ocupado} checked={dados.confirmarPrecedenciaUltimoDia} onChange={(e) => alterar("confirmarPrecedenciaUltimoDia", e.target.checked)} /> Confirmo a precedência: a alocação vale no último dia e o desligamento após esse dia.</label>}
        {erro && <p role="alert">{erro}</p>}

        {configuracao.previa && (
          <div className={estilos.areaPrevia}>
            <button type="button" className={estilos.botaoSecundario} onClick={gerarPrevia} disabled={carregandoPrevia || ocupado}><GitCompareArrows size={16} /> {carregandoPrevia ? "Gerando…" : "Gerar prévia de impacto"}</button>
            {previa && <div className={estilos.resultadoPrevia}><strong>Prévia vinculada ao formulário atual</strong><p>{formatarDataPessoaVinculo(previa.valorAtual)} → {formatarDataPessoaVinculo(previa.valorProposto)}</p>{(previa.impactos || []).map((impacto) => <span key={impacto}>{impacto}</span>)}<label className={estilos.confirmacao}><input type="checkbox" disabled={ocupado} checked={dados.confirmarImpacto} onChange={(e) => setDados((atual) => ({ ...atual, confirmarImpacto: e.target.checked }))} /> Confirmo os impactos apresentados e o uso deste token de prévia.</label></div>}
          </div>
        )}

        <footer className={estilos.rodapeModal}>
          <p><ShieldCheck size={16} /> O Java revalidará estado, concorrência, datas e token no momento do comando.</p>
          <div><button type="button" className={estilos.botaoSecundario} onClick={fechar} disabled={ocupado}>Voltar</button><button type="submit" className={estilos.botaoPerigo} disabled={ocupado || carregandoPrevia}>{ocupado ? "Processando…" : "Confirmar ação"}</button></div>
        </footer>
      </form>
    </ModalEstrutural>
  );
};

const LinhaDado = ({ rotulo, valor }) => <div className={estilos.linhaDado}><dt>{rotulo}</dt><dd>{valor || "—"}</dd></div>;

export const ModalVinculo = ({
  aberto, vinculo, eventos = [], posicao, carregando, consultandoTemporal, podeGerenciar,
  aoFechar, aoConsultarTemporal, aoAbrirAcao, aoAbrirAlocacoes,
}) => {
  const hoje = useMemo(() => new Date().toISOString().slice(0, 10), []);
  const [dataTemporal, setDataTemporal] = useState(hoje);
  const [conhecidoAte, setConhecidoAte] = useState("");
  const identidadeResolvida = posicao?.identidadeHistorica;
  const nomeHistorico = identidadeResolvida?.comprovado === true && identidadeResolvida.nome
    ? identidadeResolvida.nome
    : posicao?.fotografiaIdentidadeComprovada === true && posicao.nomeFotografia
      ? posicao.nomeFotografia
      : null;

  useEffect(() => { if (aberto) { setDataTemporal(hoje); setConhecidoAte(""); } }, [aberto, hoje, vinculo?.id]);

  const acoes = useMemo(() => {
    if (!vinculo || !podeGerenciar || vinculo.estadoNegocial !== "REGISTRADO") return [];
    const lista = [
      ["RETIFICAR_INICIO", "Retificar início", CalendarClock],
      ["ALTERAR_MATRICULA", "Alterar matrícula", IdCard],
    ];
    if (vinculo.situacaoTemporal === "PROGRAMADO") lista.push(["CANCELAR_PROGRAMACAO", "Cancelar programação", Link2Off]);
    if (vinculo.dataFim) {
      lista.push(["RETIFICAR_DESLIGAMENTO", "Retificar desligamento", GitCompareArrows]);
      if (vinculo.dataFim >= hoje) lista.push(["CANCELAR_DESLIGAMENTO", "Cancelar desligamento programado", Link2Off]);
    } else lista.push(["DESLIGAR", "Registrar desligamento", Link2Off]);
    return lista;
  }, [hoje, podeGerenciar, vinculo]);

  return (
    <ModalEstrutural aberto={aberto} titulo={`Vínculo ${vinculo?.id || ""}`} subtitulo="Vigência efetiva, posição conhecida e trilha imutável de eventos" aoFechar={aoFechar} largura="extra">
      {carregando || !vinculo ? <p className={estilos.estadoVazio}>Carregando o vínculo e seus eventos…</p> : (
        <div className={estilos.colunasDetalhe}>
          <div className={estilos.colunaPrincipal}>
            <section className={estilos.secaoDetalhe}>
              <div className={estilos.tituloSecao}><IdCard size={18} /><h3>Dados contratuais</h3><em className={estilos.estadoVinculo}>{rotularSituacaoVinculo(vinculo.situacaoTemporal)}</em></div>
              <dl className={estilos.gradeDados}>
                <LinhaDado rotulo="Pessoa" valor={vinculo.pessoaNome} />
                <LinhaDado rotulo="CPF" valor={formatarCpfProtegido(vinculo.pessoaCpf)} />
                <LinhaDado rotulo="Contrato" valor={`${vinculo.contratoNumero || vinculo.contratoId}${vinculo.contratoAno ? `/${vinculo.contratoAno}` : ""}`} />
                <LinhaDado rotulo="Gestão Local" valor={vinculo.gestaoLocalNome} />
                <LinhaDado rotulo="Início efetivo" valor={formatarDataPessoaVinculo(vinculo.dataInicio)} />
                <LinhaDado rotulo="Fim efetivo" valor={formatarDataPessoaVinculo(vinculo.dataFim, "Em aberto")} />
                <LinhaDado rotulo="Matrícula" valor={vinculo.matricula} />
                <LinhaDado rotulo="Estado negocial" valor={rotularEstadoVinculo(vinculo.estadoNegocial)} />
                <LinhaDado rotulo="Motivo do término" valor={vinculo.motivoDesligamentoDescricao} />
              </dl>
            </section>

            <section className={estilos.secaoDetalhe}>
              <div className={estilos.tituloSecao}><MapPin size={18} /><h3>Alocações temporais</h3></div>
              <div className={estilos.chamadaAlocacoes}><div><strong>Lotação + Turno + Cargo são faixas do vínculo</strong><span>Consulte períodos, eventos, posição bitemporal e, quando autorizado, programe ou retifique mudanças sem criar novo vínculo.</span></div><button type="button" className={estilos.botaoSecundario} onClick={aoAbrirAlocacoes}><CalendarClock size={16} /> Abrir linha de alocações</button></div>
            </section>

            <section className={estilos.secaoDetalhe}>
              <div className={estilos.tituloSecao}><Search size={18} /><h3>Posição temporal da pessoa</h3></div>
              <div className={estilos.consultaTemporal}>
                <label><span>Data de referência</span><input type="date" value={dataTemporal} onChange={(e) => setDataTemporal(e.target.value)} /></label>
                <label><span>Conhecido até (opcional)</span><input type="datetime-local" value={conhecidoAte} onChange={(e) => setConhecidoAte(e.target.value)} /></label>
                <button type="button" className={estilos.botaoSecundario} onClick={() => aoConsultarTemporal({ data: dataTemporal, conhecidoAte: conhecidoAte ? new Date(conhecidoAte).toISOString() : undefined })} disabled={!dataTemporal || consultandoTemporal}>{consultandoTemporal ? "Consultando…" : "Consultar servidor"}</button>
              </div>
              {posicao && <div className={estilos.resultadoTemporal}>
                <span>Nome histórico no período consultado</span>
                <strong>{nomeHistorico || "Nome histórico não comprovado"}</strong>
                <span>Nome atual: {posicao.nomeAtual || "Não informado"}</span>
                <span>Referência: {formatarDataPessoaVinculo(posicao.dataReferencia)} · conhecimento: {formatarInstantePessoaVinculo(posicao.conhecidoAte)}</span>
                <p>{posicao.vinculos?.length || 0} vínculo(s) compõem esta fotografia bitemporal.</p>
              </div>}
            </section>

            <section className={estilos.secaoDetalhe}>
              <div className={estilos.tituloSecao}><History size={18} /><h3>Linha do tempo</h3><span>{eventos.length}</span></div>
              {eventos.length === 0 ? <p className={estilos.estadoVazio}>Nenhum evento visível.</p> : <ol className={estilos.linhaTempo}>{eventos.map((evento) => (
                <li key={evento.id}><span className={estilos.marcadorEvento} /><article><header><strong>{rotularTipoEventoVinculo(evento.tipo)}</strong><em>efetivo em {formatarDataPessoaVinculo(evento.dataEfetiva)}</em></header><small>Registrado em {formatarInstantePessoaVinculo(evento.registradoEm)} por {evento.responsavelUsername || "sistema"}</small>{evento.justificativa && <p>{evento.justificativa}</p>}<div className={estilos.fotografiaEvento}><span>{evento.pessoaNomeFotografia}</span><span>{formatarCpfProtegido(evento.pessoaCpfFotografia)}</span><span>{evento.contratoNumeroFotografia}{evento.contratoAnoFotografia ? `/${evento.contratoAnoFotografia}` : ""}</span></div></article></li>
              ))}</ol>}
            </section>
          </div>
          {acoes.length > 0 && <aside className={estilos.colunaAcoes}><h3>Ações autorizadas</h3><p>Os botões emitem comandos de domínio; nenhum campo do vínculo é editado diretamente.</p>{acoes.map(([tipo, rotulo, Icone]) => <button type="button" key={tipo} className={tipo.includes("CANCELAR") || tipo === "DESLIGAR" ? estilos.botaoPerigoContorno : estilos.botaoSecundario} onClick={() => aoAbrirAcao(tipo)}><Icone size={16} /> {rotulo}</button>)}</aside>}
        </div>
      )}
    </ModalEstrutural>
  );
};
