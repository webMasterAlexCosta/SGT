package com.example.siggtrat.integration;

import com.example.siggtrat.dto.PosicaoTemporalContratoDTO;
import com.example.siggtrat.dto.VersaoContratoNegocialDTO;
import com.example.siggtrat.entidade.*;
import com.example.siggtrat.servico.MotorTemporalContratoServico;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.ArrayList;
import java.util.UUID;
import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.*;

/**
 * TMP-004/005/008/010/014: JPA/MySQL reais e releitura em novas transações.
 * Datas de conhecimento sintéticas são declaradas antes de persistir a fonte;
 * nenhum objeto de repositório/motor é mockado e nenhuma consulta cria história.
 */
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class MotorTemporalVigenciaReplayIT extends MySqlIntegrationTestSupport {
    private static final LocalDate INICIO = LocalDate.of(2026, 1, 1);
    private static final LocalDate FIM = LocalDate.of(2026, 1, 31);
    private static final Instant T0 = Instant.parse("2026-01-01T01:00:00Z");
    private static final Instant T1 = Instant.parse("2026-02-01T10:00:00Z");
    private static final Instant T2 = Instant.parse("2026-02-02T10:00:00Z");
    private static final Instant T3 = Instant.parse("2026-02-03T10:00:00Z");

    @Autowired EntityManager em;
    @Autowired PlatformTransactionManager transacoes;
    @Autowired MotorTemporalContratoServico motor;


    // Somente identidades artificiais desta invocação. Os cenários precisam de
    // commits e releituras reais, mas não podem deixar acervo ambíguo para outro
    // teste que consulta legitimamente o escopo nacional no mesmo banco.
    private final List<Long> contratosSinteticosDestaInvocacao = new ArrayList<>();

    @AfterEach
    void removerApenasFixturesConfirmadasDestaInvocacao() {
        if (contratosSinteticosDestaInvocacao.isEmpty()) return;
        tx(() -> {
            for (Long id : contratosSinteticosDestaInvocacao) {
                Contrato c = em.find(Contrato.class, id);
                if (c == null) continue; // Uma preparação que sofreu rollback não deixou acervo.
                assertThat(c.getNumeroContrato()).startsWith("REPLAY-");
                GestaoLocal gestao = c.getGestaoLocal();
                assertThat(gestao.getNome()).isEqualTo("Replay vigência");
                // Nenhum comando de produção cria dependências nesta fixture;
                // as relações e instrumentos abaixo foram persistidos pelo teste.
                em.createQuery("select r from RelacaoPrecedenciaEventoContrato r where r.contrato.id = :id",
                                RelacaoPrecedenciaEventoContrato.class).setParameter("id", id)
                        .getResultList().forEach(em::remove);
                em.flush();
                em.createQuery("select v from VersaoContratoNegocial v where v.contrato.id = :id",
                                VersaoContratoNegocial.class).setParameter("id", id)
                        .getResultList().forEach(em::remove);
                em.createQuery("select a from AditamentoContratual a where a.contrato.id = :id",
                                AditamentoContratual.class).setParameter("id", id)
                        .getResultList().forEach(em::remove);
                em.flush();
                em.createQuery("select e from EventoCicloVidaContrato e where e.contrato.id = :id",
                                EventoCicloVidaContrato.class).setParameter("id", id)
                        .getResultList().forEach(em::remove);
                em.flush();
                em.remove(c);
                em.flush();
                em.remove(gestao);
            }
            em.flush();
            return null;
        });
        // Nova transação confirma o isolamento físico; o @AfterEach não converte
        // os testes de persistência acima em testes sob rollback externo.
        assertThat(tx(() -> em.createQuery("select count(c) from Contrato c where c.id in :ids", Long.class)
                .setParameter("ids", contratosSinteticosDestaInvocacao).getSingleResult())).isZero();
        contratosSinteticosDestaInvocacao.clear();
    }

    @Test
    void descricaoPosteriorNaoApagaDeltaAnteriorConhecidoDepoisNemAlteraFotografias() {
        long id = tx(() -> {
            Contrato c = contrato(true);
            EventoCicloVidaContrato descricao = evento(c, TipoEventoCicloVidaContrato.ADITAMENTO,
                    INICIO.plusDays(19), T1, "DESCRICAO");
            instrumento(c, descricao, T1, null, null);
            fotografia(c, descricao, 2, descricao.getDataEfetiva(), T1, INICIO, FIM);
            EventoCicloVidaContrato prorrogacao = evento(c, TipoEventoCicloVidaContrato.ADITAMENTO,
                    INICIO.plusDays(9), T2, "VIGENCIA");
            instrumento(c, prorrogacao, T2, null, FIM.plusDays(30));
            fotografia(c, prorrogacao, 3, prorrogacao.getDataEfetiva(), T2, INICIO, FIM.plusDays(30));
            return c.getId();
        });
        Prova antes = prova(id);

        assertVigencia(posicao(id, FIM.plusDays(5), T1.plusSeconds(1)), INICIO, FIM,
                SituacaoTemporalContrato.ENCERRADO);
        assertVigencia(posicao(id, FIM.plusDays(5), T2.plusSeconds(1)), INICIO, FIM.plusDays(30),
                SituacaoTemporalContrato.VIGENTE);
        assertVigencia(posicao(id, INICIO.plusDays(8), T3), INICIO, FIM, SituacaoTemporalContrato.VIGENTE);
        assertVigencia(posicao(id, INICIO.plusDays(9), T3), INICIO, FIM.plusDays(30), SituacaoTemporalContrato.VIGENTE);

        assertThat(prova(id)).as("Consulta confirmada não regrava mestre nem fotografias armazenadas").isEqualTo(antes);
        assertThat(prova(id).versoes().get(1).dataFim()).isEqualTo(FIM);
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void deltaExigeConhecimentoDoInstrumentoEDoEvento(boolean eventoConhecidoDepois) {
        long id = tx(() -> {
            Contrato c = contrato(true);
            EventoCicloVidaContrato e = evento(c, TipoEventoCicloVidaContrato.ADITAMENTO,
                    INICIO.plusDays(9), eventoConhecidoDepois ? T2 : T1, "VIGENCIA");
            instrumento(c, e, eventoConhecidoDepois ? T1 : T2, null, FIM.plusDays(30));
            // Uma foto integral já disponível não é o delta explícito ausente do corte.
            fotografia(c, e, 2, e.getDataEfetiva(), T1, INICIO, FIM.plusDays(30));
            return c.getId();
        });

        assertVigencia(posicao(id, FIM.plusDays(5), T1.plusSeconds(1)), INICIO, FIM,
                SituacaoTemporalContrato.ENCERRADO);
        assertVigencia(posicao(id, FIM.plusDays(5), T2.plusSeconds(1)), INICIO, FIM.plusDays(30),
                SituacaoTemporalContrato.VIGENTE);
    }

    @Test
    void precedenciaExplicitaOrdenaDeltasNaMesmaDataSemUsarMaiorIdOuRegistro() {
        long[] ids = tx(() -> {
            Contrato c = contrato(true);
            LocalDate efeito = INICIO.plusDays(9);
            EventoCicloVidaContrato sucessor = evento(c, TipoEventoCicloVidaContrato.ADITAMENTO, efeito, T1, "VIGENCIA");
            instrumento(c, sucessor, T1, null, FIM.plusDays(30));
            EventoCicloVidaContrato antecessor = evento(c, TipoEventoCicloVidaContrato.ADITAMENTO, efeito, T2, "VIGENCIA");
            instrumento(c, antecessor, T2, null, FIM.plusDays(10));
            assertThat(sucessor.getId()).isLessThan(antecessor.getId());
            relacao(c, sucessor, antecessor, TipoRelacaoPrecedenciaNegocial.SUCEDE, T3);
            return new long[]{c.getId(), sucessor.getId(), antecessor.getId()};
        });

        assertThatThrownBy(() -> posicao(ids[0], FIM.plusDays(5), T2.plusSeconds(1)))
                .isInstanceOfSatisfying(ResponseStatusException.class,
                        e -> assertThat(e.getStatusCode().value()).isEqualTo(409));
        var ordenada = posicao(ids[0], FIM.plusDays(5), T3.plusSeconds(1));
        assertVigencia(ordenada, INICIO, FIM.plusDays(30), SituacaoTemporalContrato.VIGENTE);
        assertThat(ordenada.eventosAplicados()).extracting(e -> e.id()).containsSubsequence(ids[2], ids[1]);
    }

    @ParameterizedTest
    @EnumSource(value = TipoRelacaoPrecedenciaNegocial.class, names = {"CANCELA", "RETIFICA", "SUBSTITUI"})
    void retiradaDaProrrogacaoPreservaOCorteAnteriorESoRemoveSeuDelta(TipoRelacaoPrecedenciaNegocial tipo) {
        long id = tx(() -> {
            Contrato c = contrato(true);
            LocalDate efeito = INICIO.plusDays(9);
            EventoCicloVidaContrato anterior = evento(c, TipoEventoCicloVidaContrato.ADITAMENTO, efeito, T1, "VIGENCIA");
            instrumento(c, anterior, T1, null, FIM.plusDays(30));
            fotografia(c, anterior, 2, efeito, T1, INICIO, FIM.plusDays(30));
            EventoCicloVidaContrato correcao = evento(c, tipo == TipoRelacaoPrecedenciaNegocial.CANCELA
                    ? TipoEventoCicloVidaContrato.CANCELAMENTO_ADITAMENTO : TipoEventoCicloVidaContrato.ADITAMENTO,
                    efeito, T2, "VIGENCIA", tipo == TipoRelacaoPrecedenciaNegocial.CANCELA ? null : anterior.getFatoLogicoId());
            if (tipo != TipoRelacaoPrecedenciaNegocial.CANCELA) {
                instrumento(c, correcao, T2, null, null);
                fotografia(c, correcao, 3, efeito, T2, INICIO, FIM.plusDays(30));
            }
            relacao(c, correcao, anterior, tipo, T2);
            return c.getId();
        });

        assertVigencia(posicao(id, FIM.plusDays(5), T1.plusSeconds(1)), INICIO, FIM.plusDays(30),
                SituacaoTemporalContrato.VIGENTE);
        assertVigencia(posicao(id, FIM.plusDays(5), T2.plusSeconds(1)), INICIO, FIM,
                SituacaoTemporalContrato.ENCERRADO);
    }

    @Test
    void aditamentoPosteriorNaoCancelaEncerramentoESeusDtosConservamMesmoLimite() {
        LocalDate fimEncerramento = INICIO.plusDays(11);
        long id = tx(() -> {
            Contrato c = contrato(true);
            evento(c, TipoEventoCicloVidaContrato.ENCERRAMENTO_ANTECIPADO, fimEncerramento, T1, "VIGENCIA");
            EventoCicloVidaContrato a = evento(c, TipoEventoCicloVidaContrato.ADITAMENTO,
                    INICIO.plusDays(14), T2, "VIGENCIA");
            instrumento(c, a, T2, null, FIM.plusDays(30));
            fotografia(c, a, 2, a.getDataEfetiva(), T2, INICIO, FIM.plusDays(30));
            return c.getId();
        });

        assertVigencia(posicao(id, INICIO.plusDays(10), T3), INICIO, FIM, SituacaoTemporalContrato.VIGENTE);
        assertVigencia(posicao(id, fimEncerramento, T3), INICIO, fimEncerramento, SituacaoTemporalContrato.VIGENTE);
        assertVigencia(posicao(id, INICIO.plusDays(20), T3), INICIO, fimEncerramento, SituacaoTemporalContrato.ENCERRADO);
    }

    @Test
    void ausenciaDaOrigemNaoUsaMestreENaoRetroageFonteLegadaDatada() {
        LocalDate dataDaFonte = INICIO.plusDays(19);
        long id = tx(() -> {
            Contrato c = contrato(false);
            c.setDataInicio(INICIO.minusYears(2)); c.setDataFim(FIM.plusYears(3));
            fotografia(c, null, 2, dataDaFonte, T1, INICIO, FIM);
            return c.getId();
        });

        var antes = posicao(id, dataDaFonte.minusDays(1), T3);
        assertThat(antes.vigenciaInicio()).isNull(); assertThat(antes.vigenciaFim()).isNull();
        assertThat(antes.situacaoTemporal()).isNull(); assertThat(antes.versaoContratual()).isNull();
        var desconhecida = posicao(id, dataDaFonte, T0);
        assertThat(desconhecida.vigenciaInicio()).isNull(); assertThat(desconhecida.vigenciaFim()).isNull();
        assertVigencia(posicao(id, dataDaFonte, T3), INICIO, FIM, SituacaoTemporalContrato.VIGENTE);
    }

    @Test
    void camposNulosNaOrigemContinuamDesconhecidosEAditamentoSoComprovaOCampoExplicito() {
        long id = tx(() -> {
            Contrato c = contrato(false);
            fotografia(c, null, 1, INICIO, T0, null, null);
            EventoCicloVidaContrato a = evento(c, TipoEventoCicloVidaContrato.ADITAMENTO, INICIO.plusDays(9), T1, "VIGENCIA");
            instrumento(c, a, T1, null, FIM.plusDays(30));
            return c.getId();
        });

        assertVigencia(posicao(id, INICIO.plusDays(5), T3), null, null, null);
        assertVigencia(posicao(id, INICIO.plusDays(10), T3), null, FIM.plusDays(30), null);
    }

    @Test
    void deltaDeInicioTambemEReaplicadoSemUsarFotografiaIntegralPosterior() {
        long id = tx(() -> {
            Contrato c = contrato(true);
            EventoCicloVidaContrato descricao = evento(c, TipoEventoCicloVidaContrato.ADITAMENTO, INICIO.plusDays(19), T1, "DESCRICAO");
            instrumento(c, descricao, T1, null, null);
            fotografia(c, descricao, 2, descricao.getDataEfetiva(), T1, INICIO, FIM);
            EventoCicloVidaContrato inicio = evento(c, TipoEventoCicloVidaContrato.ADITAMENTO, INICIO.plusDays(9), T2, "VIGENCIA");
            instrumento(c, inicio, T2, INICIO.minusDays(2), null);
            fotografia(c, inicio, 3, inicio.getDataEfetiva(), T2, INICIO.minusDays(2), FIM);
            return c.getId();
        });
        assertVigencia(posicao(id, INICIO.plusDays(20), T1.plusSeconds(1)), INICIO, FIM, SituacaoTemporalContrato.VIGENTE);
        assertVigencia(posicao(id, INICIO.plusDays(20), T3), INICIO.minusDays(2), FIM, SituacaoTemporalContrato.VIGENTE);
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void fonteLegadaAceitaDeltaPosteriorMasNaoAdivinhaIncorporacaoDeDeltaAnterior(boolean deltaPosterior) {
        long id = tx(() -> {
            Contrato c = contrato(false);
            LocalDate dataDaFonte = INICIO.plusDays(19);
            fotografia(c, null, 2, dataDaFonte, T1, INICIO, FIM);
            EventoCicloVidaContrato a = evento(c, TipoEventoCicloVidaContrato.ADITAMENTO,
                    deltaPosterior ? dataDaFonte.plusDays(1) : dataDaFonte.minusDays(1), T2, "VIGENCIA");
            instrumento(c, a, T2, null, FIM.plusDays(30));
            return c.getId();
        });

        if (deltaPosterior) {
            assertVigencia(posicao(id, FIM.plusDays(5), T3), INICIO, FIM.plusDays(30), SituacaoTemporalContrato.VIGENTE);
        } else {
            assertThatThrownBy(() -> posicao(id, FIM.plusDays(5), T3))
                    .isInstanceOfSatisfying(ResponseStatusException.class, e -> {
                        assertThat(e.getStatusCode().value()).isEqualTo(409);
                        assertThat(e.getReason()).contains("conciliação documental");
                    });
        }
    }

    private Contrato contrato(boolean origem) {
        SuperintendenciaEstadual se = em.createQuery("select s from SuperintendenciaEstadual s order by s.id", SuperintendenciaEstadual.class)
                .setMaxResults(1).getResultStream().findFirst().orElseGet(() -> {
                    var nova = new SuperintendenciaEstadual("Fixture de vigência", "XV"); em.persist(nova); return nova;
                });
        GestaoLocal gestao = new GestaoLocal(); gestao.setSuperintendencia(se); gestao.setNome("Replay vigência");
        gestao.setCodigo(UUID.randomUUID().toString().substring(0, 12)); em.persist(gestao);
        Contrato c = new Contrato(); c.setGestaoLocal(gestao); c.setNumeroContrato("REPLAY-" + UUID.randomUUID());
        c.setAnoContrato(2026); c.setDataInicio(INICIO); c.setDataFim(FIM);
        c.setEstadoNegocial(EstadoNegocialContrato.PUBLICADO); c.setPublicadoEm(T0); em.persist(c);
        contratosSinteticosDestaInvocacao.add(c.getId());
        if (origem) fotografia(c, null, 1, INICIO, T0, INICIO, FIM);
        return c;
    }

    private EventoCicloVidaContrato evento(Contrato c, TipoEventoCicloVidaContrato tipo,
                                          LocalDate data, Instant registro, String chave) {
        return evento(c, tipo, data, registro, chave, null);
    }

    private EventoCicloVidaContrato evento(Contrato c, TipoEventoCicloVidaContrato tipo,
                                          LocalDate data, Instant registro, String chave, String fatoLogico) {
        EventoCicloVidaContrato e = new EventoCicloVidaContrato(); e.setContrato(c); e.setTipoEvento(tipo);
        e.setDataEfetiva(data); e.setDataAto(data); e.setChaveNegocial(chave);
        if (fatoLogico != null) { e.setFatoLogicoId(fatoLogico); e.setVersaoNegocial(2); }
        e.setMotivo("Fonte sintética explícita de reconstrução da vigência"); e.setReferenciaSei("SEI-REPLAY");
        ReflectionTestUtils.setField(e, "registradoEm", registro); em.persist(e); return e;
    }

    private void instrumento(Contrato c, EventoCicloVidaContrato e, Instant registro, LocalDate inicio, LocalDate fim) {
        AditamentoContratual a = new AditamentoContratual(); a.setContrato(c); a.setEvento(e);
        a.setIdentificacao("ADI-" + UUID.randomUUID()); a.setDataDocumento(e.getDataEfetiva()); a.setDataEfetiva(e.getDataEfetiva());
        a.setJustificativa("Documento sintético de vigência"); a.setReferenciaSei("SEI-REPLAY");
        a.setNovaDescricao("Descrição registrada " + e.getId()); a.setNovaDataInicio(inicio); a.setNovaDataFim(fim);
        ReflectionTestUtils.setField(a, "registradoEm", registro); em.persist(a);
    }

    private void fotografia(Contrato c, EventoCicloVidaContrato e, int numero, LocalDate efetiva,
                             Instant registro, LocalDate inicio, LocalDate fim) {
        VersaoContratoNegocial v = new VersaoContratoNegocial(); v.setContrato(c); v.setEventoOrigem(e);
        v.setNumeroVersao(numero); v.setDataEfetiva(efetiva); v.setDataInicio(inicio); v.setDataFim(fim);
        v.setNumeroContrato(c.getNumeroContrato()); v.setAnoContrato(c.getAnoContrato());
        v.setGestaoLocalId(c.getGestaoLocal().getId()); v.setGestaoLocalNome(c.getGestaoLocal().getNome());
        v.setHabilitacaoAdministrativa(HabilitacaoAdministrativaContrato.ATIVO);
        ReflectionTestUtils.setField(v, "registradoEm", registro); em.persist(v);
    }

    private void relacao(Contrato c, EventoCicloVidaContrato fonte, EventoCicloVidaContrato alvo,
                          TipoRelacaoPrecedenciaNegocial tipo, Instant registro) {
        RelacaoPrecedenciaEventoContrato r = new RelacaoPrecedenciaEventoContrato(); r.setContrato(c);
        r.setEventoFonte(fonte); r.setEventoAlvo(alvo); r.setTipoRelacao(tipo); r.setMotivo("Ordem documental explícita");
        ReflectionTestUtils.setField(r, "registradoEm", registro); em.persist(r);
    }

    private PosicaoTemporalContratoDTO posicao(long id, LocalDate data, Instant conhecido) {
        return tx(() -> motor.reconstruirPosicao(em.find(Contrato.class, id), data, conhecido));
    }

    private void assertVigencia(PosicaoTemporalContratoDTO p, LocalDate inicio, LocalDate fim, SituacaoTemporalContrato situacao) {
        assertThat(p.vigenciaInicio()).isEqualTo(inicio); assertThat(p.vigenciaFim()).isEqualTo(fim);
        assertThat(p.situacaoTemporal()).isEqualTo(situacao);
        assertThat(p.versaoContratual()).as("A fonte conhecida deve continuar disponível no DTO").isNotNull();
        assertThat(p.versaoContratual().dataInicio()).isEqualTo(inicio);
        assertThat(p.versaoContratual().dataFim()).isEqualTo(fim);
    }

    private Prova prova(long id) {
        return tx(() -> {
            Contrato c = em.find(Contrato.class, id);
            return new Prova(c.getDataInicio(), c.getDataFim(), c.getVersion(), motor.listarVersoes(id));
        });
    }

    private <T> T tx(Supplier<T> operacao) {
        return new TransactionTemplate(transacoes).execute(status -> { em.clear(); return operacao.get(); });
    }

    private record Prova(LocalDate inicioMestre, LocalDate fimMestre, long versaoMestre,
                         List<VersaoContratoNegocialDTO> versoes) { }
}
