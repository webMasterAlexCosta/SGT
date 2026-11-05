package com.example.siggtrat.servico;

import com.example.siggtrat.dto.ExportacaoRelatorioStatusDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.LongConsumer;

/**
 * Coordena exportações demoradas sem manter a requisição HTTP aberta durante a
 * geração. O trabalho continua no servidor, grava o arquivo em disco e expõe
 * progresso para a interface consultar periodicamente.
 */
@Service
public class RelatorioExportacaoAssincronaServico {

    private static final Logger LOGGER = LoggerFactory.getLogger(RelatorioExportacaoAssincronaServico.class);
    private static final long TOTAL_MAXIMO_ESTIMADO = 20_000_000L;

    private final RelatorioExportacaoServico exportacaoServico;
    private final ArmazenamentoTemporarioRelatorioServico armazenamentoTemporario;
    private final AutorizacaoExportacaoRelatorioServico autorizacao;
    private final Executor executor;
    private final Duration ttlConcluidos;
    private final ConcurrentHashMap<UUID, TarefaExportacao> tarefas = new ConcurrentHashMap<>();

    public RelatorioExportacaoAssincronaServico(
            RelatorioExportacaoServico exportacaoServico,
            ArmazenamentoTemporarioRelatorioServico armazenamentoTemporario,
            AutorizacaoExportacaoRelatorioServico autorizacao,
            @Qualifier("relatorioExportacaoExecutor") Executor executor,
            @Value("${app.relatorios.exportacao.status-ttl-ms:7200000}") long ttlConcluidosMs
    ) {
        this.exportacaoServico = exportacaoServico;
        this.armazenamentoTemporario = armazenamentoTemporario;
        this.autorizacao = autorizacao;
        this.executor = executor;
        this.ttlConcluidos = Duration.ofMillis(Math.max(Duration.ofMinutes(30).toMillis(), ttlConcluidosMs));
    }

    public ExportacaoRelatorioStatusDTO solicitarFrequencia(
            LocalDate inicio, LocalDate fim, Collection<Long> contratoIds, Long lotacaoId, Long jornadaId,
            String formato, Long totalEstimado
    ) {
        String tipo = validarFormato(formato);
        var contexto = exportacaoServico.criarContextoFrequencia(contratoIds, lotacaoId, jornadaId);
        String nome = exportacaoServico.nomeArquivoFrequencia(contexto, tipo);
        return iniciar(
                "Frequência mensal", tipo, nome, totalEstimado,
                progresso -> "PDF".equals(tipo)
                        ? exportacaoServico.prepararFrequenciaPdf(
                                inicio, fim, contratoIds, lotacaoId, jornadaId, contexto, progresso)
                        : exportacaoServico.prepararFrequenciaXlsx(
                                inicio, fim, contratoIds, lotacaoId, jornadaId, contexto, progresso));
    }

    public ExportacaoRelatorioStatusDTO solicitarAbsenteismo(
            LocalDate inicio, LocalDate fim, Collection<Long> contratoIds, Long lotacaoId, Long jornadaId,
            double meta, double limiteDisrupcao, String formato, Long totalEstimado
    ) {
        String tipo = validarFormato(formato);
        var contexto = exportacaoServico.criarContextoAbsenteismo(contratoIds, lotacaoId, jornadaId);
        String nome = exportacaoServico.nomeArquivoAbsenteismo(contexto, tipo);
        return iniciar(
                "Absenteísmo", tipo, nome, totalEstimado,
                progresso -> "PDF".equals(tipo)
                        ? exportacaoServico.prepararAbsenteismoPdf(
                                inicio, fim, contratoIds, lotacaoId, jornadaId,
                                meta, limiteDisrupcao, contexto, progresso)
                        : exportacaoServico.prepararAbsenteismoXlsx(
                                inicio, fim, contratoIds, lotacaoId, jornadaId,
                                meta, limiteDisrupcao, contexto, progresso));
    }

    public ExportacaoRelatorioStatusDTO solicitarTerceirizados(
            String busca, String status, Long contratoId, Boolean semContrato, Long lotacaoId,
            String formato, Long totalEstimado
    ) {
        String tipo = validarFormato(formato);
        String nome = "Relatorio_Visao_Consolidada." + tipo.toLowerCase(Locale.ROOT);
        return iniciar(
                "Visão consolidada", tipo, nome, totalEstimado,
                progresso -> "PDF".equals(tipo)
                        ? exportacaoServico.prepararTerceirizadosPdf(
                                busca, status, contratoId, semContrato, lotacaoId, progresso)
                        : exportacaoServico.prepararTerceirizadosXlsx(
                                busca, status, contratoId, semContrato, lotacaoId, progresso));
    }

    public ExportacaoRelatorioStatusDTO consultar(UUID id) {
        return tarefaDoUsuario(id).paraDto(armazenamentoTemporario);
    }

    /**
     * Autoriza o download ainda na thread HTTP autenticada e devolve um handle
     * imutável para a fase de streaming. O StreamingResponseBody é executado
     * posteriormente pelo MVC; por isso a transferência não deve depender do
     * SecurityContext da thread assíncrona.
     */
    public TransferenciaExportacao prepararTransferencia(UUID id) {
        TarefaExportacao tarefa = tarefaDoUsuario(id);
        synchronized (tarefa) {
            if (tarefa.status != Status.CONCLUIDO || tarefa.arquivo == null) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "A exportação ainda não está pronta para download.");
            }
            long tamanho = Math.max(tarefa.tamanhoBytes, armazenamentoTemporario.tamanho(tarefa.arquivo));
            if (tamanho <= 0L) {
                tarefa.status = Status.FALHA;
                tarefa.etapa = "Arquivo indisponível";
                tarefa.mensagem = "O relatório foi concluído, mas o arquivo temporário não está mais disponível.";
                tarefa.atualizadoEm = Instant.now();
                throw new ResponseStatusException(HttpStatus.GONE, "Arquivo temporário da exportação não está disponível.");
            }
            return new TransferenciaExportacao(
                    tarefa.id, tarefa.nomeArquivo, tarefa.formato, tarefa.arquivo, tamanho);
        }
    }

    /**
     * Transfere um handle previamente autorizado. Em falha de rede o arquivo
     * permanece no servidor e a tarefa volta a CONCLUIDO, permitindo retry sem
     * reconstruir 268 mil registros.
     */
    public void transferirArquivo(TransferenciaExportacao transferencia, OutputStream destino) throws IOException {
        if (transferencia == null) throw new IllegalArgumentException("Transferência da exportação é obrigatória.");
        if (destino == null) throw new IllegalArgumentException("Destino da exportação é obrigatório.");

        TarefaExportacao tarefa = tarefas.get(transferencia.id());
        if (tarefa == null) throw new IOException("Exportação não encontrada ou expirada durante o download.");

        Path arquivo = transferencia.arquivo();
        synchronized (tarefa) {
            // O handle foi autorizado na thread HTTP. Relê a autorização antes
            // do primeiro byte sem depender do SecurityContext da thread MVC.
            validarAutorizacaoAtual(tarefa);
            if (tarefa.arquivo == null || !tarefa.arquivo.equals(arquivo)) {
                throw new IOException("Arquivo temporário da exportação não está mais disponível.");
            }
            if (tarefa.status != Status.CONCLUIDO) {
                throw new IOException("A exportação não está disponível para uma nova transferência.");
            }
            tarefa.status = Status.BAIXANDO;
            tarefa.etapa = "Baixando arquivo";
            tarefa.mensagem = "O download está sendo iniciado.";
            tarefa.atualizadoEm = Instant.now();
        }

        try {
            armazenamentoTemporario.transferir(arquivo, destino);
            synchronized (tarefa) {
                armazenamentoTemporario.excluir(arquivo);
                tarefa.arquivo = null;
                tarefa.status = Status.BAIXADO;
                tarefa.etapa = "Download concluído";
                tarefa.mensagem = "Arquivo baixado com sucesso.";
                tarefa.atualizadoEm = Instant.now();
            }
        } catch (IOException e) {
            synchronized (tarefa) {
                // A geração já terminou e continua íntegra no disco. Não apaga
                // o arquivo por uma falha do cliente/proxy durante o download.
                if (tarefa.arquivo != null && tarefa.arquivo.equals(arquivo)) {
                    tarefa.status = Status.CONCLUIDO;
                    tarefa.etapa = "Arquivo pronto";
                    tarefa.mensagem = "O download foi interrompido. Clique em Baixar arquivo para tentar novamente.";
                    tarefa.atualizadoEm = Instant.now();
                }
            }
            throw e;
        }
    }

    public record TransferenciaExportacao(
            UUID id, String nomeArquivo, String formato, Path arquivo, long tamanhoBytes
    ) {}

    private ExportacaoRelatorioStatusDTO iniciar(
            String relatorio, String formato, String nomeArquivo, Long totalEstimado, GeradorAssincrono gerador
    ) {
        Authentication autenticacao = autenticacaoAtual();
        var permissao = autorizacao.capturar(autenticacao.getName()).contexto();
        long total = normalizarTotal(totalEstimado);
        TarefaExportacao tarefa = new TarefaExportacao(
                UUID.randomUUID(), autenticacao.getName(), permissao, relatorio, formato, nomeArquivo, total);
        tarefas.put(tarefa.id, tarefa);

        SecurityContext contexto = SecurityContextHolder.createEmptyContext();
        contexto.setAuthentication(autenticacao);

        try {
            executor.execute(() -> executarComContexto(tarefa, gerador, contexto));
        } catch (RuntimeException e) {
            tarefas.remove(tarefa.id);
            throw new ResponseStatusException(
                    HttpStatus.SERVICE_UNAVAILABLE,
                    "A fila de exportações está cheia. Aguarde a conclusão de uma exportação e tente novamente.", e);
        }
        return tarefa.paraDto(armazenamentoTemporario);
    }

    private void executarComContexto(TarefaExportacao tarefa, GeradorAssincrono gerador, SecurityContext contexto) {
        SecurityContext anterior = SecurityContextHolder.getContext();
        SecurityContextHolder.setContext(contexto);
        Path arquivoGerado = null;
        try {
            var inicioGeracao = validarAutorizacaoAtual(tarefa);
            tarefa.iniciarProcessamento();
            LongConsumer progresso = tarefa::registrarLote;
            arquivoGerado = gerador.gerar(progresso);
            var fimGeracao = validarAutorizacaoAtual(tarefa);
            // Também detecta mudar e restaurar o perfil/escopo durante a
            // apuração. Um arquivo de política incerta não pode ser publicado.
            if (!inicioGeracao.revisoes().equals(fimGeracao.revisoes())) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                        "A autorização foi alterada durante a geração. Solicite uma nova exportação.");
            }
            tarefa.concluir(arquivoGerado, armazenamentoTemporario.tamanho(arquivoGerado));
        } catch (Exception e) {
            if (arquivoGerado != null) armazenamentoTemporario.excluir(arquivoGerado);
            if (tarefa.arquivo != null) armazenamentoTemporario.excluir(tarefa.arquivo);
            tarefa.falhar(e);
            LOGGER.error("Falha na exportação {} ({}) id={}: {}",
                    tarefa.relatorio, tarefa.formato, tarefa.id, e.getMessage(), e);
        } finally {
            SecurityContextHolder.setContext(anterior);
        }
    }

    private TarefaExportacao tarefaDoUsuario(UUID id) {
        if (id == null) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Identificador da exportação é obrigatório.");
        TarefaExportacao tarefa = tarefas.get(id);
        if (tarefa == null) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Exportação não encontrada ou expirada.");
        String usuario = autenticacaoAtual().getName();
        if (!tarefa.usuario.equals(usuario)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Esta exportação pertence a outro usuário.");
        }
        validarAutorizacaoAtual(tarefa);
        return tarefa;
    }

    private AutorizacaoExportacaoRelatorioServico.Fotografia validarAutorizacaoAtual(TarefaExportacao tarefa) {
        var atual = autorizacao.capturar(tarefa.usuario);
        if (!tarefa.autorizacao.equals(atual.contexto())) {
            // A recusa de consulta/download não altera a tarefa nem o arquivo.
            // Restabelecer a mesma autorização permite transferir os bytes originais.
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Seu perfil ou escopo atual não autoriza esta exportação.");
        }
        return atual;
    }

    private Authentication autenticacaoAtual() {
        Authentication autenticacao = SecurityContextHolder.getContext().getAuthentication();
        if (autenticacao == null || !autenticacao.isAuthenticated() || autenticacao.getName() == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Sessão inválida para exportação.");
        }
        return autenticacao;
    }

    private static String validarFormato(String formato) {
        String valor = formato == null ? "XLSX" : formato.trim().toUpperCase(Locale.ROOT);
        if (!List.of("XLSX", "PDF").contains(valor)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Formato deve ser XLSX ou PDF.");
        }
        return valor;
    }

    private static long normalizarTotal(Long total) {
        if (total == null || total <= 0) return 0L;
        return Math.min(total, TOTAL_MAXIMO_ESTIMADO);
    }

    @Scheduled(
            fixedDelayString = "${app.relatorios.exportacao.status-cleanup-ms:600000}",
            initialDelayString = "${app.relatorios.exportacao.status-cleanup-initial-delay-ms:600000}"
    )
    public void limparTarefasExpiradas() {
        Instant limite = Instant.now().minus(ttlConcluidos);
        tarefas.entrySet().removeIf(entry -> {
            TarefaExportacao tarefa = entry.getValue();
            if (tarefa.emAndamento() || tarefa.atualizadoEm.isAfter(limite)) return false;
            if (tarefa.arquivo != null) armazenamentoTemporario.excluir(tarefa.arquivo);
            return true;
        });
    }

    @FunctionalInterface
    private interface GeradorAssincrono {
        Path gerar(LongConsumer progresso) throws IOException;
    }

    private enum Status {
        NA_FILA,
        PROCESSANDO,
        CONCLUIDO,
        BAIXANDO,
        BAIXADO,
        FALHA
    }

    private static final class TarefaExportacao {
        private final UUID id;
        private final String usuario;
        private final AutorizacaoExportacaoRelatorioServico.Contexto autorizacao;
        private final String relatorio;
        private final String formato;
        private final String nomeArquivo;
        private final AtomicLong processados = new AtomicLong();
        private final AtomicLong totalEstimado;
        private final Instant criadoEm = Instant.now();

        private volatile Status status = Status.NA_FILA;
        private volatile String etapa = "Na fila";
        private volatile String mensagem = "Sua exportação foi adicionada à fila.";
        private volatile Instant iniciadoEm;
        private volatile Instant atualizadoEm = criadoEm;
        private volatile Path arquivo;
        private volatile long tamanhoBytes;

        private TarefaExportacao(
                UUID id, String usuario, AutorizacaoExportacaoRelatorioServico.Contexto autorizacao,
                String relatorio, String formato, String nomeArquivo, long totalEstimado
        ) {
            this.id = id;
            this.usuario = usuario;
            this.autorizacao = autorizacao;
            this.relatorio = relatorio;
            this.formato = formato;
            this.nomeArquivo = nomeArquivo;
            this.totalEstimado = new AtomicLong(totalEstimado);
        }

        private void iniciarProcessamento() {
            status = Status.PROCESSANDO;
            etapa = "Processando dados";
            mensagem = "Os dados do relatório estão sendo processados.";
            iniciadoEm = Instant.now();
            atualizadoEm = iniciadoEm;
        }

        private void registrarLote(long quantidade) {
            long atual = processados.addAndGet(Math.max(0L, quantidade));
            long total = totalEstimado.get();
            if (total > 0 && atual >= total) {
                etapa = "Finalizando arquivo";
                mensagem = "Os dados foram processados. Estamos finalizando o arquivo.";
            } else {
                etapa = "Preparando relatório";
                mensagem = "Processamento em andamento. Você pode continuar usando o sistema.";
            }
            atualizadoEm = Instant.now();
        }

        private void concluir(Path arquivo, long tamanhoBytes) {
            this.arquivo = arquivo;
            this.tamanhoBytes = Math.max(0L, tamanhoBytes);
            long atual = processados.get();
            totalEstimado.set(atual);
            status = Status.CONCLUIDO;
            etapa = "Arquivo pronto";
            mensagem = "Relatório concluído e pronto para download.";
            atualizadoEm = Instant.now();
        }

        private void falhar(Exception erro) {
            status = Status.FALHA;
            etapa = "Falha na exportação";
            String detalhe = erro == null ? "" : erro.getMessage();
            mensagem = detalhe == null || detalhe.isBlank()
                    ? "Não foi possível concluir o relatório. Tente novamente."
                    : "Não foi possível concluir o relatório: " + detalhe;
            atualizadoEm = Instant.now();
        }

        private boolean emAndamento() {
            return status == Status.NA_FILA || status == Status.PROCESSANDO || status == Status.BAIXANDO;
        }

        private ExportacaoRelatorioStatusDTO paraDto(ArmazenamentoTemporarioRelatorioServico armazenamento) {
            long quantidade = processados.get();
            long total = totalEstimado.get();
            int percentual = percentual(quantidade, total);
            long decorridos = iniciadoEm == null ? 0L
                    : Math.max(0L, Duration.between(iniciadoEm, Instant.now()).toSeconds());
            Long restantes = estimarRestante(quantidade, total, decorridos);
            long tamanho = arquivo == null ? tamanhoBytes : Math.max(tamanhoBytes, armazenamento.tamanho(arquivo));
            boolean concluido = status == Status.CONCLUIDO || status == Status.BAIXANDO || status == Status.BAIXADO;
            boolean falhou = status == Status.FALHA;
            return new ExportacaoRelatorioStatusDTO(
                    id, relatorio, formato, nomeArquivo, status.name(), etapa, mensagem,
                    quantidade, total, percentual, decorridos, restantes, tamanho,
                    concluido, falhou, status == Status.CONCLUIDO, iniciadoEm, atualizadoEm);
        }

        private int percentual(long processados, long total) {
            if (status == Status.CONCLUIDO || status == Status.BAIXANDO || status == Status.BAIXADO) return 100;
            if (status == Status.NA_FILA) return 0;
            if (total <= 0 || processados <= 0) return 1;
            return (int) Math.min(99L, Math.max(1L, Math.round((processados * 100.0) / total)));
        }

        private Long estimarRestante(long processados, long total, long decorridos) {
            if (status != Status.PROCESSANDO || total <= 0 || processados <= 0 || decorridos < 2 || processados >= total) {
                return null;
            }
            double porSegundo = processados / (double) decorridos;
            if (porSegundo <= 0.0) return null;
            return Math.max(1L, Math.round((total - processados) / porSegundo));
        }
    }
}
