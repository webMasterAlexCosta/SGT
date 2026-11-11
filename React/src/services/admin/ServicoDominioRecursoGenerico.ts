export const prepararRecursoParaEnvio = <T extends { id?: number | null; version?: number | null }>(item: T) => {
  const recurso = { ...item };
  if (recurso.id == null) delete recurso.id;
  if (recurso.version == null) delete recurso.version;
  return recurso;
};
