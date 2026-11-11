package com.example.siggtrat.auditoria20261029;

import com.example.siggtrat.utilitario.UtilitarioHashSeguranca;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.test.web.servlet.MvcResult;

import javax.sql.DataSource;
import java.math.BigDecimal;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.PosixFilePermissions;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Somente testes: SELECT* de todas as colunas, com valores tipados e hasheados.
 * Contrato e Pessoa não são reconstruídos de DTOs nem de campos escolhidos.
 * Uma conexão/transação read-only REPEATABLE_READ dá um corte aos snapshots;
 * não há DDL, coleção de PII em claro ou leitura de configuração operacional.
 */
final class SnapshotQuantitativos {
    private SnapshotQuantitativos() { }

    record Coluna(String nome, int jdbcTipo, String bancoTipo, String classeJava) { }
    record Celula(String coluna, boolean nulo, String sha256) { }
    record Linha(long id, List<Celula> valores) { }
    record Tabela(List<Coluna> colunas, List<Linha> linhas) { }
    record Estado(long contratoId, Long pessoaId, Map<String, Tabela> tabelas) { }

    static Estado capturar(DataSource fonte, long contrato, Long pessoa) throws SQLException {
        try (Connection c = fonte.getConnection()) {
            if (!"PostgreSQL".equals(c.getMetaData().getDatabaseProductName()))
                throw new IllegalStateException("O snapshot integral exige PostgreSQL real.");
            c.setReadOnly(true);
            c.setTransactionIsolation(Connection.TRANSACTION_REPEATABLE_READ);
            c.setAutoCommit(false);
            try {
                Map<String, Tabela> tabelas = new LinkedHashMap<>();
                ler(tabelas, c, "contrato", "id_contrato", "id_contrato=?", contrato);
                ler(tabelas, c, "composicao_contratual", "id_composicao_contratual", "id_contrato=?", contrato);
                ler(tabelas, c, "aditamento_contratual", "id_aditamento", "id_contrato=?", contrato);
                ler(tabelas, c, "aditamento_composicao", "id_aditamento_composicao",
                        "id_aditamento in (select id_aditamento from aditamento_contratual where id_contrato=?)", contrato);
                ler(tabelas, c, "remanejamento_contratual", "id_remanejamento", "id_contrato=?", contrato);
                ler(tabelas, c, "remanejamento_composicao", "id_remanejamento_composicao",
                        "id_remanejamento in (select id_remanejamento from remanejamento_contratual where id_contrato=?)", contrato);
                ler(tabelas, c, "contrato_evento_ciclo_vida", "id_evento_ciclo_vida", "id_contrato=?", contrato);
                ler(tabelas, c, "contrato_versao_negocial", "id_contrato_versao", "id_contrato=?", contrato);
                ler(tabelas, c, "contrato_evento_precedencia", "id_evento_precedencia", "id_contrato=?", contrato);
                ler(tabelas, c, "pendencia_administrativa", "id", "contrato_id=?", contrato);
                ler(tabelas, c, "permissao_excepcional", "id_permissao_excepcional", "id_contrato=?", contrato);
                ler(tabelas, c, "permissao_excepcional_uso", "id_permissao_uso",
                        "id_permissao_excepcional in (select id_permissao_excepcional from permissao_excepcional where id_contrato=?)", contrato);
                ler(tabelas, c, "vinculo_contratual", "id_vinculo_contratual", "id_contrato=?", contrato);
                ler(tabelas, c, "alocacao_vinculo", "id_alocacao_vinculo",
                        "id_vinculo_contratual in (select id_vinculo_contratual from vinculo_contratual where id_contrato=?)", contrato);
                if (pessoa != null) ler(tabelas, c, "pessoa", "id_pessoa", "id_pessoa=?", pessoa);
                c.commit();
                return new Estado(contrato, pessoa, Map.copyOf(tabelas));
            } catch (SQLException | RuntimeException | Error falha) {
                c.rollback();
                throw falha;
            }
        }
    }

    private static void ler(Map<String, Tabela> destino, Connection c, String tabela, String pk, String filtro, long id)
            throws SQLException {
        // Todos os identificadores/filtros são constantes privadas acima. Somente
        // o ID varia e é bindado; nenhuma consulta é registrada no artefato.
        try (var comando = c.prepareStatement("select * from " + tabela + " where " + filtro + " order by " + pk)) {
            comando.setLong(1, id);
            try (ResultSet r = comando.executeQuery()) {
                var m = r.getMetaData();
                List<Coluna> colunas = new ArrayList<>();
                for (int i = 1; i <= m.getColumnCount(); i++)
                    colunas.add(new Coluna(m.getColumnLabel(i), m.getColumnType(i), m.getColumnTypeName(i), m.getColumnClassName(i)));
                List<Linha> linhas = new ArrayList<>();
                while (r.next()) {
                    List<Celula> valores = new ArrayList<>();
                    for (int i = 1; i <= m.getColumnCount(); i++) {
                        Object valor = r.getObject(i);
                        Coluna coluna = colunas.get(i - 1);
                        String tipado = coluna.jdbcTipo() + ":" + coluna.bancoTipo() + ":"
                                + (valor == null ? "NULL" : valor.getClass().getName() + ":" + canonico(valor));
                        valores.add(new Celula(coluna.nome(), valor == null, hash(tipado)));
                    }
                    linhas.add(new Linha(r.getLong(pk), List.copyOf(valores)));
                }
                destino.put(tabela, new Tabela(List.copyOf(colunas), List.copyOf(linhas)));
            }
        }
    }

    private static String canonico(Object valor) throws SQLException {
        if (valor instanceof byte[] bytes) return Base64.getEncoder().encodeToString(bytes);
        if (valor instanceof java.sql.Array array) return Arrays.deepToString((Object[]) array.getArray());
        if (valor instanceof BigDecimal decimal) return decimal.toPlainString();
        if (valor instanceof java.sql.Timestamp data) return data.toInstant().toString();
        if (valor instanceof java.sql.Date data) return data.toLocalDate().toString();
        if (valor instanceof java.sql.Time hora) return hora.toLocalTime().toString();
        return valor.toString();
    }

    static String hash(String valor) { return UtilitarioHashSeguranca.sha256Hexadecimal(valor); }
    static String hashJson(ObjectMapper mapper, Object valor) throws Exception { return hash(mapper.writeValueAsString(valor)); }

    static Map<String, Object> http(ObjectMapper mapper, MvcResult resultado) throws Exception {
        var raiz = mapper.readTree(resultado.getResponse().getContentAsString(StandardCharsets.UTF_8));
        Map<String, Object> resumo = new LinkedHashMap<>();
        resumo.put("status", resultado.getResponse().getStatus());
        resumo.put("code", raiz == null ? null : raiz.path("code").asText(null));
        resumo.put("bodySha256", hash(resultado.getResponse().getContentAsString(StandardCharsets.UTF_8)));
        resumo.put("messageSha256", raiz == null || !raiz.path("message").isTextual() ? null : hash(raiz.path("message").asText()));
        // Preserva a guarda negocial que recusou uma pré-condição sem incluir
        // payload/alertas. O handler público já sanitiza falhas inesperadas;
        // esta proteção adicional não registra SQL ou documentos pessoais.
        String mensagem = raiz == null ? "" : raiz.path("message").asText("");
        if (resultado.getResponse().getStatus() >= 400 && resultado.getResponse().getStatus() < 500) {
            String publica = mensagem.matches("(?is).*\\b(select|insert|update|delete|jdbc|sqlstate|hibernate)\\b.*")
                    ? "[detalhe técnico omitido]" : mensagem.replaceAll("\\b\\d{11,14}\\b", "[documento]")
                            .replaceAll("[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}", "[email]");
            resumo.put("mensagemPublica", publica.substring(0, Math.min(1000, publica.length())));
        }
        return resumo;
    }

    static Path registrar(ObjectMapper mapper, String suite, String caso, Object dados) throws Exception {
        if (!suite.matches("[a-z0-9-]+") || !caso.matches("[a-zA-Z0-9-]+")) throw new IllegalArgumentException("Rótulo inválido");
        Path pasta = Path.of("target/quantitativos-" + suite).toAbsolutePath();
        var permissoes = PosixFilePermissions.fromString("rwx------");
        Files.createDirectories(pasta, PosixFilePermissions.asFileAttribute(permissoes));
        Files.setPosixFilePermissions(pasta, permissoes);
        Path arquivo = Files.createFile(pasta.resolve(caso + "-" + UUID.randomUUID() + ".json"),
                PosixFilePermissions.asFileAttribute(PosixFilePermissions.fromString("rw-------")));
        Map<String, Object> registro = new LinkedHashMap<>();
        registro.put("suite", suite); registro.put("caso", caso); registro.put("registradoEm", Instant.now());
        registro.put("auditRound", System.getenv("SGT_AUDITORIA_RODADA"));
        registro.put("limites", "HTTP MockMvc/serviços/JPA/PostgreSQL reais. Snapshot inclui todas as colunas tipadas, "
                + "valores hasheados, IDs e conjunto de colunas. Sem PII em claro, senha/token/cookie, SQL bruto ou mensagens de Throwable. "
                + "Dados contemporâneos sintéticos; não comprova jornada em navegador ou homologação global.");
        registro.put("evidencia", dados);
        byte[] bytes = (mapper.writerWithDefaultPrettyPrinter().writeValueAsString(registro) + "\n").getBytes(StandardCharsets.UTF_8);
        try (FileChannel canal = FileChannel.open(arquivo, StandardOpenOption.WRITE)) {
            ByteBuffer buffer = ByteBuffer.wrap(bytes); while (buffer.hasRemaining()) canal.write(buffer); canal.force(true);
        }
        try (FileChannel diretorio = FileChannel.open(pasta, StandardOpenOption.READ)) { diretorio.force(true); }
        return arquivo;
    }
}
