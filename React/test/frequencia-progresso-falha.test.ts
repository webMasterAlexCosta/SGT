import { expect, it, vi } from 'vitest';
import { executarLotesFrequenciaSequencial } from '../src/services/rh/ServicoDominioFrequencia';
it('mantém a causa e o progresso de lotes anteriores quando o transporte rejeita Error', async () => {
  const causa = new Error('Lote recusado');
  const enviarLote = vi.fn().mockResolvedValueOnce([1, 2]).mockRejectedValueOnce(causa);
  await expect(executarLotesFrequenciaSequencial({ payloads: [1, 2, 3], tamanhoLote: 2, enviarIndividual: vi.fn(), enviarLote })).rejects.toBe(causa);
  expect(causa).toMatchObject({ progressoFrequencia: { totalSalvo: 2, total: 3, lotesConcluidos: 1, loteFalhou: 2, totalLotes: 2 } });
  expect(enviarLote).toHaveBeenCalledTimes(2);
});
it.each([null, 'falha primitiva'])('preserva causa não Error e progresso sem mascará-los com TypeError (%s)', async (causa) => {
  const enviarLote = vi.fn().mockResolvedValueOnce([1, 2]).mockRejectedValueOnce(causa);
  await expect(executarLotesFrequenciaSequencial({ payloads: [1, 2, 3], tamanhoLote: 2, enviarIndividual: vi.fn(), enviarLote })).rejects.toMatchObject({ cause: causa, progressoFrequencia: { totalSalvo: 2, total: 3, loteFalhou: 2 } });
  expect(enviarLote).toHaveBeenCalledTimes(2);
});
