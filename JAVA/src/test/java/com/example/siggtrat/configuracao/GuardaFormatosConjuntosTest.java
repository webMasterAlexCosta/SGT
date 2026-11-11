package com.example.siggtrat.configuracao;

import com.example.siggtrat.entidade.ConfiguracaoSistema;
import com.example.siggtrat.regra.AssinaturaPreviaNegocial;
import com.example.siggtrat.regra.MaterialHashEventos;
import com.example.siggtrat.repositorio.ConfiguracaoSistemaRepositorio;
import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.SimpleTransactionStatus;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/** Uma transação e nenhuma marca antes de validar as duas famílias; não prova rollback JPA real. */
class GuardaFormatosConjuntosTest {
    @ParameterizedTest @ValueSource(strings={"CorrecaoCnpjContrato", "EventoDisponibilidadeCatalogo", "ProvaNomePessoaHistorico", "ProvaMotivoDesligamentoHistorico"})
    void legadoDeQualquerFamilia57RecusaAntesDeGravarMarcador53(String acervo) {
        Ambiente a = ambiente(null, null, acervo);
        var erro = assertThrows(IllegalStateException.class, a.guarda::afterSingletonsInstantiated);
        assertEquals("SGT_HASH_PREVIAS_SEM_MARCADOR_COM_ACERVO", erro.getMessage());
        verify(a.em, never()).persist(any()); verify(a.tm, never()).commit(any()); verify(a.tm).rollback(any());
    }
    @Test void legado53NaoDeixaMarca57() {
        Ambiente a = ambiente(null, null, "EventoVinculoContratual");
        assertThrows(IllegalStateException.class, a.guarda::afterSingletonsInstantiated);
        verify(a.em, never()).persist(any()); verify(a.tm, never()).commit(any()); verify(a.tm).rollback(any());
    }
    @Test void baseVaziaRegistraDoisMarcadoresEmUmUnicoCommit() {
        Ambiente a = ambiente(null, null, null);
        a.guarda.afterSingletonsInstantiated();
        var captura = org.mockito.ArgumentCaptor.forClass(ConfiguracaoSistema.class);
        verify(a.em, times(2)).persist(captura.capture());
        assertEquals(Set.of(MaterialHashEventos.CHAVE_MARCADOR, AssinaturaPreviaNegocial.CHAVE_MARCADOR),
                new HashSet<>(captura.getAllValues().stream().map(ConfiguracaoSistema::getChave).toList()));
        verify(a.tm).getTransaction(any()); verify(a.tm).commit(any()); verify(a.tm, never()).rollback(any());
    }
    @Test void falhaAoPersistirSegundaMarcaReverteMesmaTransacao() {
        Ambiente a = ambiente(null, null, null);
        doAnswer(i -> { if (((ConfiguracaoSistema)i.getArgument(0)).getChave().equals(AssinaturaPreviaNegocial.CHAVE_MARCADOR)) throw new IllegalStateException("fixture-falha"); return null; }).when(a.em).persist(any());
        assertThrows(IllegalStateException.class, a.guarda::afterSingletonsInstantiated);
        verify(a.tm).getTransaction(any()); verify(a.tm).rollback(any()); verify(a.tm, never()).commit(any());
    }
    @Test void marcador57DivergenteNaoGrava53MesmoSemAcervo() {
        Ambiente a = ambiente(null, "OUTRO-FORMATO", null);
        assertThrows(IllegalStateException.class, a.guarda::afterSingletonsInstantiated);
        verify(a.em, never()).persist(any()); verify(a.tm).rollback(any());
    }
    @Test void marcador53DivergenteNaoGrava57MesmoSemAcervo() {
        Ambiente a = ambiente("OUTRO-FORMATO", null, null);
        assertThrows(IllegalStateException.class, a.guarda::afterSingletonsInstantiated);
        verify(a.em, never()).persist(any()); verify(a.tm).rollback(any());
    }
    @Test void formatosCorretosNaoRecalculamNemRegravamAcervo() {
        Ambiente a = ambiente(MaterialHashEventos.FORMATO, AssinaturaPreviaNegocial.FORMATO, "EventoDisponibilidadeCatalogo");
        a.guarda.afterSingletonsInstantiated();
        verify(a.em, never()).createQuery(anyString(), eq(Long.class)); verify(a.em, never()).persist(any());
        verify(a.em, never()).merge(any()); verify(a.em, never()).remove(any()); verify(a.tm).commit(any());
    }
    @Test void apiOperacionalNaoExibeNemEditaMarcadoresTecnicos() {
        var repo = mock(ConfiguracaoSistemaRepositorio.class);
        when(repo.findAll()).thenReturn(List.of(
                new ConfiguracaoSistema(MaterialHashEventos.CHAVE_MARCADOR, MaterialHashEventos.FORMATO, "tecnico"),
                new ConfiguracaoSistema(AssinaturaPreviaNegocial.CHAVE_MARCADOR, AssinaturaPreviaNegocial.FORMATO, "tecnico"),
                new ConfiguracaoSistema("FORMATO_HASH_OUTRA_FAMILIA", "valor", "preservada")));
        var servico = new com.example.siggtrat.servico.ConfiguracaoSistemaServico(repo,
                mock(com.example.siggtrat.configuracao.propriedades.PropriedadesSistema.class), mock(com.example.siggtrat.servico.AcessoOrganizacionalServico.class));
        assertEquals(List.of("FORMATO_HASH_OUTRA_FAMILIA"), servico.listar().stream().map(c -> c.chave()).toList());
        for (String chave : List.of(MaterialHashEventos.CHAVE_MARCADOR, AssinaturaPreviaNegocial.CHAVE_MARCADOR))
            assertThrows(com.example.siggtrat.excecao.ExcecaoValidacaoNegocio.class, () -> servico.salvar(List.of(
                    new com.example.siggtrat.dto.ConfiguracaoSistemaDTO(chave, "OUTRO-FORMATO", "tentativa-sintetica"))));
        verify(repo, never()).saveAll(any());
    }
    @SuppressWarnings("unchecked") private Ambiente ambiente(String eventos, String previas, String acervo) {
        var repo = mock(ConfiguracaoSistemaRepositorio.class); var em = mock(EntityManager.class); var tm = mock(PlatformTransactionManager.class);
        when(tm.getTransaction(any())).thenReturn(new SimpleTransactionStatus());
        when(repo.buscarPorChaveParaAtualizacao(MaterialHashEventos.CHAVE_MARCADOR)).thenReturn(Optional.ofNullable(eventos == null ? null : new ConfiguracaoSistema(MaterialHashEventos.CHAVE_MARCADOR, eventos, "fixture")));
        when(repo.buscarPorChaveParaAtualizacao(AssinaturaPreviaNegocial.CHAVE_MARCADOR)).thenReturn(Optional.ofNullable(previas == null ? null : new ConfiguracaoSistema(AssinaturaPreviaNegocial.CHAVE_MARCADOR, previas, "fixture")));
        when(em.createQuery(anyString(), eq(Long.class))).thenAnswer(i -> {
            TypedQuery<Long> q = mock(TypedQuery.class, RETURNS_SELF);
            when(q.getResultList()).thenReturn(acervo != null && i.getArgument(0).equals("select e.id from " + acervo + " e") ? List.of(1L) : List.of());
            return q;
        });
        return new Ambiente(repo, em, tm, new GuardaFormatoHashEventos(repo, em, tm));
    }
    private record Ambiente(ConfiguracaoSistemaRepositorio repo, EntityManager em, PlatformTransactionManager tm, GuardaFormatoHashEventos guarda) {}
}
