package com.example.siggtrat.etapa06;

import com.example.siggtrat.dto.*;
import com.example.siggtrat.entidade.*;
import com.example.siggtrat.repositorio.*;
import com.example.siggtrat.servico.*;
import org.junit.jupiter.api.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class PessoaServicoEtapa06Test {
    private PessoaRepositorio pessoas;
    private VinculoContratualRepositorio vinculos;
    private AcessoOrganizacionalServico acesso;
    private PessoaServico servico;
    private UsuarioAplicacao usuario;

    @BeforeEach void preparar() {
        pessoas = mock(PessoaRepositorio.class); acesso = mock(AcessoOrganizacionalServico.class);
        vinculos = mock(VinculoContratualRepositorio.class);
        servico = new PessoaServico(pessoas, mock(CorrecaoPessoaRepositorio.class),
                vinculos, acesso,
                new MinimizadorDadosPessoaisServico(), mock(PermissaoExcepcionalServico.class));
        usuario = new UsuarioAplicacao(); usuario.setId(9L); usuario.setUsername("gestor");
        usuario.setPerfil(com.example.siggtrat.dominio.acesso.PerfilAcesso.GESTOR_LOCAL);
        doNothing().when(acesso).exigirGestaoEmpregado(any());
        when(pessoas.save(any())).thenAnswer(i -> i.getArgument(0));
    }

    @Test void duplicidadeDeCpfBloqueiaCriacaoEFusaoAutomatica() {
        Pessoa existente = new Pessoa(); existente.setId(1L);
        when(pessoas.findByCpf("52998224725")).thenReturn(Optional.of(existente));
        var req = new CadastroPessoaRequisicao("Maria", "529.982.247-25", null, null, null, null, null, null);
        assertThrows(ResponseStatusException.class, () -> servico.criarParaVinculo(req, usuario));
    }

    @Test void cadastroAceitaCamposOpcionaisOmitidos() {
        when(pessoas.findByCpf("52998224725")).thenReturn(Optional.empty());
        Pessoa p = servico.criarParaVinculo(new CadastroPessoaRequisicao("  Maria  Silva ", "52998224725", null, null, null, null, null, null), usuario);
        assertEquals("Maria Silva", p.getNome()); assertNull(p.getRg()); assertNull(p.getEndereco());
    }

    @Test void omissaoNaAtualizacaoMantemValorELimpezaExigeIndicador() {
        Pessoa p = new Pessoa(); p.setId(1L); p.setNome("Maria"); p.setCpf("52998224725"); p.setRg("123");
        GestaoLocal gestao = new GestaoLocal(); gestao.setId(7L); gestao.setAtivo(true); usuario.setGestaoLocal(gestao);
        Contrato contrato = new Contrato(); contrato.setId(8L); contrato.setGestaoLocal(gestao);
        VinculoContratual vinculo = new VinculoContratual(); vinculo.setPessoa(p); vinculo.setContrato(contrato);
        when(acesso.usuarioAtual()).thenReturn(usuario);
        when(vinculos.findByPessoaIdOrderByDataInicioAscIdAsc(1L)).thenReturn(List.of(vinculo));
        when(pessoas.findById(1L)).thenReturn(Optional.of(p));
        var manter = new AtualizacaoPessoaOpcionalRequisicao(null, false, null, false, null, false, null, false, null, false, null, false);
        assertEquals("123", servico.atualizarOpcionais(1L, manter).rg());
        var limpar = new AtualizacaoPessoaOpcionalRequisicao(null, true, null, false, null, false, null, false, null, false, null, false);
        assertNull(servico.atualizarOpcionais(1L, limpar).rg());
    }
}
