import React, { useEffect, useId, useRef, useState } from "react";
import { createPortal } from "react-dom";
import { CircleHelp, Maximize2, Minimize2, X } from "lucide-react";
import estilos from "./index.module.css";

export const BotaoAjudaPainel = ({ titulo, aoAbrir }: { titulo: string; aoAbrir: () => void }) => (
  <button
    type="button"
    className={estilos.botaoAjuda}
    onClick={aoAbrir}
    title={`Entender indicador: ${titulo}`}
    aria-label={`Entender indicador: ${titulo}`}
  >
    <CircleHelp size={17} aria-hidden="true" />
  </button>
);

export const BotaoExpandirPainel = ({ titulo, aoAbrir }: { titulo: string; aoAbrir: () => void }) => (
  <button
    type="button"
    className={estilos.botaoExpandir}
    onClick={aoAbrir}
    title={`Abrir análise completa: ${titulo}`}
    aria-label={`Abrir análise completa: ${titulo}`}
  >
    <Maximize2 size={16} aria-hidden="true" />
  </button>
);

export const ModalPainel = ({ aberto, titulo, detalhe, aoFechar, children, selo = "Análise detalhada", permitirTelaCheia = true, lateral = false }: { aberto: (boolean); titulo: (string); detalhe: (string) | (React.ReactNode); aoFechar: (() => void); children: React.ReactNode; selo?: (string); permitirTelaCheia?: boolean; lateral?: boolean }) => {
  const tituloId = useId();
  const modalRef = useRef<HTMLDivElement | null>(null);
  const fecharRef = useRef<HTMLElementTagNameMap['button'] | null>(null);
  const focoAnteriorRef = useRef<(Element | null) | null>(null);
  const [telaCheia, setTelaCheia] = useState(false);

  useEffect(() => {
    if (!aberto) {
      setTelaCheia(false);
      return undefined;
    }

    focoAnteriorRef.current = document.activeElement;
    const estiloAnterior = document.body.style.overflow;
    document.body.style.overflow = "hidden";

    const aoTeclar = (evento: KeyboardEvent) => {
      if (evento.key === "Escape") {
        evento.preventDefault();
        aoFechar?.();
        return;
      }

      if (evento.key !== "Tab" || !modalRef.current) return;
      const focaveis = Array.from(
        modalRef.current.querySelectorAll<HTMLElement>(
          'button:not([disabled]), a[href], input:not([disabled]), select:not([disabled]), textarea:not([disabled]), [tabindex]:not([tabindex="-1"])',
        ),
      );
      if (!focaveis.length) return;
      const primeiro = focaveis[0];
      const ultimo = focaveis[focaveis.length - 1];
      if (evento.shiftKey && document.activeElement === primeiro) {
        evento.preventDefault();
        ultimo.focus();
      } else if (!evento.shiftKey && document.activeElement === ultimo) {
        evento.preventDefault();
        primeiro.focus();
      }
    };

    window.addEventListener("keydown", aoTeclar);
    const quadroFoco = requestAnimationFrame(() => fecharRef.current?.focus());

    return () => {
      cancelAnimationFrame(quadroFoco);
      document.body.style.overflow = estiloAnterior;
      window.removeEventListener("keydown", aoTeclar);
      if (focoAnteriorRef.current && "focus" in focoAnteriorRef.current && typeof focoAnteriorRef.current.focus === "function") focoAnteriorRef.current.focus();
    };
  }, [aberto, aoFechar]);

  if (!aberto || typeof document === "undefined") return null;

  return createPortal(
    <div
      className={`${estilos.sobreposicao} ${lateral ? estilos.sobreposicaoLateral : ''}`}
      role="presentation"
      onMouseDown={(evento) => {
        if (evento.target === evento.currentTarget) aoFechar?.();
      }}
    >
      <section
        ref={modalRef}
        className={`${estilos.modal} ${telaCheia ? estilos.modalTelaCheia : ""} ${lateral ? estilos.modalLateral : ''}`}
        role="dialog"
        aria-modal="true"
        aria-labelledby={tituloId}
      >
        <header className={estilos.cabecalho}>
          <div>
            <span className={estilos.selo}>{selo}</span>
            <h2 id={tituloId}>{titulo}</h2>
            {detalhe ? <p>{detalhe}</p> : null}
          </div>
          <div className={estilos.acoesModal}>
            {permitirTelaCheia && (
              <button
                type="button"
                className={estilos.botaoJanela}
                onClick={() => setTelaCheia((atual) => !atual)}
                aria-label={telaCheia ? "Sair da tela cheia" : "Expandir modal para tela cheia"}
                title={telaCheia ? "Sair da tela cheia" : "Tela cheia"}
              >
                {telaCheia ? <Minimize2 size={19} aria-hidden="true" /> : <Maximize2 size={19} aria-hidden="true" />}
              </button>
            )}
            <button
              ref={fecharRef}
              type="button"
              className={estilos.botaoFechar}
              onClick={aoFechar}
              aria-label="Fechar análise detalhada"
            >
              <X size={20} aria-hidden="true" />
            </button>
          </div>
        </header>
        <div className={estilos.conteudo}>{children}</div>
      </section>
    </div>,
    document.body,
  );
};


export const ModalAjudaIndicador = ({ aberto, titulo, ajuda, aoFechar }: { aberto: boolean; titulo: string; ajuda?: import("../../../../contracts/painel").IndicatorHelp; aoFechar: () => void }) => {
  if (!ajuda) return null;
  const blocos = [
    ["Para que serve", ajuda.finalidade],
    ["Como é calculado", ajuda.calculo],
    ["Fonte dos dados", ajuda.fonte],
    ["Como interpretar", ajuda.interpretacao],
  ].filter(([, texto]) => texto);
  return (
    <ModalPainel
      aberto={aberto}
      titulo={titulo}
      detalhe="Definição, fonte e regra de cálculo do indicador"
      selo="Como interpretar"
      permitirTelaCheia={false}
      aoFechar={aoFechar}
    >
      <div className={estilos.ajudaIndicador}>
        {blocos.map(([rotulo, texto]) => (
          <section key={rotulo}>
            <h3>{rotulo}</h3>
            <p>{texto}</p>
          </section>
        ))}
      </div>
    </ModalPainel>
  );
};
