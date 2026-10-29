import { useEffect, useRef } from "react";

const SELETOR_FOCAVEL = [
  'a[href]',
  'button:not([disabled])',
  'input:not([disabled]):not([type="hidden"])',
  'select:not([disabled])',
  'textarea:not([disabled])',
  '[tabindex]:not([tabindex="-1"])',
].join(",");

const obterFocaveis = (conteiner) =>
  Array.from(conteiner?.querySelectorAll?.(SELETOR_FOCAVEL) || []).filter(
    (elemento) => !elemento.hasAttribute("hidden") && elemento.getAttribute("aria-hidden") !== "true",
  );

export const useDialogoAcessivel = ({
  aberto,
  aoFechar,
  bloquearFechamento = false,
  fecharComEsc = true,
}) => {
  const referenciaDialogo = useRef(null);

  useEffect(() => {
    if (!aberto || typeof document === "undefined") return undefined;

    const focoAnterior = document.activeElement;
    const overflowAnterior = document.body.style.overflow;
    document.body.style.overflow = "hidden";

    const focarInicio = () => {
      const dialogo = referenciaDialogo.current;
      if (!dialogo) return;
      const focoPreferencial = dialogo.querySelector("[data-foco-inicial]");
      const [primeiro] = obterFocaveis(dialogo);
      (focoPreferencial || primeiro || dialogo).focus?.();
    };

    const idFoco = window.setTimeout(focarInicio, 0);

    const lidarComTecla = (evento) => {
      const dialogo = referenciaDialogo.current;
      if (!dialogo) return;

      if (evento.key === "Escape") {
        if (fecharComEsc && !bloquearFechamento) {
          evento.preventDefault();
          aoFechar?.();
        }
        return;
      }

      if (evento.key !== "Tab") return;
      const focaveis = obterFocaveis(dialogo);
      if (focaveis.length === 0) {
        evento.preventDefault();
        dialogo.focus?.();
        return;
      }

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

    document.addEventListener("keydown", lidarComTecla);
    return () => {
      window.clearTimeout(idFoco);
      document.removeEventListener("keydown", lidarComTecla);
      document.body.style.overflow = overflowAnterior;
      if (focoAnterior instanceof HTMLElement && focoAnterior.isConnected) {
        focoAnterior.focus?.();
      }
    };
  }, [aberto, aoFechar, bloquearFechamento, fecharComEsc]);

  return referenciaDialogo;
};
