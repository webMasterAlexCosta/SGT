package com.example.siggtrat.contrato;

import com.example.siggtrat.dto.AcaoCicloVidaContratoDTO;
import com.example.siggtrat.entidade.*;
import com.example.siggtrat.repositorio.*;
import com.example.siggtrat.servico.MotorTemporalContratoServico;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.Optional;
import java.util.List;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class MotorPrecedenciaCicloVidaTest {
    @Test
    void correcaoDeUmaAnulacaoNaoRetiraValidadeDeOutraAnulacaoIndependente() {
        var eventos = mock(EventoCicloVidaContratoRepositorio.class);
        var relacoes = mock(RelacaoPrecedenciaEventoContratoRepositorio.class);
        var motor = new MotorTemporalContratoServico(eventos, relacoes, mock(VersaoContratoNegocialRepositorio.class),
                mock(AditamentoContratualRepositorio.class));
        var contrato = new Contrato(); contrato.setId(10L); contrato.setEstadoNegocial(EstadoNegocialContrato.PUBLICADO);
        contrato.setPublicadoEm(Instant.parse("2026-01-01T00:00:00Z"));
        contrato.setDataInicio(LocalDate.of(2026, 1, 1)); contrato.setDataFim(LocalDate.of(2026, 12, 31));
        var original = fato(1L, contrato, TipoEventoCicloVidaContrato.ANULACAO, LocalDate.of(2026, 1, 20));
        var correcao = fato(2L, contrato, TipoEventoCicloVidaContrato.CORRECAO_ANULACAO, LocalDate.of(2026, 1, 20));
        var outraAnulacao = fato(3L, contrato, TipoEventoCicloVidaContrato.ANULACAO, LocalDate.of(2026, 1, 10));
        var relacao = new RelacaoPrecedenciaEventoContrato(); relacao.setContrato(contrato);
        relacao.setEventoFonte(correcao); relacao.setEventoAlvo(original); relacao.setTipoRelacao(TipoRelacaoPrecedenciaNegocial.CANCELA);
        when(eventos.findByContratoIdOrderByDataEfetivaAscRegistradoEmAsc(10L)).thenReturn(List.of(original, correcao, outraAnulacao));
        when(relacoes.findByContratoIdAndRegistradoEmLessThanEqualOrderByRegistradoEmAsc(eq(10L), any())).thenReturn(List.of(relacao));

        var posicao = motor.reconstruirPosicao(contrato, LocalDate.of(2026, 2, 1), Instant.now().plusSeconds(1));

        assertThat(posicao.estadoNegocial()).isEqualTo(EstadoNegocialContrato.ANULADO);
        assertThat(motor.estadoNegocialEm(contrato, LocalDate.of(2026, 2, 1), Instant.now().plusSeconds(1)))
                .isEqualTo(EstadoNegocialContrato.ANULADO);
    }

    @ParameterizedTest
    @EnumSource(value = TipoEventoCicloVidaContrato.class,
            names = {"INATIVACAO_ADMINISTRATIVA", "REATIVACAO_ADMINISTRATIVA"})
    void operacaoAdministrativaNaoSubstituiCorrecaoExplicitaDeAnulacao(TipoEventoCicloVidaContrato tipo) {
        var eventos = mock(EventoCicloVidaContratoRepositorio.class);
        var relacoes = mock(RelacaoPrecedenciaEventoContratoRepositorio.class);
        var motor = new MotorTemporalContratoServico(eventos, relacoes, mock(VersaoContratoNegocialRepositorio.class),
                mock(AditamentoContratualRepositorio.class));
        var contrato = new Contrato(); contrato.setId(10L);
        var alvo = new EventoCicloVidaContrato(); alvo.setContrato(contrato);
        alvo.setTipoEvento(TipoEventoCicloVidaContrato.ANULACAO); alvo.setDataEfetiva(LocalDate.of(2026, 7, 1));
        ReflectionTestUtils.setField(alvo, "id", 1L);
        when(eventos.findById(1L)).thenReturn(Optional.of(alvo));
        when(eventos.save(any(EventoCicloVidaContrato.class))).thenAnswer(i -> {
            EventoCicloVidaContrato e = i.getArgument(0); ReflectionTestUtils.setField(e, "id", 2L); return e;
        });
        when(relacoes.save(any(RelacaoPrecedenciaEventoContrato.class))).thenAnswer(i -> i.getArgument(0));
        var acao = new AcaoCicloVidaContratoDTO(alvo.getDataEfetiva(), alvo.getDataEfetiva(), "Ato administrativo", "SEI-ADMIN",
                1L, TipoRelacaoPrecedenciaNegocial.CANCELA, null);

        assertThatThrownBy(() -> motor.registrarEvento(contrato, tipo, acao, null, "anterior", "novo"))
                .isInstanceOf(ResponseStatusException.class);
    }

    private EventoCicloVidaContrato fato(long id, Contrato contrato, TipoEventoCicloVidaContrato tipo, LocalDate data) {
        var evento = new EventoCicloVidaContrato(); ReflectionTestUtils.setField(evento, "id", id);
        evento.setContrato(contrato); evento.setTipoEvento(tipo); evento.setDataEfetiva(data);
        evento.setChaveNegocial(MotorTemporalContratoServico.chaveNegocialPadrao(tipo)); return evento;
    }
}
