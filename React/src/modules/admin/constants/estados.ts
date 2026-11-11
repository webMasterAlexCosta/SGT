export const TODOS_OS_ESTADOS = "TODOS";
export const ESTADO_PADRAO_ADMIN = "RJ";

export const ESTADOS_BRASIL = Object.freeze([
  { sigla: "AC", nome: "Acre" },
  { sigla: "AL", nome: "Alagoas" },
  { sigla: "AP", nome: "Amapá" },
  { sigla: "AM", nome: "Amazonas" },
  { sigla: "BA", nome: "Bahia" },
  { sigla: "CE", nome: "Ceará" },
  { sigla: "DF", nome: "Distrito Federal" },
  { sigla: "ES", nome: "Espírito Santo" },
  { sigla: "GO", nome: "Goiás" },
  { sigla: "MA", nome: "Maranhão" },
  { sigla: "MT", nome: "Mato Grosso" },
  { sigla: "MS", nome: "Mato Grosso do Sul" },
  { sigla: "MG", nome: "Minas Gerais" },
  { sigla: "PA", nome: "Pará" },
  { sigla: "PB", nome: "Paraíba" },
  { sigla: "PR", nome: "Paraná" },
  { sigla: "PE", nome: "Pernambuco" },
  { sigla: "PI", nome: "Piauí" },
  { sigla: "RJ", nome: "Rio de Janeiro" },
  { sigla: "RN", nome: "Rio Grande do Norte" },
  { sigla: "RS", nome: "Rio Grande do Sul" },
  { sigla: "RO", nome: "Rondônia" },
  { sigla: "RR", nome: "Roraima" },
  { sigla: "SC", nome: "Santa Catarina" },
  { sigla: "SP", nome: "São Paulo" },
  { sigla: "SE", nome: "Sergipe" },
  { sigla: "TO", nome: "Tocantins" },
]);

export const normalizarUf = (valor: string | null | undefined) =>
  String(valor || "").trim().toUpperCase().slice(0, 2);

export const filtrarPorEstado = <T extends { uf?: string | null },>(itens: T[] | null | undefined, estado: string, habilitado = true) => {
  const lista = Array.isArray(itens) ? itens : [];
  if (!habilitado || estado === TODOS_OS_ESTADOS) return lista;
  const uf = normalizarUf(estado);
  return lista.filter((item) => normalizarUf(item?.uf) === uf);
};

export const contarPorEstado = (itens: { uf?: string | null }[] | null | undefined) => {
  const contagens: Record<string, number> = { [TODOS_OS_ESTADOS]: 0 };
  for (const item of Array.isArray(itens) ? itens : []) {
    const uf = normalizarUf(item?.uf);
    contagens[TODOS_OS_ESTADOS] += 1;
    if (uf) contagens[uf] = (contagens[uf] || 0) + 1;
  }
  return contagens;
};
