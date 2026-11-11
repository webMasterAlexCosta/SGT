import { observarFalha } from '../../../../core/http/falhaObservada';
import type * as Input from '../../../../contracts/java-requests';
import type { RequestOptions, HttpError } from '../../../../core/http/types';
import type { PersonDetailsView, ContractOptionDisplay, LinkCommand, AllocationSubmission } from '../../../../contracts/pessoas-vinculos';
import type * as DTO from "../../../../contracts/java";
import type { Page } from "../../../../contracts/shared";
import type { MutableForm } from '../../../../contracts/formularios';
import React, { useCallback, useEffect, useMemo, useState } from "react";
import {
  CalendarRange, ChevronLeft, ChevronRight, Eye, Filter, History, Link2,
  Plus, RefreshCw, Search, Settings2, ShieldCheck, UserRound, UsersRound,
} from "lucide-react";
import { useAutenticacao } from "../../../auth/context.tsx";
import { useConfirmacaoSenha } from "../../../../core/security/useConfirmacaoSenha.tsx";
import { podeGerenciarPessoasVinculos } from "../../../../core/security/roles.ts";
import { notificar } from "../../../../core/events.ts";
import { ServicoContrato } from "../../../../services/admin/ServicoContrato.ts";
import { ServicoPessoa } from "../../../../services/rh/ServicoPessoa.ts";
import { ServicoVinculoContratual } from "../../../../services/rh/ServicoVinculoContratual.ts";
import { ServicoMotivoDesligamento } from "../../../../services/rh/ServicoMotivoDesligamento.ts";
import {
  ESTADOS_VINCULO,
  formatarCpfProtegido,
  formatarDataPessoaVinculo,
  formatarInstantePessoaVinculo,
  rotularEstadoVinculo,
  rotularSituacaoVinculo,
} from "../../../../services/rh/ServicoDominioPessoaVinculo.ts";
import { ModalCadastroVinculo } from "./components/ModalCadastroVinculo.tsx";
import { ModalPessoa } from "./components/ModalPessoa.tsx";
import { ModalVinculo, ModalAcaoVinculo } from "./components/ModalVinculo.tsx";
import { ModalAlocacoesVinculo } from "./components/ModalAlocacoesVinculo.tsx";
import { ModalMotivosDesligamento } from "./components/ModalMotivosDesligamento.tsx";
import estilos from "./index.module.css";

const PAGINA_VAZIA = Object.freeze({ content: [], number: 0, size: 5, totalElements: 0, totalPages: 0, first: true, last: true });
const FILTROS_PESSOA = Object.freeze({ busca: "", contratoId: "", ativoEm: "" });
const FILTROS_VINCULO = Object.freeze({ busca: "", contratoId: "", estado: "", inicioDe: "", inicioAte: "", ativoEm: "" });

const rotuloContrato = (contrato: ContractOptionDisplay) => {
  const numero = contrato.numeroContrato || contrato.numero || contrato.label || `Contrato ${contrato.id}`;
  const ano = contrato.anoContrato || contrato.ano;
  return ano ? `${numero}/${ano}` : numero;
};

const mensagemErro = (erro: Partial<HttpError>, fallback: string) => {
  if (erro?.status === 409) return erro.message || "O registro mudou ou conflita com outro fato. Atualize os dados antes de decidir novamente.";
  if (erro?.resultadoIncerto) return `O resultado da operação é incerto. Não repita sem conferir a linha do tempo.${erro.requestId ? ` Referência: ${erro.requestId}.` : ""}`;
  return erro?.message || fallback;
};

const EstadoCarregamento = ({ carregando, vazio, children }: { carregando: (boolean); vazio: (boolean); children: React.ReactNode }) => {
  if (carregando) return <div className={estilos.estadoCentral}><RefreshCw className={estilos.girando} size={22} /><strong>Consultando o servidor…</strong><span>A paginação e o escopo são aplicados pela API.</span></div>;
  if (vazio) return <div className={estilos.estadoCentral}><Search size={24} /><strong>Nenhum resultado</strong><span>Altere os filtros ou verifique seu escopo de gestão.</span></div>;
  return children;
};

const Paginacao = ({ pagina, aoMudar }: { pagina: (Partial<import("../../../../contracts/shared").Page<DTO.PessoaResumoDTO>> & { content: DTO.PessoaResumoDTO[]; }) | (Partial<import("../../../../contracts/shared").Page<DTO.VinculoContratualDTO>> & { content: DTO.VinculoContratualDTO[]; }); aoMudar: (React.Dispatch<React.SetStateAction<number>>) }) => {
  const atual = Number(pagina.number || 0);
  const total = Number(pagina.totalPages || 0);
  return (
    <nav className={estilos.paginacao} aria-label="Paginação">
      <span>{pagina.totalElements || 0} registro(s) · página {total ? atual + 1 : 0} de {total}</span>
      <div><button type="button" onClick={() => aoMudar(atual - 1)} disabled={pagina.first || atual <= 0}><ChevronLeft size={17} /> Anterior</button><button type="button" onClick={() => aoMudar(atual + 1)} disabled={pagina.last || atual + 1 >= total}>Próxima <ChevronRight size={17} /></button></div>
    </nav>
  );
};

export const PaginaPessoasVinculos = () => {
  const { user } = useAutenticacao();
  const { confirmarComSenha, ModalConfirmacao } = useConfirmacaoSenha();
  const podeGerenciar = useMemo(() => podeGerenciarPessoasVinculos(user), [user]);
  const [aba, setAba] = useState("PESSOAS");
  const [contratos, setContratos] = useState<(import("../../../../contracts/opcoes-apresentacao").ContractOptionView[])>([]);
  const [motivos, setMotivos] = useState<(DTO.MotivoDesligamentoDTO[])>([]);

  const [filtrosPessoa, setFiltrosPessoa] = useState<MutableForm<typeof FILTROS_PESSOA>>({ ...FILTROS_PESSOA });
  const [consultaPessoa, setConsultaPessoa] = useState<MutableForm<typeof FILTROS_PESSOA>>({ ...FILTROS_PESSOA });
  const [paginaPessoas, setPaginaPessoas] = useState<Partial<Page<DTO.PessoaResumoDTO>> & { content: DTO.PessoaResumoDTO[] }>({ ...PAGINA_VAZIA });
  const [numeroPaginaPessoas, setNumeroPaginaPessoas] = useState(0);
  const [carregandoPessoas, setCarregandoPessoas] = useState(false);

  const [filtrosVinculo, setFiltrosVinculo] = useState<MutableForm<typeof FILTROS_VINCULO>>({ ...FILTROS_VINCULO });
  const [consultaVinculo, setConsultaVinculo] = useState<MutableForm<typeof FILTROS_VINCULO>>({ ...FILTROS_VINCULO });
  const [paginaVinculos, setPaginaVinculos] = useState<Partial<Page<DTO.VinculoContratualDTO>> & { content: DTO.VinculoContratualDTO[] }>({ ...PAGINA_VAZIA });
  const [numeroPaginaVinculos, setNumeroPaginaVinculos] = useState(0);
  const [carregandoVinculos, setCarregandoVinculos] = useState(false);

  const [pessoaAberta, setPessoaAberta] = useState(false);
  const [pessoaDetalhe, setPessoaDetalhe] = useState<PersonDetailsView | null>(null);
  const [vinculosPessoa, setVinculosPessoa] = useState<(DTO.VinculoContratualDTO[])>([]);
  const [correcoesPessoa, setCorrecoesPessoa] = useState<(DTO.CorrecaoPessoaDTO[])>([]);
  const [carregandoPessoa, setCarregandoPessoa] = useState(false);

  const [vinculoAberto, setVinculoAberto] = useState(false);
  const [alocacoesAbertas, setAlocacoesAbertas] = useState(false);
  const [vinculoDetalhe, setVinculoDetalhe] = useState<(DTO.VinculoContratualDTO) | null>(null);
  const [eventosVinculo, setEventosVinculo] = useState<(DTO.EventoVinculoContratualDTO[])>([]);
  const [posicaoPessoa, setPosicaoPessoa] = useState<(DTO.PosicaoPessoaDTO) | null>(null);
  const [carregandoVinculo, setCarregandoVinculo] = useState(false);
  const [consultandoTemporal, setConsultandoTemporal] = useState(false);

  const [cadastroAberto, setCadastroAberto] = useState(false);
  const [pessoaInicial, setPessoaInicial] = useState<PersonDetailsView | null>(null);
  const [acaoVinculo, setAcaoVinculo] = useState<string | null>(null);
  const [motivosAbertos, setMotivosAbertos] = useState(false);
  const [processando, setProcessando] = useState(false);

  const carregarCatalogos = useCallback(async () => {
    try {
      const [opcoesContratos, listaMotivos] = await Promise.all([
        ServicoContrato.obterOpcoes(),
        ServicoMotivoDesligamento.listar(),
      ]);
      setContratos(opcoesContratos);
      setMotivos(listaMotivos);
    } catch (erroOriginal) {
      const erro = observarFalha(erroOriginal);
 if (!erro?.canceled) notificar("error", mensagemErro(erro, "Não foi possível carregar os catálogos da tela.")); }
  }, []);

  useEffect(() => { carregarCatalogos(); }, [carregarCatalogos]);

  const carregarPessoas = useCallback(async (controladorExterno?: AbortController) => {
    const controlador = controladorExterno || new AbortController();
    setCarregandoPessoas(true);
    try {
      const pagina = await ServicoPessoa.listar({ ...consultaPessoa, page: numeroPaginaPessoas, size: 5, sort: "nome,asc" }, { signal: controlador.signal });
      setPaginaPessoas(pagina);
    } catch (erroOriginal) {
      const erro = observarFalha(erroOriginal);
 if (!erro?.canceled) notificar("error", mensagemErro(erro, "Não foi possível consultar pessoas.")); }
    finally { if (!controlador.signal.aborted) setCarregandoPessoas(false); }
    return controlador;
  }, [consultaPessoa, numeroPaginaPessoas]);

  const carregarVinculos = useCallback(async (controladorExterno?: AbortController) => {
    const controlador = controladorExterno || new AbortController();
    setCarregandoVinculos(true);
    try {
      const pagina = await ServicoVinculoContratual.listar({ ...consultaVinculo, page: numeroPaginaVinculos, size: 5, sort: "dataInicio,desc" }, { signal: controlador.signal });
      setPaginaVinculos(pagina);
    } catch (erroOriginal) {
      const erro = observarFalha(erroOriginal);
 if (!erro?.canceled) notificar("error", mensagemErro(erro, "Não foi possível consultar vínculos contratuais.")); }
    finally { if (!controlador.signal.aborted) setCarregandoVinculos(false); }
    return controlador;
  }, [consultaVinculo, numeroPaginaVinculos]);

  useEffect(() => { const controlador = new AbortController(); carregarPessoas(controlador); return () => controlador.abort(); }, [carregarPessoas]);
  useEffect(() => { const controlador = new AbortController(); carregarVinculos(controlador); return () => controlador.abort(); }, [carregarVinculos]);

  const pesquisarPessoas = (evento: React.FormEvent<HTMLFormElement>) => { evento.preventDefault(); setNumeroPaginaPessoas(0); setConsultaPessoa({ ...filtrosPessoa }); };
  const pesquisarVinculos = (evento: React.FormEvent<HTMLFormElement>) => { evento.preventDefault(); setNumeroPaginaVinculos(0); setConsultaVinculo({ ...filtrosVinculo }); };

  const abrirPessoa = async (pessoa: (DTO.PessoaResumoDTO)) => {
    setPessoaDetalhe(pessoa);
    setPessoaAberta(true);
    setVinculosPessoa([]);
    setCorrecoesPessoa([]);
    setCarregandoPessoa(true);
    try {
      const [detalhe, paginaDosVinculos, correcoes] = await Promise.all([
        ServicoPessoa.buscarPorId(pessoa.id),
        ServicoVinculoContratual.listar({ pessoaId: pessoa.id, page: 0, size: 100, sort: "dataInicio,desc" }),
        ServicoPessoa.listarCorrecoesIdentidade(pessoa.id),
      ]);
      setPessoaDetalhe(detalhe);
      setVinculosPessoa(paginaDosVinculos.content || []);
      setCorrecoesPessoa(correcoes);
    } catch (erroOriginal) {
      const erro = observarFalha(erroOriginal);
 notificar("error", mensagemErro(erro, "Não foi possível carregar os detalhes da pessoa.")); }
    finally { setCarregandoPessoa(false); }
  };

  const atualizarPessoaAberta = async () => {
    if (!pessoaDetalhe?.id) return;
    const [detalhe, correcoes] = await Promise.all([ServicoPessoa.buscarPorId(pessoaDetalhe?.id), ServicoPessoa.listarCorrecoesIdentidade(pessoaDetalhe?.id)]);
    setPessoaDetalhe(detalhe);
    setCorrecoesPessoa(correcoes);
  };

  const abrirVinculo = async (vinculo: (DTO.VinculoContratualDTO)) => {
    setVinculoDetalhe(vinculo);
    setVinculoAberto(true);
    setPessoaAberta(false);
    setEventosVinculo([]);
    setPosicaoPessoa(null);
    setCarregandoVinculo(true);
    try {
      const [detalhe, eventos] = await Promise.all([ServicoVinculoContratual.buscarPorId(vinculo.id), ServicoVinculoContratual.listarEventos(vinculo.id)]);
      setVinculoDetalhe(detalhe);
      setEventosVinculo(eventos);
    } catch (erroOriginal) {
      const erro = observarFalha(erroOriginal);
 notificar("error", mensagemErro(erro, "Não foi possível carregar o vínculo.")); }
    finally { setCarregandoVinculo(false); }
  };

  const atualizarVinculoAberto = async () => {
    if (!vinculoDetalhe?.id) return;
    const [detalhe, eventos] = await Promise.all([ServicoVinculoContratual.buscarPorId(vinculoDetalhe?.id), ServicoVinculoContratual.listarEventos(vinculoDetalhe?.id)]);
    setVinculoDetalhe(detalhe);
    setEventosVinculo(eventos);
  };

  const consultarTemporal = async ({ data, conhecidoAte }: { data?: string; conhecidoAte?: string }) => {
    if (!vinculoDetalhe?.pessoaId) return;
    setConsultandoTemporal(true);
    try { setPosicaoPessoa(await ServicoVinculoContratual.consultarPosicaoPessoa(vinculoDetalhe.pessoaId, { data, conhecidoAte })); }
    catch (erroOriginal) {
      const erro = observarFalha(erroOriginal);
 notificar("error", mensagemErro(erro, "Não foi possível obter a fotografia temporal.")); }
    finally { setConsultandoTemporal(false); }
  };

  const confirmarMutacao = ({ titulo, mensagem, acao }: { titulo: string; mensagem: string; acao: (options: RequestOptions) => unknown | Promise<unknown> }) => confirmarComSenha({ titulo, mensagem, acao: async (senhaConfirmacao) => {
    setProcessando(true);
    try { return await acao({ senhaConfirmacao }); }
    catch (erroOriginal) {
      const erro = observarFalha(erroOriginal);
 notificar("error", mensagemErro(erro, "Não foi possível concluir o comando.")); throw erroOriginal; }
    finally { setProcessando(false); }
  } });

  const cadastrarVinculo = (payload: Input.RequestCadastroVinculoContratualRequisicao) => confirmarMutacao({
    titulo: "Confirmar novo vínculo",
    mensagem: "A API validará CPF, matrícula, vigência, capacidade e escopo em uma única transação.",
    acao: async (options) => {
      const criado = await ServicoVinculoContratual.cadastrar(payload, options);
      setCadastroAberto(false);
      setPessoaInicial(null);
      await Promise.all([carregarPessoas(), carregarVinculos()]);
      if (criado) await abrirVinculo(criado);
    },
  });

  const atualizarOpcionais = (payload: Input.RequestAtualizacaoPessoaOpcionalRequisicao) => confirmarMutacao({
    titulo: "Atualizar dados opcionais",
    mensagem: "Campos vazios só serão apagados quando a opção de limpeza explícita estiver marcada.",
    acao: async (options) => { await ServicoPessoa.atualizarDadosOpcionais(pessoaDetalhe?.id, payload, options); await Promise.all([atualizarPessoaAberta(), carregarPessoas()]); },
  });

  const corrigirIdentidade = (payload: Input.RequestCorrecaoIdentidadePessoaRequisicao) => confirmarMutacao({
    titulo: "Confirmar correção excepcional",
    mensagem: "A identidade atual será corrigida sem reescrever as fotografias históricas dos eventos.",
    acao: async (options) => { await ServicoPessoa.corrigirIdentidade(pessoaDetalhe?.id, payload, options); await Promise.all([atualizarPessoaAberta(), carregarPessoas(), carregarVinculos()]); },
  });

  const gerarPrevia = (tipo: string, payload: LinkCommand) => {
    if (tipo === "RETIFICAR_INICIO") return ServicoVinculoContratual.gerarPreviaRetificacaoInicio(vinculoDetalhe?.id, payload);
    if (tipo === "RETIFICAR_DESLIGAMENTO") return ServicoVinculoContratual.gerarPreviaRetificacaoDesligamento(vinculoDetalhe?.id, payload);
    throw new Error("Esta ação não possui prévia de impacto.");
  };

  const abrirAlocacoes = () => {
    if (!vinculoDetalhe?.id) return;
    setVinculoAberto(false);
    setAlocacoesAbertas(true);
  };

  const salvarAlocacao = ({ modo, alocacaoId, payload }: AllocationSubmission) => confirmarMutacao({
    titulo: modo === "RETIFICAR" ? "Confirmar retificação de alocação" : "Confirmar alteração de alocação",
    mensagem: "O Java revalidará composição, linha temporal, capacidade, prévia, precedência e impactos antes de gravar o novo fato.",
    acao: async (options) => {
      if (modo === "RETIFICAR") await ServicoVinculoContratual.retificarAlocacao(vinculoDetalhe?.id, alocacaoId, payload, options);
      else await ServicoVinculoContratual.alterarAlocacao(vinculoDetalhe?.id, payload, options);
      await Promise.all([atualizarVinculoAberto(), carregarPessoas(), carregarVinculos()]);
    },
  });

  const cancelarAlocacao = (alocacao: DTO.AlocacaoVinculoDTO, payload: Input.RequestAcaoVinculoContratualRequisicao) => confirmarMutacao({
    titulo: "Cancelar programação futura de alocação",
    mensagem: "O servidor validará se a faixa ainda é futura e restaurará a predecessora quando aplicável, sem apagar a trilha histórica.",
    acao: async (options) => {
      await ServicoVinculoContratual.cancelarProgramacaoAlocacao(vinculoDetalhe?.id, alocacao.id, payload, options);
      await Promise.all([atualizarVinculoAberto(), carregarPessoas(), carregarVinculos()]);
    },
  });

  const executarAcaoVinculo = (tipo: string, payload: LinkCommand) => confirmarMutacao({
    titulo: "Confirmar comando sobre o vínculo",
    mensagem: "O servidor revalidará o estado atual e registrará datas efetiva e de conhecimento separadamente.",
    acao: async (options) => {
      if (tipo === "RETIFICAR_INICIO") await ServicoVinculoContratual.retificarInicio(vinculoDetalhe?.id, payload, options);
      else if (tipo === "ALTERAR_MATRICULA") await ServicoVinculoContratual.alterarMatricula(vinculoDetalhe?.id, payload, options);
      else if (tipo === "CANCELAR_PROGRAMACAO") await ServicoVinculoContratual.cancelarProgramacao(vinculoDetalhe?.id, payload, options);
      else if (tipo === "DESLIGAR") await ServicoVinculoContratual.desligar(vinculoDetalhe?.id, payload, options);
      else if (tipo === "RETIFICAR_DESLIGAMENTO") await ServicoVinculoContratual.retificarDesligamento(vinculoDetalhe?.id, payload, options);
      else if (tipo === "CANCELAR_DESLIGAMENTO") await ServicoVinculoContratual.cancelarDesligamentoProgramado(vinculoDetalhe?.id, payload, options);
      else throw new Error(`Comando não suportado: ${tipo}`);
      setAcaoVinculo(null);
      await Promise.all([atualizarVinculoAberto(), carregarPessoas(), carregarVinculos()]);
    },
  });

  const salvarMotivo = (dados: Input.RequestMotivoDesligamentoDTO) => confirmarMutacao({
    titulo: dados.id ? "Atualizar motivo de desligamento" : "Criar motivo de desligamento",
    mensagem: "O catálogo será alterado no escopo da Gestão Local sem modificar vínculos históricos.",
    acao: async (options) => {
      if (dados.id) await ServicoMotivoDesligamento.atualizar(dados.id, dados, options);
      else await ServicoMotivoDesligamento.criar(dados, options);
      setMotivos(await ServicoMotivoDesligamento.listar());
    },
  });

  const tratarFimNatural = () => {
    const contratoId = aba === "VINCULOS" ? consultaVinculo.contratoId : consultaPessoa.contratoId;
    if (!contratoId) { notificar("warning", "Filtre um contrato específico antes de tratar o fim natural."); return; }
    confirmarMutacao({
      titulo: "Tratar fim natural do contrato",
      mensagem: "A API encerrará apenas os vínculos elegíveis na data final canônica do contrato.",
      acao: async (options) => { await ServicoVinculoContratual.tratarFimNatural(contratoId, options); await Promise.all([carregarPessoas(), carregarVinculos()]); },
    });
  };

  return (
    <div className={estilos.pagina}>
      <header className={estilos.cabecalhoPagina}>
        <div className={estilos.identidadePagina}><span><UsersRound size={26} /></span><div><p>Gestão de terceirizados</p><h1>Pessoas e vínculos contratuais</h1><small>Identidade única, múltiplos vínculos e trilha temporal governada pela API.</small></div></div>
        {podeGerenciar && <div className={estilos.acoesCabecalho}><button type="button" className={estilos.botaoSecundario} onClick={() => setMotivosAbertos(true)}><Settings2 size={17} /> Motivos</button><button type="button" className={estilos.botaoSecundario} onClick={tratarFimNatural}><CalendarRange size={17} /> Fim natural</button><button type="button" className={estilos.botaoPrimario} onClick={() => { setPessoaInicial(null); setCadastroAberto(true); }}><Plus size={17} /> Novo vínculo</button></div>}
      </header>

      <div className={estilos.avisoModelo}><ShieldCheck size={20} /><div><strong>Modelo canônico ativo</strong><span>Criações e alterações usam `/pessoas` e `/vinculos-contratuais`. “Empregado” permanece somente como projeção de compatibilidade para módulos operacionais.</span></div></div>

      <div className={estilos.abas} role="tablist" aria-label="Cadastros de RH">
        <button type="button" role="tab" aria-selected={aba === "PESSOAS"} className={aba === "PESSOAS" ? estilos.abaAtiva : ""} onClick={() => setAba("PESSOAS")}><UserRound size={17} /> Pessoas <span>{paginaPessoas.totalElements || 0}</span></button>
        <button type="button" role="tab" aria-selected={aba === "VINCULOS"} className={aba === "VINCULOS" ? estilos.abaAtiva : ""} onClick={() => setAba("VINCULOS")}><Link2 size={17} /> Vínculos <span>{paginaVinculos.totalElements || 0}</span></button>
      </div>

      {aba === "PESSOAS" ? (
        <section className={estilos.cartaoLista}>
          <form className={estilos.filtros} onSubmit={pesquisarPessoas}>
            <label className={estilos.busca}><Search size={17} /><input value={filtrosPessoa.busca} onChange={(e) => setFiltrosPessoa((a) => ({ ...a, busca: e.target.value }))} placeholder="Buscar por nome ou CPF" /></label>
            <label><span>Contrato</span><select value={filtrosPessoa.contratoId} onChange={(e) => setFiltrosPessoa((a) => ({ ...a, contratoId: e.target.value }))}><option value="">Todos do escopo</option>{contratos.map((contrato) => <option key={contrato.id} value={(contrato.id) ?? undefined}>{rotuloContrato(contrato)}</option>)}</select></label>
            <label><span>Ativo em</span><input type="date" value={filtrosPessoa.ativoEm} onChange={(e) => setFiltrosPessoa((a) => ({ ...a, ativoEm: e.target.value }))} /></label>
            <button type="submit" className={estilos.botaoPrimario}><Filter size={16} /> Aplicar filtros</button>
          </form>
          <EstadoCarregamento carregando={carregandoPessoas} vazio={!paginaPessoas.content?.length}>
            <div className={estilos.tabelaResponsiva}><table><thead><tr><th>Pessoa</th><th>CPF protegido</th><th>Atualização</th><th aria-label="Ações" /></tr></thead><tbody>{paginaPessoas.content?.map((pessoa) => <tr key={pessoa.id}><td><strong>{pessoa.nome}</strong><small>ID {pessoa.id}</small></td><td>{formatarCpfProtegido(pessoa.cpf)}</td><td>{formatarInstantePessoaVinculo(pessoa.atualizadoEm)}</td><td><button type="button" className={estilos.botaoTabela} onClick={() => abrirPessoa(pessoa)}><Eye size={16} /> Detalhes</button></td></tr>)}</tbody></table></div>
            <Paginacao pagina={paginaPessoas} aoMudar={setNumeroPaginaPessoas} />
          </EstadoCarregamento>
        </section>
      ) : (
        <section className={estilos.cartaoLista}>
          <form className={`${estilos.filtros} ${estilos.filtrosVinculo}`} onSubmit={pesquisarVinculos}>
            <label className={estilos.busca}><Search size={17} /><input value={filtrosVinculo.busca} onChange={(e) => setFiltrosVinculo((a) => ({ ...a, busca: e.target.value }))} placeholder="Nome, CPF ou matrícula" /></label>
            <label><span>Contrato</span><select value={filtrosVinculo.contratoId} onChange={(e) => setFiltrosVinculo((a) => ({ ...a, contratoId: e.target.value }))}><option value="">Todos do escopo</option>{contratos.map((contrato) => <option key={contrato.id} value={(contrato.id) ?? undefined}>{rotuloContrato(contrato)}</option>)}</select></label>
            <label><span>Estado negocial</span><select value={filtrosVinculo.estado} onChange={(e) => setFiltrosVinculo((a) => ({ ...a, estado: e.target.value }))}><option value="">Todos</option>{ESTADOS_VINCULO.map((estado) => <option key={estado.value} value={estado.value}>{estado.label}</option>)}</select></label>
            <label><span>Início de</span><input type="date" value={filtrosVinculo.inicioDe} onChange={(e) => setFiltrosVinculo((a) => ({ ...a, inicioDe: e.target.value }))} /></label>
            <label><span>Início até</span><input type="date" value={filtrosVinculo.inicioAte} onChange={(e) => setFiltrosVinculo((a) => ({ ...a, inicioAte: e.target.value }))} /></label>
            <label><span>Ativo em</span><input type="date" value={filtrosVinculo.ativoEm} onChange={(e) => setFiltrosVinculo((a) => ({ ...a, ativoEm: e.target.value }))} /></label>
            <button type="submit" className={estilos.botaoPrimario}><Filter size={16} /> Aplicar filtros</button>
          </form>
          <EstadoCarregamento carregando={carregandoVinculos} vazio={!paginaVinculos.content?.length}>
            <div className={estilos.tabelaResponsiva}><table><thead><tr><th>Pessoa</th><th>Contrato / matrícula</th><th>Vigência</th><th>Situação</th><th aria-label="Ações" /></tr></thead><tbody>{paginaVinculos.content?.map((vinculo) => <tr key={vinculo.id}><td><strong>{vinculo.pessoaNome}</strong><small>{formatarCpfProtegido(vinculo.pessoaCpf)}</small></td><td><strong>{vinculo.contratoNumero || vinculo.contratoId}{vinculo.contratoAno ? `/${vinculo.contratoAno}` : ""}</strong><small>{vinculo.matricula || "Sem matrícula"}</small></td><td>{formatarDataPessoaVinculo(vinculo.dataInicio)}<small>até {formatarDataPessoaVinculo(vinculo.dataFim, "em aberto")}</small></td><td><span className={estilos.pilula}>{rotularSituacaoVinculo(vinculo.situacaoTemporal)}</span><small>{rotularEstadoVinculo(vinculo.estadoNegocial)}</small></td><td><button type="button" className={estilos.botaoTabela} onClick={() => abrirVinculo(vinculo)}><History size={16} /> Linha do tempo</button></td></tr>)}</tbody></table></div>
            <Paginacao pagina={paginaVinculos} aoMudar={setNumeroPaginaVinculos} />
          </EstadoCarregamento>
        </section>
      )}

      <ModalCadastroVinculo aberto={cadastroAberto} contratos={contratos} pessoas={paginaPessoas.content} pessoaInicial={pessoaInicial} salvando={processando} aoFechar={() => { if (!processando) { setCadastroAberto(false); setPessoaInicial(null); } }} aoSalvar={cadastrarVinculo} />
      <ModalPessoa aberto={pessoaAberta} pessoa={pessoaDetalhe} vinculos={vinculosPessoa} correcoes={correcoesPessoa} carregando={carregandoPessoa} processando={processando} podeGerenciar={podeGerenciar} aoFechar={() => setPessoaAberta(false)} aoAbrirVinculo={abrirVinculo} aoNovoVinculo={(pessoa) => { setPessoaAberta(false); setPessoaInicial(pessoa); setCadastroAberto(true); }} aoSolicitarRestritos={(finalidade) => ServicoPessoa.buscarDadosRestritos(pessoaDetalhe?.id, finalidade)} aoAtualizar={atualizarOpcionais} aoCorrigir={corrigirIdentidade} />
      <ModalVinculo aberto={vinculoAberto} vinculo={vinculoDetalhe} eventos={eventosVinculo} posicao={posicaoPessoa} carregando={carregandoVinculo} consultandoTemporal={consultandoTemporal} podeGerenciar={podeGerenciar} aoFechar={() => setVinculoAberto(false)} aoConsultarTemporal={consultarTemporal} aoAbrirAcao={setAcaoVinculo} aoAbrirAlocacoes={abrirAlocacoes} />
      <ModalAlocacoesVinculo aberto={alocacoesAbertas} vinculo={vinculoDetalhe} podeGerenciar={podeGerenciar} processando={processando} aoFechar={() => { setAlocacoesAbertas(false); if (vinculoDetalhe) setVinculoAberto(true); }} aoSalvarAlocacao={salvarAlocacao} aoCancelarAlocacao={cancelarAlocacao} />
      <ModalAcaoVinculo aberto={Boolean(acaoVinculo)} tipo={acaoVinculo ?? ''} vinculo={vinculoDetalhe} motivos={motivos} processando={processando} aoFechar={() => setAcaoVinculo(null)} aoPrevia={gerarPrevia} aoExecutar={executarAcaoVinculo} />
      <ModalMotivosDesligamento aberto={motivosAbertos} motivos={motivos} processando={processando} aoFechar={() => setMotivosAbertos(false)} aoSalvar={salvarMotivo} />
      <ModalConfirmacao />
    </div>
  );
};
