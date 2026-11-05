package com.example.siggtrat.auditoria20261029;

import com.example.siggtrat.entidade.Empregado;
import com.example.siggtrat.entidade.FrequenciaAdicional;
import com.example.siggtrat.entidade.FrequenciaExcecao;
import com.example.siggtrat.entidade.Pessoa;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;

/** FRE-001/018 e TMP-001: coincidencia de CPF nao comprova a identidade de uma raiz legada. */
class FrequenciaComplementosIdentidadeLegadaHttpIT extends ContratosHttpBase {

    @ParameterizedTest
    @ValueSource(strings = {"excecoes", "adicionais"})
    void fotografiaComprovadaMantemIdentidadeOriginalMesmoSemAssociacaoAtual(String recurso) throws Exception {
        Cenario c = criarCenario();
        long id = enviar(c.localA, rota(recurso), corpo(c.empregadoA.getId(), recurso), 201,
                "Fonte original publicada com identidade A comprovada") .path("id").asLong();
        JsonNode original = consultar(c.localA, rota(recurso) + "/" + id + "/eventos",
                "Fotografia original ancora o vinculo A");
        prepararIdentidadeLegadaSemProva(c);
        JsonNode origem = dados(executar(c.localA, get(rota(recurso)).param("data", DATA.toString()), 200,
                "Fotografia documentada continua consultavel pela Gestao original"));
        JsonNode alheia = dados(executar(c.localB, get(rota(recurso)).param("data", DATA.toString()), 200,
                "CPF de outra Pessoa nao substitui a identidade ja fotografada"));
        assertEquals(1, origem.path("content").size());
        assertEquals(id, origem.path("content").get(0).path("id").asLong());
        assertEquals(0, alheia.path("content").size());
        assertEquals(original, consultar(c.localA, rota(recurso) + "/" + id + "/eventos",
                "Leitura da prova nao reescreve identidade, contexto nem hash"));
        executar(c.localB, get(rota(recurso) + "/{id}/eventos", id), 404,
                "Gestao B nao herda o acesso a fotografia por coincidencia do CPF");
    }

    @ParameterizedTest
    @ValueSource(strings = {"excecoes", "adicionais"})
    void criarComplementoNaoAdotaVinculoDeOutraPessoaPeloCpfLegado(String recurso) throws Exception {
        Cenario c = criarCenario();
        prepararIdentidadeLegadaSemProva(c);
        String rota = rota(recurso);
        enviar(c.localB, rota, corpo(c.empregadoB.getId(), recurso), 201,
                "Controle: a Pessoa B associada expressamente ao Empregado B possui contexto valido");
        enviar(c.localB, rota, corpo(c.empregadoA.getId(), recurso), 409,
                "Sem associacao canonica ou fotografia, CPF atual de outra Pessoa nao autoriza criar a fonte");
        em.flush(); em.clear();
        assertEquals(0L, quantidade(recurso, c.empregadoA.getId()),
                "Comando recusado nao grava a raiz manual em um vinculo inferido por CPF");
        assertNull(em.find(Empregado.class, c.empregadoA.getId()).getPessoaCanonica(),
                "A tentativa tambem nao regulariza a identidade sem evidencia explicita");
    }

    @ParameterizedTest
    @ValueSource(strings = {"excecoes", "adicionais"})
    void fonteLegadaSemProvaNaoMudaDeGestaoPorCoincidenciaAtualDeCpf(String recurso) throws Exception {
        Cenario c = criarCenario();
        prepararIdentidadeLegadaSemProva(c);
        long id = persistirFonteLegada(c.empregadoA.getId(), recurso);
        em.flush(); em.clear();
        JsonNode lista = dados(executar(c.localB, get(rota(recurso)).param("data", DATA.toString()), 200,
                "Gestao B nao consulta fonte legada de identidade nao comprovada"));
        assertEquals(0, lista.path("content").size(),
                "Um CPF correspondente a outra Pessoa nao transfere a fonte para o vinculo B");
        executar(c.localB, get(rota(recurso) + "/{id}/eventos", id), 404,
                "Consulta de eventos nao atribui escopo ficticio ao legado sem fotografia");
        Map<String, Object> mudanca = recurso.equals("excecoes")
                ? mapa("observacao", "Captura indevida da narrativa") : mapa("minutos", 90);
        mudanca.put("justificativa", "Tentativa nao autorizada de assumir a fonte legada");
        executar(c.localB, put(rota(recurso) + "/{id}", id).contentType(APPLICATION_JSON).content(json(mudanca)),
                403, "PUT exige identidade e escopo comprovados antes de incorporar o legado");
        executar(c.localB, delete(rota(recurso) + "/{id}", id).param("justificativa", "Tentativa de cancelamento alheio"),
                403, "DELETE tambem nao assume a fonte por CPF");
        em.flush(); em.clear();
        assertEquals(1L, quantidade(recurso, c.empregadoA.getId()));
        if (recurso.equals("excecoes")) {
            FrequenciaExcecao fonte = em.find(FrequenciaExcecao.class, id);
            assertEquals("APROVADO", fonte.getStatus());
            assertEquals("Narrativa legada preservada", fonte.getObservacao());
        } else {
            FrequenciaAdicional fonte = em.find(FrequenciaAdicional.class, id);
            assertEquals("CALCULADO", fonte.getStatus());
            assertEquals(60, fonte.getMinutos());
        }
        assertEquals(0L, em.createQuery("select count(v) from VersaoComplementoFrequencia v "
                        + "where v.tipoComplemento=:t and v.complementoId=:id", Long.class)
                .setParameter("t", recurso.equals("excecoes") ? "EXCECAO" : "ADICIONAL")
                .setParameter("id", id).getSingleResult(),
                "Negacoes nao fabricam versao de incorporacao nem autoria historica");
    }

    private void prepararIdentidadeLegadaSemProva(Cenario c) {
        Empregado legado = em.find(Empregado.class, c.empregadoA.getId());
        String cpfAnterior = legado.getCpf();
        String novoCpf;
        do {
            novoCpf = cpf(700000000 + Math.floorMod(UUID.randomUUID().hashCode(), 100000000));
        } while (em.createQuery("select count(p) from Pessoa p where p.cpf=:cpf", Long.class)
                .setParameter("cpf", novoCpf).getSingleResult() != 0);
        // Base legada sem associacao: a igualdade entre os CPFs em tabelas diferentes
        // deixou de representar a mesma raiz. Nenhum evento/foto liga o legado a B.
        em.find(Pessoa.class, c.pessoaA.getId()).setCpf(novoCpf);
        legado.setPessoaCanonica(null);
        em.flush();
        em.find(Pessoa.class, c.pessoaB.getId()).setCpf(cpfAnterior);
        em.flush(); em.clear();
        Empregado recarregado = em.find(Empregado.class, c.empregadoA.getId());
        assertNull(recarregado.getPessoaCanonica());
        assertEquals(cpfAnterior, recarregado.getCpf());
        assertEquals(c.pessoaB.getId(), em.createQuery("select p from Pessoa p where p.cpf=:cpf", Pessoa.class)
                .setParameter("cpf", cpfAnterior).getSingleResult().getId());
        assertNotEquals(c.pessoaA.getId(), c.pessoaB.getId());
        assertEquals(0L, em.createQuery("select count(f) from Frequencia f where f.empregado.id=:id", Long.class)
                .setParameter("id", c.empregadoA.getId()).getSingleResult());
    }

    private long persistirFonteLegada(Long empregadoId, String recurso) {
        Empregado empregado = em.getReference(Empregado.class, empregadoId);
        if (recurso.equals("excecoes")) {
            FrequenciaExcecao fonte = new FrequenciaExcecao();
            fonte.setEmpregado(empregado); fonte.setDataInicio(DATA); fonte.setDataFim(DATA);
            fonte.setTipo("FOLGA_REMUNERADA"); fonte.setStatus("APROVADO");
            fonte.setObservacao("Narrativa legada preservada");
            em.persist(fonte); return fonte.getId();
        }
        FrequenciaAdicional fonte = new FrequenciaAdicional();
        fonte.setEmpregado(empregado); fonte.setDataReferencia(DATA); fonte.setTipo("RT");
        fonte.setStatus("CALCULADO"); fonte.setMinutos(60); fonte.setQuantidade(BigDecimal.ONE);
        fonte.setJustificativa("Narrativa legada preservada");
        em.persist(fonte); return fonte.getId();
    }

    private Long quantidade(String recurso, Long empregadoId) {
        String entidade = recurso.equals("excecoes") ? "FrequenciaExcecao" : "FrequenciaAdicional";
        return em.createQuery("select count(f) from " + entidade + " f where f.empregado.id=:id", Long.class)
                .setParameter("id", empregadoId).getSingleResult();
    }

    private Map<String, Object> corpo(Long empregadoId, String recurso) {
        return recurso.equals("excecoes")
                ? mapa("empregadoId", empregadoId, "dataInicio", DATA, "dataFim", DATA, "tipo", "FOLGA_REMUNERADA")
                : mapa("empregadoId", empregadoId, "dataReferencia", DATA, "tipo", "RT", "minutos", 60, "quantidade", 1);
    }

    private String rota(String recurso) { return "/api/frequencias-" + recurso; }
}
