import test from "node:test";
import assert from "node:assert/strict";
import { readFile } from "node:fs/promises";
import {
  CHAVES_CONFIGURACAO_FRONTEND,
  criarConfiguracaoFrontend,
} from "../src/core/configuracaoFrontend.js";

const carregarEnvExemplo = async () => {
  const texto = await readFile(new URL("../.env.example", import.meta.url), "utf8");
  return Object.fromEntries(
    texto.split(/\r?\n/)
      .map((linha) => linha.trim())
      .filter((linha) => linha && !linha.startsWith("#") && linha.includes("="))
      .map((linha) => {
        const indice = linha.indexOf("=");
        return [linha.slice(0, indice), linha.slice(indice + 1)];
      }),
  );
};

test("configuração válida é normalizada e congelada", async () => {
  const ambiente = await carregarEnvExemplo();
  const config = criarConfiguracaoFrontend(ambiente, { producao: false, versao: "1.2.3" });

  assert.equal(config.API_URL, "/api");
  assert.equal(config.HTTP_TIMEOUT, 15000);
  assert.equal(config.APP_VERSION, "1.2.3");
  assert.deepEqual(config.PAGINATION_OPTIONS, [10, 50, 100, 1000]);
  assert.equal(Object.isFrozen(config), true);
  assert.equal(Object.isFrozen(config.PAGINATION_OPTIONS), true);
  assert.deepEqual(Object.keys(config).sort(), [...CHAVES_CONFIGURACAO_FRONTEND].sort());
});

test("número inválido, zero proibido e lista com NaN falham de forma explícita", async () => {
  const ambiente = await carregarEnvExemplo();
  assert.throws(
    () => criarConfiguracaoFrontend({ ...ambiente, VITE_HTTP_TIMEOUT: "NaN" }),
    /VITE_HTTP_TIMEOUT deve ser numérica/,
  );
  assert.throws(
    () => criarConfiguracaoFrontend({ ...ambiente, VITE_HTTP_TIMEOUT: "0" }),
    /VITE_HTTP_TIMEOUT não pode ser zero/,
  );
  assert.throws(
    () => criarConfiguracaoFrontend({ ...ambiente, VITE_PAGINATION_OPTIONS: "10,x,50" }),
    /VITE_PAGINATION_OPTIONS/,
  );
});

test("booleano inválido e API URL insegura são rejeitados", async () => {
  const ambiente = await carregarEnvExemplo();
  assert.throws(
    () => criarConfiguracaoFrontend({ ...ambiente, VITE_EM_MANUTENCAO: "sim" }),
    /VITE_EM_MANUTENCAO deve ser true ou false/,
  );
  assert.throws(
    () => criarConfiguracaoFrontend({ ...ambiente, VITE_API_URL: "javascript:alert(1)" }),
    /VITE_API_URL aceita somente HTTP\(S\)/,
  );
});

test("variável crítica ausente interrompe bootstrap", async () => {
  const ambiente = await carregarEnvExemplo();
  delete ambiente.VITE_API_URL;
  assert.throws(() => criarConfiguracaoFrontend(ambiente), /VITE_API_URL é obrigatória/);
});

test("credenciais de teste falham fechadas em produção", async () => {
  const ambiente = await carregarEnvExemplo();
  const config = criarConfiguracaoFrontend({
    ...ambiente,
    VITE_SHOW_TEST_CREDENTIALS: "true",
    VITE_TEST_GLOBAL_PASSWORD: "segredo-local-1",
    VITE_TEST_GESTOR_PASSWORD: "segredo-local-2",
  }, { producao: true, versao: "1" });

  assert.equal(config.SHOW_TEST_CREDENTIALS, false);
  assert.equal(config.TEST_GLOBAL_PASSWORD, "");
  assert.equal(config.TEST_GESTOR_PASSWORD, "");
});

test("credenciais de teste só são expostas quando habilitadas fora de produção", async () => {
  const ambiente = await carregarEnvExemplo();
  const config = criarConfiguracaoFrontend({
    ...ambiente,
    VITE_SHOW_TEST_CREDENTIALS: "true",
    VITE_TEST_GLOBAL_PASSWORD: "local-1",
    VITE_TEST_GESTOR_PASSWORD: "local-2",
  }, { producao: false, versao: "1" });

  assert.equal(config.SHOW_TEST_CREDENTIALS, true);
  assert.equal(config.TEST_GLOBAL_PASSWORD, "local-1");
  assert.equal(config.TEST_GESTOR_PASSWORD, "local-2");
});
test("faixas relacionadas de retry, paginação e avisos de sessão falham fechadas", async () => {
  const ambiente = await carregarEnvExemplo();
  assert.throws(
    () => criarConfiguracaoFrontend({ ...ambiente, VITE_HTTP_RETRY_BASE_MS: "3000", VITE_HTTP_RETRY_MAX_MS: "2000" }),
    /VITE_HTTP_RETRY_MAX_MS não pode ser menor/,
  );
  assert.throws(
    () => criarConfiguracaoFrontend({ ...ambiente, VITE_PAGINATION_DEFAULT_SIZE: "3000", VITE_PAGINATION_MAX_SIZE: "2000" }),
    /VITE_PAGINATION_DEFAULT_SIZE não pode exceder/,
  );
  assert.throws(
    () => criarConfiguracaoFrontend({ ...ambiente, VITE_SESSION_WARN_1MIN_MS: "300000" }),
    /VITE_SESSION_WARN_1MIN_MS deve ser menor/,
  );
});

