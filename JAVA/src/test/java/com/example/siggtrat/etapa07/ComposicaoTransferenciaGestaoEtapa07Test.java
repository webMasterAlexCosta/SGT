package com.example.siggtrat.etapa07;

import com.example.siggtrat.entidade.*;
import com.example.siggtrat.repositorio.*;
import com.example.siggtrat.servico.ComposicaoContratualTemporalServico;
import com.example.siggtrat.servico.MotorTemporalContratoServico;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.time.*;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class ComposicaoTransferenciaGestaoEtapa07Test {
    @Test
    void transferenciaProjetadaTrocaSomenteOContextoPosteriorMapeado() throws Exception {
        ComposicaoContratualRepositorio composicoes = mock(ComposicaoContratualRepositorio.class);
        AditamentoContratualRepositorio aditamentos = mock(AditamentoContratualRepositorio.class);
        RemanejamentoContratualRepositorio remanejamentos = mock(RemanejamentoContratualRepositorio.class);
        MotorTemporalContratoServico motor = mock(MotorTemporalContratoServico.class);
        TransferenciaGestaoContratoRepositorio transferencias = mock(TransferenciaGestaoContratoRepositorio.class);
        ComposicaoContratualTemporalServico servico = new ComposicaoContratualTemporalServico(composicoes, aditamentos, remanejamentos, motor);
        servico.configurarTransferenciasGestao(transferencias);
        Contrato contrato = contrato();
        ComposicaoContratual origem = composicao(1L, contrato, 10L, "Origem", 20L, "Diurno", 30L, "Cargo O", 1);
        MapeamentoTransferenciaGestao mapa = new MapeamentoTransferenciaGestao();
        mapa.setLotacaoOrigem(origem.getLotacao()); mapa.setJornadaOrigem(origem.getJornada()); mapa.setCargoOrigem(origem.getCargo());
        mapa.setLotacaoDestino(catalogoLotacao(110L, "Destino", 100L));
        mapa.setJornadaDestino(catalogoJornada(120L, "Noturno", 100L));
        mapa.setCargoDestino(catalogoCargo(130L, "Cargo D", 100L));
        mapa.getLotacaoDestino().setTurnos(List.of(mapa.getJornadaDestino()));
        TransferenciaGestaoContrato transferencia = new TransferenciaGestaoContrato(); transferencia.setMapeamentos(List.of(mapa));
        EventoCicloVidaContrato evento = new EventoCicloVidaContrato(); definirId(evento, 77L); evento.setTipoEvento(TipoEventoCicloVidaContrato.TRANSFERENCIA_GESTAO_LOCAL);
        evento.setDataEfetiva(LocalDate.of(2026, 7, 1));

        when(composicoes.findByContratoIdOrderByLotacaoNomeAscJornadaDescricaoAscCargoNomeAsc(5L)).thenReturn(List.of(origem));
        when(aditamentos.findByContratoIdOrderByDataEfetivaAscRegistradoEmAsc(5L)).thenReturn(List.of());
        when(remanejamentos.findByContratoIdOrderByDataEfetivaAscRegistradoEmAsc(5L)).thenReturn(List.of());
        when(motor.eventosValidosOrdenados(eq(5L), any(), any())).thenReturn(List.of(evento));
        when(transferencias.findByEventoId(77L)).thenReturn(Optional.of(transferencia));

        var projetada = servico.projetarEntidades(contrato, LocalDate.of(2026, 7, 1), Instant.now());

        assertEquals(1, projetada.size());
        assertEquals("Destino", projetada.get(0).lotacao().getNome());
        assertEquals("Noturno", projetada.get(0).jornada().getDescricao());
        assertEquals("Cargo D", projetada.get(0).cargo().getNome());
    }

    @Test
    void transferenciaSemMapaPreservaUltimaVerdadeEProduzDiagnostico() throws Exception {
        ComposicaoContratualRepositorio composicoes = mock(ComposicaoContratualRepositorio.class);
        AditamentoContratualRepositorio aditamentos = mock(AditamentoContratualRepositorio.class);
        RemanejamentoContratualRepositorio remanejamentos = mock(RemanejamentoContratualRepositorio.class);
        MotorTemporalContratoServico motor = mock(MotorTemporalContratoServico.class);
        TransferenciaGestaoContratoRepositorio transferencias = mock(TransferenciaGestaoContratoRepositorio.class);
        ComposicaoContratualTemporalServico servico = new ComposicaoContratualTemporalServico(composicoes, aditamentos, remanejamentos, motor);
        servico.configurarTransferenciasGestao(transferencias);
        Contrato contrato = contrato();
        ComposicaoContratual origem = composicao(1L, contrato, 10L, "Origem", 20L, "Diurno", 30L, "Cargo O", 1);
        EventoCicloVidaContrato evento = new EventoCicloVidaContrato(); definirId(evento, 78L); evento.setTipoEvento(TipoEventoCicloVidaContrato.TRANSFERENCIA_GESTAO_LOCAL);
        evento.setDataEfetiva(LocalDate.of(2026, 7, 1));
        TransferenciaGestaoContrato transferencia = new TransferenciaGestaoContrato(); transferencia.setMapeamentos(List.of());
        when(composicoes.findByContratoIdOrderByLotacaoNomeAscJornadaDescricaoAscCargoNomeAsc(5L)).thenReturn(List.of(origem));
        when(aditamentos.findByContratoIdOrderByDataEfetivaAscRegistradoEmAsc(5L)).thenReturn(List.of());
        when(remanejamentos.findByContratoIdOrderByDataEfetivaAscRegistradoEmAsc(5L)).thenReturn(List.of());
        when(motor.eventosValidosOrdenados(eq(5L), any(), any())).thenReturn(List.of(evento));
        when(transferencias.findByEventoId(78L)).thenReturn(Optional.of(transferencia));

        var projetada = servico.projetarEntidades(contrato, LocalDate.of(2026, 7, 1), Instant.now());
        var conflitos = servico.diagnosticarConflitos(contrato, LocalDate.of(2026, 7, 1), Instant.now());
        assertEquals(1, projetada.size());
        assertEquals("Origem", projetada.get(0).lotacao().getNome());
        assertEquals(1, conflitos.size());
        assertEquals(78L, conflitos.get(0).eventoId());
    }

    private Contrato contrato() throws Exception {
        Contrato c = new Contrato(); definirId(c, 5L); c.setDataInicio(LocalDate.of(2026, 1, 1)); c.setDataFim(LocalDate.of(2026, 12, 31));
        c.setGestaoLocal(gestao(1L)); return c;
    }

    private ComposicaoContratual composicao(Long id, Contrato contrato, Long lotacaoId, String lotacao, Long jornadaId,
                                             String jornada, Long cargoId, String cargo, int efetivo) throws Exception {
        ComposicaoContratual c = new ComposicaoContratual(); c.setId(id); c.setContrato(contrato); c.setLotacao(catalogoLotacao(lotacaoId, lotacao, 1L));
        c.setJornada(catalogoJornada(jornadaId, jornada, 1L)); c.setCargo(catalogoCargo(cargoId, cargo, 1L));
        c.getLotacao().setTurnos(List.of(c.getJornada())); c.setEfetivoPrevisto(efetivo); c.setDataInicioVigencia(LocalDate.of(2026, 1, 1));
        c.setDataFimVigencia(LocalDate.of(2026, 12, 31)); return c;
    }
    private Lotacao catalogoLotacao(Long id, String nome, Long gestaoId) throws Exception {
        Lotacao l = new Lotacao(); definirId(l, id); l.setNome(nome); l.setGestaoLocal(gestao(gestaoId)); l.setAtivo(true); return l;
    }
    private Jornada catalogoJornada(Long id, String nome, Long gestaoId) throws Exception {
        Jornada j = new Jornada(); definirId(j, id); j.setDescricao(nome); j.setGestaoLocal(gestao(gestaoId)); j.setAtivo(true); return j;
    }
    private Cargo catalogoCargo(Long id, String nome, Long gestaoId) throws Exception {
        Cargo c = new Cargo(); definirId(c, id); c.setNome(nome); c.setGestaoLocal(gestao(gestaoId)); c.setAtivo(true); return c;
    }
    private GestaoLocal gestao(Long id) throws Exception {
        GestaoLocal g = new GestaoLocal(); definirId(g, id); g.setNome("G" + id); g.setAtivo(true); return g;
    }
    private void definirId(Object alvo, Long id) throws Exception {
        Field campo = alvo.getClass().getDeclaredField("id"); campo.setAccessible(true); campo.set(alvo, id);
    }
}
