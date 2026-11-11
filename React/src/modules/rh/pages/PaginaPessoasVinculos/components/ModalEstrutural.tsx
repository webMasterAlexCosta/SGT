import type * as DTO from '../../../../../contracts/java';
import React, { useId } from "react";
import { X } from "lucide-react";
import { useDialogoAcessivel } from "../../../../../core/ui/acessibilidadeDialogo.ts";
import estilos from "../index.module.css";

export const ModalEstrutural = ({ aberto, titulo, subtitulo, aoFechar, children, largura = "amplo" }: React.PropsWithChildren<{ aberto: boolean; titulo: string; subtitulo?: string; aoFechar: () => void; largura?: string }>) => {
  const referencia = useDialogoAcessivel({ aberto, aoFechar });
  const tituloId = useId();

  if (!aberto) return null;

  return (
    <div className={estilos.sobreposicaoModal} role="presentation" onMouseDown={(evento) => {
      if (evento.target === evento.currentTarget) aoFechar();
    }}>
      <section
        ref={referencia}
        className={`${estilos.modal} ${estilos[`modal${largura[0].toUpperCase()}${largura.slice(1)}`] || ""}`}
        role="dialog"
        aria-modal="true"
        aria-labelledby={tituloId}
        tabIndex={-1}
      >
        <header className={estilos.cabecalhoModal}>
          <div>
            <h2 id={tituloId}>{titulo}</h2>
            {subtitulo && <p>{subtitulo}</p>}
          </div>
          <button type="button" className={estilos.botaoIcone} onClick={aoFechar} aria-label="Fechar janela">
            <X size={20} aria-hidden="true" />
          </button>
        </header>
        <div className={estilos.corpoModal}>{children}</div>
      </section>
    </div>
  );
};
