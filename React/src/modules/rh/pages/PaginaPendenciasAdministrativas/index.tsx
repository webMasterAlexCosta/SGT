import type { ObservedFailure } from "../../../../core/http/falhaObservada";
import { observarFalha } from '../../../../core/http/falhaObservada';
import type { PaginaPendenciasDTO, ResumoPendenciasDTO } from '../../../../contracts/java';
import React, { useCallback, useEffect, useMemo, useState } from 'react';
import { CheckCircle2, Clock3, ExternalLink, PlayCircle, RefreshCw, TriangleAlert } from 'lucide-react';
import { useNavigate } from 'react-router-dom';
import { EstruturaPagina } from '../../../../core/ui/EstruturaPagina.tsx';
import { Botao, JanelaModal } from '../../../../core/ui/EnvolucroDica.tsx';
import { notificar } from '../../../../core/events.ts';
import { ServicoPendenciaAdministrativa } from '../../../../services/rh/ServicoPendenciaAdministrativa.ts';
import estilos from './index.module.css';

const ESTADOS = ['', 'ABERTA', 'EM_TRATAMENTO', 'RESOLVIDA'];
const TIPOS = ['', 'RETIFICACAO_FREQUENCIA', 'FIM_NATURAL_VIGENCIA', 'CONFLITO_REPLAY_TEMPORAL'];
const rotulo = (valor: (string) | (import("../../../../contracts/java").EstadoPendencia | null) | (import("../../../../contracts/java").TipoPendencia | null) | (import("../../../../contracts/java").PerfilAcesso | null) | (import("../../../../contracts/java").EscopoBloqueioPendencia | null)) => String(valor || '-').replaceAll('_', ' ').toLocaleLowerCase('pt-BR').replace(/(^|\s)\S/g, (m) => m.toUpperCase());
const formatarInstante = (valor: (import("../../../../contracts/shared").Instant | null)) => valor ? new Date(valor).toLocaleString('pt-BR') : '-';

export const PaginaPendenciasAdministrativas = () => {
  const navegar = useNavigate();
  const [filtros, setFiltros] = useState({ estado: '', tipo: '', inicio: '', fim: '' });
  const [pagina, setPagina] = useState(0);
  const [dados, setDados] = useState<Partial<PaginaPendenciasDTO>>({ registros: [], totalElementos: 0, totalPaginas: 0, primeira: true, ultima: true });
  const [resumo, setResumo] = useState<Partial<ResumoPendenciasDTO>>({ abertas: 0, emTratamento: 0, resolvidas: 0, retificacoesFrequencia: 0, finsNaturaisVigencia: 0 });
  const [carregando, setCarregando] = useState(false);
  const [erro, setErro] = useState<ObservedFailure | null>(null);
  const [tratamento, setTratamento] = useState<({ id: number | null; acao: string; }) | null>(null);
  const [observacao, setObservacao] = useState('');

  const parametrosBase = useMemo(() => ({
    ...(filtros.estado ? { estado: filtros.estado } : {}),
    ...(filtros.tipo ? { tipo: filtros.tipo } : {}),
    ...(filtros.inicio ? { inicio: filtros.inicio } : {}),
    ...(filtros.fim ? { fim: filtros.fim } : {}),
  }), [filtros]);

  const carregar = useCallback(async () => {
    const controlador = new AbortController();
    setCarregando(true);
    setErro(null);
    try {
      const [paginaResposta, resumoResposta] = await Promise.all([
        ServicoPendenciaAdministrativa.listar({ ...parametrosBase, pagina, tamanho: 50 }, { signal: controlador.signal }),
        ServicoPendenciaAdministrativa.obterResumo(parametrosBase, { signal: controlador.signal }),
      ]);
      setDados(paginaResposta || { registros: [] });
      setResumo(resumoResposta || {});
    } catch (falhaOriginal) {
      const falha = observarFalha(falhaOriginal);

      if (!falha?.canceled) setErro(falha);
    } finally {
      if (!controlador.signal.aborted) setCarregando(false);
    }
    return () => controlador.abort();
  }, [pagina, parametrosBase]);

  useEffect(() => { void carregar(); }, [carregar]);
  useEffect(() => { setPagina(0); }, [filtros]);

  const confirmarTratamento = async () => {
    if (!tratamento?.id) return;
    try {
      if (tratamento.acao === 'iniciar') {
        await ServicoPendenciaAdministrativa.iniciarTratamento(tratamento.id, observacao);
        notificar('success', 'Tratamento iniciado conforme o estado validado pelo Java.');
      } else {
        await ServicoPendenciaAdministrativa.confirmarResolucao(tratamento.id, observacao);
        notificar('success', 'Resolução confirmada conforme a validação do Java.');
      }
      setTratamento(null);
      setObservacao('');
      await carregar();
    } catch (falhaOriginal) {
      const falha = observarFalha(falhaOriginal);

      notificar('error', falha?.message || 'Não foi possível atualizar a pendência.');
    }
  };

  const filtrosAtivos = Object.values(filtros).filter(Boolean).length;
  const registros = Array.isArray(dados?.registros) ? dados.registros : [];

  return (
    <EstruturaPagina
      titulo="Pendências administrativas"
      subtitulo="Fila central de saneamento gerada pelo Java. O React não recalcula estado, responsabilidade, bloqueio nem resolução."
      carregando={carregando}
      erro={erro}
      aoTentarNovamente={carregar}
      vazio={!carregando && !erro && registros.length === 0}
      quantidadeFiltrosAtivos={filtrosAtivos}
      aoLimparFiltros={() => setFiltros({ estado: '', tipo: '', inicio: '', fim: '' })}
      acoes={<Botao variant="outline" onClick={carregar}><RefreshCw size={16} /> Atualizar</Botao>}
      filtros={(
        <div className={estilos.filtros}>
          <label>Estado<select value={filtros.estado} onChange={(e) => setFiltros((a) => ({ ...a, estado: e.target.value }))}>{ESTADOS.map((item) => <option key={item || 'todos'} value={item}>{item ? rotulo(item) : 'Todos'}</option>)}</select></label>
          <label>Tipo<select value={filtros.tipo} onChange={(e) => setFiltros((a) => ({ ...a, tipo: e.target.value }))}>{TIPOS.map((item) => <option key={item || 'todos'} value={item}>{item ? rotulo(item) : 'Todos'}</option>)}</select></label>
          <label>Início<input type="date" value={filtros.inicio} onChange={(e) => setFiltros((a) => ({ ...a, inicio: e.target.value }))} /></label>
          <label>Fim<input type="date" value={filtros.fim} onChange={(e) => setFiltros((a) => ({ ...a, fim: e.target.value }))} /></label>
        </div>
      )}
    >
      <div className={estilos.resumo}>
        <article><TriangleAlert size={20} /><span>Abertas</span><strong>{Number(resumo.abertas || 0).toLocaleString('pt-BR')}</strong></article>
        <article><Clock3 size={20} /><span>Em tratamento</span><strong>{Number(resumo.emTratamento || 0).toLocaleString('pt-BR')}</strong></article>
        <article><CheckCircle2 size={20} /><span>Resolvidas</span><strong>{Number(resumo.resolvidas || 0).toLocaleString('pt-BR')}</strong></article>
        <article><span>Retificações</span><strong>{Number(resumo.retificacoesFrequencia || 0).toLocaleString('pt-BR')}</strong></article>
        <article><span>Fins de vigência</span><strong>{Number(resumo.finsNaturaisVigencia || 0).toLocaleString('pt-BR')}</strong></article>
      </div>

      <div className={estilos.lista}>
        {registros.map((item) => (
          <article className={estilos.cartao} key={item.id}>
            <div className={estilos.topoCartao}>
              <div><span className={`${estilos.estado} ${estilos[`estado${item.estado}`] || ''}`}>{rotulo(item.estado)}</span><strong>#{item.id} · {rotulo(item.tipo)}</strong></div>
              <small>{formatarInstante(item.surgidaEm)}</small>
            </div>
            <p>{item.motivo || 'Sem motivo textual informado.'}</p>
            {item.acaoSugerida && <div className={estilos.sugestao}><strong>Ação sugerida pelo Java:</strong> {item.acaoSugerida}</div>}
            <dl className={estilos.detalhes}>
              <div><dt>Responsável</dt><dd>{rotulo(item.perfilResponsavel)}</dd></div>
              <div><dt>Escopo de bloqueio</dt><dd>{rotulo(item.escopoBloqueio)}</dd></div>
              <div><dt>Data efetiva</dt><dd>{item.dataEfetiva || '-'}</dd></div>
              <div><dt>Operação de origem</dt><dd>{item.operacaoOrigem || '-'}</dd></div>
              {item.contratoId && <div><dt>Contrato</dt><dd>{item.contrato || `#${item.contratoId}`}</dd></div>}
              {item.pessoaId && <div><dt>Pessoa</dt><dd>{item.pessoa || `#${item.pessoaId}`}</dd></div>}
              {item.vinculoId && <div><dt>Vínculo</dt><dd>#{item.vinculoId}</dd></div>}
              {item.frequenciaId && <div><dt>Frequência</dt><dd>#{item.frequenciaId}</dd></div>}
            </dl>
            {item.observacaoTratamento && <div className={estilos.observacao}><strong>Tratamento:</strong> {item.observacaoTratamento}</div>}
            <div className={estilos.acoes}>
              {item.estado === 'ABERTA' && <Botao onClick={() => { setTratamento({ id: item.id, acao: 'iniciar' }); setObservacao(''); }}><PlayCircle size={16} /> Iniciar tratamento</Botao>}
              {item.estado === 'EM_TRATAMENTO' && <Botao onClick={() => { setTratamento({ id: item.id, acao: 'resolver' }); setObservacao(item.observacaoTratamento || ''); }}><CheckCircle2 size={16} /> Confirmar resolução</Botao>}
              {item.contratoId && <Botao variant="outline" onClick={() => navegar(`/contratos?contratoId=${item.contratoId}`)}><ExternalLink size={16} /> Contrato</Botao>}
              {item.vinculoId && <Botao variant="outline" onClick={() => navegar(`/pessoas?vinculoId=${item.vinculoId}`)}><ExternalLink size={16} /> Vínculo</Botao>}
              {item.frequenciaId && <Botao variant="outline" onClick={() => navegar(`/frequencia?frequenciaId=${item.frequenciaId}${item.dataEfetiva ? `&data=${item.dataEfetiva}` : ''}`)}><ExternalLink size={16} /> Frequência</Botao>}
            </div>
          </article>
        ))}
      </div>

      {Number(dados.totalPaginas || 0) > 1 && <div className={estilos.paginacao}><Botao variant="outline" disabled={dados.primeira} onClick={() => setPagina((p) => Math.max(0, p - 1))}>Anterior</Botao><span>Página {Number(dados.pagina || 0) + 1} de {dados.totalPaginas}</span><Botao variant="outline" disabled={dados.ultima} onClick={() => setPagina((p) => p + 1)}>Próxima</Botao></div>}

      <JanelaModal isOpen={Boolean(tratamento)} onClose={() => setTratamento(null)} title={tratamento?.acao === 'resolver' ? 'Confirmar resolução' : 'Iniciar tratamento'}>
        <div className={estilos.modalConteudo}>
          <p>A observação é opcional e será enviada ao endpoint de tratamento. O estado final continua sendo decidido pelo Java.</p>
          <label>Observação<textarea maxLength={1000} value={observacao} onChange={(e) => setObservacao(e.target.value)} placeholder="Contexto do tratamento administrativo" /></label>
          <div className={estilos.acoes}><Botao variant="outline" onClick={() => setTratamento(null)}>Cancelar</Botao><Botao onClick={confirmarTratamento}>Confirmar</Botao></div>
        </div>
      </JanelaModal>
    </EstruturaPagina>
  );
};
