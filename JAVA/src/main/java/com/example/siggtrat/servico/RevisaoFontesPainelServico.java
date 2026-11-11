package com.example.siggtrat.servico;

import com.example.siggtrat.entidade.RevisaoFontesPainel;
import com.example.siggtrat.repositorio.RevisaoFontesPainelRepositorio;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.UUID;

/**
 * Uma revisão compartilhada existe antes de qualquer job. A mudança participa
 * do commit da fonte; não há janela entre confirmar o negócio e invalidar o
 * agregado. A linha única também serializa escritores de instâncias distintas.
 */
@Service
@Order(Ordered.HIGHEST_PRECEDENCE)
public class RevisaoFontesPainelServico implements ApplicationRunner, com.example.siggtrat.diagnostico.DiagnosticoOperacao {
    private static final org.slf4j.Logger LOG_DIAGNOSTICO = org.slf4j.LoggerFactory.getLogger(RevisaoFontesPainelServico.class);

    @Override public org.slf4j.Logger loggerDiagnostico() { return LOG_DIAGNOSTICO; }

    private final RevisaoFontesPainelRepositorio repositorio;
    private final TransactionTemplate inicializacao;

    public RevisaoFontesPainelServico(RevisaoFontesPainelRepositorio repositorio,
                                      PlatformTransactionManager transactionManager) {
        this.repositorio = repositorio;
        this.inicializacao = new TransactionTemplate(transactionManager);
        this.inicializacao.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
    }

    @Override
    public void run(ApplicationArguments args) {
        try {
            inicializacao.executeWithoutResult(status -> {
                if (repositorio.valorAtual().isEmpty()) {
                    repositorio.saveAndFlush(new RevisaoFontesPainel(UUID.randomUUID().toString()));
                }
            });
        } catch (DataIntegrityViolationException concorrente) {
            // Duas instâncias podem iniciar juntas. A consulta ocorre somente
            // depois do rollback da tentativa de INSERT que perdeu a disputa.
            if (repositorio.valorAtual().isEmpty()) throw concorrente;
        }
    }

    @Transactional(readOnly = true)
    public String atual() {
        return repositorio.valorAtual().orElseThrow(() ->
                new IllegalStateException("Revisão técnica das fontes do painel não inicializada."));
    }

    @Transactional
    public void registrarMudanca() {
        if (repositorio.registrarMudanca(UUID.randomUUID().toString()) != 1) {
            throw new IllegalStateException("Revisão técnica das fontes do painel não inicializada.");
        }
    }
}
