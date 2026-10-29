import { describe, expect, it } from "vitest";

const modulosReact = import.meta.glob("../../src/**/*.jsx");

const entradas = Object.entries(modulosReact)
  .filter(([caminho]) => !caminho.endsWith("/main.jsx"))
  .sort(([a], [b]) => a.localeCompare(b));

describe("Todos os módulos React - importação e compilação", () => {
  it("encontra módulos JSX para validar", () => {
    expect(entradas.length).toBeGreaterThan(0);
  });

  it.each(entradas)("%s importa sem erro", async (_caminho, carregarModulo) => {
    const modulo = await carregarModulo();
    expect(modulo).toBeTruthy();
    expect(Object.keys(modulo).length).toBeGreaterThan(0);
  });
});
