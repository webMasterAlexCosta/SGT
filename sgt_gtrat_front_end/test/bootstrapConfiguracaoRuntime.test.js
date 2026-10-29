import test from "node:test";
import assert from "node:assert/strict";
import { readFile } from "node:fs/promises";

const ler = (caminho) => readFile(new URL(`../${caminho}`, import.meta.url), "utf8");

test("bootstrap carrega /config.json antes de decidir manutenção e aplica precedência do backend", async () => {
  const [aplicacao, paginaManutencao] = await Promise.all([
    ler("src/Aplicacao.jsx"),
    ler("src/core/ui/PaginaManutencao.jsx"),
  ]);

  assert.match(aplicacao, /ServicoSistema\.carregarRuntimeConfig\(urlBaseBackend\)/);
  assert.match(aplicacao, /combinarConfiguracaoEfetiva\(CONFIG, configuracaoServidor\)/);
  assert.match(aplicacao, /if \(!configuracaoEfetiva\) return <CarregamentoPagina \/>;/);
  assert.match(aplicacao, /configuracaoEfetiva\.EM_MANUTENCAO/);
  assert.match(aplicacao, /previsaoRetorno=\{configuracaoEfetiva\.PREVISAO_RETORNO\}/);

  assert.match(paginaManutencao, /previsaoRetorno = CONFIG\.PREVISAO_RETORNO/);
  assert.match(paginaManutencao, /const stringAlvo = previsaoRetorno;/);
  assert.doesNotMatch(paginaManutencao, /const stringAlvo = CONFIG\.PREVISAO_RETORNO;/);
});
