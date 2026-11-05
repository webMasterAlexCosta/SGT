package com.example.siggtrat.integration;

import com.example.siggtrat.dominio.relatorio.EstadoEmissaoRelatorio;
import com.example.siggtrat.dominio.relatorio.FormatoEmissaoRelatorio;
import com.example.siggtrat.dominio.relatorio.NaturezaEmissaoRelatorio;
import com.example.siggtrat.dominio.relatorio.TipoRelatorioAdministrativo;
import com.example.siggtrat.entidade.EmissaoRelatorioAdministrativo;
import com.example.siggtrat.repositorio.EmissaoRelatorioAdministrativoRepositorio;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

@DisplayName("Integração MySQL: fechamento de persistência da Etapa 12")
class Etapa12PersistenceIT extends MySqlIntegrationTestSupport {

    @Autowired private EmissaoRelatorioAdministrativoRepositorio emissoes;
    @Autowired private PlatformTransactionManager transactionManager;
    @PersistenceContext private EntityManager entityManager;

    private final String prefixo = "it-etapa12-" + UUID.randomUUID();

    @AfterEach
    void limpar() {
        new TransactionTemplate(transactionManager).executeWithoutResult(status -> {
            var criadasPeloTeste = entityManager.createQuery("""
                    select e from EmissaoRelatorioAdministrativo e
                    where e.chaveIdempotencia like :prefixo
                    """, EmissaoRelatorioAdministrativo.class)
                    .setParameter("prefixo", prefixo + "%").getResultList();
            criadasPeloTeste.forEach(entityManager::remove);
            entityManager.flush();
        });
    }

    @Test
    @DisplayName("LOB de 32 KiB sobrevive a transações distintas sem truncamento")
    void lobGrandePersisteIntegralmente() {
        byte[] conteudo = new byte[32 * 1024];
        for (int i = 0; i < conteudo.length; i++) conteudo[i] = (byte) (i % 251);
        TransactionTemplate tx = new TransactionTemplate(transactionManager);

        Long id = tx.execute(status -> emissoes.saveAndFlush(
                novaEmissao(serie(), 1, 12001L, chave("lob"), conteudo)).getId());
        assertNotNull(id);

        byte[] recuperado = tx.execute(status -> emissoes.findById(id).orElseThrow().getConteudoSnapshotGzip());

        assertNotNull(recuperado);
        assertEquals(conteudo.length, recuperado.length);
        assertArrayEquals(conteudo, recuperado);
    }

    @Test
    @DisplayName("idempotência é única por usuário no schema físico")
    void idempotenciaDuplicadaEhRejeitada() {
        String chave = chave("idem");
        emissoes.saveAndFlush(novaEmissao(serie(), 1, 12002L, chave, new byte[]{1}));

        assertThrows(DataIntegrityViolationException.class, () -> emissoes.saveAndFlush(
                novaEmissao(serie(), 1, 12002L, chave, new byte[]{2})));
    }

    @Test
    @DisplayName("número de versão não se repete dentro da série")
    void versaoDuplicadaNaSerieEhRejeitada() {
        String serie = serie();
        emissoes.saveAndFlush(novaEmissao(serie, 1, 12003L, chave("serie-a"), new byte[]{1}));

        assertThrows(DataIntegrityViolationException.class, () -> emissoes.saveAndFlush(
                novaEmissao(serie, 1, 12004L, chave("serie-b"), new byte[]{2})));
    }

    private EmissaoRelatorioAdministrativo novaEmissao(String serie, int versao, long usuarioId,
                                                         String idempotencia, byte[] conteudo) {
        EmissaoRelatorioAdministrativo emissao = new EmissaoRelatorioAdministrativo();
        emissao.setChaveSerie(serie);
        emissao.setNumeroVersao(versao);
        emissao.setTipo(TipoRelatorioAdministrativo.FREQUENCIA_CONSOLIDADA);
        emissao.setFormato(FormatoEmissaoRelatorio.VISUALIZACAO);
        emissao.setNatureza(NaturezaEmissaoRelatorio.ORIGINAL);
        emissao.setEstado(EstadoEmissaoRelatorio.VIGENTE);
        emissao.setInicio(LocalDate.of(2026, 8, 1));
        emissao.setFim(LocalDate.of(2026, 8, 31));
        emissao.setGeradoEm(Instant.now());
        emissao.setFonteConhecidaAte(Instant.now());
        emissao.setUsuarioId(usuarioId);
        emissao.setUsuarioUsername("it.etapa12");
        emissao.setPerfilUsuario("GESTOR_NACIONAL");
        emissao.setSujeitoRevisao(false);
        emissao.setQuantidadePendencias(0);
        emissao.setAvisoRevisao("Sem pendências conhecidas no corte temporal.");
        emissao.setConteudoSnapshotGzip(Arrays.copyOf(conteudo, conteudo.length));
        emissao.setHashConteudo("a".repeat(64));
        emissao.setTamanhoConteudoBytes(conteudo.length);
        emissao.setFiltrosJson("{}");
        emissao.setChaveIdempotencia(idempotencia);
        emissao.setHashIntencao("b".repeat(64));
        return emissao;
    }

    private String serie() {
        return UUID.randomUUID().toString().replace("-", "")
                + UUID.randomUUID().toString().replace("-", "");
    }

    private String chave(String sufixo) {
        return prefixo + "-" + sufixo;
    }
}
