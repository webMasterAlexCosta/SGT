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
public class LotacaoControlador implements com.example.siggtrat.diagnostico.DiagnosticoOperacao {
    private static final org.slf4j.Logger LOG_DIAGNOSTICO = org.slf4j.LoggerFactory.getLogger(LotacaoControlador.class);

    @Override public org.slf4j.Logger loggerDiagnostico() { return LOG_DIAGNOSTICO; }


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
                .orElseThrow(() -> new org.springframework.web.server.ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Lotação não encontrada no seu escopo."));
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
