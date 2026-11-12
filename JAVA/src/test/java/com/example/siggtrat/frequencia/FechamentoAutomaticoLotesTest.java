package com.example.siggtrat.frequencia;

import com.example.siggtrat.dominio.frequencia.EstadoNegocialFrequencia;
import com.example.siggtrat.entidade.Frequencia;
import com.example.siggtrat.repositorio.FrequenciaRepositorio;
import com.example.siggtrat.servico.*;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.*;
import org.springframework.data.domain.Pageable;
import org.springframework.transaction.*;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.transaction.support.*;

import java.time.LocalDate;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class FechamentoAutomaticoLotesTest {
    @Test
    void fechaMilEUmRegistrosComTransacoesLimitadasEAuditoriaPorRegistro() {
        try (var contexto = new AnnotationConfigApplicationContext()) {
            contexto.register(Configuracao.class, FechamentoAutomaticoFrequenciaNegocialServico.class);
            contexto.refresh();
            var dados = contexto.getBean(Dados.class);
            var transacoes = contexto.getBean(Transacoes.class);

            int fechadas = contexto.getBean(FechamentoAutomaticoFrequenciaNegocialServico.class).fecharVencidas();

            assertEquals(1001, fechadas);
            assertTrue(dados.registros.stream().allMatch(f -> f.getEstadoNegocial() == EstadoNegocialFrequencia.FECHADA));
            assertEquals(1001, dados.auditoriasPorTransacao.values().stream().mapToInt(Integer::intValue).sum());
            assertTrue(dados.auditoriasPorTransacao.values().stream().allMatch(n -> n <= 500),
                    "Uma transação não pode reter mais que o lote de 500 frequências e seus eventos");
            assertTrue(transacoes.confirmadas >= 3, "Os 1001 registros devem ser confirmados em pelo menos 3 lotes");
            verify(contexto.getBean(InvalidacaoCacheServico.class), times(3)).invalidarCachesDeNegocioAposCommit();
        }
    }

    static class Dados {
        final List<Frequencia> registros = new ArrayList<>();
        final Map<Integer, Integer> auditoriasPorTransacao = new HashMap<>();
        Dados() {
            for (long id = 1; id <= 1001; id++) {
                var frequencia = new Frequencia();
                frequencia.setId(id);
                frequencia.setDataReferencia(LocalDate.now(new PoliticaTemporalFrequenciaServico(7).zona()).minusDays(8));
                frequencia.setEstadoNegocial(EstadoNegocialFrequencia.ABERTA);
                frequencia.setVersaoNegocial(1);
                frequencia.setRegistradoEm(java.time.Instant.now().minus(java.time.Duration.ofDays(8)));
                registros.add(frequencia);
            }
        }
    }

    @Configuration(proxyBeanMethods = false)
    @EnableTransactionManagement(proxyTargetClass = true)
    static class Configuracao {
        @Bean Dados dados() { return new Dados(); }
        @Bean Transacoes transactionManager() { return new Transacoes(); }
        @Bean EntityManager entityManager() { return mock(EntityManager.class); }
        @Bean PoliticaTemporalFrequenciaServico politica() { return new PoliticaTemporalFrequenciaServico(7); }
        @Bean InvalidacaoCacheServico invalidacao() {
            var cache = mock(InvalidacaoCacheServico.class);
            doAnswer(inv -> {
                assertTrue(TransactionSynchronizationManager.isActualTransactionActive(),
                        "A revisão dos agregados deve participar da transação de cada lote");
                return null;
            }).when(cache).invalidarCachesDeNegocioAposCommit();
            return cache;
        }
        @Bean FrequenciaRepositorio repositorio(Dados dados) {
            return mock(FrequenciaRepositorio.class, invocacao -> {
                String nome = invocacao.getMethod().getName();
                if (nome.equals("saveAndFlush") || nome.equals("save")) return invocacao.getArgument(0);
                if (nome.equals("saveAll")) return invocacao.getArgument(0);
                if (List.class.isAssignableFrom(invocacao.getMethod().getReturnType())) {
                    int limite = Arrays.stream(invocacao.getArguments()).filter(Pageable.class::isInstance)
                            .map(Pageable.class::cast).mapToInt(Pageable::getPageSize).findFirst().orElse(Integer.MAX_VALUE);
                    return dados.registros.stream().filter(f -> f.getEstadoNegocial() == EstadoNegocialFrequencia.ABERTA)
                            .limit(limite).toList();
                }
                return RETURNS_DEFAULTS.answer(invocacao);
            });
        }
        @Bean AuditoriaFrequenciaServico auditoria(Dados dados, Transacoes transacoes) {
            var auditoria = mock(AuditoriaFrequenciaServico.class);
            when(auditoria.registrar(any(), any(), any(), any(), any(), any(), any())).thenAnswer(inv -> {
                assertTrue(TransactionSynchronizationManager.isActualTransactionActive(), "Estado e auditoria exigem uma transação");
                dados.auditoriasPorTransacao.merge(transacoes.iniciadas, 1, Integer::sum);
                return null;
            });
            return auditoria;
        }
    }

    static class Transacoes extends AbstractPlatformTransactionManager {
        private final ThreadLocal<EstadoTransacao> atual = new ThreadLocal<>();
        int iniciadas;
        int confirmadas;
        @Override protected Object doGetTransaction() {
            return atual.get() == null ? new EstadoTransacao() : atual.get();
        }
        @Override protected boolean isExistingTransaction(Object transaction) {
            return ((EstadoTransacao) transaction).ativo;
        }
        @Override protected void doBegin(Object transaction, TransactionDefinition definition) {
            iniciadas++;
            var estado = (EstadoTransacao) transaction;
            estado.ativo = true;
            atual.set(estado);
        }
        @Override protected void doCommit(DefaultTransactionStatus status) { confirmadas++; }
        @Override protected void doRollback(DefaultTransactionStatus status) { }
        @Override protected void doCleanupAfterCompletion(Object transaction) {
            ((EstadoTransacao) transaction).ativo = false;
            atual.remove();
        }
        static class EstadoTransacao { boolean ativo; }
    }
}
