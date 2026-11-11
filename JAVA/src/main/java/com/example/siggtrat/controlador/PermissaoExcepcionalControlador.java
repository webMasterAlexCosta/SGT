package com.example.siggtrat.controlador;

import com.example.siggtrat.configuracao.InvalidaCacheNegocio;
import com.example.siggtrat.dto.*;
import com.example.siggtrat.seguranca.ExigirSenha;
import com.example.siggtrat.servico.PermissaoExcepcionalServico;
import com.example.siggtrat.servico.ConcessaoLeituraOcorrenciaServico;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@InvalidaCacheNegocio
@RestController
@RequestMapping("/api/permissoes-excepcionais")
public class PermissaoExcepcionalControlador implements com.example.siggtrat.diagnostico.DiagnosticoOperacao {
    private static final org.slf4j.Logger LOG_DIAGNOSTICO = org.slf4j.LoggerFactory.getLogger(PermissaoExcepcionalControlador.class);

    @Override public org.slf4j.Logger loggerDiagnostico() { return LOG_DIAGNOSTICO; }

    private final PermissaoExcepcionalServico servico;
    private final ConcessaoLeituraOcorrenciaServico concessaoOcorrencia;

    public PermissaoExcepcionalControlador(PermissaoExcepcionalServico servico,
                                          ConcessaoLeituraOcorrenciaServico concessaoOcorrencia) {
        this.servico = servico;
        this.concessaoOcorrencia = concessaoOcorrencia;
    }

    @GetMapping
    public List<PermissaoExcepcionalDTO> listar() { return servico.listar(); }

    @PostMapping
    @ExigirSenha
    public ResponseEntity<PermissaoExcepcionalDTO> conceder(
            @Valid @RequestBody ConcessaoPermissaoExcepcionalRequisicao requisicao) {
        return ResponseEntity.status(HttpStatus.CREATED).body(requisicao.ocorrenciaId() == null
                ? servico.conceder(requisicao) : concessaoOcorrencia.conceder(requisicao));
    }

    @PostMapping("/{id}/revogar")
    @ExigirSenha
    public PermissaoExcepcionalDTO revogar(@PathVariable Long id,
                                            @Valid @RequestBody RevogacaoPermissaoExcepcionalRequisicao requisicao) {
        return servico.revogar(id, requisicao);
    }

    @GetMapping("/{id}/usos")
    public List<UsoPermissaoExcepcionalDTO> usos(@PathVariable Long id) {
        return servico.listarUsos(id);
    }
}
