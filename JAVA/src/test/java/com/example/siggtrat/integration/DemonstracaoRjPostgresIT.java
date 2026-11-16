package com.example.siggtrat.integration;

import com.example.siggtrat.entidade.*;
import com.example.siggtrat.repositorio.*;
import com.example.siggtrat.servico.*;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDate;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

@Transactional
class DemonstracaoRjPostgresIT extends PostgresIntegrationTestSupport {
    @Autowired EntityManager em;
    @Autowired PlatformTransactionManager tm;
    @Autowired ConfiguracaoSistemaRepositorio configs;
    @Autowired RegraAdicionalRepositorio regras;
    @Autowired EventoFrequenciaRepositorio eventos;
    @Autowired PainelAnaliticoRepositorio analiticoRepo;
    @Autowired PainelEfetivoRepositorio efetivoRepo;
    @Autowired PainelRepositorio gestaoRepo;
    private Object populador() {
        var cfg=new ConfiguracaoServico(configs);
        var calc=new CalculoFrequenciaServico(cfg,regras,new CalculadoraJornadaNoturnaServico(cfg));
        var audit=new AuditoriaFrequenciaServico(eventos,new MinimizadorDadosPessoaisServico());
        return assertDoesNotThrow(()->Class.forName("com.example.siggtrat.configuracao.demonstracao.PopuladorDadosRj")
                .getConstructor(EntityManager.class,PlatformTransactionManager.class,CalculoFrequenciaServico.class,
                        AuditoriaFrequenciaServico.class,PoliticaTemporalFrequenciaServico.class,org.springframework.security.crypto.password.PasswordEncoder.class)
                .newInstance(em,tm,calc,audit,new PoliticaTemporalFrequenciaServico(),new BCryptPasswordEncoder(4)),
                "Populador da SE/RJ ainda não implementado");
    }
    private Object popular(Object p) throws Exception {
        return p.getClass().getMethod("popular",LocalDate.class,String.class).invoke(p,LocalDate.of(2026,11,16),"Ficticia!Inicial2026");
    }
    private long count(String jpql){return em.createQuery(jpql,Long.class).getSingleResult();}
    @Test void empregadosVinculosEFrequenciasAlimentamIndicadoresSemDuplicar() throws Exception {
        var p=populador();popular(p);em.flush();em.clear();
        assertEquals(54,count("select count(e) from Empregado e where e.gestaoLocal.superintendencia.uf='RJ'"));
        assertEquals(54,count("select count(v) from VinculoContratual v where v.contrato.gestaoLocal.superintendencia.uf='RJ'"));
        assertEquals(54,count("select count(a) from AlocacaoVinculo a where a.vinculo.contrato.gestaoLocal.superintendencia.uf='RJ'"));
        assertEquals(6,count("select count(c) from Contrato c where c.gestaoLocal.superintendencia.uf='RJ'"));
        long total=count("select count(f) from Frequencia f");assertTrue(total>3000);
        assertEquals(0,count("select count(f) from Frequencia f where f.dataReferencia > {d '2026-11-16'}"));
        assertEquals(0,count("select count(f) from Frequencia f where f.pessoa is null or f.vinculoContratual is null or f.alocacaoVinculo is null or f.superintendenciaSnapshotId is null or f.jornadaSnapshotDiaTrabalho is null"));
        for(String status:List.of("PRESENCA","FALTA","ATESTADO","ATRASO","PRESENCA_PARCIAL"))
            assertTrue(em.createQuery("select count(f) from Frequencia f where f.status=:s",Long.class).setParameter("s",status).getSingleResult()>0,status);
        assertEquals(total,count("select count(e) from EventoFrequencia e"));
        var audit=new AuditoriaFrequenciaServico(eventos,new MinimizadorDadosPessoaisServico());
        Long id=em.createQuery("select f.id from Frequencia f order by f.id",Long.class).setMaxResults(1).getSingleResult();
        assertTrue(audit.evidenciasIntegridade(id).cadeiaIntegra());
        long hoje=count("select count(f) from Frequencia f where f.dataReferencia={d '2026-11-16'}");assertTrue(hoje>0);
        long pessoas=count("select count(p) from Pessoa p");long users=count("select count(u) from UsuarioAplicacao u");
        popular(p);em.flush();em.clear();
        assertEquals(total,count("select count(f) from Frequencia f"));assertEquals(pessoas,count("select count(p) from Pessoa p"));assertEquals(users,count("select count(u) from UsuarioAplicacao u"));
        assertNotNull(em.find(ConfiguracaoSistema.class,"DEMO_RJ_DADOS_V1"));
        // Fonte real do painel: a escala e o recorte diário precisam ser calculáveis.
        var contexto=org.mockito.Mockito.mock(PainelServico.class,org.mockito.Mockito.CALLS_REAL_METHODS);
        var se=em.createQuery("select s from SuperintendenciaEstadual s where s.uf='RJ'",SuperintendenciaEstadual.class).getSingleResult();
        org.mockito.Mockito.doReturn(new PainelServico.PainelCalculoContexto("Gestão Estadual","SE/RJ",se.getId(),null,true,true,true,true,true)).when(contexto).contextoAtual();
        var visoes=new PainelVisoesServico(new PainelAnaliticoServico(analiticoRepo,efetivoRepo,contexto),contexto,gestaoRepo);
        for(String periodo:List.of("DIARIO","SEMANAL","MENSAL","TRIMESTRAL","ANUAL")) {
            var dto=visoes.obter(periodo,LocalDate.of(2026,11,16),null,null,null,true,0,null);
            assertTrue(dto.analitico().resumoPeriodo().registros()>0,periodo);
            assertTrue(dto.analitico().resumoPeriodo().ausencias()>0,periodo);
            assertEquals(dto.analitico().resumoPeriodo().registros(),dto.gestao().periodo().registrosOperacionais());
        }
    }
    @Test void recusaMisturarMassaComPessoasPreexistentes() {
        var pessoa=new Pessoa();pessoa.setNome("Pessoa anterior preservada");pessoa.setCpf("90345678901");em.persist(pessoa);em.flush();
        var p=populador();assertThrows(java.lang.reflect.InvocationTargetException.class,()->popular(p));
        assertEquals(1,count("select count(p) from Pessoa p"));assertEquals(0,count("select count(c) from Contrato c"));
    }
}
