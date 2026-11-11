export const formatarValorAuditoria = (valor: unknown): string | undefined => {
  if (valor === undefined || valor === null) return '—';
  if (typeof valor === 'boolean') return valor ? 'Sim' : 'Não';
  if (typeof valor === 'number') return String(valor);
  if (typeof valor === 'string') {
    const v = valor.trim();
    if (!v) return '—';
    const civil = /^\d{4}-\d{2}-\d{2}$/.test(v);
    const d = new Date(civil ? `${v}T12:00:00Z` : v);
    if (!Number.isNaN(d.getTime()) && /^\d{4}-\d{2}-\d{2}/.test(v)) return d.toLocaleDateString('pt-BR', civil ? { timeZone: 'UTC' } : undefined);
    return v;
  }
  if (Array.isArray(valor)) return !valor.length ? '—' : valor.map(formatarValorAuditoria).join(', ');
  if (typeof valor === 'object') {
    const pareceNome: unknown = Reflect.get(valor, 'nome') ?? Reflect.get(valor, 'descricao') ?? Reflect.get(valor, 'name') ?? Reflect.get(valor, 'title');
    if (pareceNome) return String(pareceNome);
    if (Reflect.get(valor, 'id') != null) return `#${Reflect.get(valor, 'id')}`;
    try { return JSON.stringify(valor); } catch { return String(valor); }
  }
  return String(valor);
};

