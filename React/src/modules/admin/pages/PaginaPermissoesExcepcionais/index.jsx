import React, { useCallback, useEffect, useMemo, useState } from 'react';
import { Eye, KeyRound, Plus, RefreshCw, ShieldOff } from 'lucide-react';
import { EstruturaPagina } from '../../../../core/ui/EstruturaPagina.jsx';
import { Botao, JanelaModal } from '../../../../core/ui/EnvolucroDica.jsx';
import { notificar } from '../../../../core/events.js';
import { useConfirmacaoSenha } from '../../../../core/security/useConfirmacaoSenha.jsx';
import { useAutenticacao } from '../../../auth/context.jsx';
import { podeGerenciarPermissoesExcepcionais, podeVisualizarUsosPermissaoExcepcional } from '../../../../core/security/roles.js';
import { ServicoUsuario } from '../../../../services/admin/ServicoUsuario.js';
import { ServicoContrato } from '../../../../services/admin/ServicoContrato.js';
import { ServicoOrganizacional } from '../../../../services/admin/ServicoOrganizacional.js';
import { ServicoPermissaoExcepcional, TIPOS_PERMISSAO_EXCEPCIONAL } from '../../../../services/admin/ServicoPermissaoExcepcional.js';
import estilos from './index.module.css';

const rotulo = (valor) => String(valor || '-').replaceAll('_', ' ').toLocaleLowerCase('pt-BR').replace(/(^|\s)\S/g, (m) => m.toUpperCase());
const formatarInstante = (valor) => valor ? new Date(valor).toLocaleString('pt-BR') : '-';
const inicial = () => ({ tipo: TIPOS_PERMISSAO_EXCEPCIONAL[0], usuarioBeneficiarioId: '', contratoId: '', gestaoLocalId: '', finalidade: '', justificativa: '', autorizacaoAdministrativa: '', validoAte: '' });

export const PaginaPermissoesExcepcionais = () => {
  const { user } = useAutenticacao();
  const podeGerenciar = podeGerenciarPermissoesExcepcionais(user);
  const podeVerUsos = podeVisualizarUsosPermissaoExcepcional(user);
  const { confirmarComSenha, ModalConfirmacao } = useConfirmacaoSenha();
  const [permissoes, setPermissoes] = useState([]);
  const [usuarios, setUsuarios] = useState([]);
  const [contratos, setContratos] = useState([]);
  const [gestoes, setGestoes] = useState([]);
  const [carregando, setCarregando] = useState(false);
  const [erro, setErro] = useState(null);
  const [formAberto, setFormAberto] = useState(false);
  const [form, setForm] = useState(inicial);
  const [revogando, setRevogando] = useState(null);
  const [motivoRevogacao, setMotivoRevogacao] = useState('');
  const [usos, setUsos] = useState(null);

  const carregar = useCallback(async () => {
    const controlador = new AbortController();
    setCarregando(true);
    setErro(null);
    try {
      const lista = await ServicoPermissaoExcepcional.listar({ signal: controlador.signal });
      setPermissoes(Array.isArray(lista) ? lista : []);
      if (podeGerenciar) {
        const [listaUsuarios, listaContratos, listaGestoes] = await Promise.all([
          ServicoUsuario.listarTodos({ signal: controlador.signal }),
          ServicoContrato.listarTodos({ signal: controlador.signal }),
          ServicoOrganizacional.listarGestoesLocais({}, { signal: controlador.signal }),
        ]);
        setUsuarios(Array.isArray(listaUsuarios) ? listaUsuarios : []);
        setContratos(Array.isArray(listaContratos) ? listaContratos : []);
        setGestoes(Array.isArray(listaGestoes) ? listaGestoes : []);
      }
    } catch (falha) {
      if (!falha?.canceled) setErro(falha);
    } finally {
      if (!controlador.signal.aborted) setCarregando(false);
    }
    return () => controlador.abort();
  }, [podeGerenciar]);

  useEffect(() => { void carregar(); }, [carregar]);

  const conceder = async () => {
    if (!form.usuarioBeneficiarioId || !form.finalidade.trim() || !form.justificativa.trim() || !form.autorizacaoAdministrativa.trim() || !form.validoAte) {
      notificar('warning', 'Preencha beneficiário, finalidade, justificativa, autorização e validade.');
      return;
    }
    const validoAte = new Date(form.validoAte);
    if (!Number.isFinite(validoAte.getTime())) {
      notificar('warning', 'Informe uma validade válida.');
      return;
    }
    const payload = {
      tipo: form.tipo,
      usuarioBeneficiarioId: Number(form.usuarioBeneficiarioId),
      contratoId: form.contratoId ? Number(form.contratoId) : null,
      gestaoLocalId: form.gestaoLocalId ? Number(form.gestaoLocalId) : null,
      finalidade: form.finalidade.trim(),
      justificativa: form.justificativa.trim(),
      autorizacaoAdministrativa: form.autorizacaoAdministrativa.trim(),
      validoAte: validoAte.toISOString(),
    };
    await confirmarComSenha({
      titulo: 'Conceder permissão excepcional',
      mensagem: 'A elegibilidade do beneficiário e do escopo será validada integralmente pelo Java.',
      acao: async (senhaConfirmacao) => {
        await ServicoPermissaoExcepcional.conceder(payload, { senhaConfirmacao });
        setFormAberto(false);
        setForm(inicial());
        notificar('success', 'Permissão excepcional concedida e auditada pelo Java.');
        await carregar();
      },
    });
  };

  const revogar = async () => {
    if (!revogando?.id || !motivoRevogacao.trim()) {
      notificar('warning', 'Informe o motivo da revogação.');
      return;
    }
    await confirmarComSenha({
      titulo: 'Revogar permissão excepcional',
      mensagem: 'A revogação interrompe novos usos após a validação do Java.',
      acao: async (senhaConfirmacao) => {
        await ServicoPermissaoExcepcional.revogar(revogando.id, motivoRevogacao.trim(), { senhaConfirmacao });
        setRevogando(null);
        setMotivoRevogacao('');
        notificar('success', 'Permissão revogada.');
        await carregar();
      },
    });
  };

  const abrirUsos = async (permissao) => {
    try {
      const lista = await ServicoPermissaoExcepcional.listarUsos(permissao.id);
      setUsos({ permissao, registros: Array.isArray(lista) ? lista : [] });
    } catch (falha) {
      notificar('error', falha?.message || 'Não foi possível consultar os usos da permissão.');
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
        acoes={<><Botao variant="outline" onClick={carregar}><RefreshCw size={16} /> Atualizar</Botao>{podeGerenciar && <Botao onClick={() => { setForm(inicial()); setFormAberto(true); }}><Plus size={16} /> Nova permissão</Botao>}</>}
      >
        <div className={estilos.resumo}><article><span>Total visível</span><strong>{permissoes.length}</strong></article><article><span>Ativas segundo o Java</span><strong>{ativos}</strong></article><article><span>Inativas/expiradas</span><strong>{permissoes.length - ativos}</strong></article></div>
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
          <label>Tipo<select value={form.tipo} onChange={(e) => setForm((a) => ({ ...a, tipo: e.target.value }))}>{TIPOS_PERMISSAO_EXCEPCIONAL.map((tipo) => <option key={tipo} value={tipo}>{rotulo(tipo)}</option>)}</select></label>
          <label>Beneficiário<select value={form.usuarioBeneficiarioId} onChange={(e) => setForm((a) => ({ ...a, usuarioBeneficiarioId: e.target.value }))} required><option value="">Selecione...</option>{usuarios.map((item) => <option key={item.id} value={item.id}>{item.nome || item.username || item.matricula || `Usuário ${item.id}`} · {rotulo(item.perfil)}</option>)}</select></label>
          <label>Contrato (quando aplicável)<select value={form.contratoId} onChange={(e) => setForm((a) => ({ ...a, contratoId: e.target.value }))}><option value="">Sem contrato específico</option>{contratos.map((item) => <option key={item.id} value={item.id}>{item.numeroContrato || item.numero || item.nome || `Contrato ${item.id}`}</option>)}</select></label>
          <label>Gestão Local (quando aplicável)<select value={form.gestaoLocalId} onChange={(e) => setForm((a) => ({ ...a, gestaoLocalId: e.target.value }))}><option value="">Sem Gestão Local específica</option>{gestoes.map((item) => <option key={item.id} value={item.id}>{item.nome || item.sigla || `Gestão Local ${item.id}`}</option>)}</select></label>
          <label>Validade<input type="datetime-local" value={form.validoAte} onChange={(e) => setForm((a) => ({ ...a, validoAte: e.target.value }))} required /></label>
          <label className={estilos.largo}>Finalidade<textarea value={form.finalidade} onChange={(e) => setForm((a) => ({ ...a, finalidade: e.target.value }))} required /></label>
          <label className={estilos.largo}>Justificativa<textarea value={form.justificativa} onChange={(e) => setForm((a) => ({ ...a, justificativa: e.target.value }))} required /></label>
          <label className={estilos.largo}>Autorização administrativa<textarea value={form.autorizacaoAdministrativa} onChange={(e) => setForm((a) => ({ ...a, autorizacaoAdministrativa: e.target.value }))} required /></label>
          <p className={estilos.nota}>O React não tenta inferir quais combinações de tipo, perfil, contrato ou Gestão Local são válidas. Essa regra permanece exclusivamente no serviço Java.</p>
          <div className={`${estilos.acoes} ${estilos.largo}`}><Botao variant="outline" onClick={() => setFormAberto(false)}>Cancelar</Botao><Botao onClick={conceder}>Conceder</Botao></div>
        </div>
      </JanelaModal>

      <JanelaModal isOpen={Boolean(revogando)} onClose={() => setRevogando(null)} title="Revogar permissão">
        <div className={estilos.modalSimples}><p>{rotulo(revogando?.tipo)}</p><label>Motivo<textarea value={motivoRevogacao} onChange={(e) => setMotivoRevogacao(e.target.value)} required /></label><div className={estilos.acoes}><Botao variant="outline" onClick={() => setRevogando(null)}>Cancelar</Botao><Botao onClick={revogar}>Revogar</Botao></div></div>
      </JanelaModal>

      <JanelaModal isOpen={Boolean(usos)} onClose={() => setUsos(null)} title="Usos auditados da permissão" maxWidth="max-w-2xl">
        <div className={estilos.listaUsos}>{usos?.registros?.length ? usos.registros.map((uso) => <article key={uso.id}><strong>{uso.tipoRecurso || 'Recurso'} {uso.recursoId ? `#${uso.recursoId}` : ''}</strong><span>{uso.finalidade}</span><small>{uso.usuario || `Usuário #${uso.usuarioId}`} · {formatarInstante(uso.usadoEm)}</small></article>) : <p>Nenhum uso registrado pelo Java.</p>}</div>
      </JanelaModal>
    </>
  );
};
