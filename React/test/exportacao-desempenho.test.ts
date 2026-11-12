// @vitest-environment node
import { expect, it } from 'vitest';
import { criarPdf, linhasRelatorio } from '../src/modules/painel/components/PainelDesempenho/exportacao';
import { fixture } from './fixtures/painel-analitico';

it('mantém comparação e as três métricas da evolução no PDF real', async () => {
  const dados = fixture();
  dados.comparativo = { registros: 987, presencas: 876, ausencias: 111 };
  dados.evolucao = [{
    periodo: { inicio: '2026-10-01', fim: '2026-10-31', rotulo: 'Outubro', parcial: false },
    periodoAnterior: null, atual: { registros: 765, presencas: 654, ausencias: 111 }, anterior: null,
  }];
  const doc = await criarPdf(dados);
  const pdf = doc.output();
  expect(pdf).toContain('(Anterior)');
  for (const valor of ['987', '876', '111', '765', '654']) expect(pdf).toContain(`(${valor})`);
  expect(pdf).toContain('(Metodologia)');
});

it('exporta pendências e bases anteriores sem omitir indisponibilidade',()=>{
 const dados=fixture();const periodo={inicio:'2026-11-01',fim:'2026-11-10',rotulo:'Novembro',parcial:true};
 dados.evolucao=[{periodo,periodoAnterior:{...periodo,inicio:'2026-10-01',fim:'2026-10-10'},atual:{registros:23,presencas:15,ausencias:8},anterior:{registros:27,presencas:20,ausencias:7}}];
 dados.indicadores.evolucaoPendencias=[{periodo,atual:{vinculados:27,esperados:20,lancados:15,pendentes:5,semProva:0,comAdmissao:24},anterior:{vinculados:27,esperados:20,lancados:17,pendentes:3,semProva:1,comAdmissao:24}}];
 const linhas=linhasRelatorio(dados);
 expect(linhas.some(l=>l.includes('5 / 20') && l.includes('3 / 20'))).toBe(true);
 expect(linhas.flat().join(' ')).toContain('Incompleta: 1 sem prova');
 expect(linhas.flat().join(' ')).toContain('01/10/2026 a 10/10/2026');
 expect(linhas.some(l=>l.includes('registros') && l.includes('23 / 27'))).toBe(true);
});
