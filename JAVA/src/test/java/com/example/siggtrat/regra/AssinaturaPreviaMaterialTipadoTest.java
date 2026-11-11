package com.example.siggtrat.regra;

import com.example.siggtrat.servico.SuporteProvaHistoricaServico;
import org.junit.jupiter.api.Test;
import java.util.Arrays;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class AssinaturaPreviaMaterialTipadoTest {
    @Test void separadorDentroDoTextoNaoMudaFronteiraEntreCampos() {
        assertNotEquals(AssinaturaPreviaNegocial.sha256(List.of("A\u001fB", "C")),
                AssinaturaPreviaNegocial.sha256(List.of("A", "B\u001fC")));
    }
    @Test void nullNaoEquivaleAoLiteralMesmoNoWrapperPersistido() {
        assertNotEquals(AssinaturaPreviaNegocial.sha256(Arrays.asList((String) null)),
                AssinaturaPreviaNegocial.sha256(List.of("<null>")));
        assertNotEquals(SuporteProvaHistoricaServico.hash("fixture-prova-v1", (Object) null),
                SuporteProvaHistoricaServico.hash("fixture-prova-v1", "<null>"));
    }
    @Test void adapterUsaExatamenteOCodec53SemNovoEncoder() {
        var entrada = AssinaturaPreviaNegocial.partes(10L, "A\u001fB", null, List.of(1, "1"));
        var esperado = MaterialHashEventos.sha256("fixture-tipada-v1", MaterialHashEventos.grupo(MaterialHashEventos.texto("java.util.Collection"), MaterialHashEventos.grupo(
                MaterialHashEventos.longo(10L), MaterialHashEventos.texto("A\u001fB"), MaterialHashEventos.texto(null),
                MaterialHashEventos.grupo(MaterialHashEventos.texto("java.util.Collection"), MaterialHashEventos.grupo(MaterialHashEventos.inteiro(1), MaterialHashEventos.texto("1"))))));
        assertEquals(esperado, AssinaturaPreviaNegocial.sha256("fixture-tipada-v1", entrada));
        assertNotEquals(AssinaturaPreviaNegocial.sha256("dominio-a", entrada), AssinaturaPreviaNegocial.sha256("dominio-b", entrada));
    }
    @Test void nuloVazioTipoENivelContinuamDistintosSemReservarTextoValido() {
        assertNotEquals(AssinaturaPreviaNegocial.sha256((List<?>) null), AssinaturaPreviaNegocial.sha256(List.of()));
        assertNotEquals(AssinaturaPreviaNegocial.sha256(List.of(1)), AssinaturaPreviaNegocial.sha256(List.of("1")));
        assertNotEquals(AssinaturaPreviaNegocial.sha256(List.of("A", "B")), AssinaturaPreviaNegocial.sha256(List.of(List.of("A", "B"))));
        assertNotEquals(AssinaturaPreviaNegocial.sha256(List.of(true)), AssinaturaPreviaNegocial.sha256(List.of(List.of("java.lang.Boolean", 1))));
        assertDoesNotThrow(() -> AssinaturaPreviaNegocial.sha256(List.of("<null>", "A\u001fB")));
        assertEquals("<null>", AssinaturaPreviaNegocial.valor(null), "Compatibilidade de outros escritores fica intacta");
    }
}
