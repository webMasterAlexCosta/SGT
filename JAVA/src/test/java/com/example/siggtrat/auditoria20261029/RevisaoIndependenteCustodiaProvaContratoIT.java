package com.example.siggtrat.auditoria20261029;

import com.example.siggtrat.dominio.documento.ClassificacaoAcessoDocumento;
import com.example.siggtrat.dominio.documento.EstadoDocumentoComprobatorio;
import com.example.siggtrat.entidade.*;
import com.example.siggtrat.servico.MotorTemporalContratoServico;
import com.example.siggtrat.utilitario.RelogioSistema;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

/** Hipótese H-RPCH-07 preparada separadamente; execução reservada à raiz. */
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class RevisaoIndependenteCustodiaProvaContratoIT extends ContratosHttpBase {
    @Autowired MotorTemporalContratoServico motor;

    @Test
    void designacaoDeConferenciaPermiteFonteNegocialPreservadaSemAbrirConsultaGeral() throws Exception {
        var c = criarCenarioPersistido();
        var n = emTransacao(() -> {
            try { return publicado(c, 10, null, false); }
            catch (Exception ex) { throw new IllegalStateException(ex); }
        });
        var req = emTransacao(() -> {
            var contrato = em.find(Contrato.class, n.id());
            var versao = motor.fotografiaOriginalConhecida(contrato, Instant.now()).orElseThrow();
            @SuppressWarnings("unchecked") Map<String, Object> foto = mapper.convertValue(motor.paraVersaoDTO(versao), LinkedHashMap.class);
            for (String campo : List.of("id", "numeroVersao", "eventoOrigemId", "registradoEm", "tipoContratacaoComprovado"))
                foto.remove(campo);
            var comp = em.find(ComposicaoContratual.class, n.composicao1());
            var documento = em.createQuery("select d from DocumentoComprobatorio d where d.contrato.id=:c", DocumentoComprobatorio.class)
                    .setParameter("c", n.id()).getSingleResult();
            assertThat(documento.getEstado()).isEqualTo(EstadoDocumentoComprobatorio.PUBLICADO);
            assertThat(documento.getClassificacaoAcesso()).isEqualTo(ClassificacaoAcessoDocumento.NEGOCIAL);
            var item = mapa("composicaoId", comp.getId(), "lotacaoNome", comp.getLotacaoNomeFotografia(),
                    "jornadaDescricao", comp.getJornadaDescricaoFotografia(), "cargoNome", comp.getCargoNomeFotografia(),
                    "lotacaoMunicipio", comp.getLotacaoMunicipioFotografia(), "municipioComprovado", true,
                    "jornadaSemanal", comp.getJornadaSemanalFotografia(), "jornadaComprovada", true);
            var fonte = mapa("eventoOrigemId", versao.getEventoOrigem().getId(), "versaoAnteriorId", null,
                    "dataDocumento", n.inicio(), "documentoComprobatorioId", documento.getId(),
                    "finalidadeAcessoDocumento", "Conferência restrita à incorporação documental expressamente designada",
                    "motivo", "Fonte original negocial preservada, sem qualquer fotografia contratual remanescente",
                    "autorizacaoAdministrativa", "DESIGNACAO-EXPRESSA-SINTETICA", "versaoContratoEsperada", contrato.getVersion(),
                    "chaveIdempotencia", UUID.randomUUID().toString(), "configuracaoCompletaComprovada", true,
                    "fotografia", foto, "composicoes", List.of(item));
            em.createQuery("delete from VersaoContratoNegocial v where v.contrato.id=:c")
                    .setParameter("c", n.id()).executeUpdate();
            return fonte;
        });
        enviar(c.admin, "/api/permissoes-excepcionais", mapa(
                "usuarioBeneficiarioId", c.localA.getId(), "contratoId", n.id(), "gestaoLocalId", c.gestaoA.getId(),
                "tipo", "INCORPORACAO_PROVA_HISTORICA", "finalidade", "Conferir fonte original já preservada",
                "justificativa", "Ausência de fotografias em fixture descartável",
                "autorizacaoAdministrativa", "DESIGNACAO-EXPRESSA-SINTETICA", "validoAte", Instant.now().plusSeconds(7200)),
                201, "designação limitada à conferência documental deste contrato");

        long documentoId = ((Number) req.get("documentoComprobatorioId")).longValue();
        executar(c.localA, get(base(n.id()) + "/documentos/" + documentoId), 404,
                "designação de incorporação não equivale a autorização de leitura geral do contrato");
        String rota = base(n.id()) + "/provas-historicas";
        var previa = enviar(c.localA, rota + "/previa", req, 200,
                "HIS-006: fonte NEGOCIAL preservada pode fundamentar a conferência específica autorizada");
        req.put("tokenPrevia", previa.path("tokenPrevia").asText());
        req.put("confirmarImpacto", true);
        enviar(c.localA, rota, req, 201, "incorporar a fonte documentada sem inferir o contexto pelo mestre");
        emTransacao(() -> {
            var prova = em.createQuery("select p from ProvaHistoricaContrato p where p.contrato.id=:c", ProvaHistoricaContrato.class)
                    .setParameter("c", n.id()).getSingleResult();
            assertThat(prova.getDocumentoComprobatorio().getId()).isEqualTo(documentoId);
            assertThat(prova.getReferenciaSei()).isNull();
            return null;
        });
    }
}
