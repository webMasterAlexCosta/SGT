package com.example.siggtrat.etapa06;

import com.example.siggtrat.dominio.vinculo.*;
import com.example.siggtrat.entidade.*;
import com.example.siggtrat.servico.PessoaServico;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.*;

class PessoaVinculoEtapa06Test {
    @Test void cpfValidoEhNormalizadoSemPontuacao() {
        assertEquals("52998224725", PessoaServico.normalizarCpf("529.982.247-25"));
    }
    @Test void cpfInvalidoEhBloqueado() {
        assertThrows(ResponseStatusException.class, () -> PessoaServico.normalizarCpf("111.111.111-11"));
    }
    @Test void cadastroPermanenteNaoPossuiDadosBancariosNemMatricula() {
        var nomes = Arrays.stream(Pessoa.class.getDeclaredFields()).map(java.lang.reflect.Field::getName).toList();
        assertFalse(nomes.contains("conta")); assertFalse(nomes.contains("banco")); assertFalse(nomes.contains("matricula"));
    }
    @Test void obrigatoriedadeDaMatriculaPodeSerHerdadaDoTipo() {
        TipoContratacao tipo = new TipoContratacao(); tipo.setMatriculaVinculoObrigatoria(true);
        Contrato contrato = new Contrato(); contrato.setTipoContratacao(tipo);
        assertTrue(contrato.exigeMatriculaNoVinculo());
    }
    @Test void contratoPodeSobrescreverRegraDeMatriculaDoTipo() {
        TipoContratacao tipo = new TipoContratacao(); tipo.setMatriculaVinculoObrigatoria(true);
        Contrato contrato = new Contrato(); contrato.setTipoContratacao(tipo); contrato.setMatriculaVinculoObrigatoria(false);
        assertFalse(contrato.exigeMatriculaNoVinculo());
    }
    @Test void fimDoVinculoEhInclusivo() {
        VinculoContratual v = new VinculoContratual();
        v.setDataInicio(LocalDate.of(2026, 1, 1)); v.setDataFim(LocalDate.of(2026, 1, 31));
        v.setEstadoNegocial(EstadoNegocialVinculo.REGISTRADO);
        assertEquals(SituacaoTemporalVinculo.VIGENTE, v.situacaoEm(LocalDate.of(2026, 1, 31)));
        assertEquals(SituacaoTemporalVinculo.ENCERRADO, v.situacaoEm(LocalDate.of(2026, 2, 1)));
    }
}
