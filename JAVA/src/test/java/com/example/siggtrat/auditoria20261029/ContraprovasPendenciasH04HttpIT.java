package com.example.siggtrat.auditoria20261029;

import com.example.siggtrat.entidade.Contrato;
import com.example.siggtrat.entidade.EventoDisponibilidadeCatalogo;
import com.example.siggtrat.entidade.Jornada;
import com.example.siggtrat.entidade.PendenciaAdministrativa;
import com.example.siggtrat.servico.GestaoContratoTemporalServico;
import com.example.siggtrat.utilitario.RelogioSistema;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;

/** AUD-006:002/004. Atos pela API, commit real e SELECT posterior; sem relógio simulado. */
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class ContraprovasPendenciasH04HttpIT extends ContratosHttpBase {
    @Autowired JdbcTemplate jdbc;
    @Autowired GestaoContratoTemporalServico gestaoTemporal;
    @Autowired com.example.siggtrat.servico.FilaRegistroAuditoria filaAuditoria;

    @Test
    void pendenciaDeRevisaoRecusaDiaAfetadoMasPermiteEscritaOperacionalForaDoRecorte() throws Exception {
        var c = criarCenarioPersistido();
        declararFonteNominalSintetica(c, "H04-AUD006-002");
        var dias = diasUteisSemEventoNaJanela(c);
        assertThat(dias).as("Dois dias úteis reais, sem calendário especial, dentro de D+7").hasSizeGreaterThanOrEqualTo(2);
        LocalDate d1 = dias.get(0), d2 = dias.get(1);
        long fonte = enviar(c.localA, "/api/frequencias-excecoes", mapa(
                "empregadoId", c.empregadoA.getId(), "dataInicio", d1, "dataFim", d1,
                "tipo", "FOLGA_REMUNERADA", "observacao", "Fonte sintética delimitada somente a D1"),
                201, "Preparação legítima: fonte de folga em um único dia").path("id").asLong();
        var inicial1 = enviar(c.operacionalA, "/api/frequencias", pedido(c, d1, "FOLGA"), 200,
                "O mesmo Operacional cria a frequência que depois ficará sujeita a revisão");
        var inicial2 = enviar(c.operacionalA, "/api/frequencias", pedido(c, d2, "FALTA"), 200,
                "Controle positivo originalmente editável, sem intervenção gerencial");
        long f1 = inicial1.path("id").asLong(), f2 = inicial2.path("id").asLong();
        assertThat(f1).isPositive(); assertThat(f2).isPositive().isNotEqualTo(f1);
        for (var dto : List.of(inicial1, inicial2)) {
            assertThat(dto.path("valida").isMissingNode()).isTrue(); // não confundir campo inexistente com validade
            assertThat(dto.path("validadeOperacional").asText()).isEqualTo("VALIDA");
            assertThat(dto.path("bloqueadaEdicaoOperacional").asBoolean()).isFalse();
            assertThat(dto.path("estadoNegocial").asText()).isEqualTo("ABERTA");
        }
        var antesRetirada = fotografiaFrequencias(c.contratoA.getId());
        executar(c.localA, delete("/api/frequencias-excecoes/{id}", fonte)
                        .param("justificativa", "Retirada documentada da fonte sintética exclusiva de D1"),
                204, "A API retira a fonte e produz a revisão, sem alteração artificial da frequência");
        var revisao = fotografiaFrequencias(c.contratoA.getId());
        var raiz1 = linhaId(revisao.path("frequencias"), f1, "id");
        var raiz2 = linhaId(revisao.path("frequencias"), f2, "id");
        assertThat(raiz1.path("status").asText()).isEqualTo("FOLGA");
        assertThat(raiz1.path("estado_auditoria").asText()).isEqualTo("SUJEITA_REVISAO");
        assertThat(raiz1.path("validade_operacional").asText()).isNotEqualTo("VALIDA");
        assertThat(raiz1.path("bloqueada_edicao_operacional").asText()).isEqualTo("t");
        assertThat(raiz2.path("validade_operacional").asText()).isEqualTo("VALIDA");
        assertThat(raiz2.path("bloqueada_edicao_operacional").asText()).isEqualTo("f");
        assertThat(raiz2).isEqualTo(linhaId(antesRetirada.path("frequencias"), f2, "id"));
        assertThat(revisao.path("pendencias")).hasSize(1);
        var pendencia = revisao.path("pendencias").get(0);
        assertThat(pendencia.path("frequencia_id").asLong()).isEqualTo(f1);
        assertThat(pendencia.path("estado").asText()).isEqualTo("ABERTA");
        assertThat(pendencia.path("data_efetiva").asText()).isEqualTo(d1.toString());
        assertThat(pendencia.path("gestao_local_id").asLong()).isEqualTo(c.gestaoA.getId());
        var recusada = executar(c.operacionalA, post("/api/frequencias").contentType(APPLICATION_JSON)
                        .content(json(pedido(c, d1, "FALTA"))), 403,
                "Dia ainda na janela: bloqueio da revisão, não expiração de D+7");
        assertThat(recusada.getResponse().getContentAsString())
                .contains("intervenção do Gestor Local").doesNotContain("FECHADA");
        assertThat(fotografiaFrequencias(c.contratoA.getId())).as("Recusa não altera raiz, eventos nem pendência")
                .isEqualTo(revisao);
        var pedidoControle = pedido(c, d2, "PRESENCA");
        pedidoControle.putAll(mapa("entrada1", "08:00", "saida1", "12:00", "entrada2", "13:00", "saida2", "17:00"));
        var alterada = enviar(c.operacionalA, "/api/frequencias", pedidoControle, 200,
                "Contraprova discriminante: escrita efetiva fora do recorte pelo mesmo Operacional");
        assertThat(alterada.path("id").asLong()).isEqualTo(f2);
        assertThat(alterada.path("status").asText()).isEqualTo("PRESENCA");
        var depois = fotografiaFrequencias(c.contratoA.getId());
        var raiz2Depois = linhaId(depois.path("frequencias"), f2, "id");
        assertThat(raiz2Depois.path("version").asLong()).isGreaterThan(raiz2.path("version").asLong());
        assertThat(raiz2Depois.path("versao_negocial").asInt()).isGreaterThan(raiz2.path("versao_negocial").asInt());
        assertThat(raiz2Depois.path("status").asText()).isEqualTo("PRESENCA");
        assertThat(raiz2Depois.path("data_referencia").asText()).isEqualTo(d2.toString());
        assertThat(raiz2Depois.path("autor_original_id").asLong()).isEqualTo(c.operacionalA.getId());
        assertThat(raiz2Depois.path("gestao_local_snapshot_id").asLong()).isEqualTo(c.gestaoA.getId());
        assertThat(raiz2Depois.path("lotacao_snapshot_id").asLong()).isEqualTo(c.lotacaoA.getId());
        assertThat(raiz2Depois.path("jornada_snapshot_id").asLong()).isEqualTo(c.jornadaA.getId());
        var eventosControle = elementos(depois.path("eventos"), "id_frequencia", f2);
        assertThat(eventosControle).hasSize(elementos(revisao.path("eventos"), "id_frequencia", f2).size() + 1);
        var ultimo = eventosControle.get(eventosControle.size() - 1);
        assertThat(ultimo.path("tipo_evento").asText()).isEqualTo("ALTERACAO_OPERACIONAL");
        assertThat(ultimo.path("usuario_id").asLong()).isEqualTo(c.operacionalA.getId());
        assertThat(ultimo.path("chave_idempotencia").asText()).isEqualTo(pedidoControle.get("chaveIdempotencia"));
        assertThat(ultimo.path("data_referencia").asText()).isEqualTo(d2.toString());
        assertThat(linhaId(depois.path("frequencias"), f1, "id")).isEqualTo(raiz1);
        assertThat(elementos(depois.path("eventos"), "id_frequencia", f1))
                .isEqualTo(elementos(revisao.path("eventos"), "id_frequencia", f1));
        assertThat(depois.path("pendencias")).isEqualTo(revisao.path("pendencias"));
        assertThat(ultimo.path("snapshot_anterior").asText()).contains("status=FALTA");
        assertThat(ultimo.path("snapshot_novo").asText()).contains("status=PRESENCA");
        var integridade1 = consultar(c.localA, "/api/frequencias/" + f1 + "/evidencias-integridade", "Cadeia afetada preservada");
        var integridade2 = consultar(c.localA, "/api/frequencias/" + f2 + "/evidencias-integridade", "Cadeia da escrita de controle íntegra");
        assertThat(integridade1.path("cadeiaIntegra").asBoolean()).isTrue();
        assertThat(integridade2.path("cadeiaIntegra").asBoolean()).isTrue();
        registrar("AUD-006-002", mapa("hojeReal", RelogioSistema.hoje(), "d1", d1, "d2", d2,
                "operacionalId", c.operacionalA.getId(), "antesRetirada", antesRetirada,
                "aposRetirada", revisao, "aposEscritaControle", depois, "respostaControle", alterada,
                "integridadeAfetada", integridade1, "integridadeControle", integridade2));
    }

    @Test
    void conflitoDeReplayPertenceAGestaoEfetivaDoAtoMesmoComGestaoAtualDiferente() throws Exception {
        var c = criarCenarioPersistido();
        var n = emTransacao(() -> {
            try { return publicado(c, 10, 1, true); }
            catch (Exception e) { throw new IllegalStateException(e); }
        });
        LocalDate d1 = n.inicio().plusDays(2), t = n.inicio().plusDays(3), d2 = n.inicio().plusDays(4);
        var r1 = registrarRemanejamento(c, n, "H04-R1", d1, -4);
        long cargoB2 = emTransacao(() -> {
            var cargo = new com.example.siggtrat.entidade.Cargo();
            cargo.setGestaoLocal(c.gestaoB);
            cargo.setNome("Cargo B secundário H04 " + UUID.randomUUID());
            cargo.setAtivo(true);
            em.persist(cargo); em.flush();
            var origem = new EventoDisponibilidadeCatalogo();
            origem.setTipoCatalogo(com.example.siggtrat.dominio.catalogo.TipoCatalogoTemporal.CARGO);
            origem.setCatalogoId(cargo.getId()); origem.setDataEfetiva(n.inicio()); origem.setAtivo(true);
            origem.setMotivo("Fonte sintética explícita do segundo cargo destino; combinações não convergentes");
            em.persist(origem); em.flush(); return cargo.getId();
        });
        conceder(c, n.id(), "TRANSFERENCIA_GESTAO_LOCAL");
        enviar(c.localA, base(n.id()) + "/transferir-gestao", mapa(/* Conferência SINTÉTICA explícita desta fixture; não é padrão do produto. */
                "conferenciaDocumentalConfirmada", true, "versaoConferenciaDocumental", "transferencia-gestao-conferencia-v1", 
                "dataEfetiva", t, "dataAto", RelogioSistema.hoje(), "gestaoLocalDestinoId", c.gestaoB.getId(),
                "tipoContratacaoDestinoId", c.tipoB.getId(), "motivo", "Transferência futura entre R1 e R2",
                "referenciaSei", "SEI-H04-AUD006-004", "autorizacaoAdministrativa", "ATO-HTTP",
                "mapeamentos", List.of(mapa("composicaoOrigemId", n.composicao1(),
                        "lotacaoDestinoId", c.lotacaoB.getId(), "jornadaDestinoId", c.jornadaB.getId(), "cargoDestinoId", c.cargoB.getId()),
                        mapa("composicaoOrigemId", n.composicao2(), "lotacaoDestinoId", c.lotacaoB.getId(),
                                "jornadaDestinoId", c.jornadaB.getId(), "cargoDestinoId", cargoB2))),
                200, "Transferência legítima com mapa completo não convergente");
        var r2 = registrarRemanejamento(c, n, "H04-R2", d2, 4);
        var contexto = emTransacao(() -> {
            em.clear(); var contrato = em.find(Contrato.class, n.id());
            assertThat(contrato.getGestaoLocal().getId()).isEqualTo(c.gestaoA.getId());
            assertThat(gestaoTemporal.gestaoAtual(contrato).getId()).isEqualTo(c.gestaoA.getId());
            assertThat(gestaoTemporal.gestaoEm(contrato, d2, Instant.now()).getId()).isEqualTo(c.gestaoB.getId());
            assertThat(em.createQuery("select count(v) from VinculoContratual v where v.contrato.id=:c", Long.class)
                    .setParameter("c", n.id()).getSingleResult()).isZero();
            return mapa("gestaoAtualId", contrato.getGestaoLocal().getId(), "gestaoEfetivaD2Id", c.gestaoB.getId());
        });
        var antes = consultar(c.localA, base(n.id()) + "/remanejamentos", "Fotografias originais commitadas de R1 e R2");
        var transferencias = consultar(c.localA, base(n.id()) + "/transferencias-gestao", "Ato de transferência original");
        assertQuantidades(c, n, d2, 1, 9);
        enviar(c.localA, base(n.id()) + "/remanejamentos/" + r1.path("id").asLong() + "/cancelar",
                mapa("dataAto", RelogioSistema.hoje(), "motivo", "Revogação de R1 preservando R2 e transferência",
                        "referenciaSei", "SEI-H04-CANCELAMENTO"), 200, "A operação-alvo torna R2 incompatível, sem fabricar estado");
        var pendencias = fotografiaPendencias(n.id());
        assertThat(pendencias).hasSize(1);
        var p = pendencias.get(0); long pendenciaId = p.path("id").asLong();
        assertThat(p.path("tipo").asText()).isEqualTo("CONFLITO_REPLAY_TEMPORAL");
        assertThat(p.path("estado").asText()).isEqualTo("ABERTA");
        assertThat(p.path("origem_entidade").asText()).isEqualTo("EventoCicloVidaContrato");
        assertThat(p.path("origem_id").asLong()).isEqualTo(r2.path("eventoId").asLong());
        assertThat(p.path("operacao_origem").asText()).isEqualTo("CANCELAMENTO_REMANEJAMENTO");
        assertThat(p.path("data_efetiva").asText()).isEqualTo(d2.toString());
        assertThat(p.path("gestao_local_id").asLong()).isEqualTo(c.gestaoB.getId());
        assertThat(p.path("superintendencia_id").asLong()).isEqualTo(c.seB.getId());
        assertThat(p.path("motivo").asText()).isNotBlank(); assertThat(p.path("surgida_em").asText()).isNotBlank();
        var visivelB = consultar(c.localB, "/api/pendencias?contratoIds=" + n.id(), "Gestão efetiva B recebe o conflito");
        var invisivelA = consultar(c.localA, "/api/pendencias?contratoIds=" + n.id(), "Gestão atual A não recebe o conflito de D2");
        assertThat(elementos(visivelB.path("registros"), "id", pendenciaId)).hasSize(1);
        assertThat(elementos(invisivelA.path("registros"), "id", pendenciaId)).isEmpty();
        filaAuditoria.descarregar();
        var auditoriaAntes = fotografiaAuditoriaTratamento(pendenciaId);
        assertThat(auditoriaAntes).isEmpty();
        enviar(c.localA, "/api/pendencias/" + pendenciaId + "/iniciar-tratamento",
                mapa("observacao", "Tentativa no contexto atual incorreto"), 404, "A não decide tarefa do contexto histórico B");
        assertThat(fotografiaPendencias(n.id())).isEqualTo(pendencias);
        filaAuditoria.descarregar();
        assertThat(fotografiaAuditoriaTratamento(pendenciaId)).isEqualTo(auditoriaAntes);
        var depois = consultar(c.localA, base(n.id()) + "/remanejamentos", "Instrumentos originais são preservados");
        assertInstrumentoPreservado(linhaId(depois, r1.path("id").asLong(), "id"), linhaId(antes, r1.path("id").asLong(), "id"), "CANCELADO");
        assertInstrumentoPreservado(linhaId(depois, r2.path("id").asLong(), "id"), linhaId(antes, r2.path("id").asLong(), "id"), "VIGENTE");
        assertThat(consultar(c.localA, base(n.id()) + "/transferencias-gestao", "Transferência preservada pelo cancelamento de R1"))
                .isEqualTo(transferencias);
        var precedencias = consultar(c.localA, base(n.id()) + "/eventos/precedencias", "Cancelamento aponta somente R1");
        assertThat(precedencias).hasSize(1);
        assertThat(precedencias.get(0).path("tipoRelacao").asText()).isEqualTo("CANCELA");
        assertThat(precedencias.get(0).path("eventoAlvoId").asLong()).isEqualTo(r1.path("eventoId").asLong());
        assertQuantidades(c, n, d2, 1, 9); assertQuantidades(c, n, d2, 1, 9);
        assertThat(fotografiaPendencias(n.id())).isEqualTo(pendencias);
        var tratada = enviar(c.localB, "/api/pendencias/" + pendenciaId + "/iniciar-tratamento",
                mapa("observacao", "Gestão B assume a tarefa de sua data efetiva"), 200, "Controle positivo da autoridade decisória B");
        var aposTratamento = fotografiaPendencias(n.id());
        assertThat(aposTratamento).hasSize(1);
        assertThat(aposTratamento.get(0).path("estado").asText()).isEqualTo("EM_TRATAMENTO");
        assertThat(aposTratamento.get(0).path("iniciada_por_id").asLong()).isEqualTo(c.localB.getId());
        assertThat(aposTratamento.get(0).path("iniciada_em").asText()).isNotBlank();
        var auditoriaDepois = aguardarAuditoriaTratamento(pendenciaId);
        assertThat(auditoriaDepois).hasSize(1);
        assertThat(auditoriaDepois.get(0).path("user_id").asLong()).isEqualTo(c.localB.getId());
        assertThat(auditoriaDepois.get(0).path("username").asText()).isEqualTo(c.localB.getUsername());
        assertThat(auditoriaDepois.get(0).path("created_at").asText()).isNotBlank();
        assertThat(auditoriaDepois.get(0).path("details_json").asText()).contains("EM_TRATAMENTO");
        registrar("AUD-006-004", mapa("contexto", contexto, "d1", d1, "transferenciaEm", t, "d2", d2,
                "instrumentosAntes", antes, "instrumentosDepois", depois, "transferencias", transferencias,
                "pendenciaAberta", pendencias, "listagemB", visivelB, "listagemA", invisivelA,
                "precedencias", precedencias, "tratamentoB", tratada, "pendenciaDepoisTratamento", aposTratamento,
                "auditoriaAntesRecusaA", auditoriaAntes, "auditoriaDepoisSucessoB", auditoriaDepois));
    }

    private List<LocalDate> diasUteisSemEventoNaJanela(Cenario c) throws Exception {
        var dias = new ArrayList<LocalDate>();
        for (LocalDate d = RelogioSistema.hoje().minusDays(6); !d.isAfter(RelogioSistema.hoje()); d = d.plusDays(1)) {
            final LocalDate data = d;
            boolean trabalha = emTransacao(() -> em.find(Jornada.class, c.jornadaA.getId()).getDias().stream()
                    .anyMatch(j -> j.getDiaSemana() == data.getDayOfWeek() && Boolean.TRUE.equals(j.getDiaTrabalho())));
            if (trabalha && consultar(c.localA, "/api/calendario-eventos?data=" + data + "&lotacaoId=" + c.lotacaoA.getId(),
                    "Conferir calendário efetivo antes de selecionar cada dia").isEmpty()) dias.add(data);
        }
        return dias;
    }

    private Map<String, Object> pedido(Cenario c, LocalDate data, String status) {
        return mapa("vinculoContratualId", c.vinculoA.getId(), "dataReferencia", data, "status", status,
                "chaveIdempotencia", "H04-AUD-" + UUID.randomUUID());
    }

    private JsonNode registrarRemanejamento(Cenario c, NovoContrato n, String nome, LocalDate data, int delta) throws Exception {
        var req = remanejamento(n, nome, data, delta);
        var previa = enviar(c.localA, base(n.id()) + "/remanejamentos/previa", req, 200, "Prévia legítima de " + nome);
        req.put("tokenPrevia", previa.path("tokenPrevia").asText());
        return enviar(c.localA, base(n.id()) + "/remanejamentos", req, 201, "Instrumento confirmado " + nome);
    }

    private void assertQuantidades(Cenario c, NovoContrato n, LocalDate data, int primeira, int segunda) throws Exception {
        var itens = consultar(c.localA, base(n.id()) + "/composicoes?data=" + data, "Replay sem quantitativos negativos");
        assertThat(itens).hasSize(2);
        assertThat(linhaId(itens, n.composicao1(), "id").path("efetivoPrevisto").asInt()).isEqualTo(primeira);
        assertThat(linhaId(itens, n.composicao2(), "id").path("efetivoPrevisto").asInt()).isEqualTo(segunda);
        for (var item : itens) assertThat(item.path("efetivoPrevisto").asInt()).isNotNegative();
    }

    private void assertInstrumentoPreservado(JsonNode atual, JsonNode original, String situacao) {
        for (String campo : List.of("id", "contratoId", "eventoId", "identificacao", "versaoInstrumento", "dataEfetiva",
                "justificativa", "referenciaSei", "registradoEm", "usuarioResponsavelId", "usuarioResponsavelUsername", "somaVariacoes", "itens"))
            assertThat(atual.path(campo)).as("Fotografia imutável de " + campo).isEqualTo(original.path(campo));
        assertThat(atual.path("situacao").asText()).isEqualTo(situacao);
    }

    private JsonNode fotografiaFrequencias(long contrato) {
        var tx = new TransactionTemplate(transactionManager); tx.setReadOnly(true);
        return tx.execute(s -> mapper.valueToTree(mapa(
                "frequencias", linhas("select * from frequencia where contrato_snapshot_id=? order by id", contrato),
                "eventos", linhas("select e.* from evento_frequencia e join frequencia f on f.id=e.id_frequencia where f.contrato_snapshot_id=? order by e.id_evento_frequencia", contrato),
                "pendencias", linhas("select * from pendencia_administrativa where contrato_id=? order by id", contrato))));
    }

    private JsonNode fotografiaPendencias(long contrato) {
        var tx = new TransactionTemplate(transactionManager); tx.setReadOnly(true);
        return tx.execute(s -> mapper.valueToTree(linhas("select * from pendencia_administrativa where contrato_id=? order by id", contrato)));
    }

    private JsonNode fotografiaAuditoriaTratamento(long id) {
        var tx = new TransactionTemplate(transactionManager); tx.setReadOnly(true);
        return tx.execute(s -> mapper.valueToTree(linhas("select * from registro_auditoria where entity_type='PendenciaAdministrativa' "
                + "and entity_id=? and action='PENDENCIA_INICIAR_TRATAMENTO' order by id", id)));
    }

    private JsonNode aguardarAuditoriaTratamento(long id) throws Exception {
        // RegistroAuditoria é publicado após commit em uma fila própria.
        // Esperar o consumidor não altera o ato, os contadores ou sua regra.
        long limite = System.nanoTime() + java.util.concurrent.TimeUnit.SECONDS.toNanos(10);
        JsonNode registros;
        do {
            filaAuditoria.descarregar();
            registros = fotografiaAuditoriaTratamento(id);
            if (!registros.isEmpty()) return registros;
            java.util.concurrent.TimeUnit.MILLISECONDS.sleep(20);
        } while (System.nanoTime() < limite);
        assertThat(registros).as("Auditoria após commit deve ser persistida pelo consumidor").hasSize(1);
        return registros;
    }

    /** Somente SELECT em todas as colunas: nenhum SQL DDL/DML de preparo ou correção. */
    private List<Map<String, String>> linhas(String sql, long contrato) {
        return jdbc.query(sql, (rs, n) -> {
            var linha = new LinkedHashMap<String, String>(); var meta = rs.getMetaData();
            for (int i = 1; i <= meta.getColumnCount(); i++) linha.put(meta.getColumnLabel(i), rs.getString(i));
            return linha;
        }, contrato);
    }

    private List<JsonNode> elementos(JsonNode itens, String campo, long id) {
        var r = new ArrayList<JsonNode>(); for (var item : itens) if (item.path(campo).asLong() == id) r.add(item); return r;
    }

    private JsonNode linhaId(JsonNode itens, long id, String campo) {
        var encontrados = elementos(itens, campo, id); assertThat(encontrados).as(campo + "=" + id).hasSize(1); return encontrados.get(0);
    }

    private void registrar(String criterio, Map<String, Object> evidencia) throws Exception {
        evidencia.put("criterio", criterio); evidencia.put("resultado", "PASS"); evidencia.put("observadoEm", Instant.now());
        evidencia.put("camada", "MockMvc + segurança real + JPA + PostgreSQL; sem TCP/Chrome ou relógio simulado");
        var arquivo = Path.of("target/auditoria-http/h04-contraprovas/" + criterio + ".json");
        Files.createDirectories(arquivo.getParent());
        Files.writeString(arquivo, mapper.writerWithDefaultPrettyPrinter().writeValueAsString(evidencia));
    }
}
