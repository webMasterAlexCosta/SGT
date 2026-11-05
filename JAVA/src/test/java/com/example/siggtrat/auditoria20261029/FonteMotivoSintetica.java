package com.example.siggtrat.auditoria20261029;

import com.example.siggtrat.dominio.vinculo.CategoriaTerminoVinculo;
import com.example.siggtrat.entidade.*;
import com.example.siggtrat.servico.GestaoContratoTemporalServico;
import com.example.siggtrat.utilitario.UtilitarioHashSeguranca;
import jakarta.persistence.EntityManager;
import java.time.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

/** Fixture sintética opt-in, somente dentro da transação do cenário.
 * Não é fluxo de incorporação por HTTP, não testa autorização, não toca disponibilidade.
 * Todos os valores que a fonte sintética pretende comprovar são argumentos literais
 * do cenário: nenhum atributo é copiado do mestre como atalho de prova histórica. */
final class FonteMotivoSintetica {
    private FonteMotivoSintetica() {}
    static long declarar(EntityManager em, GestaoContratoTemporalServico gestaoTemporal,
            long contratoId, long motivoId, long gestaoComprovadaId, UsuarioAplicacao autor,
            String descricaoDocumentada, CategoriaTerminoVinculo categoriaDocumentada,
            LocalDate inicio, LocalDate fim, Instant conhecidoEm, String referenciaSintetica) {
        assertNotNull(inicio); assertNotNull(fim); assertFalse(fim.isBefore(inicio));
        assertNotNull(conhecidoEm); assertNotNull(autor); assertNotNull(autor.getId());
        assertNotNull(categoriaDocumentada); assertNotNull(descricaoDocumentada); assertFalse(descricaoDocumentada.isBlank());
        assertNotNull(referenciaSintetica); assertTrue(referenciaSintetica.startsWith("SEI-SINTETICO-"));
        Contrato c=em.find(Contrato.class,contratoId); MotivoDesligamento m=em.find(MotivoDesligamento.class,motivoId);
        assertNotNull(c); assertNotNull(m); assertNotNull(m.getGestaoLocal());
        assertEquals(gestaoComprovadaId,m.getGestaoLocal().getId().longValue());
        // Não alarga motivo da origem até o destino nem mistura duas gestões num único intervalo.
        for(LocalDate data:gestaoTemporal.datasDeMudancaNoIntervalo(c,inicio,fim,conhecidoEm)) {
            GestaoLocal g=gestaoTemporal.gestaoEm(c,data,conhecidoEm);
            assertNotNull(g,"O próprio cenário deve documentar a Gestão na data "+data);
            assertEquals(gestaoComprovadaId,g.getId().longValue());
        }
        assertEquals(0L,em.createQuery("select count(p) from ProvaMotivoDesligamentoHistorico p "
                +"where p.contrato.id=:c and p.motivoDesligamento.id=:m and p.dataInicioEfetiva<=:fim "
                +"and (p.dataFimEfetiva is null or p.dataFimEfetiva>=:inicio)",Long.class)
                .setParameter("c",contratoId).setParameter("m",motivoId).setParameter("inicio",inicio).setParameter("fim",fim).getSingleResult(),
                "Esta fixture cria origem explícita; retificação deve ser declarada como outro cenário, não sobreposta silenciosamente.");
        var p=new ProvaMotivoDesligamentoHistorico();
        p.setContrato(c);p.setMotivoDesligamento(m);p.setDescricaoComprovada(descricaoDocumentada);p.setCategoriaComprovada(categoriaDocumentada);
        p.setDataInicioEfetiva(inicio);p.setDataFimEfetiva(fim);p.setRegistradoEm(conhecidoEm);
        p.setResponsavelId(autor.getId());p.setResponsavelUsername(autor.getUsername());
        p.setReferenciaSei(referenciaSintetica);p.setMotivo("Fixture opt-in: fonte sintética de descrição e categoria no intervalo declarado");
        p.setAutorizacao("ATO-SINTETICO-"+referenciaSintetica);p.setChaveIdempotencia(UUID.randomUUID().toString());
        String material=String.join("|","FIXTURE-CONTEXTO-MOTIVO",Long.toString(contratoId),Long.toString(motivoId),
                Long.toString(gestaoComprovadaId),descricaoDocumentada,categoriaDocumentada.name(),inicio.toString(),fim.toString(),
                conhecidoEm.toString(),referenciaSintetica,autor.getId().toString());
        p.setHashIntencao(UtilitarioHashSeguranca.sha256Hexadecimal(material));p.setHashConteudo(p.getHashIntencao());
        p.setHashDocumento(UtilitarioHashSeguranca.sha256Hexadecimal(referenciaSintetica));
        em.persist(p);em.flush();return p.getId();
    }
}
