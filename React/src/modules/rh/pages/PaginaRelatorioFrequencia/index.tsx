import { observarFalha } from '../../../../core/http/falhaObservada';
import React, { useEffect, useLayoutEffect, useMemo, useState } from "react";
import { ServicoFrequencia } from "../../../../services/rh/ServicoFrequencia";
import { notificar } from "../../../../core/events";
import { useExportacaoRelatorio } from "../../../../core/exportacao/context";
import estilos from "./index.module.css";
import { gerarPlanilhaExcel, gerarDocumentoPDF } from "./exportacao";
import { criarDias, formatarLinhaRelatorio } from "../../../../services/rh/ServicoDominioRelatorioFrequencia.ts";
import { CabecalhoRelatorio } from "./components/CabecalhoRelatorio";
import { FiltrosRelatorio } from "./components/FiltrosRelatorio";
import { TabelaRelatorio } from "./components/TabelaRelatorio";
import { EstadoVazioRelatorio } from "./components/EstadoVazioRelatorio";
import { LegendaRelatorio } from "./components/LegendaRelatorio";
import { ResumoRegraNoturna } from "./components/ResumoRegraNoturna";
import { normalizarAvisoRevisaoRelatorio } from "../../../../services/rh/revisaoRelatorio.ts";
import { useConsultaVigente } from "../../../../core/hooks/useConsultaVigente.ts";
import { obterGeracaoSessao } from "../../../../core/security/geracaoSessao.ts";
import { obterDataNoFusoNegocio, obterPeriodoMensalNoFusoNegocio } from "../../../../core/formatters/dataHoraNegocio.ts";

export const PaginaRelatorioFrequencia = () => {
  const { iniciarExportacao } = useExportacaoRelatorio();
  const primeiroDiaMes = obterPeriodoMensalNoFusoNegocio()?.start || "";
  const hoje = obterDataNoFusoNegocio() || "";

  const [dataInicio, setDataInicio] = useState(primeiroDiaMes);
  const [dataFim, setDataFim] = useState(hoje);
  const [contratos, setContratos] = useState<(import("../../../../contracts/opcoes-apresentacao").ContractOptionView[])>([]);
  const [lotacoesGerais, setLotacoesGerais] = useState<(import("../../../../contracts/opcoes-apresentacao").LocationOptionView[])>([]);
  const [jornadasGlobais, setJornadasGlobais] = useState<(import("../../../../contracts/opcoes-apresentacao").ShiftSummaryView[])>([]);
  const [contratosSelecionados, setContratosSelecionados] = useState<string[]>([]);
  const [lotacaoSelecionada, setLotacaoSelecionada] = useState("");
  const [turnoSelecionado, setTurnoSelecionado] = useState("");
  const [dadosRelatorio, setDadosRelatorio] = useState<({ id: string | null; empregadoId: number | null; nome: string | null; cargo: string; contrato: string; lotacao: string; turno: string; diario: { [k: string]: { status: string; sigla: string; data: import("../../../../contracts/shared").CivilDate | null; diaTrabalho: boolean; minutosPrevistos: number; lancado: boolean; entrada1: string | null; saida1: string | null; entrada2: string | null; saida2: string | null; minutosFisicos: number; minutosNoturnosReais: number; minutosNoturnosReduzidos: number; minutosAdicionais: number; minutosPagaveis: number; calculoVersao: string | null; ocorrenciaSobreposta: boolean; }; }; totais: { presencas: number; parciais: number; faltas: number; atestados: number; descansos: number; dsrPerdido: number; rt: number; semLancamento: number; horasFisicas: string; totalFisicoDecimal: number; horasPagaveis: string; totalPagavelDecimal: number; horasNoturnas: string; totalNoturnoDecimal: number; horasRt: string; totalRtDecimal: number; minutosFisicos: number; minutosPagaveis: number; minutosAdicionais: number; }; }[])>([]);
  const [diasDoMes, setDiasDoMes] = useState<(string[])>([]);
  const [carregando, setCarregando] = useState(false);
  const [revisao, setRevisao] = useState(() => normalizarAvisoRevisaoRelatorio(null));
  const [paginacao, setPaginacao] = useState({
    pagina: 0,
    tamanho: 5,
    totalPaginas: 0,
    totalColaboradores: 0,
    primeira: true,
    ultima: true,
  });
  const geracaoSessao = obterGeracaoSessao();
  const contextoRelatorio = JSON.stringify([dataInicio, dataFim, contratosSelecionados, lotacaoSelecionada, turnoSelecionado, geracaoSessao]);
  const consultasPagina = useConsultaVigente(contextoRelatorio);
  const consultasFiltros = useConsultaVigente(geracaoSessao);

  useLayoutEffect(() => {
    setDadosRelatorio([]);
    setDiasDoMes([]);
    setRevisao(normalizarAvisoRevisaoRelatorio(null));
    setCarregando(false);
    setPaginacao((atual) => ({ ...atual, pagina: 0, totalPaginas: 0, totalColaboradores: 0, primeira: true, ultima: true }));
  }, [contextoRelatorio]);

  useEffect(() => {
    const consulta = consultasFiltros.iniciar();
    setContratos([]);
    setLotacoesGerais([]);
    setJornadasGlobais([]);
    setContratosSelecionados([]);
    setLotacaoSelecionada("");
    setTurnoSelecionado("");
    const carregarDadosIniciais = async () => {
      try {
        const filtros = await ServicoFrequencia.carregarFiltrosRelatorio({ signal: consulta.signal });
        if (!consulta.atual()) return;
        setContratos(Array.isArray(filtros?.contratos) ? filtros.contratos : []);
        setLotacoesGerais(Array.isArray(filtros?.lotacoes) ? filtros.lotacoes : []);
        setJornadasGlobais(Array.isArray(filtros?.turnos) ? filtros.turnos : []);
      } catch (erro) {
        if (!consulta.atual()) return;
        console.error("Falha ao carregar filtros do relatório", erro);
        notificar("error", "Não foi possível carregar os filtros. Nenhum relatório foi gerado.");
      }
    };
    carregarDadosIniciais();
    return consultasFiltros.invalidar;
  }, [consultasFiltros, geracaoSessao]);

  const lotacoesDisponiveis = useMemo(() => 
    [...lotacoesGerais].sort((a, b) => String(a.nome || "").localeCompare(String(b.nome || ""))), 
    [lotacoesGerais]
  );

  const turnosDisponiveis = useMemo(() => {
    if (!lotacaoSelecionada) {
      return [...jornadasGlobais].sort((a, b) => String(a.descricao || "").localeCompare(String(b.descricao || "")));
    }
    const lotacao = lotacoesDisponiveis.find((item) => String(item.id) === String(lotacaoSelecionada));
    return lotacao?.turnos || [];
  }, [jornadasGlobais, lotacaoSelecionada, lotacoesDisponiveis]);

  useEffect(() => {
    setLotacaoSelecionada("");
    setTurnoSelecionado("");
  }, [contratosSelecionados]);

  useEffect(() => {
    setTurnoSelecionado("");
  }, [lotacaoSelecionada]);

  const parametrosRelatorio = () => ({
    inicio: dataInicio,
    fim: dataFim,
    ...(contratosSelecionados.length ? { contratoIds: contratosSelecionados } : {}),
    ...(lotacaoSelecionada ? { lotacaoId: lotacaoSelecionada } : {}),
    ...(turnoSelecionado ? { jornadaId: turnoSelecionado } : {}),
  });

  const carregarPagina = async (pagina = 0, exibirMensagem = false, tamanhoSolicitado = paginacao.tamanho) => {
    const consulta = consultasPagina.iniciar();
    setCarregando(true);
    try {
      const resposta = await ServicoFrequencia.gerarRelatorioConsolidado({
        ...parametrosRelatorio(),
        page: pagina,
        size: tamanhoSolicitado,
      }, { signal: consulta.signal });
      if (!consulta.atual()) return;

      if (resposta?.completo !== true || !Array.isArray(resposta?.registros)) {
        throw new Error("O servidor não confirmou a conclusão da página solicitada.");
      }

      const linhas = resposta.registros.map(formatarLinhaRelatorio);
      setDiasDoMes(criarDias(dataInicio, dataFim));
      setDadosRelatorio(linhas);
      setRevisao(normalizarAvisoRevisaoRelatorio(resposta.revisao));
      setPaginacao((atual) => ({
        ...atual,
        pagina: Number(resposta.pagina || 0),
        tamanho: Number(resposta.tamanho || atual.tamanho),
        totalPaginas: Number(resposta.totalPaginas || 0),
        totalColaboradores: Number(resposta.totalColaboradores || 0),
        primeira: Boolean(resposta.primeira),
        ultima: Boolean(resposta.ultima),
      }));

      if (!linhas.length && pagina === 0) {
        notificar("warning", "Nenhuma alocação histórica foi encontrada para os filtros.");
      } else if (exibirMensagem) {
        notificar("success", `Relatório oficial pronto. ${Number(resposta.totalColaboradores || 0).toLocaleString("pt-BR")} colaborador(es) disponíveis.`);
      }
    } catch (erroOriginal) {
      const erro = observarFalha(erroOriginal);

      if (!consulta.atual()) return;
      console.error("Falha na apuração oficial", erroOriginal);
      if (pagina === 0) {
        setDadosRelatorio([]);
        setDiasDoMes([]);
        setRevisao(normalizarAvisoRevisaoRelatorio(null));
        setPaginacao((atual) => ({ ...atual, pagina: 0, totalPaginas: 0, totalColaboradores: 0, primeira: true, ultima: true }));
      }
      notificar("error", erro?.message || "A apuração falhou. O relatório não foi concluído.");
    } finally {
      if (consulta.atual()) setCarregando(false);
    }
  };

  const gerarRelatorio = async () => {
    const inicio = new Date(`${dataInicio}T12:00:00Z`);
    const fim = new Date(`${dataFim}T12:00:00Z`);
    if (inicio > fim) {
      notificar("warning", "A data inicial não pode ser posterior à data final.");
      return;
    }
    if (Math.ceil((fim.getTime() - inicio.getTime()) / 86400000) + 1 > 366) {
      notificar("warning", "O período não pode exceder 366 dias.");
      return;
    }
    setDadosRelatorio([]);
    setDiasDoMes([]);
    setRevisao(normalizarAvisoRevisaoRelatorio(null));
    setPaginacao((atual) => ({ ...atual, pagina: 0, totalPaginas: 0, totalColaboradores: 0, primeira: true, ultima: true }));
    await carregarPagina(0, true);
  };

  const propriedadesExportacao = {
    dados: dadosRelatorio,
    dataInicio,
    dataFim,
    contratos,
    contratosSelecionados,
    lotacoesDisponiveis,
    lotacaoSelecionada,
    turnosDisponiveis,
    turnoSelecionado,
    diasDoMes,
    totalColaboradores: paginacao.totalColaboradores,
  };

  return (
    <main className={estilos.containerGeral}>
      <CabecalhoRelatorio
        existemDados={paginacao.totalColaboradores > 0}
        aoExportarExcel={() => gerarPlanilhaExcel(propriedadesExportacao, iniciarExportacao)}
        aoExportarPdf={() => gerarDocumentoPDF(propriedadesExportacao, iniciarExportacao)}
      />
      
      <FiltrosRelatorio
        dataInicio={dataInicio}
        setDataInicio={setDataInicio}
        dataFim={dataFim}
        setDataFim={setDataFim}
        contratos={contratos}
        contratosSelecionados={contratosSelecionados}
        setContratosSelecionados={setContratosSelecionados}
        lotacoesDisponiveis={lotacoesDisponiveis}
        lotacaoSelecionada={lotacaoSelecionada}
        setLotacaoSelecionada={setLotacaoSelecionada}
        turnosDisponiveis={turnosDisponiveis}
        turnoSelecionado={turnoSelecionado}
        setTurnoSelecionado={setTurnoSelecionado}
        aoGerarRelatorio={gerarRelatorio}
        carregando={carregando}
      />

      <ResumoRegraNoturna />

      {revisao.sujeitoARevisao && (
        <div className={estilos.avisoRevisao} role="alert">
          <strong>SUJEITO A REVISÃO</strong>
          <span>{revisao.mensagem || `${revisao.quantidadePendencias} pendência(s) de retificação no recorte.`}</span>
        </div>
      )}

      {dadosRelatorio.length > 0 ? (
        <div className={estilos.secaoResultados}>
          <TabelaRelatorio
            dados={dadosRelatorio}
            diasDoMes={diasDoMes}
            totalColaboradores={paginacao.totalColaboradores}
            pagina={paginacao.pagina}
            totalPaginas={paginacao.totalPaginas}
            primeira={paginacao.primeira}
            ultima={paginacao.ultima}
            carregando={carregando}
            aoMudarPagina={(pagina) => carregarPagina(pagina, false)}
            tamanho={paginacao.tamanho}
            aoMudarTamanho={(novoTamanho) => carregarPagina(0, false, novoTamanho)}
          />
          <LegendaRelatorio />
        </div>
      ) : (
        <EstadoVazioRelatorio carregando={carregando} />
      )}
    </main>
  );
};
