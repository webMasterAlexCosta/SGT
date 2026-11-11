package com.example.siggtrat.auditoria20261029;

import com.example.siggtrat.entidade.*;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import javax.sql.DataSource;
import java.time.LocalDate;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

/**
 * Auditor independente, candidato2 SHA-256 do manifesto
 * 54af0c36b2ca034453dd0ee79da9f390e0cc92f1446947c294681449263f3aac.
 * C2-PES-01: a autorização temporal de nome-historico deve valer também na coleção.
 * Sem mocks de domínio/persistência; cada comando HTTP confirma sua transação.
 * Preparação JPA declara explicitamente duas faixas sintéticas, sem simular que
 * a projeção atual prova a existência passada. As provas nominais usam a API.
 * Não executado pelo revisor; a raiz executará PostgreSQL 18.6 + Redis reais em série.
 */
@Transactional(propagation = Propagation.NOT_SUPPORTED)
@TestPropertySource(properties = {"spring.cache.type=redis", "spring.sql.init.mode=never",
        "app.cache.redis.http-get.enabled=true"})
class Candidato2ProvasNomeEscopoTemporalHttpIT extends ContratosHttpBase {
    @Autowired private DataSource dataSource;
    @Autowired private RedisConnectionFactory redis;
    @Autowired private Environment ambiente;

    @BeforeEach
    void exigePersistenciaReal() throws Exception {
        try (var c = dataSource.getConnection()) {
            assertThat(c.getMetaData().getDatabaseProductName()).isEqualTo("PostgreSQL");
            assertThat(c.getMetaData().getDatabaseProductVersion()).matches("18\\.6(?: \\(Debian [^()]+\\))?");
        }
        assertThat(ambiente.getProperty("spring.jpa.hibernate.ddl-auto")).isEqualTo("update");
        assertThat(ambiente.getProperty("spring.sql.init.mode")).isEqualTo("never");
        assertThat(ambiente.getProperty("spring.cache.type")).isEqualTo("redis");
        try (var c = redis.getConnection()) { assertThat(c.ping()).isEqualTo("PONG"); }
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void listagemNaoExpoeProvaDeNomeDoPeriodoForaDoEscopoOperacional(boolean filtrarVinculo) throws Exception {
        Cenario c = criarCenarioPersistido();
        prepararDuasFaixasDocumentadas(c);
        conceder(c, c.contratoA.getId(), "INCORPORACAO_PROVA_HISTORICA");
        String nomePermitido = "Nome documentado na lotação autorizada";
        String nomeExterno = "Nome reservado ao período na outra lotação";
        JsonNode permitida = incorporar(c, nomePermitido, DATA.minusDays(2), DATA.minusDays(1));
        JsonNode externa = incorporar(c, nomeExterno, DATA, DATA.plusDays(1));

        // Controle: existem ambas as fontes, legitimamente conhecidas pela Gestão.
        JsonNode gestor = consultar(c.localA, rota(c), "Gestor Local conserva as duas provas documentadas");
        assertThat(gestor).hasSize(2);
        assertThat(gestor.toString()).contains(nomePermitido, nomeExterno);
        JsonNode pontual = dados(executar(c.operacionalA, get(nomeRota(c))
                        .param("vinculoId", c.vinculoA.getId().toString()).param("data", DATA.minusDays(1).toString()),
                200, "Operacional consulta o nome somente no período de sua Lotação/Turno"));
        assertThat(pontual.path("nome").asText()).isEqualTo(nomePermitido);
        executar(c.operacionalA, get(nomeRota(c)).param("vinculoId", c.vinculoA.getId().toString())
                        .param("data", DATA.toString()), 404,
                "O período na segunda Lotação está fora do escopo do mesmo Operacional");

        var pedido = get(rota(c));
        if (filtrarVinculo) pedido.param("vinculoId", c.vinculoA.getId().toString());
        JsonNode lista = dados(executar(c.operacionalA, pedido, 200,
                "Coleção de provas nominais deve aplicar o mesmo recorte temporal do detalhe"));
        assertThat(lista.toString()).as("A coleção não pode contornar o 404 do detalhe temporal")
                .doesNotContain(nomeExterno);
        assertThat(lista).hasSize(1);
        assertThat(lista.get(0).path("id").asLong()).isEqualTo(permitida.path("id").asLong());
        assertThat(lista.get(0).path("id").asLong()).isNotEqualTo(externa.path("id").asLong());
        assertThat(lista.get(0).path("nomeComprovado").asText()).isEqualTo(nomePermitido);

        assertThat(emTransacao(() -> em.createQuery(
                "select count(p) from ProvaNomePessoaHistorico p where p.vinculo.id=:v", Long.class)
                .setParameter("v", c.vinculoA.getId()).getSingleResult()))
                .as("Filtrar a leitura não remove as fontes preservadas").isEqualTo(2L);
    }

    private void prepararDuasFaixasDocumentadas(Cenario c) {
        emTransacao(() -> {
            GestaoLocal gestao = em.find(GestaoLocal.class, c.gestaoA.getId());
            Jornada jornada = em.find(Jornada.class, c.jornadaA.getId());
            Lotacao outra = new Lotacao("Lotação sintética externa " + UUID.randomUUID());
            outra.setGestaoLocal(gestao);
            outra.setMunicipio("Município da segunda fonte sintética");
            outra.getTurnos().add(jornada);
            em.persist(outra);
            em.find(ComposicaoContratual.class, c.composicaoA.getId()).setEfetivoPrevisto(2);
            ComposicaoContratual comp = composicao(em.find(Contrato.class, c.contratoA.getId()),
                    outra, jornada, em.find(Cargo.class, c.cargoA.getId()), 3);
            AlocacaoVinculo primeira = em.find(AlocacaoVinculo.class, c.alocacaoA.getId());
            primeira.setDataFim(DATA.minusDays(1));
            AlocacaoVinculo segunda = new AlocacaoVinculo();
            segunda.setVinculo(em.find(VinculoContratual.class, c.vinculoA.getId()));
            segunda.setComposicao(comp);
            segunda.setLotacao(outra);
            segunda.setJornada(jornada);
            segunda.setCargo(em.find(Cargo.class, c.cargoA.getId()));
            segunda.setDataInicio(DATA);
            segunda.setDataFim(FIM);
            segunda.setCriadoPorId(c.localA.getId());
            segunda.setCriadoPorUsername(c.localA.getUsername());
            segunda.aplicarFotografiaDocumentada(outra.getNome(), jornada.getDescricao(),
                    segunda.getCargo().getNome(), outra.getMunicipio(),
                    com.example.siggtrat.utilitario.FotografiaJornada.capturar(jornada));
            em.persist(segunda);
            em.flush();
            assertThat(em.createQuery("select count(a) from AlocacaoVinculo a where a.vinculo.id=:v", Long.class)
                    .setParameter("v", c.vinculoA.getId()).getSingleResult()).isEqualTo(2L);
            return null;
        });
    }

    private JsonNode incorporar(Cenario c, String nome, LocalDate inicio, LocalDate fim) throws Exception {
        Map<String,Object> req = mapa("vinculoId", c.vinculoA.getId(), "nomeComprovado", nome,
                "dataInicioEfetiva", inicio, "dataFimEfetiva", fim,
                "referenciaSei", "SEI-SINTETICO-C2-PES-01-" + UUID.randomUUID(),
                "motivo", "Fonte sintética explícita comprova identidade somente no intervalo declarado",
                "autorizacao", "ATO-SINTETICO-C2-PES-01", "chaveIdempotencia", UUID.randomUUID().toString(),
                "versaoPessoaEsperada", consultar(c.localA, "/api/pessoas/" + c.pessoaA.getId(),
                        "Versão corrente da Pessoa").path("version").asLong(),
                "versaoVinculoEsperada", consultar(c.localA, "/api/vinculos-contratuais/" + c.vinculoA.getId(),
                        "Versão corrente do Vínculo").path("version").asLong());
        JsonNode previa = enviar(c.localA, rota(c) + "/previa", req, 200,
                "Prévia da fonte nominal explícita no vínculo canônico");
        req.put("confirmarImpacto", true);
        req.put("tokenPrevia", previa.path("tokenPrevia").asText());
        return enviar(c.localA, rota(c), req, 201, "Incorporação nominal autorizada, com commit real");
    }

    private String rota(Cenario c) { return "/api/pessoas/" + c.pessoaA.getId() + "/provas-nome"; }
    private String nomeRota(Cenario c) { return "/api/pessoas/" + c.pessoaA.getId() + "/nome-historico"; }
}
