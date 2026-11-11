import React from "react";
import { KeyRound, ShieldCheck } from "lucide-react";
import { Botao, CampoEntrada, JanelaModal } from "../../../../../../core/ui/EnvolucroDica";
import estilos from "./index.module.css";

export const ModalRedefinirSenha = ({ aberto, dados, setDados, aoFechar, aoConfirmar, senhaMinima = 8 }: { aberto: (boolean); dados: import("../../../../../../contracts/usuarios").PasswordResetForm; setDados: React.Dispatch<React.SetStateAction<import("../../../../../../contracts/usuarios").PasswordResetForm>>; aoFechar: (() => void); aoConfirmar: (() => Promise<void>); senhaMinima?: number }) => (
  <JanelaModal isOpen={aberto} onClose={aoFechar} title="Redefinir senha do usuário">
    <div className={estilos.conteudo}>
      <p>Usuário: <strong>{dados.usuario?.username || "—"}</strong>. A redefinição desbloqueia a conta e encerra as sessões anteriores.</p>
      <CampoEntrada label="Nova senha temporária" type="password" value={dados.novaSenha} onChange={(e) => setDados((atual) => ({ ...atual, novaSenha: e.target.value }))} autoComplete="new-password" minLength={senhaMinima} maxLength={72} />
      <CampoEntrada label="Confirmar nova senha" type="password" value={dados.confirmarSenha} onChange={(e) => setDados((atual) => ({ ...atual, confirmarSenha: e.target.value }))} autoComplete="new-password" minLength={senhaMinima} maxLength={72} />
      <div className={estilos.aviso}><ShieldCheck size={18} /><span>A troca desta senha será obrigatória no próximo login.</span></div>
      <div className={estilos.rodape}><Botao variant="secondary" onClick={aoFechar}>Cancelar</Botao><Botao onClick={aoConfirmar}><KeyRound size={16} /> Redefinir senha</Botao></div>
    </div>
  </JanelaModal>
);
