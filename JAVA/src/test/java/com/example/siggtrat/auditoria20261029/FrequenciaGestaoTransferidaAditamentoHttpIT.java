package com.example.siggtrat.auditoria20261029;

import com.example.siggtrat.entidade.Frequencia;
import com.example.siggtrat.utilitario.RelogioSistema;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/** CT007 nos consumidores históricos: ADI foi conhecido antes da transferência. */
class FrequenciaGestaoTransferidaAditamentoHttpIT extends ContratosHttpBase {
    @Test
    void frequenciaDepoisDoAditamentoUsaGestaoDoFatoSemRestaurarFotografiaAnterior() throws Exception {
        Preparado f = preparar();
        long id = enviar(f.c().localB, "/api/frequencias", mapa(
                "vinculoContratualId", f.vinculoId(), "dataReferencia", DATA,
                "status", "PRESENCA", "horasTrabalhadas", "04:00", "horarioSaida", "12:00",
                "justificativaRetificacao", "Registro histórico da Gestão que responde na data do fato",
                "chaveIdempotencia", "gestao-apos-adi-" + UUID.randomUUID()),
                200, "TMP-004/PER-010 ADI descritivo não desfaz a Gestão efetiva da transferência").path("id").asLong();
        em.flush(); em.clear();
        Frequencia fonte = em.find(Frequencia.class, id);
        assertEquals(f.c().gestaoB.getId(), fonte.getGestaoLocalSnapshotId());
        assertEquals(f.c().seB.getId(), fonte.getSuperintendenciaSnapshotId());
        assertEquals(f.c().lotacaoB.getId(), fonte.getLotacaoSnapshotId());
        assertEquals(f.c().jornadaB.getId(), fonte.getJornadaSnapshotId());
        assertEquals(240, fonte.getMinutosFisicos());
    }

    @Test
    void relatorioDepoisDoAditamentoPermaneceNoEscopoDestinoDaTransferencia() throws Exception {
        Preparado f = preparar();
        String path = "/api/relatorios/frequencia-consolidada?inicio=" + DATA + "&fim=" + DATA
                + "&contratoIds=" + f.contrato().id();
        JsonNode nacional = consultar(f.c().nacional, path, "REL-001 população canônica do contrato transferido");
        JsonNode origem = consultar(f.c().localA, path, "PER-010 Gestão anterior não conserva o recorte posterior");
        JsonNode destino = consultar(f.c().localB, path, "PER-010 Gestão efetiva recebe o recorte posterior");
        assertAll("A população posterior pertence à Gestão B após o ADI descritivo",
                () -> assertEquals(1, nacional.path("registros").size(), "Controle: uma alocação canônica no contrato"),
                () -> assertEquals(0, origem.path("registros").size(), "Origem não recebe população da Gestão B"),
                () -> assertEquals(1, destino.path("registros").size(), "Destino mantém sua população histórica"));
        JsonNode linha = destino.path("registros").get(0);
        assertEquals(f.c().lotacaoB.getId().longValue(), linha.path("lotacaoId").asLong());
        JsonNode dia = linha.path("diario").path(DATA.toString());
        assertEquals("SEM_LANCAMENTO", dia.path("status").asText());
        assertEquals(480, dia.path("minutosPrevistos").asInt());
    }

    private Preparado preparar() throws Exception {
        Cenario c = criarCenario();
        // Registro e consulta usam PostgreSQL/HTTP reais. O relógio contratual na
        // preparação apenas torna a transferência programada: DATA continua
        // passada para o lançamento real de frequência, sem mockar LocalDate.
        LocalDate momentoPreparacao = DATA.minusDays(10);
        LocalDate dataTransferencia = DATA.minusDays(1);
        NovoContrato contrato;
        long vinculoId;
        try (var relogio = Mockito.mockStatic(RelogioSistema.class, Mockito.CALLS_REAL_METHODS)) {
            relogio.when(RelogioSistema::hoje).thenReturn(momentoPreparacao);
            contrato = publicado(c, 100, null, false);
            JsonNode vinculo = enviar(c.localA, "/api/vinculos-contratuais", mapa(
                    "pessoa", mapa("nome", "Pessoa da transferência seguida por ADI",
                            "cpf", cpf(Math.toIntExact(950000000L + contrato.id()))),
                    "contratoId", contrato.id(), "composicaoId", contrato.composicao1(),
                    "dataInicio", momentoPreparacao),
                    201, "Vínculo criado antes de conhecer o ADI e a transferência");
            vinculoId = vinculo.path("id").asLong();
            var adi = aditamento("ADI-CONHECIDO-ANTES-DA-TRANSFERENCIA", DATA);
            adi.put("novaDescricao", "Descrição posterior que não altera a Gestão Local");
            registrarAditamento(c, contrato.id(), adi);
            conceder(c, contrato.id(), "TRANSFERENCIA_GESTAO_LOCAL");
            JsonNode contratoAtualizado = enviar(c.localA, base(contrato.id()) + "/transferir-gestao", mapa(/* Conferência SINTÉTICA explícita desta fixture; não é padrão do produto. */
                "conferenciaDocumentalConfirmada", true, "versaoConferenciaDocumental", "transferencia-gestao-conferencia-v1", 
                    "dataEfetiva", dataTransferencia, "dataAto", momentoPreparacao,
                    "gestaoLocalDestinoId", c.gestaoB.getId(), "tipoContratacaoDestinoId", c.tipoB.getId(), "motivo", "Transferência anterior ao efeito do ADI já conhecido",
                    "referenciaSei", "SEI-TRANSFERENCIA-ADI-FREQUENCIA", "autorizacaoAdministrativa", "ATO-HTTP",
                    "mapeamentos", List.of(mapa("composicaoOrigemId", contrato.composicao1(),
                            "lotacaoDestinoId", c.lotacaoB.getId(), "jornadaDestinoId", c.jornadaB.getId(),
                            "cargoDestinoId", c.cargoB.getId()))),
                    200, "A transferência real preserva fatos posteriores e mapeia a alocação para B");
            assertEquals(contrato.id(), contratoAtualizado.path("id").asLong());
            JsonNode transferencias = consultar(c.nacional, base(contrato.id()) + "/transferencias-gestao",
                    "A prova da transferência está no recurso de transferências, pois o POST retorna ContratoDTO");
            assertEquals(1, transferencias.size());
            JsonNode transferencia = transferencias.get(0);
            assertEquals(c.gestaoA.getId().longValue(), transferencia.path("gestaoOrigemId").asLong());
            assertEquals(c.gestaoB.getId().longValue(), transferencia.path("gestaoDestinoId").asLong());
            assertEquals(dataTransferencia.toString(), transferencia.path("dataEfetiva").asText());
        }
        em.flush(); em.clear();
        return new Preparado(c, contrato, vinculoId);
    }

    private record Preparado(Cenario c, NovoContrato contrato, long vinculoId) {}
}
