package com.example.siggtrat.controlador;

import com.example.siggtrat.dto.AlteracaoSenhaInicialAutenticacaoDTO;
import com.example.siggtrat.dto.CredenciaisLoginDTO;
import com.example.siggtrat.dto.CadastroAutenticacaoDTO;
import com.example.siggtrat.dto.RenovacaoSessaoDTO;
import com.example.siggtrat.dto.SolicitacaoRecuperacaoSenhaDTO;
import com.example.siggtrat.dto.ConfirmacaoRecuperacaoSenhaDTO;
import com.example.siggtrat.dto.ValidacaoTokenRecuperacaoSenhaDTO;
import com.example.siggtrat.dto.MensagemApiDTO;
import com.example.siggtrat.dto.AutenticacaoSuperintendenciaDTO;
import com.example.siggtrat.dto.AutenticacaoGestaoLocalDTO;
import com.example.siggtrat.dto.TokenCsrfDTO;
import com.example.siggtrat.servico.AutenticacaoServico;
import com.example.siggtrat.servico.RecuperacaoSenhaServico;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/autenticacao")
public class AutenticacaoControlador implements com.example.siggtrat.diagnostico.DiagnosticoOperacao {
    private static final org.slf4j.Logger LOG_DIAGNOSTICO = org.slf4j.LoggerFactory.getLogger(AutenticacaoControlador.class);

    @Override public org.slf4j.Logger loggerDiagnostico() { return LOG_DIAGNOSTICO; }


    private final AutenticacaoServico authServico;
    private final RecuperacaoSenhaServico recuperacaoSenhaServico;

    public AutenticacaoControlador(
            AutenticacaoServico authServico,
            RecuperacaoSenhaServico recuperacaoSenhaServico
    ) {
        this.authServico = authServico;
        this.recuperacaoSenhaServico = recuperacaoSenhaServico;
    }

    @PostMapping("/alterar-senha-inicial")
    public ResponseEntity<?> alterarSenhaInicial(
            @RequestBody AlteracaoSenhaInicialAutenticacaoDTO data,
            HttpServletRequest request) {
        return authServico.alterarSenhaInicial(data, request);
    }

    @GetMapping("/sessao")
    public ResponseEntity<?> informacoesSessao(
            HttpServletRequest request,
            @RequestParam(name = "touch", defaultValue = "false") boolean touch) {
        return authServico.informacoesSessao(request, touch);
    }

    @PostMapping("/renovar-sessao")
    public ResponseEntity<?> renovarSessao(
            @RequestBody(required = false) RenovacaoSessaoDTO payload,
            HttpServletRequest request) {
        return authServico.renovarSessao(payload, request);
    }

    @PostMapping("/autenticar")
    public ResponseEntity<?> autenticar(
            @RequestBody(required = false) CredenciaisLoginDTO loginData,
            HttpServletRequest request) {
        return authServico.autenticar(loginData, request);
    }

    /** Inicializa XSRF-TOKEN para o React e informa o cabeçalho de retorno. */
    @GetMapping("/csrf")
    public ResponseEntity<TokenCsrfDTO> tokenCsrf(CsrfToken csrfToken) {
        return ResponseEntity.ok(new TokenCsrfDTO(
                csrfToken.getHeaderName(), csrfToken.getParameterName(), csrfToken.getToken()));
    }

    @GetMapping("/superintendencias")
    public ResponseEntity<List<AutenticacaoSuperintendenciaDTO>> listarSuperintendenciasParaCadastro() {
        return authServico.listarSuperintendenciasParaCadastro();
    }

    @GetMapping("/gestoes-locais")
    public ResponseEntity<List<AutenticacaoGestaoLocalDTO>> listarGestoesLocaisParaCadastro(
            @RequestParam Long superintendenciaId
    ) {
        return authServico.listarGestoesLocaisParaCadastro(superintendenciaId);
    }

    @PostMapping("/cadastrar")
    public ResponseEntity<?> registrar(@Valid @RequestBody CadastroAutenticacaoDTO userData) {
        return authServico.registrar(userData);
    }

    @PostMapping("/recuperar-senha/solicitar")
    public ResponseEntity<MensagemApiDTO> solicitarRecuperacaoSenha(
            @Valid @RequestBody SolicitacaoRecuperacaoSenhaDTO payload,
            HttpServletRequest request
    ) {
        return ResponseEntity.ok(
                MensagemApiDTO.sucesso(recuperacaoSenhaServico.solicitar(payload.email(), request))
        );
    }

    @GetMapping("/recuperar-senha/validar")
    public ResponseEntity<ValidacaoTokenRecuperacaoSenhaDTO> validarTokenRecuperacaoSenha(
            @RequestParam("token") String token
    ) {
        return ResponseEntity.ok(recuperacaoSenhaServico.validar(token));
    }

    @PostMapping("/recuperar-senha/confirmar")
    public ResponseEntity<MensagemApiDTO> confirmarRecuperacaoSenha(
            @Valid @RequestBody ConfirmacaoRecuperacaoSenhaDTO payload
    ) {
        return ResponseEntity.ok(MensagemApiDTO.sucesso(
                recuperacaoSenhaServico.confirmar(payload.token())
        ));
    }

    @PostMapping("/encerrar-sessao")
    public ResponseEntity<?> encerrarSessao(HttpServletRequest request) {
        return authServico.encerrarSessao(request);
    }
}
