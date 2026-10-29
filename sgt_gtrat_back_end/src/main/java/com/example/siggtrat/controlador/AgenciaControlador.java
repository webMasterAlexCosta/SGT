package com.example.siggtrat.controlador;

import com.example.siggtrat.configuracao.InvalidaCacheNegocio;
import com.example.siggtrat.constante.ConstantesDominio;
import com.example.siggtrat.dto.AgenciaDTO;
import com.example.siggtrat.servico.AgenciaServico;
import com.example.siggtrat.seguranca.ExigirSenha;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

@InvalidaCacheNegocio
@RestController
@RequestMapping("/api/agencias")
public class AgenciaControlador {

    private final AgenciaServico agenciaServico;

    public AgenciaControlador(AgenciaServico agenciaServico) {
        this.agenciaServico = agenciaServico;
    }

    @GetMapping
    public Page<AgenciaDTO> listarTodos(Pageable pageable) {
        return agenciaServico.listarTodos(pageable);
    }

    @GetMapping("/{id}")
    public ResponseEntity<AgenciaDTO> buscarPorId(@PathVariable Long id) {
        AgenciaDTO dto = agenciaServico.buscarPorId(id);
        return dto != null ? ResponseEntity.ok(dto) : ResponseEntity.notFound().build();
    }

    @PostMapping
    @ExigirSenha
    public AgenciaDTO criar(@RequestBody AgenciaDTO dto) {
        return agenciaServico.criar(dto);
    }

    @PreAuthorize("hasAuthority(T(com.example.siggtrat.constante.ConstantesDominio).PERFIL_ADMINISTRADOR)")
    @PutMapping("/{id}")
    @ExigirSenha
    public ResponseEntity<AgenciaDTO> atualizar(@PathVariable Long id, @RequestBody AgenciaDTO dto) {
        AgenciaDTO updated = agenciaServico.atualizar(id, dto);
        return updated != null ? ResponseEntity.ok(updated) : ResponseEntity.notFound().build();
    }

    @DeleteMapping("/{id}")
    @ExigirSenha
    public ResponseEntity<Void> excluir(@PathVariable Long id) {
        agenciaServico.excluir(id);
        return ResponseEntity.noContent().build();
    }
}