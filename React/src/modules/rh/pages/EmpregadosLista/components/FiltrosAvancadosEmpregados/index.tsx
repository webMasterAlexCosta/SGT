import React, { memo, useEffect, useMemo, useState } from "react";
import { createPortal } from "react-dom";
import { Clock3, Filter, List, MapPin, RotateCcw, X } from "lucide-react";
import { CONFIG } from "../../../../../../core/config.ts";
import estilos from "./index.module.css";

export const FiltrosAvancadosEmpregados = memo(
  ({
    aberto,
    lotacaoFiltro,
    turnoFiltro,
    tamanhoPagina,
    lotacoes,
    turnos,
    aoAplicar,
    aoLimpar,
    aoFechar,
  }: { aberto: boolean; lotacaoFiltro?: string; turnoFiltro?: string; tamanhoPagina: number; lotacoes: import("../../../../../../contracts/opcoes-apresentacao").LocationOptionView[]; turnos: import("../../../../../../contracts/opcoes-apresentacao").ShiftSummaryView[]; aoAplicar: (filtros: { lotacaoId: string; turnoId: string; tamanho: number }) => void; aoLimpar: () => void; aoFechar: () => void }) => {
    const [rascunhoLotacao, definirRascunhoLotacao] = useState(lotacaoFiltro || "");
    const [rascunhoTurno, definirRascunhoTurno] = useState(turnoFiltro || "");
    const [rascunhoTamanho, definirRascunhoTamanho] = useState(tamanhoPagina);

    useEffect(() => {
      definirRascunhoLotacao(lotacaoFiltro || "");
      definirRascunhoTurno(turnoFiltro || "");
      definirRascunhoTamanho(tamanhoPagina);
    }, [lotacaoFiltro, turnoFiltro, tamanhoPagina, aberto]);

    useEffect(() => {
      if (!aberto || typeof document === "undefined") return undefined;

      const overflowAnterior = document.body.style.overflow;
      document.body.style.overflow = "hidden";

      const aoPressionarTecla = (evento: KeyboardEvent) => {
        if (evento.key === "Escape") aoFechar();
      };
      document.addEventListener("keydown", aoPressionarTecla);

      return () => {
        document.body.style.overflow = overflowAnterior;
        document.removeEventListener("keydown", aoPressionarTecla);
      };
    }, [aberto, aoFechar]);

    const turnosDoRascunho = useMemo(() => {
      if (!rascunhoLotacao) return [];
      const lotacao = lotacoes.find(
        (item) => String(item.id) === String(rascunhoLotacao),
      );
      return lotacao?.turnos || turnos || [];
    }, [lotacoes, rascunhoLotacao, turnos]);

    if (!aberto || typeof document === "undefined") return null;

    const aplicar = () => {
      aoAplicar({
        lotacaoId: rascunhoLotacao,
        turnoId: rascunhoTurno,
        tamanho: rascunhoTamanho,
      });
      aoFechar();
    };

    const limpar = () => {
      definirRascunhoLotacao("");
      definirRascunhoTurno("");
      definirRascunhoTamanho(CONFIG.PAGINATION_DEFAULT_SIZE);
      aoLimpar();
    };

    const modal = (
      <div
        className={estilos.sobreposicaoModal}
        role="presentation"
        onMouseDown={(evento) => {
          if (evento.target === evento.currentTarget) aoFechar();
        }}
      >
        <section
          className={estilos.modalFiltros}
          role="dialog"
          aria-modal="true"
          aria-labelledby="titulo-filtros-empregados"
          aria-describedby="descricao-filtros-empregados"
        >
          <header className={estilos.cabecalhoModal}>
            <div className={estilos.identidadeModal}>
              <span className={estilos.iconeTitulo} aria-hidden="true">
                <Filter size={19} />
              </span>
              <div className={estilos.textosTitulo}>
                <span className={estilos.labelModal}>FILTROS DA LISTAGEM</span>
                <h3 id="titulo-filtros-empregados">Filtros avançados</h3>
                <p id="descricao-filtros-empregados">
                  Refine os colaboradores exibidos sem reduzir a área da tabela.
                </p>
              </div>
            </div>
            <button
              type="button"
              className={estilos.botaoFechar}
              onClick={aoFechar}
              aria-label="Fechar filtros avançados"
            >
              <X size={19} />
            </button>
          </header>

          <div className={estilos.conteudoModal}>
            <div className={estilos.listaCampos}>
              <label className={estilos.campoFiltro}>
                <span className={estilos.rotuloCampo}><MapPin size={14} /> Lotação</span>
                <select
                  value={rascunhoLotacao}
                  onChange={(evento) => {
                    definirRascunhoLotacao(evento.target.value);
                    definirRascunhoTurno("");
                  }}
                  className={estilos.entradaCampo}
                >
                  <option value="">Todas as lotações</option>
                  {lotacoes.map((lotacao) => (
                    <option key={lotacao.id} value={lotacao.id ?? undefined}>
                      {lotacao.nome || lotacao.descricao}
                    </option>
                  ))}
                </select>
              </label>

              <label className={estilos.campoFiltro}>
                <span className={estilos.rotuloCampo}><Clock3 size={14} /> Turno</span>
                <select
                  value={rascunhoTurno}
                  onChange={(evento) => definirRascunhoTurno(evento.target.value)}
                  className={estilos.entradaCampo}
                  disabled={!rascunhoLotacao}
                >
                  <option value="">{rascunhoLotacao ? "Todos os turnos" : "Selecione a lotação"}</option>
                  {turnosDoRascunho.map((turno) => (
                    <option key={turno.id} value={turno.id ?? undefined}>
                      {turno.descricao || turno.nome}
                    </option>
                  ))}
                </select>
              </label>

              <label className={`${estilos.campoFiltro} ${estilos.campoFiltroLargo}`}>
                <span className={estilos.rotuloCampo}><List size={14} /> Quantidade por página</span>
                <select
                  value={rascunhoTamanho}
                  onChange={(evento) => definirRascunhoTamanho(Number(evento.target.value))}
                  className={estilos.entradaCampo}
                >
                  <option value={0}>Exibir nenhum</option>
                  {CONFIG.PAGINATION_OPTIONS.map((opcao) => (
                    <option key={opcao} value={opcao}>
                      Exibir {opcao}
                    </option>
                  ))}
                  <option value={CONFIG.PAGINATION_MAX_SIZE}>
                    Exibir até {CONFIG.PAGINATION_MAX_SIZE}
                  </option>
                </select>
              </label>
            </div>
          </div>

          <footer className={estilos.rodapeModal}>
            <button type="button" className={estilos.botaoCancelar} onClick={aoFechar}>
              Cancelar
            </button>
            <button type="button" className={estilos.botaoSecundario} onClick={limpar}>
              <RotateCcw size={15} /> Limpar filtros
            </button>
            <button type="button" className={estilos.botaoPrimario} onClick={aplicar}>
              <Filter size={15} /> Aplicar filtros
            </button>
          </footer>
        </section>
      </div>
    );

    return createPortal(modal, document.body);
  },
);

FiltrosAvancadosEmpregados.displayName = "FiltrosAvancadosEmpregados";
