package com.example.siggtrat.controlador;

import com.example.siggtrat.dto.BancoDTO;
import com.example.siggtrat.servico.BancoServico;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.standaloneSetup;

@ExtendWith(MockitoExtension.class)
class BancoControladorMockMvcTest {

    @Mock
    private BancoServico servico;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = standaloneSetup(new BancoControlador(servico)).build();
    }

    @Test
    void getPorIdExpõeContratoHttpEJsonDoBanco() throws Exception {
        when(servico.buscarPorId(1L)).thenReturn(new BancoDTO(1L, "Banco Teste", "001"));

        mockMvc.perform(get("/api/bancos/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.nome").value("Banco Teste"))
                .andExpect(jsonPath("$.codigo").value("001"));
    }

    @Test
    void getPorIdRetorna404QuandoServiceNaoEncontrarRegistro() throws Exception {
        when(servico.buscarPorId(404L)).thenReturn(null);

        mockMvc.perform(get("/api/bancos/404"))
                .andExpect(status().isNotFound());
    }
}
