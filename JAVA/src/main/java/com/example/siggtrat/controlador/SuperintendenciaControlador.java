package com.example.siggtrat.controlador;

import com.example.siggtrat.configuracao.InvalidaCacheNegocio;
import com.example.siggtrat.dto.AtivacaoUsuarioAplicacaoDTO;
import com.example.siggtrat.dto.SuperintendenciaDTO;
import com.example.siggtrat.seguranca.ExigirSenha;
import com.example.siggtrat.servico.SuperintendenciaServico;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@InvalidaCacheNegocio
@RestController
@RequestMapping("/api/superintendencias")
public class SuperintendenciaControlador implements com.example.siggtrat.diagnostico.DiagnosticoOperacao {
    private static final org.slf4j.Logger LOG_DIAGNOSTICO = org.slf4j.LoggerFactory.getLogger(SuperintendenciaControlador.class);

    @Override public org.slf4j.Logger loggerDiagnostico() { return LOG_DIAGNOSTICO; }

    private final SuperintendenciaServico servico;
    public SuperintendenciaControlador(SuperintendenciaServico servico) { this.servico = servico; }

    @GetMapping
    public List<SuperintendenciaDTO> listar() { return servico.listar(); }

    @PostMapping
    @PreAuthorize("hasAuthority(T(com.example.siggtrat.constante.ConstantesDominio).PERFIL_ADMINISTRADOR)")
    @ExigirSenha
    public ResponseEntity<SuperintendenciaDTO> criar(@RequestBody SuperintendenciaDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(servico.criar(dto));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority(T(com.example.siggtrat.constante.ConstantesDominio).PERFIL_ADMINISTRADOR)")
    @ExigirSenha
    public SuperintendenciaDTO atualizar(@PathVariable Long id, @RequestBody SuperintendenciaDTO dto) {
        return servico.atualizar(id, dto);
    }

    @PostMapping("/{id}/ativar")
    @PreAuthorize("hasAuthority(T(com.example.siggtrat.constante.ConstantesDominio).PERFIL_ADMINISTRADOR)")
    @ExigirSenha
    public AtivacaoUsuarioAplicacaoDTO ativar(@PathVariable Long id) {
        return servico.ativar(id);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority(T(com.example.siggtrat.constante.ConstantesDominio).PERFIL_ADMINISTRADOR)")
    @ExigirSenha
    public ResponseEntity<Void> excluir(@PathVariable Long id) {
        servico.validarExclusao(id);
        return ResponseEntity.noContent().build();
    }
}
