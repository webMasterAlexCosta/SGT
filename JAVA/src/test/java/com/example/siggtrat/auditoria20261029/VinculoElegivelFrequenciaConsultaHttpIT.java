package com.example.siggtrat.auditoria20261029;

import com.example.siggtrat.entidade.*;
import com.example.siggtrat.utilitario.RelogioSistema;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

/** Proposta de contrato de leitura operacional; rota nova ainda não implementada. */
class VinculoElegivelFrequenciaConsultaHttpIT extends ContratosHttpBase {
    private static final String ROTA = "/api/vinculos-contratuais/elegiveis-frequencia";

    @Test
    void seletorExcluiContratoInativoNoDiaEPreservaHistoricoCadastralEDiaAnterior() throws Exception {
        Cenario c = criarCenario(); LocalDate inativacao = RelogioSistema.hoje().minusDays(1);
        var req = mapa("dataEfetiva", inativacao, "motivo", "Inativação explícita para seletor operacional");
        var previa = enviar(c.localA, base(c.contratoA.getId()) + "/previas/inativar", req, 200, "Prévia concreta");
        req.put("tokenPrevia", previa.path("tokenPrevia").asText()); req.put("confirmarImpacto", true);
        enviar(c.localA, base(c.contratoA.getId()) + "/inativar", req, 200, "Inativação por fluxo real");
        em.flush(); em.clear();
        assertFalse(em.find(Contrato.class, c.contratoA.getId()).getAtivo());
        JsonNode cadastral = consultar(c.localA, "/api/vinculos-contratuais?contratoId=" + c.contratoA.getId(),
                "A história cadastral continua visível");
        assertEquals(List.of(c.vinculoA.getId()), ids(cadastral));
        JsonNode antes = selecionar(c, inativacao.minusDays(1), 0);
        assertEquals(List.of(c.vinculoA.getId()), ids(antes));
        assertEquals(1L, antes.path("totalElements").asLong());
        JsonNode inativo = selecionar(c, inativacao, 0);
        assertEquals(List.of(), ids(inativo));
        assertEquals(0L, inativo.path("totalElements").asLong());
        assertNull(em.find(VinculoContratual.class, c.vinculoA.getId()).getDataFim());
    }

    @Test
    void parSelecionadoFiltraAntesDoCountEPaginacaoSemOcultarOutroParNoCadastro() throws Exception {
        Cenario c = criarCenario();
        Jornada outraJornada = new Jornada();
        outraJornada.setDescricao("Turno alternativo fase36 " + c.gestaoA.getId());
        outraJornada.setGestaoLocal(c.gestaoA);
        for (JornadaDiaSemana original : c.jornadaA.getDias()) {
            JornadaDiaSemana dia = new JornadaDiaSemana(); dia.setJornada(outraJornada);
            dia.setDiaSemana(original.getDiaSemana()); dia.setDiaTrabalho(original.getDiaTrabalho());
            dia.setEntrada1(original.getEntrada1()); dia.setSaida1(original.getSaida1());
            dia.setEntrada2(original.getEntrada2()); dia.setSaida2(original.getSaida2());
            outraJornada.getDias().add(dia);
        }
        em.persist(outraJornada);
        Lotacao outraLotacao = new Lotacao("Lotação alternativa fase36 " + c.gestaoA.getId());
        outraLotacao.setGestaoLocal(c.gestaoA); outraLotacao.setMunicipio(c.lotacaoA.getMunicipio());
        outraLotacao.getTurnos().add(outraJornada); em.persist(outraLotacao);
        ComposicaoContratual outraComposicao = composicao(c.contratoA, outraLotacao, outraJornada, c.cargoA, 5);
        Pessoa outraPessoa = pessoa("Outra alocação autorizada " + c.gestaoA.getId(), cpf(Math.toIntExact(860000000L + c.gestaoA.getId())));
        VinculoContratual outro = vinculo(outraPessoa, c.contratoA);
        projecao.projetar(outro, alocacao(outro, outraComposicao)); em.flush();
        JsonNode cadastral = consultar(c.localA, "/api/vinculos-contratuais?contratoId=" + c.contratoA.getId(),
                "Cadastro preserva os dois pares permitidos à Gestão");
        assertEquals(2, ids(cadastral).size());
        assertTrue(ids(cadastral).containsAll(List.of(c.vinculoA.getId(), outro.getId())));
        JsonNode pagina0 = selecionar(c, RelogioSistema.hoje(), 0);
        assertEquals(List.of(c.vinculoA.getId()), ids(pagina0));
        assertEquals(1L, pagina0.path("totalElements").asLong());
        assertEquals(1, pagina0.path("totalPages").asInt());
        JsonNode pagina1 = selecionar(c, RelogioSistema.hoje(), 1);
        assertEquals(List.of(), ids(pagina1));
        assertEquals(1L, pagina1.path("totalElements").asLong());
        executar(c.localB, get(ROTA).param("dataRef", RelogioSistema.hoje().toString())
                .param("lotacaoId", c.lotacaoA.getId().toString()).param("jornadaId", c.jornadaA.getId().toString()),
                403, "Par explicitamente fora da Gestão não concede seleção operacional");
    }

    @Test
    void fimNaturalPermiteUltimoDiaERecusaDiaSeguinteSemEncerramentoMaterializado() throws Exception {
        Cenario c = criarCenario();
        c.contratoA.setDataFim(DATA);
        em.flush();
        assertEquals(1,em.createQuery("update VersaoContratoNegocial v set v.dataFim=:fim where v.contrato.id=:c")
                .setParameter("fim",DATA).setParameter("c",c.contratoA.getId()).executeUpdate());
        // Alocação permanece coberta em D+1 para isolar o limite do Contrato.
        assertTrue(c.alocacaoA.getDataFim().isAfter(DATA));
        em.flush(); em.clear();
        assertNull(em.find(VinculoContratual.class,c.vinculoA.getId()).getDataFim());
        JsonNode ultimo = selecionar(c, DATA, 0);
        assertEquals(List.of(c.vinculoA.getId()),ids(ultimo));
        JsonNode linha = ultimo.path("content").get(0);
        assertEquals(c.pessoaA.getId().longValue(),linha.path("pessoaId").asLong());
        assertEquals(c.contratoA.getId().longValue(),linha.path("contratoId").asLong());
        assertEquals(c.alocacaoA.getId().longValue(),linha.path("alocacaoId").asLong());
        var permitido = mapa("vinculoContratualId",c.vinculoA.getId(),"dataReferencia",DATA,
                "status","FALTA","justificativaRetificacao","Registro sintético no último dia inclusive",
                "chaveIdempotencia","B05-ULTIMO-DIA-"+c.vinculoA.getId());
        JsonNode frequencia = enviar(c.localA,"/api/frequencias",permitido,200,
                "VIN008 D é operacional mesmo após o calendário alcançar o fim natural");
        assertEquals(c.vinculoA.getId().longValue(),frequencia.path("vinculoContratualId").asLong());
        assertEquals(c.alocacaoA.getId().longValue(),frequencia.path("alocacaoVinculoId").asLong());
        assertEquals(0L,selecionar(c,DATA.plusDays(1),0).path("totalElements").asLong());
        var negado = new java.util.LinkedHashMap<String,Object>(permitido);
        negado.put("dataReferencia",DATA.plusDays(1)); negado.put("chaveIdempotencia","B05-APOS-FIM-"+c.vinculoA.getId());
        JsonNode erro = enviar(c.localA,"/api/frequencias",negado,409,
                "VIN008 D+1 não é operacional, sem confundir com janela, autorização ou data futura");
        assertTrue(erro.path("message").asText().contains("não está operacional"));
        em.flush(); em.clear();
        assertNull(em.find(VinculoContratual.class,c.vinculoA.getId()).getDataFim());
        assertEquals(1L,em.createQuery("select count(f) from Frequencia f where f.vinculoContratual.id=:v",Long.class)
                .setParameter("v",c.vinculoA.getId()).getSingleResult());
        assertEquals(0L,em.createQuery("select count(e) from EventoVinculoContratual e where e.vinculo.id=:v",Long.class)
                .setParameter("v",c.vinculoA.getId()).getSingleResult());
    }

    private JsonNode selecionar(Cenario c, LocalDate data, int pagina) throws Exception {
        return dados(executar(c.localA, get(ROTA).param("dataRef", data.toString())
                .param("contratoId", c.contratoA.getId().toString()).param("lotacaoId", c.lotacaoA.getId().toString())
                .param("jornadaId", c.jornadaA.getId().toString()).param("page", Integer.toString(pagina)).param("size", "1"),
                200, "Seleção operacional temporal com recorte antes de paginar"));
    }
    private List<Long> ids(JsonNode pagina) {
        List<Long> r = new ArrayList<>(); pagina.path("content").forEach(x -> r.add(x.path("id").asLong())); return r;
    }
}
