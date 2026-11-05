import { extrairLista, requisitar } from '../../core/http.js';
import { serializarInstant, serializarLocalDate } from '../../core/http/datasHttp.js';
import { idHistorico, prepararConsultaHistorica, tipoCatalogoHistorico } from './dominioHistorico.js';

const consultar = (url, params, options = {}) => requisitar({ ...options, method: 'GET', url, ...(params ? { params } : {}), silentError: true });
const listar = async (url, params, options) => extrairLista(await consultar(url, params, options));
const cortes = (alvo) => {
  const { id, ...params } = prepararConsultaHistorica(alvo);
  return { id, params };
};
const caminho = (tipo, alvo) => {
  const id = idHistorico(alvo?.id);
  if (tipo === 'contrato') return `/contratos/${id}/provas-historicas`;
  if (tipo === 'catalogo') return `/catalogos-temporais/${tipoCatalogoHistorico(alvo.tipoCatalogo)}/${id}/transicoes`;
  if (tipo === 'nome') return `/pessoas/${id}/provas-nome`;
  if (tipo === 'motivo') return `/motivos-desligamento/${id}/provas-historicas`;
  throw new TypeError('Contexto histórico inválido.');
};

export const ServicoHistorico = Object.freeze({
  obterVersoesAtuais: async (tipo, alvo, options) => {
    const id = idHistorico(alvo.id);
    const catalogos = { LOTACAO: '/lotacoes', JORNADA: '/jornadas', CARGO: '/cargos', TIPO_CONTRATACAO: '/tipos-contratacao', MOTIVO_DESLIGAMENTO: '/motivos-desligamento' };
    let leituras;
    if (tipo === 'contrato') leituras = [['versaoContratoEsperada', `/contratos/${id}`]];
    else if (tipo === 'catalogo') leituras = [['versaoEsperada', `${catalogos[tipoCatalogoHistorico(alvo.tipoCatalogo)]}/${id}`]];
    else if (tipo === 'nome') leituras = [['versaoPessoaEsperada', `/pessoas/${id}`], ['versaoVinculoEsperada', `/vinculos-contratuais/${idHistorico(alvo.vinculoId, 'ID do vínculo')}`]];
    else if (tipo === 'motivo') leituras = [['versaoMotivoEsperada', `/motivos-desligamento/${id}`], ['versaoContratoEsperada', `/contratos/${idHistorico(alvo.contratoId, 'ID do contrato')}`]];
    else throw new TypeError('Contexto histórico inválido.');
    const versoes = await Promise.all(leituras.map(async ([campo, url]) => {
      const registro = await consultar(url, undefined, options);
      if (!Number.isSafeInteger(registro?.version) || registro.version < 0) throw new TypeError('A versão de concorrência atual não foi informada pelo servidor.');
      // Somente a versão de concorrência sai desta leitura do cadastro atual.
      return [campo, registro.version];
    }));
    return Object.fromEntries(versoes);
  },
  consultarContrato: async (alvo, options) => {
    const { id, params } = cortes(alvo);
    return consultar(`/contratos/${id}/posicao-temporal`, params, options);
  },
  listarVersoes: (alvo, options) => listar(`/contratos/${idHistorico(alvo.id)}/versoes`, undefined, options),
  listarEventos: (alvo, options) => listar(`/contratos/${idHistorico(alvo.id)}/eventos`, undefined, options),
  // API legada: conserva a leitura por D no conhecimento atual.
  listarComposicoes: (alvo, options) => listar(`/contratos/${idHistorico(alvo.id)}/composicoes`, { data: serializarLocalDate(alvo.data) }, options),
  // Evolução aditiva: o servidor aplica D/K e devolve os cortes na própria composição.
  listarComposicoesNoRecorte: (alvo, options) => {
    const { id, params } = cortes(alvo);
    return listar(`/contratos/${id}/composicoes`, params, options);
  },
  listarProvasContrato: (alvo, options) => listar(`/contratos/${idHistorico(alvo.id)}/provas-historicas`, alvo.conhecidoAte ? { conhecidoAte: serializarInstant(alvo.conhecidoAte) } : undefined, options),
  consultarCatalogo: async (alvo, options) => {
    const tipo = tipoCatalogoHistorico(alvo.tipoCatalogo);
    const { id, params } = cortes(alvo);
    return consultar(`/catalogos-temporais/${tipo}/${id}/disponibilidade`, params, options);
  },
  consultarNome: async (alvo, options) => {
    const { id, params } = cortes(alvo);
    return consultar(`/pessoas/${id}/nome-historico`, { vinculoId: idHistorico(alvo.vinculoId, 'ID do vínculo'), ...params }, options);
  },
  listarProvasNome: (alvo, options) => listar(`/pessoas/${idHistorico(alvo.id)}/provas-nome`, { vinculoId: idHistorico(alvo.vinculoId, 'ID do vínculo') }, options),
  consultarMotivo: async (alvo, options) => {
    const { id, params } = cortes(alvo);
    return consultar(`/motivos-desligamento/${id}/contexto-historico`, { contratoId: idHistorico(alvo.contratoId, 'ID do contrato'), ...params }, options);
  },
  listarProvasMotivo: (alvo, options) => listar(`/motivos-desligamento/${idHistorico(alvo.id)}/provas-historicas`, { contratoId: idHistorico(alvo.contratoId, 'ID do contrato') }, options),
  analisar: async (tipo, alvo, data, options = {}) => requisitar({
    ...options, method: 'POST', url: `${caminho(tipo, alvo)}/previa`, data,
    retries: 0, silentError: true, csrfRetrySeguro: true,
  }),
  confirmar: async (tipo, alvo, data, options = {}) => requisitar({
    ...options, method: 'POST', url: caminho(tipo, alvo), data,
    retries: 0, silentError: true, mutacaoIdempotente: true,
    confirmacaoSenha: {
      acao: tipo === 'catalogo' ? 'Registrar disponibilidade documentada' : 'Incorporar prova histórica',
      entidade: `Registro ${alvo.id}`,
      consequencia: 'O servidor incorporará a intenção analisada e reavaliará os efeitos informados na prévia.',
    },
  }),
});
