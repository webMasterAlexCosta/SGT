package com.example.siggtrat.servico;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.time.Instant;

/**
 * Gera relatório de usuários (CSV) com streaming para suportar grandes volumes.
 */
@Service
public class RelatorioUsuarioServico {

    private final JdbcTemplate jdbcTemplate;

    public RelatorioUsuarioServico(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public record Metadados(String fileName, Instant generatedAt) {}

    public Metadados gravarCsv(OutputStream out) throws IOException {
        String fileName = "usuarios_" + Instant.now().toString().replace(':', '-') + ".csv";

        try (BufferedWriter w = new BufferedWriter(new OutputStreamWriter(out, StandardCharsets.UTF_8))) {
            // BOM UTF-8 para preservar a acentuação ao abrir o CSV no Excel.
            w.write("\uFEFF");
            w.write("id;username;email;matricula;perfil;active;lotacao_id;lotacao_nome;jornada_id;jornada_desc");
            w.newLine();

            String sql = """
                    SELECT u.id,
                           u.username,
                           u.email,
                           u.matricula,
                           u.perfil,
                           u.active,
                           l.id_lotacao   AS lotacao_id,
                           l.nome         AS lotacao_nome,
                           j.id_jornada   AS jornada_id,
                           j.descricao    AS jornada_desc
                      FROM usuario_aplicacao u
                      LEFT JOIN lotacao l ON u.id_lotacao = l.id_lotacao
                      LEFT JOIN jornada j ON u.id_jornada = j.id_jornada
                     ORDER BY u.id
                    """;

            jdbcTemplate.query(sql, rs -> {
                try {
                    w.write(nuloSeguro(rs.getObject("id")));
                    w.write(';');
                    w.write(seguroParaCsv(rs.getString("username")));
                    w.write(';');
                    w.write(seguroParaCsv(rs.getString("email")));
                    w.write(';');
                    w.write(seguroParaCsv(rs.getString("matricula")));
                    w.write(';');
                    w.write(seguroParaCsv(rs.getString("perfil")));
                    w.write(';');
                    w.write(nuloSeguro(rs.getObject("active")));
                    w.write(';');
                    w.write(nuloSeguro(rs.getObject("lotacao_id")));
                    w.write(';');
                    w.write(seguroParaCsv(rs.getString("lotacao_nome")));
                    w.write(';');
                    w.write(nuloSeguro(rs.getObject("jornada_id")));
                    w.write(';');
                    w.write(seguroParaCsv(rs.getString("jornada_desc")));
                    w.newLine();
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            });

            w.flush();
        }

        return new Metadados(fileName, Instant.now());
    }

    private String nuloSeguro(Object v) {
        return v == null ? "" : String.valueOf(v);
    }

    /**
     * CSV simples com separador ';'. Remove quebras de linha e normaliza aspas.
     */
    private String seguroParaCsv(String v) {
        if (v == null) return "";
        String s = v.replace('\n', ' ').replace('\r', ' ');
        // Evita quebrar leitura em parsers que interpretam aspas.
        return s.replace('"', '\'');
    }
}
