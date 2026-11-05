import React, { useCallback, useEffect, useRef, useState } from 'react';
import { BookOpenCheck, CalendarClock, FileClock, Search, Tags, UserRound } from 'lucide-react';
import { EstruturaPagina, EstadoPagina } from '../../core/ui/EstruturaPagina.jsx';
import { serializarLocalDate } from '../../core/http/datasHttp.js';
import { obterGeracaoSessao, obterSinalSessao, sessaoAtual } from '../../core/security/geracaoSessao.js';
import { ServicoHistorico } from '../../services/historico/ServicoHistorico.js';
import { idHistorico, prepararConsultaHistorica, tipoCatalogoHistorico, TIPOS_CATALOGO } from '../../services/historico/dominioHistorico.js';
import { CampoHistorico as Campo } from './CampoHistorico.jsx';
import { FormularioProvaHistorica } from './FormularioProvaHistorica.jsx';
import {
  COLUNAS_EVENTOS, COLUNAS_PROVAS, COLUNAS_VERSOES, formatarDadoHistorico,
  PublicacaoOriginal, ResultadoCatalogo, ResultadoContrato, ResultadoMotivo, ResultadoNome, TabelaHistorica,
} from './ResultadosHistoricos.jsx';
import estilos from './historico.module.css';

const ABAS = [
  { chave: 'contrato', nome: 'Contrato', icone: FileClock }, { chave: 'catalogo', nome: 'Catálogo', icone: Tags },
  { chave: 'nome', nome: 'Nome', icone: UserRound }, { chave: 'motivo', nome: 'Motivo', icone: BookOpenCheck },
];
const ROTULOS_ID = { contrato: 'ID do contrato', catalogo: 'ID do item do catálogo', nome: 'ID da pessoa', motivo: 'ID do motivo de término' };
const planos = {
  contrato: [
    ['posicao', 'Posição contratual', ServicoHistorico.consultarContrato],
    ['versoes', 'Fotografias contratuais', ServicoHistorico.listarVersoes],
    ['eventos', 'Eventos contratuais', ServicoHistorico.listarEventos],
    ['provas', 'Provas históricas do contrato', ServicoHistorico.listarProvasContrato],
    ['composicoesRecorte', 'Composições no recorte D/K', ServicoHistorico.listarComposicoesNoRecorte],
    ['composicoes', 'Composições por data', ServicoHistorico.listarComposicoes],
  ],
  catalogo: [['posicao', 'Disponibilidade histórica', ServicoHistorico.consultarCatalogo]],
  nome: [['posicao', 'Nome histórico', ServicoHistorico.consultarNome], ['provas', 'Cadeia de provas nominais', ServicoHistorico.listarProvasNome]],
  motivo: [['posicao', 'Contexto histórico do motivo', ServicoHistorico.consultarMotivo], ['provas', 'Cadeia de provas do motivo', ServicoHistorico.listarProvasMotivo]],
};
const COLUNAS_COMPOSICOES = [
  { chave: 'id', titulo: 'Composição' }, { chave: 'lotacaoNome', titulo: 'Lotação retornada' },
  { chave: 'jornadaDescricao', titulo: 'Jornada retornada' }, { chave: 'cargoNome', titulo: 'Cargo retornado' }, { chave: 'efetivoPrevisto', titulo: 'Efetivo previsto' },
];
const provasPessoais = (tipo) => [
  { chave: 'id', titulo: 'Prova' }, { chave: tipo === 'nome' ? 'nomeComprovado' : 'descricaoComprovada', titulo: tipo === 'nome' ? 'Nome documentado' : 'Descrição documentada' },
  { chave: 'dataInicioEfetiva', titulo: 'Desde' }, { chave: 'dataFimEfetiva', titulo: 'Até' }, { chave: 'registradoEm', titulo: 'Conhecida desde' },
];

export const PaginaHistorico = () => {
  const [geracaoSessao, setGeracaoSessao] = useState(obterGeracaoSessao);
  const [aba, setAba] = useState('contrato');
  const [filtros, setFiltros] = useState(() => ({ id: '', vinculoId: '', contratoId: '', tipoCatalogo: 'LOTACAO', data: serializarLocalDate(new Date()), conhecidoAte: new Date().toISOString() }));
  const [alvo, setAlvo] = useState(null);
  const [resultado, setResultado] = useState(null);
  const [carregando, setCarregando] = useState(false);
  const [erro, setErro] = useState(null);
  const [confirmando, setConfirmando] = useState(false);
  const [incorporado, setIncorporado] = useState(false);
  const consultaRef = useRef(null);
  const geracaoRef = useRef(0);

  const invalidarConsulta = useCallback(() => {
    consultaRef.current?.abort();
    consultaRef.current = null;
    geracaoRef.current += 1;
    setCarregando(false); setAlvo(null); setResultado(null); setErro(null); setIncorporado(false);
  }, []);
  const mudarFiltro = (campo, valor) => {
    if (confirmando) return;
    invalidarConsulta();
    setFiltros((estado) => ({ ...estado, [campo]: valor }));
  };
  const mudarAba = (chave) => {
    if (confirmando || chave === aba) return;
    invalidarConsulta(); setAba(chave);
    setFiltros((estado) => ({ ...estado, id: '', vinculoId: '', contratoId: '' }));
  };
  useEffect(() => () => { consultaRef.current?.abort(); geracaoRef.current += 1; }, []);
  useEffect(() => {
    const sinal = obterSinalSessao(geracaoSessao);
    const encerrar = () => {
      setGeracaoSessao(obterGeracaoSessao());
      invalidarConsulta(); setConfirmando(false);
    };
    sinal.addEventListener('abort', encerrar, { once: true });
    if (sinal.aborted) encerrar();
    return () => sinal.removeEventListener('abort', encerrar);
  }, [geracaoSessao, invalidarConsulta]);

  const consultar = async (evento) => {
    evento?.preventDefault();
    if (!sessaoAtual(geracaoSessao) || consultaRef.current || confirmando) return;
    let consulta;
    try {
      consulta = prepararConsultaHistorica(filtros);
      if (aba === 'catalogo') consulta.tipoCatalogo = tipoCatalogoHistorico(filtros.tipoCatalogo);
      if (aba === 'nome') consulta.vinculoId = idHistorico(filtros.vinculoId, 'ID do vínculo');
      if (aba === 'motivo') consulta.contratoId = idHistorico(filtros.contratoId, 'ID do contrato');
    } catch (falha) { setErro(falha.message); return; }
    const controlador = new AbortController();
    consultaRef.current = controlador;
    const geracao = ++geracaoRef.current;
    setCarregando(true); setAlvo(consulta); setResultado(null); setErro(null); setIncorporado(false);
    const chamadas = planos[aba];
    const respostas = await Promise.allSettled(chamadas.map(([, , executar]) => executar(consulta, { signal: controlador.signal })));
    if (!sessaoAtual(geracaoSessao) || controlador.signal.aborted || geracaoRef.current !== geracao) return;
    const dados = { erros: [] };
    respostas.forEach((resposta, indice) => {
      const [chave, rotulo] = chamadas[indice];
      if (resposta.status === 'fulfilled') dados[chave] = resposta.value;
      else if (!resposta.reason?.canceled) dados.erros.push({ rotulo, mensagem: resposta.reason?.message || 'Não foi possível obter esta parte da consulta.' });
    });
    consultaRef.current = null;
    setResultado(dados); setCarregando(false);
  };

  const posicao = resultado?.posicao && !Array.isArray(resultado.posicao) ? resultado.posicao : null;
  return <EstruturaPagina titulo="Histórico e provas" subtitulo="Consulte a situação efetiva em uma data e o conhecimento disponível em outro instante. Incorpore fontes documentais com prévia dos impactos." className={estilos.pagina}>
    <div className={estilos.abas} role="tablist" aria-label="Contexto do histórico">{ABAS.map(({ chave, nome, icone: Icone }) => <button key={chave} id={`historico-aba-${chave}`} type="button" role="tab" aria-selected={aba === chave} aria-controls={`historico-painel-${chave}`} disabled={confirmando} onClick={() => mudarAba(chave)}><Icone size={18} aria-hidden="true" />{nome}</button>)}</div>
    <div id={`historico-painel-${aba}`} role="tabpanel" aria-labelledby={`historico-aba-${aba}`} className={estilos.pilha}>
      <form aria-label="Consulta histórica" className={estilos.consulta} noValidate onSubmit={consultar}>
        <div className={estilos.cabecalhoCartao}><CalendarClock size={19} aria-hidden="true" /><h2>Identifique o registro e os dois recortes</h2></div>
        <div className={estilos.gridConsulta}>
          {aba === 'catalogo' && <Campo rotulo="Tipo de catálogo" valor={filtros.tipoCatalogo} opcoes={TIPOS_CATALOGO} disabled={confirmando} aoMudar={(valor) => mudarFiltro('tipoCatalogo', valor)} />}
          <Campo rotulo={ROTULOS_ID[aba]} tipo="number" min="1" step="1" required valor={filtros.id} disabled={confirmando} aoMudar={(valor) => mudarFiltro('id', valor)} />
          {aba === 'nome' && <Campo rotulo="ID do vínculo" tipo="number" min="1" step="1" required valor={filtros.vinculoId} disabled={confirmando} aoMudar={(valor) => mudarFiltro('vinculoId', valor)} />}
          {aba === 'motivo' && <Campo rotulo="ID do contrato" tipo="number" min="1" step="1" required valor={filtros.contratoId} disabled={confirmando} aoMudar={(valor) => mudarFiltro('contratoId', valor)} />}
          <Campo rotulo="Data efetiva consultada" tipo="date" required valor={filtros.data} disabled={confirmando} aoMudar={(valor) => mudarFiltro('data', valor)} />
          <Campo rotulo="Conhecido até (ISO com fuso)" valor={filtros.conhecidoAte} placeholder="2026-01-02T12:00:00-03:00" disabled={confirmando} aoMudar={(valor) => mudarFiltro('conhecidoAte', valor)} />
          <button type="submit" className={estilos.primario} disabled={carregando || confirmando}><Search size={17} aria-hidden="true" />{carregando ? 'Consultando…' : 'Consultar histórico'}</button>
        </div>
        <p className={estilos.ajuda}>A data escolhe o fato efetivo; o instante limita o que já era conhecido. Use Z para UTC ou um deslocamento como -03:00. Deixe o instante vazio somente para consultar o conhecimento atual. Os IDs mantêm a identidade canônica mesmo com alterações posteriores.</p>
        {erro && <div role="alert" className={estilos.erro}>{erro}</div>}
      </form>

      {alvo && <div className={estilos.contexto}><strong>Registro #{alvo.id}</strong><span>Data efetiva: {formatarDadoHistorico(alvo.data)}</span><span>Conhecido até: {alvo.conhecidoAte ? formatarDadoHistorico(alvo.conhecidoAte) : 'conhecimento atual do servidor'}</span></div>}
      {carregando ? <EstadoPagina titulo="Consultando o histórico" descricao="O servidor aplica as regras temporais e o seu escopo de acesso." girando /> : !alvo ? <EstadoPagina titulo="Escolha um recorte histórico" descricao="Informe o registro e as datas para consultar posições, fotografias e fontes preservadas." /> : resultado && <>
        {resultado.erros.map((item, indice) => <div key={indice} role="alert" className={estilos.erro}><strong>{item.rotulo}:</strong> {item.mensagem}</div>)}
        {posicao && aba === 'contrato' && <ResultadoContrato dados={posicao} />}
        {posicao && aba === 'catalogo' && <ResultadoCatalogo dados={posicao} />}
        {posicao && aba === 'nome' && <ResultadoNome dados={posicao} />}
        {posicao && aba === 'motivo' && <ResultadoMotivo dados={posicao} />}
        {!posicao && !resultado.erros.some((item) => item.rotulo === planos[aba][0][1]) && <div className={estilos.aviso}>O servidor não retornou uma posição histórica para este recorte.</div>}

        {aba === 'contrato' && <>
          {Array.isArray(resultado.provas) && <TabelaHistorica titulo="Provas conhecidas até o corte" itens={resultado.provas} colunas={[...COLUNAS_PROVAS, { chave: 'eventoOrigemId', titulo: 'Evento de origem' }, { chave: 'numeroRevisao', titulo: 'Revisão' }]} descricao="Recibos retornados pelo servidor com o corte de conhecimento informado." />}
          {Array.isArray(resultado.composicoesRecorte) && <TabelaHistorica titulo="Composições no recorte D/K" itens={resultado.composicoesRecorte} colunas={COLUNAS_COMPOSICOES} provaPorCampo
            descricao="Configurações reconstruídas pelo servidor para a data efetiva e o corte de conhecimento consultados. Os detalhes preservam a prova independente de cada campo e suas fontes." />}
          <details className={estilos.grupoDetalhes}><summary>Publicação original e acervo contratual · conhecimento atual</summary>
            <p className={estilos.ajuda}>Este acervo contém as versões e os eventos retornados na consulta atual. A posição acima já informa quais eventos foram aplicados no recorte escolhido.</p>
            <div className={estilos.pilha}>
              {Array.isArray(resultado.eventos) && Array.isArray(resultado.versoes) && <PublicacaoOriginal eventos={resultado.eventos} versoes={resultado.versoes} provas={resultado.provas || []} />}
              {Array.isArray(resultado.versoes) && <TabelaHistorica titulo="Cadeia completa de fotografias retornadas" itens={resultado.versoes} colunas={COLUNAS_VERSOES} fotografiaContratual />}
              {Array.isArray(resultado.eventos) && <TabelaHistorica titulo="Cadeia completa de eventos retornados" itens={resultado.eventos} colunas={COLUNAS_EVENTOS} />}
              {Array.isArray(resultado.composicoes) && <TabelaHistorica titulo="Composições na data · conhecimento atual" itens={resultado.composicoes} colunas={COLUNAS_COMPOSICOES} provaPorCampo descricao="Esta consulta usa apenas a data efetiva. O corte 'conhecido até' não é aplicado a estas composições; elas não representam a publicação original nem substituem a posição bitemporal acima." />}
            </div>
          </details>
        </>}
        {(aba === 'nome' || aba === 'motivo') && Array.isArray(resultado.provas) && <TabelaHistorica titulo="Cadeia documental · conhecimento atual" itens={resultado.provas} colunas={provasPessoais(aba)} descricao="A cadeia inclui as provas retornadas no escopo atual. O corte de conhecimento da posição acima não filtra esta listagem." />}
        {incorporado && <p className={estilos.aviso}>Uma fonte foi incorporada. Consulte novamente para atualizar o recorte e a cadeia documental.</p>}
        <details className={estilos.incorporacao}><summary>{aba === 'catalogo' ? 'Registrar um ato de disponibilidade' : 'Incorporar uma fonte histórica'}</summary>
          <FormularioProvaHistorica key={`${aba}:${alvo.id}:${alvo.vinculoId || alvo.contratoId || ''}`} tipo={aba} alvo={alvo} aoOcupacao={setConfirmando} aoSucesso={() => setIncorporado(true)} />
        </details>
      </>}
    </div>
  </EstruturaPagina>;
};
export default PaginaHistorico;
