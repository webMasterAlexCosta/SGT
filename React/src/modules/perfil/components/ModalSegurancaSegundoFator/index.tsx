import React from "react";
import { KeyRound, QrCode, ShieldCheck, Smartphone } from "lucide-react";

import { Botao, JanelaModal } from "../../../../core/ui/EnvolucroDica";
import { GerenciadorSegundoFator } from "../GerenciadorSegundoFator";
import estilos from "./index.module.css";

export const ModalSegurancaSegundoFator = ({ isOpen, onClose, onStatusChange }: { isOpen: (boolean); onClose: (() => void); onStatusChange: (ativo: boolean) => void }) => {
  if (!isOpen) return null;

  return (
    <JanelaModal
      isOpen={isOpen}
      onClose={onClose}
      title="Proteção Avançada (2FA)"
      maxWidth="max-w-2xl"
    >
      <div className={estilos.container}>
        <section className={estilos.apresentacao}>
          <div className={estilos.cabecalhoApresentacao}>
            <span className={estilos.iconeProtecao} aria-hidden="true">
              <ShieldCheck size={24} />
            </span>
            <div>
              <h3>Adicione uma segunda camada de proteção à sua conta</h3>
              <p>
                Depois de ativado, o acesso pode exigir um código temporário gerado
                no seu aplicativo autenticador, além da sua senha.
              </p>
            </div>
          </div>

          <div className={estilos.gradeBeneficios} aria-label="Benefícios do 2FA">
            <div className={estilos.beneficio}>
              <KeyRound size={18} />
              <span>Reduz o risco de acesso apenas com a senha.</span>
            </div>
            <div className={estilos.beneficio}>
              <Smartphone size={18} />
              <span>O código temporário é gerado no seu dispositivo.</span>
            </div>
            <div className={estilos.beneficio}>
              <ShieldCheck size={18} />
              <span>Reforça a validação de ações e sessões protegidas.</span>
            </div>
          </div>
        </section>

        <section className={estilos.configuracao}>
          <div className={estilos.cabecalhoConfiguracao}>
            <QrCode size={20} aria-hidden="true" />
            <div>
              <h3>Aplicativo autenticador</h3>
              <p>
                Configure, confirme ou desative o segundo fator sem misturar essa
                configuração com a alteração da sua senha.
              </p>
            </div>
          </div>

          <GerenciadorSegundoFator onStatusChange={onStatusChange} />
        </section>

        <div className={estilos.rodape}>
          <Botao variant="secondary" onClick={onClose} type="button">
            Fechar
          </Botao>
        </div>
      </div>
    </JanelaModal>
  );
};
