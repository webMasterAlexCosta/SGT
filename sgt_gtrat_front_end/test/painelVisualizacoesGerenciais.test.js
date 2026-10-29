import test from "node:test";
import assert from "node:assert/strict";
import { readFile } from "node:fs/promises";

const ler = (caminho) => readFile(new URL(`../${caminho}`, import.meta.url), "utf8");

test("painel oferece linhas, barras, dispersão e infográfico em um único visual principal", async () => {
  const [painel, visualizador] = await Promise.all([
    ler("src/modules/painel/components/IndicadorEfetivo/index.jsx"),
    ler("src/modules/painel/components/VisualizadorEfetivo/index.jsx"),
  ]);

  assert.match(painel, /<VisualizadorEfetivo/);
  assert.match(visualizador, /id: "LINHAS", rotulo: "Linhas"/);
  assert.match(visualizador, /id: "BARRAS", rotulo: "Barras"/);
  assert.match(visualizador, /id: "DISPERSAO", rotulo: "Dispersão"/);
  assert.match(visualizador, /id: "INFOGRAFICO", rotulo: "Infográfico"/);
  assert.match(visualizador, /useState\("LINHAS"\)/);
  assert.match(visualizador, /aria-pressed=\{tipo === id\}/);
});

test("visualização diária evita tendência e dispersão sem amostra suficiente", async () => {
  const visualizador = await ler("src/modules/painel/components/VisualizadorEfetivo/index.jsx");

  assert.match(visualizador, /const podeExibirLinhas = serie\.length >= 2/);
  assert.match(visualizador, /const podeExibirDispersao = pontosDispersao\.length >= 3/);
  assert.match(visualizador, /tipoSelecionado === "LINHAS" && !podeExibirLinhas/);
  assert.match(visualizador, /tipoSelecionado === "DISPERSAO" && !podeExibirDispersao/);
  assert.match(visualizador, /disabled=\{indisponivel\}/);
});

test("barras de uma única referência usam comparação horizontal direta", async () => {
  const [visualizador, css] = await Promise.all([
    ler("src/modules/painel/components/VisualizadorEfetivo/index.jsx"),
    ler("src/modules/painel/components/VisualizadorEfetivo/index.module.css"),
  ]);

  assert.match(visualizador, /const BarrasFotografia/);
  assert.match(visualizador, /if \(serie\.length <= 1\) return <BarrasFotografia/);
  assert.match(visualizador, /Dimensionamento vigente devolvido pelo Java/);
  assert.match(css, /\.fotografiaBarras/);
  assert.match(css, /\.trilhoFotografia/);
});

test("dispersão relaciona efetivo operacional e presentes sem sugerir causalidade", async () => {
  const visualizador = await ler("src/modules/painel/components/VisualizadorEfetivo/index.jsx");

  assert.match(visualizador, /<ScatterChart/);
  assert.match(visualizador, /dataKey="efetivoAlocado"/);
  assert.match(visualizador, /dataKey="presentes"/);
  assert.match(visualizador, /dataKey="impactoOperacional"/);
  assert.match(visualizador, /ausentes.*pendentes/si);
  assert.doesNotMatch(visualizador, /calcularCorrelacao|descreverCorrelacao|Correlação .*r =/);
  assert.match(visualizador, /a diagonal representa presença integral/i);
});

test("dispersão diária recebe contratos ou dimensões operacionais comparáveis", async () => {
  const painel = await ler("src/modules/painel/components/IndicadorEfetivo/index.jsx");

  assert.match(painel, /const dispersaoGerencial = useMemo/);
  assert.match(painel, /detalhamentoContratos/);
  assert.match(painel, /detalhamentoLotacoes/);
  assert.match(painel, /detalhamentoTurnos/);
  assert.match(painel, /detalhamentoCargos/);
  assert.match(painel, /pontosDispersao=\{dispersaoGerencial\.pontos\}/);
  assert.match(painel, /contextoDispersao=\{dispersaoGerencial\.contexto\}/);
});

test("infográfico mantém a cadeia contratual e separa pendência de ausência", async () => {
  const visualizador = await ler("src/modules/painel/components/VisualizadorEfetivo/index.jsx");

  assert.match(visualizador, /1\. Capacidade contratada/);
  assert.match(visualizador, /2\. Efetivo operacional/);
  assert.match(visualizador, /3\. Registros realizados/);
  assert.match(visualizador, /4\. Resultado operacional/);
  assert.match(visualizador, /faixaAusentes/);
  assert.match(visualizador, /faixaPendentes/);
  assert.match(visualizador, /Pendentes/);
});

test("visualizador possui acabamento responsivo próprio", async () => {
  const css = await ler("src/modules/painel/components/VisualizadorEfetivo/index.module.css");
  assert.match(css, /\.seletorVisualizacao/);
  assert.match(css, /\.fluxoInfografico/);
  assert.match(css, /@media \(max-width: 920px\)/);
  assert.match(css, /@media \(max-width: 620px\)/);
  assert.match(css, /@media \(max-width: 410px\)/);
  assert.doesNotMatch(css, /radial-gradient|conic-gradient/);
  assert.match(css, /\.tabelaAlternativa/);
});
