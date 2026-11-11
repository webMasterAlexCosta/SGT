package com.example.siggtrat.auditoria20261029;

import com.example.siggtrat.entidade.Contrato;
import com.example.siggtrat.utilitario.RelogioSistema;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.AbstractMockHttpServletRequestBuilder;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;

/** AUD-04/CON001-004: cada requisição confirma sua própria transação PostgreSQL. */
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class ConclusaoAud04IdentidadeContratualHttpIT extends ContratosHttpBase {
    @Test
    void cadastroNaoReutilizaIdentificadorAntigoEPesquisaHistoricaRespeitaEscopo() throws Exception {
        Preparado f = corrigido();
        long contratosAntes = quantidadeContratos(f.c());
        enviar(f.c().localA, "/api/contratos", rascunhoComIdentidade(f.c(),
                "  " + f.numeroAnterior().toLowerCase(Locale.ROOT) + "  ", f.anoAnterior()), 409,
                "AUD04 identidade publicada permanece reservada após correção, inclusive caixa e espaços externos");
        assertThat(quantidadeContratos(f.c())).isEqualTo(contratosAntes);
        assertThat(quantidadeComIdentificadorAtual(f.numeroAnterior(), f.anoAnterior())).isZero();
        JsonNode historico = consultar(f.c().localA, historico(f),
                "CON003 identificador anterior encontra a correção do contrato original");
        assertThat(historico).hasSize(1);
        assertThat(historico.get(0).path("contratoId").asLong()).isEqualTo(f.n().id());
        assertThat(historico.get(0).path("numeroAnterior").asText()).isEqualTo(f.numeroAnterior());
        assertThat(consultar(f.c().localB, historico(f),
                "PER001 pesquisa de identidade histórica não expõe contrato de outra Gestão")).isEmpty();
    }

    @Test
    void rascunhoNaoAssumeIdentificadorHistoricoDeOutroContrato() throws Exception {
        Preparado f = corrigido();
        String numeroRascunho = "AUD04-RASC-" + UUID.randomUUID().toString().substring(0, 8);
        long rascunhoId = enviar(f.c().localA, "/api/contratos",
                rascunhoComIdentidade(f.c(), numeroRascunho, f.anoAnterior()), 201,
                "CON002 rascunho recebe identidade ainda livre").path("id").asLong();
        String identidadePersistidaAntes = numeroAtual(rascunhoId);
        executar(f.c().localA, put(base(rascunhoId)).contentType(MediaType.APPLICATION_JSON)
                .content(json(rascunhoComIdentidade(f.c(), f.numeroAnterior(), f.anoAnterior()))), 409,
                "AUD04 atualização do rascunho respeita a reserva histórica de outro contrato");
        assertThat(numeroAtual(rascunhoId)).isEqualTo(identidadePersistidaAntes);
        assertThat(numeroAtual(f.n().id())).isEqualTo(f.numeroNovo());
        assertThat(quantidadeComIdentificadorAtual(f.numeroAnterior(), f.anoAnterior())).isZero();
    }

    @Test
    void correcaoExcepcionalNaoAssumeIdentificadorHistoricoDeOutroContratoNemPublicaFatosParciais() throws Exception {
        Preparado f = corrigido();
        NovoContrato outro = publicar(f.c());
        String numeroOutro = numeroAtual(outro.id());
        conceder(f.c(), outro.id(), "CORRECAO_IDENTIFICADOR_CONTRATO");
        long eventosAntes = contar(outro.id(), "EventoCicloVidaContrato");
        long versoesAntes = contar(outro.id(), "VersaoContratoNegocial");
        enviar(f.c().localA, base(outro.id()) + "/correcoes-identificador",
                correcao(f.numeroAnterior(), f.anoAnterior(), RelogioSistema.hoje()), 409,
                "AUD04 autorização excepcional não concede identidade reservada a outro contrato");
        assertThat(numeroAtual(outro.id())).isEqualTo(numeroOutro);
        assertThat(contar(outro.id(), "CorrecaoIdentificadorContrato")).isZero();
        assertThat(contar(outro.id(), "EventoCicloVidaContrato")).isEqualTo(eventosAntes);
        assertThat(contar(outro.id(), "VersaoContratoNegocial")).isEqualTo(versoesAntes);
        assertThat(contar(f.n().id(), "CorrecaoIdentificadorContrato")).isEqualTo(1L);
    }

    @Test
    @Timeout(value = 45, unit = TimeUnit.SECONDS)
    void doisCadastrosConcorrentesRejeitamIdentificadorHistoricoSemCriarContrato() throws Exception {
        Preparado f = corrigido();
        long contratosAntes = quantidadeContratos(f.c());
        var a = autenticar(post("/api/contratos").contentType(MediaType.APPLICATION_JSON)
                .content(json(rascunhoComIdentidade(f.c(), f.numeroAnterior(), f.anoAnterior()))), f.c().localA);
        var b = autenticar(post("/api/contratos").contentType(MediaType.APPLICATION_JSON)
                .content(json(rascunhoComIdentidade(f.c(), f.numeroAnterior().toLowerCase(Locale.ROOT), f.anoAnterior()))),
                f.c().localA);
        List<MvcResult> respostas = simultaneos(a, b);
        assertThat(respostas.stream().map(r -> r.getResponse().getStatus()).toList())
                .as("ambas as transações disputam uma identidade historicamente indisponível")
                .containsExactly(409, 409);
        assertThat(quantidadeContratos(f.c())).isEqualTo(contratosAntes);
        assertThat(quantidadeComIdentificadorAtual(f.numeroAnterior(), f.anoAnterior())).isZero();
        assertThat(contar(f.n().id(), "CorrecaoIdentificadorContrato")).isEqualTo(1L);
    }

    @Test
    @Timeout(value = 45, unit = TimeUnit.SECONDS)
    void correcoesConcorrentesParaIdentidadeLivreConfirmamUmaSoTransacao() throws Exception {
        Cenario c = criarCenarioPersistido();
        NovoContrato a = publicar(c), b = publicar(c);
        String anteriorA = numeroAtual(a.id()), anteriorB = numeroAtual(b.id());
        conceder(c, a.id(), "CORRECAO_IDENTIFICADOR_CONTRATO");
        conceder(c, b.id(), "CORRECAO_IDENTIFICADOR_CONTRATO");
        String disputa = "AUD04-CONCORRENTE-" + a.id();
        int ano = RelogioSistema.hoje().getYear();
        var reqA = autenticar(post(base(a.id()) + "/correcoes-identificador").contentType(MediaType.APPLICATION_JSON)
                .content(json(correcao(disputa, ano, RelogioSistema.hoje()))), c.localA);
        var reqB = autenticar(post(base(b.id()) + "/correcoes-identificador").contentType(MediaType.APPLICATION_JSON)
                .content(json(correcao(disputa.toLowerCase(Locale.ROOT), ano, RelogioSistema.hoje()))), c.localA);
        List<MvcResult> respostas = simultaneos(reqA, reqB);
        assertThat(respostas.stream().map(r -> r.getResponse().getStatus()).toList())
                .containsExactlyInAnyOrder(201, 409);
        long vencedor = respostas.get(0).getResponse().getStatus() == 201 ? a.id() : b.id();
        long perdedor = vencedor == a.id() ? b.id() : a.id();
        assertThat(numeroAtual(perdedor)).isEqualTo(perdedor == a.id() ? anteriorA : anteriorB);
        assertThat(quantidadeComIdentificadorAtual(disputa, ano)).isEqualTo(1L);
        assertThat(contar(vencedor, "CorrecaoIdentificadorContrato")).isEqualTo(1L);
        assertThat(contar(perdedor, "CorrecaoIdentificadorContrato")).isZero();
        assertThat(contar(perdedor, "EventoCicloVidaContrato")).isEqualTo(1L);
        assertThat(contar(perdedor, "VersaoContratoNegocial")).isEqualTo(1L);
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void rascunhoNuncaPublicadoLiberaIdentidadeAoDescartarOuAlterar(boolean descartar) throws Exception {
        Cenario c = criarCenarioPersistido();
        String numero = "AUD04-LIVRE-" + UUID.randomUUID().toString().substring(0, 8);
        int ano = RelogioSistema.hoje().getYear();
        long id = enviar(c.localA, "/api/contratos", rascunhoComIdentidade(c, numero, ano), 201,
                "CON002 rascunho ainda sem fato publicado").path("id").asLong();
        if (descartar) {
            executar(c.localA, delete(base(id)), 204, "CON004 descarte permitido de rascunho sem história");
        } else {
            executar(c.localA, put(base(id)).contentType(MediaType.APPLICATION_JSON)
                    .content(json(rascunhoComIdentidade(c, numero + "-NOVO", ano))), 200,
                    "CON002 correção livre anterior à publicação");
        }
        long novo = enviar(c.localA, "/api/contratos", rascunhoComIdentidade(c, numero, ano), 201,
                "CON004 identidade jamais publicada volta a ficar livre").path("id").asLong();
        assertThat(novo).isNotEqualTo(id);
        assertThat(quantidadeComIdentificadorAtual(numero, ano)).isEqualTo(1L);
    }

    @Test
    void identificadorHistoricoPodeVoltarAoMesmoContratoComNovaCorrecaoAuditada() throws Exception {
        Preparado f = corrigido();
        java.time.Instant conhecidoAntes = java.time.Instant.now();
        LocalDate dataRetorno = RelogioSistema.hoje().plusDays(1);
        JsonNode retorno = enviar(f.c().localA, base(f.n().id()) + "/correcoes-identificador",
                correcao(f.numeroAnterior(), f.anoAnterior(), dataRetorno), 201,
                "CON003 reserva histórica pertence ao contrato e não impede nova correção documentada do mesmo titular");
        // TMP003/004/005: registro conhecido hoje não antecipa efeito de amanhã.
        assertThat(numeroAtual(f.n().id())).isEqualTo(f.numeroNovo());
        assertThat(retorno.path("numeroAnterior").asText()).isEqualTo(f.numeroNovo());
        assertThat(retorno.path("numeroNovo").asText()).isEqualTo(f.numeroAnterior());
        assertThat(retorno.path("dataEfetiva").asText()).isEqualTo(dataRetorno.toString());
        assertThat(consultar(f.c().localA, base(f.n().id()) + "/posicao-temporal?data=" + RelogioSistema.hoje(),
                "CON003/TMP005 a projeção de hoje conserva o ato que já produz efeito")
                .path("versaoContratual").path("numeroContrato").asText()).isEqualTo(f.numeroNovo());
        assertThat(consultar(f.c().localA, base(f.n().id()) + "/posicao-temporal?data=" + dataRetorno
                        + "&conhecidoAte=" + conhecidoAntes,
                "TMP004/005 amanhã sob K anterior ainda não conhece o retorno documentado depois")
                .path("versaoContratual").path("numeroContrato").asText()).isEqualTo(f.numeroNovo());
        assertThat(consultar(f.c().localA, base(f.n().id()) + "/posicao-temporal?data=" + dataRetorno
                        + "&conhecidoAte=" + retorno.path("registradoEm").asText(),
                "CON003 retorno ao identificador original vale em sua data efetiva, com K que conhece o ato")
                .path("versaoContratual").path("numeroContrato").asText()).isEqualTo(f.numeroAnterior());
        emTransacao(() -> {
            assertThat(em.createQuery("select r.numero from ReservaIdentificadorContrato r "
                            + "where r.contrato.id=:id and r.ano=:ano", String.class)
                    .setParameter("id", f.n().id()).setParameter("ano", f.anoAnterior()).getResultList())
                    .as("As duas identidades permanecem reservadas ao titular, além da projeção atual")
                    .contains(f.numeroAnterior(), f.numeroNovo());
            return null;
        });
        assertThat(contar(f.n().id(), "CorrecaoIdentificadorContrato")).isEqualTo(2L);
        assertThat(consultar(f.c().localA, historico(f), "CON003 as duas correções continuam pesquisáveis")).hasSize(2);
        enviar(f.c().localA, "/api/contratos", rascunhoComIdentidade(f.c(), f.numeroNovo(), f.anoAnterior()), 409,
                "AUD04 identidade intermediária publicada também permanece reservada");
    }

    private Preparado corrigido() throws Exception {
        Cenario c = criarCenarioPersistido();
        NovoContrato n = publicar(c);
        String anterior = numeroAtual(n.id());
        int ano = n.inicio().getYear();
        String novo = "AUD04-CORRIGIDO-" + n.id();
        conceder(c, n.id(), "CORRECAO_IDENTIFICADOR_CONTRATO");
        enviar(c.localA, base(n.id()) + "/correcoes-identificador", correcao(novo, ano, RelogioSistema.hoje()), 201,
                "CON003 correção efetiva de contrato publicado preserva identidade anterior");
        return new Preparado(c, n, anterior, ano, novo);
    }

    private NovoContrato publicar(Cenario c) {
        return emTransacao(() -> {
            try { return publicado(c, 5, null, false); }
            catch (Exception e) { throw new IllegalStateException(e); }
        });
    }

    private Map<String, Object> rascunhoComIdentidade(Cenario c, String numero, int ano) {
        Map<String, Object> req = rascunho(c, numero, 5, RelogioSistema.hoje(), RelogioSistema.hoje().plusDays(30));
        req.put("anoContrato", ano);
        return req;
    }

    private Map<String, Object> correcao(String numero, int ano, LocalDate data) {
        return mapa("novoNumero", numero, "novoAno", ano, "dataEfetiva", data, "dataAto", RelogioSistema.hoje(),
                "motivo", "Correção documental para prova de reserva histórica", "referenciaSei", "SEI-AUD04",
                "autorizacaoAdministrativa", "ATO-HTTP", "chaveIdempotencia", "AUD04-" + UUID.randomUUID());
    }

    private String numeroAtual(long contrato) {
        return emTransacao(() -> em.find(Contrato.class, contrato).getNumeroContrato());
    }

    private String historico(Preparado f) {
        return "/api/contratos/historico-identificador?numero=" + f.numeroAnterior() + "&ano=" + f.anoAnterior();
    }

    private long quantidadeContratos(Cenario c) {
        return emTransacao(() -> em.createQuery("select count(c) from Contrato c where c.gestaoLocal.id=:gestao", Long.class)
                .setParameter("gestao", c.gestaoA.getId()).getSingleResult());
    }

    private long quantidadeComIdentificadorAtual(String numero, int ano) {
        return emTransacao(() -> em.createQuery("select count(c) from Contrato c "
                        + "where lower(c.numeroContrato)=lower(:numero) and c.anoContrato=:ano", Long.class)
                .setParameter("numero", numero).setParameter("ano", ano).getSingleResult());
    }

    private long contar(long contrato, String entidade) {
        return emTransacao(() -> em.createQuery("select count(x) from " + entidade + " x where x.contrato.id=:id", Long.class)
                .setParameter("id", contrato).getSingleResult());
    }

    private List<MvcResult> simultaneos(AbstractMockHttpServletRequestBuilder<?> a, AbstractMockHttpServletRequestBuilder<?> b) throws Exception {
        CountDownLatch largada = new CountDownLatch(1);
        ExecutorService pool = Executors.newFixedThreadPool(2);
        try {
            Future<MvcResult> fa = pool.submit(() -> { largada.await(); return mvc.perform(a).andReturn(); });
            Future<MvcResult> fb = pool.submit(() -> { largada.await(); return mvc.perform(b).andReturn(); });
            largada.countDown();
            return List.of(fa.get(35, TimeUnit.SECONDS), fb.get(35, TimeUnit.SECONDS));
        } finally { largada.countDown(); pool.shutdownNow(); }
    }

    private record Preparado(Cenario c, NovoContrato n, String numeroAnterior, int anoAnterior, String numeroNovo) {}
}
