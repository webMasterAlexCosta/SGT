package com.example.siggtrat.contrato;

import com.example.siggtrat.dto.AcaoCicloVidaContratoDTO;
import com.example.siggtrat.dto.PosicaoTemporalContratoDTO;
import com.example.siggtrat.entidade.*;
import com.example.siggtrat.repositorio.AditamentoContratualRepositorio;
import com.example.siggtrat.repositorio.EventoCicloVidaContratoRepositorio;
import com.example.siggtrat.repositorio.RelacaoPrecedenciaEventoContratoRepositorio;
import com.example.siggtrat.repositorio.VersaoContratoNegocialRepositorio;
import com.example.siggtrat.servico.MotorTemporalContratoServico;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import java.lang.reflect.Field;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MotorTemporalContratoEtapa04Test {

    @Mock EventoCicloVidaContratoRepositorio eventoRepositorio;
    @Mock RelacaoPrecedenciaEventoContratoRepositorio relacaoRepositorio;
    @Mock VersaoContratoNegocialRepositorio versaoRepositorio;
    @Mock AditamentoContratualRepositorio aditamentoRepositorio;

    private MotorTemporalContratoServico motor;
    private Contrato contrato;
    private UsuarioAplicacao usuario;

    @BeforeEach
    void preparar() {
        motor = new MotorTemporalContratoServico(eventoRepositorio, relacaoRepositorio, versaoRepositorio, aditamentoRepositorio);
        contrato = new Contrato();
        contrato.setId(10L);
        contrato.setEstadoNegocial(EstadoNegocialContrato.PUBLICADO);
        contrato.setDataInicio(LocalDate.of(2026, 1, 1));
        contrato.setDataFim(LocalDate.of(2026, 12, 31));
        contrato.setPublicadoEm(Instant.now().minusSeconds(3600));

        usuario = new UsuarioAplicacao();
        usuario.setId(20L);
        usuario.setUsername("gestor.local");
    }

    @Test
    void conflitoNaMesmaDataEChaveSemPrecedenciaEhBloqueado() throws Exception {
        LocalDate data = LocalDate.of(2026, 6, 1);
        EventoCicloVidaContrato anterior = evento(1L, TipoEventoCicloVidaContrato.INATIVACAO_ADMINISTRATIVA, data);

        when(eventoRepositorio.findByContratoIdAndChaveNegocialAndDataEfetiva(
                10L, "HABILITACAO_ADMINISTRATIVA", data)).thenReturn(List.of(anterior));
        when(eventoRepositorio.findByContratoIdOrderByDataEfetivaAscRegistradoEmAsc(10L))
                .thenReturn(List.of(anterior));
        when(relacaoRepositorio.findByContratoIdOrderByRegistradoEmAsc(10L)).thenReturn(List.of());

        AcaoCicloVidaContratoDTO acao = new AcaoCicloVidaContratoDTO(
                data, data, "Reativação conflitante", "SEI-1");

        ResponseStatusException erro = assertThrows(ResponseStatusException.class, () ->
                motor.registrarEvento(
                        contrato,
                        TipoEventoCicloVidaContrato.REATIVACAO_ADMINISTRATIVA,
                        acao,
                        usuario,
                        "INATIVO",
                        "ATIVO"
                ));

        assertEquals(409, erro.getStatusCode().value());
        assertTrue(erro.getReason().contains("eventoPrecedenteId"));
        verify(eventoRepositorio, never()).save(argThat(e -> e.getId() == null));
    }

    @Test
    void sucedeExplicitoResolveConflitoSemUsarOrdemDeGravacao() throws Exception {
        LocalDate data = LocalDate.of(2026, 6, 1);
        EventoCicloVidaContrato anterior = evento(1L, TipoEventoCicloVidaContrato.INATIVACAO_ADMINISTRATIVA, data);
        List<EventoCicloVidaContrato> eventos = new ArrayList<>(List.of(anterior));
        List<RelacaoPrecedenciaEventoContrato> relacoes = new ArrayList<>();

        when(eventoRepositorio.findById(1L)).thenReturn(Optional.of(anterior));
        when(eventoRepositorio.findByContratoIdAndChaveNegocialAndDataEfetiva(
                10L, "HABILITACAO_ADMINISTRATIVA", data)).thenReturn(List.of(anterior));
        when(eventoRepositorio.findByContratoIdOrderByDataEfetivaAscRegistradoEmAsc(10L))
                .thenAnswer(inv -> List.copyOf(eventos));
        when(relacaoRepositorio.findByContratoIdOrderByRegistradoEmAsc(10L))
                .thenAnswer(inv -> List.copyOf(relacoes));
        when(eventoRepositorio.save(any(EventoCicloVidaContrato.class))).thenAnswer(inv -> {
            EventoCicloVidaContrato e = inv.getArgument(0);
            if (e.getId() == null) {
                setId(e, 2L);
                eventos.add(e);
            }
            return e;
        });
        when(relacaoRepositorio.save(any(RelacaoPrecedenciaEventoContrato.class))).thenAnswer(inv -> {
            RelacaoPrecedenciaEventoContrato r = inv.getArgument(0);
            relacoes.add(r);
            return r;
        });
        when(eventoRepositorio.saveAll(any())).thenAnswer(inv -> inv.getArgument(0));

        AcaoCicloVidaContratoDTO acao = new AcaoCicloVidaContratoDTO(
                data,
                data,
                "Reativação na mesma data com precedência formal",
                "SEI-2",
                1L,
                TipoRelacaoPrecedenciaNegocial.SUCEDE,
                2
        );

        EventoCicloVidaContrato novo = motor.registrarEvento(
                contrato,
                TipoEventoCicloVidaContrato.REATIVACAO_ADMINISTRATIVA,
                acao,
                usuario,
                "INATIVO",
                "ATIVO"
        );

        assertEquals(2L, novo.getId());
        assertEquals(1, relacoes.size());
        assertEquals(TipoRelacaoPrecedenciaNegocial.SUCEDE, relacoes.get(0).getTipoRelacao());
        assertSame(anterior, relacoes.get(0).getEventoAlvo());
        assertSame(novo, relacoes.get(0).getEventoFonte());
    }

    @Test
    void retificacaoPreservaFatoLogicoEIncrementaVersao() throws Exception {
        LocalDate data = LocalDate.of(2026, 4, 1);
        EventoCicloVidaContrato anterior = evento(5L, TipoEventoCicloVidaContrato.ENCERRAMENTO_ANTECIPADO, data);
        anterior.setFatoLogicoId("fato-vigencia-1");
        anterior.setVersaoNegocial(1);

        List<EventoCicloVidaContrato> eventos = new ArrayList<>(List.of(anterior));
        List<RelacaoPrecedenciaEventoContrato> relacoes = new ArrayList<>();

        when(eventoRepositorio.findById(5L)).thenReturn(Optional.of(anterior));
        when(eventoRepositorio.findByContratoIdAndChaveNegocialAndDataEfetiva(10L, "VIGENCIA", data))
                .thenReturn(List.of(anterior));
        when(eventoRepositorio.findByContratoIdOrderByDataEfetivaAscRegistradoEmAsc(10L))
                .thenAnswer(inv -> List.copyOf(eventos));
        when(relacaoRepositorio.findByContratoIdOrderByRegistradoEmAsc(10L))
                .thenAnswer(inv -> List.copyOf(relacoes));
        when(eventoRepositorio.save(any(EventoCicloVidaContrato.class))).thenAnswer(inv -> {
            EventoCicloVidaContrato e = inv.getArgument(0);
            if (e.getId() == null) {
                setId(e, 6L);
                eventos.add(e);
            }
            return e;
        });
        when(relacaoRepositorio.save(any(RelacaoPrecedenciaEventoContrato.class))).thenAnswer(inv -> {
            RelacaoPrecedenciaEventoContrato r = inv.getArgument(0);
            relacoes.add(r);
            return r;
        });
        when(eventoRepositorio.saveAll(any())).thenAnswer(inv -> inv.getArgument(0));

        EventoCicloVidaContrato retificado = motor.registrarEvento(
                contrato,
                TipoEventoCicloVidaContrato.ENCERRAMENTO_ANTECIPADO,
                new AcaoCicloVidaContratoDTO(
                        data,
                        data,
                        "Correção material da data",
                        "SEI-3",
                        5L,
                        TipoRelacaoPrecedenciaNegocial.RETIFICA,
                        2
                ),
                usuario,
                "2026-12-31",
                "2026-10-31"
        );

        assertEquals("fato-vigencia-1", retificado.getFatoLogicoId());
        assertEquals(2, retificado.getVersaoNegocial());
        assertEquals(SituacaoFatoNegocial.RETIFICADO, anterior.getSituacaoFato());
        assertEquals(SituacaoFatoNegocial.VIGENTE, retificado.getSituacaoFato());
    }

    @Test
    void consultaConhecidoAteDistingueConhecimentoDoFato() throws Exception {
        LocalDate dataEfetiva = LocalDate.of(2026, 2, 1);
        EventoCicloVidaContrato inativacao = evento(9L, TipoEventoCicloVidaContrato.INATIVACAO_ADMINISTRATIVA, dataEfetiva);
        Instant antesDoRegistro = inativacao.getRegistradoEm().minusSeconds(1);
        Instant depoisDoRegistro = inativacao.getRegistradoEm().plusSeconds(1);

        when(eventoRepositorio.findByContratoIdOrderByDataEfetivaAscRegistradoEmAsc(10L))
                .thenReturn(List.of(inativacao));
        when(relacaoRepositorio.findByContratoIdAndRegistradoEmLessThanEqualOrderByRegistradoEmAsc(eq(10L), any()))
                .thenReturn(List.of());
        when(versaoRepositorio.existsByContratoId(10L)).thenReturn(false);

        PosicaoTemporalContratoDTO antes = motor.reconstruirPosicao(
                contrato, LocalDate.of(2026, 3, 1), antesDoRegistro);
        PosicaoTemporalContratoDTO depois = motor.reconstruirPosicao(
                contrato, LocalDate.of(2026, 3, 1), depoisDoRegistro);

        // Sem fotografia inicial, o corte anterior ao primeiro ato é desconhecido.
        assertNull(antes.habilitacaoAdministrativa());
        assertEquals(HabilitacaoAdministrativaContrato.INATIVO, depois.habilitacaoAdministrativa());
        assertTrue(antes.eventosAplicados().isEmpty());
        assertEquals(1, depois.eventosAplicados().size());
    }

    @Test
    void correcaoCancelaAnulacaoSemApagarHistorico() throws Exception {
        LocalDate data = LocalDate.of(2026, 5, 1);
        EventoCicloVidaContrato anulacao = evento(30L, TipoEventoCicloVidaContrato.ANULACAO, data);
        EventoCicloVidaContrato correcao = evento(31L, TipoEventoCicloVidaContrato.CORRECAO_ANULACAO, data);
        RelacaoPrecedenciaEventoContrato relacao = new RelacaoPrecedenciaEventoContrato();
        relacao.setContrato(contrato); relacao.setEventoFonte(correcao); relacao.setEventoAlvo(anulacao);
        relacao.setTipoRelacao(TipoRelacaoPrecedenciaNegocial.CANCELA);
        when(eventoRepositorio.findByContratoIdOrderByDataEfetivaAscRegistradoEmAsc(10L))
                .thenReturn(List.of(anulacao, correcao));
        when(relacaoRepositorio.findByContratoIdAndRegistradoEmLessThanEqualOrderByRegistradoEmAsc(eq(10L), any()))
                .thenReturn(List.of(relacao));
        when(versaoRepositorio.existsByContratoId(10L)).thenReturn(false);

        PosicaoTemporalContratoDTO posicao = motor.reconstruirPosicao(contrato, data.plusDays(1), Instant.now());

        assertEquals(EstadoNegocialContrato.PUBLICADO, posicao.estadoNegocial());
        assertEquals(1, posicao.eventosAplicados().size());
        assertEquals(TipoEventoCicloVidaContrato.CORRECAO_ANULACAO, posicao.eventosAplicados().get(0).tipoEvento());
    }

    private EventoCicloVidaContrato evento(Long id, TipoEventoCicloVidaContrato tipo, LocalDate data) throws Exception {
        EventoCicloVidaContrato e = new EventoCicloVidaContrato();
        setId(e, id);
        e.setContrato(contrato);
        e.setTipoEvento(tipo);
        e.setChaveNegocial(com.example.siggtrat.servico.MotorTemporalContratoServico.chaveNegocialPadrao(tipo));
        e.setDataEfetiva(data);
        e.setMotivo("teste");
        return e;
    }

    private static void setId(Object alvo, Long id) throws Exception {
        Field campo = alvo.getClass().getDeclaredField("id");
        campo.setAccessible(true);
        campo.set(alvo, id);
    }
}
