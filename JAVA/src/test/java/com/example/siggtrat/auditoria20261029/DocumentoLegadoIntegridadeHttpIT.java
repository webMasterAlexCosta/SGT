package com.example.siggtrat.auditoria20261029;

import com.example.siggtrat.dominio.documento.TipoEventoDocumento;
import com.example.siggtrat.entidade.DocumentoComprobatorio;
import com.example.siggtrat.entidade.EventoDocumentoComprobatorio;
import com.example.siggtrat.utilitario.UtilitarioHashSeguranca;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;

class DocumentoLegadoIntegridadeHttpIT extends ContratosHttpBase {
    @Test
    void documentoSemTrilhaNaoEhDeclaradoIntegroNemRecebeEventoInventado() throws Exception {
        var c = criarCenario();
        var doc = new DocumentoComprobatorio(); doc.setContrato(c.contratoA);
        doc.setTipoFato(com.example.siggtrat.dominio.documento.TipoFatoDocumental.CONTRATO_ORIGINAL);
        doc.setClassificacaoAcesso(com.example.siggtrat.dominio.documento.ClassificacaoAcessoDocumento.NEGOCIAL);
        doc.setTitulo("Fonte incorporada sem trilha de eventos"); doc.setReferenciaSei("SEI-SEM-TRILHA");
        doc.setDataEfetiva(DATA); em.persist(doc); em.flush(); long id = doc.getId(); long contratoId = c.contratoA.getId(); em.clear();
        var evidencia = consultar(c.localA, base(contratoId) + "/documentos/" + id + "/evidencias-integridade",
                "DOC-005 ausência de eventos não constitui prova de integridade");
        assertThat(evidencia.path("estado").asText()).isEqualTo("INCONSISTENTE_OU_NAO_VERIFICAVEL");
        assertThat(evidencia.path("cadeiaIntegra").asBoolean()).isFalse();
        assertThat(evidencia.path("eventos")).isEmpty();
        executar(c.localA, get(base(contratoId) + "/documentos/" + id + "/eventos"), 409,
                "DOC-005 caminho estrito também rejeita ausência de trilha");
        assertThat(em.createQuery("select count(e) from EventoDocumentoComprobatorio e where e.documento.id=:id", Long.class)
                .setParameter("id", id).getSingleResult()).isZero();
    }

    @Test
    void consultaDeEvidenciasRestritasExigeFinalidadeConcessaoEAuditaHistorico() throws Exception {
        var c = criarCenario(); var n = publicado(c, 100, null, true);
        long publicacao = consultar(c.localA, base(n.id()) + "/eventos", "evento original da fonte restrita")
                .get(0).path("id").asLong();
        var doc = dados(executar(c.localA, multipart(base(n.id()) + "/documentos").file(metadados(mapa(
                "tipoFato", "CONTRATO_ORIGINAL", "fatoId", publicacao, "fatoChave", "ORIGINAL",
                "titulo", "Fonte restrita íntegra", "referenciaSei", "SEI-EVIDENCIA-RESTRITA",
                "classificacaoAcesso", "PESSOAL_RESTRITO", "dataEfetiva", n.inicio()))), 201,
                "DOC-010 fonte restrita publicada"));
        String path = base(n.id()) + "/documentos/" + doc.path("id").asLong();
        executar(c.localA, get(path + "/evidencias-integridade"), 400, "DOC-010 diagnóstico também exige finalidade");
        executar(c.nacional, get(path + "/evidencias-integridade").param("finalidade", "conferencia"), 403,
                "DOC-010 diagnóstico restrito sem concessão não expõe eventos");
        var permissao = enviar(c.admin, path + "/permissoes", mapa("usuarioId", c.nacional.getId(),
                "finalidade", "conferencia", "justificativa", "Conferência autorizada da integridade documental",
                "autorizacaoAdministrativa", "ATO-DIAGNOSTICO", "validoAte", Instant.now().plusSeconds(3600)), 200,
                "DOC-010 concessão específica para conferência");
        em.flush(); em.clear();
        var evidencia = consultar(c.nacional, path + "/evidencias-integridade?finalidade=conferencia",
                "DOC-005/010 cadeia nova íntegra consultada com concessão");
        assertThat(evidencia.path("estado").asText()).isEqualTo("INTEGRA");
        assertThat(evidencia.path("cadeiaIntegra").asBoolean()).isTrue();
        for (var evento : evidencia.path("eventos")) {
            assertThat(evento.path("hashConfere").asBoolean()).isTrue();
            assertThat(evento.path("encadeamentoConfere").asBoolean()).isTrue();
            assertThat(evento.path("hashCalculado").asText()).isEqualTo(evento.path("hashArmazenado").asText());
        }
        var acessos = consultar(c.localA, path + "/acessos", "DOC-010 acesso ao diagnóstico gera trilha própria");
        assertThat(acessos.size()).isEqualTo(1);
        assertThat(acessos.get(0).path("usuarioId").asLong()).isEqualTo(c.nacional.getId());
        assertThat(acessos.get(0).path("permissaoId").asLong()).isEqualTo(permissao.path("id").asLong());
        assertThat(acessos.get(0).path("tipoAcesso").asText()).isEqualTo("CONSULTA_HISTORICO");
        assertThat(acessos.get(0).path("finalidade").asText()).isEqualTo("conferencia");
        executar(c.localB, get(path + "/evidencias-integridade").param("finalidade", "conferencia"), 404,
                "DOC-010 evidência restrita respeita gestão");
    }

    @Test
    void hashAnteriorComPrecisaoPerdidaPermaneceIntocadoENaoEhDeclaradoIntegro() throws Exception {
        var c = criarCenario();
        var d = dados(executar(c.localA, multipart(base(c.contratoA.getId()) + "/documentos").file(metadados(mapa(
                "tipoFato", "CONTRATO_ORIGINAL", "fatoChave", "ORIGINAL", "titulo", "Documento de compatibilidade",
                "referenciaSei", "SEI-LEGADO", "classificacaoAcesso", "NEGOCIAL", "dataEfetiva", DATA))), 201,
                "DOC-005 documento de referência"));
        long documentoId = d.path("id").asLong();
        var doc = em.find(DocumentoComprobatorio.class, documentoId);
        var primeiro = em.createQuery("select e from EventoDocumentoComprobatorio e where e.documento.id=:id", EventoDocumentoComprobatorio.class)
                .setParameter("id", documentoId).getSingleResult();
        Instant comNanos = Instant.now().plusSeconds(1).truncatedTo(ChronoUnit.SECONDS).plusNanos(123456789);
        var antigo = new EventoDocumentoComprobatorio(); antigo.setDocumento(doc); antigo.setGrupoLogicoId(doc.getGrupoLogicoId());
        antigo.setTipoEvento(TipoEventoDocumento.RETIFICACAO_SEI); antigo.setDataEfetiva(DATA); antigo.setRegistradoEm(comNanos);
        antigo.setResponsavelId(c.localA.getId()); antigo.setResponsavelUsername(c.localA.getUsername());
        antigo.setMotivo("Evento produzido pela assinatura antiga com nanos"); antigo.setReferenciaSeiAnterior("SEI-LEGADO");
        antigo.setReferenciaSeiNova("SEI-LEGADO-CORRIGIDO"); antigo.setHashAnterior(primeiro.getHashEvento());
        String material = String.join("|", doc.getGrupoLogicoId(), String.valueOf(doc.getId()), "RETIFICACAO_SEI", DATA.toString(),
                comNanos.toString(), c.localA.getId().toString(), c.localA.getUsername(), antigo.getMotivo(), "", "", "SEI-LEGADO",
                "SEI-LEGADO-CORRIGIDO", "", "", primeiro.getHashEvento());
        String hashOriginal = UtilitarioHashSeguranca.sha256Hexadecimal(material); antigo.setHashEvento(hashOriginal);
        em.persist(antigo); em.flush(); long eventoId = antigo.getId(); long contratoId = c.contratoA.getId(); em.clear();
        var recarregado = em.find(EventoDocumentoComprobatorio.class, eventoId);
        assertThat(recarregado.getRegistradoEm()).isNotEqualTo(comNanos);
        executar(c.localA, get(base(contratoId) + "/documentos/" + documentoId + "/eventos"), 409,
                "DOC-005 cadeia anterior não verificável permanece explicitamente bloqueada");
        assertThat(em.find(EventoDocumentoComprobatorio.class, eventoId).getHashEvento()).isEqualTo(hashOriginal);
        assertThat(consultar(c.localA, base(contratoId) + "/documentos/" + documentoId,
                "DOC-005 metadados originais continuam consultáveis").path("referenciaSei").asText()).isEqualTo("SEI-LEGADO");
        var evidencias = consultar(c.localA, base(contratoId) + "/documentos/" + documentoId + "/evidencias-integridade",
                "DOC-005 evidência preservada sem falsa declaração de integridade");
        assertThat(evidencias.path("estado").asText()).isEqualTo("INCONSISTENTE_OU_NAO_VERIFICAVEL");
        assertThat(evidencias.path("cadeiaIntegra").asBoolean()).isFalse();
        assertThat(evidencias.path("eventos").size()).isEqualTo(2);
        var ultimo = evidencias.path("eventos").get(1);
        assertThat(ultimo.path("hashArmazenado").asText()).isEqualTo(hashOriginal);
        assertThat(ultimo.path("hashCalculado").asText()).isNotEqualTo(hashOriginal);
        assertThat(ultimo.path("eventoOriginal").path("motivo").asText()).isEqualTo(antigo.getMotivo());
        executar(c.localB, get(base(contratoId) + "/documentos/" + documentoId + "/evidencias-integridade"), 404,
                "DOC-010 consulta de evidência mantém escopo");
        executar(c.operacionalA, get(base(contratoId) + "/documentos/" + documentoId + "/evidencias-integridade"), 403,
                "DOC-010 consulta de evidência mantém política do documento");
        em.flush(); em.clear();
        assertThat(em.find(EventoDocumentoComprobatorio.class, eventoId).getHashEvento()).isEqualTo(hashOriginal);
    }
}
