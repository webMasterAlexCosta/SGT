package com.example.siggtrat.auditoria20261029;

import com.fasterxml.jackson.databind.JsonNode;
import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

import static org.assertj.core.api.Assertions.assertThat;

/** Prova proposta de SQL real: contratos fora do filtro não multiplicam consultas da página de uma Pessoa. */
class EmpregadoConsultaFiltradaEscalaHttpIT extends ContratosHttpBase {
    @ParameterizedTest
    @ValueSource(strings = {"lista", "resumo"})
    void filtroExplicitoNaoReconstruiTodosOsContratosDaGestao(String rota) throws Exception {
        Cenario c = criarCenario();
        String url = rota.equals("lista") ? "/api/empregados?contratoId=" : "/api/empregados/resumo?contratoIds=";
        url += c.contratoA.getId() + "&page=0&size=1";
        consultar(c.localA, url, "Aquecimento de autenticação e consulta antes da medição JDBC");
        em.flush(); em.clear();
        Statistics estatisticas = em.getEntityManagerFactory().unwrap(SessionFactory.class).getStatistics();
        boolean habilitadasAntes = estatisticas.isStatisticsEnabled();
        estatisticas.setStatisticsEnabled(true);
        try {
            estatisticas.clear();
            JsonNode paginaInicial = consultar(c.localA, url, "Página filtrada com um contrato da Gestão");
            long consultasIniciais = estatisticas.getPrepareStatementCount();
            assertThat(paginaInicial.path("content").size()).isEqualTo(1);
            assertThat(paginaInicial.path("content").get(0).path("id").asLong()).isEqualTo(c.empregadoA.getId());
            assertThat(paginaInicial.path("totalElements").asLong()).isEqualTo(1);

            for (int i = 0; i < 16; i++) contrato(c.gestaoA, c.tipoA, "ESCALA-" + c.gestaoA.getId() + "-" + rota + "-" + i);
            em.flush(); em.clear();
            estatisticas.clear();
            JsonNode paginaComOutrosContratos = consultar(c.localA, url,
                    "A mesma página filtrada após acrescentar 16 contratos alheios ao filtro");
            long consultasComOutrosContratos = estatisticas.getPrepareStatementCount();
            assertThat(paginaComOutrosContratos.path("content").size()).isEqualTo(1);
            assertThat(paginaComOutrosContratos.path("content").get(0).path("id").asLong()).isEqualTo(c.empregadoA.getId());
            assertThat(paginaComOutrosContratos.path("totalElements").asLong()).isEqualTo(1);

            Path evidencia = Path.of("target/auditoria-http/empregado-escala-consultas.jsonl");
            Files.createDirectories(evidencia.getParent());
            Files.writeString(evidencia, json(mapa("rota", rota, "size", 1, "contratosAdicionais", 16,
                            "consultasIniciais", consultasIniciais, "consultasComOutrosContratos", consultasComOutrosContratos)) + "\n",
                    StandardOpenOption.CREATE, StandardOpenOption.APPEND);
            assertThat(consultasIniciais).isPositive();
            assertThat(consultasComOutrosContratos)
                    .as("Filtro explícito deve limitar a reconstrução ao contrato pedido; contratos alheios não acrescentam SQL por candidato")
                    .isLessThanOrEqualTo(consultasIniciais + 2);

            JsonNode foraDoEscopo = consultar(c.localB, url, "Filtro por ID não contorna a autorização temporal do contrato");
            assertThat(foraDoEscopo.path("content").size()).isZero();
            assertThat(foraDoEscopo.path("totalElements").asLong()).isZero();
        } finally {
            estatisticas.setStatisticsEnabled(habilitadasAntes);
        }
    }
}
