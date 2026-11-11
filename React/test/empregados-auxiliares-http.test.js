import { beforeEach, describe, expect, it, vi } from 'vitest';

const { requisitar } = vi.hoisted(() => ({ requisitar: vi.fn() }));
vi.mock('../src/core/http.ts', async (original) => ({ ...(await original()), requisitar }));
import { ServicoEmpregado } from '../src/services/rh/ServicoEmpregado.ts';

beforeEach(() => { requisitar.mockReset(); });

describe('Auxiliares canônicos compartilhados do empregado', () => {
  it('consulta somente catálogos existentes no Java e preserva o sinal de cancelamento', async () => {
    const signal = new AbortController().signal;
    requisitar.mockImplementation(async ({ url }) => [{ id: 1, nome: url }]);
    const result = await ServicoEmpregado.buscarDadosAuxiliares({ signal, retries: 0 });
    expect(requisitar.mock.calls.map(([request]) => request.url).sort()).toEqual([
      '/cargos', '/contratos', '/lotacoes', '/status-empregados',
    ]);
    expect(requisitar.mock.calls.every(([request]) => request.signal === signal && request.retries === 0)).toBe(true);
    expect(Object.keys(result).sort()).toEqual(['cargos', 'contratos', 'lotacoes', 'statuses']);
    expect(result.contratos).toEqual([{ id: 1, nome: '/contratos' }]);
  });

  it('propaga recusa real do catálogo para a tela, sem convertê-la em lista vazia', async () => {
    requisitar.mockImplementation(async ({ url }) => {
      if (url === '/contratos') throw Object.assign(new Error('Consulta fora do escopo autorizado'), { status: 403 });
      return [];
    });
    await expect(ServicoEmpregado.buscarDadosAuxiliares()).rejects.toMatchObject({
      status: 403, message: 'Consulta fora do escopo autorizado',
    });
  });

  it('carrega todas as páginas de cargos para não omitir opções válidas', async () => {
    requisitar.mockImplementation(async ({ url, params }) => {
      if (url !== '/cargos') return [];
      const page = params?.page || 0;
      return { content: [{ id: page + 1, nome: `Cargo ${page + 1}` }], number: page, totalPages: 2, totalElements: 2, size: 1 };
    });
    const result = await ServicoEmpregado.buscarDadosAuxiliares();
    expect(result.cargos.map(({ id }) => id)).toEqual([1, 2]);
    expect(requisitar.mock.calls.filter(([request]) => request.url === '/cargos')).toHaveLength(2);
  });
});
