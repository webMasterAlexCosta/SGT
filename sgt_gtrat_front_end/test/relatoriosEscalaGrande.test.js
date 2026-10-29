import test from "node:test";
import assert from "node:assert/strict";
import { readFile } from "node:fs/promises";

const ler = (caminho) => readFile(new URL(`../${caminho}`, import.meta.url), "utf8");

test("visão consolidada pagina a tela e exporta a base completa no servidor", async () => {
  const pagina = await ler("src/modules/rh/pages/PaginaRelatoriosTerceirizados/index.jsx");
  const servico = await ler("src/services/rh/ServicoRelatorioTerceirizados.js");

  assert.match(servico, /\/relatorios\/terceirizados\/consolidado/);
  assert.match(servico, /\/relatorios\/terceirizados\/exportacao/);
  assert.match(servico, /RELATORIO_TERCEIRIZADOS_TAMANHO_MAXIMO\s*=\s*5000/);
  assert.doesNotMatch(servico, /consolidadoCompleto|PARALELISMO_CARGA_COMPLETA/);
  assert.match(pagina, /const TAMANHO_PADRAO = 100/);
  assert.match(pagina, /page:\s*pagina/);
  assert.match(pagina, /useExportacaoRelatorio/);
  assert.match(pagina, /iniciarExportacao/);
  assert.match(pagina, /tipo:\s*"terceirizados"/);
  assert.match(pagina, /paginacao\.totalElements/);
  assert.doesNotMatch(pagina, /ServicoEmpregado/);
  assert.doesNotMatch(pagina, /PAGINATION_MAX_SIZE/);
  assert.doesNotMatch(pagina, /calcularIndicadores|agruparPorStatus|agruparPorContrato|filtrarEmpregados/);
});

test("visão consolidada oferece 1000, 2000, 5000 e Todos mantendo 100 como padrão", async () => {
  const tabela = await ler("src/modules/rh/pages/PaginaRelatoriosTerceirizados/components/TabelaEmpregadosRelatorio/index.jsx");
  const pagina = await ler("src/modules/rh/pages/PaginaRelatoriosTerceirizados/index.jsx");
  assert.match(tabela, /\[25, 50, 100, 200, 1000, 2000, 5000\]/);
  assert.match(tabela, /<option value=\{TAMANHO_TODOS\}>Todos<\/option>/);
  assert.match(pagina, /const TAMANHO_PADRAO = 100/);
  assert.match(tabela, /aoMudarPagina/);
});

test("exportação de terceiros usa somente o arquivo oficial do Java para o filtro completo", async () => {
  const cabecalho = await ler("src/modules/rh/pages/PaginaRelatoriosTerceirizados/components/CabecalhoRelatorios/index.jsx");
  const pagina = await ler("src/modules/rh/pages/PaginaRelatoriosTerceirizados/index.jsx");
  assert.match(cabecalho, /Resultado filtrado completo/);
  assert.match(cabecalho, /gerado oficialmente pelo servidor/);
  assert.match(cabecalho, /aoExportarExcel/);
  assert.match(cabecalho, /aoExportarPdf/);
  assert.doesNotMatch(cabecalho, /Página atual|Todas as páginas/);
  assert.match(pagina, /executarExportacaoOficial/);
  assert.match(pagina, /useExportacaoRelatorio/);
  assert.match(pagina, /iniciarExportacao/);
  assert.match(pagina, /totalEstimado:\s*paginacao\.totalElements/);
  assert.doesNotMatch(pagina, /exportarRelatorioExcel|exportarRelatorioPdf/);
});

test("painel usa diretamente a semântica de registros lançados", async () => {
  const executivo = await ler("src/modules/painel/components/GestaoExecutiva/index.jsx");
  const ferramentas = await ler("src/modules/painel/components/FerramentasGestao/index.jsx");
  assert.doesNotMatch(executivo, /registrosLancados \?\?/);
  assert.match(executivo, /Registros lançados/);
  assert.doesNotMatch(ferramentas, /registrosLancados \?\?/);
});

test("tabela diferencia contrato vigente de vínculo histórico", async () => {
  const tabela = await ler("src/modules/rh/pages/PaginaRelatoriosTerceirizados/components/TabelaEmpregadosRelatorio/index.jsx");
  assert.match(tabela, /contratosVigentes/);
  assert.match(tabela, /contratosHistoricos/);
  assert.match(tabela, /Sem contrato vigente/);
  assert.match(tabela, /Histórico:/);
});
