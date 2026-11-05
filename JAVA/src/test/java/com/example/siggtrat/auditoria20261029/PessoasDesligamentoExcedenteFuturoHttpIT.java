package com.example.siggtrat.auditoria20261029;

import com.example.siggtrat.dominio.vinculo.CategoriaTerminoVinculo;
import com.example.siggtrat.entidade.MotivoDesligamento;
import com.example.siggtrat.entidade.VinculoContratual;
import com.example.siggtrat.utilitario.RelogioSistema;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/** Variantes PEO001 ainda sem correção: executar RED antes de ampliar o consentimento de término. */
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class PessoasDesligamentoExcedenteFuturoHttpIT extends ContratosHttpBase {
    private static final String V = "/api/vinculos-contratuais";

    @Test
    void cancelarDesligamentoQueAmpliaFaixaExigeConsentimentoParaOcupanteFuturo() throws Exception {
        Preparado f = prepararComOcupanteFuturo();
        Map<String, Object> req = acao();
        enviar(f.c().localA, V + "/" + f.vinculoId() + "/cancelar-desligamento-programado", req, 409,
                "CMP009 cancelamento de término não pode ampliar faixa e criar excedente futuro sem consentimento específico");
        assertThat(emTransacao(() -> em.find(VinculoContratual.class, f.vinculoId()).getDataFim())).isEqualTo(f.fimAnterior());
        req.put("confirmarInclusaoExcedente", true);
        enviar(f.c().localA, V + "/" + f.vinculoId() + "/cancelar-desligamento-programado", req, 200,
                "CMP008 cancelamento com excedente continua permitido quando a decisão é explícita e documentada");
        assertThat(emTransacao(() -> em.find(VinculoContratual.class, f.vinculoId()).getDataFim())).isNull();
        conferirAuditoria(f, "CANCELAMENTO_DESLIGAMENTO_PROGRAMADO");
    }

    @Test
    void retificarDesligamentoQueAmpliaFaixaExigeConsentimentoParaOcupanteFuturo() throws Exception {
        Preparado f = prepararComOcupanteFuturo();
        Map<String, Object> req = acao();
        req.put("dataFim", RelogioSistema.hoje().plusDays(20));
        req.put("motivoDesligamentoId", f.motivoId());
        JsonNode previa = previa(f, req);
        enviar(f.c().localA, V + "/" + f.vinculoId() + "/retificar-desligamento", req, 409,
                "CMP009 retificar fim não contorna consentimento de inclusão excedente durante a extensão");
        assertThat(previa.path("impactos").toString().toUpperCase()).contains("EXCEDENTE");
        assertThat(emTransacao(() -> em.find(VinculoContratual.class, f.vinculoId()).getDataFim())).isEqualTo(f.fimAnterior());
        req.put("confirmarInclusaoExcedente", true);
        previa(f, req);
        enviar(f.c().localA, V + "/" + f.vinculoId() + "/retificar-desligamento", req, 200,
                "CMP008 extensão confirmada com excedente autorizado mantém fluxo sem teto artificial");
        conferirAuditoria(f, "RETIFICACAO_DESLIGAMENTO");
    }

    private Preparado prepararComOcupanteFuturo() throws Exception {
        Base b = emTransacao(() -> {
            try {
                Cenario c = criarCenario(); NovoContrato contrato = publicado(c, 1, null, false);
                MotivoDesligamento motivo = new MotivoDesligamento(); motivo.setGestaoLocal(c.gestaoA);
                motivo.setDescricao("Desligamento documentado para testar extensão");
                motivo.setCategoria(CategoriaTerminoVinculo.DESLIGAMENTO_INDIVIDUAL); em.persist(motivo); em.flush();
                return new Base(c, contrato, motivo.getId());
            } catch (Exception e) { throw new IllegalStateException(e); }
        });
        long primeiro = cadastrar(b, 0, RelogioSistema.hoje().plusDays(1)).path("id").asLong();
        LocalDate fim = RelogioSistema.hoje().plusDays(5);
        Map<String, Object> desligamento = acao(); desligamento.put("dataFim", fim); desligamento.put("motivoDesligamentoId", b.motivoId());
        enviar(b.c().localA, V + "/" + primeiro + "/desligar", desligamento, 200, "Preparação: último dia anterior ao ocupante futuro");
        cadastrar(b, 1, RelogioSistema.hoje().plusDays(10));
        return new Preparado(b.c(), b.contrato(), primeiro, b.motivoId(), fim);
    }

    private JsonNode cadastrar(Base b, int ordinal, LocalDate inicio) throws Exception {
        return enviar(b.c().localA, V,
                mapa("pessoa", mapa("nome", "Pessoa extensão término " + ordinal,
                                "cpf", cpf(Math.toIntExact(940000000L + b.contrato().id() * 10L + ordinal))),
                        "contratoId", b.contrato().id(), "composicaoId", b.contrato().composicao1(), "dataInicio", inicio),
                201, "Preparação: vínculos distintos não excedem nas faixas originais");
    }

    private Map<String, Object> acao() {
        return mapa("justificativa", "Continuidade adicional necessária e documentada",
                "evidencia", "SEI-EXCEDENTE-EXTENSAO", "autorizacao", "ATO-EXCEDENTE-EXTENSAO",
                "confirmarImpacto", true, "confirmarInclusaoExcedente", false);
    }

    private JsonNode previa(Preparado f, Map<String, Object> req) throws Exception {
        JsonNode previa = enviar(f.c().localA, V + "/" + f.vinculoId() + "/previas/retificacao-desligamento", req, 200,
                "AUD004 prévia deve incluir ocupação de toda a parte estendida");
        req.put("tokenPrevia", previa.path("token").asText());
        return previa;
    }

    private void conferirAuditoria(Preparado f, String tipo) throws Exception {
        JsonNode eventos = consultar(f.c().localA, V + "/" + f.vinculoId() + "/eventos", "AUD001 consentimento específico persiste na trilha");
        var correspondentes = new ArrayList<JsonNode>();
        eventos.forEach(e -> { if (tipo.equals(e.path("tipo").asText()) && e.path("inclusaoExcedente").asBoolean()) correspondentes.add(e); });
        assertThat(correspondentes).hasSize(1);
        assertThat(correspondentes.get(0).path("ocupacaoAposInclusaoExcedente").asLong()).isEqualTo(2);
        assertThat(correspondentes.get(0).path("evidencia").asText()).isEqualTo("SEI-EXCEDENTE-EXTENSAO");
        assertThat(correspondentes.get(0).path("autorizacao").asText()).isEqualTo("ATO-EXCEDENTE-EXTENSAO");
    }

    private record Base(Cenario c, NovoContrato contrato, long motivoId) {}
    private record Preparado(Cenario c, NovoContrato contrato, long vinculoId, long motivoId, LocalDate fimAnterior) {}
}
