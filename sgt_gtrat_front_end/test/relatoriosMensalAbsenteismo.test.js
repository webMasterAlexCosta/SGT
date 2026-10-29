import test from "node:test";
import assert from "node:assert/strict";
import { readFile } from "node:fs/promises";
import { resolve } from "node:path";

const lerFront = (caminho) => readFile(new URL(`../${caminho}`, import.meta.url), "utf8");
const lerBack = (caminho) => readFile(resolve(process.env.SGT_BACKEND_DIR, caminho), "utf8");

test("mensal e absenteísmo usam filtros dedicados ao escopo do relatório", async () => {
  const frequencia = await lerFront("src/services/rh/ServicoFrequencia.js");
  const mensal = await lerFront("src/modules/rh/pages/PaginaRelatorioFrequencia/index.jsx");
  const absenteismo = await lerFront("src/services/rh/ServicoAbsenteismo.js");
  const aplicacao = await lerFront("src/Aplicacao.jsx");
  const barra = await lerFront("src/Leiaute/BarraLateral/index.jsx");

  assert.match(frequencia, /\/relatorios\/filtros/);
  assert.match(mensal, /carregarFiltrosRelatorio/);
  assert.match(absenteismo, /carregarFiltrosRelatorio/);
  assert.match(aplicacao, /visaoConsolidadaRelatorios/);
  assert.match(barra, /permissoes\.visaoConsolidadaRelatorios/);
});

test("absenteísmo aceita todos os contratos, lotações e turnos", async () => {
  const pagina = await lerFront("src/modules/rh/pages/PaginaRelatorioAbsenteismo/index.jsx");
  const servico = await lerFront("src/services/rh/ServicoAbsenteismo.js");

  assert.doesNotMatch(servico, /CONTRATO_OBRIGATORIO/);
  assert.match(pagina, /TODOS OS CONTRATOS/);
  assert.match(pagina, /TODAS AS LOTAÇÕES/);
  assert.match(pagina, /TODOS OS TURNOS/);
  assert.match(pagina, /disabled=\{carregando\}/);
  assert.match(pagina, /turnosDisponiveis/);
});

test("backend mantém escopo por perfil e processa grandes bases em lotes", async () => {
  assert.ok(process.env.SGT_BACKEND_DIR, "SGT_BACKEND_DIR é obrigatório para o teste integrado");
  const [controller, acesso, consolidado, frequencia] = await Promise.all([
    lerBack("src/main/java/com/example/siggtrat/controlador/RelatorioControlador.java"),
    lerBack("src/main/java/com/example/siggtrat/servico/AcessoOrganizacionalServico.java"),
    lerBack("src/main/java/com/example/siggtrat/servico/RelatorioFrequenciaConsolidadoServico.java"),
    lerFront("src/services/rh/ServicoFrequencia.js"),
  ]);

  const inicioFrequencia = controller.indexOf('@GetMapping("/frequencia-consolidada")');
  const fimFrequencia = controller.indexOf('@GetMapping("/frequencia-consolidada/paginada")');
  const blocoFrequencia = controller.slice(inicioFrequencia, fimFrequencia);
  assert.doesNotMatch(blocoFrequencia, /PERFIL_PRESIDENTE/);
  assert.match(controller, /PERFIL_GESTOR_ESTADUAL/);
  assert.equal(controller.includes("PERFIL_OPERACIONAL"), false);
  assert.match(controller, /@RequestParam\(required = false\) Long contratoId/);
  assert.match(acesso, /podeAcessarAlocacaoRelatorio/);
  assert.match(consolidado, /TAMANHO_LOTE_FREQUENCIAS = 1000/);
  assert.match(consolidado, /buscarIdsColaboradoresAposId/);
  assert.match(consolidado, /entityManager\.clear\(\)/);
  assert.match(consolidado, /processados \+= ids\.size\(\)/);
  assert.match(consolidado, /alocacoesPorEmpregado/);
  assert.match(consolidado, /AbsenteismoAcumulado/);
  assert.match(frequencia, /absenteismo\/resumo[\s\S]{0,120}timeout:\s*0/);
});

test("absenteísmo usa resumo global para indicadores e Top 10, sem confundir a página atual com ranking nacional", async () => {
  const [pagina, servico, resumoDto] = await Promise.all([
    lerFront("src/modules/rh/pages/PaginaRelatorioAbsenteismo/index.jsx"),
    lerFront("src/services/rh/ServicoAbsenteismo.js"),
    lerBack("src/main/java/com/example/siggtrat/dto/RelatorioAbsenteismoResumoDTO.java"),
  ]);

  assert.match(resumoDto, /List<RelatorioAbsenteismoDTO> maioresTaxas/);
  assert.match(servico, /maioresTaxas:\s*extrairLista\(resumo\?\.maioresTaxas\)/);
  assert.match(pagina, /indicadores\.maioresTaxas/);
  assert.match(pagina, /Top 10 maiores taxas de absenteísmo/);
  assert.match(pagina, /DETALHAMENTO DE ABSENTEÍSMO POR COLABORADOR/);
  assert.match(pagina, /resumoRequisicaoRef/);
  assert.match(pagina, /setLotacaoSelecionada\(""\)[\s\S]{0,80}setTurnoSelecionado\(""\)/);
  assert.doesNotMatch(pagina, /dados\.slice\(0,\s*10\)/);
});

test("banco novo cria histórico completo antes do primeiro relatório", async () => {
  assert.ok(process.env.SGT_BACKEND_DIR, "SGT_BACKEND_DIR é obrigatório para o teste integrado");
  const inicializador = await lerBack("src/main/java/com/example/siggtrat/configuracao/InicializadorDados.java");

  assert.match(inicializador, /historico\.setLotacao\(empregado\.getLotacao\(\)\)/);
  assert.match(inicializador, /historico\.setJornada\(empregado\.getJornada\(\)\)/);
  assert.match(inicializador, /historico\.setCargo\(empregado\.getCargo\(\)\)/);
  assert.match(inicializador, /historico\.setLotacaoSnapshotNome/);
  assert.match(inicializador, /historico\.setJornadaSnapshotDescricao/);
  assert.match(inicializador, /preencherSnapshotJornada\(historico, empregado\.getJornada\(\)\)/);
  assert.match(inicializador, /historico\.setJornadaSnapshotDiasTrabalho/);
  assert.match(inicializador, /historico\.setJornadaSnapshotMinutosDia/);
  assert.match(inicializador, /jornada\.getDias\(\)\.size\(\)/);
});

test("consolidação especial do RJ fica restrita à carga de desenvolvimento e preserva histórico", async () => {
  const [inicializador, historicoRepo] = await Promise.all([
    lerBack("src/main/java/com/example/siggtrat/configuracao/InicializadorDados.java"),
    lerBack("src/main/java/com/example/siggtrat/repositorio/HistoricoVinculoContratoRepositorio.java"),
  ]);

  const inicioRunner = inicializador.indexOf("CommandLineRunner consolidarLotacoesRioDeJaneiro(");
  const inicioCondicao = inicializador.lastIndexOf("@ConditionalOnProperty", inicioRunner);
  const cabecalho = inicializador.slice(inicioCondicao, inicioRunner);
  assert.match(cabecalho, /app\.seed\.terceirizados-enabled/);
  assert.match(inicializador, /historicoVinculoContratoRepositorio\.existsByLotacao_Id/);
  assert.match(historicoRepo, /boolean existsByLotacao_Id\(Long idLotacao\)/);
});

test("gestor estadual recebe leitura da tela e da API de feriados e folgas", async () => {
  const [roles, seguranca] = await Promise.all([
    lerFront("src/core/security/roles.js"),
    lerBack("src/main/java/com/example/siggtrat/configuracao/ConfiguracaoSeguranca.java"),
  ]);

  assert.match(roles, /podeVisualizarCalendario[\s\S]{0,320}AUTORIDADES\.GESTOR_ESTADUAL/);
  assert.match(roles, /podeGerenciarCalendario[\s\S]{0,120}AUTORIDADES\.GESTOR_LOCAL/);

  const correspondencia = seguranca.match(
    /HttpMethod\.GET,\s*"\/api\/calendario-eventos\/\*\*"\s*\)\.hasAnyAuthority\(([\s\S]*?)\)\s*\.requestMatchers\(\s*"\/api\/calendario-eventos\/\*\*"/,
  );
  assert.ok(correspondencia, "Regra GET do calendário deve existir antes da regra de escrita.");
  const leituraCalendario = correspondencia[1];
  assert.match(leituraCalendario, /PERFIL_ADMINISTRADOR/);
  assert.match(leituraCalendario, /PERFIL_GESTOR_NACIONAL/);
  assert.match(leituraCalendario, /PERFIL_PRESIDENTE/);
  assert.match(leituraCalendario, /PERFIL_GESTOR_ESTADUAL/);
  assert.match(leituraCalendario, /PERFIL_GESTOR_LOCAL/);
  assert.match(leituraCalendario, /PERFIL_OPERACIONAL/);
});
