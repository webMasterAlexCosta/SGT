import React from "react";
import { Menu, X } from "lucide-react";
import { ContagemRegressivaSessao } from "../../modules/auth/components/ContagemRegressivaSessao";
import { InformacoesUsuario } from "./components/InformacoesUsuario";
import { AcoesCabecalho } from "./components/AcoesCabecalho";
import estilos from "./index.module.css";

const Cabecalho = ({
  user: usuario,
  aoSair,
  onNavigate: aoNavegar,
  setMobileMenu: definirMenuMobile,
  menuMobileAberto = false,
}: { user: import("../../contracts/auth").AuthenticatedUser | null; aoSair: () => void; onNavigate: (id: string) => void; setMobileMenu: React.Dispatch<React.SetStateAction<boolean>>; menuMobileAberto?: boolean }) => (
  <header className={estilos.cabecalho}>
    <button
      onClick={() => definirMenuMobile((valorAtual) => !valorAtual)}
      className={estilos.botaoMenu}
      type="button"
      aria-label={menuMobileAberto ? "Fechar menu principal" : "Abrir menu principal"}
      aria-expanded={menuMobileAberto}
      aria-controls="navegacao-principal"
    >
      {menuMobileAberto ? <X aria-hidden="true" /> : <Menu aria-hidden="true" />}
    </button>

    <div className={estilos.conteinerAcoes}>
      <ContagemRegressivaSessao />
      <InformacoesUsuario usuario={usuario} />
      <AcoesCabecalho usuario={usuario} aoSair={aoSair} aoNavegar={aoNavegar} />
    </div>
  </header>
);

export default Cabecalho;
