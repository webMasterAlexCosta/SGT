// Transporte ausente: estes testes exercitam os renderizadores React reais com DTOs sintéticos.
// Não demonstram persistência, autenticidade documental ou evolução do contrato Java.
import React from 'react';
import { afterEach, describe, expect, it } from 'vitest';
import { cleanup, fireEvent, render, screen, within } from '@testing-library/react';
import '@testing-library/jest-dom/vitest';
import {
  PublicacaoOriginal, ResultadoCatalogo, ResultadoContrato, ResultadoNome,
} from '../src/modules/historico/ResultadosHistoricos.tsx';

afterEach(cleanup);

const fotografia = () => ({
  id: null, numeroVersao: 2, eventoOrigemId: 41,
  dataEfetiva: '2026-01-01', registradoEm: '2026-02-01T12:00:00Z',
  numeroContrato: 'CONTRATO-SINTETICO', anoContrato: 2026, descricao: 'Descrição fotografada',
  tipoContratacaoId: 8, tipoContratacaoDescricao: 'Tipo fotografado', fornecedorId: null,
  cnpjEmpresa: null, razaoSocialEmpresa: null, nomeFantasiaEmpresa: null,
  dataInicio: '2026-01-01', dataFim: '2026-12-31', efetivoPrevisto: 0,
  gestaoLocalId: 4, gestaoLocalNome: 'Gestão fotografada', matriculaVinculoObrigatoria: false,
  matriculaTipoContratacaoObrigatoria: false, matriculaVinculoObrigatoriaEfetiva: false,
  diaSemanaReduzido: null, horasDiaReduzido: null, habilitacaoAdministrativa: null,
  tipoContratacaoComprovado: true,
});
const posicao = (versaoContratual) => ({
  contratoId: 9, dataReferencia: '2026-01-01', conhecidoAte: '2026-02-02T00:00:00Z',
  estadoNegocial: 'PUBLICADO', situacaoTemporal: 'VIGENTE', habilitacaoAdministrativa: null,
  vigenciaInicio: '2026-01-01', vigenciaFim: '2026-12-31', versaoContratual,
  eventosAplicados: [], relacoesConsideradas: [],
});
const valor = (raiz, rotulo) => within(raiz).getByText(rotulo, { selector: 'dt' }).nextElementSibling;
const detalhePosicao = () => screen.getByText('Fotografia contratual aplicada ao recorte').closest('details');

describe('IND-08: alcance informacional da fotografia contratual', () => {
  it('HIS-001: informa que a leitura não distingue ausência documentada de lacuna por campo', () => {
    render(<ResultadoContrato dados={posicao(fotografia())} />);
    const aviso = within(detalhePosicao()).getByRole('note', { name: /limites da comprovação histórica/i });
    expect(aviso).toHaveTextContent(/não permite distinguir/i);
    expect(aviso).toHaveTextContent(/ausência documentada/i);
    expect(aviso).toHaveTextContent(/lacuna/i);
    expect(valor(detalhePosicao(), 'Dia de jornada reduzida')).not.toHaveTextContent(/ausência (comprovada|documentada)/i);
  });

  it('CAT-004/HIS-001: distingue campo não retornado de null sem apagar false ou zero', () => {
    const foto = fotografia();
    delete foto.horasDiaReduzido;
    render(<ResultadoContrato dados={posicao(foto)} />);
    const detalhe = detalhePosicao();
    expect(valor(detalhe, 'Horas no dia reduzido')).toHaveTextContent(/campo não retornado/i);
    expect(valor(detalhe, 'Dia de jornada reduzida')).toHaveTextContent(/valor não informado/i);
    expect(valor(detalhe, 'Matrícula exigida pelo contrato')).toHaveTextContent(/^Não$/);
    expect(valor(detalhe, 'Efetivo original previsto')).toHaveTextContent(/^0$/);
    expect(valor(detalhe, 'Tipo de contratação comprovado')).toHaveTextContent(/^Sim$/);
  });

  it('HIS-001: preserva uma projeção válida com id null e a proveniência efetivamente retornada', () => {
    render(<ResultadoContrato dados={posicao(fotografia())} />);
    const detalhe = detalhePosicao();
    expect(valor(detalhe, 'Descrição')).toHaveTextContent('Descrição fotografada');
    expect(valor(detalhe, 'Evento de origem')).toHaveTextContent(/^41$/);
    expect(valor(detalhe, 'Registro do conhecimento')).toHaveTextContent('01/02/2026');
    expect(valor(detalhe, 'Identificador')).not.toHaveTextContent(/^41$/);
    expect(screen.queryByText('Fotografia contratual desconhecida neste recorte.')).not.toBeInTheDocument();
  });

  it('HIS-006: mantém o limite por campo nos detalhes ligados à publicação original', async () => {
    render(<PublicacaoOriginal
      eventos={[{ id: 41, tipoEvento: 'PUBLICACAO', dataEfetiva: '2026-01-01', registradoEm: '2026-01-02T00:00:00Z' }]}
      versoes={[{ ...fotografia(), id: 31 }]}
      provas={[{ id: 81, contratoId: 9, eventoOrigemId: 41, provaAnteriorId: null, numeroRevisao: 1,
        versaoAnteriorId: 30, versaoGeradaId: 31, dataEfetiva: '2026-01-01', registradoEm: '2026-02-01T12:00:00Z',
        hashConteudo: 'hash-sintetico', configuracaoCompletaComprovada: false }]}
    />);
    const tabela = screen.getByRole('table', { name: 'Fotografias vinculadas à publicação' });
    fireEvent.click(within(tabela).getByText('Ver registro 31'));
    const aviso = await within(tabela).findByRole('note', { name: /limites da comprovação histórica/i });
    expect(aviso).toHaveTextContent(/não permite distinguir/i);
    expect(valor(tabela, 'Origem da fotografia')).toHaveTextContent('Incorporada pela prova 81');
    expect(valor(tabela, 'Dia de jornada reduzida')).not.toHaveTextContent(/ausência (comprovada|documentada)/i);
  });

  it('HIS-006: um aditamento não passa a comprovar a publicação original', () => {
    render(<PublicacaoOriginal eventos={[{ id: 41, tipoEvento: 'ADITAMENTO' }]} versoes={[fotografia()]} provas={[]} />);
    expect(screen.getByText(/publicação original não comprovada/i)).toBeInTheDocument();
    expect(screen.queryByRole('table', { name: 'Fotografias vinculadas à publicação' })).not.toBeInTheDocument();
  });

  it.each([
    [false, true, 'Inativo na data consultada'],
    [true, true, 'Ativo na data consultada'],
    [null, false, 'Disponibilidade desconhecida na data consultada'],
    [false, false, 'Disponibilidade desconhecida na data consultada'],
    ['false', true, 'Disponibilidade desconhecida na data consultada'],
  ])('CAT-007: disponibilidade %s e comprovação %s conservam o significado do DTO', (ativo, disponibilidadeComprovada, titulo) => {
    render(<ResultadoCatalogo dados={{ tipoCatalogo: 'CARGO', catalogoId: 8,
      dataReferencia: '2026-01-01', conhecidoAte: '2026-02-01T00:00:00Z', ativo, disponibilidadeComprovada,
      eventoFonteId: disponibilidadeComprovada ? 12 : null, dataEfetivaFonte: disponibilidadeComprovada ? '2026-01-01' : null,
      registradoEmFonte: disponibilidadeComprovada ? '2026-01-02T00:00:00Z' : null, ativoAnterior: null }} />);
    expect(screen.getByRole('heading', { name: titulo })).toBeInTheDocument();
  });

  it('COL-010: fonte nominal ausente conserva o desconhecimento sem afirmar um nome', () => {
    render(<ResultadoNome dados={{ nome: null, comprovado: false, tipoFonte: 'AUSENTE',
      fonteId: null, fonteRegistradaEm: null, inicioComprovado: null, fimComprovado: null }} />);
    expect(screen.getByRole('heading', { name: 'Nome desconhecido no período consultado' })).toBeInTheDocument();
    expect(screen.queryByText('Fonte comprovada')).not.toBeInTheDocument();
  });

  it('COL-010: nome comprovado conserva fonte, registro e intervalo documental', () => {
    const { container } = render(<ResultadoNome dados={{ nome: 'Nome documentado sintético', comprovado: true,
      tipoFonte: 'PROVA_NOME_HISTORICO', fonteId: 81, fonteRegistradaEm: '2026-02-01T12:00:00Z',
      inicioComprovado: '2026-01-01', fimComprovado: '2026-01-31' }} />);
    expect(screen.getByRole('heading', { name: 'Nome documentado sintético' })).toBeInTheDocument();
    expect(valor(container, 'Identificador da fonte')).toHaveTextContent(/^81$/);
    expect(valor(container, 'Início comprovado')).toHaveTextContent('01/01/2026');
    expect(valor(container, 'Fim comprovado')).toHaveTextContent('31/01/2026');
    expect(valor(container, 'Registro da fonte')).toHaveTextContent('01/02/2026');
  });
});
