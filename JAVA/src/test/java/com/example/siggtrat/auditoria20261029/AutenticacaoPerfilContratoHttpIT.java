package com.example.siggtrat.auditoria20261029;

import com.example.siggtrat.entidade.*;
import com.example.siggtrat.servico.EmailGmailServico;
import com.example.siggtrat.servico.TotpServico;
import com.example.siggtrat.utilitario.RelogioSistema;
import com.example.siggtrat.utilitario.UtilitarioHashSeguranca;
import com.fasterxml.jackson.databind.JsonNode;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import java.util.*;
import java.util.regex.Pattern;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;

/** Real HTTP/session/database flow; only outbound mail is replaced, never sent. */
class AutenticacaoPerfilContratoHttpIT extends AuditoriaHttpSuporte {
    @MockitoBean EmailGmailServico email;
    @Autowired TotpServico totp;
    @BeforeEach void emailExternoControlado(){when(email.enviarHtml(anyString(),anyString(),anyString())).thenReturn(true);}

    @Test void loginSessaoRenovacaoLogoutECsrfReal() throws Exception {
        var f=criarCenario();
        var bootstrap=dados(executar(null,get("/api/autenticacao/csrf"),200,"Autenticação bootstrap CSRF público"));
        assertEquals("X-XSRF-TOKEN",bootstrap.path("headerName").asText());assertFalse(bootstrap.path("token").asText().isBlank());
        executar(null,post("/api/autenticacao/autenticar").contentType(MediaType.APPLICATION_JSON).content(json(Map.of("matricula",f.localA.getMatricula(),"password",SENHA))),403,"Autenticação login exige CSRF");
        executar(null,publico(post("/api/autenticacao/autenticar")).contentType(MediaType.APPLICATION_JSON).content(json(Map.of("matricula",f.localA.getMatricula(),"password","SenhaIncorreta@2026"))),401,"Autenticação credencial inválida");
        MvcResult login=login(f.localA,SENHA);var d=dados(login);assertEquals(f.localA.getUsername(),d.path("username").asText());
        assertEquals("GESTOR_LOCAL",d.path("perfil").asText());assertFalse(d.has("token"));assertFalse(d.has("password"));
        Map<String,Cookie> jar=cookies(login);assertTrue(jar.containsKey("autenticacao"));assertTrue(jar.containsKey("id_dispositivo"));
        renovarCsrf(jar);
        var session=dados(executar(null,comCookies(get("/api/autenticacao/sessao").param("touch","true"),jar),200,"Autenticação sessão vinculada a dispositivo"));
        assertTrue(session.path("authenticated").asBoolean());assertEquals(f.localA.getId(),session.path("user").path("id").asLong());
        assertTrue(session.path("session").path("boundToDevice").asBoolean());
        var renewed=executar(null,comCookies(post("/api/autenticacao/renovar-sessao"),jar).contentType(MediaType.APPLICATION_JSON).content(json(Map.of("password",SENHA))),200,"Autenticação renovação com confirmação");
        jar.putAll(cookies(renewed));renovarCsrf(jar);
        executar(null,comCookies(post("/api/autenticacao/encerrar-sessao"),jar),200,"Autenticação logout revoga sessão persistida");
        assertEquals(0L,em.createQuery("select count(t) from TokenAtivo t where t.username=:u",Long.class).setParameter("u",f.localA.getUsername()).getSingleResult());
        executar(null,comCookies(get("/api/autenticacao/sessao"),jar),401,"Autenticação cookie revogado não recupera sessão");
    }

    @Test void primeiroAcessoBloqueiaNegocioAteTrocarSenhaERotacionaSessao() throws Exception {
        var f=criarCenario();f.localA.setForceChangePassword(true);
        var login=login(f.localA,SENHA);assertTrue(dados(login).path("forceChangePassword").asBoolean());
        Map<String,Cookie> jar=cookies(login);renovarCsrf(jar);
        executar(null,comCookies(get("/api/contratos"),jar),403,"Primeiro acesso impede consulta negocial antes da troca");
        executar(null,comCookies(post("/api/autenticacao/alterar-senha-inicial"),jar).contentType(MediaType.APPLICATION_JSON).content(json(Map.of("newPassword",SENHA))),400,"Primeiro acesso rejeita reutilização de senha temporária");
        var changed=executar(null,comCookies(post("/api/autenticacao/alterar-senha-inicial"),jar).contentType(MediaType.APPLICATION_JSON).content(json(Map.of("newPassword","NovaSenhaInicial@2026"))),200,"Primeiro acesso troca senha e libera conta");
        assertFalse(f.localA.getForceChangePassword());assertTrue(passwordEncoder.matches("NovaSenhaInicial@2026",f.localA.getPassword()));
        assertFalse(f.localA.getTwoFactorEnabled());assertNull(f.localA.getSecret2fa());
        jar.putAll(cookies(changed));renovarCsrf(jar);
        var session=dados(executar(null,comCookies(get("/api/autenticacao/sessao"),jar),200,"Primeiro acesso sessão nova reflete liberação"));
        assertFalse(session.path("user").path("forceChangePassword").asBoolean());
        executar(null,comCookies(post("/api/autenticacao/alterar-senha-inicial"),jar).contentType(MediaType.APPLICATION_JSON).content(json(Map.of("newPassword","OutraSenha@2026"))),403,"Primeiro acesso não pode ser reutilizado como troca comum");
    }

    @Test void cadastroPublicoValidaEstruturaEFicaInativoAteAprovacaoLocal() throws Exception {
        var f=criarCenario();String suffix=UUID.randomUUID().toString().substring(0,8);
        var states=dados(executar(null,get("/api/autenticacao/superintendencias"),200,"Cadastro público unidades ativas sem usuários"));assertTrue(states.isArray());
        var locais=dados(executar(null,get("/api/autenticacao/gestoes-locais").param("superintendenciaId",f.seA.getId().toString()),200,"Cadastro público gestões da SE selecionada"));
        assertTrue(containsId(locais,f.gestaoA.getId()));assertFalse(containsId(locais,f.gestaoB.getId()));
        Map<String,Object> p=new LinkedHashMap<>(Map.of("username","solicitante-"+suffix,"email","solicitante-"+suffix+"@correios.com.br","matricula",String.valueOf(70000000+f.localA.getId()),"superintendenciaId",f.seA.getId(),"gestaoLocalId",f.gestaoB.getId()));
        executar(null,publico(post("/api/autenticacao/cadastrar")).contentType(MediaType.APPLICATION_JSON).content(json(p)),400,"Cadastro público combinação SE/gestão inválida");
        p.put("gestaoLocalId",f.gestaoA.getId());
        executar(null,publico(post("/api/autenticacao/cadastrar")).contentType(MediaType.APPLICATION_JSON).content(json(p)),200,"Cadastro público solicita acesso sem ativação automática");
        var user=em.createQuery("select u from UsuarioAplicacao u where u.username=:u",UsuarioAplicacao.class).setParameter("u",p.get("username")).getSingleResult();
        assertFalse(user.getActive());assertTrue(user.getForceChangePassword());assertEquals("OPERACIONAL",user.getPerfil().name());assertNull(user.getLotacao());
        executar(null,publico(post("/api/autenticacao/cadastrar")).contentType(MediaType.APPLICATION_JSON).content(json(p)),400,"Cadastro público duplicidade rejeitada");
    }

    @Test void loginComSegundoFatorSoEmiteSessaoDepoisDoCodigoValido() throws Exception {
        var f=criarCenario();String secret=totp.gerarSegredo();f.localA.setSecret2fa(secret);f.localA.setTwoFactorEnabled(true);
        var credentials=new LinkedHashMap<String,Object>(Map.of("matricula",f.localA.getMatricula(),"password",SENHA));
        var challenge=executar(null,publico(post("/api/autenticacao/autenticar")).contentType(MediaType.APPLICATION_JSON).content(json(credentials)),202,"Autenticação 2FA exige segundo fator antes de emitir cookie");
        assertTrue(dados(challenge).path("requiresTwoFactor").asBoolean());assertFalse(cookies(challenge).containsKey("autenticacao"));
        credentials.put("totp","invalido");
        executar(null,publico(post("/api/autenticacao/autenticar")).contentType(MediaType.APPLICATION_JSON).content(json(credentials)),401,"Autenticação 2FA código inválido não concede sessão");
        credentials.put("totp",codigo(secret));
        var accepted=executar(null,publico(post("/api/autenticacao/autenticar")).contentType(MediaType.APPLICATION_JSON).content(json(credentials)),200,"Autenticação 2FA código válido cria sessão verificada");
        assertTrue(dados(accepted).path("twoFactorVerified").asBoolean());assertTrue(cookies(accepted).containsKey("autenticacao"));
    }

    @Test void perfilValidaSenhaTrocaSenhaEInscreveSegundoFatorComCodigoReal() throws Exception {
        var f=criarCenario();
        var valid=dados(executar(f.localA,post("/api/perfil/validar-senha").contentType(MediaType.APPLICATION_JSON).content(json(Map.of("password",SENHA))),200,"Perfil confirmação de senha"));assertTrue(valid.path("valid").asBoolean());
        executar(f.localA,post("/api/perfil/validar-senha").contentType(MediaType.APPLICATION_JSON).content(json(Map.of("password","errada"))),400,"Perfil senha incorreta");
        var status=dados(executar(f.localA,get("/api/perfil/2fa/status"),200,"Perfil 2FA inicialmente desabilitado"));assertFalse(status.path("enabled").asBoolean());
        executar(f.localA,post("/api/perfil/2fa/configurar").contentType(MediaType.APPLICATION_JSON).content(json(Map.of("currentPassword","errada"))),400,"Perfil configuração de 2FA exige senha atual");
        var setup=dados(executar(f.localA,post("/api/perfil/2fa/configurar").contentType(MediaType.APPLICATION_JSON).content(json(Map.of("currentPassword",SENHA))),200,"Perfil configuração de 2FA entrega segredo somente na inscrição"));
        String secret=setup.path("secret").asText();assertTrue(totp.segredoUtilizavel(secret));assertTrue(setup.path("qrCodeDataUrl").asText().startsWith("data:image/svg+xml;base64,"));
        executar(f.localA,post("/api/perfil/2fa/ativar").contentType(MediaType.APPLICATION_JSON).content(json(Map.of("token","invalid"))),400,"Perfil ativação rejeita TOTP malformado");
        var enabled=dados(executar(f.localA,post("/api/perfil/2fa/ativar").contentType(MediaType.APPLICATION_JSON).content(json(Map.of("token",codigo(secret)))),200,"Perfil ativação exige TOTP válido"));assertTrue(enabled.path("enabled").asBoolean());
        var status2=dados(executar(f.localA,get("/api/perfil/2fa/status"),200,"Perfil status não expõe segredo"));assertTrue(status2.path("enabled").asBoolean());assertFalse(status2.has("secret"));
        executar(f.localA,delete("/api/perfil/2fa").contentType(MediaType.APPLICATION_JSON).content(json(Map.of("currentPassword",SENHA,"token",codigo(secret)))),200,"Perfil desativação de 2FA exige senha e código");
        assertFalse(f.localA.getTwoFactorEnabled());assertNull(f.localA.getSecret2fa());
        executar(f.localA,post("/api/perfil/alterar-senha").contentType(MediaType.APPLICATION_JSON).content(json(Map.of("currentPassword",SENHA,"newPassword","PerfilNovaSenha@2026"))),200,"Perfil troca própria senha");
        assertTrue(passwordEncoder.matches("PerfilNovaSenha@2026",f.localA.getPassword()));
    }

    @Test void avatarAceitaImagemEEntregaMesmoConteudoRejeitandoFormatoInvalido() throws Exception {
        var f=criarCenario();byte[] png=Base64.getDecoder().decode("iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mP8/x8AAwMCAO+afo0AAAAASUVORK5CYII=");
        var upload=dados(executar(f.localA,multipart("/api/perfil/avatar").file(new MockMultipartFile("file","avatar.png","image/png",png)),200,"Perfil upload de avatar PNG"));
        String path=upload.path("fotoUrl").asText();assertTrue(path.startsWith("/api/perfil/avatar/avatar_"));
        var downloaded=executar(f.localA,get(path),200,"Perfil download de avatar persistido");assertArrayEquals(png,downloaded.getResponse().getContentAsByteArray());
        executar(f.localA,multipart("/api/perfil/avatar").file(new MockMultipartFile("file","documento.txt","text/plain","texto".getBytes())),400,"Perfil avatar rejeita tipo indevido");
        executar(f.localA,get("/api/perfil/avatar/avatar_0_0.png"),404,"Perfil avatar ausente");
    }

    @Test void recuperacaoEnviaLinkConsomeUmaVezERevogaSessoes() throws Exception {
        var f=criarCenario();
        executar(f.localA,get("/api/usuarios/meu-perfil"),200,"Recuperação prepara sessão a revogar");
        var generic=dados(executar(null,publico(post("/api/autenticacao/recuperar-senha/solicitar")).contentType(MediaType.APPLICATION_JSON).content(json(Map.of("email","ausente@example.test"))),200,"Recuperação não enumera contas ausentes"));
        var existing=dados(executar(null,publico(post("/api/autenticacao/recuperar-senha/solicitar")).contentType(MediaType.APPLICATION_JSON).content(json(Map.of("email",f.localA.getEmail()))),200,"Recuperação envia link para conta ativa"));
        assertEquals(generic,existing);
        var html=ArgumentCaptor.forClass(String.class);verify(email).enviarHtml(eq(f.localA.getEmail()),anyString(),html.capture());
        var matcher=Pattern.compile("token=([A-Za-z0-9_-]+)").matcher(html.getValue());assertTrue(matcher.find());String raw=matcher.group(1);
        var token=em.createQuery("select t from RecuperacaoSenhaToken t where t.usuario.id=:id",RecuperacaoSenhaToken.class).setParameter("id",f.localA.getId()).getSingleResult();
        assertEquals(UtilitarioHashSeguranca.sha256Hexadecimal(raw),token.getTokenHash());assertNotEquals(raw,token.getTokenHash());
        var validated=dados(executar(null,get("/api/autenticacao/recuperar-senha/validar").param("token",raw),200,"Recuperação link válido"));assertTrue(validated.path("valid").asBoolean());
        String before=f.localA.getPassword();
        executar(null,publico(post("/api/autenticacao/recuperar-senha/confirmar")).contentType(MediaType.APPLICATION_JSON).content(json(Map.of("token",raw))),200,"Recuperação confirma link e envia senha temporária");
        assertNotEquals(before,f.localA.getPassword());assertTrue(f.localA.getForceChangePassword());assertNotNull(token.getUsedAt());
        assertEquals(0L,em.createQuery("select count(t) from TokenAtivo t where t.username=:u",Long.class).setParameter("u",f.localA.getUsername()).getSingleResult());
        executar(null,publico(post("/api/autenticacao/recuperar-senha/confirmar")).contentType(MediaType.APPLICATION_JSON).content(json(Map.of("token",raw))),400,"Recuperação link não pode ser consumido duas vezes");
    }

    @Test @Transactional(propagation=Propagation.NOT_SUPPORTED)
    void falhaDeEmailReverteSenhaEConsumoEmTransacaoReal() throws Exception {
        var f=criarCenarioPersistido();String raw="token-sintetico-"+UUID.randomUUID();
        Long tokenId=emTransacao(()->{var t=new RecuperacaoSenhaToken();t.setUsuario(em.find(UsuarioAplicacao.class,f.localA.getId()));t.setTokenHash(UtilitarioHashSeguranca.sha256Hexadecimal(raw));t.setCreatedAt(RelogioSistema.agora());t.setExpireAt(RelogioSistema.agora().plusMinutes(30));em.persist(t);em.flush();return t.getId();});
        String before=emTransacao(()->em.find(UsuarioAplicacao.class,f.localA.getId()).getPassword());
        when(email.enviarHtml(anyString(),anyString(),anyString())).thenReturn(false);
        executar(null,publico(post("/api/autenticacao/recuperar-senha/confirmar")).contentType(MediaType.APPLICATION_JSON).content(json(Map.of("token",raw))),503,"Recuperação indisponibilidade de e-mail causa rollback real");
        emTransacao(()->{assertEquals(before,em.find(UsuarioAplicacao.class,f.localA.getId()).getPassword());assertNull(em.find(RecuperacaoSenhaToken.class,tokenId).getUsedAt());return null;});
    }

    private MvcResult login(UsuarioAplicacao user,String senha)throws Exception{return executar(null,publico(post("/api/autenticacao/autenticar")).contentType(MediaType.APPLICATION_JSON).content(json(Map.of("matricula",user.getMatricula(),"password",senha))),200,"Autenticação login real com matrícula e senha");}
    private MockHttpServletRequestBuilder publico(MockHttpServletRequestBuilder r){return r.cookie(new Cookie("XSRF-TOKEN","csrf-publico-auditoria")).header("X-XSRF-TOKEN","csrf-publico-auditoria");}
    private Map<String,Cookie> cookies(MvcResult r){Map<String,Cookie> jar=new LinkedHashMap<>();for(String h:r.getResponse().getHeaders("Set-Cookie")){String[] pair=h.split(";",2)[0].split("=",2);if(pair.length==2&&!pair[1].isBlank())jar.put(pair[0],new Cookie(pair[0],pair[1]));}return jar;}
    private MockHttpServletRequestBuilder comCookies(MockHttpServletRequestBuilder r,Map<String,Cookie> jar){r.cookie(jar.values().toArray(Cookie[]::new));if(jar.containsKey("XSRF-TOKEN"))r.header("X-XSRF-TOKEN",jar.get("XSRF-TOKEN").getValue());return r;}
    private void renovarCsrf(Map<String,Cookie> jar)throws Exception{jar.remove("XSRF-TOKEN");var result=executar(null,comCookies(get("/api/autenticacao/csrf"),jar),200,"Autenticação renova CSRF após mudança de sessão");jar.put("XSRF-TOKEN",new Cookie("XSRF-TOKEN",dados(result).path("token").asText()));}
    private String codigo(String secret){return String.format(java.util.Locale.ROOT,"%06d",totp.obterCodigoAtual(secret));}
    private boolean containsId(JsonNode n,long id){for(var d:n)if(d.path("id").asLong()==id)return true;return false;}
}
