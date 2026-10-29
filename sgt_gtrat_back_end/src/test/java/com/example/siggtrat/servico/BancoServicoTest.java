package com.example.siggtrat.servico;

import com.example.siggtrat.dto.BancoDTO;
import com.example.siggtrat.entidade.Banco;
import com.example.siggtrat.repositorio.BancoRepositorio;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BancoServicoTest {

    @Mock
    private BancoRepositorio repositorio;

    @InjectMocks
    private BancoServico servico;

    @Test
    void criaBancoMapeandoDtoParaEntidadeEDtoDeResposta() {
        when(repositorio.save(any(Banco.class))).thenAnswer(invocation -> {
            Banco banco = invocation.getArgument(0);
            banco.setId(10L);
            return banco;
        });

        BancoDTO resposta = servico.criar(new BancoDTO(null, "Banco Teste", "999"));

        assertEquals(10L, resposta.getId());
        assertEquals("Banco Teste", resposta.getNome());
        assertEquals("999", resposta.getCodigo());

        ArgumentCaptor<Banco> captor = ArgumentCaptor.forClass(Banco.class);
        verify(repositorio).save(captor.capture());
        assertEquals("Banco Teste", captor.getValue().getNome());
        assertEquals("999", captor.getValue().getCodigo());
    }

    @Test
    void atualizaBancoExistenteSemCriarOutraEntidade() {
        Banco existente = new Banco("Antigo", "001");
        existente.setId(7L);
        when(repositorio.findById(7L)).thenReturn(Optional.of(existente));
        when(repositorio.save(existente)).thenReturn(existente);

        BancoDTO resposta = servico.atualizar(7L, new BancoDTO(null, "Novo", "002"));

        assertNotNull(resposta);
        assertEquals(7L, resposta.getId());
        assertEquals("Novo", resposta.getNome());
        assertEquals("002", resposta.getCodigo());
        verify(repositorio).save(existente);
    }

    @Test
    void atualizacaoDeBancoInexistenteRetornaNullENaoSalva() {
        when(repositorio.findById(404L)).thenReturn(Optional.empty());

        assertNull(servico.atualizar(404L, new BancoDTO(null, "Novo", "002")));
        verify(repositorio, never()).save(any());
    }

    @Test
    void exclusaoDelegaAoRepositorio() {
        servico.excluir(5L);
        verify(repositorio).deleteById(5L);
    }
}
