import test from "node:test";
import assert from "node:assert/strict";
import { access, readFile } from "node:fs/promises";
import { pathToFileURL } from "node:url";
import { resolve } from "node:path";

const ler = (caminho) => readFile(new URL(caminho, import.meta.url), "utf8");
const backendRoot = process.env.SGT_BACKEND_DIR
  ? pathToFileURL(`${resolve(process.env.SGT_BACKEND_DIR)}/`)
  : new URL("../../backend/", import.meta.url);
const lerBackend = (caminho) => readFile(new URL(caminho, backendRoot), "utf8");

test("ativação revela senha temporária uma vez e redefinição sempre exige troca", async () => {
  const [pagina, modal, redefinicao, servicoJava, gestaoAcessoJava, dtoJava] = await Promise.all([
    ler("../src/modules/admin/pages/PaginaUsuarios/index.jsx"),
    ler("../src/modules/admin/components/ModalSenhaTemporaria/index.jsx"),
    ler("../src/modules/admin/pages/PaginaUsuarios/components/ModalRedefinirSenha/index.jsx"),
    lerBackend("src/main/java/com/example/siggtrat/servico/UsuarioAplicacaoServico.java"),
    lerBackend("src/main/java/com/example/siggtrat/servico/GestaoAcessoHierarquicoServico.java"),
    lerBackend("src/main/java/com/example/siggtrat/dto/RedefinicaoSenhaUsuarioAplicacaoDTO.java"),
  ]);

  assert.match(pagina, /const resposta = await ServicoUsuario\.ativar/);
  assert.match(pagina, /setCredencialTemporaria\(resposta\)/);
  assert.match(modal, /temporaryPassword/);
  assert.match(modal, /apenas agora/);
  assert.match(redefinicao, /obrigatória no próximo login/);
  assert.doesNotMatch(pagina, /forceChangeOnNextLogin:/);
  assert.doesNotMatch(dtoJava, /boolean/);
  assert.match(servicoJava, /aplicarNovaSenhaAdministrativa\(user, dto\.newPassword\(\), true\)/);
  assert.match(gestaoAcessoJava, /geradorSenhaTemporaria\.gerar\(\)/);
});

test("relatório exporta turno, descanso e totais físico/noturno/pagável", async () => {
  const [exportacaoFront, exportacaoJava, tabela] = await Promise.all([
    ler("../src/modules/rh/pages/PaginaRelatorioFrequencia/exportacao.js"),
    lerBackend("src/main/java/com/example/siggtrat/servico/RelatorioExportacaoServico.java"),
    ler("../src/modules/rh/pages/PaginaRelatorioFrequencia/components/TabelaRelatorio/index.jsx"),
  ]);
  assert.match(exportacaoFront, /obterUrlExportacaoFrequencia/);
  for (const rotulo of ["TURNO", '"D"', "H.REAL", "H.NOT", "H.PAG"]) {
    assert.match(exportacaoJava, new RegExp(rotulo.replace(".", "\\.")));
  }
  assert.match(tabela, /empregado\.turno/);
  assert.match(tabela, /totais\?\.descansos/);
  assert.match(tabela, /totais\?\.horasFisicas/);
  assert.match(tabela, /totais\?\.horasPagaveis/);
});

test("páginas administrativas possuem subcomponentes com CSS próprio", async () => {
  const arquivos = [
    "../src/modules/admin/pages/PaginaAgencias/components/CabecalhoAgencias/index.module.css",
    "../src/modules/admin/pages/PaginaRegistrosAuditoria/components/TabelaAuditoria/index.module.css",
    "../src/modules/admin/pages/PaginaCalendarioFolgas/components/ModalEvento/index.module.css",
    "../src/modules/admin/pages/PaginaEstruturaOrganizacional/components/GradeGestoresEstaduais/index.module.css",
    "../src/modules/admin/pages/PaginaLotacoes/components/ListaLotacoes/index.module.css",
    "../src/modules/admin/pages/PaginaUsuarios/components/TabelaUsuarios/index.module.css",
    "../src/modules/rh/pages/PaginaRelatorioFrequencia/components/TabelaRelatorio/index.module.css",
  ];
  await Promise.all(arquivos.map((arquivo) => access(new URL(arquivo, import.meta.url))));
});

test("controller de lotações delega a regra para um serviço Java único", async () => {
  const [controller, service] = await Promise.all([
    lerBackend("src/main/java/com/example/siggtrat/controlador/LotacaoControlador.java"),
    lerBackend("src/main/java/com/example/siggtrat/servico/LotacaoServico.java"),
  ]);
  assert.match(controller, /private final LotacaoServico lotacaoServico/);
  assert.doesNotMatch(controller, /LotacaoRepositorio|JornadaRepositorio|EmpregadoRepositorio/);
  assert.match(service, /listarAcessiveis/);
  assert.match(service, /associarTurnos/);
  assert.match(service, /possuiVinculos/);
});
