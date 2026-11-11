package com.example.siggtrat.diagnostico;
import com.example.siggtrat.configuracao.TratadorGlobalExcecoes;
import com.example.siggtrat.configuracao.RespostaErroCorrelacionadaAdvice;
import com.example.siggtrat.configuracao.FiltroIdentificadorRequisicao;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.test.web.servlet.MockMvc;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
class ErroHttpSeguroTest {
    @RestController static class Cenario {
        @GetMapping("/invalid") String invalido(){throw new IllegalArgumentException("password=SEGREDO_FICTICIO_NAO_HTTP");}
        @GetMapping("/unexpected") String inesperado(){throw new IllegalStateException("jdbc:postgresql://interno SEGREDO_FICTICIO_NAO_HTTP");}
        @GetMapping("/business") String negocio(){throw new com.example.siggtrat.excecao.ExcecaoValidacaoNegocio("Data final deve ser posterior à inicial.");}
        @GetMapping("/unauthenticated") String semAutenticacao(){throw new org.springframework.security.authentication.AuthenticationCredentialsNotFoundException("SEGREDO_FICTICIO_NAO_HTTP");}
        @GetMapping("/protocol") String protocolo(){throw new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.NOT_FOUND,"Recurso não encontrado neste escopo.");}
        @GetMapping("/numeric/{id}") String numero(@org.springframework.web.bind.annotation.PathVariable Long id){return "ok";}
        @GetMapping("/enum") String enumerado(@org.springframework.web.bind.annotation.RequestParam com.example.siggtrat.entidade.FechamentoFrequenciaStatus status){return "ok";}
        @GetMapping("/date") String data(@org.springframework.web.bind.annotation.RequestParam @org.springframework.format.annotation.DateTimeFormat(iso=org.springframework.format.annotation.DateTimeFormat.ISO.DATE) java.time.LocalDate data){return "ok";}
        @GetMapping("/success") java.util.Map<String,Integer> sucesso(){return java.util.Map.of("valor",7);}
        @GetMapping("/duplicate/{constraint}") String duplicado(@org.springframework.web.bind.annotation.PathVariable String constraint) {
            var sql=new java.sql.SQLException("SEGREDO_FICTICIO_NAO_HTTP uk_fornecedor_cnpj uk_frequencia_empregado_data", "23505");
            throw new org.springframework.dao.DataIntegrityViolationException("Falha de persistência",
                    new org.hibernate.exception.ConstraintViolationException("SEGREDO_FICTICIO_NAO_HTTP", sql, constraint));
        }
    }
    @Test void argumentoNaoClassificadoNaoPublicaMensagemTecnica() throws Exception {
        MockMvcBuilders.standaloneSetup(new Cenario()).setControllerAdvice(new TratadorGlobalExcecoes()).build()
            .perform(get("/invalid")).andExpect(status().isBadRequest())
            .andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("SEGREDO_FICTICIO_NAO_HTTP"))));
    }
    @Test void inesperadoRetornaErroSeguroETratado500() throws Exception {
        MockMvcBuilders.standaloneSetup(new Cenario()).setControllerAdvice(new TratadorGlobalExcecoes()).build()
            .perform(get("/unexpected")).andExpect(status().isInternalServerError())
            .andExpect(jsonPath("$.code").value("INTERNAL_OPERATION_FAILED"))
            .andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("SEGREDO_FICTICIO_NAO_HTTP"))));
    }
    private MockMvc mvcCorrelacionado() {
        return MockMvcBuilders.standaloneSetup(new Cenario())
            .setControllerAdvice(new TratadorGlobalExcecoes(),new RespostaErroCorrelacionadaAdvice(new ObjectMapper()))
            .addFilters(new FiltroIdentificadorRequisicao()).build();
    }
    @Test void erroPreservaCorrelacaoHeaderCorpoECodigoSemDadosTecnicos() throws Exception {
        mvcCorrelacionado().perform(get("/invalid").header("X-Request-Id","r-error-1"))
            .andExpect(status().isBadRequest()).andExpect(header().string("X-Request-Id","r-error-1"))
            .andExpect(resultado -> org.junit.jupiter.api.Assertions.assertIterableEquals(java.util.List.of("r-error-1"),
                    resultado.getResponse().getHeaders("X-Request-Id"), "Um único identificador deve chegar ao cliente"))
            .andExpect(jsonPath("$.requestId").value("r-error-1")).andExpect(jsonPath("$.code").value("INVALID_ARGUMENT"))
            .andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("SEGREDO_FICTICIO_NAO_HTTP"))));
    }
    @Test void mensagemDeValidacaoDeliberadamentePublicaPermaneceEspecifica() throws Exception {
        mvcCorrelacionado().perform(get("/business")).andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message").value("Data final deve ser posterior à inicial."));
    }
    @Test void statusDeAutenticacaoEProtocoloNaoVira500() throws Exception {
        var mvc=mvcCorrelacionado();
        mvc.perform(get("/unauthenticated")).andExpect(status().isUnauthorized()).andExpect(jsonPath("$.code").value("AUTHENTICATION_REQUIRED"))
            .andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("SEGREDO_FICTICIO_NAO_HTTP"))));
        mvc.perform(get("/protocol")).andExpect(status().isNotFound()).andExpect(jsonPath("$.message").value("Recurso não encontrado neste escopo."));
    }
    @Test void correlacaoDeErroNaoEnvolveRespostaDeSucesso() throws Exception {
        mvcCorrelacionado().perform(get("/success")).andExpect(status().isOk())
            .andExpect(jsonPath("$.valor").value(7)).andExpect(jsonPath("$.code").doesNotExist()).andExpect(jsonPath("$.requestId").doesNotExist());
    }
    @org.junit.jupiter.params.ParameterizedTest
    @org.junit.jupiter.params.provider.ValueSource(strings={"/numeric/not-a-number","/enum?status=INVALIDO","/date?data=not-a-date"})
    void conversaoMalformadaConserva400SemExporValorOuTipoTecnico(String rota) throws Exception {
        mvcCorrelacionado().perform(get(rota)).andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value("INVALID_PARAMETER"))
            .andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("not-a-"))));
    }

    @Test void cnpjDuplicadoRetornaOrientacaoPublicaEspecificaSemSQL() throws Exception {
        mvcCorrelacionado().perform(get("/duplicate/uk_fornecedor_cnpj"))
            .andExpect(status().isConflict()).andExpect(jsonPath("$.code").value("CNPJ_DUPLICADO"))
            .andExpect(jsonPath("$.message").value("Já existe uma empresa cadastrada com este CNPJ. Verifique o cadastro existente."))
            .andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("SEGREDO_FICTICIO_NAO_HTTP"))));
    }
    @Test void restricaoDeFrequenciaConhecidaMantemCodigoPublicoEspecifico() throws Exception {
        mvcCorrelacionado().perform(get("/duplicate/uk_frequencia_empregado_data"))
            .andExpect(status().isConflict()).andExpect(jsonPath("$.code").value("FREQUENCIA_DUPLICADA"))
            .andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("SEGREDO_FICTICIO_NAO_HTTP"))));
    }
    @Test void nomesConhecidosDentroDeMensagemTecnicaNaoClassificamOutraRestricao() throws Exception {
        mvcCorrelacionado().perform(get("/duplicate/uk_desconhecida"))
            .andExpect(status().isConflict()).andExpect(jsonPath("$.code").value("HTTP_409"))
            .andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("CNPJ_DUPLICADO"))))
            .andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("FREQUENCIA_DUPLICADA"))))
            .andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("SEGREDO_FICTICIO_NAO_HTTP"))));
    }
}
