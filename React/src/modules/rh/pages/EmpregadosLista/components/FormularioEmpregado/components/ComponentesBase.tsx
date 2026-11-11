import React, { memo } from "react";
import { Lock, Edit } from "lucide-react";
import estilos from "./ComponentesBase.module.css";

export const ComponenteCampoBloqueado = memo(({ children, ehNovoRegistro, estaDesbloqueadoParaEdicao, aoAlternarTrava, rotuloDoCampo }: React.PropsWithChildren<{ ehNovoRegistro?: boolean; estaDesbloqueadoParaEdicao?: boolean; aoAlternarTrava: () => void; rotuloDoCampo: string }>) => {
  if (ehNovoRegistro) return (
    <div className={estilos.conteudoCampoBloqueado}>{children}</div>
  );
  
  return (
    <div className={estilos.envoltorioCampoBloqueado}>
      <div className={estilos.conteudoCampoBloqueado}>{children}</div>
      <button 
        type="button" 
        onClick={aoAlternarTrava} 
        className={`${estilos.botaoCampoBloqueado} ${estaDesbloqueadoParaEdicao ? estilos.botaoCampoBloqueadoDesbloqueado : estilos.botaoCampoBloqueadoBloqueado}`}
        title={estaDesbloqueadoParaEdicao ? `Bloquear edição: ${rotuloDoCampo}` : `Liberar edição: ${rotuloDoCampo}`}
        aria-label={estaDesbloqueadoParaEdicao ? `Bloquear edição de ${rotuloDoCampo || "campo"}` : `Liberar edição de ${rotuloDoCampo || "campo"}`}
      >
        {estaDesbloqueadoParaEdicao ? <Lock size={18} /> : <Edit size={18} />}
      </button>
    </div>
  );
});

ComponenteCampoBloqueado.displayName = "ComponenteCampoBloqueado";

export const ComponenteInterruptorLogico = memo(({ rotuloTexto, estaMarcado, aoAlternarMudanca, nomeAtributo, estaDesabilitado }: { rotuloTexto: string; estaMarcado?: boolean; aoAlternarMudanca: React.ChangeEventHandler<HTMLInputElement>; nomeAtributo: string; estaDesabilitado?: boolean }) => (
  <label className={`${estilos.envoltorioInterruptor} ${estaDesabilitado ? estilos.interruptorDesabilitado : ""}`}>
    <div className={estilos.envoltorioTrilhaInterruptor}>
      <input type="checkbox" name={nomeAtributo} checked={estaMarcado} onChange={aoAlternarMudanca} disabled={estaDesabilitado} className={estilos.entradaInterruptor} />
      <div className={`${estilos.trilhaInterruptor} ${estaMarcado ? estilos.trilhaInterruptorMarcado : ""}`} />
      <div className={`${estilos.polegarInterruptor} ${estaMarcado ? estilos.polegarInterruptorMarcado : ""}`} />
    </div>
    <span className={estilos.rotuloInterruptor}>{rotuloTexto}</span>
  </label>
));

ComponenteInterruptorLogico.displayName = "ComponenteInterruptorLogico";

export const ComponenteCartaoDeSecao = memo(({ iconeDecorativo: Icone, tituloDaSecao, children, classeAdicional = "" }: React.PropsWithChildren<{ iconeDecorativo: import("lucide-react").LucideIcon; tituloDaSecao: string; classeAdicional?: string }>) => (
  <div className={`${estilos.cartaoSecao} ${classeAdicional}`}>
    <div className={estilos.cabecalhoSecao}>
      <div className={estilos.envoltorioIconeSecao}>
        <Icone size={18} strokeWidth={2.5} />
      </div>
      <h3 className={estilos.tituloSecao}>{tituloDaSecao}</h3>
    </div>
    <div className={estilos.corpoSecao}>{children}</div>
  </div>
));

ComponenteCartaoDeSecao.displayName = "ComponenteCartaoDeSecao";