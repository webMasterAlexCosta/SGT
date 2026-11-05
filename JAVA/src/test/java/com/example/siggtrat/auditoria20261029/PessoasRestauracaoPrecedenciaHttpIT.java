package com.example.siggtrat.auditoria20261029;

import com.example.siggtrat.dominio.vinculo.CategoriaTerminoVinculo;
import com.example.siggtrat.dominio.catalogo.TipoCatalogoTemporal;
import com.example.siggtrat.entidade.EventoDisponibilidadeCatalogo;
import com.example.siggtrat.entidade.MotivoDesligamento;
import com.example.siggtrat.entidade.VinculoContratual;
import com.example.siggtrat.utilitario.RelogioSistema;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@Transactional(propagation = Propagation.NOT_SUPPORTED)
class PessoasRestauracaoPrecedenciaHttpIT extends ContratosHttpBase {
    @org.springframework.beans.factory.annotation.Autowired
    private com.example.siggtrat.servico.GestaoContratoTemporalServico gestaoDaFonte;

    private static final String V = "/api/vinculos-contratuais";

    @Test
    void restaurarSucessoraNoNovoUltimoDiaExigePrecedenciaExplicita() throws Exception {
        Preparado f = emTransacao(() -> {
            try {
                Cenario c = criarCenario();
                NovoContrato contrato = publicado(c, 2, 1, false);
                MotivoDesligamento motivo = new MotivoDesligamento();
                motivo.setGestaoLocal(c.gestaoA);
                motivo.setDescricao("Desligamento precedência restaurada " + contrato.id());
                motivo.setCategoria(CategoriaTerminoVinculo.DESLIGAMENTO_INDIVIDUAL);
                em.persist(motivo); em.flush();
                EventoDisponibilidadeCatalogo origem = new EventoDisponibilidadeCatalogo();
                origem.setTipoCatalogo(TipoCatalogoTemporal.MOTIVO_DESLIGAMENTO);
                origem.setCatalogoId(motivo.getId()); origem.setDataEfetiva(contrato.inicio()); origem.setAtivo(true);
                origem.setMotivo("Fonte sintética explícita da causa individual desde o início do cenário");
                em.persist(origem); em.flush();
                // Fonte contextual sintética opt-in deste cenário; disponibilidade permanece fonte separada.
                FonteMotivoSintetica.declarar(em, gestaoDaFonte, contrato.id(), motivo.getId(), c.gestaoA.getId(), c.localA,
                        "Desligamento individual documentado para restauração de precedência", CategoriaTerminoVinculo.DESLIGAMENTO_INDIVIDUAL,
                        RelogioSistema.hoje().minusDays(15), RelogioSistema.hoje().minusDays(10), java.time.Instant.now(), "SEI-SINTETICO-C5-PES-RESTAURACAO-PRECEDENCIA");
                return new Preparado(c, contrato, motivo.getId());
            } catch (Exception e) { throw new IllegalStateException(e); }
        });
        int seed = Math.toIntExact(710000000L + f.contrato().id());
        long vinculo = enviar(f.c().localA, V, mapa("pessoa", mapa("nome", "Pessoa precedência " + seed, "cpf", cpf(seed)),
                        "contratoId", f.contrato().id(), "composicaoId", f.contrato().composicao1(),
                        "dataInicio", RelogioSistema.hoje().minusDays(20)), 201,
                "Preparação: vínculo com alocação inicial A").path("id").asLong();
        LocalDate inicioB = RelogioSistema.hoje().minusDays(10);
        Map<String, Object> mudanca = acao();
        mudanca.put("dataEfetiva", inicioB); mudanca.put("composicaoId", f.contrato().composicao2());
        confirmar(mudanca, enviar(f.c().localA, V + "/" + vinculo + "/previas/alocacoes", mudanca, 200,
                "Preparação: prévia da sucessora histórica B"));
        enviar(f.c().localA, V + "/" + vinculo + "/alocacoes", mudanca, 200, "ALO002 registro da faixa histórica B");
        LocalDate fimAnterior = RelogioSistema.hoje().minusDays(15);
        Map<String, Object> desligar = acao();
        desligar.put("dataFim", fimAnterior); desligar.put("motivoDesligamentoId", f.motivoId());
        enviar(f.c().localA, V + "/" + vinculo + "/desligar", desligar, 200,
                "DES003 término retroativo legítimo recorta a cobertura antes de B");

        Map<String, Object> retificar = acao();
        retificar.put("dataFim", inicioB); retificar.put("motivoDesligamentoId", f.motivoId());
        JsonNode previa = enviar(f.c().localA, V + "/" + vinculo + "/previas/retificacao-desligamento", retificar, 200,
                "ALO005 a prévia inclui a sucessora histórica que será restaurada no último dia");
        confirmar(retificar, previa);
        enviar(f.c().localA, V + "/" + vinculo + "/retificar-desligamento", retificar, 409,
                "ALO005 restaurar B exatamente no último dia exige decisão explícita de precedência");
        assertThat(previa.path("impactos").toString().toLowerCase()).contains("precedência");
        assertThat(emTransacao(() -> em.find(VinculoContratual.class, vinculo).getDataFim())).isEqualTo(fimAnterior);

        retificar.put("confirmarPrecedenciaUltimoDia", true);
        confirmar(retificar, enviar(f.c().localA, V + "/" + vinculo + "/previas/retificacao-desligamento", retificar, 200,
                "Nova prévia com decisão explícita de que B vale durante o último dia"));
        enviar(f.c().localA, V + "/" + vinculo + "/retificar-desligamento", retificar, 200,
                "ALO005 DES002 a restauração confirmada aplica B no último dia e término depois dele");
        JsonNode ultimoDia = consultar(f.c().localA, V + "/" + vinculo + "/alocacoes/posicao?data=" + inicioB,
                "B existe durante o novo último dia");
        assertThat(ultimoDia.path("composicaoBaseId").asLong()).isEqualTo(f.contrato().composicao2());
        assertThat(consultar(f.c().localA, V + "/" + vinculo + "/alocacoes/posicao?data=" + inicioB.plusDays(1),
                "A inatividade começa somente no dia seguinte").path("situacaoTemporal").asText()).isEqualTo("SEM_ALOCACAO");
        assertThat(consultar(f.c().localA, V + "/" + vinculo + "/eventos", "A precedência integra a trilha auditada").toString())
                .contains("ALOCACAO_VALE_NO_ULTIMO_DIA", "DESLIGAMENTO_APOS_O_DIA");
    }

    private Map<String, Object> acao() {
        return mapa("justificativa", "Restauração temporal documentada", "evidencia", "SEI-PRECEDENCIA-RESTAURADA",
                "autorizacao", "ATO-PRECEDENCIA-RESTAURADA");
    }

    private void confirmar(Map<String, Object> req, JsonNode previa) {
        req.put("confirmarImpacto", true); req.put("tokenPrevia", previa.path("token").asText());
    }

    private record Preparado(Cenario c, NovoContrato contrato, long motivoId) {}
}
