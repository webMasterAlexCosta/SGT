package com.example.siggtrat.auditoria20261029;

import com.example.siggtrat.entidade.*;
import com.example.siggtrat.servico.GestaoContratoTemporalServico;
import com.example.siggtrat.servico.MotorTemporalContratoServico;
import com.example.siggtrat.utilitario.RelogioSistema;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/** Proposta TDD fase36: seleção operacional, sem eliminar cadastro/história. */
class GradeFrequenciaElegibilidadeContratoHttpIT extends ContratosHttpBase {
    @Autowired GestaoContratoTemporalServico gestoes;
    @Autowired MotorTemporalContratoServico contratos;

    @Test
    void empregadoSemPessoaCanonicaNaoEntraNaGradeMesmoComHistoricoEContratoAtivo() throws Exception {
        Cenario c = criarCenario();
        Empregado legado = new Empregado();
        legado.setNome("Legado da fase36 " + c.gestaoA.getId());
        legado.setCpf(cpf(Math.toIntExact(850000000L + c.gestaoA.getId())));
        legado.setGestaoLocal(c.gestaoA); legado.setLotacao(c.lotacaoA); legado.setJornada(c.jornadaA);
        legado.setCargo(c.cargoA); legado.setStatus(c.empregadoA.getStatus());
        legado.setDataAdmissao(RelogioSistema.hoje().minusDays(1));
        legado.setContratos(new ArrayList<>(List.of(c.contratoA))); em.persist(legado); em.flush();
        HistoricoVinculoContrato h = new HistoricoVinculoContrato();
        h.setEmpregado(legado); h.setContrato(c.contratoA); h.setLotacao(c.lotacaoA);
        h.setJornada(c.jornadaA); h.setCargo(c.cargoA);
        h.setDataInicio(legado.getDataAdmissao().atStartOfDay(ZoneId.of("America/Sao_Paulo")));
        em.persist(h); em.flush();
        JsonNode pagina = grade(c, RelogioSistema.hoje());
        assertEquals(List.of(c.empregadoA.getId()), ids(pagina));
        assertEquals(1L, pagina.path("totalElements").asLong());
        assertNull(legado.getPessoaCanonica());
        assertEquals(legado.getId().longValue(), consultar(c.localA, "/api/empregados/" + legado.getId(),
                "Excluir da grade não apaga cadastro autorizado").path("id").asLong());
        JsonNode historia = consultar(c.localA, "/api/empregados/" + legado.getId() + "/historico-contratos",
                "Fonte anterior preservada como história");
        assertEquals(1, historia.size());
        assertEquals(h.getId().longValue(), historia.get(0).path("id").asLong());
    }

    @Test
    void contratoInativoHojeNaoApagaDiaAnteriorValidoNemMantemDiaInativoNaGrade() throws Exception {
        Cenario c = criarCenario();
        LocalDate inativacao = RelogioSistema.hoje().minusDays(1);
        var req = mapa("dataEfetiva", inativacao, "motivo", "Inativação explícita para controle da grade");
        var previa = enviar(c.localA, base(c.contratoA.getId()) + "/previas/inativar", req, 200, "Prévia do ato real");
        req.put("tokenPrevia", previa.path("tokenPrevia").asText()); req.put("confirmarImpacto", true);
        enviar(c.localA, base(c.contratoA.getId()) + "/inativar", req, 200, "Ato real e projeção atual");
        em.flush(); em.clear();
        assertFalse(em.find(Contrato.class, c.contratoA.getId()).getAtivo());
        assertNull(em.find(VinculoContratual.class, c.vinculoA.getId()).getDataFim());
        JsonNode antes = grade(c, inativacao.minusDays(1));
        assertEquals(List.of(c.empregadoA.getId()), ids(antes));
        assertEquals(1L, antes.path("totalElements").asLong());
        JsonNode durante = grade(c, inativacao);
        assertEquals(List.of(), ids(durante));
        assertEquals(0L, durante.path("totalElements").asLong());
    }

    @Test
    void ausenciaDeGestaoHistoricaNaoConcedeElegibilidadePelaGestaoAtual() throws Exception {
        Cenario c = criarCenario(); LocalDate dia = RelogioSistema.hoje();
        assertEquals(1, em.createQuery("update VersaoContratoNegocial set gestaoLocalId=null where contrato.id=:id")
                .setParameter("id", c.contratoA.getId()).executeUpdate());
        em.clear();
        Contrato contrato = em.find(Contrato.class, c.contratoA.getId());
        var posicao = contratos.reconstruirPosicao(contrato, dia, java.time.Instant.now());
        assertEquals(EstadoNegocialContrato.PUBLICADO, posicao.estadoNegocial());
        assertEquals(SituacaoTemporalContrato.VIGENTE, posicao.situacaoTemporal());
        assertEquals(HabilitacaoAdministrativaContrato.ATIVO, posicao.habilitacaoAdministrativa());
        assertNull(gestoes.gestaoEm(contrato, dia, java.time.Instant.now()), "Pré-condição específica: somente Gestão desconhecida");
        JsonNode pagina = grade(c, dia);
        assertEquals(List.of(), ids(pagina));
        assertEquals(0L, pagina.path("totalElements").asLong());
        assertNotNull(contrato.getGestaoLocal(), "Master atual permanece e não é prova do passado");
    }

    private JsonNode grade(Cenario c, LocalDate dia) throws Exception {
        return consultar(c.localA, "/api/empregados/filtrados-frequencia?dataRef=" + dia
                + "&contratoId=" + c.contratoA.getId() + "&lotacaoId=" + c.lotacaoA.getId()
                + "&turnoId=" + c.jornadaA.getId(), "Grade canônica elegível no dia");
    }
    private List<Long> ids(JsonNode pagina) {
        List<Long> r = new ArrayList<>(); pagina.path("content").forEach(x -> r.add(x.path("id").asLong())); return r;
    }
}
