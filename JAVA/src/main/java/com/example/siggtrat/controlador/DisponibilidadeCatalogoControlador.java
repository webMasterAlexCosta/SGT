package com.example.siggtrat.controlador;

import com.example.siggtrat.dominio.catalogo.TipoCatalogoTemporal;
import com.example.siggtrat.dto.DisponibilidadeCatalogoDTO;
import com.example.siggtrat.dto.PreviaTransicaoCatalogoDTO;
import com.example.siggtrat.dto.TransicaoCatalogoDTO;
import com.example.siggtrat.dto.TransicaoDisponibilidadeCatalogoRequisicao;
import com.example.siggtrat.servico.DisponibilidadeCatalogoTemporalServico;
import com.example.siggtrat.servico.TransicaoDisponibilidadeCatalogoServico;
import com.example.siggtrat.seguranca.ExigirSenha;
import org.springframework.web.bind.annotation.*;
import java.time.Instant;
import java.time.LocalDate;

@RestController
@com.example.siggtrat.configuracao.InvalidaCacheNegocio
@RequestMapping("/api/catalogos-temporais")
public class DisponibilidadeCatalogoControlador implements com.example.siggtrat.diagnostico.DiagnosticoOperacao {
    private static final org.slf4j.Logger LOG_DIAGNOSTICO = org.slf4j.LoggerFactory.getLogger(DisponibilidadeCatalogoControlador.class);

    @Override public org.slf4j.Logger loggerDiagnostico() { return LOG_DIAGNOSTICO; }

    private final DisponibilidadeCatalogoTemporalServico servico;
    private final TransicaoDisponibilidadeCatalogoServico transicoes;
    public DisponibilidadeCatalogoControlador(DisponibilidadeCatalogoTemporalServico servico,
                                              TransicaoDisponibilidadeCatalogoServico transicoes) {
        this.servico = servico; this.transicoes = transicoes;
    }

    @GetMapping("/{tipo}/{id}/disponibilidade")
    public DisponibilidadeCatalogoDTO consultar(@PathVariable TipoCatalogoTemporal tipo, @PathVariable Long id,
            @RequestParam LocalDate data, @RequestParam(required = false) Instant conhecidoAte) {
        return servico.consultar(tipo, id, data, conhecidoAte);
    }

    @PostMapping("/{tipo}/{id}/transicoes/previa")
    public PreviaTransicaoCatalogoDTO previa(@PathVariable TipoCatalogoTemporal tipo, @PathVariable Long id,
            @RequestBody TransicaoDisponibilidadeCatalogoRequisicao requisicao) {
        return transicoes.previa(tipo, id, requisicao);
    }

    @ExigirSenha
    @PostMapping("/{tipo}/{id}/transicoes")
    public TransicaoCatalogoDTO registrar(@PathVariable TipoCatalogoTemporal tipo, @PathVariable Long id,
            @RequestBody TransicaoDisponibilidadeCatalogoRequisicao requisicao) {
        return transicoes.registrar(tipo, id, requisicao);
    }
}
