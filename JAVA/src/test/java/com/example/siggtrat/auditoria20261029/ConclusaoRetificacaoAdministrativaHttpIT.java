package com.example.siggtrat.auditoria20261029;

import com.example.siggtrat.utilitario.RelogioSistema;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/** GOV003/TMP008/016/AUD001: retificação administrativa não se valida contra o efeito substituído. */
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class ConclusaoRetificacaoAdministrativaHttpIT extends ContratosHttpBase {
    @ParameterizedTest
    @ValueSource(strings = {"inativar", "reativar"})
    void retificacoesSucessivasPreservamCadeiaAdministrativaEConhecimentoAnterior(String operacao) throws Exception {
        var c = criarCenarioPersistido();
        var n = emTransacao(() -> { try { return publicado(c, 5, null, true); }
            catch (Exception e) { throw new IllegalStateException(e); } });
        LocalDate inativacao = n.inicio().plusDays(2), reativacao = n.inicio().plusDays(6);
        confirmar(c, n.id(), "inativar", acao(inativacao, "Inativação programada comprovada"));
        confirmar(c, n.id(), "reativar", acao(reativacao, "Reativação programada independente"));
        String tipo = operacao.equals("inativar") ? "INATIVACAO_ADMINISTRATIVA" : "REATIVACAO_ADMINISTRATIVA";
        long original = ultimoEvento(c, n.id(), tipo);
        LocalDate dataOriginal = operacao.equals("inativar") ? inativacao : reativacao;
        Instant conhecidoAntes = Instant.now();
        long precedente = original;
        for (int versao = 2; versao <= 3; versao++) {
            var req = acao(dataOriginal.plusDays(versao - 1), "Retificação documental da data, versão " + versao);
            req.put("eventoPrecedenteId", precedente); req.put("relacaoPrecedencia", "RETIFICA");
            confirmar(c, n.id(), operacao, req);
            precedente = ultimoEvento(c, n.id(), tipo);
        }
        String estadoNovo = operacao.equals("inativar") ? "INATIVO" : "ATIVO";
        String estadoAntes = operacao.equals("inativar") ? "ATIVO" : "INATIVO";
        assertThat(posicao(c, n.id(), dataOriginal, null).path("habilitacaoAdministrativa").asText()).isEqualTo(estadoAntes);
        assertThat(posicao(c, n.id(), dataOriginal.plusDays(2), null).path("habilitacaoAdministrativa").asText()).isEqualTo(estadoNovo);
        assertThat(posicao(c, n.id(), dataOriginal, conhecidoAntes).path("habilitacaoAdministrativa").asText()).isEqualTo(estadoNovo);
        if (operacao.equals("inativar"))
            assertThat(posicao(c, n.id(), reativacao, null).path("habilitacaoAdministrativa").asText()).isEqualTo("ATIVO");
        var eventos = consultar(c.localA, base(n.id()) + "/eventos", "GOV003 todas as versões administrativas preservadas");
        assertThat(eventos.size()).isEqualTo(5);
        var versoes = java.util.stream.StreamSupport.stream(eventos.spliterator(), false)
                .filter(e -> e.path("tipoEvento").asText().equals(tipo)).toList();
        assertThat(versoes).hasSize(3);
        assertThat(versoes.stream().filter(e -> e.path("situacaoFato").asText().equals("RETIFICADO")).count()).isEqualTo(2L);
        assertThat(versoes.stream().filter(e -> e.path("situacaoFato").asText().equals("VIGENTE")).count()).isEqualTo(1L);
    }

    private Map<String, Object> acao(LocalDate data, String motivo) {
        return mapa("dataEfetiva", data, "dataAto", RelogioSistema.hoje(), "motivo", motivo, "referenciaSei", "SEI-ADMINISTRATIVO");
    }
    private void confirmar(Cenario c, long id, String operacao, Map<String, Object> req) throws Exception {
        var previa = enviar(c.localA, base(id) + "/previas/" + operacao, req, 200, "AUD004 prévia administrativa atualizada");
        req.put("tokenPrevia", previa.path("tokenPrevia").asText()); req.put("confirmarImpacto", true);
        enviar(c.localA, base(id) + "/" + operacao, req, 200,
                "GOV003/TMP016 a correção explícita substitui a versão alvo e preserva os fatos independentes");
    }
    private long ultimoEvento(Cenario c, long id, String tipo) throws Exception {
        var eventos = consultar(c.localA, base(id) + "/eventos", "AUD001 versão vigente identificável");
        return java.util.stream.StreamSupport.stream(eventos.spliterator(), false)
                .filter(e -> e.path("tipoEvento").asText().equals(tipo) && e.path("situacaoFato").asText().equals("VIGENTE"))
                .findFirst().orElseThrow().path("id").asLong();
    }
    private JsonNode posicao(Cenario c, long id, LocalDate data, Instant conhecido) throws Exception {
        return consultar(c.localA, base(id) + "/posicao-temporal?data=" + data
                + (conhecido == null ? "" : "&conhecidoAte=" + conhecido), "HIS001 cortes efetivo e de conhecimento");
    }
}
