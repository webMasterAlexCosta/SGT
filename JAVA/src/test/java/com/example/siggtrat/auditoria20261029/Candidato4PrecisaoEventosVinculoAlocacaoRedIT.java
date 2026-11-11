package com.example.siggtrat.auditoria20261029;

import com.example.siggtrat.entidade.EventoAlocacaoVinculo;
import com.example.siggtrat.entidade.EventoVinculoContratual;
import com.example.siggtrat.utilitario.RelogioSistema;
import com.fasterxml.jackson.databind.JsonNode;
import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import javax.sql.DataSource;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Autoria independente: /root/revisao_final_pessoas. Hipótese C4-PES-H05.
 * Base C4 13c84fd30b668335228704fd1028ddea11adad6f607458a7b7fa993dcbc64a89.
 * Um ato legítimo cria os dois tipos de evento. Captura entidades gerenciadas
 * na mesma transação da escrita, confirma e relê em nova transação PostgreSQL.
 * Não altera Instant legado, não cria evento diretamente nem fornece história
 * pelo mestre. A publicação do helper é fonte sintética integral declarada.
 * Teste proposto: não compilado/executado pelo revisor.
 */
@Transactional(propagation = Propagation.NOT_SUPPORTED)
@TestPropertySource(properties = {"spring.cache.type=redis", "spring.sql.init.mode=never",
        "app.cache.redis.http-get.enabled=true"})
class Candidato4PrecisaoEventosVinculoAlocacaoRedIT extends ContratosHttpBase {
    @Autowired private DataSource dataSource;
    @Autowired private RedisConnectionFactory redis;
    @Autowired private Environment ambiente;

    @Test
    void eventosDoCadastroConservamInstanteGerenciadoAposCommitMysql() throws Exception {
        try (var conexao = dataSource.getConnection()) {
            assertThat(conexao.getMetaData().getDatabaseProductName()).isEqualTo("PostgreSQL");
            assertThat(conexao.getMetaData().getDatabaseProductVersion()).matches("18\\.6(?: \\(Debian [^()]+\\))?");
        }
        assertThat(ambiente.getProperty("spring.jpa.hibernate.ddl-auto")).isEqualTo("update");
        assertThat(ambiente.getProperty("spring.sql.init.mode")).isEqualTo("never");
        try (var conexao = redis.getConnection()) { assertThat(conexao.ping()).isEqualTo("PONG"); }
        Cenario c = criarCenarioPersistido();
        NovoContrato contrato = emTransacao(() -> {
            try { return publicado(c, 10, 5, false); }
            catch (Exception ex) { throw new IllegalStateException(ex); }
        });
        Captura gerenciada = emTransacao(() -> {
            try {
                JsonNode criado = enviar(c.localA, "/api/vinculos-contratuais", mapa(
                        "pessoa", mapa("nome", "Pessoa sintética C4 precisão de eventos", "cpf",
                                cpf(695000000 + Math.toIntExact(contrato.id()))),
                        "contratoId", contrato.id(), "composicaoId", contrato.composicao1(),
                        "dataInicio", RelogioSistema.hoje()), 201,
                        "Cadastro contemporâneo canônico produz eventos legítimos na mesma transação");
                long vinculoId = criado.path("id").asLong();
                em.flush();
                EventoVinculoContratual ev = em.createQuery(
                                "select e from EventoVinculoContratual e where e.vinculo.id=:v", EventoVinculoContratual.class)
                        .setParameter("v", vinculoId).getSingleResult();
                EventoAlocacaoVinculo ea = em.createQuery(
                                "select e from EventoAlocacaoVinculo e where e.vinculo.id=:v", EventoAlocacaoVinculo.class)
                        .setParameter("v", vinculoId).getSingleResult();
                assertThat(em.contains(ev)).as("Evento do vínculo ainda gerenciado na transação produtora").isTrue();
                assertThat(em.contains(ea)).as("Evento de alocação ainda gerenciado na transação produtora").isTrue();
                assertThat(ev.getTipoEvento().name()).isEqualTo("CRIACAO");
                assertThat(ea.getTipoEvento().name()).isEqualTo("CRIACAO");
                return new Captura(vinculoId, ev.getId(), ev.getRegistradoEm(), ev.getHashEvento(),
                        ea.getId(), ea.getRegistradoEm(), ea.getHashEvento());
            } catch (Exception ex) { throw new IllegalStateException(ex); }
        }); // O TransactionTemplate confirma antes de retornar a captura.
        Captura persistida = emTransacao(() -> {
            em.clear();
            EventoVinculoContratual ev = em.find(EventoVinculoContratual.class, gerenciada.eventoVinculoId());
            EventoAlocacaoVinculo ea = em.find(EventoAlocacaoVinculo.class, gerenciada.eventoAlocacaoId());
            assertThat(ev.getVinculo().getId()).isEqualTo(gerenciada.vinculoId());
            assertThat(ea.getVinculo().getId()).isEqualTo(gerenciada.vinculoId());
            return new Captura(gerenciada.vinculoId(), ev.getId(), ev.getRegistradoEm(), ev.getHashEvento(),
                    ea.getId(), ea.getRegistradoEm(), ea.getHashEvento());
        });
        Path evidencia = Path.of("target/auditoria-http/candidato4-eventos-vinculo-alocacao-precisao.json");
        Files.createDirectories(evidencia.getParent());
        Files.writeString(evidencia, json(mapa("hipotese", "C4-PES-H05", "gerenciada_antes_commit", gerenciada,
                "persistida_nova_transacao", persistida, "origem", "cadastro HTTP MVC contemporâneo e canônico",
                "infraestrutura", "PostgreSQL18.6 e Redis reais; não TCP do JAR")));
        SoftAssertions.assertSoftly(soft -> {
            soft.assertThat(gerenciada.registroVinculo()).as("EventoVinculo conserva seu Instant após commit")
                    .isEqualTo(persistida.registroVinculo());
            soft.assertThat(gerenciada.registroAlocacao()).as("EventoAlocacao conserva seu Instant após commit")
                    .isEqualTo(persistida.registroAlocacao());
            soft.assertThat(persistida.hashVinculo()).isEqualTo(gerenciada.hashVinculo());
            soft.assertThat(persistida.hashAlocacao()).isEqualTo(gerenciada.hashAlocacao());
        });
    }

    private record Captura(long vinculoId, long eventoVinculoId, Instant registroVinculo, String hashVinculo,
                           long eventoAlocacaoId, Instant registroAlocacao, String hashAlocacao) {}
}
