package com.example.siggtrat.diagnostico;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.type.filter.AnnotationTypeFilter;
import java.lang.reflect.Modifier;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
class CoberturaLoggerTest {
    @Test void todosControllersEServicesPossuemLoggerProprioEContratoDeUso() throws Exception {
        var scanner=new ClassPathScanningCandidateComponentProvider(false);
        scanner.addIncludeFilter(new AnnotationTypeFilter(org.springframework.stereotype.Service.class));
        scanner.addIncludeFilter(new AnnotationTypeFilter(org.springframework.stereotype.Controller.class));
        var ausentes=new ArrayList<String>(); int total=0;
        for(var bean:scanner.findCandidateComponents("com.example.siggtrat")) {
            Class<?> c=Class.forName(bean.getBeanClassName(),false,getClass().getClassLoader());
            // Fixtures @Controller do classpath de testes não integram o produto.
            var origemProduto=com.example.siggtrat.AplicacaoBackendSigGtrat.class.getProtectionDomain().getCodeSource().getLocation();
            if(!origemProduto.equals(c.getProtectionDomain().getCodeSource().getLocation())) continue;
            total++;
            boolean logger=Arrays.stream(c.getDeclaredFields()).anyMatch(f->org.slf4j.Logger.class.isAssignableFrom(f.getType())&&Modifier.isStatic(f.getModifiers()));
            if(!logger || !DiagnosticoOperacao.class.isAssignableFrom(c))ausentes.add(c.getSimpleName());
        }
        assertTrue(total>=158,"Não perder classes do inventário inicial");
        assertTrue(ausentes.isEmpty(),()->"Falta Logger útil no contrato: "+ausentes);
    }
}
