import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { AxiosError } from 'axios';
import {
  api,
  extrairPagina,
  obterContextoResposta,
  requisitar,
  requisitarDownload,
  requisitarEnvelope,
} from '../../src/core/http.js';
import { invalidarTokenCsrf, resetarCoordenadorCsrfParaTestes } from '../../src/core/http/coordenadorCsrf.js';
import {
  criarErroConfirmacaoCancelada,
  registrarSolicitadorConfirmacaoSenha,
  resetarSolicitadorConfirmacaoSenhaParaTestes,
} from '../../src/core/http/coordenadorConfirmacaoSenha.js';
import { criarConfiguracaoRequisicaoAxios } from '../../src/core/http/configuracaoRequisicaoAxios.js';
import { serializarParametros } from '../../src/core/http/serializadorParametros.js';
import { serializarInstant, serializarLocalDate, serializarYearMonth } from '../../src/core/http/datasHttp.js';
import { calcularRetryAfterMs, normalizarErroHttp } from '../../src/core/http/erroHttp.js';
import { criarFormData, removerContentTypeMultipart } from '../../src/core/http/multipart.js';

const resposta = (config, status, data, headers = {}) => ({
  config,
  status,
  statusText: String(status),
  data,
  headers,
  request: {},
});

const envelope = (status, data, extras = {}) => ({
  timestamp: '2026-09-11T12:00:00Z',
  status,
  error: null,
  message: status === 201 ? 'Created' : 'OK',
  data,
  path: extras.path || '/api/teste',
  requestId: extras.requestId || 'req-servidor',
  meta: extras.meta ?? null,
});

const erroAxios = (config, status, data, headers = {}) => {
  const response = status ? resposta(config, status, data, headers) : undefined;
  return new AxiosError(
    status ? `Request failed with status code ${status}` : 'Network Error',
    status ? AxiosError.ERR_BAD_RESPONSE : AxiosError.ERR_NETWORK,
    config,
    null,
    response,
  );
};

const header = (config, nome) => config?.headers?.get?.(nome) ?? config?.headers?.[nome] ?? config?.headers?.[nome.toLowerCase()];

let adapterOriginal;

beforeEach(() => {
  adapterOriginal = api.defaults.adapter;
  resetarCoordenadorCsrfParaTestes();
  resetarSolicitadorConfirmacaoSenhaParaTestes();
  vi.spyOn(Math, 'random').mockReturnValue(0);
});

afterEach(() => {
  api.defaults.adapter = adapterOriginal;
  invalidarTokenCsrf();
  resetarSolicitadorConfirmacaoSenhaParaTestes();
  vi.restoreAllMocks();
});

describe('Etapa 03 - contrato HTTP central', () => {
  it('faz bootstrap CSRF uma única vez para mutações concorrentes e usa headerName do Java', async () => {
    let bootstraps = 0;
    const mutacoes = [];

    api.defaults.adapter = async (config) => {
      if (config.url === '/autenticacao/csrf') {
        bootstraps += 1;
        await new Promise((resolve) => setTimeout(resolve, 5));
        return resposta(config, 200, envelope(200, {
          headerName: 'X-CSRF-DINAMICO',
          parameterName: '_csrf',
          token: 'token-abc',
        }));
      }
      mutacoes.push({
        metodo: config.method,
        url: config.url,
        csrf: header(config, 'X-CSRF-DINAMICO'),
        requestId: header(config, 'X-Request-Id'),
        client: header(config, 'X-Client'),
        timezone: header(config, 'X-Timezone'),
      });
      return resposta(config, 200, envelope(200, { ok: true }));
    };

    await Promise.all([
      requisitar({ method: 'POST', url: '/teste/a', data: { a: 1 } }),
      requisitar({ method: 'PATCH', url: '/teste/b', data: { b: 2 } }),
      requisitar({ method: 'DELETE', url: '/teste/c' }),
    ]);

    expect(bootstraps).toBe(1);
    expect(mutacoes).toHaveLength(3);
    expect(mutacoes.every((item) => item.csrf === 'token-abc')).toBe(true);
    expect(mutacoes.every((item) => item.requestId && item.client && item.timezone)).toBe(true);
  });

  it('não busca CSRF em GET e preserva envelope, meta, status, headers e requestId', async () => {
    api.defaults.adapter = async (config) => resposta(
      config,
      202,
      envelope(202, { content: [{ id: 1 }] }, {
        requestId: 'req-body',
        meta: { page: 0, size: 20, totalElements: 41, totalPages: 3, first: true, last: false },
      }),
      { 'X-Request-Id': 'req-header', 'X-Teste': 'ok' },
    );

    const contexto = await requisitarEnvelope({ method: 'GET', url: '/teste/pagina' });
    expect(contexto.status).toBe(202);
    expect(contexto.pendente).toBe(true);
    expect(contexto.requestId).toBe('req-body');
    expect(contexto.meta.totalElements).toBe(41);
    expect(contexto.headers['x-teste']).toBe('ok');

    const pagina = extrairPagina(contexto);
    expect(pagina.content).toEqual([{ id: 1 }]);
  });

  it('mantém contexto de envelope acessível sem quebrar serviços legados que recebem apenas data', async () => {
    api.defaults.adapter = async (config) => resposta(config, 200, envelope(200, { id: 7 }, { requestId: 'req-7', meta: { origem: 'java' } }));
    const dados = await requisitar({ method: 'GET', url: '/teste/7' });
    expect(dados).toEqual({ id: 7 });
    const contexto = obterContextoResposta(dados);
    expect(contexto.requestId).toBe('req-7');
    expect(contexto.meta).toEqual({ origem: 'java' });
  });

  it('trata 204 de forma explícita no modo envelope sem forçar acesso a propriedades inexistentes', async () => {
    api.defaults.adapter = async (config) => resposta(config, 204, null, { 'X-Request-Id': 'req-204' });
    const contexto = await requisitarEnvelope({ method: 'GET', url: '/teste/vazio' });
    expect(contexto.semConteudo).toBe(true);
    expect(contexto.data).toBeNull();
    expect(contexto.requestId).toBe('req-204');
  });

  it('executa fluxo central 428 e faz replay único com X-User-Password sem persistir a senha', async () => {
    let chamadasMutacao = 0;
    let senhaVistaNoAdapter = null;

    registrarSolicitadorConfirmacaoSenha(async (contexto) => contexto.executar('senha-correta'));
    api.defaults.adapter = async (config) => {
      if (config.url === '/autenticacao/csrf') {
        return resposta(config, 200, envelope(200, { headerName: 'X-XSRF-TOKEN', parameterName: '_csrf', token: 'csrf-1' }));
      }
      chamadasMutacao += 1;
      if (chamadasMutacao === 1) {
        throw erroAxios(config, 428, {
          status: 428,
          code: 'PASSWORD_CONFIRMATION_REQUIRED',
          message: 'A senha de confirmação é obrigatória para esta operação.',
        }, { 'X-Request-Id': 'req-428' });
      }
      senhaVistaNoAdapter = header(config, 'X-User-Password');
      return resposta(config, 200, envelope(200, { atualizado: true }));
    };

    const resultado = await requisitar({
      method: 'PUT',
      url: '/usuarios/10',
      data: { nome: 'Teste' },
      confirmacaoSenha: { acao: 'Alterar usuário', entidade: 'Usuário 10', consequencia: 'Dados serão atualizados.' },
    });

    expect(resultado).toEqual({ atualizado: true });
    expect(chamadasMutacao).toBe(2);
    expect(senhaVistaNoAdapter).toBe('senha-correta');
  });

  it('senha incorreta retorna erro sem encerrar sessão e objeto de erro não expõe header sensível', async () => {
    registrarSolicitadorConfirmacaoSenha(async (contexto) => contexto.executar('senha-incorreta'));
    api.defaults.adapter = async (config) => {
      if (config.url === '/autenticacao/csrf') {
        return resposta(config, 200, envelope(200, { headerName: 'X-XSRF-TOKEN', parameterName: '_csrf', token: 'csrf-1' }));
      }
      if (!header(config, 'X-User-Password')) {
        throw erroAxios(config, 428, { status: 428, code: 'PASSWORD_CONFIRMATION_REQUIRED', message: 'Confirme.' });
      }
      throw erroAxios(config, 403, { status: 403, code: 'INVALID_CONFIRMATION_PASSWORD', message: 'Senha de confirmação inválida.' });
    };

    await expect(requisitar({ method: 'DELETE', url: '/teste/1', silentError: true })).rejects.toMatchObject({
      status: 403,
      code: 'INVALID_CONFIRMATION_PASSWORD',
      senhaConfirmacaoInvalida: true,
    });
  });

  it('renova CSRF apenas quando a falha é identificada e o replay foi declarado seguro', async () => {
    let bootstrap = 0;
    let mutacoes = 0;
    const tokens = [];

    api.defaults.adapter = async (config) => {
      if (config.url === '/autenticacao/csrf') {
        bootstrap += 1;
        return resposta(config, 200, envelope(200, {
          headerName: 'X-CSRF', parameterName: '_csrf', token: `token-${bootstrap}`,
        }));
      }
      mutacoes += 1;
      tokens.push(header(config, 'X-CSRF'));
      if (mutacoes === 1) {
        throw erroAxios(config, 403, { status: 403, code: 'CSRF_INVALID', message: 'Token CSRF inválido.' });
      }
      return resposta(config, 200, envelope(200, { ok: true }));
    };

    const resultado = await requisitar({ method: 'POST', url: '/teste/csrf', data: { x: 1 }, csrfRetrySeguro: true });
    expect(resultado.ok).toBe(true);
    expect(bootstrap).toBe(2);
    expect(mutacoes).toBe(2);
    expect(tokens).toEqual(['token-1', 'token-2']);
  });

  it('não repete mutação arbitrária após falha de rede e marca resultado como incerto', async () => {
    let mutacoes = 0;
    api.defaults.adapter = async (config) => {
      if (config.url === '/autenticacao/csrf') {
        return resposta(config, 200, envelope(200, { headerName: 'X-CSRF', parameterName: '_csrf', token: 'token' }));
      }
      mutacoes += 1;
      throw erroAxios(config, 0, null);
    };

    await expect(requisitar({ method: 'POST', url: '/teste/incerto', data: { x: 1 }, retries: 2, silentError: true })).rejects.toMatchObject({
      status: 0,
      resultadoIncerto: true,
      podeReenviarAutomaticamente: false,
    });
    expect(mutacoes).toBe(1);
  });

  it('GET usa retry limitado e cancelável em falha transitória', async () => {
    let tentativas = 0;
    api.defaults.adapter = async (config) => {
      tentativas += 1;
      if (tentativas === 1) throw erroAxios(config, 503, { status: 503, message: 'Indisponível.' });
      return resposta(config, 200, envelope(200, { ok: true }));
    };
    const resultado = await requisitar({ method: 'GET', url: '/teste/retry', retries: 1, retryDelayMs: 0, silentError: true });
    expect(resultado.ok).toBe(true);
    expect(tentativas).toBe(2);
  });

  it('idempotência é gerada por intenção e injetada no campo esperado pelo Java', () => {
    const primeira = criarConfiguracaoRequisicaoAxios({ method: 'POST', url: '/relatorios', data: { tipo: 'X' }, idempotencia: true });
    const chave = primeira.data.chaveIdempotencia;
    expect(chave).toMatch(/^cmd-/);
    expect(primeira.mutacaoIdempotente).toBe(true);
    expect(primeira.data.chaveIdempotencia).toBe(chave);

    const segunda = criarConfiguracaoRequisicaoAxios({ method: 'POST', url: '/relatorios', data: { tipo: 'X' }, idempotencia: true });
    expect(segunda.data.chaveIdempotencia).not.toBe(chave);
  });

  it('serializa arrays e tipos temporais no formato do Spring sem conversão UTC acidental de LocalDate', () => {
    expect(serializarParametros({ contratoIds: [1, 2], termo: 'A&B' })).toBe('contratoIds=1&contratoIds=2&termo=A%26B');
    const dataLocal = new Date(2026, 8, 11, 23, 30, 0);
    expect(serializarLocalDate(dataLocal)).toBe('2026-09-11');
    expect(serializarYearMonth(dataLocal)).toBe('2026-09');
    expect(serializarInstant('2026-09-11T12:30:00-03:00')).toBe('2026-09-11T12:30:00-03:00');
    expect(() => serializarInstant('2026-09-11T12:30:00')).toThrow(/offset/);
  });

  it('download usa blob/headers reais e sanitiza Content-Disposition', async () => {
    const blob = new Blob(['abc'], { type: 'text/plain' });
    api.defaults.adapter = async (config) => resposta(config, 200, blob, {
      'Content-Disposition': 'attachment; filename="../relatorio teste.txt"',
      'Content-Type': 'text/plain',
      'X-Request-Id': 'req-download',
    });
    const download = await requisitarDownload({ method: 'GET', url: '/relatorio/download' });
    expect(download.blob).toBe(blob);
    expect(download.nomeArquivo).toBe('relatorio teste.txt');
    expect(download.requestId).toBe('req-download');
  });

  it('mantém withCredentials habilitado e sinal AbortSignal na configuração central', () => {
    const controller = new AbortController();
    const cfg = criarConfiguracaoRequisicaoAxios({ method: 'GET', url: '/teste/sinal', signal: controller.signal });
    expect(api.defaults.withCredentials).toBe(true);
    expect(cfg.signal).toBe(controller.signal);
  });

  it('multipart remove Content-Type manual para o navegador gerar boundary e preserva nomes das partes', () => {
    const form = criarFormData({ arquivo: new Blob(['abc'], { type: 'text/plain' }), descricao: 'teste', tags: ['a', 'b'] });
    const headers = { 'Content-Type': 'multipart/form-data', 'X-Teste': 'ok' };
    removerContentTypeMultipart(headers, form);

    expect(headers).not.toHaveProperty('Content-Type');
    expect(headers['X-Teste']).toBe('ok');
    expect(form.get('descricao')).toBe('teste');
    expect(form.getAll('tags')).toEqual(['a', 'b']);
  });

  it('cancelamento durante backoff de GET interrompe retry sem gerar falso erro de negócio', async () => {
    const controller = new AbortController();
    let tentativas = 0;
    api.defaults.adapter = async (config) => {
      tentativas += 1;
      throw erroAxios(config, 503, { status: 503, message: 'Indisponível.' });
    };

    const chamada = requisitar({
      method: 'GET',
      url: '/teste/cancelavel',
      retries: 2,
      retryDelayMs: 1000,
      signal: controller.signal,
      silentError: true,
    });
    setTimeout(() => controller.abort(), 5);

    await expect(chamada).rejects.toMatchObject({
      code: 'REQUEST_CANCELED',
      tipo: 'CANCELED',
      canceled: true,
      silencioso: true,
    });
    expect(tentativas).toBe(1);
  });

  it('cancelamento da confirmação 428 não executa replay nem conserva senha', async () => {
    let mutacoes = 0;
    registrarSolicitadorConfirmacaoSenha(async () => { throw criarErroConfirmacaoCancelada(); });
    api.defaults.adapter = async (config) => {
      if (config.url === '/autenticacao/csrf') {
        return resposta(config, 200, envelope(200, { headerName: 'X-CSRF', parameterName: '_csrf', token: 'csrf-cancel' }));
      }
      mutacoes += 1;
      throw erroAxios(config, 428, { status: 428, code: 'PASSWORD_CONFIRMATION_REQUIRED', message: 'Confirme.' });
    };

    await expect(requisitar({ method: 'DELETE', url: '/teste/cancelar', silentError: true })).rejects.toMatchObject({
      code: 'PASSWORD_CONFIRMATION_CANCELED',
      canceled: true,
    });
    expect(mutacoes).toBe(1);
  });

  it('preserva a mesma chave de idempotência no replay da mesma intenção após 428', async () => {
    const chaves = [];
    registrarSolicitadorConfirmacaoSenha(async (contexto) => contexto.executar('senha-ok'));
    api.defaults.adapter = async (config) => {
      if (config.url === '/autenticacao/csrf') {
        return resposta(config, 200, envelope(200, { headerName: 'X-CSRF', parameterName: '_csrf', token: 'csrf-idem' }));
      }
      const corpo = typeof config.data === 'string' ? JSON.parse(config.data) : config.data;
      chaves.push(corpo.chaveIdempotencia);
      if (chaves.length === 1) {
        throw erroAxios(config, 428, { status: 428, code: 'PASSWORD_CONFIRMATION_REQUIRED', message: 'Confirme.' });
      }
      return resposta(config, 200, envelope(200, { ok: true }));
    };

    await requisitar({ method: 'POST', url: '/teste/idempotente', data: { valor: 1 }, idempotencia: true });
    expect(chaves).toHaveLength(2);
    expect(chaves[0]).toBeTruthy();
    expect(chaves[1]).toBe(chaves[0]);
  });

  it('não interpreta 403 genérico como CSRF nem faz replay arriscado', async () => {
    let bootstraps = 0;
    let mutacoes = 0;
    api.defaults.adapter = async (config) => {
      if (config.url === '/autenticacao/csrf') {
        bootstraps += 1;
        return resposta(config, 200, envelope(200, { headerName: 'X-CSRF', parameterName: '_csrf', token: `csrf-${bootstraps}` }));
      }
      mutacoes += 1;
      throw erroAxios(config, 403, { status: 403, message: 'Acesso negado.' });
    };

    await expect(requisitar({ method: 'POST', url: '/teste/403', data: { a: 1 }, csrfRetrySeguro: true, silentError: true })).rejects.toMatchObject({
      status: 403,
      tipo: 'AUTHORIZATION',
    });
    expect(bootstraps).toBe(1);
    expect(mutacoes).toBe(1);
  });

  it('classifica conflitos, upload e validação 422 sem expor detalhes técnicos', () => {
    const conflito = normalizarErroHttp({ config: { method: 'PUT' }, response: { status: 409, data: { message: 'Conflito de versão.' } } });
    const grande = normalizarErroHttp({ config: { method: 'POST' }, response: { status: 413, data: {} } });
    const midia = normalizarErroHttp({ config: { method: 'POST' }, response: { status: 415, data: {} } });
    const validacao = normalizarErroHttp({
      config: { method: 'POST' },
      response: { status: 422, data: { message: 'Dados inválidos.', fields: [{ field: 'nome', message: 'Obrigatório.' }] } },
    });

    expect(conflito.tipo).toBe('CONFLICT');
    expect(grande.tipo).toBe('PAYLOAD_TOO_LARGE');
    expect(midia.tipo).toBe('UNSUPPORTED_MEDIA');
    expect(validacao.tipo).toBe('VALIDATION');
    expect(validacao.fields).toEqual([{ field: 'nome', message: 'Obrigatório.' }]);
  });

  it('interpreta Retry-After e sanitiza erro técnico/PII/segredos', () => {
    expect(calcularRetryAfterMs('3', 0)).toBe(3000);
    const erro = normalizarErroHttp({
      config: { method: 'POST', headers: { 'X-Request-Id': 'req-local', 'X-User-Password': 'segredo' } },
      response: {
        status: 500,
        headers: { 'Retry-After': '2', 'X-Request-Id': 'req-server' },
        data: {
          message: 'java.lang.Exception password=abc CPF 123.456.789-00',
          password: 'abc',
          stack: 'at java.X:10',
        },
      },
    });
    expect(erro.message).toBe('O servidor não conseguiu concluir a operação.');
    expect(erro.requestId).toBe('req-server');
    expect(erro.data.password).toBe('[REDACTED]');
    expect(erro.data.stack).toBeUndefined();
    expect(erro).not.toHaveProperty('headers');
  });
});
