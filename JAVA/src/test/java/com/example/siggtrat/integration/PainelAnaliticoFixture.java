package com.example.siggtrat.integration;

import com.example.siggtrat.entidade.*;
import jakarta.persistence.EntityManager;
import java.time.*;
import java.util.*;

/** Dados sintéticos persistidos por JPA, compartilhados pelos testes e pelo laboratório opt-in. */
final class PainelAnaliticoFixture {
    record Dados(GestaoLocal gestao,Lotacao lotacao,Jornada jornada,Cargo cargo,Contrato contrato,
                 Pessoa pessoa,VinculoContratual vinculo,AlocacaoVinculo alocacao,Empregado empregado) {}
    static Dados criar(EntityManager em,String nome) { return criar(em,nome,true); }
    static Dados criar(EntityManager em,String nome,boolean comGrade) { return criar(em,nome,comGrade,null); }
    static Dados criar(EntityManager em,String nome,boolean comGrade,GestaoLocal gestaoExistente) {
        var se=em.createQuery("select s from SuperintendenciaEstadual s where s.uf='YX'",SuperintendenciaEstadual.class).getResultStream().findFirst().orElseGet(()->{
            var nova=new SuperintendenciaEstadual("SE sintética dashboard","YX");em.persist(nova);return nova;
        });
        var g=gestaoExistente;
        if(g==null) {g=new GestaoLocal();g.setNome(nome);g.setCodigo(UUID.randomUUID().toString().substring(0,8));g.setSuperintendencia(se);em.persist(g);}
        var l=new Lotacao("Lotação "+nome);l.setGestaoLocal(g);em.persist(l);
        var j=new Jornada();j.setDescricao("Jornada "+nome);j.setGestaoLocal(g);em.persist(j);
        if(comGrade) for(DayOfWeek dia:DayOfWeek.values()) {
            var grade=new JornadaDiaSemana();grade.setJornada(j);grade.setDiaSemana(dia);boolean trabalho=dia.getValue()<=5;
            grade.setDiaTrabalho(trabalho);if(trabalho) {grade.setEntrada1("08:00");grade.setSaida1("17:00");}
            j.getDias().add(grade);em.persist(grade);
        }
        var cargo=new Cargo("Cargo "+nome,"1");cargo.setGestaoLocal(g);em.persist(cargo);
        var c=new Contrato();c.setNumeroContrato("DASH-LAB-"+UUID.randomUUID());c.setAnoContrato(2025);c.setGestaoLocal(g);
        c.setDataInicio(LocalDate.of(2025,1,1));c.setDataFim(LocalDate.of(2027,12,31));c.setEfetivoPrevisto(10);
        c.setPublicadoEm(Instant.parse("2024-12-31T12:00:00Z"));c.setEstadoNegocial(EstadoNegocialContrato.PUBLICADO);em.persist(c);
        var versao=new VersaoContratoNegocial();versao.setContrato(c);versao.setNumeroVersao(1);versao.setDataEfetiva(c.getDataInicio());
        versao.setDataInicio(c.getDataInicio());versao.setDataFim(c.getDataFim());versao.setNumeroContrato(c.getNumeroContrato());versao.setAnoContrato(c.getAnoContrato());
        versao.setGestaoLocalId(g.getId());versao.setGestaoLocalNome(g.getNome());versao.setEfetivoPrevisto(10);versao.setHabilitacaoAdministrativa(HabilitacaoAdministrativaContrato.ATIVO);em.persist(versao);
        var comp=new ComposicaoContratual();comp.setContrato(c);comp.setLotacao(l);comp.setJornada(j);comp.setCargo(cargo);comp.setEfetivoPrevisto(10);
        comp.setDataInicioVigencia(c.getDataInicio());comp.setDataFimVigencia(c.getDataFim());em.persist(comp);
        var status=em.createQuery("select s from StatusEmpregado s where s.descricao='ATIVO'",StatusEmpregado.class).getResultStream().findFirst().orElseGet(()->{
            var novo=new StatusEmpregado("ATIVO");em.persist(novo);return novo;
        });
        String cpf=String.format("%011d",Integer.toUnsignedLong(UUID.randomUUID().hashCode()));
        var pessoa=new Pessoa();pessoa.setNome("Pessoa sintética "+nome);pessoa.setCpf(cpf);em.persist(pessoa);
        var v=new VinculoContratual();v.setPessoa(pessoa);v.setContrato(c);v.setDataInicio(c.getDataInicio());em.persist(v);
        var a=new AlocacaoVinculo();a.setVinculo(v);a.setComposicao(comp);a.setLotacao(l);a.setJornada(j);a.setCargo(cargo);
        a.setDataInicio(c.getDataInicio());a.setDataFim(c.getDataFim());em.persist(a);
        var e=new Empregado();e.setNome(pessoa.getNome());e.setCpf(cpf);e.setPessoaCanonica(pessoa);e.setGestaoLocal(g);e.setLotacao(l);e.setJornada(j);e.setCargo(cargo);
        e.setStatus(status);e.setContratos(new ArrayList<>(List.of(c)));em.persist(e);
        return new Dados(g,l,j,cargo,c,pessoa,v,a,e);
    }
    static Dados adicionarTrabalhador(EntityManager em,Dados origem,String nome) {
        String cpf=String.format("%011d",Integer.toUnsignedLong(UUID.randomUUID().hashCode()));
        var pessoa=new Pessoa();pessoa.setNome(nome);pessoa.setCpf(cpf);em.persist(pessoa);
        var v=new VinculoContratual();v.setPessoa(pessoa);v.setContrato(origem.contrato());v.setDataInicio(origem.contrato().getDataInicio());em.persist(v);
        var a=new AlocacaoVinculo();a.setVinculo(v);a.setComposicao(origem.alocacao().getComposicao());a.setLotacao(origem.lotacao());a.setJornada(origem.jornada());a.setCargo(origem.cargo());
        a.setDataInicio(v.getDataInicio());a.setDataFim(origem.contrato().getDataFim());em.persist(a);
        var e=new Empregado();e.setNome(nome);e.setCpf(cpf);e.setPessoaCanonica(pessoa);e.setGestaoLocal(origem.gestao());e.setLotacao(origem.lotacao());e.setJornada(origem.jornada());e.setCargo(origem.cargo());
        e.setStatus(origem.empregado().getStatus());e.setContratos(new ArrayList<>(List.of(origem.contrato())));e.setDataAdmissao(v.getDataInicio());em.persist(e);
        return new Dados(origem.gestao(),origem.lotacao(),origem.jornada(),origem.cargo(),origem.contrato(),pessoa,v,a,e);
    }
    static Frequencia frequencia(EntityManager em,Dados f,LocalDate dia,String status) {
        var r=montarFrequencia(f,dia,status);em.persist(r);return r;
    }
    static Frequencia frequenciaComPrazo(EntityManager em,Dados f,LocalDate dia,String status,Instant primeiro) {
        var r=montarFrequencia(f,dia,status);
        new com.example.siggtrat.servico.PoliticaTemporalFrequenciaServico(7,72).capturarPrazos(r,primeiro);
        em.persist(r);return r;
    }
    private static Frequencia montarFrequencia(Dados f,LocalDate dia,String status) {
        var r=new Frequencia();r.setEmpregado(f.empregado());r.setPessoa(f.pessoa());r.setVinculoContratual(f.vinculo());r.setAlocacaoVinculo(f.alocacao());r.setDataReferencia(dia);r.setStatus(status);
        r.setGestaoLocalSnapshotId(f.gestao().getId());r.setSuperintendenciaSnapshotId(f.gestao().getSuperintendencia().getId());r.setLotacaoSnapshotId(f.lotacao().getId());
        r.setContratoSnapshotId(f.contrato().getId());r.setJornadaSnapshotId(f.jornada().getId());r.setCargoSnapshotId(f.cargo().getId());return r;
    }
}
