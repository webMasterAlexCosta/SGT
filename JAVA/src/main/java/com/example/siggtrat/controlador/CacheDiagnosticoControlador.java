package com.example.siggtrat.controlador;

import com.example.siggtrat.dto.CacheDiagnosticoDTO;
import com.example.siggtrat.servico.CacheDiagnosticoServico;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/cache")
@PreAuthorize("hasAuthority(T(com.example.siggtrat.constante.ConstantesDominio).PERFIL_ADMINISTRADOR)")
public class CacheDiagnosticoControlador implements com.example.siggtrat.diagnostico.DiagnosticoOperacao {
    private static final org.slf4j.Logger LOG_DIAGNOSTICO = org.slf4j.LoggerFactory.getLogger(CacheDiagnosticoControlador.class);

    @Override public org.slf4j.Logger loggerDiagnostico() { return LOG_DIAGNOSTICO; }


    private final CacheDiagnosticoServico servico;

    public CacheDiagnosticoControlador(CacheDiagnosticoServico servico) {
        this.servico = servico;
    }

    @GetMapping("/status")
    public CacheDiagnosticoDTO status() {
        return servico.status();
    }
}
