import { defineConfig } from "vitest/config";
import react from "@vitejs/plugin-react";

export default defineConfig({
  plugins: [react()],
  test: {
    environment: "jsdom",
    setupFiles: ["./test/vitest-agressivo/setupAgressivo.js"],
    include: ["test/vitest-agressivo/camada3/**/*.test.{js,jsx}"],
    restoreMocks: true,
    clearMocks: true,
    mockReset: true,
    testTimeout: 30000,
    hookTimeout: 30000,
    reporters: [
      "default",
      ["junit", { outputFile: "./reports/camada3-robustez-junit.xml", suiteName: "SGT React - Camada 3 - Robustez e Performance" }],
    ],
    coverage: { enabled: false },
  },
});
