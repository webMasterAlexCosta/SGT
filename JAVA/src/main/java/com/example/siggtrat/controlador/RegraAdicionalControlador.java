package com.example.siggtrat.controlador;

import com.example.siggtrat.configuracao.InvalidaCacheNegocio;
import com.example.siggtrat.dto.RegraAdicionalDTO;
import com.example.siggtrat.seguranca.ExigirSenha;
import com.example.siggtrat.servico.RegraAdicionalServico;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@InvalidaCacheNegocio
@RestController
@RequestMapping("/api")
public class RegraAdicionalControlador implements com.example.siggtrat.diagnostico.DiagnosticoOperacao {
    private static final org.slf4j.Logger LOG_DIAGNOSTICO = org.slf4j.LoggerFactory.getLogger(RegraAdicionalControlador.class);

    @Override public org.slf4j.Logger loggerDiagnostico() { return LOG_DIAGNOSTICO; }

    private final RegraAdicionalServico servico;
    public RegraAdicionalControlador(RegraAdicionalServico servico) { this.servico = servico; }

    @GetMapping("/jornadas/{jornadaId}/regras-adicionais")
    public List<RegraAdicionalDTO> listar(@PathVariable Long jornadaId) { return servico.listar(jornadaId); }

    @PostMapping("/jornadas/{jornadaId}/regras-adicionais")
    @ExigirSenha
    public ResponseEntity<RegraAdicionalDTO> criar(@PathVariable Long jornadaId, @RequestBody RegraAdicionalDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(servico.criar(jornadaId, dto));
    }

    @PutMapping("/regras-adicionais/{id}")
    @ExigirSenha
    public ResponseEntity<RegraAdicionalDTO> atualizar(@PathVariable Long id, @RequestBody RegraAdicionalDTO dto) {
        return ResponseEntity.ok(servico.atualizar(id, dto));
    }

    @DeleteMapping("/regras-adicionais/{id}")
    @ExigirSenha
    public ResponseEntity<Void> excluir(@PathVariable Long id) {
        servico.excluir(id);
        return ResponseEntity.noContent().build();
    }
}
