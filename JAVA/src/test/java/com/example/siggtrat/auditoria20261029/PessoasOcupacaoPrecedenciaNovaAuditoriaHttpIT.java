package com.example.siggtrat.auditoria20261029;

import com.example.siggtrat.dominio.vinculo.CategoriaTerminoVinculo;
import com.example.siggtrat.entidade.AlocacaoVinculo;
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

/**
 * Novos casos RED propostos pela revisão independente de pessoas.
 * Usa MVC, segurança, serviços e MySQL da base existente, sem transação externa
 * envolvendo os comandos exercidos. Não foi executado pelo autor da revisão.
 */
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class PessoasOcupacaoPrecedenciaNovaAuditoriaHttpIT extends ContratosHttpBase {
    private static final String V = "/api/vinculos-contratuais";

    @Test
    void cadastroAnteriorAOcupacaoFuturaExigeConsentimentoDeExcedente() throws Exception {
        Preparado f = preparar(false);
        LocalDate inicioFuturo = RelogioSistema.hoje().plusDays(10);
        cadastrar(f, 0, f.contrato().composicao1(), inicioFuturo);
        Map<String, Object> proposta = cadastro(f, 1, f.contrato().composicao1(), RelogioSistema.hoje().plusDays(1));
        String cpfNovo = ((Map<?, ?>) proposta.get("pessoa")).get("cpf").toString();

        enviar(f.cenario().localA, V, proposta, 409,
                "CMP009 inclusão deve considerar ocupação futura dentro da faixa, não somente o dia inicial");
        emTransacao(() -> {
            assertThat(em.createQuery("select count(p) from Pessoa p where p.cpf=:cpf", Long.class)
                    .setParameter("cpf", cpfNovo).getSingleResult()).isZero();
            assertThat(em.createQuery("select count(v) from VinculoContratual v where v.contrato.id=:contrato", Long.class)
                    .setParameter("contrato", f.contrato().id()).getSingleResult()).isEqualTo(1);
            return null;
        });

        proposta.put("confirmarInclusaoExcedente", true);
        proposta.put("justificativaExcedente", "Cobertura futura adicional autorizada");
        proposta.put("evidenciaExcedente", "SEI-EXCEDENTE-FUTURO");
        proposta.put("autorizacaoExcedente", "ATO-EXCEDENTE-FUTURO");
        JsonNode salvo = enviar(f.cenario().localA, V, proposta, 201,
                "CMP008 CMP010 excedente futuro permanece permitido mediante consentimento específico");
        emTransacao(() -> {
            assertThat(em.createQuery("select count(e) from EventoVinculoContratual e where e.vinculo.id=:v and e.inclusaoExcedente=true", Long.class)
                    .setParameter("v", salvo.path("id").asLong()).getSingleResult()).isEqualTo(1);
            return null;
        });
    }

    @Test
    void alteracaoAnteriorAOcupacaoFuturaExigeConsentimentoDeExcedente() throws Exception {
        Preparado f = preparar(true);
        cadastrar(f, 0, f.contrato().composicao1(), RelogioSistema.hoje().plusDays(10));
        JsonNode sujeito = cadastrar(f, 1, f.contrato().composicao2(), RelogioSistema.hoje().plusDays(1));
        long vinculo = sujeito.path("id").asLong();
        Map<String, Object> proposta = mudanca(RelogioSistema.hoje().plusDays(2), f.contrato().composicao1());
        JsonNode previa = previaAlocacao(f, vinculo, proposta);
        confirmar(proposta, previa);

        enviar(f.cenario().localA, V + "/" + vinculo + "/alocacoes", proposta, 409,
                "CMP009 mudança deve detectar o excedente que começa depois da data inicial proposta");
        assertThat(previa.path("impactos").toString().toUpperCase()).contains("EXCEDENTE");
        emTransacao(() -> {
            var faixas = em.createQuery("select a from AlocacaoVinculo a where a.vinculo.id=:v", AlocacaoVinculo.class)
                    .setParameter("v", vinculo).getResultList();
            assertThat(faixas).hasSize(1);
            assertThat(faixas.get(0).getComposicao().getId()).isEqualTo(f.contrato().composicao2());
            return null;
        });

        proposta.put("confirmarInclusaoExcedente", true);
        confirmar(proposta, previaAlocacao(f, vinculo, proposta));
        enviar(f.cenario().localA, V + "/" + vinculo + "/alocacoes", proposta, 200,
                "CMP008 excedente futuro confirmado não possui teto técnico artificial");
        emTransacao(() -> {
            assertThat(em.createQuery("select count(e) from EventoAlocacaoVinculo e where e.vinculo.id=:v and e.inclusaoExcedente=true", Long.class)
                    .setParameter("v", vinculo).getSingleResult()).isEqualTo(1);
            return null;
        });
    }

    @Test
    void desligamentoRegistradoDepoisDaAlocacaoNoMesmoDiaExigePrecedencia() throws Exception {
        Preparado f = preparar(true);
        LocalDate dia = RelogioSistema.hoje().plusDays(10);
        JsonNode sujeito = cadastrar(f, 0, f.contrato().composicao1(), RelogioSistema.hoje().plusDays(1));
        long vinculo = sujeito.path("id").asLong();
        mudar(f, vinculo, dia);
        Map<String, Object> req = desligamento(f, dia);
        enviar(f.cenario().localA, V + "/" + vinculo + "/desligar", req, 409,
                "ALO005 ordem de registro inversa também exige precedência do último dia");
        emTransacao(() -> {
            assertThat(em.find(VinculoContratual.class, vinculo).getDataFim()).isNull();
            return null;
        });

        req.put("confirmarPrecedenciaUltimoDia", true);
        enviar(f.cenario().localA, V + "/" + vinculo + "/desligar", req, 200,
                "ALO005 confirmação explícita permite alocação no último dia e término depois dele");
        conferirUltimoDiaETrilha(f, vinculo, dia);
    }

    @Test
    void retificacaoDeDesligamentoParaInicioDeAlocacaoExigePrecedencia() throws Exception {
        Preparado f = preparar(true);
        LocalDate dia = RelogioSistema.hoje().plusDays(10);
        JsonNode sujeito = cadastrar(f, 0, f.contrato().composicao1(), RelogioSistema.hoje().plusDays(1));
        long vinculo = sujeito.path("id").asLong();
        mudar(f, vinculo, dia);
        LocalDate fimAnterior = dia.plusDays(10);
        enviar(f.cenario().localA, V + "/" + vinculo + "/desligar", desligamento(f, fimAnterior), 200,
                "Preparação: desligamento em data distinta da mudança de alocação");

        Map<String, Object> req = desligamento(f, dia);
        JsonNode previa = enviar(f.cenario().localA, V + "/" + vinculo + "/previas/retificacao-desligamento", req, 200,
                "DES005 prévia da nova data de término");
        confirmar(req, previa);
        enviar(f.cenario().localA, V + "/" + vinculo + "/retificar-desligamento", req, 409,
                "ALO005 retificação de término não pode contornar a precedência entre fatos do último dia");
        emTransacao(() -> {
            assertThat(em.find(VinculoContratual.class, vinculo).getDataFim()).isEqualTo(fimAnterior);
            return null;
        });

        req.put("confirmarPrecedenciaUltimoDia", true);
        confirmar(req, enviar(f.cenario().localA, V + "/" + vinculo + "/previas/retificacao-desligamento", req, 200,
                "Nova prévia com consentimento explícito de precedência"));
        enviar(f.cenario().localA, V + "/" + vinculo + "/retificar-desligamento", req, 200,
                "DES005 ALO005 retificação confirmada mantém a alocação durante o último dia");
        conferirUltimoDiaETrilha(f, vinculo, dia);
    }

    private Preparado preparar(boolean duasComposicoes) {
        return emTransacao(() -> {
            try {
                Cenario c = criarCenario();
                NovoContrato contrato = publicado(c, duasComposicoes ? 2 : 1, duasComposicoes ? Integer.valueOf(1) : null, false);
                MotivoDesligamento motivo = new MotivoDesligamento();
                motivo.setGestaoLocal(c.gestaoA);
                motivo.setDescricao("Desligamento individual da revisão " + contrato.id());
                motivo.setCategoria(CategoriaTerminoVinculo.DESLIGAMENTO_INDIVIDUAL);
                em.persist(motivo);
                em.flush();
                return new Preparado(c, contrato, motivo.getId());
            } catch (Exception e) {
                throw new IllegalStateException("Falha na preparação por HTTP da revisão independente", e);
            }
        });
    }

    private Map<String, Object> cadastro(Preparado f, int ordinal, long composicao, LocalDate inicio) {
        int seed = Math.toIntExact(900000000L + f.contrato().id() * 10L + ordinal);
        return mapa("pessoa", mapa("nome", "Pessoa revisão " + f.contrato().id() + "-" + ordinal, "cpf", cpf(seed)),
                "contratoId", f.contrato().id(), "composicaoId", composicao, "dataInicio", inicio,
                "confirmarInclusaoExcedente", false);
    }

    private JsonNode cadastrar(Preparado f, int ordinal, long composicao, LocalDate inicio) throws Exception {
        return enviar(f.cenario().localA, V, cadastro(f, ordinal, composicao, inicio), 201,
                "VIN001 preparação integrada de Pessoa, Vínculo e Alocação pela API");
    }

    private Map<String, Object> mudanca(LocalDate dia, long composicao) {
        return mapa("dataEfetiva", dia, "composicaoId", composicao,
                "justificativa", "Mudança documentada da revisão", "evidencia", "SEI-MUDANCA-REVISAO",
                "autorizacao", "ATO-MUDANCA-REVISAO", "confirmarInclusaoExcedente", false,
                "confirmarPrecedenciaUltimoDia", false);
    }

    private JsonNode previaAlocacao(Preparado f, long vinculo, Map<String, Object> req) throws Exception {
        return enviar(f.cenario().localA, V + "/" + vinculo + "/previas/alocacoes", req, 200,
                "AUD004 prévia de alocação com contexto completo");
    }

    private void confirmar(Map<String, Object> req, JsonNode previa) {
        req.put("confirmarImpacto", true);
        req.put("tokenPrevia", previa.path("token").asText());
    }

    private void mudar(Preparado f, long vinculo, LocalDate dia) throws Exception {
        Map<String, Object> req = mudanca(dia, f.contrato().composicao2());
        confirmar(req, previaAlocacao(f, vinculo, req));
        enviar(f.cenario().localA, V + "/" + vinculo + "/alocacoes", req, 200,
                "ALO002 mudança antes do registro do desligamento");
    }

    private Map<String, Object> desligamento(Preparado f, LocalDate dia) {
        return mapa("dataFim", dia, "motivoDesligamentoId", f.motivoId(),
                "justificativa", "Desligamento individual documentado", "evidencia", "SEI-DESLIGAMENTO-REVISAO",
                "autorizacao", "ATO-DESLIGAMENTO-REVISAO", "confirmarPrecedenciaUltimoDia", false);
    }

    private void conferirUltimoDiaETrilha(Preparado f, long vinculo, LocalDate dia) throws Exception {
        JsonNode posicao = consultar(f.cenario().localA, V + "/" + vinculo + "/alocacoes/posicao?data=" + dia,
                "DES002 nova alocação continua válida no último dia");
        assertThat(posicao.path("situacaoTemporal").asText()).isEqualTo("VIGENTE");
        assertThat(posicao.path("composicaoBaseId").asLong()).isEqualTo(f.contrato().composicao2());
        JsonNode depois = consultar(f.cenario().localA, V + "/" + vinculo + "/alocacoes/posicao?data=" + dia.plusDays(1),
                "DES002 inatividade começa no dia seguinte");
        assertThat(depois.path("situacaoTemporal").asText()).isEqualTo("SEM_ALOCACAO");
        JsonNode eventos = consultar(f.cenario().localA, V + "/" + vinculo + "/eventos",
                "AUD001 ALO005 consentimento de precedência permanece na cadeia");
        assertThat(eventos.toString()).contains("PRECEDENCIA", "ALOCACAO_VALE_NO_ULTIMO_DIA", "DESLIGAMENTO_APOS_O_DIA");
    }

    private record Preparado(Cenario cenario, NovoContrato contrato, long motivoId) {}
}
