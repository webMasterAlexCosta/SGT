import test from "node:test";
import assert from "node:assert/strict";
import { readFile } from "node:fs/promises";
import { resolve } from "node:path";

const backend = process.env.SGT_BACKEND_DIR;
if (!backend) throw new Error("SGT_BACKEND_DIR é obrigatório para validar a Etapa 15 contra o Java definitivo.");

const lerFront = (caminho) => readFile(new URL(`../${caminho}`, import.meta.url), "utf8");
const lerBack = (caminho) => readFile(resolve(backend, caminho), "utf8");
const CONTROLES = "src/main/java/com/example/siggtrat/controlador";

const blocoEntre = (texto, inicio, fim) => {
  const a = texto.indexOf(inicio);
  assert.ok(a >= 0, `Trecho inicial não encontrado: ${inicio}`);
  const b = fim ? texto.indexOf(fim, a + inicio.length) : texto.length;
  assert.ok(b > a, `Trecho final não encontrado: ${fim}`);
  return texto.slice(a, b);
};

test("Etapa 15 cobre os 8 endpoints dos dois controladores de painel do Java", async () => {
  const [calculo, painel, servico] = await Promise.all([
    lerBack(`${CONTROLES}/PainelCalculoControlador.java`),
    lerBack(`${CONTROLES}/PainelControlador.java`),
    lerFront("src/services/painel/ServicoPainel.js"),
  ]);
  assert.equal((calculo.match(/@(Get|Post)Mapping/g) || []).length, 2);
  assert.equal((painel.match(/@GetMapping/g) || []).length, 6);
  for (const rota of [
    "/painel/calculos", "/painel/gestao", "/painel/estatisticas", "/painel/atividades",
    "/painel/ranking-lotacoes", "/painel/efetivo/filtros", "/painel/efetivo",
  ]) assert.match(servico, new RegExp(rota.replaceAll("/", "\\/")));
  assert.match(servico, /`\/painel\/calculos\/\$\{id\}`/);
});

test("cálculo gerencial continua assíncrono, acompanha 202/poll e cancela contexto antigo", async () => {
  const [controlador, hook] = await Promise.all([
    lerBack(`${CONTROLES}/PainelCalculoControlador.java`),
    lerFront("src/modules/painel/pages/index.js"),
  ]);
  assert.match(controlador, /ResponseEntity\.status\(resultado\.status\(\)\)/);
  assert.match(hook, /ServicoPainel\.solicitarCalculo/);
  assert.match(hook, /ServicoPainel\.consultarCalculo/);
  assert.match(hook, /while \(!calculoTerminou\(resposta\)\)/);
  assert.match(hook, /controladorRef\.current\?\.abort\(\)/);
  assert.match(hook, /signal/);
  assert.doesNotMatch(hook, /window\.location\.reload|location\.reload/);
});

test("cinco perfis gerenciais seguem o matcher Java e Operacional permanece fora do painel", async () => {
  const [seguranca, roles] = await Promise.all([
    lerBack("src/main/java/com/example/siggtrat/configuracao/ConfiguracaoSeguranca.java"),
    lerFront("src/core/security/roles.js"),
  ]);
  const trechoJava = blocoEntre(seguranca, '"/api/painel/**"', '"/api/relatorios/**"');
  const trechoReact = blocoEntre(roles, "export const podeAcessarPainelGerencial", "export const podeAcessarRelatorios");
  for (const perfil of ["ADMINISTRADOR", "GESTOR_NACIONAL", "PRESIDENTE", "GESTOR_ESTADUAL", "GESTOR_LOCAL"]) {
    assert.match(trechoJava, new RegExp(`PERFIL_${perfil}`));
    assert.match(trechoReact, new RegExp(`AUTORIDADES\\.${perfil}`));
  }
  assert.doesNotMatch(trechoJava, /PERFIL_OPERACIONAL/);
  assert.doesNotMatch(trechoReact, /AUTORIDADES\.OPERACIONAL/);
});

test("Presidência mantém leitura nacional mas sem detalhamento operacional", async () => {
  const [servicoJava, componente] = await Promise.all([
    lerBack("src/main/java/com/example/siggtrat/servico/PainelServico.java"),
    lerFront("src/modules/painel/components/IndicadorEfetivo/index.jsx"),
  ]);
  assert.match(servicoJava, /"NACIONAL", "Brasil — visão por gestor estadual"/);
  assert.match(servicoJava, /!acessoOrganizacionalServico\.ehPresidente\(user\)/);
  assert.match(servicoJava, /if \(!contexto\.permiteDetalhamentoOperacional\(\)\)[\s\S]*lotacaoId = null;[\s\S]*jornadaId = null;[\s\S]*cargoId = null;/);
  assert.match(componente, /detalhamentoPermitido = filtros\.permiteDetalhamentoOperacional !== false/);
  assert.match(componente, /if \(!detalhamentoPermitido\)/);
});

test("filtros Todos usam null/ausência de parâmetro e geram painéis separados por item", async () => {
  const componente = await lerFront("src/modules/painel/components/IndicadorEfetivo/index.jsx");
  for (const texto of ["Todos os contratos", "Todas as lotações", "Todos os turnos", "Todos os cargos"]) assert.match(componente, new RegExp(texto));
  assert.match(componente, /const gruposTodos = \[/);
  for (const detalhe of ["detalhamentoContratos", "detalhamentoLotacoes", "detalhamentoTurnos", "detalhamentoCargos"]) assert.match(componente, new RegExp(detalhe));
  assert.match(componente, /Desdobramento da seleção Todos/);
  assert.match(componente, /GrupoPaineisTodos/);
  assert.match(componente, /usePaginacao\(itens, 6\)/);
});

test("React não recalcula percentuais gerenciais que o PainelEfetivoDTO já entrega", async () => {
  const [dto, indicador, visualizador, executivo, exportacaoIndicador, exportacaoPainel] = await Promise.all([
    lerBack("src/main/java/com/example/siggtrat/dto/PainelEfetivoDTO.java"),
    lerFront("src/modules/painel/components/IndicadorEfetivo/index.jsx"),
    lerFront("src/modules/painel/components/VisualizadorEfetivo/index.jsx"),
    lerFront("src/modules/painel/components/GestaoExecutiva/index.jsx"),
    lerFront("src/modules/painel/components/IndicadorEfetivo/exportacao.js"),
    lerFront("src/modules/painel/exportacao.js"),
  ]);
  assert.match(dto, /double percentualCobertura/);
  assert.match(dto, /double percentualAtendimento/);
  for (const codigo of [indicador, visualizador, executivo, exportacaoIndicador, exportacaoPainel]) {
    assert.doesNotMatch(codigo, /percentualRazao|calcularPercentual|calcularCorrelacao|descreverCorrelacao/);
  }
  assert.match(indicador, /resumo\.percentualCobertura/);
  assert.match(indicador, /resumo\.percentualAtendimento/);
  assert.match(visualizador, /resumo\.percentualCobertura/);
  assert.match(visualizador, /resumo\.percentualAtendimento/);
});

test("gráficos ficam limitados à composição visual e oferecem tabela acessível equivalente", async () => {
  const [visualizador, css] = await Promise.all([
    lerFront("src/modules/painel/components/VisualizadorEfetivo/index.jsx"),
    lerFront("src/modules/painel/components/VisualizadorEfetivo/index.module.css"),
  ]);
  for (const modo of ["LINHAS", "BARRAS", "DISPERSAO", "INFOGRAFICO"]) assert.match(visualizador, new RegExp(`id: "${modo}"`));
  assert.match(visualizador, /Transformação estritamente visual/);
  assert.match(visualizador, /const larguraVisual/);
  assert.match(visualizador, /TabelaAlternativa/);
  assert.match(visualizador, /Ver os dados desta visualização em tabela acessível/);
  assert.match(css, /\.tabelaAlternativa/);
  assert.doesNotMatch(visualizador, /Correlação .*r =|Pearson/i);
});

test("navegação temporal é reativa e o Java rejeita referência futura", async () => {
  const [componente, servicoJava] = await Promise.all([
    lerFront("src/modules/painel/components/IndicadorEfetivo/index.jsx"),
    lerBack("src/main/java/com/example/siggtrat/servico/PainelServico.java"),
  ]);
  assert.match(componente, /mudarDia\(-1\)/);
  assert.match(componente, /mudarDia\(1\)/);
  assert.match(componente, /max=\{hoje\}/);
  assert.match(componente, /controladorDadosRef\.current\?\.abort\(\)/);
  assert.match(componente, /setTimeout\(async \(\) =>/);
  assert.match(servicoJava, /if \(fim\.isAfter\(hoje\)\)/);
  assert.match(servicoJava, /A data de referência não pode estar no futuro/);
});

test("exportações usam o mesmo payload filtrado e os percentuais autoritativos da API", async () => {
  const [componente, exportacaoIndicador, exportacaoPainel] = await Promise.all([
    lerFront("src/modules/painel/components/IndicadorEfetivo/index.jsx"),
    lerFront("src/modules/painel/components/IndicadorEfetivo/exportacao.js"),
    lerFront("src/modules/painel/exportacao.js"),
  ]);
  assert.match(componente, /aoAtualizarRelatorio\?\.\(\{ dados, contexto: contextoExportacao \}\)/);
  assert.match(exportacaoIndicador, /pct\(resumo\.percentualCobertura\)/);
  assert.match(exportacaoIndicador, /pct\(resumo\.percentualAtendimento\)/);
  assert.match(exportacaoPainel, /celulaPercentual\(resumo\.percentualCobertura\)/);
  assert.match(exportacaoPainel, /textoPercentual\(resumoIndicador\.percentualCobertura\)/);
});

test("Java documenta que totais do painel são calculados no servidor", async () => {
  const servicoJava = await lerBack("src/main/java/com/example/siggtrat/servico/PainelServico.java");
  assert.match(servicoJava, /Todos os totais são calculados no servidor e filtrados pelo escopo do usuário autenticado/);
});
