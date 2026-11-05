package com.example.siggtrat.auditoria20261029;

import com.example.siggtrat.dominio.vinculo.CategoriaTerminoVinculo;
import com.example.siggtrat.entidade.Contrato;
import com.example.siggtrat.entidade.GestaoLocal;
import com.example.siggtrat.entidade.MotivoDesligamento;
import com.example.siggtrat.utilitario.RelogioSistema;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/** CON-020: corrigir a anulação conserva bloqueio operacional até reativação explícita. */
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class ConclusaoCorrecaoAnulacaoOperacaoHttpIT extends ContratosHttpBase {
    @org.springframework.beans.factory.annotation.Autowired
    private com.example.siggtrat.servico.GestaoContratoTemporalServico gestaoTemporal;

    @Test
    void novoVinculoDepoisDeCorrigirAnulacaoExigeReativacaoSemDeixarPessoaOuFaixaParcial() throws Exception {
        var c = criarCenarioPersistido();
        long[] motivoAnulacaoId = new long[1];
        var n = emTransacao(() -> {
            var motivo = new MotivoDesligamento();
            motivo.setGestaoLocal(em.find(GestaoLocal.class, c.gestaoA.getId()));
            motivo.setCategoria(CategoriaTerminoVinculo.ANULACAO_CONTRATO);
            motivo.setDescricao("Anulação documentada do contrato");
            motivo.setCriadoPorId(c.localA.getId());
            motivo.setCriadoPorUsername(c.localA.getUsername());
            em.persist(motivo);
            em.flush();
            motivoAnulacaoId[0] = motivo.getId();
            var origem = new com.example.siggtrat.entidade.EventoDisponibilidadeCatalogo();
            origem.setTipoCatalogo(com.example.siggtrat.dominio.catalogo.TipoCatalogoTemporal.MOTIVO_DESLIGAMENTO);
            origem.setCatalogoId(motivo.getId()); origem.setDataEfetiva(RelogioSistema.hoje().minusDays(30)); origem.setAtivo(true);
            origem.setMotivo("Fonte sintética explícita da causa de anulação desde a origem do contrato");
            em.persist(origem);
            try { return publicado(c, 5, null, false); }
            catch (Exception ex) { throw new IllegalStateException(ex); }
        });
        var alcance = n.inicio().plusDays(2);
        emTransacao(() -> {
            // Premissa positiva opt-in: disponibilidade não prova descrição/categoria.
            // O alcance é apenas o dia usado pelo ato anulatório deste cenário.
            FonteMotivoSintetica.declarar(em, gestaoTemporal, n.id(), motivoAnulacaoId[0],
                    c.gestaoA.getId(), c.localA, "Anulação documentada do contrato",
                    CategoriaTerminoVinculo.ANULACAO_CONTRATO, alcance, alcance,
                    com.example.siggtrat.utilitario.PrecisaoTemporal.agoraMicros(),
                    "SEI-SINTETICO-CON020-CORRECAO-ANULACAO-OPERACAO");
            return null;
        });
        ciclo(c, n.id(), "anular", mapa("dataEfetiva", alcance, "dataAto", RelogioSistema.hoje(),
                "motivo", "Anulação documental cuja incorreção foi identificada", "referenciaSei", "SEI-ANULACAO-INDEVIDA"));
        long anulacao = 0;
        for (var evento : consultar(c.localA, base(n.id()) + "/eventos", "CON020 localizar anulação original"))
            if (evento.path("tipoEvento").asText().equals("ANULACAO")) anulacao = evento.path("id").asLong();
        assertThat(anulacao).isPositive();
        ciclo(c, n.id(), "corrigir-anulacao", mapa("dataEfetiva", alcance, "dataAto", RelogioSistema.hoje(),
                "motivo", "Correção expressa do ato anulatório indevido", "referenciaSei", "SEI-CORRECAO-ANULACAO",
                "eventoPrecedenteId", anulacao, "relacaoPrecedencia", "CANCELA"));
        long anulacaoOriginal = anulacao;
        var relacoes = consultar(c.localA, base(n.id()) + "/eventos/precedencias",
                "CON020 a correção publica uma relação CANCELA apontando a anulação original");
        assertThat(relacoes).hasSize(1);
        assertThat(relacoes.get(0).path("tipoRelacao").asText()).isEqualTo("CANCELA");
        assertThat(relacoes.get(0).path("eventoAlvoId").asLong()).isEqualTo(anulacaoOriginal);
        long eventoCorrecao = relacoes.get(0).path("eventoFonteId").asLong();
        assertThat(eventoCorrecao).isPositive().isNotEqualTo(anulacaoOriginal);
        emTransacao(() -> {
            var relacao = em.createQuery("select r from RelacaoPrecedenciaEventoContrato r "
                            + "where r.contrato.id=:c", com.example.siggtrat.entidade.RelacaoPrecedenciaEventoContrato.class)
                    .setParameter("c", n.id()).getSingleResult();
            assertThat(relacao.getTipoRelacao().name()).isEqualTo("CANCELA");
            assertThat(relacao.getEventoAlvo().getId()).isEqualTo(anulacaoOriginal);
            assertThat(relacao.getEventoFonte().getId()).isEqualTo(eventoCorrecao);
            assertThat(relacao.getEventoFonte().getTipoEvento().name()).isEqualTo("CORRECAO_ANULACAO");
            assertThat(relacao.getRegistradoEm()).isNotNull();
            return null;
        });
        var detalhe = consultar(c.localA, base(n.id()), "CON020 contrato juridicamente publicado continua bloqueado");
        assertThat(detalhe.path("estadoNegocial").asText()).isEqualTo("PUBLICADO");
        assertThat(detalhe.path("habilitacaoAdministrativa").asText()).isEqualTo("INATIVO");
        long eventosAntes = contar(n.id(), "EventoCicloVidaContrato");
        long versoesAntes = contar(n.id(), "VersaoContratoNegocial");
        String cpf = cpf(Math.toIntExact(610000000L + n.id()));
        var cadastro = mapa("contratoId", n.id(), "composicaoId", n.composicao1(), "dataInicio", RelogioSistema.hoje(),
                "pessoa", mapa("nome", "Pessoa da correção de anulação " + n.id(), "cpf", cpf));
        enviar(c.localA, "/api/vinculos-contratuais", cadastro, 409,
                "CON020 correção do ato anulatório não autoriza nova operação antes de reativar");
        emTransacao(() -> {
            assertThat(em.createQuery("select count(p) from Pessoa p where p.cpf=:cpf", Long.class)
                    .setParameter("cpf", cpf).getSingleResult()).isZero();
            assertThat(em.createQuery("select count(v) from VinculoContratual v where v.contrato.id=:id", Long.class)
                    .setParameter("id", n.id()).getSingleResult()).isZero();
            assertThat(em.createQuery("select count(a) from AlocacaoVinculo a where a.vinculo.contrato.id=:id", Long.class)
                    .setParameter("id", n.id()).getSingleResult()).isZero();
            assertThat(em.find(Contrato.class, n.id()).getAtivo()).isFalse();
            return null;
        });
        assertThat(contar(n.id(), "EventoCicloVidaContrato")).isEqualTo(eventosAntes);
        assertThat(contar(n.id(), "VersaoContratoNegocial")).isEqualTo(versoesAntes);

        ciclo(c, n.id(), "reativar", mapa("dataEfetiva", RelogioSistema.hoje(),
                "motivo", "Retomada operacional autorizada após análise da correção", "referenciaSei", "SEI-REATIVACAO-EXPLICITA"));
        var vinculo = enviar(c.localA, "/api/vinculos-contratuais", cadastro, 201,
                "CON020 somente o novo ato administrativo libera a mesma inclusão anteriormente recusada");
        assertThat(vinculo.path("contratoId").asLong()).isEqualTo(n.id());
        assertThat(vinculo.path("situacaoTemporal").asText()).isEqualTo("VIGENTE");
        assertThat(contar(n.id(), "VinculoContratual")).isEqualTo(1L);
        assertThat(contar(n.id(), "EventoCicloVidaContrato")).isEqualTo(eventosAntes + 1);
        var eventos = consultar(c.localA, base(n.id()) + "/eventos", "CON020 anulação e correção continuam auditáveis após a retomada");
        assertThat(eventos.toString()).contains("ANULACAO", "CORRECAO_ANULACAO", "REATIVACAO_ADMINISTRATIVA");
        assertThat(eventos.findValues("id").stream().map(JsonNode::asLong).toList()).contains(anulacao);
    }

    private void ciclo(Cenario c, long id, String operacao, Map<String, Object> req) throws Exception {
        var previa = enviar(c.localA, base(id) + "/previas/" + operacao, req, 200, "CON020 prévia da operação " + operacao);
        req.put("tokenPrevia", previa.path("tokenPrevia").asText());
        req.put("confirmarImpacto", true);
        enviar(c.localA, base(id) + "/" + operacao, req, 200, "CON020 confirmação explícita " + operacao);
    }

    private long contar(long id, String entidade) {
        return emTransacao(() -> em.createQuery("select count(e) from " + entidade + " e where e.contrato.id=:id", Long.class)
                .setParameter("id", id).getSingleResult());
    }
}
