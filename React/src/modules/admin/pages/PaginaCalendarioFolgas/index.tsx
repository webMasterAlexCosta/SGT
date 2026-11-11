import { observarFalha } from '../../../../core/http/falhaObservada';
import React, { useEffect, useMemo, useRef, useState } from "react";
import { notificar } from "../../../../core/events.ts";
import { useConfirmacaoSenha } from "../../../../core/security/useConfirmacaoSenha.tsx";
import { useAutenticacao } from "../../../auth/context.tsx";
import { eGestaoLocal, podeGerenciarCalendario, possuiAcessoGlobal } from "../../../../core/security/roles.ts";
import { ServicoCalendarioEvento } from "../../../../services/rh/ServicoFrequencia.ts";
import { ServicoLotacao } from "../../../../services/admin/ServicoLotacao.ts";
import { ServicoOrganizacional } from "../../../../services/admin/ServicoOrganizacional.ts";
import { CONFIG } from "../../../../core/config.ts";
import { formatarGestorEstadual, formatarGestaoLocal } from "../../../../core/formatters/nomenclaturaGestao.ts";
import { obterDataNoFusoNegocio, obterPeriodoMensalNoFusoNegocio } from "../../../../core/formatters/dataHoraNegocio.ts";
import estilos from "./index.module.css";
import { CabecalhoCalendario } from "./components/CabecalhoCalendario/index.tsx";
import { IndicadoresCalendario } from "./components/IndicadoresCalendario/index.tsx";
import { PainelEventos } from "./components/PainelEventos/index.tsx";
import { ModalEvento } from "./components/ModalEvento/index.tsx";

const obterISODataHoje = () => obterDataNoFusoNegocio() || "";
const obterPeriodoMensal = () => obterPeriodoMensalNoFusoNegocio() || { start: "", end: "" };

const criarFormularioInicial = (usuario: (import("../../../../contracts/auth").AuthenticatedUser | null)) => ({
  data: obterISODataHoje(),
  tipo: "FERIADO",
  descricao: "",
  abrangencia: possuiAcessoGlobal(usuario) ? "NACIONAL" : "ESTADUAL",
  superintendenciaId: usuario?.superintendenciaId || "",
  gestaoLocalId: usuario?.gestaoLocalId || "",
  municipio: "",
  lotacaoId: "",
  ativo: true,
});

export const PaginaCalendarioFolgas = () => {
  const referenciaControlador = useRef<(AbortController) | null>(null);
  const { user: usuario } = useAutenticacao();
  const { confirmarComSenha, ModalConfirmacao } = useConfirmacaoSenha();

  const periodoInicial = useMemo(() => obterPeriodoMensal(), []);
  const [dataInicio, setDataInicio] = useState(periodoInicial.start);
  const [dataFim, setDataFim] = useState(periodoInicial.end);
  const [eventos, setEventos] = useState<(import("../../../../contracts/opcoes-apresentacao").CalendarEventView[])>([]);
  const [lotacoes, setLotacoes] = useState<(import("../../../../contracts/opcoes-apresentacao").LocationView[])>([]);
  const [gestoesLocais, setGestoesLocais] = useState<(import("../../../../contracts/opcoes-apresentacao").LocalManagementView[])>([]);
  const [carregando, setCarregando] = useState(false);
  const [modalAberto, setModalAberto] = useState(false);
  const [idEdicao, setIdEdicao] = useState<number | null>(null);
  const [dadosFormulario, setDadosFormulario] = useState(() => criarFormularioInicial(usuario));

  const acessoGlobal = possuiAcessoGlobal(usuario);
  const perfilGestaoLocal = eGestaoLocal(usuario);
  const podeEditar = podeGerenciarCalendario(usuario);

  const fecharModal = () => {
    setModalAberto(false);
    setIdEdicao(null);
    setDadosFormulario(criarFormularioInicial(usuario));
  };

  const carregarDados = async (opcoes = {}) => {
    setCarregando(true);
    try {
      const [resEventos, resLotacoes, resGestoesLocais] = await Promise.all([
        ServicoCalendarioEvento.listarPorPeriodo(dataInicio, dataFim, opcoes),
        ServicoLotacao.listar({ size: CONFIG.PAGINATION_MAX_SIZE }, opcoes),
        acessoGlobal ? ServicoOrganizacional.listarGestoesLocais({}, opcoes) : Promise.resolve([]),
      ]);
      const listaEventos = resEventos;
      const listaLotacoes = resLotacoes;

      setEventos(
        listaEventos
          .slice()
          .sort((a, b) => String(a?.data || "").localeCompare(String(b?.data || "")) || String(a?.tipo || "").localeCompare(String(b?.tipo || ""))),
      );
      setLotacoes(listaLotacoes);
      setGestoesLocais(resGestoesLocais);
    } catch (erroOriginal) {
      const erro = observarFalha(erroOriginal);

      if (erro?.canceled) return;
      console.error(erroOriginal);
      notificar("error", "Falha ao carregar o calendário da sua unidade de gestão.");
    } finally {
      setCarregando(false);
    }
  };

  useEffect(() => {
    const controlador = new AbortController();
    referenciaControlador.current = controlador;
    carregarDados({ signal: controlador.signal });
    return () => controlador.abort();
    // O período é atualizado pelo botão para evitar requisições a cada digitação.
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  const lotacoesDisponiveis = useMemo(() => {
    if (!dadosFormulario.gestaoLocalId) return perfilGestaoLocal ? lotacoes : [];
    return lotacoes.filter((lotacao) => String(lotacao.gestaoLocalId || lotacao.gestaoLocal?.id) === String(dadosFormulario.gestaoLocalId));
  }, [dadosFormulario.gestaoLocalId, lotacoes, perfilGestaoLocal]);

  const municipiosDisponiveis = useMemo(() =>
    Array.from(new Set(
      lotacoesDisponiveis
        .map((lotacao) => String(lotacao?.municipio || "").trim())
        .filter(Boolean),
    )).sort((a, b) => a.localeCompare(b, "pt-BR")),
  [lotacoesDisponiveis]);

  const alterarAbrangencia = (abrangencia: ReturnType<typeof criarFormularioInicial>["abrangencia"]) => {
    setDadosFormulario((atual) => ({
      ...atual,
      abrangencia,
      gestaoLocalId: abrangencia === "NACIONAL" ? "" : atual.gestaoLocalId || usuario?.gestaoLocalId || "",
      superintendenciaId: abrangencia === "NACIONAL" ? "" : atual.superintendenciaId || usuario?.superintendenciaId || "",
      municipio: ["NACIONAL", "ESTADUAL"].includes(abrangencia) ? "" : atual.municipio,
      lotacaoId: abrangencia === "LOTACAO" ? atual.lotacaoId : "",
    }));
  };

  const alterarGestaoLocal = (gestaoLocalId: string) => {
    const gestaoLocal = gestoesLocais.find((item) => String(item.id) === String(gestaoLocalId));
    setDadosFormulario((atual) => ({
      ...atual,
      gestaoLocalId,
      superintendenciaId: gestaoLocal?.superintendenciaId || gestaoLocal?.superintendencia?.id || "",
      lotacaoId: "",
      municipio: "",
    }));
  };

  const alterarLotacao = (lotacaoId: string) => {
    const lotacao = lotacoesDisponiveis.find((item) => String(item.id) === String(lotacaoId));
    setDadosFormulario((atual) => ({
      ...atual,
      lotacaoId,
      municipio: lotacao?.municipio || atual.municipio || "",
    }));
  };

  const abrirNovo = () => {
    setIdEdicao(null);
    setDadosFormulario(criarFormularioInicial(usuario));
    setModalAberto(true);
  };

  const abrirEdicao = (item: import("../../../../contracts/opcoes-apresentacao").CalendarEventView) => {
    setIdEdicao(item.id);
    setDadosFormulario({
      data: item?.data || obterISODataHoje(),
      tipo: String(item?.tipo || "FERIADO").toUpperCase(),
      descricao: item?.descricao || "",
      abrangencia: String(item?.abrangencia || (item?.gestaoLocal ? "ESTADUAL" : "NACIONAL")).toUpperCase(),
      superintendenciaId: item?.superintendencia?.id || item?.superintendenciaId || "",
      gestaoLocalId: item?.gestaoLocal?.id || item?.gestaoLocalId || "",
      municipio: item?.municipio || "",
      lotacaoId: String(item?.lotacaoId || ""),
      ativo: item?.ativo !== false,
    });
    setModalAberto(true);
  };

  const validarFormulario = () => {
    if (!dadosFormulario.data) return "Informe a data do evento.";
    if (!String(dadosFormulario.tipo || "").trim()) return "Informe o tipo do evento.";
    if (dadosFormulario.abrangencia !== "NACIONAL" && !dadosFormulario.gestaoLocalId && acessoGlobal) {
      return "Selecione a Gestão Local responsável pelo evento regional.";
    }
    if (dadosFormulario.abrangencia === "MUNICIPAL" && !dadosFormulario.municipio?.trim()) {
      return "Informe o município afetado pelo evento.";
    }
    if (dadosFormulario.abrangencia === "LOTACAO" && !dadosFormulario.lotacaoId) {
      return "Selecione a lotação afetada pelo evento.";
    }
    return null;
  };

  const salvarEvento = async () => {
    const erroValidacao = validarFormulario();
    if (erroValidacao) {
      notificar("warning", erroValidacao);
      return;
    }

    await confirmarComSenha({
      titulo: idEdicao ? "Atualizar evento" : "Cadastrar evento",
      mensagem: "O evento será aplicado somente ao escopo indicado. Informe sua senha para prosseguir.",
      acao: async (senhaConfirmacao) => {
        const cargaEnvio = {
          data: dadosFormulario.data,
          tipo: String(dadosFormulario.tipo || "").trim().toUpperCase(),
          descricao: dadosFormulario.descricao || null,
          abrangencia: dadosFormulario.abrangencia,
          superintendenciaId: dadosFormulario.superintendenciaId ? Number(dadosFormulario.superintendenciaId) : null,
          gestaoLocalId: dadosFormulario.gestaoLocalId ? Number(dadosFormulario.gestaoLocalId) : null,
          municipio: dadosFormulario.abrangencia === "MUNICIPAL" ? dadosFormulario.municipio : null,
          lotacaoId: dadosFormulario.abrangencia === "LOTACAO" ? Number(dadosFormulario.lotacaoId) : null,
          ativo: Boolean(dadosFormulario.ativo),
        };

        if (idEdicao) await ServicoCalendarioEvento.atualizar(idEdicao, cargaEnvio, { senhaConfirmacao });
        else await ServicoCalendarioEvento.criar(cargaEnvio, { senhaConfirmacao });

        fecharModal();
        await carregarDados({ signal: referenciaControlador.current?.signal });
        notificar("success", "Evento salvo no calendário da unidade correta.");
      },
    });
  };

  const excluirEvento = async (idAlvo: number | null) => {
    await confirmarComSenha({
      titulo: "Excluir evento",
      mensagem: "Esta ação não pode ser desfeita. Informe sua senha para prosseguir.",
      acao: async (senhaConfirmacao) => {
        await ServicoCalendarioEvento.excluir(idAlvo, { senhaConfirmacao });
        await carregarDados({ signal: referenciaControlador.current?.signal });
        notificar("success", "Evento removido com sucesso.");
      },
    });
  };

  const indicadores = useMemo(() => ({
    total: eventos.length,
    ativos: eventos.filter((evento) => evento.ativo !== false).length,
    feriados: eventos.filter((evento) => String(evento.tipo).toUpperCase() === "FERIADO").length,
    locais: eventos.filter((evento) => ["MUNICIPAL", "LOTACAO"].includes(String(evento.abrangencia).toUpperCase())).length,
  }), [eventos]);

  const formatarData = (valor: string | null) => {
    if (!valor) return "-";
    const [ano, mes, dia] = String(valor).split("-");
    return ano && mes && dia ? `${dia}/${mes}/${ano}` : valor;
  };

  const obterRotuloEscopo = (evento: import("../../../../contracts/opcoes-apresentacao").CalendarEventView) => {
    const abrangencia = String(evento?.abrangencia || "NACIONAL").toUpperCase();
    if (abrangencia === "NACIONAL") return "Todo o Brasil";
    if (abrangencia === "LOTACAO") {
      const lotacao = lotacoes.find((item) => String(item.id) === String(evento.lotacaoId));
      return lotacao?.nome || `Lotação ${evento.lotacaoId || ""}`;
    }
    if (abrangencia === "MUNICIPAL") return `${evento.municipio || "Município"} · ${evento.uf || ""}`;
    if (evento?.superintendencia || evento?.superintendenciaNome || evento?.uf) {
      return formatarGestorEstadual(evento.superintendencia || evento.superintendenciaNome, evento.uf);
    }
    return evento?.gestaoLocal ? formatarGestaoLocal(evento.gestaoLocal) : "Escopo estadual";
  };

  const identificacaoEscopo = perfilGestaoLocal
    ? `${formatarGestaoLocal(usuario?.gestaoLocal || usuario?.gestaoLocalNome)} · ${formatarGestorEstadual(usuario?.superintendencia || usuario?.superintendenciaNome, usuario?.uf)}`
    : acessoGlobal ? "Visão nacional e calendários regionais" : "Calendário do seu escopo";

  return (
    <div className={estilos.containerPagina}>
      <ModalConfirmacao />

      <CabecalhoCalendario identificacaoEscopo={identificacaoEscopo} aoCriar={abrirNovo} podeEditar={podeEditar} />
      <IndicadoresCalendario indicadores={indicadores} />
      <PainelEventos dataInicio={dataInicio} dataFim={dataFim} aoMudarInicio={setDataInicio} aoMudarFim={setDataFim} aoAtualizar={() => carregarDados({ signal: referenciaControlador.current?.signal })} carregando={carregando} eventos={eventos} formatarData={formatarData} obterRotuloEscopo={obterRotuloEscopo} aoEditar={abrirEdicao} aoExcluir={excluirEvento} podeEditar={podeEditar} />
      {podeEditar && <ModalEvento aberto={modalAberto} editando={Boolean(idEdicao)} dados={dadosFormulario} setDados={setDadosFormulario} acessoGlobal={acessoGlobal} gestoesLocais={gestoesLocais} municipiosDisponiveis={municipiosDisponiveis} lotacoesDisponiveis={lotacoesDisponiveis} aoFechar={fecharModal} aoSalvar={salvarEvento} aoAlterarAbrangencia={alterarAbrangencia} aoAlterarGestaoLocal={alterarGestaoLocal} aoAlterarLotacao={alterarLotacao} />}
    </div>
  );
};
