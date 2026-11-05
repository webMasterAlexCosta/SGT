package com.example.siggtrat.auditoria20261029;

import com.example.siggtrat.dominio.catalogo.TipoCatalogoTemporal;
import com.example.siggtrat.dominio.pendencia.EstadoPendencia;
import com.example.siggtrat.dominio.pendencia.TipoPendencia;
import com.example.siggtrat.entidade.*;
import com.example.siggtrat.utilitario.RelogioSistema;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/** Uma nova incompatibilidade da mesma origem não pode desaparecer no histórico já resolvido. */
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class ConclusaoAud01RecorrenciaPendenciaHttpIT extends ContratosHttpBase {
    private record Resolucao(Instant em, Long responsavel, String observacao, long versao) {}

    @Test
    void recorrenciaAbreNovaPendenciaSemReescreverResolucaoAnteriorNemDuplicarAtiva() throws Exception {
        Cenario c = criarCenarioPersistido();
        LocalDate inicio = RelogioSistema.hoje().minusDays(30), fim = RelogioSistema.hoje().plusDays(120);
        long cargo2 = emTransacao(() -> {
            Cargo cargo = new Cargo("Cargo AUD01 recorrência " + UUID.randomUUID(), "2");
            cargo.setGestaoLocal(em.find(GestaoLocal.class, c.gestaoA.getId())); em.persist(cargo); em.flush();
            EventoDisponibilidadeCatalogo fonte = new EventoDisponibilidadeCatalogo();
            fonte.setTipoCatalogo(TipoCatalogoTemporal.CARGO); fonte.setCatalogoId(cargo.getId());
            fonte.setDataEfetiva(INICIO); fonte.setAtivo(true);
            fonte.setMotivo("Fonte documental sintética do Cargo secundário desde o início do cenário");
            em.persist(fonte); return cargo.getId();
        });
        long contratoId = enviar(c.localA, "/api/contratos", rascunho(c,
                "REC-AUD01-" + UUID.randomUUID().toString().substring(0, 8), 100, inicio, fim), 201,
                "Fixture recorrência: contrato isolado").path("id").asLong();
        long comp1 = enviar(c.localA, base(contratoId) + "/composicoes", composicao(c, c.cargoA.getId(), 25),
                201, "Fixture recorrência: capacidade de origem 25").path("id").asLong();
        long comp2 = enviar(c.localA, base(contratoId) + "/composicoes", composicao(c, cargo2, 75),
                201, "Fixture recorrência: capacidade de destino 75").path("id").asLong();
        var publicacao = mapa("dataPublicacao", RelogioSistema.hoje(), "referenciaSei", "SEI-REC-ORIGINAL",
                "justificativa", "Fonte original sintética explicitamente publicada");
        confirmarPrevia(c, base(contratoId) + "/previas/publicar", publicacao);
        enviar(c.localA, base(contratoId) + "/publicar", publicacao, 200, "Publicação confirmada");
        NovoContrato n = new NovoContrato(contratoId, comp1, comp2, inicio, fim);
        var rem = remanejamento(n, "REM-REC-AUD01", inicio.plusDays(5), 20);
        confirmarPrevia(c, base(contratoId) + "/remanejamentos/previa", rem);
        long eventoOrigem = enviar(c.localA, base(contratoId) + "/remanejamentos", rem, 201,
                "Remanejamento consome 20 da capacidade inicial 25").path("eventoId").asLong();
        var reduzir = quantitativo(comp1, inicio.plusDays(2), 15, 90);
        confirmarPrevia(c, base(contratoId) + "/aditamentos/previa", reduzir);
        JsonNode primeiro = enviar(c.localA, base(contratoId) + "/aditamentos", reduzir, 201,
                "TMP-009 redução antecedente torna o remanejamento incompatível");
        long pendenciaOriginal = emTransacao(() -> pendencias(contratoId, eventoOrigem).get(0).getId());
        enviar(c.localA, "/api/pendencias/" + pendenciaOriginal + "/iniciar-tratamento",
                mapa("observacao", "Conferência documental da capacidade"), 200, "AUD-006 tratar a primeira ocorrência");

        JsonNode corrigido = retificar(c, contratoId, primeiro,
                quantitativo(comp1, inicio.plusDays(2), 22, 97));
        enviar(c.localA, "/api/pendencias/" + pendenciaOriginal + "/confirmar-resolucao",
                mapa("observacao", "Origem compatível com capacidade 22"), 200,
                "AUD-01 primeira causa efetivamente eliminada por retificação");
        Resolucao anterior = emTransacao(() -> fotografia(em.find(PendenciaAdministrativa.class, pendenciaOriginal)));

        retificar(c, contratoId, corrigido, quantitativo(comp1, inicio.plusDays(2), 15, 90));
        conferirRecorrencia(contratoId, eventoOrigem, pendenciaOriginal, anterior);

        var descritivo = aditamento("ADI-REC-DESCRICAO", inicio.plusDays(1));
        descritivo.put("novaDescricao", "Descrição documental retificada sem mudar capacidade");
        confirmarPrevia(c, base(contratoId) + "/aditamentos/previa", descritivo);
        enviar(c.localA, base(contratoId) + "/aditamentos", descritivo, 201,
                "AUD-005 outro replay da mesma causa ativa não duplica a nova ocorrência");
        conferirRecorrencia(contratoId, eventoOrigem, pendenciaOriginal, anterior);
    }

    private Map<String, Object> quantitativo(long composicao, LocalDate data, int capacidade, int total) {
        var req = aditamento("ADI-REC-AUD01", data);
        req.put("novoEfetivoTotal", total);
        req.put("alteracoesComposicao", List.of(mapa("composicaoId", composicao, "quantidadeResultante", capacidade)));
        return req;
    }

    private JsonNode retificar(Cenario c, long contratoId, JsonNode original, Map<String, Object> req) throws Exception {
        req.put("eventoPrecedenteId", original.path("eventoId").asLong()); req.put("relacaoPrecedencia", "RETIFICA");
        confirmarPrevia(c, base(contratoId) + "/aditamentos/previa", req);
        return enviar(c.localA, base(contratoId) + "/aditamentos/" + original.path("id").asLong() + "/retificar",
                req, 200, "ADI-008 retificação explícita do antecedente de capacidade");
    }

    private void confirmarPrevia(Cenario c, String path, Map<String, Object> req) throws Exception {
        JsonNode p = enviar(c.localA, path, req, 200, "Prévia documental da recorrência AUD01");
        req.put("tokenPrevia", p.path("tokenPrevia").asText()); req.put("confirmarImpacto", true);
    }

    private List<PendenciaAdministrativa> pendencias(long contratoId, long eventoId) {
        return em.createQuery("select p from PendenciaAdministrativa p where p.contratoId=:c and p.origemId=:e "
                        + "and p.tipo=:tipo order by p.id", PendenciaAdministrativa.class)
                .setParameter("c", contratoId).setParameter("e", eventoId)
                .setParameter("tipo", TipoPendencia.CONFLITO_REPLAY_TEMPORAL).getResultList();
    }

    private Resolucao fotografia(PendenciaAdministrativa p) {
        assertThat(p.getEstado()).isEqualTo(EstadoPendencia.RESOLVIDA);
        return new Resolucao(p.getResolvidaEm(), p.getResolvidaPorId(), p.getObservacaoTratamento(), p.getVersion());
    }

    private void conferirRecorrencia(long contratoId, long eventoId, long originalId, Resolucao resolucao) {
        emTransacao(() -> {
            List<PendenciaAdministrativa> tarefas = pendencias(contratoId, eventoId);
            assertThat(tarefas).as("A ocorrência resolvida permanece e uma nova tarefa ativa representa a recorrência").hasSize(2);
            PendenciaAdministrativa original = tarefas.stream().filter(p -> p.getId().equals(originalId)).findFirst().orElseThrow();
            assertThat(fotografia(original)).isEqualTo(resolucao);
            var ativas = tarefas.stream().filter(p -> p.getEstado() != EstadoPendencia.RESOLVIDA).toList();
            assertThat(ativas).hasSize(1);
            assertThat(ativas.get(0).getEstado()).isEqualTo(EstadoPendencia.ABERTA);
            assertThat(ativas.get(0).getId()).isNotEqualTo(originalId);
            assertThat(ativas.get(0).getResolvidaEm()).isNull();
            assertThat(em.find(EventoCicloVidaContrato.class, eventoId).getSituacaoFato()).isEqualTo(SituacaoFatoNegocial.VIGENTE);
            return null;
        });
    }
}
