package com.example.siggtrat.controlador;

import com.example.siggtrat.configuracao.InvalidaCacheNegocio;
import com.example.siggtrat.dto.GestaoLocalDTO;
import com.example.siggtrat.seguranca.ExigirSenha;
import com.example.siggtrat.servico.GestaoLocalServico;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@InvalidaCacheNegocio
@RestController
@RequestMapping("/api/gestoes-locais")
public class GestaoLocalControlador implements com.example.siggtrat.diagnostico.DiagnosticoOperacao {
    private static final org.slf4j.Logger LOG_DIAGNOSTICO = org.slf4j.LoggerFactory.getLogger(GestaoLocalControlador.class);

    @Override public org.slf4j.Logger loggerDiagnostico() { return LOG_DIAGNOSTICO; }

    private final GestaoLocalServico servico;
    public GestaoLocalControlador(GestaoLocalServico servico) { this.servico = servico; }

    @GetMapping
    public List<GestaoLocalDTO> listar(@RequestParam(required = false) Long superintendenciaId) {
        return servico.listar(superintendenciaId);
    }

    @PostMapping
    @PreAuthorize("hasAuthority(T(com.example.siggtrat.constante.ConstantesDominio).PERFIL_ADMINISTRADOR)")
    @ExigirSenha
    public ResponseEntity<GestaoLocalDTO> criar(@RequestBody GestaoLocalDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(servico.criar(dto));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority(T(com.example.siggtrat.constante.ConstantesDominio).PERFIL_ADMINISTRADOR)")
    @ExigirSenha
    public GestaoLocalDTO atualizar(@PathVariable Long id, @RequestBody GestaoLocalDTO dto) { return servico.atualizar(id, dto); }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority(T(com.example.siggtrat.constante.ConstantesDominio).PERFIL_ADMINISTRADOR)")
    @ExigirSenha
    public ResponseEntity<Void> excluir(@PathVariable Long id) {
        servico.validarExclusao(id);
        return ResponseEntity.noContent().build();
    }
}
