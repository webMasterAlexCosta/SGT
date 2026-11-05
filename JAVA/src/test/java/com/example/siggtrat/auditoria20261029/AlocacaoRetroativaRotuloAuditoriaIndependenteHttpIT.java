package com.example.siggtrat.auditoria20261029;

import com.example.siggtrat.entidade.AlocacaoVinculo;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.http.MediaType;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;

/** Regressão do RED independente: fotografia original e ato contemporâneo distintos. */
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class AlocacaoRetroativaRotuloAuditoriaIndependenteHttpIT extends ContratosHttpBase {
    @ParameterizedTest
    @CsvSource({"cargos,true", "lotacoes,true", "jornadas,true", "cargos,false", "lotacoes,false", "jornadas,false"})
    void inclusaoFotografaRotuloComprovadoParaDataEfetiva(String recurso, boolean retroativa) throws Exception {
        var c = criarCenarioPersistido();
        NovoContrato n = emTransacao(() -> {
            try { return publicado(c, 5, null, false); }
            catch (Exception e) { throw new IllegalStateException(e); }
        });
        String campoHistorico = switch (recurso) {
            case "lotacoes" -> "lotacaoNome";
            case "jornadas" -> "jornadaDescricao";
            default -> "cargoNome";
        };
        String campoMestre = recurso.equals("jornadas") ? "descricao" : "nome";
        long catalogoId = switch (recurso) {
            case "lotacoes" -> c.lotacaoA.getId();
            case "jornadas" -> c.jornadaA.getId();
            default -> c.cargoA.getId();
        };
        String rota = "/api/" + recurso + "/" + catalogoId;
        String rotuloDaEpoca = consultar(c.localA,
                "/api/vinculos-contratuais/contratos/" + n.id() + "/opcoes-alocacao?data=" + n.inicio(),
                "Fonte publicada anterior à renomeação").get(0).path(campoHistorico).asText();
        assertThat(rotuloDaEpoca).isNotBlank();
        // Jornada normaliza a descrição para maiúsculas; este cenário verifica
        // a origem temporal do rótulo, conservando comparações literais exatas.
        String rotuloAtual = "RÓTULO ADOTADO SOMENTE DEPOIS DO PERÍODO " + n.id();
        var atualizacao = (com.fasterxml.jackson.databind.node.ObjectNode) consultar(c.localA, rota,
                "Conserva todos os demais atributos do catálogo na renomeação").deepCopy();
        atualizacao.put(campoMestre, rotuloAtual);
        executar(c.localA, put(rota).contentType(MediaType.APPLICATION_JSON)
                .content(json(atualizacao)), 200,
                "Renomeação atual não é correção material dos fatos passados");
        assertThat(consultar(c.localA, rota, "Mestre atual foi renomeado")
                .path(campoMestre).asText()).isEqualTo(rotuloAtual);
        var dataEfetiva = retroativa ? n.inicio() : com.example.siggtrat.utilitario.RelogioSistema.hoje();
        String esperado = retroativa ? rotuloDaEpoca : rotuloAtual;
        JsonNode v = enviar(c.localA, "/api/vinculos-contratuais", mapa(
                "pessoa", mapa("nome", "Pessoa no período original " + n.id(),
                        "cpf", cpf(Math.toIntExact(686000000L + n.id()))),
                "contratoId", n.id(), "composicaoId", n.composicao1(), "dataInicio", dataEfetiva),
                201, "Inserção de vínculo e Alocação na data efetiva informada");
        long vinculoId = v.path("id").asLong();
        JsonNode historico = consultar(c.localA, "/api/vinculos-contratuais/" + vinculoId
                + "/alocacoes/posicao?data=" + dataEfetiva, "CAT-004/HIS-004 rótulo na data efetiva");
        assertThat(historico.path(campoHistorico).asText())
                .as("A fotografia respeita a fonte da data: recurso=%s, retroativa=%s", recurso, retroativa)
                .isEqualTo(esperado);
        emTransacao(() -> {
            var a = em.createQuery("select a from AlocacaoVinculo a where a.vinculo.id=:v", AlocacaoVinculo.class)
                    .setParameter("v", vinculoId).getSingleResult();
            String persistido = switch (recurso) {
                case "lotacoes" -> a.getLotacaoNomeFotografia();
                case "jornadas" -> a.getJornadaDescricaoFotografia();
                default -> a.getCargoNomeFotografia();
            };
            assertThat(persistido).isEqualTo(esperado);
            return null;
        });
    }
}
