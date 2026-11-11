package com.example.siggtrat.controlador;

import com.example.siggtrat.dto.ExportacaoRelatorioStatusDTO;
import com.example.siggtrat.dto.RelatorioAbsenteismoDTO;
import com.example.siggtrat.dto.RelatorioAbsenteismoPaginaDTO;
import com.example.siggtrat.dto.RelatorioAbsenteismoResumoDTO;
import com.example.siggtrat.dto.RelatorioFrequenciaConsolidadoDTO;
import com.example.siggtrat.dto.RelatorioFrequenciaPaginaDTO;
import com.example.siggtrat.dto.RelatorioFiltrosDTO;
import com.example.siggtrat.dto.RelatorioHorasDTO;
import com.example.siggtrat.dto.RelatorioTerceirizadosDTO;
import com.example.siggtrat.servico.RelatorioConsultaServico;
import com.example.siggtrat.servico.RelatorioExportacaoAssincronaServico;
import com.example.siggtrat.servico.RelatorioExportacaoServico;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/relatorios")
@PreAuthorize("hasAnyAuthority(" +
        "T(com.example.siggtrat.constante.ConstantesDominio).PERFIL_ADMINISTRADOR, " +
        "T(com.example.siggtrat.constante.ConstantesDominio).PERFIL_GESTOR_NACIONAL, " +
        "T(com.example.siggtrat.constante.ConstantesDominio).PERFIL_PRESIDENTE, " +
        "T(com.example.siggtrat.constante.ConstantesDominio).PERFIL_GESTOR_ESTADUAL, " +
        "T(com.example.siggtrat.constante.ConstantesDominio).PERFIL_GESTOR_LOCAL)")
public class RelatorioControlador implements com.example.siggtrat.diagnostico.DiagnosticoOperacao {
    private static final org.slf4j.Logger LOG_DIAGNOSTICO = org.slf4j.LoggerFactory.getLogger(RelatorioControlador.class);

    @Override public org.slf4j.Logger loggerDiagnostico() { return LOG_DIAGNOSTICO; }

    private final RelatorioConsultaServico servico;
    private final RelatorioExportacaoServico exportacaoServico;
    private final RelatorioExportacaoAssincronaServico exportacaoAssincronaServico;

    public RelatorioControlador(
            RelatorioConsultaServico servico,
            RelatorioExportacaoServico exportacaoServico,
            RelatorioExportacaoAssincronaServico exportacaoAssincronaServico
    ) {
        this.servico = servico;
        this.exportacaoServico = exportacaoServico;
        this.exportacaoAssincronaServico = exportacaoAssincronaServico;
    }


    @GetMapping("/filtros")
    public ResponseEntity<RelatorioFiltrosDTO> filtros(
            @RequestParam(required = false) LocalDate inicio,
            @RequestParam(required = false) LocalDate fim) {
        return ResponseEntity.ok(servico.filtros(inicio, fim));
    }

    @GetMapping("/frequencia-consolidada")
    public ResponseEntity<RelatorioFrequenciaConsolidadoDTO> frequenciaConsolidada(
            @RequestParam LocalDate inicio,
            @RequestParam LocalDate fim,
            @RequestParam(required = false) List<Long> contratoIds,
            @RequestParam(required = false) Long lotacaoId,
            @RequestParam(required = false) Long jornadaId) {
        return ResponseEntity.ok(servico.frequenciaConsolidada(inicio, fim, contratoIds, lotacaoId, jornadaId));
    }

    @GetMapping("/frequencia-consolidada/paginada")
    public ResponseEntity<RelatorioFrequenciaPaginaDTO> frequenciaConsolidadaPaginada(
            @RequestParam LocalDate inicio,
            @RequestParam LocalDate fim,
            @RequestParam(required = false) List<Long> contratoIds,
            @RequestParam(required = false) Long lotacaoId,
            @RequestParam(required = false) Long jornadaId,
            @RequestParam(defaultValue = "0") Integer page,
            @RequestParam(defaultValue = "100") Integer size) {
        return ResponseEntity.ok(servico.frequenciaConsolidadaPaginada(
                inicio, fim, contratoIds, lotacaoId, jornadaId, page, size));
    }

    @GetMapping("/frequencia-consolidada/exportacao")
    public ResponseEntity<StreamingResponseBody> exportarFrequenciaConsolidada(
            @RequestParam LocalDate inicio, @RequestParam LocalDate fim,
            @RequestParam(required = false) List<Long> contratoIds,
            @RequestParam(required = false) Long lotacaoId,
            @RequestParam(required = false) Long jornadaId,
            @RequestParam(defaultValue = "XLSX") String formato) {
        String tipo = validarFormato(formato);
        var contexto = exportacaoServico.criarContextoFrequencia(contratoIds, lotacaoId, jornadaId);
        String nome = exportacaoServico.nomeArquivoFrequencia(contexto, tipo);
        StreamingResponseBody corpo = output -> {
            if ("PDF".equals(tipo)) {
                exportacaoServico.exportarFrequenciaPdf(output, inicio, fim, contratoIds, lotacaoId, jornadaId);
            } else {
                exportacaoServico.exportarFrequenciaXlsx(output, inicio, fim, contratoIds, lotacaoId, jornadaId);
            }
        };
        return respostaDownload(corpo, nome, tipo);
    }

    @PostMapping("/frequencia-consolidada/exportacao/solicitacao")
    public ResponseEntity<ExportacaoRelatorioStatusDTO> solicitarExportacaoFrequencia(
            @RequestParam LocalDate inicio, @RequestParam LocalDate fim,
            @RequestParam(required = false) List<Long> contratoIds,
            @RequestParam(required = false) Long lotacaoId,
            @RequestParam(required = false) Long jornadaId,
            @RequestParam(defaultValue = "XLSX") String formato,
            @RequestParam(required = false) Long totalEstimado) {
        return ResponseEntity.accepted().body(exportacaoAssincronaServico.solicitarFrequencia(
                inicio, fim, contratoIds, lotacaoId, jornadaId, formato, totalEstimado));
    }

    @GetMapping("/terceirizados/consolidado")
    public ResponseEntity<RelatorioTerceirizadosDTO> consolidadoTerceirizados(
            @RequestParam(required = false) String busca,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) Long contratoId,
            @RequestParam(required = false, defaultValue = "false") Boolean semContrato,
            @RequestParam(required = false) Long lotacaoId,
            @RequestParam(required = false, defaultValue = "0") Integer page,
            @RequestParam(required = false, defaultValue = "100") Integer size) {
        return ResponseEntity.ok(servico.consolidadoTerceirizados(busca, status, contratoId, semContrato, lotacaoId, page, size));
    }

    @GetMapping("/terceirizados/exportacao")
    public ResponseEntity<StreamingResponseBody> exportarTerceirizados(
            @RequestParam(required = false) String busca,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) Long contratoId,
            @RequestParam(required = false, defaultValue = "false") Boolean semContrato,
            @RequestParam(required = false) Long lotacaoId,
            @RequestParam(defaultValue = "XLSX") String formato) {
        String tipo = validarFormato(formato);
        String nome = "Relatorio_Visao_Consolidada." + tipo.toLowerCase();
        StreamingResponseBody corpo = output -> {
            if ("PDF".equals(tipo)) {
                exportacaoServico.exportarTerceirizadosPdf(
                        output, busca, status, contratoId, semContrato, lotacaoId);
            } else {
                exportacaoServico.exportarTerceirizadosXlsx(
                        output, busca, status, contratoId, semContrato, lotacaoId);
            }
        };
        return respostaDownload(corpo, nome, tipo);
    }

    @PostMapping("/terceirizados/exportacao/solicitacao")
    public ResponseEntity<ExportacaoRelatorioStatusDTO> solicitarExportacaoTerceirizados(
            @RequestParam(required = false) String busca,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) Long contratoId,
            @RequestParam(required = false, defaultValue = "false") Boolean semContrato,
            @RequestParam(required = false) Long lotacaoId,
            @RequestParam(defaultValue = "XLSX") String formato,
            @RequestParam(required = false) Long totalEstimado) {
        return ResponseEntity.accepted().body(exportacaoAssincronaServico.solicitarTerceirizados(
                busca, status, contratoId, semContrato, lotacaoId, formato, totalEstimado));
    }

    @GetMapping("/absenteismo")
    public ResponseEntity<List<RelatorioAbsenteismoDTO>> gerarRelatorioAbsenteismo(
            @RequestParam int mes, @RequestParam int ano,
            @RequestParam(required = false) Long contratoId,
            @RequestParam(required = false) Long lotacaoId,
            @RequestParam(required = false) Long jornadaId) {
        return ResponseEntity.ok(servico.gerarRelatorioAbsenteismo(mes, ano, contratoId, lotacaoId, jornadaId));
    }

    @GetMapping("/absenteismo/paginado")
    public ResponseEntity<RelatorioAbsenteismoPaginaDTO> gerarRelatorioAbsenteismoPaginado(
            @RequestParam int mes, @RequestParam int ano,
            @RequestParam(required = false) Long contratoId,
            @RequestParam(required = false) Long lotacaoId,
            @RequestParam(required = false) Long jornadaId,
            @RequestParam(defaultValue = "0") Integer page,
            @RequestParam(defaultValue = "100") Integer size) {
        return ResponseEntity.ok(servico.gerarRelatorioAbsenteismoPaginado(
                mes, ano, contratoId, lotacaoId, jornadaId, page, size));
    }

    @GetMapping("/absenteismo/resumo")
    public ResponseEntity<RelatorioAbsenteismoResumoDTO> gerarResumoAbsenteismo(
            @RequestParam int mes, @RequestParam int ano,
            @RequestParam(required = false) Long contratoId,
            @RequestParam(required = false) Long lotacaoId,
            @RequestParam(required = false) Long jornadaId,
            @RequestParam(defaultValue = "50") int limiteDisrupcao) {
        return ResponseEntity.ok(servico.gerarResumoAbsenteismo(
                mes, ano, contratoId, lotacaoId, jornadaId, limiteDisrupcao));
    }

    @GetMapping("/absenteismo/exportacao")
    public ResponseEntity<StreamingResponseBody> exportarAbsenteismo(
            @RequestParam int mes, @RequestParam int ano,
            @RequestParam(required = false) Long contratoId,
            @RequestParam(required = false) Long lotacaoId,
            @RequestParam(required = false) Long jornadaId,
            @RequestParam(defaultValue = "3") double meta,
            @RequestParam(defaultValue = "50") double limiteDisrupcao,
            @RequestParam(defaultValue = "XLSX") String formato) {
        if (mes < 1 || mes > 12 || ano < 2000 || ano > 2200)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Competência inválida.");
        YearMonth referencia = YearMonth.of(ano, mes);
        LocalDate inicio = referencia.atDay(1);
        LocalDate fim = referencia.atEndOfMonth();
        List<Long> contratos = contratoId == null ? null : List.of(contratoId);
        String tipo = validarFormato(formato);
        var contexto = exportacaoServico.criarContextoAbsenteismo(contratos, lotacaoId, jornadaId);
        String nome = exportacaoServico.nomeArquivoAbsenteismo(contexto, tipo);
        StreamingResponseBody corpo = output -> {
            if ("PDF".equals(tipo)) {
                exportacaoServico.exportarAbsenteismoPdf(
                        output, inicio, fim, contratos, lotacaoId, jornadaId, meta, limiteDisrupcao, contexto);
            } else {
                exportacaoServico.exportarAbsenteismoXlsx(
                        output, inicio, fim, contratos, lotacaoId, jornadaId, meta, limiteDisrupcao, contexto);
            }
        };
        return respostaDownload(corpo, nome, tipo);
    }

    @PostMapping("/absenteismo/exportacao/solicitacao")
    public ResponseEntity<ExportacaoRelatorioStatusDTO> solicitarExportacaoAbsenteismo(
            @RequestParam int mes, @RequestParam int ano,
            @RequestParam(required = false) Long contratoId,
            @RequestParam(required = false) Long lotacaoId,
            @RequestParam(required = false) Long jornadaId,
            @RequestParam(defaultValue = "3") double meta,
            @RequestParam(defaultValue = "50") double limiteDisrupcao,
            @RequestParam(defaultValue = "XLSX") String formato,
            @RequestParam(required = false) Long totalEstimado) {
        if (mes < 1 || mes > 12 || ano < 2000 || ano > 2200)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Competência inválida.");
        YearMonth referencia = YearMonth.of(ano, mes);
        List<Long> contratos = contratoId == null ? null : List.of(contratoId);
        return ResponseEntity.accepted().body(exportacaoAssincronaServico.solicitarAbsenteismo(
                referencia.atDay(1), referencia.atEndOfMonth(), contratos, lotacaoId, jornadaId,
                meta, limiteDisrupcao, formato, totalEstimado));
    }

    @GetMapping("/exportacoes/{id}")
    public ResponseEntity<ExportacaoRelatorioStatusDTO> consultarExportacao(@PathVariable UUID id) {
        return ResponseEntity.ok(exportacaoAssincronaServico.consultar(id));
    }

    @PostMapping("/exportacoes/{id}/cancelamento")
    public ResponseEntity<ExportacaoRelatorioStatusDTO> cancelarExportacao(@PathVariable UUID id) {
        return ResponseEntity.ok(exportacaoAssincronaServico.cancelar(id));
    }

    @GetMapping("/exportacoes/{id}/arquivo")
    public ResponseEntity<StreamingResponseBody> baixarExportacao(@PathVariable UUID id) {
        // A autorização e a resolução do arquivo acontecem antes de o MVC
        // entregar o StreamingResponseBody para outra thread. Isso evita que a
        // transferência dependa do SecurityContext assíncrono.
        var transferencia = exportacaoAssincronaServico.prepararTransferencia(id);
        StreamingResponseBody corpo = output -> exportacaoAssincronaServico.transferirArquivo(transferencia, output);
        return respostaDownload(
                corpo, transferencia.nomeArquivo(), transferencia.formato(), transferencia.tamanhoBytes());
    }

    @GetMapping("/horas-trabalhadas")
    public ResponseEntity<List<RelatorioHorasDTO>> gerarRelatorioHorasTrabalhadas(
            @RequestParam int mes, @RequestParam int ano, @RequestParam Long contratoId,
            @RequestParam(required = false) Long lotacaoId,
            @RequestParam(required = false) Long jornadaId) {
        return ResponseEntity.ok(servico.gerarRelatorioHorasTrabalhadas(mes, ano, contratoId, lotacaoId, jornadaId));
    }
    private String validarFormato(String formato) {
        String valor = formato == null ? "XLSX" : formato.trim().toUpperCase();
        if (!"XLSX".equals(valor) && !"PDF".equals(valor))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Formato deve ser XLSX ou PDF.");
        return valor;
    }

    private ResponseEntity<StreamingResponseBody> respostaDownload(StreamingResponseBody corpo, String nome, String formato) {
        return respostaDownload(corpo, nome, formato, null);
    }

    private ResponseEntity<StreamingResponseBody> respostaDownload(
            StreamingResponseBody corpo, String nome, String formato, Long tamanhoBytes
    ) {
        MediaType mediaType = "PDF".equals(formato)
                ? MediaType.APPLICATION_PDF
                : MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        var resposta = ResponseEntity.ok()
                .contentType(mediaType)
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + nome + "\"")
                .header(HttpHeaders.CACHE_CONTROL, "no-store")
                .header("X-Content-Type-Options", "nosniff");
        if (tamanhoBytes != null && tamanhoBytes > 0L) {
            resposta.contentLength(tamanhoBytes);
        }
        return resposta.body(corpo);
    }

}
