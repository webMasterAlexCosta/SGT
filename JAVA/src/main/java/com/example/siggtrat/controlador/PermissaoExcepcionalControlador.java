package com.example.siggtrat.controlador;

import com.example.siggtrat.configuracao.InvalidaCacheNegocio;
import com.example.siggtrat.dto.*;
import com.example.siggtrat.seguranca.ExigirSenha;
import com.example.siggtrat.servico.PermissaoExcepcionalServico;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@InvalidaCacheNegocio
@RestController
@RequestMapping("/api/permissoes-excepcionais")
public class PermissaoExcepcionalControlador {
    private final PermissaoExcepcionalServico servico;

    public PermissaoExcepcionalControlador(PermissaoExcepcionalServico servico) {
        this.servico = servico;
    }

    @GetMapping
    public List<PermissaoExcepcionalDTO> listar() { return servico.listar(); }

    @PostMapping
    @ExigirSenha
    public ResponseEntity<PermissaoExcepcionalDTO> conceder(
            @Valid @RequestBody ConcessaoPermissaoExcepcionalRequisicao requisicao) {
        return ResponseEntity.status(HttpStatus.CREATED).body(servico.conceder(requisicao));
    }

    @PostMapping("/{id}/revogar")
    @ExigirSenha
    public PermissaoExcepcionalDTO revogar(@PathVariable Long id,
                                            @Valid @RequestBody RevogacaoPermissaoExcepcionalRequisicao requisicao) {
        return servico.revogar(id, requisicao);
    }

    @GetMapping("/{id}/usos")
    public List<UsoPermissaoExcepcionalDTO> usos(@PathVariable Long id) {
        return servico.listarUsos(id);
    }
}
