import { describe, expect, it } from 'vitest';
import { descricaoDisponibilidade } from '../src/core/catalogos/catalogoTemporal.js';
import { serializarLocalDate, serializarYearMonth } from '../src/core/http/datasHttp.js';

describe('datas válidas no contrato Java', () => {
  it('rejeita um dia que não existe em fevereiro antes de enviar ao servidor', () => {
    expect(() => serializarLocalDate('2026-02-31')).toThrow();
  });
  it('preserva data civil bissexta sem converter fuso', () => {
    expect(serializarLocalDate('2024-02-29')).toBe('2024-02-29');
  });
  it('rejeita mês inexistente no filtro de frequência', () => {
    expect(() => serializarYearMonth('2026-13')).toThrow();
  });
});

describe('disponibilidade não comprovada', () => {
  it('não apresenta estado desconhecido como ativo', () => {
    expect(descricaoDisponibilidade({ ativo: null, disponibilidadeComprovada: false })).toMatch(/desconhecida/i);
  });
  it('mantém o estado explicitamente inativo', () => {
    expect(descricaoDisponibilidade({ ativo: false })).toMatch(/inativo/i);
  });
});
