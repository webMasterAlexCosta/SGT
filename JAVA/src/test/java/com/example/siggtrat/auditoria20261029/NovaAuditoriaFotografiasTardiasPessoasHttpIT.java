package com.example.siggtrat.auditoria20261029;

import com.example.siggtrat.entidade.EventoVinculoContratual;
import com.example.siggtrat.entidade.Pessoa;
import com.example.siggtrat.entidade.VinculoContratual;
import com.example.siggtrat.utilitario.RelogioSistema;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import javax.sql.DataSource;
import java.time.LocalDate;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;

/**
 * Proposta RED independente, ainda não executada pelo revisor.
 * Usa a cadeia MVC/segurança/serviços/JPA real do suporte do projeto.
 * MockMvc não substitui a prova separada por TCP de um JAR em execução.
 * Não usa mock de persistência, não altera schema e não fabrica foto histórica.
 * O suporte só substitui o agendador; este teste exige PostgreSQL 18.6 e Redis reais.
 */
@Transactional(propagation = Propagation.NOT_SUPPORTED)
@TestPropertySource(properties = {
        "spring.cache.type=redis",
        "spring.sql.init.mode=never",
        "app.cache.redis.http-get.enabled=true"
})
class NovaAuditoriaFotografiasTardiasPessoasHttpIT extends ContratosHttpBase {
    private static final String V = "/api/vinculos-contratuais";
    @Autowired private DataSource dataSource;
    @Autowired private RedisConnectionFactory redis;
    @Autowired private Environment ambiente;

    @BeforeEach
    void exigirInfraestruturaRealEUnicoGestorDeSchema() throws Exception {
        try (var conexao = dataSource.getConnection()) {
            assertThat(conexao.getMetaData().getDatabaseProductName()).isEqualTo("PostgreSQL");
            assertThat(conexao.getMetaData().getDatabaseProductVersion()).matches("18\\.6(?: \\(Debian [^()]+\\))?");
        }
        assertThat(ambiente.getProperty("spring.jpa.hibernate.ddl-auto")).isEqualTo("update");
        assertThat(ambiente.getProperty("spring.sql.init.mode")).isEqualTo("never");
        assertThat(ambiente.getProperty("spring.cache.type")).isEqualTo("redis");
        try (var conexao = redis.getConnection()) {
            assertThat(conexao.ping()).isEqualTo("PONG");
        }
    }

    @Test
    void novoVinculoRetroativoDaMesmaPessoaNaoFabricaNomeDeEpocaComNomeAtual() throws Exception {
        Preparado p = preparar();
        String nomeOriginal = "Nome da fonte original " + p.n().id();
        String nomeAtual = "Nome legal conhecido agora " + p.n().id();
        LocalDate inicioOriginal = p.n().inicio().plusDays(1);
        LocalDate inicioTardio = p.n().inicio().plusDays(10);
        JsonNode primeiro = criarPessoa(p, 1, nomeOriginal, inicioOriginal, inicioOriginal.plusDays(2));
        long pessoaId = primeiro.path("pessoaId").asLong();
        long primeiroId = primeiro.path("id").asLong();
        JsonNode eventosOriginais = consultar(p.c().localA, V + "/" + primeiroId + "/eventos",
                "Fonte nominal já documentada antes da correção do cadastro atual");
        String hashOriginal = eventosOriginais.get(0).path("hashEvento").asText();
        assertThat(eventosOriginais.get(0).path("pessoaNomeFotografia").asText()).isEqualTo(nomeOriginal);

        enviar(p.c().localA, "/api/pessoas/" + pessoaId + "/correcoes-identidade", mapa(
                "tipo", "NOME", "novoValor", nomeAtual,
                "motivo", "Mudança legal atual; esta ação não documenta o nome do vínculo passado",
                "evidencia", "SEI-SINTETICO-NOME-ATUAL", "autorizacao", "ATO-SINTETICO-NOME-ATUAL",
                "impactosEncontrados", "Preservar fotografias e documentos anteriores"), 200,
                "Correção nominal muda apenas o cadastro atual e conserva identidade canônica");
        EstadoPessoa antes = estadoPessoa(pessoaId);

        // pessoaId não contém nome de época, data do nome ou fonte nominal histórica.
        var resposta = mvc.perform(autenticar(post(V).contentType(MediaType.APPLICATION_JSON).content(json(mapa(
                "pessoaId", pessoaId, "contratoId", p.n().id(), "composicaoId", p.n().composicao1(),
                "dataInicio", inicioTardio, "dataFim", inicioTardio.plusDays(2)))), p.c().localA)).andReturn();
        int status = resposta.getResponse().getStatus();
        assertThat(status).as("Sem fonte, é válido expor desconhecimento ou recusar atomicamente")
                .isIn(201, 409);
        if (status == 409) {
            assertThat(estadoPessoa(pessoaId)).as("Recusa não cria vínculo, faixa, evento ou reserva")
                    .isEqualTo(antes);
        } else {
            long novoId = dados(resposta).path("id").asLong();
            emTransacao(() -> {
                var fontes = em.createQuery("select e from EventoVinculoContratual e where e.vinculo.id=:v",
                        EventoVinculoContratual.class).setParameter("v", novoId).getResultList();
                assertThat(fontes).hasSize(1);
                String fotografia = fontes.get(0).getPessoaNomeFotografia();
                assertThat(fotografia).as("COL010: pessoaId atual não prova o nome no período retroativo")
                        .isNotEqualTo(nomeAtual);
                assertThat(fotografia).as("A fonte do primeiro vínculo terminou antes do novo período; a lacuna deve permanecer desconhecida")
                        .isNull();
                return null;
            });
        }

        JsonNode preservado = consultar(p.c().localA, V + "/" + primeiroId + "/eventos",
                "Primeiro fato permanece íntegro após cadastro tardio ou recusa");
        assertThat(preservado.get(0).path("pessoaNomeFotografia").asText()).isEqualTo(nomeOriginal);
        assertThat(preservado.get(0).path("hashEvento").asText()).isEqualTo(hashOriginal);
        emTransacao(() -> {
            assertThat(em.find(Pessoa.class, pessoaId).getNome()).isEqualTo(nomeAtual);
            return null;
        });
    }

    @Test
    void desligamentoRetroativoNaoImportaDescricaoAtualDoMotivoRenomeado() throws Exception {
        Preparado p = preparar();
        LocalDate fim = p.n().inicio().plusDays(9);
        long primeiro = criarPessoa(p, 2, "Pessoa fonte de causa " + p.n().id(), p.n().inicio().plusDays(1), null)
                .path("id").asLong();
        long segundo = criarPessoa(p, 3, "Pessoa término tardio " + p.n().id(), p.n().inicio().plusDays(1), null)
                .path("id").asLong();
        String original = "Causa original " + p.n().id();
        String atual = "Causa renomeada agora " + p.n().id();
        long motivoId = enviar(p.c().localA, "/api/motivos-desligamento", mapa("descricao", original,
                "categoria", "DESLIGAMENTO_INDIVIDUAL", "ativo", true), 201,
                "Cadastro da causa própria selecionada por ID").path("id").asLong();
        documentarDisponibilidade(p, motivoId, p.n().inicio());
        documentarContextoMotivo(p, motivoId, original, fim);

        enviar(p.c().localA, V + "/" + primeiro + "/desligar", desligamento(fim, motivoId), 200,
                "Primeiro fato conserva a descrição da causa documentada para essa data");
        String hashPrimeiro = emTransacao(() -> em.createQuery(
                "select e from EventoVinculoContratual e where e.vinculo.id=:v order by e.id desc",
                EventoVinculoContratual.class).setParameter("v", primeiro).getResultList().get(0).getHashEvento());
        executar(p.c().localA, put("/api/motivos-desligamento/" + motivoId).contentType(MediaType.APPLICATION_JSON)
                .content(json(mapa("descricao", atual, "categoria", "DESLIGAMENTO_INDIVIDUAL", "ativo", true))),
                200, "Renomeação atual não comprova o novo texto na data do término anterior");
        long pessoaSegundo = emTransacao(() -> em.find(VinculoContratual.class, segundo).getPessoa().getId());
        EstadoPessoa antes = estadoPessoa(pessoaSegundo);

        var resposta = mvc.perform(autenticar(post(V + "/" + segundo + "/desligar")
                .contentType(MediaType.APPLICATION_JSON).content(json(desligamento(fim, motivoId))), p.c().localA))
                .andReturn();
        assertThat(resposta.getResponse().getStatus()).isIn(200, 409);
        if (resposta.getResponse().getStatus() == 409) {
            assertThat(estadoPessoa(pessoaSegundo)).isEqualTo(antes);
            emTransacao(() -> { assertThat(em.find(VinculoContratual.class, segundo).getDataFim()).isNull(); return null; });
        } else {
            emTransacao(() -> {
                VinculoContratual v = em.find(VinculoContratual.class, segundo);
                String descricao = v.getMotivoDesligamentoDescricaoFotografia();
                assertThat(descricao).as("CAT004/DES004: disponibilidade ativa não prova a descrição corrente no passado")
                        .isNotEqualTo(atual);
                assertThat(descricao == null || descricao.isBlank() || descricao.equals(original)).isTrue();
                var e = em.createQuery("select e from EventoVinculoContratual e where e.vinculo.id=:v order by e.id desc",
                        EventoVinculoContratual.class).setParameter("v", segundo).getResultList().get(0);
                assertThat(e.getMotivoDesligamentoDescricaoFotografia()).isEqualTo(descricao);
                return null;
            });
        }
        emTransacao(() -> {
            VinculoContratual v = em.find(VinculoContratual.class, primeiro);
            assertThat(v.getMotivoDesligamentoDescricaoFotografia()).isEqualTo(original);
            var e = em.createQuery("select e from EventoVinculoContratual e where e.vinculo.id=:v order by e.id desc",
                    EventoVinculoContratual.class).setParameter("v", primeiro).getResultList().get(0);
            assertThat(e.getMotivoDesligamentoDescricaoFotografia()).isEqualTo(original);
            assertThat(e.getHashEvento()).isEqualTo(hashPrimeiro);
            return null;
        });
    }

    private Preparado preparar() {
        Cenario c = criarCenarioPersistido();
        NovoContrato n = emTransacao(() -> {
            try { return publicado(c, 20, null, false); }
            catch (Exception ex) { throw new IllegalStateException(ex); }
        });
        return new Preparado(c, n);
    }

    private JsonNode criarPessoa(Preparado p, int ordinal, String nome, LocalDate inicio, LocalDate fim) throws Exception {
        int semente = 760000000 + Math.toIntExact(p.n().id() % 1000000) * 10 + ordinal;
        return enviar(p.c().localA, V, mapa("pessoa", mapa("nome", nome, "cpf", cpf(semente)),
                "contratoId", p.n().id(), "composicaoId", p.n().composicao1(), "dataInicio", inicio, "dataFim", fim),
                201, "Fixture canônica registra a fonte nominal explicitamente informada pelo cenário");
    }

    private Map<String, Object> desligamento(LocalDate fim, long motivoId) {
        return mapa("dataFim", fim, "motivoDesligamentoId", motivoId,
                "justificativa", "Registro tardio do término; sem prova de renomeação pretérita da causa",
                "evidencia", "SEI-SINTETICO-TERMINO", "autorizacao", "ATO-SINTETICO-TERMINO");
    }

    /** Fonte contextual opt-in: disponibilidade sozinha não informa descrição/categoria (CAT-004). */
    private void documentarContextoMotivo(Preparado p, long motivoId, String descricao, LocalDate data) throws Exception {
        conceder(p.c(), p.n().id(), "INCORPORACAO_PROVA_HISTORICA");
        String rota = "/api/motivos-desligamento/" + motivoId + "/provas-historicas";
        Map<String, Object> req = mapa("contratoId", p.n().id(), "dataInicioEfetiva", data, "dataFimEfetiva", data,
                "descricaoComprovada", descricao, "categoriaComprovada", "DESLIGAMENTO_INDIVIDUAL",
                "referenciaSei", "SEI-SINTETICO-CONTEXTO-CAUSA", "motivo", "Fonte sintética explicita descrição/categoria no dia do término",
                "autorizacao", "ATO-SINTETICO-CONTEXTO-CAUSA", "chaveIdempotencia", UUID.randomUUID().toString(),
                "versaoMotivoEsperada", consultar(p.c().localA, "/api/motivos-desligamento/" + motivoId, "Versão do motivo").path("version").asLong(),
                "versaoContratoEsperada", consultar(p.c().localA, base(p.n().id()), "Versão do Contrato").path("version").asLong());
        JsonNode previa = enviar(p.c().localA, rota + "/previa", req, 200, "Prévia da fonte contextual explícita do cenário");
        req.put("confirmarImpacto", true); req.put("tokenPrevia", previa.path("tokenPrevia").asText());
        enviar(p.c().localA, rota, req, 201, "Prova contextual sintética sem alterar disponibilidade nem mestre");
    }

    private void documentarDisponibilidade(Preparado p, long motivoId, LocalDate inicio) throws Exception {
        String rota = "/api/catalogos-temporais/MOTIVO_DESLIGAMENTO/" + motivoId + "/transicoes";
        long versao = consultar(p.c().localA, "/api/motivos-desligamento/" + motivoId,
                "Versão atual antes da incorporação da disponibilidade documental").path("version").asLong();
        Map<String, Object> req = mapa("dataEfetiva", inicio, "ativo", true, "versaoEsperada", versao,
                "motivo", "Fonte sintética comprova disponibilidade desde o início do cenário",
                "evidencia", "SEI-SINTETICO-DISPONIBILIDADE", "autorizacao", "ATO-SINTETICO-DISPONIBILIDADE",
                "chaveIdempotencia", UUID.randomUUID().toString());
        JsonNode previa = enviar(p.c().localA, rota + "/previa", req, 200, "Prévia de prova tardia escopada");
        req.put("confirmarImpacto", true); req.put("tokenPrevia", previa.path("tokenPrevia").asText());
        enviar(p.c().localA, rota, req, 200, "Incorporação documental sem inferir o período anterior à fonte");
    }

    private EstadoPessoa estadoPessoa(long pessoaId) {
        return emTransacao(() -> new EstadoPessoa(
                em.createQuery("select count(v) from VinculoContratual v where v.pessoa.id=:p", Long.class)
                        .setParameter("p", pessoaId).getSingleResult(),
                em.createQuery("select count(a) from AlocacaoVinculo a where a.vinculo.pessoa.id=:p", Long.class)
                        .setParameter("p", pessoaId).getSingleResult(),
                em.createQuery("select count(e) from EventoVinculoContratual e where e.vinculo.pessoa.id=:p", Long.class)
                        .setParameter("p", pessoaId).getSingleResult(),
                em.createQuery("select count(e) from EventoAlocacaoVinculo e where e.vinculo.pessoa.id=:p", Long.class)
                        .setParameter("p", pessoaId).getSingleResult(),
                em.createQuery("select count(r) from ReservaMatriculaContrato r where r.vinculo.pessoa.id=:p", Long.class)
                        .setParameter("p", pessoaId).getSingleResult()));
    }

    private record Preparado(Cenario c, NovoContrato n) {}
    private record EstadoPessoa(long vinculos, long alocacoes, long eventosVinculo, long eventosAlocacao, long reservas) {}
}
