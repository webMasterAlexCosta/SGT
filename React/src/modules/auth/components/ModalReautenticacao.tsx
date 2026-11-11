import { observarFalha } from '../../../core/http/falhaObservada';
import React, { useState, useEffect } from "react";
import { createPortal } from "react-dom";
import { Lock, AlertTriangle, X } from "lucide-react";
import { Botao } from "../../../core/ui/EnvolucroDica";
import { ServicoAutenticacao } from "../../../services/auth/ServicoAutenticacao";
import { CONFIG } from "../../../core/config";
import estilos from "./ModalReautenticacao.module.css";

export const ModalReautenticacao = ({
  isOpen,
  onClose,
  onConfirm,
  user,
  expiraEmEpochMs,
}: { isOpen: boolean; onClose: () => void; onConfirm: () => Promise<void>; user: import("../../../contracts/auth").AuthenticatedUser | null; expiraEmEpochMs?: number | null }) => {
  const [senhaInformada, setSenhaInformada] = useState("");
  const [erro, setErro] = useState("");
  const [carregando, setCarregando] = useState(false);
  const [restanteMs, setRestanteMs] = useState<(number) | null>(null);

  useEffect(() => {
    if (!isOpen || !expiraEmEpochMs) return;
    const atualizarRestante = () => setRestanteMs(Math.max(0, Number(expiraEmEpochMs) - Date.now()));
    atualizarRestante();
    const id = setInterval(atualizarRestante, 1000);
    return () => clearInterval(id);
  }, [isOpen, expiraEmEpochMs]);

  useEffect(() => {
    if (isOpen) {
      setSenhaInformada("");
      setErro("");
    }
  }, [isOpen]);

  if (!user && isOpen) return null;
  if (!isOpen) return null;

  const lidarComEnvio = async (evento: React.FormEvent<HTMLFormElement>) => {
    evento.preventDefault();
    if (!senhaInformada) return;

    setCarregando(true);
    setErro("");

    try {
      await ServicoAutenticacao.renovarSessao(senhaInformada);
      await onConfirm();
    } catch (errOriginal) {
      const err = observarFalha(errOriginal);

      setErro(err?.message || "Senha incorreta.");
    } finally {
      setCarregando(false);
    }
  };

  const eCritico = (restanteMs || 0) <= CONFIG.SESSION_WARN_1MIN_MS;

  return createPortal(
    <div className={estilos.sobreposicaoModal}>
      <div className={estilos.cartaoModal}>
        <button onClick={onClose} className={estilos.botaoFechar}>
          <X size={20} />
        </button>

        <div
          className={`${estilos.cabecalhoModal} ${eCritico ? estilos.fundoCritico : estilos.fundoAviso}`}
        >
          <div
            className={`${estilos.envoltorioIcone} ${eCritico ? estilos.iconeCritico : estilos.iconeAviso}`}
          >
            <AlertTriangle size={24} />
          </div>
          <h3 className={estilos.tituloCabecalho}>
            {eCritico ? "Atenção Crítica!" : "Sessão Expirando"}
          </h3>
          <p className={estilos.textoSubtitulo}>
            {eCritico
              ? "Sua sessão expira em menos de 1 minuto. Confirme sua senha agora para não perder seu trabalho!"
              : "Sua sessão irá expirar em breve. Confirme sua senha para renovar ou ignore este aviso."}
          </p>
        </div>

        <form
          onSubmit={lidarComEnvio}
          className={estilos.formularioReautenticacao}
        >
          <div className={estilos.grupoEntrada}>
            <label className={estilos.rotuloEntrada}>Confirme sua Senha</label>
            <div className={estilos.envoltorioRelativo}>
              <div className={estilos.iconeCadeado}>
                <Lock size={16} />
              </div>
              <input
                type="password"
                value={senhaInformada}
                onChange={(evento) => setSenhaInformada(evento.target.value)}
                className={estilos.campoEntrada}
                placeholder="Sua senha atual"
                autoFocus
                autoComplete="current-password"
                maxLength={72}
              />
            </div>
            {erro && <p className={estilos.textoErro}>{erro}</p>}
          </div>

          <div className={estilos.rodapeBotoes}>
            <Botao
              type="button"
              variant="secondary"
              onClick={onClose}
              disabled={carregando}
            >
              Ignorar
            </Botao>
            <Botao
              type="submit"
              disabled={carregando || !senhaInformada}
              className={
                eCritico
                  ? estilos.botaoRenovarCritico
                  : estilos.botaoRenovarAviso
              }
            >
              {carregando ? "Verificando..." : "Renovar Sessão"}
            </Botao>
          </div>
        </form>
      </div>
    </div>,
    document.body,
  );
};