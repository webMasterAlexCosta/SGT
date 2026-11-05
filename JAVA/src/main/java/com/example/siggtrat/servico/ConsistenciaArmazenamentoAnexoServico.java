package com.example.siggtrat.servico;

import com.example.siggtrat.configuracao.propriedades.PropriedadesArmazenamentoArquivo;
import com.example.siggtrat.repositorio.AnexoOcorrenciaRepositorio;
import com.example.siggtrat.repositorio.DocumentoComprobatorioRepositorio;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.regex.Pattern;
import java.util.stream.Stream;

/**
 * Recupera o armazenamento de anexos depois de encerramentos abruptos.
 * Arquivos temporários são removidos e arquivos finais sem referência no banco
 * são isolados, nunca apagados silenciosamente.
 */
@Service
public class ConsistenciaArmazenamentoAnexoServico {

    private static final Logger log = LoggerFactory.getLogger(ConsistenciaArmazenamentoAnexoServico.class);
    private static final Pattern NOME_GERENCIADO = Pattern.compile(
            "^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}(?:\\.[a-z0-9]{1,10})?$",
            Pattern.CASE_INSENSITIVE
    );
    private static final Duration IDADE_MINIMA_RECUPERACAO = Duration.ofMinutes(15);

    private final AnexoOcorrenciaRepositorio repositorio;
    private final DocumentoComprobatorioRepositorio documentoRepositorio;
    private final Path uploadRoot;
    private final Path stagingRoot;
    private final Path orphanRoot;
    private final AtomicBoolean executando = new AtomicBoolean(false);

    public ConsistenciaArmazenamentoAnexoServico(AnexoOcorrenciaRepositorio repositorio,
                                          DocumentoComprobatorioRepositorio documentoRepositorio,
                                          PropriedadesArmazenamentoArquivo properties) {
        this.repositorio = repositorio;
        this.documentoRepositorio = documentoRepositorio;
        this.uploadRoot = properties.uploadDir().toAbsolutePath().normalize();
        this.stagingRoot = uploadRoot.resolve(".staging").normalize();
        this.orphanRoot = uploadRoot.resolve(".orphaned").normalize();
    }

    @EventListener(ApplicationReadyEvent.class)
    public void reconciliarAoIniciar() {
        reconciliar();
    }

    @Scheduled(
            fixedDelayString = "${app.upload.reconcile-delay-ms:3600000}",
            initialDelayString = "${app.upload.reconcile-initial-delay-ms:60000}"
    )
    public void reconciliarPeriodicamente() {
        reconciliar();
    }

    private void reconciliar() {
        if (!executando.compareAndSet(false, true)) return;
        try {
            Files.createDirectories(uploadRoot);
            Files.createDirectories(stagingRoot);
            Files.createDirectories(orphanRoot);
            limparTemporariosInterrompidos();

            Set<String> referenciados = new HashSet<>(repositorio.buscarTodosNomesArmazenados());
            referenciados.addAll(documentoRepositorio.buscarTodosNomesArmazenados());
            isolarArquivosOrfaos(referenciados);
            registrarReferenciasAusentes(referenciados);
        } catch (Exception ex) {
            log.error("Falha na verificação de consistência do armazenamento de anexos.", ex);
        } finally {
            executando.set(false);
        }
    }

    private void limparTemporariosInterrompidos() throws IOException {
        try (Stream<Path> arquivos = Files.list(stagingRoot)) {
            arquivos.filter(Files::isRegularFile)
                    .filter(this::podeRecuperar)
                    .forEach(this::excluirSilenciosamente);
        }
    }

    private void isolarArquivosOrfaos(Set<String> referenciados) throws IOException {
        try (Stream<Path> arquivos = Files.list(uploadRoot)) {
            arquivos.filter(Files::isRegularFile).forEach(arquivo -> {
                String nome = arquivo.getFileName().toString();
                if (!NOME_GERENCIADO.matcher(nome).matches()
                        || referenciados.contains(nome)
                        || !podeRecuperar(arquivo)) return;

                Path destino = destinoOrfaoUnico(nome);
                try {
                    Files.move(arquivo, destino, StandardCopyOption.REPLACE_EXISTING);
                    log.warn("Anexo físico sem registro foi isolado em: {}", destino);
                } catch (IOException ex) {
                    log.error("Não foi possível isolar o anexo órfão: {}", arquivo, ex);
                }
            });
        }
    }

    private void registrarReferenciasAusentes(Set<String> referenciados) {
        for (String nome : referenciados) {
            if (nome == null || nome.isBlank()) continue;
            try {
                String nomeSeguro = Path.of(nome).getFileName().toString();
                if (!nomeSeguro.equals(nome)) {
                    log.error("Registro de anexo contém caminho inválido no banco: {}", nome);
                    continue;
                }
                Path arquivo = uploadRoot.resolve(nomeSeguro).normalize();
                if (!arquivo.startsWith(uploadRoot) || !Files.isRegularFile(arquivo)) {
                    log.error("INTEGRIDADE: o banco referencia um anexo físico ausente: {}", nome);
                }
            } catch (RuntimeException ex) {
                log.error("Registro de anexo contém nome inválido no banco: {}", nome, ex);
            }
        }
    }


    private boolean podeRecuperar(Path arquivo) {
        try {
            Instant limite = Instant.now().minus(IDADE_MINIMA_RECUPERACAO);
            return Files.getLastModifiedTime(arquivo).toInstant().isBefore(limite);
        } catch (IOException ex) {
            log.warn("Não foi possível verificar a idade do arquivo: {}", arquivo, ex);
            return false;
        }
    }

    private Path destinoOrfaoUnico(String nome) {
        Path destino = orphanRoot.resolve(nome).normalize();
        if (!destino.startsWith(orphanRoot)) {
            throw new IllegalArgumentException("Nome de anexo órfão inválido.");
        }
        if (!Files.exists(destino)) return destino;

        String normalizado = nome.toLowerCase(Locale.ROOT);
        int ponto = normalizado.lastIndexOf('.');
        String base = ponto > 0 ? nome.substring(0, ponto) : nome;
        String extensao = ponto > 0 ? nome.substring(ponto) : "";
        return orphanRoot.resolve(base + "-duplicado-" + System.nanoTime() + extensao).normalize();
    }

    private void excluirSilenciosamente(Path arquivo) {
        try {
            Files.deleteIfExists(arquivo);
        } catch (IOException ex) {
            log.warn("Não foi possível remover arquivo temporário interrompido: {}", arquivo, ex);
        }
    }
}
