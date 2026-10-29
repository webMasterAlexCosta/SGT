package com.example.siggtrat.controlador;

import com.example.siggtrat.configuracao.InvalidaCacheNegocio;
import com.example.siggtrat.dto.FrequenciaDTO;
import com.example.siggtrat.dto.EventoFrequenciaDTO;
import com.example.siggtrat.seguranca.ExigirSenha;
import com.example.siggtrat.servico.FrequenciaServico;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@InvalidaCacheNegocio
@RestController
@RequestMapping("/api/frequencias")
public class FrequenciaControlador {

    private final FrequenciaServico frequenciaServico;

    public FrequenciaControlador(FrequenciaServico frequenciaServico) {
        this.frequenciaServico = frequenciaServico;
    }

    @GetMapping
    public ResponseEntity<Page<FrequenciaDTO>> buscarPorData(
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate data,
            @RequestParam(required = false) Long lotacaoId,
            @RequestParam(required = false) Long jornadaId,
            @RequestParam(required = false) Long contratoId,
            @RequestParam(required = false) Long gestaoLocalId,
            @RequestParam(required = false) Long superintendenciaId,
            @RequestParam(defaultValue = "false") boolean skipCount,
            Pageable pageable) {
        return ResponseEntity.ok(frequenciaServico.buscarPorData(data, lotacaoId, jornadaId, contratoId,
                gestaoLocalId, superintendenciaId, skipCount, pageable));
    }

    @GetMapping("/mensal")
    public ResponseEntity<Page<FrequenciaDTO>> obterMensal(
            @RequestParam int mes,
            @RequestParam int ano,
            @RequestParam(required = false) Long lotacaoId,
            @RequestParam(required = false) Long jornadaId,
            @RequestParam(required = false) Long contratoId,
            @RequestParam(required = false) Long gestaoLocalId,
            @RequestParam(required = false) Long superintendenciaId,
            @RequestParam(defaultValue = "false") boolean skipCount,
            Pageable pageable) {
        return ResponseEntity.ok(frequenciaServico.obterMensal(mes, ano, lotacaoId, jornadaId, contratoId,
                gestaoLocalId, superintendenciaId, skipCount, pageable));
    }

    @GetMapping("/intervalo")
    public ResponseEntity<Page<FrequenciaDTO>> obterIntervalo(
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate inicio,
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate fim,
            @RequestParam(required = false) Long lotacaoId,
            @RequestParam(required = false) Long jornadaId,
            @RequestParam(required = false) Long contratoId,
            @RequestParam(required = false) Long gestaoLocalId,
            @RequestParam(required = false) Long superintendenciaId,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "false") boolean skipCount,
            Pageable pageable) {
        return ResponseEntity.ok(frequenciaServico.obterIntervalo(inicio, fim, lotacaoId, jornadaId, contratoId,
                gestaoLocalId, superintendenciaId, status, skipCount, pageable));
    }

    @GetMapping("/{id}/eventos")
    public ResponseEntity<List<EventoFrequenciaDTO>> listarEventos(@PathVariable Long id) {
        return ResponseEntity.ok(frequenciaServico.listarEventos(id));
    }

    @PostMapping
    @ExigirSenha
    public ResponseEntity<FrequenciaDTO> salvarOuAtualizar(@Valid @RequestBody FrequenciaDTO dto) {
        return ResponseEntity.ok(frequenciaServico.salvarOuAtualizar(dto));
    }

    @PostMapping("/lote")
    @ExigirSenha
    public ResponseEntity<List<FrequenciaDTO>> salvarOuAtualizarLote(
            @RequestBody List<@Valid FrequenciaDTO> itens) {
        return ResponseEntity.ok(frequenciaServico.salvarOuAtualizarLote(itens));
    }
}
