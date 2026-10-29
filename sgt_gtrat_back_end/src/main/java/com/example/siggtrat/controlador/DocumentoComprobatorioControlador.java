package com.example.siggtrat.controlador;

import com.example.siggtrat.configuracao.InvalidaCacheNegocio;
import com.example.siggtrat.dto.*;
import com.example.siggtrat.seguranca.ExigirSenha;
import com.example.siggtrat.servico.DocumentoComprobatorioServico;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.nio.charset.StandardCharsets;
import java.util.List;

@InvalidaCacheNegocio
@RestController
@RequestMapping("/api/contratos/{contratoId}/documentos")
public class DocumentoComprobatorioControlador {
    private final DocumentoComprobatorioServico servico;

    public DocumentoComprobatorioControlador(DocumentoComprobatorioServico servico) {
        this.servico = servico;
    }

    @GetMapping
    public List<DocumentoComprobatorioDTO> listar(@PathVariable Long contratoId,
                                                  @RequestParam(required = false) String finalidade) {
        return servico.listar(contratoId, finalidade);
    }

    @GetMapping("/{documentoId}")
    public DocumentoComprobatorioDTO buscar(@PathVariable Long contratoId, @PathVariable Long documentoId,
                                             @RequestParam(required = false) String finalidade) {
        return servico.buscar(contratoId, documentoId, finalidade);
    }

    @GetMapping("/{documentoId}/eventos")
    public List<EventoDocumentoComprobatorioDTO> eventos(@PathVariable Long contratoId, @PathVariable Long documentoId,
                                                          @RequestParam(required = false) String finalidade) {
        return servico.listarEventos(contratoId, documentoId, finalidade);
    }

    @GetMapping("/{documentoId}/arquivo")
    public ResponseEntity<FileSystemResource> baixar(@PathVariable Long contratoId, @PathVariable Long documentoId,
                                                       @RequestParam(required = false) String finalidade) {
        BaixaDocumentoComprobatorioDTO baixa = servico.baixar(contratoId, documentoId, finalidade);
        MediaType tipo;
        try { tipo = MediaType.parseMediaType(baixa.tipoMime()); }
        catch (Exception ex) { tipo = MediaType.APPLICATION_OCTET_STREAM; }
        return ResponseEntity.ok()
                .contentType(tipo)
                .contentLength(baixa.tamanhoBytes())
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment().filename(baixa.nomeOriginal(), StandardCharsets.UTF_8).build().toString())
                .header("X-Content-Type-Options", "nosniff")
                .body(new FileSystemResource(baixa.caminho()));
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ExigirSenha
    public ResponseEntity<DocumentoComprobatorioDTO> criar(
            @PathVariable Long contratoId,
            @RequestPart("metadados") CadastroDocumentoComprobatorioRequisicao requisicao,
            @RequestPart(value = "arquivo", required = false) MultipartFile arquivo) {
        return ResponseEntity.status(HttpStatus.CREATED).body(servico.criar(contratoId, requisicao, arquivo));
    }

    @PostMapping("/{documentoId}/retificar-sei")
    @ExigirSenha
    public DocumentoComprobatorioDTO retificarSei(@PathVariable Long contratoId, @PathVariable Long documentoId,
                                                   @RequestBody RetificacaoSeiDocumentoRequisicao requisicao) {
        return servico.retificarSei(contratoId, documentoId, requisicao);
    }

    @PostMapping(value = "/{documentoId}/substituir", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ExigirSenha
    public DocumentoComprobatorioDTO substituir(
            @PathVariable Long contratoId, @PathVariable Long documentoId,
            @RequestPart("metadados") SubstituicaoDocumentoComprobatorioRequisicao requisicao,
            @RequestPart(value = "arquivo", required = false) MultipartFile arquivo) {
        return servico.substituir(contratoId, documentoId, requisicao, arquivo);
    }

    @PostMapping("/{documentoId}/invalidar")
    @ExigirSenha
    public DocumentoComprobatorioDTO invalidar(@PathVariable Long contratoId, @PathVariable Long documentoId,
                                                @RequestBody AcaoDocumentoComprobatorioRequisicao requisicao) {
        return servico.invalidar(contratoId, documentoId, requisicao);
    }

    @DeleteMapping("/{documentoId}")
    @ExigirSenha
    public ResponseEntity<Void> descartarRascunho(@PathVariable Long contratoId, @PathVariable Long documentoId) {
        servico.descartarRascunho(contratoId, documentoId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{documentoId}/permissoes")
    public List<PermissaoDocumentoRestritoDTO> permissoes(@PathVariable Long contratoId, @PathVariable Long documentoId) {
        return servico.listarPermissoes(contratoId, documentoId);
    }

    @PostMapping("/{documentoId}/permissoes")
    @ExigirSenha
    public PermissaoDocumentoRestritoDTO conceder(@PathVariable Long contratoId, @PathVariable Long documentoId,
                                                   @RequestBody ConcessaoAcessoDocumentoRequisicao requisicao) {
        return servico.concederAcesso(contratoId, documentoId, requisicao);
    }

    @PostMapping("/{documentoId}/permissoes/{permissaoId}/revogar")
    @ExigirSenha
    public PermissaoDocumentoRestritoDTO revogar(@PathVariable Long contratoId, @PathVariable Long documentoId,
                                                  @PathVariable Long permissaoId,
                                                  @RequestBody AcaoDocumentoComprobatorioRequisicao requisicao) {
        return servico.revogarAcesso(contratoId, documentoId, permissaoId, requisicao);
    }

    @GetMapping("/{documentoId}/acessos")
    public List<AcessoDocumentoRestritoDTO> acessos(@PathVariable Long contratoId, @PathVariable Long documentoId) {
        return servico.listarAcessos(contratoId, documentoId);
    }
}

