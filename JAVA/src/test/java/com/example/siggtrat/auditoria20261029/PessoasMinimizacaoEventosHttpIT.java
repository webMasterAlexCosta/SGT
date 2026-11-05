package com.example.siggtrat.auditoria20261029;

import com.example.siggtrat.entidade.*;
import com.example.siggtrat.dominio.catalogo.TipoCatalogoTemporal;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.http.MediaType;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;

/** Evidência administrativa é conservada no evento, com exposição dependente do perfil. */
class PessoasMinimizacaoEventosHttpIT extends AuditoriaHttpSuporte {
    @ParameterizedTest
    @ValueSource(strings={"GESTOR_LOCAL","OPERACIONAL","GESTOR_ESTADUAL","GESTOR_NACIONAL","ADMINISTRADOR","PRESIDENTE"})
    void eventoDeAlocacaoPreservaDocumentoMasMinimizaEvidenciaEAutorizacaoPorPerfil(String perfil) throws Exception {
        Cenario c=criarCenario();
        Cargo destino=new Cargo(); destino.setNome("Cargo de destino "+c.gestaoA.getId()); destino.setGestaoLocal(c.gestaoA); em.persist(destino);
        // Fonte sintética do Cargo desta fixture desde o início da composição;
        // disponibilidade antiga não é inferida do cadastro mestre atual.
        EventoDisponibilidadeCatalogo origem=new EventoDisponibilidadeCatalogo();
        origem.setTipoCatalogo(TipoCatalogoTemporal.CARGO); origem.setCatalogoId(destino.getId());
        origem.setDataEfetiva(INICIO); origem.setAtivo(true);
        origem.setMotivo("Fonte sintética explícita do Cargo para a mudança de alocação documentada"); em.persist(origem);
        ComposicaoContratual composicao= composicao(c.contratoA,c.lotacaoA,c.jornadaA,destino,5);
        String evidencia="Documento pessoal: CPF "+c.pessoaA.getCpf()+", RG material 12345";
        String autorizacao="Autorização nominativa: "+c.pessoaA.getNome()+"; documento restrito SEI-999";
        Map<String,Object> req=new LinkedHashMap<>(Map.of("dataEfetiva",DATA,"composicaoId",composicao.getId(),
                "justificativa","Mudança documentada de cargo","evidencia",evidencia,"autorizacao",autorizacao));
        String base="/api/vinculos-contratuais/"+c.vinculoA.getId();
        JsonNode previa=dados(executar(c.localA,post(base+"/previas/alocacoes").contentType(MediaType.APPLICATION_JSON).content(json(req)),
                200,"Prévia real da alteração que contém documentos restritos"));
        assertThat(previa.path("incompatibilidades")).isEmpty();
        req.put("tokenPrevia",previa.path("token").asText()); req.put("confirmarImpacto",true);
        executar(c.localA,post(base+"/alocacoes").contentType(MediaType.APPLICATION_JSON).content(json(req)),200,
                "Registro real de alocação mantém a evidência administrativa");
        UsuarioAplicacao leitor=switch(perfil) {
            case "GESTOR_LOCAL" -> c.localA;
            case "OPERACIONAL" -> c.operacionalA;
            case "GESTOR_ESTADUAL" -> c.estadualA;
            case "GESTOR_NACIONAL" -> c.nacional;
            case "ADMINISTRADOR" -> c.admin;
            case "PRESIDENTE" -> c.presidente;
            default -> throw new IllegalArgumentException(perfil);
        };
        JsonNode eventos=dados(executar(leitor,get(base+"/alocacoes/eventos"),200,
                "PER004 PER006 PER011 consulta de evento por "+perfil));
        assertThat(eventos).hasSize(2);
        int alteracoesParaDestino=0;
        for (JsonNode evento:eventos) {
            if (evento.path("cargoNovoId").asLong()==destino.getId()) {
                alteracoesParaDestino++;
                assertThat(evento.path("tipoEvento").asText()).isEqualTo("ALTERACAO");
            } else {
                assertThat(evento.path("tipoEvento").asText()).isEqualTo("AJUSTE_LIMITE_VINCULO");
                assertThat(evento.path("fimNovo").asText()).isEqualTo(DATA.minusDays(1).toString());
            }
            assertThat(evento.path("hashEvento").asText()).hasSize(64);
            if (perfil.equals("GESTOR_LOCAL")) {
                assertThat(evento.path("evidencia").asText()).isEqualTo(evidencia);
                assertThat(evento.path("autorizacao").asText()).isEqualTo(autorizacao);
            } else {
                assertThat(evento.path("evidencia").isNull()).as("Evidência restrita para %s",perfil).isTrue();
                assertThat(evento.path("autorizacao").isNull()).as("Autorização restrita para %s",perfil).isTrue();
            }
            EventoAlocacaoVinculo armazenado=em.find(EventoAlocacaoVinculo.class,evento.path("id").asLong());
            assertThat(armazenado.getEvidencia()).isEqualTo(evidencia);
            assertThat(armazenado.getAutorizacao()).isEqualTo(autorizacao);
        }
        assertThat(alteracoesParaDestino).isEqualTo(1);
        executar(c.localB,get(base+"/alocacoes/eventos"),404,"Documentos históricos não concedem consulta em outra Gestão");
        executar(c.operacionalB,get(base+"/alocacoes/eventos"),404,"Evento fora da Lotação/Turno operacional é invisível");
    }
}
