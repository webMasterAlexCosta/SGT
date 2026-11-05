import React, { useEffect, useMemo, useState } from "react";
import { CONFIG } from "../../../../core/config.js";
import { notificar } from "../../../../core/events.js";
import { ServicoAuditoria } from "../../../../services/admin/ServicoAuditoria.js";
import { CabecalhoAuditoria } from "./components/CabecalhoAuditoria/index.jsx";
import { FiltrosAuditoria } from "./components/FiltrosAuditoria/index.jsx";
import { ModalDetalhesAuditoria } from "./components/ModalDetalhesAuditoria/index.jsx";
import { TabelaAuditoria } from "./components/TabelaAuditoria/index.jsx";
import estilos from "./index.module.css";

const filtrosIniciais = Object.freeze({
  action: "",
  username: "",
  entityType: "",
  entityId: "",
  requestId: "",
  startDate: "",
  endDate: "",
});

export const PaginaRegistrosAuditoria = () => {
  const [filtros, setFiltros] = useState({ ...filtrosIniciais });
  const [pagina, setPagina] = useState(0);
  const [tamanho, setTamanho] = useState(CONFIG.AUDIT_DEFAULT_SIZE);
  const [carregando, setCarregando] = useState(false);
  const [dados, setDados] = useState({ content: [], totalElements: 0, totalPages: 0, number: 0 });
  const [selecionado, setSelecionado] = useState(null);

  const parametros = useMemo(() => {
    const resultado = { page: pagina, size: tamanho, sort: "createdAt,desc" };
    ["action", "username", "entityType", "requestId"].forEach((chave) => {
      const valor = String(filtros[chave] || "").trim();
      if (valor) resultado[chave] = valor;
    });
    const entityId = Number(filtros.entityId);
    if (filtros.entityId && Number.isFinite(entityId)) resultado.entityId = entityId;
    if (filtros.startDate) resultado.startDate = new Date(`${filtros.startDate}T00:00:00`).toISOString();
    if (filtros.endDate) resultado.endDate = new Date(`${filtros.endDate}T23:59:59.999`).toISOString();
    return resultado;
  }, [filtros, pagina, tamanho]);

  const carregarDados = async (opcoes = {}, parametrosForcados = parametros) => {
    setCarregando(true);
    try {
      const resposta = await ServicoAuditoria.buscar(parametrosForcados, opcoes);
      setDados(resposta || { content: [], totalElements: 0, totalPages: 0, number: 0 });
    } catch (erro) {
      if (!erro?.canceled) console.error("Erro ao carregar logs", erro);
    } finally {
      setCarregando(false);
    }
  };

  useEffect(() => {
    const controlador = new AbortController();
    carregarDados({ signal: controlador.signal });
    return () => controlador.abort();
    // Filtros são aplicados somente pelo botão Buscar.
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [pagina, tamanho]);

  const pesquisar = () => {
    if (pagina !== 0) setPagina(0);
    else carregarDados();
  };

  const limpar = () => {
    setFiltros({ ...filtrosIniciais });
    setPagina(0);
    carregarDados({}, { page: 0, size: tamanho, sort: "createdAt,desc" });
  };

  const abrirDetalhes = async (registro) => {
    if (!registro?.id) return;
    try {
      const detalhe = await ServicoAuditoria.buscarPorId(registro.id, { silentError: true });
      setSelecionado(detalhe || null);
    } catch (erro) {
      notificar("error", erro?.message || "Não foi possível consultar o detalhe autoritativo do registro de auditoria.");
    }
  };

  return (
    <div className={estilos.containerPagina}>
      <CabecalhoAuditoria registros={dados.content || []} carregando={carregando} aoAtualizar={() => carregarDados()} />
      <FiltrosAuditoria filtros={filtros} setFiltros={setFiltros} aoBuscar={pesquisar} aoLimpar={limpar} />
      <TabelaAuditoria dados={dados} carregando={carregando} pagina={pagina} setPagina={setPagina} tamanho={tamanho} setTamanho={setTamanho} aoSelecionar={abrirDetalhes} />
      <ModalDetalhesAuditoria registro={selecionado} aoFechar={() => setSelecionado(null)} />
    </div>
  );
};
