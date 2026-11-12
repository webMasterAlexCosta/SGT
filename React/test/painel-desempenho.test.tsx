import React from 'react';
import { act, cleanup, fireEvent, render, screen, waitFor } from '@testing-library/react';
import { afterEach, beforeEach, describe, expect, it } from 'vitest';
import '@testing-library/jest-dom/vitest';
import { api } from '../src/core/http';
import { invalidarGeracaoSessao, obterGeracaoSessao } from '../src/core/security/geracaoSessao';
import PainelDesempenho from '../src/modules/painel/components/PainelDesempenho';
import type { PainelAnaliticoDTO } from '../src/contracts/painel-analitico';
import { fixture, tipos } from './fixtures/painel-analitico';
import type { AxiosAdapter, InternalAxiosRequestConfig } from 'axios';
import { AxiosError } from 'axios';
import { linhasRelatorio } from '../src/modules/painel/components/PainelDesempenho/exportacao';

let adapter:typeof api.defaults.adapter;
let pedidos:InternalAxiosRequestConfig[];
const ok=(config:InternalAxiosRequestConfig,data:PainelAnaliticoDTO)=>({config,data,status:200,statusText:'OK',headers:{}});
beforeEach(()=>{
 adapter=api.defaults.adapter;pedidos=[];
 api.defaults.adapter=(async config=>{
  pedidos.push(config);
  if(config.url==='/painel/analitico/registros') return {config,status:200,statusText:'OK',headers:{},data:{registros:[{id:77,data:'2026-11-10',status:'FALTA',versao:1,contratoId:8,unidadeId:9}],pagina:0,total:1,tamanho:20}};
  return ok(config,fixture(config.params.granularidade));
 }) satisfies AxiosAdapter;
});
afterEach(()=>{cleanup();api.defaults.adapter=adapter;invalidarGeracaoSessao();});

describe('Dashboard com contrato HTTP real e transporte isolado',()=>{
 it('apresenta vigência canônica e alerta de vencimento também no relatório',async()=>{
  const dados=fixture();
  dados.indicadores.contratos=[{id:8,nome:'Contrato sintético',prestador:'Prestador fictício',previsto:10,vinculado:9,esperados:9,lancados:8,pendentes:1,vigenciaFim:'2026-12-10'}];
  dados.alertas=[{codigo:'CONTRATOS_VENCENDO_30_DIAS',severidade:'AVISO',titulo:'Vencimento contratual em até 30 dias',mensagem:'1 contrato no filtro pela vigência canônica.',rota:null}];
  api.defaults.adapter=async config=>ok(config,dados);
  render(<PainelDesempenho referenciaInicial="2026-11-10" />);
  await screen.findByText('Vencimento contratual em até 30 dias');
  fireEvent.click(screen.getByRole('button',{name:'Contratos e prestadores',exact:true}));
  expect(screen.getByRole('columnheader',{name:'Vigência até'})).toBeVisible();
  expect(screen.getByRole('cell',{name:'10/12/2026'})).toBeVisible();
  expect(linhasRelatorio(dados).flat()).toContain('2026-12-10');
  expect(linhasRelatorio(dados).flat()).toContain('1 contrato no filtro pela vigência canônica.');
 });
 it('mantém os quatro KPIs aprovados, diário padrão e bases gerenciais',async()=>{
  render(<PainelDesempenho referenciaInicial="2026-11-10" />);
  await screen.findByRole('button',{name:'Total de registros: 128',exact:true});
  expect(pedidos[0].params.granularidade).toBe('DIARIO');
  expect(screen.getByRole('button',{name:/Dentro do prazo:/})).toHaveTextContent('120 de 128 classificados');
  expect(screen.getByRole('button',{name:'Fora do prazo: 8'})).toBeVisible();
  expect(screen.getByRole('button',{name:'Em revisão: 3'})).toBeVisible();
  expect(screen.getByRole('heading',{name:'Funil de cobertura'})).toBeVisible();
  expect(screen.getByRole('heading',{name:'Heatmap — pendências de frequência'})).toBeVisible();
 });

 it('mantém a comparação na métrica selecionada da evolução',async()=>{
  render(<PainelDesempenho referenciaInicial="2026-11-10" />);
  await screen.findByRole('button',{name:'Total de registros: 128',exact:true});
  fireEvent.change(screen.getByLabelText('Métrica da evolução'),{target:{value:'ausencias'}});
  expect(document.querySelector('[aria-label="Comparação de ausências"]')).toHaveTextContent('28 ausências');
 });

 it('limpa contrato e unidade sem trocar o período de análise',async()=>{
  render(<PainelDesempenho referenciaInicial="2026-11-10" />);
  await screen.findByRole('button',{name:/Total de registros/});
  fireEvent.click(screen.getByRole('button',{name:'Anual',exact:true}));
  await screen.findByRole('button',{name:'Total de registros: 132',exact:true});
  fireEvent.change(screen.getByLabelText('Contrato'),{target:{value:'8'}});
  await waitFor(()=>expect(pedidos.at(-1)?.params.contratoId).toBe(8));
  fireEvent.click(screen.getByRole('button',{name:'Limpar contrato e unidade',exact:true}));
  await waitFor(()=>expect(pedidos.at(-1)?.params.contratoId).toBeUndefined());
  expect(pedidos.at(-1)?.params.granularidade).toBe('ANUAL');
  expect(pedidos.at(-1)?.params.referencia).toBe('2026-11-10');
 });
 it('envia os cinco períodos e preserva filtros nas métricas reais',async()=>{
  render(<PainelDesempenho referenciaInicial="2026-11-10" />);
  await screen.findByRole('button',{name:/Total de registros/});
  expect(screen.getByRole('button',{name:'Em revisão: 3'})).toBeVisible();
  for(const [i,rotulo] of ['Diário','Semanal','Mensal','Trimestral','Anual'].entries()) {
   fireEvent.click(screen.getByRole('button',{name:rotulo,exact:true}));
   await waitFor(()=>expect(pedidos.at(-1)?.params.granularidade).toBe(tipos[i]));
   await screen.findByRole('button',{name:/Total de registros/});
  }
  fireEvent.change(screen.getByLabelText('Contrato'),{target:{value:'8'}});
  await waitFor(()=>expect(pedidos.at(-1)?.params.contratoId).toBe(8));
  fireEvent.change(screen.getByLabelText('Unidade'),{target:{value:'9'}});
  await waitFor(()=>expect(pedidos.at(-1)?.params.unidadeId).toBe(9));
  expect(pedidos.at(-1)?.url).toBe('/painel/analitico');
  expect(pedidos.at(-1)?.params.tipoUnidade).toBe('LOTACAO');
 });

 it('descarta resposta antiga após troca rápida de período',async()=>{
  let liberar:((v:ReturnType<typeof ok>)=>void)|undefined;
  api.defaults.adapter=async config=>{
   pedidos.push(config);
   if(config.params.granularidade==='DIARIO') return new Promise(resolve=>{liberar=resolve;});
   return ok(config,fixture('ANUAL'));
  };
  render(<PainelDesempenho referenciaInicial="2026-11-10" />);
  await waitFor(()=>expect(liberar).toBeDefined());
  const antiga=pedidos[0];
  fireEvent.click(screen.getByRole('button',{name:'Anual',exact:true}));
  await screen.findByRole('button',{name:/Total de registros/});
  expect(antiga.signal?.aborted).toBe(true);
  await act(async()=>liberar?.(ok(antiga,fixture('DIARIO'))));
  expect(screen.getByRole('button',{name:/Total de registros/})).toHaveTextContent('132');
 });

 it('abre detalhamento com intervalo e fecha com ESC',async()=>{
  render(<PainelDesempenho referenciaInicial="2026-11-10" />);
  fireEvent.click(await screen.findByRole('button',{name:/Total de registros/}));
  expect(screen.getByRole('dialog')).toHaveTextContent('10/11/2026');
  fireEvent.keyDown(window,{key:'Escape'});
  expect(screen.queryByRole('dialog')).not.toBeInTheDocument();
 });

 it('consulta registros de origem no intervalo do indicador',async()=>{
  render(<PainelDesempenho referenciaInicial="2026-11-10" />);
  fireEvent.click(await screen.findByRole('button',{name:/Total de registros/}));
  expect(await screen.findByRole('cell',{name:'FALTA',exact:true})).toBeVisible();
  const consulta=pedidos.find(p=>p.url==='/painel/analitico/registros');
  expect(consulta?.params.inicio).toBe('2026-11-10');
  expect(consulta?.params.fim).toBe('2026-11-10');
  expect(consulta?.params.metrica).toBe('registros');
 });

 it('403 remove valores antigos e preserva a sessão e os filtros',async()=>{
  render(<PainelDesempenho referenciaInicial="2026-11-10" />);
  await screen.findByRole('button',{name:/Total de registros/});
  const geracao=obterGeracaoSessao();
  api.defaults.adapter=async config=>{throw new AxiosError('Acesso negado','ERR_BAD_RESPONSE',config,undefined,{config,data:{message:'Recorte negado'},status:403,statusText:'Forbidden',headers:{}});};
  fireEvent.change(screen.getByLabelText('Contrato'),{target:{value:'8'}});
  await screen.findByRole('alert');
  expect(screen.queryByRole('button',{name:/Total de registros/})).not.toBeInTheDocument();
  expect(screen.getByLabelText('Contrato')).toHaveValue('8');
  expect(obterGeracaoSessao()).toBe(geracao);
 });

 it('troca de sessão limpa dados e opções já renderizados',async()=>{
  render(<PainelDesempenho referenciaInicial="2026-11-10" />);
  await screen.findByRole('button',{name:/Total de registros/});
  act(()=>{invalidarGeracaoSessao();});
  expect(screen.queryByRole('button',{name:/Total de registros/})).not.toBeInTheDocument();
  expect(screen.queryByRole('option',{name:'Contrato sintético'})).not.toBeInTheDocument();
 });

 it('exporta filtros, período, fonte e indisponibilidade sem números propostos',()=>{
  const relatorio=linhasRelatorio(fixture()).flat();
  expect(relatorio).toContain('01/11/2026 a 10/11/2026');
  expect(relatorio).toContain('Laboratório');
  expect(relatorio).toContain('Classificação de tempestividade: aguardando definição negocial.');
  expect(relatorio).toContain('Somente dados reais.');
 });
});
