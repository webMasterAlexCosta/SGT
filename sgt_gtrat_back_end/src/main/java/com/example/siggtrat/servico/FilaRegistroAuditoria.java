package com.example.siggtrat.servico;

import com.example.siggtrat.configuracao.propriedades.PropriedadesRegistroAuditoria;
import com.example.siggtrat.configuracao.propriedades.PropriedadesAuditoria;
import com.example.siggtrat.entidade.RegistroAuditoria;
import com.example.siggtrat.repositorio.RegistroAuditoriaRepositorio;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

@Component
public class FilaRegistroAuditoria {

    private static final Logger log = LoggerFactory.getLogger(FilaRegistroAuditoria.class);

    private final RegistroAuditoriaRepositorio auditLogRepositorio;
    private final BlockingQueue<RegistroAuditoria> queue;
    private final int batchSize;
    private final Path fallbackLog;

    public FilaRegistroAuditoria(RegistroAuditoriaRepositorio auditLogRepositorio,
                          PropriedadesRegistroAuditoria auditLogProperties,
                          PropriedadesAuditoria auditProperties) {
        this.auditLogRepositorio = auditLogRepositorio;
        this.queue = new ArrayBlockingQueue<>(Math.max(1000, auditLogProperties.queue().capacity()));
        this.batchSize = Math.max(50, auditLogProperties.flush().batchSize());
        this.fallbackLog = auditProperties.fallbackLog();
    }

    public void enfileirarOuPersistir(RegistroAuditoria auditLog) {
        if (auditLog == null) return;
        if (!queue.offer(auditLog)) {
            log.warn("AUDIT buffer cheio. Persistindo de forma síncrona (fallback).");
            try {
                auditLogRepositorio.save(auditLog);
            } catch (Exception e) {
                log.error("CRÍTICO: falha ao persistir log síncrono. Dado pode ser perdido.", e);
            }
        }
    }

    @Scheduled(fixedDelayString = "${auditlog.flush.delay-ms}")
    @org.springframework.retry.annotation.Retryable(
            value = java.lang.Exception.class,
            maxAttemptsExpression = "${auditlog.flush.retry-attempts}",
            backoff = @org.springframework.retry.annotation.Backoff(
                    delayExpression = "${auditlog.flush.retry-delay-ms}",
                    multiplier = 2.0
            )
    )
    public void descarregar() {
        if (queue.isEmpty()) return;

        List<RegistroAuditoria> batch = new ArrayList<>(batchSize);
        queue.drainTo(batch, batchSize);
        if (batch.isEmpty()) return;

        try {
            auditLogRepositorio.saveAll(batch);
            auditLogRepositorio.flush();
        } catch (Exception e) {
            log.error("Erro ao persistir batch de auditoria. Devolvendo itens para a fila.", e);

            List<RegistroAuditoria> rejectedLogs = new ArrayList<>();
            for (RegistroAuditoria logItem : batch) {
                if (!queue.offer(logItem)) {
                    rejectedLogs.add(logItem);
                }
            }

            if (!rejectedLogs.isEmpty()) {
                try {
                    gravarLoteArquivoContingencia(rejectedLogs);
                } catch (Exception fallbackException) {
                    log.error("Falha ao registrar logs de auditoria no arquivo de fallback.", fallbackException);
                }
            }
        }
    }

    private void gravarLoteArquivoContingencia(List<RegistroAuditoria> batch) throws IOException {
        if (batch == null || batch.isEmpty()) return;

        Path absoluteFile = fallbackLog.toAbsolutePath().normalize();
        Path parent = absoluteFile.getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }

        List<String> lines = batch.stream()
                .filter(java.util.Objects::nonNull)
                .map(Object::toString)
                .toList();

        Files.write(absoluteFile, lines, StandardCharsets.UTF_8,
                StandardOpenOption.CREATE, StandardOpenOption.APPEND);
    }

    @PreDestroy
    public void descarregarAoEncerrar() {
        try {
            log.info("Encerrando aplicação. Tentando persistir logs restantes no buffer...");
            int remainingAttempts = 5;
            while (!queue.isEmpty() && remainingAttempts-- > 0) {
                descarregar();
            }
        } catch (Exception e) {
            log.error("Falha no shutdown do buffer de auditoria.", e);
        }
    }
}
