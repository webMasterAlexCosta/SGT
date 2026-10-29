import test from 'node:test';
import assert from 'node:assert/strict';
import { readFile } from 'node:fs/promises';
import { resolve } from 'node:path';

const backend = process.env.SGT_BACKEND_DIR;
const lerBackend = (caminho) => readFile(resolve(backend, caminho), 'utf8');
const lerReact = (caminho) => readFile(new URL(`../${caminho}`, import.meta.url), 'utf8');

test('Etapa 09: os três controladores Java totalizam 15 endpoints e o serviço React cobre todos', async () => {
  assert.ok(backend, 'SGT_BACKEND_DIR é obrigatório');
  const [react, comp, adit, rem] = await Promise.all([
    lerReact('src/services/admin/ServicoAlteracoesContrato.js'),
    lerBackend('src/main/java/com/example/siggtrat/controlador/ComposicaoContratualControlador.java'),
    lerBackend('src/main/java/com/example/siggtrat/controlador/AditamentoContratualControlador.java'),
    lerBackend('src/main/java/com/example/siggtrat/controlador/RemanejamentoContratualControlador.java'),
  ]);
  for (const trecho of ['/composicoes', '/composicoes/excedentes', '/aditamentos', '/aditamentos/previa', '/remanejamentos', '/remanejamentos/previa', '/retificar', '/cancelar']) assert.match(react, new RegExp(trecho.replaceAll('/', '\\/')));
  assert.equal((comp.match(/@(Get|Post|Put|Delete)Mapping/g) || []).length, 5);
  assert.equal((adit.match(/@(Get|Post|Put|Delete)Mapping/g) || []).length, 5);
  assert.equal((rem.match(/@(Get|Post|Put|Delete)Mapping/g) || []).length, 5);
});

test('Etapa 09: escrita de composição direta é restrita a rascunho e permite efetivo zero', async () => {
  const [java, react] = await Promise.all([
    lerBackend('src/main/java/com/example/siggtrat/servico/ComposicaoContratualServico.java'),
    lerReact('src/modules/admin/pages/PaginaContratos/components/PainelAlteracoesContrato/index.jsx'),
  ]);
  assert.match(java, /obterRascunhoAcessivel/);
  assert.match(java, /efetivoPrevisto\(\)[\s\S]{0,160}< 0|efetivoPrevisto[^\n]{0,100}< 0/);
  assert.match(react, /rascunho && podeGerenciar/);
  assert.match(react, /min="0"/);
});

test('Etapa 09: aditamento exige token de prévia imediatamente anterior e revalida ocupação', async () => {
  const [java, dominio, painel] = await Promise.all([
    lerBackend('src/main/java/com/example/siggtrat/servico/AditamentoContratualServico.java'),
    lerReact('src/services/admin/ServicoDominioAlteracoesContrato.js'),
    lerReact('src/modules/admin/pages/PaginaContratos/components/PainelAlteracoesContrato/index.jsx'),
  ]);
  assert.match(java, /Aditamento exige prévia imediatamente anterior/);
  assert.match(java, /tokenPrevia/);
  assert.match(java, /ocupacaoEm/);
  assert.match(dominio, /previaContinuaValida/);
  assert.match(painel, /setPreviaAditamento\(invalidarPrevia\(\)\)/);
});

test('Etapa 09: redução global acima de 25% e ocupação são decisões autoritativas do Java', async () => {
  const [java, painel] = await Promise.all([
    lerBackend('src/main/java/com/example/siggtrat/servico/AditamentoContratualServico.java'),
    lerReact('src/modules/admin/pages/PaginaContratos/components/PainelAlteracoesContrato/index.jsx'),
  ]);
  assert.match(java, /25%|limite normal de 25/);
  assert.match(java, /reducaoExcepcional/);
  assert.match(java, /confirmarImpacto/);
  assert.match(painel, /exigeAutorizacaoExcepcional/);
  assert.match(painel, /possuiImpactoOcupacao/);
  assert.match(painel, /Nenhuma pessoa será movida automaticamente/);
});

test('Etapa 09: remanejamento exige duas composições, variações não nulas e soma zero', async () => {
  const [java, dominio] = await Promise.all([
    lerBackend('src/main/java/com/example/siggtrat/servico/RemanejamentoContratualServico.java'),
    lerReact('src/services/admin/ServicoDominioAlteracoesContrato.js'),
  ]);
  assert.match(java, /pelo menos duas composições existentes/);
  assert.match(java, /variação diferente de zero/);
  assert.match(java, /soma das variações deve ser zero/);
  assert.match(dominio, /somaVariacoesRemanejamento/);
  assert.match(dominio, /pelo menos duas composições/);
});

test('Etapa 09: retificações são versionadas por precedência e cancelamento só vale para fatos futuros', async () => {
  const [adit, rem, painel] = await Promise.all([
    lerBackend('src/main/java/com/example/siggtrat/servico/AditamentoContratualServico.java'),
    lerBackend('src/main/java/com/example/siggtrat/servico/RemanejamentoContratualServico.java'),
    lerReact('src/modules/admin/pages/PaginaContratos/components/PainelAlteracoesContrato/index.jsx'),
  ]);
  for (const fonte of [adit, rem]) {
    assert.match(fonte, /TipoRelacaoPrecedenciaNegocial\.RETIFICA/);
    assert.match(fonte, /Somente .* futuro|Somente .* futuro pode ser cancelado|Somente .* futuro pode ser cancelado antes de produzir efeito/i);
  }
  assert.match(painel, /Retificar/);
  assert.match(painel, /Cancelar futuro/);
});

test('Etapa 09: GET de composição aceita data e excedente expõe desde-quando e justificativas', async () => {
  const [controlador, dto, painel] = await Promise.all([
    lerBackend('src/main/java/com/example/siggtrat/controlador/ComposicaoContratualControlador.java'),
    lerBackend('src/main/java/com/example/siggtrat/dto/ExcedenteComposicaoDTO.java'),
    lerReact('src/modules/admin/pages/PaginaContratos/components/PainelAlteracoesContrato/index.jsx'),
  ]);
  assert.match(controlador, /@RequestParam\(required = false\) LocalDate data/);
  for (const campo of ['desdeQuandoExcedente', 'justificativas', 'quantidadeExcedente']) assert.match(dto, new RegExp(campo));
  assert.match(painel, /desdeQuandoExcedente/);
  assert.match(painel, /justificativas/);
});

test('Etapa 09: React não contém comando de movimentação automática de pessoas', async () => {
  const [servico, painel] = await Promise.all([
    lerReact('src/services/admin/ServicoAlteracoesContrato.js'),
    lerReact('src/modules/admin/pages/PaginaContratos/components/PainelAlteracoesContrato/index.jsx'),
  ]);
  assert.doesNotMatch(servico, /\/vinculos|\/empregados|realocar|moverEmpregado/i);
  assert.match(painel, /pessoas não serão movidas automaticamente|Nenhuma pessoa será movida automaticamente/i);
});
