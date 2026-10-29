import React, { useState, useEffect, useCallback } from "react";
import { createPortal } from "react-dom"; // <-- ADICIONADO: Importação do createPortal
import { CheckCircle, XCircle, AlertTriangle, Info, X } from "lucide-react";
import { EVENT_SHOW_TOAST } from "../events.js"; 
import { CONFIG } from "../config.js"; 
import estilos from "./Aviso.module.css";

let contadorToast = 0;

const TEMPO_PADRAO = CONFIG.TOAST_DURATION_MS; 

let adicionarToastGlobal = null;
export const notificar = (type, message, duration = TEMPO_PADRAO) => {
  if (adicionarToastGlobal) {
    adicionarToastGlobal(type, message, duration);
  }
};

const Aviso = ({ id, type, message, duration, onRemove }) => {
  const [progress, setProgress] = useState(100);
  const [isClosing, setIsClosing] = useState(false);

  const aoFecharAviso = useCallback(() => {
    setIsClosing(true);
    setTimeout(() => {
      onRemove(id);
    }, 300);
  }, [id, onRemove]);

  useEffect(() => {
    const updateInterval = 10;
    const step = (100 / duration) * updateInterval;

    const timer = setInterval(() => {
      setProgress((prev) => {
        if (prev <= 0) {
          clearInterval(timer);
          aoFecharAviso();
          return 0;
        }
        return prev - step;
      });
    }, updateInterval);

    return () => clearInterval(timer);
  }, [duration, aoFecharAviso]);

  const iconeConfig = {
    success: { icon: CheckCircle, className: estilos.iconSuccess },
    error: { icon: XCircle, className: estilos.iconError },
    warning: { icon: AlertTriangle, className: estilos.iconWarning },
    info: { icon: Info, className: estilos.iconInfo },
  };

  const { icon: Icon, className: iconeClasse } =
    iconeConfig[type] || iconeConfig.info;

  return (
    <div
      className={`${estilos.toastItem} ${estilos[type]} ${isClosing ? estilos.toastClosing : ""}`}
    >
      <div className={estilos.toastContent}>
        <Icon className={`${estilos.toastIcon} ${iconeClasse}`} size={20} />
        <p className={estilos.toastMessage}>{message}</p>
        <button
          onClick={aoFecharAviso}
          className={estilos.closeButton}
          aria-label="Fechar notificação"
        >
          <X size={16} />
        </button>
      </div>
      <div className={estilos.progressBarContainer}>
        <div
          className={estilos.progressBar}
          style={{ transform: `scaleX(${progress / 100})` }}
        />
      </div>
    </div>
  );
};

export const ConteinerAvisos = () => {
  const [toasts, setToasts] = useState([]);

  useEffect(() => {
    const adicionarToast = (type, message, duration = TEMPO_PADRAO) => {
      const id = ++contadorToast;
      setToasts((prev) => [...prev, { id, type, message, duration }]);
    };

    adicionarToastGlobal = adicionarToast;

    const lidarComEventoToast = (evento) => {
      const { type, message, duration } = evento.detail;
      adicionarToast(type, message, duration);
    };

    window.addEventListener(EVENT_SHOW_TOAST, lidarComEventoToast);

    return () => {
      adicionarToastGlobal = null;
      window.removeEventListener(EVENT_SHOW_TOAST, lidarComEventoToast);
    };
  }, []);

  const removeToast = useCallback((id) => {
    setToasts((prev) => prev.filter((toast) => toast.id !== id));
  }, []);

  if (typeof document === 'undefined') return null;

  return createPortal(
    <div className={estilos.toastContainer}>
      {toasts.map((toast) => (
        <Aviso key={toast.id} {...toast} onRemove={removeToast} />
      ))}
    </div>,
    document.body
  );
};
