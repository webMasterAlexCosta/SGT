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
public class CargoControlador implements com.example.siggtrat.diagnostico.DiagnosticoOperacao {
    private static final org.slf4j.Logger LOG_DIAGNOSTICO = org.slf4j.LoggerFactory.getLogger(CargoControlador.class);

    @Override public org.slf4j.Logger loggerDiagnostico() { return LOG_DIAGNOSTICO; }

    private final CargoServico servico;
    public CargoControlador(CargoServico servico) { this.servico = servico; }

    @GetMapping
    public Page<CargoDTO> listarTodos(Pageable pageable) { return servico.listarTodos(pageable); }

    @GetMapping("/{id}")
    public ResponseEntity<CargoDTO> buscarPorId(@PathVariable Long id) {
        CargoDTO dto = servico.buscarPorId(id);
        if (dto == null) throw new org.springframework.web.server.ResponseStatusException(
                org.springframework.http.HttpStatus.NOT_FOUND, "Cargo não encontrado no seu escopo.");
        return ResponseEntity.ok(dto);
    }

    @PostMapping
    @ExigirSenha
    public CargoDTO criar(@Valid @RequestBody CargoDTO dto) { return servico.criar(dto); }

    @PutMapping("/{id}")
    @ExigirSenha
    public ResponseEntity<CargoDTO> atualizar(@PathVariable Long id, @Valid @RequestBody CargoDTO dto) {
        CargoDTO updated = servico.atualizar(id, dto);
        if (updated == null) throw new org.springframework.web.server.ResponseStatusException(
                org.springframework.http.HttpStatus.NOT_FOUND, "Cargo não encontrado no seu escopo.");
        return ResponseEntity.ok(updated);
    }

    @DeleteMapping("/{id}")
    @ExigirSenha
    public ResponseEntity<Void> excluir(@PathVariable Long id) {
        if (!servico.excluir(id)) throw new org.springframework.web.server.ResponseStatusException(
                org.springframework.http.HttpStatus.NOT_FOUND, "Cargo não encontrado no seu escopo.");
        return ResponseEntity.noContent().build();
    }
}
