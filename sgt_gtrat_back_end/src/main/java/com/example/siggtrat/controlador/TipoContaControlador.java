package com.example.siggtrat.controlador;

import com.example.siggtrat.configuracao.InvalidaCacheNegocio;
import com.example.siggtrat.dto.TipoContaDTO;
import com.example.siggtrat.seguranca.ExigirSenha;
import com.example.siggtrat.servico.TipoContaServico;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@InvalidaCacheNegocio
@RestController
@RequestMapping("/api/tipos-conta")
public class TipoContaControlador {
    private final TipoContaServico servico;
    public TipoContaControlador(TipoContaServico servico) { this.servico = servico; }

    @GetMapping
    public Page<TipoContaDTO> listarTodos(Pageable pageable) { return servico.listarTodos(pageable); }

    @GetMapping("/{id}")
    public ResponseEntity<TipoContaDTO> buscarPorId(@PathVariable Long id) {
        TipoContaDTO dto = servico.buscarPorId(id);
        return dto == null ? ResponseEntity.notFound().build() : ResponseEntity.ok(dto);
    }

    @PostMapping @ExigirSenha
    public TipoContaDTO criar(@Valid @RequestBody TipoContaDTO dto) { return servico.criar(dto); }

    @PutMapping("/{id}") @ExigirSenha
    public ResponseEntity<TipoContaDTO> atualizar(@PathVariable Long id, @Valid @RequestBody TipoContaDTO dto) {
        TipoContaDTO updated = servico.atualizar(id, dto);
        return updated == null ? ResponseEntity.notFound().build() : ResponseEntity.ok(updated);
    }

    @DeleteMapping("/{id}") @ExigirSenha
    public ResponseEntity<Void> excluir(@PathVariable Long id) {
        return servico.excluir(id) ? ResponseEntity.noContent().build() : ResponseEntity.notFound().build();
    }
}
