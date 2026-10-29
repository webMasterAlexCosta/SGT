import { defineConfig } from "vitest/config";
import react from "@vitejs/plugin-react";

export default defineConfig({
  plugins: [react()],
  test: {
    environment: "jsdom",
    setupFiles: ["./test/vitest/setup.js"],
    include: ["test/vitest/**/*.test.{js,jsx}"],
    restoreMocks: true,
    clearMocks: true,
    mockReset: true,
    testTimeout: 10000,
    reporters: [
      "default",
      ["junit", { outputFile: "./reports/vitest-junit.xml", suiteName: "SGT React - Integridade e Confiabilidade" }],
    ],
    coverage: {
      provider: "v8",
      reporter: ["text", "json-summary", "html"],
      reportsDirectory: "./reports/coverage-react",
      include: [
        "src/services/rh/ServicoAbsenteismo.js",
        "src/core/security/roles.js",
        "src/modules/rh/pages/PaginaRelatorioAbsenteismo/index.jsx",
      ],
      thresholds: {
        lines: 55,
        functions: 55,
        statements: 55,
        branches: 45,
      },
    },
  },
});
