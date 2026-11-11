import React, { memo } from "react";
import { EnvolucroDica } from "../../../core/ui/EnvolucroDica";
import estilos from "../index.module.css";

export const ItemNavegacao = memo(
  ({
    icone: Icone,
    rotulo,
    id,
    ativo,
    aoNavegar,
    dica,
    posicaoDica = "right",
    eSubItem = false,
  }: { icone: import("lucide-react").LucideIcon; rotulo: React.ReactNode; id: string; ativo?: boolean; aoNavegar: (id: string) => void; dica?: string; posicaoDica?: string; eSubItem?: boolean }) => (
    <EnvolucroDica
      tooltip={dica}
      position={posicaoDica}
      wrapperClass={estilos.envoltorioItemDica}
    >
      <button
        onClick={() => aoNavegar(id)}
        className={`${estilos.botaoItemNavegacao} ${eSubItem ? estilos.itemNavegacaoSub : estilos.itemNavegacaoNormal} ${ativo ? estilos.itemAtivo : estilos.itemInativo}`}
        type="button"
      >
        <Icone size={eSubItem ? 16 : 20} className={estilos.iconeItem} />
        <span
          className={`${estilos.textoItem} ${eSubItem ? estilos.textoItemSub : estilos.textoItemNormal}`}
        >
          {rotulo}
        </span>
      </button>
    </EnvolucroDica>
  ),
);

ItemNavegacao.displayName = "ItemNavegacao";
