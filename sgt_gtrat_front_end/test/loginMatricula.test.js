import test from "node:test";
import assert from "node:assert/strict";
import { readFile } from "node:fs/promises";

test("login envia matrícula, senha e TOTP opcional segundo o DTO Java", async () => {
  const service = await readFile(new URL("../src/services/auth/ServicoAutenticacao.js", import.meta.url), "utf8");
  const formulario = await readFile(new URL("../src/modules/auth/pages/Login/components/FormularioAcesso.jsx", import.meta.url), "utf8");

  assert.match(service, /autenticar:\s*async \(matricula, password, totp\)/);
  assert.match(service, /data:\s*\{ matricula, password, totp: totp \|\| undefined \}/);
  assert.doesNotMatch(service, /data:\s*\{ username, password \}/);
  assert.doesNotMatch(service, /canSkipTwoFactor|skipTwoFactor/);
  assert.match(formulario, /estadoTela === "LOGIN"/);
  assert.match(formulario, /rotulo="Matrícula"/);
  assert.match(formulario, /value=\{credenciais\.matricula\}/);
});

test("contexto publica usuário somente após reconhecer perfil e autoridade canônicos", async () => {
  const contexto = await readFile(new URL("../src/modules/auth/context.jsx", import.meta.url), "utf8");

  assert.match(contexto, /normalizarUsuarioAutenticado/);
  assert.match(contexto, /const perfilCanonico = obterPerfilPrincipal/);
  assert.match(contexto, /const autoridadeCanonica = obterAutoridade/);
  assert.match(contexto, /if \(!perfilCanonico \|\| !autoridadeCanonica\) return null/);
  assert.match(contexto, /buscarMeuPerfil/);
});

test("tela de frequência usa a capacidade semântica de lançamento diário", async () => {
  const hook = await readFile(new URL("../src/modules/rh/pages/PaginaFrequencia/useLogicaFrequencia.js", import.meta.url), "utf8");

  assert.match(hook, /podeAcessarFrequenciaDiaria\(user\)/);
  assert.match(hook, /podeRealizarFrequencias/);
});


test("rate limit de login só cria contagem local quando o backend informa lockout", async () => {
  const login = await readFile(new URL("../src/modules/auth/pages/Login/index.jsx", import.meta.url), "utf8");
  assert.match(login, /const segundosBloqueio = Number\(resultado\.lockout\)/);
  assert.match(login, /Number\.isFinite\(segundosBloqueio\) && segundosBloqueio > 0/);
  assert.doesNotMatch(login, /resultado\.lockout \|\| \d+/);
});
