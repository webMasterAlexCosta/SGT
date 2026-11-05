package com.example.siggtrat.auditoria20261029;

import com.example.siggtrat.dominio.vinculo.CategoriaTerminoVinculo;
import com.example.siggtrat.entidade.*;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;

/** DES-003/FRE-017: término retroativo invalida somente dias posteriores; provas continuam auditáveis. */
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class DesligamentoFrequenciaProvaPreservadaHttpIT extends ContratosHttpBase {
    @org.springframework.beans.factory.annotation.Autowired
    private com.example.siggtrat.servico.GestaoContratoTemporalServico gestaoDaFonte;

    @Test
    void desligamentoRetroativoPreservaFrequenciaOcorrenciaEAnexoEExcluiCalculoSomenteAposUltimoDia() throws Exception {
        Cenario c = criarCenarioPersistido();
        declararFonteNominalSintetica(c, "DESLIGAMENTO-PROVA-FREQUENCIA-PRESERVADA");
        LocalDate diaAfetado = DATA.plusDays(1);
        byte[] arquivo = "%PDF-1.4\n1 0 obj\n<< /Type /Catalog >>\nendobj\n%%EOF".getBytes(StandardCharsets.UTF_8);
        JsonNode ocorrencia = dados(executar(c.localA, multipart("/api/ocorrencias")
                .file(new MockMultipartFile("anexos", "prova-jornada.pdf", "application/pdf", arquivo))
                .param("empregadoId", c.empregadoA.getId().toString()).param("motivo", "FALTA")
                .param("dataOcorrencia", diaAfetado.toString()).param("dias", "1")
                .param("chaveIdempotencia", "desligamento-fonte-" + UUID.randomUUID())
                .param("classificacoesAnexos", "NEGOCIAL"),
                201, "Fonte documentada da frequência anterior ao conhecimento do desligamento"));
        long ocorrenciaId = ocorrencia.path("id").asLong();
        long anexoId = ocorrencia.path("anexos").get(0).path("id").asLong();
        long frequenciaId = emTransacao(() -> em.createQuery(
                "select f.id from Frequencia f where f.empregado.id=:e and f.dataReferencia=:d", Long.class)
                .setParameter("e", c.empregadoA.getId()).setParameter("d", diaAfetado).getSingleResult());
        long controleId = enviar(c.localA, "/api/frequencias", mapa("vinculoContratualId", c.vinculoA.getId(),
                "dataReferencia", DATA, "status", "FALTA", "justificativaRetificacao", "Conferência documental do último dia",
                "chaveIdempotencia", "ultimo-dia-" + UUID.randomUUID()), 200,
                "O último dia do vínculo tem cobertura inclusiva").path("id").asLong();
        Long autorOriginal = emTransacao(() -> {
            Frequencia f = em.find(Frequencia.class, frequenciaId);
            assertEquals("FECHADA", f.getEstadoNegocial().name());
            assertEquals("VALIDA", f.getValidadeOperacional().name());
            return f.getAutorOriginalId();
        });
        JsonNode eventosOriginais = consultar(c.localA, "/api/frequencias/" + frequenciaId + "/eventos", "Cadeia original confirmada");
        JsonNode emitida = enviar(c.localA, "/api/relatorios/administrativos/emissoes", filtro(c, diaAfetado), 201,
                "Emissão anterior ao conhecimento do desligamento");
        assertEquals(1, emitida.path("conteudo").path("totais").path("faltas").asInt());
        long motivo = emTransacao(() -> {
            MotivoDesligamento m = new MotivoDesligamento();
            m.setGestaoLocal(em.getReference(GestaoLocal.class, c.gestaoA.getId()));
            m.setDescricao("Desligamento individual documentado, conhecido depois da frequência");
            m.setCategoria(CategoriaTerminoVinculo.DESLIGAMENTO_INDIVIDUAL);
            em.persist(m); em.flush();
            EventoDisponibilidadeCatalogo origem = new EventoDisponibilidadeCatalogo();
            origem.setTipoCatalogo(com.example.siggtrat.dominio.catalogo.TipoCatalogoTemporal.MOTIVO_DESLIGAMENTO);
            origem.setCatalogoId(m.getId()); origem.setDataEfetiva(INICIO); origem.setAtivo(true);
            origem.setMotivo("Fonte sintética explícita do motivo desde o início do vínculo");
            em.persist(origem);
            FonteMotivoSintetica.declarar(em, gestaoDaFonte, c.contratoA.getId(), m.getId(), c.gestaoA.getId(), c.localA,
                    "Desligamento individual documentado, conhecido depois da frequência", CategoriaTerminoVinculo.DESLIGAMENTO_INDIVIDUAL,
                    DATA, DATA, java.time.Instant.now(), "SEI-SINTETICO-DES003-CONTEXTO-MOTIVO");
            return m.getId();
        });
        enviar(c.localA, "/api/vinculos-contratuais/" + c.vinculoA.getId() + "/desligar",
                mapa("dataFim", DATA, "motivoDesligamentoId", motivo,
                        "justificativa", "Documento posterior comprova o último dia efetivamente trabalhado",
                        "evidencia", "SEI-DESLIGAMENTO-RETROATIVO", "autorizacao", "ATO-DESLIGAMENTO"),
                200, "Desligamento retroativo confirmado sem apagar fontes existentes");
        emTransacao(() -> {
            Frequencia f = em.find(Frequencia.class, frequenciaId);
            assertNotNull(f); assertEquals("FALTA", f.getStatus());
            assertEquals("FECHADA", f.getEstadoNegocial().name());
            assertEquals(autorOriginal, f.getAutorOriginalId());
            assertEquals("INVALIDA_COBERTURA", f.getValidadeOperacional().name());
            assertEquals("SUJEITA_REVISAO", f.getEstadoAuditoria().name());
            assertTrue(Boolean.TRUE.equals(f.getBloqueadaEdicaoOperacional()));
            assertNotNull(f.getMotivoRevisao());
            assertEquals("VALIDA", em.find(Frequencia.class, controleId).getValidadeOperacional().name());
            assertEquals(DATA, em.find(VinculoContratual.class, c.vinculoA.getId()).getDataFim());
            assertNotNull(em.find(Ocorrencia.class, ocorrenciaId));
            assertNotNull(em.find(AnexoOcorrencia.class, anexoId));
            return null;
        });
        JsonNode eventosDepois = consultar(c.localA, "/api/frequencias/" + frequenciaId + "/eventos", "Cadeia continua consultável sem cobertura operacional");
        assertEquals(eventosOriginais.get(0), eventosDepois.get(0));
        assertTrue(eventosDepois.size() > eventosOriginais.size());
        JsonNode historica = consultar(c.localA, "/api/relatorios/administrativos/emissoes/"
                + emitida.path("metadados").path("id").asLong(), "Emissão original permanece íntegra e identificada como superada");
        assertEquals("SUPERADA", historica.path("metadados").path("estado").asText());
        assertEquals(emitida.path("conteudo"), historica.path("conteudo"));
        assertEquals(emitida.path("metadados").path("hashConteudo"), historica.path("metadados").path("hashConteudo"));
        JsonNode atual = enviar(c.localA, "/api/relatorios/administrativos/previa", filtro(c, diaAfetado), 200,
                "Cálculo atual exclui a falta sem cobertura e identifica a pendência");
        assertEquals(0, atual.path("conteudo").path("registros").size());
        assertEquals(0, atual.path("conteudo").path("totais").path("faltas").asInt());
        assertTrue(atual.path("metadados").path("sujeitoARevisao").asBoolean());
        assertEquals(1, atual.path("metadados").path("quantidadePendencias").asLong());
        assertArrayEquals(arquivo, executar(c.localA, get("/api/ocorrencias/anexos/" + anexoId), 200,
                "O arquivo original continua recuperável depois do desligamento retroativo").getResponse().getContentAsByteArray());
    }

    private Map<String, Object> filtro(Cenario c, LocalDate data) {
        return mapa("tipo", "FREQUENCIA_CONSOLIDADA", "formato", "VISUALIZACAO", "inicio", data, "fim", data,
                "contratoIds", List.of(c.contratoA.getId()), "chaveIdempotencia", "emissao-desligamento-" + UUID.randomUUID());
    }
}
