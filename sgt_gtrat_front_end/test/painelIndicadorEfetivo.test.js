import test from "node:test";
import assert from "node:assert/strict";
import { readFile } from "node:fs/promises";
import path from "node:path";

const ler = (caminho) => readFile(new URL(`../${caminho}`, import.meta.url), "utf8");
const backend = process.env.SGT_BACKEND_DIR;
const lerBack = (relativo) => readFile(path.join(backend, relativo), "utf8");

test("painel React usa somente selects para combinações válidas do indicador", async () => {
  const [pagina, componente, css] = await Promise.all([
    ler("src/modules/painel/pages/index.jsx"),
    ler("src/modules/painel/components/IndicadorEfetivo/index.jsx"),
    ler("src/modules/painel/components/IndicadorEfetivo/index.module.css"),
  ]);
  assert.match(pagina, /<IndicadorEfetivo referencia=\{referencia\}/);
  assert.doesNotMatch(componente, /COMBINACOES|gradeCombinacoes/);
  assert.doesNotMatch(css, /gradeCombinacoes|combinacaoAtiva|\.combinacoes/);
  assert.match(componente, /<option value="">Todos os contratos<\/option>/);
  assert.match(componente, /<option value="">Todas as lotações<\/option>/);
  assert.match(componente, /<option value="">Todos os turnos<\/option>/);
  assert.match(componente, /<option value="">Todos os cargos<\/option>/);
});

test("filtros vazios não são enviados e representam consolidação da dimensão", async () => {
  const componente = await ler("src/modules/painel/components/IndicadorEfetivo/index.jsx");
  assert.match(componente, /\.\.\.\(contratoId \? \{ contratoId \} : \{\}\)/);
  assert.match(componente, /\.\.\.\(detalhamentoPermitido && lotacaoId \? \{ lotacaoId \} : \{\}\)/);
  assert.match(componente, /\.\.\.\(detalhamentoPermitido && turnoId \? \{ jornadaId: turnoId \} : \{\}\)/);
  assert.match(componente, /\.\.\.\(detalhamentoPermitido && cargoId \? \{ cargoId \} : \{\}\)/);
});

test("indicador mantém períodos, comparação e cancelamento de consulta", async () => {
  const componente = await ler("src/modules/painel/components/IndicadorEfetivo/index.jsx");
  for (const periodo of ["DIARIO", "SEMANAL", "QUINZENAL", "MENSAL", "SEMESTRAL", "ANUAL"]) {
    assert.match(componente, new RegExp(`\\[\\"${periodo}\\"`));
  }
  assert.match(componente, /Comparar ano anterior/);
  assert.match(componente, /presentesAnoAnterior/);
  assert.match(componente, /if \(!filtrosCarregados\) return undefined/);
  assert.match(componente, /controladorDadosRef\.current\?\.abort\(\)/);
  assert.match(componente, /setTimeout\(async \(\) =>/);
});

test("React consulta agregação do painel sem baixar colaboradores", async () => {
  const servico = await ler("src/services/painel/ServicoPainel.js");
  assert.match(servico, /\/painel\/efetivo\/filtros/);
  assert.match(servico, /\/painel\/efetivo/);
  assert.match(servico, /timeout: 120_000/);
});

test("backend calcula previsto, alocado, lançado e pendência como conceitos separados", async () => {
  assert.ok(backend, "SGT_BACKEND_DIR é obrigatório para o contrato integrado");
  const [controlador, servico, repositorio, dto] = await Promise.all([
    lerBack("src/main/java/com/example/siggtrat/controlador/PainelControlador.java"),
    lerBack("src/main/java/com/example/siggtrat/servico/PainelServico.java"),
    lerBack("src/main/java/com/example/siggtrat/repositorio/PainelEfetivoRepositorio.java"),
    lerBack("src/main/java/com/example/siggtrat/dto/PainelEfetivoDTO.java"),
  ]);
  assert.match(controlador, /@GetMapping\("\/efetivo"\)/);
  assert.match(controlador, /@GetMapping\("\/efetivo\/filtros"\)/);
  assert.match(repositorio, /WITH RECURSIVE datas/);
  assert.match(repositorio, /c\.efetivo_previsto/);
  assert.match(repositorio, /historico_vinculo_contrato/);
  assert.match(repositorio, /COUNT\(DISTINCT h\.id_empregado\) AS efetivo_alocado/);
  assert.match(repositorio, /COUNT\(\*\) AS registros_lancados/);
  assert.match(repositorio, /Math\.max\(0L, alocado - registros\)/);
  assert.match(repositorio, /percentual\(presentes, previsto\)/);
  assert.match(dto, /long efetivoPrevisto/);
  assert.match(dto, /long efetivoAlocado/);
  assert.match(dto, /long registrosLancados/);
  assert.match(dto, /long pendentesLancamento/);
  assert.match(dto, /long deficitContratual/);
  assert.doesNotMatch(dto, /efetivoAcionado|mediaEfetivo/);
  assert.doesNotMatch(repositorio, /efetivo_acionado/);
  assert.match(servico, /ponto = serie\.get\(serie\.size\(\) - 1\)/);
  assert.doesNotMatch(servico, /diasComMovimento/);
});

test("categorias de presença e ausência continuam iguais às regras existentes", async () => {
  const repositorio = await lerBack("src/main/java/com/example/siggtrat/repositorio/PainelEfetivoRepositorio.java");
  assert.match(repositorio, /'PRESENCA','PRESENCA_PARCIAL','ATRASO','TR'/);
  assert.match(repositorio, /'FALTA','ATESTADO','SUSPENSAO'/);
  assert.doesNotMatch(repositorio, /STATUS_PRESENTES = .*'RT'/);
});

test("dia sem frequência continua aparecendo para que a pendência seja detectada", async () => {
  const [repositorio, componente, visualizador] = await Promise.all([
    lerBack("src/main/java/com/example/siggtrat/repositorio/PainelEfetivoRepositorio.java"),
    ler("src/modules/painel/components/IndicadorEfetivo/index.jsx"),
    ler("src/modules/painel/components/VisualizadorEfetivo/index.jsx"),
  ]);
  assert.match(repositorio, /FROM datas d[\s\S]*LEFT JOIN previsto p[\s\S]*LEFT JOIN alocados a[\s\S]*LEFT JOIN lancamentos l/);
  assert.match(componente, /Pendentes do operador/);
  assert.match(componente, /alocados sem presença\/ausência/);
  assert.match(componente, /Empregado\(s\) alocado\(s\) ainda sem presença ou ausência registrada/);
  assert.match(componente, /exibirPendentesNoGrafico/);
  assert.match(`${componente}\n${visualizador}`, /empregado alocado sem lançamento/);
});


test("gráfico prioriza previsto, efetivo operacional, presentes e ausentes", async () => {
  const [componente, visualizador] = await Promise.all([
    ler("src/modules/painel/components/IndicadorEfetivo/index.jsx"),
    ler("src/modules/painel/components/VisualizadorEfetivo/index.jsx"),
  ]);
  assert.match(componente, /<VisualizadorEfetivo/);
  assert.match(visualizador, /dataKey="efetivoPrevisto" name="Previsto em contrato"/);
  assert.match(visualizador, /dataKey="efetivoAlocado" name="Efetivo operacional"/);
  assert.match(visualizador, /dataKey="presentes" name="Presentes"/);
  assert.match(visualizador, /dataKey="ausentes" name="Ausentes"/);
  assert.match(componente, /exibirPendentesNoGrafico/);
  assert.match(visualizador, /dataKey="pendentesLancamento" name="Pendentes de lançamento"/);
  assert.doesNotMatch(visualizador, /dataKey="registrosLancados" name="Lançamentos"/);
  assert.doesNotMatch(visualizador, /name="% atendimento contratual"/);
});

test("leitura operacional fica em modal com abas e paginação interna", async () => {
  const [componente, css, modal] = await Promise.all([
    ler("src/modules/painel/components/IndicadorEfetivo/index.jsx"),
    ler("src/modules/painel/components/IndicadorEfetivo/index.module.css"),
    ler("src/modules/painel/components/ModalPainel/index.jsx"),
  ]);
  assert.match(componente, /<ModalPainel/);
  assert.match(componente, /Abrir leitura operacional/);
  assert.match(componente, /Por dimensão/);
  assert.match(componente, /Composição do dia/);
  assert.match(componente, /usePaginacao\(itens, 8\)/);
  assert.match(componente, /usePaginacao\(linhas, 15\)/);
  assert.match(componente, /<PaginacaoQuadro/);
  assert.match(css, /\.chamadaLeitura/);
  assert.match(css, /\.modalLeituraOperacional/);
  assert.match(css, /\.abasLeituraOperacional/);
  assert.match(css, /\.paginacaoQuadro/);
  assert.match(modal, /role="dialog"/);
  assert.match(modal, /evento\.key === "Escape"/);
});

test("comparativo só ocupa espaço quando a comparação anual está ativa", async () => {
  const [componente, css] = await Promise.all([
    ler("src/modules/painel/components/IndicadorEfetivo/index.jsx"),
    ler("src/modules/painel/components/IndicadorEfetivo/index.module.css"),
  ]);

  assert.match(componente, /comparar \? estilos\.corpoComComparacao : ""/);
  assert.match(componente, /\{comparar && \(\s*<aside className=\{estilos\.comparativoCard\}/);
  assert.match(css, /\.corpoComComparacao/);
  assert.doesNotMatch(componente, /Ative a comparação acima/);
});

test("modal operacional não estica quadros curtos e distribui alertas responsivamente", async () => {
  const css = await ler("src/modules/painel/components/IndicadorEfetivo/index.module.css");

  assert.match(css, /\.gradeDimensoes[\s\S]*align-items: start/);
  assert.match(css, /\.quadroDimensao,[\s\S]*align-self: start/);
  assert.match(css, /grid-template-columns: repeat\(auto-fit, minmax\(270px, 1fr\)\)/);
});

test("quadro dimensionado parte da alocação e usa frequência em left join", async () => {
  const [componente, dto, repositorio, servicoJava] = await Promise.all([
    ler("src/modules/painel/components/IndicadorEfetivo/index.jsx"),
    lerBack("src/main/java/com/example/siggtrat/dto/PainelEfetivoDTO.java"),
    lerBack("src/main/java/com/example/siggtrat/repositorio/PainelEfetivoRepositorio.java"),
    lerBack("src/main/java/com/example/siggtrat/servico/PainelServico.java"),
  ]);
  assert.match(componente, /Onde está a diferença/);
  assert.match(componente, /detalhamentoContratos/);
  assert.match(componente, /detalhamentoLotacoes/);
  assert.match(componente, /detalhamentoTurnos/);
  assert.match(componente, /detalhamentoCargos/);
  assert.match(componente, /Composição operacional do dia/);
  assert.match(dto, /record DetalheContrato/);
  assert.match(dto, /record DetalheDimensao/);
  assert.match(dto, /record LinhaDimensionada/);
  assert.match(repositorio, /FROM historico_vinculo_contrato h[\s\S]*LEFT JOIN frequencia f/);
  assert.match(repositorio, /COUNT\(DISTINCT h\.id_empregado\) AS efetivo_alocado/);
  assert.match(servicoJava, /resumirDimensao\(quadroDimensionado, "LOTACAO"\)/);
  assert.match(servicoJava, /resumirDimensao\(quadroDimensionado, "TURNO"\)/);
  assert.match(servicoJava, /resumirDimensao\(quadroDimensionado, "CARGO"\)/);
});

test("contrato usa efetivo previsto próprio e não resumo derivado de frequência", async () => {
  const [repositorio, servicoJava] = await Promise.all([
    lerBack("src/main/java/com/example/siggtrat/repositorio/PainelEfetivoRepositorio.java"),
    lerBack("src/main/java/com/example/siggtrat/servico/PainelServico.java"),
  ]);
  assert.match(repositorio, /COALESCE\(c\.efetivo_previsto, 0\) AS efetivo_previsto/);
  assert.match(servicoJava, /obterDetalhamentoContratos\(/);
  assert.doesNotMatch(servicoJava, /resumirDimensao\(quadroDimensionado, "CONTRATO"\)/);
});

test("indicador permite navegar pela data sem recalcular todo o painel pai", async () => {
  const componente = await ler("src/modules/painel/components/IndicadorEfetivo/index.jsx");
  assert.match(componente, /const \[referenciaSelecionada, setReferenciaSelecionada\]/);
  assert.match(componente, /mudarDia\(-1\)/);
  assert.match(componente, /mudarDia\(1\)/);
  assert.match(componente, /type="date"/);
  assert.match(componente, /max=\{hoje\}/);
  assert.match(componente, /referencia: referenciaSelecionada/);
});

test("indicador exporta PDF e XLSX com semântica contratual e ícones adequados", async () => {
  const [componente, exportacao] = await Promise.all([
    ler("src/modules/painel/components/IndicadorEfetivo/index.jsx"),
    ler("src/modules/painel/components/IndicadorEfetivo/exportacao.js"),
  ]);
  assert.match(componente, /BiLogoAdobe/);
  assert.match(componente, /FaFileExcel/);
  assert.match(exportacao, /Efetivo previsto/);
  assert.match(exportacao, /Pendentes de lançamento/);
  assert.match(exportacao, /Lançamentos ÷ efetivo alocado/);
  assert.match(exportacao, /jspdf-autotable/);
  assert.match(exportacao, /baixarArquivoXlsx/);
  assert.doesNotMatch(exportacao, /Efetivo acionado|mediaEfetivo/);
});

test("perfil sem detalhamento operacional não recebe lotação turno cargo no payload dimensional", async () => {
  const servicoJava = await lerBack("src/main/java/com/example/siggtrat/servico/PainelServico.java");
  const repositorio = await lerBack("src/main/java/com/example/siggtrat/repositorio/PainelEfetivoRepositorio.java");
  assert.match(servicoJava, /if \(contexto\.permiteDetalhamentoOperacional\(\)\)/);
  assert.match(servicoJava, /detalhamentoLotacoes = List\.of\(\)/);
  assert.match(servicoJava, /detalhamentoTurnos = List\.of\(\)/);
  assert.match(servicoJava, /detalhamentoCargos = List\.of\(\)/);
  assert.match(repositorio, /obterDetalhamentoContratos/);
});

test("selects do indicador respeitam escopo real e combinações disponíveis", async () => {
  const [componente, servicoReact, servicoJava, repositorio] = await Promise.all([
    ler("src/modules/painel/components/IndicadorEfetivo/index.jsx"),
    ler("src/services/painel/ServicoPainel.js"),
    lerBack("src/main/java/com/example/siggtrat/servico/PainelServico.java"),
    lerBack("src/main/java/com/example/siggtrat/repositorio/PainelEfetivoRepositorio.java"),
  ]);
  assert.match(componente, /buscarFiltrosEfetivo\(parametrosFiltros/);
  assert.match(componente, /selecionarContrato/);
  assert.match(componente, /selecionarLotacao/);
  assert.match(componente, /selecionarTurno/);
  assert.match(servicoReact, /url: "\/painel\/efetivo\/filtros", params/);
  assert.match(servicoJava, /listarCargos\([\s\S]*contexto\.superintendenciaId\(\)/);
  assert.match(repositorio, /SELECT DISTINCT c\.id_cargo AS id, c\.nome AS nome[\s\S]*FROM empregado e[\s\S]*JOIN cargo c ON c\.id_cargo = e\.id_cargo/);
  assert.match(repositorio, /UPPER\(TRIM\(COALESCE\(s\.descricao, ''\)\)\) = 'ATIVO'/);
  assert.match(repositorio, /filtroEscopoEmpregado/);
  assert.doesNotMatch(repositorio, /SELECT c\.id_cargo AS id, c\.nome AS nome FROM cargo c ORDER BY c\.nome/);
});

test("indicador contratual integra o resumo e os relatórios gerais", async () => {
  const [pagina, executivo, exportacaoGeral] = await Promise.all([
    ler("src/modules/painel/pages/index.jsx"),
    ler("src/modules/painel/components/GestaoExecutiva/index.jsx"),
    ler("src/modules/painel/exportacao.js"),
  ]);
  assert.match(pagina, /indicadorEfetivoRelatorio/);
  assert.match(pagina, /aoAtualizarRelatorio=\{setIndicadorEfetivoRelatorio\}/);
  assert.match(executivo, /Atendimento contratual/);
  assert.match(executivo, /Cobertura dos alocados/);
  assert.match(executivo, /registros de.*alocados/);
  assert.match(exportacaoGeral, /INDICADOR CONTRATUAL DE EFETIVO/);
  assert.match(exportacaoGeral, /COMPOSIÇÃO OPERACIONAL DA REFERÊNCIA/);
  assert.doesNotMatch(exportacaoGeral, /efetivoAcionado|mediaEfetivo/);
});
