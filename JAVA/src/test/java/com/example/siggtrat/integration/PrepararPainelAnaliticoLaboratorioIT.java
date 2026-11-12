package com.example.siggtrat.integration;

import com.example.siggtrat.dominio.acesso.PerfilAcesso;
import com.example.siggtrat.entidade.*;
import java.time.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Assumptions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import java.nio.file.*;
import java.nio.file.attribute.PosixFilePermissions;
import java.security.SecureRandom;
import java.time.LocalDate;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

/** Opt-in humano para a base nova deste chat. Não executa no profile integration comum. */
class PrepararPainelAnaliticoLaboratorioIT extends PostgresIntegrationTestSupport {
    @Autowired EntityManager em;
    @Autowired PlatformTransactionManager tm;
    @Autowired JdbcTemplate jdbc;
    @Test void prepararFixturePersistenteDoLaboratorioAutorizado() throws Exception {
        Assumptions.assumeTrue("true".equals(System.getenv("SGT_DASHBOARD_PREPARAR_LAB")));
        assertEquals("sgt_dashboard_ui_lab",jdbc.queryForObject("select current_database()",String.class));
        assertEquals("sgt_dashboard_app",jdbc.queryForObject("select current_user",String.class));
        Path destino=Path.of(java.util.Objects.requireNonNull(System.getenv("SGT_DASHBOARD_LAB_ACCESS"), "Defina o destino privado do laboratório"));
        assertFalse(Files.exists(destino),"Fixture já preparada; não sobrescrever acesso nem duplicar fatos.");
        var acesso=new TransactionTemplate(tm).execute(tx->{
            assertEquals(0L,em.createQuery("select count(u) from UsuarioAplicacao u",Long.class).getSingleResult());
            var origem=PainelAnaliticoFixture.criar(em,"Dashboard laboratório origem");
            var externo=PainelAnaliticoFixture.criar(em,"Dashboard laboratório externo");
            var segundo=PainelAnaliticoFixture.criar(em,"CDD Centro sintético",true,origem.gestao());
            var terceiro=PainelAnaliticoFixture.criar(em,"CDD Norte sintético",true,origem.gestao());
            int numero=0;
            for(var unidade:List.of(origem,segundo,terceiro)) {
                var fornecedor=new Fornecedor();fornecedor.setRazaoSocial("Prestador sintético "+(++numero));fornecedor.setCnpj("9900000000000"+numero);em.persist(fornecedor);
                unidade.contrato().setFornecedor(fornecedor);unidade.contrato().setRazaoSocialEmpresa(fornecedor.getRazaoSocial());unidade.contrato().setNomeFantasiaEmpresa(fornecedor.getRazaoSocial());
                var pessoas=new ArrayList<PainelAnaliticoFixture.Dados>();pessoas.add(unidade);
                for(int i=1;i<=8;i++) pessoas.add(PainelAnaliticoFixture.adicionarTrabalhador(em,unidade,"Colaborador sintético "+numero+"-"+i));
                for(int i=0;i<pessoas.size();i++) {
                    var pessoa=pessoas.get(i);
                    for(int dia=1;dia<=10;dia++) {
                        LocalDate data=LocalDate.of(2026,11,dia);
                        if(data.getDayOfWeek().getValue()>5 || (i+dia+numero)%7==0) continue;
                        var primeiro=data.plusDays(dia<3 && i%4==0?8:1).atTime(14,0).atZone(ZoneId.of("America/Sao_Paulo")).toInstant();
                        var f=PainelAnaliticoFixture.frequenciaComPrazo(em,pessoa,data,(i+dia)%5==0?"FALTA":(i+dia)%9==0?"ATESTADO":"PRESENCA",primeiro);
                        if(dia==10 && i==0) {f.setRevisaoSolicitadaStatus("PENDENTE");f.setRevisaoSolicitadaEm(Instant.now());f.setRevisaoSolicitadaMotivo("Conferência sintética para homologação visual");}
                    }
                    PainelAnaliticoFixture.frequencia(em,pessoa,LocalDate.of(2026,10,9),"PRESENCA");
                }
            }
            PainelAnaliticoFixture.frequencia(em,externo,LocalDate.of(2026,11,10),"FALTA");
            List<Map<String,String>> atores=new ArrayList<>();
            String[] nomes={"dashboard_lab","dashboard_externo","dashboard_presidente"};
            String[] matriculas={"99000601","99000602","99000603"};
            for(int i=0;i<3;i++) {
                String senha=senha(),novaSenha=senha();
                var u=new UsuarioAplicacao();u.setUsername(nomes[i]);u.setMatricula(matriculas[i]);u.setEmail(nomes[i]+"@example.test");
                u.setPassword(new BCryptPasswordEncoder().encode(senha));u.setActive(true);u.setForceChangePassword(true);
                u.setPerfil(i==2?PerfilAcesso.PRESIDENTE:PerfilAcesso.GESTOR_LOCAL);
                if(i!=2)u.setGestaoLocal(i==0?origem.gestao():externo.gestao());
                em.persist(u);
                atores.add(Map.of("username",nomes[i],"matricula",matriculas[i],"senhaAtual",senha,"senhaDefinitiva",novaSenha));
            }
            em.flush();
            return Map.of("natureza","FIXTURE SINTETICA DE LABORATORIO; SEM DADOS REAIS", "atores",atores,
                    "referencia","2026-11-10","contratoOrigem",origem.contrato().getId(),"lotacaoOrigem",origem.lotacao().getId(),
                    "contratoExterno",externo.contrato().getId(),"lotacaoExterna",externo.lotacao().getId());
        });
        Files.createFile(destino,PosixFilePermissions.asFileAttribute(PosixFilePermissions.fromString("rw-------")));
        new ObjectMapper().writerWithDefaultPrettyPrinter().writeValue(destino.toFile(),acesso);
        assertEquals(3L,jdbc.queryForObject("select count(*) from usuario_aplicacao",Long.class));
    }
    @Test void prepararOperacionalAdicionalSemRecriarFixture() throws Exception {
        Assumptions.assumeTrue("true".equals(System.getenv("SGT_DASHBOARD_PREPARAR_LAB")));
        assertEquals("sgt_dashboard_ui_lab",jdbc.queryForObject("select current_database()",String.class));
        assertEquals("sgt_dashboard_app",jdbc.queryForObject("select current_user",String.class));
        Path destino=Path.of(Objects.requireNonNull(System.getenv("SGT_DASHBOARD_LAB_ACCESS")));
        var mapper=new ObjectMapper();var acesso=mapper.readTree(destino.toFile());
        assertEquals("FIXTURE SINTETICA DE LABORATORIO; SEM DADOS REAIS",acesso.path("natureza").asText());
        var credencial=new TransactionTemplate(tm).execute(tx->{
            assertEquals(0L,em.createQuery("select count(u) from UsuarioAplicacao u where u.username='dashboard_operacional'",Long.class).getSingleResult());
            var gestor=em.createQuery("select u from UsuarioAplicacao u where u.username='dashboard_lab'",UsuarioAplicacao.class).getSingleResult();
            var lotacao=em.find(Lotacao.class,acesso.path("lotacaoOrigem").asLong());
            var jornada=em.createQuery("select a.jornada from AlocacaoVinculo a where a.lotacao.id=:id order by a.id",Jornada.class).setParameter("id",lotacao.getId()).setMaxResults(1).getSingleResult();
            // O vínculo Lotação/Turno é parte do cadastro autorizado do Operacional.
            if(!lotacao.getTurnos().contains(jornada)) lotacao.getTurnos().add(jornada);
            String senha=senha();var u=new UsuarioAplicacao();u.setUsername("dashboard_operacional");u.setMatricula("99000604");u.setEmail("dashboard_operacional@example.test");u.setPassword(new BCryptPasswordEncoder().encode(senha));u.setActive(true);u.setForceChangePassword(true);u.setPerfil(PerfilAcesso.OPERACIONAL);u.setGestaoLocal(gestor.getGestaoLocal());em.persist(u);
            var escopo=new VinculoGestaoUsuarioAplicacao();escopo.setUser(u);escopo.setLotacao(lotacao);escopo.setJornada(jornada);em.persist(escopo);em.flush();
            return Map.of("username",u.getUsername(),"matricula",u.getMatricula(),"senhaAtual",senha,"senhaDefinitiva",senha());
        });
        ((com.fasterxml.jackson.databind.node.ArrayNode)acesso.path("atores")).add(mapper.valueToTree(credencial));
        mapper.writerWithDefaultPrettyPrinter().writeValue(destino.toFile(),acesso);
    }

    private String senha() {
        byte[] bytes=new byte[24];new SecureRandom().nextBytes(bytes);
        return "Aa1!"+Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}
