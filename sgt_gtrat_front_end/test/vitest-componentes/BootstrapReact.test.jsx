import { beforeEach, describe, expect, it, vi } from "vitest";

const mocks = vi.hoisted(() => ({
  criarRoot: vi.fn(),
  renderizar: vi.fn(),
}));

vi.mock("react-dom/client", () => ({
  default: {
    createRoot: mocks.criarRoot,
  },
  createRoot: mocks.criarRoot,
}));

vi.mock("../../src/Aplicacao.jsx", () => ({
  default: () => null,
}));

describe("Bootstrap React", () => {
  beforeEach(() => {
    vi.resetModules();
    vi.clearAllMocks();
    document.body.innerHTML = '<div id="root"></div>';
    mocks.criarRoot.mockReturnValue({ render: mocks.renderizar });
  });

  it("localiza o elemento root e inicializa a aplicação React", async () => {
    await import("../../src/main.jsx");

    expect(mocks.criarRoot).toHaveBeenCalledWith(document.getElementById("root"));
    expect(mocks.renderizar).toHaveBeenCalledTimes(1);
  });
});
