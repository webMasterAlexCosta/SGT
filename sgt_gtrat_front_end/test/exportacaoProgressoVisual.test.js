import test from "node:test";
import assert from "node:assert/strict";
import { readFile } from "node:fs/promises";
import { pathToFileURL } from "node:url";
import { resolve } from "node:path";

const ler = (caminho) => readFile(new URL(`../${caminho}`, import.meta.url), "utf8");
const backendRoot = process.env.SGT_BACKEND_DIR
  ? pathToFileURL(`${resolve(process.env.SGT_BACKEND_DIR)}/`)
  : new URL("../../backend/", import.meta.url);
const lerBackend = (caminho) => readFile(new URL(caminho, backendRoot), "utf8");

test("exportação longa usa central visual própria com progresso, ETA e sem SweetAlert", async () => {
  const [contexto, painel, css, servico] = await Promise.all([
    ler("src/core/exportacao/context.jsx"),
    ler("src/core/exportacao/PainelExportacoes.jsx"),
    ler("src/core/exportacao/PainelExportacoes.module.css"),
    ler("src/core/exportacao/ServicoExportacaoRelatorio.js"),
  ]);

  assert.match(contexto, /INTERVALO_ATUALIZACAO_MS\s*=\s*1500/);
  assert.match(contexto, /sessionStorage/);
  assert.match(contexto, /obterUrlDownload/);
  assert.match(contexto, /document\.createElement\(\"iframe\"\)/,
    "download grande deve usar iframe invisível e nunca navegar a tela principal");
  assert.doesNotMatch(contexto, /document\.createElement\(\"a\"\)/,
    "download automático não deve trocar a rota atual por uma página de erro");
  assert.match(painel, /Baixar arquivo/);
  assert.match(painel, /Tempo restante/);
  assert.match(painel, /Lotes processados/);
  assert.doesNotMatch(painel, /tempor[aá]rio foi removido|persist[eê]ncia tempor[aá]ria|lotes no servidor/i);
  assert.match(painel, /Você pode continuar usando o sistema/);
  assert.match(css, /position:\s*fixed/);
  assert.match(css, /#00416b/i);
  assert.match(css, /--px-accent-amber:\s*#f59e0b/i);
  assert.doesNotMatch(`${contexto}\n${painel}\n${servico}`, /Swal|sweetalert/i);
  assert.match(servico, /exportacao\/solicitacao/);
  assert.match(servico, /\/relatorios\/exportacoes\/\$\{encodeURIComponent\(id\)\}/);
});

test("backend acompanha cada lote de 5000 e calcula estimativa de tempo", async () => {
  const [assincrono, exportacao, controlador] = await Promise.all([
    lerBackend("src/main/java/com/example/siggtrat/servico/RelatorioExportacaoAssincronaServico.java"),
    lerBackend("src/main/java/com/example/siggtrat/servico/RelatorioExportacaoServico.java"),
    lerBackend("src/main/java/com/example/siggtrat/controlador/RelatorioControlador.java"),
  ]);

  assert.match(exportacao, /progresso\.accept\(lote\.size\(\)\)/);
  assert.match(assincrono, /estimarRestante/);
  assert.match(assincrono, /processados \/ \(double\) decorridos/);
  assert.match(assincrono, /relatorioExportacaoExecutor/);
  assert.match(controlador, /\/exportacoes\/\{id\}/);
  assert.match(controlador, /\/exportacoes\/\{id\}\/arquivo/);
});
