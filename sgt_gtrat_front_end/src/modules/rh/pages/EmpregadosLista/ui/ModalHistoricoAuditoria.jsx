import React, { useEffect, useMemo, useState } from 'react';
import { ServicoAuditoria } from '../../../../../services/admin/ServicoAuditoria.js';
import { ServicoJornada } from '../../../../../services/admin/ServicoJornada.js';
import { ServicoEmpregado } from '../../../../../services/rh/ServicoEmpregado.js';
import { ServicoOcorrencia } from '../../../../../services/rh/ServicoOcorrencia.js';
import { Botao, JanelaModal, CampoEntrada } from '../../../../../core/ui/EnvolucroDica';
import { Eye, FileText, RefreshCcw, ChevronLeft, ChevronRight } from 'lucide-react';
import { CONFIG } from '../../../../../core/config';
import estilos from './ModalHistoricoAuditoria.module.css';

const ROTULOS_CAMPOS = {
  id: 'ID', nome: 'Nome', cpf: 'CPF', matricula: 'Matrícula', telefone: 'Telefone', email: 'E-mail',
  ativo: 'Ativo', status: 'Status', 'cargo.id': 'Cargo', cargo: 'Cargo', cargoId: 'Cargo', idCargo: 'Cargo',
  'lotacao.id': 'Lotação', lotacao: 'Lotação', lotacaoId: 'Lotação', idLotacao: 'Lotação',
  'turno.id': 'Turno', turno: 'Turno', jornada: 'Turno', 'jornada.id': 'Turno', jornadaId: 'Turno', idJornada: 'Turno',
  motivo: 'Motivo', dataOcorrencia: 'Data da Ocorrência', tipoRo: 'Tipo RO', numeroSei: 'Nº SEI',
  diasSuspensao: 'Dias de Suspensão', diasAfastamento: 'Dias de Afastamento', observacao: 'Observação', quantidadeDias: 'Quantidade de Dias'
};

const TIPO_CAMPO = {
  ativo: 'active', enabled: 'active', status: 'status', statusEmpregado: 'status', cargo: 'cargo',
  'cargo.id': 'cargo', cargoId: 'cargo', idCargo: 'cargo', lotacao: 'lotacao', 'lotacao.id': 'lotacao',
  lotacaoId: 'lotacao', idLotacao: 'lotacao', turno: 'turno', 'turno.id': 'turno', jornada: 'turno',
  'jornada.id': 'turno', jornadaId: 'turno', idJornada: 'turno',
};

const rotuloAcao = (acao, tipoEntidade) => {
  const a = String(acao || '').toUpperCase();
  const e = String(tipoEntidade || '').toUpperCase();
  let prefixo = '';
  if (e.includes('OCORRENCIA')) prefixo = '[Ocorrência] ';
  if (!a) return '—';
  if (a.includes('CREATE') || a.includes('CAD')) return prefixo + 'Cadastro';
  if (a.includes('UPDATE') || a.includes('EDIT')) return prefixo + 'Alteração';
  if (a.includes('DELETE') || a.includes('REM')) return prefixo + 'Remoção';
  return prefixo + acao;
};

const rotuloParaCampo = (campo) => {
  if (!campo) return 'Campo';
  const chave = String(campo);
  if (ROTULOS_CAMPOS[chave]) return ROTULOS_CAMPOS[chave];
  const final = chave.split('.').pop();
  if (ROTULOS_CAMPOS[final]) return ROTULOS_CAMPOS[final];
  return chave;
};

const normalizarChaveCampo = (campo) => {
  if (!campo) return '';
  const chave = String(campo);
  if (TIPO_CAMPO[chave]) return chave;
  const final = chave.split('.').pop();
  return final;
};

const tentarExtrairId = (valor) => {
  if (valor === undefined || valor === null) return null;
  if (typeof valor === 'number') return valor;
  if (typeof valor === 'string') { const combinacao = valor.match(/^#?(\d+)$/); if (combinacao) return Number(combinacao[1]); }
  if (typeof valor === 'object' && valor.id != null) return Number(valor.id);
  return null;
};

const formatarBooleanoParaStatus = (booleano) => {
  if (booleano === true) return 'Ativo';
  if (booleano === false) return 'Inativo';
  return '—';
};

const classificarMudanca = (campo) => {
  const chave = TIPO_CAMPO[normalizarChaveCampo(campo)] ?? TIPO_CAMPO[campo] ?? '';
  if (chave === 'active') return 1; if (chave === 'status') return 2;
  if (chave === 'cargo') return 3; if (chave === 'turno') return 4; if (chave === 'lotacao') return 5;
  return 99;
};

const humanizarMudanca = (mudanca, referencias, linha) => {
  if (!mudanca) return null;
  const ehOcorrencia = linha?.entityType === 'Ocorrencia';

  if (mudanca.field === '__create__') return { type: 'create', text: ehOcorrencia ? 'Ocorrência registrada.' : 'Colaborador cadastrado no sistema.' };
  if (mudanca.field === '__delete__') return { type: 'delete', text: ehOcorrencia ? 'Ocorrência excluída.' : 'Colaborador removido do sistema.' };

  const campoBruto = mudanca.field;
  const chaveCampo = normalizarChaveCampo(campoBruto);
  const tipo = TIPO_CAMPO[chaveCampo] || TIPO_CAMPO[campoBruto] || null;

  const idAntigo = tentarExtrairId(mudanca.from);
  const idNovo = tentarExtrairId(mudanca.to);

  if (tipo === 'active') {
    const antigo = (typeof mudanca.from === 'boolean') ? mudanca.from : (String(mudanca.from).toLowerCase() === 'true');
    const novo = (typeof mudanca.to === 'boolean') ? mudanca.to : (String(mudanca.to).toLowerCase() === 'true');
    if (antigo === true && novo === false) return { type: 'active', text: 'Colaborador foi desativado (ficou inativo).' };
    if (antigo === false && novo === true) return { type: 'active', text: 'Colaborador foi reativado (voltou a ficar ativo).' };
    return { type: 'active', text: `Situação: ${formatarBooleanoParaStatus(antigo)} → ${formatarBooleanoParaStatus(novo)}` };
  }

  if (tipo === 'status') {
    const antigo = formatarValor(mudanca.from); const novo = formatarValor(mudanca.to);
    if (antigo !== '—' && novo !== '—') return { type: 'status', text: `Status do colaborador: ${antigo} → ${novo}` };
    return { type: 'status', text: 'Status do colaborador foi alterado.' };
  }

  if (tipo === 'cargo') {
    const nomeAntigo = idAntigo != null ? (referencias?.cargos?.get(idAntigo) || `#${idAntigo}`) : formatarValor(mudanca.from);
    const nomeNovo = idNovo != null ? (referencias?.cargos?.get(idNovo) || `#${idNovo}`) : formatarValor(mudanca.to);
    return { type: 'cargo', text: `Cargo alterado: ${nomeAntigo} → ${nomeNovo}` };
  }
  if (tipo === 'turno') {
    const nomeAntigo = idAntigo != null ? (referencias?.jornadas?.get(idAntigo) || `#${idAntigo}`) : formatarValor(mudanca.from);
    const nomeNovo = idNovo != null ? (referencias?.jornadas?.get(idNovo) || `#${idNovo}`) : formatarValor(mudanca.to);
    return { type: 'turno', text: `Turno alterado: ${nomeAntigo} → ${nomeNovo}` };
  }
  if (tipo === 'lotacao') {
    const nomeAntigo = idAntigo != null ? (referencias?.lotacoes?.get(idAntigo) || `#${idAntigo}`) : formatarValor(mudanca.from);
    const nomeNovo = idNovo != null ? (referencias?.lotacoes?.get(idNovo) || `#${idNovo}`) : formatarValor(mudanca.to);
    return { type: 'lotacao', text: `Lotação alterada: ${nomeAntigo} → ${nomeNovo}` };
  }

  const rotulo = rotuloParaCampo(campoBruto);
  const antigo = mudanca.from === undefined ? null : formatarValor(mudanca.from);
  const novo = mudanca.to === undefined ? null : formatarValor(mudanca.to);
  if (antigo === null && novo === null) return { type: 'generic', text: `${rotulo} foi alterado.` };
  if (antigo === null) return { type: 'generic', text: `${rotulo} definido para ${novo}.` };
  if (novo === null) return { type: 'generic', text: `${rotulo} alterado (valor anterior: ${antigo}).` };
  return { type: 'generic', text: `${rotulo}: ${antigo} → ${novo}` };
};

const formatarValor = (valor) => {
  if (valor === undefined || valor === null) return '—';
  if (typeof valor === 'boolean') return valor ? 'Sim' : 'Não';
  if (typeof valor === 'number') return String(valor);
  if (typeof valor === 'string') {
    const v = valor.trim();
    if (!v) return '—';
    const d = new Date(v);
    if (!Number.isNaN(d.getTime()) && /^\d{4}-\d{2}-\d{2}/.test(v)) return d.toLocaleDateString('pt-BR');
    return v;
  }
  if (Array.isArray(valor)) return !valor.length ? '—' : valor.map(formatarValor).join(', ');
  if (typeof valor === 'object') {
    const pareceNome = valor.nome ?? valor.descricao ?? valor.name ?? valor.title;
    if (pareceNome) return String(pareceNome);
    if (valor.id != null) return `#${valor.id}`;
    try { return JSON.stringify(valor); } catch { return String(valor); }
  }
  return String(valor);
};

const formatarDataHora = (iso) => {
  if (!iso) return '—';
  const d = new Date(iso);
  if (Number.isNaN(d.getTime())) return String(iso);
  return d.toLocaleString('pt-BR');
};

const analisarJsonSeguro = (valor) => {
  if (!valor) return null;
  if (typeof valor === 'object') return valor;
  try { return JSON.parse(valor); } catch { return null; }
};

const pareceObjetoSimples = (v) => v && typeof v === 'object' && !Array.isArray(v);

const normalizarValorMudanca = (v) => {
  if (pareceObjetoSimples(v)) {
    if (v.nome) return v.nome; if (v.descricao) return v.descricao;
    if (v.titulo) return v.titulo; if (v.label) return v.label;
    if (v.value) return v.value; if (v.id !== undefined && v.id !== null) return `#${v.id}`;
  }
  return v;
};

const compararObjetosRaso = (antes, depois, caminhoBase = '') => {
  if (!pareceObjetoSimples(antes) || !pareceObjetoSimples(depois)) return [];
  const chaves = new Set([...Object.keys(antes), ...Object.keys(depois)]);
  const resultado = []; let contagem = 0;
  for (const k of chaves) {
    if (contagem > 60) break;
    const caminho = caminhoBase ? `${caminhoBase}.${k}` : k;
    const ant = normalizarValorMudanca(antes[k]); const dep = normalizarValorMudanca(depois[k]);
    if (JSON.stringify(ant) !== JSON.stringify(dep)) { resultado.push({ field: caminho, from: ant, to: dep }); contagem++; }
  }
  return resultado;
};

const extrairMudancas = (linha) => {
  const acao = String(linha?.action || '').toUpperCase();
  const detalhes = analisarJsonSeguro(linha?.detailsJson);
  const payload = analisarJsonSeguro(linha?.payloadJson);
  const estaEmBranco = (v) => v === undefined || v === null || (typeof v === 'string' && v.trim().length === 0);

  if (Array.isArray(detalhes?.changes) && detalhes.changes.length) {
    return detalhes.changes.filter((c) => c && (c.field || c.campo)).map((c) => ({
        field: c.field ?? c.campo, from: normalizarValorMudanca(c.from ?? c.de ?? c.old ?? c.before), to: normalizarValorMudanca(c.to ?? c.para ?? c.new ?? c.after),
      })).filter((c) => !(estaEmBranco(c.from) && estaEmBranco(c.to)));
  }

  if (Array.isArray(detalhes?.changedFields) && detalhes.changedFields.length) {
    return detalhes.changedFields.map((f) => ({ field: f, from: undefined, to: undefined })).filter((c) => !(estaEmBranco(c.from) && estaEmBranco(c.to)));
  }

  if (pareceObjetoSimples(detalhes?.changedFields)) {
    const resultado = [];
    for (const [campo, val] of Object.entries(detalhes.changedFields)) {
      if (pareceObjetoSimples(val) && ('from' in val || 'to' in val || 'before' in val || 'after' in val)) {
        resultado.push({ field: campo, from: normalizarValorMudanca(val.from ?? val.before ?? val.old), to: normalizarValorMudanca(val.to ?? val.after ?? val.new) });
      } else if (Array.isArray(val) && val.length >= 2) {
        resultado.push({ field: campo, from: normalizarValorMudanca(val[0]), to: normalizarValorMudanca(val[1]) });
      } else { resultado.push({ field: campo, from: undefined, to: normalizarValorMudanca(val) }); }
    }
    if (resultado.length) return resultado.filter((c) => !(estaEmBranco(c.from) && estaEmBranco(c.to)));
  }

  const antes = detalhes?.before ?? detalhes?.old ?? payload?.before ?? payload?.old ?? payload?.previous;
  const depois = detalhes?.after ?? detalhes?.new ?? payload?.after ?? payload?.new ?? payload?.current;
  const diferenca = compararObjetosRaso(antes, depois);
  if (diferenca.length) return diferenca.filter((c) => !(estaEmBranco(normalizarValorMudanca(c.from)) && estaEmBranco(normalizarValorMudanca(c.to))));

  if (acao.includes('CREATE')) return [{ field: '__create__', from: undefined, to: undefined }];
  if (acao.includes('DELETE')) return [{ field: '__delete__', from: undefined, to: undefined }];

  return [];
};

const foiCancelado = (err) => err?.canceled === true || err?.original?.name === 'CanceledError' || err?.name === 'CanceledError' || err?.code === 'ERR_CANCELED';
const eProibido = (err) => err?.status === 403;

export const ModalHistoricoAuditoria = ({ isOpen, onClose, empregado }) => {
  const [carregando, setCarregando] = useState(false);
  const [carregandoRefs, setCarregandoRefs] = useState(false);
  const [referencias, setReferencias] = useState({ cargos: new Map(), lotacoes: new Map(), jornadas: new Map() });
  const [dados, setDados] = useState({ content: [], totalPages: 0, totalElements: 0, number: 0 });
  const [pagina, setPagina] = useState(0);

  const [tamanho] = useState(CONFIG.AUDIT_DEFAULT_SIZE);

  const [filtroAcao, setFiltroAcao] = useState('');
  const [selecionado, setSelecionado] = useState(null);
  const [proibido, setProibido] = useState(false);
  const [mostrarJsonBruto, setMostrarJsonBruto] = useState(false);
  const [mostrarMudancasCampos, setMostrarMudancasCampos] = useState(false);
  const [idsOcorrencia, setIdsOcorrencia] = useState([]);

  useEffect(() => {
    if (!isOpen || !empregado?.id) return;
    const buscarOcorrencias = async () => {
        try {
            const resposta = await ServicoOcorrencia.listarPorEmpregado(empregado.id);
            const lista = Array.isArray(resposta) ? resposta : (resposta?.content || []);
            setIdsOcorrencia(lista.map(o => o.id).filter(id => id != null));
        } catch (e) { setIdsOcorrencia([]); }
    };
    buscarOcorrencias();
  }, [isOpen, empregado?.id]);

  useEffect(() => {
    const controlador = new AbortController();
    const carregarReferencias = async () => {
      if (!isOpen) return;
      setCarregandoRefs(true);
      try {
        const [aux, respostaJornadas] = await Promise.all([
          ServicoEmpregado.buscarDadosAuxiliares({ signal: controlador.signal, retries: 0 }),
          ServicoJornada.listar({ signal: controlador.signal, retries: 0 }).catch(() => []),
        ]);
        const cargos = new Map((aux?.cargos || []).map((c) => [Number(c.id), c.nome || c.descricao || String(c.id)]));
        const lotacoes = new Map((aux?.lotacoes || []).map((l) => [Number(l.id), l.nome || l.descricao || String(l.id)]));
        const listaJornadas = Array.isArray(respostaJornadas) ? respostaJornadas : (respostaJornadas?.content || respostaJornadas || []);
        const jornadas = new Map((listaJornadas || []).map((j) => [Number(j.id), j.descricao || j.nome || String(j.id)]));
        setReferencias({ cargos, lotacoes, jornadas });
      } catch (e) { if (!foiCancelado(e)) console.warn(e); } finally { setCarregandoRefs(false); }
    };
    carregarReferencias();
    return () => controlador.abort();
  }, [isOpen]);

  const parametros = useMemo(() => {
    const p = { page: pagina, size: tamanho, sort: 'createdAt,desc', entityType: 'Empregado', entityId: empregado?.id };
    if (idsOcorrencia.length > 0) { p.includeEntityType = 'Ocorrencia'; p.includeEntityIds = idsOcorrencia; }
    if (filtroAcao && filtroAcao.trim()) p.action = filtroAcao.trim();
    return p;
  }, [pagina, tamanho, empregado?.id, filtroAcao, idsOcorrencia]);

  useEffect(() => {
    const controlador = new AbortController();
    const carregar = async () => {
      if (!isOpen || !empregado?.id) return;
      setProibido(false); setCarregando(true);
      try {
        const resposta = await ServicoAuditoria.buscar(parametros, { signal: controlador.signal, retries: 0 });
        setDados(resposta || { content: [], totalPages: 0, totalElements: 0, number: 0 });
      } catch (e) {
        if (foiCancelado(e)) return;
        if (eProibido(e)) { setProibido(true); setDados({ content: [], totalPages: 0, totalElements: 0, number: 0 }); return; }
      } finally { setCarregando(false); }
    };
    carregar();
    return () => controlador.abort();
  }, [isOpen, empregado?.id, parametros]);

  useEffect(() => { if (isOpen) setPagina(0); }, [empregado?.id, isOpen]);

  const fecharTudo = () => { setSelecionado(null); setMostrarJsonBruto(false); setMostrarMudancasCampos(false); onClose?.(); };

  const renderizarResumoDetalhes = (linha) => {
    const mudancas = extrairMudancas(linha);
    const itens = (mudancas || []).slice().sort((a, b) => classificarMudanca(a.field) - classificarMudanca(b.field)).map((c) => humanizarMudanca(c, referencias, linha)).filter(Boolean);
    if (!itens.length) return '—';
    const topo = itens.slice(0, 2).map((i) => i.text).join(' • ');
    return `${topo}${itens.length > 2 ? ` (+${itens.length - 2})` : ''}`;
  };

  return (
    <>
      <JanelaModal isOpen={isOpen} onClose={fecharTudo} title={`Histórico de alterações — ${empregado?.nome || 'Empregado'}`} maxWidth="max-w-5xl">
        <div className={estilos.conteinerGeral}>
          <div className={estilos.cabecalhoFiltros}>
            <div className={estilos.grupoFiltro}>
              <CampoEntrada value={filtroAcao} onChange={(e) => setFiltroAcao(e.target.value)} placeholder="Filtrar por ação (ex: CREATE)" />
              <Botao variant="secondary" onClick={() => setPagina(0)} disabled={carregando} title="Aplicar filtro"><RefreshCcw size={16} /></Botao>
            </div>
            <div className={estilos.infoPaginacao}>
              <span>Total: <b>{dados?.totalElements ?? 0}</b></span><span className={estilos.separadorPaginacao}>|</span>
              <span>Página: <b>{(dados?.number ?? 0) + 1}</b></span>
              {carregandoRefs ? <><span className={estilos.separadorPaginacao}>|</span><span className={estilos.textoCarregandoRefs}>Carregando referências...</span></> : null}
            </div>
          </div>

          {proibido ? (
            <div className={estilos.mensagemProibido}>
              Acesso restrito. Apenas ADMIN pode visualizar logs de auditoria.
            </div>
          ) : (
            <div className={estilos.envoltorioTabela}>
              <div className={estilos.containerRolagemTabela}>
                <table className={estilos.tabelaAuditoria}>
                  <thead className={estilos.cabecalhoTabela}>
                    <tr className={estilos.linhaCabecalho}>
                      <th className={estilos.celulaCabecalho}>Data/Hora</th>
                      <th className={estilos.celulaCabecalho}>Evento</th>
                      <th className={estilos.celulaCabecalho}>Usuário</th>
                      <th className={estilos.celulaCabecalho}>O que mudou</th>
                      <th className={estilos.celulaCabecalhoDireita}>Detalhes</th>
                    </tr>
                  </thead>
                  <tbody className={estilos.corpoTabela}>
                    {carregando ? (
                      <tr><td colSpan={5} className={estilos.mensagemCentralizada}>Carregando...</td></tr>
                    ) : (dados?.content?.length ? (
                      dados.content.map((linha) => (
                        <tr key={linha.id} className={estilos.linhaTabela}>
                          <td className={estilos.celulaData}>{formatarDataHora(linha.createdAt)}</td>
                          <td className={estilos.celulaAcao}>{rotuloAcao(linha.action, linha.entityType)}</td>
                          <td className={estilos.celulaUsuario}>{linha.username || '—'}</td>
                          <td className={estilos.celulaMudanca}>{renderizarResumoDetalhes(linha)}</td>
                          <td className={estilos.celulaAcoesTabela}>
                            <button className={estilos.botaoIconeTabela} onClick={() => setSelecionado(linha)} title="Ver detalhes"><Eye size={16} /></button>
                          </td>
                        </tr>
                      ))
                    ) : (
                      <tr><td colSpan={5} className={estilos.mensagemCentralizada}>Nenhum registro encontrado.</td></tr>
                    ))}
                  </tbody>
                </table>
              </div>
              <div className={estilos.rodapeTabela}>
                <Botao variant="secondary" onClick={() => setPagina((p) => Math.max(0, p - 1))} disabled={carregando || (dados?.number ?? 0) <= 0}><ChevronLeft size={16} /> Anterior</Botao>
                <Botao variant="secondary" onClick={() => setPagina((p) => p + 1)} disabled={carregando || (dados?.number ?? 0) >= (dados?.totalPages ?? 1) - 1}>Próxima <ChevronRight size={16} /></Botao>
              </div>
            </div>
          )}
          <div className={estilos.rodapeModalGeral}><Botao variant="secondary" onClick={fecharTudo}>Fechar</Botao></div>
        </div>
      </JanelaModal>

      <JanelaModal isOpen={!!selecionado} onClose={() => { setSelecionado(null); setMostrarJsonBruto(false); setMostrarMudancasCampos(false); }} title="Detalhes do log" maxWidth="max-w-4xl">
        <div className={estilos.containerDetalhes}>
          <div className={estilos.gradeInformacoes}>
            <div className={estilos.cartaoInformacao}><div className={estilos.rotuloInformacao}>Data/Hora</div><div className={estilos.valorInformacao}>{formatarDataHora(selecionado?.createdAt)}</div></div>
            <div className={estilos.cartaoInformacao}><div className={estilos.rotuloInformacao}>Evento</div><div className={estilos.valorInformacao}>{rotuloAcao(selecionado?.action, selecionado?.entityType)}</div></div>
            <div className={estilos.cartaoInformacao}><div className={estilos.rotuloInformacao}>Usuário</div><div className={estilos.valorInformacao}>{selecionado?.username || '—'}</div></div>
            <div className={estilos.cartaoInformacao}><div className={estilos.rotuloInformacao}>Entidade</div><div className={estilos.valorInformacao}>{selecionado?.entityType} #{selecionado?.entityId}</div></div>
          </div>
          <div className={estilos.secaoOQueMudou}>
            <div className={estilos.tituloOQueMudou}>O que mudou</div>
            {(() => {
              const mudancasObj = extrairMudancas(selecionado);
              const itens = (mudancasObj || []).slice().sort((a, b) => classificarMudanca(a.field) - classificarMudanca(b.field)).map((c) => humanizarMudanca(c, referencias, selecionado)).filter(Boolean);
              if (!itens.length) return <div className={estilos.mensagemSemMudancas}>Sem detalhes disponíveis.</div>;
              return (
                <div className={estilos.containerDetalhes}>
                  <ul className={estilos.listaMudancasResumo}>{itens.map((i, idx) => (<li key={`${i.type}-${idx}`}>{i.text}</li>))}</ul>
                  <button className={estilos.botaoTexto} onClick={() => setMostrarMudancasCampos((v) => !v)} type="button">{mostrarMudancasCampos ? 'Ocultar detalhes' : 'Exibir detalhes'}</button>
                  {mostrarMudancasCampos ? (
                    <div className={estilos.containerComparacaoCampos}>
                      {mudancasObj.map((c, idx) => (
                        <div key={`${c.field}-${idx}`} className={estilos.linhaComparacao}>
                          <div className={estilos.nomeCampoComparacao}>{rotuloParaCampo(c.field)}</div>
                          {(c.from === undefined && c.to === undefined) ? (<div className={estilos.valoresComparacao}>Campo alterado</div>) : (
                            <div className={estilos.valoresComparacao}><span className={estilos.textoRotuloDePara}>De:</span> <b className={estilos.valorNegrito}>{formatarValor(c.from)}</b> <span className={estilos.divisorComparacao}>|</span> <span className={estilos.textoRotuloDePara}>Para:</span> <b className={estilos.valorNegrito}>{formatarValor(c.to)}</b></div>
                          )}
                        </div>
                      ))}
                    </div>
                  ) : null}
                </div>
              );
            })()}
            <div className={estilos.secaoCabecalhoJson}>
              <button className={estilos.botaoTexto} onClick={() => setMostrarJsonBruto((v) => !v)} type="button"><FileText size={14} /> {mostrarJsonBruto ? 'Ocultar JSON' : 'Exibir JSON'}</button>
            </div>
            {mostrarJsonBruto ? (
              <div className={estilos.containerPreJson}>
                <div className={estilos.cartaoJson}><div className={estilos.tituloJson}><FileText size={16} /> detailsJson</div><pre className={estilos.codigoPreFormatado}>{(() => { const parseado = analisarJsonSeguro(selecionado?.detailsJson); return parseado ? JSON.stringify(parseado, null, 2) : (selecionado?.detailsJson || '—'); })()}</pre></div>
                <div className={estilos.cartaoJson}><div className={estilos.tituloJson}><FileText size={16} /> payloadJson</div><pre className={estilos.codigoPreFormatado}>{(() => { const parseado = analisarJsonSeguro(selecionado?.payloadJson); return parseado ? JSON.stringify(parseado, null, 2) : (selecionado?.payloadJson || '—'); })()}</pre></div>
              </div>
            ) : null}
          </div>
          <div className={estilos.rodapeModalGeral}><Botao variant="secondary" onClick={() => { setSelecionado(null); setMostrarJsonBruto(false); setMostrarMudancasCampos(false); }}>Fechar</Botao></div>
        </div>
      </JanelaModal>
    </>
  );
};