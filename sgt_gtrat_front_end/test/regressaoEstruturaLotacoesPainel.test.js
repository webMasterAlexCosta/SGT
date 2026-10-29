import test from "node:test";
import assert from "node:assert/strict";
import { readFile } from "node:fs/promises";

const ler = (caminho) => readFile(new URL(`../${caminho}`, import.meta.url), "utf8");

test("perfis com consulta carregam Gestões Locais e somente administrador recebe ações de escrita", async () => {
  const pagina = await ler("src/modules/admin/pages/PaginaEstruturaOrganizacional/index.jsx");
  assert.match(pagina, /const resultadoGestoesLocais = await ServicoOrganizacional\.listarGestoesLocais\(\)/);
  assert.match(pagina, /somenteLeitura=\{!administrador\}/);
  assert.match(pagina, /aoEditar=\{administrador \? abrirGestaoLocal : null\}/);
  assert.match(pagina, /Estrutura estadual carregada, mas não foi possível carregar as Gestões Locais do seu escopo/);
});

test("falha na leitura das Gestões Locais não derruba os gestores estaduais", async () => {
  const pagina = await ler("src/modules/admin/pages/PaginaEstruturaOrganizacional/index.jsx");
  assert.match(pagina, /const resultadoSuperintendencias = await ServicoOrganizacional\.listarSuperintendencias\(\)/);
  assert.match(pagina, /try \{\s*const resultadoGestoesLocais = await ServicoOrganizacional\.listarGestoesLocais\(\)/s);
  assert.match(pagina, /setSuperintendencias\(resultadoSuperintendencias \|\| \[\]\)/);
});

test("estrutura nacional exibe hierarquia expansível de Gestões Locais", async () => {
  const grade = await ler("src/modules/admin/pages/PaginaEstruturaOrganizacional/components/GradeGestoresEstaduais/index.jsx");
  assert.match(grade, /Gestões Locais/);
  assert.match(grade, /<details className=\{estilos\.locais\}>/);
  assert.match(grade, /formatarGestaoLocal\(local\)/);
  assert.match(grade, /Gestão Local\(s\)/);
});

test("administrador carrega todas as páginas de lotações antes do filtro por UF", async () => {
  const [servico, pagina] = await Promise.all([
    ler("src/services/admin/ServicoLotacao.js"),
    ler("src/modules/admin/pages/PaginaLotacoes/index.jsx"),
  ]);
  assert.match(servico, /listarTodos: paginaTodas/);
  assert.match(servico, /while \(pagina < totalPaginas\)/);
  assert.match(pagina, /ServicoLotacao\.listarTodos\(/);
});

test("painel oferece modo foco sem remover a opção de visualizar tudo", async () => {
  const pagina = await ler("src/modules/painel/pages/index.jsx");
  assert.match(pagina, /const \[visaoPainel, setVisaoPainel\] = useState\("resumo"\)/);
  assert.match(pagina, /\{ id: "contratos", rotulo: "Contratos" \}/);
  assert.match(pagina, /\{ id: "operacao", rotulo: "Operação" \}/);
  assert.match(pagina, /\{ id: "governanca", rotulo: "Governança" \}/);
  assert.match(pagina, /\{ id: "tudo", rotulo: "Tudo" \}/);
  assert.match(pagina, /const exibir = \(secao\) => visaoPainel === "tudo" \|\| visaoPainel === secao/);
});

test("radar executivo abre diretamente o domínio relacionado", async () => {
  const pagina = await ler("src/modules/painel/pages/index.jsx");
  assert.match(pagina, /RadarExecutivo dados=\{dados\} aoAbrirSecao=\{setVisaoPainel\}/);
  assert.match(pagina, /onClick=\{\(\) => aoAbrirSecao\(secao\)\}/);
});

test("gráficos comparativos usam cores semânticas por condição", async () => {
  const graficos = await ler("src/modules/painel/components/FerramentasGestao/index.jsx");
  assert.match(graficos, /const corCobertura =/);
  assert.match(graficos, /const corOcupacao =/);
  assert.match(graficos, /const corAbsenteismo =/);
  assert.match(graficos, /fill=\{item\.corCobertura\}/);
  assert.match(graficos, /fill=\{item\.corOcupacao\}/);
  assert.match(graficos, /legendaSemantica/);
});

test("estrutura organizacional agrupa estados pelas cinco regiões e preserva Gestões Locais", async () => {
  const grade = await ler("src/modules/admin/pages/PaginaEstruturaOrganizacional/components/GradeGestoresEstaduais/index.jsx");
  assert.match(grade, /nome: "Norte"/);
  assert.match(grade, /nome: "Nordeste"/);
  assert.match(grade, /nome: "Centro-Oeste"/);
  assert.match(grade, /nome: "Sudeste"/);
  assert.match(grade, /nome: "Sul"/);
  assert.match(grade, /regiao\.gestores\.map/);
  assert.match(grade, /formatarGestorEstadual\(item, item\.uf\)/);
  assert.match(grade, /formatarGestaoLocal\(local\)/);
});
