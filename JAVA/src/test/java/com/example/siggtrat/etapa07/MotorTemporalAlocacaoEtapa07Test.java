package com.example.siggtrat.etapa07;

import com.example.siggtrat.dominio.alocacao.*;
import com.example.siggtrat.dominio.vinculo.EstadoNegocialVinculo;
import com.example.siggtrat.dto.PosicaoAlocacaoTemporalDTO;
import com.example.siggtrat.entidade.*;
import com.example.siggtrat.repositorio.AlocacaoVinculoRepositorio;
import com.example.siggtrat.repositorio.EventoAlocacaoVinculoRepositorio;
import com.example.siggtrat.servico.MotorTemporalAlocacaoServico;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.time.*;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class MotorTemporalAlocacaoEtapa07Test {
    @Test
    void fotografiaDoEventoPreservaNomesDaAlocacaoHistorica() throws Exception {
        EventoAlocacaoVinculoRepositorio eventos = mock(EventoAlocacaoVinculoRepositorio.class);
        AlocacaoVinculoRepositorio alocacoes = mock(AlocacaoVinculoRepositorio.class);
        MotorTemporalAlocacaoServico motor = new MotorTemporalAlocacaoServico(eventos, alocacoes);
        VinculoContratual vinculo = vinculo();
        AlocacaoVinculo faixa = faixa(vinculo);
        EventoAlocacaoVinculo evento = new EventoAlocacaoVinculo();
        definirId(evento, 31L); evento.setVinculo(vinculo); evento.setAlocacaoNova(faixa);
        evento.setTipoEvento(TipoEventoAlocacaoVinculo.CRIACAO); evento.setDataEfetiva(LocalDate.of(2026, 6, 1));
        evento.setEstadoNovo(EstadoNegocialAlocacao.REGISTRADA); evento.setComposicaoNovaId(9L);
        evento.setLotacaoNovaId(10L); evento.setLotacaoNovaNome("Pronto Atendimento");
        evento.setJornadaNovaId(11L); evento.setJornadaNovaDescricao("12x36");
        evento.setCargoNovoId(12L); evento.setCargoNovoNome("Técnico"); evento.setInicioNovo(LocalDate.of(2026, 6, 1));
        evento.setFimNovo(LocalDate.of(2026, 6, 30));
        evento.setHashEvento("d7fd86b211f619e07d4ab9ecd1a3a6bf473fb451dee88e2e64806b66592ceabf");
        Instant conhecido = Instant.now();
        when(eventos.findByVinculoIdAndRegistradoEmLessThanEqualOrderByRegistradoEmAscIdAsc(eq(7L), any())).thenReturn(List.of(evento));
        when(eventos.findByVinculoIdOrderByRegistradoEmAscIdAsc(7L)).thenReturn(List.of(evento));

        PosicaoAlocacaoTemporalDTO posicao = motor.reconstruir(vinculo, LocalDate.of(2026, 6, 15), conhecido);

        assertEquals(SituacaoTemporalAlocacao.VIGENTE, posicao.situacaoTemporal());
        assertEquals("Pronto Atendimento", posicao.lotacaoNome());
        assertEquals("12x36", posicao.jornadaDescricao());
        assertEquals("Técnico", posicao.cargoNome());
        assertTrue(motor.cadeiaHashIntegra(7L));
    }

    @Test
    void faixaCanceladaNaoEhReconstruidaComoEfetiva() throws Exception {
        EventoAlocacaoVinculoRepositorio eventos = mock(EventoAlocacaoVinculoRepositorio.class);
        AlocacaoVinculoRepositorio alocacoes = mock(AlocacaoVinculoRepositorio.class);
        MotorTemporalAlocacaoServico motor = new MotorTemporalAlocacaoServico(eventos, alocacoes);
        VinculoContratual vinculo = vinculo();
        AlocacaoVinculo faixa = faixa(vinculo);
        EventoAlocacaoVinculo evento = new EventoAlocacaoVinculo();
        definirId(evento, 32L); evento.setVinculo(vinculo); evento.setAlocacaoNova(faixa);
        evento.setTipoEvento(TipoEventoAlocacaoVinculo.CANCELAMENTO_PROGRAMACAO); evento.setDataEfetiva(LocalDate.of(2026, 6, 1));
        evento.setEstadoNovo(EstadoNegocialAlocacao.CANCELADA); evento.setLotacaoNovaId(10L); evento.setJornadaNovaId(11L);
        evento.setCargoNovoId(12L); evento.setInicioNovo(LocalDate.of(2026, 6, 1)); evento.setFimNovo(LocalDate.of(2026, 6, 30));
        evento.setHashEvento("b".repeat(64));
        when(eventos.findByVinculoIdAndRegistradoEmLessThanEqualOrderByRegistradoEmAscIdAsc(eq(7L), any())).thenReturn(List.of(evento));

        assertEquals(SituacaoTemporalAlocacao.SEM_ALOCACAO,
                motor.reconstruir(vinculo, LocalDate.of(2026, 6, 15), Instant.now()).situacaoTemporal());
    }

    @Test
    void ajusteDaFaixaAnteriorEvitaSobreposicaoNaReconstrucaoDaMudanca() throws Exception {
        EventoAlocacaoVinculoRepositorio eventos = mock(EventoAlocacaoVinculoRepositorio.class);
        AlocacaoVinculoRepositorio alocacoes = mock(AlocacaoVinculoRepositorio.class);
        MotorTemporalAlocacaoServico motor = new MotorTemporalAlocacaoServico(eventos, alocacoes);
        VinculoContratual vinculo = vinculo();
        AlocacaoVinculo antiga = faixa(vinculo);
        AlocacaoVinculo nova = faixaDestino(vinculo);
        EventoAlocacaoVinculo troca = evento(vinculo, 40L, nova, TipoEventoAlocacaoVinculo.ALTERACAO,
                21L, 10L, "Pronto Atendimento", 11L, "12x36", 12L, "Técnico",
                LocalDate.of(2026, 6, 1), LocalDate.of(2026, 6, 30), EstadoNegocialAlocacao.REGISTRADA,
                22L, 110L, "Nova Lotação", 111L, "Noturno", 112L, "Novo Cargo",
                LocalDate.of(2026, 6, 10), LocalDate.of(2026, 6, 30), EstadoNegocialAlocacao.REGISTRADA);
        EventoAlocacaoVinculo ajuste = evento(vinculo, 41L, antiga, TipoEventoAlocacaoVinculo.AJUSTE_LIMITE_VINCULO,
                21L, 10L, "Pronto Atendimento", 11L, "12x36", 12L, "Técnico",
                LocalDate.of(2026, 6, 1), LocalDate.of(2026, 6, 30), EstadoNegocialAlocacao.REGISTRADA,
                21L, 10L, "Pronto Atendimento", 11L, "12x36", 12L, "Técnico",
                LocalDate.of(2026, 6, 1), LocalDate.of(2026, 6, 9), EstadoNegocialAlocacao.REGISTRADA);
        when(eventos.findByVinculoIdAndRegistradoEmLessThanEqualOrderByRegistradoEmAscIdAsc(eq(7L), any())).thenReturn(List.of(troca, ajuste));

        PosicaoAlocacaoTemporalDTO posicao = motor.reconstruir(vinculo, LocalDate.of(2026, 6, 15), Instant.now());

        assertEquals("Nova Lotação", posicao.lotacaoNome());
        assertEquals("Novo Cargo", posicao.cargoNome());
    }

    private VinculoContratual vinculo() {
        GestaoLocal g = new GestaoLocal(); g.setId(1L); g.setNome("GL");
        Contrato c = new Contrato(); c.setId(2L); c.setGestaoLocal(g); c.setDataInicio(LocalDate.of(2026, 1, 1));
        Pessoa p = new Pessoa(); p.setId(3L); p.setNome("Maria"); p.setCpf("52998224725");
        VinculoContratual v = new VinculoContratual(); v.setId(7L); v.setPessoa(p); v.setContrato(c);
        v.setDataInicio(LocalDate.of(2026, 6, 1)); v.setDataFim(LocalDate.of(2026, 6, 30));
        v.setEstadoNegocial(EstadoNegocialVinculo.REGISTRADO); return v;
    }

    private AlocacaoVinculo faixa(VinculoContratual v) throws Exception {
        GestaoLocal g = v.getContrato().getGestaoLocal();
        Lotacao l = new Lotacao(); definirId(l, 10L); l.setNome("Atual"); l.setGestaoLocal(g);
        Jornada j = new Jornada(); definirId(j, 11L); j.setDescricao("Atual"); j.setGestaoLocal(g);
        Cargo c = new Cargo(); definirId(c, 12L); c.setNome("Atual"); c.setGestaoLocal(g);
        AlocacaoVinculo a = new AlocacaoVinculo(); definirId(a, 21L); a.setVinculo(v); a.setLotacao(l); a.setJornada(j); a.setCargo(c);
        a.setDataInicio(LocalDate.of(2026, 6, 1)); a.setDataFim(LocalDate.of(2026, 6, 30));
        a.setEstadoNegocial(EstadoNegocialAlocacao.REGISTRADA); return a;
    }

    private AlocacaoVinculo faixaDestino(VinculoContratual v) throws Exception {
        GestaoLocal g = v.getContrato().getGestaoLocal();
        Lotacao l = new Lotacao(); definirId(l, 110L); l.setNome("Nova Lotação"); l.setGestaoLocal(g);
        Jornada j = new Jornada(); definirId(j, 111L); j.setDescricao("Noturno"); j.setGestaoLocal(g);
        Cargo c = new Cargo(); definirId(c, 112L); c.setNome("Novo Cargo"); c.setGestaoLocal(g);
        AlocacaoVinculo a = new AlocacaoVinculo(); definirId(a, 22L); a.setVinculo(v); a.setLotacao(l); a.setJornada(j); a.setCargo(c);
        a.setDataInicio(LocalDate.of(2026, 6, 10)); a.setDataFim(LocalDate.of(2026, 6, 30));
        a.setEstadoNegocial(EstadoNegocialAlocacao.REGISTRADA); return a;
    }

    private EventoAlocacaoVinculo evento(VinculoContratual vinculo, Long id, AlocacaoVinculo alocacaoNova,
                                          TipoEventoAlocacaoVinculo tipo, Long anteriorId, Long lotacaoAnteriorId,
                                          String lotacaoAnterior, Long jornadaAnteriorId, String jornadaAnterior,
                                          Long cargoAnteriorId, String cargoAnterior, LocalDate inicioAnterior,
                                          LocalDate fimAnterior, EstadoNegocialAlocacao estadoAnterior, Long composicaoNova,
                                          Long lotacaoNovaId, String lotacaoNova, Long jornadaNovaId, String jornadaNova,
                                          Long cargoNovoId, String cargoNovo, LocalDate inicioNovo, LocalDate fimNovo,
                                          EstadoNegocialAlocacao estadoNovo) throws Exception {
        EventoAlocacaoVinculo e = new EventoAlocacaoVinculo(); definirId(e, id); e.setVinculo(vinculo); e.setAlocacaoNova(alocacaoNova);
        e.setTipoEvento(tipo); e.setDataEfetiva(inicioNovo); e.setHashEvento(("c" + id).repeat(32).substring(0, 64));
        e.setAlocacaoAnteriorId(anteriorId); e.setLotacaoAnteriorId(lotacaoAnteriorId); e.setLotacaoAnteriorNome(lotacaoAnterior);
        e.setJornadaAnteriorId(jornadaAnteriorId); e.setJornadaAnteriorDescricao(jornadaAnterior); e.setCargoAnteriorId(cargoAnteriorId);
        e.setCargoAnteriorNome(cargoAnterior); e.setInicioAnterior(inicioAnterior); e.setFimAnterior(fimAnterior); e.setEstadoAnterior(estadoAnterior);
        e.setComposicaoNovaId(composicaoNova); e.setLotacaoNovaId(lotacaoNovaId); e.setLotacaoNovaNome(lotacaoNova);
        e.setJornadaNovaId(jornadaNovaId); e.setJornadaNovaDescricao(jornadaNova); e.setCargoNovoId(cargoNovoId);
        e.setCargoNovoNome(cargoNovo); e.setInicioNovo(inicioNovo); e.setFimNovo(fimNovo); e.setEstadoNovo(estadoNovo); return e;
    }

    private void definirId(Object alvo, Long id) throws Exception {
        Field campo = alvo.getClass().getDeclaredField("id"); campo.setAccessible(true); campo.set(alvo, id);
    }
}
