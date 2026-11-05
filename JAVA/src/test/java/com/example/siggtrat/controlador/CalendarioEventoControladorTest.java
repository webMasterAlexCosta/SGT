package com.example.siggtrat.controlador;

import com.example.siggtrat.configuracao.ConfiguracaoJackson;
import com.example.siggtrat.dto.CalendarioEventoRequisicao;
import com.example.siggtrat.excecao.ExcecaoConflitoDados;
import com.example.siggtrat.entidade.UsuarioAplicacao;
import com.example.siggtrat.entidade.CalendarioEvento;
import com.example.siggtrat.entidade.GestaoLocal;
import com.example.siggtrat.entidade.SuperintendenciaEstadual;
import com.example.siggtrat.dominio.acesso.PerfilAcesso;
import com.example.siggtrat.repositorio.*;
import com.example.siggtrat.servico.CalendarioEventoServico;
import com.example.siggtrat.servico.AcessoOrganizacionalServico;
import com.example.siggtrat.servico.EscopoAcessoUsuarioServico;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class CalendarioEventoControladorTest {

    @Test
    void deveIgnorarCamposDeControleQueNaoPertencemAoComando() throws Exception {
        ObjectMapper mapper = new ConfiguracaoJackson().objectMapper();

        String json = """
                {
                  "id": null,
                  "version": null,
                  "data": "2026-08-01",
                  "tipo": "FERIADO",
                  "descricao": "Teste",
                  "ativo": true
                }
                """;

        CalendarioEventoRequisicao requisicao = mapper.readValue(json, CalendarioEventoRequisicao.class);

        assertEquals(LocalDate.of(2026, 8, 1), requisicao.data());
        assertEquals("FERIADO", requisicao.tipo());
        assertEquals("Teste", requisicao.descricao());
    }

    @Test
    void deveRejeitarCorpoAusenteComMensagemClara() {
        CalendarioEventoRepositorio repositorio = mock(CalendarioEventoRepositorio.class);
        CalendarioEventoServico servico = novoServico(repositorio, mock(AcessoOrganizacionalServico.class));

        IllegalArgumentException erro = assertThrows(IllegalArgumentException.class, () -> servico.criar(null));

        assertEquals("Informe os dados do evento.", erro.getMessage());
        verifyNoInteractions(repositorio);
    }

    @Test
    void deveInformarConflitoQuandoJaExistirEventoNoMesmoEscopo() {
        CalendarioEventoRepositorio repositorio = mock(CalendarioEventoRepositorio.class);
        AcessoOrganizacionalServico acesso = acessoGestorLocal();
        CalendarioEventoServico servico = novoServico(repositorio, acesso);
        CalendarioEventoRequisicao requisicao = novaRequisicao();

        CalendarioEvento existente = new CalendarioEvento();
        existente.setId(9L);
        existente.setData(requisicao.data());
        existente.setTipo("FERIADO");
        existente.setAbrangencia("ESTADUAL");
        existente.setGestaoLocal(gestaoLocal());
        existente.setSuperintendencia(existente.getGestaoLocal().getSuperintendencia());
        when(repositorio.findByData(requisicao.data())).thenReturn(List.of(existente));

        assertThrows(ExcecaoConflitoDados.class, () -> servico.criar(requisicao));
        verify(repositorio, never()).save(any());
    }

    @Test
    void deveNormalizarESalvarEventoValidoSemReceberEntidadeJpa() {
        CalendarioEventoRepositorio repositorio = mock(CalendarioEventoRepositorio.class);
        CalendarioEventoServico servico = novoServico(repositorio, acessoGestorLocal());
        CalendarioEventoRequisicao requisicao = new CalendarioEventoRequisicao(
                LocalDate.of(2026, 8, 1),
                " feriado ",
                "  Dia   local  ",
                "ESTADUAL",
                null,
                null,
                null,
                null,
                null,
                true
        );
        when(repositorio.findByData(requisicao.data())).thenReturn(List.of());
        when(repositorio.save(any(CalendarioEvento.class))).thenAnswer(invocacao -> invocacao.getArgument(0));

        var resposta = servico.criar(requisicao);

        assertNotNull(resposta);
        assertNull(resposta.id());
        assertEquals("FERIADO", resposta.tipo());
        assertEquals("Dia local", resposta.descricao());
        assertEquals("ESTADUAL", resposta.abrangencia());
    }

    private CalendarioEventoServico novoServico(CalendarioEventoRepositorio repositorio,
                                                        AcessoOrganizacionalServico acesso) {
        return new CalendarioEventoServico(
                repositorio,
                mock(LotacaoRepositorio.class),
                acesso,
                new EscopoAcessoUsuarioServico());
    }

    private AcessoOrganizacionalServico acessoGestorLocal() {
        AcessoOrganizacionalServico acesso = mock(AcessoOrganizacionalServico.class);
        UsuarioAplicacao gestor = new UsuarioAplicacao();
        gestor.setPerfil(PerfilAcesso.GESTOR_LOCAL);
        gestor.setGestaoLocal(gestaoLocal());
        when(acesso.usuarioAtual()).thenReturn(gestor);
        when(acesso.podeGerenciarCalendario(gestor)).thenReturn(true);
        when(acesso.ehGestaoLocal(gestor)).thenReturn(true);
        when(acesso.podeAcessarGestaoLocal(gestor, gestor.getGestaoLocal())).thenReturn(true);
        return acesso;
    }

    private GestaoLocal gestaoLocal() {
        SuperintendenciaEstadual superintendencia = new SuperintendenciaEstadual();
        superintendencia.setId(27L);
        superintendencia.setNome("SE/SP");
        superintendencia.setUf("SP");
        superintendencia.setAtivo(true);
        GestaoLocal gestao = new GestaoLocal();
        gestao.setId(2701L);
        gestao.setNome("GTRAT/SP");
        gestao.setAtivo(true);
        gestao.setSuperintendencia(superintendencia);
        return gestao;
    }

    private CalendarioEventoRequisicao novaRequisicao() {
        return new CalendarioEventoRequisicao(
                LocalDate.of(2026, 8, 1),
                "FERIADO",
                null,
                "ESTADUAL",
                null,
                null,
                null,
                null,
                null,
                true
        );
    }
}
