package com.example.siggtrat.configuracao.transacao;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.SimpleTransactionStatus;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import java.sql.SQLException;
import java.util.concurrent.atomic.AtomicInteger;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ExecutorTransacaoIdempotenteTest {
    @AfterEach void limparThread() { TransactionSynchronizationManager.clear(); }
    private RuntimeException falha(String state) {
        return new DataAccessResourceFailureException("conteudo-nao-deve-ir-para-log", new SQLException("payload-privado", state));
    }
    @Test void rollbackCompletoPrecedeNovaTentativaESomenteUltimaConfirma() {
        for (String estado : new String[]{"40001", "40P01"}) {
            var manager = mock(PlatformTransactionManager.class);
            var primeira = new SimpleTransactionStatus(); var segunda = new SimpleTransactionStatus();
            when(manager.getTransaction(any())).thenReturn(primeira, segunda);
            var chamadas = new AtomicInteger();
            assertEquals("feito", new ExecutorTransacaoIdempotente(manager).executar(() -> {
                if (chamadas.incrementAndGet() == 1) throw falha(estado);
                return "feito";
            }));
            assertEquals(2, chamadas.get()); var ordem = inOrder(manager);
            ordem.verify(manager).getTransaction(any()); ordem.verify(manager).rollback(primeira);
            ordem.verify(manager).getTransaction(any()); ordem.verify(manager).commit(segunda);
            verify(manager, never()).commit(primeira);
        }
    }
    @Test void tentativasSaoLimitadasETodasRollback() {
        var manager = mock(PlatformTransactionManager.class);
        when(manager.getTransaction(any())).thenAnswer(inv -> new SimpleTransactionStatus());
        var chamadas = new AtomicInteger(); var erro = falha("40001");
        assertSame(erro, assertThrows(RuntimeException.class, () -> new ExecutorTransacaoIdempotente(manager).executar(() -> { chamadas.incrementAndGet(); throw erro; })));
        assertEquals(3, chamadas.get()); verify(manager, times(3)).rollback(any()); verify(manager, never()).commit(any());
    }
    @Test void naoRepeteViolacaoDeUnicidadeNemErroDeNegocio() {
        for (RuntimeException erro : new RuntimeException[]{falha("23505"), new IllegalArgumentException("regra")}) {
            var manager = mock(PlatformTransactionManager.class); when(manager.getTransaction(any())).thenReturn(new SimpleTransactionStatus());
            var chamadas = new AtomicInteger();
            assertSame(erro, assertThrows(RuntimeException.class, () -> new ExecutorTransacaoIdempotente(manager).executar(() -> { chamadas.incrementAndGet(); throw erro; })));
            assertEquals(1, chamadas.get()); verify(manager, times(1)).rollback(any());
        }
    }
    @Test void transacaoAmbienteNaoEConfirmadaSuspensaNemRepetidaPeloExecutor() {
        var manager = mock(PlatformTransactionManager.class); var erro = falha("40001"); var chamadas = new AtomicInteger();
        TransactionSynchronizationManager.setActualTransactionActive(true);
        assertSame(erro, assertThrows(RuntimeException.class, () -> new ExecutorTransacaoIdempotente(manager).executar(() -> { chamadas.incrementAndGet(); throw erro; })));
        assertEquals(1, chamadas.get()); verifyNoInteractions(manager);
    }
    @Test void serializacaoNaConfirmacaoTambemReabreTransacaoCompleta() {
        var manager = mock(PlatformTransactionManager.class); var primeira = new SimpleTransactionStatus(); var segunda = new SimpleTransactionStatus();
        when(manager.getTransaction(any())).thenReturn(primeira, segunda);
        doThrow(falha("40001")).when(manager).commit(primeira);
        var chamadas = new AtomicInteger();
        assertEquals(2, new ExecutorTransacaoIdempotente(manager).executar(chamadas::incrementAndGet));
        verify(manager).commit(segunda);
    }
}
