const SIGLAS_UF = new Set([
  "AC", "AL", "AP", "AM", "BA", "CE", "DF", "ES", "GO", "MA", "MT",
  "MS", "MG", "PA", "PB", "PR", "PE", "PI", "RJ", "RN", "RS", "RO",
  "RR", "SC", "SP", "SE", "TO",
]);

const normalizarUf = (valor) => {
  const texto = String(valor || "").trim().toUpperCase();
  if (!texto) return "";

  // Em valores institucionais como "SE/RJ", a UF é a parte após a barra.
  const partesDaBarra = texto.split("/").map((parte) => parte.trim()).filter(Boolean);
  for (let indice = partesDaBarra.length - 1; indice >= 0; indice -= 1) {
    if (SIGLAS_UF.has(partesDaBarra[indice])) return partesDaBarra[indice];
  }

  const tokens = texto.match(/\b[A-Z]{2}\b/g) || [];
  for (let indice = tokens.length - 1; indice >= 0; indice -= 1) {
    if (SIGLAS_UF.has(tokens[indice])) return tokens[indice];
  }
  return "";
};

export const formatarGestorEstadual = (nomeOuObjeto, ufAlternativa = "") => {
  const objeto = nomeOuObjeto && typeof nomeOuObjeto === "object" ? nomeOuObjeto : null;
  const nome = objeto?.label || objeto?.nome || nomeOuObjeto || "";
  const uf = normalizarUf(objeto?.uf || ufAlternativa || nome);
  return uf ? `Gestor estadual/${uf}` : "Gestor estadual";
};

export const formatarGestaoLocal = (nomeOuObjeto, codigoAlternativo = "") => {
  const objeto = nomeOuObjeto && typeof nomeOuObjeto === "object" ? nomeOuObjeto : null;
  const nomeOriginal = String(objeto?.label || objeto?.nome || nomeOuObjeto || "").trim();
  const codigo = String(objeto?.codigo || codigoAlternativo || "").trim();

  if (!nomeOriginal) return codigo || "Gestão Local";
  return codigo && !nomeOriginal.toUpperCase().includes(codigo.toUpperCase())
    ? `${nomeOriginal} (${codigo})`
    : nomeOriginal;
};
