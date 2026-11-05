package com.example.siggtrat.controlador;

import com.example.siggtrat.configuracao.InvalidaCacheNegocio;
import com.example.siggtrat.dto.ContratoFornecedorDTO;
import com.example.siggtrat.dto.FornecedorDTO;
import com.example.siggtrat.dto.FornecedorEntradaDTO;
import com.example.siggtrat.seguranca.ExigirSenha;
import com.example.siggtrat.servico.FornecedorServico;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/** Cadastro da empresa contratada e seleção de sua identidade no rascunho. */
@InvalidaCacheNegocio
@RestController
@RequestMapping("/api")
@PreAuthorize("hasAuthority('PERFIL_GESTOR_LOCAL')")
public class FornecedorControlador {
    private final FornecedorServico servico;

    public FornecedorControlador(FornecedorServico servico) {
        this.servico = servico;
    }

    @GetMapping("/fornecedores")
    public List<FornecedorDTO> fornecedores() {
        return servico.fornecedores();
    }

    @PostMapping("/fornecedores")
    @ExigirSenha
    public FornecedorDTO criarFornecedor(@RequestBody FornecedorEntradaDTO dados) {
        return servico.criarFornecedor(dados);
    }

    @PutMapping("/fornecedores/{id}")
    @ExigirSenha
    public FornecedorDTO atualizarFornecedor(@PathVariable Long id, @RequestBody FornecedorEntradaDTO dados) {
        return servico.atualizarFornecedor(id, dados);
    }

    @PutMapping("/contratos/{contratoId}/fornecedor/{fornecedorId}")
    @ExigirSenha
    public ContratoFornecedorDTO vincularFornecedor(@PathVariable Long contratoId, @PathVariable Long fornecedorId) {
        return servico.vincularFornecedor(contratoId, fornecedorId);
    }
}
