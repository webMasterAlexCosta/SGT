package com.example.siggtrat.auditoria20261029;

import com.example.siggtrat.entidade.DocumentoComprobatorio;
import com.example.siggtrat.entidade.EventoDocumentoComprobatorio;
import com.example.siggtrat.entidade.EventoFrequencia;
import com.example.siggtrat.entidade.Frequencia;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;

/**
 * Hipótese de integridade do acervo, não exploração HTTP: dano é introduzido
 * deliberadamente por DML na fixture. Oráculo adicional sujeito a aceite:
 * endpoint não deve certificar cadeia íntegra após truncamento de seu sufixo.
 */
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class TransversaisIntegridadeSufixoHipoteseRedIT extends ContratosHttpBase {
    @Test
    void evidenciaDeFrequenciaNaoCertificaPrefixoDepoisDePerderUltimoEvento() throws Exception {
        Cenario c = criarCenarioPersistido();
        long frequencia = registrar(c, "FALTA");
        assertEquals(frequencia, registrar(c, "PRESENCA"));
        String rota = "/api/frequencias/" + frequencia + "/evidencias-integridade";
        assertTrue(consultar(c.localA, rota, "Controle de cadeia antes da corrupção sintética")
                .path("cadeiaIntegra").asBoolean());
        int versao = emTransacao(() -> em.find(Frequencia.class, frequencia).getVersaoNegocial());
        emTransacao(() -> {
            var eventos = em.createQuery("select e from EventoFrequencia e where e.frequencia.id=:f "
                            + "order by e.numeroVersao,e.id", EventoFrequencia.class)
                    .setParameter("f", frequencia).getResultList();
            assertEquals(2, eventos.size());
            assertEquals(1, em.createQuery("delete from EventoFrequencia where id=:id")
                    .setParameter("id", eventos.get(1).getId()).executeUpdate());
            return null;
        });
        assertEquals(versao, emTransacao(() -> em.find(Frequencia.class, frequencia).getVersaoNegocial()));
        assertFalse(consultar(c.localA, rota, "Cadeia incompleta não deve ser certificada pelo prefixo remanescente")
                .path("cadeiaIntegra").asBoolean(), "Última versão da raiz perdeu sua prova sem invalidar a resposta INTEGRA.");
    }

    @Test
    void evidenciaDocumentalNaoCertificaPrefixoDepoisDePerderRetificacaoFinal() throws Exception {
        Cenario c = criarCenarioPersistido();
        String raiz = base(c.contratoA.getId()) + "/documentos";
        long original = dados(executar(c.localA, multipart(raiz).file(metadados(mapa(
                        "tipoFato", "VINCULO", "fatoId", c.vinculoA.getId(), "titulo", "Fonte da hipótese de integridade",
                        "referenciaSei", "SEI-INICIAL", "classificacaoAcesso", "NEGOCIAL", "dataEfetiva", DATA,
                        "chaveIdempotencia", "integridade-doc-" + UUID.randomUUID()))), 201,
                "Controle documental publicado e vinculado à própria raiz canônica")).path("id").asLong();
        long nova = enviar(c.localA, raiz + "/" + original + "/retificar-sei", mapa(
                        "novaReferenciaSei", "SEI-RETIFICADO", "motivo", "Correção documental comprovada",
                        "chaveIdempotencia", "integridade-ret-" + UUID.randomUUID()), 200,
                "Nova versão conserva o original").path("id").asLong();
        String rota = raiz + "/" + nova + "/evidencias-integridade";
        assertTrue(consultar(c.localA, rota, "Controle de cadeia documental íntegra")
                .path("cadeiaIntegra").asBoolean());
        emTransacao(() -> {
            var eventos = em.createQuery("select e from EventoDocumentoComprobatorio e where e.documento.id=:d",
                            EventoDocumentoComprobatorio.class).setParameter("d", nova).getResultList();
            assertEquals(1, eventos.size());
            assertEquals(1, em.createQuery("delete from EventoDocumentoComprobatorio where id=:id")
                    .setParameter("id", eventos.get(0).getId()).executeUpdate());
            return null;
        });
        assertEquals(2, consultar(c.localA, raiz + "/" + nova, "A raiz da nova versão ainda existe")
                .path("versaoDocumento").asInt());
        assertFalse(consultar(c.localA, rota, "Prefixo intacto não demonstra completude de cadeia documental")
                .path("cadeiaIntegra").asBoolean());
    }

    @Test
    void evidenciaDocumentalDetectaPerdaDeConcessaoSemNovaVersao() throws Exception {
        Cenario c = criarCenarioPersistido();
        long documento = criarFonteRestrita(c);
        String rota = base(c.contratoA.getId()) + "/documentos/" + documento;
        enviar(c.admin, rota + "/permissoes", mapa("usuarioId", c.nacional.getId(),
                "finalidade", "auditoria", "justificativa", "Concessão sintética para conferir integridade",
                "autorizacaoAdministrativa", "ATO-INTEGRIDADE"), 200, "Concessão acrescenta evento na mesma versão");
        assertTrue(consultar(c.localA, rota + "/evidencias-integridade?finalidade=auditoria", "Controle íntegro")
                .path("cadeiaIntegra").asBoolean());
        emTransacao(() -> {
            DocumentoComprobatorio raiz = em.find(DocumentoComprobatorio.class, documento);
            assertEquals(1, raiz.getVersaoDocumento());
            assertEquals(2L, raiz.getQuantidadeEventosAuditoria());
            var eventos = em.createQuery("select e from EventoDocumentoComprobatorio e where e.documento.id=:d "
                            + "order by e.registradoEm,e.id", EventoDocumentoComprobatorio.class)
                    .setParameter("d", documento).getResultList();
            assertEquals(2, eventos.size());
            em.remove(eventos.get(1));
            return null;
        });
        assertFalse(consultar(c.localA, rota + "/evidencias-integridade?finalidade=auditoria",
                "Versão não mudou, mas a raiz conserva quantidade e terminal do evento perdido")
                .path("cadeiaIntegra").asBoolean());
    }

    @Test
    void versaoLegadaSemAncoraNaoAdotaPrefixoAoReceberOutroEvento() throws Exception {
        Cenario c = criarCenarioPersistido();
        long documento = criarFonteRestrita(c);
        String rota = base(c.contratoA.getId()) + "/documentos/" + documento;
        emTransacao(() -> {
            DocumentoComprobatorio raiz = em.find(DocumentoComprobatorio.class, documento);
            raiz.setQuantidadeEventosAuditoria(null);
            raiz.setUltimoHashEventoAuditoria(null);
            return null;
        });
        assertFalse(consultar(c.localA, rota + "/evidencias-integridade?finalidade=auditoria",
                "Legado sem âncora não permite certificar completude").path("cadeiaIntegra").asBoolean());
        enviar(c.admin, rota + "/permissoes", mapa("usuarioId", c.nacional.getId(),
                "finalidade", "auditoria", "justificativa", "Concessão não recupera prova antiga ausente",
                "autorizacaoAdministrativa", "ATO-LEGADO"), 200, "Evento novo conserva ausência da âncora antiga");
        assertFalse(consultar(c.localA, rota + "/evidencias-integridade?finalidade=auditoria",
                "Novo evento não adota silenciosamente os eventos que restaram").path("cadeiaIntegra").asBoolean());
        emTransacao(() -> {
            assertNull(em.find(DocumentoComprobatorio.class, documento).getQuantidadeEventosAuditoria());
            return null;
        });
    }

    private long criarFonteRestrita(Cenario c) throws Exception {
        return dados(executar(c.localA, multipart(base(c.contratoA.getId()) + "/documentos").file(metadados(mapa(
                "tipoFato", "VINCULO", "fatoId", c.vinculoA.getId(), "titulo", "Fonte restrita para integridade",
                "referenciaSei", "SEI-INTEGRIDADE-" + UUID.randomUUID(), "classificacaoAcesso", "PESSOAL_RESTRITO",
                "dataEfetiva", DATA))), 201, "Fonte da própria raiz canônica")).path("id").asLong();
    }

    private long registrar(Cenario c, String status) throws Exception {
        return enviar(c.localA, "/api/frequencias", mapa("vinculoContratualId", c.vinculoA.getId(),
                        "dataReferencia", DATA, "status", status,
                        "justificativaRetificacao", "Conferência documental da integridade sintética",
                        "chaveIdempotencia", "integridade-freq-" + UUID.randomUUID()), 200,
                "Gravação formal com cadeia auditável").path("id").asLong();
    }
}
