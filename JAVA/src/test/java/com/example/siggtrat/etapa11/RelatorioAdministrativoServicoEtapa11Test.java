package com.example.siggtrat.etapa11;

import com.example.siggtrat.dominio.acesso.PerfilAcesso;
import com.example.siggtrat.dominio.relatorio.*;
import com.example.siggtrat.dto.*;
import com.example.siggtrat.entidade.EmissaoRelatorioAdministrativo;
import com.example.siggtrat.entidade.UsuarioAplicacao;
import com.example.siggtrat.repositorio.EmissaoRelatorioAdministrativoRepositorio;
import com.example.siggtrat.servico.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class RelatorioAdministrativoServicoEtapa11Test {
    private EmissaoRelatorioAdministrativoRepositorio repositorio;
    private RelatorioFrequenciaConsolidadoServico fonte;
    private PendenciaAdministrativaServico pendencias;
    private AcessoOrganizacionalServico acesso;
    private RelatorioAdministrativoServico servico;
    private UsuarioAplicacao usuario;
    private final AtomicLong ids = new AtomicLong();

    @BeforeEach
    void preparar() {
        repositorio = mock(EmissaoRelatorioAdministrativoRepositorio.class);
        fonte = mock(RelatorioFrequenciaConsolidadoServico.class);
        when(fonte.bloquearFontesParaEmissao(anyCollection())).thenReturn(List.of(10L));
        pendencias = mock(PendenciaAdministrativaServico.class);
        acesso = mock(AcessoOrganizacionalServico.class);
        usuario = new UsuarioAplicacao();
        usuario.setId(9L);
        usuario.setUsername("gestor.nacional");
        usuario.setPerfil(PerfilAcesso.GESTOR_NACIONAL);
        when(acesso.usuarioAtual()).thenReturn(usuario);
        when(acesso.podeAcessarRelatoriosGestao(usuario)).thenReturn(true);
        when(acesso.ehGestorNacional(usuario)).thenReturn(true);
        when(pendencias.avisoRelatorio(any(), any(), anyCollection(), isNull(), isNull()))
                .thenReturn(AvisoRevisaoRelatorioDTO.definitivo());
        when(fonte.gerar(any(), any(), anyCollection(), isNull(), isNull()))
                .thenReturn(new RelatorioFrequenciaConsolidadoDTO(true, "CALC-TESTE",
                        LocalDate.of(2026, 8, 1), LocalDate.of(2026, 8, 31), List.of(10L),
                        List.of(), new RelatorioFrequenciaConsolidadoDTO.Totais(
                        0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0)));
        when(repositorio.findByUsuarioIdAndChaveIdempotencia(anyLong(), anyString())).thenReturn(Optional.empty());
        when(repositorio.saveAndFlush(any())).thenAnswer(invocacao -> {
            EmissaoRelatorioAdministrativo e = invocacao.getArgument(0);
            if (e.getId() == null) ReflectionTestUtils.setField(e, "id", ids.incrementAndGet());
            return e;
        });
        servico = new RelatorioAdministrativoServico(repositorio, fonte, pendencias, acesso,
                mock(RegistroAuditoriaServico.class), new ObjectMapper().findAndRegisterModules(),
                new MinimizadorDadosPessoaisServico(), mock(com.example.siggtrat.servico.InvalidacaoCacheServico.class));
    }

    @Test
    void emissaoOriginalPreservaSnapshotHashAutorPeriodoEPendencias() {
        when(repositorio.findTopByChaveSerieOrderByNumeroVersaoDescIdDesc(anyString()))
                .thenReturn(Optional.empty());

        RelatorioAdministrativoDTO resultado = servico.emitir(requisicao("emissao-1"));

        assertThat(resultado.metadados().id()).isEqualTo(1L);
        assertThat(resultado.metadados().versao()).isEqualTo(1);
        assertThat(resultado.metadados().natureza()).isEqualTo(NaturezaEmissaoRelatorio.ORIGINAL);
        assertThat(resultado.metadados().estado()).isEqualTo(EstadoEmissaoRelatorio.VIGENTE);
        assertThat(resultado.metadados().usuario()).isEqualTo("gestor.nacional");
        assertThat(resultado.metadados().hashConteudo()).hasSize(64);
        assertThat(resultado.conteudo().get("calculoVersao").asText()).isEqualTo("CALC-TESTE");
    }

    @Test
    void novaEmissaoAposInvalidacaoDaFonteEhRetificadaEPreservaAnterior() {
        when(repositorio.findTopByChaveSerieOrderByNumeroVersaoDescIdDesc(anyString()))
                .thenReturn(Optional.empty());
        servico.emitir(requisicao("emissao-1"));
        ArgumentCaptor<EmissaoRelatorioAdministrativo> captor = ArgumentCaptor.forClass(EmissaoRelatorioAdministrativo.class);
        verify(repositorio).saveAndFlush(captor.capture());
        EmissaoRelatorioAdministrativo anterior = captor.getValue();
        anterior.setEstado(EstadoEmissaoRelatorio.SUPERADA);
        anterior.setCausaSuperacao(CausaSuperacaoRelatorio.RETIFICACAO_FONTE);
        anterior.setSuperadaEm(java.time.Instant.now());
        anterior.setMotivoSuperacao("Retificação retroativa do vínculo");
        when(repositorio.findTopByChaveSerieOrderByNumeroVersaoDescIdDesc(anyString()))
                .thenReturn(Optional.of(anterior));

        RelatorioAdministrativoDTO resultado = servico.emitir(requisicao("emissao-2"));

        assertThat(resultado.metadados().versao()).isEqualTo(2);
        assertThat(resultado.metadados().natureza()).isEqualTo(NaturezaEmissaoRelatorio.RETIFICADA);
        assertThat(resultado.metadados().emissaoAnteriorId()).isEqualTo(1L);
        assertThat(resultado.metadados().situacao()).isEqualTo("RETIFICADA");
    }

    @Test
    void consultaPorConhecimentoDevolveSnapshotDaEmissaoExistenteNaqueleInstante() {
        when(repositorio.findTopByChaveSerieOrderByNumeroVersaoDescIdDesc(anyString()))
                .thenReturn(Optional.empty());
        servico.emitir(requisicao("emissao-conhecimento"));
        ArgumentCaptor<EmissaoRelatorioAdministrativo> captor = ArgumentCaptor.forClass(EmissaoRelatorioAdministrativo.class);
        verify(repositorio).saveAndFlush(captor.capture());
        EmissaoRelatorioAdministrativo gravada = captor.getValue();
        when(repositorio.findTopByChaveSerieAndGeradoEmLessThanEqualOrderByGeradoEmDescIdDesc(
                anyString(), any())).thenReturn(Optional.of(gravada));
        EmissaoRelatorioRequisicao consulta = new EmissaoRelatorioRequisicao(
                TipoRelatorioAdministrativo.FREQUENCIA_CONSOLIDADA,
                FormatoEmissaoRelatorio.VISUALIZACAO, LocalDate.of(2026, 8, 1),
                LocalDate.of(2026, 8, 31), List.of(10L), null, null, null);

        RelatorioAdministrativoDTO conhecido = servico.conhecidaAte(
                consulta, gravada.getGeradoEm().plusSeconds(1));

        assertThat(conhecido.metadados().id()).isEqualTo(gravada.getId());
        assertThat(conhecido.conteudo().get("calculoVersao").asText()).isEqualTo("CALC-TESTE");
    }

    @Test
    void presidentePodeConsultarEmissaoAdministrativaNacional() {
        when(repositorio.findTopByChaveSerieOrderByNumeroVersaoDescIdDesc(anyString()))
                .thenReturn(Optional.empty());
        servico.emitir(requisicao("emissao-consulta-presidente"));
        ArgumentCaptor<EmissaoRelatorioAdministrativo> captor =
                ArgumentCaptor.forClass(EmissaoRelatorioAdministrativo.class);
        verify(repositorio).saveAndFlush(captor.capture());
        EmissaoRelatorioAdministrativo gravada = captor.getValue();
        when(repositorio.findById(gravada.getId())).thenReturn(Optional.of(gravada));

        usuario.setPerfil(PerfilAcesso.PRESIDENTE);
        when(acesso.ehGestorNacional(usuario)).thenReturn(false);
        when(acesso.ehPresidente(usuario)).thenReturn(true);

        RelatorioAdministrativoDTO consultada = servico.obter(gravada.getId());

        assertThat(consultada.metadados().id()).isEqualTo(gravada.getId());
        assertThat(consultada.conteudo().get("calculoVersao").asText()).isEqualTo("CALC-TESTE");
    }

    private EmissaoRelatorioRequisicao requisicao(String chave) {
        return new EmissaoRelatorioRequisicao(TipoRelatorioAdministrativo.FREQUENCIA_CONSOLIDADA,
                FormatoEmissaoRelatorio.VISUALIZACAO, LocalDate.of(2026, 8, 1),
                LocalDate.of(2026, 8, 31), List.of(10L), null, null, chave);
    }
}
