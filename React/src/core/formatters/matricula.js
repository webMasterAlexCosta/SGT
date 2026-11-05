export const extrairDigitosMatricula = (valor = "") =>
  String(valor ?? "").replace(/\D/g, "").slice(0, 8);

export const formatarMatricula = (valor = "") => {
  const digitos = extrairDigitosMatricula(valor);
  if (!digitos) return "";
  if (digitos.length <= 1) return digitos;
  if (digitos.length <= 4) return `${digitos.slice(0, 1)}.${digitos.slice(1)}`;
  if (digitos.length <= 7) {
    return `${digitos.slice(0, 1)}.${digitos.slice(1, 4)}.${digitos.slice(4)}`;
  }
  return `${digitos.slice(0, 1)}.${digitos.slice(1, 4)}.${digitos.slice(4, 7)}-${digitos.slice(7)}`;
};

export const matriculaEstaCompleta = (valor = "") =>
  extrairDigitosMatricula(valor).length === 8;
