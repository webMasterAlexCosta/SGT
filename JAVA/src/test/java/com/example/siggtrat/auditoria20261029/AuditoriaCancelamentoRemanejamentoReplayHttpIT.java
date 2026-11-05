package com.example.siggtrat.auditoria20261029;

import com.example.siggtrat.entidade.PendenciaAdministrativa;
import com.example.siggtrat.utilitario.RelogioSistema;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** REM-007/TMP-009: conflito e cancelamento atômicos, sem apagar fatos nem duplicar pendências. */
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class AuditoriaCancelamentoRemanejamentoReplayHttpIT extends ContratosHttpBase {
    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void cancelarRemanejamentoFuturoDeveSinalizarOInstrumentoPosteriorQueFicaIncompativel(boolean posteriorNaProrrogacao) throws Exception {
        var c = criarCenarioPersistido();
        NovoContrato n = emTransacao(() -> {
            try { return publicado(c, 10, 1, true); }
            catch (Exception ex) { throw new IllegalStateException(ex); }
        });
        var d1 = n.inicio().plusDays(2);
        var d2 = posteriorNaProrrogacao ? n.fim().plusDays(2) : n.inicio().plusDays(3);
        if (posteriorNaProrrogacao) {
            var extensao = aditamento("ADI-PRORROGA-ANTES-DOS-REM", n.inicio().plusDays(1));
            extensao.put("novaDataFim", n.fim().plusDays(30));
            var previa = enviar(c.localA, base(n.id()) + "/aditamentos/previa", extensao, 200,
                    "Prorrogação futura documental válida amplia o período de R2");
            extensao.put("tokenPrevia", previa.path("tokenPrevia").asText());
            enviar(c.localA, base(n.id()) + "/aditamentos", extensao, 201,
                    "Contrato atual não materializa antecipadamente a data final futura");
        }

        // Base A=1/B=9. R1 produz A=5/B=5; R2 depois retorna A=1/B=9.
        var req1 = remanejamento(n, "REM-BASE-POSTERIOR", d1, -4);
        var previa1 = enviar(c.localA, base(n.id()) + "/remanejamentos/previa", req1, 200, "Prévia de R1");
        req1.put("tokenPrevia", previa1.path("tokenPrevia").asText());
        var r1 = enviar(c.localA, base(n.id()) + "/remanejamentos", req1, 201,
                "REM-002 primeiro instrumento válido, sem pessoas ou dependentes artificiais");
        var req2 = remanejamento(n, "REM-DEPENDENTE-POSTERIOR", d2, 4);
        var previa2 = enviar(c.localA, base(n.id()) + "/remanejamentos/previa", req2, 200, "Prévia de R2");
        req2.put("tokenPrevia", previa2.path("tokenPrevia").asText());
        var r2 = enviar(c.localA, base(n.id()) + "/remanejamentos", req2, 201,
                "REM-002 segundo instrumento válido na linha temporal original");
        assertThat(quantidade(c, n, d2, n.composicao1())).isEqualTo(1);
        assertThat(quantidade(c, n, d2, n.composicao2())).isEqualTo(9);
        var originaisPersistidos = consultar(c.localA, base(n.id()) + "/remanejamentos",
                "Fotografias originais relidas após o commit, antes do cancelamento");

        enviar(c.localA, base(n.id()) + "/remanejamentos/" + r1.path("id").asLong() + "/cancelar",
                mapa("motivo", "Revogação documental do primeiro remanejamento antes do efeito"), 200,
                "REM-007 cancelamento preserva os fatos posteriores e deve sinalizar incompatibilidade");

        // Sem R1, o delta de R2 produziria A=-3. O replay deve preservar R2,
        // impedir o negativo e identificar o conflito na central de pendências.
        assertThat(quantidade(c, n, d2, n.composicao1())).isEqualTo(1);
        assertThat(quantidade(c, n, d2, n.composicao2())).isEqualTo(9);
        JsonNode instrumentos = consultar(c.localA, base(n.id()) + "/remanejamentos",
                "GOV-003 ambos os instrumentos permanecem auditáveis");
        assertThat(instrumentos).hasSize(2);
        assertThat(instrumentos.get(0).path("situacao").asText()).isEqualTo("CANCELADO");
        long pendencias = emTransacao(() -> em.createQuery("select count(p) from PendenciaAdministrativa p "
                        + "where p.contratoId=:contrato and p.origemId=:evento", Long.class)
                .setParameter("contrato", n.id())
                .setParameter("evento", r2.path("eventoId").asLong())
                .getSingleResult());
        assertThat(pendencias)
                .as("TMP-009/HIS-001: R2 ficou incompatível; o cancelamento não pode omitir o conflito")
                .isEqualTo(1);
        var pendencia = pendencias(n.id()).get(0);
        assertThat(pendencia.getTipo().name()).isEqualTo("CONFLITO_REPLAY_TEMPORAL");
        assertThat(pendencia.getEstado().name()).isEqualTo("ABERTA");
        assertThat(pendencia.getOrigemEntidade()).isEqualTo("EventoCicloVidaContrato");
        assertThat(pendencia.getOrigemId()).isEqualTo(r2.path("eventoId").asLong());
        assertThat(pendencia.getDataEfetiva()).isEqualTo(d2);
        assertThat(pendencia.getOperacaoOrigem()).isEqualTo("CANCELAMENTO_REMANEJAMENTO");
        assertThat(pendencia.getGestaoLocalId()).isEqualTo(c.gestaoA.getId());
        assertThat(pendencia.getMotivo()).isNotBlank();
        assertThat(pendencia.getSurgidaEm()).isNotNull();
        assertInstrumentoPreservado(instrumentos.get(0), originaisPersistidos.get(0), "CANCELADO");
        assertInstrumentoPreservado(instrumentos.get(1), originaisPersistidos.get(1), "VIGENTE");
        var relacoes = consultar(c.localA, base(n.id()) + "/eventos/precedencias",
                "REM-007 o cancelamento aponta o instrumento original, sem retificar o posterior");
        assertThat(relacoes).hasSize(1);
        assertThat(relacoes.get(0).path("tipoRelacao").asText()).isEqualTo("CANCELA");
        assertThat(relacoes.get(0).path("eventoAlvoId").asLong()).isEqualTo(r1.path("eventoId").asLong());
    }

    @Test
    void cancelamentoComPosteriorCompativelPreservaDeltasEHistoricoSemCriarPendencia() throws Exception {
        var c = criarCenarioPersistido();
        var n = prepararContrato(c);
        var d1 = n.inicio().plusDays(2);
        var d2 = d1.plusDays(2);
        var r1 = registrarRemanejamento(c, n, "REM-CANCELAMENTO-LEGITIMO", d1, -4);
        var r2 = registrarRemanejamento(c, n, "REM-POSTERIOR-COMPATIVEL", d2, -1);
        // Antes: 1/9 -> 5/5 -> 6/4. Retirar somente R1 deve resultar em 1/9 -> 2/8.
        assertThat(quantidade(c, n, d2, n.composicao1())).isEqualTo(6);
        assertThat(quantidade(c, n, d2, n.composicao2())).isEqualTo(4);

        cancelar(c, n, r1, 200);

        assertThat(quantidade(c, n, d1, n.composicao1())).isEqualTo(1);
        assertThat(quantidade(c, n, d1, n.composicao2())).isEqualTo(9);
        assertThat(quantidade(c, n, d2, n.composicao1())).isEqualTo(2);
        assertThat(quantidade(c, n, d2, n.composicao2())).isEqualTo(8);
        assertThat(pendencias(n.id())).isEmpty();
        var instrumentos = consultar(c.localA, base(n.id()) + "/remanejamentos",
                "REM-007 cancelamento não reescreve a fotografia quantitativa do posterior válido");
        assertThat(instrumentos).hasSize(2);
        assertInstrumentoPreservado(instrumentos.get(0), r1, "CANCELADO");
        assertInstrumentoPreservado(instrumentos.get(1), r2, "VIGENTE");
        var eventos = consultar(c.localA, base(n.id()) + "/eventos", "REM-007 publicação, dois REM e um cancelamento");
        assertThat(eventos).hasSize(4);
        JsonNode eventoCancelamento = null;
        for (var evento : eventos) {
            if (evento.path("tipoEvento").asText().equals("CANCELAMENTO_REMANEJAMENTO")) eventoCancelamento = evento;
        }
        assertThat(eventoCancelamento).isNotNull();
        assertThat(eventoCancelamento.path("motivo").asText()).isEqualTo("Revogação documental antes do efeito");
        assertThat(eventoCancelamento.path("referenciaSei").asText()).isEqualTo("SEI-CANCELAMENTO-REM-REPLAY");
        assertThat(eventoCancelamento.path("usuarioResponsavelId").asLong()).isEqualTo(c.localA.getId());
        assertThat(eventoCancelamento.path("dataAto").asText()).isEqualTo(RelogioSistema.hoje().toString());
        assertThat(eventoCancelamento.path("dataEfetiva").asText()).isEqualTo(d1.toString());
        assertThat(eventoCancelamento.path("registradoEm").asText()).isNotBlank();
    }

    @Test
    void doisCancelamentosQueAtingemMesmoPosteriorReutilizamPendenciaAberta() throws Exception {
        var c = criarCenarioPersistido();
        var n = prepararContrato(c);
        var d1 = n.inicio().plusDays(2);
        var d2 = d1.plusDays(2);
        var r1 = registrarRemanejamento(c, n, "REM-ANTECEDENTE-QUATRO", d1, -4);
        var intermediario = registrarRemanejamento(c, n, "REM-ANTECEDENTE-UM", d1.plusDays(1), -1);
        var r2 = registrarRemanejamento(c, n, "REM-POSTERIOR-DEPENDENTE-DE-CINCO", d2, 5);
        // 1/9 -> 5/5 -> 6/4 -> 1/9. Cada cancelamento mantém R2 incompatível.
        cancelar(c, n, r1, 200);
        var primeiraLeitura = pendencias(n.id());
        assertThat(primeiraLeitura).hasSize(1);
        var primeira = primeiraLeitura.get(0);
        assertThat(primeira.getOrigemId()).isEqualTo(r2.path("eventoId").asLong());
        assertThat(quantidade(c, n, d2, n.composicao1())).isEqualTo(2);
        assertThat(quantidade(c, n, d2, n.composicao2())).isEqualTo(8);

        cancelar(c, n, intermediario, 200);

        var segundaLeitura = pendencias(n.id());
        assertThat(segundaLeitura).hasSize(1);
        assertThat(segundaLeitura.get(0).getId()).isEqualTo(primeira.getId());
        assertThat(segundaLeitura.get(0).getChaveOrigem()).isEqualTo(primeira.getChaveOrigem());
        assertThat(segundaLeitura.get(0).getSurgidaEm()).isEqualTo(primeira.getSurgidaEm());
        assertThat(segundaLeitura.get(0).getEstado().name()).isEqualTo("ABERTA");
        assertThat(quantidade(c, n, d2, n.composicao1())).isEqualTo(1);
        assertThat(quantidade(c, n, d2, n.composicao2())).isEqualTo(9);
        var instrumentos = consultar(c.localA, base(n.id()) + "/remanejamentos", "Dois cancelamentos conservam os três instrumentos");
        assertThat(instrumentos).hasSize(3);
        assertInstrumentoPreservado(instrumentos.get(0), r1, "CANCELADO");
        assertInstrumentoPreservado(instrumentos.get(1), intermediario, "CANCELADO");
        assertInstrumentoPreservado(instrumentos.get(2), r2, "VIGENTE");
        assertThat(consultar(c.localA, base(n.id()) + "/eventos", "Somente dois cancelamentos foram publicados")).hasSize(6);
        cancelar(c, n, intermediario, 409);
        assertThat(pendencias(n.id())).hasSize(1);
        assertThat(consultar(c.localA, base(n.id()) + "/eventos", "Repetição recusada não cria evento extra")).hasSize(6);
    }

    @Test
    void rollbackDoCancelamentoTambemDesfazPendenciaERelacaoNaMesmaTransacao() throws Exception {
        var c = criarCenarioPersistido();
        var n = prepararContrato(c);
        var d1 = n.inicio().plusDays(2);
        var d2 = d1.plusDays(1);
        var r1 = registrarRemanejamento(c, n, "REM-ROLLBACK-ANTECEDENTE", d1, -4);
        var r2 = registrarRemanejamento(c, n, "REM-ROLLBACK-POSTERIOR", d2, 4);

        assertThatThrownBy(() -> emTransacao(() -> {
            try {
                cancelar(c, n, r1, 200);
                em.flush();
                em.clear();
                assertThat(pendencias(n.id())).hasSize(1);
                var dentro = consultar(c.localA, base(n.id()) + "/remanejamentos",
                        "Cancelamento e pendência existem juntos antes do commit");
                assertThat(dentro.get(0).path("situacao").asText()).isEqualTo("CANCELADO");
                assertThat(consultar(c.localA, base(n.id()) + "/eventos/precedencias",
                        "A relação CANCELA pertence à mesma transação")).hasSize(1);
            } catch (Exception ex) {
                throw new IllegalStateException(ex);
            }
            throw new FalhaAntesDoCommit();
        })).isInstanceOf(FalhaAntesDoCommit.class);

        assertThat(pendencias(n.id())).isEmpty();
        assertThat(consultar(c.localA, base(n.id()) + "/eventos", "Nova transação não vê cancelamento abortado")).hasSize(3);
        assertThat(consultar(c.localA, base(n.id()) + "/eventos/precedencias", "Relação abortada não permanece")).isEmpty();
        var depois = consultar(c.localA, base(n.id()) + "/remanejamentos", "Rollback restaura as duas situações originais");
        assertInstrumentoPreservado(depois.get(0), r1, "VIGENTE");
        assertInstrumentoPreservado(depois.get(1), r2, "VIGENTE");
        assertThat(quantidade(c, n, d1, n.composicao1())).isEqualTo(5);
        assertThat(quantidade(c, n, d1, n.composicao2())).isEqualTo(5);
        assertThat(quantidade(c, n, d2, n.composicao1())).isEqualTo(1);
        assertThat(quantidade(c, n, d2, n.composicao2())).isEqualTo(9);

        cancelar(c, n, r1, 200);
        assertThat(pendencias(n.id())).hasSize(1);
        assertThat(consultar(c.localA, base(n.id()) + "/eventos", "Nova tentativa confirma exatamente um cancelamento")).hasSize(4);
    }

    private NovoContrato prepararContrato(Cenario c) {
        return emTransacao(() -> {
            try { return publicado(c, 10, 1, true); }
            catch (Exception ex) { throw new IllegalStateException(ex); }
        });
    }

    private JsonNode registrarRemanejamento(Cenario c, NovoContrato n, String identificacao, LocalDate data, int variacao) throws Exception {
        var req = remanejamento(n, identificacao, data, variacao);
        var previa = enviar(c.localA, base(n.id()) + "/remanejamentos/previa", req, 200, "Prévia independente de " + identificacao);
        req.put("tokenPrevia", previa.path("tokenPrevia").asText());
        var registrado = enviar(c.localA, base(n.id()) + "/remanejamentos", req, 201, "Registro documentado de " + identificacao);
        var persistidos = consultar(c.localA, base(n.id()) + "/remanejamentos", "Fotografia confirmada de " + identificacao);
        for (var persistido : persistidos) {
            if (persistido.path("id").asLong() == registrado.path("id").asLong()) return persistido;
        }
        throw new AssertionError("Instrumento recém-confirmado não foi encontrado: " + identificacao);
    }

    private JsonNode cancelar(Cenario c, NovoContrato n, JsonNode original, int status) throws Exception {
        return enviar(c.localA, base(n.id()) + "/remanejamentos/" + original.path("id").asLong() + "/cancelar",
                mapa("dataAto", RelogioSistema.hoje(), "motivo", "Revogação documental antes do efeito",
                        "referenciaSei", "SEI-CANCELAMENTO-REM-REPLAY"), status, "Cancelamento explícito de REM");
    }

    private List<PendenciaAdministrativa> pendencias(long contratoId) {
        return emTransacao(() -> em.createQuery("select p from PendenciaAdministrativa p where p.contratoId=:contrato order by p.id",
                        PendenciaAdministrativa.class)
                .setParameter("contrato", contratoId).getResultList());
    }

    private void assertInstrumentoPreservado(JsonNode atual, JsonNode original, String situacao) {
        for (String campo : List.of("id", "contratoId", "eventoId", "identificacao", "versaoInstrumento", "dataEfetiva",
                "justificativa", "referenciaSei", "registradoEm", "usuarioResponsavelId", "usuarioResponsavelUsername", "somaVariacoes", "itens")) {
            assertThat(atual.path(campo)).as("Fotografia original de " + campo).isEqualTo(original.path(campo));
        }
        assertThat(atual.path("situacao").asText()).isEqualTo(situacao);
    }

    private static class FalhaAntesDoCommit extends RuntimeException {}

    private int quantidade(Cenario c, NovoContrato n, LocalDate data, long composicao) throws Exception {
        JsonNode itens = consultar(c.localA, base(n.id()) + "/composicoes?data=" + data,
                "CMP-003 reconstrução da composição após reaplicação dos fatos válidos");
        for (JsonNode item : itens) {
            if (item.path("id").asLong() == composicao) return item.path("efetivoPrevisto").asInt();
        }
        throw new AssertionError("Composição esperada ausente: " + composicao);
    }
}
