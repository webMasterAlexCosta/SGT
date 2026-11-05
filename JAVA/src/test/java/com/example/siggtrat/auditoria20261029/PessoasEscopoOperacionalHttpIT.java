package com.example.siggtrat.auditoria20261029;

import com.example.siggtrat.entidade.*;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import java.time.DayOfWeek;
import java.util.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;

class PessoasEscopoOperacionalHttpIT extends AuditoriaHttpSuporte {
    @ParameterizedTest
    @CsvSource({"pessoas,lotacao","pessoas,turno","vinculos,lotacao","vinculos,turno",
            "pessoa,lotacao","pessoa,turno","vinculo,lotacao","vinculo,turno",
            "alocacoes,lotacao","alocacoes,turno","eventos,lotacao","eventos,turno",
            "eventos-alocacoes,lotacao","eventos-alocacoes,turno","posicao-pessoa,lotacao","posicao-pessoa,turno",
            "posicao-alocacao,lotacao","posicao-alocacao,turno","opcoes,lotacao","opcoes,turno"})
    void leituraCanonicaOperacionalRespeitaLotacaoETurnoNaMesmaGestao(String recurso,String dimensao) throws Exception {
        Cenario c=criarCenario(); Jornada turno=dimensao.equals("turno")?turnoExterno(c):c.jornadaA;
        Lotacao lotacao;
        if(dimensao.equals("lotacao")) {
            lotacao=new Lotacao("Outra lotação "+c.gestaoA.getId()); lotacao.setGestaoLocal(c.gestaoA); lotacao.setMunicipio("MESMO MUNICIPIO");
            lotacao.getTurnos().add(turno); em.persist(lotacao);
        } else { lotacao=c.lotacaoA; lotacao.getTurnos().add(turno); }
        ComposicaoContratual comp=composicao(c.contratoA,lotacao,turno,c.cargoA,5);
        Pessoa pessoa=pessoa("Pessoa fora do recorte "+dimensao,cpf(840000000+c.pessoaA.getId().intValue()));
        VinculoContratual vinculo=vinculo(pessoa,c.contratoA); alocacao(vinculo,comp);
        if(recurso.equals("pessoas") || recurso.equals("vinculos")) {
            String path=recurso.equals("pessoas")?"/api/pessoas":"/api/vinculos-contratuais";
            JsonNode pagina=dados(executar(c.operacionalA,get(path).param("contratoId",c.contratoA.getId().toString()).param("ativoEm",DATA.toString()),200,
                    "PER010 coleção canônica restringe "+dimensao));
            List<Long> ids=new ArrayList<>(); pagina.path("content").forEach(n->ids.add(n.path("id").asLong()));
            assertThat(ids).contains(recurso.equals("pessoas")?c.pessoaA.getId():c.vinculoA.getId())
                    .doesNotContain(recurso.equals("pessoas")?pessoa.getId():vinculo.getId());
        } else if(recurso.equals("opcoes")) {
            JsonNode opcoes=dados(executar(c.operacionalA,get("/api/vinculos-contratuais/contratos/"+c.contratoA.getId()+"/opcoes-alocacao")
                    .param("data",DATA.toString()),200,"PER010 opções limitadas à combinação autorizada"));
            assertThat(opcoes).hasSize(1); assertThat(opcoes.get(0).path("lotacaoId").asLong()).isEqualTo(c.lotacaoA.getId());
            assertThat(opcoes.get(0).path("jornadaId").asLong()).isEqualTo(c.jornadaA.getId());
        } else {
            String path=switch(recurso) {
                case "pessoa"->"/api/pessoas/"+pessoa.getId();
                case "vinculo"->"/api/vinculos-contratuais/"+vinculo.getId();
                case "alocacoes"->"/api/vinculos-contratuais/"+vinculo.getId()+"/alocacoes";
                case "eventos"->"/api/vinculos-contratuais/"+vinculo.getId()+"/eventos";
                case "eventos-alocacoes"->"/api/vinculos-contratuais/"+vinculo.getId()+"/alocacoes/eventos";
                case "posicao-pessoa"->"/api/vinculos-contratuais/pessoas/"+pessoa.getId()+"/posicao";
                default->"/api/vinculos-contratuais/"+vinculo.getId()+"/alocacoes/posicao";
            };
            executar(c.operacionalA,get(path).param("data",DATA.toString()),404,"PER010 leitura fora do recorte "+dimensao+" "+recurso);
            executar(c.localA,get(path).param("data",DATA.toString()),200,"Gestor Local conserva consulta de toda a Gestão "+recurso);
        }
    }

    @Test
    void escopoDeConsultaUsaAlocacaoCanonicaMesmoComProjecaoAtualDivergente() throws Exception {
        Cenario c=criarCenario(); Jornada outra=turnoExterno(c); c.empregadoA.setJornada(outra);
        JsonNode pessoa=dados(executar(c.operacionalA,get("/api/pessoas/"+c.pessoaA.getId()),200,"Escopo canônico não usa projeção Empregado divergente"));
        assertThat(pessoa.path("id").asLong()).isEqualTo(c.pessoaA.getId());
        JsonNode alocacao=dados(executar(c.operacionalA,get("/api/vinculos-contratuais/"+c.vinculoA.getId()+"/alocacoes/posicao")
                .param("data",DATA.toString()),200,"História autorizada mantém turno fotografado"));
        assertThat(alocacao.path("jornadaId").asLong()).isEqualTo(c.jornadaA.getId());
    }

    private Jornada turnoExterno(Cenario c) {
        Jornada j=new Jornada(); j.setDescricao("TURNO EXTERNO "+c.gestaoA.getId()); j.setGestaoLocal(c.gestaoA);
        for(DayOfWeek dia:DayOfWeek.values()) { JornadaDiaSemana d=new JornadaDiaSemana(); d.setJornada(j); d.setDiaSemana(dia); d.setDiaTrabalho(dia.getValue()<=5);
            if(dia.getValue()<=5) {d.setEntrada1("09:00");d.setSaida1("17:00");} j.getDias().add(d); }
        em.persist(j); return j;
    }
}
