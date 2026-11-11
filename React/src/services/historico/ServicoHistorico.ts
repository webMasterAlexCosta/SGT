import type { HistoricalTarget, HistoricalCommand, HistoricalPreview, HistoricalReceipt, NameProofReceipt, ReasonProofReceipt } from '../../contracts/historico';
import type * as DTO from '../../contracts/java';
import type { Page } from '../../contracts/shared';
import type { RequestOptions, QueryParams } from '../../core/http/types';
import { extrairLista, requisitar } from '../../core/http.ts';
import { serializarInstant, serializarLocalDate } from '../../core/http/datasHttp.ts';
import { idHistorico, prepararConsultaHistorica, tipoCatalogoHistorico } from './dominioHistorico.ts';

const consultar = <T,>(url: string, params: QueryParams | undefined, options: RequestOptions = {}): Promise<T> => requisitar<T>({ ...options, method: 'GET', url, ...(params ? { params } : {}), silentError: true });
const listar = async <T,>(url: string, params: QueryParams | undefined, options: RequestOptions): Promise<T[]> => extrairLista(await consultar<T[] | Page<T>>(url, params, options));
const cortes = (alvo: HistoricalTarget) => {
  const { id, ...params } = prepararConsultaHistorica(alvo);
  return { id, params };
};
const caminho = (tipo: string, alvo: HistoricalTarget) => {
  const id = idHistorico(alvo?.id);
  if (tipo === 'contrato') return `/contratos/${id}/provas-historicas`;
  if (tipo === 'catalogo') return `/catalogos-temporais/${tipoCatalogoHistorico(alvo.tipoCatalogo)}/${id}/transicoes`;
  if (tipo === 'nome') return `/pessoas/${id}/provas-nome`;
  if (tipo === 'motivo') return `/motivos-desligamento/${id}/provas-historicas`;
  throw new TypeError('Contexto histórico inválido.');
};

export const ServicoHistorico = Object.freeze({
  obterVersoesAtuais: async (tipo: string, alvo: HistoricalTarget, options: RequestOptions): Promise<import("../../contracts/historico").HistoricalVersions> => {
    const id = idHistorico(alvo.id);
    const catalogos: Record<string, string> = { LOTACAO: '/lotacoes', JORNADA: '/jornadas', CARGO: '/cargos', TIPO_CONTRATACAO: '/tipos-contratacao', MOTIVO_DESLIGAMENTO: '/motivos-desligamento' };
    let leituras: [keyof import("../../contracts/historico").HistoricalVersions, string][];
    if (tipo === 'contrato') leituras = [['versaoContratoEsperada', `/contratos/${id}`]];
    else if (tipo === 'catalogo') leituras = [['versaoEsperada', `${catalogos[tipoCatalogoHistorico(alvo.tipoCatalogo)]}/${id}`]];
    else if (tipo === 'nome') leituras = [['versaoPessoaEsperada', `/pessoas/${id}`], ['versaoVinculoEsperada', `/vinculos-contratuais/${idHistorico(alvo.vinculoId, 'ID do vínculo')}`]];
    else if (tipo === 'motivo') leituras = [['versaoMotivoEsperada', `/motivos-desligamento/${id}`], ['versaoContratoEsperada', `/contratos/${idHistorico(alvo.contratoId, 'ID do contrato')}`]];
    else throw new TypeError('Contexto histórico inválido.');
    const versoes = await Promise.all(leituras.map(async ([campo, url]) => {
      const registro = await consultar<{ version: number }>(url, undefined, options);
      if (!Number.isSafeInteger(registro?.version) || registro.version < 0) throw new TypeError('A versão de concorrência atual não foi informada pelo servidor.');
      // Somente a versão de concorrência sai desta leitura do cadastro atual.
      return [campo, registro.version] as const;
    }));
    return Object.fromEntries(versoes);
  },
  consultarContrato: async (alvo: HistoricalTarget, options: RequestOptions) => {
    const { id, params } = cortes(alvo);
    return consultar<DTO.PosicaoTemporalContratoLeituraDTO>(`/contratos/${id}/posicao-temporal`, params, options);
  },
  listarVersoes: (alvo: HistoricalTarget, options: RequestOptions) => listar<DTO.VersaoContratoLeituraDTO>(`/contratos/${idHistorico(alvo.id)}/versoes`, undefined, options),
  listarEventos: (alvo: HistoricalTarget, options: RequestOptions) => listar<DTO.EventoCicloVidaContratoDTO>(`/contratos/${idHistorico(alvo.id)}/eventos`, undefined, options),
  // API legada: conserva a leitura por D no conhecimento atual.
  listarComposicoes: (alvo: HistoricalTarget, options: RequestOptions) => listar<DTO.ComposicaoContratualLeituraDTO>(`/contratos/${idHistorico(alvo.id)}/composicoes`, { data: serializarLocalDate(alvo.data) }, options),
  // Evolução aditiva: o servidor aplica D/K e devolve os cortes na própria composição.
  listarComposicoesNoRecorte: (alvo: HistoricalTarget, options: RequestOptions) => {
    const { id, params } = cortes(alvo);
    return listar<DTO.ComposicaoContratualLeituraDTO>(`/contratos/${id}/composicoes`, params, options);
  },
  listarProvasContrato: (alvo: HistoricalTarget, options: RequestOptions) => listar<DTO.ProvaHistoricaContratoDTO>(`/contratos/${idHistorico(alvo.id)}/provas-historicas`, alvo.conhecidoAte ? { conhecidoAte: serializarInstant(alvo.conhecidoAte) } : undefined, options),
  consultarCatalogo: async (alvo: HistoricalTarget, options: RequestOptions) => {
    const tipo = tipoCatalogoHistorico(alvo.tipoCatalogo);
    const { id, params } = cortes(alvo);
    return consultar<DTO.DisponibilidadeCatalogoDTO>(`/catalogos-temporais/${tipo}/${id}/disponibilidade`, params, options);
  },
  consultarNome: async (alvo: HistoricalTarget, options: RequestOptions) => {
    const { id, params } = cortes(alvo);
    return consultar<DTO.IdentidadeHistoricaResolvidaDTO>(`/pessoas/${id}/nome-historico`, { vinculoId: idHistorico(alvo.vinculoId, 'ID do vínculo'), ...params }, options);
  },
  listarProvasNome: (alvo: HistoricalTarget, options: RequestOptions) => listar<NameProofReceipt>(`/pessoas/${idHistorico(alvo.id)}/provas-nome`, { vinculoId: idHistorico(alvo.vinculoId, 'ID do vínculo') }, options),
  consultarMotivo: async (alvo: HistoricalTarget, options: RequestOptions) => {
    const { id, params } = cortes(alvo);
    return consultar<DTO.ContextoMotivoHistoricoDTO>(`/motivos-desligamento/${id}/contexto-historico`, { contratoId: idHistorico(alvo.contratoId, 'ID do contrato'), ...params }, options);
  },
  listarProvasMotivo: (alvo: HistoricalTarget, options: RequestOptions) => listar<ReasonProofReceipt>(`/motivos-desligamento/${idHistorico(alvo.id)}/provas-historicas`, { contratoId: idHistorico(alvo.contratoId, 'ID do contrato') }, options),
  analisar: async (tipo: string, alvo: HistoricalTarget, data: HistoricalCommand, options: RequestOptions = {}) => requisitar<DTO.PreviaProvaHistoricaContratoDTO | DTO.PreviaProvaHistoricaDTO | DTO.PreviaTransicaoCatalogoDTO>({
    ...options, method: 'POST', url: `${caminho(tipo, alvo)}/previa`, data,
    retries: 0, silentError: true, csrfRetrySeguro: true,
  }),
  confirmar: async (tipo: string, alvo: HistoricalTarget, data: HistoricalCommand, options: RequestOptions = {}) => requisitar<HistoricalReceipt>({
    ...options, method: 'POST', url: caminho(tipo, alvo), data,
    retries: 0, silentError: true, mutacaoIdempotente: true,
    confirmacaoSenha: {
      acao: tipo === 'catalogo' ? 'Registrar disponibilidade documentada' : 'Incorporar prova histórica',
      entidade: `Registro ${alvo.id}`,
      consequencia: 'O servidor incorporará a intenção analisada e reavaliará os efeitos informados na prévia.',
    },
  }),
});
