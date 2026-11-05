const ausente = (valor) => valor === undefined || valor === null || String(valor).trim() === "";

const textoObrigatorio = (ambiente, chave) => {
  const valor = ambiente[chave];
  if (ausente(valor)) {
    throw new TypeError(`[CONFIG] ${chave} é obrigatória e não pode estar vazia.`);
  }
  return String(valor).trim();
};

const textoOpcional = (ambiente, chave, padrao = "") =>
  ausente(ambiente[chave]) ? padrao : String(ambiente[chave]).trim();

const numero = (ambiente, chave, { minimo, maximo, inteiro = false, permitirZero = false } = {}) => {
  const bruto = textoObrigatorio(ambiente, chave);
  const valor = Number(bruto);
  if (!Number.isFinite(valor)) {
    throw new TypeError(`[CONFIG] ${chave} deve ser numérica.`);
  }
  if (inteiro && !Number.isInteger(valor)) {
    throw new TypeError(`[CONFIG] ${chave} deve ser um número inteiro.`);
  }
  if (!permitirZero && valor === 0) {
    throw new TypeError(`[CONFIG] ${chave} não pode ser zero.`);
  }
  if (minimo !== undefined && valor < minimo) {
    throw new TypeError(`[CONFIG] ${chave} deve ser maior ou igual a ${minimo}.`);
  }
  if (maximo !== undefined && valor > maximo) {
    throw new TypeError(`[CONFIG] ${chave} deve ser menor ou igual a ${maximo}.`);
  }
  return valor;
};

const booleano = (ambiente, chave, { padrao } = {}) => {
  if (ausente(ambiente[chave])) {
    if (padrao !== undefined) return Boolean(padrao);
    throw new TypeError(`[CONFIG] ${chave} é obrigatória.`);
  }
  const valor = String(ambiente[chave]).trim().toLowerCase();
  if (valor !== "true" && valor !== "false") {
    throw new TypeError(`[CONFIG] ${chave} deve ser true ou false.`);
  }
  return valor === "true";
};

const listaInteiros = (ambiente, chave) => {
  const bruto = textoObrigatorio(ambiente, chave);
  const valores = bruto.split(",").map((item) => Number(item.trim()));
  if (
    valores.length === 0 ||
    valores.some((valor) => !Number.isInteger(valor) || valor <= 0 || valor > 1_000_000)
  ) {
    throw new TypeError(`[CONFIG] ${chave} deve conter inteiros positivos separados por vírgula.`);
  }
  return Object.freeze([...new Set(valores)]);
};

const validarApiUrl = (valor) => {
  if (valor.startsWith("/")) return valor.replace(/\/$/, "") || "/";
  let url;
  try {
    url = new URL(valor);
  } catch {
    throw new TypeError("[CONFIG] VITE_API_URL deve ser uma URL HTTP(S) ou um caminho iniciado por '/'.");
  }
  if (!['http:', 'https:'].includes(url.protocol)) {
    throw new TypeError("[CONFIG] VITE_API_URL aceita somente HTTP(S).");
  }
  return valor.replace(/\/$/, "");
};

export const CHAVES_CONFIGURACAO_FRONTEND = Object.freeze([
  "API_URL",
  "HTTP_TIMEOUT",
  "HTTP_RETRY_BASE_MS",
  "HTTP_RETRY_MAX_MS",
  "PING_INTERVAL_MS",
  "SESSION_MAX_TIME_MS",
  "INTRO_MIN_DURATION_MS",
  "APP_NAME",
  "APP_VERSION",
  "APP_COMPANY",
  "APP_CNPJ",
  "AUTHOR_ELABORADOR_1",
  "AUTHOR_ELABORADOR_2",
  "AUTHOR_NAME",
  "AUTHOR_MATRICULA",
  "EM_MANUTENCAO",
  "PREVISAO_RETORNO",
  "FREQUENCIA_MAX_GRID_ITEMS",
  "FREQUENCIA_EDIT_WARNING_MINUTES",
  "ABSENTEISMO_DEFAULT_META",
  "PAGINATION_MAX_SIZE",
  "SHOW_TEST_CREDENTIALS",
  "TEST_GLOBAL_PASSWORD",
  "TEST_GESTOR_PASSWORD",
  "PAGINATION_DEFAULT_SIZE",
  "EMPREGADOS_INITIAL_SIZE",
  "AUDIT_DEFAULT_SIZE",
  "PAGINATION_OPTIONS",
  "AUDIT_PAGINATION_OPTIONS",
  "DASHBOARD_RECENT_ACTIVITIES",
  "UPLOAD_MAX_SIZE_MB",
  "TOAST_DURATION_MS",
  "TOAST_DEDUPE_WINDOW_MS",
  "HOMOLOGACAO_EXPIRY_DAYS",
  "CONNECTION_SUCCESS_DELAY_MS",
  "OFFLINE_RETRY_INTERVAL_MS",
  "SESSION_WARN_5MIN_MS",
  "SESSION_WARN_1MIN_MS",
  "SESSION_REFRESH_INTERVAL_MS",
  "HELP_TOOLTIP_DAYS",
  "INACTIVITY_TIMEOUT_MS",
  "LATENCY_CHECK_INTERVAL_MS",
  "ABSENTEISMO_DISRUPCAO_THRESHOLD",
  "APP_ALLOWED_DOMAIN",
  "APP_CLIENT_ID",
  "SEARCH_DEBOUNCE_MS",
  "COMPANY_DEPARTMENT",
  "JORNADA_REFERENCIA_HORAS",
  "JORNADA_LIMIAR_INFERIOR",
  "JORNADA_LIMIAR_MEDIO",
]);

/**
 * Configuração de build/UX do React.
 *
 * Importante: valores de VITE_* nunca são fonte autoritativa para autorização,
 * prazo, permissão ou validação de regra de negócio. Essas decisões pertencem à API.
 */
export const criarConfiguracaoFrontend = (ambiente, { producao = false, versao = "0.0.0" } = {}) => {
  if (!ambiente || typeof ambiente !== "object" || Array.isArray(ambiente)) {
    throw new TypeError("[CONFIG] Ambiente inválido.");
  }

  const apiUrl = validarApiUrl(textoObrigatorio(ambiente, "VITE_API_URL"));
  const retryBase = numero(ambiente, "VITE_HTTP_RETRY_BASE_MS", { minimo: 0, maximo: 60_000, inteiro: true, permitirZero: true });
  const retryMax = numero(ambiente, "VITE_HTTP_RETRY_MAX_MS", { minimo: 0, maximo: 120_000, inteiro: true, permitirZero: true });
  if (retryMax < retryBase) {
    throw new TypeError("[CONFIG] VITE_HTTP_RETRY_MAX_MS não pode ser menor que VITE_HTTP_RETRY_BASE_MS.");
  }

  const mostrarCredenciaisSolicitado = booleano(ambiente, "VITE_SHOW_TEST_CREDENTIALS", { padrao: false });
  const mostrarCredenciais = !producao && mostrarCredenciaisSolicitado;

  const configuracao = {
    // Infraestrutura e resiliência.
    API_URL: apiUrl,
    HTTP_TIMEOUT: numero(ambiente, "VITE_HTTP_TIMEOUT", { minimo: 1_000, maximo: 300_000, inteiro: true }),
    HTTP_RETRY_BASE_MS: retryBase,
    HTTP_RETRY_MAX_MS: retryMax,
    PING_INTERVAL_MS: numero(ambiente, "VITE_PING_INTERVAL_MS", { minimo: 1_000, maximo: 86_400_000, inteiro: true }),
    SESSION_MAX_TIME_MS: numero(ambiente, "VITE_SESSION_MAX_TIME_MS", { minimo: 60_000, maximo: 86_400_000, inteiro: true }),

    // UX e identidade institucional.
    INTRO_MIN_DURATION_MS: numero(ambiente, "VITE_INTRO_MIN_DURATION_MS", { minimo: 0, maximo: 60_000, inteiro: true, permitirZero: true }),
    APP_NAME: textoObrigatorio(ambiente, "VITE_APP_NAME"),
    APP_VERSION: String(versao),
    APP_COMPANY: textoObrigatorio(ambiente, "VITE_APP_COMPANY"),
    APP_CNPJ: textoObrigatorio(ambiente, "VITE_APP_CNPJ"),
    AUTHOR_ELABORADOR_1: textoObrigatorio(ambiente, "VITE_APP_AUTHOR_ELABORADOR_1"),
    AUTHOR_ELABORADOR_2: textoObrigatorio(ambiente, "VITE_APP_AUTHOR_ELABORADOR_2"),
    AUTHOR_NAME: textoObrigatorio(ambiente, "VITE_APP_AUTHOR_NAME"),
    AUTHOR_MATRICULA: textoObrigatorio(ambiente, "VITE_APP_AUTHOR_MATRICULA"),

    // Somente fallback visual até /config.json responder; o backend prevalece.
    EM_MANUTENCAO: booleano(ambiente, "VITE_EM_MANUTENCAO"),
    PREVISAO_RETORNO: textoOpcional(ambiente, "VITE_PREVISAO_RETORNO", ""),

    // Limites de renderização/aviso; não autorizam nem recusam operação no backend.
    FREQUENCIA_MAX_GRID_ITEMS: numero(ambiente, "VITE_FREQUENCIA_MAX_GRID_ITEMS", { minimo: 1, maximo: 1_000_000, inteiro: true }),
    FREQUENCIA_EDIT_WARNING_MINUTES: numero(ambiente, "VITE_FREQUENCIA_EDIT_WARNING_MINUTES", { minimo: 1, maximo: 1_440, inteiro: true }),
    ABSENTEISMO_DEFAULT_META: numero(ambiente, "VITE_ABSENTEISMO_DEFAULT_META", { minimo: 0, maximo: 100, permitirZero: true }),

    // Paginação e UX local.
    PAGINATION_MAX_SIZE: numero(ambiente, "VITE_PAGINATION_MAX_SIZE", { minimo: 1, maximo: 1_000_000, inteiro: true }),
    SHOW_TEST_CREDENTIALS: mostrarCredenciais,
    TEST_GLOBAL_PASSWORD: mostrarCredenciais ? textoOpcional(ambiente, "VITE_TEST_GLOBAL_PASSWORD", "") : "",
    TEST_GESTOR_PASSWORD: mostrarCredenciais ? textoOpcional(ambiente, "VITE_TEST_GESTOR_PASSWORD", "") : "",
    PAGINATION_DEFAULT_SIZE: numero(ambiente, "VITE_PAGINATION_DEFAULT_SIZE", { minimo: 1, maximo: 1_000_000, inteiro: true }),
    EMPREGADOS_INITIAL_SIZE: numero(ambiente, "VITE_EMPREGADOS_INITIAL_SIZE", { minimo: 0, maximo: 1_000_000, inteiro: true, permitirZero: true }),
    AUDIT_DEFAULT_SIZE: numero(ambiente, "VITE_AUDIT_DEFAULT_SIZE", { minimo: 1, maximo: 1_000_000, inteiro: true }),
    PAGINATION_OPTIONS: listaInteiros(ambiente, "VITE_PAGINATION_OPTIONS"),
    AUDIT_PAGINATION_OPTIONS: listaInteiros(ambiente, "VITE_AUDIT_PAGINATION_OPTIONS"),
    DASHBOARD_RECENT_ACTIVITIES: numero(ambiente, "VITE_DASHBOARD_RECENT_ACTIVITIES", { minimo: 1, maximo: 1_000, inteiro: true }),
    UPLOAD_MAX_SIZE_MB: numero(ambiente, "VITE_UPLOAD_MAX_SIZE_MB", { minimo: 1, maximo: 1_024, inteiro: true }),
    TOAST_DURATION_MS: numero(ambiente, "VITE_TOAST_DURATION_MS", { minimo: 0, maximo: 120_000, inteiro: true, permitirZero: true }),
    TOAST_DEDUPE_WINDOW_MS: numero(ambiente, "VITE_TOAST_DEDUPE_WINDOW_MS", { minimo: 0, maximo: 120_000, inteiro: true, permitirZero: true }),
    HOMOLOGACAO_EXPIRY_DAYS: numero(ambiente, "VITE_HOMOLOGACAO_EXPIRY_DAYS", { minimo: 1, maximo: 365, inteiro: true }),
    CONNECTION_SUCCESS_DELAY_MS: numero(ambiente, "VITE_CONNECTION_SUCCESS_DELAY_MS", { minimo: 0, maximo: 60_000, inteiro: true, permitirZero: true }),
    OFFLINE_RETRY_INTERVAL_MS: numero(ambiente, "VITE_OFFLINE_RETRY_INTERVAL_MS", { minimo: 1_000, maximo: 3_600_000, inteiro: true }),
    SESSION_WARN_5MIN_MS: numero(ambiente, "VITE_SESSION_WARN_5MIN_MS", { minimo: 1_000, maximo: 3_600_000, inteiro: true }),
    SESSION_WARN_1MIN_MS: numero(ambiente, "VITE_SESSION_WARN_1MIN_MS", { minimo: 1_000, maximo: 3_600_000, inteiro: true }),
    SESSION_REFRESH_INTERVAL_MS: numero(ambiente, "VITE_SESSION_REFRESH_INTERVAL_MS", { minimo: 1_000, maximo: 3_600_000, inteiro: true }),
    HELP_TOOLTIP_DAYS: numero(ambiente, "VITE_HELP_TOOLTIP_DAYS", { minimo: 1, maximo: 3_650, inteiro: true }),
    INACTIVITY_TIMEOUT_MS: numero(ambiente, "VITE_INACTIVITY_TIMEOUT_MS", { minimo: 60_000, maximo: 86_400_000, inteiro: true }),
    LATENCY_CHECK_INTERVAL_MS: numero(ambiente, "VITE_LATENCY_CHECK_INTERVAL_MS", { minimo: 1_000, maximo: 3_600_000, inteiro: true }),
    ABSENTEISMO_DISRUPCAO_THRESHOLD: numero(ambiente, "VITE_ABSENTEISMO_DISRUPCAO_THRESHOLD", { minimo: 0, maximo: 100, permitirZero: true }),
    APP_ALLOWED_DOMAIN: textoObrigatorio(ambiente, "VITE_APP_ALLOWED_DOMAIN"),
    APP_CLIENT_ID: textoObrigatorio(ambiente, "VITE_APP_CLIENT_ID"),
    SEARCH_DEBOUNCE_MS: numero(ambiente, "VITE_SEARCH_DEBOUNCE_MS", { minimo: 0, maximo: 10_000, inteiro: true, permitirZero: true }),
    COMPANY_DEPARTMENT: textoObrigatorio(ambiente, "VITE_COMPANY_DEPARTMENT"),

    // Referências exclusivamente visuais para classificação/filtros. A API valida a jornada.
    JORNADA_REFERENCIA_HORAS: numero(ambiente, "VITE_JORNADA_REFERENCIA_HORAS", { minimo: 1, maximo: 168 }),
    JORNADA_LIMIAR_INFERIOR: numero(ambiente, "VITE_JORNADA_LIMIAR_INFERIOR", { minimo: 0, maximo: 168, permitirZero: true }),
    JORNADA_LIMIAR_MEDIO: numero(ambiente, "VITE_JORNADA_LIMIAR_MEDIO", { minimo: 0, maximo: 168, permitirZero: true }),
  };

  if (configuracao.PAGINATION_DEFAULT_SIZE > configuracao.PAGINATION_MAX_SIZE) {
    throw new TypeError("[CONFIG] VITE_PAGINATION_DEFAULT_SIZE não pode exceder VITE_PAGINATION_MAX_SIZE.");
  }
  if (configuracao.SESSION_WARN_5MIN_MS >= configuracao.SESSION_MAX_TIME_MS) {
    throw new TypeError("[CONFIG] VITE_SESSION_WARN_5MIN_MS deve ser menor que VITE_SESSION_MAX_TIME_MS.");
  }
  if (configuracao.SESSION_WARN_1MIN_MS >= configuracao.SESSION_WARN_5MIN_MS) {
    throw new TypeError("[CONFIG] VITE_SESSION_WARN_1MIN_MS deve ser menor que VITE_SESSION_WARN_5MIN_MS.");
  }

  return Object.freeze(configuracao);
};
