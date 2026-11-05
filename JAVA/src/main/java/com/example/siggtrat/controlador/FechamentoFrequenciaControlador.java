package com.example.siggtrat.controlador;

import com.example.siggtrat.configuracao.InvalidaCacheNegocio;
import com.example.siggtrat.dto.RequisicaoCriacaoFechamentoFrequencia;
import com.example.siggtrat.dto.FechamentoFrequenciaDTO;
import com.example.siggtrat.seguranca.ExigirSenha;
import com.example.siggtrat.servico.FechamentoFrequenciaServico;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@InvalidaCacheNegocio
@RestController
@RequestMapping("/api/frequencias/fechamentos")
public class FechamentoFrequenciaControlador {

    private final FechamentoFrequenciaServico servico;

    public FechamentoFrequenciaControlador(FechamentoFrequenciaServico servico) {
        this.servico = servico;
    }

    @PostMapping
    @ExigirSenha
    public ResponseEntity<FechamentoFrequenciaDTO> criar(
            @Valid @RequestBody RequisicaoCriacaoFechamentoFrequencia request) {
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(servico.criarOuReiniciar(request));
    }

    @GetMapping("/{id}")
    public ResponseEntity<FechamentoFrequenciaDTO> buscar(@PathVariable Long id) {
        return ResponseEntity.ok(servico.buscar(id));
    }

    @GetMapping("/atual")
    public ResponseEntity<FechamentoFrequenciaDTO> buscarAtual(
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate data,
            @RequestParam Long lotacaoId,
            @RequestParam Long jornadaId,
            @RequestParam(required = false) List<Long> contratoIds) {
        return servico.buscarAtual(data, lotacaoId, jornadaId, contratoIds)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.noContent().build());
    }

    @PostMapping("/{id}/cancelamento")
    public ResponseEntity<FechamentoFrequenciaDTO> cancelar(@PathVariable Long id) {
        return ResponseEntity.accepted().body(servico.solicitarCancelamento(id));
    }

    @PostMapping("/{id}/reprocessamento")
    @ExigirSenha
    public ResponseEntity<FechamentoFrequenciaDTO> reprocessar(@PathVariable Long id) {
        return ResponseEntity.accepted().body(servico.reprocessar(id));
    }
}
