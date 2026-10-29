import React, { createContext, useCallback, useContext, useEffect, useMemo, useRef, useState } from "react";
import { ServicoExportacaoRelatorio } from "./ServicoExportacaoRelatorio";
import { PainelExportacoes } from "./PainelExportacoes";

const CHAVE_SESSAO = "sgt_exportacoes_relatorios";
const INTERVALO_ATUALIZACAO_MS = 1500;
const LIMITE_HISTORICO = 5;

const ContextoExportacaoRelatorio = createContext(null);

const lerIdsPersistidos = () => {
  try {
    const valor = JSON.parse(sessionStorage.getItem(CHAVE_SESSAO) || "[]");
    return Array.isArray(valor) ? valor.filter(Boolean).slice(0, LIMITE_HISTORICO) : [];
  } catch {
    return [];
  }
};

const persistirIds = (tarefas) => {
  try {
    const ids = tarefas.map((item) => item?.id).filter(Boolean).slice(0, LIMITE_HISTORICO);
    sessionStorage.setItem(CHAVE_SESSAO, JSON.stringify(ids));
  } catch {}
};

const iniciarDownload = (url, id) => {
  const iframe = document.createElement("iframe");
  iframe.src = url;
  iframe.title = `download-relatorio-${id || "arquivo"}`;
  iframe.setAttribute("aria-hidden", "true");
  iframe.tabIndex = -1;
  iframe.style.display = "none";
  document.body.appendChild(iframe);
  return iframe;
};

const solicitadores = {
  frequencia: ServicoExportacaoRelatorio.solicitarFrequencia,
  absenteismo: ServicoExportacaoRelatorio.solicitarAbsenteismo,
  terceirizados: ServicoExportacaoRelatorio.solicitarTerceirizados,
};

export const ProvedorExportacaoRelatorio = ({ children }) => {
  const [tarefas, setTarefas] = useState([]);
  const [minimizado, setMinimizado] = useState(false);
  const downloadsIniciados = useRef(new Set());
  const iframesDownload = useRef(new Map());
  const solicitacoesEmAndamento = useRef(new Map());
  const restaurouSessao = useRef(false);

  const atualizarTarefa = useCallback((status) => {
    if (!status?.id) return;
    setTarefas((atuais) => {
      const existe = atuais.some((item) => item.id === status.id);
      const proximas = existe
        ? atuais.map((item) => item.id === status.id ? { ...item, ...status } : item)
        : [{ ...status }, ...atuais];
      persistirIds(proximas);
      return proximas.slice(0, LIMITE_HISTORICO);
    });
  }, []);

  const removerTarefa = useCallback((id) => {
    setTarefas((atuais) => {
      const proximas = atuais.filter((item) => item.id !== id);
      persistirIds(proximas);
      return proximas;
    });
    downloadsIniciados.current.delete(id);
    const iframe = iframesDownload.current.get(id);
    if (iframe) {
      iframe.remove();
      iframesDownload.current.delete(id);
    }
  }, []);

  const consultarUma = useCallback(async (id) => {
    try {
      const status = await ServicoExportacaoRelatorio.consultar(id);
      atualizarTarefa(status);
      if (status?.status === "BAIXADO") {
        const iframe = iframesDownload.current.get(id);
        if (iframe) {
          iframe.remove();
          iframesDownload.current.delete(id);
        }
      }
      return status;
    } catch (erro) {
      if (erro?.status === 404 || erro?.status === 403) removerTarefa(id);
      return null;
    }
  }, [atualizarTarefa, removerTarefa]);

  useEffect(() => {
    if (restaurouSessao.current) return;
    restaurouSessao.current = true;
    const ids = lerIdsPersistidos();
    ids.forEach((id) => consultarUma(id));
  }, [consultarUma]);

  useEffect(() => {
    const ativas = tarefas.filter((item) => !item?.falhou && item?.status !== "BAIXADO");
    if (!ativas.length) return undefined;

    const timer = window.setInterval(() => {
      ativas.forEach((item) => consultarUma(item.id));
    }, INTERVALO_ATUALIZACAO_MS);

    return () => window.clearInterval(timer);
  }, [tarefas, consultarUma]);

  const solicitarDownload = useCallback((tarefa, forcar = false) => {
    if (!tarefa?.id || !tarefa?.podeBaixar) return;
    if (!forcar && downloadsIniciados.current.has(tarefa.id)) return;

    const anterior = iframesDownload.current.get(tarefa.id);
    if (anterior) anterior.remove();

    downloadsIniciados.current.add(tarefa.id);
    const iframe = iniciarDownload(ServicoExportacaoRelatorio.obterUrlDownload(tarefa.id), tarefa.id);
    iframesDownload.current.set(tarefa.id, iframe);

    atualizarTarefa({
      ...tarefa,
      status: "BAIXANDO",
      etapa: "Transferindo arquivo",
      mensagem: "O relatório está pronto e foi enviado ao gerenciador de downloads do navegador.",
      podeBaixar: false,
    });
  }, [atualizarTarefa]);

  useEffect(() => {
    tarefas.forEach((tarefa) => {
      if (!tarefa?.podeBaixar || downloadsIniciados.current.has(tarefa.id)) return;
      window.setTimeout(() => solicitarDownload(tarefa), 500);
    });
  }, [tarefas, solicitarDownload]);

  const tentarDownloadNovamente = useCallback((id) => {
    const tarefa = tarefas.find((item) => item?.id === id);
    if (!tarefa?.podeBaixar) return;
    downloadsIniciados.current.delete(id);
    solicitarDownload(tarefa, true);
  }, [tarefas, solicitarDownload]);

  const iniciarExportacao = useCallback(async ({ tipo, parametros = {}, formato = "XLSX", totalEstimado = 0 }) => {
    const solicitar = solicitadores[tipo];
    if (!solicitar) throw new Error("Tipo de exportação não reconhecido.");

    // O Java cria um job a cada POST de solicitacao e nao recebe uma chave de
    // idempotencia nesses endpoints. Deduplicamos somente requisicoes
    // simultaneas identicas (ex.: duplo clique) e preservamos o ID retornado.
    const chaveSolicitacao = JSON.stringify({ tipo, formato: String(formato || "XLSX").toUpperCase(), parametros, totalEstimado });
    const existente = solicitacoesEmAndamento.current.get(chaveSolicitacao);
    if (existente) return existente;

    setMinimizado(false);
    const promessa = (async () => {
      try {
        const status = await solicitar(parametros, formato, totalEstimado);
        atualizarTarefa(status);
        return status;
      } catch (erro) {
        const id = `local-${Date.now()}`;
        atualizarTarefa({
          id,
          relatorio: "Relatório",
          formato: String(formato || "XLSX").toUpperCase(),
          status: "FALHA",
          etapa: "Não foi possível iniciar",
          mensagem: erro?.message || "O servidor não aceitou a solicitação de exportação.",
          processados: 0,
          totalEstimado: Number(totalEstimado || 0),
          percentual: 0,
          segundosDecorridos: 0,
          segundosRestantesEstimados: null,
          falhou: true,
          concluido: false,
          podeBaixar: false,
        });
        throw erro;
      } finally {
        solicitacoesEmAndamento.current.delete(chaveSolicitacao);
      }
    })();
    solicitacoesEmAndamento.current.set(chaveSolicitacao, promessa);
    return promessa;
  }, [atualizarTarefa]);

  const valor = useMemo(() => ({ iniciarExportacao, tarefas }), [iniciarExportacao, tarefas]);

  return (
    <ContextoExportacaoRelatorio.Provider value={valor}>
      {children}
      <PainelExportacoes
        tarefas={tarefas}
        minimizado={minimizado}
        aoAlternarMinimizado={() => setMinimizado((valorAtual) => !valorAtual)}
        aoFechar={removerTarefa}
        aoBaixarNovamente={tentarDownloadNovamente}
      />
    </ContextoExportacaoRelatorio.Provider>
  );
};

export const useExportacaoRelatorio = () => {
  const contexto = useContext(ContextoExportacaoRelatorio);
  if (!contexto) throw new Error("useExportacaoRelatorio deve ser usado dentro de ProvedorExportacaoRelatorio.");
  return contexto;
};
