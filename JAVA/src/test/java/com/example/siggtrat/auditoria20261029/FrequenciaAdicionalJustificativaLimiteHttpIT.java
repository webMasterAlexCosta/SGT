package com.example.siggtrat.auditoria20261029;

import com.example.siggtrat.entidade.FrequenciaAdicional;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;

/** RI08/AUD002: API, raiz e versao devem conservar integralmente o mesmo limite de justificativa. */
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class FrequenciaAdicionalJustificativaLimiteHttpIT extends ContratosHttpBase {

    @ParameterizedTest
    @ValueSource(ints = {255, 256, 2000, 2001})
    void criacaoValidaLimiteSemTruncarMotivoNemGravarParcialmente(int tamanho) throws Exception {
        Cenario c = criarCenarioPersistido();
        String texto = "J".repeat(tamanho);
        long versoesAntes = contarVersoes();
        JsonNode resposta = enviar(c.localA, "/api/frequencias-adicionais", mapa("empregadoId", c.empregadoA.getId(),
                "dataReferencia", DATA, "tipo", "RT", "minutos", 60, "quantidade", 1, "justificativa", texto),
                tamanho <= 2000 ? 201 : 400,
                "POST aceita ate2000 caracteres integralmente e recusa2001 antes de gravar");
        if (tamanho > 2000) {
            assertEquals(0L, contarFontes(c.empregadoA.getId()));
            assertEquals(versoesAntes, contarVersoes(), "Recusa nao deixa versao orfa nem fonte parcial");
            return;
        }
        long id = resposta.path("id").asLong();
        assertEquals(texto, resposta.path("justificativa").asText());
        conferirPersistencia(c, id, texto, 60);
        JsonNode eventos = consultar(c.localA, "/api/frequencias-adicionais/" + id + "/eventos",
                "Versao de criacao conserva o motivo integral apos commit");
        assertEquals(1, eventos.size());
        assertEquals(texto, eventos.get(0).path("justificativa").asText());
        assertEquals(texto, eventos.get(0).path("fotografia").path("justificativa").asText());
        assertEquals(tamanho, eventos.get(0).path("justificativa").asText().length());
    }

    @ParameterizedTest
    @ValueSource(ints = {255, 256, 2000, 2001})
    void retificacaoValidaLimiteEPreservaOriginalQuandoMotivoExcedeMaximo(int tamanho) throws Exception {
        Cenario c = criarCenarioPersistido();
        String original = "Justificativa original curta e comprovada";
        long id = enviar(c.localA, "/api/frequencias-adicionais", mapa("empregadoId", c.empregadoA.getId(),
                "dataReferencia", DATA, "tipo", "RT", "minutos", 60, "quantidade", 1, "justificativa", original),
                201, "Fonte original publica antes da tentativa de retificacao").path("id").asLong();
        JsonNode cadeiaOriginal = consultar(c.localA, "/api/frequencias-adicionais/" + id + "/eventos",
                "Fotografia original permite verificar atomicidade da recusa");
        String texto = "R".repeat(tamanho);
        JsonNode resposta = dados(executar(c.localA, put("/api/frequencias-adicionais/{id}", id)
                .contentType(APPLICATION_JSON).content(json(mapa("minutos", 90, "justificativa", texto))),
                tamanho <= 2000 ? 200 : 400,
                "PUT aceita o mesmo limite da criacao sem perder motivo ou raiz"));
        JsonNode eventos = consultar(c.localA, "/api/frequencias-adicionais/" + id + "/eventos",
                "Releitura da cadeia depois da transacao da retificacao");
        if (tamanho > 2000) {
            conferirPersistencia(c, id, original, 60);
            assertEquals(cadeiaOriginal, eventos,
                    "400 conserva conteudo, quantidade de versoes, autor, instante e hashes anteriores");
            assertEquals(1L, contarFontes(c.empregadoA.getId()));
            return;
        }
        assertEquals(texto, resposta.path("justificativa").asText());
        conferirPersistencia(c, id, texto, 90);
        assertEquals(2, eventos.size());
        assertEquals(cadeiaOriginal.get(0), eventos.get(0));
        assertEquals(texto, eventos.get(1).path("justificativa").asText());
        assertEquals(texto, eventos.get(1).path("fotografia").path("justificativa").asText());
        assertEquals(tamanho, eventos.get(1).path("justificativa").asText().length());
        assertEquals(eventos.get(0).path("hashVersao"), eventos.get(1).path("hashAnterior"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"POST", "PUT"})
    void espacosExternosNaoFazemMotivoValidoDivergirEntreRaizEVersao(String metodo) throws Exception {
        Cenario c = criarCenarioPersistido();
        String texto = "M".repeat(2000);
        String recebido = "  " + texto + "\t ";
        long id;
        JsonNode resposta;
        JsonNode anterior = null;
        if (metodo.equals("POST")) {
            resposta = enviar(c.localA, "/api/frequencias-adicionais", mapa("empregadoId", c.empregadoA.getId(),
                    "dataReferencia", DATA, "tipo", "RT", "minutos", 60, "quantidade", 1, "justificativa", recebido),
                    201, "Limite considera o motivo normalizado e grava o mesmo texto aceito na raiz e na versao");
            id = resposta.path("id").asLong();
        } else {
            id = enviar(c.localA, "/api/frequencias-adicionais", mapa("empregadoId", c.empregadoA.getId(),
                    "dataReferencia", DATA, "tipo", "RT", "minutos", 60, "quantidade", 1,
                    "justificativa", "Motivo original preservado antes da nova versao"),
                    201, "Original publicado antes da retificacao com espacos externos").path("id").asLong();
            anterior = consultar(c.localA, "/api/frequencias-adicionais/" + id + "/eventos", "Cadeia original do adicional").get(0);
            resposta = dados(executar(c.localA, put("/api/frequencias-adicionais/{id}", id)
                    .contentType(APPLICATION_JSON).content(json(mapa("minutos", 90, "justificativa", recebido))),
                    200, "Retificacao persiste o motivo normalizado que passou pelo limite de 2000"));
        }
        assertEquals(texto, resposta.path("justificativa").asText());
        conferirPersistencia(c, id, texto, metodo.equals("POST") ? 60 : 90);
        JsonNode eventos = consultar(c.localA, "/api/frequencias-adicionais/" + id + "/eventos",
                "Raiz, fotografia e motivo da versao nao divergem por espacos externos");
        assertEquals(metodo.equals("POST") ? 1 : 2, eventos.size());
        JsonNode ultima = eventos.get(eventos.size() - 1);
        assertEquals(texto, ultima.path("justificativa").asText());
        assertEquals(texto, ultima.path("fotografia").path("justificativa").asText());
        if (anterior != null) {
            assertEquals(anterior, eventos.get(0));
            assertEquals(anterior.path("hashVersao"), ultima.path("hashAnterior"));
        }
    }

    private void conferirPersistencia(Cenario c, long id, String motivo, int minutos) throws Exception {
        emTransacao(() -> {
            FrequenciaAdicional salvo = em.find(FrequenciaAdicional.class,id);
            assertNotNull(salvo);
            assertEquals(motivo, salvo.getJustificativa());
            assertEquals(minutos, salvo.getMinutos());
            return null;
        });
        JsonNode lista = dados(executar(c.localA, get("/api/frequencias-adicionais").param("data", DATA.toString())
                .param("contratoId", c.contratoA.getId().toString()), 200,
                "GET posterior devolve texto integral gravado em transacao ja concluida"));
        assertEquals(1, lista.path("content").size());
        assertEquals(id, lista.path("content").get(0).path("id").asLong());
        assertEquals(motivo, lista.path("content").get(0).path("justificativa").asText());
        assertEquals(minutos, lista.path("content").get(0).path("minutos").asInt());
    }

    private long contarFontes(Long empregadoId) {
        return emTransacao(() -> em.createQuery("select count(a) from FrequenciaAdicional a where a.empregado.id=:id",Long.class)
                .setParameter("id", empregadoId).getSingleResult());
    }

    private long contarVersoes() {
        return emTransacao(() -> em.createQuery("select count(v) from VersaoComplementoFrequencia v where v.tipoComplemento='ADICIONAL'",Long.class)
                .getSingleResult());
    }
}
