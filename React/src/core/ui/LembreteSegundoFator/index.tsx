import { observarFalha } from "../../http/falhaObservada";
import React, { useCallback, useEffect, useMemo, useState } from "react";
import {
  ChevronRight,
  KeyRound,
  ShieldCheck,
  Smartphone,
  X,
} from "lucide-react";
import { CONFIG } from "../../config";
import { ServicoPerfil } from "../../../services/perfil/ServicoPerfil";
import {
  calcularProximoLembrete2FA,
  criarChaveLembrete2FA,
  deveExibirLembrete2FA,
  lerProximoLembrete2FA,
  limparLembrete2FA,
  salvarProximoLembrete2FA,
  TWO_FACTOR_REMINDER_DAYS,
} from "../../security/twoFactorReminder";
import estilos from "./index.module.css";

export const LembreteSegundoFator = ({ user, onNavigate }: { user: import("../../../contracts/auth").AuthenticatedUser | null; onNavigate?: (id: string) => void }) => {
  const chaveArmazenamento = useMemo(
    () => criarChaveLembrete2FA(CONFIG.APP_NAME, user),
    [user?.id, user?.matricula, user?.username],
  );
  const [visivel, setVisivel] = useState(false);

  const aplicarStatus = useCallback(
    (ativo: (boolean)) => {
      if (ativo) {
        limparLembrete2FA(localStorage, chaveArmazenamento);
        setVisivel(false);
        return;
      }

      const proximoLembreteEm = lerProximoLembrete2FA(
        localStorage,
        chaveArmazenamento,
      );
      const deveExibir = deveExibirLembrete2FA({
        ativo: false,
        proximoLembreteEm,
      });

      if (deveExibir) {
        // O simples ato de exibir já agenda a próxima janela. Assim, sair e
        // entrar novamente no mesmo dia não transforma o aviso em spam.
        salvarProximoLembrete2FA(
          localStorage,
          chaveArmazenamento,
          calcularProximoLembrete2FA(),
        );
      }
      setVisivel(deveExibir);
    },
    [chaveArmazenamento],
  );

  useEffect(() => {
    if (!user) {
      setVisivel(false);
      return undefined;
    }

    if (user.twoFactorEnabled) {
      aplicarStatus(true);
      return undefined;
    }

    const controlador = new AbortController();
    let montado = true;

    ServicoPerfil.buscarStatusSegundoFator({ signal: controlador.signal })
      .then((status) => {
        if (montado) aplicarStatus(Boolean(status?.enabled));
      })
      .catch((causa: unknown) => {
        const erro = observarFalha(causa);
        if (montado && !erro?.canceled) {
          aplicarStatus(Boolean(user?.twoFactorEnabled));
        }
      });

    return () => {
      montado = false;
      controlador.abort();
    };
  }, [user, user?.twoFactorEnabled, aplicarStatus]);

  useEffect(() => {
    const sincronizarAbas = (evento: StorageEvent) => {
      if (evento.key !== chaveArmazenamento || user?.twoFactorEnabled) return;
      aplicarStatus(false);
    };
    window.addEventListener("storage", sincronizarAbas);
    return () => window.removeEventListener("storage", sincronizarAbas);
  }, [aplicarStatus, chaveArmazenamento, user?.twoFactorEnabled]);

  const adiarPorSeteDias = useCallback(() => {
    salvarProximoLembrete2FA(
      localStorage,
      chaveArmazenamento,
      calcularProximoLembrete2FA(),
    );
    setVisivel(false);
  }, [chaveArmazenamento]);

  const configurarAgora = useCallback(() => {
    adiarPorSeteDias();
    onNavigate?.("/perfil?security=2fa");
  }, [adiarPorSeteDias, onNavigate]);

  if (!user || !visivel) return null;

  return (
    <aside
      className={estilos.balao}
      aria-label="Aviso de Proteção Avançada"
      aria-live="polite"
    >
      <button
        type="button"
        className={estilos.fechar}
        onClick={adiarPorSeteDias}
        aria-label={`Fechar e lembrar novamente em ${TWO_FACTOR_REMINDER_DAYS} dias`}
      >
        <X size={17} />
      </button>

      <div className={estilos.cabecalho}>
        <span className={estilos.iconePrincipal} aria-hidden="true">
          <ShieldCheck size={22} strokeWidth={2.3} />
        </span>
        <div>
          <span className={estilos.selo}>Segurança da conta</span>
          <h3>Ative a Proteção Avançada</h3>
        </div>
      </div>

      <p className={estilos.descricao}>
        Habilite o segundo fator (2FA) para adicionar uma camada de proteção
        além da sua senha.
      </p>

      <div className={estilos.beneficios} aria-label="Benefícios do 2FA">
        <div>
          <KeyRound size={16} aria-hidden="true" />
          <span>Reduz o risco de acesso apenas com a senha.</span>
        </div>
        <div>
          <Smartphone size={16} aria-hidden="true" />
          <span>Usa um código temporário gerado no seu autenticador.</span>
        </div>
        <div>
          <ShieldCheck size={16} aria-hidden="true" />
          <span>Reforça a validação de ações protegidas no sistema.</span>
        </div>
      </div>

      <div className={estilos.acoes}>
        <button
          type="button"
          className={estilos.botaoDepois}
          onClick={adiarPorSeteDias}
        >
          Lembrar em {TWO_FACTOR_REMINDER_DAYS} dias
        </button>
        <button
          type="button"
          className={estilos.botaoAtivar}
          onClick={configurarAgora}
        >
          Ativar proteção <ChevronRight size={16} aria-hidden="true" />
        </button>
      </div>
    </aside>
  );
};
