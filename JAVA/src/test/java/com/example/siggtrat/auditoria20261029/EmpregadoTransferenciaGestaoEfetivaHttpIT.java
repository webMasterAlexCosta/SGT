package com.example.siggtrat.auditoria20261029;

import com.example.siggtrat.utilitario.RelogioSistema;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mockito;
import org.springframework.http.MediaType;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;

/** Variante CT007: consultas gerais de Empregado ainda usam sua projeção de Gestão/Alocação. */
class EmpregadoTransferenciaGestaoEfetivaHttpIT extends ContratosHttpBase {
    @Test
    void pessoaAposTransferenciaAceitaAtualizacaoOpcionalPelaGestaoEfetiva() throws Exception {
        Preparado f = preparar();
        try (var relogio = Mockito.mockStatic(RelogioSistema.class, Mockito.CALLS_REAL_METHODS)) {
            relogio.when(RelogioSistema::hoje).thenReturn(f.data());
            ObjectNode dto = (ObjectNode) consultar(f.c().localB, "/api/empregados/" + f.empregadoId(),
                    "GET fornece contexto canônico para edição opcional");
            dto.put("rg", "RG-PUT-SEM-TROCAR-CONTEXTO");
            JsonNode salvo = dados(executar(f.c().localB, put("/api/pessoas/" + f.pessoaId() + "/dados-opcionais")
                    .contentType(MediaType.APPLICATION_JSON).content(json(dto)), 200,
                    "COL011 GET→PUT só de opcional compara contexto atual, sem exigir projeção antiga"));
            assertThat(salvo.path("rg").asText()).isEqualTo("RG-PUT-SEM-TROCAR-CONTEXTO");
            JsonNode projecao = consultar(f.c().localB, "/api/empregados/" + f.empregadoId(), "A projeção continua no contexto efetivo");
            assertThat(projecao.path("lotacaoId").asLong()).isEqualTo(f.c().lotacaoB.getId());
            assertThat(projecao.path("gestaoLocalId").asLong()).isEqualTo(f.c().gestaoB.getId());
        }
    }

    @Test
    void camposTemporaisExtrasNaPessoaNaoAlteramAlocacaoOuContrato() throws Exception {
        Preparado f = preparar();
        try (var relogio = Mockito.mockStatic(RelogioSistema.class, Mockito.CALLS_REAL_METHODS)) {
            relogio.when(RelogioSistema::hoje).thenReturn(f.data());
            ObjectNode original = (ObjectNode) consultar(f.c().localB, "/api/empregados/" + f.empregadoId(),
                    "Contexto atual de referência para controles de escrita legada");
            ObjectNode trocaLotacao = original.deepCopy(); trocaLotacao.put("lotacaoId", f.c().lotacaoA.getId());
            executar(f.c().localB, put("/api/pessoas/" + f.pessoaId() + "/dados-opcionais").contentType(MediaType.APPLICATION_JSON)
                    .content(json(trocaLotacao)), 200, "ALO002 propriedade extra não é um comando de Alocação");
            ObjectNode trocaContrato = original.deepCopy();
            trocaContrato.set("contratos", mapper.valueToTree(List.of(mapa("id", f.c().contratoB.getId()))));
            executar(f.c().localB, put("/api/pessoas/" + f.pessoaId() + "/dados-opcionais").contentType(MediaType.APPLICATION_JSON)
                    .content(json(trocaContrato)), 200, "VIN003 propriedade extra não cria nem troca Vínculo");
            JsonNode depois = consultar(f.c().localB, "/api/empregados/" + f.empregadoId(), "Contexto protegido pelo modelo de escrita canônico");
            assertThat(depois.path("lotacaoId")).isEqualTo(original.path("lotacaoId"));
            assertThat(depois.path("contratos")).isEqualTo(original.path("contratos"));
        }
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void detalheLegadoSegueGestaoEAlocacaoCanonicasAtuais(boolean origem) throws Exception {
        Preparado f = preparar();
        var usuario = origem ? f.c().localA : f.c().localB;
        executar(usuario, get("/api/empregados/" + f.empregadoId()), origem ? 200 : 404,
                "Antes da transferência, acesso do Empregado permanece na origem");
        try (var relogio = Mockito.mockStatic(RelogioSistema.class, Mockito.CALLS_REAL_METHODS)) {
            relogio.when(RelogioSistema::hoje).thenReturn(f.data());
            JsonNode dto = dados(executar(usuario, get("/api/empregados/" + f.empregadoId()), origem ? 404 : 200,
                    "PER001 HIS005 detalhe legado usa autorização/contexto canônicos após transferência efetiva"));
            if (!origem) {
                assertThat(dto.path("gestaoLocalId").asLong()).isEqualTo(f.c().gestaoB.getId());
                assertThat(dto.path("lotacaoId").asLong()).isEqualTo(f.c().lotacaoB.getId());
                assertThat(dto.path("jornadaId").asLong()).isEqualTo(f.c().jornadaB.getId());
            }
        }
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void listaGeralLegadaUsaMesmoEscopoDoDetalheAposTransferencia(boolean origem) throws Exception {
        Preparado f = preparar();
        var usuario = origem ? f.c().localA : f.c().localB;
        try (var relogio = Mockito.mockStatic(RelogioSistema.class, Mockito.CALLS_REAL_METHODS)) {
            relogio.when(RelogioSistema::hoje).thenReturn(f.data());
            JsonNode pagina = consultar(usuario, "/api/empregados?contratoId=" + f.contrato().id(),
                    "PER001 lista e contagem legadas usam responsabilidade efetiva, sem depender de scheduler");
            List<Long> ids = new ArrayList<>(); pagina.path("content").forEach(x -> ids.add(x.path("id").asLong()));
            if (origem) assertThat(ids).doesNotContain(f.empregadoId());
            else {
                assertThat(ids).contains(f.empregadoId());
                assertThat(pagina.path("totalElements").asLong()).isEqualTo(1);
                assertThat(pagina.path("content").get(0).path("gestaoLocalId").asLong()).isEqualTo(f.c().gestaoB.getId());
            }
            JsonNode resumo = consultar(usuario, "/api/empregados/resumo?contratoIds=" + f.contrato().id(),
                    "A listagem resumida aplica o mesmo contexto antes de paginar");
            List<Long> idsResumo = new ArrayList<>(); resumo.path("content").forEach(x -> idsResumo.add(x.path("id").asLong()));
            if (origem) assertThat(idsResumo).doesNotContain(f.empregadoId());
            else {
                assertThat(idsResumo).contains(f.empregadoId());
                assertThat(resumo.path("totalElements").asLong()).isEqualTo(1);
                assertThat(resumo.path("content").get(0).path("lotacaoId").asLong()).isEqualTo(f.c().lotacaoB.getId());
                assertThat(resumo.path("content").get(0).path("jornadaId").asLong()).isEqualTo(f.c().jornadaB.getId());
            }
        }
    }

    private Preparado preparar() throws Exception {
        Cenario c = criarCenario(); NovoContrato contrato = publicado(c, 100, null, false);
        JsonNode v = enviar(c.localA, "/api/vinculos-contratuais",
                mapa("pessoa", mapa("nome", "Pessoa com projeção legada transferida", "cpf", cpf(Math.toIntExact(950000000L + contrato.id()))),
                        "contratoId", contrato.id(), "composicaoId", contrato.composicao1(), "dataInicio", RelogioSistema.hoje()),
                201, "Pessoa canônica cria projeção Empregado na origem");
        long empregadoId = em.createQuery("select e.id from Empregado e where e.pessoaCanonica.id=:p", Long.class)
                .setParameter("p", v.path("pessoaId").asLong()).getSingleResult();
        LocalDate data = RelogioSistema.hoje().plusDays(5);
        conceder(c, contrato.id(), "TRANSFERENCIA_GESTAO_LOCAL");
        enviar(c.localA, base(contrato.id()) + "/transferir-gestao",
                mapa("dataEfetiva", data, "dataAto", RelogioSistema.hoje(), "gestaoLocalDestinoId", c.gestaoB.getId(), "tipoContratacaoDestinoId", c.tipoB.getId(),
                        "motivo", "Transferência futura de Pessoa com projeção legada", "referenciaSei", "SEI-TRANSFERENCIA-LEGADO",
                        "autorizacaoAdministrativa", "ATO-HTTP",
                        "mapeamentos", List.of(mapa("composicaoOrigemId", contrato.composicao1(),
                                "lotacaoDestinoId", c.lotacaoB.getId(), "jornadaDestinoId", c.jornadaB.getId(), "cargoDestinoId", c.cargoB.getId()))),
                200, "Transferência futura não deve antecipar gestão materializada do Empregado");
        em.flush(); em.clear(); return new Preparado(c, contrato, empregadoId, v.path("pessoaId").asLong(), data);
    }

    private record Preparado(Cenario c, NovoContrato contrato, long empregadoId, long pessoaId, LocalDate data) {}
}
