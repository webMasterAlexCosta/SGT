import test from "node:test";
import assert from "node:assert/strict";
import { readFile } from "node:fs/promises";

const base = new URL("../src/modules/rh/pages/EmpregadosLista/", import.meta.url);

const ler = (caminho) => readFile(new URL(caminho, base), "utf8");

test("EmpregadosLista e seus principais subcomponentes usam CSS Modules próprios", async () => {
  const arquivos = [
    ["index.jsx", "./index.module.css"],
    ["components/CabecalhoEmpregados/index.jsx", "./index.module.css"],
    ["components/FiltrosEmpregados/index.jsx", "./index.module.css"],
    ["components/EstatisticasEmpregados/index.jsx", "./index.module.css"],
    ["components/BarraVisualizacaoEmpregados/index.jsx", "./index.module.css"],
    ["components/FiltrosAvancadosEmpregados/index.jsx", "./index.module.css"],
    ["components/PaginacaoEmpregados/index.jsx", "./index.module.css"],
    ["components/GrupoEmpregados/index.jsx", "./index.module.css"],
    ["components/ModaisAgrupados.jsx", "./ModaisAgrupados.module.css"],
    ["ui/ModalHistoricoContrato.jsx", "./ModalHistoricoContrato.module.css"],
    ["components/FormularioEmpregado/index.jsx", "./index.module.css"],
    ["components/FormularioEmpregado/components/ComponentesBase.jsx", "./ComponentesBase.module.css"],
    ["components/FormularioEmpregado/components/SecoesFormulario.jsx", "./SecoesFormulario.module.css"],
  ];

  for (const [arquivo, modulo] of arquivos) {
    const conteudo = await ler(arquivo);
    assert.ok(
      conteudo.includes(`import estilos from "${modulo}"`) ||
        conteudo.includes(`import estilos from '${modulo}'`),
      `${arquivo} deve importar ${modulo}`,
    );
  }
});

test("layout da lista responde a desktop, notebook, tablet e celular", async () => {
  const [pagina, filtrosAvancados, toolbar, grupo, formulario, secoes] = await Promise.all([
    ler("index.module.css"),
    ler("components/FiltrosAvancadosEmpregados/index.module.css"),
    ler("components/BarraVisualizacaoEmpregados/index.module.css"),
    ler("components/GrupoEmpregados/index.module.css"),
    ler("components/FormularioEmpregado/index.module.css"),
    ler("components/FormularioEmpregado/components/SecoesFormulario.module.css"),
  ]);

  assert.match(pagina, /@media \(min-width: 1040px\)/);
  assert.match(pagina, /@media \(max-width: 640px\)/);
  assert.match(filtrosAvancados, /\.sobreposicaoModal\s*\{[^}]*position:\s*fixed/s);
  assert.match(filtrosAvancados, /@media \(max-width: 620px\)/);
  assert.match(toolbar, /@media \(max-width: 520px\)/);
  assert.match(grupo, /overflow:\s*auto/);
  assert.match(grupo, /content-visibility:\s*auto/);
  assert.match(formulario, /@media \(max-width: 820px\)/);
  assert.match(formulario, /@media \(max-width: 640px\)/);
  assert.match(secoes, /@media \(max-width: 600px\)/);
});

test("tabela mantém leitura em telas estreitas e grade vira coluna única quando necessário", async () => {
  const grupo = await ler("components/GrupoEmpregados/index.module.css");
  const secoes = await ler("components/FormularioEmpregado/components/SecoesFormulario.module.css");

  assert.match(grupo, /min-width:\s*930px/);
  assert.match(grupo, /grid-template-columns:\s*1fr/);
  assert.match(secoes, /\.gradeContratos\s*\{[^}]*grid-template-columns:\s*1fr/s);
});
