import type { PainelAnaliticoDTO, GranularidadePainel } from '../../src/contracts/painel-analitico';
// Fixture exclusivamente de teste; nunca importada pelo produto.
export const tipos: GranularidadePainel[]=['DIARIO','SEMANAL','MENSAL','TRIMESTRAL','ANUAL'];
export const fixture=(tipo:GranularidadePainel='MENSAL'):PainelAnaliticoDTO=>({
  indicadores:{tempestividade:{dentro:120,fora:8,indeterminado:tipos.indexOf(tipo),emRevisao:3},cobertura:{vinculados:18,esperados:16,lancados:15,pendentes:1,semProva:0,comAdmissao:17},evolucaoPendencias:[],heatmapPendencias:[],contratos:[]},
  referencia:'2026-11-10',geradoEm:'2026-11-11T10:00:00Z',versaoMetodologia:'analitico-1',
  escopo:{nivel:'Gestão Local',nome:'Laboratório',superintendenciaId:1,gestaoLocalId:2,permiteDetalhamentoOperacional:true},
  intervalo:{granularidade:tipo,inicio:tipo==='DIARIO'?'2026-11-10':tipo==='SEMANAL'?'2026-11-09':tipo==='TRIMESTRAL'?'2026-10-01':tipo==='ANUAL'?'2026-01-01':'2026-11-01',fim:'2026-11-10',parcial:true,anteriorInicio:'2026-10-01',anteriorFim:'2026-10-10'},
  selecao:{prestadorId:null,contratoId:null,tipoUnidade:'LOTACAO',unidadeId:null,comparar:true},
  filtros:{prestadores:[],contratos:[{id:8,nome:'Contrato sintético'}],unidades:[{id:9,nome:'Lotação sintética'}],tipoUnidade:'LOTACAO'},
  resumoPeriodo:{registros:128+tipos.indexOf(tipo),presencas:100+tipos.indexOf(tipo),ausencias:28},comparativo:{registros:120,presencas:99,ausencias:21},
  variacoes:{registros:{diferenca:8,variacaoPercentual:6.67},presencas:{diferenca:1,variacaoPercentual:1.01},ausencias:{diferenca:7,variacaoPercentual:33.33}},
  referenciaDiaria:{efetivoPrevisto:20,efetivoAlocado:18,registrosLancados:15,pendentesEntreAlocados:3,coberturaAlocados:83.33,ocupacao:90,contratosVigentes:1},
  evolucao:[],heatmap:{colunas:[],linhas:[]},unidades:[],paginaUnidades:0,totalUnidades:0,tamanhoPagina:20,
  metas:[{codigo:'COBERTURA_ALOCADOS',nome:'Cobertura do efetivo alocado',valor:83.33,meta:null,fonte:'Fonte canônica'}],
  alertas:[],indisponibilidades:['Classificação de tempestividade: aguardando definição negocial.'],metodologia:['Somente dados reais.'],
});
