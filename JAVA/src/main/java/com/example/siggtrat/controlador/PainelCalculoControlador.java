package com.example.siggtrat.controlador;

import com.example.siggtrat.dto.PainelCalculoDTO;
import com.example.siggtrat.servico.PainelCalculoServico;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/painel/calculos")
public class PainelCalculoControlador implements com.example.siggtrat.diagnostico.DiagnosticoOperacao {
    private static final org.slf4j.Logger LOG_DIAGNOSTICO = org.slf4j.LoggerFactory.getLogger(PainelCalculoControlador.class);

    @Override public org.slf4j.Logger loggerDiagnostico() { return LOG_DIAGNOSTICO; }


    private final PainelCalculoServico servico;

    public PainelCalculoControlador(PainelCalculoServico servico) {
        this.servico = servico;
    }

    @PostMapping
    public ResponseEntity<PainelCalculoDTO> solicitar(
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate referencia,
            @RequestParam(required = false, defaultValue = "1") Integer periodoDias,
            @RequestParam(required = false, defaultValue = "false") boolean forcar) {
        var resultado = servico.solicitar(referencia, periodoDias, forcar);
        return ResponseEntity.status(resultado.status()).body(resultado.corpo());
    }

    @GetMapping("/{id}")
    public ResponseEntity<PainelCalculoDTO> consultar(@PathVariable Long id) {
        return ResponseEntity.ok(servico.consultar(id));
    }
}
