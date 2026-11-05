package com.example.siggtrat.auditoria20261029;

import com.example.siggtrat.entidade.*;
import com.example.siggtrat.utilitario.RelogioSistema;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mockito;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

/** Controles de COL013/MAT001/TMP004/PER010 para o adaptador de consulta de Empregado. */
class EmpregadoContextoCanonicoControlesHttpIT extends ContratosHttpBase {
    @Test
    void recontratacaoMantemIdentidadeEHistoricoSemAnteciparContextoFuturo() throws Exception {
        Cenario c = criarCenario(); NovoContrato primeiro = publicado(c, 100, null, false);
        LocalDate hoje = RelogioSistema.hoje(), novoInicio = hoje.plusDays(4);
        JsonNode inicial = enviar(c.localA, "/api/vinculos-contratuais",
                mapa("pessoa", mapa("nome", "Pessoa recontratada sem duplicar Empregado", "cpf", cpf(Math.toIntExact(960000000L + primeiro.id()))),
                        "contratoId", primeiro.id(), "composicaoId", primeiro.composicao1(),
                        "dataInicio", hoje, "dataFim", novoInicio.minusDays(1)), 201,
                "COL013 vínculo original tem fim humano explícito");
        long pessoaId = inicial.path("pessoaId").asLong(), vinculoOriginal = inicial.path("id").asLong();
        long empregadoId = em.createQuery("select e.id from Empregado e where e.pessoaCanonica.id=:p", Long.class)
                .setParameter("p", pessoaId).getSingleResult();
        enviar(c.localB, "/api/vinculos-contratuais", mapa("pessoaId", pessoaId,
                        "contratoId", c.contratoB.getId(), "composicaoId", c.composicaoB.getId(), "dataInicio", novoInicio),
                201, "COL013 mesma Pessoa, novo vínculo em outra Gestão sem sobreposição");
        em.flush(); em.clear();
        assertThat(em.createQuery("select count(e) from Empregado e where e.pessoaCanonica.id=:p", Long.class)
                .setParameter("p", pessoaId).getSingleResult()).isEqualTo(1);
        assertThat(consultar(c.localA, "/api/empregados/" + empregadoId, "Programação não antecipa contexto atual")
                .path("gestaoLocalId").asLong()).isEqualTo(c.gestaoA.getId());
        executar(c.localB, get("/api/empregados/" + empregadoId), 404, "Destino futuro não recebe o cadastro corrente antecipadamente");
        try (var relogio = Mockito.mockStatic(RelogioSistema.class, Mockito.CALLS_REAL_METHODS)) {
            relogio.when(RelogioSistema::hoje).thenReturn(novoInicio);
            JsonNode atual = consultar(c.localB, "/api/empregados/" + empregadoId, "Novo vínculo passa a governar o mesmo Empregado");
            assertThat(atual.path("gestaoLocalId").asLong()).isEqualTo(c.gestaoB.getId());
            assertThat(atual.path("lotacaoId").asLong()).isEqualTo(c.lotacaoB.getId());
            assertThat(atual.path("contratos").get(0).path("id").asLong()).isEqualTo(c.contratoB.getId());
            assertThat(atual.path("dataAdmissao").asText()).isEqualTo(novoInicio.toString());
            executar(c.localA, get("/api/empregados/" + empregadoId), 404, "Projeção antiga não concede acesso corrente depois da recontratação");
            assertThat(ids(consultar(c.localA, "/api/empregados?contratoId=" + primeiro.id(), "Lista anterior não conserva o contexto corrente")))
                    .doesNotContain(empregadoId);
            assertThat(ids(consultar(c.localB, "/api/empregados?contratoId=" + c.contratoB.getId(), "Lista atual preserva a identidade")))
                    .contains(empregadoId);
            JsonNode historico = consultar(c.localA, "/api/vinculos-contratuais/" + vinculoOriginal
                    + "/alocacoes/posicao?data=" + hoje, "TMP004 o vínculo original continua consultável em sua data");
            assertThat(historico.path("lotacaoId").asLong()).isEqualTo(c.lotacaoA.getId());
            assertThat(em.find(Empregado.class, empregadoId).getGestaoLocal().getId()).isEqualTo(c.gestaoA.getId());
        }
    }

    @Test
    void empregadoLegadoSemPessoaCanonicaPreservaEscopoEFiltrosAnteriores() throws Exception {
        Cenario c = criarCenario();
        Empregado legado = new Empregado(); legado.setNome("Legado sem identidade canônica " + c.gestaoA.getId());
        legado.setCpf(cpf(Math.toIntExact(970000000L + c.gestaoA.getId()))); legado.setGestaoLocal(c.gestaoA);
        legado.setLotacao(c.lotacaoA); legado.setJornada(c.jornadaA); legado.setCargo(c.cargoA);
        legado.setStatus(c.empregadoA.getStatus()); legado.setDataAdmissao(RelogioSistema.hoje().minusDays(1));
        legado.setContratos(new ArrayList<>(List.of(c.contratoA))); em.persist(legado); em.flush();
        HistoricoVinculoContrato historico = new HistoricoVinculoContrato();
        historico.setEmpregado(legado); historico.setContrato(c.contratoA); historico.setLotacao(c.lotacaoA);
        historico.setJornada(c.jornadaA); historico.setCargo(c.cargoA);
        historico.setDataInicio(legado.getDataAdmissao().atStartOfDay(ZoneId.of("America/Sao_Paulo")));
        em.persist(historico); em.flush();
        JsonNode detalhe = consultar(c.localA, "/api/empregados/" + legado.getId(), "Legado sem canônico mantém gestão e contexto próprios");
        assertThat(detalhe.path("gestaoLocalId").asLong()).isEqualTo(c.gestaoA.getId());
        assertThat(detalhe.path("lotacaoId").asLong()).isEqualTo(c.lotacaoA.getId());
        executar(c.localB, get("/api/empregados/" + legado.getId()), 404, "Legado não amplia escopo entre Gestões");
        executar(c.operacionalA, get("/api/empregados/" + legado.getId()), 403,
                "A rota de cadastro completo continua indisponível ao Operacional");
        assertThat(ids(consultar(c.operacionalA, "/api/empregados/filtrados-frequencia?dataRef=" + RelogioSistema.hoje()
                        + "&contratoId=" + c.contratoA.getId() + "&lotacaoId=" + c.lotacaoA.getId()
                        + "&turnoId=" + c.jornadaA.getId() + "&searchTerm=" + legado.getCpf(),
                "Recorte operacional legado permanece válido na grade autorizada"))).containsExactly(legado.getId());
        String filtro = "?contratoId=" + c.contratoA.getId() + "&lotacaoId=" + c.lotacaoA.getId() + "&search=" + legado.getCpf();
        assertThat(ids(consultar(c.localA, "/api/empregados" + filtro, "Filtros anteriores preservados para legado")))
                .containsExactly(legado.getId());
        assertThat(ids(consultar(c.localA, "/api/empregados/resumo?contratoIds=" + c.contratoA.getId() + "&search=" + legado.getCpf(),
                "Resumo preserva cadastro sem Pessoa"))).containsExactly(legado.getId());
        assertThat(em.find(Empregado.class, legado.getId()).getPessoaCanonica()).isNull();
    }

    @ParameterizedTest
    @ValueSource(strings = {"lotacao", "turno"})
    void contextoCanonicoNaoAmpliaRecorteOperacionalMesmoComProjecaoPermitida(String dimensao) throws Exception {
        Cenario c = criarCenario(); Lotacao lotacao = c.lotacaoA; Jornada jornada = c.jornadaA;
        if (dimensao.equals("lotacao")) {
            lotacao = new Lotacao("Lotação canônica fora do recorte " + c.gestaoA.getId());
            lotacao.setGestaoLocal(c.gestaoA); lotacao.setMunicipio("MUNICIPIO"); lotacao.getTurnos().add(jornada); em.persist(lotacao);
        } else {
            jornada = new Jornada(); jornada.setDescricao("Turno canônico fora do recorte " + c.gestaoA.getId()); jornada.setGestaoLocal(c.gestaoA);
            for (DayOfWeek dia : DayOfWeek.values()) {
                JornadaDiaSemana d = new JornadaDiaSemana(); d.setJornada(jornada); d.setDiaSemana(dia); d.setDiaTrabalho(dia.getValue() <= 5);
                if (dia.getValue() <= 5) { d.setEntrada1("09:00"); d.setSaida1("17:00"); } jornada.getDias().add(d);
            }
            em.persist(jornada); lotacao.getTurnos().add(jornada);
        }
        ComposicaoContratual composicao = composicao(c.contratoA, lotacao, jornada, c.cargoA, 5);
        Pessoa pessoa = pessoa("Pessoa fora do recorte canônico " + dimensao, cpf(Math.toIntExact(980000000L + c.gestaoA.getId())));
        VinculoContratual vinculo = vinculo(pessoa, c.contratoA); AlocacaoVinculo alocacao = alocacao(vinculo, composicao);
        Empregado empregado = projecao.projetar(vinculo, alocacao);
        empregado.setLotacao(c.lotacaoA); empregado.setJornada(c.jornadaA); em.flush();
        executar(c.operacionalA, get("/api/empregados/" + empregado.getId()), 403,
                "A rota de cadastro completo continua indisponível ao Operacional");
        JsonNode grade = consultar(c.operacionalA, "/api/empregados/filtrados-frequencia?dataRef=" + RelogioSistema.hoje()
                        + "&contratoId=" + c.contratoA.getId() + "&lotacaoId=" + c.lotacaoA.getId()
                        + "&turnoId=" + c.jornadaA.getId(), "PER010 filtro canônico aplicado antes da paginação na grade autorizada");
        assertThat(ids(grade)).containsExactly(c.empregadoA.getId()).doesNotContain(empregado.getId());
        assertThat(grade.path("totalElements").asLong()).isEqualTo(1);
        executar(c.operacionalA, get("/api/empregados/filtrados-frequencia?dataRef=" + RelogioSistema.hoje()
                        + "&contratoId=" + c.contratoA.getId() + "&lotacaoId=" + lotacao.getId()
                        + "&turnoId=" + jornada.getId()), 403, "Solicitação explícita fora do recorte operacional continua bloqueada");
        JsonNode dto = consultar(c.localA, "/api/empregados/" + empregado.getId(), "Gestor mantém consulta da própria Gestão");
        assertThat(dto.path("lotacaoId").asLong()).isEqualTo(lotacao.getId());
        assertThat(dto.path("jornadaId").asLong()).isEqualTo(jornada.getId());
    }

    private List<Long> ids(JsonNode pagina) {
        List<Long> ids = new ArrayList<>(); pagina.path("content").forEach(x -> ids.add(x.path("id").asLong())); return ids;
    }
}
