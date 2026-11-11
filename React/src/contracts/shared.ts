/** LocalDate is a civil date: never parse it through an implicit UTC Date. */
export type CivilDate = `${number}-${number}-${number}`;
/** Instant/OffsetDateTime carry an explicit offset. */
export type Instant = `${string}T${string}Z` | `${string}T${string}+${string}` | `${string}T${string}-${string}`;
export type LocalDateTime = `${CivilDate}T${string}`;
export type LocalTime = `${number}:${number}` | `${number}:${number}:${number}`;
export type JsonValue = null | boolean | number | string | JsonValue[] | { [key: string]: JsonValue };
export interface Page<T> {
  content: T[]; totalElements: number; totalPages: number; number: number; size: number;
  first: boolean; last: boolean; sort: JsonValue;
}
export interface PageMeta {
  totalElements: number; totalPages: number; page: number; size: number;
  first: boolean; last: boolean; sort: string | null;
}
export interface ApiResponse<T> {
  timestamp: Instant; status: number; error: string | null; message: string | null;
  requestId: string | null; meta: PageMeta | null; data: T | null; path: string | null;
}
export interface ValidationField { field: string | null; message: string; }
export interface ApiErrorBody {
  status?: number; code?: string; message?: string; mensagem?: string; detail?: string; title?: string;
  requestId?: string; fields?: (ValidationField | string)[]; errors?: (ValidationField | string)[];
}
