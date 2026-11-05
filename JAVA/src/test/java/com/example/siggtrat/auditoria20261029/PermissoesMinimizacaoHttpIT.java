package com.example.siggtrat.auditoria20261029;

import com.example.siggtrat.entidade.*;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.http.MediaType;
import java.time.Instant;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;

/** PER-001..007/009/011: real authorization and minimized HTTP contracts. */
class PermissoesMinimizacaoHttpIT extends AuditoriaHttpSuporte {
    @ParameterizedTest
    @ValueSource(strings={"GESTOR_LOCAL", "OPERACIONAL", "GESTOR_ESTADUAL", "GESTOR_NACIONAL", "ADMINISTRADOR", "PRESIDENTE"})
    void posicaoTemporalMinimizaMatriculaEmCadaVinculo(String perfil) throws Exception {
        var f=criarCenario();
        var usuario=switch(perfil) {
            case "GESTOR_LOCAL" -> f.localA;
            case "OPERACIONAL" -> f.operacionalA;
            case "GESTOR_ESTADUAL" -> f.estadualA;
            case "GESTOR_NACIONAL" -> f.nacional;
            case "ADMINISTRADOR" -> f.admin;
            default -> f.presidente;
        };
        var body=dados(executar(usuario,get("/api/vinculos-contratuais/pessoas/{id}/posicao",f.pessoaA.getId())
                .param("data",DATA.toString()),200,"PER-006/011 matrícula aninhada na posição temporal: "+perfil));
        var vinculos=body.path("vinculos");
        assertEquals(1,vinculos.size());
        assertEquals(f.vinculoA.getId(),vinculos.get(0).path("vinculoId").asLong());
        var matricula=vinculos.get(0).path("matricula");
        if(perfil.equals("PRESIDENTE")) {
            assertTrue(matricula.isNull()||matricula.isMissingNode(),"Consulta institucional não expõe matrícula em níveis internos do DTO");
        } else if(perfil.equals("GESTOR_LOCAL")||perfil.equals("OPERACIONAL")) {
            assertEquals(f.vinculoA.getMatricula(),matricula.asText());
        } else {
            String original=f.vinculoA.getMatricula();
            assertEquals("*".repeat(original.length()-2)+original.substring(original.length()-2),matricula.asText(),
                    "Auditoria recebe a mesma minimização da listagem e do detalhe do vínculo");
        }
    }

    @ParameterizedTest
    @ValueSource(strings={"pessoas", "pessoa", "correcoes", "vinculos", "vinculo", "posicao"})
    void presidenteConsultaNacionalCanonicaSemIdentificacaoPessoal(String recurso) throws Exception {
        var f=criarCenario();
        f.pessoaB.setRg("RG-RESTRITO"); f.pessoaB.setEmail("privado@example.test");
        String path=switch(recurso) {
            case "pessoas" -> "/api/pessoas?contratoId="+f.contratoB.getId();
            case "pessoa" -> "/api/pessoas/"+f.pessoaB.getId();
            case "correcoes" -> "/api/pessoas/"+f.pessoaB.getId()+"/correcoes-identidade";
            case "vinculos" -> "/api/vinculos-contratuais?contratoId="+f.contratoB.getId();
            case "vinculo" -> "/api/vinculos-contratuais/"+f.vinculoB.getId();
            default -> "/api/vinculos-contratuais/pessoas/"+f.pessoaB.getId()+"/posicao?data="+DATA;
        };
        JsonNode body=dados(executar(f.presidente,get(path),200,"PER-004/011 consulta nacional minimizada "+recurso));
        assertFalse(body.toString().contains(f.pessoaB.getCpf()));
        assertFalse(body.toString().contains(f.pessoaB.getNome()));
        assertFalse(body.toString().contains("RG-RESTRITO"));
        assertFalse(body.toString().contains("privado@example.test"));
        if(recurso.equals("pessoas")||recurso.equals("vinculos")) assertEquals(1,body.path("totalElements").asInt());
        if(recurso.equals("pessoa")) assertEquals(f.pessoaB.getId(),body.path("id").asLong());
        if(recurso.equals("vinculo")) assertEquals(f.contratoB.getId(),body.path("contratoId").asLong());
        if(recurso.equals("posicao")) assertEquals(f.pessoaB.getId(),body.path("pessoaId").asLong());
        if(recurso.equals("correcoes")) assertTrue(body.isArray());
    }

    @Test void auditoresRecebemCpfMascaradoEOpcionalRestritoAusente() throws Exception {
        var f=criarCenario(); f.pessoaA.setRg("RG-NAO-PUBLICO"); f.pessoaA.setEmail("pessoal@example.test");
        for(var auditor:List.of(f.estadualA,f.nacional,f.admin)) {
            var d=dados(executar(auditor,get("/api/pessoas/{id}",f.pessoaA.getId()),200,"PER-006/011 minimização "+auditor.getPerfil()));
            assertEquals(f.pessoaA.getId(),d.path("id").asLong());
            assertEquals("***.***.***-"+f.pessoaA.getCpf().substring(9),d.path("cpf").asText());
            assertTrue(d.path("rg").isNull()||d.path("rg").isMissingNode());
            assertTrue(d.path("email").isNull()||d.path("email").isMissingNode());
        }
        executar(f.estadualB,get("/api/pessoas/{id}",f.pessoaA.getId()),404,"PER-002 pessoa fora do estado não é exposta");
    }

    @Test void somenteGestorLocalResponsavelEscreveDadosPessoais() throws Exception {
        var f=criarCenario(); String body=json(Map.of("rg","RG-AUDITADO"));
        for(var u:List.of(f.admin,f.estadualA,f.nacional,f.presidente,f.operacionalA))
            executar(u,put("/api/pessoas/{id}/dados-opcionais",f.pessoaA.getId()).contentType(MediaType.APPLICATION_JSON).content(body),403,
                    "PER-001..005 escrita pessoal proibida "+u.getPerfil());
        executar(f.localB,put("/api/pessoas/{id}/dados-opcionais",f.pessoaA.getId()).contentType(MediaType.APPLICATION_JSON).content(body),404,"PER-001 escrita fora da gestão");
        var d=dados(executar(f.localA,put("/api/pessoas/{id}/dados-opcionais",f.pessoaA.getId()).contentType(MediaType.APPLICATION_JSON).content(body),200,"PER-001 escrita local autorizada"));
        assertEquals("RG-AUDITADO",d.path("rg").asText());
        assertEquals("RG-AUDITADO",em.find(Pessoa.class,f.pessoaA.getId()).getRg());
    }

    @Test void permissaoPessoalTemEscopoValidadeUsoAuditadoERevogacao() throws Exception {
        var f=criarCenario();
        String consulta="/api/pessoas/"+f.pessoaA.getId()+"/dados-restritos?finalidade=Auditoria%20formal";
        executar(f.presidente,get(consulta),403,"PER-011 acesso integral exige concessão");
        var payload=permissao(f,f.presidente);
        var granted=dados(executar(f.admin,post("/api/permissoes-excepcionais").contentType(MediaType.APPLICATION_JSON).content(json(payload)),201,"PER-005/011 administrador concede escopo específico"));
        long id=granted.path("id").asLong(); assertTrue(id>0); assertTrue(granted.path("ativa").asBoolean());
        assertEquals(f.contratoA.getId(),granted.path("contratoId").asLong());
        var full=dados(executar(f.presidente,get(consulta),200,"PER-011 acesso justificado e auditado"));
        assertEquals(f.pessoaA.getCpf(),full.path("cpf").asText());
        executar(f.presidente,get("/api/pessoas/{id}/dados-restritos",f.pessoaB.getId()).param("finalidade","Auditoria"),403,"PER-011 concessão não atravessa contrato");
        var usos=dados(executar(f.admin,get("/api/permissoes-excepcionais/{id}/usos",id),200,"AUD-001 uso da concessão consultável"));
        assertEquals(1,usos.size());
        assertEquals(f.presidente.getId(),usos.get(0).path("usuarioId").asLong());
        assertEquals(f.pessoaA.getId(),usos.get(0).path("recursoId").asLong());
        var own=dados(executar(f.presidente,get("/api/permissoes-excepcionais"),200,"PER-005 beneficiário só consulta próprias concessões"));
        assertTrue(containsId(own,id));
        var other=dados(executar(f.nacional,get("/api/permissoes-excepcionais"),200,"PER-005 terceiro não vê concessão"));
        assertFalse(containsId(other,id));
        executar(f.presidente,get("/api/permissoes-excepcionais/{id}/usos",id),403,"AUD-001 usos restritos à administração técnica");
        executar(f.admin,post("/api/permissoes-excepcionais").contentType(MediaType.APPLICATION_JSON).content(json(payload)),409,"PER-005 concessão ativa duplicada rejeitada");
        var revoked=dados(executar(f.admin,post("/api/permissoes-excepcionais/{id}/revogar",id).contentType(MediaType.APPLICATION_JSON).content(json(Map.of("motivo","Finalidade encerrada"))),200,"PER-005 revogação identificada"));
        assertFalse(revoked.path("ativa").asBoolean());
        assertEquals("Finalidade encerrada",revoked.path("motivoRevogacao").asText());
        executar(f.presidente,get(consulta),403,"PER-011 revogação tem efeito imediato");
        executar(f.admin,post("/api/permissoes-excepcionais/{id}/revogar",id).contentType(MediaType.APPLICATION_JSON).content(json(Map.of("motivo","Repetição"))),409,"PER-005 revogação repetida é conflito");
    }

    @Test void concessaoInvalidaNaoAmpliaPrivilegios() throws Exception {
        var f=criarCenario(); var payload=permissao(f,f.estadualB);
        executar(f.admin,post("/api/permissoes-excepcionais").contentType(MediaType.APPLICATION_JSON).content(json(payload)),400,"PER-002 concessão fora do estado rejeitada");
        payload=permissao(f,f.nacional);
        executar(f.localA,post("/api/permissoes-excepcionais").contentType(MediaType.APPLICATION_JSON).content(json(payload)),403,"PER-005 concessão exclusiva administração");
        payload.put("validoAte",Instant.now().minusSeconds(60).toString());
        executar(f.admin,post("/api/permissoes-excepcionais").contentType(MediaType.APPLICATION_JSON).content(json(payload)),400,"PER-005 validade expirada rejeitada");
        executar(f.admin,get("/api/permissoes-excepcionais/{id}/usos",Long.MAX_VALUE),404,"PER-005 concessão ausente");
    }
    private Map<String,Object> permissao(Cenario f,UsuarioAplicacao u) {
        Map<String,Object> p=new LinkedHashMap<>(); p.put("tipo","ACESSO_DADOS_PESSOAIS_RESTRITOS");
        p.put("usuarioBeneficiarioId",u.getId()); p.put("contratoId",f.contratoA.getId());
        p.put("finalidade","Auditoria formal");p.put("justificativa","Verificação autorizada de identidade");
        p.put("autorizacaoAdministrativa","SEI-PER-TESTE");p.put("validoAte",Instant.now().plusSeconds(3600).toString());return p;
    }
    private boolean containsId(JsonNode nodes,long id) { for(var node:nodes) if(node.path("id").asLong()==id)return true;return false; }
}
