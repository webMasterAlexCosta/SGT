import React, { useState, useEffect } from "react";
import { WifiOff, Loader2 } from "lucide-react";
import estilos from "./ModalConexao.module.css";
import { CONFIG } from "./config";

export const ModalConexao = () => {
  const [isOffline, setIsOffline] = useState(!navigator.onLine);
  const [isChecking, setIsChecking] = useState(false);
  const [progress, setProgress] = useState(100);

  useEffect(() => {
    const aoRestaurarConexao = () => {
      setIsChecking(true);
      setTimeout(() => {
        setIsChecking(false);
        setIsOffline(false);
        setProgress(100);
      }, CONFIG.CONNECTION_SUCCESS_DELAY_MS);
    };

    const aoPerderConexao = () => {
      setIsOffline(true);
      setProgress(100);
    };

    window.addEventListener("online", aoRestaurarConexao);
    window.addEventListener("offline", aoPerderConexao);

    return () => {
      window.removeEventListener("online", aoRestaurarConexao);
      window.removeEventListener("offline", aoPerderConexao);
    };
  }, []);

  useEffect(() => {
    let timer: ReturnType<typeof setTimeout> | undefined;
    if (isOffline && !isChecking) {
      const interval = 100;
      const step = (interval / CONFIG.OFFLINE_RETRY_INTERVAL_MS) * 100;

      timer = setInterval(() => {
        setProgress((prev) => {
          if (prev <= 0) {
            window.dispatchEvent(new Event("online"));
            return 100;
          }
          return prev - step;
        });
      }, interval);
    }
    return () => clearInterval(timer);
  }, [isOffline, isChecking]);

  if (!isOffline && !isChecking) return null;

  return (
    <div className={estilos.modalOverlay}>
      <div className={estilos.modalContent}>
        <div className={estilos.iconeContainer}>
          {isChecking ? (
            <Loader2
              className={`${estilos.icone} ${estilos.iconeGirando}`}
              size={48}
            />
          ) : (
            <WifiOff
              className={`${estilos.icone} ${estilos.iconePulsando}`}
              size={48}
            />
          )}
        </div>
        <h2 className={estilos.titulo}>
          {isChecking
            ? "Restabelecendo Conexão..."
            : "Sem Conexão com a Intranet"}
        </h2>
        <p className={estilos.mensagem}>
          {isChecking
            ? "Aguarde enquanto verificamos o status da rede."
            : "O sistema perdeu a comunicação com o servidor. Verifique a sua rede."}
        </p>

        {!isChecking && (
          <div className={estilos.progressoContainer}>
            <div className={estilos.textoProgresso}>
              <span>A tentar novamente em instantes</span>
              <span>
                {Math.ceil(
                  (progress / 100) * (CONFIG.OFFLINE_RETRY_INTERVAL_MS / 1000),
                )}
                s
              </span>
            </div>
            <div className={estilos.barraFundo}>
              {/* OTIMIZAÇÃO GPU: transform scaleX em vez de width */}
              <div
                className={estilos.barraProgresso}
                style={{ transform: `scaleX(${progress / 100})` }}
              />
            </div>
          </div>
        )}
      </div>
    </div>
  );
};
