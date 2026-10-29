import { defineConfig } from "vitest/config";
import react from "@vitejs/plugin-react";

export default defineConfig({
  plugins: [react()],
  test: {
    environment: "jsdom",
    setupFiles: ["./test/vitest-componentes/setupComponentes.js"],
    include: ["test/vitest-componentes/**/*.test.{js,jsx}"],
    restoreMocks: true,
    clearMocks: true,
    mockReset: true,
    testTimeout: 15000,
    hookTimeout: 15000,
    reporters: [
      "default",
      [
        "junit",
        {
          outputFile: "./reports/componentes-junit.xml",
          suiteName: "SGT React - Todos os Componentes",
        },
      ],
    ],
    coverage: {
      provider: "v8",
      reporter: ["text", "json-summary", "html"],
      reportsDirectory: "./reports/coverage-componentes",
      include: ["src/**/*.jsx"],
      exclude: ["src/main.jsx"],
    },
  },
});
