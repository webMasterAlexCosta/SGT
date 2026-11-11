export const obterContratosDoEmpregado = (empregado: import("../../../../contracts/relatorios").ContractorEmployeeView) => {
  if (Array.isArray(empregado?.contratos)) return empregado.contratos;
  if (empregado?.contrato) return [empregado.contrato];
  return [];
};

export const formatarData = (valor: (`${number}-${number}-${number}` | null)) => {
  if (!valor) return "—";
  const data = new Date(`${String(valor).split("T")[0]}T12:00:00`);
  if (Number.isNaN(data.getTime())) return "—";
  return new Intl.DateTimeFormat("pt-BR").format(data);
};
