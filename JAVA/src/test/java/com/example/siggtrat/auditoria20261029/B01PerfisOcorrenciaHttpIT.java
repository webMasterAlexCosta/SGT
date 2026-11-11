package com.example.siggtrat.auditoria20261029;

import com.example.siggtrat.configuracao.propriedades.PropriedadesArmazenamentoArquivo;
import com.example.siggtrat.entidade.UsuarioAplicacao;
import com.example.siggtrat.servico.HistoricoIntegridadeServico;
import com.example.siggtrat.utilitario.RelogioSistema;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.request.AbstractMockHttpServletRequestBuilder;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.Arrays;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;

/** Proposta B01: exige execução pela raiz; não declara defeito nem resultado PASS. */
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class B01PerfisOcorrenciaHttpIT extends ContratosHttpBase {
    private static final String NARRATIVA = "B01-NARRATIVA-PESSOAL-SINTETICA";
    private static final String NOME_ANEXO = "B01-ANEXO-PESSOAL-SINTETICO.txt";
    private static final String SEI = "00001.000002/2026-01";
    private static final byte[] BYTES = "B01 bytes pessoais sintéticos preservados".getBytes(StandardCharsets.UTF_8);

    @Autowired PropriedadesArmazenamentoArquivo armazenamento;
    @Autowired HistoricoIntegridadeServico integridade;

    private record Contexto(Cenario c, long contratoId, long empregadoId, LocalDate data,
                            long ocorrenciaId, long anexoId, JsonNode criada) { }

    // Detecta POST indevidamente autorizado após GET e cancelamento Estadual;
    // a mesma intenção é depois aceita pelo Local, isolando a capacidade do perfil.
    @ParameterizedTest(name = "consulta não concede escrita a {0}")
    @ValueSource(strings = {"ESTADUAL", "PRESIDENCIA"})
    void consultaAutorizadaNaoConcedeCriacaoEEstadualNaoCancela(String perfil) throws Exception {
        Contexto x = preparar();
        UsuarioAplicacao leitor = perfil.equals("ESTADUAL") ? x.c.estadualA : x.c.presidente;
        JsonNode lida = consultar(leitor, "/api/ocorrencias/" + x.ocorrenciaId,
                "B01 controle positivo: perfil consulta a ocorrência existente no escopo");
        assertEquals(x.ocorrenciaId, lida.path("ocorrencia").path("id").asLong());
        assertTrue(integridade.verificarEmpregado(x.empregadoId).integro());
        var fontesAntes = fontesJpa(x.empregadoId);
        assertEquals(1, fontesAntes.get("ocorrencias").size());
        assertEquals(1, fontesAntes.get("anexos").size());
        assertFalse(fontesAntes.get("contextos").isEmpty());
        assertFalse(fontesAntes.get("historicos").isEmpty());
        var arquivosAntes = arquivos();
        String chave = "b01-negativa-" + UUID.randomUUID();

        executar(leitor, pedido(x, x.data.plusDays(1), chave), 403,
                "B01 GET autorizado não libera POST válido do perfil de consulta");
        assertEquals(fontesAntes, fontesJpa(x.empregadoId),
                "POST negado não cria nem muda fatos, anexos, contextos ou trilhas negociais");
        assertEquals(arquivosAntes, arquivos(), "POST negado não deixa arquivo nem staging órfão");
        conferirFonte(x);

        if (perfil.equals("ESTADUAL")) {
            executar(leitor, delete("/api/ocorrencias/{id}", x.ocorrenciaId), 403,
                    "B01 Estadual não cancela a mesma ocorrência válida que conseguiu consultar");
            assertEquals(fontesAntes, fontesJpa(x.empregadoId),
                    "DELETE negado conserva inclusive cancelamento, versões e hashes anteriores");
            assertEquals(arquivosAntes, arquivos());
            conferirFonte(x);
        }

        JsonNode controle = dados(executar(x.c.localA, pedido(x, x.data.plusDays(1), chave), 201,
                "B01 controle positivo: mesma intenção e arquivo são válidos para o Gestor Local"));
        assertTrue(controle.path("id").asLong() > 0);
        assertNotEquals(x.ocorrenciaId, controle.path("id").asLong());
        assertArrayEquals(BYTES, executar(x.c.localA,
                get("/api/ocorrencias/anexos/{id}", controle.path("anexos").get(0).path("id").asLong()),
                200, "B01 controle positivo dos bytes do POST válido").getResponse().getContentAsByteArray());
    }

    // Detecta lista vazia usada como falso oráculo ou exposição de campos/canários;
    // a releitura JPA/arquivo distingue minimização de destruição da fonte.
    @Test
    void listaEstadualConservaIdETotalSemNarrativaSeiOuAnexoPessoal() throws Exception {
        Contexto x = preparar();
        var fontesAntes = fontesJpa(x.empregadoId);
        var arquivosAntes = arquivos();
        JsonNode lista = dados(executar(x.c.estadualA, get("/api/ocorrencias")
                        .param("empregadoId", Long.toString(x.empregadoId))
                        .param("contratoId", Long.toString(x.contratoId)), 200,
                "B01 lista Estadual tem recorte positivo e aplica minimização própria"));
        assertEquals(1L, lista.path("totalElements").asLong());
        assertEquals(1, lista.path("content").size());
        JsonNode item = lista.path("content").get(0);
        assertEquals(x.ocorrenciaId, item.path("id").asLong());
        assertEquals(x.empregadoId, item.path("empregado").path("id").asLong());
        assertEquals(x.data.toString(), item.path("dataOcorrencia").asText());
        assertEquals("ATESTADO", item.path("motivo").asText());
        assertTrue(item.path("observacao").isNull() || item.path("observacao").isMissingNode());
        assertTrue(item.path("numeroSei").isNull() || item.path("numeroSei").isMissingNode());
        assertTrue(item.path("anexos").isMissingNode() || item.path("anexos").isNull()
                || item.path("anexos").isEmpty());
        String resposta = lista.toString();
        assertFalse(resposta.contains(NARRATIVA));
        assertFalse(resposta.contains(SEI));
        assertFalse(resposta.contains(NOME_ANEXO));
        assertFalse(resposta.contains(x.c.localA.getUsername()));
        assertEquals(fontesAntes, fontesJpa(x.empregadoId), "Minimização da lista não reescreve fontes");
        assertEquals(arquivosAntes, arquivos());
        conferirFonte(x);
    }

    private Contexto preparar() throws Exception {
        Cenario c = criarCenarioPersistido();
        assertNotEquals(c.gestaoA.getId(), c.gestaoB.getId());
        assertNotEquals(c.seA.getId(), c.seB.getId());
        assertNotEquals(c.seA.getUf(), c.seB.getUf());
        Path recibos = Path.of("target/auditoria-http/b01-escopos.jsonl");
        Files.createDirectories(recibos.getParent());
        Files.writeString(recibos, json(mapa("gestaoAId", c.gestaoA.getId(), "estadoAId", c.seA.getId(),
                "ufA", c.seA.getUf(), "gestaoBId", c.gestaoB.getId(), "estadoBId", c.seB.getId(),
                "ufB", c.seB.getUf())) + "\n", java.nio.file.StandardOpenOption.CREATE,
                java.nio.file.StandardOpenOption.APPEND);
        NovoContrato contrato = emTransacao(() -> {
            try { return publicado(c, 100, null, false); }
            catch (Exception e) { throw new IllegalStateException("Falha na fixture HTTP B01", e); }
        });
        LocalDate data = RelogioSistema.hoje().with(TemporalAdjusters.previous(DayOfWeek.WEDNESDAY));
        long vinculoId = enviar(c.localA, "/api/vinculos-contratuais", mapa(
                "pessoa", mapa("nome", "Pessoa sintética B01", "cpf", cpf(Math.toIntExact(970000000L + contrato.id()))),
                "contratoId", contrato.id(), "composicaoId", contrato.composicao1(), "dataInicio", data.minusDays(1)),
                201, "B01 vínculo próprio em contrato publicado").path("id").asLong();
        JsonNode frequencia = enviar(c.localA, "/api/frequencias", mapa("vinculoContratualId", vinculoId,
                "dataReferencia", data, "status", "PRESENCA", "horasTrabalhadas", "04:00", "horarioSaida", "12:00",
                "justificativaRetificacao", "Fonte sintética B01 anterior à ocorrência",
                "chaveIdempotencia", "b01-frequencia-" + UUID.randomUUID()), 200, "B01 controle do vínculo ativo");
        long empregadoId = frequencia.path("empregado").path("id").asLong();
        Contexto base = new Contexto(c, contrato.id(), empregadoId, data, 0, 0, null);
        JsonNode criada = dados(executar(c.localA, pedido(base, data, "b01-fonte-" + UUID.randomUUID()), 201,
                "B01 ocorrência válida com canários que realmente existem na fonte"));
        assertEquals(NARRATIVA, criada.path("observacao").asText());
        assertEquals(SEI, criada.path("numeroSei").asText());
        assertEquals(1, criada.path("anexos").size());
        assertEquals(NOME_ANEXO, criada.path("anexos").get(0).path("nomeOriginal").asText());
        Contexto x = new Contexto(c, contrato.id(), empregadoId, data, criada.path("id").asLong(),
                criada.path("anexos").get(0).path("id").asLong(), criada);
        conferirFonte(x);
        return x;
    }

    private AbstractMockHttpServletRequestBuilder<?> pedido(Contexto x, LocalDate data, String chave) {
        return multipart("/api/ocorrencias")
                .file(new MockMultipartFile("anexos", NOME_ANEXO, "text/plain", BYTES))
                .param("empregadoId", Long.toString(x.empregadoId)).param("motivo", "ATESTADO")
                .param("dataOcorrencia", data.toString()).param("dias", "1")
                .param("observacao", NARRATIVA).param("numeroSei", SEI)
                .param("classificacoesAnexos", "PESSOAL_RESTRITO").param("chaveIdempotencia", chave);
    }

    private void conferirFonte(Contexto x) throws Exception {
        JsonNode completa = consultar(x.c.localA, "/api/ocorrencias/" + x.ocorrenciaId,
                "B01 controle Local: a prova integral anterior foi preservada");
        assertEquals(x.criada, completa.path("ocorrencia"));
        assertFalse(completa.path("cancelada").asBoolean());
        assertArrayEquals(BYTES, executar(x.c.localA, get("/api/ocorrencias/anexos/{id}", x.anexoId),
                200, "B01 bytes originais continuam acessíveis ao Local").getResponse().getContentAsByteArray());
        assertTrue(integridade.verificarEmpregado(x.empregadoId).integro());
    }

    private Map<String, List<List<Object>>> fontesJpa(long empregadoId) {
        return emTransacao(() -> {
            em.clear();
            return Map.of(
                "ocorrencias", linhas("select o.id,o.empregado.id,o.dataOcorrencia,o.motivo,o.tipoRo,o.numeroSei,"
                    + "o.observacao,o.quantidadeDias,o.diasSuspensao,o.diasAfastamento,o.canceladoEm,o.canceladoPorId,"
                    + "o.canceladoPorUsername,o.motivoCancelamento,o.registradoEm,o.registradoPorId,"
                    + "o.empregadoNomeFotografia,o.chaveIdempotencia,o.hashIntencao,o.version from Ocorrencia o "
                    + "where o.empregado.id=:e order by o.id", empregadoId),
                "anexos", linhas("select a.id,a.ocorrencia.id,a.nomeOriginal,a.nomeSalvo,a.tipoArquivo,"
                    + "a.classificacaoAcesso,a.sha256,a.version from AnexoOcorrencia a "
                    + "where a.ocorrencia.empregado.id=:e order by a.id", empregadoId),
                "contextos", linhas("select c.id,c.ocorrencia.id,c.dataReferencia,c.contratoId,c.vinculoId,"
                    + "c.alocacaoId,c.gestaoLocalId,c.superintendenciaId,c.lotacaoId,c.jornadaId from ContextoOcorrencia c "
                    + "where c.ocorrencia.empregado.id=:e order by c.id", empregadoId),
                "historicos", linhas("select h.id,h.empregado.id,h.dataEvento,h.descricao,u.id,h.gestaoLocalSnapshotId,"
                    + "h.superintendenciaSnapshotId,h.hashIntegridade,h.versaoAssinatura,h.dataEfetiva,o.id,h.version "
                    + "from HistoricoEmpregado h left join h.usuario u left join h.ocorrencia o "
                    + "where h.empregado.id=:e order by h.id", empregadoId),
                "frequencias", linhas("select f.id,f.version,f.status,f.observacao,f.ocorrenciaOverrideAtivo,"
                    + "f.statusBaseAntesOcorrencia,f.estadoNegocial,f.validadeOperacional,f.estadoAuditoria,"
                    + "f.autorOriginalId,f.registradoEm,f.versaoNegocial from Frequencia f "
                    + "where f.empregado.id=:e order by f.id", empregadoId),
                "eventosFrequencia", linhas("select v.id,v.frequencia.id,v.numeroVersao,v.tipoEvento,v.dataReferencia,"
                    + "v.snapshotAnterior,v.snapshotNovo,v.motivo,v.usuarioId,v.usuarioUsername,v.registradoEm,"
                    + "v.chaveIdempotencia,v.hashIntencao,v.hashAnterior,v.hashEvento from EventoFrequencia v "
                    + "where v.frequencia.empregado.id=:e order by v.id", empregadoId));
        });
    }

    private List<List<Object>> linhas(String jpql, long empregadoId) {
        return em.createQuery(jpql, Object[].class).setParameter("e", empregadoId).getResultList().stream()
                .map(Arrays::asList).toList();
    }

    private Map<String, String> arquivos() throws Exception {
        Path raiz = armazenamento.uploadDir().toAbsolutePath().normalize();
        Map<String, String> hashes = new LinkedHashMap<>();
        if (Files.exists(raiz)) try (var caminhos = Files.walk(raiz)) {
            for (Path arquivo : caminhos.filter(Files::isRegularFile).sorted().toList()) {
                hashes.put(raiz.relativize(arquivo).toString(), HexFormat.of().formatHex(
                        MessageDigest.getInstance("SHA-256").digest(Files.readAllBytes(arquivo))));
            }
        }
        return hashes;
    }
}
