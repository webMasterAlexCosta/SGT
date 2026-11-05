package com.example.siggtrat.auditoria20261029;

import com.example.siggtrat.entidade.EmissaoRelatorioAdministrativo;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;

/** Revisor transversal C4, hipótese C4-TR-H04. Proposta não executada pelo autor.
 * HTTP e persistência reais. A transação externa somente permite observar o
 * fato gerado antes do commit; não altera relógio, entidade, DTO ou banco.
 * As releituras ocorrem após commit em novas transações e não usam clear como
 * substituto de commit. Não basta truncar a apresentação JSON do instante. */
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class Candidato4EmissaoPrecisaoInstantesRedIT extends ContratosHttpBase {
    private static final String ROTA = "/api/relatorios/administrativos/emissoes";

    @Test
    void novaEmissaoESuperacaoConservamInstantesExatosEntreReciboEntidadeEReleitura() throws Exception {
        Cenario c = criarCenarioPersistido();
        Captura primeira = emitirECapturar(c, null);
        long primeiroId = primeira.resposta().path("metadados").path("id").asLong();
        Instantes primeiraPersistida = reler(primeiroId);
        JsonNode primeiroGet = consultar(c.localA, ROTA + "/" + primeiroId,
                "Controle: primeira emissão commitada, consultável e ainda vigente");

        Captura segunda = emitirECapturar(c, primeiroId);
        long segundoId = segunda.resposta().path("metadados").path("id").asLong();
        Instantes segundaPersistida = reler(segundoId);
        Instantes primeiraSuperadaPersistida = reler(primeiroId);
        JsonNode segundoGet = consultar(c.localA, ROTA + "/" + segundoId,
                "Segunda emissão relida após commit em transação nova");
        JsonNode primeiraSuperadaGet = consultar(c.localA, ROTA + "/" + primeiroId,
                "Primeira emissão superada conserva conteúdo e instantes originais");

        Path evidencia = Path.of("target/auditoria-http/candidato4-emissao-precisao-instantes.json");
        Files.createDirectories(evidencia.getParent());
        Files.writeString(evidencia, mapper.writerWithDefaultPrettyPrinter().writeValueAsString(mapa(
                "hipotese", "C4-TR-H04", "primeiroId", primeiroId, "segundoId", segundoId,
                "primeiroRecibo", primeira.resposta().path("metadados"),
                "primeiraEntidadeAntesCommit", primeira.nova(), "primeiraEntidadeAposCommit", primeiraPersistida,
                "primeiroGet", primeiroGet.path("metadados"),
                "segundoRecibo", segunda.resposta().path("metadados"),
                "segundaEntidadeAntesCommit", segunda.nova(), "segundaEntidadeAposCommit", segundaPersistida,
                "segundoGet", segundoGet.path("metadados"),
                "superacaoEntidadeAntesCommit", segunda.anterior(),
                "superacaoGetAntesCommit", segunda.getAnterior().path("metadados"),
                "superacaoEntidadeAposCommit", primeiraSuperadaPersistida,
                "superacaoGetAposCommit", primeiraSuperadaGet.path("metadados"))));

        assertAll("Instantes de fatos novos têm uma única representação persistível e estável",
                () -> assertThat(primeiroId).isPositive(),
                () -> assertThat(segundoId).isNotEqualTo(primeiroId),
                () -> assertThat(segundoGet.path("metadados").path("emissaoAnteriorId").asLong()).isEqualTo(primeiroId),
                () -> assertThat(primeiroGet.path("metadados").path("estado").asText()).isEqualTo("VIGENTE"),
                () -> assertThat(primeiraSuperadaGet.path("metadados").path("estado").asText()).isEqualTo("SUPERADA"),
                () -> assertThat(primeiraSuperadaPersistida.superadaEm()).isNotNull(),
                () -> assertThat(instantes(primeira.resposta())).as("Primeiro recibo representa o fato em memória").isEqualTo(primeira.nova()),
                () -> assertThat(primeira.nova()).as("Primeira entidade mantém instantes após commit real").isEqualTo(primeiraPersistida),
                () -> assertThat(instantes(primeiroGet)).as("Primeiro GET conserva exatamente o recibo").isEqualTo(instantes(primeira.resposta())),
                () -> assertThat(instantes(segunda.resposta())).as("Segundo recibo representa o fato em memória").isEqualTo(segunda.nova()),
                () -> assertThat(segunda.nova()).as("Segunda entidade mantém instantes após commit real").isEqualTo(segundaPersistida),
                () -> assertThat(instantes(segundoGet)).as("Segundo GET conserva exatamente o recibo").isEqualTo(instantes(segunda.resposta())),
                () -> assertThat(segunda.anterior()).as("Superação não muda de instante no commit").isEqualTo(primeiraSuperadaPersistida),
                () -> assertThat(instantes(segunda.getAnterior())).as("GET na transação representa o fato de superação").isEqualTo(segunda.anterior()),
                () -> assertThat(instantes(primeiraSuperadaGet)).as("GET após commit conserva o mesmo fato de superação").isEqualTo(instantes(segunda.getAnterior())),
                () -> assertThat(primeiraSuperadaGet.path("conteudo")).isEqualTo(primeira.resposta().path("conteudo")),
                () -> assertThat(primeiraSuperadaGet.path("metadados").path("hashConteudo")).isEqualTo(primeira.resposta().path("metadados").path("hashConteudo")));
    }

    private Captura emitirECapturar(Cenario c, Long anteriorId) {
        return emTransacao(() -> {
            try {
                JsonNode resposta = enviar(c.localA, ROTA, mapa("tipo", "FREQUENCIA_CONSOLIDADA",
                        "formato", "VISUALIZACAO", "inicio", DATA, "fim", DATA,
                        "contratoIds", List.of(c.contratoA.getId()),
                        "chaveIdempotencia", "c4-precisao-emissao-" + UUID.randomUUID()), 201,
                        "Criação HTTP real de emissão para comparar instantes antes e depois do commit");
                long id = resposta.path("metadados").path("id").asLong();
                Instantes nova = instantes(em.find(EmissaoRelatorioAdministrativo.class, id));
                Instantes anterior = anteriorId == null ? null
                        : instantes(em.find(EmissaoRelatorioAdministrativo.class, anteriorId));
                JsonNode getAnterior = anteriorId == null ? mapper.getNodeFactory().nullNode()
                        : consultar(c.localA, ROTA + "/" + anteriorId,
                                "Captura da superação gerada pela segunda emissão, ainda na transação autora");
                return new Captura(resposta, nova, anterior, getAnterior);
            } catch (Exception ex) { throw new IllegalStateException("Falha na fixture HTTP da emissão", ex); }
        });
    }

    private Instantes reler(long id) {
        return emTransacao(() -> instantes(em.find(EmissaoRelatorioAdministrativo.class, id)));
    }

    private Instantes instantes(EmissaoRelatorioAdministrativo e) {
        return new Instantes(e.getGeradoEm(), e.getFonteConhecidaAte(), e.getSuperadaEm());
    }

    private Instantes instantes(JsonNode resposta) {
        JsonNode m = resposta.path("metadados");
        return new Instantes(Instant.parse(m.path("geradoEm").asText()),
                Instant.parse(m.path("fonteConhecidaAte").asText()),
                m.path("superadaEm").isNull() ? null : Instant.parse(m.path("superadaEm").asText()));
    }

    private record Instantes(Instant geradoEm, Instant fonteConhecidaAte, Instant superadaEm) { }
    private record Captura(JsonNode resposta, Instantes nova, Instantes anterior, JsonNode getAnterior) { }
}
