import { beforeEach, describe, expect, it, vi } from 'vitest';

const { requisitar } = vi.hoisted(() => ({ requisitar: vi.fn() }));
vi.mock('../src/core/http.js', () => ({ requisitar, requisitarDownload: vi.fn(), extrairLista: (r) => Array.isArray(r) ? r : r?.content || [] }));
import { ServicoExcecoesContrato } from '../src/services/admin/ServicoExcecoesContrato.js';
import * as dominio from '../src/services/admin/ServicoDominioExcecoesContrato.js';

const formulario = {
  cnpjCorrigido: '11.222.333/0001-44', dataEfetiva: '2026-10-01', dataAto: '2026-10-02',
  motivo: ' Correção documental sintética ', referenciaSei: ' SEI-CNPJ-SINTETICO ',
  chaveIdempotencia: ' cnpj-intencao-sintetica ', eventoPrecedenteId: '101', relacaoPrecedencia: 'RETIFICA', ordemDocumental: '2',
};
const payload = {
  cnpjCorrigido: '11222333000144', dataEfetiva: '2026-10-01', dataAto: '2026-10-02',
  motivo: 'Correção documental sintética', referenciaSei: 'SEI-CNPJ-SINTETICO',
  chaveIdempotencia: 'cnpj-intencao-sintetica', eventoPrecedenteId: 101, relacaoPrecedencia: 'RETIFICA', ordemDocumental: 2,
};
beforeEach(() => { requisitar.mockReset(); requisitar.mockResolvedValue([]); });

describe('Contrato congelado de correção material de CNPJ', () => {
  it('consulta o histórico pelo GET literal e propaga o cancelamento da consulta', async () => {
    const signal = new AbortController().signal;
    requisitar.mockResolvedValue([{ id: 1, cnpjAnterior: '11111111000111', cnpjCorrigido: '22222222000122' }]);
    expect(typeof ServicoExcecoesContrato.listarCorrecoesCnpj).toBe('function');
    expect(await ServicoExcecoesContrato.listarCorrecoesCnpj(9, { signal })).toHaveLength(1);
    expect(requisitar).toHaveBeenCalledWith({ method: 'GET', url: '/contratos/9/correcoes-cnpj', signal });
  });

  it('envia o DTO de CNPJ pelo POST real com a chave fornecida e a confirmação de senha', async () => {
    expect(typeof ServicoExcecoesContrato.corrigirCnpj).toBe('function');
    await ServicoExcecoesContrato.corrigirCnpj(9, payload, { senhaConfirmacao: 'senha-sintetica' });
    expect(requisitar).toHaveBeenCalledWith(expect.objectContaining({ method: 'POST', url: '/contratos/9/correcoes-cnpj', data: payload, senhaConfirmacao: 'senha-sintetica', retries: 0 }));
    expect(requisitar.mock.calls[0][0].data).not.toHaveProperty('tokenPrevia');
    expect(requisitar.mock.calls[0][0].data).not.toHaveProperty('empresaId');
  });

  it('serializa exatamente os dez campos do DTO, com datas locais e CNPJ normalizado', () => {
    expect(typeof dominio.prepararCorrecaoCnpj).toBe('function');
    expect(dominio.prepararCorrecaoCnpj(formulario)).toEqual(payload);
  });

  it('recusa CNPJ incompleto e precedência incompleta antes de abrir a confirmação', () => {
    expect(typeof dominio.validarCorrecaoCnpj).toBe('function');
    expect(dominio.validarCorrecaoCnpj({ ...formulario, cnpjCorrigido: '123' })).toMatch(/14 dígitos/);
    expect(dominio.validarCorrecaoCnpj({ ...formulario, relacaoPrecedencia: '' })).toMatch(/precedência/i);
    expect(dominio.validarCorrecaoCnpj(formulario)).toBeNull();
  });
});
