import React, { useCallback, useEffect, useMemo, useRef, useState } from 'react';
import { Download, FileArchive, FilePlus2, GitBranch, History, KeyRound, Plus, RefreshCw, Search, ShieldCheck, Trash2 } from 'lucide-react';
import { useAutenticacao } from '../../../../../auth/context.jsx';
import { eAdministradorTecnico, eUsuarioOperacional } from '../../../../../../core/security/roles.js';
import { useConfirmacaoSenha } from '../../../../../../core/security/useConfirmacaoSenha.jsx';
import { notificar } from '../../../../../../core/events.js';
import { gerarChaveIdempotencia } from '../../../../../../core/http/idempotencia.js';
import { ServicoExcecoesContrato } from '../../../../../../services/admin/ServicoExcecoesContrato.js';
import { ServicoAlteracoesContrato } from '../../../../../../services/admin/ServicoAlteracoesContrato.js';
import { possuiMetadadosProva, rotuloComposicaoComProva, valorCampoDisponivel } from '../../../../../../services/historico/provaHistoricaLeitura.js';
import { ServicoContrato } from '../../../../../../services/admin/ServicoContrato.js';
import { ServicoUsuario } from '../../../../../../services/admin/ServicoUsuario.js';
import { ModalAcaoContrato, ResumoPreviaContrato } from '../ModalAcaoContrato/index.jsx';
import { CorrecaoCnpjContrato } from './CorrecaoCnpjContrato.jsx';
import {
  CLASSIFICACOES_DOCUMENTO,
  TIPOS_FATO_DOCUMENTAL,
  ehDocumentoRestrito,
  mensagemErroDocumento,
  podeDescartarDocumento,
  prepararCorrecaoAnulacao,
  prepararCorrecaoIdentificador,
  prepararMetadadosDocumento,
  prepararTransferenciaGestao,
  validarCorrecaoIdentificador,
  validarDocumentoLocal,
  validarTransferenciaGestao,
} from '../../../../../../services/admin/ServicoDominioExcecoesContrato.js';
import { formatarDataBrasileira, formatarDataHoraBrasileira } from '../../../../../../services/admin/ServicoDominioContrato.js';
import estilos from './index.module.css';

const hoje = () => {
  const data = new Date();
  return `${data.getFullYear()}-${String(data.getMonth() + 1).padStart(2, '0')}-${String(data.getDate()).padStart(2, '0')}`;
};
const lista = (v) => Array.isArray(v) ? v : [];
const rotulo = (v) => String(v || '').replaceAll('_', ' ');
const FORM_DOC = Object.freeze({ tipoFato:'CONTRATO_ORIGINAL', fatoId:'', fatoChave:'', titulo:'', descricao:'', referenciaSei:'', classificacaoAcesso:'NEGOCIAL', dataEfetiva:'' });
const FORM_CORRECAO = Object.freeze({ novoNumero:'', novoAno:'', dataEfetiva:'', dataAto:'', motivo:'', referenciaSei:'', autorizacaoAdministrativa:'', eventoPrecedenteId:'', relacaoPrecedencia:'', ordemDocumental:'' });
const FORM_TRANSFERENCIA = Object.freeze({ dataEfetiva:'', dataAto:'', gestaoLocalDestinoId:'', tipoContratacaoDestinoId:'', motivo:'', referenciaSei:'', autorizacaoAdministrativa:'', mapeamentos:[], eventoPrecedenteId:'', relacaoPrecedencia:'', ordemDocumental:'', tokenPrevia:null, confirmarImpacto:false });
const FORM_PERMISSAO = Object.freeze({ usuarioId:'', finalidade:'', justificativa:'', autorizacaoAdministrativa:'', validoAte:'' });

const salvarBlob = ({ blob, nomeArquivo }) => {
  if (!blob || typeof URL === 'undefined') return;
  const url = URL.createObjectURL(blob);
  const a = document.createElement('a'); a.href = url; a.download = nomeArquivo || 'documento'; a.style.display='none'; document.body.appendChild(a); a.click(); a.remove();
  setTimeout(() => URL.revokeObjectURL(url), 1000);
};

const Aba = ({ ativa, children, ...props }) => <button type="button" role="tab" aria-selected={ativa} className={ativa ? estilos.abaAtiva : ''} {...props}>{children}</button>;

const Paginas = ({ titulo, total, pagina, aoMudar }) => {
  const paginas = Math.max(1, Math.ceil(total / 5));
  if (paginas === 1) return null;
  return <nav className={estilos.paginacao} aria-label={titulo}><button type="button" disabled={pagina === 0} onClick={() => aoMudar(pagina - 1)}>Anterior</button><span>Página {pagina + 1} de {paginas} · {total} registros</span><button type="button" disabled={pagina + 1 >= paginas} onClick={() => aoMudar(pagina + 1)}>Próxima</button></nav>;
};

const ListaPaginada = ({ children, titulo, elemento: Elemento = 'div', className }) => {
  const registros = React.Children.toArray(children);
  const [pagina, setPagina] = useState(0);
  const ultimaPagina = Math.max(0, Math.ceil(registros.length / 5) - 1);
  const paginaAtual = Math.min(pagina, ultimaPagina);
  useEffect(() => { setPagina((atual) => Math.min(atual, ultimaPagina)); }, [ultimaPagina]);
  return <><Elemento className={className}>{registros.slice(paginaAtual * 5, paginaAtual * 5 + 5)}</Elemento><Paginas titulo={titulo} total={registros.length} pagina={paginaAtual} aoMudar={setPagina} /></>;
};

const MapaHistorico = ({ mapas }) => {
  const [pagina, setPagina] = useState(0);
  return <><ul>{lista(mapas).slice(pagina * 5, pagina * 5 + 5).map((mapa) => <li key={mapa.id || `${mapa.lotacaoOrigemId}-${mapa.jornadaOrigemId}-${mapa.cargoOrigemId}`}>
    Origem #{mapa.composicaoOrigemId ?? 'não informada'}: {mapa.lotacaoOrigemNome ?? 'Lotação não comprovada'} (#{mapa.lotacaoOrigemId}) · {mapa.jornadaOrigemDescricao ?? 'Turno não comprovado'} (#{mapa.jornadaOrigemId}) · {mapa.cargoOrigemNome ?? 'Cargo não comprovado'} (#{mapa.cargoOrigemId})<br />
    Destino: {mapa.lotacaoDestinoNome ?? 'Lotação não comprovada'} (#{mapa.lotacaoDestinoId}) · {mapa.jornadaDestinoDescricao ?? 'Turno não comprovado'} (#{mapa.jornadaDestinoId}) · {mapa.cargoDestinoNome ?? 'Cargo não comprovado'} (#{mapa.cargoDestinoId})
  </li>)}</ul><Paginas titulo="Páginas dos mapas históricos" total={lista(mapas).length} pagina={pagina} aoMudar={setPagina} /></>;
};

const CampoId = ({ rotulo: nome, valor, aoMudar, obrigatorio = true }) => <label><span>{nome}{obrigatorio ? ' *' : ''}</span><input aria-label={nome} type="number" min="1" step="1" value={valor ?? ''} onChange={(e) => aoMudar(e.target.value)} required={obrigatorio} /></label>;
const chaveMapa = () => globalThis.crypto?.randomUUID?.() || `${Date.now()}-${Math.random()}`;
const novoMapa = (origem = {}) => ({ chave: chaveMapa(), composicaoOrigemId: '', lotacaoOrigemId: '', jornadaOrigemId: '', cargoOrigemId: '', lotacaoDestinoId: '', jornadaDestinoId: '', cargoDestinoId: '', ...origem });
const origemComposicao = (item) => ({ composicaoOrigemId: item.id ?? '',
  lotacaoOrigemId: valorCampoDisponivel(item, 'lotacaoId') ?? '',
  jornadaOrigemId: valorCampoDisponivel(item, 'jornadaId') ?? '',
  cargoOrigemId: valorCampoDisponivel(item, 'cargoId') ?? '' });

const FluxoTransferenciaContrato = ({ contrato, transferencias, eventos, consultaTransferencias, podeGerenciar, confirmarComSenha, fecharConfirmacao, aoRecarregar }) => {
  const [form, setForm] = useState({ ...FORM_TRANSFERENCIA });
  const [composicoes, setComposicoes] = useState([]);
  const [carregandoComposicoes, setCarregandoComposicoes] = useState(false);
  const [erroComposicoes, setErroComposicoes] = useState('');
  const [eventosAtuais, setEventosAtuais] = useState(eventos);
  const [erroHistorico, setErroHistorico] = useState('');
  const [pagina, setPagina] = useState(0);
  const [previa, setPrevia] = useState(null);
  const [confirmado, setConfirmado] = useState(false);
  const [consultando, setConsultando] = useState(false);
  const [ocupado, setOcupado] = useState(false);
  const [erro, setErro] = useState('');
  const [resultadoIncerto, setResultadoIncerto] = useState(false);
  const [retificando, setRetificando] = useState(null);
  const [cancelando, setCancelando] = useState(null);
  const [cancelamento, setCancelamento] = useState({ dataAto: hoje(), motivo: '', referenciaSei: '', autorizacaoAdministrativa: '' });
  const pedidoRef = useRef(0);
  const controleRef = useRef(null);
  const envioRef = useRef(false);
  const montadoRef = useRef(true);
  const payload = useMemo(() => prepararTransferenciaGestao({ ...form, tokenPrevia: null, confirmarImpacto: false }), [form]);
  const assinatura = JSON.stringify(payload);
  const previaValida = Boolean(previa?.resposta?.tokenPrevia && previa.assinatura === assinatura);

  const invalidar = () => {
    pedidoRef.current += 1;
    controleRef.current?.abort();
    setPrevia(null); setConfirmado(false); setConsultando(false);
  };

  const recarregarEventos = useCallback(async () => {
    try {
      const resposta = await ServicoContrato.listarEventos(contrato.id);
      if (montadoRef.current) { setEventosAtuais(lista(resposta)); setErroHistorico(''); }
    } catch (falha) {
      if (montadoRef.current && !falha?.canceled) setErroHistorico(falha?.message || 'Não foi possível conferir a situação dos eventos.');
    }
  }, [contrato.id]);

  useEffect(() => {
    montadoRef.current = true;
    recarregarEventos();
    return () => { montadoRef.current = false; pedidoRef.current += 1; controleRef.current?.abort(); };
  }, [recarregarEventos]);
  useEffect(() => { setPagina((atual) => Math.min(atual, Math.max(0, Math.ceil(transferencias.length / 5) - 1))); }, [transferencias.length]);
  useEffect(() => {
    const controlador = new AbortController();
    setComposicoes([]); setErroComposicoes('');
    if (!podeGerenciar || !form.dataEfetiva) { setCarregandoComposicoes(false); return () => controlador.abort(); }
    setCarregandoComposicoes(true);
    ServicoAlteracoesContrato.listarComposicoes(contrato.id, form.dataEfetiva, { signal: controlador.signal })
      .then((resposta) => { if (!controlador.signal.aborted) setComposicoes(lista(resposta)); })
      .catch((falha) => { if (!controlador.signal.aborted && !falha?.canceled) setErroComposicoes(falha?.message || 'Não foi possível consultar as composições na data.'); })
      .finally(() => { if (!controlador.signal.aborted) setCarregandoComposicoes(false); });
    return () => controlador.abort();
  }, [contrato.id, form.dataEfetiva, podeGerenciar]);

  const editar = (alteracao) => {
    if (envioRef.current) return;
    invalidar(); setErro('');
    setForm((anterior) => typeof alteracao === 'function' ? alteracao(anterior) : { ...anterior, ...alteracao });
  };
  const alterarCampo = (campo, valor) => editar((anterior) => ({ ...anterior, [campo]: valor,
    ...((campo === 'dataEfetiva' && !retificando) || campo === 'gestaoLocalDestinoId' ? { mapeamentos: [] } : {}),
    ...(campo === 'gestaoLocalDestinoId' ? { tipoContratacaoDestinoId: '' } : {}),
  }));
  const alterarMapa = (chave, campo, valor) => editar((anterior) => ({ ...anterior, mapeamentos: anterior.mapeamentos.map((m) => m.chave === chave ? { ...m, [campo]: valor } : m) }));

  const gerarPrevia = async () => {
    if (ocupado || consultando || resultadoIncerto) return;
    const validacao = validarTransferenciaGestao(form);
    if (validacao) { setErro(validacao); return; }
    invalidar(); setErro(''); setConsultando(true);
    const pedido = pedidoRef.current;
    const controlador = new AbortController(); controleRef.current = controlador;
    try {
      const resposta = await ServicoExcecoesContrato.previaTransferenciaGestao(contrato.id, payload, { signal: controlador.signal });
      if (pedido !== pedidoRef.current || controlador.signal.aborted) return;
      if (!resposta?.tokenPrevia) throw new Error('A prévia retornou sem token de confirmação. Consulte novamente.');
      setPrevia({ resposta, assinatura });
    } catch (falha) {
      if (pedido === pedidoRef.current && !falha?.canceled && !controlador.signal.aborted) setErro(falha?.message || 'Não foi possível gerar a prévia.');
    } finally { if (pedido === pedidoRef.current) setConsultando(false); }
  };

  const falhou = (falha) => {
    if (falha?.senhaConfirmacaoInvalida || falha?.code === 'INVALID_CONFIRMATION_PASSWORD') return;
    invalidar(); setErro(falha?.message || 'Operação não confirmada.');
    if (falha?.resultadoIncerto) setResultadoIncerto(true);
    fecharConfirmacao();
  };
  const transferir = async (e) => {
    e.preventDefault();
    if (envioRef.current || !previaValida || !confirmado || resultadoIncerto || !podeGerenciar) return;
    const validacao = validarTransferenciaGestao(form);
    if (validacao) { setErro(validacao); return; }
    const intencao = { ...payload, tokenPrevia: previa.resposta.tokenPrevia, confirmarImpacto: true };
    envioRef.current = true; setOcupado(true);
    try {
      const confirmou = await confirmarComSenha({ titulo: retificando ? 'Confirmar retificação da transferência' : 'Confirmar transferência de Gestão Local', mensagem: 'Os destinos, os mapas e os impactos revisados serão revalidados antes da gravação.', acao: async (senhaConfirmacao) => {
        try {
          await ServicoExcecoesContrato.transferirGestao(contrato.id, intencao, { senhaConfirmacao });
          if (montadoRef.current) { invalidar(); setForm({ ...FORM_TRANSFERENCIA }); setRetificando(null); }
          await Promise.all([aoRecarregar(), recarregarEventos()]);
        } catch (falha) { if (montadoRef.current) falhou(falha); throw falha; }
      } });
      if (confirmou === false && montadoRef.current) invalidar();
    } catch (falha) { if (montadoRef.current && !falha?.canceled) setErro(falha?.message || 'A transferência não foi confirmada.'); }
    finally { envioRef.current = false; if (montadoRef.current) setOcupado(false); }
  };

  const retificar = (item) => {
    if (envioRef.current) return;
    invalidar(); setErro(''); setRetificando(item.id); setCancelando(null);
    setForm({ ...FORM_TRANSFERENCIA, dataEfetiva: item.dataEfetiva, dataAto: hoje(), gestaoLocalDestinoId: item.gestaoDestinoId,
      tipoContratacaoDestinoId: item.tipoContratacaoDestinoId ?? '', eventoPrecedenteId: item.eventoContratoId,
      relacaoPrecedencia: 'RETIFICA', mapeamentos: lista(item.mapeamentos).map((mapa) => novoMapa({
        composicaoOrigemId: mapa.composicaoOrigemId ?? '', lotacaoOrigemId: mapa.lotacaoOrigemId ?? '', jornadaOrigemId: mapa.jornadaOrigemId ?? '', cargoOrigemId: mapa.cargoOrigemId ?? '',
        lotacaoDestinoId: mapa.lotacaoDestinoId ?? '', jornadaDestinoId: mapa.jornadaDestinoId ?? '', cargoDestinoId: mapa.cargoDestinoId ?? '',
      })),
    });
  };

  const cancelar = async (e) => {
    e.preventDefault();
    if (envioRef.current || !cancelando || !podeGerenciar || resultadoIncerto) return;
    if (![cancelamento.motivo, cancelamento.referenciaSei, cancelamento.autorizacaoAdministrativa].every((valor) => valor.trim())) { setErro('Informe motivo, SEI e autorização do cancelamento.'); return; }
    const dados = { dataEfetiva: cancelando.dataEfetiva, dataAto: cancelamento.dataAto || null, motivo: cancelamento.motivo.trim(), referenciaSei: cancelamento.referenciaSei.trim(), autorizacaoAdministrativa: cancelamento.autorizacaoAdministrativa.trim() };
    const transferenciaId = cancelando.id;
    envioRef.current = true; setOcupado(true);
    try {
      await confirmarComSenha({ titulo: 'Cancelar transferência futura', mensagem: 'O efeito da transferência será retirado na data programada original. O ato e seus mapas continuam no histórico.', acao: async (senhaConfirmacao) => {
        try {
          await ServicoExcecoesContrato.cancelarTransferenciaGestao(contrato.id, transferenciaId, dados, { senhaConfirmacao });
          if (montadoRef.current) setCancelando(null);
          await Promise.all([aoRecarregar(), recarregarEventos()]);
        } catch (falha) { if (montadoRef.current) falhou(falha); throw falha; }
      } });
    } catch (falha) { if (montadoRef.current && !falha?.canceled) setErro(falha?.message || 'Cancelamento não confirmado.'); }
    finally { envioRef.current = false; if (montadoRef.current) setOcupado(false); }
  };

  return <div className={estilos.conteudo}>
    <p className={estilos.aviso}>A transferência preserva vínculos e fotografias anteriores. Cada recorte de origem precisa de um destino documentado e de confirmação dos impactos.</p>
    <div className={estilos.acoes}><button type="button" disabled={ocupado} onClick={() => Promise.all([aoRecarregar(), recarregarEventos()])}><RefreshCw size={15} />Atualizar histórico</button></div>
    {erroHistorico && <p role="alert" className={estilos.erro}>{erroHistorico}</p>}
    {consultaTransferencias?.status === 'carregando' && <p role="status">Consultando histórico de transferências…</p>}
    {consultaTransferencias?.status === 'erro' && <p role="alert" className={estilos.erro}>{consultaTransferencias.mensagem}</p>}
    <div className={estilos.lista}>{consultaTransferencias?.status === 'sucesso' && transferencias.length === 0 && <p>Nenhuma transferência retornada para o contexto consultado.</p>}{transferencias.slice(pagina * 5, pagina * 5 + 5).map((item) => {
      const fato = lista(eventosAtuais).find((e) => Number(e.id) === Number(item.eventoContratoId));
      const vigente = fato?.situacaoFato === 'VIGENTE' && !erroHistorico;
      return <article key={item.id} className={estilos.transferenciaHistorica}><div><strong>{item.gestaoOrigemNome ?? 'Origem não comprovada'} → {item.gestaoDestinoNome ?? 'Destino não comprovado'}</strong>
        <span>{formatarDataBrasileira(item.dataEfetiva)} · {item.motivo}</span><small>Evento #{item.eventoContratoId} · {fato ? rotulo(fato.situacaoFato) : 'Situação ainda não consultada'} · registrado em {formatarDataHoraBrasileira(item.registradoEm)}</small>
        <small>Tipo de contratação: {item.tipoContratacaoDestinoDescricao ?? 'Descrição não comprovada'} (#{item.tipoContratacaoDestinoId ?? 'não informado'}) · SEI {item.referenciaSei}</small>
        <details><summary>{lista(item.mapeamentos).length} mapa(s) preservado(s)</summary><MapaHistorico mapas={item.mapeamentos} /></details>
      </div>{podeGerenciar && vigente && <div className={estilos.acoes}><button type="button" disabled={ocupado} onClick={() => retificar(item)}>Retificar transferência</button>{item.dataEfetiva > hoje() && <button type="button" disabled={ocupado} onClick={() => { invalidar(); setErro(''); setCancelando(item); setCancelamento({ dataAto: hoje(), motivo: '', referenciaSei: '', autorizacaoAdministrativa: '' }); }}>Cancelar transferência futura</button>}</div>}</article>;
    })}</div><Paginas titulo="Páginas do histórico de transferências" total={transferencias.length} pagina={pagina} aoMudar={setPagina} />
    {erro && <p role="alert" className={estilos.erro}>{erro}</p>}
    {resultadoIncerto && <label className={estilos.aviso}><input type="checkbox" onChange={(e) => e.target.checked && setResultadoIncerto(false)} /> Consulte o histórico e marque após conferir o resultado antes de reenviar.</label>}
    {podeGerenciar && cancelando && <form aria-label="Cancelamento de transferência" className={estilos.formulario} onSubmit={cancelar}>
      <h4>Cancelar transferência #{cancelando.id}</h4><p>Data programada preservada: {formatarDataBrasileira(cancelando.dataEfetiva)}.</p>
      <fieldset className={estilos.campos} disabled={ocupado}><div className={estilos.grade}>
        <label><span>Data do ato de cancelamento</span><input type="date" value={cancelamento.dataAto} onChange={(e) => setCancelamento((atual) => ({ ...atual, dataAto: e.target.value }))} /></label>
        <label><span>SEI do cancelamento *</span><input value={cancelamento.referenciaSei} onChange={(e) => setCancelamento((atual) => ({ ...atual, referenciaSei: e.target.value }))} required /></label>
        <label><span>Autorização do cancelamento *</span><input value={cancelamento.autorizacaoAdministrativa} onChange={(e) => setCancelamento((atual) => ({ ...atual, autorizacaoAdministrativa: e.target.value }))} required /></label>
        <label className={estilos.amplo}><span>Motivo do cancelamento *</span><textarea value={cancelamento.motivo} onChange={(e) => setCancelamento((atual) => ({ ...atual, motivo: e.target.value }))} required /></label>
      </div></fieldset><div className={estilos.acoes}><button type="button" disabled={ocupado} onClick={() => setCancelando(null)}>Voltar sem cancelar</button><button type="submit" disabled={ocupado || resultadoIncerto}>Confirmar cancelamento</button></div>
    </form>}
    {podeGerenciar && !cancelando && <form aria-label="Transferência de gestão" className={estilos.formulario} onSubmit={transferir}>
      <h4>{retificando ? `Retificar transferência #${retificando}` : 'Nova transferência de gestão'}</h4>
      <p className={estilos.aviso}>Os catálogos de outra Gestão Local podem não aparecer no seu escopo de consulta. Informe os IDs constantes da autorização; a prévia verifica os destinos, a disponibilidade temporal e os mapas antes da confirmação.</p>
      <fieldset className={estilos.campos} disabled={ocupado}><div className={estilos.grade}>
        <label><span>Data efetiva da transferência *</span><input type="date" value={form.dataEfetiva} onChange={(e) => alterarCampo('dataEfetiva', e.target.value)} required /></label>
        <label><span>Data do ato de transferência</span><input type="date" value={form.dataAto} onChange={(e) => alterarCampo('dataAto', e.target.value)} /></label>
        <CampoId rotulo="Gestão destino (ID)" valor={form.gestaoLocalDestinoId} aoMudar={(v) => alterarCampo('gestaoLocalDestinoId', v)} />
        <CampoId rotulo="Tipo de contratação destino (ID)" valor={form.tipoContratacaoDestinoId} aoMudar={(v) => alterarCampo('tipoContratacaoDestinoId', v)} />
        <label><span>SEI da transferência *</span><input value={form.referenciaSei} onChange={(e) => alterarCampo('referenciaSei', e.target.value)} required /></label>
        <label><span>Autorização administrativa *</span><input value={form.autorizacaoAdministrativa} onChange={(e) => alterarCampo('autorizacaoAdministrativa', e.target.value)} required /></label>
        <label className={estilos.amplo}><span>Motivo da transferência *</span><textarea value={form.motivo} onChange={(e) => alterarCampo('motivo', e.target.value)} required /></label>
        <CampoId rotulo="Evento precedente da transferência (ID)" obrigatorio={false} valor={form.eventoPrecedenteId} aoMudar={(v) => alterarCampo('eventoPrecedenteId', v)} />
        <label><span>Relação de precedência da transferência</span><select value={form.relacaoPrecedencia} onChange={(e) => alterarCampo('relacaoPrecedencia', e.target.value)}><option value="">Sem precedência</option>{['SUCEDE', 'RETIFICA', 'SUBSTITUI', 'MANTEM_COEXISTE'].map((relacao) => <option key={relacao} value={relacao}>{relacao.replaceAll('_', ' ')}</option>)}</select></label>
        <label><span>Ordem documental da transferência</span><input type="number" min="0" step="1" value={form.ordemDocumental} onChange={(e) => alterarCampo('ordemDocumental', e.target.value)} /></label>
      </div>
      <div className={estilos.mapeamentos}><div><strong>Mapas por recorte de origem</strong><button type="button" onClick={() => editar((anterior) => ({ ...anterior, mapeamentos: [...anterior.mapeamentos, novoMapa()] }))}><Plus size={14} />Adicionar mapa</button></div>
        {carregandoComposicoes && <p>Consultando composições na data efetiva…</p>}
        {erroComposicoes && <p role="alert" className={estilos.erro}>{erroComposicoes}</p>}
        {!carregandoComposicoes && form.dataEfetiva && <p>{composicoes.length} composição(ões) retornada(s) para esta data, conforme o conhecimento atual. Os mapas da retificação conservam a origem do ato selecionado.</p>}
        {form.mapeamentos.map((mapa, indice) => <fieldset key={mapa.chave} className={estilos.blocoMapa}><legend>Mapa {indice + 1}</legend>
          <label className={estilos.escolhaComposicao}><span>Selecionar origem consultada na data</span><select aria-label={`Composição disponível na data do mapa ${indice + 1}`} value="" onChange={(e) => { const origem = composicoes[Number(e.target.value)]; if (!origem) return; editar((anterior) => ({ ...anterior, mapeamentos: anterior.mapeamentos.map((m) => m.chave === mapa.chave ? { ...m, ...origemComposicao(origem) } : m) })); }}><option value="">Preencher IDs a partir de uma composição</option>{composicoes.map((item, i) => <option key={`${item.id}-${i}`} value={i}>{possuiMetadadosProva(item) ? rotuloComposicaoComProva(item) : `#${item.id ?? 'sem ID base'} · ${item.lotacaoNome ?? 'Lotação não comprovada'} · ${item.jornadaDescricao ?? 'Turno não comprovado'} · ${item.cargoNome ?? 'Cargo não comprovado'}`}</option>)}</select></label>
          <div className={estilos.gradeMapas}>{[['composicaoOrigemId', 'Composição origem'], ['lotacaoOrigemId', 'Lotação origem'], ['jornadaOrigemId', 'Turno origem'], ['cargoOrigemId', 'Cargo origem'], ['lotacaoDestinoId', 'Lotação destino'], ['jornadaDestinoId', 'Turno destino'], ['cargoDestinoId', 'Cargo destino']].map(([campo, nome]) => <CampoId key={campo} rotulo={`${nome} do mapa ${indice + 1}`} obrigatorio={campo !== 'composicaoOrigemId'} valor={mapa[campo]} aoMudar={(v) => alterarMapa(mapa.chave, campo, v)} />)}</div>
          <button type="button" aria-label={`Remover mapa ${indice + 1}`} onClick={() => editar((anterior) => ({ ...anterior, mapeamentos: anterior.mapeamentos.filter((m) => m.chave !== mapa.chave) }))}><Trash2 size={14} />Remover mapa</button>
        </fieldset>)}
      </div></fieldset>
      {previaValida && <><ResumoPreviaContrato previa={previa.resposta} /><label className={estilos.confirmacaoImpacto}><input type="checkbox" disabled={ocupado} checked={confirmado} onChange={(e) => setConfirmado(e.target.checked)} /> Confirmo os impactos e os mapas desta transferência.</label></>}
      <div className={estilos.acoes}>{retificando && <button type="button" disabled={ocupado} onClick={() => { invalidar(); setRetificando(null); setForm({ ...FORM_TRANSFERENCIA }); }}>Voltar para nova transferência</button>}
        <button type="button" disabled={ocupado || consultando || resultadoIncerto} onClick={gerarPrevia}>{consultando ? 'Consultando prévia…' : 'Gerar prévia da transferência'}</button>
        <button type="submit" disabled={ocupado || !previaValida || !confirmado || resultadoIncerto}>{ocupado ? 'Aguardando confirmação…' : 'Confirmar transferência'}</button>
      </div>
    </form>}
  </div>;
};

export const PainelExcecoesContrato = ({ contrato, eventos = [], podeGerenciar = false, aoAtualizarContrato }) => {
  const contratoId = contrato?.id;
  const { user } = useAutenticacao();
  const administrador = eAdministradorTecnico(user);
  const operacional = eUsuarioOperacional(user);
  const podeVerDocumentos = !operacional;
  const { confirmarComSenha: solicitarConfirmacaoSenha, ModalConfirmacao, fechar: fecharConfirmacao } = useConfirmacaoSenha();
  const [aba, setAba] = useState('documentos');
  const [processando, setProcessando] = useState(false);
  const [documentos, setDocumentos] = useState([]);
  const [finalidade, setFinalidade] = useState('');
  const [formDoc, setFormDoc] = useState({ ...FORM_DOC });
  const [arquivo, setArquivo] = useState(null);
  const [progresso, setProgresso] = useState(0);
  const [substituindoId, setSubstituindoId] = useState(null);
  const [arquivoSubstituicao, setArquivoSubstituicao] = useState(null);
  const [formSubstituicao, setFormSubstituicao] = useState({ titulo:'', descricao:'', referenciaSei:'', motivo:'' });
  const [documentoSelecionado, setDocumentoSelecionado] = useState(null);
  const [eventosDocumento, setEventosDocumento] = useState([]);
  const [permissoes, setPermissoes] = useState([]);
  const [acessos, setAcessos] = useState([]);
  const [consultaDocumento, setConsultaDocumento] = useState({ status: 'inicial', erros: [] });
  const [usuarios, setUsuarios] = useState([]);
  const [formPermissao, setFormPermissao] = useState({ ...FORM_PERMISSAO });
  const [transferencias, setTransferencias] = useState([]);
  const [correcoes, setCorrecoes] = useState([]);
  const [formCorrecao, setFormCorrecao] = useState({ ...FORM_CORRECAO });
  const [buscaHistorico, setBuscaHistorico] = useState({ numero:'', ano:'' });
  const [historicoIdentificador, setHistoricoIdentificador] = useState([]);
  const [eventoAnulacao, setEventoAnulacao] = useState(null);
  const [eventosCorrigidos, setEventosCorrigidos] = useState(null);
  const [consultas, setConsultas] = useState({ transferencias: { status: 'inicial' }, correcoes: { status: 'inicial' }, documentos: { status: 'inicial' } });
  const leituraRef = useRef(0);
  const leituraDocumentoRef = useRef({ geracao: 0, controlador: null });
  const confirmacaoRef = useRef(false);
  const intencoesRef = useRef(new Map());

  const limparDocumento = useCallback(() => {
    const anterior = leituraDocumentoRef.current;
    anterior.controlador?.abort();
    leituraDocumentoRef.current = { geracao: anterior.geracao + 1, controlador: null };
    setDocumentoSelecionado(null);
    setEventosDocumento([]);
    setPermissoes([]);
    setAcessos([]);
    setFormPermissao({ ...FORM_PERMISSAO });
    setConsultaDocumento({ status: 'inicial', erros: [] });
  }, []);

  useEffect(() => {
    limparDocumento();
    return () => {
      const anterior = leituraDocumentoRef.current;
      anterior.controlador?.abort();
      leituraDocumentoRef.current = { geracao: anterior.geracao + 1, controlador: null };
    };
  }, [contratoId, finalidade, administrador, podeGerenciar, podeVerDocumentos, user?.id, limparDocumento]);

  const prepararIntencao = (escopo, dados, anexo = null) => {
    const assinatura = JSON.stringify(dados);
    let intencao = intencoesRef.current.get(escopo);
    if (!intencao || intencao.assinatura !== assinatura || intencao.anexo !== anexo) {
      intencao = { assinatura, anexo, chave: gerarChaveIdempotencia(escopo) };
      intencoesRef.current.set(escopo, intencao);
    }
    return { ...dados, chaveIdempotencia: intencao.chave };
  };

  const confirmarComSenha = async (configuracao) => {
    if (confirmacaoRef.current) return false;
    confirmacaoRef.current = true;
    setProcessando(true);
    try { return await solicitarConfirmacaoSenha(configuracao); }
    finally { confirmacaoRef.current = false; setProcessando(false); }
  };


  const carregar = useCallback(async () => {
    limparDocumento();
    if (!contratoId) return;
    const pedido = ++leituraRef.current;
    const tarefas = [
      ['transferencias', () => ServicoExcecoesContrato.listarTransferencias(contratoId), setTransferencias],
      ['correcoes', () => ServicoExcecoesContrato.listarCorrecoesIdentificador(contratoId), setCorrecoes],
      ...(podeVerDocumentos ? [['documentos', () => ServicoExcecoesContrato.listarDocumentos(contratoId, finalidade || undefined), setDocumentos]] : []),
    ];
    setConsultas(Object.fromEntries(tarefas.map(([nome]) => [nome, { status: 'carregando' }])));
    tarefas.forEach(([, , atualizar]) => atualizar([]));
    const respostas = await Promise.allSettled(tarefas.map(([, consultar]) => consultar()));
    if (pedido !== leituraRef.current) return;
    const estados = {};
    respostas.forEach((resposta, indice) => {
      const [nome, , atualizar] = tarefas[indice];
      if (resposta.status === 'fulfilled') {
        atualizar(lista(resposta.value));
        estados[nome] = { status: 'sucesso' };
      } else {
        estados[nome] = { status: 'erro', mensagem: resposta.reason?.message || `Não foi possível consultar ${nome}.` };
      }
    });
    setConsultas(estados);
  }, [contratoId, finalidade, podeVerDocumentos, limparDocumento]);

  useEffect(() => { carregar(); return () => { leituraRef.current += 1; }; }, [carregar]);
  useEffect(() => { intencoesRef.current.clear(); }, [contratoId]);
  useEffect(() => {
    setEventosCorrigidos(null);
    setEventoAnulacao(null);
  }, [contratoId, eventos]);
  useEffect(() => {
    if (!administrador) { setUsuarios([]); return undefined; }
    const controlador = new AbortController();
    ServicoUsuario.listarTodos({ signal: controlador.signal })
      .then((resposta) => { if (!controlador.signal.aborted) setUsuarios(lista(resposta)); })
      .catch((erro) => { if (!controlador.signal.aborted && !erro?.canceled) notificar('error', erro.message || 'Não foi possível consultar os usuários.'); });
    return () => controlador.abort();
  }, [administrador]);

  const recarregarDocumento = async (doc) => {
    limparDocumento();
    if (!doc?.id || !contratoId || !podeVerDocumentos) return;
    const controlador = new AbortController();
    const consulta = { geracao: leituraDocumentoRef.current.geracao, controlador, contratoId, documentoId: doc.id, finalidade };
    leituraDocumentoRef.current = consulta;
    const aindaAtual = () => leituraDocumentoRef.current === consulta && !controlador.signal.aborted;
    const options = { signal: controlador.signal };
    setConsultaDocumento({ status: 'carregando', erros: [] });
    try {
      const [det, ev] = await Promise.all([
        ServicoExcecoesContrato.buscarDocumento(contratoId, doc.id, finalidade || undefined, options),
        ServicoExcecoesContrato.listarEventosDocumento(contratoId, doc.id, finalidade || undefined, options),
      ]);
      if (!aindaAtual()) return;
      setDocumentoSelecionado(det);
      setEventosDocumento(lista(ev));
      if ((administrador || podeGerenciar) && ehDocumentoRestrito(det)) {
        setConsultaDocumento({ status: 'acessos', erros: [] });
        const respostas = await Promise.allSettled([
          ServicoExcecoesContrato.listarPermissoesDocumento(contratoId, doc.id, options),
          ServicoExcecoesContrato.listarAcessosDocumento(contratoId, doc.id, options),
        ]);
        if (!aindaAtual()) return;
        const erros = [];
        respostas.forEach((resposta, indice) => {
          if (resposta.status === 'fulfilled') {
            (indice === 0 ? setPermissoes : setAcessos)(lista(resposta.value));
          } else {
            const nome = indice === 0 ? 'Permissões documentais' : 'Registros de acesso';
            erros.push(`${nome}: ${resposta.reason?.message || 'Não foi possível consultar os dados.'}`);
          }
        });
        setConsultaDocumento({ status: erros.length ? 'erro' : 'sucesso', erros });
      } else {
        setConsultaDocumento({ status: 'sucesso', erros: [] });
      }
    } catch (erro) {
      if (!aindaAtual() || erro?.canceled) return;
      const mensagem = mensagemErroDocumento(erro);
      setConsultaDocumento({ status: 'erro', erros: [mensagem] });
      notificar('error', mensagem);
    }
  };

  const enviarDocumento = async (e) => {
    e.preventDefault();
    if (confirmacaoRef.current) return;
    const erroLocal = validarDocumentoLocal(formDoc, arquivo);
    if (erroLocal) return notificar('warning', erroLocal);
    const payload = prepararIntencao('doc', prepararMetadadosDocumento(formDoc), arquivo);
    const anexo = arquivo;
    await confirmarComSenha({ titulo: 'Registrar documento comprobatório', mensagem: 'O documento será versionado e auditado pelo servidor.', acao: async (senhaConfirmacao) => {
      setProgresso(0);
      try {
        await ServicoExcecoesContrato.criarDocumento(contratoId, payload, anexo, { senhaConfirmacao, onUploadProgress: (evt) => setProgresso(evt.total ? Math.round(evt.loaded * 100 / evt.total) : 0) });
        intencoesRef.current.delete('doc');
        setFormDoc({ ...FORM_DOC }); setArquivo(null); setProgresso(0); await carregar();
      } catch (erro) { notificar('error', mensagemErroDocumento(erro)); }
    } });
  };

  const baixar = async (doc) => {
    if (ehDocumentoRestrito(doc) && !finalidade.trim()) return notificar('warning','Informe a finalidade antes de acessar documento Pessoal/Restrito.');
    try { salvarBlob(await ServicoExcecoesContrato.baixarDocumento(contratoId, doc.id, finalidade || undefined)); }
    catch (erro) { notificar('error', mensagemErroDocumento(erro)); }
  };

  const acaoDoc = async (tipo, doc) => {
    const motivo = window.prompt(tipo === 'retificar' ? 'Motivo da retificação SEI:' : tipo === 'substituir' ? 'Motivo da substituição:' : 'Motivo da invalidação/descarte:'); if (motivo == null) return;
    let extra = {};
    if (tipo === 'retificar') { const novaReferenciaSei = window.prompt('Nova referência SEI:'); if (!novaReferenciaSei) return; extra = prepararIntencao(`sei-${doc.id}`, { novaReferenciaSei, motivo }); }
    await confirmarComSenha({ titulo:'Confirmar ação documental', mensagem:'A operação preservará a cadeia de versões e eventos.', acao: async (senhaConfirmacao) => {
      setProcessando(true); try {
        if (tipo === 'retificar') { await ServicoExcecoesContrato.retificarSei(contratoId, doc.id, extra, { senhaConfirmacao }); intencoesRef.current.delete(`sei-${doc.id}`); }
        if (tipo === 'invalidar') await ServicoExcecoesContrato.invalidarDocumento(contratoId, doc.id, { motivo, autorizacaoAdministrativa: window.prompt('Autorização administrativa:') || '' }, { senhaConfirmacao });
        if (tipo === 'descartar') await ServicoExcecoesContrato.descartarRascunhoDocumento(contratoId, doc.id, { senhaConfirmacao });
        await carregar(); setDocumentoSelecionado(null);
      } catch (erro) { notificar('error', mensagemErroDocumento(erro)); } finally { setProcessando(false); }
    }});
  };

  const concederPermissao = async (e) => {
    e.preventDefault(); if (!documentoSelecionado?.id) return;
    const consulta = leituraDocumentoRef.current;
    const payload = { ...formPermissao, usuarioId:Number(formPermissao.usuarioId), validoAte:formPermissao.validoAte ? new Date(formPermissao.validoAte).toISOString() : null };
    await confirmarComSenha({ titulo:'Conceder acesso restrito', mensagem:'A concessão será auditada e vinculada à finalidade informada.', acao: async (senhaConfirmacao)=>{
      try {
        await ServicoExcecoesContrato.concederPermissaoDocumento(contratoId, documentoSelecionado.id, payload, { senhaConfirmacao });
        if (leituraDocumentoRef.current === consulta) await recarregarDocumento(documentoSelecionado);
      } catch (erro) { notificar('error', erro.message); }
    }});
  };

  const revogarPermissao = async (p) => {
    const consulta = leituraDocumentoRef.current;
    const motivo = window.prompt('Motivo da revogação:'); const autorizacaoAdministrativa = window.prompt('Autorização administrativa:'); if (!motivo || !autorizacaoAdministrativa) return;
    await confirmarComSenha({ titulo:'Revogar acesso restrito', mensagem:'A revogação será preservada no histórico.', acao: async (senhaConfirmacao)=>{
      await ServicoExcecoesContrato.revogarPermissaoDocumento(contratoId, documentoSelecionado.id, p.id, { motivo, autorizacaoAdministrativa }, { senhaConfirmacao });
      if (leituraDocumentoRef.current === consulta) await recarregarDocumento(documentoSelecionado);
    }});
  };

  const corrigirIdentificador = async (e) => {
    e.preventDefault();
    if (confirmacaoRef.current) return;
    const erro = validarCorrecaoIdentificador(formCorrecao);
    if (erro) return notificar('warning', erro);
    const payload = prepararIntencao('corr-id', prepararCorrecaoIdentificador(formCorrecao));
    await confirmarComSenha({ titulo: 'Correção excepcional do identificador', mensagem: 'O número/ano anterior permanecerá pesquisável e a correção será auditada.', acao: async (senhaConfirmacao) => {
      try {
        await ServicoExcecoesContrato.corrigirIdentificador(contratoId, payload, { senhaConfirmacao });
        intencoesRef.current.delete('corr-id');
        setFormCorrecao({ ...FORM_CORRECAO }); await carregar();
      } catch (erro) { notificar('error', erro.message); }
    } });
  };

  const confirmarCorrecaoAnulacao = (dados, aoErro) => confirmarComSenha({
    titulo: 'Corrigir anulação',
    mensagem: 'A correção cria um ato CANCELA. O contrato permanece administrativamente inativo até reativação explícita.',
    acao: async (senhaConfirmacao) => {
      setProcessando(true);
      try {
        await ServicoExcecoesContrato.corrigirAnulacao(contratoId, prepararCorrecaoAnulacao(dados), { senhaConfirmacao });
        setEventoAnulacao(null);
        await carregar();
        try { setEventosCorrigidos(lista(await ServicoContrato.listarEventos(contratoId))); }
        catch (erro) { notificar('error', erro.message || 'Correção registrada; não foi possível atualizar o histórico de eventos.'); }
      } catch (erro) {
        if (aoErro?.(erro)) fecharConfirmacao();
        throw erro;
      } finally { setProcessando(false); }
    },
  });

  const pesquisarHistorico = async (e) => { e.preventDefault(); if(!buscaHistorico.numero || !buscaHistorico.ano) return; setHistoricoIdentificador(await ServicoExcecoesContrato.pesquisarHistoricoIdentificador(buscaHistorico.numero,Number(buscaHistorico.ano))); };
  const anulacoes = useMemo(() => lista(eventosCorrigidos ?? eventos).filter((e) => e.tipoEvento === 'ANULACAO'), [eventosCorrigidos, eventos]);

  return <section className={estilos.painel}>
    <div className={estilos.abas} role="tablist" aria-label="Documentos e ações excepcionais">
      {podeVerDocumentos && <Aba ativa={aba==='documentos'} onClick={()=>setAba('documentos')}><FileArchive size={16}/>Documentos</Aba>}
      <Aba ativa={aba==='transferencias'} onClick={()=>setAba('transferencias')}><GitBranch size={16}/>Transferências</Aba>
      <Aba ativa={aba==='correcoes'} onClick={()=>setAba('correcoes')}><History size={16}/>Correções</Aba>
      <Aba ativa={aba==='cnpj'} onClick={()=>setAba('cnpj')}><History size={16}/>Correções CNPJ</Aba>
    </div>

    {aba==='documentos' && podeVerDocumentos && <div className={estilos.conteudo}>
      <div className={estilos.barra}><label><span>Finalidade para PESSOAL_RESTRITO</span><input disabled={processando} value={finalidade} onChange={(e)=>{limparDocumento();setFinalidade(e.target.value);}} placeholder="Obrigatória para acesso restrito"/></label><button type="button" onClick={carregar}><RefreshCw size={15}/>Recarregar</button></div>
      {consultas.documentos?.status === 'carregando' && <p role="status">Consultando documentos…</p>}{consultas.documentos?.status === 'erro' && <p role="alert" className={estilos.erro}>{consultas.documentos.mensagem}</p>}
      {consultaDocumento.status === 'carregando' && <p role="status">Consultando cadeia documental…</p>}
      {consultaDocumento.status === 'acessos' && <p role="status">Consultando permissões e acessos do documento selecionado…</p>}
      {consultaDocumento.erros.length > 0 && <div role="alert" className={estilos.erro}>{consultaDocumento.erros.map((mensagem, indice) => <p key={indice}>{mensagem}</p>)}</div>}
      <ListaPaginada key={contratoId} titulo="Páginas de documentos" className={estilos.lista}>{consultas.documentos?.status === 'sucesso' && documentos.length===0 && <p>Nenhum documento visível para o contexto/finalidade atual.</p>}{documentos.map((d)=><article key={d.id} className={documentoSelecionado?.id===d.id ? estilos.selecionado : ''}><div><strong>{d.titulo}</strong><span>{rotulo(d.estado)} · {rotulo(d.classificacaoAcesso)} · v{d.versaoDocumento}</span><small>{d.referenciaSei ? `SEI ${d.referenciaSei}` : d.nomeOriginalArquivo || 'Sem arquivo'} · {formatarDataBrasileira(d.dataEfetiva)}</small></div><div className={estilos.acoes}><button type="button" onClick={()=>recarregarDocumento(d)}>Detalhes</button>{d.downloadDisponivel && <button type="button" onClick={()=>baixar(d)}><Download size={14}/>Baixar</button>}{podeGerenciar && <>{['PUBLICADO','RASCUNHO'].includes(d.estado) && <button type="button" onClick={()=>acaoDoc('retificar',d)}>Retificar SEI</button>}{['PUBLICADO','RASCUNHO'].includes(d.estado) && <button type="button" onClick={()=>{setSubstituindoId(d.id);setFormSubstituicao({titulo:d.titulo||'',descricao:d.descricao||'',referenciaSei:d.referenciaSei||'',motivo:''});setArquivoSubstituicao(null);}}>Nova versão/anexo</button>}{d.estado==='PUBLICADO' && <button type="button" onClick={()=>acaoDoc('invalidar',d)}>Invalidar</button>}{podeDescartarDocumento(d) && <button type="button" className={estilos.perigoso} onClick={()=>acaoDoc('descartar',d)}><Trash2 size={14}/>Descartar</button>}</>}</div></article>)}</ListaPaginada>
      {podeGerenciar && <form className={estilos.formulario} onSubmit={enviarDocumento}><h4><FilePlus2 size={16}/>Registrar documento</h4><div className={estilos.grade}><label><span>Tipo de fato *</span><select disabled={processando} value={formDoc.tipoFato} onChange={(e)=>setFormDoc({...formDoc,tipoFato:e.target.value})}>{TIPOS_FATO_DOCUMENTAL.map(x=><option key={x}>{x}</option>)}</select></label><label><span>Classificação *</span><select disabled={processando} value={formDoc.classificacaoAcesso} onChange={(e)=>setFormDoc({...formDoc,classificacaoAcesso:e.target.value})}>{CLASSIFICACOES_DOCUMENTO.map(x=><option key={x}>{x}</option>)}</select></label>{formDoc.tipoFato!=='CONTRATO_ORIGINAL'&&<><label><span>ID do fato</span><input disabled={processando} type="number" min="1" value={formDoc.fatoId} onChange={(e)=>setFormDoc({...formDoc,fatoId:e.target.value})}/></label><label><span>Chave do fato futuro</span><input disabled={processando} value={formDoc.fatoChave} onChange={(e)=>setFormDoc({...formDoc,fatoChave:e.target.value})} placeholder="Obrigatória quando o fato ainda não possui ID"/></label></>}<label><span>Título *</span><input disabled={processando} value={formDoc.titulo} onChange={(e)=>setFormDoc({...formDoc,titulo:e.target.value})}/></label><label><span>Data efetiva</span><input disabled={processando} type="date" value={formDoc.dataEfetiva} onChange={(e)=>setFormDoc({...formDoc,dataEfetiva:e.target.value})}/></label><label><span>Referência SEI</span><input disabled={processando} value={formDoc.referenciaSei} onChange={(e)=>setFormDoc({...formDoc,referenciaSei:e.target.value})}/></label><label><span>Arquivo opcional</span><input disabled={processando} type="file" accept=".pdf,.png,.jpg,.jpeg,.doc,.docx,.xls,.xlsx" onChange={(e)=>setArquivo(e.target.files?.[0]||null)}/></label><label className={estilos.amplo}><span>Descrição</span><textarea disabled={processando} value={formDoc.descricao} onChange={(e)=>setFormDoc({...formDoc,descricao:e.target.value})}/></label></div>{progresso>0 && <progress max="100" value={progresso}>{progresso}%</progress>}<button type="submit" disabled={processando}>Registrar com confirmação</button></form>}
      {substituindoId && podeGerenciar && <form className={estilos.formulario} onSubmit={(e)=>{e.preventDefault(); if(confirmacaoRef.current) return; const atual=documentos.find(d=>d.id===substituindoId); if(!arquivoSubstituicao && !formSubstituicao.referenciaSei.trim()) return notificar('warning','A nova versão precisa de anexo, referência SEI ou ambos.'); const dadosSubstituicao=prepararIntencao(`doc-${substituindoId}`,{...formSubstituicao,titulo:formSubstituicao.titulo||atual?.titulo},arquivoSubstituicao); confirmarComSenha({titulo:'Registrar nova versão documental',mensagem:'A versão anterior será preservada como SUBSTITUIDO quando aplicável.',acao:async(senhaConfirmacao)=>{setProcessando(true);try{await ServicoExcecoesContrato.substituirDocumento(contratoId,substituindoId,dadosSubstituicao,arquivoSubstituicao,{senhaConfirmacao,onUploadProgress:(evt)=>setProgresso(evt.total?Math.round(evt.loaded*100/evt.total):0)});intencoesRef.current.delete(`doc-${substituindoId}`);setSubstituindoId(null);setArquivoSubstituicao(null);setFormSubstituicao({titulo:'',descricao:'',referenciaSei:'',motivo:''});await carregar();}catch(err){notificar('error',mensagemErroDocumento(err));}finally{setProcessando(false);}}});}}><h4>Nova versão / acrescentar anexo</h4><div className={estilos.grade}><label><span>Título</span><input disabled={processando} value={formSubstituicao.titulo} onChange={(e)=>setFormSubstituicao({...formSubstituicao,titulo:e.target.value})}/></label><label><span>Referência SEI</span><input disabled={processando} value={formSubstituicao.referenciaSei} onChange={(e)=>setFormSubstituicao({...formSubstituicao,referenciaSei:e.target.value})}/></label><label><span>Novo arquivo opcional</span><input disabled={processando} type="file" accept=".pdf,.png,.jpg,.jpeg,.doc,.docx,.xls,.xlsx" onChange={(e)=>setArquivoSubstituicao(e.target.files?.[0]||null)}/></label><label className={estilos.amplo}><span>Motivo *</span><textarea disabled={processando} required value={formSubstituicao.motivo} onChange={(e)=>setFormSubstituicao({...formSubstituicao,motivo:e.target.value})}/></label></div><div className={estilos.acoes}><button type="button" onClick={()=>setSubstituindoId(null)}>Cancelar</button><button type="submit" disabled={processando}>Registrar nova versão</button></div></form>}
      {documentoSelecionado && <div className={estilos.detalhe}><h4>{documentoSelecionado.titulo} · cadeia documental</h4><ListaPaginada key={`eventos-${documentoSelecionado.id}`} titulo="Páginas dos eventos documentais" elemento="ul">{eventosDocumento.map(ev=><li key={ev.id}><strong>{rotulo(ev.tipoEvento)}</strong> · {formatarDataHoraBrasileira(ev.registradoEm)} · {ev.motivo || 'sem motivo'}</li>)}</ListaPaginada>{(administrador||podeGerenciar) && ehDocumentoRestrito(documentoSelecionado) && <><h5>Permissões e acessos</h5><ListaPaginada key={`permissoes-${documentoSelecionado.id}`} titulo="Páginas das permissões documentais" elemento="ul">{permissoes.map(p=><li key={p.id}>{p.usuarioBeneficiarioUsername} · {p.finalidade} · {p.ativo?'ativa':'revogada'} {administrador&&p.ativo&&<button type="button" onClick={()=>revogarPermissao(p)}>Revogar</button>}</li>)}</ListaPaginada><ListaPaginada key={`acessos-${documentoSelecionado.id}`} titulo="Páginas dos acessos documentais" elemento="ul">{acessos.map(a=><li key={a.id}>{a.usuarioUsername} · {rotulo(a.tipoAcesso)} · {a.finalidade} · {formatarDataHoraBrasileira(a.acessadoEm)}</li>)}</ListaPaginada>{administrador&&<form className={estilos.grade} onSubmit={concederPermissao}><label><span>Usuário *</span><select disabled={processando} value={formPermissao.usuarioId} onChange={(e)=>setFormPermissao({...formPermissao,usuarioId:e.target.value})}><option value="">Selecione</option>{usuarios.map(u=><option key={u.id} value={u.id}>{u.username||u.nome||u.matricula}</option>)}</select></label><label><span>Finalidade *</span><input disabled={processando} value={formPermissao.finalidade} onChange={(e)=>setFormPermissao({...formPermissao,finalidade:e.target.value})}/></label><label><span>Justificativa *</span><input disabled={processando} value={formPermissao.justificativa} onChange={(e)=>setFormPermissao({...formPermissao,justificativa:e.target.value})}/></label><label><span>Autorização *</span><input disabled={processando} value={formPermissao.autorizacaoAdministrativa} onChange={(e)=>setFormPermissao({...formPermissao,autorizacaoAdministrativa:e.target.value})}/></label><label><span>Válido até</span><input disabled={processando} type="datetime-local" value={formPermissao.validoAte} onChange={(e)=>setFormPermissao({...formPermissao,validoAte:e.target.value})}/></label><button type="submit"><KeyRound size={14}/>Conceder</button></form>}</>}</div>}
    </div>}

    {aba === 'transferencias' && <FluxoTransferenciaContrato key={contratoId} contrato={contrato} transferencias={transferencias} eventos={eventos} consultaTransferencias={consultas.transferencias} podeGerenciar={podeGerenciar} confirmarComSenha={confirmarComSenha} fecharConfirmacao={fecharConfirmacao} aoRecarregar={carregar} />}

    {aba==='correcoes' && <div className={estilos.conteudo}>{consultas.correcoes?.status === 'carregando' && <p role="status">Consultando correções…</p>}{consultas.correcoes?.status === 'erro' && <p role="alert" className={estilos.erro}>{consultas.correcoes.mensagem}</p>}<div className={estilos.duasColunas}><section><h4>Correções de identificador</h4><ListaPaginada key={`correcoes-${contratoId}`} titulo="Páginas das correções de identificador" elemento="ul">{correcoes.map(c=><li key={c.id}>{c.numeroAnterior}/{c.anoAnterior} → <b>{c.numeroNovo}/{c.anoNovo}</b> · {formatarDataBrasileira(c.dataEfetiva)}</li>)}</ListaPaginada>{podeGerenciar&&<form className={estilos.formulario} onSubmit={corrigirIdentificador}><div className={estilos.grade}><label><span>Novo número *</span><input disabled={processando} value={formCorrecao.novoNumero} onChange={(e)=>setFormCorrecao({...formCorrecao,novoNumero:e.target.value})}/></label><label><span>Novo ano *</span><input disabled={processando} type="number" min="1900" max="9999" value={formCorrecao.novoAno} onChange={(e)=>setFormCorrecao({...formCorrecao,novoAno:e.target.value})}/></label><label><span>Data efetiva *</span><input disabled={processando} type="date" value={formCorrecao.dataEfetiva} onChange={(e)=>setFormCorrecao({...formCorrecao,dataEfetiva:e.target.value})}/></label><label><span>Data do ato *</span><input disabled={processando} type="date" value={formCorrecao.dataAto} onChange={(e)=>setFormCorrecao({...formCorrecao,dataAto:e.target.value})}/></label><label><span>SEI *</span><input disabled={processando} value={formCorrecao.referenciaSei} onChange={(e)=>setFormCorrecao({...formCorrecao,referenciaSei:e.target.value})}/></label><label><span>Autorização *</span><input disabled={processando} value={formCorrecao.autorizacaoAdministrativa} onChange={(e)=>setFormCorrecao({...formCorrecao,autorizacaoAdministrativa:e.target.value})}/></label><label className={estilos.amplo}><span>Motivo *</span><textarea disabled={processando} value={formCorrecao.motivo} onChange={(e)=>setFormCorrecao({...formCorrecao,motivo:e.target.value})}/></label></div><button type="submit">Corrigir preservando histórico</button></form>}</section><section><h4>Pesquisar identificador antigo</h4><form className={estilos.busca} onSubmit={pesquisarHistorico}><input disabled={processando} placeholder="Número" value={buscaHistorico.numero} onChange={(e)=>setBuscaHistorico({...buscaHistorico,numero:e.target.value})}/><input disabled={processando} type="number" placeholder="Ano" value={buscaHistorico.ano} onChange={(e)=>setBuscaHistorico({...buscaHistorico,ano:e.target.value})}/><button><Search size={14}/>Pesquisar</button></form><ListaPaginada key={`pesquisa-${contratoId}`} titulo="Páginas da pesquisa de identificador" elemento="ul">{historicoIdentificador.map(c=><li key={c.id}>{c.numeroAnterior}/{c.anoAnterior} → {c.numeroNovo}/{c.anoNovo}</li>)}</ListaPaginada><h4>Correção de anulação</h4>{anulacoes.length===0?<p>Nenhum evento de anulação disponível.</p>:<ListaPaginada key={`anulacoes-${contratoId}`} titulo="Páginas das anulações">{anulacoes.map(e=><article key={e.id}><span>Anulação em {formatarDataBrasileira(e.dataEfetiva)}</span>{podeGerenciar&&contrato?.estadoNegocial==='ANULADO'&&e.situacaoFato==='VIGENTE'&&<button type="button" onClick={()=>setEventoAnulacao(e)}><ShieldCheck size={14}/>Registrar CANCELA</button>}</article>)}</ListaPaginada>}</section></div></div>}
    {aba === 'cnpj' && <CorrecaoCnpjContrato key={contratoId} contrato={contrato} podeGerenciar={podeGerenciar} confirmarComSenha={confirmarComSenha} fecharConfirmacao={fecharConfirmacao} aoAtualizar={carregar} aoAtualizarContrato={aoAtualizarContrato} />}
    <ModalAcaoContrato aberto={Boolean(eventoAnulacao)} tipoAcao="CORRIGIR_ANULACAO" contrato={contrato}
      dadosIniciais={eventoAnulacao ? { dataEfetiva: eventoAnulacao.dataEfetiva, eventoPrecedenteId: eventoAnulacao.id, relacaoPrecedencia: 'CANCELA' } : undefined}
      processando={processando} aoFechar={() => setEventoAnulacao(null)} aoConfirmar={confirmarCorrecaoAnulacao} />
    <ModalConfirmacao />
  </section>;
};
