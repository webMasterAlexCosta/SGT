package com.example.siggtrat.auditoria20261029;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDate;
import java.util.Map;
import java.util.UUID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

/**
 * C2-PES-02: um evento com fotografia nominal de um único dia não comprova D+1.
 * Oráculo COL-010: aplicar intervalo da origem e nunca preencher lacuna por uma
 * fotografia de outro período. Exercita API de prova, comando e as duas leituras.
 * Proposta independente; sem execução/compilação pelo revisor.
 */
@Transactional(propagation = Propagation.NOT_SUPPORTED)
@TestPropertySource(properties={"spring.cache.type=redis", "spring.sql.init.mode=never",
        "app.cache.redis.http-get.enabled=true"})
class Candidato2PosicaoFonteNominalPontualHttpIT extends ContratosHttpBase {
    @Test
    void posicaoNaoEstendeParaDiaSeguinteNomeComprovadoSomenteNoDiaDoEvento() throws Exception {
        Cenario c = criarCenarioPersistido();
        conceder(c, c.contratoA.getId(), "INCORPORACAO_PROVA_HISTORICA");
        String nome = "Nome comprovado exclusivamente no dia documental";
        String provas = "/api/pessoas/" + c.pessoaA.getId() + "/provas-nome";
        Map<String,Object> req = mapa("vinculoId", c.vinculoA.getId(), "nomeComprovado", nome,
                "dataInicioEfetiva", DATA, "dataFimEfetiva", DATA,
                "referenciaSei", "SEI-SINTETICO-PONTUAL-" + UUID.randomUUID(),
                "motivo", "A fonte sintética comprova nome apenas neste dia, sem cobertura do dia seguinte",
                "autorizacao", "ATO-SINTETICO-C2-PES-02", "chaveIdempotencia", UUID.randomUUID().toString(),
                "versaoPessoaEsperada", consultar(c.localA, "/api/pessoas/" + c.pessoaA.getId(),
                        "Versão da Pessoa").path("version").asLong(),
                "versaoVinculoEsperada", consultar(c.localA, "/api/vinculos-contratuais/" + c.vinculoA.getId(),
                        "Versão do Vínculo").path("version").asLong());
        JsonNode previa = enviar(c.localA, provas + "/previa", req, 200, "Fonte nominal com intervalo de um dia");
        req.put("confirmarImpacto", true);
        req.put("tokenPrevia", previa.path("tokenPrevia").asText());
        enviar(c.localA, provas, req, 201, "Incorporar fonte sem reescrever origem ausente");
        enviar(c.localA, "/api/vinculos-contratuais/" + c.vinculoA.getId() + "/alterar-matricula",
                mapa("dataEfetiva", DATA, "novaMatricula", "PONTUAL-" + c.vinculoA.getId(),
                        "justificativa", "Alteração real de matrícula no dia comprovado",
                        "evidencia", "SEI-SINTETICO-MATRICULA-PONTUAL", "autorizacao", "ATO-SINTETICO-MATRICULA"),
                200, "Novo fato retroativo captura somente o nome documentado nesse dia");
        JsonNode eventos = consultar(c.localA, "/api/vinculos-contratuais/" + c.vinculoA.getId() + "/eventos",
                "Controle: evento de matrícula preserva a fotografia pontual");
        assertThat(eventos).hasSize(1);
        assertThat(eventos.get(0).path("pessoaNomeFotografia").asText()).isEqualTo(nome);

        assertThat(nome(c, DATA).path("nome").asText()).isEqualTo(nome);
        JsonNode semFonte = nome(c, DATA.plusDays(1));
        assertThat(semFonte.path("comprovado").asBoolean()).isFalse();
        assertThat(semFonte.path("nome").isNull()).isTrue();
        JsonNode posicao = dados(executar(c.localA,
                get("/api/vinculos-contratuais/pessoas/" + c.pessoaA.getId() + "/posicao")
                        .param("data", DATA.plusDays(1).toString()), 200,
                "A posição da Pessoa deve concordar com a ausência de fonte nominal para D+1"));
        assertThat(posicao.path("identidadeHistorica").path("comprovado").asBoolean()).isFalse();
        assertThat(posicao.path("identidadeHistorica").path("nome").isNull()).isTrue();
        assertThat(posicao.path("nomeFotografia").isNull())
                .as("Nome fotografado no fato de D não preenche a posição nominal de D+1").isTrue();
        assertThat(posicao.path("fotografiaIdentidadeComprovada").asBoolean()).isFalse();
        assertThat(posicao.path("eventoIdentidadeFonteId").isNull()).isTrue();
        assertThat(consultar(c.localA, "/api/vinculos-contratuais/" + c.vinculoA.getId() + "/eventos",
                "Negar extrapolação não apaga nem altera a foto do evento").get(0).path("pessoaNomeFotografia").asText())
                .isEqualTo(nome);
    }

    private JsonNode nome(Cenario c, LocalDate data) throws Exception {
        return dados(executar(c.localA, get("/api/pessoas/" + c.pessoaA.getId() + "/nome-historico")
                        .param("vinculoId", c.vinculoA.getId().toString()).param("data", data.toString()),
                200, "Resolver identidade pela data e pelo intervalo declarado"));
    }
}
