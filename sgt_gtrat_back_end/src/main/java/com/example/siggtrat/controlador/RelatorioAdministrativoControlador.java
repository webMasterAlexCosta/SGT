package com.example.siggtrat.controlador;

import com.example.siggtrat.configuracao.InvalidaCacheNegocio;
import com.example.siggtrat.dominio.relatorio.EstadoEmissaoRelatorio;
import com.example.siggtrat.dominio.relatorio.TipoRelatorioAdministrativo;
import com.example.siggtrat.dto.*;
import com.example.siggtrat.servico.RelatorioAdministrativoServico;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.time.LocalDate;

@RestController
@RequestMapping("/api/relatorios/administrativos")
@InvalidaCacheNegocio
@PreAuthorize("hasAnyAuthority(" +
        "T(com.example.siggtrat.constante.ConstantesDominio).PERFIL_ADMINISTRADOR, " +
        "T(com.example.siggtrat.constante.ConstantesDominio).PERFIL_GESTOR_NACIONAL, " +
        "T(com.example.siggtrat.constante.ConstantesDominio).PERFIL_PRESIDENTE, " +
        "T(com.example.siggtrat.constante.ConstantesDominio).PERFIL_GESTOR_ESTADUAL, " +
        "T(com.example.siggtrat.constante.ConstantesDominio).PERFIL_GESTOR_LOCAL)")
public class RelatorioAdministrativoControlador {
    private final RelatorioAdministrativoServico servico;

    public RelatorioAdministrativoControlador(RelatorioAdministrativoServico servico) {
        this.servico = servico;
    }

    /** Visão atual, ainda não formalizada, já marcada com a situação de revisão. */
    @PostMapping("/previa")
    public ResponseEntity<RelatorioAdministrativoDTO> previa(
            @RequestBody EmissaoRelatorioRequisicao requisicao) {
        return ResponseEntity.ok(servico.previsualizar(requisicao));
    }

    /** Cria uma versão administrativa imutável e idempotente. */
    @PostMapping("/emissoes")
    @PreAuthorize("!hasAuthority(T(com.example.siggtrat.constante.ConstantesDominio).PERFIL_PRESIDENTE)")
    public ResponseEntity<RelatorioAdministrativoDTO> emitir(
            @Valid @RequestBody EmissaoRelatorioRequisicao requisicao) {
        return ResponseEntity.status(HttpStatus.CREATED).body(servico.emitir(requisicao));
    }

    @GetMapping("/emissoes/{id}")
    public ResponseEntity<RelatorioAdministrativoDTO> obter(@PathVariable Long id) {
        return ResponseEntity.ok(servico.obter(id));
    }

    /** Recupera exatamente o que estava formalmente conhecido até o instante informado. */
    @PostMapping("/emissoes/conhecimento")
    public ResponseEntity<RelatorioAdministrativoDTO> conhecidaAte(
            @RequestParam Instant conhecidoAte,
            @RequestBody EmissaoRelatorioRequisicao requisicao) {
        return ResponseEntity.ok(servico.conhecidaAte(requisicao, conhecidoAte));
    }

    @GetMapping("/emissoes/{id}/comparacao-vigente")
    public ResponseEntity<ComparacaoEmissaoRelatorioDTO> comparar(@PathVariable Long id) {
        return ResponseEntity.ok(servico.compararComVigente(id));
    }

    @GetMapping("/emissoes")
    public ResponseEntity<Page<MetadadosEmissaoRelatorioDTO>> listar(
            @RequestParam(required = false) TipoRelatorioAdministrativo tipo,
            @RequestParam(required = false) EstadoEmissaoRelatorio estado,
            @RequestParam(required = false) LocalDate inicio,
            @RequestParam(required = false) LocalDate fim,
            @RequestParam(defaultValue = "0") Integer pagina,
            @RequestParam(defaultValue = "50") Integer tamanho) {
        return ResponseEntity.ok(servico.listar(tipo, estado, inicio, fim, pagina, tamanho));
    }
}
