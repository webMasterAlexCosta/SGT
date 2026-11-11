package com.example.siggtrat.auditoria20261029;

import com.example.siggtrat.entidade.Empregado;
import com.example.siggtrat.entidade.HistoricoEmpregado;
import com.example.siggtrat.utilitario.RelogioSistema;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mockito;
import org.springframework.http.MediaType;

import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;

/** A escrita por Pessoa conserva o escopo temporal da prova assinada e a projeção de leitura. */
class EmpregadoHistoricoEscopoAtualHttpIT extends ContratosHttpBase {
    @ParameterizedTest
    @ValueSource(strings = {"transferencia", "recontratacao"})
    void novoHistoricoOpcionalSegueGestaoAtualSemReescreverNemExporHistoriaPosteriorNaOrigem(String mudanca) throws Exception {
        Preparado f = preparar(mudanca);
        String origem = "RG-ANTERIOR-ORIGEM-" + f.empregadoId();
        ObjectNode original = (ObjectNode) consultar(f.c().localA, "/api/empregados/" + f.empregadoId(),
                "Preparar opcional anterior à mudança de Gestão");
        original.put("rg", origem);
        dados(executar(f.c().localA, put("/api/pessoas/" + f.pessoaId() + "/dados-opcionais")
                        .contentType(MediaType.APPLICATION_JSON).content(json(original)), 200,
                "Histórico original pertence à Gestão A antes da mudança"));
        em.flush(); em.clear();
        List<HistoricoEmpregado> anteriores = historicos(f.empregadoId());
        assertThat(anteriores).isNotEmpty();
        Map<Long, String> hashesAnteriores = anteriores.stream().collect(Collectors.toMap(HistoricoEmpregado::getId,
                HistoricoEmpregado::getHashIntegridade, (a, b) -> a, LinkedHashMap::new));
        anteriores.forEach(h -> assertThat(h.getGestaoLocalSnapshotId()).isEqualTo(f.c().gestaoA.getId()));

        try (var relogio = Mockito.mockStatic(RelogioSistema.class, Mockito.CALLS_REAL_METHODS)) {
            relogio.when(RelogioSistema::hoje).thenReturn(f.data());
            String destino = "RG-NOVO-RESTRITO-DESTINO-" + f.empregadoId();
            ObjectNode atual = (ObjectNode) consultar(f.c().localB, "/api/empregados/" + f.empregadoId(),
                    "B consulta contexto canônico efetivo");
            assertThat(atual.path("gestaoLocalId").asLong()).isEqualTo(f.c().gestaoB.getId());
            atual.put("rg", destino);
            dados(executar(f.c().localB, put("/api/pessoas/" + f.pessoaId() + "/dados-opcionais")
                            .contentType(MediaType.APPLICATION_JSON).content(json(atual)), 200,
                    "Gestão B registra novo opcional sem alterar vínculo/alocação"));
            em.flush(); em.clear();

            String consulta = "/api/historicos-empregados/empregado/" + f.empregadoId();
            JsonNode visivelA = consultar(f.c().localA, consulta,
                    "A conserva somente sua história anterior, sem narrativa pessoal nova de B");
            JsonNode visivelB = consultar(f.c().localB, consulta,
                    "B recebe o histórico opcional que acabou de registrar em seu escopo");
            List<HistoricoEmpregado> todos = historicos(f.empregadoId());
            List<HistoricoEmpregado> novos = todos.stream().filter(h -> !hashesAnteriores.containsKey(h.getId())).toList();
            JsonNode integridade = consultar(f.c().admin, consulta + "/integridade",
                    "A mudança de escopo não rompe a cadeia assinada por Empregado");

            assertAll(
                    () -> assertThat(visivelA.toString()).contains(origem).doesNotContain(destino),
                    () -> assertThat(visivelB.toString()).contains(destino),
                    () -> {
                        assertThat(novos).isNotEmpty();
                        novos.forEach(h -> {
                            assertThat(h.getGestaoLocalSnapshotId()).isEqualTo(f.c().gestaoB.getId());
                            assertThat(h.getSuperintendenciaSnapshotId()).isEqualTo(f.c().seB.getId());
                        });
                    },
                    () -> todos.stream().filter(h -> hashesAnteriores.containsKey(h.getId())).forEach(h -> {
                        assertThat(h.getHashIntegridade()).isEqualTo(hashesAnteriores.get(h.getId()));
                        assertThat(h.getGestaoLocalSnapshotId()).isEqualTo(f.c().gestaoA.getId());
                    }),
                    () -> assertThat(integridade.path("verificavel").asBoolean()).isTrue(),
                    () -> assertThat(integridade.path("integro").asBoolean()).isTrue(),
                    () -> assertThat(em.find(Empregado.class, f.empregadoId()).getGestaoLocal().getId())
                            .isEqualTo(f.c().gestaoA.getId())
            );
        }
    }

    private List<HistoricoEmpregado> historicos(long empregadoId) {
        return em.createQuery("select h from HistoricoEmpregado h where h.empregado.id=:id order by h.id", HistoricoEmpregado.class)
                .setParameter("id", empregadoId).getResultList();
    }

    private Preparado preparar(String mudanca) throws Exception {
        Cenario c = criarCenario(); NovoContrato contrato = publicado(c, 100, null, false);
        LocalDate data = RelogioSistema.hoje().plusDays(5);
        var cadastro = mapa("pessoa", mapa("nome", "Pessoa histórico em Gestão efetiva " + mudanca,
                        "cpf", cpf(Math.toIntExact(989000000L + contrato.id()))),
                "contratoId", contrato.id(), "composicaoId", contrato.composicao1(), "dataInicio", RelogioSistema.hoje());
        if (mudanca.equals("recontratacao")) cadastro.put("dataFim", data.minusDays(1));
        JsonNode v = enviar(c.localA, "/api/vinculos-contratuais", cadastro, 201,
                "Pessoa canônica inicialmente registrada na origem");
        long empregadoId = em.createQuery("select e.id from Empregado e where e.pessoaCanonica.id=:p", Long.class)
                .setParameter("p", v.path("pessoaId").asLong()).getSingleResult();
        if (mudanca.equals("transferencia")) {
            conceder(c, contrato.id(), "TRANSFERENCIA_GESTAO_LOCAL");
            enviar(c.localA, base(contrato.id()) + "/transferir-gestao",
                    mapa(/* Conferência SINTÉTICA explícita desta fixture; não é padrão do produto. */
                "conferenciaDocumentalConfirmada", true, "versaoConferenciaDocumental", "transferencia-gestao-conferencia-v1", "dataEfetiva", data, "dataAto", RelogioSistema.hoje(), "gestaoLocalDestinoId", c.gestaoB.getId(), "tipoContratacaoDestinoId", c.tipoB.getId(),
                            "motivo", "Transferência com histórico pessoal anterior preservado", "referenciaSei", "SEI-HISTORICO-ESCOPO",
                            "autorizacaoAdministrativa", "ATO-HTTP",
                            "mapeamentos", List.of(mapa("composicaoOrigemId", contrato.composicao1(),
                                    "lotacaoDestinoId", c.lotacaoB.getId(), "jornadaDestinoId", c.jornadaB.getId(), "cargoDestinoId", c.cargoB.getId()))),
                    200, "Programar mudança de Gestão sem reescrever projeção antiga");
        } else {
            enviar(c.localB, "/api/vinculos-contratuais", mapa("pessoaId", v.path("pessoaId").asLong(),
                            "contratoId", c.contratoB.getId(), "composicaoId", c.composicaoB.getId(), "dataInicio", data),
                    201, "Recontratação usa a mesma Pessoa em novo contexto sem sobreposição");
        }
        em.flush(); em.clear();
        return new Preparado(c, empregadoId, v.path("pessoaId").asLong(), data);
    }

    private record Preparado(Cenario c, long empregadoId, long pessoaId, LocalDate data) {}
}
