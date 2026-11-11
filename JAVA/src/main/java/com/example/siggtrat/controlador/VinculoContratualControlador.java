package com.example.siggtrat.controlador;

import com.example.siggtrat.configuracao.InvalidaCacheNegocio;
import com.example.siggtrat.dto.*;
import com.example.siggtrat.seguranca.ExigirSenha;
import com.example.siggtrat.servico.AlocacaoVinculoServico;
import com.example.siggtrat.servico.VinculoContratualServico;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.format.annotation.DateTimeFormat;
import java.time.*;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import com.example.siggtrat.dominio.vinculo.EstadoNegocialVinculo;

@RestController
@RequestMapping("/api/vinculos-contratuais")
@InvalidaCacheNegocio
public class VinculoContratualControlador implements com.example.siggtrat.diagnostico.DiagnosticoOperacao {
    private static final org.slf4j.Logger LOG_DIAGNOSTICO = org.slf4j.LoggerFactory.getLogger(VinculoContratualControlador.class);

    @Override public org.slf4j.Logger loggerDiagnostico() { return LOG_DIAGNOSTICO; }

    private final VinculoContratualServico servico;
    private AlocacaoVinculoServico alocacaoServico;
    public VinculoContratualControlador(VinculoContratualServico servico) { this.servico = servico; }
    @Autowired(required = false)
    public void configurarAlocacaoTemporal(AlocacaoVinculoServico alocacaoServico) { this.alocacaoServico = alocacaoServico; }

    @GetMapping
    public Page<VinculoContratualDTO> listar(@RequestParam(required = false) Long contratoId,
            @RequestParam(required = false) Long pessoaId,
            @RequestParam(required = false) EstadoNegocialVinculo estado,
            @RequestParam(required = false) LocalDate inicioDe,
            @RequestParam(required = false) LocalDate inicioAte,
            @RequestParam(required = false) LocalDate fimDe,
            @RequestParam(required = false) LocalDate fimAte,
            @RequestParam(required = false) LocalDate ativoEm,
            @RequestParam(required = false) String busca, Pageable pageable) {
        return servico.listar(contratoId, pessoaId, estado, inicioDe, inicioAte, fimDe, fimAte, ativoEm, busca, pageable);
    }
    @GetMapping("/elegiveis-frequencia")
    public Page<VinculoElegivelFrequenciaDTO> elegiveisFrequencia(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dataRef,
            @RequestParam Long lotacaoId, @RequestParam Long jornadaId,
            @RequestParam(required = false) Long contratoId,
            @RequestParam(required = false) List<Long> contratoIds,
            @RequestParam(required = false) String busca, Pageable pageable) {
        return servico.listarElegiveisParaFrequencia(dataRef, lotacaoId, jornadaId,
                contratoId, contratoIds, busca, pageable);
    }

    @GetMapping("/{id}") public VinculoContratualDTO buscar(@PathVariable Long id) { return servico.buscar(id); }
    @GetMapping("/{id}/eventos") public List<EventoVinculoContratualDTO> eventos(@PathVariable Long id) { return servico.listarEventos(id); }
    @GetMapping("/{id}/alocacoes")
    public List<AlocacaoVinculoDTO> alocacoes(@PathVariable Long id,
                                               @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM") YearMonth mes) {
        return alocacao().listar(id, mes);
    }
    @GetMapping("/{id}/alocacoes/eventos")
    public List<EventoAlocacaoVinculoDTO> eventosAlocacao(@PathVariable Long id) { return alocacao().listarEventos(id); }
    @GetMapping("/{id}/alocacoes/posicao")
    public PosicaoAlocacaoTemporalDTO posicaoAlocacao(@PathVariable Long id, @RequestParam LocalDate data,
                                                       @RequestParam(required = false) Instant conhecidoAte) {
        return alocacao().posicao(id, data, conhecidoAte);
    }
    @GetMapping("/contratos/{contratoId}/opcoes-alocacao")
    public List<OpcaoAlocacaoTemporalDTO> opcoesAlocacao(@PathVariable Long contratoId, @RequestParam LocalDate data,
                                                          @RequestParam(required = false) Long lotacaoId,
                                                          @RequestParam(required = false) Long jornadaId) {
        return alocacao().opcoes(contratoId, data, lotacaoId, jornadaId);
    }
    @GetMapping("/pessoas/{pessoaId}/posicao")
    public PosicaoPessoaDTO posicao(@PathVariable Long pessoaId, @RequestParam LocalDate data,
                                    @RequestParam(required = false) Instant conhecidoAte) {
        return servico.posicaoPessoa(pessoaId, data, conhecidoAte);
    }
    @PostMapping @ExigirSenha
    public ResponseEntity<VinculoContratualDTO> cadastrar(@RequestBody CadastroVinculoContratualRequisicao req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(servico.cadastrar(req));
    }
    @PostMapping("/{id}/previas/retificacao-inicio")
    public PreviaImpactoVinculoDTO previaInicio(@PathVariable Long id, @RequestBody RetificacaoInicioVinculoRequisicao req) {
        return servico.previaRetificacaoInicio(id, req);
    }
    @PostMapping("/{id}/retificar-inicio") @ExigirSenha
    public VinculoContratualDTO retificarInicio(@PathVariable Long id, @RequestBody RetificacaoInicioVinculoRequisicao req) {
        return servico.retificarInicio(id, req);
    }
    @PostMapping("/{id}/previas/alocacoes")
    public PreviaImpactoAlocacaoDTO previaAlocacao(@PathVariable Long id, @RequestBody AlteracaoAlocacaoVinculoRequisicao req) {
        return alocacao().previaAlteracao(id, req);
    }
    @PostMapping("/{id}/alocacoes") @ExigirSenha
    public AlocacaoVinculoDTO alterarAlocacao(@PathVariable Long id, @RequestBody AlteracaoAlocacaoVinculoRequisicao req) {
        return alocacao().alterar(id, req);
    }
    @PostMapping("/{id}/alocacoes/{alocacaoId}/retificar") @ExigirSenha
    public AlocacaoVinculoDTO retificarAlocacao(@PathVariable Long id, @PathVariable Long alocacaoId,
                                                 @RequestBody AlteracaoAlocacaoVinculoRequisicao req) {
        return alocacao().retificar(id, alocacaoId, req);
    }
    @PostMapping("/{id}/alocacoes/{alocacaoId}/cancelar-programacao") @ExigirSenha
    public AlocacaoVinculoDTO cancelarAlocacao(@PathVariable Long id, @PathVariable Long alocacaoId,
                                                @RequestBody AcaoVinculoContratualRequisicao req) {
        return alocacao().cancelarProgramacao(id, alocacaoId, req);
    }
    @PostMapping("/{id}/alterar-matricula") @ExigirSenha
    public VinculoContratualDTO alterarMatricula(@PathVariable Long id, @RequestBody AlteracaoMatriculaVinculoRequisicao req) {
        return servico.alterarMatricula(id, req);
    }
    @PostMapping("/{id}/cancelar-programacao") @ExigirSenha
    public VinculoContratualDTO cancelarProgramacao(@PathVariable Long id, @RequestBody AcaoVinculoContratualRequisicao req) {
        return servico.cancelarProgramacao(id, req);
    }
    @PostMapping("/{id}/desligar") @ExigirSenha
    public VinculoContratualDTO desligar(@PathVariable Long id, @RequestBody DesligamentoVinculoRequisicao req) {
        return servico.desligar(id, req);
    }
    @PostMapping("/{id}/previas/retificacao-desligamento")
    public PreviaImpactoVinculoDTO previaDesligamento(@PathVariable Long id, @RequestBody DesligamentoVinculoRequisicao req) {
        return servico.previaRetificacaoDesligamento(id, req);
    }
    @PostMapping("/{id}/retificar-desligamento") @ExigirSenha
    public VinculoContratualDTO retificarDesligamento(@PathVariable Long id, @RequestBody DesligamentoVinculoRequisicao req) {
        return servico.retificarDesligamento(id, req);
    }
    @PostMapping("/{id}/cancelar-desligamento-programado") @ExigirSenha
    public VinculoContratualDTO cancelarDesligamento(@PathVariable Long id, @RequestBody AcaoVinculoContratualRequisicao req) {
        return servico.cancelarDesligamentoProgramado(id, req);
    }
    @PostMapping("/contratos/{contratoId}/tratar-fim-natural") @ExigirSenha
    public List<VinculoContratualDTO> tratarFimNatural(@PathVariable Long contratoId) { return servico.tratarFimNatural(contratoId); }

    private AlocacaoVinculoServico alocacao() {
        if (alocacaoServico == null) throw new IllegalStateException("Módulo de Alocação temporal indisponível.");
        return alocacaoServico;
    }
}
