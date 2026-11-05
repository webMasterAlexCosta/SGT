package com.example.siggtrat.controlador;

import com.example.siggtrat.configuracao.InvalidaCacheNegocio;
import com.example.siggtrat.dto.TipoContratacaoDTO;
import com.example.siggtrat.seguranca.ExigirSenha;
import com.example.siggtrat.servico.TipoContratacaoServico;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@InvalidaCacheNegocio
@RestController
@RequestMapping("/api/tipos-contratacao")
public class TipoContratacaoControlador {

    private final TipoContratacaoServico servico;

    public TipoContratacaoControlador(TipoContratacaoServico servico) {
        this.servico = servico;
    }

    @GetMapping
    public List<TipoContratacaoDTO> listar() {
        return servico.listar();
    }

    @GetMapping("/{id}")
    public TipoContratacaoDTO buscarPorId(@PathVariable Long id) {
        return servico.buscarPorId(id);
    }

    @PostMapping
    @ExigirSenha
    public ResponseEntity<TipoContratacaoDTO> criar(@RequestBody TipoContratacaoDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(servico.criar(dto));
    }

    @PutMapping("/{id}")
    @ExigirSenha
    public TipoContratacaoDTO atualizar(@PathVariable Long id, @RequestBody TipoContratacaoDTO dto) {
        return servico.atualizar(id, dto);
    }
}
