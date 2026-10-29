import { access, cp, mkdir, readFile, readdir, rename, rm } from "node:fs/promises";
import path from "node:path";
import process from "node:process";
import { fileURLToPath } from "node:url";

const frontendRoot = path.resolve(path.dirname(fileURLToPath(import.meta.url)), "..");
const distDir = path.resolve(frontendRoot, process.env.FRONTEND_DIST_DIR || "dist");

const existe = async (caminho) => {
  try { await access(caminho); return true; } catch { return false; }
};

const backendValido = async (diretorio) => {
  if (!diretorio) return false;
  const pom = path.join(diretorio, "pom.xml");
  const resources = path.join(diretorio, "src", "main", "resources");
  if (!(await existe(pom)) || !(await existe(resources))) return false;
  try {
    const conteudoPom = await readFile(pom, "utf8");
    return /sig-gtrat|siggtrat/i.test(conteudoPom);
  } catch {
    return false;
  }
};

const localizarBackend = async () => {
  if (process.env.JAVA_BACKEND_DIR) {
    const explicito = path.resolve(frontendRoot, process.env.JAVA_BACKEND_DIR);
    if (!(await backendValido(explicito))) {
      throw new Error(`JAVA_BACKEND_DIR não aponta para o back-end SIG-GTRAT: ${explicito}`);
    }
    return explicito;
  }

  const pastaPai = path.dirname(frontendRoot);
  const entradas = await readdir(pastaPai, { withFileTypes: true });
  for (const entrada of entradas) {
    if (!entrada.isDirectory()) continue;
    const candidato = path.join(pastaPai, entrada.name);
    if (candidato === frontendRoot) continue;
    if (await backendValido(candidato)) return candidato;
  }
  return null;
};

if (!(await existe(path.join(distDir, "index.html")))) {
  throw new Error(`Build React não encontrado em ${distDir}. Execute "npm run build" antes da sincronização.`);
}

let staticDir;
if (process.env.JAVA_STATIC_DIR) {
  staticDir = path.resolve(frontendRoot, process.env.JAVA_STATIC_DIR);
} else {
  const backendDir = await localizarBackend();
  if (!backendDir) {
    throw new Error("Back-end SIG-GTRAT não encontrada. Defina JAVA_BACKEND_DIR ou JAVA_STATIC_DIR.");
  }
  staticDir = path.join(backendDir, "src", "main", "resources", "static");
}

const parent = path.dirname(staticDir);
const staging = path.join(parent, `${path.basename(staticDir)}.__novo__`);
const backup = path.join(parent, `${path.basename(staticDir)}.__backup__`);

await mkdir(parent, { recursive: true });
await rm(staging, { recursive: true, force: true });
await rm(backup, { recursive: true, force: true });
await cp(distDir, staging, { recursive: true });

let possuiBackup = false;
try {
  if (await existe(staticDir)) {
    await rename(staticDir, backup);
    possuiBackup = true;
  }
  await rename(staging, staticDir);
  if (possuiBackup) await rm(backup, { recursive: true, force: true });
} catch (erro) {
  await rm(staging, { recursive: true, force: true }).catch(() => {});
  if (possuiBackup && !(await existe(staticDir)) && await existe(backup)) {
    await rename(backup, staticDir).catch(() => {});
  }
  throw erro;
}

console.log(`Build React sincronizado com segurança em: ${staticDir}`);
