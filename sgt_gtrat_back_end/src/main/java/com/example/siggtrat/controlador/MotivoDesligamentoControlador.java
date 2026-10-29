package com.example.siggtrat.controlador;

import com.example.siggtrat.configuracao.InvalidaCacheNegocio;
import com.example.siggtrat.dto.MotivoDesligamentoDTO;
import com.example.siggtrat.seguranca.ExigirSenha;
import com.example.siggtrat.servico.MotivoDesligamentoServico;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@InvalidaCacheNegocio
@RestController
@RequestMapping("/api/motivos-desligamento")
public class MotivoDesligamentoControlador {
    private final MotivoDesligamentoServico servico;

    public MotivoDesligamentoControlador(MotivoDesligamentoServico servico) {
        this.servico = servico;
    }

    @GetMapping
    public List<MotivoDesligamentoDTO> listar() { return servico.listar(); }

    @GetMapping("/{id}")
    public MotivoDesligamentoDTO buscarPorId(@PathVariable Long id) { return servico.buscarPorId(id); }

    @PostMapping
    @ExigirSenha
    public ResponseEntity<MotivoDesligamentoDTO> criar(@Valid @RequestBody MotivoDesligamentoDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(servico.criar(dto));
    }

    @PutMapping("/{id}")
    @ExigirSenha
    public MotivoDesligamentoDTO atualizar(@PathVariable Long id, @Valid @RequestBody MotivoDesligamentoDTO dto) {
        return servico.atualizar(id, dto);
    }
}
