import { useEffect, useState } from "react";

export const useConsultaMidia = (consulta: (string)) => {
  const [corresponde, definirCorresponde] = useState(() => {
    if (typeof window === "undefined" || typeof window.matchMedia !== "function") return false;
    return window.matchMedia(consulta).matches;
  });

  useEffect(() => {
    if (typeof window === "undefined" || typeof window.matchMedia !== "function") return undefined;
    const lista = window.matchMedia(consulta);
    const atualizar = (evento: MediaQueryListEvent) => definirCorresponde(evento.matches);
    definirCorresponde(lista.matches);
    lista.addEventListener?.("change", atualizar);
    return () => lista.removeEventListener?.("change", atualizar);
  }, [consulta]);

  return corresponde;
};
