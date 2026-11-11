import React, { useId, useState } from "react";
import { createPortal } from "react-dom";
import { AlertTriangle } from "lucide-react";
import { Botao } from "./EnvolucroDica";
import { useDialogoAcessivel } from "./acessibilidadeDialogo.ts";
import estilos from "./ModalConfirmacao.module.css";

export const ModalConfirmacao = ({
  isOpen,
  onClose,
  onConfirm,
  title,
  message,
  confirmText = "Confirmar",
  cancelText = "Cancelar",
  variant = "danger",
  critica = true,
}: { isOpen: boolean; onClose: () => void; onConfirm: () => void | Promise<void>; title?: React.ReactNode; message?: React.ReactNode; confirmText?: string; cancelText?: string; variant?: string; critica?: boolean }) => {
  const [confirmando, definirConfirmando] = useState(false);
  const idBase = useId().replace(/:/g, "");
  const idTitulo = `confirmacao-${idBase}-titulo`;
  const idMensagem = `confirmacao-${idBase}-mensagem`;
  const referenciaDialogo = useDialogoAcessivel({
    aberto: isOpen,
    aoFechar: onClose,
    bloquearFechamento: confirmando,
    fecharComEsc: true,
  });

  if (!isOpen) return null;

  const confirmar = async () => {
    if (confirmando) return;
    definirConfirmando(true);
    try {
      await onConfirm?.();
      onClose?.();
    } catch (erro) {
      if (import.meta?.env?.DEV) console.error("Falha na confirmação crítica:", erro);
    } finally {
      definirConfirmando(false);
    }
  };

  const fecharPeloFundo = (evento: React.MouseEvent<HTMLElement>) => {
    if (critica || confirmando) return;
    if (evento.target === evento.currentTarget) onClose?.();
  };

  return createPortal(
    <div className={estilos.sobreposicaoModal} onMouseDown={fecharPeloFundo}>
      <div
        ref={referenciaDialogo}
        className={estilos.cartaoModal}
        role="alertdialog"
        aria-modal="true"
        aria-labelledby={idTitulo}
        aria-describedby={idMensagem}
        aria-busy={confirmando || undefined}
        tabIndex={-1}
      >
        <div className={estilos.corpoModal}>
          <div className={`${estilos.iconeCircular} ${variant === "danger" ? estilos.fundoPerigo : estilos.fundoAviso}`} aria-hidden="true">
            <AlertTriangle size={32} />
          </div>

          <h3 id={idTitulo} className={estilos.tituloAlerta}>{title || "Confirmar ação"}</h3>
          <p id={idMensagem} className={estilos.textoSecundario}>
            {message || "Revise os dados antes de continuar."}
          </p>

          <div className={estilos.grupoBotoes}>
            <Botao
              variant="secondary"
              onClick={onClose}
              className={estilos.botaoLarguraTotal}
              disabled={confirmando}
            >
              {cancelText}
            </Botao>
            <Botao
              variant={variant}
              onClick={confirmar}
              className={estilos.botaoLarguraTotal}
              carregando={confirmando}
              data-foco-inicial
            >
              {confirmText}
            </Botao>
          </div>
        </div>
      </div>
    </div>,
    document.body,
  );
};
