package com.example.siggtrat.integration;

import com.example.siggtrat.dominio.acesso.PerfilAcesso;
import com.example.siggtrat.dominio.pendencia.*;
import com.example.siggtrat.dominio.relatorio.*;
import com.example.siggtrat.entidade.EmissaoRelatorioAdministrativo;
import com.example.siggtrat.entidade.PendenciaAdministrativa;
import com.example.siggtrat.repositorio.EmissaoRelatorioAdministrativoRepositorio;
import com.example.siggtrat.repositorio.PendenciaAdministrativaRepositorio;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Integração MySQL: persistência da Etapa 11")
class Etapa11PersistenceIT extends MySqlIntegrationTestSupport {
    @Autowired private PendenciaAdministrativaRepositorio pendencias;
    @Autowired private EmissaoRelatorioAdministrativoRepositorio emissoes;

    @Test
    @Transactional
    @DisplayName("pendência e snapshot comprimido da emissão preservam metadados auditáveis")
    void persistePendenciaEEmissaoVersionada() {
        PendenciaAdministrativa pendencia = new PendenciaAdministrativa();
        pendencia.setChaveOrigem("IT:FREQUENCIA:1:VERSAO:2");
        pendencia.setTipo(TipoPendencia.RETIFICACAO_FREQUENCIA);
        pendencia.setEstado(EstadoPendencia.ABERTA);
        pendencia.setMotivo("Retificação temporal em teste de integração.");
        pendencia.setDataEfetiva(LocalDate.of(2026, 8, 15));
        pendencia.setOperacaoOrigem("IMPACTO_TEMPORAL_FREQUENCIA");
        pendencia.setPerfilResponsavel(PerfilAcesso.GESTOR_LOCAL);
        pendencia.setEscopoBloqueio(EscopoBloqueioPendencia.FREQUENCIA_PERIODO);
        pendencia.setOrigemEntidade("Frequencia");
        pendencia.setOrigemId(1L);
        pendencia = pendencias.saveAndFlush(pendencia);

        byte[] snapshot = new byte[]{31, -117, 8, 0, 1, 2, 3, 4};
        EmissaoRelatorioAdministrativo emissao = new EmissaoRelatorioAdministrativo();
        emissao.setChaveSerie("a".repeat(64));
        emissao.setNumeroVersao(1);
        emissao.setTipo(TipoRelatorioAdministrativo.FREQUENCIA_CONSOLIDADA);
        emissao.setFormato(FormatoEmissaoRelatorio.VISUALIZACAO);
        emissao.setNatureza(NaturezaEmissaoRelatorio.ORIGINAL);
        emissao.setEstado(EstadoEmissaoRelatorio.VIGENTE);
        emissao.setInicio(LocalDate.of(2026, 8, 1));
        emissao.setFim(LocalDate.of(2026, 8, 31));
        emissao.setGeradoEm(Instant.now());
        emissao.setFonteConhecidaAte(Instant.now());
        emissao.setUsuarioId(999L);
        emissao.setUsuarioUsername("it.etapa11");
        emissao.setPerfilUsuario(PerfilAcesso.GESTOR_NACIONAL.name());
        emissao.setSujeitoRevisao(true);
        emissao.setQuantidadePendencias(1L);
        emissao.setAvisoRevisao("SUJEITO A REVISÃO — 1 pendência de retificação.");
        emissao.setConteudoSnapshotGzip(snapshot);
        emissao.setHashConteudo("b".repeat(64));
        emissao.setTamanhoConteudoBytes(42L);
        emissao.setFiltrosJson("{\"inicio\":\"2026-08-01\",\"fim\":\"2026-08-31\"}");
        emissao.setChaveIdempotencia("it-emissao-etapa11");
        emissao.setHashIntencao("c".repeat(64));
        emissao = emissoes.saveAndFlush(emissao);

        assertNotNull(pendencia.getId());
        assertNotNull(emissao.getId());
        assertTrue(emissao.isSujeitoRevisao());
        assertEquals(1L, emissao.getQuantidadePendencias());
        assertTrue(Arrays.equals(snapshot, emissao.getConteudoSnapshotGzip()));
        assertEquals(EstadoEmissaoRelatorio.VIGENTE, emissao.getEstado());
    }
}
