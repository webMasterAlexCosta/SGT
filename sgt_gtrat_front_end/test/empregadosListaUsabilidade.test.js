import test from "node:test";
import assert from "node:assert/strict";
import { readFile } from "node:fs/promises";

const caminhoFiltro = new URL(
  "../src/modules/rh/pages/EmpregadosLista/components/FiltrosEmpregados/index.jsx",
  import.meta.url,
);
const caminhoFiltroCss = new URL(
  "../src/modules/rh/pages/EmpregadosLista/components/FiltrosEmpregados/index.module.css",
  import.meta.url,
);
const caminhoPaginacao = new URL(
  "../src/modules/rh/pages/EmpregadosLista/components/PaginacaoEmpregados/index.jsx",
  import.meta.url,
);
const caminhoCabecalho = new URL(
  "../src/modules/rh/pages/EmpregadosLista/components/CabecalhoEmpregados/index.jsx",
  import.meta.url,
);
const caminhoFiltrosAvancados = new URL(
  "../src/modules/rh/pages/EmpregadosLista/components/FiltrosAvancadosEmpregados/index.jsx",
  import.meta.url,
);
const caminhoLogica = new URL(
  "../src/modules/rh/pages/EmpregadosLista/useEmpregadosLista.js",
  import.meta.url,
);
const caminhoPagina = new URL(
  "../src/modules/rh/pages/EmpregadosLista/index.jsx",
  import.meta.url,
);
const caminhoServico = new URL(
  "../src/services/rh/ServicoEmpregado.js",
  import.meta.url,
);
const caminhoVisualizacao = new URL(
  "../src/modules/rh/pages/EmpregadosLista/components/BarraVisualizacaoEmpregados/index.jsx",
  import.meta.url,
);
const caminhoGrupo = new URL(
  "../src/modules/rh/pages/EmpregadosLista/components/GrupoEmpregados/index.jsx",
  import.meta.url,
);
const caminhoGrupoCss = new URL(
  "../src/modules/rh/pages/EmpregadosLista/components/GrupoEmpregados/index.module.css",
  import.meta.url,
);
const caminhoFavoritos = new URL(
  "../src/modules/rh/pages/EmpregadosLista/favoritos.js",
  import.meta.url,
);

test("painel de contratos usa toda a base e oferece favoritos persistidos", async () => {
  const [filtro, filtroCss, logica, favoritos] = await Promise.all([
    readFile(caminhoFiltro, "utf8"),
    readFile(caminhoFiltroCss, "utf8"),
    readFile(caminhoLogica, "utf8"),
    readFile(caminhoFavoritos, "utf8"),
  ]);

  assert.match(filtro, /placeholder="Pesquisar contrato\.\.\."/);
  assert.match(filtro, /definirLimiteVisivel\(60\)/);
  assert.match(filtro, /useDeferredValue/);
  assert.match(filtro, /indiceBusca/);
  assert.match(filtro, /opcoesFiltradas\.slice\(0, limiteVisivel\)/);
  assert.match(filtro, /Favoritos/);
  assert.match(filtro, /aoAlternarFavoritoContrato/);
  assert.match(filtro, /filtro\.quantidadeColaboradores/);
  assert.match(filtro, /quantidade\.toLocaleString\("pt-BR"\)/);
  assert.doesNotMatch(filtro, /contagemPorGrupo/);
  assert.match(filtroCss, /max-height:\s*43rem/);
  assert.match(logica, /const \[contratosBase, definirContratosBase\]/);
  assert.match(logica, /quantidadeColaboradores: Number\(contrato\.quantidadeColaboradores/);
  assert.match(logica, /contratoIds:\s*consulta\.contratoIds/);
  assert.match(logica, /const \[contratosFavoritos, definirContratosFavoritos\]/);
  assert.match(logica, /salvarContratosFavoritos\(window\.localStorage, contratosFavoritos\)/);
  assert.match(favoritos, /CHAVE_CONTRATOS_FAVORITOS/);
  assert.doesNotMatch(
    logica,
    /remove seleções que não existem mais/,
    "As escolhas não devem ser apagadas ao trocar de página.",
  );
});

test("lista usa endpoint enxuto, filtro de turno no servidor e feedback de espera", async () => {
  const [logica, pagina, servico] = await Promise.all([
    readFile(caminhoLogica, "utf8"),
    readFile(caminhoPagina, "utf8"),
    readFile(caminhoServico, "utf8"),
  ]);

  assert.match(servico, /url:\s*'\/empregados\/resumo'/);
  assert.match(servico, /url:\s*'\/contratos\/opcoes'/);
  assert.match(logica, /criarConsultaEmpregados/);
  assert.match(logica, /turnoId:\s*consulta\.turnoId/);
  assert.doesNotMatch(logica, /PAGINAÇÃO DO LADO DO CLIENTE/);
  assert.doesNotMatch(logica, /size:\s*precisaPaginacaoCliente\s*\?\s*10000/);
  assert.match(logica, /sequenciaConsultaRef/);
  assert.match(logica, /controladorConsultaRef/);
  assert.match(logica, /versaoDebounceRef/);
  assert.match(logica, /definirRevisaoConsulta/);
  assert.match(pagina, /Aguarde, buscando colaboradores/);
  assert.match(pagina, /aria-busy="true"/);
});

test("filtros avançados concentram lotação, turno e quantidade por página", async () => {
  const [filtrosAvancados, cabecalho] = await Promise.all([
    readFile(caminhoFiltrosAvancados, "utf8"),
    readFile(caminhoCabecalho, "utf8"),
  ]);

  assert.match(filtrosAvancados, /Filtros avançados/);
  assert.match(filtrosAvancados, /Todas as lotações/);
  assert.match(filtrosAvancados, /Todos os turnos/);
  assert.match(filtrosAvancados, /Exibir até \{CONFIG\.PAGINATION_MAX_SIZE\}/);
  assert.match(filtrosAvancados, /Aplicar filtros/);
  assert.match(cabecalho, /Cadastro de empregados/);
  assert.match(cabecalho, /quantidadeContratos/);
});


test("componente oferece tabela, lista e grade sem nova consulta ao servidor", async () => {
  const [visualizacao, grupo, grupoCss, logica, pagina] = await Promise.all([
    readFile(caminhoVisualizacao, "utf8"),
    readFile(caminhoGrupo, "utf8"),
    readFile(caminhoGrupoCss, "utf8"),
    readFile(caminhoLogica, "utf8"),
    readFile(caminhoPagina, "utf8"),
  ]);

  assert.match(visualizacao, /MODOS_VISUALIZACAO\.TABELA/);
  assert.match(visualizacao, /MODOS_VISUALIZACAO\.LISTA/);
  assert.match(visualizacao, /MODOS_VISUALIZACAO\.GRADE/);
  assert.match(visualizacao, /aria-pressed=\{selecionado\}/);
  assert.match(visualizacao, /não refaz a consulta ao servidor/);
  assert.match(pagina, /<BarraVisualizacaoEmpregados/);
  assert.match(pagina, /modoVisualizacao=\{estado\.modoVisualizacao\}/);
  assert.match(grupo, /modoVisualizacao === MODOS_VISUALIZACAO\.GRADE/);
  assert.match(grupo, /modoVisualizacao === MODOS_VISUALIZACAO\.LISTA/);
  assert.match(grupo, /ocultarCabecalho \|\| estaExpandido/);
  assert.match(pagina, /ocultarCabecalho/);
  assert.match(grupo, /aria-expanded=\{Boolean\(estaExpandido\)\}/);
  assert.match(grupoCss, /content-visibility:\s*auto/);
  assert.match(logica, /salvarModoVisualizacao\(window\.localStorage, modoVisualizacao\)/);
  assert.doesNotMatch(
    logica,
    /modoVisualizacao\s*:/,
    "O modo visual não deve ser enviado ao backend como parâmetro de consulta.",
  );
});

test("limpar filtros cancela consulta antiga e remove todos os estados visuais", async () => {
  const logica = await readFile(caminhoLogica, "utf8");

  assert.match(logica, /const aoLimparBuscaEFiltros = useCallback/);
  assert.match(logica, /controladorConsultaRef\.current\?\.abort\(\)/);
  assert.match(logica, /definirTermoBusca\(""\)/);
  assert.match(logica, /definirBuscaDebounced\(""\)/);
  assert.match(logica, /definirFiltrosGrupoAtivos\(\[\]\)/);
  assert.match(logica, /definirLotacaoFiltro\(""\)/);
  assert.match(logica, /definirTurnoFiltro\(""\)/);
  assert.match(logica, /definirTamanhoPagina\(CONFIG\.PAGINATION_DEFAULT_SIZE\)/);
});

test("histórico de contrato usa DTO plano completo e não depende de entidade lazy", async () => {
  const caminhoHistoricoModal = new URL(
    "../src/modules/rh/pages/EmpregadosLista/ui/ModalHistoricoContrato.jsx",
    import.meta.url,
  );
  const historico = await readFile(caminhoHistoricoModal, "utf8");

  assert.match(historico, /item\.nomeContrato/);
  assert.match(historico, /item\.descricaoContrato/);
  assert.match(historico, /item\.tipoContrato/);
  assert.match(historico, /item\.gestaoLocalNome/);
  assert.match(historico, /item\.superintendenciaNome/);
  assert.match(historico, /item\.efetivoPrevisto/);
  assert.doesNotMatch(historico, /item\.contrato\?\./);
});

test("paginação continua disponível no limite máximo e permite navegação direta", async () => {
  const [paginacao, filtrosAvancados] = await Promise.all([
    readFile(caminhoPaginacao, "utf8"),
    readFile(caminhoFiltrosAvancados, "utf8"),
  ]);

  assert.doesNotMatch(paginacao, /tamanhoPagina\s*===\s*CONFIG\.PAGINATION_MAX_SIZE/);
  assert.match(paginacao, /carregarPagina\(paginaAtual \+ 1\)/);
  assert.match(paginacao, /carregarPagina\(totalPaginas - 1\)/);
  assert.match(paginacao, /criarPaginasVisiveis/);
  assert.match(filtrosAvancados, /Exibir até \{CONFIG\.PAGINATION_MAX_SIZE\}/);
});
