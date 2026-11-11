package com.example.siggtrat.diagnostico;

import com.example.siggtrat.configuracao.*;
import com.example.siggtrat.configuracao.propriedades.PropriedadesLimiteRequisicoes;
import com.example.siggtrat.controlador.*;
import com.example.siggtrat.seguranca.*;
import com.example.siggtrat.servico.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/** Controles de corpo/status/correlação; mocks isolados, sem alegação de autorização ou TCP. */
class ErrosSemCorpoEFiltrosTest {
    private final ObjectMapper mapper=new ObjectMapper();

    @ParameterizedTest @ValueSource(strings={"cargos","jornadas","lotacoes","status-empregados","registros-auditoria"})
    void registroAusenteTemMensagemECodigoSemDistinguirForaDoEscopo(String recurso) throws Exception {
        MockMvcBuilders.standaloneSetup(new CargoControlador(mock(CargoServico.class)),
                new JornadaControlador(mock(JornadaServico.class)),new LotacaoControlador(mock(LotacaoServico.class)),
                new StatusEmpregadoControlador(mock(StatusEmpregadoServico.class)),
                new RegistroAuditoriaControlador(mock(ConsultaRegistroAuditoriaServico.class)))
            .setControllerAdvice(new TratadorGlobalExcecoes(),new RespostaErroCorrelacionadaAdvice(mapper))
            .addFilters(new FiltroIdentificadorRequisicao()).build()
            .perform(get("/api/"+recurso+"/999999").header("X-Request-Id","r-ausente-1"))
            .andExpect(status().isNotFound()).andExpect(jsonPath("$.status").value(404))
            .andExpect(jsonPath("$.requestId").value("r-ausente-1"))
            .andExpect(jsonPath("$.code").isNotEmpty()).andExpect(jsonPath("$.message").isNotEmpty());
    }
    @Test void acessoNegadoPeloFiltroTemCorpoCorrelacionadoSemMensagemDaExcecao() throws Exception {
        var req=request("/api/cargos","r-negado-1");var res=new MockHttpServletResponse();
        new FiltroIdentificadorRequisicao().doFilter(req,res,(r,s)->new TratadorAcessoNegadoRest()
                .handle(req,res,new AccessDeniedException("SEGREDO_FICTICIO_NAO_PUBLICAR")));
        verificar(res,403,"r-negado-1");
    }
    @Test void entradaNaoAutenticadaPreservaAcaoEAdicionaCorrelacao() throws Exception {
        var req=request("/api/cargos","r-sem-sessao-1");var res=new MockHttpServletResponse();
        new FiltroIdentificadorRequisicao().doFilter(req,res,(r,s)->new PontoEntradaAutenticacaoRest(mapper)
                .commence(req,res,new BadCredentialsException("SEGREDO_FICTICIO_NAO_PUBLICAR")));
        verificar(res,401,"r-sem-sessao-1");
        assertEquals("LOGOUT",mapper.readTree(res.getContentAsString()).path("action").asText());
        assertTrue(res.getHeader("Cache-Control").contains("no-store"));
    }
    @Test void limiteExcedidoRetorna429ECorrelacao() throws Exception {
        var filtro=new FiltroLimitacaoRequisicoes(new PropriedadesLimiteRequisicoes(1,3600,3600000,3600000));
        filtrar(filtro,request("/api/cargos","r-primeira-1"));
        verificar(filtrar(filtro,request("/api/cargos","r-limite-1")),429,"r-limite-1");
    }
    @Test void cabecalhoDeProxyNaoConfiavelNaoReiniciaLimiteDoMesmoCliente() throws Exception {
        var filtro=new FiltroLimitacaoRequisicoes(new PropriedadesLimiteRequisicoes(1,3600,3600000,3600000));
        var a=request("/api/cargos","r-xff-1");a.addHeader("X-Forwarded-For","198.51.100.1");
        var b=request("/api/cargos","r-xff-2");b.addHeader("X-Forwarded-For","198.51.100.2");
        assertEquals(200,filtrar(filtro,a).getStatus());
        verificar(filtrar(filtro,b),429,"r-xff-2");
    }
    private MockHttpServletRequest request(String path,String id) {
        var req=new MockHttpServletRequest("GET",path);req.setRemoteAddr("127.0.0.1");req.addHeader("X-Request-Id",id);return req;
    }
    private MockHttpServletResponse filtrar(FiltroLimitacaoRequisicoes filtro,MockHttpServletRequest req) throws Exception {
        var res=new MockHttpServletResponse();new FiltroIdentificadorRequisicao().doFilter(req,res,
                (r,s)->filtro.doFilter(r,s,(a,b)->{}));return res;
    }
    private void verificar(MockHttpServletResponse res,int status,String id) throws Exception {
        assertEquals(status,res.getStatus());var body=mapper.readTree(res.getContentAsString());
        assertEquals(status,body.path("status").asInt());assertEquals(id,res.getHeader("X-Request-Id"));
        assertEquals(id,body.path("requestId").asText());assertFalse(body.path("code").asText().isBlank());
        assertFalse(body.path("message").asText().isBlank());
        assertFalse(res.getContentAsString().contains("SEGREDO_FICTICIO_NAO_PUBLICAR"));
    }
}
