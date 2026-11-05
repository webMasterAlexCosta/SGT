package com.example.siggtrat.auditoria20261029;

import com.example.siggtrat.dominio.catalogo.TipoCatalogoTemporal;
import com.example.siggtrat.entidade.*;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

/** Rollback real: a recusa de catálogo acontece na sincronização, depois da invalidação antecipada. */
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class RetificacaoInicioEmissaoRollbackHttpIT extends ContratosHttpBase {
    @Test
    void recusaTardiaDaAlocacaoDesfazInicioInvalidacaoDaEmissaoEEventosNaMesmaTransacao() throws Exception {
        Cenario c = criarCenarioPersistido();
        declararFonteNominalSintetica(c, "VIN014-ROLLBACK-EMISSAO");
        LocalDate novoInicio = DATA.plusDays(1);
        emTransacao(() -> {
            EventoDisponibilidadeCatalogo inativacao = new EventoDisponibilidadeCatalogo();
            inativacao.setTipoCatalogo(TipoCatalogoTemporal.CARGO);
            inativacao.setCatalogoId(c.cargoA.getId());
            inativacao.setDataEfetiva(novoInicio);
            inativacao.setAtivo(false);
            inativacao.setMotivo("Fonte sintética: Cargo válido no dia emitido, inativo no novo início solicitado");
            em.persist(inativacao);
            return null;
        });
        Map<String, Object> filtro = mapa("tipo", "FREQUENCIA_CONSOLIDADA", "formato", "VISUALIZACAO",
                "inicio", DATA, "fim", DATA, "contratoIds", List.of(c.contratoA.getId()),
                "lotacaoId", c.lotacaoA.getId(), "jornadaId", c.jornadaA.getId(),
                "chaveIdempotencia", "vin014-rollback-" + UUID.randomUUID());
        JsonNode original = enviar(c.localA, "/api/relatorios/administrativos/emissoes", filtro,
                201, "VIN-014 emissão válida antes da inativação efetiva do Cargo");
        long emissaoId = original.path("metadados").path("id").asLong();
        assertEquals(1, original.path("conteudo").path("registros").size());
        assertEquals(480, original.path("conteudo").path("totais").path("minutosPrevistos").asInt());
        Foto antes = fotografia(c, emissaoId);
        assertEquals("VIGENTE", antes.estado());
        assertEquals(INICIO, antes.inicioVinculo());
        assertEquals(INICIO, antes.inicioAlocacao());
        assertEquals(0, antes.eventosAlocacao());

        String rota = "/api/vinculos-contratuais/" + c.vinculoA.getId();
        Map<String, Object> pedido = mapa("novoInicio", novoInicio,
                "justificativa", "Tentativa com catálogo inativo no novo início",
                "evidencia", "SEI-SINTETICO-VIN014-ROLLBACK", "autorizacao", "ATO-SINTETICO-VIN014");
        JsonNode previa = enviar(c.localA, rota + "/previas/retificacao-inicio", pedido,
                200, "A prévia de redução é válida; a disponibilidade será revalidada na sincronização");
        assertEquals(64, previa.path("token").asText().length());
        pedido.put("tokenPrevia", previa.path("token").asText());
        pedido.put("confirmarImpacto", true);
        var recusada = executar(c.localA, post(rota + "/retificar-inicio")
                        .contentType(MediaType.APPLICATION_JSON).content(json(pedido)), 409,
                "VIN-014 rollback de todo o comando após recusa do catálogo na Alocação");
        assertTrue(recusada.getResponse().getContentAsString().contains("inativo"),
                "A recusa deve ser a validação tardia do catálogo, não token ou preparação inválidos");

        Foto depois = fotografia(c, emissaoId);
        assertAll("A invalidação antecipada não pode escapar ao rollback do mesmo comando",
                () -> assertEquals(antes.inicioVinculo(), depois.inicioVinculo()),
                () -> assertEquals(antes.inicioAlocacao(), depois.inicioAlocacao()),
                () -> assertEquals("VIGENTE", depois.estado()),
                () -> assertNull(depois.causa()),
                () -> assertEquals(antes.version(), depois.version()),
                () -> assertEquals(antes.hash(), depois.hash()),
                () -> assertArrayEquals(antes.gzip(), depois.gzip()),
                () -> assertEquals(antes.eventosVinculo(), depois.eventosVinculo()),
                () -> assertEquals(antes.eventosAlocacao(), depois.eventosAlocacao()));
        JsonNode consultada = consultar(c.localA,
                "/api/relatorios/administrativos/emissoes/" + emissaoId,
                "REL-006 a emissão permanece íntegra e vigente depois da tentativa abortada");
        assertEquals("VIGENTE", consultada.path("metadados").path("estado").asText());
        assertEquals(original.path("conteudo"), consultada.path("conteudo"));
    }

    private Foto fotografia(Cenario c, long emissaoId) {
        return emTransacao(() -> {
            em.clear();
            EmissaoRelatorioAdministrativo e = em.find(EmissaoRelatorioAdministrativo.class, emissaoId);
            long eventosVinculo = em.createQuery(
                            "select count(e) from EventoVinculoContratual e where e.vinculo.id=:id", Long.class)
                    .setParameter("id", c.vinculoA.getId()).getSingleResult();
            long eventosAlocacao = em.createQuery(
                            "select count(e) from EventoAlocacaoVinculo e where e.vinculo.id=:id", Long.class)
                    .setParameter("id", c.vinculoA.getId()).getSingleResult();
            return new Foto(em.find(VinculoContratual.class, c.vinculoA.getId()).getDataInicio(),
                    em.find(AlocacaoVinculo.class, c.alocacaoA.getId()).getDataInicio(),
                    e.getEstado().name(), e.getCausaSuperacao() == null ? null : e.getCausaSuperacao().name(),
                    e.getVersion(), e.getHashConteudo(), e.getConteudoSnapshotGzip().clone(), eventosVinculo, eventosAlocacao);
        });
    }

    private record Foto(LocalDate inicioVinculo, LocalDate inicioAlocacao, String estado, String causa,
                        long version, String hash, byte[] gzip, long eventosVinculo, long eventosAlocacao) { }
}
