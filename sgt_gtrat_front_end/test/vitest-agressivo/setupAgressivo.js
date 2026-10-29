import "../vitest/setup.js";
import { vi } from "vitest";

class ResizeObserverAgressivo {
  constructor(callback) {
    this.callback = callback;
    this.alvos = new Set();
  }
  observe(alvo) {
    this.alvos.add(alvo);
    queueMicrotask(() => {
      if (!this.alvos.has(alvo)) return;
      this.callback?.([
        {
          target: alvo,
          contentRect: { width: 1280, height: 720, top: 0, left: 0, right: 1280, bottom: 720 },
        },
      ]);
    });
  }
  unobserve(alvo) {
    this.alvos.delete(alvo);
  }
  disconnect() {
    this.alvos.clear();
  }
}

globalThis.ResizeObserver = ResizeObserverAgressivo;

globalThis.IntersectionObserver = class IntersectionObserverAgressivo {
  constructor(callback) {
    this.callback = callback;
    this.alvos = new Set();
  }
  observe(alvo) {
    this.alvos.add(alvo);
    queueMicrotask(() => {
      if (!this.alvos.has(alvo)) return;
      this.callback?.([{ target: alvo, isIntersecting: true, intersectionRatio: 1 }]);
    });
  }
  unobserve(alvo) { this.alvos.delete(alvo); }
  disconnect() { this.alvos.clear(); }
};

Object.defineProperty(window, "innerWidth", {
  configurable: true,
  writable: true,
  value: 1366,
});
Object.defineProperty(window, "innerHeight", {
  configurable: true,
  writable: true,
  value: 768,
});

window.matchMedia = vi.fn((consulta) => ({
  matches: /max-width:\s*(?:7|8|9)\d{2}px/i.test(String(consulta)) ? window.innerWidth <= 900 : false,
  media: String(consulta),
  onchange: null,
  addEventListener: vi.fn(),
  removeEventListener: vi.fn(),
  addListener: vi.fn(),
  removeListener: vi.fn(),
  dispatchEvent: vi.fn(() => true),
}));

if (!globalThis.requestAnimationFrame) {
  globalThis.requestAnimationFrame = (callback) => setTimeout(() => callback(performance.now()), 0);
}
if (!globalThis.cancelAnimationFrame) {
  globalThis.cancelAnimationFrame = (id) => clearTimeout(id);
}

if (!HTMLElement.prototype.scrollIntoView) HTMLElement.prototype.scrollIntoView = vi.fn();
if (!HTMLElement.prototype.scrollTo) HTMLElement.prototype.scrollTo = vi.fn();
window.scrollTo = vi.fn();
window.open = vi.fn(() => null);
window.print = vi.fn();
window.confirm = vi.fn(() => true);
window.alert = vi.fn();

if (!URL.createObjectURL) URL.createObjectURL = vi.fn(() => "blob:teste-agressivo");
if (!URL.revokeObjectURL) URL.revokeObjectURL = vi.fn();

if (!navigator.clipboard) {
  Object.defineProperty(navigator, "clipboard", {
    configurable: true,
    value: {
      writeText: vi.fn(async () => undefined),
      readText: vi.fn(async () => ""),
    },
  });
}

Object.defineProperty(navigator, "geolocation", {
  configurable: true,
  value: {
    getCurrentPosition: vi.fn((sucesso) =>
      sucesso?.({ coords: { latitude: -22.9068, longitude: -43.1729, accuracy: 10 } }),
    ),
    watchPosition: vi.fn(() => 1),
    clearWatch: vi.fn(),
  },
});


if (!globalThis.CSS) globalThis.CSS = {};
if (!globalThis.CSS.escape) {
  globalThis.CSS.escape = (valor) => String(valor).replace(/[^a-zA-Z0-9_-]/g, "\\$&");
}

if (typeof HTMLDialogElement !== "undefined") {
  if (!HTMLDialogElement.prototype.showModal) {
    HTMLDialogElement.prototype.showModal = function showModal() { this.open = true; };
  }
  if (!HTMLDialogElement.prototype.close) {
    HTMLDialogElement.prototype.close = function close() { this.open = false; };
  }
}

const retangulo = {
  x: 0,
  y: 0,
  top: 0,
  left: 0,
  right: 1280,
  bottom: 720,
  width: 1280,
  height: 720,
  toJSON: () => ({}),
};
HTMLElement.prototype.getBoundingClientRect = vi.fn(() => retangulo);

if (typeof HTMLFormElement !== "undefined") {
  HTMLFormElement.prototype.requestSubmit = function requestSubmit() {
    this.dispatchEvent(new Event("submit", { bubbles: true, cancelable: true }));
  };
  HTMLFormElement.prototype.submit = vi.fn();
}

Object.defineProperty(navigator, "mediaDevices", {
  configurable: true,
  value: {
    getUserMedia: vi.fn(async () => ({
      getTracks: () => [{ stop: vi.fn() }],
    })),
  },
});

if (typeof HTMLMediaElement !== "undefined") {
  HTMLMediaElement.prototype.play = vi.fn(async () => undefined);
  HTMLMediaElement.prototype.pause = vi.fn();
}
