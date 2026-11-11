import { beforeEach, describe, expect, it, vi } from 'vitest';

const { requisitar } = vi.hoisted(() => ({ requisitar: vi.fn() }));
vi.mock('../src/core/http.ts', async (original) => ({ ...(await original()), requisitar }));
import { ServicoOcorrencia } from '../src/services/rh/ServicoOcorrencia.ts';

const contexto = { dataRef: '2026-11-01', lotacaoId: 7, turnoId: 8 };
beforeEach(() => { requisitar.mockReset(); });

describe('IND-06: busca autorizada de Empregado para Ocorrências', () => {
  it('usa o endpoint operacional com contexto e percorre todas as páginas sem trocar os IDs', async () => {
    requisitar.mockImplementation(async ({ url, params }) => {
      if (url === '/empregados') return [{ id: 999, nome: 'Resultado proibido' }];
      const page = Number(params.page || 0);
      return { content: [{ id: page === 0 ? 17 : 18, nome: 'Ana Silva', cargoNome: 'Cargo', lotacaoId: 7, turnoId: 8 }], number: page, totalPages: 2, totalElements: 2, size: 1 };
    });
    const resultado = await ServicoOcorrencia.buscarEmpregados(' Ana ', { skipLoading: true }, contexto);
    expect(resultado.map((p) => p.id)).toEqual([17, 18]);
    expect(requisitar.mock.calls.every(([r]) => r.url === '/empregados/filtrados-frequencia')).toBe(true);
    for (const [request] of requisitar.mock.calls) {
      expect(request.params).toMatchObject({ dataRef: '2026-11-01', lotacaoId: 7, turnoId: 8, searchTerm: 'Ana' });
      expect(request.params).not.toHaveProperty('search');
    }
  });

  it('preserva o 403 do Java sem fallback para busca geral ou lista vazia', async () => {
    requisitar.mockRejectedValue(Object.assign(new Error('A lotação/turno não pertence ao seu escopo operacional.'), { status: 403 }));
    await expect(ServicoOcorrencia.buscarEmpregados('Ana', {}, contexto)).rejects.toMatchObject({ status: 403 });
    expect(requisitar).toHaveBeenCalledTimes(1);
    expect(requisitar.mock.calls[0][0].url).toBe('/empregados/filtrados-frequencia');
  });

  it.each([{ dataRef: '2026-11-01', lotacaoId: 7 }, { dataRef: '2026-11-01', turnoId: 8 }, { lotacaoId: 7, turnoId: 8 }])('não faz busca operacional sem contexto completo %o', async (incompleto) => {
    requisitar.mockResolvedValue([]);
    await expect(ServicoOcorrencia.buscarEmpregados('Ana', {}, incompleto)).rejects.toThrow(/data|lotação|turno/i);
    expect(requisitar).not.toHaveBeenCalled();
  });

  it('preserva a consulta geral dos perfis aos quais o Java a autoriza', async () => {
    requisitar.mockResolvedValue([{ id: 17, nome: 'Ana Silva' }]);
    await expect(ServicoOcorrencia.buscarEmpregados('Ana')).resolves.toEqual([{ id: 17, nome: 'Ana Silva' }]);
    expect(requisitar.mock.calls[0][0]).toMatchObject({ url: '/empregados', params: { search: 'Ana' } });
  });
});
