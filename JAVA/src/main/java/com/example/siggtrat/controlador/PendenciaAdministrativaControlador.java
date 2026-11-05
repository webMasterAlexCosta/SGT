package com.example.siggtrat.controlador;

import com.example.siggtrat.configuracao.InvalidaCacheNegocio;
import com.example.siggtrat.dominio.pendencia.EstadoPendencia;
import com.example.siggtrat.dominio.pendencia.TipoPendencia;
import com.example.siggtrat.dto.*;
import com.example.siggtrat.servico.PendenciaAdministrativaServico;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/pendencias")
@InvalidaCacheNegocio
@PreAuthorize("hasAnyAuthority(" +
        "T(com.example.siggtrat.constante.ConstantesDominio).PERFIL_ADMINISTRADOR, " +
        "T(com.example.siggtrat.constante.ConstantesDominio).PERFIL_GESTOR_NACIONAL, " +
        "T(com.example.siggtrat.constante.ConstantesDominio).PERFIL_GESTOR_ESTADUAL, " +
        "T(com.example.siggtrat.constante.ConstantesDominio).PERFIL_GESTOR_LOCAL)")
public class PendenciaAdministrativaControlador {
    private final PendenciaAdministrativaServico servico;

    public PendenciaAdministrativaControlador(PendenciaAdministrativaServico servico) {
        this.servico = servico;
    }

    @GetMapping
    @PreAuthorize("hasAnyAuthority(" +
            "T(com.example.siggtrat.constante.ConstantesDominio).PERFIL_ADMINISTRADOR, " +
            "T(com.example.siggtrat.constante.ConstantesDominio).PERFIL_GESTOR_NACIONAL, " +
            "T(com.example.siggtrat.constante.ConstantesDominio).PERFIL_GESTOR_ESTADUAL, " +
            "T(com.example.siggtrat.constante.ConstantesDominio).PERFIL_GESTOR_LOCAL, " +
            "T(com.example.siggtrat.constante.ConstantesDominio).PERFIL_PRESIDENTE)")
    public ResponseEntity<PaginaPendenciasDTO> listar(
            @RequestParam(required = false) EstadoPendencia estado,
            @RequestParam(required = false) TipoPendencia tipo,
            @RequestParam(required = false) LocalDate inicio,
            @RequestParam(required = false) LocalDate fim,
            @RequestParam(required = false) List<Long> contratoIds,
            @RequestParam(defaultValue = "0") Integer pagina,
            @RequestParam(defaultValue = "50") Integer tamanho) {
        servico.sincronizarFimNatural(contratoIds);
        return ResponseEntity.ok(servico.listar(estado, tipo, inicio, fim, contratoIds, pagina, tamanho));
    }

    @GetMapping("/resumo")
    @PreAuthorize("hasAnyAuthority(" +
            "T(com.example.siggtrat.constante.ConstantesDominio).PERFIL_ADMINISTRADOR, " +
            "T(com.example.siggtrat.constante.ConstantesDominio).PERFIL_GESTOR_NACIONAL, " +
            "T(com.example.siggtrat.constante.ConstantesDominio).PERFIL_GESTOR_ESTADUAL, " +
            "T(com.example.siggtrat.constante.ConstantesDominio).PERFIL_GESTOR_LOCAL, " +
            "T(com.example.siggtrat.constante.ConstantesDominio).PERFIL_PRESIDENTE)")
    public ResponseEntity<ResumoPendenciasDTO> resumo(
            @RequestParam(required = false) LocalDate inicio,
            @RequestParam(required = false) LocalDate fim,
            @RequestParam(required = false) List<Long> contratoIds) {
        servico.sincronizarFimNatural(contratoIds);
        return ResponseEntity.ok(servico.resumo(inicio, fim, contratoIds));
    }

    @PostMapping("/{id}/iniciar-tratamento")
    public ResponseEntity<PendenciaDTO> iniciar(
            @PathVariable Long id,
            @Valid @RequestBody(required = false) TratamentoPendenciaRequisicao requisicao) {
        return ResponseEntity.ok(servico.iniciarTratamento(id,
                requisicao == null ? null : requisicao.observacao()));
    }

    @PostMapping("/{id}/confirmar-resolucao")
    public ResponseEntity<PendenciaDTO> confirmarResolucao(
            @PathVariable Long id,
            @Valid @RequestBody(required = false) TratamentoPendenciaRequisicao requisicao) {
        return ResponseEntity.ok(servico.confirmarResolucao(id,
                requisicao == null ? null : requisicao.observacao()));
    }
}
