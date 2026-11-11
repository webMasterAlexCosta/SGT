import React from "react";
import { Loader2 } from "lucide-react";
import { EnvolucroDica } from "../../../../../core/ui/EnvolucroDica";
import estilos from "../index.module.css";

export const BotaoAcao = React.memo(
  ({
    children,
    carregando,
    dica,
    modoDesempenho,
    desabilitado,
    ...propriedadesBotao
  }: React.ButtonHTMLAttributes<HTMLButtonElement> & { carregando?: boolean; dica?: React.ReactNode; modoDesempenho?: boolean; desabilitado?: boolean }) => {
    const estaBloqueado = desabilitado || carregando;
    const classeVariante = estaBloqueado
      ? estilos.botaoAcaoDesabilitado
      : estilos.botaoAcaoPrimario;

    return (
      <EnvolucroDica tooltip={dica} position="top" disabled={estaBloqueado}>
        <button
          className={`${estilos.botaoAcaoGeral} ${classeVariante} ${!estaBloqueado && !modoDesempenho ? estilos.botaoFluido : ""}`}
          disabled={estaBloqueado}
          {...propriedadesBotao}
        >
          {!estaBloqueado && !modoDesempenho && (
            <span className={estilos.bolhasFluido}>
              {Array.from({ length: 10 }).map((_, indice) => (
                <span key={`bolha-decorativa-${indice + 1}`} className={estilos.bolhaUnica}></span>
              ))}
            </span>
          )}
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
