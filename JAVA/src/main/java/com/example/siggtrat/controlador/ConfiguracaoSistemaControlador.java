package com.example.siggtrat.controlador;

import com.example.siggtrat.configuracao.InvalidaCacheNegocio;
import com.example.siggtrat.dto.MensagemApiDTO;
import com.example.siggtrat.dto.ConfiguracaoSistemaDTO;
import com.example.siggtrat.dto.ConfiguracaoPublicaSistemaDTO;
import com.example.siggtrat.servico.ConfiguracaoSistemaServico;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.concurrent.TimeUnit;

@InvalidaCacheNegocio
@RestController
public class ConfiguracaoSistemaControlador implements com.example.siggtrat.diagnostico.DiagnosticoOperacao {
    private static final org.slf4j.Logger LOG_DIAGNOSTICO = org.slf4j.LoggerFactory.getLogger(ConfiguracaoSistemaControlador.class);

    @Override public org.slf4j.Logger loggerDiagnostico() { return LOG_DIAGNOSTICO; }

    private final ConfiguracaoSistemaServico servico;
    public ConfiguracaoSistemaControlador(ConfiguracaoSistemaServico servico) { this.servico = servico; }

    @GetMapping("/config.json")
    public ResponseEntity<ConfiguracaoPublicaSistemaDTO> obterConfiguracaoPublica(HttpServletRequest request) {
        return ResponseEntity.ok()
                .cacheControl(CacheControl.maxAge(0, TimeUnit.SECONDS).mustRevalidate().cachePrivate())
                .body(servico.configuracaoPublica(request.getRemoteAddr()));
    }

    @GetMapping("/api/sistema/configuracoes")
    @PreAuthorize("hasAuthority(T(com.example.siggtrat.constante.ConstantesDominio).PERFIL_ADMINISTRADOR)")
    public ResponseEntity<List<ConfiguracaoSistemaDTO>> listarConfiguracoes() {
        return ResponseEntity.ok(servico.listar());
    }

    @PostMapping("/api/sistema/configuracoes")
    @PreAuthorize("hasAuthority(T(com.example.siggtrat.constante.ConstantesDominio).PERFIL_ADMINISTRADOR)")
    public ResponseEntity<MensagemApiDTO> salvarConfiguracao(@Valid @RequestBody List<ConfiguracaoSistemaDTO> configuracoes) {
        try {
            return ResponseEntity.ok(servico.salvar(configuracoes));
        } catch (org.springframework.web.server.ResponseStatusException e) {
            // Uma revogação durante a espera conserva sua resposta de autorização.
            throw e;
        } catch (com.example.siggtrat.excecao.ExcecaoValidacaoNegocio e) {
            return ResponseEntity.badRequest().body(MensagemApiDTO.erro(
                    com.example.siggtrat.diagnostico.DiagnosticoFalha.mensagemPublica(e.getMessage(), "Valor de configuração inválido.")));
        } catch (IllegalArgumentException | java.time.format.DateTimeParseException e) {
            return ResponseEntity.badRequest().body(MensagemApiDTO.erro("Valor de configuração inválido. Verifique datas, números e formatos informados."));
        }
    }
}
