package com.example.siggtrat.servico;

import com.example.siggtrat.dominio.alocacao.EstadoNegocialAlocacao;
import com.example.siggtrat.dominio.relatorio.EstadoEmissaoRelatorio;
import com.example.siggtrat.dominio.relatorio.TipoRelatorioAdministrativo;
import com.example.siggtrat.entidade.*;
import com.example.siggtrat.repositorio.EmissaoRelatorioAdministrativoRepositorio;
import com.example.siggtrat.repositorio.EmissaoRelatorioAdministrativoRepositorio.CandidatoInvalidacao;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class InvalidacaoEmissaoTemporalTest {
    private static final LocalDate JUNHO = LocalDate.of(2026, 6, 1), JULHO = LocalDate.of(2026, 7, 1);
    private static final Instant ANTES = Instant.parse("2026-05-01T00:00:00Z");
    private final EmissaoRelatorioAdministrativoRepositorio repo = mock(EmissaoRelatorioAdministrativoRepositorio.class);
    private final GestaoContratoTemporalServico gestoes = mock(GestaoContratoTemporalServico.class);
    private final MotorTemporalAlocacaoServico alocacoes = mock(MotorTemporalAlocacaoServico.class);
    private final ComposicaoContratualTemporalServico composicoes = mock(ComposicaoContratualTemporalServico.class);
    private final EntityManager em = mock(EntityManager.class);
    private final InvalidacaoEmissaoRelatorioServico servico = new InvalidacaoEmissaoRelatorioServico(repo,
            mock(RegistroAuditoriaServico.class));
    private final Contrato contrato = new Contrato();

    InvalidacaoEmissaoTemporalTest() {
        ReflectionTestUtils.setField(contrato, "id", 10L);
        servico.configurarContextoTemporal(gestoes, alocacoes, composicoes, em);
    }

    @Test
    void intervaloAtingeOrigemEDestinoEPreservaDatasExternasEOutroContrato() {
        GestaoLocal a = gestao(1L), b = gestao(2L);
        when(gestoes.gestoesNoIntervalo(eq(contrato), any(), any(), any())).thenReturn(List.of(a, b));
        var origem = candidata(1, JUNHO, JULHO.plusDays(10), "10", 1L, null, null);
        var destino = candidata(2, JUNHO, JULHO.plusDays(10), "10", 2L, null, null);
        var anterior = candidata(3, JUNHO.minusDays(10), JUNHO.minusDays(1), "10", 1L, null, null);
        var posterior = candidata(4, JULHO.plusDays(11), JULHO.plusDays(20), "10", 2L, null, null);
        var outro = candidata(5, JUNHO, JULHO.plusDays(10), "99", 1L, null, null);
        prepararLote(origem, destino, anterior, posterior, outro);

        assertEquals(2, servico.invalidarPorContrato(contrato, JUNHO, JULHO.plusDays(10), "Fonte alterada", null));
        assertEquals(EstadoEmissaoRelatorio.SUPERADA, entidade(1).getEstado());
        assertEquals(EstadoEmissaoRelatorio.SUPERADA, entidade(2).getEstado());
        verify(repo, never()).findById(3L);
        verify(repo, never()).findById(4L);
        verify(repo, never()).findById(5L);
        verify(em).refresh(entidade(1), LockModeType.PESSIMISTIC_WRITE);
        verify(em).refresh(entidade(2), LockModeType.PESSIMISTIC_WRITE);
    }

    @Test
    void gestaoELotacaoPrecisamCoincidirNoMesmoSegmentoDoIntervalo() {
        GestaoLocal a = gestao(1L), b = gestao(2L);
        when(gestoes.gestoesNoIntervalo(eq(contrato), any(), any(), any())).thenAnswer(inv ->
                ((LocalDate) inv.getArgument(1)).isBefore(JULHO) ? List.of(a) : List.of(b));
        VinculoContratual vinculo = vinculo();
        when(alocacoes.reconstruirFaixas(eq(vinculo), any())).thenReturn(List.of(
                faixa(1, 10, 100, JUNHO, JULHO.minusDays(1)),
                faixa(2, 20, 200, JULHO, JULHO.plusDays(10))));
        var origemReal = candidata(1, JUNHO, JULHO.plusDays(10), "", 1L, 10L, 100L);
        var destinoReal = candidata(2, JUNHO, JULHO.plusDays(10), "", 2L, 20L, 200L);
        var cruzamentoInexistente = candidata(3, JUNHO, JULHO.plusDays(10), "", 1L, 20L, 200L);
        prepararLote(origemReal, destinoReal, cruzamentoInexistente);

        assertEquals(2, servico.invalidarPorVinculo(vinculo, JUNHO, JULHO.plusDays(10), "Fonte alterada", null));
        verify(repo, never()).findById(3L);
        assertEquals(EstadoEmissaoRelatorio.SUPERADA, entidade(1).getEstado());
        assertEquals(EstadoEmissaoRelatorio.SUPERADA, entidade(2).getEstado());
    }

    @Test
    void retificacaoAtingeAlocacaoAnteriorENovaEPreservaOutroRecorteEConteudo() {
        VinculoContratual vinculo = vinculo();
        when(alocacoes.reconstruirFaixas(eq(vinculo), any())).thenAnswer(inv ->
                ANTES.equals(inv.getArgument(1))
                        ? List.of(faixa(1, 10, 100, JUNHO, JULHO.minusDays(1)))
                        : List.of(faixa(2, 20, 200, JUNHO, JULHO.minusDays(1))));
        var antes = candidata(1, JUNHO, JUNHO.plusDays(10), "10", null, 10L, 100L);
        var depois = candidata(2, JUNHO, JUNHO.plusDays(10), "10", null, 20L, 200L);
        var outro = candidata(3, JUNHO, JUNHO.plusDays(10), "10", null, 30L, 300L);
        prepararLote(antes, depois, outro);

        assertEquals(2, servico.invalidarPorVinculo(vinculo, JUNHO, JULHO.minusDays(1), "Fonte alterada", null));
        verify(repo, never()).findById(3L);
        assertEquals("hash-original-1", entidade(1).getHashConteudo());
        assertArrayEquals(new byte[]{1, 2, 3}, entidade(1).getConteudoSnapshotGzip());
        assertEquals("hash-original-2", entidade(2).getHashConteudo());
        verifyNoInteractions(gestoes); // Escopo nacional não depende da Gestão atual do usuário.
    }

    @Test
    void contratoNaoInvalidaLotacaoDeOutraFaixaTerritorialOuOutroContrato() {
        GestaoLocal a = gestao(1L), b = gestao(2L);
        when(gestoes.datasDeMudancaNoIntervalo(eq(contrato), any(), any(), any())).thenReturn(List.of(JUNHO, JULHO));
        when(gestoes.gestaoEm(eq(contrato), eq(JUNHO), any())).thenReturn(a);
        when(gestoes.gestaoEm(eq(contrato), eq(JULHO), any())).thenReturn(b);
        when(composicoes.projetarEntidades(eq(contrato), eq(JUNHO), any())).thenReturn(List.of(composicao(a, 10, 100)));
        when(composicoes.projetarEntidades(eq(contrato), eq(JULHO), any())).thenReturn(List.of(composicao(b, 20, 200)));
        var real = candidata(1, JUNHO, JULHO.plusDays(10), "", 2L, 20L, 200L);
        var cruzamento = candidata(2, JUNHO, JULHO.plusDays(10), "", 1L, 20L, 200L);
        var outroRecorte = candidata(3, JUNHO, JULHO.plusDays(10), "", 1L, 30L, 300L);
        prepararLote(real, cruzamento, outroRecorte);

        assertEquals(1, servico.invalidarPorContrato(contrato, JUNHO, JULHO.plusDays(10), "Fonte alterada", null));
        verify(repo, never()).findById(2L);
        verify(repo, never()).findById(3L);
    }

    private final java.util.Map<Long, EmissaoRelatorioAdministrativo> entidades = new java.util.HashMap<>();
    private EmissaoRelatorioAdministrativo entidade(long id) { return entidades.get(id); }

    private void prepararLote(CandidatoInvalidacao... candidatas) {
        when(repo.buscarCandidatasInvalidacaoEmLote(any(), any(), nullable(LocalDate.class), anyLong(), any()))
                .thenReturn(List.of(candidatas), List.of());
    }

    private CandidatoInvalidacao candidata(long id, LocalDate inicio, LocalDate fim, String contratos,
                                           Long gestao, Long lotacao, Long jornada) {
        CandidatoInvalidacao c = mock(CandidatoInvalidacao.class);
        when(c.getId()).thenReturn(id); when(c.getInicio()).thenReturn(inicio); when(c.getFim()).thenReturn(fim);
        when(c.getContratoIdsCsv()).thenReturn(contratos); when(c.getGestaoLocalEscopoId()).thenReturn(gestao);
        when(c.getSuperintendenciaEscopoId()).thenReturn(null);
        when(c.getLotacaoId()).thenReturn(lotacao); when(c.getJornadaId()).thenReturn(jornada);
        when(c.getFonteConhecidaAte()).thenReturn(ANTES);
        EmissaoRelatorioAdministrativo e = new EmissaoRelatorioAdministrativo();
        ReflectionTestUtils.setField(e, "id", id); e.setInicio(inicio); e.setFim(fim);
        e.setEstado(EstadoEmissaoRelatorio.VIGENTE); e.setTipo(TipoRelatorioAdministrativo.FREQUENCIA_CONSOLIDADA);
        e.setNumeroVersao(1); e.setHashConteudo("hash-original-" + id); e.setConteudoSnapshotGzip(new byte[]{1, 2, 3});
        entidades.put(id, e); when(repo.findById(id)).thenReturn(Optional.of(e));
        return c;
    }

    private GestaoLocal gestao(long id) {
        GestaoLocal g = new GestaoLocal(); ReflectionTestUtils.setField(g, "id", id); return g;
    }

    private VinculoContratual vinculo() {
        VinculoContratual v = new VinculoContratual(); ReflectionTestUtils.setField(v, "id", 20L);
        v.setContrato(contrato); return v;
    }

    private MotorTemporalAlocacaoServico.FaixaProjetada faixa(long id, long lotacao, long jornada,
                                                               LocalDate inicio, LocalDate fim) {
        return new MotorTemporalAlocacaoServico.FaixaProjetada(id, 1L, lotacao, "Lotação",
                jornada, "Jornada", 1L, "Cargo", inicio, fim, EstadoNegocialAlocacao.REGISTRADA);
    }

    private ComposicaoContratualTemporalServico.ComposicaoProjetada composicao(GestaoLocal gestao,
                                                                               long lotacaoId, long jornadaId) {
        Lotacao lotacao = new Lotacao("Lotação de controle"); lotacao.setGestaoLocal(gestao);
        ReflectionTestUtils.setField(lotacao, "id", lotacaoId);
        Jornada jornada = new Jornada(); jornada.setGestaoLocal(gestao);
        ReflectionTestUtils.setField(jornada, "id", jornadaId);
        return new ComposicaoContratualTemporalServico.ComposicaoProjetada(1L, lotacao, jornada, null,
                "Lotação", "Jornada", "Cargo", 10, JUNHO, null, null, null);
    }
}
