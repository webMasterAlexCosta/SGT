package com.example.siggtrat.controlador;

import com.example.siggtrat.configuracao.InvalidaCacheNegocio;
import com.example.siggtrat.dto.ContaDTO;
import com.example.siggtrat.seguranca.ExigirSenha;
import com.example.siggtrat.servico.ContaServico;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@InvalidaCacheNegocio
@RestController
@PreAuthorize("hasAuthority(T(com.example.siggtrat.constante.ConstantesDominio).PERFIL_ADMINISTRADOR)")
@RequestMapping("/api/contas")
public class ContaControlador {
    private final ContaServico servico;
    public ContaControlador(ContaServico servico) { this.servico = servico; }

    @GetMapping
    public Page<ContaDTO> listarTodos(Pageable pageable) { return servico.listarTodos(pageable); }

    @GetMapping("/{id}")
    public ResponseEntity<ContaDTO> buscarPorId(@PathVariable Long id) {
        ContaDTO dto = servico.buscarPorId(id);
        return dto == null ? ResponseEntity.notFound().build() : ResponseEntity.ok(dto);
    }

    @PostMapping @ExigirSenha
    public ContaDTO criar(@Valid @RequestBody ContaDTO dto) { return servico.criar(dto); }

    @PutMapping("/{id}") @ExigirSenha
    public ResponseEntity<ContaDTO> atualizar(@PathVariable Long id, @Valid @RequestBody ContaDTO dto) {
        return ResponseEntity.ok(servico.atualizar(id, dto));
    }

    @DeleteMapping("/{id}") @ExigirSenha
    public ResponseEntity<Void> excluir(@PathVariable Long id) {
        servico.excluir(id);
        return ResponseEntity.noContent().build();
    }
}
