import { notificar } from "../../../../core/events";
import { baixarArquivoXlsx, celulaXlsx } from "../../../../core/export/xlsx";

const CORES = Object.freeze({
  azul: "FF00416B",
  azulClaro: "FFE7F2F8",
  amarelo: "FFFFD400",
  branco: "FFFFFFFF",
});

const estiloTitulo = { font: { bold: true, size: 17, color: CORES.branco }, fillColor: CORES.azul, alignment: { vertical: "center" }, border: true };
const estiloSubtitulo = { font: { bold: true, size: 11, color: CORES.azul }, fillColor: CORES.amarelo, alignment: { vertical: "center", wrapText: true }, border: true };
const estiloSecao = { font: { bold: true, size: 12, color: CORES.branco }, fillColor: CORES.azul, alignment: { vertical: "center" }, border: true };
const estiloCabecalho = { font: { bold: true, color: CORES.azul }, fillColor: CORES.azulClaro, alignment: { vertical: "center", wrapText: true }, border: true };
const estiloTexto = { alignment: { vertical: "center", wrapText: true }, border: true };
const estiloInteiro = { numberFormat: "#,##0", border: true };
const estiloPercentual = { numberFormat: "0.00%", border: true };

const celula = (value, style = estiloTexto) => celulaXlsx(value, style);
const inteiro = (value) => celulaXlsx(Number(value || 0), estiloInteiro);
const pct = (value) => celulaXlsx(Number(value || 0) / 100, estiloPercentual);
const lista = (value) => Array.isArray(value) ? value : [];

const dataPtBr = (valor) => {
  if (!valor) return "—";
  const data = new Date(`${String(valor).slice(0, 10)}T12:00:00`);
  return Number.isNaN(data.getTime()) ? String(valor) : new Intl.DateTimeFormat("pt-BR").format(data);
};

const nomeSeguro = (texto) => String(texto || "indicador")
  .normalize("NFD")
  .replace(/[\u0300-\u036f]/g, "")
  .replace(/[^a-zA-Z0-9_-]+/g, "_")
  .replace(/^_+|_+$/g, "")
  .toLowerCase();

const planilhaContratos = (itens, referencia) => ({
  name: "Contratos",
  columns: [{ width: 34 }, ...Array.from({ length: 9 }, () => ({ width: 18 }))],
  rows: [
    { cells: [celulaXlsx(`CONTRATOS — ${dataPtBr(referencia)}`, estiloSecao)], height: 24 },
    { cells: ["Contrato", "Previsto", "Alocados", "Lançados", "Pendentes", "Presentes", "Ausentes", "Déficit contratual", "% Cobertura", "% Atendimento"].map((item) => celulaXlsx(item, estiloCabecalho)) },
    ...lista(itens).map((item) => ({ cells: [
      celula(item.nome || "Não informado"),
      inteiro(item.efetivoPrevisto),
      inteiro(item.efetivoAlocado),
      inteiro(item.registrosLancados),
      inteiro(item.pendentesLancamento),
      inteiro(item.presentes),
      inteiro(item.ausentes),
      inteiro(item.deficitContratual),
      pct(item.percentualCobertura),
      pct(item.percentualAtendimento),
    ] })),
  ],
  merges: ["A1:J1"],
  freezeRows: 2,
});

const planilhaDimensao = (nome, itens, referencia) => ({
  name: nome,
  columns: [{ width: 38 }, { width: 16 }, { width: 16 }, { width: 16 }, { width: 16 }, { width: 16 }, { width: 18 }],
  rows: [
    { cells: [celulaXlsx(`${nome.toUpperCase()} — ${dataPtBr(referencia)}`, estiloSecao)], height: 24 },
    { cells: ["Descrição", "Alocados", "Lançados", "Pendentes", "Presentes", "Ausentes", "% Cobertura"].map((item) => celulaXlsx(item, estiloCabecalho)) },
    ...lista(itens).map((item) => ({ cells: [
      celula(item.nome || "Não informado"),
      inteiro(item.efetivoAlocado),
      inteiro(item.registrosLancados),
      inteiro(item.pendentesLancamento),
      inteiro(item.presentes),
      inteiro(item.ausentes),
      pct(item.percentualCobertura),
    ] })),
  ],
  merges: ["A1:G1"],
  freezeRows: 2,
});

const criarConfiguracao = (dados, contexto) => {
  const resumo = dados?.resumoAtual || {};
  const anterior = dados?.resumoAnoAnterior || null;
  const serieAtual = lista(dados?.serieAtual);
  const serieAnterior = lista(dados?.serieAnoAnterior);
  const anteriorPorIndice = new Map(serieAnterior.map((item, indice) => [indice, item]));
  const quadro = lista(dados?.quadroDimensionado);
  const referencia = contexto.referencia || dados?.fim;

  const sheets = [
    {
      name: "Resumo",
      columns: [{ width: 30 }, { width: 22 }, { width: 22 }, { width: 52 }],
      rows: [
        { cells: [celulaXlsx("INDICADOR CONTRATUAL DE EFETIVO — SGT-GTRAT", estiloTitulo)], height: 28 },
        { cells: [celulaXlsx(`Escopo: ${contexto.escopoSelecionado || "—"}`, estiloSubtitulo)], height: 22 },
        { cells: ["Referência", dataPtBr(referencia), "Período", contexto.periodoRotulo || dados?.periodo || "—"] },
        { cells: ["Intervalo do gráfico", `${dataPtBr(dados?.inicio)} a ${dataPtBr(dados?.fim)}`, "Comparação", contexto.comparar ? "Ano atual × ano anterior" : "Desativada"] },
        { cells: [] },
        { cells: [celulaXlsx("FOTOGRAFIA DA DATA DE REFERÊNCIA", estiloSecao)] },
        { cells: ["Indicador", "Atual", "Ano anterior", "Regra"].map((item) => celulaXlsx(item, estiloCabecalho)) },
        { cells: ["Cobertura dos alocados", pct(resumo.percentualCobertura), anterior ? pct(anterior.percentualCobertura) : celula("—"), celula("Percentual autoritativo devolvido pelo Java")] },
        { cells: ["Atendimento contratual", pct(resumo.percentualAtendimento), anterior ? pct(anterior.percentualAtendimento) : celula("—"), celula("Percentual autoritativo devolvido pelo Java")] },
        { cells: ["Efetivo previsto", inteiro(resumo.efetivoPrevisto), anterior ? inteiro(anterior.efetivoPrevisto) : celula("—"), celula("Soma do efetivo_previsto dos contratos vigentes")] },
        { cells: ["Efetivo alocado", inteiro(resumo.efetivoAlocado), anterior ? inteiro(anterior.efetivoAlocado) : celula("—"), celula("Empregados com vínculo contratual vigente na data")] },
        { cells: ["Lançamentos", inteiro(resumo.registrosLancados), anterior ? inteiro(anterior.registrosLancados) : celula("—"), celula("Todos os registros de frequência persistidos na data")] },
        { cells: ["Pendentes de lançamento", inteiro(resumo.pendentesLancamento), anterior ? inteiro(anterior.pendentesLancamento) : celula("—"), celula("Alocados − lançamentos; alerta de frequência não registrada")] },
        { cells: ["Completude dos alocados", pct(resumo.percentualCobertura), anterior ? pct(anterior.percentualCobertura) : celula("—"), celula("Lançamentos ÷ efetivo alocado; identifica omissão do operador")] },
        { cells: ["Presentes", inteiro(resumo.presentes), anterior ? inteiro(anterior.presentes) : celula("—"), celula("Presença, presença parcial, atraso e TR")] },
        { cells: ["Ausentes", inteiro(resumo.ausentes), anterior ? inteiro(anterior.ausentes) : celula("—"), celula("Falta, atestado e suspensão")] },
        { cells: ["Outros registros", inteiro(resumo.outrosRegistros), anterior ? inteiro(anterior.outrosRegistros) : celula("—"), celula("Registros persistidos em outros status")] },
        { cells: ["Déficit contratual", inteiro(resumo.deficitContratual), anterior ? inteiro(anterior.deficitContratual) : celula("—"), celula("Previsto − alocados; não confundir com lançamento pendente")] },
        { cells: ["Excedente contratual", inteiro(resumo.excedenteContratual), anterior ? inteiro(anterior.excedenteContratual) : celula("—"), celula("Alocados acima do quantitativo previsto")] },
      ],
      merges: ["A1:D1", "A2:D2", "A6:D6"],
      freezeRows: 2,
    },
    {
      name: "Serie historica",
      columns: Array.from({ length: 12 }, (_, indice) => ({ width: indice === 0 ? 16 : 18 })),
      rows: [
        { cells: [celulaXlsx("SÉRIE HISTÓRICA DO INDICADOR", estiloSecao)], height: 24 },
        { cells: ["Data", "Previsto", "Alocados", "Lançados", "Pendentes", "Presentes", "Ausentes", "Outros", "Déficit", "% Completude alocados", "% Atendimento", "% Atendimento ano anterior"].map((item) => celulaXlsx(item, estiloCabecalho)) },
        ...serieAtual.map((item, indice) => ({ cells: [
          celula(dataPtBr(item.data)),
          inteiro(item.efetivoPrevisto),
          inteiro(item.efetivoAlocado),
          inteiro(item.registrosLancados),
          inteiro(item.pendentesLancamento),
          inteiro(item.presentes),
          inteiro(item.ausentes),
          inteiro(item.outrosRegistros),
          inteiro(item.deficitContratual),
          pct(item.percentualCobertura),
          pct(item.percentualAtendimento),
          contexto.comparar && anteriorPorIndice.get(indice) ? pct(anteriorPorIndice.get(indice).percentualAtendimento) : celula("—"),
        ] })),
      ],
      merges: ["A1:L1"],
      freezeRows: 2,
    },
    planilhaContratos(dados?.detalhamentoContratos, referencia),
  ];

  if (lista(dados?.detalhamentoLotacoes).length) sheets.push(planilhaDimensao("Lotacoes", dados.detalhamentoLotacoes, referencia));
  if (lista(dados?.detalhamentoTurnos).length) sheets.push(planilhaDimensao("Turnos", dados.detalhamentoTurnos, referencia));
  if (lista(dados?.detalhamentoCargos).length) sheets.push(planilhaDimensao("Cargos", dados.detalhamentoCargos, referencia));

  if (quadro.length) {
    sheets.push({
      name: "Quadro dimensionado",
      columns: [{ width: 24 }, { width: 32 }, { width: 26 }, { width: 32 }, ...Array.from({ length: 6 }, () => ({ width: 16 }))],
      rows: [
        { cells: [celulaXlsx(`COMPOSIÇÃO OPERACIONAL — ${dataPtBr(referencia)}`, estiloSecao)], height: 24 },
        { cells: ["Contrato", "Lotação", "Turno", "Cargo", "Alocados", "Lançados", "Pendentes", "Presentes", "Ausentes", "% Cobertura"].map((item) => celulaXlsx(item, estiloCabecalho)) },
        ...quadro.map((item) => ({ cells: [
          celula(item.contrato || "Sem contrato"),
          celula(item.lotacao || "Sem lotação"),
          celula(item.turno || "Sem turno"),
          celula(item.cargo || "Sem cargo"),
          inteiro(item.efetivoAlocado),
          inteiro(item.registrosLancados),
          inteiro(item.pendentesLancamento),
          inteiro(item.presentes),
          inteiro(item.ausentes),
          pct(item.percentualCobertura),
        ] })),
      ],
      merges: ["A1:J1"],
      freezeRows: 2,
    });
  }

  return { creator: "SGT-GTRAT", sheets };
};

export const exportarIndicadorEfetivoXlsx = async (dados, contexto = {}) => {
  if (!dados) return;
  try {
    const referencia = contexto.referencia || dados.fim || "referencia";
    baixarArquivoXlsx(
      criarConfiguracao(dados, contexto),
      `indicador_efetivo_${nomeSeguro(contexto.contratoNome || "consolidado")}_${referencia}.xlsx`,
    );
    notificar("success", "Indicador exportado em XLSX com visão contratual e pendências de lançamento.");
  } catch (erro) {
    console.error("Falha ao exportar indicador de efetivo em XLSX:", erro);
    notificar("error", "Não foi possível exportar o indicador em XLSX.");
  }
};

const tabelaContratosPdf = (autoTable, doc, itens, referencia, margem) => {
  const linhas = lista(itens);
  if (!linhas.length) return;
  let y = (doc.lastAutoTable?.finalY || 80) + 18;
  if (y > doc.internal.pageSize.getHeight() - 120) {
    doc.addPage("a4", "landscape");
    y = 48;
  }
  doc.setFont("helvetica", "bold");
  doc.setFontSize(10);
  doc.text(`Contratos — ${dataPtBr(referencia)}`, margem, y);
  autoTable(doc, {
    startY: y + 7,
    margin: { left: margem, right: margem, bottom: 34 },
    head: [["Contrato", "Prev.", "Aloc.", "Lanç.", "Pend.", "Pres.", "Aus.", "Déficit", "% Atend."]],
    body: linhas.map((item) => [
      item.nome || "Não informado",
      Number(item.efetivoPrevisto || 0),
      Number(item.efetivoAlocado || 0),
      Number(item.registrosLancados || 0),
      Number(item.pendentesLancamento || 0),
      Number(item.presentes || 0),
      Number(item.ausentes || 0),
      Number(item.deficitContratual || 0),
      `${Number(item.percentualAtendimento || 0).toLocaleString("pt-BR", { maximumFractionDigits: 2 })}%`,
    ]),
    theme: "grid",
    styles: { fontSize: 6.8, cellPadding: 3.1, overflow: "linebreak" },
    headStyles: { fillColor: [0, 65, 107], textColor: [255, 255, 255] },
    alternateRowStyles: { fillColor: [248, 250, 252] },
    columnStyles: { 0: { cellWidth: 190 } },
  });
};

const tabelaDimensaoPdf = (autoTable, doc, titulo, itens, margem) => {
  const linhas = lista(itens);
  if (!linhas.length) return;
  let y = (doc.lastAutoTable?.finalY || 80) + 18;
  if (y > doc.internal.pageSize.getHeight() - 120) {
    doc.addPage("a4", "landscape");
    y = 48;
  }
  doc.setFont("helvetica", "bold");
  doc.setFontSize(10);
  doc.text(titulo, margem, y);
  autoTable(doc, {
    startY: y + 7,
    margin: { left: margem, right: margem, bottom: 34 },
    head: [["Descrição", "Alocados", "Lançados", "Pendentes", "Presentes", "Ausentes", "% Cobertura"]],
    body: linhas.map((item) => [
      item.nome || "Não informado",
      Number(item.efetivoAlocado || 0),
      Number(item.registrosLancados || 0),
      Number(item.pendentesLancamento || 0),
      Number(item.presentes || 0),
      Number(item.ausentes || 0),
      `${Number(item.percentualCobertura || 0).toLocaleString("pt-BR", { maximumFractionDigits: 2 })}%`,
    ]),
    theme: "grid",
    styles: { fontSize: 7, cellPadding: 3.2, overflow: "linebreak" },
    headStyles: { fillColor: [0, 65, 107], textColor: [255, 255, 255] },
    alternateRowStyles: { fillColor: [248, 250, 252] },
    columnStyles: { 0: { cellWidth: 260 } },
  });
};

export const exportarIndicadorEfetivoPdf = async (dados, contexto = {}) => {
  if (!dados) return;
  try {
    const [{ default: jsPDF }, { default: autoTable }] = await Promise.all([import("jspdf"), import("jspdf-autotable")]);
    const doc = new jsPDF("l", "pt", "a4");
    const margem = 28;
    const largura = doc.internal.pageSize.getWidth();
    const resumo = dados.resumoAtual || {};
    const anterior = dados.resumoAnoAnterior || null;
    const referencia = contexto.referencia || dados.fim;

    doc.setFillColor(0, 65, 107);
    doc.rect(0, 0, largura, 58, "F");
    doc.setFillColor(255, 212, 0);
    doc.rect(0, 58, largura, 4, "F");
    doc.setTextColor(255, 255, 255);
    doc.setFont("helvetica", "bold");
    doc.setFontSize(17);
    doc.text("Indicador Contratual de Efetivo — SGT-GTRAT", margem, 27);
    doc.setFont("helvetica", "normal");
    doc.setFontSize(8.5);
    doc.text(`Referência: ${dataPtBr(referencia)} | ${contexto.escopoSelecionado || "Escopo consolidado"}`, margem, 43);
    doc.setTextColor(40, 56, 66);

    autoTable(doc, {
      startY: 76,
      margin: { left: margem, right: margem, bottom: 34 },
      head: [["Indicador", "Atual", "Ano anterior", "Leitura"]],
      body: [
        ["Cobertura dos alocados", `${Number(resumo.percentualCobertura || 0).toLocaleString("pt-BR", { maximumFractionDigits: 2 })}%`, anterior ? `${Number(anterior.percentualCobertura || 0).toLocaleString("pt-BR", { maximumFractionDigits: 2 })}%` : "—", "Percentual devolvido pelo Java"],
        ["Atendimento contratual", `${Number(resumo.percentualAtendimento || 0).toLocaleString("pt-BR", { maximumFractionDigits: 2 })}%`, anterior ? `${Number(anterior.percentualAtendimento || 0).toLocaleString("pt-BR", { maximumFractionDigits: 2 })}%` : "—", "Presentes ÷ efetivo previsto"],
        ["Efetivo previsto", Number(resumo.efetivoPrevisto || 0), anterior ? Number(anterior.efetivoPrevisto || 0) : "—", "Dimensionamento dos contratos vigentes"],
        ["Efetivo alocado", Number(resumo.efetivoAlocado || 0), anterior ? Number(anterior.efetivoAlocado || 0) : "—", "Vínculos vigentes na data"],
        ["Lançamentos", Number(resumo.registrosLancados || 0), anterior ? Number(anterior.registrosLancados || 0) : "—", "Registros de frequência persistidos"],
        ["Pendentes", Number(resumo.pendentesLancamento || 0), anterior ? Number(anterior.pendentesLancamento || 0) : "—", "Alocados sem lançamento"],
        ["Completude dos alocados", `${Number(resumo.percentualCobertura || 0).toLocaleString("pt-BR", { maximumFractionDigits: 2 })}%`, anterior ? `${Number(anterior.percentualCobertura || 0).toLocaleString("pt-BR", { maximumFractionDigits: 2 })}%` : "—", "Lançamentos ÷ alocados; sinaliza omissão operacional"],
        ["Presentes", Number(resumo.presentes || 0), anterior ? Number(anterior.presentes || 0) : "—", "Presença, parcial, atraso e TR"],
        ["Ausentes", Number(resumo.ausentes || 0), anterior ? Number(anterior.ausentes || 0) : "—", "Falta, atestado e suspensão"],
        ["Déficit contratual", Number(resumo.deficitContratual || 0), anterior ? Number(anterior.deficitContratual || 0) : "—", "Previsto − alocados"],
      ],
      theme: "grid",
      styles: { fontSize: 7.5, cellPadding: 3.8, overflow: "linebreak" },
      headStyles: { fillColor: [0, 65, 107], textColor: [255, 255, 255] },
      alternateRowStyles: { fillColor: [248, 250, 252] },
    });

    let y = (doc.lastAutoTable?.finalY || 170) + 18;
    doc.setFont("helvetica", "bold");
    doc.setFontSize(10);
    doc.text(`Série histórica — ${dataPtBr(dados.inicio)} a ${dataPtBr(dados.fim)}`, margem, y);
    autoTable(doc, {
      startY: y + 7,
      margin: { left: margem, right: margem, bottom: 34 },
      head: [["Data", "Prev.", "Aloc.", "Lanç.", "Pend.", "Pres.", "Aus.", "% Comp. aloc.", "% Atend."]],
      body: lista(dados.serieAtual).map((item) => [
        dataPtBr(item.data),
        Number(item.efetivoPrevisto || 0),
        Number(item.efetivoAlocado || 0),
        Number(item.registrosLancados || 0),
        Number(item.pendentesLancamento || 0),
        Number(item.presentes || 0),
        Number(item.ausentes || 0),
        `${Number(item.percentualCobertura || 0).toLocaleString("pt-BR", { maximumFractionDigits: 2 })}%`,
        `${Number(item.percentualAtendimento || 0).toLocaleString("pt-BR", { maximumFractionDigits: 2 })}%`,
      ]),
      theme: "grid",
      styles: { fontSize: 6.7, cellPadding: 2.8 },
      headStyles: { fillColor: [0, 65, 107], textColor: [255, 255, 255] },
      alternateRowStyles: { fillColor: [248, 250, 252] },
    });

    tabelaContratosPdf(autoTable, doc, dados.detalhamentoContratos, referencia, margem);
    tabelaDimensaoPdf(autoTable, doc, `Lotações — ${dataPtBr(referencia)}`, dados.detalhamentoLotacoes, margem);
    tabelaDimensaoPdf(autoTable, doc, `Turnos — ${dataPtBr(referencia)}`, dados.detalhamentoTurnos, margem);
    tabelaDimensaoPdf(autoTable, doc, `Cargos — ${dataPtBr(referencia)}`, dados.detalhamentoCargos, margem);

    const quadro = lista(dados.quadroDimensionado);
    if (quadro.length) {
      doc.addPage("a4", "landscape");
      doc.setTextColor(40, 56, 66);
      doc.setFont("helvetica", "bold");
      doc.setFontSize(11);
      doc.text(`Composição operacional — ${dataPtBr(referencia)}`, margem, 34);
      autoTable(doc, {
        startY: 44,
        margin: { left: margem, right: margem, bottom: 34 },
        head: [["Contrato", "Lotação", "Turno", "Cargo", "Aloc.", "Lanç.", "Pend.", "Pres.", "Aus.", "% Cob."]],
        body: quadro.map((item) => [
          item.contrato || "Sem contrato",
          item.lotacao || "Sem lotação",
          item.turno || "Sem turno",
          item.cargo || "Sem cargo",
          Number(item.efetivoAlocado || 0),
          Number(item.registrosLancados || 0),
          Number(item.pendentesLancamento || 0),
          Number(item.presentes || 0),
          Number(item.ausentes || 0),
          `${Number(item.percentualCobertura || 0).toLocaleString("pt-BR", { maximumFractionDigits: 2 })}%`,
        ]),
        theme: "grid",
        styles: { fontSize: 6.1, cellPadding: 2.7, overflow: "linebreak" },
        headStyles: { fillColor: [0, 65, 107], textColor: [255, 255, 255] },
        alternateRowStyles: { fillColor: [248, 250, 252] },
        columnStyles: { 0: { cellWidth: 90 }, 1: { cellWidth: 118 }, 2: { cellWidth: 90 }, 3: { cellWidth: 125 } },
      });
    }

    const paginas = doc.getNumberOfPages();
    for (let pagina = 1; pagina <= paginas; pagina += 1) {
      doc.setPage(pagina);
      const altura = doc.internal.pageSize.getHeight();
      const larguraPagina = doc.internal.pageSize.getWidth();
      doc.setDrawColor(220, 228, 233);
      doc.line(margem, altura - 24, larguraPagina - margem, altura - 24);
      doc.setFont("helvetica", "normal");
      doc.setTextColor(105, 116, 124);
      doc.setFontSize(7);
      doc.text("SGT-GTRAT | Efetivo contratual e cobertura de frequência", margem, altura - 11);
      doc.text(`Página ${pagina} de ${paginas}`, larguraPagina - margem, altura - 11, { align: "right" });
    }

    doc.save(`indicador_efetivo_${nomeSeguro(contexto.contratoNome || "consolidado")}_${referencia || "referencia"}.pdf`);
    notificar("success", "Indicador exportado em PDF com visão contratual e pendências de lançamento.");
  } catch (erro) {
    console.error("Falha ao exportar indicador de efetivo em PDF:", erro);
    notificar("error", "Não foi possível exportar o indicador em PDF.");
  }
};
