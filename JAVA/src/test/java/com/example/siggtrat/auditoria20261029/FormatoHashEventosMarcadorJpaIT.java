package com.example.siggtrat.auditoria20261029;

import com.example.siggtrat.entidade.ConfiguracaoSistema;
import com.example.siggtrat.configuracao.GuardaFormatoHashEventos;
import com.example.siggtrat.regra.MaterialHashEventos;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import static org.junit.jupiter.api.Assertions.*;

/** Exige base de integração compatível/vazia no primeiro boot; sem fallback nem limpeza automática. */
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class FormatoHashEventosMarcadorJpaIT extends AuditoriaHttpSuporte {
    @Autowired GuardaFormatoHashEventos guarda;

    @Test
    void marcadorDoStartupEstaConfirmadoEmNovaTransacaoENaoEReescritoNaRevalidacao() {
        String descricao = emTransacao(() -> {
            em.clear();
            ConfiguracaoSistema marcador = em.find(ConfiguracaoSistema.class, MaterialHashEventos.CHAVE_MARCADOR);
            assertNotNull(marcador);
            assertEquals(MaterialHashEventos.FORMATO, marcador.obterValor());
            return marcador.getDescricao();
        });
        guarda.afterSingletonsInstantiated();
        emTransacao(() -> {
            em.clear();
            ConfiguracaoSistema marcador = em.find(ConfiguracaoSistema.class, MaterialHashEventos.CHAVE_MARCADOR);
            assertEquals(MaterialHashEventos.FORMATO, marcador.obterValor());
            assertEquals(descricao, marcador.getDescricao());
            assertEquals(1L, em.createQuery("select count(c) from ConfiguracaoSistema c where c.chave=:chave", Long.class)
                    .setParameter("chave", MaterialHashEventos.CHAVE_MARCADOR).getSingleResult().longValue());
            return null;
        });
    }
}
