package com.example.siggtrat.controlador;

import com.example.siggtrat.configuracao.InvalidaCacheNegocio;
import com.example.siggtrat.dto.JornadaDTO;
import com.example.siggtrat.seguranca.ExigirSenha;
import com.example.siggtrat.servico.JornadaServico;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@InvalidaCacheNegocio
@RestController
@RequestMapping("/api/jornadas")
public class JornadaControlador {
    private final JornadaServico servico;
    public JornadaControlador(JornadaServico servico) { this.servico = servico; }

    @GetMapping
    public List<JornadaDTO> listar(@RequestParam(required = false) Long lotacaoId) { return servico.listar(lotacaoId); }

    @GetMapping("/{id}")
    public ResponseEntity<JornadaDTO> obter(@PathVariable Long id) {
        JornadaDTO dto = servico.obter(id);
        return dto == null ? ResponseEntity.notFound().build() : ResponseEntity.ok(dto);
    }

    @PostMapping @ExigirSenha
    public ResponseEntity<JornadaDTO> criar(@RequestBody JornadaDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(servico.criar(dto));
    }

    @PutMapping("/{id}") @ExigirSenha
    public ResponseEntity<JornadaDTO> atualizar(@PathVariable Long id, @RequestBody JornadaDTO dto) {
        return ResponseEntity.ok(servico.atualizar(id, dto));
    }

    @DeleteMapping("/{id}") @ExigirSenha
    public ResponseEntity<Void> excluir(@PathVariable Long id) {
        servico.excluir(id);
        return ResponseEntity.noContent().build();
    }
}
