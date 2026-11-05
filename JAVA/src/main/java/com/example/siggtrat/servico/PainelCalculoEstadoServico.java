package com.example.siggtrat.servico;

import com.example.siggtrat.constante.ConstantesDominio;
import com.example.siggtrat.entidade.PainelCalculo;
import com.example.siggtrat.entidade.PainelCalculoStatus;
import com.example.siggtrat.repositorio.PainelCalculoRepositorio;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.annotation.Isolation;
import java.util.Objects;
import java.util.UUID;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Optional;

@Service
public class PainelCalculoEstadoServico {

    private final PainelCalculoRepositorio repositorio;
    private final RevisaoFontesPainelServico revisaoFontes;
    private final ZoneId zoneId = ZoneId.of(ConstantesDominio.TIMEZONE_PADRAO);

    public PainelCalculoEstadoServico(PainelCalculoRepositorio repositorio, RevisaoFontesPainelServico revisaoFontes) {
        this.repositorio = repositorio;
        this.revisaoFontes = revisaoFontes;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW, isolation = Isolation.READ_COMMITTED)
    public Optional<Descritor> reivindicar(Long id) {
        Optional<PainelCalculo> opt = repositorio.buscarPorIdParaAtualizacao(id);
        if (opt.isEmpty()) return Optional.empty();
        PainelCalculo calculo = opt.get();
        if (calculo.getStatus() == PainelCalculoStatus.PROCESSANDO
                || calculo.getStatus() == PainelCalculoStatus.CONCLUIDO) {
            return Optional.empty();
        }
        LocalDateTime agora = agora();
        calculo.setExecucaoToken(UUID.randomUUID().toString());
        calculo.setStatus(PainelCalculoStatus.PROCESSANDO);
        calculo.setProgresso(Math.max(5, calculo.getProgresso()));
        calculo.setIniciadoEm(agora);
        calculo.setHeartbeatEm(agora);
        calculo.setMensagem("Consolidando indicadores no servidor.");
        calculo.setDetalheErro(null);
        repositorio.save(calculo);
        return Optional.of(descritor(calculo));
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW, isolation = Isolation.READ_COMMITTED)
    public void concluir(Descritor execucao, String payloadJson) {
        if (payloadJson == null || payloadJson.isBlank() || "null".equalsIgnoreCase(payloadJson.trim())) {
            throw new IllegalArgumentException("Não é permitido concluir o painel sem payload válido.");
        }
        PainelCalculo calculo = repositorio.buscarPorIdParaAtualizacao(execucao.id()).orElseThrow();
        // Um worker anterior não pode concluir ou sobrescrever outra execução.
        if (!mesmaExecucao(calculo, execucao)) return;
        if (!Objects.equals(execucao.revisaoFontes(), revisaoFontes.atual())) {
            calculo.setStatus(PainelCalculoStatus.PENDENTE);
            calculo.setExecucaoToken(null);
            calculo.setHeartbeatEm(null);
            calculo.setProgresso(0);
            calculo.setMensagem("As fontes mudaram durante o cálculo. Nova consolidação necessária.");
            repositorio.save(calculo);
            return;
        }
        calculo.setRevisaoPayload(execucao.revisaoFontes());
        LocalDateTime agora = agora();
        calculo.setPayloadJson(payloadJson);
        calculo.setStatus(PainelCalculoStatus.CONCLUIDO);
        calculo.setProgresso(100);
        calculo.setHeartbeatEm(agora);
        calculo.setConcluidoEm(agora);
        calculo.setMensagem("Painel consolidado com sucesso.");
        calculo.setDetalheErro(null);
        repositorio.save(calculo);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW, isolation = Isolation.READ_COMMITTED)
    public void falhar(Descritor execucao, Throwable erro) {
        repositorio.buscarPorIdParaAtualizacao(execucao.id()).ifPresent(calculo -> {
            if (!mesmaExecucao(calculo, execucao)) return;
            calculo.setStatus(PainelCalculoStatus.ERRO);
            calculo.setProgresso(0);
            calculo.setHeartbeatEm(agora());
            calculo.setMensagem(mensagem(erro));
            calculo.setDetalheErro(rastroPilha(erro));
            repositorio.save(calculo);
        });
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW, isolation = Isolation.READ_COMMITTED)
    public void reenfileirar(Long id, String mensagem) {
        repositorio.buscarPorIdParaAtualizacao(id).ifPresent(calculo -> {
            if (calculo.getStatus() == PainelCalculoStatus.CONCLUIDO) return;
            calculo.setExecucaoToken(null);
            calculo.setStatus(PainelCalculoStatus.PENDENTE);
            calculo.setProgresso(0);
            calculo.setHeartbeatEm(null);
            calculo.setMensagem(mensagem);
            repositorio.save(calculo);
        });
    }

    private Descritor descritor(PainelCalculo p) {
        return new Descritor(
                p.getId(), p.getDataReferencia(), p.getPeriodoDias(), p.getExecucaoToken(), revisaoFontes.atual(),
                new PainelServico.PainelCalculoContexto(
                        p.getNivelEscopo(), p.getNomeEscopo(), p.getSuperintendenciaId(), p.getGestaoLocalId(),
                        p.isPermiteDetalhamentoOperacional(), p.isPodeVisualizarContratos(),
                        p.isPodeVisualizarEmpregados(), p.isPodeOperarFrequencia(), p.isPodeAcessarRelatorios()
                )
        );
    }

    private boolean mesmaExecucao(PainelCalculo calculo, Descritor execucao) {
        return calculo.getStatus() == PainelCalculoStatus.PROCESSANDO
                && execucao.token() != null && Objects.equals(calculo.getExecucaoToken(), execucao.token());
    }

    private LocalDateTime agora() { return LocalDateTime.now(zoneId); }

    private String mensagem(Throwable erro) {
        String valor = erro == null ? null : erro.getMessage();
        if (valor == null || valor.isBlank()) valor = "Falha ao consolidar o painel gerencial.";
        return valor.length() > 500 ? valor.substring(0, 500) : valor;
    }

    private String rastroPilha(Throwable erro) {
        if (erro == null) return null;
        StringWriter sw = new StringWriter();
        erro.printStackTrace(new PrintWriter(sw));
        String valor = sw.toString();
        return valor.length() > 12000 ? valor.substring(0, 12000) : valor;
    }

    public record Descritor(
            Long id,
            java.time.LocalDate referencia,
            int periodoDias,
            String token,
            String revisaoFontes,
            PainelServico.PainelCalculoContexto contexto
    ) {}
}
