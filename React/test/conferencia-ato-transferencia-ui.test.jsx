// @vitest-environment jsdom
// Fixtures integralmente sintéticas. Nenhuma confirmação humana é fornecida por props ou pelo mock HTTP.
import React from 'react';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { act, cleanup, fireEvent, render, screen, waitFor, within } from '@testing-library/react';
import '@testing-library/jest-dom/vitest';
const { requisitar, confirmarComSenha, auth } = vi.hoisted(() => ({ requisitar: vi.fn(), confirmarComSenha: vi.fn(), auth: { user: { id: 7, perfil: 'GESTOR_LOCAL', gestaoLocalId: 1 } } }));
vi.mock('../src/core/http.ts', () => ({ requisitar, requisitarDownload: vi.fn(), extrairLista: r => Array.isArray(r) ? r : r?.content || [], extrairPagina: r => Array.isArray(r) ? { content: r, totalPages: 1 } : r }));
vi.mock('../src/modules/auth/context.tsx', () => ({ useAutenticacao: () => ({ user: auth.user }) }));
vi.mock('../src/core/security/useConfirmacaoSenha.tsx', () => ({ useConfirmacaoSenha: () => ({ confirmarComSenha, ModalConfirmacao: () => null, fechar: vi.fn() }) }));
vi.mock('../src/core/events.ts', () => ({ notificar: vi.fn() }));
// Somente símbolos já existentes: este arquivo deve carregar na fase RED contra a fonte atual.
import { PainelExcecoesContrato } from '../src/modules/admin/pages/PaginaContratos/components/PainelExcecoesContrato/index.tsx';
import { prepararTransferenciaGestao } from '../src/services/admin/ServicoDominioExcecoesContrato.ts';
const VERSAO = 'transferencia-gestao-conferencia-v1';
const DECLARACAO = 'Declaro que conferi as referências informadas e que a autorização administrativa do ato abrange esta operação, suas datas, as Gestões de origem e destino, o Tipo de Contratação, todos os mapeamentos e o ato anterior indicado, quando houver. Esta declaração registra minha conferência e não comprova leitura, autenticidade ou validação jurídica pelo SGT.';
const nomeConferencia = 'Conferência documental deste ato';
const mapa = { id: 70, composicaoOrigemId: 31, lotacaoOrigemId: 32, jornadaOrigemId: 33, cargoOrigemId: 34, lotacaoDestinoId: 42, jornadaDestinoId: 43, cargoDestinoId: 44 };
const conferenciaAnterior = { versaoDeclaracao: VERSAO, declaracao: DECLARACAO, versaoIntencao: 'intencao-sintetica-v1', intencaoCanonica: '{"fixture":"ato-sintetico-anterior"}', hashIntencao: 'a'.repeat(64), autorizacaoAdministrativaAto: 'ATO-SINTETICO-ANTERIOR', usoPermissaoExcepcionalId: 201, permissaoExcepcionalId: 202 };
const transferencia = { id: 17, contratoId: 9, eventoContratoId: 101, gestaoOrigemId: 1, gestaoOrigemNome: 'Origem LAB', gestaoDestinoId: 23, gestaoDestinoNome: 'Destino LAB', dataEfetiva: '2099-11-05', motivo: 'Ato sintético anterior', referenciaSei: 'SEI-FICTICIO-ANTERIOR', autorizacaoAdministrativa: 'ATO-SINTETICO-ANTERIOR', registradoEm: '2026-11-01T12:00:00Z', mapeamentos: [mapa], tipoContratacaoDestinoId: 71, conferenciaDocumental: conferenciaAnterior };
const evento = { id: 101, tipoEvento: 'TRANSFERENCIA_GESTAO_LOCAL', situacaoFato: 'VIGENTE', dataEfetiva: '2099-11-05', registradoEm: '2026-11-01T12:00:00Z', conferenciaDocumental: conferenciaAnterior };
const previa = { contratoId: 9, operacao: 'transferir-gestao', tokenPrevia: 'PREVIA-SINTETICA-64', impactosConhecidos: ['Impactos sintéticos do ato atual'], vinculosAfetados: [], programacoesFuturasAfetadas: [], datasComFrequenciasAfetadas: [], exigeConfirmacao: true };
const responder = async p => {
 if (p.url.endsWith('/transferencias-gestao/previa')) return previa;
 if (p.url.endsWith('/transferencias-gestao')) return [transferencia];
 if (p.url.endsWith('/eventos')) return [evento];
 if (p.url.endsWith('/composicoes')) return [{ id: 31, lotacaoId: 32, jornadaId: 33, cargoId: 34, permiteNovaAlocacao: true }];
 if (p.method === 'POST') return { id: 9, estadoNegocial: 'PUBLICADO' };
 return [];
};
beforeEach(() => { auth.user = { id: 7, perfil: 'GESTOR_LOCAL', gestaoLocalId: 1 }; requisitar.mockReset(); requisitar.mockImplementation(responder); confirmarComSenha.mockReset(); confirmarComSenha.mockImplementation(async ({ acao }) => { await acao('senha-sintetica64'); return true; }); });
afterEach(cleanup);
const renderizar = (contrato = { id: 9, version: 1, gestaoLocalId: 1, estadoNegocial: 'PUBLICADO' }) => <PainelExcecoesContrato contrato={contrato} eventos={[evento]} podeGerenciar aoAtualizarContrato={() => {}} />;
const abrir = async () => { const mounted = render(renderizar()); fireEvent.click(screen.getByRole('tab', { name: 'Transferências' })); await screen.findByText('Origem LAB → Destino LAB'); return mounted; };
const mudar = (nome, valor) => fireEvent.change(screen.getByLabelText(nome), { target: { value: valor } });
const preencher = async () => {
 const mounted = await abrir();
 for (const [nome,valor] of [[/Data efetiva da transferência/,'2026-11-05'],[/Data do ato de transferência/,'2026-11-06'],[/Gestão destino.*ID/,'23'],[/Tipo de contratação destino.*ID/,'71'],[/^SEI da transferência/,'SEI-FICTICIO-64'],[/^Autorização administrativa/,'ATO-SINTETICO-ATUAL'],[/^Motivo da transferência/,'Conferência do ato sintético atual']]) mudar(nome,valor);
 fireEvent.click(screen.getByRole('button', { name: 'Adicionar mapa' }));
 for (const [nome,valor] of [['Composição origem do mapa 1','31'],['Lotação origem do mapa 1','32'],['Turno origem do mapa 1','33'],['Cargo origem do mapa 1','34'],['Lotação destino do mapa 1','42'],['Turno destino do mapa 1','43'],['Cargo destino do mapa 1','44']]) mudar(nome,valor);
 return mounted;
};
const declarar = () => fireEvent.click(screen.getByRole('checkbox', { name: nomeConferencia }));
const conferirPrevia = async () => { fireEvent.click(screen.getByRole('button', { name: 'Gerar prévia da transferência' })); await screen.findByText('Impactos sintéticos do ato atual'); fireEvent.click(screen.getByRole('checkbox', { name: /Confirmo os impactos e os mapas desta transferência/ })); };
const posts = path => requisitar.mock.calls.map(([p])=>p).filter(p=>p.method==='POST'&&p.url===path);
const preencherCancelamento = async () => {
 fireEvent.click(screen.getByRole('button', { name: 'Cancelar transferência futura', exact: true }));
 const form = screen.getByRole('form', { name: 'Cancelamento de transferência' });
 for (const [nome,valor] of [[/Data do ato de cancelamento/,'2026-11-06'],[/Motivo do cancelamento/,'Cancelamento sintético próprio'],[/SEI do cancelamento/,'SEI-FICTICIO-CANCELAR-64'],[/Autorização do cancelamento/,'ATO-SINTETICO-CANCELAR-64']]) fireEvent.change(within(form).getByLabelText(nome),{target:{value:valor}});
 return form;
};

describe('Conferência humana específica do ato64 — RED/GREEN com os mesmos bytes', () => {
 it('controle positivo: o formulário atual aceita os campos e mapas sintéticos existentes', async () => {
  await preencher();expect(screen.getByLabelText('Lotação destino do mapa 1')).toHaveValue(42);expect(screen.getByLabelText(/Tipo de contratação destino.*ID/)).toHaveValue(71);expect(screen.getByLabelText(/^Autorização administrativa/)).toHaveValue('ATO-SINTETICO-ATUAL');expect(confirmarComSenha).not.toHaveBeenCalled();
 });
 it('não transporta cancelamento sem declaração, mesmo com submit programático e demais campos válidos', async () => {
  await abrir();const form=await preencherCancelamento();await act(async()=>{fireEvent.submit(form);});expect(posts('/contratos/9/transferencias-gestao/17/cancelar')).toHaveLength(0);expect(confirmarComSenha).not.toHaveBeenCalled();
 });
 it('nova intenção começa desmarcada apesar de uma conferência registrada no ato anterior', async () => {
  await preencher();const check=screen.queryByRole('checkbox',{name:nomeConferencia});expect(check).toBeInTheDocument();expect(check).not.toBeChecked();expect(within(screen.getByRole('form',{name:'Transferência de gestão'})).getByText(DECLARACAO,{exact:true})).toBeInTheDocument();expect(screen.getByRole('button',{name:'Gerar prévia da transferência'})).toBeDisabled();await act(async()=>fireEvent.submit(screen.getByRole('form',{name:'Transferência de gestão'})));expect(posts('/contratos/9/transferir-gestao')).toHaveLength(0);
 });
 it('envia versão literal e true somente após clique humano, prévia e impacto confirmados', async () => {
  await preencher();declarar();await conferirPrevia();fireEvent.submit(screen.getByRole('form',{name:'Transferência de gestão'}));await waitFor(()=>expect(posts('/contratos/9/transferir-gestao')).toHaveLength(1));
  expect(posts('/contratos/9/transferencias-gestao/previa')[0].data).toEqual({dataEfetiva:'2026-11-05',dataAto:'2026-11-06',gestaoLocalDestinoId:23,tipoContratacaoDestinoId:71,motivo:'Conferência do ato sintético atual',referenciaSei:'SEI-FICTICIO-64',autorizacaoAdministrativa:'ATO-SINTETICO-ATUAL',mapeamentos:[{composicaoOrigemId:31,lotacaoOrigemId:32,jornadaOrigemId:33,cargoOrigemId:34,lotacaoDestinoId:42,jornadaDestinoId:43,cargoDestinoId:44}],eventoPrecedenteId:null,relacaoPrecedencia:null,ordemDocumental:null,tokenPrevia:null,confirmarImpacto:false,conferenciaDocumentalConfirmada:true,versaoConferenciaDocumental:VERSAO});
  const command=posts('/contratos/9/transferir-gestao')[0];expect(command.data).toEqual({...posts('/contratos/9/transferencias-gestao/previa')[0].data,tokenPrevia:'PREVIA-SINTETICA-64',confirmarImpacto:true});expect(command.retries).toBe(0);expect(command.senhaConfirmacao).toBe('senha-sintetica64');
 });
 it.each([[/Data efetiva da transferência/,'2026-11-04'],[/Data do ato de transferência/,'2026-11-04'],[/Gestão destino.*ID/,'24'],[/Tipo de contratação destino.*ID/,'72'],[/^SEI da transferência/,'SEI-FICTICIO-OUTRO'],[/^Autorização administrativa/,'ATO-SINTETICO-OUTRO'],[/^Motivo da transferência/,'Outro motivo sintético'],['Evento precedente da transferência (ID)','102'],['Relação de precedência da transferência','RETIFICA'],['Ordem documental da transferência','2'],['Cargo destino do mapa 1','45']])('editar %s limpa declaração e impacto e impede token antigo',async(nome,valor)=>{
  await preencher();declarar();await conferirPrevia();mudar(nome,valor);expect(screen.getByRole('checkbox',{name:nomeConferencia})).not.toBeChecked();expect(screen.queryByRole('checkbox',{name:/Confirmo os impactos e os mapas desta transferência/})).not.toBeInTheDocument();expect(screen.getByRole('button',{name:'Confirmar transferência'})).toBeDisabled();await act(async()=>fireEvent.submit(screen.getByRole('form',{name:'Transferência de gestão'})));expect(posts('/contratos/9/transferir-gestao')).toHaveLength(0);
 });
 it.each(['Adicionar mapa','Remover mapa 1'])('mudar o conjunto de mapas por %s exige nova conferência',async(nome)=>{
  await preencher();declarar();await conferirPrevia();fireEvent.click(screen.getByRole('button',{name:nome,exact:true}));expect(screen.getByRole('checkbox',{name:nomeConferencia})).not.toBeChecked();expect(screen.getByRole('button',{name:'Confirmar transferência'})).toBeDisabled();
 });
 it('desmarcar e remarcar não ressuscita a prévia ou a confirmação de impacto anterior',async()=>{
  await preencher();declarar();await conferirPrevia();declarar();expect(screen.getByRole('checkbox',{name:nomeConferencia})).not.toBeChecked();declarar();expect(screen.getByRole('checkbox',{name:nomeConferencia})).toBeChecked();expect(screen.queryByRole('checkbox',{name:/Confirmo os impactos e os mapas desta transferência/})).not.toBeInTheDocument();expect(screen.getByRole('button',{name:'Confirmar transferência'})).toBeDisabled();
 });
 it('voltar ao valor anterior depois de editar não herda a declaração antiga',async()=>{
  await preencher();declarar();await conferirPrevia();mudar('Cargo destino do mapa 1','45');mudar('Cargo destino do mapa 1','44');expect(screen.getByRole('checkbox',{name:nomeConferencia})).not.toBeChecked();expect(screen.getByRole('button',{name:'Confirmar transferência'})).toBeDisabled();expect(posts('/contratos/9/transferir-gestao')).toHaveLength(0);
 });
 it('retificação conserva alvo e mapa, mas pede referência e declaração próprias',async()=>{
  await preencher();declarar();await conferirPrevia();fireEvent.click(screen.getByRole('button',{name:'Retificar transferência',exact:true}));expect(screen.getByLabelText('Evento precedente da transferência (ID)')).toHaveValue(101);expect(screen.getByLabelText('Lotação origem do mapa 1')).toHaveValue(32);expect(screen.getByLabelText(/^Autorização administrativa/)).toHaveValue('');expect(screen.getByRole('checkbox',{name:nomeConferencia})).not.toBeChecked();expect(screen.queryByRole('checkbox',{name:/Confirmo os impactos e os mapas desta transferência/})).not.toBeInTheDocument();
 });
 it('retificação envia nova declaração e referência do ato com o evento anterior exato',async()=>{
  await abrir();fireEvent.click(screen.getByRole('button',{name:'Retificar transferência',exact:true}));for(const[nome,valor]of[[/Data do ato de transferência/,'2026-11-06'],[/^SEI da transferência/,'SEI-FICTICIO-RETIFICAR-64'],[/^Autorização administrativa/,'ATO-SINTETICO-RETIFICAR-64'],[/^Motivo da transferência/,'Retificação sintética específica']])mudar(nome,valor);expect(screen.getByRole('checkbox',{name:nomeConferencia})).not.toBeChecked();declarar();await conferirPrevia();fireEvent.submit(screen.getByRole('form',{name:'Transferência de gestão'}));await waitFor(()=>expect(posts('/contratos/9/transferir-gestao')).toHaveLength(1));expect(posts('/contratos/9/transferir-gestao')[0].data).toEqual({dataEfetiva:'2099-11-05',dataAto:'2026-11-06',gestaoLocalDestinoId:23,tipoContratacaoDestinoId:71,motivo:'Retificação sintética específica',referenciaSei:'SEI-FICTICIO-RETIFICAR-64',autorizacaoAdministrativa:'ATO-SINTETICO-RETIFICAR-64',mapeamentos:[{composicaoOrigemId:31,lotacaoOrigemId:32,jornadaOrigemId:33,cargoOrigemId:34,lotacaoDestinoId:42,jornadaDestinoId:43,cargoDestinoId:44}],eventoPrecedenteId:101,relacaoPrecedencia:'RETIFICA',ordemDocumental:null,tokenPrevia:'PREVIA-SINTETICA-64',confirmarImpacto:true,conferenciaDocumentalConfirmada:true,versaoConferenciaDocumental:VERSAO});
 });
 it('cancelamento não herda declaração e usa endpoint/data/alvo originais com referência própria',async()=>{
  await preencher();declarar();await conferirPrevia();const form=await preencherCancelamento();expect(within(form).getByRole('checkbox',{name:nomeConferencia})).not.toBeChecked();await act(async()=>fireEvent.submit(form));expect(posts('/contratos/9/transferencias-gestao/17/cancelar')).toHaveLength(0);declarar();await act(async()=>fireEvent.submit(form));expect(posts('/contratos/9/transferencias-gestao/17/cancelar')).toHaveLength(1);expect(posts('/contratos/9/transferencias-gestao/17/cancelar')[0].data).toEqual({dataEfetiva:'2099-11-05',dataAto:'2026-11-06',motivo:'Cancelamento sintético próprio',referenciaSei:'SEI-FICTICIO-CANCELAR-64',autorizacaoAdministrativa:'ATO-SINTETICO-CANCELAR-64',conferenciaDocumentalConfirmada:true,versaoConferenciaDocumental:VERSAO});expect(posts('/contratos/9/transferencias-gestao/17/previa')).toHaveLength(0);expect(requisitar.mock.calls.some(([p])=>p.method==='DELETE')).toBe(false);
 });
 it.each([[/Data do ato de cancelamento/,'2026-11-07'],[/Motivo do cancelamento/,'Outro motivo próprio'],[/SEI do cancelamento/,'SEI-FICTICIO-OUTRO-CANCEL'],[/Autorização do cancelamento/,'ATO-SINTETICO-OUTRO-CANCEL']])('editar %s no cancelamento limpa a declaração e bloqueia submit',async(nome,valor)=>{
  await abrir();const form=await preencherCancelamento();declarar();fireEvent.change(within(form).getByLabelText(nome),{target:{value:valor}});expect(within(form).getByRole('checkbox',{name:nomeConferencia})).not.toBeChecked();await act(async()=>fireEvent.submit(form));expect(posts('/contratos/9/transferencias-gestao/17/cancelar')).toHaveLength(0);
 });
 it.each(['contrato','ator'])('mudança de %s invalida conferência mesmo com dados do formulário iguais',async(tipo)=>{
  const mounted=await preencher();declarar();await conferirPrevia();if(tipo==='ator')auth.user={id:8,perfil:'GESTOR_LOCAL',gestaoLocalId:1};mounted.rerender(renderizar({id:9,version:tipo==='contrato'?2:1,gestaoLocalId:1,estadoNegocial:'PUBLICADO'}));await waitFor(()=>expect(screen.getByRole('checkbox',{name:nomeConferencia})).not.toBeChecked());expect(screen.getByRole('button',{name:'Confirmar transferência'})).toBeDisabled();
 });
 it('resposta antiga de prévia não revive a declaração depois de editar mapa',async()=>{
  let resolve;requisitar.mockImplementation(p=>p.url.endsWith('/transferencias-gestao/previa')?new Promise(r=>{resolve=r;}):responder(p));await preencher();declarar();fireEvent.click(screen.getByRole('button',{name:'Gerar prévia da transferência'}));await waitFor(()=>expect(posts('/contratos/9/transferencias-gestao/previa')).toHaveLength(1));mudar('Cargo destino do mapa 1','45');await act(async()=>resolve(previa));expect(screen.getByRole('checkbox',{name:nomeConferencia})).not.toBeChecked();expect(screen.queryByText('Impactos sintéticos do ato atual')).not.toBeInTheDocument();expect(screen.getByRole('button',{name:'Confirmar transferência'})).toBeDisabled();
 });
 it.each([undefined,false,true])('normalização preserva ação humana %s sem inferir true de outros campos',flag=>{
  const raw={dataEfetiva:'2026-11-05',conferenciaDocumentalConfirmada:flag,versaoConferenciaDocumental:VERSAO};const result=prepararTransferenciaGestao(raw);expect(result.conferenciaDocumentalConfirmada).toBe(flag===true);expect(result.versaoConferenciaDocumental).toBe(VERSAO);
 });
});
