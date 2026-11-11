package com.example.siggtrat.auditoria20261029;

import com.example.siggtrat.entidade.UsuarioAplicacao;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

@TestPropertySource(properties = "frequencia.janela-operacional-dias=30")
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class FechamentoDataFuturaHttpIT extends ContratosHttpBase {
    @Test
    void apiRecusaFechamentoFuturoParaGestorEOperacionalSemCriarJobOuFonte() throws Exception {
        Cenario c = criarCenarioPersistido();
        LocalDate amanha = LocalDate.now(ZoneId.of("America/Sao_Paulo")).plusDays(1);
        List<Long> antes = estado(c);
        for (UsuarioAplicacao ator : List.of(c.localA, c.operacionalA)) {
            JsonNode negativa = solicitar(c, ator, amanha, 400);
            assertTrue(negativa.path("message").asText().contains("futura"));
            assertEquals(antes, estado(c), "Recusa não deixa job, frequência ou evento parcial");
        }
    }

    @Test
    void recusaOperacionalTardiaInformaJanelaEfetivaConfiguradaSemLiteralSeteDias() throws Exception {
        Cenario c = criarCenarioPersistido();
        LocalDate dia = LocalDate.now(ZoneId.of("America/Sao_Paulo")).minusDays(30);
        List<Long> antes = estado(c);
        JsonNode negativa = solicitar(c, c.operacionalA, dia, 403);
        String mensagem = negativa.path("message").asText();
        assertTrue(mensagem.contains("FECHADA"));
        assertTrue(mensagem.contains("30"), "Mensagem deve refletir a configuração efetiva, sem prazo fixo obsoleto");
        assertFalse(mensagem.contains("8º"));
        assertEquals(antes, estado(c));
    }

    private JsonNode solicitar(Cenario c, UsuarioAplicacao ator, LocalDate data, int status) throws Exception {
        Map<String,Object> corpo = mapa("dataReferencia", data, "lotacaoId", c.lotacaoA.getId(),
                "jornadaId", c.jornadaA.getId(), "contratoIds", List.of(c.contratoA.getId()),
                "alteracoes", List.of(), "justificativaRetificacao", "Controle sintético de data do fechamento",
                "chaveIdempotencia", "data-fechamento-" + UUID.randomUUID());
        return dados(executar(ator, post("/api/frequencias/fechamentos").contentType(APPLICATION_JSON)
                .content(json(corpo)), status, "Data do fechamento é validada no backend"));
    }

    private List<Long> estado(Cenario c) {
        return emTransacao(() -> List.of(
                em.createQuery("select count(j) from FechamentoFrequencia j where j.lotacao.id=:l", Long.class)
                        .setParameter("l", c.lotacaoA.getId()).getSingleResult(),
                em.createQuery("select count(f) from Frequencia f where f.vinculoContratual.id=:v", Long.class)
                        .setParameter("v", c.vinculoA.getId()).getSingleResult(),
                em.createQuery("select count(e) from EventoFrequencia e where e.frequencia.vinculoContratual.id=:v", Long.class)
                        .setParameter("v", c.vinculoA.getId()).getSingleResult()));
    }
}
