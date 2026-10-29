import test from "node:test";
import assert from "node:assert/strict";
import { readFile } from "node:fs/promises";
import { pathToFileURL } from "node:url";
import { resolve } from "node:path";

const ler = (caminho) => readFile(new URL(caminho, import.meta.url), "utf8");
const backendRoot = process.env.SGT_BACKEND_DIR
  ? pathToFileURL(`${resolve(process.env.SGT_BACKEND_DIR)}/`)
  : new URL("../../backend/", import.meta.url);
const lerBackend = (caminho) => readFile(new URL(caminho, backendRoot), "utf8");

test("relatório mensal usa somente a apuração histórica autoritativa", async () => {
  const fonte = await ler("../src/modules/rh/pages/PaginaRelatorioFrequencia/index.jsx");
  assert.match(fonte, /ServicoFrequencia\.gerarRelatorioConsolidado/);
  const servico = await ler("../src/services/rh/ServicoFrequencia.js");
  assert.match(servico, /\/relatorios\/frequencia-consolidada/);
  assert.match(fonte, /resposta\?\.completo !== true/);
  assert.doesNotMatch(fonte, /ServicoFrequencia\.buscarIntervalo|apurarMinutosRelatorio/);
  assert.doesNotMatch(fonte, /\.catch\(\(\) => \[\]\)/);
});

test("absenteísmo usa o denominador de escala devolvido pelo Java", async () => {
  const servico = await ler("../src/services/rh/ServicoAbsenteismo.js");
  const pagina = await ler("../src/modules/rh/pages/PaginaRelatorioAbsenteismo/index.jsx");
  assert.match(servico, /diasTrabalhaveis/);
  assert.match(servico, /ServicoFrequencia\.gerarRelatorioAbsenteismo/);
  assert.match(pagina, /ServicoAbsenteismo\.gerarRelatorio/);
  assert.doesNotMatch(servico + pagina, /CONFIG\.ABSENTEISMO_DIAS_UTEIS/);
});

test("segundo fator não possui mais endpoint que revela o código", async () => {
  const [servico, componente, auth] = await Promise.all([
    ler("../src/services/perfil/ServicoPerfil.js"),
    ler("../src/modules/perfil/components/GerenciadorSegundoFator/index.jsx"),
    ler("../src/services/auth/ServicoAutenticacao.js"),
  ]);
  assert.doesNotMatch(servico, /2fa-code/);
  assert.match(servico, /2fa\/configurar/);
  assert.match(servico, /2fa\/ativar/);
  assert.match(componente, /aplicativo autenticador/);
  assert.match(componente, /qrCodeDataUrl/);
  assert.match(auth, /totp/);
});

test("ocorrência preserva batidas e restaura status-base", async () => {
  const fonte = await lerBackend("src/main/java/com/example/siggtrat/servico/OcorrenciaServico.java");
  const reconciliacao = fonte.slice(fonte.indexOf("private void reconciliarFrequenciaDia"), fonte.indexOf("private boolean abrangeData"));
  assert.match(reconciliacao, /setStatusBaseAntesOcorrencia/);
  assert.match(reconciliacao, /setOcorrenciaOverrideAtivo/);
  assert.doesNotMatch(reconciliacao, /setEntrada1\(null\)|setHorasTrabalhadas\(null\)/);
});

test("regras adicionais entram na memória oficial e módulos de ciclo estão roteados", async () => {
  const [calculo, app, governanca] = await Promise.all([
    lerBackend("src/main/java/com/example/siggtrat/servico/CalculoFrequenciaServico.java"),
    ler("../src/Aplicacao.jsx"),
    ler("../src/modules/governanca/pages/PaginaGovernancaTerceiros/index.jsx"),
  ]);
  assert.match(calculo, /calcularMinutosAdicionais/);
  assert.match(calculo, /setMinutosPagaveis/);
  assert.match(app, /governanca-terceiros/);
  for (const termo of ["Admissão", "Tabelas tarifárias", "Folhas de horas", "Faturamento", "Risco contínuo"]) {
    assert.match(governanca, new RegExp(termo, "i"));
  }
});
