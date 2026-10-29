package com.example.siggtrat.controlador;

import com.example.siggtrat.configuracao.InvalidaCacheNegocio;
import com.example.siggtrat.dto.TelefoneDTO;
import com.example.siggtrat.seguranca.ExigirSenha;
import com.example.siggtrat.servico.TelefoneServico;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@InvalidaCacheNegocio
@RestController
@PreAuthorize("hasAuthority(T(com.example.siggtrat.constante.ConstantesDominio).PERFIL_ADMINISTRADOR)")
@RequestMapping("/api/telefones")
public class TelefoneControlador {
    private final TelefoneServico servico;
    public TelefoneControlador(TelefoneServico servico) { this.servico = servico; }

    @GetMapping
    public Page<TelefoneDTO> listarTodos(Pageable pageable) { return servico.listarTodos(pageable); }

    @GetMapping("/{id}")
    public ResponseEntity<TelefoneDTO> buscarPorId(@PathVariable Long id) {
        TelefoneDTO dto = servico.buscarPorId(id);
        return dto == null ? ResponseEntity.notFound().build() : ResponseEntity.ok(dto);
    }

    @PostMapping @ExigirSenha
    public TelefoneDTO criar(@Valid @RequestBody TelefoneDTO dto) { return servico.criar(dto); }

    @PutMapping("/{id}") @ExigirSenha
    public ResponseEntity<TelefoneDTO> atualizar(@PathVariable Long id, @Valid @RequestBody TelefoneDTO dto) {
        TelefoneDTO updated = servico.atualizar(id, dto);
        return updated == null ? ResponseEntity.notFound().build() : ResponseEntity.ok(updated);
    }

    @DeleteMapping("/{id}") @ExigirSenha
    public ResponseEntity<Void> excluir(@PathVariable Long id) {
        return servico.excluir(id) ? ResponseEntity.noContent().build() : ResponseEntity.notFound().build();
    }
}
