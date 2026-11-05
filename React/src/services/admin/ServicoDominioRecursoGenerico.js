export const prepararRecursoParaEnvio = (item = {}) => {
  const recurso = { ...item };
  if (recurso.id == null) delete recurso.id;
  if (recurso.version == null) delete recurso.version;
  return recurso;
};
