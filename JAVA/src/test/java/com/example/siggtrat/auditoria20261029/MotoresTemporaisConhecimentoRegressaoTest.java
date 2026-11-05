package com.example.siggtrat.auditoria20261029;

import com.example.siggtrat.dominio.alocacao.*;
import com.example.siggtrat.dominio.vinculo.*;
import com.example.siggtrat.dto.PosicaoTemporalContratoDTO;
import com.example.siggtrat.entidade.*;
import com.example.siggtrat.repositorio.*;
import com.example.siggtrat.servico.*;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/** Um corte de conhecimento não pode usar projeções que só seriam conhecidas depois. */
class MotoresTemporaisConhecimentoRegressaoTest {
    private static final Instant REGISTRO = Instant.parse("2026-01-01T10:00:00Z");
    private static final Instant CORTE = Instant.parse("2026-01-15T10:00:00Z");
    private static final LocalDate DATA = LocalDate.of(2026, 6, 15);
    private AnnotationConfigApplicationContext contexto;
    private MotorTemporalContratoServico motorContrato;
    private MotorTemporalVinculoServico motorVinculo;
    private MotorTemporalAlocacaoServico motorAlocacao;
    private VinculoContratual vinculo;
    private AlocacaoVinculo alocacao;
    private final List<EventoVinculoContratual> eventosVinculo = new ArrayList<>();
    private final List<EventoAlocacaoVinculo> eventosAlocacao = new ArrayList<>();

    @BeforeEach
    void preparar() {
        EventoVinculoContratualRepositorio repositorioV = mock(EventoVinculoContratualRepositorio.class);
        EventoAlocacaoVinculoRepositorio repositorioA = mock(EventoAlocacaoVinculoRepositorio.class);
        AlocacaoVinculoRepositorio faixas = mock(AlocacaoVinculoRepositorio.class);
        motorContrato = mock(MotorTemporalContratoServico.class);
        Pessoa pessoa = new Pessoa(); pessoa.setId(1L); pessoa.setNome("Maria"); pessoa.setCpf("52998224725");
        Contrato contrato = new Contrato(); contrato.setId(2L); contrato.setEstadoNegocial(EstadoNegocialContrato.PUBLICADO);
        contrato.setDataInicio(LocalDate.of(2026, 1, 1)); contrato.setDataFim(LocalDate.of(2026, 12, 31));
        contrato.setAtivo(true);
        vinculo = new VinculoContratual(); vinculo.setId(3L); vinculo.setPessoa(pessoa); vinculo.setContrato(contrato);
        vinculo.setDataInicio(LocalDate.of(2026, 1, 1)); vinculo.setEstadoNegocial(EstadoNegocialVinculo.REGISTRADO);
        Lotacao l = new Lotacao(); l.setId(10L); l.setNome("Lotação histórica");
        Jornada j = new Jornada(); j.setId(11L); j.setDescricao("Turno histórico");
        Cargo c = new Cargo(); c.setId(12L); c.setNome("Cargo histórico");
        alocacao = new AlocacaoVinculo(); ReflectionTestUtils.setField(alocacao, "id", 4L); alocacao.setVinculo(vinculo);
        alocacao.setLotacao(l); alocacao.setJornada(j); alocacao.setCargo(c);
        alocacao.setDataInicio(LocalDate.of(2026, 1, 1)); alocacao.setDataFim(LocalDate.of(2026, 12, 31));
        alocacao.setEstadoNegocial(EstadoNegocialAlocacao.REGISTRADA);
        EventoVinculoContratual criacao = new EventoVinculoContratual(); criacao.setVinculo(vinculo);
        criacao.setTipoEvento(TipoEventoVinculoContratual.CRIACAO); criacao.setDataEfetiva(vinculo.getDataInicio());
        criacao.setInicioNovo(vinculo.getDataInicio()); criacao.setRegistradoEm(REGISTRO); eventosVinculo.add(criacao);
        EventoAlocacaoVinculo origem = new EventoAlocacaoVinculo(); origem.setVinculo(vinculo); origem.setAlocacaoNova(alocacao);
        origem.setTipoEvento(TipoEventoAlocacaoVinculo.CRIACAO); origem.setDataEfetiva(alocacao.getDataInicio());
        origem.setRegistradoEm(REGISTRO); origem.setInicioNovo(alocacao.getDataInicio()); origem.setFimNovo(alocacao.getDataFim());
        origem.setEstadoNovo(EstadoNegocialAlocacao.REGISTRADA); origem.setLotacaoNovaId(10L); origem.setLotacaoNovaNome(l.getNome());
        origem.setJornadaNovaId(11L); origem.setJornadaNovaDescricao(j.getDescricao()); origem.setCargoNovoId(12L); origem.setCargoNovoNome(c.getNome());
        eventosAlocacao.add(origem);
        when(repositorioV.findByVinculoIdAndRegistradoEmLessThanEqualOrderByRegistradoEmAscIdAsc(eq(3L), any()))
                .thenAnswer(i -> eventosVinculo.stream().filter(e -> !e.getRegistradoEm().isAfter(i.getArgument(1))).toList());
        when(repositorioA.findByVinculoIdAndRegistradoEmLessThanEqualOrderByRegistradoEmAscIdAsc(eq(3L), any()))
                .thenAnswer(i -> eventosAlocacao.stream().filter(e -> !e.getRegistradoEm().isAfter(i.getArgument(1))).toList());
        when(repositorioV.findByVinculoIdOrderByRegistradoEmAscIdAsc(3L)).thenAnswer(i -> List.copyOf(eventosVinculo));
        when(repositorioA.findByVinculoIdOrderByRegistradoEmAscIdAsc(3L)).thenAnswer(i -> List.copyOf(eventosAlocacao));
        when(faixas.findByVinculoIdOrderByDataInicioAscIdAsc(3L)).thenReturn(List.of(alocacao));
        when(motorContrato.reconstruirPosicao(eq(contrato), any(), any())).thenAnswer(i -> posicaoContrato(
                i.getArgument(1), i.getArgument(2), LocalDate.of(2026, 12, 31)));
        contexto = new AnnotationConfigApplicationContext();
        contexto.registerBean(EventoVinculoContratualRepositorio.class, () -> repositorioV);
        contexto.registerBean(EventoAlocacaoVinculoRepositorio.class, () -> repositorioA);
        contexto.registerBean(AlocacaoVinculoRepositorio.class, () -> faixas);
        contexto.registerBean(MotorTemporalContratoServico.class, () -> motorContrato);
        contexto.register(MotorTemporalVinculoServico.class, MotorTemporalAlocacaoServico.class);
        contexto.refresh();
        motorVinculo = contexto.getBean(MotorTemporalVinculoServico.class);
        motorAlocacao = contexto.getBean(MotorTemporalAlocacaoServico.class);
    }

    @AfterEach
    void fechar() { if (contexto != null) contexto.close(); }

    @Test
    void corteAnteriorAoRegistroNaoRevelaAlocacaoAtual() {
        assertTrue(motorAlocacao.reconstruirFaixas(vinculo, REGISTRO.minusSeconds(1)).isEmpty());
    }

    @Test
    void cancelamentoConhecidoDepoisNaoApagaAlocacaoDaVisaoAnterior() {
        vinculo.setEstadoNegocial(EstadoNegocialVinculo.CANCELADO);
        EventoVinculoContratual cancelamento = new EventoVinculoContratual(); cancelamento.setVinculo(vinculo);
        cancelamento.setTipoEvento(TipoEventoVinculoContratual.CANCELAMENTO_PROGRAMACAO);
        cancelamento.setDataEfetiva(vinculo.getDataInicio()); cancelamento.setRegistradoEm(Instant.parse("2026-02-01T10:00:00Z"));
        eventosVinculo.add(cancelamento);

        assertEquals(SituacaoTemporalAlocacao.VIGENTE,
                motorAlocacao.reconstruir(vinculo, DATA, CORTE).situacaoTemporal());
        assertEquals(SituacaoTemporalAlocacao.CANCELADA,
                motorAlocacao.reconstruir(vinculo, DATA, Instant.parse("2026-02-02T10:00:00Z")).situacaoTemporal());
    }

    @Test
    void reducaoPosteriorDoContratoNaoEncerraVinculoNoConhecimentoAnterior() {
        vinculo.getContrato().setDataFim(LocalDate.of(2026, 3, 31));

        assertEquals(SituacaoTemporalVinculo.VIGENTE,
                motorVinculo.reconstruir(vinculo, DATA, CORTE).situacaoTemporal());
    }

    @Test
    void prorrogacaoPosteriorNaoReabreVinculoNaVisaoAnteriorAoAditamento() {
        when(motorContrato.reconstruirPosicao(eq(vinculo.getContrato()), any(), eq(CORTE)))
                .thenAnswer(i -> posicaoContrato(i.getArgument(1), CORTE, LocalDate.of(2026, 3, 31)));

        assertEquals(SituacaoTemporalVinculo.ENCERRADO,
                motorVinculo.reconstruir(vinculo, DATA, CORTE).situacaoTemporal());
    }

    @Test
    void matriculasRetroativasRespeitamDataEfetivaMesmoQuandoRegistradasForaDeOrdem() {
        eventosVinculo.get(0).setMatriculaNova("ORIGINAL");
        EventoVinculoContratual posterior = new EventoVinculoContratual();
        posterior.setVinculo(vinculo); posterior.setTipoEvento(TipoEventoVinculoContratual.ALTERACAO_MATRICULA);
        posterior.setDataEfetiva(LocalDate.of(2026, 6, 1)); posterior.setRegistradoEm(REGISTRO.plusSeconds(60));
        posterior.setMatriculaAnterior("ORIGINAL"); posterior.setMatriculaNova("JUNHO");
        eventosVinculo.add(posterior);
        EventoVinculoContratual retroativo = new EventoVinculoContratual();
        retroativo.setVinculo(vinculo); retroativo.setTipoEvento(TipoEventoVinculoContratual.ALTERACAO_MATRICULA);
        retroativo.setDataEfetiva(LocalDate.of(2026, 3, 1)); retroativo.setRegistradoEm(REGISTRO.plusSeconds(120));
        retroativo.setMatriculaAnterior("ORIGINAL"); retroativo.setMatriculaNova("MARCO");
        eventosVinculo.add(retroativo);

        assertEquals("ORIGINAL", motorVinculo.reconstruir(vinculo, LocalDate.of(2026, 2, 1), CORTE).matricula());
        assertEquals("MARCO", motorVinculo.reconstruir(vinculo, LocalDate.of(2026, 4, 1), CORTE).matricula());
        assertEquals("JUNHO", motorVinculo.reconstruir(vinculo, LocalDate.of(2026, 7, 1), CORTE).matricula(),
                "Registro tardio de fato retroativo não pode substituir a matrícula de data efetiva posterior");
    }

    private PosicaoTemporalContratoDTO posicaoContrato(LocalDate data, Instant conhecimento, LocalDate fim) {
        return new PosicaoTemporalContratoDTO(2L, data, conhecimento, EstadoNegocialContrato.PUBLICADO,
                data.isAfter(fim) ? SituacaoTemporalContrato.ENCERRADO : SituacaoTemporalContrato.VIGENTE,
                HabilitacaoAdministrativaContrato.ATIVO, LocalDate.of(2026, 1, 1), fim, null, List.of(), List.of());
    }
}
