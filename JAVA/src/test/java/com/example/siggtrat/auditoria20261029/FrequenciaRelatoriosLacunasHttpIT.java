package com.example.siggtrat.auditoria20261029;

import com.example.siggtrat.dto.FrequenciaDTO;
import com.example.siggtrat.entidade.*;
import com.example.siggtrat.servico.FrequenciaServico;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;

/** Regressões de resultados negociais derivados do levantamento, com HTTP e banco reais. */
class FrequenciaRelatoriosLacunasHttpIT extends AuditoriaHttpSuporte {
    @Autowired FrequenciaServico frequencias;

    @ParameterizedTest(name = "Presidência consulta {0}")
    @ValueSource(strings = {
            "/filtros?inicio=2026-06-10&fim=2026-06-10",
            "/frequencia-consolidada?inicio=2026-06-10&fim=2026-06-10",
            "/frequencia-consolidada/paginada?inicio=2026-06-10&fim=2026-06-10",
            "/terceirizados/consolidado?",
            "/absenteismo?mes=6&ano=2026",
            "/absenteismo/paginado?mes=6&ano=2026",
            "/absenteismo/resumo?mes=6&ano=2026",
            "/horas-trabalhadas?mes=6&ano=2026"
    })
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    void presidentePossuiConsultaNacionalEmTodosOsRelatorios(String rota) throws Exception {
        Cenario c = criarCenarioPersistido();
        criarFrequencia(c, DATA, "PRESENCA", "perfil-" + UUID.randomUUID());
        String parametro = rota.contains("frequencia-consolidada") || rota.startsWith("/filtros")
                ? "contratoIds" : "contratoId";
        JsonNode resposta = dados(executar(c.presidente,
                get("/api/relatorios" + rota + "&" + parametro + "=" + c.contratoA.getId()),
                200, "PER-004: Presidente consulta relatório nacional " + rota));
        assertTrue(resposta.isContainerNode(), "A consulta deve entregar contrato JSON de relatório.");
    }

    @Test
    void excecaoDeOutraGestaoNaoPodeSerCapturadaTrocandoEmpregado() throws Exception {
        Cenario c = criarCenario();
        JsonNode criada = dados(executar(c.localB,
                post("/api/frequencias-excecoes").contentType(APPLICATION_JSON).content(json(Map.of(
                        "empregadoId", c.empregadoB.getId(), "dataInicio", DATA.toString(),
                        "dataFim", DATA.toString(), "tipo", "FOLGA_REMUNERADA"))),
                201, "Exceção válida na Gestão B"));
        Long id = criada.path("id").longValue();

        executar(c.localA, put("/api/frequencias-excecoes/{id}", id)
                        .contentType(APPLICATION_JSON).content(json(Map.of("empregadoId", c.empregadoA.getId()))),
                403, "PER-001: origem alheia não pode ser capturada por substituição do empregado");

        em.flush();
        em.clear();
        FrequenciaExcecao preservada = em.find(FrequenciaExcecao.class, id);
        assertEquals(c.empregadoB.getId(), preservada.getEmpregado().getId());
        assertEquals(DATA, preservada.getDataInicio());
    }

    @Test
    void cadeiaDeFrequenciaContinuaIntegraAposRoundTripNoMySqlENormalizacaoDoMotivo() throws Exception {
        Cenario c = criarCenario();
        JsonNode criada = dados(executar(c.localA, post("/api/frequencias")
                        .contentType(APPLICATION_JSON).content(json(Map.of(
                                "vinculoContratualId", c.vinculoA.getId(), "dataReferencia", DATA.toString(),
                                "status", "FALTA", "justificativaRetificacao", "  Motivo   documentado  ",
                                "chaveIdempotencia", "hash-banco-" + UUID.randomUUID()))),
                200, "Registro com motivo normalizável"));
        long id = criada.path("id").longValue();
        em.flush(); em.clear();

        JsonNode eventos = dados(executar(c.localA, get("/api/frequencias/{id}/eventos", id),
                200, "FRE-004: hash permanece válido depois de persistir/ler o timestamp e normalizar motivo"));
        assertEquals(1, eventos.size());
        assertEquals("Motivo documentado", eventos.get(0).path("motivo").asText());
        assertEquals(64, eventos.get(0).path("hashEvento").asText().length());
    }

    @Test
    void semLancamentoPreservaCargaSemanalDaAlocacaoAposEdicaoDoCadastro() throws Exception {
        Cenario c = criarCenario();
        JsonNode antes = relatorioDia(c);
        assertEquals(480, antes.path("minutosPrevistos").intValue());
        assertEquals("SEM_LANCAMENTO", antes.path("status").asText());

        JornadaDiaSemana dia = c.jornadaA.getDias().stream()
                .filter(d -> d.getDiaSemana() == DATA.getDayOfWeek()).findFirst().orElseThrow();
        dia.setEntrada1("08:00"); dia.setSaida1("10:00");
        dia.setEntrada2(null); dia.setSaida2(null);
        em.flush();

        JsonNode depois = relatorioDia(c);
        assertEquals(480, depois.path("minutosPrevistos").intValue(),
                "TMP-004/REL-001: editar o turno mestre não reduz a carga histórica de 8h para 2h.");
        assertEquals("SEM_LANCAMENTO", depois.path("status").asText());
    }

    @Test
    void semLancamentoPreservaMunicipioHistoricoParaFeriado() throws Exception {
        Cenario c = criarCenario();
        assertNotNull(c.lotacaoA.getMunicipio(), "A fixture deve fotografar um município antes da alocação.");
        CalendarioEvento feriado = new CalendarioEvento();
        feriado.setData(DATA); feriado.setTipo("FERIADO"); feriado.setDescricao("Feriado municipal histórico");
        feriado.setAbrangencia("MUNICIPAL"); feriado.setMunicipio(c.lotacaoA.getMunicipio());
        feriado.setGestaoLocal(c.gestaoA); feriado.setSuperintendencia(c.seA); feriado.setAtivo(true);
        em.persist(feriado); em.flush();
        assertEquals("FERIADO", relatorioDia(c).path("status").asText());

        c.lotacaoA.setMunicipio("Município alterado depois do fato"); em.flush();

        JsonNode depois = relatorioDia(c);
        assertEquals("FERIADO", depois.path("status").asText(),
                "TMP-004: município atual não remove feriado da alocação histórica.");
        assertEquals(0, depois.path("minutosPrevistos").intValue());
    }

    @Test
    void lancamentoRetroativoUsaHorarioFotografadoNaAlocacao() throws Exception {
        Cenario c = criarCenario();
        JornadaDiaSemana dia = c.jornadaA.getDias().stream()
                .filter(d -> d.getDiaSemana() == DATA.getDayOfWeek()).findFirst().orElseThrow();
        dia.setEntrada1("08:00"); dia.setSaida1("10:00");
        dia.setEntrada2(null); dia.setSaida2(null); em.flush();

        JsonNode criada = dados(executar(c.localA, post("/api/frequencias")
                        .contentType(APPLICATION_JSON).content(json(Map.of(
                                "vinculoContratualId", c.vinculoA.getId(), "dataReferencia", DATA.toString(),
                                "status", "PRESENCA", "justificativaRetificacao", "Registro retroativo documentado",
                                "chaveIdempotencia", "retro-horario-" + UUID.randomUUID()))),
                200, "FRE-001: presença retroativa sem batidas usa jornada histórica"));
        assertEquals("08:00", criada.path("horasTrabalhadas").asText());
        assertEquals(480, relatorioDia(c).path("minutosPrevistos").intValue());
    }

    @Test
    void horasTrabalhadasContinuamVisiveisQuandoProjecaoAtualMudouDeContrato() throws Exception {
        Cenario c = criarCenario();
        criarFrequencia(c, DATA, "PRESENCA", "historico-horas-" + UUID.randomUUID());
        c.empregadoA.getContratos().clear();
        c.empregadoA.getContratos().add(c.contratoB);
        c.empregadoA.setLotacao(c.lotacaoB); c.empregadoA.setJornada(c.jornadaB);
        c.empregadoA.setCargo(c.cargoB); c.empregadoA.setGestaoLocal(c.gestaoB);
        em.flush();

        JsonNode linhas = dados(executar(c.localA, get("/api/relatorios/horas-trabalhadas")
                        .param("mes", "6").param("ano", "2026").param("contratoId", c.contratoA.getId().toString()),
                200, "REL-001/FRE-015: horas da Gestão A usam vínculo e frequência históricos"));
        assertEquals(1, linhas.size(), "Transferência da projeção atual não apaga o trabalho anterior.");
        assertEquals(c.empregadoA.getId().longValue(), linhas.get(0).path("empregadoId").longValue());
        assertEquals("08:00", linhas.get(0).path("totalHorasTrabalhadas").asText());
        assertEquals(1, linhas.get(0).path("totalPresencas").intValue());
        assertEquals(c.lotacaoA.getNome(), linhas.get(0).path("lotacao").asText());
    }

    @Test
    void ocorrenciaSobreFrequenciaFechadaGeraVersaoFormalEPreservaEvidencia() throws Exception {
        Cenario c = criarCenario();
        JsonNode original = criarFrequencia(c, DATA, "PRESENCA", "antes-ocorrencia-" + UUID.randomUUID());
        long frequenciaId = original.path("id").longValue();
        int versao = original.path("versaoNegocial").intValue();
        int quantidadeEventos = dados(executar(c.localA,
                get("/api/frequencias/{id}/eventos", frequenciaId), 200, "Cadeia anterior à ocorrência")).size();

        executar(c.localA, multipart("/api/ocorrencias")
                        .param("empregadoId", c.empregadoA.getId().toString()).param("motivo", "ATESTADO")
                        .param("dataOcorrencia", DATA.toString()).param("dias", "1")
                        .param("observacao", "Atestado documentado no processo da ocorrência"),
                201, "Ocorrência gerencial corrige classificação da frequência fechada");

        JsonNode eventos = dados(executar(c.localA, get("/api/frequencias/{id}/eventos", frequenciaId),
                200, "FRE-005/FRE-014: ocorrência deve gerar evento e versão formal"));
        assertEquals(quantidadeEventos + 1, eventos.size());
        em.flush();
        Frequencia alterada = em.find(Frequencia.class, frequenciaId);
        assertEquals(versao + 1, alterada.getVersaoNegocial());
        assertEquals("ATESTADO", alterada.getStatus());
        assertEquals("08:00", alterada.getEntrada1(), "Evidência de batida original é preservada.");
        assertTrue(alterada.getBloqueadaEdicaoOperacional());
        assertEquals(c.localA.getId(), alterada.getAutorOriginalId());
    }

    @Test
    void blocoDoFechamentoPreservaJustificativaEIdempotenciaGerenciais() {
        Cenario c = criarCenario();
        FechamentoFrequencia trabalho = new FechamentoFrequencia();
        trabalho.setChaveEscopo(UUID.randomUUID().toString()); trabalho.setDataReferencia(DATA);
        trabalho.setLotacao(c.lotacaoA); trabalho.setJornada(c.jornadaA);
        trabalho.setUsuarioCriador(c.localA); trabalho.setStatus(FechamentoFrequenciaStatus.PROCESSANDO);
        trabalho.setTotalPrevisto(1); em.persist(trabalho); em.flush();
        FrequenciaDTO alteracao = new FrequenciaDTO();
        alteracao.setEmpregado(new FrequenciaDTO.EmpregadoResumoDTO(c.empregadoA.getId(), c.empregadoA.getNome()));
        alteracao.setDataReferencia(DATA); alteracao.setStatus("PRESENCA");
        alteracao.setJustificativaRetificacao("Conferência documentada da frequência em lote");
        alteracao.setChaveIdempotencia("fechamento-gerencial-" + UUID.randomUUID());

        var resultado = assertDoesNotThrow(() -> frequencias.processarBlocoFechamento(trabalho.getId(),
                List.of(c.empregadoA.getId()), DATA, Map.of(c.empregadoA.getId(), alteracao), c.localA.getId()),
                "O worker deve transportar justificativa/chave recebidas, não descartá-las.");
        assertEquals(1, resultado.inseridos());
        assertEquals(1, em.createQuery("select count(f) from Frequencia f where f.vinculoContratual.id = :v", Long.class)
                .setParameter("v", c.vinculoA.getId()).getSingleResult());
    }

    private JsonNode criarFrequencia(Cenario c, LocalDate data, String status, String chave) throws Exception {
        return dados(executar(c.localA, post("/api/frequencias").contentType(APPLICATION_JSON).content(json(Map.of(
                        "vinculoContratualId", c.vinculoA.getId(), "dataReferencia", data.toString(),
                        "status", status, "entrada1", "08:00", "saida1", "12:00", "entrada2", "13:00", "saida2", "17:00",
                        "justificativaRetificacao", "Conferência documentada para auditoria", "chaveIdempotencia", chave))),
                200, "FRE-001/FRE-005: lançamento canônico e retificação gerencial"));
    }

    private JsonNode relatorioDia(Cenario c) throws Exception {
        JsonNode resposta = dados(executar(c.localA, get("/api/relatorios/frequencia-consolidada")
                        .param("inicio", DATA.toString()).param("fim", DATA.toString())
                        .param("contratoIds", c.contratoA.getId().toString()),
                200, "REL-001: consulta de jornada histórica com alocação canônica"));
        assertEquals(1, resposta.path("registros").size());
        return resposta.path("registros").get(0).path("diario").path(DATA.toString());
    }
}
