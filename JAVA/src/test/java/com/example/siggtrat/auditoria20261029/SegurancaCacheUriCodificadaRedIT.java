package com.example.siggtrat.auditoria20261029;

import com.example.siggtrat.entidade.Cargo;
import com.example.siggtrat.servico.RevisaoFontesPainelServico;
import com.example.siggtrat.utilitario.RelogioSistema;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.CacheManager;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.method.HandlerMethod;

import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;

/**
 * SEG-001 candidato: exclusão de cache por URI bruta versus rota MVC decodificada.
 * Usa PostgreSQL/Redis reais, JWT e filtros reais. Nenhum relógio, banco, Redis ou
 * serviço de autorização é substituído. A concessão vence pelo tempo real.
 * Uma recusa do firewall na primeira URI codificada descarta o caminho proposto;
 * não deve ser contada como reprodução da exposição.
 */
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class SegurancaCacheUriCodificadaRedIT extends ContratosHttpBase {
    @Autowired CacheManager caches;
    @Autowired RedisConnectionFactory redis;
    @Autowired RevisaoFontesPainelServico revisoes;

    @DynamicPropertySource
    static void redisReal(DynamicPropertyRegistry p) {
        p.add("spring.cache.type", () -> "redis");
        p.add("spring.data.redis.host", () -> System.getenv().getOrDefault("REDIS_HOST", "127.0.0.1"));
        p.add("spring.data.redis.port", () -> System.getenv().getOrDefault("REDIS_PORT", "26379"));
        p.add("spring.data.redis.password", () -> System.getenv().getOrDefault("REDIS_PASSWORD", ""));
        p.add("spring.data.redis.database", () -> System.getenv().getOrDefault("REDIS_DATABASE", "0"));
        p.add("app.cache.redis.enabled", () -> "true");
        p.add("app.cache.redis.http-get.enabled", () -> "true");
        p.add("app.cache.redis.ttl.http-get", () -> "45s");
    }

    @AfterEach
    void limparSomenteCacheHttpDoCenarioSintetico() {
        var cache = caches.getCache(com.example.siggtrat.configuracao.NomesCache.API_GET_RESPONSES);
        if (cache != null) cache.clear();
    }

    @Test
    @Timeout(value = 60, unit = TimeUnit.SECONDS)
    void uriCodificadaNaoPodeDispensarAuditoriaNemConcessaoVigente() throws Exception {
        assertInstanceOf(RedisCacheManager.class, caches, "A prova depende do cache Redis real.");
        try (var conexao = redis.getConnection()) { assertEquals("PONG", conexao.ping()); }
        Cenario c = criarCenarioPersistido();
        NovoContrato n = emTransacao(() -> {
            try { return publicado(c, 5, null, false); }
            catch (Exception erro) { throw new IllegalStateException(erro); }
        });
        long publicacao = consultar(c.localA, base(n.id()) + "/eventos", "Fonte publicada da prova de cache")
                .get(0).path("id").asLong();
        String marcador = "CONTEUDO_PESSOAL_SINTETICO_" + UUID.randomUUID();
        JsonNode criado = dados(executar(c.localA, multipart(base(n.id()) + "/documentos")
                .file(metadados(mapa("tipoFato", "CONTRATO_ORIGINAL", "fatoId", publicacao,
                        "fatoChave", "ORIGINAL", "titulo", marcador,
                        "descricao", "Narrativa pessoal sintética protegida por concessão temporária",
                        "referenciaSei", "SEI-SEG-CACHE-" + UUID.randomUUID(),
                        "classificacaoAcesso", "PESSOAL_RESTRITO", "dataEfetiva", n.inicio(),
                        "chaveIdempotencia", "seg-cache-" + UUID.randomUUID()))),
                201, "Fonte pessoal restrita com metadados identificáveis"));
        long documentoId = criado.path("id").asLong();
        String canonica = base(n.id()) + "/documentos/" + documentoId;
        String codificada = base(n.id()) + "/doc%75mentos/" + documentoId;
        executar(c.nacional, get(canonica).param("finalidade", "auditoria"), 403,
                "Controle: documento restrito não é legível sem concessão");
        Instant expira = Instant.now().plusSeconds(8);
        enviar(c.admin, canonica + "/permissoes", mapa("usuarioId", c.nacional.getId(),
                "finalidade", "auditoria", "justificativa", "Prova sintética de expiração do cache",
                "autorizacaoAdministrativa", "ATO-SEG-CACHE", "validoAte", expira), 200,
                "Concessão real de curta duração; o teste não a edita nem revoga");

        var primeira = executar(c.nacional, get(URI.create(codificada)).param("finalidade", "auditoria"),
                200, "Verificar alcance: o firewall e o MVC aceitam a URI com letra codificada");
        assertEquals(codificada, primeira.getRequest().getRequestURI(), "A codificação deve chegar intacta ao filtro.");
        assertInstanceOf(HandlerMethod.class, primeira.getHandler(), "A primeira resposta deve vir de um controlador MVC.");
        assertEquals("buscar", ((HandlerMethod) primeira.getHandler()).getMethod().getName());
        assertEquals("DocumentoComprobatorioControlador",
                ((HandlerMethod) primeira.getHandler()).getBeanType().getSimpleName());
        assertEquals(marcador, dados(primeira).path("titulo").asText());
        assertTrue(Instant.now().isBefore(expira), "Primeiro acesso precisa ocorrer ainda dentro da concessão.");

        var segunda = executar(c.nacional, get(URI.create(codificada)).param("finalidade", "auditoria"),
                200, "Repetição autorizada ainda exige registro de novo acesso pessoal");
        assertEquals(marcador, dados(segunda).path("titulo").asText());
        long acessosAposDuasLeituras = emTransacao(() -> em.createQuery(
                        "select count(a) from AcessoDocumentoRestrito a where a.documento.id=:d and a.usuarioId=:u", Long.class)
                .setParameter("d", documentoId).setParameter("u", c.nacional.getId()).getSingleResult());
        while (Instant.now().isBefore(expira.plusMillis(150))) TimeUnit.MILLISECONDS.sleep(50);

        var negadaCanonica = executar(c.nacional, get(canonica).param("finalidade", "auditoria"), 403,
                "Controle: a mesma concessão já vencida é recusada no caminho canônico");
        var aposExpiracao = mvc.perform(autenticar(get(URI.create(codificada))
                .param("finalidade", "auditoria"), c.nacional)).andReturn();
        String conteudoApos = aposExpiracao.getResponse().getContentAsString();
        Path evidencia = Path.of("target/auditoria-http/seguranca-cache-uri-codificada.json");
        Files.createDirectories(evidencia.getParent());
        Files.writeString(evidencia, mapper.writerWithDefaultPrettyPrinter().writeValueAsString(mapa(
                "uriBruta", codificada, "handlerPrimeiraLeitura", primeira.getHandler().toString(),
                "primeiraStatus", primeira.getResponse().getStatus(),
                "primeiraCache", primeira.getResponse().getHeader("X-SGT-Cache"),
                "segundaStatus", segunda.getResponse().getStatus(),
                "segundaCache", segunda.getResponse().getHeader("X-SGT-Cache"),
                "acessosAuditadosAposDuasLeituras", acessosAposDuasLeituras,
                "concessaoValidaAte", expira.toString(), "leituraDepoisEm", Instant.now().toString(),
                "canonicaDepoisStatus", negadaCanonica.getResponse().getStatus(),
                "codificadaDepoisStatus", aposExpiracao.getResponse().getStatus(),
                "codificadaDepoisCache", aposExpiracao.getResponse().getHeader("X-SGT-Cache"),
                "conservaConteudoPessoal", conteudoApos.contains(marcador))));
        assertAll(
                () -> assertEquals(2L, acessosAposDuasLeituras, "DOC-010: cada leitura pessoal deve ser auditada."),
                () -> assertNotEquals("HIT", segunda.getResponse().getHeader("X-SGT-Cache"),
                        "Uma grafia equivalente da rota não deve pular a política de não armazenar dados restritos."),
                () -> assertEquals(403, aposExpiracao.getResponse().getStatus(),
                        "DOC-010: mesma sessão e mesma URI não podem recuperar conteúdo após vencer a concessão."),
                () -> assertFalse(conteudoApos.contains(marcador),
                        "O corpo recusado não pode conter os metadados pessoais armazenados na primeira resposta."));
    }

    @Test
    void transicaoDeCatalogoConfirmadaDeveInvalidarCacheDaProjecaoEDaDisponibilidade() throws Exception {
        assertInstanceOf(RedisCacheManager.class, caches);
        try (var conexao = redis.getConnection()) { assertEquals("PONG", conexao.ping()); }
        Cenario c = criarCenarioPersistido();
        JsonNode novo = enviar(c.localA, "/api/cargos", mapa("nome", "Cargo SEG-cache " + UUID.randomUUID(),
                "ativo", true), 200, "Cargo próprio novo, sem fatos dependentes");
        long cargoId = novo.path("id").asLong();
        String cadastro = "/api/cargos/" + cargoId;
        String disponibilidade = "/api/catalogos-temporais/CARGO/" + cargoId
                + "/disponibilidade?data=" + RelogioSistema.hoje();
        var primeira = executar(c.localA, get(cadastro), 200, "Aquecer cadastro no Redis");
        var repetida = executar(c.localA, get(cadastro), 200, "Controle positivo de HIT antes da transição");
        assertEquals("HIT", repetida.getResponse().getHeader("X-SGT-Cache"));
        assertTrue(dados(primeira).path("ativo").asBoolean());
        executar(c.localA, get(disponibilidade), 200, "Aquecer disponibilidade no Redis");
        assertEquals("HIT", executar(c.localA, get(disponibilidade), 200,
                "Controle de HIT na disponibilidade").getResponse().getHeader("X-SGT-Cache"));
        String revisaoAntes = revisoes.atual();
        var recibo = enviar(c.localA, "/api/catalogos-temporais/CARGO/" + cargoId + "/transicoes", mapa(
                "dataEfetiva", RelogioSistema.hoje(), "ativo", false,
                "versaoEsperada", dados(primeira).path("version").asLong(),
                "motivo", "Inativação administrativa para prova do cache",
                "evidencia", "SEI-SEG-CACHE-CATALOGO", "autorizacao", "ATO-SEG-CACHE-CATALOGO",
                "chaveIdempotencia", UUID.randomUUID().toString()), 200,
                "Transição canônica confirmada precisa atualizar os consumidores");
        assertFalse(recibo.path("ativoAtualAposRegistro").asBoolean());
        boolean persistido = emTransacao(() -> Boolean.TRUE.equals(em.find(Cargo.class, cargoId).getAtivo()));
        assertFalse(persistido, "Controle: nova transação confirma que o PostgreSQL mudou.");
        var depoisCadastro = executar(c.localA, get(cadastro), 200, "Cadastro imediatamente após commit");
        var depoisDisponibilidade = executar(c.localA, get(disponibilidade), 200,
                "Disponibilidade imediatamente após commit");
        String revisaoDepois = revisoes.atual();
        var fresca = executar(c.localA, get(cadastro).header("Cache-Control", "no-cache"), 200,
                "Controle de origem: solicitar leitura fresca encontra estado novo");
        assertFalse(dados(fresca).path("ativo").asBoolean());
        Path evidencia = Path.of("target/auditoria-http/seguranca-transicao-catalogo-cache.json");
        Files.createDirectories(evidencia.getParent());
        Files.writeString(evidencia, mapper.writerWithDefaultPrettyPrinter().writeValueAsString(mapa(
                "cargoId", cargoId, "persistidoAtivo", persistido,
                "recibo", recibo, "revisaoAntes", revisaoAntes, "revisaoDepois", revisaoDepois,
                "cadastroCache", depoisCadastro.getResponse().getHeader("X-SGT-Cache"),
                "cadastroAtivo", dados(depoisCadastro).path("ativo").asBoolean(),
                "disponibilidadeCache", depoisDisponibilidade.getResponse().getHeader("X-SGT-Cache"),
                "disponibilidadeAtivo", dados(depoisDisponibilidade).path("ativo").asBoolean(),
                "origemFrescaAtivo", dados(fresca).path("ativo").asBoolean())));
        assertAll(
                () -> assertFalse(dados(depoisCadastro).path("ativo").asBoolean(),
                        "O GET não pode anunciar ativo=true depois do commit que devolveu ativo=false."),
                () -> assertFalse(dados(depoisDisponibilidade).path("ativo").asBoolean(),
                        "O consumidor temporal deve refletir o novo ato efetivo na mesma data consultada."),
                () -> assertNotEquals(revisaoAntes, revisaoDepois,
                        "Toda mutação negocial deve alterar a revisão usada pelas chaves de cache."));
    }
}
