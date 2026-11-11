import { validarRuntimeConfig } from "../../core/runtimeConfig";

const normalizarBase = (urlBaseBackend = "") => String(urlBaseBackend).replace(/\/$/, "");

const buscarConfigJson = async (urlBaseBackend = "", { cacheBust = false } = {}) => {
  const base = normalizarBase(urlBaseBackend);
  const sufixo = cacheBust ? `?t=${Date.now()}` : "";
  const resposta = await fetch(`${base}/config.json${sufixo}`, {
    method: "GET",
    cache: "no-store",
    // Configuração e latência são públicas; a sondagem não participa da sessão.
    credentials: "omit",
    headers: { Accept: "application/json" },
  });

  if (!resposta.ok) {
    throw new Error(`Falha ao carregar configuração (HTTP ${resposta.status}).`);
  }

  return resposta;
};

export const ServicoSistema = {
  carregarRuntimeConfig: async (urlBaseBackend = "") => {
    const resposta = await buscarConfigJson(urlBaseBackend);
    return validarRuntimeConfig(await resposta.json());
  },

  medirLatencia: async (urlBaseBackend = "") => {
    const inicio = performance.now();
    await buscarConfigJson(urlBaseBackend, { cacheBust: true });
    return Math.round(performance.now() - inicio);
  },

  buscarLocalizacaoPorCoordenadas: async ({ latitude, longitude }: { latitude: number; longitude: number }, { signal }: { signal?: AbortSignal } = {}) => {
    const parametros = new URLSearchParams({
      lat: String(latitude),
      lon: String(longitude),
      format: "jsonv2",
      addressdetails: "1",
      "accept-language": "pt-BR",
    });
    const resposta = await fetch(`https://nominatim.openstreetmap.org/reverse?${parametros}`, { signal });
    if (!resposta.ok) throw new Error("Falha ao consultar localização.");

    const json: unknown = await resposta.json();
    const dados = json !== null && typeof json === "object" ? json as Record<string, unknown> : {};
    const endereco = dados.address !== null && typeof dados.address === "object" ? dados.address as Record<string, unknown> : {};
    const texto = (...campos: string[]): string | null => {
      for (const campo of campos) if (typeof endereco[campo] === "string" && endereco[campo]) return endereco[campo];
      return null;
    };
    return { cidade: texto("city", "town", "municipality", "village"), estado: texto("state", "state_code") };
  },
};
