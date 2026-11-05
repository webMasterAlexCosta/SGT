import { describe, expect, it } from 'vitest';
import { api } from '../src/core/http.js';
import { criarConfiguracaoRequisicaoAxios } from '../src/core/http/configuracaoRequisicaoAxios.js';

describe('erros JSON em download binário', () => {
  it('mantém código e mensagem de conflito para o formulário decidir refazer a prévia', async () => {
    const tentativa = api.get('/contratos/1/documentos/1/arquivo', {
      responseType: 'blob', silentError: true, retries: 0,
      adapter: async (config) => {
        throw { config, response: { status: 409, headers: { 'content-type': 'application/json' },
          data: new Blob([JSON.stringify({ code: 'PREVIEW_STALE', message: 'A versão mudou. Refaça a prévia.' })], { type: 'application/json' }) } };
      },
    });
    await expect(tentativa).rejects.toMatchObject({ status: 409, code: 'PREVIEW_STALE', message: 'A versão mudou. Refaça a prévia.' });
  });
});

describe('progresso real de upload', () => {
  it('repassa o callback do formulário para o transporte Axios', () => {
    const eventos = [];
    const config = criarConfiguracaoRequisicaoAxios({ method: 'POST', url: '/contratos/1/documentos',
      data: new FormData(), onUploadProgress: (e) => eventos.push(e.loaded) });
    config.onUploadProgress?.({ loaded: 37 });
    expect(eventos).toEqual([37]);
  });
});
