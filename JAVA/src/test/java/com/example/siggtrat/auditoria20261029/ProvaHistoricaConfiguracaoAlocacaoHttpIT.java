package com.example.siggtrat.auditoria20261029;

import com.example.siggtrat.entidade.*;
import com.example.siggtrat.servico.MotorTemporalContratoServico;
import com.example.siggtrat.servico.ResolvedorContextoTemporalFrequenciaServico;
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

/** Prova tipada do mapa viabiliza somente alocações correspondentes, sem preencher o mapa antigo. */
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class ProvaHistoricaConfiguracaoAlocacaoHttpIT extends ContratosHttpBase {
    @Autowired MotorTemporalContratoServico motor;
    @Autowired ResolvedorContextoTemporalFrequenciaServico resolvedor;

    @Test
    void documentoTardioDoMapaPermiteNovoVinculoNoDestinoSemRecapturarMestreAtual() throws Exception {
        var c = criarCenarioPersistido();
        var n = emTransacao(() -> {
            try { return publicado(c, 10, null, false); }
            catch (Exception ex) { throw new IllegalStateException(ex); }
        });
        conceder(c, n.id(), "TRANSFERENCIA_GESTAO_LOCAL");
        var data = RelogioSistema.hoje().minusDays(2);
        enviar(c.localA, base(n.id()) + "/transferir-gestao", mapa(/* Conferência SINTÉTICA explícita desta fixture; não é padrão do produto. */
                "conferenciaDocumentalConfirmada", true, "versaoConferenciaDocumental", "transferencia-gestao-conferencia-v1", "dataEfetiva", data,
                "dataAto", RelogioSistema.hoje(), "gestaoLocalDestinoId", c.gestaoB.getId(),
                "tipoContratacaoDestinoId", c.tipoB.getId(), "motivo", "Transferência com mapa documental sintético",
                "referenciaSei", "SEI-TRANSFERENCIA-MAPA", "autorizacaoAdministrativa", "ATO-HTTP",
                "mapeamentos", List.of(mapa("composicaoOrigemId", n.composicao1(),
                        "lotacaoDestinoId", c.lotacaoB.getId(), "jornadaDestinoId", c.jornadaB.getId(),
                        "cargoDestinoId", c.cargoB.getId()))), 200, "Transferência original válida e documentada");
        var fonte = emTransacao(() -> {
            var t = em.createQuery("select t from TransferenciaGestaoContrato t where t.contrato.id=:c", TransferenciaGestaoContrato.class)
                    .setParameter("c", n.id()).getSingleResult();
            var v = em.createQuery("select v from VersaoContratoNegocial v where v.eventoOrigem.id=:e", VersaoContratoNegocial.class)
                    .setParameter("e", t.getEvento().getId()).getSingleResult();
            @SuppressWarnings("unchecked") Map<String, Object> foto = mapper.convertValue(motor.paraVersaoDTO(v), LinkedHashMap.class);
            for (String campo : List.of("id", "numeroVersao", "eventoOrigemId", "registradoEm", "tipoContratacaoComprovado")) foto.remove(campo);
            var m = t.getMapeamentos().get(0);
            var item = mapa("mapeamentoId", m.getId(), "lotacaoNome", m.getLotacaoDestinoNomeFotografia(),
                    "jornadaDescricao", m.getJornadaDestinoDescricaoFotografia(), "cargoNome", m.getCargoDestinoNomeFotografia(),
                    "lotacaoMunicipio", m.getLotacaoDestinoMunicipioFotografia(), "municipioComprovado", true,
                    "jornadaSemanal", m.getJornadaDestinoSemanalFotografia(), "jornadaComprovada", true);
            var req = mapa("eventoOrigemId", t.getEvento().getId(), "versaoAnteriorId", v.getId(),
                    "dataDocumento", data, "referenciaSei", "SEI-FONTE-ORIGINAL-MAPA",
                    "motivo", "Fonte original sintética encontrada depois da perda parcial do mapa",
                    "autorizacaoAdministrativa", "ATO-CONFERENCIA-MAPA", "versaoContratoEsperada", t.getContrato().getVersion(),
                    "chaveIdempotencia", UUID.randomUUID().toString(), "configuracaoCompletaComprovada", true,
                    "fotografia", foto, "composicoes", List.of(item));
            em.createQuery("update MapeamentoTransferenciaGestao m set m.jornadaDestinoSemanalFotografia=null, "
                            + "m.cargoDestinoNomeFotografia='' where m.id=:id").setParameter("id", m.getId()).executeUpdate();
            em.find(Jornada.class, c.jornadaB.getId()).setDescricao("Mestre posterior não documenta o mapa antigo");
            em.find(Lotacao.class, c.lotacaoB.getId()).setNome("Lotação renomeada depois do mapa");
            em.find(Lotacao.class, c.lotacaoB.getId()).setMunicipio("Município atual diferente");
            return req;
        });
        var vinculo = mapa("contratoId", n.id(), "composicaoId", n.composicao1(),
                "lotacaoId", c.lotacaoB.getId(), "jornadaId", c.jornadaB.getId(), "cargoId", c.cargoB.getId(),
                "dataInicio", RelogioSistema.hoje().plusDays(2),
                "pessoa", mapa("nome", "Pessoa de novo ato após prova do mapa", "cpf", cpf(700000000 + Math.toIntExact(n.id()))));
        enviar(c.localB, "/api/vinculos-contratuais", vinculo, 409,
                "Ausência de grade no mapa impede copiar a grade do mestre para novo vínculo");
        enviar(c.admin, "/api/permissoes-excepcionais", mapa("usuarioBeneficiarioId", c.localB.getId(), "contratoId", n.id(),
                "tipo", "INCORPORACAO_PROVA_HISTORICA", "finalidade", "Conferência da fonte original do mapa",
                "justificativa", "Documento original sintético de configuração", "autorizacaoAdministrativa", "ATO-CONFERENCIA-MAPA",
                "validoAte", Instant.now().plusSeconds(7200)), 201, "Concessão específica ao responsável atual");
        var rota = base(n.id()) + "/provas-historicas";
        var previa = enviar(c.localB, rota + "/previa", fonte, 200, "Prévia confere a fonte original do mapa");
        fonte.put("tokenPrevia", previa.path("tokenPrevia").asText());
        fonte.put("confirmarImpacto", true);
        enviar(c.localB, rota, fonte, 201, "Configuração é incorporada com conhecimento atual sem alterar mapa antigo");
        var salvo = enviar(c.localB, "/api/vinculos-contratuais", vinculo, 201,
                "Novo vínculo usa a configuração documentada do exato mapa de destino");
        @SuppressWarnings("unchecked") var item = ((List<Map<String, Object>>) fonte.get("composicoes")).get(0);
        emTransacao(() -> {
            var a = em.createQuery("select a from AlocacaoVinculo a where a.vinculo.id=:v", AlocacaoVinculo.class)
                    .setParameter("v", salvo.path("id").asLong()).getSingleResult();
            assertThat(a.getLotacaoNomeFotografia()).isEqualTo(item.get("lotacaoNome"));
            assertThat(a.getJornadaDescricaoFotografia()).isEqualTo(item.get("jornadaDescricao"));
            assertThat(a.getCargoNomeFotografia()).isEqualTo(item.get("cargoNome"));
            assertThat(a.getLotacaoMunicipioFotografia()).isEqualTo(item.get("lotacaoMunicipio"));
            assertThat(a.getJornadaSemanalFotografia()).isEqualTo(item.get("jornadaSemanal"));
            var m = em.find(MapeamentoTransferenciaGestao.class, ((Number) item.get("mapeamentoId")).longValue());
            assertThat(m.getJornadaDestinoSemanalFotografia()).isNull();
            assertThat(m.getCargoDestinoNomeFotografia()).isEmpty();
            return null;
        });
        emTransacao(() -> {
            em.createQuery("update AlocacaoVinculo a set a.jornadaSemanalFotografia=null, "
                            + "a.lotacaoMunicipioFotografia=null where a.vinculo.id=:v")
                    .setParameter("v", salvo.path("id").asLong()).executeUpdate();
            return null;
        });
        emTransacao(() -> {
            var contexto = resolvedor.resolverPorVinculo(salvo.path("id").asLong(), RelogioSistema.hoje().plusDays(2));
            var configuracao = contexto.configuracaoAlocacao();
            assertThat(configuracao.jornadaSemanal()).isEqualTo(item.get("jornadaSemanal"));
            assertThat(configuracao.lotacaoMunicipio()).isEqualTo(item.get("lotacaoMunicipio"));
            assertThat(contexto.alocacao().getJornadaSemanalFotografia()).isNull();
            assertThat(contexto.alocacao().getLotacaoMunicipioFotografia()).isNull();
            return null;
        });
    }
}
