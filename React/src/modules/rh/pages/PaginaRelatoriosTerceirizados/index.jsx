import React, { useCallback, useEffect, useMemo, useRef, useState } from "react";
import { useNavigate } from "react-router-dom";
import {
  ServicoRelatorioTerceirizados,
  RELATORIO_TERCEIRIZADOS_TAMANHO_MAXIMO,
} from "../../../../services/rh/ServicoRelatorioTerceirizados";
import { CONFIG } from "../../../../core/config";
import { useExportacaoRelatorio } from "../../../../core/exportacao/context";
import { notificar } from "../../../../core/events";
import { CabecalhoRelatorios } from "./components/CabecalhoRelatorios";
import { IndicadoresRelatorios } from "./components/IndicadoresRelatorios";
import { FiltrosRelatorios } from "./components/FiltrosRelatorios";
import { GraficosRelatorios } from "./components/GraficosRelatorios";
import { TabelaEmpregadosRelatorio } from "./components/TabelaEmpregadosRelatorio";
import { CatalogoRelatorios } from "./components/CatalogoRelatorios";
import estilos from "./index.module.css";

const TAMANHO_PADRAO = 5;
const TAMANHO_TODOS = "TODOS";

const PAGINACAO_INICIAL = {
  content: [],
  totalElements: 0,
  totalPages: 0,
  number: 0,
  size: TAMANHO_PADRAO,
  first: true,
  last: true,
};

const debounceMs = Math.min(1000, Math.max(200, Number(CONFIG.SEARCH_DEBOUNCE_MS || 400)));

export const PaginaRelatoriosTerceirizados = () => {
  const { iniciarExportacao } = useExportacaoRelatorio();
  const navegar = useNavigate();
  const controladorRef = useRef(null);
  const [dados, setDados] = useState(null);
  const [carregando, setCarregando] = useState(true);
  const [erroConsulta, setErroConsulta] = useState("");
  const [termoBusca, setTermoBusca] = useState("");
  const [buscaAplicada, setBuscaAplicada] = useState("");
  const [statusSelecionado, setStatusSelecionado] = useState("TODOS");
  const [contratoSelecionado, setContratoSelecionado] = useState("TODOS");
  const [lotacaoSelecionada, setLotacaoSelecionada] = useState("TODAS");
  const [pagina, setPagina] = useState(0);
  const [tamanhoPagina, setTamanhoPagina] = useState(TAMANHO_PADRAO);
  const [versaoConsulta, setVersaoConsulta] = useState(0);
  const [exportandoCompleto, setExportandoCompleto] = useState("");
  const [progressoCargaTotal, setProgressoCargaTotal] = useState(null);

  useEffect(() => {
    const timer = window.setTimeout(() => {
      setPagina(0);
      setBuscaAplicada(termoBusca.trim());
    }, debounceMs);
    return () => window.clearTimeout(timer);
  }, [termoBusca]);

  const filtrosConsulta = useMemo(() => ({
    ...(buscaAplicada ? { busca: buscaAplicada } : {}),
    ...(statusSelecionado !== "TODOS" ? { status: statusSelecionado } : {}),
    ...(contratoSelecionado === "SEM_CONTRATO"
      ? { semContrato: true }
      : contratoSelecionado !== "TODOS"
        ? { contratoId: Number(contratoSelecionado) }
        : {}),
    ...(lotacaoSelecionada !== "TODAS" ? { lotacaoId: Number(lotacaoSelecionada) } : {}),
  }), [buscaAplicada, statusSelecionado, contratoSelecionado, lotacaoSelecionada]);

  const exibindoTodos = tamanhoPagina === TAMANHO_TODOS;

  const parametros = useMemo(() => ({
    ...filtrosConsulta,
    page: pagina,
    size: exibindoTodos ? RELATORIO_TERCEIRIZADOS_TAMANHO_MAXIMO : Number(tamanhoPagina),
  }), [filtrosConsulta, exibindoTodos, pagina, tamanhoPagina]);

  const carregarRelatorio = useCallback(async () => {
    controladorRef.current?.abort();
    const controlador = new AbortController();
    controladorRef.current = controlador;
    setCarregando(true);
    setErroConsulta("");
    setProgressoCargaTotal(null);

    try {
      // Mesmo no modo “Todos”, somente um bloco de até 5.000 registros fica
      // no navegador. O usuário navega pelos blocos sem materializar 268 mil
      // objetos/linhas no DOM.
      const resposta = await ServicoRelatorioTerceirizados.consolidado(
        parametros,
        { signal: controlador.signal },
      );

      if (controlador.signal.aborted) return;

      const paginaRecebida = resposta?.empregados || PAGINACAO_INICIAL;
      const totalPaginasResposta = Math.max(0, Number(paginaRecebida.totalPages || 0));

      if (totalPaginasResposta > 0 && pagina >= totalPaginasResposta) {
        setPagina(totalPaginasResposta - 1);
        return;
      }
      if (totalPaginasResposta === 0 && pagina > 0) {
        setPagina(0);
        return;
      }

      setDados(resposta);
    } catch (erro) {
      if (!controlador.signal.aborted && !erro?.canceled) {
        const mensagem = "Não foi possível carregar a visão consolidada. Verifique a conexão e tente novamente.";
        setErroConsulta(mensagem);
        notificar("error", mensagem);
      }
    } finally {
      if (!controlador.signal.aborted) {
        setCarregando(false);
        setProgressoCargaTotal(null);
      }
    }
  }, [parametros, pagina]);

  useEffect(() => {
    carregarRelatorio();
    return () => controladorRef.current?.abort();
  }, [carregarRelatorio, versaoConsulta]);

  const paginacao = dados?.empregados || PAGINACAO_INICIAL;
  const empregados = Array.isArray(paginacao.content) ? paginacao.content : [];
  const indicadores = dados?.indicadores || {};
  const statusDisponiveis = (Array.isArray(dados?.statusDisponiveis) ? dados.statusDisponiveis : [])
    .map((item) => item?.valor || item?.nome)
    .filter(Boolean);
  const contratos = Array.isArray(dados?.contratos) ? dados.contratos : [];
  const lotacoes = Array.isArray(dados?.lotacoes) ? dados.lotacoes : [];
  const distribuicaoStatus = Array.isArray(dados?.distribuicaoStatus) ? dados.distribuicaoStatus : [];
  const distribuicaoContratos = Array.isArray(dados?.distribuicaoContratos) ? dados.distribuicaoContratos : [];

  const alterarStatus = (valor) => { setPagina(0); setStatusSelecionado(valor); };
  const alterarContrato = (valor) => { setPagina(0); setContratoSelecionado(valor); };
  const alterarLotacao = (valor) => { setPagina(0); setLotacaoSelecionada(valor); };
  const alterarTamanho = (valor) => {
    setPagina(0);
    setTamanhoPagina(valor === TAMANHO_TODOS ? TAMANHO_TODOS : Number(valor));
  };

  const limparFiltros = () => {
    setTermoBusca("");
    setBuscaAplicada("");
    setStatusSelecionado("TODOS");
    setContratoSelecionado("TODOS");
    setLotacaoSelecionada("TODAS");
    setPagina(0);
  };

  const atualizar = () => setVersaoConsulta((valor) => valor + 1);

  const executarExportacaoOficial = async (formato) => {
    if (exportandoCompleto || carregando) return;
    setExportandoCompleto(formato);
    try {
      const tipo = formato === "pdf" ? "PDF" : "XLSX";
      await iniciarExportacao({
        tipo: "terceirizados",
        parametros: filtrosConsulta,
        formato: tipo,
        totalEstimado: paginacao.totalElements || 0,
      });
    } catch (erro) {
      console.error(erro);
      notificar("error", "Não foi possível iniciar a exportação completa.");
    } finally {
      setExportandoCompleto("");
    }
  };

  return (
    <div className={estilos.containerPagina}>
      <CabecalhoRelatorios
        carregando={carregando}
        exportandoCompleto={exportandoCompleto}
        quantidadeRegistros={paginacao.totalElements || 0}
        escopo={dados?.escopo}
        referencia={dados?.referencia}
        geradoEm={dados?.geradoEm}
        aoAtualizar={atualizar}
        aoExportarExcel={() => executarExportacaoOficial("excel")}
        aoExportarPdf={() => executarExportacaoOficial("pdf")}
      />
      <IndicadoresRelatorios indicadores={indicadores} />
      <CatalogoRelatorios aoNavegar={navegar} />
      <FiltrosRelatorios
        termoBusca={termoBusca}
        statusSelecionado={statusSelecionado}
        contratoSelecionado={contratoSelecionado}
        lotacaoSelecionada={lotacaoSelecionada}
        statusDisponiveis={statusDisponiveis}
        contratos={contratos}
        lotacoes={lotacoes}
        totalFiltrado={paginacao.totalElements || 0}
        carregando={carregando}
        aoAlterarBusca={setTermoBusca}
        aoAlterarStatus={alterarStatus}
        aoAlterarContrato={alterarContrato}
        aoAlterarLotacao={alterarLotacao}
        aoLimpar={limparFiltros}
      />
      <GraficosRelatorios distribuicaoStatus={distribuicaoStatus} distribuicaoContratos={distribuicaoContratos} />

      {erroConsulta && (
        <section className={estilos.avisoErroConsulta} role="alert">
          <div>
            <strong>Falha ao atualizar os dados</strong>
            <span>{erroConsulta}</span>
            {dados && <small>Os últimos dados carregados continuam visíveis abaixo.</small>}
          </div>
          <button type="button" onClick={atualizar} disabled={carregando}>Tentar novamente</button>
        </section>
      )}

      {(!erroConsulta || dados) && (
        <TabelaEmpregadosRelatorio
          empregados={empregados}
          paginacao={paginacao}
          carregando={carregando}
          tamanhoSelecionado={tamanhoPagina}
          exibindoTodos={exibindoTodos}
          progressoCargaTotal={progressoCargaTotal}
          aoMudarPagina={setPagina}
          aoMudarTamanho={alterarTamanho}
        />
      )}
    </div>
  );
};
