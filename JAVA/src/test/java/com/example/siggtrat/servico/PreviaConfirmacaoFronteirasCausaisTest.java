package com.example.siggtrat.servico;

import com.example.siggtrat.dto.AcaoCicloVidaContratoDTO;
import com.example.siggtrat.entidade.*;
import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;
import java.time.LocalDate;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/** Casos públicos estáveis: gerar prévia e confirmar outro texto; nenhuma chamada privada/reflexão. */
class PreviaConfirmacaoFronteirasCausaisTest {
    @Test void redistribuirSeparadorEntreMotivoEReferenciaNaoReutilizaPreviaAnterior() {
        verificar("Motivo A\u001fReferencia B", "Referencia C", "Motivo A", "Referencia B\u001fReferencia C");
    }
    @Test void referenciaAusenteNaoEquivaleAoLiteralNull() {
        verificar("Motivo documentado", null, "Motivo documentado", "<null>");
    }
    private void verificar(String motivoA, String referenciaA, String motivoB, String referenciaB) {
        var em = mock(EntityManager.class);
        var impacto = mock(AssinaturaImpactoVinculoServico.class);
        consultasVazias(em);
        when(impacto.partesPorContrato(anyLong(), any(), any())).thenReturn(List.of("dependencia-estavel"));
        var servico = new PreviaCicloVidaContratoServico(em, impacto);
        var contrato = mock(Contrato.class); when(contrato.getId()).thenReturn(10L);
        var usuario = mock(UsuarioAplicacao.class); when(usuario.getId()).thenReturn(20L); when(usuario.getUsername()).thenReturn("ator-sintetico");
        var previaA = servico.gerar(contrato, "inativar", acao(motivoA, referenciaA, null, false), usuario);
        assertDoesNotThrow(() -> servico.validarConfirmacao(contrato, "inativar", acao(motivoA, referenciaA, previaA.tokenPrevia(), true), usuario));
        var erro = assertThrows(ResponseStatusException.class, () -> servico.validarConfirmacao(contrato, "inativar",
                acao(motivoB, referenciaB, previaA.tokenPrevia(), true), usuario), "Outra intenção não foi a prévia confirmada");
        assertEquals(409, erro.getStatusCode().value());
        var previaB = servico.gerar(contrato, "inativar", acao(motivoB, referenciaB, null, false), usuario);
        assertNotEquals(previaA.tokenPrevia(), previaB.tokenPrevia());
        assertDoesNotThrow(() -> servico.validarConfirmacao(contrato, "inativar", acao(motivoB, referenciaB, previaB.tokenPrevia(), true), usuario));
    }
    private AcaoCicloVidaContratoDTO acao(String motivo, String referencia, String token, boolean confirmar) {
        return new AcaoCicloVidaContratoDTO(LocalDate.of(2026, 6, 10), LocalDate.of(2026, 6, 9), motivo, referencia,
                null, null, null, token, confirmar);
    }
    @SuppressWarnings("unchecked") private void consultasVazias(EntityManager em) {
        TypedQuery<Long> contagens = mock(TypedQuery.class, RETURNS_SELF); when(contagens.getSingleResult()).thenReturn(0L);
        TypedQuery<VinculoContratual> vinculos = mock(TypedQuery.class, RETURNS_SELF); when(vinculos.getResultList()).thenReturn(List.of());
        TypedQuery<Object[]> linhas = mock(TypedQuery.class, RETURNS_SELF); when(linhas.getResultList()).thenReturn(List.of());
        TypedQuery<LocalDate> datas = mock(TypedQuery.class, RETURNS_SELF); when(datas.getResultList()).thenReturn(List.of());
        when(em.createQuery(anyString(), eq(Long.class))).thenReturn(contagens);
        when(em.createQuery(anyString(), eq(VinculoContratual.class))).thenReturn(vinculos);
        when(em.createQuery(anyString(), eq(Object[].class))).thenReturn(linhas);
        when(em.createQuery(anyString(), eq(LocalDate.class))).thenReturn(datas);
    }
}
