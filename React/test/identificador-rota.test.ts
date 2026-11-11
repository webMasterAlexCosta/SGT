import { beforeEach, expect, it, vi } from 'vitest';
const { enviar } = vi.hoisted(() => ({ enviar: vi.fn() }));
vi.mock('../src/core/http', () => ({ requisitar: enviar, extrairLista: (value: unknown) => value, extrairPagina: (value: unknown) => value }));
import { ServicoContrato } from '../src/services/admin/ServicoContrato';

beforeEach(() => { enviar.mockReset(); enviar.mockResolvedValue({ id: 7 }); });
it.each([null, undefined, ''])('recusa identificação ausente %s antes de montar a URL', async (id) => {
  await expect(ServicoContrato.buscarPorId(id)).rejects.toThrow(TypeError);
  expect(enviar).not.toHaveBeenCalled();
});
it.each([7, '7', 0, -1, 'identificador-presente'])('preserva o identificador presente %s e o contexto da requisição', async (id) => {
  const controller = new AbortController();
  await ServicoContrato.buscarPorId(id, { signal: controller.signal, withCredentials: true, headers: { 'X-CSRF-TOKEN': 'csrf-fixture' } });
  expect(enviar).toHaveBeenCalledWith(expect.objectContaining({ method: 'GET', url: `/contratos/${id}`, signal: controller.signal, withCredentials: true, headers: { 'X-CSRF-TOKEN': 'csrf-fixture' } }));
});

import { ServicoEmpresaContratada } from '../src/services/admin/ServicoEmpresaContratada';
import { ServicoRelatorioAdministrativo } from '../src/services/rh/ServicoRelatorioAdministrativo';
const operacoesAssincronas = [
  ['atualizar fornecedor', (id: null | undefined) => ServicoEmpresaContratada.atualizar(id, { razaoSocial: 'Fornecedor', cnpj: '00000000000000', email: null, ativo: true })],
  ['vincular contrato ausente', (id: null | undefined) => ServicoEmpresaContratada.vincularAoContrato(id, 7)],
  ['vincular fornecedor ausente', (id: null | undefined) => ServicoEmpresaContratada.vincularAoContrato(7, id)],
  ['obter emissão', (id: null | undefined) => ServicoRelatorioAdministrativo.obter(id)],
  ['comparar emissão', (id: null | undefined) => ServicoRelatorioAdministrativo.compararComVigente(id)],
] as const;
it.each(operacoesAssincronas)('%s entrega ausência à cadeia .catch sem transportar', async (_rotulo, executar) => {
  for (const id of [null, undefined]) {
    const capturar = vi.fn();
    await executar(id).then(() => { throw new Error('Não deveria resolver'); }).catch(capturar);
    expect(capturar).toHaveBeenCalledWith(expect.any(TypeError));
  }
  expect(enviar).not.toHaveBeenCalled();
});

import { ServicoExportacaoRelatorio } from '../src/core/exportacao/ServicoExportacaoRelatorio';
it.each([null, undefined])('consulta de job rejeita ausência %s pela Promise e não transporta', async (id) => {
  const capturar = vi.fn();
  await ServicoExportacaoRelatorio.consultar(id).catch(capturar);
  expect(capturar).toHaveBeenCalledWith(expect.any(TypeError));
  expect(enviar).not.toHaveBeenCalled();
});
it.each([null, undefined])('URL pura de download recusa ausência %s sem montar endereço', (id) => {
  expect(() => ServicoExportacaoRelatorio.obterUrlDownload(id)).toThrow(TypeError);
  expect(enviar).not.toHaveBeenCalled();
});
it.each(['job-7', 0, -1, 'job /x'])('preserva e codifica o identificador de job presente %s', async (id) => {
  await ServicoExportacaoRelatorio.consultar(id);
  expect(enviar).toHaveBeenCalledWith(expect.objectContaining({ method: 'GET', url: `/relatorios/exportacoes/${encodeURIComponent(id)}`, retries: 1 }));
  expect(ServicoExportacaoRelatorio.obterUrlDownload(id)).toContain(`/relatorios/exportacoes/${encodeURIComponent(id)}/arquivo`);
});
