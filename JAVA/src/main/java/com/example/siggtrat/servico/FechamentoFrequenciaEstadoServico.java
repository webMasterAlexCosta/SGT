package com.example.siggtrat.servico;

import com.example.siggtrat.constante.ConstantesDominio;
import com.example.siggtrat.entidade.FechamentoFrequencia;
import com.example.siggtrat.entidade.FechamentoFrequenciaStatus;
import com.example.siggtrat.repositorio.FechamentoFrequenciaRepositorio;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
public class FechamentoFrequenciaEstadoServico {

    private static final Logger log = LoggerFactory.getLogger(FechamentoFrequenciaEstadoServico.class);
    private static final ZoneId ZONE_ID = ZoneId.of(ConstantesDominio.TIMEZONE_PADRAO);

    private final FechamentoFrequenciaRepositorio repositorio;

    public FechamentoFrequenciaEstadoServico(FechamentoFrequenciaRepositorio repositorio) {
        this.repositorio = repositorio;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Optional<DescritorProcessamento> reivindicar(Long id) {
        FechamentoFrequencia f = repositorio.buscarPorIdParaAtualizacao(id).orElse(null);
        if (f == null || f.getStatus() != FechamentoFrequenciaStatus.PENDENTE) return Optional.empty();
        LocalDateTime agora = agora();
        f.setStatus(FechamentoFrequenciaStatus.PROCESSANDO);
        f.setIniciadoEm(f.getIniciadoEm() == null ? agora : f.getIniciadoEm());
        f.setHeartbeatEm(agora);
        f.setMensagem("Processamento assíncrono iniciado.");
        repositorio.save(f);
        return Optional.of(paraDescritor(f));
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW, readOnly = true)
    public boolean cancelamentoSolicitado(Long id) {
        return repositorio.findById(id)
                .map(f -> f.isCancelamentoSolicitado() || f.getStatus() == FechamentoFrequenciaStatus.CANCELADO)
                .orElse(true);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void registrarPreparacao(Long id) {
        FechamentoFrequencia f = repositorio.buscarPorIdParaAtualizacao(id).orElse(null);
        if (f == null || f.getStatus() != FechamentoFrequenciaStatus.PROCESSANDO) return;
        f.setHeartbeatEm(agora());
        f.setMensagem("Reconstruindo a elegibilidade histórica em lotes.");
        repositorio.save(f);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void definirTotalPrevisto(Long id, long totalPrevisto) {
        if (totalPrevisto <= 0) {
            throw new IllegalArgumentException("O fechamento não possui colaboradores elegíveis.");
        }
        FechamentoFrequencia f = repositorio.buscarPorIdParaAtualizacao(id).orElse(null);
        if (f == null || f.getStatus() != FechamentoFrequenciaStatus.PROCESSANDO) return;
        // Não reduz um total já apurado durante uma retomada. Isso mantém o
        // percentual monotônico mesmo se o efetivo mudar enquanto o job executa.
        if (f.getTotalPrevisto() <= 0) f.setTotalPrevisto(totalPrevisto);
        f.setHeartbeatEm(agora());
        f.setMensagem("Processamento iniciado para " + f.getTotalPrevisto() + " colaboradores.");
        repositorio.save(f);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void concluir(Long id) {
        FechamentoFrequencia f = repositorio.buscarPorIdParaAtualizacao(id).orElse(null);
        if (f == null || f.getStatus() != FechamentoFrequenciaStatus.PROCESSANDO) return;
        LocalDateTime agora = agora();
        f.setStatus(FechamentoFrequenciaStatus.CONCLUIDO);
        if (f.getTotalProcessado() != f.getTotalPrevisto()) {
            // O efetivo pode mudar enquanto o trabalho está na fila. O total real
            // processado passa a ser a referência final do fechamento.
            f.setTotalPrevisto(f.getTotalProcessado());
        }
        f.setHeartbeatEm(agora);
        f.setConcluidoEm(agora);
        f.setMensagem("Fechamento concluído com sucesso.");
        f.setDetalheErro(null);
        repositorio.save(f);
        log.info("Fechamento de frequência concluído: id={} processados={} inseridos={} atualizados={} preservados={}",
                f.getId(), f.getTotalProcessado(), f.getTotalInserido(), f.getTotalAtualizado(), f.getTotalPreservado());
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void cancelar(Long id) {
        FechamentoFrequencia f = repositorio.buscarPorIdParaAtualizacao(id).orElse(null);
        if (f == null || f.getStatus() == FechamentoFrequenciaStatus.CONCLUIDO) return;
        LocalDateTime agora = agora();
        f.setStatus(FechamentoFrequenciaStatus.CANCELADO);
        f.setHeartbeatEm(agora);
        f.setConcluidoEm(agora);
        f.setMensagem("Fechamento cancelado após o último bloco confirmado.");
        repositorio.save(f);
        log.warn("Fechamento de frequência cancelado: id={} processados={}", f.getId(), f.getTotalProcessado());
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void falhar(Long id, Throwable erro) {
        FechamentoFrequencia f = repositorio.buscarPorIdParaAtualizacao(id).orElse(null);
        if (f == null) return;
        LocalDateTime agora = agora();
        f.setStatus(FechamentoFrequenciaStatus.ERRO);
        f.setHeartbeatEm(agora);
        f.setConcluidoEm(agora);
        String mensagem = erro == null || erro.getMessage() == null || erro.getMessage().isBlank()
                ? "Falha inesperada no processamento do fechamento."
                : erro.getMessage();
        f.setMensagem(mensagem.length() > 500 ? mensagem.substring(0, 500) : mensagem);
        f.setDetalheErro(detalhar(erro));
        repositorio.save(f);
        log.error("Fechamento de frequência falhou: id={} processados={} mensagem={}",
                f.getId(), f.getTotalProcessado(), f.getMensagem(), erro);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public List<Long> reenfileirarTravados(LocalDateTime limite) {
        List<FechamentoFrequencia> travados = repositorio.buscarTravados(
                FechamentoFrequenciaStatus.PROCESSANDO, limite);
        List<Long> ids = new ArrayList<>();
        for (FechamentoFrequencia f : travados) {
            f.setStatus(FechamentoFrequenciaStatus.PENDENTE);
            f.setMensagem("Trabalho reenfileirado após ausência de heartbeat.");
            f.setHeartbeatEm(null);
            repositorio.save(f);
            ids.add(f.getId());
        }
        return ids;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW, readOnly = true)
    public List<Long> pendentes() {
        return repositorio.findTop50ByStatusOrderByCriadoEmAsc(FechamentoFrequenciaStatus.PENDENTE)
                .stream().map(FechamentoFrequencia::getId).toList();
    }

    private static LocalDateTime agora() {
        return LocalDateTime.now(ZONE_ID);
    }

    private DescritorProcessamento paraDescritor(FechamentoFrequencia f) {
        return new DescritorProcessamento(
                f.getId(),
                f.getDataReferencia(),
                f.getLotacao().getId(),
                f.getJornada().getId(),
                FechamentoFrequenciaServico.desserializarIds(f.getContratoIds()),
                f.getUsuarioCriador().getId(),
                f.isSomenteAlteracoes(),
                f.getTotalPrevisto(),
                f.getUltimoEmpregadoIdProcessado() == null ? 0L : f.getUltimoEmpregadoIdProcessado()
        );
    }

    private String detalhar(Throwable erro) {
        if (erro == null) return null;
        StringBuilder detalhe = new StringBuilder();
        Throwable atual = erro;
        int nivel = 0;
        while (atual != null && nivel < 8) {
            if (nivel > 0) detalhe.append("\nCausado por: ");
            detalhe.append(atual.getClass().getSimpleName()).append(": ")
                    .append(atual.getMessage() == null ? "sem mensagem" : atual.getMessage());
            atual = atual.getCause();
            nivel++;
        }
        return detalhe.length() > 8000 ? detalhe.substring(0, 8000) : detalhe.toString();
    }

    public record DescritorProcessamento(
            Long id,
            LocalDate dataReferencia,
            Long lotacaoId,
            Long jornadaId,
            List<Long> contratoIds,
            Long usuarioCriadorId,
            boolean somenteAlteracoes,
            long totalPrevisto,
            long ultimoEmpregadoIdProcessado
    ) {
    }
}
