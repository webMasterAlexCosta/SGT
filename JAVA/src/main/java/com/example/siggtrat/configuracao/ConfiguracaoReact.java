package com.example.siggtrat.configuracao;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
public class ConfiguracaoReact implements com.example.siggtrat.diagnostico.DiagnosticoOperacao {
    private static final org.slf4j.Logger LOG_DIAGNOSTICO = org.slf4j.LoggerFactory.getLogger(ConfiguracaoReact.class);

    @Override public org.slf4j.Logger loggerDiagnostico() { return LOG_DIAGNOSTICO; }


    @RequestMapping(value = "/{path:[^\\.]*}")
    public String redirecionar() {
        return "forward:/index.html";
    }
}
