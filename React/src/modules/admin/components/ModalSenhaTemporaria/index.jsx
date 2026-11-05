import React, { useState } from "react";
import { Check, Copy, KeyRound, ShieldAlert } from "lucide-react";
import { notificar } from "../../../../core/events";
import { Botao, JanelaModal } from "../../../../core/ui/EnvolucroDica";
import estilos from "./index.module.css";

export const ModalSenhaTemporaria = ({ credencial, aoFechar }) => {
  const [copiada, setCopiada] = useState(false);
  const senha = credencial?.temporaryPassword || "";

  const copiar = async () => {
    try {
      await navigator.clipboard.writeText(senha);
      setCopiada(true);
      window.setTimeout(() => setCopiada(false), 1800);
    } catch {
      notificar("warning", "Não foi possível copiar automaticamente. Selecione a senha e copie manualmente.");
    }
  };

  return (
    <JanelaModal isOpen={Boolean(credencial)} onClose={aoFechar} title="Usuário ativado">
      <div className={estilos.conteudo}>
        <span className={estilos.icone}><KeyRound size={26} /></span>
        <h3>Senha temporária gerada</h3>
        <p>Envie esta credencial ao usuário <strong>{credencial?.username || "—"}</strong> por um canal seguro.</p>
        <div className={estilos.credencial}>
          <code>{senha}</code>
          <button type="button" onClick={copiar} aria-label="Copiar senha temporária">
            {copiada ? <Check size={18} /> : <Copy size={18} />}
          </button>
        </div>
        <div className={estilos.alerta}>
          <ShieldAlert size={19} />
          <span>A senha aparece apenas agora. No primeiro login, o usuário será obrigado a criar uma nova senha.</span>
        </div>
        <Botao onClick={aoFechar} className={estilos.aviso}>Entendi, já guardei a senha</Botao>
      </div>
    </JanelaModal>
  );
};
