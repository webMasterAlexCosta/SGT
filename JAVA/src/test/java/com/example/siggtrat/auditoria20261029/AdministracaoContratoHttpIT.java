package com.example.siggtrat.auditoria20261029;

import com.example.siggtrat.dominio.acesso.PerfilAcesso;
import com.example.siggtrat.entidade.*;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import java.time.Instant;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;

/** Administrative contracts: hierarchical scopes, structural records and read-only audit. */
class AdministracaoContratoHttpIT extends AuditoriaHttpSuporte {
    @Test void cicloUsuarioOperacionalExigeGestorResponsavelESenhaTemporaria() throws Exception {
        var f=criarCenario(); String suffix=UUID.randomUUID().toString().substring(0,8);
        Map<String,Object> p=new LinkedHashMap<>();p.put("username","novo-"+suffix);p.put("email","audit-"+suffix+"@correios.com.br");
        p.put("matricula",String.valueOf(60000000+f.localA.getId()));p.put("password",SENHA);p.put("perfil","OPERACIONAL");
        p.put("superintendenciaId",f.seA.getId());p.put("gestaoLocalId",f.gestaoA.getId());p.put("lotacaoId",f.lotacaoA.getId());p.put("jornadaId",f.jornadaA.getId());
        var created=dados(executar(f.localA,post("/api/usuarios").contentType(MediaType.APPLICATION_JSON).content(json(p)),201,"Usuários criação operacional no escopo hierárquico"));
        long id=created.path("id").asLong();assertTrue(id>0);assertEquals("OPERACIONAL",created.path("perfil").asText());
        assertTrue(em.find(UsuarioAplicacao.class,id).getForceChangePassword());
        var list=dados(executar(f.localA,get("/api/usuarios"),200,"Usuários listagem só subordinados da gestão"));
        assertTrue(containsId(list,id));assertFalse(containsId(list,f.operacionalB.getId()));
        executar(f.localB,put("/api/usuarios/{id}",id).contentType(MediaType.APPLICATION_JSON).content(json(Map.of("email","outro-"+suffix+"@correios.com.br"))),404,"Usuários alteração fora da gestão");
        executar(f.localA,put("/api/usuarios/{id}",id).contentType(MediaType.APPLICATION_JSON).content(json(Map.of("email","atualizado-"+suffix+"@correios.com.br"))),200,"Usuários atualização autorizada");
        assertEquals("atualizado-"+suffix+"@correios.com.br",em.find(UsuarioAplicacao.class,id).getEmail());
        executar(f.localA,post("/api/usuarios/{id}/conceder-acesso-retroativo",id),200,"Usuários concessão temporária de janela retroativa");
        assertNotNull(em.find(UsuarioAplicacao.class,id).getRetroactiveUnlockExpireAt());
        executar(f.localA,post("/api/usuarios/{id}/revogar-acesso-retroativo",id),200,"Usuários revogação da janela retroativa");
        assertNull(em.find(UsuarioAplicacao.class,id).getRetroactiveUnlockExpireAt());
        executar(f.localA,post("/api/usuarios/{id}/redefinir-senha",id).contentType(MediaType.APPLICATION_JSON).content(json(Map.of("newPassword","TemporariaNova@2026"))),200,"Usuários redefinição exige primeiro acesso");
        assertTrue(passwordEncoder.matches("TemporariaNova@2026",em.find(UsuarioAplicacao.class,id).getPassword()));
        executar(f.localA,delete("/api/usuarios/{id}",id),200,"Usuários exclusão lógica preserva identidade");
        assertFalse(em.find(UsuarioAplicacao.class,id).getActive());
        var activated=dados(executar(f.localA,post("/api/usuarios/{id}/ativar",id),200,"Usuários reativação emite nova senha temporária"));
        assertTrue(activated.path("forceChangeOnNextLogin").asBoolean());
        assertTrue(passwordEncoder.matches(activated.path("temporaryPassword").asText(),em.find(UsuarioAplicacao.class,id).getPassword()));
        assertTrue(em.find(UsuarioAplicacao.class,id).getActive());
        var me=dados(executar(f.operacionalA,get("/api/usuarios/meu-perfil"),200,"Usuários perfil próprio disponível ao operacional"));
        assertEquals(f.operacionalA.getId(),me.path("id").asLong());
        assertFalse(me.has("password"));assertFalse(me.has("secret2fa"));
    }

    @Test void usuariosBloqueiamEscaladaForaDeEscopoPayloadInvalidoEObjetoAusente() throws Exception {
        var f=criarCenario();
        for(var u:List.of(f.presidente,f.operacionalA)) {
            executar(u,get("/api/usuarios"),403,"Usuários sem administração hierárquica "+u.getPerfil());
            executar(u,post("/api/usuarios").contentType(MediaType.APPLICATION_JSON).content("{}"),403,"Usuários criação proibida "+u.getPerfil());
            executar(u,put("/api/usuarios/{id}",f.operacionalA.getId()).contentType(MediaType.APPLICATION_JSON).content("{}"),403,"Usuários edição proibida "+u.getPerfil());
            executar(u,delete("/api/usuarios/{id}",f.operacionalA.getId()),403,"Usuários desativação proibida "+u.getPerfil());
            for(String op:List.of("conceder-acesso-retroativo","revogar-acesso-retroativo","ativar","redefinir-senha"))
                executar(u,post("/api/usuarios/{id}/"+op,f.operacionalA.getId()).contentType(MediaType.APPLICATION_JSON).content("{}"),403,"Usuários "+op+" proibido "+u.getPerfil());
        }
        executar(f.admin,post("/api/usuarios").contentType(MediaType.APPLICATION_JSON).content("{}"),400,"Usuários campos obrigatórios");
        executar(f.localA,put("/api/usuarios/{id}",f.operacionalA.getId()).contentType(MediaType.APPLICATION_JSON).content(json(Map.of("perfil","ADMINISTRADOR"))),403,"Usuários bloqueia autopromoção de subordinado");
        executar(f.localA,post("/api/usuarios/{id}/conceder-acesso-retroativo",f.operacionalB.getId()),404,"Usuários janela fora da gestão");
        executar(f.admin,post("/api/usuarios/{id}/revogar-acesso-retroativo",Long.MAX_VALUE),404,"Usuários alvo ausente");
    }

    @Test void consultaOrganizacionalObedecePerfilEEstado() throws Exception {
        var f=criarCenario();
        var se=dados(executar(f.localA,get("/api/superintendencias"),200,"Estrutura consulta local só própria SE"));
        assertEquals(1,se.size());assertEquals(f.seA.getId(),se.get(0).path("id").asLong());
        var locals=dados(executar(f.estadualA,get("/api/gestoes-locais"),200,"Estrutura consulta estadual só gestões próprias"));
        assertTrue(containsId(locals,f.gestaoA.getId()));assertFalse(containsId(locals,f.gestaoB.getId()));
        var pres=dados(executar(f.presidente,get("/api/gestoes-locais"),200,"Estrutura consulta nacional Presidente"));
        assertTrue(containsId(pres,f.gestaoA.getId()));assertTrue(containsId(pres,f.gestaoB.getId()));
        var op=dados(executar(f.operacionalA,get("/api/gestoes-locais"),200,"Estrutura operacional consulta unidade à qual está vinculado"));
        assertEquals(1,op.size());assertEquals(f.gestaoA.getId(),op.get(0).path("id").asLong());
    }

    @Test void gestaoLocalPermiteCrudTecnicoMasPreservaEstruturaEHistorico() throws Exception {
        var f=criarCenario();String code="API-"+UUID.randomUUID().toString().substring(0,8);
        Map<String,Object> p=new LinkedHashMap<>(Map.of("nome","Gestão criada via API","codigo",code,"superintendenciaId",f.seA.getId(),"ativo",true));
        var created=dados(executar(f.admin,post("/api/gestoes-locais").contentType(MediaType.APPLICATION_JSON).content(json(p)),201,"Estrutura criar gestão"));
        long id=created.path("id").asLong();assertEquals(f.seA.getId(),created.path("superintendenciaId").asLong());
        p.put("nome","Gestão atualizada via API");
        var changed=dados(executar(f.admin,put("/api/gestoes-locais/{id}",id).contentType(MediaType.APPLICATION_JSON).content(json(p)),200,"Estrutura alterar nome sem trocar identidade"));
        assertEquals("Gestão atualizada via API",changed.path("nome").asText());assertEquals(id,changed.path("id").asLong());
        executar(f.admin,delete("/api/gestoes-locais/{id}",id),409,"Estrutura exclusão física proibida por contrato");
        assertNotNull(em.find(GestaoLocal.class,id));
        p.put("superintendenciaId",f.seB.getId());
        executar(f.admin,put("/api/gestoes-locais/{id}",id).contentType(MediaType.APPLICATION_JSON).content(json(p)),400,"Estrutura UF não migra por edição");
        executar(f.admin,post("/api/gestoes-locais").contentType(MediaType.APPLICATION_JSON).content("{}"),400,"Estrutura nome código e SE obrigatórios");
        for(var u:List.of(f.localA,f.estadualA,f.nacional,f.presidente,f.operacionalA)) {
            executar(u,post("/api/gestoes-locais").contentType(MediaType.APPLICATION_JSON).content(json(p)),403,"Estrutura criação técnica exclusiva "+u.getPerfil());
            executar(u,put("/api/gestoes-locais/{id}",id).contentType(MediaType.APPLICATION_JSON).content(json(p)),403,"Estrutura edição técnica exclusiva "+u.getPerfil());
            executar(u,delete("/api/gestoes-locais/{id}",id),403,"Estrutura exclusão técnica exclusiva "+u.getPerfil());
        }
    }

    @Test void superintendenciaTemUfUnicaReativacaoControladaEExclusaoVedada() throws Exception {
        var f=criarCenario();
        // Simulate one missing structural seed without deleting any referenced row; rollback restores it.
        var original=em.createQuery("select s from SuperintendenciaEstadual s where s.uf='SP'",SuperintendenciaEstadual.class).getSingleResult();
        original.setUf("YY");em.flush();
        var payload=new LinkedHashMap<String,Object>(Map.of("uf","SP","nome","Nome recebido","ativo",true));
        var d=dados(executar(f.admin,post("/api/superintendencias").contentType(MediaType.APPLICATION_JSON).content(json(payload)),201,"Estrutura criação de SE ausente"));
        long id=d.path("id").asLong();assertEquals("SE/SP",d.path("nome").asText());
        executar(f.admin,post("/api/superintendencias").contentType(MediaType.APPLICATION_JSON).content(json(payload)),409,"Estrutura UF duplicada");
        var estadual=usuario(PerfilAcesso.GESTOR_ESTADUAL,null);estadual.setSuperintendencia(em.find(SuperintendenciaEstadual.class,id));
        payload.put("ativo",false);
        var inactive=dados(executar(f.admin,put("/api/superintendencias/{id}",id).contentType(MediaType.APPLICATION_JSON).content(json(payload)),200,"Estrutura inativação da SE"));
        assertFalse(inactive.path("ativo").asBoolean());assertFalse(estadual.getActive());
        var active=dados(executar(f.admin,post("/api/superintendencias/{id}/ativar",id),200,"Estrutura reativação gera credencial temporária"));
        assertTrue(active.path("forceChangeOnNextLogin").asBoolean());
        assertTrue(em.find(SuperintendenciaEstadual.class,id).getAtivo());
        assertTrue(passwordEncoder.matches(active.path("temporaryPassword").asText(),estadual.getPassword()));
        executar(f.admin,delete("/api/superintendencias/{id}",id),409,"Estrutura SE não pode ser excluída");
        for(var u:List.of(f.localA,f.estadualA,f.nacional,f.presidente,f.operacionalA)) {
            executar(u,post("/api/superintendencias").contentType(MediaType.APPLICATION_JSON).content(json(payload)),403,"Estrutura criar SE proibido "+u.getPerfil());
            executar(u,put("/api/superintendencias/{id}",id).contentType(MediaType.APPLICATION_JSON).content(json(payload)),403,"Estrutura editar SE proibido "+u.getPerfil());
            executar(u,post("/api/superintendencias/{id}/ativar",id),403,"Estrutura reativar SE proibido "+u.getPerfil());
            executar(u,delete("/api/superintendencias/{id}",id),403,"Estrutura excluir SE proibido "+u.getPerfil());
        }
    }

    @Test void configuracaoPublicaSoExpoeChavesPermitidasEAdministracaoValidaAtomica() throws Exception {
        var f=criarCenario();
        var initial=dados(executar(null,get("/config.json"),200,"Configuração pública sem segredos"));
        assertEquals(Set.of("EM_MANUTENCAO","PREVISAO_RETORNO","CLIENT_IP","HORAS_DIA_TRABALHO","HORA_INICIO_NOTURNO","MINUTOS_HORA_NOTURNA"),fieldNames(initial));
        var payload=List.of(Map.of("chave","HORAS_DIA_TRABALHO","valor","7"),Map.of("chave","HORA_INICIO_NOTURNO","valor","21:30"));
        executar(f.admin,post("/api/sistema/configuracoes").contentType(MediaType.APPLICATION_JSON).content(json(payload)),200,"Configuração gravação técnica validada");
        var changed=dados(executar(null,get("/config.json"),200,"Configuração pública reflete valores confirmados"));
        assertEquals(7,changed.path("HORAS_DIA_TRABALHO").asInt());assertEquals("21:30",changed.path("HORA_INICIO_NOTURNO").asText());
        var listed=dados(executar(f.admin,get("/api/sistema/configuracoes"),200,"Configuração consulta técnica"));assertTrue(listed.isArray());
        executar(f.admin,post("/api/sistema/configuracoes").contentType(MediaType.APPLICATION_JSON).content(json(List.of(Map.of("chave","HORAS_DIA_TRABALHO","valor","6"),Map.of("chave","MINUTOS_HORA_NOTURNA","valor","NaN")))),400,"Configuração lote inválido não grava parcialmente");
        assertEquals("7",em.find(ConfiguracaoSistema.class,"HORAS_DIA_TRABALHO").obterValor());
        for(var u:List.of(f.localA,f.estadualA,f.nacional,f.presidente,f.operacionalA)) {
            executar(u,get("/api/sistema/configuracoes"),403,"Configuração privada "+u.getPerfil());
            executar(u,post("/api/sistema/configuracoes").contentType(MediaType.APPLICATION_JSON).content(json(payload)),403,"Configuração escrita técnica exclusiva "+u.getPerfil());
        }
        var cache=dados(executar(f.admin,get("/api/cache/status"),200,"Diagnóstico cache técnico"));assertTrue(cache.has("cacheManager"));assertTrue(cache.has("conectado"));
        executar(f.nacional,get("/api/cache/status"),403,"Diagnóstico cache não é consulta negocial");
    }

    @Test void auditoriaEConsultavelPorFiltroEIdSomentePeloAdministrador() throws Exception {
        var f=criarCenario();String requestId=UUID.randomUUID().toString();
        RegistroAuditoria r=new RegistroAuditoria();r.setAction("AUDITORIA_HTTP_FIXTURE");r.setEntityType("Contrato");r.setEntityId(f.contratoA.getId());
        r.setUserId(f.localA.getId());r.setUsername(f.localA.getUsername());r.setRequestId(requestId);r.setCreatedAt(Instant.now());
        r.setHttpMethod("POST");r.setPath("/api/contratos");r.setDetailsJson("{\"antes\":1,\"depois\":2}");em.persist(r);
        var page=dados(executar(f.admin,get("/api/registros-auditoria").param("requestId",requestId).param("action","AUDITORIA_HTTP_FIXTURE"),200,"AUD-001 consulta paginada por evento e requisição"));
        assertEquals(1,page.path("totalElements").asInt());assertEquals(r.getId(),page.path("content").get(0).path("id").asLong());
        var detail=dados(executar(f.admin,get("/api/registros-auditoria/{id}",r.getId()),200,"AUD-001 detalhe preserva autor ação e estado"));
        assertEquals(f.localA.getId(),detail.path("userId").asLong());assertEquals("AUDITORIA_HTTP_FIXTURE",detail.path("action").asText());
        assertEquals(2,mapper.readTree(detail.path("detailsJson").asText()).path("depois").asInt());
        executar(f.admin,get("/api/registros-auditoria/{id}",Long.MAX_VALUE),404,"Auditoria registro ausente");
        for(var u:List.of(f.localA,f.estadualA,f.nacional,f.presidente,f.operacionalA)) {
            executar(u,get("/api/registros-auditoria"),403,"Auditoria técnica restrita "+u.getPerfil());
            executar(u,get("/api/registros-auditoria/{id}",r.getId()),403,"Auditoria detalhe técnico restrito "+u.getPerfil());
        }
    }
    private Set<String> fieldNames(JsonNode n){Set<String>s=new HashSet<>();n.fieldNames().forEachRemaining(s::add);return s;}
    private boolean containsId(JsonNode n,long id){for(var d:n)if(d.path("id").asLong()==id)return true;return false;}
}
