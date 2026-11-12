package com.example.siggtrat.controlador;

import com.example.siggtrat.dto.PainelVisoesDTO;
import com.example.siggtrat.servico.PainelVisoesServico;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDate;

@RestController
@RequestMapping("/api/painel/visoes")
public class PainelVisoesControlador implements com.example.siggtrat.diagnostico.DiagnosticoOperacao {
    private static final org.slf4j.Logger LOG = org.slf4j.LoggerFactory.getLogger(PainelVisoesControlador.class);
    @Override public org.slf4j.Logger loggerDiagnostico() { return LOG; }
    private final PainelVisoesServico servico;
    public PainelVisoesControlador(PainelVisoesServico servico) { this.servico = servico; }

    @GetMapping
    public PainelVisoesDTO obter(@RequestParam(defaultValue = "DIARIO") String granularidade,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate referencia,
            @RequestParam(required = false) Long contratoId, @RequestParam(required = false) String tipoUnidade,
            @RequestParam(required = false) Long unidadeId, @RequestParam(defaultValue = "true") boolean comparar,
            @RequestParam(defaultValue = "0") int paginaUnidades, @RequestParam(required = false) Long prestadorId) {
        return servico.obter(granularidade, referencia, contratoId, tipoUnidade, unidadeId, comparar, paginaUnidades, prestadorId);
    }
}
