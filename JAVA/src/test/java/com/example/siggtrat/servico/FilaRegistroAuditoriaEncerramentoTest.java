package com.example.siggtrat.servico;

import com.example.siggtrat.configuracao.propriedades.PropriedadesAuditoria;
import com.example.siggtrat.configuracao.propriedades.PropriedadesRegistroAuditoria;
import com.example.siggtrat.entidade.RegistroAuditoria;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.*;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class FilaRegistroAuditoriaEncerramentoTest {
    @TempDir Path temporario;

    @Test
    void encerramentoPersisteSeisLotesCompletosEUltimoLoteParcial() {
        var persistencia = mock(PersistenciaRegistroAuditoriaServico.class);
        var persistidos = new ArrayList<RegistroAuditoria>();
        doAnswer(inv -> {
            List<RegistroAuditoria> lote = inv.getArgument(0);
            assertTrue(lote.size() <= 50);
            persistidos.addAll(lote);
            return null;
        }).when(persistencia).salvarLote(anyList());
        var fila = fila(persistencia, temporario.resolve("auditoria.jsonl"));
        for (long id = 1; id <= 301; id++) fila.enfileirarOuPersistir(evento(id));

        fila.descarregarAoEncerrar();

        assertEquals(301, persistidos.size(), "O shutdown não pode limitar a descarga a cinco lotes");
        assertEquals(301, persistidos.stream().map(RegistroAuditoria::getEntityId).distinct().count());
        verify(persistencia, times(7)).salvarLote(anyList());
    }

    @Test
    void encerramentoPreservaTodosOsLotesEmContingenciaQuandoBancoFalha() throws Exception {
        var persistencia = mock(PersistenciaRegistroAuditoriaServico.class);
        doThrow(new IllegalStateException("Banco indisponível")).when(persistencia).salvarLote(anyList());
        Path contingencia = temporario.resolve("auditoria.jsonl");
        var fila = fila(persistencia, contingencia);
        for (long id = 1; id <= 301; id++) fila.enfileirarOuPersistir(evento(id));

        fila.descarregarAoEncerrar();

        assertTrue(Files.isRegularFile(contingencia), "Todos os registros precisam de destino durável no shutdown");
        var linhas = Files.readAllLines(contingencia);
        assertEquals(301, linhas.size());
        var json = new ObjectMapper();
        var ids = new HashSet<Long>();
        for (String linha : linhas) {
            var registro = json.readTree(linha);
            ids.add(registro.get("entityId").asLong());
            assertEquals("{\"motivo\":\"auditoria integral\"}", registro.get("detailsJson").asText());
        }
        assertEquals(301, ids.size());
    }

    @Test
    void eventoRecebidoDepoisDoEncerramentoUsaPersistenciaDireta() {
        var persistencia = mock(PersistenciaRegistroAuditoriaServico.class);
        var fila = fila(persistencia, temporario.resolve("auditoria.jsonl"));
        fila.descarregarAoEncerrar();
        var tardio = evento(999L);

        fila.enfileirarOuPersistir(tardio);

        verify(persistencia).salvar(tardio);
    }

    @Test
    void falhaNoBancoENaContingenciaESinalizadaSemDescartarLote() throws Exception {
        var persistencia = mock(PersistenciaRegistroAuditoriaServico.class);
        var persistidos = new ArrayList<RegistroAuditoria>();
        doThrow(new IllegalStateException("Banco indisponível")).doAnswer(inv -> {
            List<RegistroAuditoria> lote = inv.getArgument(0);
            persistidos.addAll(lote);
            return null;
        }).when(persistencia).salvarLote(anyList());
        Path arquivoImpedindoDiretorio = temporario.resolve("impedimento");
        Files.writeString(arquivoImpedindoDiretorio, "Arquivo comum, não diretório");
        var fila = fila(persistencia, arquivoImpedindoDiretorio.resolve("auditoria.jsonl"));
        for (long id = 1; id <= 301; id++) fila.enfileirarOuPersistir(evento(id));

        assertThrows(IllegalStateException.class, fila::descarregarAoEncerrar);
        assertTrue(persistidos.isEmpty());

        fila.descarregarAoEncerrar();

        assertEquals(301, persistidos.size(), "O lote que falhou também precisa estar disponível na nova tentativa");
        assertEquals(301, persistidos.stream().map(RegistroAuditoria::getEntityId).distinct().count());
    }

    private FilaRegistroAuditoria fila(PersistenciaRegistroAuditoriaServico persistencia, Path caminho) {
        return new FilaRegistroAuditoria(persistencia,
                new PropriedadesRegistroAuditoria(new PropriedadesRegistroAuditoria.Fila(1000),
                        new PropriedadesRegistroAuditoria.Descarga(50, 1000, 2, 100)),
                new PropriedadesAuditoria("teste", caminho,
                        new PropriedadesAuditoria.ConteudoRequisicao(4096), "(?i).*(senha|token|secret).*"));
    }

    private RegistroAuditoria evento(long id) {
        var evento = new RegistroAuditoria();
        evento.setAction("ALTERAR");
        evento.setEntityType("Contrato");
        evento.setEntityId(id);
        evento.setUsername("gestor.teste");
        evento.setDetailsJson("{\"motivo\":\"auditoria integral\"}");
        return evento;
    }
}
