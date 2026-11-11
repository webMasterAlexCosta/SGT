import { observarFalha } from '../http/falhaObservada';
import type { ExportTask, ExportRequest, ExportContext } from '../../contracts/exportacao';
import type { ExportacaoRelatorioStatusDTO } from '../../contracts/java';
import React, { createContext, useCallback, useContext, useEffect, useMemo, useRef, useState } from "react";
import { ServicoExportacaoRelatorio } from "./ServicoExportacaoRelatorio";
import { PainelExportacoes } from "./PainelExportacoes";
import { criarErroSessaoObsoleta, obterGeracaoSessao, obterSinalSessao, sessaoAtual } from "../security/geracaoSessao.ts";

const CHAVE_SESSAO = "sgt_exportacoes_relatorios";
const INTERVALO_ATUALIZACAO_MS = 1500;
const LIMITE_HISTORICO = 5;

// Only finished history may be evicted. CONCLUIDO/BAIXANDO still require
// tracking until the server confirms the transfer or the user dismisses it.
const limitarHistorico = (tarefas: ExportTask[]) => {
  let terminadas = 0;
  return tarefas.filter(tarefa => {
    const historico = tarefa.falhou || ['BAIXADO', 'CANCELADO', 'FALHA'].includes(tarefa.status ?? '');
    return !historico || ++terminadas <= LIMITE_HISTORICO;
  });
};

const ContextoExportacaoRelatorio = createContext<ExportContext | null>(null);

const lerIdsPersistidos = () => {
  try {
    const valor: unknown = JSON.parse(sessionStorage.getItem(CHAVE_SESSAO) || "[]");
    return Array.isArray(valor) ? [...new Set(valor.filter((id: unknown): id is string => typeof id === "string" && Boolean(id)))] : [];
  } catch {
    return [];
  }
};

const persistirIds = (tarefas: ExportTask[]) => {
  try {
    const ids = limitarHistorico(tarefas).map((item) => item?.id).filter(Boolean);
    sessionStorage.setItem(CHAVE_SESSAO, JSON.stringify(ids));
  } catch {}
};

const iniciarDownload = (url: (string), id: (string)) => {
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

export const ProvedorExportacaoRelatorio = ({ children }: React.PropsWithChildren) => {
  const [tarefas, setTarefas] = useState<ExportTask[]>([]);
  const [minimizado, setMinimizado] = useState(false);
  const [cancelando, setCancelando] = useState<ReadonlySet<string>>(new Set());
  const [errosCancelamento, setErrosCancelamento] = useState<Record<string, string>>({});
  const consultas = useRef(new Map<string, AbortController>());
  const revisoes = useRef(new Map<string, number>());
  const cancelamentos = useRef(new Map<string, { controller: AbortController; promessa: Promise<void> }>());
  const downloadsIniciados = useRef(new Set<string | null>());
  const iframesDownload = useRef(new Map<string | null, HTMLIFrameElement>());
  const solicitacoesEmAndamento = useRef(new Map<string, Promise<ExportacaoRelatorioStatusDTO>>());
  const restaurouSessao = useRef(false);
  const geracaoRef = useRef(obterGeracaoSessao());
  const montadoRef = useRef(true);
  const cicloRef = useRef(0);
  const timersDownload = useRef(new Set<number>());

  const contextoAtual = useCallback((ciclo = cicloRef.current) =>
    montadoRef.current && ciclo === cicloRef.current && sessaoAtual(geracaoRef.current), []);

  useEffect(() => {
    montadoRef.current = true;
    const sinal = obterSinalSessao(geracaoRef.current);
    const limparRecursos = () => {
      timersDownload.current.forEach(timer => window.clearTimeout(timer));
      timersDownload.current.clear();
      iframesDownload.current.forEach(iframe => iframe.remove());
      iframesDownload.current.clear();
      downloadsIniciados.current.clear();
      solicitacoesEmAndamento.current.clear();
      consultas.current.forEach(controller => controller.abort());
      consultas.current.clear();
      cancelamentos.current.forEach(operacao => operacao.controller.abort());
      cancelamentos.current.clear();
      revisoes.current.clear();
    };
    const encerrarContexto = () => {
      montadoRef.current = false;
      cicloRef.current += 1;
      limparRecursos();
      sessionStorage.removeItem(CHAVE_SESSAO);
      setTarefas([]);
      setMinimizado(false);
      setCancelando(new Set());
      setErrosCancelamento({});
    };
    if (sinal.aborted) encerrarContexto();
    else sinal.addEventListener('abort', encerrarContexto, { once: true });
    return () => {
      montadoRef.current = false;
      cicloRef.current += 1;
      sinal.removeEventListener('abort', encerrarContexto);
      limparRecursos();
      restaurouSessao.current = false;
    };
  }, []);

  const atualizarTarefa = useCallback((status: ExportTask, vigente: () => boolean = () => true) => {
    if (!status?.id || !contextoAtual() || !vigente()) return;
    setTarefas((atuais) => {
      if (!contextoAtual() || !vigente()) return atuais;
      if (atuais.some(item => item.id === status.id && item.status === 'CANCELADO') && status.status !== 'CANCELADO') return atuais;
      const existe = atuais.some((item) => item.id === status.id);
      const proximas = existe
        ? atuais.map((item) => item.id === status.id ? { ...item, ...status } : item)
        : [{ ...status }, ...atuais];
      const acompanhadas = limitarHistorico(proximas);
      persistirIds(acompanhadas);
      return acompanhadas;
    });
  }, [contextoAtual]);

  const removerTarefa = useCallback((id: (string | null)) => {
    if (!contextoAtual()) return;
    if (id) {
      revisoes.current.set(id, (revisoes.current.get(id) ?? 0) + 1);
      consultas.current.get(id)?.abort();
      consultas.current.delete(id);
    }
    setTarefas((atuais) => {
      if (!contextoAtual()) return atuais;
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
  }, [contextoAtual]);

  const consultarUma = useCallback(async (id: (string | null)) => {
    const ciclo = cicloRef.current;
    if (!id || !contextoAtual(ciclo) || consultas.current.has(id) || cancelamentos.current.has(id)) return null;
    const controller = new AbortController();
    const revisao = revisoes.current.get(id) ?? 0;
    consultas.current.set(id, controller);
    const vigente = () => contextoAtual(ciclo) && !controller.signal.aborted
      && (revisoes.current.get(id) ?? 0) === revisao && !cancelamentos.current.has(id);
    try {
      const status = await ServicoExportacaoRelatorio.consultar(id, { signal: controller.signal });
      if (!vigente()) return null;
      atualizarTarefa(status, vigente);
      if (status?.status === "BAIXADO") {
        const iframe = iframesDownload.current.get(id);
        if (iframe) {
          iframe.remove();
          iframesDownload.current.delete(id);
        }
      }
      return status;
    } catch (erroOriginal) {
      const erro = observarFalha(erroOriginal);

      if (!vigente() || erro?.canceled) return null;
      if (erro?.status === 404 || erro?.status === 403) removerTarefa(id);
      return null;
    } finally {
      if (consultas.current.get(id) === controller) consultas.current.delete(id);
    }
  }, [atualizarTarefa, removerTarefa, contextoAtual]);

  useEffect(() => {
    if (restaurouSessao.current) return;
    restaurouSessao.current = true;
    const ids = lerIdsPersistidos();
    ids.forEach((id) => consultarUma(id));
  }, [consultarUma]);

  useEffect(() => {
    const ativas = tarefas.filter((item) => !item?.falhou && !['BAIXADO', 'CANCELADO', 'FALHA'].includes(item.status ?? ''));
    if (!ativas.length) return undefined;

    const timer = window.setInterval(() => {
      ativas.forEach((item) => consultarUma(item.id));
    }, INTERVALO_ATUALIZACAO_MS);

    return () => window.clearInterval(timer);
  }, [tarefas, consultarUma]);

  const solicitarDownload = useCallback((tarefa: ExportTask, forcar = false) => {
    if (!tarefa?.id || tarefa.status === 'CANCELADO' || !tarefa?.podeBaixar || !contextoAtual() || cancelamentos.current.has(tarefa.id)) return;
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
  }, [atualizarTarefa, contextoAtual]);

  useEffect(() => {
    const ciclo = cicloRef.current;
    const temporizadores: number[] = [];
    tarefas.forEach((tarefa) => {
      if (!tarefa.id || tarefa.status === 'CANCELADO' || !tarefa?.podeBaixar || downloadsIniciados.current.has(tarefa.id) || cancelamentos.current.has(tarefa.id)) return;
      const revisao = revisoes.current.get(tarefa.id) ?? 0;
      const timer = window.setTimeout(() => {
        timersDownload.current.delete(timer);
        if (contextoAtual(ciclo) && (revisoes.current.get(tarefa.id ?? '') ?? 0) === revisao) solicitarDownload(tarefa);
      }, 500);
      temporizadores.push(timer);
      timersDownload.current.add(timer);
    });
    return () => temporizadores.forEach(timer => {
      window.clearTimeout(timer);
      timersDownload.current.delete(timer);
    });
  }, [tarefas, cancelando, solicitarDownload, contextoAtual]);

  const cancelarExportacao = useCallback((id: string | null): Promise<void> => {
    if (!id || !contextoAtual()) return Promise.resolve();
    const existente = cancelamentos.current.get(id);
    if (existente) return existente.promessa;
    const tarefa = tarefas.find(item => item.id === id);
    if (!tarefa || !['NA_FILA', 'PROCESSANDO'].includes(tarefa.status ?? '')) return Promise.resolve();
    const ciclo = cicloRef.current;
    const revisao = (revisoes.current.get(id) ?? 0) + 1;
    revisoes.current.set(id, revisao);
    consultas.current.get(id)?.abort();
    consultas.current.delete(id);
    const operacao = { controller: new AbortController(), promessa: Promise.resolve() };
    cancelamentos.current.set(id, operacao);
    const vigente = () => contextoAtual(ciclo) && !operacao.controller.signal.aborted && revisoes.current.get(id) === revisao;
    setCancelando(atuais => new Set([...atuais, id]));
    setErrosCancelamento(atuais => ({ ...atuais, [id]: '' }));
    operacao.promessa = (async () => {
      try {
        const status = await ServicoExportacaoRelatorio.cancelar(id, { signal: operacao.controller.signal });
        if (!vigente()) return;
        atualizarTarefa(status, vigente);
      } catch (falhaOriginal) {
        const falha = observarFalha(falhaOriginal);
        if (vigente() && !falha.canceled) setErrosCancelamento(atuais => ({ ...atuais, [id]: falha.message || 'Não foi possível cancelar. Consulte o estado atual e tente novamente.' }));
      } finally {
        if (cancelamentos.current.get(id) === operacao) cancelamentos.current.delete(id);
        if (vigente()) setCancelando(atuais => { const proximos = new Set(atuais); proximos.delete(id); return proximos; });
      }
    })();
    return operacao.promessa;
  }, [tarefas, contextoAtual, atualizarTarefa]);

  const tentarDownloadNovamente = useCallback((id: string | null) => {
    const tarefa = tarefas.find((item) => item?.id === id);
    if (!tarefa?.podeBaixar) return;
    downloadsIniciados.current.delete(id);
    solicitarDownload(tarefa, true);
  }, [tarefas, solicitarDownload]);

  const iniciarExportacao = useCallback(async ({ tipo, parametros = {}, formato = "XLSX", totalEstimado = 0 }: ExportRequest) => {
    const ciclo = cicloRef.current;
    if (!contextoAtual(ciclo)) throw criarErroSessaoObsoleta();
    const solicitar = solicitadores[tipo];
    if (!solicitar) throw new Error("Tipo de exportação não reconhecido.");

    // O Java cria um job a cada POST de solicitacao e nao recebe uma chave de
    // idempotencia nesses endpoints. Deduplicamos somente requisicoes
    // simultaneas identicas (ex.: duplo clique) e preservamos o ID retornado.
    const chaveSolicitacao = JSON.stringify({ tipo, formato: String(formato || "XLSX").toUpperCase(), parametros, totalEstimado });
    const existente = solicitacoesEmAndamento.current.get(chaveSolicitacao);
    if (existente) return existente;

    setMinimizado(false);
    let promessa: Promise<ExportacaoRelatorioStatusDTO> | null = null;
    promessa = (async () => {
      try {
        const status = await solicitar(parametros, formato, totalEstimado);
        if (!contextoAtual(ciclo)) throw criarErroSessaoObsoleta();
        atualizarTarefa(status);
        return status;
      } catch (erroOriginal) {
      const erro = observarFalha(erroOriginal);

        if (!contextoAtual(ciclo) || erro?.canceled) throw criarErroSessaoObsoleta();
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
        throw erroOriginal;
      } finally {
        if (solicitacoesEmAndamento.current.get(chaveSolicitacao) === promessa) solicitacoesEmAndamento.current.delete(chaveSolicitacao);
      }
    })();
    solicitacoesEmAndamento.current.set(chaveSolicitacao, promessa);
    return promessa;
  }, [atualizarTarefa, contextoAtual]);

  const valor = useMemo(() => ({ iniciarExportacao, cancelarExportacao, tarefas }), [iniciarExportacao, cancelarExportacao, tarefas]);

  return (
    <ContextoExportacaoRelatorio.Provider value={valor}>
      {children}
      <PainelExportacoes
        tarefas={tarefas}
        minimizado={minimizado}
        aoAlternarMinimizado={() => setMinimizado((valorAtual) => !valorAtual)}
        aoFechar={removerTarefa}
        aoBaixarNovamente={tentarDownloadNovamente}
        aoCancelar={cancelarExportacao}
        cancelando={cancelando}
        errosCancelamento={errosCancelamento}
      />
    </ContextoExportacaoRelatorio.Provider>
  );
};

export const useExportacaoRelatorio = () => {
  const contexto = useContext(ContextoExportacaoRelatorio);
  if (!contexto) throw new Error("useExportacaoRelatorio deve ser usado dentro de ProvedorExportacaoRelatorio.");
  return contexto;
};
