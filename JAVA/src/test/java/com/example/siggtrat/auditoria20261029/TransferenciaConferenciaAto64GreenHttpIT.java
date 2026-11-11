package com.example.siggtrat.auditoria20261029;

import com.example.siggtrat.entidade.*;
import com.example.siggtrat.regra.MaterialHashEventos;
import com.example.siggtrat.utilitario.RelogioSistema;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.annotation.*;
import java.time.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

/** GREEN proposto, ainda não executado. Cada leitura após HTTP usa outra transação, sem transação ambiente do teste. */
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class TransferenciaConferenciaAto64GreenHttpIT extends TransferenciaConferencia64HttpBase {
    @Test
    void intencaoCompletaEConferenciaLigamEventoAoUsoExatoComJPACommitado() throws Exception {
        var f = preparar64(true); var d = f.n().inicio().plusDays(2); var req = intencao64(f, d); declarar64(req);
        req.put("autorizacaoAdministrativa", "ATO-SINTETICO64-DO-ATO|distinto");
        enviar(f.c().localA, base(f.n().id()) + "/transferir-gestao", req, 200, "64 conferência humana sintética fixa esta intenção");
        var prova = conferir64(f, transferencia64(f));
        var intencao = mapper.readTree(prova.canonico());
        assertEquals(f.n().id(), intencao.path("contratoId").asLong());
        assertEquals("TRANSFERIR_GESTAO", intencao.path("operacao").asText());
        assertEquals(d.toString(), intencao.path("dataEfetiva").asText());
        assertEquals(f.n().inicio().toString(), intencao.path("inicioVigencia").asText());
        assertEquals(f.n().fim().toString(), intencao.path("fimVigencia").asText());
        assertEquals(f.c().gestaoA.getId().longValue(), intencao.path("gestaoOrigemId").asLong());
        assertEquals(f.c().gestaoB.getId().longValue(), intencao.path("gestaoDestinoId").asLong());
        assertEquals(f.c().tipoB.getId().longValue(), intencao.path("tipoDestinoId").asLong());
        var mapa = intencao.path("mapeamentos").get(0);
        assertEquals(f.n().composicao1(), mapa.path("composicaoOrigemId").asLong());
        assertEquals(f.c().lotacaoA.getId().longValue(), mapa.path("lotacaoOrigemId").asLong());
        assertEquals(f.c().jornadaA.getId().longValue(), mapa.path("jornadaOrigemId").asLong());
        assertEquals(f.c().cargoA.getId().longValue(), mapa.path("cargoOrigemId").asLong());
        assertEquals(f.c().lotacaoB.getId().longValue(), mapa.path("lotacaoDestinoId").asLong());
        assertEquals(f.c().jornadaB.getId().longValue(), mapa.path("jornadaDestinoId").asLong());
        assertEquals(f.c().cargoB.getId().longValue(), mapa.path("cargoDestinoId").asLong());
        assertEquals(req.get("autorizacaoAdministrativa"), intencao.path("autorizacaoAdministrativaAto").asText());
        assertEquals(req.get("referenciaSei"), intencao.path("referenciaSei").asText());
        var transferencias = consultar(f.c().nacional, base(f.n().id()) + "/transferencias-gestao", "64 DTO de conferência do ato");
        assertEquals(prova.usoId(), transferencias.get(0).path("conferenciaDocumental").path("usoPermissaoExcepcionalId").asLong());
        assertEquals(prova.hash(), transferencias.get(0).path("conferenciaDocumental").path("hashIntencao").asText());
        var eventos = consultar(f.c().nacional, base(f.n().id()) + "/eventos", "64 histórico conserva null desconhecido de publicação");
        var publicacao = java.util.stream.StreamSupport.stream(eventos.spliterator(), false)
                .filter(e -> e.path("tipoEvento").asText().equals("PUBLICACAO")).findFirst().orElseThrow();
        assertTrue(publicacao.path("conferenciaDocumental").isNull() || publicacao.path("conferenciaDocumental").isMissingNode());
        var antesReenvio = identidades64(f);
        enviar(f.c().localA, base(f.n().id()) + "/transferir-gestao", req, 400,
                "64 reenvio idêntico encontra destino já vigente na data; guarda existente400 sem deduplicação prometida");
        assertEquals(antesReenvio, identidades64(f)); assertEquals(prova, conferir64(f, transferencia64(f)));
    }

    @Test
    void previaSemConsentimentoNaoAutorizaMudancaDeAtoEContemPoliticaVigente() throws Exception {
        var f = preparar64(true); var req = intencao64(f, f.n().inicio().plusDays(2));
        var antes = identidades64(f);
        var previa = enviar(f.c().localA, base(f.n().id()) + "/transferencias-gestao/previa", req, 200,
                "64 prévia não depende de conferir antes de visualizar; não registra uso nem fato");
        assertEquals(antes, identidades64(f));
        String token = previa.path("tokenPrevia").asText();
        req.put("tokenPrevia", token); req.put("confirmarImpacto", true); declarar64(req);
        req.put("autorizacaoAdministrativa", "ATO-SINTETICO64-ALTERADO-APOS-PREVIA");
        enviar(f.c().localA, base(f.n().id()) + "/transferir-gestao", req, 409,
                "64 só altera ATO: token da intenção anterior não autoriza novo alcance declarado");
        assertEquals(antes, identidades64(f));
        req.put("autorizacaoAdministrativa", "ATO-HTTP");
        enviar(f.c().localA, base(f.n().id()) + "/transferir-gestao", req, 200,
                "64 mesma intenção da prévia, conferida depois, pode concluir; boolean separado do digest");
        conferir64(f, transferencia64(f));
    }

    @Test
    void cancelamentoFixaNovoAlvoUsoEConferenciaSemSobrescreverOriginal() throws Exception {
        var f = preparar64(true); var d = f.n().inicio().plusDays(2); var req = intencao64(f, d); declarar64(req);
        enviar(f.c().localA, base(f.n().id()) + "/transferir-gestao", req, 200, "64 transferência sintética origem do cancelamento");
        long transferencia = transferencia64(f); var original = conferir64(f, transferencia);
        var cancelar = mapa("dataEfetiva", d, "dataAto", RelogioSistema.hoje(),
                "motivo", "Cancelamento sintético64 com conferência própria", "referenciaSei", "SEI-SINTETICO-64-CANCELAR",
                "autorizacaoAdministrativa", "ATO-SINTETICO-64-CANCELAR"); declarar64(cancelar);
        String rota = base(f.n().id()) + "/transferencias-gestao/" + transferencia + "/cancelar";
        enviar(f.c().localA, rota, cancelar, 200, "64 referência e conferência novas, concessão genérica vigente permanece");
        assertEquals(original, conferir64(f, transferencia), "Fotografia original, hash e uso não foram substituídos");
        var novo = emTransacao(() -> {
            var e = em.createQuery("select e from EventoCicloVidaContrato e where e.contrato.id=:c and e.tipoEvento=:tipo", EventoCicloVidaContrato.class)
                    .setParameter("c", f.n().id()).setParameter("tipo", TipoEventoCicloVidaContrato.CANCELAMENTO_TRANSFERENCIA_GESTAO).getSingleResult();
            return prova64(e);
        });
        assertNotEquals(original.eventoId(), novo.eventoId()); assertNotEquals(original.usoId(), novo.usoId());
        assertEquals(original.permissaoId(), novo.permissaoId()); assertNotEquals(original.hash(), novo.hash());
        var intencao = mapper.readTree(novo.canonico());
        assertEquals("CANCELAR_TRANSFERENCIA_GESTAO", intencao.path("operacao").asText());
        assertEquals(transferencia, intencao.path("transferenciaAlvoId").asLong());
        assertEquals(original.eventoId(), intencao.path("eventoPrecedenteId").asLong());
        assertEquals(original.hash(), intencao.path("hashIntencaoOriginal").asText());
        assertEquals(1, intencao.path("mapeamentosDaTransferenciaCancelada").size());
        assertEquals(f.c().tipoA.getId().longValue(), intencao.path("tipoDestinoId").asLong());
        var antes = identidades64(f);
        enviar(f.c().localA, rota, cancelar, 409, "64 reenvio de cancelamento já concluído não cria novo fato/uso");
        assertEquals(antes, identidades64(f));
        var eventos = consultar(f.c().nacional, base(f.n().id()) + "/eventos", "64 cancelamento tem conferência consultável no seu evento");
        var dto = java.util.stream.StreamSupport.stream(eventos.spliterator(), false)
                .filter(e -> e.path("id").asLong() == novo.eventoId()).findFirst().orElseThrow();
        assertEquals(novo.usoId(), dto.path("conferenciaDocumental").path("usoPermissaoExcepcionalId").asLong());
    }

    @Test
    void retificacaoRetroativaUsaConcessaoAtualNovaConferenciaENovoUsoPreservandoFonteOriginal() throws Exception {
        var f = preparar64(false); var d = RelogioSistema.hoje().minusDays(10); var req = intencao64(f, d); declarar64(req);
        enviar(f.c().localA, base(f.n().id()) + "/transferir-gestao", req, 200,
                "64 permissão concedida hoje autoriza ato de efeito passado, sem presumir acesso pretérito");
        long transferencia = transferencia64(f); var original = conferir64(f, transferencia);
        enviar(f.c().admin, "/api/permissoes-excepcionais", mapa("tipo", "TRANSFERENCIA_GESTAO_LOCAL",
                "usuarioBeneficiarioId", f.c().localB.getId(), "contratoId", f.n().id(), "finalidade", "Fixture sintética64 retificar",
                "justificativa", "Conferência sintética explícita de capacidade atual", "autorizacaoAdministrativa", "CAPACIDADE-SINTETICA64-B",
                "validoAte", Instant.now().plusSeconds(7200)), 201, "64 executor B recebe capacidade vigente hoje");
        var retificar = intencao64(f, d.plusDays(1));
        retificar.put("eventoPrecedenteId", original.eventoId()); retificar.put("relacaoPrecedencia", "RETIFICA");
        retificar.put("autorizacaoAdministrativa", "ATO-SINTETICO64-RETIFICACAO");
        var previa = enviar(f.c().localB, base(f.n().id()) + "/transferencias-gestao/previa", retificar, 200,
                "64 prévia da retificação retroativa sem conferir pelo usuário");
        retificar.put("tokenPrevia", previa.path("tokenPrevia").asText()); retificar.put("confirmarImpacto", true);
        var antes = identidades64(f);
        enviar(f.c().localB, base(f.n().id()) + "/transferir-gestao", retificar, 400, "64 conferir ato anterior não confirma retificação nova");
        assertEquals(antes, identidades64(f)); declarar64(retificar);
        enviar(f.c().localB, base(f.n().id()) + "/transferir-gestao", retificar, 200,
                "64 retificação com consentimento humano novo e capacidade do executor atual");
        assertEquals(original, conferir64(f, transferencia));
        var nova = emTransacao(() -> em.createQuery("select t.id from TransferenciaGestaoContrato t where t.contrato.id=:c and t.id<>:orig", Long.class)
                .setParameter("c", f.n().id()).setParameter("orig", transferencia).getSingleResult());
        var prova = conferir64(f, nova); assertNotEquals(original.usoId(), prova.usoId());
        assertNotEquals(original.permissaoId(), prova.permissaoId());
        var intencao = mapper.readTree(prova.canonico());
        assertEquals("RETIFICAR_TRANSFERENCIA_GESTAO", intencao.path("operacao").asText());
        assertEquals(transferencia, intencao.path("transferenciaAlvoId").asLong());
        assertEquals(original.eventoId(), intencao.path("eventoPrecedenteId").asLong());
        var antesReenvio = identidades64(f);
        enviar(f.c().localB, base(f.n().id()) + "/transferir-gestao", retificar, 409,
                "64 versão alvo já retificada bloqueia reenvio sem deduplicar HTTP");
        assertEquals(antesReenvio, identidades64(f));
    }

    private Prova conferir64(Fixture f, long transferencia) {
        return emTransacao(() -> {
            var t = em.find(TransferenciaGestaoContrato.class, transferencia);
            var e = t.getEvento(); var c = e.getConferenciaDocumental(); assertNotNull(c);
            var uso = c.getUsoPermissaoExcepcional(); assertNotNull(uso); assertNotNull(uso.getId());
            assertEquals(VERSAO, c.getVersaoDeclaracao()); assertEquals(ConferenciaDocumentalTransferencia.DECLARACAO, c.getDeclaracao());
            assertEquals(e.getUsuarioResponsavelId(), uso.getUsuarioId()); assertEquals(e.getUsuarioResponsavelUsername(), uso.getUsuarioUsername());
            assertEquals(f.n().id(), uso.getRecursoId().longValue()); assertEquals("Contrato", uso.getTipoRecurso());
            assertEquals(com.example.siggtrat.dominio.acesso.TipoPermissaoExcepcional.TRANSFERENCIA_GESTAO_LOCAL, uso.getTipo());
            assertEquals(t.getAutorizacaoAdministrativa(), c.getAutorizacaoAdministrativaAto());
            assertEquals(MaterialHashEventos.sha256(c.getVersaoIntencao(), MaterialHashEventos.texto(c.getIntencaoCanonica())), c.getHashIntencao());
            return prova64(e);
        });
    }
    private Prova prova64(EventoCicloVidaContrato e) {
        var c = e.getConferenciaDocumental(); var uso = c.getUsoPermissaoExcepcional();
        return new Prova(e.getId(), c.getIntencaoCanonica(), c.getHashIntencao(), uso.getId(), uso.getPermissao().getId());
    }
    private record Prova(long eventoId, String canonico, String hash, long usoId, long permissaoId) { }
}
