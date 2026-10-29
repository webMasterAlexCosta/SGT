import { defineConfig } from "vitest/config";
import react from "@vitejs/plugin-react";

export default defineConfig({
  plugins: [react()],
  test: {
    environment: "jsdom",
    setupFiles: ["./test/vitest-agressivo/setupAgressivo.js"],
    include: ["test/vitest-agressivo/camada2/**/*.test.{js,jsx}"],
    restoreMocks: true,
    clearMocks: true,
    mockReset: true,
    testTimeout: 20000,
    hookTimeout: 20000,
    reporters: [
      "default",
      ["junit", { outputFile: "./reports/camada2-comportamento-junit.xml", suiteName: "SGT React - Camada 2 - Comportamento" }],
    ],
    coverage: {
      provider: "v8",
      reporter: ["text", "json-summary", "html"],
      reportsDirectory: "./reports/coverage-camada2",
      include: ["src/**/*.{js,jsx}"],
      exclude: ["src/main.jsx"],
    },
  },
});
