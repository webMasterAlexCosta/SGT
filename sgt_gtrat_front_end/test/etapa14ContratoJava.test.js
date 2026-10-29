import test from "node:test";
import assert from "node:assert/strict";
import { readFile } from "node:fs/promises";
import { resolve } from "node:path";

const backend = process.env.SGT_BACKEND_DIR;
if (!backend) throw new Error("SGT_BACKEND_DIR é obrigatório para validar a Etapa 14 contra o Java definitivo.");

const lerFront = (caminho) => readFile(new URL(`../${caminho}`, import.meta.url), "utf8");
const lerBack = (caminho) => readFile(resolve(backend, caminho), "utf8");

const CONTROLES = "src/main/java/com/example/siggtrat/controlador";
const DTO = "src/main/java/com/example/siggtrat/dto";
const DOMINIO = "src/main/java/com/example/siggtrat/dominio";

function blocoEntre(texto, inicio, fim) {
  const a = texto.indexOf(inicio);
  assert.ok(a >= 0, `Trecho inicial não encontrado: ${inicio}`);
  const b = fim ? texto.indexOf(fim, a + inicio.length) : texto.length;
  assert.ok(b > a, `Trecho final não encontrado: ${fim}`);
  return texto.slice(a, b);
}

test("Etapa 14 cobre os 29 endpoints previstos pelos sete controladores Java", async () => {
  const nomes = [
    "CalendarioEventoControlador.java",
    "FechamentoFrequenciaControlador.java",
    "FrequenciaAdicionalControlador.java",
    "FrequenciaExcecaoControlador.java",
    "OcorrenciaControlador.java",
    "PendenciaAdministrativaControlador.java",
    "PermissaoExcepcionalControlador.java",
  ];
  const controladores = await Promise.all(nomes.map((nome) => lerBack(`${CONTROLES}/${nome}`)));
  const total = controladores.reduce((soma, codigo) => soma + (codigo.match(/@(Get|Post|Put|Delete|Patch)Mapping/g) || []).length, 0);
  assert.equal(total, 29);

  const [frequencia, ocorrencia, pendencia, permissao] = await Promise.all([
    lerFront("src/services/rh/ServicoFrequencia.js"),
    lerFront("src/services/rh/ServicoOcorrencia.js"),
    lerFront("src/services/rh/ServicoPendenciaAdministrativa.js"),
    lerFront("src/services/admin/ServicoPermissaoExcepcional.js"),
  ]);
  for (const rota of ["/calendario-eventos", "/frequencias/fechamentos", "/frequencias-excecoes", "/frequencias-adicionais"]) {
    assert.match(frequencia, new RegExp(rota.replaceAll("/", "\\/")));
  }
  assert.match(ocorrencia, /\/ocorrencias/);
  assert.match(pendencia, /\/pendencias/);
  assert.match(permissao, /\/permissoes-excepcionais/);
});

test("fechamento assíncrono respeita 202, recuperação, cancelamento sem senha e reprocessamento com senha", async () => {
  const [controlador, dto, servicoFront, logica, painel] = await Promise.all([
    lerBack(`${CONTROLES}/FechamentoFrequenciaControlador.java`),
    lerBack(`${DTO}/FechamentoFrequenciaDTO.java`),
    lerFront("src/services/rh/ServicoFrequencia.js"),
    lerFront("src/modules/rh/pages/PaginaFrequencia/useLogicaFrequencia.js"),
    lerFront("src/modules/rh/pages/PaginaFrequencia/components/PainelFechamentoFrequencia.jsx"),
  ]);
  assert.match(controlador, /HttpStatus\.ACCEPTED/);
  assert.match(controlador, /@GetMapping\("\/atual"\)/);
  const cancelamento = blocoEntre(controlador, '@PostMapping("/{id}/cancelamento")', '@PostMapping("/{id}/reprocessamento")');
  assert.doesNotMatch(cancelamento, /@ExigirSenha/);
  const reprocessamento = controlador.slice(controlador.indexOf('@PostMapping("/{id}/reprocessamento")'));
  assert.match(reprocessamento, /@ExigirSenha/);
  for (const campo of ["status", "totalPrevisto", "totalProcessado", "totalInserido", "totalAtualizado", "totalPreservado", "totalAlteracoes", "percentual", "tentativa", "detalheErro"]) {
    assert.match(dto, new RegExp(`\\b${campo}\\b`));
  }
  for (const rota of ["/fechamentos/atual", "/cancelamento", "/reprocessamento"]) assert.match(servicoFront, new RegExp(rota.replaceAll("/", "\\/")));
  assert.match(logica, /calcularEsperaPolling/);
  assert.match(logica, /buscarFechamentoAtual/);
  assert.match(logica, /recuperarFechamentoAtual/);
  assert.match(logica, /\["ERRO",\s*"CANCELADO"\]/);
  assert.match(painel, /percentual/);
});

test("calendário conserva tipos aceitos pelo Java e não transforma sugestões em enum do navegador", async () => {
  const [requestJava, pagina, modal] = await Promise.all([
    lerBack(`${DTO}/CalendarioEventoRequisicao.java`),
    lerFront("src/modules/admin/pages/PaginaCalendarioFolgas/index.jsx"),
    lerFront("src/modules/admin/pages/PaginaCalendarioFolgas/components/ModalEvento/index.jsx"),
  ]);
  assert.match(requestJava, /String\s+tipo/);
  assert.doesNotMatch(pagina, /listaEventos[\s\S]{0,100}?\.filter\([^\n]*(FERIADO|DIA_DESCANSO)/);
  assert.match(modal, /<input list="tipos-evento-calendario"/);
  assert.match(modal, /outros tipos devolvidos\/aceitos pelo Java permanecem disponíveis/);
});

test("ocorrência envia todos os campos multipart do Java e mantém empregadoId documentado como compatibilidade legada", async () => {
  const [controlador, servico, pagina] = await Promise.all([
    lerBack(`${CONTROLES}/OcorrenciaControlador.java`),
    lerFront("src/services/rh/ServicoOcorrencia.js"),
    lerFront("src/modules/rh/pages/PaginaOcorrencias/index.jsx"),
  ]);
  for (const campo of ["empregadoId", "motivo", "dataOcorrencia", "observacao", "tipoRo", "numeroSei", "dias", "anexos"]) {
    assert.match(controlador, new RegExp(`"${campo}"`));
    assert.match(servico, new RegExp(campo));
  }
  assert.match(servico, /Compatibilidade temporária: o contrato Java de ocorrência ainda exige empregadoId/);
  assert.doesNotMatch(servico, /formulario\.append\(['"]dias['"],\s*1\)/);
  assert.match(pagina, /tipoRo/);
  assert.match(pagina, /numeroSei/);
  for (const mime of ["application/pdf", "image/jpeg", "image/png", "image/webp", "text/plain", "application/msword", "application/vnd.openxmlformats-officedocument.wordprocessingml.document"]) assert.match(pagina, new RegExp(mime.replaceAll("/", "\\/")));
});

test("pendências administrativas ficam restritas aos quatro perfis definidos no controlador Java", async () => {
  const [controlador, roles, pagina, servico] = await Promise.all([
    lerBack(`${CONTROLES}/PendenciaAdministrativaControlador.java`),
    lerFront("src/core/security/roles.js"),
    lerFront("src/modules/rh/pages/PaginaPendenciasAdministrativas/index.jsx"),
    lerFront("src/services/rh/ServicoPendenciaAdministrativa.js"),
  ]);
  for (const perfil of ["PERFIL_ADMINISTRADOR", "PERFIL_GESTOR_NACIONAL", "PERFIL_GESTOR_ESTADUAL", "PERFIL_GESTOR_LOCAL"]) assert.match(controlador, new RegExp(perfil));
  assert.doesNotMatch(controlador, /PERFIL_PRESIDENTE|PERFIL_OPERACIONAL/);
  const funcao = blocoEntre(roles, "export const podeAcessarPendenciasAdministrativas", "export const podeAcessarPermissoesExcepcionais");
  for (const papel of ["ADMINISTRADOR", "GESTOR_NACIONAL", "GESTOR_ESTADUAL", "GESTOR_LOCAL"]) assert.match(funcao, new RegExp(`AUTORIDADES\\.${papel}`));
  assert.doesNotMatch(funcao, /PRESIDENTE|OPERACIONAL/);
  assert.match(servico, /iniciar-tratamento/);
  assert.match(servico, /confirmar-resolucao/);
  assert.match(pagina, /acaoSugerida/);
});

test("estados e tipos de pendência exibidos pelo React derivam literalmente dos enums Java", async () => {
  const [estadoJava, tipoJava, pagina] = await Promise.all([
    lerBack(`${DOMINIO}/pendencia/EstadoPendencia.java`),
    lerBack(`${DOMINIO}/pendencia/TipoPendencia.java`),
    lerFront("src/modules/rh/pages/PaginaPendenciasAdministrativas/index.jsx"),
  ]);
  for (const valor of ["ABERTA", "EM_TRATAMENTO", "RESOLVIDA"]) {
    assert.match(estadoJava, new RegExp(`\\b${valor}\\b`));
    assert.match(pagina, new RegExp(`\\b${valor}\\b`));
  }
  for (const valor of ["RETIFICACAO_FREQUENCIA", "FIM_NATURAL_VIGENCIA", "CONFLITO_REPLAY_TEMPORAL"]) {
    assert.match(tipoJava, new RegExp(`\\b${valor}\\b`));
    assert.match(pagina, new RegExp(`\\b${valor}\\b`));
  }
});

test("permissões excepcionais usam os cinco tipos Java, leitura por cinco perfis e gestão somente pelo Administrador", async () => {
  const [enumJava, seguranca, servicoJava, roles, servicoFront] = await Promise.all([
    lerBack(`${DOMINIO}/acesso/TipoPermissaoExcepcional.java`),
    lerBack("src/main/java/com/example/siggtrat/configuracao/ConfiguracaoSeguranca.java"),
    lerBack("src/main/java/com/example/siggtrat/servico/PermissaoExcepcionalServico.java"),
    lerFront("src/core/security/roles.js"),
    lerFront("src/services/admin/ServicoPermissaoExcepcional.js"),
  ]);
  const tipos = [
    "CORRECAO_IDENTIFICADOR_CONTRATO",
    "REDUCAO_CONTRATUAL_EXCEPCIONAL",
    "TRANSFERENCIA_GESTAO_LOCAL",
    "ACESSO_DADOS_PESSOAIS_RESTRITOS",
    "INTERVENCAO_NEGOCIAL_TECNICA",
  ];
  for (const tipo of tipos) {
    assert.match(enumJava, new RegExp(`\\b${tipo}\\b`));
    assert.match(servicoFront, new RegExp(`['"]${tipo}['"]`));
  }
  const trechoSeguranca = blocoEntre(seguranca, 'HttpMethod.GET,\n                                "/api/permissoes-excepcionais/**"', "DEMAIS ENDPOINTS DA API");
  for (const perfil of ["PERFIL_ADMINISTRADOR", "PERFIL_GESTOR_NACIONAL", "PERFIL_PRESIDENTE", "PERFIL_GESTOR_ESTADUAL", "PERFIL_GESTOR_LOCAL"]) assert.match(trechoSeguranca, new RegExp(perfil));
  assert.doesNotMatch(trechoSeguranca, /PERFIL_OPERACIONAL/);
  assert.match(trechoSeguranca, /hasAuthority\(ConstantesDominio\.PERFIL_ADMINISTRADOR\)/);
  assert.match(servicoJava, /public List<UsoPermissaoExcepcionalDTO> listarUsos/);
  assert.match(blocoEntre(servicoJava, "public List<UsoPermissaoExcepcionalDTO> listarUsos", "public boolean possuiPermissaoAtiva"), /exigirAdministrador\(usuario\)/);
  assert.match(roles, /podeVisualizarUsosPermissaoExcepcional[\s\S]*AUTORIDADES\.ADMINISTRADOR/);
});

test("acesso a dados pessoais restritos continua fail-closed e registra o uso da permissão no Java", async () => {
  const pessoaServico = await lerBack("src/main/java/com/example/siggtrat/servico/PessoaServico.java");
  const trecho = blocoEntre(pessoaServico, "public PessoaDTO buscarDadosRestritos", "public Pessoa obterEntidade");
  assert.match(trecho, /PERFIL|TipoPermissaoExcepcional\.ACESSO_DADOS_PESSOAIS_RESTRITOS/);
  assert.match(trecho, /exigirERegistrarUso/);
  assert.match(trecho, /Usuário operacional não acessa o cadastro pessoal integral/);
});

test("exceções e adicionais mantêm empregadoId apenas porque os DTOs Java atuais ainda o exigem", async () => {
  const [dtoExcecao, dtoAdicional, pagina, servico] = await Promise.all([
    lerBack(`${DTO}/FrequenciaExcecaoDTO.java`),
    lerBack(`${DTO}/FrequenciaAdicionalDTO.java`),
    lerFront("src/modules/rh/pages/PaginaAjustesFrequencia/index.jsx"),
    lerFront("src/services/rh/ServicoOcorrencia.js"),
  ]);
  assert.match(dtoExcecao, /Long\s+empregadoId/);
  assert.match(dtoAdicional, /Long\s+empregadoId/);
  assert.match(pagina, /compatibilidade (?:temporária|legada)/i);
  assert.match(servico, /backend expuser Pessoa\/Vínculo como identidade do comando/);
});

test("mutações da Etapa 14 não fazem retry cego no navegador", async () => {
  const [frequencia, ocorrencia, pendencia, permissao] = await Promise.all([
    lerFront("src/services/rh/ServicoFrequencia.js"),
    lerFront("src/services/rh/ServicoOcorrencia.js"),
    lerFront("src/services/rh/ServicoPendenciaAdministrativa.js"),
    lerFront("src/services/admin/ServicoPermissaoExcepcional.js"),
  ]);
  for (const codigo of [frequencia, ocorrencia, pendencia, permissao]) {
    const mutacoes = codigo.match(/method:\s*['"](?:POST|PUT|DELETE)['"][\s\S]{0,220}?retries:\s*0/g) || [];
    assert.ok(mutacoes.length > 0, "Serviço mutável sem retries: 0 explícito.");
  }
});
