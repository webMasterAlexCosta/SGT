package com.example.siggtrat.auditoria20261029;

import com.example.siggtrat.entidade.*;
import com.example.siggtrat.utilitario.RelogioSistema;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.CacheManager;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;

/**
 * Proposta RED; não executada pelo revisor. TMP-004/HIS-002/HIS-004/HIS-005.
 * Os fatos são retroativos; o instante da correção do cadastro não prova
 * vigência jurídica do novo nome. Sem prova, null ou 409 atômico são seguros.
 */
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class TransversaisIdentidadeRetroativaRedIT extends ContratosHttpBase {
    @Autowired RedisConnectionFactory redis;
    @Autowired CacheManager caches;

    @DynamicPropertySource
    static void redisReal(DynamicPropertyRegistry p) {
        p.add("spring.cache.type", () -> "redis");
        p.add("spring.data.redis.host", () -> System.getenv().getOrDefault("REDIS_HOST", "127.0.0.1"));
        p.add("spring.data.redis.port", () -> System.getenv().getOrDefault("REDIS_PORT", "6379"));
        p.add("spring.data.redis.password", () -> System.getenv().getOrDefault("REDIS_PASSWORD", ""));
        p.add("spring.data.redis.database", () -> System.getenv().getOrDefault("REDIS_DATABASE", "0"));
        p.add("app.cache.redis.http-get.enabled", () -> "true");
    }

    @BeforeEach
    void infraestruturaReal() {
        assertInstanceOf(RedisCacheManager.class, caches);
        try (var conexao = redis.getConnection()) { assertEquals("PONG", conexao.ping()); }
    }

    @ParameterizedTest(name = "{0}: fonte nominal anterior={1}")
    @CsvSource({"OCORRENCIA,true", "OCORRENCIA,false", "FREQUENCIA,true", "FREQUENCIA,false"})
    void novoFatoRetroativoNaoFotografaNomeAtualComoNomeDaData(String recurso, boolean comFonte) throws Exception {
        assertTrue(DATA.isBefore(RelogioSistema.hoje()), "O oráculo exige um dia efetivamente passado.");
        Cenario c = criarCenarioPersistido();
        String nomeComprovado = c.pessoaA.getNome();
        if (comFonte) declararFonteNominalSintetica(c, "transversal-nome-" + UUID.randomUUID());
        List<String> fontesAntes = fotografiasNominais(c);
        String nomeAtual = "Nome atual sem prova de vigencia passada " + UUID.randomUUID();

        executar(c.localA, post("/api/pessoas/{id}/correcoes-identidade", c.pessoaA.getId())
                        .contentType(APPLICATION_JSON).content(json(mapa("tipo", "NOME", "novoValor", nomeAtual,
                                "motivo", "Correção do cadastro atual; vigência no passado não foi declarada",
                                "evidencia", "SEI-CORRECAO-ATUAL", "autorizacao", "ATO-IDENTIDADE",
                                "impactosEncontrados", "Preservar a verdade histórica e as fontes originais"))),
                200, "Correção atual não declara data efetiva do novo nome");
        assertEquals(nomeAtual, emTransacao(() -> em.find(Pessoa.class, c.pessoaA.getId()).getNome()));
        Map<String, Long> quantidadesAntes = quantidades(c);

        var pedido = recurso.equals("OCORRENCIA")
                ? multipart("/api/ocorrencias").param("empregadoId", c.empregadoA.getId().toString())
                    .param("motivo", "FALTA").param("dataOcorrencia", DATA.toString()).param("dias", "1")
                    .param("chaveIdempotencia", "retroativo-nome-" + UUID.randomUUID())
                : post("/api/frequencias").contentType(APPLICATION_JSON).content(json(mapa(
                    "vinculoContratualId", c.vinculoA.getId(), "dataReferencia", DATA.toString(), "status", "FALTA",
                    "justificativaRetificacao", "Conferência documental de fato efetivo anterior à correção atual",
                    "chaveIdempotencia", "retroativo-nome-" + UUID.randomUUID())));
        MvcResult resposta = mvc.perform(autenticar(pedido, c.localA)).andReturn();
        int status = resposta.getResponse().getStatus();
        if (status == 409) {
            assertEquals(quantidadesAntes, quantidades(c), "Rejeitar por falta de fonte não deixa fatos parciais.");
        } else {
            assertEquals(recurso.equals("OCORRENCIA") ? 201 : 200, status,
                    "Só sucesso com fotografia histórica segura ou conflito atômico satisfaz o oráculo.");
            JsonNode fato = dados(resposta);
            long id = fato.path("id").asLong();
            assertTrue(id > 0);
            String fotoHttp = fato.path("empregado").path("nome").isNull()
                    || fato.path("empregado").path("nome").isMissingNode()
                    ? null : fato.path("empregado").path("nome").asText();
            assertFotoHistorica(fotoHttp, nomeAtual, nomeComprovado, comFonte);
            String fotoPersistida = emTransacao(() -> recurso.equals("OCORRENCIA")
                    ? em.find(Ocorrencia.class, id).getEmpregadoNomeFotografia()
                    : em.find(Frequencia.class, id).getEmpregadoSnapshotNome());
            assertFotoHistorica(fotoPersistida, nomeAtual, nomeComprovado, comFonte);
        }
        assertEquals(fontesAntes, fotografiasNominais(c), "O novo conhecimento não altera a prova original.");
        assertEquals(nomeAtual, emTransacao(() -> em.find(Pessoa.class, c.pessoaA.getId()).getNome()));
    }

    private void assertFotoHistorica(String foto, String nomeAtual, String nomeComprovado, boolean comFonte) {
        assertNotEquals(nomeAtual, foto,
                "Nome do cadastro atual foi apresentado como fotografia de uma data anterior sem fonte de vigência.");
        assertTrue(foto == null || foto.isBlank() || comFonte && Objects.equals(nomeComprovado, foto),
                "O fato deve trazer desconhecimento ou a identidade comprovada no recorte.");
    }

    private List<String> fotografiasNominais(Cenario c) {
        return emTransacao(() -> em.createQuery(
                        "select e from EventoVinculoContratual e where e.vinculo.id=:v order by e.id",
                        EventoVinculoContratual.class).setParameter("v", c.vinculoA.getId()).getResultList().stream()
                .map(e -> e.getId() + "|" + e.getPessoaNomeFotografia() + "|" + e.getDataEfetiva()
                        + "|" + e.getRegistradoEm() + "|" + e.getHashEvento()).toList());
    }

    private Map<String, Long> quantidades(Cenario c) {
        return emTransacao(() -> Map.of(
                "ocorrencias", em.createQuery("select count(o) from Ocorrencia o where o.empregado.id=:e", Long.class)
                        .setParameter("e", c.empregadoA.getId()).getSingleResult(),
                "frequencias", em.createQuery("select count(f) from Frequencia f where f.vinculoContratual.id=:v", Long.class)
                        .setParameter("v", c.vinculoA.getId()).getSingleResult(),
                "historicos", em.createQuery("select count(h) from HistoricoEmpregado h where h.empregado.id=:e", Long.class)
                        .setParameter("e", c.empregadoA.getId()).getSingleResult()));
    }
}
