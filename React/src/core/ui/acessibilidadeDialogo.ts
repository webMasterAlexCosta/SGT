import { useEffect, useRef } from "react";

const SELETOR_FOCAVEL = [
  'a[href]',
  'button:not([disabled])',
  'input:not([disabled]):not([type="hidden"])',
  'select:not([disabled])',
  'textarea:not([disabled])',
  '[tabindex]:not([tabindex="-1"])',
].join(",");

const obterFocaveis = (conteiner: HTMLElement | null) =>
  Array.from(conteiner?.querySelectorAll?.<HTMLElement>(SELETOR_FOCAVEL) || []).filter(
    (elemento) => !elemento.hasAttribute("hidden") && elemento.getAttribute("aria-hidden") !== "true",
  );

const travasRolagem = new Set<React.RefObject<HTMLDivElement | null>>();
let overflowAntesDosDialogos = '';

const obterDialogoSuperior = () => {
  const candidatos = Array.from(document.querySelectorAll<HTMLElement>('[role="dialog"][aria-modal="true"]'));
  let superior: HTMLElement | null = null;
  let camadaSuperior = -Infinity;
  for (const candidato of candidatos) {
    let visivel = true;
    let camada = 0;
    for (let elemento: HTMLElement | null = candidato; elemento; elemento = elemento.parentElement) {
      const estilo = window.getComputedStyle(elemento);
      if (elemento.hidden || elemento.getAttribute('aria-hidden') === 'true' || estilo.display === 'none' || estilo.visibility === 'hidden') { visivel = false; break; }
      const z = Number.parseInt(estilo.zIndex, 10);
      if (Number.isFinite(z)) camada = Math.max(camada, z);
    }
    if (visivel && camada >= camadaSuperior) { superior = candidato; camadaSuperior = camada; }
  }
  return superior;
};

export const useDialogoAcessivel = ({
  aberto,
  aoFechar,
  bloquearFechamento = false,
  fecharComEsc = true,
}: { aberto: boolean; aoFechar?: () => void; bloquearFechamento?: boolean; fecharComEsc?: boolean }) => {
  const referenciaDialogo = useRef<HTMLDivElement>(null);
  const opcoesAtuais = useRef({ aoFechar, bloquearFechamento, fecharComEsc });
  opcoesAtuais.current = { aoFechar, bloquearFechamento, fecharComEsc };

  useEffect(() => {
    if (!aberto || typeof document === "undefined") return undefined;

    const focoAnterior = document.activeElement;
    if (travasRolagem.size === 0) overflowAntesDosDialogos = document.body.style.overflow;
    travasRolagem.add(referenciaDialogo);
    document.body.style.overflow = "hidden";

    const focarInicio = () => {
      const dialogo = referenciaDialogo.current;
      if (!dialogo || obterDialogoSuperior() !== dialogo) return;
      const focoPreferencial = dialogo.querySelector<HTMLElement>("[data-foco-inicial]");
      const [primeiro] = obterFocaveis(dialogo);
      (focoPreferencial || primeiro || dialogo).focus?.();
    };

    const idFoco = window.setTimeout(focarInicio, 0);

    const lidarComTecla = (evento: KeyboardEvent) => {
      const dialogo = referenciaDialogo.current;
      if (!dialogo || obterDialogoSuperior() !== dialogo) return;

      if (evento.key === "Escape") {
        if (opcoesAtuais.current.fecharComEsc) {
          evento.preventDefault();
          evento.stopImmediatePropagation();
          if (!opcoesAtuais.current.bloquearFechamento) opcoesAtuais.current.aoFechar?.();
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
      travasRolagem.delete(referenciaDialogo);
      document.body.style.overflow = travasRolagem.size ? 'hidden' : overflowAntesDosDialogos;
      const superior = obterDialogoSuperior();
      if (focoAnterior instanceof HTMLElement && focoAnterior.isConnected && (!superior || superior.contains(focoAnterior))) {
        focoAnterior.focus?.();
      }
    };
  }, [aberto]);

  return referenciaDialogo;
};
