// @vitest-environment jsdom
import React from 'react';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { act, cleanup, fireEvent, render, screen, waitFor, within } from '@testing-library/react';
import '@testing-library/jest-dom/vitest';
const { requisitar, autenticacao } = vi.hoisted(() => ({
  requisitar: vi.fn(), autenticacao: { user: { perfil: 'GESTOR_LOCAL', autoridade: 'PERFIL_GESTOR_LOCAL' } },
}));
vi.mock('../src/core/http.ts', () => ({ requisitar, extrairLista: (v) => Array.isArray(v) ? v : v?.content || [] }));
vi.mock('../src/modules/auth/context.tsx', () => ({ useAutenticacao: () => autenticacao }));
import { FormularioProvaHistorica } from '../src/modules/historico/FormularioProvaHistorica.tsx';
import { ResultadoNome, TabelaHistorica, PublicacaoOriginal } from '../src/modules/historico/ResultadosHistoricos.tsx';

const previa = { tipoCatalogo: 'CARGO', catalogoId: 8, dataEfetiva: '2026-01-01', ativoAnterior: true,
  ativo: false, ativoAtualAposRegistro: false, versaoAtual: 0, tokenPrevia: 'token-java', impactos: ['Reavaliar dependências temporais.'] };
const preencher = () => {
  fireEvent.change(screen.getByLabelText('Data efetiva da transição'), { target: { value: '2026-01-01' } });
  fireEvent.change(screen.getByLabelText('Disponibilidade documentada'), { target: { value: 'false' } });
  fireEvent.change(screen.getByLabelText('Versão esperada do catálogo'), { target: { value: '0' } });
  fireEvent.change(screen.getByLabelText('Motivo / justificativa'), { target: { value: 'Ato documental sintético' } });
  fireEvent.change(screen.getByLabelText('Evidência da transição'), { target: { value: 'SEI-SINTETICO' } });
  fireEvent.change(screen.getByLabelText('Autorização'), { target: { value: 'AUT-SINTETICA' } });
};
const montar = () => { const view = render(<FormularioProvaHistorica tipo="catalogo" alvo={{ tipoCatalogo: 'CARGO', id: 8 }} />); preencher(); return view; };
const aprovar = async () => {
  fireEvent.click(screen.getByRole('button', { name: 'Gerar prévia' }));
  await screen.findByText('Reavaliar dependências temporais.');
  fireEvent.click(screen.getByRole('checkbox', { name: /Li os impactos e confirmo esta intenção/i }));
};

beforeEach(() => {
  requisitar.mockReset(); requisitar.mockResolvedValue(previa);
  autenticacao.user = { perfil: 'GESTOR_LOCAL', autoridade: 'PERFIL_GESTOR_LOCAL' };
});
afterEach(() => cleanup());

describe('Confirmação histórica fiel à intenção analisada', () => {
  it('carrega a versão de concorrência atual e a aplica à intenção sem importar rótulos atuais', async () => {
    requisitar.mockImplementation(({ method }) => Promise.resolve(method === 'GET' ? { version: 5, nome: 'Nome atual que não comprova fonte' } : previa));
    montar();
    const carregar = screen.queryByRole('button', { name: 'Carregar versões atuais' });
    expect(carregar).toBeInTheDocument();
    fireEvent.click(carregar);
    await waitFor(() => expect(screen.getByLabelText('Versão esperada do catálogo')).toHaveValue(5));
    expect(screen.queryByText('Nome atual que não comprova fonte')).not.toBeInTheDocument();
    await aprovar();
    expect(requisitar.mock.calls.find(([cfg]) => cfg.url.endsWith('/previa'))[0].data.versaoEsperada).toBe(5);
  });

  it('aguarda versões atuais antes de permitir uma nova prévia',async()=>{
    let resolver;
    requisitar.mockImplementation(({method})=>method==='GET'?new Promise(resolve=>{resolver=resolve;}):Promise.resolve(previa));
    montar();fireEvent.click(screen.getByRole('button',{name:'Carregar versões atuais'}));
    expect(screen.getByRole('button',{name:'Gerar prévia'})).toBeDisabled();
    await act(async()=>resolver({version:5}));
    await aprovar();
    expect(requisitar.mock.calls.find(([cfg])=>cfg.url.endsWith('/previa'))[0].data.versaoEsperada).toBe(5);
  });

  it('impede submissão direta antes da prévia Java e aceite explícito', () => {
    montar();
    fireEvent.submit(screen.getByRole('form', { name: 'Fonte histórica' }));
    expect(requisitar).not.toHaveBeenCalled();
    expect(screen.getByRole('button', { name: 'Registrar transição' })).toBeDisabled();
  });

  it('descarta prévia antiga que responde após edição e cancela o sinal', async () => {
    let resolver;
    requisitar.mockImplementation(() => new Promise((resolve) => { resolver = resolve; }));
    montar();
    fireEvent.click(screen.getByRole('button', { name: 'Gerar prévia' }));
    const sinal = requisitar.mock.calls[0][0].signal;
    fireEvent.change(screen.getByLabelText('Motivo / justificativa'), { target: { value: 'Ato posterior' } });
    await act(async () => resolver(previa));
    expect(sinal.aborted).toBe(true);
    expect(screen.queryByText('Reavaliar dependências temporais.')).not.toBeInTheDocument();
    expect(screen.getByRole('button', { name: 'Registrar transição' })).toBeDisabled();
    fireEvent.submit(screen.getByRole('form', { name: 'Fonte histórica' }));
    expect(requisitar).toHaveBeenCalledTimes(1);
  });

  it('invalida confirmação quando o alvo muda sem reaproveitar o token', async () => {
    const view = montar(); await aprovar();
    view.rerender(<FormularioProvaHistorica tipo="catalogo" alvo={{ tipoCatalogo: 'CARGO', id: 9 }} />);
    fireEvent.submit(screen.getByRole('form', { name: 'Fonte histórica' }));
    expect(requisitar).toHaveBeenCalledTimes(1);
    expect(screen.queryByRole('checkbox', { name: /Li os impactos/i })).not.toBeInTheDocument();
  });

  it('envia a fotografia exata da prévia e bloqueia duplo submit enquanto a mutação está pendente', async () => {
    let resolverFinal;
    requisitar.mockImplementation(({ url }) => url.endsWith('/previa') ? Promise.resolve(previa) : new Promise((resolve) => { resolverFinal = resolve; }));
    montar(); await aprovar();
    const intencao = requisitar.mock.calls[0][0].data;
    const form = screen.getByRole('form', { name: 'Fonte histórica' });
    act(() => { fireEvent.submit(form); fireEvent.submit(form); });
    expect(requisitar).toHaveBeenCalledTimes(2);
    expect(requisitar.mock.calls[1][0].data).toEqual({ ...intencao, tokenPrevia: 'token-java', confirmarImpacto: true });
    expect(screen.queryByText(/Registrado com sucesso/i)).not.toBeInTheDocument();
    await act(async () => resolverFinal({ eventoId: 81, tipoCatalogo: 'CARGO', catalogoId: 8, dataEfetiva: '2026-01-01', ativo: false, registradoEm: '2026-02-01T12:00:00Z', ativoAtualAposRegistro: false, versaoAtual: 1, reenvio: false }));
    expect(screen.getByText(/Registrado com sucesso/i)).toBeInTheDocument();
  });

  it.each([409, 403])('resposta %s preserva campos e exige nova análise sem declarar sucesso', async (status) => {
    requisitar.mockImplementation(({ url }) => url.endsWith('/previa') ? Promise.resolve(previa) : Promise.reject({ status, message: 'Dependência ou autorização mudou.' }));
    montar(); await aprovar();
    fireEvent.submit(screen.getByRole('form', { name: 'Fonte histórica' }));
    expect(await screen.findByRole('alert')).toHaveTextContent('Dependência ou autorização mudou.');
    expect(screen.getByLabelText('Motivo / justificativa')).toHaveValue('Ato documental sintético');
    expect(screen.getByLabelText('Versão esperada do catálogo')).toHaveValue(0);
    expect(screen.queryByRole('checkbox', { name: /Li os impactos/i })).not.toBeInTheDocument();
    expect(screen.getByRole('button', { name: 'Registrar transição' })).toBeDisabled();
    expect(screen.queryByText(/Registrado com sucesso/i)).not.toBeInTheDocument();
  });

  it('resultado de rede incerto mantém a chave e o payload para repetição manual idempotente', async () => {
    let tentativas = 0;
    requisitar.mockImplementation(({ url }) => {
      if (url.endsWith('/previa')) return Promise.resolve(previa);
      tentativas += 1;
      return tentativas === 1 ? Promise.reject({ status: 0, resultadoIncerto: true, message: 'Resposta não recebida.' }) : Promise.resolve({ eventoId: 81, reenvio: true, versaoAtual: 1 });
    });
    montar(); await aprovar();
    fireEvent.submit(screen.getByRole('form', { name: 'Fonte histórica' }));
    const repetir = await screen.findByRole('button', { name: /Repetir a mesma intenção/i });
    expect(screen.getByRole('alert')).toHaveTextContent(/incerto/i);
    fireEvent.click(repetir);
    await screen.findByText(/Registrado com sucesso/i);
    expect(requisitar.mock.calls[2][0].data).toEqual(requisitar.mock.calls[1][0].data);
  });

  it('mantém leitura para perfil não gestor sem mostrar comandos de escrita', () => {
    autenticacao.user = { perfil: 'ADMINISTRADOR', autoridade: 'PERFIL_ADMINISTRADOR' };
    render(<FormularioProvaHistorica tipo="catalogo" alvo={{ tipoCatalogo: 'CARGO', id: 8 }} />);
    expect(screen.queryByRole('button', { name: 'Gerar prévia' })).not.toBeInTheDocument();
    expect(screen.queryByRole('button', { name: 'Registrar transição' })).not.toBeInTheDocument();
  });
});

describe('Leitura histórica sem preencher lacunas pelo presente', () => {
  it('AUSENTE com comprovado=false significa desconhecido, mesmo se houver nome atual', () => {
    render(<ResultadoNome dados={{ nome: null, comprovado: false, tipoFonte: 'AUSENTE', fonteId: null, fonteRegistradaEm: null, inicioComprovado: null, fimComprovado: null, nomeAtual: 'Nome atual que não comprova passado' }} />);
    expect(screen.getByText(/Nome desconhecido no período consultado/i)).toBeInTheDocument();
    expect(screen.queryByText('Nome atual que não comprova passado')).not.toBeInTheDocument();
    expect(screen.queryByText(/ausência comprovada/i)).not.toBeInTheDocument();
  });

  it('limita a cinco registros e permite acessar a sexta prova na página seguinte', () => {
    render(<TabelaHistorica titulo="Provas históricas" itens={Array.from({ length: 6 }, (_, i) => ({ id: i + 1, motivo: `Prova documental ${i + 1}` }))} colunas={[{ chave: 'id', titulo: 'ID' }, { chave: 'motivo', titulo: 'Motivo' }]} />);
    const tabela = screen.getByRole('table', { name: 'Provas históricas' });
    expect(within(tabela).getAllByRole('row')).toHaveLength(6);
    expect(screen.queryByText('Prova documental 6')).not.toBeInTheDocument();
    fireEvent.click(screen.getByRole('button', { name: /Próxima/i }));
    expect(screen.getByText('Prova documental 6')).toBeInTheDocument();
    expect(screen.queryByText('Prova documental 1')).not.toBeInTheDocument();
  });

  it('não inventa publicação com base numa versão sem evento de publicação', () => {
    render(<PublicacaoOriginal eventos={[{ id: 10, tipoEvento: 'ADITAMENTO' }]} versoes={[{ id: 11, eventoOrigemId: 10, descricao: 'Aditamento real' }]} provas={[]} />);
    expect(screen.getByText(/Publicação original não comprovada no acervo retornado/i)).toBeInTheDocument();
    expect(screen.queryByText('Aditamento real')).not.toBeInTheDocument();
  });
});
