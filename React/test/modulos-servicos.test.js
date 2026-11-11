import { beforeEach, describe, expect, it, vi } from 'vitest';
const { requisitar, requisitarDownload } = vi.hoisted(() => ({ requisitar: vi.fn(), requisitarDownload: vi.fn() }));
vi.mock('../src/core/http.ts', async (original) => ({ ...(await original()), requisitar, requisitarDownload }));
import { ServicoRecursoGenerico } from '../src/services/admin/ServicoRecursoGenerico.ts';
import { ServicoOcorrencia } from '../src/services/rh/ServicoOcorrencia.ts';

beforeEach(() => { requisitar.mockReset(); requisitarDownload.mockReset(); });

describe('Carga completa do catálogo de Cargos para opções', () => {
  it('inclui cargos das páginas posteriores na ordem recebida, sem reduzir o lote para a paginação visual', async () => {
    requisitar.mockImplementation(async ({ params }) => {
      const page = params?.page || 0;
      return { content: [{ id: page + 1, nome: `Cargo ${page + 1}` }], number: page, totalPages: 3, totalElements: 3, size: 1 };
    });
    const lista = await ServicoRecursoGenerico('/cargos').listarTodos();
    expect(lista.map((cargo) => cargo.id)).toEqual([1, 2, 3]);
    expect(requisitar.mock.calls.every(([request]) => request.params?.size > 5)).toBe(true);
  });
  it('não entrega uma lista truncada quando uma página posterior falha', async () => {
    requisitar.mockImplementation(async ({ params }) => {
      if (params?.page > 0) throw new Error('Falha na segunda página');
      return { content: [{ id: 1 }], totalPages: 2, totalElements: 2, number: 0 };
    });
    await expect(ServicoRecursoGenerico('/cargos').listarTodos()).rejects.toThrow('Falha na segunda página');
  });
  it('mantém a consulta simples dos catálogos que retornam lista', async () => {
    requisitar.mockResolvedValue([{ id: 1 }, { id: 2 }]);
    await expect(ServicoRecursoGenerico('/tipos-contratacao').listarTodos()).resolves.toEqual([{ id: 1 }, { id: 2 }]);
    expect(requisitar).toHaveBeenCalledTimes(1);
  });
});

describe('Anexo de ocorrência com finalidade auditável', () => {
  it('inclui a finalidade codificada no endereço de acesso quando informada', () => {
    const url = new URL(ServicoOcorrencia.obterUrlAnexo(15, 'Conferência & vínculo'), 'http://localhost');
    expect(url.pathname).toBe('/api/ocorrencias/anexos/15');
    expect(url.searchParams.get('finalidade')).toBe('Conferência & vínculo');
  });
  it('usa o canal de download autenticado com finalidade e preserva o erro de autorização', async () => {
    requisitarDownload.mockRejectedValue(Object.assign(new Error('Concessão específica ausente'), { status: 403 }));
    expect(typeof ServicoOcorrencia.baixarAnexo).toBe('function');
    await expect(ServicoOcorrencia.baixarAnexo(15, '  Auditoria do ato  ')).rejects.toMatchObject({ status: 403 });
    expect(requisitarDownload.mock.calls[0][0]).toEqual(expect.objectContaining({
      method: 'GET', url: '/ocorrencias/anexos/15', params: { finalidade: 'Auditoria do ato' },
    }));
  });
});
