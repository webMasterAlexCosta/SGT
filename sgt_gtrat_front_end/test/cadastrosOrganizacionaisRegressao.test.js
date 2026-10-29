import test from "node:test";
import assert from "node:assert/strict";
import { readFile } from "node:fs/promises";
import { resolve } from "node:path";
import { prepararRecursoParaEnvio } from "../src/services/admin/ServicoDominioRecursoGenerico.js";

const backend = process.env.SGT_BACKEND_DIR;
const lerBackend = (caminho) => readFile(resolve(backend, caminho), "utf8");
const lerReact = (caminho) => readFile(new URL(`../${caminho}`, import.meta.url), "utf8");

test("cadastro novo remove metadados nulos antes de enviar", () => {
  assert.deepEqual(
    prepararRecursoParaEnvio({ id: null, nome: "Supervisor", version: null }),
    { nome: "Supervisor" },
  );
  assert.deepEqual(
    prepararRecursoParaEnvio({ id: 4, descricao: "Corrente", version: 2 }),
    { id: 4, descricao: "Corrente", version: 2 },
  );
});

test("DTOs de cargo e tipo de conta aceitam versão ausente", async () => {
  assert.ok(backend, "SGT_BACKEND_DIR é obrigatório para validar o contrato HTTP");
  const [cargo, tipoConta] = await Promise.all([
    lerBackend("src/main/java/com/example/siggtrat/dto/CargoDTO.java"),
    lerBackend("src/main/java/com/example/siggtrat/dto/TipoContaDTO.java"),
  ]);

  assert.match(cargo, /Long version/);
  assert.match(tipoConta, /Long version/);
  assert.doesNotMatch(cargo, /\blong version\b/);
  assert.doesNotMatch(tipoConta, /\blong version\b/);
});

test("administrador escolhe a Gestão Local ao criar turno", async () => {
  const pagina = await lerReact("src/modules/admin/pages/PaginaTurnos/index.jsx");

  assert.match(pagina, /listaDeGestoesLocaisDisponiveis/);
  assert.match(pagina, /listaDeGestoesLocais=\{listaDeGestoesLocaisDisponiveis\}/);
  assert.doesNotMatch(pagina, /listaDeGestoesLocais=\{listaDeGestoesLocaisDoEstado\}/);
});

test("inativação estrutural invalida senhas e persiste toda a hierarquia como inativa", async () => {
  const [detalhes, superintendencia, gestaoAcesso, repositorio] = await Promise.all([
    lerBackend("src/main/java/com/example/siggtrat/servico/DetalhesUsuarioPersonalizadoServico.java"),
    lerBackend("src/main/java/com/example/siggtrat/servico/SuperintendenciaServico.java"),
    lerBackend("src/main/java/com/example/siggtrat/servico/GestaoAcessoHierarquicoServico.java"),
    lerBackend("src/main/java/com/example/siggtrat/repositorio/UsuarioAplicacaoRepositorio.java"),
  ]);

  assert.match(detalhes, /estruturaOrganizacionalAtiva/);
  assert.match(detalhes, /getSuperintendencia\(\)\.getAtivo\(\)/);
  assert.match(superintendencia, /inativarSuperintendencia/);
  assert.match(gestaoAcesso, /gestaoLocal\.setAtivo\(false\)/);
  assert.match(gestaoAcesso, /usuario\.setActive\(false\)/);
  assert.match(gestaoAcesso, /usuario\.setPassword\(codificadorSenha\.encode/);
  assert.match(gestaoAcesso, /deleteByUsername/);
  assert.match(gestaoAcesso, /removerSessoesUsuarioDoCache/);
  assert.match(repositorio, /buscarUsuariosVinculadosASuperintendencia/);
});

test("reativação estadual gera senha temporária sem reativar subordinados", async () => {
  const [controlador, gestaoAcesso, pagina, servicoReact] = await Promise.all([
    lerBackend("src/main/java/com/example/siggtrat/controlador/SuperintendenciaControlador.java"),
    lerBackend("src/main/java/com/example/siggtrat/servico/GestaoAcessoHierarquicoServico.java"),
    lerReact("src/modules/admin/pages/PaginaEstruturaOrganizacional/index.jsx"),
    lerReact("src/services/admin/ServicoOrganizacional.js"),
  ]);

  assert.match(controlador, /@PostMapping\("\/\{id\}\/ativar"\)/);
  assert.match(gestaoAcesso, /ativarGestorEstadual/);
  assert.match(gestaoAcesso, /ativarComSenhaTemporaria/);
  assert.doesNotMatch(gestaoAcesso, /gestoesLocais\.forEach\(gestaoLocal -> gestaoLocal\.setAtivo\(true\)\)/);
  assert.match(servicoReact, /`\/superintendencias\/\$\{id\}\/ativar`/);
  assert.match(pagina, /setCredencialTemporaria\(resposta\)/);
  assert.match(pagina, /Gestões Locais e usuários operacionais permanecerão inativos/);
});
