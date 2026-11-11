import { afterEach, beforeEach, describe, expect, it } from 'vitest';
import type { InternalAxiosRequestConfig } from 'axios';
import type { VinculoElegivelFrequenciaDTO } from '../src/contracts/java';
import type { ApiResponse, Page } from '../src/contracts/shared';
import { api } from '../src/core/http.ts';
import { invalidarGeracaoSessao } from '../src/core/security/geracaoSessao.ts';
import { ServicoVinculoContratual } from '../src/services/rh/ServicoVinculoContratual.ts';

const elegivel: VinculoElegivelFrequenciaDTO = {
  id: 501, pessoaId: 101, pessoaNome: 'Pessoa sintética LAB', pessoaCpf: null,
  contratoId: 9, contratoNumero: 'CONTRATO-LAB-9', contratoAno: 2026, matricula: null,
  dataReferencia: '2026-11-01', conhecidoAte: '2026-11-01T12:00:00Z', gestaoLocalId: 55,
  alocacaoId: 61, lotacaoId: 7, jornadaId: 8, cargoId: 6,
};
const pagina: Page<VinculoElegivelFrequenciaDTO> = {
  content: [elegivel], totalElements: 6, totalPages: 2, number: 0, size: 5,
  first: true, last: false, sort: null,
};
const envelope: ApiResponse<Page<VinculoElegivelFrequenciaDTO>> = {
  timestamp: '2026-11-01T12:00:00Z', status: 200, error: null, message: null,
  requestId: 'LAB-SELECAO-FREQUENCIA', meta: null, data: pagina,
  path: '/api/vinculos-contratuais/elegiveis-frequencia',
};
let adapterOriginal: typeof api.defaults.adapter;
let pedidos: InternalAxiosRequestConfig[];
beforeEach(() => {
  adapterOriginal = api.defaults.adapter; pedidos = [];
  api.defaults.adapter = async config => {
    pedidos.push(config);
    return { config, data: envelope, status: 200, statusText: 'OK', headers: {} };
  };
});
afterEach(() => { api.defaults.adapter = adapterOriginal; invalidarGeracaoSessao(); });

describe('seleção própria da frequência pelo HTTP central', () => {
  it('envia D/lotação/jornada e mantém paginação, nulidade e cookies no envelope real', async () => {
    const controller = new AbortController();
    const resposta = await ServicoVinculoContratual.listarElegiveisParaFrequencia({
      dataRef: '2026-11-01', lotacaoId: 7, jornadaId: 8, contratoId: 9, page: 0, size: 5,
    }, { signal: controller.signal, headers: { 'X-Prova-Lab': 'selecao-frequencia' }, skipLoading: true });
    expect(resposta).toEqual(pagina);
    expect(resposta.content[0].pessoaCpf).toBeNull();
    expect(pedidos).toHaveLength(1);
    expect(pedidos[0]).toMatchObject({ method: 'get', url: '/vinculos-contratuais/elegiveis-frequencia',
      params: { dataRef: '2026-11-01', lotacaoId: 7, jornadaId: 8, contratoId: 9, page: 0, size: 5 }, withCredentials: true });
    expect(pedidos[0].params).not.toHaveProperty('ativoEm');
    expect(pedidos[0].headers.get('X-Prova-Lab')).toBe('selecao-frequencia');
    expect(pedidos[0].signal).toBeDefined(); expect(pedidos[0].signal?.aborted).toBe(false);
  });

  it('encaminha filtro de contratos/busca sem converter cadastro histórico em fonte operacional', async () => {
    await ServicoVinculoContratual.listarElegiveisParaFrequencia({
      dataRef: '2026-11-01', lotacaoId: 7, jornadaId: 8, contratoIds: [9, 10], busca: 'Pessoa sintética', page: 1, size: 5,
    });
    expect(pedidos[0].params).toEqual({ dataRef: '2026-11-01', lotacaoId: 7, jornadaId: 8,
      contratoIds: [9, 10], busca: 'Pessoa sintética', page: 1, size: 5 });
    expect(pedidos.some(pedido => pedido.url === '/vinculos-contratuais')).toBe(false);
  });

  it('preserva recusa403 do Java, sem retry ou fallback cadastral', async () => {
    api.defaults.adapter = async config => {
      pedidos.push(config);
      throw Object.assign(new Error('Escopo sintético revogado'), { config,
        response: { status: 403, data: { status: 403, code: 'LAB_SCOPE_REVOKED', message: 'Escopo sintético revogado' }, headers: {} } });
    };
    await expect(ServicoVinculoContratual.listarElegiveisParaFrequencia({
      dataRef: '2026-11-01', lotacaoId: 7, jornadaId: 8,
    }, { silentError: true })).rejects.toMatchObject({ status: 403, message: 'Escopo sintético revogado' });
    expect(pedidos).toHaveLength(1);
    expect(pedidos[0].url).toBe('/vinculos-contratuais/elegiveis-frequencia');
  });

  it('não inicia transporte com consulta já cancelada', async () => {
    const controller = new AbortController(); controller.abort();
    await expect(ServicoVinculoContratual.listarElegiveisParaFrequencia({
      dataRef: '2026-11-01', lotacaoId: 7, jornadaId: 8,
    }, { signal: controller.signal })).rejects.toBeDefined();
    expect(pedidos).toEqual([]);
  });
});
