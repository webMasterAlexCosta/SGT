package com.example.siggtrat.auditoria20261029;

import com.example.siggtrat.entidade.Frequencia;
import com.example.siggtrat.servico.AuditoriaFrequenciaServico;
import com.example.siggtrat.servico.FilaRegistroAuditoria;
import com.example.siggtrat.servico.PoliticaTemporalFrequenciaServico;
import com.example.siggtrat.utilitario.RelogioSistema;
import com.example.siggtrat.utilitario.UtilitarioHashSeguranca;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import javax.sql.DataSource;
import java.sql.Connection;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.time.temporal.TemporalAdjusters;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

/** ROP014/015: política/HTTP/JPA reais com N30; somente leitura do relógio controlada. */
@TestPropertySource(properties = "frequencia.janela-operacional-dias=30")
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class FrequenciaFronteiraTrintaDiasHttpIT extends ContratosHttpBase {
    @Autowired DataSource fonte;
    @Autowired PoliticaTemporalFrequenciaServico politica;
    @Autowired AuditoriaFrequenciaServico auditoria;
    @Autowired FilaRegistroAuditoria fila;

    @ParameterizedTest @CsvSource({"-1,200", "0,403", "1,403"})
    void apiAplicaTrintaDatasCivisELimiteExclusivoSemEfeitoNaRecusa(long nanos, int esperado) throws Exception {
        var c = criarCenarioPersistido();
        declararFonteNominalSintetica(c, "MESTRE-FRONTEIRA-TRINTA-SINTETICA");
        LocalDate data = RelogioSistema.hoje().minusDays(35).with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        assertThat(data).isAfterOrEqualTo(INICIO);
        assertThat(politica.diasJanela()).isEqualTo(30);
        ZonedDateTime limite = data.plusDays(30).atStartOfDay(RelogioSistema.ZONA);
        ZonedDateTime antesDoLimite = limite.minusSeconds(1);
        ZonedDateTime instante = limite.plusNanos(nanos);
        assertThat(politica.instanteFechamento(data)).isEqualTo(limite);
        String consulta = "/api/frequencias?data=" + data + "&contratoId=" + c.contratoA.getId();
        // Sessões reais são estabelecidas antes do controle exclusivo do relógio negocial.
        consultar(c.operacionalA, consulta, "Controle positivo de sessão operacional");
        consultar(c.localA, consulta, "Controle positivo de sessão do gestor");
        try (var relogio = Mockito.mockStatic(ZonedDateTime.class, Mockito.CALLS_REAL_METHODS)) {
            relogio.when(() -> ZonedDateTime.now(RelogioSistema.ZONA)).thenReturn(antesDoLimite);
            var criada = enviar(c.operacionalA, "/api/frequencias", pedido(c, data, "FALTA"), 200,
                    "N30: criação legítima antes do fechamento");
            long id = criada.path("id").asLong();
            assertThat(LocalDateTime.parse(criada.path("prazoEdicao").asText())).isEqualTo(limite.toLocalDateTime());
            fila.descarregar();
            var antes = snapshot();
            var eventosAntes = consultar(c.localA, "/api/frequencias/" + id + "/eventos", "Evento original real");
            assertThat(eventosAntes.size()).isEqualTo(1);
            relogio.when(() -> ZonedDateTime.now(RelogioSistema.ZONA)).thenReturn(instante);
            var resposta = enviar(c.operacionalA, "/api/frequencias", pedido(c, data, "PRESENCA"), esperado,
                    "N30: um nanossegundo antes, no instante exclusivo e um nanossegundo depois");
            fila.descarregar();
            var depois = snapshot();
            var eventosDepois = consultar(c.localA, "/api/frequencias/" + id + "/eventos", "Cadeia após comando");
            if (esperado == 200) {
                assertThat(resposta.path("id").asLong()).isEqualTo(id);
                assertThat(resposta.path("status").asText()).isEqualTo("PRESENCA");
                assertThat(resposta.path("estadoNegocial").asText()).isEqualTo("ABERTA");
                assertThat(resposta.path("versaoNegocial").asLong()).isEqualTo(criada.path("versaoNegocial").asLong() + 1);
                assertThat(eventosDepois.size()).isEqualTo(2);
                assertThat(eventosDepois.get(0)).isEqualTo(eventosAntes.get(0));
                String persistido = emTransacao(() -> { em.clear(); return String.valueOf(em.find(Frequencia.class, id).getStatus()); });
                assertThat(persistido).isEqualTo("PRESENCA");
            } else {
                assertThat(resposta.path("code").asText()).isEqualTo("HTTP_403");
                assertThat(resposta.path("message").asText()).contains("FECHADA");
                assertThat(depois).isEqualTo(antes);
                assertThat(eventosDepois).isEqualTo(eventosAntes);
            }
            var pagina = consultar(c.localA, consulta, "Projeção temporal não altera a fonte");
            assertThat(pagina.path("totalElements").asInt()).isEqualTo(1);
            assertThat(pagina.path("content").get(0).path("estadoNegocial").asText()).isEqualTo(esperado == 200 ? "ABERTA" : "FECHADA");
            assertThat(snapshot()).isEqualTo(depois);
            assertThat(auditoria.evidenciasIntegridade(id).cadeiaIntegra()).isTrue();
            SnapshotQuantitativos.registrar(mapper, "frequencia-trinta", "limite-" + nanos,
                    mapa("janela", 30, "dataReferencia", data, "zona", RelogioSistema.ZONA.getId(),
                            "limiteExclusivo", limite, "instanteControlado", instante, "httpEsperado", esperado,
                            "antes", antes, "depois", depois, "eventosAntes", eventosAntes.size(),
                            "eventosDepois", eventosDepois.size(), "controleRelogio",
                            "Somente ZonedDateTime.now(zona), CALLS_REAL_METHODS; sem virada física/TCP/Chrome"));
        }
    }

    private Map<String, Object> pedido(Cenario c, LocalDate data, String status) {
        return mapa("vinculoContratualId", c.vinculoA.getId(), "dataReferencia", data, "status", status);
    }

    private Map<String, String> snapshot() throws Exception {
        Map<String, String> valores = new LinkedHashMap<>();
        try (Connection c = fonte.getConnection()) {
            assertThat(c.getMetaData().getDatabaseProductName()).isEqualTo("PostgreSQL");
            c.setReadOnly(true); c.setTransactionIsolation(Connection.TRANSACTION_REPEATABLE_READ); c.setAutoCommit(false);
            try {
                for (String tabela : List.of("frequencia", "evento_frequencia", "historico_empregado", "pessoa",
                        "vinculo_contratual", "alocacao_vinculo", "contrato", "registro_auditoria")) {
                    String filtro = tabela.equals("registro_auditoria")
                            ? " where entity_type in ('Frequencia','EventoFrequencia','HistoricoEmpregado')" : "";
                    // Identificadores fixos; todas as colunas/linhas são hasheadas, sem SQL/dados em artefato.
                    try (var stmt = c.createStatement(); var r = stmt.executeQuery("select coalesce(jsonb_agg(to_jsonb(x) "
                            + "order by to_jsonb(x)::text),'[]'::jsonb)::text from " + tabela + " x" + filtro)) {
                        assertThat(r.next()).isTrue(); valores.put(tabela, UtilitarioHashSeguranca.sha256Hexadecimal(r.getString(1)));
                    }
                }
                c.commit();
            } catch (Exception erro) { c.rollback(); throw erro; }
        }
        return Map.copyOf(valores);
    }
}
