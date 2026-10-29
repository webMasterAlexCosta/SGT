import test from "node:test";
import assert from "node:assert/strict";
import { readFile } from "node:fs/promises";

const ler = (caminho) => readFile(new URL(`../${caminho}`, import.meta.url), "utf8");

test("troca de senha não envia token 2FA vazio quando o segundo fator está desativado", async () => {
  const [servico, pagina] = await Promise.all([
    ler("src/services/perfil/ServicoPerfil.js"),
    ler("src/modules/perfil/pages/PaginaPerfil/index.jsx"),
  ]);

  assert.match(servico, /\.\.\.\(token \? \{ token \} : \{\}\)/);
  assert.match(pagina, /twoFactorEnabled \? dadosSenha\.token : undefined/);
});

test("troca da própria senha mantém a sessão quando o backend não exige reautenticação", async () => {
  const pagina = await ler("src/modules/perfil/pages/PaginaPerfil/index.jsx");

  assert.match(pagina, /await atualizarPerfil\(\);/);
  assert.match(pagina, /await atualizarInformacoesSessao\(\{ touch: false, silentError: true \}\);/);
  assert.match(pagina, /resultado\?\.message \|\| "Senha alterada com sucesso\."/);
  const fluxoSenha = pagina.slice(pagina.indexOf("const lidarComEnvioSenha"), pagina.indexOf("if (!user) return null"));
  assert.equal(fluxoSenha.includes("encerrarSessao"), false);
});


test("cache de avatar usa a mesma chave derivada do APP_NAME e permanece não autoritativo", async () => {
  const [contexto, pagina] = await Promise.all([
    ler("src/modules/auth/context.jsx"),
    ler("src/modules/perfil/pages/PaginaPerfil/index.jsx"),
  ]);
  assert.match(contexto, /`\$\{CONFIG\.APP_NAME\.toLowerCase\(\)\}_foto_url`/);
  assert.match(pagina, /chaveCacheFotoPerfil = `\$\{CONFIG\.APP_NAME\.toLowerCase\(\)\}_foto_url`/);
  assert.doesNotMatch(pagina, /localStorage\.getItem\("sgt_foto_url"\)/);
  assert.match(contexto, /localStorage\.removeItem\(chaveArmazenamentoFoto\)/);
});
