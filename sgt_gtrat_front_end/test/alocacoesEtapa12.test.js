import test from "node:test";
import assert from "node:assert/strict";
import { readFile } from "node:fs/promises";
import { resolve } from "node:path";

const backend = process.env.SGT_BACKEND_DIR;
const lerFront = (caminho) => readFile(new URL(`../${caminho}`, import.meta.url), "utf8");
const lerBack = (caminho) => readFile(resolve(backend, caminho), "utf8");

test("Etapa 12 integra exatamente os oito endpoints temporais do Java", async () => {
  const [servico, controlador] = await Promise.all([
    lerFront("src/services/rh/ServicoVinculoContratual.js"),
    lerBack("src/main/java/com/example/siggtrat/controlador/VinculoContratualControlador.java"),
  ]);
  for (const trecho of [
    "/{id}/alocacoes", "/{id}/alocacoes/eventos", "/{id}/alocacoes/posicao", "/contratos/{contratoId}/opcoes-alocacao",
    "/{id}/previas/alocacoes", "/{id}/alocacoes/{alocacaoId}/retificar", "/{id}/alocacoes/{alocacaoId}/cancelar-programacao",
  ]) assert.match(controlador, new RegExp(trecho.replace(/[{}]/g, ".")));
  for (const trecho of ["listarAlocacoes", "listarEventosAlocacao", "consultarPosicaoAlocacao", "obterOpcoesAlocacao", "gerarPreviaAlocacao", "alterarAlocacao", "retificarAlocacao", "cancelarProgramacaoAlocacao"]) {
    assert.match(servico, new RegExp(`\\b${trecho}\\b`));
  }
});

test("opções encadeadas usam AbortController e chave de contexto por nível", async () => {
  const modal = await lerFront("src/modules/rh/pages/PaginaPessoasVinculos/components/ModalAlocacoesVinculo.jsx");
  assert.match(modal, /new AbortController\(\)/);
  assert.match(modal, /criarChaveContextoOpcoesAlocacao/);
  assert.match(modal, /estadoAtual\.chave !== chave/);
  assert.match(modal, /controlador\.signal\.aborted/);
  assert.match(modal, /LOTACOES/);
  assert.match(modal, /JORNADAS/);
  assert.match(modal, /CARGOS/);
});

test("prévia de alocação é invalidada quando contexto ou dados auditáveis mudam", async () => {
  const modal = await lerFront("src/modules/rh/pages/PaginaPessoasVinculos/components/ModalAlocacoesVinculo.jsx");
  assert.match(modal, /tokenPrevia:\s*""/);
  assert.match(modal, /confirmarImpacto:\s*false/);
  assert.match(modal, /setPrevia\(null\)/);
  assert.match(modal, /incompatibilidades/);
  assert.match(modal, /confirmarPrecedenciaUltimoDia/);
});

test("linha temporal lista por YearMonth e posição usa conhecidoAte", async () => {
  const [modal, controlador] = await Promise.all([
    lerFront("src/modules/rh/pages/PaginaPessoasVinculos/components/ModalAlocacoesVinculo.jsx"),
    lerBack("src/main/java/com/example/siggtrat/controlador/VinculoContratualControlador.java"),
  ]);
  assert.match(modal, /type="month"/);
  assert.match(modal, /conhecidoAte/);
  assert.match(controlador, /DateTimeFormat\(pattern = "yyyy-MM"\) YearMonth mes/);
  assert.match(controlador, /Instant conhecidoAte/);
});

test("capacidade e cobertura permanecem autoritativas no backend", async () => {
  const [modal, alocacaoServico] = await Promise.all([
    lerFront("src/modules/rh/pages/PaginaPessoasVinculos/components/ModalAlocacoesVinculo.jsx"),
    lerBack("src/main/java/com/example/siggtrat/servico/AlocacaoVinculoServico.java"),
  ]);
  assert.match(modal, /Capacidade informada pelo Java/);
  assert.doesNotMatch(modal, /efetivoPrevisto\s*[-+*/]/);
  assert.match(alocacaoServico, /garantirCobertura\(vinculo\)/);
  assert.match(alocacaoServico, /lacuna na linha temporal/);
  assert.match(alocacaoServico, /sobreposição de Alocações/);
});


test("retificação usa exatamente o início da faixa e preserva a versão anterior no Java", async () => {
  const [modal, alocacaoServico] = await Promise.all([
    lerFront("src/modules/rh/pages/PaginaPessoasVinculos/components/ModalAlocacoesVinculo.jsx"),
    lerBack("src/main/java/com/example/siggtrat/servico/AlocacaoVinculoServico.java"),
  ]);
  assert.match(modal, /dataEfetiva:\s*alocacao\.dataInicio/);
  assert.match(modal, /disabled=\{modo === "RETIFICAR"\}/);
  assert.match(alocacaoServico, /A retificação deve usar o primeiro dia da alocação informada/);
  assert.match(alocacaoServico, /TipoEventoAlocacaoVinculo\.RETIFICACAO/);
});

test("cancelamento só alcança programação futura e o Java restaura a predecessora", async () => {
  const [dominio, alocacaoServico] = await Promise.all([
    lerFront("src/services/rh/ServicoDominioAlocacaoVinculo.js"),
    lerBack("src/main/java/com/example/siggtrat/servico/AlocacaoVinculoServico.java"),
  ]);
  assert.match(dominio, /estadoNegocial === "REGISTRADA"/);
  assert.match(dominio, /hoje < alocacao\.dataInicio/);
  assert.match(alocacaoServico, /Somente alocação futura ainda registrada pode ter a programação cancelada/);
  assert.match(alocacaoServico, /predecessora\.setDataFim\(anterior\.fim\(\)\)/);
});

test("prévia retroativa explicita eventos posteriores e incompatibilidades sem reescrever fatos no React", async () => {
  const [modal, alocacaoServico] = await Promise.all([
    lerFront("src/modules/rh/pages/PaginaPessoasVinculos/components/ModalAlocacoesVinculo.jsx"),
    lerBack("src/main/java/com/example/siggtrat/servico/AlocacaoVinculoServico.java"),
  ]);
  assert.match(modal, /previa\.impactos/);
  assert.match(modal, /previa\.incompatibilidades/);
  assert.match(alocacaoServico, /eventos de alocação posteriores preservados/);
  assert.match(alocacaoServico, /revalidarFrequencias\(vinculo, req\.dataEfetiva\(\)/);
});

test("catálogo histórico continua legível e opção sem efetivo não recebe fallback local", async () => {
  const modal = await lerFront("src/modules/rh/pages/PaginaPessoasVinculos/components/ModalAlocacoesVinculo.jsx");
  assert.match(modal, /formatarCombinacao\(alocacao\)/);
  assert.match(modal, /alocacao\.dataInicio/);
  assert.match(modal, /O servidor não encontrou composição com efetivo disponível/);
  assert.match(modal, /Não há fallback local/);
});
