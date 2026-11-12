// Harness exclusivo de teste visual; não é rota/módulo do produto nem prova de API real.
import React from 'react';
import { createRoot } from 'react-dom/client';
import PainelDesempenho from '../../src/modules/painel/components/PainelDesempenho';
import Rodape from '../../src/Leiaute/Rodape';
import { api } from '../../src/core/http';
import { ServicoSistema } from '../../src/services/system/ServicoSistema';
import { fixture } from '../fixtures/painel-analitico';
import '../../src/styles/tokens.css';
import '../../src/index.css';

api.defaults.adapter = async config => {
  if(config.url === '/painel/analitico/registros') return {config,status:200,statusText:'OK',headers:{},data:{registros:[{id:77,data:'2026-11-10',status:'FALTA',versao:1,contratoId:8,unidadeId:9}],pagina:0,total:1,tamanho:20}};
  if(config.url !== '/painel/analitico') throw new Error('O teste isolado não permite outras consultas.');
  const d=fixture(config.params.granularidade);
  d.escopo.nome='Laboratório — fixture visual isolada';
  d.selecao.comparar=config.params.comparar;
  d.selecao.contratoId=config.params.contratoId??null;
  d.selecao.unidadeId=config.params.unidadeId??null;
  const registros=[25,23,22,21,20,19],presentes=[20,18,17,16,15,14];
  d.resumoPeriodo={registros:130,presencas:100,ausencias:30};
  d.variacoes={registros:{diferenca:10,variacaoPercentual:8.33},presencas:{diferenca:1,variacaoPercentual:1.01},ausencias:{diferenca:9,variacaoPercentual:42.86}};
  d.evolucao=Array.from({length:12},(_,i)=>{
    const ano=i===0?2025:2026,mes=i===0?12:i;
    const inicio=`${ano}-${String(mes).padStart(2,'0')}-01`;
    return {periodo:{inicio,fim:i===11?'2026-11-10':`${ano}-${String(mes).padStart(2,'0')}-${new Date(Date.UTC(ano,mes,0)).getUTCDate()}`,rotulo:`${String(mes).padStart(2,'0')}/${ano}`,parcial:i===11},periodoAnterior:null,atual:{registros:i===11?130:360+i*7,presencas:i===11?100:300+i*5,ausencias:i===11?30:60+i*2},anterior:{registros:i===11?120:345+i*6,presencas:290+i*4,ausencias:55+i*2}};
  });
  d.heatmap.colunas=[{inicio:'2026-11-01',fim:'2026-11-07',rotulo:'Dias 1–7',parcial:false},{inicio:'2026-11-08',fim:'2026-11-10',rotulo:'Dias 8–10',parcial:true}];
  d.unidades=registros.map((r,i)=>({id:i+1,nome:`Unidade sintética ${i+1}`,totais:{registros:r,presencas:presentes[i],ausencias:r-presentes[i]}}));
  d.totalUnidades=d.unidades.length;
  d.heatmap.linhas=d.unidades.map(u=>({unidadeId:u.id,nome:u.nome,celulas:[u.totais,{registros:5,presencas:4,ausencias:1}]}));
  d.metas.push({codigo:'OCUPACAO',nome:'Ocupação do efetivo previsto',valor:90,meta:null,fonte:'Fixture do read model temporal de efetivo.'},{codigo:'CADASTRO',nome:'Qualidade cadastral',valor:null,meta:null,fonte:'Indicador indisponível no recorte.'});
  d.alertas=[{codigo:'ALOCADOS_SEM_REGISTRO',severidade:'AVISO',titulo:'Alocações sem lançamento na referência',mensagem:'3 pendências entre alocados; não significam faltas.',rota:null}];
  if(!d.selecao.comparar) { d.comparativo=null;d.variacoes=null;d.evolucao.forEach(p=>p.anterior=null); }
  return {config,data:d,status:200,statusText:'OK',headers:{}};
};
// A sondagem do rodapé também permanece isolada; nenhuma API externa é acessada.
ServicoSistema.medirLatencia=async()=>{throw new Error('Sondagem desativada no teste visual.');};
document.body.style.cssText='margin:0;background:var(--cor-fundo-aplicacao);font-family:var(--fonte-sistema)';
const raiz=document.getElementById('root');
if(raiz) createRoot(raiz).render(<div style={{maxWidth:1280,margin:'0 auto',padding:'24px'}}><p style={{fontSize:12,color:'#4b5d68'}}>Teste visual isolado · dados fictícios · sem conexão com banco/API</p><PainelDesempenho referenciaInicial="2026-11-10"/><Rodape/></div>);
