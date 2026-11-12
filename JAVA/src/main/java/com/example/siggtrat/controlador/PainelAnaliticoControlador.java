package com.example.siggtrat.controlador;

import com.example.siggtrat.dto.PainelAnaliticoDTO;
import com.example.siggtrat.servico.PainelAnaliticoServico;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDate;

@RestController
@RequestMapping("/api/painel/analitico")
public class PainelAnaliticoControlador implements com.example.siggtrat.diagnostico.DiagnosticoOperacao {
    private static final org.slf4j.Logger LOG = org.slf4j.LoggerFactory.getLogger(PainelAnaliticoControlador.class);
    @Override public org.slf4j.Logger loggerDiagnostico() { return LOG; }
    private final PainelAnaliticoServico servico;
    public PainelAnaliticoControlador(PainelAnaliticoServico servico) { this.servico=servico; }
    @GetMapping
    public PainelAnaliticoDTO obter(@RequestParam(defaultValue="DIARIO") String granularidade,
            @RequestParam(required=false) @DateTimeFormat(iso=DateTimeFormat.ISO.DATE) LocalDate referencia,
            @RequestParam(required=false) Long contratoId,@RequestParam(required=false) String tipoUnidade,
            @RequestParam(required=false) Long unidadeId,@RequestParam(defaultValue="true") boolean comparar,
            @RequestParam(defaultValue="0") int paginaUnidades,@RequestParam(required=false) Long prestadorId) {
        return servico.obter(granularidade,referencia,contratoId,tipoUnidade,unidadeId,comparar,paginaUnidades,prestadorId);
    }
    @GetMapping("/registros")
    public PainelAnaliticoDTO.PaginaRegistros registros(
            @RequestParam @DateTimeFormat(iso=DateTimeFormat.ISO.DATE) LocalDate referencia,
            @RequestParam @DateTimeFormat(iso=DateTimeFormat.ISO.DATE) LocalDate inicio,
            @RequestParam @DateTimeFormat(iso=DateTimeFormat.ISO.DATE) LocalDate fim,
            @RequestParam(required=false) Long contratoId,@RequestParam(required=false) String tipoUnidade,
            @RequestParam(required=false) Long unidadeId,@RequestParam(defaultValue="registros") String metrica,
            @RequestParam(defaultValue="0") int pagina,@RequestParam(required=false) Long prestadorId) {
        return servico.registros(referencia,inicio,fim,contratoId,tipoUnidade,unidadeId,metrica,pagina,prestadorId);
    }
}
