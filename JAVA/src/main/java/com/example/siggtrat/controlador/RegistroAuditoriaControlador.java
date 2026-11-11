package com.example.siggtrat.controlador;

import com.example.siggtrat.dto.RegistroAuditoriaDTO;
import com.example.siggtrat.servico.ConsultaRegistroAuditoriaServico;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;

@RestController
@RequestMapping("/api/registros-auditoria")
@PreAuthorize("hasAuthority(T(com.example.siggtrat.constante.ConstantesDominio).PERFIL_ADMINISTRADOR)")
public class RegistroAuditoriaControlador implements com.example.siggtrat.diagnostico.DiagnosticoOperacao {
    private static final org.slf4j.Logger LOG_DIAGNOSTICO = org.slf4j.LoggerFactory.getLogger(RegistroAuditoriaControlador.class);

    @Override public org.slf4j.Logger loggerDiagnostico() { return LOG_DIAGNOSTICO; }

    private final ConsultaRegistroAuditoriaServico servico;
    public RegistroAuditoriaControlador(ConsultaRegistroAuditoriaServico servico) { this.servico = servico; }

    @GetMapping
    public Page<RegistroAuditoriaDTO> pesquisar(
            @RequestParam(name = "action", required = false) String acao,
            @RequestParam(name = "username", required = false) String nomeUsuario,
            @RequestParam(name = "entityType", required = false) String tipoEntidade,
            @RequestParam(name = "entityId", required = false) Long idEntidade,
            @RequestParam(name = "requestId", required = false) String idRequisicao,
            @RequestParam(name = "startDate", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant dataInicio,
            @RequestParam(name = "endDate", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant dataFim,
            @RequestParam(name = "includeEntityType", required = false) String tipoEntidadeIncluida,
            @RequestParam(name = "includeEntityIds", required = false) List<Long> idsEntidadesIncluidas,
            Pageable pageable) {
        return servico.pesquisar(acao, nomeUsuario, tipoEntidade, idEntidade, idRequisicao, dataInicio, dataFim,
                tipoEntidadeIncluida, idsEntidadesIncluidas, pageable);
    }

    @GetMapping("/{id}")
    public ResponseEntity<RegistroAuditoriaDTO> buscarPorId(@PathVariable Long id) {
        RegistroAuditoriaDTO dto = servico.buscarPorId(id);
        if (dto == null) throw new org.springframework.web.server.ResponseStatusException(
                org.springframework.http.HttpStatus.NOT_FOUND, "Registro de auditoria não encontrado no seu escopo.");
        return ResponseEntity.ok(dto);
    }
}
