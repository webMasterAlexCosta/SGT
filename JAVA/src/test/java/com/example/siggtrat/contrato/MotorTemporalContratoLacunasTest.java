package com.example.siggtrat.contrato;

import com.example.siggtrat.dto.AcaoCicloVidaContratoDTO;
import com.example.siggtrat.entidade.*;
import com.example.siggtrat.repositorio.AditamentoContratualRepositorio;
import com.example.siggtrat.repositorio.EventoCicloVidaContratoRepositorio;
import com.example.siggtrat.repositorio.RelacaoPrecedenciaEventoContratoRepositorio;
import com.example.siggtrat.repositorio.VersaoContratoNegocialRepositorio;
import com.example.siggtrat.servico.MotorTemporalContratoServico;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/** Regressões derivadas de CON-020/022, ADI-010/REM-004 e TMP-004/005. */
class MotorTemporalContratoLacunasTest {
    private final EventoCicloVidaContratoRepositorio eventos = mock(EventoCicloVidaContratoRepositorio.class);
    private final RelacaoPrecedenciaEventoContratoRepositorio relacoes = mock(RelacaoPrecedenciaEventoContratoRepositorio.class);
    private final VersaoContratoNegocialRepositorio versoes = mock(VersaoContratoNegocialRepositorio.class);
    private final MotorTemporalContratoServico motor = new MotorTemporalContratoServico(eventos, relacoes, versoes, mock(AditamentoContratualRepositorio.class));
    private final Contrato contrato = new Contrato();
    private final List<EventoCicloVidaContrato> fatos = new ArrayList<>();
    private final List<RelacaoPrecedenciaEventoContrato> precedencias = new ArrayList<>();
    private static final Instant T1 = Instant.parse("2026-01-02T12:00:00Z");
    private static final Instant T2 = Instant.parse("2026-01-03T12:00:00Z");
    private static final LocalDate EFEITO = LocalDate.of(2026, 1, 2);

    @BeforeEach
    void preparar() {
        contrato.setId(10L); contrato.setEstadoNegocial(EstadoNegocialContrato.PUBLICADO);
        contrato.setPublicadoEm(T1.minusSeconds(86400)); contrato.setAtivo(true);
        contrato.setDataInicio(LocalDate.of(2026, 1, 1)); contrato.setDataFim(LocalDate.of(2026, 12, 31));
        when(eventos.findByContratoIdOrderByDataEfetivaAscRegistradoEmAsc(10L)).thenAnswer(i -> List.copyOf(fatos));
        when(relacoes.findByContratoIdOrderByRegistradoEmAsc(10L)).thenAnswer(i -> List.copyOf(precedencias));
        when(relacoes.findByContratoIdAndRegistradoEmLessThanEqualOrderByRegistradoEmAsc(eq(10L), any()))
                .thenAnswer(i -> precedencias.stream().filter(r -> !r.getRegistradoEm().isAfter(i.getArgument(1))).toList());
        when(eventos.save(any(EventoCicloVidaContrato.class))).thenAnswer(i -> {
            EventoCicloVidaContrato e = i.getArgument(0);
            if (e.getId() == null) {
                ReflectionTestUtils.setField(e, "id", 100L + fatos.size()); fatos.add(e);
            }
            return e;
        });
        when(relacoes.save(any(RelacaoPrecedenciaEventoContrato.class))).thenAnswer(i -> {
            RelacaoPrecedenciaEventoContrato r = i.getArgument(0); precedencias.add(r); return r;
        });
        when(eventos.saveAll(any())).thenAnswer(i -> i.getArgument(0));
    }

    @Test
    void correcaoDaAnulacaoMantemBloqueioAteReativacaoExplicita() {
        var anulacao = evento(1L, TipoEventoCicloVidaContrato.ANULACAO, EFEITO, T1);
        var correcao = evento(2L, TipoEventoCicloVidaContrato.CORRECAO_ANULACAO, EFEITO, T2);
        relacionar(correcao, anulacao, TipoRelacaoPrecedenciaNegocial.CANCELA, T2);
        contrato.setAtivo(false);

        var posicao = motor.reconstruirPosicao(contrato, EFEITO.plusDays(1), T2.plusSeconds(1));

        assertThat(posicao.estadoNegocial()).isEqualTo(EstadoNegocialContrato.PUBLICADO);
        assertThat(posicao.habilitacaoAdministrativa()).isEqualTo(HabilitacaoAdministrativaContrato.INATIVO);
        assertThat(posicao.eventosAplicados()).extracting(e -> e.tipoEvento())
                .containsExactly(TipoEventoCicloVidaContrato.CORRECAO_ANULACAO);

        evento(3L, TipoEventoCicloVidaContrato.REATIVACAO_ADMINISTRATIVA, EFEITO.plusDays(2), T2.plusSeconds(2));
        assertThat(motor.reconstruirPosicao(contrato, EFEITO.plusDays(2), T2.plusSeconds(3))
                .habilitacaoAdministrativa()).isEqualTo(HabilitacaoAdministrativaContrato.ATIVO);
    }

    @Test
    void corteDeConhecimentoNaoExpoeRetificacaoConhecidaSomenteDepois() {
        var original = evento(1L, TipoEventoCicloVidaContrato.INATIVACAO_ADMINISTRATIVA, EFEITO, T1);
        var retificacao = evento(2L, TipoEventoCicloVidaContrato.INATIVACAO_ADMINISTRATIVA, EFEITO.plusDays(1), T2);
        retificacao.setFatoLogicoId(original.getFatoLogicoId());
        original.setSituacaoFato(SituacaoFatoNegocial.RETIFICADO);
        relacionar(retificacao, original, TipoRelacaoPrecedenciaNegocial.RETIFICA, T2);

        var antes = motor.reconstruirPosicao(contrato, EFEITO.plusDays(3), T1.plusSeconds(1));
        var depois = motor.reconstruirPosicao(contrato, EFEITO.plusDays(3), T2.plusSeconds(1));

        assertThat(antes.eventosAplicados()).hasSize(1);
        assertThat(antes.eventosAplicados().get(0).id()).isEqualTo(1L);
        assertThat(antes.eventosAplicados().get(0).situacaoFato()).isEqualTo(SituacaoFatoNegocial.VIGENTE);
        assertThat(depois.eventosAplicados()).hasSize(1);
        assertThat(depois.eventosAplicados().get(0).id()).isEqualTo(2L);
        assertThat(original.getSituacaoFato()).isEqualTo(SituacaoFatoNegocial.RETIFICADO);
    }

    @ParameterizedTest
    @CsvSource({"ADITAMENTO,CANCELA", "ADITAMENTO,RETIFICA", "ADITAMENTO,SUBSTITUI",
            "REMANEJAMENTO,CANCELA", "REMANEJAMENTO,RETIFICA", "REMANEJAMENTO,SUBSTITUI"})
    void instrumentoNaoPodeInvalidarEventoProprioDoCicloDeVida(
            TipoEventoCicloVidaContrato tipo, TipoRelacaoPrecedenciaNegocial relacao) {
        var inativacao = evento(1L, TipoEventoCicloVidaContrato.INATIVACAO_ADMINISTRATIVA, EFEITO, T1);
        when(eventos.findById(1L)).thenReturn(Optional.of(inativacao));
        var acao = new AcaoCicloVidaContratoDTO(EFEITO, EFEITO, "Instrumento documentado", "SEI-TESTE", 1L, relacao, null);

        assertThatThrownBy(() -> motor.registrarEvento(contrato, tipo, acao, null, "anterior", "novo", tipo + ":NOVO"))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(erro -> assertThat(((ResponseStatusException) erro).getStatusCode().value()).isIn(400, 409));
        assertThat(fatos).hasSize(1);
        assertThat(inativacao.getSituacaoFato()).isEqualTo(SituacaoFatoNegocial.VIGENTE);
        assertThat(precedencias).isEmpty();
    }

    @Test
    void precedenciaDeOrdemEntreTiposDiferentesContinuaPermitida() {
        var inativacao = evento(1L, TipoEventoCicloVidaContrato.INATIVACAO_ADMINISTRATIVA, EFEITO, T1);
        when(eventos.findById(1L)).thenReturn(Optional.of(inativacao));
        var acao = new AcaoCicloVidaContratoDTO(EFEITO, EFEITO, "Sequência documental", "SEI-TESTE",
                1L, TipoRelacaoPrecedenciaNegocial.SUCEDE, null);

        var registrado = motor.registrarEvento(contrato, TipoEventoCicloVidaContrato.ADITAMENTO,
                acao, null, "anterior", "novo", "ADITAMENTO:NOVO");

        assertThat(registrado.getTipoEvento()).isEqualTo(TipoEventoCicloVidaContrato.ADITAMENTO);
        assertThat(inativacao.getSituacaoFato()).isEqualTo(SituacaoFatoNegocial.VIGENTE);
        assertThat(precedencias).hasSize(1);
    }

    private EventoCicloVidaContrato evento(long id, TipoEventoCicloVidaContrato tipo, LocalDate data, Instant registro) {
        EventoCicloVidaContrato e = new EventoCicloVidaContrato();
        ReflectionTestUtils.setField(e, "id", id); ReflectionTestUtils.setField(e, "registradoEm", registro);
        e.setContrato(contrato); e.setTipoEvento(tipo); e.setDataEfetiva(data); e.setMotivo("Fato comprovado");
        e.setChaveNegocial(MotorTemporalContratoServico.chaveNegocialPadrao(tipo)); fatos.add(e); return e;
    }

    private void relacionar(EventoCicloVidaContrato fonte, EventoCicloVidaContrato alvo,
                            TipoRelacaoPrecedenciaNegocial tipo, Instant registro) {
        var r = new RelacaoPrecedenciaEventoContrato(); r.setContrato(contrato);
        r.setEventoFonte(fonte); r.setEventoAlvo(alvo); r.setTipoRelacao(tipo);
        ReflectionTestUtils.setField(r, "registradoEm", registro); precedencias.add(r);
    }
}
