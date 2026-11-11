import React, { useState, Suspense, lazy } from "react";
import { CONFIG } from "../../core/config";
import { LinksRodape } from "./components/LinksRodape";
import { StatusServidor } from "./components/StatusServidor";
import logotipoGoverno from "../../assets/logo-footer.png";
import estilos from "./index.module.css";

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
