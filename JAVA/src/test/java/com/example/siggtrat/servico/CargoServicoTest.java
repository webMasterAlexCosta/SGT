package com.example.siggtrat.servico;

import com.example.siggtrat.dto.CargoDTO;
import com.example.siggtrat.entidade.Cargo;
import com.example.siggtrat.entidade.GestaoLocal;
import com.example.siggtrat.entidade.UsuarioAplicacao;
import com.example.siggtrat.repositorio.CargoRepositorio;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CargoServicoTest {

    @Mock private CargoRepositorio repositorio;
    @Mock private AcessoOrganizacionalServico acesso;
    @Mock private ProgramacaoFuturaCatalogoServico programacaoFutura;
    @Mock private RegistroAuditoriaServico auditoria;
    @Mock private UsuarioAplicacao usuario;
    @Mock private GestaoLocal gestaoLocal;

    @InjectMocks private CargoServico servico;

    @BeforeEach
    void prepararGestaoLocal() {
        lenient().when(acesso.usuarioAtual()).thenReturn(usuario);
        lenient().when(acesso.ehGestaoLocal(usuario)).thenReturn(true);
        lenient().when(usuario.getGestaoLocal()).thenReturn(gestaoLocal);
        lenient().when(gestaoLocal.getId()).thenReturn(5L);
        lenient().when(gestaoLocal.getAtivo()).thenReturn(true);
    }

    private CargoDTO dto(String nome) {
        return new CargoDTO(null, nome, " 1234-56 ", " descrição ", null,
                5L, null, null, null, null, null);
    }

    @Test
    void criaCargoNormalizandoCamposTextuaisNoEscopoDaGestaoLocal() {
        when(repositorio.bloquearNamespaceDaGestaoLocal(5L)).thenReturn(Optional.of(true));
        when(repositorio.findByGestaoLocalIdAndNomeIgnoreCase(5L, "Analista")).thenReturn(Optional.empty());
        when(repositorio.save(any(Cargo.class))).thenAnswer(invocation -> {
            Cargo cargo = invocation.getArgument(0);
            cargo.setId(11L);
            return cargo;
        });

        CargoDTO resposta = servico.criar(dto("  Analista  "));

        assertEquals(11L, resposta.id());
        assertEquals("Analista", resposta.nome());
        assertEquals("1234-56", resposta.cbo());
        assertEquals("descrição", resposta.descricao());
        assertEquals(5L, resposta.gestaoLocalId());
        assertTrue(resposta.ativo());

        ArgumentCaptor<Cargo> captor = ArgumentCaptor.forClass(Cargo.class);
        verify(repositorio).save(captor.capture());
        assertSame(gestaoLocal, captor.getValue().getGestaoLocal());
    }

    @Test
    void rejeitaCriacaoSemDto() {
        when(repositorio.bloquearNamespaceDaGestaoLocal(5L)).thenReturn(Optional.of(true));
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> servico.criar(null));
        assertTrue(ex.getMessage().contains("cargo"));
        verify(repositorio, never()).save(any());
    }

    @Test
    void atualizacaoInexistenteRetornaNull() {
        when(repositorio.bloquearNamespaceDaGestaoLocal(5L)).thenReturn(Optional.of(true));
        when(repositorio.findById(90L)).thenReturn(Optional.empty());
        assertNull(servico.atualizar(90L, dto("Cargo")));
        verify(repositorio, never()).save(any());
    }

    @Test
    void excluirRetornaFalseQuandoRegistroNaoExiste() {
        when(repositorio.findById(90L)).thenReturn(Optional.empty());
        assertFalse(servico.excluir(90L));
        verify(repositorio, never()).save(any());
        verify(repositorio, never()).deleteById(anyLong());
    }

    @Test
    void excluirInativaSemApagarFisicamente() {
        Cargo cargo = new Cargo();
        cargo.setId(8L);
        cargo.setNome("Analista");
        cargo.setGestaoLocal(gestaoLocal);
        cargo.setAtivo(true);
        when(repositorio.findById(8L)).thenReturn(Optional.of(cargo));
        when(repositorio.save(cargo)).thenReturn(cargo);

        assertTrue(servico.excluir(8L));
        assertFalse(cargo.getAtivo());
        verify(programacaoFutura).exigirSemProgramacaoFutura(
                ProgramacaoFuturaCatalogoServico.TipoCatalogo.CARGO, 8L);
        verify(repositorio).save(cargo);
        verify(repositorio, never()).delete(any(Cargo.class));
        verify(repositorio, never()).deleteById(anyLong());
    }

    @Test
    void mantemCargoAtivoQuandoExisteProgramacaoFutura() {
        Cargo cargo = new Cargo();
        cargo.setId(8L);
        cargo.setNome("Analista");
        cargo.setGestaoLocal(gestaoLocal);
        cargo.setAtivo(true);
        when(repositorio.findById(8L)).thenReturn(Optional.of(cargo));
        doThrow(new ResponseStatusException(HttpStatus.CONFLICT, "programação futura"))
                .when(programacaoFutura).exigirSemProgramacaoFutura(
                        ProgramacaoFuturaCatalogoServico.TipoCatalogo.CARGO, 8L);

        assertThrows(ResponseStatusException.class, () -> servico.excluir(8L));

        assertTrue(cargo.getAtivo());
        verify(repositorio, never()).save(any());
        verifyNoInteractions(auditoria);
    }
}
