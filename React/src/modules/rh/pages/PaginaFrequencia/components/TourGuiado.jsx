import React, { useState, useEffect } from "react";
import { createPortal } from "react-dom";
import { Check } from "lucide-react";
import estilos from "../index.module.css";

export const TourGuiado = ({ passos, aberto, aoFechar }) => {
  const [passoAtual, setPassoAtual] = useState(0);
  const [estiloAlvo, setEstiloAlvo] = useState(null);
  const [estiloPopup, setEstiloPopup] = useState({ top: 0, left: 0, transform: "translate(-50%, -50%)" });

  // 1. Efeito para rolar a página: Dispara APENAS quando o passo mudar
  useEffect(() => {
    if (!aberto || !passos || !passos[passoAtual]) return;
    
    const passoAExecutar = passos[passoAtual];
    const elementoAlvo = document.querySelector(`[data-tour="${passoAExecutar.id}"]`);
    
    if (elementoAlvo) {
        const retangulo = elementoAlvo.getBoundingClientRect();
        // A aplicação usa um <main> rolável, não a janela. scrollIntoView encontra
        // automaticamente o ancestral correto e evita o tour ficar preso fora da tela.
        if (retangulo.top < 80 || retangulo.bottom > window.innerHeight) {
          elementoAlvo.scrollIntoView({ behavior: "smooth", block: "center", inline: "nearest" });
        }
    }
  }, [aberto, passoAtual, passos]);

  // 2. Efeito para desenhar: Acompanha o elemento ao rolar/redimensionar a tela
  useEffect(() => {
    if (!aberto) {
      setPassoAtual(0);
      setEstiloAlvo(null);
      return;
    }

    let idAnimacao;

    const atualizarPosicao = () => {
      if (!passos || !passos[passoAtual]) return;

      const passoAExecutar = passos[passoAtual];
      const elementoAlvo = document.querySelector(`[data-tour="${passoAExecutar.id}"]`);

      if (elementoAlvo) {
        const retangulo = elementoAlvo.getBoundingClientRect();
        const top = retangulo.top;
        const left = retangulo.left;

        setEstiloAlvo({
          top, left, width: retangulo.width, height: retangulo.height,
        });

        const larguraCaixaDica = 320;
        const alturaEstimadaDica = 220;
        const espacoExtra = 20;

        const larguraDireitaLivre = window.innerWidth - (retangulo.left + retangulo.width);
        const larguraEsquerdaLivre = retangulo.left;

        let posicaoTopo = top + retangulo.height / 2 - alturaEstimadaDica / 2;
        let posicaoEsquerda = 0;

        if (larguraDireitaLivre > larguraCaixaDica + espacoExtra) {
          posicaoEsquerda = left + retangulo.width + espacoExtra;
        } else if (larguraEsquerdaLivre > larguraCaixaDica + espacoExtra) {
          posicaoEsquerda = left - larguraCaixaDica - espacoExtra;
        } else {
          posicaoEsquerda = Math.max(16, window.innerWidth / 2 - larguraCaixaDica / 2);
          if (retangulo.top > alturaEstimadaDica + espacoExtra) {
            posicaoTopo = top - alturaEstimadaDica - espacoExtra;
          } else {
            posicaoTopo = top + retangulo.height + espacoExtra;
          }
        }

        const minimoTopo = 16;
        const maximoTopo = window.innerHeight - alturaEstimadaDica - 16;
        posicaoTopo = Math.max(minimoTopo, Math.min(posicaoTopo, maximoTopo));

        setEstiloPopup({ top: posicaoTopo, left: posicaoEsquerda, transform: "none" });
      } else {
        setEstiloAlvo(null);
        setEstiloPopup({ top: window.innerHeight / 2, left: window.innerWidth / 2, transform: "translate(-50%, -50%)" });
      }
    };

    const aoRolarOuRedimensionar = () => {
      if (idAnimacao) cancelAnimationFrame(idAnimacao);
      idAnimacao = requestAnimationFrame(atualizarPosicao);
    };

    atualizarPosicao();
    window.addEventListener("resize", aoRolarOuRedimensionar);
    document.addEventListener("scroll", aoRolarOuRedimensionar, true);

    return () => {
      if (idAnimacao) cancelAnimationFrame(idAnimacao);
      window.removeEventListener("resize", aoRolarOuRedimensionar);
      document.removeEventListener("scroll", aoRolarOuRedimensionar, true);
    };
  }, [aberto, passoAtual, passos]);

  if (!aberto) return null;

  return createPortal(
    <div className={estilos.sobreposicaoTour}>
      {estiloAlvo && (
        <div className={estilos.destaqueTour} style={{ top: estiloAlvo.top - 8, left: estiloAlvo.left - 8, width: estiloAlvo.width + 16, height: estiloAlvo.height + 16 }}>
          <div className={estilos.destaqueTourInterno}></div>
        </div>
      )}
      <div className={estilos.popupTour} style={{ top: estiloPopup.top, left: estiloPopup.left, transform: estiloPopup.transform }}>
        <h3 className={estilos.cabecalhoTour}>
          <span className={estilos.emblemaTour}>{passoAtual + 1}</span>
          {passos[passoAtual].title}
        </h3>
        <p className={estilos.textoTour}>{passos[passoAtual].text}</p>
        <div className={estilos.rodapeTour}>
          <button onClick={aoFechar} className={estilos.botaoPularTour}>Pular Dicas</button>
          <div className={estilos.acoesTour}>
            <button onClick={() => setPassoAtual((ant) => Math.max(0, ant - 1))} disabled={passoAtual === 0} className={estilos.botaoVoltarTour}>Voltar</button>
            {passoAtual < passos.length - 1 ? (
              <button onClick={() => setPassoAtual((ant) => ant + 1)} className={estilos.botaoAvancarTour}>Avançar</button>
            ) : (
              <button onClick={aoFechar} className={estilos.botaoConcluirTour}><Check size={14} /> Entendido!</button>
            )}
          </div>
        </div>
      </div>
    </div>,
    document.body
  );
};
