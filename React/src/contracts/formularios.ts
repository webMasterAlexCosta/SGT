/** Campos editáveis preservam a estrutura inicial; valores de controles não ficam
 * limitados aos literais usados para inicializar um formulário vazio. */
export type MutableForm<T> = {
  -readonly [K in keyof T]: T[K] extends string ? string
    : T[K] extends number ? number
      : T[K] extends boolean ? boolean
        : T[K] extends readonly (infer U)[] ? MutableForm<U>[]
          : T[K] extends object ? MutableForm<T[K]> : T[K];
};

/** Controles HTML aceitam identificadores numéricos como texto até serializar. */
type FormFieldValue<V> = V extends string
  ? V extends import('./shared').CivilDate | import('./shared').Instant | import('./shared').LocalDateTime | import('./shared').LocalTime ? string : V | ''
  : V extends number ? string | number
    : V extends readonly (infer U)[] ? FormDraft<U>[]
      : V extends object ? FormDraft<V> : V;
export type FormDraft<T> = { [K in keyof T]?: FormFieldValue<T[K]> };
