package com.example.siggtrat.auditoria20261029;

import com.example.siggtrat.configuracao.propriedades.PropriedadesAuditoria;
import com.example.siggtrat.entidade.Empregado;
import com.example.siggtrat.entidade.HistoricoEmpregado;
import com.example.siggtrat.entidade.UsuarioAplicacao;
import com.example.siggtrat.servico.HistoricoIntegridadeServico;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.HexFormat;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;

/** Compatibilidade real PostgreSQL: assinatura original, v2 com escopo e v3 com fonte coexistem. */
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class HistoricoAssinaturaLegadaMistaHttpIT extends ContratosHttpBase {
    @Autowired HistoricoIntegridadeServico integridade;
    @Autowired PropriedadesAuditoria configuracao;

    @Test
    void cadeiaMistaPreservaHashOriginalEAutenticaIdentidadeDaOcorrenciaNova() throws Exception {
        Cenario c = criarCenarioPersistido();
        long idLegado = emTransacao(() -> {
            Empregado e = em.find(Empregado.class, c.empregadoA.getId());
            assertEquals(0, e.getHistoricoQuantidadeAssinada());
            HistoricoEmpregado h = novo(e, em.find(UsuarioAplicacao.class, c.localA.getId()), "Conteúdo legado sintético");
            h.setHashIntegridade("PENDENTE-FIXTURE"); em.persist(h); em.flush();
            // Formato original produzido independentemente do serviço sob teste.
            String canonico = e.getId() + "|" + h.getId() + "|" + h.getDataEvento() + "|" + c.localA.getId()
                    + "|" + Base64.getEncoder().encodeToString(h.getDescricao().getBytes(StandardCharsets.UTF_8)) + "|GENESIS";
            String hash = assinar(canonico); h.setHashIntegridade(hash);
            e.setHistoricoQuantidadeAssinada(1); e.setHistoricoTotalAncorado(1);
            e.setHistoricoUltimoIdAssinado(h.getId()); e.setHistoricoUltimoHash(hash);
            e.setHistoricoAncoraAssinatura(assinar(e.getId() + "|1|1|" + h.getId() + "|" + hash));
            em.flush(); return h.getId();
        });
        String hashOriginal = emTransacao(() -> em.find(HistoricoEmpregado.class, idLegado).getHashIntegridade());
        assertTrue(integridade.verificarEmpregado(c.empregadoA.getId()).integro());
        long idV2 = emTransacao(() -> {
            HistoricoEmpregado h = novo(em.find(Empregado.class, c.empregadoA.getId()),
                    em.find(UsuarioAplicacao.class, c.localA.getId()), "Registro genérico novo com escopo autenticado");
            h.setGestaoLocalSnapshotId(c.gestaoA.getId()); h.setSuperintendenciaSnapshotId(c.seA.getId());
            h.setDataEfetiva(DATA);
            HistoricoEmpregado salvo = integridade.salvarAssinado(h);
            assertEquals(2, salvo.getVersaoAssinatura()); return salvo.getId();
        });
        adulteracaoV2Isolada(c, idV2, "Gestão", h -> h.setGestaoLocalSnapshotId(c.gestaoB.getId()),
                h -> h.setGestaoLocalSnapshotId(c.gestaoA.getId()));
        adulteracaoV2Isolada(c, idV2, "Estado", h -> h.setSuperintendenciaSnapshotId(c.seB.getId()),
                h -> h.setSuperintendenciaSnapshotId(c.seA.getId()));
        adulteracaoV2Isolada(c, idV2, "Data efetiva", h -> h.setDataEfetiva(DATA.plusDays(1)),
                h -> h.setDataEfetiva(DATA));
        adulteracaoV2Isolada(c, idV2, "Descrição", h -> h.setDescricao("Descrição sintética adulterada"),
                h -> h.setDescricao("Registro genérico novo com escopo autenticado"));
        adulteracaoV2Isolada(c, idV2, "Instante", h -> h.setDataEvento(LocalDateTime.of(2026, 6, 10, 9, 30, 1)),
                h -> h.setDataEvento(LocalDateTime.of(2026, 6, 10, 9, 30)));
        adulteracaoV2Isolada(c, idV2, "Autor", h -> h.setUsuario(em.find(UsuarioAplicacao.class, c.localB.getId())),
                h -> h.setUsuario(em.find(UsuarioAplicacao.class, c.localA.getId())));
        adulteracaoV2Isolada(c, idV2, "Marcador de versão", h -> h.setVersaoAssinatura(1), h -> h.setVersaoAssinatura(2));
        long ocorrencia = dados(executar(c.localA, multipart("/api/ocorrencias")
                .param("empregadoId", c.empregadoA.getId().toString()).param("motivo", "ATESTADO")
                .param("dataOcorrencia", DATA.toString()).param("dias", "1")
                .param("chaveIdempotencia", "cadeia-mista-" + UUID.randomUUID()), 201,
                "Novo fato acrescenta assinatura de fonte sem alterar os registros anteriores")).path("id").asLong();
        long idV3 = emTransacao(() -> {
            HistoricoEmpregado h = em.createQuery("select h from HistoricoEmpregado h where h.empregado.id=:id order by h.id desc",
                    HistoricoEmpregado.class).setParameter("id", c.empregadoA.getId()).setMaxResults(1).getSingleResult();
            assertEquals(3, h.getVersaoAssinatura()); assertEquals(ocorrencia, h.getOcorrencia().getId());
            assertEquals(2, em.find(HistoricoEmpregado.class, idV2).getVersaoAssinatura());
            assertNull(em.find(HistoricoEmpregado.class, idLegado).getVersaoAssinatura());
            assertEquals(hashOriginal, em.find(HistoricoEmpregado.class, idLegado).getHashIntegridade());
            return h.getId();
        });
        JsonNode verificacao = consultar(c.localA, "/api/historicos-empregados/empregado/" + c.empregadoA.getId() + "/integridade",
                "Verificação HTTP relê a cadeia mista confirmada no PostgreSQL");
        assertTrue(verificacao.path("integro").asBoolean()); assertEquals(3, verificacao.path("registrosVerificados").asInt());
        assertTrue(verificacao.path("mensagem").asText().contains("legado"),
                "Não se atribui proteção de escopo retroativa à assinatura original");
        emTransacao(() -> {
            assertEquals(1, em.createQuery("update HistoricoEmpregado set ocorrencia=null where id=:id")
                    .setParameter("id", idV3).executeUpdate()); return null;
        });
        assertFalse(integridade.verificarEmpregado(c.empregadoA.getId()).integro(),
                "Remover o vínculo que participa da autorização deve invalidar o HMAC v3");
        assertEquals(hashOriginal, emTransacao(() -> em.find(HistoricoEmpregado.class, idLegado).getHashIntegridade()));
    }

    private HistoricoEmpregado novo(Empregado e, UsuarioAplicacao u, String descricao) {
        HistoricoEmpregado h = new HistoricoEmpregado(); h.setEmpregado(e); h.setUsuario(u);
        h.setDataEvento(LocalDateTime.of(2026, 6, 10, 9, 30)); h.setDescricao(descricao); return h;
    }

    private void adulteracaoV2Isolada(Cenario c, long historico, String campo,
            java.util.function.Consumer<HistoricoEmpregado> alterar,
            java.util.function.Consumer<HistoricoEmpregado> restaurar) {
        assertTrue(integridade.verificarEmpregado(c.empregadoA.getId()).integro(), "Controle íntegro antes de " + campo);
        String hash = emTransacao(() -> em.find(HistoricoEmpregado.class, historico).getHashIntegridade());
        emTransacao(() -> { alterar.accept(em.find(HistoricoEmpregado.class, historico)); em.flush(); return null; });
        var adulterada = integridade.verificarEmpregado(c.empregadoA.getId());
        assertFalse(adulterada.integro(), "Cada campo v2 rompe isoladamente a prova: " + campo);
        assertEquals(historico, adulterada.primeiroRegistroInvalido());
        emTransacao(() -> {
            HistoricoEmpregado h = em.find(HistoricoEmpregado.class, historico); restaurar.accept(h);
            assertEquals(hash, h.getHashIntegridade(), "Restaurar o fato não recalcula nem troca sua assinatura");
            em.flush(); return null;
        });
        assertTrue(integridade.verificarEmpregado(c.empregadoA.getId()).integro(), "Controle restaurado entre tentativas: " + campo);
    }

    private String assinar(String conteudo) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(configuracao.secret().getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            return "HMAC-SHA256:" + HexFormat.of().formatHex(mac.doFinal(conteudo.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) { throw new IllegalStateException(e); }
    }
}
