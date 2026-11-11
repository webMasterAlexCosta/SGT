package com.example.siggtrat.controlador;

import com.example.siggtrat.configuracao.InvalidaCacheNegocio;
import com.example.siggtrat.dto.CorrecaoCnpjContratoDTO;
import com.example.siggtrat.dto.CorrecaoCnpjContratoRequisicao;
import com.example.siggtrat.seguranca.ExigirSenha;
import com.example.siggtrat.servico.CorrecaoCnpjContratoServico;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/contratos/{contratoId}/correcoes-cnpj")
@InvalidaCacheNegocio
public class CorrecaoCnpjContratoControlador implements com.example.siggtrat.diagnostico.DiagnosticoOperacao {
    private static final org.slf4j.Logger LOG_DIAGNOSTICO = org.slf4j.LoggerFactory.getLogger(CorrecaoCnpjContratoControlador.class);

    @Override public org.slf4j.Logger loggerDiagnostico() { return LOG_DIAGNOSTICO; }

    private final CorrecaoCnpjContratoServico servico;
    public CorrecaoCnpjContratoControlador(CorrecaoCnpjContratoServico servico) { this.servico = servico; }
    @PostMapping @ResponseStatus(HttpStatus.CREATED) @ExigirSenha
    public CorrecaoCnpjContratoDTO corrigir(@PathVariable Long contratoId, @Valid @RequestBody CorrecaoCnpjContratoRequisicao req) {
        return servico.corrigir(contratoId, req);
    }
    @GetMapping
    public List<CorrecaoCnpjContratoDTO> listar(@PathVariable Long contratoId) { return servico.listar(contratoId); }
}
