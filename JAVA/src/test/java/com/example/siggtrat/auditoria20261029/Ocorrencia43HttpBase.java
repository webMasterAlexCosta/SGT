package com.example.siggtrat.auditoria20261029;

import com.example.siggtrat.entidade.*;
import com.example.siggtrat.servico.AuditoriaFrequenciaServico;
import com.example.siggtrat.utilitario.FotografiaJornada;
import com.fasterxml.jackson.databind.JsonNode;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.*;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;

/** Fixtures declaradamente sintéticas; nenhum serviço novo43 é referenciado pelo RED. */
abstract class Ocorrencia43HttpBase extends ContratosHttpBase {
    protected static final ZoneId ZONA43 = ZoneId.of("America/Sao_Paulo");
    @Autowired AuditoriaFrequenciaServico auditoria43;
    protected record Fonte43(long id, LocalDate inicio, String motivo, int dias, String observacao, String chave) {}

    protected Cenario cenario43() {
        Cenario c = criarCenarioPersistido();
        // Declara semana sintética de8h em todos os dias ANTES de qualquer fonte do cenário.
        // Não se trata de recuperar documento/fotografia autênticos nem de alterar fonte preexistente do usuário.
        emTransacao(() -> {
            for (Long jornadaId : List.of(c.jornadaA.getId(), c.jornadaB.getId())) {
                Jornada j = em.find(Jornada.class, jornadaId);
                j.getDias().forEach(d -> { d.setDiaTrabalho(true); d.setEntrada1("08:00"); d.setSaida1("12:00"); d.setEntrada2("13:00"); d.setSaida2("17:00"); });
                String foto = FotografiaJornada.capturar(j); em.flush();
                em.createQuery("update AlocacaoVinculo set jornadaSemanalFotografia=:f where jornada.id=:j")
                        .setParameter("f", foto).setParameter("j", jornadaId).executeUpdate();
                em.createQuery("update ComposicaoContratual set jornadaSemanalFotografia=:f where jornada.id=:j")
                        .setParameter("f", foto).setParameter("j", jornadaId).executeUpdate();
            }
            em.clear(); return null;
        });
        declararFonteNominalSintetica(c, "F43-" + UUID.randomUUID());
        return c;
    }

    protected Fonte43 fonteAnteriorComDiaHoje(Cenario c, String motivo) throws Exception {
        LocalDate ontem = LocalDate.now(ZONA43).minusDays(1);
        try (MockedStatic<LocalDate> calendario = Mockito.mockStatic(LocalDate.class, Mockito.CALLS_REAL_METHODS)) {
            calendario.when(() -> LocalDate.now(ZONA43)).thenReturn(ontem);
            return criarFonte43(c, ontem, motivo, 2);
        }
        // Ao sair, hoje volta a ser real também no worker assíncrono; não há prova física de virada de dia.
    }
    protected Fonte43 criarFonte43(Cenario c, LocalDate inicio, String motivo, int dias) throws Exception {
        String chave = "f43-" + UUID.randomUUID();
        String observacao = "Conteúdo clínico sintético restrito que não deve ser copiado " + UUID.randomUUID();
        var resultado = dados(executar(c.localA, multipart("/api/ocorrencias")
                .param("empregadoId", c.empregadoA.getId().toString()).param("dataOcorrencia", inicio.toString())
                .param("motivo", motivo).param("dias", Integer.toString(dias)).param("observacao", observacao)
                .param("chaveIdempotencia", chave), 201, "Fonte autorizada com contextos de todos os dias"));
        return new Fonte43(resultado.path("id").asLong(), inicio, motivo, dias, observacao, chave);
    }
    protected JsonNode reenviarFonte43(Cenario c, Fonte43 f) throws Exception {
        return dados(executar(c.localA, multipart("/api/ocorrencias")
                .param("empregadoId", c.empregadoA.getId().toString()).param("dataOcorrencia", f.inicio().toString())
                .param("motivo", f.motivo()).param("dias", Integer.toString(f.dias())).param("observacao", f.observacao())
                .param("chaveIdempotencia", f.chave()), 201, "Reenvio da intenção original não refaz frequência existente"));
    }
    protected Map<String,Object> presenca43(long vinculo, LocalDate dia, String chave) {
        return mapa("vinculoContratualId", vinculo, "dataReferencia", dia, "status", "PRESENCA",
                "entrada1", "08:00", "saida1", "12:00", "entrada2", "13:00", "saida2", "17:00",
                "observacao", "Apontamento físico de8h", "chaveIdempotencia", chave);
    }
    protected JsonNode gravar43(UsuarioAplicacao u, Map<String,Object> corpo, int status) throws Exception {
        return dados(executar(u, post("/api/frequencias").contentType(APPLICATION_JSON).content(json(corpo)),
                status, "Comando normal de Frequência com o ator real"));
    }
    protected long quantidade43(long vinculo, LocalDate dia) {
        return emTransacao(() -> em.createQuery("select count(f) from Frequencia f where f.vinculoContratual.id=:v and f.dataReferencia=:d", Long.class)
                .setParameter("v", vinculo).setParameter("d", dia).getSingleResult());
    }
    protected long frequencia43(long vinculo, LocalDate dia) {
        return emTransacao(() -> em.createQuery("select f.id from Frequencia f where f.vinculoContratual.id=:v and f.dataReferencia=:d", Long.class)
                .setParameter("v", vinculo).setParameter("d", dia).getSingleResult());
    }
    protected String snapshot43(long id) { return emTransacao(() -> auditoria43.snapshot(em.find(Frequencia.class, id))); }
    protected List<String> hashes43(long id) {
        return emTransacao(() -> em.createQuery("select e.hashEvento from EventoFrequencia e where e.frequencia.id=:id order by e.id", String.class)
                .setParameter("id", id).getResultList());
    }
    protected void fisico48043(long id) {
        emTransacao(() -> {
            Frequencia f = em.find(Frequencia.class, id);
            assertEquals("08:00", f.getEntrada1()); assertEquals("12:00", f.getSaida1());
            assertEquals("13:00", f.getEntrada2()); assertEquals("17:00", f.getSaida2());
            assertEquals(480, f.getMinutosFisicos()); assertEquals(480, f.getMinutosPagaveis());
            return null;
        });
    }
    protected String status43(long id) { return emTransacao(() -> em.find(Frequencia.class, id).getStatus()); }
    protected List<String> fotosFonte43(long id) {
        return emTransacao(() -> em.createQuery("select c.dataReferencia,c.contratoId,c.vinculoId,c.alocacaoId,c.gestaoLocalId,c.superintendenciaId,c.lotacaoId,c.jornadaId from ContextoOcorrencia c where c.ocorrencia.id=:id order by c.dataReferencia", Object[].class)
                .setParameter("id", id).getResultList().stream().map(java.util.Arrays::toString).toList());
    }
    protected long historicosFonte43(long id) {
        return emTransacao(() -> em.createQuery("select count(h) from HistoricoEmpregado h where h.ocorrencia.id=:id", Long.class)
                .setParameter("id", id).getSingleResult());
    }
    protected JsonNode aguardar43(UsuarioAplicacao u, long id) throws Exception {
        long limite = System.nanoTime() + java.util.concurrent.TimeUnit.SECONDS.toNanos(30);
        JsonNode atual;
        do {
            atual = consultar(u, "/api/frequencias/fechamentos/" + id, "Estado real do worker da frequência");
            if (List.of("CONCLUIDO", "ERRO", "CANCELADO").contains(atual.path("status").asText())) return atual;
            Thread.sleep(50);
        } while (System.nanoTime() < limite);
        fail("Fechamento não terminou: " + atual); return atual;
    }
}
