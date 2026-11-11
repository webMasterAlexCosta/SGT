package com.example.siggtrat.etapa07;

import com.example.siggtrat.dto.*;
import com.example.siggtrat.entidade.*;
import com.example.siggtrat.repositorio.*;
import com.example.siggtrat.servico.*;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

import java.time.*;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class TransferenciaGestaoContratoServicoEtapa07Test {
    @Test
    void transfereSomenteDepoisDeMapearContextoOperacionalDeDestino() {
        TransferenciaGestaoContratoRepositorio transferencias = mock(TransferenciaGestaoContratoRepositorio.class);
        GestaoLocalRepositorio gestoes = mock(GestaoLocalRepositorio.class);
        LotacaoRepositorio lotacoes = mock(LotacaoRepositorio.class);
        JornadaRepositorio jornadas = mock(JornadaRepositorio.class);
        CargoRepositorio cargos = mock(CargoRepositorio.class);
        ComposicaoContratualRepositorio composicoes = mock(ComposicaoContratualRepositorio.class);
        AlocacaoVinculoRepositorio alocacoes = mock(AlocacaoVinculoRepositorio.class);
        ComposicaoContratualTemporalServico temporal = mock(ComposicaoContratualTemporalServico.class);
        MotorTemporalContratoServico motor = mock(MotorTemporalContratoServico.class);
        AlocacaoVinculoServico alocacaoServico = mock(AlocacaoVinculoServico.class);
        PermissaoExcepcionalServico permissaoServico = mock(PermissaoExcepcionalServico.class);
        TransferenciaGestaoContratoServico servico = new TransferenciaGestaoContratoServico(transferencias, gestoes,
                lotacoes, jornadas, cargos, composicoes, alocacoes, temporal, motor, alocacaoServico,
                permissaoServico);

        GestaoLocal origem = gestao(1L, "Origem"); GestaoLocal destino = gestao(2L, "Destino");
        TipoContratacaoRepositorio tipos = mock(TipoContratacaoRepositorio.class);
        TipoContratacao tipoDestino = new TipoContratacao(); tipoDestino.setId(150L);
        tipoDestino.setGestaoLocal(destino); tipoDestino.setDescricao("Tipo destino"); tipoDestino.setAtivo(true);
        when(tipos.findById(150L)).thenReturn(Optional.of(tipoDestino)); servico.configurarTipos(tipos);
        // Esta prova unitária verifica coordenação e mapeamento. O replay e a
        // política dos vínculos são exercitados nos IT HTTP/PostgreSQL correspondentes.
        AditamentoContratualServico projecao = mock(AditamentoContratualServico.class);
        servico.configurarProjecaoContratual(projecao);
        GestaoContratoTemporalServico gestaoTemporal = mock(GestaoContratoTemporalServico.class);
        servico.configurarGestaoContratoTemporal(gestaoTemporal);
        Contrato contrato = new Contrato(); contrato.setId(9L); contrato.setGestaoLocal(origem);
        contrato.setEstadoNegocial(EstadoNegocialContrato.PUBLICADO); contrato.setDataInicio(LocalDate.of(2026, 1, 1));
        contrato.setDataFim(LocalDate.of(2026, 12, 31));
        when(gestaoTemporal.gestaoAtual(contrato)).thenReturn(origem);
        ComposicaoContratual base = composicao(30L, contrato, origem, 10L, 20L, 40L);
        Lotacao loteDestino = lotacao(110L, destino); Jornada jornadaDestino = jornada(120L, destino); Cargo cargoDestino = cargo(140L, destino);
        loteDestino.setTurnos(List.of(jornadaDestino));
        when(gestoes.findById(1L)).thenReturn(Optional.of(origem));
        when(gestoes.findById(2L)).thenReturn(Optional.of(destino));
        when(composicoes.findById(30L)).thenReturn(Optional.of(base));
        when(lotacoes.findById(110L)).thenReturn(Optional.of(loteDestino));
        when(jornadas.findById(120L)).thenReturn(Optional.of(jornadaDestino));
        when(cargos.findById(140L)).thenReturn(Optional.of(cargoDestino));
        var estadoOrigem = List.of(
                new ComposicaoContratualTemporalServico.ComposicaoProjetada(30L, base.getLotacao(), base.getJornada(), base.getCargo(),
                        base.getLotacao().getNome(), base.getJornada().getDescricao(), base.getCargo().getNome(),
                        2, LocalDate.of(2026, 1, 1), LocalDate.of(2026, 12, 31),
                        base.getLotacaoMunicipioFotografia(), base.getJornadaSemanalFotografia()));
        var estadoDestino = List.of(new ComposicaoContratualTemporalServico.ComposicaoProjetada(30L,
                loteDestino, jornadaDestino, cargoDestino, loteDestino.getNome(), jornadaDestino.getDescricao(), cargoDestino.getNome(),
                2, contrato.getDataInicio(), contrato.getDataFim(), null, null));
        when(temporal.projetarEntidades(eq(contrato), any(), any(), anySet())).thenReturn(estadoOrigem);
        when(temporal.projetarEntidades(eq(contrato), any(), any())).thenReturn(estadoDestino);
        when(temporal.localizar(anyList(), eq(30L), isNull(), isNull(), isNull())).thenReturn(Optional.of(estadoOrigem.get(0)));
        when(alocacoes.findByVinculoContratoIdOrderByVinculoIdAscDataInicioAscIdAsc(9L)).thenReturn(List.of());
        EventoCicloVidaContrato evento = new EventoCicloVidaContrato() {
            @Override public Long getId() { return 500L; }
        };
        evento.setTipoEvento(TipoEventoCicloVidaContrato.TRANSFERENCIA_GESTAO_LOCAL);
        evento.setDataEfetiva(LocalDate.of(2026, 7, 1));
        when(motor.registrarEvento(eq(contrato), eq(TipoEventoCicloVidaContrato.TRANSFERENCIA_GESTAO_LOCAL), any(), any(), any(), any(), isNull(), any(ConferenciaDocumentalTransferencia.class)))
                .thenReturn(evento);
        var salva = new java.util.concurrent.atomic.AtomicBoolean(false);
        when(transferencias.save(any(TransferenciaGestaoContrato.class))).thenAnswer(inv -> {
            salva.set(true); return inv.getArgument(0);
        });
        when(projecao.projetarVersaoContrato(eq(contrato), any(), any(), anySet())).thenAnswer(inv -> {
            boolean aplicada = salva.get() && !((Set<Long>) inv.getArgument(3)).contains(500L);
            GestaoLocal gestao = aplicada ? destino : origem;
            return new VersaoContratoNegocialDTO(null, 1, aplicada ? 500L : null, contrato.getDataInicio(), null,
                    "CONTRATO", 2026, "Contrato", aplicada ? 150L : null, aplicada ? "Tipo destino" : null,
                    null, null, null, null, contrato.getDataInicio(), contrato.getDataFim(), 2, gestao.getId(), gestao.getNome(),
                    false, false, false, null, null, HabilitacaoAdministrativaContrato.ATIVO, true);
        });
        UsuarioAplicacao usuario = new UsuarioAplicacao(); usuario.setId(8L); usuario.setUsername("gestor-origem");
        PermissaoExcepcional permissao = new PermissaoExcepcional();
        permissao.setAutorizacaoAdministrativa("Portaria 20/2026");
        UsoPermissaoExcepcional usoSintetico = new UsoPermissaoExcepcional(); usoSintetico.setPermissao(permissao);
        when(permissaoServico.exigirERegistrarUsoComRecibo(eq(usuario), any(), eq(contrato), eq(origem),
                anyString(), eq("Contrato"), eq(9L))).thenReturn(new PermissaoExcepcionalServico.ConsumoPermissao(permissao, usoSintetico));
        TransferenciaGestaoContratoRequisicao req = new TransferenciaGestaoContratoRequisicao(LocalDate.of(2026, 7, 1),
                LocalDate.of(2026, 6, 30), 2L, "reorganização real", "SEI-500", "Portaria 20/2026",
                List.of(new MapeamentoTransferenciaGestaoRequisicao(30L, null, null, null, 110L, 120L, 140L)), null, null, null, 150L, null, null, true, ConferenciaDocumentalTransferencia.VERSAO_DECLARACAO);

        TransferenciaGestaoContrato transferencia = servico.transferir(contrato, req, usuario);

        assertSame(destino, contrato.getGestaoLocal());
        assertEquals(1, transferencia.getMapeamentos().size());
        verify(alocacaoServico).aplicarTransferenciaGestao(transferencia, usuario);
        verify(motor).registrarFotografiaProjetada(eq(contrato), eq(evento),
                eq(LocalDate.of(2026, 7, 1)), any(VersaoContratoNegocialDTO.class));
    }

    @Test
    void bloqueiaTransferenciaSemMapeamentoDoContextoPosterior() {
        TransferenciaGestaoContratoServico servico = new TransferenciaGestaoContratoServico(
                mock(TransferenciaGestaoContratoRepositorio.class), mock(GestaoLocalRepositorio.class), mock(LotacaoRepositorio.class),
                mock(JornadaRepositorio.class), mock(CargoRepositorio.class), mock(ComposicaoContratualRepositorio.class),
                mock(AlocacaoVinculoRepositorio.class), mock(ComposicaoContratualTemporalServico.class),
                mock(MotorTemporalContratoServico.class), mock(AlocacaoVinculoServico.class),
                mock(PermissaoExcepcionalServico.class));
        Contrato contrato = new Contrato(); contrato.setId(9L); contrato.setGestaoLocal(gestao(1L, "Origem"));
        contrato.setEstadoNegocial(EstadoNegocialContrato.PUBLICADO); contrato.setDataInicio(LocalDate.of(2026, 1, 1));
        contrato.setDataFim(LocalDate.of(2026, 12, 31));
        TransferenciaGestaoContratoRequisicao req = new TransferenciaGestaoContratoRequisicao(LocalDate.of(2026, 7, 1), null,
                2L, "motivo", "SEI", "ato", List.of(), null, null, null, null, null, null, null, null);

        assertThrows(ResponseStatusException.class, () -> servico.transferir(contrato, req, new UsuarioAplicacao()));
    }

    private ComposicaoContratual composicao(Long id, Contrato contrato, GestaoLocal gestao, Long lotacaoId, Long jornadaId, Long cargoId) {
        Lotacao l = lotacao(lotacaoId, gestao); Jornada j = jornada(jornadaId, gestao); Cargo c = cargo(cargoId, gestao); l.setTurnos(List.of(j));
        ComposicaoContratual comp = new ComposicaoContratual(); comp.setId(id); comp.setContrato(contrato); comp.setLotacao(l); comp.setJornada(j);
        comp.setCargo(c); comp.setEfetivoPrevisto(2); comp.setDataInicioVigencia(LocalDate.of(2026, 1, 1));
        comp.setDataFimVigencia(LocalDate.of(2026, 12, 31)); return comp;
    }
    private Lotacao lotacao(Long id, GestaoLocal g) {
        Lotacao l = new Lotacao(); l.setId(id); l.setNome("L" + id); l.setGestaoLocal(g); l.setAtivo(true); return l;
    }
    private Jornada jornada(Long id, GestaoLocal g) {
        Jornada j = new Jornada(); j.setId(id); j.setDescricao("J" + id); j.setGestaoLocal(g); j.setAtivo(true); return j;
    }
    private Cargo cargo(Long id, GestaoLocal g) {
        Cargo c = new Cargo(); c.setId(id); c.setNome("C" + id); c.setGestaoLocal(g); c.setAtivo(true); return c;
    }
    private GestaoLocal gestao(Long id, String nome) {
        SuperintendenciaEstadual se = new SuperintendenciaEstadual(); se.setId(id + 50); se.setAtivo(true);
        GestaoLocal g = new GestaoLocal(); g.setId(id); g.setNome(nome); g.setCodigo("G" + id); g.setAtivo(true); g.setSuperintendencia(se); return g;
    }
}
