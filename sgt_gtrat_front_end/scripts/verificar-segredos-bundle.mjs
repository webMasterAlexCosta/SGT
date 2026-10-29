import { mkdtemp, readFile, readdir, rm } from "node:fs/promises";
import { spawnSync } from "node:child_process";
import os from "node:os";
import path from "node:path";

const raiz = process.cwd();
const pastaTemporaria = await mkdtemp(path.join(os.tmpdir(), "sgt-bundle-segredos-"));
const canarioGlobal = "SGT_CANARY_GLOBAL_9f7c1e2b_20260911";
const canarioGestor = "SGT_CANARY_GESTOR_4a8d6c3f_20260911";

const listarArquivos = async (diretorio) => {
  const saida = [];
  for (const entrada of await readdir(diretorio, { withFileTypes: true })) {
    const completo = path.join(diretorio, entrada.name);
    if (entrada.isDirectory()) saida.push(...await listarArquivos(completo));
    else saida.push(completo);
  }
  return saida;
};

try {
  const vite = path.join(raiz, "node_modules", ".bin", "vite");
  const execucao = spawnSync(
    vite,
    ["build", "--mode", "production", "--outDir", pastaTemporaria, "--emptyOutDir"],
    {
      cwd: raiz,
      env: {
        ...process.env,
        VITE_SHOW_TEST_CREDENTIALS: "true",
        VITE_TEST_GLOBAL_PASSWORD: canarioGlobal,
        VITE_TEST_GESTOR_PASSWORD: canarioGestor,
      },
      encoding: "utf8",
    },
  );

  if (execucao.status !== 0) {
    console.error("Não foi possível executar a build-canário de produção.");
    if (execucao.stderr) console.error(execucao.stderr);
    process.exitCode = 1;
  } else {
    const violacoes = [];
    for (const arquivo of await listarArquivos(pastaTemporaria)) {
      const texto = await readFile(arquivo, "utf8").catch(() => "");
      if (texto.includes(canarioGlobal)) violacoes.push("VITE_TEST_GLOBAL_PASSWORD");
      if (texto.includes(canarioGestor)) violacoes.push("VITE_TEST_GESTOR_PASSWORD");
    }

    if (violacoes.length) {
      console.error(
        "Bundle de produção incorporou credencial-canário de teste: " +
        [...new Set(violacoes)].join(", "),
      );
      process.exitCode = 1;
    } else {
      console.log(
        "Fail-closed validado: build de produção ignora VITE_SHOW_TEST_CREDENTIALS=true e não incorpora credenciais-canário.",
      );
    }
  }
} finally {
  await rm(pastaTemporaria, { recursive: true, force: true });
}
