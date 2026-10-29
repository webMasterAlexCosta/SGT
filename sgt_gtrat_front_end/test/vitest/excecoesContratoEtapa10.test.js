import { beforeEach, describe, expect, it, vi } from 'vitest';
const requisitar = vi.hoisted(() => vi.fn());
const requisitarDownload = vi.hoisted(() => vi.fn());
vi.mock('../../src/core/http.js', () => ({
  requisitar,
  requisitarDownload,
  extrairLista: (v) => Array.isArray(v) ? v : v?.content || [],
}));
vi.mock('../../src/core/http/idempotencia.js', () => ({ gerarChaveIdempotencia: (p='cmd') => `${p}-teste` }));

import { ServicoExcecoesContrato } from '../../src/services/admin/ServicoExcecoesContrato.js';
import { mensagemErroDocumento, prepararCorrecaoAnulacao, prepararCorrecaoIdentificador, prepararMetadadosDocumento, prepararTransferenciaGestao, validarDocumentoLocal, validarTransferenciaGestao } from '../../src/services/admin/ServicoDominioExcecoesContrato.js';

beforeEach(() => { requisitar.mockReset(); requisitarDownload.mockReset(); requisitar.mockResolvedValue([]); requisitarDownload.mockResolvedValue({ blob:new Blob(['x']), nomeArquivo:'x.pdf' }); });

describe('Etapa 10 - documentos, transferências e correções excepcionais', () => {
  it('consome os 19 endpoints planejados', async () => {
    const id=7, doc=3, perm=9;
    await ServicoExcecoesContrato.listarTransferencias(id); await ServicoExcecoesContrato.transferirGestao(id,{}); await ServicoExcecoesContrato.corrigirAnulacao(id,{});
    await ServicoExcecoesContrato.corrigirIdentificador(id,{}); await ServicoExcecoesContrato.listarCorrecoesIdentificador(id); await ServicoExcecoesContrato.pesquisarHistoricoIdentificador('123',2026);
    await ServicoExcecoesContrato.listarDocumentos(id); await ServicoExcecoesContrato.buscarDocumento(id,doc); await ServicoExcecoesContrato.listarEventosDocumento(id,doc); await ServicoExcecoesContrato.baixarDocumento(id,doc);
    await ServicoExcecoesContrato.criarDocumento(id,{ titulo:'x',tipoFato:'CONTRATO_ORIGINAL',classificacaoAcesso:'NEGOCIAL',referenciaSei:'1' });
    await ServicoExcecoesContrato.retificarSei(id,doc,{}); await ServicoExcecoesContrato.substituirDocumento(id,doc,{ titulo:'x',referenciaSei:'1' }); await ServicoExcecoesContrato.invalidarDocumento(id,doc,{}); await ServicoExcecoesContrato.descartarRascunhoDocumento(id,doc);
    await ServicoExcecoesContrato.listarPermissoesDocumento(id,doc); await ServicoExcecoesContrato.concederPermissaoDocumento(id,doc,{}); await ServicoExcecoesContrato.revogarPermissaoDocumento(id,doc,perm,{}); await ServicoExcecoesContrato.listarAcessosDocumento(id,doc);
    expect(requisitar).toHaveBeenCalledTimes(18); expect(requisitarDownload).toHaveBeenCalledTimes(1);
    const calls=requisitar.mock.calls.map(([c])=>`${c.method} ${c.url}`);
    expect(calls).toContain('POST /contratos/7/transferir-gestao'); expect(calls).toContain('POST /contratos/7/corrigir-anulacao'); expect(calls).toContain('GET /contratos/historico-identificador');
    expect(calls).toContain('POST /contratos/7/documentos/3/permissoes/9/revogar'); expect(calls).toContain('GET /contratos/7/documentos/3/acessos');
  });

  it('multipart usa partes metadados JSON e arquivo opcional, com chave estável no corpo', async () => {
    const arquivo=new File(['%PDF-'], 'teste.pdf', { type:'application/pdf' });
    await ServicoExcecoesContrato.criarDocumento(1,{ titulo:'Contrato',tipoFato:'CONTRATO_ORIGINAL',classificacaoAcesso:'NEGOCIAL' },arquivo);
    const cfg=requisitar.mock.calls[0][0]; expect(cfg.data).toBeInstanceOf(FormData); expect(cfg.data.has('metadados')).toBe(true); expect(cfg.data.get('arquivo')).toBe(arquivo);
  });

  it('valida arquivo/SEI e traduz 413/415 sem inventar limite em bytes', () => {
    expect(validarDocumentoLocal({ titulo:'x',tipoFato:'CONTRATO_ORIGINAL',classificacaoAcesso:'NEGOCIAL',referenciaSei:'' },null)).toMatch(/arquivo|SEI/i);
    expect(validarDocumentoLocal({ titulo:'x',tipoFato:'CONTRATO_ORIGINAL',classificacaoAcesso:'NEGOCIAL',referenciaSei:'SEI' },null)).toBeNull();
    expect(mensagemErroDocumento({ status:413 })).toMatch(/limite.*servidor/i); expect(mensagemErroDocumento({ status:415 })).toMatch(/MIME|formato/i);
  });

  it('correção de identificador e de anulação preservam campos temporais/precedência', () => {
    expect(prepararCorrecaoIdentificador({ novoNumero:' 99 ',novoAno:'2027',dataEfetiva:'2027-01-01',dataAto:'2026-12-20',motivo:' erro ',referenciaSei:' S ',autorizacaoAdministrativa:' A ' })).toMatchObject({ novoNumero:'99',novoAno:2027,dataEfetiva:'2027-01-01',dataAto:'2026-12-20' });
    expect(prepararCorrecaoAnulacao({ dataEfetiva:'2026-09-01',dataAto:'2026-09-02',motivo:'x',referenciaSei:'s',eventoPrecedenteId:'8' })).toMatchObject({ eventoPrecedenteId:8,relacaoPrecedencia:'CANCELA' });
  });

  it('transferência exige mapeamentos completos como comando atômico', () => {
    expect(validarTransferenciaGestao({ dataEfetiva:'2026-10-01',gestaoLocalDestinoId:'2',motivo:'x',referenciaSei:'s',autorizacaoAdministrativa:'a',mapeamentos:[] })).toMatch(/Mapeie/i);
    const payload=prepararTransferenciaGestao({ dataEfetiva:'2026-10-01',gestaoLocalDestinoId:'2',motivo:'x',referenciaSei:'s',autorizacaoAdministrativa:'a',mapeamentos:[{ composicaoOrigemId:'1',lotacaoOrigemId:'2',jornadaOrigemId:'3',cargoOrigemId:'4',lotacaoDestinoId:'5',jornadaDestinoId:'6',cargoDestinoId:'7' }] });
    expect(payload.mapeamentos[0]).toEqual({ composicaoOrigemId:1,lotacaoOrigemId:2,jornadaOrigemId:3,cargoOrigemId:4,lotacaoDestinoId:5,jornadaDestinoId:6,cargoDestinoId:7 });
  });
});
