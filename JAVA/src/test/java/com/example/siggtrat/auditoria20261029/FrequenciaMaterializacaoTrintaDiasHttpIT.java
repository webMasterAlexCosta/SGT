package com.example.siggtrat.auditoria20261029;

import com.example.siggtrat.entidade.EventoFrequencia;
import com.example.siggtrat.entidade.Frequencia;
import com.example.siggtrat.servico.AuditoriaFrequenciaServico;
import com.example.siggtrat.servico.FechamentoAutomaticoFrequenciaNegocialServico;
import com.example.siggtrat.servico.PoliticaTemporalFrequenciaServico;
import com.example.siggtrat.utilitario.RelogioSistema;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import javax.sql.DataSource;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;

/** Lançamento configurado N30, fechamento pela edição de 72 horas; commits reais. */
@TestPropertySource(properties = "frequencia.janela-operacional-dias=30")
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class FrequenciaMaterializacaoTrintaDiasHttpIT extends MaterializacaoTrintaSuporte {
    @Autowired private FechamentoAutomaticoFrequenciaNegocialServico fechamento;
    @Autowired private PoliticaTemporalFrequenciaServico politica;

    @Test
    @Timeout(value = 90, unit = TimeUnit.SECONDS)
    void getProjetaFechadaSemEscreverEServicoMaterializaUmaVezAposPrazoDeEdicao() throws Exception {
        Cenario c;
        LocalDate hoje = RelogioSistema.hoje();
        LocalDate d = hoje.minusDays(29).with(TemporalAdjusters.nextOrSame(DayOfWeek.MONDAY));
        LocalDate recente = hoje.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        assertThat(politica.diasJanela()).isEqualTo(30);
        assertThat(Mockito.mockingDetails(taskScheduler).isMock()).isTrue();
        // O alvo nasce com registro anterior; o limite não vence em massa as
        // frequências recentes dos outros ensaios que compartilham o laboratório.
        Instant primeiroLancamento = Instant.now().minus(java.time.Duration.ofDays(4));
        long id;
        try (var relogio = Mockito.mockStatic(Instant.class, Mockito.CALLS_REAL_METHODS)) {
            relogio.when(Instant::now).thenReturn(primeiroLancamento);
            c = criarCenarioPersistido();
            declararFonteNominalSintetica(c, "N30-MATERIALIZACAO-SINTETICA");
            id = seed(c, d);
        }
        String consulta = "/api/frequencias?data=" + d + "&contratoId=" + c.contratoA.getId();
        long controle = seed(c, recente);
        Instant limite = emTransacao(() -> em.find(Frequencia.class, id).getPrazoEdicaoEm());
        Instant limiteControle = emTransacao(() -> em.find(Frequencia.class, controle).getPrazoEdicaoEm());
        assertThat(limiteControle).isAfter(limite);
        Foto original = foto(id), controleAntes = foto(controle);
        registrar("semantico-seed", mapa("original", original, "controle", controleAntes, "limite", limite));
        conferirOriginal(original, c.operacionalA.getId());
        conferirOriginal(controleAntes, c.operacionalA.getId());
        consultar(c.localA, consulta, "Sessão legítima do observador antes do relógio semântico");
        // Todas as datas/instantes foram calculadas antes de abrir o MockedStatic.
        try (var relogio = Mockito.mockStatic(Instant.class, Mockito.CALLS_REAL_METHODS)) {
            relogio.when(Instant::now).thenReturn(limite);
            var get = consultar(c.localA, consulta, "GET calcula FECHADA mas não materializa evento");
            Foto depoisGet = foto(id);
            int primeira = fechamento.fecharVencidas();
            Foto fechada = foto(id), controleDepois = foto(controle);
            boolean integra = auditoria.evidenciasIntegridade(id).cadeiaIntegra();
            int repeticao = fechamento.fecharVencidas();
            Foto estavel = foto(id), controleEstavel = foto(controle);
            registrar("semantico-final", mapa("janela", 30, "limite", limite, "getSha", hJson(get), "original", original,
                    "depoisGet", depoisGet, "fechada", fechada, "estavel", estavel, "controleAntes", controleAntes,
                    "controleDepois", controleDepois, "controleEstavel", controleEstavel, "primeiraExecucao", primeira,
                    "repeticao", repeticao, "cadeiaIntegra", integra,
                    "limiteDaProva", "Só Instant.now controlado. Não prova timer/JAR/virada física."));
            assertAll(() -> assertThat(get.path("totalElements").asInt()).isEqualTo(1),
                    () -> assertThat(get.path("content").get(0).path("estadoNegocial").asText()).isEqualTo("FECHADA"),
                    () -> assertThat(depoisGet).as("GET preserva banco ABERTA/v1 e evento original").isEqualTo(original),
                    () -> assertThat(primeira).isGreaterThanOrEqualTo(1),
                    () -> conferirFechada(original, fechada, false),
                    () -> {
                        String motivo = emTransacao(() -> em.find(EventoFrequencia.class,
                                fechada.eventos().get(1).id()).getMotivo());
                        assertThat(motivo).isEqualTo("Fechamento automático após o prazo de edição do primeiro lançamento.");
                    },
                    () -> assertThat(integra).isTrue(),
                    () -> assertThat(controleDepois).isEqualTo(controleAntes),
                    () -> assertThat(repeticao).isZero(),
                    () -> assertThat(estavel).isEqualTo(fechada),
                    () -> assertThat(controleEstavel).isEqualTo(controleAntes));
        }
    }
}

/** Suporte interno comum aos dois drafts, sem novos endpoints ou regras. */
abstract class MaterializacaoTrintaSuporte extends ContratosHttpBase {
    @Autowired protected DataSource fonte;
    @Autowired protected AuditoriaFrequenciaServico auditoria;

    protected long seed(Cenario c, LocalDate data) throws Exception {
        return enviar(c.operacionalA, "/api/frequencias", mapa("vinculoContratualId", c.vinculoA.getId(), "dataReferencia", data,
                "status", "FALTA", "chaveIdempotencia", "n30-materializacao-" + java.util.UUID.randomUUID()), 200,
                "Fato operacional sintético e autenticado, em dia útil dentro da janela do preparador").path("id").asLong();
    }

    protected Foto foto(long id) {
        return emTransacao(() -> {
            Frequencia f = em.find(Frequencia.class, id);
            List<Evento> eventos = new ArrayList<>();
            for (EventoFrequencia e : em.createQuery("select e from EventoFrequencia e where e.frequencia.id=:id order by e.numeroVersao,e.id", EventoFrequencia.class)
                    .setParameter("id", id).getResultList()) {
                String linha = String.valueOf(em.createNativeQuery("select to_jsonb(e)::text from evento_frequencia e where e.id_evento_frequencia=:id")
                        .setParameter("id", e.getId()).getSingleResult());
                eventos.add(new Evento(e.getId(), e.getNumeroVersao(), e.getTipoEvento().name(), e.getDataReferencia(),
                        e.getUsuarioId(), "SISTEMA".equals(e.getUsuarioUsername()), e.getMotivo() != null && e.getMotivo().contains("31º dia"),
                        e.getSnapshotNovo() != null && e.getSnapshotNovo().startsWith("snapshotVersao=2;"),
                        h(e.getSnapshotAnterior()), h(e.getSnapshotNovo()), e.getHashAnterior(), e.getHashEvento(), e.getRegistradoEm(), h(linha)));
            }
            String linha = String.valueOf(em.createNativeQuery("select to_jsonb(f)::text from frequencia f where f.id=:id")
                    .setParameter("id", id).getSingleResult());
            String fonte = String.valueOf(em.createNativeQuery("select (to_jsonb(f)-ARRAY['estado_negocial','versao_negocial','version','updated_at']::text[])::text "
                    + "from frequencia f where f.id=:id").setParameter("id", id).getSingleResult());
            return new Foto(id, f.getEstadoNegocial().name(), f.getVersaoNegocial(), f.getStatus(), f.getDataReferencia(),
                    f.getAutorOriginalId(), f.getUltimaRetificacaoPorId(), h(fonte), h(linha), List.copyOf(eventos));
        });
    }

    protected void conferirOriginal(Foto f, long autor) {
        assertAll(() -> assertThat(f.estado()).isEqualTo("ABERTA"), () -> assertThat(f.versao()).isEqualTo(1),
                () -> assertThat(f.autorOriginal()).isEqualTo(autor), () -> assertThat(f.eventos()).hasSize(1),
                () -> assertThat(f.eventos().get(0).tipo()).isEqualTo("REGISTRO"),
                () -> assertThat(f.eventos().get(0).usuario()).isEqualTo(autor),
                () -> assertThat(f.eventos().get(0).snapshotV2()).isTrue());
    }

    protected void conferirFechada(Foto original, Foto fechada) {
        conferirFechada(original, fechada, true);
    }

    /** O ensaio histórico de JAR mantém a exigência original de motivo do 31º dia. */
    protected void conferirFechada(Foto original, Foto fechada, boolean motivoTrintaUmDias) {
        assertAll(() -> assertThat(fechada.estado()).isEqualTo("FECHADA"), () -> assertThat(fechada.versao()).isEqualTo(2),
                () -> assertThat(fechada.status()).isEqualTo(original.status()), () -> assertThat(fechada.data()).isEqualTo(original.data()),
                () -> assertThat(fechada.autorOriginal()).isEqualTo(original.autorOriginal()),
                () -> assertThat(fechada.ultimaRetificacao()).isEqualTo(original.ultimaRetificacao()),
                () -> assertThat(fechada.fonteSha()).as("Todos os campos de fonte, inclusive fotografias e autoria, preservados").isEqualTo(original.fonteSha()),
                () -> assertThat(fechada.eventos()).hasSize(2),
                () -> assertThat(fechada.eventos().get(0)).as("Prefixo integral, hashes/snapshots/instante/id preservados").isEqualTo(original.eventos().get(0)),
                () -> {
                    Evento e = fechada.eventos().get(1);
                    assertAll(() -> assertThat(e.tipo()).isEqualTo("FECHAMENTO_AUTOMATICO"), () -> assertThat(e.versao()).isEqualTo(2),
                            () -> assertThat(e.data()).isEqualTo(original.data()), () -> assertThat(e.usuario()).isNull(),
                            () -> assertThat(e.sistema()).isTrue(), () -> assertThat(e.motivo31Dia()).isEqualTo(motivoTrintaUmDias),
                            () -> assertThat(e.snapshotV2()).isTrue(), () -> assertThat(e.anterior()).isEqualTo(original.eventos().get(0).hash()),
                            () -> assertThat(e.hash()).hasSize(64), () -> assertThat(e.registradoEm()).isNotNull());
                });
    }

    protected void registrar(String caso, Object dados) throws Exception { SnapshotQuantitativos.registrar(mapper, "n30-materializacao", caso, dados); }
    protected String h(String s) { return s == null ? null : SnapshotQuantitativos.hash(s); }
    protected String hJson(Object o) throws Exception { return SnapshotQuantitativos.hashJson(mapper, o); }
    protected record Foto(long id, String estado, Integer versao, String status, LocalDate data, Long autorOriginal,
                          Long ultimaRetificacao, String fonteSha, String linhaSha, List<Evento> eventos) { }
    protected record Evento(long id, Integer versao, String tipo, LocalDate data, Long usuario, boolean sistema, boolean motivo31Dia,
                            boolean snapshotV2, String snapshotAntesSha, String snapshotNovoSha, String anterior,
                            String hash, Instant registradoEm, String linhaSha) { }
}
