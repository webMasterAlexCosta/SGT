package com.example.siggtrat.auditoria20261029;

import com.example.siggtrat.entidade.Contrato;
import com.example.siggtrat.entidade.EventoCicloVidaContrato;
import com.example.siggtrat.entidade.SituacaoFatoNegocial;
import com.example.siggtrat.entidade.TipoEventoCicloVidaContrato;
import com.example.siggtrat.utilitario.RelogioSistema;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.http.MediaType;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import javax.sql.DataSource;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

/**
 * Proposta independente de comprovação ADI-010:002/003 e REM-004:004.
 * Não pressupõe defeito: execução GREEN direta pode completar prova de código correto.
 * O revisor não compilou nem executou. Fixture/SEI são sintéticos e explícitos.
 * Sem mocks de persistência, H2 ou DDL; leituras em novas transações após HTTP.
 * MockMvc não substitui o gate separado TCP/JAR/Redis.
 */
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class InstrumentosFronteiraCicloVidaHttpIT extends ContratosHttpBase {
    @Autowired DataSource dataSource;
    @Autowired Environment ambiente;

    @BeforeEach
    void exigirPersistenciaPermitida() throws Exception {
        try (var conexao = dataSource.getConnection()) {
            var meta = conexao.getMetaData();
            assertThat(meta.getDatabaseProductName()).isEqualToIgnoringCase("PostgreSQL");
            assertThat(meta.getDatabaseProductVersion()).matches("18\\.6(?: \\(Debian [^()]+\\))?");
        }
        assertThat(ambiente.getProperty("spring.jpa.hibernate.ddl-auto")).isEqualTo("update");
        assertThat(ambiente.getProperty("spring.sql.init.mode")).isEqualTo("never");
    }

    @ParameterizedTest(name = "{0} não pode {1} a inativação administrativa")
    @CsvSource({"aditamentos,CANCELA", "aditamentos,RETIFICA", "aditamentos,SUBSTITUI",
            "remanejamentos,CANCELA", "remanejamentos,RETIFICA", "remanejamentos,SUBSTITUI"})
    void instrumentoNaoInvalidaEventoAdministrativoPorRelacaoDeOutroDominio(String recurso, String relacao)
            throws Exception {
        Preparado p = preparar();
        Map<String, Object> req = recurso.equals("aditamentos")
                ? aditamento("ADI-FRONTEIRA-" + p.n().id(), p.dia())
                : remanejamento(p.n(), "REM-FRONTEIRA-" + p.n().id(), p.dia(), 1);
        if (recurso.equals("aditamentos")) req.put("novaDescricao", "Descrição que não pode cancelar inativação");
        req.put("eventoPrecedenteId", p.inativacao());
        req.put("relacaoPrecedencia", relacao);
        String rota = base(p.n().id()) + "/" + recurso;
        req.put("tokenPrevia", enviar(p.c().localA, rota + "/previa", req, 200,
                "Os campos e impactos próprios do instrumento são válidos").path("tokenPrevia").asText());
        req.put("confirmarImpacto", true);
        String antes = estadoPersistido(p);
        var recusado = executar(p.c().localA, post(rota).contentType(MediaType.APPLICATION_JSON).content(json(req)),
                400, "Relação destrutiva entre instrumento e ciclo é recusada pelo domínio");
        assertThat(recusado.getResponse().getContentAsString(StandardCharsets.UTF_8))
                .contains("A relação não pode invalidar fatos de outra operação negocial");
        assertThat(estadoPersistido(p)).as("Recusa após prévia não deixa evento, instrumento, versão ou relação parcial")
                .isEqualTo(antes);
        conferirEstadoAdministrativo(p);
    }

    @Test
    void sucedeMistoOrdenaAditamentoSemInvalidarInativacaoNemReativarOperacao() throws Exception {
        Preparado p = preparar();
        String eventoOriginal = eventoOriginal(p);
        var req = aditamento("ADI-ORDEM-MISTA-" + p.n().id(), p.dia());
        req.put("novaDescricao", "Descrição posterior explicitamente ordenada");
        req.put("eventoPrecedenteId", p.inativacao());
        req.put("relacaoPrecedencia", "SUCEDE");
        String rota = base(p.n().id()) + "/aditamentos";
        req.put("tokenPrevia", enviar(p.c().localA, rota + "/previa", req, 200,
                "SUCEDE entre tipos diferentes ordena sem invalidar").path("tokenPrevia").asText());
        req.put("confirmarImpacto", true);
        var adi = enviar(p.c().localA, rota, req, 201, "Ordem mista válida com persistência real");
        conferirEstadoAdministrativo(p);
        assertThat(eventoOriginal(p)).isEqualTo(eventoOriginal);
        JsonNode pos = consultar(p.c().localA, base(p.n().id()) + "/posicao-temporal?data=" + p.dia(),
                "Ambos os fatos continuam válidos no mesmo recorte");
        assertThat(pos.path("versaoContratual").path("descricao").asText()).isEqualTo(req.get("novaDescricao"));
        assertThat(pos.path("eventosAplicados").findValues("id")).extracting(JsonNode::asLong)
                .contains(p.inativacao(), adi.path("eventoId").asLong());
        JsonNode relacoes = consultar(p.c().localA, base(p.n().id()) + "/eventos/precedencias",
                "Relação SUCEDE preservada entre fonte e alvo exatos");
        assertThat(relacoes).hasSize(1);
        assertThat(relacoes.get(0).path("tipoRelacao").asText()).isEqualTo("SUCEDE");
        assertThat(relacoes.get(0).path("eventoFonteId").asLong()).isEqualTo(adi.path("eventoId").asLong());
        assertThat(relacoes.get(0).path("eventoAlvoId").asLong()).isEqualTo(p.inativacao());
        emTransacao(() -> {
            var alvo = em.find(EventoCicloVidaContrato.class, p.inativacao());
            assertThat(alvo.getSituacaoFato()).isEqualTo(SituacaoFatoNegocial.VIGENTE);
            var fonte = em.find(EventoCicloVidaContrato.class, adi.path("eventoId").asLong());
            assertThat(fonte.getTipoEvento()).isEqualTo(TipoEventoCicloVidaContrato.ADITAMENTO);
            assertThat(fonte.getSituacaoFato()).isEqualTo(SituacaoFatoNegocial.VIGENTE);
            return null;
        });
    }

    private Preparado preparar() throws Exception {
        Cenario c = criarCenarioPersistido();
        NovoContrato n = emTransacao(() -> {
            try { return publicado(c, 100, 60, false); }
            catch (Exception ex) { throw new IllegalStateException(ex); }
        });
        LocalDate dia = RelogioSistema.hoje().plusDays(4);
        var req = mapa("dataEfetiva", dia, "dataAto", RelogioSistema.hoje(),
                "motivo", "Inativação futura sintética para verificar separação dos domínios",
                "referenciaSei", "SEI-SINTETICO-FRONTEIRA-CICLO");
        req.put("tokenPrevia", enviar(c.localA, base(n.id()) + "/previas/inativar", req, 200,
                "Prévia da inativação futura").path("tokenPrevia").asText());
        req.put("confirmarImpacto", true);
        enviar(c.localA, base(n.id()) + "/inativar", req, 200, "Estado administrativo futuro tem operação própria");
        long inativacao = emTransacao(() -> em.createQuery(
                        "select e.id from EventoCicloVidaContrato e where e.contrato.id=:c and e.tipoEvento=:t", Long.class)
                .setParameter("c", n.id()).setParameter("t", TipoEventoCicloVidaContrato.INATIVACAO_ADMINISTRATIVA)
                .getSingleResult());
        Preparado p = new Preparado(c, n, dia, inativacao);
        assertThat(consultar(c.localA, base(n.id()), "Hoje a programação não antecipa bloqueio")
                .path("habilitacaoAdministrativa").asText()).isEqualTo("ATIVO");
        conferirEstadoAdministrativo(p);
        return p;
    }

    private void conferirEstadoAdministrativo(Preparado p) throws Exception {
        JsonNode pos = consultar(p.c().localA, base(p.n().id()) + "/posicao-temporal?data=" + p.dia(),
                "Instrumento não pode reativar nem retirar evento administrativo");
        assertThat(pos.path("estadoNegocial").asText()).isEqualTo("PUBLICADO");
        assertThat(pos.path("situacaoTemporal").asText()).isEqualTo("VIGENTE");
        assertThat(pos.path("habilitacaoAdministrativa").asText()).isEqualTo("INATIVO");
    }

    private String eventoOriginal(Preparado p) {
        return emTransacao(() -> {
            var e = em.find(EventoCicloVidaContrato.class, p.inativacao());
            return mapper.valueToTree(mapa("id", e.getId(), "tipo", e.getTipoEvento(),
                    "situacao", e.getSituacaoFato(), "fatoLogico", e.getFatoLogicoId(),
                    "dataEfetiva", e.getDataEfetiva(), "dataAto", e.getDataAto(),
                    "registro", e.getRegistradoEm(), "autor", e.getUsuarioResponsavelId(),
                    "motivo", e.getMotivo(), "fonte", e.getReferenciaSei())).toString();
        });
    }

    private String estadoPersistido(Preparado p) {
        return emTransacao(() -> {
            var c = em.find(Contrato.class, p.n().id());
            return mapper.valueToTree(mapa("versaoContrato", c.getVersion(), "estado", c.getEstadoNegocial(),
                    "ativo", c.getAtivo(), "descricao", c.getDescricao(), "previsto", c.getEfetivoPrevisto(),
                    "eventos", linhas(p, "EventoCicloVidaContrato", "e.id,e.tipoEvento,e.situacaoFato,e.fatoLogicoId,e.registradoEm"),
                    "versoes", linhas(p, "VersaoContratoNegocial", "e.id,e.numeroVersao,e.descricao,e.efetivoPrevisto"),
                    "adis", linhas(p, "AditamentoContratual", "e.id,e.identificacao,e.version"),
                    "rems", linhas(p, "RemanejamentoContratual", "e.id,e.identificacao,e.version"),
                    "relacoes", linhas(p, "RelacaoPrecedenciaEventoContrato", "e.id,e.tipoRelacao,e.eventoFonte.id,e.eventoAlvo.id"),
                    "composicoes", linhas(p, "ComposicaoContratual", "e.id,e.efetivoPrevisto,e.dataInicioVigencia,e.dataFimVigencia"))).toString();
        });
    }

    private java.util.List<Object[]> linhas(Preparado p, String entidade, String campos) {
        return em.createQuery("select " + campos + " from " + entidade + " e where e.contrato.id=:c order by e.id", Object[].class)
                .setParameter("c", p.n().id()).getResultList();
    }

    private record Preparado(Cenario c, NovoContrato n, LocalDate dia, long inativacao) {}
}
