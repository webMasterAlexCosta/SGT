import { requisitar } from "../../core/http.js";

const texto = (valor) => String(valor ?? "").trim();

export const criarChaveIdempotenciaRelatorio = () => {
  const uuid = globalThis.crypto?.randomUUID?.();
  if (uuid) return `react-${uuid}`;
  return `react-${Date.now()}-${Math.random().toString(36).slice(2, 12)}`;
};

export const normalizarRequisicaoRelatorioAdministrativo = (dados = {}) => ({
  tipo: texto(dados.tipo),
  formato: texto(dados.formato || "VISUALIZACAO"),
  inicio: texto(dados.inicio),
  fim: texto(dados.fim),
  contratoIds: Array.isArray(dados.contratoIds)
    ? dados.contratoIds.map(Number).filter(Number.isFinite)
    : [],
  lotacaoId: dados.lotacaoId ? Number(dados.lotacaoId) : null,
  jornadaId: dados.jornadaId ? Number(dados.jornadaId) : null,
  chaveIdempotencia: texto(dados.chaveIdempotencia) || criarChaveIdempotenciaRelatorio(),
});

export const ServicoRelatorioAdministrativo = {
  previa: (dados, options = {}) => requisitar({
    method: "POST",
    url: "/relatorios/administrativos/previa",
    data: normalizarRequisicaoRelatorioAdministrativo(dados),
    retries: 0,
    ...options,
  }),

  emitir: (dados, options = {}) => requisitar({
    method: "POST",
    url: "/relatorios/administrativos/emissoes",
    data: normalizarRequisicaoRelatorioAdministrativo(dados),
    retries: 0,
    ...options,
  }),

  obter: (id, options = {}) => requisitar({
    method: "GET",
    url: `/relatorios/administrativos/emissoes/${id}`,
    ...options,
  }),

  conhecidaAte: (conhecidoAte, dados, options = {}) => requisitar({
    method: "POST",
    url: "/relatorios/administrativos/emissoes/conhecimento",
    params: { conhecidoAte },
    data: normalizarRequisicaoRelatorioAdministrativo(dados),
    retries: 0,
    ...options,
  }),

  compararComVigente: (id, options = {}) => requisitar({
    method: "GET",
    url: `/relatorios/administrativos/emissoes/${id}/comparacao-vigente`,
    ...options,
  }),

  listar: (params = {}, options = {}) => requisitar({
    method: "GET",
    url: "/relatorios/administrativos/emissoes",
    params,
    ...options,
  }),
};
