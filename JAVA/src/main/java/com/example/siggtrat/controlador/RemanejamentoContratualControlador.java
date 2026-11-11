package com.example.siggtrat.controlador;

import com.example.siggtrat.configuracao.InvalidaCacheNegocio;
import com.example.siggtrat.dto.*;
import com.example.siggtrat.seguranca.ExigirSenha;
import com.example.siggtrat.servico.RemanejamentoContratualServico;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@InvalidaCacheNegocio
@RestController
@RequestMapping("/api/contratos/{contratoId}/remanejamentos")
public class RemanejamentoContratualControlador implements com.example.siggtrat.diagnostico.DiagnosticoOperacao {
    private static final org.slf4j.Logger LOG_DIAGNOSTICO = org.slf4j.LoggerFactory.getLogger(RemanejamentoContratualControlador.class);

    @Override public org.slf4j.Logger loggerDiagnostico() { return LOG_DIAGNOSTICO; }

    private final RemanejamentoContratualServico servico;

    public RemanejamentoContratualControlador(RemanejamentoContratualServico servico) {
        this.servico = servico;
    }

    @GetMapping
    public List<RemanejamentoContratualDTO> listar(@PathVariable Long contratoId) {
        return servico.listar(contratoId);
    }

    @PostMapping("/previa")
    public PreviaRemanejamentoContratualDTO previa(
            @PathVariable Long contratoId,
            @RequestBody RemanejamentoContratualRequisicao requisicao) {
        return servico.previa(contratoId, requisicao);
    }

    @PostMapping
    @ExigirSenha
    public ResponseEntity<RemanejamentoContratualDTO> registrar(
            @PathVariable Long contratoId,
            @RequestBody RemanejamentoContratualRequisicao requisicao) {
        return ResponseEntity.status(HttpStatus.CREATED).body(servico.registrar(contratoId, requisicao));
    }

    @PostMapping("/{remanejamentoId}/retificar")
    @ExigirSenha
    public RemanejamentoContratualDTO retificar(
            @PathVariable Long contratoId,
            @PathVariable Long remanejamentoId,
            @RequestBody RemanejamentoContratualRequisicao requisicao) {
        return servico.retificar(contratoId, remanejamentoId, requisicao);
    }

    @PostMapping("/{remanejamentoId}/cancelar")
    @ExigirSenha
    public RemanejamentoContratualDTO cancelar(
            @PathVariable Long contratoId,
            @PathVariable Long remanejamentoId,
            @RequestBody AcaoCicloVidaContratoDTO acao) {
        return servico.cancelarProgramado(contratoId, remanejamentoId, acao);
    }
}
