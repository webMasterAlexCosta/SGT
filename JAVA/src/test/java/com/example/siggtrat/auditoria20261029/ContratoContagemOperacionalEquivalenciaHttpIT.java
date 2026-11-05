package com.example.siggtrat.auditoria20261029;

import com.example.siggtrat.dominio.vinculo.EstadoNegocialVinculo;
import com.example.siggtrat.entidade.EstadoNegocialContrato;
import com.example.siggtrat.repositorio.VinculoContratualRepositorio;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/** Executa os dois JPQL reais: o lote não redefine o significado de vínculo operacional. */
class ContratoContagemOperacionalEquivalenciaHttpIT extends ContratosHttpBase {
    @Autowired private VinculoContratualRepositorio vinculos;

    private record Caso(String nome, EstadoNegocialContrato contrato, boolean ativo,
                        LocalDate inicioContrato, LocalDate fimContrato, LocalDate inicioVinculo,
                        LocalDate fimVinculo, EstadoNegocialVinculo vinculo, long esperado) {}

    @Test
    void consultaAgrupadaPreservaEstadosLimitesInclusivosAusenciasENulosNoMysqlReal() {
        var c = criarCenario();
        LocalDate antes = DATA.minusDays(1), depois = DATA.plusDays(1);
        var publicado = EstadoNegocialContrato.PUBLICADO;
        var registrado = EstadoNegocialVinculo.REGISTRADO;
        List<Caso> casos = List.of(
                new Caso("vigente", publicado, true, antes, depois, antes, depois, registrado, 1),
                new Caso("inicio-exato", publicado, true, DATA, depois, DATA, depois, registrado, 1),
                new Caso("fim-exato", publicado, true, antes, DATA, antes, DATA, registrado, 1),
                new Caso("fim-aberto", publicado, true, antes, null, antes, null, registrado, 1),
                new Caso("vigencia-contrato-ausente", publicado, true, null, null, antes, null, registrado, 1),
                new Caso("contrato-futuro", publicado, true, depois, depois.plusDays(1), antes, null, registrado, 0),
                new Caso("contrato-expirado", publicado, true, antes.minusDays(1), antes, antes, null, registrado, 0),
                new Caso("contrato-inativo", publicado, false, antes, depois, antes, null, registrado, 0),
                new Caso("rascunho", EstadoNegocialContrato.RASCUNHO, true, antes, depois, antes, null, registrado, 0),
                new Caso("contrato-cancelado", EstadoNegocialContrato.CANCELADO, true, antes, depois, antes, null, registrado, 0),
                new Caso("contrato-anulado", EstadoNegocialContrato.ANULADO, true, antes, depois, antes, null, registrado, 0),
                new Caso("vinculo-futuro", publicado, true, antes, depois, depois, null, registrado, 0),
                new Caso("vinculo-expirado", publicado, true, antes.minusDays(1), depois, antes, antes, registrado, 0),
                new Caso("vinculo-cancelado", publicado, true, antes, depois, antes, null, EstadoNegocialVinculo.CANCELADO, 0),
                new Caso("vinculo-anulado", publicado, true, antes, depois, antes, null, EstadoNegocialVinculo.ANULADO, 0));
        Map<Long, Long> esperados = new LinkedHashMap<>();
        String sufixo = UUID.randomUUID().toString().substring(0, 8);
        for (Caso caso : casos) {
            var contrato = contrato(c.gestaoA, c.tipoA, "CONTAGEM-" + sufixo + "-" + caso.nome());
            contrato.setEstadoNegocial(caso.contrato()); contrato.setAtivo(caso.ativo());
            contrato.setDataInicio(caso.inicioContrato()); contrato.setDataFim(caso.fimContrato());
            var vinculo = vinculo(c.pessoaA, contrato);
            vinculo.setDataInicio(caso.inicioVinculo()); vinculo.setDataFim(caso.fimVinculo());
            vinculo.setEstadoNegocial(caso.vinculo());
            esperados.put(contrato.getId(), caso.esperado());
        }
        var vazio = contrato(c.gestaoA, c.tipoA, "CONTAGEM-" + sufixo + "-SEM-VINCULO");
        esperados.put(vazio.getId(), 0L);
        em.flush(); em.clear();

        Map<Long, Long> agrupados = new LinkedHashMap<>();
        for (Object[] linha : vinculos.contarOperacionaisPorContrato(esperados.keySet(), DATA)) {
            agrupados.put(((Number) linha[0]).longValue(), ((Number) linha[1]).longValue());
        }
        assertThat(agrupados.keySet()).isSubsetOf(esperados.keySet());
        esperados.forEach((id, esperado) -> {
            assertThat(vinculos.countOperacionaisNoContrato(id, DATA)).as("Consulta individual do contrato %s", id)
                    .isEqualTo(esperado);
            assertThat(agrupados.getOrDefault(id, 0L)).as("Consulta agrupada do contrato %s", id)
                    .isEqualTo(esperado);
        });
        assertThat(agrupados).doesNotContainKeys(c.contratoA.getId(), c.contratoB.getId());
    }
}
