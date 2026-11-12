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

/** Cadeia MVC/autorização/PostgreSQL real, cache HTTP habilitado com armazenamento em memória. */
@Import(CacheEscopoTemporalHttpIT.CacheMemoria.class)
class CacheEscopoTemporalHttpIT extends ContratosHttpBase {
    @TestConfiguration
    static class CacheMemoria {
        @Bean @Primary CacheManager cacheAuditoria() {
            return new ConcurrentMapCacheManager() {
                @Override protected org.springframework.cache.Cache createConcurrentMapCache(String nome) {
                    return new CacheObservadoB06(nome);
                }
            };
        }
    }

    static class CacheObservadoB06 extends org.springframework.cache.concurrent.ConcurrentMapCache {
        int carregamentos, acertos;
        CacheObservadoB06(String nome) { super(nome); }
        @Override public <T> T get(Object chave, java.util.concurrent.Callable<T> loader) {
            boolean presente = lookup(chave) != null;
            T valor = super.get(chave, () -> { carregamentos++; return loader.call(); });
            if (presente) acertos++;
            return valor;
        }
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
    void cachePreservaEdicaoAindaAbertaDepoisDoPrazoDeLancamento() throws Exception {
        var c = criarCenario();
        LocalDate hoje = RelogioSistema.hoje(), data = hoje.minusDays(politica.diasJanela() - 1L), amanha = hoje.plusDays(1);
        String statusCompativel = data.getDayOfWeek().getValue() >= 6 ? "DESCANSO" : "FALTA";
        var criada = enviar(c.operacionalA, "/api/frequencias", mapa("vinculoContratualId", c.vinculoA.getId(),
                "dataReferencia", data, "status", statusCompativel), 200, "Frequência operacional no último dia da janela vigente");
        String rota = "/api/frequencias?data=" + data + "&contratoId=" + c.contratoA.getId();
        limparCaches();
        var primeira = consultar(c.operacionalA, rota, "Consulta preenche o cache paginado com estado aberto");
        assertEquals("ABERTA", primeira.path("content").get(0).path("estadoNegocial").asText());
        CacheObservadoB06 cache = (CacheObservadoB06) caches.getCache(com.example.siggtrat.configuracao.NomesCache.FREQUENCIAS_CONSULTA);
        assertNotNull(cache);
        var chavesAntes = java.util.Set.copyOf(cache.getNativeCache().keySet());
        assertEquals(1, chavesAntes.size(), "FRE-013:002[0] esta rota materializou exatamente uma página no cache interno");
        Object chaveAntiga = chavesAntes.iterator().next();
        var paginaAntiga = (com.example.siggtrat.dto.PaginaCacheDTO<?>) cache.get(chaveAntiga).get();
        assertEquals(criada.path("id").asLong(), ((com.example.siggtrat.dto.FrequenciaDTO) paginaAntiga.content().get(0)).getId().longValue());
        int cargasAntes = cache.carregamentos, acertosAntes = cache.acertos;
        var repetida = consultar(c.operacionalA, rota, "FRE013 controle de HIT da mesma rota e usuário antes da fronteira");
        assertEquals(primeira, repetida);
        assertEquals(cargasAntes, cache.carregamentos, "HIT não executa novamente o loader de consulta");
        assertEquals(acertosAntes + 1, cache.acertos);
        assertSame(paginaAntiga, cache.get(chaveAntiga).get());
        assertEquals(data.plusDays(politica.diasJanela()).atStartOfDay(RelogioSistema.ZONA), politica.instanteFechamento(data));
        ZonedDateTime depoisDaVirada = amanha.atStartOfDay(RelogioSistema.ZONA).plusSeconds(1);
        try (var calendario = Mockito.mockStatic(RelogioSistema.class, Mockito.CALLS_REAL_METHODS);
             var instante = Mockito.mockStatic(ZonedDateTime.class, Mockito.CALLS_REAL_METHODS)) {
            calendario.when(RelogioSistema::hoje).thenReturn(amanha);
            instante.when(() -> ZonedDateTime.now(RelogioSistema.ZONA)).thenReturn(depoisDaVirada);
            assertTrue(politica.fechada(data, null), "Controle: regra vigente já considera a fronteira configurada fechada");
            var atual = consultar(c.operacionalA, rota, "Cache respeita a independência entre lançamento e edição");
            assertEquals("ABERTA", atual.path("content").get(0).path("estadoNegocial").asText(),
                    "O fim do lançamento não encerra as 72 horas independentes de edição");
            assertEquals(criada.path("id").asLong(), atual.path("content").get(0).path("id").asLong());
            var novas = new java.util.HashSet<>(cache.getNativeCache().keySet()); novas.removeAll(chavesAntes);
            assertEquals(1, novas.size(), "Nova data negocial usa uma entrada distinta");
            var paginaAtual = (com.example.siggtrat.dto.PaginaCacheDTO<?>) cache.get(novas.iterator().next()).get();
            var linhaAtual = (com.example.siggtrat.dto.FrequenciaDTO) paginaAtual.content().get(0);
            assertEquals(criada.path("id").asLong(), linhaAtual.getId().longValue());
            assertEquals("ABERTA", linhaAtual.getEstadoNegocial().name());
            assertEquals(cargasAntes + 1, cache.carregamentos);
            int acertosNaVirada = cache.acertos;
            var repetidaFechada = consultar(c.operacionalA, rota, "FRE013 HIT da entrada vigente após a fronteira");
            assertEquals("ABERTA", repetidaFechada.path("content").get(0).path("estadoNegocial").asText());
            assertEquals(acertosNaVirada + 1, cache.acertos);
            assertEquals(cargasAntes + 1, cache.carregamentos);
        }
        var fonte = em.find(com.example.siggtrat.entidade.Frequencia.class, criada.path("id").asLong());
        assertEquals(com.example.siggtrat.dominio.frequencia.EstadoNegocialFrequencia.ABERTA, fonte.getEstadoNegocial(),
                "Consulta não materializa fato negocial fictício ao derivar fechamento");
    }

    @ParameterizedTest
    @ValueSource(strings = {"dia", "intervalo", "mensal"})
    void cacheDerivaFechamentoNoInstanteExatoDoPrazoDeEdicao(String consulta) throws Exception {
        var c = criarCenario();
        LocalDate data = RelogioSistema.hoje();
        String status = data.getDayOfWeek().getValue() >= 6 ? "DESCANSO" : "FALTA";
        var criada = enviar(c.operacionalA, "/api/frequencias", mapa("vinculoContratualId", c.vinculoA.getId(),
                "dataReferencia", data, "status", status), 200, "Primeiro lançamento fixa prazo de edição");
        var prazo = java.time.LocalDateTime.parse(criada.path("prazoEdicao").asText())
                .atZone(RelogioSistema.ZONA).toInstant();
        String rota = switch (consulta) {
            case "intervalo" -> "/api/frequencias/intervalo?inicio=" + data + "&fim=" + data;
            case "mensal" -> "/api/frequencias/mensal?mes=" + data.getMonthValue() + "&ano=" + data.getYear();
            default -> "/api/frequencias?data=" + data;
        };
        rota += "&contratoId=" + c.contratoA.getId();
        limparCaches();
        // Mantém o mesmo dia na chave para isolar uma fronteira de horas/minutos dentro do dia.
        try (var instante = Mockito.mockStatic(java.time.Instant.class, Mockito.CALLS_REAL_METHODS)) {
            instante.when(java.time.Instant::now).thenReturn(prazo.minusSeconds(1));
            var antes = consultar(c.operacionalA, rota, "Consulta antes do limite exclusivo");
            assertEquals("ABERTA", antes.path("content").get(0).path("estadoNegocial").asText());
            var cache = (CacheObservadoB06) caches.getCache(com.example.siggtrat.configuracao.NomesCache.FREQUENCIAS_CONSULTA);
            assertNotNull(cache);
            int cargas = cache.carregamentos, acertos = cache.acertos;
            var chave = cache.getNativeCache().keySet().iterator().next();
            var pagina = (com.example.siggtrat.dto.PaginaCacheDTO<?>) cache.get(chave).get();
            instante.when(java.time.Instant::now).thenReturn(prazo);
            var depois = consultar(c.operacionalA, rota, "HIT deve derivar fechamento exato sem esperar o TTL");
            assertEquals("FECHADA", depois.path("content").get(0).path("estadoNegocial").asText());
            assertEquals(cargas, cache.carregamentos, "A projeção temporal preserva o cache de dados");
            assertEquals(acertos + 1, cache.acertos, "A consulta atravessou um HIT real");
            assertSame(pagina, cache.get(chave).get());
            assertEquals("ABERTA", ((com.example.siggtrat.dto.FrequenciaDTO) pagina.content().get(0))
                    .getEstadoNegocial().name(), "A leitura não altera o DTO compartilhado no cache");
        }
        assertEquals(com.example.siggtrat.dominio.frequencia.EstadoNegocialFrequencia.ABERTA,
                em.find(com.example.siggtrat.entidade.Frequencia.class, criada.path("id").asLong()).getEstadoNegocial(),
                "A consulta não materializa fechamento no banco");
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
        enviar(c.localA, base(n.id()) + "/transferir-gestao", mapa(/* Conferência SINTÉTICA explícita desta fixture; não é padrão do produto. */
                "conferenciaDocumentalConfirmada", true, "versaoConferenciaDocumental", "transferencia-gestao-conferencia-v1", 
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
