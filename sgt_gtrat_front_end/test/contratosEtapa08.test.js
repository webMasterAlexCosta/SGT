import test from "node:test";
import assert from "node:assert/strict";
import { readFile } from "node:fs/promises";
import { resolve } from "node:path";

const backend = process.env.SGT_BACKEND_DIR;
const lerBackend = (caminho) => readFile(resolve(backend, caminho), "utf8");
const lerReact = (caminho) => readFile(new URL(`../${caminho}`, import.meta.url), "utf8");

test("Etapa 08: React cobre os 16 endpoints do núcleo de contrato sem antecipar comandos excepcionais", async () => {
  assert.ok(backend, "SGT_BACKEND_DIR é obrigatório para validar o contrato da Etapa 08");
  const servico = await lerReact("src/services/admin/ServicoContrato.js");
  for (const trecho of [
    "/contratos", "/opcoes", "/eventos", "/eventos/precedencias", "/versoes", "/posicao-temporal",
    "/publicar", "/inativar", "/reativar", "/encerrar-antecipadamente", "/cancelar-futuro", "/anular",
  ]) assert.match(servico, new RegExp(trecho.replaceAll("/", "\\/")));
  assert.doesNotMatch(servico, /corrigir-anulacao|transferir-gestao|transferencias-gestao/);
});

test("Etapa 08: controlador Java declara os endpoints de ciclo de vida e protege todas as mutações com ExigirSenha", async () => {
  const controlador = await lerBackend("src/main/java/com/example/siggtrat/controlador/ContratoControlador.java");
  for (const trecho of ["/opcoes", "/{id}/eventos", "/{id}/eventos/precedencias", "/{id}/versoes", "/{id}/posicao-temporal", "/{id}/publicar", "/{id}/inativar", "/{id}/reativar", "/{id}/encerrar-antecipadamente", "/{id}/cancelar-futuro", "/{id}/anular"]) {
    assert.match(controlador, new RegExp(trecho.replace(/[{}]/g, "\\$&").replaceAll("/", "\\/")));
  }
  for (const metodo of ["criar", "publicar", "atualizar", "inativar", "reativar", "encerrarAntecipadamente", "cancelarFuturo", "anular", "excluir"]) {
    const pos = controlador.indexOf(`${metodo}(`);
    assert.ok(pos > 0, `método ${metodo} não encontrado`);
    assert.match(controlador.slice(Math.max(0, pos - 380), pos), /@ExigirSenha/);
  }
});

test("Etapa 08: matriz de segurança Java dá leitura aos seis perfis e escrita somente ao Gestor Local", async () => {
  const [seguranca, roles] = await Promise.all([
    lerBackend("src/main/java/com/example/siggtrat/configuracao/ConfiguracaoSeguranca.java"),
    lerReact("src/core/security/roles.js"),
  ]);
  const posGet = seguranca.indexOf('HttpMethod.GET', seguranca.indexOf('CONTRATOS'));
  assert.ok(posGet >= 0);
  const blocoGet = seguranca.slice(posGet, posGet + 700);
  assert.match(blocoGet, /"\/api\/contratos\/\*\*"/);
  for (const p of ["PERFIL_ADMINISTRADOR", "PERFIL_GESTOR_NACIONAL", "PERFIL_PRESIDENTE", "PERFIL_GESTOR_ESTADUAL", "PERFIL_GESTOR_LOCAL", "PERFIL_OPERACIONAL"]) assert.match(blocoGet, new RegExp(p));
  const primeiraRota = seguranca.indexOf('"/api/contratos/**"', posGet);
  const posWrite = seguranca.indexOf('"/api/contratos/**"', primeiraRota + 1);
  assert.ok(posWrite > primeiraRota);
  const blocoWrite = seguranca.slice(posWrite, posWrite + 300);
  assert.match(blocoWrite, /PERFIL_GESTOR_LOCAL/);
  assert.match(roles, /podeVisualizarContratos[\s\S]{0,420}AUTORIDADES\.OPERACIONAL/);
  assert.match(roles, /podeGerenciarContratos[\s\S]{0,100}AUTORIDADES\.GESTOR_LOCAL/);
});

test("Etapa 08: React usa os campos canônicos do ContratoDTO e não mantém tipo livre/ativo como autoridade", async () => {
  const [dto, formulario, dominio] = await Promise.all([
    lerBackend("src/main/java/com/example/siggtrat/dto/ContratoDTO.java"),
    lerReact("src/modules/admin/pages/PaginaContratos/components/ModalContrato/index.jsx"),
    lerReact("src/services/admin/ServicoDominioContrato.js"),
  ]);
  for (const campo of ["numeroContrato", "anoContrato", "tipoContratacaoId", "cnpjEmpresa", "razaoSocialEmpresa", "nomeFantasiaEmpresa", "estadoNegocial", "situacaoTemporal", "habilitacaoAdministrativa", "matriculaVinculoObrigatoria"]) {
    assert.match(dto, new RegExp(`\\b${campo}\\b`));
  }
  for (const campo of ["anoContrato", "tipoContratacaoId", "cnpjEmpresa", "razaoSocialEmpresa", "nomeFantasiaEmpresa", "matriculaVinculoObrigatoria"]) assert.match(formulario, new RegExp(campo));
  assert.doesNotMatch(formulario, /name=["']ativo["']/);
  assert.match(dominio, /tipoContratacaoId/);
  assert.doesNotMatch(dominio, /\btipo:\s*String\(dados/);
});

test("Etapa 08: Java permite rascunho incompleto, mas restringe edição e descarte físico ao estado RASCUNHO", async () => {
  const servico = await lerBackend("src/main/java/com/example/siggtrat/servico/ContratoServico.java");
  assert.match(servico, /validarRascunho/);
  assert.match(servico, /RASCUNHO/);
  assert.match(servico, /estadoNegocial/);
  assert.match(servico, /excluir|descartar/i);
  const dominio = await lerReact("src/services/admin/ServicoDominioContrato.js");
  assert.match(dominio, /podeEditarRascunho\s*=\s*ehRascunho/);
  assert.match(dominio, /podeDescartarRascunho\s*=\s*ehRascunho/);
});

test("Etapa 08: publicação fica submetida às validações canônicas de empresa, vigência e composição do Java", async () => {
  const [servicoJava, acaoReact] = await Promise.all([
    lerBackend("src/main/java/com/example/siggtrat/servico/ContratoServico.java"),
    lerReact("src/modules/admin/pages/PaginaContratos/components/ModalAcaoContrato/index.jsx"),
  ]);
  for (const termo of ["tipoContratacao", "cnpj", "razaoSocial", "nomeFantasia", "dataInicio", "dataFim", "composicao"]) assert.match(servicoJava.toLowerCase(), new RegExp(termo.toLowerCase()));
  assert.match(acaoReact, /composição original consistente/);
  assert.match(acaoReact, /referenciaSei/);
});

test("Etapa 08: consulta bitemporal usa data efetiva e conhecidoAte com Instant no Java", async () => {
  const [controlador, servicoReact, detalhes] = await Promise.all([
    lerBackend("src/main/java/com/example/siggtrat/controlador/ContratoControlador.java"),
    lerReact("src/services/admin/ServicoContrato.js"),
    lerReact("src/modules/admin/pages/PaginaContratos/components/ModalDetalhesContrato/index.jsx"),
  ]);
  assert.match(controlador, /LocalDate\s+data/);
  assert.match(controlador, /Instant\s+conhecidoAte/);
  assert.match(servicoReact, /params:\s*\{\s*data,[\s\S]{0,90}conhecidoAte/);
  assert.match(detalhes, /Conhecido até/);
});

test("Etapa 08: evento Java e timeline React distinguem data efetiva, data do ato e instante de registro", async () => {
  const [dto, detalhe] = await Promise.all([
    lerBackend("src/main/java/com/example/siggtrat/dto/EventoCicloVidaContratoDTO.java"),
    lerReact("src/modules/admin/pages/PaginaContratos/components/ModalDetalhesContrato/index.jsx"),
  ]);
  for (const campo of ["dataEfetiva", "dataAto", "registradoEm"]) {
    assert.match(dto, new RegExp(`\\b${campo}\\b`));
    assert.match(detalhe, new RegExp(`evento\\.${campo}`));
  }
  assert.match(detalhe, /Efetiva:/);
  assert.match(detalhe, /Ato:/);
  assert.match(detalhe, /Registro:/);
});
