import type * as DTO from '../../contracts/java';
import type { ProofReading } from '../../services/historico/provaHistoricaLeitura';
type HistoricalFormatter = (value: unknown, present?: boolean) => React.ReactNode;
interface HistoricalColumn { chave: string; titulo: string; renderizar?: (value: unknown, item: object) => React.ReactNode }
import React, { useEffect, useState } from 'react';
import { ChevronLeft, ChevronRight, FileClock, HelpCircle, ShieldCheck } from 'lucide-react';
import { CAMPOS_FOTOGRAFIA, CATEGORIAS_MOTIVO, DIAS_SEMANA, TIPOS_CATALOGO } from '../../services/historico/dominioHistorico.ts';
import { camposMetadadosProva, formatarInstanteFonte, lerProvaCampo, possuiMetadadosProva } from '../../services/historico/provaHistoricaLeitura.ts';
import estilos from './historico.module.css';

const ROTULOS: Readonly<Record<string, string | undefined>> = {
  ...Object.fromEntries(CAMPOS_FOTOGRAFIA.map(({ nome, rotulo }) => [nome, rotulo])),
  id: 'Identificador', contratoId: 'Contrato', pessoaId: 'Pessoa', vinculoId: 'Vínculo', catalogoId: 'Item do catálogo',
  tipoCatalogo: 'Catálogo', motivoDesligamentoId: 'Motivo de término', nome: 'Nome documentado', nomeComprovado: 'Nome comprovado',
  descricao: 'Descrição', descricaoComprovada: 'Descrição comprovada', categoria: 'Categoria', categoriaComprovada: 'Categoria comprovada',
  dataReferencia: 'Data efetiva consultada', conhecidoAte: 'Conhecido até', estadoNegocial: 'Estado negocial', situacaoTemporal: 'Situação temporal',
  vigenciaInicio: 'Início de vigência no recorte', vigenciaFim: 'Fim de vigência no recorte', versaoContratual: 'Fotografia contratual do recorte',
  eventosAplicados: 'Eventos aplicados pelo servidor', relacoesConsideradas: 'Precedências consideradas',
  numeroVersao: 'Número da fotografia', eventoOrigemId: 'Evento de origem', versaoAnteriorId: 'Fotografia anterior', versaoGeradaId: 'Fotografia gerada',
  provaAnteriorId: 'Prova anterior', numeroRevisao: 'Revisão documental', configuracaoCompletaComprovada: 'Configuração integral comprovada',
  registradoEm: 'Registro do conhecimento', hashConteudo: 'Hash do conteúdo', eventoId: 'Evento registrado',
  ativo: 'Indicador de atividade retornado', ativoAnterior: 'Estado anterior informado', ativoAtualAposRegistro: 'Estado atual após o registro',
  disponibilidadeComprovada: 'Disponibilidade comprovada', eventoFonteId: 'Evento fonte', dataEfetivaFonte: 'Data efetiva da fonte', registradoEmFonte: 'Registro da fonte',
  comprovado: 'Comprovação histórica', tipoFonte: 'Tipo da fonte', fonteId: 'Identificador da fonte', fonteRegistradaEm: 'Registro da fonte',
  inicioComprovado: 'Início comprovado', fimComprovado: 'Fim comprovado', dataInicioEfetiva: 'Início do intervalo', dataFimEfetiva: 'Fim do intervalo',
  motivo: 'Motivo / justificativa', autorizacao: 'Autorização', referenciaSei: 'Referência SEI', documentoComprobatorioId: 'Documento comprobatório',
  responsavelId: 'Responsável (ID)', responsavelUsername: 'Responsável', eventoOrigemRetificadoId: 'Evento nominal retificado', relacao: 'Relação na cadeia',
  reenvio: 'Reenvio da mesma intenção', versaoAtual: 'Versão atual de concorrência', versaoContrato: 'Versão do contrato', tokenPrevia: 'Código da prévia',
  versaoContratoEsperada: 'Versão atual do contrato', versaoPessoaEsperada: 'Versão atual da pessoa', versaoVinculoEsperada: 'Versão atual do vínculo', versaoMotivoEsperada: 'Versão atual do motivo', versaoEsperada: 'Versão atual do catálogo',
  recuperaPublicacao: 'Recupera publicação original', composicoesComprovadas: 'Configurações comprovadas', efeito: 'Efeito da incorporação', impactos: 'Impactos previstos',
  tipoEvento: 'Tipo do evento', fatoLogicoId: 'Fato lógico', chaveNegocial: 'Chave negocial', versaoNegocial: 'Versão negocial', ordemDocumental: 'Ordem documental',
  situacaoFato: 'Situação do fato', dataAto: 'Data do ato', usuarioResponsavelId: 'Responsável pelo evento (ID)', usuarioResponsavelUsername: 'Responsável pelo evento',
  valorAnterior: 'Valor anterior preservado', valorNovo: 'Valor novo preservado', eventoAlvoId: 'Evento alvo', tipoRelacao: 'Tipo de precedência',
  lotacaoId: 'Lotação (ID)', lotacaoNome: 'Nome da lotação retornado', jornadaId: 'Jornada (ID)', jornadaDescricao: 'Jornada retornada', cargoId: 'Cargo (ID)', cargoNome: 'Cargo retornado',
  dataInicioVigencia: 'Início da composição', dataFimVigencia: 'Fim da composição', permiteNovaAlocacao: 'Permite nova alocação', version: 'Versão de concorrência',
  tipoContratacaoComprovado: 'Tipo de contratação comprovado', origemFotografia: 'Origem da fotografia',
  lotacaoMunicipio: 'Município documentado', jornadaSemanal: 'Grade semanal documentada',
  camposComprovados: 'Campos declarados nesta revisão', fotografia: 'Fotografia cumulativa até esta revisão',
  composicoes: 'Configurações documentadas', composicaoId: 'Composição de origem',
  aditamentoComposicaoId: 'Item de aditamento', mapeamentoId: 'Mapeamento de transferência',
  municipioComprovado: 'Declaração de comprovação municipal', jornadaComprovada: 'Declaração de comprovação da grade',
};
const ENUMS: Record<string, string> = {
  ...Object.fromEntries([...CATEGORIAS_MOTIVO, ...DIAS_SEMANA, ...TIPOS_CATALOGO]),
  AUSENTE: 'Fonte histórica não localizada', PUBLICACAO: 'Publicação', ADITAMENTO: 'Aditamento', REMANEJAMENTO: 'Remanejamento',
  TRANSFERENCIA_GESTAO_LOCAL: 'Transferência de gestão local', INATIVACAO_ADMINISTRATIVA: 'Inativação administrativa', REATIVACAO_ADMINISTRATIVA: 'Reativação administrativa',
  ENCERRAMENTO_ANTECIPADO: 'Encerramento antecipado', CANCELAMENTO_CONTRATO_FUTURO: 'Cancelamento de contrato futuro', ANULACAO: 'Anulação',
  CORRECAO_ANULACAO: 'Correção de anulação', CORRECAO_IDENTIFICADOR: 'Correção de identificador', CORRECAO_CNPJ: 'Correção de CNPJ',
  CANCELAMENTO_TRANSFERENCIA_GESTAO: 'Cancelamento de transferência', CANCELAMENTO_ADITAMENTO: 'Cancelamento de aditamento', CANCELAMENTO_REMANEJAMENTO: 'Cancelamento de remanejamento',
  PROVA_NOME_HISTORICO: 'Prova de nome histórico', PROVA_MOTIVO_HISTORICO: 'Prova histórica do motivo', CADASTRO_CONTEMPORANEO: 'Cadastro contemporâneo ao fato',
  ORIGEM: 'Origem', RETIFICA: 'Retificação documental', ATIVO: 'Ativo', INATIVO: 'Inativo', RASCUNHO: 'Rascunho', PUBLICADO: 'Publicado',
};
const humanizar = (valor: (string)) => String(valor).replace(/([a-z])([A-Z])/g, '$1 $2').replace(/_/g, ' ').toLowerCase().replace(/^./, (letra) => letra.toUpperCase());
export const formatarDadoHistorico = (valor: unknown): string => {
  if (valor == null || valor === '') return 'Não informado pelo servidor';
  if (typeof valor === 'boolean') return valor ? 'Sim' : 'Não';
  if (typeof valor === 'number') return String(valor);
  if (typeof valor !== 'string') return String(valor);
  if (ENUMS[valor]) return ENUMS[valor];
  if (/^\d{4}-\d{2}-\d{2}$/.test(valor)) return valor.split('-').reverse().join('/');
  if (/^\d{4}-\d{2}-\d{2}T/.test(valor) && Number.isFinite(Date.parse(valor))) {
    return `${new Date(valor).toLocaleString('pt-BR', { timeZone: 'UTC', hourCycle: 'h23' })} UTC`;
  }
  return String(valor);
};
const EscalarHistorico = ({ valor, formatarValor = formatarDadoHistorico, presente = true }: { valor: unknown; formatarValor?: HistoricalFormatter; presente?: boolean }) => {
  const exibido = formatarValor(valor, presente);
  return typeof valor === 'string' && /^\d{4}-\d{2}-\d{2}T/.test(valor) && /(?:Z|[+-]\d{2}:\d{2})$/i.test(valor) && Number.isFinite(Date.parse(valor))
    ? <time dateTime={valor}>{exibido}{' '}<small className={estilos.instanteExato}>{valor}</small></time> : exibido;
};
const TIPOS_ORIGEM: Readonly<Record<string, string | undefined>> = {
  FOTOGRAFIA_CONTRATUAL: 'Fotografia contratual', PROVA_HISTORICA: 'Prova histórica', EVENTO_CONTRATUAL: 'Evento contratual',
  COMPOSICAO_PUBLICADA: 'Composição publicada', ITEM_ADITAMENTO: 'Item de aditamento', MAPEAMENTO_TRANSFERENCIA: 'Mapeamento de transferência',
};
const IDS_ORIGEM = { provaId: 'Prova', eventoId: 'Evento', versaoId: 'Fotografia', composicaoId: 'Composição',
  aditamentoComposicaoId: 'Item de aditamento', mapeamentoId: 'Mapeamento' };
export const ValorComProva = ({ prova, compacto = false }: { prova: ProofReading; compacto?: boolean }) => <div className={estilos.provaCampo}>
  <span className={`${estilos.estadoProva} ${prova.tipo === 'RESTRITA' ? estilos.provaRestrita : ''}`}>{prova.rotulo}{' '}</span>
  {prova.visibilidade === 'DISPONIVEL' && <small>Visibilidade: disponível{' '}</small>}
  {prova.tipo === 'VALOR_COMPROVADO' && <span className={estilos.valorProva}>{prova.valor === '' ? 'Texto vazio documentado' : formatarDadoHistorico(prova.valor)}{' '}</span>}
  {!compacto && prova.origens.length > 0 && <ul className={estilos.origensProva} aria-label="Origens autorizadas do campo">
    {prova.origens.map((origem, indice) => <li key={indice}>
      <strong>{TIPOS_ORIGEM[String(origem.tipo)]}{' '}</strong>
      <span>{Object.entries(IDS_ORIGEM).filter(([chave]) => Reflect.get(origem, chave) != null).map(([chave, rotulo]) => `${rotulo} #${Reflect.get(origem, chave)}`).join(' · ')}{' '}</span>
      <span>Data efetiva da fonte: {origem.dataEfetiva ? formatarDadoHistorico(origem.dataEfetiva) : 'Não informada'}{' '}</span>
      {origem.registradoEm ? <><span>Registro da fonte: {formatarInstanteFonte(origem.registradoEm)}{' '}</span><time dateTime={origem.registradoEm}>{origem.registradoEm}{' '}</time></> : <span>Registro da fonte: não informado{' '}</span>}
    </li>)}
  </ul>}
</div>;
/** Campo de DTO histórico para consumidores que não exibem a ficha inteira. */
export const CampoComProva = ({ dados, campo, compacto = false, formatarValor = formatarDadoHistorico }: { dados: object | null; campo: string; compacto?: boolean; formatarValor?: HistoricalFormatter }) => {
  const prova = lerProvaCampo(dados, campo);
  return prova ? <ValorComProva prova={prova} compacto={compacto} />
    : <EscalarHistorico valor={(dados ? Reflect.get(dados, campo) : undefined)} formatarValor={formatarValor} presente={dados != null && Object.hasOwn(dados, campo)} />;
};
const Paginador = ({ pagina, total, quantidade, aoMudar, titulo }: { pagina: number; total: number; quantidade: number; aoMudar: (pagina: number) => void; titulo: string }) => <nav className={estilos.paginacao} aria-label={`Paginação de ${titulo}`}>
  <span>{quantidade} registro(s) · página {total ? pagina + 1 : 0} de {total} · 5 por página</span>
  <div><button type="button" disabled={pagina === 0} onClick={() => aoMudar(pagina - 1)}><ChevronLeft size={15} aria-hidden="true" /> Anterior</button>
    <button type="button" disabled={pagina + 1 >= total} onClick={() => aoMudar(pagina + 1)}>Próxima <ChevronRight size={15} aria-hidden="true" /></button></div>
</nav>;
const ListaDados = ({ dados, titulo }: { dados: unknown[]; titulo: string }) => {
  const [pagina, setPagina] = useState(0);
  const total = Math.ceil(dados.length / 5);
  const atual = Math.min(pagina, Math.max(0, total - 1));
  if (!dados.length) return <span>Nenhum registro retornado</span>;
  return <div className={estilos.listaDados}><ol start={atual * 5 + 1}>{dados.slice(atual * 5, atual * 5 + 5).map((item, indice) => <li key={indice}>{typeof item === 'object' && item !== null ? <DetalhesDados dados={item} /> : formatarDadoHistorico(item)}</li>)}</ol>
    {total > 1 && <Paginador pagina={atual} total={total} quantidade={dados.length} aoMudar={setPagina} titulo={titulo} />}</div>;
};
export const DetalhesDados = ({ dados, camposEsperados, formatarValor = formatarDadoHistorico }: { dados: unknown; camposEsperados?: string[]; formatarValor?: HistoricalFormatter }) => {
  if (!dados || typeof dados !== 'object') return <span>{formatarDadoHistorico(dados)}</span>;
  const campos = [...new Set([...(camposEsperados || []), ...Object.keys(dados), ...camposMetadadosProva(dados)])].filter(chave => chave !== 'provaCampos');
  return <dl className={estilos.dados}>{campos.map((chave) => {
    const valor: unknown = Reflect.get(dados, chave);
    const prova = lerProvaCampo(dados, chave);
    return <div key={chave}>
      <dt>{ROTULOS[chave] || humanizar(chave)}</dt><dd>{prova ? <ValorComProva prova={prova} /> : Array.isArray(valor) ? <ListaDados dados={valor} titulo={ROTULOS[chave] || humanizar(chave)} /> : valor && typeof valor === 'object' ? chave === 'fotografia' ? <FotografiaContratual dados={valor} /> : <DetalhesDados dados={valor} /> : <EscalarHistorico valor={valor} formatarValor={formatarValor} presente={Object.hasOwn(dados, chave)} />}</dd>
    </div>;
  })}</dl>;
};

// Campos escalares de VersaoContratoNegocialDTO. Enumerá-los explicita uma
// propriedade não retornada; não acrescenta valor nem estado documental ao DTO.
const CAMPOS_VERSAO_CONTRATUAL = [
  'id', 'numeroVersao', 'eventoOrigemId', 'dataEfetiva', 'registradoEm',
  ...CAMPOS_FOTOGRAFIA.map(({ nome }) => nome).filter((nome) => nome !== 'dataEfetiva'),
  'tipoContratacaoComprovado',
];
const formatarCampoFotografia = (valor: unknown, presente = false) => {
  if (!presente || valor === undefined) return 'Campo não retornado nesta consulta';
  if (valor === null || valor === '') return 'Valor não informado nesta consulta';
  return formatarDadoHistorico(valor);
};
const FotografiaContratual = ({ dados }: { dados: unknown }) => <div className={estilos.pilha}>
  {!possuiMetadadosProva(dados) && <p className={estilos.aviso} role="note" aria-label="Limites da comprovação histórica">
    A comprovação individual de cada campo não está disponível nesta consulta. Um campo sem valor pode representar uma lacuna histórica ou uma ausência documentada; esta leitura não permite distinguir as duas situações. Os valores atuais do cadastro não preenchem essas lacunas.
  </p>}
  <DetalhesDados dados={dados} camposEsperados={CAMPOS_VERSAO_CONTRATUAL} formatarValor={formatarCampoFotografia} />
</div>;

const DetalhesRegistro = ({ item, numero, fotografiaContratual }: { item: object; numero: string | number; fotografiaContratual?: boolean }) => {
  const [aberto, setAberto] = useState(false);
  return <details className={estilos.detalheRegistro} onToggle={(evento) => setAberto(evento.currentTarget.open)}>
    <summary>Ver registro {numero}</summary>{aberto && (fotografiaContratual ? <FotografiaContratual dados={item} /> : <DetalhesDados dados={item} />)}
  </details>;
};

export const TabelaHistorica = ({ titulo, itens = [], colunas, descricao, detalhes = true, fotografiaContratual = false, provaPorCampo = false, mensagemVazia = 'Nenhum registro retornado para esta consulta.' }: { titulo: string; itens?: object[]; colunas: HistoricalColumn[]; descricao?: string; detalhes?: boolean; fotografiaContratual?: boolean; provaPorCampo?: boolean; mensagemVazia?: string }) => {
  const lista = Array.isArray(itens) ? itens : [];
  const [pagina, setPagina] = useState(0);
  useEffect(() => setPagina(0), [itens]);
  const total = Math.ceil(lista.length / 5);
  const atual = Math.min(pagina, Math.max(0, total - 1));
  return <section className={estilos.cartao}>
    <header className={estilos.cabecalhoCartao}><h3>{titulo}</h3><span className={estilos.contagem}>{lista.length}</span></header>
    {descricao && <p className={estilos.ajuda}>{descricao}</p>}
    {provaPorCampo && lista.some(item => !possuiMetadadosProva(item)) && <p className={estilos.aviso} role="note">Parte desta resposta não informa comprovação por campo. Nesses registros, um valor ausente não distingue ausência documentada de desconhecimento; dados atuais não completam a leitura.</p>}
    {!lista.length ? <p className={estilos.vazioTabela}>{mensagemVazia}</p> : <>
      <div className={estilos.rolagemTabela}><table aria-label={titulo}><thead><tr>{colunas.map((coluna) => <th scope="col" key={coluna.chave}>{coluna.titulo}</th>)}{detalhes && <th scope="col">Detalhes</th>}</tr></thead>
        <tbody>{lista.slice(atual * 5, atual * 5 + 5).map((item, indice) => <tr key={`${Reflect.get(item, "id") ?? Reflect.get(item, "eventoId") ?? indice}-${indice}`}>
          {colunas.map((coluna) => {
            const prova = lerProvaCampo(item, coluna.chave);
            return <td key={coluna.chave}>{prova ? <ValorComProva prova={prova} compacto /> : coluna.renderizar ? coluna.renderizar(Reflect.get(item, coluna.chave), item) : <EscalarHistorico valor={Reflect.get(item, coluna.chave)} />}</td>;
          })}
          {detalhes && <td><DetalhesRegistro item={item} numero={String(Reflect.get(item, "id") ?? Reflect.get(item, "eventoId") ?? atual * 5 + indice + 1)} fotografiaContratual={fotografiaContratual} /></td>}
        </tr>)}</tbody></table></div>
      <Paginador pagina={atual} total={total} quantidade={lista.length} aoMudar={setPagina} titulo={titulo} />
    </>}
  </section>;
};

const EstadoFonte = ({ comprovado, titulo, children }: React.PropsWithChildren<{ comprovado?: boolean; titulo: string }>) => <section className={`${estilos.cartao} ${comprovado ? estilos.fonteComprovada : estilos.fonteDesconhecida}`}>
  <header className={estilos.cabecalhoCartao}>{comprovado ? <ShieldCheck size={22} aria-hidden="true" /> : <HelpCircle size={22} aria-hidden="true" />}<h2>{titulo}</h2><span className={estilos.selo}>{comprovado ? 'Fonte comprovada' : 'Conhecimento insuficiente'}</span></header>
  {children}
</section>;
export const ResultadoNome = ({ dados = {} }: { dados?: Partial<DTO.IdentidadeHistoricaResolvidaDTO> }) => {
  const { nome, comprovado, tipoFonte, fonteId, fonteRegistradaEm, inicioComprovado, fimComprovado } = dados;
  const conhecido = comprovado === true && nome != null;
  return <EstadoFonte comprovado={conhecido} titulo={conhecido ? nome : 'Nome desconhecido no período consultado'}>
    {!conhecido && <p className={estilos.ajuda}>A fonte retornada não comprova o nome nesse recorte. A mesma Pessoa/Vínculo permanece identificada pelos IDs consultados.</p>}
    <DetalhesDados dados={{ comprovado, tipoFonte, fonteId, fonteRegistradaEm, inicioComprovado, fimComprovado }} />
  </EstadoFonte>;
};
export const ResultadoMotivo = ({ dados = {} }: { dados?: Partial<DTO.ContextoMotivoHistoricoDTO> }) => {
  const { descricao, categoria, comprovado, tipoFonte, fonteId, fonteRegistradaEm, inicioComprovado, fimComprovado } = dados;
  const conhecido = comprovado === true && descricao != null;
  return <EstadoFonte comprovado={conhecido} titulo={conhecido ? descricao : 'Motivo histórico desconhecido no período consultado'}>
    <DetalhesDados dados={{ categoria: conhecido ? categoria : null, comprovado, tipoFonte, fonteId, fonteRegistradaEm, inicioComprovado, fimComprovado }} />
  </EstadoFonte>;
};
export const ResultadoCatalogo = ({ dados = {} }: { dados?: Partial<DTO.DisponibilidadeCatalogoDTO> }) => {
  const conhecido = dados.disponibilidadeComprovada === true && typeof dados.ativo === 'boolean';
  return <EstadoFonte comprovado={conhecido} titulo={conhecido ? (dados.ativo ? 'Ativo na data consultada' : 'Inativo na data consultada') : 'Disponibilidade desconhecida na data consultada'}>
    {!conhecido && <p className={estilos.ajuda}>O período anterior à primeira fonte permanece desconhecido.</p>}
    <DetalhesDados dados={dados} />
  </EstadoFonte>;
};

export const COLUNAS_EVENTOS = [{ chave: 'id', titulo: 'Evento' }, { chave: 'tipoEvento', titulo: 'Ato' }, { chave: 'dataEfetiva', titulo: 'Data efetiva' }, { chave: 'registradoEm', titulo: 'Conhecido desde' }];
export const COLUNAS_VERSOES = [{ chave: 'id', titulo: 'Fotografia' }, { chave: 'numeroVersao', titulo: 'Nº' }, { chave: 'eventoOrigemId', titulo: 'Evento de origem' }, { chave: 'dataEfetiva', titulo: 'Data efetiva' }, { chave: 'registradoEm', titulo: 'Registrada em' }];
export const COLUNAS_PROVAS = [{ chave: 'id', titulo: 'Prova' }, { chave: 'provaAnteriorId', titulo: 'Predecessora' }, { chave: 'registradoEm', titulo: 'Conhecida desde' }];
export const ResultadoContrato = ({ dados = {} }: { dados?: Partial<DTO.PosicaoTemporalContratoLeituraDTO> }) => <div className={estilos.pilha}>
  <section className={estilos.cartao}><header className={estilos.cabecalhoCartao}><FileClock size={22} aria-hidden="true" /><h2>Posição contratual no recorte</h2><span className={estilos.selo}>Reconstruída pelo servidor</span></header>
    <DetalhesDados dados={{ contratoId: dados.contratoId, dataReferencia: dados.dataReferencia, conhecidoAte: dados.conhecidoAte,
      estadoNegocial: dados.estadoNegocial, situacaoTemporal: dados.situacaoTemporal, habilitacaoAdministrativa: dados.habilitacaoAdministrativa,
      vigenciaInicio: dados.vigenciaInicio, vigenciaFim: dados.vigenciaFim,
      ...(possuiMetadadosProva(dados) ? { provaCampos: dados.provaCampos } : {}) }} />
    <details className={estilos.grupoDetalhes} open><summary>Fotografia contratual aplicada ao recorte</summary>
      {dados.versaoContratual ? <FotografiaContratual dados={dados.versaoContratual} /> : <p className={estilos.aviso}>Fotografia contratual desconhecida neste recorte.</p>}
    </details>
  </section>
  <TabelaHistorica titulo="Eventos aplicados ao recorte" itens={dados.eventosAplicados || []} colunas={COLUNAS_EVENTOS} descricao="Sequência retornada pelo motor temporal do servidor." />
  <TabelaHistorica titulo="Precedências consideradas" itens={dados.relacoesConsideradas || []} colunas={[{ chave: 'id', titulo: 'Relação' }, { chave: 'eventoFonteId', titulo: 'Evento fonte' }, { chave: 'eventoAlvoId', titulo: 'Evento alvo' }, { chave: 'tipoRelacao', titulo: 'Precedência' }]} />
</div>;

export const PublicacaoOriginal = ({ eventos = [], versoes = [], provas = [] }: { eventos?: DTO.EventoCicloVidaContratoDTO[]; versoes?: DTO.VersaoContratoLeituraDTO[]; provas?: DTO.ProvaHistoricaContratoDTO[] }) => {
  const publicacoes = eventos.filter((evento) => evento.tipoEvento === 'PUBLICACAO');
  const ids = new Set(publicacoes.map((evento) => String(evento.id)));
  const fotografias = versoes.filter((versao) => ids.has(String(versao.eventoOrigemId))).map((versao) => {
    const prova = provas.find((item) => String(item.versaoGeradaId) === String(versao.id));
    return { ...versao, origemFotografia: prova ? `Incorporada pela prova ${prova.id}` : 'Fotografia retornada no acervo' };
  });
  if (!publicacoes.length) return <div className={estilos.aviso}>Publicação original não comprovada no acervo retornado. A existência de versões de outros atos não comprova uma publicação.</div>;
  return <div className={estilos.pilha}>
    <TabelaHistorica titulo="Publicação original: eventos preservados" itens={publicacoes} colunas={COLUNAS_EVENTOS} descricao="Eventos de publicação efetivamente retornados. O acervo é consultado com o conhecimento atual." />
    <TabelaHistorica titulo="Fotografias vinculadas à publicação" itens={fotografias} colunas={[...COLUNAS_VERSOES, { chave: 'origemFotografia', titulo: 'Origem' }]} fotografiaContratual mensagemVazia="O servidor retornou o evento de publicação, mas não uma fotografia vinculada. A lacuna permanece explícita." />
  </div>;
};
