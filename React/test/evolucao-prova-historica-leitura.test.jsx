// Renderizadores reais, DTOs sintéticos do contrato aditivo. Não certifica a fonte documental.
import React from 'react';
import { afterEach, describe, expect, it } from 'vitest';
import { cleanup, fireEvent, render, screen, within } from '@testing-library/react';
import '@testing-library/jest-dom/vitest';
import { DetalhesDados, PublicacaoOriginal, ResultadoContrato, TabelaHistorica } from '../src/modules/historico/ResultadosHistoricos.tsx';

afterEach(cleanup);
const origem = (campos = {}) => ({ tipo: 'PROVA_HISTORICA', provaId: 81, eventoId: 41, versaoId: 31,
  dataEfetiva: '2026-01-01', registradoEm: '2026-02-01T12:00:00Z', ...campos });
const prova = (estado, origens = [origem()]) => ({ estado, visibilidade: 'DISPONIVEL', origens });
const fotografia = (campos = {}) => ({ id: null, numeroVersao: 2, eventoOrigemId: 41,
  dataEfetiva: '2026-01-01', registradoEm: '2026-02-01T12:00:00Z', descricao: 'Descrição documentada sintética',
  efetivoPrevisto: 0, matriculaVinculoObrigatoria: false, diaSemanaReduzido: null,
  horasDiaReduzido: null, provaCampos: {
    descricao: prova('VALOR_COMPROVADO'), efetivoPrevisto: prova('VALOR_COMPROVADO'),
    matriculaVinculoObrigatoria: prova('VALOR_COMPROVADO'), diaSemanaReduzido: prova('AUSENCIA_COMPROVADA'),
    horasDiaReduzido: prova('NAO_COMPROVADO', []),
  }, ...campos });
const posicao = (foto, campos = {}) => ({ contratoId: 9, dataReferencia: '2026-01-15',
  conhecidoAte: '2026-02-02T00:00:00Z', estadoNegocial: 'PUBLICADO', situacaoTemporal: 'VIGENTE',
  habilitacaoAdministrativa: 'ATIVO', vigenciaInicio: '2026-01-01', vigenciaFim: '2026-12-31',
  versaoContratual: foto, eventosAplicados: [], relacoesConsideradas: [], ...campos });
const detalheFoto = () => screen.getByText('Fotografia contratual aplicada ao recorte').closest('details');
const campo = (raiz, rotulo) => within(raiz).getByText(rotulo, { selector: 'dt' }).nextElementSibling;

describe('IND-08: semântica recebida por campo', () => {
  it('distingue zero e falso comprovados de ausência comprovada e desconhecimento', () => {
    render(<ResultadoContrato dados={posicao(fotografia())} />);
    const raiz = detalheFoto();
    expect(campo(raiz, 'Efetivo original previsto')).toHaveTextContent(/valor comprovado/i);
    expect(campo(raiz, 'Efetivo original previsto')).toHaveTextContent(/\b0\b/);
    expect(campo(raiz, 'Matrícula exigida pelo contrato')).toHaveTextContent(/valor comprovado/i);
    expect(campo(raiz, 'Matrícula exigida pelo contrato')).toHaveTextContent(/Não/);
    expect(campo(raiz, 'Dia de jornada reduzida')).toHaveTextContent(/ausência comprovada/i);
    expect(campo(raiz, 'Horas no dia reduzido')).toHaveTextContent(/desconhecido|não comprovado/i);
    expect(within(raiz).queryByRole('note', { name: /limites da comprovação histórica/i })).not.toBeInTheDocument();
  });

  it('suprime o escalar legado e as origens quando a visibilidade é restrita', () => {
    const foto = fotografia({ descricao: 'CONTEUDO-RESTRITO-98765' });
    foto.provaCampos.descricao = { estado: null, visibilidade: 'RESTRITA', origens: [origem({ provaId: 98765 })] };
    render(<ResultadoContrato dados={posicao(foto)} />);
    expect(campo(detalheFoto(), 'Descrição')).toHaveTextContent(/acesso restrito|visibilidade restrita/i);
    expect(campo(detalheFoto(), 'Descrição')).not.toHaveTextContent(/desconhecido|ausência comprovada|valor comprovado/i);
    expect(screen.queryByText(/CONTEUDO-RESTRITO-98765/)).not.toBeInTheDocument();
    expect(document.body).not.toHaveTextContent('98765');
  });

  it('não apresenta um escalar residual como fato quando o servidor informa desconhecimento', () => {
    const foto = fotografia({ descricao: 'VALOR-RESIDUAL-SEM-PROVA' });
    foto.provaCampos.descricao = prova('NAO_COMPROVADO', []);
    render(<ResultadoContrato dados={posicao(foto)} />);
    expect(campo(detalheFoto(), 'Descrição')).toHaveTextContent(/desconhecido|não comprovado/i);
    expect(document.body).not.toHaveTextContent('VALOR-RESIDUAL-SEM-PROVA');
  });

  it('não promove null a ausência comprovada se a entrada de comprovação estiver ausente', () => {
    const foto = fotografia();
    delete foto.provaCampos.diaSemanaReduzido;
    render(<ResultadoContrato dados={posicao(foto)} />);
    expect(campo(detalheFoto(), 'Dia de jornada reduzida')).toHaveTextContent(/não informad|indisponível|desconhecido/i);
    expect(campo(detalheFoto(), 'Dia de jornada reduzida')).not.toHaveTextContent(/ausência comprovada/i);
  });

  it.each([
    { estado: 'ESTADO_NAO_SUPORTADO', visibilidade: 'DISPONIVEL', origens: [] },
    { estado: 'VALOR_COMPROVADO', visibilidade: 'VISIBILIDADE_NAO_SUPORTADA', origens: [] },
    null,
  ])('trata metadado inválido como indisponível sem retornar ao escalar', (metadado) => {
    const foto = fotografia({ descricao: 'ESCALAR-NAO-AUTORIZADO-PELO-METADADO' });
    foto.provaCampos.descricao = metadado;
    render(<ResultadoContrato dados={posicao(foto)} />);
    expect(campo(detalheFoto(), 'Descrição')).toHaveTextContent(/não informad|indisponível|desconhecido/i);
    expect(document.body).not.toHaveTextContent('ESCALAR-NAO-AUTORIZADO-PELO-METADADO');
  });

  it('preserva a leitura antiga e seu aviso quando o DTO não possui provaCampos', () => {
    const foto = fotografia();
    delete foto.provaCampos;
    render(<ResultadoContrato dados={posicao(foto)} />);
    expect(within(detalheFoto()).getByRole('note', { name: /limites da comprovação histórica/i })).toBeInTheDocument();
    expect(campo(detalheFoto(), 'Efetivo original previsto')).toHaveTextContent(/^0$/);
    expect(campo(detalheFoto(), 'Matrícula exigida pelo contrato')).toHaveTextContent(/^Não$/);
    expect(campo(detalheFoto(), 'Dia de jornada reduzida')).toHaveTextContent(/valor não informado/i);
  });

  it('mostra proveniência e cortes retornados sem inventar identidade física da projeção', () => {
    render(<ResultadoContrato dados={posicao(fotografia())} />);
    const dd = campo(detalheFoto(), 'Descrição');
    expect(dd).toHaveTextContent(/81/);
    expect(dd).toHaveTextContent(/41/);
    expect(dd).toHaveTextContent(/01\/01\/2026/);
    expect(dd).toHaveTextContent(/01\/02\/2026/);
    expect(document.body).toHaveTextContent('15/01/2026');
    expect(document.body).toHaveTextContent('02/02/2026');
    expect(campo(detalheFoto(), 'Identificador')).not.toHaveTextContent(/^41$/);
    expect(screen.queryByText('Fotografia contratual desconhecida neste recorte.')).not.toBeInTheDocument();
  });

  it('aplica visibilidade aos eixos da posição antes de exibir os escalares legados', () => {
    render(<ResultadoContrato dados={posicao(fotografia(), {
      estadoNegocial: 'ESTADO-RESTRITO-SINTETICO', provaCampos: {
        estadoNegocial: { estado: null, visibilidade: 'RESTRITA', origens: [] },
      },
    })} />);
    expect(document.body).not.toHaveTextContent('ESTADO-RESTRITO-SINTETICO');
    expect(campo(document.body, 'Estado negocial')).toHaveTextContent(/restrito/i);
  });

  it('preserva comprovação independente de município e grade nos fragmentos', () => {
    render(<DetalhesDados dados={{ composicoes: [{ composicaoId: 71, lotacaoMunicipio: null,
      jornadaSemanal: 'GRADE-RESIDUAL-NAO-COMPROVADA', provaCampos: {
        lotacaoMunicipio: prova('AUSENCIA_COMPROVADA', [origem({ composicaoId: 71 })]),
        jornadaSemanal: prova('NAO_COMPROVADO', []),
      },
    }] }} />);
    expect(campo(document.body, 'Município documentado')).toHaveTextContent(/ausência comprovada/i);
    expect(campo(document.body, 'Grade semanal documentada')).toHaveTextContent(/desconhecido|não comprovado/i);
    expect(document.body).not.toHaveTextContent('GRADE-RESIDUAL-NAO-COMPROVADA');
  });

  it('mantém metadados da revisão e a precisão do instante separados da prova factual da fotografia', () => {
    const registradoEm = '2026-02-01T12:00:00.123456Z';
    render(<DetalhesDados dados={{ id: 81, numeroRevisao: 2, versaoGeradaId: 31,
      dataEfetiva: '2026-01-01', registradoEm, referenciaSei: 'REFERENCIA-RESTRITA-SINTETICA',
      provaCampos: { referenciaSei: { estado: null, visibilidade: 'RESTRITA', origens: [] } },
    }} />);
    expect(campo(document.body, 'Data efetiva do ato documentado')).toHaveTextContent('01/01/2026');
    expect(screen.getByText(registradoEm)).toBeInTheDocument();
    expect(document.body).not.toHaveTextContent('REFERENCIA-RESTRITA-SINTETICA');
  });

  it('não expõe valor restrito nas células da tabela mesmo com detalhes fechados', () => {
    render(<TabelaHistorica titulo="Fragmentos consultados" itens={[{ id: 71, lotacaoNome: 'LOTACAO-RESTRITA-123',
      provaCampos: { lotacaoNome: { estado: null, visibilidade: 'RESTRITA', origens: [] } },
    }]} colunas={[{ chave: 'lotacaoNome', titulo: 'Lotação' }]} />);
    expect(screen.getByRole('table', { name: 'Fragmentos consultados' })).toHaveTextContent(/restrito/i);
    expect(document.body).not.toHaveTextContent('LOTACAO-RESTRITA-123');
  });

  it('aplica os estados também à fotografia vinculada à publicação preservada', async () => {
    render(<PublicacaoOriginal eventos={[{ id: 41, tipoEvento: 'PUBLICACAO', dataEfetiva: '2026-01-01', registradoEm: '2026-01-02T00:00:00Z' }]}
      versoes={[fotografia({ id: 31 })]} provas={[{ id: 81, versaoGeradaId: 31 }]} />);
    const tabela = screen.getByRole('table', { name: 'Fotografias vinculadas à publicação' });
    fireEvent.click(within(tabela).getByText('Ver registro 31'));
    await within(tabela).findByText('Dia de jornada reduzida', { selector: 'dt' });
    expect(campo(tabela, 'Dia de jornada reduzida')).toHaveTextContent(/ausência comprovada/i);
    expect(campo(tabela, 'Horas no dia reduzido')).toHaveTextContent(/desconhecido|não comprovado/i);
    expect(campo(tabela, 'Origem da fotografia')).toHaveTextContent('Incorporada pela prova 81');
  });
});
