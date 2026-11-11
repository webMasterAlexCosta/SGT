import { beforeEach, describe, expect, it, vi } from 'vitest';
import * as dominio from '../src/services/admin/ServicoDominioExcecoesContrato.ts';

const { requisitar } = vi.hoisted(() => ({ requisitar: vi.fn() }));
vi.mock('../src/core/http.ts', () => ({
  requisitar,
  extrairLista: (dados) => Array.isArray(dados) ? dados : dados?.content || [],
  requisitarDownload: vi.fn(),
}));

import { ServicoContrato } from '../src/services/admin/ServicoContrato.ts';
import { ServicoExcecoesContrato } from '../src/services/admin/ServicoExcecoesContrato.ts';

const formularioTransferencia = () => ({
  dataEfetiva: '2026-11-05', dataAto: '2026-10-31', gestaoLocalDestinoId: '23',
  tipoContratacaoDestinoId: '71', motivo: ' Transferência documentada ',
  referenciaSei: ' TESTE-TRANSFERENCIA ', autorizacaoAdministrativa: ' AUTORIZACAO-SINTETICA ',
  eventoPrecedenteId: '', relacaoPrecedencia: '', ordemDocumental: '',
  tokenPrevia: 'token-da-previa', confirmarImpacto: true,
  mapeamentos: [{ composicaoOrigemId: '31', lotacaoOrigemId: '32', jornadaOrigemId: '33', cargoOrigemId: '34', lotacaoDestinoId: '42', jornadaDestinoId: '43', cargoDestinoId: '44' }],
});

beforeEach(() => { requisitar.mockReset(); requisitar.mockResolvedValue({ id: 9 }); });

describe('Contrato congelado de transferência e ciclo de vida', () => {
  it('preserva o tipo de contratação e a confirmação de prévia no DTO da transferência', () => {
    expect(dominio.prepararTransferenciaGestao(formularioTransferencia())).toEqual({
      dataEfetiva: '2026-11-05', dataAto: '2026-10-31', gestaoLocalDestinoId: 23,
      tipoContratacaoDestinoId: 71, motivo: 'Transferência documentada',
      referenciaSei: 'TESTE-TRANSFERENCIA', autorizacaoAdministrativa: 'AUTORIZACAO-SINTETICA',
      eventoPrecedenteId: null, relacaoPrecedencia: null, ordemDocumental: null,
      tokenPrevia: 'token-da-previa', confirmarImpacto: true,
      conferenciaDocumentalConfirmada: false, versaoConferenciaDocumental: null,
      mapeamentos: [{ composicaoOrigemId: 31, lotacaoOrigemId: 32, jornadaOrigemId: 33, cargoOrigemId: 34, lotacaoDestinoId: 42, jornadaDestinoId: 43, cargoDestinoId: 44 }],
    });
  });

  it('recusa intenção sem tipo de contratação do destino antes de pedir prévia', () => {
    expect(dominio.validarTransferenciaGestao({ ...formularioTransferencia(), tipoContratacaoDestinoId: '' })).toMatch(/tipo de contratação/i);
  });

  it('recusa mapas de origem duplicados para não ocultar uma composição ausente', () => {
    const dados = formularioTransferencia();
    dados.mapeamentos.push({ ...dados.mapeamentos[0], lotacaoDestinoId: '99' });
    expect(dominio.validarTransferenciaGestao(dados)).toMatch(/duplicad|repetid/i);
  });

  it('preserva token e confirmação ao corrigir anulação pela mesma API canônica', () => {
    expect(dominio.prepararCorrecaoAnulacao({
      dataEfetiva: '2026-10-01', dataAto: '2026-11-01', motivo: 'Correção documentada',
      referenciaSei: 'TESTE-ANULACAO', eventoPrecedenteId: '41', ordemDocumental: '2',
      tokenPrevia: 'token-correcao', confirmarImpacto: true,
    })).toEqual({
      dataEfetiva: '2026-10-01', dataAto: '2026-11-01', motivo: 'Correção documentada',
      referenciaSei: 'TESTE-ANULACAO', eventoPrecedenteId: 41, ordemDocumental: 2,
      relacaoPrecedencia: 'CANCELA', tokenPrevia: 'token-correcao', confirmarImpacto: true,
    });
  });

  it('envia a intenção completa à prévia especializada de transferência', async () => {
    expect(ServicoExcecoesContrato.previaTransferenciaGestao).toBeTypeOf('function');
    const dados = { ...formularioTransferencia(), tokenPrevia: null, confirmarImpacto: false };
    await ServicoExcecoesContrato.previaTransferenciaGestao(9, dados);
    expect(requisitar).toHaveBeenCalledWith(expect.objectContaining({ method: 'POST', url: '/contratos/9/transferencias-gestao/previa', data: dados, retries: 0 }));
  });

  it('usa o cancelamento existente sem excluir a transferência futura', async () => {
    expect(ServicoExcecoesContrato.cancelarTransferenciaGestao).toBeTypeOf('function');
    const dados = { dataEfetiva: '2026-11-01', dataAto: '2026-11-01', motivo: 'Revogação', referenciaSei: 'TESTE-CANCELAR', autorizacaoAdministrativa: 'AUTORIZACAO-SINTETICA' };
    await ServicoExcecoesContrato.cancelarTransferenciaGestao(9, 17, dados, { senhaConfirmacao: 'senha-sintetica' });
    expect(requisitar).toHaveBeenCalledWith(expect.objectContaining({ method: 'POST', url: '/contratos/9/transferencias-gestao/17/cancelar', data: dados, retries: 0, senhaConfirmacao: 'senha-sintetica' }));
  });

  it('prévia de publicação conserva o DTO próprio da publicação', async () => {
    expect(ServicoContrato.previaPublicacao).toBeTypeOf('function');
    const dados = { dataPublicacao: '2026-11-01', referenciaSei: 'TESTE-PUBLICACAO', justificativa: 'Publicação retroativa' };
    await ServicoContrato.previaPublicacao(9, dados);
    expect(requisitar).toHaveBeenCalledWith(expect.objectContaining({ method: 'POST', url: '/contratos/9/previas/publicar', data: dados, retries: 0 }));
  });

  it('prévia de ciclo de vida usa a operação literal canônica', async () => {
    expect(ServicoContrato.previaCicloVida).toBeTypeOf('function');
    const dados = { dataEfetiva: '2026-11-01', motivo: 'Inativação documentada' };
    await ServicoContrato.previaCicloVida(9, 'inativar', dados);
    expect(requisitar).toHaveBeenCalledWith(expect.objectContaining({ method: 'POST', url: '/contratos/9/previas/inativar', data: dados, retries: 0 }));
  });

});
