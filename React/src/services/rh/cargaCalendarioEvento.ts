/**
 * Constrói o contrato público de escrita do calendário.
 *
 * A função cria um novo objeto em vez de reutilizar a entidade recebida pela
 * listagem. Dessa forma, id, version e quaisquer outros campos internos nunca
 * são enviados novamente ao back-end.
 */
export const prepararCargaCalendarioEvento = (dados: { data?: unknown; tipo?: unknown; descricao?: unknown; abrangencia?: unknown; uf?: unknown; municipio?: unknown; superintendenciaId?: unknown; gestaoLocalId?: unknown; lotacaoId?: unknown; ativo?: unknown } = {}) => ({
  data: dados?.data || null,
  tipo: String(dados?.tipo || '').trim().toUpperCase(),
  descricao: String(dados?.descricao || '').trim() || null,
  abrangencia: String(dados?.abrangencia || '').trim().toUpperCase() || null,
  uf: String(dados?.uf || '').trim().toUpperCase() || null,
  municipio: String(dados?.municipio || '').trim() || null,
  superintendenciaId: dados?.superintendenciaId ?? null,
  gestaoLocalId: dados?.gestaoLocalId ?? null,
  lotacaoId: dados?.lotacaoId ?? null,
  ativo: dados?.ativo !== false,
});
