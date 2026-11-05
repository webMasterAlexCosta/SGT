import React, { useCallback, useEffect, useMemo, useRef, useState } from 'react';
import {
  AlertTriangle,
  ArrowRightLeft,
  Calculator,
  FilePenLine,
  Layers3,
  Plus,
  RefreshCw,
  Save,
  Trash2,
  X,
} from 'lucide-react';
import { useConfirmacaoSenha } from '../../../../../../core/security/useConfirmacaoSenha.jsx';
import { useConsultaVigente } from '../../../../../../core/hooks/useConsultaVigente.js';
import { notificar } from '../../../../../../core/events.js';
import { ServicoAlteracoesContrato } from '../../../../../../services/admin/ServicoAlteracoesContrato.js';
import { ServicoLotacao } from '../../../../../../services/admin/ServicoLotacao.js';
import { ServicoJornada } from '../../../../../../services/admin/ServicoJornada.js';
import { ServicoRecursoGenerico } from '../../../../../../services/admin/ServicoRecursoGenerico.js';
import {
  criarEstadoPrevia,
  criarLinhaAditamento,
  criarLinhaRemanejamento,
  ehConflitoDePrevia,
  invalidarPrevia,
  payloadComTokenDaPrevia,
  prepararAditamento,
  prepararComposicao,
  prepararRemanejamento,
  previaContinuaValida,
  registrarPrevia,
  somaVariacoesRemanejamento,
  validarAditamentoLocal,
  validarRemanejamentoLocal,
} from '../../../../../../services/admin/ServicoDominioAlteracoesContrato.js';
import { ehRascunho, formatarDataBrasileira } from '../../../../../../services/admin/ServicoDominioContrato.js';
import { possuiMetadadosProva, rotuloComposicaoComProva } from '../../../../../../services/historico/provaHistoricaLeitura.js';
import { CampoComProva } from '../../../../../historico/ResultadosHistoricos.jsx';
import estilos from './index.module.css';

const ServicoCargo = ServicoRecursoGenerico('/cargos');
const hoje = () => new Date().toISOString().slice(0, 10);
const normalizarLista = (valor) => Array.isArray(valor) ? valor : [];
const dataFutura = (data) => Boolean(data && String(data) > hoje());

const FORM_COMPOSICAO = Object.freeze({
  id: null,
  lotacaoId: '',
  jornadaId: '',
  cargoId: '',
  efetivoPrevisto: 0,
  dataInicioVigencia: '',
  dataFimVigencia: '',
  version: null,
});

const FORM_ADITAMENTO = Object.freeze({
  identificacao: '',
  dataDocumento: '',
  dataAssinatura: '',
  dataEfetiva: '',
  justificativa: '',
  referenciaSei: '',
  novaDescricao: '',
  novaDataInicio: '',
  novaDataFim: '',
  novoEfetivoTotal: '',
  reducaoExcepcional: false,
  fundamentoExcecao: '',
  confirmarImpacto: false,
  alteracoesComposicao: [],
  eventoPrecedenteId: null,
  relacaoPrecedencia: null,
  ordemDocumental: '',
});

const FORM_REMANEJAMENTO = Object.freeze({
  identificacao: '',
  dataEfetiva: '',
  justificativa: '',
  referenciaSei: '',
  confirmarImpacto: false,
  itens: [],
  eventoPrecedenteId: null,
  relacaoPrecedencia: null,
  ordemDocumental: '',
});

const rotuloComposicao = rotuloComposicaoComProva;
const rotuloSituacao = (valor) => String(valor || 'VIGENTE').replaceAll('_', ' ');

const CartaoAlerta = ({ children, perigo = false }) => (
  <div className={`${estilos.alerta} ${perigo ? estilos.alertaPerigo : ''}`}><AlertTriangle size={17}/><span>{children}</span></div>
);

const ListaImpactos = ({ impactos = [] }) => {
  if (!impactos.length) return null;
  return <div className={estilos.impactos}>{impactos.map((item) => (
    <div className={estilos.impacto} key={`${item.composicaoId || 'nova'}-${item.lotacaoId}-${item.jornadaId}-${item.cargoId}`}>
      <strong>{rotuloComposicao(item)}</strong>
      <span>{item.quantidadeAnterior ?? 0} → {item.quantidadeResultante ?? 0} ({Number(item.variacao || 0) >= 0 ? '+' : ''}{item.variacao ?? 0})</span>
      <small>Ocupação na data: {item.colaboradoresAtuais ?? 0}{item.excedenteResultante > 0 ? ` · excedente resultante: ${item.excedenteResultante}` : ''}</small>
      {item.exigeTratamento && <em>Exige tratamento operacional; pessoas não serão movidas automaticamente.</em>}
    </div>
  ))}</div>;
};

const BotoesAbas = ({ aba, aoAlterar }) => (
  <div className={estilos.abas} role="tablist" aria-label="Alterações contratuais">
    <button type="button" role="tab" aria-selected={aba === 'composicao'} className={aba === 'composicao' ? estilos.abaAtiva : ''} onClick={() => aoAlterar('composicao')}><Layers3 size={16}/>Composição</button>
    <button type="button" role="tab" aria-selected={aba === 'aditamentos'} className={aba === 'aditamentos' ? estilos.abaAtiva : ''} onClick={() => aoAlterar('aditamentos')}><FilePenLine size={16}/>Aditamentos</button>
    <button type="button" role="tab" aria-selected={aba === 'remanejamentos'} className={aba === 'remanejamentos' ? estilos.abaAtiva : ''} onClick={() => aoAlterar('remanejamentos')}><ArrowRightLeft size={16}/>Remanejamentos</button>
  </div>
);

export const PainelAlteracoesContrato = ({ contrato, podeGerenciar = false }) => {
  const contratoId = contrato?.id;
  const rascunho = ehRascunho(contrato);
  const { confirmarComSenha, ModalConfirmacao } = useConfirmacaoSenha();
  const [aba, setAba] = useState('composicao');
  const [carregando, setCarregando] = useState(false);
  const [erroConsulta, setErroConsulta] = useState('');
  const [erroCatalogos, setErroCatalogos] = useState('');
  const [processando, setProcessando] = useState(false);
  const [dataReferencia, setDataReferencia] = useState(hoje());
  const [composicoes, setComposicoes] = useState([]);
  const [excedentes, setExcedentes] = useState([]);
  const [aditamentos, setAditamentos] = useState([]);
  const [remanejamentos, setRemanejamentos] = useState([]);
  const [lotacoes, setLotacoes] = useState([]);
  const [jornadas, setJornadas] = useState([]);
  const [cargos, setCargos] = useState([]);
  const [formComposicao, setFormComposicao] = useState({ ...FORM_COMPOSICAO });
  const [formAditamento, setFormAditamento] = useState({ ...FORM_ADITAMENTO });
  const [formRemanejamento, setFormRemanejamento] = useState({ ...FORM_REMANEJAMENTO });
  const [previaAditamento, setPreviaAditamento] = useState(criarEstadoPrevia());
  const [previaRemanejamento, setPreviaRemanejamento] = useState(criarEstadoPrevia());
  const [retificandoAditamentoId, setRetificandoAditamentoId] = useState(null);
  const [retificandoRemanejamentoId, setRetificandoRemanejamentoId] = useState(null);
  const [calculandoPreviaAditamento, setCalculandoPreviaAditamento] = useState(false);
  const [calculandoPreviaRemanejamento, setCalculandoPreviaRemanejamento] = useState(false);
  const consultas = useConsultaVigente(`${contratoId}:${dataReferencia}`);
  const consultasCatalogos = useConsultaVigente(`${contratoId}:${podeGerenciar}`);
  const chavePreviaAditamento = JSON.stringify([contratoId, podeGerenciar, rascunho, retificandoAditamentoId, formAditamento]);
  const chavePreviaRemanejamento = JSON.stringify([contratoId, podeGerenciar, rascunho, retificandoRemanejamentoId, formRemanejamento]);
  const consultasPreviaAditamento = useConsultaVigente(chavePreviaAditamento);
  const consultasPreviaRemanejamento = useConsultaVigente(chavePreviaRemanejamento);
  const contextoAtual = useRef(null);
  contextoAtual.current = { contrato, podeGerenciar, formAditamento, formRemanejamento, previaAditamento, previaRemanejamento,
    chavePreviaAditamento, chavePreviaRemanejamento };

  useEffect(() => {
    setPreviaAditamento(criarEstadoPrevia());
    setCalculandoPreviaAditamento(false);
  }, [chavePreviaAditamento]);
  useEffect(() => {
    setPreviaRemanejamento(criarEstadoPrevia());
    setCalculandoPreviaRemanejamento(false);
  }, [chavePreviaRemanejamento]);

  const confirmarPreviaAtual = (tipo, token) => {
    const atual = contextoAtual.current;
    const aditamento = tipo === 'aditamento';
    const previa = aditamento ? atual.previaAditamento : atual.previaRemanejamento;
    const dados = aditamento ? atual.formAditamento : atual.formRemanejamento;
    const chave = aditamento ? atual.chavePreviaAditamento : atual.chavePreviaRemanejamento;
    if (atual.contrato?.id === contratoId && atual.podeGerenciar && !ehRascunho(atual.contrato)
      && previa?.contratoId === contratoId && previa?.contexto === chave
      && previa?.previa?.tokenPrevia === token && previaContinuaValida(previa, dados)) return true;
    notificar('warning', 'A prévia não é mais válida. Calcule novamente antes de confirmar.');
    return false;
  };

  const carregar = useCallback(async () => {
    if (!contratoId) return;
    const consulta = consultas.iniciar();
    const options = { signal: consulta.signal, silentError: true };
    setCarregando(true);
    setErroConsulta('');
    setComposicoes([]);
    setExcedentes([]);
    setAditamentos([]);
    setRemanejamentos([]);
    try {
      const [listaComposicoes, listaExcedentes, listaAditamentos, listaRemanejamentos] = await Promise.all([
        ServicoAlteracoesContrato.listarComposicoes(contratoId, dataReferencia, options),
        ServicoAlteracoesContrato.listarExcedentes(contratoId, dataReferencia, options),
        ServicoAlteracoesContrato.listarAditamentos(contratoId, options),
        ServicoAlteracoesContrato.listarRemanejamentos(contratoId, options),
      ]);
      if (!consulta.atual()) return;
      setComposicoes(normalizarLista(listaComposicoes));
      setExcedentes(normalizarLista(listaExcedentes));
      setAditamentos(normalizarLista(listaAditamentos));
      setRemanejamentos(normalizarLista(listaRemanejamentos));
    } catch (erro) {
      if (consulta.atual() && !erro?.canceled) {
        const mensagem = 'Não foi possível carregar composição e alterações do contrato. Atualize para tentar novamente.';
        setErroConsulta(mensagem);
        notificar('error', mensagem);
      }
    } finally {
      if (consulta.atual()) setCarregando(false);
    }
  }, [contratoId, dataReferencia, consultas]);

  useEffect(() => { carregar(); }, [carregar]);

  useEffect(() => {
    if (!contratoId || !podeGerenciar) return;
    const consulta = consultasCatalogos.iniciar();
    const options = { signal: consulta.signal, silentError: true };
    setErroCatalogos('');
    setLotacoes([]);
    setJornadas([]);
    setCargos([]);
    Promise.all([
      ServicoLotacao.listarTodos({ size: 500 }, options),
      ServicoJornada.listarTodos(options),
      ServicoCargo.listarTodos(options),
    ]).then(([l, j, c]) => {
      if (!consulta.atual()) return;
      setLotacoes(normalizarLista(l));
      setJornadas(normalizarLista(j));
      setCargos(normalizarLista(c));
    }).catch(erro => {
      if (consulta.atual() && !erro?.canceled) setErroCatalogos('Não foi possível carregar os catálogos de lotação, turno e cargo. Reabra o painel para tentar novamente.');
    });
    return consultasCatalogos.invalidar;
  }, [contratoId, podeGerenciar, consultasCatalogos]);

  const atualizarAditamento = (campo, valor) => {
    setFormAditamento((atual) => ({ ...atual, [campo]: valor }));
    setPreviaAditamento(invalidarPrevia());
  };
  const atualizarRemanejamento = (campo, valor) => {
    setFormRemanejamento((atual) => ({ ...atual, [campo]: valor }));
    setPreviaRemanejamento(invalidarPrevia());
  };

  const salvarComposicao = async (evento) => {
    evento.preventDefault();
    if (!rascunho || !podeGerenciar || processando) return;
    if (!formComposicao.lotacaoId || !formComposicao.jornadaId || !formComposicao.cargoId) {
      notificar('warning', 'Informe Lotação, Turno/Jornada e Cargo.');
      return;
    }
    const payload = prepararComposicao(formComposicao);
    await confirmarComSenha({
      titulo: formComposicao.id ? 'Confirmar alteração da composição' : 'Confirmar inclusão na composição',
      mensagem: 'A composição direta só pode ser alterada enquanto o contrato estiver em rascunho.',
      acao: async (senhaConfirmacao) => {
        setProcessando(true);
        try {
          if (formComposicao.id) await ServicoAlteracoesContrato.atualizarComposicao(contratoId, formComposicao.id, payload, { senhaConfirmacao });
          else await ServicoAlteracoesContrato.criarComposicao(contratoId, payload, { senhaConfirmacao });
          setFormComposicao({ ...FORM_COMPOSICAO });
          await carregar();
        } finally { setProcessando(false); }
      },
    });
  };

  const editarComposicao = (item) => setFormComposicao({
    ...FORM_COMPOSICAO,
    ...item,
    lotacaoId: String(item.lotacaoId || ''),
    jornadaId: String(item.jornadaId || ''),
    cargoId: String(item.cargoId || ''),
    dataInicioVigencia: item.dataInicioVigencia || '',
    dataFimVigencia: item.dataFimVigencia || '',
  });

  const excluirComposicao = async (item) => {
    await confirmarComSenha({
      titulo: 'Remover composição do rascunho?',
      mensagem: `${rotuloComposicao(item)} será removida. A operação não é permitida após publicação.`,
      acao: async (senhaConfirmacao) => {
        setProcessando(true);
        try { await ServicoAlteracoesContrato.excluirComposicao(contratoId, item.id, { senhaConfirmacao }); await carregar(); }
        finally { setProcessando(false); }
      },
    });
  };

  const alterarLinhaAditamento = (chaveCliente, campo, valor) => {
    atualizarAditamento('alteracoesComposicao', formAditamento.alteracoesComposicao.map((linha) => linha.chaveCliente === chaveCliente ? { ...linha, [campo]: valor } : linha));
  };
  const removerLinhaAditamento = (chaveCliente) => atualizarAditamento('alteracoesComposicao', formAditamento.alteracoesComposicao.filter((linha) => linha.chaveCliente !== chaveCliente));
  const adicionarLinhaAditamento = () => atualizarAditamento('alteracoesComposicao', [...formAditamento.alteracoesComposicao, criarLinhaAditamento()]);

  const calcularPreviaAditamento = async () => {
    const erro = validarAditamentoLocal(formAditamento);
    if (erro) { notificar('warning', erro); return; }
    const consulta = consultasPreviaAditamento.iniciar();
    setCalculandoPreviaAditamento(true);
    try {
      const dadosSemToken = prepararAditamento(formAditamento, null);
      const previa = await ServicoAlteracoesContrato.previaAditamento(contratoId, dadosSemToken, { signal: consulta.signal });
      if (!consulta.atual()) return;
      setPreviaAditamento({ ...registrarPrevia(formAditamento, previa), contratoId, contexto: chavePreviaAditamento });
    } catch (erroReq) {
      if (consulta.atual() && !erroReq?.canceled) notificar('error', erroReq?.message || 'Não foi possível calcular a prévia do aditamento.');
    } finally { if (consulta.atual()) setCalculandoPreviaAditamento(false); }
  };

  const confirmarAditamento = async () => {
    const payload = payloadComTokenDaPrevia(prepararAditamento, formAditamento, previaAditamento);
    if (!payload) { notificar('warning', 'A prévia não é mais válida. Calcule novamente antes de confirmar.'); return; }
    if (previaAditamento.previa?.possuiImpactoOcupacao && !formAditamento.confirmarImpacto) {
      notificar('warning', 'Confirme explicitamente o impacto sobre a ocupação antes de registrar.'); return;
    }
    if (previaAditamento.previa?.exigeAutorizacaoExcepcional && (!formAditamento.reducaoExcepcional || !String(formAditamento.fundamentoExcecao || '').trim())) {
      notificar('warning', 'A redução acima do limite normal exige fluxo excepcional e fundamento.'); return;
    }
    await confirmarComSenha({
      titulo: retificandoAditamentoId ? 'Confirmar retificação do aditamento' : 'Confirmar aditamento',
      mensagem: 'A confirmação usa exclusivamente o token da prévia imediatamente anterior. Pessoas não serão movimentadas automaticamente.',
      acao: async (senhaConfirmacao) => {
        if (!confirmarPreviaAtual('aditamento', payload.tokenPrevia)) return false;
        setProcessando(true);
        try {
          if (retificandoAditamentoId) await ServicoAlteracoesContrato.retificarAditamento(contratoId, retificandoAditamentoId, payload, { senhaConfirmacao });
          else await ServicoAlteracoesContrato.registrarAditamento(contratoId, payload, { senhaConfirmacao });
          setFormAditamento({ ...FORM_ADITAMENTO });
          setRetificandoAditamentoId(null);
          setPreviaAditamento(criarEstadoPrevia());
          await carregar();
        } catch (erroReq) {
          if (ehConflitoDePrevia(erroReq)) {
            setPreviaAditamento(criarEstadoPrevia());
            notificar('warning', 'O contexto mudou ou a prévia expirou. Gere uma nova prévia antes de confirmar.');
          }
          throw erroReq;
        } finally { setProcessando(false); }
      },
    });
  };

  const iniciarRetificacaoAditamento = (item) => {
    setRetificandoAditamentoId(item.id);
    setFormAditamento({
      ...FORM_ADITAMENTO,
      identificacao: item.identificacao || '',
      dataDocumento: item.dataDocumento || '',
      dataAssinatura: item.dataAssinatura || '',
      dataEfetiva: item.dataEfetiva || '',
      justificativa: item.justificativa || '',
      referenciaSei: item.referenciaSei || '',
      novaDescricao: item.novaDescricao || '',
      novaDataInicio: item.novaDataInicio || '',
      novaDataFim: item.novaDataFim || '',
      novoEfetivoTotal: item.novoEfetivoTotal ?? '',
      reducaoExcepcional: Boolean(item.reducaoExcepcional),
      fundamentoExcecao: item.fundamentoExcecao || '',
      confirmarImpacto: Boolean(item.impactoConfirmado),
      eventoPrecedenteId: item.eventoId || null,
      relacaoPrecedencia: 'RETIFICA',
      alteracoesComposicao: (item.alteracoesComposicao || []).map((impacto) => criarLinhaAditamento({ ...impacto, quantidadeResultante: impacto.quantidadeResultante })),
    });
    setPreviaAditamento(criarEstadoPrevia());
  };

  const alterarLinhaRemanejamento = (chaveCliente, campo, valor) => atualizarRemanejamento('itens', formRemanejamento.itens.map((linha) => linha.chaveCliente === chaveCliente ? { ...linha, [campo]: valor } : linha));
  const removerLinhaRemanejamento = (chaveCliente) => atualizarRemanejamento('itens', formRemanejamento.itens.filter((linha) => linha.chaveCliente !== chaveCliente));
  const adicionarLinhaRemanejamento = () => atualizarRemanejamento('itens', [...formRemanejamento.itens, criarLinhaRemanejamento()]);

  const calcularPreviaRemanejamento = async () => {
    const erro = validarRemanejamentoLocal(formRemanejamento);
    if (erro) { notificar('warning', erro); return; }
    const consulta = consultasPreviaRemanejamento.iniciar();
    setCalculandoPreviaRemanejamento(true);
    try {
      const dadosSemToken = prepararRemanejamento(formRemanejamento, null);
      const previa = await ServicoAlteracoesContrato.previaRemanejamento(contratoId, dadosSemToken, { signal: consulta.signal });
      if (!consulta.atual()) return;
      setPreviaRemanejamento({ ...registrarPrevia(formRemanejamento, previa), contratoId, contexto: chavePreviaRemanejamento });
    } catch (erroReq) {
      if (consulta.atual() && !erroReq?.canceled) notificar('error', erroReq?.message || 'Não foi possível calcular a prévia do remanejamento.');
    } finally { if (consulta.atual()) setCalculandoPreviaRemanejamento(false); }
  };

  const confirmarRemanejamento = async () => {
    const payload = payloadComTokenDaPrevia(prepararRemanejamento, formRemanejamento, previaRemanejamento);
    if (!payload) { notificar('warning', 'A prévia não é mais válida. Calcule novamente antes de confirmar.'); return; }
    if (previaRemanejamento.previa?.possuiImpactoOcupacao && !formRemanejamento.confirmarImpacto) {
      notificar('warning', 'Confirme explicitamente o impacto sobre a ocupação antes de registrar.'); return;
    }
    await confirmarComSenha({
      titulo: retificandoRemanejamentoId ? 'Confirmar retificação do remanejamento' : 'Confirmar remanejamento',
      mensagem: 'A soma das variações deve permanecer zero e a confirmação usa o token da prévia vigente.',
      acao: async (senhaConfirmacao) => {
        if (!confirmarPreviaAtual('remanejamento', payload.tokenPrevia)) return false;
        setProcessando(true);
        try {
          if (retificandoRemanejamentoId) await ServicoAlteracoesContrato.retificarRemanejamento(contratoId, retificandoRemanejamentoId, payload, { senhaConfirmacao });
          else await ServicoAlteracoesContrato.registrarRemanejamento(contratoId, payload, { senhaConfirmacao });
          setFormRemanejamento({ ...FORM_REMANEJAMENTO });
          setRetificandoRemanejamentoId(null);
          setPreviaRemanejamento(criarEstadoPrevia());
          await carregar();
        } catch (erroReq) {
          if (ehConflitoDePrevia(erroReq)) {
            setPreviaRemanejamento(criarEstadoPrevia());
            notificar('warning', 'O contexto mudou ou a prévia expirou. Gere uma nova prévia antes de confirmar.');
          }
          throw erroReq;
        } finally { setProcessando(false); }
      },
    });
  };

  const iniciarRetificacaoRemanejamento = (item) => {
    setRetificandoRemanejamentoId(item.id);
    setFormRemanejamento({
      ...FORM_REMANEJAMENTO,
      identificacao: item.identificacao || '',
      dataEfetiva: item.dataEfetiva || '',
      justificativa: item.justificativa || '',
      referenciaSei: item.referenciaSei || '',
      eventoPrecedenteId: item.eventoId || null,
      relacaoPrecedencia: 'RETIFICA',
      itens: (item.itens || []).map((impacto) => criarLinhaRemanejamento({ ...impacto, variacao: impacto.variacao })),
    });
    setPreviaRemanejamento(criarEstadoPrevia());
  };

  const cancelarProgramacao = async (tipo, item) => {
    const motivo = window.prompt(`Motivo do cancelamento do ${tipo === 'aditamento' ? 'aditamento' : 'remanejamento'} futuro:`);
    if (!String(motivo || '').trim()) return;
    await confirmarComSenha({
      titulo: 'Confirmar cancelamento de fato futuro',
      mensagem: 'Somente fato negocial ainda futuro e vigente pode ser cancelado antes de produzir efeito.',
      acao: async (senhaConfirmacao) => {
        setProcessando(true);
        try {
          const payload = { dataEfetiva: item.dataEfetiva, dataAto: hoje(), motivo: String(motivo).trim(), referenciaSei: item.referenciaSei || null };
          if (tipo === 'aditamento') await ServicoAlteracoesContrato.cancelarAditamento(contratoId, item.id, payload, { senhaConfirmacao });
          else await ServicoAlteracoesContrato.cancelarRemanejamento(contratoId, item.id, payload, { senhaConfirmacao });
          await carregar();
        } finally { setProcessando(false); }
      },
    });
  };

  const previaAditValida = previaAditamento.contratoId === contratoId && previaAditamento.contexto === chavePreviaAditamento
    && previaContinuaValida(previaAditamento, formAditamento);
  const previaRemValida = previaRemanejamento.contratoId === contratoId && previaRemanejamento.contexto === chavePreviaRemanejamento
    && previaContinuaValida(previaRemanejamento, formRemanejamento);
  const somaRemanejamento = somaVariacoesRemanejamento(formRemanejamento.itens);

  if (!contratoId) return null;

  return <section className={estilos.painel} aria-labelledby="titulo-alteracoes-contrato">
    <ModalConfirmacao />
    <header className={estilos.cabecalho}>
      <div><span>Capacidade e fatos negociais</span><h3 id="titulo-alteracoes-contrato">Composição, aditamentos e remanejamentos</h3></div>
      <div className={estilos.referencia}><label>Data de referência<input type="date" value={dataReferencia} onChange={(e) => setDataReferencia(e.target.value)}/></label><button type="button" onClick={carregar} disabled={carregando}><RefreshCw size={15}/>Atualizar</button></div>
    </header>

    <BotoesAbas aba={aba} aoAlterar={setAba}/>
    {carregando && <p className={estilos.carregando}>Atualizando posição contratual...</p>}
    {erroConsulta && <p role="alert" className={estilos.alerta}>{erroConsulta}</p>}
    {erroCatalogos && <p role="alert" className={estilos.alerta}>{erroCatalogos}</p>}

    {aba === 'composicao' && <div role="tabpanel" className={estilos.conteudo}>
      <CartaoAlerta>Capacidade e ocupação vêm do Java na data de referência. Excedente não é reconciliado automaticamente no navegador.</CartaoAlerta>
      <div className={estilos.gradeCartoes}>{composicoes.map((item) => {
        const excedente = excedentes.find((e) => String(e.composicaoId) === String(item.id) || (String(e.lotacaoId) === String(item.lotacaoId) && String(e.jornadaId) === String(item.jornadaId) && String(e.cargoId) === String(item.cargoId)));
        return <article className={estilos.cartao} key={`${item.id || 'proj'}-${item.lotacaoId}-${item.jornadaId}-${item.cargoId}`}>
          <strong>{rotuloComposicao(item)}</strong>
          {possuiMetadadosProva(item) ? <>
            <div><span>Efetivo previsto: </span><CampoComProva dados={item} campo="efetivoPrevisto" /></div>
            <div><small>Início da vigência: </small><CampoComProva dados={item} campo="dataInicioVigencia" /></div>
            <div><small>Fim da vigência: </small><CampoComProva dados={item} campo="dataFimVigencia" /></div>
          </> : <><span>Efetivo previsto: <b>{item.efetivoPrevisto ?? 0}</b></span><small>Vigência: {formatarDataBrasileira(item.dataInicioVigencia)} a {formatarDataBrasileira(item.dataFimVigencia)}</small></>}
          <small>Ocupação: {excedente?.quantidadeVinculadaAtual ?? 0}{excedente?.excedente ? ` · Excedente: ${excedente.quantidadeExcedente}` : ' · Sem excedente'}</small>
          {excedente?.desdeQuandoExcedente && <em>Excedente desde {formatarDataBrasileira(excedente.desdeQuandoExcedente)}</em>}
          {normalizarLista(excedente?.justificativas).map((j) => <small key={`${j.eventoId}-${j.vinculoId}`}>Justificativa: {j.justificativa || j.autorizacao || 'evidência registrada'}</small>)}
          {rascunho && podeGerenciar && <div className={estilos.acoesLinha}><button type="button" onClick={() => editarComposicao(item)}>Editar</button><button type="button" className={estilos.perigoso} onClick={() => excluirComposicao(item)} disabled={processando}><Trash2 size={14}/>Excluir</button></div>}
        </article>;
      })}</div>
      {!carregando && !erroConsulta && composicoes.length === 0 && <p className={estilos.vazio}>Nenhuma composição válida na data selecionada.</p>}

      {rascunho && podeGerenciar && <form className={estilos.formulario} onSubmit={salvarComposicao}>
        <h4>{formComposicao.id ? 'Editar composição do rascunho' : 'Adicionar composição ao rascunho'}</h4>
        <p>Valores atuais de preparação do rascunho. A edição não transforma esses dados em prova histórica publicada.</p>
        <div className={estilos.gradeCampos}>
          <label><span>Lotação *</span><select value={formComposicao.lotacaoId} onChange={(e) => setFormComposicao((f) => ({ ...f, lotacaoId: e.target.value }))} required><option value="">Selecione</option>{lotacoes.map((x) => <option key={x.id} value={x.id}>{x.nome}</option>)}</select></label>
          <label><span>Turno/Jornada *</span><select value={formComposicao.jornadaId} onChange={(e) => setFormComposicao((f) => ({ ...f, jornadaId: e.target.value }))} required><option value="">Selecione</option>{jornadas.map((x) => <option key={x.id} value={x.id}>{x.descricao || x.nome}</option>)}</select></label>
          <label><span>Cargo *</span><select value={formComposicao.cargoId} onChange={(e) => setFormComposicao((f) => ({ ...f, cargoId: e.target.value }))} required><option value="">Selecione</option>{cargos.map((x) => <option key={x.id} value={x.id}>{x.nome}</option>)}</select></label>
          <label><span>Efetivo previsto *</span><input type="number" min="0" value={formComposicao.efetivoPrevisto} onChange={(e) => setFormComposicao((f) => ({ ...f, efetivoPrevisto: e.target.value }))} required/></label>
          <label><span>Início de vigência</span><input type="date" value={formComposicao.dataInicioVigencia} onChange={(e) => setFormComposicao((f) => ({ ...f, dataInicioVigencia: e.target.value }))}/></label>
          <label><span>Fim de vigência</span><input type="date" value={formComposicao.dataFimVigencia} onChange={(e) => setFormComposicao((f) => ({ ...f, dataFimVigencia: e.target.value }))}/></label>
        </div><div className={estilos.acoesFormulario}><button type="button" onClick={() => setFormComposicao({ ...FORM_COMPOSICAO })}><X size={15}/>Limpar</button><button type="submit" disabled={processando}><Save size={15}/>Salvar composição</button></div>
      </form>}
    </div>}

    {aba === 'aditamentos' && <div role="tabpanel" className={estilos.conteudo}>
      {rascunho ? <CartaoAlerta perigo>Publique o contrato antes de registrar aditamentos. Em rascunho, altere a composição diretamente.</CartaoAlerta> : <CartaoAlerta>O piso normal é de 75% do efetivo total originalmente publicado, com redução ordinária máxima de 25%. Aumentos posteriores não alteram essa base. A confirmação usa a última prévia válida, que verifica a redução global e a ocupação por composição.</CartaoAlerta>}
      <div className={estilos.historico}>{aditamentos.map((item) => <article key={item.id}><div><strong>{item.identificacao} · v{item.versaoInstrumento || 1}</strong><span>{formatarDataBrasileira(item.dataEfetiva)} · {rotuloSituacao(item.situacao)}</span><small>{item.justificativa}</small></div>{podeGerenciar && !rascunho && <div className={estilos.acoesLinha}><button type="button" onClick={() => iniciarRetificacaoAditamento(item)}>Retificar</button>{dataFutura(item.dataEfetiva) && <button type="button" className={estilos.perigoso} onClick={() => cancelarProgramacao('aditamento', item)}>Cancelar futuro</button>}</div>}</article>)}</div>
      {!rascunho && podeGerenciar && <div className={estilos.formulario}>
        <h4>{retificandoAditamentoId ? 'Retificação de aditamento' : 'Novo aditamento'}</h4>
        <div className={estilos.gradeCampos}>
          <label><span>Identificação *</span><input value={formAditamento.identificacao} onChange={(e) => atualizarAditamento('identificacao', e.target.value)} disabled={Boolean(retificandoAditamentoId)}/></label>
          <label><span>Data efetiva *</span><input type="date" value={formAditamento.dataEfetiva} onChange={(e) => atualizarAditamento('dataEfetiva', e.target.value)}/></label>
          <label><span>Data do documento</span><input type="date" value={formAditamento.dataDocumento} onChange={(e) => atualizarAditamento('dataDocumento', e.target.value)}/></label>
          <label><span>Data da assinatura</span><input type="date" value={formAditamento.dataAssinatura} onChange={(e) => atualizarAditamento('dataAssinatura', e.target.value)}/></label>
          <label><span>Nova data final da vigência</span><input type="date" value={formAditamento.novaDataFim} onChange={(e) => atualizarAditamento('novaDataFim', e.target.value)}/></label>
          <label><span>Referência SEI</span><input value={formAditamento.referenciaSei} onChange={(e) => atualizarAditamento('referenciaSei', e.target.value)}/></label>
          <label><span>Novo efetivo total</span><input type="number" min="0" value={formAditamento.novoEfetivoTotal} onChange={(e) => atualizarAditamento('novoEfetivoTotal', e.target.value)}/></label>
          <label className={estilos.campoAmplo}><span>Justificativa *</span><textarea value={formAditamento.justificativa} onChange={(e) => atualizarAditamento('justificativa', e.target.value)}/></label>
        </div>
        <div className={estilos.subsecao}><div className={estilos.tituloLinha}><strong>Alterações de composição</strong><button type="button" onClick={adicionarLinhaAditamento}><Plus size={14}/>Adicionar linha</button></div>{formAditamento.alteracoesComposicao.map((linha) => <div className={estilos.linhaComposicao} key={linha.chaveCliente}>
          <select value={linha.composicaoId} onChange={(e) => alterarLinhaAditamento(linha.chaveCliente, 'composicaoId', e.target.value)}><option value="">Nova combinação</option>{composicoes.map((x) => <option key={x.id} value={x.id}>{rotuloComposicao(x)}</option>)}</select>
          {!linha.composicaoId && <><select value={linha.lotacaoId} onChange={(e) => alterarLinhaAditamento(linha.chaveCliente, 'lotacaoId', e.target.value)}><option value="">Lotação</option>{lotacoes.map((x) => <option key={x.id} value={x.id}>{x.nome}</option>)}</select><select value={linha.jornadaId} onChange={(e) => alterarLinhaAditamento(linha.chaveCliente, 'jornadaId', e.target.value)}><option value="">Turno</option>{jornadas.map((x) => <option key={x.id} value={x.id}>{x.descricao || x.nome}</option>)}</select><select value={linha.cargoId} onChange={(e) => alterarLinhaAditamento(linha.chaveCliente, 'cargoId', e.target.value)}><option value="">Cargo</option>{cargos.map((x) => <option key={x.id} value={x.id}>{x.nome}</option>)}</select></>}
          <input type="number" min="0" aria-label="Quantidade resultante" value={linha.quantidadeResultante} onChange={(e) => alterarLinhaAditamento(linha.chaveCliente, 'quantidadeResultante', e.target.value)}/><button type="button" aria-label="Remover linha" onClick={() => removerLinhaAditamento(linha.chaveCliente)}><Trash2 size={14}/></button>
        </div>)}</div>
        {previaAditamento.previa && <div className={estilos.previa}><h5><Calculator size={16}/>Prévia calculada no servidor</h5><div className={estilos.metricas}><span>Efetivo: <b>{previaAditamento.previa.efetivoAnterior} → {previaAditamento.previa.efetivoResultante}</b></span><span>Piso normal (75% da base original): <b>{previaAditamento.previa.pisoNormalReducao}</b></span></div>{normalizarLista(previaAditamento.previa.alertas).map((a) => <CartaoAlerta perigo key={a}>{a}</CartaoAlerta>)}<ListaImpactos impactos={previaAditamento.previa.impactosComposicao}/>{previaAditamento.previa.possuiImpactoOcupacao && <label className={estilos.check}><input type="checkbox" checked={formAditamento.confirmarImpacto} onChange={(e) => atualizarAditamento('confirmarImpacto', e.target.checked)}/>Confirmo que revisei o impacto de ocupação. Nenhuma pessoa será movida automaticamente.</label>}{previaAditamento.previa.exigeAutorizacaoExcepcional && <div className={estilos.excecao}><label className={estilos.check}><input type="checkbox" checked={formAditamento.reducaoExcepcional} onChange={(e) => atualizarAditamento('reducaoExcepcional', e.target.checked)}/>Usar fluxo excepcional para redução acima do limite normal</label><label><span>Fundamento da exceção *</span><textarea value={formAditamento.fundamentoExcecao} onChange={(e) => atualizarAditamento('fundamentoExcecao', e.target.value)}/></label></div>}</div>}
        <div className={estilos.acoesFormulario}><button type="button" onClick={() => { setFormAditamento({ ...FORM_ADITAMENTO }); setRetificandoAditamentoId(null); setPreviaAditamento(criarEstadoPrevia()); }}>Limpar</button><button type="button" onClick={calcularPreviaAditamento} disabled={processando || calculandoPreviaAditamento}><Calculator size={15}/>Calcular prévia</button><button type="button" onClick={confirmarAditamento} disabled={processando || calculandoPreviaAditamento || !previaAditValida}><Save size={15}/>Confirmar com esta prévia</button></div>
      </div>}
    </div>}

    {aba === 'remanejamentos' && <div role="tabpanel" className={estilos.conteudo}>
      {rascunho ? <CartaoAlerta perigo>Remanejamento é fato de contrato publicado. No rascunho, altere a composição diretamente.</CartaoAlerta> : <CartaoAlerta>Remanejamento preserva o efetivo total: pelo menos duas composições e soma das variações igual a zero. O Java revalida ocupação na confirmação.</CartaoAlerta>}
      <div className={estilos.historico}>{remanejamentos.map((item) => <article key={item.id}><div><strong>{item.identificacao} · v{item.versaoInstrumento || 1}</strong><span>{formatarDataBrasileira(item.dataEfetiva)} · soma {item.somaVariacoes ?? 0} · {rotuloSituacao(item.situacao)}</span><small>{item.justificativa}</small></div>{podeGerenciar && !rascunho && <div className={estilos.acoesLinha}><button type="button" onClick={() => iniciarRetificacaoRemanejamento(item)}>Retificar</button>{dataFutura(item.dataEfetiva) && <button type="button" className={estilos.perigoso} onClick={() => cancelarProgramacao('remanejamento', item)}>Cancelar futuro</button>}</div>}</article>)}</div>
      {!rascunho && podeGerenciar && <div className={estilos.formulario}>
        <h4>{retificandoRemanejamentoId ? 'Retificação de remanejamento' : 'Novo remanejamento'}</h4>
        <div className={estilos.gradeCampos}><label><span>Identificação *</span><input value={formRemanejamento.identificacao} onChange={(e) => atualizarRemanejamento('identificacao', e.target.value)} disabled={Boolean(retificandoRemanejamentoId)}/></label><label><span>Data efetiva *</span><input type="date" value={formRemanejamento.dataEfetiva} onChange={(e) => atualizarRemanejamento('dataEfetiva', e.target.value)}/></label><label><span>Referência SEI</span><input value={formRemanejamento.referenciaSei} onChange={(e) => atualizarRemanejamento('referenciaSei', e.target.value)}/></label><label className={estilos.campoAmplo}><span>Justificativa *</span><textarea value={formRemanejamento.justificativa} onChange={(e) => atualizarRemanejamento('justificativa', e.target.value)}/></label></div>
        <div className={estilos.subsecao}><div className={estilos.tituloLinha}><strong>Itens do remanejamento · soma: {somaRemanejamento}</strong><button type="button" onClick={adicionarLinhaRemanejamento}><Plus size={14}/>Adicionar item</button></div>{formRemanejamento.itens.map((linha) => <div className={estilos.linhaRemanejamento} key={linha.chaveCliente}><select value={linha.composicaoId} onChange={(e) => alterarLinhaRemanejamento(linha.chaveCliente, 'composicaoId', e.target.value)}><option value="">Selecione a composição</option>{composicoes.map((x) => <option key={x.id} value={x.id}>{rotuloComposicao(x)}</option>)}</select><input type="number" aria-label="Variação" value={linha.variacao} onChange={(e) => alterarLinhaRemanejamento(linha.chaveCliente, 'variacao', e.target.value)}/><button type="button" aria-label="Remover item" onClick={() => removerLinhaRemanejamento(linha.chaveCliente)}><Trash2 size={14}/></button></div>)}</div>
        {previaRemanejamento.previa && <div className={estilos.previa}><h5><Calculator size={16}/>Prévia calculada no servidor</h5><div className={estilos.metricas}><span>Total: <b>{previaRemanejamento.previa.efetivoTotalAntes} → {previaRemanejamento.previa.efetivoTotalDepois}</b></span><span>Soma das variações: <b>{previaRemanejamento.previa.somaVariacoes}</b></span><span>Preservado: <b>{previaRemanejamento.previa.totalPreservado ? 'Sim' : 'Não'}</b></span></div>{normalizarLista(previaRemanejamento.previa.alertas).map((a) => <CartaoAlerta perigo key={a}>{a}</CartaoAlerta>)}<ListaImpactos impactos={previaRemanejamento.previa.impactosComposicao}/>{previaRemanejamento.previa.possuiImpactoOcupacao && <label className={estilos.check}><input type="checkbox" checked={formRemanejamento.confirmarImpacto} onChange={(e) => atualizarRemanejamento('confirmarImpacto', e.target.checked)}/>Confirmo que revisei o impacto de ocupação. Nenhuma pessoa será movida automaticamente.</label>}</div>}
        <div className={estilos.acoesFormulario}><button type="button" onClick={() => { setFormRemanejamento({ ...FORM_REMANEJAMENTO }); setRetificandoRemanejamentoId(null); setPreviaRemanejamento(criarEstadoPrevia()); }}>Limpar</button><button type="button" onClick={calcularPreviaRemanejamento} disabled={processando || calculandoPreviaRemanejamento}><Calculator size={15}/>Calcular prévia</button><button type="button" onClick={confirmarRemanejamento} disabled={processando || calculandoPreviaRemanejamento || !previaRemValida}><Save size={15}/>Confirmar com esta prévia</button></div>
      </div>}
    </div>}
  </section>;
};
