package com.example.siggtrat.controlador;

import com.example.siggtrat.configuracao.InvalidaCacheNegocio;
import com.example.siggtrat.dto.*;
import com.example.siggtrat.seguranca.ExigirSenha;
import com.example.siggtrat.servico.GovernancaTerceirosServico;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@InvalidaCacheNegocio
@RestController
@RequestMapping("/api/governanca-terceiros")
@PreAuthorize("hasAuthority('PERFIL_GESTOR_LOCAL')")
public class GovernancaTerceirosControlador {

    private final GovernancaTerceirosServico servico;

    public GovernancaTerceirosControlador(GovernancaTerceirosServico servico) {
        this.servico = servico;
    }

    @GetMapping("/resumo")
    public GovernancaResumoDTO resumo() { return servico.resumo(); }

    @GetMapping("/fornecedores")
    public List<FornecedorGovernancaDTO> fornecedores() { return servico.fornecedores(); }

    @PostMapping("/fornecedores") @ExigirSenha
    public FornecedorGovernancaDTO criarFornecedor(@RequestBody FornecedorGovernancaEntradaDTO dados) {
        return servico.criarFornecedor(dados);
    }

    @PutMapping("/fornecedores/{id}") @ExigirSenha
    public FornecedorGovernancaDTO atualizarFornecedor(
            @PathVariable Long id, @RequestBody FornecedorGovernancaEntradaDTO dados) {
        return servico.atualizarFornecedor(id, dados);
    }

    @PutMapping("/contratos/{contratoId}/fornecedor/{fornecedorId}") @ExigirSenha
    public ContratoFornecedorGovernancaDTO vincularFornecedor(
            @PathVariable Long contratoId, @PathVariable Long fornecedorId) {
        return servico.vincularFornecedor(contratoId, fornecedorId);
    }

    @GetMapping("/tabelas-tarifarias")
    public List<TabelaTarifariaGovernancaDTO> tabelasTarifarias(@RequestParam Long contratoId) {
        return servico.tabelasTarifarias(contratoId);
    }

    @PostMapping("/tabelas-tarifarias") @ExigirSenha
    public TabelaTarifariaGovernancaDTO criarTabelaTarifaria(@RequestBody TabelaTarifariaGovernancaEntradaDTO dados) {
        return servico.criarTabelaTarifaria(dados);
    }

    @GetMapping("/processos")
    public List<ProcessoTerceiroGovernancaDTO> processos() { return servico.processos(); }

    @PostMapping("/processos") @ExigirSenha
    public ProcessoTerceiroGovernancaDTO criarProcesso(@RequestBody ProcessoTerceiroEntradaDTO dados) {
        return servico.criarProcesso(dados);
    }

    @PatchMapping("/processos/{id}/status") @ExigirSenha
    public ProcessoTerceiroGovernancaDTO statusProcesso(
            @PathVariable Long id, @RequestBody StatusGovernancaDTO dados) {
        return servico.statusProcesso(id, dados);
    }

    @GetMapping("/folhas-horas")
    public List<FolhaHorasGovernancaDTO> folhasHoras() { return servico.folhasHoras(); }

    @PostMapping("/folhas-horas/gerar") @ExigirSenha
    public FolhaHorasGovernancaDTO gerarFolhaHoras(@RequestBody FolhaHorasGeracaoDTO dados) {
        return servico.gerarFolhaHoras(dados);
    }

    @PostMapping("/folhas-horas/{id}/submeter") @ExigirSenha
    public FolhaHorasGovernancaDTO submeter(@PathVariable Long id) { return servico.submeter(id); }

    @PostMapping("/folhas-horas/{id}/aprovar") @ExigirSenha
    public FolhaHorasGovernancaDTO aprovar(@PathVariable Long id) { return servico.aprovar(id); }

    @PostMapping("/folhas-horas/{id}/rejeitar") @ExigirSenha
    public FolhaHorasGovernancaDTO rejeitar(@PathVariable Long id, @RequestBody MotivoGovernancaDTO dados) {
        return servico.rejeitar(id, dados);
    }

    @GetMapping("/faturas")
    public List<FaturaGovernancaDTO> faturas() { return servico.faturas(); }

    @PostMapping("/faturas/gerar/{folhaHorasId}") @ExigirSenha
    public FaturaGovernancaDTO gerarFatura(
            @PathVariable Long folhaHorasId,
            @RequestBody(required = false) FaturaGeracaoDTO dados) {
        return servico.gerarFatura(folhaHorasId, dados);
    }

    @PatchMapping("/faturas/{id}/status") @ExigirSenha
    public FaturaGovernancaDTO statusFatura(@PathVariable Long id, @RequestBody FaturaStatusDTO dados) {
        return servico.statusFatura(id, dados);
    }

    @GetMapping("/riscos")
    public List<RiscoFornecedorGovernancaDTO> riscos() { return servico.riscos(); }

    @PostMapping("/riscos") @ExigirSenha
    public RiscoFornecedorGovernancaDTO avaliarRisco(@RequestBody RiscoFornecedorEntradaDTO dados) {
        return servico.avaliarRisco(dados);
    }
}
