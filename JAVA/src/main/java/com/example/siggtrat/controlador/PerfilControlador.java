package com.example.siggtrat.controlador;

import com.example.siggtrat.dto.SenhaAtualDTO;
import com.example.siggtrat.dto.ValidacaoSenhaDTO;
import com.example.siggtrat.dto.AlteracaoSenhaPerfilDTO;
import com.example.siggtrat.dto.TokenTotpDTO;
import com.example.siggtrat.dto.DesativacaoSegundoFatorDTO;
import com.example.siggtrat.servico.PerfilServico;
import jakarta.validation.Valid;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/perfil")
public class PerfilControlador implements com.example.siggtrat.diagnostico.DiagnosticoOperacao {
    private static final org.slf4j.Logger LOG_DIAGNOSTICO = org.slf4j.LoggerFactory.getLogger(PerfilControlador.class);

    @Override public org.slf4j.Logger loggerDiagnostico() { return LOG_DIAGNOSTICO; }

    private final PerfilServico servico;
    public PerfilControlador(PerfilServico servico) { this.servico = servico; }

    @GetMapping("/2fa/status")
    public ResponseEntity<?> statusSegundoFator() { return servico.statusSegundoFator(); }

    @PostMapping("/2fa/configurar")
    public ResponseEntity<?> configurarSegundoFator(@RequestBody SenhaAtualDTO payload) { return servico.configurarSegundoFator(payload); }

    @PostMapping("/2fa/ativar")
    public ResponseEntity<?> ativarSegundoFator(@RequestBody TokenTotpDTO payload) { return servico.ativarSegundoFator(payload); }

    @DeleteMapping("/2fa")
    public ResponseEntity<?> desativarSegundoFator(@RequestBody DesativacaoSegundoFatorDTO payload) { return servico.desativarSegundoFator(payload); }

    @PostMapping("/alterar-senha")
    public ResponseEntity<?> alterarSenha(@Valid @RequestBody AlteracaoSenhaPerfilDTO payload) { return servico.alterarSenha(payload); }

    @PostMapping("/validar-senha")
    public ResponseEntity<?> validarSenha(@RequestBody ValidacaoSenhaDTO payload) { return servico.validarSenha(payload); }

    @PostMapping(value = "/avatar", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> enviarAvatar(@RequestParam("file") MultipartFile file) { return servico.enviarAvatar(file); }

    @GetMapping("/avatar/{nomeArquivo}")
    public ResponseEntity<Resource> obterAvatar(@PathVariable String nomeArquivo) { return servico.obterAvatar(nomeArquivo); }
}
