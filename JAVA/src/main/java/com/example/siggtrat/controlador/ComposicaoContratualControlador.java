package com.example.siggtrat.controlador;

import com.example.siggtrat.configuracao.InvalidaCacheNegocio;
import com.example.siggtrat.dto.ComposicaoContratualDTO;
import com.example.siggtrat.dto.ComposicaoContratualLeituraDTO;
import com.example.siggtrat.dto.ExcedenteComposicaoDTO;
import com.example.siggtrat.seguranca.ExigirSenha;
import com.example.siggtrat.servico.ComposicaoContratualServico;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.Instant;
import java.util.List;

@InvalidaCacheNegocio
@RestController
@RequestMapping("/api/contratos/{contratoId}/composicoes")
public class ComposicaoContratualControlador implements com.example.siggtrat.diagnostico.DiagnosticoOperacao {
    private static final org.slf4j.Logger LOG_DIAGNOSTICO = org.slf4j.LoggerFactory.getLogger(ComposicaoContratualControlador.class);

    @Override public org.slf4j.Logger loggerDiagnostico() { return LOG_DIAGNOSTICO; }

    private final ComposicaoContratualServico servico;

    public ComposicaoContratualControlador(ComposicaoContratualServico servico) {
        this.servico = servico;
    }

    @GetMapping
    public List<ComposicaoContratualLeituraDTO> listar(
            @PathVariable Long contratoId,
            @RequestParam(required = false) LocalDate data,
            @RequestParam(required = false) Instant conhecidoAte) {
        return servico.listarLeitura(contratoId, data, conhecidoAte);
    }

    @GetMapping("/excedentes")
    public List<ExcedenteComposicaoDTO> listarExcedentes(
            @PathVariable Long contratoId,
            @RequestParam(required = false) LocalDate data) {
        return servico.listarExcedentes(contratoId, data);
    }

    @PostMapping
    @ExigirSenha
    public ResponseEntity<ComposicaoContratualDTO> criar(
            @PathVariable Long contratoId,
            @RequestBody ComposicaoContratualDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(servico.criar(contratoId, dto));
    }

    @PutMapping("/{id}")
    @ExigirSenha
    public ComposicaoContratualDTO atualizar(
            @PathVariable Long contratoId,
            @PathVariable Long id,
            @RequestBody ComposicaoContratualDTO dto) {
        return servico.atualizar(contratoId, id, dto);
    }

    @DeleteMapping("/{id}")
    @ExigirSenha
    public ResponseEntity<Void> excluir(@PathVariable Long contratoId, @PathVariable Long id) {
        servico.excluir(contratoId, id);
        return ResponseEntity.noContent().build();
    }
}
