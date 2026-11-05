package com.example.siggtrat.auditoria20261029;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

@Transactional(propagation = Propagation.NOT_SUPPORTED)
class PessoasMatriculaConcorrenciaHttpIT extends ContratosHttpBase {
    @Test
    @Timeout(value = 45, unit = TimeUnit.SECONDS)
    void cpfsDistintosDisputamMesmaMatriculaSemGravacaoParcialDaTentativaPerdedora() throws Exception {
        Cenario c = emTransacao(this::criarCenario);
        List<String> cpfs = List.of(cpf(Math.toIntExact(748000000L + c.contratoA.getId() * 2L)),
                cpf(Math.toIntExact(748000001L + c.contratoA.getId() * 2L)));
        String matricula = "MAT-CONCORRENTE-" + c.contratoA.getId();
        consultar(c.localA, "/api/vinculos-contratuais/" + c.vinculoA.getId(), "Preparar sessão antes da disputa concorrente");
        var reqA = autenticar(post("/api/vinculos-contratuais").contentType(MediaType.APPLICATION_JSON)
                .content(json(cadastro(c, cpfs.get(0), matricula))), c.localA);
        var reqB = autenticar(post("/api/vinculos-contratuais").contentType(MediaType.APPLICATION_JSON)
                .content(json(cadastro(c, cpfs.get(1), "  " + matricula.toLowerCase(java.util.Locale.ROOT) + "  "))), c.localA);
        CountDownLatch largada = new CountDownLatch(1);
        ExecutorService pool = Executors.newFixedThreadPool(2);
        MvcResult a;
        MvcResult b;
        try {
            Future<MvcResult> fa = pool.submit(() -> { largada.await(); return mvc.perform(reqA).andReturn(); });
            Future<MvcResult> fb = pool.submit(() -> { largada.await(); return mvc.perform(reqB).andReturn(); });
            largada.countDown();
            a = fa.get(35, TimeUnit.SECONDS); b = fb.get(35, TimeUnit.SECONDS);
        } finally { largada.countDown(); pool.shutdownNow(); }
        assertThat(List.of(a.getResponse().getStatus(), b.getResponse().getStatus()))
                .as("A=%s B=%s", a.getResponse().getContentAsString(), b.getResponse().getContentAsString())
                .containsExactlyInAnyOrder(201, 409);
        MvcResult vencedora = a.getResponse().getStatus() == 201 ? a : b;
        JsonNode criado = dados(vencedora);
        long pessoaId = criado.path("pessoaId").asLong(), vinculoId = criado.path("id").asLong();
        assertThat(criado.path("matricula").asText()).isEqualTo(matricula);
        emTransacao(() -> {
            assertThat(em.createQuery("select p.id from Pessoa p where p.cpf in :cpfs", Long.class)
                    .setParameter("cpfs", cpfs).getResultList()).containsExactly(pessoaId);
            assertThat(em.createQuery("select e.pessoaCanonica.id from Empregado e where e.cpf in :cpfs", Long.class)
                    .setParameter("cpfs", cpfs).getResultList()).containsExactly(pessoaId);
            assertThat(em.createQuery("select v.id from VinculoContratual v where v.pessoa.cpf in :cpfs", Long.class)
                    .setParameter("cpfs", cpfs).getResultList()).containsExactly(vinculoId);
            assertThat(em.createQuery("select a.vinculo.id from AlocacaoVinculo a where a.vinculo.pessoa.cpf in :cpfs", Long.class)
                    .setParameter("cpfs", cpfs).getResultList()).containsExactly(vinculoId);
            assertThat(em.createQuery("select r.vinculo.id from ReservaMatriculaContrato r where r.contrato.id=:c and r.matricula=:m", Long.class)
                    .setParameter("c", c.contratoA.getId()).setParameter("m", matricula).getResultList()).containsExactly(vinculoId);
            assertThat(em.createQuery("select e.vinculo.id from EventoVinculoContratual e where e.vinculo.pessoa.cpf in :cpfs", Long.class)
                    .setParameter("cpfs", cpfs).getResultList()).containsExactly(vinculoId);
            return null;
        });
    }

    private Map<String, Object> cadastro(Cenario c, String cpf, String matricula) {
        return mapa("pessoa", mapa("nome", "Pessoa concorrência de matrícula " + cpf, "cpf", cpf),
                "contratoId", c.contratoA.getId(), "composicaoId", c.composicaoA.getId(),
                "dataInicio", DATA, "matricula", matricula);
    }
}
