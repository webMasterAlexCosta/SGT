import React from 'react';
import { afterEach, beforeEach, expect, it, vi } from 'vitest';
import { cleanup, fireEvent, render, screen } from '@testing-library/react';
import '@testing-library/jest-dom/vitest';
import { api } from '../src/core/http';
import { TabelaFrequencia } from '../src/modules/rh/pages/PaginaFrequencia/components/TabelaFrequencia';
let adapter;
beforeEach(()=>{
 localStorage.setItem('sgt_frequencia_virtualizacao','false');adapter=api.defaults.adapter;
 api.defaults.adapter=async config=>({config,status:200,statusText:'OK',headers:{},data:{frequenciaId:77,versao:2,status:'SEM_SOLICITACAO',solicitadaEm:null,motivo:null,decididaEm:null,decisao:null,podeSolicitar:true,podeDecidir:false}});
});
afterEach(()=>{cleanup();api.defaults.adapter=adapter;localStorage.removeItem('sgt_frequencia_virtualizacao');});
it('permite solicitar conferência na tela operacional mesmo após fechar a edição',async()=>{
 render(<TabelaFrequencia estaCarregando={false} erroCarregamento={null} listaDeFrequencia={[{frequenciaId:77,empregadoId:7,jaSalvo:true,nome:'Colaborador sintético',status:'FALTA',obs:'',lotacao:'Lotação sintética',turno:'Diurno',identidadeCanonicaDisponivel:true,estadoNegocial:'FECHADA',edicaoBloqueadaPeloBackend:true}]} statusDaData={{valid:true,reason:''}} eGestorOperacional={false} edicaoBloqueada={true} funcaoObterJanelaDeEdicao={()=>({isValid:false})} aoSelecionarStatusDaTabela={vi.fn()} aoAtualizarObservacaoDaTabela={vi.fn()} aoAbrirModalParcial={vi.fn()} paginacaoGrade={{pagina:0,totalPaginas:1,totalElementos:1}} aoMudarPagina={vi.fn()} />);
 fireEvent.click(screen.getByRole('button',{name:'Conferência do registro 77'}));
 const dialog=await screen.findByRole('dialog');expect(dialog).toHaveTextContent('Conferência de frequência');
 expect(await screen.findByRole('button',{name:'Solicitar revisão',exact:true})).toBeDisabled();
 fireEvent.change(screen.getByLabelText('Motivo da solicitação'),{target:{value:'Conferir ausência registrada'}});
 expect(screen.getByRole('button',{name:'Solicitar revisão',exact:true})).toBeEnabled();
 expect(screen.queryByRole('button',{name:'Concluir conferência',exact:true})).not.toBeInTheDocument();
 fireEvent.keyDown(window,{key:'Escape'});expect(screen.queryByRole('dialog')).not.toBeInTheDocument();
});
