import React, { useMemo, useState, useEffect } from "react";
import { RefreshCw, Hourglass } from "lucide-react";
import { useAutenticacao } from "../context";
import { CONFIG } from "../../../core/config";
import estilos from "./ContagemRegressivaSessao.module.css";

const preencherZeros = (num) =>
  String(Math.max(0, Math.floor(num))).padStart(2, "0");

const formatarHoraMinutoSegundo = (milissegundos) => {
  const totalSegundos = Math.max(0, Math.floor(Number(milissegundos) / 1000));
  const horas = Math.floor(totalSegundos / 3600);
  const minutos = Math.floor((totalSegundos % 3600) / 60);
  const segundos = totalSegundos % 60;
  return `${preencherZeros(horas)}:${preencherZeros(minutos)}:${preencherZeros(segundos)}`;
};

export const ContagemRegressivaSessao = React.memo(({ aoEstenderSessao }) => {
  const { sessionExpiresAtEpochMs: expiraEmMsSessao } = useAutenticacao();
  const [restanteMsSessao, setRestanteMsSessao] = useState(null);

  const TEMPO_MAXIMO = CONFIG.SESSION_MAX_TIME_MS;

  useEffect(() => {
    if (!expiraEmMsSessao) {
      setRestanteMsSessao(null);
      return;
    }
    const atualizarContagemRegressiva = () => {
      setRestanteMsSessao(Math.max(0, Number(expiraEmMsSessao) - Date.now()));
    };
    atualizarContagemRegressiva();
    const id = setInterval(atualizarContagemRegressiva, 1000);
    return () => clearInterval(id);
  }, [expiraEmMsSessao]);

  const visualizacao = useMemo(() => {
    if (restanteMsSessao == null || !Number.isFinite(Number(restanteMsSessao)))
      return null;

    const restante = Number(restanteMsSessao);
    let statusAtual = "seguro";

    if (restante <= 10 * 60 * 1000) statusAtual = "critico";
    else if (restante <= 30 * 60 * 1000) statusAtual = "aviso";

    const progressoAtual = Math.min(
      100,
      Math.max(0, (restante / TEMPO_MAXIMO) * 100),
    );

    const titulo = expiraEmMsSessao
      ? `Sessão válida até: ${new Date(Number(expiraEmMsSessao)).toLocaleTimeString("pt-BR")}`
      : "Sessão ativa";

    return {
      status: statusAtual,
      rotulo: formatarHoraMinutoSegundo(restante),
      progresso: progressoAtual,
      titulo,
    };
  }, [restanteMsSessao, expiraEmMsSessao, TEMPO_MAXIMO]);

  if (!visualizacao) return null;

  const temas = {
    seguro: {
      envoltorio: estilos.estadoSeguro,
      progresso: estilos.progressoSeguro,
      animacaoIcone: estilos.animacaoSeguro,
      hoverBotao: estilos.hoverSeguro,
    },
    aviso: {
      envoltorio: estilos.estadoAviso,
      progresso: estilos.progressoAviso,
      animacaoIcone: estilos.animacaoAviso,
      hoverBotao: estilos.hoverAviso,
    },
    critico: {
      envoltorio: estilos.estadoCritico,
      progresso: estilos.progressoCritico,
      animacaoIcone: estilos.animacaoCritico,
      hoverBotao: estilos.hoverCritico,
    },
  };

  const temaAtual = temas[visualizacao.status];

  return (
    <div
      className={`${estilos.envoltorioContador} ${temaAtual.envoltorio}`}
      title={visualizacao.titulo}
    >
      <div
        className={`${estilos.barraProgresso} ${temaAtual.progresso}`}
        style={{ transform: `scaleX(${visualizacao.progresso / 100})` }}
      />
      <div className={estilos.brilhoTopo} />
      <div className={estilos.conteudoInterno}>
        <Hourglass
          size={18}
          className={`${estilos.iconeAnimado} ${temaAtual.animacaoIcone}`}
          strokeWidth={2.5}
        />
        <div className={estilos.textosContador}>
          <span className={estilos.rotuloTopo}>Tempo de Sessão</span>
          <span className={estilos.tempoDigitos}>{visualizacao.rotulo}</span>
        </div>
        {aoEstenderSessao && (
          <>
            <div className={estilos.divisorRenovar} />
            <button
              onClick={(evento) => {
                evento.preventDefault();
                evento.stopPropagation();
                aoEstenderSessao();
              }}
              className={`${estilos.botaoRenovar} ${temaAtual.hoverBotao}`}
              title="Renovar Sessão Agora"
            >
              <RefreshCw
                size={14}
                strokeWidth={3}
                className={
                  visualizacao.status !== "seguro"
                    ? estilos.iconeGirandoRenovar
                    : ""
                }
              />
            </button>
          </>
        )}
      </div>
    </div>
  );
});

ContagemRegressivaSessao.displayName = "ContagemRegressivaSessao";
