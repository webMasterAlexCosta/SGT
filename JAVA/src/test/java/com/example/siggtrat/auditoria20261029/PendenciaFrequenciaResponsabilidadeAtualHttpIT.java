package com.example.siggtrat.auditoria20261029;

import com.example.siggtrat.entidade.Frequencia;
import com.example.siggtrat.entidade.PendenciaAdministrativa;
import com.example.siggtrat.utilitario.RelogioSistema;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;

/** AUD006/FRE011/REL004: responsabilidade pelo tratamento não é a fotografia original da fonte. */
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class PendenciaFrequenciaResponsabilidadeAtualHttpIT extends ContratosHttpBase {
    private record Prova(Long gestao, Long lotacao, Long turno, String nome, Long autor, String status) {}
    private record Tarefa(Long id, String chave, String motivo, java.time.Instant surgida,
                          Long iniciadaPor, String estado, Long gestao, Long lotacao, Long turno) {}

    @Test
    void transferenciaRealinhaTodasAsRevisoesAbertasDeVersoesAnterioresSemAlterarSuaHistoria() throws Exception {
        Cenario c = criarCenarioPersistido();
        long frequenciaId = registrar(c, c.localA, "FALTA").path("id").asLong();
        Prova fotoOriginal = prova(frequenciaId);
        assertEquals("VALIDA", emTransacao(() -> em.find(Frequencia.class, frequenciaId).getValidadeOperacional().name()));
        JsonNode eventoOriginal = consultar(c.localA, "/api/frequencias/" + frequenciaId + "/eventos",
                "Fonte original antes de revisões sucessivas").get(0);
        // Dois atos válidos em datas distintas mudam a classificação sem tratar
        // a fonte: a inativação retira cobertura e a reativação exige conferência.
        cicloAdministrativo(c, "inativar", DATA.minusDays(1));
        assertEquals("INVALIDA_COBERTURA", emTransacao(() -> em.find(Frequencia.class, frequenciaId).getValidadeOperacional().name()));
        List<Tarefa> primeira = tarefas(frequenciaId);
        assertEquals(1, primeira.size());
        long antigaId = primeira.get(0).id();
        enviar(c.localA, "/api/pendencias/" + antigaId + "/iniciar-tratamento",
                mapa("observacao", "Conferência iniciada antes do novo ato documental"), 200,
                "Histórico de início do tratamento pertence ao autor original");
        cicloAdministrativo(c, "reativar", DATA);
        assertEquals("SUJEITA_REVISAO", emTransacao(() -> em.find(Frequencia.class, frequenciaId).getValidadeOperacional().name()));
        List<Tarefa> antes = tarefas(frequenciaId);
        assertEquals(2, antes.size(), "Duas versões pendentes são mantidas pelo contrato da central");
        assertEquals("EM_TRATAMENTO", antes.get(0).estado());
        assertEquals("ABERTA", antes.get(1).estado());
        assertEquals(2, antes.stream().map(Tarefa::chave).distinct().count());
        assertTrue(antes.stream().allMatch(t -> t.gestao().equals(c.gestaoA.getId())));
        conceder(c, c.contratoA.getId(), "TRANSFERENCIA_GESTAO_LOCAL");
        enviar(c.localA, base(c.contratoA.getId()) + "/transferir-gestao", mapa(
                "dataEfetiva", DATA.minusDays(2), "dataAto", RelogioSistema.hoje(),
                "gestaoLocalDestinoId", c.gestaoB.getId(), "tipoContratacaoDestinoId", c.tipoB.getId(),
                "motivo", "Responsabilidade documental sobre todas as revisões ainda abertas",
                "referenciaSei", "SEI-REVISOES-ABERTAS", "autorizacaoAdministrativa", "ATO-HTTP",
                "mapeamentos", List.of(mapa("composicaoOrigemId", c.composicaoA.getId(),
                        "lotacaoDestinoId", c.lotacaoB.getId(), "jornadaDestinoId", c.jornadaB.getId(),
                        "cargoDestinoId", c.cargoB.getId()))), 200,
                "Transferência não deixa tarefa ativa de versão anterior na organização antiga");
        List<Tarefa> depois = tarefas(frequenciaId);
        assertEquals(antes.stream().map(Tarefa::id).toList(), depois.stream().map(Tarefa::id).toList());
        for (int i = 0; i < antes.size(); i++) {
            Tarefa a = antes.get(i), d = depois.get(i);
            assertEquals(a.chave(), d.chave()); assertEquals(a.motivo(), d.motivo());
            assertEquals(a.surgida(), d.surgida()); assertEquals(a.estado(), d.estado());
            assertEquals(a.iniciadaPor(), d.iniciadaPor());
        }
        assertEquals(fotoOriginal, prova(frequenciaId));
        JsonNode origem = pendencias(c.localA, c), destino = pendencias(c.localB, c);
        JsonNode previa = enviar(c.localB, "/api/relatorios/administrativos/previa", filtro(c), 200,
                "Aviso inclui todas as revisões não resolvidas no escopo efetivo");
        assertAll(
                () -> assertTrue(depois.stream().allMatch(t -> c.gestaoB.getId().equals(t.gestao())
                        && c.lotacaoB.getId().equals(t.lotacao()) && c.jornadaB.getId().equals(t.turno())),
                        "Todas as versões abertas precisam do contexto responsável atual"),
                () -> assertTrue(antes.stream().noneMatch(t -> contem(origem, t.id())),
                        "A central antiga não deve conservar revisões que já não pode tratar"),
                () -> assertTrue(antes.stream().allMatch(t -> contem(destino, t.id())),
                        "O destino deve descobrir inclusive a versão anterior em tratamento"),
                () -> assertTrue(previa.path("metadados").path("sujeitoARevisao").asBoolean()),
                () -> assertEquals(2, previa.path("metadados").path("quantidadePendencias").asLong()));
        executar(c.localA, post("/api/pendencias/{id}/iniciar-tratamento", antigaId), 404,
                "Autoria antiga de início de tratamento não concede poder atual");
        enviar(c.localB, "/api/pendencias/" + antigaId + "/iniciar-tratamento",
                mapa("observacao", "Continuidade documentada pelo novo responsável"), 200,
                "Destino retoma tarefa anterior sem apagar quem iniciou o tratamento");
        registrar(c, c.localB, "PRESENCA");
        List<Tarefa> resolvidas = tarefas(frequenciaId);
        assertTrue(resolvidas.stream().allMatch(t -> t.estado().equals("RESOLVIDA")));
        assertEquals(c.localA.getId(), resolvidas.get(0).iniciadaPor());
        JsonNode eventos = consultar(c.localB, "/api/frequencias/" + frequenciaId + "/eventos",
                "Fonte e eventos originais conservados após resolução das duas versões");
        assertEquals(eventoOriginal, eventos.get(0));
        JsonNode relatorio = enviar(c.localB, "/api/relatorios/administrativos/previa", filtro(c), 200,
                "Retificação humana resolve as revisões da origem e retira o aviso");
        assertEquals(0, relatorio.path("metadados").path("quantidadePendencias").asLong());
        assertFalse(relatorio.path("metadados").path("sujeitoARevisao").asBoolean());
    }

    private void cicloAdministrativo(Cenario c, String comando, java.time.LocalDate data) throws Exception {
        Map<String, Object> req = mapa("dataEfetiva", data, "dataAto", RelogioSistema.hoje(),
                "motivo", "Ato administrativo documentado para conferir a cobertura da fonte",
                "referenciaSei", "SEI-REVISAO-" + comando);
        JsonNode previa = enviar(c.localA, base(c.contratoA.getId()) + "/previas/" + comando,
                req, 200, "Prévia do impacto administrativo sobre a frequência");
        req.put("tokenPrevia", previa.path("tokenPrevia").asText()); req.put("confirmarImpacto", true);
        enviar(c.localA, base(c.contratoA.getId()) + "/" + comando, req, 200,
                "Ato preserva a frequência e cria revisão sem resolução humana automática");
    }

    private List<Tarefa> tarefas(long frequenciaId) {
        return emTransacao(() -> em.createQuery("select p from PendenciaAdministrativa p where p.frequenciaId=:id order by p.id",
                        PendenciaAdministrativa.class).setParameter("id", frequenciaId).getResultList().stream()
                .map(p -> new Tarefa(p.getId(), p.getChaveOrigem(), p.getMotivo(), p.getSurgidaEm(),
                        p.getIniciadaPorId(), p.getEstado().name(), p.getGestaoLocalId(), p.getLotacaoId(), p.getJornadaId()))
                .toList());
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void transferenciaRetroativaEntregaPendenciaEAvisoAoResponsavelSemReescreverFonte(boolean novaTransferenciaComRevisaoAberta) throws Exception {
        Cenario c = criarCenarioPersistido();
        long frequenciaId = registrar(c, c.localA, "FALTA").path("id").asLong();
        Prova antes = prova(frequenciaId);
        JsonNode eventosAntes = consultar(c.localA, "/api/frequencias/" + frequenciaId + "/eventos",
                "Fotografia do lançamento antes da transferência");
        conceder(c, c.contratoA.getId(), "TRANSFERENCIA_GESTAO_LOCAL");
        enviar(c.localA, base(c.contratoA.getId()) + "/transferir-gestao", mapa(
                "dataEfetiva", DATA.minusDays(2), "dataAto", RelogioSistema.hoje(),
                "gestaoLocalDestinoId", c.gestaoB.getId(), "tipoContratacaoDestinoId", c.tipoB.getId(),
                "motivo", "Retificação documental da responsabilidade pelo período", "referenciaSei", "SEI-PENDENCIA-DESTINO",
                "autorizacaoAdministrativa", "ATO-HTTP", "mapeamentos", List.of(mapa(
                        "composicaoOrigemId", c.composicaoA.getId(), "lotacaoDestinoId", c.lotacaoB.getId(),
                        "jornadaDestinoId", c.jornadaB.getId(), "cargoDestinoId", c.cargoB.getId()))),
                200, "Transferência retroativa exige revisão humana da frequência já publicada");
        long pendenciaId = emTransacao(() -> {
            var f = em.find(Frequencia.class, frequenciaId);
            assertEquals("SUJEITA_REVISAO", f.getEstadoAuditoria().name());
            return em.createQuery("select p.id from PendenciaAdministrativa p where p.frequenciaId=:id", Long.class)
                    .setParameter("id", frequenciaId).getSingleResult();
        });
        if (novaTransferenciaComRevisaoAberta) {
            enviar(c.admin, "/api/permissoes-excepcionais", mapa("tipo", "TRANSFERENCIA_GESTAO_LOCAL",
                    "usuarioBeneficiarioId", c.localB.getId(), "contratoId", c.contratoA.getId(),
                    "finalidade", "Novo ato documental sobre revisão ainda aberta", "justificativa", "Conferência do ato de retorno",
                    "autorizacaoAdministrativa", "ATO-RETORNO", "validoAte", java.time.Instant.now().plusSeconds(3600)),
                    201, "Concessão própria do responsável por nova transferência");
            enviar(c.localB, base(c.contratoA.getId()) + "/transferir-gestao", mapa(
                    "dataEfetiva", DATA.minusDays(1), "dataAto", RelogioSistema.hoje(),
                    "gestaoLocalDestinoId", c.gestaoA.getId(), "tipoContratacaoDestinoId", c.tipoA.getId(),
                    "motivo", "Segundo ato documentado durante revisão não concluída", "referenciaSei", "SEI-PENDENCIA-RETORNO",
                    "autorizacaoAdministrativa", "ATO-RETORNO", "mapeamentos", List.of(mapa(
                            "lotacaoOrigemId", c.lotacaoB.getId(), "jornadaOrigemId", c.jornadaB.getId(), "cargoOrigemId", c.cargoB.getId(),
                            "lotacaoDestinoId", c.lotacaoA.getId(), "jornadaDestinoId", c.jornadaA.getId(), "cargoDestinoId", c.cargoA.getId()))),
                    200, "Revisão já aberta acompanha novo responsável sem duplicar pendência ou reescrever a fonte");
            assertEquals(List.of(pendenciaId), emTransacao(() -> em.createQuery(
                    "select p.id from PendenciaAdministrativa p where p.frequenciaId=:id order by p.id", Long.class)
                    .setParameter("id", frequenciaId).getResultList()));
        }
        var responsavel = novaTransferenciaComRevisaoAberta ? c.localA : c.localB;
        var anterior = novaTransferenciaComRevisaoAberta ? c.localB : c.localA;
        var gestaoEsperada = novaTransferenciaComRevisaoAberta ? c.gestaoA : c.gestaoB;
        var lotacaoEsperada = novaTransferenciaComRevisaoAberta ? c.lotacaoA : c.lotacaoB;
        var jornadaEsperada = novaTransferenciaComRevisaoAberta ? c.jornadaA : c.jornadaB;
        assertEquals(antes, prova(frequenciaId), "Diagnóstico não substitui os dados originais do lançamento");
        JsonNode origem = pendencias(anterior, c);
        JsonNode destino = pendencias(responsavel, c);
        JsonNode previa = enviar(responsavel, "/api/relatorios/administrativos/previa", filtro(c), 200,
                "Relatório do responsável efetivo deve avisar a revisão ainda não tratada");
        assertAll(
                () -> assertFalse(contem(origem, pendenciaId), "Origem não mantém responsabilidade de tratamento"),
                () -> assertTrue(contem(destino, pendenciaId), "Destino precisa descobrir o fato pendente"),
                () -> assertTrue(previa.path("metadados").path("sujeitoARevisao").asBoolean()),
                () -> assertEquals(1, previa.path("metadados").path("quantidadePendencias").asLong()));
        emTransacao(() -> {
            var p = em.find(PendenciaAdministrativa.class, pendenciaId);
            assertEquals(gestaoEsperada.getId(), p.getGestaoLocalId());
            assertEquals(lotacaoEsperada.getId(), p.getLotacaoId());
            assertEquals(jornadaEsperada.getId(), p.getJornadaId());
            assertEquals(antes.nome(), p.getPessoaFotografia()); return null;
        });
        executar(anterior, post("/api/pendencias/{id}/iniciar-tratamento", pendenciaId), 404,
                "Fotografia da fonte não concede à antiga Gestão poder sobre a pendência atual");
        enviar(responsavel, "/api/pendencias/" + pendenciaId + "/iniciar-tratamento",
                mapa("observacao", "Conferência documental na Gestão responsável"), 200,
                "Destino efetivo inicia o tratamento sem alterar autoria da fonte");
        registrar(c, responsavel, "PRESENCA");
        var resolvida = enviar(responsavel, "/api/pendencias/" + pendenciaId + "/confirmar-resolucao",
                mapa("observacao", "Frequência retificada e conferida no contexto efetivo"), 200,
                "Tratamento só é encerrado após a correção da origem");
        assertEquals("RESOLVIDA", resolvida.path("estado").asText());
        JsonNode eventosDepois = consultar(responsavel, "/api/frequencias/" + frequenciaId + "/eventos",
                "História preserva o lançamento original e acrescenta a retificação");
        assertEquals(eventosAntes.get(0), eventosDepois.get(0));
        assertTrue(eventosDepois.size() > eventosAntes.size());
        JsonNode atual = enviar(responsavel, "/api/relatorios/administrativos/previa", filtro(c), 200,
                "Relatório do destino deixa de ter pendência depois da resolução comprovada");
        assertFalse(atual.path("metadados").path("sujeitoARevisao").asBoolean());
        assertEquals(0, atual.path("metadados").path("quantidadePendencias").asLong());
    }

    private JsonNode registrar(Cenario c, com.example.siggtrat.entidade.UsuarioAplicacao user, String status) throws Exception {
        return enviar(user, "/api/frequencias", mapa("vinculoContratualId", c.vinculoA.getId(),
                "dataReferencia", DATA, "status", status, "justificativaRetificacao", "Documento comprova o lançamento no período",
                "chaveIdempotencia", "pendencia-responsavel-" + UUID.randomUUID()), 200,
                "Lançamento gerencial com motivo explícito e persistência confirmada");
    }

    private JsonNode pendencias(com.example.siggtrat.entidade.UsuarioAplicacao user, Cenario c) throws Exception {
        return dados(executar(user, get("/api/pendencias").param("inicio", DATA.toString()).param("fim", DATA.toString())
                .param("contratoIds", c.contratoA.getId().toString()), 200, "Pendências filtradas por responsabilidade atual"));
    }

    private Map<String, Object> filtro(Cenario c) {
        return mapa("tipo", "FREQUENCIA_CONSOLIDADA", "formato", "VISUALIZACAO", "inicio", DATA, "fim", DATA,
                "contratoIds", List.of(c.contratoA.getId()), "chaveIdempotencia", "relatorio-pendencia-" + UUID.randomUUID());
    }

    private boolean contem(JsonNode pagina, long id) {
        for (JsonNode item : pagina.path("registros")) if (item.path("id").asLong() == id) return true;
        return false;
    }

    private Prova prova(long id) {
        return emTransacao(() -> {
            var f = em.find(Frequencia.class, id);
            return new Prova(f.getGestaoLocalSnapshotId(), f.getLotacaoSnapshotId(), f.getJornadaSnapshotId(),
                    f.getEmpregadoSnapshotNome(), f.getAutorOriginalId(), f.getStatus());
        });
    }
}
