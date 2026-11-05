import React, { useMemo, useCallback } from "react";
import { Search } from "lucide-react";
import estilos from "./index.module.css";

import { useEmpregadosLista } from "./useEmpregadosLista.js";
import { ModaisAgrupados } from "./components/ModaisAgrupados.jsx";
import { CabecalhoEmpregados } from "./components/CabecalhoEmpregados/index.jsx";
import { EstatisticasEmpregados } from "./components/EstatisticasEmpregados/index.jsx";
import { FiltrosEmpregados } from "./components/FiltrosEmpregados/index.jsx";
import { GrupoEmpregados } from "./components/GrupoEmpregados/index.jsx";
import { BarraVisualizacaoEmpregados } from "./components/BarraVisualizacaoEmpregados/index.jsx";
import { PaginacaoEmpregados } from "./components/PaginacaoEmpregados/index.jsx";
import { FiltrosAvancadosEmpregados } from "./components/FiltrosAvancadosEmpregados/index.jsx";
import { useAutenticacao } from "../../../auth/context.jsx";
import { podeGerenciarEmpregados } from "../../../../core/security/roles.js";

const FeedbackCarregamento = ({ mensagem, sobreposto = false }) => (
  <div
    className={`${estilos.feedbackCarregamento} ${
      sobreposto ? estilos.feedbackCarregamentoSobreposto : ""
    }`}
    role="status"
    aria-live="polite"
    aria-busy="true"
  >
    <div className={estilos.girador} aria-hidden="true" />
    <div>
      <p className={estilos.tituloCarregamento}>Aguarde, buscando colaboradores</p>
      <p className={estilos.textoCarregamento}>
        {mensagem || "Consultando os dados no servidor..."}
      </p>
    </div>
  </div>
);

export const ListaEmpregados = () => {
  const { user } = useAutenticacao();
  const podeEditar = podeGerenciarEmpregados(user);
  const { estado, processamento, acoes, modais, ModalConfirmacao } =
    useEmpregadosLista();

  const modaisAcoes = useMemo(
    () => ({
      abrirEdicao: podeEditar ? (id) => modais.definirEdicao({ aberto: true, id }) : null,
      abrirHistoricoAuditoria: (colab) =>
        modais.definirAuditoria({ aberto: true, colaborador: colab }),
      abrirHistoricoContrato: (colab) =>
        modais.definirHistorico({ aberto: true, colaborador: colab }),
    }),
    [podeEditar, modais.definirEdicao, modais.definirAuditoria, modais.definirHistorico],
  );

  const abrirModalNovo = useCallback(() => {
    modais.definirEdicao({ aberto: true, id: null });
  }, [modais.definirEdicao]);

  const grupoConsolidado = useMemo(
    () => ({
      id: "resultados-consolidados",
      titulo: "Resultados",
      descricao: "",
      isWarning: false,
      lista: estado.colaboradores,
      stats: {
        total: estado.colaboradores.length,
        ativos: processamento.estatisticasRenderizadas.ativos || 0,
      },
    }),
    [estado.colaboradores, processamento.estatisticasRenderizadas.ativos],
  );

  const carregamentoSemDados =
    estado.carregando && estado.colaboradores.length === 0;

  return (
    <div className={estilos.containerPagina}>
      <ModalConfirmacao />

      <ModaisAgrupados
        modais={modais}
        paginaAtual={estado.paginaAtual}
        carregarColaboradores={acoes.carregarColaboradores}
      />

      <CabecalhoEmpregados
        totalElementos={estado.totalElementos}
        quantidadeContratos={processamento.quantidadeContratos}
        quantidadeAtivos={processamento.estatisticasRenderizadas.ativos}
        quantidadeFavoritos={processamento.quantidadeFavoritos}
        abrirModalNovo={abrirModalNovo}
        podeEditar={podeEditar}
      />

      {carregamentoSemDados ? (
        <div className={estilos.painelCarregamentoInicial}>
          <FeedbackCarregamento mensagem={estado.mensagemCarregamento} />
        </div>
      ) : (
        <div className={estilos.gradeOperacional}>
          <FiltrosEmpregados
            filtrosDisponiveis={processamento.filtrosDisponiveis}
            filtrosGrupoAtivos={estado.filtrosGrupoAtivos}
            alternarFiltro={acoes.aoAlternarFiltro}
            gruposBase={processamento.gruposBase}
            contratosFavoritos={estado.contratosFavoritos}
            aoAlternarFavoritoContrato={acoes.aoAlternarFavoritoContrato}
          />

          <section className={estilos.colunaPrincipal}>
            <BarraVisualizacaoEmpregados
              modoVisualizacao={estado.modoVisualizacao}
              aoMudarModo={acoes.aoMudarModoVisualizacao}
              quantidadeGrupos={processamento.gruposParaRenderizar.length}
              quantidadePagina={processamento.quantidadePagina}
              termoBusca={estado.termoBusca}
              aoMudarBusca={acoes.definirTermoBusca}
              aoAcionarBusca={acoes.definirBuscaDebounced}
              quantidadeContratosSelecionados={estado.filtrosGrupoAtivos.length}
              lotacaoSelecionadaNome={estado.lotacaoSelecionadaNome}
              turnoSelecionadoNome={estado.turnoSelecionadoNome}
              aoAbrirFiltros={acoes.aoAlternarFiltrosAvancados}
              aoLimparFiltros={acoes.aoLimparBuscaEFiltros}
              filtrosAvancadosAbertos={estado.filtrosAvancadosAbertos}
              aoAtualizar={() =>
                acoes.carregarColaboradores(
                  estado.paginaAtual,
                  estado.tamanhoPagina,
                  estado.termoBusca.trim(),
                )
              }
              carregando={estado.carregando}
            />

            <EstatisticasEmpregados
              estatisticas={processamento.estatisticasRenderizadas}
              totalGeral={estado.totalElementos}
              quantidadeFavoritos={processamento.quantidadeFavoritos}
            />

            <PaginacaoEmpregados
              paginaAtual={estado.paginaAtual}
              totalPaginas={estado.totalPaginas}
              tamanhoPagina={estado.tamanhoPagina}
              totalElementos={estado.totalElementos}
              quantidadeExibida={processamento.quantidadePagina}
              carregarPagina={acoes.carregarColaboradores}
              carregando={estado.carregando}
              duracaoUltimaBuscaMs={estado.duracaoUltimaBuscaMs}
            />

            <div className={estilos.areaResultados}>
              {estado.carregando && (
                <FeedbackCarregamento
                  mensagem={estado.mensagemCarregamento}
                  sobreposto
                />
              )}

              {estado.colaboradores.length === 0 ? (
                <div className={estilos.estadoVazio}>
                  <div className={estilos.envoltorioIconeVazio}>
                    <Search size={28} />
                  </div>
                  <p className={estilos.textoVazio}>
                    {estado.tamanhoPagina === 0
                      ? "Utilize a busca ou altere a quantidade para exibir os colaboradores."
                      : "Nenhum colaborador encontrado nos filtros aplicados."}
                  </p>
                  <button
                    onClick={acoes.aoLimparBuscaEFiltros}
                    className={estilos.linkVazio}
                  >
                    Limpar busca e exibir tudo
                  </button>
                </div>
              ) : (
                <GrupoEmpregados
                  grupo={grupoConsolidado}
                  estaExpandido
                  alternarGrupo={() => {}}
                  cpfsRevelados={estado.cpfsRevelados}
                  acoes={acoes}
                  modaisAcoes={modaisAcoes}
                  eAdministrador={processamento.eAdministrador}
                  podeEditar={podeEditar}
                  modoVisualizacao={estado.modoVisualizacao}
                  ocultarCabecalho
                />
              )}
            </div>
          </section>

          <FiltrosAvancadosEmpregados
            aberto={estado.filtrosAvancadosAbertos}
            lotacaoFiltro={estado.lotacaoFiltro}
            turnoFiltro={estado.turnoFiltro}
            tamanhoPagina={estado.tamanhoPagina}
            lotacoes={processamento.lotacoesBase}
            turnos={processamento.turnosDisponiveis}
            aoAplicar={acoes.aoAplicarFiltrosAvancados}
            aoLimpar={acoes.aoLimparBuscaEFiltros}
            aoFechar={acoes.aoFecharFiltrosAvancados}
          />
        </div>
      )}
    </div>
  );
};
