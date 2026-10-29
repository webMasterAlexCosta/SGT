package com.example.siggtrat.etapa10;

import com.example.siggtrat.dominio.acesso.PerfilAcesso;
import com.example.siggtrat.dominio.acesso.TipoPermissaoExcepcional;
import com.example.siggtrat.dto.ConcessaoPermissaoExcepcionalRequisicao;
import com.example.siggtrat.dto.PermissaoExcepcionalDTO;
import com.example.siggtrat.dto.RevogacaoPermissaoExcepcionalRequisicao;
import com.example.siggtrat.entidade.Contrato;
import com.example.siggtrat.entidade.GestaoLocal;
import com.example.siggtrat.entidade.PermissaoExcepcional;
import com.example.siggtrat.entidade.SuperintendenciaEstadual;
import com.example.siggtrat.entidade.UsuarioAplicacao;
import com.example.siggtrat.entidade.UsoPermissaoExcepcional;
import com.example.siggtrat.repositorio.*;
import com.example.siggtrat.servico.AcessoOrganizacionalServico;
import com.example.siggtrat.servico.PermissaoExcepcionalServico;
import com.example.siggtrat.servico.RegistroAuditoriaServico;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class PermissaoExcepcionalServicoEtapa10Test {

    private final PermissaoExcepcionalRepositorio permissoes = mock(PermissaoExcepcionalRepositorio.class);
    private final UsoPermissaoExcepcionalRepositorio usos = mock(UsoPermissaoExcepcionalRepositorio.class);
    private final UsuarioAplicacaoRepositorio usuarios = mock(UsuarioAplicacaoRepositorio.class);
    private final ContratoRepositorio contratos = mock(ContratoRepositorio.class);
    private final GestaoLocalRepositorio gestoes = mock(GestaoLocalRepositorio.class);
    private final AcessoOrganizacionalServico acesso = mock(AcessoOrganizacionalServico.class);
    private final RegistroAuditoriaServico auditoria = mock(RegistroAuditoriaServico.class);
    private final PermissaoExcepcionalServico servico = new PermissaoExcepcionalServico(
            permissoes, usos, usuarios, contratos, gestoes, acesso, auditoria);

    private UsuarioAplicacao administrador;
    private UsuarioAplicacao gestorLocal;
    private GestaoLocal gestao;
    private Contrato contrato;

    @BeforeEach
    void preparar() {
        administrador = usuario(1L, "adm", PerfilAcesso.ADMINISTRADOR);
        gestao = gestao(10L, 20L);
        gestorLocal = usuario(2L, "gestor.local", PerfilAcesso.GESTOR_LOCAL);
        gestorLocal.setGestaoLocal(gestao);
        gestorLocal.setSuperintendencia(gestao.getSuperintendencia());
        contrato = new Contrato();
        contrato.setId(30L);
        contrato.setNumeroContrato("CT-30");
        contrato.setAnoContrato(2026);
        contrato.setGestaoLocal(gestao);

        when(acesso.usuarioAtual()).thenReturn(administrador);
        when(acesso.ehAdministrador(administrador)).thenReturn(true);
        when(usuarios.findById(gestorLocal.getId())).thenReturn(Optional.of(gestorLocal));
        when(contratos.findById(contrato.getId())).thenReturn(Optional.of(contrato));
        when(gestoes.findById(gestao.getId())).thenReturn(Optional.of(gestao));
        when(permissoes.buscarAtivas(anyLong(), any(), any())).thenReturn(List.of());
        when(permissoes.save(any(PermissaoExcepcional.class))).thenAnswer(invocacao -> {
            PermissaoExcepcional permissao = invocacao.getArgument(0);
            permissao.setId(100L);
            return permissao;
        });
    }

    @Test
    void administradorConcedePermissaoTemporariaPessoalEContratual() {
        ConcessaoPermissaoExcepcionalRequisicao requisicao = requisicao(
                TipoPermissaoExcepcional.CORRECAO_IDENTIFICADOR_CONTRATO, gestorLocal.getId());

        PermissaoExcepcionalDTO dto = servico.conceder(requisicao);

        assertEquals(100L, dto.id());
        assertEquals(PerfilAcesso.GESTOR_LOCAL, dto.perfilBeneficiario());
        assertEquals(contrato.getId(), dto.contratoId());
        assertNull(dto.gestaoLocalId(), "o contrato já representa integralmente o escopo da concessão");
        assertTrue(dto.ativa());
        verify(auditoria).registrarAcao(eq("CONCEDER_PERMISSAO_EXCEPCIONAL"),
                eq("PermissaoExcepcional"), eq(100L),
                eq(TipoPermissaoExcepcional.CORRECAO_IDENTIFICADOR_CONTRATO.name()), anyMap());
    }

    @Test
    void rejeitaPermissaoNegocialParaGestorEstadual() {
        UsuarioAplicacao estadual = usuario(3L, "gestor.estado", PerfilAcesso.GESTOR_ESTADUAL);
        estadual.setSuperintendencia(gestao.getSuperintendencia());
        when(usuarios.findById(estadual.getId())).thenReturn(Optional.of(estadual));

        ResponseStatusException erro = assertThrows(ResponseStatusException.class,
                () -> servico.conceder(requisicao(
                        TipoPermissaoExcepcional.TRANSFERENCIA_GESTAO_LOCAL, estadual.getId())));

        assertEquals(HttpStatus.BAD_REQUEST, erro.getStatusCode());
        verify(permissoes, never()).save(any());
    }

    @Test
    void rejeitaConcessaoPorQuemNaoForAdministrador() {
        when(acesso.usuarioAtual()).thenReturn(gestorLocal);
        when(acesso.ehAdministrador(gestorLocal)).thenReturn(false);

        ResponseStatusException erro = assertThrows(ResponseStatusException.class,
                () -> servico.conceder(requisicao(
                        TipoPermissaoExcepcional.CORRECAO_IDENTIFICADOR_CONTRATO,
                        gestorLocal.getId())));

        assertEquals(HttpStatus.FORBIDDEN, erro.getStatusCode());
        verifyNoInteractions(usuarios, contratos, gestoes);
    }

    @Test
    void consumoExigePermissaoVigenteERegistraTrilhaImutavel() {
        PermissaoExcepcional permissao = permissaoAtiva(
                TipoPermissaoExcepcional.CORRECAO_IDENTIFICADOR_CONTRATO, gestorLocal, contrato);
        when(permissoes.buscarAtivas(eq(gestorLocal.getId()),
                eq(TipoPermissaoExcepcional.CORRECAO_IDENTIFICADOR_CONTRATO), any()))
                .thenReturn(List.of(permissao));
        when(usos.save(any(UsoPermissaoExcepcional.class))).thenAnswer(invocacao -> {
            UsoPermissaoExcepcional uso = invocacao.getArgument(0);
            ReflectionTestUtils.setField(uso, "id", 200L);
            return uso;
        });

        PermissaoExcepcional utilizada = servico.exigirERegistrarUso(gestorLocal,
                TipoPermissaoExcepcional.CORRECAO_IDENTIFICADOR_CONTRATO,
                contrato, gestao, "Correção aprovada", "Contrato", contrato.getId());

        assertSame(permissao, utilizada);
        verify(usos).save(argThat(uso -> uso.getPermissao() == permissao
                && uso.getUsuarioId().equals(gestorLocal.getId())
                && uso.getTipo() == TipoPermissaoExcepcional.CORRECAO_IDENTIFICADOR_CONTRATO));
        verify(auditoria).registrarAcao(eq("USAR_PERMISSAO_EXCEPCIONAL"),
                eq("Contrato"), eq(contrato.getId()),
                eq(TipoPermissaoExcepcional.CORRECAO_IDENTIFICADOR_CONTRATO.name()), anyMap());
    }

    @Test
    void bloqueiaConsumoForaDoContratoConcedido() {
        Contrato outro = new Contrato();
        outro.setId(31L);
        outro.setGestaoLocal(gestao);
        PermissaoExcepcional permissao = permissaoAtiva(
                TipoPermissaoExcepcional.CORRECAO_IDENTIFICADOR_CONTRATO, gestorLocal, contrato);
        when(permissoes.buscarAtivas(eq(gestorLocal.getId()), any(), any()))
                .thenReturn(List.of(permissao));

        ResponseStatusException erro = assertThrows(ResponseStatusException.class,
                () -> servico.exigirERegistrarUso(gestorLocal,
                        TipoPermissaoExcepcional.CORRECAO_IDENTIFICADOR_CONTRATO,
                        outro, gestao, "Tentativa", "Contrato", outro.getId()));

        assertEquals(HttpStatus.FORBIDDEN, erro.getStatusCode());
        verifyNoInteractions(usos);
    }

    @Test
    void revogacaoAdministrativaInativaAConcessaoEAuditaOMotivo() {
        PermissaoExcepcional permissao = permissaoAtiva(
                TipoPermissaoExcepcional.CORRECAO_IDENTIFICADOR_CONTRATO, gestorLocal, contrato);
        when(permissoes.findById(100L)).thenReturn(Optional.of(permissao));

        PermissaoExcepcionalDTO dto = servico.revogar(100L,
                new RevogacaoPermissaoExcepcionalRequisicao("Mudança de necessidade"));

        assertFalse(dto.ativa());
        assertEquals("Mudança de necessidade", dto.motivoRevogacao());
        verify(auditoria).registrarAcao(eq("REVOGAR_PERMISSAO_EXCEPCIONAL"),
                eq("PermissaoExcepcional"), eq(100L), anyString(), anyMap());
    }

    private ConcessaoPermissaoExcepcionalRequisicao requisicao(
            TipoPermissaoExcepcional tipo, Long beneficiarioId) {
        return new ConcessaoPermissaoExcepcionalRequisicao(tipo, beneficiarioId,
                contrato.getId(), gestao.getId(), "Execução do ato autorizado",
                "Necessidade excepcional comprovada", "ATO-ADM-2026-001",
                Instant.now().plusSeconds(3600));
    }

    private PermissaoExcepcional permissaoAtiva(TipoPermissaoExcepcional tipo,
                                                  UsuarioAplicacao beneficiario,
                                                  Contrato escopoContrato) {
        PermissaoExcepcional permissao = new PermissaoExcepcional();
        permissao.setId(100L);
        permissao.setTipo(tipo);
        permissao.setUsuarioBeneficiario(beneficiario);
        permissao.setContrato(escopoContrato);
        permissao.setFinalidade("Ato autorizado");
        permissao.setJustificativa("Necessidade comprovada");
        permissao.setAutorizacaoAdministrativa("ATO-ADM-2026-001");
        permissao.setConcedidoPorId(administrador.getId());
        permissao.setConcedidoPorUsername(administrador.getUsername());
        permissao.setValidoAte(Instant.now().plusSeconds(3600));
        permissao.setAtivo(true);
        return permissao;
    }

    private UsuarioAplicacao usuario(Long id, String username, PerfilAcesso perfil) {
        UsuarioAplicacao usuario = new UsuarioAplicacao();
        usuario.setId(id);
        usuario.setUsername(username);
        usuario.setPerfil(perfil);
        usuario.setActive(true);
        return usuario;
    }

    private GestaoLocal gestao(Long id, Long superintendenciaId) {
        SuperintendenciaEstadual superintendencia = new SuperintendenciaEstadual();
        superintendencia.setId(superintendenciaId);
        superintendencia.setAtivo(true);
        GestaoLocal gestaoLocal = new GestaoLocal();
        gestaoLocal.setId(id);
        gestaoLocal.setAtivo(true);
        gestaoLocal.setNome("GTRAT/SP");
        gestaoLocal.setSuperintendencia(superintendencia);
        return gestaoLocal;
    }
}
