import type { ObservedFailure } from "../../../../core/http/falhaObservada";
import type { RequestConcessaoPermissaoExcepcionalRequisicao } from '../../../../contracts/java-requests';
import { observarFalha } from '../../../../core/http/falhaObservada';
import React, { useCallback, useLayoutEffect, useMemo, useState } from 'react';
import { Eye, KeyRound, Plus, RefreshCw, ShieldOff } from 'lucide-react';
import { EstruturaPagina } from '../../../../core/ui/EstruturaPagina.tsx';
import { Botao, JanelaModal } from '../../../../core/ui/EnvolucroDica.tsx';
import { notificar } from '../../../../core/events.ts';
import { useConfirmacaoSenha } from '../../../../core/security/useConfirmacaoSenha.tsx';
import { useConsultaVigente } from '../../../../core/hooks/useConsultaVigente.ts';
import { criarErroSessaoObsoleta } from '../../../../core/security/geracaoSessao.ts';
import { useAutenticacao } from '../../../auth/context.tsx';
import { podeGerenciarPermissoesExcepcionais, podeVisualizarUsosPermissaoExcepcional } from '../../../../core/security/roles.ts';
import { ServicoUsuario } from '../../../../services/admin/ServicoUsuario.ts';
import { ServicoContrato } from '../../../../services/admin/ServicoContrato.ts';
import { ServicoOrganizacional } from '../../../../services/admin/ServicoOrganizacional.ts';
import { ServicoPermissaoExcepcional, TIPOS_PERMISSAO_EXCEPCIONAL } from '../../../../services/admin/ServicoPermissaoExcepcional.ts';
import estilos from './index.module.css';

const rotulo = (valor: (import("../../../../contracts/java").TipoPermissaoExcepcional | null | undefined) | (import("../../../../contracts/java").PerfilAcesso | null) | (string)) => String(valor || '-').replaceAll('_', ' ').toLocaleLowerCase('pt-BR').replace(/(^|\s)\S/g, (m) => m.toUpperCase());
const formatarInstante = (valor: (import("../../../../contracts/shared").Instant | null)) => valor ? new Date(valor).toLocaleString('pt-BR') : '-';
const inicial = () => ({ tipo: TIPOS_PERMISSAO_EXCEPCIONAL[0], escopo: 'geral', ocorrenciaId: '', usuarioBeneficiarioId: '', contratoId: '', gestaoLocalId: '', finalidade: '', justificativa: '', autorizacaoAdministrativa: '', validoAte: '' });

export const PaginaPermissoesExcepcionais = () => {
  const { user } = useAutenticacao();
  const podeGerenciar = podeGerenciarPermissoesExcepcionais(user);
  const podeVerUsos = podeVisualizarUsosPermissaoExcepcional(user);
  const contextoSessao = `${user?.id ?? ''}:${user?.perfil ?? ''}`;
  const consultaLista = useConsultaVigente(contextoSessao);
  const consultaUsos = useConsultaVigente(contextoSessao);
  const mutacao = useConsultaVigente(contextoSessao);
  const { confirmarComSenha, ModalConfirmacao } = useConfirmacaoSenha();
  const [permissoes, setPermissoes] = useState<(import("../../../../contracts/java").PermissaoExcepcionalDTO[])>([]);
  const [usuarios, setUsuarios] = useState<(import("../../../../contracts/opcoes-apresentacao").UserSummaryView[])>([]);
  const [contratos, setContratos] = useState<(import("../../../../contracts/opcoes-apresentacao").ContractView[])>([]);
  const [gestoes, setGestoes] = useState<(import("../../../../contracts/opcoes-apresentacao").LocalManagementView[])>([]);
  const [carregando, setCarregando] = useState(false);
  const [erro, setErro] = useState<ObservedFailure | null>(null);
  const [formAberto, setFormAberto] = useState(false);
  const [form, setForm] = useState(inicial);
  const [erroOcorrencia, setErroOcorrencia] = useState('');
  const [revogando, setRevogando] = useState<(import("../../../../contracts/java").PermissaoExcepcionalDTO) | null>(null);
  const [motivoRevogacao, setMotivoRevogacao] = useState('');
  const [usos, setUsos] = useState<({ permissao: import("../../../../contracts/java").PermissaoExcepcionalDTO; registros: import("../../../../contracts/java").UsoPermissaoExcepcionalDTO[]; }) | null>(null);

  const carregar = useCallback(async () => {
    const consulta = consultaLista.iniciar();
    setCarregando(true);
    setErro(null);
    try {
      const lista = await ServicoPermissaoExcepcional.listar({ signal: consulta.signal });
      if (!consulta.atual()) return;
      setPermissoes(Array.isArray(lista) ? lista : []);
      if (podeGerenciar) {
        const [listaUsuarios, listaContratos, listaGestoes] = await Promise.all([
          ServicoUsuario.listarTodos({ signal: consulta.signal }),
          ServicoContrato.listarTodos({ signal: consulta.signal }),
          ServicoOrganizacional.listarGestoesLocais({}, { signal: consulta.signal }),
        ]);
        if (!consulta.atual()) return;
        setUsuarios(Array.isArray(listaUsuarios) ? listaUsuarios : []);
        setContratos(Array.isArray(listaContratos) ? listaContratos : []);
        setGestoes(Array.isArray(listaGestoes) ? listaGestoes : []);
      }
    } catch (falhaOriginal) {
      const falha = observarFalha(falhaOriginal);

      if (consulta.atual() && !falha?.canceled) setErro(falha);
    } finally {
      if (consulta.atual()) setCarregando(false);
    }
  }, [podeGerenciar, consultaLista]);

  useLayoutEffect(() => {
    setPermissoes([]); setUsuarios([]); setContratos([]); setGestoes([]);
    setFormAberto(false); setForm(inicial()); setErroOcorrencia('');
    setRevogando(null); setMotivoRevogacao(''); setUsos(null);
    void carregar();
    return () => { consultaLista.invalidar(); consultaUsos.invalidar(); mutacao.invalidar(); };
  }, [carregar, contextoSessao, consultaLista, consultaUsos, mutacao]);

  const conceder = async () => {
    const especifica = form.escopo === 'ocorrencia';
    const ocorrenciaId = Number(form.ocorrenciaId);
    if (especifica && (!/^[1-9]\d*$/.test(form.ocorrenciaId) || !Number.isSafeInteger(ocorrenciaId))) {
      setErroOcorrencia('Informe um ID de ocorrência inteiro positivo válido.');
      return;
    }
    if (!form.usuarioBeneficiarioId || !form.finalidade.trim() || !form.justificativa.trim() || !form.autorizacaoAdministrativa.trim() || !form.validoAte) {
      notificar('warning', 'Preencha beneficiário, finalidade, justificativa, autorização e validade.');
      return;
    }
    const validoAte = new Date(form.validoAte);
    if (!Number.isFinite(validoAte.getTime())) {
      notificar('warning', 'Informe uma validade válida.');
      return;
    }
    const payload: RequestConcessaoPermissaoExcepcionalRequisicao = {
      tipo: especifica ? 'ACESSO_DADOS_PESSOAIS_RESTRITOS' : form.tipo,
      usuarioBeneficiarioId: Number(form.usuarioBeneficiarioId),
      contratoId: !especifica && form.contratoId ? Number(form.contratoId) : null,
      gestaoLocalId: !especifica && form.gestaoLocalId ? Number(form.gestaoLocalId) : null,
      ...(especifica ? { ocorrenciaId } : {}),
      finalidade: form.finalidade.trim(),
      justificativa: form.justificativa.trim(),
      autorizacaoAdministrativa: form.autorizacaoAdministrativa.trim(),
      validoAte: validoAte.toISOString(),
    };
    const operacao = mutacao.iniciar();
    await confirmarComSenha({
      titulo: 'Conceder permissão excepcional',
      mensagem: especifica ? 'Autorizar somente a leitura restrita desta ocorrência. O contexto histórico de todos os dias será validado antes da concessão.' : 'A elegibilidade do beneficiário e do escopo será validada antes da concessão.',
      acao: async (senhaConfirmacao) => {
        if (!operacao.atual()) throw criarErroSessaoObsoleta();
        await ServicoPermissaoExcepcional.conceder(payload, { senhaConfirmacao, signal: operacao.signal });
        if (!operacao.atual()) return;
        setFormAberto(false);
        setForm(inicial());
        notificar('success', 'Permissão excepcional concedida e auditada.');
        await carregar();
      },
    });
  };

  const revogar = async () => {
    if (!revogando?.id || !motivoRevogacao.trim()) {
      notificar('warning', 'Informe o motivo da revogação.');
      return;
    }
    const operacao = mutacao.iniciar();
    await confirmarComSenha({
      titulo: 'Revogar permissão excepcional',
      mensagem: 'A revogação interrompe novos usos após a confirmação.',
      acao: async (senhaConfirmacao) => {
        if (!operacao.atual()) throw criarErroSessaoObsoleta();
        await ServicoPermissaoExcepcional.revogar(revogando.id, motivoRevogacao.trim(), { senhaConfirmacao, signal: operacao.signal });
        if (!operacao.atual()) return;
        setRevogando(null);
        setMotivoRevogacao('');
        notificar('success', 'Permissão revogada.');
        await carregar();
      },
    });
  };

  const abrirUsos = async (permissao: (import("../../../../contracts/java").PermissaoExcepcionalDTO)) => {
    const consulta = consultaUsos.iniciar();
    setUsos(null);
    try {
      const lista = await ServicoPermissaoExcepcional.listarUsos(permissao.id, { signal: consulta.signal });
      if (!consulta.atual()) return;
      setUsos({ permissao, registros: Array.isArray(lista) ? lista : [] });
    } catch (falhaOriginal) {
      const falha = observarFalha(falhaOriginal);

      if (consulta.atual() && !falha.canceled) notificar('error', falha.message || 'Não foi possível consultar os usos da permissão.');
    }
  };

  const ativos = useMemo(() => permissoes.filter((item) => item.ativa === true).length, [permissoes]);

  return (
    <>
      <ModalConfirmacao />
      <EstruturaPagina
        titulo="Permissões excepcionais"
        subtitulo="Concessões explícitas, temporárias, revogáveis e auditáveis. O servidor determina elegibilidade, escopo, expiração e autorização efetiva."
        carregando={carregando}
        erro={erro}
        aoTentarNovamente={carregar}
        vazio={!carregando && !erro && permissoes.length === 0}
        acoes={<><Botao variant="outline" onClick={carregar}><RefreshCw size={16} /> Atualizar</Botao>{podeGerenciar && <Botao onClick={() => { setForm(inicial()); setErroOcorrencia(''); setFormAberto(true); }}><Plus size={16} /> Nova permissão</Botao>}</>}
      >
        <div className={estilos.resumo}><article><span>Total visível</span><strong>{permissoes.length}</strong></article><article><span>Ativas</span><strong>{ativos}</strong></article><article><span>Inativas/expiradas</span><strong>{permissoes.length - ativos}</strong></article></div>
        <div className={estilos.lista}>
          {permissoes.map((item) => (
            <article key={item.id} className={estilos.cartao}>
              <div className={estilos.topo}><div><KeyRound size={18} /><strong>{rotulo(item.tipo)}</strong><span className={item.ativa ? estilos.ativa : estilos.inativa}>{item.ativa ? 'Ativa' : 'Inativa'}</span></div><small>#{item.id}</small></div>
              <dl className={estilos.detalhes}>
                <div><dt>Beneficiário</dt><dd>{item.usuarioBeneficiario || `#${item.usuarioBeneficiarioId}`}</dd></div>
                <div><dt>Perfil</dt><dd>{rotulo(item.perfilBeneficiario)}</dd></div>
                <div><dt>Validade</dt><dd>{formatarInstante(item.validoAte)}</dd></div>
                <div><dt>Concedida em</dt><dd>{formatarInstante(item.concedidoEm)}</dd></div>
                <div><dt>Contrato</dt><dd>{item.contrato || (item.contratoId ? `#${item.contratoId}` : '-')}</dd></div>
                <div><dt>Gestão Local</dt><dd>{item.gestaoLocal || (item.gestaoLocalId ? `#${item.gestaoLocalId}` : '-')}</dd></div>
                {item.ocorrenciaId && <div><dt>Ocorrência · leitura restrita</dt><dd>#{item.ocorrenciaId}</dd></div>}
              </dl>
              <div className={estilos.textos}><p><strong>Finalidade:</strong> {item.finalidade}</p><p><strong>Justificativa:</strong> {item.justificativa}</p><p><strong>Autorização:</strong> {item.autorizacaoAdministrativa}</p>{item.motivoRevogacao && <p><strong>Revogação:</strong> {item.motivoRevogacao}</p>}</div>
              <div className={estilos.acoes}>
                {podeVerUsos && <Botao variant="outline" onClick={() => abrirUsos(item)}><Eye size={16} /> Usos auditados</Botao>}
                {podeGerenciar && item.ativa && <Botao variant="outline" onClick={() => { setRevogando(item); setMotivoRevogacao(''); }}><ShieldOff size={16} /> Revogar</Botao>}
              </div>
            </article>
          ))}
        </div>
      </EstruturaPagina>

      <JanelaModal isOpen={formAberto} onClose={() => setFormAberto(false)} title="Conceder permissão excepcional" maxWidth="max-w-2xl">
        <div className={estilos.formulario}>
          <label>Escopo da concessão<select value={form.escopo} onChange={(e) => { const escopo = e.target.value; setErroOcorrencia(''); setForm(a => ({ ...a, escopo, usuarioBeneficiarioId: '', contratoId: '', gestaoLocalId: '', ocorrenciaId: '', tipo: escopo === 'ocorrencia' ? 'ACESSO_DADOS_PESSOAIS_RESTRITOS' : TIPOS_PERMISSAO_EXCEPCIONAL[0] })); }}><option value="geral">Contrato ou Gestão Local</option><option value="ocorrencia">Ocorrência específica</option></select></label>
          <label>Tipo<select value={form.tipo} disabled={form.escopo === 'ocorrencia'} onChange={(e) => setForm((a) => ({ ...a, tipo: TIPOS_PERMISSAO_EXCEPCIONAL.find((tipo) => tipo === e.target.value) ?? form.tipo }))}>{TIPOS_PERMISSAO_EXCEPCIONAL.map((tipo) => <option key={tipo} value={tipo}>{rotulo(tipo)}</option>)}</select></label>
          <label>Beneficiário<select value={form.usuarioBeneficiarioId} onChange={(e) => setForm((a) => ({ ...a, usuarioBeneficiarioId: e.target.value }))} required><option value="">Selecione...</option>{usuarios.filter(item => form.escopo !== 'ocorrencia' || item.active === true).map((item) => <option key={item.id} value={(item.id) ?? undefined}>{item.nome || item.username || item.matricula || `Usuário ${item.id}`} · {rotulo(item.perfil)}</option>)}</select></label>
          {form.escopo === 'ocorrencia' ? <>
            <label>Ocorrência (ID)<input inputMode="numeric" value={form.ocorrenciaId} aria-invalid={Boolean(erroOcorrencia)} aria-describedby="permissao-ocorrencia-ajuda permissao-ocorrencia-erro" onChange={e => { setErroOcorrencia(''); setForm(a => ({ ...a, ocorrenciaId: e.target.value })); }} required /></label>
            <p id="permissao-ocorrencia-ajuda" className={estilos.nota}>Somente leitura restrita da ocorrência indicada. Não permite alterar registros. O contexto histórico de todos os dias deve estar comprovado.</p>
            {erroOcorrencia && <p id="permissao-ocorrencia-erro" role="alert" className={estilos.largo}>{erroOcorrencia}</p>}
          </> : <>
            <label>Contrato (quando aplicável)<select value={form.contratoId} onChange={(e) => setForm((a) => ({ ...a, contratoId: e.target.value }))}><option value="">Sem contrato específico</option>{contratos.map((item) => <option key={item.id} value={(item.id) ?? undefined}>{item.numeroContrato || item.numero || item.nome || `Contrato ${item.id}`}</option>)}</select></label>
            <label>Gestão Local (quando aplicável)<select value={form.gestaoLocalId} onChange={(e) => setForm((a) => ({ ...a, gestaoLocalId: e.target.value }))}><option value="">Sem Gestão Local específica</option>{gestoes.map((item) => <option key={item.id} value={(item.id) ?? undefined}>{item.nome || item.sigla || `Gestão Local ${item.id}`}</option>)}</select></label>
          </>}
          <label>Validade<input type="datetime-local" value={form.validoAte} onChange={(e) => setForm((a) => ({ ...a, validoAte: e.target.value }))} required /></label>
          <label className={estilos.largo}>Finalidade<textarea value={form.finalidade} onChange={(e) => setForm((a) => ({ ...a, finalidade: e.target.value }))} required /></label>
          <label className={estilos.largo}>Justificativa<textarea value={form.justificativa} onChange={(e) => setForm((a) => ({ ...a, justificativa: e.target.value }))} required /></label>
          <label className={estilos.largo}>Autorização administrativa<textarea value={form.autorizacaoAdministrativa} onChange={(e) => setForm((a) => ({ ...a, autorizacaoAdministrativa: e.target.value }))} required /></label>
          <p className={estilos.nota}>A concessão depende da elegibilidade atual, do escopo e das provas históricas. A validade usa o horário deste computador.</p>
          <div className={`${estilos.acoes} ${estilos.largo}`}><Botao variant="outline" onClick={() => setFormAberto(false)}>Cancelar</Botao><Botao onClick={conceder}>Conceder</Botao></div>
        </div>
      </JanelaModal>

      <JanelaModal isOpen={Boolean(revogando)} onClose={() => setRevogando(null)} title="Revogar permissão">
        <div className={estilos.modalSimples}><p>{rotulo(revogando?.tipo)}</p><label>Motivo<textarea value={motivoRevogacao} onChange={(e) => setMotivoRevogacao(e.target.value)} required /></label><div className={estilos.acoes}><Botao variant="outline" onClick={() => setRevogando(null)}>Cancelar</Botao><Botao onClick={revogar}>Revogar</Botao></div></div>
      </JanelaModal>

      <JanelaModal isOpen={Boolean(usos)} onClose={() => { consultaUsos.invalidar(); setUsos(null); }} title="Usos auditados da permissão" maxWidth="max-w-2xl">
        <div className={estilos.listaUsos}>{usos?.registros?.length ? usos.registros.map((uso) => <article key={uso.id}><strong>{uso.tipoRecurso || 'Recurso'} {uso.recursoId ? `#${uso.recursoId}` : ''}</strong><span>{uso.finalidade}</span><small>{uso.usuario || `Usuário #${uso.usuarioId}`} · {formatarInstante(uso.usadoEm)}</small></article>) : <p>Nenhum uso registrado.</p>}</div>
      </JanelaModal>
    </>
  );
};
