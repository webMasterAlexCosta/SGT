package com.example.siggtrat.etapa11;

import com.example.siggtrat.dominio.relatorio.*;
import com.example.siggtrat.entidade.EmissaoRelatorioAdministrativo;
import com.example.siggtrat.entidade.Frequencia;
import com.example.siggtrat.repositorio.EmissaoRelatorioAdministrativoRepositorio;
import com.example.siggtrat.servico.InvalidacaoEmissaoRelatorioServico;
import com.example.siggtrat.servico.RegistroAuditoriaServico;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class InvalidacaoEmissaoRelatorioServicoEtapa11Test {
    @Test
    void retificacaoSuperaSomenteEmissaoQueContemDataEContrato() {
        EmissaoRelatorioAdministrativo atingida = emissao(1L, LocalDate.of(2026, 8, 1),
                LocalDate.of(2026, 8, 31), "10");
        EmissaoRelatorioAdministrativo outroContrato = emissao(2L, LocalDate.of(2026, 8, 1),
                LocalDate.of(2026, 8, 31), "99");
        EmissaoRelatorioAdministrativo periodoPosterior = emissao(3L, LocalDate.of(2026, 9, 1),
                LocalDate.of(2026, 9, 30), "10");
        EmissaoRelatorioAdministrativoRepositorio repositorio = mock(EmissaoRelatorioAdministrativoRepositorio.class);
        when(repositorio.buscarVigentesAfetaveis(EstadoEmissaoRelatorio.VIGENTE, LocalDate.of(2026, 8, 15)))
                .thenReturn(List.of(atingida, outroContrato, periodoPosterior));
        InvalidacaoEmissaoRelatorioServico servico = new InvalidacaoEmissaoRelatorioServico(
                repositorio, mock(RegistroAuditoriaServico.class));
        Frequencia frequencia = new Frequencia();
        frequencia.setId(44L);
        frequencia.setDataReferencia(LocalDate.of(2026, 8, 15));
        frequencia.setContratoSnapshotId(10L);

        int total = servico.invalidarPorFrequencia(frequencia, "Retificação concluída", null);

        assertThat(total).isEqualTo(1);
        assertThat(atingida.getEstado()).isEqualTo(EstadoEmissaoRelatorio.SUPERADA);
        assertThat(atingida.getCausaSuperacao()).isEqualTo(CausaSuperacaoRelatorio.RETIFICACAO_FONTE);
        assertThat(outroContrato.getEstado()).isEqualTo(EstadoEmissaoRelatorio.VIGENTE);
        assertThat(periodoPosterior.getEstado()).isEqualTo(EstadoEmissaoRelatorio.VIGENTE);
        verify(repositorio).save(atingida);
    }

    private EmissaoRelatorioAdministrativo emissao(Long id, LocalDate inicio, LocalDate fim, String contratos) {
        EmissaoRelatorioAdministrativo e = new EmissaoRelatorioAdministrativo();
        org.springframework.test.util.ReflectionTestUtils.setField(e, "id", id);
        e.setInicio(inicio);
        e.setFim(fim);
        e.setEstado(EstadoEmissaoRelatorio.VIGENTE);
        e.setContratoIdsCsv(contratos);
        e.setTipo(TipoRelatorioAdministrativo.FREQUENCIA_CONSOLIDADA);
        e.setNumeroVersao(1);
        return e;
    }
}
