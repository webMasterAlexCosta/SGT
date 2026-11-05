package com.example.siggtrat.contrato;

import com.example.siggtrat.entidade.*;
import com.example.siggtrat.repositorio.*;
import com.example.siggtrat.servico.*;
import com.example.siggtrat.utilitario.FotografiaJornada;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.SoftAssertions.assertSoftly;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Exercita o ramo null em entidades históricas sem enfraquecer as colunas NOT NULL.
 * Os testes HTTP correspondentes usam vazio/branco, representação persistível da ausência.
 */
class ComposicaoFotografiaLegadaTest {
    private static final LocalDate INICIO = LocalDate.of(2026, 1, 1);
    private static final LocalDate EFEITO = LocalDate.of(2026, 2, 1);
    private static final LocalDate CONSULTA = LocalDate.of(2026, 3, 1);

    @ParameterizedTest
    @ValueSource(strings = {"BASE", "ADITAMENTO", "TRANSFERENCIA", "ADITAMENTO_ORIGEM_AUSENTE"})
    void projecaoComFotografiaNulaNaoConsultaNomeAtualDoCatalogo(String fonte) {
        var composicoes = mock(ComposicaoContratualRepositorio.class);
        var aditamentos = mock(AditamentoContratualRepositorio.class);
        var remanejamentos = mock(RemanejamentoContratualRepositorio.class);
        var transferencias = mock(TransferenciaGestaoContratoRepositorio.class);
        var motor = mock(MotorTemporalContratoServico.class);
        var servico = new ComposicaoContratualTemporalServico(composicoes, aditamentos, remanejamentos, motor);
        servico.configurarTransferenciasGestao(transferencias);
        var contrato = contrato();
        var base = composicao(contrato);
        when(composicoes.findByContratoIdOrderByLotacaoNomeAscJornadaDescricaoAscCargoNomeAsc(5L)).thenReturn(List.of(base));
        when(aditamentos.findByContratoIdOrderByDataEfetivaAscRegistradoEmAsc(5L)).thenReturn(List.of());
        when(remanejamentos.findByContratoIdOrderByDataEfetivaAscRegistradoEmAsc(5L)).thenReturn(List.of());
        when(motor.eventosValidosOrdenados(eq(5L), any(), any())).thenReturn(List.of());
        long lotacaoId = 10, jornadaId = 20, cargoId = 30;
        int quantidade = 5;

        switch (fonte) {
            case "BASE" -> {
                base.setLotacaoNomeFotografia(null); base.setJornadaDescricaoFotografia(null); base.setCargoNomeFotografia(null);
            }
            case "ADITAMENTO", "ADITAMENTO_ORIGEM_AUSENTE" -> {
                if (fonte.equals("ADITAMENTO_ORIGEM_AUSENTE")) {
                    base.setLotacaoNomeFotografia(null);
                    base.setJornadaDescricaoFotografia(null);
                    base.setCargoNomeFotografia(null);
                }
                var evento = evento(contrato, TipoEventoCicloVidaContrato.ADITAMENTO);
                var a = new AditamentoContratual(); a.setContrato(contrato); a.setEvento(evento); a.setDataEfetiva(EFEITO);
                a.setNovoEfetivoTotal(6);
                var item = new AditamentoComposicao(); item.setComposicaoBase(base);
                item.setLotacao(base.getLotacao()); item.setJornada(base.getJornada()); item.setCargo(base.getCargo());
                item.setQuantidadeAnterior(5); item.setVariacao(1); item.setQuantidadeResultante(6);
                a.adicionarAlteracao(item);
                when(aditamentos.findByContratoIdOrderByDataEfetivaAscRegistradoEmAsc(5L)).thenReturn(List.of(a));
                when(motor.eventosValidosOrdenados(eq(5L), any(), any())).thenReturn(List.of(evento));
                quantidade = 6;
            }
            case "TRANSFERENCIA" -> {
                var evento = evento(contrato, TipoEventoCicloVidaContrato.TRANSFERENCIA_GESTAO_LOCAL);
                var transferencia = new TransferenciaGestaoContrato(); transferencia.setContrato(contrato);
                transferencia.setEvento(evento); transferencia.setDataEfetiva(EFEITO);
                var mapa = mapa(transferencia, base); transferencia.setMapeamentos(List.of(mapa));
                when(transferencias.findByEventoId(7L)).thenReturn(Optional.of(transferencia));
                when(motor.eventosValidosOrdenados(eq(5L), any(), any())).thenReturn(List.of(evento));
                lotacaoId = 110; jornadaId = 120; cargoId = 130;
            }
            default -> throw new AssertionError(fonte);
        }

        var resultado = servico.projetarDTO(contrato, CONSULTA, Instant.now());
        assertThat(resultado).hasSize(1);
        var item = resultado.get(0);
        assertThat(item.id()).isEqualTo(50L);
        assertThat(item.lotacaoId()).isEqualTo(lotacaoId);
        assertThat(item.jornadaId()).isEqualTo(jornadaId);
        assertThat(item.cargoId()).isEqualTo(cargoId);
        assertThat(item.efetivoPrevisto()).isEqualTo(quantidade);
        assertThat(item.dataInicioVigencia()).isEqualTo(INICIO);
        assertThat(item.dataFimVigencia()).isEqualTo(LocalDate.of(2026, 12, 31));
        // O aditamento de combinação existente altera somente a quantidade.
        // Fotos nulas no item não apagam a origem comprovada nem a completam
        // com os nomes atuais dos mestres; a variante ausente cobre o null real.
        if (fonte.equals("ADITAMENTO")) {
            assertSoftly(s -> {
                s.assertThat(item.lotacaoNome()).isEqualTo("Lotação original");
                s.assertThat(item.jornadaDescricao()).isEqualTo("Jornada original");
                s.assertThat(item.cargoNome()).isEqualTo("Cargo original");
            });
        } else {
            assertSoftly(s -> {
                s.assertThat(item.lotacaoNome()).as(fonte + " lotação sem prova").isNull();
                s.assertThat(item.jornadaDescricao()).as(fonte + " jornada sem prova").isNull();
                s.assertThat(item.cargoNome()).as(fonte + " cargo sem prova").isNull();
            });
        }
    }

    @Test
    void remanejamentoComFotografiaNulaPreservaQuantidadesSemInventarRotulos() {
        var contratos = mock(ContratoRepositorio.class);
        var remanejamentos = mock(RemanejamentoContratualRepositorio.class);
        var acesso = mock(AcessoOrganizacionalServico.class);
        var servico = new RemanejamentoContratualServico(contratos, remanejamentos,
                mock(AditamentoContratualRepositorio.class), mock(ComposicaoContratualRepositorio.class),
                acesso, mock(MotorTemporalContratoServico.class), mock(ComposicaoContratualTemporalServico.class));
        var contrato = contrato(); var base = composicao(contrato);
        var rem = new RemanejamentoContratual(); rem.setContrato(contrato);
        rem.setEvento(evento(contrato, TipoEventoCicloVidaContrato.REMANEJAMENTO));
        rem.setDataEfetiva(EFEITO); rem.setIdentificacao("REM-LEGADO"); rem.setJustificativa("Redistribuição documentada");
        var primeiro = itemRemanejamento(base, base.getCargo(), -1);
        var segundo = itemRemanejamento(base, cargo(31, "Cargo atual segundo"), 1);
        segundo.setLotacaoNomeFotografia("Lotação fotografada");
        segundo.setJornadaDescricaoFotografia("Jornada fotografada");
        segundo.setCargoNomeFotografia("Cargo segundo fotografado");
        rem.adicionarItem(primeiro); rem.adicionarItem(segundo);
        when(contratos.findById(5L)).thenReturn(Optional.of(contrato));
        when(remanejamentos.findByContratoIdOrderByDataEfetivaAscRegistradoEmAsc(5L)).thenReturn(List.of(rem));

        var dto = servico.listar(5L).get(0);
        assertThat(dto.somaVariacoes()).isZero();
        var item = dto.itens().get(0);
        assertThat(item.composicaoId()).isEqualTo(50L);
        assertThat(item.lotacaoId()).isEqualTo(10L);
        assertThat(item.jornadaId()).isEqualTo(20L);
        assertThat(item.cargoId()).isEqualTo(30L);
        assertThat(item.quantidadeAnterior()).isEqualTo(5);
        assertThat(item.variacao()).isEqualTo(-1);
        assertThat(item.quantidadeResultante()).isEqualTo(4);
        assertThat(dto.itens().get(1).cargoNome()).isEqualTo("Cargo segundo fotografado");
        assertSoftly(s -> {
            s.assertThat(item.lotacaoNome()).isNull();
            s.assertThat(item.jornadaDescricao()).isNull();
            s.assertThat(item.cargoNome()).isNull();
        });
    }

    @Test
    void callbackDeNovoMapaPreservaOrigemDesconhecidaECapturaDestinoDocumentado() {
        var contrato = contrato(); var base = composicao(contrato);
        var transferencia = new TransferenciaGestaoContrato(); transferencia.setContrato(contrato);
        var mapa = mapa(transferencia, base);
        mapa.getLotacaoDestino().setMunicipio("Município do novo ato");
        var segunda = new JornadaDiaSemana(); segunda.setJornada(mapa.getJornadaDestino());
        segunda.setDiaSemana(DayOfWeek.MONDAY); segunda.setDiaTrabalho(true);
        segunda.setEntrada1("08:00"); segunda.setSaida1("12:00");
        mapa.getJornadaDestino().getDias().add(segunda);

        ReflectionTestUtils.invokeMethod(mapa, "validar");

        assertThat(mapa.getLotacaoDestinoNomeFotografia()).isEqualTo("Lotação destino atual");
        assertThat(mapa.getJornadaDestinoDescricaoFotografia()).isEqualTo("Jornada destino atual");
        assertThat(mapa.getCargoDestinoNomeFotografia()).isEqualTo("Cargo destino atual");
        assertThat(mapa.getLotacaoDestinoMunicipioFotografia()).isEqualTo("Município do novo ato");
        assertThat(mapa.getJornadaDestinoSemanalFotografia()).isEqualTo(FotografiaJornada.capturar(mapa.getJornadaDestino()));
        assertSoftly(s -> {
            s.assertThat(mapa.getLotacaoOrigemNomeFotografia()).as("callback não inventa a origem").isNull();
            s.assertThat(mapa.getJornadaOrigemDescricaoFotografia()).isNull();
            s.assertThat(mapa.getCargoOrigemNomeFotografia()).isNull();
        });
    }

    private Contrato contrato() {
        var c = new Contrato(); ReflectionTestUtils.setField(c, "id", 5L);
        c.setDataInicio(INICIO); c.setDataFim(LocalDate.of(2026, 12, 31)); return c;
    }

    private ComposicaoContratual composicao(Contrato contrato) {
        var c = new ComposicaoContratual(); c.setId(50L); c.setContrato(contrato);
        c.setLotacao(lotacao(10, "Lotação atual renomeada"));
        c.setJornada(jornada(20, "Jornada atual renomeada")); c.setCargo(cargo(30, "Cargo atual renomeado"));
        c.setEfetivoPrevisto(5); c.setDataInicioVigencia(INICIO); c.setDataFimVigencia(contrato.getDataFim());
        c.setLotacaoNomeFotografia("Lotação original"); c.setJornadaDescricaoFotografia("Jornada original");
        c.setCargoNomeFotografia("Cargo original"); return c;
    }

    private EventoCicloVidaContrato evento(Contrato contrato, TipoEventoCicloVidaContrato tipo) {
        var e = new EventoCicloVidaContrato(); ReflectionTestUtils.setField(e, "id", 7L);
        e.setContrato(contrato); e.setTipoEvento(tipo); e.setDataEfetiva(EFEITO); return e;
    }

    private MapeamentoTransferenciaGestao mapa(TransferenciaGestaoContrato transferencia, ComposicaoContratual origem) {
        var m = new MapeamentoTransferenciaGestao(); m.setTransferencia(transferencia); m.setComposicaoOrigem(origem);
        m.setLotacaoOrigem(origem.getLotacao()); m.setJornadaOrigem(origem.getJornada()); m.setCargoOrigem(origem.getCargo());
        m.setLotacaoDestino(lotacao(110, "Lotação destino atual"));
        m.setJornadaDestino(jornada(120, "Jornada destino atual")); m.setCargoDestino(cargo(130, "Cargo destino atual"));
        return m;
    }

    private RemanejamentoComposicao itemRemanejamento(ComposicaoContratual base, Cargo cargo, int variacao) {
        var i = new RemanejamentoComposicao(); i.setComposicaoBase(base); i.setLotacao(base.getLotacao());
        i.setJornada(base.getJornada()); i.setCargo(cargo);
        i.setQuantidadeAnterior(5); i.setVariacao(variacao); i.setQuantidadeResultante(5 + variacao); return i;
    }

    private Lotacao lotacao(long id, String nome) {
        var l = new Lotacao(); ReflectionTestUtils.setField(l, "id", id); l.setNome(nome); return l;
    }

    private Jornada jornada(long id, String descricao) {
        var j = new Jornada(); ReflectionTestUtils.setField(j, "id", id); j.setDescricao(descricao); return j;
    }

    private Cargo cargo(long id, String nome) {
        var c = new Cargo(); ReflectionTestUtils.setField(c, "id", id); c.setNome(nome); return c;
    }
}
