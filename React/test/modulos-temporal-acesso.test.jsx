import React from 'react';
import { afterEach, describe, expect, it, vi } from 'vitest';
import { cleanup, render, screen, within } from '@testing-library/react';
import '@testing-library/jest-dom/vitest';
import { ModalVinculo } from '../src/modules/rh/pages/PaginaPessoasVinculos/components/ModalVinculo.jsx';
import { podeVisualizarPessoasVinculos, podeGerenciarPessoasVinculos } from '../src/core/security/roles.js';

afterEach(cleanup);

const exibir = (posicao) => {
  render(<ModalVinculo aberto vinculo={{ id: 1, pessoaNome: 'Nome cadastral', estadoNegocial: 'REGISTRADO' }}
    posicao={{ dataReferencia: '2025-02-01', conhecidoAte: '2025-02-02T12:00:00Z', vinculos: [], ...posicao }}
    podeGerenciar={false} aoFechar={vi.fn()} aoConsultarTemporal={vi.fn()} aoAbrirAcao={vi.fn()} aoAbrirAlocacoes={vi.fn()} />);
  return within(screen.getByRole('heading', { name: 'Posição temporal da pessoa' }).closest('section'));
};

describe('Nome apresentado na consulta temporal da Pessoa', () => {
  it('exibe a fotografia comprovada e identifica separadamente o cadastro atual', () => {
    const consulta = exibir({ nomeAtual: 'Nome atualizado', nomeFotografia: 'Nome do ato', fotografiaIdentidadeComprovada: true });
    expect(consulta.queryByText('Nome do ato')).toBeInTheDocument();
    expect(consulta.queryByText(/Nome atual.*Nome atualizado/)).toBeInTheDocument();
  });

  it('prioriza a identidade histórica resolvida e comprovada no corte consultado', () => {
    const consulta = exibir({ nomeAtual: 'Nome atual', nomeFotografia: 'Foto anterior', fotografiaIdentidadeComprovada: true,
      identidadeHistorica: { nome: 'Nome da prova histórica', comprovado: true, tipoFonte: 'PROVA', fonteId: 33 } });
    expect(consulta.queryByText('Nome da prova histórica')).toBeInTheDocument();
  });

  it('mantém desconhecimento quando o servidor não comprova a identidade, sem usar o nome atual', () => {
    const consulta = exibir({ nomeAtual: 'Nome presente', nomeFotografia: 'Texto sem comprovação', fotografiaIdentidadeComprovada: false,
      identidadeHistorica: { nome: null, comprovado: false, tipoFonte: 'AUSENTE' } });
    expect(consulta.queryByText(/Nome histórico não comprovado/)).toBeInTheDocument();
    expect(consulta.queryByText('Texto sem comprovação')).not.toBeInTheDocument();
    expect(consulta.queryByText(/Nome atual.*Nome presente/)).toBeInTheDocument();
  });

  it('não promove nome retornado sem flag de comprovação a nome histórico', () => {
    const consulta = exibir({ nomeAtual: 'Presente', identidadeHistorica: { nome: 'Não comprovado', comprovado: false } });
    expect(consulta.queryByText(/Nome histórico não comprovado/)).toBeInTheDocument();
    expect(consulta.queryByText('Não comprovado')).not.toBeInTheDocument();
  });
});

describe('Consulta canônica de Pessoas/Vínculos por perfil', () => {
  it.each(['PRESIDENTE', 'PERFIL_PRESIDENTE'])('permite leitura ao %s sem conceder escrita', (perfil) => {
    expect(podeVisualizarPessoasVinculos(perfil)).toBe(true);
    expect(podeGerenciarPessoasVinculos(perfil)).toBe(false);
  });
  it('continua negando perfil desconhecido e reserva escrita ao Gestor Local', () => {
    expect(podeVisualizarPessoasVinculos('DESCONHECIDO')).toBe(false);
    expect(podeGerenciarPessoasVinculos('GESTOR_LOCAL')).toBe(true);
    expect(podeGerenciarPessoasVinculos('ADMINISTRADOR')).toBe(false);
  });
});
