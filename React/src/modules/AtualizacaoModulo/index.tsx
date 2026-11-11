import React, { useEffect, useState, useRef, useMemo } from "react";
import { CONFIG } from "../../core/config";
import logotipoCorreios from "../../assets/correios.png";
import { ServicoSistema } from "../../services/system/ServicoSistema";

import { TerminalHistorico } from "./components/TerminalHistorico";
import { PainelHUDDiagnostico } from "./components/PainelHUDDiagnostico";
import { PainelHUDSeguranca } from "./components/PainelHUDSeguranca";
import { ReatorCentral } from "./components/ReatorCentral";

import estilos from "./index.module.css";

const PASSOS_INTRODUCAO = [
  { label: `INICIALIZANDO MATRIZ DE SEGURANÇA ${CONFIG.APP_NAME}...`, pct: 15 },
  { label: "NEGOCIANDO CERTIFICADO TLS...", pct: 35 },
  { label: "EXTRAINDO PACOTES DA APLICAÇÃO...", pct: 55 },
  { label: "VALIDANDO CREDENCIAIS...", pct: 75 },
  { label: `SISTEMA ONLINE. BEM-VINDO AO ${CONFIG.APP_NAME}.`, pct: 95 },
];

const PASSOS_SAIDA = [
  { label: "DESMONTANDO INTERFACE GRÁFICA...", pct: 15 },
  { label: "PURGANDO REGISTOS TEMPORÁRIOS...", pct: 35 },
  { label: "ENCERRANDO TÚNEIS DE CONEXÃO...", pct: 55 },
  { label: "SESSÃO INVALIDADA NO GATEWAY...", pct: 75 },
  { label: "DESCONEXÃO LIMPA EFETUADA.", pct: 95 },
];

export default function AtualizacaoModulo({
  ready: estaPronto = false,
  onDone: aoConcluir,
  minDurationMs: duracaoMinimaMs = CONFIG.INTRO_MIN_DURATION_MS,
  mode: modo = "intro",
}: { ready?: boolean; onDone: () => void; minDurationMs?: number; mode?: string }) {
  const [visivel, definirVisivel] = useState(true);
  const [saindo, definirSaindo] = useState(false);
  const [progresso, definirProgresso] = useState(0);
  const [rotulo, definirRotulo] = useState(
    modo === "intro" ? "AGUARDANDO BOOT..." : "INICIANDO CORTE...",
  );

  const [historico, definirHistorico] = useState<string[]>([]);
  const referenciaRotuloAnterior = useRef(rotulo);

  const [latencia, definirLatencia] = useState(0);
  const [memoria, definirMemoria] = useState(0);
  const [cargaCpu, definirCargaCpu] = useState(0);
  const [statusSistema, definirStatusSistema] = useState(
    "CONECTANDO AO SERVIDOR...",
  );

  const [usuarioSo, definirUsuarioSo] = useState("VERIFICANDO...");
  const [nomeNavegador, definirNomeNavegador] = useState("IDENTIFICANDO...");
  const [ipCliente, definirIpCliente] = useState("OBTENDO IP...");
  const [tokenAutenticacao, definirTokenAutenticacao] =
    useState("VERIFICANDO...");

  const [tempoConcluido, definirTempoConcluido] = useState(false);
  const referenciaFinalizado = useRef(false);

  const passosAtuais = useMemo(
    () => (modo === "logout" ? PASSOS_SAIDA : PASSOS_INTRODUCAO),
    [modo],
  );

  useEffect(() => {
    if (rotulo !== referenciaRotuloAnterior.current) {
      definirHistorico((estadoAnterior) =>
        [...estadoAnterior, referenciaRotuloAnterior.current].slice(-2),
      );
      referenciaRotuloAnterior.current = rotulo;
    }
  }, [rotulo]);

  useEffect(() => {
    let idQuadroAnimacao: number;
    let ultimoTempo = performance.now();

    const medirCpu = () => {
      const agora = performance.now();
      const delta = agora - ultimoTempo;
      let carga = Math.round(((delta - 16.6) / 16.6) * 100);
      if (carga < 2) carga = Math.floor(Math.random() * 5) + 2;
      if (carga > 100) carga = 100;

      definirCargaCpu(carga);
      ultimoTempo = agora;
      idQuadroAnimacao = requestAnimationFrame(medirCpu);
    };
    medirCpu();

    const agenteUsuario = navigator.userAgent;
    if (agenteUsuario.includes("Edg")) definirNomeNavegador("MICROSOFT EDGE");
    else if (agenteUsuario.includes("Chrome"))
      definirNomeNavegador("GOOGLE CHROME");
    else if (agenteUsuario.includes("Firefox"))
      definirNomeNavegador("MOZILLA FIREFOX");
    else if (agenteUsuario.includes("Safari"))
      definirNomeNavegador("APPLE SAFARI");
    else definirNomeNavegador("NAVEGADOR DESCONHECIDO");

    const atualizarMetricasReais = async () => {
      try {
        const inicioLatencia = performance.now();
        const urlBaseBackend = CONFIG.API_URL.replace(/\/api\/?$/, "");
        const configuracaoServidor = await ServicoSistema.carregarRuntimeConfig(urlBaseBackend);
        definirLatencia(Math.round(performance.now() - inicioLatencia));

        if (configuracaoServidor.EM_MANUTENCAO) {
          definirStatusSistema(
            `MANUTENÇÃO: ${configuracaoServidor.PREVISAO_RETORNO || "SEM PREVISÃO"}`,
          );
        } else {
          definirStatusSistema("ONLINE / ESTÁVEL");
        }

        definirIpCliente(configuracaoServidor.CLIENT_IP || "DESCONHECIDO");
      } catch (e) {
        definirLatencia(999);
        definirStatusSistema("OFFLINE / DESCONECTADO");
        definirIpCliente("DESCONECTADO");
      }

      if (window.performance && 'memory' in window.performance && typeof window.performance.memory === 'object' && window.performance.memory !== null && 'usedJSHeapSize' in window.performance.memory) {
        definirMemoria(
          Number((Number(window.performance.memory.usedJSHeapSize) / (1024 * 1024)).toFixed(1)),
        );
      } else {
        definirMemoria(Number((performance.now() / 200).toFixed(1)));
      }

      definirTokenAutenticacao("[ ENCRYPTED TOKEN ]");

      // Identidade/autorização nunca é reconstruída de Web Storage. Este HUD
      // exibe apenas informação local do ambiente do navegador.
      const plataforma = String(navigator.platform || "WEB").toUpperCase();
      definirUsuarioSo(`${plataforma}\\SECURE_SESSION`);
    };

    atualizarMetricasReais();
    const intervaloMetricas = setInterval(atualizarMetricasReais, 800);

    return () => {
      cancelAnimationFrame(idQuadroAnimacao);
      clearInterval(intervaloMetricas);
    };
  }, []);

  useEffect(() => {
    const tempoInicial = performance.now();
    const duracao = duracaoMinimaMs;
    const intervaloProgresso = setInterval(() => {
      const decorrido = performance.now() - tempoInicial;
      const porcentagem = Math.min((decorrido / duracao) * 100, 100);
      definirProgresso(porcentagem);
      const passoEncontrado = passosAtuais.find(
        (passo) => porcentagem <= passo.pct,
      );

      if (passoEncontrado) definirRotulo(passoEncontrado.label);

      if (decorrido >= duracao) {
        clearInterval(intervaloProgresso);
        definirTempoConcluido(true);
      }
    }, 50);
    return () => clearInterval(intervaloProgresso);
  }, [duracaoMinimaMs, passosAtuais]);

  useEffect(() => {
    const liberadoParaConcluir = modo === "logout" ? true : estaPronto;
    if (!tempoConcluido || !liberadoParaConcluir) return;
    if (referenciaFinalizado.current) return;

    referenciaFinalizado.current = true;

    if (modo === "logout") {
      if (aoConcluir) aoConcluir();
    } else {
      definirSaindo(true);
      const temporizador = setTimeout(() => {
        definirVisivel(false);
        if (aoConcluir) aoConcluir();
      }, 1200);
      return () => clearTimeout(temporizador);
    }
  }, [tempoConcluido, estaPronto, aoConcluir, modo]);

  if (!visivel) return null;

  return (
    <div
      className={`${estilos.conteinerPrincipal} ${saindo ? estilos.animacaoSaida : ""}`}
    >
      <div className={estilos.gradeFundo}></div>
      <div className={estilos.linhasVarredura}></div>

      <PainelHUDDiagnostico
        statusSistema={statusSistema}
        latencia={latencia}
        memoria={memoria}
        cargaCpu={cargaCpu}
        nomeNavegador={nomeNavegador}
      />

      <PainelHUDSeguranca
        usuarioSo={usuarioSo}
        nomeNavegador={nomeNavegador}
        tokenAutenticacao={tokenAutenticacao}
        ipCliente={ipCliente}
      />

      <ReatorCentral
        progresso={progresso}
        logotipoCorreios={logotipoCorreios}
      />

      <TerminalHistorico historico={historico} rotuloAtual={rotulo} />
    </div>
  );
}
