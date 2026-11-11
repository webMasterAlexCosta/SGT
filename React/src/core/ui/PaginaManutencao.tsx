import React, { useState, useEffect } from "react";
import { Wrench, Clock, Activity } from "lucide-react";
import { CONFIG } from "../../core/config";
import logotipo from "../../assets/logo-siggtrat.png";
import estilos from "./PaginaManutencao.module.css";

export const PaginaManutencao = ({ previsaoRetorno = CONFIG.PREVISAO_RETORNO }: { previsaoRetorno?: string }) => {
  const [tempoRestante, setTempoRestante] = useState({
    horas: 0,
    minutos: 0,
    segundos: 0,
    expirou: false,
    temAlvo: false,
  });

  useEffect(() => {
    const stringAlvo = previsaoRetorno;
    if (!stringAlvo) return;

    const tempoAlvo = new Date(stringAlvo).getTime();

    const calcularTempoRestante = () => {
      const agora = new Date().getTime();
      const diferenca = tempoAlvo - agora;

      if (diferenca <= 0) {
        setTempoRestante({
          horas: 0,
          minutos: 0,
          segundos: 0,
          expirou: true,
          temAlvo: true,
        });
        return;
      }

      setTempoRestante({
        horas: Math.floor(
          (diferenca % (1000 * 60 * 60 * 24)) / (1000 * 60 * 60),
        ),
        minutos: Math.floor((diferenca % (1000 * 60 * 60)) / (1000 * 60)),
        segundos: Math.floor((diferenca % (1000 * 60)) / 1000),
        expirou: false,
        temAlvo: true,
      });
    };

    calcularTempoRestante();
    const idCronometro = setInterval(calcularTempoRestante, 1000);
    return () => clearInterval(idCronometro);
  }, [previsaoRetorno]);

  const formatarNumero = (num: (number)) => num.toString().padStart(2, "0");

  return (
    <div className={estilos.containerPagina}>
      <div className={estilos.decoracaoFundo}>
        <Wrench size={300} className={estilos.chaveEsquerda} />
        <Wrench size={400} className={estilos.chaveDireita} />
      </div>

      <div className={estilos.cartaoManutencao}>
        <div className={estilos.envoltorioLogotipo}>
          <img
            src={logotipo}
            alt={`Logo ${CONFIG.APP_NAME}`}
            className={estilos.logotipoApp}
          />
        </div>

        <div className={estilos.envoltorioIconeCentro}>
          <Activity className={estilos.iconePulso} size={50} />
          <Wrench className={estilos.iconeCentral} size={36} />
        </div>

        <h1 className={estilos.tituloManutencao}>Sistema em Manutenção</h1>

        <p className={estilos.textoDescricao}>
          Estamos realizando atualizações e melhorias na infraestrutura para
          garantir que o <strong>{CONFIG.APP_NAME}</strong> continue rápido,
          seguro e eficiente.
        </p>

        <div className={estilos.caixaTempo}>
          <div className={estilos.conteudoCaixaTempo}>
            <Clock className={estilos.iconeRelogio} size={24} />
            <div style={{ flex: 1 }}>
              <h3 className={estilos.tituloCaixaTempo}>Previsão de Retorno</h3>

              {!tempoRestante.temAlvo ? (
                <p className={estilos.textoSemPrevisao}>
                  Nossa equipe técnica está trabalhando. O acesso será
                  restabelecido em breve. Agradecemos a sua paciência!
                </p>
              ) : tempoRestante.expirou ? (
                <div className={estilos.textoAjustesFinais}>
                  <Activity size={18} className={estilos.iconeFinalizando} />
                  Finalizando ajustes! Retornando a qualquer momento...
                </div>
              ) : (
                <div className={estilos.gradeTempo}>
                  <div className={estilos.blocoNumero}>
                    <span className={estilos.numeroTempo}>
                      {formatarNumero(tempoRestante.horas)}
                    </span>
                    <span className={estilos.rotuloTempo}>Horas</span>
                  </div>
                  <div className={estilos.doisPontos}>:</div>
                  <div className={estilos.blocoNumero}>
                    <span className={estilos.numeroTempo}>
                      {formatarNumero(tempoRestante.minutos)}
                    </span>
                    <span className={estilos.rotuloTempo}>Min</span>
                  </div>
                  <div className={estilos.doisPontos}>:</div>
                  <div className={estilos.blocoNumero}>
                    <span className={estilos.numeroTempo}>
                      {formatarNumero(tempoRestante.segundos)}
                    </span>
                    <span className={estilos.rotuloTempo}>Seg</span>
                  </div>
                </div>
              )}
            </div>
          </div>
        </div>
      </div>

      <div className={estilos.rodapeManutencao}>
        &copy; {new Date().getFullYear()} {CONFIG.APP_NAME} -{" "}
        {CONFIG.APP_COMPANY}
      </div>
    </div>
  );
};
