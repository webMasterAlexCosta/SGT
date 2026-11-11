package com.example.siggtrat.servico;

import com.example.siggtrat.configuracao.propriedades.PropriedadesRegistroAuditoria;
import com.example.siggtrat.configuracao.propriedades.PropriedadesAuditoria;
import com.example.siggtrat.entidade.RegistroAuditoria;
import com.fasterxml.jackson.databind.ObjectMapper;
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

    private final PersistenciaRegistroAuditoriaServico persistencia;
    private final BlockingQueue<RegistroAuditoria> queue;
    private final int batchSize;
    private final Path fallbackLog;
    private final ObjectMapper serializadorContingencia = new ObjectMapper().findAndRegisterModules();
    private final Object controleEnfileiramento = new Object();
    private boolean encerrando;
    // É o lote já drenado, não outra fila de produtores. Só descarregar/encerrar
    // (synchronized) acessam este estado e não drenam outro lote antes de salvá-lo.
    private List<RegistroAuditoria> lotePendente = List.of();

    public FilaRegistroAuditoria(PersistenciaRegistroAuditoriaServico persistencia,
                          PropriedadesRegistroAuditoria auditLogProperties,
                          PropriedadesAuditoria auditProperties) {
        this.persistencia = persistencia;
        this.queue = new ArrayBlockingQueue<>(Math.max(1000, auditLogProperties.queue().capacity()));
        this.batchSize = Math.max(50, auditLogProperties.flush().batchSize());
        this.fallbackLog = auditProperties.fallbackLog();
    }

    public void enfileirarOuPersistir(RegistroAuditoria auditLog) {
        if (auditLog == null) return;
        boolean emEncerramento;
        synchronized (controleEnfileiramento) {
            emEncerramento = encerrando;
            // O início do shutdown e o último offer são mutuamente exclusivos.
            if (!emEncerramento && queue.offer(auditLog)) return;
        }
        if (!emEncerramento) {
            log.warn("AUDIT buffer cheio. Persistindo de forma síncrona (fallback).");
        }
        try {
            // O bean separado abre REQUIRES_NEW apenas neste fallback,
            // sem reaproveitar a transação de negócio já confirmada.
            persistencia.salvar(auditLog);
        } catch (Exception e) {
            log.error("FilaRegistroAuditoria: Falha ao persistir auditoria síncrona. Gravando evento em contingência. falha={}", com.example.siggtrat.diagnostico.DiagnosticoFalha.resumir(e));
            try {
                gravarLoteArquivoContingencia(List.of(auditLog));
            } catch (IOException falhaContingencia) {
                var falha = new IllegalStateException("Falha ao preservar auditoria no banco e no arquivo de contingência.",
                        falhaContingencia);
                falha.addSuppressed(e);
                throw falha;
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
    public synchronized void descarregar() {
        if (!recuperarLotePendente()) return;
        if (queue.isEmpty()) return;

        List<RegistroAuditoria> batch = new ArrayList<>(batchSize);
        queue.drainTo(batch, batchSize);
        if (batch.isEmpty()) return;

        try {
            persistencia.salvarLote(batch);
        } catch (Exception e) {
            log.error("FilaRegistroAuditoria: Erro ao persistir batch de auditoria. Devolvendo itens para a fila. falha={}", com.example.siggtrat.diagnostico.DiagnosticoFalha.resumir(e));

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
                    // O espaço liberado já foi ocupado por produtores. Não perder o
                    // lote recusado nem retirar mais eventos enquanto ele está pendente.
                    lotePendente = List.copyOf(rejectedLogs);
                    log.error("Auditoria retida para nova tentativa: quantidade={} falhaBanco={} falhaArquivo={}", rejectedLogs.size(),
                            com.example.siggtrat.diagnostico.DiagnosticoFalha.resumir(e),
                            com.example.siggtrat.diagnostico.DiagnosticoFalha.resumir(fallbackException));
                }
            }
        }
    }

    private boolean recuperarLotePendente() {
        if (lotePendente.isEmpty()) return true;
        try {
            persistencia.salvarLote(lotePendente);
        } catch (Exception falhaBanco) {
            try {
                gravarLoteArquivoContingencia(lotePendente);
            } catch (Exception falhaArquivo) {
                log.error("Auditoria continua retida: quantidade={} falhaBanco={} falhaArquivo={}", lotePendente.size(),
                        com.example.siggtrat.diagnostico.DiagnosticoFalha.resumir(falhaBanco),
                        com.example.siggtrat.diagnostico.DiagnosticoFalha.resumir(falhaArquivo));
                return false;
            }
        }
        log.info("Lote de auditoria recuperado: quantidade={}", lotePendente.size());
        lotePendente = List.of();
        return true;
    }

    private synchronized void gravarLoteArquivoContingencia(List<RegistroAuditoria> batch) throws IOException {
        if (batch == null || batch.isEmpty()) return;

        Path absoluteFile = fallbackLog.toAbsolutePath().normalize();
        Path parent = absoluteFile.getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }

        List<String> lines = new ArrayList<>(batch.size());
        for (RegistroAuditoria evento : batch) {
            if (evento != null) lines.add(serializadorContingencia.writeValueAsString(evento));
        }

        Files.write(absoluteFile, lines, StandardCharsets.UTF_8,
                StandardOpenOption.CREATE, StandardOpenOption.APPEND);
    }

    @PreDestroy
    public synchronized void descarregarAoEncerrar() {
        // A mesma exclusão mútua de descarregar() aguarda um lote agendado em andamento.
        synchronized (controleEnfileiramento) {
            encerrando = true;
        }
        log.info("Encerrando aplicação. Persistindo todos os registros restantes de auditoria...");
        if (!recuperarLotePendente()) {
            // A causa técnica já foi registrada de forma segura. Conservar o lote
            // e tornar o encerramento incompleto explícito ao chamador.
            throw new IllegalStateException("Auditoria pendente não pôde ser preservada; lote mantido para nova tentativa.");
        }
        Exception falhaBanco = null;
        while (!queue.isEmpty()) {
            List<RegistroAuditoria> batch = new ArrayList<>(batchSize);
            queue.drainTo(batch, batchSize);
            if (batch.isEmpty()) break;
            if (falhaBanco == null) {
                try {
                    persistencia.salvarLote(batch);
                    continue;
                } catch (Exception e) {
                    falhaBanco = e;
                    log.error("FilaRegistroAuditoria: Banco indisponível no encerramento. Preservando os lotes restantes em contingência. falha={}", com.example.siggtrat.diagnostico.DiagnosticoFalha.resumir(e));
                }
            }
            try {
                gravarLoteArquivoContingencia(batch);
            } catch (Exception falhaContingencia) {
                // Não há novos produtores na fila nem outra descarga ativa; o espaço
                // liberado por drainTo comporta integralmente este lote para nova tentativa.
                queue.addAll(batch);
                var falha = new IllegalStateException(
                        "Falha ao preservar auditoria no encerramento: banco e contingência indisponíveis.",
                        falhaContingencia);
                falha.addSuppressed(falhaBanco);
                throw falha;
            }
        }
    }
}
