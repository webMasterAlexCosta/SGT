import { beforeEach, describe, expect, it, vi } from 'vitest';
const { requisitar } = vi.hoisted(() => ({ requisitar: vi.fn() }));
vi.mock('../src/core/http.ts', () => ({ requisitar, extrairLista: (v) => Array.isArray(v) ? v : v?.content || [] }));
import { ServicoHistorico } from '../src/services/historico/ServicoHistorico.ts';

beforeEach(() => { requisitar.mockReset(); requisitar.mockResolvedValue([]); });

describe('Rotas históricas Java pelo transporte existente', () => {
  it('obtém somente versões atuais da mesma Pessoa/Vínculo, sem usar o nome atual como história', async () => {
    requisitar.mockImplementation(({ url }) => Promise.resolve(url === '/pessoas/4' ? { id: 4, version: 0, nome: 'Nome atual' } : { id: 5, version: 7, pessoaId: 4 }));
    expect(await ServicoHistorico.obterVersoesAtuais('nome', { id: 4, vinculoId: 5 })).toEqual({ versaoPessoaEsperada: 0, versaoVinculoEsperada: 7 });
    expect(requisitar.mock.calls.map(([cfg]) => cfg.url)).toEqual(['/pessoas/4', '/vinculos-contratuais/5']);
  });

  it('obtém as versões de contrato e motivo nos GET individuais documentados', async () => {
    requisitar.mockResolvedValue({ version: 0, descricao: 'Valor atual' });
    expect(await ServicoHistorico.obterVersoesAtuais('contrato', { id: 9 })).toEqual({ versaoContratoEsperada: 0 });
    expect(await ServicoHistorico.obterVersoesAtuais('motivo', { id: 6, contratoId: 9 })).toEqual({ versaoMotivoEsperada: 0, versaoContratoEsperada: 0 });
    expect(requisitar.mock.calls.map(([cfg]) => cfg.url)).toEqual(['/contratos/9', '/motivos-desligamento/6', '/contratos/9']);
  });

  it.each([['LOTACAO', '/lotacoes/8'], ['JORNADA', '/jornadas/8'], ['CARGO', '/cargos/8'], ['TIPO_CONTRATACAO', '/tipos-contratacao/8'], ['MOTIVO_DESLIGAMENTO', '/motivos-desligamento/8']])('consulta versão do catálogo %s sem inventar endpoint', async (tipoCatalogo, url) => {
    requisitar.mockResolvedValue({ version: 0 });
    expect(await ServicoHistorico.obterVersoesAtuais('catalogo', { tipoCatalogo, id: 8 })).toEqual({ versaoEsperada: 0 });
    expect(requisitar).toHaveBeenCalledWith(expect.objectContaining({ method: 'GET', url }));
  });

  it('recusa cadastro atual sem versão, sem tomar número da fotografia como concorrência', async () => {
    requisitar.mockResolvedValue({ numeroVersao: 12, version: null });
    await expect(ServicoHistorico.obterVersoesAtuais('contrato', { id: 9 })).rejects.toThrow(/versão.*atual|atual.*versão/i);
  });

  it('envia data e corte de conhecimento à posição, sem reconstrução local', async () => {
    const resultado = { contratoId: 9, versaoContratual: null, eventosAplicados: [], conhecidoAte: '2026-01-02T00:00:00Z' };
    requisitar.mockResolvedValue(resultado);
    expect(await ServicoHistorico.consultarContrato({ id: 9, data: '2026-01-01', conhecidoAte: '2026-01-02T00:00:00Z' })).toBe(resultado);
    expect(requisitar).toHaveBeenCalledWith(expect.objectContaining({ method: 'GET', url: '/contratos/9/posicao-temporal', params: { data: '2026-01-01', conhecidoAte: '2026-01-02T00:00:00Z' } }));
  });

  it('usa os parâmetros de Pessoa/Vínculo e motivo/contrato previstos nos controladores', async () => {
    await ServicoHistorico.consultarNome({ id: 4, vinculoId: 5, data: '2026-01-01', conhecidoAte: '2026-01-02T00:00:00Z' });
    await ServicoHistorico.listarProvasNome({ id: 4, vinculoId: 5 });
    await ServicoHistorico.consultarMotivo({ id: 6, contratoId: 9, data: '2026-01-01', conhecidoAte: '2026-01-02T00:00:00Z' });
    await ServicoHistorico.listarProvasMotivo({ id: 6, contratoId: 9 });
    expect(requisitar.mock.calls.map(([cfg]) => [cfg.url, cfg.params])).toEqual([
      ['/pessoas/4/nome-historico', { vinculoId: 5, data: '2026-01-01', conhecidoAte: '2026-01-02T00:00:00Z' }],
      ['/pessoas/4/provas-nome', { vinculoId: 5 }],
      ['/motivos-desligamento/6/contexto-historico', { contratoId: 9, data: '2026-01-01', conhecidoAte: '2026-01-02T00:00:00Z' }],
      ['/motivos-desligamento/6/provas-historicas', { contratoId: 9 }],
    ]);
  });

  it('isola a cadeia sem corte da consulta bitemporal e usa somente tipos canônicos de catálogo', async () => {
    await ServicoHistorico.listarVersoes({ id: 9 });
    await ServicoHistorico.listarEventos({ id: 9 });
    await ServicoHistorico.listarProvasContrato({ id: 9, conhecidoAte: '2026-01-02T00:00:00Z' });
    await ServicoHistorico.consultarCatalogo({ tipoCatalogo: 'JORNADA', id: 8, data: '2026-01-01', conhecidoAte: '2026-01-02T00:00:00Z' });
    expect(requisitar.mock.calls.map(([cfg]) => [cfg.url, cfg.params])).toEqual([
      ['/contratos/9/versoes', undefined], ['/contratos/9/eventos', undefined],
      ['/contratos/9/provas-historicas', { conhecidoAte: '2026-01-02T00:00:00Z' }],
      ['/catalogos-temporais/JORNADA/8/disponibilidade', { data: '2026-01-01', conhecidoAte: '2026-01-02T00:00:00Z' }],
    ]);
    await expect(ServicoHistorico.consultarCatalogo({ tipoCatalogo: 'TIPO_LIVRE', id: 8, data: '2026-01-01' })).rejects.toThrow(/catálogo/i);
  });

  it('não atribui corte de conhecimento ao endpoint de composições que aceita apenas data', async () => {
    await ServicoHistorico.listarComposicoes({ id: 9, data: '2026-01-01', conhecidoAte: '2026-01-02T00:00:00Z' });
    expect(requisitar).toHaveBeenCalledWith(expect.objectContaining({ method: 'GET', url: '/contratos/9/composicoes', params: { data: '2026-01-01' } }));
  });

  it.each([
    ['contrato', { id: 9 }, '/contratos/9/provas-historicas'],
    ['catalogo', { tipoCatalogo: 'CARGO', id: 8 }, '/catalogos-temporais/CARGO/8/transicoes'],
    ['nome', { id: 4, vinculoId: 5 }, '/pessoas/4/provas-nome'],
    ['motivo', { id: 6, contratoId: 9 }, '/motivos-desligamento/6/provas-historicas'],
  ])('retém a mesma intenção e chave na prévia/confirmação de %s', async (tipo, alvo, url) => {
    const dados = { motivo: 'Documento sintético', versaoEsperada: 0, chaveIdempotencia: 'intencao-fixa' };
    await ServicoHistorico.analisar(tipo, alvo, dados);
    await ServicoHistorico.confirmar(tipo, alvo, { ...dados, tokenPrevia: 'token-java', confirmarImpacto: true });
    expect(requisitar.mock.calls[0][0]).toMatchObject({ method: 'POST', url: `${url}/previa`, data: dados, retries: 0 });
    expect(requisitar.mock.calls[1][0]).toMatchObject({ method: 'POST', url, data: { ...dados, tokenPrevia: 'token-java', confirmarImpacto: true }, retries: 0, mutacaoIdempotente: true });
  });
});
