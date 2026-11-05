package com.example.siggtrat.etapa11;

import com.example.siggtrat.dominio.acesso.PerfilAcesso;
import com.example.siggtrat.dominio.relatorio.*;
import com.example.siggtrat.dto.*;
import com.example.siggtrat.entidade.*;
import com.example.siggtrat.repositorio.EmissaoRelatorioAdministrativoRepositorio;
import com.example.siggtrat.servico.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.*;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.retry.annotation.EnableRetry;
import org.springframework.transaction.*;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.transaction.interceptor.TransactionAspectSupport;
import org.springframework.transaction.support.*;

import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/** Exercita os interceptadores reais de transação/retry, sem simular um banco MySQL. */
class RelatorioAdministrativoConcorrenciaTest {
    @Test
    void repeticaoConcorrenteSoRetornaDepoisDeNovaTransacaoValida() {
        try (var contexto = new AnnotationConfigApplicationContext(Configuracao.class)) {
            var repositorio = contexto.getBean(EmissaoRelatorioAdministrativoRepositorio.class);
            var transacoes = contexto.getBean(Transacoes.class);
            var concorrente = new AtomicReference<EmissaoRelatorioAdministrativo>();
            when(repositorio.findByUsuarioIdAndChaveIdempotencia(anyLong(), anyString()))
                    .thenAnswer(inv -> Optional.ofNullable(concorrente.get()));
            when(repositorio.saveAndFlush(any())).thenAnswer(inv -> {
                concorrente.set(inv.getArgument(0));
                TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
                throw new DataIntegrityViolationException("Outra transação gravou a mesma chave");
            });

            var resposta = contexto.getBean(RelatorioAdministrativoServico.class).emitir(
                    new EmissaoRelatorioRequisicao(TipoRelatorioAdministrativo.FREQUENCIA_CONSOLIDADA,
                            FormatoEmissaoRelatorio.VISUALIZACAO, LocalDate.of(2026, 8, 1),
                            LocalDate.of(2026, 8, 31), List.of(10L), null, null, "idem-concorrente"));

            assertNotNull(resposta);
            assertEquals(2, transacoes.iniciadas, "O retry deve abrir nova transação após o rollback");
            assertEquals(1, transacoes.desfeitas);
            assertEquals(1, transacoes.confirmadas, "Não pode devolver sucesso a partir da transação abortada");
            verify(repositorio, times(1)).saveAndFlush(any());
        }
    }

    @Configuration(proxyBeanMethods = false)
    @EnableRetry
    @EnableTransactionManagement
    static class Configuracao {
        @Bean Transacoes transactionManager() { return new Transacoes(); }
        @Bean EmissaoRelatorioAdministrativoRepositorio repositorio() {
            return mock(EmissaoRelatorioAdministrativoRepositorio.class);
        }
        @Bean RelatorioAdministrativoServico relatorio(EmissaoRelatorioAdministrativoRepositorio repositorio) {
            var acesso = mock(AcessoOrganizacionalServico.class);
            var usuario = new UsuarioAplicacao();
            usuario.setId(9L);
            usuario.setUsername("gestor.nacional");
            usuario.setPerfil(PerfilAcesso.GESTOR_NACIONAL);
            when(acesso.usuarioAtual()).thenReturn(usuario);
            when(acesso.podeAcessarRelatoriosGestao(usuario)).thenReturn(true);
            when(acesso.ehGestorNacional(usuario)).thenReturn(true);
            var pendencias = mock(PendenciaAdministrativaServico.class);
            when(pendencias.avisoRelatorio(any(), any(), anyCollection(), isNull(), isNull()))
                    .thenReturn(AvisoRevisaoRelatorioDTO.definitivo());
            var fonte = mock(RelatorioFrequenciaConsolidadoServico.class);
            when(fonte.bloquearFontesParaEmissao(anyCollection())).thenReturn(List.of(10L));
            when(fonte.gerar(any(), any(), anyCollection(), isNull(), isNull()))
                    .thenReturn(new RelatorioFrequenciaConsolidadoDTO(true, "TESTE",
                            LocalDate.of(2026, 8, 1), LocalDate.of(2026, 8, 31), List.of(10L),
                            List.of(), new RelatorioFrequenciaConsolidadoDTO.Totais(
                                    0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0)));
            return new RelatorioAdministrativoServico(repositorio, fonte, pendencias, acesso,
                    mock(RegistroAuditoriaServico.class), new ObjectMapper().findAndRegisterModules(),
                    new com.example.siggtrat.servico.MinimizadorDadosPessoaisServico(),
                    mock(com.example.siggtrat.servico.InvalidacaoCacheServico.class));
        }
    }

    static class Transacoes extends AbstractPlatformTransactionManager {
        int iniciadas;
        int confirmadas;
        int desfeitas;
        @Override protected Object doGetTransaction() { return new Object(); }
        @Override protected void doBegin(Object transaction, TransactionDefinition definition) { iniciadas++; }
        @Override protected void doCommit(DefaultTransactionStatus status) { confirmadas++; }
        @Override protected void doRollback(DefaultTransactionStatus status) { desfeitas++; }
    }
}
