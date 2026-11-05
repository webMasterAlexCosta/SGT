package com.example.siggtrat.auditoria20261029;

import com.example.siggtrat.entidade.*;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;

/** Reexecuta os runners reais no mesmo contexto: não publica ApplicationReadyEvent nem inicia jobs. */
class TemporalidadeInicializacaoHttpIT extends AuditoriaHttpSuporte {
    @Autowired List<CommandLineRunner> inicializadores;

    @Test
    void reexecutarInicializadoresPreservaCustomizacaoEFatosNegociaisJaRegistrados() throws Exception {
        Cenario c = criarCenario();
        c.pessoaA.setNome("Pessoa com nome corrigido antes do reinício");
        c.contratoA.setDescricao("Contrato com descrição negocial preservada");
        JsonNode frequencia = dados(executar(c.localA, post("/api/frequencias").contentType(APPLICATION_JSON).content(json(Map.of(
                "vinculoContratualId", c.vinculoA.getId(), "dataReferencia", DATA.toString(), "status", "FALTA",
                "justificativaRetificacao", "Fato registrado antes da reexecução técnica",
                "chaveIdempotencia", "antes-reinicio-" + UUID.randomUUID()))), 200, "Fato e cadeia de auditoria anteriores à inicialização"));
        SuperintendenciaEstadual sePadrao = em.createQuery("select s from SuperintendenciaEstadual s where s.uf='SP'", SuperintendenciaEstadual.class)
                .getSingleResult();
        sePadrao.setNome("Nome institucional mantido pela administração"); sePadrao.setAtivo(false);
        GestaoLocal gestaoPadrao = em.createQuery("select g from GestaoLocal g where g.superintendencia.id=:s order by g.id", GestaoLocal.class)
                .setParameter("s", sePadrao.getId()).setMaxResults(1).getSingleResult();
        gestaoPadrao.setNome("Gestão deliberadamente desativada antes do reinício"); gestaoPadrao.setAtivo(false);
        em.flush(); em.clear();
        String antes = fotografia(c, frequencia.path("id").asLong(), sePadrao.getId(), gestaoPadrao.getId());
        assertTrue(inicializadores.size() >= 3, "Executar os mesmos runners reais habilitados na aplicação.");
        for (CommandLineRunner inicializador : inicializadores) inicializador.run();
        for (CommandLineRunner inicializador : inicializadores) inicializador.run();
        em.flush(); em.clear();
        assertEquals(antes, fotografia(c, frequencia.path("id").asLong(), sePadrao.getId(), gestaoPadrao.getId()),
                "TMP-006: reexecutar infraestrutura não reativa cadastros nem altera raízes, versões ou eventos negociais.");
        JsonNode eventos = dados(executar(c.localA, get("/api/frequencias/{id}/eventos", frequencia.path("id").asLong()),
                200, "Cadeia posterior à reexecução continua íntegra e com a mesma versão"));
        assertEquals(1, eventos.size());
        assertEquals("Fato registrado antes da reexecução técnica", eventos.get(0).path("motivo").asText());
    }

    private String fotografia(Cenario c, long frequenciaId, long seId, long gestaoId) throws Exception {
        Map<String,Object> foto = new LinkedHashMap<>();
        foto.put("pessoa", em.createQuery("select p.id,p.nome,p.cpf,p.criadoEm from Pessoa p where p.id=:id")
                .setParameter("id", c.pessoaA.getId()).getResultList());
        foto.put("contrato", em.createQuery("select c.id,c.descricao,c.dataInicio,c.dataFim,c.ativo,c.estadoNegocial from Contrato c where c.id=:id")
                .setParameter("id", c.contratoA.getId()).getResultList());
        foto.put("vinculo", em.createQuery("select v.id,v.dataInicio,v.dataFim,v.matricula,v.estadoNegocial,v.version from VinculoContratual v where v.id=:id")
                .setParameter("id", c.vinculoA.getId()).getResultList());
        foto.put("alocacao", em.createQuery("select a.id,a.dataInicio,a.dataFim,a.estadoNegocial,a.jornadaSemanalFotografia,a.version from AlocacaoVinculo a where a.id=:id")
                .setParameter("id", c.alocacaoA.getId()).getResultList());
        foto.put("frequencia", em.createQuery("select f.id,f.status,f.versaoNegocial,f.estadoNegocial,f.estadoAuditoria,f.autorOriginalId from Frequencia f where f.id=:id")
                .setParameter("id", frequenciaId).getResultList());
        foto.put("eventosFrequencia", em.createQuery("select e.id,e.numeroVersao,e.hashEvento,e.registradoEm,e.motivo from EventoFrequencia e where e.frequencia.id=:id order by e.id")
                .setParameter("id", frequenciaId).getResultList());
        foto.put("eventosContrato", em.createQuery("select e.id from EventoCicloVidaContrato e where e.contrato.id=:id order by e.id")
                .setParameter("id", c.contratoA.getId()).getResultList());
        foto.put("versoesContrato", em.createQuery("select v.id from VersaoContratoNegocial v where v.contrato.id=:id order by v.id")
                .setParameter("id", c.contratoA.getId()).getResultList());
        foto.put("eventosVinculo", em.createQuery("select e.id from EventoVinculoContratual e where e.vinculo.id=:id order by e.id")
                .setParameter("id", c.vinculoA.getId()).getResultList());
        foto.put("eventosAlocacao", em.createQuery("select e.id from EventoAlocacaoVinculo e where e.alocacaoNova.id=:id order by e.id")
                .setParameter("id", c.alocacaoA.getId()).getResultList());
        foto.put("sePadrao", em.createQuery("select s.nome,s.ativo from SuperintendenciaEstadual s where s.id=:id")
                .setParameter("id", seId).getResultList());
        foto.put("gestaoPadrao", em.createQuery("select g.nome,g.ativo from GestaoLocal g where g.id=:id")
                .setParameter("id", gestaoId).getResultList());
        return json(foto);
    }
}
