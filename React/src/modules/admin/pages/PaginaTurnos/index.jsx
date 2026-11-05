import React, { useEffect, useMemo, useRef, useState } from "react";
import { ServicoJornada } from "../../../../services/admin/ServicoJornada.js";
import { notificar } from "../../../../core/events";
import { useConfirmacaoSenha } from "../../../../core/security/useConfirmacaoSenha.jsx";
import { CONFIG } from "../../../../core/config";
import { useAutenticacao } from "../../../auth/context.jsx";
import { possuiAcessoGlobal } from "../../../../core/security/roles";
import { ServicoOrganizacional } from "../../../../services/admin/ServicoOrganizacional";
import {
  contarPorEstado,
  ESTADO_PADRAO_ADMIN,
  filtrarPorEstado,
  normalizarUf,
  TODOS_OS_ESTADOS,
} from "../../constants/estados.js";

import { CabecalhoHero } from "./components/CabecalhoHero";
import { FiltrosAvancados } from "./components/FiltrosAvancados";
import { ListaDeTurnos } from "./components/ListaDeTurnos";
import { ModalDeTurno } from "./components/ModalDeTurno";

import {
  ORDEM_FORMULARIO,
  obterInformacoesDoDia,
  validarFormatoDeHora,
  formatarEntradaDeHora,
  formatarParaHorasEMinutos,
  analisarDiaDeTrabalho,
  analisarEscalaCompleta,
} from "../../../../services/admin/ServicoDominioJornada.js";

import estilos from "./index.module.css";

export const PaginaTurnos = ({ podeEscrever = false }) => {
  const { user: usuario } = useAutenticacao();
  const acessoGlobal = possuiAcessoGlobal(usuario);
  const controladorDeRequisicoes = useRef(null);
  const [listaTotalDeTurnos, setListaTotalDeTurnos] = useState([]);
  const [listaDeGestoesLocais, setListaDeGestoesLocais] = useState([]);
  const [estaCarregandoDados, setEstaCarregandoDados] = useState(false);
  const [modalEstaAberto, setModalEstaAberto] = useState(false);
  const [idDoTurnoEmEdicao, setIdDoTurnoEmEdicao] = useState(null);

  const [textoDaBusca, setTextoDaBusca] = useState("");
  const [filtroDeEstado, setFiltroDeEstado] = useState(() =>
    acessoGlobal ? ESTADO_PADRAO_ADMIN : TODOS_OS_ESTADOS,
  );
  const [filtroDeCategoriaAtivo, setFiltroDeCategoriaAtivo] = useState("TODAS");
  const [regraDeOrdenacaoAtual, setRegraDeOrdenacaoAtual] = useState("NOME_AZ");

  const { confirmarComSenha, ModalConfirmacao } = useConfirmacaoSenha();
  const [dadosDoFormularioDeCriacao, setDadosDoFormularioDeCriacao] = useState({
    descricao: "",
    gestaoLocalId: usuario?.gestaoLocalId ? String(usuario.gestaoLocalId) : "",
    ativo: true,
    dias: [],
  });
  const [regrasAdicionais, setRegrasAdicionais] = useState([]);
  const [carregandoRegras, setCarregandoRegras] = useState(false);
  const [regraEmEdicao, setRegraEmEdicao] = useState(null);
  const [formularioRegra, setFormularioRegra] = useState({
    tipo: "", inicio: "", fim: "", percentual: "", ativo: true, ordem: 0,
  });

  const fabricarDiasComConfiguracaoPadrao = () =>
    ORDEM_FORMULARIO.map((siglaDiaSemana) => ({
      diaSemana: siglaDiaSemana,
      diaTrabalho: siglaDiaSemana !== "SATURDAY" && siglaDiaSemana !== "SUNDAY",
      entrada1: "",
      saida1: "",
      entrada2: "",
      saida2: "",
    }));

  const acionarCarregamentoDosTurnosDoServidor = async (
    opcoesDaRequisicao = {},
  ) => {
    setEstaCarregandoDados(true);
    try {
      const [respostaDoServidor, respostaGestoesLocais = []] = await Promise.all([
        ServicoJornada.listar(opcoesDaRequisicao),
        acessoGlobal ? ServicoOrganizacional.listarGestoesLocais({}, opcoesDaRequisicao) : Promise.resolve([]),
      ]);
      setListaTotalDeTurnos(respostaDoServidor);
      setListaDeGestoesLocais(respostaGestoesLocais);
    } catch (erroGerado) {
      if (!erroGerado?.canceled)
        notificar("error", "Falha ao carregar as escalas armazenadas.");
    } finally {
      setEstaCarregandoDados(false);
    }
  };

  useEffect(() => {
    const canceladorLocal = new AbortController();
    controladorDeRequisicoes.current = canceladorLocal;
    acionarCarregamentoDosTurnosDoServidor({ signal: canceladorLocal.signal });
    return () => canceladorLocal.abort();
  }, []);

  useEffect(() => {
    setFiltroDeEstado((estadoAtual) => {
      if (!acessoGlobal) return TODOS_OS_ESTADOS;
      return estadoAtual === TODOS_OS_ESTADOS
        ? ESTADO_PADRAO_ADMIN
        : estadoAtual;
    });
  }, [acessoGlobal]);

  const lidarComAberturaDoModalParaNovoTurno = () => {
    if (!podeEscrever) return;
    setIdDoTurnoEmEdicao(null);
    setRegrasAdicionais([]);
    setRegraEmEdicao(null);
    setDadosDoFormularioDeCriacao({
      descricao: "",
      gestaoLocalId: usuario?.gestaoLocalId ? String(usuario.gestaoLocalId) : "",
      ativo: true,
      dias: fabricarDiasComConfiguracaoPadrao(),
    });
    setModalEstaAberto(true);
  };

  const lidarComAberturaDoModalParaEdicaoDeTurno = async (idAlvoParaEdicao) => {
    if (!podeEscrever) return;
    const turnoEncontrado = listaTotalDeTurnos.find(
      (turnoItem) => String(turnoItem.id) === String(idAlvoParaEdicao),
    );
    if (!turnoEncontrado) return;

    const dicionarioDosDiasSalvos = new Map(
      (turnoEncontrado.dias || []).map((diaMap) => [diaMap.diaSemana, diaMap]),
    );

    const baseDeDiasRemontada = ORDEM_FORMULARIO.map((siglaDiaSemana) => {
      const registroExistente = dicionarioDosDiasSalvos.get(siglaDiaSemana);
      if (registroExistente) {
        return {
          diaSemana: siglaDiaSemana,
          diaTrabalho: registroExistente.diaTrabalho !== false,
          entrada1: registroExistente.entrada1 || "",
          saida1: registroExistente.saida1 || "",
          entrada2: registroExistente.entrada2 || "",
          saida2: registroExistente.saida2 || "",
        };
      }
      return {
        diaSemana: siglaDiaSemana,
        diaTrabalho: false,
        entrada1: "",
        saida1: "",
        entrada2: "",
        saida2: "",
      };
    });

    setIdDoTurnoEmEdicao(idAlvoParaEdicao);
    setDadosDoFormularioDeCriacao({
      descricao: turnoEncontrado.descricao || "",
      gestaoLocalId: String(turnoEncontrado.gestaoLocalId || usuario?.gestaoLocalId || ""),
      ativo: turnoEncontrado.ativo !== false,
      dias: baseDeDiasRemontada,
    });
    setModalEstaAberto(true);
    setRegraEmEdicao(null);
    setCarregandoRegras(true);
    try {
      const regras = await ServicoJornada.buscarRegras(idAlvoParaEdicao);
      setRegrasAdicionais(Array.isArray(regras) ? regras : []);
    } catch (erro) {
      if (!erro?.canceled) notificar("error", "Não foi possível carregar as regras adicionais deste turno.");
      setRegrasAdicionais([]);
    } finally {
      setCarregandoRegras(false);
    }
  };

  const fecharOModalAtivo = () => {
    setModalEstaAberto(false);
    setIdDoTurnoEmEdicao(null);
    setRegrasAdicionais([]);
    setRegraEmEdicao(null);
  };

  const lidarComAMudancaDoNomeDoTurnoNoFormulario = (eventoDeTexto) =>
    setDadosDoFormularioDeCriacao((estadoPrevio) => ({
      ...estadoPrevio,
      descricao: eventoDeTexto.target.value,
    }));

  const lidarComMudancaAtivo = (ativo) =>
    setDadosDoFormularioDeCriacao((estadoPrevio) => ({ ...estadoPrevio, ativo }));

  const lidarComAMudancaNosCamposDeUmDiaEspecifico = (
    indiceDoDiaNoArray,
    campoAlvoDaMudanca,
    valorOriginalDoCampo,
  ) => {
    const valorProntoParaSalvar =
      campoAlvoDaMudanca.includes("entrada") ||
      campoAlvoDaMudanca.includes("saida")
        ? formatarEntradaDeHora(valorOriginalDoCampo)
        : valorOriginalDoCampo;
    setDadosDoFormularioDeCriacao((estadoPrevio) => {
      const cloneDosDias = [...estadoPrevio.dias];
      cloneDosDias[indiceDoDiaNoArray] = {
        ...cloneDosDias[indiceDoDiaNoArray],
        [campoAlvoDaMudanca]: valorProntoParaSalvar,
      };

      if (
        campoAlvoDaMudanca === "diaTrabalho" &&
        valorProntoParaSalvar === false
      ) {
        cloneDosDias[indiceDoDiaNoArray].entrada1 = "";
        cloneDosDias[indiceDoDiaNoArray].saida1 = "";
        cloneDosDias[indiceDoDiaNoArray].entrada2 = "";
        cloneDosDias[indiceDoDiaNoArray].saida2 = "";
      }
      return { ...estadoPrevio, dias: cloneDosDias };
    });
  };

  const acionarAreplicacaoDosHorariosPreenchidos = (
    indiceDoDiaSementeParaReplicar,
  ) => {
    const diaSemente =
      dadosDoFormularioDeCriacao.dias[indiceDoDiaSementeParaReplicar];
    const analiseDesteDia = analisarDiaDeTrabalho(diaSemente);

    if (!diaSemente.diaTrabalho) return;
    if (analiseDesteDia.semHorario || analiseDesteDia.incompleto) {
      notificar(
        "warning",
        "É obrigatório preencher ao menos um período completo antes de acionar a replicação.",
      );
      return;
    }

    setDadosDoFormularioDeCriacao((estadoPrevio) => {
      const novosDiasReplicados = estadoPrevio.dias.map(
        (diaIterado, indiceLaco) => {
          if (
            indiceLaco !== indiceDoDiaSementeParaReplicar &&
            diaIterado.diaTrabalho
          ) {
            return {
              ...diaIterado,
              entrada1: diaSemente.entrada1,
              saida1: diaSemente.saida1,
              entrada2: diaSemente.entrada2,
              saida2: diaSemente.saida2,
            };
          }
          return diaIterado;
        },
      );
      return { ...estadoPrevio, dias: novosDiasReplicados };
    });
    notificar(
      "success",
      "Os horários foram replicados para todos os demais dias úteis ativados.",
    );
  };

  const aplicarRegrasDeValidacaoAoFormulario = () => {
    if (!dadosDoFormularioDeCriacao.descricao?.trim())
      return "Por favor, defina um nome obrigatório para a escala de trabalho.";
    if (acessoGlobal && !dadosDoFormularioDeCriacao.gestaoLocalId)
      return "Selecione a Gestão Local responsável pelo turno.";
    if (
      !dadosDoFormularioDeCriacao.dias.some(
        (diaValidacao) => diaValidacao.diaTrabalho,
      )
    )
      return "É necessário marcar e configurar pelo menos um dia útil de trabalho para a escala existir.";

    for (const diaSendoValidado of dadosDoFormularioDeCriacao.dias) {
      if (!diaSendoValidado.diaTrabalho) continue;
      const nomenclaturaDoDia = obterInformacoesDoDia(
        diaSendoValidado.diaSemana,
      ).nome;
      const blocosParaChecar = [
        ["Entrada 1", diaSendoValidado.entrada1],
        ["Saída 1", diaSendoValidado.saida1],
        ["Entrada 2", diaSendoValidado.entrada2],
        ["Saída 2", diaSendoValidado.saida2],
      ];

      for (const [rotuloParaMensagem, valorPreenchido] of blocosParaChecar) {
        if (!validarFormatoDeHora(valorPreenchido))
          return `O campo correspondente a ${rotuloParaMensagem} é inválido na ${nomenclaturaDoDia}.`;
      }
      const analiseLocal = analisarDiaDeTrabalho(diaSendoValidado);
      if (analiseLocal.incompleto)
        return `Existe um período que começou, mas não terminou, e está incompleto em ${nomenclaturaDoDia}.`;
      if (analiseLocal.semHorario)
        return `${nomenclaturaDoDia} não possui nenhum horário de trabalho associado apesar de estar marcado como dia de trabalho.`;
      if (
        analiseLocal.periodos.some(
          (periodoDaAnalise) => periodoDaAnalise.minutos === 0,
        )
      )
        return `Foi detectado um período cuja duração efetiva é igual a zero em ${nomenclaturaDoDia}.`;
    }

    // A API é a fonte autoritativa para limites regulamentares da jornada.
    // O frontend mantém apenas validações estruturais do formulário acima;
    // referências de horas continuam disponíveis para filtros/indicadores visuais.
    return null;
  };

  const enviarRotinaProcessadaAoBackend = async () => {
    if (!podeEscrever) return;
    const erroEncontradoNaValidacao = aplicarRegrasDeValidacaoAoFormulario();
    if (erroEncontradoNaValidacao) {
      notificar("warning", erroEncontradoNaValidacao);
      return;
    }

    await confirmarComSenha({
      titulo: idDoTurnoEmEdicao
        ? "Confirmar o Salvamento da Escala"
        : "Confirmar a Criação de Nova Escala",
      mensagem:
        "Confirme a sua senha para enviar e gravar definitivamente esta rotina de escala semanal no sistema central.",
      acao: async (senhaConfirmacao) => {
        const corpoFormatadoParaAAPI = {
          id: idDoTurnoEmEdicao || null,
          descricao: dadosDoFormularioDeCriacao.descricao?.trim(),
          ativo: dadosDoFormularioDeCriacao.ativo !== false,
          gestaoLocalId: dadosDoFormularioDeCriacao.gestaoLocalId ? Number(dadosDoFormularioDeCriacao.gestaoLocalId) : null,
          dias: dadosDoFormularioDeCriacao.dias.map((d) => ({
            diaSemana: d.diaSemana,
            diaTrabalho: d.diaTrabalho,
            entrada1: d.entrada1 || null,
            saida1: d.saida1 || null,
            entrada2: d.entrada2 || null,
            saida2: d.saida2 || null,
          })),
        };
        await ServicoJornada.salvar(corpoFormatadoParaAAPI, { senhaConfirmacao });
        notificar(
          "success",
          "A escala foi salva com total sucesso e já está disponível para uso.",
        );
        fecharOModalAtivo();
        acionarCarregamentoDosTurnosDoServidor();
      },
    });
  };

  const apagarEscalaAtivaDoServidor = async (idParaExcluir) => {
    if (!podeEscrever) return;
    await confirmarComSenha({
      titulo: "Inativar turno",
      mensagem:
        "O turno será inativado, preservando sua identidade histórica. O Java bloqueará a operação se existir programação futura que dependa dele.",
      acao: async (senhaConfirmacao) => {
        await ServicoJornada.excluir(idParaExcluir, { senhaConfirmacao });
        notificar(
          "success",
          "O turno foi inativado e permanece disponível para consultas históricas.",
        );
        acionarCarregamentoDosTurnosDoServidor();
      },
    });
  };

  const limparFormularioRegra = () => {
    setRegraEmEdicao(null);
    setFormularioRegra({ tipo: "", inicio: "", fim: "", percentual: "", ativo: true, ordem: 0 });
  };

  const editarRegraAdicional = (regra) => {
    if (!podeEscrever) return;
    setRegraEmEdicao(regra?.id || null);
    setFormularioRegra({
      tipo: regra?.tipo || "",
      inicio: regra?.inicio || "",
      fim: regra?.fim || "",
      percentual: regra?.percentual ?? "",
      ativo: regra?.ativo !== false,
      ordem: regra?.ordem ?? 0,
    });
  };

  const salvarRegraAdicional = async () => {
    if (!podeEscrever || !idDoTurnoEmEdicao) return;
    if (!formularioRegra.tipo?.trim()) {
      notificar("warning", "Informe o tipo da regra adicional.");
      return;
    }
    await confirmarComSenha({
      titulo: regraEmEdicao ? "Atualizar regra adicional" : "Adicionar regra ao turno",
      mensagem: "A regra será validada pelo Java e ficará vinculada a esta jornada.",
      acao: async (senhaConfirmacao) => {
        const payload = {
          tipo: formularioRegra.tipo.trim(),
          inicio: formularioRegra.inicio || null,
          fim: formularioRegra.fim || null,
          percentual: formularioRegra.percentual === "" ? null : Number(formularioRegra.percentual),
          ativo: formularioRegra.ativo !== false,
          ordem: formularioRegra.ordem === "" ? null : Number(formularioRegra.ordem),
        };
        if (regraEmEdicao) await ServicoJornada.atualizarRegra(regraEmEdicao, payload, { senhaConfirmacao });
        else await ServicoJornada.adicionarRegra(idDoTurnoEmEdicao, payload, { senhaConfirmacao });
        const atualizadas = await ServicoJornada.buscarRegras(idDoTurnoEmEdicao);
        setRegrasAdicionais(Array.isArray(atualizadas) ? atualizadas : []);
        limparFormularioRegra();
      },
    });
  };

  const excluirRegraAdicional = async (regraId) => {
    if (!podeEscrever || !regraId) return;
    await confirmarComSenha({
      titulo: "Excluir regra adicional",
      mensagem: "A exclusão é definitiva para esta regra. O turno e seu histórico permanecem preservados.",
      acao: async (senhaConfirmacao) => {
        await ServicoJornada.excluirRegra(regraId, { senhaConfirmacao });
        setRegrasAdicionais((atuais) => atuais.filter((regra) => String(regra.id) !== String(regraId)));
        if (String(regraEmEdicao) === String(regraId)) limparFormularioRegra();
      },
    });
  };

  const listagemDeTurnosTurbinada = useMemo(() => {
    return listaTotalDeTurnos.map((itemBruto) => ({
      ...itemBruto,
      analise: analisarEscalaCompleta(itemBruto),
    }));
  }, [listaTotalDeTurnos]);

  const contagensDeTurnosPorEstado = useMemo(
    () => contarPorEstado(listagemDeTurnosTurbinada),
    [listagemDeTurnosTurbinada],
  );

  const listagemDeTurnosDoEstado = useMemo(
    () => filtrarPorEstado(listagemDeTurnosTurbinada, filtroDeEstado, acessoGlobal),
    [listagemDeTurnosTurbinada, filtroDeEstado, acessoGlobal],
  );

  const calculoDasSomasDosFiltros = useMemo(() => {
    const estruturaDeContagens = {
      TODAS: listagemDeTurnosDoEstado.length,
      FIM_DE_SEMANA: 0,
      NOTURNAS: 0,
      ATE_LIMIAR_INF: 0,
      PROX_REF: 0,
      ACIMA_REF: 0,
    };

    listagemDeTurnosDoEstado.forEach((turnoDoLaco) => {
      if (turnoDoLaco.analise.possuiFimDeSemana)
        estruturaDeContagens.FIM_DE_SEMANA++;
      if (turnoDoLaco.analise.atravessaMeiaNoite)
        estruturaDeContagens.NOTURNAS++;

      const calculoParcialEmHoras = turnoDoLaco.analise.minutosSemanais / 60;
      if (calculoParcialEmHoras <= CONFIG.JORNADA_LIMIAR_INFERIOR)
        estruturaDeContagens.ATE_LIMIAR_INF++;
      else if (
        calculoParcialEmHoras > CONFIG.JORNADA_LIMIAR_INFERIOR &&
        calculoParcialEmHoras <= CONFIG.JORNADA_REFERENCIA_HORAS
      )
        estruturaDeContagens.PROX_REF++;
      else if (calculoParcialEmHoras > CONFIG.JORNADA_REFERENCIA_HORAS)
        estruturaDeContagens.ACIMA_REF++;
    });
    return estruturaDeContagens;
  }, [listagemDeTurnosDoEstado]);

  const aplicacaoDeFiltrosERenderizacao = useMemo(() => {
    let arrayLocalFiltrado = [...listagemDeTurnosDoEstado];

    if (textoDaBusca) {
      const textoTransformadoEmBaixo = textoDaBusca.toLowerCase();
      arrayLocalFiltrado = arrayLocalFiltrado.filter(
        (turnoParaTeste) =>
          turnoParaTeste.descricao
            ?.toLowerCase()
            .includes(textoTransformadoEmBaixo) ||
          turnoParaTeste.gestaoLocalNome
            ?.toLowerCase()
            .includes(textoTransformadoEmBaixo) ||
          turnoParaTeste.superintendenciaNome
            ?.toLowerCase()
            .includes(textoTransformadoEmBaixo) ||
          normalizarUf(turnoParaTeste.uf)
            .toLowerCase()
            .includes(textoTransformadoEmBaixo) ||
          String(turnoParaTeste.id).includes(textoTransformadoEmBaixo) ||
          formatarParaHorasEMinutos(turnoParaTeste.analise.minutosSemanais)
            .toLowerCase()
            .includes(textoTransformadoEmBaixo),
      );
    }

    if (filtroDeCategoriaAtivo !== "TODAS") {
      arrayLocalFiltrado = arrayLocalFiltrado.filter((turnoTesteCategoria) => {
        const numeroHorasAvaliado =
          turnoTesteCategoria.analise.minutosSemanais / 60;
        switch (filtroDeCategoriaAtivo) {
          case "FIM_DE_SEMANA":
            return turnoTesteCategoria.analise.possuiFimDeSemana;
          case "NOTURNAS":
            return turnoTesteCategoria.analise.atravessaMeiaNoite;
          case "ATE_LIMIAR_INF":
            return numeroHorasAvaliado <= CONFIG.JORNADA_LIMIAR_INFERIOR;
          case "PROX_REF":
            return (
              numeroHorasAvaliado > CONFIG.JORNADA_LIMIAR_INFERIOR &&
              numeroHorasAvaliado <= CONFIG.JORNADA_REFERENCIA_HORAS
            );
          case "ACIMA_REF":
            return numeroHorasAvaliado > CONFIG.JORNADA_REFERENCIA_HORAS;
          default:
            return true;
        }
      });
    }

    return arrayLocalFiltrado.sort((comparadorA, comparadorB) => {
      switch (regraDeOrdenacaoAtual) {
        case "NOME_AZ":
          return String(comparadorA.descricao || "").localeCompare(
            String(comparadorB.descricao || ""),
            "pt-BR",
          );
        case "MAIOR_CARGA":
          return (
            comparadorB.analise.minutosSemanais -
            comparadorA.analise.minutosSemanais
          );
        case "MENOR_CARGA":
          return (
            comparadorA.analise.minutosSemanais -
            comparadorB.analise.minutosSemanais
          );
        default:
          return 0;
      }
    });
  }, [
    listagemDeTurnosDoEstado,
    textoDaBusca,
    filtroDeCategoriaAtivo,
    regraDeOrdenacaoAtual,
  ]);

  const resumoExecutivoDaPagina = useMemo(() => {
    return {
      totalDeTurnos: listagemDeTurnosDoEstado.length,
      turnosComFimDeSemana: calculoDasSomasDosFiltros.FIM_DE_SEMANA,
      turnosNoturnos: calculoDasSomasDosFiltros.NOTURNAS,
    };
  }, [listagemDeTurnosDoEstado, calculoDasSomasDosFiltros]);

  const listaDeGestoesLocaisDisponiveis = useMemo(() => {
    const gestorSelecionado = String(dadosDoFormularioDeCriacao.gestaoLocalId || "");
    return listaDeGestoesLocais
      .filter((gestaoLocal) =>
        gestaoLocal?.ativo !== false || String(gestaoLocal?.id) === gestorSelecionado,
      )
      .toSorted((primeiro, segundo) => {
        const ufPrimeiro = normalizarUf(primeiro?.uf || primeiro?.superintendencia?.uf);
        const ufSegundo = normalizarUf(segundo?.uf || segundo?.superintendencia?.uf);
        return `${ufPrimeiro}-${primeiro?.nome || ""}`.localeCompare(
          `${ufSegundo}-${segundo?.nome || ""}`,
          "pt-BR",
        );
      });
  }, [listaDeGestoesLocais, dadosDoFormularioDeCriacao.gestaoLocalId]);

  const calculoDoEstadoDoFormulario = useMemo(
    () => analisarEscalaCompleta({ dias: dadosDoFormularioDeCriacao.dias }),
    [dadosDoFormularioDeCriacao.dias],
  );

  return (
    <div className={estilos.containerDaPaginaBase}>
      <ModalConfirmacao />

      <CabecalhoHero
        termoDeBusca={textoDaBusca}
        aoMudarTermoDeBusca={setTextoDaBusca}
        aoAbrirNovoTurno={lidarComAberturaDoModalParaNovoTurno}
        resumoExecutivo={resumoExecutivoDaPagina}
        podeEscrever={podeEscrever}
      />

      <FiltrosAvancados
        totalDeResultados={aplicacaoDeFiltrosERenderizacao.length}
        contagensDeFiltro={calculoDasSomasDosFiltros}
        filtroAtivo={filtroDeCategoriaAtivo}
        aoDefinirFiltroAtivo={setFiltroDeCategoriaAtivo}
        ordenacaoAtual={regraDeOrdenacaoAtual}
        aoDefinirOrdenacao={setRegraDeOrdenacaoAtual}
        exibirFiltroEstado={acessoGlobal}
        estadoSelecionado={filtroDeEstado}
        aoDefinirEstado={setFiltroDeEstado}
        contagensPorEstado={contagensDeTurnosPorEstado}
      />

      <ListaDeTurnos
        carregandoOsDados={estaCarregandoDados}
        turnosProcessadosNaLista={aplicacaoDeFiltrosERenderizacao}
        aoEditarTurno={lidarComAberturaDoModalParaEdicaoDeTurno}
        aoExcluirTurno={apagarEscalaAtivaDoServidor}
        podeEscrever={podeEscrever}
      />

      <ModalDeTurno
        aberto={modalEstaAberto}
        aoFechar={fecharOModalAtivo}
        dadosDoFormulario={dadosDoFormularioDeCriacao}
        idEdicaoTurno={idDoTurnoEmEdicao}
        podeEscrever={podeEscrever}
        analiseDoFormulario={calculoDoEstadoDoFormulario}
        aoMudarDadosGerais={lidarComAMudancaDoNomeDoTurnoNoFormulario}
        aoMudarAtivo={lidarComMudancaAtivo}
        acessoGlobal={acessoGlobal}
        listaDeGestoesLocais={listaDeGestoesLocaisDisponiveis}
        aoMudarGestaoLocal={(gestaoLocalId) => setDadosDoFormularioDeCriacao((atual) => ({ ...atual, gestaoLocalId }))}
        aoMudarConfiguracaoDoDia={lidarComAMudancaNosCamposDeUmDiaEspecifico}
        aoReplicarHorariosAosOutrosDias={
          acionarAreplicacaoDosHorariosPreenchidos
        }
        aoSalvarTurnoNoSistema={enviarRotinaProcessadaAoBackend}
        regrasAdicionais={regrasAdicionais}
        carregandoRegras={carregandoRegras}
        formularioRegra={formularioRegra}
        regraEmEdicao={regraEmEdicao}
        aoMudarFormularioRegra={(campo, valor) => setFormularioRegra((atual) => ({ ...atual, [campo]: valor }))}
        aoEditarRegra={editarRegraAdicional}
        aoSalvarRegra={salvarRegraAdicional}
        aoExcluirRegra={excluirRegraAdicional}
        aoCancelarEdicaoRegra={limparFormularioRegra}
      />
    </div>
  );
};
