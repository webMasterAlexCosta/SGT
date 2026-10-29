import test from 'node:test';
import assert from 'node:assert/strict';
import { readFile } from 'node:fs/promises';
import { resolve } from 'node:path';

const backend = process.env.SGT_BACKEND_DIR;
const lerBackend = (caminho) => readFile(resolve(backend, caminho), 'utf8');
const lerReact = (caminho) => readFile(new URL(`../${caminho}`, import.meta.url), 'utf8');

test('perfil próprio usa a rota autenticada antes da regra administrativa de usuários e não passa pelo cache HTTP', async () => {
  assert.ok(backend, 'SGT_BACKEND_DIR é obrigatório para validar o contrato HTTP');
  const [servico, controlador, seguranca, filtroCache] = await Promise.all([
    lerReact('src/services/auth/ServicoAutenticacao.js'),
    lerBackend('src/main/java/com/example/siggtrat/controlador/UsuarioAplicacaoControlador.java'),
    lerBackend('src/main/java/com/example/siggtrat/configuracao/ConfiguracaoSeguranca.java'),
    lerBackend('src/main/java/com/example/siggtrat/configuracao/FiltroCacheConsultaHttpRedis.java'),
  ]);

  assert.match(servico, /url:\s*["']\/usuarios\/meu-perfil["']/);
  assert.match(controlador, /@GetMapping\(["']\/meu-perfil["']\)/);
  const posicaoPerfil = seguranca.indexOf('"/api/usuarios/meu-perfil"');
  const posicaoRegraAdministrativa = seguranca.indexOf('"/api/usuarios/**"');
  assert.ok(posicaoPerfil >= 0, 'a rota do perfil próprio precisa estar declarada na segurança');
  assert.ok(posicaoPerfil < posicaoRegraAdministrativa, 'a exceção autenticada deve vir antes da regra administrativa');
  assert.match(filtroCache, /uri\.equals\(["']\/api\/usuarios\/meu-perfil["']\)/);
});

test('rota de status dos empregados é idêntica no React e no Java', async () => {
  assert.ok(backend, 'SGT_BACKEND_DIR é obrigatório para validar o contrato HTTP');
  const [servico, controlador] = await Promise.all([
    lerReact('src/services/rh/ServicoEmpregado.js'),
    lerBackend('src/main/java/com/example/siggtrat/controlador/StatusEmpregadoControlador.java'),
  ]);

  assert.match(servico, /['"]\/status-empregados['"]/);
  assert.doesNotMatch(servico, /status_empregados/);
  assert.match(controlador, /@RequestMapping\("\/api\/status-empregados"\)/);
});

test('campos multipart da ocorrência são recebidos pelo Java com os mesmos nomes', async () => {
  const [servico, controlador] = await Promise.all([
    lerReact('src/services/rh/ServicoOcorrencia.js'),
    lerBackend('src/main/java/com/example/siggtrat/controlador/OcorrenciaControlador.java'),
  ]);

  const camposReact = new Set(
    [...servico.matchAll(/formulario\.append\(["']([^"']+)["']/g)].map((resultado) => resultado[1]),
  );
  for (const campo of camposReact) {
    assert.match(controlador, new RegExp(`@RequestParam\\([^)]*["']${campo}["']`));
  }
  assert.ok(camposReact.has('anexos'));
});

test('campo multipart do avatar tem o mesmo nome no React e no Java', async () => {
  const [servico, controlador] = await Promise.all([
    lerReact('src/services/perfil/ServicoPerfil.js'),
    lerBackend('src/main/java/com/example/siggtrat/controlador/PerfilControlador.java'),
  ]);
  assert.match(servico, /formData\.append\(['"]file['"]/);
  assert.match(controlador, /@RequestParam\(['"]file['"]\) MultipartFile/);
});

test('filtros de auditoria enviados pelo React são declarados pelo controlador Java', async () => {
  const [pagina, controlador] = await Promise.all([
    lerReact('src/modules/admin/pages/PaginaRegistrosAuditoria/index.jsx'),
    lerBackend('src/main/java/com/example/siggtrat/controlador/RegistroAuditoriaControlador.java'),
  ]);
  const parametros = ['action', 'username', 'entityType', 'entityId', 'requestId', 'startDate', 'endDate'];

  for (const parametro of parametros) {
    assert.match(pagina, new RegExp(`\\b${parametro}\\b`));
    assert.match(controlador, new RegExp(`@RequestParam\\(name = ["']${parametro}["']`));
  }
  assert.doesNotMatch(controlador, /Instant\s+(?:from|to)\b/);
});

test('listagem de jornadas não envia paginação que o endpoint Java não declara', async () => {
  const [servico, controlador] = await Promise.all([
    lerReact('src/services/admin/ServicoJornada.js'),
    lerBackend('src/main/java/com/example/siggtrat/controlador/JornadaControlador.java'),
  ]);

  assert.doesNotMatch(servico, /params:\s*\{\s*size/);
  assert.match(controlador, /@RequestMapping\("\/api\/jornadas"\)/);
});

test('paginação só envia skipCount para endpoints que o declaram no Java', async () => {
  const [paginacao, frequencia, empregado, controladorFrequencia, controladorEmpregado] = await Promise.all([
    lerReact('src/services/rh/paginacao.js'),
    lerReact('src/services/rh/ServicoFrequencia.js'),
    lerReact('src/services/rh/ServicoEmpregado.js'),
    lerBackend('src/main/java/com/example/siggtrat/controlador/FrequenciaControlador.java'),
    lerBackend('src/main/java/com/example/siggtrat/controlador/EmpregadoControlador.java'),
  ]);

  assert.match(paginacao, /if \(otimizarContagem\) parametros\.skipCount/);
  assert.match(frequencia, /otimizarContagem: true/);
  assert.match(empregado, /filtrados-frequencia[\s\S]{0,180}otimizarContagem: true/);
  assert.match(controladorFrequencia, /@RequestParam\(defaultValue = "false"\) boolean skipCount/);
  assert.match(controladorEmpregado, /filtrados-frequencia[\s\S]{0,700}boolean skipCount/);
});

test('corpos principais têm os mesmos campos públicos nos dois projetos', async () => {
  const pares = [
    ['src/services/auth/ServicoAutenticacao.js', 'src/main/java/com/example/siggtrat/dto/CredenciaisLoginDTO.java', ['matricula', 'password', 'totp']],
    ['src/services/auth/ServicoAutenticacao.js', 'src/main/java/com/example/siggtrat/dto/CadastroAutenticacaoDTO.java', ['username', 'email', 'matricula', 'superintendenciaId', 'gestaoLocalId']],
    ['src/services/perfil/ServicoPerfil.js', 'src/main/java/com/example/siggtrat/dto/AlteracaoSenhaPerfilDTO.java', ['currentPassword', 'newPassword', 'token']],
    ['src/services/governanca/ServicoGovernancaTerceiros.js', 'src/main/java/com/example/siggtrat/dto/FaturaStatusDTO.java', ['status', 'numeroDocumento']],
  ];

  for (const [arquivoReact, arquivoJava, campos] of pares) {
    const [fonteReact, fonteJava] = await Promise.all([lerReact(arquivoReact), lerBackend(arquivoJava)]);
    for (const campo of campos) {
      const padrao = new RegExp(`\\b${campo}\\b`);
      assert.match(fonteReact, padrao, `${arquivoReact} não envia ${campo}`);
      assert.match(fonteJava, padrao, `${arquivoJava} não recebe ${campo}`);
    }
  }
});
