package com.example.siggtrat.auditoria20261029;

import com.example.siggtrat.entidade.FrequenciaAdicional;
import com.example.siggtrat.entidade.FrequenciaExcecao;
import com.example.siggtrat.entidade.VersaoComplementoFrequencia;
import com.example.siggtrat.utilitario.RelogioSistema;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import javax.sql.DataSource;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;

/**
 * Autoria independente: /root/revisao_final_pessoas.
 * Fonte examinada C5: 8888e7335e25cd80519f1f0d17c9d1c190e49d2f0f77131ecfc9fafe909e6fa8.
 * Complementa, sem alterar, o oráculo de Candidato4ComplementoNomeHistoricoRedIT.
 * Todos os documentos/SEIs e cenários abaixo são explicitamente sintéticos.
 * A fixture começa sem origem nominal, sem apagar fontes para fabricar ausência.
 * As provas entram por concessão, prévia e confirmação HTTP, nunca por INSERT
 * direto de prova ou UPDATE de fotografia antiga. Os comandos têm commits reais.
 * Não compilado nem executado pelo revisor; execução central serial pela raiz.
 */
@Transactional(propagation = Propagation.NOT_SUPPORTED)
@TestPropertySource(properties = {"spring.cache.type=redis", "spring.sql.init.mode=never",
        "app.cache.redis.http-get.enabled=true"})
class Candidato5ComplementoNomePeriodoHttpIT extends ContratosHttpBase {
    @Autowired private DataSource dataSource;
    @Autowired private RedisConnectionFactory redis;
    @Autowired private Environment ambiente;

    @BeforeEach
    void exigirInfraestruturaEPeriodoReais() throws Exception {
        try (var conexao = dataSource.getConnection()) {
            assertThat(conexao.getMetaData().getDatabaseProductName()).isEqualTo("MySQL");
            assertThat(conexao.getMetaData().getDatabaseProductVersion()).isEqualTo("8.0.45");
        }
        assertThat(ambiente.getProperty("spring.jpa.hibernate.ddl-auto")).isEqualTo("update");
        assertThat(ambiente.getProperty("spring.sql.init.mode")).isEqualTo("never");
        assertThat(ambiente.getProperty("spring.cache.type")).isEqualTo("redis");
        try (var conexao = redis.getConnection()) { assertThat(conexao.ping()).isEqualTo("PONG"); }
        assertThat(DATA.plusDays(2)).as("Todos os dias do ensaio são retroativos").isBefore(RelogioSistema.hoje());
    }

    @Test
    void excecaoComExtremosComprovadosELacunaNominalCentralNaoAfirmaNomeParaTodoIntervalo() throws Exception {
        Cenario c = semOrigemNominal();
        conceder(c, c.contratoA.getId(), "INCORPORACAO_PROVA_HISTORICA");
        String nome = "Nome documental sintético dos extremos C5";
        registrarProva(c, nome, DATA, DATA);
        registrarProva(c, nome, DATA.plusDays(2), DATA.plusDays(2));
        assertThat(nomeHistorico(c, DATA, null).path("nome").asText()).isEqualTo(nome);
        assertThat(nomeHistorico(c, DATA.plusDays(2), null).path("nome").asText()).isEqualTo(nome);
        assertThat(nomeHistorico(c, DATA.plusDays(1), null).path("comprovado").asBoolean()).isFalse();

        JsonNode criada = enviar(c.localA, rota("excecoes"), pedido(c, "excecoes", DATA, DATA.plusDays(2)),
                201, "Exceção de três dias não extrapola as fontes nominais de seus extremos");
        long id = criada.path("id").asLong();
        Estado antes = estado("excecoes", id);
        assertThat(criada.path("empregadoNome").asText(null)).isNull();
        assertThat(antes.nome()).isNull();
        assertThat(antes.versoes()).hasSize(1);
        assertThat(nomeNaVersao(antes.versoes().get(0))).isNull();
        JsonNode eventosAntes = eventos(c, "excecoes", id, null);
        assertThat(eventosAntes).hasSize(1);
        assertThat(eventosAntes.get(0).path("contextos")).hasSize(3);
        assertThat(eventosAntes.get(0).path("fotografia").path("empregadoNome").asText(null)).isNull();

        // Controle positivo por fluxo legítimo: reduzir o recorte ao dia provado.
        JsonNode reduzida = atualizar(c, "excecoes", id, mapa("dataFim", DATA,
                "justificativa", "Retificação documental sintética limita a exceção ao primeiro dia comprovado"));
        Estado depois = estado("excecoes", id);
        assertThat(reduzida.path("empregadoNome").asText()).isEqualTo(nome);
        assertThat(depois.empregadoId()).isEqualTo(c.empregadoA.getId());
        assertThat(depois.inicio()).isEqualTo(DATA);
        assertThat(depois.fim()).isEqualTo(DATA);
        assertThat(depois.nome()).isEqualTo(nome);
        assertThat(depois.versoes()).hasSize(2);
        assertThat(depois.versoes().get(0)).as("A fotografia ampla sem nome permanece byte a byte").isEqualTo(antes.versoes().get(0));
        assertThat(nomeNaVersao(depois.versoes().get(1))).isEqualTo(nome);
        JsonNode eventosDepois = eventos(c, "excecoes", id, null);
        assertThat(eventosDepois.get(0)).isEqualTo(eventosAntes.get(0));
        assertThat(eventosDepois.get(1).path("contextos")).hasSize(1);
        conferirPredecessora(depois.versoes().get(0), depois.versoes().get(1));
        evidenciar("intervalo-parcial", mapa("antes", antes, "depois", depois,
                "eventosAntes", eventosAntes, "eventosDepois", eventosDepois));
    }

    @ParameterizedTest
    @ValueSource(strings = {"adicionais", "excecoes"})
    void mudarSomenteDataNoMesmoEmpregadoReavaliaNomeSemEmprestarFotoDoDiaAnterior(String recurso) throws Exception {
        Cenario c = semOrigemNominal();
        conceder(c, c.contratoA.getId(), "INCORPORACAO_PROVA_HISTORICA");
        String nome = "Nome documental sintético do primeiro dia C5";
        registrarProva(c, nome, DATA, DATA);
        assertThat(nomeHistorico(c, DATA.plusDays(1), null).path("nome").asText(null)).isNull();
        JsonNode criada = enviar(c.localA, rota(recurso), pedido(c, recurso, DATA, DATA), 201,
                "Origem no dia nominalmente comprovado");
        long id = criada.path("id").asLong();
        Estado antes = estado(recurso, id);
        assertThat(criada.path("empregadoNome").asText()).isEqualTo(nome);
        assertThat(antes.nome()).isEqualTo(nome);
        assertThat(antes.versoes()).hasSize(1);

        Map<String,Object> mudanca = somenteData(recurso, DATA.plusDays(1));
        assertThat(mudanca).as("O comando altera a data e declara motivo; não troca Empregado").doesNotContainKey("empregadoId");
        JsonNode alterada = atualizar(c, recurso, id, mudanca);
        Estado depois = estado(recurso, id);
        assertThat(alterada.path("empregadoId").asLong()).isEqualTo(c.empregadoA.getId());
        assertThat(alterada.path("empregadoNome").asText(null)).isNull();
        assertThat(depois.empregadoId()).isEqualTo(antes.empregadoId());
        assertThat(depois.inicio()).isEqualTo(DATA.plusDays(1));
        assertThat(depois.fim()).isEqualTo(DATA.plusDays(1));
        assertThat(depois.nome()).isNull();
        assertThat(depois.versoes()).hasSize(2);
        assertThat(depois.versoes().get(0)).isEqualTo(antes.versoes().get(0));
        assertThat(nomeNaVersao(depois.versoes().get(1))).isNull();
        conferirPredecessora(depois.versoes().get(0), depois.versoes().get(1));
        JsonNode historico = eventos(c, recurso, id, null);
        assertThat(historico.get(0).path("fotografia").path("empregadoNome").asText()).isEqualTo(nome);
        assertThat(historico.get(1).path("fotografia").path("empregadoNome").asText(null)).isNull();
        assertThat(eventos(c, recurso, id, antes.versoes().get(0).registradoEm())).hasSize(1);
        evidenciar("somente-data-" + recurso, mapa("antes", antes, "depois", depois, "eventos", historico));
    }

    @ParameterizedTest
    @ValueSource(strings = {"adicionais", "excecoes"})
    void provaTardiaEnriqueceNovoRecortePreservandoVersoesAntigasSemNome(String recurso) throws Exception {
        Cenario c = semOrigemNominal();
        JsonNode criada = enviar(c.localA, rota(recurso), pedido(c, recurso, DATA, DATA), 201,
                "Fonte manual retroativa originalmente sem nome comprovado");
        long id = criada.path("id").asLong();
        Estado original = estado(recurso, id);
        JsonNode eventosOriginais = eventos(c, recurso, id, null);
        assertThat(criada.path("empregadoNome").asText(null)).isNull();
        assertThat(original.nome()).isNull();
        assertThat(original.versoes()).hasSize(1);
        assertThat(nomeNaVersao(original.versoes().get(0))).isNull();
        Instant corteOriginal = original.versoes().get(0).registradoEm();

        conceder(c, c.contratoA.getId(), "INCORPORACAO_PROVA_HISTORICA");
        String nome = "Nome documental sintético recebido tardiamente C5";
        JsonNode prova = registrarProva(c, nome, DATA, DATA.plusDays(1));
        assertThat(Instant.parse(prova.path("registradoEm").asText())).isAfter(corteOriginal);
        assertThat(nomeHistorico(c, DATA, null).path("nome").asText()).isEqualTo(nome);
        assertThat(nomeHistorico(c, DATA, corteOriginal).path("nome").asText(null)).isNull();
        assertThat(estado(recurso, id)).as("Incorporar prova não reescreve a fonte manual já registrada").isEqualTo(original);
        assertThat(eventos(c, recurso, id, null)).isEqualTo(eventosOriginais);
        assertThat(consultar(c.localA, "/api/pessoas/" + c.pessoaA.getId(), "Prova histórica não renomeia cadastro atual")
                .path("nome").asText()).isEqualTo(c.pessoaA.getNome());

        Map<String,Object> mesmoRecorte = recurso.equals("excecoes")
                ? mapa("observacao", "Observação retificada sem mudar o período original",
                        "justificativa", "Retificação sintética apenas de observação; ausência nominal anterior preservada")
                : mapa("minutos", 45, "justificativa", "Retificação sintética de quantidade; ausência nominal anterior preservada");
        JsonNode comum = atualizar(c, recurso, id, mesmoRecorte);
        Estado intermediario = estado(recurso, id);
        assertThat(comum.path("empregadoNome").asText(null)).isNull();
        assertThat(intermediario.nome()).isNull();
        assertThat(intermediario.inicio()).isEqualTo(DATA);
        assertThat(intermediario.fim()).isEqualTo(DATA);
        assertThat(intermediario.versoes()).hasSize(2);
        assertThat(intermediario.versoes().get(0)).isEqualTo(original.versoes().get(0));
        assertThat(nomeNaVersao(intermediario.versoes().get(1))).isNull();

        JsonNode novoRecorte = atualizar(c, recurso, id, somenteData(recurso, DATA.plusDays(1)));
        Estado finalizado = estado(recurso, id);
        assertThat(novoRecorte.path("empregadoNome").asText()).isEqualTo(nome);
        assertThat(finalizado.nome()).isEqualTo(nome);
        assertThat(finalizado.empregadoId()).isEqualTo(original.empregadoId());
        assertThat(finalizado.inicio()).isEqualTo(DATA.plusDays(1));
        assertThat(finalizado.fim()).isEqualTo(DATA.plusDays(1));
        assertThat(finalizado.versoes()).hasSize(3);
        assertThat(finalizado.versoes().subList(0, 2)).as("Duas fotografias anteriores continuam desconhecidas e intactas")
                .isEqualTo(intermediario.versoes());
        assertThat(nomeNaVersao(finalizado.versoes().get(2))).isEqualTo(nome);
        conferirPredecessora(finalizado.versoes().get(0), finalizado.versoes().get(1));
        conferirPredecessora(finalizado.versoes().get(1), finalizado.versoes().get(2));
        JsonNode historia = eventos(c, recurso, id, null);
        assertThat(historia).hasSize(3);
        assertThat(historia.get(0)).isEqualTo(eventosOriginais.get(0));
        assertThat(historia.get(1).path("fotografia").path("empregadoNome").asText(null)).isNull();
        assertThat(historia.get(2).path("fotografia").path("empregadoNome").asText()).isEqualTo(nome);
        assertThat(eventos(c, recurso, id, corteOriginal)).isEqualTo(eventosOriginais);
        assertThat(eventos(c, recurso, id, intermediario.versoes().get(1).registradoEm())).hasSize(2);
        evidenciar("prova-tardia-" + recurso, mapa("original", original, "prova", prova,
                "intermediario", intermediario, "finalizado", finalizado, "eventos", historia));
    }

    private Cenario semOrigemNominal() {
        Cenario c = criarCenarioPersistido();
        emTransacao(() -> {
            assertThat(em.createQuery("select count(e) from EventoVinculoContratual e where e.vinculo.id=:v", Long.class)
                    .setParameter("v", c.vinculoA.getId()).getSingleResult()).isZero();
            assertThat(em.createQuery("select count(p) from ProvaNomePessoaHistorico p where p.pessoa.id=:p", Long.class)
                    .setParameter("p", c.pessoaA.getId()).getSingleResult()).isZero();
            return null;
        });
        assertThat(c.pessoaA.getNome()).isNotBlank();
        return c;
    }

    private JsonNode registrarProva(Cenario c, String nome, LocalDate inicio, LocalDate fim) throws Exception {
        long pessoaVersao = consultar(c.localA, "/api/pessoas/" + c.pessoaA.getId(), "Versão corrente da Pessoa")
                .path("version").asLong();
        long vinculoVersao = consultar(c.localA, "/api/vinculos-contratuais/" + c.vinculoA.getId(), "Versão corrente do Vínculo")
                .path("version").asLong();
        Map<String,Object> req = mapa("vinculoId", c.vinculoA.getId(), "dataInicioEfetiva", inicio,
                "dataFimEfetiva", fim, "nomeComprovado", nome,
                "referenciaSei", "SEI-SINTETICO-C5-NOME-PERIODO-" + UUID.randomUUID(),
                "motivo", "Fonte sintética explícita comprova somente o nome e intervalo declarados neste ensaio",
                "autorizacao", "ATO-HTTP", "versaoPessoaEsperada", pessoaVersao,
                "versaoVinculoEsperada", vinculoVersao, "chaveIdempotencia", UUID.randomUUID().toString());
        String rota = "/api/pessoas/" + c.pessoaA.getId() + "/provas-nome";
        JsonNode previa = enviar(c.localA, rota + "/previa", req, 200, "Prévia legítima da fonte nominal sintética");
        req.put("confirmarImpacto", true); req.put("tokenPrevia", previa.path("tokenPrevia").asText());
        return enviar(c.localA, rota, req, 201, "Incorporação tardia por comando canônico, sem reescrever origem");
    }

    private JsonNode nomeHistorico(Cenario c, LocalDate data, Instant k) throws Exception {
        var req = get("/api/pessoas/" + c.pessoaA.getId() + "/nome-historico")
                .param("vinculoId", c.vinculoA.getId().toString()).param("data", data.toString());
        if (k != null) req.param("conhecidoAte", k.toString());
        return dados(executar(c.localA, req, 200, "Prova nominal da mesma Pessoa/Vínculo em D e K explícitos"));
    }

    private Map<String,Object> pedido(Cenario c, String recurso, LocalDate inicio, LocalDate fim) {
        return recurso.equals("excecoes")
                ? mapa("empregadoId", c.empregadoA.getId(), "dataInicio", inicio, "dataFim", fim,
                        "tipo", "FOLGA_REMUNERADA", "observacao", "Exceção sintética para análise nominal",
                        "justificativa", "Fonte manual sintética de exceção para teste do intervalo nominal")
                : mapa("empregadoId", c.empregadoA.getId(), "dataReferencia", inicio, "tipo", "RT", "minutos", 30,
                        "quantidade", 1, "justificativa", "Fonte manual sintética adicional para teste nominal");
    }
    private Map<String,Object> somenteData(String recurso, LocalDate dia) {
        return recurso.equals("excecoes")
                ? mapa("dataInicio", dia, "dataFim", dia, "justificativa", "Retificação explícita somente da data da exceção sintética")
                : mapa("dataReferencia", dia, "justificativa", "Retificação explícita somente da data do adicional sintético");
    }
    private JsonNode atualizar(Cenario c, String recurso, long id, Map<String,Object> req) throws Exception {
        return dados(executar(c.localA, put(rota(recurso) + "/" + id).contentType(MediaType.APPLICATION_JSON)
                .content(json(req)), 200, "Retificação autorizada preserva versões e reavalia o recorte quando alterado"));
    }
    private JsonNode eventos(Cenario c, String recurso, long id, Instant k) throws Exception {
        var req = get(rota(recurso) + "/" + id + "/eventos");
        if (k != null) req.param("conhecidoAte", k.toString());
        return dados(executar(c.localA, req, 200, "Cadeia manual preservada e verificável no corte de conhecimento"));
    }
    private String rota(String recurso) { return "/api/frequencias-" + recurso; }
    private Estado estado(String recurso, long id) {
        return emTransacao(() -> {
            em.clear();
            boolean excecao = recurso.equals("excecoes");
            FrequenciaExcecao e = excecao ? em.find(FrequenciaExcecao.class, id) : null;
            FrequenciaAdicional a = excecao ? null : em.find(FrequenciaAdicional.class, id);
            List<Versao> vs = em.createQuery("select v from VersaoComplementoFrequencia v "
                            + "where v.tipoComplemento=:tipo and v.complementoId=:id order by v.numeroVersao", VersaoComplementoFrequencia.class)
                    .setParameter("tipo", excecao ? "EXCECAO" : "ADICIONAL").setParameter("id", id).getResultList().stream()
                    .map(v -> new Versao(v.getId(), v.getNumeroVersao(), v.getTipoOperacao(), v.getDataEfetiva(),
                            v.getRegistradoEm(), v.getUsuarioId(), v.getUsuarioUsername(), v.getJustificativa(),
                            v.getMotivoTecnico(), v.getVersaoAnteriorId(), v.getFotografiaJson(), v.getContextosJson(),
                            v.getHashAnterior(), v.getHashVersao())).toList();
            return new Estado(id, excecao ? e.getEmpregado().getId() : a.getEmpregado().getId(),
                    excecao ? e.getEmpregadoNomeFotografia() : a.getEmpregadoNomeFotografia(),
                    excecao ? e.getDataInicio() : a.getDataReferencia(), excecao ? e.getDataFim() : a.getDataReferencia(), vs);
        });
    }
    private String nomeNaVersao(Versao versao) throws Exception {
        return mapper.readTree(versao.fotografiaJson()).path("empregadoNome").asText(null);
    }
    private void conferirPredecessora(Versao anterior, Versao nova) {
        assertThat(nova.anteriorId()).isEqualTo(anterior.id());
        assertThat(nova.hashAnterior()).isEqualTo(anterior.hash());
        assertThat(nova.numero()).isEqualTo(anterior.numero() + 1);
        assertThat(nova.operacao()).isEqualTo("RETIFICACAO");
    }
    private void evidenciar(String caso, Map<String,Object> conteudo) throws Exception {
        Path p = Path.of("target/auditoria-http/candidato5-complemento-nome-" + caso + ".json");
        Files.createDirectories(p.getParent());
        conteudo.put("autoria", "/root/revisao_final_pessoas");
        conteudo.put("fontes", "SEIs e cenários sintéticos explícitos; incorporação nominal por HTTP canônico");
        conteudo.put("transporte", "MockMvc/dispatcher, MySQL8.0.45 e Redis reais; não TCP do JAR");
        Files.writeString(p, mapper.writerWithDefaultPrettyPrinter().writeValueAsString(conteudo));
    }
    private record Estado(long id, long empregadoId, String nome, LocalDate inicio, LocalDate fim, List<Versao> versoes) { }
    private record Versao(long id, int numero, String operacao, LocalDate dataEfetiva, Instant registradoEm,
                           Long usuarioId, String usuarioUsername, String justificativa, String motivoTecnico,
                           Long anteriorId, String fotografiaJson, String contextosJson, String hashAnterior, String hash) { }
}
