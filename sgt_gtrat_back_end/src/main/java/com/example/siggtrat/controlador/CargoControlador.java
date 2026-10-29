package com.example.siggtrat.controlador;

import com.example.siggtrat.configuracao.InvalidaCacheNegocio;
import com.example.siggtrat.dto.CargoDTO;
import com.example.siggtrat.seguranca.ExigirSenha;
import com.example.siggtrat.servico.CargoServico;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@InvalidaCacheNegocio
@RestController
@RequestMapping("/api/cargos")
public class CargoControlador {
    private final CargoServico servico;
    public CargoControlador(CargoServico servico) { this.servico = servico; }

    @GetMapping
    public Page<CargoDTO> listarTodos(Pageable pageable) { return servico.listarTodos(pageable); }

    @GetMapping("/{id}")
    public ResponseEntity<CargoDTO> buscarPorId(@PathVariable Long id) {
        CargoDTO dto = servico.buscarPorId(id);
        return dto == null ? ResponseEntity.notFound().build() : ResponseEntity.ok(dto);
    }

    @PostMapping
    @ExigirSenha
    public CargoDTO criar(@Valid @RequestBody CargoDTO dto) { return servico.criar(dto); }

    @PutMapping("/{id}")
    @ExigirSenha
    public ResponseEntity<CargoDTO> atualizar(@PathVariable Long id, @Valid @RequestBody CargoDTO dto) {
        CargoDTO updated = servico.atualizar(id, dto);
        return updated == null ? ResponseEntity.notFound().build() : ResponseEntity.ok(updated);
    }

    @DeleteMapping("/{id}")
    @ExigirSenha
    public ResponseEntity<Void> excluir(@PathVariable Long id) {
        return servico.excluir(id) ? ResponseEntity.noContent().build() : ResponseEntity.notFound().build();
    }
}
