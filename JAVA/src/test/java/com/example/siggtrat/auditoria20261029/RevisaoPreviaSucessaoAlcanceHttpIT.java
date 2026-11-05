package com.example.siggtrat.auditoria20261029;

import com.example.siggtrat.entidade.Frequencia;
import com.example.siggtrat.utilitario.RelogioSistema;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;

/** AUD004/TMP009: sucessão conserva o ato anterior e não retira seu alcance. */
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class RevisaoPreviaSucessaoAlcanceHttpIT extends ContratosHttpBase {
    @Test
    void sucessaoNaoIncluiFaixaAnteriorPreservadaNemInvalidaTokenPorMudancaForaDoSeuImpacto() throws Exception {
        var c = criarCenarioPersistido();
        long id = c.contratoA.getId();
        var diaDaFrequencia = RelogioSistema.hoje().minusDays(5);
        long frequenciaId = emTransacao(() -> {
            var f = frequencia(c, false, diaDaFrequencia, "FALTA"); em.flush(); return f.getId();
        });
        var original = mapa("dataEfetiva", diaDaFrequencia.minusDays(1),
                "motivo", "Inativação original que continuará válida em seu intervalo");
        var previaOriginal = enviar(c.localA, base(id) + "/previas/inativar", original, 200,
                "AUD004 prévia do intervalo originalmente afetado");
        original.put("tokenPrevia", previaOriginal.path("tokenPrevia").asText());
        original.put("confirmarImpacto", true);
        enviar(c.localA, base(id) + "/inativar", original, 200, "CON010 ato original com impacto confirmado");
        var eventos = consultar(c.localA, base(id) + "/eventos", "TMP016 localizar o ato que será sucedido");
        long inativacao = java.util.stream.StreamSupport.stream(eventos.spliterator(), false)
                .filter(e -> e.path("tipoEvento").asText().equals("INATIVACAO_ADMINISTRATIVA"))
                .findFirst().orElseThrow().path("id").asLong();
        var sucessao = mapa("dataEfetiva", diaDaFrequencia.plusDays(1),
                "motivo", "Retomada posterior, preservando a inativação anterior",
                "eventoPrecedenteId", inativacao, "relacaoPrecedencia", "SUCEDE");
        var previa = enviar(c.localA, base(id) + "/previas/reativar", sucessao, 200,
                "AUD004 sucessão informa somente o alcance que o novo ato modifica");
        assertThat(previa.path("quantidadeFrequenciasAfetadas").asLong()).isZero();
        assertThat(previa.path("datasComFrequenciasAfetadas")).isEmpty();
        sucessao.put("tokenPrevia", previa.path("tokenPrevia").asText());
        sucessao.put("confirmarImpacto", true);
        emTransacao(() -> {
            em.find(Frequencia.class, frequenciaId).setObservacao("Alteração concorrente fora do alcance da sucessão");
            return null;
        });
        enviar(c.localA, base(id) + "/reativar", sucessao, 200,
                "AUD004 dependência anterior preservada não torna a confirmação desatualizada");
        assertThat(consultar(c.localA, base(id) + "/posicao-temporal?data=" + diaDaFrequencia,
                "TMP009 o alcance anterior é preservado").path("habilitacaoAdministrativa").asText()).isEqualTo("INATIVO");
        assertThat(consultar(c.localA, base(id) + "/posicao-temporal?data=" + diaDaFrequencia.plusDays(1),
                "TMP009 a sucessão vale somente desde sua própria data").path("habilitacaoAdministrativa").asText()).isEqualTo("ATIVO");
    }
}
