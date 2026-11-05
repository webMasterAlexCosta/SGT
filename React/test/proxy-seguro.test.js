// @vitest-environment node
import { expect, it } from 'vitest';
import configuracao from '../vite.config.js';

it('encaminha Origin ao Java sem removê-lo no proxy de desenvolvimento', () => {
  const cfg = configuracao({ mode: 'test' });
  const headers = { origin: 'http://127.0.0.1:5173', cookie: 'cookie-sintetico', 'x-xsrf-token': 'token-sintetico' };
  const req = { removeHeader: (nome) => { delete headers[nome]; } };
  cfg.server.proxy['/api'].configure?.({ on: (evento, callback) => { if (evento === 'proxyReq') callback(req); } });
  expect(headers.origin).toBe('http://127.0.0.1:5173');
  expect(headers.cookie).toBe('cookie-sintetico');
  expect(headers['x-xsrf-token']).toBe('token-sintetico');
});

it('verifica o certificado quando o destino do proxy usa HTTPS', () => {
  expect(configuracao({ mode: 'test' }).server.proxy['/api'].secure).toBe(true);
});
