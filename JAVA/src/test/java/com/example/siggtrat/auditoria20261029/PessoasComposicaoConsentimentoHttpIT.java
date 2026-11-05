package com.example.siggtrat.auditoria20261029;

import com.example.siggtrat.entidade.EventoVinculoContratual;
import com.example.siggtrat.utilitario.RelogioSistema;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/** Controles por HTTP com confirmação da persistência em outra transação. */
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class PessoasComposicaoConsentimentoHttpIT extends ContratosHttpBase {
    private static final String ROTA = "/api/vinculos-contratuais";

    @Test
    void composicaoInexistenteRecusaVinculoSemGravarPessoaReservaOuHistorico() throws Exception {
        Preparado f = preparar();
        String cpf = cpf(Math.toIntExact(743000000L + f.contrato().id()));
        Map<String, Object> req = cadastro(f, cpf, RelogioSistema.hoje());
        req.put("composicaoId", Long.MAX_VALUE);
        List<Long> antes = contagens(f, cpf);
        enviar(f.c().localA, ROTA, req, 400,
                "CMP001 composição inexistente não pode gerar Pessoa, Vínculo, Alocação ou evento parcial");
        assertThat(contagens(f, cpf)).as("Persistência permanece idêntica após rejeição em nova transação")
                .isEqualTo(antes);
        assertThat(antes).containsOnly(0L);
    }

    @Test
    void consentimentoDeExcedenteSemJustificativaRecusaSemGravar() throws Exception {
        Preparado f = preparar();
        ocupar(f);
        String cpf = cpf(Math.toIntExact(744000000L + f.contrato().id()));
        Map<String, Object> req = cadastro(f, cpf, RelogioSistema.hoje());
        req.put("confirmarInclusaoExcedente", true);
        req.put("evidenciaExcedente", "SEI-CONSENTIMENTO-SEM-JUSTIFICATIVA");
        req.put("autorizacaoExcedente", "ATO-CONSENTIMENTO-SEM-JUSTIFICATIVA");
        List<Long> antes = contagens(f, cpf);
        enviar(f.c().localA, ROTA, req, 409,
                "CMP009 consentimento e documentos não dispensam justificativa do excedente");
        assertThat(contagens(f, cpf)).isEqualTo(antes);
        req.put("justificativaExcedente", "   ");
        enviar(f.c().localA, ROTA, req, 409,
                "CMP009 justificativa em branco também não autoriza excedente");
        assertThat(contagens(f, cpf)).isEqualTo(antes);
    }

    @Test
    void excedenteAutorizadoPersisteAutoriaDocumentosDataECapacidadeNoEvento() throws Exception {
        Preparado f = preparar();
        ocupar(f);
        LocalDate dataEfetiva = RelogioSistema.hoje();
        String cpf = cpf(Math.toIntExact(745000000L + f.contrato().id()));
        Map<String, Object> req = cadastro(f, cpf, dataEfetiva);
        req.put("confirmarInclusaoExcedente", true);
        req.put("justificativaExcedente", "Substituição extraordinária formalmente aprovada");
        req.put("evidenciaExcedente", "SEI-PROVA-EVENTO-EXCEDENTE");
        req.put("autorizacaoExcedente", "ATO-PROVA-EVENTO-EXCEDENTE");
        Instant antes = Instant.now().minusSeconds(1);
        JsonNode criado = enviar(f.c().localA, ROTA, req, 201,
                "CMP009 Gestor Local pode confirmar excedente com documentação completa");
        Instant depois = Instant.now().plusSeconds(1);
        long vinculoId = criado.path("id").asLong();
        JsonNode eventos = consultar(f.c().localA, ROTA + "/" + vinculoId + "/eventos",
                "CMP009 leitura nova mostra a autorização fotografada no primeiro evento");
        assertThat(eventos).hasSize(1);
        JsonNode evento = eventos.get(0);
        assertThat(evento.path("tipo").asText()).isEqualTo("CRIACAO");
        assertThat(evento.path("responsavelId").asLong()).isEqualTo(f.c().localA.getId());
        assertThat(evento.path("responsavelUsername").asText()).isEqualTo(f.c().localA.getUsername());
        assertThat(evento.path("dataEfetiva").asText()).isEqualTo(dataEfetiva.toString());
        assertThat(Instant.parse(evento.path("registradoEm").asText())).isBetween(antes, depois);
        assertThat(evento.path("justificativa").asText()).contains((String) req.get("justificativaExcedente"));
        assertThat(evento.path("evidencia").asText()).isEqualTo(req.get("evidenciaExcedente"));
        assertThat(evento.path("autorizacao").asText()).isEqualTo(req.get("autorizacaoExcedente"));
        assertThat(evento.path("inclusaoExcedente").asBoolean()).isTrue();
        assertThat(evento.path("composicaoExcedenteId").asLong()).isEqualTo(f.contrato().composicao1());
        assertThat(evento.path("lotacaoExcedenteId").asLong()).isEqualTo(f.c().lotacaoA.getId());
        assertThat(evento.path("jornadaExcedenteId").asLong()).isEqualTo(f.c().jornadaA.getId());
        assertThat(evento.path("cargoExcedenteId").asLong()).isEqualTo(f.c().cargoA.getId());
        assertThat(evento.path("efetivoPrevistoExcedente").asInt()).isEqualTo(1);
        assertThat(evento.path("ocupacaoAposInclusaoExcedente").asLong()).isEqualTo(2);
        assertThat(evento.path("hashEvento").asText()).hasSize(64);
        emTransacao(() -> {
            EventoVinculoContratual persistido = em.find(EventoVinculoContratual.class, evento.path("id").asLong());
            assertThat(persistido.getVinculo().getId()).isEqualTo(vinculoId);
            assertThat(persistido.getResponsavelId()).isEqualTo(f.c().localA.getId());
            assertThat(persistido.getResponsavelUsername()).isEqualTo(f.c().localA.getUsername());
            assertThat(persistido.getDataEfetiva()).isEqualTo(dataEfetiva);
            assertThat(persistido.getRegistradoEm()).isEqualTo(Instant.parse(evento.path("registradoEm").asText()));
            assertThat(persistido.getJustificativa()).isEqualTo(evento.path("justificativa").asText());
            assertThat(persistido.getEvidencia()).isEqualTo(req.get("evidenciaExcedente"));
            assertThat(persistido.getAutorizacao()).isEqualTo(req.get("autorizacaoExcedente"));
            assertThat(persistido.isInclusaoExcedente()).isTrue();
            assertThat(persistido.getEfetivoPrevistoExcedente()).isEqualTo(1);
            assertThat(persistido.getOcupacaoAposInclusaoExcedente()).isEqualTo(2L);
            assertThat(persistido.getHashEvento()).isEqualTo(evento.path("hashEvento").asText());
            return null;
        });
    }

    private Preparado preparar() {
        return emTransacao(() -> {
            try {
                Cenario c = criarCenario();
                return new Preparado(c, publicado(c, 1, null, false));
            } catch (Exception e) { throw new IllegalStateException(e); }
        });
    }

    private void ocupar(Preparado f) throws Exception {
        Map<String, Object> req = cadastro(f, cpf(Math.toIntExact(746000000L + f.contrato().id())),
                RelogioSistema.hoje().minusDays(1));
        req.put("matricula", "CMP-OCUPANTE-" + f.contrato().id());
        enviar(f.c().localA, ROTA, req, 201, "Fixture ocupa a única vaga prevista na composição publicada");
    }

    private Map<String, Object> cadastro(Preparado f, String cpf, LocalDate inicio) {
        return mapa("pessoa", mapa("nome", "Pessoa prova CMP " + cpf, "cpf", cpf),
                "contratoId", f.contrato().id(), "composicaoId", f.contrato().composicao1(),
                "dataInicio", inicio, "matricula", "CMP-PROPOSTA-" + f.contrato().id());
    }

    private List<Long> contagens(Preparado f, String cpf) {
        return emTransacao(() -> List.of(
                em.createQuery("select count(p) from Pessoa p where p.cpf=:cpf", Long.class).setParameter("cpf", cpf).getSingleResult(),
                em.createQuery("select count(e) from Empregado e where e.cpf=:cpf", Long.class).setParameter("cpf", cpf).getSingleResult(),
                em.createQuery("select count(v) from VinculoContratual v where v.contrato.id=:c", Long.class)
                        .setParameter("c", f.contrato().id()).getSingleResult(),
                em.createQuery("select count(a) from AlocacaoVinculo a where a.vinculo.contrato.id=:c", Long.class)
                        .setParameter("c", f.contrato().id()).getSingleResult(),
                em.createQuery("select count(e) from EventoVinculoContratual e where e.vinculo.contrato.id=:c", Long.class)
                        .setParameter("c", f.contrato().id()).getSingleResult(),
                em.createQuery("select count(e) from EventoAlocacaoVinculo e where e.vinculo.contrato.id=:c", Long.class)
                        .setParameter("c", f.contrato().id()).getSingleResult(),
                em.createQuery("select count(r) from ReservaMatriculaContrato r where r.contrato.id=:c", Long.class)
                        .setParameter("c", f.contrato().id()).getSingleResult()));
    }

    private record Preparado(Cenario c, NovoContrato contrato) {}
}
