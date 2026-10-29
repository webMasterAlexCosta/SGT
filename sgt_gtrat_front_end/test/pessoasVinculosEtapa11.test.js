import test from "node:test";
import assert from "node:assert/strict";
import { readFile } from "node:fs/promises";
import { resolve } from "node:path";

const backend = process.env.SGT_BACKEND_DIR;
const lerFront = (caminho) => readFile(new URL(`../${caminho}`, import.meta.url), "utf8");
const lerBack = (caminho) => readFile(resolve(backend, caminho), "utf8");

test("Etapa 11 usa Pessoa e Vínculo como escrita canônica e mantém Empregado somente leitura", async () => {
  const [aplicacao, barra, legado, pessoa, vinculo] = await Promise.all([
    lerFront("src/Aplicacao.jsx"),
    lerFront("src/Leiaute/BarraLateral/index.jsx"),
    lerFront("src/services/rh/ServicoEmpregado.js"),
    lerFront("src/services/rh/ServicoPessoa.js"),
    lerFront("src/services/rh/ServicoVinculoContratual.js"),
  ]);

  assert.match(aplicacao, /Route path="\/pessoas"/);
  assert.match(aplicacao, /Route path="\/empregados"[\s\S]{0,180}Navigate to="\/pessoas" replace/);
  assert.match(barra, /Pessoas e vínculos/);
  assert.match(pessoa, /const endpoint = "\/pessoas"/);
  assert.match(vinculo, /const endpoint = "\/vinculos-contratuais"/);
  assert.doesNotMatch(legado, /method:\s*empregado\.id\s*\?\s*['"]PUT['"]\s*:\s*['"]POST['"]/);
  assert.doesNotMatch(legado, /method:\s*['"]DELETE['"][\s\S]{0,80}\/empregados/);
  assert.match(legado, /projeção somente leitura/);
});

test("listagens canônicas são paginadas no servidor com filtros temporais e de contrato", async () => {
  const [pagina, pessoaControlador, vinculoControlador] = await Promise.all([
    lerFront("src/modules/rh/pages/PaginaPessoasVinculos/index.jsx"),
    lerBack("src/main/java/com/example/siggtrat/controlador/PessoaControlador.java"),
    lerBack("src/main/java/com/example/siggtrat/controlador/VinculoContratualControlador.java"),
  ]);
  for (const campo of ["busca", "contratoId", "ativoEm", "page", "size"]) assert.match(pagina, new RegExp(`\\b${campo}\\b`));
  assert.match(pessoaControlador, /Page<PessoaResumoDTO>[\s\S]{0,380}String busca[\s\S]{0,180}Long contratoId[\s\S]{0,180}LocalDate ativoEm[\s\S]{0,180}Pageable pageable/);
  assert.match(vinculoControlador, /Page<VinculoContratualDTO>[\s\S]{0,700}LocalDate inicioDe[\s\S]{0,180}LocalDate inicioAte[\s\S]{0,180}Pageable pageable/);
});

test("dados restritos exigem finalidade e CPF permanece protegido na listagem", async () => {
  const [servico, pagina, modal] = await Promise.all([
    lerFront("src/services/rh/ServicoPessoa.js"),
    lerFront("src/modules/rh/pages/PaginaPessoasVinculos/index.jsx"),
    lerFront("src/modules/rh/pages/PaginaPessoasVinculos/components/ModalPessoa.jsx"),
  ]);
  assert.match(servico, /dados-restritos/);
  assert.match(servico, /params:\s*\{ finalidade:/);
  assert.match(pagina, /formatarCpfProtegido\(pessoa\.cpf\)/);
  assert.match(modal, /Informe a finalidade antes de revelar dados restritos/);
  assert.match(modal, /setRestritos\(null\)/);
  assert.doesNotMatch(pagina, /localStorage|sessionStorage/);
});

test("cadastro integrado respeita DTO Java, capacidade zero e confirmação de excedente", async () => {
  const [dominio, modal, dto, opcoes] = await Promise.all([
    lerFront("src/services/rh/ServicoDominioPessoaVinculo.js"),
    lerFront("src/modules/rh/pages/PaginaPessoasVinculos/components/ModalCadastroVinculo.jsx"),
    lerBack("src/main/java/com/example/siggtrat/dto/CadastroVinculoContratualRequisicao.java"),
    lerBack("src/main/java/com/example/siggtrat/dto/OpcaoAlocacaoTemporalDTO.java"),
  ]);
  for (const campo of ["pessoaId", "pessoa", "contratoId", "dataInicio", "dataFim", "matricula", "composicaoId", "confirmarInclusaoExcedente", "justificativaExcedente", "evidenciaExcedente", "autorizacaoExcedente"]) {
    assert.match(dominio, new RegExp(`\\b${campo}\\b`));
    assert.match(dto, new RegExp(`\\b${campo}\\b`));
  }
  assert.match(modal, /disponivelParaNovaAlocacao === false/);
  assert.match(modal, /exigeJustificativaExcedente/);
  assert.match(dominio, /A inclusão excedente exige confirmação, justificativa, evidência e autorização/);
  assert.match(opcoes, /efetivoPrevisto/);
  assert.match(opcoes, /ocupacaoAposInclusao/);
});

test("retificações invalidam prévia ao editar e confirmam somente token atual", async () => {
  const [modal, servico, dto] = await Promise.all([
    lerFront("src/modules/rh/pages/PaginaPessoasVinculos/components/ModalVinculo.jsx"),
    lerFront("src/services/rh/ServicoVinculoContratual.js"),
    lerBack("src/main/java/com/example/siggtrat/dto/PreviaImpactoVinculoDTO.java"),
  ]);
  assert.match(modal, /setPrevia\(null\)/);
  assert.match(modal, /tokenPrevia:\s*""/);
  assert.match(modal, /Gere a prévia e confirme os impactos atuais/);
  assert.match(servico, /previas\/retificacao-inicio/);
  assert.match(servico, /previas\/retificacao-desligamento/);
  assert.match(dto, /String token/);
  assert.match(dto, /List<String> impactos/);
});

test("Etapa 11 cobre matrícula, desligamento, cancelamentos e fim natural sem cálculo local", async () => {
  const [servico, pagina, dominio] = await Promise.all([
    lerFront("src/services/rh/ServicoVinculoContratual.js"),
    lerFront("src/modules/rh/pages/PaginaPessoasVinculos/index.jsx"),
    lerFront("src/services/rh/ServicoDominioPessoaVinculo.js"),
  ]);
  for (const trecho of ["alterar-matricula", "cancelar-programacao", "desligar", "retificar-desligamento", "cancelar-desligamento-programado", "tratar-fim-natural"]) assert.match(servico, new RegExp(trecho));
  assert.match(pagina, /Filtre um contrato específico antes de tratar o fim natural/);
  assert.match(dominio, /A matrícula anterior permanecerá reservada|prepararAlteracaoMatricula/);
  assert.doesNotMatch(pagina, /efetivoPrevisto\s*[-+*\/]/);
});

test("comandos críticos não têm repetição automática e senha não entra nos DTOs de tela", async () => {
  const [pessoa, vinculo, motivos, cadastro, acao] = await Promise.all([
    lerFront("src/services/rh/ServicoPessoa.js"),
    lerFront("src/services/rh/ServicoVinculoContratual.js"),
    lerFront("src/services/rh/ServicoMotivoDesligamento.js"),
    lerFront("src/modules/rh/pages/PaginaPessoasVinculos/components/ModalCadastroVinculo.jsx"),
    lerFront("src/modules/rh/pages/PaginaPessoasVinculos/components/ModalVinculo.jsx"),
  ]);
  for (const fonte of [pessoa, vinculo, motivos]) assert.doesNotMatch(fonte, /retries:\s*[2-9]/);
  for (const fonte of [cadastro, acao]) assert.doesNotMatch(fonte, /name=["']password["']|type=["']password["']|localStorage|sessionStorage/i);
  assert.match(vinculo, /retries:\s*0/);
});

test("perfis de Pessoa e Vínculo preservam consulta gerencial e escrita exclusiva da Gestão Local", async () => {
  const roles = await import("../src/core/security/roles.js");
  const usuario = (perfil) => ({ perfil, autoridade: roles.autoridadeDoPerfil(perfil) });
  assert.equal(roles.podeVisualizarPessoasVinculos(usuario(roles.PERFIS.ADMINISTRADOR)), true);
  assert.equal(roles.podeVisualizarPessoasVinculos(usuario(roles.PERFIS.GESTOR_NACIONAL)), true);
  assert.equal(roles.podeVisualizarPessoasVinculos(usuario(roles.PERFIS.GESTOR_ESTADUAL)), true);
  assert.equal(roles.podeVisualizarPessoasVinculos(usuario(roles.PERFIS.GESTOR_LOCAL)), true);
  assert.equal(roles.podeVisualizarPessoasVinculos(usuario(roles.PERFIS.OPERACIONAL)), false);
  assert.equal(roles.podeGerenciarPessoasVinculos(usuario(roles.PERFIS.GESTOR_LOCAL)), true);
  assert.equal(roles.podeGerenciarPessoasVinculos(usuario(roles.PERFIS.GESTOR_ESTADUAL)), false);
});
