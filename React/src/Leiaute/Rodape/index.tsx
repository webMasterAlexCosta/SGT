import React, { useState, Suspense, lazy } from "react";
import { CONFIG } from "../../core/config";
import { LinksRodape } from "./components/LinksRodape";
import { StatusServidor } from "./components/StatusServidor";
import logotipoGoverno from "../../assets/logo-footer.png";
import estilos from "./index.module.css";

import iconeJava from "../../assets/tecnologias/java.svg";
import iconePostgresql from "../../assets/tecnologias/postgresql.svg";
import iconeReact from "../../assets/tecnologias/react.svg";
import iconeSpring from "../../assets/tecnologias/spring.svg";
import iconeTypescript from "../../assets/tecnologias/typescript.svg";
import iconeVitejs from "../../assets/tecnologias/vitejs.svg";

import iconeRedis from "../../assets/tecnologias/redis.svg";

const ModalCreditos = lazy(() =>
  import("../../core/ui/ModalCreditosColaboracao").then((m) => ({
    default: m.ModalCreditosColaboracao,
  })),
);

const Rodape = () => {
  const [modalCreditosAberto, definirModalCreditosAberto] = useState(false);

  return (
    <>
      <footer className={estilos.rodapeGeral}>
        <div className={estilos.topo}>
          <div className={estilos.interno}>
            <LinksRodape
              aoAbrirCreditos={() => definirModalCreditosAberto(true)}
            />
            <div className={estilos.tecnologias} aria-label="Tecnologias utilizadas">
              <strong>Tecnologias utilizadas</strong>
              <ul>{[
                ['React', iconeReact], ['TypeScript', iconeTypescript], ['Vite', iconeVitejs],
                ['Java 25', iconeJava], ['Spring Boot', iconeSpring], ['PostgreSQL', iconePostgresql], ['Redis', iconeRedis],
              ].map(([nome, icone]) => <li key={nome}><img src={icone} alt="" width="24" height="24" loading="lazy" /><span>{nome}</span></li>)}</ul>
              <a href="/licenses/devicon.txt" target="_blank" rel="noopener noreferrer">Ícones Devicon · MIT</a>
            </div>
          </div>
        </div>

        <div className={estilos.fundo}>
          <div className={`${estilos.interno} ${estilos.fundoInterno}`}>
            <div className={estilos.marca}>
              <img
                src={logotipoGoverno}
                alt="Logo"
                width="166"
                height="38"
                className={estilos.imagemMarca}
                loading="lazy"
              />
            </div>

            <div className={estilos.direitosAutorais}>
              <p>
                © {new Date().getFullYear()} Sistema desenvolvido por{" "}
                {CONFIG.AUTHOR_NAME} {CONFIG.AUTHOR_MATRICULA} –{" "}
                {CONFIG.COMPANY_DEPARTMENT}
              </p>
              <p className={estilos.cnpj}>CNPJ {CONFIG.APP_CNPJ}</p>
              <div className={estilos.metadadosSistema}>
                <span
                  className={estilos.seloVersao}
                  title={`Versão instalada: ${CONFIG.APP_VERSION}`}
                >
                  Versão {CONFIG.APP_VERSION}
                </span>
                <StatusServidor />
              </div>
            </div>
          </div>
        </div>
      </footer>

      <Suspense fallback={null}>
        {modalCreditosAberto && (
          <ModalCreditos
            isOpen={modalCreditosAberto}
            onClose={() => definirModalCreditosAberto(false)}
          />
        )}
      </Suspense>
    </>
  );
};

export default Rodape;
