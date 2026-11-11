package com.example.siggtrat.controlador;

import com.example.siggtrat.configuracao.InvalidaCacheNegocio;
import com.example.siggtrat.dto.CalendarioEventoDTO;
import com.example.siggtrat.dto.CalendarioEventoRequisicao;
import com.example.siggtrat.dto.VersaoCalendarioEventoDTO;
import com.example.siggtrat.seguranca.ExigirSenha;
import com.example.siggtrat.servico.CalendarioEventoServico;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.Instant;
import java.util.List;

@InvalidaCacheNegocio
@RestController
@RequestMapping("/api/calendario-eventos")
public class CalendarioEventoControlador implements com.example.siggtrat.diagnostico.DiagnosticoOperacao {
    private static final org.slf4j.Logger LOG_DIAGNOSTICO = org.slf4j.LoggerFactory.getLogger(CalendarioEventoControlador.class);

    @Override public org.slf4j.Logger loggerDiagnostico() { return LOG_DIAGNOSTICO; }

    private final CalendarioEventoServico servico;
    public CalendarioEventoControlador(CalendarioEventoServico servico) { this.servico = servico; }

    @GetMapping
    public List<CalendarioEventoDTO> listar(
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate data,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate inicio,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate fim,
            @RequestParam(required = false) Long lotacaoId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant conhecidoAte) {
        return servico.listar(data, inicio, fim, lotacaoId, conhecidoAte);
    }

    @GetMapping("/{id}/eventos")
    public List<VersaoCalendarioEventoDTO> listarEventos(@PathVariable Long id,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant conhecidoAte) {
        return servico.listarEventos(id, conhecidoAte);
    }

    @PostMapping @ExigirSenha
    public ResponseEntity<CalendarioEventoDTO> criar(@Valid @RequestBody(required = false) CalendarioEventoRequisicao dados) {
        return ResponseEntity.status(HttpStatus.CREATED).body(servico.criar(dados));
    }

    @PutMapping("/{id}") @ExigirSenha
    public ResponseEntity<CalendarioEventoDTO> atualizar(@PathVariable Long id, @Valid @RequestBody CalendarioEventoRequisicao dados) {
        return ResponseEntity.ok(servico.atualizar(id, dados));
    }

    @DeleteMapping("/{id}") @ExigirSenha
    public ResponseEntity<Void> excluir(@PathVariable Long id,
            @RequestParam(required = false) String justificativa) {
        servico.excluir(id, justificativa);
        return ResponseEntity.noContent().build();
    }
}
