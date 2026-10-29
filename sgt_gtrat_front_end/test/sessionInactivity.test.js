import test from 'node:test';
import assert from 'node:assert/strict';
import { readFile } from 'node:fs/promises';
import {
  calcularRestanteInatividade,
  criarChaveAtividadeSessao,
  estaInativa,
} from '../src/core/security/sessionInactivity.js';

const ler = (caminho) => readFile(new URL(`../${caminho}`, import.meta.url), 'utf8');

test('inatividade expira exatamente em 30 minutos', () => {
  const timeoutMs = 30 * 60 * 1000;
  const inicio = Date.UTC(2026, 7, 17, 12, 0, 0);

  assert.equal(calcularRestanteInatividade({ ultimaAtividadeEm: inicio, agora: inicio, timeoutMs }), timeoutMs);
  assert.equal(estaInativa({ ultimaAtividadeEm: inicio, agora: inicio + timeoutMs - 1, timeoutMs }), false);
  assert.equal(estaInativa({ ultimaAtividadeEm: inicio, agora: inicio + timeoutMs, timeoutMs }), true);
});

test('atividade compartilhada é isolada por usuário', () => {
  const a = criarChaveAtividadeSessao('SGT-GTRAT', { id: 10, matricula: '111' });
  const b = criarChaveAtividadeSessao('SGT-GTRAT', { id: 11, matricula: '111' });
  assert.notEqual(a, b);
  assert.match(a, /session_last_activity_10$/);
});

test('heartbeat não transforma consulta periódica em atividade falsa', async () => {
  const [hook, contexto, env, app, authService] = await Promise.all([
    ler('src/hooks/usePulsoAtividade.js'),
    ler('src/modules/auth/context.jsx'),
    ler('.env.example'),
    ler('src/Aplicacao.jsx'),
    ler('src/services/auth/ServicoAutenticacao.js'),
  ]);

  assert.match(env, /VITE_INACTIVITY_TIMEOUT_MS=1800000/);
  assert.match(env, /VITE_PING_INTERVAL_MS=300000/);
  assert.match(hook, /touch:\s*true/);
  assert.match(hook, /addEventListener\('storage'/);
  assert.match(hook, /visibilitychange/);
  assert.match(contexto, /touch:\s*false/);
  assert.match(app, /usePulsoAtividade\(user, atualizarInformacoesSessao\)/);
  assert.match(authService, /retryNonIdempotent:\s*true/);
  assert.match(authService, /skipAuthLogout:\s*true/);
});

test('contrato integrado Java aplica 30 minutos e logout revogável', async () => {
  const backend = process.env.SGT_BACKEND_DIR;
  assert.ok(backend, 'SGT_BACKEND_DIR é obrigatório para validar a sessão Java');

  const { readFile: lerArquivo } = await import('node:fs/promises');
  const [envBackend, filtro, security, authService] = await Promise.all([
    lerArquivo(`${backend}/.env`, 'utf8'),
    lerArquivo(`${backend}/src/main/java/com/example/siggtrat/seguranca/FiltroAutenticacaoJwt.java`, 'utf8'),
    lerArquivo(`${backend}/src/main/java/com/example/siggtrat/configuracao/ConfiguracaoSeguranca.java`, 'utf8'),
    lerArquivo(`${backend}/src/main/java/com/example/siggtrat/servico/AutenticacaoServico.java`, 'utf8'),
  ]);

  assert.match(envBackend, /JWT_MAX_INACTIVITY_MINUTES=30/);
  assert.match(filtro, /atingiuLimiteInatividade/);
  assert.match(filtro, /ehRegistroAtividadeExplicita/);
  assert.match(filtro, /Boolean\.parseBoolean\(request\.getParameter\("touch"\)\)/);
  assert.match(filtro, /GET\/HEAD podem ser polling/);
  assert.match(filtro, /"POST"\.equalsIgnoreCase\(method\)/);
  assert.match(
    security,
    /\.requestMatchers\([\s\S]*?HttpMethod\.POST,[\s\S]*?"\/api\/autenticacao\/encerrar-sessao"[\s\S]*?\)\.permitAll\(\)/,
  );
  assert.match(authService, /excluirPorTokenBruto\(token\)/);
  assert.match(authService, /removerTokenDoCache\(token\)/);
  assert.match(authService, /\.maxAge\(0\)/);
});
