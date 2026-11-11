package com.example.siggtrat.auditoria20261029;

import com.example.siggtrat.dominio.acesso.TipoPermissaoExcepcional;
import com.example.siggtrat.entidade.ComposicaoContratual;
import com.example.siggtrat.entidade.Contrato;
import com.example.siggtrat.entidade.VersaoContratoNegocial;
import com.example.siggtrat.servico.MotorTemporalContratoServico;
import com.example.siggtrat.utilitario.RelogioSistema;
import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import javax.sql.DataSource;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

/**
 * HIS-006/HIS-001 e seções 5.1/5.4 do levantamento: uma fonte integral não
 * certifica uma publicação cuja composição original inteira permanece ausente.
 * A perda é simulada exclusivamente em uma fixture PostgreSQL descartável.
 */
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class ProvaIntegralSemComposicaoOriginalHttpIT extends ContratosHttpBase {
    @Autowired MotorTemporalContratoServico motor;
    @Autowired DataSource dataSource;

    @BeforeEach
    void exigirMySqlReal() throws Exception {
        try (var conexao = dataSource.getConnection()) {
            assertThat(conexao.getMetaData().getDatabaseProductName()).isEqualToIgnoringCase("PostgreSQL");
        }
    }

    @Test
    void fonteIntegralSemQualquerComposicaoOriginalNaoRecebePreviaNemProduzProva() throws Exception {
        var c = criarCenarioPersistido();
        var n = novo(c);
        var req = fonteOriginal(n);
        long versaoOriginalId = ((Number) req.get("versaoAnteriorId")).longValue();

        emTransacao(() -> {
            assertThat(em.createQuery("select count(v) from VinculoContratual v where v.contrato.id=:c", Long.class)
                    .setParameter("c", n.id()).getSingleResult()).isZero();
            assertThat(em.find(ComposicaoContratual.class, n.composicao1()).getEfetivoPrevisto()).isEqualTo(10);
            // DML de preparação: representa acervo incompleto; não é operação
            // autorizada de descarte de história nem alteração manual de schema.
            assertThat(em.createQuery("delete from ComposicaoContratual c where c.contrato.id=:c")
                    .setParameter("c", n.id()).executeUpdate()).isEqualTo(1);
            return null;
        });
        req.put("composicoes", List.of());
        conceder(c, n.id(), "INCORPORACAO_PROVA_HISTORICA");
        Estado antes = estado(n.id(), versaoOriginalId);
        assertThat(antes.composicoes()).isZero();
        assertThat(antes.efetivoAtual()).isEqualTo(10);

        var previa = mvc.perform(autenticar(post(rota(n) + "/previa")
                .contentType(MediaType.APPLICATION_JSON).content(json(req)), c.localA)).andReturn();
        var verificacao = new SoftAssertions();
        verificacao.assertThat(previa.getResponse().getStatus())
                .as("HIS-006: efetivo 10 sem qualquer composição não possui fotografia integral")
                .isIn(400, 409);

        // Se a versão defeituosa emitir um token, exercita também a confirmação
        // real: o RED registra a certificação incorreta e seus efeitos persistidos.
        if (previa.getResponse().getStatus() == 200) {
            String token = dados(previa).path("tokenPrevia").asText();
            req.put("tokenPrevia", token);
            req.put("confirmarImpacto", true);
            var confirmacao = mvc.perform(autenticar(post(rota(n))
                    .contentType(MediaType.APPLICATION_JSON).content(json(req)), c.localA)).andReturn();
            verificacao.assertThat(confirmacao.getResponse().getStatus())
                    .as("HIS-006: a confirmação também deve recusar uma origem estruturalmente incompleta")
                    .isIn(400, 409);
        }

        verificacao.assertThat(estado(n.id(), versaoOriginalId))
                .as("Recusa atômica conserva fotografia, versão contratual, eventos e concessão sem incorporar prova")
                .isEqualTo(antes);
        verificacao.assertAll();
    }

    @Test
    void fonteIntegralComComposicaoOriginalCoerenteContinuaIncorporavel() throws Exception {
        var c = criarCenarioPersistido();
        var n = novo(c);
        var req = fonteOriginal(n);
        long versaoOriginalId = ((Number) req.get("versaoAnteriorId")).longValue();
        conceder(c, n.id(), "INCORPORACAO_PROVA_HISTORICA");
        Estado antes = estado(n.id(), versaoOriginalId);
        assertThat(antes.composicoes()).isEqualTo(1);

        var previa = enviar(c.localA, rota(n) + "/previa", req, 200,
                "HIS-006: fonte integral identifica a composição original e o efetivo 10");
        req.put("tokenPrevia", previa.path("tokenPrevia").asText());
        req.put("confirmarImpacto", true);
        var recibo = enviar(c.localA, rota(n), req, 201,
                "HIS-006: fonte completa produz conhecimento novo sem apagar a fotografia original");
        assertThat(recibo.path("configuracaoCompletaComprovada").asBoolean()).isTrue();
        assertThat(recibo.path("id").asLong()).isPositive();

        Estado depois = estado(n.id(), versaoOriginalId);
        assertThat(depois.provas()).isEqualTo(antes.provas() + 1);
        assertThat(depois.fotografias()).isEqualTo(antes.fotografias() + 1);
        assertThat(depois.usosConcessao()).isEqualTo(antes.usosConcessao() + 1);
        assertThat(depois.eventos()).isEqualTo(antes.eventos());
        assertThat(depois.composicoes()).isEqualTo(1);
        assertThat(depois.fotografiaOriginal()).isEqualTo(antes.fotografiaOriginal());
        assertThat(depois.efetivoAtual()).isEqualTo(10);
        assertThat(depois.efetivoOriginal()).isEqualTo(10);
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
            @SuppressWarnings("unchecked") Map<String, Object> foto = mapper.convertValue(
                    motor.paraVersaoDTO(versao), LinkedHashMap.class);
            for (String campo : List.of("id", "numeroVersao", "eventoOrigemId", "registradoEm", "tipoContratacaoComprovado"))
                foto.remove(campo);
            var comp = em.find(ComposicaoContratual.class, n.composicao1());
            var item = mapa("composicaoId", comp.getId(), "lotacaoNome", comp.getLotacaoNomeFotografia(),
                    "jornadaDescricao", comp.getJornadaDescricaoFotografia(), "cargoNome", comp.getCargoNomeFotografia(),
                    "lotacaoMunicipio", comp.getLotacaoMunicipioFotografia(), "municipioComprovado", true,
                    "jornadaSemanal", comp.getJornadaSemanalFotografia(), "jornadaComprovada", true);
            return mapa("eventoOrigemId", versao.getEventoOrigem().getId(), "versaoAnteriorId", versao.getId(),
                    "dataDocumento", RelogioSistema.hoje().minusDays(40), "referenciaSei", "SEI-FIXTURE-COMPOSICAO-INTEGRAL",
                    "motivo", "Fonte sintética de teste para verificar completude estrutural da publicação",
                    "autorizacaoAdministrativa", "ATO-HTTP", "versaoContratoEsperada", contrato.getVersion(),
                    "chaveIdempotencia", UUID.randomUUID().toString(), "configuracaoCompletaComprovada", true,
                    "fotografia", foto, "composicoes", List.of(item));
        });
    }

    private Estado estado(long contratoId, long versaoOriginalId) {
        return emTransacao(() -> {
            var contrato = em.find(Contrato.class, contratoId);
            var original = em.find(VersaoContratoNegocial.class, versaoOriginalId);
            long fotografias = em.createQuery("select count(v) from VersaoContratoNegocial v where v.contrato.id=:c", Long.class)
                    .setParameter("c", contratoId).getSingleResult();
            long eventos = em.createQuery("select count(e) from EventoCicloVidaContrato e where e.contrato.id=:c", Long.class)
                    .setParameter("c", contratoId).getSingleResult();
            long provas = em.createQuery("select count(p) from ProvaHistoricaContrato p where p.contrato.id=:c", Long.class)
                    .setParameter("c", contratoId).getSingleResult();
            long usos = em.createQuery("select count(u) from UsoPermissaoExcepcional u where u.permissao.contrato.id=:c and u.tipo=:tipo", Long.class)
                    .setParameter("c", contratoId)
                    .setParameter("tipo", TipoPermissaoExcepcional.INCORPORACAO_PROVA_HISTORICA).getSingleResult();
            long composicoes = em.createQuery("select count(c) from ComposicaoContratual c where c.contrato.id=:c", Long.class)
                    .setParameter("c", contratoId).getSingleResult();
            return new Estado(contrato.getVersion(), fotografias, eventos, provas, usos, composicoes,
                    contrato.getEfetivoPrevisto(), contrato.getEfetivoOriginalPublicado(),
                    mapper.valueToTree(motor.paraVersaoDTO(original)).toString());
        });
    }

    private record Estado(long versaoContrato, long fotografias, long eventos, long provas,
                          long usosConcessao, long composicoes, Integer efetivoAtual,
                          Integer efetivoOriginal, String fotografiaOriginal) {}
}
