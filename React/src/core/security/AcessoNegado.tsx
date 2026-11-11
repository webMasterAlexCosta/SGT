import React from "react";
import { ShieldX } from "lucide-react";
import { useNavigate } from "react-router-dom";
import estilos from "./AcessoNegado.module.css";

export const AcessoNegado = ({ rotaRetorno = "/perfil" }: { rotaRetorno?: "/painel" | "/frequencia" | "/perfil" }) => {
  const navegar = useNavigate();
  return (
    <section className={estilos.container} role="alert" aria-live="polite">
      <ShieldX size={46} aria-hidden="true" />
      <div>
        <h1>Acesso não autorizado</h1>
        <p>Seu perfil autenticado não possui capacidade para esta funcionalidade.</p>
        <p className={estilos.observacao}>A interface não altera seu perfil nem tenta contornar a decisão do servidor.</p>
      </div>
      <button type="button" onClick={() => navegar(rotaRetorno, { replace: true })}>
        Voltar para uma área permitida
      </button>
    </section>
  );
};
