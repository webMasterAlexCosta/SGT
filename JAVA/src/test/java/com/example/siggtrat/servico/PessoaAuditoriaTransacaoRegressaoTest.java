package com.example.siggtrat.servico;

import com.example.siggtrat.entidade.*;
import com.example.siggtrat.repositorio.*;
import org.junit.jupiter.api.Test;
import org.springframework.aop.framework.ProxyFactory;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.annotation.AnnotationTransactionAttributeSource;
import org.springframework.transaction.interceptor.TransactionInterceptor;
import org.springframework.transaction.support.AbstractPlatformTransactionManager;
import org.springframework.transaction.support.DefaultTransactionStatus;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class PessoaAuditoriaTransacaoRegressaoTest {
    @Test
    void consultaRestritaAbreTransacaoDeEscritaParaAuditarUsoDaPermissao() {
        PessoaRepositorio pessoas = mock(PessoaRepositorio.class);
        VinculoContratualRepositorio vinculos = mock(VinculoContratualRepositorio.class);
        AcessoOrganizacionalServico acesso = mock(AcessoOrganizacionalServico.class);
        PermissaoExcepcionalServico permissoes = mock(PermissaoExcepcionalServico.class);
        UsuarioAplicacao usuario = new UsuarioAplicacao();
        usuario.setId(9L);
        Pessoa pessoa = new Pessoa();
        pessoa.setId(7L);
        pessoa.setNome("Pessoa Teste");
        pessoa.setCpf("12345678909");
        Contrato contrato = new Contrato();
        contrato.setId(1L);
        VinculoContratual vinculo = new VinculoContratual();
        vinculo.setContrato(contrato);
        when(pessoas.findById(7L)).thenReturn(Optional.of(pessoa));
        when(vinculos.findByPessoaIdOrderByDataInicioAscIdAsc(7L)).thenReturn(List.of(vinculo));
        when(acesso.usuarioAtual()).thenReturn(usuario);
        when(acesso.podeAcessarContrato(usuario, contrato)).thenReturn(true);
        AtomicBoolean auditou = new AtomicBoolean();
        when(permissoes.exigirERegistrarUso(eq(usuario), any(), eq(contrato), isNull(),
                eq("auditoria"), eq("Pessoa"), eq(7L))).thenAnswer(invocation -> {
            assertTrue(TransactionSynchronizationManager.isActualTransactionActive());
            assertFalse(TransactionSynchronizationManager.isCurrentTransactionReadOnly(),
                    "A auditoria não pode tentar persistir dentro de transação somente leitura.");
            auditou.set(true);
            return new PermissaoExcepcional();
        });
        PessoaServico servico = new PessoaServico(pessoas, mock(CorrecaoPessoaRepositorio.class), vinculos,
                acesso, new MinimizadorDadosPessoaisServico(), permissoes, mock(AuditoriaPessoaServico.class));
        ProxyFactory proxy = new ProxyFactory(servico);
        proxy.setProxyTargetClass(true);
        proxy.addAdvice(new TransactionInterceptor(new TransacaoDeTeste(), new AnnotationTransactionAttributeSource()));

        var resultado = ((PessoaServico) proxy.getProxy()).buscarDadosRestritos(7L, "auditoria");

        assertTrue(auditou.get());
        assertEquals("12345678909", resultado.cpf());
        assertFalse(TransactionSynchronizationManager.isActualTransactionActive());
    }

    private static final class TransacaoDeTeste extends AbstractPlatformTransactionManager {
        @Override protected Object doGetTransaction() { return new Object(); }
        @Override protected void doBegin(Object transaction, TransactionDefinition definition) { }
        @Override protected void doCommit(DefaultTransactionStatus status) { }
        @Override protected void doRollback(DefaultTransactionStatus status) { }
    }
}
