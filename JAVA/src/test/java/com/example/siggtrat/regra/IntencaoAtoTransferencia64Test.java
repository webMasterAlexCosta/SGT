package com.example.siggtrat.regra;

import org.junit.jupiter.api.Test;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

/** Vetores puros sintéticos; não substituem HTTP/JPA/commit. */
class IntencaoAtoTransferencia64Test {
    @Test void fronteirasNullUnicodeETiposNaoColidem() {
        var a = new LinkedHashMap<String, Object>(); a.put("ato", "a|b"); a.put("sei", "c");
        var b = new LinkedHashMap<String, Object>(); b.put("ato", "a"); b.put("sei", "b|c");
        assertNotEquals(IntencaoAtoTransferencia.capturar(a).hash(), IntencaoAtoTransferencia.capturar(b).hash());
        a.put("ato", null); b.put("ato", "null"); b.put("sei", "c");
        assertNotEquals(IntencaoAtoTransferencia.capturar(a).hash(), IntencaoAtoTransferencia.capturar(b).hash());
        a.put("ato", "Gestão São Luís 🧾"); b.put("ato", "Gestao Sao Luis 🧾");
        assertNotEquals(IntencaoAtoTransferencia.capturar(a).hash(), IntencaoAtoTransferencia.capturar(b).hash());
        assertNotEquals(IntencaoAtoTransferencia.capturar(Map.of("id", 1L)).hash(), IntencaoAtoTransferencia.capturar(Map.of("id", "1")).hash());
    }
    @Test void ordemDeCamposNaoMudaIntencaoMasCadaMetadadoMuda() {
        var base = new LinkedHashMap<String, Object>();
        base.put("contratoId", 1L); base.put("operacao", "TRANSFERIR_GESTAO"); base.put("dataEfetiva", "2026-11-20");
        base.put("dataAto", "2026-11-06"); base.put("inicioVigencia", "2026-11-01"); base.put("fimVigencia", "2027-11-01");
        base.put("gestaoOrigemId", 2L); base.put("gestaoDestinoId", 3L); base.put("tipoDestinoId", 4L);
        base.put("transferenciaAlvoId", 5L); base.put("eventoPrecedenteId", 6L); base.put("relacaoPrecedencia", "RETIFICA");
        base.put("autorizacaoAdministrativaAto", "ATO-SINTETICO"); base.put("referenciaSei", "SEI-SINTETICO");
        base.put("versaoConferenciaDocumental", "transferencia-gestao-conferencia-v1");
        base.put("mapeamentos", List.of(Map.of("lotacaoOrigemId", 7L, "lotacaoDestinoId", 8L, "cargoDestinoId", 9L)));
        var inversa = new LinkedHashMap<String, Object>(); var chaves = new ArrayList<>(base.keySet()); Collections.reverse(chaves);
        chaves.forEach(k -> inversa.put(k, base.get(k)));
        var original = IntencaoAtoTransferencia.capturar(base);
        assertEquals(original, IntencaoAtoTransferencia.capturar(inversa));
        for (String chave : base.keySet()) {
            var alterada = new LinkedHashMap<>(base); alterada.put(chave, "MUDANCA-ISOLADA-SINTETICA");
            assertNotEquals(original.hash(), IntencaoAtoTransferencia.capturar(alterada).hash(), chave);
        }
        assertEquals(MaterialHashEventos.sha256(original.versao(), MaterialHashEventos.texto(original.canonico())), original.hash());
    }
    @Test void dtoHistoricoAusentePermaneceDesconhecido() {
        assertNull(com.example.siggtrat.servico.MotorTemporalContratoServico.paraConferenciaDocumentalDTO(new com.example.siggtrat.entidade.EventoCicloVidaContrato()));
    }
}
