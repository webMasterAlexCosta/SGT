import test from "node:test";
import assert from "node:assert/strict";
import { readFile } from "node:fs/promises";
import { resolve } from "node:path";
import {
  AUTORIDADES,
  PERFIS,
  podeAcessarFrequenciaDiaria,
  podeAcessarRelatorios,
  podeAcessarRelatoriosAdministrativos,
  podeGerenciarEmpregados,
  podeVisualizarEmpregados,
  podeVisualizarFrequencia,
} from "../src/core/security/roles.js";

const backend = process.env.SGT_BACKEND_DIR;
const lerFront = (caminho) => readFile(new URL(`../${caminho}`, import.meta.url), "utf8");
const lerBack = (caminho) => readFile(resolve(backend, caminho), "utf8");
const usuario = (perfil, autoridade) => ({ perfil, autoridade });

test("cadastro público exige seleção UF → Gestão Local sem escolha automática", async () => {
  const [pagina, formulario, servico, dto] = await Promise.all([
    lerFront("src/modules/auth/pages/Login/index.jsx"),
    lerFront("src/modules/auth/pages/Login/components/FormularioAcesso.jsx"),
    lerFront("src/services/auth/ServicoAutenticacao.js"),
    lerBack("src/main/java/com/example/siggtrat/dto/CadastroAutenticacaoDTO.java"),
  ]);

  assert.match(servico, /\/autenticacao\/gestoes-locais/);
  assert.match(servico, /data: \{ username, email, matricula, superintendenciaId, gestaoLocalId \}/);
  assert.match(dto, /Long superintendenciaId/);
  assert.match(dto, /Long gestaoLocalId/);
  assert.match(pagina, /campo === "superintendenciaId" \? \{ gestaoLocalId: "" \}/);
  assert.match(pagina, /if \(!credenciais\.gestaoLocalId\)/);
  assert.match(formulario, /aria-label="Gestão Local"/);
  assert.match(formulario, /!credenciais\.superintendenciaId/);
  assert.doesNotMatch(pagina, /gestoesLocais\[0\]/);
  assert.doesNotMatch(formulario, /gestoesLocais\[0\]/);
});

test("backend valida unidade ativa e pertencente à UF informada", async () => {
  const autenticacao = await lerBack("src/main/java/com/example/siggtrat/servico/AutenticacaoServico.java");

  assert.match(autenticacao, /gestaoLocalRepositorio\.findById\(gestaoLocalId\)/);
  assert.match(autenticacao, /getSuperintendencia\(\)\.getId\(\)/);
  assert.match(autenticacao, /A Gestão Local selecionada não pertence à Gestão Estadual\/UF informada/);
  assert.match(autenticacao, /Gestão Local inválida ou inativa/);
});

test("carga inicial cria GTRAT, GEDIS e GETRA em todas as UFs com perfil canônico", async () => {
  const inicializador = await lerBack("src/main/java/com/example/siggtrat/configuracao/InicializadorDados.java");

  assert.match(inicializador, /List\.of\("GTRAT", "GEDIS", "GETRA"\)/);
  assert.match(inicializador, /for \(String sigla : SIGLAS_GESTOES_LOCAIS_INICIAIS\)/);
  assert.match(inicializador, /for \(GestaoLocal gestaoLocal : listarGestoesLocais/);
  assert.match(inicializador, /PerfilAcesso\.GESTOR_LOCAL/);
});

test("seeds de desenvolvimento registram conclusão e não repetem a carga", async () => {
  const [inicializador, propriedades, env] = await Promise.all([
    lerBack("src/main/java/com/example/siggtrat/configuracao/InicializadorDados.java"),
    lerBack("src/main/resources/application.properties"),
    lerBack(".env"),
  ]);

  assert.match(inicializador, /executarCargaTransacionalUmaVez/);
  assert.match(inicializador, /INTERNO_SEED_HOMOLOGACAO_GESTAO_LOCAL_V1/);
  assert.match(inicializador, /INTERNO_SEED_TERCEIRIZADOS_V1/);
  assert.match(inicializador, /configuracaoRepositorio\.existsById\(chave\)/);
  assert.match(inicializador, /app\.seed\.unidades-operacionais-enabled/);
  assert.match(propriedades, /APP_SEED_EXECUTAR_APENAS_UMA_VEZ:true/);
  assert.match(env, /APP_SEED_EXECUTAR_APENAS_UMA_VEZ=true/);
});

test("cache autenticado inclui Gestão Estadual e Gestão Local no escopo", async () => {
  const chaveCache = await lerBack("src/main/java/com/example/siggtrat/configuracao/ServicoChaveCacheRedis.java");

  assert.match(chaveCache, /superintendenciaId/);
  assert.match(chaveCache, /gestaoLocalId/);
  assert.match(chaveCache, /username \+ "\|" \+ authorities \+ "\|" \+ superintendenciaId \+ "\|" \+ gestaoLocalId/);
});

test("Gestor Estadual tem leitura sem herdar escrita operacional", async () => {
  const [seguranca, relatorioServico, relatorioRepositorio] = await Promise.all([
    lerBack("src/main/java/com/example/siggtrat/configuracao/ConfiguracaoSeguranca.java"),
    lerBack("src/main/java/com/example/siggtrat/servico/RelatorioTerceirizadosServico.java"),
    lerBack("src/main/java/com/example/siggtrat/repositorio/RelatorioTerceirizadosRepositorio.java"),
  ]);
  const gestor = usuario(PERFIS.GESTOR_ESTADUAL, AUTORIDADES.GESTOR_ESTADUAL);

  assert.equal(podeVisualizarFrequencia(gestor), true);
  assert.equal(podeAcessarFrequenciaDiaria(gestor), false);
  assert.equal(podeVisualizarEmpregados(gestor), true);
  assert.equal(podeGerenciarEmpregados(gestor), false);
  assert.match(seguranca, /"\/api\/frequencias\/\*\*"[\s\S]{0,320}PERFIL_GESTOR_ESTADUAL/);
  assert.match(relatorioServico, /superintendenciaEfetiva\(usuario\)/);
  assert.match(relatorioRepositorio, /gl\.id_superintendencia = :superintendenciaId/);
});

test("Presidência recebe leitura administrativa nacional e não vira perfil operacional", async () => {
  const [seguranca, acesso] = await Promise.all([
    lerBack("src/main/java/com/example/siggtrat/configuracao/ConfiguracaoSeguranca.java"),
    lerBack("src/main/java/com/example/siggtrat/servico/AcessoOrganizacionalServico.java"),
  ]);
  const presidente = usuario(PERFIS.PRESIDENTE, AUTORIDADES.PRESIDENTE);

  assert.equal(podeAcessarRelatorios(presidente), false);
  assert.equal(podeAcessarRelatoriosAdministrativos(presidente), true);
  assert.equal(podeAcessarFrequenciaDiaria(presidente), false);
  assert.match(seguranca, /"\/api\/relatorios\/\*\*"[\s\S]{0,320}PERFIL_PRESIDENTE/);
  assert.match(acesso, /podeAcessarRelatoriosGestao[\s\S]{0,220}ehPresidente\(user\)/);
});

test("usuário operacional não recebe capacidade de relatório", async () => {
  const [seguranca, acesso, controlador] = await Promise.all([
    lerBack("src/main/java/com/example/siggtrat/configuracao/ConfiguracaoSeguranca.java"),
    lerBack("src/main/java/com/example/siggtrat/servico/AcessoOrganizacionalServico.java"),
    lerBack("src/main/java/com/example/siggtrat/controlador/RelatorioControlador.java"),
  ]);
  const operacional = usuario(PERFIS.OPERACIONAL, AUTORIDADES.OPERACIONAL);
  assert.equal(podeAcessarRelatorios(operacional), false);

  const regraHttp = seguranca.slice(seguranca.indexOf("* RELATÓRIOS"), seguranca.indexOf("* GOVERNANÇA DE TERCEIROS"));
  const regraDominio = acesso.slice(acesso.indexOf("public boolean podeAcessarRelatoriosGestao"), acesso.indexOf("public boolean podeAcessarAlocacaoRelatorio"));
  assert.equal(regraHttp.includes("PERFIL_OPERACIONAL"), false);
  assert.equal(regraDominio.includes("ehOperacional(user)"), false);
  assert.equal(controlador.includes("PERFIL_OPERACIONAL"), false);
});

test("transferência preserva e autoriza o escopo histórico por período", async () => {
  const [historicoEmpregado, historicoContrato, consultaHistorico, empregados] = await Promise.all([
    lerBack("src/main/java/com/example/siggtrat/entidade/HistoricoEmpregado.java"),
    lerBack("src/main/java/com/example/siggtrat/entidade/HistoricoVinculoContrato.java"),
    lerBack("src/main/java/com/example/siggtrat/servico/HistoricoEmpregadoConsultaServico.java"),
    lerBack("src/main/java/com/example/siggtrat/servico/EmpregadoServico.java"),
  ]);

  for (const fonte of [historicoEmpregado, historicoContrato]) {
    assert.match(fonte, /gestaoLocalSnapshotId/);
    assert.match(fonte, /superintendenciaSnapshotId/);
  }
  assert.match(consultaHistorico, /findByEmpregado_IdAndGestaoLocalSnapshotIdOrderByDataEventoDesc/);
  assert.match(consultaHistorico, /findByEmpregado_IdAndSuperintendenciaSnapshotIdOrderByDataEventoDesc/);
  assert.match(empregados, /item\.getGestaoLocalSnapshotId\(\)/);
  assert.match(empregados, /item\.getSuperintendenciaSnapshotId\(\)/);
});
