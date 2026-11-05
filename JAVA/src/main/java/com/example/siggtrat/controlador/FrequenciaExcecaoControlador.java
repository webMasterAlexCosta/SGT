package com.example.siggtrat.controlador;

import com.example.siggtrat.configuracao.InvalidaCacheNegocio;
import com.example.siggtrat.dto.FrequenciaExcecaoDTO;
import com.example.siggtrat.dto.VersaoComplementoFrequenciaDTO;
import com.example.siggtrat.seguranca.ExigirSenha;
import com.example.siggtrat.servico.FrequenciaExcecaoServico;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.Instant;
import java.util.List;

@InvalidaCacheNegocio
@RestController
@RequestMapping("/api/frequencias-excecoes")
public class FrequenciaExcecaoControlador {

    private final FrequenciaExcecaoServico servico;

    public FrequenciaExcecaoControlador(FrequenciaExcecaoServico servico) {
        this.servico = servico;
    }

    @GetMapping
    public ResponseEntity<Page<FrequenciaExcecaoDTO>> listarAtivasPorData(
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate data,
            @RequestParam(required = false) Long lotacaoId,
            @RequestParam(required = false) Long jornadaId,
            @RequestParam(required = false) Long contratoId,
            Pageable pageable) {
        return ResponseEntity.ok(servico.listarAtivasPorData(data, lotacaoId, jornadaId, contratoId, pageable));
    }

    @PostMapping
    @ExigirSenha
    public ResponseEntity<FrequenciaExcecaoDTO> criar(@RequestBody FrequenciaExcecaoDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(servico.criar(dto));
    }

    @PutMapping("/{id}")
    @ExigirSenha
    public ResponseEntity<FrequenciaExcecaoDTO> atualizar(@PathVariable Long id, @RequestBody FrequenciaExcecaoDTO dto) {
        return ResponseEntity.ok(servico.atualizar(id, dto));
    }

    @DeleteMapping("/{id}")
    @ExigirSenha
    public ResponseEntity<Void> excluir(@PathVariable Long id,
            @RequestParam(required = false) String justificativa) {
        servico.excluir(id, justificativa);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}/eventos")
    public List<VersaoComplementoFrequenciaDTO> listarEventos(@PathVariable Long id,
            @RequestParam(required = false) Instant conhecidoAte) {
        return servico.listarEventos(id, conhecidoAte);
    }
}
