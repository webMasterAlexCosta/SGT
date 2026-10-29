import "../vitest/setup.js";
import { vi } from "vitest";

if (!globalThis.requestAnimationFrame) {
  globalThis.requestAnimationFrame = (callback) => setTimeout(() => callback(performance.now()), 0);
}
if (!globalThis.cancelAnimationFrame) {
  globalThis.cancelAnimationFrame = (id) => clearTimeout(id);
}

if (!HTMLElement.prototype.scrollIntoView) {
  HTMLElement.prototype.scrollIntoView = vi.fn();
}
if (!HTMLElement.prototype.scrollTo) {
  HTMLElement.prototype.scrollTo = vi.fn();
}
if (!window.scrollTo) {
  window.scrollTo = vi.fn();
}

if (!globalThis.IntersectionObserver) {
  globalThis.IntersectionObserver = class IntersectionObserver {
    observe() {}
    unobserve() {}
    disconnect() {}
  };
}

if (!URL.createObjectURL) URL.createObjectURL = vi.fn(() => "blob:teste");
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

if (!navigator.geolocation) {
  Object.defineProperty(navigator, "geolocation", {
    configurable: true,
    value: {
      getCurrentPosition: vi.fn((sucesso) =>
        sucesso?.({ coords: { latitude: -22.9068, longitude: -43.1729 } }),
      ),
      watchPosition: vi.fn(() => 1),
      clearWatch: vi.fn(),
    },
  });
}

if (!window.open) window.open = vi.fn();
if (!window.confirm) window.confirm = vi.fn(() => true);
if (!window.alert) window.alert = vi.fn();

Object.defineProperty(window, "innerWidth", {
  configurable: true,
  writable: true,
  value: 1366,
});
