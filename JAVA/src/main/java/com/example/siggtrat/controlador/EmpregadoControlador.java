package com.example.siggtrat.controlador;

import com.example.siggtrat.dto.EmpregadoDTO;
import com.example.siggtrat.dto.EmpregadoFrequenciaDTO;
import com.example.siggtrat.dto.EmpregadoListaDTO;
import com.example.siggtrat.dto.HistoricoVinculoContratoDTO;
import com.example.siggtrat.servico.EmpregadoServico;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/empregados")
public class EmpregadoControlador implements com.example.siggtrat.diagnostico.DiagnosticoOperacao {
    private static final org.slf4j.Logger LOG_DIAGNOSTICO = org.slf4j.LoggerFactory.getLogger(EmpregadoControlador.class);

    @Override public org.slf4j.Logger loggerDiagnostico() { return LOG_DIAGNOSTICO; }


    private final EmpregadoServico empregadoServico;

    public EmpregadoControlador(EmpregadoServico empregadoServico) {
        this.empregadoServico = empregadoServico;
    }

    @GetMapping("/{id}/historico-contratos")
    public ResponseEntity<List<HistoricoVinculoContratoDTO>> obterHistoricoContratos(@PathVariable Long id,
            @RequestParam(required = false) LocalDate data,
            @RequestParam(required = false) java.time.Instant conhecidoAte) {
        return ResponseEntity.ok(empregadoServico.obterHistoricoContratos(id, data, conhecidoAte));
    }

    @GetMapping
    public Page<EmpregadoDTO> listarTodos(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) Long lotacaoId,
            @RequestParam(required = false) Long cargoId,
            @RequestParam(required = false) Long contratoId,
            @RequestParam(required = false, defaultValue = "false") Boolean semContrato,
            Pageable pageable) {
        return empregadoServico.listarTodos(search, status, lotacaoId, cargoId, contratoId, semContrato, pageable);
    }

    @GetMapping("/resumo")
    public Page<EmpregadoListaDTO> obterResumo(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) Long lotacaoId,
            @RequestParam(required = false) Long cargoId,
            @RequestParam(required = false) Long turnoId,
            @RequestParam(name = "contratoIds", required = false) List<String> contratoIds,
            @RequestParam(required = false, defaultValue = "false") Boolean incluirSemContrato,
            Pageable pageable) {
        return empregadoServico.obterResumo(
                search, status, lotacaoId, cargoId, turnoId, contratoIds, incluirSemContrato, pageable);
    }

    @GetMapping("/filtrados-frequencia")
    public ResponseEntity<Page<EmpregadoFrequenciaDTO>> obterEmpregadosParaFrequencia(
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate dataRef,
            @RequestParam(required = false) Long contratoId,
            @RequestParam(required = false) List<Long> contratoIds,
            @RequestParam(required = false) Long lotacaoId,
            @RequestParam(required = false) Long turnoId,
            @RequestParam(required = false) String searchTerm,
            @RequestParam(defaultValue = "false") boolean skipCount,
            Pageable pageable) {
        return ResponseEntity.ok(empregadoServico.obterEmpregadosParaFrequencia(
                dataRef, contratoId, contratoIds, lotacaoId, turnoId, searchTerm, skipCount, pageable));
    }

    @GetMapping("/{id}")
    public ResponseEntity<EmpregadoDTO> buscarPorId(@PathVariable Long id) {
        return ResponseEntity.ok(empregadoServico.buscarPorId(id));
    }

}
