package com.example.siggtrat.controlador;

import com.example.siggtrat.configuracao.InvalidaCacheNegocio;
import com.example.siggtrat.dto.BaixaAnexoDTO;
import com.example.siggtrat.dto.CriacaoOcorrenciaDTO;
import com.example.siggtrat.dto.OcorrenciaHistoricoDTO;
import com.example.siggtrat.dto.OcorrenciaResumoDTO;
import com.example.siggtrat.seguranca.ExigirSenha;
import com.example.siggtrat.servico.OcorrenciaServico;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.util.List;

@InvalidaCacheNegocio
@RestController
@RequestMapping("/api/ocorrencias")
public class OcorrenciaControlador {

    private final OcorrenciaServico ocorrenciaServico;

    public OcorrenciaControlador(OcorrenciaServico ocorrenciaServico) {
        this.ocorrenciaServico = ocorrenciaServico;
    }

    @GetMapping
    public Page<OcorrenciaResumoDTO> listarTodos(
            @RequestParam(required = false) Long empregadoId,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate data,
            @RequestParam(required = false) Long lotacaoId,
            @RequestParam(required = false) Long jornadaId,
            @RequestParam(required = false) Long contratoId,
            Pageable pageable) {
        return ocorrenciaServico.listarTodos(empregadoId, data, lotacaoId, jornadaId, contratoId, pageable);
    }

    @GetMapping("/{id}")
    public OcorrenciaHistoricoDTO consultarHistorico(@PathVariable Long id,
                                                   @RequestParam(required = false) String finalidade) {
        return ocorrenciaServico.consultarHistorico(id, finalidade);
    }

    @GetMapping("/anexos/{anexoId}")
    public ResponseEntity<Resource> baixarAnexo(@PathVariable Long anexoId,
                                               @RequestParam(required = false) String finalidade) {
        BaixaAnexoDTO download = ocorrenciaServico.baixarAnexo(anexoId, finalidade);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        download.disposition() + "; filename=\"" + download.filename() + "\"")
                .contentType(MediaType.parseMediaType(download.contentType()))
                .body(download.resource());
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ExigirSenha
    public ResponseEntity<OcorrenciaResumoDTO> criar(
            @RequestParam("empregadoId") Long empregadoId,
            @RequestParam("motivo") String motivo,
            @RequestParam("dataOcorrencia") @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate dataOcorrencia,
            @RequestParam(value = "observacao", required = false) String observacao,
            @RequestParam(value = "tipoRo", required = false) String tipoRo,
            @RequestParam(value = "numeroSei", required = false) String numeroSei,
            @RequestParam(value = "dias", required = false) Integer dias,
            @RequestParam(value = "classificacoesAnexos", required = false) List<String> classificacoesAnexos,
            @RequestParam(value = "chaveIdempotencia", required = false) String chaveIdempotencia,
            @RequestParam(value = "anexos", required = false) List<MultipartFile> arquivos) {
        CriacaoOcorrenciaDTO dto = new CriacaoOcorrenciaDTO(
                empregadoId, motivo, dataOcorrencia, observacao, tipoRo, numeroSei, dias, classificacoesAnexos, chaveIdempotencia);
        return ResponseEntity.status(HttpStatus.CREATED).body(ocorrenciaServico.criar(dto, arquivos));
    }

    @DeleteMapping("/{id}")
    @ExigirSenha
    public ResponseEntity<Void> excluir(@PathVariable Long id) {
        ocorrenciaServico.excluir(id);
        return ResponseEntity.noContent().build();
    }
}
