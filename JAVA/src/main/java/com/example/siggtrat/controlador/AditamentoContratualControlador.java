package com.example.siggtrat.controlador;

import com.example.siggtrat.configuracao.InvalidaCacheNegocio;
import com.example.siggtrat.dto.*;
import com.example.siggtrat.seguranca.ExigirSenha;
import com.example.siggtrat.servico.AditamentoContratualServico;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@InvalidaCacheNegocio
@RestController
@RequestMapping("/api/contratos/{contratoId}/aditamentos")
public class AditamentoContratualControlador implements com.example.siggtrat.diagnostico.DiagnosticoOperacao {
    private static final org.slf4j.Logger LOG_DIAGNOSTICO = org.slf4j.LoggerFactory.getLogger(AditamentoContratualControlador.class);

    @Override public org.slf4j.Logger loggerDiagnostico() { return LOG_DIAGNOSTICO; }

    private final AditamentoContratualServico servico;

    public AditamentoContratualControlador(AditamentoContratualServico servico) {
        this.servico = servico;
    }

    @GetMapping
    public List<AditamentoContratualDTO> listar(@PathVariable Long contratoId) {
        return servico.listar(contratoId);
    }

    @PostMapping("/previa")
    public PreviaAditamentoContratualDTO previa(
            @PathVariable Long contratoId,
            @RequestBody AditamentoContratualRequisicao requisicao) {
        return servico.previa(contratoId, requisicao);
    }

    @PostMapping
    @ExigirSenha
    public ResponseEntity<AditamentoContratualDTO> registrar(
            @PathVariable Long contratoId,
            @RequestBody AditamentoContratualRequisicao requisicao) {
        return ResponseEntity.status(HttpStatus.CREATED).body(servico.registrar(contratoId, requisicao));
    }

    @PostMapping("/{aditamentoId}/retificar")
    @ExigirSenha
    public AditamentoContratualDTO retificar(
            @PathVariable Long contratoId,
            @PathVariable Long aditamentoId,
            @RequestBody AditamentoContratualRequisicao requisicao) {
        return servico.retificar(contratoId, aditamentoId, requisicao);
    }

    @PostMapping("/{aditamentoId}/cancelar")
    @ExigirSenha
    public AditamentoContratualDTO cancelar(
            @PathVariable Long contratoId,
            @PathVariable Long aditamentoId,
            @RequestBody AcaoCicloVidaContratoDTO acao) {
        return servico.cancelarProgramado(contratoId, aditamentoId, acao);
    }
}
