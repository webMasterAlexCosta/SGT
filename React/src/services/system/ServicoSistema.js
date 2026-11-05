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

  buscarLocalizacaoPorCoordenadas: async ({ latitude, longitude }, { signal } = {}) => {
    const parametros = new URLSearchParams({
      lat: String(latitude),
      lon: String(longitude),
      format: "jsonv2",
      addressdetails: "1",
      "accept-language": "pt-BR",
    });
    const resposta = await fetch(`https://nominatim.openstreetmap.org/reverse?${parametros}`, { signal });
    if (!resposta.ok) throw new Error("Falha ao consultar localização.");

    const dados = await resposta.json();
    return {
      cidade: dados.address?.city
        || dados.address?.town
        || dados.address?.municipality
        || dados.address?.village
        || null,
      estado: dados.address?.state || dados.address?.state_code || null,
    };
  },
};
