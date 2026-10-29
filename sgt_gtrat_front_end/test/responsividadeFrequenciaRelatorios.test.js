import test from "node:test";
import assert from "node:assert/strict";
import { readFile } from "node:fs/promises";

const ler = (caminho) => readFile(new URL(`../${caminho}`, import.meta.url), "utf8");

test("frequência adapta a virtualização ao viewport e evita corte no mobile", async () => {
  const [tabela, estilos] = await Promise.all([
    ler("src/modules/rh/pages/PaginaFrequencia/components/TabelaFrequencia.jsx"),
    ler("src/modules/rh/pages/PaginaFrequencia/index.module.css"),
  ]);

  assert.match(tabela, /larguraViewport/);
  assert.match(tabela, /modoMobile\s*=\s*larguraViewport\s*<=\s*820/);
  assert.match(tabela, /modoMobileCompacto[\s\S]*390/);
  assert.match(tabela, /itemSize=\{modoMobile \? null : alturaLinhaCompacta\}/);
  assert.match(estilos, /@media \(max-width: 820px\)[\s\S]*\.linhaVirtualBase[\s\S]*flex-direction:\s*column/);
  assert.match(estilos, /\.colunaAcoesLargura::before[\s\S]*Status diário/);
});

test("visão consolidada diferencia falha real de resultado vazio e corrige pagina inválida", async () => {
  const pagina = await ler("src/modules/rh/pages/PaginaRelatoriosTerceirizados/index.jsx");

  assert.match(pagina, /erroConsulta/);
  assert.match(pagina, /totalPaginasResposta > 0 && pagina >= totalPaginasResposta/);
  assert.match(pagina, /setPagina\(totalPaginasResposta - 1\)/);
  assert.match(pagina, /\(!erroConsulta \|\| dados\)/);
  assert.match(pagina, /Tentar novamente/);
});

test("quadro consolidado vira cartões no celular sem rolagem horizontal obrigatória", async () => {
  const [tabela, estilos] = await Promise.all([
    ler("src/modules/rh/pages/PaginaRelatoriosTerceirizados/components/TabelaEmpregadosRelatorio/index.jsx"),
    ler("src/modules/rh/pages/PaginaRelatoriosTerceirizados/components/TabelaEmpregadosRelatorio/index.module.css"),
  ]);

  assert.match(tabela, /data-label="Empregado"/);
  assert.match(tabela, /data-label="Contrato\(s\)"/);
  assert.match(estilos, /@media \(max-width: 700px\)[\s\S]*\.cartaoTabela thead\s*\{\s*display:\s*none/);
  assert.match(estilos, /\.cartaoTabela td::before[\s\S]*attr\(data-label\)/);
  assert.match(estilos, /\.cartaoTabela tbody tr[\s\S]*grid-template-columns:\s*1fr 1fr/);
});

test("gráfico de contratos possui leitura compacta própria para mobile", async () => {
  const [grafico, estilos] = await Promise.all([
    ler("src/modules/rh/pages/PaginaRelatoriosTerceirizados/components/GraficosRelatorios/index.jsx"),
    ler("src/modules/rh/pages/PaginaRelatoriosTerceirizados/components/GraficosRelatorios/index.module.css"),
  ]);

  assert.match(grafico, /listaContratosMobile/);
  assert.match(grafico, /maiorContrato/);
  assert.match(estilos, /@media \(max-width: 700px\)[\s\S]*\.areaGrafico\s*\{\s*display:\s*none/);
  assert.match(estilos, /\.listaContratosMobile[\s\S]*display:\s*flex/);
});
