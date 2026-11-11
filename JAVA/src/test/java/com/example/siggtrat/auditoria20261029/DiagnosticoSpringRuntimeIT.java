package com.example.siggtrat.auditoria20261029;

import com.example.siggtrat.diagnostico.DiagnosticoOperacao;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.aop.support.AopUtils;
import java.util.HashSet;
import java.util.LinkedHashSet;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/** Contexto MVC/security/JPA real; inventário de Logger não substitui cobertura funcional. */
class DiagnosticoSpringRuntimeIT extends ContratosHttpBase {
    @Autowired ApplicationContext contexto;
    @Test void todosBeansControllersEServicesAtivosTemLoggerDaPropriaClasseEProxy() {
        var nomes=new LinkedHashSet<String>();
        nomes.addAll(java.util.Arrays.asList(contexto.getBeanNamesForAnnotation(org.springframework.stereotype.Service.class)));
        nomes.addAll(java.util.Arrays.asList(contexto.getBeanNamesForAnnotation(org.springframework.stereotype.Controller.class)));
        var classes=new HashSet<Class<?>>();
        var origem=com.example.siggtrat.AplicacaoBackendSigGtrat.class.getProtectionDomain().getCodeSource().getLocation();
        for(String nome:nomes) {
            Object bean=contexto.getBean(nome);Class<?> tipo=AopUtils.getTargetClass(bean);
            if(!origem.equals(tipo.getProtectionDomain().getCodeSource().getLocation())) continue;
            assertInstanceOf(DiagnosticoOperacao.class,bean,tipo.getName());
            assertTrue(AopUtils.isAopProxy(bean),tipo.getName()+" precisa de interceptação efetiva");
            DiagnosticoOperacao alvo=org.springframework.test.util.AopTestUtils.getUltimateTargetObject(bean);
            assertEquals(tipo.getName(),alvo.loggerDiagnostico().getName());classes.add(tipo);
        }
        assertTrue(classes.size()>=158,"Tipos ativos encontrados="+classes.size()+"; comparar ao inventário de158 tipos");
    }
    @Test void negacaoHttpRealTemMesmoRequestIdNoHeaderECorpo() throws Exception {
        var c=criarCenario();
        mvc.perform(autenticar(get("/api/sistema/configuracoes"),c.localA).header("X-Request-Id","sgt-runtime-error-42"))
            .andExpect(status().isForbidden()).andExpect(header().string("X-Request-Id","sgt-runtime-error-42"))
            .andExpect(jsonPath("$.requestId").value("sgt-runtime-error-42"));
    }
}
