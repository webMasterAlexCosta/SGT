package com.example.siggtrat.etapa11;

import com.example.siggtrat.dominio.acesso.PerfilAcesso;
import com.example.siggtrat.dominio.frequencia.EstadoAuditoriaFrequencia;
import com.example.siggtrat.dominio.pendencia.*;
import com.example.siggtrat.entidade.Frequencia;
import com.example.siggtrat.entidade.Contrato;
import com.example.siggtrat.entidade.GestaoLocal;
import com.example.siggtrat.entidade.PendenciaAdministrativa;
import com.example.siggtrat.entidade.Pessoa;
import com.example.siggtrat.entidade.SuperintendenciaEstadual;
import com.example.siggtrat.entidade.UsuarioAplicacao;
import com.example.siggtrat.entidade.VinculoContratual;
import com.example.siggtrat.repositorio.*;
import com.example.siggtrat.servico.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.dao.DataIntegrityViolationException;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class PendenciaAdministrativaServicoEtapa11Test {
    private PendenciaAdministrativaRepositorio repositorio;
    private FrequenciaRepositorio frequencias;
    private VinculoContratualRepositorio vinculos;
    private AcessoOrganizacionalServico acesso;
    private PendenciaAdministrativaServico servico;
    private GestaoContratoTemporalServico gestaoTemporal;
    private MotorTemporalContratoServico motorContrato;

    @BeforeEach
    void preparar() {
        repositorio = mock(PendenciaAdministrativaRepositorio.class);
        frequencias = mock(FrequenciaRepositorio.class);
        vinculos = mock(VinculoContratualRepositorio.class);
        acesso = mock(AcessoOrganizacionalServico.class);
        gestaoTemporal = mock(GestaoContratoTemporalServico.class);
        motorContrato = mock(MotorTemporalContratoServico.class);
        var contratos = mock(ContratoRepositorio.class);
        when(contratos.buscarPorIdParaAtualizacao(anyLong())).thenAnswer(inv -> {
            var contrato = new Contrato();
            contrato.setId(inv.getArgument(0));
            return Optional.of(contrato);
        });
        var escopoProva = mock(ResolvedorEscopoProvaFrequenciaServico.class);
        // Estes testes unitários partem de responsabilidade histórica inalterada.
        // Transferências e releitura transacional são exercitadas no HTTP/PostgreSQL.
        when(escopoProva.realinhar(any(), any())).thenAnswer(inv -> inv.getArgument(0));
        servico = new PendenciaAdministrativaServico(
                repositorio, frequencias, vinculos,
                contratos, acesso, new MinimizadorDadosPessoaisServico(),
                mock(RegistroAuditoriaServico.class), gestaoTemporal, escopoProva,
                mock(ComposicaoContratualTemporalServico.class), mock(EventoCicloVidaContratoRepositorio.class),
                mock(jakarta.persistence.EntityManager.class), motorContrato);
        when(repositorio.saveAndFlush(any())).thenAnswer(invocacao -> invocacao.getArgument(0));
    }

    @Test
    void conflitoDePersistenciaNaoRetornaSucessoDaTransacaoInvalida() {
        Frequencia frequencia = new Frequencia();
        frequencia.setId(77L);
        frequencia.setDataReferencia(LocalDate.of(2026, 8, 14));
        frequencia.setVersaoNegocial(4);
        frequencia.setContratoSnapshotId(10L);
        var concorrente = new PendenciaAdministrativa();
        when(repositorio.findByChaveOrigem("FREQUENCIA:77:VERSAO:4"))
                .thenReturn(Optional.empty(), Optional.of(concorrente));
        var falha = new DataIntegrityViolationException("Conflito de unicidade");
        when(repositorio.saveAndFlush(any())).thenThrow(falha);

        var erro = assertThrows(DataIntegrityViolationException.class,
                () -> servico.registrarRevisaoFrequencia(frequencia, "Impacto temporal"));

        assertSame(falha, erro);
        verify(repositorio, times(1)).findByChaveOrigem("FREQUENCIA:77:VERSAO:4");
    }

    @Test
    void fimNaturalCriaPendenciaAdministrativaPorVinculoAberto() {
        SuperintendenciaEstadual se = mock(SuperintendenciaEstadual.class);
        when(se.getId()).thenReturn(50L);
        GestaoLocal gestao = mock(GestaoLocal.class);
        when(gestao.getId()).thenReturn(40L);
        when(gestao.getSuperintendencia()).thenReturn(se);
        Contrato contrato = mock(Contrato.class);
        when(contrato.getId()).thenReturn(10L);
        when(contrato.getDataFim()).thenReturn(LocalDate.of(2026, 7, 31));
        when(contrato.getNumeroContrato()).thenReturn("123");
        when(contrato.getAnoContrato()).thenReturn(2026);
        when(contrato.getGestaoLocal()).thenReturn(gestao);
        when(gestaoTemporal.gestaoEm(eq(contrato), eq(LocalDate.of(2026, 7, 31)), any())).thenReturn(gestao);
        Pessoa pessoa = mock(Pessoa.class);
        when(pessoa.getId()).thenReturn(60L);
        when(pessoa.getNome()).thenReturn("Pessoa Teste");
        VinculoContratual vinculo = mock(VinculoContratual.class);
        when(vinculo.getId()).thenReturn(70L);
        when(vinculo.getContrato()).thenReturn(contrato);
        when(vinculo.getPessoa()).thenReturn(pessoa);
        when(vinculo.getEstadoNegocial()).thenReturn(com.example.siggtrat.dominio.vinculo.EstadoNegocialVinculo.REGISTRADO);
        when(motorContrato.reconstruirPosicao(eq(contrato), any(), any())).thenReturn(
                new com.example.siggtrat.dto.PosicaoTemporalContratoDTO(10L, LocalDate.of(2026, 8, 1),
                        java.time.Instant.now(), com.example.siggtrat.entidade.EstadoNegocialContrato.PUBLICADO,
                        com.example.siggtrat.entidade.SituacaoTemporalContrato.ENCERRADO,
                        com.example.siggtrat.entidade.HabilitacaoAdministrativaContrato.ATIVO,
                        LocalDate.of(2026, 1, 1), LocalDate.of(2026, 7, 31), null, List.of(), List.of()));
        when(vinculos.buscarCandidatosFimNatural(any(), eq(0L), eq(0L), any())).thenReturn(List.of(vinculo));
        when(repositorio.findByChaveOrigem("FIM_VIGENCIA_VINCULO:70:2026-07-31"))
                .thenReturn(Optional.empty());

        assertThat(servico.sincronizarFimNatural()).isEqualTo(1);

        var captor = org.mockito.ArgumentCaptor.forClass(PendenciaAdministrativa.class);
        verify(repositorio).saveAndFlush(captor.capture());
        assertThat(captor.getValue().getTipo()).isEqualTo(TipoPendencia.FIM_NATURAL_VIGENCIA);
        assertThat(captor.getValue().getEscopoBloqueio()).isEqualTo(EscopoBloqueioPendencia.OPERACAO_VINCULO);
        assertThat(captor.getValue().getContratoFotografia()).isEqualTo("123/2026");
        assertThat(captor.getValue().getGestaoLocalId()).isEqualTo(40L);
    }

    @Test
    void geraPendenciaDeFrequenciaComOrigemVersaoEEscopoRestrito() {
        Frequencia frequencia = new Frequencia();
        frequencia.setId(77L);
        frequencia.setDataReferencia(LocalDate.of(2026, 8, 14));
        frequencia.setVersaoNegocial(4);
        frequencia.setContratoSnapshotId(10L);
        frequencia.setContratoSnapshotNumero("123/2026");
        frequencia.setLotacaoSnapshotId(20L);
        frequencia.setJornadaSnapshotId(30L);
        frequencia.setGestaoLocalSnapshotId(40L);
        frequencia.setSuperintendenciaSnapshotId(50L);
        frequencia.setEmpregadoSnapshotNome("Pessoa Teste");
        when(repositorio.findByChaveOrigem("FREQUENCIA:77:VERSAO:4")).thenReturn(Optional.empty());

        PendenciaAdministrativa item = servico.registrarRevisaoFrequencia(
                frequencia, "Retificação de início do vínculo");

        assertThat(item.getTipo()).isEqualTo(TipoPendencia.RETIFICACAO_FREQUENCIA);
        assertThat(item.getEstado()).isEqualTo(EstadoPendencia.ABERTA);
        assertThat(item.getEscopoBloqueio()).isEqualTo(EscopoBloqueioPendencia.FREQUENCIA_PERIODO);
        assertThat(item.getChaveOrigem()).isEqualTo("FREQUENCIA:77:VERSAO:4");
        assertThat(item.getContratoId()).isEqualTo(10L);
        assertThat(item.getGestaoLocalId()).isEqualTo(40L);
        assertThat(item.getSuperintendenciaId()).isEqualTo(50L);
    }

    @Test
    void consultaDeAvisoNaoMaterializaPendenciasDuranteRelatorio() {
        UsuarioAplicacao usuario = new UsuarioAplicacao();
        usuario.setId(1L);
        usuario.setPerfil(PerfilAcesso.GESTOR_NACIONAL);
        when(acesso.usuarioAtual()).thenReturn(usuario);
        when(acesso.podeAcessarRelatoriosGestao(usuario)).thenReturn(true);
        when(acesso.ehGestorNacional(usuario)).thenReturn(true);
        var frequencia = new Frequencia();
        frequencia.setId(77L);
        frequencia.setDataReferencia(LocalDate.of(2026, 8, 14));
        frequencia.setContratoSnapshotId(10L);
        frequencia.setEstadoAuditoria(EstadoAuditoriaFrequencia.SUJEITA_REVISAO);
        when(frequencias.findByEstadoAuditoriaAndDataReferenciaBetweenOrderByDataReferenciaAscIdAsc(
                eq(EstadoAuditoriaFrequencia.SUJEITA_REVISAO), any(), any())).thenReturn(List.of(frequencia));
        when(repositorio.count(any(Specification.class))).thenReturn(1L);

        var aviso = servico.avisoRelatorio(LocalDate.of(2026, 8, 1), LocalDate.of(2026, 8, 31),
                List.of(10L), null, null);

        assertThat(aviso.sujeitoARevisao()).isTrue();
        verify(repositorio, never()).saveAndFlush(any());
        verify(repositorio, never()).save(any());
    }

    @Test
    void avisoContaSomentePendenciasAtivasNoRecorteAutorizado() {
        UsuarioAplicacao usuario = new UsuarioAplicacao();
        usuario.setId(1L);
        usuario.setPerfil(PerfilAcesso.GESTOR_NACIONAL);
        when(acesso.usuarioAtual()).thenReturn(usuario);
        when(acesso.podeAcessarRelatoriosGestao(usuario)).thenReturn(true);
        when(acesso.ehGestorNacional(usuario)).thenReturn(true);
        when(frequencias.findByEstadoAuditoriaAndDataReferenciaBetweenOrderByDataReferenciaAscIdAsc(
                eq(EstadoAuditoriaFrequencia.SUJEITA_REVISAO), any(), any())).thenReturn(List.of());
        when(repositorio.count(any(Specification.class))).thenReturn(3L);

        var aviso = servico.avisoRelatorio(LocalDate.of(2026, 8, 1), LocalDate.of(2026, 8, 31),
                List.of(10L), 20L, 30L);

        assertThat(aviso.sujeitoARevisao()).isTrue();
        assertThat(aviso.quantidadePendencias()).isEqualTo(3L);
        assertThat(aviso.mensagem()).contains("SUJEITO A REVISÃO", "3 pendências");
    }
}
