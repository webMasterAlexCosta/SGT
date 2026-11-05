package com.example.siggtrat.auditoria20261029;

import com.example.siggtrat.entidade.DocumentoComprobatorio;
import com.example.siggtrat.entidade.EventoDocumentoComprobatorio;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;

/** Dano sintético do acervo por JPA: não representa exploração de endpoint de exclusão. */
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class Candidato2DocumentoPerdaVersaoRedIT extends ContratosHttpBase {
    @Test
    void ultimaVersaoPerdidaComSeusEventosNaoPodeCertificarGrupoCompleto() throws Exception {
        Cenario c = criarCenarioPersistido();
        String raiz = base(c.contratoA.getId()) + "/documentos";
        long original = dados(executar(c.localA, multipart(raiz).file(metadados(mapa(
                "tipoFato", "VINCULO", "fatoId", c.vinculoA.getId(), "titulo", "Cadeia da revisão independente",
                "referenciaSei", "SEI-ORIGINAL-" + UUID.randomUUID(), "classificacaoAcesso", "NEGOCIAL",
                "dataEfetiva", DATA))), 201, "Fonte original íntegra")).path("id").asLong();
        long nova = enviar(c.localA, raiz + "/" + original + "/retificar-sei", mapa(
                "novaReferenciaSei", "SEI-NOVO-" + UUID.randomUUID(), "motivo", "Retificação documentada",
                "chaveIdempotencia", UUID.randomUUID().toString()), 200,
                "Nova versão conserva a anterior").path("id").asLong();
        String evidencias = raiz + "/" + original + "/evidencias-integridade";
        assertTrue(consultar(c.localA, evidencias, "Controle antes do dano sintético").path("cadeiaIntegra").asBoolean());
        emTransacao(() -> {
            var eventos = em.createQuery("select e from EventoDocumentoComprobatorio e where e.documento.id=:id",
                    EventoDocumentoComprobatorio.class).setParameter("id", nova).getResultList();
            assertEquals(1, eventos.size());
            eventos.forEach(em::remove);
            em.flush();
            em.remove(em.find(DocumentoComprobatorio.class, nova));
            em.flush();
            return null;
        });
        emTransacao(() -> {
            assertNull(em.find(DocumentoComprobatorio.class, nova));
            assertEquals("SUBSTITUIDO", em.find(DocumentoComprobatorio.class, original).getEstado().name());
            return null;
        });
        assertFalse(consultar(c.localA, evidencias,
                "Grupo com versão substituída sem sucessora não demonstra completude").path("cadeiaIntegra").asBoolean(),
                "Âncoras das raízes sobreviventes não provam que a última raiz e seus eventos sobreviveram.");
    }
}
