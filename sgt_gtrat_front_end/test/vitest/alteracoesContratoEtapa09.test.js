import { readFileSync } from 'node:fs';
import { resolve } from 'node:path';
import { beforeEach, describe, expect, it, vi } from 'vitest';

const requisitar = vi.hoisted(() => vi.fn());
vi.mock('../../src/core/http.js', () => ({
  requisitar,
  extrairLista: (valor) => Array.isArray(valor) ? valor : valor?.content || [],
}));

import { ServicoAlteracoesContrato } from '../../src/services/admin/ServicoAlteracoesContrato.js';
import {
  criarEstadoPrevia,
  criarLinhaAditamento,
  criarLinhaRemanejamento,
  ehConflitoDePrevia,
  invalidarPrevia,
  payloadComTokenDaPrevia,
  prepararAditamento,
  prepararComposicao,
  prepararRemanejamento,
  previaContinuaValida,
  registrarPrevia,
  somaVariacoesRemanejamento,
  validarRemanejamentoLocal,
} from '../../src/services/admin/ServicoDominioAlteracoesContrato.js';

beforeEach(() => {
  requisitar.mockReset();
  requisitar.mockResolvedValue([]);
});

describe('Etapa 09 - composição, aditamentos e remanejamentos', () => {
  it('consome exatamente os 15 endpoints planejados', async () => {
    const id = 7;
    await ServicoAlteracoesContrato.listarComposicoes(id, '2026-09-12');
    await ServicoAlteracoesContrato.listarExcedentes(id, '2026-09-12');
    await ServicoAlteracoesContrato.criarComposicao(id, {});
    await ServicoAlteracoesContrato.atualizarComposicao(id, 2, {});
    await ServicoAlteracoesContrato.excluirComposicao(id, 2);
    await ServicoAlteracoesContrato.listarAditamentos(id);
    await ServicoAlteracoesContrato.previaAditamento(id, {});
    await ServicoAlteracoesContrato.registrarAditamento(id, {});
    await ServicoAlteracoesContrato.retificarAditamento(id, 3, {});
    await ServicoAlteracoesContrato.cancelarAditamento(id, 3, {});
    await ServicoAlteracoesContrato.listarRemanejamentos(id);
    await ServicoAlteracoesContrato.previaRemanejamento(id, {});
    await ServicoAlteracoesContrato.registrarRemanejamento(id, {});
    await ServicoAlteracoesContrato.retificarRemanejamento(id, 4, {});
    await ServicoAlteracoesContrato.cancelarRemanejamento(id, 4, {});

    expect(requisitar).toHaveBeenCalledTimes(15);
    expect(requisitar.mock.calls.map(([c]) => `${c.method} ${c.url}`)).toEqual([
      'GET /contratos/7/composicoes',
      'GET /contratos/7/composicoes/excedentes',
      'POST /contratos/7/composicoes',
      'PUT /contratos/7/composicoes/2',
      'DELETE /contratos/7/composicoes/2',
      'GET /contratos/7/aditamentos',
      'POST /contratos/7/aditamentos/previa',
      'POST /contratos/7/aditamentos',
      'POST /contratos/7/aditamentos/3/retificar',
      'POST /contratos/7/aditamentos/3/cancelar',
      'GET /contratos/7/remanejamentos',
      'POST /contratos/7/remanejamentos/previa',
      'POST /contratos/7/remanejamentos',
      'POST /contratos/7/remanejamentos/4/retificar',
      'POST /contratos/7/remanejamentos/4/cancelar',
    ]);
  });

  it('aceita efetivo zero em composição de rascunho sem inventar capacidade mínima no cliente', () => {
    expect(prepararComposicao({ lotacaoId: '1', jornadaId: '2', cargoId: '3', efetivoPrevisto: 0 })).toMatchObject({
      lotacaoId: 1,
      jornadaId: 2,
      cargoId: 3,
      efetivoPrevisto: 0,
    });
  });

  it('descarta chave visual das linhas e monta o DTO canônico de aditamento', () => {
    const linha = criarLinhaAditamento({ composicaoId: 9, quantidadeResultante: 4 });
    const payload = prepararAditamento({
      identificacao: ' AD-1 ', dataEfetiva: '2026-10-01', justificativa: ' ajuste ', alteracoesComposicao: [linha],
    }, 'TOKEN');
    expect(payload.identificacao).toBe('AD-1');
    expect(payload.alteracoesComposicao[0]).toEqual({ composicaoId: 9, lotacaoId: null, jornadaId: null, cargoId: null, quantidadeResultante: 4 });
    expect(payload.alteracoesComposicao[0]).not.toHaveProperty('chaveCliente');
    expect(payload.tokenPrevia).toBe('TOKEN');
  });

  it('invalida prévia quando qualquer campo é alterado e não reaproveita token antigo', () => {
    const dados = { identificacao: 'AD-1', dataEfetiva: '2026-10-01', justificativa: 'x', alteracoesComposicao: [] };
    const estado = registrarPrevia(dados, { tokenPrevia: 'abc' });
    expect(previaContinuaValida(estado, dados)).toBe(true);
    expect(payloadComTokenDaPrevia(prepararAditamento, { ...dados, justificativa: 'mudou' }, estado)).toBeNull();
    expect(invalidarPrevia()).toEqual(criarEstadoPrevia());
  });

  it('monta confirmação somente com token da prévia que corresponde aos dados atuais', () => {
    const dados = { identificacao: 'AD-2', dataEfetiva: '2026-10-02', justificativa: 'x', confirmarImpacto: true, alteracoesComposicao: [] };
    const estado = registrarPrevia(dados, { tokenPrevia: 'token-servidor' });
    expect(payloadComTokenDaPrevia(prepararAditamento, dados, estado)).toMatchObject({ tokenPrevia: 'token-servidor', confirmarImpacto: true });
  });

  it('valida remanejamento com pelo menos duas composições e soma zero', () => {
    const valido = { identificacao: 'REM-1', dataEfetiva: '2026-10-01', justificativa: 'ajuste', itens: [criarLinhaRemanejamento({ composicaoId: 1, variacao: -2 }), criarLinhaRemanejamento({ composicaoId: 2, variacao: 2 })] };
    expect(somaVariacoesRemanejamento(valido.itens)).toBe(0);
    expect(validarRemanejamentoLocal(valido)).toBeNull();
    expect(validarRemanejamentoLocal({ ...valido, itens: valido.itens.slice(0, 1) })).toMatch(/pelo menos duas/i);
    expect(validarRemanejamentoLocal({ ...valido, itens: [criarLinhaRemanejamento({ composicaoId: 1, variacao: -2 }), criarLinhaRemanejamento({ composicaoId: 2, variacao: 1 })] })).toMatch(/soma das variações/i);
  });

  it('DTO de remanejamento usa variação e descarta chave visual', () => {
    const payload = prepararRemanejamento({ identificacao: 'REM-2', dataEfetiva: '2026-11-01', justificativa: 'x', itens: [criarLinhaRemanejamento({ composicaoId: 1, variacao: -1 }), criarLinhaRemanejamento({ composicaoId: 2, variacao: 1 })] }, 'tok');
    expect(payload.itens).toEqual([
      { composicaoId: 1, lotacaoId: null, jornadaId: null, cargoId: null, variacao: -1 },
      { composicaoId: 2, lotacaoId: null, jornadaId: null, cargoId: null, variacao: 1 },
    ]);
    expect(payload.tokenPrevia).toBe('tok');
  });

  it('reconhece HTTP 409 como conflito de prévia/contexto e não outros status', () => {
    expect(ehConflitoDePrevia({ status: 409 })).toBe(true);
    expect(ehConflitoDePrevia({ response: { status: 409 } })).toBe(true);
    expect(ehConflitoDePrevia({ status: 400 })).toBe(false);
  });

  it('não contém mutação automática de vínculos ou empregados nesta etapa', async () => {
    const fonte = readFileSync(resolve(process.cwd(), 'src/services/admin/ServicoAlteracoesContrato.js'), 'utf8');
    expect(fonte).not.toMatch(/\/vinculos|\/empregados|moverEmpregado|realocar/i);
  });
});
