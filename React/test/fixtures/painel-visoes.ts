import { fixture } from './painel-analitico';
import type { GranularidadePainel } from '../../src/contracts/painel-analitico';
import type { PainelVisoesDTO } from '../../src/contracts/painel-visoes';

export function fixtureVisoes(tipo: GranularidadePainel = 'DIARIO'): PainelVisoesDTO {
  const analitico = fixture(tipo);
  analitico.filtros.prestadores = [{ id: 11, nome: 'Empresa sintética' }];
  return { analitico, ranking: [], efetivoAnterior: null, gestao: {
    referencia: '2026-11-10', geradoEm: '2026-11-12T08:00:00',
    escopo: analitico.escopo,
    efetivo: { efetivoPrevisto: 20, efetivoVinculado: 18, empregadosAtivosCadastrados: 18, saldoContratual: 2, percentualOcupacao: 90, ativosSemContrato: 0, contratosVigentes: 1 },
    quadro: { empregadosCadastrados: 18, ativos: 18, inativosOuAfastados: 0, lotacoes: 1, cargos: 1, jornadas: 1, percentualAtivos: 100 },
    analiseCadastro: { admitidosNoPeriodo: 0, admitidosNoMesReferencia: 0, admitidosUltimos12Meses: 0, comDataAdmissao: 17, semDataAdmissao: 1, percentualComDataAdmissao: 94.4 },
    contratos: { total: 1, vigentes: 1, vencidosMarcadosAtivos: 0, vencendoEm30Dias: 0, semDataFim: 0, semEfetivoPrevisto: 0, acimaDoEfetivoPrevisto: 0, abaixoDoEfetivoPrevisto: 1 },
    frequencia: { data: '2026-11-10', empregadosAtivos: 18, empregadosEsperados: 16, registrosEsperados: 15, pendentes: 1, percentualCobertura: 93.75, presencas: 15, ausencias: 0, ausenciasJustificadas: 0, faltasNaoJustificadas: 0, repousosTrabalhados: 0, trabalhosForaEscalaSemRt: 0 },
    periodo: { inicio: '2026-11-10', fim: '2026-11-10', dias: 1, registrosOperacionais: 128, presencas: 100, presencasParciais: 0, atrasos: 0, faltasNaoJustificadas: 28, atestados: 0, suspensoes: 0, dsrPerdidos: 0, repousosTrabalhados: 0, empregadosComAusencia: 1, empregadosComAusenciaRecorrente: 0, taxaPresenca: 78.125, taxaAbsenteismo: 21.875, ultimaAtualizacao: null },
    comparativoPeriodo: null,
    pressaoOperacional: { repousosTrabalhados: 0, empregadosComRt: 0, ausenciasNoPeriodo: 28, rtPorCemAtivos: 0, relacaoRtAusencias: 0 },
    integridade: { totalInconsistencias: 0, empregadosAtivosSemContrato: 0, empregadosComMultiplosContratosVigentes: 0, empregadoContratoGestaoLocalDivergente: 0, empregadoLotacaoGestaoLocalDivergente: 0, empregadoJornadaGestaoLocalDivergente: 0, jornadasComCalendarioIncompleto: 0, empregadosAtivosSemLotacao: 0, empregadosAtivosSemJornada: 0, adicionaisRtSemFrequencia: 0, empregadosSemGestaoLocal: 0, contratosSemGestaoLocal: 0 },
    horizonteContratual: { vencidos: 0, ate30Dias: 0, de31A90Dias: 0, de91A180Dias: 0, acimaDe180Dias: 1, semDataFim: 0 },
    admissoesMensais: [], distribuicaoStatus: [], situacaoQuadro: [], padraoSemanal: [], unidades: [], evolucao: [], alertas: [], notasMetodologicas: [],
  } };
}
