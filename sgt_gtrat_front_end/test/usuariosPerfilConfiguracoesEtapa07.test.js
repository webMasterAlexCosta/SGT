import test from "node:test";
import assert from "node:assert/strict";
import { readFile } from "node:fs/promises";
import { resolve } from "node:path";

const backend = process.env.SGT_BACKEND_DIR;
const lerBackend = (caminho) => readFile(resolve(backend, caminho), "utf8");
const lerReact = (caminho) => readFile(new URL(`../${caminho}`, import.meta.url), "utf8");

test("Etapa 07: inventário React cobre os 19 endpoints de usuários, perfil e configurações", async () => {
  assert.ok(backend, "SGT_BACKEND_DIR é obrigatório para validar o contrato da Etapa 07");
  const [configReact, perfilReact, usuarioReact, authReact] = await Promise.all([
    lerReact("src/services/admin/ServicoConfiguracaoSistema.js"),
    lerReact("src/services/perfil/ServicoPerfil.js"),
    lerReact("src/services/admin/ServicoUsuario.js"),
    lerReact("src/services/auth/ServicoAutenticacao.js"),
  ]);

  for (const trecho of ["/sistema/configuracoes"]) assert.match(configReact, new RegExp(trecho.replaceAll("/", "\\/")));
  for (const trecho of ["/perfil/2fa/status", "/perfil/2fa/configurar", "/perfil/2fa/ativar", "/perfil/2fa", "/perfil/alterar-senha", "/perfil/validar-senha", "/perfil/avatar"]) {
    assert.match(perfilReact, new RegExp(trecho.replaceAll("/", "\\/")));
  }
  for (const trecho of ["/usuarios", "conceder-acesso-retroativo", "revogar-acesso-retroativo", "redefinir-senha", "ativar"]) {
    assert.match(usuarioReact, new RegExp(trecho.replaceAll("/", "\\/")));
  }
  assert.match(authReact, /\/usuarios\/meu-perfil/);
});

test("Etapa 07: controlador Java declara exatamente 2 endpoints administrativos e 8 endpoints de perfil", async () => {
  const [configJava, perfilJava] = await Promise.all([
    lerBackend("src/main/java/com/example/siggtrat/controlador/ConfiguracaoSistemaControlador.java"),
    lerBackend("src/main/java/com/example/siggtrat/controlador/PerfilControlador.java"),
  ]);

  assert.match(configJava, /@GetMapping\("\/api\/sistema\/configuracoes"\)/);
  assert.match(configJava, /@PostMapping\("\/api\/sistema\/configuracoes"\)/);
  const mapeamentosPerfil = [...perfilJava.matchAll(/@(Get|Post|Delete)Mapping(?:\([^\n]*\)|\([^)]*\))/g)];
  assert.equal(mapeamentosPerfil.length, 8);
  assert.match(perfilJava, /@GetMapping\("\/avatar\/\{nomeArquivo\}"\)/);
  assert.match(perfilJava, /@RequestParam\("file"\) MultipartFile/);
});

test("Etapa 07: as três chaves editáveis do React são as mesmas autorizadas pelo serviço Java", async () => {
  const [pagina, servicoJava] = await Promise.all([
    lerReact("src/modules/admin/pages/PaginaConfiguracoesSistema/index.jsx"),
    lerBackend("src/main/java/com/example/siggtrat/servico/ConfiguracaoSistemaServico.java"),
  ]);
  const chaves = ["HORAS_DIA_TRABALHO", "HORA_INICIO_NOTURNO", "MINUTOS_HORA_NOTURNA"];
  for (const chave of chaves) {
    assert.match(pagina, new RegExp(chave));
    assert.match(servicoJava, new RegExp(chave));
  }
  assert.doesNotMatch(pagina, /chave:\s*["']HORA_FIM_NOTURNO["']/);
});

test("Etapa 07: configurações administrativas são exclusivas do Administrador no Java e no React", async () => {
  const [controlador, roles] = await Promise.all([
    lerBackend("src/main/java/com/example/siggtrat/controlador/ConfiguracaoSistemaControlador.java"),
    lerReact("src/core/security/roles.js"),
  ]);
  assert.match(controlador, /@PreAuthorize\("hasAuthority\([^\n]*PERFIL_ADMINISTRADOR/);
  assert.match(roles, /podeGerenciarConfiguracoesSistema[\s\S]{0,180}AUTORIDADES\.ADMINISTRADOR/);
});

test("Etapa 07: mutações administrativas de usuário permanecem protegidas por ExigirSenha", async () => {
  const controlador = await lerBackend("src/main/java/com/example/siggtrat/controlador/UsuarioAplicacaoControlador.java");
  const assinaturas = [
    /@PostMapping\s*\n\s*@ExigirSenha[\s\S]{0,160}criar\(/,
    /conceder-acesso-retroativo"\)[\s\S]{0,80}@ExigirSenha/,
    /revogar-acesso-retroativo"\)[\s\S]{0,80}@ExigirSenha/,
    /redefinir-senha"\)[\s\S]{0,80}@ExigirSenha/,
    /@PutMapping\("\/\{id\}"\)[\s\S]{0,80}@ExigirSenha/,
    /\/\{id\}\/ativar"\)[\s\S]{0,80}@ExigirSenha/,
    /@DeleteMapping\("\/\{id\}"\)[\s\S]{0,80}@ExigirSenha/,
  ];
  for (const padrao of assinaturas) assert.match(controlador, padrao);
});

test("Etapa 07: matcher Java permite gestão de usuários apenas aos quatro perfis gerenciais", async () => {
  const seguranca = await lerBackend("src/main/java/com/example/siggtrat/configuracao/ConfiguracaoSeguranca.java");
  const inicio = seguranca.indexOf('"/api/usuarios/**"');
  assert.ok(inicio >= 0);
  const bloco = seguranca.slice(inicio, inicio + 520);
  for (const autoridade of ["PERFIL_ADMINISTRADOR", "PERFIL_GESTOR_NACIONAL", "PERFIL_GESTOR_ESTADUAL", "PERFIL_GESTOR_LOCAL"]) {
    assert.match(bloco, new RegExp(autoridade));
  }
  assert.doesNotMatch(bloco, /PERFIL_PRESIDENTE/);
  assert.doesNotMatch(bloco, /PERFIL_OPERACIONAL/);
});

test("Etapa 07: política UX de senha acompanha o backend de referência sem substituir a validação Java", async () => {
  const [politicaReact, envJava] = await Promise.all([
    lerReact("src/core/security/politicaSenha.js"),
    lerBackend(".env"),
  ]);
  assert.match(politicaReact, /SENHA_MINIMA_UI\s*=\s*8/);
  assert.match(politicaReact, /Java continua sendo a autoridade final/);
  assert.match(envJava, /^AUTH_PASSWORD_MIN_LENGTH=8$/m);
});
