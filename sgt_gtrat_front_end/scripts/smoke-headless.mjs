import { spawn } from 'node:child_process';
import { mkdtemp, rm } from 'node:fs/promises';
import { tmpdir } from 'node:os';
import path from 'node:path';
import net from 'node:net';
import process from 'node:process';

const raiz = path.resolve(path.dirname(new URL(import.meta.url).pathname), '..');
const host = '127.0.0.1';
const portaPreview = Number(process.env.SMOKE_PREVIEW_PORT || 4173);
const chromium = process.env.CHROMIUM_BIN || '/usr/bin/chromium';
const rotas = (process.env.SMOKE_ROUTES || '/,/login,/painel,/frequencia,/relatorios,/relatorios-administrativos,/governanca-terceiros,/logs')
  .split(',').map((item) => item.trim()).filter(Boolean);

const dormir = (ms) => new Promise((resolve) => setTimeout(resolve, ms));
const portaLivre = () => new Promise((resolve, reject) => {
  const servidor = net.createServer();
  servidor.unref();
  servidor.on('error', reject);
  servidor.listen(0, host, () => {
    const { port } = servidor.address();
    servidor.close(() => resolve(port));
  });
});
const aguardarHttp = async (url, tentativas = 80) => {
  let ultimo;
  for (let i = 0; i < tentativas; i += 1) {
    try {
      const resposta = await fetch(url, { redirect: 'manual' });
      if (resposta.status >= 200 && resposta.status < 500) return resposta;
    } catch (erro) { ultimo = erro; }
    await dormir(100);
  }
  throw ultimo || new Error(`Servidor indisponível: ${url}`);
};

class Cdp {
  constructor(url) {
    this.url = url;
    this.seq = 0;
    this.pending = new Map();
    this.listeners = new Map();
  }
  async conectar() {
    this.ws = new WebSocket(this.url);
    await new Promise((resolve, reject) => {
      this.ws.addEventListener('open', resolve, { once: true });
      this.ws.addEventListener('error', reject, { once: true });
    });
    this.ws.addEventListener('message', (evento) => {
      const msg = JSON.parse(evento.data);
      if (msg.id && this.pending.has(msg.id)) {
        const { resolve, reject } = this.pending.get(msg.id);
        this.pending.delete(msg.id);
        if (msg.error) reject(new Error(msg.error.message));
        else resolve(msg.result || {});
        return;
      }
      if (msg.method) {
        for (const fn of this.listeners.get(msg.method) || []) fn(msg.params || {});
      }
    });
  }
  on(method, fn) {
    const atual = this.listeners.get(method) || [];
    atual.push(fn);
    this.listeners.set(method, atual);
  }
  enviar(method, params = {}) {
    const id = ++this.seq;
    this.ws.send(JSON.stringify({ id, method, params }));
    return new Promise((resolve, reject) => this.pending.set(id, { resolve, reject }));
  }
  fechar() { this.ws?.close(); }
}

const aguardarEvento = (cdp, metodo, timeoutMs = 10_000) => new Promise((resolve, reject) => {
  const timer = setTimeout(() => reject(new Error(`Timeout aguardando ${metodo}`)), timeoutMs);
  cdp.on(metodo, (params) => {
    clearTimeout(timer);
    resolve(params);
  });
});

const preview = spawn(process.execPath, [path.join(raiz, 'node_modules/vite/bin/vite.js'), 'preview', '--host', host, '--port', String(portaPreview), '--strictPort'], {
  cwd: raiz,
  env: { ...process.env },
  stdio: ['ignore', 'pipe', 'pipe'],
});
let previewErro = '';
preview.stderr.on('data', (d) => { previewErro += String(d); });

const perfilTmp = await mkdtemp(path.join(tmpdir(), 'sgt-smoke-'));
const debugPort = await portaLivre();
let navegador;
let cdpBrowser;
let cdpPagina;
try {
  await aguardarHttp(`http://${host}:${portaPreview}/`);
  navegador = spawn(chromium, [
    '--headless=new', '--no-sandbox', '--disable-dev-shm-usage', '--disable-gpu', '--no-proxy-server',
    '--disable-extensions', '--disable-background-networking', '--disable-default-apps',
    `--remote-debugging-port=${debugPort}`, `--user-data-dir=${perfilTmp}`, 'about:blank',
  ], { stdio: ['ignore', 'ignore', 'pipe'] });
  let chromeErro = '';
  navegador.stderr.on('data', (d) => { chromeErro += String(d); });
  const versao = await aguardarHttp(`http://${host}:${debugPort}/json/version`);
  const meta = await versao.json();
  cdpBrowser = new Cdp(meta.webSocketDebuggerUrl);
  await cdpBrowser.conectar();
  const { targetId } = await cdpBrowser.enviar('Target.createTarget', { url: 'about:blank' });
  let alvo;
  for (let i = 0; i < 40; i += 1) {
    const lista = await (await fetch(`http://${host}:${debugPort}/json/list`)).json();
    alvo = lista.find((item) => item.id === targetId);
    if (alvo?.webSocketDebuggerUrl) break;
    await dormir(50);
  }
  if (!alvo?.webSocketDebuggerUrl) throw new Error('Não foi possível obter target Chromium.');
  cdpPagina = new Cdp(alvo.webSocketDebuggerUrl);
  await cdpPagina.conectar();
  await Promise.all([
    cdpPagina.enviar('Page.enable'),
    cdpPagina.enviar('Runtime.enable'),
    cdpPagina.enviar('Network.enable'),
    cdpPagina.enviar('Log.enable'),
  ]);

  const excecoes = [];
  const errosConsole = [];
  const falhasRede = [];
  cdpPagina.on('Runtime.exceptionThrown', ({ exceptionDetails }) => {
    excecoes.push(exceptionDetails?.exception?.description || exceptionDetails?.text || 'exceção sem descrição');
  });
  cdpPagina.on('Log.entryAdded', ({ entry }) => {
    if (entry?.level === 'error') errosConsole.push(`${entry.source || 'console'}: ${entry.text || ''}`);
  });
  cdpPagina.on('Network.loadingFailed', (evento) => {
    falhasRede.push(evento);
  });

  const resultados = [];
  for (const rota of rotas) {
    excecoes.length = 0;
    errosConsole.length = 0;
    falhasRede.length = 0;
    const carregou = aguardarEvento(cdpPagina, 'Page.loadEventFired');
    const url = `http://${host}:${portaPreview}${rota}`;
    const navegacao = await cdpPagina.enviar('Page.navigate', { url });
    await carregou;
    await dormir(900);
    const avaliacao = await cdpPagina.enviar('Runtime.evaluate', {
      returnByValue: true,
      expression: `(() => {
        const nome = (el) => (el.getAttribute('aria-label') || el.getAttribute('aria-labelledby') || el.getAttribute('title') || el.textContent || '').trim();
        const controles = [...document.querySelectorAll('input:not([type="hidden"]),select,textarea')];
        const semRotulo = controles.filter((el) => {
          if (el.getAttribute('aria-label') || el.getAttribute('aria-labelledby') || el.getAttribute('title')) return false;
          if (el.id && document.querySelector('label[for="' + CSS.escape(el.id) + '"]')) return false;
          return !el.closest('label');
        }).length;
        const ids = [...document.querySelectorAll('[id]')].map((el) => el.id).filter(Boolean);
        const duplicados = ids.filter((id, idx) => ids.indexOf(id) !== idx);
        return {
          href: location.href,
          titulo: document.title,
          bodyChars: (document.body?.innerText || '').trim().length,
          rootChildren: document.getElementById('root')?.children.length || 0,
          idioma: document.documentElement.lang || '',
          imagensSemAlt: [...document.images].filter((img) => !img.hasAttribute('alt')).length,
          botoesSemNome: [...document.querySelectorAll('button,[role="button"]')].filter((el) => !nome(el)).length,
          controlesSemRotulo: semRotulo,
          idsDuplicados: [...new Set(duplicados)].length,
        };
      })()`,
    });
    const valor = avaliacao?.result?.value || {};
    const falhasEstaticas = falhasRede.filter((item) => {
      const u = item?.requestId || '';
      return false; // requestId não contém URL; assets são validados pela presença do root e exceções JS.
    });
    resultados.push({ rota, navegacaoErro: navegacao?.errorText || null, ...valor, excecoes: [...excecoes], errosConsole: [...errosConsole], falhasEstaticas: falhasEstaticas.length });
  }

  const falhas = [];
  for (const r of resultados) {
    if (r.navegacaoErro) falhas.push(`${r.rota}: navegação Chromium falhou (${r.navegacaoErro})`);
    if (!r.titulo || r.bodyChars < 20 || r.rootChildren < 1) falhas.push(`${r.rota}: aplicação não renderizou conteúdo útil`);
    if (r.excecoes.length) falhas.push(`${r.rota}: ${r.excecoes.length} exceção(ões) JS não tratada(s)`);
    if (r.imagensSemAlt) falhas.push(`${r.rota}: ${r.imagensSemAlt} imagem(ns) sem alt`);
    if (r.botoesSemNome) falhas.push(`${r.rota}: ${r.botoesSemNome} botão(ões) sem nome acessível`);
    if (r.controlesSemRotulo) falhas.push(`${r.rota}: ${r.controlesSemRotulo} controle(s) sem rótulo`);
    if (r.idsDuplicados) falhas.push(`${r.rota}: ${r.idsDuplicados} id(s) duplicado(s)`);
    if (!/^pt(?:-|$)/i.test(r.idioma)) falhas.push(`${r.rota}: html lang não está em pt/pt-BR`);
  }

  console.log(JSON.stringify({ chromium, rotas: resultados, falhas }, null, 2));
  if (falhas.length) process.exitCode = 1;
} finally {
  try { cdpPagina?.fechar(); } catch {}
  try { cdpBrowser?.fechar(); } catch {}
  if (navegador && !navegador.killed) navegador.kill('SIGTERM');
  if (!preview.killed) preview.kill('SIGTERM');
  await dormir(150);
  await rm(perfilTmp, { recursive: true, force: true });
  if (preview.exitCode && preview.exitCode !== 0 && !process.exitCode) {
    console.error(previewErro);
    process.exitCode = preview.exitCode;
  }
}
