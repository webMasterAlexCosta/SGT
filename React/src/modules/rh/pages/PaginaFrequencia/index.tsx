import React, { useState, Suspense, lazy } from "react";
import { useLogicaFrequencia } from "./useLogicaFrequencia";

import { CabecalhoFrequencia } from "./components/CabecalhoFrequencia.tsx";
import { FiltrosFrequencia } from "./components/FiltrosFrequencia.tsx";
import { TabelaFrequencia } from "./components/TabelaFrequencia.tsx";
import { PainelFechamentoFrequencia } from "./components/PainelFechamentoFrequencia.tsx";
import { LancamentoPorVinculo } from "./components/LancamentoPorVinculo.tsx";

import estilos from "./index.module.css";

const ComponenteTourGuiado = lazy(() =>
  import("./components/TourGuiado.tsx").then((m) => ({
    default: m.TourGuiado,
  })),
);
const ComponenteModalParcial = lazy(() =>
  import("./components/ModalParcial.tsx").then((m) => ({
    default: m.ModalParcial,
  })),
);

const PASSOS_DO_TOUR_INFO = [
  {
    id: "freq-header",
    title: "Data de Lançamento",
    text: "Aqui você acompanha a data da frequência. Você pode retroceder ou avançar os dias. Fique atento ao limite de bloqueio de edição.",
  },
  {
    id: "freq-filters",
    title: "Filtros da Escala",
    text: "Selecione obrigatoriamente a Lotação e o Turno (Jornada) correspondente para carregar a lista completa de colaboradores escalados.",
  },
  {
    id: "freq-table",
    title: "Quadro de Colaboradores",
    text: "Aqui aparecerá a lista. Você terá botões rápidos com cores intuitivas para marcar PRESENÇA (verde), FALTA (vermelho) ou PARCIAL (amarelo) nos casos de atraso.",
  },
  {
    id: "freq-save",
    title: "Gravação Integrada",
    text: 'Importante! Após preencher todos os pontos, não esqueça de clicar neste botão para "Salvar Tudo" e oficializar a frequência no sistema.',
  },
];

export const PaginaFrequencia = () => {
  const [tourAberto, setTourAberto] = useState(false);
  const { estado, filtros, acoes, auth, utilitarios, ModalConfirmacao } =
    useLogicaFrequencia();

  return (
    <div className={estilos.containerPagina}>
      <ModalConfirmacao />

      <Suspense fallback={null}>
        <ComponenteTourGuiado
          passos={PASSOS_DO_TOUR_INFO}
          aberto={tourAberto}
          aoFechar={() => setTourAberto(false)}
        />
        <ComponenteModalParcial
          dadosModalParcial={estado.estadoDoModalParcial}
          aoAlterarModalParcial={acoes.setEstadoDoModalParcial}
          aoConfirmarParcial={acoes.validarFechamentoEConfirmacaoDoModalParcial}
        />
      </Suspense>

      <CabecalhoFrequencia
        eAdministradorGlobal={auth.isAdmin}
        eGestorGestaoLocal={auth.eGestorGestaoLocal}
        eGestorOperacional={auth.eGestorOperacional}
        usuarioLogado={auth.user}
        dataReferencia={estado.dataReferenciaAtiva}
        aoAlterarDataReferencia={acoes.setDataReferenciaAtiva}
        statusDaData={estado.statusDaDataValidada}
        estaSalvando={estado.indicadorSalvamento}
        progressoEnvio={estado.progressoEnvio}
        edicaoBloqueada={!auth.podeRealizarFrequencias}
        estaCarregando={estado.indicadorCarregamento}
        listaDeFrequencia={estado.listaDaFrequencia}
        aoMudarDataPorBotoes={acoes.aoAcionarMudarDataPeloCabecalho}
        aoIniciarSalvamentoGlobal={
          acoes.invocarGatilhoDeDespachoAoServidorGlobal
        }
        aoAbrirTourGuiado={() => setTourAberto(true)}
      />

      <FiltrosFrequencia
        eGestorOperacional={auth.eGestorOperacional}
        eGestorGestaoLocal={auth.eGestorGestaoLocal}
        escoposDoUsuario={filtros.escoposLiberadosDoUsuario}
        listaDeContratos={filtros.contratosBaixados}
        listaDeLotacoesDisponiveis={filtros.listaLotacoesVisiveisAoUsuario}
        listaDeTurnosDisponiveis={filtros.listaTurnosVisiveisAoUsuario}
        contratosSelecionados={filtros.contratosSelecionadosPeloUsuario}
        aoMudarContratos={acoes.setContratosSelecionadosPeloUsuario}
        lotacaoSelecionada={filtros.lotacaoSelecionadaPeloUsuario}
        aoMudarLotacao={acoes.setLotacaoSelecionadaPeloUsuario}
        turnoSelecionado={filtros.turnoSelecionadoPeloUsuario}
        aoMudarTurno={acoes.setTurnoSelecionadoPeloUsuario}
        termoDeBuscaGlobal={filtros.termoDeBuscaDigitado}
        aoMudarTermoDeBusca={acoes.setTermoDeBuscaDigitado}
        aoLimparTodosOsFiltros={acoes.aoRequisitarLimpezaDosFiltros}
        escopoBloqueado={estado.indicadorSalvamento}
        justificativaRetificacao={filtros.justificativaRetificacao}
        aoMudarJustificativaRetificacao={acoes.setJustificativaRetificacao}
      />

      <LancamentoPorVinculo
        dataReferencia={estado.dataReferenciaAtiva}
        lotacaoId={filtros.lotacaoSelecionadaPeloUsuario}
        jornadaId={filtros.turnoSelecionadoPeloUsuario}
        contratoIds={filtros.contratosSelecionadosPeloUsuario}
        contratos={filtros.contratosBaixados}
        usuario={auth.user}
        gestorLocal={auth.eGestorGestaoLocal}
        podeLancar={auth.podeRealizarFrequencias}
        bloqueado={estado.indicadorSalvamento}
        dataValida={estado.statusDaDataValidada.valid}
        aoSalvar={acoes.recarregarFrequencia}
      />

      <PainelFechamentoFrequencia
        fechamento={estado.fechamentoFrequencia}
        ativo={estado.fechamentoAtivo}
        carregando={estado.carregandoFechamento}
        erro={estado.erroFechamento}
        podeGerenciar={auth.podeRealizarFrequencias}
        dataValida={Boolean(estado.dataReferenciaAtiva && estado.statusDaDataValidada.valid)}
        filtrosProntos={Boolean(filtros.lotacaoSelecionadaPeloUsuario && filtros.turnoSelecionadoPeloUsuario)}
        possuiRascunhos={Boolean(estado.listaDaFrequencia.some((linha) => linha.alterado))}
        aoCriar={acoes.solicitarCriacaoDoFechamento}
        aoCancelar={acoes.solicitarCancelamentoDoFechamento}
        aoReprocessar={acoes.solicitarReprocessamentoDoFechamento}
        aoAtualizar={acoes.recuperarFechamentoAtual}
      />

      <TabelaFrequencia
        aoRevisaoConcluida={acoes.recarregarFrequencia}
        estaCarregando={estado.indicadorCarregamento}
        erroCarregamento={estado.erroCargaFrequencia}
        listaDeFrequencia={estado.listaDaFrequenciaFiltradaNaMemoria}
        statusDaData={estado.statusDaDataValidada}
        eGestorOperacional={auth.eGestorOperacional}
        edicaoBloqueada={estado.indicadorSalvamento || !auth.podeRealizarFrequencias}
        aoSelecionarStatusDaTabela={
          acoes.invocarMudancaDeStatusDoUsuarioNaTabela
        }
        aoAtualizarObservacaoDaTabela={
          acoes.invocarAlteracaoDeAnotacaoDeObservacao
        }
        aoAbrirModalParcial={acoes.setEstadoDoModalParcial}
        funcaoObterJanelaDeEdicao={utilitarios.obterRelatorioDaJanelaDeEdicao}
        paginacaoGrade={estado.paginacaoGrade}
        aoMudarPagina={acoes.setPaginaGrade}
        pendenciasSemIdentidadeCanonica={estado.pendenciasSemIdentidadeCanonica}
      />
    </div>
  );
};
