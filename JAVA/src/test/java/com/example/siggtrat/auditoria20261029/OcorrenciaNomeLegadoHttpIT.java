package com.example.siggtrat.auditoria20261029;

import com.example.siggtrat.entidade.Empregado;
import com.example.siggtrat.entidade.Ocorrencia;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.springframework.http.MediaType;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;

/** RI-07: identidade/escopo comprovados não provam um nome histórico nunca fotografado. */
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class OcorrenciaNomeLegadoHttpIT extends ContratosHttpBase {
    @ParameterizedTest
    @NullAndEmptySource
    void nomeHistoricoAusentePermaneceDesconhecidoAposCorrecaoECancelamento(String ausente) throws Exception {
        Cenario c = criarCenarioPersistido();
        declararFonteNominalSintetica(c, "preservacao-ocorrencia-" + UUID.randomUUID());
        JsonNode criada = criar(c, DATA);
        long id = criada.path("id").asLong();
        assertEquals(c.empregadoA.getNome(), criada.path("empregado").path("nome").asText());
        emTransacao(() -> {
            assertEquals(1, em.createQuery("update Ocorrencia set empregadoNomeFotografia=:nome where id=:id")
                    .setParameter("nome", ausente).setParameter("id", id).executeUpdate());
            return null;
        });
        String novoNome = "Nome atual posterior ao fato " + UUID.randomUUID();
        executar(c.localA, post("/api/pessoas/{id}/correcoes-identidade", c.pessoaA.getId())
                .contentType(MediaType.APPLICATION_JSON).content(json(mapa("tipo", "NOME", "novoValor", novoNome,
                        "motivo", "Correção documentada do cadastro atual", "evidencia", "SEI-NOME-ATUAL",
                        "autorizacao", "ATO-IDENTIDADE", "impactosEncontrados", "Fotografias históricas devem ser preservadas"))),
                200, "Correção excepcional do nome atual sem editar a fonte legada");
        assertEquals(novoNome, emTransacao(() -> em.find(Empregado.class, c.empregadoA.getId()).getNome()));
        JsonNode detalhe = consultar(c.localA, "/api/ocorrencias/" + id, "Fato legado continua consultável no escopo comprovado");
        assertTrue(detalhe.path("ocorrencia").path("empregado").path("nome").isNull(),
                "Nome atual não pode ser apresentado como fotografia ausente do fato antigo");
        JsonNode lista = dados(executar(c.localA, get("/api/ocorrencias")
                .param("empregadoId", c.empregadoA.getId().toString()).param("data", DATA.toString()), 200,
                "Listagem aplica a mesma semântica de nome histórico desconhecido"));
        assertEquals(1, lista.path("totalElements").asInt());
        assertTrue(lista.path("content").get(0).path("empregado").path("nome").isNull());
        executar(c.localA, delete("/api/ocorrencias/" + id), 204,
                "Cancelamento lógico não autoriza preencher fotografia ausente");
        emTransacao(() -> {
            Ocorrencia o = em.find(Ocorrencia.class, id);
            assertEquals(ausente, o.getEmpregadoNomeFotografia());
            assertNotNull(o.getCanceladoEm());
            assertFalse(o.getContextos().isEmpty(), "A prova de escopo existente não foi apagada");
            return null;
        });
        assertTrue(consultar(c.localA, "/api/ocorrencias/" + id, "Consulta após cancelamento preserva desconhecimento")
                .path("ocorrencia").path("empregado").path("nome").isNull());
        JsonNode nova = criar(c, DATA.plusDays(1));
        // TMP-004/005 e COL-010, conforme CONFLITOS_ORACULOS_E_FIXTURES.md:
        // a prova opt-in cobre o período; a correção atual não muda sua vigência.
        assertEquals(c.pessoaA.getNome(), nova.path("empregado").path("nome").asText(),
                "Um novo lançamento passado usa a fonte nominal do período, sem copiar o cadastro atual");
    }

    private JsonNode criar(Cenario c, LocalDate data) throws Exception {
        return dados(executar(c.localA, multipart("/api/ocorrencias")
                .param("empregadoId", c.empregadoA.getId().toString()).param("motivo", "FALTA")
                .param("dataOcorrencia", data.toString()).param("dias", "1")
                .param("chaveIdempotencia", "nome-historico-" + UUID.randomUUID()), 201,
                "Ocorrência com associação canônica e fotografia de escopo real"));
    }
}
