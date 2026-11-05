import React, { useCallback, useEffect, useRef, useState } from "react";
import { Loader2, LockKeyhole, QrCode, ShieldCheck, ShieldX } from "lucide-react";
import { ServicoPerfil } from "../../../../services/perfil/ServicoPerfil";
import { notificar } from "../../../../core/events";
import { CodigoAutenticador } from "./components/CodigoAutenticador";
import { ConfiguracaoCodigoQr } from "./components/ConfiguracaoCodigoQr";
import { StatusSegundoFator } from "./components/StatusSegundoFator";
import estilos from "./index.module.css";

export const GerenciadorSegundoFator = ({ onStatusChange }) => {
  const [status, setStatus] = useState({ enabled: false, setupPending: false });
  const [password, setPassword] = useState("");
  const [token, setToken] = useState("");
  const [setup, setSetup] = useState(null);
  const [carregando, setCarregando] = useState(true);
  const onStatusChangeAtual = useRef(onStatusChange);

  useEffect(() => { onStatusChangeAtual.current = onStatusChange; }, [onStatusChange]);

  const atualizarStatus = useCallback((proximo) => {
    const normalizado = {
      enabled: Boolean(proximo?.enabled),
      setupPending: Boolean(proximo?.setupPending),
    };
    setStatus(normalizado);
    onStatusChangeAtual.current?.(normalizado.enabled);
    return normalizado;
  }, []);

  const recarregarStatus = useCallback(async (options = {}) => {
    const remoto = await ServicoPerfil.buscarStatusSegundoFator(options);
    return atualizarStatus(remoto);
  }, [atualizarStatus]);

  useEffect(() => {
    const controlador = new AbortController();
    recarregarStatus({ signal: controlador.signal })
      .catch((erro) => {
        if (!erro?.canceled) notificar("error", "Não foi possível consultar o segundo fator.");
      })
      .finally(() => setCarregando(false));
    return () => controlador.abort();
  }, [recarregarStatus]);

  const iniciarConfiguracao = async () => {
    if (!password) {
      notificar("warning", "Informe sua senha atual para gerar o QR Code.");
      return;
    }
    setCarregando(true);
    try {
      const resposta = await ServicoPerfil.configurarSegundoFator(password);
      if (!resposta?.secret || !resposta?.otpauthUri || !resposta?.qrCodeDataUrl) {
        throw new Error("O servidor não retornou os dados completos do QR Code.");
      }
      setSetup(resposta);
      setToken("");
      await recarregarStatus({ silentError: false });
      notificar("info", "Escaneie o QR Code e confirme o código gerado no celular.");
    } catch (erro) {
      if (erro?.message && !erro?.status) notificar("error", erro.message);
    } finally {
      setCarregando(false);
    }
  };

  const ativar = async () => {
    if (!/^\d{6}$/.test(token)) {
      notificar("warning", "Informe um código válido de 6 dígitos.");
      return;
    }
    setCarregando(true);
    try {
      const confirmado = await ServicoPerfil.ativarSegundoFator(token);
      atualizarStatus(confirmado);
      setPassword("");
      setToken("");
      setSetup(null);
      notificar("success", "Segundo fator ativado. Nos próximos logins o código do autenticador será obrigatório.");
      await recarregarStatus({ silentError: false });
    } catch (erro) {
      if (!erro?.canceled && !erro?.status) notificar("error", erro?.message || "Não foi possível ativar o segundo fator.");
    } finally {
      setCarregando(false);
    }
  };

  const desativar = async () => {
    if (!password || !/^\d{6}$/.test(token)) {
      notificar("warning", "Informe a senha atual e o código de 6 dígitos.");
      return;
    }
    setCarregando(true);
    try {
      const confirmado = await ServicoPerfil.desativarSegundoFator(password, token);
      atualizarStatus(confirmado);
      setPassword("");
      setToken("");
      setSetup(null);
      notificar("success", "Segundo fator desativado. As ações protegidas voltam a usar a confirmação por senha.");
      await recarregarStatus({ silentError: false });
    } catch (erro) {
      if (!erro?.canceled && !erro?.status) notificar("error", erro?.message || "Não foi possível desativar o segundo fator.");
    } finally {
      setCarregando(false);
    }
  };

  if (carregando && !setup && !status.enabled) {
    return (
      <div className={estilos.carregando} aria-live="polite">
        <Loader2 size={20} /> Carregando configurações de segurança...
      </div>
    );
  }

  return (
    <div className={estilos.container}>
      <StatusSegundoFator
        ativo={status.enabled}
        configuracaoPendente={status.setupPending && !setup}
      />

      {!status.enabled && !setup && (
        <div className={estilos.painelAcao}>
          <div className={estilos.textoAcao}>
            <QrCode size={20} />
            <div>
              <strong>Configurar aplicativo autenticador</strong>
              <p>É opcional. A ativação só acontece depois que você confirmar um código válido.</p>
            </div>
          </div>
          <div className={estilos.linhaSenha}>
            <label htmlFor="senha-configurar-2fa">Senha atual</label>
            <div>
              <LockKeyhole size={17} />
              <input
                id="senha-configurar-2fa"
                type="password"
                value={password}
                onChange={(evento) => setPassword(evento.target.value)}
                placeholder="Confirme sua senha"
                maxLength={72}
                autoComplete="current-password"
              />
            </div>
          </div>
          <button
            type="button"
            className={estilos.botaoPrimario}
            onClick={iniciarConfiguracao}
            disabled={carregando}
          >
            <QrCode size={16} />
            {status.setupPending ? "Gerar novo QR Code" : "Gerar QR Code"}
          </button>
        </div>
      )}

      {!status.enabled && setup && (
        <div className={estilos.fluxoAtivacao}>
          <ConfiguracaoCodigoQr
            qrCodeDataUrl={setup.qrCodeDataUrl}
            secret={setup.secret}
            otpauthUri={setup.otpauthUri}
          />
          <div className={estilos.confirmacaoAtivacao}>
            <CodigoAutenticador valor={token} aoMudar={setToken} desabilitado={carregando} />
            <div className={estilos.botoesAtivacao}>
              <button type="button" className={estilos.botaoSecundario} onClick={iniciarConfiguracao} disabled={carregando}>
                Gerar outro QR
              </button>
              <button type="button" className={estilos.botaoPrimario} onClick={ativar} disabled={carregando}>
                <ShieldCheck size={16} /> Confirmar e ativar
              </button>
            </div>
          </div>
        </div>
      )}

      {status.enabled && (
        <div className={estilos.painelDesativacao}>
          <div className={estilos.avisoDesativacao}>
            <ShieldX size={19} />
            <div>
              <strong>Desativar segundo fator</strong>
              <p>Confirme a senha e um código atual do aplicativo. Esta ação é opcional e reversível.</p>
            </div>
          </div>
          <div className={estilos.gradeDesativacao}>
            <div className={estilos.linhaSenha}>
              <label htmlFor="senha-desativar-2fa">Senha atual</label>
              <div>
                <LockKeyhole size={17} />
                <input
                  id="senha-desativar-2fa"
                  type="password"
                  value={password}
                  onChange={(evento) => setPassword(evento.target.value)}
                  placeholder="Sua senha"
                  maxLength={72}
                  autoComplete="current-password"
                />
              </div>
            </div>
            <CodigoAutenticador
              id="codigo-desativar-2fa"
              valor={token}
              aoMudar={setToken}
              desabilitado={carregando}
            />
          </div>
          <button type="button" className={estilos.botaoPerigo} onClick={desativar} disabled={carregando}>
            <ShieldX size={16} /> Desativar segundo fator
          </button>
        </div>
      )}
    </div>
  );
};
