package com.example.siggtrat.auditoria20261029;

import com.example.siggtrat.dominio.acesso.PerfilAcesso;
import com.example.siggtrat.dominio.catalogo.TipoCatalogoTemporal;
import com.example.siggtrat.entidade.*;
import com.example.siggtrat.servico.PoliticaTemporalFrequenciaServico;
import com.example.siggtrat.utilitario.RelogioSistema;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.*;
import java.time.temporal.TemporalAdjusters;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/** B06: fonte preexistente e comando real; dados/fontes inteiramente sintéticos. */
@TestPropertySource(properties = "frequencia.janela-operacional-dias=30")
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class FrequenciaFontePreservadaB06HttpIT extends ContratosHttpBase {
    @Autowired PoliticaTemporalFrequenciaServico politica;

    @Test
    void retificacaoRealPreservaFrequenciaAnteriorESuaAutoriaSemTransferirEdicaoAoDestino() throws Exception {
        Cenario c = criarCenarioPersistido();
        declararFonteNominalSintetica(c, "B06-FONTE-PREEXISTENTE");
        LocalDate dia = RelogioSistema.hoje().minusDays(2).with(TemporalAdjusters.previousOrSame(DayOfWeek.WEDNESDAY));
        assertTrue(politica.abertaParaOperacional(dia, null));
        assertFalse(dia.isBefore(INICIO));
        JsonNode criada = gravarB06(c.operacionalA, c.vinculoA.getId(), dia, "FALTA");
        long frequenciaId = criada.path("id").asLong();
        JsonNode editadaPeloOriginal = gravarB06(c.operacionalA, c.vinculoA.getId(), dia, "PRESENCA");
        assertEquals(frequenciaId, editadaPeloOriginal.path("id").asLong(), "Controle positivo do autor original antes da retificação");
        assertEquals("ALTERADA_OPERACIONAL", editadaPeloOriginal.path("estadoAuditoria").asText());
        List<Object> fonteAntes = fonteB06(frequenciaId);
        List<List<Object>> eventosAntes = eventosB06(frequenciaId);
        assertEquals(2, eventosAntes.size());
        assertEquals(c.operacionalA.getId(), fonteAntes.get(5));
        assertEquals(c.alocacaoA.getId(), fonteAntes.get(4));

        DestinoB06 destino = emTransacao(() -> {
            GestaoLocal gestao = em.find(GestaoLocal.class, c.gestaoA.getId());
            Jornada jornada = em.find(Jornada.class, c.jornadaA.getId());
            Lotacao l = new Lotacao("Destino sintético B06 " + UUID.randomUUID());
            l.setGestaoLocal(gestao); l.setMunicipio(c.lotacaoA.getMunicipio()); l.getTurnos().add(jornada);
            em.persist(l); em.flush();
            EventoDisponibilidadeCatalogo disponibilidade = new EventoDisponibilidadeCatalogo();
            disponibilidade.setTipoCatalogo(TipoCatalogoTemporal.LOTACAO); disponibilidade.setCatalogoId(l.getId());
            disponibilidade.setDataEfetiva(INICIO); disponibilidade.setAtivo(true);
            disponibilidade.setMotivo("Fonte sintética explícita de existência do destino desde INICIO");
            em.persist(disponibilidade);
            ComposicaoContratual composicao = composicao(em.find(Contrato.class, c.contratoA.getId()), l, jornada,
                    em.find(Cargo.class, c.cargoA.getId()), 5);
            UsuarioAplicacao operador = usuario(PerfilAcesso.OPERACIONAL, gestao);
            operador.setLotacao(l); operador.setJornada(jornada); em.flush();
            return new DestinoB06(composicao.getId(), l.getId(), operador);
        });
        String rota = "/api/vinculos-contratuais/" + c.vinculoA.getId();
        var requisicao = mapa("dataEfetiva", INICIO, "composicaoId", destino.composicaoId(),
                "justificativa", "Retificação sintética real posterior à frequência já persistida",
                "evidencia", "SEI-SINTETICO-B06-RETIFICACAO", "autorizacao", "ATO-SINTETICO-B06");
        JsonNode previa = enviar(c.localA, rota + "/previas/alocacoes", requisicao, 200, "FRE016 prévia de retificação com fonte preexistente");
        assertTrue(previa.path("incompatibilidades").isEmpty());
        requisicao.put("confirmarImpacto", true); requisicao.put("tokenPrevia", previa.path("token").asText());
        JsonNode retificada = enviar(c.localA, rota + "/alocacoes/" + c.alocacaoA.getId() + "/retificar", requisicao, 200,
                "FRE-016:001[0] comando real de retificação depois da mesma Frequência");
        assertNotEquals(c.alocacaoA.getId().longValue(), retificada.path("id").asLong());
        assertEquals(destino.lotacaoId(), retificada.path("lotacaoId").asLong());
        assertEquals(fonteAntes, fonteB06(frequenciaId), "FRE-016:001[1] ID, Pessoa, Vínculo, Alocação, autor e fotografias da fonte preservados");
        emTransacao(() -> {
            em.flush(); em.clear();
            Frequencia fonte = em.find(Frequencia.class, frequenciaId);
            assertEquals(c.pessoaA.getId(), fonte.getPessoa().getId());
            assertEquals(c.vinculoA.getId(), fonte.getVinculoContratual().getId());
            assertEquals(c.alocacaoA.getId(), fonte.getAlocacaoVinculo().getId());
            assertEquals(c.operacionalA.getId(), fonte.getAutorOriginalId());
            assertEquals("SUJEITA_REVISAO", fonte.getValidadeOperacional().name());
            assertEquals("SUJEITA_REVISAO", fonte.getEstadoAuditoria().name());
            assertTrue(fonte.getBloqueadaEdicaoOperacional());
            assertEquals(editadaPeloOriginal.path("versaoNegocial").asInt() + 1, fonte.getVersaoNegocial());
            assertEquals(1L, em.createQuery("select count(f) from Frequencia f where f.vinculoContratual.id=:v and f.dataReferencia=:d", Long.class)
                    .setParameter("v", c.vinculoA.getId()).setParameter("d", dia).getSingleResult());
            return null;
        });
        List<List<Object>> eventosDepois = eventosB06(frequenciaId);
        assertEquals(eventosAntes.size() + 1, eventosDepois.size());
        assertEquals(eventosAntes, eventosDepois.subList(0, eventosAntes.size()), "Eventos anteriores permanecem com os mesmos IDs, snapshots e hashes");
        assertEquals("IMPACTO_TEMPORAL", eventosDepois.get(eventosDepois.size() - 1).get(2).toString());
        assertEquals(eventosAntes.get(eventosAntes.size() - 1).get(7), eventosDepois.get(eventosDepois.size() - 1).get(6));

        JsonNode aposImpacto = estadoCompletoB06(frequenciaId);
        for (UsuarioAplicacao operador : List.of(destino.operador(), c.operacionalA)) {
            enviar(operador, "/api/frequencias", corpoB06(c.vinculoA.getId(), dia, "FALTA"), 403,
                    "FRE-016:001[2] nova responsabilidade não herda edição e a fonte em revisão também não volta ao original");
            assertEquals(aposImpacto, estadoCompletoB06(frequenciaId), "Recusa preserva a mesma frequência e toda a trilha selecionada");
        }
        JsonNode controleDestino = gravarB06(destino.operador(), c.vinculoA.getId(), dia.plusDays(1), "PRESENCA");
        assertNotEquals(frequenciaId, controleDestino.path("id").asLong());
        assertEquals(retificada.path("id").asLong(), controleDestino.path("alocacaoVinculoId").asLong());
        assertEquals(aposImpacto, estadoCompletoB06(frequenciaId), "Controle separado do destino não substitui a fonte preexistente");
    }

    private JsonNode gravarB06(UsuarioAplicacao ator, long vinculo, LocalDate dia, String status) throws Exception {
        return enviar(ator, "/api/frequencias", corpoB06(vinculo, dia, status), 200, "B06 gravação operacional dentro da janela vigente");
    }
    private Map<String,Object> corpoB06(long vinculo, LocalDate dia, String status) {
        var r = mapa("vinculoContratualId", vinculo, "dataReferencia", dia, "status", status,
                "chaveIdempotencia", "B06-" + UUID.randomUUID());
        if (status.equals("PRESENCA")) r.putAll(mapa("entrada1", "08:00", "saida1", "12:00", "entrada2", "13:00", "saida2", "17:00"));
        return r;
    }
    private List<Object> fonteB06(long id) {
        return emTransacao(() -> {
            em.clear();
            return Arrays.asList(em.createQuery("select f.id,f.pessoa.id,f.empregado.id,f.vinculoContratual.id,f.alocacaoVinculo.id,"
                    + "f.autorOriginalId,f.autorOriginalUsername,f.dataReferencia,f.contratoSnapshotId,f.contratoSnapshotNumero,"
                    + "f.gestaoLocalSnapshotId,f.superintendenciaSnapshotId,f.lotacaoSnapshotId,f.lotacaoSnapshotNome,f.lotacaoSnapshotMunicipio,"
                    + "f.jornadaSnapshotId,f.jornadaSnapshotDescricao,f.jornadaSnapshotDiaTrabalho,f.jornadaSnapshotEntrada1,f.jornadaSnapshotSaida1,"
                    + "f.jornadaSnapshotEntrada2,f.jornadaSnapshotSaida2,f.cargoSnapshotId,f.cargoSnapshotNome,f.empregadoSnapshotNome "
                    + "from Frequencia f where f.id=:id", Object[].class).setParameter("id", id).getSingleResult());
        });
    }
    private List<List<Object>> eventosB06(long id) {
        return emTransacao(() -> em.createQuery("select e.id,e.frequencia.id,e.tipoEvento,e.numeroVersao,e.snapshotAnterior,e.snapshotNovo,"
                + "e.hashAnterior,e.hashEvento,e.usuarioId,e.usuarioUsername,e.registradoEm from EventoFrequencia e where e.frequencia.id=:id order by e.id", Object[].class)
                .setParameter("id", id).getResultList().stream().map(Arrays::asList).toList());
    }
    private JsonNode estadoCompletoB06(long id) {
        return emTransacao(() -> {
            em.clear();
            var f = em.find(Frequencia.class, id);
            return mapper.valueToTree(mapa("fonte", fonteB06(id), "estado", Arrays.asList(f.getStatus(), f.getEntrada1(), f.getSaida1(),
                    f.getEntrada2(), f.getSaida2(), f.getEstadoNegocial(), f.getEstadoAuditoria(), f.getValidadeOperacional(),
                    f.getBloqueadaEdicaoOperacional(), f.getVersaoNegocial(), f.getVersion(), f.getUltimaRetificacaoPorId(), f.getMotivoRevisao()),
                    "eventos", eventosB06(id)));
        });
    }
    private record DestinoB06(long composicaoId, long lotacaoId, UsuarioAplicacao operador) {}
}
