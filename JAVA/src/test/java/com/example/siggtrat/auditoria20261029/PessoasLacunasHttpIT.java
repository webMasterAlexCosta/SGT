package com.example.siggtrat.auditoria20261029;

import com.example.siggtrat.entidade.*;
import com.example.siggtrat.dominio.vinculo.CategoriaTerminoVinculo;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;

/** Regressões de requisitos derivadas do levantamento, pela API e MySQL reais. */
class PessoasLacunasHttpIT extends AuditoriaHttpSuporte {
    @org.springframework.beans.factory.annotation.Autowired
    private com.example.siggtrat.servico.GestaoContratoTemporalServico gestaoDaFonte;


    @Test
    void pesquisaVinculosIniciadosNaoRetornaVinculoAnteriorAindaAberto() throws Exception {
        var c = criarCenario();
        var desde = c.vinculoA.getDataInicio().plusDays(1);
        JsonNode resposta = dados(executar(c.localA, get("/api/vinculos-contratuais")
                .param("pessoaId", c.pessoaA.getId().toString())
                .param("inicioDe", desde.toString()).param("inicioAte", desde.plusDays(10).toString()),
                200, "HIS-003/VIN-002 filtro de início usa data efetiva de início"));
        assertThat(resposta.path("content")).isEmpty();
        assertThat(resposta.path("totalElements").asLong()).isZero();
    }

    @Test
    void pesquisaDesligadosNaoRetornaVinculoAindaAberto() throws Exception {
        var c = criarCenario();
        JsonNode resposta = dados(executar(c.localA, get("/api/vinculos-contratuais")
                .param("pessoaId", c.pessoaA.getId().toString())
                .param("fimDe", DATA.minusDays(30).toString()).param("fimAte", DATA.toString()),
                200, "HIS-003 pesquisa por término efetivo exclui vínculo aberto"));
        assertThat(resposta.path("content")).isEmpty();
        assertThat(resposta.path("totalElements").asLong()).isZero();
    }

    @Test
    void alocacoesHistoricasNaoSaoRenomeadasPelosCatalogosAtuais() throws Exception {
        var c = criarCenario();
        String lotacaoOriginal = c.lotacaoA.getNome();
        String jornadaOriginal = c.jornadaA.getDescricao();
        String cargoOriginal = c.cargoA.getNome();
        c.lotacaoA.setNome("Lotação renomeada depois do fato");
        c.jornadaA.setDescricao("Turno renomeado depois do fato");
        c.cargoA.setNome("Cargo renomeado depois do fato");
        em.flush();
        JsonNode faixas = dados(executar(c.localA, get("/api/vinculos-contratuais/{id}/alocacoes", c.vinculoA.getId()),
                200, "CAT-004/HIS-004 nomes históricos da faixa permanecem imutáveis"));
        JsonNode faixa = elementoComId(faixas, c.alocacaoA.getId());
        assertThat(faixa.path("lotacaoNome").asText()).isEqualTo(lotacaoOriginal);
        assertThat(faixa.path("jornadaDescricao").asText()).isEqualTo(jornadaOriginal);
        assertThat(faixa.path("cargoNome").asText()).isEqualTo(cargoOriginal);
    }

    @Test
    void motivoAplicadoAoDesligamentoSobreviveARenomeacaoDoCatalogo() throws Exception {
        var c = criarCenario();
        declararFonteNominalSintetica(c, "DES-MOTIVO-PRESERVADO");
        MotivoDesligamento motivo = motivo(c, "Pedido individual original");
        var fim = DATA.minusDays(1);
        FonteMotivoSintetica.declarar(em, gestaoDaFonte, c.contratoA.getId(), motivo.getId(), c.gestaoA.getId(), c.localA,
                "Pedido individual original", CategoriaTerminoVinculo.DESLIGAMENTO_INDIVIDUAL,
                fim, fim, Instant.now(), "SEI-SINTETICO-DES004-CONTEXTO-MOTIVO");
        executar(c.localA, post("/api/vinculos-contratuais/{id}/desligar", c.vinculoA.getId())
                .contentType(MediaType.APPLICATION_JSON).content(json(desligamento(fim, motivo.getId()))),
                200, "DES-001/004 desligamento registra causa de catálogo");
        motivo.setDescricao("Descrição alterada posteriormente");
        motivo.setCategoria(CategoriaTerminoVinculo.OUTRO);
        em.flush();
        JsonNode vinculo = dados(executar(c.localA, get("/api/vinculos-contratuais/{id}", c.vinculoA.getId()),
                200, "CAT-004/DES-004 histórico preserva motivo efetivamente aplicado"));
        assertThat(vinculo.path("motivoDesligamentoDescricao").asText()).isEqualTo("Pedido individual original");
        JsonNode eventos = dados(executar(c.localA, get("/api/vinculos-contratuais/{id}/eventos", c.vinculoA.getId()),
                200, "DES-004 evento conserva motivo e categoria aplicados"));
        JsonNode evento = null;
        for (JsonNode candidato : eventos) if ("DESLIGAMENTO".equals(candidato.path("tipo").asText())) evento = candidato;
        assertThat(evento).isNotNull();
        assertThat(evento.path("motivoDesligamentoIdFotografia").asLong()).isEqualTo(motivo.getId());
        assertThat(evento.path("motivoDesligamentoDescricaoFotografia").asText()).isEqualTo("Pedido individual original");
        assertThat(evento.path("motivoDesligamentoCategoriaFotografia").asText()).isEqualTo("DESLIGAMENTO_INDIVIDUAL");
    }

    @Test
    void novaFrequenciaAtingidaInvalidaPreviaDeRetificacaoDoInicio() throws Exception {
        var c = criarCenario();
        var inicioOriginal = c.vinculoA.getDataInicio();
        Map<String,Object> body = new LinkedHashMap<>();
        body.put("novoInicio", inicioOriginal.plusDays(1));
        body.put("justificativa", "Correção material do início");
        body.put("evidencia", "SEI-INICIO");
        body.put("autorizacao", "Gestor Local");
        JsonNode previa = dados(executar(c.localA, post("/api/vinculos-contratuais/{id}/previas/retificacao-inicio", c.vinculoA.getId())
                .contentType(MediaType.APPLICATION_JSON).content(json(body)), 200,
                "VIN-014 prévia antes de frequência atingida"));
        Frequencia frequencia = new Frequencia();
        frequencia.setEmpregado(c.empregadoA);
        frequencia.setPessoa(c.pessoaA);
        frequencia.setVinculoContratual(c.vinculoA);
        frequencia.setAlocacaoVinculo(c.alocacaoA);
        frequencia.setDataReferencia(inicioOriginal);
        frequencia.setStatus("FALTA");
        frequencia.setContratoSnapshotId(c.contratoA.getId());
        frequencia.setGestaoLocalSnapshotId(c.gestaoA.getId());
        frequencia.setSuperintendenciaSnapshotId(c.seA.getId());
        frequencia.setLotacaoSnapshotId(c.lotacaoA.getId());
        frequencia.setJornadaSnapshotId(c.jornadaA.getId());
        frequencia.setCargoSnapshotId(c.cargoA.getId());
        em.persist(frequencia);
        em.flush();
        body.put("confirmarImpacto", true);
        body.put("tokenPrevia", previa.path("token").asText());
        executar(c.localA, post("/api/vinculos-contratuais/{id}/retificar-inicio", c.vinculoA.getId())
                .contentType(MediaType.APPLICATION_JSON).content(json(body)), 409,
                "VIN-014/AUD-004 impacto aumentou: prévia anterior deve ser rejeitada");
        assertThat(c.vinculoA.getDataInicio()).isEqualTo(inicioOriginal);
    }

    @Test
    void corteAnteriorAoCadastroNaoRevelaVinculoDaProjecaoAtual() throws Exception {
        var c = criarCenario();
        JsonNode posicao = dados(executar(c.localA, get("/api/vinculos-contratuais/pessoas/{id}/posicao", c.pessoaA.getId())
                .param("data", DATA.toString()).param("conhecidoAte", Instant.EPOCH.toString()), 200,
                "HIS-005 corte de conhecimento anterior ao cadastro não inventa vínculo conhecido"));
        assertThat(posicao.path("vinculos")).isEmpty();
    }

    @Test
    void corrigirCpfLiberaIdentificadorAntigoSemReatribuirProjecaoDaPessoaAnterior() throws Exception {
        var c = criarCenario();
        String cpfAntigo = c.pessoaA.getCpf();
        String cpfCorrigido = "39053344705";
        executar(c.localA, post("/api/pessoas/{id}/correcoes-identidade", c.pessoaA.getId())
                .contentType(MediaType.APPLICATION_JSON).content(json(Map.of(
                        "tipo", "CPF", "novoValor", cpfCorrigido, "motivo", "Erro material comprovado",
                        "evidencia", "SEI-CPF", "autorizacao", "Gestor Local", "impactosEncontrados", "Vínculo e projeção operacional"))),
                200, "COL-006 correção excepcional do CPF");
        em.flush();
        Long empregadoOriginal = c.empregadoA.getId();
        JsonNode novoVinculo = dados(executar(c.localA, post("/api/vinculos-contratuais")
                .contentType(MediaType.APPLICATION_JSON).content(json(Map.of(
                        "pessoa", Map.of("nome", "Titular real do CPF liberado", "cpf", cpfAntigo),
                        "contratoId", c.contratoA.getId(), "dataInicio", DATA,
                        "composicaoId", c.composicaoA.getId(), "matricula", "CPF-LIBERADO"))),
                201, "COL-005/013 CPF antigo liberado para titular real"));
        em.flush();
        Long novaPessoa = novoVinculo.path("pessoaId").asLong();
        assertThat(novaPessoa).isNotEqualTo(c.pessoaA.getId());
        Empregado anterior = em.find(Empregado.class, empregadoOriginal);
        assertThat(anterior.getPessoaCanonica().getId()).isEqualTo(c.pessoaA.getId());
        assertThat(anterior.getCpf()).isEqualTo(cpfCorrigido);
        assertThat(em.createQuery("select e.id from Empregado e where e.pessoaCanonica.id=:p", Long.class)
                .setParameter("p", novaPessoa).getResultList()).doesNotContain(empregadoOriginal).hasSize(1);
    }

    @Test
    void fluxoOpcionalDePessoaNaoPodeTrocarIdentidadeCanonica() throws Exception {
        var c = criarCenario();
        ObjectNode dto = (ObjectNode) dados(executar(c.localA, get("/api/empregados/{id}", c.empregadoA.getId()),
                200, "COL-009 consulta da projeção legada"));
        String nomeOriginal = c.empregadoA.getNome();
        dto.put("nome", "Identidade substituída por fluxo comum");
        executar(c.localA, put("/api/pessoas/{id}/dados-opcionais", c.pessoaA.getId())
                .contentType(MediaType.APPLICATION_JSON).content(json(dto)), 200,
                "COL-009 propriedades extras não alteram identidade pela rota opcional");
        em.flush();
        assertThat(c.empregadoA.getNome()).isEqualTo(nomeOriginal);
        assertThat(c.pessoaA.getNome()).isEqualTo(nomeOriginal);
    }

    private MotivoDesligamento motivo(Cenario c, String descricao) {
        MotivoDesligamento motivo = new MotivoDesligamento();
        motivo.setGestaoLocal(c.gestaoA);
        motivo.setDescricao(descricao);
        motivo.setCategoria(CategoriaTerminoVinculo.DESLIGAMENTO_INDIVIDUAL);
        motivo.setAtivo(true);
        motivo.setCriadoPorId(c.localA.getId());
        motivo.setCriadoPorUsername(c.localA.getUsername());
        em.persist(motivo);
        em.flush();
        // Fonte sintética explícita do motivo desde INICIO; criadoEm/ativo atuais
        // não constituem, isoladamente, prova de disponibilidade retroativa.
        var origem = new com.example.siggtrat.entidade.EventoDisponibilidadeCatalogo();
        origem.setTipoCatalogo(com.example.siggtrat.dominio.catalogo.TipoCatalogoTemporal.MOTIVO_DESLIGAMENTO);
        origem.setCatalogoId(motivo.getId()); origem.setDataEfetiva(INICIO); origem.setAtivo(true);
        origem.setMotivo("Fonte sintética explícita do Motivo usado no fato histórico deste teste");
        origem.setResponsavelId(c.localA.getId()); origem.setResponsavelUsername(c.localA.getUsername());
        em.persist(origem); em.flush();
        return motivo;
    }

    private Map<String,Object> desligamento(java.time.LocalDate fim, Long motivo) {
        return Map.of("dataFim", fim, "motivoDesligamentoId", motivo,
                "justificativa", "Desligamento individual documentado", "evidencia", "SEI-DES", "autorizacao", "Gestor Local");
    }

    private JsonNode elementoComId(JsonNode lista, Long id) {
        for (JsonNode item : lista) if (item.path("id").asLong() == id) return item;
        throw new AssertionError("Registro histórico não retornado: " + id);
    }
}
