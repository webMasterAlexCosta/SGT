package com.example.siggtrat.auditoria20261029;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.web.servlet.view.xslt.XsltView;
import org.springframework.web.servlet.view.xslt.XsltViewResolver;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Guarda das condições de aplicabilidade de CVE-2026-47884 e CVE-2026-47890.
 * Não declara que Spring6.2.19 recebeu correção: comprova que este contexto
 * não instala XSLT nem publica fragmentos de view por SSE. A adoção dessas
 * capacidades exige rever a versão do framework e esta restrição explícita.
 */
class SuperficieRenderizacaoSeguraHttpIT extends AuditoriaHttpSuporte {
    @Autowired ApplicationContext contexto;

    @Test
    void contextoRealNaoAtivaXsltNemFragmentosDeViewPorSse() {
        assertThat(contexto.getBeansOfType(XsltView.class)).isEmpty();
        assertThat(contexto.getBeansOfType(XsltViewResolver.class)).isEmpty();
        var rotasAplicacao = mappings.getHandlerMethods().entrySet().stream()
                .filter(e -> e.getValue().getBeanType().getPackageName().startsWith("com.example.siggtrat"))
                .toList();
        assertThat(rotasAplicacao).isNotEmpty();
        for (var rota : rotasAplicacao) {
            String retorno = rota.getValue().getMethod().getGenericReturnType().getTypeName();
            assertThat(retorno).as("Retorno da rota %s", rota.getKey())
                    .doesNotContain("SseEmitter", "ServerSentEvent", "FragmentsRendering", "XsltView");
            assertThat(rota.getKey().getProducesCondition().getProducibleMediaTypes())
                    .as("Nenhuma rota declara fluxo de fragmentos SSE: %s", rota.getKey())
                    .noneMatch(m -> m.toString().startsWith("text/event-stream"));
        }
    }
}
