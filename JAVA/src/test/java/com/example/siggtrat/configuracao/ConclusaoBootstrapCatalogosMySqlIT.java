package com.example.siggtrat.configuracao;

import com.example.siggtrat.auditoria20261029.AuditoriaHttpSuporte;
import com.example.siggtrat.dominio.catalogo.TipoCatalogoTemporal;
import com.example.siggtrat.entidade.EventoDisponibilidadeCatalogo;
import com.example.siggtrat.entidade.GestaoLocal;
import com.example.siggtrat.entidade.Jornada;
import com.example.siggtrat.entidade.JornadaDiaSemana;
import com.example.siggtrat.entidade.Lotacao;
import com.example.siggtrat.utilitario.RelogioSistema;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.ApplicationContext;
import org.springframework.test.util.ReflectionTestUtils;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.time.DayOfWeek;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/** Bootstrap real no mesmo PostgreSQL do contexto; a transação de cada caso é revertida. */
class ConclusaoBootstrapCatalogosMySqlIT extends AuditoriaHttpSuporte {
    @Autowired private ApplicationContext contexto;

    @Test
    void bootstrapDeNovosCatalogosRegistraOrigemDeHojeSemInferirPassado() throws Exception {
        Long gestaoId = gestaoRj().getId();

        executarBootstrap("initJornadasOperacionais");
        executarBootstrap("initUnidadesOperacionais");
        em.flush(); em.clear();

        Jornada jornada = jornada("TURNO 1A", gestaoId);
        Lotacao lotacao = lotacao("CTE BENFICA", gestaoId);
        assertOrigemProspectiva(TipoCatalogoTemporal.JORNADA, jornada.getId());
        assertOrigemProspectiva(TipoCatalogoTemporal.LOTACAO, lotacao.getId());

        executarBootstrap("initJornadasOperacionais");
        executarBootstrap("initUnidadesOperacionais");
        em.flush(); em.clear();
        assertEquals(1, eventos(TipoCatalogoTemporal.JORNADA, jornada.getId()).size(),
                "Reinicialização não pode duplicar a origem da Jornada.");
        assertEquals(1, eventos(TipoCatalogoTemporal.LOTACAO, lotacao.getId()).size(),
                "Reinicialização não pode duplicar a origem da Lotação.");
    }

    @Test
    void reexecutarBootstrapNaoReescreveJornadaExistenteNemFabricaOrigem() throws Exception {
        GestaoLocal gestao = gestaoRj();
        Jornada existente = new Jornada();
        existente.setDescricao("TURNO 1A"); existente.setGestaoLocal(gestao); existente.setAtivo(false);
        JornadaDiaSemana dia = new JornadaDiaSemana();
        dia.setJornada(existente); dia.setDiaSemana(DayOfWeek.MONDAY); dia.setDiaTrabalho(true);
        dia.setEntrada1("09:13"); dia.setSaida1("11:17");
        existente.getDias().add(dia);
        em.persist(existente); em.flush();
        Long id = existente.getId();

        executarBootstrap("initJornadasOperacionais");
        executarBootstrap("initJornadasOperacionais");
        em.flush(); em.clear();

        Jornada preservada = em.find(Jornada.class, id);
        assertFalse(preservada.getAtivo());
        assertEquals(1, preservada.getDias().size(), "Bootstrap não é uma revisão da Jornada vigente.");
        assertEquals("09:13", preservada.getDias().get(0).getEntrada1());
        assertEquals("11:17", preservada.getDias().get(0).getSaida1());
        assertTrue(eventos(TipoCatalogoTemporal.JORNADA, id).isEmpty(),
                "O estado de item preexistente não comprova uma origem temporal.");
    }

    @Test
    void reexecutarBootstrapNaoReescreveLotacaoExistenteNemFabricaOrigem() throws Exception {
        GestaoLocal gestao = gestaoRj();
        Jornada propria = new Jornada(); propria.setDescricao("JORNADA PRÓPRIA"); propria.setGestaoLocal(gestao);
        em.persist(propria);
        Lotacao existente = new Lotacao("CTE BENFICA");
        existente.setGestaoLocal(gestao); existente.setMunicipio("MUNICÍPIO PRESERVADO"); existente.setAtivo(false);
        existente.setTurnos(new ArrayList<>(List.of(propria)));
        em.persist(existente); em.flush();
        Long id = existente.getId(); Long jornadaId = propria.getId();

        executarBootstrap("initUnidadesOperacionais");
        executarBootstrap("initUnidadesOperacionais");
        em.flush(); em.clear();

        Lotacao preservada = em.find(Lotacao.class, id);
        assertFalse(preservada.getAtivo());
        assertEquals("MUNICÍPIO PRESERVADO", preservada.getMunicipio());
        assertEquals(List.of(jornadaId), preservada.getTurnos().stream().map(Jornada::getId).toList(),
                "Bootstrap não pode acrescentar turnos a uma Lotação já existente.");
        assertTrue(eventos(TipoCatalogoTemporal.LOTACAO, id).isEmpty(),
                "O estado de item preexistente não comprova uma origem temporal.");
    }

    private void executarBootstrap(String nome) throws Exception {
        // Resolve os mesmos argumentos por tipo que o Spring injeta no factory @Bean.
        // A invocação explícita permite testar também a reexecução com a flag uma-vez=false.
        InicializadorDados inicializador = new InicializadorDados();
        ReflectionTestUtils.setField(inicializador, "executarSeedsApenasUmaVez", false);
        Method fabrica = Arrays.stream(InicializadorDados.class.getDeclaredMethods())
                .filter(m -> m.getName().equals(nome) && m.getReturnType() == CommandLineRunner.class)
                .findFirst().orElseThrow();
        fabrica.setAccessible(true);
        Object[] argumentos = Arrays.stream(fabrica.getParameterTypes()).map(contexto::getBean).toArray();
        try {
            ((CommandLineRunner) fabrica.invoke(inicializador, argumentos)).run();
        } catch (InvocationTargetException e) {
            if (e.getCause() instanceof Exception causa) throw causa;
            throw e;
        }
    }

    private GestaoLocal gestaoRj() {
        return em.createQuery("select g from GestaoLocal g where g.codigo = 'GTRAT-RJ'", GestaoLocal.class)
                .getSingleResult();
    }

    private Jornada jornada(String descricao, Long gestaoId) {
        return em.createQuery("select j from Jornada j where j.descricao = :descricao and j.gestaoLocal.id = :gestao", Jornada.class)
                .setParameter("descricao", descricao).setParameter("gestao", gestaoId).getSingleResult();
    }

    private Lotacao lotacao(String nome, Long gestaoId) {
        return em.createQuery("select l from Lotacao l where l.nome = :nome and l.gestaoLocal.id = :gestao", Lotacao.class)
                .setParameter("nome", nome).setParameter("gestao", gestaoId).getSingleResult();
    }

    private List<EventoDisponibilidadeCatalogo> eventos(TipoCatalogoTemporal tipo, Long id) {
        return em.createQuery("select e from EventoDisponibilidadeCatalogo e where e.tipoCatalogo = :tipo and e.catalogoId = :id order by e.id", EventoDisponibilidadeCatalogo.class)
                .setParameter("tipo", tipo).setParameter("id", id).getResultList();
    }

    private void assertOrigemProspectiva(TipoCatalogoTemporal tipo, Long id) {
        List<EventoDisponibilidadeCatalogo> trilha = eventos(tipo, id);
        assertEquals(1, trilha.size(), "Catálogo criado pelo bootstrap precisa ter origem prospectiva comprovada.");
        EventoDisponibilidadeCatalogo origem = trilha.get(0);
        assertEquals(RelogioSistema.hoje(), origem.getDataEfetiva());
        assertTrue(origem.getAtivo());
        assertNull(origem.getAtivoAnterior());
        assertNotNull(origem.getRegistradoEm());
        assertEquals("SISTEMA_INICIALIZACAO", origem.getResponsavelUsername());
        assertTrue(trilha.stream().noneMatch(e -> e.getDataEfetiva().isBefore(RelogioSistema.hoje())),
                "O bootstrap não pode criar disponibilidade retroativa sem fonte documental.");
    }
}
