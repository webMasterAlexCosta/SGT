package com.example.siggtrat.configuracao;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
public class ConfiguracaoReact {

    @RequestMapping(value = "/{path:[^\\.]*}")
    public String redirecionar() {
        return "forward:/index.html";
    }
}
