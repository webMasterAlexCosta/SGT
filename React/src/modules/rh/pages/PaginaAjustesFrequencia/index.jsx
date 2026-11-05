import React, { useCallback, useEffect, useMemo, useState } from 'react';
import { CalendarClock, Edit3, Plus, RefreshCw, Search, Trash2 } from 'lucide-react';
import { EstruturaPagina } from '../../../../core/ui/EstruturaPagina.jsx';
import { Botao, JanelaModal } from '../../../../core/ui/EnvolucroDica.jsx';
import { notificar } from '../../../../core/events.js';
import { useConfirmacaoSenha } from '../../../../core/security/useConfirmacaoSenha.jsx';
import { useAutenticacao } from '../../../auth/context.jsx';
import { eAdministradorTecnico, eGestorNacional, podeGerenciarAjustesFrequencia } from '../../../../core/security/roles.js';
import { ServicoEmpregado } from '../../../../services/rh/ServicoEmpregado.js';
import { ServicoOcorrencia } from '../../../../services/rh/ServicoOcorrencia.js';
import { ServicoAdicionalFrequencia, ServicoExcecaoFrequencia } from '../../../../services/rh/ServicoFrequencia.js';
import estilos from './index.module.css';

const hoje = () => {
  const d = new Date();
  return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`;
};
const rotulo = (v) => String(v || '-').replaceAll('_', ' ');
const formExcecaoInicial = () => ({ id: null, empregadoId: '', empregadoNome: '', dataInicio: hoje(), dataFim: hoje(), tipo: '', status: '', observacao: '' });
const formAdicionalInicial = () => ({ id: null, empregadoId: '', empregadoNome: '', dataReferencia: hoje(), tipo: '', minutos: '', quantidade: '', status: '', justificativa: '' });

export const PaginaAjustesFrequencia = () => {
  const { user } = useAutenticacao();
  const podeEditar = podeGerenciarAjustesFrequencia(user);
  const exigeFiltroGlobal = eAdministradorTecnico(user) || eGestorNacional(user);
  const { confirmarComSenha, ModalConfirmacao } = useConfirmacaoSenha();
  const [data, setData] = useState(hoje());
  const [lotacaoId, setLotacaoId] = useState('');
  const [jornadaId, setJornadaId] = useState('');
  const [contratoId, setContratoId] = useState('');
  const [auxiliares, setAuxiliares] = useState({ lotacoes: [], contratos: [] });
  const [excecoes, setExcecoes] = useState([]);
  const [adicionais, setAdicionais] = useState([]);
  const [carregando, setCarregando] = useState(false);
  const [erro, setErro] = useState(null);
  const [aba, setAba] = useState('excecoes');
  const [modal, setModal] = useState(null);
  const [formExcecao, setFormExcecao] = useState(formExcecaoInicial);
  const [formAdicional, setFormAdicional] = useState(formAdicionalInicial);
  const [buscaEmpregado, setBuscaEmpregado] = useState('');
  const [empregados, setEmpregados] = useState([]);

  useEffect(() => {
    const controlador = new AbortController();
    ServicoEmpregado.buscarDadosAuxiliaresFrequencia({ signal: controlador.signal })
      .then((dados) => setAuxiliares({ lotacoes: Array.isArray(dados?.lotacoes) ? dados.lotacoes : [], contratos: Array.isArray(dados?.contratos) ? dados.contratos : [] }))
      .catch((falha) => { if (!falha?.canceled) console.error(falha); });
    return () => controlador.abort();
  }, []);

  const turnos = useMemo(() => auxiliares.lotacoes.find((l) => String(l.id) === String(lotacaoId))?.turnos || [], [auxiliares.lotacoes, lotacaoId]);
  const parametros = useMemo(() => ({
    ...(lotacaoId ? { lotacaoId: Number(lotacaoId) } : {}),
    ...(jornadaId ? { jornadaId: Number(jornadaId) } : {}),
    ...(contratoId ? { contratoId: Number(contratoId) } : {}),
  }), [contratoId, jornadaId, lotacaoId]);
  const filtroGlobalAusente = exigeFiltroGlobal && !lotacaoId && !jornadaId && !contratoId;

  const carregar = useCallback(async () => {
    if (!data || filtroGlobalAusente) {
      setExcecoes([]);
      setAdicionais([]);
      return;
    }
    const controlador = new AbortController();
    setCarregando(true);
    setErro(null);
    try {
      const [listaExcecoes, listaAdicionais] = await Promise.all([
        ServicoExcecaoFrequencia.buscarPorData(data, parametros, { signal: controlador.signal }),
        ServicoAdicionalFrequencia.buscarPorData(data, parametros, { signal: controlador.signal }),
      ]);
      setExcecoes(Array.isArray(listaExcecoes) ? listaExcecoes : []);
      setAdicionais(Array.isArray(listaAdicionais) ? listaAdicionais : []);
    } catch (falha) {
      if (!falha?.canceled) setErro(falha);
    } finally {
      if (!controlador.signal.aborted) setCarregando(false);
    }
    return () => controlador.abort();
  }, [data, filtroGlobalAusente, parametros]);

  useEffect(() => { void carregar(); }, [carregar]);

  useEffect(() => {
    if (buscaEmpregado.trim().length < 2 || !modal) { setEmpregados([]); return undefined; }
    const controlador = new AbortController();
    const timer = setTimeout(() => {
      ServicoOcorrencia.buscarEmpregados(buscaEmpregado.trim(), { signal: controlador.signal, skipLoading: true })
        .then((lista) => setEmpregados(Array.isArray(lista) ? lista : []))
        .catch((falha) => { if (!falha?.canceled) console.error(falha); });
    }, 300);
    return () => { clearTimeout(timer); controlador.abort(); };
  }, [buscaEmpregado, modal]);

  const selecionarEmpregado = (item) => {
    setBuscaEmpregado(item.nome || item.matricula || `Empregado ${item.id}`);
    setEmpregados([]);
    if (modal === 'excecao') setFormExcecao((a) => ({ ...a, empregadoId: item.id, empregadoNome: item.nome || '' }));
    else setFormAdicional((a) => ({ ...a, empregadoId: item.id, empregadoNome: item.nome || '' }));
  };

  const abrirNovo = (tipo) => {
    setModal(tipo);
    setBuscaEmpregado('');
    setEmpregados([]);
    if (tipo === 'excecao') setFormExcecao({ ...formExcecaoInicial(), dataInicio: data, dataFim: data });
    else setFormAdicional({ ...formAdicionalInicial(), dataReferencia: data });
  };

  const abrirEdicao = (tipo, item) => {
    setModal(tipo);
    setBuscaEmpregado(item.empregadoNome || '');
    setEmpregados([]);
    if (tipo === 'excecao') setFormExcecao({ ...formExcecaoInicial(), ...item, empregadoId: item.empregadoId || '' });
    else setFormAdicional({ ...formAdicionalInicial(), ...item, empregadoId: item.empregadoId || '', minutos: item.minutos ?? '', quantidade: item.quantidade ?? '' });
  };

  const salvar = async () => {
    const ehExcecao = modal === 'excecao';
    const form = ehExcecao ? formExcecao : formAdicional;
    if (!form.empregadoId || !String(form.tipo || '').trim()) {
      notificar('warning', 'Selecione o empregado e informe o tipo.'); return;
    }
    if (ehExcecao && (!form.dataInicio || !form.dataFim)) { notificar('warning', 'Informe o período da exceção.'); return; }
    if (!ehExcecao && !form.dataReferencia) { notificar('warning', 'Informe a data do adicional.'); return; }

    const payload = ehExcecao ? {
      empregadoId: Number(form.empregadoId), dataInicio: form.dataInicio, dataFim: form.dataFim,
      tipo: form.tipo.trim(), status: form.status?.trim() || null, observacao: form.observacao?.trim() || null,
    } : {
      empregadoId: Number(form.empregadoId), dataReferencia: form.dataReferencia, tipo: form.tipo.trim(),
      minutos: form.minutos === '' ? null : Number(form.minutos), quantidade: form.quantidade === '' ? null : Number(form.quantidade),
      status: form.status?.trim() || null, justificativa: form.justificativa?.trim() || null,
    };
    await confirmarComSenha({
      titulo: form.id ? 'Atualizar ajuste de frequência' : 'Criar ajuste de frequência',
      mensagem: 'Sobreposição, escopo, status padrão e demais regras serão validados pelo Java.',
      acao: async (senhaConfirmacao) => {
        const servico = ehExcecao ? ServicoExcecaoFrequencia : ServicoAdicionalFrequencia;
        if (form.id) await servico.atualizar(form.id, payload, { senhaConfirmacao });
        else await servico.criar(payload, { senhaConfirmacao });
        setModal(null);
        notificar('success', 'Ajuste salvo conforme o contrato Java.');
        await carregar();
      },
    });
  };

  const excluir = async (tipo, item) => {
    await confirmarComSenha({
      titulo: 'Excluir ajuste de frequência',
      mensagem: `Excluir ${rotulo(item.tipo)} de ${item.empregadoNome || `empregado #${item.empregadoId}`}?`,
      acao: async (senhaConfirmacao) => {
        const servico = tipo === 'excecao' ? ServicoExcecaoFrequencia : ServicoAdicionalFrequencia;
        await servico.excluir(item.id, { senhaConfirmacao });
        notificar('success', 'Ajuste removido.');
        await carregar();
      },
    });
  };

  const registros = aba === 'excecoes' ? excecoes : adicionais;
  return (
    <>
      <ModalConfirmacao />
      <EstruturaPagina
        titulo="Exceções e adicionais de frequência"
        subtitulo="Consulta e manutenção dos ajustes aceitos pela API. Tipos e estados retornados pelo Java são preservados sem catálogo restritivo no navegador."
        carregando={carregando}
        erro={erro}
        aoTentarNovamente={carregar}
        vazio={!filtroGlobalAusente && !carregando && !erro && registros.length === 0}
        acoes={<><Botao variant="outline" onClick={carregar}><RefreshCw size={16} /> Atualizar</Botao>{podeEditar && <Botao onClick={() => abrirNovo(aba === 'excecoes' ? 'excecao' : 'adicional')}><Plus size={16} /> Novo ajuste</Botao>}</>}
        filtros={<div className={estilos.filtros}><label>Data<input type="date" value={data} onChange={(e) => setData(e.target.value)} /></label><label>Lotação<select value={lotacaoId} onChange={(e) => { setLotacaoId(e.target.value); setJornadaId(''); }}><option value="">Todas</option>{auxiliares.lotacoes.map((item) => <option key={item.id} value={item.id}>{item.nome || item.label || `Lotação ${item.id}`}</option>)}</select></label><label>Turno<select value={jornadaId} onChange={(e) => setJornadaId(e.target.value)}><option value="">Todos</option>{turnos.map((item) => <option key={item.id} value={item.id}>{item.descricao || item.nome || `Turno ${item.id}`}</option>)}</select></label><label>Contrato<select value={contratoId} onChange={(e) => setContratoId(e.target.value)}><option value="">Todos</option>{auxiliares.contratos.map((item) => <option key={item.id} value={item.id}>{item.numeroContrato || item.numero || item.nome || `Contrato ${item.id}`}</option>)}</select></label></div>}
      >
        {filtroGlobalAusente ? <div className={estilos.aviso}>Para Administrador/Gestor Nacional, o Java exige ao menos lotação, turno ou contrato nesta consulta.</div> : <>
          <div className={estilos.abas}><button type="button" className={aba === 'excecoes' ? estilos.abaAtiva : ''} onClick={() => setAba('excecoes')}>Exceções ({excecoes.length})</button><button type="button" className={aba === 'adicionais' ? estilos.abaAtiva : ''} onClick={() => setAba('adicionais')}>Adicionais ({adicionais.length})</button></div>
          <div className={estilos.lista}>{registros.map((item) => <article key={`${aba}-${item.id}`} className={estilos.cartao}><div className={estilos.topo}><div><CalendarClock size={18} /><strong>{item.empregadoNome || `Empregado #${item.empregadoId}`}</strong></div><span>{rotulo(item.status)}</span></div><dl>{aba === 'excecoes' ? <><div><dt>Período</dt><dd>{item.dataInicio} a {item.dataFim}</dd></div><div><dt>Tipo</dt><dd>{rotulo(item.tipo)}</dd></div><div><dt>Observação</dt><dd>{item.observacao || '-'}</dd></div></> : <><div><dt>Data</dt><dd>{item.dataReferencia}</dd></div><div><dt>Tipo</dt><dd>{rotulo(item.tipo)}</dd></div><div><dt>Minutos</dt><dd>{item.minutos ?? '-'}</dd></div><div><dt>Quantidade</dt><dd>{item.quantidade ?? '-'}</dd></div><div><dt>Justificativa</dt><dd>{item.justificativa || '-'}</dd></div></>}</dl>{podeEditar && <div className={estilos.acoes}><Botao variant="outline" onClick={() => abrirEdicao(aba === 'excecoes' ? 'excecao' : 'adicional', item)}><Edit3 size={16} /> Editar</Botao><Botao variant="outline" onClick={() => excluir(aba === 'excecoes' ? 'excecao' : 'adicional', item)}><Trash2 size={16} /> Excluir</Botao></div>}</article>)}</div>
        </>}
      </EstruturaPagina>

      <JanelaModal isOpen={Boolean(modal)} onClose={() => setModal(null)} title={modal === 'excecao' ? 'Exceção de frequência' : 'Adicional de frequência'} maxWidth="max-w-2xl">
        <div className={estilos.formulario}>
          <label className={estilos.largo}>Empregado <div className={estilos.busca}><Search size={16} /><input value={buscaEmpregado} onChange={(e) => { setBuscaEmpregado(e.target.value); if ((modal === 'excecao' ? formExcecao.empregadoNome : formAdicional.empregadoNome) !== e.target.value) { if (modal === 'excecao') setFormExcecao((a) => ({ ...a, empregadoId: '', empregadoNome: '' })); else setFormAdicional((a) => ({ ...a, empregadoId: '', empregadoNome: '' })); } }} placeholder="Digite ao menos 2 caracteres" disabled={Boolean((modal === 'excecao' ? formExcecao.id : formAdicional.id))} /></div>{empregados.length > 0 && <div className={estilos.resultados}>{empregados.slice(0, 10).map((item) => <button type="button" key={item.id} onClick={() => selecionarEmpregado(item)}>{item.nome || `Empregado ${item.id}`} {item.matricula ? `· ${item.matricula}` : ''}</button>)}</div>}</label>
          {modal === 'excecao' ? <><label>Início<input type="date" value={formExcecao.dataInicio} onChange={(e) => setFormExcecao((a) => ({ ...a, dataInicio: e.target.value }))} /></label><label>Fim<input type="date" value={formExcecao.dataFim} onChange={(e) => setFormExcecao((a) => ({ ...a, dataFim: e.target.value }))} /></label><label>Tipo<input value={formExcecao.tipo} onChange={(e) => setFormExcecao((a) => ({ ...a, tipo: e.target.value.toUpperCase() }))} placeholder="Tipo aceito pelo Java" /></label><label>Status opcional<input value={formExcecao.status} onChange={(e) => setFormExcecao((a) => ({ ...a, status: e.target.value.toUpperCase() }))} /></label><label className={estilos.largo}>Observação<textarea value={formExcecao.observacao || ''} onChange={(e) => setFormExcecao((a) => ({ ...a, observacao: e.target.value }))} /></label></> : <><label>Data<input type="date" value={formAdicional.dataReferencia} onChange={(e) => setFormAdicional((a) => ({ ...a, dataReferencia: e.target.value }))} /></label><label>Tipo<input value={formAdicional.tipo} onChange={(e) => setFormAdicional((a) => ({ ...a, tipo: e.target.value.toUpperCase() }))} placeholder="Tipo aceito pelo Java" /></label><label>Minutos<input type="number" value={formAdicional.minutos} onChange={(e) => setFormAdicional((a) => ({ ...a, minutos: e.target.value }))} /></label><label>Quantidade<input type="number" step="0.01" value={formAdicional.quantidade} onChange={(e) => setFormAdicional((a) => ({ ...a, quantidade: e.target.value }))} /></label><label>Status opcional<input value={formAdicional.status} onChange={(e) => setFormAdicional((a) => ({ ...a, status: e.target.value.toUpperCase() }))} /></label><label className={estilos.largo}>Justificativa<textarea value={formAdicional.justificativa || ''} onChange={(e) => setFormAdicional((a) => ({ ...a, justificativa: e.target.value }))} /></label></>}
          <p className={estilos.nota}>A busca de empregado é compatibilidade temporária porque os dois contratos Java desta etapa ainda recebem <code>empregadoId</code>. Nenhuma nova identidade negocial é criada no React.</p>
          <div className={`${estilos.acoes} ${estilos.largo}`}><Botao variant="outline" onClick={() => setModal(null)}>Cancelar</Botao><Botao onClick={salvar}>Salvar</Botao></div>
        </div>
      </JanelaModal>
    </>
  );
};
