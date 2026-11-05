package com.example.siggtrat.auditoria20261029;

import com.example.siggtrat.entidade.*;
import com.example.siggtrat.servico.MotorTemporalContratoServico;
import com.example.siggtrat.utilitario.RelogioSistema;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

/**
 * Dois casos adicionais para execução exclusiva da raiz, ainda não executados
 * pelo revisor. A ausência é simulada apenas nas fixtures descartáveis.
 * Fonte recebida: manifesto SHA-256
 * 7d443bb0049979083ed022b0d5585c2318a35f794ac20c1f2bad9c4ba0e87d7f.
 */
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class RevisaoIndependenteProvaContratoValidacaoIT extends ContratosHttpBase {
    @Autowired MotorTemporalContratoServico motor;

    @Test
    void fonteIntegralNaoAceitaCnpjComQuatorzeLetrasComoEmpresaComprovada() throws Exception {
        var c = criarCenarioPersistido();
        var n = novo(c);
        var req = fonteOriginal(n);
        long versaoId = ((Number) req.get("versaoAnteriorId")).longValue();
        emTransacao(() -> {
            em.createQuery("update VersaoContratoNegocial v set v.cnpjEmpresa=null where v.id=:v")
                    .setParameter("v", versaoId).executeUpdate();
            return null;
        });
        @SuppressWarnings("unchecked") var foto = (Map<String, Object>) req.get("fotografia");
        foto.put("cnpjEmpresa", "abcdefghijklmn");
        conceder(c, n.id(), "INCORPORACAO_PROVA_HISTORICA");

        enviar(c.localA, rota(n) + "/previa", req, 400,
                "EMP-001/HIS-006: quatorze caracteres não numéricos não comprovam CNPJ válido");
        emTransacao(() -> {
            assertThat(contarProvas(n.id())).isZero();
            assertThat(em.find(VersaoContratoNegocial.class, versaoId).getCnpjEmpresa()).isNull();
            assertThat(em.find(Contrato.class, n.id()).getCnpjEmpresa()).isEqualTo("12345678000190");
            return null;
        });
    }

    @Test
    void totalDocumentalDaPublicacaoDeveConcordarComQuantidadesOriginaisPreservadas() throws Exception {
        var c = criarCenarioPersistido();
        var n = novo(c);
        var req = fonteOriginal(n);
        long versaoId = ((Number) req.get("versaoAnteriorId")).longValue();
        long versaoContrato = emTransacao(() -> em.find(Contrato.class, n.id()).getVersion());
        emTransacao(() -> {
            // Apenas a coluna agregada da fotografia se perdeu. A quantidade da
            // composição original permanece uma fonte canônica independente.
            assertThat(em.find(ComposicaoContratual.class, n.composicao1()).getEfetivoPrevisto()).isEqualTo(10);
            em.createQuery("update VersaoContratoNegocial v set v.efetivoPrevisto=null where v.id=:v")
                    .setParameter("v", versaoId).executeUpdate();
            return null;
        });
        @SuppressWarnings("unchecked") var foto = (Map<String, Object>) req.get("fotografia");
        foto.put("efetivoPrevisto", 99);
        conceder(c, n.id(), "INCORPORACAO_PROVA_HISTORICA");

        var resposta = mvc.perform(autenticar(post(rota(n) + "/previa")
                .contentType(MediaType.APPLICATION_JSON).content(json(req)), c.localA)).andReturn();
        emTransacao(() -> {
            assertThat(contarProvas(n.id())).isZero();
            var contrato = em.find(Contrato.class, n.id());
            assertThat(contrato.getVersion()).isEqualTo(versaoContrato);
            assertThat(contrato.getEfetivoPrevisto()).isEqualTo(10);
            assertThat(contrato.getEfetivoOriginalPublicado()).isEqualTo(10);
            assertThat(em.find(ComposicaoContratual.class, n.composicao1()).getEfetivoPrevisto()).isEqualTo(10);
            assertThat(em.find(VersaoContratoNegocial.class, versaoId).getEfetivoPrevisto()).isNull();
            assertThat(em.createQuery("select count(v) from VersaoContratoNegocial v where v.contrato.id=:c", Long.class)
                    .setParameter("c", n.id()).getSingleResult()).isEqualTo(1L);
            return null;
        });
        assertThat(resposta.getResponse().getStatus())
                .as("HIS-006/CMP: total 99 contradiz a soma original preservada 10; não pode receber prévia de incorporação")
                .isIn(400, 409);
    }

    private long contarProvas(long contratoId) {
        return em.createQuery("select count(p) from ProvaHistoricaContrato p where p.contrato.id=:c", Long.class)
                .setParameter("c", contratoId).getSingleResult();
    }

    private NovoContrato novo(Cenario c) {
        return emTransacao(() -> {
            try { return publicado(c, 10, null, false); }
            catch (Exception ex) { throw new IllegalStateException(ex); }
        });
    }

    private String rota(NovoContrato n) { return base(n.id()) + "/provas-historicas"; }

    private Map<String, Object> fonteOriginal(NovoContrato n) {
        return emTransacao(() -> {
            var contrato = em.find(Contrato.class, n.id());
            var versao = motor.fotografiaOriginalConhecida(contrato, Instant.now()).orElseThrow();
            @SuppressWarnings("unchecked") Map<String, Object> foto = mapper.convertValue(motor.paraVersaoDTO(versao), LinkedHashMap.class);
            for (String campo : List.of("id", "numeroVersao", "eventoOrigemId", "registradoEm", "tipoContratacaoComprovado"))
                foto.remove(campo);
            var comp = em.find(ComposicaoContratual.class, n.composicao1());
            var item = mapa("composicaoId", comp.getId(), "lotacaoNome", comp.getLotacaoNomeFotografia(),
                    "jornadaDescricao", comp.getJornadaDescricaoFotografia(), "cargoNome", comp.getCargoNomeFotografia(),
                    "lotacaoMunicipio", comp.getLotacaoMunicipioFotografia(), "municipioComprovado", true,
                    "jornadaSemanal", comp.getJornadaSemanalFotografia(), "jornadaComprovada", true);
            return mapa("eventoOrigemId", versao.getEventoOrigem().getId(), "versaoAnteriorId", versao.getId(),
                    "dataDocumento", RelogioSistema.hoje().minusDays(40), "referenciaSei", "SEI-VALIDACAO-INDEPENDENTE",
                    "motivo", "Conferência de fonte original sintética preservando campos não comprovados",
                    "autorizacaoAdministrativa", "ATO-CONFERENCIA", "versaoContratoEsperada", contrato.getVersion(),
                    "chaveIdempotencia", UUID.randomUUID().toString(), "configuracaoCompletaComprovada", true,
                    "fotografia", foto, "composicoes", List.of(item));
        });
    }
}
