package com.example.siggtrat.integration;

import com.example.siggtrat.servico.*;
import com.example.siggtrat.repositorio.*;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDate;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/** Calendários e recortes das visões: SQL/JPA reais, fixture sintética e rollback. */
@Transactional
class PainelVisoesPostgresIT extends PostgresIntegrationTestSupport {
    @Autowired EntityManager em;
    @Autowired PainelAnaliticoRepositorio analiticoRepo;
    @Autowired PainelEfetivoRepositorio efetivoRepo;
    @Autowired PainelRepositorio gestaoRepo;
    final LocalDate ref = LocalDate.of(2026, 11, 10);

    @Test void calendariosEComparacaoUsamOMesmoRecorteNosIndicadoresEspecificos() {
        var a = PainelAnaliticoFixture.criar(em, "Visões A");
        var b = PainelAnaliticoFixture.criar(em, "Visões B no mesmo escopo", true, a.gestao());
        var fora = PainelAnaliticoFixture.criar(em, "Visões fora do escopo");
        var fornecedor = new com.example.siggtrat.entidade.Fornecedor();
        fornecedor.setRazaoSocial("Prestador sintético da visão A");
        fornecedor.setCnpj(String.format(java.util.Locale.ROOT, "75%012d", a.contrato().getId()));
        em.persist(fornecedor);
        a.contrato().setFornecedor(fornecedor);
        for (var dia : List.of("2026-01-01", "2026-10-01", "2026-11-01", "2026-11-09", "2026-11-10"))
            PainelAnaliticoFixture.frequencia(em, a, LocalDate.parse(dia), "PRESENCA");
        PainelAnaliticoFixture.frequencia(em, b, ref, "FALTA");
        em.flush();
        var contexto = mock(PainelServico.class, CALLS_REAL_METHODS);
        doReturn(new PainelServico.PainelCalculoContexto("Gestão Local", a.gestao().getNome(), a.gestao().getSuperintendencia().getId(), a.gestao().getId(), true, true, true, true, true)).when(contexto).contextoAtual();
        var analitico = new PainelAnaliticoServico(analiticoRepo, efetivoRepo, contexto);
        var servico = new PainelVisoesServico(analitico, contexto, gestaoRepo);
        String[] tipos = {"DIARIO", "SEMANAL", "MENSAL", "TRIMESTRAL", "ANUAL"};
        for (int i = 0; i < tipos.length; i++) {
            var d = servico.obter(tipos[i], ref, a.contrato().getId(), "LOTACAO", a.lotacao().getId(), true, 0, null);
            assertEquals(i + 1, d.analitico().resumoPeriodo().registros());
            assertEquals(d.analitico().resumoPeriodo().registros(), d.gestao().periodo().registrosOperacionais());
            assertEquals(d.analitico().intervalo().inicio(), d.gestao().periodo().inicio());
            assertEquals(d.analitico().intervalo().anteriorInicio(), d.gestao().comparativoPeriodo().inicioAnterior());
            assertEquals(d.analitico().intervalo().anteriorFim(), d.gestao().comparativoPeriodo().fimAnterior());
            assertEquals(0, d.gestao().periodo().faltasNaoJustificadas());
            assertTrue(d.gestao().unidades().stream().allMatch(u -> u.id().equals(a.lotacao().getId())));
        }
        var porContrato = servico.obter("DIARIO", ref, a.contrato().getId(), null, null, true, 0, null);
        var porUnidade = servico.obter("DIARIO", ref, null, "LOTACAO", a.lotacao().getId(), true, 0, null);
        var porPrestador = servico.obter("DIARIO", ref, null, null, null, true, 0, fornecedor.getId());
        for (var d : List.of(porContrato, porUnidade, porPrestador)) {
            assertEquals(1, d.gestao().periodo().registrosOperacionais());
            assertEquals(0, d.gestao().periodo().faltasNaoJustificadas());
            assertEquals(1, d.gestao().quadro().empregadosCadastrados());
            assertEquals(1, d.gestao().contratos().total());
            assertNotNull(d.efetivoAnterior());
        }
        var semComparacao = servico.obter("MENSAL", ref, null, null, null, false, 0, null);
        assertNull(semComparacao.gestao().comparativoPeriodo());
        assertNull(semComparacao.analitico().comparativo());
        assertNull(semComparacao.efetivoAnterior());
        assertThrows(org.springframework.web.server.ResponseStatusException.class,
                () -> servico.obter("DIARIO", ref, fora.contrato().getId(), null, null, true, 0, null));
    }
}
