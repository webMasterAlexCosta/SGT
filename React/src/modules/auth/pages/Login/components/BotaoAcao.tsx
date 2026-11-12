import React from "react";
import { Loader2 } from "lucide-react";
import { EnvolucroDica } from "../../../../../core/ui/EnvolucroDica";
import estilos from "../index.module.css";

export const BotaoAcao = React.memo(
  ({
    children,
    carregando,
    dica,
    desabilitado,
    disabled,
    className,
    ...propriedadesBotao
  }: React.ButtonHTMLAttributes<HTMLButtonElement> & { carregando?: boolean; dica?: React.ReactNode; desabilitado?: boolean }) => {
    const estaBloqueado = disabled || desabilitado || carregando;
    const classeVariante = estaBloqueado
      ? estilos.botaoAcaoDesabilitado
      : estilos.botaoAcaoPrimario;

    return (
      <EnvolucroDica tooltip={dica} position="top" disabled={estaBloqueado}>
        <button
          {...propriedadesBotao}
          className={`${estilos.botaoAcaoGeral} ${classeVariante} ${className || ""}`}
          disabled={estaBloqueado}
          aria-busy={carregando || undefined}
        >
          {carregando ? (
            <Loader2 size={18} className={estilos.iconeCarregandoBotao} />
          ) : (
            <span className={estilos.conteudoBotao}>{children}</span>
          )}
        </button>
      </EnvolucroDica>
    );
  },
);
BotaoAcao.displayName = "BotaoAcao";
