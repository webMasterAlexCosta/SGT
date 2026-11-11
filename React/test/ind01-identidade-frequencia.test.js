import { beforeEach, describe, expect, it, vi } from 'vitest';

const { requisitar } = vi.hoisted(() => ({ requisitar: vi.fn() }));
vi.mock('../src/core/http.ts', async (original) => ({ ...(await original()), requisitar }));
import { ServicoVinculoContratual } from '../src/services/rh/ServicoVinculoContratual.ts';
import { montarPayloadFrequenciaCanonica, possuiIdentidadeCanonicaFrequencia } from '../src/services/rh/ServicoDominioFrequencia.ts';

const contexto = { data: '2026-11-01', contratoIds: [9] };
const base = { empregadoId: 17, pessoaId: 101, nome: 'Ana Silva', matricula: 'MAT-123', cpf: '111.111.111-11', lotacaoId: 7, turnoId: 8 };

beforeEach(() => { requisitar.mockReset(); });

describe('IND-01: identidade de frequência não é deduzida de resultados textuais', () => {
  it.each([
    ['homônimo com CPF e matrícula coincidentes', [{ id: 902, pessoaId: 202, contratoId: 9, matricula: 'MAT-123', pessoaNome: 'Ana Silva', pessoaCpf: '11111111111' }], 7],
    ['mesma Pessoa com vínculos múltiplos', [{ id: 902, pessoaId: 101, contratoId: 9, matricula: 'MAT-123' }, { id: 903, pessoaId: 101, contratoId: 9, matricula: 'MAT-123' }], 7],
    ['candidato único em outra lotação', [{ id: 902, pessoaId: 101, contratoId: 9, matricula: 'MAT-123' }], 99],
    ['candidato único na mesma lotação e jornada sem associação da linha', [{ id: 902, pessoaId: 101, contratoId: 9, matricula: 'MAT-123' }], 7],
  ])('recusa %s sem sequer iniciar busca inferencial', async (_nome, candidatos, lotacaoId) => {
    requisitar.mockImplementation(async ({ url }) => url === '/vinculos-contratuais'
      ? { content: candidatos, totalPages: 1 }
      : { vinculoId: 902, dataReferencia: '2026-11-01', lotacaoId, jornadaId: 8 });
    await expect(ServicoVinculoContratual.resolverVinculoAtivoParaFrequencia(base, contexto)).rejects.toThrow(/associação|identidade|vínculo/i);
    expect(requisitar).not.toHaveBeenCalled();
  });

  it('preserva o vínculo canônico explícito sem pesquisa textual', async () => {
    await expect(ServicoVinculoContratual.resolverVinculoAtivoParaFrequencia({ ...base, vinculoContratualId: 501 }, contexto)).resolves.toBe(501);
    expect(requisitar).not.toHaveBeenCalled();
  });

  it.each([null, 0, -1, 2.5, Infinity, '501x', true, Number.MAX_SAFE_INTEGER + 1])('não promove ID inválido %s a identidade canônica', async (vinculoContratualId) => {
    requisitar.mockResolvedValue({ content: [], totalPages: 1 });
    const linha = { ...base, vinculoContratualId };
    expect(possuiIdentidadeCanonicaFrequencia(linha)).toBe(false);
    await expect(ServicoVinculoContratual.resolverVinculoAtivoParaFrequencia(linha, contexto)).rejects.toThrow();
  });
});

describe('IND-01: transporte conserva a identidade de origem comprovada', () => {
  it('envia vínculo e Empregado original para a checagem de correspondência no Java', () => {
    const payload = montarPayloadFrequenciaCanonica({ linha: { ...base, vinculoContratualId: 501, status: 'PRESENCA' }, dataReferencia: '2026-11-01' });
    expect(payload).toMatchObject({ vinculoContratualId: 501, empregado: { id: 17 }, dataReferencia: '2026-11-01', status: 'PRESENCA' });
    expect(payload).not.toHaveProperty('cpf');
    expect(payload).not.toHaveProperty('nome');
    expect(payload).not.toHaveProperty('matricula');
  });

  it('não produz POST compatível apenas por Empregado quando o contrato público exige vínculo', () => {
    expect(() => montarPayloadFrequenciaCanonica({ linha: { ...base, status: 'PRESENCA' }, dataReferencia: '2026-11-01' })).toThrow(/vinculoContratualId/);
  });
});

describe('IND-01: associação exata do histórico autorizado ao Gestor Local', () => {
  const consulta = { ...contexto, consultarAssociacaoPorEmpregado: true };
  const associacao = (id = 501) => ({ empregadoId: 17, contratoId: 9, vinculoCanonicoId: id, dataReferenciaIdentidade: '2026-11-01' });
  const posicao = (id = 501, lotacaoId = 7) => ({ vinculoId: id, dataReferencia: '2026-11-01', alocacaoId: 61, situacaoTemporal: 'VIGENTE', lotacaoId, jornadaId: 8 });

  it('usa somente a associação explícita Empregado/Vínculo/data e os IDs da alocação', async () => {
    requisitar.mockImplementation(async ({ url }) => url === '/empregados/17/historico-contratos' ? [associacao()] : posicao());
    await expect(ServicoVinculoContratual.resolverVinculoAtivoParaFrequencia(base, consulta)).resolves.toBe(501);
    expect(requisitar.mock.calls.map(([r]) => [r.url, r.params])).toEqual([
      ['/empregados/17/historico-contratos', { data: '2026-11-01' }],
      ['/vinculos-contratuais/501/alocacoes/posicao', { data: '2026-11-01' }],
    ]);
  });

  it.each([
    ['outro Empregado', { ...associacao(), empregadoId: 18 }],
    ['outra data', { ...associacao(), dataReferenciaIdentidade: '2026-10-31' }],
    ['identidade ausente', { ...associacao(), vinculoCanonicoId: null }],
    ['contrato fora do filtro', { ...associacao(), contratoId: 99 }],
  ])('recusa associação retornada para %s', async (_caso, historico) => {
    requisitar.mockResolvedValue([historico]);
    await expect(ServicoVinculoContratual.resolverVinculoAtivoParaFrequencia(base, consulta)).rejects.toThrow();
    expect(requisitar.mock.calls.every(([r]) => r.url === '/empregados/17/historico-contratos')).toBe(true);
  });

  it('não aceita candidato único cuja alocação pertença a outra lotação', async () => {
    requisitar.mockImplementation(async ({ url }) => url === '/empregados/17/historico-contratos' ? [associacao()] : posicao(501, 99));
    await expect(ServicoVinculoContratual.resolverVinculoAtivoParaFrequencia(base, consulta)).rejects.toThrow();
    expect(requisitar).toHaveBeenCalledTimes(2);
  });

  it('recusa múltiplos vínculos canônicos na mesma lotação e jornada', async () => {
    requisitar.mockImplementation(async ({ url }) => url === '/empregados/17/historico-contratos'
      ? [associacao(501), associacao(502)] : posicao(url.includes('/501/') ? 501 : 502));
    await expect(ServicoVinculoContratual.resolverVinculoAtivoParaFrequencia(base, consulta)).rejects.toThrow(/ambígua|mais de um|múltiplos/i);
  });

  it('seleciona vínculo associado explicitamente à linha/data e alocação exata entre contextos distintos', async () => {
    requisitar.mockImplementation(async ({ url }) => url === '/empregados/17/historico-contratos'
      ? [associacao(501), associacao(502)] : url.includes('/501/') ? posicao(501, 99) : posicao(502, 7));
    await expect(ServicoVinculoContratual.resolverVinculoAtivoParaFrequencia(base, consulta)).resolves.toBe(502);
  });

  it('preserva recusa HTTP do histórico e não recorre a busca textual', async () => {
    requisitar.mockRejectedValue(Object.assign(new Error('Histórico contratual não é tela operacional'), { status: 403 }));
    await expect(ServicoVinculoContratual.resolverVinculoAtivoParaFrequencia(base, consulta)).rejects.toMatchObject({ status: 403 });
    expect(requisitar).toHaveBeenCalledTimes(1);
  });
});
