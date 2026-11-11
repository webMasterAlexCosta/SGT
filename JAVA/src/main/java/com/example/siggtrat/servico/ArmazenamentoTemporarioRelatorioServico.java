package com.example.siggtrat.servico;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.time.Duration;
import java.time.Instant;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;

/**
 * Armazena exportações grandes em disco no servidor durante a geração.
 *
 * <p>O arquivo nasce com sufixo {@code .part}, recebe os lotes processados e só
 * é promovido ao nome final depois que o escritor XLSX/PDF é fechado com
 * sucesso. Dessa forma, nenhum consumidor confunde um arquivo incompleto com
 * um relatório íntegro.</p>
 */
@Service
public class ArmazenamentoTemporarioRelatorioServico implements com.example.siggtrat.diagnostico.DiagnosticoOperacao {
    @Override public org.slf4j.Logger loggerDiagnostico() { return LOGGER; }


    private static final Logger LOGGER = LoggerFactory.getLogger(ArmazenamentoTemporarioRelatorioServico.class);
    private static final int BUFFER_IO_BYTES = 1024 * 1024;

    private final Path diretorio;
    private final Duration ttl;
    private final Set<Path> arquivosAtivos = ConcurrentHashMap.newKeySet();

    public ArmazenamentoTemporarioRelatorioServico(
            @Value("${app.relatorios.temp.diretorio:}") String diretorioConfigurado,
            @Value("${app.relatorios.temp.ttl-ms:28800000}") long ttlMs
    ) {
        String configurado = diretorioConfigurado == null ? "" : diretorioConfigurado.trim();
        this.diretorio = configurado.isEmpty()
                ? Path.of(System.getProperty("java.io.tmpdir"), "sgt", "relatorios")
                : Path.of(configurado).toAbsolutePath().normalize();
        this.ttl = Duration.ofMillis(Math.max(ttlMs, Duration.ofMinutes(30).toMillis()));
    }

    @FunctionalInterface
    public interface GeradorArquivo {
        void gerar(OutputStream output) throws IOException;
    }

    /**
     * Gera um arquivo completo em disco e o mantém disponível para download.
     * Em falha, remove tanto o {@code .part} quanto qualquer arquivo final.
     */
    public Path gerarArquivoConcluido(String prefixo, String extensao, GeradorArquivo gerador) throws IOException {
        if (gerador == null) throw new com.example.siggtrat.excecao.ExcecaoValidacaoNegocio("Gerador da exportação é obrigatório.");

        garantirDiretorio();
        String extensaoSegura = normalizarExtensao(extensao);
        String prefixoSeguro = normalizarPrefixo(prefixo);
        Path parcial = Files.createTempFile(diretorio, prefixoSeguro + "-", extensaoSegura + ".part");
        Path concluido = parcial.resolveSibling(removerSufixoPart(parcial.getFileName().toString()));
        arquivosAtivos.add(parcial);

        try {
            try (OutputStream arquivo = new BufferedOutputStream(
                    Files.newOutputStream(parcial, StandardOpenOption.WRITE, StandardOpenOption.TRUNCATE_EXISTING),
                    BUFFER_IO_BYTES)) {
                gerador.gerar(arquivo);
                arquivo.flush();
            }

            arquivosAtivos.add(concluido);
            moverComoConcluido(parcial, concluido);
            arquivosAtivos.remove(parcial);
            return concluido;
        } catch (IOException | RuntimeException e) {
            arquivosAtivos.remove(parcial);
            arquivosAtivos.remove(concluido);
            excluirSilenciosamente(parcial);
            excluirSilenciosamente(concluido);
            throw e;
        } finally {
            arquivosAtivos.remove(parcial);
        }
    }

    /**
     * Transfere um arquivo já concluído sem removê-lo. É usado pelas exportações
     * assíncronas para permitir nova tentativa caso o navegador ou a rede
     * interrompa o download depois que o relatório já foi gerado com sucesso.
     */
    public void transferir(Path arquivoConcluido, OutputStream destino) throws IOException {
        if (arquivoConcluido == null) throw new com.example.siggtrat.excecao.ExcecaoValidacaoNegocio("Arquivo da exportação é obrigatório.");
        if (destino == null) throw new com.example.siggtrat.excecao.ExcecaoValidacaoNegocio("Destino da exportação é obrigatório.");
        arquivosAtivos.add(arquivoConcluido);
        try (InputStream arquivo = new BufferedInputStream(Files.newInputStream(arquivoConcluido), BUFFER_IO_BYTES)) {
            arquivo.transferTo(destino);
            destino.flush();
        } finally {
            arquivosAtivos.remove(arquivoConcluido);
        }
    }

    /**
     * Mantém o comportamento dos endpoints de download direto: transfere e remove o
     * temporário ao final da tentativa.
     */
    public void transferirEExcluir(Path arquivoConcluido, OutputStream destino) throws IOException {
        try {
            transferir(arquivoConcluido, destino);
        } finally {
            excluir(arquivoConcluido);
        }
    }

    /** Adapta o arquivo temporário aos endpoints síncronos de download. */
    public void gerarTransferirEExcluir(
            String prefixo,
            String extensao,
            OutputStream destino,
            GeradorArquivo gerador
    ) throws IOException {
        Path concluido = gerarArquivoConcluido(prefixo, extensao, gerador);
        transferirEExcluir(concluido, destino);
    }

    public long tamanho(Path caminho) {
        if (caminho == null) return 0L;
        try {
            return Files.size(caminho);
        } catch (IOException e) {
            return 0L;
        }
    }

    public void excluir(Path caminho) {
        if (caminho == null) return;
        arquivosAtivos.remove(caminho);
        excluirSilenciosamente(caminho);
    }

    /**
     * Rede de segurança para arquivos deixados por encerramento abrupto da JVM.
     * Arquivos pertencentes a exportações ativas nunca são removidos.
     */
    @Scheduled(
            fixedDelayString = "${app.relatorios.temp.cleanup-ms:1800000}",
            initialDelayString = "${app.relatorios.temp.cleanup-initial-delay-ms:300000}"
    )
    public void limparExpirados() {
        if (!Files.isDirectory(diretorio)) return;
        Instant limite = Instant.now().minus(ttl);
        try (Stream<Path> caminhos = Files.list(diretorio)) {
            caminhos.filter(Files::isRegularFile)
                    .filter(caminho -> !arquivosAtivos.contains(caminho))
                    .forEach(caminho -> excluirSeExpirado(caminho, limite));
        } catch (IOException e) {
            LOGGER.warn("ArmazenamentoTemporarioRelatorioServico: Não foi possível varrer temporários de relatórios em falha={}", com.example.siggtrat.diagnostico.DiagnosticoFalha.resumir(e));
        }
    }

    private void excluirSeExpirado(Path caminho, Instant limite) {
        try {
            if (Files.getLastModifiedTime(caminho).toInstant().isBefore(limite)) {
                Files.deleteIfExists(caminho);
            }
        } catch (IOException e) {
            LOGGER.warn("ArmazenamentoTemporarioRelatorioServico: Não foi possível remover temporário expirado de relatório falha={}", com.example.siggtrat.diagnostico.DiagnosticoFalha.resumir(e));
        }
    }

    private void garantirDiretorio() throws IOException {
        Files.createDirectories(diretorio);
    }

    private void moverComoConcluido(Path origem, Path destino) throws IOException {
        try {
            Files.move(origem, destino, StandardCopyOption.ATOMIC_MOVE);
        } catch (AtomicMoveNotSupportedException e) {
            Files.move(origem, destino, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    private void excluirSilenciosamente(Path caminho) {
        try {
            Files.deleteIfExists(caminho);
        } catch (IOException e) {
            LOGGER.warn("ArmazenamentoTemporarioRelatorioServico: Não foi possível remover temporário de relatório falha={}", com.example.siggtrat.diagnostico.DiagnosticoFalha.resumir(e));
        }
    }

    private static String removerSufixoPart(String nome) {
        return nome.endsWith(".part") ? nome.substring(0, nome.length() - ".part".length()) : nome;
    }

    private static String normalizarExtensao(String extensao) {
        String valor = extensao == null ? ".tmp" : extensao.trim().toLowerCase(Locale.ROOT);
        if (!valor.startsWith(".")) valor = "." + valor;
        return valor.matches("\\.[a-z0-9]{1,10}") ? valor : ".tmp";
    }

    private static String normalizarPrefixo(String prefixo) {
        String valor = prefixo == null ? "relatorio" : prefixo.trim();
        valor = valor.replaceAll("[^A-Za-z0-9_-]", "-");
        valor = valor.replaceAll("-+", "-");
        if (valor.isBlank()) valor = "relatorio";
        return valor.length() <= 48 ? valor : valor.substring(0, 48);
    }
}
