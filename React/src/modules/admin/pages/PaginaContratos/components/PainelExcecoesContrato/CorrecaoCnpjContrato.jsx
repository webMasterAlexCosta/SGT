import React, { useCallback, useEffect, useMemo, useRef, useState } from 'react';
import { RefreshCw } from 'lucide-react';
import { gerarChaveIdempotencia } from '../../../../../../core/http/idempotencia.js';
import { ServicoExcecoesContrato } from '../../../../../../services/admin/ServicoExcecoesContrato.js';
import { prepararCorrecaoCnpj, RELACOES_PRECEDENCIA_CNPJ, validarCorrecaoCnpj } from '../../../../../../services/admin/ServicoDominioExcecoesContrato.js';
import { formatarDataBrasileira, formatarDataHoraBrasileira } from '../../../../../../services/admin/ServicoDominioContrato.js';
import estilos from './index.module.css';

const FORMULARIO_INICIAL = Object.freeze({
  cnpjCorrigido: '', dataEfetiva: '', dataAto: '', motivo: '', referenciaSei: '',
  eventoPrecedenteId: '', relacaoPrecedencia: '', ordemDocumental: '',
});
const senhaInvalida = (erro) => erro?.senhaConfirmacaoInvalida || erro?.code === 'INVALID_CONFIRMATION_PASSWORD';

export const CorrecaoCnpjContrato = ({ contrato, podeGerenciar, confirmarComSenha, fecharConfirmacao, aoAtualizar, aoAtualizarContrato }) => {
  const [formulario, setFormulario] = useState({ ...FORMULARIO_INICIAL });
  const [historico, setHistorico] = useState([]);
  const [consulta, setConsulta] = useState({ estado: 'inicial', mensagem: '' });
  const [pagina, setPagina] = useState(0);
  const [ocupado, setOcupado] = useState(false);
  const [erro, setErro] = useState('');
  const [sucesso, setSucesso] = useState('');
  const [resultadoIncerto, setResultadoIncerto] = useState(false);
  const leituraRef = useRef(0);
  const controleRef = useRef(null);
  const envioRef = useRef(false);
  const montadoRef = useRef(true);
  const intencaoRef = useRef(null);
  const payload = useMemo(() => prepararCorrecaoCnpj(formulario), [formulario]);
  const assinatura = JSON.stringify(payload);
  const publicado = contrato?.estadoNegocial === 'PUBLICADO';
  const paginas = Math.max(1, Math.ceil(historico.length / 5));
  const paginaAtual = Math.min(pagina, paginas - 1);

  const carregar = useCallback(async () => {
    if (!contrato?.id) return;
    const pedido = ++leituraRef.current;
    controleRef.current?.abort();
    const controlador = new AbortController();
    controleRef.current = controlador;
    setConsulta({ estado: 'carregando', mensagem: '' });
    setHistorico([]);
    try {
      const resposta = await ServicoExcecoesContrato.listarCorrecoesCnpj(contrato.id, { signal: controlador.signal });
      if (!montadoRef.current || pedido !== leituraRef.current || controlador.signal.aborted) return;
      setHistorico(Array.isArray(resposta) ? resposta : []);
      setConsulta({ estado: 'sucesso', mensagem: '' });
      setPagina(0);
    } catch (falha) {
      if (montadoRef.current && pedido === leituraRef.current && !controlador.signal.aborted && !falha?.canceled) {
        setConsulta({ estado: 'erro', mensagem: falha?.message || 'Não foi possível consultar as correções de CNPJ.' });
      }
    }
  }, [contrato?.id]);

  useEffect(() => {
    montadoRef.current = true;
    carregar();
    return () => {
      montadoRef.current = false;
      leituraRef.current += 1;
      controleRef.current?.abort();
      if (envioRef.current) fecharConfirmacao();
    };
  }, [carregar, fecharConfirmacao]);

  const alterar = (campo, valor) => {
    if (envioRef.current) return;
    setErro(''); setSucesso('');
    setFormulario((atual) => ({ ...atual, [campo]: valor }));
  };

  const informarErro = (falha) => {
    if (senhaInvalida(falha)) return;
    setErro(falha?.message || 'A correção de CNPJ não foi confirmada.');
    setResultadoIncerto(Boolean(falha?.resultadoIncerto));
    fecharConfirmacao();
  };

  const submeter = async (evento) => {
    evento.preventDefault();
    if (envioRef.current || !podeGerenciar || !publicado) return;
    const falhaLocal = validarCorrecaoCnpj(formulario);
    if (falhaLocal) { setErro(falhaLocal); return; }
    if (!intencaoRef.current || intencaoRef.current.assinatura !== assinatura) {
      intencaoRef.current = { assinatura, dados: { ...payload, chaveIdempotencia: gerarChaveIdempotencia('corr-cnpj') } };
    }
    const intencao = intencaoRef.current.dados;
    envioRef.current = true;
    setOcupado(true); setErro(''); setSucesso('');
    try {
      await confirmarComSenha({
        titulo: 'Confirmar correção material de CNPJ',
        mensagem: 'A correção documentada será registrada no histórico do contrato. A troca real da empresa deve ser feita por ADITAMENTO.',
        acao: async (senhaConfirmacao) => {
          if (!montadoRef.current) return false;
          try {
            await ServicoExcecoesContrato.corrigirCnpj(contrato.id, intencao, { senhaConfirmacao });
          } catch (falha) {
            if (montadoRef.current) informarErro(falha);
            throw falha;
          }
          if (montadoRef.current) {
            intencaoRef.current = null;
            setFormulario({ ...FORMULARIO_INICIAL });
            setResultadoIncerto(false);
            setSucesso('Correção de CNPJ registrada.');
            await carregar();
            try {
              await aoAtualizar?.();
              await aoAtualizarContrato?.();
            } catch (falha) {
              if (montadoRef.current) setErro(`A correção foi registrada, mas não foi possível atualizar a visão do contrato. ${falha?.message || 'Consulte novamente os detalhes.'}`);
            }
          }
          return true;
        },
      });
    } catch (falha) {
      if (montadoRef.current) informarErro(falha);
    } finally {
      envioRef.current = false;
      if (montadoRef.current) setOcupado(false);
    }
  };

  return <div className={estilos.conteudo}>
    <h3>Correção material de CNPJ</h3>
    <p className={estilos.aviso}>Use esta ação para corrigir um erro material no CNPJ documentado do contrato. A troca real de empresa deve ser registrada por ADITAMENTO. As correções preservam o histórico e respeitam a vigência e a precedência dos atos.</p>
    <div className={estilos.acoes}><button type="button" disabled={ocupado || consulta.estado === 'carregando'} onClick={carregar}><RefreshCw size={15} />Atualizar histórico de CNPJ</button></div>
    {consulta.estado === 'carregando' && <p role="status">Consultando correções de CNPJ…</p>}
    {consulta.estado === 'erro' && <p role="alert" className={estilos.erro}>{consulta.mensagem}</p>}
    {consulta.estado === 'sucesso' && historico.length === 0 && <p>Nenhuma correção de CNPJ retornada para este contrato.</p>}
    <div className={estilos.lista}>{historico.slice(paginaAtual * 5, paginaAtual * 5 + 5).map((item) => <article key={item.id}>
      <div>
        <h4>Correção #{item.id}</h4>
        <strong>{item.cnpjAnterior ?? 'CNPJ anterior não informado'} → {item.cnpjCorrigido ?? 'CNPJ corrigido não informado'}</strong>
        <span>Efetiva em {formatarDataBrasileira(item.dataEfetiva)} · Ato de {formatarDataBrasileira(item.dataAto)}</span>
        <span>{item.motivo}</span>
        <small>SEI {item.referenciaSei ?? 'não informado'} · Evento #{item.eventoId ?? 'não informado'}</small>
        <small>Registrada em {formatarDataHoraBrasileira(item.registradoEm)} · {item.responsavelUsername ?? 'Responsável não informado'}</small>
      </div>
    </article>)}</div>
    {paginas > 1 && <nav className={estilos.paginacao} aria-label="Páginas das correções de CNPJ">
      <button type="button" disabled={paginaAtual === 0} onClick={() => setPagina(paginaAtual - 1)}>Anterior</button>
      <span>Página {paginaAtual + 1} de {paginas} · {historico.length} registros</span>
      <button type="button" disabled={paginaAtual + 1 === paginas} onClick={() => setPagina(paginaAtual + 1)}>Próxima</button>
    </nav>}
    {erro && <p role="alert" className={estilos.erro}>{erro}</p>}
    {sucesso && <p role="status">{sucesso}</p>}
    {resultadoIncerto && <p className={estilos.aviso}>O resultado da tentativa ainda não foi confirmado. Consulte o histórico antes de alterar os dados. Repetir os mesmos dados permite conferir a tentativa anterior.</p>}
    {podeGerenciar && !publicado && <p className={estilos.aviso}>A correção material exige contrato publicado.</p>}
    {podeGerenciar && publicado && <form aria-label="Correção material de CNPJ" className={estilos.formulario} onSubmit={submeter}>
      <fieldset className={estilos.campos} disabled={ocupado}><div className={estilos.grade}>
        <label><span>CNPJ corrigido *</span><input inputMode="numeric" maxLength={18} value={formulario.cnpjCorrigido} onChange={(e) => alterar('cnpjCorrigido', e.target.value)} required /></label>
        <label><span>Data efetiva da correção *</span><input type="date" value={formulario.dataEfetiva} onChange={(e) => alterar('dataEfetiva', e.target.value)} required /></label>
        <label><span>Data do ato da correção *</span><input type="date" value={formulario.dataAto} onChange={(e) => alterar('dataAto', e.target.value)} required /></label>
        <label><span>Referência SEI da correção *</span><input maxLength={120} value={formulario.referenciaSei} onChange={(e) => alterar('referenciaSei', e.target.value)} required /></label>
        <label className={estilos.amplo}><span>Motivo da correção *</span><textarea maxLength={1000} value={formulario.motivo} onChange={(e) => alterar('motivo', e.target.value)} required /></label>
        <label><span>Evento precedente da correção (ID)</span><input type="number" min="1" step="1" value={formulario.eventoPrecedenteId} onChange={(e) => alterar('eventoPrecedenteId', e.target.value)} /></label>
        <label><span>Relação de precedência da correção</span><select value={formulario.relacaoPrecedencia} onChange={(e) => alterar('relacaoPrecedencia', e.target.value)}><option value="">Sem precedência</option>{RELACOES_PRECEDENCIA_CNPJ.map((relacao) => <option key={relacao} value={relacao}>{relacao.replaceAll('_', ' ')}</option>)}</select></label>
        <label><span>Ordem documental da correção</span><input type="number" min="-2147483648" max="2147483647" step="1" value={formulario.ordemDocumental} onChange={(e) => alterar('ordemDocumental', e.target.value)} /></label>
      </div></fieldset>
      <button type="submit" disabled={ocupado}>{ocupado ? 'Aguardando confirmação…' : 'Corrigir CNPJ com confirmação'}</button>
    </form>}
  </div>;
};
