import React, { useState, useEffect, useId } from "react";
import { AlertTriangle, CheckCircle, Info } from "lucide-react";
import { CONFIG } from "../config";
import { useDialogoAcessivel } from "./acessibilidadeDialogo";
import estilos from "./HomologacaoModal.module.css";

const HomologacaoModal = () => {
  const [aberto, setAberto] = useState(false);
  const [naoMostrarNovamente, setNaoMostrarNovamente] = useState(false);
  const buildProducao = CONFIG.BUILD_PRODUCAO;
  const tituloId = useId();

  const CHAVE_ARMAZENAMENTO = `ocultarHomologacao_${CONFIG.APP_NAME}_Expiracao`;

  useEffect(() => {
    if (buildProducao) return;
    try {
      const dadosExpiracao = localStorage.getItem(CHAVE_ARMAZENAMENTO);
      const tempoExpiracao: unknown = dadosExpiracao ? JSON.parse(dadosExpiracao) : null;
      if (typeof tempoExpiracao === "number" && Number.isFinite(tempoExpiracao) && tempoExpiracao > Date.now()) {
        setAberto(false);
        return;
      }
      if (dadosExpiracao) localStorage.removeItem(CHAVE_ARMAZENAMENTO);
    } catch {
      // Preferência local opcional: dados inválidos ou armazenamento bloqueado
      // não podem interromper o acesso à aplicação nem ocultar o aviso.
    }
    setAberto(true);
  }, [CHAVE_ARMAZENAMENTO, buildProducao]);

  const lidarComFechamento = () => {
    if (naoMostrarNovamente) {
      const limiteEmMs = CONFIG.HOMOLOGACAO_EXPIRY_DAYS * 24 * 60 * 60 * 1000;
      const expiraEm = new Date().getTime() + limiteEmMs;
      try {
        localStorage.setItem(CHAVE_ARMAZENAMENTO, JSON.stringify(expiraEm));
      } catch {
        // O aviso volta no próximo acesso se o navegador não puder lembrar.
      }
    }
    setAberto(false);
  };

  const referenciaDialogo = useDialogoAcessivel({
    aberto: aberto && !buildProducao,
    aoFechar: lidarComFechamento,
  });

  if (buildProducao || !aberto) return null;

  return (
    <div className={estilos.sobreposicaoModal}>
      <div className={estilos.cartaoModal} ref={referenciaDialogo} role="dialog" aria-modal="true" aria-labelledby={tituloId} tabIndex={-1}>
        <div className={estilos.cabecalhoAviso}>
          <div className={estilos.fundoTracejado}></div>
          <div className={estilos.envoltorioIcone}>
            <AlertTriangle
              className={estilos.iconeAviso}
              size={32}
              strokeWidth={2.5}
            />
          </div>
          <h2 id={tituloId} className={estilos.tituloAviso} tabIndex={-1} data-foco-inicial>Ambiente de DESENVOLVIMENTO</h2>
          <p className={estilos.subtituloAviso}>
            {CONFIG.APP_NAME} 
          </p>
        </div>

        <div className={estilos.corpoModal}>
          <p>
            Olá! Seja bem-vindo(a) ao{" "}
            <strong>{CONFIG.APP_NAME} - DESENVOLVIMENTO</strong>. Você está
            navegando em uma versão de testes de novas funcionalidades.
          </p>
          <div className={estilos.caixaDestaqueAzul}>
            <Info className={estilos.iconeInformacao} size={24} />
            <p>
              <strong>Use dados de teste.</strong> Este ambiente serve para
              simular rotinas e avaliar as funcionalidades do sistema.
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
