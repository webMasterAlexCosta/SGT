import type { AdministrativeReport, AdministrativeRequest } from '../../contracts/relatorios';
import { exigirIdentificadorRota, type ResourceIdentifierInput } from '../../core/http/identificadores';
import type * as DTO from '../../contracts/java';
import type { Page } from '../../contracts/shared';
import type { RequestOptions, QueryParams } from '../../core/http/types';
import { requisitar } from "../../core/http.ts";

const texto = (valor: unknown) => String(valor ?? "").trim();

export const criarChaveIdempotenciaRelatorio = () => {
  const uuid = globalThis.crypto?.randomUUID?.();
  if (uuid) return `react-${uuid}`;
  return `react-${Date.now()}-${Math.random().toString(36).slice(2, 12)}`;
};

export const normalizarRequisicaoRelatorioAdministrativo = (dados: Partial<AdministrativeRequest> = {}) => ({
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
  previa: (dados: AdministrativeRequest, options: RequestOptions = {}) => requisitar<AdministrativeReport>({
    method: "POST",
    url: "/relatorios/administrativos/previa",
    data: normalizarRequisicaoRelatorioAdministrativo(dados),
    retries: 0,
    ...options,
  }),

  emitir: (dados: AdministrativeRequest, options: RequestOptions = {}) => requisitar<AdministrativeReport>({
    method: "POST",
    url: "/relatorios/administrativos/emissoes",
    data: normalizarRequisicaoRelatorioAdministrativo(dados),
    retries: 0,
    ...options,
  }),

  obter: async (id: ResourceIdentifierInput, options: RequestOptions = {}) => { id = exigirIdentificadorRota(id); return requisitar<AdministrativeReport>({
    method: "GET",
    url: `/relatorios/administrativos/emissoes/${id}`,
    ...options,
  }); },

  conhecidaAte: (conhecidoAte: (string), dados: AdministrativeRequest, options: RequestOptions = {}) => requisitar<AdministrativeReport>({
    method: "POST",
    url: "/relatorios/administrativos/emissoes/conhecimento",
    params: { conhecidoAte },
    data: normalizarRequisicaoRelatorioAdministrativo(dados),
    retries: 0,
    ...options,
  }),

  compararComVigente: async (id: ResourceIdentifierInput, options: RequestOptions = {}) => { id = exigirIdentificadorRota(id); return requisitar<DTO.ComparacaoEmissaoRelatorioDTO>({
    method: "GET",
    url: `/relatorios/administrativos/emissoes/${id}/comparacao-vigente`,
    ...options,
  }); },

  listar: (params: QueryParams = {}, options: RequestOptions = {}) => requisitar<Page<DTO.MetadadosEmissaoRelatorioDTO>>({
    method: "GET",
    url: "/relatorios/administrativos/emissoes",
    params,
    // A tela vincula o erro ao filtro vigente; o interceptor não deve avisar
    // sobre falhas de uma consulta que o usuário já abandonou.
    silentError: true,
    ...options,
  }),
};
