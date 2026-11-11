package com.example.siggtrat.auditoria20261029;

import com.example.siggtrat.entidade.AlocacaoVinculo;
import com.example.siggtrat.entidade.VinculoContratual;
import com.example.siggtrat.utilitario.RelogioSistema;
import com.fasterxml.jackson.databind.JsonNode;
import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.BeforeEach;
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
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Autoria: /root/revisao_final_pessoas. RED complementar solicitado pela raiz.
 * Fonte C4 intacta: manifesto 13c84fd30b668335228704fd1028ddea11adad6f607458a7b7fa993dcbc64a89.
 * Não compilado/executado pelo revisor. Novos fatos contemporâneos/futuros,
 * sem inferência nominal ou de motivo para período retroativo. O helper publicado
 * declara explicitamente a fonte sintética integral e a disponibilidade do Cargo.
 * A fixture compartilhada simula somente o agendador; PostgreSQL/Redis e HTTP MVC reais.
 */
@Transactional(propagation = Propagation.NOT_SUPPORTED)
@TestPropertySource(properties = {"spring.cache.type=redis", "spring.sql.init.mode=never",
        "app.cache.redis.http-get.enabled=true"})
class Candidato4RecibosVinculoAlocacaoPrecisaoHttpIT extends ContratosHttpBase {
    @Autowired private DataSource dataSource;
    @Autowired private RedisConnectionFactory redis;
    @Autowired private Environment ambiente;

    @BeforeEach
    void infraestruturaReal() throws Exception {
        try (var conexao = dataSource.getConnection()) {
            assertThat(conexao.getMetaData().getDatabaseProductName()).isEqualTo("PostgreSQL");
            assertThat(conexao.getMetaData().getDatabaseProductVersion()).matches("18\\.6(?: \\(Debian [^()]+\\))?");
        }
        assertThat(ambiente.getProperty("spring.jpa.hibernate.ddl-auto")).isEqualTo("update");
        assertThat(ambiente.getProperty("spring.sql.init.mode")).isEqualTo("never");
        try (var conexao = redis.getConnection()) { assertThat(conexao.ping()).isEqualTo("PONG"); }
    }

    @Test
    void desligamentoERetificacaoDevolvemOsInstantesQuePermanecemNoMysql() throws Exception {
        Cenario c = criarCenarioPersistido();
        long motivo = enviar(c.localA, "/api/motivos-desligamento", mapa(
                "descricao", "Causa contemporânea sintética C4 " + UUID.randomUUID(),
                "categoria", "DESLIGAMENTO_INDIVIDUAL", "ativo", true), 201,
                "Novo catálogo contemporâneo; não comprova período anterior a hoje").path("id").asLong();
        String rota = "/api/vinculos-contratuais/" + c.vinculoA.getId();
        Map<String, Object> req = mapa("dataFim", RelogioSistema.hoje(), "motivoDesligamentoId", motivo,
                "justificativa", "Ato contemporâneo sintético de término individual",
                "evidencia", "SEI-SINTETICO-C4-RECIBO-DESLIGAMENTO", "autorizacao", "ATO-SINTETICO-C4");
        JsonNode desligado = enviar(c.localA, rota + "/desligar", req, 200,
                "Desligamento efetivo hoje com causa contemporânea expressamente escolhida");
        Recibo primeiro = reciboVinculo(c, rota, desligado);
        req.put("dataFim", RelogioSistema.hoje().plusDays(1));
        JsonNode previa = enviar(c.localA, rota + "/previas/retificacao-desligamento", req, 200,
                "Prévia explícita da extensão do último dia");
        req.put("confirmarImpacto", true); req.put("tokenPrevia", previa.path("token").asText());
        JsonNode retificado = enviar(c.localA, rota + "/retificar-desligamento", req, 200,
                "Retificação preserva a origem e confirma o impacto");
        Recibo segundo = reciboVinculo(c, rota, retificado);
        JsonNode eventos = consultar(c.localA, rota + "/eventos", "Trilha original e retificação preservadas");
        assertThat(eventos).hasSize(2);
        assertThat(eventos.get(0).path("tipo").asText()).isEqualTo("DESLIGAMENTO");
        assertThat(eventos.get(1).path("tipo").asText()).isEqualTo("RETIFICACAO_DESLIGAMENTO");
        assertThat(eventos.get(1).path("eventoPrecedenteId").asLong()).isEqualTo(eventos.get(0).path("id").asLong());
        evidenciar("desligamento-retificacao", List.of(primeiro, segundo));
        SoftAssertions.assertSoftly(soft -> {
            conferir(soft, primeiro, "desligamento");
            conferir(soft, segundo, "retificação");
        });
    }

    @Test
    void reciboDaNovaAlocacaoConservaMesmoCriadoEmAposCommitEReleitura() throws Exception {
        Cenario c = criarCenarioPersistido();
        NovoContrato contrato = emTransacao(() -> {
            try { return publicado(c, 10, 5, false); }
            catch (Exception ex) { throw new IllegalStateException(ex); }
        });
        JsonNode criado = enviar(c.localA, "/api/vinculos-contratuais", mapa(
                "pessoa", mapa("nome", "Pessoa contemporânea sintética de recibo C4",
                        "cpf", cpf(680000000 + Math.toIntExact(contrato.id()))),
                "contratoId", contrato.id(), "composicaoId", contrato.composicao1(),
                "dataInicio", RelogioSistema.hoje()), 201,
                "Cadastro contemporâneo em publicação sintética integral, com origem explícita");
        long vinculoId = criado.path("id").asLong();
        String rota = "/api/vinculos-contratuais/" + vinculoId;
        Map<String, Object> req = mapa("dataEfetiva", RelogioSistema.hoje().plusDays(1),
                "composicaoId", contrato.composicao2(), "justificativa", "Nova alocação futura sintética C4",
                "evidencia", "SEI-SINTETICO-C4-ALOCACAO", "autorizacao", "ATO-SINTETICO-C4");
        JsonNode previa = enviar(c.localA, rota + "/previas/alocacoes", req, 200,
                "Prévia de mudança para a segunda combinação expressamente publicada");
        assertThat(previa.path("incompatibilidades")).isEmpty();
        req.put("tokenPrevia", previa.path("token").asText()); req.put("confirmarImpacto", true);
        JsonNode alocacao = enviar(c.localA, rota + "/alocacoes", req, 200, "Confirmação da nova faixa futura");
        long id = alocacao.path("id").asLong();
        Instant recebido = Instant.parse(alocacao.path("criadoEm").asText());
        Instant persistido = emTransacao(() -> {
            em.clear();
            AlocacaoVinculo salva = em.find(AlocacaoVinculo.class, id);
            assertThat(salva.getVinculo().getId()).isEqualTo(vinculoId);
            assertThat(salva.getDataInicio()).isEqualTo(RelogioSistema.hoje().plusDays(1));
            return salva.getCriadoEm();
        });
        JsonNode lista = consultar(c.localA, rota + "/alocacoes", "Releitura HTTP em nova transação após o commit");
        JsonNode relida = null;
        for (JsonNode item : lista) if (item.path("id").asLong() == id) relida = item;
        assertThat(relida).isNotNull();
        Recibo recibo = new Recibo(recebido, persistido, Instant.parse(relida.path("criadoEm").asText()));
        evidenciar("alocacao", List.of(recibo));
        SoftAssertions.assertSoftly(soft -> conferir(soft, recibo, "alocação"));
    }

    private Recibo reciboVinculo(Cenario c, String rota, JsonNode dto) throws Exception {
        Instant recebido = Instant.parse(dto.path("desligamentoRegistradoEm").asText());
        Instant persistido = emTransacao(() -> {
            em.clear();
            return em.find(VinculoContratual.class, c.vinculoA.getId()).getDesligamentoRegistradoEm();
        });
        JsonNode relido = consultar(c.localA, rota, "Releitura do Vínculo em transação independente");
        return new Recibo(recebido, persistido, Instant.parse(relido.path("desligamentoRegistradoEm").asText()));
    }
    private void conferir(SoftAssertions soft, Recibo recibo, String tipo) {
        // Instant exato; não compara formatação e não arredonda/trunca o oráculo.
        soft.assertThat(recibo.recebido()).as("Recibo de %s coincide exatamente com PostgreSQL", tipo)
                .isEqualTo(recibo.persistido());
        soft.assertThat(recibo.relido()).as("GET de %s coincide com persistência real", tipo)
                .isEqualTo(recibo.persistido());
        soft.assertThat(recibo.relido()).as("Releitura não muda o instante anunciado para %s", tipo)
                .isEqualTo(recibo.recebido());
    }
    private void evidenciar(String tipo, List<Recibo> recibos) throws Exception {
        Path destino = Path.of("target/auditoria-http/candidato4-recibos-" + tipo + ".json");
        Files.createDirectories(destino.getParent());
        Files.writeString(destino, json(mapa("hipotese", "C4-PES-H03", "tipo", tipo, "recibos", recibos,
                "transporte", "MockMvc/dispatcher real; PostgreSQL18.6 e Redis reais; não TCP")));
    }
    private record Recibo(Instant recebido, Instant persistido, Instant relido) {}
}
