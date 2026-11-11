package com.example.siggtrat.auditoria20261029;

import com.example.siggtrat.entidade.Cargo;
import com.example.siggtrat.entidade.Empregado;
import com.example.siggtrat.entidade.Jornada;
import com.example.siggtrat.entidade.Lotacao;
import com.example.siggtrat.entidade.UsuarioAplicacao;
import com.example.siggtrat.entidade.VinculoContratual;
import com.example.siggtrat.utilitario.RelogioSistema;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * AUD-03: reprodução dos recortes de vínculos cadastrados após a transferência documentada.
 * Reutilizam o contexto PostgreSQL real do projeto. Não possuem DDL nem mocks de dados.
 * Cada consulta emTransacao é posterior ao commit do comando HTTP, porque a
 * classe não abre transação de teste envolvendo as requisições.
 */
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class ConclusaoAud03VinculosTransferenciaProgramadaHttpIT extends ContratosHttpBase {
    private static final String V = "/api/vinculos-contratuais";

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void cadastroPosteriorAoAgendamentoAplicaMapaNasDuasFaixasSemAnteciparProjecao(boolean futura) throws Exception {
        Preparado f = preparar(5, futura);
        renomearDestinoDepoisDoMapa(f);
        LocalDate inicio = RelogioSistema.hoje().minusDays(10);
        LocalDate fim = f.dataTransferencia().plusDays(8);
        JsonNode novo = enviar(executorAtual(f), V, cadastro(f, 1, inicio, fim), 201,
                "AUD03 cadastro posterior ao ato atravessa a transferência já documentada");
        long id = novo.path("id").asLong();
        long pessoaId = novo.path("pessoaId").asLong();

        assertRecorte(f, id, f.dataTransferencia().minusDays(1), false);
        assertRecorte(f, id, f.dataTransferencia(), true);
        assertRecorte(f, id, fim, true);
        JsonNode destino = posicao(f, id, f.dataTransferencia(), null);
        assertThat(destino.path("lotacaoNome").asText()).isEqualTo(f.nomeDestinoFotografado());
        assertThat(destino.path("cargoNome").asText()).isEqualTo(f.cargoDestinoFotografado());
        assertThat(destino.path("jornadaDescricao").asText()).isEqualTo(f.jornadaDestinoFotografada());

        List<Object[]> faixas = faixasAtivas(id);
        assertThat(faixas).hasSize(2);
        assertThat(faixas.get(0)).containsExactly(inicio, f.dataTransferencia().minusDays(1),
                f.c().lotacaoA.getId(), f.c().jornadaA.getId(), f.c().cargoA.getId());
        assertThat(faixas.get(1)).containsExactly(f.dataTransferencia(), fim,
                f.c().lotacaoB.getId(), f.c().jornadaB.getId(), f.c().cargoB.getId());
        assertThat(contarNoContrato(f, "VinculoContratual")).isEqualTo(1L);
        assertThat(contarNoContrato(f, "TransferenciaGestaoContrato")).isEqualTo(1L);

        List<JsonNode> derivados = new java.util.ArrayList<>();
        consultar(executorAtual(f), V + "/" + id + "/alocacoes/eventos",
                "AUD03 transferência aplicada conserva fonte, data e autoria").forEach(e -> {
            if ("TRANSFERENCIA_GESTAO".equals(e.path("tipoEvento").asText())) derivados.add(e);
        });
        assertThat(derivados).hasSize(1);
        assertThat(derivados.get(0).path("transferenciaGestaoId").asLong()).isEqualTo(f.transferenciaId());
        assertThat(derivados.get(0).path("dataEfetiva").asText()).isEqualTo(f.dataTransferencia().toString());
        assertThat(derivados.get(0).path("hashEvento").asText()).hasSize(64);

        Object[] projecaoAtual = emTransacao(() -> em.createQuery(
                        "select e.gestaoLocal.id, e.lotacao.id, e.jornada.id, e.cargo.id "
                                + "from Empregado e where e.pessoaCanonica.id=:pessoa", Object[].class)
                .setParameter("pessoa", pessoaId).getSingleResult());
        assertThat(projecaoAtual).containsExactly(
                futura ? f.c().gestaoA.getId() : f.c().gestaoB.getId(),
                futura ? f.c().lotacaoA.getId() : f.c().lotacaoB.getId(),
                futura ? f.c().jornadaA.getId() : f.c().jornadaB.getId(),
                futura ? f.c().cargoA.getId() : f.c().cargoB.getId());
    }

    @Test
    void capacidadeDoDestinoExigeConsentimentoEspecificoERollbackAntesDaInclusao() throws Exception {
        Preparado f = preparar(1, true);
        enviar(f.c().localA, V, cadastro(f, 2, f.dataTransferencia(), f.dataTransferencia().plusDays(12)), 201,
                "AUD03 ocupante programado consome a única vaga do destino");
        Map<String, Object> req = cadastro(f, 3, RelogioSistema.hoje(), f.dataTransferencia().plusDays(8));
        long vinculosAntes = contarNoContrato(f, "VinculoContratual");
        long alocacoesAntes = contarAlocacoes(f);
        long eventosAntes = contarEventosAlocacao(f);
        enviar(f.c().localA, V, req, 409,
                "AUD03 vaga livre em A não autoriza excedente na combinação B a partir da transferência");
        assertThat(contarPessoa(f, 3)).isZero();
        assertThat(contarNoContrato(f, "VinculoContratual")).isEqualTo(vinculosAntes);
        assertThat(contarAlocacoes(f)).isEqualTo(alocacoesAntes);
        assertThat(contarEventosAlocacao(f)).isEqualTo(eventosAntes);

        consentirExcedente(req);
        JsonNode novo = enviar(f.c().localA, V, req, 201,
                "CMP009/CMP010 consentimento documentado permite o excedente sem perder o mapeamento");
        long id = novo.path("id").asLong();
        assertRecorte(f, id, f.dataTransferencia().minusDays(1), false);
        assertRecorte(f, id, f.dataTransferencia(), true);
        assertThat(consultar(f.c().localA, base(f.n().id()) + "/composicoes/excedentes?data=" + f.dataTransferencia(),
                "CMP011 excedente da Gestão destino conserva sua própria evidência").toString())
                .contains("SEI-AUD03-EXCEDENTE", "ATO-AUD03-EXCEDENTE");
        assertThat(emTransacao(() -> em.createQuery(
                        "select count(e) from EventoAlocacaoVinculo e where e.vinculo.id=:id "
                                + "and e.inclusaoExcedente=true and e.lotacaoNovaId=:lotacao", Long.class)
                .setParameter("id", id).setParameter("lotacao", f.c().lotacaoB.getId()).getSingleResult()))
                .isGreaterThanOrEqualTo(1L);
    }

    @Test
    void antecipacaoDoInicioAtravessaTransferenciaReconstroiOrigemEPreservaConhecimentoAnterior() throws Exception {
        Preparado f = preparar(5, true);
        LocalDate inicioOriginal = f.dataTransferencia().plusDays(2);
        LocalDate fim = f.dataTransferencia().plusDays(8);
        JsonNode novo = enviar(f.c().localA, V, cadastro(f, 4, inicioOriginal, fim), 201,
                "AUD03 vínculo original começa no destino depois da transferência");
        long id = novo.path("id").asLong();
        assertProjecaoDoVinculo(id, false, f.c().gestaoB.getId());
        Instant conhecimentoAnterior = Instant.now();
        LocalDate inicioCorrigido = RelogioSistema.hoje();
        Map<String, Object> req = retificacao(inicioCorrigido);
        JsonNode previa = enviar(f.c().localA, V + "/" + id + "/previas/retificacao-inicio", req, 200,
                "AUD03 prévia da extensão retrocede pelo mapa até a Gestão de origem");
        req.put("confirmarImpacto", true);
        req.put("tokenPrevia", previa.path("token").asText());
        enviar(f.c().localA, V + "/" + id + "/retificar-inicio", req, 200,
                "AUD03 antecipação não estende o catálogo de B para o período de A");

        assertRecorte(f, id, f.dataTransferencia().minusDays(1), false);
        assertRecorte(f, id, f.dataTransferencia(), true);
        assertRecorte(f, id, inicioOriginal, true);
        JsonNode posicaoAnterior = posicao(f, id, f.dataTransferencia().minusDays(1), conhecimentoAnterior);
        assertThat(posicaoAnterior.path("situacaoTemporal").asText()).isEqualTo("PROGRAMADA");
        assertThat(posicaoAnterior.path("alocacaoId").isNull()).isTrue();
        assertThat(emTransacao(() -> em.find(VinculoContratual.class, id).getDataInicio())).isEqualTo(inicioCorrigido);
        assertThat(contarNoContrato(f, "VinculoContratual")).isEqualTo(1L);
        assertProjecaoDoVinculo(id, true, f.c().gestaoA.getId());
        assertThat(consultar(f.c().localA, V + "/" + id + "/eventos",
                "AUD002 retificação mantém a criação e o início original").toString())
                .contains("CRIACAO", "RETIFICACAO_INICIO", inicioOriginal.toString());
    }

    @Test
    void antecipacaoExigeConsentimentoQuandoSomenteAOrigemTemExcedente() throws Exception {
        Preparado f = preparar(1, true);
        enviar(f.c().localA, V, cadastro(f, 5, RelogioSistema.hoje(), f.dataTransferencia().minusDays(1)), 201,
                "AUD03 vaga de A ocupada apenas antes da transferência");
        LocalDate inicioOriginal = f.dataTransferencia().plusDays(1);
        long id = enviar(f.c().localA, V, cadastro(f, 6, inicioOriginal, f.dataTransferencia().plusDays(8)), 201,
                "AUD03 vaga de B desocupada para o vínculo programado").path("id").asLong();
        Map<String, Object> req = retificacao(RelogioSistema.hoje());
        JsonNode previa = enviar(f.c().localA, V + "/" + id + "/previas/retificacao-inicio", req, 200,
                "AUD03 prévia mede ocupação do recorte A no intervalo que será acrescido");
        assertThat(previa.path("impactos").toString()).contains("ocupação excedente");
        req.put("confirmarImpacto", true);
        req.put("tokenPrevia", previa.path("token").asText());
        long eventosAntes = contarEventosAlocacao(f);
        enviar(f.c().localA, V + "/" + id + "/retificar-inicio", req, 409,
                "AUD03 confirmação de impacto não substitui o consentimento de excedente");
        assertThat(emTransacao(() -> em.find(VinculoContratual.class, id).getDataInicio())).isEqualTo(inicioOriginal);
        assertThat(contarEventosAlocacao(f)).isEqualTo(eventosAntes);
        req.put("confirmarInclusaoExcedente", true);
        JsonNode atualizada = enviar(f.c().localA, V + "/" + id + "/previas/retificacao-inicio", req, 200,
                "AUD03 nova prévia antes da confirmação documentada");
        req.put("tokenPrevia", atualizada.path("token").asText());
        enviar(f.c().localA, V + "/" + id + "/retificar-inicio", req, 200,
                "AUD03 autorização excepcional cobre o excedente da origem");
        assertRecorte(f, id, f.dataTransferencia().minusDays(1), false);
        assertRecorte(f, id, f.dataTransferencia(), true);
    }

    @Test
    void mapaSemFotografiaDeJornadaRecusaCadastroSemInventarDestinoENaoDeixaRegistrosParciais() throws Exception {
        Preparado f = preparar(5, true);
        emTransacao(() -> {
            assertThat(em.createQuery("update MapeamentoTransferenciaGestao "
                            + "set jornadaDestinoSemanalFotografia=null where transferencia.id=:id")
                    .setParameter("id", f.transferenciaId()).executeUpdate()).isEqualTo(1);
            return null;
        });
        long eventosAntes = contarEventosAlocacao(f);
        enviar(f.c().localA, V, cadastro(f, 7, RelogioSistema.hoje(), f.dataTransferencia().plusDays(8)), 409,
                "CON023/CAT004 ausência de fotografia não pode ser preenchida pela jornada atual");
        assertThat(contarPessoa(f, 7)).isZero();
        assertThat(contarNoContrato(f, "VinculoContratual")).isZero();
        assertThat(contarAlocacoes(f)).isZero();
        assertThat(contarEventosAlocacao(f)).isEqualTo(eventosAntes);
        assertThat(contarNoContrato(f, "TransferenciaGestaoContrato")).isEqualTo(1L);
    }

    @Test
    void destinoAindaFuturoNaoRecebeEscritaAtualSobreOVinculoQueAtravessaTransferencia() throws Exception {
        Preparado f = preparar(5, true);
        enviar(f.c().localB, V, cadastro(f, 8, RelogioSistema.hoje(), f.dataTransferencia().plusDays(8)), 404,
                "PER001/CON013 destino futuro não é responsável negocial antes da data");
        assertThat(contarPessoa(f, 8)).isZero();
        assertThat(contarNoContrato(f, "VinculoContratual")).isZero();
    }

    private Preparado preparar(int capacidade, boolean futura) throws Exception {
        Cenario c = criarCenarioPersistido();
        NovoContrato n = emTransacao(() -> {
            try { return publicado(c, capacidade, null, false); }
            catch (Exception e) { throw new IllegalStateException(e); }
        });
        LocalDate data = RelogioSistema.hoje().plusDays(futura ? 5 : -5);
        conceder(c, n.id(), "TRANSFERENCIA_GESTAO_LOCAL");
        enviar(c.localA, base(n.id()) + "/transferir-gestao", mapa(/* Conferência SINTÉTICA explícita desta fixture; não é padrão do produto. */
                "conferenciaDocumentalConfirmada", true, "versaoConferenciaDocumental", "transferencia-gestao-conferencia-v1", 
                "dataEfetiva", data, "dataAto", RelogioSistema.hoje(),
                "gestaoLocalDestinoId", c.gestaoB.getId(), "tipoContratacaoDestinoId", c.tipoB.getId(),
                "motivo", "Ato de transferência anterior ao cadastro ou correção do vínculo",
                "referenciaSei", "SEI-AUD03-MAPA", "autorizacaoAdministrativa", "ATO-HTTP",
                "mapeamentos", List.of(mapa("composicaoOrigemId", n.composicao1(),
                        "lotacaoDestinoId", c.lotacaoB.getId(), "jornadaDestinoId", c.jornadaB.getId(),
                        "cargoDestinoId", c.cargoB.getId()))), 200,
                "CON023 transferência documental sem vínculos ainda cadastrados neste contrato");
        long transferencia = emTransacao(() -> em.createQuery(
                        "select t.id from TransferenciaGestaoContrato t where t.contrato.id=:id", Long.class)
                .setParameter("id", n.id()).getSingleResult());
        return new Preparado(c, n, data, transferencia, futura,
                c.lotacaoB.getNome(), c.jornadaB.getDescricao(), c.cargoB.getNome());
    }

    private void renomearDestinoDepoisDoMapa(Preparado f) {
        emTransacao(() -> {
            em.find(Lotacao.class, f.c().lotacaoB.getId()).setNome("Lotação posterior ao mapa " + f.n().id());
            em.find(Jornada.class, f.c().jornadaB.getId()).setDescricao("Jornada posterior ao mapa " + f.n().id());
            em.find(Cargo.class, f.c().cargoB.getId()).setNome("Cargo posterior ao mapa " + f.n().id());
            return null;
        });
    }

    private Map<String, Object> cadastro(Preparado f, int slot, LocalDate inicio, LocalDate fim) {
        return mapa("pessoa", mapa("nome", "Pessoa AUD03 " + f.n().id() + " " + slot, "cpf", cpfTeste(f, slot)),
                "contratoId", f.n().id(), "composicaoId", f.n().composicao1(), "dataInicio", inicio, "dataFim", fim);
    }

    private Map<String, Object> retificacao(LocalDate novoInicio) {
        return mapa("novoInicio", novoInicio, "justificativa", "Correção documental do início atravessando transferência",
                "evidencia", "SEI-AUD03-RETIFICACAO", "autorizacao", "ATO-AUD03-RETIFICACAO");
    }

    private void consentirExcedente(Map<String, Object> req) {
        req.put("confirmarInclusaoExcedente", true);
        req.put("justificativaExcedente", "Ocupação excedente no destino expressamente autorizada");
        req.put("evidenciaExcedente", "SEI-AUD03-EXCEDENTE");
        req.put("autorizacaoExcedente", "ATO-AUD03-EXCEDENTE");
    }

    private UsuarioAplicacao executorAtual(Preparado f) { return f.futura() ? f.c().localA : f.c().localB; }

    private String cpfTeste(Preparado f, int slot) { return cpf(Math.toIntExact(850000000L + f.n().id() * 10L + slot)); }

    private JsonNode posicao(Preparado f, long id, LocalDate data, Instant conhecido) throws Exception {
        return consultar(f.c().nacional, V + "/" + id + "/alocacoes/posicao?data=" + data
                + (conhecido == null ? "" : "&conhecidoAte=" + conhecido), "AUD03 recorte histórico pelos dois cortes");
    }

    private void assertRecorte(Preparado f, long id, LocalDate data, boolean destino) throws Exception {
        JsonNode p = posicao(f, id, data, null);
        assertThat(p.path("lotacaoId").asLong()).isEqualTo(destino ? f.c().lotacaoB.getId() : f.c().lotacaoA.getId());
        assertThat(p.path("jornadaId").asLong()).isEqualTo(destino ? f.c().jornadaB.getId() : f.c().jornadaA.getId());
        assertThat(p.path("cargoId").asLong()).isEqualTo(destino ? f.c().cargoB.getId() : f.c().cargoA.getId());
    }

    private List<Object[]> faixasAtivas(long id) {
        return emTransacao(() -> em.createQuery("select a.dataInicio, a.dataFim, a.lotacao.id, a.jornada.id, a.cargo.id "
                        + "from AlocacaoVinculo a where a.vinculo.id=:id and a.estadoNegocial="
                        + "com.example.siggtrat.dominio.alocacao.EstadoNegocialAlocacao.REGISTRADA "
                        + "order by a.dataInicio, a.id", Object[].class).setParameter("id", id).getResultList());
    }

    private void assertProjecaoDoVinculo(long id, boolean ativa, long gestao) {
        emTransacao(() -> {
            long pessoa = em.find(VinculoContratual.class, id).getPessoa().getId();
            Empregado e = em.createQuery("select e from Empregado e where e.pessoaCanonica.id=:p", Empregado.class)
                    .setParameter("p", pessoa).getSingleResult();
            assertThat(e.getGestaoLocal().getId()).isEqualTo(gestao);
            assertThat(e.getStatus().getDescricao()).isEqualToIgnoringCase(ativa ? "ATIVO" : "INATIVO");
            assertThat(e.getContratos()).hasSize(ativa ? 1 : 0);
            return null;
        });
    }

    private long contarNoContrato(Preparado f, String entidade) {
        return emTransacao(() -> em.createQuery("select count(x) from " + entidade + " x where x.contrato.id=:id", Long.class)
                .setParameter("id", f.n().id()).getSingleResult());
    }

    private long contarAlocacoes(Preparado f) {
        return emTransacao(() -> em.createQuery("select count(a) from AlocacaoVinculo a where a.vinculo.contrato.id=:id", Long.class)
                .setParameter("id", f.n().id()).getSingleResult());
    }

    private long contarEventosAlocacao(Preparado f) {
        return emTransacao(() -> em.createQuery("select count(e) from EventoAlocacaoVinculo e where e.vinculo.contrato.id=:id", Long.class)
                .setParameter("id", f.n().id()).getSingleResult());
    }

    private long contarPessoa(Preparado f, int slot) {
        return emTransacao(() -> em.createQuery("select count(p) from Pessoa p where p.cpf=:cpf", Long.class)
                .setParameter("cpf", cpfTeste(f, slot)).getSingleResult());
    }

    private record Preparado(Cenario c, NovoContrato n, LocalDate dataTransferencia, long transferenciaId,
                             boolean futura, String nomeDestinoFotografado,
                             String jornadaDestinoFotografada, String cargoDestinoFotografado) {}
}
