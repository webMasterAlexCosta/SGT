package com.example.siggtrat.servico;

import com.example.siggtrat.entidade.Fornecedor;
import com.example.siggtrat.repositorio.FornecedorRepositorio;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

@Service
public class RiscoContinuoServico {
    private final FornecedorRepositorio repositorio;
    public RiscoContinuoServico(FornecedorRepositorio repositorio) { this.repositorio = repositorio; }

    /** Reavalia diariamente a validade do monitoramento sem inventar score. */
    @Scheduled(cron = "${app.risco-continuo.cron:0 15 2 * * *}", zone = "America/Sao_Paulo")
    @Transactional
    public void atualizarVencimentos() {
        LocalDate hoje = LocalDate.now();
        for (Fornecedor fornecedor : repositorio.findAll()) {
            if (!Boolean.TRUE.equals(fornecedor.getAtivo())) continue;
            String status = fornecedor.getProximaAvaliacao() == null ? "SEM_AVALIACAO"
                    : fornecedor.getProximaAvaliacao().isBefore(hoje) ? "REVISAO_VENCIDA"
                    : fornecedor.getProximaAvaliacao().isBefore(hoje.plusDays(30)) ? "REVISAO_PROXIMA" : "EM_DIA";
            if (!status.equals(fornecedor.getMonitoramentoStatus())) {
                fornecedor.setMonitoramentoStatus(status);
                repositorio.save(fornecedor);
            }
        }
    }
}
