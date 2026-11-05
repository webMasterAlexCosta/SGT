package com.example.siggtrat.auditoria20261029;

import com.example.siggtrat.entidade.Frequencia;
import com.example.siggtrat.utilitario.RelogioSistema;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/** AUD004/TMP009: mudar a data também afeta o intervalo retirado do ato original. */
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class ConclusaoAlcancePreviaContratoHttpIT extends ContratosHttpBase {
    @ParameterizedTest
    @ValueSource(strings = {"inativar", "transferir-gestao"})
    void previaIncluiIntervaloRetiradoDoAtoEAssinaDependenciaNesseIntervalo(String operacao) throws Exception {
        var c = criarCenarioPersistido();
        long contratoId = c.contratoA.getId();
        LocalDate frequenciaData = RelogioSistema.hoje().minusDays(5);
        long frequenciaId = emTransacao(() -> {
            var f = frequencia(c, false, frequenciaData, "FALTA"); em.flush(); return f.getId();
        });
        LocalDate original = frequenciaData.minusDays(1), corrigida = frequenciaData.plusDays(1);
        Map<String, Object> req = mapa("dataEfetiva", original, "dataAto", RelogioSistema.hoje(),
                "motivo", "Ato original anterior à frequência comprovada", "referenciaSei", "SEI-ALCANCE-ORIGINAL");
        if (operacao.equals("transferir-gestao")) {
            conceder(c, contratoId, "TRANSFERENCIA_GESTAO_LOCAL");
            req.put("gestaoLocalDestinoId", c.gestaoB.getId()); req.put("tipoContratacaoDestinoId", c.tipoB.getId());
            req.put("autorizacaoAdministrativa", "ATO-HTTP");
            req.put("mapeamentos", List.of(mapa("composicaoOrigemId", c.composicaoA.getId(),
                    "lotacaoDestinoId", c.lotacaoB.getId(), "jornadaDestinoId", c.jornadaB.getId(), "cargoDestinoId", c.cargoB.getId())));
        } else {
            var previa = enviar(c.localA, base(contratoId) + "/previas/inativar", req, 200, "Prévia do ato inicial");
            req.put("tokenPrevia", previa.path("tokenPrevia").asText()); req.put("confirmarImpacto", true);
        }
        enviar(c.localA, base(contratoId) + "/" + operacao, req, 200, "Ato inicial comprovado e confirmado");
        var executor = operacao.equals("transferir-gestao") ? c.localB : c.localA;
        if (operacao.equals("transferir-gestao"))
            enviar(c.admin, "/api/permissoes-excepcionais", mapa("tipo", "TRANSFERENCIA_GESTAO_LOCAL",
                    "usuarioBeneficiarioId", c.localB.getId(), "contratoId", contratoId, "finalidade", "Retificação de alcance",
                    "justificativa", "Correção documental", "autorizacaoAdministrativa", "ATO-HTTP",
                    "validoAte", Instant.now().plusSeconds(7200)), 201, "PER012 autorização própria da Gestão atual");
        String tipo = operacao.equals("transferir-gestao") ? "TRANSFERENCIA_GESTAO_LOCAL" : "INATIVACAO_ADMINISTRATIVA";
        var eventos = consultar(executor, base(contratoId) + "/eventos", "Evento original identificável");
        long eventoId = java.util.stream.StreamSupport.stream(eventos.spliterator(), false)
                .filter(e -> e.path("tipoEvento").asText().equals(tipo)).findFirst().orElseThrow().path("id").asLong();
        req.put("dataEfetiva", corrigida); req.put("motivo", "Correção que retira dois dias do alcance anterior");
        req.put("eventoPrecedenteId", eventoId); req.put("relacaoPrecedencia", "RETIFICA");
        req.remove("tokenPrevia"); req.remove("confirmarImpacto");
        String rotaPrevia = base(contratoId) + (operacao.equals("transferir-gestao")
                ? "/transferencias-gestao/previa" : "/previas/inativar");
        var previa = enviar(executor, rotaPrevia, req, 200, "AUD004 a prévia cobre ambos os alcances da retificação");
        assertThat(previa.path("quantidadeFrequenciasAfetadas").asLong()).isEqualTo(1L);
        assertThat(previa.path("datasComFrequenciasAfetadas").toString()).contains(frequenciaData.toString());
        req.put("tokenPrevia", previa.path("tokenPrevia").asText()); req.put("confirmarImpacto", true);
        emTransacao(() -> {
            // Simula modificação concorrente da dependência em transação distinta.
            em.find(Frequencia.class, frequenciaId).setObservacao("Dependência alterada após a prévia na faixa retirada");
            return null;
        });
        enviar(executor, base(contratoId) + "/" + operacao, req, 409,
                "AUD004 mudança apenas na faixa retirada também invalida o token anterior");
        var atualizada = enviar(executor, rotaPrevia, req, 200, "Recalcular após alteração real da dependência");
        assertThat(atualizada.path("tokenPrevia").asText()).isNotEqualTo(previa.path("tokenPrevia").asText());
        req.put("tokenPrevia", atualizada.path("tokenPrevia").asText());
        enviar(executor, base(contratoId) + "/" + operacao, req, 200, "Confirmação corresponde ao alcance integral conhecido");
    }
}
