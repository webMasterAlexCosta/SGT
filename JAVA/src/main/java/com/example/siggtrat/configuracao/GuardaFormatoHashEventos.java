package com.example.siggtrat.configuracao;

import com.example.siggtrat.entidade.ConfiguracaoSistema;
import com.example.siggtrat.regra.MaterialHashEventos;
import com.example.siggtrat.regra.AssinaturaPreviaNegocial;
import com.example.siggtrat.repositorio.ConfiguracaoSistemaRepositorio;
import jakarta.persistence.EntityManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.SmartInitializingSingleton;
import org.springframework.context.annotation.DependsOn;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

/** Barreira de compatibilidade antes do refresh publicar servidor/lifecycles e runners. */
@Component
@Lazy(false)
@DependsOn("entityManagerFactory")
public final class GuardaFormatoHashEventos implements SmartInitializingSingleton {
    private static final Logger LOG = LoggerFactory.getLogger(GuardaFormatoHashEventos.class);
    private final ConfiguracaoSistemaRepositorio configuracoes;
    private final EntityManager em;
    private final TransactionTemplate transacao;

    public GuardaFormatoHashEventos(ConfiguracaoSistemaRepositorio configuracoes, EntityManager em,
                                   PlatformTransactionManager transactionManager) {
        this.configuracoes = configuracoes;
        this.em = em;
        this.transacao = new TransactionTemplate(transactionManager);
        this.transacao.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
    }

    @Override
    public void afterSingletonsInstantiated() {
        try {
            transacao.executeWithoutResult(status -> verificarOuRegistrar());
            LOG.info("SGT_HASH_EVENTOS_FORMATO_CONFIRMADO formato={} previasIntencoes={}", MaterialHashEventos.FORMATO, AssinaturaPreviaNegocial.FORMATO);
        } catch (FormatoIncompativel incompatibilidade) {
            LOG.error("SGT_HASH_EVENTOS_STARTUP_RECUSADO codigo={}", incompatibilidade.getMessage());
            throw incompatibilidade;
        } catch (RuntimeException falha) {
            LOG.error("SGT_HASH_EVENTOS_STARTUP_FALHOU falha={}",
                    com.example.siggtrat.diagnostico.DiagnosticoFalha.resumir(falha));
            throw new IllegalStateException("SGT_HASH_EVENTOS_INICIALIZACAO_FALHOU");
        }
    }

    private void verificarOuRegistrar() {
        var eventos = configuracoes.buscarPorChaveParaAtualizacao(MaterialHashEventos.CHAVE_MARCADOR);
        var previas = configuracoes.buscarPorChaveParaAtualizacao(AssinaturaPreviaNegocial.CHAVE_MARCADOR);
        if (eventos.isPresent() && !MaterialHashEventos.FORMATO.equals(eventos.get().obterValor()))
            throw new FormatoIncompativel("SGT_HASH_EVENTOS_MARCADOR_DIVERGENTE");
        if (previas.isPresent() && !AssinaturaPreviaNegocial.FORMATO.equals(previas.get().obterValor()))
            throw new FormatoIncompativel("SGT_HASH_PREVIAS_MARCADOR_DIVERGENTE");
        if (eventos.isEmpty() && (possuiAcervo("EventoVinculoContratual") || possuiAcervo("EventoAlocacaoVinculo")))
            throw new FormatoIncompativel("SGT_HASH_EVENTOS_SEM_MARCADOR_COM_ACERVO");
        if (previas.isEmpty() && (possuiAcervo("CorrecaoCnpjContrato") || possuiAcervo("EventoDisponibilidadeCatalogo")
                || possuiAcervo("ProvaNomePessoaHistorico") || possuiAcervo("ProvaMotivoDesligamentoHistorico")))
            throw new FormatoIncompativel("SGT_HASH_PREVIAS_SEM_MARCADOR_COM_ACERVO");
        // Todos os formatos/acervos foram aprovados ANTES de persistir qualquer marcador.
        // Uma transação: nunca deixar a marca de uma família quando a outra foi recusada.
        if (eventos.isEmpty()) em.persist(new ConfiguracaoSistema(MaterialHashEventos.CHAVE_MARCADOR,
                MaterialHashEventos.FORMATO, "Formato técnico dos hashes de eventos; não editável pela API"));
        if (previas.isEmpty()) em.persist(new ConfiguracaoSistema(AssinaturaPreviaNegocial.CHAVE_MARCADOR,
                AssinaturaPreviaNegocial.FORMATO, "Formato técnico de prévias e intenções; somente base nova, sem recálculo histórico"));
        if (eventos.isEmpty() || previas.isEmpty()) em.flush();
    }

    private boolean possuiAcervo(String entidade) {
        return !em.createQuery("select e.id from " + entidade + " e", Long.class).setMaxResults(1).getResultList().isEmpty();
    }

    private static final class FormatoIncompativel extends IllegalStateException {
        FormatoIncompativel(String codigo) { super(codigo); }
    }
}
