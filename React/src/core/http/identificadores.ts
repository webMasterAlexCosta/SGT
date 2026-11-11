/** Campo do controle antes de validar a presença exigida por uma rota. */
export type ResourceIdentifierInput = string | number | null | undefined;
export type ResourceIdentifier = string | number;

/** Validade negocial, faixa numérica e autorização continuam no Java. */
export const exigirIdentificadorRota = (value: ResourceIdentifierInput): ResourceIdentifier => {
  if (value === null || value === undefined || value === '') {
    throw new TypeError('Identificador do recurso não informado.');
  }
  return value;
};
