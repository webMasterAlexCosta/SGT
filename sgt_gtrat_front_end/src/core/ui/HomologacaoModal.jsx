import React, { useState, useEffect } from "react";
import { AlertTriangle, CheckCircle, Info } from "lucide-react";
import { CONFIG } from "../config";
import estilos from "./HomologacaoModal.module.css";

const HomologacaoModal = () => {
  const [aberto, setAberto] = useState(false);
  const [naoMostrarNovamente, setNaoMostrarNovamente] = useState(false);

  const CHAVE_ARMAZENAMENTO = `ocultarHomologacao_${CONFIG.APP_NAME}_Expiracao`;

  useEffect(() => {
    const dadosExpiracao = localStorage.getItem(CHAVE_ARMAZENAMENTO);

    if (dadosExpiracao) {
      const agora = new Date().getTime();
      const tempoExpiracao = JSON.parse(dadosExpiracao);

      if (agora > tempoExpiracao) {
        localStorage.removeItem(CHAVE_ARMAZENAMENTO);
        setAberto(true);
      } else {
        setAberto(false);
      }
    } else {
      setAberto(true);
    }
  }, [CHAVE_ARMAZENAMENTO]);

  const lidarComFechamento = () => {
    if (naoMostrarNovamente) {
      const limiteEmMs = CONFIG.HOMOLOGACAO_EXPIRY_DAYS * 24 * 60 * 60 * 1000;
      const expiraEm = new Date().getTime() + limiteEmMs;
      localStorage.setItem(CHAVE_ARMAZENAMENTO, JSON.stringify(expiraEm));
    }
    setAberto(false);
  };

  if (!aberto) return null;

  return (
    <div className={estilos.sobreposicaoModal}>
      <div className={estilos.cartaoModal}>
        <div className={estilos.cabecalhoAviso}>
          <div className={estilos.fundoTracejado}></div>
          <div className={estilos.envoltorioIcone}>
            <AlertTriangle
              className={estilos.iconeAviso}
              size={32}
              strokeWidth={2.5}
            />
          </div>
          <h2 className={estilos.tituloAviso}>Ambiente de DESENVOLVIMENTO</h2>
          <p className={estilos.subtituloAviso}>
            {CONFIG.APP_NAME} 
          </p>
        </div>

        <div className={estilos.corpoModal}>
          <p>
            Olá! Seja bem-vindo(a) ao{" "}
            <strong>{CONFIG.APP_NAME} - DESENVOLVIMENTO</strong>. Você está
            navegando em uma versão de testes De novas funcionalidades.
          </p>
          <div className={estilos.caixaDestaqueAzul}>
            <Info className={estilos.iconeInformacao} size={24} />
            <p>
              <strong>Fique tranquilo(a)!</strong> Os dados inseridos, alterados
              ou apagados aqui{" "}
              <strong> não afetam o ambiente oficial de produção</strong>.
            </p>
          </div>
          <p>
            Este é o seu espaço para explorar as novidades, simular rotinas
            diárias e "quebrar" o que for preciso. Sua participação é nossa
            melhor ferramenta para evoluir!
          </p>
        </div>

        <div className={estilos.rodapeModal}>
          <label className={estilos.rotuloCheckbox}>
            <div className={estilos.caixaCustomizada}>
              <input
                type="checkbox"
                className={estilos.entradaEscondida}
                checked={naoMostrarNovamente}
                onChange={(evento) =>
                  setNaoMostrarNovamente(evento.target.checked)
                }
              />
              <div
                className={`${estilos.fundoMarcado} ${naoMostrarNovamente ? estilos.fundoAtivo : estilos.fundoInativo}`}
              >
                {naoMostrarNovamente && (
                  <CheckCircle size={16} color="#ffffff" />
                )}
              </div>
            </div>
            <span className={estilos.textoCheckbox}>
              Não mostrar por {CONFIG.HOMOLOGACAO_EXPIRY_DAYS} dias
            </span>
          </label>

          <button onClick={lidarComFechamento} className={estilos.botaoCiente}>
            Ciente, Vamos lá!
          </button>
        </div>
      </div>
    </div>
  );
};

export default HomologacaoModal;
