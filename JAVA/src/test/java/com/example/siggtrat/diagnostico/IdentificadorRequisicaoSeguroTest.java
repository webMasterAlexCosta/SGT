package com.example.siggtrat.diagnostico;
import com.example.siggtrat.configuracao.FiltroIdentificadorRequisicao;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import static org.junit.jupiter.api.Assertions.*;
class IdentificadorRequisicaoSeguroTest {
    @AfterEach void limpar(){MDC.clear();}
    @Test void preservaIdentificadorValidoEContextoAnterior() throws Exception {
        MDC.put("contextoAnterior","preservar");var req=new MockHttpServletRequest();req.addHeader("X-Request-Id","req-valid:42");var res=new MockHttpServletResponse();
        new FiltroIdentificadorRequisicao().doFilter(req,res,(a,b)->assertEquals("req-valid:42",MDC.get("requestId")));
        assertEquals("req-valid:42",res.getHeader("X-Request-Id"));assertEquals("preservar",MDC.get("contextoAnterior"));assertNull(MDC.get("requestId"));
    }
    @Test void naoAceitaInjecaoControleNemIdentificadorIlimitado() throws Exception {
        for(String valor:new String[]{"fake\r\nADMIN=true","x".repeat(1000)}) {
            var req=new MockHttpServletRequest();req.addHeader("X-Request-Id",valor);req.addHeader("X-Client","fake\nFORGED");var res=new MockHttpServletResponse();
            new FiltroIdentificadorRequisicao().doFilter(req,res,(a,b)->assertFalse(String.valueOf(MDC.get("clientOrigin")).contains("\n")));
            assertTrue(res.getHeader("X-Request-Id").matches("[A-Za-z0-9][A-Za-z0-9._:-]{0,63}"));assertNotEquals(valor,res.getHeader("X-Request-Id"));
        }
    }
}
