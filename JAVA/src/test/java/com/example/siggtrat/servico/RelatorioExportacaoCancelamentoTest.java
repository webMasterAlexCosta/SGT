package com.example.siggtrat.servico;

import com.example.siggtrat.dominio.acesso.NivelExposicaoDadosPessoais;
import com.example.siggtrat.dominio.acesso.PerfilAcesso;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.server.ResponseStatusException;

import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.LongConsumer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/** Ordenação determinística do executor; autorização e gerador são isolados aqui. */
class RelatorioExportacaoCancelamentoTest {
    @TempDir Path arquivos;
    private RelatorioExportacaoServico gerador;
    private RelatorioExportacaoAssincronaServico tarefas;
    private final ArrayDeque<Runnable> fila = new ArrayDeque<>();

    @BeforeEach
    void preparar() {
        var contexto = SecurityContextHolder.createEmptyContext();
        contexto.setAuthentication(new UsernamePasswordAuthenticationToken("owner", "", List.of()));
        SecurityContextHolder.setContext(contexto);
        gerador = mock(RelatorioExportacaoServico.class);
        var autorizacao = mock(AutorizacaoExportacaoRelatorioServico.class);
        var permissao = new AutorizacaoExportacaoRelatorioServico.Contexto(1L, "owner", PerfilAcesso.GESTOR_LOCAL,
                NivelExposicaoDadosPessoais.COMPLETO, null, null, null, List.of(), true, true, true, false, true);
        when(autorizacao.capturar("owner")).thenReturn(new AutorizacaoExportacaoRelatorioServico.Fotografia(permissao, Map.of("usuario:1", 0L)));
        tarefas = new RelatorioExportacaoAssincronaServico(gerador,
                new ArmazenamentoTemporarioRelatorioServico(arquivos.toString(), 3_600_000), autorizacao, fila::addLast, 3_600_000);
    }

    @AfterEach void limparContexto() { SecurityContextHolder.clearContext(); }

    @Test
    void cancelarAntesDoExecutorImpedeIniciarGeracaoERepeticaoMantemOTerminal() {
        var criada = tarefas.solicitarTerceirizados(null, null, null, false, null, "XLSX", 1L);
        assertThat(criada.status()).isEqualTo("NA_FILA");
        var cancelada = tarefas.cancelar(criada.id());
        assertThat(tarefas.cancelar(criada.id())).isEqualTo(cancelada);
        var autenticacao = SecurityContextHolder.getContext().getAuthentication();
        fila.removeFirst().run();
        assertThat(tarefas.consultar(criada.id()).status()).isEqualTo("CANCELADO");
        assertThat(tarefas.consultar(criada.id()).concluido()).isFalse();
        assertThat(tarefas.consultar(criada.id()).podeBaixar()).isFalse();
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isSameAs(autenticacao);
        assertThatThrownBy(() -> tarefas.prepararTransferencia(criada.id())).isInstanceOfSatisfying(ResponseStatusException.class,
                e -> assertThat(e.getStatusCode().value()).isEqualTo(409));
        verifyNoInteractions(gerador);
    }

    @Test
    void progressoCooperativoInterrompeGeradorDepoisDoCancelamentoSemContabilizarLote() throws Exception {
        AtomicReference<UUID> id = new AtomicReference<>();
        doAnswer(i -> {
            tarefas.cancelar(id.get());
            i.getArgument(5, LongConsumer.class).accept(1L);
            throw new AssertionError("Lote posterior ao cancelamento não pode ser aceito");
        }).when(gerador).prepararTerceirizadosXlsx(any(), any(), any(), any(), any(), any(LongConsumer.class));
        id.set(tarefas.solicitarTerceirizados(null, null, null, false, null, "XLSX", 1L).id());
        fila.removeFirst().run();
        var estado = tarefas.consultar(id.get());
        assertThat(estado.status()).isEqualTo("CANCELADO");
        assertThat(estado.processados()).isZero();
        assertThat(estado.concluido()).isFalse();
        assertThat(estado.falhou()).isFalse();
        assertThat(estado.podeBaixar()).isFalse();
    }
}
