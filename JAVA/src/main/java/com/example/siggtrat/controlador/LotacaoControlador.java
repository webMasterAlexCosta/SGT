package com.example.siggtrat.controlador;

import com.example.siggtrat.configuracao.InvalidaCacheNegocio;
import com.example.siggtrat.dto.LotacaoDTO;
import com.example.siggtrat.dto.LotacaoOpcaoDTO;
import com.example.siggtrat.seguranca.ExigirSenha;
import com.example.siggtrat.servico.LotacaoServico;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@InvalidaCacheNegocio
@RestController
@RequestMapping("/api/lotacoes")
public class LotacaoControlador {

    private final LotacaoServico lotacaoServico;

    public LotacaoControlador(LotacaoServico lotacaoServico) {
        this.lotacaoServico = lotacaoServico;
    }

    @GetMapping
    public Page<LotacaoDTO> listarTodos(Pageable pageable) {
        return lotacaoServico.listar(pageable);
    }

    @GetMapping("/opcoes")
    public List<LotacaoOpcaoDTO> obterOpcoes() {
        return lotacaoServico.listarOpcoes();
    }

    @GetMapping("/{id}")
    public ResponseEntity<LotacaoDTO> buscarPorId(@PathVariable Long id) {
        return lotacaoServico.buscar(id)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PostMapping
    @ExigirSenha
    public ResponseEntity<LotacaoDTO> criar(@RequestBody LotacaoDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(lotacaoServico.criar(dto));
    }

    @PutMapping("/{id}")
    @ExigirSenha
    public ResponseEntity<LotacaoDTO> atualizar(@PathVariable Long id, @RequestBody LotacaoDTO dto) {
        return ResponseEntity.ok(lotacaoServico.atualizar(id, dto));
    }

    @DeleteMapping("/{id}")
    @ExigirSenha
    public ResponseEntity<Void> excluir(@PathVariable Long id) {
        lotacaoServico.excluir(id);
        return ResponseEntity.noContent().build();
    }
}
