export const MODOS_VISUALIZACAO = Object.freeze({
  TABELA: "tabela",
  LISTA: "lista",
  GRADE: "grade",
});

export const CHAVE_MODO_VISUALIZACAO = "sgt.empregados.modoVisualizacao";

const MODOS_VALIDOS = new Set(Object.values(MODOS_VISUALIZACAO));

export const normalizarModoVisualizacao = (modo) =>
  MODOS_VALIDOS.has(modo) ? modo : MODOS_VISUALIZACAO.GRADE;

export const lerModoVisualizacao = (armazenamento) => {
  try {
    return normalizarModoVisualizacao(
      armazenamento?.getItem?.(CHAVE_MODO_VISUALIZACAO),
    );
  } catch {
    return MODOS_VISUALIZACAO.GRADE;
  }
};

export const salvarModoVisualizacao = (armazenamento, modo) => {
  const modoNormalizado = normalizarModoVisualizacao(modo);
  try {
    armazenamento?.setItem?.(CHAVE_MODO_VISUALIZACAO, modoNormalizado);
  } catch {
    // A preferência visual não pode impedir o funcionamento da tela quando o
    // navegador bloqueia o armazenamento local.
  }
  return modoNormalizado;
};

export const obterModoVisualizacaoInicial = () =>
  typeof window === "undefined"
    ? MODOS_VISUALIZACAO.GRADE
    : lerModoVisualizacao(window.localStorage);
