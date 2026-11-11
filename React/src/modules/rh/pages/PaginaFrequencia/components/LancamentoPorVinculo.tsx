import type { VinculoElegivelFrequenciaDTO } from '../../../../../contracts/java';
import { correspondeAoContextoDaFrequencia, ehReferenciaCivilIso } from '../../../../../services/rh/ServicoSelecaoFrequencia.ts';
import { observarFalha } from '../../../../../core/http/falhaObservada';
import { mensagemFalha } from "../../../../../core/http/mensagemFalha";
import React, { useEffect, useLayoutEffect, useRef, useState } from 'react';
import { JanelaModal, Botao } from '../../../../../core/ui/EnvolucroDica.tsx';
import { useConfirmacaoSenha } from '../../../../../core/security/useConfirmacaoSenha.tsx';
import { useConsultaVigente } from '../../../../../core/hooks/useConsultaVigente.ts';
import { obterGeracaoSessao } from '../../../../../core/security/geracaoSessao.ts';
import { notificar } from '../../../../../core/events.ts';
import { ServicoVinculoContratual } from '../../../../../services/rh/ServicoVinculoContratual.ts';
import { ServicoFrequencia } from '../../../../../services/rh/ServicoFrequencia.ts';
import { gerarChaveIdempotenciaFrequencia, montarPayloadFrequenciaCanonica } from '../../../../../services/rh/ServicoDominioFrequencia.ts';
import estilos from './LancamentoPorVinculo.module.css';

const TAMANHO_PAGINA = 5;
const STATUS = [
  ['PRESENCA', 'Presença'], ['FALTA', 'Falta'], ['PRESENCA_PARCIAL', 'Presença parcial'],
  ['ATESTADO', 'Atestado'], ['FOLGA', 'Folga'], ['DESCANSO', 'Descanso'],
  ['DSR_PERDIDO', 'DSR perdido'], ['RT', 'Responsabilidade técnica (RT)'],
];
const formularioVazio = () => ({ status: '', observacao: '', justificativa: '', horasTrabalhadas: '', horarioSaida: '', horasNoturnas: '' });
const idValido = (valor: string | number | null | undefined): valor is string | number => (typeof valor === 'number' || typeof valor === 'string') && Number.isSafeInteger(Number(valor)) && Number(valor) > 0;
const mensagemErro = (erro: unknown) => mensagemFalha(erro, 'Não foi possível concluir a operação.');
const posicaoCompativel = (posicao: (import("../../../../../contracts/java").PosicaoAlocacaoTemporalDTO) | (import("../../../../../contracts/java").PosicaoAlocacaoTemporalDTO | null), vinculoId: (number | null), data: (string | null), lotacaoId: (string), jornadaId: (string)) =>
  Number(posicao?.vinculoId) === Number(vinculoId) && posicao?.dataReferencia === data
  && posicao?.situacaoTemporal === 'VIGENTE' && idValido(posicao?.alocacaoId)
  && Number(posicao?.lotacaoId) === Number(lotacaoId) && Number(posicao?.jornadaId) === Number(jornadaId);
const pessoaCompativel = (posicao: (import("../../../../../contracts/java").PosicaoPessoaDTO), vinculo: VinculoElegivelFrequenciaDTO, data: (string | null)) => {
  if (Number(posicao?.pessoaId) !== Number(vinculo.pessoaId) || posicao?.dataReferencia !== data || !Array.isArray(posicao?.vinculos)) return false;
  const correspondentes = posicao.vinculos.filter(item => Number(item?.vinculoId) === Number(vinculo.id));
  if (correspondentes.length !== 1) return false;
  const fotografia = correspondentes[0];
  return Number(fotografia.pessoaId) === Number(vinculo.pessoaId) && Number(fotografia.contratoId) === Number(vinculo.contratoId)
    && fotografia.dataReferencia === data && fotografia.estadoNegocial === 'REGISTRADO' && fotografia.situacaoTemporal === 'VIGENTE';
};

export const LancamentoPorVinculo = ({
  dataReferencia, lotacaoId, jornadaId, contratoIds = [], contratos = [], usuario,
  gestorLocal = false, podeLancar = false, bloqueado = false, dataValida = true, aoSalvar,
}: { dataReferencia: (string | null); lotacaoId: (string); jornadaId: (string); contratoIds?: (string[]); contratos?: (import("../../../../../contracts/opcoes-apresentacao").ContractView[]); usuario: (import("../../../../../contracts/auth").AuthenticatedUser | null); gestorLocal?: (boolean); podeLancar?: (boolean); bloqueado?: (boolean); dataValida?: (boolean); aoSalvar: ((origem?: { signal: AbortSignal; atual: () => boolean; }) => Promise<void>) }) => {
  const [aberto, setAberto] = useState(false);
  const [pagina, setPagina] = useState(0);
  const [contratoEscolhido, setContratoEscolhido] = useState('');
  const [lista, setLista] = useState<VinculoElegivelFrequenciaDTO[]>([]);
  const [totalPaginas, setTotalPaginas] = useState(0);
  const [totalVinculos, setTotalVinculos] = useState(0);
  const [estadoLista, setEstadoLista] = useState('inicial');
  const [erroLista, setErroLista] = useState('');
  const [selecionado, setSelecionado] = useState<VinculoElegivelFrequenciaDTO | null>(null);
  const [posicao, setPosicao] = useState<(import("../../../../../contracts/java").PosicaoAlocacaoTemporalDTO) | null>(null);
  const [consultandoPosicao, setConsultandoPosicao] = useState(false);
  const [erroSelecao, setErroSelecao] = useState('');
  const [formulario, setFormulario] = useState(formularioVazio);
  const [erroEnvio, setErroEnvio] = useState('');
  const [ocupado, setOcupado] = useState(false);
  const [intencaoFixada, setIntencaoFixada] = useState(false);
  const chaveRef = useRef('');
  const intencaoEnviadaRef = useRef<(Readonly<import("../../../../../contracts/java-requests").RequestFrequenciaDTO>) | null>(null);
  const operacaoEmCursoRef = useRef<({ signal: AbortSignal; atual: () => boolean; }) | null>(null);
  const postEmCursoRef = useRef<({ signal: AbortSignal; atual: () => boolean; }) | null>(null);
  const aoSalvarRef = useRef(aoSalvar);
  const { confirmarComSenha, ModalConfirmacao, fechar: fecharConfirmacao } = useConfirmacaoSenha();

  const contratosDoFiltro = [...new Set(contratoIds.filter(idValido).map(Number))].sort((a, b) => a - b);
  const contratoConsulta = contratosDoFiltro.length === 1 ? contratosDoFiltro[0] : Number(contratoEscolhido) || null;
  const escopoPronto = Boolean(dataReferencia && idValido(lotacaoId) && idValido(jornadaId));
  const gestaoLocalIdAtor = usuario?.perfil === 'GESTOR_LOCAL' || usuario?.perfil === 'OPERACIONAL'
    ? usuario.gestaoLocalId ?? usuario.gestaoLocal?.id ?? null : null;
  const escoposAtor = usuario?.vinculosGestao?.map(item => [item.lotacaoId ?? item.lotacao?.id, item.jornadaId ?? item.jornada?.id]);
  const contextoResposta = { dataReferencia, lotacaoId, jornadaId, contratoId: contratoConsulta, gestaoLocalId: gestaoLocalIdAtor };
  const contexto = JSON.stringify([gestaoLocalIdAtor, escoposAtor, dataReferencia, lotacaoId, jornadaId, contratosDoFiltro, usuario?.id, usuario?.perfil, usuario?.autoridade, gestorLocal, podeLancar, obterGeracaoSessao()]);
  const contextoLista = JSON.stringify([contexto, aberto, pagina, contratoConsulta]);
  const consultasLista = useConsultaVigente(contextoLista);
  const consultasPosicao = useConsultaVigente(contextoLista);
  const operacoes = useConsultaVigente(JSON.stringify([contextoLista, selecionado?.id]));

  useLayoutEffect(() => { aoSalvarRef.current = aoSalvar; }, [aoSalvar]);

  useLayoutEffect(() => {
    setAberto(false); setPagina(0); setContratoEscolhido('');
    setSelecionado(null); setPosicao(null); setFormulario(formularioVazio());
    setErroEnvio(''); setErroSelecao(''); setConsultandoPosicao(false); setOcupado(false);
    chaveRef.current = ''; intencaoEnviadaRef.current = null; setIntencaoFixada(false);
    operacaoEmCursoRef.current = null; postEmCursoRef.current = null;
    fecharConfirmacao?.();
  }, [contexto, fecharConfirmacao]);

  useEffect(() => {
    setLista([]); setTotalPaginas(0); setTotalVinculos(0); setErroLista(''); setEstadoLista('inicial');
    setSelecionado(null); setPosicao(null); setErroSelecao(''); setConsultandoPosicao(false);
    setFormulario(formularioVazio()); setErroEnvio(''); chaveRef.current = '';
    intencaoEnviadaRef.current = null; setIntencaoFixada(false);
    if (!aberto || !escopoPronto || !podeLancar || !ehReferenciaCivilIso(dataReferencia) || (contratosDoFiltro.length > 1 && !contratoConsulta)) return;
    const consulta = consultasLista.iniciar();
    setEstadoLista('carregando');
    ServicoVinculoContratual.listarElegiveisParaFrequencia({
      dataRef: dataReferencia, lotacaoId: Number(lotacaoId), jornadaId: Number(jornadaId),
      ...(contratoConsulta ? { contratoId: contratoConsulta } : {}), page: pagina, size: TAMANHO_PAGINA,
    }, { signal: consulta.signal, skipLoading: true, silentError: true, retries: 0 }).then(resposta => {
      if (!consulta.atual()) return;
      const registros = Array.isArray(resposta?.content) ? resposta.content : [];
      if (!registros.every(item => correspondeAoContextoDaFrequencia(item, contextoResposta))) {
        throw new Error('A seleção retornada não corresponde à data, gestão, contrato, lotação e jornada da consulta. Refaça a seleção.');
      }
      setLista(registros);
      setTotalPaginas(Number(resposta?.totalPages || 0)); setTotalVinculos(Number(resposta?.totalElements || 0));
      setEstadoLista('sucesso');
    }).catch((erroOriginal: unknown) => {
      const erro = observarFalha(erroOriginal);

      if (!consulta.atual()) return;
      setErroLista(mensagemErro(erro)); setEstadoLista('erro');
    });
    return consultasLista.invalidar;
  }, [contextoLista, consultasLista, aberto, escopoPronto, podeLancar, dataReferencia, contratoConsulta, pagina, contratosDoFiltro.length]);

  const fechar = () => {
    consultasLista.invalidar(); consultasPosicao.invalidar(); operacoes.invalidar();
    fecharConfirmacao?.(); setAberto(false); setOcupado(false);
    operacaoEmCursoRef.current = null;
  };

  const selecionar = async (vinculo: VinculoElegivelFrequenciaDTO) => {
    if (ocupado || operacaoEmCursoRef.current || intencaoEnviadaRef.current) return;
    const consulta = consultasPosicao.iniciar();
    operacoes.invalidar(); setSelecionado(vinculo); setPosicao(null); setErroSelecao(''); setErroEnvio('');
    setFormulario(formularioVazio()); chaveRef.current = gerarChaveIdempotenciaFrequencia('freq-vinculo');
    if (!correspondeAoContextoDaFrequencia(vinculo, contextoResposta)) {
      setErroSelecao('O vínculo retornado não possui os IDs canônicos exigidos ou não pertence ao contrato selecionado.');
      return;
    }
    setConsultandoPosicao(true);
    try {
      const opcoes = { signal: consulta.signal, skipLoading: true, silentError: true, retries: 0 };
      const [pessoa, resultado] = await Promise.all([
        ServicoVinculoContratual.consultarPosicaoPessoa(vinculo.pessoaId, { data: dataReferencia }, opcoes),
        ServicoVinculoContratual.consultarPosicaoAlocacao(vinculo.id, { data: dataReferencia }, opcoes),
      ]);
      if (!consulta.atual()) return;
      if (!pessoaCompativel(pessoa, vinculo, dataReferencia)) {
        throw new Error('A posição da Pessoa não confirma o vínculo, o contrato e a data selecionados. Confira a identidade canônica antes de lançar.');
      }
      if (!posicaoCompativel(resultado, vinculo.id, dataReferencia, lotacaoId, jornadaId)
        || Number(resultado.alocacaoId) !== Number(vinculo.alocacaoId) || Number(resultado.cargoId) !== Number(vinculo.cargoId)) {
        throw new Error('A posição não confirma o vínculo, a data e o escopo selecionados. Confira a alocação vigente antes de lançar.');
      }
      setPosicao(resultado);
    } catch (erroOriginal) {
      const erro = observarFalha(erroOriginal);

      if (consulta.atual()) setErroSelecao(mensagemErro(erro));
    } finally {
      if (consulta.atual()) setConsultandoPosicao(false);
    }
  };

  const alterarCampo = (campo: (string), valor: (string)) => {
    if (operacaoEmCursoRef.current || intencaoEnviadaRef.current) return;
    setFormulario(atual => ({ ...atual, [campo]: valor }));
  };
  const editarComoNovaOperacao = () => {
    if (ocupado || operacaoEmCursoRef.current || postEmCursoRef.current) return;
    operacoes.invalidar();
    chaveRef.current = gerarChaveIdempotenciaFrequencia('freq-vinculo');
    intencaoEnviadaRef.current = null;
    setIntencaoFixada(false); setErroEnvio('');
  };
  const enviar = async (evento: React.FormEvent<HTMLFormElement>) => {
    evento.preventDefault();
    if (operacaoEmCursoRef.current || !podeLancar || bloqueado || !dataValida) return;
    if (!selecionado || !posicaoCompativel(posicao, selecionado.id, dataReferencia, lotacaoId, jornadaId)) return;
    setErroEnvio('');
    if (!STATUS.some(([status]) => status === formulario.status)) { setErroEnvio('Selecione o status da frequência.'); return; }
    if (gestorLocal && formulario.justificativa.trim().length < 5) { setErroEnvio('Informe uma justificativa de retificação com ao menos 5 caracteres.'); return; }
    if (formulario.status === 'ATESTADO' && !formulario.observacao.trim()) { setErroEnvio('Informe a observação do atestado.'); return; }
    if (formulario.status === 'PRESENCA_PARCIAL' && (!(Number(String(formulario.horasTrabalhadas).replace(',', '.')) > 0) || formulario.observacao.trim().length < 5)) {
      setErroEnvio('Informe horas trabalhadas positivas e uma observação de ao menos 5 caracteres para a presença parcial.'); return;
    }
    const payload = intencaoEnviadaRef.current || Object.freeze(montarPayloadFrequenciaCanonica({
      linha: { vinculoContratualId: Number(selecionado.id), status: formulario.status, obs: formulario.observacao,
        ...(formulario.status === 'PRESENCA_PARCIAL' ? { horasTrabalhadas: formulario.horasTrabalhadas, horarioSaida: formulario.horarioSaida, horasNoturnas: formulario.horasNoturnas } : {}),
      },
      dataReferencia, gestorLocal, justificativaRetificacao: formulario.justificativa, chaveIdempotencia: chaveRef.current,
    }));
    const operacao = operacoes.iniciar();
    operacaoEmCursoRef.current = operacao; setOcupado(true);
    let concluida = false;
    try {
      await confirmarComSenha({
        titulo: 'Confirmar lançamento por vínculo',
        mensagem: `Vínculo ${selecionado.id}, Pessoa ${selecionado.pessoaId}, Contrato ${selecionado.contratoId}. Data ${dataReferencia}; lotação ${lotacaoId}, jornada ${jornadaId}. O Java revalidará este lançamento.`,
        acao: async senhaConfirmacao => {
          if (!operacao.atual() || concluida || postEmCursoRef.current) return false;
          postEmCursoRef.current = operacao;
          // Uma tentativa enviada conserva exatamente o comando e sua chave até
          // a pessoa declarar uma nova operação ou descartar este formulário.
          intencaoEnviadaRef.current = payload; setIntencaoFixada(true);
          try {
            await ServicoFrequencia.registrar(payload, { senhaConfirmacao, skipLoading: true, silentError: true, retries: 0 });
            if (!operacao.atual()) return false;
            concluida = true;
            notificar('success', `Frequência do vínculo ${selecionado.id} registrada pelo Java.`);
            await aoSalvarRef.current?.(operacao);
            if (!operacao.atual()) return false;
            setAberto(false);
            return true;
          } catch (erroOriginal) {
      const erro = observarFalha(erroOriginal);

            if (!operacao.atual()) return false;
            setErroEnvio(`${mensagemErro(erro)} Dados e chave de operação mantidos. Confira o resultado antes de reenviar; não houve nova tentativa automática.`);
            throw erroOriginal;
          } finally {
            if (postEmCursoRef.current === operacao) postEmCursoRef.current = null;
          }
        },
      });
    } finally {
      if (operacaoEmCursoRef.current === operacao) operacaoEmCursoRef.current = null;
      if (operacao.atual()) setOcupado(false);
    }
  };

  if (!podeLancar) return null;
  return <>
    <section className={estilos.abertura} aria-label="Lançamento por vínculo contratual">
      <div><strong>Lançamento por vínculo contratual</strong><p>Escolha um vínculo e confira sua alocação para a data e o escopo dos filtros.</p></div>
      <Botao type="button" onClick={() => { setPagina(0); setAberto(true); }} disabled={!escopoPronto || bloqueado || !dataValida}>Lançar por vínculo</Botao>
    </section>
    <JanelaModal isOpen={aberto} onClose={fechar} title="Lançar por vínculo" bloquearFechamento={ocupado}>
      <div className={estilos.conteudo}>
        <p className={estilos.contexto}><strong>Data do lançamento: {dataReferencia}</strong><span>Lotação ID: {lotacaoId} · Jornada ID: {jornadaId}</span></p>
        <p className={estilos.ajuda}>A identificação usa os IDs do vínculo escolhido. O nome cadastral atual ajuda na consulta; a matrícula e a alocação correspondem à data do lançamento.</p>
        {contratosDoFiltro.length > 1 && <label className={estilos.campo}>Contrato da listagem
          <select aria-label="Contrato da listagem" value={contratoEscolhido} disabled={ocupado || intencaoFixada} onChange={evento => { if (operacaoEmCursoRef.current || intencaoEnviadaRef.current) return; setPagina(0); setContratoEscolhido(evento.target.value); }}>
            <option value="">Selecione um contrato do filtro</option>
            {contratosDoFiltro.map(id => <option key={id} value={id}>{contratos.find(item => Number(item.id) === id)?.numeroContrato || `Contrato ${id}`} · ID {id}</option>)}
          </select>
        </label>}
        {estadoLista === 'carregando' && <p role="status">Consultando vínculos elegíveis para a data e o escopo...</p>}
        {erroLista && <p role="alert" className={estilos.erro}>{erroLista}</p>}
        {estadoLista === 'sucesso' && lista.length === 0 && <p role="status">Nenhum vínculo encontrado para este recorte.</p>}
        {lista.length > 0 && <ul className={estilos.lista}>
          {lista.map(vinculo => <li key={vinculo.id} className={Number(selecionado?.id) === Number(vinculo.id) ? estilos.selecionado : undefined}>
            <div><strong>Vínculo ID: {vinculo.id}</strong><span>Pessoa ID: {vinculo.pessoaId} · Contrato ID: {vinculo.contratoId}</span><span>Matrícula na data: {vinculo.matricula || 'Não informada'}</span>{vinculo.pessoaNome && <span>Nome cadastral atual: {vinculo.pessoaNome}</span>}</div>
            <button type="button" onClick={() => selecionar(vinculo)} disabled={ocupado || intencaoFixada} aria-pressed={Number(selecionado?.id) === Number(vinculo.id)}>Selecionar vínculo {vinculo.id}</button>
          </li>)}
        </ul>}
        {estadoLista === 'sucesso' && totalPaginas > 0 && <nav className={estilos.paginacao} aria-label="Paginação de vínculos">
          <button type="button" aria-label="Vínculos anteriores" disabled={pagina === 0 || ocupado || intencaoFixada} onClick={() => setPagina(atual => atual - 1)}>Anterior</button>
          <span>Página {pagina + 1} de {totalPaginas} · {totalVinculos} vínculo(s)</span>
          <button type="button" aria-label="Próximos vínculos" disabled={pagina + 1 >= totalPaginas || ocupado || intencaoFixada} onClick={() => setPagina(atual => atual + 1)}>Próxima</button>
        </nav>}
        {consultandoPosicao && <p role="status">Conferindo posição do vínculo selecionado...</p>}
        {erroSelecao && <p role="alert" className={estilos.erro}>{erroSelecao}</p>}
        {posicao && <form onSubmit={enviar} className={estilos.formulario}>
          <p className={estilos.validado} role="status">Posição validada para {dataReferencia}: vínculo {selecionado?.id}, alocação {posicao.alocacaoId}, lotação {posicao.lotacaoId} e jornada {posicao.jornadaId}. O Java revalida as regras ao gravar.</p>
          <fieldset disabled={ocupado || intencaoFixada} className={estilos.campos}>
            <label className={estilos.campo}>Status da frequência<select aria-label="Status da frequência" value={formulario.status} onChange={evento => alterarCampo('status', evento.target.value)} required><option value="">Selecione um status</option>{STATUS.map(([valor, rotulo]) => <option key={valor} value={valor}>{rotulo}</option>)}</select></label>
            <label className={estilos.campo}>Observação do lançamento<textarea aria-label="Observação do lançamento" value={formulario.observacao} onChange={evento => alterarCampo('observacao', evento.target.value)} rows={3} /></label>
            {formulario.status === 'PRESENCA_PARCIAL' && <div className={estilos.gradeParcial}>
              <label className={estilos.campo}>Horas trabalhadas<input aria-label="Horas trabalhadas" inputMode="decimal" value={formulario.horasTrabalhadas} onChange={evento => alterarCampo('horasTrabalhadas', evento.target.value)} required /></label>
              <label className={estilos.campo}>Horário de saída<input aria-label="Horário de saída" type="time" value={formulario.horarioSaida} onChange={evento => alterarCampo('horarioSaida', evento.target.value)} /></label>
              <label className={estilos.campo}>Horas noturnas<input aria-label="Horas noturnas" type="number" min="0" step="0.01" value={formulario.horasNoturnas} onChange={evento => alterarCampo('horasNoturnas', evento.target.value)} /></label>
            </div>}
            {gestorLocal && <label className={estilos.campo}>Justificativa da retificação<textarea aria-label="Justificativa da retificação" value={formulario.justificativa} onChange={evento => alterarCampo('justificativa', evento.target.value)} minLength={5} required rows={2} /></label>}
          </fieldset>
          {intencaoFixada && <div>
            <p className={estilos.ajuda}>Confira o resultado da tentativa anterior antes de editar. Reenviar mantém os mesmos dados e a mesma chave de operação. Para alterar o lançamento após essa conferência, declare uma nova operação.</p>
            <Botao variant="ghost" type="button" onClick={editarComoNovaOperacao} disabled={ocupado}>Editar como nova operação</Botao>
          </div>}
          {erroEnvio && <p role="alert" className={estilos.erro}>{erroEnvio}</p>}
          <div className={estilos.acoes}><Botao variant="ghost" type="button" onClick={fechar} disabled={ocupado}>Cancelar</Botao><Botao type="submit" disabled={ocupado || !formulario.status || (gestorLocal && formulario.justificativa.trim().length < 5)}>{ocupado ? 'Confirmação em andamento...' : 'Confirmar lançamento'}</Botao></div>
        </form>}
      </div>
    </JanelaModal>
    <ModalConfirmacao />
  </>;
};
