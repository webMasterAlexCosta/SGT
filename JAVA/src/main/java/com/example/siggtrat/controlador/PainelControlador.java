package com.example.siggtrat.controlador;

import com.example.siggtrat.dto.AtividadePainelDTO;
import com.example.siggtrat.dto.PainelRankingDTO;
import com.example.siggtrat.dto.PainelGestaoDTO;
import com.example.siggtrat.dto.PainelEfetivoDTO;
import com.example.siggtrat.dto.EstatisticasPainelDTO;
import com.example.siggtrat.servico.PainelServico;
import org.springframework.data.domain.Page;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/painel")
public class PainelControlador implements com.example.siggtrat.diagnostico.DiagnosticoOperacao {
    private static final org.slf4j.Logger LOG_DIAGNOSTICO = org.slf4j.LoggerFactory.getLogger(PainelControlador.class);

    @Override public org.slf4j.Logger loggerDiagnostico() { return LOG_DIAGNOSTICO; }


    private final PainelServico dashboardServico;

    public PainelControlador(PainelServico dashboardServico) {
        this.dashboardServico = dashboardServico;
    }

    @GetMapping("/efetivo/filtros")
    public ResponseEntity<PainelEfetivoDTO.Filtros> obterFiltrosEfetivo(
            @RequestParam(required = false) Long contratoId,
            @RequestParam(required = false) Long lotacaoId,
            @RequestParam(required = false) Long jornadaId,
            @RequestParam(required = false) Long cargoId,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate referencia,
            @RequestParam(defaultValue = "DIARIO") String periodo,
            @RequestParam(defaultValue = "false") boolean compararAnoAnterior) {
        return ResponseEntity.ok(dashboardServico.obterFiltrosEfetivo(
                contratoId, lotacaoId, jornadaId, cargoId, referencia, periodo, compararAnoAnterior));
    }

    @GetMapping("/efetivo")
    public ResponseEntity<PainelEfetivoDTO> obterIndicadorEfetivo(
            @RequestParam(required = false)
            @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate referencia,
            @RequestParam(required = false, defaultValue = "DIARIO") String periodo,
            @RequestParam(required = false) Long contratoId,
            @RequestParam(required = false) Long lotacaoId,
            @RequestParam(required = false) Long jornadaId,
            @RequestParam(required = false) Long cargoId,
            @RequestParam(required = false, defaultValue = "false") boolean compararAnoAnterior) {
        return ResponseEntity.ok(dashboardServico.obterIndicadorEfetivo(
                referencia, periodo, contratoId, lotacaoId, jornadaId, cargoId, compararAnoAnterior));
    }

    @GetMapping("/gestao")
    public ResponseEntity<PainelGestaoDTO> obterGestao(
            @RequestParam(required = false)
            @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate referencia,
            @RequestParam(required = false, defaultValue = "1") Integer periodoDias) {
        return ResponseEntity.ok(dashboardServico.obterGestao(referencia, periodoDias));
    }

    @GetMapping("/estatisticas")
    public ResponseEntity<EstatisticasPainelDTO> obterEstatisticas() {
        return ResponseEntity.ok(dashboardServico.obterEstatisticas());
    }

    @GetMapping("/atividades")
    public ResponseEntity<Page<AtividadePainelDTO>> obterAtividadesRecentes(
            @RequestParam(defaultValue = "5") int size) {
        return ResponseEntity.ok(dashboardServico.obterAtividadesRecentes(size));
    }

    @GetMapping("/ranking-lotacoes")
    public ResponseEntity<List<PainelRankingDTO>> obterRankingLotacoes(
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate inicio,
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate fim,
            @RequestParam(required = false) Long contratoId) {
        return ResponseEntity.ok(dashboardServico.obterRankingLotacoes(inicio, fim, contratoId));
    }
}
