import { defineConfig, loadEnv } from "vite";
import react from "@vitejs/plugin-react";
import fs from "fs";
import path from "path";

const lerBooleano = (ambiente, chave, padrao = false) => {
  const bruto = ambiente[chave];
  if (bruto === undefined || bruto === "") return padrao;
  const valor = String(bruto).trim().toLowerCase();
  if (valor !== "true" && valor !== "false") {
    throw new TypeError(`${chave} deve ser true ou false.`);
  }
  return valor === "true";
};

const lerPorta = (ambiente, chave, padrao) => {
  const valor = Number(ambiente[chave] || padrao);
  if (!Number.isInteger(valor) || valor < 1 || valor > 65535) {
    throw new TypeError(`${chave} deve ser uma porta TCP válida (1-65535).`);
  }
  return valor;
};

const validarDestinoProxy = (valor) => {
  let url;
  try {
    url = new URL(valor);
  } catch {
    throw new TypeError("DEV_PROXY_TARGET deve ser uma URL HTTP(S) válida.");
  }
  if (!['http:', 'https:'].includes(url.protocol)) {
    throw new TypeError("DEV_PROXY_TARGET aceita somente HTTP(S).");
  }
  return valor.replace(/\/$/, "");
};

const criarProxy = (destino) => ({
  target: destino,
  changeOrigin: true,
  secure: true,
});

const criarConfiguracaoHttps = (ambiente) => {
  if (!lerBooleano(ambiente, "DEV_HTTPS_ENABLED", false)) return false;

  const caminhoInformado = ambiente.DEV_HTTPS_PFX_PATH;
  if (!caminhoInformado) {
    throw new Error(
      "DEV_HTTPS_PFX_PATH deve ser informado quando DEV_HTTPS_ENABLED=true.",
    );
  }

  const caminhoCertificado = path.resolve(process.cwd(), caminhoInformado);
  if (!fs.existsSync(caminhoCertificado)) {
    throw new Error(`Certificado HTTPS não encontrado em: ${caminhoCertificado}`);
  }

  return {
    pfx: fs.readFileSync(caminhoCertificado),
    passphrase: ambiente.DEV_HTTPS_PFX_PASSPHRASE || undefined,
  };
};

export default defineConfig(({ mode }) => {
  const ambiente = loadEnv(mode, process.cwd(), "");
  const destinoProxy = validarDestinoProxy(
    ambiente.DEV_PROXY_TARGET || "http://127.0.0.1:5000",
  );
  const hostDesenvolvimento = ambiente.DEV_SERVER_HOST || "127.0.0.1";
  const portaDesenvolvimento = lerPorta(ambiente, "DEV_SERVER_PORT", 5173);

  return {
    plugins: [react()],
    assetsInclude: ["**/*.png", "**/*.jpg", "**/*.jpeg", "**/*.svg"],

    server: {
      host: hostDesenvolvimento,
      port: portaDesenvolvimento,
      https: criarConfiguracaoHttps(ambiente),
      proxy: {
        "/api": criarProxy(destinoProxy),
        "/config.json": criarProxy(destinoProxy),
      },
    },

    build: {
      target: "es2022",
      outDir: "dist",
      emptyOutDir: true,
      chunkSizeWarningLimit: 1000,
      rollupOptions: {
        output: {
          manualChunks(id) {
            if (!id.includes("node_modules")) return undefined;
            if (id.includes("lucide-react")) return "vendor-icons";
            if (id.includes("recharts")) return "vendor-charts";
            if (id.includes("exceljs") || id.includes("file-saver")) {
              return "vendor-excel";
            }
            if (id.includes("jspdf") || id.includes("jspdf-autotable")) {
              return "vendor-pdf";
            }
            if (id.includes("@vladmandic/face-api")) return "vendor-face";
            if (
              id.includes("react") ||
              id.includes("react-dom") ||
              id.includes("react-router-dom")
            ) {
              return "vendor-react";
            }
            return "vendor-core";
          },
        },
      },
    },
  };
});
