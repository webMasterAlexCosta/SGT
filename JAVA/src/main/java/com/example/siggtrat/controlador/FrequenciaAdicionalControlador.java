package com.example.siggtrat.controlador;

import com.example.siggtrat.configuracao.InvalidaCacheNegocio;
import com.example.siggtrat.dto.FrequenciaAdicionalDTO;
import com.example.siggtrat.dto.VersaoComplementoFrequenciaDTO;
import com.example.siggtrat.seguranca.ExigirSenha;
import com.example.siggtrat.servico.FrequenciaAdicionalServico;
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
@RequestMapping("/api/frequencias-adicionais")
public class FrequenciaAdicionalControlador {

    private final FrequenciaAdicionalServico servico;

    public FrequenciaAdicionalControlador(FrequenciaAdicionalServico servico) {
        this.servico = servico;
    }

    @GetMapping
    public ResponseEntity<Page<FrequenciaAdicionalDTO>> listar(
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate data,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate inicio,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate fim,
            @RequestParam(required = false) String tipo,
            @RequestParam(required = false) Long lotacaoId,
            @RequestParam(required = false) Long jornadaId,
            @RequestParam(required = false) Long contratoId,
            Pageable pageable) {
        return ResponseEntity.ok(servico.listar(data, inicio, fim, tipo, lotacaoId, jornadaId, contratoId, pageable));
    }

    @PostMapping
    @ExigirSenha
    public ResponseEntity<FrequenciaAdicionalDTO> criar(@RequestBody FrequenciaAdicionalDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(servico.criar(dto));
    }

    @PutMapping("/{id}")
    @ExigirSenha
    public ResponseEntity<FrequenciaAdicionalDTO> atualizar(@PathVariable Long id, @RequestBody FrequenciaAdicionalDTO dto) {
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
