package com.example.siggtrat.auditoria20261029;

import com.example.siggtrat.entidade.*;
import com.example.siggtrat.servico.MotorTemporalContratoServico;
import com.example.siggtrat.utilitario.RelogioSistema;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.http.MediaType;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import javax.sql.DataSource;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermissions;
import java.security.MessageDigest;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

/**
 * Preparação OPT-IN, fora de src/test: não é prova de transporte HTTP/TCP.
 *
 * Publica e transfere somente o contrato criado aqui, preserva em arquivo a
 * fonte sintética ANTES de simular a perda de grade/município do mapa e relê os
 * commits em transações novas. O roteiro Python realiza a cadeia negocial no
 * JAR: grade -> nova alocação -> frequência fechada -> município -> reinício.
 *
 * MySQL real 8.0.45, schema exclusivamente Hibernate/update. Não contém DDL,
 * recuperação de acervo recebido, mocks de persistência ou frequências prontas.
 * O TaskScheduler suprimido é herdado da infraestrutura de preparação; o JAR
 * usado pelo roteiro TCP é o executável normal, sem essa configuração de teste.
 */
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class PreparacaoMapaParcialTcpIT extends ContratosHttpBase {
    @Autowired DataSource dataSource;
    @Autowired Environment ambiente;
    @Autowired MotorTemporalContratoServico motor;

    /** Evita o arquivo adicional de exemplos que grava tokenPrevia sem máscara. */
    @Override
    protected JsonNode enviar(UsuarioAplicacao usuario, String path, Object body,
                               int status, String regra) throws Exception {
        return dados(executar(usuario, post(path).contentType(MediaType.APPLICATION_JSON)
                .content(json(body)), status, regra));
    }

    @Test
    void prepararMapaPorTriadeComFonteAnteriorALacuna() throws Exception {
        String url = System.getProperty("integration.mysql.url", System.getenv("INTEGRATION_MYSQL_URL"));
        assertNotNull(url, "Declare explicitamente a base MySQL sintética local");
        assertTrue(url.matches("jdbc:mysql://(127\\.0\\.0\\.1|localhost):[0-9]+/(sgt_audit_[A-Za-z0-9_]+|sgt_correcao_exec|sgt8045_(baseline|candidata|upgrade|http|409_red|regressao))(\\?.*)?"),
                "A preparação é restrita a bases sintéticas locais explicitamente nomeadas");
        assertEquals("update", ambiente.getProperty("spring.jpa.hibernate.ddl-auto"));
        assertEquals("never", ambiente.getProperty("spring.sql.init.mode"));
        String banco;
        String versaoMysql;
        try (var conexao = dataSource.getConnection()) {
            var meta = conexao.getMetaData();
            assertEquals("MySQL", meta.getDatabaseProductName());
            assertEquals("8.0.45", meta.getDatabaseProductVersion(), "Servidor JDBC deve ser exatamente o alvo autorizado");
            try (var consulta = conexao.createStatement(); var resultado = consulta.executeQuery("SELECT VERSION()")) {
                assertTrue(resultado.next());
                assertEquals("8.0.45", resultado.getString(1), "Versão observada na mesma conexão JDBC");
            }
            banco = conexao.getCatalog();
            versaoMysql = meta.getDatabaseProductVersion();
        }
        Path arquivo = Path.of(System.getProperty("fixture.mapa-parcial.saida",
                "target/auditoria-http/fixture_mapa_parcial_tcp.json")).toAbsolutePath().normalize();
        Path arquivoFonte = arquivo.resolveSibling(arquivo.getFileName() + ".fonte_preexistente.json");
        assertFalse(Files.exists(arquivo), "Não sobrescrever fixture/execução anterior");
        assertFalse(Files.exists(arquivoFonte), "Preserve inclusive a fonte de uma preparação interrompida");

        Cenario c = criarCenarioPersistido();
        NovoContrato n = emTransacao(() -> {
            try { return publicado(c, 3, null, false); }
            catch (Exception ex) { throw new IllegalStateException(ex); }
        });
        LocalDate transferenciaEm = n.inicio().plusDays(2);
        LocalDate diaFrequencia = transferenciaEm.plusDays(3)
                .with(TemporalAdjusters.nextOrSame(DayOfWeek.MONDAY));
        LocalDate diaSemFrequencia = diaFrequencia.plusDays(1);
        assertTrue(diaSemFrequencia.isBefore(RelogioSistema.hoje().minusDays(7)),
                "O teste usa a janela normal: ambos os dias já passaram do prazo operacional");
        String municipio = "MUNICIPIO SINTETICO MAPA TCP " + n.id();
        String mestreDivergente = "MUNICIPIO ATUAL SEM PROVA TCP " + n.id();
        emTransacao(() -> {
            em.find(Lotacao.class, c.lotacaoB.getId()).setMunicipio(municipio);
            return null;
        });
        conceder(c, n.id(), "TRANSFERENCIA_GESTAO_LOCAL");
        Map<String, Object> mapaPorTriade = mapa(
                "lotacaoOrigemId", c.lotacaoA.getId(), "jornadaOrigemId", c.jornadaA.getId(),
                "cargoOrigemId", c.cargoA.getId(), "lotacaoDestinoId", c.lotacaoB.getId(),
                "jornadaDestinoId", c.jornadaB.getId(), "cargoDestinoId", c.cargoB.getId());
        assertFalse(mapaPorTriade.containsKey("composicaoOrigemId"));
        Map<String, Object> req = mapa("dataEfetiva", transferenciaEm, "dataAto", RelogioSistema.hoje(),
                "gestaoLocalDestinoId", c.gestaoB.getId(), "tipoContratacaoDestinoId", c.tipoB.getId(),
                "motivo", "Transferência sintética por tríade para prova TCP de propriedades parciais",
                "referenciaSei", "SEI-SINTETICO-TCP-TRANSFERENCIA-" + n.id(),
                "autorizacaoAdministrativa", "ATO-HTTP", "mapeamentos", List.of(mapaPorTriade));
        req.put("tokenPrevia", enviar(c.localA, base(n.id()) + "/transferencias-gestao/previa", req, 200,
                "Origem canônica aceita por tríade, sem composicaoOrigemId").path("tokenPrevia").asText());
        req.put("confirmarImpacto", true);
        enviar(c.localA, base(n.id()) + "/transferir-gestao", req, 200,
                "Transferência sintética confirmada pelas rotas existentes");

        // O calendário existe antes das provas e usa o município ainda presente
        // no mestre sintético. Depois o mestre diverge; não pode ser recapturado.
        var calendarios = new java.util.ArrayList<Long>();
        for (LocalDate dia : List.of(diaFrequencia, diaSemFrequencia)) {
            JsonNode calendario = enviar(c.localB, "/api/calendario-eventos", mapa(
                    "data", dia, "tipo", "FERIADO", "descricao", "Feriado municipal sintético TCP do mapa",
                    "abrangencia", "MUNICIPAL", "municipio", municipio, "ativo", true), 201,
                    "Calendário municipal sintético criado pelo serviço canônico");
            calendarios.add(calendario.path("id").asLong());
        }

        String cenarioId = UUID.randomUUID().toString();
        Map<String, Object> fonte = emTransacao(() -> {
            TransferenciaGestaoContrato t = em.createQuery(
                    "select t from TransferenciaGestaoContrato t where t.contrato.id=:c", TransferenciaGestaoContrato.class)
                    .setParameter("c", n.id()).getSingleResult();
            assertEquals(1, t.getMapeamentos().size());
            MapeamentoTransferenciaGestao m = t.getMapeamentos().get(0);
            assertNull(m.getComposicaoOrigem(), "A origem foi declarada apenas pela tríade");
            assertEquals(c.lotacaoA.getId(), m.getLotacaoOrigem().getId());
            assertEquals(c.jornadaA.getId(), m.getJornadaOrigem().getId());
            assertEquals(c.cargoA.getId(), m.getCargoOrigem().getId());
            assertEquals(municipio, m.getLotacaoDestinoMunicipioFotografia());
            assertNotNull(m.getJornadaDestinoSemanalFotografia());
            assertFalse(m.getJornadaDestinoSemanalFotografia().isBlank());
            VersaoContratoNegocial v = em.createQuery(
                    "select v from VersaoContratoNegocial v where v.eventoOrigem.id=:e", VersaoContratoNegocial.class)
                    .setParameter("e", t.getEvento().getId()).getSingleResult();
            VersaoContratoNegocial publicacao = motor.fotografiaOriginalConhecida(t.getContrato(), Instant.now()).orElseThrow();
            assertEquals(TipoEventoCicloVidaContrato.PUBLICACAO, publicacao.getEventoOrigem().getTipoEvento());
            Map<String, Object> foto = fotografia(v);
            for (String campo : List.of("id", "numeroVersao", "eventoOrigemId", "registradoEm", "tipoContratacaoComprovado"))
                foto.remove(campo);
            return mapa("somente_dados_sinteticos", true, "cenario_id", cenarioId,
                    "capturada_antes_da_lacuna_em", Instant.now(),
                    "descricao", "Documento-oráculo da criação sintética; não representa acervo autêntico externo",
                    "contrato_id", n.id(), "composicao_base_id", n.composicao1(),
                    "transferencia_id", t.getId(), "mapeamento_id", m.getId(),
                    "evento_transferencia_id", t.getEvento().getId(), "versao_transferencia_id", v.getId(),
                    "versao_publicacao_id", publicacao.getId(), "data_transferencia", transferenciaEm,
                    "dia_frequencia", diaFrequencia, "dia_sem_frequencia", diaSemFrequencia,
                    "jornada_semanal", m.getJornadaDestinoSemanalFotografia(), "municipio", municipio,
                    "municipio_mestre_divergente", mestreDivergente,
                    "fotografia_transferencia", foto,
                    "versao_original_transferencia", fotografia(v), "versao_original_publicacao", fotografia(publicacao),
                    "mapa_documentado", mapa("id", m.getId(), "composicaoOrigemId", null,
                            "lotacaoOrigemId", m.getLotacaoOrigem().getId(), "jornadaOrigemId", m.getJornadaOrigem().getId(),
                            "cargoOrigemId", m.getCargoOrigem().getId(), "lotacaoDestinoId", m.getLotacaoDestino().getId(),
                            "jornadaDestinoId", m.getJornadaDestino().getId(), "cargoDestinoId", m.getCargoDestino().getId(),
                            "lotacaoNome", m.getLotacaoDestinoNomeFotografia(), "jornadaDescricao", m.getJornadaDestinoDescricaoFotografia(),
                            "cargoNome", m.getCargoDestinoNomeFotografia(), "lotacaoMunicipio", m.getLotacaoDestinoMunicipioFotografia(),
                            "jornadaSemanal", m.getJornadaDestinoSemanalFotografia()),
                    "calendario_ids", List.copyOf(calendarios),
                    "referencia_sei_grade", "SEI-SINTETICO-TCP-GRADE-MAPA-" + n.id(),
                    "referencia_sei_municipio", "SEI-SINTETICO-TCP-MUNICIPIO-MAPA-" + n.id());
        });
        String fonteJson = json(fonte);
        salvarPrivadoNovo(arquivoFonte, fonteJson);
        String fonteSha = sha(Files.readAllBytes(arquivoFonte));
        long mapaId = ((Number) fonte.get("mapeamento_id")).longValue();
        emTransacao(() -> {
            MapeamentoTransferenciaGestao m = em.find(MapeamentoTransferenciaGestao.class, mapaId);
            assertEquals(n.id(), m.getTransferencia().getContrato().getId());
            assertEquals(municipio, m.getLotacaoDestinoMunicipioFotografia());
            assertEquals(1, em.createQuery("update MapeamentoTransferenciaGestao m set "
                    + "m.jornadaDestinoSemanalFotografia=null, m.lotacaoDestinoMunicipioFotografia=null where m.id=:m")
                    .setParameter("m", mapaId).executeUpdate());
            em.clear();
            em.find(Lotacao.class, c.lotacaoB.getId()).setMunicipio(mestreDivergente);
            return null;
        });
        // Uma transação nova comprova o commit da perda e a ausência de atos
        // que somente o roteiro TCP pode produzir neste contrato.
        emTransacao(() -> {
            MapeamentoTransferenciaGestao m = em.find(MapeamentoTransferenciaGestao.class, mapaId);
            assertNull(m.getJornadaDestinoSemanalFotografia());
            assertNull(m.getLotacaoDestinoMunicipioFotografia());
            assertNull(m.getComposicaoOrigem());
            assertEquals(mestreDivergente, em.find(Lotacao.class, c.lotacaoB.getId()).getMunicipio());
            assertEquals(mapper.valueToTree(fonte.get("versao_original_transferencia")), mapper.valueToTree(
                    fotografia(em.find(VersaoContratoNegocial.class, ((Number) fonte.get("versao_transferencia_id")).longValue()))));
            assertEquals(mapper.valueToTree(fonte.get("versao_original_publicacao")), mapper.valueToTree(
                    fotografia(em.find(VersaoContratoNegocial.class, ((Number) fonte.get("versao_publicacao_id")).longValue()))));
            assertEquals(0L, em.createQuery("select count(v) from VinculoContratual v where v.contrato.id=:c", Long.class)
                    .setParameter("c", n.id()).getSingleResult());
            assertEquals(0L, em.createQuery("select count(p) from ProvaHistoricaContrato p where p.contrato.id=:c", Long.class)
                    .setParameter("c", n.id()).getSingleResult());
            return null;
        });
        Map<String, UsuarioAplicacao> atores = Map.of("local", c.localB, "origem", c.localA,
                "nacional", c.nacional, "admin", c.admin);
        Map<String, Object> usuarios = new LinkedHashMap<>();
        Map<String, Object> usuariosIds = new LinkedHashMap<>();
        atores.forEach((papel, u) -> { usuarios.put(papel, u.getMatricula()); usuariosIds.put(papel, u.getId()); });
        Map<String, Object> fixture = mapa("somente_dados_sinteticos", true, "cenario_id", cenarioId,
                "banco_sintetico", banco, "mysql_versao", versaoMysql,
                "senha", SENHA, "usuarios", usuarios, "usuarios_ids", usuariosIds,
                "gestao_destino_id", c.gestaoB.getId(), "lotacao_destino_id", c.lotacaoB.getId(),
                "jornada_destino_id", c.jornadaB.getId(), "cargo_destino_id", c.cargoB.getId(),
                "arquivo_fonte_preexistente", arquivoFonte.toString(), "fonte_preexistente_json", fonteJson,
                "fonte_preexistente_sha256", fonteSha);
        salvarPrivadoNovo(arquivo, json(fixture));
        assertEquals(fonteSha, sha(Files.readAllBytes(arquivoFonte)));
        assertEquals(cenarioId, mapper.readTree(Files.readString(arquivo)).path("cenario_id").asText());
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> fotografia(VersaoContratoNegocial v) {
        return mapper.convertValue(motor.paraVersaoDTO(v), LinkedHashMap.class);
    }

    private static void salvarPrivadoNovo(Path path, String conteudo) throws Exception {
        Files.createDirectories(path.getParent());
        Files.createFile(path, PosixFilePermissions.asFileAttribute(PosixFilePermissions.fromString("rw-------")));
        Files.writeString(path, conteudo, StandardCharsets.UTF_8);
    }

    private static String sha(byte[] bytes) throws Exception {
        return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));
    }
}
