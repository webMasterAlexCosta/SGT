package com.example.siggtrat.auditoria20261029;

import com.example.siggtrat.entidade.EventoCicloVidaContrato;
import com.example.siggtrat.entidade.RelacaoPrecedenciaEventoContrato;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/** TMP016: relações positivas publicadas por HTTP, com reconsulta JPA após commit. */
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class RevisaoPrecedenciaExplicitaHttpIT extends ContratosHttpBase {
    @Test
    void substituicaoPublicadaPreservaOriginalFatoLogicoECorteDeConhecimento() throws Exception {
        var c = criarCenarioPersistido(); var n = publicar(c);
        var data = n.inicio().plusDays(1);
        var primeiro = aditamento("SUBSTITUICAO-" + n.id(), data);
        primeiro.put("novaDescricao", "Texto original antes da substituição documental");
        var a = registrar(c, n.id(), primeiro);
        Instant conhecimentoAnterior = Instant.now();
        var segundo = aditamento("SUBSTITUICAO-" + n.id(), data);
        segundo.put("novaDescricao", "Texto substituto explicitamente aprovado");
        segundo.put("eventoPrecedenteId", a.path("eventoId").asLong());
        segundo.put("relacaoPrecedencia", "SUBSTITUI");
        var b = registrar(c, n.id(), segundo);
        assertThat(b.path("versaoInstrumento").asInt()).isEqualTo(2);
        assertThat(b.path("id").asLong()).isNotEqualTo(a.path("id").asLong());
        var instrumentos = consultar(c.localA, base(n.id()) + "/aditamentos", "TMP016 substituição conserva ambos os instrumentos");
        assertThat(instrumentos).hasSize(2);
        assertThat(instrumentos.get(0).path("id").asLong()).isEqualTo(a.path("id").asLong());
        assertThat(instrumentos.get(0).path("situacao").asText()).isEqualTo("SUBSTITUIDO");
        assertThat(instrumentos.get(1).path("situacao").asText()).isEqualTo("VIGENTE");
        assertThat(consultar(c.localA, base(n.id()) + "/posicao-temporal?data=" + data,
                "TMP016 posição atual usa somente o substituto").path("versaoContratual").path("descricao").asText())
                .isEqualTo(segundo.get("novaDescricao"));
        assertThat(consultar(c.localA, base(n.id()) + "/posicao-temporal?data=" + data + "&conhecidoAte=" + conhecimentoAnterior,
                "TMP005 conhecimento anterior à substituição conserva o original").path("versaoContratual").path("descricao").asText())
                .isEqualTo(primeiro.get("novaDescricao"));
        conferirRelacao(n.id(), a.path("eventoId").asLong(), b.path("eventoId").asLong(), "SUBSTITUI", true);
        var relacoes = consultar(c.localA, base(n.id()) + "/eventos/precedencias", "TMP016 relação publicamente auditável");
        assertThat(relacoes).hasSize(1);
        assertThat(relacoes.get(0).path("tipoRelacao").asText()).isEqualTo("SUBSTITUI");
    }

    @Test
    void coexistenciaMantemCamposDistintosMasNaoAutorizaDuasDescricoesConflitantes() throws Exception {
        var c = criarCenarioPersistido(); var n = publicar(c);
        var data = n.inicio().plusDays(1);
        var reqA = aditamento("COEXISTENCIA-DESCRICAO-" + n.id(), data);
        reqA.put("novaDescricao", "Descrição do ato coexistente");
        var a = registrar(c, n.id(), reqA);
        var reqB = aditamento("COEXISTENCIA-EMPRESA-" + n.id(), data);
        reqB.put("novaRazaoSocialEmpresa", "Razão social do ato coexistente");
        reqB.put("eventoPrecedenteId", a.path("eventoId").asLong());
        reqB.put("relacaoPrecedencia", "MANTEM_COEXISTE");
        var b = registrar(c, n.id(), reqB);
        conferirRelacao(n.id(), a.path("eventoId").asLong(), b.path("eventoId").asLong(), "MANTEM_COEXISTE", false);
        var posicao = consultar(c.localA, base(n.id()) + "/posicao-temporal?data=" + data,
                "TMP016 campos independentes conservam os dois efeitos sem precedência técnica").path("versaoContratual");
        assertThat(posicao.path("descricao").asText()).isEqualTo(reqA.get("novaDescricao"));
        assertThat(posicao.path("razaoSocialEmpresa").asText()).isEqualTo(reqB.get("novaRazaoSocialEmpresa"));
        var eventosAntes = consultar(c.localA, base(n.id()) + "/eventos", "TMP016 estado antes da tentativa conflitante");
        var versoesAntes = consultar(c.localA, base(n.id()) + "/versoes", "TMP016 fotografias antes da tentativa conflitante");
        var conflito = aditamento("COEXISTENCIA-CONFLITANTE-" + n.id(), data);
        conflito.put("novaDescricao", "Outra descrição na mesma data");
        conflito.put("eventoPrecedenteId", a.path("eventoId").asLong());
        conflito.put("relacaoPrecedencia", "MANTEM_COEXISTE");
        var previa = enviar(c.localA, base(n.id()) + "/aditamentos/previa", conflito, 200,
                "TMP016 prévia não substitui validação de conflito na confirmação");
        conflito.put("tokenPrevia", previa.path("tokenPrevia").asText());
        var erro = enviar(c.localA, base(n.id()) + "/aditamentos", conflito, 409,
                "TMP016 coexistência não permite dois valores singulares do mesmo campo na mesma data");
        assertThat(erro.path("message").asText()).contains("MANTEM_COEXISTE");
        assertThat(consultar(c.localA, base(n.id()) + "/eventos", "TMP016 recusa não publica evento parcial")).isEqualTo(eventosAntes);
        assertThat(consultar(c.localA, base(n.id()) + "/versoes", "TMP016 recusa não publica fotografia parcial")).isEqualTo(versoesAntes);
        assertThat(consultar(c.localA, base(n.id()) + "/aditamentos", "TMP016 somente dois instrumentos válidos")).hasSize(2);
        emTransacao(() -> {
            assertThat(em.createQuery("select count(r) from RelacaoPrecedenciaEventoContrato r where r.contrato.id=:c", Long.class)
                    .setParameter("c", n.id()).getSingleResult()).isEqualTo(1L);
            return null;
        });
    }

    private void conferirRelacao(long contrato, long original, long novo, String tipo, boolean versiona) {
        emTransacao(() -> {
            var a = em.find(EventoCicloVidaContrato.class, original);
            var b = em.find(EventoCicloVidaContrato.class, novo);
            assertThat(a.getContrato().getId()).isEqualTo(contrato);
            assertThat(b.getContrato().getId()).isEqualTo(contrato);
            assertThat(a.getSituacaoFato().name()).isEqualTo(versiona ? "SUBSTITUIDO" : "VIGENTE");
            assertThat(b.getSituacaoFato().name()).isEqualTo("VIGENTE");
            if (versiona) {
                assertThat(b.getFatoLogicoId()).isEqualTo(a.getFatoLogicoId());
                assertThat(b.getVersaoNegocial()).isEqualTo(a.getVersaoNegocial() + 1);
            } else assertThat(b.getFatoLogicoId()).isNotEqualTo(a.getFatoLogicoId());
            var relacao = em.createQuery("select r from RelacaoPrecedenciaEventoContrato r where r.contrato.id=:c", RelacaoPrecedenciaEventoContrato.class)
                    .setParameter("c", contrato).getSingleResult();
            assertThat(relacao.getTipoRelacao().name()).isEqualTo(tipo);
            assertThat(relacao.getEventoFonte().getId()).isEqualTo(novo);
            assertThat(relacao.getEventoAlvo().getId()).isEqualTo(original);
            assertThat(relacao.getUsuarioResponsavelId()).isEqualTo(b.getUsuarioResponsavelId());
            assertThat(relacao.getRegistradoEm()).isNotNull();
            assertThat(relacao.getMotivo()).isNotBlank();
            return null;
        });
    }

    private JsonNode registrar(Cenario c, long contrato, Map<String,Object> req) throws Exception {
        var previa = enviar(c.localA, base(contrato) + "/aditamentos/previa", req, 200, "TMP016 prévia de instrumento documentado");
        req.put("tokenPrevia", previa.path("tokenPrevia").asText());
        return enviar(c.localA, base(contrato) + "/aditamentos", req, 201, "TMP016 publicação de relação explícita");
    }

    private NovoContrato publicar(Cenario c) {
        return emTransacao(() -> {
            try { return publicado(c, 5, null, true); }
            catch (Exception e) { throw new IllegalStateException(e); }
        });
    }
}
