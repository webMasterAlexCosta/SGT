package com.example.siggtrat.controlador;

import com.example.siggtrat.configuracao.InvalidaCacheNegocio;
import com.example.siggtrat.dto.StatusEmpregadoDTO;
import com.example.siggtrat.seguranca.ExigirSenha;
import com.example.siggtrat.servico.StatusEmpregadoServico;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@InvalidaCacheNegocio
@RestController
@RequestMapping("/api/status-empregados")
public class StatusEmpregadoControlador implements com.example.siggtrat.diagnostico.DiagnosticoOperacao {
    private static final org.slf4j.Logger LOG_DIAGNOSTICO = org.slf4j.LoggerFactory.getLogger(StatusEmpregadoControlador.class);

    @Override public org.slf4j.Logger loggerDiagnostico() { return LOG_DIAGNOSTICO; }

    private final StatusEmpregadoServico servico;
    public StatusEmpregadoControlador(StatusEmpregadoServico servico) { this.servico = servico; }

    @GetMapping
    public Page<StatusEmpregadoDTO> listarTodos(Pageable pageable) { return servico.listarTodos(pageable); }

    @GetMapping("/{id}")
    public ResponseEntity<StatusEmpregadoDTO> buscarPorId(@PathVariable Long id) {
        StatusEmpregadoDTO dto = servico.buscarPorId(id);
        if (dto == null) throw new org.springframework.web.server.ResponseStatusException(
                org.springframework.http.HttpStatus.NOT_FOUND, "Status do empregado não encontrado no seu escopo.");
        return ResponseEntity.ok(dto);
    }

    @PreAuthorize("hasAuthority(T(com.example.siggtrat.constante.ConstantesDominio).PERFIL_ADMINISTRADOR)")
    @PostMapping @ExigirSenha
    public StatusEmpregadoDTO criar(@Valid @RequestBody StatusEmpregadoDTO dto) { return servico.criar(dto); }

    @PreAuthorize("hasAuthority(T(com.example.siggtrat.constante.ConstantesDominio).PERFIL_ADMINISTRADOR)")
    @PutMapping("/{id}") @ExigirSenha
    public ResponseEntity<StatusEmpregadoDTO> atualizar(@PathVariable Long id, @Valid @RequestBody StatusEmpregadoDTO dto) {
        return ResponseEntity.ok(servico.atualizar(id, dto));
    }

    @PreAuthorize("hasAuthority(T(com.example.siggtrat.constante.ConstantesDominio).PERFIL_ADMINISTRADOR)")
    @DeleteMapping("/{id}") @ExigirSenha
    public ResponseEntity<Void> excluir(@PathVariable Long id) {
        servico.excluir(id);
        return ResponseEntity.noContent().build();
    }
}
