const dois = (n: number) => String(n).padStart(2, '0');

export const serializarLocalDate = (valor: unknown) => {
  if (typeof valor === 'string' && /^\d{4}-\d{2}-\d{2}$/.test(valor)) {
    const data = new Date(`${valor}T00:00:00.000Z`);
    if (Number.isFinite(data.getTime()) && data.toISOString().slice(0, 10) === valor) return valor;
  }
  if (valor instanceof Date && Number.isFinite(valor.getTime())) {
    return `${valor.getFullYear()}-${dois(valor.getMonth() + 1)}-${dois(valor.getDate())}`;
  }
  throw new TypeError('LocalDate deve ser Date válida ou YYYY-MM-DD.');
};

export const serializarYearMonth = (valor: unknown) => {
  if (typeof valor === 'string' && /^\d{4}-\d{2}$/.test(valor) && Number(valor.slice(5)) >= 1 && Number(valor.slice(5)) <= 12) return valor;
  if (valor instanceof Date && Number.isFinite(valor.getTime())) {
    return `${valor.getFullYear()}-${dois(valor.getMonth() + 1)}`;
  }
  throw new TypeError('YearMonth deve ser Date válida ou YYYY-MM.');
};

export const serializarInstant = (valor: unknown) => {
  if (valor instanceof Date && Number.isFinite(valor.getTime())) return valor.toISOString();
  if (typeof valor === 'string') {
    const texto = valor.trim();
    if (!/(?:Z|[+-]\d{2}:\d{2})$/i.test(texto) || !Number.isFinite(Date.parse(texto))) {
      throw new TypeError('Instant deve usar ISO-8601 com Z ou offset.');
    }
    return texto;
  }
  throw new TypeError('Instant inválido.');
};
