package com.example.siggtrat.controlador;

import com.example.siggtrat.configuracao.InvalidaCacheNegocio;
import com.example.siggtrat.dto.*;
import com.example.siggtrat.seguranca.ExigirSenha;
import com.example.siggtrat.servico.PessoaServico;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.time.LocalDate;

@RestController
@RequestMapping("/api/pessoas")
@InvalidaCacheNegocio
public class PessoaControlador implements com.example.siggtrat.diagnostico.DiagnosticoOperacao {
    private static final org.slf4j.Logger LOG_DIAGNOSTICO = org.slf4j.LoggerFactory.getLogger(PessoaControlador.class);

    @Override public org.slf4j.Logger loggerDiagnostico() { return LOG_DIAGNOSTICO; }

    private final PessoaServico servico;
    public PessoaControlador(PessoaServico servico) { this.servico = servico; }

    @GetMapping
    public Page<PessoaResumoDTO> listar(@RequestParam(required = false) String busca,
                                        @RequestParam(required = false) Long contratoId,
                                        @RequestParam(required = false) LocalDate ativoEm,
                                        Pageable pageable) {
        return servico.listar(busca, contratoId, ativoEm, pageable);
    }
    @GetMapping("/{id}") public PessoaDTO buscar(@PathVariable Long id) { return servico.buscar(id); }
    @GetMapping("/{id}/dados-restritos")
    public PessoaDTO buscarDadosRestritos(@PathVariable Long id, @RequestParam String finalidade) {
        return servico.buscarDadosRestritos(id, finalidade);
    }
    @PutMapping("/{id}/dados-opcionais") @ExigirSenha
    public PessoaDTO atualizarOpcionais(@PathVariable Long id, @RequestBody AtualizacaoPessoaOpcionalRequisicao req) {
        return servico.atualizarOpcionais(id, req);
    }
    @PostMapping("/{id}/correcoes-identidade") @ExigirSenha
    public PessoaDTO corrigir(@PathVariable Long id, @RequestBody CorrecaoIdentidadePessoaRequisicao req) {
        return servico.corrigirIdentidade(id, req);
    }
    @GetMapping("/{id}/correcoes-identidade")
    public List<CorrecaoPessoaDTO> correcoes(@PathVariable Long id) { return servico.listarCorrecoes(id); }
}
