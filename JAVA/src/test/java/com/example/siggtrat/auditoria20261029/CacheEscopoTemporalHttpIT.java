package com.example.siggtrat.auditoria20261029;

import com.example.siggtrat.utilitario.RelogioSistema;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.cache.CacheManager;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

/** Cadeia MVC/autorização/MySQL real, cache HTTP habilitado com armazenamento em memória. */
@Import(CacheEscopoTemporalHttpIT.CacheMemoria.class)
class CacheEscopoTemporalHttpIT extends ContratosHttpBase {
    @TestConfiguration
    static class CacheMemoria {
        @Bean @Primary CacheManager cacheAuditoria() { return new ConcurrentMapCacheManager(); }
    }

    @DynamicPropertySource
    static void cacheAtivo(DynamicPropertyRegistry registry) {
        registry.add("app.cache.redis.http-get.enabled", () -> "true");
    }

    @Autowired CacheManager caches;
    @Autowired com.example.siggtrat.servico.PoliticaTemporalFrequenciaServico politica;

    @AfterEach
    void limparCaches() {
        for (String nome : caches.getCacheNames()) caches.getCache(nome).clear();
    }

    @Test
    void cacheInternoNaoConservaEstadoAbertoDepoisDoInicioDoOitavoDia() throws Exception {
        var c = criarCenario();
        LocalDate hoje = RelogioSistema.hoje(), data = hoje.minusDays(6), amanha = hoje.plusDays(1);
        String statusCompativel = data.getDayOfWeek().getValue() >= 6 ? "DESCANSO" : "FALTA";
        var criada = enviar(c.operacionalA, "/api/frequencias", mapa("vinculoContratualId", c.vinculoA.getId(),
                "dataReferencia", data, "status", statusCompativel), 200, "Frequência operacional dentro do sétimo dia");
        String rota = "/api/frequencias?data=" + data + "&contratoId=" + c.contratoA.getId();
        limparCaches();
        var primeira = consultar(c.operacionalA, rota, "Consulta preenche o cache paginado com estado aberto");
        assertEquals("ABERTA", primeira.path("content").get(0).path("estadoNegocial").asText());
        ZonedDateTime depoisDaVirada = amanha.atStartOfDay(RelogioSistema.ZONA).plusSeconds(1);
        try (var calendario = Mockito.mockStatic(RelogioSistema.class, Mockito.CALLS_REAL_METHODS);
             var instante = Mockito.mockStatic(ZonedDateTime.class, Mockito.CALLS_REAL_METHODS)) {
            calendario.when(RelogioSistema::hoje).thenReturn(amanha);
            instante.when(() -> ZonedDateTime.now(RelogioSistema.ZONA)).thenReturn(depoisDaVirada);
            assertTrue(politica.fechada(data, null), "Controle: regra vigente já considera o oitavo dia fechado");
            var atual = consultar(c.operacionalA, rota, "FRE-003 cache acompanha fechamento derivado da passagem do tempo");
            assertEquals("FECHADA", atual.path("content").get(0).path("estadoNegocial").asText());
            assertEquals(criada.path("id").asLong(), atual.path("content").get(0).path("id").asLong());
        }
        var fonte = em.find(com.example.siggtrat.entidade.Frequencia.class, criada.path("id").asLong());
        assertEquals(com.example.siggtrat.dominio.frequencia.EstadoNegocialFrequencia.ABERTA, fonte.getEstadoNegocial(),
                "Consulta não materializa fato negocial fictício ao derivar fechamento");
    }

    @ParameterizedTest
    @ValueSource(strings = {"contrato", "empregado"})
    void cacheNaoConservaAcessoDaOrigemQuandoTransferenciaProgramadaSeTornaEfetiva(String recurso) throws Exception {
        var c = criarCenario();
        var n = publicado(c, 5, null, false);
        LocalDate hoje = RelogioSistema.hoje(), data = hoje.plusDays(2);
        var v = enviar(c.localA, "/api/vinculos-contratuais", mapa(
                "pessoa", mapa("nome", "Pessoa para cache temporal", "cpf", cpf(Math.toIntExact(960000000L + n.id()))),
                "contratoId", n.id(), "composicaoId", n.composicao1(), "dataInicio", hoje),
                201, "Pessoa canônica para testar escopo após transferência");
        long empregadoId = em.createQuery("select e.id from Empregado e where e.pessoaCanonica.id=:p", Long.class)
                .setParameter("p", v.path("pessoaId").asLong()).getSingleResult();
        conceder(c, n.id(), "TRANSFERENCIA_GESTAO_LOCAL");
        enviar(c.localA, base(n.id()) + "/transferir-gestao", mapa(
                "dataEfetiva", data, "dataAto", hoje, "gestaoLocalDestinoId", c.gestaoB.getId(), "tipoContratacaoDestinoId", c.tipoB.getId(),
                "motivo", "Transferência programada deve revogar acesso na virada do dia",
                "referenciaSei", "SEI-CACHE-TEMPORAL", "autorizacaoAdministrativa", "ATO-HTTP",
                "mapeamentos", List.of(mapa("composicaoOrigemId", n.composicao1(), "lotacaoDestinoId", c.lotacaoB.getId(),
                        "jornadaDestinoId", c.jornadaB.getId(), "cargoDestinoId", c.cargoB.getId()))),
                200, "Transferência real com efeito futuro");
        String path = recurso.equals("contrato") ? base(n.id()) : "/api/empregados/" + empregadoId;
        limparCaches();
        executar(c.localA, get(path), 200, "Origem pode consultar antes do efeito");
        var hit = executar(c.localA, get(path), 200, "Controle: cache HTTP realmente habilitado");
        assertEquals("HIT", hit.getResponse().getHeader("X-SGT-Cache"));

        try (var clock = Mockito.mockStatic(RelogioSistema.class, Mockito.CALLS_REAL_METHODS)) {
            clock.when(RelogioSistema::hoje).thenReturn(data);
            executar(c.localA, get(path).header("Cache-Control", "no-cache"), 404,
                    "Controle: autorização do serviço já recusa a antiga Gestão na data efetiva");
            var destino = dados(executar(c.localB, get(path), 200, "Destino legítimo mantém consulta"));
            assertEquals(c.gestaoB.getId().longValue(), destino.path("gestaoLocalId").asLong());
            executar(c.localA, get(path), 404,
                    "PER-001/CON-009 cache não pode restaurar acesso já retirado pela data do fato");
        }
    }
}
