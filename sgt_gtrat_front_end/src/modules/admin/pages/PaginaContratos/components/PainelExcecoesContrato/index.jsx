import React, { useCallback, useEffect, useMemo, useState } from 'react';
import { Download, FileArchive, FilePlus2, GitBranch, History, KeyRound, Plus, RefreshCw, Search, ShieldCheck, Trash2 } from 'lucide-react';
import { useAutenticacao } from '../../../../../auth/context.jsx';
import { eAdministradorTecnico, eUsuarioOperacional } from '../../../../../../core/security/roles.js';
import { useConfirmacaoSenha } from '../../../../../../core/security/useConfirmacaoSenha.jsx';
import { notificar } from '../../../../../../core/events.js';
import { ServicoExcecoesContrato } from '../../../../../../services/admin/ServicoExcecoesContrato.js';
import { ServicoAlteracoesContrato } from '../../../../../../services/admin/ServicoAlteracoesContrato.js';
import { ServicoOrganizacional } from '../../../../../../services/admin/ServicoOrganizacional.js';
import { ServicoLotacao } from '../../../../../../services/admin/ServicoLotacao.js';
import { ServicoJornada } from '../../../../../../services/admin/ServicoJornada.js';
import { ServicoRecursoGenerico } from '../../../../../../services/admin/ServicoRecursoGenerico.js';
import { ServicoUsuario } from '../../../../../../services/admin/ServicoUsuario.js';
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

const ServicoCargo = ServicoRecursoGenerico('/cargos');
const hoje = () => new Date().toISOString().slice(0, 10);
const lista = (v) => Array.isArray(v) ? v : [];
const rotulo = (v) => String(v || '').replaceAll('_', ' ');
const FORM_DOC = Object.freeze({ tipoFato:'CONTRATO_ORIGINAL', fatoId:'', fatoChave:'', titulo:'', descricao:'', referenciaSei:'', classificacaoAcesso:'NEGOCIAL', dataEfetiva:'' });
const FORM_CORRECAO = Object.freeze({ novoNumero:'', novoAno:'', dataEfetiva:'', dataAto:'', motivo:'', referenciaSei:'', autorizacaoAdministrativa:'', eventoPrecedenteId:'', relacaoPrecedencia:'', ordemDocumental:'' });
const FORM_TRANSFERENCIA = Object.freeze({ dataEfetiva:'', dataAto:'', gestaoLocalDestinoId:'', motivo:'', referenciaSei:'', autorizacaoAdministrativa:'', mapeamentos:[], eventoPrecedenteId:'', relacaoPrecedencia:'', ordemDocumental:'' });
const FORM_PERMISSAO = Object.freeze({ usuarioId:'', finalidade:'', justificativa:'', autorizacaoAdministrativa:'', validoAte:'' });

const salvarBlob = ({ blob, nomeArquivo }) => {
  if (!blob || typeof URL === 'undefined') return;
  const url = URL.createObjectURL(blob);
  const a = document.createElement('a'); a.href = url; a.download = nomeArquivo || 'documento'; a.style.display='none'; document.body.appendChild(a); a.click(); a.remove();
  setTimeout(() => URL.revokeObjectURL(url), 1000);
};

const Aba = ({ ativa, children, ...props }) => <button type="button" role="tab" aria-selected={ativa} className={ativa ? estilos.abaAtiva : ''} {...props}>{children}</button>;

export const PainelExcecoesContrato = ({ contrato, eventos = [], podeGerenciar = false }) => {
  const contratoId = contrato?.id;
  const { user } = useAutenticacao();
  const administrador = eAdministradorTecnico(user);
  const operacional = eUsuarioOperacional(user);
  const podeVerDocumentos = !operacional;
  const { confirmarComSenha, ModalConfirmacao } = useConfirmacaoSenha();
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
  const [usuarios, setUsuarios] = useState([]);
  const [formPermissao, setFormPermissao] = useState({ ...FORM_PERMISSAO });
  const [transferencias, setTransferencias] = useState([]);
  const [correcoes, setCorrecoes] = useState([]);
  const [formCorrecao, setFormCorrecao] = useState({ ...FORM_CORRECAO });
  const [buscaHistorico, setBuscaHistorico] = useState({ numero:'', ano:'' });
  const [historicoIdentificador, setHistoricoIdentificador] = useState([]);
  const [formTransferencia, setFormTransferencia] = useState({ ...FORM_TRANSFERENCIA });
  const [gestoes, setGestoes] = useState([]);
  const [composicoes, setComposicoes] = useState([]);
  const [lotacoes, setLotacoes] = useState([]);
  const [jornadas, setJornadas] = useState([]);
  const [cargos, setCargos] = useState([]);

  const carregar = useCallback(async () => {
    if (!contratoId) return;
    const tarefas = [ServicoExcecoesContrato.listarTransferencias(contratoId), ServicoExcecoesContrato.listarCorrecoesIdentificador(contratoId)];
    if (podeVerDocumentos) tarefas.push(ServicoExcecoesContrato.listarDocumentos(contratoId, finalidade || undefined));
    try {
      const [t, c, d = []] = await Promise.all(tarefas);
      setTransferencias(lista(t)); setCorrecoes(lista(c)); if (podeVerDocumentos) setDocumentos(lista(d));
    } catch (erro) { if (!erro?.canceled) notificar('error', erro?.message || 'Não foi possível carregar documentos e exceções.'); }
  }, [contratoId, finalidade, podeVerDocumentos]);

  useEffect(() => { carregar(); }, [carregar]);
  useEffect(() => {
    if (!contratoId || (!podeGerenciar && !administrador)) return;
    Promise.all([
      ServicoOrganizacional.listarGestoesLocais(),
      ServicoAlteracoesContrato.listarComposicoes(contratoId, hoje()),
      ServicoLotacao.listarTodos({ size:500 }), ServicoJornada.listarTodos(), ServicoCargo.listarTodos(),
      administrador ? ServicoUsuario.listarTodos() : Promise.resolve([]),
    ]).then(([g, co, l, j, c, u]) => { setGestoes(lista(g)); setComposicoes(lista(co)); setLotacoes(lista(l)); setJornadas(lista(j)); setCargos(lista(c)); setUsuarios(lista(u)); }).catch(()=>{});
  }, [contratoId, podeGerenciar, administrador]);

  const recarregarDocumento = async (doc) => {
    if (!doc) return;
    try {
      const [det, ev] = await Promise.all([
        ServicoExcecoesContrato.buscarDocumento(contratoId, doc.id, finalidade || undefined),
        ServicoExcecoesContrato.listarEventosDocumento(contratoId, doc.id, finalidade || undefined),
      ]);
      setDocumentoSelecionado(det); setEventosDocumento(lista(ev));
      if (administrador || podeGerenciar) {
        const [p, a] = await Promise.all([
          ServicoExcecoesContrato.listarPermissoesDocumento(contratoId, doc.id).catch(()=>[]),
          ServicoExcecoesContrato.listarAcessosDocumento(contratoId, doc.id).catch(()=>[]),
        ]); setPermissoes(lista(p)); setAcessos(lista(a));
      }
    } catch (erro) { notificar('error', mensagemErroDocumento(erro)); }
  };

  const enviarDocumento = async (e) => {
    e.preventDefault(); const erroLocal = validarDocumentoLocal(formDoc, arquivo); if (erroLocal) return notificar('warning', erroLocal);
    await confirmarComSenha({ titulo:'Registrar documento comprobatório', mensagem:'O documento será versionado e auditado pelo servidor.', acao: async (senhaConfirmacao) => {
      setProcessando(true); setProgresso(0);
      try {
        await ServicoExcecoesContrato.criarDocumento(contratoId, prepararMetadadosDocumento(formDoc), arquivo, { senhaConfirmacao, onUploadProgress:(evt)=>setProgresso(evt.total ? Math.round(evt.loaded*100/evt.total) : 0) });
        setFormDoc({ ...FORM_DOC }); setArquivo(null); setProgresso(0); await carregar();
      } catch (erro) { notificar('error', mensagemErroDocumento(erro)); } finally { setProcessando(false); }
    }});
  };

  const baixar = async (doc) => {
    if (ehDocumentoRestrito(doc) && !finalidade.trim()) return notificar('warning','Informe a finalidade antes de acessar documento Pessoal/Restrito.');
    try { salvarBlob(await ServicoExcecoesContrato.baixarDocumento(contratoId, doc.id, finalidade || undefined)); }
    catch (erro) { notificar('error', mensagemErroDocumento(erro)); }
  };

  const acaoDoc = async (tipo, doc) => {
    const motivo = window.prompt(tipo === 'retificar' ? 'Motivo da retificação SEI:' : tipo === 'substituir' ? 'Motivo da substituição:' : 'Motivo da invalidação/descarte:'); if (motivo == null) return;
    let extra = {};
    if (tipo === 'retificar') { const novaReferenciaSei = window.prompt('Nova referência SEI:'); if (!novaReferenciaSei) return; extra = { novaReferenciaSei, motivo }; }
    await confirmarComSenha({ titulo:'Confirmar ação documental', mensagem:'A operação preservará a cadeia de versões e eventos.', acao: async (senhaConfirmacao) => {
      setProcessando(true); try {
        if (tipo === 'retificar') await ServicoExcecoesContrato.retificarSei(contratoId, doc.id, extra, { senhaConfirmacao });
        if (tipo === 'invalidar') await ServicoExcecoesContrato.invalidarDocumento(contratoId, doc.id, { motivo, autorizacaoAdministrativa: window.prompt('Autorização administrativa:') || '' }, { senhaConfirmacao });
        if (tipo === 'descartar') await ServicoExcecoesContrato.descartarRascunhoDocumento(contratoId, doc.id, { senhaConfirmacao });
        await carregar(); setDocumentoSelecionado(null);
      } catch (erro) { notificar('error', mensagemErroDocumento(erro)); } finally { setProcessando(false); }
    }});
  };

  const concederPermissao = async (e) => {
    e.preventDefault(); if (!documentoSelecionado?.id) return;
    const payload = { ...formPermissao, usuarioId:Number(formPermissao.usuarioId), validoAte:formPermissao.validoAte ? new Date(formPermissao.validoAte).toISOString() : null };
    await confirmarComSenha({ titulo:'Conceder acesso restrito', mensagem:'A concessão será auditada e vinculada à finalidade informada.', acao: async (senhaConfirmacao)=>{
      try { await ServicoExcecoesContrato.concederPermissaoDocumento(contratoId, documentoSelecionado.id, payload, { senhaConfirmacao }); setFormPermissao({ ...FORM_PERMISSAO }); await recarregarDocumento(documentoSelecionado); } catch (erro) { notificar('error', erro.message); }
    }});
  };

  const revogarPermissao = async (p) => {
    const motivo = window.prompt('Motivo da revogação:'); const autorizacaoAdministrativa = window.prompt('Autorização administrativa:'); if (!motivo || !autorizacaoAdministrativa) return;
    await confirmarComSenha({ titulo:'Revogar acesso restrito', mensagem:'A revogação será preservada no histórico.', acao: async (senhaConfirmacao)=>{
      await ServicoExcecoesContrato.revogarPermissaoDocumento(contratoId, documentoSelecionado.id, p.id, { motivo, autorizacaoAdministrativa }, { senhaConfirmacao }); await recarregarDocumento(documentoSelecionado);
    }});
  };

  const adicionarMapeamento = () => setFormTransferencia((f)=>({ ...f, mapeamentos:[...f.mapeamentos,{ chave:crypto.randomUUID?.() || `${Date.now()}-${Math.random()}`, composicaoOrigemId:'', lotacaoOrigemId:'', jornadaOrigemId:'', cargoOrigemId:'', lotacaoDestinoId:'', jornadaDestinoId:'', cargoDestinoId:'' }] }));
  const alterarMap = (chave,campo,valor) => setFormTransferencia((f)=>({ ...f, mapeamentos:f.mapeamentos.map((m)=>m.chave===chave ? { ...m,[campo]:valor } : m) }));
  const removerMap = (chave) => setFormTransferencia((f)=>({ ...f, mapeamentos:f.mapeamentos.filter((m)=>m.chave!==chave) }));

  const transferir = async (e) => {
    e.preventDefault(); const erro = validarTransferenciaGestao(formTransferencia); if (erro) return notificar('warning',erro);
    const payload = prepararTransferenciaGestao(formTransferencia);
    await confirmarComSenha({ titulo:'Transferência excepcional de Gestão Local', mensagem:'Revise todos os mapeamentos. A operação é atômica, auditável e depende de autorização excepcional vigente no Java.', acao: async (senhaConfirmacao)=>{
      setProcessando(true); try { await ServicoExcecoesContrato.transferirGestao(contratoId,payload,{ senhaConfirmacao }); setFormTransferencia({ ...FORM_TRANSFERENCIA }); await carregar(); } catch(err){ notificar('error',err.message); } finally { setProcessando(false); }
    }});
  };

  const corrigirIdentificador = async (e) => {
    e.preventDefault(); const erro = validarCorrecaoIdentificador(formCorrecao); if (erro) return notificar('warning',erro);
    await confirmarComSenha({ titulo:'Correção excepcional do identificador', mensagem:'O número/ano anterior permanecerá pesquisável e a correção será auditada.', acao:async(senhaConfirmacao)=>{
      try { await ServicoExcecoesContrato.corrigirIdentificador(contratoId,prepararCorrecaoIdentificador(formCorrecao),{ senhaConfirmacao }); setFormCorrecao({ ...FORM_CORRECAO }); await carregar(); } catch(err){ notificar('error',err.message); }
    }});
  };

  const corrigirAnulacao = async (evento) => {
    const motivo = window.prompt('Motivo da correção da anulação:'); const referenciaSei = window.prompt('Referência documental/SEI:'); if (!motivo || !referenciaSei) return;
    const dataAto = window.prompt('Data do ato (AAAA-MM-DD):', hoje()) || hoje();
    const payload = prepararCorrecaoAnulacao({ dataEfetiva:evento.dataEfetiva, dataAto, motivo, referenciaSei, eventoPrecedenteId:evento.id, ordemDocumental:'' });
    await confirmarComSenha({ titulo:'Corrigir anulação', mensagem:'O evento de anulação não será apagado. Será criado evento CANCELA e o contrato continuará administrativamente inativo até reativação explícita.', acao:async(senhaConfirmacao)=>{ await ServicoExcecoesContrato.corrigirAnulacao(contratoId,payload,{ senhaConfirmacao }); await carregar(); } });
  };

  const pesquisarHistorico = async (e) => { e.preventDefault(); if(!buscaHistorico.numero || !buscaHistorico.ano) return; setHistoricoIdentificador(await ServicoExcecoesContrato.pesquisarHistoricoIdentificador(buscaHistorico.numero,Number(buscaHistorico.ano))); };
  const anulacoes = useMemo(()=>lista(eventos).filter((e)=>e.tipoEvento==='ANULACAO'),[eventos]);
  const destinoId = Number(formTransferencia.gestaoLocalDestinoId || 0);
  const filtrarDestino = (itens) => itens.filter((x)=>!destinoId || Number(x.gestaoLocalId || x.gestaoLocal?.id || 0)===destinoId);

  return <section className={estilos.painel}>
    <div className={estilos.abas} role="tablist" aria-label="Documentos e ações excepcionais">
      {podeVerDocumentos && <Aba ativa={aba==='documentos'} onClick={()=>setAba('documentos')}><FileArchive size={16}/>Documentos</Aba>}
      <Aba ativa={aba==='transferencias'} onClick={()=>setAba('transferencias')}><GitBranch size={16}/>Transferências</Aba>
      <Aba ativa={aba==='correcoes'} onClick={()=>setAba('correcoes')}><History size={16}/>Correções</Aba>
    </div>

    {aba==='documentos' && podeVerDocumentos && <div className={estilos.conteudo}>
      <div className={estilos.barra}><label><span>Finalidade para PESSOAL_RESTRITO</span><input value={finalidade} onChange={(e)=>setFinalidade(e.target.value)} placeholder="Obrigatória para acesso restrito"/></label><button type="button" onClick={carregar}><RefreshCw size={15}/>Recarregar</button></div>
      <div className={estilos.lista}>{documentos.length===0 && <p>Nenhum documento visível para o contexto/finalidade atual.</p>}{documentos.map((d)=><article key={d.id} className={documentoSelecionado?.id===d.id ? estilos.selecionado : ''}><div><strong>{d.titulo}</strong><span>{rotulo(d.estado)} · {rotulo(d.classificacaoAcesso)} · v{d.versaoDocumento}</span><small>{d.referenciaSei ? `SEI ${d.referenciaSei}` : d.nomeOriginalArquivo || 'Sem arquivo'} · {formatarDataBrasileira(d.dataEfetiva)}</small></div><div className={estilos.acoes}><button type="button" onClick={()=>recarregarDocumento(d)}>Detalhes</button>{d.downloadDisponivel && <button type="button" onClick={()=>baixar(d)}><Download size={14}/>Baixar</button>}{podeGerenciar && <>{['PUBLICADO','RASCUNHO'].includes(d.estado) && <button type="button" onClick={()=>acaoDoc('retificar',d)}>Retificar SEI</button>}{['PUBLICADO','RASCUNHO'].includes(d.estado) && <button type="button" onClick={()=>{setSubstituindoId(d.id);setFormSubstituicao({titulo:d.titulo||'',descricao:d.descricao||'',referenciaSei:d.referenciaSei||'',motivo:''});setArquivoSubstituicao(null);}}>Nova versão/anexo</button>}{d.estado==='PUBLICADO' && <button type="button" onClick={()=>acaoDoc('invalidar',d)}>Invalidar</button>}{podeDescartarDocumento(d) && <button type="button" className={estilos.perigoso} onClick={()=>acaoDoc('descartar',d)}><Trash2 size={14}/>Descartar</button>}</>}</div></article>)}</div>
      {podeGerenciar && <form className={estilos.formulario} onSubmit={enviarDocumento}><h4><FilePlus2 size={16}/>Registrar documento</h4><div className={estilos.grade}><label><span>Tipo de fato *</span><select value={formDoc.tipoFato} onChange={(e)=>setFormDoc({...formDoc,tipoFato:e.target.value})}>{TIPOS_FATO_DOCUMENTAL.map(x=><option key={x}>{x}</option>)}</select></label><label><span>Classificação *</span><select value={formDoc.classificacaoAcesso} onChange={(e)=>setFormDoc({...formDoc,classificacaoAcesso:e.target.value})}>{CLASSIFICACOES_DOCUMENTO.map(x=><option key={x}>{x}</option>)}</select></label>{formDoc.tipoFato!=='CONTRATO_ORIGINAL'&&<><label><span>ID do fato</span><input type="number" min="1" value={formDoc.fatoId} onChange={(e)=>setFormDoc({...formDoc,fatoId:e.target.value})}/></label><label><span>Chave do fato futuro</span><input value={formDoc.fatoChave} onChange={(e)=>setFormDoc({...formDoc,fatoChave:e.target.value})} placeholder="Obrigatória quando o fato ainda não possui ID"/></label></>}<label><span>Título *</span><input value={formDoc.titulo} onChange={(e)=>setFormDoc({...formDoc,titulo:e.target.value})}/></label><label><span>Data efetiva</span><input type="date" value={formDoc.dataEfetiva} onChange={(e)=>setFormDoc({...formDoc,dataEfetiva:e.target.value})}/></label><label><span>Referência SEI</span><input value={formDoc.referenciaSei} onChange={(e)=>setFormDoc({...formDoc,referenciaSei:e.target.value})}/></label><label><span>Arquivo opcional</span><input type="file" accept=".pdf,.png,.jpg,.jpeg,.doc,.docx,.xls,.xlsx" onChange={(e)=>setArquivo(e.target.files?.[0]||null)}/></label><label className={estilos.amplo}><span>Descrição</span><textarea value={formDoc.descricao} onChange={(e)=>setFormDoc({...formDoc,descricao:e.target.value})}/></label></div>{progresso>0 && <progress max="100" value={progresso}>{progresso}%</progress>}<button type="submit" disabled={processando}>Registrar com confirmação</button></form>}
      {substituindoId && podeGerenciar && <form className={estilos.formulario} onSubmit={(e)=>{e.preventDefault(); const atual=documentos.find(d=>d.id===substituindoId); if(!arquivoSubstituicao && !formSubstituicao.referenciaSei.trim()) return notificar('warning','A nova versão precisa de anexo, referência SEI ou ambos.'); confirmarComSenha({titulo:'Registrar nova versão documental',mensagem:'A versão anterior será preservada como SUBSTITUIDO quando aplicável.',acao:async(senhaConfirmacao)=>{setProcessando(true);try{await ServicoExcecoesContrato.substituirDocumento(contratoId,substituindoId,{...formSubstituicao,titulo:formSubstituicao.titulo||atual?.titulo},arquivoSubstituicao,{senhaConfirmacao,onUploadProgress:(evt)=>setProgresso(evt.total?Math.round(evt.loaded*100/evt.total):0)});setSubstituindoId(null);setArquivoSubstituicao(null);setFormSubstituicao({titulo:'',descricao:'',referenciaSei:'',motivo:''});await carregar();}catch(err){notificar('error',mensagemErroDocumento(err));}finally{setProcessando(false);}}});}}><h4>Nova versão / acrescentar anexo</h4><div className={estilos.grade}><label><span>Título</span><input value={formSubstituicao.titulo} onChange={(e)=>setFormSubstituicao({...formSubstituicao,titulo:e.target.value})}/></label><label><span>Referência SEI</span><input value={formSubstituicao.referenciaSei} onChange={(e)=>setFormSubstituicao({...formSubstituicao,referenciaSei:e.target.value})}/></label><label><span>Novo arquivo opcional</span><input type="file" accept=".pdf,.png,.jpg,.jpeg,.doc,.docx,.xls,.xlsx" onChange={(e)=>setArquivoSubstituicao(e.target.files?.[0]||null)}/></label><label className={estilos.amplo}><span>Motivo *</span><textarea required value={formSubstituicao.motivo} onChange={(e)=>setFormSubstituicao({...formSubstituicao,motivo:e.target.value})}/></label></div><div className={estilos.acoes}><button type="button" onClick={()=>setSubstituindoId(null)}>Cancelar</button><button type="submit" disabled={processando}>Registrar nova versão</button></div></form>}
      {documentoSelecionado && <div className={estilos.detalhe}><h4>{documentoSelecionado.titulo} · cadeia documental</h4><ul>{eventosDocumento.map(ev=><li key={ev.id}><strong>{rotulo(ev.tipoEvento)}</strong> · {formatarDataHoraBrasileira(ev.registradoEm)} · {ev.motivo || 'sem motivo'}</li>)}</ul>{(administrador||podeGerenciar) && ehDocumentoRestrito(documentoSelecionado) && <><h5>Permissões e acessos</h5><ul>{permissoes.map(p=><li key={p.id}>{p.usuarioBeneficiarioUsername} · {p.finalidade} · {p.ativo?'ativa':'revogada'} {administrador&&p.ativo&&<button type="button" onClick={()=>revogarPermissao(p)}>Revogar</button>}</li>)}</ul><ul>{acessos.map(a=><li key={a.id}>{a.usuarioUsername} · {rotulo(a.tipoAcesso)} · {a.finalidade} · {formatarDataHoraBrasileira(a.acessadoEm)}</li>)}</ul>{administrador&&<form className={estilos.grade} onSubmit={concederPermissao}><label><span>Usuário *</span><select value={formPermissao.usuarioId} onChange={(e)=>setFormPermissao({...formPermissao,usuarioId:e.target.value})}><option value="">Selecione</option>{usuarios.map(u=><option key={u.id} value={u.id}>{u.username||u.nome||u.matricula}</option>)}</select></label><label><span>Finalidade *</span><input value={formPermissao.finalidade} onChange={(e)=>setFormPermissao({...formPermissao,finalidade:e.target.value})}/></label><label><span>Justificativa *</span><input value={formPermissao.justificativa} onChange={(e)=>setFormPermissao({...formPermissao,justificativa:e.target.value})}/></label><label><span>Autorização *</span><input value={formPermissao.autorizacaoAdministrativa} onChange={(e)=>setFormPermissao({...formPermissao,autorizacaoAdministrativa:e.target.value})}/></label><label><span>Válido até</span><input type="datetime-local" value={formPermissao.validoAte} onChange={(e)=>setFormPermissao({...formPermissao,validoAte:e.target.value})}/></label><button type="submit"><KeyRound size={14}/>Conceder</button></form>}</>}</div>}
    </div>}

    {aba==='transferencias' && <div className={estilos.conteudo}><p className={estilos.aviso}>Transferência excepcional é atômica: o Java exige autorização administrativa vigente e mapeamento completo de toda composição/alocação posterior. Não existe prévia autoritativa neste contrato de API.</p><div className={estilos.lista}>{transferencias.map(t=><article key={t.id}><strong>{t.gestaoOrigemNome} → {t.gestaoDestinoNome}</strong><span>{formatarDataBrasileira(t.dataEfetiva)} · {t.motivo}</span><small>{t.mapeamentos?.length||0} mapeamentos · SEI {t.referenciaSei}</small></article>)}</div>{podeGerenciar && <form className={estilos.formulario} onSubmit={transferir}><div className={estilos.grade}><label><span>Data efetiva *</span><input type="date" value={formTransferencia.dataEfetiva} onChange={(e)=>setFormTransferencia({...formTransferencia,dataEfetiva:e.target.value})}/></label><label><span>Data do ato</span><input type="date" value={formTransferencia.dataAto} onChange={(e)=>setFormTransferencia({...formTransferencia,dataAto:e.target.value})}/></label><label><span>Gestão destino *</span><select value={formTransferencia.gestaoLocalDestinoId} onChange={(e)=>setFormTransferencia({...formTransferencia,gestaoLocalDestinoId:e.target.value,mapeamentos:[]})}><option value="">Selecione</option>{gestoes.filter(g=>g.ativo!==false&&Number(g.id)!==Number(contrato?.gestaoLocalId)).map(g=><option key={g.id} value={g.id}>{g.nome}</option>)}</select></label><label><span>SEI *</span><input value={formTransferencia.referenciaSei} onChange={(e)=>setFormTransferencia({...formTransferencia,referenciaSei:e.target.value})}/></label><label><span>Autorização administrativa *</span><input value={formTransferencia.autorizacaoAdministrativa} onChange={(e)=>setFormTransferencia({...formTransferencia,autorizacaoAdministrativa:e.target.value})}/></label><label className={estilos.amplo}><span>Motivo *</span><textarea value={formTransferencia.motivo} onChange={(e)=>setFormTransferencia({...formTransferencia,motivo:e.target.value})}/></label></div><div className={estilos.mapeamentos}><div><strong>Mapeamentos de contexto</strong><button type="button" onClick={adicionarMapeamento}><Plus size={14}/>Adicionar</button></div>{formTransferencia.mapeamentos.map(m=><div className={estilos.mapa} key={m.chave}><select value={m.composicaoOrigemId} onChange={(e)=>{const id=e.target.value;const c=composicoes.find(x=>String(x.id)===id); setFormTransferencia(f=>({...f,mapeamentos:f.mapeamentos.map(x=>x.chave===m.chave?{...x,composicaoOrigemId:id,lotacaoOrigemId:c?.lotacaoId||'',jornadaOrigemId:c?.jornadaId||'',cargoOrigemId:c?.cargoId||''}:x)}));}}><option value="">Composição origem</option>{composicoes.map(c=><option key={c.id} value={c.id}>{c.lotacaoNome} · {c.jornadaDescricao} · {c.cargoNome}</option>)}</select><select value={m.lotacaoDestinoId} onChange={(e)=>alterarMap(m.chave,'lotacaoDestinoId',e.target.value)}><option value="">Lotação destino</option>{filtrarDestino(lotacoes).filter(x=>x.ativo!==false).map(x=><option key={x.id} value={x.id}>{x.nome}</option>)}</select><select value={m.jornadaDestinoId} onChange={(e)=>alterarMap(m.chave,'jornadaDestinoId',e.target.value)}><option value="">Turno destino</option>{filtrarDestino(jornadas).filter(x=>x.ativo!==false).map(x=><option key={x.id} value={x.id}>{x.descricao||x.nome}</option>)}</select><select value={m.cargoDestinoId} onChange={(e)=>alterarMap(m.chave,'cargoDestinoId',e.target.value)}><option value="">Cargo destino</option>{filtrarDestino(cargos).filter(x=>x.ativo!==false).map(x=><option key={x.id} value={x.id}>{x.nome}</option>)}</select><button type="button" onClick={()=>removerMap(m.chave)}><Trash2 size={14}/></button></div>)}</div><button type="submit" disabled={processando}>Revisar e confirmar transferência</button></form>}</div>}

    {aba==='correcoes' && <div className={estilos.conteudo}><div className={estilos.duasColunas}><section><h4>Correções de identificador</h4><ul>{correcoes.map(c=><li key={c.id}>{c.numeroAnterior}/{c.anoAnterior} → <b>{c.numeroNovo}/{c.anoNovo}</b> · {formatarDataBrasileira(c.dataEfetiva)}</li>)}</ul>{podeGerenciar&&<form className={estilos.formulario} onSubmit={corrigirIdentificador}><div className={estilos.grade}><label><span>Novo número *</span><input value={formCorrecao.novoNumero} onChange={(e)=>setFormCorrecao({...formCorrecao,novoNumero:e.target.value})}/></label><label><span>Novo ano *</span><input type="number" min="1900" max="9999" value={formCorrecao.novoAno} onChange={(e)=>setFormCorrecao({...formCorrecao,novoAno:e.target.value})}/></label><label><span>Data efetiva *</span><input type="date" value={formCorrecao.dataEfetiva} onChange={(e)=>setFormCorrecao({...formCorrecao,dataEfetiva:e.target.value})}/></label><label><span>Data do ato *</span><input type="date" value={formCorrecao.dataAto} onChange={(e)=>setFormCorrecao({...formCorrecao,dataAto:e.target.value})}/></label><label><span>SEI *</span><input value={formCorrecao.referenciaSei} onChange={(e)=>setFormCorrecao({...formCorrecao,referenciaSei:e.target.value})}/></label><label><span>Autorização *</span><input value={formCorrecao.autorizacaoAdministrativa} onChange={(e)=>setFormCorrecao({...formCorrecao,autorizacaoAdministrativa:e.target.value})}/></label><label className={estilos.amplo}><span>Motivo *</span><textarea value={formCorrecao.motivo} onChange={(e)=>setFormCorrecao({...formCorrecao,motivo:e.target.value})}/></label></div><button type="submit">Corrigir preservando histórico</button></form>}</section><section><h4>Pesquisar identificador antigo</h4><form className={estilos.busca} onSubmit={pesquisarHistorico}><input placeholder="Número" value={buscaHistorico.numero} onChange={(e)=>setBuscaHistorico({...buscaHistorico,numero:e.target.value})}/><input type="number" placeholder="Ano" value={buscaHistorico.ano} onChange={(e)=>setBuscaHistorico({...buscaHistorico,ano:e.target.value})}/><button><Search size={14}/>Pesquisar</button></form><ul>{historicoIdentificador.map(c=><li key={c.id}>{c.numeroAnterior}/{c.anoAnterior} → {c.numeroNovo}/{c.anoNovo}</li>)}</ul><h4>Correção de anulação</h4>{anulacoes.length===0?<p>Nenhum evento de anulação disponível.</p>:anulacoes.map(e=><article key={e.id}><span>Anulação em {formatarDataBrasileira(e.dataEfetiva)}</span>{podeGerenciar&&contrato?.estadoNegocial==='ANULADO'&&<button type="button" onClick={()=>corrigirAnulacao(e)}><ShieldCheck size={14}/>Registrar CANCELA</button>}</article>)}</section></div></div>}
    <ModalConfirmacao />
  </section>;
};
