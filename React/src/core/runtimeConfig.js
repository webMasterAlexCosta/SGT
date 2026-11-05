const ehObjetoSimples = (valor) =>
  valor !== null && typeof valor === "object" && !Array.isArray(valor);

const numeroObrigatorioNoIntervalo = (entrada, chave, minimo, maximo) => {
  const numero = Number(entrada[chave]);
  if (!Number.isFinite(numero) || numero < minimo || numero > maximo) {
    throw new TypeError(`${chave} inválida.`);
  }
  return numero;
};

export const validarRuntimeConfig = (entrada) => {
  if (!ehObjetoSimples(entrada)) {
    throw new TypeError("Configuração do servidor inválida.");
  }

  if (typeof entrada.EM_MANUTENCAO !== "boolean") {
    throw new TypeError("EM_MANUTENCAO inválida.");
  }

  const horaInicio = String(entrada.HORA_INICIO_NOTURNO ?? "");
  if (!/^([01]\d|2[0-3]):[0-5]\d$/.test(horaInicio)) {
    throw new TypeError("HORA_INICIO_NOTURNO inválida.");
  }

  return Object.freeze({
    EM_MANUTENCAO: entrada.EM_MANUTENCAO,
    PREVISAO_RETORNO: String(entrada.PREVISAO_RETORNO ?? ""),
    CLIENT_IP: String(entrada.CLIENT_IP ?? "DESCONHECIDO"),
    HORAS_DIA_TRABALHO: numeroObrigatorioNoIntervalo(entrada, "HORAS_DIA_TRABALHO", 1, 24),
    HORA_INICIO_NOTURNO: horaInicio,
    MINUTOS_HORA_NOTURNA: numeroObrigatorioNoIntervalo(
      entrada,
      "MINUTOS_HORA_NOTURNA",
      Number.EPSILON,
      60,
    ),
  });
};

/**
 * Precedência da Etapa 02:
 * - .env/Vite: infraestrutura, identidade e preferências de UX;
 * - /config.json: parâmetros públicos dinâmicos e estado do servidor;
 * - respostas de endpoints de domínio: regras/autorizações específicas (não modeladas aqui).
 */
export const combinarConfiguracaoEfetiva = (configFrontend, runtimeConfig) => {
  if (!ehObjetoSimples(configFrontend)) {
    throw new TypeError("Configuração do frontend inválida.");
  }
  const runtime = validarRuntimeConfig(runtimeConfig);
  return Object.freeze({
    ...configFrontend,
    EM_MANUTENCAO: runtime.EM_MANUTENCAO,
    PREVISAO_RETORNO: runtime.PREVISAO_RETORNO,
    CLIENT_IP: runtime.CLIENT_IP,
    HORAS_DIA_TRABALHO: runtime.HORAS_DIA_TRABALHO,
    HORA_INICIO_NOTURNO: runtime.HORA_INICIO_NOTURNO,
    MINUTOS_HORA_NOTURNA: runtime.MINUTOS_HORA_NOTURNA,
  });
};
