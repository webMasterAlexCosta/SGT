import React, { memo, useMemo, useState, useEffect, useRef } from "react";
import {
  AlertCircle,
  Ban,
  Info,
  MapPin,
  Clock3,
  CheckCircle,
  Lock,
  Timer,
  Coffee,
  ThumbsDown,
  CalendarHeart,
  BriefcaseMedical,
  Clock,
  Check,
  X,
  FileSignature,
  Loader2,
  Zap,
  ZapOff,
} from "lucide-react";
import { agruparFaltasNoTopo } from "../../../../../services/rh/ServicoDominioFrequencia.js";

import estilos from "../index.module.css";

const ListaVirtualNativa = ({
  itemCount,
  itemSize,
  height,
  width,
  itemData,
  children: Row,
}) => {
  const [scrollTop, setScrollTop] = useState(0);
  if (!height || !width) return null;

  const totalHeight = itemCount * itemSize;
  const overscan = 5;

  const startIndex = Math.max(0, Math.floor(scrollTop / itemSize) - overscan);
  const endIndex = Math.min(
    itemCount - 1,
    Math.ceil((scrollTop + height) / itemSize) + overscan,
  );

  const items = [];
  for (let i = startIndex; i <= endIndex; i++) {
    items.push(
      <Row
        key={itemData.listaOrdenada[i]?.empregadoId ?? i}
        index={i}
        data={itemData}
        style={{
          position: "absolute",
          top: i * itemSize,
          left: 0,
          width: "100%",
          height: itemSize,
        }}
      />,
    );
  }

  return (
    <div
      className={estilos.areaRolagem}
      style={{
        height,
        width,
        overflowY: "auto",
        overflowX: "hidden",
        position: "relative",
      }}
      onScroll={(e) => setScrollTop(e.currentTarget.scrollTop)}
    >
      <div style={{ height: totalHeight, width: "100%", position: "relative" }}>
        {items}
      </div>
    </div>
  );
};

const ListaTradicional = ({ itemCount, itemData, itemSize, children: Row }) => {
  const items = [];
  for (let i = 0; i < itemCount; i++) {
    items.push(
      <Row
        key={itemData.listaOrdenada[i]?.empregadoId ?? i}
        index={i}
        data={itemData}
        style={{
          position: "relative",
          width: "100%",
          ...(itemSize ? { height: itemSize } : { minHeight: 0 }),
        }}
      />,
    );
  }

  return (
    <div
      className={estilos.areaRolagem}
      style={{
        height: "100%",
        width: "100%",
        overflowY: "auto",
        overflowX: "hidden",
      }}
    >
      {items}
    </div>
  );
};

const DimensionadorAutomatico = ({ children }) => {
  const [tamanho, setTamanho] = useState({ width: 0, height: 0 });
  const containerRef = useRef(null);

  useEffect(() => {
    const elemento = containerRef.current;
    if (!elemento) return;

    const observer = new ResizeObserver((entries) => {
      if (!entries.length) return;
      const { width, height } = entries[0].contentRect;
      if (width > 0 && height > 0) {
        window.requestAnimationFrame(() => setTamanho({ width, height }));
      }
    });

    observer.observe(elemento);
    return () => observer.disconnect();
  }, []);

  return (
    <div
      ref={containerRef}
      style={{
        width: "100%",
        height: "100%",
        flex: 1,
        minHeight: 0,
        overflow: "hidden",
      }}
    >
      {tamanho.width > 0 && tamanho.height > 0
        ? children({ width: tamanho.width, height: tamanho.height })
        : null}
    </div>
  );
};

const ComponenteLinha = memo(({ index, style, data }) => {
  const {
    listaOrdenada,
    statusDaData,
    eGestorOperacional,
    edicaoBloqueada,
    funcaoObterJanelaDeEdicao,
    aoSelecionarStatusDaTabela,
    aoAtualizarObservacaoDaTabela,
    aoAbrirModalParcial,
  } = data;

  const linha = listaOrdenada[index];
  if (!linha) return null;

  const possuiSuspensao = linha.ocorrencia?.motivo === "SUSPENSAO";
  const detalhesJanela =
    linha.jaSalvo && linha.prazoEdicao
      ? funcaoObterJanelaDeEdicao(linha.prazoEdicao)
      : { isValid: false };
  const bloqueado =
    edicaoBloqueada ||
    !statusDaData.valid ||
    (!linha.identidadeCanonicaDisponivel && !linha.resolucaoCanonicaPermitida) ||
    Boolean(linha.edicaoBloqueadaPeloBackend);

  const statusLimpo = String(linha.status || "").toUpperCase();
  
  const exigeObservacao = [ "ATESTADO"].includes(
    statusLimpo,
  );
  const observacaoVazia = !linha.obs || linha.obs.trim() === "";
  const campoInvalidoVisualmente =
    exigeObservacao && observacaoVazia && !bloqueado;

  const ehImpar = index % 2 !== 0;
  const cssZebra = ehImpar ? estilos.linhaImpar : estilos.linhaPar;
  let cssEstado = "";

  if (linha.jaSalvo) {
    if (bloqueado) cssEstado = estilos.bordaBloqueada;
    else cssEstado = estilos.bordaSucesso;
  } else if (possuiSuspensao) {
    cssEstado = estilos.bordaSuspensao;
  }

  const cssFalta = statusLimpo === "FALTA" ? estilos.linhaFalta : "";
  const cssAtestado = statusLimpo === "ATESTADO" ? estilos.linhaAtestado : "";
  const classesDaLinha = `${estilos.linhaVirtualBase} ${cssZebra} ${cssEstado} ${cssFalta} ${cssAtestado}`;

  const obterClasseDoBotaoDeStatus = (statusAtual, statusAlvo, tema) => {
    const ativo = statusAtual === statusAlvo;
    const temaFmt = tema.charAt(0).toUpperCase() + tema.slice(1);
    return `${estilos.botaoStatus} ${estilos[`tema${temaFmt}`]} ${ativo ? estilos.botaoAtivo : estilos.botaoInativo}`;
  };

  return (
    <div style={style} className={classesDaLinha}>
      <div className={estilos.colunaInfoLargura}>
        <div
          className={estilos.nomeEmpregado}
          title={`Nome Completo: ${linha.nome}`}
        >
          {linha.nome}
          {linha.ocorrencia && (
            <span
              title={`Ocorrência registada no sistema: ${linha.ocorrencia.motivo}`}
              className={`${estilos.emblemaOcorrencia} ${possuiSuspensao ? estilos.emblemaSuspensao : estilos.emblemaNormal}`}
            >
              {possuiSuspensao ? <Ban size={10} /> : <Info size={10} />}{" "}
              {linha.ocorrencia.motivo}
            </span>
          )}
        </div>
        <div className={estilos.infoEmpregado}>
          <span title={`Cargo do colaborador: ${linha.cargo}`}>
            {linha.cargo}
          </span>{" "}
          <span className={estilos.pontoInfo}>•</span>
          <span
            className={estilos.infoComIcone}
            title={`Lotação atual: ${linha.lotacao}`}
          >
            <MapPin size={10} className={estilos.pontoInfo} /> {linha.lotacao}
          </span>{" "}
          <span className={estilos.pontoInfo}>•</span>
          <span
            className={`${estilos.infoComIcone} ${estilos.destaqueInfo}`}
            title={`Jornada de trabalho: ${linha.turno} (${linha.horarioTrabalhoDoDia || 'Horário não definido'})`}
          >
            <Clock3 size={10} /> {linha.turno} {linha.horarioTrabalhoDoDia ? `(${linha.horarioTrabalhoDoDia})` : ""}
          </span>
        </div>

        {/* Div Unificada para Tags e Status lado a lado */}
        <div className={estilos.tagsEStatus}>
          {linha.diaDescansoCalendario && (
            <span
              title={`Este dia está configurado no calendário corporativo como: ${linha.calendarioTipo}`}
              className={`${estilos.tagBasica} ${linha.calendarioTipo === "FERIADO" ? estilos.tagFeriado : estilos.tagDescanso}`}
            >
              {linha.calendarioTipo === "FERIADO" ? "FERIADO" : "DESCANSO"}
            </span>
          )}
          {linha.folgaRemunerada && (
            <span
              title="O colaborador possui uma Folga Remunerada aprovada para este dia"
              className={`${estilos.tagBasica} ${estilos.tagFolga}`}
            >
              FOLGA REMUNERADA
            </span>
          )}
          {linha.showRtBadge && (
            <span
              title={`Responsabilidade Técnica (RT) ${linha.rtQuantidade ? `Quantidade: ${linha.rtQuantidade}` : ""}`}
              className={`${estilos.tagBasica} ${estilos.tagRt}`}
            >
              RT{linha.rtQuantidade ? ` ${linha.rtQuantidade}` : ""}
            </span>
          )}

          {!linha.jaSalvo && (
            <span className={`${estilos.itemStatus} ${estilos.statusPendente}`} title="Nenhum lançamento oficial retornado pelo backend">
              Pendente
            </span>
          )}
          {!linha.identidadeCanonicaDisponivel && (
            <span className={`${estilos.itemStatus} ${estilos.statusCanonicoIndisponivel}`} title={linha.resolucaoCanonicaPermitida
              ? "Ao salvar, a associação exata desta linha, data, lotação e turno será consultada no histórico autorizado"
              : "O servidor não forneceu a associação desta linha ao vínculo; a edição permanece bloqueada"}>
              {linha.resolucaoCanonicaPermitida ? "Associação será conferida ao salvar" : "Associação ao vínculo indisponível"}
            </span>
          )}
          {linha.jaSalvo && (
            <>
              <span
                title="Este registro já foi salvo e sincronizado na base de dados"
                className={`${estilos.itemStatus} ${estilos.statusSalvo}`}
              >
                <CheckCircle size={10} /> Salvo
              </span>
              {bloqueado ? (
                <span
                  title="O backend informou estado oficial que bloqueia a edição operacional"
                  className={`${estilos.itemStatus} ${estilos.statusBloqueado}`}
                >
                  <Lock size={8} /> Bloqueado
                </span>
              ) : null}
              {detalhesJanela.isValid && (
                <span
                  title="Prazo informado pelo backend; este contador é apenas informativo"
                  className={`${estilos.itemStatus} ${detalhesJanela.isEndingSoon ? estilos.cronometroPerigo : estilos.cronometroAviso}`}
                >
                  <Timer size={10} /> {detalhesJanela.remaining}
                </span>
              )}
              {linha.estadoNegocial && <span className={estilos.itemStatus}>{linha.estadoNegocial}</span>}
              {linha.validadeOperacional && <span className={estilos.itemStatus}>{linha.validadeOperacional}</span>}
              {linha.estadoAuditoria && <span className={estilos.itemStatus}>{linha.estadoAuditoria}</span>}
              {linha.versaoNegocial != null && <span className={estilos.itemStatus}>v{linha.versaoNegocial}</span>}
            </>
          )}
        </div>
      </div>

      <div className={estilos.colunaAcoesLargura}>
        <div
          className={`${estilos.gradeBotoes} ${bloqueado ? estilos.gradeBotoesBloqueada : ""}`}
        >
          {linha.isDiaDescanso ? (
            <>
              {!linha.diaFeriado && !linha.folgaRemunerada ? (
                <>
                  <button
                    title={`Marcar Folga/Descanso para ${linha.nome}`}
                    type="button"
                    onClick={() =>
                      aoSelecionarStatusDaTabela(
                        linha.empregadoId,
                        linha.calendarioTipo === "DIA_DESCANSO" ? "DESCANSO" : "FOLGA"
                      )
                    }
                    className={obterClasseDoBotaoDeStatus(
                      linha.status,
                      linha.calendarioTipo === "DIA_DESCANSO" ? "DESCANSO" : "FOLGA",
                      "violeta",
                    )}
                  >
                    <Coffee size={14} /> {linha.calendarioTipo === "DIA_DESCANSO" ? "DESCANSO" : "FOLGA"}
                  </button>
                  <button
                    title={`Registrar Perda de DSR por faltas para ${linha.nome}`}
                    type="button"
                    onClick={() =>
                      aoSelecionarStatusDaTabela(
                        linha.empregadoId,
                        "DSR_PERDIDO",
                      )
                    }
                    className={obterClasseDoBotaoDeStatus(
                      linha.status,
                      "DSR_PERDIDO",
                      "vermelho",
                    )}
                  >
                    <ThumbsDown size={14} /> PERDEU DSR
                  </button>
                </>
              ) : (
                <button
                  title={`Marcar Folga Remunerada/Feriado para ${linha.nome}`}
                  type="button"
                  style={{ gridColumn: "span 2" }}
                  onClick={() =>
                    aoSelecionarStatusDaTabela(linha.empregadoId, "FOLGA")
                  }
                  className={obterClasseDoBotaoDeStatus(
                    linha.status,
                    "FOLGA",
                    "ardosia",
                  )}
                >
                  <CalendarHeart size={14} /> {linha.diaFeriado ? "FERIADO" : "FOLGA REM."}
                </button>
              )}
              <button
                title={`Marcar Responsabilidade Técnica (RT) para ${linha.nome}`}
                type="button"
                onClick={() =>
                  aoSelecionarStatusDaTabela(linha.empregadoId, "RT")
                }
                className={obterClasseDoBotaoDeStatus(
                  linha.status,
                  "RT",
                  "verdeAgua",
                )}
              >
                <BriefcaseMedical size={14} /> RT
              </button>
              <button
                title={`Lançar Presença Parcial/Extra para ${linha.nome}`}
                type="button"
                onClick={() =>
                  aoSelecionarStatusDaTabela(
                    linha.empregadoId,
                    "PRESENCA_PARCIAL",
                  )
                }
                className={obterClasseDoBotaoDeStatus(
                  linha.status,
                  "PRESENCA_PARCIAL",
                  "ambar",
                )}
              >
                <Clock size={14} /> PARCIAL
              </button>
            </>
          ) : (
            <>
              <button
                title={`Confirmar Presença Integral para ${linha.nome}`}
                type="button"
                onClick={() =>
                  aoSelecionarStatusDaTabela(linha.empregadoId, "PRESENCA")
                }
                className={obterClasseDoBotaoDeStatus(
                  linha.status,
                  "PRESENCA",
                  "esmeralda",
                )}
              >
                <Check size={14} /> PRESENÇA
              </button>
              <button
                title={`Registrar Falta Integral para ${linha.nome}`}
                type="button"
                onClick={() =>
                  aoSelecionarStatusDaTabela(linha.empregadoId, "FALTA")
                }
                className={obterClasseDoBotaoDeStatus(
                  linha.status,
                  "FALTA",
                  "rosa",
                )}
              >
                <X size={14} /> FALTA
              </button>
              <button
                title={`Registrar Presença Parcial (Atraso/Saída Antecipada) para ${linha.nome}`}
                type="button"
                onClick={() =>
                  aoSelecionarStatusDaTabela(
                    linha.empregadoId,
                    "PRESENCA_PARCIAL",
                  )
                }
                className={obterClasseDoBotaoDeStatus(
                  linha.status,
                  "PRESENCA_PARCIAL",
                  "ambar",
                )}
              >
                <Clock size={14} /> PARCIAL
              </button>
              <button
                title={`Registrar Atestado Médico para ${linha.nome}`}
                type="button"
                onClick={() =>
                  aoSelecionarStatusDaTabela(linha.empregadoId, "ATESTADO")
                }
                className={obterClasseDoBotaoDeStatus(
                  linha.status,
                  "ATESTADO",
                  "ceu",
                )}
              >
                <FileSignature size={14} /> ATESTADO
              </button>
            </>
          )}
        </div>
      </div>

      <div className={estilos.colunaObsLargura}>
        {linha.status === "PRESENCA_PARCIAL" ? (
          <div
            className={estilos.caixaParcial}
            title="Resumo da Presença Parcial"
          >
            <div className={estilos.cabecalhoParcial}>
              <span
                className={estilos.textoHorasParcial}
                title="Total de horas efetivamente trabalhadas"
              >
                {linha.horasTrabalhadas}h Trab.
              </span>
              <span
                className={estilos.textoSaidaParcial}
                title="Horário exato da saída"
              >
                Saída: <b>{linha.horarioSaida || "?"}</b>
              </span>
            </div>
            {!bloqueado && (
              <button
                type="button"
                title="Clique para reabrir a janela de ajustes de horas e observação"
                onClick={() =>
                  aoAbrirModalParcial({
                    open: true,
                    empregadoId: linha.empregadoId,
                    horas: linha.horasTrabalhadas,
                    saida: linha.horarioSaida,
                    obs: linha.obs,
                    noturnas: linha.horasNoturnas,
                    entradaOriginal: linha.entradaTurno
                  })
                }
                className={estilos.botaoEditarParcial}
              >
                Editar Parcial
              </button>
            )}
          </div>
        ) : (
          <div className={estilos.envoltorioObsTabela}>
            <textarea
              title={
                bloqueado
                  ? "Anotações bloqueadas para edição"
                  : "Insira uma justificativa, observação ou motivo"
              }
              value={linha.obs || ""}
              onChange={(e) =>
                aoAtualizarObservacaoDaTabela(linha.empregadoId, e.target.value)
              }
              className={`${estilos.entradaObs} ${bloqueado ? estilos.entradaObsBloqueada : ""} ${campoInvalidoVisualmente ? estilos.entradaObsInvalida : ""}`}
              placeholder={
                exigeObservacao
                  ? "Justificativa obrigatória..."
                  : "Observação (Opcional)..."
              }
              disabled={bloqueado}
            />
            {campoInvalidoVisualmente && (
              <div className={estilos.alertaObsTabela}>
                <AlertCircle size={10} /> Requerido
              </div>
            )}
          </div>
        )}
      </div>
    </div>
  );
});
ComponenteLinha.displayName = "ComponenteLinha";

export const TabelaFrequencia = memo(
  ({
    estaCarregando,
    erroCarregamento,
    listaDeFrequencia,
    statusDaData,
    eGestorOperacional,
    edicaoBloqueada,
    aoSelecionarStatusDaTabela,
    aoAtualizarObservacaoDaTabela,
    aoAbrirModalParcial,
    funcaoObterJanelaDeEdicao,
    paginacaoGrade,
    aoMudarPagina,
    pendenciasSemIdentidadeCanonica = 0,
  }) => {
    const [preferenciaVirtualizacao, setPreferenciaVirtualizacao] = useState(() => {
      try {
        const valorSalvo = localStorage.getItem("sgt_frequencia_virtualizacao");
        return valorSalvo !== null ? JSON.parse(valorSalvo) : true;
      } catch {
        return true;
      }
    });

    const [larguraViewport, setLarguraViewport] = useState(() =>
      typeof window === "undefined" ? 1280 : window.innerWidth,
    );

    useEffect(() => {
      if (typeof window === "undefined") return undefined;
      let quadro = 0;
      const atualizarLargura = () => {
        window.cancelAnimationFrame(quadro);
        quadro = window.requestAnimationFrame(() =>
          setLarguraViewport(window.innerWidth),
        );
      };
      window.addEventListener("resize", atualizarLargura, { passive: true });
      return () => {
        window.cancelAnimationFrame(quadro);
        window.removeEventListener("resize", atualizarLargura);
      };
    }, []);

    const modoMobile = larguraViewport <= 820;
    const modoMobileCompacto = larguraViewport <= 520;
    const modoTablet = larguraViewport > 820 && larguraViewport < 1024;
    const virtualizacaoObrigatoria = listaDeFrequencia.length >= 150;
    const virtualizacaoPreferida = virtualizacaoObrigatoria || preferenciaVirtualizacao;
    // Em listas pequenas, o mobile usa altura natural para impedir cortes quando
    // nome, ocorrência, tags ou observações ocuparem mais de uma linha.
    const usarVirtualizacao =
      virtualizacaoObrigatoria || (!modoMobile && virtualizacaoPreferida);

    const alternarVirtualizacao = () => {
      if (virtualizacaoObrigatoria || modoMobile) return;
      const novoValor = !preferenciaVirtualizacao;
      setPreferenciaVirtualizacao(novoValor);
      localStorage.setItem(
        "sgt_frequencia_virtualizacao",
        JSON.stringify(novoValor),
      );
    };

    const alturaLinhaCompacta = modoMobileCompacto
      ? 390
      : modoMobile
        ? 350
        : modoTablet
          ? 104
          : 90;

    const { listaOrdenada, totalFaltas, totalAtestados } = useMemo(
      () => agruparFaltasNoTopo(listaDeFrequencia),
      [listaDeFrequencia],
    );

    const itemData = useMemo(
      () => ({
        listaOrdenada,
        statusDaData,
        eGestorOperacional,
        edicaoBloqueada,
        funcaoObterJanelaDeEdicao,
        aoSelecionarStatusDaTabela,
        aoAtualizarObservacaoDaTabela,
        aoAbrirModalParcial,
      }),
      [
        listaOrdenada,
        statusDaData,
        eGestorOperacional,
        edicaoBloqueada,
        funcaoObterJanelaDeEdicao,
        aoSelecionarStatusDaTabela,
        aoAtualizarObservacaoDaTabela,
        aoAbrirModalParcial,
      ],
    );

    return (
      <div className={estilos.cartaoTabela} data-tour="freq-table">
        <div className={estilos.cabecalhoGradePersonalizado}>
          <div className={estilos.cabecalhoGradeFlex}>
            <div className={estilos.colunaColaborador}>
              Colaborador ({listaOrdenada.length})
              {totalFaltas > 0 && (
                <span className={estilos.contadorFaltas} title="Faltas movidas para o topo da lista">
                  {totalFaltas} falta{totalFaltas === 1 ? "" : "s"}
                </span>
              )}
              {totalAtestados > 0 && (
                <span className={estilos.contadorAtestados} title="Atestados movidos logo após as faltas no topo da lista">
                  {totalAtestados} atestado{totalAtestados === 1 ? "" : "s"}
                </span>
              )}
            </div>
            <div className={estilos.colunaStatus}>Status Diário</div>
            <div className={estilos.colunaDetalhes}>
              <span>Detalhes / Obs</span>

              <button
                title={
                  virtualizacaoObrigatoria
                    ? "Alta performance obrigatória para listas com 150 ou mais colaboradores"
                    : modoMobile
                      ? "No celular, listas menores usam altura adaptativa para evitar cortes"
                      : usarVirtualizacao
                        ? "Desativar alta performance"
                        : "Ativar alta performance"
                }
                onClick={alternarVirtualizacao}
                disabled={virtualizacaoObrigatoria || modoMobile}
                className={`${estilos.botaoTurbo} ${usarVirtualizacao ? estilos.turboAtivo : estilos.turboInativo}`}
              >
                {usarVirtualizacao ? (
                  <>
                    <Zap size={12} /> Turbo Ativo
                  </>
                ) : (
                  <>
                    <ZapOff size={12} /> Normal
                  </>
                )}
              </button>
            </div>
          </div>
        </div>

        {pendenciasSemIdentidadeCanonica > 0 && (
          <div className={estilos.avisoContratoCanonico} role="status">
            <AlertCircle size={16} />
            <span>
              {pendenciasSemIdentidadeCanonica.toLocaleString("pt-BR")} linha(s) desta página estão sem associação ao vínculo contratual na resposta da grade. {eGestorOperacional
                ? "Ao salvar, o histórico autorizado será consultado para comprovar a associação do colaborador na data, lotação e turno. Ausência ou ambiguidade bloqueia a gravação."
                : "A edição dessas linhas está indisponível para o perfil Operacional porque o contrato de consulta não fornece essa associação. Solicite a conferência ao Gestor Local."}
            </span>
          </div>
        )}

        <div className={estilos.tabelaContainerFlex}>
          {estaCarregando ? (
            <div className={estilos.sobreposicaoCarregamento}>
              <div className={estilos.conteinerCarregamento}>
                <Loader2
                  size={36}
                  color="#2563eb"
                  className={estilos.iconeCarregamentoGirandoEspacado}
                />
                <span className={estilos.textoCarregamentoSync}>
                  Sincronizando a escala...
                </span>
              </div>
            </div>
          ) : erroCarregamento ? (
            <div className={estilos.estadoVazio} role="alert">
              <AlertCircle size={32} />
              <span>{erroCarregamento}</span>
            </div>
          ) : listaOrdenada.length === 0 ? (
            <div className={estilos.estadoVazio}>
              <AlertCircle size={32} />{" "}
              <span>Nenhum funcionário encontrado nos filtros atuais.</span>
            </div>
          ) : (
            <div className={estilos.containerListaRolagem}>
              {usarVirtualizacao ? (
                <DimensionadorAutomatico>
                  {({ height, width }) => (
                    <ListaVirtualNativa
                      height={height}
                      width={width}
                      itemCount={listaOrdenada.length}
                      itemSize={alturaLinhaCompacta}
                      itemData={itemData}
                    >
                      {ComponenteLinha}
                    </ListaVirtualNativa>
                  )}
                </DimensionadorAutomatico>
              ) : (
                <ListaTradicional
                  itemCount={listaOrdenada.length}
                  itemSize={modoMobile ? null : alturaLinhaCompacta}
                  itemData={itemData}
                >
                  {ComponenteLinha}
                </ListaTradicional>
              )}
            </div>
          )}
        </div>
        {paginacaoGrade?.totalPaginas > 1 && (
          <div className={estilos.paginacaoFrequencia} aria-label="Paginação da grade de frequência">
            <span>
              Página {Number(paginacaoGrade.pagina || 0) + 1} de {paginacaoGrade.totalPaginas} · {Number(paginacaoGrade.totalElementos || 0).toLocaleString("pt-BR")} colaborador(es)
            </span>
            <div>
              <button type="button" disabled={paginacaoGrade.pagina <= 0 || estaCarregando} onClick={() => aoMudarPagina?.(paginacaoGrade.pagina - 1)}>Anterior</button>
              <button type="button" disabled={paginacaoGrade.pagina >= paginacaoGrade.totalPaginas - 1 || estaCarregando} onClick={() => aoMudarPagina?.(paginacaoGrade.pagina + 1)}>Próxima</button>
            </div>
          </div>
        )}
      </div>
    );
  },
);

TabelaFrequencia.displayName = "TabelaFrequencia";
