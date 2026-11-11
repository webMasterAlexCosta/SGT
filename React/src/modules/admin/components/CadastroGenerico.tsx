import { observarFalha } from '../../../core/http/falhaObservada';
import type { CatalogRecord, ScalarCatalogProps, CatalogColumn } from '../../../contracts/catalogos';
import React, { useState, useEffect, useMemo, useCallback } from "react";
import { Plus, Edit, Trash2, CheckCircle, XCircle, Search, RotateCcw, LockKeyhole } from "lucide-react";
import { ServicoRecursoGenerico } from "../../../services/admin/ServicoRecursoGenerico";
import { Botao, Cartao, Tabela, JanelaModal, CampoEntrada } from "../../../core/ui/EnvolucroDica";
import { useConfirmacaoSenha } from "../../../core/security/useConfirmacaoSenha.tsx";
import { notificar } from "../../../core/events.ts";
import estilos from "./CadastroGenerico.module.css";

const TAMANHOS_PAGINA = [5, 20, 50, 100];

export const CadastroGenerico = ({
  title: titulo,
  descricao,
  serviceEndpoint: endpointServico,
  columns: colunas,
  fields: campos,
  podeEscrever = false,
  paginado = true,
  suportaAtivo = false,
  permiteExcluir = true,
  exclusaoEhInativacao = false,
  avisoSomenteLeitura = null,
}: ScalarCatalogProps) => {
  const [dados, setDados] = useState<CatalogRecord[]>([]);
  const [termoBusca, setTermoBusca] = useState("");
  const [filtroAtivo, setFiltroAtivo] = useState("TODOS");
  const [modalAberto, setModalAberto] = useState<{ aberto: boolean; item: CatalogRecord | null }>({ aberto: false, item: null });
  const [dadosFormulario, setDadosFormulario] = useState<CatalogRecord>({});
  const [carregando, setCarregando] = useState(false);
  const [erro, setErro] = useState<Partial<import('../../../core/http/types').HttpError> | null>(null);
  const [pagina, setPagina] = useState(0);
  const [tamanho, setTamanho] = useState(5);
  const [totalPaginas, setTotalPaginas] = useState(1);
  const [totalElementos, setTotalElementos] = useState(0);

  const { confirmarComSenha, ModalConfirmacao } = useConfirmacaoSenha();
  const servico = useMemo(() => ServicoRecursoGenerico(endpointServico), [endpointServico]);
  const paginaConsulta = paginado ? pagina : 0;
  const tamanhoConsulta = paginado ? tamanho : null;

  const atualizarDados = useCallback(async (opcoes = {}) => {
    setCarregando(true);
    setErro(null);
    try {
      if (paginado) {
        const resposta = await servico.listarPagina({ page: paginaConsulta, size: tamanhoConsulta, sort: "id,desc" }, opcoes);
        setDados(Array.isArray(resposta?.content) ? resposta.content.map((item) => ({ ...item })) : []);
        setTotalPaginas(Math.max(1, Number(resposta?.totalPages || 1)));
        setTotalElementos(Number(resposta?.totalElements || 0));
      } else {
        const resposta = await servico.listarTodos(opcoes);
        const lista = Array.isArray(resposta) ? resposta : [];
        setDados(lista.map((item) => ({ ...item })).sort((a, b) => Number(b?.id || 0) - Number(a?.id || 0)));
        setTotalPaginas(1);
        setTotalElementos(lista.length);
      }
    } catch (erroCarregamentoOriginal) {
      const erroCarregamento = observarFalha(erroCarregamentoOriginal);

      if (erroCarregamento?.canceled) return;
      setErro(erroCarregamento);
      setDados([]);
    } finally {
      setCarregando(false);
    }
  }, [paginado, paginaConsulta, tamanhoConsulta, servico]);

  useEffect(() => {
    const controlador = new AbortController();
    atualizarDados({ signal: controlador.signal });
    return () => controlador.abort();
  }, [atualizarDados]);

  const dadosFiltrados = useMemo(() => {
    const termo = termoBusca.trim().toLocaleLowerCase("pt-BR");
    return dados.filter((item) => {
      if (filtroAtivo === "ATIVOS" && item?.ativo === false) return false;
      if (filtroAtivo === "INATIVOS" && item?.ativo !== false) return false;
      if (!termo) return true;
      return Object.values(item || {}).some((valor) =>
        String(valor ?? "").toLocaleLowerCase("pt-BR").includes(termo),
      );
    });
  }, [dados, termoBusca, filtroAtivo]);

  const totalPaginasVisiveis = paginado ? totalPaginas : Math.max(1, Math.ceil(dadosFiltrados.length / tamanho));
  const totalRegistrosVisiveis = paginado ? totalElementos : dadosFiltrados.length;
  const dadosVisiveis = paginado ? dadosFiltrados : dadosFiltrados.slice(pagina * tamanho, (pagina + 1) * tamanho);

  useEffect(() => {
    if (pagina > 0 && pagina >= totalPaginasVisiveis) setPagina(Math.max(0, totalPaginasVisiveis - 1));
  }, [pagina, totalPaginasVisiveis]);

  useEffect(() => {
    if (!paginado) setPagina(0);
  }, [paginado, termoBusca, filtroAtivo]);

  const abrirModal = (item: CatalogRecord | null = null, forcarAtivo: boolean | null = null) => {
    if (!podeEscrever) return;
    if (item) {
      setDadosFormulario({ ...item, ...(forcarAtivo == null ? {} : { ativo: forcarAtivo }) });
    } else {
      const valoresPadrao: CatalogRecord = {};
      campos.forEach((campo) => {
        if (campo.defaultValue !== undefined) Object.assign(valoresPadrao, { [campo.key]: campo.defaultValue });
      });
      if (suportaAtivo && valoresPadrao.ativo === undefined) valoresPadrao.ativo = true;
      setDadosFormulario(valoresPadrao);
    }
    setModalAberto({ aberto: true, item });
  };

  const salvarDados = async (evento: React.FormEvent<HTMLFormElement>) => {
    evento.preventDefault();
    await confirmarComSenha({
      titulo: modalAberto.item ? "Confirmar alteração" : "Confirmar criação",
      mensagem: "Informe sua senha para continuar. O servidor validará escopo, vínculos e regras temporais.",
      acao: async (senhaConfirmacao) => {
        await servico.salvar(dadosFormulario, { senhaConfirmacao });
        setModalAberto({ aberto: false, item: null });
        await atualizarDados();
      },
    });
  };

  const excluirDados = async (item: (CatalogRecord)) => {
    if (!podeEscrever || !permiteExcluir) return;
    const verbo = exclusaoEhInativacao ? "Inativar registro" : "Excluir registro";
    const mensagem = exclusaoEhInativacao
      ? "A identidade histórica será preservada. O backend pode bloquear a inativação se houver programação futura."
      : "O backend validará vínculos antes da exclusão. Informe sua senha para prosseguir.";
    await confirmarComSenha({
      titulo: verbo,
      mensagem,
      acao: async (senhaConfirmacao) => {
        await servico.excluir(item.id, { senhaConfirmacao });
        await atualizarDados();
      },
    });
  };

  const reativar = async (item: (CatalogRecord)) => {
    if (!podeEscrever || !suportaAtivo) return;
    await confirmarComSenha({
      titulo: "Reativar cadastro",
      mensagem: "A mesma identidade será reativada; o histórico anterior continuará preservado.",
      acao: async (senhaConfirmacao) => {
        await servico.salvar({ ...item, ativo: true }, { senhaConfirmacao });
        notificar("success", "Cadastro reativado.");
        await atualizarDados();
      },
    });
  };

  const renderizarCelula = (item: (CatalogRecord), coluna: CatalogColumn) => {
    const valor = coluna.render ? coluna.render(item) : item[coluna.key];
    if (coluna.type === "boolean") {
      const rotuloVerdadeiro = coluna.trueLabel || "ATIVO";
      const rotuloFalso = coluna.falseLabel || "INATIVO";
      return valor ? <span className={estilos.emblemaAtivo}><CheckCircle size={14} /> {rotuloVerdadeiro}</span>
        : <span className={estilos.emblemaInativo}><XCircle size={14} /> {rotuloFalso}</span>;
    }
    if (coluna.type === "date" && valor) return new Date(typeof valor === 'number' ? valor : String(valor)).toLocaleDateString("pt-BR", { timeZone: "UTC" });
    return valor ?? "—";
  };

  const ehModoEdicao = Boolean(modalAberto.item);

  return (
    <div className={estilos.containerPagina}>
      <div className={estilos.cabecalhoPagina}>
        <div>
          <h2 className={estilos.titulo}>{titulo}</h2>
          {descricao && <p className={estilos.descricao}>{descricao}</p>}
        </div>
        <div className={estilos.controlesCabecalho}>
          <div className={estilos.envoltorioBusca}>
            <input type="search" placeholder={paginado ? "Buscar nesta página..." : "Buscar nos registros..."} value={termoBusca} onChange={(e) => setTermoBusca(e.target.value)} className={estilos.entradaBusca} aria-label={`Buscar em ${titulo}`} />
            <Search size={16} className={estilos.iconeBusca} />
          </div>
          {podeEscrever && <Botao onClick={() => abrirModal(null)}><Plus size={16} /> Novo</Botao>}
        </div>
      </div>

      {!podeEscrever && (
        <div className={estilos.avisoLeitura} role="status">
          <LockKeyhole size={17} />
          <span>{avisoSomenteLeitura || "Seu perfil possui acesso de consulta. Alterações permanecem protegidas pelo backend."}</span>
        </div>
      )}

      {suportaAtivo && (
        <div className={estilos.filtrosStatus} aria-label="Filtro por disponibilidade">
          {[["TODOS", "Todos"], ["ATIVOS", "Ativos"], ["INATIVOS", "Inativos / históricos"]].map(([valor, rotulo]) => (
            <button type="button" key={valor} aria-pressed={filtroAtivo === valor} onClick={() => setFiltroAtivo(valor)}>{rotulo}</button>
          ))}
        </div>
      )}

      <Cartao>
        {carregando ? <div className={estilos.mensagemVazia}>Carregando registros...</div>
          : erro ? <div className={estilos.mensagemErro}>Não foi possível carregar os registros. {erro?.requestId ? `Referência: ${erro.requestId}` : ""}<button type="button" onClick={() => atualizarDados()}>Tentar novamente</button></div>
          : dadosFiltrados.length === 0 ? <div className={estilos.mensagemVazia}>Nenhum registro encontrado.</div>
          : (
            <Tabela headers={[...colunas.map((c) => c.label), ...(podeEscrever ? ["Ações"] : [])]}>
              {dadosVisiveis.map((item) => (
                <tr key={item.id} className={estilos.tabelaLinha}>
                  {colunas.map((coluna, indice) => <td key={coluna.key || coluna.label || indice} className={estilos.celula}>{renderizarCelula(item, coluna)}</td>)}
                  {podeEscrever && (
                    <td className={estilos.celula}>
                      <div className={estilos.acoes}>
                        <button type="button" onClick={() => abrirModal(item)} className={`${estilos.botaoAcao} ${estilos.botaoEditar}`} title="Editar" aria-label={`Editar ${titulo}`}><Edit size={16} /></button>
                        {suportaAtivo && item?.ativo === false && <button type="button" onClick={() => reativar(item)} className={estilos.botaoAcao} title="Reativar" aria-label={`Reativar ${titulo}`}><RotateCcw size={16} /></button>}
                        {permiteExcluir && item?.ativo !== false && <button type="button" onClick={() => excluirDados(item)} className={`${estilos.botaoAcao} ${estilos.botaoExcluir}`} title={exclusaoEhInativacao ? "Inativar" : "Excluir"} aria-label={`${exclusaoEhInativacao ? "Inativar" : "Excluir"} ${titulo}`}><Trash2 size={16} /></button>}
                      </div>
                    </td>
                  )}
                </tr>
              ))}
            </Tabela>
          )}
      </Cartao>

        <div className={estilos.paginacao}>
          <span>{totalRegistrosVisiveis} registro(s) · página {pagina + 1} de {totalPaginasVisiveis}</span>
          <label>Itens por página <select value={tamanho} onChange={(e) => { setTamanho(Number(e.target.value)); setPagina(0); }}>{TAMANHOS_PAGINA.map((n) => <option key={n} value={n}>{n}</option>)}</select></label>
          <div><button type="button" onClick={() => setPagina((p) => Math.max(0, p - 1))} disabled={pagina <= 0}>Anterior</button><button type="button" onClick={() => setPagina((p) => Math.min(totalPaginasVisiveis - 1, p + 1))} disabled={pagina + 1 >= totalPaginasVisiveis}>Próxima</button></div>
        </div>

      <JanelaModal isOpen={modalAberto.aberto} onClose={() => setModalAberto({ aberto: false, item: null })} title={modalAberto.item ? "Editar registro" : "Novo registro"} maxWidth="max-w-md">
        <form onSubmit={salvarDados} className={estilos.formularioModal}>
          {campos.map((campo) => {
            const somenteLeitura = ehModoEdicao && campo.readOnlyOnEdit;
            if (campo.type === "checkbox") {
              return <label key={campo.key} className={`${estilos.linhaCheckbox} ${somenteLeitura ? estilos.linhaCheckboxDesabilitada : ""}`}><input type="checkbox" checked={Boolean(dadosFormulario[campo.key])} onChange={(e) => setDadosFormulario((atual) => ({ ...atual, [campo.key]: e.target.checked }))} disabled={somenteLeitura} className={estilos.entradaCheckbox} /><span className={estilos.rotuloCheckbox}>{campo.label}</span></label>;
            }
            return <CampoEntrada key={campo.key} label={campo.label} type={campo.type || "text"} value={String(dadosFormulario[campo.key] ?? "")} onChange={(e) => setDadosFormulario((atual) => ({ ...atual, [campo.key]: e.target.value }))} required={campo.required} disabled={somenteLeitura} />;
          })}
          <div className={estilos.rodapeModal}><Botao variant="secondary" type="button" onClick={() => setModalAberto({ aberto: false, item: null })}>Cancelar</Botao><Botao type="submit">Salvar</Botao></div>
        </form>
      </JanelaModal>
      <ModalConfirmacao />
    </div>
  );
};
