import { useEffect, useRef } from 'react';
import { CONFIG } from '../core/config.ts';
import { dispararSaida } from '../core/events.ts';
import { obterGeracaoSessao } from '../core/security/geracaoSessao.ts';
import {
  calcularRestanteInatividade,
  criarChaveAtividadeSessao,
} from '../core/security/sessionInactivity.ts';

// Eventos realmente ligados à interação humana. O heartbeat não roda em loop:
// ele só pode ser enviado quando há atividade do usuário.
const EVENTOS_ATIVIDADE = ['pointerdown', 'keydown', 'scroll', 'touchstart'];
const THROTTLE_EVENTO_MS = 1000;
const HEARTBEAT_FINAL_DELAY_MS = 2000;
const HEARTBEAT_FINAL_MIN_INTERVAL_MS = 60000;
const MAX_TIMEOUT_NAVEGADOR_MS = 2_147_000_000;

const lerTimestamp = (chave: (string)) => {
  try {
    const valor = Number(localStorage.getItem(chave));
    return Number.isFinite(valor) && valor > 0 ? valor : null;
  } catch {
    return null;
  }
};

const salvarTimestamp = (chave: (string), valor: (number)) => {
  try {
    localStorage.setItem(chave, String(valor));
  } catch {
    // A sessão continua protegida pelo timeout autoritativo do backend mesmo
    // quando o armazenamento do navegador estiver indisponível.
  }
};

export function usePulsoAtividade(usuario: (import("../contracts/auth").AuthenticatedUser | null), atualizarInformacoesSessao: ((options?: import("../core/http/types").RequestOptions<unknown> & { touch?: boolean; skipLoading?: boolean; }) => Promise<import("../contracts/auth").SessionResponse | null>)) {
  const referenciaUltimoPing = useRef(Date.now());
  const referenciaUltimaAtividade = useRef(Date.now());
  const referenciaUltimoEvento = useRef(0);
  const referenciaTemporizadorInatividade = useRef<(number) | null>(null);
  const referenciaHeartbeatFinal = useRef<(number) | null>(null);
  const referenciaLogoutDisparado = useRef(false);

  useEffect(() => {
    const estaAutenticado = Boolean(usuario);

    if (!estaAutenticado) {
      if (referenciaTemporizadorInatividade.current) {
        window.clearTimeout(referenciaTemporizadorInatividade.current);
        referenciaTemporizadorInatividade.current = null;
      }
      if (referenciaHeartbeatFinal.current) {
        window.clearTimeout(referenciaHeartbeatFinal.current);
        referenciaHeartbeatFinal.current = null;
      }
      referenciaLogoutDisparado.current = false;
      return undefined;
    }

    const chaveAtividade = criarChaveAtividadeSessao(CONFIG.APP_NAME, usuario);
    const geracaoSessao = obterGeracaoSessao();
    const minutosInatividade = Math.round(CONFIG.INACTIVITY_TIMEOUT_MS / 60000);
    let ativo = true;

    const lidarComLogoutAutomatico = () => {
      if (!ativo || referenciaLogoutDisparado.current) return;
      referenciaLogoutDisparado.current = true;
      dispararSaida(
        `Sessão encerrada por inatividade de ${minutosInatividade} minutos.`,
        { code: 'SESSION_IDLE_TIMEOUT', geracaoSessao },
      );
    };

    const verificarPrazo = () => {
      if (!ativo) return;

      const compartilhada = lerTimestamp(chaveAtividade);
      if (
        compartilhada &&
        compartilhada > referenciaUltimaAtividade.current
      ) {
        referenciaUltimaAtividade.current = compartilhada;
      }

      const restante = calcularRestanteInatividade({
        ultimaAtividadeEm: referenciaUltimaAtividade.current,
        timeoutMs: CONFIG.INACTIVITY_TIMEOUT_MS,
      });

      if (restante <= 0) {
        lidarComLogoutAutomatico();
        return;
      }

      if (referenciaTemporizadorInatividade.current) {
        window.clearTimeout(referenciaTemporizadorInatividade.current);
      }
      referenciaTemporizadorInatividade.current = window.setTimeout(
        verificarPrazo,
        Math.min(restante, MAX_TIMEOUT_NAVEGADOR_MS),
      );
    };

    const enviarPing = async () => {
      try {
        // touch=true é a única leitura periódica autorizada a renovar a
        // inatividade no servidor. Um 401 deve seguir o fluxo global de logout.
        await atualizarInformacoesSessao?.({
          touch: true,
          skipLoading: true,
          silentError: true,
        });
      } catch {
        // Falha de rede não congela a interface. O interceptor HTTP já cuida de
        // 401 e o backend continua sendo a fonte autoritativa da sessão.
      }
    };

    const agendarHeartbeatFinal = (agora: (number)) => {
      if (referenciaHeartbeatFinal.current) {
        window.clearTimeout(referenciaHeartbeatFinal.current);
        referenciaHeartbeatFinal.current = null;
      }

      // Se o servidor foi tocado há poucos segundos, a diferença entre a
      // atividade humana e lastSeenAt já é irrelevante. Isso evita escrita no
      // banco a cada clique isolado.
      if (agora - referenciaUltimoPing.current < HEARTBEAT_FINAL_MIN_INTERVAL_MS) return;

      referenciaHeartbeatFinal.current = window.setTimeout(() => {
        referenciaHeartbeatFinal.current = null;
        if (!ativo || referenciaLogoutDisparado.current) return;
        referenciaUltimoPing.current = Date.now();
        void enviarPing();
      }, HEARTBEAT_FINAL_DELAY_MS);
    };

    const registrarAtividade = () => {
      if (!ativo || referenciaLogoutDisparado.current) return;

      const agora = Date.now();
      if (agora - referenciaUltimoEvento.current < THROTTLE_EVENTO_MS) return;

      referenciaUltimoEvento.current = agora;
      referenciaUltimaAtividade.current = agora;
      salvarTimestamp(chaveAtividade, agora);
      verificarPrazo();

      if (agora - referenciaUltimoPing.current >= CONFIG.PING_INTERVAL_MS) {
        if (referenciaHeartbeatFinal.current) {
          window.clearTimeout(referenciaHeartbeatFinal.current);
          referenciaHeartbeatFinal.current = null;
        }
        referenciaUltimoPing.current = agora;
        void enviarPing();
      } else {
        // Heartbeat trailing: quando o usuário para de interagir, registra no
        // servidor a atividade final do burst sem criar um polling contínuo.
        agendarHeartbeatFinal(agora);
      }
    };

    const sincronizarOutraAba = (evento: StorageEvent) => {
      if (!ativo || evento.key !== chaveAtividade || !evento.newValue) return;
      const timestamp = Number(evento.newValue);
      if (!Number.isFinite(timestamp) || timestamp <= referenciaUltimaAtividade.current) return;

      referenciaUltimaAtividade.current = timestamp;
      verificarPrazo();
    };

    const revalidarAoRetornar = () => {
      if (document.visibilityState === 'visible') verificarPrazo();
    };

    // Abrir/recarregar uma aba autenticada é atividade do usuário. O bootstrap
    // já consultou o backend antes de montar este hook; uma sessão vencida não
    // chega até aqui como autenticada.
    const agora = Date.now();
    referenciaUltimaAtividade.current = agora;
    referenciaUltimoPing.current = agora;
    referenciaUltimoEvento.current = 0;
    referenciaLogoutDisparado.current = false;
    salvarTimestamp(chaveAtividade, agora);
    verificarPrazo();

    EVENTOS_ATIVIDADE.forEach((evento) =>
      window.addEventListener(evento, registrarAtividade, { passive: true }),
    );
    window.addEventListener('storage', sincronizarOutraAba);
    window.addEventListener('pageshow', verificarPrazo);
    window.addEventListener('focus', verificarPrazo);
    document.addEventListener('visibilitychange', revalidarAoRetornar);

    return () => {
      ativo = false;
      if (referenciaTemporizadorInatividade.current) {
        window.clearTimeout(referenciaTemporizadorInatividade.current);
        referenciaTemporizadorInatividade.current = null;
      }
      if (referenciaHeartbeatFinal.current) {
        window.clearTimeout(referenciaHeartbeatFinal.current);
        referenciaHeartbeatFinal.current = null;
      }
      EVENTOS_ATIVIDADE.forEach((evento) =>
        window.removeEventListener(evento, registrarAtividade),
      );
      window.removeEventListener('storage', sincronizarOutraAba);
      window.removeEventListener('pageshow', verificarPrazo);
      window.removeEventListener('focus', verificarPrazo);
      document.removeEventListener('visibilitychange', revalidarAoRetornar);
    };
  }, [atualizarInformacoesSessao, usuario?.id, usuario?.matricula, usuario?.username, usuario?.user]);
}
