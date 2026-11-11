package com.example.siggtrat.auditoria20261029;

import com.example.siggtrat.entidade.EmissaoRelatorioAdministrativo;
import com.example.siggtrat.entidade.VinculoContratual;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * VIN-014:003 / REL-006: reforço da ponte comando -> consumidor de emissão.
 * A fixture declara uma origem sintética explícita; ela não recupera acervo real.
 * HTTP, serviços e leituras posteriores usam transações próprias no PostgreSQL.
 */
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class RetificacaoInicioEmissaoPreexistenteHttpIT extends ContratosHttpBase {

    @Test
    void retificarInicioSuperaEmissaoPreexistentePreservaBytesEPeriodoExternoDepoisDoCommit() throws Exception {
        Cenario c = criarCenarioPersistido();
        declararFonteNominalSintetica(c, "VIN014-EMISSAO-PREEXISTENTE");
        long vinculoId = c.vinculoA.getId();
        LocalDate diaRetirado = DATA;
        LocalDate novoInicio = DATA.plusDays(1);
        String rotaVinculo = "/api/vinculos-contratuais/" + vinculoId;
        assertSemFrequencia(vinculoId);

        // O filtro por Lotação/Turno exige considerar a faixa ANTERIOR ao ato:
        // depois da retificação ela já não cobre o dia da emissão atingida.
        Map<String, Object> filtroAtingido = filtro(c, diaRetirado);
        JsonNode original = enviar(c.localA, "/api/relatorios/administrativos/emissoes",
                filtroAtingido, 201,
                "VIN-014:003/REL-006 emissão preexistente da cobertura que será retirada");
        long emissaoId = original.path("metadados").path("id").asLong();
        assertTrue(emissaoId > 0);
        assertEquals(1, original.path("conteudo").path("registros").size());
        assertEquals(1, original.path("conteudo").path("totais").path("semLancamento").asInt());
        assertEquals(480, original.path("conteudo").path("totais").path("minutosPrevistos").asInt());

        JsonNode controle = enviar(c.localA, "/api/relatorios/administrativos/emissoes",
                filtro(c, novoInicio), 201,
                "VIN-014:003 controle no primeiro dia que permanece coberto, fora do intervalo retirado");
        long controleId = controle.path("metadados").path("id").asLong();
        assertEquals(1, controle.path("conteudo").path("registros").size());
        assertEquals(480, controle.path("conteudo").path("totais").path("minutosPrevistos").asInt());
        FotoEmissao fotoOriginal = fotografia(emissaoId);
        FotoEmissao fotoControle = fotografia(controleId);
        assertEquals("VIGENTE", fotoOriginal.estado());
        assertEquals("VIGENTE", fotoControle.estado());
        assertTrue(fotoOriginal.gzip().length > 0);
        JsonNode eventosAntes = consultar(c.localA, rotaVinculo + "/eventos",
                "VIN-014 origem do vínculo antes da retificação auditada");
        assertEquals(1, eventosAntes.size());

        Map<String, Object> pedido = mapa("novoInicio", novoInicio,
                "justificativa", "Fonte sintética expressa corrige o primeiro dia do vínculo",
                "evidencia", "SEI-SINTETICO-VIN014-EMISSAO-PREEXISTENTE",
                "autorizacao", "Conferência do Gestor Local do contrato");
        JsonNode previa = enviar(c.localA, rotaVinculo + "/previas/retificacao-inicio",
                pedido, 200, "VIN-014 prévia real do mesmo conteúdo que será confirmado");
        assertEquals(INICIO.toString(), previa.path("valorAtual").asText());
        assertEquals(0, previa.path("vinculosConflitantes").size());
        assertEquals(64, previa.path("token").asText().length());
        pedido.put("tokenPrevia", previa.path("token").asText());
        pedido.put("confirmarImpacto", true);
        JsonNode retificado = enviar(c.localA, rotaVinculo + "/retificar-inicio", pedido,
                200, "VIN-014:003 o comando de início aciona a invalidação da emissão já persistida");
        assertEquals(vinculoId, retificado.path("id").asLong());
        assertEquals(novoInicio.toString(), retificado.path("dataInicio").asText());
        assertEquals(novoInicio, emTransacao(() -> {
            em.clear();
            return em.find(VinculoContratual.class, vinculoId).getDataInicio();
        }), "A nova leitura não compartilha a transação HTTP do comando");
        assertSemFrequencia(vinculoId);

        FotoEmissao depois = fotografia(emissaoId);
        FotoEmissao controleDepois = fotografia(controleId);
        assertEquals("SUPERADA", depois.estado());
        assertEquals("RETIFICACAO_FONTE", depois.causa());
        assertEquals(fotoOriginal.hash(), depois.hash());
        assertArrayEquals(fotoOriginal.gzip(), depois.gzip(),
                "A superação não recompõe nem reescreve o conteúdo oficial anterior");
        assertEquals(fotoOriginal.geradoEm(), depois.geradoEm());
        assertEquals(fotoOriginal.fonteConhecidaAte(), depois.fonteConhecidaAte());
        assertEquals("VIGENTE", controleDepois.estado());
        assertNull(controleDepois.causa());
        assertEquals(fotoControle.hash(), controleDepois.hash());
        assertArrayEquals(fotoControle.gzip(), controleDepois.gzip());

        JsonNode historica = consultar(c.localA,
                "/api/relatorios/administrativos/emissoes/" + emissaoId,
                "REL-006 a API conserva a fotografia oficial anterior com superação explícita");
        assertEquals("SUPERADA", historica.path("metadados").path("estado").asText());
        assertEquals(original.path("conteudo"), historica.path("conteudo"));
        assertEquals(original.path("metadados").path("hashConteudo"),
                historica.path("metadados").path("hashConteudo"));
        JsonNode controleHttp = consultar(c.localA,
                "/api/relatorios/administrativos/emissoes/" + controleId,
                "VIN-014:003 a borda superior do impacto não retira o primeiro dia ainda válido");
        assertEquals("VIGENTE", controleHttp.path("metadados").path("estado").asText());
        assertEquals(controle.path("conteudo"), controleHttp.path("conteudo"));

        JsonNode recalculo = enviar(c.localA, "/api/relatorios/administrativos/previa",
                filtroAtingido, 200, "Controle do efeito: a data retirada já não tem previsão operacional");
        assertEquals(0, recalculo.path("conteudo").path("registros").size());
        assertEquals(0, recalculo.path("conteudo").path("totais").path("minutosPrevistos").asInt());
        filtroAtingido.put("chaveIdempotencia", "vin014-retificada-" + UUID.randomUUID());
        JsonNode nova = enviar(c.localA, "/api/relatorios/administrativos/emissoes",
                filtroAtingido, 201, "REL-006 nova emissão sucede a original sem apagar seu conteúdo");
        assertEquals(2, nova.path("metadados").path("versao").asInt());
        assertEquals(emissaoId, nova.path("metadados").path("emissaoAnteriorId").asLong());
        assertEquals("RETIFICADA", nova.path("metadados").path("natureza").asText());
        assertEquals(0, nova.path("conteudo").path("registros").size());

        JsonNode eventosDepois = consultar(c.localA, rotaVinculo + "/eventos",
                "VIN-014 a invalidação não substitui a origem nem o evento auditado de retificação");
        assertEquals(2, eventosDepois.size());
        assertEquals(eventosAntes.get(0), eventosDepois.get(0));
        assertEquals("RETIFICACAO_INICIO", eventosDepois.get(1).path("tipo").asText());
    }

    private Map<String, Object> filtro(Cenario c, LocalDate data) {
        return mapa("tipo", "FREQUENCIA_CONSOLIDADA", "formato", "VISUALIZACAO",
                "inicio", data, "fim", data, "contratoIds", List.of(c.contratoA.getId()),
                "lotacaoId", c.lotacaoA.getId(), "jornadaId", c.jornadaA.getId(),
                "chaveIdempotencia", "vin014-emissao-" + UUID.randomUUID());
    }

    private void assertSemFrequencia(long vinculoId) {
        long quantidade = emTransacao(() -> em.createQuery(
                        "select count(f) from Frequencia f where f.vinculoContratual.id=:id", Long.class)
                .setParameter("id", vinculoId).getSingleResult());
        assertEquals(0L, quantidade,
                "A invalidação deve ocorrer pelo vínculo mesmo sem frequência materializada");
    }

    private FotoEmissao fotografia(long id) {
        return emTransacao(() -> {
            em.clear();
            EmissaoRelatorioAdministrativo e = em.find(EmissaoRelatorioAdministrativo.class, id);
            assertNotNull(e);
            return new FotoEmissao(e.getEstado().name(),
                    e.getCausaSuperacao() == null ? null : e.getCausaSuperacao().name(),
                    e.getHashConteudo(), e.getConteudoSnapshotGzip().clone(),
                    e.getGeradoEm(), e.getFonteConhecidaAte());
        });
    }

    private record FotoEmissao(String estado, String causa, String hash, byte[] gzip,
                               Instant geradoEm, Instant fonteConhecidaAte) { }
}
