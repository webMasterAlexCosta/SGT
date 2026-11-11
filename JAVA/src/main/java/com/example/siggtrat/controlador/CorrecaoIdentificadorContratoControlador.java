package com.example.siggtrat.controlador;

import com.example.siggtrat.configuracao.InvalidaCacheNegocio;
import com.example.siggtrat.dto.CorrecaoIdentificadorContratoDTO;
import com.example.siggtrat.dto.CorrecaoIdentificadorContratoRequisicao;
import com.example.siggtrat.seguranca.ExigirSenha;
import com.example.siggtrat.servico.CorrecaoIdentificadorContratoServico;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@InvalidaCacheNegocio
@RestController
@RequestMapping("/api/contratos")
public class CorrecaoIdentificadorContratoControlador implements com.example.siggtrat.diagnostico.DiagnosticoOperacao {
    private static final org.slf4j.Logger LOG_DIAGNOSTICO = org.slf4j.LoggerFactory.getLogger(CorrecaoIdentificadorContratoControlador.class);

    @Override public org.slf4j.Logger loggerDiagnostico() { return LOG_DIAGNOSTICO; }

    private final CorrecaoIdentificadorContratoServico servico;

    public CorrecaoIdentificadorContratoControlador(CorrecaoIdentificadorContratoServico servico) {
        this.servico = servico;
    }

    @PostMapping("/{contratoId}/correcoes-identificador")
    @ExigirSenha
    public ResponseEntity<CorrecaoIdentificadorContratoDTO> corrigir(
            @PathVariable Long contratoId,
            @Valid @RequestBody CorrecaoIdentificadorContratoRequisicao requisicao) {
        return ResponseEntity.status(HttpStatus.CREATED).body(servico.corrigir(contratoId, requisicao));
    }

    @GetMapping("/{contratoId}/correcoes-identificador")
    public List<CorrecaoIdentificadorContratoDTO> listar(@PathVariable Long contratoId) {
        return servico.listar(contratoId);
    }

    @GetMapping("/historico-identificador")
    public List<CorrecaoIdentificadorContratoDTO> pesquisar(@RequestParam String numero,
                                                             @RequestParam Integer ano) {
        return servico.pesquisarHistorico(numero, ano);
    }
}
