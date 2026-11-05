package com.example.siggtrat.frequencia;

import com.example.siggtrat.constante.ConstantesDominio;
import com.example.siggtrat.entidade.*;
import com.example.siggtrat.repositorio.EmpregadoRepositorio;
import com.example.siggtrat.repositorio.StatusEmpregadoRepositorio;
import com.example.siggtrat.servico.ProjecaoOperacionalPessoaServico;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ProjecaoOperacionalPessoaServicoTest {
    @Test
    void criaProjecaoInternaAPartirDoVinculoSemCadastroDuplicadoPeloCliente() {
        EmpregadoRepositorio empregados = mock(EmpregadoRepositorio.class);
        StatusEmpregadoRepositorio status = mock(StatusEmpregadoRepositorio.class);
        StatusEmpregado ativo = new StatusEmpregado(ConstantesDominio.STATUS_ATIVO);
        when(status.findByDescricaoIgnoreCase(ConstantesDominio.STATUS_ATIVO)).thenReturn(Optional.of(ativo));
        when(empregados.findByPessoaCanonicaId(1L)).thenReturn(Optional.empty());
        when(empregados.findByCpf("52998224725")).thenReturn(Optional.empty());
        when(empregados.save(any())).thenAnswer(i -> i.getArgument(0));

        Pessoa pessoa = new Pessoa(); pessoa.setId(1L); pessoa.setNome("Maria"); pessoa.setCpf("52998224725");
        GestaoLocal gestao = new GestaoLocal(); gestao.setId(2L);
        Contrato contrato = new Contrato(); contrato.setId(3L); contrato.setGestaoLocal(gestao); contrato.setAtivo(true);
        VinculoContratual vinculo = new VinculoContratual(); vinculo.setPessoa(pessoa); vinculo.setContrato(contrato);
        vinculo.setDataInicio(LocalDate.of(2026, 1, 1));
        Jornada jornada = new Jornada(); jornada.setGestaoLocal(gestao);
        Lotacao lotacao = new Lotacao(); lotacao.setGestaoLocal(gestao);
        Cargo cargo = new Cargo(); cargo.setGestaoLocal(gestao);
        AlocacaoVinculo alocacao = new AlocacaoVinculo(); alocacao.setJornada(jornada); alocacao.setLotacao(lotacao); alocacao.setCargo(cargo);

        Empregado projetado = new ProjecaoOperacionalPessoaServico(empregados, status).projetar(vinculo, alocacao);

        assertSame(pessoa, projetado.getPessoaCanonica());
        assertEquals("Maria", projetado.getNome());
        assertNull(projetado.getMatricula());
        assertEquals(1, projetado.getContratos().size());
        assertSame(alocacao.getLotacao(), projetado.getLotacao());
    }

    @Test
    void vinculaIdentidadeMasNaoReescreveFotografiaOperacionalExistente() {
        EmpregadoRepositorio empregados = mock(EmpregadoRepositorio.class);
        StatusEmpregadoRepositorio status = mock(StatusEmpregadoRepositorio.class);
        Pessoa pessoa = new Pessoa(); pessoa.setId(1L); pessoa.setNome("Nome atualizado"); pessoa.setCpf("52998224725");
        Lotacao historica = new Lotacao();
        Empregado existente = new Empregado(); existente.setLotacao(historica); existente.setCpf(pessoa.getCpf());
        when(empregados.findByPessoaCanonicaId(1L)).thenReturn(Optional.of(existente));
        when(empregados.save(any())).thenAnswer(i -> i.getArgument(0));
        VinculoContratual vinculo = new VinculoContratual(); vinculo.setPessoa(pessoa);

        Empregado projetado = new ProjecaoOperacionalPessoaServico(empregados, status)
                .projetar(vinculo, new AlocacaoVinculo());

        assertSame(pessoa, projetado.getPessoaCanonica());
        assertSame(historica, projetado.getLotacao());
        assertEquals("Nome atualizado", projetado.getNome());
    }
}
