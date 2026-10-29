export const formatarDataHora = (iso) => {
  if (!iso) return "-";
  try {
    return new Date(iso).toLocaleString("pt-BR", {
      day: "2-digit", month: "2-digit", year: "numeric",
      hour: "2-digit", minute: "2-digit", second: "2-digit",
    });
  } catch {
    return iso;
  }
};

export const copiarParaAreaTransferencia = async (texto) => {
  if (texto) await navigator.clipboard.writeText(String(texto));
};

export const analisarJsonSeguro = (valor) => {
  if (valor == null || typeof valor === "object") return valor;
  if (typeof valor !== "string") return valor;
  const texto = valor.trim();
  if (!texto) return null;
  if (!((texto.startsWith("{") && texto.endsWith("}")) || (texto.startsWith("[") && texto.endsWith("]")))) return valor;
  try { return JSON.parse(texto); } catch { return valor; }
};

export const textoJsonFormatado = (valor) => {
  const parseado = analisarJsonSeguro(valor);
  if (parseado == null) return "";
  if (typeof parseado === "string") return parseado;
  try { return JSON.stringify(parseado, null, 2); } catch { return String(parseado); }
};


const CHAVES_PROTEGIDAS_AUDITORIA = /(?:senha|password|passwd|secret|token|totp|authorization|cookie|cpf|rg|ctps|pis|pasep|nit|telefone|celular|email|endereco|logradouro|cep|numeroConta|agencia|pix)/i;

const protegerValor = (valor, chave = "") => {
  if (CHAVES_PROTEGIDAS_AUDITORIA.test(String(chave || ""))) return "[PROTEGIDO]";
  if (Array.isArray(valor)) return valor.map((item) => protegerValor(item));
  if (valor && typeof valor === "object") {
    return Object.fromEntries(Object.entries(valor).map(([nome, item]) => [nome, protegerValor(item, nome)]));
  }
  return valor;
};

export const protegerDadosAuditoria = (valor) => {
  const parseado = analisarJsonSeguro(valor);
  return protegerValor(parseado);
};

export const textoJsonProtegidoAuditoria = (valor) => {
  const protegido = protegerDadosAuditoria(valor);
  if (protegido == null) return "";
  if (typeof protegido === "string") return protegido;
  try { return JSON.stringify(protegido, null, 2); } catch { return String(protegido); }
};
