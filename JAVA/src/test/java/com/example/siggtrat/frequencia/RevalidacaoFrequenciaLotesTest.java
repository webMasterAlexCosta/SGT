package com.example.siggtrat.frequencia;

import com.example.siggtrat.dominio.frequencia.*;
import com.example.siggtrat.entidade.*;
import com.example.siggtrat.repositorio.FrequenciaRepositorio;
import com.example.siggtrat.servico.*;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class RevalidacaoFrequenciaLotesTest {
    @Test
    void revalidaHistoricoSemCarregarMaisDeUmLoteDeFrequencias() {
        var registros = new ArrayList<Frequencia>();
        var empregado = mock(Empregado.class);
        var maiorConsulta = new AtomicInteger();
        for (long id = 1; id <= 1001; id++) {
            var f = new Frequencia();
            f.setId(id);
            f.setEmpregado(empregado);
            f.setDataReferencia(LocalDate.of(2026, 1, 1));
            registros.add(f);
        }
        var repositorio = mock(FrequenciaRepositorio.class, invocacao -> {
            if (invocacao.getMethod().getName().startsWith("save")) return invocacao.getArgument(0);
            if (List.class.isAssignableFrom(invocacao.getMethod().getReturnType())) {
                var args = invocacao.getArguments();
                int limite = Arrays.stream(args).filter(Pageable.class::isInstance).map(Pageable.class::cast)
                        .mapToInt(Pageable::getPageSize).findFirst().orElse(Integer.MAX_VALUE);
                long ultimoId = args.length >= 4 && args[2] instanceof Number n ? n.longValue() : 0L;
                var lote = registros.stream().filter(f -> f.getId() > ultimoId).limit(limite).toList();
                maiorConsulta.accumulateAndGet(lote.size(), Math::max);
                return lote;
            }
            return RETURNS_DEFAULTS.answer(invocacao);
        });
        var resolvedor = mock(ResolvedorContextoTemporalFrequenciaServico.class);
        when(resolvedor.resolver(any(), any()))
                .thenThrow(new ResponseStatusException(HttpStatus.CONFLICT, "Cobertura removida"));
        var auditoria = mock(AuditoriaFrequenciaServico.class);
        var entityManager = mock(jakarta.persistence.EntityManager.class);
        var servico = new RevalidacaoFrequenciaTemporalServico(repositorio, resolvedor, auditoria, entityManager);

        int alteradas = servico.revalidarPorContrato(10L, LocalDate.of(2026, 1, 1), "Anulação retroativa");

        assertEquals(1001, alteradas);
        assertTrue(registros.stream().allMatch(f -> f.getValidadeOperacional() == ValidadeOperacionalFrequencia.INVALIDA_COBERTURA));
        assertTrue(maiorConsulta.get() <= 500, "A consulta deve respeitar o limite de 500 registros por lote");
        verify(auditoria, times(1001)).registrar(any(), any(), any(), any(), any(), any(), any());
        verify(entityManager, times(3)).flush();
        verify(entityManager, times(1001)).detach(any(Frequencia.class));
        verify(entityManager, never()).clear();
    }
}
