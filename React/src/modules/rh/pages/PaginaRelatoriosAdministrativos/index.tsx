import { observarFalha } from '../../../../core/http/falhaObservada';
import type * as DTO from '../../../../contracts/java';
import type { AdministrativeReport } from '../../../../contracts/relatorios';
import type { RequestOptions } from '../../../../core/http/types';
import React, { useCallback, useEffect, useMemo, useState } from "react";
import { FileCheck2, FileClock, History, RefreshCw, Search, ShieldAlert } from "lucide-react";
import { notificar } from "../../../../core/events";
import { useAutenticacao } from "../../../auth/context.tsx";
import { ePresidente } from "../../../../core/security/roles.ts";
import { useConsultaVigente } from "../../../../core/hooks/useConsultaVigente.ts";
import { obterDataNoFusoNegocio, obterPeriodoMensalNoFusoNegocio } from "../../../../core/formatters/dataHoraNegocio.ts";
import { ServicoFrequencia } from "../../../../services/rh/ServicoFrequencia.ts";
import {
  ServicoRelatorioAdministrativo,
  criarChaveIdempotenciaRelatorio,
} from "../../../../services/rh/ServicoRelatorioAdministrativo.ts";
import estilos from "./index.module.css";

const hojeIso = () => obterDataNoFusoNegocio() || "";
const inicioMesIso = () => obterPeriodoMensalNoFusoNegocio()?.start || "";

const formatarDataHora = (valor: (import("../../../../contracts/shared").Instant | null | undefined)) => {
  if (!valor) return "—";
  const data = new Date(valor);
  return Number.isNaN(data.getTime()) ? String(valor) : data.toLocaleString("pt-BR");
};

const rotuloTipo = (tipo: (import("../../../../contracts/java").TipoRelatorioAdministrativo | null | undefined)) => tipo === "ABSENTEISMO" ? "Absenteísmo" : "Frequência consolidada";
const rotuloFormato = (formato: string | null | undefined) => (({ VISUALIZACAO: "Visualização", XLSX: "XLSX", PDF: "PDF" } as Readonly<Record<string, string | undefined>>)[String(formato)] || formato || "—");
const rotuloEstado = (estado: (import("../../../../contracts/java").EstadoEmissaoRelatorio | null | undefined)) => (({ VIGENTE: "Vigente", SUPERADA: "Superada" } as Readonly<Record<string, string | undefined>>)[String(estado)] || estado || "—");
const extrairConteudoLista = (pagina: (import("../../../../contracts/shared").Page<import("../../../../contracts/java").MetadadosEmissaoRelatorioDTO>)) => Array.isArray(pagina?.content) ? pagina.content : [];

const AvisoRevisao = ({ metadados }: { metadados?: Partial<DTO.MetadadosEmissaoRelatorioDTO> | null }) => {
  if (!metadados) return null;
  const sujeito = Boolean(metadados.sujeitoARevisao);
  const quantidade = Number(metadados.quantidadePendencias || 0);
  return (
    <div className={sujeito ? estilos.avisoRevisao : estilos.avisoDefinitivo} role={sujeito ? "alert" : "status"}>
      <ShieldAlert size={18} aria-hidden="true" />
      <div>
        <strong>{sujeito ? "SUJEITO A REVISÃO" : "Sem pendências de retificação"}</strong>
        <span>{metadados.aviso || (sujeito ? `${quantidade} pendência(s) no recorte.` : "O Java não informou pendências no recorte consultado.")}</span>
      </div>
    </div>
  );
};

const ResumoConteudo = ({ relatorio }: { relatorio: AdministrativeReport | null }) => {
  const [paginaConteudo, setPaginaConteudo] = useState(0);
  useEffect(() => { setPaginaConteudo(0); }, [relatorio]);
  const conteudo = relatorio?.conteudo;
  if (!conteudo) return <p className={estilos.textoSecundario}>Nenhum conteúdo carregado.</p>;
  const registros = Array.isArray(conteudo) ? conteudo : (Array.isArray(conteudo?.registros) ? conteudo.registros : []);
  const totalPaginasConteudo = Math.max(1, Math.ceil(registros.length / 5));
  const totais = Array.isArray(conteudo) ? undefined : conteudo?.totais;
  return (
    <div className={estilos.conteudoResultado}>
      {totais && (
        <div className={estilos.metricas}>
          {Object.entries(totais).slice(0, 10).map(([chave, valor]: [string, unknown]) => (
            <div key={chave} className={estilos.metrica}><span>{chave}</span><strong>{String(valor ?? 0)}</strong></div>
          ))}
        </div>
      )}
      <p className={estilos.textoSecundario}>
        {registros.length.toLocaleString("pt-BR")} registro(s) no snapshot exibido. A interface não recalcula totais administrativos.
      </p>
      {registros.length > 0 && (
        <div className={estilos.tabelaRolagem}>
          <table className={estilos.tabelaConteudo}>
            <thead><tr><th>Colaborador</th><th>Cargo</th><th>Lotação</th><th>Turno</th><th>Taxa/Resumo</th></tr></thead>
            <tbody>
              {registros.slice(paginaConteudo * 5, (paginaConteudo + 1) * 5).map((item, indice) => (
                <tr key={item?.empregadoId ?? ('id' in item ? String(item.id) : indice)}>
                  <td>{item?.nome || "—"}</td>
                  <td>{item?.cargo || "—"}</td>
                  <td>{item?.lotacao || ('lotacaoNome' in item ? String(item.lotacaoNome) : null) || '—'}</td>
                  <td>{item?.turno || ('jornadaNome' in item ? String(item.jornadaNome) : null) || '—'}</td>
                  <td>{'taxaIndividual' in item && item.taxaIndividual != null ? `${item.taxaIndividual}%` : `${'totais' in item ? item.totais?.presencas ?? 0 : 0} presença(s)`}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}
      {totalPaginasConteudo > 1 && (
        <div className={estilos.paginacao}>
          <button type="button" aria-label="Página anterior do conteúdo" disabled={paginaConteudo === 0} onClick={() => setPaginaConteudo((atual) => atual - 1)}>Anterior</button>
          <span>Página {paginaConteudo + 1} de {totalPaginasConteudo}</span>
          <button type="button" aria-label="Próxima página do conteúdo" disabled={paginaConteudo + 1 >= totalPaginasConteudo} onClick={() => setPaginaConteudo((atual) => atual + 1)}>Próxima</button>
        </div>
      )}
    </div>
  );
};

export const PaginaRelatoriosAdministrativos = ({ podeEmitir = false }: { podeEmitir?: boolean }) => {
  const { user } = useAutenticacao();
  const presidente = ePresidente(user);
  const [filtros, setFiltros] = useState<{ [K in keyof DTO.RelatorioFiltrosDTO]: NonNullable<DTO.RelatorioFiltrosDTO[K]> }>({ contratos: [], lotacoes: [], turnos: [] });
  const [tipo, setTipo] = useState<DTO.TipoRelatorioAdministrativo>("FREQUENCIA_CONSOLIDADA");
  const [formato, setFormato] = useState<DTO.FormatoEmissaoRelatorio>("VISUALIZACAO");
  const [inicio, setInicio] = useState(inicioMesIso);
  const [fim, setFim] = useState(hojeIso);
  const [contratoIds, setContratoIds] = useState<(number[])>([]);
  const [lotacaoId, setLotacaoId] = useState("");
  const [jornadaId, setJornadaId] = useState("");
  const [conhecidoAte, setConhecidoAte] = useState("");
  const [resultado, setResultado] = useState<AdministrativeReport | null>(null);
  const [comparacao, setComparacao] = useState<(import("../../../../contracts/java").ComparacaoEmissaoRelatorioDTO) | null>(null);
  const [emissoes, setEmissoes] = useState<(import("../../../../contracts/java").MetadadosEmissaoRelatorioDTO[])>([]);
  const [pagina, setPagina] = useState({ numero: 0, totalPaginas: 0, totalElementos: 0, primeira: true, ultima: true });
  const [tamanho, setTamanho] = useState(5);
  const [estado, setEstado] = useState("");
  const [carregando, setCarregando] = useState(false);
  const [carregandoLista, setCarregandoLista] = useState(false);
  const [erroLista, setErroLista] = useState("");
  const [chaveIdempotencia, setChaveIdempotencia] = useState(criarChaveIdempotenciaRelatorio);
  const identidadeUsuario = JSON.stringify([user?.id, user?.username, user?.perfil, user?.autoridade, user?.vinculosGestao]);
  const contextoLista = JSON.stringify([tipo, estado, inicio, fim, tamanho, identidadeUsuario]);
  const contextoRelatorio = JSON.stringify([tipo, formato, inicio, fim, contratoIds, lotacaoId, jornadaId, conhecidoAte, identidadeUsuario]);
  const consultasLista = useConsultaVigente(contextoLista);
  const consultasFiltros = useConsultaVigente(identidadeUsuario);
  const consultasResultado = useConsultaVigente(contextoRelatorio);

  const renovarChave = useCallback(() => { setChaveIdempotencia(criarChaveIdempotenciaRelatorio()); }, []);
  useEffect(() => { renovarChave(); }, [tipo, formato, inicio, fim, contratoIds, lotacaoId, jornadaId, renovarChave]);

  const carga = useMemo(() => ({
    tipo,
    formato,
    inicio,
    fim,
    contratoIds,
    lotacaoId: lotacaoId || null,
    jornadaId: jornadaId || null,
    chaveIdempotencia,
  }), [tipo, formato, inicio, fim, contratoIds, lotacaoId, jornadaId, chaveIdempotencia]);

  const validarPeriodo = () => {
    const a = new Date(`${inicio}T12:00:00Z`);
    const b = new Date(`${fim}T12:00:00Z`);
    if (!inicio || !fim || a > b) return "Informe um período válido.";
    if (Math.floor((b.getTime() - a.getTime()) / 86400000) > 366) return "A diferença entre início e fim não pode exceder 366 dias, conforme o Java.";
    return null;
  };

  const carregarLista = useCallback(async (numero = 0, novoTamanho = tamanho) => {
    const consulta = consultasLista.iniciar();
    setCarregandoLista(true);
    setErroLista("");
    setEmissoes([]);
    try {
      const resposta = await ServicoRelatorioAdministrativo.listar({
        ...(tipo ? { tipo } : {}),
        ...(estado ? { estado } : {}),
        inicio,
        fim,
        pagina: numero,
        tamanho: novoTamanho,
      });
      if (!consulta.atual()) return;
      setEmissoes(extrairConteudoLista(resposta));
      setPagina({
        numero: Number(resposta?.number ?? numero),
        totalPaginas: Number(resposta?.totalPages || 0),
        totalElementos: Number(resposta?.totalElements || 0),
        primeira: Boolean(resposta?.first),
        ultima: Boolean(resposta?.last),
      });
    } catch (erroOriginal) {
      const erro = observarFalha(erroOriginal);

      if (!consulta.atual()) return;
      const mensagem = erro?.message || "Não foi possível listar as emissões administrativas.";
      setErroLista(mensagem);
      notificar("error", mensagem);
    } finally {
      if (consulta.atual()) setCarregandoLista(false);
    }
  }, [estado, fim, inicio, tamanho, tipo, identidadeUsuario, consultasLista]);

  useEffect(() => {
    const consulta = consultasFiltros.iniciar();
    setFiltros({ contratos: [], lotacoes: [], turnos: [] });
    ServicoFrequencia.carregarFiltrosRelatorio({ signal: consulta.signal, silentError: true }).then((resposta) => {
      if (!consulta.atual()) return;
      setFiltros({
        contratos: Array.isArray(resposta?.contratos) ? resposta.contratos : [],
        lotacoes: Array.isArray(resposta?.lotacoes) ? resposta.lotacoes : [],
        turnos: Array.isArray(resposta?.turnos) ? resposta.turnos : [],
      });
    }).catch((_erro: unknown) => {
      if (!consulta.atual()) return;
      notificar("error", "Não foi possível carregar os filtros dos relatórios.");
    });
  }, [consultasFiltros, identidadeUsuario]);

  useEffect(() => {
    setPagina({ numero: 0, totalPaginas: 0, totalElementos: 0, primeira: true, ultima: true });
    carregarLista(0);
  }, [carregarLista]);

  useEffect(() => {
    setResultado(null);
    setComparacao(null);
    setCarregando(false);
  }, [contextoRelatorio]);

  const executar = async (acao: (options: RequestOptions) => Promise<AdministrativeReport>) => {
    const erroPeriodo = validarPeriodo();
    if (erroPeriodo) return notificar("warning", erroPeriodo);
    const consulta = consultasResultado.iniciar();
    setCarregando(true);
    setResultado(null);
    setComparacao(null);
    try {
      // O descarte local não impede um interceptor de publicar um erro antes
      // do catch. A consulta também delimita o transporte e assume a mensagem.
      const resposta = await acao({ signal: consulta.signal, silentError: true });
      if (!consulta.atual()) return null;
      setResultado(resposta);
      return resposta;
    } catch (erroOriginal) {
      const erro = observarFalha(erroOriginal);

      if (!consulta.atual()) return null;
      notificar("error", erro?.message || "O Java recusou a operação administrativa.");
      return null;
    } finally {
      if (consulta.atual()) setCarregando(false);
    }
  };

  const emitir = async () => {
    if (!podeEmitir || presidente) {
      notificar("warning", "O Presidente possui leitura administrativa, mas o Java bloqueia emissão formal.");
      return;
    }
    const resposta = await executar((opcoes) => ServicoRelatorioAdministrativo.emitir(carga, opcoes));
    if (resposta) {
      notificar("success", `Emissão formal v${resposta?.metadados?.versao || 1} criada.`);
      renovarChave();
      carregarLista(0);
    }
  };

  const consultarConhecimento = async () => {
    if (!conhecidoAte) return notificar("warning", "Informe o instante de conhecimento.");
    const instante = new Date(conhecidoAte);
    if (Number.isNaN(instante.getTime())) return notificar("warning", "Instante de conhecimento inválido.");
    await executar((opcoes) => ServicoRelatorioAdministrativo.conhecidaAte(instante.toISOString(), carga, opcoes));
  };

  const abrir = async (id: (number | null)) => {
    if (presidente) {
      notificar("warning", "O serviço Java atual bloqueia a abertura por ID para Presidente; a lista e a prévia nacional permanecem disponíveis.");
      return;
    }
    await executar((opcoes) => ServicoRelatorioAdministrativo.obter(id, opcoes));
  };

  const comparar = async (id: (number | null)) => {
    if (presidente) {
      notificar("warning", "O serviço Java atual bloqueia a comparação por ID para Presidente.");
      return;
    }
    const consulta = consultasResultado.iniciar();
    setCarregando(true);
    setComparacao(null);
    setResultado(null);
    try {
      const resposta = await ServicoRelatorioAdministrativo.compararComVigente(id, { signal: consulta.signal, silentError: true });
      if (!consulta.atual()) return;
      setComparacao(resposta);
      setResultado(null);
    } catch (erroOriginal) {
      const erro = observarFalha(erroOriginal);

      if (!consulta.atual()) return;
      notificar("error", erro?.message || "Não foi possível comparar a emissão com o recorte vigente.");
    } finally {
      if (consulta.atual()) setCarregando(false);
    }
  };

  const alterarContratos = (evento: React.ChangeEvent<HTMLSelectElement>) => {
    setContratoIds(Array.from(evento.target.selectedOptions).map((opcao) => Number(opcao.value)));
  };

  return (
    <div className={estilos.pagina}>
      <header className={estilos.cabecalho}>
        <div><span className={estilos.selo}>Governança e rastreabilidade</span><h1>Emissões administrativas</h1><p>Consulte prévias, registre versões formais e recupere o que era conhecido em cada data.</p></div>
        <FileCheck2 size={34} aria-hidden="true" />
      </header>

      {presidente && <div className={estilos.avisoPresidente}><ShieldAlert size={18} /><span><strong>Presidente:</strong> leitura administrativa nacional. A emissão formal é bloqueada pelo backend.</span></div>}

      <section className={estilos.cartao} aria-label="Filtros administrativos">
        <div className={estilos.gradeFiltros}>
          <label>Relatório<select value={tipo} onChange={(e) => setTipo(e.target.value as DTO.TipoRelatorioAdministrativo)}><option value="FREQUENCIA_CONSOLIDADA">Frequência consolidada</option><option value="ABSENTEISMO">Absenteísmo</option></select></label>
          <label>Formato<select value={formato} onChange={(e) => setFormato(e.target.value as DTO.FormatoEmissaoRelatorio)}><option value="VISUALIZACAO">Visualização</option><option value="XLSX">XLSX (metadado administrativo)</option><option value="PDF">PDF (metadado administrativo)</option></select></label>
          <label>Início<input type="date" value={inicio} onChange={(e) => setInicio(e.target.value)} /></label>
          <label>Fim<input type="date" value={fim} onChange={(e) => setFim(e.target.value)} /></label>
          <label>Contratos<select multiple value={contratoIds.map(String)} onChange={alterarContratos}>{filtros.contratos.map((item) => <option key={item.id} value={(item.id) ?? undefined}>{item.numeroContrato || item.descricao || ('nome' in item ? String(item.nome) : null) || item.id}</option>)}</select><small>Sem seleção = todos permitidos no escopo.</small></label>
          <label>Lotação<select value={lotacaoId} onChange={(e) => setLotacaoId(e.target.value)}><option value="">Todas</option>{filtros.lotacoes.map((item) => <option key={item.id} value={(item.id) ?? undefined}>{item.nome || item.id}</option>)}</select></label>
          <label>Turno<select value={jornadaId} onChange={(e) => setJornadaId(e.target.value)}><option value="">Todos</option>{filtros.turnos.map((item) => <option key={item.id} value={(item.id) ?? undefined}>{item.descricao || item.id}</option>)}</select></label>
        </div>
        <div className={estilos.acoes}>
          <button type="button" disabled={carregando} onClick={() => executar((opcoes) => ServicoRelatorioAdministrativo.previa(carga, opcoes))}><Search size={16} /> Gerar prévia</button>
          <button type="button" className={estilos.primario} disabled={carregando || !podeEmitir || presidente} onClick={emitir}><FileCheck2 size={16} /> Emitir versão formal</button>
        </div>
        <p className={estilos.observacao}>A emissão formal registra uma versão dos dados. Para baixar arquivos XLSX ou PDF, use os relatórios operacionais.</p>
      </section>

      <section className={estilos.cartao} aria-label="Conhecimento histórico">
        <div className={estilos.tituloSecao}><FileClock size={20} /><div><h2>Conhecida até</h2><p>Recupera exatamente a versão formal conhecida até um instante.</p></div></div>
        <div className={estilos.linhaConhecimento}><label className={estilos.campoConhecimento}>Instante de conhecimento<input type="datetime-local" value={conhecidoAte} onChange={(e) => setConhecidoAte(e.target.value)} /></label><button type="button" disabled={carregando} onClick={consultarConhecimento}>Consultar</button></div>
      </section>

      {resultado && (
        <section className={estilos.cartao} aria-label="Resultado administrativo">
          <div className={estilos.tituloSecao}><FileCheck2 size={20} /><div><h2>{resultado?.metadados?.id ? `Emissão #${resultado.metadados.id}` : "Prévia atual"}</h2><p>{rotuloTipo(resultado?.metadados?.tipo)} · {rotuloFormato(resultado?.metadados?.formato)} · v{resultado?.metadados?.versao ?? 0} · {rotuloEstado(resultado?.metadados?.estado)}</p></div></div>
          <AvisoRevisao metadados={resultado?.metadados} />
          <div className={estilos.metadados}><span>Gerado em <strong>{formatarDataHora(resultado?.metadados?.geradoEm)}</strong></span><span>Natureza <strong>{resultado?.metadados?.natureza || "—"}</strong></span><span>Hash <code>{resultado?.metadados?.hashConteudo || "—"}</code></span></div>
          <ResumoConteudo relatorio={resultado} />
        </section>
      )}

      {comparacao && (
        <section className={estilos.cartao} aria-label="Comparação com versão vigente">
          <div className={estilos.tituloSecao}><RefreshCw size={20} /><div><h2>Comparação com o vigente</h2><p>{comparacao.alterado ? "O conteúdo vigente diverge do snapshot." : "O snapshot permanece igual ao conteúdo vigente."}</p></div></div>
          <AvisoRevisao metadados={{ sujeitoARevisao: comparacao?.revisaoVigente?.sujeitoARevisao, quantidadePendencias: comparacao?.revisaoVigente?.quantidadePendencias, aviso: comparacao?.revisaoVigente?.mensagem }} />
          <div className={estilos.metadados}><span>Hash histórico <code>{comparacao.hashHistorico}</code></span><span>Hash vigente <code>{comparacao.hashVigente}</code></span></div>
        </section>
      )}

      <section className={estilos.cartao} aria-label="Histórico de emissões">
        <div className={estilos.tituloSecao}><History size={20} /><div><h2>Histórico de emissões</h2><p>{pagina.totalElementos.toLocaleString("pt-BR")} versão(ões) no recorte.</p></div></div>
        <div className={estilos.filtrosHistorico}><label>Estado<select value={estado} onChange={(e) => setEstado(e.target.value)}><option value="">Todos</option><option value="VIGENTE">Vigente</option><option value="SUPERADA">Superada</option></select></label><label>Itens por página<select value={tamanho} onChange={(e) => setTamanho(Number(e.target.value))}><option value="5">5</option><option value="25">25</option><option value="50">50</option><option value="100">100</option><option value="200">200</option></select></label><button type="button" onClick={() => carregarLista(0)} disabled={carregando}>Atualizar</button></div>
        <div className={estilos.tabelaRolagem}><table className={estilos.tabelaHistorico}><thead><tr><th>ID</th><th>Tipo</th><th>Versão</th><th>Natureza</th><th>Estado</th><th>Revisão</th><th>Gerado em</th><th>Ações</th></tr></thead><tbody>{emissoes.map((item) => <tr key={item.id}><td>#{item.id}</td><td>{rotuloTipo(item.tipo)}</td><td>v{item.versao}</td><td>{item.natureza || "—"}</td><td>{rotuloEstado(item.estado)}</td><td>{item.sujeitoARevisao ? `${item.quantidadePendencias} pendência(s)` : "Definitivo"}</td><td>{formatarDataHora(item.geradoEm)}</td><td><div className={estilos.acoesTabela}><button type="button" disabled={carregando || presidente} onClick={() => abrir(item.id)}>Abrir</button><button type="button" disabled={carregando || presidente} onClick={() => comparar(item.id)}>Comparar</button></div></td></tr>)}</tbody></table></div>
        {carregandoLista && <p role="status">Carregando emissões...</p>}
        {erroLista && <p role="alert">{erroLista}</p>}
        {!carregandoLista && !erroLista && emissoes.length === 0 && <p className={estilos.textoSecundario}>Nenhuma emissão formal encontrada.</p>}
        {pagina.totalPaginas > 1 && <div className={estilos.paginacao}><button type="button" disabled={pagina.primeira} onClick={() => carregarLista(Math.max(0, pagina.numero - 1))}>Anterior</button><span>Página {pagina.numero + 1} de {pagina.totalPaginas}</span><button type="button" disabled={pagina.ultima} onClick={() => carregarLista(pagina.numero + 1)}>Próxima</button></div>}
      </section>
    </div>
  );
};
