import { useCallback, useLayoutEffect, useMemo, useRef } from 'react';
import { obterGeracaoSessao, sessaoAtual } from '../security/geracaoSessao.ts';

// Cada consumidor mantém sua própria sequência. Abort poupa transporte, enquanto
// atual() também protege transportes que resolvem depois de cancelados.
export const useConsultaVigente = (contexto: string | number = '') => {
  const estado = useRef({ sequencia: 0, controller: null as AbortController | null, montado: true });
  const invalidar = useCallback(() => {
    estado.current.sequencia += 1;
    estado.current.controller?.abort();
    estado.current.controller = null;
  }, []);
  useLayoutEffect(() => {
    estado.current.montado = true;
    invalidar();
    return () => { estado.current.montado = false; invalidar(); };
  }, [contexto, invalidar]);
  const iniciar = useCallback(() => {
    invalidar();
    const sequencia = estado.current.sequencia;
    const geracaoSessao = obterGeracaoSessao();
    const controller = new AbortController();
    estado.current.controller = controller;
    return {
      signal: controller.signal,
      atual: () => estado.current.montado && estado.current.sequencia === sequencia
        && !controller.signal.aborted && sessaoAtual(geracaoSessao),
    };
  }, [invalidar]);
  return useMemo(() => ({ iniciar, invalidar }), [iniciar, invalidar]);
};
