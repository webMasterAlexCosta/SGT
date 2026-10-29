import { useState, useEffect, useMemo, useCallback } from "react";
import { ServicoEmpregado } from "../../../../../../services/rh/ServicoEmpregado.js";
import { ServicoJornada } from "../../../../../../services/admin/ServicoJornada.js";
import { notificar } from "../../../../../../core/events.js";
import {
  extrairApenasDigitos,
  verificarCpfValido,
  converterMoedaParaNumero,
  calcularTotalMinutosJornada,
  formatarNumeroTelefone,
  verificarTelefoneValido,
} from "./utilitarios";
import {
  contratoUnicoParaPayload,
  normalizarContratoUnico,
} from "../../../../../../services/rh/ServicoDominioEmpregado.js";

export const useLogicaFormulario = ({ idUsuario, aoTerSucesso }) => {
  const [indicadorCarregamento, setIndicadorCarregamento] = useState(false);
  const [listasAuxiliares, setListasAuxiliares] = useState({
    lotacoes: [],
    cargos: [],
    jornadas: [],
    bancos: [],
    agencias: [],
    tiposConta: [],
    contratos: [],
    statuses: [],
  });
  const [estadoCamposBloqueados, setEstadoCamposBloqueados] = useState({});
  const [listaTelefonesEditaveis, setListaTelefonesEditaveis] = useState([
    true,
  ]);

  const [dadosFormulario, setDadosFormulario] = useState({
    nome: "",
    matricula: "",
    cpf: "",
    rg: "",
    ctpsSerie: "",
    dataNascimento: "",
    nitPisPasep: "",
    formacaoEscolar: "",
    email: "",
    dataAdmissao: "",
    lotacaoId: "",
    cargoId: "",
    jornadaId: "",
    statusId: "",
    optanteFgts: false,
    dataOpcaoFgts: "",
    qtdDependenteIr: "",
    qtdDependenteSalFamilia: "",
    qtdBeneficiariosPensaoAlim: "",
    optanteVa: false,
    valorVa: "",
    outrosBeneficios: "",
    logradouro: "",
    numero: "",
    bairro: "",
    cidade: "",
    uf: "",
    cep: "",
    telefones: [""],
    bancoId: "",
    agenciaId: "",
    tipoContaId: "",
    numeroConta: "",
    contratoId: "",
    observacao: "",
  });

  const buscarIdStatusAtivo = useCallback((listaDeStatus) => {
    const statusAtivo = (listaDeStatus || []).find(
      (status) => String(status?.descricao || "").toUpperCase() === "ATIVO",
    );
    return statusAtivo?.id ? String(statusAtivo.id) : "";
  }, []);

  useEffect(() => {
    const controladorDeAborto = new AbortController();

    const inicializarDadosDoServidor = async () => {
      setIndicadorCarregamento(true);
      try {
        const [dadosServidor, resJornadas] = await Promise.all([
          ServicoEmpregado.buscarDadosAuxiliares({
            signal: controladorDeAborto.signal,
          }),
          ServicoJornada.listarTodos({
            signal: controladorDeAborto.signal,
          }).catch(() => []),
        ]);

        const jornadasGlobais = Array.isArray(resJornadas)
          ? resJornadas
          : resJornadas?.content || [];

        const contratosAtivosFiltrados = (
          Array.isArray(dadosServidor?.contratos) ? dadosServidor.contratos : []
        ).filter((contrato) => contrato.ativo === true);

        setListasAuxiliares({
          ...dadosServidor,
          jornadas: jornadasGlobais,
          contratos: contratosAtivosFiltrados,
        });

        const idStatusPadraoAtivo = buscarIdStatusAtivo(dadosServidor.statuses);

        setDadosFormulario((estadoAnterior) => ({
          ...estadoAnterior,
          statusId: estadoAnterior.statusId || idStatusPadraoAtivo,
        }));

        if (idUsuario) {
          const dadosDoEmpregado = await ServicoEmpregado.buscarPorId(idUsuario, {
            signal: controladorDeAborto.signal,
          });

          const listaTelefonesCarregados = dadosDoEmpregado.telefones?.length
            ? dadosDoEmpregado.telefones.map((tel) => tel.numero)
            : [""];

          setDadosFormulario((estadoAnterior) => ({
            ...estadoAnterior,
            ...dadosDoEmpregado,
            matricula: dadosDoEmpregado.matricula || "",
            lotacaoId: dadosDoEmpregado.lotacaoId ? String(dadosDoEmpregado.lotacaoId) : (dadosDoEmpregado.lotacao?.id ? String(dadosDoEmpregado.lotacao.id) : ""),
            cargoId: dadosDoEmpregado.cargoId ? String(dadosDoEmpregado.cargoId) : (dadosDoEmpregado.cargo?.id ? String(dadosDoEmpregado.cargo.id) : ""),
            jornadaId: dadosDoEmpregado.jornadaId ? String(dadosDoEmpregado.jornadaId) : (dadosDoEmpregado.jornada?.id ? String(dadosDoEmpregado.jornada.id) : (dadosDoEmpregado.turno?.id ? String(dadosDoEmpregado.turno.id) : "")),
            statusId: dadosDoEmpregado.statusId ? String(dadosDoEmpregado.statusId) : (dadosDoEmpregado.status?.id ? String(dadosDoEmpregado.status.id) : estadoAnterior.statusId || idStatusPadraoAtivo),
            bancoId: dadosDoEmpregado.conta?.bancoId
              ? String(dadosDoEmpregado.conta.bancoId)
              : "",
            agenciaId: dadosDoEmpregado.conta?.agenciaId
              ? String(dadosDoEmpregado.conta.agenciaId)
              : "",
            tipoContaId: dadosDoEmpregado.conta?.tipoContaId
              ? String(dadosDoEmpregado.conta.tipoContaId)
              : "",
            numeroConta: dadosDoEmpregado.conta?.numeroConta || "",
            logradouro: dadosDoEmpregado.endereco?.logradouro || "",
            numero: dadosDoEmpregado.endereco?.numero || "",
            bairro: dadosDoEmpregado.endereco?.bairro || "",
            cidade: dadosDoEmpregado.endereco?.cidade || "",
            uf: dadosDoEmpregado.endereco?.uf || "RJ",
            cep: dadosDoEmpregado.endereco?.cep || "",
            telefones: listaTelefonesCarregados,
            contratoId: normalizarContratoUnico(dadosDoEmpregado.contratos),
            optanteFgts: Boolean(dadosDoEmpregado.optanteFgts),
            optanteVa: Boolean(dadosDoEmpregado.optanteVa),
            dataAdmissao: dadosDoEmpregado.dataAdmissao
              ? dadosDoEmpregado.dataAdmissao.split("T")[0]
              : "",
            dataNascimento: dadosDoEmpregado.dataNascimento
              ? dadosDoEmpregado.dataNascimento.split("T")[0]
              : "",
            dataOpcaoFgts: dadosDoEmpregado.dataOpcaoFgts
              ? dadosDoEmpregado.dataOpcaoFgts.split("T")[0]
              : "",
          }));
          setListaTelefonesEditaveis(
            new Array(listaTelefonesCarregados.length).fill(false),
          );
        }
      } catch (erro) {
        if (!erro?.canceled && erro?.name !== "CanceledError") {
          notificar("error", "Erro ao carregar dados do colaborador.");
        }
      } finally {
        if (!controladorDeAborto.signal.aborted) {
          setIndicadorCarregamento(false);
        }
      }
    };

    inicializarDadosDoServidor();
    return () => {
      controladorDeAborto.abort();
    };
  }, [idUsuario, buscarIdStatusAtivo]);

  const lotacaoAtualmenteSelecionada = useMemo(
    () =>
      listasAuxiliares.lotacoes.find(
        (lotacao) => String(lotacao.id) === String(dadosFormulario.lotacaoId),
      ),
    [listasAuxiliares.lotacoes, dadosFormulario.lotacaoId],
  );

  const turnosDisponiveisFiltrados = useMemo(() => {
    const turnosResumo = lotacaoAtualmenteSelecionada?.turnos || [];
    return turnosResumo.map((turnoResumo) => {
      return (
        listasAuxiliares.jornadas?.find(
          (j) => String(j.id) === String(turnoResumo.id),
        ) || turnoResumo
      );
    });
  }, [lotacaoAtualmenteSelecionada, listasAuxiliares.jornadas]);

  const jornadaAtualmenteSelecionada = useMemo(
    () =>
      turnosDisponiveisFiltrados.find(
        (turno) => String(turno.id) === String(dadosFormulario.jornadaId),
      ),
    [turnosDisponiveisFiltrados, dadosFormulario.jornadaId],
  );

  const statusAtualEstaInativo = useMemo(() => {
    const statusAtivo = listasAuxiliares.statuses.find(
      (status) => String(status.id) === String(dadosFormulario.statusId),
    );
    return statusAtivo && statusAtivo.descricao.toUpperCase() === "INATIVO";
  }, [dadosFormulario.statusId, listasAuxiliares.statuses]);

  const agenciasBancariasFiltradas = useMemo(() => {
    if (!dadosFormulario.bancoId) return listasAuxiliares.agencias || [];
    return (listasAuxiliares.agencias || []).filter(
      (agencia) =>
        String(agencia?.bancoId) === String(dadosFormulario.bancoId) ||
        String(agencia?.banco?.id) === String(dadosFormulario.bancoId)
    );
  }, [listasAuxiliares.agencias, dadosFormulario.bancoId]);

  const listaContratosSelecionados = useMemo(
    () =>
      (listasAuxiliares.contratos || []).filter((contrato) =>
        String(dadosFormulario.contratoId) === String(contrato.id),
      ),
    [listasAuxiliares.contratos, dadosFormulario.contratoId],
  );

  const resumoValorHoraContrato = useMemo(() => {
    if (!listaContratosSelecionados.length)
      return { valor: null, rotuloExibicao: "Valor/Hora" };
    const valoresColetados = listaContratosSelecionados.map((contrato) =>
      converterMoedaParaNumero(contrato.valorHora),
    );
    const valoresUnicos = [...new Set(valoresColetados)];
    if (valoresUnicos.length === 1)
      return { valor: valoresUnicos[0], rotuloExibicao: "Valor/Hora" };
    return { valor: null, rotuloExibicao: "Múltiplos valores" };
  }, [listaContratosSelecionados]);

  const valorSalarioPrevisto = useMemo(() => {
    const totalMinutos = calcularTotalMinutosJornada(
      jornadaAtualmenteSelecionada,
    );
    if (resumoValorHoraContrato.valor == null || !totalMinutos) return null;
    return resumoValorHoraContrato.valor * (totalMinutos / 60);
  }, [resumoValorHoraContrato.valor, jornadaAtualmenteSelecionada]);

  useEffect(() => {
    setDadosFormulario((estadoAnterior) => {
      let modificado = false;
      const proximoEstado = { ...estadoAnterior };

      if (!proximoEstado.lotacaoId && proximoEstado.jornadaId !== "") {
        proximoEstado.jornadaId = "";
        modificado = true;
      } else if (
        turnosDisponiveisFiltrados.length === 1 &&
        proximoEstado.jornadaId !== String(turnosDisponiveisFiltrados[0].id)
      ) {
        proximoEstado.jornadaId = String(turnosDisponiveisFiltrados[0].id);
        modificado = true;
      } else if (
        proximoEstado.jornadaId &&
        turnosDisponiveisFiltrados.length > 1 &&
        !turnosDisponiveisFiltrados.some(
          (turno) => String(turno.id) === String(proximoEstado.jornadaId),
        )
      ) {
        proximoEstado.jornadaId = "";
        modificado = true;
      }

      return modificado ? proximoEstado : estadoAnterior;
    });
  }, [dadosFormulario.lotacaoId, turnosDisponiveisFiltrados]);

  const aoLidarComMudancaEntrada = useCallback(
    (evento) => {
      const { name, value, type, checked } = evento.target;
      setDadosFormulario((estadoAnterior) => {
        const proximoEstado = {
          ...estadoAnterior,
          [name]: type === "checkbox" ? checked : value,
        };
        if (name === "statusId") {
          const itemStatus = listasAuxiliares.statuses.find(
            (status) => String(status.id) === String(value),
          );
          if (itemStatus && itemStatus.descricao.toUpperCase() === "INATIVO")
            proximoEstado.contratoId = "";
        }
        if (name === "bancoId") proximoEstado.agenciaId = "";
        return proximoEstado;
      });
    },
    [listasAuxiliares.statuses],
  );

  const aoAlternarBloqueioCampo = useCallback((nomeDoCampo) => {
    setEstadoCamposBloqueados((estadoAnterior) => ({
      ...estadoAnterior,
      [nomeDoCampo]: !estadoAnterior[nomeDoCampo],
    }));
  }, []);

  const verificarCampoEstaHabilitado = useCallback(
    (nomeDoCampo) => {
      return !idUsuario || estadoCamposBloqueados[nomeDoCampo];
    },
    [idUsuario, estadoCamposBloqueados],
  );

  const aoMudarValorTelefone = useCallback((indiceTelefone, novoValor) => {
    setDadosFormulario((estadoAnterior) => {
      const novaListaTelefones = [...estadoAnterior.telefones];
      novaListaTelefones[indiceTelefone] = novoValor;
      return { ...estadoAnterior, telefones: novaListaTelefones };
    });
  }, []);

  const gerenciarListaDeTelefones = useCallback((tipoAcao, indiceRemocao) => {
    if (tipoAcao === "adicionar") {
      setDadosFormulario((estadoAnterior) => ({
        ...estadoAnterior,
        telefones: [...estadoAnterior.telefones, ""],
      }));
      setListaTelefonesEditaveis((estadoAnterior) => [...estadoAnterior, true]);
    }
    if (tipoAcao === "remover") {
      setDadosFormulario((estadoAnterior) => {
        const listaTelefonesAtualizada = estadoAnterior.telefones.filter(
          (_, indice) => indice !== indiceRemocao,
        );
        return {
          ...estadoAnterior,
          telefones: listaTelefonesAtualizada.length
            ? listaTelefonesAtualizada
            : [""],
        };
      });
      setListaTelefonesEditaveis((estadoAnterior) => {
        const listaEditaveisAtualizada = estadoAnterior.filter(
          (_, indice) => indice !== indiceRemocao,
        );
        return listaEditaveisAtualizada.length
          ? listaEditaveisAtualizada
          : [true];
      });
    }
  }, []);

  const alternarModoEdicaoDoTelefone = useCallback(
    (indiceTelefone) => {
      setListaTelefonesEditaveis((estadoAnterior) => {
        if (estadoAnterior[indiceTelefone]) {
          const valorTelefoneAtual = dadosFormulario.telefones[indiceTelefone];
          if (
            valorTelefoneAtual &&
            !verificarTelefoneValido(valorTelefoneAtual)
          ) {
            notificar("warning", "Número de telefone inválido.");
            return estadoAnterior;
          }
        }
        const novaLista = [...estadoAnterior];
        novaLista[indiceTelefone] = !novaLista[indiceTelefone];
        return novaLista;
      });
    },
    [dadosFormulario.telefones],
  );

  const aoAlternarSelecaoDoContrato = useCallback(
    (idDoContratoSelecionado) => {
      if (statusAtualEstaInativo)
        return notificar(
          "warning",
          "Não é possível vincular contratos a um colaborador inativo.",
        );
      setDadosFormulario((estadoAnterior) => ({
        ...estadoAnterior,
        contratoId:
          String(estadoAnterior.contratoId) === String(idDoContratoSelecionado)
            ? ""
            : String(idDoContratoSelecionado),
      }));
    },
    [statusAtualEstaInativo],
  );

  const exibirDescricaoTurnoComCargaHoraria = useCallback((dadosDoTurno) => {
    if (!dadosDoTurno) return "";

    if (dadosDoTurno.dias && dadosDoTurno.dias.length > 0) {
      const diasTrabalho = dadosDoTurno.dias.filter((d) => d.diaTrabalho);
      if (diasTrabalho.length === 0)
        return `${dadosDoTurno.descricao || dadosDoTurno.nome} (Sem dias úteis)`;

      const gruposHorarios = {};
      diasTrabalho.forEach((d) => {
        const p1 = d.entrada1 ? `${d.entrada1} às ${d.saida1}` : "";
        const p2 = d.entrada2 ? `${d.entrada2} às ${d.saida2}` : "";
        const horario = p1 && p2 ? `${p1} e ${p2}` : p1 || "Horário flexível";

        if (!gruposHorarios[horario]) gruposHorarios[horario] = 0;
        gruposHorarios[horario]++;
      });

      const chaves = Object.keys(gruposHorarios);
      if (chaves.length === 1) {
        return `${dadosDoTurno.descricao || dadosDoTurno.nome} • ${chaves[0]} (${diasTrabalho.length} dias)`;
      }

      const descricaoGrupos = chaves
        .map((horario) => {
          const qtd = gruposHorarios[horario];
          return `${qtd} ${qtd > 1 ? "dias" : "dia"} (${horario})`;
        })
        .join(" | ");

      return `${dadosDoTurno.descricao || dadosDoTurno.nome} • ${descricaoGrupos}`;
    }

    const minutosCalculados = calcularTotalMinutosJornada(dadosDoTurno);
    return `${dadosDoTurno.descricao || dadosDoTurno.nome}${minutosCalculados ? ` • ${Math.floor(minutosCalculados / 60)}h${String(minutosCalculados % 60).padStart(2, "0")}` : ""}`;
  }, []);

  const aoSubmeterFormularioServidor = useCallback(
    async (eventoDeSubmissao) => {
      eventoDeSubmissao.preventDefault();
      if (!dadosFormulario.nome?.trim())
        return notificar("warning", "Atenção: O nome é obrigatório.");
      if (dadosFormulario.matricula?.trim().length > 30)
        return notificar("warning", "A matrícula deve ter no máximo 30 caracteres.");
      if (
        !dadosFormulario.cpf?.trim() ||
        !verificarCpfValido(dadosFormulario.cpf)
      )
        return notificar("warning", "O CPF informado é inválido ou está ausente.");
      if (
        !dadosFormulario.lotacaoId ||
        !dadosFormulario.cargoId ||
        !dadosFormulario.jornadaId
      )
        return notificar(
          "warning",
          "A Lotação, o Cargo e o Turno são campos obrigatórios.",
        );
      if (!dadosFormulario.dataAdmissao)
        return notificar("warning", "A Data de Admissão é obrigatória.");
      if (!statusAtualEstaInativo && !dadosFormulario.contratoId)
        return notificar(
          "warning",
          "Selecione o contrato atual do colaborador ativo.",
        );

      setIndicadorCarregamento(true);
      try {
        const pacoteDeDadosEnvio = {
          id: idUsuario ? Number(idUsuario) : undefined,
          nome: dadosFormulario.nome,
          matricula: dadosFormulario.matricula?.trim() || null,
          cpf: extrairApenasDigitos(dadosFormulario.cpf),
          rg: dadosFormulario.rg || null,
          ctpsSerie: dadosFormulario.ctpsSerie || null,
          nitPisPasep:
            extrairApenasDigitos(dadosFormulario.nitPisPasep) || null,
          formacaoEscolar: dadosFormulario.formacaoEscolar || null,
          email: dadosFormulario.email || null,
          dataNascimento: dadosFormulario.dataNascimento || null,
          dataAdmissao: dadosFormulario.dataAdmissao || null,
          optanteFgts: Boolean(dadosFormulario.optanteFgts),
          dataOpcaoFgts: dadosFormulario.dataOpcaoFgts || null,
          qtdDependenteIr:
            dadosFormulario.qtdDependenteIr !== ""
              ? Number(dadosFormulario.qtdDependenteIr)
              : null,
          qtdDependenteSalFamilia:
            dadosFormulario.qtdDependenteSalFamilia !== ""
              ? Number(dadosFormulario.qtdDependenteSalFamilia)
              : null,
          optanteVa: Boolean(dadosFormulario.optanteVa),
          valorVa: converterMoedaParaNumero(dadosFormulario.valorVa),
          outrosBeneficios: dadosFormulario.outrosBeneficios || null,
          observacao: dadosFormulario.observacao || null,
          lotacaoId: dadosFormulario.lotacaoId
            ? Number(dadosFormulario.lotacaoId)
            : null,
          cargoId: dadosFormulario.cargoId
            ? Number(dadosFormulario.cargoId)
            : null,
          statusId: dadosFormulario.statusId
            ? Number(dadosFormulario.statusId)
            : null,
          jornadaId: dadosFormulario.jornadaId
            ? Number(dadosFormulario.jornadaId)
            : null,
          contratos: contratoUnicoParaPayload(dadosFormulario.contratoId),
          telefones: dadosFormulario.telefones
            .filter(verificarTelefoneValido)
            .map((telefoneNumero) => ({
              numero: formatarNumeroTelefone(telefoneNumero),
            })),
          conta:
            dadosFormulario.numeroConta ||
            dadosFormulario.agenciaId ||
            dadosFormulario.tipoContaId
              ? {
                  numeroConta: dadosFormulario.numeroConta || null,
                  bancoId: dadosFormulario.bancoId
                    ? Number(dadosFormulario.bancoId)
                    : null,
                  agenciaId: dadosFormulario.agenciaId
                    ? Number(dadosFormulario.agenciaId)
                    : null,
                  tipoContaId: dadosFormulario.tipoContaId
                    ? Number(dadosFormulario.tipoContaId)
                    : null,
                }
              : null,
          endereco:
            dadosFormulario.logradouro ||
            dadosFormulario.cep ||
            dadosFormulario.bairro ||
            dadosFormulario.cidade ||
            dadosFormulario.numero
              ? {
                  logradouro: dadosFormulario.logradouro || null,
                  numero: dadosFormulario.numero || null,
                  bairro: dadosFormulario.bairro || null,
                  cidade: dadosFormulario.cidade || null,
                  uf: dadosFormulario.uf || null,
                  cep: extrairApenasDigitos(dadosFormulario.cep) || null,
                }
              : null,
        };

        void pacoteDeDadosEnvio;
        throw new TypeError("Formulário de Empregado está em modo somente leitura. Use Pessoas e vínculos.");

        notificar(
          "success",
          idUsuario
            ? "As informações do colaborador foram atualizadas com sucesso!"
            : "O novo colaborador foi cadastrado no sistema!",
        );
        aoTerSucesso?.();
      } catch (erroRequisicao) {
        notificar(
          "error",
          erroRequisicao?.message ||
            "Ocorreu um erro ao tentar processar a requisição no servidor.",
        );
      } finally {
        setIndicadorCarregamento(false);
      }
    },
    [dadosFormulario, idUsuario, aoTerSucesso, statusAtualEstaInativo],
  );

  const acoesMemoizadas = useMemo(
    () => ({
      aoLidarComMudancaEntrada,
      aoAlternarBloqueioCampo,
      verificarCampoEstaHabilitado,
      aoMudarValorTelefone,
      gerenciarListaDeTelefones,
      alternarModoEdicaoDoTelefone,
      aoAlternarSelecaoDoContrato,
      aoSubmeterFormularioServidor,
      exibirDescricaoTurnoComCargaHoraria,
    }),
    [
      aoLidarComMudancaEntrada,
      aoAlternarBloqueioCampo,
      verificarCampoEstaHabilitado,
      aoMudarValorTelefone,
      gerenciarListaDeTelefones,
      alternarModoEdicaoDoTelefone,
      aoAlternarSelecaoDoContrato,
      aoSubmeterFormularioServidor,
      exibirDescricaoTurnoComCargaHoraria,
    ],
  );

  const estadoMemoizado = useMemo(
    () => ({
      indicadorCarregamento,
      listasAuxiliares,
      dadosFormulario,
      listaTelefonesEditaveis,
    }),
    [
      indicadorCarregamento,
      listasAuxiliares,
      dadosFormulario,
      listaTelefonesEditaveis,
    ],
  );

  const derivadosMemoizados = useMemo(
    () => ({
      turnosDisponiveisFiltrados,
      agenciasBancariasFiltradas,
      resumoValorHoraContrato,
      valorSalarioPrevisto,
      statusAtualEstaInativo,
    }),
    [
      turnosDisponiveisFiltrados,
      agenciasBancariasFiltradas,
      resumoValorHoraContrato,
      valorSalarioPrevisto,
      statusAtualEstaInativo,
    ],
  );

  return {
    estado: estadoMemoizado,
    derivados: derivadosMemoizados,
    acoes: acoesMemoizadas,
  };
};
