import React from 'react';
import { afterEach, expect, it } from 'vitest';
import { cleanup, render, screen } from '@testing-library/react';
import { PainelFechamentoFrequencia } from '../src/modules/rh/pages/PaginaFrequencia/components/PainelFechamentoFrequencia';

afterEach(cleanup);
const props = { ativo: false, carregando: false, erro: '', podeGerenciar: false, dataValida: true, filtrosProntos: true, possuiRascunhos: false, aoCriar() {}, aoCancelar() {}, aoReprocessar() {}, aoAtualizar() {} };
it('ignora causa técnica legada e mostra referência do processamento com erro', () => {
  const fechamento = { id: 173, status: 'ERRO', mensagem: '', detalheErro: 'org.postgresql.Driver: internal SQL trace', totalProcessado: 0, totalPrevisto: 1 };
  render(<PainelFechamentoFrequencia {...props} fechamento={fechamento} />);
  expect(screen.queryByText(/org.postgresql|internal SQL trace/)).toBeNull();
  expect(screen.getByText(/#173/)).toBeTruthy();
  expect(screen.getByText(/Não foi possível concluir/)).toBeTruthy();
});
it('exibe a mensagem pública retornada pelo Java', () => {
  render(<PainelFechamentoFrequencia {...props} fechamento={{ id: 174, status: 'ERRO', mensagem: 'Não foi possível processar. Consulte o suporte com a referência.' }} />);
  expect(screen.getByText('Não foi possível processar. Consulte o suporte com a referência.')).toBeTruthy();
});
