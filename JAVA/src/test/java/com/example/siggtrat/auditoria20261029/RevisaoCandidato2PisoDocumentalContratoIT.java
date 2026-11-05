package com.example.siggtrat.auditoria20261029;

import com.example.siggtrat.entidade.ComposicaoContratual;
import com.example.siggtrat.entidade.Contrato;
import com.example.siggtrat.entidade.VersaoContratoNegocial;
import com.example.siggtrat.servico.MotorTemporalContratoServico;
import com.example.siggtrat.utilitario.RelogioSistema;
import com.fasterxml.jackson.databind.JsonNode;
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

/**
 * Revisão independente do candidato 2. Hipótese C2-CTR-01; execução reservada à raiz.
 * Manifesto auditado: 54af0c36b2ca034453dd0ee79da9f390e0cc92f1446947c294681449263f3aac.
 * A perda das duas colunas agregadas é simulada somente em fixture MySQL descartável;
 * as quantidades das composições e o documento sintético original são preservados.
 */
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class RevisaoCandidato2PisoDocumentalContratoIT extends ContratosHttpBase {
    @Autowired MotorTemporalContratoServico motor;

    @Test
    void origemDocumentalCemMantemPisoSetentaECincoAposAumentoSemMarcadorLegado() throws Exception {
        var c = criarCenarioPersistido();
        var n = novo(c);
        long versaoOriginal = recuperarOrigemCem(c, n);
        aumentarParaCentoEQuarenta(c, n);

        var reducao = aditamento("REDUCAO-PISO-DA-FONTE", n.inicio().plusDays(3));
        reducao.put("novoEfetivoTotal", 75);
        reducao.put("alteracoesComposicao", List.of(mapa("composicaoId", n.composicao1(), "quantidadeResultante", 75)));
        JsonNode previa = emTransacao(() -> {
            try { return previaAditamento(c, n.id(), reducao); }
            catch (Exception ex) { throw new IllegalStateException(ex); }
        });
        assertThat(previa.path("efetivoOriginalPublicado").asInt())
                .as("RED-006: a origem recuperada comprova 100; o aumento posterior não muda a base")
                .isEqualTo(100);
        assertThat(previa.path("pisoNormalReducao").asInt())
                .as("RED-006/007: fonte original 100 implica piso 75, mesmo sem marcador legado")
                .isEqualTo(75);
        enviar(c.localA, base(n.id()) + "/aditamentos", reducao, 201,
                "redução 140 para 75 é válida sem concessão de redução excepcional");

        emTransacao(() -> {
            var contrato = em.find(Contrato.class, n.id());
            assertThat(motor.fotografiaOriginalConhecida(contrato, Instant.now()).orElseThrow().getEfetivoPrevisto())
                    .isEqualTo(100);
            assertThat(contrato.getEfetivoPrevisto()).isEqualTo(75);
            assertThat(em.find(VersaoContratoNegocial.class, versaoOriginal).getEfetivoPrevisto()).isNull();
            assertThat(em.find(ComposicaoContratual.class, n.composicao1()).getEfetivoPrevisto()).isEqualTo(100);
            assertThat(em.createQuery("select count(a) from AditamentoContratual a where a.contrato.id=:c", Long.class)
                    .setParameter("c", n.id()).getSingleResult()).isEqualTo(2L);
            return null;
        });
        assertThat(consultar(c.localA, base(n.id()) + "/posicao-temporal?data=" + n.inicio().plusDays(4),
                "a posição após commit usa a redução válida e conserva a base documental")
                .path("versaoContratual").path("efetivoPrevisto").asInt()).isEqualTo(75);
    }

    @Test
    void fonteOriginalCemNaoAutorizaSetentaEQuatroSemConcessaoDeReducaoExcepcional() throws Exception {
        var c = criarCenarioPersistido();
        var n = novo(c);
        long versaoOriginal = recuperarOrigemCem(c, n);
        aumentarParaCentoEQuarenta(c, n);

        var reducao = aditamento("REDUCAO-ABAIXO-DA-FONTE", n.inicio().plusDays(3));
        reducao.put("novoEfetivoTotal", 74);
        reducao.put("alteracoesComposicao", List.of(mapa("composicaoId", n.composicao1(), "quantidadeResultante", 74)));
        emTransacao(() -> {
            try { return previaAditamento(c, n.id(), reducao); }
            catch (Exception ex) { throw new IllegalStateException(ex); }
        });
        enviar(c.localA, base(n.id()) + "/aditamentos", reducao, 409,
                "RED-005/008: a concessão de incorporação não autoriza redução excepcional abaixo de 75");
        emTransacao(() -> {
            assertThat(em.find(Contrato.class, n.id()).getEfetivoPrevisto()).isEqualTo(140);
            assertThat(em.find(VersaoContratoNegocial.class, versaoOriginal).getEfetivoPrevisto()).isNull();
            assertThat(em.createQuery("select count(a) from AditamentoContratual a where a.contrato.id=:c", Long.class)
                    .setParameter("c", n.id()).getSingleResult()).isEqualTo(1L);
            assertThat(em.createQuery("select count(p) from ProvaHistoricaContrato p where p.contrato.id=:c", Long.class)
                    .setParameter("c", n.id()).getSingleResult()).isEqualTo(1L);
            return null;
        });
    }

    private NovoContrato novo(Cenario c) {
        return emTransacao(() -> {
            try { return publicado(c, 100, null, false); }
            catch (Exception ex) { throw new IllegalStateException(ex); }
        });
    }

    private void aumentarParaCentoEQuarenta(Cenario c, NovoContrato n) {
        var aumento = aditamento("AUMENTO-APOS-FONTE", n.inicio().plusDays(2));
        aumento.put("novoEfetivoTotal", 140);
        aumento.put("alteracoesComposicao", List.of(mapa("composicaoId", n.composicao1(), "quantidadeResultante", 140)));
        emTransacao(() -> {
            try { return registrarAditamento(c, n.id(), aumento); }
            catch (Exception ex) { throw new IllegalStateException(ex); }
        });
        assertThat(emTransacao(() -> em.find(Contrato.class, n.id()).getEfetivoPrevisto())).isEqualTo(140);
    }

    private long recuperarOrigemCem(Cenario c, NovoContrato n) throws Exception {
        Map<String, Object> req = emTransacao(() -> {
            var contrato = em.find(Contrato.class, n.id());
            var original = motor.fotografiaOriginalConhecida(contrato, Instant.now()).orElseThrow();
            @SuppressWarnings("unchecked") Map<String, Object> foto = mapper.convertValue(motor.paraVersaoDTO(original), LinkedHashMap.class);
            for (String campo : List.of("id", "numeroVersao", "eventoOrigemId", "registradoEm", "tipoContratacaoComprovado"))
                foto.remove(campo);
            assertThat(foto.get("efetivoPrevisto")).isEqualTo(100);
            var comp = em.find(ComposicaoContratual.class, n.composicao1());
            var item = mapa("composicaoId", comp.getId(), "lotacaoNome", comp.getLotacaoNomeFotografia(),
                    "jornadaDescricao", comp.getJornadaDescricaoFotografia(), "cargoNome", comp.getCargoNomeFotografia(),
                    "lotacaoMunicipio", comp.getLotacaoMunicipioFotografia(), "municipioComprovado", true,
                    "jornadaSemanal", comp.getJornadaSemanalFotografia(), "jornadaComprovada", true);
            return mapa("eventoOrigemId", original.getEventoOrigem().getId(), "versaoAnteriorId", original.getId(),
                    "dataDocumento", RelogioSistema.hoje().minusDays(40), "referenciaSei", "SEI-ORIGEM-CEM-INDEPENDENTE",
                    "motivo", "Documento sintético original comprova cem vagas apesar da perda de agregados no legado",
                    "autorizacaoAdministrativa", "ATO-CONFERENCIA-CEM", "chaveIdempotencia", UUID.randomUUID().toString(),
                    "configuracaoCompletaComprovada", true, "fotografia", foto, "composicoes", List.of(item));
        });
        long original = ((Number) req.get("versaoAnteriorId")).longValue();
        emTransacao(() -> {
            em.find(Contrato.class, n.id()).setEfetivoOriginalPublicado(null);
            em.createQuery("update VersaoContratoNegocial v set v.efetivoPrevisto=null where v.id=:v")
                    .setParameter("v", original).executeUpdate();
            return null;
        });
        req.put("versaoContratoEsperada", emTransacao(() -> em.find(Contrato.class, n.id()).getVersion()));
        conceder(c, n.id(), "INCORPORACAO_PROVA_HISTORICA");
        String rota = base(n.id()) + "/provas-historicas";
        var previa = enviar(c.localA, rota + "/previa", req, 200, "fonte completa de cem compatível com a composição preservada");
        req.put("tokenPrevia", previa.path("tokenPrevia").asText());
        req.put("confirmarImpacto", true);
        enviar(c.localA, rota, req, 201, "recuperação documental autorizada, imutável e com conhecimento novo");
        emTransacao(() -> {
            var contrato = em.find(Contrato.class, n.id());
            assertThat(motor.fotografiaOriginalConhecida(contrato, Instant.now()).orElseThrow().getEfetivoPrevisto()).isEqualTo(100);
            assertThat(em.find(VersaoContratoNegocial.class, original).getEfetivoPrevisto()).isNull();
            assertThat(em.find(ComposicaoContratual.class, n.composicao1()).getEfetivoPrevisto()).isEqualTo(100);
            return null;
        });
        return original;
    }
}
