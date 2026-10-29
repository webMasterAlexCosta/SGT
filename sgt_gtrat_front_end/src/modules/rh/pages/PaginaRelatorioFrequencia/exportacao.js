import { notificar } from "../../../../core/events";
import { ServicoFrequencia, iniciarDownloadNativo } from "../../../../services/rh/ServicoFrequencia";

const parametrosServidor = ({
  dataInicio,
  dataFim,
  contratosSelecionados = [],
  lotacaoSelecionada,
  turnoSelecionado,
}) => ({
  inicio: dataInicio,
  fim: dataFim,
  ...(contratosSelecionados.length ? { contratoIds: contratosSelecionados } : {}),
  ...(lotacaoSelecionada ? { lotacaoId: lotacaoSelecionada } : {}),
  ...(turnoSelecionado ? { jornadaId: turnoSelecionado } : {}),
});

const iniciar = async (parametros, formato, iniciarExportacao) => {
  try {
    if (typeof iniciarExportacao === "function") {
      await iniciarExportacao({
        tipo: "frequencia",
        parametros: parametrosServidor(parametros),
        formato,
        totalEstimado: Number(parametros?.totalColaboradores || 0),
      });
      return;
    }

    iniciarDownloadNativo(
      ServicoFrequencia.obterUrlExportacaoFrequencia(parametrosServidor(parametros), formato)
    );
  } catch (erro) {
    console.error(`Erro ao iniciar exportação ${formato}`, erro);
    notificar("error", `Não foi possível iniciar a exportação ${formato}.`);
  }
};

export const gerarPlanilhaExcel = (parametros, iniciarExportacao) =>
  iniciar(parametros, "XLSX", iniciarExportacao);

export const gerarDocumentoPDF = (parametros, iniciarExportacao) =>
  iniciar(parametros, "PDF", iniciarExportacao);