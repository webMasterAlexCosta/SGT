package com.example.siggtrat.entidade;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class EntityIntegrityValidationTest {

    @Test
    void frequenciaDeveSincronizarMesEAnoComDataReferencia() {
        Frequencia frequencia = new Frequencia();
        frequencia.setDataReferencia(LocalDate.of(2026, 8, 5));

        frequencia.antesPersistirOuAtualizar();

        assertEquals(8, frequencia.getMes());
        assertEquals(2026, frequencia.getAno());
        assertNotNull(frequencia.getUpdatedAt());
    }

    @Test
    void lotacaoDeveRejeitarMarcadorDeErroDePlanilha() {
        Lotacao lotacao = new Lotacao();
        lotacao.setNome("#VALOR!");
        lotacao.setGestaoLocal(new GestaoLocal());

        IllegalStateException erro = assertThrows(
                IllegalStateException.class,
                lotacao::normalizar
        );

        assertTrue(erro.getMessage().contains("marcador inválido"));
    }

    @Test
    void historicoEncerradoDeveRemoverMarcadorAtivo() {
        HistoricoVinculoContrato historico = new HistoricoVinculoContrato();
        Empregado empregado = new Empregado();
        Contrato contrato = new Contrato();
        SuperintendenciaEstadual se = new SuperintendenciaEstadual("SE/RJ", "RJ");
        se.setId(1L);
        GestaoLocal gestaoLocal = new GestaoLocal();
        gestaoLocal.setId(10L);
        gestaoLocal.setSuperintendencia(se);
        empregado.setGestaoLocal(gestaoLocal);
        contrato.setGestaoLocal(gestaoLocal);
        ZonedDateTime inicio = ZonedDateTime.of(
                2026, 1, 1, 0, 0, 0, 0,
                ZoneId.of("America/Sao_Paulo")
        );

        historico.setEmpregado(empregado);
        historico.setContrato(contrato);
        historico.setDataInicio(inicio);
        historico.validarIntegridade();
        assertEquals(Boolean.TRUE, historico.getVinculoAtivo());
        assertEquals(10L, historico.getGestaoLocalSnapshotId());
        assertEquals(1L, historico.getSuperintendenciaSnapshotId());

        historico.setDataFim(inicio.plusDays(1));
        historico.validarIntegridade();
        assertNull(historico.getVinculoAtivo());
    }

    @Test
    void historicoCadastralPreservaEscopoMesmoDepoisDeTransferencia() {
        SuperintendenciaEstadual rj = new SuperintendenciaEstadual("SE/RJ", "RJ");
        rj.setId(1L);
        GestaoLocal gtratRj = new GestaoLocal();
        gtratRj.setId(10L);
        gtratRj.setSuperintendencia(rj);

        Empregado empregado = new Empregado();
        empregado.setGestaoLocal(gtratRj);
        HistoricoEmpregado historico = new HistoricoEmpregado();
        historico.setEmpregado(empregado);
        historico.preencherEscopoHistorico();

        SuperintendenciaEstadual sp = new SuperintendenciaEstadual("SE/SP", "SP");
        sp.setId(2L);
        GestaoLocal gedisSp = new GestaoLocal();
        gedisSp.setId(20L);
        gedisSp.setSuperintendencia(sp);
        empregado.setGestaoLocal(gedisSp);

        assertEquals(10L, historico.getGestaoLocalSnapshotId());
        assertEquals(1L, historico.getSuperintendenciaSnapshotId());
    }

    @Test
    void historicoDeveRejeitarDataFinalAnteriorAInicial() {
        HistoricoVinculoContrato historico = new HistoricoVinculoContrato();
        historico.setEmpregado(new Empregado());
        historico.setContrato(new Contrato());

        ZonedDateTime inicio = ZonedDateTime.now();
        historico.setDataInicio(inicio);
        historico.setDataFim(inicio.minusMinutes(1));

        assertThrows(IllegalStateException.class, historico::validarIntegridade);
    }
    @Test
    void empregadoDeveRejeitarDoisContratosAtuais() {
        GestaoLocal gestaoLocal = new GestaoLocal();
        gestaoLocal.setId(10L);

        Lotacao lotacao = new Lotacao();
        lotacao.setId(20L);
        lotacao.setGestaoLocal(gestaoLocal);

        Jornada jornada = new Jornada();
        jornada.setId(30L);
        jornada.setGestaoLocal(gestaoLocal);

        StatusEmpregado status = new StatusEmpregado("ATIVO");
        Cargo cargo = new Cargo("Vigilante", "5173-30");

        Contrato primeiro = new Contrato();
        primeiro.setId(40L);
        primeiro.setGestaoLocal(gestaoLocal);
        primeiro.setAtivo(true);

        Contrato segundo = new Contrato();
        segundo.setId(41L);
        segundo.setGestaoLocal(gestaoLocal);
        segundo.setAtivo(true);

        Empregado empregado = new Empregado();
        empregado.setGestaoLocal(gestaoLocal);
        empregado.setLotacao(lotacao);
        empregado.setJornada(jornada);
        empregado.setStatus(status);
        empregado.setCargo(cargo);
        empregado.setContratos(List.of(primeiro, segundo));

        IllegalStateException erro = assertThrows(
                IllegalStateException.class,
                empregado::validarIntegridadeOrganizacional
        );

        assertTrue(erro.getMessage().contains("somente um contrato atual"));
    }

    @Test
    void empregadoAtivoDeveAceitarExatamenteUmContratoDoMesmoGestor() {
        GestaoLocal gestaoLocal = new GestaoLocal();
        gestaoLocal.setId(10L);

        Lotacao lotacao = new Lotacao();
        lotacao.setGestaoLocal(gestaoLocal);

        Jornada jornada = new Jornada();
        jornada.setGestaoLocal(gestaoLocal);

        Contrato contrato = new Contrato();
        contrato.setId(40L);
        contrato.setGestaoLocal(gestaoLocal);
        contrato.setAtivo(true);

        Empregado empregado = new Empregado();
        empregado.setGestaoLocal(gestaoLocal);
        empregado.setLotacao(lotacao);
        empregado.setJornada(jornada);
        empregado.setStatus(new StatusEmpregado("ATIVO"));
        empregado.setCargo(new Cargo("Vigilante", "5173-30"));
        empregado.setContratos(List.of(contrato));

        assertDoesNotThrow(empregado::validarIntegridadeOrganizacional);
    }

}
