package com.example.siggtrat.controlador;

import com.example.siggtrat.configuracao.InvalidaCacheNegocio;
import com.example.siggtrat.dto.BancoDTO;
import com.example.siggtrat.servico.BancoServico;
import com.example.siggtrat.seguranca.ExigirSenha;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import java.util.Optional;

@InvalidaCacheNegocio
@RestController
@RequestMapping("/api/bancos")
public class BancoControlador {
    private final BancoServico bancoServico;
    public BancoControlador(BancoServico bancoServico) { this.bancoServico = bancoServico; }

    @GetMapping
    public Page<BancoDTO> listarTodos(Pageable pageable) {
        return bancoServico.listarTodos(pageable);
    }

    @GetMapping("/{id}")
    public ResponseEntity<BancoDTO> buscarPorId(@PathVariable Long id) {
        BancoDTO dto = bancoServico.buscarPorId(id);
        return dto != null ? ResponseEntity.ok(dto) : ResponseEntity.notFound().build();
    }

    @PostMapping
    @ExigirSenha
    public BancoDTO criar(@RequestBody BancoDTO dto) {
        return bancoServico.criar(dto);
    }

    @PutMapping("/{id}")
    @ExigirSenha
    public ResponseEntity<BancoDTO> atualizar(@PathVariable Long id, @RequestBody BancoDTO dto) {
        BancoDTO updated = bancoServico.atualizar(id, dto);
        return updated != null ? ResponseEntity.ok(updated) : ResponseEntity.notFound().build();
    }

    @DeleteMapping("/{id}")
    @ExigirSenha
    public ResponseEntity<Void> excluir(@PathVariable Long id) {
        bancoServico.excluir(id);
        return ResponseEntity.noContent().build();
    }
}