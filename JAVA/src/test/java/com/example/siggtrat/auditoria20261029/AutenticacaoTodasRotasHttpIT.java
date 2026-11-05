package com.example.siggtrat.auditoria20261029;

import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;

/** Additional authentication gate check; never counted as functional endpoint acceptance. */
class AutenticacaoTodasRotasHttpIT extends AuditoriaHttpSuporte {
    @Autowired RequestMappingHandlerMapping rotas;
    @Test void todasAsRotasPrivadasExigemSessaoAntesDeConsultarOuAlterar() throws Exception {
        Set<String> publicas=Set.of("GET /config.json","GET /api/autenticacao/csrf",
                "GET /api/autenticacao/superintendencias","GET /api/autenticacao/gestoes-locais",
                "GET /api/autenticacao/recuperar-senha/validar","POST /api/autenticacao/autenticar",
                "POST /api/autenticacao/cadastrar","POST /api/autenticacao/recuperar-senha/solicitar",
                "POST /api/autenticacao/recuperar-senha/confirmar","POST /api/autenticacao/encerrar-sessao");
        List<String> inventario=InventarioRotasPrivadas.coletarEConferir(rotas,publicas);
        Set<String> verificadas=new TreeSet<>();
        for(String rota:inventario){String[] p=rota.split(" ",2);String path=p[1].replaceAll("\\{[^}]+}","1");
            var req=request(HttpMethod.valueOf(p[0]),path).cookie(new Cookie("XSRF-TOKEN","csrf-anonimo-auditoria"))
                    .header("X-XSRF-TOKEN","csrf-anonimo-auditoria").contentType(MediaType.APPLICATION_JSON).content("{}");
            executar(null,req,401,"Proteção anônima adicional "+rota);
            verificadas.add(rota);
        }
        assertEquals(new TreeSet<>(inventario),verificadas, "Todas as rotas privadas registradas passaram pela recusa anônima 401");
    }
}
