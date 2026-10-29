package com.example.siggtrat.seguranca;

import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfFilter;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class CsrfSpaContractTest {
    private MockMvc mvc;

    @BeforeEach
    void preparar() {
        CookieCsrfTokenRepository repositorio = CookieCsrfTokenRepository.withHttpOnlyFalse();
        repositorio.setCookiePath("/");
        mvc = MockMvcBuilders.standaloneSetup(new ControladorTeste())
                .addFilters(new CsrfFilter(repositorio)).build();
    }

    @Test
    void spaObtemCookieLegivelEPostExigeHeaderCorrespondente() throws Exception {
        MvcResult emissao = mvc.perform(get("/csrf"))
                .andExpect(status().isOk())
                .andExpect(cookie().exists("XSRF-TOKEN"))
                .andReturn();
        var cookie = emissao.getResponse().getCookie("XSRF-TOKEN");
        assertNotNull(cookie);
        assertFalse(cookie.isHttpOnly());
        assertEquals("/", cookie.getPath());
        String token = emissao.getResponse().getContentAsString();

        mvc.perform(post("/mutacao").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isForbidden());
        mvc.perform(post("/mutacao").cookie(cookie)
                        .header("X-XSRF-TOKEN", token)
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isNoContent());
    }

    @RestController
    static class ControladorTeste {
        @GetMapping("/csrf")
        String csrf(HttpServletRequest request) {
            CsrfToken token = (CsrfToken) request.getAttribute(CsrfToken.class.getName());
            return token.getToken();
        }

        @PostMapping("/mutacao")
        @ResponseStatus(org.springframework.http.HttpStatus.NO_CONTENT)
        void mutacao() {}
    }
}
