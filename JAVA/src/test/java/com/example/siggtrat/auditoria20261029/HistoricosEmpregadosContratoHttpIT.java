package com.example.siggtrat.auditoria20261029;

import com.example.siggtrat.entidade.*;
import com.example.siggtrat.servico.HistoricoIntegridadeServico;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.ZoneId;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;

class HistoricosEmpregadosContratoHttpIT extends AuditoriaHttpSuporte {
    @Autowired HistoricoIntegridadeServico integridade;

    @Test
    void consultasLegadasMantemEscopoEMinimizacaoSemDuplicarLinhas() throws Exception {
        Cenario c=criarCenario(); HistoricoVinculoContrato h=historicoContrato(c);
        for(UsuarioAplicacao usuario:List.of(c.localA,c.estadualA,c.nacional,c.admin)) {
            JsonNode lista=dados(executar(usuario,get("/api/empregados").param("contratoId",c.contratoA.getId().toString()),200,"Lista Empregado "+usuario.getPerfil()));
            assertThat(ids(lista.path("content"))).containsExactly(c.empregadoA.getId());
            JsonNode resumo=dados(executar(usuario,get("/api/empregados/resumo").param("contratoIds",c.contratoA.getId().toString())
                    .param("turnoId",c.jornadaA.getId().toString()),200,"Resumo Empregado "+usuario.getPerfil()));
            assertThat(ids(resumo.path("content"))).containsExactly(c.empregadoA.getId());
            JsonNode detalhe=dados(executar(usuario,get("/api/empregados/"+c.empregadoA.getId()),200,"Detalhe Empregado "+usuario.getPerfil()));
            assertThat(detalhe.path("id").asLong()).isEqualTo(c.empregadoA.getId());
            assertThat(detalhe.path("lotacaoId").asLong()).isEqualTo(c.lotacaoA.getId());
            assertThat(detalhe.path("contratos")).hasSize(1);
            if(usuario!=c.localA) assertThat(detalhe.path("cpf").asText()).isNotEqualTo(c.pessoaA.getCpf());
            if(usuario==c.localA || usuario==c.operacionalA) {
                JsonNode freq=dados(executar(usuario,get("/api/empregados/filtrados-frequencia").param("dataRef",DATA.toString())
                        .param("contratoId",c.contratoA.getId().toString()).param("lotacaoId",c.lotacaoA.getId().toString())
                        .param("turnoId",c.jornadaA.getId().toString()),200,"Seleção de frequência Empregado "+usuario.getPerfil()));
                assertThat(ids(freq.path("content"))).containsExactly(c.empregadoA.getId());
            }
            if(usuario!=c.operacionalA) {
                JsonNode historico=dados(executar(usuario,get("/api/empregados/"+c.empregadoA.getId()+"/historico-contratos"),200,"Histórico de contratos "+usuario.getPerfil()));
                assertThat(historico).hasSize(1); assertThat(historico.get(0).path("id").asLong()).isEqualTo(h.getId());
                assertThat(historico.get(0).path("empregadoNome").asText()).isEqualTo(c.empregadoA.getNome());
            }
        }
        for(UsuarioAplicacao usuario:List.of(c.localB,c.estadualB)) {
            executar(usuario,get("/api/empregados/"+c.empregadoA.getId()),404,"Empregado fora do escopo "+usuario.getPerfil());
            JsonNode lista=dados(executar(usuario,get("/api/empregados").param("contratoId",c.contratoA.getId().toString()),200,"Lista escopada não revela empregado externo"));
            assertThat(lista.path("content")).isEmpty();
            JsonNode resumo=dados(executar(usuario,get("/api/empregados/resumo").param("contratoIds",c.contratoA.getId().toString()),200,"Resumo escopado não revela empregado externo"));
            assertThat(resumo.path("content")).isEmpty();
        }
        for(String rota:List.of("/api/empregados","/api/empregados/resumo","/api/empregados/"+c.empregadoA.getId()))
            executar(c.operacionalA,get(rota),403,"CRUD legado não é tela operacional "+rota);
        JsonNode frequenciaOperacional=dados(executar(c.operacionalA,get("/api/empregados/filtrados-frequencia").param("dataRef",DATA.toString())
                .param("contratoId",c.contratoA.getId().toString()).param("lotacaoId",c.lotacaoA.getId().toString())
                .param("turnoId",c.jornadaA.getId().toString()),200,"Seleção operacional autorizada para frequência"));
        assertThat(ids(frequenciaOperacional.path("content"))).containsExactly(c.empregadoA.getId());
        executar(c.operacionalB,get("/api/empregados/filtrados-frequencia").param("dataRef",DATA.toString())
                .param("lotacaoId",c.lotacaoA.getId().toString()).param("turnoId",c.jornadaA.getId().toString()),403,"Frequência fora da Lotação e Turno autorizados");
        executar(c.operacionalA,get("/api/empregados/"+c.empregadoA.getId()+"/historico-contratos"),403,"Histórico contratual não é tela operacional");
        executar(c.localA,get("/api/empregados/resumo").param("contratoIds","INVALIDO"),400,"Filtro de contratos inválido");
        executar(c.localA,get("/api/empregados/filtrados-frequencia").param("dataRef",DATA.toString()),400,"Frequência exige lotação e turno");
    }

    @Test
    void historicoAssinadoEAncoradoUsaEscopoDaEpocaEmTodasAsRotas() throws Exception {
        Cenario c=criarCenario(); HistoricoEmpregado h=historicoAssinado(c.empregadoA,c.localA,"Fato administrativo original");
        historicoAssinado(c.empregadoB,c.localB,"Fato de outra Gestão");
        for(UsuarioAplicacao usuario:List.of(c.localA,c.estadualA,c.nacional,c.admin)) {
            JsonNode pagina=dados(executar(usuario,get("/api/historicos-empregados").param("size","2000"),200,"Histórico paginado "+usuario.getPerfil()));
            assertThat(ids(pagina.path("content"))).contains(h.getId());
            if(usuario==c.localA || usuario==c.estadualA) assertThat(pagina.path("content")).hasSize(1);
            JsonNode item=dados(executar(usuario,get("/api/historicos-empregados/"+h.getId()),200,"Histórico individual "+usuario.getPerfil()));
            // Texto livre segue a mesma restrição dos opcionais: não é possível
            // assegurar ausência de RG/e-mail apenas pela aparência da narrativa.
            if(usuario==c.localA) assertThat(item.path("descricao").asText()).isEqualTo("Fato administrativo original");
            else assertThat(item.path("descricao").isNull()).isTrue();
            assertThat(item.path("usuario").path("id").asLong()).isEqualTo(c.localA.getId());
            assertThat(item.path("hashIntegridade").asText()).startsWith("HMAC-SHA256:");
            JsonNode lista=dados(executar(usuario,get("/api/historicos-empregados/empregado/"+c.empregadoA.getId()),200,"Histórico por empregado "+usuario.getPerfil()));
            assertThat(lista).hasSize(1); assertThat(lista.get(0).path("id").asLong()).isEqualTo(h.getId());
            JsonNode prova=dados(executar(usuario,get("/api/historicos-empregados/empregado/"+c.empregadoA.getId()+"/integridade"),200,"Integridade HMAC e âncora "+usuario.getPerfil()));
            assertThat(prova.path("verificavel").asBoolean()).isTrue(); assertThat(prova.path("integro").asBoolean()).isTrue();
            assertThat(prova.path("registrosVerificados").asInt()).isEqualTo(1);
        }
        executar(c.localB,get("/api/historicos-empregados/"+h.getId()),403,"Registro histórico de outra Gestão bloqueado");
        executar(c.estadualB,get("/api/historicos-empregados/empregado/"+c.empregadoA.getId()+"/integridade"),403,"Integridade sem período no escopo bloqueada");
        assertThat(dados(executar(c.localB,get("/api/historicos-empregados/empregado/"+c.empregadoA.getId()),200,"Coleção histórica externa vazia"))).isEmpty();
        for(String rota:List.of("/api/historicos-empregados","/api/historicos-empregados/"+h.getId(),
                "/api/historicos-empregados/empregado/"+c.empregadoA.getId(),"/api/historicos-empregados/empregado/"+c.empregadoA.getId()+"/integridade"))
            executar(c.operacionalA,get(rota),403,"Operacional não recebe trilha administrativa "+rota);
        c.empregadoA.setGestaoLocal(c.gestaoB); c.empregadoA.setLotacao(c.lotacaoB); c.empregadoA.setJornada(c.jornadaB);
        c.empregadoA.setCargo(c.cargoB); c.empregadoA.getContratos().clear(); c.empregadoA.getContratos().add(c.contratoB);
        JsonNode preservado=dados(executar(c.localA,get("/api/historicos-empregados/"+h.getId()),200,"Mudança atual não remove acesso ao período histórico"));
        assertThat(preservado.path("id").asLong()).isEqualTo(h.getId());
    }

    @Test
    void historicoContratualLegadoNaoMudaQuandoContratoEGestaoSaoRenomeados() throws Exception {
        Cenario c=criarCenario(); HistoricoVinculoContrato h=historicoContrato(c);
        String numero=c.contratoA.getNumeroContrato(),descricao=c.contratoA.getDescricao(),gestao=c.gestaoA.getNome();
        c.contratoA.setNumeroContrato("NUMERO-NOVO"); c.contratoA.setDescricao("Descrição nova"); c.gestaoA.setNome("Gestão renomeada");
        c.contratoA.setDataFim(FIM.minusDays(10)); c.contratoA.setEfetivoPrevisto(99);
        JsonNode itens=dados(executar(c.localA,get("/api/empregados/"+c.empregadoA.getId()+"/historico-contratos"),200,"HIS004 história legada conserva fotografia"));
        assertThat(itens).hasSize(1); JsonNode item=itens.get(0);
        assertThat(item.path("numeroContrato").asText()).isEqualTo(numero);
        assertThat(item.path("descricaoContrato").asText()).isEqualTo(descricao);
        assertThat(item.path("gestaoLocalNome").asText()).isEqualTo(gestao);
        assertThat(item.path("contratoDataFim").asText()).isEqualTo(FIM.toString());
        assertThat(item.path("efetivoPrevisto").asInt()).isEqualTo(5);
        assertThat(item.path("id").asLong()).isEqualTo(h.getId());
    }

    private HistoricoVinculoContrato historicoContrato(Cenario c) {
        HistoricoVinculoContrato h=new HistoricoVinculoContrato(); h.setEmpregado(c.empregadoA); h.setContrato(c.contratoA);
        h.setLotacao(c.lotacaoA); h.setJornada(c.jornadaA); h.setCargo(c.cargoA);
        h.setDataInicio(INICIO.atStartOfDay(ZoneId.of("America/Sao_Paulo"))); h.setObservacao("Período original"); em.persist(h); return h;
    }
    private HistoricoEmpregado historicoAssinado(Empregado empregado,UsuarioAplicacao usuario,String descricao) {
        HistoricoEmpregado h=new HistoricoEmpregado(); h.setEmpregado(empregado); h.setUsuario(usuario);
        h.setDataEvento(DATA.atTime(10,0)); h.setDescricao(descricao); return integridade.salvarAssinado(h);
    }
    private List<Long> ids(JsonNode nodes) { List<Long> ids=new ArrayList<>(); nodes.forEach(n->ids.add(n.path("id").asLong())); return ids; }
}
