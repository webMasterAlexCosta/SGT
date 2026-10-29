import test from "node:test";
import assert from "node:assert/strict";
import { readFile } from "node:fs/promises";
import {
  contarPorEstado,
  ESTADO_PADRAO_ADMIN,
  ESTADOS_BRASIL,
  filtrarPorEstado,
  TODOS_OS_ESTADOS,
} from "../src/modules/admin/constants/estados.js";

const ler = (caminho) => readFile(new URL(`../${caminho}`, import.meta.url), "utf8");

test("filtro administrativo oferece as 27 UFs, todos e RJ como padrão", () => {
  assert.equal(ESTADOS_BRASIL.length, 27);
  assert.equal(new Set(ESTADOS_BRASIL.map((estado) => estado.sigla)).size, 27);
  assert.equal(ESTADO_PADRAO_ADMIN, "RJ");
  assert.equal(TODOS_OS_ESTADOS, "TODOS");
});

test("filtro e contadores separam os registros por UF", () => {
  const itens = [{ id: 1, uf: "RJ" }, { id: 2, uf: "sp" }, { id: 3, uf: "RJ" }];
  assert.deepEqual(filtrarPorEstado(itens, "RJ").map((item) => item.id), [1, 3]);
  assert.equal(filtrarPorEstado(itens, TODOS_OS_ESTADOS), itens);
  assert.deepEqual(contarPorEstado(itens), { TODOS: 3, RJ: 2, SP: 1 });
});

test("turnos e lotações iniciam no RJ para administrador e permitem todos", async () => {
  const [turnos, lotacoes, filtro] = await Promise.all([
    ler("src/modules/admin/pages/PaginaTurnos/index.jsx"),
    ler("src/modules/admin/pages/PaginaLotacoes/index.jsx"),
    ler("src/modules/admin/components/FiltroEstado/index.jsx"),
  ]);

  for (const pagina of [turnos, lotacoes]) {
    assert.match(pagina, /acessoGlobal \? ESTADO_PADRAO_ADMIN : TODOS_OS_ESTADOS/);
    assert.match(pagina, /filtrarPorEstado/);
  }
  assert.match(filtro, /Todos os estados/);
  assert.match(filtro, /ESTADOS_BRASIL\.map/);
});

test("2FA usa QR Code e, quando exigido pelo Java, não oferece bypass no login", async () => {
  const [gerenciador, qrCode, login, formulario, confirmacao, authService, contextoAuth] = await Promise.all([
    ler("src/modules/perfil/components/GerenciadorSegundoFator/index.jsx"),
    ler("src/modules/perfil/components/GerenciadorSegundoFator/components/ConfiguracaoCodigoQr/index.jsx"),
    ler("src/modules/auth/pages/Login/index.jsx"),
    ler("src/modules/auth/pages/Login/components/FormularioAcesso.jsx"),
    ler("src/core/security/useConfirmacaoSenha.jsx"),
    ler("src/services/auth/ServicoAutenticacao.js"),
    ler("src/modules/auth/context.jsx"),
  ]);

  assert.match(gerenciador, /configurarSegundoFator/);
  assert.match(gerenciador, /qrCodeDataUrl/);
  assert.match(gerenciador, /É opcional/);
  assert.match(qrCode, /alt="QR Code para configurar o autenticador"/);
  assert.match(login, /requiresTwoFactor/);
  assert.match(formulario, /segundoFatorPendente &&/);
  assert.match(formulario, /Código do autenticador/);
  assert.doesNotMatch(authService, /canSkipTwoFactor|skipTwoFactor/);
  assert.match(confirmacao, /twoFactorEnabled && user\?\.twoFactorVerified/);
  assert.match(confirmacao, /somenteConfirmacao/);
  assert.match(confirmacao, /Proteção Avançada/);
  assert.match(contextoAuth, /ESTADOS_AUTENTICACAO\.SEGUNDO_FATOR_REQUERIDO/);
  assert.match(contextoAuth, /if \(!perfilCanonico \|\| !autoridadeCanonica\) return null/);
});
