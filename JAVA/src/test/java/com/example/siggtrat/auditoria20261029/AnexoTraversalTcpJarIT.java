package com.example.siggtrat.auditoria20261029;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.time.*;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.zip.GZIPInputStream;

import static org.junit.jupiter.api.Assertions.*;

/** SEC012: request-target cru preserva os segmentos manipulados até o Tomcat real. */
class AnexoTraversalTcpJarIT extends GapsBackend15JarSuporte {
    @Test
    @Timeout(value = 160, unit = TimeUnit.SECONDS)
    void caminhosManipuladosNaoEntregamAnexoRestritoNemConsomemConcessao() throws Exception {
        preparar("sec012");
        try {
            iniciar("unico");
            try (Sessao gestor = login(c.localA); Sessao admin = login(c.admin); Sessao leitor = login(c.nacional)) {
                String canario = "PROTEGIDO-GAP15-" + UUID.randomUUID();
                byte[] bytes = (canario + "\nConteudo sintetico nominal restrito.\n").getBytes(StandardCharsets.UTF_8);
                JsonNode ocorrencia = gestor.multipart("/api/ocorrencias", mapa("empregadoId", c.empregadoA.getId(),
                        "motivo", "ATESTADO", "dataOcorrencia", LocalDate.now(ZONA).minusDays(2), "dias", 1,
                        "observacao", "Alvo sintético de traversal TCP", "chaveIdempotencia", "traversal-" + UUID.randomUUID()),
                        "protegido.txt", "text/plain", bytes, false);
                long id = ocorrencia.path("id").asLong(), anexo = ocorrencia.path("anexos").get(0).path("id").asLong();
                assertTrue(id > 0 && anexo > 0);
                String finalidade = "Conferencia sintetica de anexo TCP";
                JsonNode grant = admin.json("POST", "/api/permissoes-excepcionais", mapa("tipo", "ACESSO_DADOS_PESSOAIS_RESTRITOS",
                        "usuarioBeneficiarioId", c.nacional.getId(), "ocorrenciaId", id, "finalidade", finalidade,
                        "justificativa", "Teste específico de recusa de caminhos manipulados",
                        "autorizacaoAdministrativa", "ATO-SINTETICO-GAP15", "validoAte", Instant.now().plusSeconds(900)), 201);
                long permissao = grant.path("id").asLong();
                assertEquals(id, grant.path("ocorrenciaId").asLong());
                assertTrue(grant.path("contratoId").isNull()); assertTrue(grant.path("gestaoLocalId").isNull());
                String query = "?finalidade=" + URLEncoder.encode(finalidade, StandardCharsets.UTF_8);
                String canonico = "/api/ocorrencias/anexos/" + anexo + query;
                assertEquals(0L, usos(permissao));
                assertArrayEquals(bytes, leitor.pedido("GET", canonico, null, null, 200).body());
                assertEquals(1L, usos(permissao));
                Banco antes = esperarQuietude();
                Map<String, String> arquivosAntes = arquivos();
                List<String> sucessoAntes = auditoriasDeUso();
                String salvo = jdbc.queryForObject("SELECT nome_salvo FROM anexo_ocorrencia WHERE id=?", String.class, anexo);
                assertNotNull(salvo); assertFalse(salvo.contains("/")); assertFalse(salvo.contains("\\"));
                // Nenhum parâmetro filename ignorado: cada vetor altera o request-target efetivo.
                List<String> vetores = List.of(
                        "/api/ocorrencias/anexos/..%2f" + anexo,
                        "/api/ocorrencias/anexos/%2e%2e%2f" + anexo,
                        "/api/ocorrencias/anexos/%252e%252e%252f" + anexo,
                        "/api/ocorrencias/anexos/%2e%2e%5c" + anexo,
                        "/api/ocorrencias/anexos/../../uploads/ocorrencias/" + salvo,
                        "/api/ocorrencias/anexos/%2e%2e/%2e%2e/uploads/ocorrencias/" + salvo);
                List<Map<String, Object>> resultados = new ArrayList<>();
                for (String vetor : vetores) {
                    Resposta r = getCru(vetor + query, leitor.cookieHeader());
                    resultados.add(mapa("requestTarget", vetor + query, "status", r.status(), "bytes", r.corpo().length,
                            "bodySha256", hash(r.corpo()), "contentType", r.headers().get("content-type")));
                    prova("sec012-vetor", resultados.get(resultados.size() - 1));
                    assertTrue(Set.of(400, 403, 404).contains(r.status()), "Traversal exige recusa HTTP, inclusive na camada Tomcat/binding");
                    byte[] conteudo = r.corpo();
                    String encoding = r.headers().getOrDefault("content-encoding", "identity");
                    if (encoding.equalsIgnoreCase("gzip")) try (var gzip = new GZIPInputStream(new ByteArrayInputStream(conteudo))) {
                        conteudo = gzip.readNBytes(1_048_577); assertTrue(conteudo.length <= 1_048_576);
                    } else assertEquals("identity", encoding.toLowerCase(Locale.ROOT), "Não ignorar um encoding não inspecionado");
                    assertFalse(Arrays.equals(bytes, conteudo));
                    assertFalse(new String(conteudo, StandardCharsets.ISO_8859_1).contains(canario));
                    assertEquals(1L, usos(permissao), "Request-target manipulado não consome a concessão");
                    assertEquals(arquivosAntes, arquivos());
                }
                Banco depois = esperarQuietude();
                for (String tabela : List.of("ocorrencia", "contexto_ocorrencia", "anexo_ocorrencia", "permissao_excepcional",
                        "permissao_excepcional_uso", "frequencia", "evento_frequencia", "historico_empregado")) {
                    assertNotNull(antes.linhas().get(tabela), "Tabela do oráculo precisa existir: " + tabela);
                    assertEquals(antes.linhas().get(tabela), depois.linhas().get(tabela), tabela);
                }
                assertEquals(sucessoAntes, auditoriasDeUso(), "Recusas não registram sucesso de baixa ou uso");
                assertEquals(arquivosAntes, arquivos());
                assertArrayEquals(bytes, leitor.pedido("GET", canonico, null, null, 200).body());
                assertEquals(2L, usos(permissao), "Controle autorizado continua utilizável após todos os vetores");
                prova("sec012-final", mapa("ocorrenciaId", id, "anexoId", anexo, "permissaoId", permissao,
                        "sha256Protegido", hash(bytes), "vetores", resultados, "antes", antes, "depois", depois,
                        "arquivosAntes", arquivosAntes, "arquivosDepois", arquivos(), "usosAntes", 1, "usosDepoisAtaques", 1,
                        "usosDepoisControle", 2, "auditoriasSucessoAntes", sucessoAntes,
                        "limite", "Recusa TCP nos vetores declarados; alguns terminam em Tomcat/firewall/binding antes do storage. Não atribui o bloqueio a um handler não alcançado."));
            }
            semErrosOtimistas();
        } finally { encerrar(); }
    }

    private long usos(long id) { return jdbc.queryForObject("SELECT count(*) FROM permissao_excepcional_uso WHERE id_permissao_excepcional=?", Long.class, id); }
    private List<String> auditoriasDeUso() {
        return jdbc.query("SELECT to_jsonb(a)::text FROM registro_auditoria a WHERE action IN ('ANEXO_DOWNLOAD','USAR_PERMISSAO_EXCEPCIONAL') ORDER BY id",
                (r, n) -> SnapshotQuantitativos.hash(r.getString(1)));
    }
    private record Resposta(int status, Map<String, String> headers, byte[] corpo) {}
    private Resposta getCru(String alvo, String cookies) throws Exception {
        assertFalse(alvo.contains("\r") || alvo.contains("\n"));
        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress("127.0.0.1", origem.getPort()), 2000); socket.setSoTimeout(5000);
            String pedido = "GET " + alvo + " HTTP/1.1\r\nHost: 127.0.0.1:" + origem.getPort()
                    + "\r\nCookie: " + cookies + "\r\nAccept: */*\r\nConnection: close\r\n\r\n";
            socket.getOutputStream().write(pedido.getBytes(StandardCharsets.ISO_8859_1)); socket.getOutputStream().flush();
            InputStream in = socket.getInputStream();
            String status = linha(in); assertTrue(status.startsWith("HTTP/1.1 ") || status.startsWith("HTTP/1.0 "));
            Map<String, String> headers = new TreeMap<>();
            for (int i = 0; ; i++) {
                assertTrue(i < 100, "Máximo de 100 cabeçalhos");
                String h = linha(in); if (h.isEmpty()) break;
                int p = h.indexOf(':'); assertTrue(p > 0); headers.put(h.substring(0, p).toLowerCase(Locale.ROOT), h.substring(p + 1).trim());
            }
            ByteArrayOutputStream corpo = new ByteArrayOutputStream();
            if (headers.getOrDefault("transfer-encoding", "").toLowerCase(Locale.ROOT).contains("chunked")) {
                for (;;) {
                    int n = Integer.parseInt(linha(in).split(";", 2)[0].trim(), 16);
                    assertTrue(n >= 0 && corpo.size() + n <= 1_048_576, "Corpo excedeu 1 MiB");
                    if (n == 0) { while (!linha(in).isEmpty()) { /* trailers sem registro */ } break; }
                    byte[] b = in.readNBytes(n); assertEquals(n, b.length); corpo.write(b); assertEquals("", linha(in));
                }
            } else if (headers.containsKey("content-length")) {
                int n = Integer.parseInt(headers.get("content-length")); assertTrue(n >= 0 && n <= 1_048_576);
                byte[] b = in.readNBytes(n); assertEquals(n, b.length); corpo.write(b);
            } else { corpo.write(in.readNBytes(1_048_577)); assertTrue(corpo.size() <= 1_048_576); }
            return new Resposta(Integer.parseInt(status.split(" ", 3)[1]), Map.copyOf(headers), corpo.toByteArray());
        }
    }
}
