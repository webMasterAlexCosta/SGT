package com.example.siggtrat.auditoria20261029;

import com.example.siggtrat.entidade.AlocacaoVinculo;
import com.example.siggtrat.entidade.VinculoContratual;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@Transactional(propagation = Propagation.NOT_SUPPORTED)
class PessoasCriteriosTemporaisComplementaresHttpIT extends ContratosHttpBase {
    @Test
    void retificarInicioRevalidaMatriculaNaPoliticaDaNovaData() throws Exception {
        Preparado f = emTransacao(() -> {
            try {
                Cenario c = criarCenario(); c.tipoA.setMatriculaVinculoObrigatoria(true); em.flush();
                NovoContrato n = publicado(c, 5, null, false);
                Map<String, Object> dispensa = aditamento("ADI-DISPENSA-CRITERIO-INICIO", n.inicio().plusDays(5));
                dispensa.put("novaMatriculaVinculoObrigatoria", false); registrarAditamento(c, n.id(), dispensa);
                return new Preparado(c, n);
            } catch (Exception e) { throw new IllegalStateException(e); }
        });
        LocalDate inicio = f.contrato().inicio().plusDays(6), anteriorObrigatorio = f.contrato().inicio().plusDays(1);
        JsonNode criado = enviar(f.c().localA, "/api/vinculos-contratuais",
                mapa("pessoa", mapa("nome", "Pessoa com matrícula dispensada por ADI",
                                "cpf", cpf(Math.toIntExact(742000000L + f.contrato().id()))),
                        "contratoId", f.contrato().id(), "composicaoId", f.contrato().composicao1(), "dataInicio", inicio),
                201, "MAT005 início após ADI admite matrícula ausente");
        long vinculoId = criado.path("id").asLong();
        assertThat(criado.path("matricula").isNull()).isTrue();
        Map<String, Object> req = mapa("novoInicio", anteriorObrigatorio, "justificativa", "Correção material da data inicial",
                "evidencia", "SEI-POLITICA-INICIO", "autorizacao", "ATO-POLITICA-INICIO");
        String rota = "/api/vinculos-contratuais/" + vinculoId;
        JsonNode previa = enviar(f.c().localA, rota + "/previas/retificacao-inicio", req, 200,
                "VIN014 prévia de antecipação para data com matrícula obrigatória");
        req.put("confirmarImpacto", true); req.put("tokenPrevia", previa.path("token").asText());
        enviar(f.c().localA, rota + "/retificar-inicio", req, 400,
                "MAT005 VIN014 confirmação revalida obrigação na nova data e não usa política opcional posterior");
        emTransacao(() -> {
            assertThat(em.find(VinculoContratual.class, vinculoId).getDataInicio()).isEqualTo(inicio);
            assertThat(em.find(VinculoContratual.class, vinculoId).getMatricula()).isNull();
            assertThat(em.createQuery("select a from AlocacaoVinculo a where a.vinculo.id=:v", AlocacaoVinculo.class)
                    .setParameter("v", vinculoId).getSingleResult().getDataInicio()).isEqualTo(inicio);
            assertThat(em.createQuery("select count(e) from EventoVinculoContratual e where e.vinculo.id=:v", Long.class)
                    .setParameter("v", vinculoId).getSingleResult()).isEqualTo(1);
            return null;
        });
    }

    private record Preparado(Cenario c, NovoContrato contrato) {}
}
