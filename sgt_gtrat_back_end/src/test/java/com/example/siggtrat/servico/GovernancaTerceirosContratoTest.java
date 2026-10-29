package com.example.siggtrat.servico;

import com.example.siggtrat.entidade.Contrato;
import com.example.siggtrat.entidade.EstadoNegocialContrato;
import com.example.siggtrat.entidade.Fornecedor;
import com.example.siggtrat.entidade.UsuarioAplicacao;
import com.example.siggtrat.repositorio.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GovernancaTerceirosContratoTest {

    @Mock private FornecedorRepositorio fornecedorRepositorio;
    @Mock private TabelaTarifariaRepositorio tabelaTarifariaRepositorio;
    @Mock private ProcessoTerceiroRepositorio processoRepositorio;
    @Mock private FolhaHorasRepositorio folhaHorasRepositorio;
    @Mock private FaturaRepositorio faturaRepositorio;
    @Mock private AvaliacaoRiscoFornecedorRepositorio riscoRepositorio;
    @Mock private ContratoRepositorio contratoRepositorio;
    @Mock private CargoRepositorio cargoRepositorio;
    @Mock private EmpregadoRepositorio empregadoRepositorio;
    @Mock private AcessoOrganizacionalServico acessoOrganizacionalServico;
    @Mock private RelatorioFrequenciaConsolidadoServico relatorioFrequenciaServico;
    @Mock private UsuarioAplicacao usuario;

    @InjectMocks private GovernancaTerceirosServico servico;

    @Test
    void contratoPublicadoNaoPermiteTrocarFornecedorForaDeAditamento() {
        Contrato contrato = new Contrato();
        contrato.setId(10L);
        contrato.setEstadoNegocial(EstadoNegocialContrato.PUBLICADO);
        when(acessoOrganizacionalServico.usuarioAtual()).thenReturn(usuario);
        when(contratoRepositorio.findById(10L)).thenReturn(Optional.of(contrato));

        ResponseStatusException erro = assertThrows(ResponseStatusException.class,
                () -> servico.vincularFornecedor(10L, 20L));

        assertEquals(409, erro.getStatusCode().value());
        verify(acessoOrganizacionalServico).exigirGestaoContrato(usuario);
        verify(fornecedorRepositorio, never()).findById(anyLong());
        verify(contratoRepositorio, never()).save(any());
    }

    @Test
    void contratoRascunhoPermiteSelecionarFornecedor() {
        Contrato contrato = new Contrato();
        contrato.setId(10L);
        contrato.setEstadoNegocial(EstadoNegocialContrato.RASCUNHO);
        Fornecedor fornecedor = new Fornecedor();
        fornecedor.setId(20L);
        fornecedor.setRazaoSocial("EMPRESA TESTE");
        fornecedor.setCnpj("12345678000199");

        when(acessoOrganizacionalServico.usuarioAtual()).thenReturn(usuario);
        when(contratoRepositorio.findById(10L)).thenReturn(Optional.of(contrato));
        when(fornecedorRepositorio.findById(20L)).thenReturn(Optional.of(fornecedor));
        when(contratoRepositorio.save(contrato)).thenReturn(contrato);

        var resposta = servico.vincularFornecedor(10L, 20L);

        assertSame(fornecedor, contrato.getFornecedor());
        assertEquals(10L, resposta.contratoId());
        verify(acessoOrganizacionalServico).exigirGestaoContrato(usuario);
        verify(contratoRepositorio).save(contrato);
    }
}
