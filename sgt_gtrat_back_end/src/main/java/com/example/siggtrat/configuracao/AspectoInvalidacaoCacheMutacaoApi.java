package com.example.siggtrat.configuracao;

import com.example.siggtrat.servico.InvalidacaoCacheServico;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Aspect
@Component
@Order(Ordered.LOWEST_PRECEDENCE)
public class AspectoInvalidacaoCacheMutacaoApi {

    private final InvalidacaoCacheServico cacheInvalidationServico;

    public AspectoInvalidacaoCacheMutacaoApi(InvalidacaoCacheServico cacheInvalidationServico) {
        this.cacheInvalidationServico = cacheInvalidationServico;
    }

    @Pointcut("@within(com.example.siggtrat.configuracao.InvalidaCacheNegocio) && ("
            + "@annotation(org.springframework.web.bind.annotation.PostMapping) || "
            + "@annotation(org.springframework.web.bind.annotation.PutMapping) || "
            + "@annotation(org.springframework.web.bind.annotation.PatchMapping) || "
            + "@annotation(org.springframework.web.bind.annotation.DeleteMapping))")
    public void operacaoDeEscritaDaApi() {
    }

    @AfterReturning("operacaoDeEscritaDaApi()")
    public void invalidarAposSucesso() {
        cacheInvalidationServico.invalidarCachesDeNegocioAposCommit();
    }
}
