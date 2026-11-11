package com.example.siggtrat.controlador;

import com.example.siggtrat.dto.HistoricoEmpregadoDTO;
import com.example.siggtrat.servico.HistoricoEmpregadoConsultaServico;
import com.example.siggtrat.servico.HistoricoIntegridadeServico;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/historicos-empregados")
public class HistoricoEmpregadoControlador implements com.example.siggtrat.diagnostico.DiagnosticoOperacao {
    private static final org.slf4j.Logger LOG_DIAGNOSTICO = org.slf4j.LoggerFactory.getLogger(HistoricoEmpregadoControlador.class);

    @Override public org.slf4j.Logger loggerDiagnostico() { return LOG_DIAGNOSTICO; }

    private final HistoricoEmpregadoConsultaServico servico;
    public HistoricoEmpregadoControlador(HistoricoEmpregadoConsultaServico servico) { this.servico = servico; }

    @GetMapping
    public Page<HistoricoEmpregadoDTO> listarTodos(Pageable pageable) { return servico.listarTodos(pageable); }

    @GetMapping("/empregado/{id}")
    public ResponseEntity<List<HistoricoEmpregadoDTO>> buscarPorEmpregado(@PathVariable Long id, Pageable pageable) {
        return ResponseEntity.ok(servico.buscarPorEmpregado(id, pageable));
    }

    @GetMapping("/empregado/{id}/integridade")
    public ResponseEntity<HistoricoIntegridadeServico.ResultadoVerificacao> verificarIntegridade(@PathVariable Long id) {
        return ResponseEntity.ok(servico.verificarIntegridade(id));
    }

    @GetMapping("/{id}")
    public ResponseEntity<HistoricoEmpregadoDTO> buscarPorId(@PathVariable Long id) {
        return ResponseEntity.ok(servico.buscarPorId(id));
    }
}
