package com.example.siggtrat.etapa10;

import com.example.siggtrat.dominio.acesso.PerfilAcesso;
import com.example.siggtrat.dominio.acesso.TipoPermissaoExcepcional;
import com.example.siggtrat.dto.CorrecaoIdentificadorContratoDTO;
import com.example.siggtrat.dto.CorrecaoIdentificadorContratoRequisicao;
import com.example.siggtrat.entidade.*;
import com.example.siggtrat.repositorio.ContratoRepositorio;
import com.example.siggtrat.repositorio.CorrecaoIdentificadorContratoRepositorio;
import com.example.siggtrat.servico.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class CorrecaoIdentificadorContratoServicoEtapa10Test {

    private final ContratoRepositorio contratos = mock(ContratoRepositorio.class);
    private final CorrecaoIdentificadorContratoRepositorio correcoes = mock(CorrecaoIdentificadorContratoRepositorio.class);
    private final AcessoOrganizacionalServico acesso = mock(AcessoOrganizacionalServico.class);
    private final PermissaoExcepcionalServico permissoes = mock(PermissaoExcepcionalServico.class);
    private final MotorTemporalContratoServico motor = mock(MotorTemporalContratoServico.class);
    private final DocumentoComprobatorioServico documentos = mock(DocumentoComprobatorioServico.class);
    private final RegistroAuditoriaServico auditoria = mock(RegistroAuditoriaServico.class);
    private final CorrecaoIdentificadorContratoServico servico = new CorrecaoIdentificadorContratoServico(
            contratos, correcoes, acesso, permissoes, motor, documentos, auditoria);

    private UsuarioAplicacao gestor;
    private Contrato contrato;

    @BeforeEach
    void preparar() {
        SuperintendenciaEstadual superintendencia = new SuperintendenciaEstadual();
        superintendencia.setId(1L);
        superintendencia.setAtivo(true);
        GestaoLocal gestao = new GestaoLocal();
        gestao.setId(10L);
        gestao.setAtivo(true);
        gestao.setSuperintendencia(superintendencia);

        gestor = new UsuarioAplicacao();
        gestor.setId(20L);
        gestor.setUsername("gestor.local");
        gestor.setPerfil(PerfilAcesso.GESTOR_LOCAL);
        gestor.setGestaoLocal(gestao);

        contrato = new Contrato();
        contrato.setId(30L);
        contrato.setNumeroContrato("CT-ANTIGO");
        contrato.setAnoContrato(2025);
        contrato.setGestaoLocal(gestao);
        contrato.setEstadoNegocial(EstadoNegocialContrato.PUBLICADO);

        when(acesso.usuarioAtual()).thenReturn(gestor);
        when(acesso.ehGestaoLocal(gestor)).thenReturn(true);
        when(correcoes.findByChaveIdempotencia(anyString())).thenReturn(Optional.empty());
        when(contratos.buscarPorIdParaAtualizacao(contrato.getId())).thenReturn(Optional.of(contrato));
        when(contratos.findByNumeroContratoIgnoreCaseAndAnoContrato(anyString(), anyInt()))
                .thenReturn(Optional.empty());
    }

    @Test
    void bloqueiaCorrecaoSemPermissaoEspecificaVigente() {
        when(permissoes.exigirERegistrarUso(eq(gestor),
                eq(TipoPermissaoExcepcional.CORRECAO_IDENTIFICADOR_CONTRATO),
                eq(contrato), eq(contrato.getGestaoLocal()), anyString(), eq("Contrato"), eq(30L)))
                .thenThrow(new ResponseStatusException(HttpStatus.FORBIDDEN, "Permissão ausente"));

        ResponseStatusException erro = assertThrows(ResponseStatusException.class,
                () -> servico.corrigir(30L, requisicao("ATO-ADM-1")));

        assertEquals(HttpStatus.FORBIDDEN, erro.getStatusCode());
        assertEquals("CT-ANTIGO", contrato.getNumeroContrato());
        verifyNoInteractions(motor, auditoria);
        verify(correcoes, never()).save(any());
    }

    @Test
    void rejeitaReferenciaAdministrativaDiferenteDaConcessao() {
        PermissaoExcepcional permissao = permissao("ATO-ADM-OFICIAL");
        when(permissoes.exigirERegistrarUso(any(), any(), any(), any(), anyString(), anyString(), anyLong()))
                .thenReturn(permissao);

        ResponseStatusException erro = assertThrows(ResponseStatusException.class,
                () -> servico.corrigir(30L, requisicao("ATO-ADM-INFORMADO-PELO-CLIENTE")));

        assertEquals(HttpStatus.CONFLICT, erro.getStatusCode());
        assertEquals("CT-ANTIGO", contrato.getNumeroContrato());
        verifyNoInteractions(motor, auditoria);
        verify(correcoes, never()).save(any());
    }

    @Test
    void corrigeVersionaMantemHistoricoEAuditaComAutorizacaoConcedida() {
        PermissaoExcepcional permissao = permissao("ATO-ADM-1");
        when(permissoes.exigirERegistrarUso(any(), any(), any(), any(), anyString(), anyString(), anyLong()))
                .thenReturn(permissao);
        EventoCicloVidaContrato evento = new EventoCicloVidaContrato();
        ReflectionTestUtils.setField(evento, "id", 40L);
        when(motor.registrarEvento(eq(contrato), eq(TipoEventoCicloVidaContrato.CORRECAO_IDENTIFICADOR),
                any(), eq(gestor), eq("CT-ANTIGO/2025"), eq("CT-NOVO/2026"))).thenReturn(evento);
        when(correcoes.save(any(CorrecaoIdentificadorContrato.class))).thenAnswer(invocacao -> {
            CorrecaoIdentificadorContrato correcao = invocacao.getArgument(0);
            ReflectionTestUtils.setField(correcao, "id", 50L);
            return correcao;
        });

        CorrecaoIdentificadorContratoDTO dto = servico.corrigir(30L, requisicao("ATO-ADM-1"));

        assertEquals("CT-NOVO", contrato.getNumeroContrato());
        assertEquals(2026, contrato.getAnoContrato());
        assertEquals("CT-ANTIGO", dto.numeroAnterior());
        assertEquals("CT-NOVO", dto.numeroNovo());
        assertEquals(100L, dto.permissaoId());
        assertEquals("ATO-ADM-1", dto.autorizacaoAdministrativa());
        verify(motor).registrarFotografia(contrato, evento, LocalDate.of(2026, 2, 1));
        verify(documentos).publicarFontesDoFato(eq(contrato), any(), anyString(), eq(50L),
                eq("SEI-123"), eq(LocalDate.of(2026, 2, 1)), eq(gestor));
        verify(auditoria).registrarAcao(eq("CORRIGIR_IDENTIFICADOR_CONTRATO"),
                eq("Contrato"), eq(30L), eq("CT-NOVO/2026"), anyMap());
    }

    private PermissaoExcepcional permissao(String autorizacao) {
        PermissaoExcepcional permissao = new PermissaoExcepcional();
        permissao.setId(100L);
        permissao.setAutorizacaoAdministrativa(autorizacao);
        return permissao;
    }

    private CorrecaoIdentificadorContratoRequisicao requisicao(String autorizacao) {
        return new CorrecaoIdentificadorContratoRequisicao("CT-NOVO", 2026,
                LocalDate.of(2026, 2, 1), LocalDate.of(2026, 2, 2),
                "Correção documental", "SEI-123", autorizacao,
                "idem-correcao-30", null, null, null);
    }
}
