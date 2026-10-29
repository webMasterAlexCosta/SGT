import test from "node:test";
import assert from "node:assert/strict";
import { readFile } from "node:fs/promises";

const base = new URL("../src/modules/rh/pages/EmpregadosLista/", import.meta.url);
const ler = (arquivo) => readFile(new URL(arquivo, base), "utf8");

test("filtros avançados abrem em modal e não ocupam coluna da listagem", async () => {
  const [pagina, cssPagina, filtros, cssFiltros] = await Promise.all([
    ler("index.jsx"),
    ler("index.module.css"),
    ler("components/FiltrosAvancadosEmpregados/index.jsx"),
    ler("components/FiltrosAvancadosEmpregados/index.module.css"),
  ]);

  assert.doesNotMatch(pagina, /gradeOperacionalComFiltros/);
  assert.doesNotMatch(cssPagina, /gradeOperacionalComFiltros/);
  assert.match(filtros, /createPortal\(modal, document\.body\)/);
  assert.match(filtros, /role="dialog"/);
  assert.match(filtros, /aria-modal="true"/);
  assert.match(cssFiltros, /\.sobreposicaoModal\s*\{[^}]*position:\s*fixed/s);
  assert.match(cssFiltros, /inset:\s*0/);
});

test("modal possui label visível, fecha com Escape e fecha após aplicar", async () => {
  const filtros = await ler("components/FiltrosAvancadosEmpregados/index.jsx");

  assert.match(filtros, /labelModal}>FILTROS DA LISTAGEM</);
  assert.match(filtros, /evento\.key === "Escape"/);
  assert.match(filtros, /aoAplicar\(\{[\s\S]*?\}\);\s*aoFechar\(\);/);
  assert.match(filtros, /aria-labelledby="titulo-filtros-empregados"/);
});

test("botão de filtros mantém texto visível e informa que abre dialog", async () => {
  const [toolbar, cssToolbar] = await Promise.all([
    ler("components/BarraVisualizacaoEmpregados/index.jsx"),
    ler("components/BarraVisualizacaoEmpregados/index.module.css"),
  ]);

  assert.match(toolbar, /aria-haspopup="dialog"/);
  assert.match(toolbar, /className=\{estilos\.rotuloBotaoFiltro\}>Filtros</);
  assert.match(cssToolbar, /\.rotuloBotaoFiltro\s*\{[^}]*display:\s*inline/s);
});

test("tabela não muda de layout quando modal de filtros está aberto", async () => {
  const grupo = await ler("components/GrupoEmpregados/index.jsx");
  const cssGrupo = await ler("components/GrupoEmpregados/index.module.css");

  assert.doesNotMatch(grupo, /filtrosAvancadosAbertos/);
  assert.doesNotMatch(grupo, /recipienteTabelaComFiltros/);
  assert.doesNotMatch(cssGrupo, /recipienteTabelaComFiltros/);
  assert.match(grupo, /className=\{estilos\.recipienteTabela\}/);
});
