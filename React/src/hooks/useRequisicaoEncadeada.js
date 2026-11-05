import { useCallback, useEffect, useRef, useState } from "react";

export const useRequisicaoEncadeada = () => {
  const controle = useRef(null);
  const sequencia = useRef(0);
  const [estado, setEstado] = useState({ carregando: false, erro: null });

  useEffect(() => () => controle.current?.abort(), []);

  const executar = useCallback(async (carregador) => {
    controle.current?.abort();
    const controlador = new AbortController();
    controle.current = controlador;
    const atual = ++sequencia.current;
    setEstado({ carregando: true, erro: null });
    try {
      const valor = await carregador({ signal: controlador.signal });
      if (atual !== sequencia.current || controlador.signal.aborted) return { ignorado: true, valor: null };
      setEstado({ carregando: false, erro: null });
      return { ignorado: false, valor };
    } catch (erro) {
      if (erro?.canceled || controlador.signal.aborted || atual !== sequencia.current) return { ignorado: true, valor: null };
      setEstado({ carregando: false, erro });
      throw erro;
    }
  }, []);

  const cancelar = useCallback(() => controle.current?.abort(), []);
  return { ...estado, executar, cancelar };
};
