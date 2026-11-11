import { describe, expect, it } from 'vitest';
import {
  prepararConsultaHistorica, prepararProvaContrato, prepararProvaNome,
  prepararProvaMotivo, prepararTransicaoCatalogo,
} from '../src/services/historico/dominioHistorico.ts';

const fonte = {
  referenciaSei: ' SEI-SINTETICO ', documentoComprobatorioId: '',
  finalidadeAcessoDocumento: '', motivo: ' Fonte documental conferida ',
  autorizacao: ' AUTORIZACAO-SINTETICA ', provaAnteriorId: '',
};

describe('DTOs históricos contra o Java congelado (sem autenticação/DB simuladas como reais)', () => {
  it('preserva versão zero, falso e ausência explícita numa fotografia parcial', () => {
    const dto = prepararProvaContrato({
      ...fonte, autorizacaoAdministrativa: ' AUT-CONTRATO ',
      eventoOrigemId: '21', versaoAnteriorId: '31', versaoContratoEsperada: '0',
      dataDocumento: '2026-01-02', recuperarPublicacaoOriginal: false,
      configuracaoCompletaComprovada: false,
      camposComprovados: ['dataEfetiva', 'matriculaVinculoObrigatoria', 'matriculaTipoContratacaoObrigatoria', 'efetivoPrevisto'],
      fotografia: { dataEfetiva: '2026-01-01', matriculaVinculoObrigatoria: '', matriculaTipoContratacaoObrigatoria: 'false', efetivoPrevisto: '0' },
      composicoes: [],
    });
    expect(dto).toMatchObject({
      eventoOrigemId: 21, versaoAnteriorId: 31, provaAnteriorId: null,
      versaoContratoEsperada: 0, recuperarPublicacaoOriginal: false,
      configuracaoCompletaComprovada: false, documentoComprobatorioId: null,
      referenciaSei: 'SEI-SINTETICO', autorizacaoAdministrativa: 'AUT-CONTRATO',
      fotografia: { dataEfetiva: '2026-01-01', matriculaVinculoObrigatoria: null, matriculaTipoContratacaoObrigatoria: false, efetivoPrevisto: 0 },
    });
    expect(dto.camposComprovados).toEqual(['dataEfetiva', 'matriculaVinculoObrigatoria', 'matriculaTipoContratacaoObrigatoria', 'efetivoPrevisto']);
    expect(dto.fotografia.gestaoLocalNome).toBeNull();
    expect(dto).not.toHaveProperty('autorizacao');
  });

  it('mantém grade comprovada independente de município desconhecido e envia os sete dias', () => {
    const dto = prepararProvaContrato({
      ...fonte, autorizacaoAdministrativa: 'AUT-CONTRATO', eventoOrigemId: 21, versaoAnteriorId: 31, versaoContratoEsperada: 0,
      dataDocumento: '2026-01-02', fotografia: {}, camposComprovados: [],
      composicoes: [{
        composicaoId: '44', mapeamentoId: '', aditamentoComposicaoId: '',
        lotacaoNome: '', jornadaDescricao: ' Jornada documentada ', cargoNome: '',
        municipioComprovado: 'false', lotacaoMunicipio: '', jornadaComprovada: 'true',
        semana: [
          { dia: 'MONDAY', trabalha: true, entrada1: '08:00', saida1: '12:00', entrada2: '13:00', saida2: '17:00' },
          { dia: 'TUESDAY', trabalha: false }, { dia: 'WEDNESDAY', trabalha: false },
          { dia: 'THURSDAY', trabalha: false }, { dia: 'FRIDAY', trabalha: false },
          { dia: 'SATURDAY', trabalha: false }, { dia: 'SUNDAY', trabalha: false },
        ],
      }],
    });
    expect(dto.composicoes[0]).toEqual({
      composicaoId: 44, mapeamentoId: null, aditamentoComposicaoId: null,
      lotacaoNome: null, jornadaDescricao: 'Jornada documentada', cargoNome: null,
      municipioComprovado: false, lotacaoMunicipio: null, jornadaComprovada: true,
      jornadaSemanal: 'MONDAY|true|08:00|12:00|13:00|17:00;TUESDAY|false||||;WEDNESDAY|false||||;THURSDAY|false||||;FRIDAY|false||||;SATURDAY|false||||;SUNDAY|false||||',
    });
  });

  it('recusa duas origens canônicas na mesma composição', () => {
    expect(() => prepararProvaContrato({
      ...fonte, autorizacaoAdministrativa: 'AUT-CONTRATO', versaoContratoEsperada: 0, dataDocumento: '2026-01-02',
      fotografia: {}, camposComprovados: [], composicoes: [{ composicaoId: '44', mapeamentoId: '45' }],
    })).toThrow(/uma única origem/i);
  });

  it('recusa grade declarada comprovada sem os sete dias', () => {
    expect(() => prepararProvaContrato({
      ...fonte, autorizacaoAdministrativa: 'AUT-CONTRATO', versaoContratoEsperada: 0, dataDocumento: '2026-01-02',
      fotografia: {}, camposComprovados: [], composicoes: [{ composicaoId: '44', jornadaComprovada: 'true', semana: [] }],
    })).toThrow(/sete dias/i);
  });

  it('exige o nome ao selecionar município presente; ausência depende de declaração própria', () => {
    const form = { ...fonte, autorizacaoAdministrativa: 'AUT-CONTRATO', versaoContratoEsperada: 0, dataDocumento: '2026-01-02', fotografia: {}, camposComprovados: [],
      composicoes: [{ composicaoId: 44, municipioEstado: 'true', municipioComprovado: 'true', lotacaoMunicipio: '', jornadaComprovada: 'false' }] };
    expect(() => prepararProvaContrato(form)).toThrow(/nome.*município|município.*nome/i);
    const ausencia = prepararProvaContrato({ ...form, composicoes: [{ ...form.composicoes[0], municipioEstado: 'ausente' }] });
    expect(ausencia.composicoes[0]).toMatchObject({ municipioComprovado: true, lotacaoMunicipio: null, jornadaComprovada: false, jornadaSemanal: null });
  });

  it('conserva ambas as versões e a identidade canônica ao comprovar nome', () => {
    const dto = prepararProvaNome({ ...fonte, vinculoId: '12', nomeComprovado: ' Nome documental ',
      eventoOrigemRetificadoId: '45', versaoPessoaEsperada: '0', versaoVinculoEsperada: '0',
      dataInicioEfetiva: '2026-01-01', dataFimEfetiva: '' });
    expect(dto).toEqual({
      vinculoId: 12, nomeComprovado: 'Nome documental', eventoOrigemRetificadoId: 45,
      versaoPessoaEsperada: 0, versaoVinculoEsperada: 0,
      dataInicioEfetiva: '2026-01-01', dataFimEfetiva: null,
      referenciaSei: 'SEI-SINTETICO', documentoComprobatorioId: null,
      finalidadeAcessoDocumento: null, motivo: 'Fonte documental conferida',
      autorizacao: 'AUTORIZACAO-SINTETICA', provaAnteriorId: null,
    });
  });

  it('recusa intervalo invertido e ID inseguro antes de consultar', () => {
    expect(() => prepararProvaNome({ ...fonte, vinculoId: '12', nomeComprovado: 'Nome', versaoPessoaEsperada: 0,
      versaoVinculoEsperada: 0, dataInicioEfetiva: '2026-02-01', dataFimEfetiva: '2026-01-01' })).toThrow(/intervalo|fim/i);
    expect(() => prepararConsultaHistorica({ id: '9007199254740993', data: '2026-01-01', conhecidoAte: '2026-01-02T00:00:00Z' })).toThrow(/identificador|ID/i);
    expect(() => prepararConsultaHistorica({ id: 0, data: '2026-01-01', conhecidoAte: '2026-01-02T00:00:00Z' })).toThrow(/identificador|ID/i);
  });

  it('preserva os cortes distintos e rejeita Instant sem fuso explícito', () => {
    expect(prepararConsultaHistorica({ id: '8', data: '2026-02-28', conhecidoAte: '2026-03-01T12:30:00-03:00' })).toEqual({ id: 8, data: '2026-02-28', conhecidoAte: '2026-03-01T12:30:00-03:00' });
    expect(() => prepararConsultaHistorica({ id: 8, data: '2026-02-28', conhecidoAte: '2026-03-01T12:30' })).toThrow(/offset|fuso|ISO/i);
    expect(() => prepararConsultaHistorica({ id: 8, data: '2026-02-30', conhecidoAte: '2026-03-01T12:30:00Z' })).toThrow();
  });

  it('mantém transição inativa com versão zero, sem converter falso para ausente', () => {
    expect(prepararTransicaoCatalogo({ dataEfetiva: '2026-01-01', ativo: 'false', versaoEsperada: '0', motivo: ' Ato ', evidencia: ' SEI ', autorizacao: ' AUT ' })).toEqual({
      dataEfetiva: '2026-01-01', ativo: false, versaoEsperada: 0, motivo: 'Ato', evidencia: 'SEI', autorizacao: 'AUT',
    });
  });

  it('recusa categoria livre e conserva as duas versões do motivo', () => {
    const form = { ...fonte, contratoId: '9', descricaoComprovada: ' Fim documentado ', categoriaComprovada: 'OUTRO',
      versaoMotivoEsperada: '0', versaoContratoEsperada: '0', dataInicioEfetiva: '2026-01-01', dataFimEfetiva: '' };
    expect(prepararProvaMotivo(form)).toMatchObject({ contratoId: 9, descricaoComprovada: 'Fim documentado', categoriaComprovada: 'OUTRO', versaoMotivoEsperada: 0, versaoContratoEsperada: 0, dataFimEfetiva: null });
    expect(() => prepararProvaMotivo({ ...form, categoriaComprovada: 'CATEGORIA_INVENTADA' })).toThrow(/categoria/i);
  });
});
